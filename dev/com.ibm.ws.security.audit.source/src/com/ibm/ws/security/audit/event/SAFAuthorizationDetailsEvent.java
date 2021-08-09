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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.websphere.security.audit.AuditEvent;

/**
 * Creates Audit report for SAF Authentication
 */
public class SAFAuthorizationDetailsEvent extends AuditEvent {

    private static final TraceComponent tc = Tr.register(SAFAuthorizationDetailsEvent.class);

    @SuppressWarnings("unchecked")
    public SAFAuthorizationDetailsEvent() {
        set(AuditEvent.EVENTNAME, AuditConstants.SECURITY_SAF_AUTHZ_DETAILS);
        setInitiator((Map<String, Object>) AuditEvent.STD_INITIATOR.clone());
        setObserver((Map<String, Object>) AuditEvent.STD_OBSERVER.clone());
        setTarget((Map<String, Object>) AuditEvent.STD_TARGET.clone());
    }

    public SAFAuthorizationDetailsEvent(int safReturnCode,
                                        int racfReturnCode,
                                        int racfReasonCode,
                                        String userSecurityName,
                                        String applid,
                                        String safProfile,
                                        String safClass,
                                        Boolean authDecision,
                                        String principalName) {
        this();

        // This is put in by default from AuditEvent, not needed so removing
        AuditEvent.STD_TARGET.remove(AuditEvent.TARGET_TYPEURI);

        set(AuditEvent.TARGET_SAF_RETURN_CODE, safReturnCode);
        set(AuditEvent.TARGET_RACF_RETURN_CODE, racfReturnCode);
        set(AuditEvent.TARGET_RACF_REASON_CODE, racfReasonCode);

        if (userSecurityName != null) {
            set(AuditEvent.TARGET_USER_SECURITY_NAME, userSecurityName);
        }
        if (applid != null) {
            set(AuditEvent.TARGET_APPLID, applid);
        }
        if (safProfile != null) {
            set(AuditEvent.TARGET_SAF_PROFILE, safProfile);
        }
        if (safClass != null) {
            set(AuditEvent.TARGET_SAF_CLASS, safClass);
        }
        if (authDecision != null) {
            if (authDecision == true) {
                set(AuditEvent.OUTCOME, AuditEvent.OUTCOME_SUCCESS);
                set(AuditEvent.TARGET_AUTHORIZATION_DECISION, true);
            } else {
                set(AuditEvent.OUTCOME, AuditEvent.OUTCOME_FAILURE);
                set(AuditEvent.TARGET_AUTHORIZATION_DECISION, false);
            }
        }
        if (principalName != null) {
            set(AuditEvent.TARGET_CREDENTIAL_TOKEN, principalName);
        }
    }
}
