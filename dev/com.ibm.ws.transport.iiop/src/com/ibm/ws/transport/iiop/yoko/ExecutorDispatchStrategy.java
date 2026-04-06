/*******************************************************************************
 * Copyright 2015,2026 IBM Corporation and others.
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
package com.ibm.ws.transport.iiop.yoko;

import java.util.concurrent.Executor;

import org.apache.yoko.orb.OB.DispatchRequest;
import org.apache.yoko.orb.OB.DispatchStrategy;
import org.omg.CORBA.Any;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;

/**
 *
 */
public class ExecutorDispatchStrategy extends LocalObject implements DispatchStrategy {
    private final Executor executor;

    public ExecutorDispatchStrategy(Executor executor) { this.executor = executor; }

    @Override
    public void dispatch(final DispatchRequest req) { executor.execute(req::invoke); }

    @Override
    public int id() { return 4; }

    @Override
    public Any info() { return ORB.init().create_any(); }
}
