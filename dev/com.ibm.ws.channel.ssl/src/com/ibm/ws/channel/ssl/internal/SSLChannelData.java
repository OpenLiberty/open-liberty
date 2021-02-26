/*******************************************************************************
 * Copyright (c) 2003, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channel.ssl.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.FlowType;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.channelfw.exception.ChannelException;

/**
 * This class will manage all the settings and data associated with a particular
 * instance of the SSL Channel.
 */
public class SSLChannelData {

    /** Trace component for WAS */
    protected static final TraceComponent tc = Tr.register(SSLChannelData.class, SSLChannelConstants.SSL_TRACE_NAME, SSLChannelConstants.SSL_BUNDLE);

    /** Property names for memory allocation in SSL channel. */
    private static final String ENCRYPT_BUFFERS_DIRECT = "encryptBuffersDirect";
    private static final String DECRYPT_BUFFERS_DIRECT = "decryptBuffersDirect";

    private static final String SSLSESSION_CACHE_SIZE = "SSLSessionCacheSize";
    private static final String SSLSESSION_TIMEOUT = "SSLSessionTimeout";
    private static final String SSLSESSION_TIMEOUT_8500 = "sessionTimeout";

    /** Defaults for some properties. */
    private static final String DEFAULT_ENCRYPT_BUFFERS_DIRECT = "true";
    private static final String DEFAULT_DECRYPT_BUFFERS_DIRECT = "false";
    private static final int DEFAULT_SSLSESSION_CACHE_SIZE = 100;
    private static final int DEFAULT_SSLSESSION_TIMEOUT = 86400;

    static final String ALIAS_KEY = "alias";

    /** Name of this channel. */
    private String name = null;
    /** Whether buffers for encrypted data should be allocated by us or not. */
    private final boolean encryptBuffersDirect;
    /** Whether buffers for decrypted data should be allocated by us or not. */
    private final boolean decryptBuffersDirect;
    /** Weight used when channel framework does discrimination. */
    private final int weight;
    /** Whether or not this channel data is for an inbound channel. */
    private final boolean isInbound;
    /** Whether client authentication is needed. */
    private boolean clientAuthentication;
    /** Properties to use in this channel. */
    private Properties properties;
    /** Size limit to place on the SSLSession cache inside the JSSE code */
    private int sslSessionCacheSize = 0;
    /** Timeout to apply to the SSLSessions, default it 24 hours */
    private int sslSessionTimeout = 0;

    /**
     * Method used for debug.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(256);
        sb.append("SSLChannelData: ").append(this.name);
        sb.append("\n\tencryptBuffersDirect = ").append(this.encryptBuffersDirect);
        sb.append("\n\tdecryptBuffersDirect = ").append(this.decryptBuffersDirect);
        sb.append("\n\tweight = ").append(this.weight);
        sb.append("\n\tisInbound = ").append(this.isInbound);
        sb.append("\n\tclientAuthentication = ").append(this.clientAuthentication);
        sb.append("\n\tsession cache size = ").append(this.sslSessionCacheSize);
        sb.append("\n\tsession timeout = ").append(this.sslSessionTimeout);
        return sb.toString();
    }

    /**
     * Constructor.
     *
     * Note, the channel framework ensures that a non null data object
     * will be passed in and it will include a non null set of properties. Therefore,
     * extra checking and exception throwing is not necessary.
     *
     * @param inputData channel data with generic map of properties
     * @throws ChannelException
     */
    public SSLChannelData(ChannelData inputData) throws ChannelException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "constructor: SSLChannelData");
        }
        this.name = inputData.getName();
        this.weight = inputData.getDiscriminatorWeight();
        this.isInbound = inputData.isInbound();
        // Make a copy of the property map to use.

        // allow custom property keys in the property bag to be case insensitive and still work.  do this by restuffing the bag
        restuffBag(inputData.getPropertyBag());

        this.properties = new Properties();
        for (Entry<Object, Object> entry : inputData.getPropertyBag().entrySet()) {
            this.properties.put(entry.getKey(), entry.getValue());
        }

        StringBuilder errors = new StringBuilder();
        // Memory management properties for the SSL channel.
        this.encryptBuffersDirect = getBooleanProperty(ENCRYPT_BUFFERS_DIRECT, DEFAULT_ENCRYPT_BUFFERS_DIRECT, errors);
        // On Z, all allocations SHOULD be direct in the config
        this.decryptBuffersDirect = getBooleanProperty(DECRYPT_BUFFERS_DIRECT, DEFAULT_DECRYPT_BUFFERS_DIRECT, errors);

        this.sslSessionCacheSize = getIntProperty(SSLSESSION_CACHE_SIZE, true, DEFAULT_SSLSESSION_CACHE_SIZE, errors);
        this.sslSessionTimeout = getIntProperty(SSLSESSION_TIMEOUT, true, DEFAULT_SSLSESSION_TIMEOUT * 1000, errors);
        // The duration tag on the sslSession Metatype will change the value to milliseconds. As such, the value
        // obtained from this will millis, convert to seconds.
        this.sslSessionTimeout = (int) (TimeUnit.MILLISECONDS.toSeconds(sslSessionTimeout));
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Property sslSessionTimout converted to seconds: " + sslSessionTimeout);
        }

        // Throw an exception if errors were found in reading data.
        if (errors.length() != 0) {
            Tr.error(tc, SSLChannelConstants.INVALID_SECURITY_PROPERTIES, errors.toString());
            throw new ChannelException("Invalid property values found:\n" + errors.toString());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "constructor: SSLChannelData");
        }
    }

    public void restuffBag(Map<Object, Object> m) {

        String key = null;
        Object value = null;
        boolean realTimeoutSet = false;
        Map<Object, Object> temp = new HashMap<Object, Object>();

        for (Entry<Object, Object> entry : m.entrySet()) {
            key = (String) entry.getKey();
            value = entry.getValue();
            // if key matches

            if (key.equalsIgnoreCase(ENCRYPT_BUFFERS_DIRECT)) {
                temp.put(ENCRYPT_BUFFERS_DIRECT, value);
                continue;
            }
            if (key.equalsIgnoreCase(DECRYPT_BUFFERS_DIRECT)) {
                temp.put(DECRYPT_BUFFERS_DIRECT, value);
                continue;
            }
            if (key.equalsIgnoreCase(SSLSESSION_CACHE_SIZE)) {
                temp.put(SSLSESSION_CACHE_SIZE, value);
                continue;
            }
            if (key.equalsIgnoreCase(SSLSESSION_TIMEOUT)) {
                temp.put(SSLSESSION_TIMEOUT, value);
                realTimeoutSet = true;
                continue;
            }
            if ((realTimeoutSet == false) && (key.equalsIgnoreCase(SSLSESSION_TIMEOUT_8500))) {
                // we only want the real timeout in the map
                temp.put(SSLSESSION_TIMEOUT, value);
                continue;
            }

            temp.put(key, value);
        }

        m.clear();
        m.putAll(temp);

    }

    /**
     * Query the name of this channel.
     *
     * @return String
     */
    public String getName() {
        return this.name;
    }

    /**
     * Query whether the encrypted buffers should be direct or indirect.
     *
     * @return boolean
     */
    public boolean getEncryptBuffersDirect() {
        return this.encryptBuffersDirect;
    }

    /**
     * Query whether the decrypted buffers should be allocated direct or not.
     *
     * @return boolean
     */
    public boolean getDecryptBuffersDirect() {
        return this.decryptBuffersDirect;
    }

    /**
     * Query the weight of this channel.
     *
     * @return int
     */
    public int getWeight() {
        return this.weight;
    }

    /**
     * Query whether this channel is inbound or not.
     *
     * @return boolean
     */
    public boolean isInbound() {
        return this.isInbound;
    }

    /**
     * Query the configured value for the SSLSession cache size.
     *
     * @return int
     */
    public int getSSLSessionCacheSize() {
        return this.sslSessionCacheSize;
    }

    /**
     * Query the configured value for the SSLSession timeout value.
     *
     * @return int (seconds)
     */
    public int getSSLSessionTimeout() {
        return this.sslSessionTimeout;
    }

    /**
     * Query the inbound vs outbound flow of this channel.
     *
     * @return boolean
     */
    public FlowType getFlowType() {
        return (isInbound() ? FlowType.INBOUND : FlowType.OUTBOUND);
    }

    /**
     * Access the properties of this channel directly.
     *
     * @return Properties
     */
    public Properties getProperties() {
        return this.properties;
    }

    /**
     * Set the properties of the channel to the input map.
     *
     * @param newMap
     */
    public void setProperties(Properties newMap) {
        this.properties = newMap;
    }

    /**
     * Query the boolean property value of the input name.
     *
     * @param propertyName
     * @return boolean
     */
    public boolean getBooleanProperty(String propertyName) {
        boolean booleanValue = false;

        // Pull the key from the property map
        Object objectValue = this.properties.get(propertyName);
        if (objectValue != null) {
            if (objectValue instanceof Boolean) {
                booleanValue = ((Boolean) objectValue).booleanValue();
            } else if (objectValue instanceof String) {
                booleanValue = Boolean.parseBoolean((String) objectValue);
            }
        }
        return booleanValue;
    }

    /**
     * Query the properties for the input name, the value will be null if not
     * found or was not a String object.
     *
     * @param propertyName
     * @return String
     */
    public String getStringProperty(String propertyName) {
        Object value = this.properties.get(propertyName);
        if (null != value) {
            if (value instanceof String) {
                return (String) value;
            } else if (value instanceof Long) {
                return ((Long) value).toString();
            }
        }

        return null;
    }

    /**
     * Handle update to the running ssl channel data. Some data will be ignored
     * because it can't be updated without a restart.
     *
     * @param inputData
     */
    public void updateChannelData(ChannelData inputData) {
        // No support for changing properties now. This will come in a future release.
    }

    /**
     * Extract String value from property list and convert to boolean.
     *
     * @param key key to look up in the property map
     * @param defaultValue used if keynot found in map.
     * @param errors list of error string accumulating from reading invalid properties
     * @return value found for key in property map
     */
    private boolean getBooleanProperty(String key, String defaultValue, StringBuilder errors) {
        boolean booleanValue = false;
        String stringValue = null;
        boolean valueCorrect = false;

        // Pull the key from the property map
        Object objectValue = this.properties.get(key);
        if (objectValue != null) {
            if (objectValue instanceof Boolean) {
                booleanValue = ((Boolean) objectValue).booleanValue();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Property " + key + " set to " + booleanValue);
                }
                return booleanValue;
            } else if (objectValue instanceof String) {
                stringValue = (String) objectValue;
            }
        } else {
            // Key is not in map.
            if (defaultValue != null) {
                stringValue = defaultValue;
            } else {
                // No default provided. Error.
                errors.append(key);
                errors.append(':');
                errors.append(stringValue);
                errors.append('\n');
                return false;
            }
        }

        // If we get this far, we have a non null string value to work with. May be the default.
        // Verify the value.
        if (stringValue != null) {
            if (stringValue.equals("true")) {
                booleanValue = true;
                valueCorrect = true;
            } else if (stringValue.equals("false")) {
                booleanValue = false;
                valueCorrect = true;
            }
        }
        if (valueCorrect) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Property " + key + " set to " + booleanValue);
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Property " + key + " has invalid value " + stringValue);
            }
            errors.append(key);
            errors.append(':');
            errors.append(stringValue);
            errors.append('\n');
        }
        return booleanValue;
    }

    /**
     * Extract an integer property from the stored values. This might use a
     * default value if provided and the property was not found.
     *
     * @param key
     * @param defaultProvided
     * @param defaultValue
     * @param errors
     * @return int
     */
    private int getIntProperty(String key, boolean defaultProvided, int defaultValue, StringBuilder errors) {
        String value = getStringProperty(key);
        if (null != value) {
            try {
                int realValue = Integer.parseInt(value);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Property " + key + " set to " + realValue);
                }
                return realValue;
            } catch (NumberFormatException nfe) {
                // no FFDC required;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Property " + key + ", format error in [" + value + "]");
                }
            }
        }
        if (!defaultProvided) {
            // No default available. This is an error.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Property " + key + " not found.  Error being tallied.");
            }
            errors.append(key);
            errors.append(":null \n");
            return -1;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Property " + key + " using default " + defaultValue);
        }
        return defaultValue;
    }

}
