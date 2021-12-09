/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.slo;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.util.Map;

import javax.security.auth.Subject;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoHandler;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.acs.WebSSOConsumer;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;

public class SLOHandler implements SsoHandler {
    private static TraceComponent tc = Tr.register(SLOHandler.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    SPInitiatedSLO spSlo;

    @Override
    public Constants.SamlSsoVersion getSamlVersion() {
        return Constants.SamlSsoVersion.SAMLSSO20;
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response, SsoRequest samlRequest,
                              Map<String, Object> parameters) throws SamlException {

        verifySsoRequestNotNull(samlRequest);

        SsoSamlService ssoService = getSsoSamlServiceParameter(parameters);

        debugRequestAndSsoServiceInfo(request, response, samlRequest, ssoService);

        try {
            if (isLogoutEndpointRequest(samlRequest)) { // logout is invoked on SP
                handleLogoutEndpointRequest(request, response, ssoService, parameters);
            } else if (isLogoutResponseFromIdP(request)) {
                handleLogoutResponseFromIdp(request, response, samlRequest, ssoService);
            } else if (isLogoutRequestFromIdp(request)) {
                handleLogoutRequestFromIdp(request, response, samlRequest, ssoService);
            }
        } catch (Exception e) {
            String eMsg = Tr.formatMessage(tc, "ERROR_HANDLING_LOGOUT_REQUEST", new Object[] { e.getLocalizedMessage() });
            throw new SamlException(eMsg, e);
        }

        postLogoutRequestProcess();
    }

    void verifySsoRequestNotNull(SsoRequest ssoRequest) throws SamlException {
        if (ssoRequest == null) {
            throw new SamlException(Tr.formatMessage(tc, "LOGOUT_REQUEST_MISSING_SSO_REQUEST"));
        }
    }

    SsoSamlService getSsoSamlServiceParameter(Map<String, Object> parameters) throws SamlException {
        if (parameters == null) {
            throw new SamlException(Tr.formatMessage(tc, "LOGOUT_CANNOT_FIND_SAML_SSO_SERVICE"));
        }
        SsoSamlService ssoService = (SsoSamlService) parameters.get(Constants.KEY_SAML_SERVICE);
        if (ssoService == null) {
            throw new SamlException(Tr.formatMessage(tc, "LOGOUT_CANNOT_FIND_SAML_SSO_SERVICE"));
        }
        return ssoService;
    }

    /**
     * @param ssoService Must not be null
     */
    void debugRequestAndSsoServiceInfo(HttpServletRequest request, HttpServletResponse response, SsoRequest samlRequest, SsoSamlService ssoService) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "handleRequest(SLO):");
            Tr.debug(tc, " providerId:" + ssoService.getProviderId());
            Tr.debug(tc, " request:" + request);
            Tr.debug(tc, " response:" + response);
            Tr.debug(tc, " Request:" + samlRequest);
            Tr.debug(tc, " Service:" + ssoService);
        }
    }

    /**
     * @param ssoRequest Must not be null
     */
    boolean isLogoutEndpointRequest(SsoRequest ssoRequest) {
        return Constants.EndpointType.LOGOUT.equals(ssoRequest.getType());
    }

    void handleLogoutEndpointRequest(HttpServletRequest request, HttpServletResponse response, SsoSamlService ssoService, Map<String, Object> parameters) throws SamlException {

        spSlo = new SPInitiatedSLO(ssoService, getSubjectFromParameters(parameters));
        spSlo.buildandSendSLORequest(request, response);
    }

    /**
     * @param parameters
     * @return
     * @throws SamlException
     */
    private Subject getSubjectFromParameters(Map<String, Object> parameters) throws SamlException {

        if (parameters.get(Constants.KEY_SECURITY_SUBJECT) != null) {
            return (Subject) parameters.get(Constants.KEY_SECURITY_SUBJECT);
        }
        Tr.error(tc, "LOGOUT_CANNOT_FIND_SAMLTOKEN");
        throw new SamlException(Tr.formatMessage(tc, "LOGOUT_CANNOT_FIND_SAMLTOKEN"));
    }

    boolean isLogoutResponseFromIdP(HttpServletRequest request) {
        return request.getParameter(Constants.SAMLResponse) != null;
    }

    void handleLogoutResponseFromIdp(HttpServletRequest request, HttpServletResponse response, SsoRequest ssoRequest,
                                     SsoSamlService ssoService) throws Exception {
        //                Collection<String> headers = response.getHeaderNames();
        //                Iterator<String> it = headers.iterator();
        //                while (it.hasNext()) {
        //                    String header = it.next();
        //                    if (tc.isDebugEnabled()) {
        //                        Tr.debug(tc, "logout response header = ", header);
        //                        Tr.debug(tc, "logout response header content = ", response.getHeader(header));
        //                    }
        //                }
        //
        //                String rawLogoutResponse = null;
        //                MyHttpServletResponseWrapper wrapper = new MyHttpServletResponseWrapper(response);
        ////                    ServletOutputStream sos = response.getOutputStream();
        ////                    sos.println("Success");
        //                rawLogoutResponse = wrapper.toString();
        String rawLogoutResponse = request.getParameter(Constants.SAMLResponse);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "logout response = ", rawLogoutResponse);
        }
        int i = response.getStatus();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "logout response status = ", i);
        }
        String externalRelayState = request.getParameter(Constants.RELAY_STATE);
        String relayState = null;
        relayState = URLDecoder.decode(externalRelayState,
                                       Constants.UTF8);
        BasicMessageContext<?, ?, ?> msgCtx = WebSSOConsumer.getInstance().handleSAMLLogoutResponse(request,
                                                                                                    response,
                                                                                                    ssoService, relayState,
                                                                                                    ssoRequest);

        SLOPostLogoutHandler postLogoutHandler = new SLOPostLogoutHandler(request, ssoService.getConfig(), msgCtx);
        postLogoutHandler.sendToPostLogoutPage(response);
    }

    boolean isLogoutRequestFromIdp(HttpServletRequest request) {
        return request.getParameter(Constants.SAMLRequest) != null;
    }

    void handleLogoutRequestFromIdp(HttpServletRequest request, HttpServletResponse response, SsoRequest ssoRequest, SsoSamlService ssoService) throws SamlException {
        String rawLogoutRequest = request.getParameter(Constants.SAMLRequest);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "logout request = ", rawLogoutRequest);
        }

        BasicMessageContext<?, ?, ?> msgCtx = WebSSOConsumer.getInstance().handleSAMLLogoutRequest(request,
                                                                                                   response,
                                                                                                   ssoService, null, // do not check inResponse in idp_init (SP Unsolicited)
                                                                                                   ssoRequest);

        
        String externalRelayState = null;
        externalRelayState = request.getParameter(Constants.RELAY_STATE);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "relaystate in logout request = ", externalRelayState);
        }

        IdPInitiatedSLO idpSlo = new IdPInitiatedSLO(ssoService, msgCtx, externalRelayState);
        idpSlo.sendSLOResponseToIdp(request, response);
    }

    void postLogoutRequestProcess() {
//      String externalRrelayState = request.getParameter(Constants.RELAY_STATE);
//
//      if ((externalRrelayState != null) &&
//          (!externalRrelayState.isEmpty()) &&
//          externalRrelayState.startsWith(Constants.SP_INITAL)) {
//          // sp_solicited:
//          //  1) It must have the relayState with the prefix of Constants.SP_INITAL
//          //  2) The shortRelayState must point to a cached RequestInfo info.
//          //     Otherwise, it is a bad sp_init or potential replay sp_init
//          // This needs to check the inResponseTo. Since it built AuthnRequest
//          solicitedHandler.handleRequest(externalRrelayState);
//      } else {
//          // Otherwise, it's idp_init (SP unsolicited)
//          // This does not check inResponseTo since it did not build the AuthnRequest
//          //   but redirect the request to the LoginPageURL of the configuration
//
//          // Do create session-cookie before we commit the response
//          if (ssoService.getConfig() != null) {
//              boolean createSession = ssoService.getConfig().createSession();
//              if (createSession) {
//                  try {
//                      request.getSession(true);
//                  } catch (Exception e) {
//                      //ignore it. Session exists
//                  }
//              }
//          }
//          unsolicitedHandler.handleRequest(externalRrelayState);
//      }
    }

    static class MyHttpServletResponseWrapper extends HttpServletResponseWrapper {

        private static final int BUFFER_SIZE = 2048;
        private final StringWriter sw = new StringWriter(BUFFER_SIZE);

        public MyHttpServletResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            return new PrintWriter(sw);
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return sw.toString();
        }
    }
}
