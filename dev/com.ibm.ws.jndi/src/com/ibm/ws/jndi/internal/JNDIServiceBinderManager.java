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
package com.ibm.ws.jndi.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 *
 */
public class JNDIServiceBinderManager implements BundleActivator {

    /** {@inheritDoc} */
    @Override
    public void start(BundleContext bundleContext) throws Exception {
        //We don't initialize the JNDIServiceBinder here because we want to be as lazy as possible.
        //It will be initialized on first use of the JNDIServiceBinderHolder
    }

    /** {@inheritDoc} */
    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        //clean up the JNDIServiceBinder when the bundle is shutdown
        if (serviceBinderCreated.get()) {
            JNDIServiceBinderHolder.HELPER.deactivate(bundleContext);
        }
    }

    private static final AtomicBoolean serviceBinderCreated = new AtomicBoolean();

    static final class JNDIServiceBinderHolder {
        static final JNDIServiceBinder HELPER = createAndInitializeServiceBinder();
        static {
            serviceBinderCreated.set(true);
        }

        private static JNDIServiceBinder createAndInitializeServiceBinder() {
            final JNDIServiceBinder helper = new JNDIServiceBinder();
            final Bundle b = FrameworkUtil.getBundle(JNDIServiceBinderManager.class);
            if (System.getSecurityManager() == null)
                helper.activate(b.getBundleContext());
            else
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        helper.activate(b.getBundleContext());
                        return null;
                    }
                });
            return helper;
        }

    }

}
