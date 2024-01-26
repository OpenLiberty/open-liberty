/*******************************************************************************
 * Copyright (c) 2019,2024 IBM Corporation and others.
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.config.admin.ConfigID;
import com.ibm.ws.config.admin.ConfigurationDictionary;
import com.ibm.ws.config.admin.ExtendedConfiguration;
import com.ibm.ws.config.admin.internal.ConfigurationStorageHelper.ConfigStorageConsumer;

import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointPhase;

//@formatter:off
/**
 * ConfigurationStore manages all active configurations along with persistence.
 *
 * The implementation uses a file store and serializes configuration dictionaries
 * to files identified by their PID.
 *
 * Persistence details are in the constructor, saveConfiguration, and
 * deleteConfiguration, and can be factored out separately if required.
 *
 * The store has two essential modes:
 *
 * <ul>
 * <li>Checkpoint processing is not enabled</li>
 * <li>Checkpoint processing is enabled</li>
 * </ul>
 *
 * When checkpoint processing is not enabled, activity flows
 * from an initial load, which occurs when the administrative
 * service is started, to occasional non-shutdown saves, which
 * are triggered by configuration updates, to a final shutdown
 * save, which occurs when the administrative service is stopped.
 *
 * A presumption is that no saves will be requested following
 * the shutdown save.  However, this is not enforced by the
 * implementation.
 *
 * A feature of the non-checkpoint flow is that overlapping
 * save requests are collapsed into a single save.  This is
 * imprecise: The save step clears the scheduled task, then
 * performs the save outside of a protected region.  The save
 * request will proceed if performed after the task is cleared
 * and before the save is performed.
 *
 * When checkpoint process is enabled, activity flows differently.
 * The initial load is performed.  However, saves requested by
 * updates which occur before the checkpoint save are not performed.
 * These instead cause a phase hook to be added, which eventually
 * causes a save through the prepare method.
 *
 * Save requests which are performed after
 *
 */
class ConfigurationStore implements CheckpointHook {
    private static final TraceComponent tc =
        Tr.register(ConfigurationStore.class,
                    ConfigAdminConstants.TR_GROUP, ConfigAdminConstants.NLS_PROPS);

    //

    /**
     * Create a new configuration store.
     *
     * Immediately load persisted configurations.
     *
     * Configurations are persisted to {@link ConfigAdminConstants#CONFIG_PERSISTENT}
     * as a bundle context specific data file.
     *
     * @param caFactory Configuration administration factory.  This is supplied to all
     *     configurations loaded or created by this configuration store.
     * @param bundleContext The current bundle context.  This provides the location of
     *     the configurations resource.
     */
    public ConfigurationStore(ConfigAdminServiceFactory caFactory, BundleContext bundleContext) {
        this.caFactory = caFactory;

        this.configurationsFile = bundleContext.getDataFile(ConfigAdminConstants.CONFIG_PERSISTENT);

        LoadResult[] loadResults = new LoadResult[1];
        this.configurations = loadConfigurationDatas(this.caFactory, this.configurationsFile, loadResults);
        this.loadResult = loadResults[0];

        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, "Resource [ " + this.configurationsFile + " ]");
            Tr.debug(tc, "Load result [ " + this.loadResult + " ]");
            Tr.debug(tc, "Loaded PIDs [ " + this.configurations.keySet() + " ]");
        }
    }

    /**
     * Control parameter for invoking {@link ExtendedConfigurationImpl#getPid(boolean)}
     * and {@link ExtendedConfigurationImpl#getFactoryPid(boolean)}.  Used to indicate
     * that the deletion state of the receiver is to be ignored.
     *
     * When retrieving a factory PID or PID, an exception is usually thrown if the
     * receiver is deleted.  Parameter value <code>!CHECK_DELETED</code> causes
     * the deletion state to be ignored.
     */
    private static final boolean CHECK_DELETED = true;

    // Extended configuration ...

    /**
     * Service factory.  Passed into extended configurations created by the store.
     * Also used to schedule saves.
     */
    private final ConfigAdminServiceFactory caFactory;

    // Core storage ...

    /** Main store of configurations.  Keys are PIDs. */
    private final Map<String, ExtendedConfigurationImpl> configurations;

    /** Lock for updates to the configurations. */
    private final ReentrantReadWriteLock configurationsLock = new ReentrantReadWriteLock();

    // Configuration persistence ...

    /** Resource used to persist configurations. */
    private final File configurationsFile;

    public static enum LoadResult {
        /** The load of the configuration was successful. */
        SUCCESS(0),

        /** The load was not performed because no resource is available. */
        MISSING_FILE(1),

        /** The load could not be performed because an unsupported version was detected. */
        UNSUPPRTED_VERSION(2),

        /** The load failed with an exception.  This is usually because of a corrupt or truncated resource. */
        READ_ERROR(3);

        private LoadResult(int code) {
            this.code = code;
        }

        public final int code;

        public int getCode() {
            return code;
        }

        public boolean wasSuccessful() {
            return ( code == SUCCESS.getCode() );
        }
    }

    /** The result of the load performed by the initializer. */
    private final LoadResult loadResult;

    /**
     * Encapsulation of a scheduled call to {@link #doSaveConfigurations()}.
     * See {@link #scheduleUpdate(Runnable)}.
     */
    private Future<?> saveTask;

    /**
     * State: Is this configuration store shut down?
     *
     * Set when saving configurations. After performing the save,
     * if the store was marked as shut down, any active save task
     * is cancelled and future save requests are ignored.
     */
    private boolean shutdown;

    /** State: Has a save hook been added to the checkpoint phase? */
    private boolean addedCheckpointSaveHook;

    /**
     * Copy of the checkpoint phase.  The presumption is that the phase
     * is retrieved after it had been set to it's final value.
     *
     * The phase, once set, is constant for the process.
     */
    private final CheckpointPhase checkpointPhase = CheckpointPhase.getPhase();

    /** Lock for save activity. */
    // TODO: Would a read-write lock be better?  'save' could use lazy locking.
    private final Lock saveLock = new ReentrantLock();

    //

    /**
     * Administration primitive: Schedule an update through
     * the administrative service factory.
     *
     * @param action The action to schedule through the factory.
     *
     * @return The action as a scheduled future.
     */
    private Future<?> scheduleUpdate(Runnable action) {
        return caFactory.updateQueue.addScheduled(action);
    }

    /**
     * Configuration factory method.  Create a configuration for a specified
     * location and using a specified PID.
     *
     * The location is usually a bundle location.
     *
     * The new configuration is empty: It has no data, no variables, and no
     * external references.
     *
     * @param location The location to use in the new configuration.
     * @param factoryPid the factory PID which provided the configuration's PID.
     * @param pid The PID associated with the new configuration.
     *
     * @return A new configuration.
     */
    @Trivial
    private ExtendedConfigurationImpl newConfig(String location, String factoryPid, String pid) {
        return new ExtendedConfigurationImpl(caFactory, location, factoryPid, pid, null, null, null);
    }

    // Core protection ...

    // TODO: Not sure if having these methods is better or worse
    //       than inlining in the code.
    //
    //       However, the locks and the methods are final, and there
    //       should be no injected trace.  These should be inlined by
    //       the JIT.

    @Trivial
    private final void readLock() {
        configurationsLock.readLock().lock();
    }

    @Trivial
    private final void readUnlock() {
        configurationsLock.readLock().unlock();
    }

    @Trivial
    private final <T> T blockUpdates(Supplier<T> supplier) {
        readLock();
        try {
            return supplier.get();
        } finally {
            readUnlock();
        }
    }

    public interface FailableRunnable<E extends Exception> {
        void run() throws E;
    }

    @Trivial
    private final <E extends Exception> void failableBlockUpdates(FailableRunnable<E> action) throws E {
        readLock();
        try {
            action.run(); // throws E
        } finally {
            readUnlock();
        }
    }

    @Trivial
    private final void writeLock() {
        // Fail fast if promotion from read to write is detected:
        // Read to write promotion can cause deadlocks.
        if ( configurationsLock.getReadHoldCount() > 0 ) {
            throw new IllegalMonitorStateException("Write lock attempted while holding read lock.");
        }
        configurationsLock.writeLock().lock();
    }

    @Trivial
    private final void writeUnlock() {
        configurationsLock.writeLock().unlock();
    }

    @Trivial
    private final void blockAccess(Runnable action) {
        writeLock();
        try {
            action.run();
        } finally {
            writeUnlock();
        }
    }

    @Trivial
    private final <T> T blockAccess(Supplier<T> supplier) {
        writeLock();
        try {
            return supplier.get();
        } finally {
            writeUnlock();
        }
    }

    @Trivial
    private final void saveProtect(Runnable action) {
        saveLock.lock();
        try {
            action.run();
        } finally {
            saveLock.unlock();
        }
    }

    @Trivial
    private final <T> T saveProtect(Supplier<T> supplier) {
        saveLock.lock();
        try {
            return supplier.get();
        } finally {
            saveLock.unlock();
        }
    }

    // PID generation ...

    /**
     * Simple integer counter type. Used for generating reference PIDs
     * for configuration elements.
     */
    public static class Counter {
        private final String factoryPid;
        private int value;

        public Counter(String factoryPid) {
            this.factoryPid = factoryPid;
            this.value = 0;
        }

        public int increment() {
            return value++;
        }

        public String generatePid() {
            return factoryPid + "_" + increment();
        }
    }

    /**
     * A counter for PID names to avoid collisions.
     *
     * This operation is not thread safe: The caller,
     * {@link #createFactoryConfiguration(String, String)}, ensures
     * safety by including the invocation in a region that is guarded
     * by the configurations write lock.
     *
     * Keys are factory PIDs. Values are counters, which are used to generate
     * reference PIDs for configuration elements.
     */
    private final Map<String, Counter> counters = new HashMap<>();

    /**
     * Generate a new count for a given factory PID.
     *
     * Counts start at 0. And are generated by incrementing the previous
     * count.
     *
     * This operation is not thread safe: The caller,
     * {@link #createFactoryConfiguration(String, String)}, ensures
     * safety by including the invocation in a region that is guarded
     * by the configurations write lock.
     *
     * Each factory PID has its own counter. That enables the configuration
     * admin to continue its main processing loops, which aggregate the
     * processing of configurations by factory PID, and which iterate across
     * the factory PIDs in an indeterminate order.
     *
     * The processing per factory PID orders the configuration data by sequence
     * number. That ensures that minimally changed configurations obtain the
     * same PIDs for the same configuration elements.
     *
     * See issue 22058 "Unnecessary application expansion on restart".
     * See also changes made to {@link com.ibm.ws.config.xml.internal.ChangeHandler},
     * which orders the processing of configuration data by sequence number.
     *
     * @param factoryPid The factory PID for which to generate a new count.
     *
     * @return The new count for the specified factory PID.
     */
    @SuppressWarnings("unused")
    private long getCount(String factoryPid) {
        return getCounter(factoryPid).increment();
    }

    /**
     * Answer a counter specific to a factory PID.  Create and store a counter
     * if necessary.
     *
     * The caller is expected to invoke {@link Counter#increment()} to generate PIDs.
     *
     * This operation is not thread safe: The caller must ensure that the invocation
     * is guarded within the context of a specific configuration store.
     *
     * @param factoryPid A factory PID.
     *
     * @return A counter of PIDs for that factory PID.
     */
    private Counter getCounter(String factoryPid) {
        return counters.computeIfAbsent(factoryPid, (String useFactoryPid) -> new Counter(useFactoryPid));
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
        Counter factoryCounter = getCounter(factoryPid);

        String pid;
        while ( configurations.containsKey( pid = factoryCounter.generatePid() ) ) {
            // EMPTY
        }
        return pid;
    }

    //

    /**
     * Answer the result of the initial load.  See the result codes
     * for more information.
     *
     * @return The result of the initial load.
     */
    public LoadResult getLoadResult() {
        return loadResult;
    }

    // Configuration access ...

    /**
     * Retrieve the list of configurations.
     *
     * The list is retrieved safely, but loses its validity as
     * soon as it is returned.
     *
     * All configurations are returned, regardless of their state.
     *
     * @return The list of configurations.
     */
    public List<ExtendedConfigurationImpl> getConfigurations() {
        return blockUpdates(() -> new ArrayList<>(configurations.values()));
    }

    /*
     * Locate all stored configurations which have a specified factory PID.
     *
     * Answer all configurations which have the factory PID, regardless of
     * their deletion status.
     *
     * The array is retrieved safely, but loses its validity as soon as it is
     * returned.
     *
     * @param factoryPid The factory PID used to locate stored configurations.
     *
     * @return The array of configurations which have the specified factory PID.
     */
    public ExtendedConfigurationImpl[] getFactoryConfigurations(String factoryPid) {
        // TODO: 'getConfiguration' will create configurations which have
        // a null factory PID.  Such configurations are invisible to
        // 'getFactoryConfigurations.
        //
        // Using 'null' as the target factory PID does not retrieve configurations
        // which have a null factory PID.

        // TODO: Contrast with 'listConfigurations' and 'unbindConfigurations',
        //       both which process the configurations outside of the protected region.

        return blockUpdates(() -> {
            List<ExtendedConfigurationImpl> matches = new ArrayList<ExtendedConfigurationImpl>();

            for ( ExtendedConfigurationImpl config : configurations.values() ) {
                String otherFactoryPid = config.getFactoryPid(!CHECK_DELETED);
                if ( (otherFactoryPid != null) && otherFactoryPid.equals(factoryPid) ) {
                    matches.add(config);
                }
            }

            return matches.toArray(new ExtendedConfigurationImpl[matches.size()]);
        });
    }


    /**
     * Retrieve the configuration having the specified PID.  Answer null
     * if no matching configuration is found.
     *
     * @param pid The PID of the target configuration.
     *
     * @return The configuration having the specified PID.  Null if no
     *     match is found.
     */
    public ExtendedConfigurationImpl findConfiguration(String pid) {
        return blockUpdates(() -> configurations.get(pid));
    }

    /**
     * Remove the configuration having the specified PID.
     *
     * Save the entire store of configurations after removing
     * the configuration.
     *
     * Invoked by {@link ExtendedConfigurationImpl#delete(boolean)}.
     *
     * @param pid The PID of the configuration which is to be
     *     removed.
     */
    public void removeConfiguration(String pid) {
        boolean doTrace = TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled();

        List<String> remainingKeys = blockAccess(() -> {
            configurations.remove(pid);
            return ( doTrace ? new ArrayList<>( configurations.keySet() ) : null );
        } );

        // 'getConfiguration' and 'createFactoryConfiguration' add new configurations
        // but do not have a corresponding 'save'.  The new configurations are not yet
        // populated.  Saving them would not be meaningful.

        save();

        if ( doTrace ) {
            Tr.debug(tc, "Removed PID [ " + pid + " ]" +
                         " remaining PIDs [ " + remainingKeys + " ] ");
        }
    }

    /**
     * Get a configuration for a specified PID and bundle.
     *
     * Create and store a configuration if necessary.
     *
     * @param pid The PID of the requested configuration.
     * @param bundle The bundle supplying the location of the
     *     configuration.
     *
     * @return The configuration having the specified PID.
     */
    @Trivial
    public ExtendedConfigurationImpl getConfiguration(String pid, Bundle bundle) {
        return getConfiguration( pid, bundle.getLocation() );
    }

    /**
     * Get a configuration for a specified PID and location.
     *
     * Create and store a configuration if necessary.
     *
     * @param pid The PID of the requested configuration.
     * @param location The location of the requested configuration.
     *
     * @return The configuration having the specified PID.
     */
    public ExtendedConfigurationImpl getConfiguration(String pid, String location) {
        // Lazy locking: Try a simple get.
        // If that fails, place a write lock and try again.
        // If that fails, create and store a new configuration, and
        // return that new configuration.

        ExtendedConfigurationImpl config0 = blockUpdates(() -> configurations.get(pid));
        if ( config0 != null ) {
            // TODO: Verify the location of the saved configuration?
            return config0;
        }

        // There is no way to promote the read lock into a write lock ... we  must
        // completely drop the read lock then put on a write lock.

        return blockAccess(() -> {
            ExtendedConfigurationImpl config1 = configurations.get(pid);
            if (config1 == null) {
                // TODO: Not setting a factory PID seems problematic.
                // See 'getFactoryConfigurations', which uses the factory PID
                // of stored configurations as a query key.  Configurations
                // created and stored by 'getConfiguration' are invisible to
                // 'getFactoryConfigurations'.

                // Note that 'removeConfiguration' does a save, but 'getConfiguration'
                // and 'createFactoryConfiguration' do not.
                //
                // The new configuration is incomplete, having neither variables, nor
                // external references, nor data.

                configurations.put(pid, config1 = newConfig(location, null, pid));
            }
            return config1;
        });
    }

    /**
     * Create a configuration using the supplied factory PID and bundle
     * using {@link #createFactoryConfiguration(String, String)}.
     *
     * Use the bundle location as the location parameter.
     *
     * @param factoryPid The factory PID of the new configuration.
     * @param bundle The bundle supplying the location of the new configuration.
     *
     * @return The new configuration.
     */
    @Trivial
    public ExtendedConfiguration createFactoryConfiguration(String factoryPid, Bundle bundle) {
        return createFactoryConfiguration( factoryPid, bundle.getLocation() );
    }

    /**
     * Create and return a new factory configuration. Store the new configuration.
     *
     * Assign the factory PID and location as specified. Assign a generated PID
     * based on the factory PID and on a counter of configurations created
     * for the factory PID.
     *
     * The configuration is guaranteed to be new and to not overwrite any current
     * configuration: A new, unique PID is generated and uused to store the new
     * configuration.
     *
     * @param factoryPid The factory PID of the new configuration.
     * @param location The location of the new configuration.
     *
     * @return The new configuration.
     */
    public ExtendedConfiguration createFactoryConfiguration(String factoryPid, String location) {
        return blockAccess(() -> {
            // 'generatePid' guarantees that the PID is not a current key of
            // 'configurations'.
            String pid = generatePid(factoryPid);

            // Note that 'removeConfiguration' does a save, but 'createFactoryConfiguration'
            // and 'getConfiguration' do not.
            //
            // The new configuration is incomplete, having neither variables, nor
            // external references, nor data.

            ExtendedConfigurationImpl config;
            configurations.put(pid, config = newConfig(location, factoryPid, pid));
            return config;
        });
    }

    /**
     * Select configurations which match a specified filter.
     *
     * The results collection is not guaranteed to remain valid after
     * it is returned.  Changes to the stored configurations which
     * are made after the collection is obtained will not be reflected
     * in the filtered collection.
     *
     * @param filter A configuration filter.  Select configurations which
     *     match this filter.
     *
     * @return All selected configurations.  Null if no configurations
     *     where selected.
     */
    public ExtendedConfigurationImpl[] listConfigurations(Filter filter) {
        // TODO: Contrast with 'getFactoryConfigurations', which processes
        //       the configurations within the protected region.

        List<ExtendedConfigurationImpl> unfiltered =
            blockUpdates( () -> new ArrayList<>(configurations.values()) );

        Iterator<ExtendedConfigurationImpl> unfilteredIterator = unfiltered.iterator();
        while ( unfilteredIterator.hasNext() ) {
            ExtendedConfigurationImpl unfilteredConfig = unfilteredIterator.next();
            if ( !unfilteredConfig.matchesFilter(filter) ) {
                unfilteredIterator.remove();
            }
        }

        int size = unfiltered.size();
        if ( size == 0 ) {
            return null;
        } else {
            return unfiltered.toArray(new ExtendedConfigurationImpl[size]);
        }
    }

    /**
     * Unbind all stored configurations from the bundle.
     *
     * If the configuration has the specified bundle, clear
     * the bundle reference of the configuration.  Otherwise,
     * do nothing to the configuration.
     *
     * See {@link ExtendedConfigurationImpl#unbind(Bundle)}.
     *
     * @param bundle The bundle to unbind from all stored
     *     configurations.
     */
    public void unbindConfigurations(Bundle bundle) {
        // TODO: Contrast with 'getFactoryConfigurations', which processes
        //       the configurations within the protected region.

        List<ExtendedConfigurationImpl> configs =
            blockUpdates( () -> new ArrayList<>(configurations.values()) );

        for ( ExtendedConfigurationImpl config : configs ) {
            config.unbind(bundle);
        }
    }

    // Configuration persistence ...

    /**
     * Conditionally add a checkpoint save hook.  Tell if there is an added
     * save hook at the conclusion of this method invocation.
     *
     * Do nothing and answer false if the checkpoint phase is restored.
     * Do nothing and answer true if the checkpoint hook was already
     * added. Otherwise, attempt to add the save hook, and tell if the
     * save hook was added.
     *
     * @return True or false telling if a checkpoint save hook is
     *    present on the checkpoint phase.
     */
    private boolean addedCheckpointSaveHook() {
        if ( checkpointPhase.restored() ) {
            return false;

        } else {
            if ( !addedCheckpointSaveHook ) {
                addedCheckpointSaveHook = checkpointPhase.addMultiThreadedHook(this);
            }
            return addedCheckpointSaveHook;
        }
    }

    /**
     * Load configurations from the configurations file.
     *
     * Load is performed with no protection.  The only current invocation
     * is from the initializer, which is a thread safe invocation.
     *
     * Answer an empty map if the configurations file is not available.
     *
     * An FFDC error is generated if the load fails with an exception.
     * In this case, an empty map is also returned.
     *
     * @param caFactory The administrative service factory used by
     *     configurations created during the load.
     * @param configurationsFile The file containing persisted configurations.
     * @param result Storage for the load result.
     *
     * @return The loaded configurations.  An empty map when the load fails.
     */
    private static Map<String, ExtendedConfigurationImpl>
        loadConfigurationDatas(ConfigAdminServiceFactory caFactory,
                               File configurationsFile,
                               LoadResult[] result) {

        if ( !configurationsFile.isFile() ) {
            result[0] = LoadResult.MISSING_FILE;
            return new HashMap<>();
        }

        ConfigStorageConsumer<String, ExtendedConfigurationImpl> consumer =
            new ConfigStorageConsumer<String, ExtendedConfigurationImpl>() {

            /**
             * Marshal configuration data into the type used by the configuration
             * store.
             */
            @Override
            public ExtendedConfigurationImpl consumeConfigData(String location,
                                                               Set<String> uniqueVars,
                                                               Set<ConfigID> references,
                                                               ConfigurationDictionary readOnlyProps) {

                String pid = (String) readOnlyProps.get(Constants.SERVICE_PID);
                String factoryPid = (String) readOnlyProps.get(ConfigurationAdmin.SERVICE_FACTORYPID);

                return new ExtendedConfigurationImpl(caFactory,
                                                     location, factoryPid, pid,
                                                     readOnlyProps, references, uniqueVars);
            }

            /**
             * Determine the key used to store a loaded configuration.
             *
             * This implementation retrieved the configuration PID, ignoring
             * the deletion state of the configuration.
             */
            @Override
            public String getKey(ExtendedConfigurationImpl configuration) {
                return configuration.getPid(!CHECK_DELETED);
            }
        };

        Map<String, ExtendedConfigurationImpl> useStorage;
        try {
            useStorage = ConfigurationStorageHelper.load(configurationsFile, consumer);
            if ( useStorage == null ) {
                result[0] = LoadResult.UNSUPPRTED_VERSION;
                useStorage = new HashMap<>();
            } else {
                result[0] = LoadResult.SUCCESS;
            }
        } catch ( IOException e ) { // FFDC
            result[0] = LoadResult.READ_ERROR;
            useStorage = new HashMap<>();
        }
        return useStorage;
    }

    /**
     * Common point for performing saves of configuration
     * data.  Invoke {@link #saveConfigurationDatas(boolean).
     * Do NOT request a shutdown.  Handle any save IO
     * exceptions.
     *
     * Entry is only from {@link #save()} and {@link #prepare()}.
     */
    private void doSaveConfigurations() {
        try {
            saveConfigurationDatas(!IS_SHUTDOWN);
        } catch ( IOException e ) { // FFDC
            // Auto-FFDC is fine here
        }
    }

    /**
     * Control parameter: Used when performing a save to
     * mark that the configuration store is shut down.
     *
     * Set when saving configurations. After performing the save,
     * if the store was marked as shut down, any active save task
     * is cancelled and future save requests are ignored.
     */
    private static final boolean IS_SHUTDOWN = true;

    private static final class SaveState {
        public final Future<?> task;
        public final boolean isShutdown;

        public SaveState(Future<?> task, boolean isShutdown) {
            this.task = task;
            this.isShutdown = isShutdown;
        }
    }

    /**
     * Base step for saving the configurations.
     *
     * Invoked from {@link ConfigAdminServiceFactory#closeServices()}
     * with <code>TRUE</code>, and from
     * {ConfigurationStore#doSaveConfigurations()} with <code>FALSE</code>.
     *
     * @param isShutdown Control parameter: Tells if this is a shutdown save.
     *
     * @throws IOException Thrown if the save failed.
     */
    void saveConfigurationDatas(boolean isShutdown) throws IOException {
        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, "Saving: Shutdown [ " + isShutdown + " ]");
        }

        // Possible entries:
        //
        // As an invocation scheduled from 'save'.
        //
        // From the administrative service factory when closing services
        // (ConfigAdminServiceFactory.closeServices()).
        //
        // Or, from 'prepare', which is invoked when performing a phase
        // checkpoint.
        //
        // Only invocations from 'save' set a save task.

        // TODO: Should shutdown be allowed to be unset?
        //
        // The save overwrites any previous setting of 'shutdown'.
        // That means a later save with 'isShutdown' being false will
        // clear the shut down setting.

        // Consume the save task; record shutdown.
        SaveState saveState = saveProtect(() -> {
            SaveState useSaveState = new SaveState(saveTask, shutdown);
            saveTask = null;
            shutdown = isShutdown;
            return useSaveState;
        });
        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, "Saving: Task [ " + saveState.task + " ];" +
                         " prior shutdown [ " + saveState.isShutdown + " ]");
        }

        // If the task is not null, this must be the first call to
        // 'saveConfigurationDatas' since the invocation of 'save':
        // Proceed with the save.
        //
        // If the task is null, this invocation of 'saveConfigurationDatas'
        // does not immediately follow an invocation of 'save'.

        // See 'https://github.com/OpenLiberty/open-liberty/pull/27387/files'.

        if ( (saveState.task == null) && checkpointPhase.restored() ) {
            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                Tr.debug(tc, "Saving: Skipped; no task and restored");
            }
            return;
        }

        // Perform the basic save.
        //
        // If shutdown was marked, if there was a task set by an
        // invocation of 'save', cancel that save request.
        //
        // That 'save' invocation cannot have led to this specific
        // method invocation, as 'save' never marks shutdown.

        failableBlockUpdates( () -> {
            ConfigurationStorageHelper.store(configurationsFile, configurations.values());
        } );

        // TFB: Moved this outside of the update region.
        //
        //      Should this be in a save protected region?
        //
        if ( isShutdown && (saveState.task != null) ) {
            saveState.task.cancel(false);
            if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
                Tr.debug(tc, "Saving: Cancelled task");
            }
        }

        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, "Saving: PIDS [ " + configurations.keySet() + " ]");
        }
    }

    /**
     * Request a save of the configuration to a file.
     *
     * Do nothing if there is another pending save.  Do nothing
     * if the store has been shut down.  Possibly, allow the save
     * to be triggered by a phase hook.
     *
     * Otherwise, schedule the save through the administrative service
     * update scheduler.
     *
     * Invoked by:
     * {@link ConfigurationStore#removeConfiguration()},
     * {@link ExtendedConfigurationImpl#doUpdateProperties()},
     * {@link ExtendedConfigurationImpl#update()}, and
     * {@link ExtendedConfigurationImpl#updateCache()}.
     *
     * 'removeConfiguration' is invoked by:
     * {@link ExtendedConfigurationImpl#delete(boolean)}.
     */
    void save() {
        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, "Requesting save ...");
        }

        String result = saveProtect( () -> {
            // TFB: Moved this check on the save task into
            // the protected region.

            // There is an active save: Don't proceed with
            // another, which would be redundant.
            if ( saveTask != null ) {
                return "Skipped: Prior scheduled save";
            }

            // The store has been shut down.  Do nothing.
            if ( shutdown ) {
                return "Skipped: Shutdown";
            }

            // If possible, rely on the checkpoint phase save hook
            // to perform the save.
            //
            // Adding the save hook causes a at most a single invocation
            // of 'prepare'.  See 'CheckpointPhaseHookImpl.prepare()'.
            if ( addedCheckpointSaveHook() ) {
                return "Skipped: Active save hook";
            }

            // Proceed with a non-checkpoint save.
            // Do so by scheduling a call through the administrative
            // service update scheduler.
            saveTask = scheduleUpdate( () -> doSaveConfigurations() );
            return "Scheduled";
        } );

        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, "Requested save ... [ " + result + " ]");
        }
    }

    // Checkpoint hook APIs ...

    /**
     * Checkpoint hook API implementation: Prepare to create a checkpoint.
     *
     * This implementation performs a configuration save.  See {@link #doSaveConfigurations()}.
     *
     * One of three checkpoint hook APIs: {@link CheckpointHook#prepare()},
     * {@link CheckpointHook#checkpointFailed()}, and {@link CheckpointHook#restore()}.
     * The other two API hooks are not extended.
     */
    @Override
    public void prepare() {
        doSaveConfigurations();
    }
}
//@formatter:on