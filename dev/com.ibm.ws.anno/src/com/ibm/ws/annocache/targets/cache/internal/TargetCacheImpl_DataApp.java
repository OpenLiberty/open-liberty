/*******************************************************************************
 * Copyright (c) 2014, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.annocache.targets.cache.internal;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.annocache.util.internal.UtilImpl_PoolExecutor;
import com.ibm.wsspi.annocache.classsource.ClassSource_Factory;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_Options;
import com.ibm.wsspi.annocache.util.Util_Consumer;

/**
 * Annotation cache data for a single application.
 * 
 * Each application has its own root folder relative to the root
 * cache folder.  See {@link TargetCacheImpl_DataApps}.
 * 
 * Each application contains a weakly held collection of module data,
 * keyed by module name, and a weakly held collection of container data,
 * keyed by container path.
 * 
 * Container data is shared between the modules of an application.  Container
 * data is <em>not</em> shared between applications because of problems using
 * container paths which are not relative to applications.  Non-relative container
 * paths must be handled as absolute paths.  Using absolute paths for containers
 * would make the annotation cache non-relocatable.
 *
 * Module data is held weakly: Module data is not retained without a reference to
 * the module data outside of the annotation cache.  Similarly, container data is
 * held weakly.
 *
 * Neither module data nor container data is initially loaded from the root cache
 * folder.
 * 
 * Container data holds in memory the last read data, and retains the time stamp of that
 * read.  That enables component data between applications.
 */
public class TargetCacheImpl_DataApp extends TargetCacheImpl_DataBase {
    private static final String CLASS_NAME = TargetCacheImpl_DataApp.class.getSimpleName();

    //

    /**
     * Create cache data for an application.
     *
     * @param apps The parent applications data.
     * @param appName The name of the application.
     * @param e_appName The encoded name of the application.
     * @param appDir The directory of the application cache data.
     */
    public TargetCacheImpl_DataApp(
        TargetCacheImpl_DataApps apps,
        String appName, String e_appName,
        File appDir) {

        super( apps.getFactory(), appName, e_appName, appDir );

        // (new Throwable("DataApp [ " + appName + " : " + ((appDir == null) ? "*** NULL ***" : appDir.getAbsolutePath()) + " ]")).printStackTrace(System.out);

        this.apps = apps;

        this.modsLock = new ModsLock();
        this.mods = new WeakHashMap<String, TargetCacheImpl_DataMod>();

        this.consLock = new ConsLock();
        this.cons = new WeakHashMap<String, TargetCacheImpl_DataCon>();

        // Writes are pooled per application.
        //
        // Writes are not possible if the application is unnamed.
        //
        // Module writes are not possible while container writes are
        // possible when the application is named but the module is
        // unnamed or is lightweight.

        if ( !this.isNamed() ) {
            this.writePool = null;

        } else {
            int writeThreads = this.cacheOptions.getWriteThreads();

            if ( (writeThreads == 1) || (writeThreads == 0) ) {
                this.writePool = null;

            } else {
                int corePoolSize = 0;

                int maxPoolSize;
                if ( writeThreads <= TargetCache_Options.WRITE_THREADS_UNBOUNDED) {
                    maxPoolSize = TargetCache_Options.WRITE_THREADS_MAX;
                } else if ( writeThreads > TargetCache_Options.WRITE_THREADS_MAX ) {
                    maxPoolSize = TargetCache_Options.WRITE_THREADS_MAX;
                } else {
                    maxPoolSize = writeThreads;
                }

                this.writePool = UtilImpl_PoolExecutor.createNonBlockingExecutor(corePoolSize, maxPoolSize);
            }
        }
    }

    // Structure

    /** Parent root cache data. */
    private TargetCacheImpl_DataApps apps;

    /**
     * Answer the parent applications data, which is the root
     * of cache data.
     *
     * @return The parent applications data.
     */
    public TargetCacheImpl_DataApps getApps() {
        return apps;
    }

    //

    /**
     * Factory method: Create module cache data.
     * 
     * Do not store the new data.
     *
     * @param modName The module name.
     * @param isLightweight Case parameter: Tells whether the
     *     module has a single container, not counting the external
     *     class source.
     *
     * @return The new module cache data.
     */
    @Trivial
    protected TargetCacheImpl_DataMod createModData(String modName, boolean isLightweight) {
        String e_modName = encode(modName);
        return createModData( modName, e_modName, e_getModDir(e_modName), isLightweight );
    }

    /**
     * Factory helper method: Determine the file of module cache directory.
     *
     * @param e_modName The encoded module name.
     *
     * @return The directory of the module cache data.
     */
    @Trivial
    public File e_getModDir(String e_modName) {
        return getDataFile( e_addModPrefix(e_modName) );
    }

    //

    /**
     * Factory method: Create cache data for a module.  Do not store the
     * module data.
     *
     * @param modName The name of the module.
     * @param e_modName The encoded name of the module.
     * @param modDir The directory of the cache data.
     * @param isLightweight Case parameter: Tells whether the
     *     module has a single container, not counting the external
     *     class source.
     *
     * @return The new module cache data.
     */
    @Trivial
    protected TargetCacheImpl_DataMod createModData(
        String modName, String e_modName, File modDir,
        boolean isLightweight) {

        return getFactory().createModData(this, modName, e_modName, modDir, isLightweight);
    }

    // Module cache data storage ...

    private class ModsLock {
        // EMPTY
    }
    private final ModsLock modsLock;
    private final WeakHashMap<String, TargetCacheImpl_DataMod> mods;

    /**
     * Answer the table of module cache data of this application.
     *
     * @return The table of module cache data of this application. 
     */
    @Trivial
    protected Map<String, TargetCacheImpl_DataMod> getMods() {
        return mods;
    }

    /**
     * Obtain cache data for a module of this application..
     * 
     * Create new data if the module is unnamed, or if the module
     * is lightweight.
     * 
     * Otherwise, either retrieve data from the module store,
     * or create and store new data, and return the new data.
     *
     * @param modName The name of the module.
     * @param isLightweight Case parameter: Tells whether the
     *     module has a single container, not counting the external
     *     class source.
     *
     * @return Cache data for the module.
     */
    public TargetCacheImpl_DataMod getModForcing(String modName, boolean isLightweight) {
        // Unnamed modules always create new data.
        // Lightweight modules always create new data.
        if ( (modName == ClassSource_Factory.UNNAMED_MOD) || isLightweight ) {
            return createModData(modName, isLightweight);
        }

        synchronized( modsLock ) {
            TargetCacheImpl_DataMod mod = mods.get(modName);
            if ( mod == null ) {
                mod = createModData(modName, isLightweight);
                mods.put(modName, mod);
            }

            return mod;
        }
    }

    // Containers storage ...

    private class ConsLock {
        // EMPTY
    }
    private final ConsLock consLock;
    private final WeakHashMap<String, TargetCacheImpl_DataCon> cons;

    /**
     * Obtain cache data for a container of this application..
     * 
     * Either retrieve data from the module store, or create and
     * store new data, and return the new data.
     *
     * @param isNamed Control parameter: Tell if the container is considered
     *     "unnamed", which means it will not be read or written.
     * @param conPath The relative path to the container.
     *
     * @return Cache data for the container.
     */
    public TargetCacheImpl_DataCon getSourceConForcing(boolean isNamed, String conPath) {
        synchronized( consLock ) {
            TargetCacheImpl_DataCon con = cons.get(conPath);
            if ( con == null ) {
                con = createSourceConData(isNamed, conPath);
                cons.put(conPath, con);
            }
            return con;
        }
    }

    /**
     * Factory method: Create container cache data.  Do not store the new data.
     * 
     * @param isNamed Control parameter: Tell if the container is considered
     *     "unnamed", which means it will not be read or written.
     * @param conPath The relative path to the container.
     *
     * @return Cache data for the container.
     */
    @Trivial
    public TargetCacheImpl_DataCon createSourceConData(boolean isNamed, String conName) {
        String e_conName = ( isNamed ? encode(conName) : null );
        File e_resultConFile = ( isNamed ? e_getConFile(e_conName) : null );
        return createConData( this,
            conName, e_conName, e_resultConFile,
            TargetCacheImpl_DataCon.IS_SOURCE);
    }

    //

    /**
     * Tell if data should be written for this application.
     *
     * Answer false if the application is unnamed, or if writes
     * are disabled via cache options.
     * 
     * @return True or false telling if data should be written for
     *     this application.
     */
    @Override
    @Trivial
    public boolean shouldWrite(String outputDescription) {
        String methodName = "shouldWrite";

        if ( !isNamed() ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "Unnamed application: Skipping write of [ {0} ]", outputDescription);
            }
            return false;

        } else {
            return super.shouldWrite(outputDescription);
            // logging in super.shouldWrite
        }
    }

    /**
     * Tell if data should be read for this application.
     *
     * Answer false if the application is unnamed, or if reads
     * are disabled via cache options.
     * 
     * @return True or false telling if data should be read for
     *     this application.
     */
    @Override
    @Trivial
    public boolean shouldRead(String inputDescription) {
        String methodName = "shouldRead";

        if ( !isNamed() ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "Unnamed application: Skipping read of [ {0} ]", inputDescription);
            }
            return false;

        } else {
            return super.shouldRead(inputDescription);
            // logging in super.shouldRead
        }
    }
    
    //

    // Pool writes at the application level.  This is to reduce the number of concurrent
    // writes.  Pooling writes at the module level allows many many concurrent writes,
    // as each module would have its own pool, and there can be many modules.
    //
    // This problem is demonstrated by a test application with hundreds of modules.
    // For example, a "huge ejbs" application was created which had 500 small ejb
    // jars.  Giving each ejb jar its own thread pool would lead to potentially
    // several thousand of simultaneous write threads.

    // Writes are simple: Most can be simply spawned and allowed to complete
    // in their own time.
    //
    // Reads require additional coordination (joins for the results) and are
    // handled by the calling code.

    // TODO:
    // The write pool is experimental.  It is current unused, pending solutions
    // to these problems:
    //
    // First, multi-threaded writes have not shown to be definitely advantageous.
    // Initial testing did not show a clear performance gain from using multiple
    // write threads.
    //
    // Second, multi-threaded writes require more a more integrated use of
    // standard threading services.
    //
    // Fourth, policies for managing threads when multiple applications are in use
    // must be determined.
    //
    // Fifth, asynchronous writes require the invention of two new capabilities:
    // Reads and writes of data must be more carefully synchronized than is provided
    // by the current implementation.  Writes must be tied to the server shutdown, to
    // ensure that writes which are pending are allowed to complete.

    protected final UtilImpl_PoolExecutor writePool;

    @Trivial
    protected UtilImpl_PoolExecutor getWritePool() {
        return writePool;
    }

    @Trivial
    protected boolean isWriteSynchronous() {
        return ( getWritePool() == null );
    }

    // TODO: Unify the writer types.

    @Trivial
    protected void scheduleWrite(
        final TargetCacheImpl_DataMod modData,
        final String description,
        final File outputFile,
        final boolean doTruncate,
        final Util_Consumer<TargetCacheImpl_Writer, IOException> writeAction,
        final Util_Consumer<TargetCacheImpl_WriterBinary, IOException> writeActionBinary) {

        String methodName = "scheduleWrite";

        UtilImpl_PoolExecutor useWritePool = getWritePool();
        if ( useWritePool == null ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "ENTER (immediate) [ {0} ]", description);
            }

            if ( writeAction != null ) {
                modData.performWrite(description, outputFile, doTruncate, writeAction);
            } else {
                modData.performBinaryWrite(description, outputFile, doTruncate, writeActionBinary);
            }

            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "RETURN (immediate) [ {0} ]", description);
            }

        } else {
            final Throwable scheduler =
                new Throwable(
                    "App [ " + getName() + " ] [ " + e_getName() + " ]" +
                    " Mod [ " + modData.getName() + " ] [ " + modData.e_getName() + " ]" +
                    " [ " + description + " ]");

            Runnable writeRunner = new Runnable() {
                @Override
                public void run() {
                    String innerMethodName = "scheduleWrite.run";
                    try {
                        if ( writeAction != null ) {
                            modData.performWrite(description, outputFile, doTruncate, writeAction);
                        } else {
                            modData.performBinaryWrite(description, outputFile, doTruncate, writeActionBinary);
                        }

                    } catch ( RuntimeException e ) {
                        // Capture and display any exception from the spawned writer thread.
                        // Without this added step information about the spawning thread is
                        // lost, making debugging writer problems very difficult.

                        logger.logp(Level.WARNING, CLASS_NAME, innerMethodName, "Caught Asynchronous exception", e);
                        logger.logp(Level.WARNING, CLASS_NAME, innerMethodName, "Scheduler", scheduler);
                        logger.logp(Level.WARNING, CLASS_NAME, innerMethodName, "Synchronization error", e);

                        throw e;
                    }
                }
            };

            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "ENTER (scheduled) [ {0} ]", description);
            }

            useWritePool.execute(writeRunner);

            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName, "RETURN (scheduled) [ {0} ]", description);
            }
        }
    }
}
