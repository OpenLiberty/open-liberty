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

import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.ws.security.audit.utils.AuditUtils;
import com.ibm.wsspi.rest.handler.RESTRequest;
import com.ibm.wsspi.rest.handler.RESTResponse;

/**
 * REST Handler authorization event
 */
public class RESTAuthorizationEvent extends AuditEvent {

    private static final TraceComponent tc = Tr.register(RESTAuthorizationEvent.class);

    @SuppressWarnings("unchecked")
    public RESTAuthorizationEvent() {
        set(AuditEvent.EVENTNAME, AuditConstants.SECURITY_REST_HANDLER_AUTHZ);
        setInitiator((Map<String, Object>) AuditEvent.STD_INITIATOR.clone());
        setObserver((Map<String, Object>) AuditEvent.STD_OBSERVER.clone());
        setTarget((Map<String, Object>) AuditEvent.STD_TARGET.clone());
    }

    public RESTAuthorizationEvent(Object request, Object resp) {
        this();
        try {
            RESTRequest req = (RESTRequest) request;
            RESTResponse response = (RESTResponse) resp;
            // add initiator
            if (req != null && req.getRemoteAddr() != null)
                set(AuditEvent.INITIATOR_HOST_ADDRESS, req.getRemoteAddr());
            String agent = req.getHeader("User-Agent");
            if (agent != null)
                set(AuditEvent.INITIATOR_HOST_AGENT, agent);
            // add target
            set(AuditEvent.TARGET_NAME, URLDecoder.decode(req.getContextPath() + req.getPath(), "UTF-8"));
            set(AuditEvent.TARGET_APPNAME, AuditUtils.getJ2EEComponentName());
            if (req.getQueryString() != null) {
                String str = URLDecoder.decode(req.getQueryString(), "UTF-8");
                str = AuditUtils.hidePassword(str);
                set(AuditEvent.TARGET_PARAMS, str);
            }

            //Get the host and port. If the port is not specified, it is the default SSL port: 443
            URL url = new URL(req.getURL());
            int port = url.getPort() == -1 ? 443 : url.getPort();

            set(AuditEvent.TARGET_HOST_ADDRESS, url.getHost() + ":" + port);
            set(AuditEvent.TARGET_CREDENTIAL_TYPE, AuditEvent.CRED_TYPE_BASIC);
            if (req.getUserPrincipal() != null && req.getUserPrincipal().getName() != null)
                set(AuditEvent.TARGET_CREDENTIAL_TOKEN, req.getUserPrincipal().getName());
            set(AuditEvent.TARGET_METHOD, req.getMethod());
            String sessionID = req.getSessionId();
            if (sessionID != null) {
                set(AuditEvent.TARGET_SESSION, sessionID);
            }

            set(AuditEvent.TARGET_REALM, AuditUtils.getRealmName());

            if (response.getRequiredRoles() != null) {
                ArrayList<String> rolesList = new ArrayList<String>();
                for (String role : response.getRequiredRoles()) {
                    rolesList.add(role);
                }
                if (!rolesList.isEmpty())
                    set(AuditEvent.TARGET_ROLE_NAMES, rolesList.toString());
            }
            int statusCode = response.getStatus();
            if (statusCode == HttpServletResponse.SC_OK) {
                setOutcome("success");
                set(AuditEvent.REASON_CODE, statusCode);
            } else {
                setOutcome("failure");
                set(AuditEvent.REASON_CODE, statusCode);
            }
            set(AuditEvent.REASON_TYPE, AuditEvent.REASON_TYPE_HTTPS);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Internal error creating RESTAuthorizationEvent", e);
            }
        }
    }

}
