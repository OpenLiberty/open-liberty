/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.install.map;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
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

    public InstallMap() {
        this.locale = null;
        this.resourceBundle = null;

        this.inputData = new HashMap<String, Object>();
        this.outputData = null;
    }

    //

    private Locale locale;
    private ResourceBundle resourceBundle;

    private void setLocale(Object locale) {
        setInput(MESSAGE_LOCALE, locale, Locale.class);
        this.locale = (Locale) locale;
    }

    private String getMessage(String key, Object... args) {
        if ( resourceBundle == null ) {
            if ( locale == null ) {
                locale = Locale.getDefault();
            }
            resourceBundle = ResourceBundle.getBundle("com.ibm.ws.install.internal.resources.InstallMapMessages", locale);
        }

        String message = resourceBundle.getString(key);
        if ( args.length == 0 ) {
            return message;
        }

        MessageFormat messageFormat = new MessageFormat(message, locale);
        return messageFormat.format(args);
    }

    //

    @Override
    public void clear() {
        clearData();
        clearLoader();
    }

    //

    private URLClassLoader loader;

    private void clearLoader() {
        if ( loader == null ) {
            return;
        }

        URLClassLoader useLoader = loader;
        loader = null;
        try {
            useLoader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void setOutput(File installDir) throws Exception {
        URL[] jars = getJars(installDir);
        loader = new URLClassLoader(jars, getClass().getClassLoader());
        outputData = AccessController.doPrivileged(
            new PrivilegedExceptionAction<Map<String, Object>>() {
                @SuppressWarnings("unchecked")
                @Override
                public Map<String, Object> run() throws Exception {
                    Class<Map<String, Object>> clazz;
                    clazz = (Class<Map<String, Object>>)
                        loader.loadClass("com.ibm.ws.install.internal.InstallKernelMap");
                    return clazz.newInstance();
                }
            });
    }

    // Keys

    public static final String RUNTIME_INSTALL_DIR = "runtime.install.dir";
    public static final String INSTALL_KERNEL_INIT_CODE = "install.kernel.init.code";
    public static final String INSTALL_MAP_JAR = "install.map.jar";
    public static final String INSTALL_MAP_JAR_FILE = "install.map.jar.file";
    public static final String OVERRIDE_JAR_BUNDLES = "override.jar.bundles";
    public static final String INSTALL_KERNEL_INIT_ERROR_MESSAGE = "install.kernel.init.error.message";
    public static final String MESSAGE_LOCALE = "message.locale";

    // Return code

    public static final Integer ERROR = Integer.valueOf(1);

    // Unsupported operations ...

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set entrySet() {
        throw new UnsupportedOperationException();
    }

    private final Map<String, Object> inputData;
    private Map<String, Object> outputData;

    private Object transferIn(String key) {
        Object outputDatum = outputData.get(key);
        inputData.put(key, outputDatum);
        return outputDatum;
    }

    private void transferInputs() {
        for ( String key : inputData.keySet() ) {
            transferOut(key);
        }
    }

    private Integer transferResults() {
        Integer rc = (Integer) transferIn(INSTALL_KERNEL_INIT_CODE);
        transferIn(INSTALL_KERNEL_INIT_ERROR_MESSAGE);
        return rc;
    }

    private Object transferOut(String key) {
        Object outputDatum = outputData.get(key);
        inputData.put(key, outputDatum);
        return outputDatum;
    }

    private void clearData() {
        inputData.clear();
        outputData = null;
    }

    private String putError(String msgKey, Object... msgArgs) {
        basicPut(INSTALL_KERNEL_INIT_CODE, ERROR);

        String message = getMessage(msgKey, msgArgs);
        basicPutError(message);
        return message;
    }

    private void basicPutError(String message) {
        basicPut(INSTALL_KERNEL_INIT_ERROR_MESSAGE, message);
    }

    private Object setInput(String key, Object value) {
        if ( RUNTIME_INSTALL_DIR.equals(key) ) {
            return setInput(RUNTIME_INSTALL_DIR, value, File.class);
        } else if ( INSTALL_MAP_JAR.equals(key) ) {
            return setInput(INSTALL_MAP_JAR, value, String.class);
        } else if ( INSTALL_MAP_JAR_FILE.equals(key) ) {
            return setInput(INSTALL_MAP_JAR_FILE, value, File.class);
        } else if ( OVERRIDE_JAR_BUNDLES.equals(key) ) {
            return setInput(OVERRIDE_JAR_BUNDLES, value, List.class);
        } else {
            throw new RuntimeException( putError("MAPBASED_ERROR_KERNEL_NOT_INIT") );
        }

    }

    private Object setInput(String key, Object value, Class<?> requiredType) {
        if ( (value != null) && !requiredType.isAssignableFrom( value.getClass() ) ) {
            throw new IllegalArgumentException(
                "Key [ " + key + " ]" +
                " requires an instance of type [ " + requiredType.getName() + " ]" +
                " but was given an instance of [ " + value.getClass().getName() + " ]");
        }
        return basicPut(key, value);
    }

    private Object basicPut(String key, Object value) {
        return inputData.put(key, value);
    }

    private Object basicGet(String key) {
        return inputData.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return inputData.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
        return inputData.isEmpty();
    }

    @Override
    public Object get(Object key) {
        String useKey = (String) key;

        if ( INSTALL_KERNEL_INIT_CODE.equals(useKey) ) {
            return initValidate();
        }

        if ( INSTALL_KERNEL_INIT_ERROR_MESSAGE.equals(useKey) ||
             RUNTIME_INSTALL_DIR.equals(useKey) ) {
            return basicGet(useKey);

        } else if ( outputData == null ) {
            throw new RuntimeException( getMessage("MAPBASED_ERROR_KERNEL_NOT_INIT") );

        } else {
            return outputData.get(useKey);
        }
    }

    @Override
    public Object put(Object key, Object value) {
        String useKey = (String) key;

        if ( MESSAGE_LOCALE.equals(useKey) ) {
            setLocale(value);
            if ( outputData != null ) {
                outputData.put(useKey, value);
            }
            return value;
        }

        if ( outputData == null ) {
            return setInput(useKey, value);

        } else {
            return outputData.put(useKey, value);
        }
    }

    private Integer initValidate() {
        File installDir = validateInstallDir();
        if ( installDir == null ) {
            return ERROR;
        }

        try {
            setOutput(installDir);
        } catch ( Exception e ) {
            putError("MAPBASED_ERROR_KERNEL_INIT_FAILED", e.getMessage());
            return ERROR;
        }

        transferInputs();

        return transferResults();
    }

    private File validateInstallDir() {
        File installDir = (File) basicGet(RUNTIME_INSTALL_DIR);
        if ( installDir == null ) {
            putError("MAPBASED_ERROR_RUNTIME_INSTALL_DIR_NOT_SET");
            return null;
        } else if ( !installDir.exists() ) {
            putError("MAPBASED_ERROR_RUNTIME_INSTALL_DIR_NOT_EXISTS", installDir);
            return null;
        } else if ( !installDir.isDirectory() ) {
            putError("MAPBASED_ERROR_RUNTIME_INSTALL_DIR_NOT_DIR", installDir);
            return null;
        } else {
            return installDir;
        }
    }

    //

    private URL[] getJars(File installDir) throws Exception {
        File mapFile = (File) basicGet(INSTALL_MAP_JAR_FILE);
        if ( mapFile == null ) {
            mapFile = new File( installDir, (String) basicGet(INSTALL_MAP_JAR) );
        }

        Manifest manifest;
        try ( JarFile installMapJar = new JarFile(mapFile) ) {
            manifest = installMapJar.getManifest();
        }
        Attributes attributes = manifest.getMainAttributes();
        String[] requireBundle = attributes.getValue("Require-Bundle").split(",");

        Map<String, File> overrideMap = getOverrideMap();

        List<URL> jarURLs = new ArrayList<URL>();

        for ( String requiredBundle : requireBundle ) {
            String[] bundle = requiredBundle.split(";");
            URL url = getURL(installDir, bundle);
            if ( url == null ) {
                continue;
            }

            if ( overrideMap == null ) {
                jarURLs.add(url);
                continue;
            }

            boolean found = false;
            for ( String bundleName : overrideMap.keySet() ) {
                if ( matchesBundle( url.toURI().toURL().toString(), bundleName ) ) {
                    found = true;
                    jarURLs.add( overrideMap.get(bundleName).toURI().toURL() );
                    overrideMap.put(bundleName, null);
                }
            }
            if ( !found ) {
                jarURLs.add(url);
            }
        }

        if ( overrideMap != null ) {
            for ( String remainingBundle : overrideMap.keySet() ) {
                if ( overrideMap.get(remainingBundle) != null ) {
                    jarURLs.add( overrideMap.get(remainingBundle).toURI().toURL() );
                }
            }
        }

        return jarURLs.toArray(new URL[jarURLs.size()]);
    }

    @SuppressWarnings("unchecked")
    private Map<String, File> getOverrideMap() {
        List<String> overrides = (List<String>) basicGet(OVERRIDE_JAR_BUNDLES);
        if ( overrides == null ) {
            return null;
        }

        Map<String, File> overrideMap = getBundleMap(overrides);
        if ( (overrideMap != null) && overrideMap.isEmpty() ) {
            overrideMap = null;
        }
        return overrideMap;
    }


    private URL getURL(File installDir, String[] bundle) {
        String fName = bundle[0].trim();
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
        Version min = minVersion;
        Version max = maxVersion;
        File[] jars = libDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return InstallMap.isJarInRange(name, fName, min, max);
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
        sort(jarsList, fName);
        try {
            return jarsList.get(jarsList.size() - 1).toURI().toURL();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private boolean matchesBundle(String urlPath, String bundleName) {
        String[] parts = urlPath.split("/");
        for ( String part : parts ) {
            if (!part.endsWith(".jar")) {
                continue;
            }

            String[] subParts = part.split("_");
            for ( String subPart : subParts ) {
                if ( subPart.equals(bundleName) ) {
                    return true;
                }
            }
        }

        return false;
    }

    private Map<String, File> getBundleMap(List<String> bundleList) {
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

    /**
     * Compare files in jarsList based on file name's length
     * Sort the files depending on the comparison
     *
     * @param jarsList -abstract representation of file/directory names
     * @param fName    - name of file
     */
    private static void sort(List<File> jarsList, String fName) {
        Collections.sort(jarsList, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                String f1Name = f1.getName();
                String f1Version = f1Name.substring(fName.length() + 1, f1Name.length() - 4);
                Version v1 = Version.createVersion(f1Version);

                String f2Name = f2.getName();
                String f2Version= f2Name.substring(fName.length() + 1, f2Name.length() - 4);
                Version v2 = Version.createVersion(f2Version);

                // TODO: What if one has a version and the other does not??
                // TODO: What if the two names are different??

                if ( (v1 != null) && (v2 != null) ) {
                    return v1.compareTo(v2);
                }
                return f1Name.compareTo(f2Name);
            }
        });
    }

    /**
     * This method compares the fName with name for a match and checks if it is a jar file
     * The file is accepted if either min/max is null or the newly created Version is null
     *
     * @param fName - file name
     * @param name  - string value
     * @param min   - Version
     * @param max   - Version
     * @return boolean value depending on acceptance
     */
    public static boolean isJarInRange(String fName, String name, Version min, Version max) {
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
}
