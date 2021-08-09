/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.jmx.internal;

import java.util.concurrent.ConcurrentLinkedQueue;

import javax.management.MBeanServer;
import javax.management.MBeanServerBuilder;
import javax.management.MBeanServerDelegate;

public final class PlatformMBeanServerBuilder extends MBeanServerBuilder {

    private static final ConcurrentLinkedQueue<PlatformMBeanServerBuilderListener> listenerQueue =
                    new ConcurrentLinkedQueue<PlatformMBeanServerBuilderListener>();
    private static volatile PlatformMBeanServer platformMBeanServer;
    private static volatile boolean loading = false;

    public PlatformMBeanServerBuilder() {}

    //
    // MBeanServerBuilder methods
    //

    @Override
    public MBeanServer newMBeanServer(String defaultDomain, MBeanServer outer,
                                      MBeanServerDelegate delegate) {

        if (loading && isCreatingPlatformMBeanServer(2)) {
            return platformMBeanServer;
        }
        MBeanServer mbs = super.newMBeanServer(defaultDomain, outer, delegate);
        PlatformMBeanServer pmbs = new PlatformMBeanServer(mbs, (PlatformMBeanServerDelegate) delegate);
        //Only one thread can ever create the platform mbean server.  If loading is true, then either this thread has already created 
        //the platform mbean server and this is a recursive call; if the request is for the platform mbean server we have already returned the cached value, 
        //and otherwise this is not a request for the platform mbean server.  Any other thread is not a request for the platform mbean server.
        if (!loading &&
            platformMBeanServer == null &&
            isCreatingPlatformMBeanServer(1)) {
            platformMBeanServer = pmbs;
            loading = true;
            notifyListeners();//this may result in a recursive call to ManagementFactory.getPlatformMBeanServer
            loading = false;
        }
        return pmbs;
    }

    @Override
    public MBeanServerDelegate newMBeanServerDelegate() {
        return new PlatformMBeanServerDelegate();
    }

    public static void addPlatformMBeanServerBuilderListener(PlatformMBeanServerBuilderListener listener) {
        final PlatformMBeanServer mbs = platformMBeanServer;
        if (mbs != null) {
            mbs.invokePlatformMBeanServerCreated(listener);
        } else {
            listenerQueue.add(listener);
            // Checking the flag again since it might have changed.
            if (platformMBeanServer != null) {
                notifyListeners();
            }
        }
    }

    /**
     * Returns true if at least 'threshold' of the ancestor calls on the stack are
     * java.lang.management.ManagementFactory.getPlatformMBeanServer().
     */
    private static boolean isCreatingPlatformMBeanServer(int threshold) {
        int count = 0;
        for (final StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            if ("java.lang.management.ManagementFactory".equals(ste.getClassName())
                && "getPlatformMBeanServer".equals(ste.getMethodName())) {
                count++;
                if (count >= threshold) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void notifyListeners() {
        final PlatformMBeanServer mbs = platformMBeanServer;
        PlatformMBeanServerBuilderListener listener = listenerQueue.poll();
        while (listener != null) {
            mbs.invokePlatformMBeanServerCreated(listener);
            listener = listenerQueue.poll();
        }
    }
}
