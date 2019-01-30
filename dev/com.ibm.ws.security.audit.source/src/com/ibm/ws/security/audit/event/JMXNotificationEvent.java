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

import javax.management.NotificationFilter;
import javax.management.ObjectName;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.ws.security.audit.utils.AuditUtils;

/**
 * Class with default values for jmx notification events
 */
public class JMXNotificationEvent extends AuditEvent {

    private static final TraceComponent tc = Tr.register(JMXNotificationEvent.class);

    @SuppressWarnings("unchecked")
    public JMXNotificationEvent() {
        set(AuditEvent.EVENTNAME, AuditConstants.JMX_NOTIFICATION);
        setInitiator((Map<String, Object>) AuditEvent.STD_INITIATOR.clone());
        setObserver((Map<String, Object>) AuditEvent.STD_OBSERVER.clone());
        setTarget((Map<String, Object>) AuditEvent.STD_TARGET.clone());
    }

    public JMXNotificationEvent(ObjectName name, Object listener, NotificationFilter filter, Object handback, String action, String outcome, String reason) {
        this();

        try {

            if (name != null)

                set(AuditEvent.TARGET_JMX_NOTIFICATION_NAME, name.toString());

            if (listener != null) {
                if (listener instanceof ObjectName) {
                    String s = listener.toString();
                    if (s.contains("@")) {
                        s = s.substring(0, s.indexOf("@"));
                    }
                    set(AuditEvent.TARGET_JMX_NOTIFICATION_LISTENER, s);
                } else {
                    set(AuditEvent.TARGET_JMX_NOTIFICATION_LISTENER, listener.getClass().getName());
                }
            }

            if (filter != null) {
                String s = filter.toString();
                if (s.contains("@")) {
                    s = s.substring(0, s.indexOf("@"));
                }
                set(AuditEvent.TARGET_JMX_NOTIFICATION_FILTER, s);

            }

            if (handback != null)
                set(AuditEvent.TARGET_JMX_NOTIFICATION_HANDBACK, handback.toString());

            if (action != null)
                set(AuditEvent.TARGET_JMX_MBEAN_ACTION, action);

            set(AuditEvent.OBSERVER_NAME, "JMXService");
            set(AuditEvent.TARGET_TYPEURI, "server/mbean/notification");
            set(AuditEvent.TARGET_REALM, AuditUtils.getRealmName());

            if (outcome.equals("success")) {
                setOutcome("success");
                set(AuditEvent.REASON_CODE, 200);
                set(AuditEvent.REASON_TYPE, reason);
            } else {
                setOutcome("failure");
                set(AuditEvent.REASON_CODE, 201);
                set(AuditEvent.REASON_TYPE, reason);
            }

        } catch (Exception e) {

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Internal error creating JMXNotificationEvent", e);
            }
        }
    }
}
