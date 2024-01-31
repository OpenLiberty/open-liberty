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
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

//@formatter:off
/**
 * This keeps track of all active managed service factories.
 *
 * This is very similar to {@link ManagedServiceTracker}, but they are not
 * quite the same:
 *
 * The factory tracker deals with both factory PIDs and configuration PIDs.
 *
 * The service tracker deals with configuration PIDs.
 */
class ManagedServiceFactoryTracker extends ServiceTracker<ManagedServiceFactory, ManagedServiceFactory> {
    private static final TraceComponent tc =
        Tr.register(ManagedServiceFactoryTracker.class,
                    ConfigAdminConstants.TR_GROUP, ConfigAdminConstants.NLS_PROPS);

    private static final String CLASS_NAME = ManagedServiceFactoryTracker.class.getName();

    //

    /**
     * Answer the service PIDs stored in a reference.
     *
     * The PIDs may be stored as a single PID, as an array of PIDs, or as a collection
     * of PIDs.  Convert the stored value into a string array.
     *
     * Answer null if no service PID is stored in the reference, or if the stored
     * PID is not a string, string array, or collection.
     *
     * See {@link Constants#SERVICE_PID}.
     *
     * @param msfRef The reference from which to retrieve service PIDs.
     *
     * @return The service PIDs stored in the reference.
     */
    private static String[] getServicePids(ServiceReference<ManagedServiceFactory> msfRef) {
        Object pidObj = msfRef.getProperty(Constants.SERVICE_PID);

        if ( pidObj instanceof String ) {
            return new String[] { (String) pidObj };

        } else if ( pidObj instanceof String[] ) {
            return (String[]) pidObj;

        } else if ( pidObj instanceof Collection ) {
            @SuppressWarnings("unchecked")
            Collection<String> pidCollection = (Collection<String>) pidObj;
            return pidCollection.toArray( new String[ pidCollection.size() ] );

        } else {
            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                Tr.debug(tc, "getServicePid: No PIDs for service factory reference: [ " + msfRef + " ]");
            }
            return null;
        }
    }

    //

    public ManagedServiceFactoryTracker(ConfigAdminServiceFactory caFactory, BundleContext bc) {
        super(bc, ManagedServiceFactory.class.getName(), null);

        this.caFactory = caFactory;

        this.managedServiceFactories = new HashMap<>();
        this.managedServiceFactoryReferences = new HashMap<>();
    }

    //

    private final ConfigAdminServiceFactory caFactory;

    /**
     * Queue a call to delete a service factory.
     *
     * See {@link ConfigAdminServiceFactory#updateQueue} and
     * {@link ManagedServiceFactory#deleted(String)}.
     *
     * @param msf The service factory for the configuration which
     *     was deleted.
     * @param factoryPID The factory PID of the configuration.
     * @param pid The PID of the configuration.
     *
     * @return The schedule task.
     */
    private Future<?> asyncDeleted(ManagedServiceFactory msf,
                                   String factoryPid, String pid) {

        return caFactory.updateQueue.add(factoryPid, () -> {
            try {
                msf.deleted(pid);
            } catch ( Throwable t ) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "asyncDeleted: Service factory exception: " + t);
                }
                FFDCFilter.processException(t, CLASS_NAME, "asyncDeleted()", new Object[] { factoryPid, pid, msf });
            }
        });
    }

    /**
     * Queue a call to update a service factory.
     *
     * See {@link ConfigAdminServiceFactory#updateQueue} and
     * {@link ManagedServiceFactory#updated(String)}.
     *
     * @param msf The service factory for the configuration which
     *     was update.
     * @param factoryPID The factory PID of the configuration.
     * @param pid The PID of the configuration.
     *
     * @return The schedule task.
     */
    private Future<?> asyncUpdated(ManagedServiceFactory msf,
                                   String factoryPid, String pid,
                                   Dictionary<String, ?> properties) {

        return caFactory.updateQueue.add(factoryPid, () -> {
            if ( TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled() ) {
                Tr.event(tc, "asyncUpdated: service.updated called for " + pid + " (" + factoryPid + ")");
            }
            try {
                msf.updated(pid, properties);
            } catch ( Throwable t ) {
                if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                    Tr.debug(tc, "asyncUpdated: Service factory exception: ", t);
                }
                FFDCFilter.processException(t, CLASS_NAME, "asyncUpdated()", new Object[] { factoryPid, pid, msf });
            }
        });
    }

    //

    /** Managed service factories.  Keys are factory PIDs. */
    private final Map<String, ManagedServiceFactory> managedServiceFactories;

    /** Managed service factory references.  Keys are factory PIDs. */
    private final Map<String, ServiceReference<ManagedServiceFactory>> managedServiceFactoryReferences;

    private ManagedServiceFactory getManagedServiceFactory(String factoryPid) {
        synchronized ( managedServiceFactoryReferences ) {
            return managedServiceFactories.get(factoryPid);
        }
    }

    private ServiceReference<ManagedServiceFactory> getManagedServiceFactoryReference(String factoryPid) {
        synchronized ( managedServiceFactoryReferences ) {
            return managedServiceFactoryReferences.get(factoryPid);
        }
    }

    /**
     * Answer the factory PIDs mapped to a service factory.
     *
     * @param targetMsf The server for which to select factory PIDs.
     *
     * @return The factory PIDs mapped to the service factory.
     */
    private List<String> getPidsForManagedServiceFactory(ManagedServiceFactory targetMsf) {
        ArrayList<String> factoryPids = new ArrayList<String>();

        synchronized ( managedServiceFactoryReferences ) {
            managedServiceFactories.forEach( ( String factoryPid, ManagedServiceFactory msf) -> {
                if ( msf == targetMsf ) {
                    factoryPids.add(factoryPid);
                }
            } );
        }

        return factoryPids;
    }

    private boolean trackManagedServiceFactory(String factoryPid,
                                               ServiceReference<ManagedServiceFactory> msfRef,
                                               ManagedServiceFactory msf) {

        synchronized ( managedServiceFactoryReferences ) {
            if ( managedServiceFactoryReferences.containsKey(factoryPid) ) {
                if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                    Tr.debug(tc, "trackManagedServiceFactory: Service factory already registered [ " + factoryPid + " ]");
                }
                return false;
            } else {
                managedServiceFactoryReferences.put(factoryPid, msfRef);
                managedServiceFactories.put(factoryPid, msf);
                return true;
            }
        }
    }

    private void untrackManagedServiceFactory(String factoryPid) {
        synchronized ( managedServiceFactoryReferences ) {
            managedServiceFactoryReferences.remove(factoryPid);
            managedServiceFactories.remove(factoryPid);
        }
    }

    //

    @Override
    public ManagedServiceFactory addingService(ServiceReference<ManagedServiceFactory> msfRef) {
        String[] factoryPids = getServicePids(msfRef);
        if ( factoryPids == null ) {
            return null;
        }

        // Ugh: Direct access to a superclass variable.
        // Unfortunately, no accessor is defined.
        ManagedServiceFactory msf = context.getService(msfRef);
        if ( msf == null ) {
            return null;
        }

        synchronized ( caFactory.getConfigurationStore() ) {
            for ( String factoryPid : factoryPids ) {
                add(msfRef, factoryPid, msf);
            }
        }

        return msf;
    }

    private void add(ServiceReference<ManagedServiceFactory> msfRef, String factoryPid, ManagedServiceFactory msf) {
        if ( !trackManagedServiceFactory(factoryPid, msfRef, msf) ) {
            return;
        }

        ExtendedConfigurationImpl[] configs = caFactory.getConfigurationStore().getFactoryConfigurations(factoryPid);
        try {
            for ( ExtendedConfigurationImpl config : configs ) {
                config.lock();
            }

            for ( ExtendedConfigurationImpl config : configs ) {
                if (config.isDeleted()) {
                    continue;
                }

                if ( !config.bind( msfRef.getBundle() ) ) {
                    if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                        Tr.debug(tc, "add: Failed to bind [ " + config.getPid() + " ]" +
                                     " to [ " + msfRef.getBundle().getLocation() + " ]");
                    }
                    continue;
                }

                Dictionary<String, Object> properties = config.getProperties();
                caFactory.modifyConfiguration(msfRef, properties, msf);
                asyncUpdated(msf, config.getFactoryPid(), config.getPid(), properties);
            }

        } finally {
            for ( ExtendedConfigurationImpl config : configs ) {
                config.unlock();
            }
        }
    }

    /**
     * Modifications have been made to the managed factories.
     *
     * Process each old factory PID and each new factory PID.
     *
     * @param msfRef Service reference.  Use to retrieve new factory PIDs.
     * @param msf Service factory.  Used to retrieve old factory PIDs.
     */
    @SuppressWarnings("null")
    @Override
    public void modifiedService(ServiceReference<ManagedServiceFactory> msfRef, ManagedServiceFactory msf) {
        List<String> oldPids = getPidsForManagedServiceFactory(msf);
        String[] newPids = getServicePids(msfRef);

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
                    add(msfRef, newPid, msf); // All new must have been added.
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
                    add(msfRef, newPid, msf);
                }
            }
        }
    }

    protected Future<?> notifyUpdated(ExtendedConfigurationImpl config) {
        config.checkLocked();

        String factoryPid = config.getFactoryPid(); // Fail if the configuration is deleted!

        ServiceReference<ManagedServiceFactory> msfRef = getManagedServiceFactoryReference(factoryPid);
        if ( msfRef == null ) {
            return null;
        }

        // 'bind' answers true if the bundle is already bound to the configuration.
        // 'bind' will assign the bundle, if none is assigned.
        if ( !config.bind( msfRef.getBundle() ) ) {
            return null;
        }

        ManagedServiceFactory msf = getManagedServiceFactory(factoryPid);
        String pid = config.getPid(); // Fail if the configuration is deleted!
        Dictionary<String, Object> properties = config.getProperties();

        caFactory.modifyConfiguration(msfRef, properties, msf);

        return asyncUpdated(msf, factoryPid, pid, properties);
    }

    //

    @Override
    public void removedService(ServiceReference<ManagedServiceFactory> msfRef, ManagedServiceFactory msf) {
        String[] factoryPids = getServicePids(msfRef);
        if ( factoryPids == null ) {
            return;
        }

        synchronized ( caFactory.getConfigurationStore() ) {
            for ( String factoryPid : factoryPids ) {
                remove(factoryPid);
            }
        }

        context.ungetService(msfRef);
    }

    private void remove(String factoryPid) {
        ExtendedConfigurationImpl[] configs = caFactory.getConfigurationStore().getFactoryConfigurations(factoryPid);
        try {
            for ( ExtendedConfigurationImpl config : configs ) {
                config.lock();
            }

            untrackManagedServiceFactory(factoryPid);

        } finally {
            for ( ExtendedConfigurationImpl config : configs ) {
                config.unlock();
            }
        }
    }

    protected Future<?> notifyDeleted(ExtendedConfigurationImpl config) {
        config.checkLocked();

        String factoryPid = config.getFactoryPid(!ExtendedConfigurationImpl.CHECK_DELETED);

        ServiceReference<ManagedServiceFactory> msfRef = getManagedServiceFactoryReference(factoryPid);
        if ( msfRef == null ) {
            return null;
        }

        // 'bind' answers true if the bundle is already bound to the configuration.
        //
        // 'bind' will assign the bundle, if none is assigned.

        if ( !config.bind( msfRef.getBundle() ) ) {
            return null;
        }

        ManagedServiceFactory msf = getManagedServiceFactory(factoryPid);
        String pid = config.getPid(!ExtendedConfigurationImpl.CHECK_DELETED);

        return asyncDeleted(msf, factoryPid, pid);
    }
}
//@formatter:on