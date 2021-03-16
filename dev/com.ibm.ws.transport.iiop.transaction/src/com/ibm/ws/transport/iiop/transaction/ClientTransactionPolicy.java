/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * @version $Rev: 451417 $ $Date: 2006-09-29 13:13:22 -0700 (Fri, 29 Sep 2006) $
 */
public class ClientTransactionPolicy extends LocalObject implements Policy {

    private final ClientTransactionPolicyConfig clientTransactionPolicyConfig;

    public ClientTransactionPolicy(ClientTransactionPolicyConfig clientTransactionPolicyConfig) {
        this.clientTransactionPolicyConfig = clientTransactionPolicyConfig;
    }


    public int policy_type() {
        return ClientTransactionPolicyFactory.POLICY_TYPE;
    }

    public Policy copy() {
        return new ClientTransactionPolicy(clientTransactionPolicyConfig);
    }

    public void destroy() {

    }

    ClientTransactionPolicyConfig getClientTransactionPolicyConfig() {
        return clientTransactionPolicyConfig;
    }
}
