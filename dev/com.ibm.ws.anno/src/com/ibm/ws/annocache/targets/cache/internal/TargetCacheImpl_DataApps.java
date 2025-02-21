/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
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
package com.ibm.ws.annocache.targets.cache.internal;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.annocache.classsource.ClassSource_Factory;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_ExternalConstants;
import com.ibm.wsspi.annocache.targets.cache.TargetCache_Options;

/**
 * Root of annotation caching data.
 *
 * Annotation cache data weakly holds application data keyed by application name.
 *
 * Annotation cache data weakly holds module specific query data.  Query data is
 * keyed on application name and module name.
 *
 * TODO: Move the query data to be within the module data.
 *
 * Annotation cache data is thread safe.
 *
 * Annotation cache data is stored hierarchically: All data except this root data
 * has parent data.  All child data has the same factory as its parent.
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
 */
public class TargetCacheImpl_DataApps extends TargetCacheImpl_DataBase {
    // private static final String CLASS_NAME = TargetCacheImpl_DataApps.class.getSimpleName();

    //
    
    private final Map<String,AppKey> appNamesToKeys = new HashMap<String,AppKey>();

	/**
	 * Create new root cache data.
	 *
	 * Options of the cache data are obtained from the factory.
	 * See {@link TargetCacheImpl_Factory#getCacheOptions()}.
	 * 
	 * In particular, the parent folder of the cache data is obtained
	 * from the options.  See {@link TargetCache_Options#getDir()}.
	 * 
	 * @param factory The factory which created the cache data.
	 * @param cacheName The name of the cache data.  Usually "anno".
	 * @param e_cacheName The encoded name of the cache data.  Used
	 *     to name the root cache data folder.
	 */
    public TargetCacheImpl_DataApps(
        TargetCacheImpl_Factory factory,
        String cacheName, String e_cacheName) {

        super( factory,
               cacheName, e_cacheName,
               new File( factory.getCacheOptions().getDir() ) );

        this.appsLock = new AppsLock();
        this.apps = new HashMap<AppKey, TargetCacheImpl_DataApp>();

        this.queriesLock = new QueriesLock();
        this.queries = new WeakHashMap<String, TargetCacheImpl_DataQueries>();
    }

    //

    /**
     * Factory helper method: Create the file for an application.  The
     * file name uses the encoded application name plus the application
     * prefix and and the application suffix, and placed relative
     * to the applications directory.
     * 
     * See {@link TargetCache_ExternalConstants#APP_PREFIX} and
     * {@link TargetCache_ExternalConstants#APP_SUFFIX}.
     * 
     * @param e_appName The encoded application name.
     *
     * @return The data file for the application.
     */
    @Trivial
    protected File e_getAppDir(String e_appName) {
        return getDataFile( e_addAppPrefix(e_appName) );
    }

    /**
     * Factory helper method: Create data for an application.
     *
     * Encode the application name and generate an application
     * directory.
     *
     * Do not stor ethe new application data.
     *
     * @param appName The un-encoded application name.
     *
     * @return New cache data for the application.
     */
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

    /**
     * Factory method: Create cache data for an application.  Do
     * not store the application.
     *
     * @param appName The name of the application.
     * @param e_appName The encoded name of the application.
     * @param appDir The directory of the application.
     *
     * @return New application cache data.
     */
    @Trivial    
    protected TargetCacheImpl_DataApp createAppData(String appName, String e_appName, File appDir) {
        return getFactory().createAppData(this, appName, e_appName, appDir);
    }

    // Application cache data storage ...

    private class AppsLock {
        // EMPTY
    }
    private final AppsLock appsLock;
    private final HashMap<AppKey, TargetCacheImpl_DataApp> apps;

    /**
     * Obtain cache data for an application.
     * 
     * Create new data if the application is unnamed.
     * 
     * Otherwise, either retrieve data from the applications store,
     * or create and store new data, and return the new data.
     *
     * @param appName The name of the application.
     *
     * @return Cache data for the application.
     */
    public TargetCacheImpl_DataApp getAppForcing(String appName) {
        // Unnamed applications always create new data.
        if ( appName == ClassSource_Factory.UNNAMED_APP ) {
            return createAppData(appName);
        }

        synchronized( appsLock ) {
            AppKey key = appNamesToKeys.computeIfAbsent(appName, AppKey::new);
            TargetCacheImpl_DataApp app = apps.get(key);
            if ( app == null ) {
                app = createAppData(appName);
                apps.put(key, app);
            }
            return app;
        }
    }

    /**
     * Release cache data for an application.
     * 
     * @param appName The name of the application.
     */
    public boolean release(String appName) {
        synchronized( appsLock ) {
            if (appNamesToKeys.containsKey(appName)) {
                AppKey key = appNamesToKeys.remove(appName);
                return apps.remove(key) != null;
            }
            return false;
        }
    }

    //

    /**
     * Factory helper method: Obtain the cache directory for a module.
     *
     * Answer null if the application is unnamed or the module is unnamed.
     *
     * @param e_appName The encoded application name.
     * @param e_modName The encoded module name.
     *
     * @return The cache directory of the module.
     */
    @Trivial
    protected File e_getModDir(String e_appName, String e_modName) {
        if ( (e_appName == null) || (e_modName == null) ) {
            return null;
        }
        return new File( e_getAppDir(e_appName), e_addModPrefix(e_modName) );
    }

    //

    /**
     * Factory method: Create queries data for a module.
     *
     * @param appName The name of the application.
     * @param e_appName The encoded name of the application.
     * @param modName The name of the module.
     * @param e_modName The encoded name of the module.
     * @param modDir The module cache directory.
     *
     * @return New queries data for the module.
     */
    @Trivial
    protected TargetCacheImpl_DataQueries createQueriesData(
        String appName, String e_appName,
        String modName, String e_modName,
        File modDir) {

        return getFactory().createQueriesData(
            appName, e_appName,
            modName, e_modName, modDir);
    }

    /**
     * Factory method: Create queries data for a module.
     *
     * @param appName The name of the application.
     * @param e_appName The encoded name of the application.
     * @param modName The name of the module.
     * @param e_modName The encoded name of the module.
     *
     * @return New queries data for the module.
     */
    @Trivial
    protected TargetCacheImpl_DataQueries createQueriesData(String appName, String modName) {
        String e_appName = encode(appName);
        String e_modName = encode(modName);

        return createQueriesData(
            appName, e_appName,
            modName, e_modName, e_getModDir(e_appName, e_modName) );
    }

    // Query data storage.

    // TODO: Put the query data as weakly held data of module data.

    private class QueriesLock {
        // EMPTY
    }
    private final QueriesLock queriesLock;
    private final WeakHashMap<String, TargetCacheImpl_DataQueries> queries;

    /**
     * Obtain query cache data for an module.
     * 
     * Create new data if the application is unnamed or the module is unnamed.
     * 
     * Otherwise, either retrieve data from the queries store,
     * or create and store new data, and return the new data.
     *
     * @param appName The name of the application.
     * @param modName The name of the module.
     *
     * @return Query cache data for the module.
     */
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
    

    private static final class AppKey {
        private final String deploymentName;

        public AppKey(String deploymentName) {
            this.deploymentName = deploymentName;
        }

        public String getDeploymentName() {
            return deploymentName;
        }

        @Override
        public int hashCode() {
            if (deploymentName == null) {
                return 0;
            }
            
            return deploymentName.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            AppKey other = (AppKey) obj;
            
            if (deploymentName == null) {
                return other.getDeploymentName() == null;
            }

            return deploymentName.equals(other.getDeploymentName());
        }
    }

}
