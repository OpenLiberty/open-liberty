/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.ws.jsf.container.cdi;

import javax.faces.context.FacesContext;

/**
 * Helper class for preparing JSF URLs which include the conversation id.
 *
 * @author Nicklas Karlsson
 * @author Dan Allen
 * @author Marko Luksa
 */
public class FacesUrlTransformer {
    private static final String HTTP_PROTOCOL_URL_PREFIX = "http://";
    private static final String HTTPS_PROTOCOL_URL_PREFIX = "https://";
    private static final String QUERY_STRING_DELIMITER = "?";
    private static final String PARAMETER_PAIR_DELIMITER = "&";
    // in rare cases, semicolon will the delimiter; e.g. when you need to encode "&" as "&amp;"
    private static final String PARAMETER_PAIR_DELIMITER_ENCODED = ";";
    private static final String PARAMETER_ASSIGNMENT_OPERATOR = "=";

    private String url;
    private final FacesContext context;

    public FacesUrlTransformer(String url, FacesContext facesContext) {
        this.url = url;
        this.context = facesContext;
    }

    public FacesUrlTransformer appendConversationIdIfNecessary(String cidParameterName, String cid) {
        this.url = appendParameterIfNeeded(url, cidParameterName, cid);
        return this;
    }

    private static String appendParameterIfNeeded(String url, String parameterName, String parameterValue) {
        int queryStringIndex = url.indexOf(QUERY_STRING_DELIMITER);
        // if there is no query string or there is a query string but the param is
        // absent, then append it
        if (queryStringIndex < 0 || isCidParamAbsent(url, parameterName, queryStringIndex)) {
            StringBuilder builder = new StringBuilder(url);
            if (queryStringIndex < 0) {
                builder.append(QUERY_STRING_DELIMITER);
            } else {
                builder.append(PARAMETER_PAIR_DELIMITER);
            }
            builder.append(parameterName).append(PARAMETER_ASSIGNMENT_OPERATOR);
            if (parameterValue != null) {
                builder.append(parameterValue);
            }
            return builder.toString();
        } else {
            return url;
        }
    }

    private static boolean isCidParamAbsent(String url, String parameterName, int queryStringIndex) {
        return url.indexOf(QUERY_STRING_DELIMITER + parameterName + PARAMETER_ASSIGNMENT_OPERATOR, queryStringIndex) < 0
            && url.indexOf(PARAMETER_PAIR_DELIMITER + parameterName + PARAMETER_ASSIGNMENT_OPERATOR, queryStringIndex) < 0
            && url.indexOf(PARAMETER_PAIR_DELIMITER_ENCODED + parameterName + PARAMETER_ASSIGNMENT_OPERATOR, queryStringIndex) < 0;
    }

    public String getUrl() {
        return url;
    }

    public FacesUrlTransformer toRedirectViewId() {
        String requestPath = context.getExternalContext().getRequestContextPath();
        if (isUrlAbsolute()) {
            url = url.substring(url.indexOf(requestPath) + requestPath.length());
        } else if (url.startsWith(requestPath)) {
            url = url.substring(requestPath.length());
        }
        return this;
    }

    public FacesUrlTransformer toActionUrl() {
        String actionUrl = context.getApplication().getViewHandler().getActionURL(context, url);

        int queryStringIndex = url.indexOf(QUERY_STRING_DELIMITER);
        if (queryStringIndex < 0) {
            url = actionUrl;
        } else {
            String queryParameters = url.substring(queryStringIndex + 1);

            int actionQueryStringIndex = actionUrl.indexOf(QUERY_STRING_DELIMITER);
            if (actionQueryStringIndex < 0) {
                url = actionUrl + QUERY_STRING_DELIMITER + queryParameters;
            } else {
                String actionQueryParameters = actionUrl.substring(actionQueryStringIndex + 1);
                if (queryParameters.startsWith(actionQueryParameters)) {
                    url = actionUrl.substring(0, actionQueryStringIndex) + QUERY_STRING_DELIMITER + queryParameters;
                } else {
                    url = actionUrl + PARAMETER_PAIR_DELIMITER + queryParameters;
                }
            }
        }
        return this;
    }

    public String encode() {
        return context.getExternalContext().encodeActionURL(url);
    }

    private boolean isUrlAbsolute() {
        return url.startsWith(HTTP_PROTOCOL_URL_PREFIX) || url.startsWith(HTTPS_PROTOCOL_URL_PREFIX);
    }
}
