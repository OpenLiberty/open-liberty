/*******************************************************************************
 * Copyright (c) 2017, 2025 IBM Corporation and others.
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

		// Manage apps as a strong collection.  There is an explicit
		// API which must be used to release application data.

        this.appsLock = new AppsLock();
        this.apps = new HashMap<String, TargetCacheImpl_DataApp>();
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

    protected static class AppsLock {
        // EMPTY
    }
    private final AppsLock appsLock;
    private final Map<String, TargetCacheImpl_DataApp> apps;

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
            // getFactory().logStack("Forcing app [ " + appName + " ]"); // 30315

            return apps.computeIfAbsent(appName, this::createAppData);
        }
    }

    /**
     * Release cache data for an application.
     * 
     * @param appName The name of the application.
     */
    public boolean release(String appName) {
        synchronized ( appsLock ) {
            return ( apps.remove(appName) != null );
        }
    }
}
