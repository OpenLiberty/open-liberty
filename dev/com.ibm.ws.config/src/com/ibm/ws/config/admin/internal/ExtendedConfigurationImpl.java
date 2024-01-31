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

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.admin.ConfigurationDictionary;
import com.ibm.ws.config.admin.ExtendedConfiguration;

//@formatter:off
/**
 * Extended configuration implementation.
 *
 * Extended configuration is a subtype of {@link org.osgi.service.cm.Configuration}.
 * This implementation is of both that and additions made by {@link ExtendedConfiguration}.
 */
class ExtendedConfigurationImpl implements ExtendedConfiguration {
    private static final TraceComponent tc =
                    Tr.register(ExtendedConfigurationImpl.class,
                                ConfigAdminConstants.TR_GROUP, ConfigAdminConstants.NLS_PROPS);

    public ExtendedConfigurationImpl(ConfigAdminServiceFactory caFactory,
                                     String bundleLocation,
                                     String factoryPid, String pid,
                                     Dictionary<String, Object> props,
                                     Set<ConfigID> references,
                                     Set<String> uniques) {
        this.caFactory = caFactory;

        // Record a bundle location.  The bundle location
        // can be persisted.  The actual bundle is set later.
        this.bundleLocation = bundleLocation;

        // Identity ...  PID is required.
        this.factoryPid = factoryPid;
        this.pid = pid;
        this.hashCode = pid.hashCode();

        // Data, and data links:
        //   'properties' are the actual configuration values.
        //   'references' are external references.
        //   'uniqueVariables' are unique variables which are defined
        //     by the configuration.

        this.properties = this.createProperties(props);
        this.references = references;
        this.uniqueVariables = uniques;

        // Link the configuration in to the administrative service.
        // This makes the configuration 'live' to the service.
        // The service keeps track of configuration PIDs, and tracks
        // external references.

        this.addPidMapping();
        this.addReferences();
    }

    /**
     * Create properties from a dictionary.
     *
     * Not static: Creation of the properties scrapes the factory PID,
     * PID, and overrides values and places them into the new properties.
     * (This overwrites any values provided by the dictionary.)  The
     * values are obtained from the configuration.
     *
     * @param d A dictionary supplying values.
     *
     * @return A new configuration dictionary.
     */
    private ConfigurationDictionary createProperties(Dictionary<String, ?> d) {
        if ( d == null ) {
            // TODO: Is this possible?  In this case, the factory PID,
            // PID, and overrides values are not set on the new properties.
            return null;
        }

        ConfigurationDictionary props = new ConfigurationDictionary();

        Enumeration<String> keys = d.keys();
        while ( keys.hasMoreElements() ) {
            String key = keys.nextElement();
            Object value = d.get(key);
            props.put( key, copy(value) );
        }

        putLocalValues(props);

        return props;
    }

    /**
     * Copy a value for use within the properties of this
     * configuration.
     *
     * Unless the value is an array or a collection, return the
     * value.  Otherwise, answer a shallow copy of the value.
     *
     * All collection valued values are copied as vectors.
     *
     * @param value A value which may be copied.
     *
     * @return Either, a copy of the value, or the value itself.
     */
    @Trivial
    private static final Object copy(Object value) {
        if ( value.getClass().isArray() ) {
            return copyArray(value);
        } else if (value instanceof Collection) {
            return copyVector(value);
        } else {
            return value;
        }
    }

    /**
     * Duplicate an array valued object.
     *
     * @param value An array value which is to be copied.
     *
     * @return A shallow copy of the array value.
     */
    @Trivial
    private static final Object copyArray(Object value) {
        int arrayLength = Array.getLength(value);
        Object valueCopy = Array.newInstance( value.getClass().getComponentType(), arrayLength );
        System.arraycopy(value, 0, valueCopy, 0, arrayLength);
        return valueCopy;
    }

    /**
     * Create a vector from a collection valued object.
     *
     * @param value A collection value which is to be copied
     *     as a vector.
     * @return A vector containing the values of the collection
     *     value.
     */
    @Trivial
    private static final Object copyVector(Object value) {
        return new Vector<Object>((Collection<?>) value);
    }

    //

    private final int hashCode;

    /**
     * Override: Extended configuration equality is based on
     * their PID.
     *
     * @return True or false telling if this configuration is
     *     equal to another object.
     */
    @Override
    @Trivial
    public boolean equals(Object o) {
        if ( o == null ) {
            return false;
        } else if ( o == this ) {
            return true;
        } else if ( !(o instanceof Configuration) ) {
            return false;
        } else {
            String oPid = ((Configuration) o).getPid();
            return this.pid.equals(oPid);
        }
    }

    /**
     * Override: Extended configuration hash codes are based on
     * their PID.
     *
     * @return The hash code of this extended configuration.
     */
    @Override
    @Trivial
    public int hashCode() {
        return hashCode;
    }

    @Override
    @Trivial
    public String toString() {
        return this.getClass().getSimpleName()
               + "[pid=" + pid
               + ",factoryPid=" + factoryPid
               + ",boundBundle=" + boundBundle
               + ",bundleLocation=" + bundleLocation
               + "]";
    }

    // Locking ...

    private final ReentrantLock lock = new ReentrantLock();

    @Override
    @Trivial
    public void lock() {
        lock.lock();
    }

    @Override
    @Trivial
    public void unlock() {
        if ( !lock.isHeldByCurrentThread() ) {
            throw new IllegalStateException("Thread not lock owner");
        }
        lock.unlock();
    }

    private final void guard(Runnable action) {
        lock();
        try {
            action.run();
        } finally {
            unlock();
        }
    }

    public static interface FailableSupplier<T, E extends Exception> {
        T get() throws E;
    }

    private final <T> T guard(Supplier<T> supplier) {
        lock();
        try {
            return supplier.get();
        } finally {
            unlock();
        }
    }

    private final <T, E extends Exception> T guardFailable(FailableSupplier<T, E> supplier) throws E {
        lock();
        try {
            return supplier.get();
        } finally {
            unlock();
        }
    }

    @Trivial
    protected void checkLocked() {
        if ( !lock.isHeldByCurrentThread() ) {
            throw new IllegalStateException("Thread not lock owner");
        }
    }

    // Bundle APIs ...

    private String bundleLocation;
    private Bundle boundBundle;

    @Override
    @Trivial
    public String getBundleLocation() {
        return getBundleLocation(CHECK_PERMISSIONS);
    }

    protected void checkBinding(Bundle bundle) {
        guard( () -> {
            exceptionIfDeleted();

            String currentLocation;
            if ( bundleLocation != null ) {
                currentLocation = bundleLocation;
            } else if ( boundBundle != null ) {
                currentLocation = boundBundle.getLocation();
            } else {
                currentLocation = null;
            }

            if ( (currentLocation != null) && !currentLocation.equals(bundle.getLocation()) ) {
                checkPermission();
            }

            if ( boundBundle == null ) {
                Tr.warning(tc, "warning.binding.config", pid, bundle.getSymbolicName());
                boundBundle = bundle;
            }
        });
    }

    /**
     * Answer the bundle location.
     *
     * Throw an exception if the configuration is deleted, or if
     * the caller does not have permissions to access the bundle.
     *
     * @param checkPermission Control parameter: Tell if permissions
     *     are to be checked.
     *
     * @return The bundle location.
     */
    protected String getBundleLocation(boolean checkPermission) {
        return guard( () -> {
            exceptionIfDeleted();
            if ( checkPermission ) {
                checkPermission();
            }
            if ( bundleLocation != null ) {
                return bundleLocation;
            }
            if ( boundBundle != null ) {
                return boundBundle.getLocation();
            }
            return null;
        } );
    }

    /**
     * Set the bundle location.
     *
     * Overwrite any previously bundle location, and clear the bundle.
     *
     * Throw an exception if the configuration is deleted, or
     * if the process does not have permission to update the
     * configuration.
     *
     * @param bundleLocation The new bundle location.
     */
    @Override
    @Trivial
    public void setBundleLocation(String bundleLocation) {
        setBundleLocation(bundleLocation, CHECK_PERMISSIONS);
    }

    /**
     * Control parameter: Should permissions be checked when
     * setting the bundle location?
     */
    private static final boolean CHECK_PERMISSIONS = true;

    /**
     * Set the bundle location.
     *
     * Overwrite any previously bundle location, and clear the bundle.
     *
     * Throw an exception if the configuration is deleted.  Conditionally
     * throw an exception if the process does not have permission to
     * update the configuration.
     *
     * @param bundleLocation The new bundle location.
     * @param checkPerm Control parameter: Should permissions be checked.
     */
    private void setBundleLocation(String bundleLocation, boolean checkPerm) {
        guard( () -> {
            exceptionIfDeleted();
            if ( checkPerm ) {
                caFactory.checkConfigurationPermission();
            }
            this.bundleLocation = bundleLocation;
            boundBundle = null;
        } );
    }

    /**
     * Tell if this configuration is not bound.  That is,
     * does this bundle not have an assigned bundle.
     *
     * @return True or false telling if this bundle does
     *     not have a set bundle.
     */
    @Trivial
    protected boolean isUnbound() {
        return ( boundBundle == null );
    }

    /**
     * Set the bundle.
     *
     * Do nothing if the bundle is already assigned, or if the bundle
     * location is set and does not match the new bundle location.
     *
     * @param bundle The bundle which is to be set.
     *
     * @return True or false telling if the bundle assignment
     *     matches the new bundle.  Usually, this is means that an
     *     an actual assignment was performed, but can also mean that
     *     the prior bundle assignment is the same as the new
     *     assignment.
     */
    @Trivial
    protected boolean bind(Bundle bundle) {
        return guard( () -> {
            // Note: No deletion or permissions check!
            if ( (boundBundle == null) &&
                 ((bundleLocation == null) || bundleLocation.equals(bundle.getLocation()))) {
                boundBundle = bundle;
            }
            return (boundBundle == bundle);
        } );
    }

    /**
     * Clear the bundle assignment.
     *
     * Do nothing if the assigned bundle does not match the bundle parameter.
     *
     * @param bundle The expected assigned bundle.
     */
    @Trivial
    protected void unbind(Bundle bundle) {
        guard( () -> {
            // Note: No deletion or permissions check!
            if ( boundBundle == bundle ) {
                boundBundle = null;
            }
        } );
    }

    // ID link for the configuration services factory ...

    /**
     * This configuration's 'full' configuration ID.
     * Needed by the a administrative services factory
     * to handle references and ID mappings.
     */
    private ConfigID configId;

    /**
     * Answer a more complete configuration ID for this configuration.
     *
     * If possible, answer the assigned full ID.  Otherwise, if a factory PID
     * is null, available, answer a new full ID using the PID.
     *
     * Otherwise, look for a an ID in the receiver's properties,
     * if possible, answer a new full ID using the ID property.
     *
     * Otherwise, answer a new full ID using the factory PID.
     *
     * If a new full ID is created, do not assign it into the receiver.
     * {@link #setFullId(ConfigID)} must be used to assign the full ID.
     *
     * @return The full ID of this configuration.
     */
    @Override
    public ConfigID getFullId() {
        if ( configId != null )
            return configId;

        // TODO: This precedence ordering is strange:
        //       The interaction with 'addPidMapping' and
        //       'setProperties' is strange.

        // Do NOT set the configuration ID here.  That happens
        // when registering the configuration.  See:
        // 'addPidMapping()', which calls 'getFullId()',
        // then calls 'ConfigAdminServiceFactory.registerConfiguration()'
        // with the full ID.  'registerConfiguration' calls back to
        // 'setFullId'.
        //
        // Calls that process references also call 'getFullId'.  However,
        // the full ID is very probably set before any of those calls.
        // There should not be a problem of recreating the full ID many
        // times.

        if ( factoryPid == null ) {
            return new ConfigID(pid);
        }

        String idText = (String) properties.get(ConfigAdminConstants.CFG_CONFIG_INSTANCE_ID);
        if ( idText != null ) {
            return ConfigID.fromProperty(idText);
        }

        return new ConfigID(factoryPid, null);
    }

    @Override
    public void setFullId(ConfigID id) {
        this.configId = id;
    }

    // Configuration service factory APIs ...

    /** Global administrative service factory. */
    private final ConfigAdminServiceFactory caFactory;

    @Trivial
    protected void checkPermission() {
        caFactory.checkConfigurationPermission();
    }

    private void notify(int event, Collection<Future<?>> futures) {
        Future<?> caFuture;
        if ( event == ConfigurationEvent.CM_DELETED ) {
            caFuture = caFactory.notifyConfigurationDeleted(this, (factoryPid != null));
        } else {
            caFuture = caFactory.notifyConfigurationUpdated(this, (factoryPid != null));
        }

        Future<?> configFuture = caFactory.dispatchEvent(event, factoryPid, pid);

        if ( futures != null ) {
            if ( caFuture != null ) {
                futures.add(caFuture);
            }
            if ( configFuture != null ) {
                futures.add(configFuture);
            }
        }
    }

    /*
     * Tell the configuration administration service that this configuration
     * has been updated.
     *
     * Throw an exception if deleted.
     *
     * Save the configuration, then perform the notification.
     *
     * @throws IOException Preserved API exception.  Never thrown
     *     by this implementation.
     */
    @Override
    public void update() throws IOException {
        guard( () -> {
            exceptionIfDeleted();
            caFactory.getConfigurationStore().save();
            caFactory.notifyConfigurationUpdated(this, (factoryPid != null));
        } );
    }

    /**
     * Administrative service helper: Remove references made
     * by this configuration from the administrative services.
     *
     * Do nothing if this configuration has null properties
     * or has null references.  There is no work to be done
     * if either is null.
     */
    private void removeReferences() {
        // No properties means there can be no external
        // references.  No references means there are no
        // external references.
        if ( (properties == null) || (references == null) ) {
            return;
        }
        caFactory.removeReferences( references, getFullId() );
    }

    /**
     * Administrative service helper: Add references made by
     * this configuration to the administrative services.
     *
     * Do nothing if this configuration has null properties
     * or has null references.  There is no work to be done
     * if either is null.
     */
    private void addReferences() {
        // No properties means there can be no external
        // references.  No references means there are no
        // external references.
        if ( (properties == null) || (references == null) ) {
            return;
        }
        caFactory.addReferences( references, getFullId() );
    }

    /**
     * Administrative service helper: Map this configuration to
     * its ID.  Do so only if this configuration has a factory PID
     * and only if properties are set.
     */
    private void addPidMapping() {
        // Don't bother registering if the properties are not set.
        // That means that having unset properties is NOT the same
        // as having empty properties.
        if ( properties == null ) {
            return;
        }
        // Registration is not possible if there is no factory PID
        // or no administration services factory.
        if ( (factoryPid == null) || (caFactory == null) ) {
            return;
        }
        caFactory.registerConfiguration( getFullId(), this );
        // This calls back to 'setFullId'.
    }

    /**
     * Administrative service helper: Unmap this configuration from
     * its ID.  Do so only if this configuration has a factory PID.
     */
    private void removePidMapping() {
        // Don't bother de-registering if the properties are not set.
        // That means that having unset properties is NOT the same
        // as having empty properties.
        if ( properties == null ) {
            return;
        }
        // Registration is not possible if there is no factory PID
        // or no administration services factory.
        if ( (factoryPid == null) || (caFactory == null) ) {
            return;
        }
        caFactory.unregisterConfiguration( getFullId() );
    }

    //

    private final String factoryPid;
    private final String pid;

    @Override
    @Trivial
    public String getFactoryPid() {
        return getFactoryPid(CHECK_DELETED);
    }

    /**
     * Answer the factory PID of this configuration.
     *
     * Conditionally, throw an exception if deleted.
     *
     * @param checkDeleted Control parameter: Tell if
     *     an exception is to be thrown if the receiver
     *     is deleted.
     * @return The factory PID of this configuration.
     */
    public String getFactoryPid(boolean checkDeleted) {
        return guard( () -> {
            if ( checkDeleted ) {
                exceptionIfDeleted();
            }
            return factoryPid;
        } );
    }

    @Override
    @Trivial
    public String getPid() {
        return getPid(CHECK_DELETED);
    }

    /**
     * Answer the PID of this configuration.
     *
     * Conditionally, throw an exception if deleted.
     *
     * @param checkDeleted Control parameter: Tell if
     *     an exception is to be thrown if the receiver
     *     is deleted.
     * @return The PID of this configuration.
     */
    public String getPid(boolean checkDeleted) {
        return guard( () -> {
            if ( checkDeleted ) {
                exceptionIfDeleted();
            }
            return pid;
        } );
    }

    //

    private ConfigurationDictionary properties;

    /** Factory PID property name: The factory PID is linked to a property value. */
    private static final String FACTORY_PID_PROPERTY_NAME = ConfigurationAdmin.SERVICE_FACTORYPID;

    /** PID property name: The PID is linked to a property value. */
    private static final String PID_PROPERTY_NAME = Constants.SERVICE_PID;

    /** Overrides property name: The overrides value is linked to a property value. */
    private static final String IN_OVERRIDES_PROPERTY_NAME = "config.overrides";

    /**
     * Put linked local values into the properties.
     *
     * @param props The properties which are to receive local
     *     values.
     */
    private void putLocalValues(ConfigurationDictionary props) {
        if ( factoryPid != null ) {
            props.put(FACTORY_PID_PROPERTY_NAME, factoryPid);
        }
        props.put(PID_PROPERTY_NAME, pid);

        if ( inOverridesFile ) {
            props.put(IN_OVERRIDES_PROPERTY_NAME, "true");
        }
    }

    /**
     * Tell if this configuration was read from the root configuration
     * resource, server.xml or from an overrides file.
     *
     * 'inOverridesFile' is linked to the properties.
     */
    private boolean inOverridesFile;

    /**
     * Answer a single property value of this configuration.
     *
     * Answer null if there are no properties, or if the
     * property is not found.
     *
     * Do NOT copy the property value.  (This is inconsistent with
     * {@link #getProperties()}, which copies vector and array
     * property values.
     *
     * @param key The key to locate.
     *
     * @return The value associated with the key.
     */
    @Override
    public Object getProperty(String key) {
        // TODO: clone the value
        // TODO: Check permissions?
        return guard( () -> {
            exceptionIfDeleted();
            return ( (properties == null) ? null : properties.get(key) );
        } );
    }

    /**
     * Answer copy of the properties of this configuration.
     *
     * Answer null if the properties are null.
     *
     * Throw an exception if the configuration is deleted.
     *
     * The copy has one level of depth: Values which are
     * vectors or arrays are duplicated.  Other values are
     * not duplicated.  See {@link ConfigurationDictionary#copy()}.
     *
     * @return A copy of the properties of this configuration.
     */
    @Override
    public Dictionary<String, Object> getProperties() {
        return guard( () -> {
            exceptionIfDeleted();
            return ( (properties == null) ? null : properties.copy() );
        } );
    }

    // TODO: This is for config plugins, which we dont support...
    //
    // However, if this throws an illegal state exception then we get this exception:
    //
    // [11/19/18 14:55:51:340 EST] 00000024 LogService-13-com.ibm.ws.org.apache.felix.scr E CWWKE0701E: bundle
    // com.ibm.ws.org.apache.felix.scr:1.0.23.201811071104 (13)Error while loading components of bundle com.ibm.ws.event:1.0.23.201811021519 (15)
    // Bundle:com.ibm.ws.org.apache.felix.scr(id=13) java.lang.IllegalStateException: getProcessedProperties(ServiceReference<?> reference) in
    // ExtendedConfiguraitonImpl.java has not been implemented.
    // at com.ibm.ws.config.admin.internal.ExtendedConfigurationImpl.getProcessedProperties(ExtendedConfigurationImpl.java:789)
    // at org.apache.felix.scr.impl.manager.RegionConfigurationSupport.configureComponentHolder(RegionConfigurationSupport.java:211)
    //
    // So for now, we'll just return a copy of the current config which lets the server start up.

    @Override
    public Dictionary<String, Object> getProcessedProperties(ServiceReference<?> reference) {
        return getProperties();
    }

    /**
     * Answer the properties of this configuration.
     *
     * Do not copy the properties.
     *
     * Throw an exception if the configuration is deleted.
     *
     * @return The raw properties of this configuration.
     */
    @Override
    @Trivial
    public Dictionary<String, Object> getReadOnlyProperties() {
        return guard( () -> {
            exceptionIfDeleted();
            return properties;
        } );
    }

    /**
     * Tell if a filter matches specified properties.
     *
     * @param filter The filter used to test the properties.
     *
     * @return True or false telling if the filter matches the properties.
     *    Answer false if the properties are null.
     */
    @Trivial
    protected boolean matchesFilter(Filter filter) {
        // TODO: Add locking?
        return ( (properties != null) && properties.matches(filter) );
    }

    @Override
    @Trivial
    public void setInOverridesFile(boolean inOverridesFile) {
        guard( () -> {
            this.inOverridesFile = inOverridesFile;

            if ( properties != null ) {
                if ( inOverridesFile ) {
                    properties.put(IN_OVERRIDES_PROPERTY_NAME, "true");
                } else {
                    properties.remove(IN_OVERRIDES_PROPERTY_NAME);
                }
            }
        } );
    }

    @Override
    @Trivial
    public boolean isInOverridesFile() {
        return guard( () -> {
            exceptionIfDeleted();
            if ( properties != null ) {
                return ( properties.get(IN_OVERRIDES_PROPERTY_NAME) != null );
            }
            return false;
        } );
    }

    //

    private final Set<Configuration.ConfigurationAttribute> attributes =
        new HashSet<Configuration.ConfigurationAttribute>();

    /**
     * Answer the raw attributes of this configuration.
     *
     * Throw an exception if deleted.
     *
     * Do not copy the attributes.
     *
     * @return The raw attributes of this configuration.
     */
    // TODO: Check IMPL of attributes...
    @Override
    public Set<ConfigurationAttribute> getAttributes() {
        // TODO: security permissions check ???
        exceptionIfDeleted();
        return attributes;
    }

    /**
     * Add attributes to this configuration.  Do not copy the
     * attributes.
     *
     * Throw an exception if deleted.
     *
     * This operation is not thread safe: Concurrent calls can mix
     * attributes. Safety of the primitive add may or may not be
     * thread safe, depending on the attributes implementation.
     * Attributes retrieved by {@link #getAttributes()} will be
     * asynchronously updated by attribute addition.
     *
     * @param attrs Attributes which are to be added.
     *
     * @throws IOException Preserved API exception.  Never thrown
     *     by this implementation.
     */
    @Override
    public void addAttributes(Configuration.ConfigurationAttribute... attrs) throws IOException {
        // TODO: security permissions check ???

        exceptionIfDeleted();

        for ( Configuration.ConfigurationAttribute attr : attrs ) {
            attributes.add(attr);
        }
    }

    /**
     * Remove attributes from this configuration.
     *
     * Throw an exception if deleted.
     *
     * This operation is not thread safe: Concurrent calls can mix
     * attributes. Safety of the primitive removal or may not be
     * thread safe, depending on the attributes implementation.
     * Attributes retrieved by {@link #getAttributes()} will be
     * asynchronously updated by attribute removals.
     *
     * @param attrs Attributes which are to be Removed.
     *
     * @throws IOException Preserved API exception.  Never thrown
     *     by this implementation.
     */
    @Override
    public void removeAttributes(Configuration.ConfigurationAttribute... attrs) throws IOException {
        // TODO: security permissions check ???

        exceptionIfDeleted();

        for ( int i = 0; i < attrs.length; i++ ) {
            attributes.remove(attrs[i]);
        }
    }

    // References ...

    /** External configuration references. */
    private Set<ConfigID> references;

    @Override
    @Trivial
    public Set<ConfigID> getReferences() {
        return guard( () -> references );
    }

    //

    private Set<String> uniqueVariables = Collections.emptySet();

    @Override
    @Trivial
    public Set<String> getUniqueVariables() {
        return guard( () -> ( (uniqueVariables == null) ? Collections.emptySet() : uniqueVariables ) );
    }

    // Deletion state ...

    /**
     * Control parameter: Used by methods which conditionally check
     * the deletion state, {@link #getFactoryPid()} and {@link #getPid()}.
     */
    public static final boolean CHECK_DELETED = true;

    /** Deletion state flag. */
    private boolean deleted;

    /**
     * Checks for deleted flag and throws an IllegalStateException if deleted.
     */
    @Trivial
    private void exceptionIfDeleted() {
        if ( deleted ) {
            throw new IllegalStateException("Configuration pid " + pid + " was deleted.");
        }
    }

    @Override
    @Trivial
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * Delete this configuration.  Do perform notifications.
     *
     * See {@link #delete(boolean)}.
     *
     * @throws IOException Thrown if the delete fails.  Never
     *     thrown by this implementation.
     */
    @Override
    @Trivial
    public void delete() throws IOException {
        delete(DO_NOTIFY);
    }

    /** Control parameter: Are notifications to be fired? */
    public boolean DO_NOTIFY = true;

    /**
     * Attempt to delete the receiver.
     *
     * Throw an exception if already deleted.
     *
     * Otherwise, conditionally fire a delete notification,
     * then update PID mappings and update references.
     *
     * Finally, remove the configuration from the configuration
     * store.
     *
     * @param fireNotifications Control parameter: Is a
     *     delete notification to be fired?
     */
    @Override
    public void delete(boolean fireNotifications) {
        guard( () -> {
            exceptionIfDeleted();
            deleted = true;

            if ( fireNotifications ) {
                fireConfigurationDeleted(null);
            }

            removePidMapping();
            removeReferences();
        } );

        caFactory.getConfigurationStore().removeConfiguration(pid);
    }

    /**
     * Fire notifications for a configuration which was just deleted.
     *
     * Up to two futures are created and stored in the futures collection
     * parameter.
     *
     * @param futures Storage for the generated notification futures.
     */
    @Override
    public void fireConfigurationDeleted(Collection<Future<?>> futures) {
        notify(ConfigurationEvent.CM_DELETED, futures);
    }

    // Change tracking ...

    /** Count of updates which have been made. */
    private long changeCount;

    /**
     * Control parameter: Tell if updates were made since the
     * last update notification.
     */
    private boolean haveChanges;

    @Override
    @Trivial
    public long getChangeCount() {
        return guard( () -> changeCount );
    }

    /**
     * Record that the configuration has been updated.  This
     * unblocks notification.
     */
    private void recordChange() {
        changeCount++;
        haveChanges = true;
    }

    /**
     * Clear any recorded changes.  Tell if any changes were
     * cleared.
     *
     * This is usually done when notification is requested: Notification
     * is not allowed to proceed unless changes were present.
     *
     * @return True or false telling if any changes were cleared.
     */
    private boolean clearChanges() {
        if ( !haveChanges ) {
            return false;
        } else {
            haveChanges = false;
            return true;
        }
    }

    //

    /**
     * Conditionally update the properties of this configuration.
     *
     * Throw an exception if deleted.
     *
     * If the properties are different than the current properties,
     * update the properties and return true.  Otherwise, do nothing
     * and return false.
     *
     * @param oProperties Properties which are to be set to the receiver.
     *
     * @return True or false telling if the properties are different
     *     than the current properties.
     *
     * @throws IOException
     */
    @Override
    public boolean updateIfDifferent(Dictionary<String, ?> oProperties) throws IOException {
        return guardFailable( () -> {
            // TODO: Check permissions?

            // Need this redundant deletion check: 'update' does the check
            // again, but won't be reached if the properties are the same.
            exceptionIfDeleted();

            if ( equalProperties(properties, oProperties) ) {
                return false;
            }

            update(oProperties); // throws IOException
            return true;
        } );
    }

    /*
     * Unconditionally update the properties.  Fire an update event.
     *
     * Throw an exception if deleted.
     *
     * @param oProperties New property values.
     *
     * @throws IOException Preserved API exception.  Never thrown
     *     by this implementation.
     */
    @Override
    public void update(Dictionary<String, ?> oProperties) throws IOException {
        // TODO: Check permissions?
        // 'doUpdateProperties' does a deletion check.

        guard( () -> {
            doUpdateProperties(oProperties);
            fireConfigurationUpdated(null);
        } );
    }

    /*
     * Unconditionally update the properties.  Do not fire an update event.
     * Do not test if the properties are different.
     *
     * Throw an exception if deleted.
     *
     * @param oProperties New property values.
     *
     * @throws IOException Preserved API exception.  Never thrown
     *     by this implementation.
     */
    @Override
    public void updateProperties(Dictionary<String, Object> oProperties) throws IOException {
        // Without other guards, separating updating the properties and
        // sending configuration events can result in missing and duplicate
        // update events even if every update is eventually associated with an event.

        // TODO: Check permissions?
        // 'doUpdateProperties' does a deletion check.

        guard( () -> doUpdateProperties(oProperties) );
    }

    /**
     * Update this configuration.
     *
     * Thrown an exception if this configuration is deleted.
     *
     * Replace the properties with the provided properties, but, keep
     * the the current factory PID, the current PID, and overrides value,
     * and set this into the assigned properties, overwriting any provided
     * values.
     *
     * The properties are copied; no writes are performed on the supplied
     * properties.
     *
     * The configuration is stored.
     *
     * @param oProperties Properties to set into the configuration.
     */
    private void doUpdateProperties(Dictionary<String, ?> oProperties) {
        exceptionIfDeleted();

        properties = createProperties(oProperties);

        // TODO: Should the PID mapping be updated?
        // The PID cannot be changed.
        addPidMapping();

        caFactory.getConfigurationStore().save();

        recordChange();
    }

    /**
     * Updates ConfigurationAdmin's cache with current config properties.
     * If replaceProp is set to true, current config properties is replace with
     * the given properties before caching
     * and the internal pid-to-config table is updated to reflect the new config
     * properties.
     */
    @Override
    public void updateCache(Dictionary<String, Object> oProperties,
                            Set<ConfigID> oReferences,
                            Set<String> oUniqueVariables) throws IOException {
        guard( () -> {
            // The PID is not un-registered then re-registered.
            // The PID set in the configuration has precedence.
            removeReferences();

            properties = createProperties(oProperties);
            references = oReferences;
            uniqueVariables = oUniqueVariables;

            caFactory.getConfigurationStore().save();

            addReferences();

            recordChange();
        } );
    }

    /**
     * Fire a configuration update event.
     *
     * Thread safe if performed locally.  Unsafe if invoked externally.
     *
     * However, unless enabled by a call to {@link #doUpdateProperties(Dictionary)},
     * nothing will be done, as the call is required to set the enabling 'sendEvents'
     * value.
     *
     * @param futures Optional sink for futures created by the update.  There
     *     can be up to two.
     */
    @Override
    public void fireConfigurationUpdated(Collection<Future<?>> futures) {
        if ( !(guard( () -> clearChanges() )) ) {
            return;
        }
        notify(ConfigurationEvent.CM_UPDATED, futures);
    }

    /**
     * Compare two properties collections.
     *
     * Answer true or false telling if they have equal keys
     * and have equal values.
     *
     * @param props0 First properties to compare.
     * @param props1 Second properties to compare.
     *
     * @return True or false telling if the properties have the
     *    equal keys and equal values.
     */
    @SuppressWarnings("null")
    @Trivial
    private static boolean equalProperties(Dictionary<String, ?> props0,
                                           Dictionary<String, ?> props1) {

        boolean empty0 = ( (props0 == null) || props0.isEmpty() );
        boolean empty1 = ( (props1 == null) || props1.isEmpty() );

        if ( empty0 && empty1 ) {
            return true;
        } else if ( !empty0 || !empty1 ) {
            return false;
        } else if ( props0.size() != props1.size() ) {
            return false;
        }

        // TODO: Can property values be null?
        //       If so, should a null property be treated
        //       as if the property is not present?

        Enumeration<String> p0Keys = props0.keys();
        while ( p0Keys.hasMoreElements() ) {
            String p0Key = p0Keys.nextElement();

            Object p1Value = props1.get(p0Key);
            if ( p1Value == null ) { // Taking this to mean the property is not present.
                return false;
            } else {
                Object p0Value = props0.get(p0Key);
                // The null check should not be necessary.  Have it to
                // avoid an NPE in 'equalValues'.
                if ( (p0Value == null) || !equalValues(p0Value, p1Value) ) {
                    return false;
                }
            }
        }

        Enumeration<String> p1Keys = props1.keys();
        while ( p1Keys.hasMoreElements() ) {
            String p1Key = p1Keys.nextElement();

            Object p0Value = props1.get(p1Key);
            if ( p0Value == null ) { // Taking this to mean the property is not present.
                return false;
            }
            // If present, the value was compared in the first loop.
        }

        return true;
    }

    @Trivial
    private static boolean equalValues(Object v0, Object v1) {
        if ( (v0 instanceof String) && (v1 instanceof String) ) {
            return v0.equals(v1);
        } else if ( (v0 instanceof String[]) && (v1 instanceof String[]) ) {
            return Arrays.equals((String[]) v0, (String[]) v1);
        } else if ( (v0 instanceof Map) && (v1 instanceof Map) ) {
            return v0.equals(v1);
        } else {
            return v0.equals(v1);
        }
    }
}
//@formatter:on