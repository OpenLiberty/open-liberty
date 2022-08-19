/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.admin.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.admin.ConfigurationDictionary;
import com.ibm.ws.config.admin.ExtendedConfiguration;
import com.ibm.ws.config.admin.internal.ConfigurationStorageHelper.ConfigStorageConsumer;

/**
 * ConfigurationStore manages all active configurations along with persistence. The current
 * implementation uses a filestore and serialization of the configuration dictionaries to files
 * identified by their pid. Persistence details are in the constructor, saveConfiguration, and
 * deleteConfiguration and can be factored out separately if required.
 */
// @formatter:off
class ConfigurationStore {
    private static final TraceComponent tc =
        Tr.register(ConfigurationStore.class,
                    ConfigAdminConstants.TR_GROUP, ConfigAdminConstants.NLS_PROPS);

    /**
     * Factory method: Create a configuration store, with the store initial contents
     * read from the bundle specific {@link ConfigAdminConstants#CONFIG_PERSISTENT}
     * data file.
     *
     * @param caFactory The service factory for the store.
     * @param bc The context supplying the configuration persistence file.
     */
    public ConfigurationStore(ConfigAdminServiceFactory caFactory, BundleContext bc) {
        this( caFactory, bc.getDataFile(ConfigAdminConstants.CONFIG_PERSISTENT) );
    }

    /**
     * Factory method: Create a configuration store with the store contents read
     * from the specified configuration persistence file.
     *
     * A failure to read the specified configuration will result in an empty
     * initial store.  If the configuration file does not exist, no error occurs.
     * If the configuration file does exist, an exception occurs.  FFDC handles
     * this exception, and processing continues with no data read.
     *
     * @param caFactory The service factory for the store.
     * @param persistentConfig The configuration persistence file from which to
     *     read the initial configurations.
     */
    public ConfigurationStore(ConfigAdminServiceFactory caFactory, File persistentConfig) {
        this.caFactory = caFactory;
        this.persistentConfig = persistentConfig;
        this.configurations = load(persistentConfig);
    }

    /**
     * Service factory provided to configurations created by this store.
     *
     * Also used to schedule configuration saves.  See {@link #save()}.
     */
    private final ConfigAdminServiceFactory caFactory;

    /**
     * Create a new extended configuration instance.
     *
     * The new configuration has null attributes.
     *
     * @param location The location of the new configuration.
     * @param factoryPid The factory PID of the new configuration.  Null for singletons.
     * @param pid The PID of the new configuration.
     *
     * @return The new configuration instance.
     */
    private ExtendedConfigurationImpl createConfiguration(String location, String factoryPid, String pid) {
        return new ExtendedConfigurationImpl(caFactory, location, factoryPid, pid, null, null, null);
    }

    /**
     * Create a new extended configuration instance.
     *
     * This factory method is used to marshall deserialized configuration data.
     * See {@link #load(File)}.
     *
     * The table of attributes is expected to contain a PID, and to possibly
     * contain a factory PID.  See {@link Constants#SERVICE_PID} and
     * {@link ConfigurationAdmin#SERVICE_FACTORYPID}.
     *
     * @param location The location of the new configuration.
     * @param uniqueVariables The variables used by the configuration.
     * @param references The references within the configuration.
     * @param properties The table of attributes of the configuration.
     *
     * @return The new configuration.
     */
    public ExtendedConfigurationImpl createConfiguration(
        String location,
        Set<String> uniqueVariables,
        Set<ConfigID> references,
        ConfigurationDictionary properties) {

        String pid = (String) properties.get(Constants.SERVICE_PID);
        String factoryPid = (String) properties.get(ConfigurationAdmin.SERVICE_FACTORYPID);
        return new ExtendedConfigurationImpl(caFactory, location, factoryPid, pid, properties, references, uniqueVariables);
    }

    /**
     * Table of current configurations. Keys are PIDs. Values are
     * configurations.
     *
     * The configurations are initially read from data file
     * {@link ConfigAdminConstants#CONFIG_PERSISTENT}.
     */
    private final Map<String, ExtendedConfigurationImpl> configurations;

    /**
     * Monitor used to guard access to the configurations collection.
     */
    private final ReentrantReadWriteLock configurationsLock = new ReentrantReadWriteLock();

    private final void readLock() {
        configurationsLock.readLock().lock();
    }

    private final void readUnlock() {
        configurationsLock.readLock().unlock();
    }

    /**
     * Perform an operation on the configurations.  Allow concurrent reads.
     *
     * @param processor The processor used to operate on the configurations.
     *
     * @return The return value from the processor.  Usually, either, one of
     *     the configurations, or null.
     */
    private final ExtendedConfigurationImpl excludeWrites(
        Function<Map<String, ExtendedConfigurationImpl>, ExtendedConfigurationImpl> processor) {

        readLock();
        try {
            return processor.apply(configurations);
        } finally {
            readUnlock();
        }
    }

    /**
     * Obtain the current configurations.  Read these in a thread safe
     * manner.  The returned collection is independent of the underlying
     * storage.
     *
     * @return A list of the current configurations.
     */
    private final List<ExtendedConfigurationImpl> getConfigurations() {
        // Unfortunately, there is no convenient way to use 'excludeWrites'.
        readLock();
        try {
            return new ArrayList<>(configurations.values());
        } finally {
            readUnlock();
        }
    }

    public final void writeLock() {
        if (configurationsLock.getReadHoldCount() > 0) {
            // This is not allowed, as it would cause deadlock.
            // Fail fast instead of deadlocking.
            throw new IllegalMonitorStateException("Requesting upgrade to write lock.");
        }
        configurationsLock.writeLock().lock();
    }

    public final void writeUnlock() {
        configurationsLock.writeLock().unlock();
    }

    /**
     * Perform an operation on the configurations.  Allow no concurrent access.
     * (The operation is expected to sometimes update the configurations.)
     *
     * @param The processor used to operate on the configurations.
     *
     * @return The return value from the processor.  Usually, one of the configurations,
     *     which may be a newly created configuration, or null.
     */
    private final ExtendedConfigurationImpl excludeAll(
        Function<Map<String, ExtendedConfigurationImpl>, ExtendedConfigurationImpl> processor) {

        writeLock();
        try {
            return processor.apply(configurations);
        } finally {
            writeUnlock();
        }
    }

    /**
     * File used as a persistent store of the configurations.
     */
    private final File persistentConfig;

    /**
     * Monitor used when saving the configurations.
     *
     * Used to guard access to {@link #saveTask} and {@link #shutdown}.
     *
     * Saves are performed asynchronously: When performing a save, the
     * base save operation is performed through the update queue.
     *
     * Multiple concurrent saves are collapsed: A save which is requested
     * while another save is pending is ignored.  This is safe, since, the
     * save is performed with a read lock on the configurations.  The
     * duplicate save would rewrite the same configurations.
     *
     * During shutdown, any scheduled save is performed immediately, and
     * future saves are disallowed.
     */
    private final Object saveMonitor = new Object();

    /**
     * Scheduled save task. Set when saving the configuration. Used to
     * avoid multiple saves: A concurrent save request returns immediately.
     *
     * The save task is accessed using double-locking, and must be volatile.
     * See {@link #save()}.
     */
    private volatile Future<?> saveTask;

    /**
     * State parameter: Used to prevent new saves when shutting down services.
     *
     * Accessed using double-locking, and must be volatile.  See {@link #save()}.
     */
    private volatile boolean shutdown;

    /**
     * Simple integer counter type. Used for generating reference PIDs
     * for configuration elements.
     */
    public static class Counter {
        private int value;

        public Counter() {
            this.value = 0;
        }

        public int increment() {
            return value++;
        }
    }

    /**
     * A counter for PID names to avoid collisions.
     *
     * The map is not thread safe: Access must be externally synchronized.
     *
     * Keys are factory PIDs. Values are counters, which are used to generate
     * reference PIDs for configuration elements.
     *
     * Counters are
     */
    private final Map<String, Counter> configCount = new HashMap<>();

    /**
     * Generate a new count for a given factory PID.
     *
     * This operation is not thread safe: The caller,
     * {@link #createFactoryConfiguration(String, String)}, ensures
     * safety by including the invocation in a region that is guarded
     * by the configurations write lock.
     *
     * Each factory PID has its own counter.  That enables the configuration
     * admin to continue its main processing loops, which aggregate the
     * processing of configurations by factory PID, and which iterate across
     * the factory PIDs in an indeterminate order.
     *
     * The processing per factory PID orders the configuration data by sequence
     * number.  That ensures that minimally changed configurations obtain the
     * same PIDs for the same configuration elements.
     *
     * @param factoryPid The factory PID for which to generate a new count.
     *
     * @return The new count for the specified factory PID.
     */
    private long getCount(String factoryPid) {
        Counter counter = configCount.computeIfAbsent(factoryPid, (String useFactoryPid) -> new Counter());
        return counter.increment();
    }

    /**
     * Generate a new PID for a given factory PID.
     *
     * PIDs are generated using a a factory PID specific counter.
     *
     * The generated PID is guaranteed to be unique relative to the
     * current stored configurations. New PIDs are generated sequentially
     * until a unique PID is generated.
     *
     * This operation is not thread safe: The caller,
     * {@link #createFactoryConfiguration(String, String)}, ensures
     * safety by including the invocation in a region that is guarded
     * by the configurations write lock.
     *
     * @param factoryPid The factory PID for which to generate a new PID.
     *
     * @return The new PID.
     */
    private String generatePid(String factoryPid) {
        String pid;
        do {
            pid = factoryPid + "_" + getCount(factoryPid);
        } while (configurations.containsKey(pid));
        return pid;
    }

    //

    /**
     * Remove a specified configuration.  After removing the configuration, save it back to
     * the configuration store.
     *
     * In operation, this method removes the element, then schedules a save of the configuration.
     *
     * A save is scheduled even if no configuration was actually removed.
     *
     * No new save is scheduled if a save is already pending.
     *
     * @param pid The PID of the element which is to be removed.
     */
    public void removeConfiguration(String pid) {
        ExtendedConfigurationImpl removedConfiguration =
            excludeAll( (useConfigurations) -> useConfigurations.remove(pid) );
        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, "Removed [ " + pid + " ] [ " + removedConfiguration + " ]");
        }
        save();
    }

    /**
     * Retrieve or create a configuration having the specified ID.
     *
     * A new configuration is created with just a PID and a location.
     *
     * @param pid The PID of the configuration which is to be retrieved.
     * @param location The location of the new configuration.
     *
     * @return The configuration which was retrieved or created.
     */
    public ExtendedConfigurationImpl getConfiguration(String pid, String location) {
        // This uses a form of double-locking:
        //
        // First, attempt to retrieve the target configuration within a read lock.
        //
        // Second, attempt again to retrieve the target configuration within a write
        // lock.  If the second attempted retrieval fails, only then create a new
        // configuration.

        ExtendedConfigurationImpl configuration =
            excludeWrites((useConfigurations) -> useConfigurations.get(pid));
        if ( configuration != null ) {
            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                Tr.debug(tc, "Retrieved [ " + pid + " ] [ " + configuration + " ]");
            }
            return configuration;
        }

        // Note that this does a second 'get'!
        configuration = excludeAll( (useConfigurations) ->
            useConfigurations.computeIfAbsent(pid, (usePid) -> createConfiguration(location, null, usePid)) );

        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, "Added [ " + pid + " ] " + configuration);
        }
        return configuration;
    }

    /**
     * Create and store a new factory element using the supplied factory PID.
     *
     * Generate a new PID for the new element.
     *
     * The new element is created with null attributes.
     *
     * @param factoryPid The factory PID of the new element.
     * @param location The location of the new element.
     *
     * @return The new factory element.
     */
    public ExtendedConfiguration createFactoryConfiguration(String factoryPid, String location) {
        ExtendedConfigurationImpl factoryConfiguration = excludeAll( (useConfigurations) -> {
            String pid = generatePid(factoryPid);
            ExtendedConfigurationImpl configuration = createConfiguration(location, factoryPid, pid);
            useConfigurations.put(pid, configuration);
            return configuration;
        } );

        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, "New factory [ " + factoryPid + " ]" + " [ " + location + " ]" +
                                    " [ " + factoryConfiguration.getPid() + " ]" +
                                    " [ " + factoryConfiguration + " ]");
        }
        return factoryConfiguration;
    }

    /**
     * Retrieve the configuration which has the specified PID.
     *
     * @param pid The PID of the configuration which is to be retrieved.
     *
     * @return The configuration matching the specified PID.  Null if no
     *     stored configuration matches the PID.
     */
    public ExtendedConfigurationImpl findConfiguration(String pid) {
        return excludeWrites( (useConfigurations) -> useConfigurations.get(pid) );
    }

    /**
     * Answer the configurations which have the specified factory PID.
     *
     * The results include deleted configurations.
     *
     * @param factoryPid The factory PID of the configurations which
     *     are to be retrieved.
     *
     * @return The configurations which have the specified factory PID.
     */
    public ExtendedConfigurationImpl[] getFactoryConfigurations(String factoryPid) {
        List<ExtendedConfigurationImpl> factoryConfigurations = new ArrayList<ExtendedConfigurationImpl>();

        excludeWrites( useConfigurations -> {
            for ( ExtendedConfigurationImpl configuration : useConfigurations.values() ) {
                // 'false' means don't fail if the configuration was deleted.
                String otherFactoryPid = configuration.getFactoryPid(false);
                if ( (otherFactoryPid != null) && otherFactoryPid.equals(factoryPid) ) {
                    factoryConfigurations.add(configuration);
                }
            }
            return null; // block return
        } );

        return factoryConfigurations.toArray(new ExtendedConfigurationImpl[factoryConfigurations.size()]);
    }

    /**
     * Answer the configurations which match a specified selection filter.
     *
     * @param selector A condition {@link Filter} which selects the
     *     configurations which are to be retrieved.
     *
     * @return The configurations which match the specified selection filter.
     *     Answer null if no configurations match the selection filter.
     */
    public ExtendedConfiguration[] listConfigurations(Filter selector) {
        List<ExtendedConfigurationImpl> selectedConfigurations = new ArrayList<>();
        excludeWrites( (useConfigurations) -> {
            selectedConfigurations.addAll(configurations.values());
            return null;
        } );

        // Note: The filter is a selection filter.  Keep configurations
        // which match the filter.  Remove configurations which do not match
        // the filter.
        selectedConfigurations.removeIf((configuration) -> !configuration.matchesFilter(selector));

        int size = selectedConfigurations.size();
        if ( size == 0 ) {
            return null;
        } else {
            return selectedConfigurations.toArray(new ExtendedConfigurationImpl[size]);
        }
    }

    /**
     * Conditionally unbind (invoke {@link ExtendedConfigurationImpl#unbind(Bundle)})
     * all of the configurations.
     *
     * Do nothing to configurations which are not bound to the specified bundle.
     *
     * @param bundle The bundle from which to unbind the configurations.
     */
    public void unbindConfigurations(Bundle bundle) {
        for ( ExtendedConfigurationImpl configuration : getConfigurations() ) {
            configuration.unbind(bundle);
        }
    }

    //

    /**
     * Second half of the persistence API. Schedule a new save of the configuration.
     *
     * Immediately return if a save is already scheduled.
     */
    void save() {
        // Safe: Double locking.
        if ( (saveTask != null) || shutdown ) {
            return;
        }

        synchronized( saveMonitor ) {
            // 'shutdown' can only be set just before
            // performing an immediate save.  That will
            // consume any pending save task.
            //
            // This code will only be reached with 'shutdown' set
            // if the cancel following the shutdown save is not
            // processed before the save is scheduled.  Normally,
            // the cancel will prevent the save from being attempted.

            if ( (saveTask != null) || shutdown ) {
                return;
            }

            // This is thread safe.  The scheduled save runs in
            // an independent thread which will block on the save
            // monitor within 'doSave'.  The assignment to 'saveTask'
            // must complete before the save monitor is released
            // and allowing 'saveTask' to be retrieved.

            saveTask = caFactory.updateQueue.addScheduled( () -> {
                try {
                    doSave();
                } catch ( IOException e ) {
                    // Auto-FFDC is fine here
                }
            } );
        }
    }

    /**
     * Immediately perform any pending save.
     *
     * This is only invoked with shutdown requested from
     * {@link com.ibm.ws.config.admin.internal.ConfigAdminServiceFactory#closeServices}.
     *
     * @param shutdown Control parameter: Disable (or enable) future
     *                 save requests. Do not interrupt any already
     *                 started save.
     *
     * @throws IOException Thrown if the save fails.
     */
    @Deprecated
    void saveConfigurationDatas(boolean shutdown) throws IOException {
        doSave(shutdown); // throws IOException
    }

    /**
     * Conditionally perform a save.  Conditionally, set the
     * shutdown flag.
     *
     * If a save task which was scheduled is still pending,
     * then clear it and proceed to store the configuration.
     *
     * After storing the configuration, if shutdown was set,
     * cancel the save task.  That is done because the save
     * was performed immediately, not as scheduled by the update
     * queue.
     *
     * @param shutdown Control parameter: Disable (or enable) future
     *                 save requests. Do not interrupt any already
     *                 started save.
     *
     * @throws IOException Thrown if the save fails.
     */
    private void doSave(boolean setShutdown) throws IOException {
        Future<?> saveTask;
        if ( (saveTask = pullSaveTask(setShutdown)) != null ) {
            if ( setShutdown ) {
                // Help the update queue to shutdown: Cancel the
                // pending save, which would obtain a null save
                // task and subsequently do nothing.
                saveTask.cancel(false);
            }
            store(); // throws IOException
        }
    }

    /**
     * Basic operation: Store the active configuration to the configuration file.
     *
     * @throws IOException Thrown if the store failed.
     */
    private void store() throws IOException {
        excludeWrites( (useConfigurations) -> {
            ConfigurationStorageHelper.store(persistentConfig, useConfigurations.values());
            // throws IOException
            return null;
        } );
    }

    private static final boolean DO_SET_SHUTDOWN = true;

    /**
     * Retrieve and set the save task to null as an atomic operation.
     *
     * Retrieval of the save task is performed when the save task eventually
     * runs.
     *
     * The shutdown flag, once set, will never be cleared.  Invoking this
     * with 'setShutdown' set to false has no affect on the shutdown flag.
     *
     * @param setShutdown Control parameter: Used to set the shutdown flag.
     *
     * @return The current save task.
     */
    private Future<?> pullSaveTask(boolean setShutdown) {
        Future<?> saveTask;
        synchronized(saveMonitor) {
            // Consume the save task reference, and return it.
            // Normally, the save task only serves to collapse
            // concurrent saves. Returning the task allows it to
            // be cancelled, which is done to speed up shutdown
            // processing.
            saveTask = this.saveTask;
            this.saveTask = null;

            // Prevent any saves from being scheduled.
            // For thread safety, this must be done within
            // the save monitor.
            if ( setShutdown ) {
                this.shutdown = true;
            }
        }
        return saveTask;
    }

    /**
     * Half of the configuration store persistence API. Marshal configurations
     * from the specified configuration file into a table keyed by persistence ID.
     *
     * Answer an empty collection if the configuration file does not exist (or
     * if it exists but is not a simple file.)
     *
     * Answer an empty collection if an IO exception occurs while reading the
     * configuration file. The exception is processed by FFDC but is otherwise
     * ignored.
     *
     * Note: No synchronization is provided for this initial load: A load currently
     * only occurs while creating a new configuration store.
     *
     * @param configurationFile A configuration file from which to read configurations.
     *
     * @return Configurations read from the configuration file.
     */
    private Map<String, ExtendedConfigurationImpl> load(File configurationFile) {
        String loadCase;

        Map<String, ExtendedConfigurationImpl> loaded;
        if ( !configurationFile.isFile() ) {
            // Either, the configuration file does not exist, or is not a normal file.

            // TODO: This might be better with distinct 'exists' and 'isFile' tests:
            //       We probably care if the configuration file exists but is not a
            //       normal file.  On the other hand, that should never happen.

            loaded = new HashMap<>();
            loadCase = "Unloadable file";

        } else {
            // Helper which marshals de-serialized configuration data.
            //
            // The marshalled data is an extended configuration instance.
            //
            // The marshalled data contains, in particular the factory PID and PID of
            // previously serialized data.

            ConfigStorageConsumer<String, ExtendedConfigurationImpl> consumer =
                new ConfigStorageConsumer<String, ExtendedConfigurationImpl>() {
                    /**
                     * Implementer API: Marshal configuration data by creating a configuration.
                     *
                     * @param location The location of the configuration.
                     * @param uniqueVariables The unique variables of the configuration.
                     * @param references The references within the configuration.
                     * @param properties The attributes of the configuration.
                     *
                     * @return The new configuration.
                     */
                    @Override
                    public ExtendedConfigurationImpl consumeConfigData(String location, Set<String> uniqueVariables, Set<ConfigID> references, ConfigurationDictionary properties) {
                        return createConfiguration(location, uniqueVariables, references, properties);
                    }

                    /**
                     * Answer the key value from a configuration.
                     *
                     * This implementation answers the PID of the configuration.
                     * The PID is answered regardless of whether the configuration
                     * has been deleted.  See {@link ExtendedConfigurationImpl#getPid(boolean)}
                     *
                     * @param configuration The configuration from which to retrieve the key.
                     *
                     * @return The key value of the configuration.
                     */
                    @Override
                    public String getKey(ExtendedConfigurationImpl configuration) {
                        return configuration.getPid(false);
                    }
            };

            // Create a table of configurations loaded from the specified configuration
            // file.

            try {
                loaded = ConfigurationStorageHelper.load(configurationFile, consumer);
                loadCase = "Loaded file";

            } catch (IOException e) {
                // FFDC, but continue.  Do not use the partially loaded configuration.
                loaded = new HashMap<>(); // AutoFFDC
                loadCase = "Load failure [ " + e + " ]";
            }
        }

        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, "Loaded PIDS [ " + loaded.keySet() + " ]" +
                                    " [ " + configurationFile.getAbsolutePath() + " ]" +
                                    " ( " + loadCase + " )");
        }
        return loaded;
    }
}
