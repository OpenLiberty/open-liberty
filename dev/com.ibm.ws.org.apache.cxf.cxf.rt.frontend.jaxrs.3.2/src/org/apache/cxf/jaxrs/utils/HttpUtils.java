/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.jaxrs.utils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.RuntimeDelegate;
import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.UrlUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.impl.HttpHeadersImpl;
import org.apache.cxf.jaxrs.impl.HttpServletRequestFilter;
import org.apache.cxf.jaxrs.impl.HttpServletResponseFilter;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.PathSegmentImpl;
import org.apache.cxf.jaxrs.impl.RuntimeDelegateImpl;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.Headers;
import org.apache.cxf.transport.servlet.BaseUrlHelper;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.utils.UriEncoder;

public final class HttpUtils {

    private static final TraceComponent tc = Tr.register(HttpUtils.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(HttpUtils.class);

    private static final String REQUEST_PATH_TO_MATCH = "path_to_match";
    private static final String REQUEST_PATH_TO_MATCH_SLASH = "path_to_match_slash";

    private static final String HTTP_SCHEME = "http";
    private static final String LOCAL_HOST_IP_ADDRESS = "127.0.0.1";
    private static final String REPLACE_LOOPBACK_PROPERTY = "replace.loopback.address.with.localhost";
    private static final String LOCAL_HOST_IP_ADDRESS_SCHEME = "://" + LOCAL_HOST_IP_ADDRESS;
    private static final String ANY_IP_ADDRESS = "0.0.0.0";
    private static final String ANY_IP_ADDRESS_SCHEME = "://" + ANY_IP_ADDRESS;
    private static final int DEFAULT_HTTP_PORT = 80;

    private static final Pattern ENCODE_PATTERN = Pattern.compile("%[0-9a-fA-F][0-9a-fA-F]");
    private static final String CHARSET_PARAMETER = "charset";
    private static final String DOUBLE_QUOTE = "\"";

    // there are more of such characters, ex, '*' but '*' is not affected by UrlEncode
    private static final String PATH_RESERVED_CHARACTERS = "=@/:!$&\'(),;~";
    private static final String QUERY_RESERVED_CHARACTERS = "?/,";
    
    private static final Set<String> KNOWN_HTTP_VERBS_WITH_NO_REQUEST_CONTENT =
        new HashSet<>(Arrays.asList(new String[]{"GET", "HEAD", "OPTIONS", "TRACE"}));
    private static final Set<String> KNOWN_HTTP_VERBS_WITH_NO_RESPONSE_CONTENT =
        new HashSet<>(Arrays.asList(new String[]{"HEAD", "OPTIONS"}));

    private HttpUtils() {
    }

    public static String urlDecode(String value, String enc) {
        return UrlUtils.urlDecode(value, enc);
    }

    public static String urlDecode(String value) {
        return UrlUtils.urlDecode(value);
    }

    public static String pathDecode(String value) {
        return UrlUtils.pathDecode(value);
    }

    private static String componentEncode(String reservedChars, String value) {

        StringBuilder buffer = null;
        int length = value.length();
        int startingIndex = 0;
        for (int i = 0; i < length; i++) {
            char currentChar = value.charAt(i);
            if (reservedChars.indexOf(currentChar) != -1) {
                if (buffer == null) {
                    buffer = new StringBuilder(length + 8);
                }
                // If it is going to be an empty string nothing to encode.
                if (i != startingIndex) {
                    buffer.append(urlEncode(value.substring(startingIndex, i)));
                }
                buffer.append(currentChar);
                startingIndex = i + 1;
            }
        }

        if (buffer == null) {
            return urlEncode(value);
        }
        if (startingIndex < length) {
            buffer.append(urlEncode(value.substring(startingIndex, length)));
        }

        return buffer.toString();
    }

    public static String queryEncode(String value) {

        return componentEncode(QUERY_RESERVED_CHARACTERS, value);
    }

    public static String urlEncode(String value) {

        return urlEncode(value, StandardCharsets.UTF_8.name());
    }

    public static String urlEncode(String value, String enc) {

        return UrlUtils.urlEncode(value, enc);
    }

    public static String pathEncode(String value) {

        String result = componentEncode(PATH_RESERVED_CHARACTERS, value);
        // URLEncoder will encode '+' to %2B but will turn ' ' into '+'
        // We need to retain '+' and encode ' ' as %20
        if (result.indexOf('+') != -1) {
            result = result.replace("+", "%20");
        }
        if (result.indexOf("%2B") != -1) {
            result = result.replace("%2B", "+");
        }

        return result;
    }

    public static boolean isPartiallyEncoded(String value) {
        return ENCODE_PATTERN.matcher(value).find();
    }

    /**
     * Encodes partially encoded string. Encode all values but those matching pattern
     * "percent char followed by two hexadecimal digits".
     *
     * @param encoded fully or partially encoded string.
     * @return fully encoded string
     */
    public static String encodePartiallyEncoded(String encoded, boolean query) {
        if (encoded.length() == 0) {
            return encoded;
        }
        Matcher m = ENCODE_PATTERN.matcher(encoded);

        if (!m.find()) {
            return query ? HttpUtils.queryEncode(encoded) : HttpUtils.pathEncode(encoded);
        }

        int length = encoded.length();
        StringBuilder sb = new StringBuilder(length + 8);
        int i = 0;
        do {
            String before = encoded.substring(i, m.start());
            sb.append(query ? HttpUtils.queryEncode(before) : HttpUtils.pathEncode(before));
            sb.append(m.group());
            i = m.end();
        } while (m.find());
        String tail = encoded.substring(i, length);
        sb.append(query ? HttpUtils.queryEncode(tail) : HttpUtils.pathEncode(tail));
        return sb.toString();
    }

    public static SimpleDateFormat getHttpDateFormat() {
        return Headers.getHttpDateFormat();
    }

    public static String toHttpDate(Date date) {
        return Headers.toHttpDate(date);
    }

    public static RuntimeDelegate getOtherRuntimeDelegate() {
        try {
            RuntimeDelegate rd = RuntimeDelegate.getInstance();
            return rd instanceof RuntimeDelegateImpl ? null : rd;
        } catch (Throwable t) {
            return null;
        }
    }

    public static HeaderDelegate<Object> getHeaderDelegate(Object o) {
        return getHeaderDelegate(RuntimeDelegate.getInstance(), o);
    }

    @SuppressWarnings("unchecked")
    public static HeaderDelegate<Object> getHeaderDelegate(RuntimeDelegate rd, Object o) {
        return rd == null ? null : (HeaderDelegate<Object>)rd.createHeaderDelegate(o.getClass());
    }

    @SuppressWarnings("unchecked")
    public static <T> MultivaluedMap<String, T> getModifiableStringHeaders(Message m) {
        MultivaluedMap<String, Object> headers = getModifiableHeaders(m);
        convertHeaderValuesToString(headers, false);
        return (MultivaluedMap<String, T>)headers;
    }

    public static MultivaluedMap<String, Object> getModifiableHeaders(Message m) {
        Map<String, List<Object>> headers = CastUtils.cast((Map<?, ?>)m.get(Message.PROTOCOL_HEADERS));
        return new MetadataMap<String, Object>(headers, false, false, true);
    }

    public static void convertHeaderValuesToString(Map<String, List<Object>> headers, boolean delegateOnly) {
        if (headers == null) {
            return;
        }
        RuntimeDelegate rd = getOtherRuntimeDelegate();
        if (rd == null && delegateOnly) {
            return;
        }
        for (Map.Entry<String, List<Object>> entry : headers.entrySet()) {
            List<Object> values = entry.getValue();
            for (int i = 0; i < values.size(); i++) {
                Object value = values.get(i);

                if (value != null && !(value instanceof String)) {

                    HeaderDelegate<Object> hd = getHeaderDelegate(rd, value);

                    if (hd != null) {
                        value = hd.toString(value);
                    } else if (!delegateOnly) {
                        value = value.toString();
                    }

                    try {
                        values.set(i, value);
                    } catch (UnsupportedOperationException ex) {
                        // this may happen if an unmodifiable List was set via Map put
                        List<Object> newList = new ArrayList<>(values);
                        newList.set(i, value);
                        // Won't help if the map is unmodifiable in which case it is a bug anyway
                        headers.put(entry.getKey(), newList);
                    }

                }

            }
        }

    }

    public static Date getHttpDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Headers.getHttpDateFormat().parse(value);
        } catch (ParseException ex) {
            return null;
        }
    }

    public static Locale getLocale(String value) {
        if (value == null) {
            return null;
        }
        String language = null;
        String locale = null;
        int index = value.indexOf('-');
        if (index == 0 || index == value.length() - 1) {
            throw new IllegalArgumentException("Illegal locale value : " + value);
        }

        if (index > 0) {
            language = value.substring(0, index);
            locale = value.substring(index + 1);
        } else {
            language = value;
        }

        if (locale == null) {
            return new Locale(language);
        }
        return new Locale(language, locale);

    }

    public static int getContentLength(String value) {
        if (value == null) {
            return -1;
        }
        try {
            int len = Integer.parseInt(value);
            return len >= 0 ? len : -1;
        } catch (Exception ex) {
            return -1;
        }
    }

    public static String getHeaderString(List<String> values) {
        if (values == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            String value = values.get(i);
            if (StringUtils.isEmpty(value)) {
                continue;
            }
            sb.append(value);
            if (i + 1 < values.size()) {
                sb.append(',');
            }
        }
        return sb.toString();
    }

    public static boolean isDateRelatedHeader(String headerName) {
        return HttpHeaders.DATE.equalsIgnoreCase(headerName)
               || HttpHeaders.IF_MODIFIED_SINCE.equalsIgnoreCase(headerName)
               || HttpHeaders.IF_UNMODIFIED_SINCE.equalsIgnoreCase(headerName)
               || HttpHeaders.EXPIRES.equalsIgnoreCase(headerName)
               || HttpHeaders.LAST_MODIFIED.equalsIgnoreCase(headerName);
    }

    public static boolean isHttpRequest(Message message) {
        return message.get(AbstractHTTPDestination.HTTP_REQUEST) != null;
    }

    public static URI toAbsoluteUri(String relativePath, Message message) {
        String base = BaseUrlHelper.getBaseURL(
            (HttpServletRequest)message.get(AbstractHTTPDestination.HTTP_REQUEST));
        return URI.create(base + relativePath);
    }

    public static URI toAbsoluteUri(URI u, Message message) {
        HttpServletRequest request =
            (HttpServletRequest)message.get(AbstractHTTPDestination.HTTP_REQUEST);
        boolean absolute = u.isAbsolute();
        StringBuilder uriBuf = new StringBuilder();
        if (request != null && (!absolute || isLocalHostOrAnyIpAddress(u, uriBuf, message))) {
            String serverAndPort = request.getServerName();
            boolean localAddressUsed = false;
            if (absolute) {
                if (ANY_IP_ADDRESS.equals(serverAndPort)) {
                    serverAndPort = request.getLocalAddr();
                    localAddressUsed = true;
                }
                if (LOCAL_HOST_IP_ADDRESS.equals(serverAndPort)) {
                    serverAndPort = "localhost";
                    localAddressUsed = true;
                }
            }


            int port = localAddressUsed ? request.getLocalPort() : request.getServerPort();
            if (port != DEFAULT_HTTP_PORT) {
                serverAndPort += ":" + port;
            }
            String base = request.getScheme() + "://" + serverAndPort;
            if (!absolute) {
                u = URI.create(base + u.toString());
            } else {
                int originalPort = u.getPort();
                String hostValue = uriBuf.toString().contains(ANY_IP_ADDRESS_SCHEME)
                    ? ANY_IP_ADDRESS : LOCAL_HOST_IP_ADDRESS;
                String replaceValue = originalPort == -1 ? hostValue : hostValue + ":" + originalPort;
                u = URI.create(u.toString().replace(replaceValue, serverAndPort));
            }
        }
        return u;
    }

    private static boolean isLocalHostOrAnyIpAddress(URI u, StringBuilder uriStringBuffer, Message m) {
        String uriString = u.toString();
        boolean result = uriString.contains(LOCAL_HOST_IP_ADDRESS_SCHEME) && replaceLoopBackAddress(m)
            || uriString.contains(ANY_IP_ADDRESS_SCHEME);
        uriStringBuffer.append(uriString);
        return result;
    }

    private static boolean replaceLoopBackAddress(Message m) {
        Object prop = m.getContextualProperty(REPLACE_LOOPBACK_PROPERTY);
        return prop == null || PropertyUtils.isTrue(prop);
    }

    public static void resetRequestURI(Message m, String requestURI) {
        m.remove(REQUEST_PATH_TO_MATCH_SLASH);
        m.remove(REQUEST_PATH_TO_MATCH);
        m.put(Message.REQUEST_URI, requestURI);
    }


    public static String getPathToMatch(Message m, boolean addSlash) {
        String var = addSlash ? REQUEST_PATH_TO_MATCH_SLASH : REQUEST_PATH_TO_MATCH;
        String pathToMatch = (String)m.get(var);
        if (pathToMatch != null) {
            return pathToMatch;
        }
        String requestAddress = getProtocolHeader(m, Message.REQUEST_URI, "/");
        if (m.get(Message.QUERY_STRING) == null) {
            int index = requestAddress.lastIndexOf('?');
            if (index > 0 && index < requestAddress.length()) {
                m.put(Message.QUERY_STRING, requestAddress.substring(index + 1));
                requestAddress = requestAddress.substring(0, index);
            }
        }
        String baseAddress = getBaseAddress(m);
        pathToMatch = getPathToMatch(requestAddress, baseAddress, addSlash);
        m.put(var, pathToMatch);
        return pathToMatch;
    }

    public static String getProtocolHeader(Message m, String name, String defaultValue) {
        return getProtocolHeader(m, name, defaultValue, false);
    }

    public static String getProtocolHeader(Message m, String name, String defaultValue, boolean setOnMessage) {
        String value = (String)m.get(name);
        if (value == null) {
            value = new HttpHeadersImpl(m).getRequestHeaders().getFirst(name);
            if (value != null && setOnMessage) {
                m.put(name, value);
            }
        }
        return value == null ? defaultValue : value;
    }

    public static String getBaseAddress(Message m) {
        String endpointAddress = getEndpointAddress(m);

        //Liberty code change start
        String[] addr = parseURI(endpointAddress, false);
        if (addr == null) {
            return endpointAddress;
        }
        String scheme = addr[0];
        String path = addr[1];
        if (scheme != null && !scheme.startsWith(HttpUtils.HTTP_SCHEME)
                && HttpUtils.isHttpRequest(m)) {
            path = HttpUtils.toAbsoluteUri(path, m).getRawPath();
        }
        return (path == null || path.length() == 0) ? "/" : path;
    }

    public static String[] parseURI(String addr, boolean parseAuthority) {
        String scheme = null;
        String parsedAuthorityOrRawPath = null;
        int n = addr.length();
        int p = scan(addr, 0, n, "/?#", ":");
        try {
            if ((p >= 0) && at(addr, p, n, ':')) {
                if (p == 0) {
                    return null;
                }    
                scheme = addr.substring(0, p);
                p++;
                if (at(addr, p, n, '/')) {
                    parsedAuthorityOrRawPath = postSchemeParse(addr, p, n, parseAuthority);
                } else {
                    int q = scan(addr, p, n, null, "#");
                    if (q <= p) {
                        return null;
                    }    
                }
            } else {
                parsedAuthorityOrRawPath = postSchemeParse(addr, 0, n, parseAuthority);
            }
        } catch (IllegalArgumentException e) {
            return null;
        }
        return new String[] { scheme, parsedAuthorityOrRawPath };
    }

    private static String postSchemeParse(String addr, int start, int n, boolean parseAuthority) {
        int p = start;
        int q = -1;
        if (at(addr, p, n, '/') && at(addr, p + 1, n, '/')) {
            p += 2;
            q = scan(addr, p, n, null, "/");
            if (q > p) {
                if (!parseAuthority) {
                    p = q;
                }
            } else if (q >= n) {
                if (parseAuthority || p == n) {
                    throw new IllegalArgumentException();
                } else {
                    return null;
                }
            }    
        }
        
        if (!parseAuthority) {
            q = scan(addr, p, n, null, "?#");
        } else if (p >= q) {
            // empty authority should be null
            return null;
        }
        
        return addr.substring(p, q);
    }
    
    private static boolean at(String addr, int start, int end, char c) {
        return (start < end) && (addr.charAt(start) == c);
    }
    
    private static int scan(String addr, int start, int end, String err, String stop) {
        int p = start;
        while (p < end) {
            char c = addr.charAt(p);
            if (err != null && err.indexOf(c) >= 0) {
                return -1;
            }
            if (stop.indexOf(c) >= 0) {
                break;
            }    
            p++;
        }
        return p;
    }
    //Liberty code change end

    public static String getEndpointAddress(Message m) {
        String address = null;
        Destination d = m.getExchange().getDestination();
        if (d != null) {
            if (d instanceof AbstractHTTPDestination) {
                HttpServletRequest request = (HttpServletRequest) m.get(AbstractHTTPDestination.HTTP_REQUEST);
                Object property = request != null ? request.getAttribute("org.apache.cxf.transport.endpoint.address") : null;
                //Liberty code change start
                address = property != null ? property.toString() : ((AbstractHTTPDestination) d).getEndpointInfo().getAddress();
                //Liberty code change end
            } else {
                address = m.containsKey(Message.BASE_PATH) ? (String) m.get(Message.BASE_PATH) : d.getAddress().getAddress().getValue();
            }
        } else {
            address = (String) m.get(Message.ENDPOINT_ADDRESS);
        }
        if (address.startsWith("http") && address.endsWith("//")) {
            address = address.substring(0, address.length() - 1);
        }
        return address;
    }

    public static void updatePath(Message m, String path) {
        String baseAddress = getBaseAddress(m);
        boolean pathSlash = path.startsWith("/");
        boolean baseSlash = baseAddress.endsWith("/");
        if (pathSlash && baseSlash) {
            path = path.substring(1);
        } else if (!pathSlash && !baseSlash) {
            path = "/" + path;
        }
        m.put(Message.REQUEST_URI, baseAddress + path);
        m.remove(REQUEST_PATH_TO_MATCH);
        m.remove(REQUEST_PATH_TO_MATCH_SLASH);
    }

    public static String getPathToMatch(String path, String address, boolean addSlash) {

        String origPath = path;

        int ind = path.indexOf(address);
        if (ind == -1 && address.equals(path + "/")) {
            path += "/";
            ind = 0;
        }
        if (ind == 0) {
            path = path.substring(address.length());
        }
        if (addSlash && !path.startsWith("/")) {
            path = "/" + path;
        }

        if (path.equals(origPath) && origPath.contains("%")) {
            address = UriEncoder.encodeString(address).replace("%2F", "/");
            path = origPath;
            ind = path.indexOf(address);
            if (ind == -1 && address.equals(path + "/")) {
                path += "/";
                ind = 0;
            }
            if (ind == 0) {
                path = path.substring(ind + address.length());
            }
            if (addSlash && !path.startsWith("/")) {
                path = "/" + path;
            }
        }
//        if (path.equals(origPath) && path.indexOf('!') != -1 && address.indexOf("%21") != -1
//                && (path.indexOf('!') == address.indexOf("%21"))) {
//            path = path.replace("!", "%21");
//            path = getPathToMatch(path, address, addSlash);
//        }
        return path;
    }

    public static String getOriginalAddress(Message m) {
        Destination d = m.getDestination();
        return d == null ? "/" : d.getAddress().getAddress().getValue();
    }

    public static String fromPathSegment(PathSegment ps) {
        if (PathSegmentImpl.class.isAssignableFrom(ps.getClass())) {
            return ((PathSegmentImpl) ps).getOriginalPath();
        }
        StringBuilder sb = new StringBuilder();
        sb.append(ps.getPath());
        for (Map.Entry<String, List<String>> entry : ps.getMatrixParameters().entrySet()) {
            for (String value : entry.getValue()) {
                sb.append(';').append(entry.getKey());
                if (value != null) {
                    sb.append('=').append(value);
                }
            }
        }
        return sb.toString();
    }

    public static Response.Status getParameterFailureStatus(ParameterType pType) {
        if (pType == ParameterType.MATRIX || pType == ParameterType.PATH
            || pType == ParameterType.QUERY) {
            return Response.Status.NOT_FOUND;
        }
        return Response.Status.BAD_REQUEST;
    }

    public static String getSetEncoding(MediaType mt, MultivaluedMap<String, Object> headers,
                                        String defaultEncoding) {
        String enc = getMediaTypeCharsetParameter(mt);
        if (enc == null) {
            return defaultEncoding;
        }
        try {
            "0".getBytes(enc);
            return enc;
        } catch (UnsupportedEncodingException ex) {
            String message = new org.apache.cxf.common.i18n.Message("UNSUPPORTED_ENCODING", BUNDLE, enc, defaultEncoding).toString();
            Tr.warning(tc, message);
            headers.putSingle(HttpHeaders.CONTENT_TYPE,
                              JAXRSUtils.mediaTypeToString(mt, CHARSET_PARAMETER)
                                                        + ';' + CHARSET_PARAMETER + "="
                                                        + (defaultEncoding == null ? StandardCharsets.UTF_8 : defaultEncoding));
        }
        return defaultEncoding;
    }

    public static String getEncoding(MediaType mt, String defaultEncoding) {
        String charset = mt == null ? defaultEncoding : getMediaTypeCharsetParameter(mt);
        return charset == null ? defaultEncoding : charset;
    }

    public static String getMediaTypeCharsetParameter(MediaType mt) {
        String charset = mt.getParameters().get(CHARSET_PARAMETER);
        if (charset != null && charset.startsWith(DOUBLE_QUOTE)
            && charset.endsWith(DOUBLE_QUOTE) && charset.length() > 1) {
            charset = charset.substring(1, charset.length() - 1);
        }
        return charset;
    }

    public static URI resolve(UriBuilder baseBuilder, URI uri) {
        if (!uri.isAbsolute()) {
            return baseBuilder.build().resolve(uri);
        }
        return uri;
    }

    public static URI relativize(URI base, URI uri) {
        // quick bail-out
        if (!(base.isAbsolute()) || !(uri.isAbsolute())) {
            return uri;
        }
        if (base.isOpaque() || uri.isOpaque()) {
            // Unlikely case of an URN which can't deal with
            // relative path, such as urn:isbn:0451450523
            return uri;
        }
        // Check for common root
        URI root = base.resolve("/");
        if (!(root.equals(uri.resolve("/")))) {
            // Different protocol/auth/host/port, return as is
            return uri;
        }

        // Ignore hostname bits for the following , but add "/" in the beginning
        // so that in worst case we'll still return "/fred" rather than
        // "http://example.com/fred".
        URI baseRel = URI.create("/").resolve(root.relativize(base));
        URI uriRel = URI.create("/").resolve(root.relativize(uri));

        // Is it same path?
        if (baseRel.getPath().equals(uriRel.getPath())) {
            return baseRel.relativize(uriRel);
        }

        // Direct siblings? (ie. in same folder)
        URI commonBase = baseRel.resolve("./");
        if (commonBase.equals(uriRel.resolve("./"))) {
            return commonBase.relativize(uriRel);
        }

        // No, then just keep climbing up until we find a common base.
        URI relative = URI.create("");
        while (!(uriRel.getPath().startsWith(commonBase.getPath())) && !"/".equals(commonBase.getPath())) {
            commonBase = commonBase.resolve("../");
            relative = relative.resolve("../");
        }

        // Now we can use URI.relativize
        URI relToCommon = commonBase.relativize(uriRel);
        // and prepend the needed ../
        return relative.resolve(relToCommon);

    }

    public static String toHttpLanguage(Locale locale) {
        return Headers.toHttpLanguage(locale);
    }

    public static boolean isPayloadEmpty(MultivaluedMap<String, String> headers) {
        if (headers != null) {
            String value = headers.getFirst(HttpHeaders.CONTENT_LENGTH);
            if (value != null) {
                try {
                    Long len = Long.valueOf(value);
                    return len <= 0;
                } catch (NumberFormatException ex) {
                    // ignore
                }
            }
        }

        return false;
    }

    public static <T> T createServletResourceValue(Message m, Class<T> clazz) {

        Object value = null;
        if (clazz == HttpServletRequest.class) {
            HttpServletRequest request = (HttpServletRequest) m.get(AbstractHTTPDestination.HTTP_REQUEST);
            value = request != null ? new HttpServletRequestFilter(request, m) : null;
        } else if (clazz == HttpServletResponse.class) {
            HttpServletResponse response = (HttpServletResponse) m.get(AbstractHTTPDestination.HTTP_RESPONSE);
            value = response != null ? new HttpServletResponseFilter(response, m) : null;
        } else if (clazz == ServletContext.class) {
            value = m.get(AbstractHTTPDestination.HTTP_CONTEXT);
        } else if (clazz == ServletConfig.class) {
            value = m.get(AbstractHTTPDestination.HTTP_CONFIG);
        }

        return clazz.cast(value);
    }

    public static boolean isMethodWithNoRequestContent(String method) {
        return KNOWN_HTTP_VERBS_WITH_NO_REQUEST_CONTENT.contains(method);
    }

    public static boolean isMethodWithNoResponseContent(String method) {
        return KNOWN_HTTP_VERBS_WITH_NO_RESPONSE_CONTENT.contains(method);
    }
}
