/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.http.utils;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.http.channel.internal.HttpMessages;

import io.openliberty.http.options.EndpointOption;

/**
 * Utility class for parsing configuration values in the HTTP transport.
 * This class provides methods to extract and parse different configuration 
 * options from a Map using the EndpointOption interface.
 */
public class HttpConfigUtils {

    private static final TraceComponent tc = Tr.register(HttpConfigUtils.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    private HttpConfigUtils() {
        // Private constructor
    }

   /**
     * Generic method to retrieve and parse different types of configuration values.
     * 
     * @param <T> The type of value to be returned
     * @param config The configuration map
     * @param option The configuration option
     * @param parser A function to parse the value from Object to the desired type
     * @return The parsed value, if present and valid, or the default value otherwise
     */
    public static <T> T getOptionValue(Map<String, Object> config, EndpointOption option, Function<Object, Optional<T>> parser) {
        Object value = config.get(option.getKey());
        if (Objects.isNull(value)) {
            return (T) option.getDefaultValue();
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Config: " + option.getKey() + " is " + value);
        }

        return parser.apply(value).orElse((T) option.getDefaultValue());
  
    }

    /**
     * Generic method to retrieve and parse different types of configuration values.
     * 
     * @param <T> The type of value to be returned
     * @param config The configuration map
     * @param option The configuration option
     * @return The parsed value, if present and valid, or the default value otherwise
     */
    public static <T> T getOptionValue(Map<String, Object> config, EndpointOption option) {

        Object value = config.get(option.getKey());

        if (Objects.isNull(value)) {
       
            return (T) option.getDefaultValue();
        }

        try {
            if(option.getValueType() == Integer.class && value instanceof Number){
                return (T) Integer.valueOf(((Number) value).intValue());
            } else if (option.getValueType() == Boolean.class && value instanceof Boolean) {
                return (T) value;
            } else if (option.getValueType().isInstance(value)) {
                return (T) value;
            } else {
                return (T) option.getDefaultValue();
            }
        } catch (ClassCastException e) {
            return (T) option.getDefaultValue();
        }
    }

    /**
     * Generic method to retrieve and parse different types of configuration values.
     * 
     * @param <T> The type of value to be returned
     * @param config The configuration map
     * @param key The configuration key
     * @param parser A function to parse the value from Object to the desired type
     * @return The parsed value, if present and valid, or an empty Optional otherwise
     */
    private static<T> Optional<T> getValue(Map<String, Object> config, String key, Function<Object, Optional<T>> parser) {
        Object value = config.get(key);
        if (Objects.isNull(value)) {
            return Optional.empty();
        }
        return parser.apply(value);
    }

    /**
     * Generic method to retrieve and parse different types of configuration values.
     * 
     * @param <T> The type of value to be returned
     * @param value The value to be parsed
     * @param valueType The type of value to be returned
     * @return The parsed value, if present and valid, or an empty Optional otherwise
     */
    @SuppressWarnings("unchecked")
    private static <T> Optional<T> parseValue(Object value, Class<T> valueType) {
        if (valueType == String.class) {
            return (Optional<T>) parseString(value);
        } else if(valueType == String[].class) {
            return (Optional<T>) parseStringArray(value);
        } else if (valueType == Boolean.class) {
            return (Optional<T>) parseBoolean(value);
        } else if (valueType == Integer.class) {
            return (Optional<T>) parseInteger(value);
        } else if (valueType == Long.class) {
            return (Optional<T>) parseLong(value);
        } else {
            Tr.warning(tc, "Unsupported value type for config: {0}", valueType);
            return Optional.empty();
        }
            
      
    }

    /**
     * Parses a String value from an Object.
     * 
     * @param value The Object to be parsed
     * @return An Optional containing the String value, if the Object is a String, or an empty Optional otherwise
     */
    private static Optional<String> parseString(Object value) {
        if (value instanceof String) {
            return Optional.of((String) value);
        }

        Tr.warning(tc, "Invalid string value for config: {0}", value);

        return Optional.empty();
    }

    /**
     * Parses a String array value from an Object.
     * 
     * @param value The Object to be parsed
     * @return An Optional containing the String array value, if the Object is a String array or a comma-separated String, or an empty Optional otherwise
     */
    private static Optional<String[]> parseStringArray(Object value) {
        if (value instanceof String[]) {
            return Optional.of((String[]) value);
        } else if (value instanceof String) {
            return Optional.of(((String) value).split("\\s*,\\s*"));
        }

        Tr.warning(tc, "Invalid string array value for config: {0}", value);

        return Optional.empty();
    }

    /**
     * Parses a Boolean value from an Object.
     * 
     * @param value The Object to be parsed
     * @return An Optional containing the Boolean value, if the Object is a Boolean, or an empty Optional otherwise
     */
    public static Optional<Boolean> parseBoolean(Object value) {
        if (value instanceof Boolean) {
            return Optional.of((Boolean) value);
        } else if(value instanceof String) {
            String stringValue = ((String) value).trim().toLowerCase();
            if(stringValue.equals("true") || stringValue.equals("false")) {
                return Optional.of(Boolean.parseBoolean(stringValue));
            }
        }

        Tr.warning(tc, "Invalid boolean value for config: {0}", value);

        return Optional.empty();
    }

    /**
     * Parses an Integer value from an Object.
     * 
     * @param value The Object to be parsed
     * @return An Optional containing the Integer value, if the Object is an Integer, or an empty Optional otherwise
     */
    public static Optional<Integer> parseInteger(Object value) {
        if (value instanceof Number) {
            return Optional.of(((Number) value).intValue());
        } else if(value instanceof String) {
            try {
                return Optional.of(Integer.parseInt(((String) value).trim()));
            } catch (NumberFormatException e) {
                Tr.warning(tc, "Invalid integer value for config: {0}", value);
                FFDCFilter.processException(e, HttpConfigUtils.class.getName(), ".parseInteger", "1");
            }
            
        }

        return Optional.empty();
    }

    /**
     * Parses a Long value from an Object.
     * 
     * @param value The Object to be parsed
     * @return An Optional containing the Long value, if the Object is a Long, or an empty Optional otherwise
     */
    public static Optional<Long> parseLong(Object value) {
        if (value instanceof Number) {
            return Optional.of(((Number) value).longValue());
        } else if(value instanceof String) {
            try {
                return Optional.of(Long.parseLong(((String) value).trim()));
            } catch (NumberFormatException e) {
                Tr.warning(tc, "Invalid long value for config: {0}", value);
                FFDCFilter.processException(e, HttpConfigUtils.class.getName(), ".parseLong", "1");
            }
            
        }

        return Optional.empty();
    }
}