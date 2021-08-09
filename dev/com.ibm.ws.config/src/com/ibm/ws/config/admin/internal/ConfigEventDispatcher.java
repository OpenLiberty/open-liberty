/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.admin.internal;

import java.util.concurrent.Future;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * Dispatches ConfigurationEvent to registered ConfigurationListeners.
 */
class ConfigEventDispatcher {

    private static final String ME = ConfigEventDispatcher.class.getName();
    private static final TraceComponent tc = Tr.register(ConfigEventDispatcher.class, ConfigAdminConstants.TR_GROUP, ConfigAdminConstants.NLS_PROPS);

    /** Service Tracker for ConfigurationListener */
    private final ServiceTracker<ConfigurationListener, ConfigurationListener> st;

    /** ConfigurationAdmin service reference */
    private ServiceReference<ConfigurationAdmin> configAdminReference;

    /** ConfigurationAdmin service factory */
    private final ConfigAdminServiceFactory caFactory;

    /**
     * Constructor.
     * 
     * @param bc
     *            - bundle context
     * @param sr
     *            - ServiceReference for ConfigurationAdmin
     */
    public ConfigEventDispatcher(ConfigAdminServiceFactory casf, BundleContext bc) {
        caFactory = casf;
        st = new ServiceTracker<ConfigurationListener, ConfigurationListener>(bc, ConfigurationListener.class.getName(), null);
        st.open();
    }

    public void close() {
        st.close();
    }

    synchronized void setServiceReference(ServiceReference<ConfigurationAdmin> reference) {
        if (configAdminReference == null)
            configAdminReference = reference;
    }

    /**
     * Dispatch ConfigurationEvent to the ConfigurationListeners.
     * 
     * @param pid
     *            - Service PID
     * @param factoryPid
     *            - factory PID
     * @param eventType
     *            - ConfigurationEvent type
     */
    protected Future<?> dispatch(final int eventType, final String factoryPid, final String pid) {
        final ConfigurationEvent event = createConfigurationEvent(eventType, factoryPid, pid);
        if (event == null)
            return null;

        final ServiceReference<ConfigurationListener>[] refs = st.getServiceReferences();
        if (refs == null)
            return null;

        final String qPid = (factoryPid != null) ? factoryPid : pid;
        return caFactory.updateQueue.add(qPid, new Runnable() {
            @Override
            @FFDCIgnore(Exception.class)
            public void run() {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(tc, "dispatch: sending configuration listener event for " + qPid);
                }
                for (ServiceReference<ConfigurationListener> sr : refs) {
                    if (sr != null) {
                        ConfigurationListener cl = st.getService(sr);
                        if (cl != null && FrameworkState.isValid()) {
                            try {
                                cl.configurationEvent(event);
                            } catch (Exception e) {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "dispatch(): Exception thrown while trying to dispatch ConfigurationEvent.", e);
                                }
                                FFDCFilter.processException(e, ME,
                                                            "dispatch(): Exception thrown while trying to dispatch ConfigurationEvent.",
                                                            new Object[] { pid, factoryPid, eventType, cl });
                            }
                        }
                    }
                }
            }
        });
    }

    private synchronized ConfigurationEvent createConfigurationEvent(int type, String factoryPid, String pid) {
        if (configAdminReference == null)
            return null;

        return new ConfigurationEvent(configAdminReference, type, factoryPid, pid);
    }

}
