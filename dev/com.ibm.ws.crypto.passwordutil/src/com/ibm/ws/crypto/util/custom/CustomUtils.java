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
package com.ibm.ws.crypto.util.custom;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.crypto.util.MessageUtils;
import com.ibm.ws.kernel.provisioning.ProductExtension;
import com.ibm.ws.kernel.provisioning.ProductExtensionInfo;

/**
 * CustomUtils: Provides helper methods in order to plug in some extension classes
 * for the command line utilities.
 */
public class CustomUtils {
    private static final Class<?> CLASS_NAME = CustomUtils.class;
    private static Logger logger = Logger.getLogger(CLASS_NAME.getCanonicalName(), MessageUtils.RB);

    public static final String CUSTOM_ENCRYPTION_DIR = "ws-customPasswordEncryption";
    static final String USER_FEATURE_DIR = "usr/extension/";

    private static final String RESOURCE_FILE_EXT = ".properties";
    private static final String JAR_FILE_EXT = ".jar";
    private static final String KEY_ALGORITHM_NAME = "name";
    private static final String KEY_FEATURE_NAME = "featurename";
    private static final String KEY_DESCRIPTION_NAME = "description";
    private static final String TOOL_EXTENSION_DIR = "bin/tools/extensions/";

    /**
     * Returns true when the value of wlp.process.type is neither server nor client.
     * false otherwise.
     */
    public static boolean isCommandLine() {
        String output = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("wlp.process.type");
            }
        });
        boolean value = true;
        if (output != null && ("server".equals(output) || "client".equals(output))) {
            value = false;
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("value: " + value);
        }
        return value;
    }

    /**
     * Returns whether the custom encryption extension is enabled.
     * This method introspects the classpath jar file, and if it contains IBM-RequiredExtensions
     * header of which value includes ws-customPasswordEncryption, then return true.
     * otherwise return false.
     */
//    public static boolean isCustomEnabled() {
//        boolean output = false;
//        Attributes attrs = AccessController.doPrivileged(new PrivilegedAction<Attributes>() {
//            @Override
//            public Attributes run() {
//                Attributes a = null;
//                String cp = System.getProperty("java.class.path");
//                if (cp != null) {
//                    try {
//                        File cpf = new File(cp);
//                        if (cpf.exists()) {
//                            JarFile jarFile = new JarFile(new File(cp));
//                            a = jarFile.getManifest().getMainAttributes();
//                            jarFile.close();
//                        }
//                    } catch (IOException e) {
//                        //do nothing.
//                    }
//                }
//                return a;
//            }
//        });
//        if (attrs != null) {
//            String extensions = attrs.getValue("IBM-RequiredExtensions");
//            if (extensions != null) {
//                for (String extension : extensions.split(",")) {
//                    if (CUSTOM_ENCRYPTION_DIR.equals(extension.trim())) {
//                        output = true;
//                        break;
//                    }
//                }
//            }
//        }
//        if (logger.isLoggable(Level.FINE)) {
//            logger.fine("value: " + output);
//        }
//        return output;
//    }

    /**
     * Returns the installation root. If it wasn't detected, use current directory.
     */
    public static String getInstallRoot() {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                String output = System.getProperty("wlp.install.dir");
                if (output == null) {
                    output = System.getenv("WLP_INSTALL_DIR");
                }
                if (output == null) {
                    // if neither of these is set. use the location where the class is loaded.
                    URL url = CLASS_NAME.getProtectionDomain().getCodeSource().getLocation();
                    try {
                        output = new File(url.toString().substring("file:".length())).getParentFile().getParentFile().getCanonicalPath();
                    } catch (IOException e) {
                        // this condition should not happen, but if that's the case, use current directory.
                        output = ".";
                        if (logger.isLoggable(Level.FINE)) {
                            logger.fine("The install root was not detected. " + e.getMessage());
                        }
                    }
                }
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("The install root is " + output);
                }
                return output;
            }
        });
    }

    /**
     * find the best matching resouce bundle of the specified resouce file name.
     */
    public static ResourceBundle getResourceBundle(File location, String name, Locale locale) {
        File[] files = new File[] { new File(location, name + "_" + locale.toString() + RESOURCE_FILE_EXT),
                                   new File(location, name + "_" + locale.getLanguage() + RESOURCE_FILE_EXT),
                                   new File(location, name + RESOURCE_FILE_EXT) };
        for (File file : files) {
            if (exists(file)) {
                try {
                    return new PropertyResourceBundle(new FileReader(file));
                } catch (IOException e) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("The resource file was not loaded. The exception is " + e.getMessage());
                    }
                }
            }
        }
        return null;
    }

    /**
     * Find the URL of the jar file which includes specified class.
     * If there is none, return empty array.
     * 
     * @throws IOException
     */
    public static List<CustomManifest> findCustomEncryption(String extension) throws IOException {
        List<File> dirs = listRootAndExtensionDirectories();
        return findCustomEncryption(dirs, TOOL_EXTENSION_DIR + extension);
    }

    /**
     * Find the custom encryption manifest files from the specified list of root directories and path name.
     * The reason why there are multiple root directories is that the product extensions which will allow to
     * add the additional root directory anywhere.
     * 
     * @throws IOException
     */
    protected static List<CustomManifest> findCustomEncryption(List<File> rootDirs, String path) throws IOException {
        List<CustomManifest> list = new ArrayList<CustomManifest>();
        for (File dir : rootDirs) {
            dir = new File(dir, path);
            if (exists(dir)) {
                File[] files = listFiles(dir);
                if (files != null) {
                    for (File file : files) {
                        if (isFile(file) && file.getName().toLowerCase().endsWith(JAR_FILE_EXT)) {
                            if (logger.isLoggable(Level.FINE)) {
                                logger.fine("The extension manifest file : " + file);
                            }
                            try {
                                CustomManifest cm = new CustomManifest(file);
                                list.add(cm);
                            } catch (IllegalArgumentException iae) {
                                if (logger.isLoggable(Level.INFO)) {
                                    logger.info(MessageUtils.getMessage("PASSWORDUTIL_ERROR_IN_EXTENSION_MANIFEST_FILE", file, iae.getMessage()));
                                }
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

    public static boolean exists(final File file) {
        Boolean result = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                if (file.exists()) {
                    return Boolean.TRUE;
                }
                return Boolean.FALSE;
            }
        });
        return result.booleanValue();
    }

    public static boolean isAbsolute(final File file) {
        Boolean result = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                if (file.isAbsolute()) {
                    return Boolean.TRUE;
                }
                return Boolean.FALSE;
            }
        });
        return result.booleanValue();
    }

    public static boolean isFile(final File file) {
        Boolean result = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                if (file.isFile()) {
                    return Boolean.TRUE;
                }
                return Boolean.FALSE;
            }
        });
        return result.booleanValue();
    }

    public static String getCanonicalPath(final File file) throws IOException {
        String path = null;
        try {
            path = AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws IOException {
                    return file.getCanonicalPath();
                }
            });
        } catch (PrivilegedActionException pae) {
            throw (IOException) pae.getException();
        }
        return path;
    }

    public static File[] listFiles(final File file) {
        return AccessController.doPrivileged(new PrivilegedAction<File[]>() {
            @Override
            public File[] run() {
                return file.listFiles();
            }
        });
    }

    /**
     * Return the JSON object of the feature information.
     * the format is {"name":"custom","featurename":<feature name>, "description": <description>}
     * 
     */
    public static String toJSON(List<CustomManifest> list) {
        String output = null;
        if (list != null && !list.isEmpty()) {
            StringBuffer sb = new StringBuffer("[");
            boolean first = true;
            for (CustomManifest cm : list) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(toJSON(cm));
            }
            sb.append("]");
            output = sb.toString();
        }
        return output;
    }

    /**
     * Return the JSON object of the feature information.
     * the format is {"name":"custom","featurename":<feature name>, "description": <description>}
     * 
     */
    private static String toJSON(CustomManifest cm) {
        String output = null;
        if (cm != null) {
            String alg = cm.getAlgorithm();
            String fi = cm.getFeatureId();
            String fn = cm.getFeatureName();
            String desc = cm.getDescription();
            StringBuffer sb = new StringBuffer("{");
            sb.append("\"").append(KEY_ALGORITHM_NAME).append("\":\"").append(alg).append("\",");
            sb.append("\"").append(KEY_FEATURE_NAME).append("\":\"").append(fi).append(':').append(fn).append("\",");
            sb.append("\"").append(KEY_DESCRIPTION_NAME).append("\":\"").append(desc).append("\"}");
            output = sb.toString();
        }
        return output;
    }

    /**
     * Returns the list of the absolute path of the extension directories.
     * The directory may or may not exist.
     * the list of directories which this method returns is:
     * 1. wlp/usr/extension/
     * 2. all locations returned from ProductExtension.getProductExtensions()
     */
    static List<File> listExtensionDirectories() {
        List<File> dirs = new ArrayList<File>();
        dirs.add(new File(CustomUtils.getInstallRoot(), USER_FEATURE_DIR)); // wlp/usr/extension

        for (ProductExtensionInfo info : ProductExtension.getProductExtensions()) {
            File extensionDir = new File(info.getLocation());
            if (!isAbsolute(extensionDir)) {
                File parentDir = new File(CustomUtils.getInstallRoot()).getParentFile();
                extensionDir = new File(parentDir, info.getLocation());
            }
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("The product extension directory : " + extensionDir);
            }
            dirs.add(extensionDir);
        }
        return dirs;
    }

    /**
     * Returns the list of the absolute path of the product root and extension directories.
     * The directory may or may not exist.
     * the list of directories which this method returns is:
     * 1. wlp/
     * 2. wlp/usr/extension/
     * 3. all locations returned from ProductExtension.getProductExtensions()
     */
    static List<File> listRootAndExtensionDirectories() {
        List<File> dirs = listExtensionDirectories();
        dirs.add(0, new File(CustomUtils.getInstallRoot())); // place wlp first.
        return dirs;
    }

}