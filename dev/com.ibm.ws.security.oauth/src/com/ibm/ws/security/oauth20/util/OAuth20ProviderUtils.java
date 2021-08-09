/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.ibm.json.java.JSONObject;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OAuthConfigurationException;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20Exception;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.exception.OAuth20BadParameterException;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.webcontainer.security.ProviderAuthenticationResult;

/**
 * This class was imported from tWAS to make only those changes necessary to
 * run OAuth on Liberty. The mission was not to refactor, restructure, or
 * generally cleanup the code.
 */
public class OAuth20ProviderUtils {

    private static TraceComponent tc = Tr.register(OAuth20ProviderUtils.class,
            "OAUTH", "com.ibm.ws.security.oauth20.resources.ProviderMsgs");

    protected static String OAuthConfigFileDir = null;

    public static final String AUTHENTICATE_HDR = "WWW-Authenticate";

    public static Object processClass(String className, String configConstant,
            Class<?> interfaceName, ClassLoader classloader)
            throws OAuthException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "processClass", new Object[] { className, configConstant, interfaceName, classloader });

        if (className == null) {
            throw new OAuthConfigurationException("security.oauth.error.config.notspecified.exception", configConstant, "null", null);
        }
        try {
            Class<?> klass = classloader.loadClass(className);
            Object ret = klass.newInstance();
            if (!(interfaceName.isAssignableFrom(ret.getClass()))) {
                throw new OAuthConfigurationException("security.oauth.error.classmismatch.exception", configConstant, interfaceName.getName(), null);
            }
            return ret;
        } catch (ClassNotFoundException e) {
            throw new OAuthConfigurationException("security.oauth.error.classinstantiation.exception", configConstant, className, e);
        } catch (IllegalAccessException e) {
            throw new OAuthConfigurationException("security.oauth.error.classinstantiation.exception", configConstant, className, e);
        } catch (InstantiationException e) {
            throw new OAuthConfigurationException("security.oauth.error.classinstantiation.exception", configConstant, className, e);
        } finally {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "processClass");
        }
    }

    public static OidcBaseClient getOidcOAuth20Client(OAuth20Provider provider, String clientId) throws OAuth20Exception {
        if (provider == null) {
            return null;
        }
        OidcOAuth20ClientProvider clientProvider = provider.getClientProvider();
        if (clientProvider != null) {
            return clientProvider.get(clientId);
        }
        return null;
    }

    public static void validateResource(HttpServletRequest request, AttributeList attrList, OidcBaseClient client) throws OAuth20BadParameterException {

        String[] resourceParams = request.getParameterValues(OAuth20Constants.RESOURCE);

        if (resourceParams == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "The resource parameter was not found");
            }
            return;
        }

        JsonArray authorizedResourceids = client.getResourceIds();
        Set<String> resourceSet = new HashSet<String>();
        int iResources = resourceParams.length;
        for (int j = 0; j < iResources; j++) {
            String resourceStr = resourceParams[0];
            String[] resources = resourceStr.split(" ");

            for (int i = 0; i < resources.length; i++) {
                String resource = resources[i].trim();
                if (resource != null && resource.length() > 0) {
                    if (OidcOAuth20Util.jsonArrayContainsString(authorizedResourceids, resource)) {
                        resourceSet.add(resource);
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "The requested resource [" + resource + "] is authorized.");
                        }
                    } else {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "The requested resource [" + resource + "] is not authorized.");
                        }
                        // # 0 - "scope" or "resource" (as of this update, may be other parms in the future}
                        // # 1 - string value, 2 - list of string values, 3 - config attributes: "scope" or "resourceIds" (as of this update, may be other parms in the future),
                        // SECURITY.OAUTH20.ERROR.VALUE.NOT.IN.LIST=CWOAU0072E: The request parameter [{0}] contains an invalid value [{1}]. Valid values are [{2}] and defined in the configuration
                        // attribute [{3}].
                        throw new OAuth20BadParameterException("SECURITY.OAUTH20.ERROR.VALUE.NOT.IN.LIST",
                                new Object[] { OAuth20Constants.RESOURCE, resource,
                                        OidcOAuth20Util.getSpaceDelimitedString(authorizedResourceids), "resourceIds" });
                    }
                }
            }
        }

        String[] validResources = resourceSet.toArray(new String[resourceSet.size()]);

        if (validResources != null && validResources.length > 0) {
            if (attrList != null) {
                attrList.setAttribute(OAuth20Constants.RESOURCE, OAuth20Constants.ATTRTYPE_PARAM_OAUTH, validResources);
            }
            request.setAttribute(OAuth20Constants.OAUTH20_AUTHEN_PARAM_RESOURCE, validResources);
        }
    }

    /**
     *
     * @param rsp -- non-null
     * @param oauthResult -- non-null
     * @param errorDescription -- non-null
     * @throws IOException
     */
    public static void handleOAuthChallenge(HttpServletResponse rsp,
            ProviderAuthenticationResult oauthResult,
            String errorDescription) throws IOException {
        if (rsp.isCommitted())
            return; // if it had already been handled the response
        final String error = "error";
        final String error_description = "error_description";
        final int errorCode = HttpServletResponse.SC_UNAUTHORIZED;

        rsp.setStatus(errorCode);
        // in case, make sure we set the WWW-Authenticate
        String wwwAuthenticate = rsp.getHeader(AUTHENTICATE_HDR);
        if (wwwAuthenticate == null || wwwAuthenticate.isEmpty()) {
            wwwAuthenticate = "Bearer realm=\"oauth\"";
            rsp.setHeader(AUTHENTICATE_HDR, wwwAuthenticate);
        }

        rsp.setHeader(OAuth20Constants.HTTP_HEADER_CONTENT_TYPE,
                OAuth20Constants.HTTP_CONTENT_TYPE_JSON);
        JSONObject responseJSON = new JSONObject();
        responseJSON.put(error, errorCode);
        if (errorDescription != null) {
            responseJSON.put(error_description, errorDescription);
        }
        PrintWriter pw;
        pw = rsp.getWriter();
        pw.write(responseJSON.toString());
        pw.flush();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "WWW-Authenticate:'" + wwwAuthenticate + "' code:" + errorCode + " reason:" + errorDescription);
        }
    }
}
