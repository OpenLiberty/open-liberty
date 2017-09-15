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

import java.util.concurrent.Executor;

import org.apache.yoko.orb.OB.DispatchRequest;
import org.apache.yoko.orb.OB.DispatchStrategy;
import org.omg.CORBA.Any;
import org.omg.CORBA.LocalObject;

/**
 *
 */
public class ExecutorDispatchStrategy extends LocalObject implements DispatchStrategy {

    private final Executor executor;

    /**
     * @param executor
     */
    public ExecutorDispatchStrategy(Executor executor) {
        this.executor = executor;
    }

    /** {@inheritDoc} */
    @Override
    public void dispatch(final DispatchRequest req) {

        executor.execute(new Runnable() {

            @Override
            public void run() {
                req.invoke();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public int id() {
        return 4;
    }

    /** {@inheritDoc} */
    @Override
    public Any info() {
        return new org.apache.yoko.orb.CORBA.Any();
    }

}
