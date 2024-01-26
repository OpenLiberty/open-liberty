/*******************************************************************************
 * Copyright (c) 2009,2024 IBM Corporation and others.
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

package com.ibm.ws.config.admin.internal;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil;
import com.ibm.wsspi.kernel.service.utils.OnErrorUtil.OnError;

//@formatter:off
/**
 * Configuration admin service bundle activator.
 */
public class WSConfigAdminActivator implements BundleActivator {
    private static final TraceComponent tc =
                    Tr.register(WSConfigAdminActivator.class,
                                ConfigAdminConstants.TR_GROUP, ConfigAdminConstants.NLS_PROPS);

    private ServiceTracker<WsLocationAdmin, WsLocationAdmin> locationTracker;

    private ServiceTracker<VariableRegistry, VariableRegistry> variableRegistryTracker;

    private ConfigAdminServiceFactory configAdminServiceFactory;

    private SystemConfigSupportImpl systemConfigSupport;

    /**
     * Start the admin service.
     *
     * Register services and allocate resources.
     *
     * Must complete in a timely manner.
     *
     * If an error occurs, shut down services and release resource.  The
     * service will remain stopped.
     *
     * @param bc Bundle context for starting the admin service.
     */
    @Override
    public void start(BundleContext bc) {
        try {
            locationTracker = new ServiceTracker<WsLocationAdmin, WsLocationAdmin>(bc, WsLocationAdmin.class.getName(), null);
            locationTracker.open();

            variableRegistryTracker = new ServiceTracker<VariableRegistry, VariableRegistry>(bc, VariableRegistry.class.getName(), null);
            variableRegistryTracker.open();

            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                Tr.debug(tc, "WSConfigAdminActivator.start():  On config error = " + setOnError());
            }

            configAdminServiceFactory = new ConfigAdminServiceFactory(bc);
            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                Tr.debug(tc, "WSConfigAdminActivator.start():  ConfigurationAdmin registered as a service.");
            }

            systemConfigSupport = new SystemConfigSupportImpl(bc, configAdminServiceFactory);

        } catch ( Exception e ) {
            quit(bc, e);
        }
    }

    @Override
    public void stop(BundleContext context) {
        if ( systemConfigSupport != null ) {
            systemConfigSupport.stop();
            systemConfigSupport = null;
        }

        if ( configAdminServiceFactory != null ) {
            this.configAdminServiceFactory.closeServices();
            this.configAdminServiceFactory = null;
        }

        if ( variableRegistryTracker != null ) {
            variableRegistryTracker.close();
            variableRegistryTracker = null;
        }

        if ( locationTracker != null ) {
            locationTracker.close();
            locationTracker = null;
        }

        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, "WSConfigAdminActivator.stop():  ConfigurationAdmin bundle stopped.");
        }
    }

    @FFDCIgnore(Exception.class)
    @SuppressWarnings("unused")
    private void quit(BundleContext bundleContext, Exception cause) {
        Tr.audit(tc, "frameworkShutdown", locationTracker.getService().getServerName());

        try {
            Bundle bundle = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
            if ( bundle != null ) {
                bundle.stop();
            }

        } catch ( Exception e ) {
            // FFDCIgnore

            // An exception can happen here when bundle context is bad
            // or when system bundle is already stopping.  This is not
            // an exceptional condition, as we want to shutdown anyway.
        }
    }

    private OnError setOnError() {
        VariableRegistry variableRegistry = variableRegistryTracker.getService();

        String onErrorVar = "${" + OnErrorUtil.CFG_KEY_ON_ERROR + "}";
        String onErrorVal = variableRegistry.resolveString(onErrorVar);

        OnError onError;
        if ( onErrorVal.equals(onErrorVar) ) {
            onError = OnErrorUtil.OnError.WARN; // Default

        } else {
            String onErrorFormatted = onErrorVal.trim().toUpperCase();
            try {
                onError = Enum.valueOf(OnErrorUtil.OnError.class, onErrorFormatted);
                // Correct the variable registry with a validated entry if needed
                if ( !onErrorVal.equals(onErrorFormatted) ) {
                    variableRegistry.replaceVariable(OnErrorUtil.CFG_KEY_ON_ERROR, onErrorFormatted);
                }
            } catch ( IllegalArgumentException err ) {
                Tr.warning(tc, "warn.config.invalid.value",
                               OnErrorUtil.CFG_KEY_ON_ERROR, onErrorVal, OnErrorUtil.CFG_VALID_OPTIONS);

                onError = OnErrorUtil.OnError.WARN; // Default
                variableRegistry.replaceVariable(OnErrorUtil.CFG_KEY_ON_ERROR, OnErrorUtil.OnError.WARN.toString());
            }
        }

        return onError;
    }
}
//@formatter:on