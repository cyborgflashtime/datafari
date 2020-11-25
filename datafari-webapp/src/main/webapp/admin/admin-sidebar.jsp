<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="utf-8"%>
<%
  String path = request.getContextPath();
  String mainCollection = "@MAINCOLLECTION@";
  String getProtocol=request.getScheme();
  String getDomain=request.getServerName();
  String getPort=Integer.toString(request.getServerPort());
  String getSolrSchemaAnalysis ="";
  String getSolrSchemaAdmin ="";
  String getPath = getProtocol+"://"+getDomain+":"+getPort+path;
  String ApachePresent = "@APACHE-PRESENT@";
  String pathKibana="";
  String getMCF = "@GET-MCF-IP@" ;
  String getSolr = "@GET-SOLR-IP@" ;
  pathKibana = "https://"+getDomain+":5601";
  getSolrSchemaAnalysis = getSolr + "/#/"+mainCollection + "/analysis";
  getSolrSchemaAdmin = getSolr + "/#/"+ mainCollection + "/schema";
  
  String getKibanaCorpusAnalysis = pathKibana +"/app/kibana?security_tenant=searchexpert_tenant#/dashboard/Content-Analysis";
  String getKibanaCorpusAnalysisOverTime = pathKibana +"/app/kibana?security_tenant=searchexpert_tenant#/dashboard/Content-Analysis-Over-Time";
  String getKibanaQueriesAnalysis = pathKibana +"/app/kibana?security_tenant=searchexpert_tenant#/dashboard/Queries-Statistics";
  String getKibanaProblematicFiles = pathKibana +"/app/kibana?security_tenant=admin_tenant#/dashboard/Crawl-monitoring-dashboard";
  String getKibanaLog = pathKibana +"/app/kibana?security_tenant=admin_tenant#/dashboard/Global-Datafari-Dashboard" ;
  String getKibanaMetricBeatOverview = pathKibana +"/app/kibana#/dashboard/Metricbeat-system-overview";    
%>
<nav id="sidebar" class="bg-light calc-height">
  <ul class="list-unstyled">
<%-- service administration menu start --%>
    <%
    if(request.isUserInRole("SearchAdministrator")){
    %>
      <li id="ServiceAdmin" class="collapse-effect">
        <a class="dropdown-toggle" data-toggle="collapse" href="#menu-service-administration" role="button" aria-expanded="false" aria-controls="menu-service-administration">
          <i class="fas fa-wrench"></i><span id="service-administration-AdminUI" class="hidden-xs">Service Administration</span>
        </a>
        <ul id="menu-service-administration" class="list-unstyled collapse">

          <li id="serviceRestart"><a id="service-restart-AdminUI" class="ajax-link" href="?page=serviceRestart">Service Restart</a></li>
          <li id="serviceBackup"><a id="service-backup-AdminUI" class="ajax-link" href="?page=serviceBackup">Service Backup</a></li>
          <li id="serviceReinit"><a id="service-reinit-AdminUI" class="ajax-link" href="?page=serviceReinit">Service Connector Reinitialization</a></li>

         <%
          }
          %>
        </ul>
      </li>
<%-- service administration menu end --%>
    <%
    if(request.isUserInRole("SearchExpert")||request.isUserInRole("SearchAdministrator")){
    %>
      <li id="Connectors" class="collapse-effect">
        <a class="dropdown-toggle" data-toggle="collapse" href="#menu-connectors" role="button" aria-expanded="false" aria-controls="menu-connectors">
          <i class="fas fa-arrows-alt"></i><span id="connectors-AdminUI" class="hidden-xs"></span>
        </a>
        <ul id="menu-connectors" class="list-unstyled collapse">
<%-- MCF menu start --%>
          <li id="MCFSimplified"><a id="MCFSimplified-AdminUI" class="ajax-link" href="?page=mcfSimplified"></a></li>
          <li id="MCFAdminUI">
            <a target="_blank" href="<%= getMCF %>" id="MCFAdmin-AdminUI"></a>
          </li>
          <%
          }
          if(request.isUserInRole("SearchAdministrator")){
          %>
          <li id="MCFBackupRestore"><a id="MCFBackupRestore-AdminUI" class="ajax-link" href="?page=mcfBackupRestore"></a></li>
          <li id="MCFChangePassword" ><a id="MCFPassword-AdminUI" class="ajax-link" href="?page=MCFChangePassword"></a></li>
         
<%-- MCF menu end --%>
         <%
          }
          %>
        </ul>
      </li>
<%-- MCF section end --%>
      
       <%
          if(request.isUserInRole("SearchExpert")||request.isUserInRole("SearchAdministrator")){
          %>
      <li id="Usages Analysis" class="collapse-effect">
        <a href="#menu-usages-analysis" class="dropdown-toggle" data-toggle="collapse" role="button" aria-expanded="false" aria-controls="menu-usages-analysis">
          <i class="fas fa-chart-bar"></i><span id="usagesAnalysis-AdminUI" class="hidden-xs"></span>
        </a>
        <ul id="menu-usages-analysis" class="list-unstyled collapse">
          <li id="CorpusAnalysis"><a id="corpusAnalysis-AdminUI" target="_blank" href="<%= getKibanaCorpusAnalysis %>"></a></li>
          <li id="CorpusOTAnalysis"><a id="corpusOTAnalysis-AdminUI" target="_blank" href="<%= getKibanaCorpusAnalysisOverTime %>"></a></li>
          <li id="QueriesAnalysis"><a id="queriesAnalysis-AdminUI" target="_blank" href="<%= getKibanaQueriesAnalysis %>"></a></li>
        </ul>
      </li>
      
      <li id="System Analysis" class="collapse-effect">
        <a href="#menu-system-analysis" class="dropdown-toggle" data-toggle="collapse" role="button" aria-expanded="false" aria-controls="menu-system-analysis">
          <i class="fas fa-clipboard"></i><span id="systemAnalysis-AdminUI" class="hidden-xs"></span>
        </a>
        <ul id="menu-system-analysis" class="list-unstyled collapse">
         <li id="Duplicates"><a id="duplicates-AdminUI" class="ajax-link" href="?page=duplicates"></a></li>
         <%
          if(request.isUserInRole("SearchAdministrator")){
         %>
         <li id="CrawlDataMonitoring"><a id="crawlDataMonitoring-AdminUI" class="ajax-link" href="?page=crawlDataMonitoring"></a></li>
         <li id="ProblematicFiles"><a id="problematicFiles-AdminUI" target="_blank" href="@PROBLEMATIC_FILES@"></a></li>
         <li id="LogsAnalysis"><a id="logsAnalysis-AdminUI" target="_blank" href="@LOG@"></a></li>
         <li id="downloadLogs"><a id="downloadLogs-AdminUI" class="ajax-link" href="?page=downloadLogs"></a></li>
<!--          <li id="metricbeatOverview"><a id="metricbeatOverview-AdminUI" target="_blank" href="@METRICBEAT@"></a></li> -->
         <%
          }
         %>
        </ul>
      </li>
      
      <li id="Admin" class="collapse-effect">
        <a href="#menu-admin" class="dropdown-toggle" data-toggle="collapse" role="button" aria-expanded="false" aria-controls="menu-admin">
          <i class="fas fa-wrench"></i><span id="searchEngineAdmin-AdminUI" class="hidden-xs"></span>
        </a>
        <ul id="menu-admin" class="list-unstyled collapse">
        <li id="SolrAdmin"><a target="_blank" href="<%= getSolr %>" id="solrAdmin-AdminUI"></a></li>
        <li id="SolrSchemaAdmin"><a target="_blank" href="<%= getSolrSchemaAdmin %>" id="schemaAdmin-AdminUI"></a></li>
        <li id="AlertAdmin"><a id="alertAdmin-AdminUI" class="ajax-link" href="?page=alertsAdmin"></a></li>
        <li id="IndexField"><a id="indexField-AdminUI" class="ajax-link" href="?page=IndexField"></a></li>
        <li id="SchemaAnalysis"><a target="_blank" id="schemaAnalysis-AdminUI" href="<%= getSolrSchemaAnalysis %>"></a></li>
          
        <%
          if(request.isUserInRole("SearchAdministrator")){
        %>          
          <li id="ELKConfiguration"><a id="elkConfiguration-AdminUI" class="ajax-link" href="?page=elkConfiguration"></a></li>
        <%
          }
        %>
          <li id="AnnotatorConfiguration"><a id="annotatorConfiguration-AdminUI" class="ajax-link" href="?page=annotatorConfiguration"></a></li>
          <li id="DuplicatesConfiguration"><a id="duplicatesConfiguration-AdminUI" class="ajax-link" href="?page=duplicatesConfiguration"></a></li>
          <li id="SizeLimitation"><a id="sizeLimitation-AdminUI" class="ajax-link" href="?page=SizeLimitations"></a></li>
          <li id="AutocompleteConfiguration"><a id="autocompleteConfig-AdminUI" class="ajax-link" href="?page=AutocompleteConfiguration"></a></li>
        </ul>
      </li>
      <li id="Conf" class="collapse-effect">
        <a href="#menu-conf" class="dropdown-toggle" data-toggle="collapse" role="button" aria-expanded="false" aria-controls="menu-conf">
          <i class="fas fa-desktop"></i><span id="searchEngineConfig-AdminUI" class="hidden-xs"></span>
        </a>
        <ul id="menu-conf" class="list-unstyled collapse">
          <li id="SearchAggregatorConf"><a id="searchAggregatorConf-AdminUI" class="ajax-link" href="?page=searchAggregatorConfiguration"></a></li>
          <li id="DepartmentSearchConf"><a id="departmentSearchConf-AdminUI" class="ajax-link" href="?page=departmentSearchConf"></a></li>
          <li id="QueryElevator"><a id="queryElevator-AdminUI" class="ajax-link" href="?page=queryElevator"></a></li>
          <li id="PromoLink"><a id="promoLinks-AdminUI" class="ajax-link" href="?page=promoLinks"></a></li>
          <li id="Synonyms" ><a id="synonyms-AdminUI" class="ajax-link" href="?page=Synonyms"></a></li>
          <li id="Stopwords"><a id="stopwords-AdminUI" class="ajax-link" href="?page=StopWords"></a></li>
          <li id="Protwords"><a id="protwords-AdminUI" class="ajax-link" href="?page=ProtWords"></a></li>
          <li id="FieldWeightAPI"><a id="fieldWeightAPI-AdminUI" class="ajax-link" href="?page=FieldWeightAPI"></a></li>

          <li id="LikesAndFavorites"><a id="likesFavoritesSearchEng-AdminUI" class="ajax-link" href="?page=config_likesAndFavorites"></a></li>
          <li id="RelevancySetupFile"><a id="relevancySetupFile-AdminUI" class="ajax-link" href="?page=relevancyFile"></a></li>
          <%
            if(request.isUserInRole("SearchAdministrator")){
          %>
          <li id="EntityExtractionConf"><a id="entityExtractionConf-AdminUI" class="ajax-link" href="?page=entityExtractionConf"></a></li>
          <li id="STTEntitiesConfiguration"><a id="sttEntitiesConfiguration-AdminUI" class="ajax-link" href="?page=sttEntitiesConfiguration">Advanced Entitiy Extraction</a></li>
          <li id="tagCloudConfiguration"><a id="tagCloudConfiguration-AdminUI" class="ajax-link" href="?page=tagCloudConfiguration">Tag Cloud</a></li> 
          <%
            }
          %>
          <li id="Zookeeper"><a id="zookeeper-AdminUI" class="ajax-link" href="?page=config_zookeeper"></a></li>
          <li id="HelpPage"><a id="helpPage-AdminUI" class="ajax-link" href="?page=HelpEditor">Edit Help Page</a></li>
          <li id="UploadCertificate"><a id="upload-AdminUI" class="ajax-link" href="?page=uploadcertificate"></a></li>

        </ul>
      </li>
      <%
        if(request.isUserInRole("SearchAdministrator")){
      %>

      <li id="userManagement" class="collapse-effect">
        <a href="#menu-user-management" class="dropdown-toggle" data-toggle="collapse" role="button" aria-expanded="false" aria-controls="menu-user-management">
          <i class="fas fa-users"></i><span id="userManagement-AdminUI" class="hidden-xs"></span>
        </a>
        <ul id="menu-user-management" class="list-unstyled collapse">
          <li id="forgetUser"><a id="forgetUser-AdminUI" class="ajax-link" href="?page=forgetUser"></a></li>
          <li id="addUser"><a id="addUser-AdminUI" class="ajax-link" href="?page=addUser"></a></li>
          <li id="modifyUser"><a id="modifyUsers-AdminUI" class="ajax-link" href="?page=modifyUsers"></a></li>
          <li id="modifyServiceUsers"><a id="modifyServiceUsers-AdminUI" class="ajax-link" href="?page=modifyServiceUsers"></a></li>
          <li id="manageImportedUsers"><a id="manageImportedUsers-AdminUI" class="ajax-link" href="?page=manageImportedUsers"></a></li>
          <li id="modifyDepartment"><a id="modifyDepartment-AdminUI" class="ajax-link" href="?page=userDepartment"></a></li>
        </ul>
      </li>
      
      <li id="activeDirectoryManagement" class="collapse-effect">
        <a href="#menu-active-directory" class="dropdown-toggle" data-toggle="collapse" role="button" aria-expanded="false" aria-controls="menu-active-directory">
          <i class="fas fa-address-book"></i><span id="activeDirectoryManagement-AdminUI" class="hidden-xs"></span>
        </a>
        <ul id="menu-active-directory" class="list-unstyled collapse">
          <li id="ADConfiguration"><a id="ADConfig-AdminUI" class="ajax-link" href="?page=ldapConfiguration"></a></li>
          <li id="testADAuthority"><a id="testADAuthority-AdminUI" class="ajax-link" href="?page=testADAuthority"></a></li>
        </ul>
      </li>
      
      <li id="Licence">
        <a href="?page=licence" class="ajax-link">
          <i class="fas fa-barcode"></i>
           <span id="licence-AdminUI" class="hidden-xs"></span>
        </a>
      </li>
      <%
        }
      }
      %>
<%-- end menu --%>
  </ul>
</nav>