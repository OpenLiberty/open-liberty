/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package test.server.config;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class WorkareaReader {

    public WorkareaReader(String installRoot, String serverName) {
        this.installRoot = new File(installRoot);
        this.serverName = serverName;
        this.workareaRoot = new File(installRoot, "usr/servers/" + serverName + '/' + "workarea");

        this.cacheData = locateCache();
        this.cacheIds = readCacheIds();
    }

    private final File installRoot;
    private final String serverName;
    private final File workareaRoot;

    public String getInstallPath() {
        return installRoot.getPath();
    }

    public File getInstallRoot() {
        return installRoot;
    }

    public String getServerName() {
        return serverName;
    }

    public String getWorkareaPath() {
        return workareaRoot.getPath();
    }

    public File getWorkareaRoot() {
        return workareaRoot;
    }

    private static class CacheData {
        public final String pluginName;
        public final File cacheRoot;

        public CacheData(String pluginName, File cacheRoot) {
            this.pluginName = pluginName;
            this.cacheRoot = cacheRoot;
        }
    }

    private final CacheData cacheData;

    public String getPluginName() {
        return ((cacheData == null) ? null : cacheData.pluginName);
    }

    public String getCachePath() {
        File useCacheRoot = getCacheRoot();
        return ((useCacheRoot == null) ? null : useCacheRoot.getPath());
    }

    public File getCacheRoot() {
        return ((cacheData == null) ? null : cacheData.cacheRoot);
    }

    // wlp/usr/servers/test/workarea/org.eclipse.osgi/47/data:
    //   cache/<app-cache-id>
    //   cacheAdapt/<app-cache-id>
    //   cacheOverlay/<app-cache-id>

    private static final File[] EMPTY_FILES = new File[] {};

    private static File[] listFiles(File parentFile) {
        if (parentFile == null) {
            return EMPTY_FILES;
        }
        File[] children = parentFile.listFiles();
        return ((children == null) ? EMPTY_FILES : children);
    }

    private static final String[] EMPTY_FILE_NAMES = new String[] {};

    private static String[] list(File parentFile) {
        if (parentFile == null) {
            return EMPTY_FILE_NAMES;
        }
        String[] children = parentFile.list();
        return ((children == null) ? EMPTY_FILE_NAMES : children);
    }

    private CacheData locateCache() {
        String cacheParent = workareaRoot + "/org.eclipse.osgi";
        File cacheParentFile = new File(cacheParent);
        System.out.println("Examining [ " + cacheParentFile.getAbsolutePath() + " ]");
        for (File plugin : listFiles(cacheParentFile)) {
            // String name = plugin.getName();
            if (!plugin.isDirectory()) {
                // System.out.println("Skipping [ " + name + " ]: Not a directory");
                continue;
            }
            // System.out.println("Examining [ " + name + " ]");

            File data = null;
            for (File pluginChild : listFiles(plugin)) {
                String childName = pluginChild.getName();
                if (childName.equals("data") && pluginChild.isDirectory()) {
                    // System.out.println("Examining [ " + name + '/' + childName + " ]");
                    data = pluginChild;
                    break;
                } else {
                    // System.out.println("Skipping [ " + name + '/' + childName + " ]: Not data");
                }
            }
            if (data == null) {
                continue;
            }

            File cache = null;
            boolean adaptFound = false;
            boolean overlayFound = false;

            for (String dataChild : list(data)) {
                if ((cache == null) && dataChild.equals("cache")) {
                    // System.out.println("Located cache [ " + dataChild + " ]");
                    cache = new File(data, dataChild);
                } else if (!adaptFound && dataChild.equals("cacheAdapt")) {
                    // System.out.println("Located cache adapt [ " + dataChild + " ]");
                    adaptFound = true;
                } else if (!overlayFound && dataChild.equals("cacheOverlay")) {
                    // System.out.println("Located cache overlay [ " + dataChild + " ]");
                    overlayFound = true;
                }
                if ((cache != null) && adaptFound && overlayFound) {
                    break;
                }
            }

            if ((cache == null) || !adaptFound || !overlayFound) {
                // System.out.println("Cache not located; continuing");
                continue;
            }

            String pluginName = plugin.getName();

            System.out.println("Plugin [ " + pluginName + " ]");
            System.out.println("Cache root [ " + cache.getAbsolutePath() + " ]");

            return new CacheData(pluginName, cache);
        }

        System.out.println("Cache not found");
        return null;
    }

    private final Set<String> cacheIds;

    public Set<String> getCacheIds() {
        return cacheIds;
    }

    // wlp/usr/servers/test/workarea/org.eclipse.osgi/47/data
    //   cache/<app-cache-id>

    private Set<String> readCacheIds() {
        File useCacheRoot = getCacheRoot();
        return ((useCacheRoot == null) ? null : asSet(list(useCacheRoot)));
    }

    private static Set<String> asSet(String... values) {
        if (values.length == 0) {
            return Collections.emptySet();
        } else {
            Set<String> set = new HashSet<>(values.length);
            for (String value : values) {
                set.add(value);
            }
            return set;
        }
    }
}
