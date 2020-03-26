/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
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
 * Creates Audit report for SAF Authorization
 */
public class SAFAuthorizationEvent extends AuditEvent {

    private static final TraceComponent tc = Tr.register(SAFAuthorizationEvent.class);

    @SuppressWarnings("unchecked")
    public SAFAuthorizationEvent() {
        set(AuditEvent.EVENTNAME, AuditConstants.SECURITY_SAF_AUTHZ);
        setInitiator((Map<String, Object>) AuditEvent.STD_INITIATOR.clone());
        setObserver((Map<String, Object>) AuditEvent.STD_OBSERVER.clone());
        setTarget((Map<String, Object>) AuditEvent.STD_TARGET.clone());
    }

    public SAFAuthorizationEvent(int safReturnCode,
                                 int racfReturnCode,
                                 int racfReasonCode,
                                 String userSecurityName,
                                 String applID,
                                 String safProfile,
                                 String safClass,
                                 Boolean authDecision,
                                 String principalName,
                                 String accessLevel,
                                 String errorMessage,
                                 String methodName,
                                 String volser,
                                 String vsam) {
        this();

        // This is put in by default from AuditEvent, not needed so removing
        AuditEvent.STD_TARGET.remove(AuditEvent.TARGET_TYPEURI);
        if (safReturnCode != -1) {
            set(AuditEvent.TARGET_SAF_RETURN_CODE, safReturnCode);
        }
        if (racfReturnCode != -1) {
            set(AuditEvent.TARGET_RACF_RETURN_CODE, racfReturnCode);
        }
        if (racfReasonCode != -1) {
            set(AuditEvent.TARGET_RACF_REASON_CODE, racfReasonCode);
        }
        if (userSecurityName != null) {
            set(AuditEvent.TARGET_USER_SECURITY_NAME, userSecurityName);
        }
        if (applID != null) {
            set(AuditEvent.TARGET_APPLID, applID);
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
        if (accessLevel != null) {
            set(AuditEvent.TARGET_ACCESS_LEVEL, accessLevel);
        }
        if (errorMessage != null) {
            set(AuditEvent.TARGET_SAF_ERROR_MESSAGE, errorMessage);
        }
        // Used to specify the method name where the audit record was cut
        // Ex: if cut in SAFPasswordChangeUtility.changePassword, methodName would be 'changePassword'
        if (methodName != null) {
            set(AuditEvent.TARGET_METHOD, methodName);
        }
        // Fields for checking SAF isAuthorizedToDataset
        if (volser != null) {
            set(AuditEvent.TARGET_VOLSER, volser);
        }
        if (vsam != null) {
            set(AuditEvent.TARGET_VSAM, vsam);
        }
    }
}
