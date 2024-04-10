/*******************************************************************************
 * Copyright (c) 2010,2024 IBM Corporation and others.
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
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.kernel.boot.Debug;
import com.ibm.ws.kernel.boot.LaunchException;
import com.ibm.ws.kernel.boot.cmdline.Utils;

/**
 * Time and file utilities used by kernel bootstrapping.
 */
public class KernelUtils {
    // Log and exception utilities ...

    /**
     * Log and return an exception.
     *
     * Return the exception, so that it can be thrown by the caller. Throwing
     * the exception here is confusing.
     *
     * @param e An exception which is to be logged.
     *
     * @return The exception.
     */
    private static <T extends Exception> T log(T e) {
        Debug.printStackTrace(e);
        return e;
    }

    public static LaunchException unwindException(String untranslatedMsg, Throwable ex) {
        Throwable cause = ex.getCause();
        if (cause == null) {
            cause = ex;
        }
        return KernelUtils.launchException(cause,
                                           untranslatedMsg,
                                           "error.unknownException", cause.toString());
    }

    /**
     * Create and return a new launch exception for a base exception.
     * Lookup the message code and use the result in the new exception,
     * optionally formatting the message with the supplied parameters.
     * Log the exception before returning it.
     *
     * @param baseException A base exception.
     * @param defaultMsg    The default exception message.
     * @param msgCode       The message code of the exception message.
     * @param msgParms      Options parameters to the message.
     *
     * @return A new launch exception.
     */
    public static LaunchException logException(Throwable baseException,
                                               String defaultMsg, String msgCode, Object... msgParms) {
        return log(launchException(baseException, defaultMsg, msgCode, msgParms));
    }

    /**
     * Create and return a new launch exception. Lookup the message code and use
     * the result in the new exception, optionally formatting the message with
     * the supplied parameters. Log the exception before returning it.
     *
     * @param defaultMsg The default exception message.
     * @param msgCode    The message code of the exception message.
     * @param msgParms   Options parameters to the message.
     *
     * @return A new launch exception.
     */
    public static LaunchException logException(String defaultMsg, String msgCode, Object... msgParms) {
        return log(launchException(defaultMsg, msgCode, msgParms));
    }

    /**
     * Create and return a new launch exception. Lookup the message code and use
     * the result in the new exception, optionally formatting the message with
     * the supplied parameters.
     *
     * @param defaultMsg The default exception message.
     * @param msgCode    The message code of the exception message.
     * @param msgParms   Options parameters to the message.
     *
     * @return A new launch exception.
     */
    public static LaunchException launchException(String defaultMsg, String msgCode, Object... msgParms) {

        return new LaunchException(defaultMsg, format(msgCode, msgParms));
    }

    /**
     * Create and return a new launch exception. Lookup the message code and use
     * the result in the new exception, optionally formatting the message with
     * the supplied parameters.
     *
     * @param baseException A base exception.
     * @param defaultMsg    The default exception message.
     * @param msgCode       The message code of the exception message.
     * @param msgParms      Options parameters to the message.
     *
     * @return A new launch exception.
     */
    public static LaunchException launchException(Throwable baseException,
                                                  String defaultMsg,
                                                  String msgCode, Object... msgParms) {

        return new LaunchException(defaultMsg, format(msgCode, msgParms), baseException);
    }

    public static String format(String msgCode, Object... msgParms) {
        String msg = BootstrapConstants.messages.getString(msgCode);
        if ((msgParms != null) && (msgParms.length > 0)) {
            msg = MessageFormat.format(msg, msgParms);
        }
        return msg;
    }

    // Collection utilities ...

    public static final List<File> emptyFiles = Collections.emptyList();
    public static final List<String> emptyStrings = Collections.emptyList();

    public static void appendURLs(List<File> files, List<URL> urls) {
        for (File jarFile : files) {
            try {
                urls.add(jarFile.toURI().toURL());
            } catch (MalformedURLException e) {
                // Unlikely: we're making URLs for files we know exist.
            }
        }
    }

    /**
     * Split input text into a list of file names. Answer each as a file
     * object. Ignore empty file names.
     *
     * The returned list is not mutable.
     *
     * @param str   Text which is to be parsed.
     * @param regex Expression used to split the input text.
     *
     * @return The parsed file names, as files.
     */
    public static List<File> parseFiles(String str, String regex) {
        if ((str != null) && !str.isEmpty()) {
            String[] parts = str.split(regex);
            if (parts.length > 0) {
                List<File> fileList = new ArrayList<File>(parts.length);
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        fileList.add(new File(part));
                    }
                }
                if (!fileList.isEmpty()) {
                    return fileList;
                }
            }
        }
        return emptyFiles;
    }

    /**
     * Generate a print string for a list of files. Answer
     * an empty string for an empty list of files.
     *
     * @param files A list of files.
     * @param delim The delimiter to put between files.
     *
     * @return A print string for the files.
     */
    public static String toString(List<File> files, String delim) {
        if ((files == null) || files.isEmpty()) {
            return "";
        }
        if (files.size() == 1) {
            return files.get(0).getAbsolutePath();
        }

        StringBuilder builder = new StringBuilder();
        for (File f : files) {
            if (builder.length() != 0) {
                builder.append(delim);
            }
            builder.append(f.getAbsolutePath());
        }
        return builder.toString();
    }

    /**
     * Parsing utility: Strip leading and trailing quotes from
     * an attribute value. Do nothing if the attribute is null
     * or doesn't have at least two characters, or doesn't have
     * leading and trailing quotes. Do not trim the attribute
     * value either before or after stripping quotes.
     *
     * @param value An attribute value.
     *
     * @return The value with leading and trailing quotes removed.
     */
    public static String stripQuotes(String value) {
        if (value == null) {
            return value;
        }
        int len = value.length();
        if (len < 2) {
            return value;
        } else if ((value.charAt(0) != '\"') || (value.charAt(len - 1) != '\"')) {
            return value;
        } else {
            return value.substring(1, len - 1);
        }
    }

    // Time utilities ...

    /**
     * Table mapping time unit text values (from several languages)
     * to time units. The text values are what are allowed in duration
     * values as duration typed command line arguments.
     */
    private static final Map<String, TimeUnit> UNIT_DESCRIPTORS;

    static {
        Map<String, TimeUnit> units = new HashMap<String, TimeUnit>();

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

    /**
     * Parse a duration, which may be an long integer value, or may have
     * embedded time unites (for example, "1d2h" or "1d2h45m").
     *
     * Convert the parsed duration to a specified time unit.
     *
     * Partial values are truncated. For example, both "1d2h" and "1d2h45m"
     * convert to 26 hours.
     *
     * @param duration A text duration value.
     *
     * @param units    The units of the result.
     *
     * @return The parsed duration print string.
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
     * Parse a duration, which may be an long integer value, or may have
     * embedded time unites (for example, "1d2h" or "1d2h45m").
     *
     * Convert the parsed duration to a specified time unit.
     *
     * Duration values which have no specified units are parsed directly
     * to the result unit.
     *
     * Partial values are truncated. For example, both "1d2h" and "1d2h45m"
     * convert to 26 hours.
     *
     * @param durationText A text duration value.
     * @param endUnit      The units of the result.
     *
     * @return The parsed duration.
     */
    @FFDCIgnore(NumberFormatException.class)
    public static Long evaluateDuration(String durationText, TimeUnit endUnit) {
        // If the value is a number, simply return the numeric value as a long
        try {
            return Long.valueOf(durationText);
        } catch (NumberFormatException ex) {
            // ignore
        }

        // Otherwise, parse the duration with unit descriptors.
        return evaluateDuration(durationText, endUnit, UNIT_DESCRIPTORS);
    }

    private final static Pattern INTERVAL_STRING = Pattern.compile("(\\d+)(\\D+)");

    /**
     * Parse a duration, which may be an long integer value, or may have
     * embedded time unites (for example, "1d2h" or "1d2h45m").
     *
     * Convert the parsed duration to a specified time unit.
     *
     * Partial values are truncated. For example, both "1d2h" and "1d2h45m"
     * convert to 26 hours.
     *
     * @param durationText    A text duration value.
     * @param endUnit         The units of the result.
     * @param unitDescriptors Text values used by fields of the input text.
     *
     * @return The parsed duration.
     */
    private static Long evaluateDuration(String durationText,
                                         TimeUnit endUnit,
                                         Map<String, TimeUnit> unitDescriptors) {

        Matcher m = INTERVAL_STRING.matcher(durationText);
        long retVal = 0;
        boolean somethingParsed = false;
        while (m.find()) {
            somethingParsed = true;
            // either of these could throw it's own Illegal argument exception
            // if one of the component parts is bad.
            Long numberVal = Long.valueOf(m.group(1));
            String unitStr = m.group(2);
            if (unitStr == null) {
                throw new IllegalArgumentException("Could not parse configuration value as a duration: " + durationText);
            }
            TimeUnit sourceUnit = unitDescriptors.get(unitStr.trim().toLowerCase());
            if (sourceUnit == null) {
                throw new IllegalArgumentException("Could not parse configuration value as a duration: " + durationText);
            }
            retVal += endUnit.convert(numberVal, sourceUnit);
        }

        if (!somethingParsed) {
            throw new IllegalArgumentException("Could not parse configuration value as a duration: " + durationText);
        }

        return retVal;
    }

    //

    /** File representing the launch location. */
    private static File launchHome;

    /** URL representing the launch location. */
    private static URL launchURL;

    /** File representing the server library location. */
    private static File libDir;

    /**
     * Locate the server bootstrap JAR. This is location of the code source
     * of this utility class.
     *
     * @return The location of the server bootstream JAR.
     */
    public static File getBootstrapJar() {
        if (launchHome == null) {
            if (launchURL == null) {
                launchURL = getLocationFromClass(KernelUtils.class);
            }
            launchHome = FileUtils.getFile(launchURL);
        }
        return launchHome;
    }

    public static URL getBootstrapJarURL() {
        if (launchURL == null) {
            getBootstrapJar();
        }
        return launchURL;
    }

    /**
     * Answer the server library location. This defaults to the parent
     * directory of the bootstrap JAR.
     *
     * @return The server library directory.
     */
    public static File getBootstrapLibDir() {
        if (libDir == null) {
            libDir = getBootstrapJar().getParentFile();
        }
        return libDir;
    }

    /**
     * Set the bootstrap library directory.
     *
     * This defaults to the parent directory of the bootstrap JAR.
     * See {@link #getBootstrapJarURL()}.
     *
     * @param libDir The drectory to set as the bootstramp library directory.
     */
    public static void setBootStrapLibDir(File libDir) {
        // For liberty boot we need to be able to set the lib dir
        // explicitly because the boot jar will not be located in lib
        KernelUtils.libDir = libDir;
    }

    /**
     * Retrieve the code source of a specified class.
     *
     * See {@link Class<?>getProtectionDomain()},
     * {@link ProtectionDomain#getCodeSource()}, and
     * {@link CodeSource#getLocation()}.
     *
     * Throw a runtime exception of the target class does not have
     * a protection domain or a code source.
     *
     * @param cls A class.
     *
     * @return The code source of the class.
     */
    public static URL getLocationFromClass(Class<?> cls) {
        ProtectionDomain domain = cls.getProtectionDomain();
        CodeSource source = null;
        if (domain != null) {
            source = domain.getCodeSource();
        }
        if ((domain == null) || (source == null)) {
            String errorMsg = MessageFormat.format(BootstrapConstants.messages.getString("error.secPermission"), (Object[]) null);
            throw new LaunchException("Can not automatically set the security manager. Please use a policy file.", errorMsg);
        }
        URL home = source.getLocation();
        if (!home.getProtocol().equals("file")) {
            String errorMsg = MessageFormat.format(BootstrapConstants.messages.getString("error.unsupportedLaunch"), home);
            throw new LaunchException("Launch location is not a local file (launch location=" + home + ")", errorMsg);
        }

        return home;
    }

    /**
     * Read properties from an input stream.
     *
     * Use the standard properties operation {@link Properties#load(InputStream)}.
     *
     * Adjust values, removing leading and trailing double quotes.
     * White space before and after the double quotes is ignored.
     * The leading and trailing double quotes cannot be the same character.
     *
     * Always close the input stream.
     *
     * @param is The stream from which to read properties.
     *
     * @return Adjusted properties read from the input stream.
     *
     * @throws IOException Thrown by a read failure.
     */
    public static Properties getProperties(InputStream is) throws IOException {
        Properties p = new Properties();

        if (is != null) {
            try {
                p.load(is); // throws IOException
            } finally {
                Utils.tryToClose(is);
            }

            for (Entry<Object, Object> entry : p.entrySet()) {
                String s = ((String) entry.getValue()).trim();
                int sLen = s.length();
                if (sLen < 2) {
                    continue; // Trim "\"abc\""; do not trim "\"".
                }
                if ((s.charAt(0) == '\"') && (s.charAt(sLen - 1) == '\"')) {
                    entry.setValue(s.substring(1, sLen - 1));
                }
            }
        }

        return p;
    }

    /**
     * Parse the first class name from a reader.
     *
     * Read lines and attempt to parse a class name from each line.
     * Stop reading lines when a class name is obtained, or after all
     * lines have been read.
     *
     * Class names are parsed by {@link #getClassFromLine(String)}.
     * Lines which have only whitespace or comments do not provide a
     * class name.
     *
     * @param reader A reader suppling text lines.
     *
     * @return The first class name parsed from a read line. Answer
     *         null if lines are exhausted before a class name is obtained.
     *
     * @throws IOException Thrown in case of a read failure.
     */
    public static String getServiceClass(BufferedReader reader) throws IOException {
        String line = null;
        while ((line = reader.readLine()) != null) {
            String className = getClassFromLine(line);
            if (className != null) {
                return className;
            }
        }
        return null; // No more lines.
    }

    /**
     * Parse a class name from the given line. Ignore whitespace.
     * Answer null if the line is empty or is a comment line.
     *
     * @param line The line which is to be parsed.
     *
     * @return The class name parse from the line.
     */
    private static String getClassFromLine(String line) {
        line = line.trim();

        // Quick checks for empty lines and for all comment lines.
        if (line.isEmpty() || line.startsWith("#")) {
            return null;
        }

        // Split at white space (\\s) and comments (#).
        String[] lineParts = line.split("[\\s#]");
        return ((lineParts.length == 0) ? null : lineParts[0]);
    }

    //

    /**
     * Clean the work area directory. That is, deleted (if necessary)
     * then created (or recreated).
     *
     * See {@link #cleanDirectory(File, String)}.
     *
     * @param workareaFile The directory which is to be cleaned.
     */
    public static void cleanStart(File workareaFile) {
        cleanDirectory(workareaFile, "workarea");
    }

    /**
     * Attempt to delete and recreate a target directory.
     *
     * Fail with a runtime exception if the directory exists and cannot
     * be deleted, or if the directory cannot be created.
     *
     * @param dir     The directory which is to be deleted and recreated.
     * @param dirType A description of the directory.
     */
    public static void cleanDirectory(File dir, String dirType) {
        if (dir.exists() && dir.isDirectory()) {
            if (!FileUtils.recursiveClean(dir)) {
                throw new IllegalStateException("The " + dirType + " could not be cleaned. " + dirType + "=" + dir);
            }
        }

        boolean created = dir.mkdirs();
        if (!dir.exists() && !created) {
            throw new IllegalStateException("The " + dirType + "  could not be created. " + dirType + "=" + dir);
        }
    }
}