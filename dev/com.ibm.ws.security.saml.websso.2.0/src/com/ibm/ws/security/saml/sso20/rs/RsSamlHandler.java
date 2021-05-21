/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.rs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.opensaml.saml.saml2.core.Assertion;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.websphere.security.saml2.Saml20Token;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.internal.Authenticator;
import com.ibm.ws.security.saml.sso20.internal.utils.UserData;
import com.ibm.ws.security.saml.sso20.token.Saml20TokenImpl;
import com.ibm.wsspi.security.tai.TAIResult;

public class RsSamlHandler {
    private static TraceComponent tc = Tr.register(RsSamlHandler.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);
    String strHeaderName = null;

    HttpServletRequest request;
    HttpServletResponse response;
    SsoSamlService ssoSamlService;

    public RsSamlHandler(HttpServletRequest request, HttpServletResponse response, SsoSamlService ssoSamlService) {
        this.request = request;
        this.response = response;
        this.ssoSamlService = ssoSamlService;
    };

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SamlHandler#getSamlVersion()
     */
    public Constants.SamlSsoVersion getSamlVersion() {
        return Constants.SamlSsoVersion.SAMLSSO20;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SamlHandler#handle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.util.Map)
     */
    public Map<String, Object> handleRequest() throws SamlException {
        Map<String, Object> results = new HashMap<String, Object>();
        TAIResult forbiddenTaiResult = badResult(HttpServletResponse.SC_UNAUTHORIZED);
        try {

            if (request == null || response == null || ssoSamlService == null) {
                // This should not happen. Just in case. No need to translate
                throw new Exception("Missing Parameter: request:" + request +
                                    " response:" + response +
                                    " ssoSamlService:" + ssoSamlService);
            }
            SsoRequest samlRequest = new SsoRequest(ssoSamlService.getProviderId(), Constants.EndpointType.ACS, request, Constants.SamlSsoVersion.SAMLSSO20, ssoSamlService);
            request.setAttribute(Constants.ATTRIBUTE_SAML20_REQUEST, samlRequest);
            return handleRequest(request, response, samlRequest);
        } catch (SamlException e) {
            results.put(SamlException.class.getName(), e.getErrorMessage());
            results.put(TAIResult.class.getName(), forbiddenTaiResult);
        } catch (Exception e) {
            results.put(Exception.class.getName(), e);
            results.put(TAIResult.class.getName(), forbiddenTaiResult);
        }
        return results;
    }

    @SuppressWarnings("rawtypes")
    public Map<String, Object> handleRequest(HttpServletRequest request,
                                             HttpServletResponse response,
                                             SsoRequest samlRequest) throws SamlException {
        Map<String, Object> results = new HashMap<String, Object>();
        SsoSamlService samlService = samlRequest.getSsoSamlService();
        //SsoConfig samlConfig = samlRequest.getSsoConfig();
        // no need to check instanceof SsoSamlService
        SsoConfig rsSamlConfig = samlRequest.getSsoConfig();

        //2. get headerName from configuration
        ArrayList<String> headerNames = rsSamlConfig.getHeaderNames();
        strHeaderName = rsSamlConfig.getHeaderName();
        //3. get SAML from header
        String headerContent = getHeaderContent(request, headerNames);

        //4. If SAML is not in header, response with 401
        if (headerContent == null || headerContent.isEmpty()) {
            throw new SamlException("RS_EMPTY_SAML_ASSERTION",
                            //RS_EMPTY_SAML_ASSERTION=CWWKS5013E: The header named as [{0}] does not exist in the HTTP request or is set to an empty string.
                            null, new Object[] { strHeaderName });
        }

        //5. unzip SAML if compressed
        // Base64 handles it
        //6. Base64 decode SAML
        //7. Convert SAML String to SAML DOM Element
        //8. Validate SAML (signature and all time stamps, and audiences if configured), similar to jax-ws
        RsSamlConsumer rsSamlConsumer = RsSamlConsumer.getInstance();
        BasicMessageContext<?, ?> rsSamlContext = rsSamlConsumer.handleSAMLResponse(request, response, samlService, samlRequest, headerContent);

        //9. Create Saml20Token
        Assertion validAssertion = rsSamlContext.getValidatedAssertion();
        Saml20Token saml20Token = new Saml20TokenImpl(validAssertion);

        //10. Map Assertion to Subject
        UserData userData = new UserData(validAssertion, saml20Token);
        Authenticator authenticator = new Authenticator(samlService, userData);
        TAIResult taiResult = authenticator.authenticateRS(request, response, samlRequest);
        results.put(TAIResult.class.getName(), taiResult);
        if (taiResult.getStatus() == HttpServletResponse.SC_OK) {
            Subject subject = taiResult.getSubject();
            results.put(Subject.class.getName(), subject);
        } else {
            // TODO error handling
        }

        return results;
    }

    /**
     * @param headerNames
     * @return
     */
    String getHeaderContent(HttpServletRequest request, ArrayList<String> headerNames) {
        // See rules in task 204127
        //c) In the above example,  find the content of saml-token-content from the headers of the http request in this sequence:
        //    saml_token1, saml_token2, saml_token3.
        String headerContent = getHdrNameContent(request, headerNames);
        //d) If no saml-token-content was found in step c, then look into the header named as "Authorization"
        //    In its content, we check its prefix. The prefix has to be, in the above example, "saml_token1", "saml_token2" or "saml_token3"
        //    The content can be in three formats, for example:
        //     i)   saml_token1=<saml-token-content>
        //     ii)  saml_token1="<saml-token-content>"
        //     iii) saml_token1 <saml-token-content>
        if (headerContent == null) {
            headerContent = getAuthorizationContent(request, headerNames);
        }
        return headerContent;
    }

    /**
     * d) If no saml-token-content was found in step c, then look into the header named as "Authorization"
     * In its content, we check its prefix. The prefix has to be, in the above example, "saml_token1", "saml_token2" or "saml_token3"
     * The content can be in three formats, for example:
     * i) saml_token1=<saml-token-content>
     * ii) saml_token1="<saml-token-content>"
     * iii) saml_token1 <saml-token-content>
     *
     * @param request
     * @param headerNames
     * @return
     */
    protected String getAuthorizationContent(HttpServletRequest request, ArrayList<String> headerNames) {
        String tokenValue = null;
        String authorizationContent = request.getHeader(Constants.HDR_NAME_Authorization); // "Authorization"
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "header content of " + Constants.HDR_NAME_Authorization + ": " + authorizationContent);
        }
        if (authorizationContent != null) {
            authorizationContent = authorizationContent.trim();
            if (!authorizationContent.isEmpty()) {
                for (String headerName : headerNames) { // allow space in the middle of headerName
                    int iSize = headerName.length();
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "headerName '" + headerName + "'");
                    }
                    if (authorizationContent.startsWith(headerName) &&
                        authorizationContent.length() > (iSize + 1)) { // content has to be more than 0 characters
                        Character charSeparator = authorizationContent.charAt(iSize);
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "separator '" + charSeparator + "'");
                        }
                        if (charSeparator == '=' || charSeparator == ' ') {
                            this.strHeaderName = headerName;
                            tokenValue = authorizationContent.substring(iSize + 1);
                            if (tokenValue != null && tokenValue.startsWith("\"")) { // remove starting and ending double-quote
                                tokenValue = tokenValue.substring(1, tokenValue.length() - 1);
                            }
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, "[" + tokenValue + "]");
                            }
                            break;
                        }
                    }
                }
            }
        }
        return tokenValue;
    }

    /**
     * c) In the above example, find the content of saml-token-content from the headers of the http request in this sequence:
     * saml_token1, saml_token2, saml_token3.
     *
     * @param request
     * @param headerNames
     * @return
     */
    protected String getHdrNameContent(HttpServletRequest request, ArrayList<String> headerNames) {
        String tokenValue = null;
        int iCnt = 0;
        for (String headerName : headerNames) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "headerName(" + (iCnt++) + "): '" + headerName + "'");
            }
            if ((tokenValue = request.getHeader(headerName)) != null) {
                this.strHeaderName = headerName;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Header name found in request: " + headerName);
                }
                break;
            }
        }
        return tokenValue;
    }

    TAIResult badResult(int iStatus) throws SamlException {//HttpServletResponse.SC_FORBIDDEN);
        TAIResult result = null;
        try {
            result = TAIResult.create(iStatus);
        } catch (WebTrustAssociationFailedException e) {
            throw new SamlException(e);
        }
        return result;
    }
}
