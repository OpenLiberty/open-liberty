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

import java.util.Vector;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.j2c.MCWrapper;

/**
 * This is a utility class used by the PoolManager.
 */

public final class ThreadSupportedCleanupAndDestroy implements Runnable {

    private final FreePool _fp;
    private final MCWrapper _mcWrapper;
    private final static boolean mcWrapperDoesNotExistInFreePool = false;
    private final static boolean synchronizeInMethod = true;
    private final static boolean notifyWaiter = false;
    private final static boolean decrementTotalCounter = false;
    private Vector<ThreadSupportedCleanupAndDestroy> _tscdList = null;

    private static final TraceComponent tc = Tr.register(ThreadSupportedCleanupAndDestroy.class, J2CConstants.traceSpec, J2CConstants.messageFile);

    protected ThreadSupportedCleanupAndDestroy(Vector<ThreadSupportedCleanupAndDestroy> tscdList, FreePool fp, MCWrapper mcWrapper) {
        super();

        _fp = fp;
        _mcWrapper = mcWrapper;
        _tscdList = tscdList;
    }

    @Override
    public void run() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) { 
            Tr.entry(tc, "run");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { 
            Tr.debug(tc, "Running thread supported cleanup and destroy on MCWrapper " + _mcWrapper.toString());
        }
        long startTime = 0;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { 
            startTime = System.currentTimeMillis();
        }

        _fp.cleanupAndDestroyMCWrapper(_mcWrapper); 

        _fp.removeMCWrapperFromList(_mcWrapper, mcWrapperDoesNotExistInFreePool, synchronizeInMethod, notifyWaiter, decrementTotalCounter);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { 
            long endTime = System.currentTimeMillis();
            long diff = endTime - startTime;
            long timeToCleanupAndDestroy = 0;
            if (diff > 0) {
                timeToCleanupAndDestroy = diff / 1000;
            }
            Tr.debug(tc, "Finished processing the thread supported cleanup and destroy of a connection in " +
                         timeToCleanupAndDestroy + " seconds");
        }
        /*
         * The thread is has completed its work, remove it from the list.
         */
        _tscdList.remove(this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) { 
            Tr.exit(tc, "run");
        }
    }

    @Override
    public String toString() {
        return _mcWrapper.toString();
    }
}