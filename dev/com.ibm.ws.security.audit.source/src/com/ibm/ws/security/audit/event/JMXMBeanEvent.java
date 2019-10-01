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

import javax.management.ObjectName;
import javax.management.QueryExp;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;
import com.ibm.ws.security.audit.utils.AuditUtils;
import com.ibm.ws.security.audit.utils.ParameterUtils;

/**
 * Class with default values for jmx notification events
 */
public class JMXMBeanEvent extends AuditEvent {

    private static final TraceComponent tc = Tr.register(JMXMBeanEvent.class);

    @SuppressWarnings("unchecked")
    public JMXMBeanEvent() {
        set(AuditEvent.EVENTNAME, AuditConstants.JMX_MBEAN);
        setInitiator((Map<String, Object>) AuditEvent.STD_INITIATOR.clone());
        setObserver((Map<String, Object>) AuditEvent.STD_OBSERVER.clone());
        setTarget((Map<String, Object>) AuditEvent.STD_TARGET.clone());
    }

    public JMXMBeanEvent(ObjectName name, String className, ObjectName loader, String operationName, Object[] params, String[] signature, QueryExp query, String action,
                         String outcome, String reason) {
        this();

        try {

            if (name != null)
                set(AuditEvent.TARGET_JMX_MBEAN_NAME, name.toString());

            if (className != null)
                set(AuditEvent.TARGET_JMX_MBEAN_CLASSNAME, className);

            if (action != null) {
                set(AuditEvent.TARGET_JMX_MBEAN_ACTION, action);
            }

            if (loader != null) {
                set(AuditEvent.TARGET_JMX_MBEAN_CLASSLOADER_NAME, loader.toString());
            }

            if (operationName != null) {
                set(AuditEvent.TARGET_JMX_MBEAN_INVOKE_OPERATION, operationName);
            }

            set(AuditEvent.TARGET_REALM, AuditUtils.getRealmName());

            if (params != null) {
                /*
                 * The format of the parameter is enclosed by bracket for each element of root parameters.
                 * For example, if parameter is {1, 2}, the output is [1][2].
                 * If an element is array, it is enclosed another bracket and each elements of the array is
                 * separated by comma. For example, if parameter is {1, {10, 20}}, the output is
                 * [1][[10, 20]].
                 * If there is a nested array, the same rule for the array will apply.
                 * For example, if paramter is {1, (10, {100, 200}}}, the output is
                 * [1][10, [100, 200]].
                 */
                StringBuffer buf = new StringBuffer();
                for (Object element : params) {
                    buf.append("[").append(ParameterUtils.format(element)).append("]");
                }

                String siggy = buf.toString();
                if (!siggy.isEmpty())
                    set(AuditEvent.TARGET_JMX_MBEAN_PARAMS, siggy);
            }

            if (signature != null) {
                StringBuffer buf = new StringBuffer();
                for (int i = 0; i < signature.length; i++) {
                    buf.append("[").append(signature[i]).append("]");
                }
                String siggy = buf.toString();
                if (!siggy.isEmpty())
                    set(AuditEvent.TARGET_JMX_MBEAN_SIGNATURE, siggy);
            }

            if (query != null) {
                set(AuditEvent.TARGET_JMX_MBEAN_QUERYEXP, query.toString());
            }

            set(AuditEvent.OBSERVER_NAME, "JMXService");

            set(AuditEvent.TARGET_TYPEURI, "server/mbean");

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
                Tr.debug(tc, "Internal error creating JMXMBeanEvent", e);
            }
        }
    }
}
