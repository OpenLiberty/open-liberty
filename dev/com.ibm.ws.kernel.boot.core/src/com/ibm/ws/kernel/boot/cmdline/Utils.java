/*******************************************************************************
 * Copyright (c) 2012,2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.cmdline;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.util.ResourceBundle;
import java.util.zip.ZipFile;

import com.ibm.ws.kernel.boot.internal.BootstrapConstants;

public class Utils {
    private static File installDir;
    private static File userDir;
    private static File outputDir;
    private static File logDir;
    private static ResourceBundle cmdlineResourceBundle;

    public static File getInstallDir() {
        if (installDir == null) {
            URL url = UtilityMain.class.getProtectionDomain().getCodeSource().getLocation();
            installDir = getFile(url).getParentFile().getParentFile();
        }

        return installDir;
    }

    public static File getUserDir() {

        String userDirLoc = null;

        // 1st check in environment variable is set.  This is the normal case
        // when the server is started from the command line.
        if (userDir == null) {
            userDirLoc = System.getenv(BootstrapConstants.ENV_WLP_USER_DIR);
            if (userDirLoc != null) {
                userDir = new File(userDirLoc);
            } else {

                // PI20344: Check if the Java property is set, which is the normal case when
                // the server is embedded; i.e. they didn't launch it from the command line.
                userDirLoc = System.getProperty(BootstrapConstants.ENV_WLP_USER_DIR);
                if (userDirLoc != null) {
                    userDir = new File(userDirLoc);
                } else {
                    File installDir = Utils.getInstallDir();
                    if (installDir != null) {
                        userDir = new File(installDir, "usr");
                    }
                }
            }
        }
        return userDir;
    }

    /**
     * Returns directory containing server output directories. A server output
     * directory has server's name as a name and located under this directory.
     *
     * @return instance of the output directory or 'null' if installation directory
     *         can't be determined.
     */
    public static File getOutputDir() {
        return Utils.getOutputDir(false);
    }

    /**
     * Returns directory containing server log directories.
     *
     * @return instance of the log directory or 'null' if LOG_DIR is not defined
     */
    public static File getLogDir() {

        String logDirLoc = null;

        // 1st check in environment variable is set.  This is the normal case
        // when the server is started from the command line.
        if (logDir == null) {

            try {
                logDirLoc = AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<String>() {
                    @Override
                    public String run() throws Exception {
                        return System.getenv(BootstrapConstants.ENV_LOG_DIR);
                    }
                });
            } catch (Exception ex) {
            }

            //outputDirLoc = System.getenv(BootstrapConstants.ENV_WLP_OUTPUT_DIR);
            if (logDirLoc != null) {
                logDir = new File(logDirLoc);
            } else {
                // PI20344: Check if the Java property is set, which is the normal case when
                // the server is embedded; i.e. they didn't launch it from the command line.
                logDirLoc = System.getProperty(BootstrapConstants.ENV_LOG_DIR);
                if (logDirLoc != null) {
                    logDir = new File(logDirLoc);
                }
            }
        }
        return logDir;
    }

    /**
     * Returns directory containing server output directories. A server output
     * directory has server's name as a name and located under this directory.
     *
     * @return instance of the output directory or 'null' if installation directory
     *         can't be determined.
     */
    public static File getOutputDir(boolean isClient) {

        String outputDirLoc = null;

        // 1st check in environment variable is set.  This is the normal case
        // when the server is started from the command line.
        if (outputDir == null) {

            try {
                outputDirLoc = AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<String>() {
                    @Override
                    public String run() throws Exception {
                        return System.getenv(BootstrapConstants.ENV_WLP_OUTPUT_DIR);
                    }
                });
            } catch (Exception ex) {
            }

            //outputDirLoc = System.getenv(BootstrapConstants.ENV_WLP_OUTPUT_DIR);
            if (outputDirLoc != null) {
                outputDir = new File(outputDirLoc);
            } else {
                // PI20344: Check if the Java property is set, which is the normal case when
                // the server is embedded; i.e. they didn't launch it from the command line.
                outputDirLoc = System.getProperty(BootstrapConstants.ENV_WLP_OUTPUT_DIR);
                if (outputDirLoc != null) {
                    outputDir = new File(outputDirLoc);
                } else {
                    File userDir = Utils.getUserDir();
                    if (userDir != null) {
                        if (isClient) {
                            outputDir = new File(userDir, "clients");
                        } else {
                            outputDir = new File(userDir, "servers");
                        }
                    }
                }
            }
        }
        return outputDir;
    }

    /**
     * Returns server output directory. This directory contains server generated
     * output (logs, the server's workarea, generated files, etc). It corresponds
     * to the value of ${server.output.dir} variable in server.xml configuration file.
     *
     * @param serverName server's name
     * @return instance of the server output directory or 'null' if installation directory
     *         can't be determined.
     */
    public static File getServerOutputDir(String serverName) {
        return Utils.getServerOutputDir(serverName, false);
    }

    /**
     * Returns server output directory. This directory contains server generated
     * output (logs, the server's workarea, generated files, etc). It corresponds
     * to the value of ${server.output.dir} variable in server.xml configuration file.
     *
     * @param serverName server's name
     * @param isClient true if the current process is client.
     * @return instance of the server output directory or 'null' if installation directory
     *         can't be determined.
     */
    public static File getServerOutputDir(String serverName, boolean isClient) {
        if (serverName == null) {
            throw new IllegalArgumentException("Parameter serverName can not be 'null'");
        }
        File outputDir = Utils.getOutputDir(isClient);
        if (outputDir != null) {
            return new File(outputDir, serverName);
        }
        return getServerConfigDir(serverName);
    }

    /**
     * Returns server configuration directory. This directory contains server configuration files
     * (bootstrap.properties, server.xml, jvm.options, etc). It correspond to the value of ${server.config.dir}
     * variable in server.xml configuration file.
     *
     * @param serverName server's name
     * @return instance of the server configuration directory or 'null' if installation directory
     *         can't be determined.
     */
    public static File getServerConfigDir(String serverName) {
        if (serverName == null) {
            throw new IllegalArgumentException("Parameter serverName can not be 'null'");
        }
        File userDir = Utils.getUserDir();
        if (userDir != null) {
            return new File(new File(userDir, "servers"), serverName);
        }
        return null;
    }

    /**
     * the file path of the %JAVA_HOME%/lib/tools.jar, which is needed when compiling the generated java source code.
     *
     * @return
     * @throws MalformedURLException
     * @throws Exception
     */
    public static File getJavaTools() throws MalformedURLException {
        File javaToolsFile = null;
        String javaHome = System.getProperty("java.home");

        //if the tools.jar can't be found under the java home, then goto java home's parent and try again.
        //this case would happen when user set the jre as the java home.
        if (null != javaHome && !javaHome.isEmpty()) {
            File javaHomeFile = new File(javaHome);
            javaToolsFile = buildJavaToolsFile(javaHomeFile);
            if (null == javaToolsFile) {
                javaToolsFile = buildJavaToolsFile(javaHomeFile.getParentFile());
            }
        }

        return javaToolsFile;
    }

    private static File buildJavaToolsFile(File javaHomeFile) {

        File javaLibFile = new File(javaHomeFile, "lib");
        final File javaToolsFile = new File(javaLibFile, "tools.jar");

        return javaToolsFile.exists() && javaToolsFile.isFile() ? javaToolsFile : null;
    }

    public static boolean hasToolsByDefault() {
        try {
            // In theory, this class might be unavailable but other classes
            // might be.  In practice, this is the class we care about.
            Class.forName("com.sun.mirror.apt.AnnotationProcessorFactory");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static ResourceBundle getResourceBundle() {
        if (cmdlineResourceBundle == null) {
            cmdlineResourceBundle = ResourceBundle.getBundle("com.ibm.ws.kernel.boot.cmdline.resources.CommandLineMessages");
        }
        return cmdlineResourceBundle;
    }

    static String getResourceBundleString(String key) {
        ResourceBundle bundle = getResourceBundle();
        return bundle == null ? null : bundle.getString(key);
    }

    /**
     * Close the closeable object
     *
     * @param closeable
     */
    public static boolean tryToClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
                return true;
            } catch (IOException e) {
                // ignore
            }
        }
        return false;
    }

    /**
     * Close the zip file
     *
     * @param zipFile
     * @return
     */
    public static boolean tryToClose(ZipFile zipFile) {
        if (zipFile != null) {
            try {
                zipFile.close();
                return true;
            } catch (IOException e) {
                // ignore
            }
        }
        return false;
    }

    /**
     * This method should be called by common install kernel only.
     *
     * @param installDir
     */
    public static void setInstallDir(File installDir) {
        Utils.installDir = installDir;
    }

    /**
     * This method should be called by common install kernel only.
     *
     * @param installDir
     */
    public static void setUserDir(File userDir) {
        Utils.userDir = userDir;
    }

    /**
     * Duplicated from FileUtils to minimize dependencies of kernel.cmdline jar
     */
    private static File getFile(URL url) {
        String path;
        try {
            // The URL for a UNC path is file:////server/path, but the
            // deprecated File.toURL() as used by java -jar/-cp incorrectly
            // returns file://server/path/, which has an invalid authority
            // component.  Rewrite any URLs with an authority ala
            // http://wiki.eclipse.org/Eclipse/UNC_Paths
            if (url.getAuthority() != null) {
                url = new URL("file://" + url.toString().substring("file:".length()));
            }

            path = new File(url.toURI()).getPath();
        } catch (MalformedURLException e) {
            path = null;
        } catch (URISyntaxException e) {
            path = null;
        } catch (IllegalArgumentException e) {
            path = null;
        }

        if (path == null) {
            // If something failed, assume the path is good enough.
            path = url.getPath();
        }

        return new File(normalizePathDrive(path));
    }

    /**
     * Duplicated from FileUtils to minimize dependencies of kernel.cmdline jar
     */
    private static String normalizePathDrive(String path) {
        if (File.separatorChar == '\\' && path.length() > 1 && path.charAt(1) == ':' && path.charAt(0) >= 'a' && path.charAt(0) <= 'z') {
            path = Character.toUpperCase(path.charAt(0)) + path.substring(1);
        }
        return path;
    }
}