/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.audit.event;

import java.net.URLDecoder;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.ws.security.audit.utils.AuditUtils;
import com.ibm.ws.webcontainer.security.AuthenticationResult;
import com.ibm.ws.webcontainer.security.WebRequest;

/**
 * Class with default values for authentication events
 */
public class AuthenticationEvent extends AuditEvent {

    private static final TraceComponent tc = Tr.register(AuthenticationEvent.class);

    @SuppressWarnings("unchecked")
    public AuthenticationEvent() {
        set(AuditEvent.EVENTNAME, AuditConstants.SECURITY_AUTHN);
        setInitiator((Map<String, Object>) AuditEvent.STD_INITIATOR.clone());
        setObserver((Map<String, Object>) AuditEvent.STD_OBSERVER.clone());
        setTarget((Map<String, Object>) AuditEvent.STD_TARGET.clone());
    }

    public AuthenticationEvent(WebRequest webreq, AuthenticationResult authResult, Integer statusCode) {
        this();
        try {
            HttpServletRequest req = webreq.getHttpServletRequest();
            // add initiator
            if (req != null && req.getRemoteAddr() != null)
                set(AuditEvent.INITIATOR_HOST_ADDRESS, req.getRemoteAddr());
            String agent = req.getHeader("User-Agent");
            if (agent != null)
                set(AuditEvent.INITIATOR_HOST_AGENT, agent);
            // add target
            set(AuditEvent.TARGET_NAME, URLDecoder.decode(req.getRequestURI(), "UTF-8"));
            set(AuditEvent.TARGET_APPNAME, AuditUtils.getJ2EEComponentName());

            if (req.getQueryString() != null) {
                String str = URLDecoder.decode(req.getQueryString(), "UTF-8");
                str = AuditUtils.hidePassword(str);
                set(AuditEvent.TARGET_PARAMS, str);
            }

            set(AuditEvent.TARGET_HOST_ADDRESS, req.getLocalAddr() + ":" + req.getLocalPort());
            set(AuditEvent.TARGET_CREDENTIAL_TYPE, authResult.getAuditCredType());
            if (authResult.getAuditCredValue() != null)
                set(AuditEvent.TARGET_CREDENTIAL_TOKEN, authResult.getAuditCredValue());
            else if (req.getUserPrincipal() != null && req.getUserPrincipal().getName() != null)
                set(AuditEvent.TARGET_CREDENTIAL_TOKEN, req.getUserPrincipal().getName());
            String sessionID = AuditUtils.getSessionID(req);
            if (sessionID != null) {
                set(AuditEvent.TARGET_SESSION, sessionID);
            }

            set(AuditEvent.TARGET_REALM, AuditUtils.getRealmName());

            if (authResult.getAuditAuthConfigProviderName() != null) {
                set(AuditEvent.TARGET_JASPI_PROVIDER, authResult.getAuditAuthConfigProviderName());
            }

            if (authResult.getAuditAuthConfigProviderAuthType() != null) {
                set(AuditEvent.TARGET_JASPI_AUTHTYPE, authResult.getAuditAuthConfigProviderAuthType());
            }

            set(AuditEvent.TARGET_METHOD, AuditUtils.getRequestMethod(req));
            String arOutcome = authResult.getAuditOutcome();
            switch (authResult.getStatus()) {
                case SUCCESS: {
                    setOutcome(arOutcome != null ? arOutcome : AuditEvent.OUTCOME_SUCCESS);
                    if (statusCode != null) {
                        set(AuditEvent.REASON_CODE, statusCode);
                        set(AuditEvent.REASON_TYPE, AuditUtils.getRequestScheme(req));
                    }
                    break;
                }
                case FAILURE: {
                    setOutcome(arOutcome != null ? arOutcome : AuditEvent.OUTCOME_FAILURE);
                    if (statusCode != null) {
                        set(AuditEvent.REASON_CODE, statusCode);
                        set(AuditEvent.REASON_TYPE, AuditUtils.getRequestScheme(req));
                    }
                    break;
                }
                case RETURN: {
                    if (statusCode != null && statusCode == 200) {
                        setOutcome(arOutcome != null ? arOutcome : AuditEvent.OUTCOME_SUCCESS);
                    } else {
                        setOutcome(arOutcome != null ? arOutcome : AuditEvent.OUTCOME_FAILURE);
                    }

                    if (statusCode != null) {
                        set(AuditEvent.REASON_CODE, statusCode);
                        set(AuditEvent.REASON_TYPE, AuditUtils.getRequestScheme(req));
                    }
                    break;
                }
                case SEND_401: {
                    setOutcome(arOutcome != null ? arOutcome : AuditEvent.OUTCOME_CHALLENGE);
                    if (statusCode != null) {
                        set(AuditEvent.REASON_CODE, statusCode);
                        set(AuditEvent.REASON_TYPE, AuditUtils.getRequestScheme(req));
                    }
                    break;
                }
                case REDIRECT: {
                    setOutcome(arOutcome != null ? arOutcome : AuditEvent.OUTCOME_REDIRECT);
                    if (statusCode != null) {
                        set(AuditEvent.REASON_CODE, statusCode);
                        set(AuditEvent.REASON_TYPE, AuditUtils.getRequestScheme(req));
                    }
                    break;
                }
                case TAI_CHALLENGE: {
                    setOutcome(arOutcome != null ? arOutcome : AuditEvent.OUTCOME_TAI_CHALLENGE);
                    if (statusCode != null) {
                        set(AuditEvent.REASON_CODE, statusCode);
                        set(AuditEvent.REASON_TYPE, AuditUtils.getRequestScheme(req));
                    }
                    break;
                }
                case REDIRECT_TO_PROVIDER: {
                    setOutcome(arOutcome != null ? arOutcome : AuditEvent.OUTCOME_REDIRECT_TO_PROVIDER);
                    if (statusCode != null) {
                        set(AuditEvent.REASON_CODE, statusCode);
                        set(AuditEvent.REASON_TYPE, AuditUtils.getRequestScheme(req));
                    }
                    break;
                }
                default: {
                    // TODO: what should we do here?
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Unknown AuthenticationResult: " + authResult.getStatus());
                    }
                    break;
                }
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Internal error creating AuthenticationEvent", e);
            }
        }
    }

}
