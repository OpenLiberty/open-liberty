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
 * CORBA policy for server transaction configuration.
 * 
 * This policy holds lightweight, truly serializable configuration data.
 * Services (TransactionManager, RemoteTransactionController, import handlers)
 * are accessed separately via TransactionServiceLocator.
 * 
 * This design satisfies CORBA specification requirements for serializable policies
 * while maintaining access to non-serializable OSGi services.
 * 
 * @version $Rev: 451417 $ $Date: 2006-09-29 13:13:22 -0700 (Fri, 29 Sep 2006) $
 */
public class ServerTransactionPolicy extends LocalObject implements Policy {
    private static final long serialVersionUID = 1L;
    
    private final ServerTransactionPolicyConfig config;

    /**
     * Creates a new server transaction policy with the specified configuration.
     * 
     * @param config the policy configuration (must be truly serializable)
     */
    public ServerTransactionPolicy(ServerTransactionPolicyConfig config) {
        this.config = config;
    }

    @Override
    public int policy_type() {
        return ServerTransactionPolicyFactory.POLICY_TYPE;
    }

    @Override
    public Policy copy() {
        return new ServerTransactionPolicy(config);
    }

    @Override
    public void destroy() {
        // No resources to clean up
    }

    /**
     * Gets the policy configuration.
     * 
     * @return the configuration, never null
     */
    public ServerTransactionPolicyConfig getConfig() {
        return config;
    }
    
    @Override
    public String toString() {
        return "ServerTransactionPolicy[" + config + "]";
    }
}

// Made with Bob
