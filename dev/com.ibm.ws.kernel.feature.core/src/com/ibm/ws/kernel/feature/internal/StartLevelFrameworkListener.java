/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal;

import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

public class StartLevelFrameworkListener implements FrameworkListener, ShutdownHookManager.ShutdownHookListener {
    private final ShutdownHookManager shutdownHook;
    private final BundleLifecycleStatus status;
    private final AtomicBoolean levelReached = new AtomicBoolean(false);

    public StartLevelFrameworkListener(ShutdownHookManager shutdownHook) {
        this.shutdownHook = shutdownHook;
        this.status = new BundleLifecycleStatus();
        shutdownHook.addListener(this);
    }

    /**
     * Called by (our) shutdown hook when the JVM is shut down;
     * Make sure any waiting threads are notified
     */
    @Override
    public void shutdownHookInvoked() {
        levelReached(true);
    }

    /**
     * Listen to framework events until we get a STARTLEVEL_CHANGED
     * event, at which time we stop listening for framework events,
     * and notify the waiting thread that a startlevel operation
     * has finished (it could, in theory, be someone else's startlevel
     * operation...).
     */
    @Override
    public void frameworkEvent(FrameworkEvent event) {
        switch (event.getType()) {
            case FrameworkEvent.ERROR:
                status.addStartException(event.getBundle(), event.getThrowable());
                break;

            // Wake up the listener if a startlevel changed event occurs, 
            // or the framework is stopped...
            case FrameworkEvent.STOPPED:
            case FrameworkEvent.STOPPED_UPDATE:
            case FrameworkEvent.STOPPED_BOOTCLASSPATH_MODIFIED:
            case FrameworkEvent.STARTLEVEL_CHANGED:
                levelReached(false);
                break;
            default:
                break;
        }
    }

    /**
     * Attach a shutdown hook to make sure that the thread waiting for
     * the start level to change is posted if the JVM is shut down
     */
    @FFDCIgnore({ InterruptedException.class })
    public synchronized void waitForLevel() {
        // Don't bother waiting if the vm has been shutdown in the meanwhile...
        while (levelReached.get() == false && !shutdownHook.vmShutdown()) {
            try {
                // waiting for a start level event should not take long
                wait(1000);
            } catch (InterruptedException e) {
                /** No-op **/
            }
        }
    }

    protected void levelReached(boolean inShutdownHook) {
        // First thread to set to true cleans up
        if (levelReached.compareAndSet(false, true)) {
            if (inShutdownHook) {
                status.markContextInvalid();
            } else {
                shutdownHook.removeListener(this);
            }
        }

        // Make sure no one is waiting
        synchronized (this) {
            notifyAll();
        }
    }

    public BundleLifecycleStatus getStatus() {
        return status;
    }
}