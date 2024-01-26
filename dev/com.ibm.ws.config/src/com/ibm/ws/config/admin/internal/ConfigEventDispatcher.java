/*******************************************************************************
 * Copyright (c) 2010,2024 IBM Corporation and others.
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

import java.util.concurrent.Future;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

//@formatter:off
/**
 * Helper for creating and sending configuration events.
 *
 * The helper encapsulates the creation of events based on an
 * event type and configuration IDs, and encapsulates querying
 * for event listeners and sending the created event to those
 * listeners.
 *
 * Events are not sent immediately: A request to dispatch an event
 * is scheduled through a queue managed by the configuration admin
 * service factory.  See {@link ConfigAdminServiceFactory#updateQueue}.
 */
class ConfigEventDispatcher {
    private static final TraceComponent tc =
        Tr.register(ConfigEventDispatcher.class,
                    ConfigAdminConstants.TR_GROUP, ConfigAdminConstants.NLS_PROPS);

    private static final String CLASS_NAME = ConfigEventDispatcher.class.getName();

    //

    public ConfigEventDispatcher(ConfigAdminServiceFactory caFactory, BundleContext bundleContext) {
        this.caFactory = caFactory;

        this.listenerTracker = new ServiceTracker<>(bundleContext, ConfigurationListener.class.getName(), null);
        this.listenerTracker.open();
    }

    public void close() {
        listenerTracker.close();
    }

    //

    private final ConfigAdminServiceFactory caFactory;

    @Trivial
    private Future<?> schedule(String eventKey, Runnable eventRunner) {
        return caFactory.updateQueue.add(eventKey, eventRunner);
    }

    //

    private ServiceReference<ConfigurationAdmin> caRef;

    synchronized void setServiceReference(ServiceReference<ConfigurationAdmin> caRef) {
        if ( this.caRef == null ) {
            this.caRef = caRef;
        }
    }

    /**
     * Factory method: Create a configuration event of a specified type and with
     * specified IDs.
     *
     * @param type The type of the event.
     * @param factoryPid The factory ID of the event.
     * @param pid The ID of the event.
     *
     * @return The new configuration event.  Null if the administration service is
     *     not available.
     */
    private synchronized ConfigurationEvent createConfigurationEvent(int type, String factoryPid, String pid) {
        if ( caRef == null ) {
            return null;
        } else {
            return new ConfigurationEvent(caRef, type, factoryPid, pid);
        }
    }

    //

    private final ServiceTracker<ConfigurationListener, ConfigurationListener> listenerTracker;

    /**
     * Schedule the send of configuration events to configuration event listeners.
     *
     * See {@link #handleEvent}.
     *
     * @param eventType The type of event which is to be sent.
     * @param factoryPid The factory PID of the event.
     * @param pid The PID of the event.
     *
     * @return The future for sending the event to selected listeners.
     */
    protected Future<?> dispatch(int eventType, String factoryPid, String pid) {
        ConfigurationEvent event = createConfigurationEvent(eventType, factoryPid, pid);
        if ( event == null ) {
            return null;
        }

        ServiceReference<ConfigurationListener>[] listenerRefs = listenerTracker.getServiceReferences();
        if ( (listenerRefs == null) || (listenerRefs.length == 0) ) {
            return null;
        }

        String qPid = (factoryPid != null) ? factoryPid : pid;

        return schedule( qPid, () -> handleEvent(factoryPid, pid, qPid, listenerRefs, event) );
    }

    /**
     * Send a configuration event to configuration event listeners.
     *
     * Emit FFDC and continue if an exception is thrown by the listener.
     *
     * @param factoryPid The factory PID of the event.
     * @param pid The PID of the event.
     * @param qPid The selected ID of the event.
     * @param listenerRefs References to listeners which are to receive the event.
     * @param event The event being sent to the listeners.
     */
    @FFDCIgnore(Exception.class)
    private void handleEvent(String factoryPid, String pid, String qPid,
                             ServiceReference<ConfigurationListener>[] listenerRefs,
                             ConfigurationEvent event) {

        String prefix = "ConfigEventDispatcher.handleEvent:";
        if ( TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled() ) {
            Tr.event(tc, prefix + " Sending event for " + qPid);
        }
        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, prefix + " Event [ " + event + " ] ID [ " + qPid + " ]");
        }

        for ( ServiceReference<ConfigurationListener> listenerRef : listenerRefs ) {
            if ( listenerRef == null ) {
                continue;
            }
            ConfigurationListener listener = listenerTracker.getService(listenerRef);
            if ( listener == null ) {
                continue;
            }

            if ( !FrameworkState.isValid() ) {
                if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                    Tr.debug(tc, prefix +
                                 " Framework is not valid: Skipping event [ " + event + " ]" +
                                 " to listener [ " + listener + " ]");
                }
                continue;
            }

            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                Tr.debug(tc, prefix +
                             " Dispatching event [ " + event + " ]" +
                             " to listener [ " + listener + " ]");
            }

            try {
                listener.configurationEvent(event);

            } catch ( Exception e ) {
                if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                    Tr.debug(tc, prefix + " Exception dispatching [ " + event + " ] to [ " + listener + " ]", e);
                }
                FFDCFilter.processException(e, CLASS_NAME,
                                            prefix + " Exception dispatching configuration event",
                                            new Object[] { pid, factoryPid, event.getType(), listener });
            }
        }
    }
}
//@Formatter:on