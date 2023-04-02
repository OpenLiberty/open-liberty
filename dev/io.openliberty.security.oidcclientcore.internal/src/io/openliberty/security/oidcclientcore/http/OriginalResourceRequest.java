/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oidcclientcore.http;

import static io.openliberty.security.oidcclientcore.storage.OidcClientStorageConstants.WAS_OIDC_REQ_HEADERS;
import static io.openliberty.security.oidcclientcore.storage.OidcClientStorageConstants.WAS_OIDC_REQ_METHOD;
import static io.openliberty.security.oidcclientcore.storage.OidcClientStorageConstants.WAS_OIDC_REQ_PARAMS;
import static io.openliberty.security.oidcclientcore.storage.OidcClientStorageConstants.WAS_REQ_URL_OIDC;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.security.oidcclientcore.authentication.AuthorizationRequestParameters;
import io.openliberty.security.oidcclientcore.storage.Storage;
import io.openliberty.security.oidcclientcore.storage.StorageFactory;
import io.openliberty.security.oidcclientcore.utils.Utils;

public class OriginalResourceRequest extends HttpServletRequestWrapper {

    private String requestUrl; // methods involving request url should already be restored after redirecting to original resource
    private String queryStringParams;
    private String method;
    private Map<String, List<String>> headers;
    private Map<String, String[]> params;

    private final Storage storage;

    public OriginalResourceRequest(HttpServletRequest request, HttpServletResponse response, boolean useSession) {
        super(request);
        this.requestUrl = "";
        this.queryStringParams = null;
        this.method = "";
        this.headers = new HashMap<>();
        this.params = new HashMap<>();
        this.storage = StorageFactory.instantiateStorage(request, response, useSession);

        restoreOriginalRequest(request.getParameter(AuthorizationRequestParameters.STATE));
    }

    public static void storeFullRequest(HttpServletRequest request, Storage storage, String state) {
        Base64.Encoder encoder = Base64.getEncoder();
        String stateHash = Utils.getStrHashCode(state);
        storeRequestCookies(request, storage, encoder, stateHash);
        storeRequestMethod(request, storage, encoder, stateHash);
        storeRequestHeaders(request, storage, encoder, stateHash);
        storeRequestParameters(request, storage, encoder, stateHash);
    }

    private static void storeRequestCookies(HttpServletRequest request, Storage storage, Base64.Encoder encoder, String stateHash) {
        // cookies should be automatically restored during redirection to original resource
    }

    private static void storeRequestMethod(HttpServletRequest request, Storage storage, Base64.Encoder encoder, String stateHash) {
        String method = request.getMethod();
        String encodedMethod = encoder.encodeToString(method.getBytes());
        storage.store(WAS_OIDC_REQ_METHOD + stateHash, encodedMethod);
    }

    private static void storeRequestHeaders(HttpServletRequest request, Storage storage, Base64.Encoder encoder, String stateHash) {
        StringJoiner headerJoiner = new StringJoiner("&");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String encodedHeaderName = encoder.encodeToString(headerName.getBytes());
            StringJoiner valueJoiner = new StringJoiner(".");
            Enumeration<String> headerValues = request.getHeaders(headerName);
            while (headerValues.hasMoreElements()) {
                String headerValue = headerValues.nextElement();
                String encodedHeaderValue = encoder.encodeToString(headerValue.getBytes());
                valueJoiner.add(encodedHeaderValue);
            }
            String encodedHeaderValues = valueJoiner.toString();
            headerJoiner.add(encodedHeaderName + ":" + encodedHeaderValues);
        }
        String encodedHeaders = headerJoiner.toString();
        storage.store(WAS_OIDC_REQ_HEADERS + stateHash, encodedHeaders);
    }

    private static void storeRequestParameters(HttpServletRequest request, Storage storage, Base64.Encoder encoder, String stateHash) {
        StringJoiner paramJoiner = new StringJoiner("&");
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String encodedParamName = encoder.encodeToString(paramName.getBytes());
            StringJoiner valueJoiner = new StringJoiner(".");
            String[] paramValues = request.getParameterValues(paramName);
            for (String paramValue : paramValues) {
                String encodedParamValue = encoder.encodeToString(paramValue.getBytes());
                valueJoiner.add(encodedParamValue);
            }
            String encodedParamValues = valueJoiner.toString();
            paramJoiner.add(encodedParamName + ":" + encodedParamValues);
        }
        String encodedParams = paramJoiner.toString();
        storage.store(WAS_OIDC_REQ_PARAMS + stateHash, encodedParams);
    }

    private void restoreOriginalRequest(String state) {
        Base64.Decoder decoder = Base64.getDecoder();
        String stateHash = Utils.getStrHashCode(state);
        restoreReqUrlAndQueryParams(stateHash);
        restoreCookies(decoder, stateHash);
        restoreMethod(decoder, stateHash);
        restoreHeaders(decoder, stateHash);
        restoreParams(decoder, stateHash);
    }

    private void restoreReqUrlAndQueryParams(String stateHash) {
        String key = WAS_REQ_URL_OIDC + stateHash;
        String requestUrlWithQueryParams = storage.get(key); // don't remove since it is used later
        if (requestUrlWithQueryParams == null || requestUrlWithQueryParams.isEmpty()) {
            return;
        }
        String[] requestUrlSplitFromQueryParams = requestUrlWithQueryParams.split(Pattern.quote("?"));
        this.requestUrl = requestUrlSplitFromQueryParams[0];
        if (requestUrlSplitFromQueryParams.length > 1) {
            this.queryStringParams = requestUrlSplitFromQueryParams[1];
        }
    }

    private void restoreCookies(Base64.Decoder decoder, String stateHash) {
        // cookies should be automatically restored during redirection to original resource
    }

    private void restoreMethod(Base64.Decoder decoder, String stateHash) {
        String key = WAS_OIDC_REQ_METHOD + stateHash;
        String encodedMethod = getAndRemoveFromStorage(key);
        if (encodedMethod == null || encodedMethod.isEmpty()) {
            return;
        }
        this.method = new String(decoder.decode(encodedMethod));
    }

    private void restoreHeaders(Base64.Decoder decoder, String stateHash) {
        String key = WAS_OIDC_REQ_HEADERS + stateHash;
        String storedHeadersString = getAndRemoveFromStorage(key);
        if (storedHeadersString == null || storedHeadersString.isEmpty()) {
            return;
        }
        Map<String, List<String>> headers = new HashMap<>();
        String[] encodedHeaders = storedHeadersString.split("&");
        for (String encodedHeader : encodedHeaders) {
            String[] splitEncodedHeader = encodedHeader.split(":");
            String headerName = new String(decoder.decode(splitEncodedHeader[0]));
            String[] encodedHeaderValues = splitEncodedHeader[1].split(Pattern.quote("."));
            List<String> headerValues = new ArrayList<>();
            for (String encodedHeaderValue : encodedHeaderValues) {
                String headerValue = new String(decoder.decode(encodedHeaderValue));
                headerValues.add(headerValue);
            }
            headers.put(headerName, headerValues);
        }
        this.headers = headers;
    }

    private void restoreParams(Base64.Decoder decoder, String stateHash) {
        String key = WAS_OIDC_REQ_PARAMS + stateHash;
        String storedParamsString = getAndRemoveFromStorage(key);
        if (storedParamsString == null || storedParamsString.isEmpty()) {
            return;
        }
        Map<String, String[]> params = new HashMap<>();
        String[] encodedParams = storedParamsString.split("&");
        for (String encodedParam : encodedParams) {
            String[] splitEncodedParam = encodedParam.split(":");
            String paramName = new String(decoder.decode(splitEncodedParam[0]));
            String[] encodedParamValues = splitEncodedParam[1].split(Pattern.quote("."));
            String[] paramValues = new String[encodedParamValues.length];
            for (int i = 0; i < encodedParamValues.length; i++) {
                String encodedParamValue = encodedParamValues[i];
                String paramValue = new String(decoder.decode(encodedParamValue));
                paramValues[i] = paramValue;
            }
            params.put(paramName, paramValues);
        }
        this.params = params;
    }

    @Trivial
    private String getAndRemoveFromStorage(String key) {
        String value = storage.get(key);
        if (value != null) {
            storage.remove(key);
        }
        return value;
    }

    @Override
    public String getQueryString() {
        return queryStringParams;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    @Sensitive
    public String getHeader(String name) {
        List<String> headerValues = headers.get(name);
        if (headerValues == null || headerValues.size() == 0) {
            return null;
        }
        return headerValues.get(0);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }

    @Override
    @Sensitive
    public Enumeration<String> getHeaders(String name) {
        List<String> headerValues = headers.get(name);
        if (headerValues == null) {
            headerValues = Collections.emptyList();
        }
        return Collections.enumeration(headerValues);
    }

    @Override
    @Sensitive
    public int getIntHeader(String name) {
        String header = getHeader(name);
        if (header == null) {
            return -1;
        }
        return Integer.parseInt(header);
    }

    @Override
    @Sensitive
    public String getParameter(String name) {
        String[] paramValues = params.get(name);
        if (paramValues == null || paramValues.length == 0) {
            return null;
        }
        return paramValues[0];
    }

    @Override
    @Sensitive
    public Map<String, String[]> getParameterMap() {
        return new HashMap<String, String[]>(params);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(params.keySet());
    }

    @Override
    @Sensitive
    public String[] getParameterValues(String name) {
        return params.get(name);
    }

}
