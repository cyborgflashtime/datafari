package com.francelabs.datafari.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.francelabs.datafari.constants.CodesReturned;
import com.francelabs.datafari.servlets.admin.ModifyRealmLdap;
import com.google.common.base.Strings;

public class RealmLdapConfiguration {

	private File fileContext;

	private static RealmLdapConfiguration instance;

	private static final Logger logger = Logger.getLogger(ModifyRealmLdap.class);

	public static final String ATTR_CONNECTION_URL = "connectionURL";
	public static final String ATTR_CONNECTION_NAME = "connectionName";
	public static final String ATTR_CONNECTION_PW = "connectionPassword";
	public static final String ATTR_DOMAIN_NAME = "userBase";
	public static final String ATTR_SUBTREE = "userSubtree";
	private static final String FILE_NAME = "context.xml";

	private RealmLdapConfiguration(final String contextPath) {

		String webAppName;

		if (!Strings.isNullOrEmpty(contextPath)) {
			if (contextPath.startsWith(File.separator)) {
				// Remove the path separator (\ or //)
				webAppName = contextPath.substring(1);
			} else {
				webAppName = contextPath;
			}
		} else {
			// Set the default value
			webAppName = "Datafari";
		}

		String env = System.getenv("DATAFARI_HOME"); // Gets the directory of
														// installation if in
														// standard environment
		if (env == null) { // If in development environment (Eclipse WTP)
			env = ExecutionEnvironment.getDevExecutionEnvironment();
			// Should return something like PATH_TO_WORKSPACE/datafari/
			// Reference WebContent folder as already done for FacetConfig class

			fileContext = new File(env + "WebContent" + File.separator + "META-INF" + File.separator + FILE_NAME);
		} else {
			fileContext = new File(env + File.separator + "tomcat" + File.separator + "webapps" + File.separator + webAppName + File.separator
					+ "META-INF" + File.separator + FILE_NAME);
		}

	}

	private static RealmLdapConfiguration getInstance(final String contextPath) {
		if (instance == null) {
			instance = new RealmLdapConfiguration(contextPath);
		}
		return instance;
	}

	public static HashMap<String, String> getConfig(final HttpServletRequest request) throws SAXException, IOException, ParserConfigurationException {
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		final Document docSchem = dBuilder.parse(getInstance(request.getContextPath()).fileContext); // Parse
																										// the
																										// schema
		final NodeList fields = docSchem.getElementsByTagName("Realm");
		final HashMap<String, String> hashMap = new HashMap<String, String>();
		for (int i = 0; i < fields.getLength(); i++) {
			final NamedNodeMap attributes = fields.item(i).getAttributes();
			if (!attributes.getNamedItem("className").getNodeValue().equals("org.apache.catalina.realm.JNDIRealm"))
				continue;
			hashMap.put(ATTR_CONNECTION_URL, attributes.getNamedItem(ATTR_CONNECTION_URL).getNodeValue());
			hashMap.put(ATTR_CONNECTION_NAME, attributes.getNamedItem(ATTR_CONNECTION_NAME).getNodeValue());
			hashMap.put(ATTR_CONNECTION_PW, attributes.getNamedItem(ATTR_CONNECTION_PW).getNodeValue());
			hashMap.put(ATTR_DOMAIN_NAME, attributes.getNamedItem(ATTR_DOMAIN_NAME).getNodeValue());
			hashMap.put(ATTR_SUBTREE, attributes.getNamedItem(ATTR_SUBTREE).getNodeValue());
		}
		return hashMap;
	}

	public static int setConfig(final HashMap<String, String> h, final HttpServletRequest request)
			throws SAXException, IOException, ParserConfigurationException {
		if (h.containsKey(ATTR_CONNECTION_URL) && h.containsKey(ATTR_CONNECTION_NAME) && h.containsKey(ATTR_CONNECTION_PW)
				&& h.containsKey(ATTR_DOMAIN_NAME) && h.containsKey(ATTR_SUBTREE)) {
			final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			final File fileContext = getInstance(request.getContextPath()).fileContext;
			final Document docSchem = dBuilder.parse(fileContext); // Parse the
																	// schema
			final Element root = docSchem.getDocumentElement();
			final NodeList fields = root.getElementsByTagName("Realm");
			for (int i = 0; i < fields.getLength(); i++) {
				final Node item = fields.item(i);
				if (!item.getAttributes().getNamedItem("className").getNodeValue().equals("org.apache.catalina.realm.JNDIRealm"))
					continue;
				if (item.getNodeType() == Node.ELEMENT_NODE) {
					final Element realmConfiguration = (Element) item;
					realmConfiguration.setAttribute(ATTR_CONNECTION_URL, h.get(ATTR_CONNECTION_URL));
					realmConfiguration.setAttribute(ATTR_CONNECTION_NAME, h.get(ATTR_CONNECTION_NAME));
					realmConfiguration.setAttribute(ATTR_CONNECTION_PW, h.get(ATTR_CONNECTION_PW));
					realmConfiguration.setAttribute(ATTR_DOMAIN_NAME, h.get(ATTR_DOMAIN_NAME));
					realmConfiguration.setAttribute(ATTR_SUBTREE, h.get(ATTR_SUBTREE));
				}
				// write the content into xml file
				final TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer;
				try {
					transformer = transformerFactory.newTransformer();
					final DOMSource source = new DOMSource(docSchem);
					final StreamResult result = new StreamResult(fileContext);
					transformer.transform(source, result);
				} catch (final TransformerException e) {
					logger.error(e);
					return CodesReturned.GENERALERROR;
				}

			}
			return CodesReturned.ALLOK;
		} else {
			return CodesReturned.PARAMETERNOTWELLSET;
		}

	}
}
