package com.locima.xml2csv.extractor;

import com.locima.xml2csv.XMLException;
import com.locima.xml2csv.configuration.IMappingContainer;
import com.locima.xml2csv.configuration.Mapping;
import com.locima.xml2csv.configuration.MappingList;
import com.locima.xml2csv.configuration.MultiValueBehaviour;
import com.locima.xml2csv.configuration.NameFormat;
import com.locima.xml2csv.configuration.PivotMapping;
import com.locima.xml2csv.configuration.XPathValue;
import com.locima.xml2csv.util.XmlUtil;

public class ConfigBuilders {

	public static Mapping createLazyMapping(MappingList parent, String xPath, int groupNumber) throws XMLException {
		return com.locima.xml2csv.TestHelpers.addMapping(parent, xPath, groupNumber, xPath);
	}

	public static MappingList createMappingList(String xPath, int groupNumber, MultiValueBehaviour mvb) throws XMLException {
		MappingList ml = new MappingList();
		ml.setName(xPath);
		ml.setGroupNumber(groupNumber);
		ml.setMultiValueBehaviour(mvb);
		ml.setMappingRoot(XmlUtil.createXPathValue(xPath));
		return ml;
	}

	/**
	 * Creates a new instance of a Pivot Mapping object.
	 *
	 * @param pivotMappingName The name given to this pivot mapping, if top-level will be used to generate the output file name.
	 * @param keyXPath the XPath expression that, when executed relative to the mapping root of the parent, will yield the base name of the fields
	 *            that this pivot mapping will return. Must not be null.
	 * @param valueXPath the XPath expression that, when executed relative to the node yielding the key, will yield the value for that key. Must not
	 *            be null.
	 * @param nameFormat the format to be used for the {@link Mapping} instance that this method creates.
	 * @param multiValueBehaviour defines what should happen when multiple values are found for a single evaluation of a single field wtihin this
	 *            mapping.
	 * @param groupNumber the logical group number of this mapping container.
	 * @param parent The parent of this mapping container. Must be null if this is a top level mapping container.
	 * @throws XMLException If there was problem compiling the expression (for example, if the XPath is invalid).
	 */
	public static PivotMapping createPivotMapping(IMappingContainer parent, String pivotMappingName, XPathValue mappingRoot, XPathValue kvPairRoot, XPathValue keyXPath,
					XPathValue valueXPath, NameFormat nameFormat, int groupNumber, MultiValueBehaviour multiValueBehaviour) throws XMLException {
		PivotMapping pivotMapping = new PivotMapping();
		pivotMapping.setParent(parent);
		pivotMapping.setMappingName(pivotMappingName);
		pivotMapping.setMappingRoot(mappingRoot);
		pivotMapping.setKVPairRoot(kvPairRoot);
		pivotMapping.setKeyXPath(keyXPath);
		pivotMapping.setValueXPath(valueXPath);
		pivotMapping.setNameFormat(nameFormat);
		pivotMapping.setGroupNumber(groupNumber);
		pivotMapping.setMultiValueBehaviour(multiValueBehaviour);
		return pivotMapping;
	}

	/**
	 * Creates a new instance of a Pivot Mapping object.
	 *
	 * @param pivotMappingName The name given to this pivot mapping, if top-level will be used to generate the output file name.
	 * @param keyXPath the XPath expression that, when executed relative to the mapping root of the parent, will yield the base name of the fields
	 *            that this pivot mapping will return. Must not be null.
	 * @param valueXPath the XPath expression that, when executed relative to the node yielding the key, will yield the value for that key. Must not
	 *            be null.
	 * @param nameFormat the format to be used for the {@link Mapping} instance that this method creates.
	 * @param multiValueBehaviour defines what should happen when multiple values are found for a single evaluation of a single field wtihin this
	 *            mapping.
	 * @param groupNumber the logical group number of this mapping container.
	 * @param parent The parent of this mapping container. Must be null if this is a top level mapping container.
	 * @throws XMLException If there was problem compiling the expression (for example, if the XPath is invalid).
	 */
	public static PivotMapping createPivotMapping(IMappingContainer parent, String pivotMappingName, String rootXPath, String kvPairXPath, String keyXPath,
					String valueXPath, NameFormat nameFormat, int groupNumber, MultiValueBehaviour multiValueBehaviour) throws XMLException {
		return createPivotMapping(parent, pivotMappingName, XmlUtil.createXPathValue(rootXPath), 
						XmlUtil.createXPathValue(kvPairXPath), XmlUtil.createXPathValue(keyXPath),
						XmlUtil.createXPathValue(valueXPath), nameFormat, groupNumber, multiValueBehaviour);
	}

}
