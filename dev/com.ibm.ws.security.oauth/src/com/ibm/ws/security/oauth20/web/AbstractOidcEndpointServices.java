/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.util.CharsetRange;
import com.ibm.ws.security.oauth20.util.DateUtil;
import com.ibm.ws.security.oauth20.util.MediaRange;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;
import com.ibm.ws.security.oauth20.util.StringUtil;

/**
 *
 */
public abstract class AbstractOidcEndpointServices {
    protected static final String FORWARD_SLASH = "/";
    protected static final String BACKWARDS_SLASH = "\\";
    protected static final String COLON = ":";
    protected static final String COLON_SLASH_SLASH = "://";
    protected static final String UTF_8 = "UTF-8"; //$NON-NLS-1$
    private static final char AMPERSAND = '&';
    private static final char EQUALS = '=';
    private static final String EMPTY_STRING = "";
    private static final String URL_ENCODED_SPACE = "%20"; //$NON-NLS-1$

    public static final String CT = "Content-Type";
    public static final String CT_APPLICATION_JSON = "application/json";
    public static final String CT_APPLICATION_JSON_AND_UTF8 = "application/json;charset=UTF-8";
    protected static final String CT_WILDCARD = "*/*";

    private static final String HDR_IF_MATCH = "If-Match";
    private static final String HDR_IF_NONE_MATCH = "If-None-Match";
    private static final String HDR_IF_MODIFIED_SINCE = "If-Modified-Since";
    private static final String HDR_IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
    public static final String HDR_WWW_AUTHENTICATE = "WWW-Authenticate";

    private static final String HDR_ACCEPT = "Accept";
    private static final String HDR_ACCEPT_CHARSET = "Accept-Charset"; //$NON-NLS-1$
    protected static final String HDR_ETAG = "ETag";

    protected static final String HDR_VALUE_PUBLIC = "public";
    protected static final String HDR_VALUE_PRIVATE = "private";
    private static final String HDR_VALUE_MAX_AGE = "max-age";

    protected static final int HTTP_DEFAULT_PORT = 80;
    protected static final int HTTP_DEFAULT_SECURE_PORT = 443;

    protected static final String HTTP_METHOD_GET = "GET";
    protected static final String HTTP_METHOD_HEAD = "HEAD";
    protected static final String HTTP_METHOD_POST = "POST";
    protected static final String HTTP_METHOD_PUT = "PUT";
    protected static final String HTTP_METHOD_DELETE = "DELETE";

    protected static final String ALG_MD5 = "MD5";

    private static TraceComponent tc = Tr.register(AbstractOidcEndpointServices.class);

    /**
     * Parses a URI-style query string into a map of key/value pairs.
     *
     * The query string takes on the format of "&foo=1&bar=2&zotz", where parameters in the query are separated by '&' and name/values within a parameter are
     * separated with '='. A parameter may exist without a value. The name/value pair must be encoded if they contain illegal characters.
     * <p>
     *
     * No encoding/decoding of the parameters happens as a side-effect of the parsing.
     *
     * @param query
     *            The URI-style query string. May be <code>null</code> or empty. If <code>null</code> or empty, an empty map is returned. The string may start
     *            with '&' or not.
     *
     * @return The map of key/value pairs. The keys are {@link String} and the values are arrays of {@link String}. Never <code>null</code>.
     */
    protected static Map<String, String[]> parseQueryParameters(String query) {
        return parseQueryParameters(query, false);
    }

    /**
     * Parses a URI-style query string into a map of key/value pairs.
     *
     * The query string takes on the format of "&foo=1&bar=2&zotz", where parameters in the query are separated by '&' and name/values within a parameter are
     * separated with '='. A parameter may exist without a value. The name/value pair must be encoded if they contain illegal characters.
     * <p>
     *
     * @param query
     *            The URI-style query string. May be <code>null</code> or empty. If <code>null</code> or empty, an empty map is returned. The string may start
     *            with '&' or not.
     *
     * @param decode
     *            Whether or not to decode the name/values (per the URI query spec in RFC 3986) in the referenced <code>parm</code> prior to inserting them into
     *            <code>map</code>. <code>true</code> to decode those name/values, <code>false</code>
     *
     * @return The map of key/value pairs. The keys are {@link String} and the values are arrays of {@link String}. Never <code>null</code>.
     */
    protected static Map<String, String[]> parseQueryParameters(String query, boolean decode) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Query String is " + query);
        }

        HashMap<String, ArrayList<String>> map = new HashMap<String, ArrayList<String>>();

        if (query == null || query.length() == 0) {
            return new HashMap<String, String[]>();
        }

        if (query.charAt(0) != AMPERSAND) {
            query = AMPERSAND + query;
        }

        int mainNdx = 0;
        while (mainNdx < query.length()) {
            /*
             * The first character is always '&' - bump past that
             */
            mainNdx++;

            // Get the next name=value parameter pair, which ends with '&' or is last.
            String parm = EMPTY_STRING;
            int startNdx = mainNdx;
            int endNdx = query.indexOf(AMPERSAND, startNdx);

            if (endNdx == -1) {
                // If there is no '&' then we're at the end of the line
                parm = query.substring(startNdx);
                mainNdx = query.length();
            } else {
                parm = query.substring(startNdx, endNdx);
                mainNdx = endNdx;
            }
            loadParmInMap(parm, map, decode);
        }

        /*
         * Convert the ArrayLists to String[] to be compatible with other APIs that deal with query parameters
         */
        HashMap<String, String[]> newMap = new HashMap<String, String[]>(map.size());

        Iterator<String> iter = map.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            ArrayList<String> list = map.get(key);
            String strings[] = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                strings[i] = list.get(i);
            }
            newMap.put(key, strings);
        }

        return newMap;
    }

    /**
     * Splits a <name>=<value> formatted string into key/value pairs, and inserts them into the referenced HashMap. Keys are for map are Strings - values are an
     * ArrayList. Values for duplicate keys are stored in the ArrayList
     *
     * @param parm
     *            The name/value pair string. Names can exist without values - the following forms are valid syntax: "name=value"; "name"; "name=". Must not be
     *            <code>null</code>.
     *
     * @param map
     *            A hashmap of String/ArrayList pairs. Must not be <code>null</code>.
     *
     * @param decode
     *            Whether or not to decode the name/values (per the URI query spec in RFC 3986) in the referenced <code>parm</code> prior to inserting them into
     *            <code>map</code>. <code>true</code> to decode those name/values, <code>false</code>
     */
    private static void loadParmInMap(String parm, HashMap<String, ArrayList<String>> map, boolean decode) {
        int equals = parm.indexOf(EQUALS);
        String key = null;
        String value = EMPTY_STRING;
        if (equals == -1) {
            key = parm;
        } else if (equals == parm.length() - 1) {
            key = parm.substring(0, equals);
        } else {
            key = parm.substring(0, equals);
            value = parm.substring(equals + 1);
        }

        if (decode) {
            key = decode(key);
            value = decode(value);
        }

        ArrayList<String> mapValue = map.get(key);
        if (mapValue == null) {
            mapValue = new ArrayList<String>();
            map.put(key, mapValue);
        }

        mapValue.add(value);
    }

    /**
     * Decodes any encoded characters considered illegal in URIs to their unescaped equivalents.
     *
     * @param str
     *            The string to escape. Must not be <code>null</code>.
     *
     * @return The new string with unescaped characters. If the source contains no illegal characters, the original is returned. If decoding errors are
     *         encountered (which can't be imagined), the error message is returned.
     */
    protected static String decode(String str) {

        if (str == null) {
            throw new IllegalArgumentException("str must not be null"); //$NON-NLS-1$
        }

        str = str.replace(URL_ENCODED_SPACE, "+"); //$NON-NLS-1$
        try {
            return URLDecoder.decode(str, UTF_8);
        } catch (UnsupportedEncodingException e) {
            String msg = String.format("An encoding error occurred during the %s encoding of string \"%s\". The exception message is \"%s\".", UTF_8, str, e.getMessage()); //$NON-NLS-1$
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * Constructs a cache control header with the format:
     * Cache-Control: public|private, max-age=xxx
     * @param isAllowIntermediateCaching <code>true</code> if the directive is to allow intermediate caches (proxies) to cache the response,
     * <code>false</code> otherwise
     * @param maxAge maximum age
     * @return header value
     */
    protected static String constructCacheControlHeaderWithMaxAge(boolean isPublic, String maxAge) {
        String type = (isPublic ? HDR_VALUE_PUBLIC : HDR_VALUE_PRIVATE);
        String headerValue = String.format("%s, %s=%s", type, HDR_VALUE_MAX_AGE, maxAge); //$NON-NLS-1$
        return headerValue;
    }

    protected static String generateUUID() {
        return UUID.randomUUID().toString().replace("-", ""); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static JsonArray getSlashTerminated(JsonArray values) {
        JsonArray slashValues = new JsonArray();

        if (values != null && values.size() > 0) {
            for (JsonElement uriEle : values) {
                String uriString = uriEle.getAsString();
                slashValues.add(new JsonPrimitive(AbstractOidcEndpointServices.addTrailingSlash(uriString)));
            }
        }

        return slashValues;
    }

    /**
     * Normalizes the referenced <code>uri</code> by adding a forward slash character ("/"), if one doesn't already exist. If the referenced uri ends with a
     * backward slash ("\"), that is replaced with a forward slash.
     *
     * @param uri
     *            The string to parse. May be <code>null</code>. If <code>null</code>, <code>null</code> is returned.
     *
     * @return A copy of the references uri with a trailing slash appended. Will be <code>null</code> if <code>uri</code> is <code>null</code>.
     */
    public static String addTrailingSlash(String uri) {
        uri = trimTrailingSlash(uri);
        if (uri == null) {
            return uri;
        }

        return uri + FORWARD_SLASH;
    }

    protected static String addLeadingSlash(String uri) {
        uri = trimLeadingSlash(uri);
        if (uri == null) {
            return uri;
        }

        return FORWARD_SLASH + uri;
    }

    protected static String trimSlashes(String uri) {
        return trimTrailingSlash(trimLeadingSlash(uri));
    }

    protected static String trimLeadingSlash(String uri) {

        if (uri == null)
            return uri;

        /*
         * Remove the leading slash if it exists. Canonicalize it so that typenames are fully-qualified.
         */
        uri = uri.trim();
        if (uri.startsWith(FORWARD_SLASH) || uri.startsWith(BACKWARDS_SLASH)) {
            if (uri.length() > 1)
                return uri.substring(1);

            uri = "";
        }

        return uri;
    }

    protected static String trimTrailingSlash(String uri) {
        if (uri == null)
            return uri;

        uri = uri.trim();
        if (uri.endsWith(FORWARD_SLASH) || uri.endsWith(BACKWARDS_SLASH)) {
            final int len = uri.length();
            if (len > 1)
                return uri.substring(0, len - 1);

            uri = "";
        }

        return uri;
    }

    protected static void validateContentType(HttpServletRequest request, String contentType) throws OidcServerException {
        // Verify valid request Content-Type
        if (!isValidContentType(request, contentType)) {
            String description = String.format("The request must contain content-type of \"%s\"", contentType); //$NON-NLS-1$
            throw new OidcServerException(description, OIDCConstants.ERROR_INVALID_REQUEST, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        }
    }

    private static boolean isValidContentType(HttpServletRequest request, String contentType) {
        return (request.getContentType() != null && request.getContentType().startsWith(contentType));
    }

    /**
     * Returns whether or not a JSON response is acceptable to the specified request. If it is not, the response is committed with an appropriate error message,
     * and <code>false</code> is returned. Otherwise, <code>true</code> is returned and the response is untouched.
     *
     * @param request
     *            The request to inspect. Must not be <code>null</code>.
     *
     * @throws IOException
     *             Thrown on response write errors.
     */
    protected static void validateJsonAcceptable(HttpServletRequest request) throws OidcServerException {

        if (isMimeTypeAcceptable(request, CT_APPLICATION_JSON, null) == false) {
            String description = String.format("The request does not allow for a response of media type \"%s\"", CT_APPLICATION_JSON);
            throw new OidcServerException(description, OIDCConstants.ERROR_INVALID_REQUEST, HttpServletResponse.SC_NOT_ACCEPTABLE);
        }

        if (isCharsetAcceptable(request, UTF_8) == false) {
            String description = String.format("The request does not allow for a response that not charset \"%s\"", UTF_8);
            throw new OidcServerException(description, OIDCConstants.ERROR_INVALID_REQUEST, HttpServletResponse.SC_NOT_ACCEPTABLE);
        }
    }

    /**
     * Tests whether or not the specified <code>mimeType</code> is acceptable
     * to the specified <code>request</code>, as indicated in that requests
     * accept headers.
     *
     * @param request The http request. Cannot be <code>null</code>.
     *
     * @param mimeType The mime type to test.  Cannot be <code>null</code> or
     * empty.
     *
     * @param parameters The optional mime type parameters. May be <code>null</code>.
     *
     * @return <code>true</code> if the specified mimeType and optional parameters
     * are acceptable to the request, as specified in request headers. If there is
     * no accept header present in the request, or the header value is empty,
     * then this function returns <code>true</code> (it is assumed that all
     * mediaTypes are acceptable).
     */
    private static boolean isMimeTypeAcceptable(HttpServletRequest request, String mimeType, Collection<String> parameters) {
        if (request == null)
            throw new IllegalArgumentException("request must not be null"); //$NON-NLS-1$
        if (mimeType == null || mimeType.length() == 0)
            throw new IllegalArgumentException("mimeType must not be null or empty"); //$NON-NLS-1$

        final String requestType = mimeType.trim().toLowerCase();
        final String[] requestPair = StringUtil.splitAcceptPairAllowingSingleAsterisk(requestType);
        final String rType = requestPair[0];

        for (MediaRange entry : parseAcceptContentHeaders(request)) {
            String entryType = entry.getType();
            String[] typePair = StringUtil.splitAcceptPairAllowingSingleAsterisk(entryType);
            String mType = typePair[0];
            String mSubType = typePair[1];
            if ((entryType.equals(requestType) ||
                    mType.equals(rType) && mSubType.equals("*") || entryType.equals("*/*")) && //$NON-NLS-1$ //$NON-NLS-2$
                    entry.getQValue().floatValue() != 0) {
                if (parameters != null) {
                    boolean acceptable = parmsInMap(parameters, entry.getParameters());
                    return acceptable;
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Tests whether or not the specified <code>charset</code> is acceptable
     * to the specified <code>request</code>, as indicated in that requests
     * accept headers.
     *
     * @param request The http request. Cannot be <code>null</code>.
     *
     * @param charset The charset type to test.  Cannot be <code>null</code> or
     * empty.
     *
     * @return <code>true</code> if the specified charset is
     * acceptable to the request, as specified in request headers. If there is
     * no accept header present in the request, or the header value is empty,
     * then this function returns <code>true</code> (it is assumed that all
     * charsets are acceptable).
     */
    private static boolean isCharsetAcceptable(HttpServletRequest request, String charset) {
        if (request == null)
            throw new IllegalArgumentException("request must not be null"); //$NON-NLS-1$
        if (charset == null || charset.length() == 0)
            throw new IllegalArgumentException("charset must not be null or empty"); //$NON-NLS-1$

        final String requestType = charset.trim().toLowerCase();
        for (CharsetRange entry : parseAcceptCharsetHeaders(request)) {
            String entryType = entry.getType();
            if ((entryType.equals(requestType) || entryType.equals("*")) && //$NON-NLS-1$
                    entry.getQValue().floatValue() != 0)
                return true;
        }
        return false;
    }

    /**
     * Returns an array of {@link MediaRange} objects corresponding the
     * specified "Accept" header value encapsulated in the specified
     * request.
     *
     * @param request The request object.  Must not be <code>null</code>
     *
     * @return The {@link MediaRange} objects corresponding to the
     * types requested as "acceptable" in the specified request.  The returned
     * array is sorted by "most acceptable" in the lowest ordinal positions
     * and "least acceptable" in the highest ordinal position.  All string values
     * referenced in the return range objects are normalized to lowercase to
     * ease comparisons.
     */
    private static MediaRange[] parseAcceptContentHeaders(HttpServletRequest request) {
        if (request == null)
            throw new IllegalArgumentException("request must not be null"); //$NON-NLS-1$
        String headerVal = getHeaderValue(HDR_ACCEPT, request);
        return MediaRange.parse(headerVal);
    }

    /**
     * Returns an array of {@link CharsetRange} objects corresponding the
     * specified "Accept-Charset" header value encapsulated in the specified
     * request.
     *
     * @param request The request object.  Must not be <code>null</code>
     *
     * @return The {@link CharsetRange} objects corresponding to the
     * types requested as "acceptable" in the specified request.  The returned
     * array is sorted by "most acceptable" in the lowest ordinal positions
     * and "least acceptable" in the highest ordinal position.  All string values
     * referenced in the return range objects are normalized to lowercase to
     * ease comparisons.
     */
    private static CharsetRange[] parseAcceptCharsetHeaders(HttpServletRequest request) {
        if (request == null)
            throw new IllegalArgumentException("request must not be null"); //$NON-NLS-1$
        String headerVal = getHeaderValue(HDR_ACCEPT_CHARSET, request);
        return CharsetRange.parse(headerVal);
    }

    /**
     * Returns a comma-delimited string for all of the values in the referenced
     * headers.
     *
     * @param headerName The name of the headers for which the values should be concatenated. May be <code>null</code>.
     * @param request The request in which to look for headers
     * @return The concatentated headers.  Will be <code>null</code> if
     * <code>headers</code> is <code>null</code> or empty.
     */
    private static String getHeaderValue(String headerName, HttpServletRequest request) {
        if (headerName == null || headerName.length() == 0)
            throw new IllegalArgumentException("headerName must not be null or empty"); //$NON-NLS-1$
        if (request == null)
            throw new IllegalArgumentException("request must not be null"); //$NON-NLS-1$

        StringBuilder sb = new StringBuilder();
        Enumeration<String> headersList = request.getHeaders(headerName);
        if (!headersList.hasMoreElements()) {
            return sb.toString();
        }

        Set<String> headerSet = new HashSet<String>();
        while (headersList.hasMoreElements()) {
            headerSet.add((headersList.nextElement()));
        }

        if (headerSet.size() > 0) {
            int counter = 0;
            for (String value : headerSet) {
                sb.append(value);
                if (counter < headerSet.size() - 1) {
                    sb.append(",");
                }
                counter++;
            }
        }

        return sb.toString();
    }

    /**
     * Returns whether or not the specified parms (which are in token '=' token format)
     * are found in the referenced parmMap.  A case-insensitive compare is performed.
     *
     * @param parms The parms to search for. Must not be <code>null</code>.
     * @param parmMap The parameter map in which to search. Must not be <code>null</code>.
     * The values in the map are assumed to be normalized to lowercase.
     *
     * @return <code>true</code> if all of the referenced parameters are found in the
     * map, <code>false</code> if they are not.
     */
    private static boolean parmsInMap(Collection<String> parms, Map<String, String[]> parmMap) {
        if (parms == null)
            throw new IllegalArgumentException("parms cannot be null"); //$NON-NLS-1$
        if (parmMap == null)
            throw new IllegalArgumentException("parmMap cannot be null"); //$NON-NLS-1$

        for (String parm : parms) {
            String[] splitParm = StringUtil.splitPair(parm, '=');
            String parmName = splitParm[0].toLowerCase();
            String parmValue = splitParm[1].toLowerCase();
            if (!parmMap.containsKey(parmName))
                return false;

            boolean valueFound = false;
            for (String value : parmMap.get(parmName)) {
                if (parmValue.equals(value)) {
                    valueFound = true;
                    break;
                }
            }
            if (!valueFound)
                return false;
        }

        return true;
    }

    protected static List<String> getList(JsonArray values) {
        if (OidcOAuth20Util.isNullEmpty(values)) {
            return new ArrayList<String>();
        }

        Gson converter = new Gson();
        Type type = new TypeToken<List<String>>() {
        }.getType();
        return converter.fromJson(values, type);
    }

    protected static OidcServerException checkConditionalExecution(HttpServletRequest request, boolean isGetOrHead, boolean exists, String eTag, Date lastModified)
            throws OidcServerException {
        boolean hasETagCondition = !OidcOAuth20Util.isNullEmpty(request.getHeaders(HDR_IF_MATCH)) || !OidcOAuth20Util.isNullEmpty(request.getHeaders(HDR_IF_NONE_MATCH));
        boolean hasModifiedCondition = !OidcOAuth20Util.isNullEmpty(request.getHeaders(HDR_IF_MODIFIED_SINCE)) || !OidcOAuth20Util.isNullEmpty(request.getHeaders(HDR_IF_UNMODIFIED_SINCE));

        // If no conditions, just return
        if (!hasETagCondition && !hasModifiedCondition) {
            return null;
        }

        // Check any ETag conditions
        OidcServerException etagException = null;
        if (hasETagCondition && eTag != null && eTag.length() != 0) {
            etagException = checkETagConditions(request, isGetOrHead, exists, eTag);
            // If no Last-Modified condition, can just return ETag result
            if (!hasModifiedCondition) {
                return etagException;
            }
        }

        // Check any last-modified conditions
        OidcServerException lastModifiedException = null;
        if (hasModifiedCondition && lastModified != null) {
            lastModifiedException = checkModifiedConditions(request, lastModified);
            // If no ETag condition, can just return Last-Modified result
            if (!hasETagCondition) {
                return lastModifiedException;
            }
        }

        // Both ETag and Last-Modified conditions are present, can only return Not Modified if both conditions
        // had the same status
        if (etagException != null && etagException.getHttpStatus() == HttpServletResponse.SC_NOT_MODIFIED) {
            return lastModifiedException;
        } else if (lastModifiedException != null && lastModifiedException.getHttpStatus() == HttpServletResponse.SC_NOT_MODIFIED) {
            return etagException;
        } else if (etagException != null) {
            return etagException;
        } else if (lastModifiedException != null) {
            return lastModifiedException;
        }

        // Conditions succeeded
        return null;
    }

    private static OidcServerException checkETagConditions(HttpServletRequest request, boolean isGetOrHead, boolean exists, String eTag)
            throws OidcServerException {
        boolean ifMatch = false;
        String headerName;
        String matchValue = getHeaderValue(HDR_IF_MATCH, request);
        if (matchValue != null && matchValue.length() != 0) {
            ifMatch = true;
            headerName = HDR_IF_MATCH;
        } else {
            matchValue = getHeaderValue(HDR_IF_NONE_MATCH, request);
            headerName = HDR_IF_NONE_MATCH;
        }

        // If no precondition specified, it succeeded
        if (matchValue == null || matchValue.length() == 0)
            return null;

        // Response can depend on whether request is GET or HEAD
        boolean atLeastOneMatch = false;
        String[] matchTokens = matchValue.split(","); //$NON-NLS-1$

        for (String token : matchTokens) {
            String testETag = token.trim();
            if (testETag.equals("*")) { //$NON-NLS-1$
                // Must be the only value
                if (matchTokens.length != 1) {
                    String errorMsg = "The value \"%s\" for \"%s\" header \"*\" is not valid because it must be the only token in the value.";
                    String description = String.format(errorMsg, matchValue, headerName);
                    throw new OidcServerException(description, OIDCConstants.ERROR_INVALID_REQUEST, HttpServletResponse.SC_BAD_REQUEST);
                }
                // For If-Match, condition succeeds if resource exists
                if (ifMatch) {
                    String description = "If-Match header specified in request and it did not match.";

                    return exists ? null : new OidcServerException(description, OIDCConstants.ERROR_INVALID_REQUEST, HttpServletResponse.SC_PRECONDITION_FAILED);
                }
                // For If-None-Match, condition fails if resource exists
                if (exists) {
                    String description = "No If-Match header specified in request.";

                    return isGetOrHead ? new OidcServerException((String) null, null, HttpServletResponse.SC_NOT_MODIFIED) : new OidcServerException(description, OIDCConstants.ERROR_INVALID_REQUEST, HttpServletResponse.SC_PRECONDITION_FAILED);
                }
                // Condition succeeds
                return null;
            }

            // ETag must be quoted string
            if (testETag.charAt(0) != '"' || testETag.charAt(testETag.length() - 1) != '"') {
                String errorMsg = "The entity tag \"%s\" in \"%s\" header is not valid because it must be a quoted string.";
                String description = String.format(errorMsg, testETag, headerName);
                throw new OidcServerException(description, OIDCConstants.ERROR_INVALID_REQUEST, HttpServletResponse.SC_BAD_REQUEST);
            }

            testETag = testETag.substring(1, testETag.length() - 1);
            if (eTag != null && eTag.equals(testETag)) {
                atLeastOneMatch = true;
            }
        }

        // If a match is required and there were none, return Precondition Failed
        if (ifMatch && !atLeastOneMatch) {
            String description = "If-Match header specified in request and it did not match.";
            return new OidcServerException(description, OIDCConstants.ERROR_INVALID_REQUEST, HttpServletResponse.SC_PRECONDITION_FAILED);
        }

        // If no matches required and there was one, return Not Modified for GET/HEAD,
        // or Precondition Failed for everything else
        if (!ifMatch && atLeastOneMatch) {
            String description = "No If-Match header specified in request.";
            return isGetOrHead ? new OidcServerException((String) null, null, HttpServletResponse.SC_NOT_MODIFIED) : new OidcServerException(description, OIDCConstants.ERROR_INVALID_REQUEST, HttpServletResponse.SC_PRECONDITION_FAILED);
        }

        // Condition met
        return null;
    }

    private static OidcServerException checkModifiedConditions(HttpServletRequest request, Date lastModified) throws OidcServerException {
        boolean ifModified = false;
        String matchValue = getHeaderValue(HDR_IF_MODIFIED_SINCE, request);
        if (matchValue != null && matchValue.length() != 0) {
            ifModified = true;
        } else {
            matchValue = getHeaderValue(HDR_IF_UNMODIFIED_SINCE, request);
        }

        // If no precondition specified, it succeeded
        if (matchValue == null || matchValue.length() == 0)
            return null;

        // Convert header value to Date
        Date matchTime = DateUtil.parseTimeRFC2616(matchValue);
        if (matchTime == null) {
            // Invalid time format; HTTP spec says condition should be ignored (i.e. request executes)
            return null;
        }

        // For If-Modified-Since, if resource not modified, return 304 Not Modified
        if (ifModified && !lastModified.after(matchTime)) {
            return new OidcServerException((String) null, null, HttpServletResponse.SC_NOT_MODIFIED);
        }

        // For If-Unmodified-Since, if resource modified, return 412 Precondition Failed
        if (!ifModified && lastModified.after(matchTime)) {
            String description = "Resource modified.";
            return new OidcServerException(description, OIDCConstants.ERROR_INVALID_REQUEST, HttpServletResponse.SC_PRECONDITION_FAILED);
        }

        // Condition met
        return null;
    }
}
