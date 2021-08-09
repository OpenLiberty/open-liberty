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

package com.ibm.ws.sib.ra;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.admin.JsEngineComponent;
import com.ibm.ws.sib.admin.JsMEConfig;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.admin.LWMConfig;
import com.ibm.ws.sib.utils.TraceGroups;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Implementation of <code>JsEngineComponent</code> to provide the inbound
 * core SPI resource adapter with notification of messaging engines running
 * within its process.
 */
public final class SibRaEngineComponent implements JsEngineComponent {

    /**
     * The messaging engine passed on initialization.
     */
    private JsMessagingEngine _messagingEngine;

    /**
     * Flag to indicate that the server is stopping.
     */
    private static boolean _serverStopping;
    
    /**
     * Set of custom groups this ME belongs to.
     */
    private static final HashMap <String, HashSet<String>> CUSTOM_GROUPS = new HashMap <String, HashSet<String>> (); 

    /**
     * A map from bus names to sets of initialized messaging engines.
     */
    private static final Map MESSAGING_ENGINES = new HashMap();

    /**
     * A map from bus names to sets of active messaging engines.
     */
    private static final Map ACTIVE_MESSAGING_ENGINES = new HashMap();

    /**
     * A map from bus names to sets of registered listeners. The
     * <code>null</code> key is used for listeners that registered for
     * notifications from all buses.
     */
    private static final Map MESSAGING_ENGINE_LISTENERS = new HashMap();

    /**
     * A set of UUIDs of messaging engines that are currently being reloaded.
     */
    private static final Set RELOADING_MESSAGING_ENGINES = Collections
            .synchronizedSet(new HashSet());

    /**
     * The component to use for trace.
     */
    private static final TraceComponent TRACE = SibTr.register(
            SibRaEngineComponent.class, TraceGroups.TRGRP_RA,
            "com.ibm.ws.sib.ra.CWSIVMessages");

    /**
     * Registers a listener for active messaging engines on a bus.
     * 
     * @param listener
     *            the listener to register
     * @param busName
     *            the name of the bus the listener wishes to be informed about
     *            messaging engines for or <code>null</code> if it wishes to
     *            know about all messaging engines
     * @return an array of the currently active messaging engines or an empty
     *         array if there are none
     */
    public static JsMessagingEngine[] registerMessagingEngineListener(
            final SibRaMessagingEngineListener listener, final String busName) {

        final String methodName = "registerMessagingEngineListener";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(TRACE, methodName, new Object[] { listener, busName });
        }

        final Set activeMessagingEngines = new HashSet();

        /*
         * Take lock on active messaging engines to ensure that the activation
         * of a messaging engine is reported once and once only either in the
         * array returned from this method or by notification to the listener.
         */

        synchronized (ACTIVE_MESSAGING_ENGINES) {

            synchronized (MESSAGING_ENGINE_LISTENERS) {

                // Add listener to map
                Set listeners = (Set) MESSAGING_ENGINE_LISTENERS.get(busName);
                if (listeners == null) {
                    listeners = new HashSet();
                    MESSAGING_ENGINE_LISTENERS.put(busName, listeners);
                }
                listeners.add(listener);

            }

            if (busName == null) {

                // Add all of the currently active messaging engines
                for (final Iterator iterator = ACTIVE_MESSAGING_ENGINES
                        .values().iterator(); iterator.hasNext();) {

                    final Set messagingEngines = (Set) iterator.next();
                    activeMessagingEngines.addAll(messagingEngines);

                }

            } else {

                // Add active messaging engines for the given bus if any
                final Set messagingEngines = (Set) ACTIVE_MESSAGING_ENGINES
                        .get(busName);
                if (messagingEngines != null) {
                    activeMessagingEngines.addAll(messagingEngines);
                }

            }

        }

        final JsMessagingEngine[] result = (JsMessagingEngine[]) activeMessagingEngines
                .toArray(new JsMessagingEngine[activeMessagingEngines.size()]);

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(TRACE, methodName, result);
        }
        return result;

    }

    /**
     * Deregisters a listener for active messaging engines on a bus.
     * 
     * @param listener
     *            the listener to deregister
     * @param busName
     *            the name of the bus the listener was registered with
     */
    public static void deregisterMessagingEngineListener(
            final SibRaMessagingEngineListener listener, final String busName) {

        final String methodName = "deregisterMessagingEngineListener";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(TRACE, methodName, new Object[] { listener, busName });
        }

        synchronized (MESSAGING_ENGINE_LISTENERS) {

            final Set listeners = (Set) MESSAGING_ENGINE_LISTENERS.get(busName);
            if (listeners != null) {
                listeners.remove(listener);
                if (listeners.isEmpty()) {
                    MESSAGING_ENGINE_LISTENERS.remove(busName);
                }
            }

        }

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(TRACE, methodName);
        }

    }

    /**
     * Returns an array of initialized messaging engines for the given bus. If
     * there are none, an empty array is returned.
     * 
     * @param busName
     *            the bus name
     * @return the set of <code>JsMessagingEngine</code> instances
     */
    public static JsMessagingEngine[] getMessagingEngines(final String busName) {

        final String methodName = "getMessagingEngines";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(TRACE, methodName, busName);
        }

        final JsMessagingEngine[] result;

        synchronized (MESSAGING_ENGINES) {

            // Do we have any messaging engines for the given bus?
            final Set messagingEngines = (Set) MESSAGING_ENGINES.get(busName);

            if (messagingEngines == null) {

                // If not, return an empty array
                result = new JsMessagingEngine[0];

            } else {

                // If we do, convert the set to an array
                result = (JsMessagingEngine[]) messagingEngines
                        .toArray(new JsMessagingEngine[messagingEngines.size()]);

            }

        }

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(TRACE, methodName, result);
        }
        return result;

    }

    /**
     * Returns an array of started messaging engines for the given bus. If there
     * are none, an empty array is returned.
     * 
     * @param busName
     *            the bus name
     * @return the set of <code>JsMessagingEngine</code> instances
     */
    public static JsMessagingEngine[] getActiveMessagingEngines(
            final String busName) {

        final String methodName = "getActiveMessagingEngines";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(TRACE, methodName, busName);
        }

        final JsMessagingEngine[] result;

        synchronized (ACTIVE_MESSAGING_ENGINES) {

            // Do we have any messaging engines for the given bus?
            final Set messagingEngines = (Set) ACTIVE_MESSAGING_ENGINES
                    .get(busName);

            if (messagingEngines == null) {

                // If not, return an empty array
                result = new JsMessagingEngine[0];

            } else {

                // If we do, convert the set to an array
                result = (JsMessagingEngine[]) messagingEngines
                        .toArray(new JsMessagingEngine[messagingEngines.size()]);

            }

        }

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(TRACE, methodName, result);
        }
        return result;

    }

    /**
     * Returns <code>true</code> if the given messaging engine is reloading,
     * otherwise <code>false</code>.
     * 
     * @param meUuid
     *            the messaging engine UUID
     * @return <code>true</code> if the messaging engine is reloading,
     *         otherwise <code>false</code>
     */
    public static boolean isMessagingEngineReloading(final String meUuid) {

        final String methodName = "isMessagingEngineReloading";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(TRACE, methodName, meUuid);
        }

        final boolean reloading = RELOADING_MESSAGING_ENGINES.contains(meUuid);

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(TRACE, methodName, Boolean.valueOf(reloading));
        }
        return reloading;

    }

    /**
     * Returns a flag indicating the server state.
     * 
     * @return <code>true</code> if the server is stopping, otherwise
     *         <code>false</code>
     */
    public static boolean isServerStopping() {

        return _serverStopping;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#initialize(com.ibm.ws.sib.admin.JsMessagingEngine)
     */
    public void initialize(final JsMessagingEngine engine) {

        final String methodName = "initialize";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, engine);
        }

        final Set listeners;

        _messagingEngine = engine;

        synchronized (MESSAGING_ENGINES) {

            // Do we already have a set of initialized MEs on this bus?
            Set messagingEngines = (Set) MESSAGING_ENGINES.get(engine
                    .getBusName());

            // If not, create a new set and add it to the map
            if (messagingEngines == null) {

                messagingEngines = new HashSet();
                MESSAGING_ENGINES.put(engine.getBusName(), messagingEngines);

            }

            // Add the initializing ME to the set
            messagingEngines.add(engine);

            // Get listeners to notify
            listeners = getListeners(_messagingEngine.getBusName());

        }

        // Notify listeners
        for (final Iterator iterator = listeners.iterator(); iterator.hasNext();) {

            final SibRaMessagingEngineListener listener = (SibRaMessagingEngineListener) iterator
                    .next();
            listener.messagingEngineInitializing(_messagingEngine);

        }

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#start(int)
     */
    public void start(int mode) {

        final String methodName = "start";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        final Set listeners;

        synchronized (ACTIVE_MESSAGING_ENGINES) {

            // Do we already have a set of active MEs on this bus?
            Set messagingEngines = (Set) ACTIVE_MESSAGING_ENGINES
                    .get(_messagingEngine.getBusName());

            // If not, create a new set and add it to the map
            if (messagingEngines == null) {

                messagingEngines = new HashSet();
                ACTIVE_MESSAGING_ENGINES.put(_messagingEngine.getBusName(),
                        messagingEngines);

            }

            // Add the activating ME to the set
            messagingEngines.add(_messagingEngine);

            // Get listeners to notify
            listeners = getListeners(_messagingEngine.getBusName());

        }

        // Notify listeners
        for (final Iterator iterator = listeners.iterator(); iterator.hasNext();) {

            final SibRaMessagingEngineListener listener = (SibRaMessagingEngineListener) iterator
                    .next();
            listener.messagingEngineStarting(_messagingEngine);

        }

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#stop(int)
     */
    public void stop(final int mode) {

        final String methodName = "stop";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        final Set listeners;

        RELOADING_MESSAGING_ENGINES.remove(_messagingEngine.getUuid()
                .toString());

        synchronized (ACTIVE_MESSAGING_ENGINES) {

            // Get the set of MEs for this ME's bus
            Set messagingEngines = (Set) ACTIVE_MESSAGING_ENGINES
                    .get(_messagingEngine.getBusName());

            // Set should always exist if the engine started but
            // just in case...

            if (messagingEngines != null) {

                // Remove the deactivating ME
                messagingEngines.remove(_messagingEngine);

                // If the set is now empty, take it out of the map
                if (messagingEngines.isEmpty()) {

                    ACTIVE_MESSAGING_ENGINES.remove(_messagingEngine
                            .getBusName());

                }

            } else {

                if (TRACE.isDebugEnabled()) {
                    SibTr.debug(this, TRACE, "Received stop for inactive ME:",
                            _messagingEngine);
                }

            }

            // Get listeners to notify
            listeners = getListeners(_messagingEngine.getBusName());

        }

        // Notify listeners
        for (final Iterator iterator = listeners.iterator(); iterator.hasNext();) {

            final SibRaMessagingEngineListener listener = (SibRaMessagingEngineListener) iterator
                    .next();
            listener.messagingEngineStopping(_messagingEngine, mode);

        }

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#destroy()
     */
    public void destroy() {

        final String methodName = "destroy";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName);
        }

        RELOADING_MESSAGING_ENGINES.remove(_messagingEngine.getUuid()
                .toString());

        synchronized (MESSAGING_ENGINES) {

            // Get the set of MEs for this ME's bus
            Set messagingEngines = (Set) MESSAGING_ENGINES.get(_messagingEngine
                    .getBusName());

            // Set should always exist if the engine initialized but
            // just in case...

            if (messagingEngines != null) {

                // Remove the destroyed ME
                messagingEngines.remove(_messagingEngine);

                // If the set is now empty, take it out of the map
                if (messagingEngines.isEmpty()) {

                    MESSAGING_ENGINES.remove(_messagingEngine.getBusName());

                }

            } else {

                if (TRACE.isDebugEnabled()) {
                    SibTr.debug(this, TRACE,
                            "Received destroy for unknown ME:",
                            _messagingEngine);
                }

            }

        }

        // Get listeners to notify
        final Set listeners = getListeners(_messagingEngine.getBusName());

        // Notify listeners
        for (final Iterator iterator = listeners.iterator(); iterator.hasNext();) {

            final SibRaMessagingEngineListener listener = (SibRaMessagingEngineListener) iterator
                    .next();
            listener.messagingEngineDestroyed(_messagingEngine);

        }

        _messagingEngine = null;

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#setCustomProperty(java.lang.String,
     *      java.lang.String)
     */
    public void setCustomProperty(String name, String value) {

        // Do nothing

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#setConfig(JsEObject)
     */
    public void setConfig(LWMConfig config) 
    {
    	// Store away the custom groups associated with the ME
    	
    	HashSet <String> customGrps = new HashSet <String>();
    	List l = new ArrayList<String>();
    	String str =((JsMEConfig) config).getMessagingEngine().getName();
     	l.add(str);
    	customGrps.addAll(l);
    	CUSTOM_GROUPS.put(_messagingEngine.getUuid().toString(), customGrps);
    	_messagingEngine.getName();
    }
    
    /**
     * Gets a set of groups that the specified ME belongs to
     * @param meUuid The uuid of the me
     * @return A set containing the names of the groups the specified ME belongs to.
     */
    public static Set getCustomGroups (String meUuid)
    {
    	return CUSTOM_GROUPS.get (meUuid);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#serverStarted()
     */
    public void serverStarted() {

        // Ignored

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#serverStopping()
     */
    public void serverStopping() {

        final String methodName = "serverStopping";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(TRACE, methodName);
            SibTr.exit(TRACE, methodName);
        }
        _serverStopping = true;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#busReloaded(ConfigObject,
     *      boolean, boolean, boolean)
     */
    public void busReloaded(final LWMConfig newBus, final boolean busChanged,
            final boolean destinationsChanged, final boolean mediationsChanged) {

        final String methodName = "busReloaded";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, new Object[] { newBus,
                    Boolean.valueOf(busChanged),
                    Boolean.valueOf(destinationsChanged),
                    Boolean.valueOf(mediationsChanged) });
        }

        RELOADING_MESSAGING_ENGINES.add(_messagingEngine.getUuid().toString());

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.JsEngineComponent#engineReloaded(com.ibm.ws.sib.admin.JsMessagingEngine)
     */
    
    //lohith liberty change
    public void engineReloaded(Object objectSent) {

    	final JsMessagingEngine engine = (JsMessagingEngine)objectSent;
    	
        final String methodName = "engineReloaded";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(this, TRACE, methodName, engine);
        }

        RELOADING_MESSAGING_ENGINES.remove(engine.getUuid().toString());

        // Get listeners to notify
        final Set listeners = getListeners(engine.getBusName());

        // Notify listeners
        for (final Iterator iterator = listeners.iterator(); iterator.hasNext();) {

            final SibRaMessagingEngineListener listener = (SibRaMessagingEngineListener) iterator
                    .next();
            listener.messagingEngineReloaded(engine);

        }

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(this, TRACE, methodName);
        }

    }

    /**
     * Returns the set of listeners for the given bus.
     * 
     * @param busName
     *            the name of the bus
     * @return the listeners for that bus or the empty set if there are none.
     */
    private static Set getListeners(final String busName) {

        final String methodName = "getListeners";
        if (TRACE.isEntryEnabled()) {
            SibTr.entry(TRACE, methodName, busName);
        }

        final Set listeners = new HashSet();

        synchronized (MESSAGING_ENGINE_LISTENERS) {

            // Get listeners for the particular bus
            final Set busListeners = (Set) MESSAGING_ENGINE_LISTENERS
                    .get(busName);
            if (busListeners != null) {
                listeners.addAll(busListeners);
            }

            // Get listeners for all busses
            final Set noBusListeners = (Set) MESSAGING_ENGINE_LISTENERS
                    .get(null);
            if (noBusListeners != null) {
                listeners.addAll(noBusListeners);
            }

        }

        if (TRACE.isEntryEnabled()) {
            SibTr.exit(TRACE, methodName, listeners);
        }
        return listeners;

    }

	@Override
	public void busReloaded(Object newBus, boolean busChanged, boolean destChg,
			boolean medChg) {
		// TODO Auto-generated method stub
		
	}


}
