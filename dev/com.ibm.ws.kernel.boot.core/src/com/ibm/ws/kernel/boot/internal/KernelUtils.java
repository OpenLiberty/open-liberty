/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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
package com.ibm.ws.kernel.boot.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.boot.LaunchException;
import com.ibm.ws.kernel.boot.cmdline.Utils;

/**
 * File utilities required by the bootstrapping code.
 */
public class KernelUtils {

    private static final Map<String, TimeUnit> UNIT_DESCRIPTORS;

    static {
        HashMap<String, TimeUnit> units = new HashMap<String, TimeUnit>();

        // We used to allow the duration abbreviations to be translated, but
        // that causes config to be non-portable, so we now recommend that
        // customers use English abbreviations only.  We hardcode the previously
        // translated abbreviations for compatibility.  Additional translations
        // should not be added.

        units.put("d", TimeUnit.DAYS); // en, es, ja, ko, pl, pt_BR, zh
        units.put("dn\u016f", TimeUnit.DAYS); // cs
        units.put("g", TimeUnit.DAYS); // it
        units.put("j", TimeUnit.DAYS); // fr
        units.put("n", TimeUnit.DAYS); // hu
        units.put("t", TimeUnit.DAYS); // de
        units.put("z", TimeUnit.DAYS); // ro
        units.put("\u0434", TimeUnit.DAYS); // ru
        units.put("\u5929", TimeUnit.DAYS); // zh_TW

        //units.put("g", TimeUnit.HOURS); // pl - conflicts with DAYS for "it"
        units.put("h", TimeUnit.HOURS); // en, de, es, fr, it, ja, ko, pt_BR, ro, zh
        units.put("hod", TimeUnit.HOURS); // cs
        units.put("\u00f3", TimeUnit.HOURS); // hu
        units.put("\u0447", TimeUnit.HOURS); // ru
        units.put("\u5c0f\u6642", TimeUnit.HOURS); // zh_TW

        units.put("m", TimeUnit.MINUTES); // en, de, es, fr, hu, it, ja, ko, pl, pt_BR, ro, zh
        units.put("min", TimeUnit.MINUTES); // cs
        units.put("\u043c", TimeUnit.MINUTES); // ru
        units.put("\u5206", TimeUnit.MINUTES); // zh_TW

        units.put("e", TimeUnit.SECONDS); // pt_BR
        units.put("mp", TimeUnit.SECONDS); // hu
        units.put("s", TimeUnit.SECONDS); // en, cs, de, es, fr, it, ja, ko, pl, ro, zh
        units.put("\u0441", TimeUnit.SECONDS); // ru
        units.put("\u79d2", TimeUnit.SECONDS); // zh_TW

        units.put("ms", TimeUnit.MILLISECONDS); // en, cs, de, es, fr, hu, it, ja, ko, pl, pt_BR, ro, zh
        units.put("\u043c\u0441", TimeUnit.MILLISECONDS); // ru
        units.put("\u6beb\u79d2", TimeUnit.MILLISECONDS); // zh_TW

        UNIT_DESCRIPTORS = Collections.unmodifiableMap(units);
    }

    private final static Pattern INTERVAL_STRING = Pattern.compile("(\\d+)(\\D+)");

    /**
     * Parse a duration from the provided 00h00m00s00ms value. Return a string in
     * the time unit provided.
     * <p>
     *
     * @param duration
     *            The object retrieved from the configuration property map/dictionary.
     *
     * @param units
     *            The unit of time for the duration value. This is only used when
     *            converting from a String value.
     *
     * @return String in the specified time units if the duration is parsed successfully, otherwise null
     */
    @FFDCIgnore(Exception.class)
    public static String parseDuration(String duration, TimeUnit units) {

        try {
            return Long.toString(evaluateDuration(duration, units));
        } catch (Exception e) {
            // null
        }
        return null;
    }

    /**
     * Converts a string value representing a unit of time into a Long value.
     *
     * @param strVal
     *            A String representing a unit of time.
     * @param unit
     *            The unit of time that the string value should be converted into
     * @return Long The value of the string in the desired time unit
     */
    @FFDCIgnore(NumberFormatException.class)
    public static Long evaluateDuration(String strVal, TimeUnit endUnit) {
        // If the value is a number, simply return the numeric value as a long
        try {
            return Long.valueOf(strVal);
        } catch (NumberFormatException ex) {
            // ignore
        }

        // Otherwise, parse the duration with unit descriptors.
        return evaluateDuration(strVal, endUnit, UNIT_DESCRIPTORS);
    }

    private static Long evaluateDuration(String strVal, TimeUnit endUnit, Map<String, TimeUnit> unitDescriptors) {
        Matcher m = INTERVAL_STRING.matcher(strVal);
        long retVal = 0;
        boolean somethingParsed = false;
        while (m.find()) {
            somethingParsed = true;
            // either of these could throw it's own Illegal argument exception
            // if one of the component parts is bad.
            Long numberVal = Long.valueOf(m.group(1));
            String unitStr = m.group(2);
            if (unitStr == null) {
                throw new IllegalArgumentException("Could not parse configuration value as a duration: " + strVal);
            }
            TimeUnit sourceUnit = unitDescriptors.get(unitStr.trim().toLowerCase());
            if (sourceUnit == null) {
                throw new IllegalArgumentException("Could not parse configuration value as a duration: " + strVal);
            }
            retVal += endUnit.convert(numberVal, sourceUnit);
        }

        if (!somethingParsed) {
            throw new IllegalArgumentException("Could not parse configuration value as a duration: " + strVal);
        }

        return retVal;
    }

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
            throw new LaunchException("Can not automatically set the security manager. Please use a policy file.", MessageFormat.format(BootstrapConstants.messages.getString("error.secPermission"),
                                                                                                                                        (Object[]) null));
        }
        URL home = source.getLocation();
        if (!home.getProtocol().equals("file"))
            throw new LaunchException("Launch location is not a local file (launch location=" + home
                                      + ")", MessageFormat.format(BootstrapConstants.messages.getString("error.unsupportedLaunch"), home));
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