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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.ws.security.audit.utils.AuditUtils;

/**
 * Class with default values for authorization delegation events
 */
public class AuthorizationDelegationEvent extends AuditEvent {

    private static final TraceComponent tc = Tr.register(AuthorizationDelegationEvent.class);

    @SuppressWarnings("unchecked")
    public AuthorizationDelegationEvent() {
        set(AuditEvent.EVENTNAME, AuditConstants.SECURITY_AUTHZ_DELEGATION);
        setInitiator((Map<String, Object>) AuditEvent.STD_INITIATOR.clone());
        setObserver((Map<String, Object>) AuditEvent.STD_OBSERVER.clone());
        setTarget((Map<String, Object>) AuditEvent.STD_TARGET.clone());
    }

    public AuthorizationDelegationEvent(HashMap<String, Object> extraAuditData, Integer statusCode) {

        this();
        try {
            HttpServletRequest req = (HttpServletRequest) extraAuditData.get("HTTP_SERVLET_REQUEST");

            if (req != null) {
                // add initiator
                if (req.getRemoteAddr() != null)
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
                set(AuditEvent.TARGET_CREDENTIAL_TYPE, "BASIC");
                if (req.getUserPrincipal() != null && req.getUserPrincipal().getName() != null)
                    set(AuditEvent.TARGET_CREDENTIAL_TOKEN, req.getUserPrincipal().getName());
                //else if (authResult.getAuditCredValue() != null)
                //    set(AuditEvent.TARGET_CREDENTIAL_TOKEN, authResult.getAuditCredValue());

                String sessionID = AuditUtils.getSessionID(req);

                if (sessionID != null) {
                    set(AuditEvent.TARGET_SESSION, sessionID);
                }

                set(AuditEvent.TARGET_REALM, AuditUtils.getRealmName());

                if (extraAuditData.get("DELEGATION_USERS_LIST") != null) {
                    ArrayList<String> delUsers = new ArrayList<String>();
                    delUsers = (ArrayList<String>) extraAuditData.get("DELEGATION_USERS_LIST");
                    String users = "";
                    for (int i = 0; i < delUsers.size(); i++) {
                        users = users.concat(delUsers.get(i)).concat("; ");
                    }
                    set(AuditEvent.TARGET_DELEGATION_USERS, users.substring(0, users.length() - 2));

                }
                if (extraAuditData.get("RUN_AS_ROLE") != null)
                    set(AuditEvent.TARGET_RUNAS_ROLE, extraAuditData.get("RUN_AS_ROLE"));

                set(AuditEvent.TARGET_METHOD, AuditUtils.getRequestMethod(req));

                if (statusCode == 200) {
                    setOutcome(AuditEvent.OUTCOME_SUCCESS);
                } else {
                    setOutcome(AuditEvent.OUTCOME_FAILURE);
                }
                set(AuditEvent.REASON_CODE, statusCode);
                set(AuditEvent.REASON_TYPE, "EJB");

            }

        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Internal error creating AuthenticationDelegationEvent", e);
            }
        }
    }
}
