/*******************************************************************************
 * Copyright (c) 2010, 2025 IBM Corporation and others.
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
package com.ibm.ws.kernel.service.location.internal;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Hashtable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.condition.Condition;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.pseudo.internal.PseudoContextFactory;
import com.ibm.ws.kernel.service.util.JavaInfo;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

import io.openliberty.checkpoint.spi.CheckpointHook;

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

    private ServiceRegistration<WsLocationAdmin> wsLocationAdminRegistration;

    private ServiceRegistration<VariableRegistry> variableRegistryRegistration;

    private ServiceRegistration<CheckpointHook> checkpointHookRegistration;

    @Override
    @FFDCIgnore(IllegalStateException.class)
    public void start(BundleContext context) throws Exception {
        this.context = context;
        FrameworkState.isValid();
        try {
            WsLocationAdminImpl locServiceImpl = WsLocationAdminImpl.createLocations(context.getBundle(0).getBundleContext());
            wsLocationAdminRegistration = context.registerService(WsLocationAdmin.class, locServiceImpl, locServiceImpl.getServiceProps());
            VariableRegistryHelper variableRegistry = new VariableRegistryHelper();
            variableRegistryRegistration = context.registerService(VariableRegistry.class, variableRegistry, null);
            // Service ranking of checkpointHookRegistration needs to be less than com.ibm.ws.config.xml.internal.SystemConfiguration.checkpointHookRegistration.
            // This is important in order to maintain the order of running the hooks.
            checkpointHookRegistration = context.registerService(CheckpointHook.class, locServiceImpl,
                                                                 FrameworkUtil.asDictionary(Collections.singletonMap(Constants.SERVICE_RANKING, 100)));

            Hashtable<String, Object> javaConditionProps = new Hashtable<>();
            javaConditionProps.put(JavaInfo.CONDITION_ID, JavaInfo.majorVersion());
            javaConditionProps.put(Condition.CONDITION_ID, JavaInfo.CONDITION_ID);
            context.registerService(Condition.class, Condition.INSTANCE, javaConditionProps);

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

        // unregister service registrations
        if (wsLocationAdminRegistration != null) {
            wsLocationAdminRegistration.unregister();
        }
        if (variableRegistryRegistration != null) {
            variableRegistryRegistration.unregister();
        }
        if (checkpointHookRegistration != null) {
            checkpointHookRegistration.unregister();
        }

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
            if (bundle != null) {
                CountDownLatch stopping = new CountDownLatch(1);
                    SynchronousBundleListener l = new SynchronousBundleListener() {
                    @Override
                    public void bundleChanged(BundleEvent e) {
                        if (BundleEvent.STOPPING == e.getType() && e.getBundle().getBundleId() == 0) {
                            stopping.countDown();
                        }
                    }
                };
                context.addBundleListener(l);
                bundle.stop();
                stopping.await(1000, TimeUnit.MILLISECONDS);
                // no need to remove listener since we are stopping anyway
            }
        } catch (Exception e) {
            // Exception could happen here if bundle context is bad, or system bundle
            // is already stopping: not an exceptional condition, as we
            // want to shutdown anyway.
        }
    }
}
