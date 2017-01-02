package com.francelabs.datafari.utils;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.francelabs.datafari.exception.CodesReturned;

public class ActivateLDAPSolr {
	private static File solrConfig;
	private static File customSearchHandler;
	private static ActivateLDAPSolr instance;
	private final static Logger logger = Logger.getLogger(ActivateLDAPSolr.class);

	private ActivateLDAPSolr() {
		final String filePath = Environment.getProperty("catalina.home") + File.separator + ".." + File.separator + "solr" + File.separator
				+ "solrcloud" + File.separator + "FileShare" + File.separator + "conf" + File.separator + "solrconfig.xml";
		solrConfig = new File(filePath);
		final String filePathJSON = Environment.getProperty("catalina.home") + File.separator + ".." + File.separator + "bin" + File.separator
				+ "common" + File.separator + "config" + File.separator + "manifoldcf" + File.separator + "monoinstance" + File.separator
				+ "authorityconnections" + File.separator + "authorityConnection.json";
		final String filePathGroupJSON = Environment.getProperty("catalina.home") + File.separator + ".." + File.separator + "bin" + File.separator
				+ "common" + File.separator + "config" + File.separator + "manifoldcf" + File.separator + "monoinstance" + File.separator
				+ "authorityconnections" + File.separator + "authorityGroups.json";
		new File(filePathJSON);
		new File(filePathGroupJSON);

		// Custom searchHandler
		final String customSearchHandlerPath = Environment.getProperty("catalina.home") + File.separator + ".." + File.separator + "solr"
				+ File.separator + "solrcloud" + File.separator + "FileShare" + File.separator + "conf" + File.separator + "customs_solrconfig"
				+ File.separator + "custom_search_handler.incl";
		customSearchHandler = new File(customSearchHandlerPath);
	}

	private static ActivateLDAPSolr getInstance() {
		if (instance == null) {
			return instance = new ActivateLDAPSolr();
		}
		return instance;
	}

	public static int activate() throws Exception {
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		File fileToParse = getInstance().customSearchHandler;
		Document docSchem;
		try {
			docSchem = dBuilder.parse(fileToParse);
		} catch (final Exception e) {
			fileToParse = getInstance().solrConfig;
			docSchem = dBuilder.parse(fileToParse);
		}

		final XPathFactory xPathfactory = XPathFactory.newInstance();
		final XPath xpath = xPathfactory.newXPath();
		XPathExpression expr = xpath.compile("//requestHandler[@class=\"solr.SearchHandler\" and @name=\"/select\"]");
		final Node requestHandler = (Node) expr.evaluate(docSchem, XPathConstants.NODE);

		if (requestHandler != null) {
			expr = xpath.compile("//lst[@name=\"appends\"]");
			final Node appends = (Node) expr.evaluate(requestHandler, XPathConstants.NODE);
			if (appends == null) {
				// Save the current searchHandler version
				final String oldSearchHandler = nodeToString(requestHandler);

				// Add the manifoldCFSecurity to the searchHandler
				final Element elementRoot = docSchem.createElement("lst");
				final Element elementChild = docSchem.createElement("str");
				elementRoot.setAttribute("name", "appends");
				elementChild.setAttribute("name", "fq");
				elementChild.appendChild(docSchem.createTextNode("{!manifoldCFSecurity}"));
				elementRoot.appendChild(elementChild);
				requestHandler.appendChild(elementRoot);

				// Replace the old searchHandler by the new one
				String configContent = getFileContent(fileToParse);
				final String newSearchHandler = nodeToString(requestHandler);
				configContent = configContent.replace(oldSearchHandler, newSearchHandler);

				// Save the file
				saveFile(fileToParse, configContent);
			}
		}

		return CodesReturned.ALLOK.getValue();
	}

	public static int disactivate() throws Exception {
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		File fileToParse = getInstance().customSearchHandler;
		Document docSchem;
		try {
			docSchem = dBuilder.parse(fileToParse);
		} catch (final Exception e) {
			fileToParse = getInstance().solrConfig;
			docSchem = dBuilder.parse(fileToParse);
		}

		final XPathFactory xPathfactory = XPathFactory.newInstance();
		final XPath xpath = xPathfactory.newXPath();
		XPathExpression expr = xpath.compile("//requestHandler[@class=\"solr.SearchHandler\" and @name=\"/select\"]");
		final Node requestHandler = (Node) expr.evaluate(docSchem, XPathConstants.NODE);

		if (requestHandler != null) {
			expr = xpath.compile("//lst[@name=\"appends\"]");
			final Node appends = (Node) expr.evaluate(requestHandler, XPathConstants.NODE);
			if (appends != null) {
				// Save the current searchHandler version
				final String oldSearchHandler = nodeToString(requestHandler);

				// Remove the manifoldCFSecurity
				requestHandler.removeChild(appends);

				// Replace the old searchHandler by the new one
				String configContent = getFileContent(fileToParse);
				final String newSearchHandler = nodeToString(requestHandler);
				configContent = configContent.replace(oldSearchHandler, newSearchHandler);

				// Save the file
				saveFile(fileToParse, configContent);
			}
		}

		return CodesReturned.ALLOK.getValue();
	}

	/**
	 * Convert XML Node to String
	 *
	 * @param node
	 *            the node to convert
	 * @return the String equivalent of the node
	 * @throws TransformerException
	 */
	private static String nodeToString(final Node node) throws TransformerException {
		final Transformer tf = TransformerFactory.newInstance().newTransformer();
		tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		tf.setOutputProperty(OutputKeys.INDENT, "yes");
		final Writer out = new StringWriter();
		tf.transform(new DOMSource(node), new StreamResult(out));
		return out.toString();
	}

	/**
	 * Get the file content as a String
	 *
	 * @param file
	 * @return the file content as a String
	 * @throws IOException
	 */
	private static String getFileContent(final File file) throws IOException {
		final Path filePath = Paths.get(file.getAbsolutePath());
		final Charset charset = StandardCharsets.UTF_8;
		return new String(Files.readAllBytes(filePath), charset);
	}

	private static void saveFile(final File file, final String fileContent) throws Exception {
		final Path configPath = Paths.get(file.getAbsolutePath());
		Files.write(configPath, fileContent.getBytes(StandardCharsets.UTF_8));
	}
}