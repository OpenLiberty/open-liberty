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

/**
 * Class with default values for authentication events
 */
public class ApiAuthnTerminateEvent extends AuditEvent {

    private static final TraceComponent tc = Tr.register(ApiAuthnTerminateEvent.class);

    @SuppressWarnings("unchecked")
    public ApiAuthnTerminateEvent() {
        set(AuditEvent.EVENTNAME, AuditConstants.SECURITY_API_AUTHN_TERMINATE);
        setInitiator((Map<String, Object>) AuditEvent.STD_INITIATOR.clone());
        setObserver((Map<String, Object>) AuditEvent.STD_OBSERVER.clone());
        setTarget((Map<String, Object>) AuditEvent.STD_TARGET.clone());
    }

    public ApiAuthnTerminateEvent(HttpServletRequest req, AuthenticationResult authResult, Integer statusCode) {
        this();
        try {
            // add initiator
            if (req != null && req.getRemoteAddr() != null)
                set(AuditEvent.INITIATOR_HOST_ADDRESS, req.getRemoteAddr());
            String agent = req.getHeader("User-Agent");
            if (agent != null)
                set(AuditEvent.INITIATOR_HOST_AGENT, agent);
            // add target

            set(AuditEvent.TARGET_NAME, URLDecoder.decode(req.getRequestURI(), "UTF-8"));
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

            set(AuditEvent.TARGET_APPNAME, AuditUtils.getJ2EEComponentName());

            String sessionID = AuditUtils.getSessionID(req);
            if (sessionID != null) {
                set(AuditEvent.TARGET_SESSION, sessionID);
            }
            set(AuditEvent.TARGET_METHOD, AuditUtils.getRequestMethod(req));

            set(AuditEvent.TARGET_REALM, AuditUtils.getRealmName());

            if (authResult.getAuditAuthConfigProviderName() != null) {
                set(AuditEvent.TARGET_JASPI_PROVIDER, authResult.getAuditAuthConfigProviderName());
            }

            if (authResult.getAuditAuthConfigProviderAuthType() != null) {
                set(AuditEvent.TARGET_JASPI_AUTHTYPE, authResult.getAuditAuthConfigProviderAuthType());
            }

            if (statusCode != null) {
                set(AuditEvent.REASON_CODE, statusCode);
                set(AuditEvent.REASON_TYPE, AuditUtils.getRequestScheme(req));
                if (statusCode == 200) {
                    setOutcome(AuditEvent.OUTCOME_SUCCESS);
                } else {
                    setOutcome(AuditEvent.OUTCOME_FAILURE);
                }
            }

        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Internal error creating SessionLogoutEvent", e);
            }
        }
    }

}
