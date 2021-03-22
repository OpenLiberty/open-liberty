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
package com.ibm.ws.security.saml.sso20.acs;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoHandler;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.error.SamlException;

public class AcsHandler implements SsoHandler {
    private static TraceComponent tc = Tr.register(AcsHandler.class,
                                                   TraceConstants.TRACE_GROUP,
                                                   TraceConstants.MESSAGE_BUNDLE);

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SamlHandler#getSamlVersion()
     */
    @Override
    public Constants.SamlSsoVersion getSamlVersion() {
        return Constants.SamlSsoVersion.SAMLSSO20;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.security.saml.SamlHandler#handle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.util.Map)
     */
    @Override
    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response,
                              SsoRequest samlRequest,
                              Map<String, Object> parameters) throws SamlException {
        SolicitedHandler solicitedHandler = new SolicitedHandler(request, response, samlRequest, parameters);
        UnsolicitedHandler unsolicitedHandler = new UnsolicitedHandler(request, response, samlRequest, parameters);
        handleRequest(request, response, samlRequest, parameters, solicitedHandler, unsolicitedHandler);
    }

    public void handleRequest(HttpServletRequest request,
                              HttpServletResponse response,
                              SsoRequest samlRequest,
                              Map<String, Object> parameters,
                              SolicitedHandler solicitedHandler,
                              UnsolicitedHandler unsolicitedHandler) throws SamlException {
        SsoSamlService ssoService = (SsoSamlService) parameters.get(Constants.KEY_SAML_SERVICE);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "handleRequest(ACS):" +
                         " providerId:" + ssoService.getProviderId() +
                         " request:" + request +
                         " response:" + response +
                         " Request:" + samlRequest +
                         " Service:" + ssoService);
        }

        String externalRrelayState = request.getParameter(Constants.RELAY_STATE);

        if ((externalRrelayState != null) &&
            (!externalRrelayState.isEmpty()) &&
            externalRrelayState.startsWith(Constants.SP_INITAL)) {
            // sp_solicited:
            //  1) It must have the relayState with the prefix of Constants.SP_INITAL
            //  2) The shortRelayState must point to a cached RequestInfo info.
            //     Otherwise, it is a bad sp_init or potential replay sp_init
            // This needs to check the inResponseTo. Since it built AuthnRequest
            solicitedHandler.handleRequest(externalRrelayState);
        } else {
            // Otherwise, it's idp_init (SP unsolicited)
            // This does not check inResponseTo since it did not build the AuthnRequest
            //   but redirect the request to the LoginPageURL of the configuration

            // Do create session-cookie before we commit the response
            if (ssoService.getConfig() != null) {
                boolean createSession = ssoService.getConfig().createSession();
                if (createSession) {
                    try {
                        request.getSession(true);
                    } catch (Exception e) {
                        //ignore it. Session exists
                    }
                }
            }
            unsolicitedHandler.handleRequest(externalRrelayState);
        }
    }
}
