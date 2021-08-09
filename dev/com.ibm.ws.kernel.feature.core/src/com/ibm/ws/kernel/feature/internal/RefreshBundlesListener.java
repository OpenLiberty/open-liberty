/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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

public class RefreshBundlesListener implements FrameworkListener, ShutdownHookManager.ShutdownHookListener {
    private final ShutdownHookManager shutdownHook;
    private final BundleLifecycleStatus status;
    private final AtomicBoolean done = new AtomicBoolean(false);

    public RefreshBundlesListener(ShutdownHookManager shutdownHook) {
        this.shutdownHook = shutdownHook;
        this.status = new BundleLifecycleStatus();
        shutdownHook.addListener(this);
    }

    /** {@inheritDoc} */
    @Override
    public void shutdownHookInvoked() {
        finish(true);
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

            // Wake up the listener when packages refreshed event occurs, 
            // or the framework is stopped...
            case FrameworkEvent.STOPPED:
            case FrameworkEvent.STOPPED_UPDATE:
            case FrameworkEvent.PACKAGES_REFRESHED:
                finish(false);
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
    public synchronized void waitForComplete() {
        // Don't bother waiting if the vm has been shutdown in the meanwhile...
        while (done.get() == false && !shutdownHook.vmShutdown()) {
            try {
                // refresh packages should not take a long time..
                wait(1000);
            } catch (InterruptedException e) {
                /** No-op **/
            }
        }
    }

    protected void finish(boolean inShutdownHook) {
        // only have to attempt clean up once
        if (done.compareAndSet(false, true)) {
            if (inShutdownHook) {
                status.markContextInvalid();
            } else {
                shutdownHook.removeListener(this);
            }
        }

        // make sure no one is waiting on this condition.. 
        synchronized (this) {
            notifyAll();
        }
    }

    public BundleLifecycleStatus getStatus() {
        return status;
    }
}