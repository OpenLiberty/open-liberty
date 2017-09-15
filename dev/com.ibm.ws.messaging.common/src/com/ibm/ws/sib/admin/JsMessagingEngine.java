/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.admin;

import java.util.Set;

import com.ibm.ws.sib.utils.SIBUuid8;
/**
 * Interface to support the concept of a SIB Messaging Engine.
 */
public interface JsMessagingEngine {

    /**
     * Get the name of the Bus that this Messaging Engine is associated with. If
     * the Messaging Engine is standalone (i.e. it is not associated with a
     * particular Bus), then an empty string is returned.
     * 
     * @return String
     */
    public String getBusName();

    /**
     * Get the name of this Messaging Engine.
     * 
     * @return String
     */
    public String getName();

    /**
     * Gets the assigned UUID of this Messaging Engine.
     * 
     * @return SIBUuid8
     */
    public String getUuid();

    /**
     * Get the named instance of an internal component associated with this
     * instance of a Messaging Engine.
     * 
     * @param className The class name of the requested engine component.
     * @return JsEngineComponent the instance of the named class. If the specified
     *         class name was not found, then null is returned.
     */
    public JsEngineComponent getEngineComponent(String className);

    /**
     * Get an engine component that implements the specified interface. If there
     * are multiple components implementing the same interface which one is
     * returned is undefined, in this case the getEngineComponents method should
     * be used instead. If no engine component exists null will be returned.
     * 
     * @param <EngineComponent> The service interface provided by the engine
     *            component. This is not expected to be the
     *            JsEngineComponent interface.
     * @param clazz The class object representing the engine component.
     * @return The requested engine component, or null if none
     *         is found.
     */
    public <EngineComponent> EngineComponent getEngineComponent(Class<EngineComponent> clazz);

    /**
     * Get all the engine component that implements the specified service
     * interface. If no engine components exist then an empty array will be
     * returned.
     * 
     * @param <EngineComponent> The service interface provided by the engine
     *            component. This is not expected to be the
     *            JsEngineComponent interface.
     * @param clazz The class object representing the engine component.
     * @return The requested engine components.
     */
    public <EngineComponent> EngineComponent[] getEngineComponents(Class<EngineComponent> clazz);

    /**
     * Convenience method to return the Message Processor component associated
     * with this instance of a Messaging Engine.
     * 
     * @return JsEngineComponent the instance of the Message Processor component.
     *         If this should not exist, then null is returned.
     */
    public JsEngineComponent getMessageProcessor();

   
    /**
     * Convenience method to return the Message Store component associated with
     * this instance of a Messaging Engine.
     * 
     * @return Object the instance of the Message Store component. If this should
     *         not exist, then null is returned.
     */
    public Object getMessageStore();

    /**
     * Returns the runtime configuration object proxy for the SIBus WCCM object
     * which represents the bus in which this Messaging Engine resides.
     * 
     * @return the runtime configuration object proxy for the SIBus WCCM object
     *         which represents the bus in which this Messaging Engine resides.
     */
    public LWMConfig getBus();

    /**
     * Returns the type of message store being used by this messaging engine.
     * 
     * @return int MESSAGE_STORE_TYPE
     */
    public int getMessageStoreType();

    /** A type constant representing a file store for messages */
    public static final int MESSAGE_STORE_TYPE_FILESTORE = 0;
    /** A type constant representing a data store for messages */
    public static final int MESSAGE_STORE_TYPE_DATASTORE = 1;

    /**
     * Returns whether a SIBDatastore WCCM object has been configured on this
     * Messaging Engine.
     * 
     * @return boolean
     */
    public boolean datastoreExists();

 

    /**
     * Returns whether a SIBFilestore WCCM object has been configured on this
     * Messaging Engine.
     * 
     * @return boolean
     */
    public boolean filestoreExists();

    /**
     * Returns the runtime configuration object proxy for the SIBFilestore WCCM
     * object, if one has been configured. Use the filestoreExists() method to
     * determine whether this Messaging Engine has a SIBFilestore prior to an
     * invocation of this method.
     * 
     * @return JsEObject the SIBFilestore object
     */
    public LWMConfig getFilestore();

    /**
     * Returns the runtime configuration object proxy for a named SIBMQLink WCCM
     * object, if one exists. If an object with the specified name does not exist,
     * then null is returned.
     * 
     * @param name
     * @return the runtime configuration object proxy for a named SIBMQLink WCCM
     *         object, if one exists.
     */
    public LWMConfig getMQLink(String name);

    /**
     * Returns the JsEngineComponent for a named MQ Link instance if one exists.
     * If an object with the specified name does not exist, then null is returned.
     * 
     * @param name
     *            The name of required instance
     * @return The JsEngineComponent instance for the required name
     */
    public JsEngineComponent getMQLinkEngineComponent(String name);

    
    /**
     * Load all destinations localized at this Messaging Engine into the runtime
     * Message Processor component.
     */
    public void loadLocalizations();

    public ControllableRegistrationService getMBeanFactory();

    /**
     * Read the WCCM configuration repository and return a
     * BaseDestinationDefinition containing the configuration for the specified
     * destination.
     * 
     * Any resolving of dynamic attributes of the BaseDestinationDefinition, such
     * as the Exception Destination, is performed.
     * 
     * @param busName
     *            the name of the bus on which the destination resides. This can be
     *            the current bus, as denoted by null, an empty string or the
     *            current bus name. For a foreign destination, a name other than the
     *            current (i.e. local) bus must be specified.
     * @param name
     *            the name, or identifier, of the destination
     * @return BaseDestinationDefinition
     * @throws SIBExceptionBase
     * @throws SIBExceptionDestinationNotFound
     */
    public BaseDestinationDefinition getSIBDestination(String busName, String name) throws SIBExceptionBase, SIBExceptionDestinationNotFound;

    /**
     * Read the WCCM configuration repository and return a
     * BaseDestinationDefinition containing the configuration for the specified
     * destination.
     * 
     * Any resolving of dynamic attributes of the BaseDestinationDefinition, such
     * as the Exception Destination, is performed.
     * 
     * @param busName
     *            the name of the bus on which the destination resides. This can be
     *            the current bus, as denoted by null, an empty string or the
     *            current bus name. For a foreign destination, a name other than the
     *            current (i.e. local) bus must be specified.
     * @param uuid
     *            the uuid, of the destination
     * @return BaseDestinationDefinition
     * @throws SIBExceptionBase
     * @throws SIBExceptionDestinationNotFound
     */
    public BaseDestinationDefinition getSIBDestinationByUuid(String busName, String uuid) throws SIBExceptionBase, SIBExceptionDestinationNotFound;

    /**
     * Read the WCCM configuration repository and update a
     * BaseDestinationDefinition containing the configuration for the specified
     * destination.
     * 
     * Any resolving of dynamic attributes of the BaseDestinationDefinition, such
     * as the Exception Destination, is performed.
     * 
     * @param busName
     *            the name of the bus on which the destination resides. This can be
     *            the current bus, as denoted by null, an empty string or the
     *            current bus name. For a foreign destination, a name other than the
     *            current (i.e. local) bus must be specified.
     * @param name
     *            the name, or identifier, of the destination
     * @param dd
     *            the DestinationDefinition to use to return the configuration
     * @throws SIBExceptionBase
     * @throws SIBExceptionDestinationNotFound
     */
    public void getSIBDestination(String busName, String name, DestinationDefinition dd) throws SIBExceptionBase, SIBExceptionDestinationNotFound;

    /**
     * Return a set of UUIDs for the Messaging Engines which localize a specified
     * destination.
     * 
     * @param busName
     * @param uuid
     * @return a set of UUIDs for the Messaging Engines which localize a specified
     *         destination.
     * @throws SIBExceptionBase
     * @throws SIBExceptionDestinationNotFound
     */
    public Set getSIBDestinationLocalitySet(String busName, String uuid) throws SIBExceptionBase, SIBExceptionDestinationNotFound;

    /**
     * Return a set of UUIDs for the Messaging Engines which localize a specified
     * destination.
     * 
     * @param busName
     * @param uuid
     * @param newCache
     * @return a set of UUIDs for the Messaging Engines which localize a specified
     *         destination.
     * @throws SIBExceptionBase
     * @throws SIBExceptionDestinationNotFound
     */
    public Set getSIBDestinationLocalitySet(String busName, String uuid, boolean newCache) throws SIBExceptionBase, SIBExceptionDestinationNotFound;

   

    /**
     * Allows a caller to query whether Event Notifications are enabled.
     * 
     * @return True if event notification is enabled, false otherwise.
     */
    public boolean isEventNotificationEnabled();

    /**
     * Return a set containing the UUIDs of all SIBMQServerBusMember instances
     * that this messaging engine knows about.
     * 
     * @return a set containing the UUIDs of all SIBMQServerBusMember instances
     *         that this messaging engine knows about.
     */

    public void setMEUUID(SIBUuid8 uuid);
    
    public long getMEThreshold();


}
