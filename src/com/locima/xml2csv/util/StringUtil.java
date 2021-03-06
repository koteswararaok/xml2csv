package com.locima.xml2csv.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Utility methods for dealing with Strings.
 */
public class StringUtil {

	/**
	 * Used with {@link StringUtil#toStringList}.
	 *
	 * @param <T> the input object type that will be converted to a string.
	 */
	public interface IConverter<T> {
		/**
		 * Converts the <code>input</code> to a string.
		 *
		 * @param input an input object, may be null.
		 * @return a string that represents the <code>input</code> object.
		 */
		String convert(T input);
	}

	/**
	 * An empty, zero length, non-null string.
	 */
	public static final String EMPTY_STRING = "";

	public static final String LINE_SEPARATOR = System.getProperty("line.separator");

	public static final String NULL_OR_EMPTY_MESSAGE = "Must be a string containing at least one character.";

	/**
	 * Converts a list of values in to a single output line.
	 *
	 * @param fields the collection of strings that are the individual fields to output.
	 * @param fieldSeparator the character to use to separate all the values. Must not be null.
	 * @param wrapper a string to write before and after all the values. May be null (which means no wrapper written).
	 * @return a String, possibly empty, but never null.
	 */
	public static String collectionToString(List<?> fields, String fieldSeparator, String wrapper) {
		if (null == fields) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		if (null != wrapper) {
			sb.append(wrapper);
		}
		int size = fields.size();
		for (int i = 0; i < size; i++) {
			sb.append(StringUtil.escapeForCsv(fields.get(i)));
			if (i < (size - 1)) {
				sb.append(fieldSeparator);
			}
		}
		if (null != wrapper) {
			sb.append(wrapper);
		}
		return sb.toString();
	}

	/**
	 * Concatenates two arrays together to make one array.
	 *
	 * @param first the first array that will form the first elements of the concanenated array.
	 * @param second the second array that will form the last elements of the concanenated array.
	 * @param <T> the type of object used for all the arrays.
	 * @return an array formed of the elements in the first and second arrays.
	 */
	public static <T> T[] concatenate(T[] first, T[] second) {
		int aLen = first.length;
		int bLen = second.length;

		@SuppressWarnings("unchecked")
		T[] concatenated = (T[]) Array.newInstance(first.getClass().getComponentType(), aLen + bLen);
		System.arraycopy(first, 0, concatenated, 0, aLen);
		System.arraycopy(second, 0, concatenated, aLen, bLen);

		return concatenated;
	}

	/**
	 * Serialises and escapes any value so that it can be added to a CSV file. If the value contains a double-quote, CR, LF, comma or semi-colon, then
	 * the entire value is wrapped in double-quotes. Any instances of double quotes (") are replaced with two double-quotes.
	 *
	 * @param value Any value that can be converted to a String using a {@link Object#toString()}.
	 * @return A string suitable to be embedded in to a CSV file that will be read by RFC4180 compliant application or Microsoft Excel. If null is
	 *         passed, null is returned.
	 */
	public static String escapeForCsv(Object value) {
		// Handle either null inputs or toString() methods that return null.
		String inputString = value == null ? null : value.toString();
		if (inputString == null) {
			return null;
		}

		// Do we need to wrap the entire value in quotes?
		boolean quotesRequired = false;

		int inputLength = inputString.length();
		
		// Allocate a few extra bytes so we don't need to dynamically extend in the event of a quotes being required.
		final int extraCharsForQuoting = 3;
		StringBuffer retValue = new StringBuffer(inputLength + extraCharsForQuoting);

		for (int i = 0; i < inputLength; i++) {
			char ch = inputString.charAt(i);
			retValue.append(ch);
			if (ch == '\n' || ch == '\r' || ch == ',' || ch == ';' || ch == '\"') {
				quotesRequired = true;
				if (ch == '\"') {
					retValue.append(ch);
				}
			}
		}

		// Wrap the whole value in double quotes if we've found character that needed to be escaped.
		if (quotesRequired) {
			retValue.insert(0, "\"");
			retValue.append('\"');
		}

		return retValue.toString();
	}

	/**
	 * Returns true if the passed string is either null or has zero length.
	 *
	 * @param s the string to test.
	 * @return true if the passed string is either null or has zero length, false otherwise.
	 */
	public static boolean isNullOrEmpty(String s) {
		return (s == null) || (s.length() == 0);
	}

	/**
	 * Converts the passed collection to a string ready for output to a CSV file. Each element of <code>inputCollection</code> will have its
	 * {@link Object#toString()} method called to get an output value.
	 *
	 * @param inputCollection a collection of objects.
	 * @param <T> the type of object contained within <code>inputCollection</code>.
	 * @return a string in CSV format, ready for output in to a CSV file.
	 */
	public static <T> String toCsvRecord(Collection<T> inputCollection) {
		StringUtil.IConverter<T> genericConverter = new StringUtil.IConverter<T>() {

			@Override
			public String convert(T input) {
				if (input == null) {
					return EMPTY_STRING;
				} else {
					return StringUtil.escapeForCsv(input.toString());
				}
			}
		};
		return toString(inputCollection, ",", genericConverter);
	}

	/**
	 * Converts the collection passed to set of strings separated by <code>fieldSeparator</code>.
	 *
	 * @param inputCollection the collection to convert to a list of strings.
	 * @param fieldSeparator the string inserted between each element of the collection.
	 * @param converter a method that converts each element of <code>inputCollection</code> to a string.
	 * @param <T> the type of object in <code>inputCollection</code>.
	 * @return a string
	 */
	public static <T> String toString(Collection<T> inputCollection, String fieldSeparator, IConverter<T> converter) {
		StringBuffer sb = new StringBuffer();
		Iterator<T> iterator = inputCollection.iterator();
		while (iterator.hasNext()) {
			String convertedValue = converter.convert(iterator.next());
			sb.append(convertedValue);
			if (iterator.hasNext()) {
				sb.append(fieldSeparator);
			}
		}
		return sb.toString();
	}

	/**
	 * Converts an array of objects to a comma separated list within a single String, with each member of the array effectively being passed to
	 * {@link String#valueOf(Object)}.
	 *
	 * @param objects the array to convert, may be null or empty, in which case an empty string is returned.
	 * @return a string, possibly empty, of all the members of the passed array, separated by commas.
	 */
	public static String toString(Object[] objects) {
		if ((objects == null) || (objects.length == 0)) {
			return EMPTY_STRING;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < objects.length; i++) {
			sb.append(objects[i]);
			if (i < (objects.length - 1)) {
				sb.append(",");
			}
		}
		return sb.toString();
	}

	/**
	 * Converts the collection passed to a list of strings.
	 *
	 * @param inputCollection the collection to convert to a list of strings.
	 * @param converter a method that converts each element of <code>inputCollection</code> to a string.
	 * @param <T> the type of object in <code>inputCollection</code>.
	 * @return a list of strings.
	 */
	public static <T> List<String> toStringList(Collection<T> inputCollection, IConverter<T> converter) {
		List<String> output = new ArrayList<String>(inputCollection.size());
		for (T t : inputCollection) {
			output.add(converter.convert(t));
		}
		return output;
	}

	/**
	 * Converts a String array to a {@link List}.
	 *
	 * @param strings the input array. If null returns an empty list.
	 * @return a possibly empty list, never null.
	 */
	public static List<String> toStringList(String[] strings) {
		if (strings == null) {
			return new ArrayList<String>(0);
		}
		List<String> list = new ArrayList<String>(strings.length);
		for (String s : strings) {
			list.add(s);
		}
		return list;
	}

	/**
	 * Prevents instantiation, this is a utility class with only static methods.
	 */
	private StringUtil() {
	}
}
