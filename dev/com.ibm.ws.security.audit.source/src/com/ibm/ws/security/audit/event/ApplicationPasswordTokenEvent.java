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
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.ws.security.audit.utils.AuditUtils;

/**
 * Class with default values for authentication events
 */
public class ApplicationPasswordTokenEvent extends AuditEvent {

    private static final TraceComponent tc = Tr.register(ApplicationPasswordTokenEvent.class);

    @SuppressWarnings("unchecked")
    public ApplicationPasswordTokenEvent() {
        set(AuditEvent.EVENTNAME, AuditConstants.APPLICATION_TOKEN_MANAGEMENT);
        setInitiator((Map<String, Object>) AuditEvent.STD_INITIATOR.clone());
        setObserver((Map<String, Object>) AuditEvent.STD_OBSERVER.clone());
        setTarget((Map<String, Object>) AuditEvent.STD_TARGET.clone());

        set(AuditEvent.OBSERVER_NAME, "OidcSecurityService");
    }

    public ApplicationPasswordTokenEvent(Map<String, Object> m) {
        this();

        try {
            HttpServletRequest req = (HttpServletRequest) m.get(AuditConstants.REQUEST);
            HttpServletResponse res = (HttpServletResponse) m.get(AuditConstants.RESPONSE);

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

            set(AuditEvent.TARGET_TYPEURI, "service/oidc");

            set(AuditEvent.TARGET_HOST_ADDRESS, req.getLocalAddr() + ":" + req.getLocalPort());

            if (req.getUserPrincipal() != null && req.getUserPrincipal().getName() != null)
                set(AuditEvent.TARGET_CREDENTIAL_TOKEN, req.getUserPrincipal().getName());

            if ((String) m.get(AuditConstants.INITIATOR_ROLE) != null) {
                set(AuditEvent.TARGET_INITIATOR_ROLE, m.get(AuditConstants.INITIATOR_ROLE));
            }

            String sessionID = AuditUtils.getSessionID(req);
            if (sessionID != null) {
                set(AuditEvent.TARGET_SESSION, sessionID);
            }

            String endpoint = (String) m.get(AuditConstants.ENDPOINT);
            if (endpoint != null) {
                set(AuditEvent.TARGET_ENDPOINT, endpoint);
                if ((String) m.get(AuditConstants.APP_OR_TOKEN_ID) != null) {
                    set(AuditEvent.TARGET_APPLICATION_ID, m.get(AuditConstants.APP_OR_TOKEN_ID));
                }
            }

            if ((String) m.get("clientId") != null) {
                set(AuditEvent.TARGET_CLIENT_ID, m.get(AuditConstants.CLIENT_ID));
            }

            if ((String) m.get(AuditConstants.PROVIDER) != null) {
                set(AuditEvent.TARGET_PROVIDER, m.get(AuditConstants.PROVIDER));
            }

            String user_id = "";

            if (req.getQueryString() != null) {
                String queryString = URLDecoder.decode(req.getQueryString(), "UTF-8");

                if (queryString != null) {
                    int index1 = queryString.indexOf("user_id=");
                    if (index1 != -1) {
                        String queryString2 = queryString.substring(index1, queryString.length());
                        int index2 = queryString2.indexOf("&");
                        if (index2 != -1 && index2 != 8) {
                            user_id = queryString2.substring(8, index2);
                        } else if (index2 != -1 && index2 == 8) {
                            user_id = "";
                        } else {
                            user_id = queryString2.substring(8, queryString2.length());
                        }
                        set(AuditEvent.TARGET_USERID, user_id);
                    }
                }
            } else if ((String) m.get(AuditConstants.USER) != null) {
                set(AuditEvent.TARGET_USERID, m.get(AuditConstants.USER));

            } else if (req.getUserPrincipal() != null && req.getUserPrincipal().getName() != null) {
                set(AuditEvent.TARGET_USERID, req.getUserPrincipal().getName());
            }

            if (req.getQueryString() != null) {
                String str = URLDecoder.decode(req.getQueryString(), "UTF-8");
                str = AuditUtils.hidePassword(str);
                set(AuditEvent.TARGET_PARAMS, str);
            }

            set(AuditEvent.TARGET_METHOD, AuditUtils.getRequestMethod(req));

            if (AuditUtils.getRequestMethod(req).equals("POST")) {
                if (endpoint != null) {
                    String sos = (String) m.get("respBody");
                    if (sos != null) {
                        if (sos.indexOf("app_id") != -1) {
                            int index1 = sos.indexOf("app_id");
                            int index2 = sos.indexOf("created_at");
                            if (index2 != -1) {
                                String s1 = sos.substring(index1 + 9, index2 - 3);
                                set(AuditEvent.TARGET_APPLICATION_ID, s1);
                            }
                        }
                    }
                }
            }

            if (AuditUtils.getRequestMethod(req).equals("DELETE")) {
                set(AuditEvent.TARGET_NUMBER_REVOKED, m.get(AuditConstants.NUMBER_REVOKED));
            }

            String authResult = (String) m.get(AuditConstants.AUDIT_OUTCOME);
            if (authResult.equals(AuditConstants.SUCCESS)) {
                setOutcome(AuditEvent.OUTCOME_SUCCESS);
                set(AuditEvent.REASON_CODE, res.getStatus());
                set(AuditEvent.REASON_TYPE, AuditUtils.getRequestScheme(req));
            } else if (authResult.equals(AuditConstants.FAILURE)) {
                setOutcome(AuditEvent.OUTCOME_FAILURE);
                set(AuditEvent.REASON_CODE, res.getStatus());
                String detailedError = (String) m.get(AuditConstants.DETAILED_ERROR);
                if (detailedError != null)
                    set(AuditEvent.REASON_TYPE, AuditUtils.getRequestScheme(req).concat(": ").concat(detailedError));
                else
                    set(AuditEvent.REASON_TYPE, AuditUtils.getRequestScheme(req));
            }

        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Internal error creating ApplicationPasswordTokenEvent", e);
            }
        }
    }
}
