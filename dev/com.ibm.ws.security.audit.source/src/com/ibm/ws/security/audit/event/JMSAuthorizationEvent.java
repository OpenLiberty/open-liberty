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

import java.util.Arrays;
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
public class JMSAuthorizationEvent extends AuditEvent {

    private static final TraceComponent tc = Tr.register(JMSAuthorizationEvent.class);

    @SuppressWarnings("unchecked")
    public JMSAuthorizationEvent() {
        set(AuditEvent.EVENTNAME, AuditConstants.SECURITY_JMS_AUTHZ);
        setInitiator((Map<String, Object>) AuditEvent.STD_INITIATOR.clone());
        setObserver((Map<String, Object>) AuditEvent.STD_OBSERVER.clone());
        setTarget((Map<String, Object>) AuditEvent.STD_TARGET.clone());
    }

    public JMSAuthorizationEvent(String userName, String hostAddress, String port, String chainName, String busName, String messagingEngine, String destination,
                                 String operationType, String[] roles, String resource, Integer statusCode) {
        this();
        try {

            if (busName != null)
                set(AuditEvent.TARGET_MESSAGING_BUSNAME, busName);
            else
                set(AuditEvent.TARGET_MESSAGING_BUSNAME, "defaultBus");

            if (messagingEngine != null)
                set(AuditEvent.TARGET_MESSAGING_ENGINE, messagingEngine);
            else
                set(AuditEvent.TARGET_MESSAGING_ENGINE, "defaultME");

            set(AuditEvent.OBSERVER_NAME, "JMSMessagingImplementation");
            if (userName != null) {
                set(AuditEvent.TARGET_CREDENTIAL_TOKEN, userName);
                set(AuditEvent.TARGET_CREDENTIAL_TYPE, "BASIC");

            }
            if (destination != null) {
                set(AuditEvent.TARGET_MESSAGING_DESTINATION, destination);
            }
            if (operationType != null) {
                set(AuditEvent.TARGET_MESSAGING_OPERATIONTYPE, operationType);
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

            if (chainName != null)
                set(AuditEvent.TARGET_MESSAGING_REMOTE_CHAIN_NAME, chainName);

            set(AuditEvent.TARGET_REALM, AuditUtils.getRealmName());

            if (roles != null && roles.length != 0) {
                set(AuditEvent.TARGET_MESSAGING_JMS_ACTIONS, Arrays.toString(roles));
            }

            if (resource != null) {
                set(AuditEvent.TARGET_MESSAGING_JMS_RESOURCE, resource);
            }

            if (statusCode == HttpServletResponse.SC_OK) {
                setOutcome("success");
                set(AuditEvent.REASON_CODE, statusCode);
                set(AuditEvent.REASON_TYPE, "JMS");
            } else {
                setOutcome("failure");
                set(AuditEvent.REASON_CODE, statusCode);
                set(AuditEvent.REASON_TYPE, "JMS");
            }
            set(AuditEvent.TARGET_TYPEURI, "service/jms/messagingResource");
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Internal error creating JMSAuthorizationEvent", e);
            }
        }
    }
}