/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.install.map;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * This is a loosely-typed interface onto the Install Kernel, it allows the installation from
 * a program that is not able to make direct java calls. The idea is the calling code would
 * create a URLClassLoader to the wlp/lib/com.ibm.ws.install.map.jar and then instanciate this
 * class by the name "com.ibm.ws.install.InstallMap". Once instanciated it initiates
 * the install via the use of the put and get methods on the map. All other methods throw
 * UnsupportedOperationException.
 *
 * <p>This class consists of several keys which can be put. The keys that can be put are:
 * <ul>
 * <li><b>action.install</b>
 * <ul><li>A java.util.List that contains the list of asset ids to install</li>
 * <li>A java.io.File that represents the location of the esa to install</li>
 * <li>An error occurs if null is returned. Get "action.error.message" to find out why the requested
 * action failed. </li></ul>
 * </li>
 * <li><b>action.uninstall</b>
 * <ul><li>A java.lang.List that indicates the list of feature ids to be uninstalled.</li></ul>
 * </li>
 * <li><b>dowload.external.deps</b>
 * <ul><li>A java.lang.Boolean that specifies whether to download any external dependencies.</li>
 * <li>If it is not set, the default is true.</li></ul>
 * </li>
 * <li><b>license.accept</b>
 * <ul><li>A java.lang.Boolean representing true. This is required in order to install.</li></ul>
 * </li>
 * <li><b>message.locale</b>
 * <ul><li>A java.util.Locale that identifies the locale for all messages (e.g. action.error.message, install.kernel.init.message, etc.)</li></ul>
 * </li>
 * <li><b>progress.monitor.cancelled</b>
 * <ul><li>A java.util.List that indicates whether the action was canceled.</li>
 * <li>The install kernel will do a get before each unit of work. The get method return a Boolean value. If the value is true, the install kernel will abort the operation and roll
 * everything back.</li></ul>
 * </li>
 * <li><b>progress.monitor.message</b>
 * <ul><li>A java.util.List that shows the status of the current operation.</li>
 * <li>The install kernel will call the add method on the list with a String representing the current status (e.g. "Downloading adminCenter-1.0...").</li></ul>
 * </li>
 * <li><b>repositories.propertie</b>
 * <ul><li>A java.io.File that indicates the path to the respositories.properties file.</li></ul>
 * </li>
 * <li><b>runtime.install.dir</b>
 * <ul><li>A java.io.File that indicates the path where the runtime is installed</li></ul>
 * </li>
 * <li><b>target.user.directory</b>
 * <ul><li>A java.io.File object that indicates which user directory to install sample content into.</li>
 * <li>If unspecified, the default behaviour is used, which checks for server.env files, WLP_USER_DIR environment variables, and failing that, falls back to the 'usr' subdirectory
 * in the runtime install directory.</li></ul>
 * </li>
 * <li><b>user.agent</b>
 * <ul><li>A java.lang.String that indicates the user information.</li>
 * <li>If it is not set, the default is "InstallMap".</li></ul>
 * </li>
 * </ul>
 * </p>
 *
 * <p>Several values can be got from this map. Calling get on some of these is unnecessary but advised.
 * <ul>
 * <li><b>action.error.message</b>
 * <ul><li>A java.lang.String for a friendly translated message that indicates why the requested action failed.</li></ul>
 * </li>
 * <li><b>action.install.result</b>
 * <ul><li>A Map&lt;String, Collection&lt;String&gt;&gt; that contains the installed asset names,
 * where the keys can be "addon", "feature", "ifix", "sample", "productsample", or "opensource".</li></ul>
 * </li>
 * <li><b>action.result</b>
 * <ul><li>A java.lang.Integer</li>
 * <li>If the install kernel was built correctly it will initiate the requested action.</li>
 * <li>Once the request has completed it will return a java.lang.Integer with a code indicating how it functioned.
 * <ul><li>A null value indicates the action requested is invalid</li>
 * <li>An integer value of 0 indicates everything went well</li>
 * <li>An integer value of -1 indicates the operation was cancelled</li>
 * <li>An integer value of 1 indicates an error</li></ul>
 * </li></ul>
 * </li>
 * <li><b>install.kernel.init.code</b>
 * <ul><li>A java.lang.Integer indicating the error code for creating the install kernel.
 * If this is zero, the install kernel was created correctly. If it is non-zero, an error occurred.</li></ul>
 * </li>
 * <li><b>install.kernel.init.error.message</b>
 * <ul><li>A java.lang.String for a friendly translated message that indicates why the install kernel failed to create.</li></ul>
 * </li>
 * <li><b>progress.monitor.size</b>
 * <ul><li>A java.lang.Integer indicating the progress monitor total unit of work that will allow the calling code to display the progress monitor correctly.</li></ul>
 * </li>
 * </ul>
 * </p>
 */
@SuppressWarnings("rawtypes")
public class InstallMap implements Map {

    // Keys
    private static final String RUNTIME_INSTALL_DIR = "runtime.install.dir";
    private static final String INSTALL_KERNEL_INIT_CODE = "install.kernel.init.code";
    private static final String INSTALL_MAP_JAR = "install.map.jar";
    private static final String INSTALL_MAP_JAR_FILE = "install.map.jar.file";
    private static final String OVERRIDE_JAR_BUNDLES = "override.jar.bundles";
    private static final String INSTALL_KERNEL_INIT_ERROR_MESSAGE = "install.kernel.init.error.message";
    private static final String MESSAGE_LOCALE = "message.locale";

    // Return code
    private static final Integer ERROR = Integer.valueOf(1);

    private final Map data = new HashMap();
    private Map installKernelMap = null;

    private Locale locale;
    private ResourceBundle messagesRes;

    public InstallMap() {}

    private String getMessage(String key, Object... args) {
        if (messagesRes == null) {
            if (locale == null)
                locale = Locale.getDefault();
            messagesRes = ResourceBundle.getBundle("com.ibm.ws.install.internal.resources.InstallMapMessages", locale);
        }
        String message = messagesRes.getString(key);
        if (args.length == 0)
            return message;
        MessageFormat messageFormat = new MessageFormat(message, locale);
        return messageFormat.format(args);
    }

    /**
     * Unsupported operation
     */
    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * return true if object is empty
     */

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * {@inheritDoc}
     * return true if object contains the input key
     */
    @Override
    public boolean containsKey(Object key) {
        return data.containsKey(key);
    }

    /**
     * Unsupported operation
     */
    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation
     */
    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation
     */
    @Override
    public void putAll(Map m) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation
     */
    @Override
    public Set keySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation
     */
    @Override
    public Collection values() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation
     */
    @Override
    public Set entrySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * return object value associated with input key
     */
    @Override
    public Object get(Object key) {
        if (INSTALL_KERNEL_INIT_CODE.equals(key))
            return initValidate();
        if (INSTALL_KERNEL_INIT_ERROR_MESSAGE.equals(key) ||
            RUNTIME_INSTALL_DIR.equals(key))
            return data.get(key);
        if (installKernelMap == null) {
            throw new RuntimeException(getMessage("MAPBASED_ERROR_KERNEL_NOT_INIT"));
        }
        return installKernelMap.get(key);
    }

    /**
     * {@inheritDoc}
     *
     * @return the value, or null if an error occurs.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object put(Object key, Object value) {
        if (MESSAGE_LOCALE.equals(key)) {
            if (value instanceof Locale) {
                locale = (Locale) value;
                data.put(MESSAGE_LOCALE, value);
            } else {
                throw new IllegalArgumentException();
            }
            if (installKernelMap != null)
                installKernelMap.put(key, value);
            return value;
        }
        if (installKernelMap == null) {
            if (RUNTIME_INSTALL_DIR.equals(key)) {
                if (value instanceof File) {
                    return data.put(RUNTIME_INSTALL_DIR, value);
                } else {
                    throw new IllegalArgumentException();
                }
            } else if (INSTALL_MAP_JAR.equals(key)) {
                if (value instanceof String) {
                    return data.put(INSTALL_MAP_JAR, value);
                } else {
                    throw new IllegalArgumentException();
                }
            } else if (INSTALL_MAP_JAR_FILE.equals(key)) {
                if (value instanceof File) {
                    return data.put(INSTALL_MAP_JAR_FILE, value);
                } else {
                    throw new IllegalArgumentException();
                }
            } else if (OVERRIDE_JAR_BUNDLES.equals(key)) {
                if (value instanceof List) {
                    return data.put(OVERRIDE_JAR_BUNDLES, value);
                } else {
                    throw new IllegalArgumentException();
                }
            } else {
                data.put(INSTALL_KERNEL_INIT_CODE, ERROR);
                data.put(INSTALL_KERNEL_INIT_ERROR_MESSAGE, getMessage("MAPBASED_ERROR_KERNEL_NOT_INIT"));
                throw new RuntimeException(getMessage("MAPBASED_ERROR_KERNEL_NOT_INIT"));
            }
        } else {
            return installKernelMap.put(key, value);
        }
    }

    /**
     * Compare files in jarsList based on file name's length
     * Sort the files depending on the comparison
     *
     * @param jarsList -abstract representation of file/directory names
     * @param fName - name of file
     */
    public static void sortFile(List<File> jarsList, final String fName) {
        Collections.sort(jarsList, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                String f1Name = f1.getName();
                f1Name = f1Name.substring(fName.length() + 1, f1Name.length() - 4);
                String f2Name = f2.getName();
                f2Name = f2Name.substring(fName.length() + 1, f2Name.length() - 4);
                Version v1 = Version.createVersion(f1Name);
                Version v2 = Version.createVersion(f2Name);
                if (v1 != null && v2 != null)
                    return v1.compareTo(v2);
                return f1Name.compareTo(f2Name);
            }
        });
    }

    /**
     * This method compares the fName with name for a match and checks if it is a jar file
     * The file is accepted if either min/max is null or the newly created Version is null
     *
     * @param fName - file name
     * @param name - string value
     * @param min - Version
     * @param max - Version
     * @return boolean value depending on acceptance
     */
    public static boolean accept(String fName, String name, Version min, Version max) {
        if (fName.startsWith(name + "_") && fName.toLowerCase().endsWith(".jar")) {
            if (min == null || max == null)
                return true;
            int i = fName.indexOf("_");
            String versionStr = fName.substring(i + 1, fName.length() - 4);
            Version v = Version.createVersion(versionStr);
            if (v == null)
                return true;
            return v.compareTo(min) >= 0 && v.compareTo(max) < 0;
        }
        return false;
    }

    private URL getURL(File installDir, String[] bundle) {
        final String fName = bundle[0].trim();
        String location = null;
        Version minVersion = null;
        Version maxVersion = null;
        for (String b : bundle) {
            b = b.trim();
            if (b.toLowerCase().startsWith("location=")) {
                location = b.substring(9);
                location = location.replaceAll("\"", "");
                location = location.replaceAll("'", "");
                location = location.trim();
            }
            if (b.toLowerCase().startsWith("version=")) {
                int i = b.indexOf("[");
                if (i > 0) {
                    int j = b.indexOf(")");
                    if (j > 0) {
                        String rangeStr = b.substring(i + 1, j);
                        String range[] = rangeStr.split(",");
                        if (range.length > 1) {
                            minVersion = Version.createVersion(range[0].trim());
                            maxVersion = Version.createVersion(range[1].trim());
                        }
                    }
                }
            }
        }
        File libDir = new File(installDir, location == null ? "lib" : location);
        final Version min = minVersion;
        final Version max = maxVersion;
        File[] jars = libDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return InstallMap.accept(name, fName, min, max);
            }
        });

        if (jars == null || jars.length == 0)
            return null;
        else if (jars.length == 1) {
            try {
                return jars[0].toURI().toURL();
            } catch (MalformedURLException e) {
                return null;
            }
        }
        List<File> jarsList = Arrays.asList(jars);
        sortFile(jarsList, fName);
        try {
            return jarsList.get(jarsList.size() - 1).toURI().toURL();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @SuppressWarnings({ "unchecked" })
    private URL[] getJars(File installDir) {
        JarFile installKernelJar = null;
        List<URL> jarURLs = new ArrayList<URL>();
        try {
            File installKernelJarFile = (File) data.get(INSTALL_MAP_JAR_FILE);
            installKernelJar = new JarFile(installKernelJarFile != null ? installKernelJarFile : new File(installDir, (String) data.get(INSTALL_MAP_JAR)));
            List<String> overrideList = (List<String>) data.get(OVERRIDE_JAR_BUNDLES);
            boolean doOverride = overrideList != null;
            Map<String, File> overrideJarMap = null;
            if (doOverride) {
                overrideJarMap = getBundleFileMap(overrideList);
            }
            if (overrideJarMap == null) {
                doOverride = false;
            }
            Manifest manifest = installKernelJar.getManifest();
            Attributes attributes = manifest.getMainAttributes();
            String[] requireBundles = attributes.getValue("Require-Bundle").split(",");
            for (String requireBundle : requireBundles) {
                String[] bundle = requireBundle.split(";");
                URL url = getURL(installDir, bundle);
                if (url != null) {
                    if (doOverride) {
                        boolean jarFound = false;
                        for (String bundleName : overrideJarMap.keySet()) {
                            if (isBundleInUrl(url.toURI().toURL().toString(), bundleName)) {
                                jarFound = true;
                                jarURLs.add(overrideJarMap.get(bundleName).toURI().toURL());
                                overrideJarMap.put(bundleName, null);
                            }
                        }
                        if (!jarFound) {
                            jarURLs.add(url);
                        }
                    } else {
                        jarURLs.add(url);
                    }
                }
            }
            if (doOverride) {
                for (String remainingBundle : overrideJarMap.keySet()) {
                    if (overrideJarMap.get(remainingBundle) != null) {
                        jarURLs.add(overrideJarMap.get(remainingBundle).toURI().toURL());
                    }
                }
            }
        } catch (Exception e) {
            data.put(INSTALL_KERNEL_INIT_ERROR_MESSAGE, e.getMessage());
            return null;
        } finally {
            if (installKernelJar != null) {
                try {
                    installKernelJar.close();
                } catch (IOException e) {
                }
            }
        }
        return jarURLs.toArray(new URL[jarURLs.size()]);
    }

    private boolean isBundleInUrl(String urlPath, String bundleName) {
        boolean bundleInUrl = false;
        String[] splitBundlePath = urlPath.split("/");

        for (String item : splitBundlePath) {
            if (item.endsWith(".jar")) {
                String[] splitOnUnderscore = item.split("_");
                for (String str : splitOnUnderscore) {
                    if (str.equals(bundleName)) {
                        bundleInUrl = true;
                    }
                }
            }
        }
        return bundleInUrl;
    }

    private Map<String, File> getBundleFileMap(List<String> bundleList) {
        Map<String, File> bundleNameToFile = new HashMap<String, File>();

        for (String bundle : bundleList) {
            String[] bundleSplit = bundle.split(";");
            String bundleName = "";
            String bundleFilePath = "";
            for (String item : bundleSplit) {
                if (item.contains(".jar")) {
                    bundleFilePath = item;
                } else {
                    bundleName = item;
                }
            }
            if (bundleName != "" && bundleFilePath != "") {
                bundleNameToFile.put(bundleName, new File(bundleFilePath));
            }
        }
        return bundleNameToFile;
    }

    @SuppressWarnings("unchecked")
    private Integer initValidate() {
        File installDir = (File) data.get(RUNTIME_INSTALL_DIR);
        if (installDir == null) {
            data.put(INSTALL_KERNEL_INIT_ERROR_MESSAGE, getMessage("MAPBASED_ERROR_RUNTIME_INSTALL_DIR_NOT_SET"));
            return null;
        }
        if (!installDir.exists()) {
            data.put(INSTALL_KERNEL_INIT_ERROR_MESSAGE, getMessage("MAPBASED_ERROR_RUNTIME_INSTALL_DIR_NOT_EXISTS", installDir));
            return null;
        }
        if (!installDir.isDirectory()) {
            data.put(INSTALL_KERNEL_INIT_ERROR_MESSAGE, getMessage("MAPBASED_ERROR_RUNTIME_INSTALL_DIR_NOT_DIR", installDir));
            return null;
        }
        final URL[] jars = getJars(installDir);
        if (jars == null) {
            return ERROR;
        }
        try {
            installKernelMap = AccessController.doPrivileged(new PrivilegedExceptionAction<Map<String, Object>>() {
                @Override
                public Map<String, Object> run() throws Exception {
                    @SuppressWarnings("resource")
                    ClassLoader loader = new URLClassLoader(jars, getClass().getClassLoader());
                    Class<Map<String, Object>> clazz;
                    clazz = (Class<Map<String, Object>>) loader.loadClass("com.ibm.ws.install.internal.InstallKernelMap");
                    return clazz.newInstance();
                }
            });
        } catch (PrivilegedActionException e) {
            data.put(INSTALL_KERNEL_INIT_ERROR_MESSAGE, getMessage("MAPBASED_ERROR_KERNEL_INIT_FAILED", e.getMessage()));
            return ERROR;
        }
        for (Object key : data.keySet()) {
            installKernelMap.put(key, data.get(key));
        }
        Integer rc = (Integer) installKernelMap.get(INSTALL_KERNEL_INIT_CODE);
        data.put(INSTALL_KERNEL_INIT_CODE, rc);
        data.put(INSTALL_KERNEL_INIT_ERROR_MESSAGE, installKernelMap.get(INSTALL_KERNEL_INIT_ERROR_MESSAGE));
        return rc;
    }
}
