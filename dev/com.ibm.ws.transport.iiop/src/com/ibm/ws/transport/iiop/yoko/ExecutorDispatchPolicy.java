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
package com.ibm.ws.transport.iiop.yoko;

import org.apache.yoko.orb.OB.DispatchStrategy;
import org.apache.yoko.orb.OBPortableServer.DISPATCH_STRATEGY_POLICY_ID;
import org.apache.yoko.orb.OBPortableServer.DispatchStrategyPolicy;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.Policy;

/**
 *
 */
public class ExecutorDispatchPolicy extends LocalObject implements DispatchStrategyPolicy {

    /**  */
    private static final long serialVersionUID = 1L;
    private final transient DispatchStrategy dispatchStrategy;

    /**
     * @param dispatchStrategy
     */
    public ExecutorDispatchPolicy(DispatchStrategy dispatchStrategy) {
        this.dispatchStrategy = dispatchStrategy;
    }

    /** {@inheritDoc} */
    @Override
    public int policy_type() {
        return DISPATCH_STRATEGY_POLICY_ID.value;
    }

    /** {@inheritDoc} */
    @Override
    public Policy copy() {
        return new ExecutorDispatchPolicy(dispatchStrategy);
    }

    /** {@inheritDoc} */
    @Override
    public void destroy() {}

    /** {@inheritDoc} */
    @Override
    public DispatchStrategy value() {
        return dispatchStrategy;
    }

}
