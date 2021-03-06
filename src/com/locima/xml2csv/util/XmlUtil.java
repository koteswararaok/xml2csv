package com.locima.xml2csv.util;

import java.io.File;
import java.util.Map;

import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.s9api.OccurrenceIndicator;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathExecutable;
import net.sf.saxon.s9api.XdmNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.locima.xml2csv.BugException;
import com.locima.xml2csv.XMLException;
import com.locima.xml2csv.configuration.XPathValue;
import com.locima.xml2csv.extractor.DataExtractorException;

/**
 * Utility methods to make dealing with Saxon easier.
 */
public class XmlUtil {

	private static final Logger LOG = LoggerFactory.getLogger(XmlUtil.class);

	/**
	 * Managed a singleton Saxon Processor instance.
	 * <p>
	 * This is required because if you don't you'll get the following exception when attempting to evaluate XPath expressions:
	 * <code>Caused by: net.sf.saxon.s9api.SaxonApiException: Supplied node must be built using the same or a compatible Configuration
	 * at net.sf.saxon.s9api.XPathSelector.setContextItem(XPathSelector.java:62</code>.
	 */
	private static Processor processor = new Processor(false);

	/**
	 * Creates an Saxon executable XPath expression based on the XPath and a set of namespace prefix to URI mappings.
	 *
	 * @param namespaceMappings A mapping of namespace prefix to URI mappings. May be null if there are no namespaces involved.
	 * @param xPathExpression An XPath expression to compile. Must be valid XPath.
	 * @param parameters a set of parameters to declare in the compiled XPath.
	 * @return a Saxon executable XPath expression, never null.
	 * @throws XMLException If there are any problems compiling <code>xPathExpression</code>.
	 */
	public static XPathExecutable createXPathExecutable(Map<String, String> namespaceMappings, String xPathExpression, String... parameters)
					throws XMLException {
		// Need to construct a new compiler because the set of namespaces is (potentially) unique to the expression.
		// We could cache a set of compilers, but I doubt it's worth it.
		XPathCompiler xPathCompiler = getProcessor().newXPathCompiler();
		for (String parameter : parameters) {
			try {
				xPathCompiler.declareVariable(new QName(parameter), ItemType.STRING, OccurrenceIndicator.ZERO_OR_MORE);
			} catch (SaxonApiException e) {
				throw new BugException(e, "Unable to declare variable for \"%s\" variable to Saxon XPath Compiler", parameter);
			}
		}
		if (namespaceMappings != null) {
			for (Map.Entry<String, String> entry : namespaceMappings.entrySet()) {
				String prefix = entry.getKey();
				String uri = entry.getValue();
				xPathCompiler.declareNamespace(prefix, uri);
			}
		}

		try {
			XPathExecutable xPath = xPathCompiler.compile(xPathExpression);
			return xPath;
		} catch (SaxonApiException e) {
			throw new XMLException(e, "Unable to compile invalid XPath: %s", xPathExpression);
		}
	}

	/**
	 * Creates an executable XPath expression based on the XPath and a set of namespace prefix to URI mappings.
	 *
	 * @param namespaceMappings A mapping of namespace prefix to URI mappings. May be null if there are no namespaces involved.
	 * @param xPathExpression An XPath expression to compile. Must be valid XPath or null. If null then null is returned.
	 * @param variableNames a set of parameters to declare in the compiled XPath.
	 * @return a Saxon executable XPath expression, or null if <code>xPathExpression</code> is null.
	 * @throws XMLException If there are any problems compiling <code>xPathExpression</code>.
	 */
	public static XPathValue createXPathValue(Map<String, String> namespaceMappings, String xPathExpression, String... variableNames)
					throws XMLException {
		return xPathExpression == null ? null : new XPathValue(xPathExpression, createXPathExecutable(namespaceMappings, xPathExpression,
						variableNames));
	}

	/**
	 * Creates an executable XPath expression based on the XPath where no XML namespaces are referenced.
	 *
	 * @param xPathExpression An XPath expression to compile. Must be valid XPath or null. If null then null is returned.
	 * @param variableNames variables to declare to the XPath compiler to help with the execution later.
	 * @return a Saxon executable XPath expression, or null if <code>xPathExpression</code> is null.
	 * @throws XMLException If there are any problems compiling <code>xPathExpression</code>.
	 */
	public static XPathValue createXPathValue(String xPathExpression, String... variableNames) throws XMLException {
		return createXPathValue(null, xPathExpression, variableNames);
	}

	/**
	 * Returns the singleton instance.
	 *
	 * @return instance of the Saxon processor manager, never returns null.
	 */
	public static Processor getProcessor() {
		return processor;
	}

	/**
	 * Loads the XML file specified and returns as a Saxon XML document.
	 *
	 * @param xmlFile The XML file to read data from, must be a valid file.
	 * @return The loaded XML document, never returns null.
	 * @throws DataExtractorException If an error occurs during extraction of data from the XML.
	 */
	public static XdmNode loadXmlFile(File xmlFile) throws DataExtractorException {
		try {
			DocumentBuilder db = getProcessor().newDocumentBuilder();
			LOG.debug("Loading and parsing XML file {}", xmlFile.getAbsolutePath());
			XdmNode document = db.build(xmlFile);
			LOG.info("XML file {} loaded succesfully", xmlFile.getAbsolutePath());
			return document;
		} catch (SaxonApiException e) {
			throw new DataExtractorException(e, "Unable to read XML file %s", xmlFile.getAbsolutePath());
		}
	}

	/**
	 * Prevents instantiation.
	 */
	private XmlUtil() {
	}

}
