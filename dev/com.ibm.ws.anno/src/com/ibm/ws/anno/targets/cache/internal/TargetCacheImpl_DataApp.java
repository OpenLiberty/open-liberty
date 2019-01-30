/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2014, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.anno.targets.cache.internal;

import java.io.File;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.anno.targets.cache.internal.TargetCacheImpl_DataMod.ScheduleCallback;
import com.ibm.ws.anno.util.internal.UtilImpl_PoolExecutor;
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;
import com.ibm.wsspi.anno.targets.cache.TargetCache_Options;

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
 */
public class TargetCacheImpl_DataApp extends TargetCacheImpl_DataBase {
    private static final String CLASS_NAME = TargetCacheImpl_DataApp.class.getSimpleName();

    //

    public TargetCacheImpl_DataApp(
        TargetCacheImpl_DataApps apps,
        String appName, String e_appName, File appDir) {

        super( apps.getFactory(), appName, e_appName, appDir );

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

    //

    private TargetCacheImpl_DataApps apps;

    public TargetCacheImpl_DataApps getApps() {
        return apps;
    }

    //

    @Trivial
    protected TargetCacheImpl_DataMod createModData(String modName, boolean isLightweight) {
        String e_modName = encode(modName);
        return createModData( modName, e_modName, e_getModDir(e_modName), isLightweight );
    }

    @Trivial
    public File e_getModDir(String e_modName) {
        return getDataFile( e_addModPrefix(e_modName) );
    }

    //

    @Trivial
    protected TargetCacheImpl_DataMod createModData(
        String modName, String e_modName, File modDir,
        boolean isLightweight) {

        return getFactory().createModData(this, modName, e_modName, modDir, isLightweight);
    }

    //

    private class ModsLock {
        // EMPTY
    }
    private final ModsLock modsLock;
    private final WeakHashMap<String, TargetCacheImpl_DataMod> mods;

    @Trivial
    protected Map<String, TargetCacheImpl_DataMod> getMods() {
        return mods;
    }

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

    //

    private class ConsLock {
        // EMPTY
    }
    private final ConsLock consLock;
    private final WeakHashMap<String, TargetCacheImpl_DataCon> cons;

    public TargetCacheImpl_DataCon getConForcing(String conPath) {
        synchronized( consLock ) {
            TargetCacheImpl_DataCon con = cons.get(conPath);
            if ( con == null ) {
                con = createSimpleConData(conPath);
                cons.put(conPath, con);
            }
            return con;
        }
    }

    @Trivial
    public TargetCacheImpl_DataCon createSimpleConData(String conName) {
        String e_conName = encode(conName);
        File e_resultConDir = e_getConDir(e_conName);
        return createConData(this, conName, e_conName, e_resultConDir);
    }

    //

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

    protected final UtilImpl_PoolExecutor writePool;

    @Trivial
    protected UtilImpl_PoolExecutor getWritePool() {
        return writePool;
    }

    @Trivial
    protected boolean isWriteSynchronous() {
    	return ( getWritePool() == null );
    }

    @Trivial
    protected void scheduleWrite(
    	TargetCacheImpl_DataMod modData,
    	final ScheduleCallback writer, String description) {

        String methodName = "scheduleWrite";

        final Throwable scheduler =
            new Throwable(
            	"App [ " + getName() + " ] [ " + e_getName() + " ]" +
            	" Mod [ " + modData.getName() + " ] [ " + modData.e_getName() + " ]" +
            	" [ " + description + " ]");

        UtilImpl_PoolExecutor useWritePool = getWritePool();
        if ( useWritePool == null ) {
            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "ENTER (immediate) [ {0} ] [ {1} ] [ {2} ]",
                    new Object[] { getName(), writer, description });
            }

            writer.execute();

            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "RETURN (immediate) [ {0} ] [ {1} ] [ {2} ]",
                    new Object[] { getName(), writer, description });
            }

        } else {
            Runnable writeRunner = new Runnable() {
                @Override
                public void run() {
                    String methodName = "scheduleWrite.run";
                    try {
                        // System.out.println("BEGIN write [ " + writer + " ]");
                        writer.execute();
                        // System.out.println("END write [ " + writer + " ]");

                    } catch ( RuntimeException e ) {
                        // Capture and display any exception from the spawned writer thread.
                        // Without this added step information about the spawning thread is
                        // lost, making debugging writer problems very difficult.

                        logger.logp(Level.WARNING, CLASS_NAME, methodName, "Caught Asynchronous exception", e);
                        logger.logp(Level.WARNING, CLASS_NAME, methodName, "Scheduler", scheduler);
                        logger.logp(Level.WARNING, CLASS_NAME, methodName, "Synchronization error", e);

                        throw e;
                    }
                }
            };

            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "ENTER (scheduled) [ {0} ] [ {1} ] [ {2} ]",
                    new Object[] { getName(), writer, description });
            }

            useWritePool.execute(writeRunner);

            if ( logger.isLoggable(Level.FINER) ) {
                logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "RETURN (scheduled) [ {0} ] [ {1} ] [ {2} ]",
                    new Object[] { getName(), writer, description });
            }
        }
    }
}
