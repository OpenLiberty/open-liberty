/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.web;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.websphere.simplicity.log.Log;

public class WebRequestUtils {

    public static Class<?> thisClass = WebRequestUtils.class;

    final static String REGEX_PATTERN_QUOTE_START = "\\Q";
    final static String REGEX_PATTERN_QUOTE_END = "\\E";

    /**
     * Builds a URL query string using the provided map of parameter names and values.
     *
     * @param paramMap
     *            Maps request parameter names to their lists of values.
     * @return
     */
    public String buildUrlQueryString(Map<String, List<String>> paramMap) {
        StringBuilder queryString = new StringBuilder();
        if (paramMap != null) {
            for (Entry<String, List<String>> param : paramMap.entrySet()) {
                queryString = addParameterToQueryString(queryString, param.getKey(), param.getValue());
            }
        }
        return queryString.toString();
    }

    /**
     * Adds the provided parameter and all of its values to the specified query string. If the query string is non-empty and does
     * not already end with a query parameter delimiter, one will be added before appending the new parameter and values.
     */
    public StringBuilder addParameterToQueryString(StringBuilder queryString, String key, List<String> values) {
        if (queryString == null) {
            queryString = new StringBuilder();
        }
        StringBuilder builtQueryString = createQueryStringForParameterAndValues(key, values);
        if (isParameterDelimiterNeeded(queryString) && builtQueryString.length() > 0) {
            // Only append the "&" character if the resulting parameter's query string is non-empty
            queryString.append("&");
        }
        return queryString.append(builtQueryString);
    }

    /**
     * Builds a properly URL-encoded query string value for the provided parameter and each of its values. If there are multiple
     * values in the provided list, every value will be added to the result with each instance separated by the appropriate
     * request parameter delimiter ({@code "&"}).
     *
     * @param key
     *            If not null, the key will be URL-encoded and added to the result. If null, an empty value will be returned.
     * @param values
     *            If not null, each value will be URL-encoded and added as a key-value pair in the result. If null, no values
     *            will be added to the result. Note: If the key was not null and the values are null, the key will appear alone
     *            in the result.
     */
    public StringBuilder createQueryStringForParameterAndValues(String key, List<String> values) {
        StringBuilder paramAndValues = new StringBuilder();
        if (key == null) {
            return paramAndValues;
        }
        try {
            paramAndValues.append(URLEncoder.encode(key, "UTF-8"));
            if (values != null) {
                paramAndValues.append(addNonNullKeyAndValuesToParameterString(key, values));
            }
        } catch (UnsupportedEncodingException e) {
            // Do nothing - UTF-8 must be supported
        }
        return paramAndValues;
    }

    private StringBuilder addNonNullKeyAndValuesToParameterString(String key, List<String> values) throws UnsupportedEncodingException {
        StringBuilder paramAndValues = new StringBuilder();
        boolean isFirst = true;
        for (String value : values) {
            if (value == null) {
                continue;
            }
            if (!isFirst) {
                paramAndValues.append("&").append(URLEncoder.encode(key, "UTF-8"));
            }
            paramAndValues.append("=").append(URLEncoder.encode(value, "UTF-8"));
            if (isFirst) {
                isFirst = false;
            }
        }
        return paramAndValues;
    }

    /**
     * Returns whether the query string already ends with a '&' character.
     */
    public boolean isParameterDelimiterNeeded(StringBuilder queryString) {
        return (queryString != null && queryString.length() > 0 && queryString.charAt(queryString.length() - 1) != '&');
    }

    /**
     * Creates a valid regular expression that can be used to match the provided value. The regular expression is the URL encoded
     * value with any single space characters replaced by a regex group that matches either "+" or "%20". Both encodings of the
     * single space character are valid for URI values, so the regex created by this method will ensure that either occurrence
     * will match.
     * <p>
     * Example: The input {@code "a * char"} will create the result {@code "\Qa\E(\+|%20)\Q*\E(\+|%20)\Qchar\E"}. The
     * {@code "\Q"} and {@code "\E"} strings denote quoted values that will be matched as a literal in a regular expression. That
     * means the {@code "*"} character will NOT be interpreted as the "zero-or-more" meta-character, but instead will be
     * interpreted as the literal {@code "*"} character. Each space character is replaced by an OR group {@code "(\+|%20)"} to
     * denote that either the {@code "+"} or {@code "%20"} strings are acceptable to match where a single space appears.
     */
    public String getRegexSearchStringForUrlQueryValue(String value) {
        String thisMethod = "getRegexSearchStringForUrlQueryValue";
        if (value == null) {
            return "";
        }

        String replacementString = "#REPLACEME#";
        String validSpaceRegex = "(?:\\\\+|%20)";

        String result = getNormalizedUrlEncodedString(value, replacementString);
        result = getRegexFromNormalizedUrlEncodedString(result, replacementString, validSpaceRegex);

        Log.info(thisClass, thisMethod, "Created new parameter regex: " + result);
        return result;
    }

    /**
     * URL-encodes the provided input string and replaces all "+" and "%20" strings with the specified replacement string.
     */
    String getNormalizedUrlEncodedString(String input, String spaceReplacementString) {
        String result = getUrlEncodedValue(input);
        if (spaceReplacementString == null) {
            return result;
        }
        result = result.replaceAll("\\+", spaceReplacementString).replaceAll("%20", spaceReplacementString);
        return result;
    }

    String getUrlEncodedValue(String input) {
        if (input == null) {
            return "";
        }
        try {
            input = URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Do nothing - UTF-8 must be supported
        }
        return input;
    }

    /**
     * Takes a normalized URL encoded string created by this class and replaces all occurrences of one string with the specified
     * replacement string. Non-matching segments of the input string will be enclosed in regular expression pattern quote strings
     * ({@code "\Q"} and {@code "\E"}) to ensure those segments are properly escaped. This is to ensure that characters like "*",
     * "(", and "?" will be treated as literals instead of as regular expression meta-characters.
     *
     * @param input
     * @param stringToReplace
     * @param replacementString
     * @return
     */
    String getRegexFromNormalizedUrlEncodedString(String input, String stringToReplace, String replacementString) {
        if (input.equals(stringToReplace)) {
            return input.replaceAll("^" + stringToReplace + "$", replacementString);
        }

        input = replaceStringAtStartOfString(input, stringToReplace, replacementString);
        input = replaceStringAtEndOfString(input, stringToReplace, replacementString);
        input = replaceStringWithinString(input, stringToReplace, replacementString);
        return input;
    }

    /**
     * Replaces the specified string in the provided input with the provided replacement string if the input starts with that
     * string. The string is replaced and a regular expression start quote sequence ({@code "\\Q"}) is added immediately after
     * the replacement string. If the input doesn't start with the string to replace, a regex start quote sequence is simply
     * prepended to the input. This ensures that the rest of the input is treated as a literal string and any regex
     * meta-characters that happen to be in the input are treated as literals.
     */
    String replaceStringAtStartOfString(String input, String stringToReplace, String replacementString) {
        if (input.startsWith(stringToReplace)) {
            // Extra "\\" is needed to ensure the replaceAll() call keeps the regex quote pattern escaped
            return input.replaceAll("^" + stringToReplace, replacementString + "\\" + REGEX_PATTERN_QUOTE_START);
        }
        return REGEX_PATTERN_QUOTE_START + input;
    }

    /**
     * Replaces the specified string in the provided input with the provided replacement string if the input ends with that
     * string. The string is replaced and a regular expression end quote sequence ({@code "\\E"}) is added immediately before the
     * replacement string. If the input doesn't end with the string to replace, a regex end quote sequence is simply appended to
     * the input. This ensures that the rest of the input is treated as a literal string and any regex meta-characters that
     * happen to be in the input are treated as literals.
     */
    String replaceStringAtEndOfString(String input, String stringToReplace, String replacementString) {
        if (input.endsWith(stringToReplace)) {
            // Extra "\\" is needed to ensure the replaceAll() call keeps the regex quote pattern escaped
            return input.replaceAll(stringToReplace + "$", "\\" + REGEX_PATTERN_QUOTE_END + replacementString);
        }
        return input + REGEX_PATTERN_QUOTE_END;
    }

    /**
     * Replaces the specified string in the provided input with the provided replacement string everywhere it occurs within the
     * input. The string is replaced and a regular expression end quote sequence ({@code "\\E"}) is added immediately before the
     * replacement string and a regular expression start quote sequence ({@code "\\Q"}) is added immediately after the
     * replacement string. If the input doesn't contain the string to replace, the result should be identical to the input. The
     * regex quote sequences ensure that the rest of the input is treated as a literal string and any regex meta-characters that
     * happen to be in the input are treated as literals.
     */
    String replaceStringWithinString(String input, String stringToReplace, String replacementString) {
        input = input.replaceAll(stringToReplace, "\\" + REGEX_PATTERN_QUOTE_END + replacementString + "\\" + REGEX_PATTERN_QUOTE_START);

        // Empty strings that are regex-quoted ("\\Q\\E") can simply be replaced by empty strings since the quote pattern is unnecessary
        input = input.replaceAll("\\" + REGEX_PATTERN_QUOTE_START + "\\" + REGEX_PATTERN_QUOTE_END, "");

        return input;
    }

}
