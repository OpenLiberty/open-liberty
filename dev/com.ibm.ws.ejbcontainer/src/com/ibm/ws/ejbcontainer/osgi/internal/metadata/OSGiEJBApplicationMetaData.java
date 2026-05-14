/*******************************************************************************
 * Copyright (c) 2013, 2026 IBM Corporation and others.
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
package com.ibm.ws.ejbcontainer.osgi.internal.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import com.ibm.ejs.container.EJSContainer;
import com.ibm.ejs.container.EJSHome;
import com.ibm.ejs.csi.EJBApplicationMetaData;
import com.ibm.ejs.csi.EJBModuleMetaDataImpl;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.productinfo.ProductInfo;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.wsspi.kernel.service.utils.ServerQuiesceListener;

/**
 * Extend EJBApplicationMetaData to gain access to the set of modules running
 * in this application.
 *
 */
public class OSGiEJBApplicationMetaData extends EJBApplicationMetaData implements ServerQuiesceListener {
    private static final TraceComponent tc = Tr.register(OSGiEJBApplicationMetaData.class);
    private static final boolean isBeta = ProductInfo.getBetaEdition();

    public OSGiEJBApplicationMetaData(EJSContainer container, String name, String logicalName, boolean standaloneModule, ApplicationMetaData amd, boolean started,
                                      boolean blockWorkUntilStarted) {
        super(container, name, logicalName, standaloneModule, amd, started, blockWorkUntilStarted);
    }

    private final Map<String, EJBModuleMetaDataImpl> modulesMap = new HashMap<String, EJBModuleMetaDataImpl>();

    /**
     * OSGi service registration for this instance as a ServerQuiesceListener.
     * This registration is created when the first startup singleton with the quiesce
     * property is initialized, allowing the application to receive server shutdown
     * notifications and properly destroy quiesce-enabled singletons in reverse order.
     * The registration is cleaned up when the application stops or during server quiesce.
     */
    private ServiceRegistration<ServerQuiesceListener> quiesceRegistration = null;

    /**
     * List of startup singletons to destroy during quiesce, in the order that they were initialized.
     * A Set is used to ensure that a singleton is only added once even if errors occur and initialization
     * is attempted multiple times. A LinkedHashSet is used to ensure stable ordering.
     *
     * <p>
     * This list is null until initialization is attempted for a quiesce singleton.
     */
    private LinkedHashSet<EJSHome> quiesceSingletons;

    @Override
    public synchronized void startingModule(EJBModuleMetaDataImpl mmd, boolean blockWorkUntilStarted) {
        super.startingModule(mmd, blockWorkUntilStarted);
        modulesMap.put(mmd.getName(), mmd);
    }

    @Override
    public void stoppingModule(EJBModuleMetaDataImpl mmd) {
        modulesMap.remove(mmd.getName());
        super.stoppingModule(mmd);
    }

    public EJBModuleMetaDataImpl getModuleMetaData(String name) {
        return modulesMap.get(name);
    }

    /**
     * Adds a startup singleton to the initialization list and registers for server quiesce
     * if the singleton is configured with the quiesce property.
     *
     * @param home the EJB home for the singleton bean being initialized
     */
    @Override
    public synchronized void addSingletonInitialization(EJSHome home) {
        super.addSingletonInitialization(home);

        if (isBeta && home.getBeanMetaData().isDestroyOnQuiesce()) {

            // Create list of singleton to destroy during server quiesce and register for server quiesce
            if (quiesceSingletons == null) {
                quiesceSingletons = new LinkedHashSet<EJSHome>();

                // Register this instance as a ServerQuiesceListener service
                BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
                quiesceRegistration = bundleContext.registerService(ServerQuiesceListener.class, this,
                                                                    FrameworkUtil.asDictionary(Collections.singletonMap("quiesce.phase", "PRE")));
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "registered quiesce listener : " + this);
            }

            // Add to set of beans to be destroyed during server quiesce
            quiesceSingletons.add(home);
        }
    }

    /**
     * Called when the application is stopping. Unregisters the quiesce listener service
     * if it was registered.
     */
    @Override
    public void stopping() {
        super.stopping();

        // Unregister the quiesce listener service
        if (quiesceRegistration != null) {
            quiesceRegistration.unregister();
            quiesceRegistration = null;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "unregistered quiesce listener : " + this);
        }
    }

    /**
     * ServerQuiesceListener callback invoked during server shutdown.
     * Destroys all quiesce-enabled startup singletons in reverse initialization order,
     * then unregisters the quiesce listener service.
     */
    @Override
    public void serverStopping() {
        if (quiesceSingletons != null) {
            List<EJSHome> reverse = new ArrayList<EJSHome>(quiesceSingletons);
            for (int i = reverse.size(); --i >= 0;) {
                EJSHome home = reverse.get(i);
                home.destroy();
                quiesceSingletons.remove(home);
            }
        }

        // Unregister the quiesce listener service
        if (quiesceRegistration != null) {
            quiesceRegistration.unregister();
            quiesceRegistration = null;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "unregistered quiesce listener : " + this);
        }
    }

}
