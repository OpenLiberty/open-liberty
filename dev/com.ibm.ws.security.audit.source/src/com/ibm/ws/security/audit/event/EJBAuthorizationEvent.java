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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.AuditAuthenticationResult;
import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.ws.security.audit.utils.AuditUtils;
import com.ibm.ws.security.audit.utils.ParameterUtils;
import com.ibm.ws.webcontainer.security.WebRequest;

/**
 * Class with default values for authorization events
 */
public class EJBAuthorizationEvent extends AuditEvent {

    private static final TraceComponent tc = Tr.register(EJBAuthorizationEvent.class);

    @SuppressWarnings("unchecked")
    public EJBAuthorizationEvent() {
        set(AuditEvent.EVENTNAME, AuditConstants.SECURITY_AUTHZ);
        setInitiator((Map<String, Object>) AuditEvent.STD_INITIATOR.clone());
        setObserver((Map<String, Object>) AuditEvent.STD_OBSERVER.clone());
        setTarget((Map<String, Object>) AuditEvent.STD_TARGET.clone());
    }

    public EJBAuthorizationEvent(AuditAuthenticationResult authResult, HashMap request, Object hreq, Object hwebReq, String realm, Subject subject, Collection<String> roles,
                                 Integer statusCode) {
        this();

        try {
            HttpServletRequest req = ((HttpServletRequest) hreq);
            WebRequest webreq = ((WebRequest) hwebReq);

            if (req != null) {
                String remoteAddr = req.getRemoteAddr();
                if (remoteAddr != null)
                    set(AuditEvent.INITIATOR_HOST_ADDRESS, remoteAddr);

                String agent = req.getHeader("User-Agent");
                if (agent != null)
                    set(AuditEvent.INITIATOR_HOST_AGENT, agent);

                String requestURI = req.getRequestURI();
                if (requestURI != null)
                    set(AuditEvent.TARGET_NAME, URLDecoder.decode(requestURI, "UTF-8"));

                String localAddr = req.getLocalAddr();
                int localPort = req.getLocalPort();
                if (localAddr != null && localPort >= 0)
                    set(AuditEvent.TARGET_HOST_ADDRESS, localAddr + ":" + localPort);

                String queryString = req.getQueryString();
                if (queryString != null) {
                    String str = URLDecoder.decode(queryString, "UTF-8");
                    str = AuditUtils.hidePassword(str);
                    set(AuditEvent.TARGET_PARAMS, str);
                }

                set(AuditEvent.TARGET_METHOD, AuditUtils.getRequestMethod(req));

                String sessionID = AuditUtils.getSessionID(req);
                if (sessionID != null) {
                    set(AuditEvent.TARGET_SESSION, sessionID);
                }

            }

            set(AuditEvent.TARGET_APPNAME, AuditUtils.getJ2EEComponentName());

            if (subject != null) {
                set(AuditEvent.TARGET_CREDENTIAL_TOKEN, subject.getPrincipals().iterator().next().getName());

            } else {
                if (req != null && req.getUserPrincipal() != null && req.getUserPrincipal().getName() != null)
                    set(AuditEvent.TARGET_CREDENTIAL_TOKEN, req.getUserPrincipal().getName());
            }

            set(AuditEvent.TARGET_CREDENTIAL_TYPE, authResult.getAuditCredType());

            if (roles != null && !roles.isEmpty()) {
                set(AuditEvent.TARGET_ROLE_NAMES, roles.toString());
            }

            if (request.get("applicationName") != null) {
                set(AuditEvent.TARGET_APPNAME, request.get("applicationName"));
            }

            if (request.get("moduleName") != null) {
                set(AuditEvent.TARGET_EJB_MODULE_NAME, request.get("moduleName"));
            }

            if (request.get("methodName") != null) {
                set(AuditEvent.TARGET_METHOD, request.get("methodName"));
            }

            if (request.get("methodInterface") != null) {
                set(AuditEvent.TARGET_EJB_METHOD_INTERFACE, request.get("methodInterface"));
            }

            if (request.get("methodSignature") != null) {
                set(AuditEvent.TARGET_EJB_METHOD_SIGNATURE, request.get("methodSignature"));
            }

            if (request.get("beanName") != null) {
                set(AuditEvent.TARGET_EJB_BEAN_NAME, request.get("beanName"));
            }

            if (request.get("methodParameters") != null) {
                set(AuditEvent.TARGET_EJB_METHOD_PARAMETERS, ParameterUtils.format(request.get("methodParameters")).toString());
            }

            if (request.get(AuditEvent.REASON_TYPE) != null)
                set(AuditEvent.REASON_TYPE, request.get(AuditEvent.REASON_TYPE));
            else
                set(AuditEvent.REASON_TYPE, "EBJ");

            set(AuditEvent.TARGET_REALM, AuditUtils.getRealmName());

            if (statusCode == HttpServletResponse.SC_OK) {
                setOutcome("success");
                set(AuditEvent.REASON_CODE, statusCode);
            } else {
                setOutcome("failure");
                set(AuditEvent.REASON_CODE, statusCode);
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Internal error creating EJBAuthorizationEvent", e);
            }
        }
    }
}
