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
package com.ibm.ws.security.common.config;

import java.util.Map;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.common.TraceConstants;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

public class CommonConfigUtils {
    public static final TraceComponent tc = Tr.register(CommonConfigUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    /**
     * Returns the value for the configuration attribute matching the key provided. If the value does not exist or is empty, the
     * resulting value will be {@code null}.
     */
    public String getConfigAttribute(Map<String, Object> props, String key) {
        return getConfigAttributeWithDefaultValue(props, key, null);
    }

    /**
     * Returns the value for the configuration attribute matching the key provided. If the value does not exist or is empty, the
     * provided default value will be returned.
     */
    public String getConfigAttributeWithDefaultValue(Map<String, Object> props, String key, String defaultValue) {
        String result = getAndTrimConfigAttribute(props, key);
        if (key != null && result == null) {
            if (defaultValue != null) {
                result = defaultValue;
            }
        }
        return result;
    }

    /**
     * Returns the value for the configuration attribute matching the key provided. If the value does not exist or is empty, the
     * resulting value will be {@code null} and an error message will be logged.
     */
    public String getRequiredConfigAttribute(Map<String, Object> props, String key) {
        return getRequiredConfigAttributeWithDefaultValueAndConfigId(props, key, null, null);
    }

    /**
     * Returns the value for the configuration attribute matching the key provided. If the value does not exist or is empty, the
     * resulting value will be {@code null} and an error message will be logged.
     */
    public String getRequiredConfigAttributeWithDefaultValue(Map<String, Object> props, String key, String defaultValue) {
        return getRequiredConfigAttributeWithDefaultValueAndConfigId(props, key, defaultValue, null);
    }

    /**
     * Returns the value for the configuration attribute matching the key provided. If the value does not exist or is empty, the
     * resulting value will be {@code null} and an error message will be logged.
     */
    public String getRequiredConfigAttributeWithConfigId(Map<String, Object> props, String key, String configId) {
        return getRequiredConfigAttributeWithDefaultValueAndConfigId(props, key, null, configId);
    }

    /**
     * Returns the value for the configuration attribute matching the key provided. If the value does not exist or is empty, the
     * provided default value will be returned. If the default value is also null, an error message will be logged.
     */
    public String getRequiredConfigAttributeWithDefaultValueAndConfigId(Map<String, Object> props, String key, String defaultValue, String configId) {
        String result = getAndTrimConfigAttribute(props, key);
        if (key != null && result == null) {
            if (defaultValue != null) {
                result = defaultValue;
            } else {
                logMessageForMissingRequiredAttribute(key, configId);
            }
        }
        return result;
    }

    void logMessageForMissingRequiredAttribute(String key, String configId) {
        if (configId != null) {
            Tr.warning(tc, "CONFIG_REQUIRED_ATTRIBUTE_NULL_WITH_CONFIG_ID", new Object[] { key, configId });
        } else {
            Tr.warning(tc, "CONFIG_REQUIRED_ATTRIBUTE_NULL", new Object[] { key });
        }
    }

    String getAndTrimConfigAttribute(Map<String, Object> props, String key) {
        return trim((String) props.get(key));
    }
    
    @Sensitive
    public String processProtectedString(Map<String, Object> props, String cfgKey) {
        String secret;
        Object o = props.get(cfgKey);
        if (o != null) {
            if (o instanceof SerializableProtectedString) {
                secret = new String(((SerializableProtectedString) o).getChars());
            } else {
                secret = (String) o;
            }
        } else {
            secret = null;
        }
        // decode
        secret = PasswordUtil.passwordDecode(secret);
        return secret;
    }

    /**
     * Returns the value for the configuration attribute matching the key provided. If the value does not exist or is empty, the
     * resulting value will be {@code null}.
     */
    public String[] getStringArrayConfigAttribute(Map<String, Object> props, String key) {
        return trim((String[]) props.get(key));
    }

    /**
     * Returns the value for the configuration attribute matching the key provided. If the value does not exist or is empty, the
     * provided default value will be returned.
     */
    public boolean getBooleanConfigAttribute(Map<String, Object> props, String key, boolean defaultValue) {
        if (props.containsKey(key)) {
            return (Boolean) props.get(key);
        }
        return defaultValue;
    }

    /**
     * Returns the value for the configuration attribute matching the key provided. If the value does not exist or is empty, the
     * provided default value will be returned.
     */
    public int getIntegerConfigAttribute(Map<String, Object> props, String key, int defaultValue) {
        if (props.containsKey(key)) {
            return (Integer) props.get(key);
        }
        return defaultValue;
    }

    /**
     * Returns the value for the configuration attribute matching the key provided. If the value does not exist or is empty, the
     * provided default value will be returned.
     */
    public long getLongConfigAttribute(Map<String, Object> props, String key, long defaultValue) {
        if (props.containsKey(key)) {
            return (Long) props.get(key);
        }
        return defaultValue;
    }

    public static String[] trim(final String[] originals) {
        if (originals == null || originals.length == 0) {
            return null;
        }
        String[] tmpResults = new String[originals.length];
        int numTrimmedEntries = 0;
        for (int i = 0; i < originals.length; i++) {
            String original = trim(originals[i]);
            if (original != null) {
                tmpResults[numTrimmedEntries++] = original;
            }
        }
        if (numTrimmedEntries == 0) {
            return null;
        }
        String[] results = new String[numTrimmedEntries];
        System.arraycopy(tmpResults, 0, results, 0, numTrimmedEntries);
        return results;
    }

    /**
     * Calls {@code java.lang.String.trim()} on the provided value.
     * 
     * @param original
     * @return {@code null} if {@code original} is {@code null} or empty after the {@code java.lang.String.trim()} operation.
     *         Otherwise returns the trimmed result.
     */
    @Trivial
    public static String trim(String original) {
        if (original == null) {
            return null;
        }
        String result = original.trim();
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

}
