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

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * There are two types of event listeners the feature manager uses: {@link RefreshBundlesListener} and {@link StartLevelFrameworkListener}.
 * Both of these listeners will wait (on a thread created from a pool for the purpose) for a condition to
 * be satisfied before feature processing can continue.
 * 
 * This provides a common shutdown hook for both listener types to give
 * the best chance for a clean shutdown: it will notify any registered listener
 * that the VM is shutting down. The {@link RefreshBundlesListener} and {@link StartLevelFrameworkListener} can
 * then use this notification to unblock/wrap pending events.
 * 
 * For a long running server, this wouldn't matter: during development, it does happen that the server
 * is stopped before it has finished starting. This class tries to provide some common function
 * to simplify the two event listeners.
 */
public class ShutdownHookManager extends Thread {

    static interface ShutdownHookListener {
        /**
         * The ShutdownHookManager will call registered listeners if/when the shutdown hook
         * is run. The called method should abide by the guidelines written for shutdown hooks:
         * code defensively, no deadlocks etc.
         * 
         * @see {@link Runtime#addShutdownHook(Thread)}
         */
        void shutdownHookInvoked();
    }

    protected final CopyOnWriteArraySet<ShutdownHookListener> shutdownListeners = new CopyOnWriteArraySet<ShutdownHookListener>();
    private final AtomicBoolean hookSet = new AtomicBoolean(false);

    private volatile boolean shutdownInvoked = false;

    /**
     * Add the shutdown hook to the VM (FeatureManager.activate)
     * 
     * @see {@link Runtime#addShutdownHook(Thread)}
     */
    @FFDCIgnore(IllegalStateException.class)
    public void addShutdownHook() {
        try {
            if (hookSet.compareAndSet(false, true)) {
                Runtime.getRuntime().addShutdownHook(this);
            }
        } catch (IllegalStateException e) {
            // ok. based on timing, if we start shutting down, we may
            // not be able to remove the hook.
        }
    }

    /**
     * Remove the shutdown hook from the VM (FeatureManager.deactivate)
     * 
     * @see {@link Runtime#addShutdownHook(Thread)}
     * @see {@link Runtime#removeShutdownHook(Thread)}
     */
    @FFDCIgnore(IllegalStateException.class)
    public synchronized void removeShutdownHook() {
        if (shutdownInvoked)
            return;

        try {
            if (hookSet.compareAndSet(true, false)) {
                Runtime.getRuntime().removeShutdownHook(this);
            }
        } catch (IllegalStateException e) {
            // ok. based on timing, if we start shutting down, we may
            // not be able to remove the hook.
        }
    }

    /**
     * Add a listener, provided the shutdown hook hasn't already been called.
     * 
     * @param listener ShutdownHookListener to add
     */
    public void addListener(ShutdownHookListener listener) {
        if (shutdownInvoked) {
            listener.shutdownHookInvoked();
            return;
        }

        shutdownListeners.add(listener);
    }

    /**
     * Remove a listener that does not need to be notified any more.
     * 
     * @param listener ShutdownHookListener to remove
     */
    public void removeListener(ShutdownHookListener listener) {
        shutdownListeners.remove(listener);
    }

    public boolean vmShutdown() {
        return shutdownInvoked;
    }

    /**
     * The run() method of a shutdown hook is called when a JVM is shut down cleanly.
     * 
     * @see {@link Runtime#addShutdownHook(Thread)}
     */
    @Override
    public void run() {
        shutdownInvoked = true;

        // each listener will have to know/track it's state to be as sure as 
        // possible that the VM will not hang on stop if this is called
        for (ShutdownHookListener listener : shutdownListeners) {
            listener.shutdownHookInvoked();
        }
    }

}
