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
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;

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

    public static final String CLASS_NAME = TargetCacheImpl_DataApp.class.getSimpleName();

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
    }

    //

    private TargetCacheImpl_DataApps apps;

    public TargetCacheImpl_DataApps getApps() {
        return apps;
    }

    //

    @Trivial
    protected TargetCacheImpl_DataMod createModData(String modName) {
        String e_modName = encode(modName);
        return createModData( modName, e_modName, e_getModDir(e_modName) );
    }

    @Trivial
    public File e_getModDir(String e_modName) {
        return getDataFile( e_addModPrefix(e_modName) );
    }

// Currently unused
//
//    @Trivial
//    protected TargetCacheImpl_DataMod createModData(File modDir) {
//        String e_modDirName = modDir.getName();
//        String e_modName = e_removeModPrefix(e_modDirName);
//        String modName = decode(e_modName);
//
//        return createModData(modName, e_modName, modDir);
//    }

    //

    @Trivial
    protected TargetCacheImpl_DataMod createModData(String modName, String e_modName, File modDir) {
        return getFactory().createModData(this, modName, e_modName, modDir);
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

    public TargetCacheImpl_DataMod getModForcing(String modName) {
        // Unnamed modules always create new data.
        if ( modName == ClassSource_Factory.UNNAMED_MOD ) {
            return createModData(modName);
        }

        synchronized( modsLock ) {
            TargetCacheImpl_DataMod mod = mods.get(modName);
            if ( mod == null ) {
                mod = createModData(modName);
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
                con = createConData(conPath);
                cons.put(conPath, con);
            }
            return con;
        }
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
}
