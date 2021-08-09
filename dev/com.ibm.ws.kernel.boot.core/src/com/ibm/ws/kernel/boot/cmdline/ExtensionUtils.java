/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.cmdline;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import com.ibm.ws.kernel.provisioning.ProductExtension;
import com.ibm.ws.kernel.provisioning.ProductExtensionInfo;

/**
 * ExtensionUtils: Provides helper methods in order to plug in some extension classes
 * for the command line utilities.
 */
public class ExtensionUtils {

    static private final String EXTENSION_DIR = "bin/tools/extensions/";
    static private final String USER_EXTENSION_DIR = "usr/extension/" + EXTENSION_DIR;

    /**
     * Find Required bundles from the manifest file which matches extension name..
     * returns list of RequredBundle object, if there is none, return null.
     * the directories which the code looks into are:
     * 1, wlp/bin/tools/extensions,
     * 2. wlp/usr/extension/bin/tools/extensions,
     * 3. all locations returned from ProductExtension.getProductExtensions() with /bin/tools/extensions
     */
    static List<LaunchManifest.RequiredBundle> findExtensionBundles(String extension) throws IOException {
        // first, make the list of directories to look into.
        List<File> dirs = listToolExtensionDirectories(extension);
        List<LaunchManifest.RequiredBundle> list = findExtensionBundles(dirs);
        return list;
    }

    /**
     * Compose a list which contains all of directories which need to look into.
     * The directory may or may not exist.
     * the list of directories which this method returns is:
     * 1, wlp/bin/tools/extensions,
     * 2. wlp/usr/extension/bin/tools/extensions,
     * 3. all locations returned from ProductExtension.getProductExtensions() with /bin/tools/extensions
     */
    static private List<File> listToolExtensionDirectories(String extension) {
        List<File> dirs = new ArrayList<File>();
        dirs.add(new File(Utils.getInstallDir(), EXTENSION_DIR + extension)); // wlp/bin/tools/extensions
        dirs.add(new File(Utils.getInstallDir(), USER_EXTENSION_DIR + extension)); // wlp/usr/extension/bin/tools/extensions
        List<File> extDirs = listProductExtensionDirectories();
        // add extension directory path and extension name.
        for (File extDir : extDirs) {
            dirs.add(new File(extDir, EXTENSION_DIR + extension));
        }
        return dirs;
    }

    /**
     * List all of product extension directory.
     * It returns empty List object if there is no product extension.
     */
    static List<File> listProductExtensionDirectories() {
        List<File> dirs = new ArrayList<File>();
        for (ProductExtensionInfo info : ProductExtension.getProductExtensions()) {
            // there are some fat tests which constructs incorrect ProductExtensionInfo.
            // in order to avoid the test failure, check null object and skip it if it is null.
            String loc = info.getLocation();
            if (loc != null) {
                File extensionDir = new File(info.getLocation());
                if (!extensionDir.isAbsolute()) {
                    File parentDir = Utils.getInstallDir().getParentFile();
                    extensionDir = new File(parentDir, info.getLocation());
                }
                dirs.add(extensionDir);
            }
        }
        return dirs;
    }

    /**
     * Find Required bundles from the manifest file underneath specified directory recursively.
     * this method does not expect that the parameters are null.
     * returns list of RequredBundle object.
     */
    static private List<LaunchManifest.RequiredBundle> findExtensionBundles(List<File> dirs) throws IOException {
        List<LaunchManifest.RequiredBundle> list = new ArrayList<LaunchManifest.RequiredBundle>();
        for (File dir : dirs) {
            if (dir.exists()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (!file.isDirectory() && file.getName().toLowerCase().endsWith(".jar")) {
                            String bundles = getRequiredBundles(file);
                            if (bundles != null) {
                                list.addAll(LaunchManifest.parseRequireBundle(bundles));
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

    /**
     * find the Require-Bundle name from the specified jar file.
     */

    static private String getRequiredBundles(File file) throws IOException {
        JarFile jar = new JarFile(file);
        Attributes attr = jar.getManifest().getMainAttributes();
        jar.close();
        return attr.getValue("Require-Bundle");
    }
}