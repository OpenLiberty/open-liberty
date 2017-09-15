/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.launch.internal;

import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class StartLevelFrameworkListener implements FrameworkListener {
    BundleStartStatus status = null;

    private volatile boolean levelReached = false;
    private volatile boolean hookSet = false;

    private final Thread shutdownHook = new Thread()
    {
        @Override
        public void run()
        {
            levelReached(true);
        }
    };

    public StartLevelFrameworkListener(boolean trackExceptions) {
        if (trackExceptions)
            status = new BundleStartStatus();
    }

    /**
     * Listen to framework events until we get a STARTLEVEL_CHANGED event, at
     * which time we stop listening for framework events, and notify the waiting
     * thread that a startlevel operation has finished (it could, in theory, be
     * someone else's startlevel operation...).
     */
    @Override
    public void frameworkEvent(FrameworkEvent event) {
        switch (event.getType()) {
            case FrameworkEvent.ERROR:
                if (status != null)
                    status.addStartException(event.getBundle(), event.getThrowable());
                break;
            // Wake up the listener if the framework is stopped,
            // too...
            case FrameworkEvent.STOPPED:
            case FrameworkEvent.STOPPED_UPDATE:
            case FrameworkEvent.STARTLEVEL_CHANGED:
                levelReached(false);
                break;
            default:
                break;
        }
    }

    /**
     * Attach a shutdown hook to make sure that the thread waiting for the start
     * level to change is posted if the JVM is shut down
     */
    public synchronized void waitForLevel() {
        while (!levelReached) {
            if (hookSet == false) {
                Runtime.getRuntime().addShutdownHook(shutdownHook);
                hookSet = true;
            }

            try {
                wait();
            } catch (InterruptedException e) {
                /** No-op **/
            }
        }
    }

    @FFDCIgnore(IllegalStateException.class)
    protected synchronized void levelReached(boolean inShutdownHook) {
        levelReached = true;

        if (inShutdownHook)
            status.markContextInvalid();

        notify();

        if (hookSet && !inShutdownHook) {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook);
                hookSet = false;
            } catch (IllegalStateException e) {
                // ok. based on timing, if we start shutting down, we may
                // not be able to remove the hook.
            }
        }
    }

    public BundleStartStatus getStatus() {
        return status;
    }
}