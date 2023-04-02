/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package test.concurrent.sim.context.zos.thread;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.RejectedExecutionException;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.ibm.wsspi.threadcontext.ThreadContext;

/**
 * This a fake thread context that we made up for testing purposes.
 */
@SuppressWarnings("deprecation")
public class ThreadNameContext implements ThreadContext {
    private static final long serialVersionUID = 1L;
    static final String CLEARED = "Unnamed Thread";

    private String threadNameToPropagate;
    private transient String threadNameToRestore;

    public ThreadNameContext(String threadName) {
        threadNameToPropagate = threadName;
    }

    @Override
    public ThreadNameContext clone() {
        return new ThreadNameContext(threadNameToPropagate);
    }

    /**
     * Establishes context on the current thread.
     */
    @Override
    public void taskStarting() throws RejectedExecutionException {
        threadNameToRestore = Thread.currentThread().getName();
        try {
            if (!CLEARED.equals(threadNameToPropagate))
                // Prove that prerequisite APPLICATION context is applied to the thread before propagating this context type
                if (!"1value".equals(InitialContext.doLookup("java:comp/env/1entry"))) {
                    String error = "ThreadContextProvider lacks access to prerequisite context when propagating context to thread.";

                    // Encourage server.stop to detect the error if it doesn't otherwise flow back to the application
                    System.out.println(" E ERROR0001E: " + error);

                    throw new AssertionError(error);
                }

            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                Thread.currentThread().setName(threadNameToPropagate);
                return null;
            });
        } catch (Throwable x) {
            throw new RejectedExecutionException(x);
        }
    }

    /**
     * Restore the thread to its previous state from before the most recently applied context.
     */
    @Override
    public void taskStopping() {
        if (!CLEARED.equals(threadNameToPropagate))
            try {
                // Prove that prerequisite APPLICATION context remains on the thread while context is being removed/restored
                if (!"1value".equals(InitialContext.doLookup("java:comp/env/1entry"))) {
                    String error = "ThreadContextProvider lacks access to prerequisite context when removing/restoring thread context.";

                    // Encourage server.stop to detect the error if it doesn't otherwise flow back to the application
                    System.out.println(" E ERROR0001E: " + error);

                    throw new AssertionError(error);
                }
            } catch (NamingException x) {
                throw new IllegalStateException(x);
            }

        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            Thread.currentThread().setName(threadNameToRestore);
            return null;
        });
    }

    @Override
    public String toString() {
        return "ThreadNameContext propagate [" + threadNameToPropagate + "] restore [" + threadNameToRestore + "]";
    }
}