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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.model.MethodInvocationInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfoStack;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.MessageUtils;

public class UriInfoImpl implements UriInfo {
    private static final Logger LOG = LogUtils.getL7dLogger(UriInfoImpl.class);
    private static final String CASE_INSENSITIVE_QUERIES = "org.apache.cxf.http.case_insensitive_queries";
    private static final String PARSE_QUERY_VALUE_AS_COLLECTION = "parse.query.value.as.collection";

    private MultivaluedMap<String, String> templateParams;
    private Message message;
    private OperationResourceInfoStack stack;
    private boolean caseInsensitiveQueries;
    private boolean queryValueIsCollection;

    @SuppressWarnings("unchecked")
    public UriInfoImpl(Message m) {
        //Liberty code change start
        this(m, (MultivaluedMap<String, String>)((MessageImpl) m).getTemplateParameters());
        //Liberty code change end
    }

    public UriInfoImpl(Message m, MultivaluedMap<String, String> templateParams) {
        this.message = m;
        this.templateParams = templateParams;
        if (m != null) {
            //Liberty code change start
            this.stack = (OperationResourceInfoStack) ((MessageImpl) m).getOperationResourceInfoStack();
            //Liberty code change end
            this.caseInsensitiveQueries =
                MessageUtils.getContextualBoolean(m, CASE_INSENSITIVE_QUERIES);
            this.queryValueIsCollection =
                MessageUtils.getContextualBoolean(m, PARSE_QUERY_VALUE_AS_COLLECTION);
        }
    }

    public URI getAbsolutePath() {
        String path = getAbsolutePathAsString();
        return URI.create(path);
    }

    public UriBuilder getAbsolutePathBuilder() {
        return new UriBuilderImpl(getAbsolutePath());
    }

    public URI getBaseUri() {
        URI u = URI.create(HttpUtils.getEndpointAddress(message));
        return HttpUtils.toAbsoluteUri(u, message);
    }

    public UriBuilder getBaseUriBuilder() {
        return new UriBuilderImpl(getBaseUri());
    }

    public String getPath() {
        return getPath(true);
    }

    public String getPath(boolean decode) {
        String value = doGetPath(decode, true);
        if (value.length() > 1 && value.startsWith("/")) {
            return value.substring(1);
        }
        return value;
    }

    public List<PathSegment> getPathSegments() {
        return getPathSegments(true);
    }

    public List<PathSegment> getPathSegments(boolean decode) {
        return JAXRSUtils.getPathSegments(getPath(false), decode);
    }

    public MultivaluedMap<String, String> getQueryParameters() {
        return getQueryParameters(true);
    }

    public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
        MultivaluedMap<String, String> queries = !caseInsensitiveQueries
            ? new MetadataMap<String, String>() : new MetadataMap<String, String>(false, true);
        //Liberty code change start
        JAXRSUtils.getStructuredParams(queries, (String)((MessageImpl) message).getQueryString(),
                                      "&", decode, decode, queryValueIsCollection);
        //Liberty code change end
        return queries;

    }

    public URI getRequestUri() {
        String path = getAbsolutePathAsString();
        //Liberty code change start
        String queries = (String)((MessageImpl) message).getQueryString();
        //Liberty code change end
        if (queries != null) {
            path += "?" + queries;
        }
        return URI.create(path);
    }

    public UriBuilder getRequestUriBuilder() {
        return new UriBuilderImpl(getRequestUri());
    }

    public MultivaluedMap<String, String> getPathParameters() {
        return getPathParameters(true);
    }

    public MultivaluedMap<String, String> getPathParameters(boolean decode) {
        MetadataMap<String, String> values = new MetadataMap<>();
        if (templateParams == null) {
            return values;
        }
        for (Map.Entry<String, List<String>> entry : templateParams.entrySet()) {
            if (entry.getKey().equals(URITemplate.FINAL_MATCH_GROUP)) {
                continue;
            }
            values.add(entry.getKey(), decode ? HttpUtils.pathDecode(entry.getValue().get(0)) : entry
                .getValue().get(0));
        }
        return values;
    }

    public List<Object> getMatchedResources() {
        if (stack != null) {
            List<Object> resources = new ArrayList<>(stack.size());
            for (MethodInvocationInfo invocation : stack) {
                resources.add(0, invocation.getRealClass());
            }
            return resources;
        }
        LOG.fine("No resource stack information, returning empty list");
        return Collections.emptyList();
    }

    public List<String> getMatchedURIs() {
        return getMatchedURIs(true);
    }

    public List<String> getMatchedURIs(boolean decode) {
        if (stack != null) {
            List<String> objects = new ArrayList<>();
            List<String> uris = new LinkedList<>();
            StringBuilder sumPath = new StringBuilder("");
            for (MethodInvocationInfo invocation : stack) {
                List<String> templateObjects = invocation.getTemplateValues();
                OperationResourceInfo ori = invocation.getMethodInfo();
                URITemplate[] paths = {
                    ori.getClassResourceInfo().getURITemplate(),
                    ori.getURITemplate()
                };
                if (paths[0] != null) {
                    int count = paths[0].getVariables().size();
                    List<String> rootObjects = new ArrayList<>(count);
                    for (int i = 0; i < count && i < templateObjects.size(); i++) {
                        rootObjects.add(templateObjects.get(i));
                    }
                    uris.add(0, createMatchedPath(paths[0].getValue(), rootObjects, decode));
                }
                if (paths[1] != null && paths[1].getValue().length() > 1) {
                    for (URITemplate t : paths) {
                        if (t != null) {
                            sumPath.append('/').append(t.getValue());
                        }
                    }
                    objects.addAll(templateObjects);
                    uris.add(0, createMatchedPath(sumPath.toString(), objects, decode));
                }
            }
            return uris;
        }
        LOG.fine("No resource stack information, returning empty list");
        return Collections.emptyList();
    }

    private static String createMatchedPath(String uri, List<? extends Object> vars, boolean decode) {
        String uriPath = UriBuilder.fromPath(uri).buildFromEncoded(vars.toArray()).getRawPath();
        uriPath = decode ? HttpUtils.pathDecode(uriPath) : uriPath;
        if (uriPath.startsWith("/")) {
            uriPath = uriPath.substring(1);
        }
        return uriPath;
    }
    private String doGetPath(boolean decode, boolean addSlash) {
        String path = HttpUtils.getPathToMatch(message, addSlash);
        return decode ? HttpUtils.pathDecode(path) : path;
    }

    private String getAbsolutePathAsString() {
        String address = getBaseUri().toString();
        if (MessageUtils.isRequestor(message)) {
            return address;
        }
        String path = doGetPath(false, false);
        if (path.startsWith("/") && address.endsWith("/")) {
            address = address.substring(0, address.length() - 1);
        }
        if (!path.isEmpty() && !path.startsWith("/") && !address.endsWith("/")) {
            address = address + "/";
        }
        return address + path;
    }

    @Override
    public URI relativize(URI uri) {
        URI resolved = HttpUtils.resolve(getBaseUriBuilder(), uri);
        return HttpUtils.relativize(getRequestUri(), resolved);
    }

    @Override
    public URI resolve(URI uri) {
        return HttpUtils.resolve(getBaseUriBuilder(), uri);
    }

}
