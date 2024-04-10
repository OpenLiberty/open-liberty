/*******************************************************************************
 * Copyright (c) 2012,2024 IBM Corporation and others.
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
package com.ibm.ws.kernel.boot.cmdline;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ResourceBundle;
import java.util.zip.ZipFile;

import com.ibm.ws.kernel.boot.internal.BootstrapConstants;

/**
 * Server location utilities.
 *
 * Use these utilities to read standard server locations from the environment
 * and from system properties.
 *
 * WLP_HOME: Installation root folder: Externally set,
 * or computed from a code source.
 *
 * WLP_USER: User root folder: Configured (ENV_WLP_USER_DIR),
 * or relative to WLP_HOME (WLP_HOME/usr).
 *
 * WLP_OUTPUT: Root output folder. Configured (ENV_WLP_OUTPUT_DIR),
 * or relative to the WLP_USER (WLP_USER/clients or WLP_USER/servers;
 * WLP_HOME/usr/clients or WLP_HOME/usr/servers).
 *
 * Server folder. Relative to the root output folder, or relative to the
 * user folder.
 * WLP_OUTPUT/SERVER_NAME; WLP_HOME/usr/clients/SERVER_NAME or
 * WLP_HOME/usr/servers/SERVER_NAME, or, WLP_USER/servers/SERVER_NAME.
 *
 * Log folder. Configured (ENV_LOG_DIR).
 *
 * Tools jar. Relative to "java.home": JAVA_HOME/lib/ tools.jar or
 * JAVA_HOME/../lib/tools.jar.
 */
public class Utils {
    /**
     * Lookup a value first from the environment then as a system property. If a
     * value is located, answer a file on the value. Otherwise, answer null.
     *
     * @param propertyName The target environment variable or system property.
     *
     * @return The value of the environment variable or system property as a file.
     *         Null if the value is not available.
     */
    private static File envGetFile(String propertyName) {
        String path;
        try {
            path = AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws Exception {
                    // Usually set when launching the server from the command line.
                    return System.getenv(propertyName);
                }
            });
        } catch (Exception ex) {
            path = null;
        }

        if (path == null) {
            // Usually set for an embedded server (PI20344).
            path = System.getProperty(propertyName);
        }

        return ((path == null) ? null : new File(path));
    }

    //

    private static File installDir;

    public static void setInstallDir(File installDir) {
        Utils.installDir = installDir;
    }

    /**
     * Answer the installation directory.
     *
     * Default to the second parent of the location of the code source of class
     * {@link UtilityMain}. For example, for "wlp/lib/utility.jar//UtilityMain.class",
     * with code source location "wlp/lib/utility.jar" answers "wlp".
     *
     * @return The installation directory.
     */
    public static File getInstallDir() {
        if (installDir == null) {
            URL url = UtilityMain.class.getProtectionDomain().getCodeSource().getLocation();
            installDir = getFile(url).getParentFile().getParentFile();
        }
        return installDir;
    }

    //

    private static File userDir;

    public static void setUserDir(File userDir) {
        Utils.userDir = userDir;
    }

    /**
     * Answer the user directory.
     *
     * Default the user directory to the environment or system property value,
     * or to the installation based directory.
     *
     * See {@link #envUserDir()}, and {@link #installUserDir()}.
     *
     * @return
     */
    public static File getUserDir() {
        if (userDir == null) {
            File useUserDir = envUserDir();
            if (useUserDir == null) {
                useUserDir = installUserDir();
            }
            userDir = useUserDir;
        }
        return userDir;
    }

    /**
     * Answer the environment based user directory.
     *
     * Unless {@link BootstrapConstants#LOC_PROPERTY_IGNORE_INSTANCE_DIR_FROM_ENV} is
     * set, answer {@link BootstrapConstants#ENV_WLP_USER_DIR} as an environment
     * value or a system property.
     *
     * @return The environment based user directory.
     */
    private static File envUserDir() {
        if (!Boolean.getBoolean(BootstrapConstants.LOC_PROPERTY_IGNORE_INSTANCE_DIR_FROM_ENV)) {
            return envGetFile(BootstrapConstants.ENV_WLP_USER_DIR);
        }
        return null;
    }

    /**
     * Answer the installation based user directory. This is sub-directory "usr" of
     * the installation directory.
     *
     * @return The installation based user directory.
     */
    private static File installUserDir() {
        File useInstallDir = getInstallDir();
        if (useInstallDir != null) {
            return new File(useInstallDir, "usr");
        } else {
            return null;
        }
    }

    //

    private static File outputDir;

    /**
     * Answer the output folder for servers.
     *
     * See {@link #getOutputDir(boolean)}.
     *
     * @return The output folder for servers.
     *
     */
    public static File getOutputDir() {
        return getOutputDir(!IS_CLIENT);
    }

    public static final boolean IS_CLIENT = true;

    /**
     * Answer the output directory for clients or for servers.
     *
     * This is determined using an environment variable or a system
     * property, or is determined relative to the user output directory.
     *
     * Answer null if no value is available from the environment or the system,
     * or if the user output directory is not available.
     *
     * @return The output directory for all clients or for all servers.
     */
    public static File getOutputDir(boolean isClient) {
        if (outputDir == null) {
            File useOutputDir = envOutputDir();
            if (useOutputDir == null) {
                useOutputDir = userOutputDir(isClient);
            }
            outputDir = useOutputDir;
        }
        return outputDir;
    }

    /**
     * Answer the output directory (for servers or clients) from the environment or
     * from a system property.
     *
     * Look for {@link BootstrapConstants#ENV_WLP_OUTPUT_DIR}.
     *
     * @return The output directory for servers or for clients, obtained from the
     *         environment or from system properties.
     */
    private static File envOutputDir() {
        return envGetFile(BootstrapConstants.ENV_WLP_OUTPUT_DIR);
    }

    /**
     * Answer the output directory based on the user directory.
     *
     * The output directory is either "clients" or "servers", depending on the control
     * parameter.
     *
     * Answer null if the user directory is not available.
     *
     * @param isClient Control parameter: Is the request for the output directory
     *                     for all clients, or for the output directory for all servers.
     *
     * @return The output directory for all clients or for all servers.
     */
    private static File userOutputDir(boolean isClient) {
        File useUserDir = getUserDir();
        if (useUserDir == null) {
            return null;
        }

        String dirName = (isClient ? "clients" : "servers");
        return new File(useUserDir, dirName);
    }

    //

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
        return getServerOutputDir(serverName, false);
    }

    /**
     * Answer the output directory for a specified client or server.
     *
     * The specific output directory is relative to the output directory.
     * See {@link #getOutputDir(boolean)}.
     *
     * If an output directory is available, the directory is relative to
     * that directory. If an output directory is not available, answer
     * the server configuration directory. (See {@link #getServerConfigDir(String)}.)
     *
     * @param serverName A server name.
     * @param isClient   Control parameter: Is the location for a client or for
     *                       a server.
     *
     * @return The output directory of the server.
     *
     */
    public static File getServerOutputDir(String serverName, boolean isClient) {
        if (serverName == null) {
            throw new IllegalArgumentException("Null server name");
        }

        File useOutputDir = getOutputDir(isClient);
        if (useOutputDir != null) {
            return new File(useOutputDir, serverName);
        } else {
            return getServerConfigDir(serverName);
        }
    }

    /**
     * Answer the directory for a named server.
     *
     * The directory is relative to the user directory:
     *
     * Answer null if the user directory is null.
     *
     * <pre>
     * USER_DIR / servers / SERVER_NAME
     * </pre>
     *
     * See {@link #getUserDir()}.
     *
     * @param serverName A server name.
     *
     * @return The directory of that server.
     *
     */
    public static File getServerConfigDir(String serverName) {
        if (serverName == null) {
            throw new IllegalArgumentException("Null server name");
        }

        File useUserDir = getUserDir();
        if (useUserDir == null) {
            return null;
        }
        return new File(new File(useUserDir, "servers"), serverName);
    }

    //

    private static File logDir;

    /**
     * Answer the directory containing server logs.
     *
     * Answer the value of {@link BootstrapConstants#ENV_LOG_DIR}, either as an
     * environment value or as a system property.
     *
     * Answer null if neither is available.
     *
     * @return The directory container server logs.
     */
    public static File getLogDir() {
        if (logDir == null) {
            logDir = envGetFile(BootstrapConstants.ENV_LOG_DIR);
        }
        return logDir;
    }

    //

    /**
     * Locate the java tools JAR, <code>lib/tools.jar</code>, relative to the java home
     * location, stored in system property <code>java.home</code>.
     *
     * The JAR is expected relative to the java home location, or relative to the parent
     * of the java home location. See {@link #locateJavaTools(File)}. One of:
     *
     * <pre>
     * JAVA_HOME / lib / tools.jar
     * JAVA_HOME / .. / lib / tools.jar
     * </pre>
     *
     * @return The java tools JAR relative to the java home. Null if the java home
     *         is not available or if the java tools could not be found relative to the
     *         java home.
     */
    public static File getJavaTools() {
        String javaHomePath = System.getProperty("java.home");
        if ((javaHomePath == null) || javaHomePath.isEmpty()) {
            return null;
        }

        File javaHome = new File(javaHomePath);

        File javaTools = locateJavaTools(javaHome);

        if (javaTools == null) {
            javaTools = locateJavaTools(javaHome.getParentFile());
        }

        return javaTools;
    }

    /**
     * Locate the java tooling jar relative to a candidate java home folder.
     *
     * The java tooling jar is <code>lib/tools</code> relative to
     * java home.
     *
     * Answer the tooling jar as a file if it is located and is a simple
     * file. Otherwise, answer null.
     *
     * @param javaHome A candidate java home folder.
     *
     * @return The tooling jar at its usual location relative to the java home
     *         folder. Null if the tooling jar does not exist at that location.
     */
    private static File locateJavaTools(File javaHome) {
        File javaLib = new File(javaHome, "lib");
        File javaTools = new File(javaLib, "tools.jar");

        return ((javaTools.exists() && javaTools.isFile()) ? javaTools : null);
    }

    /**
     * Tell if java tools are available in the class path.
     *
     * Look for class <code>com.sun.mirror.apt.AnnotationProcessorFactory</code>.
     *
     * @return True or false telling if java tools are available.
     */
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

    // Command line resource access ...

    public static final String CMDLINE_MSG_RESOURCE = "com.ibm.ws.kernel.boot.cmdline.resources.CommandLineMessages";

    private static ResourceBundle cmdlineResourceBundle;

    /**
     * Answer the resource bundle for command line messages.
     * (See {@link #CMDLINE_MSG_RESOURCE}.
     *
     * @return The resource bundle for command line messaes.
     */
    private static ResourceBundle getResourceBundle() {
        if (cmdlineResourceBundle == null) {
            cmdlineResourceBundle = ResourceBundle.getBundle(CMDLINE_MSG_RESOURCE);
        }
        return cmdlineResourceBundle;
    }

    /**
     * Answer a string value from the command line messages resource.
     *
     * Answer null if the messages resource was not loaded, or if no
     * value is stored for the specified key.
     *
     * @param key A key to lookup.
     *
     * @return The value associated with the key.
     */
    public static String getResourceBundleString(String key) {
        ResourceBundle bundle = getResourceBundle();
        return ((bundle == null) ? null : bundle.getString(key));
    }

    // File utilities ...

    /**
     * Obtain a file based on the path component of a URL.
     *
     * Normalize the drive letter to upper case if the path
     * is a windows root drive letter path.
     *
     * Duplicated from FileUtils to minimize dependencies of kernel.cmdline jar.
     *
     * @return A file based on the path of a URL.
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
            path = url.getPath(); // Assume the path is good enough.
        }

        return new File(normalizePathDrive(path));
    }

    /**
     * Normalize the drive letter to upper case when a path is a root windows
     * path.
     *
     * Answer the unmodified path if not on windows or if the path does not
     * have a drive letter.
     *
     * Duplicated from FileUtils to minimize dependencies of kernel.cmdline jar
     *
     * @return The normalized path.
     */
    private static String normalizePathDrive(String path) {
        if ((File.separatorChar == '\\') &&
            (path.length() > 1) &&
            (path.charAt(1) == ':') &&
            (path.charAt(0) >= 'a') && (path.charAt(0) <= 'z')) {
            path = Character.toUpperCase(path.charAt(0)) + path.substring(1);
        }
        return path;
    }

    // Closeable utilities ...

    /**
     * Attempt to close a closeable. Answer true if the
     * closeable is not null and could be closed.
     *
     * @param closeable A closeable.
     *
     * @return True or false telling if the closeable
     *         could be closed.
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
     * Attempt to close a zip file. Answer true if the
     * zip file is not null and could be closed.
     *
     * @param zipFile A zip file.
     *
     * @return True or false telling if the zip file
     *         could be closed.
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
}
