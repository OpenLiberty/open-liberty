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

import java.security.Principal;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.ws.security.saml.sso20.internal.SAMLResponseTAI;
import com.ibm.wsspi.security.tai.TAIResult;

public class SamlInboundService {
    public static final TraceComponent tc = Tr.register(SamlInboundService.class,
                                                        TraceConstants.TRACE_GROUP,
                                                        TraceConstants.MESSAGE_BUNDLE);

    static public final String KEY_TYPE = "type";
    static public final String TYPE = "RsSaml";
    static public final String VERSION = "v1.0";
    static protected final String KEY_SERVICE_PID = "service.pid";
    static protected final String KEY_PROVIDER_ID = "id";
    static protected final String KEY_ID = "id";

    @Activate
    protected void activate(ComponentContext cc, Map<String, Object> props) {
        SAMLResponseTAI.setActivatedInboundService(this);
    }

    @Modified
    protected void modified(ComponentContext cc, Map<String, Object> props) {
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
    }

    /*
     * This is merged into SAMLResponeTAI and
     * will be called by SAMLResponseTAI
     */
    public TAIResult negotiateValidateandEstablishTrust(HttpServletRequest req,
                                                        HttpServletResponse resp,
                                                        SsoSamlService ssoSamlService) throws WebTrustAssociationFailedException {
        String requestUrl = req.getRequestURL().toString();
        if (ssoSamlService == null) {
            // almost impossible unless during dynamic updating or internal error
            throw new WebTrustAssociationFailedException(SamlException.formatMessage("RS_SAML_SERVER_INTERNAL_LOG_ERROR",
                                                                                     // RS_SAML_SERVER_INTERNAL_LOG_ERROR=CWWKS5201E: An internal server error occurred while processing SAML Web Single Sign-On (SSO) within JAXRS request [{0}].
                                                                                     //Cause:[{1}], StackTrace: [{2}].
                                                                                     null,
                                                                                     new Object[] { requestUrl, "", "" }));
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "ssoSamlServiceId:" + ssoSamlService.getProviderId() +
                         "\nssoSamlService:" + ssoSamlService +
                         "\nrequestUrl:" + requestUrl +
                         "\nheaders:" + ssoSamlService.getConfig().getHeaderName());
        }

        try {
            return callRsSaml(req, resp, ssoSamlService);
        } catch (Exception e) {
            setErrorHeader(resp, HttpServletResponse.SC_UNAUTHORIZED, "invalid_token");
            throw new WebTrustAssociationFailedException(e.getMessage());
        }

    }

    // ToBeDeleted temporary code
    public String getUserName(Subject subject) {
        if (subject == null)
            return null;
        Set<Principal> principals = subject.getPrincipals();
        Iterator<Principal> principalsIterator = principals.iterator();
        if (principalsIterator.hasNext()) {
            Principal principal = principalsIterator.next();
            return principal.getName();
        }
        return null;
    }

    /**
     * @param req
     * @param resp
     * @param SsoSamlService
     * @return
     * @throws Exception
     */
    TAIResult callRsSaml(HttpServletRequest req, HttpServletResponse resp, SsoSamlService ssoSamlService) throws Exception {

        RsSamlHandler rsSamlHandler = new RsSamlHandler(req, resp, ssoSamlService);
        Map<String, Object> results = rsSamlHandler.handleRequest();

        TAIResult taiResult = (TAIResult) results.get(TAIResult.class.getName());

        if (taiResult == null) {
            setErrorHeader(resp, HttpServletResponse.SC_UNAUTHORIZED, "invalid_token");
            return TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED);
        } else if (taiResult.getStatus() != HttpServletResponse.SC_OK) {
            // error handling
            String errorMessage = (String) results.get(SamlException.class.getName());
            if (errorMessage != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, " hits SamlException and error message:" + errorMessage);
                }
            } else { // get an unexpected Exception
                Exception e = (Exception) results.get(Exception.class.getName());
                if (e != null) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "RsSamlHandler hits Exception and error message:" + e);
                    }
                } else {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "RsSamlHandler hits unknown error Results:" + results);
                    }
                }
            }
            setErrorHeader(resp, HttpServletResponse.SC_UNAUTHORIZED, "invalid_token");
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "TAIResult:" + taiResult + " \nSubject:" + taiResult.getSubject());
        }
        return taiResult;
    }

    /**
     * Set the WWW-Authenticate header in the response, using the given
     * error, error description, status and scope.
     *
     * @param response
     * @param status
     * @param error
     * @param errorDescription
     * @param scope
     */
    private void setErrorHeader(HttpServletResponse response,
                                int status,
                                String error) {
        //String header = "SAML error=" + error;
        //response.setHeader(WWW_AUTHENTICATE_HEADER, header);
        response.setStatus(status);
    }
}
