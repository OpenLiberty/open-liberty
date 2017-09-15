/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.Map.Entry;
import java.util.Properties;

import com.ibm.ws.kernel.boot.LaunchException;
import com.ibm.ws.kernel.boot.cmdline.Utils;

/**
 * File utilities required by the bootstrapping code.
 */
public class KernelUtils {
    /**
     * File representing the launch location.
     *
     * @see {@link #getBootstrapJar()}
     */
    private static File launchHome = null;

    /**
     * URL representing the launch location.
     *
     * @see {@link #getBootstrapJar()}
     */
    private static URL launchURL = null;

    /**
     * File representing the fixed lib directory.
     *
     * @see {@link #getBootstrapJar()}
     */
    private static File libDir = null;

    /**
     * The location of the launch jar is only obtained once.
     *
     * @return a File representing the location of the launching jar
     */
    public static File getBootstrapJar() {
        if (launchHome == null) {
            if (launchURL == null) {
                // How were we launched?
                launchURL = getLocationFromClass(KernelUtils.class);
            }

            launchHome = FileUtils.getFile(launchURL);
        }
        return launchHome;
    }

    public static URL getLocationFromClass(Class<?> cls) {
        ProtectionDomain domain = cls.getProtectionDomain();
        CodeSource source = null;
        if (domain != null)
            source = domain.getCodeSource();
        if (domain == null || source == null) {
            throw new LaunchException("Can not automatically set the security manager. Please use a policy file.",
                            MessageFormat.format(BootstrapConstants.messages.getString("error.secPermission"), (Object[]) null));
        }
        URL home = source.getLocation();
        if (!home.getProtocol().equals("file"))
            throw new LaunchException("Launch location is not a local file (launch location=" + home + ")",
                            MessageFormat.format(BootstrapConstants.messages.getString("error.unsupportedLaunch"), home));
        return home;
    }

    public static URL getBootstrapJarURL() {
        if (launchURL == null) {
            getBootstrapJar();
        }
        return launchURL;
    }

    /**
     * The lib dir is the parent of the bootstrap jar
     *
     * @return a File representing the location of the launching jar
     */
    public static File getBootstrapLibDir() {
        if (libDir == null) {
            libDir = getBootstrapJar().getParentFile();
        }
        return libDir;
    }

    public static void setBootStrapLibDir(File libDir) {
        // For liberty boot we need to be able to set the lib dir
        // explicitly because the boot jar will not be located in lib
        KernelUtils.libDir = libDir;
    }

    public static void cleanStart(File workareaFile) {
        cleanDirectory(workareaFile, "workarea");
    }

    /**
     * Read properties from input stream. Will close the input stream before
     * returning.
     *
     * @param is
     *            InputStream to read properties from
     * @return Properties object; will be empty if InputStream is null or empty.
     * @throws LaunchException
     */
    public static Properties getProperties(final InputStream is) throws IOException {
        Properties p = new Properties();

        try {
            if (is != null) {
                p.load(is);

                // Look for "values" and strip the quotes to values
                for (Entry<Object, Object> entry : p.entrySet()) {
                    String s = ((String) entry.getValue()).trim();
                    // If first and last characters are ", strip them off..
                    if (s.length() > 1 && s.startsWith("\"") && s.endsWith("\"")) {
                        entry.setValue(s.substring(1, s.length() - 1));
                    }
                }
            }
        } finally {
            Utils.tryToClose(is);
        }

        return p;
    }

    /**
     * @param reader
     *            Reader from which to read service class names
     * @param limit
     *            Maximum number of service classes to find (0 is no limit)
     * @return
     * @throws IOException
     */
    public static String getServiceClass(BufferedReader reader) throws IOException {
        String line = null;

        while ((line = reader.readLine()) != null) {
            String className = getClassFromLine(line);

            if (className != null)
                return className;
        }

        return null;
    }

    /**
     * Read a service class from the given line. Must ignore whitespace, and
     * skip comment lines, or end of line comments.
     *
     * @param line
     * @return class name (first text on a line not starting with #) or null for
     *         empty/comment lines
     */
    private static String getClassFromLine(String line) {
        line = line.trim();

        // Skip commented lines
        if (line.length() == 0 || line.startsWith("#"))
            return null;

        // lop off spaces/tabs/end-of-line-comments
        String[] className = line.split("[\\s#]");

        if (className.length >= 1)
            return className[0];

        return null;
    }

    /**
     * This
     *
     * @param stateDir
     */
    public static void cleanDirectory(File dir, String dirType) {
        boolean cleaned = true;

        if (dir.exists() && dir.isDirectory())
            cleaned = FileUtils.recursiveClean(dir);

        if (!cleaned)
            throw new IllegalStateException("The " + dirType + " could not be cleaned. " + dirType + "=" + dir);

        // re-create empty directory if it doesn't exist
        boolean created = dir.mkdirs();
        if (!dir.exists() && !created)
            throw new IllegalStateException("The " + dirType + "  could not be created. " + dirType + "=" + dir);

    }
}