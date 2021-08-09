/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Utility class to simplify retrieving values from injected config.
 * <p>
 * The presence of the Metatype service will cause ConfigAdmin to use
 * real types (in addition to String) as values in the the configuration
 * properties provided to declarative services components.
 * <p>
 * This class provides utility methods for retrieving a desired
 * type (a boolean, a long, an array of strings.. ) from a component's
 * configuration properties. They handle objects of either type (those already
 * created/populated as the right type, or values that
 * are still encoded as strings).
 */
public class MetatypeUtils {
    static final TraceComponent tc = Tr.register(MetatypeUtils.class);

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
     * Parse a boolean from the provided config value: checks for whether or not
     * the object read from the Service/Component configuration is a String
     * or a Metatype converted boolean.
     * <p>
     * If an exception occurs converting the object parameter:
     * A translated warning message will be issued using the provided propertyKey and object
     * as parameters. FFDC for the exception is suppressed: Callers should handle the thrown
     * IllegalArgumentException as appropriate.
     * 
     * @param configAlias
     *            Name of config (pid or alias) associated with a registered service
     *            or DS component.
     * @param propertyKey
     *            The key used to retrieve the property value from the map.
     *            Used in the warning message if the value is badly formed.
     * @param obj
     *            The object retrieved from the configuration property map/dictionary.
     * 
     * @return boolean parsed from obj, the default value if obj is null.
     * @throws IllegalArgumentException If value is not a String/Boolean, or if the String
     *             boolean is not "true" or "false" (ignoring case)
     */
    public static boolean parseBoolean(Object configAlias, String propertyKey, Object obj, boolean defaultValue) {
        if (obj != null) {
            if (obj instanceof String) {
                String value = (String) obj;
                if (value.equalsIgnoreCase("true")) {
                    return true;
                } else if (value.equalsIgnoreCase("false")) {
                    return false;
                } else {
                    Tr.warning(tc, "invalidBoolean", configAlias, propertyKey, obj);
                    throw new IllegalArgumentException("Boolean value could not be parsed: key=" + propertyKey + ", value=" + obj);
                }
            } else if (obj instanceof Boolean) {
                return (Boolean) obj;
            }

            // unknown type
            Tr.warning(tc, "invalidBoolean", configAlias, propertyKey, obj);
            throw new IllegalArgumentException("Boolean value could not be parsed: key=" + propertyKey + ", value=" + obj);
        }
        return defaultValue;
    }

    /**
     * Parse a string array from the provided config value: returns
     * an array of strings generated from either a comma-separated single
     * string value, or a metatype generated string array.
     * <p>
     * If an exception occurs converting the object parameter:
     * A translated warning message will be issued using the provided propertyKey and object
     * as parameters. FFDC for the exception is suppressed: Callers should handle the thrown
     * IllegalArgumentException as appropriate.
     * 
     * @param configAlias
     *            Name of config (pid or alias) associated with a registered service
     *            or DS component.
     * @param propertyKey
     *            The key used to retrieve the property value from the map.
     *            Used in the warning message if the value is badly formed.
     * @param obj
     *            The object retrieved from the configuration property map/dictionary.
     * @param defaultValue
     *            The default value that should be applied if the object is null.
     * @return An array of strings parsed from the obj parameter, or default value if obj is null
     * @throws IllegalArgumentException If value is not a String or String array, or if an error
     *             occurs while converting/casting the object to the return parameter type.
     */
    @SuppressWarnings("unchecked")
    @FFDCIgnore(Exception.class)
    public static String[] parseStringArray(Object configAlias, String propertyKey, Object obj, String[] defaultValue) {
        final String[] emptyArray = new String[0];

        if (obj != null) {
            try {
                if (obj instanceof String[]) {
                    return (String[]) obj;
                } else if (obj instanceof String) {
                    String commaList = (String) obj;
                    // split the string, consuming/removing whitespace
                    return commaList.split("\\s*,\\s*");
                } else if (obj instanceof Collection) {
                    return ((Collection<String>) obj).toArray(emptyArray);
                }
            } catch (Exception e) {
                Tr.warning(tc, "invalidStringArray", configAlias, propertyKey, obj);
                throw new IllegalArgumentException("String array value could not be parsed: key=" + propertyKey + ", value=" + obj, e);
            }

            // unknown type
            Tr.warning(tc, "invalidStringArray", configAlias, propertyKey, obj);
            throw new IllegalArgumentException("String array value could not be parsed: key=" + propertyKey + ", value=" + obj);
        }

        return defaultValue;
    }

    /**
     * Parse a string collection from the provided config value: returns
     * a collection of strings generated from either a comma-separated single
     * string value, or a metatype provided string collection.
     * <p>
     * If an exception occurs converting the object parameter:
     * A translated warning message will be issued using the provided propertyKey and object
     * as parameters. FFDC for the exception is suppressed: Callers should handle the thrown
     * IllegalArgumentException as appropriate.
     * 
     * @param configAlias
     *            Name of config (pid or alias) associated with a registered service
     *            or DS component.
     * @param propertyKey
     *            The key used to retrieve the property value from the map.
     *            Used in the warning message if the value is badly formed.
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
    public static Collection<String> parseStringCollection(Object configAlias, String propertyKey, Object obj, Collection<String> defaultValue) {
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
                Tr.warning(tc, "invalidStringCollection", configAlias, propertyKey, obj);
                throw new IllegalArgumentException("Collection of strings could not be parsed: key=" + propertyKey + ", value=" + obj, e);
            }

            // unknown type
            Tr.warning(tc, "invalidStringCollection", configAlias, propertyKey, obj);
            throw new IllegalArgumentException("Collection of strings could not be parsed: key=" + propertyKey + ", value=" + obj);
        }

        return defaultValue;
    }

    /**
     * Parse a long from the provided config value: checks for whether or not
     * the object read from the Service/Component configuration is a String
     * or a Metatype converted long
     * <p>
     * If an exception occurs converting the object parameter:
     * A translated warning message will be issued using the provided propertyKey and object
     * as parameters. FFDC for the exception is suppressed: Callers should handle the thrown
     * IllegalArgumentException as appropriate.
     * 
     * @param configAlias
     *            Name of config (pid or alias) associated with a registered service
     *            or DS component.
     * @param propertyKey
     *            The key used to retrieve the property value from the map.
     *            Used in the warning message if the value is badly formed.
     * @param obj
     *            The object retrieved from the configuration property map/dictionary.
     * @param defaultValue
     *            The default value that should be applied if the object is null.
     * 
     * @return Long parsed/retrieved from obj, or default value if obj is null
     * @throws IllegalArgumentException If value is not a String/Short/Integer/Long, or if an error
     *             occurs while converting/casting the object to the return parameter type.
     */
    @FFDCIgnore(Exception.class)
    public static long parseLong(Object configAlias, String propertyKey, Object obj, long defaultValue) {
        if (obj != null) {
            try {
                if (obj instanceof String)
                    return Long.parseLong((String) obj);
                else if (obj instanceof Short)
                    return ((Short) obj).longValue();
                else if (obj instanceof Integer)
                    return ((Integer) obj).longValue();
                else if (obj instanceof Long)
                    return (Long) obj;
            } catch (Exception e) {
                Tr.warning(tc, "invalidLong", configAlias, propertyKey, obj);
                throw new IllegalArgumentException("Long value could not be parsed: key=" + propertyKey + ", value=" + obj, e);
            }

            // unknown type
            Tr.warning(tc, "invalidLong", configAlias, propertyKey, obj);
            throw new IllegalArgumentException("Long value could not be parsed: key=" + propertyKey + ", value=" + obj);
        }

        return defaultValue;
    }

    /**
     * Parse a int from the provided config value: checks for whether or not
     * the object read from the Service/Component configuration is a String
     * or a Metatype converted int
     * <p>
     * If an exception occurs converting the object parameter:
     * A translated warning message will be issued using the provided propertyKey and object
     * as parameters. FFDC for the exception is suppressed: Callers should handle the thrown
     * IllegalArgumentException as appropriate.
     * 
     * @param configAlias
     *            Name of config (pid or alias) associated with a registered service
     *            or DS component.
     * @param propertyKey
     *            The key used to retrieve the property value from the map.
     *            Used in the warning message if the value is badly formed.
     * @param obj
     *            The object retrieved from the configuration property map/dictionary.
     * @param defaultValue
     *            The default value that should be applied if the object is null.
     * 
     * @return Integer parsed/retrieved from obj, or default value if obj is null
     * @throws IllegalArgumentException If value is not a String/Short/Integer/Long, or if an error
     *             occurs while converting/casting the object to the return parameter type.
     */
    @FFDCIgnore(Exception.class)
    public static int parseInteger(Object configAlias, String propertyKey, Object obj, int defaultValue) {
        if (obj != null) {
            try {
                if (obj instanceof String)
                    return Integer.parseInt((String) obj);
                else if (obj instanceof Short)
                    return ((Short) obj).intValue();
                else if (obj instanceof Integer)
                    return (Integer) obj;
                else if (obj instanceof Long)
                    return ((Long) obj).intValue();
            } catch (Exception e) {
                Tr.warning(tc, "invalidInteger", configAlias, propertyKey, obj);
                throw new IllegalArgumentException("Integer value could not be parsed: key=" + propertyKey + ", value=" + obj, e);
            }

            // unknown type
            Tr.warning(tc, "invalidInteger", configAlias, propertyKey, obj);
            throw new IllegalArgumentException("Integer value could not be parsed: key=" + propertyKey + ", value=" + obj);
        }

        return defaultValue;
    }

    /**
     * Parse a string array from the provided config value: returns
     * an array of strings generated from either a comma-separated single
     * string value, or a metatype generated string array.
     * <p>
     * If an exception occurs converting the object parameter:
     * A translated warning message will be issued using the provided propertyKey and object
     * as parameters. FFDC for the exception is suppressed: Callers should handle the thrown
     * IllegalArgumentException as appropriate.
     * 
     * @param configAlias
     *            Name of config (pid or alias) associated with a registered service
     *            or DS component.
     * @param propertyKey
     *            The key used to retrieve the property value from the map.
     *            Used in the warning message if the value is badly formed.
     * @param obj
     *            The object retrieved from the configuration property map/dictionary.
     * @param defaultValue
     *            The default value that should be applied if the object is null.
     * @return An array of strings parsed from the obj parameter, or default value if obj is null
     * @throws IllegalArgumentException If value is not a String or String array, or if an error
     *             occurs while converting/casting the object to the return parameter type.
     */
    @FFDCIgnore(Exception.class)
    public static int[] parseIntegerArray(Object configAlias, String propertyKey, Object obj, int[] defaultValue) {
        if (obj != null) {
            try {
                if (obj instanceof int[]) {
                    return (int[]) obj;
                } else if (obj instanceof String) {
                    String commaList = (String) obj;
                    // split the string, consuming/removing whitespace --> push into a list for below.
                    obj = Arrays.asList(commaList.split("\\s*,\\s*"));
                }

                if (obj instanceof Collection<?>) {
                    Collection<?> c = (Collection<?>) obj;
                    int[] newArray = new int[c.size()];
                    int i = 0;
                    for (Object o2 : c) {
                        if (o2 instanceof String)
                            newArray[i] = Integer.parseInt((String) o2);
                        else if (o2 instanceof Short)
                            newArray[i] = ((Short) o2).intValue();
                        else if (o2 instanceof Integer)
                            newArray[i] = (Integer) o2;
                        else if (o2 instanceof Long)
                            newArray[i] = ((Long) o2).intValue();

                        i++;
                    }
                    return newArray;
                }
            } catch (Exception e) {
                Tr.warning(tc, "invalidIntegerArray", configAlias, propertyKey, obj);
                throw new IllegalArgumentException("Integer array value could not be parsed: key=" + propertyKey + ", value=" + obj, e);
            }

            // unknown type
            Tr.warning(tc, "invalidIntegerArray", configAlias, propertyKey, obj);
            throw new IllegalArgumentException("Integer array value could not be parsed: key=" + propertyKey + ", value=" + obj);
        }

        return defaultValue;
    }

    /**
     * Parse a duration from the provided config value: checks for whether or not
     * the object read from the Service/Component configuration is a String
     * or a Metatype converted long duration value
     * <p>
     * If an exception occurs converting the object parameter:
     * A translated warning message will be issued using the provided propertyKey and object
     * as parameters. FFDC for the exception is suppressed: Callers should handle the thrown
     * IllegalArgumentException as appropriate.
     * 
     * @param configAlias
     *            Name of config (pid or alias) associated with a registered service
     *            or DS component.
     * @param propertyKey
     *            The key used to retrieve the property value from the map.
     *            Used in the warning message if the value is badly formed.
     * @param obj
     *            The object retrieved from the configuration property map/dictionary.
     * @param defaultValue
     *            The default value that should be applied if the object is null.
     * 
     * @return Long parsed/retrieved from obj, or default value if obj is null
     * @throws IllegalArgumentException If value is not a Long, or if an error
     *             occurs while converting/casting the object to the return parameter type.
     */
    public static long parseDuration(Object configAlias, String propertyKey, Object obj, long defaultValue) {
        return parseDuration(configAlias, propertyKey, obj, defaultValue, TimeUnit.MILLISECONDS);
    }

    /**
     * Parse a duration from the provided config value: checks for whether or not
     * the object read from the Service/Component configuration is a String
     * or a Metatype converted long duration value
     * <p>
     * If an exception occurs converting the object parameter:
     * A translated warning message will be issued using the provided propertyKey and object
     * as parameters. FFDC for the exception is suppressed: Callers should handle the thrown
     * IllegalArgumentException as appropriate.
     * 
     * @param configAlias
     *            Name of config (pid or alias) associated with a registered service
     *            or DS component.
     * @param propertyKey
     *            The key used to retrieve the property value from the map.
     *            Used in the warning message if the value is badly formed.
     * @param obj
     *            The object retrieved from the configuration property map/dictionary.
     * @param defaultValue
     *            The default value that should be applied if the object is null.
     * @param units
     *            The unit of time for the duration value. This is only used when
     *            converting from a String value.
     * 
     * @return Long parsed/retrieved from obj, or default value if obj is null
     * @throws IllegalArgumentException If value is not a String/Short/Integer/Long, or if an error
     *             occurs while converting/casting the object to the return parameter type.
     */
    @FFDCIgnore(Exception.class)
    public static long parseDuration(Object configAlias, String propertyKey, Object obj, long defaultValue, TimeUnit units) {
        if (obj != null) {
            try {
                if (obj instanceof String)
                    return evaluateDuration((String) obj, units);
                else if (obj instanceof Long)
                    return (Long) obj;
            } catch (Exception e) {
                Tr.warning(tc, "invalidDuration", configAlias, propertyKey, obj);
                throw new IllegalArgumentException("Duration value could not be parsed: key=" + propertyKey + ", value=" + obj, e);
            }

            // unknown type
            Tr.warning(tc, "invalidDuration", configAlias, propertyKey, obj);
            throw new IllegalArgumentException("Duration value could not be parsed: key=" + propertyKey + ", value=" + obj);
        }

        return defaultValue;
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
     * Converts a String value into a token value by collapsing whitespace. All whitespace at the beginning and
     * end of the String will be removed, and contiguous sequences of whitespace will be replaced with a single
     * space character.
     * 
     * @param strVal
     *            A String to be trimmed
     * @return String The collapsed String
     */
    public static String evaluateToken(String strVal) {
        return strVal == null ? null : collapseWhitespace(strVal);
    }

    @Trivial
    private static boolean isSpace(char ch) {
        return ch == 0x20 || ch == 0xA || ch == 0xD || ch == 0x9;
    }

    /**
     * Collapses contiguous sequences of whitespace to a single 0x20.
     * Leading and trailing whitespace is removed.
     */
    @Trivial
    private static String collapseWhitespace(String value) {
        final int length = value.length();
        for (int i = 0; i < length; ++i) {
            if (isSpace(value.charAt(i))) {
                return collapse0(value, i, length);
            }
        }
        return value;
    }

    @Trivial
    private static String collapse0(String value, int i, int length) {
        StringBuilder sb = null;
        boolean needToWriteSpace = false;
        if (i > 0) {
            // Copy the start of the CharSequence
            needToWriteSpace = true;
            sb = new StringBuilder();
            sb.append(value, 0, i);
        } else {
            // Skip over leading whitespace
            while (++i < length) {
                char c = value.charAt(i);
                if (!isSpace(c)) {
                    sb = new StringBuilder();
                    sb.append(c);
                    break;
                }
            }
            // All whitespace so just return the empty string
            if (i == length) {
                return "";
            }
        }
        // Process the rest of the CharSequence
        while (++i < length) {
            char c = value.charAt(i);
            if (!isSpace(c)) {
                if (needToWriteSpace) {
                    sb.append(' ');
                    needToWriteSpace = false;
                }
                sb.append(c);
            } else {
                needToWriteSpace = true;
            }
        }
        return sb.toString();
    }

}
