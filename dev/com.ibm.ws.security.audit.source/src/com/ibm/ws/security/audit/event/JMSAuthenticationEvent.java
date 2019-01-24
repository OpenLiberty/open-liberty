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

import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.ws.security.audit.utils.AuditConstants;
import com.ibm.ws.security.audit.utils.AuditUtils;

/**
 * Class with default values for authorization events
 */
public class JMSAuthenticationEvent extends AuditEvent {

    private static final TraceComponent tc = Tr.register(JMSAuthenticationEvent.class);

    @SuppressWarnings("unchecked")
    public JMSAuthenticationEvent() {
        set(AuditEvent.EVENTNAME, AuditConstants.SECURITY_JMS_AUTHN);
        setInitiator((Map<String, Object>) AuditEvent.STD_INITIATOR.clone());
        setObserver((Map<String, Object>) AuditEvent.STD_OBSERVER.clone());
        setTarget((Map<String, Object>) AuditEvent.STD_TARGET.clone());
    }

    public JMSAuthenticationEvent(String userName, String hostAddress, String port, String chainName, String busName, String messagingEngine, String credentialType,
                                  Integer statusCode) {
        this();
        try {

            if (busName != null)
                set(AuditEvent.TARGET_MESSAGING_BUSNAME, busName);

            if (messagingEngine != null)
                set(AuditEvent.TARGET_MESSAGING_ENGINE, messagingEngine);

            set(AuditEvent.OBSERVER_NAME, "JMSMessagingImplementation");
            if (userName != null) {
                set(AuditEvent.TARGET_CREDENTIAL_TOKEN, userName);
                set(AuditEvent.TARGET_CREDENTIAL_TYPE, "BASIC");

            }
            if (credentialType != null) {
                set(AuditEvent.TARGET_MESSAGING_LOGIN_TYPE, credentialType);
            }

            if (hostAddress != null) {
                set(AuditEvent.TARGET_MESSAGING_CALLTYPE, "remote");
                set(AuditEvent.TARGET_HOST_ADDRESS, hostAddress);
                if (port != null)
                    set(AuditEvent.TARGET_HOST_ADDRESS, hostAddress + ":" + port);
            } else {
                set(AuditEvent.TARGET_MESSAGING_CALLTYPE, "local");
                set(AuditEvent.TARGET_HOST_ADDRESS, "127.0.0.1:8010");
            }

            set(AuditEvent.TARGET_REALM, AuditUtils.getRealmName());

            if (chainName != null)
                set(AuditEvent.TARGET_MESSAGING_REMOTE_CHAIN_NAME, chainName);

            if (statusCode == HttpServletResponse.SC_OK) {
                setOutcome("success");
                set(AuditEvent.REASON_CODE, statusCode);
                set(AuditEvent.REASON_TYPE, "JMS");
            } else {
                setOutcome("failure");
                set(AuditEvent.REASON_CODE, statusCode);
                set(AuditEvent.REASON_TYPE, "JMS");
            }

            set(AuditEvent.TARGET_TYPEURI, "service/jms/messagingEngine");
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Internal error creating JMSAuthenticationEvent", e);
            }
        }
    }
}