package com.locima.xml2csv.extractor;

import com.locima.xml2csv.util.EqualsUtil;

/**
 * A simple name/value pair of field name mapped to field value within a single output record.
 */
public class ExtractedField {

	private final String fieldName;
	private final String value;

	public ExtractedField(String fieldName, String fieldValue) {
		this.fieldName = fieldName;
		this.value = fieldValue;
	}

	@Override
	public boolean equals(Object obj) {
		if ((obj == null) || (!(obj instanceof ExtractedField))) {
			return false;
		}
		ExtractedField that = (ExtractedField) obj;
		return EqualsUtil.areEqual(this.fieldName, that.fieldName) && EqualsUtil.areEqual(this.value, that.value);
	}

	public String getFieldName() {
		return this.fieldName;
	}

	public String getFieldValue() {
		return this.value;
	}

	@Override
	public int hashCode() {
		return this.fieldName.hashCode() ^ this.value.hashCode();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("EF(");
		sb.append(this.fieldName);
		sb.append(',');
		sb.append(this.value);
		sb.append(')');
		return sb.toString();

	}
}
