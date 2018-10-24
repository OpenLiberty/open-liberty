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
import java.util.WeakHashMap;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.anno.classsource.ClassSource_Factory;
import com.ibm.wsspi.anno.targets.cache.TargetCache_ExternalConstants;

/**
 * Root of annotation caching data.
 *
 * Annotation cache data has weakly held application data which is keyed
 * by application name.
 * 
 * The annotations cache as a whole is stored beneath a single root directory,
 * which is provided by options which are held by the cache factory.
 *
 * Each application has its own directory, relative to the root cache directory,
 * and named using the standard pattern:
 * <code>
 *     rootFolder + APP_PREFIX + encode(appName)
 * </code>
 *
 * See {@link TargetCache_ExternalConstants#APP_PREFIX} and 
 * {@link TargetCacheImpl_Utils#encodePath(String)}.
 * 
 * Application data is held weakly: Application data is not retained without a
 * reference to the application data outside of the annotation cache.
 */
public class TargetCacheImpl_DataApps extends TargetCacheImpl_DataBase {
    // private static final String CLASS_NAME = TargetCacheImpl_DataApps.class.getSimpleName();

    //

    public TargetCacheImpl_DataApps(
        TargetCacheImpl_Factory factory,
        String cacheName, String e_cacheName) {

        super( factory,
               cacheName, e_cacheName,
               new File( factory.getCacheOptions().getDir() ) );

        this.appsLock = new AppsLock();
        this.apps = new WeakHashMap<String, TargetCacheImpl_DataApp>();

        this.queriesLock = new QueriesLock();
        this.queries = new WeakHashMap<String, TargetCacheImpl_DataQueries>();
    }

    //

    @Trivial
    protected File e_getAppDir(String e_appName) {
        return getDataFile( e_addAppPrefix(e_appName) );
    }

    @Trivial
    protected TargetCacheImpl_DataApp createAppData(String appName) {
        String e_appName = encode(appName);
        return createAppData( appName, e_appName, e_getAppDir(e_appName) );
    }

// Currently unused
//
//    @Trivial
//    protected TargetCacheImpl_DataApp createAppData(File appDir) {
//        String appDirName = appDir.getName();
//        String e_appName = e_removeAppPrefix(appDirName);
//        String appName = decode(e_appName);
//
//        return createAppData(appName, e_appName, appDir);
//    }

    //

    @Trivial    
    protected TargetCacheImpl_DataApp createAppData(String appName, String e_appName, File appDir) {
        return getFactory().createAppData(this, appName, e_appName, appDir);
    }

    //

    private class AppsLock {
        // EMPTY
    }
    private final AppsLock appsLock;
    private final WeakHashMap<String, TargetCacheImpl_DataApp> apps;

    public TargetCacheImpl_DataApp getAppForcing(String appName) {
        // Unnamed applications always create new data.
        if ( appName == ClassSource_Factory.UNNAMED_APP ) {
            return createAppData(appName);
        }

        synchronized( appsLock ) {
            TargetCacheImpl_DataApp app = apps.get(appName);
            if ( app == null ) {
                app = createAppData(appName);
                apps.put(appName, app);
            }
            return app;
        }
    }

    //

    @Trivial
    protected File e_getModDir(String e_appName, String e_modName) {
        if ( (e_appName == null) || (e_modName == null) ) {
            return null;
        }
        return new File( e_getAppDir(e_appName), e_addModPrefix(e_modName) );
    }

    @Trivial    
    protected TargetCacheImpl_DataQueries createQueriesData(
        String appName, String e_appName,
        String modName, String e_modName,
        File modDir) {

        return getFactory().createQueriesData(
            appName, e_appName,
            modName, e_modName, modDir);
    }

    @Trivial
    protected TargetCacheImpl_DataQueries createQueriesData(String appName, String modName) {
        String e_appName = encode(appName);
        String e_modName = encode(modName);

        return createQueriesData(
            appName, e_appName,
            modName, e_modName, e_getModDir(e_appName, e_modName) );
    }

    //

    private class QueriesLock {
        // EMPTY
    }
    private final QueriesLock queriesLock;
    private final WeakHashMap<String, TargetCacheImpl_DataQueries> queries;

    public TargetCacheImpl_DataQueries getQueriesForcing(String appName, String modName) {
        // Unnamed data always create new data.
        if ( (appName == ClassSource_Factory.UNNAMED_APP) ||
             (modName == ClassSource_Factory.UNNAMED_MOD) ) {
            return createQueriesData(appName, modName);
        }

        String queryKey = appName + '!' + modName;

        synchronized( queriesLock ) {
            TargetCacheImpl_DataQueries queriesData = queries.get(queryKey);
            if ( queriesData == null ) {
                queriesData = createQueriesData(appName, modName);
                queries.put(queryKey, queriesData);
            }
            return queriesData;
        }
    }
}
