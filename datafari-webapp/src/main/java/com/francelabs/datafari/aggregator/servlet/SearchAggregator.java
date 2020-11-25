/*******************************************************************************
 * Copyright 2020 France Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.francelabs.datafari.aggregator.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.keycloak.adapters.springsecurity.account.SimpleKeycloakAccount;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;

import com.francelabs.datafari.aggregator.utils.SearchAggregatorAccessTokenManager;
import com.francelabs.datafari.api.SearchAPI;
import com.francelabs.datafari.api.SuggesterAPI;
import com.francelabs.datafari.ldap.LdapUsers;
import com.francelabs.datafari.utils.AuthenticatedUserName;
import com.francelabs.datafari.utils.HttpClientProvider;
import com.francelabs.datafari.utils.SearchAggregatorConfiguration;
import com.francelabs.datafari.utils.SearchAggregatorUserConfig;

/**
 * Servlet implementation class SearchProxy
 */
@WebServlet("/SearchAggregator/*")
public class SearchAggregator extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private static final Logger LOGGER = LogManager.getLogger(SearchAggregator.class.getName());

  private static final Map<String, ExecutorService> runningThreads = new HashMap<String, ExecutorService>();

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {

    final SearchAggregatorConfiguration sac = SearchAggregatorConfiguration.getInstance();
    final String jaExternalDatafarisStr = sac.getProperty(SearchAggregatorConfiguration.EXTERNAL_DATAFARIS);
    final boolean activated = Boolean.valueOf(sac.getProperty(SearchAggregatorConfiguration.ACTIVATED));
    final int timeoutPerRequest = Integer.parseInt(sac.getProperty(SearchAggregatorConfiguration.TIMEOUT_PER_REQUEST));
    final int globalTimeout = Integer.parseInt(sac.getProperty(SearchAggregatorConfiguration.GLOBAL_TIMEOUT));
    final String defaultDatafari = sac.getProperty(SearchAggregatorConfiguration.DEFAULT_DATAFARI) == null ? "" : sac.getProperty(SearchAggregatorConfiguration.DEFAULT_DATAFARI);

    final SearchAggregatorUserConfig aggregatorUserConfig = SearchAggregatorUserConfig.getInstance();

    // Retrieve username
    String requestingUser = "";
    String userHomeDatafari = "";
    requestingUser = AuthenticatedUserName.getName(request);
    if (requestingUser == null) {
      requestingUser = "";
    }

    userHomeDatafari = aggregatorUserConfig.getDefaultSourceFor(requestingUser);

    if (userHomeDatafari == null || userHomeDatafari.trim().length() == 0) {
      userHomeDatafari = defaultDatafari;
    }

    // Get query id if available
    final String queryId = request.getParameter("id");

    // Get the action from the request: null or searchAPI = search, suggest = suggestAPI
    final String action = request.getParameter("action");

    try {
      if (activated) {
        final List<JSONObject> responses = new ArrayList<JSONObject>();
        final JSONParser parser = new JSONParser();
        final ArrayList<String> selectedSourcesList = getSelectedSources(request.getParameter("aggregator"), userHomeDatafari);
        final ArrayList<String> filterSourceList = mergeSelectedAndAloowedList(selectedSourcesList, aggregatorUserConfig.getAllowedSourcesFor(requestingUser));

        // Extract original start and rows parameters
        int orgStart = 0;
        int orgRows = 10;
        if (request.getParameter("start") != null) {
          orgStart = Integer.parseInt(request.getParameter("start"));
        }
        if (request.getParameter("rows") != null) {
          orgRows = Integer.parseInt(request.getParameter("rows"));
        }
        // Start for aggregation requests must always be 0 as we mix results and must
        // ask all results from 0 to x (x = page requested * rows per
        // page) on each datafari and sort the results
        // This way we can guarantee the same ordering for each asked page of a same
        // request and that no document is missed
        final int start = 0;
        // Rows for aggregation requests must be original start + original rows
        final int rows = orgStart + orgRows;
        // Add local response
        final String handler = getHandler(request);
        final String protocol = request.getScheme() + ":";
        final Map<String, String[]> parameterMap = new HashedMap<String, String[]>();
        parameterMap.putAll(request.getParameterMap());
        // Remove potential AuthenticatedUserName param, as this param should only be
        // set by the search aggregator
        parameterMap.remove("AuthenticatedUserName");
        // Add fl parameter if not present
        if (!parameterMap.containsKey("fl")) {
          parameterMap.put("fl", new String[] { "*,score" });
        } else {
          String value = parameterMap.get("fl")[0];
          value += ",score";
          parameterMap.remove("fl");
          parameterMap.put("fl", new String[] { value });
        }
        // Change start and rows parameters
        if (parameterMap.containsKey("start")) {
          parameterMap.remove("start");
        }
        if (parameterMap.containsKey("rows")) {
          parameterMap.remove("rows");
        }
        parameterMap.put("start", new String[] { String.valueOf(start) });
        parameterMap.put("rows", new String[] { String.valueOf(rows) });
        boolean wildCardQuery = false;
        if (parameterMap.containsKey("q") && parameterMap.get("q")[0].toString().contentEquals("*:*")) {
          wildCardQuery = true;
        }

        final Map<String, String[]> localMap = new HashedMap<>(parameterMap);
        localMap.remove("rows");
        localMap.put("rows", new String[] { "0" });

        final JSONArray jaExternalDatafaris = (JSONArray) parser.parse(jaExternalDatafarisStr);
        // Filter out external sources that are not selected.
        // If nothing is selected, we keep all sources
        jaExternalDatafaris.removeIf(jsonObject -> (filterSourceList.size() != 0
            && !filterSourceList.contains(((JSONObject) jsonObject).get("label")))
            || !((Boolean) ((JSONObject) jsonObject).get("enabled")));

        if (jaExternalDatafaris.size() == 0) {
          LOGGER.warn("No external Datafari activated and available to process an aggregator query.");
          JSONObject jsonResp = new JSONObject();
          jsonResp.put("origin", "aggregator");
          jsonResp.put("code", 1);
          jsonResp.put("message", "No external Datafari available for this request.");
          response.setStatus(500);
          response.setCharacterEncoding("utf-8");
          response.setContentType("text/json;charset=utf-8");
          response.setHeader("Content-Type", "application/json;charset=UTF-8 ");
          response.getWriter().write(jsonResp.toJSONString());
          return;
        }
        // If request is a suggest request then perform the query if there is only one Datafari to request
        // Otherwise return an empty json
        if (action != null && action.contentEquals("suggest")) {
          String outputString = "{}";
          if (jaExternalDatafaris.size() == 1) {
            final JSONObject externalDatafari = (JSONObject) jaExternalDatafaris.get(0);
            final String authUsername = requestingUser;
            String suggestResponse = externalDatafariRequest(timeoutPerRequest, handler, parameterMap, externalDatafari,
                authUsername);
            if (suggestResponse != null) {
              final String wrapperFunction = request.getParameter("json.wrf");
              if (wrapperFunction != null) {
                outputString= wrapperFunction + "(" + suggestResponse + ")";
              }
            }
          }
          response.setStatus(200);
          response.setCharacterEncoding("utf-8");
          response.setContentType("text/json;charset=utf-8");
          response.setHeader("Content-Type", "application/json;charset=UTF-8 ");
          response.getWriter().write(outputString);
          return;
        }

        if (jaExternalDatafaris.size() > 0) {
          // Create a thread pool of external datafaris nb threads
          final ExecutorService threadPool = Executors.newFixedThreadPool(jaExternalDatafaris.size());
          for (int i = 0; i < jaExternalDatafaris.size(); i++) {
            final JSONObject externalDatafari = (JSONObject) jaExternalDatafaris.get(i);
            final String authUsername = requestingUser;
            threadPool.submit(() -> {
              final String searchResponse = externalDatafariRequest(timeoutPerRequest, handler, parameterMap, externalDatafari, authUsername);
              if (searchResponse != null) {
                try {
                  responses.add((JSONObject) parser.parse(searchResponse));
                } catch (final Exception e) {
                  LOGGER.error("Error processing external Datafari request", e);
                }
              }
            });
          }

          // Register ThreadPool so that it can be canceled by a POST request
          String threadPoolId = requestingUser;
          if (queryId != null) {
            threadPoolId = queryId;
          }
          runningThreads.put(threadPoolId, threadPool);

          // Wait for threadpool to end for 60 seconds max
          threadPool.shutdown();
          if (!threadPool.awaitTermination(globalTimeout, TimeUnit.SECONDS)) {
            threadPool.shutdownNow();
          }
          runningThreads.remove(threadPoolId);
        }

        // None of the external Datafaris responded correctly (but they were contacted)
        if (responses.size() == 0) {
          LOGGER.warn("No external Datafari responded correctly to process an aggregator query.");
          JSONObject jsonResp = new JSONObject();
          jsonResp.put("origin", "aggregator");
          jsonResp.put("code", 2);
          jsonResp.put("message", "External Datafari unavailable or unreachable for this request.");
          response.setStatus(500);
          response.setCharacterEncoding("utf-8");
          response.setContentType("text/json;charset=utf-8");
          response.setHeader("Content-Type", "application/json;charset=UTF-8 ");
          response.getWriter().write(jsonResp.toJSONString());
          return;
        }
        // Merge the responses
        String finalResponseStr = mergeResponses(responses, orgStart, orgRows, wildCardQuery).toJSONString();
        final String wrapperFunction = request.getParameter("json.wrf");
        if (wrapperFunction != null) {
          finalResponseStr = wrapperFunction + "(" + finalResponseStr + ")";
        }
        response.setStatus(200);
        response.setCharacterEncoding("utf-8");
        response.setContentType("text/json;charset=utf-8");
        response.setHeader("Content-Type", "application/json;charset=UTF-8 ");
        response.getWriter().write(finalResponseStr);
      } else {
        String searchResponse = "";
        if (action != null) {
          switch (action) {
            case "suggest":
              searchResponse = SuggesterAPI.suggest(request);
              break;
            case "search":
            default:
              searchResponse = SearchAPI.search(request);
          }
        } else {
          searchResponse = SearchAPI.search(request);
        }
        final String wrapperFunction = request.getParameter("json.wrf");
        if (wrapperFunction != null) {
          searchResponse = wrapperFunction + "(" + searchResponse + ")";
        }
        response.setStatus(200);
        response.setCharacterEncoding("utf-8");
        response.setContentType("text/json;charset=utf-8");
        response.setHeader("Content-Type", "application/json;charset=UTF-8 ");
        response.getWriter().write(searchResponse);
      }

    } catch (

    final Exception e) {
      LOGGER.error("Search aggregator unexpected error", e);
      response.setStatus(500);
      response.getWriter().write(e.getMessage());
    }

  }

  private String externalDatafariRequest(final int timeoutPerRequest, final String handler,
      final Map<String, String[]> parameterMap, final JSONObject externalDatafari, final String authUsername) {
    try (final CloseableHttpClient client = HttpClientProvider.getInstance().newClient(timeoutPerRequest,
        timeoutPerRequest);) {
      final boolean enabled = (Boolean) externalDatafari.get("enabled");
      if (enabled) {
        final String searchApiUrl = externalDatafari.get("search_api_url").toString();
        final String secret = externalDatafari.get("search_aggregator_secret").toString();
        final String tokenRequestUrl = externalDatafari.get("token_request_url").toString();
        final String accessToken = SearchAggregatorAccessTokenManager.getInstance().getAccessToken(tokenRequestUrl,
            secret);
        final URIBuilder uriBuilder = new URIBuilder(searchApiUrl + handler);
        parameterMap.forEach((name, values) -> {
          for (int j = 0; j < values.length; j++) {
            uriBuilder.addParameter(name, values[j]);
          }
        });
        // Add username param
        // Remove potential AuthenticatedUserName param, as this param should only be
        // set by the API
        uriBuilder.addParameter("AuthenticatedUserName", authUsername);
        final HttpGet getReq = new HttpGet(uriBuilder.build());
        getReq.addHeader(HttpHeaders.AUTHORIZATION, "bearer " + accessToken);
        try (CloseableHttpResponse getResponse = client.execute(getReq);) {
          if (getResponse.getStatusLine().getStatusCode() == 200) {
            return IOUtils.toString(getResponse.getEntity().getContent(), StandardCharsets.UTF_8);
          } else {
            LOGGER.error("Error " + getResponse.getStatusLine().getStatusCode() + " "
                + getResponse.getStatusLine().getReasonPhrase() + " requesting " + getReq.toString());
          }
        }
      }
    } catch (final Exception e) {
      LOGGER.error("Error processing external Datafari request", e);
    }
    return null;
  }

  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse resp) throws ServletException, IOException {
    // Retrieve username
    String requestingUser = "";
    if (request.getUserPrincipal() != null) {
      // Get the username
      if (request.getUserPrincipal() instanceof KeycloakAuthenticationToken) {
        final KeycloakAuthenticationToken keycloakToken = (KeycloakAuthenticationToken) request.getUserPrincipal();
        if (keycloakToken.getDetails() instanceof SimpleKeycloakAccount) {
          final SimpleKeycloakAccount keycloakAccount = (SimpleKeycloakAccount) keycloakToken.getDetails();
          requestingUser = keycloakAccount.getKeycloakSecurityContext().getToken().getPreferredUsername();
        } else {
          requestingUser = request.getUserPrincipal().getName().replaceAll("[^\\\\]*\\\\", "");
        }
      } else {
        requestingUser = request.getUserPrincipal().getName().replaceAll("[^\\\\]*\\\\", "");
      }
      if (!requestingUser.contains("@")) {
        final String domain = LdapUsers.getInstance().getUserDomain(requestingUser);
        if (domain != null && !domain.isEmpty()) {
          requestingUser += "@" + domain;
        }
      }
    }

    // Get query id if available
    final String queryId = request.getParameter("id");

    final String action = request.getParameter("action");
    if (action != null && action.contentEquals("stop") && !requestingUser.isEmpty() && runningThreads.containsKey(requestingUser)) {

      String threadPoolId = requestingUser;
      if (queryId != null) {
        threadPoolId = queryId;
      }

      runningThreads.get(threadPoolId).shutdownNow();
      runningThreads.remove(threadPoolId);
    }
  }

  private JSONObject mergeResponses(final List<JSONObject> responses, final int originalStart, final int originalRows, final boolean wildCardQuery) {
    JSONObject finalResponse = new JSONObject();
    if (responses.size() > 0) {
      finalResponse = (JSONObject) responses.get(0).clone();

      final LinkedList<JSONObject> orderedDocs = new LinkedList<JSONObject>();
      final JSONObject mergedHighlightings = new JSONObject();
      int aggregatedNumFound = 0;
      int aggregatedQTime = 0;
      Double maxScore = 0.0;
      final JSONObject mergedFacetQueries = new JSONObject();
      final JSONObject mergedFacetFields = new JSONObject();
      final Map<String, Map<String, Integer>> mergedFacetFieldsMap = new HashMap<String, Map<String, Integer>>();

      // If wildcard query then mix documents equally
      if (wildCardQuery) {
        mixDocuments(orderedDocs, responses);
      }
      for (final JSONObject result : responses) {
        final JSONObject resultResponse = (JSONObject) result.get("response");
        final JSONArray docs = (JSONArray) resultResponse.get("docs");
        final JSONObject highlighting = (JSONObject) result.get("highlighting");

        // Aggregate numFound
        aggregatedNumFound += (Long) resultResponse.get("numFound");

        // Update maxScore
        final Double resultMaxScore = (Double) resultResponse.get("maxScore");
        if (maxScore < resultMaxScore) {
          maxScore = resultMaxScore;
        }

        // Aggregate QTime
        aggregatedQTime += (Long) ((JSONObject) result.get("responseHeader")).get("QTime");

        // Order documents
        if (!wildCardQuery) {
          orderDocs(orderedDocs, docs);
        }

        // Merge highlights
        if (highlighting != null) {
          highlighting.forEach((key, value) -> {
            mergedHighlightings.put(key, value);
          });
        }

        // Merge facets
        mergeFacets(result, mergedFacetQueries, mergedFacetFieldsMap);

        // Construct mergedFacetFields from the map
        mergedFacetFieldsMap.forEach((field, mapValues) -> {
          final JSONArray fieldValues = new JSONArray();
          mapValues.forEach((fieldValue, count) -> {
            fieldValues.add(fieldValue);
            fieldValues.add(count);
          });
          mergedFacetFields.put(field, fieldValues);
        });
      }

      // Construct finalResponse JSON
      final JSONObject finalResponseResp = (JSONObject) finalResponse.get("response");
      // Replace numFound
      finalResponseResp.put("numFound", aggregatedNumFound);
      // Replace max score
      finalResponseResp.put("maxScore", maxScore);
      // Update start
      finalResponseResp.put("start", originalStart);
      // Replace QTime
      final JSONObject finalResponseHeader = (JSONObject) finalResponse.get("responseHeader");
      finalResponseHeader.put("QTime", aggregatedQTime);
      // Update rows
      final JSONObject params = (JSONObject) finalResponseHeader.get("params");
      params.put("rows", originalRows);
      // Put neutral spellcheck
      final JSONObject neutralSpellcheck = new JSONObject();
      neutralSpellcheck.put("collations", new JSONArray());
      neutralSpellcheck.put("suggestions", new JSONArray());
      neutralSpellcheck.put("correctlySpelled", true);
      finalResponse.put("spellcheck", neutralSpellcheck);

      // Construct final docs and highlighting
      final JSONArray fDocs = (JSONArray) finalResponseResp.get("docs");
      final JSONObject highlighting = (JSONObject) finalResponse.get("highlighting");
      fDocs.clear();
      highlighting.clear();
      int maxIterations = originalStart + originalRows;
      if (maxIterations > orderedDocs.size()) {
        maxIterations = orderedDocs.size();
      }

      // The response must contains the ordered docs between position originalStart
      // and originalStart + Rows
      for (int i = originalStart; i < maxIterations; i++) {
        final JSONObject doc = orderedDocs.get(i);
        fDocs.add(doc);
        // Add the corresponding higlighting
        final String docId = doc.get("id").toString();
        if (mergedHighlightings.containsKey(docId)) {
          final JSONObject hlObj = (JSONObject) mergedHighlightings.get(docId);
          highlighting.put(docId, hlObj);
        }
      }

      if (finalResponse.get("facet_counts") != null) {
        final JSONObject facetCounts = (JSONObject) finalResponse.get("facet_counts");
        // Replace facet queries
        facetCounts.put("facet_queries", mergedFacetQueries);
        // Replace facet fields
        facetCounts.put("facet_fields", mergedFacetFields);
      }
    }
    return finalResponse;
  }

  private void mixDocuments(final LinkedList<JSONObject> orderedDocs, final List<JSONObject> responses) {
    int maxSize = 0;
    final List<JSONArray> docs = new ArrayList<JSONArray>();
    for (final JSONObject response : responses) {
      final JSONArray respDocs = (JSONArray) ((JSONObject) response.get("response")).get("docs");
      docs.add(respDocs);
      if (maxSize < respDocs.size()) {
        maxSize = respDocs.size();
      }
    }
    for (int i = 0; i < maxSize; i++) {
      for (final JSONArray d : docs) {
        if (i < d.size()) {
          orderedDocs.add((JSONObject) d.get(i));
        }
      }
    }
  }

  /**
   * Order docs in a {@link LinkedList} according to their score
   *
   * @param orderedDocs
   *          {@link LinkedList} containing ordered docs, in which provided new docs will be inserted at the right place (according to their
   *          score)
   * @param docs
   *          new docs to insert in the provided {@link LinkedList} at the right place
   */
  private void orderDocs(final LinkedList<JSONObject> orderedDocs, final JSONArray docs) {
    for (final Object docObj : docs) {
      final JSONObject doc = (JSONObject) docObj;
      final Double score = (Double) doc.get("score");
      if (orderedDocs.isEmpty()) {
        orderedDocs.add(doc);
      } else {
        int insertIndex = -1;
        for (int i = 0; i < orderedDocs.size(); i++) {
          final Double orderedScore = (Double) orderedDocs.get(i).get("score");
          if (score > orderedScore) {
            insertIndex = i;
            break;
          }
        }
        if (insertIndex != -1) {
          orderedDocs.add(insertIndex, doc);
        } else {
          orderedDocs.addLast(doc);
        }
      }
    }
  }

  private void mergeFacets(final JSONObject result, final JSONObject mergedFacetQueries, final Map<String, Map<String, Integer>> mergedFacetFieldsMap) {
    if (result.get("facet_counts") != null) {
      final JSONObject facetCounts = (JSONObject) result.get("facet_counts");

      // Facet Queries
      if (facetCounts.get("facet_queries") != null) {
        final JSONObject facetQueries = (JSONObject) facetCounts.get("facet_queries");
        facetQueries.forEach((key, value) -> {
          if (mergedFacetQueries.containsKey(key)) {
            int existingValue = ((Long) mergedFacetQueries.get(key)).intValue();
            existingValue += (Long) value;
            mergedFacetQueries.put(key, existingValue);
          } else {
            mergedFacetQueries.put(key, value);
          }
        });
      }

      // Facet Fields
      if (facetCounts.get("facet_fields") != null) {
        final JSONObject facetFields = (JSONObject) facetCounts.get("facet_fields");
        facetFields.forEach((field, facets) -> {
          final JSONArray facetValues = (JSONArray) facets;
          Map<String, Integer> existingFacetValues = new HashMap<String, Integer>();
          if (mergedFacetFieldsMap.containsKey(field)) {
            existingFacetValues = mergedFacetFieldsMap.get(field);
          } else {
            mergedFacetFieldsMap.put(field.toString(), existingFacetValues);
          }
          for (int i = 0; i < facetValues.size(); i += 2) {
            final String facetValue = facetValues.get(i).toString();
            final int facetCount = ((Long) facetValues.get(i + 1)).intValue();
            if (existingFacetValues.containsKey(facetValue)) {
              int existingFacetCount = existingFacetValues.get(facetValue);
              existingFacetCount += facetCount;
              existingFacetValues.put(facetValue, existingFacetCount);
            } else {
              existingFacetValues.put(facetValue, facetCount);
            }
          }

        });
      }
    }
  }

  private String getHandler(final HttpServletRequest servletRequest) {
    String pathInfo = servletRequest.getPathInfo();
    if (pathInfo == null) {
      pathInfo = servletRequest.getServletPath();
      if (pathInfo == null) {
        pathInfo = "/select";
      }
    }
    return pathInfo.substring(pathInfo.lastIndexOf("/"), pathInfo.length());
  }

  private ArrayList<String> getSelectedSources(final String parameter, final String defaultDatafari) {
    final ArrayList<String> result = new ArrayList<>();
    if (parameter == null && defaultDatafari != null && defaultDatafari.trim().length()>0) {
      result.add(defaultDatafari);
    } else if (parameter != null && parameter.length() != 0) {
      // If the parameter is set as empty, we set an empty list meaning no filtering
      // (search on everything)
        result.addAll(Arrays.asList(parameter.split(",")));
    }
    return result;
  }

  /**
   * Returns an empty arraylist if both arbuments are null or empty. If one is null or empty and the other is not, returns the one that is not
   * empty. If both arguments are not empty returns the selected sources list filtered by removing any element that is not present in the
   * allowed sources list.
   *
   * @param selectedSourcesList
   * @param allowedSources
   * @return
   */
  private ArrayList<String> mergeSelectedAndAloowedList(final ArrayList<String> selectedSourcesList, final ArrayList<String> allowedSources) {
    ArrayList<String> result = new ArrayList<>();
    if (selectedSourcesList != null && (allowedSources == null || allowedSources.size() == 0)) {
      result = selectedSourcesList;
    } else if (allowedSources != null && (selectedSourcesList == null || selectedSourcesList.size() == 0)) {
      result = allowedSources;
    } else if (selectedSourcesList != null && allowedSources != null) {
      result.addAll(selectedSourcesList);
      result.removeIf(selectedName -> !allowedSources.contains(selectedName));
    }
    return result;
  }

}