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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.internal.OAuthConstants;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.util.Base64;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;

public class CoverageMapEndpointServices extends AbstractOidcEndpointServices {

    protected static final String MESSAGE_BUNDLE = "com.ibm.ws.security.oauth20.internal.resources.OAuthMessages";
    private static TraceComponent tc = Tr.register(CoverageMapEndpointServices.class);

    protected void handleEndpointRequest(OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response)
            throws OidcServerException, IOException {
        if (request.getMethod().equalsIgnoreCase(HTTP_METHOD_GET) || request.getMethod().equalsIgnoreCase(HTTP_METHOD_HEAD)) {
            processHeadOrGet(provider, request, response);
        } else {
            String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                    MESSAGE_BUNDLE,
                    "OAUTH_UNSUPPORTED_METHOD",
                    new Object[] { request.getMethod(), this.getClass().getSimpleName() },
                    "CWWKS1433E: The HTTP method {0} is not supported for the service {1}.");
            Tr.error(tc, errorMsg);

            throw new OidcServerException(errorMsg, OIDCConstants.ERROR_SERVER_ERROR, HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    private void processHeadOrGet(OAuth20Provider provider, HttpServletRequest request, HttpServletResponse response)
            throws IOException, OidcServerException {
        // Validates that a JSON response is acceptable
        validateJsonAcceptable(request);

        /*
         * Validate the token type that indicates the type of coverage to return. If token type was incorrectly specified, update the response
         * appropriately with error and throw OidcServerException.
         */
        validateTokenType(request, response);

        // Extract Unique Set of Trusted Uri Prefixes
        JsonArray members = new JsonArray();
        Set<String> trustedUriPrefixesSet = getTrustedUriPrefixes(provider.getClientProvider());
        for (String trustedUriPrefix : trustedUriPrefixesSet) {
            members.add(new JsonPrimitive(addTrailingSlash(trustedUriPrefix)));
        }

        // Add any OP service endpoints that accept the token type.
        /**
        OidcServerConfig oidcServerCfg = ConfigUtils.getOidcServerConfigForOAuth20Provider(provider.getID());
        if(oidcServerCfg == null || OidcOAuth20Util.isNullEmpty(oidcServerCfg.getProviderId())) {
            String description = "Unable to retrieve OIDC provider id for this request.";
            throw new OidcServerException(description, OIDCConstants.ERROR_INVALID_REQUEST, HttpServletResponse.SC_BAD_REQUEST);
        }
        
        JsonPrimitive registrationEndpoint = new JsonPrimitive(addTrailingSlash(getCalculatedIssuerId(oidcServerCfg.getProviderId(), request) + OAuth20RequestFilter.SLASH_PATH_REGISTRATION));
        members.add(registrationEndpoint);
        **/

        // Set ETag headers
        String eTag = getETag(members);
        response.addHeader(HDR_ETAG, String.format("\"%s\"", eTag)); //$NON-NLS-1$        

        // Set Cache-Control Max Age
        response.setHeader(OAuthConstants.HEADER_CACHE_CONTROL, constructCacheControlHeaderWithMaxAge(true, String.valueOf(provider.getCoverageMapSessionMaxAge())));

        // Set Content-Type
        response.setHeader(CT, CT_APPLICATION_JSON);

        // Check for conditional method execution
        OidcServerException preconditionException = checkConditionalExecution(request, true, true, eTag, null);
        if (preconditionException != null) {
            response.setStatus(preconditionException.getHttpStatus());
            response.flushBuffer();
            return;
        }

        if (request.getMethod().equalsIgnoreCase(HTTP_METHOD_GET)) {
            response.getOutputStream().print(OidcOAuth20Util.GSON_RAW.toJson(members));
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.flushBuffer();
        return;
    }

    private Set<String> getTrustedUriPrefixes(OidcOAuth20ClientProvider clientProvider) throws OidcServerException {
        Set<String> trustedUriPrefixesSet = new HashSet<String>();
        Collection<OidcBaseClient> allClients = clientProvider.getAll();
        for (OidcBaseClient cr : allClients) {
            JsonArray trustedUriPrefixes = cr.getTrustedUriPrefixes();
            for (JsonElement trustedUriPrefix : trustedUriPrefixes) {
                trustedUriPrefixesSet.add(trustedUriPrefix.getAsString());
            }
        }
        return trustedUriPrefixesSet;
    }

    /**
     * Validates and returns the token_type specified with the referenced request. Returns <code>null</code> if token_type parameter was incorrectly specified
     * on the request, with the response object updated with the appropriate error message.
     * 
     * @param request
     *            The request to parse query parms from. Must not be <code>null</code>.
     * @param response
     *            The response to conditionally write errors to. Must not be <code>null</code>.
     * @return The bearer token type. May return <code>null</code>. If <code>null</code> is returned, the response is updated and committed with the appropriate
     *         error message.
     * @throws IOException
     *             Thrown if there was a problem writing error diagnostics to the referenced response.
     */
    private String validateTokenType(HttpServletRequest request, HttpServletResponse response)
            throws IOException, OidcServerException {
        String queryString = request.getQueryString();
        if (queryString == null) {
            String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                    MESSAGE_BUNDLE,
                    "OAUTH_COVERAGE_MAP_MISSING_PARAMS",
                    new Object[] { OAuth20Constants.TOKEN_TYPE },
                    "CWWKS1434E: Missing required parameters in request.");
            Tr.error(tc, errorMsg);

            throw new OidcServerException(errorMsg, OIDCConstants.ERROR_INVALID_REQUEST, HttpServletResponse.SC_BAD_REQUEST);
        }

        Map<String, String[]> queryParms = parseQueryParameters(queryString);
        String[] tokenTypes = queryParms.get(OAuth20Constants.TOKEN_TYPE);
        if (tokenTypes == null) {
            String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                    MESSAGE_BUNDLE,
                    "OAUTH_COVERAGE_MAP_MISSING_TOKEN_PARAM",
                    new Object[] { OAuth20Constants.TOKEN_TYPE },
                    "CWWKS1435E: Missing {0} parameter in request.");
            Tr.error(tc, errorMsg);
            throw new OidcServerException(errorMsg, OIDCConstants.ERROR_INVALID_REQUEST, HttpServletResponse.SC_BAD_REQUEST);
        }
        if (tokenTypes.length > 1) {
            String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                    MESSAGE_BUNDLE,
                    "OAUTH_COVERAGE_MAP_MULTIPLE_TOKEN_PARAM",
                    new Object[] { OAuth20Constants.TOKEN_TYPE },
                    "CWWKS1436E: Request contains multiple {0} parameters.");
            Tr.error(tc, errorMsg);
            throw new OidcServerException(errorMsg, OIDCConstants.ERROR_INVALID_REQUEST, HttpServletResponse.SC_BAD_REQUEST);
        }

        String tokenType = decode(tokenTypes[0]);
        if (tokenType.equalsIgnoreCase(OAuth20Constants.SUBTYPE_BEARER))
            return tokenType.toLowerCase();

        String errorMsg = TraceNLS.getFormattedMessage(this.getClass(),
                MESSAGE_BUNDLE,
                "OAUTH_COVERAGE_MAP_UNRECOGNIZED_TOKEN_PARAM",
                new Object[] { tokenType },
                "CWWKS1437E: Request contains unrecognized token type parameter {0}.");
        Tr.error(tc, errorMsg);
        throw new OidcServerException(errorMsg, OIDCConstants.ERROR_INVALID_REQUEST, HttpServletResponse.SC_BAD_REQUEST);
    }

    /**
     * Compute an ETag from the referenced list of appRoots.
     * 
     * @param results
     *            the list of JSON consumer objects
     * @return the ETag for the list of objects
     * @throws IOException
     */
    private String getETag(JsonArray appRoots) {
        List<String> appRootsList = getList(appRoots);
        Collections.sort(appRootsList);
        MessageDigest digest;

        try {
            digest = MessageDigest.getInstance(ALG_MD5); //$NON-NLS-1$

            for (Object appRoot : appRootsList) {
                digest.update((Base64Coder.getBytes((String) appRoot)));
            }
        } catch (NoSuchAlgorithmException e) {
            // should never happen since all Java implementations must support MD5
            throw new RuntimeException(e);
        }

        byte[] digestBytes = digest.digest();
        return Base64.encode(digestBytes);
    }
}
