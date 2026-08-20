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

import org.omg.CORBA.Any;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.Policy;
import org.omg.CORBA.PolicyError;
import org.omg.PortableInterceptor.PolicyFactory;

/**
 * CORBA PolicyFactory for creating ClientTransactionPolicy instances.
 *
 * This factory creates stateless policies that serve as enablement gates.
 * The policy presence indicates that transaction propagation is enabled.
 *
 * @version $Rev: 451417 $ $Date: 2006-09-29 13:13:22 -0700 (Fri, 29 Sep 2006) $
 */
public class ClientTransactionPolicyFactory extends LocalObject implements PolicyFactory {
    private static final long serialVersionUID = 1L;
    public final static int POLICY_TYPE = 0x41534603;

    /**
     * Create a ClientTransactionPolicy.
     * The value parameter is ignored - the policy is stateless.
     *
     * @param type policy type (must be POLICY_TYPE)
     * @param value ignored (policy is stateless)
     * @return ClientTransactionPolicy instance
     * @throws PolicyError if type is wrong
     */
    @Override
    public Policy create_policy(int type, Any value) throws PolicyError {
        if (type != POLICY_TYPE) {
            throw new PolicyError(org.omg.CORBA.BAD_POLICY.value);
        }
        
        // Create stateless policy - value parameter ignored
        return new ClientTransactionPolicy();
    }
}
