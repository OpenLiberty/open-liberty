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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

//@formatter:off
/**
 * This keeps track of all active managed services..
 *
 * This is very similar to {@link ManagedServiceFactoryTracker}, but they are not
 * quite the same:
 *
 * The factory tracker deals with both factory PIDs and configuration PIDs.
 *
 * The service tracker deals with configuration PIDs.
 */
class ManagedServiceTracker extends ServiceTracker<ManagedService, ManagedService> {
    private static final TraceComponent tc = Tr.register(ManagedServiceTracker.class, ConfigAdminConstants.TR_GROUP, ConfigAdminConstants.NLS_PROPS);

    private static final String CLASS_NAME = ManagedServiceTracker.class.getName();

    //

    private static String[] getServicePids(ServiceReference<ManagedService> msRef) {
        Object pidObj = msRef.getProperty(Constants.SERVICE_PID);

        if ( pidObj instanceof String ) {
            return new String[] { (String) pidObj };

        } else if ( pidObj instanceof String[] ) {
            return (String[]) pidObj;

        } else if ( pidObj instanceof Collection ) {
            @SuppressWarnings("unchecked")
            Collection<String> pidCollection = (Collection<String>) pidObj;
            return pidCollection.toArray(new String[pidCollection.size()]);

        } else {
            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                Tr.debug(tc, "getServicePid: No PIDs for service reference: [ " + msRef + " ]");
            }
            return null;
        }
    }

    //

    public ManagedServiceTracker(ConfigAdminServiceFactory casf, BundleContext bc) {
        super(bc, ManagedService.class.getName(), null);

        this.caFactory = casf;

        this.managedServices = new HashMap<>();
        this.managedServiceReferences = new HashMap<>();
    }

    //

    private final ConfigAdminServiceFactory caFactory;

    /**
     * Queue a call to update a service.
     *
     * See {@link ConfigAdminServiceFactory#updateQueue} and
     * {@link ManagedService#updated(Dictionary)}.
     *
     * @param ms The service for the configuration which was update.
     * @param pid The PID of the configuration.
     * @param properties The properties of the configuration.
     *
     * @return The schedule task.
     */
    private Future<?> asyncUpdated(ManagedService ms, String pid, Dictionary<String, ?> properties) {
        return caFactory.updateQueue.add(pid, () -> {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "asyncUpdated: service.updated called for " + pid);
            }
            try {
                ms.updated(properties);
            } catch ( Throwable t ) {
                if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                    Tr.debug(tc, "asyncUpdated: Service exception: ", t);
                }
                FFDCFilter.processException(t, CLASS_NAME, "asyncUpdated()", new Object[] { pid, ms });
            }
        });
    }

    //

    private final Map<String, ManagedService> managedServices;
    private final Map<String, ServiceReference<ManagedService>> managedServiceReferences;

    private ManagedService getManagedService(String pid) {
        synchronized ( managedServiceReferences ) {
            return managedServices.get(pid);
        }
    }

    private ServiceReference<ManagedService> getManagedServiceReference(String pid) {
        synchronized ( managedServiceReferences ) {
            return managedServiceReferences.get(pid);
        }
    }

    private List<String> getPidsForManagedService(ManagedService targetService) {
        ArrayList<String> pids = new ArrayList<String>();

        synchronized ( managedServiceReferences ) {
            managedServices.forEach( (String pid, ManagedService service) -> {
                if ( service == targetService ) {
                    pids.add(pid);
                }
            } );
        }

        return pids;
    }

    private boolean trackManagedService(String pid, ServiceReference<ManagedService> reference, ManagedService service) {
        synchronized ( managedServiceReferences ) {
            if ( managedServiceReferences.containsKey(pid) ) {
                if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                    Tr.debug(tc, "trackManagedService: Service already registered [ " + pid + " ]");
                }
                return false;
            } else {
                managedServiceReferences.put(pid, reference);
                managedServices.put(pid, service);
                return true;
            }
        }
    }

    private void untrackManagedService(String pid) {
        synchronized ( managedServiceReferences ) {
            managedServiceReferences.remove(pid);
            managedServices.remove(pid);
        }
    }

    //

    @Override
    public ManagedService addingService(ServiceReference<ManagedService> msRef) {
        String[] pids = getServicePids(msRef);
        if ( pids == null ) {
            return null;
        }

        // Ugh: Direct access to a superclass variable.
        // Unfortunately, no accessor is defined.
        ManagedService ms = context.getService(msRef);
        if ( ms == null ) {
            return null;
        }

        synchronized ( caFactory.getConfigurationStore() ) {
            for ( String pid : pids ) {
                add(msRef, pid, ms);
            }
        }

        return ms;
    }

    private void add(ServiceReference<ManagedService> msRef, String pid, ManagedService ms) {
        if ( !trackManagedService(pid, msRef, ms) ) {
            return;
        }

        ExtendedConfigurationImpl config = caFactory.getConfigurationStore().findConfiguration(pid);

        if ( config == null ) {
            asyncUpdated(ms, pid, null);

        } else {
            config.lock();

            try {
                if ( config.getFactoryPid(!ExtendedConfigurationImpl.CHECK_DELETED) != null ) {
                    if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() )
                        Tr.debug(tc, "Configuration [ " + pid + " ] should only be used by a [ " + ManagedServiceFactory.class.getName() + " ]",
                                 config);

                } else if ( config.isDeleted() ) {
                    asyncUpdated(ms, pid, null);

                } else if ( !config.bind(msRef.getBundle()) ) {
                    if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                        Tr.debug(tc, "add: Failed to bind [ " + pid + " ]" +
                                     " to [ " + msRef.getBundle().getLocation() + " ]");
                    }
                } else {
                    Dictionary<String, Object> properties = config.getProperties();
                    caFactory.modifyConfiguration(msRef, properties, ms);
                    asyncUpdated(ms, pid, properties);
                }

            } finally {
                config.unlock();
            }
        }
    }

    @SuppressWarnings("null")
    @Override
    public void modifiedService(ServiceReference<ManagedService> msRef, ManagedService ms) {
        String[] newPids = getServicePids(msRef);
        List<String> oldPids = getPidsForManagedService(ms);

        boolean noOld = ( (oldPids == null) || oldPids.isEmpty() );
        boolean noNew = ( (newPids == null) || (newPids.length == 0) );
        if ( noNew && noOld ) {
            return;
        }

        synchronized ( caFactory.getConfigurationStore() ) {
            if ( noNew ) { // 'noOld' must be false.
                for ( String oldPid : oldPids ) {
                    remove(oldPid); // All old must have been removed.
                }

            } else if ( noOld ) { // 'noNew' must be false.
                for ( String newPid : newPids ) {
                    add(msRef, newPid, ms); // All new must have been added.
                }

            } else {
                Set<String> useNew = new HashSet<>( newPids.length );
                for ( String newPid : newPids ) {
                    useNew.add(newPid);
                }

                for ( String oldPid : oldPids ) {
                    if ( !useNew.remove(oldPid) ) {
                        remove(oldPid); // Not there any more.
                    } else {
                        // Still present; nothing to do.
                    }
                }

                // Any remaining 'new' have no corresponding 'old',
                // and must really be new.

                for ( String newPid : newPids ) {
                    add(msRef, newPid, ms);
                }
            }
        }
    }

    protected Future<?> notifyUpdated(ExtendedConfigurationImpl config) {
        config.checkLocked();

        String pid = config.getPid(); // Fail if the configuration is deleted!

        ServiceReference<ManagedService> msRef = getManagedServiceReference(pid);
        if ( msRef == null ) {
            return null;
        }

        // 'bind' answers true if the bundle is already bound to the configuration.
        // 'bind' will assign the bundle, if none is assigned.
        if ( !config.bind( msRef.getBundle() ) ) {
            return null;
        }

        ManagedService ms = getManagedService(pid);
        Dictionary<String, Object> properties = config.getProperties();

        caFactory.modifyConfiguration(msRef, properties, ms);

        return asyncUpdated(ms, pid, properties);
    }

    @Override
    public void removedService(ServiceReference<ManagedService> msRef, ManagedService ms) {
        String[] pids = getServicePids(msRef);
        if ( pids == null ) {
            return;
        }

        synchronized ( caFactory.getConfigurationStore() ) {
            for ( String pid : pids ) {
                remove(pid);
            }
        }

        context.ungetService(msRef);
    }

    private void remove(String pid) {
        ExtendedConfigurationImpl config = caFactory.getConfigurationStore().findConfiguration(pid);
        if ( config == null ) {
            untrackManagedService(pid);
        } else {
            config.lock();
            try {
                untrackManagedService(pid);
            } finally {
                config.unlock();
            }
        }
    }

    protected Future<?> notifyDeleted(ExtendedConfigurationImpl config) {
        config.checkLocked();

        String pid = config.getPid(!ExtendedConfigurationImpl.CHECK_DELETED);

        ServiceReference<ManagedService> msRef = getManagedServiceReference(pid);
        if ( msRef == null ) {
            return null;
        }

        if ( !config.bind(msRef.getBundle()) ) {
            return null;
        }

        ManagedService ms = getManagedService(pid);
        return asyncUpdated(ms, pid, null);
    }
}
//@formatter:on