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
// Liberty Change - 3.4.3 https://github.com/apache/cxf/blob/50f6d7fc063b8728cd3903a9a7775c0462860a83/rt/transports/http/src/main/java/org/apache/cxf/transport/servlet/AbstractHTTPServlet.java
package org.apache.cxf.transport.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.transport.http.AbstractHTTPDestination;


public abstract class AbstractHTTPServlet extends HttpServlet implements Filter {

    private static final long serialVersionUID = -8357252743467075117L;
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractHTTPServlet.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(AbstractHTTPServlet.class);
    /**
     * List of well-known HTTP 1.1 verbs, with POST and GET being the most used verbs at the top
     */
    private static final List<String> KNOWN_HTTP_VERBS =
        Arrays.asList(new String[]{"POST", "GET", "PUT", "DELETE", "HEAD", "OPTIONS", "TRACE"});

    private static final String STATIC_RESOURCES_PARAMETER = "static-resources-list";
    private static final String STATIC_WELCOME_FILE_PARAMETER = "static-welcome-file";
    private static final String STATIC_CACHE_CONTROL = "static-cache-control";
    private static final String STATIC_RESOURCES_MAP_RESOURCE = "/cxfServletStaticResourcesMap.txt";

    private static final String REDIRECTS_PARAMETER = "redirects-list";
    private static final String REDIRECT_SERVLET_NAME_PARAMETER = "redirect-servlet-name";
    private static final String REDIRECT_SERVLET_PATH_PARAMETER = "redirect-servlet-path";
    private static final String REDIRECT_ATTRIBUTES_PARAMETER = "redirect-attributes";
    private static final String REDIRECT_QUERY_CHECK_PARAMETER = "redirect-query-check";
    private static final String REDIRECT_WITH_INCLUDE_PARAMETER = "redirect-with-include";
    private static final String USE_X_FORWARDED_HEADERS_PARAMETER = "use-x-forwarded-headers";
    private static final String X_FORWARDED_PROTO_HEADER = "X-Forwarded-Proto";
    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String X_FORWARDED_PREFIX_HEADER = "X-Forwarded-Prefix";
    private static final String X_FORWARDED_HOST_HEADER = "X-Forwarded-Host";
    private static final String X_FORWARDED_PORT_HEADER = "X-Forwarded-Port";

    private static final Map<String, String> DEFAULT_STATIC_CONTENT_TYPES;

    static {
        DEFAULT_STATIC_CONTENT_TYPES = new HashMap<>();
        DEFAULT_STATIC_CONTENT_TYPES.put("html", "text/html");
        DEFAULT_STATIC_CONTENT_TYPES.put("txt", "text/plain");
        DEFAULT_STATIC_CONTENT_TYPES.put("css", "text/css");
        DEFAULT_STATIC_CONTENT_TYPES.put("pdf", "application/pdf");
        DEFAULT_STATIC_CONTENT_TYPES.put("xsd", "application/xml");
        DEFAULT_STATIC_CONTENT_TYPES.put("js", "application/javascript");
    }

    private List<Pattern> staticResourcesList;
    private String staticWelcomeFile;
    private List<Pattern> redirectList;
    private String dispatcherServletPath;
    private String dispatcherServletName;
    private Map<String, String> redirectAttributes;
    private Map<String, String> staticContentTypes =
        new HashMap<>(DEFAULT_STATIC_CONTENT_TYPES);
    private boolean redirectQueryCheck;
    private boolean useXForwardedHeaders;

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);

        staticResourcesList = parseListSequence(servletConfig.getInitParameter(STATIC_RESOURCES_PARAMETER));
        staticWelcomeFile = servletConfig.getInitParameter(STATIC_WELCOME_FILE_PARAMETER);
        redirectList = parseListSequence(servletConfig.getInitParameter(REDIRECTS_PARAMETER));
        redirectQueryCheck = Boolean.valueOf(servletConfig.getInitParameter(REDIRECT_QUERY_CHECK_PARAMETER));
        dispatcherServletName = servletConfig.getInitParameter(REDIRECT_SERVLET_NAME_PARAMETER);
        dispatcherServletPath = servletConfig.getInitParameter(REDIRECT_SERVLET_PATH_PARAMETER);
        redirectAttributes = parseMapSequence(servletConfig.getInitParameter(REDIRECT_ATTRIBUTES_PARAMETER));
        useXForwardedHeaders = Boolean.valueOf(servletConfig.getInitParameter(USE_X_FORWARDED_HEADERS_PARAMETER));
    }

    public void destroy() {
        FileUtils.maybeDeleteDefaultTempDir();
    }

    protected void finalizeServletInit(ServletConfig servletConfig) throws ServletException {
        InputStream is = getResourceAsStream("/WEB-INF" + STATIC_RESOURCES_MAP_RESOURCE);
        if (is == null) {
            is = getResourceAsStream(STATIC_RESOURCES_MAP_RESOURCE);
        }
        if (is != null) {
            try {
                Properties props = new Properties();
                props.load(is);
                for (String name : props.stringPropertyNames()) {
                    staticContentTypes.put(name, props.getProperty(name));
                }
                is.close();
            } catch (IOException ex) {
                String message = new org.apache.cxf.common.i18n.Message("STATIC_RESOURCES_MAP_LOAD_FAILURE",
                                                                        BUNDLE).toString();
                LOG.warning(message);
            }
        }
    }

    protected InputStream getResourceAsStream(String path) {

        InputStream is = ClassLoaderUtils.getResourceAsStream(path, AbstractHTTPServlet.class);
        if (is == null && getBus() != null) {
            ResourceManager rm = getBus().getExtension(ResourceManager.class);
            if (rm != null) {
                is = rm.resolveResource(path, InputStream.class);
            }
        }
        return is;
    }

    public final void init(final FilterConfig filterConfig) throws ServletException {
        init(new ServletConfig() {
            public String getServletName() {
                return filterConfig.getFilterName();
            }
            public ServletContext getServletContext() {
                return filterConfig.getServletContext();
            }
            public String getInitParameter(String name) {
                return filterConfig.getInitParameter(name);
            }
            public Enumeration<String> getInitParameterNames() {
                return filterConfig.getInitParameterNames();
            }
        });
    }

    protected static List<Pattern> parseListSequence(String values) {
        if (values != null) {
            List<Pattern> list = new ArrayList<>();
            for (String value : values.split("\\s")) {
                if (!value.isEmpty()) {
                    list.add(Pattern.compile(value));
                }
            }
            ((ArrayList<?>)list).trimToSize();
            return list;
        }
        return null;
    }

    protected static Map<String, String> parseMapSequence(String sequence) {
        if (sequence != null) {
            sequence = sequence.trim();
            Map<String, String> map = new HashMap<>();
            for (String pair : sequence.split("\\s")) {
                if (!pair.isEmpty()) {
                    String[] value = pair.split("=");
                    if (value.length == 2) {
                        map.put(value[0], value[1]);
                    } else {
                        map.put(pair, "");
                    }
                }
            }
            return map;
        }
        return Collections.emptyMap();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        handleRequest(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        handleRequest(request, response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        handleRequest(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        handleRequest(request, response);
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        handleRequest(request, response);
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        handleRequest(request, response);
    }

    /**
     * {@inheritDoc}
     *
     * javax.http.servlet.HttpServlet does not let to override the code which deals with
     * unrecognized HTTP verbs such as PATCH (being standardized), WebDav ones, etc.
     * Thus we let CXF servlets process unrecognized HTTP verbs directly, otherwise we delegate
     * to HttpService
     */
    @Override
    public void service(ServletRequest req, ServletResponse res)
        throws ServletException, IOException {

        HttpServletRequest      request;
        HttpServletResponse     response;

        try {
            request = (HttpServletRequest) req;
            response = (HttpServletResponse) res;
        } catch (ClassCastException e) {
            throw new ServletException("Unrecognized HTTP request or response object");
        }

        String method = request.getMethod();
        if (KNOWN_HTTP_VERBS.contains(method)) {
            super.service(request, response);
        } else {
            handleRequest(request, response);
        }
    }

    protected void handleRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletException {
        if ((dispatcherServletPath != null || dispatcherServletName != null)
            && (redirectList != null && matchPath(redirectQueryCheck, redirectList, request) // Liberty Change
                || redirectList == null)) {
            // if no redirectList is provided then this servlet is redirecting only
            redirect(request, response, request.getPathInfo());
            return;
        }
        boolean staticResourcesMatch = staticResourcesList != null
            && matchPath(false, staticResourcesList, request); // Liberty Change
        boolean staticWelcomeFileMatch = staticWelcomeFile != null
            && (StringUtils.isEmpty(request.getPathInfo()) || "/".equals(request.getPathInfo()));
        if (staticResourcesMatch || staticWelcomeFileMatch) {
            serveStaticContent(request, response,
                               staticWelcomeFileMatch ? staticWelcomeFile : request.getPathInfo());
            return;
        }
        request = checkXForwardedHeaders(request);
        invoke(request, response);
    }

    protected HttpServletRequest checkXForwardedHeaders(HttpServletRequest request) {
        if (useXForwardedHeaders) {
            String originalProtocol = request.getHeader(X_FORWARDED_PROTO_HEADER);
            String originalRemoteAddr = request.getHeader(X_FORWARDED_FOR_HEADER);
            String originalPrefix = request.getHeader(X_FORWARDED_PREFIX_HEADER);
            String originalHost = request.getHeader(X_FORWARDED_HOST_HEADER);
            String originalPort = request.getHeader(X_FORWARDED_PORT_HEADER);

            // If at least one of the X-Forwarded-Xxx headers is set, try to use them
            if (Stream.of(originalProtocol, originalRemoteAddr, originalPrefix,
                    originalHost, originalPort).anyMatch(Objects::nonNull)) {
                return new HttpServletRequestXForwardedFilter(request,
                                                              originalProtocol,
                                                              originalRemoteAddr,
                                                              originalPrefix,
                                                              originalHost,
                                                              originalPort);
            }
        }

        return request;

    }


    private static boolean matchPath(boolean checkRedirect, List<Pattern> values, HttpServletRequest request) { // Liberty Change
        String path = request.getPathInfo();
        if (path == null) {
            path = "/";
        }
        if (checkRedirect) { // Liberty Change
            String queryString = request.getQueryString();
            if (queryString != null && !queryString.isEmpty()) {
                path += "?" + queryString;
            }
        }
        for (Pattern pattern : values) {
            if (pattern.matcher(path).matches()) {
                return true;
            }
        }
        return false;
    }

    protected abstract Bus getBus();

    protected void serveStaticContent(HttpServletRequest request,
                                      HttpServletResponse response,
                                      String pathInfo) throws ServletException {
        try (InputStream is = getResourceAsStream(pathInfo)) {
            if (is == null) {
                throw new ServletException("Static resource " + pathInfo + " is not available");
            }
            int ind = pathInfo.lastIndexOf('.');
            if (ind > 0) {
                String type = getStaticResourceContentType(pathInfo.substring(ind + 1));
                if (type != null) {
                    response.setContentType(type);
                }
            }
            String cacheControl = getServletConfig().getInitParameter(STATIC_CACHE_CONTROL);
            if (cacheControl != null) {
                response.setHeader("Cache-Control", cacheControl.trim());
            }
            IOUtils.copy(is, response.getOutputStream());
        } catch (IOException ex) {
            throw new ServletException("Static resource " + pathInfo
                                       + " can not be written to the output stream");
        }

    }

    protected String getStaticResourceContentType(String extension) {
        return staticContentTypes.get(extension);
    }

    protected void redirect(HttpServletRequest request, HttpServletResponse response, String pathInfo)
        throws ServletException {
        boolean customServletPath = dispatcherServletPath != null;
        String theServletPath = customServletPath ? dispatcherServletPath : "/";

        ServletContext sc = super.getServletContext();
        RequestDispatcher rd = dispatcherServletName != null
            ? sc.getNamedDispatcher(dispatcherServletName)
            : sc.getRequestDispatcher((theServletPath + pathInfo).replace("//", "/"));
        if (rd == null) {
            String errorMessage = "No RequestDispatcher can be created for path " + pathInfo;
            if (dispatcherServletName != null) {
                errorMessage += ", dispatcher name: " + dispatcherServletName;
            }
            throw new ServletException(errorMessage);
        }
        try {
            for (Map.Entry<String, String> entry : redirectAttributes.entrySet()) {
                request.setAttribute(entry.getKey(), entry.getValue());
            }
            HttpServletRequest servletRequest =
                new HttpServletRequestRedirectFilter(request, pathInfo, theServletPath, customServletPath);
            if (PropertyUtils.isTrue(getServletConfig().getInitParameter(REDIRECT_WITH_INCLUDE_PARAMETER))) {
                rd.include(servletRequest, response);
            } else {
                rd.forward(servletRequest, response);
            }
        } catch (Throwable ex) {
            throw new ServletException("RequestDispatcher for path " + pathInfo + " has failed", ex);
        }
    }


    protected abstract void invoke(HttpServletRequest request, HttpServletResponse response)
        throws ServletException;

    private static class HttpServletRequestRedirectFilter extends HttpServletRequestWrapper {

        private String pathInfo;
        private String servletPath;

        HttpServletRequestRedirectFilter(HttpServletRequest request,
                                        String pathInfo,
                                        String servletPath,
                                        boolean customServletPath) {
            super(request);
            this.pathInfo = pathInfo;
            this.servletPath = servletPath;
            if ("/".equals(this.servletPath) && !customServletPath) {
                if (this.pathInfo == null) {
                    this.pathInfo = "/";
                    this.servletPath = "";
                } else {
                    this.servletPath = "";
                }
            }
        }

        @Override
        public String getServletPath() {
            return servletPath;
        }

        @Override
        public String getPathInfo() {
            return pathInfo;
        }

        @Override
        public String getRequestURI() {
            String contextPath = getContextPath();
            if ("/".equals(contextPath)) {
                contextPath = "";
            }
            return contextPath + (servletPath + pathInfo).replace("//", "/");
        }

        @Override
        public Object getAttribute(String name) {
            if (AbstractHTTPDestination.SERVICE_REDIRECTION.equals(name)) {
                return "true";
            }
            return super.getAttribute(name);
        }
    }
    private static class HttpServletRequestXForwardedFilter extends HttpServletRequestWrapper {

        private String newProtocol;
        private String newRemoteAddr;

        private String newContextPath;
        private String newServletPath;
        private String newRequestUri;
        private StringBuffer newRequestUrl;

        HttpServletRequestXForwardedFilter(HttpServletRequest request,
                                           String originalProto,
                                           String originalRemoteAddr,
                                           String originalPrefix,
                                           String originalHost,
                                           String originalPort) {
            super(request);
            this.newProtocol = originalProto;
            if (originalRemoteAddr != null) {
                newRemoteAddr = (originalRemoteAddr.split(",")[0]).trim();
            }
            newRequestUri = calculateNewRequestUri(request, originalPrefix);
            // Although per Mozilla documentation
            // (https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-Host)
            // it should contain one value, Apache's mod_proxy says the comma separated list could
            // be returned (http://httpd.apache.org/docs/2.2/mod/mod_proxy.html). We don't need
            // more than 2 components.
            String outermostHost = originalHost != null ? (originalHost.split(",", 2)[0]).trim() : originalHost;
            newRequestUrl = calculateNewRequestUrl(request,
                                                   originalProto,
                                                   originalPrefix,
                                                   outermostHost,
                                                   originalPort);
            newContextPath = calculateNewContextPath(request, originalPrefix);
            newServletPath = calculateNewServletPath(request, originalPrefix);
        }
        private static String calculateNewContextPath(HttpServletRequest request, String originalPrefix) {
            if (originalPrefix != null) {
                return originalPrefix;
            } else {
                return request.getContextPath();
            }
        }
        private static String calculateNewServletPath(HttpServletRequest request, String originalPrefix) {
            String servletPath = request.getServletPath();
            if (originalPrefix != null) {
                servletPath = request.getContextPath() + servletPath;
            }
            return servletPath;
        }
        private static String calculateNewRequestUri(HttpServletRequest request, String originalPrefix) {
            String requestUri = request.getRequestURI();
            if (originalPrefix != null) {
                requestUri = originalPrefix + requestUri;
            }
            return requestUri;
        }
        private static StringBuffer calculateNewRequestUrl(HttpServletRequest request,
                                                           String originalProto,
                                                           String originalPrefix,
                                                           String originalHost,
                                                           String originalPort) {
            URI uri = URI.create(request.getRequestURL().toString());

            StringBuffer sb = new StringBuffer();

            sb.append(originalProto != null ? originalProto : uri.getScheme())
                .append("://")
                .append(originalHost != null ? originalHost : uri.getHost())
                .append(originalPort != null && !"-1".equals(originalPort)
                    ? ":" + originalPort : uri.getPort() != -1 ? ":" + uri.getPort() : "")
                .append(originalPrefix != null ? originalPrefix : "")
                .append(uri.getRawPath());

            String query = uri.getRawQuery();
            if (query != null) {
                sb.append('?').append(query);
            }

            return sb;
        }
        @Override
        public boolean isSecure() {
            if (newProtocol != null) {
                return "https".equals(newProtocol);
            }
            return super.isSecure();
        }
        @Override
        public StringBuffer getRequestURL() {
            return newRequestUrl;
        }
        @Override
        public String getRemoteAddr() {
            if (newRemoteAddr != null) {
                return newRemoteAddr;
            }
            return super.getRemoteAddr();
        }

        @Override
        public String getRequestURI() {
            return newRequestUri;
        }

        @Override
        public String getContextPath() {
            return newContextPath;
        }

        @Override
        public String getServletPath() {
            return newServletPath;
        }


    }

}