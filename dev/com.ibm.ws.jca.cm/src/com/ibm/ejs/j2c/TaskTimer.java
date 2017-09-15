/*******************************************************************************
 * Copyright (c) 2001, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This is a utility class used by the PoolManager.
 */
public final class TaskTimer extends Thread { 
    private PoolManager pm = null;
    private static final TraceComponent tc = Tr.register(TaskTimer.class, J2CConstants.traceSpec, J2CConstants.messageFile); 

    /**
     * Create a new TaskTimer.
     */

    protected TaskTimer(PoolManager value) {
        super();
        pm = value;
        final TaskTimer finalThis = this; 
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                finalThis.setDaemon(true);
                return null;
            }
        }); 

        start();
    }

    /**
     * This method is the implementation of Thread.run().
     */

    @Override
    public void run() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) { 
            Tr.entry(tc, "run");
        }
        pm.executeTask();
        pm.reaperThreadStarted = false;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) { 
            Tr.exit(tc, "run");
        }

    }
}