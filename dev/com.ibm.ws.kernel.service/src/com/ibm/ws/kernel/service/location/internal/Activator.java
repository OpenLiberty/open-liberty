/*******************************************************************************
 * Copyright (c) 2010, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.service.location.internal;

import java.lang.reflect.Field;

import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.kernel.pseudo.internal.PseudoContextFactory;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

public class Activator implements BundleActivator {
    private static final TraceComponent tc = Tr.register(Activator.class);

    /** Reference to active BundleContext (will be null between stop and start) */
    protected BundleContext context = null;

    /**
     * The JNDI context factory registered here - will be null if we failed to
     * register it. this can happen if somebody else registers one. Because we
     * must be able to restart the OSGi framework in the same JVM, we must be
     * able to re-set the InitialContextFactoryBuilder, but the Naming API does
     * not allow it - this is why we must use reflection to clear it on
     * shutdown.
     */
    private PseudoContextFactory contextFactory;

    @Override
    @FFDCIgnore(IllegalStateException.class)
    public void start(BundleContext context) throws Exception {
        this.context = context;
        FrameworkState.isValid();
        try {
            WsLocationAdminImpl locServiceImpl = WsLocationAdminImpl.createLocations(context.getBundle(0).getBundleContext());
            context.registerService(WsLocationAdmin.class.getName(), locServiceImpl, locServiceImpl.getServiceProps());
            VariableRegistryHelper variableRegistry = new VariableRegistryHelper();
            context.registerService(VariableRegistry.class.getName(), variableRegistry, null);

            // Assume this is the first place that tries to set this
            try {
                PseudoContextFactory factory = new PseudoContextFactory();
                NamingManager.setInitialContextFactoryBuilder(factory);
                contextFactory = factory;
            } catch (IllegalStateException ex) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Failed to install initialContextFactoryBuilder because it was already installed", ex);
            }
        } catch (Exception t) {
            Tr.audit(tc, "frameworkShutdown");

            // FFDC will catch/trace: don't re-log.. messages already issued when exception is thrown
            // If server is coming down (i.e. registerService fails w/ IllegalStateException), no
            // need to write a message about that to the log either
            shutdownFramework();
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        this.context = null;

        // If we set the InitialContextFactoryBuilder (and it is still set to ours),
        // then we must clear it out.
        if (contextFactory != null) {
            try {
                for (Field field : NamingManager.class.getDeclaredFields()) {
                    if (InitialContextFactoryBuilder.class.equals(field.getType())) {
                        field.setAccessible(true);
                        if (field.get(null) == contextFactory) {

                            field.set(null, null);
                        }
                        break;
                    }
                }
            } catch (Exception ex) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Failed to uninstall initialContextFactoryBuilder", ex);
            }
        }
    }

    /**
     * When an error occurs during startup,
     * then this method is used to stop the root bundle thus bringing down the
     * OSGi framework.
     */
    @FFDCIgnore(Exception.class)
    protected final void shutdownFramework() {
        try {
            Bundle bundle = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
            if (bundle != null)
                bundle.stop();
        } catch (Exception e) {
            // Exception could happen here if bundle context is bad, or system bundle
            // is already stopping: not an exceptional condition, as we
            // want to shutdown anyway.
        }
    }
}