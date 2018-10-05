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

package org.apache.cxf.jaxrs.impl;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class UriBuilderImpl extends UriBuilder implements Cloneable {

    private String scheme;
    private String userInfo;
    private int port = -1;
    private String host;
    private List<PathSegment> paths = new ArrayList<PathSegment>();
    private boolean originalPathEmpty;
    private boolean leadingSlash;
    private String fragment;
    private String schemeSpecificPart; 
    private MultivaluedMap<String, String> query = new MetadataMap<String, String>();
    private MultivaluedMap<String, String> matrix = new MetadataMap<String, String>();
    
    private Map<String, Object> resolvedTemplates;
    private Map<String, Object> resolvedTemplatesPathEnc;
    private Map<String, Object> resolvedEncodedTemplates;
    
    /**
     * Creates builder with empty URI.
     */
    public UriBuilderImpl() {
    }

    /**
     * Creates builder initialized with given URI.
     * 
     * @param uri initial value for builder
     * @throws IllegalArgumentException when uri is null
     */
    public UriBuilderImpl(URI uri) throws IllegalArgumentException {
        setUriParts(uri);
    }

    @Override
    public URI build(Object... values) throws IllegalArgumentException, UriBuilderException {
        return doBuild(false, true, values);
    }

    private static Map<String, Object> getResolvedTemplates(Map<String, Object> rtemplates) {
        return rtemplates == null
            ? Collections.<String, Object>emptyMap() : new LinkedHashMap<String, Object>(rtemplates);
    }
    
    private URI doBuild(boolean fromEncoded, boolean encodePathSlash, Object... values) {
        if (values == null) {
            throw new IllegalArgumentException("Template parameter values are set to null");
        }
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                throw new IllegalArgumentException("Template parameter value at position " + i + " is set to null");
            }
        }
        
        UriParts parts = doBuildUriParts(fromEncoded, encodePathSlash, false, values);
        try {
            return buildURI(fromEncoded, parts.path, parts.query, parts.fragment);
        } catch (URISyntaxException ex) {
            throw new UriBuilderException("URI can not be built", ex);
        }
    }
    
    private UriParts doBuildUriParts(boolean fromEncoded, boolean encodePathSlash, 
                                     boolean allowUnresolved, Object... values) {
        
        Map<String, Object> alreadyResolvedTs = getResolvedTemplates(resolvedTemplates);
        Map<String, Object> alreadyResolvedTsPathEnc = getResolvedTemplates(resolvedTemplatesPathEnc);
        Map<String, Object> alreadyResolvedEncTs = getResolvedTemplates(resolvedEncodedTemplates);
        final int resolvedTsSize = alreadyResolvedTs.size() 
            + alreadyResolvedEncTs.size()
            + alreadyResolvedTsPathEnc.size();
        
        String thePath = buildPath();
        URITemplate pathTempl = URITemplate.createExactTemplate(thePath);
        thePath = substituteVarargs(pathTempl, alreadyResolvedTs, alreadyResolvedTsPathEnc, 
                                    alreadyResolvedEncTs, values, 0, false, fromEncoded, 
                                    allowUnresolved, encodePathSlash);
        int pathTemplateVarsSize = pathTempl.getVariables().size();
        
        String theQuery = buildQuery();
        int queryTemplateVarsSize = 0;
        if (theQuery != null) {
            URITemplate queryTempl = URITemplate.createExactTemplate(theQuery);
            queryTemplateVarsSize = queryTempl.getVariables().size();
            if (queryTemplateVarsSize > 0) {
                int lengthDiff = values.length + resolvedTsSize 
                    - alreadyResolvedTs.size() - alreadyResolvedTsPathEnc.size() - alreadyResolvedEncTs.size() 
                    - pathTemplateVarsSize; 
                theQuery = substituteVarargs(queryTempl, alreadyResolvedTs, alreadyResolvedTsPathEnc, 
                                             alreadyResolvedEncTs, values, values.length - lengthDiff, 
                                             true, fromEncoded, allowUnresolved, false);
            }
        }
        
        String theFragment = fragment;
        if (theFragment != null) {
            URITemplate fragmentTempl = URITemplate.createExactTemplate(theFragment);
            if (fragmentTempl.getVariables().size() > 0) {
                int lengthDiff = values.length  + resolvedTsSize 
                    - alreadyResolvedTs.size() - alreadyResolvedTsPathEnc.size() - alreadyResolvedEncTs.size()
                    - pathTemplateVarsSize - queryTemplateVarsSize; 
                theFragment = substituteVarargs(fragmentTempl, alreadyResolvedTs, alreadyResolvedTsPathEnc, 
                                                alreadyResolvedEncTs, values, values.length - lengthDiff, 
                                                true, fromEncoded, allowUnresolved, false);
            }
        }
        
        return new UriParts(thePath, theQuery, theFragment);
    }
    
    private URI buildURI(boolean fromEncoded, String thePath, String theQuery, String theFragment) 
        throws URISyntaxException {
        if (fromEncoded) { 
            return buildURIFromEncoded(thePath, theQuery, theFragment);
        } else if (!isSchemeOpaque()) {
            if ((scheme != null || host != null || userInfo != null)
                && thePath.length() != 0 && !(thePath.startsWith("/") || thePath.startsWith(";"))) {
                thePath = "/" + thePath;
            }
            try {
                return buildURIFromEncoded(thePath, theQuery, theFragment);
            } catch (Exception ex) {
                // lets try the option below
            }
            URI uri = new URI(scheme, userInfo, host, port, 
                           thePath, theQuery, theFragment);
            if (thePath.contains("%2F")) {
                // TODO: the bogus case of segments containing encoded '/'
                // Not sure if we have a cleaner solution though.
                String realPath = uri.getRawPath().replace("%252F", "%2F");
                uri = buildURIFromEncoded(realPath, uri.getRawQuery(), uri.getRawFragment());
            }
            return uri;
        } else {
            return new URI(scheme, schemeSpecificPart, theFragment);
        }
    }
    
    private URI buildURIFromEncoded(String thePath, String theQuery, String theFragment) 
        throws URISyntaxException {
        return new URI(buildUriString(thePath, theQuery, theFragment));
    }
    
    private String buildUriString(String thePath, String theQuery, String theFragment) {
        StringBuilder b = new StringBuilder();
        if (scheme != null) {
            b.append(scheme).append(":");
        }
        if (!isSchemeOpaque()) {
            if (scheme != null) {
                b.append("//");
            }
            if (userInfo != null) {
                b.append(userInfo).append('@');
            }
            if (host != null) {
                b.append(host);
            }
            if (port != -1) {
                b.append(':').append(port);    
            }
            if (thePath != null && thePath.length() > 0) {
                b.append(thePath.startsWith("/") || b.length() == 0 || originalPathEmpty 
                    ? thePath : '/' + thePath);
            }
            if (theQuery != null && theQuery.length() != 0) {
                b.append('?').append(theQuery);
            }
        } else {
            b.append(schemeSpecificPart);
        }
        if (theFragment != null) {
            b.append('#').append(theFragment);
        }
        return b.toString();
    }
    
    private boolean isSchemeOpaque() {
        return schemeSpecificPart != null;
    }
    
    @Override
    public URI buildFromEncoded(Object... values) throws IllegalArgumentException, UriBuilderException {
        return doBuild(true, false, values);
    }

    @Override
    public URI buildFromMap(Map<String, ?> map) throws IllegalArgumentException,
        UriBuilderException {
        return doBuildFromMap(map, false, true);
    }

    private URI doBuildFromMap(Map<String, ? extends Object> map, boolean fromEncoded, 
                               boolean encodePathSlash) 
        throws IllegalArgumentException, UriBuilderException {
        try {
            Map<String, Object> alreadyResolvedTs = getResolvedTemplates(resolvedTemplates);
            Map<String, Object> alreadyResolvedTsPathEnc = getResolvedTemplates(resolvedTemplatesPathEnc);
            Map<String, Object> alreadyResolvedEncTs = getResolvedTemplates(resolvedEncodedTemplates);
                        
            String thePath = buildPath();
            thePath = substituteMapped(thePath, map, alreadyResolvedTs, alreadyResolvedTsPathEnc, 
                                       alreadyResolvedEncTs, false, fromEncoded, encodePathSlash);
            
            String theQuery = buildQuery();
            if (theQuery != null) {
                theQuery = substituteMapped(theQuery, map, alreadyResolvedTs, alreadyResolvedTsPathEnc, 
                                            alreadyResolvedEncTs, true, fromEncoded, false);
            }
            
            String theFragment = fragment == null 
                ? null : substituteMapped(fragment, map, alreadyResolvedTs, alreadyResolvedTsPathEnc, 
                                          alreadyResolvedEncTs, true, fromEncoded, encodePathSlash);
            
            return buildURI(fromEncoded, thePath, theQuery, theFragment);
        } catch (URISyntaxException ex) {
            throw new UriBuilderException("URI can not be built", ex);
        }
    }
    //CHECKSTYLE:OFF
    private String substituteVarargs(URITemplate templ, 
                                     Map<String, Object> alreadyResolvedTs,
                                     Map<String, Object> alreadyResolvedTsPathEnc,
                                     Map<String, Object> alreadyResolvedTsEnc,
                                     Object[] values, 
                                     int ind,
                                     boolean isQuery,
                                     boolean fromEncoded,
                                     boolean allowUnresolved,
                                     boolean encodePathSlash) {
        
   //CHECKSTYLE:ON     
        Map<String, String> varValueMap = new HashMap<String, String>();
        
        // vars in set are properly ordered due to linking in hash set
        Set<String> uniqueVars = new LinkedHashSet<String>(templ.getVariables());
        if (!allowUnresolved && values.length + alreadyResolvedTs.size() + alreadyResolvedTsEnc.size()
            + alreadyResolvedTsPathEnc.size() < uniqueVars.size()) {
            throw new IllegalArgumentException("Unresolved variables; only " + values.length
                                               + " value(s) given for " + uniqueVars.size()
                                               + " unique variable(s)");
        }
        int idx = ind;
        Set<String> pathEncodeVars = alreadyResolvedTsPathEnc.isEmpty() && !encodePathSlash 
            ? Collections.<String>emptySet() : new HashSet<String>();
        for (String var : uniqueVars) {
            
            boolean resolvedPathVarHasToBeEncoded = alreadyResolvedTsPathEnc.containsKey(var);
            boolean varValueHasToBeEncoded = resolvedPathVarHasToBeEncoded || alreadyResolvedTs.containsKey(var);
            
            Map<String, Object> resolved = !varValueHasToBeEncoded ? alreadyResolvedTsEnc 
                : resolvedPathVarHasToBeEncoded ? alreadyResolvedTsPathEnc : alreadyResolvedTs;
            Object oval = resolved.isEmpty() ? null : resolved.remove(var);
            boolean valueFromEncodedMap = false;
            if (oval == null) {
                if (allowUnresolved) {
                    continue;
                }
                oval = values[idx++];
            } else {
                valueFromEncodedMap = resolved == alreadyResolvedTsEnc;
            }
            
            if (oval == null) {
                throw new IllegalArgumentException("No object for " + var);
            }
            String value = oval.toString();
            if (fromEncoded || valueFromEncodedMap) {
                value = HttpUtils.encodePartiallyEncoded(value, isQuery);
            } else {
                value = isQuery ? HttpUtils.queryEncode(value) : HttpUtils.pathEncode(value);
            }
            
            varValueMap.put(var, value);
            
            if (!isQuery && (resolvedPathVarHasToBeEncoded 
                || encodePathSlash && !varValueHasToBeEncoded)) {
                pathEncodeVars.add(var);
            }
            
        }
        return templ.substitute(varValueMap, pathEncodeVars, allowUnresolved);
    }
    
    //CHECKSTYLE:OFF
    private String substituteMapped(String path, 
                                    Map<String, ? extends Object> varValueMap,
                                    Map<String, Object> alreadyResolvedTs,
                                    Map<String, Object> alreadyResolvedTsPathEnc,
                                    Map<String, Object> alreadyResolvedTsEnc,
                                    boolean isQuery,
                                    boolean fromEncoded,
                                    boolean encodePathSlash) {
    //CHECKSTYLE:ON
        URITemplate templ = URITemplate.createExactTemplate(path);
        
        Set<String> uniqueVars = new HashSet<String>(templ.getVariables());
        if (varValueMap.size() + alreadyResolvedTs.size() + alreadyResolvedTsEnc.size()
            + alreadyResolvedTsPathEnc.size() < uniqueVars.size()) {
            throw new IllegalArgumentException("Unresolved variables; only " + varValueMap.size()
                                               + " value(s) given for " + uniqueVars.size()
                                               + " unique variable(s)");
        }
        
        Set<String> pathEncodeVars = alreadyResolvedTsPathEnc.isEmpty() && !encodePathSlash 
            ? Collections.<String>emptySet() : new HashSet<String>();
        
        Map<String, Object> theMap = new LinkedHashMap<String, Object>(); 
        for (String var : uniqueVars) {
            boolean isPathEncVar = !isQuery && alreadyResolvedTsPathEnc.containsKey(var);
            
            boolean isVarEncoded = isPathEncVar || alreadyResolvedTs.containsKey(var) ? false : true;
            Map<String, Object> resolved = isVarEncoded ? alreadyResolvedTsEnc 
                : isPathEncVar ? alreadyResolvedTsPathEnc : alreadyResolvedTs;
            Object oval = resolved.isEmpty() ? null : resolved.remove(var);
            if (oval == null) {
                oval = varValueMap.get(var);
            }  
            if (oval == null) {
                throw new IllegalArgumentException("No object for " + var);
            }
            if (fromEncoded) {
                oval = HttpUtils.encodePartiallyEncoded(oval.toString(), isQuery);
            } else {
                oval = isQuery ? HttpUtils.queryEncode(oval.toString()) : HttpUtils.pathEncode(oval.toString());
            }
            theMap.put(var, oval);
            if (!isQuery && (isPathEncVar || encodePathSlash)) {
                pathEncodeVars.add(var);
            }
        }
        return templ.substitute(theMap, pathEncodeVars, false);
    }

    @Override
    public URI buildFromEncodedMap(Map<String, ?> map) throws IllegalArgumentException,
        UriBuilderException {
        
        Map<String, String> decodedMap = new HashMap<String, String>(map.size());
        for (Map.Entry<String, ? extends Object> entry : map.entrySet()) {
            if (entry.getValue() == null) {
                throw new IllegalArgumentException("Value is null");
            }
            String theValue = entry.getValue().toString();
            if (theValue.contains("/")) {
                // protecting '/' from being encoded here assumes that a given value may constitute multiple
                // path segments - very questionable especially given that queries and fragments may also 
                // contain template vars - technically this can be covered by checking where a given template
                // var is coming from and act accordingly. Confusing nonetheless.
                StringBuilder buf = new StringBuilder();
                String[] values = StringUtils.split(theValue, "/");
                for (int i = 0; i < values.length; i++) {
                    buf.append(HttpUtils.encodePartiallyEncoded(values[i], false));
                    if (i + 1 < values.length) {
                        buf.append("/");
                    }
                }
                decodedMap.put(entry.getKey(), buf.toString());
            } else {
                decodedMap.put(entry.getKey(), HttpUtils.encodePartiallyEncoded(theValue, false));
            }
            
        }
        return doBuildFromMap(decodedMap, true, false);
    }

    // CHECKSTYLE:OFF
    @Override
    public UriBuilder clone() {
        UriBuilderImpl builder = new UriBuilderImpl();
        builder.scheme = scheme;
        builder.userInfo = userInfo;
        builder.port = port;
        builder.host = host;
        builder.paths = new ArrayList<PathSegment>(paths);
        builder.fragment = fragment;
        builder.query = new MetadataMap<String, String>(query);
        builder.matrix = new MetadataMap<String, String>(matrix);
        builder.schemeSpecificPart = schemeSpecificPart;
        builder.leadingSlash = leadingSlash;
        builder.originalPathEmpty = originalPathEmpty;
        builder.resolvedEncodedTemplates = 
            resolvedEncodedTemplates == null ? null : new HashMap<String, Object>(resolvedEncodedTemplates);
        builder.resolvedTemplates = 
            resolvedTemplates == null ? null : new HashMap<String, Object>(resolvedTemplates);
        builder.resolvedTemplatesPathEnc = 
            resolvedTemplatesPathEnc == null ? null : new HashMap<String, Object>(resolvedTemplatesPathEnc);
        return builder;
    }
    // CHECKSTYLE:ON
    
    @Override
    public UriBuilder fragment(String theFragment) throws IllegalArgumentException {
        this.fragment = theFragment;
        return this;
    }

    @Override
    public UriBuilder host(String theHost) throws IllegalArgumentException {
        if ("".equals(theHost)) {
            throw new IllegalArgumentException("Host cannot be empty");
        }
        this.host = theHost;
        return this;
    }

    @Override
    public UriBuilder path(@SuppressWarnings("rawtypes") Class resource) throws IllegalArgumentException {
        if (resource == null) {
            throw new IllegalArgumentException("resource is null");
        }
        Class<?> cls = resource;
        Path ann = cls.getAnnotation(Path.class);
        if (ann == null) {
            throw new IllegalArgumentException("Class '" + resource.getCanonicalName()
                                               + "' is not annotated with Path");
        }
        // path(String) decomposes multi-segment path when necessary
        return path(ann.value());
    }

    @Override
    public UriBuilder path(@SuppressWarnings("rawtypes") Class resource, String method) 
        throws IllegalArgumentException {
        if (resource == null) {
            throw new IllegalArgumentException("resource is null");
        }
        if (method == null) {
            throw new IllegalArgumentException("method is null");
        }
        Path foundAnn = null;
        for (Method meth : resource.getMethods()) {
            if (meth.getName().equals(method)) {
                Path ann = meth.getAnnotation(Path.class);
                if (foundAnn != null && ann != null) {
                    throw new IllegalArgumentException("Multiple Path annotations for '" + method
                        + "' overloaded method");
                }
                foundAnn = ann;
            }
        }
        if (foundAnn == null) {
            throw new IllegalArgumentException("No Path annotation for '" + method + "' method");
        }
        // path(String) decomposes multi-segment path when necessary
        return path(foundAnn.value());
    }
    

    @Override
    public UriBuilder path(Method method) throws IllegalArgumentException {
        if (method == null) {
            throw new IllegalArgumentException("method is null");
        }
        Path ann = method.getAnnotation(Path.class);
        if (ann == null) {
            throw new IllegalArgumentException("Method '" + method.getClass().getCanonicalName() + "."
                                               + method.getName() + "' is not annotated with Path");
        }
        // path(String) decomposes multi-segment path when necessary
        return path(ann.value());
    }

    @Override
    public UriBuilder path(String path) throws IllegalArgumentException {
        return doPath(path, true);
    }

    private UriBuilder doPath(String path, boolean checkSegments) {
        if (path == null) {
            throw new IllegalArgumentException("path is null");
        }
        if (isAbsoluteUriPath(path)) {
            try {
                URI uri = URI.create(path);
                this.originalPathEmpty = StringUtils.isEmpty(uri.getPath());
                uri(uri);
            } catch (IllegalArgumentException ex) {
                if (!URITemplate.createExactTemplate(path).getVariables().isEmpty()) {
                    return uriAsTemplate(path);
                }
                String pathEncoded = HttpUtils.pathEncode(path);
                // Bad hack to bypass the TCK usage of bogus URI with empty paths containing matrix parameters, 
                // which even URI class chokes upon; cheaper to do the following than try to challenge,
                // given that URI RFC mentions the possibility of empty paths, though no word on the possibility of
                // such empty paths having matrix parameters... 
                int schemeIndex = pathEncoded.indexOf("//");
                if (schemeIndex != -1) {
                    int pathComponentStart = pathEncoded.indexOf("/", schemeIndex + 2);
                    if (pathComponentStart == -1) {
                        this.originalPathEmpty = true;
                        pathComponentStart = pathEncoded.indexOf(";");
                        if (pathComponentStart != -1) {
                            pathEncoded = pathEncoded.substring(0, pathComponentStart)
                                + "/" + pathEncoded.substring(pathComponentStart);
                        }
                    }
                }
                setUriParts(URI.create(pathEncoded));
            } 
            return this;
        }
        
        if (paths.isEmpty()) {
            leadingSlash = path.startsWith("/");
        }
        
        List<PathSegment> segments;
        if (checkSegments) { 
            segments = JAXRSUtils.getPathSegments(path, false, false);
        } else {
            segments = new ArrayList<PathSegment>();
            path = path.replaceAll("/", "%2F");
            segments.add(new PathSegmentImpl(path, false));
        }
        if (!paths.isEmpty() && !matrix.isEmpty()) {
            PathSegment ps = paths.remove(paths.size() - 1);
            paths.add(replacePathSegment(ps));
        }
        paths.addAll(segments);
        matrix.clear();
        if (!paths.isEmpty()) {
            matrix = paths.get(paths.size() - 1).getMatrixParameters();        
        }
        return this;
    }
    
    @Override
    public UriBuilder port(int thePort) throws IllegalArgumentException {
        if (thePort < 0 && thePort != -1) {
            throw new IllegalArgumentException("Port cannot be negative");
        }
        this.port = thePort;
        return this;
    }

    @Override
    public UriBuilder scheme(String s) throws IllegalArgumentException {
        scheme = s;
        return this;
    }

    @Override
    public UriBuilder schemeSpecificPart(String ssp) throws IllegalArgumentException {
        // scheme-specific part is whatever after ":" of URI
        // see: http://en.wikipedia.org/wiki/URI_scheme
        try {
            if (scheme == null) {
                scheme = "http";
            }
            URI uri = new URI(scheme, ssp, fragment);
            setUriParts(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Wrong syntax of scheme-specific part", e);
        }
        return this;
    }

    @Override
    public UriBuilder uri(URI uri) throws IllegalArgumentException {
        setUriParts(uri);
        return this;
    }

    @Override
    public UriBuilder userInfo(String ui) throws IllegalArgumentException {
        this.userInfo = ui;
        return this;
    }

    private void setUriParts(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("uri is null");
        }
        String theScheme = uri.getScheme();
        if (theScheme != null) {
            scheme = theScheme;
        }
        String rawPath = uri.getRawPath();
        if (!uri.isOpaque() && schemeSpecificPart == null
            && (theScheme != null || rawPath != null)) {
            port = uri.getPort();
            host = uri.getHost();
            if (rawPath != null) {
                setPathAndMatrix(rawPath);
            }
            String rawQuery = uri.getRawQuery();
            if (rawQuery != null) {
                query = JAXRSUtils.getStructuredParams(rawQuery, "&", false, true);
            }
            userInfo = uri.getUserInfo();
            schemeSpecificPart = null;
        } else {
            schemeSpecificPart = uri.getSchemeSpecificPart();
        }
        String theFragment = uri.getFragment();
        if (theFragment != null) {
            fragment = theFragment;
        }
    }

    private void setPathAndMatrix(String path) {
        leadingSlash = !originalPathEmpty && path.startsWith("/");
        paths = JAXRSUtils.getPathSegments(path, false, false);
        if (!paths.isEmpty()) {
            matrix = paths.get(paths.size() - 1).getMatrixParameters();
        } else {
            matrix.clear();
        }
    }
    
    private String buildPath() {
        StringBuilder sb = new StringBuilder();
        Iterator<PathSegment> iter = paths.iterator();
        while (iter.hasNext()) {
            PathSegment ps = iter.next();
            String p = ps.getPath();
            if (p.length() != 0 || !iter.hasNext()) {
                p = URITemplate.createExactTemplate(p).encodeLiteralCharacters(false);
                if (sb.length() == 0 && leadingSlash) {
                    sb.append('/');
                } else if (!p.startsWith("/") && sb.length() > 0) {
                    sb.append('/');
                }
                sb.append(p);
                if (iter.hasNext()) {
                    buildMatrix(sb, ps.getMatrixParameters());
                }
            }
        }
        buildMatrix(sb, matrix);
        return sb.toString();
    }

    private String buildQuery() {
        return buildParams(query, '&');
    }

    @Override
    public UriBuilder matrixParam(String name, Object... values) throws IllegalArgumentException {
        if (name == null || values == null) {
            throw new IllegalArgumentException("name or values is null");
        }
        List<String> list = matrix.get(name);
        if (list == null) {
            matrix.put(name, toStringList(true, values));
        } else {
            list.addAll(toStringList(true, values));
        }
        return this;
    }

    @Override
    public UriBuilder queryParam(String name, Object... values) throws IllegalArgumentException {
        if (name == null || values == null) {
            throw new IllegalArgumentException("name or values is null");
        }
        List<String> list = query.get(name);
        if (list == null) {
            query.put(name, toStringList(false, values));
        } else {
            list.addAll(toStringList(false, values));
        }
        return this;
    }

    @Override
    public UriBuilder replaceMatrix(String matrixValues) throws IllegalArgumentException {
        String encodedMatrixValues = matrixValues != null ? HttpUtils.pathEncode(matrixValues) : null;
        this.matrix = JAXRSUtils.getStructuredParams(encodedMatrixValues, ";", true, false);
        return this;
    }

    @Override
    public UriBuilder replaceMatrixParam(String name, Object... values) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (values != null && values.length >= 1 && values[0] != null) {
            matrix.put(name, toStringList(true, values));
        } else {
            matrix.remove(name);
        }
        return this;
    }

    @Override
    public UriBuilder replacePath(String path) {
        if (path == null) {
            clearPathAndMatrix();
        } else if (isAbsoluteUriPath(path)) {
            clearPathAndMatrix();
            uri(URI.create(path));
        } else {
            setPathAndMatrix(path);
        }
        return this;
    }

    private void clearPathAndMatrix() {
        paths.clear();
        matrix.clear();
    }
    
    private boolean isAbsoluteUriPath(String path) {
        // This is the cheapest way to figure out if a given path is an absolute 
        // URI with the http(s) scheme, more expensive way is to always convert 
        // a path to URI and check if it starts from some scheme or not
        
        // Given that the list of schemes can be open-ended it is recommended that
        // UriBuilder.fromUri is called instead for schemes like 'file', 'jms', etc
        // be supported though the use of non-http schemes for *building* new URIs
        // is pretty limited in the context of working with JAX-RS services
         
        return path.startsWith("http:") || path.startsWith("https:");
    }
    
    @Override
    public UriBuilder replaceQuery(String queryValue) throws IllegalArgumentException {
        if (queryValue != null) {
            // workaround to do with a conflicting and confusing requirement where spaces 
            // passed as part of replaceQuery are encoded as %20 while those passed as part 
            // of quertyParam are encoded as '+'
            queryValue = queryValue.replace(" ", "%20");
        }
        query = JAXRSUtils.getStructuredParams(queryValue, "&", false, true);
        return this;
    }

    @Override
    public UriBuilder replaceQueryParam(String name, Object... values) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (values != null && values.length >= 1 && values[0] != null) {
            query.put(name, toStringList(false, values));
        } else {
            query.remove(name);
        }
        return this;
    }

    @Override
    public UriBuilder segment(String... segments) throws IllegalArgumentException {
        if (segments == null) {
            throw new IllegalArgumentException("Segments should not be null");
        }
        for (String segment : segments) {
            doPath(segment, false);
        }
        return this;
    }

    /**
     * Query or matrix params convertion from object values vararg to list of strings. No encoding is
     * provided.
     * 
     * @param values entry vararg values
     * @return list of strings
     * @throws IllegalArgumentException when one of values is null
     */
    private List<String> toStringList(boolean encodeSlash, Object... values) throws IllegalArgumentException {
        List<String> list = new ArrayList<String>();
        if (values != null && values.length > 0) {
            for (int i = 0; i < values.length; i++) {
                Object value = values[i];
                if (value == null) {
                    throw new IllegalArgumentException("Null value on " + i + " position");
                }
                String strValue = value.toString();
                if (encodeSlash) {
                    strValue = strValue.replaceAll("/", "%2F");
                }
                list.add(strValue);
            }
        } else {
            list.add(null);
        }
        return list;
    }

    /**
     * Builds param string for query part or matrix part of URI.
     * 
     * @param map query or matrix multivalued map
     * @param separator params separator, '&' for query ';' for matrix
     * @param fromEncoded if true then values will be decoded 
     * @return stringified params.
     */
    private String buildParams(MultivaluedMap<String, String> map, char separator) {
        boolean isQuery = separator == '&';
        StringBuilder b = new StringBuilder();
        for (Iterator<Map.Entry<String, List<String>>> it = map.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, List<String>> entry = it.next();
            for (Iterator<String> sit = entry.getValue().iterator(); sit.hasNext();) {
                String val = sit.next();
                b.append(entry.getKey());
                if (val != null) {
                    boolean templateValue = val.startsWith("{") && val.endsWith("}");
                    if (!templateValue) {
                        val = HttpUtils.encodePartiallyEncoded(val, isQuery);
                        if (!isQuery) {
                            val = val.replaceAll("/", "%2F");
                        }
                    } else {
                        val = URITemplate.createExactTemplate(val).encodeLiteralCharacters(isQuery);
                    }
                    if (!val.isEmpty()) {
                        b.append('='); // Liberty Change
                        b.append(val);
                    }
                }
                if (sit.hasNext() || it.hasNext()) {
                    b.append(separator);
                }
            }
        }
        return b.length() > 0 ? b.toString() : null;
    }
    
    /**
     * Builds param string for matrix part of URI.
     * 
     * @param sb buffer to add the matrix part to, will get ';' added if map is not empty 
     * @param map matrix multivalued map
     */    
    private void buildMatrix(StringBuilder sb, MultivaluedMap<String, String> map) {
        if (!map.isEmpty()) {
            sb.append(';');
            sb.append(buildParams(map, ';'));
        }
    }
    
    private PathSegment replacePathSegment(PathSegment ps) {
        StringBuilder sb = new StringBuilder();
        sb.append(ps.getPath());
        buildMatrix(sb, matrix);
        return new PathSegmentImpl(sb.toString());
    }

    @FFDCIgnore(Exception.class) // Liberty Change
    public UriBuilder uri(String uriTemplate) throws IllegalArgumentException {
        if (StringUtils.isEmpty(uriTemplate)) {
            throw new IllegalArgumentException();
        }
        try {
            return uri(URI.create(uriTemplate));
        } catch (Exception ex) {
            if (URITemplate.createExactTemplate(uriTemplate).getVariables().isEmpty()) {
                throw new IllegalArgumentException(ex);    
            } else {
                return uriAsTemplate(uriTemplate);
            }
            
        }
    }

    public UriBuilder uriAsTemplate(String uri) {
        // This can be a start of replacing URI class Parser completely
        // but it can be too complicated, the following code is needed for now 
        // to deal with URIs containing template variables. 
        int index = uri.indexOf(":");
        if (index != -1) {
            this.scheme = uri.substring(0, index);
            uri = uri.substring(index + 1);
            if (uri.indexOf("//") == 0) {
                uri = uri.substring(2);
                index = uri.indexOf("/");
                if (index != -1) {
                    String[] schemePair = uri.substring(0, index).split(":");
                    this.host = schemePair[0];
                    this.port = schemePair.length == 2 ? Integer.parseInt(schemePair[1]) : -1;
                    
                }
                uri = uri.substring(index);
            }
            
        }
        String rawQuery = null;
        index = uri.indexOf("?");
        if (index != -1) {
            rawQuery = uri.substring(index + 1);
            uri = uri.substring(0, index);
        }
        setPathAndMatrix(uri);
        if (rawQuery != null) {
            query = JAXRSUtils.getStructuredParams(rawQuery, "&", false, true);
        }
        
        return this;
    }
    
    //the clarified rules for encoding values of uri templates are:
    //  - encode each value contextually based on the URI component containing the template
    //  - in path templates, by default, encode also slashes (i.e. treat all path templates as 
    //    part of a single path segment, to be consistent with @Path annotation templates)
    //  - for special cases when the slash encoding in path templates is not desired, 
    //    users may use the newly added build methods to override the default behavior
    
    @Override
    public URI build(Object[] vars, boolean encodePathSlash) throws IllegalArgumentException, UriBuilderException {
        return doBuild(false, encodePathSlash, vars);
    }

    @Override
    public URI buildFromMap(Map<String, ?> map, boolean encodePathSlash) throws IllegalArgumentException,
        UriBuilderException {
        return doBuildFromMap(map, false, encodePathSlash);
    }


    @Override
    public String toTemplate() {
        UriParts parts = doBuildUriParts(false, false, true);
        return buildUriString(parts.path, parts.query, parts.fragment);
    }
    
    @Override
    public UriBuilder resolveTemplate(String name, Object value) throws IllegalArgumentException {
        return resolveTemplate(name, value, true);
    }
    
    @Override
    public UriBuilder resolveTemplate(String name, Object value, boolean encodePathSlash) 
        throws IllegalArgumentException {
        return resolveTemplates(Collections.singletonMap(name, value), encodePathSlash);
    }
    
    @Override
    public UriBuilder resolveTemplates(Map<String, Object> values) throws IllegalArgumentException {
        return resolveTemplates(values, true);
    }
    
    @Override
    public UriBuilder resolveTemplates(Map<String, Object> values, boolean encodePathSlash) 
        throws IllegalArgumentException {
        if (encodePathSlash) {
            resolvedTemplatesPathEnc = fillInResolveTemplates(resolvedTemplatesPathEnc, values);
        } else {
            resolvedTemplates = fillInResolveTemplates(resolvedTemplates, values);
        }
        return this;
    }
    
    @Override
    public UriBuilder resolveTemplateFromEncoded(String name, Object value) throws IllegalArgumentException {
        return resolveTemplatesFromEncoded(Collections.singletonMap(name, value));
    }
    
    @Override
    public UriBuilder resolveTemplatesFromEncoded(Map<String, Object> values) 
        throws IllegalArgumentException {
        resolvedEncodedTemplates = fillInResolveTemplates(resolvedEncodedTemplates, values);
        return this;
    }
    
    private static Map<String, Object> fillInResolveTemplates(Map<String, Object> map, Map<String, Object> values) 
        throws IllegalArgumentException {
        if (values == null) {
            throw new IllegalArgumentException();
        }
        if (map == null) {
            map = new LinkedHashMap<String, Object>();
        }

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new IllegalArgumentException();
            }
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }
    
    private static class UriParts {
        String path;
        String query;
        String fragment;
        
        UriParts(String path, String query, String fragment) {
            this.path = path;
            this.query = query;
            this.fragment = fragment;
        }
    }
}
