/*******************************************************************************
 * Copyright (c) 2015, 2026 IBM Corporation and others.
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
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.transaction;

import org.omg.CORBA.LocalObject;
import org.omg.CORBA.Policy;

/**
 * Minimal CORBA policy that serves as an enablement gate for client-side
 * transaction propagation. The presence of this policy indicates that the
 * transaction subsystem is properly initialized and ready to propagate
 * transactions.
 *
 * This policy does not carry configuration data or service references - it
 * simply signals that transaction propagation is enabled. The interceptor
 * uses TransactionSubsystemFactory.getActiveFactory() to access exporters.
 *
 * @version $Rev: 451417 $ $Date: 2006-09-29 13:13:22 -0700 (Fri, 29 Sep 2006) $
 */
public class ClientTransactionPolicy extends LocalObject implements Policy {
    private static final long serialVersionUID = 1L;

    /**
     * Create a client transaction policy.
     * The presence of this policy signals that transaction propagation is enabled.
     * No state is needed - this is just an enablement marker.
     */
    public ClientTransactionPolicy() {
        // Stateless - just an enablement marker
    }

    @Override
    public int policy_type() {
        return ClientTransactionPolicyFactory.POLICY_TYPE;
    }

    @Override
    public Policy copy() {
        return new ClientTransactionPolicy();
    }

    @Override
    public void destroy() {
        // Nothing to destroy - stateless policy
    }
}
