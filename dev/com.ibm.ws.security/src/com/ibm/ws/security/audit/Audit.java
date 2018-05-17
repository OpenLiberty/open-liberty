/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.audit;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * This class provides an audit probe point method and constants.
 */
@Trivial
public class Audit {
    @Trivial
    public static enum EventID {
        SECURITY_AUTHN_01,
        SECURITY_AUTHZ_01, // web
        SECURITY_AUTHZ_02, // jacc web
        SECURITY_AUTHZ_03, // jacc ejb
        SECURITY_AUTHZ_04, // ejb
        SECURITY_AUDIT_MGMT_01,
        SECURITY_AUDIT_MGMT_02,
        SECURITY_AUTHN_DELEGATION_01,
        SECURITY_AUTHZ_DELEGATION_01,
        SECURITY_API_AUTHN_01,
        SECURITY_API_AUTHN_TERMINATE_01,
        SECURITY_AUTHN_TERMINATE_01,
        SECURITY_AUTHN_FAILOVER_01,
        SECURITY_MEMBER_MGMT_01,
        SECURITY_JMS_AUTHN_01,
        SECURITY_JMS_AUTHZ_01,
        SECURITY_JMS_AUTHN_TERMINATE_01,
        SECURITY_JMS_CLOSED_CONNECTION_01,
        JMX_NOTIFICATION_01,
        JMX_MBEAN_01,
        JMX_MBEAN_ATTRIBUTES_01,
        JMX_MBEAN_REGISTER_01

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
