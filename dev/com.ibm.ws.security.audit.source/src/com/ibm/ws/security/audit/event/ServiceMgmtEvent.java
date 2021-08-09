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

/**
 *
 */

import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;

/**
 * Class with default values for authentication events
 */
public class ServiceMgmtEvent extends AuditEvent {

    private static final TraceComponent tc = Tr.register(ServiceMgmtEvent.class);

    @SuppressWarnings("unchecked")
    public ServiceMgmtEvent() {
        setInitiator((Map<String, Object>) AuditEvent.STD_INITIATOR.clone());
        setObserver((Map<String, Object>) AuditEvent.STD_OBSERVER.clone());
        setTarget((Map<String, Object>) AuditEvent.STD_TARGET.clone());

        set(AuditEvent.EVENTNAME, AuditConstants.SECURITY_SERVICE_MGMT);

    }

    public ServiceMgmtEvent(Map<String, Object> configuration, String serviceName, String action) {
        this();
        setOutcome("success");
        set(AuditEvent.OBSERVER_NAME, serviceName);
        set(AuditEvent.TARGET_TYPEURI, "service/" + serviceName + "/" + action);
    }
}
