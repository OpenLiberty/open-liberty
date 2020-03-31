/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.impl;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;

import com.ibm.websphere.logging.WsLevel;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.logging.internal.impl.LoggingConstants.FFDCSummaryPolicy;
import com.ibm.ws.logging.internal.impl.LoggingConstants.TraceFormat;

/**
 *
 */
public class LoggingConfigUtils {

    /**
     * Find, create, and validate the log directory.
     *
     * @param newValue
     *            New parameter value to parse/evaluate
     * @param defaultValue
     *            Starting/Previous log directory-- this value might *also* be null.
     * @return defaultValue if the newValue is null or is was badly
     *         formatted, or the converted new value
     */
    static File getLogDirectory(Object newValue, File defaultDirectory) {

        File newDirectory = defaultDirectory;

        // If a value was specified, try creating a file with it
        if (newValue != null && newValue instanceof String) {
            newDirectory = new File((String) newValue);
        }

        if (newDirectory == null) {
            String value = ".";
            try {
                value = AccessController.doPrivileged(new java.security.PrivilegedExceptionAction<String>() {
                    @Override
                    public String run() throws Exception {
                        return System.getProperty("user.dir");
                    }
                });
            } catch (Exception ex) {
                // do nothing
            }
            newDirectory = new File(value);
        }

        return LoggingFileUtils.validateDirectory(newDirectory);
    }

    /**
     * Read boolean value from properties: begin by preserving the old value. If
     * the property is found, and the new value is an boolean, the new value
     * will be returned.
     *
     * @param newValue
     *            New parameter value to parse/evaluate
     * @param defaultValue
     *            Starting/Previous value
     * @return defaultValue if the newValue is null or is was badly
     *         formatted, or the converted new value
     */
    public static boolean getBooleanValue(Object newValue, boolean defaultValue) {
        if (newValue != null) {
            if (newValue instanceof String) {
                return Boolean.parseBoolean((String) newValue);
            } else if (newValue instanceof Boolean)
                return (Boolean) newValue;
        }

        return defaultValue;
    }

    /**
     * Read integer value from properties: begin by preserving the old value. If
     * the property is found, and the new value is an integer, the new value
     * will be returned.
     *
     * @param newValue
     *            New parameter value to parse/evaluate
     * @param defaultValue
     *            Starting/Previous value
     * @return defaultValue if the newValue is null or is was badly
     *         formatted, or the converted new value
     */
    public static int getIntValue(Object newValue, int defaultValue) {
        if (newValue != null) {
            if (newValue instanceof String) {
                try {
                    return Integer.parseInt((String) newValue);
                } catch (NumberFormatException ex) {
                }
            } else if (newValue instanceof Integer)
                return (Integer) newValue;
        }

        return defaultValue;
    }

    /**
     * If the value is null, return the defaultValue.
     * Otherwise return the new value.
     */
    public static String getStringValue(Object newValue, String defaultValue) {
        if (newValue == null)
            return defaultValue;

        return (String) newValue;
    }

    /**
     * @param newValue String representation of a log level.
     * @param defaultValue The default and/or current value.
     * @return The new log level, or the default value if the new value is null or
     *         outside of the accepted range.
     */
    public static Level getLogLevel(Object newValue, Level defaultLevel) {
        Level result = defaultLevel;

        if (newValue != null && newValue instanceof String) {
            String strValue = ((String) newValue).toUpperCase();

            // This is a filtered/restricted set.
            if (strValue.equals("INFO")) {
                return Level.INFO;
            } else if (strValue.equals("AUDIT")) {
                return WsLevel.AUDIT;
            } else if (strValue.equals("WARNING")) {
                return Level.WARNING;
            } else if (strValue.equals("ERROR")) {
                return WsLevel.ERROR;
            } else if (strValue.equals("OFF")) {
                return Level.OFF;
            }
        }

        return result;
    }

    /**
     * Convert the property value to a TraceFormat type
     *
     * @param s
     *            String value
     * @return TraceFormat, BASIC is the default.
     */
    public static TraceFormat getFormatValue(Object newValue, TraceFormat defaultValue) {
        if (newValue != null && newValue instanceof String) {
            String strValue = ((String) newValue).toUpperCase();
            try {
                return TraceFormat.valueOf(strValue);
            } catch (Exception e) {
            }
        }

        return defaultValue;
    }

    public static FFDCSummaryPolicy getFFDCSummaryPolicy(Object newValue, FFDCSummaryPolicy defaultValue) {
        if (newValue != null && newValue instanceof String) {
            String strValue = ((String) newValue).toUpperCase();
            try {
                return FFDCSummaryPolicy.valueOf(strValue);
            } catch (Exception e) {
            }
        }

        return defaultValue;
    }

    /**
     * Create a delegate instance of the specified (or default) delegate class.
     *
     * @param delegate Specifically configured delegate class
     * @param defaultDelegateClass Default delegate class
     * @return constructed delegate instance
     */
    public static <T> T getDelegate(Class<T> delegateClass, String className, String defaultDelegateClass) {
        if (className == null)
            className = defaultDelegateClass;

        try {
            return Class.forName(className).asSubclass(delegateClass).newInstance();
        } catch (Throwable e) {
            System.err.println("Unable to locate configured delegate: " + delegateClass + ", " + e);
        }
        return null;
    }

    /**
     * Parse a string collection and returns
     * a collection of strings generated from either a comma-separated single
     * string value, or a string collection.
     * <p>
     * If an exception occurs converting the object parameter:
     * FFDC for the exception is suppressed: Callers should handle the thrown
     * IllegalArgumentException as appropriate.
     *
     * @param propertyKey
     *            The name of the configuration property.
     * @param obj
     *            The object retrieved from the configuration property map/dictionary.
     * @param defaultValue
     *            The default value that should be applied if the object is null.
     *
     * @return Collection of strings parsed/retrieved from obj, or default value if obj is null
     * @throws IllegalArgumentException If value is not a String, String collection, or String array, or if an error
     *             occurs while converting/casting the object to the return parameter type.
     */
    @SuppressWarnings("unchecked")
    @FFDCIgnore(Exception.class)
    public static Collection<String> parseStringCollection(String propertyKey, Object obj, Collection<String> defaultValue) {
        if (obj != null) {
            try {
                if (obj instanceof Collection) {
                    return (Collection<String>) obj;
                } else if (obj instanceof String) {
                    String commaList = (String) obj;
                    // split the string, consuming/removing whitespace
                    return Arrays.asList(commaList.split("\\s*,\\s*"));
                } else if (obj instanceof String[]) {
                    return Arrays.asList((String[]) obj);
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Collection of strings could not be parsed: key=" + propertyKey + ", value=" + obj, e);
            }

            // unknown type
            throw new IllegalArgumentException("Collection of strings could not be parsed: key=" + propertyKey + ", value=" + obj);
        }

        return defaultValue;
    }

    /**
     * Convert a collection of String values back into a comma separated list
     *
     * @param values The collection of strings
     */
    public static String getStringFromCollection(Collection<String> values) {
        StringBuilder builder = new StringBuilder();
        if (values != null) {
            for (String value : values) {
                builder.append(value).append(',');
            }
            if (builder.charAt(builder.length() - 1) == ',')
                builder.deleteCharAt(builder.length() - 1);
        }
        return builder.toString();
    }

    public static String getEnvValue(final String envName) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {

            @Override
            public String run() {
                return System.getenv(envName);
            }

        });
    }

    public static boolean isMessageFormatValueValid(String formatValue) {
        if (formatValue.toLowerCase().equals(LoggingConstants.DEFAULT_MESSAGE_FORMAT) || formatValue.toLowerCase().equals(LoggingConstants.JSON_FORMAT)
            || formatValue.toLowerCase().equals(LoggingConstants.DEPRECATED_DEFAULT_FORMAT)) {
            return true;
        }
        return false;
    }

    public static boolean isConsoleFormatValueValid(String formatValue) {
        if (formatValue.toLowerCase().equals(LoggingConstants.DEFAULT_CONSOLE_FORMAT) || formatValue.toLowerCase().equals(LoggingConstants.DEFAULT_MESSAGE_FORMAT)
            || formatValue.toLowerCase().equals(LoggingConstants.JSON_FORMAT) || formatValue.toLowerCase().equals(LoggingConstants.DEPRECATED_DEFAULT_FORMAT)) {
            return true;
        }
        return false;
    }
}
