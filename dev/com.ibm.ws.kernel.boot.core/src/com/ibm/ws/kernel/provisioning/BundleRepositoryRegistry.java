/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.provisioning;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import com.ibm.ws.kernel.boot.cmdline.Utils;

/**
 * This is a registry of known Repositories
 */
public class BundleRepositoryRegistry {

    private static boolean allUseMsgs = true;
    private static String cacheServerName = null;
    private static boolean isClient = false;

    /**
     * Key is a string representation of the repository ("" for root, "usr" for user, or "extensionX" for extensions... ),
     * value is a BundleRepositoryHolder, which holds sufficient information to defer creation of the repository
     * until its contents are needed.
     */
    protected final static HashMap<String, BundleRepositoryHolder> repositoryHolders = new HashMap<String, BundleRepositoryHolder>();

    /**
     * Add the default repositories for the product
     * 
     * @param serverName If set to a serverName, a cache will be created in that server's workarea. A null value disables caching.
     * @param useMsgs This setting is passed on to the held ContentLocalBundleRepositories.
     */
    public static synchronized void initializeDefaults(String serverName, boolean useMsgs) {
        allUseMsgs = useMsgs;
        cacheServerName = serverName;
        addBundleRepository(Utils.getInstallDir().getAbsolutePath(), ExtensionConstants.CORE_EXTENSION);
        addBundleRepository(new File(Utils.getUserDir(), "/extension/").getAbsolutePath(), ExtensionConstants.USER_EXTENSION);
    }

    /**
     * Add the default repositories for the product
     * 
     * @param processName If set to a processName, a cache will be created in that process's workarea. A null value disables caching.
     * @param useMsgs This setting is passed on to the held ContentLocalBundleRepositories.
     * @param isClient This is true when the current process is client.
     */
    public static synchronized void initializeDefaults(String processName, boolean useMsgs, boolean isClient) {
        BundleRepositoryRegistry.isClient = isClient;
        BundleRepositoryRegistry.initializeDefaults(processName, useMsgs);
    }

    /**
     * Add a bundle repository to the map if one for that feature type has not
     * already been added.
     * 
     * @param installDir The install location for the repository. This can vary, i.e. product extensions.
     * @param featureType The "name" for this repository. "" for default, "usr" for the user extension, etc.
     */
    public synchronized static void addBundleRepository(String installDir, String featureType) {
        BundleRepositoryHolder bundleRepositoryHolder = new BundleRepositoryHolder(installDir, cacheServerName, featureType);

        if (!repositoryHolders.containsKey(featureType))
            repositoryHolders.put(featureType, bundleRepositoryHolder);
    }

    /**
     * @return The default product bundle repository
     * @see #initializeDefaults(boolean, boolean)
     */
    public synchronized static ContentBasedLocalBundleRepository getInstallBundleRepository() {
        BundleRepositoryHolder bh = repositoryHolders.get(ExtensionConstants.CORE_EXTENSION);
        return bh == null ? null : bh.getBundleRepository();
    }

    /**
     * @return The usr/extension bundle repository
     * @see #initializeDefaults(boolean, boolean)
     */
    public static ContentBasedLocalBundleRepository getUsrInstallBundleRepository() {
        BundleRepositoryHolder bh = repositoryHolders.get(ExtensionConstants.USER_EXTENSION);
        return bh == null ? null : bh.getBundleRepository();
    }

    /**
     * @param featureType The type or name of repository to find/return
     * @return The bundle repository for the given feature type or extension
     */
    public synchronized static BundleRepositoryHolder getRepositoryHolder(String featureType) {
        return repositoryHolders.get(featureType);
    }

    /**
     * Flush caches and release resources used for provisioning operations
     */
    public synchronized static void disposeAll() {
        for (BundleRepositoryHolder holder : repositoryHolders.values()) {
            holder.dispose();
        }
    }

    public synchronized static Collection<String> keys() {
        return new ArrayList<String>(repositoryHolders.keySet());
    }

    public synchronized static Collection<BundleRepositoryHolder> holders() {
        return new ArrayList<BundleRepositoryHolder>(repositoryHolders.values());
    }

    public synchronized static boolean isEmpty() {
        return repositoryHolders.isEmpty();
    }

    /**
     * BundleRepositoryHolder is used hold the information required to
     * create a cache-based ContentBasedLocalBundleRepository when it's contents.
     * are needed.
     */
    public static class BundleRepositoryHolder {
        protected final File installDir;
        protected final File cacheFile;
        protected final String featureType;

        protected ContentBasedLocalBundleRepository bundleRepository;

        private BundleRepositoryHolder(String installDir, String serverName, String featureType) {
            this.installDir = new File(installDir);
            this.cacheFile = serverName == null ? null : new File(Utils.getServerOutputDir(serverName, isClient), "workarea/platform/bundle/" + featureType + ".cache");
            this.featureType = featureType;
        }

        /**
         * Get bundle repository
         * 
         * @param locService
         *            a location service
         * @param msgs
         *            true if messages should be output to the log, false otherwise.
         * 
         * @return a bundle repository
         * 
         */
        public ContentBasedLocalBundleRepository getBundleRepository() {
            if (bundleRepository == null) {
                bundleRepository = new ContentBasedLocalBundleRepository(installDir, cacheFile, allUseMsgs);
            }
            return bundleRepository;
        }

        /**
         * This method is called once provisioning is over. It flushes the cache and clears up memory.
         * After this is called future calls to public methods will fail.
         */
        void dispose() {
            if (bundleRepository != null) {
                bundleRepository.dispose();
                bundleRepository = null;
            }
        }

        /**
         * @return
         */
        public String getInstallDir() {
            return installDir.getAbsolutePath();
        }

        /**
         * Gets the feature type (product extension name).
         * 
         * @return
         */
        public String getFeatureType() {
            return featureType;
        }
    }
}
