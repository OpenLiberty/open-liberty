/*******************************************************************************
 * Copyright (c) 2016, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.audit;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.audit.AuditConstants;
import com.ibm.ws.security.utils.SecurityUtils;
import com.ibm.wsspi.security.audit.AuditService;

/**
 * This class provides an audit probe point method and constants.
 */
@Trivial
public class Audit {
    @Trivial
    public static enum EventID {
        SECURITY_AUTHN_01(AuditConstants.SECURITY_AUTHN),
        SECURITY_AUTHZ_01(AuditConstants.SECURITY_AUTHZ), // web
        SECURITY_AUTHZ_02(AuditConstants.SECURITY_AUTHZ), // jacc web
        SECURITY_AUTHZ_03(AuditConstants.SECURITY_AUTHZ), // jacc ejb
        SECURITY_AUTHZ_04(AuditConstants.SECURITY_AUTHZ), // ejb
        SECURITY_AUDIT_MGMT_01(AuditConstants.SECURITY_AUDIT_MGMT),
        SECURITY_AUDIT_MGMT_02(AuditConstants.SECURITY_AUDIT_MGMT),
        SECURITY_AUTHN_DELEGATION_01(AuditConstants.SECURITY_AUTHN_DELEGATION),
        SECURITY_AUTHZ_DELEGATION_01(AuditConstants.SECURITY_AUTHZ_DELEGATION),
        SECURITY_API_AUTHN_01(AuditConstants.SECURITY_API_AUTHN),
        SECURITY_API_AUTHN_TERMINATE_01(AuditConstants.SECURITY_API_AUTHN_TERMINATE),
        SECURITY_AUTHN_TERMINATE_01(AuditConstants.SECURITY_AUTHN_TERMINATE),
        SECURITY_AUTHN_FAILOVER_01(AuditConstants.SECURITY_AUTHN_FAILOVER),
        SECURITY_MEMBER_MGMT_01(AuditConstants.SECURITY_MEMBER_MGMT),
        SECURITY_JMS_AUTHN_01(AuditConstants.SECURITY_JMS_AUTHN),
        SECURITY_JMS_AUTHZ_01(AuditConstants.SECURITY_JMS_AUTHZ),
        SECURITY_JMS_AUTHN_TERMINATE_01(AuditConstants.SECURITY_JMS_AUTHN_TERMINATE),
        SECURITY_JMS_CLOSED_CONNECTION_01(AuditConstants.SECURITY_JMS_CLOSED_CONNECTION),
        SECURITY_REST_HANDLER_AUTHZ(AuditConstants.SECURITY_REST_HANDLER_AUTHZ),
        SECURITY_SAF_AUTHZ(AuditConstants.SECURITY_SAF_AUTHZ),
        SECURITY_SAF_AUTHZ_DETAILS(AuditConstants.SECURITY_SAF_AUTHZ_DETAILS),
        JMX_NOTIFICATION_01(AuditConstants.JMX_NOTIFICATION),
        JMX_MBEAN_01(AuditConstants.JMX_MBEAN),
        JMX_MBEAN_ATTRIBUTES_01(AuditConstants.JMX_MBEAN_ATTRIBUTES),
        JMX_MBEAN_REGISTER_01(AuditConstants.JMX_MBEAN_REGISTER),
        APPLICATION_PASSWORD_TOKEN_01(AuditConstants.APPLICATION_TOKEN_MANAGEMENT),
        SERVER_CONFIG_CHANGE_01(AuditConstants.SERVER_CONFIG_CHANGE);

        final String eventType;

        EventID(String eventType) {
            this.eventType = eventType;
        }

        public String getEventType() {
            return eventType;
        }

    }

    public static boolean isAuditServiceEnabled() {
        return SecurityUtils.getAuditService() != null;
    }

    public static boolean isAuditRequired(EventID eventID, String outcome) {
        AuditService auditService = SecurityUtils.getAuditService();
        if (auditService == null) {
            return false;
        }
        return auditService.isAuditRequired(eventID.getEventType(), outcome);
    }

    /**
     * Audit probe point. This method should be called to generate a
     * security audit record. It does nothing and returns nothing - the audit
     * record is produced by a com.ibm.wsspi.probeExtension.ProbeExtension
     * implementation which will be called (via bytecode injection) when the
     * audit feature is enabled and this method is invoked.
     *
     * @param eventId -
     *            The unique ID identifying the ProbeExtension method to be
     *            called to generate the audit record. The ID should be defined
     *            in the Audit.EventID enumeration. An ID should be defined
     *            for each unique set of params to be passed to the ProbeExtension.
     * @param params -
     *            The objects needed to produce the audit record.
     */
    @Trivial
    public static void audit(EventID eventId, Object... params) {}
}
