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

/**
 * Activator for the WebSphere Application Server Kernel bundle.
 *
 * This class loads other core bundles and ensures that core services
 * (like the ConfigAdminService) are started. Those services will continue
 * initialization of the system based on provided configuration data.
 */
public class WSConfigAdminActivator implements BundleActivator {

    private static final TraceComponent tc = Tr.register(WSConfigAdminActivator.class, ConfigAdminConstants.TR_GROUP, ConfigAdminConstants.NLS_PROPS);

    /** Factory to create ConfigurationAdmin service */
    private ConfigAdminServiceFactory configAdminServiceFactory = null;

    /** Creates the system configuration */
    private SystemConfigSupportImpl systemConfigSupport = null;

    /** Tracker for standard runtime handling of the Location admin service */
    private ServiceTracker<WsLocationAdmin, WsLocationAdmin> locationTracker = null;

    /** Tracker for variable registry service */
    private ServiceTracker<VariableRegistry, VariableRegistry> variableRegistryTracker = null;

    /**
     * Called when bundle is started: can register services, allocate resources,
     * etc.
     * Must complete and return to caller in a timely manner.
     *
     * If method throws an exception, bundle will be marked as stopped,
     * and the framework will remove bundle's listeners, unregister bundle's
     * services,
     * etc.
     *
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext bc) {
        try {
            locationTracker = new ServiceTracker<WsLocationAdmin, WsLocationAdmin>(bc, WsLocationAdmin.class.getName(), null);
            locationTracker.open();

            variableRegistryTracker = new ServiceTracker<VariableRegistry, VariableRegistry>(bc, VariableRegistry.class.getName(), null);
            variableRegistryTracker.open();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "WSConfigActivator.start():  On config error = " + this.getOnError());
            }

            // create ConfigurationAdmin service
            configAdminServiceFactory = new ConfigAdminServiceFactory(bc);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "WSConfigActivator.start():  ConfigurationAdmin registered as a service.");
            }

            systemConfigSupport = new SystemConfigSupportImpl(bc, configAdminServiceFactory);

        } catch (Exception e) {
            // this is something fatal/unexpected an usually means we can't process config
            quit(bc, e);
        }
    }

    /**
     * Called when bundle is stopped. Should undo what the start method did.
     *
     * A stopped bundle must not call any framework objects. There should be no
     * active threads started by this bundle remaining when this method returns.
     *
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) {

        if (systemConfigSupport != null) {
            systemConfigSupport.stop();
            systemConfigSupport = null;
        }

        if (configAdminServiceFactory != null) {
            this.configAdminServiceFactory.closeServices();
            this.configAdminServiceFactory = null;
        }

        if (null != locationTracker) {
            locationTracker.close();
            locationTracker = null;
        }

        if (null != variableRegistryTracker) {
            variableRegistryTracker.close();
            variableRegistryTracker = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "WSConfigActivator.stop():  ConfigurationAdmin bundle stopped.");
    }

    @FFDCIgnore(Exception.class)
    private void quit(BundleContext bundleContext, Exception cause) {
        Tr.audit(tc, "frameworkShutdown", locationTracker.getService().getServerName());

        try {
            Bundle bundle = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
            if (bundle != null)
                bundle.stop();
        } catch (Exception e) {
            // Exception could happen here if bundle context is bad, or system bundle
            // is already stopping: not an exceptional condition, as we
            // want to shutdown anyway.
        }
    }

    /**
     * @return
     */
    private OnError getOnError() {

        VariableRegistry variableRegistry = variableRegistryTracker.getService();

        OnError onError;
        String onErrorVar = "${" + OnErrorUtil.CFG_KEY_ON_ERROR + "}";
        String onErrorVal = variableRegistry.resolveString(onErrorVar);

        if ((onErrorVal.equals(onErrorVar))) {
            onError = OnErrorUtil.OnError.WARN; // Default value if not set
        } else {
            String onErrorFormatted = onErrorVal.trim().toUpperCase();
            try {
                onError = Enum.valueOf(OnErrorUtil.OnError.class, onErrorFormatted);
                // Correct the variable registry with a validated entry if needed
                if (!onErrorVal.equals(onErrorFormatted))
                    variableRegistry.replaceVariable(OnErrorUtil.CFG_KEY_ON_ERROR, onErrorFormatted);
            } catch (IllegalArgumentException err) {
                if (tc.isWarningEnabled()) {
                    Tr.warning(tc, "warn.config.invalid.value", OnErrorUtil.CFG_KEY_ON_ERROR, onErrorVal, OnErrorUtil.CFG_VALID_OPTIONS);
                }
                onError = OnErrorUtil.OnError.WARN; // Default value if error
                variableRegistry.replaceVariable(OnErrorUtil.CFG_KEY_ON_ERROR, OnErrorUtil.OnError.WARN.toString());
            }
        }
        return onError;
    }
}
