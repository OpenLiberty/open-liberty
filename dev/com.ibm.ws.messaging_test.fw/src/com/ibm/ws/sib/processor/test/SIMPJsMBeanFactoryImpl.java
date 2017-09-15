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

package com.ibm.ws.sib.processor.test;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Properties;

import javax.management.StandardMBean;

import com.ibm.ws.sib.admin.Controllable;
import com.ibm.ws.sib.admin.ControllableRegistrationService;
import com.ibm.ws.sib.admin.ControllableType;
import com.ibm.ws.sib.admin.JsMessagingEngine;
import com.ibm.ws.sib.admin.RuntimeEventListener;
import com.ibm.ws.sib.admin.SIBExceptionInvalidValue;
import com.ibm.ws.sib.admin.exception.AlreadyRegisteredException;
import com.ibm.ws.sib.admin.exception.NotRegisteredException;
import com.ibm.ws.sib.admin.exception.ParentNotFoundException;

/**
 * Inner class to handle and hide the registration gawp from the outer class.
 * 
 */
public class SIMPJsMBeanFactoryImpl
                implements ControllableRegistrationService,
                RegistrationServiceBackDoor {
    // Keep track of events fired, if event notification is enabled
    private static ArrayList events = null;

    /**
     * A default runtime event listener. Allows the events to be routed to
     * someone else if they set their RuntimeEventListener into it.
     */
    public class ForwardingRuntimeEventListener implements RuntimeEventListener {
        RuntimeEventListener forwardToListener = null;

        public void runtimeEventOccurred(RuntimeEvent event) {
            if (forwardToListener == null) {
                // Ignore any events we're passed by default.
            } else {
                // Pass the event on.
                forwardToListener.runtimeEventOccurred(null,
                                                       null,
                                                       null,
                                                       null);
            }
        }

        public void setForwardToListener(RuntimeEventListener newListener) {

        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.admin.RuntimeEventListener#runtimeEventOccurred(com.ibm.ws.sib.admin.JsMessagingEngine, java.lang.String, java.lang.String, java.util.Properties)
         */
        @Override
        public void runtimeEventOccurred(JsMessagingEngine me,
                                         String type,
                                         String message,
                                         Properties properties) {

            RuntimeEvent ev = new RuntimeEvent(type, message, properties);
            events.add(ev);
        }
    }

    // Guess the max number of controllable types there might be...
    private static final int MAX_CONTROLLABLE_TYPES = 20;
    private final Registrations[] registrations =
                    new Registrations[MAX_CONTROLLABLE_TYPES];

    public SIMPJsMBeanFactoryImpl() {
        for (int index = 0; index < registrations.length; index += 1) {
            registrations[index] = new Registrations();
        }
    }

    /**
     * Holds a set of registrations for a particular type of resource.
     */
    class Registrations {
        /**
         * Returns null if a controllable using that id can't be found.
         * 
         * @param id
         * @return
         */
        public synchronized Controllable findControllableById(String id) {
            Controllable result = (Controllable) lookupById.get(id);
            return result;
        }

        private final Hashtable lookupByHashCode = new Hashtable();
        private final Hashtable lookupListenerByControllableHash = new Hashtable();
        private final Hashtable lookupById = new Hashtable();

        /**
         * Looks up the current event listener used by a registered controllable.
         * Tells it to forward events to the user-specified event listener.
         * 
         * @param usersListener
         * @param controllable
         * @throws NotRegisteredException
         */
        public synchronized void setControllableEventListener(
                                                              RuntimeEventListener usersListener,
                                                              Controllable controllable)
                        throws NotRegisteredException {
            if (lookupByHashCode.get(controllable) == null) {
                throw new NotRegisteredException("Controller not registered.");
            }
            ForwardingRuntimeEventListener forwardingListener =
                            (ForwardingRuntimeEventListener) lookupListenerByControllableHash.get(
                                            controllable);
            forwardingListener.setForwardToListener(usersListener);

        }

        public synchronized void deregister(Controllable control, String id)
                        throws NotRegisteredException {

            if (lookupByHashCode.get(control) == null) {
                throw new NotRegisteredException(
                                "Control with hash " + control.hashCode() + " is not registered.");
            }

            if (lookupById.get(id) == null) {
                throw new NotRegisteredException(
                                "Control with id <" + id + "> is not registered.");
            }

            // The controllable is registered. Remove the registration.
            lookupById.remove(id);
            lookupByHashCode.remove(control);
            lookupListenerByControllableHash.remove(control);
        }

        public synchronized void register(
                                          Controllable control,
                                          String id,
                                          RuntimeEventListener listener)
                        throws AlreadyRegisteredException {
            if (lookupByHashCode.get(control) != null) {
                throw new AlreadyRegisteredException(
                                "Object " + control.hashCode() + " is already registered.");
            }

            if (lookupById.get(id) != null) {
                throw new AlreadyRegisteredException(
                                "Object with msgstore id <" + id + "> is already registered.");
            }

            lookupById.put(id, control);
            lookupByHashCode.put(control, control);
            lookupListenerByControllableHash.put(control, listener);
        }

    } // End of the Registrations class.

    /**
     * Holds a Runtime Event.
     */
    public class RuntimeEvent {
        private final String type;
        private final String message;
        private final Properties props;

        RuntimeEvent(String type,
                     String message,
                     Properties props) {
            this.type = type;
            this.message = message;
            this.props = props;
        }

        /**
         * @return
         */
        public String getMessage() {
            return message;
        }

        /**
         * @return
         */
        public Properties getProps() {
            return props;
        }

        /**
         * @return
         */
        public String getType() {
            return type;
        }

        @Override
        public String toString() {
            String evString = "*NOTIFICATION* Type: " + type + ", message: " + message + ", properties: " + props;
            return evString;
        }
    }

    /**
     * If the unit test has a reference to the resource's controllable
     * implementatation object, he can add a runtime event listener of
     * his own, to listen for specific events coming from that resource.
     * <p>
     * Calling it again forces the old event listener to be abandoned.
     * 
     * @param listener
     * @param controllable
     */
    @Override
    public synchronized void setControllableEventListener(
                                                          RuntimeEventListener listener,
                                                          Controllable controllable,
                                                          ControllableType type)
                    throws SIBExceptionInvalidValue, NotRegisteredException {
        assertValidControllable(controllable);
        Registrations regs = getRegistrations(type);
        regs.setControllableEventListener(listener, controllable);
    }

    private void assertValidControllable(Controllable control)
                    throws SIBExceptionInvalidValue {
        if (control == null) {
            throw new SIBExceptionInvalidValue("controllable is null");
        }
    }

    private Registrations getRegistrations(ControllableType type)
                    throws SIBExceptionInvalidValue {
        int index = type.toInt();
        if (index < 0 || index >= MAX_CONTROLLABLE_TYPES) {
            throw new SIBExceptionInvalidValue("type invalid");
        }

        Registrations regs = registrations[index];
        return regs;
    }

    @Override
    public RuntimeEventListener register(
                                 Controllable controllable,
                                 ControllableType type)
                    throws AlreadyRegisteredException, SIBExceptionInvalidValue {
        RuntimeEventListener listenerToReturn =
                        new ForwardingRuntimeEventListener();
        assertValidControllable(controllable);
        Registrations regs = getRegistrations(type);
        String id = controllable.getId();
        regs.register(controllable, id, listenerToReturn);
        return listenerToReturn;
    }

    /**
     * De-registers
     * 
     * @param controllable
     * @param type
     * @throws NotRegisteredException
     * @throws SIBExceptionInvalidValue
     */
    @Override
    public void deregister(Controllable controllable, ControllableType type)
                    throws NotRegisteredException, SIBExceptionInvalidValue {
        assertValidControllable(controllable);
        Registrations regs = getRegistrations(type);
        String id = controllable.getId();
        regs.deregister(controllable, id);
    }

    /**
     * Allows the caller to look up a controllable by id.
     * 
     * @return null if not found, or the controllable if it is found.
     */
    @Override
    public Controllable findControllableById(String id, ControllableType type)
                    throws SIBExceptionInvalidValue {
        Controllable controllable = null;
        Registrations regs = getRegistrations(type);
        controllable = regs.findControllableById(id);
        return controllable;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.ControllableRegistrationService#register(com.ibm.ws.sib.admin.Controllable, com.ibm.ws.sib.admin.Controllable,
     * com.ibm.ws.sib.admin.ControllableType)
     */
    @Override
    public RuntimeEventListener register(
                                 Controllable arg0,
                                 Controllable arg1,
                                 ControllableType arg2) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.admin.ControllableRegistrationService#register(com.ibm.ws.sib.admin.Controllable, com.ibm.ws.sib.admin.RuntimeEventListener,
     * com.ibm.ws.sib.admin.ControllableType)
     */
    public RuntimeEventListener register(
                                         Controllable arg0,
                                         RuntimeEventListener arg1,
                                         ControllableType arg2) {
        return null;
    }

    /**
     * The following methods allow rudimentary capture and reporting
     * of events in the standalone unittest environment.
     */
    public static void resetEventing() {
        if (events == null) {
            events = new ArrayList();
        } else
            events.clear();
    }

    public static ArrayList getEvents() {
        return events;
    }

    public static String getEventType(int eventId) {
        RuntimeEvent ev = (RuntimeEvent) events.get(eventId);
        return ev.getType();
    }

    public static boolean checkForEventType(String eventType) {
        boolean foundEvent = false;

        for (int i = 0; i < events.size(); i++) {
            RuntimeEvent ev = (RuntimeEvent) events.get(i);
            if (eventType.equals(ev.getType())) {
                foundEvent = true;
                break;
            }
        }
        return foundEvent;
    }

    public RuntimeEventListener createListener() {
        return new ForwardingRuntimeEventListener();
    }

    /** {@inheritDoc} */
    @Override
    public RuntimeEventListener register(Controllable controllable, StandardMBean parent, ControllableType type) throws AlreadyRegisteredException, ParentNotFoundException, SIBExceptionInvalidValue {
        // TODO Auto-generated method stub
        return null;
    }
}
