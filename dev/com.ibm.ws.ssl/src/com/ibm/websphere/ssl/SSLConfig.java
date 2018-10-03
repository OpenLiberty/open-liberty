/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.websphere.ssl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ssl.JSSEProviderFactory;
import com.ibm.ws.ssl.config.SSLConfigManager;
import com.ibm.ws.ssl.config.WSKeyStore;

/**
 * SSLConfig is responsible for maintaining all of the properties for an
 * individual SSL configuration that can be used to create an SSLContext.
 * <p>
 * This class represents a single SSLConfig in the runtime. It uses an
 * underlying Properties object to hold the data.
 * </p>
 *
 * @author IBM Corporation
 * @version WAS 7.0
 * @since WAS 7.0
 */
public class SSLConfig extends Properties {
    private static final long serialVersionUID = 5592062346302545106L;

    protected static final TraceComponent tc = Tr.register(SSLConfig.class,
                                                           "SSL", "com.ibm.ws.ssl.resources.ssl");

    /**
     * Constructor.
     */
    public SSLConfig() {
        super();
        initializeDefaults();
    }

    /**
     * Constructor.
     *
     * @param props
     */
    public SSLConfig(Properties props) {
        super();

        // this is needed to avoid a recursive stack overflow
        if (props != null) {
            super.putAll(props);
        }

        initializeDefaults();
    }

    /**
     * Constructor.
     *
     * @param configURL
     */
    public SSLConfig(String configURL) {
        super();
        loadPropertiesFile(configURL, false);
        initializeDefaults();
    }

    /***
     * This method initializes properties that can have smart defaults
     * when they are not yet specified. If specified, the specified values
     * overwrite the default values.
     ***/
    private void initializeDefaults() {
        String keyStoreType = getProperty(Constants.SSLPROP_KEY_STORE_TYPE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "keyStoreType: " + keyStoreType);

        if (keyStoreType == null && getProperty(Constants.SYSTEM_SSLPROP_KEY_STORE_TYPE) == null) {
            setProperty(Constants.SSLPROP_KEY_STORE_FILE_BASED, Constants.TRUE);
            setProperty(Constants.SSLPROP_KEY_STORE_TYPE, Constants.KEYSTORE_TYPE_JKS);
            keyStoreType = Constants.KEYSTORE_TYPE_JKS;
        } else {
            if (keyStoreType != null &&
                !keyStoreType.equals(Constants.KEYSTORE_TYPE_JKS) &&
                !keyStoreType.equals(Constants.KEYSTORE_TYPE_JCEKS) &&
                !keyStoreType.equals(Constants.KEYSTORE_TYPE_PKCS12)) {
                setProperty(Constants.SSLPROP_KEY_STORE_FILE_BASED, Constants.FALSE);
            } else {
                setProperty(Constants.SSLPROP_KEY_STORE_FILE_BASED, Constants.TRUE);
            }
        }

        String trustStoreType = getProperty(Constants.SSLPROP_TRUST_STORE_TYPE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "trustStoreType: " + trustStoreType);

        if (trustStoreType == null && getProperty(Constants.SYSTEM_SSLPROP_TRUST_STORE_TYPE) == null) {
            setProperty(Constants.SSLPROP_TRUST_STORE_FILE_BASED, Constants.TRUE);
            setProperty(Constants.SSLPROP_TRUST_STORE_TYPE, Constants.KEYSTORE_TYPE_JKS);
            trustStoreType = Constants.KEYSTORE_TYPE_JKS;
        } else {
            if (trustStoreType != null &&
                !trustStoreType.equals(Constants.KEYSTORE_TYPE_JKS) &&
                !trustStoreType.equals(Constants.KEYSTORE_TYPE_JCEKS) &&
                !trustStoreType.equals(Constants.KEYSTORE_TYPE_PKCS12)) {
                setProperty(Constants.SSLPROP_TRUST_STORE_FILE_BASED, Constants.FALSE);
            } else {
                setProperty(Constants.SSLPROP_TRUST_STORE_FILE_BASED, Constants.TRUE);
            }
        }

        JSSEProvider defaultProvider = JSSEProviderFactory.getInstance();

        if (getProperty(Constants.SSLPROP_KEY_MANAGER) == null)
            setProperty(Constants.SSLPROP_KEY_MANAGER, JSSEProviderFactory.getKeyManagerFactoryAlgorithm());

        if (getProperty(Constants.SSLPROP_KEY_STORE_PROVIDER) == null
            && null != defaultProvider.getKeyStoreProvider())
            setProperty(Constants.SSLPROP_KEY_STORE_PROVIDER, defaultProvider.getKeyStoreProvider());

        if (getProperty(Constants.SSLPROP_PROTOCOL) == null)
            setProperty(Constants.SSLPROP_PROTOCOL, defaultProvider.getDefaultProtocol());

        if (getProperty(Constants.SSLPROP_CLIENT_AUTHENTICATION) == null)
            setProperty(Constants.SSLPROP_CLIENT_AUTHENTICATION, Constants.FALSE);

        if (getProperty(Constants.SSLPROP_CONTEXT_PROVIDER) == null)
            setProperty(Constants.SSLPROP_CONTEXT_PROVIDER, defaultProvider.getContextProvider());

        if (getProperty(Constants.SSLPROP_SECURITY_LEVEL) == null)
            setProperty(Constants.SSLPROP_SECURITY_LEVEL, Constants.SECURITY_LEVEL_HIGH);

        if (getProperty(Constants.SSLPROP_TRUST_MANAGER) == null)
            setProperty(Constants.SSLPROP_TRUST_MANAGER, JSSEProviderFactory.getTrustManagerFactoryAlgorithm());

        if (getProperty(Constants.SSLPROP_TRUST_STORE_PROVIDER) == null
            && null != defaultProvider.getKeyStoreProvider())
            setProperty(Constants.SSLPROP_TRUST_STORE_PROVIDER, defaultProvider.getKeyStoreProvider());

        if (getProperty(Constants.SSLPROP_VALIDATION_ENABLED) == null)
            setProperty(Constants.SSLPROP_VALIDATION_ENABLED, Constants.FALSE);

        if (getProperty(Constants.SSLPROP_TOKEN_ENABLED) == null)
            setProperty(Constants.SSLPROP_TOKEN_ENABLED, Constants.FALSE);

        String keyStore = getProperty(Constants.SSLPROP_KEY_STORE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "keyStore: " + keyStore);
        }
        String keyStoreName = getProperty(Constants.SSLPROP_KEY_STORE_NAME);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "keyStoreName: " + keyStoreName);
        }
        String keyStorePassword = getProperty(Constants.SSLPROP_KEY_STORE_PASSWORD);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "keyStorePassword: " + SSLConfigManager.mask(keyStorePassword));

        String trustStore = getProperty(Constants.SSLPROP_TRUST_STORE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "trustStore: " + trustStore);
        }
        String trustStoreName = getProperty(Constants.SSLPROP_TRUST_STORE_NAME);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "trustStoreName: " + trustStoreName);
        }
        String trustStorePassword = getProperty(Constants.SSLPROP_TRUST_STORE_PASSWORD);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "trustStorePassword: " + SSLConfigManager.mask(trustStorePassword));

        // When only a trust store is specified, use it as the key store as well.
        if (keyStore == null && trustStore != null && trustStorePassword != null && trustStoreType != null) {
            setProperty(Constants.SSLPROP_KEY_STORE, trustStore);
            setProperty(Constants.SSLPROP_KEY_STORE_NAME, trustStoreName);
            setProperty(Constants.SSLPROP_KEY_STORE_PASSWORD, trustStorePassword);
            setProperty(Constants.SSLPROP_KEY_STORE_TYPE, trustStoreType);
        }
        // When only a key store is specified, use it as the trust store as well.
        else if (trustStore == null && keyStore != null && keyStorePassword != null && keyStoreType != null) {
            setProperty(Constants.SSLPROP_TRUST_STORE, keyStore);
            setProperty(Constants.SSLPROP_TRUST_STORE_NAME, keyStoreName);
            setProperty(Constants.SSLPROP_TRUST_STORE_PASSWORD, keyStorePassword);
            setProperty(Constants.SSLPROP_TRUST_STORE_TYPE, keyStoreType);
        }
    }

    /***
     * This method attempts to validate a set of SSL configuration properties
     * by trying to create an SSLContext from them. An error message will be printed
     * whenever validation is enabled and it fails.
     *
     * @throws Exception
     ***/
    public void validateSSLConfig() throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "validateSSLConfig");

        try {
            if (Boolean.parseBoolean(getProperty(Constants.SSLPROP_VALIDATION_ENABLED))) {
                JSSEProviderFactory.getInstance().getSSLContext(null, this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    Tr.exit(tc, "validateSSLConfig -> true");
                return;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "validateSSLConfig not enabled.");
            return;
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "validateSSLConfig -> false");
            throw e;
        }
    }

    /**
     * Check whether the input key exists as a non-null and non-empty
     * property. The existing containsKey() only checks for non-null.
     *
     * @param key
     * @return boolean
     */
    private boolean propertyExists(String key) {
        String value = getProperty(key);
        return (null != value && 0 < value.length());
    }

    /***
     * This ensures there is at least a keystore, truststore or crypto configured.
     *
     * @return boolean
     ***/
    public boolean requiredPropertiesArePresent() {
        // trust store specified OR key store specified OR both -> valid
        boolean valid = (
        // explicit keystore and truststore exists
        (propertyExists(Constants.SSLPROP_KEY_STORE)
         && propertyExists(Constants.SSLPROP_TRUST_STORE)));
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "requiredPropertiesArePresent -> " + valid);
        return valid;
    }

    /***
     * This method prints the SSL properties to trace without printing passwords.
     * Any property that contains "password" in any case will have the value
     * masked.
     *
     * @return String
     * @see Object#toString()
     ***/
    @Override
    public String toString() {
        Enumeration<?> e = propertyNames();
        StringBuilder buf = new StringBuilder(256);
        buf.append("SSLConfig.toString() {\n");

        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            String value = getProperty(name);

            if (name.toLowerCase().indexOf("password") != -1) {
                buf.append(name);
                buf.append('=');
                buf.append(SSLConfigManager.mask(value));
                buf.append('\n');
            } else {
                buf.append(name);
                buf.append('=');
                buf.append(value);
                buf.append('\n');
            }
        }
        buf.append('}');
        return buf.toString();
    }

    /**
     * Load ConfigURL from a url string that names a properties file. This
     * method does not check that values are in valid range,
     *
     * @param propertiesURL - the properties file to load the SSL properties
     * @param multiConfigURL
     * @return SSLConfig[]
     */

    public SSLConfig[] loadPropertiesFile(final String propertiesURL, final boolean multiConfigURL) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "loadPropertiesFile", new Object[] { propertiesURL, Boolean.valueOf(multiConfigURL) });
        // sslConfigs is used when multiConfigURL=true;
        SSLConfig[] sslConfigs = null;

        if (propertiesURL == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "loadPropertiesFile (null props file)");
            return null;
        }
        try {
            sslConfigs = AccessController.doPrivileged(
                                                       new PrivilegedExceptionAction<SSLConfig[]>() {
                                                           @Override
                                                           public SSLConfig[] run() throws Exception {

                                                               InputStream istream = null;
                                                               BufferedReader in = null;

                                                               try {
                                                                   URL url = new URL(propertiesURL);
                                                                   istream = url.openStream();

                                                                   if (!multiConfigURL) {
                                                                       load(istream);
                                                                   } else {
                                                                       String s = null;
                                                                       in = new BufferedReader(new InputStreamReader(istream));
                                                                       List<SSLConfig> sslConfigList = new ArrayList<SSLConfig>();
                                                                       SSLConfig currentSSLConfig = new SSLConfig();
                                                                       currentSSLConfig.clear(); // clear the default properties

                                                                       while ((s = in.readLine()) != null) {
                                                                           if (!(s.trim().startsWith("#") || s.trim().length() <= 0)) {
                                                                               if (s.trim().startsWith("com.ibm.ssl.alias")) {
                                                                                   if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                                                                       Tr.debug(tc, "Saving SSL configuration...");
                                                                                   sslConfigList.add(currentSSLConfig);

                                                                                   // now start a new one.
                                                                                   currentSSLConfig = new SSLConfig();

                                                                                   int index = s.indexOf('=');
                                                                                   String name = s.substring(0, index);
                                                                                   String value = s.substring(index + 1);
                                                                                   if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                                                                       Tr.debug(tc, "Parsing SSL configuration with alias: " + value);
                                                                                   currentSSLConfig.setProperty(name, value, true);
                                                                               } else if (s.trim().indexOf('=') != -1) {
                                                                                   int index = s.indexOf('=');
                                                                                   String name = s.substring(0, index);
                                                                                   String value = null;
                                                                                   if (name != null)
                                                                                       value = System.getProperty(name);
                                                                                   if (value == null)
                                                                                       value = s.substring(index + 1);
                                                                                   if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                                                                       Tr.debug(tc, "Parsing SSL property: " + name + " = " + value);
                                                                                   currentSSLConfig.setProperty(name, value, true);
                                                                               }
                                                                           }
                                                                       }

                                                                       if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                                                           Tr.debug(tc, "Saving SSL configuration...");
                                                                       sslConfigList.add(currentSSLConfig);
                                                                       return sslConfigList.toArray(new SSLConfig[sslConfigList.size()]);
                                                                   }
                                                               } finally {
                                                                   if (null != in) {
                                                                       in.close();
                                                                   }
                                                                   if (istream != null)
                                                                       istream.close();
                                                               }
                                                               return null;
                                                           }
                                                       });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "loadPropertiesFile");
            return sslConfigs;
        } catch (PrivilegedActionException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "loadPropertiesFile exception; " + e.getException());
            FFDCFilter.processException(e.getException(), getClass().getName(), "loadPropertiesFile", this);
            // TODO shouldn't this be throwing the error?
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "loadPropertiesFile");
        return null;
    }

    /***
     * This method attempts to decode the values of any property that contains
     * "password" in any case. If the value is not decoded, it simply leaves
     * it alone, otherwise the encoded value is decoded.
     ***/
    public void decodePasswords() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "decodePasswords");
        Enumeration<?> e = propertyNames();

        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            if (name.toLowerCase().indexOf("password") != -1) {
                setProperty(name, WSKeyStore.decodePassword(getProperty(name)));
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "decodePasswords");
    }

    /***
     * This method tries to normalize the ConfigURL value in an attempt to
     * correct any URL parsing errors.
     *
     * @param propertiesURL
     * @return String
     ***/
    public static String validateURL(final String propertiesURL) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "Existing propertiesURL: " + propertiesURL);
        int end = 0;

        // get rid of all of the / or \ after file: and replace with just /
        char c;
        for (int i = propertiesURL.indexOf(':', 0) + 1; i < propertiesURL.length(); i++) {
            c = propertiesURL.charAt(i);
            if (c != '/' && c != '\\') {
                end = i;
                break;
            }
        }

        String rc = "file:/" + propertiesURL.substring(end);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "New propertiesURL: " + rc);
        return rc;
    }

    /***
     * This method returns the com.ibm.ssl.dynamicSelectionInfo property value.
     *
     * @return String
     ***/
    public String getDynamicSelectionProperty() {
        return getProperty(Constants.SSLPROP_DYNAMIC_SELECTION_INFO);
    }

    /***
     * This method determines if the current instance of SSLConfig equals the
     * one passed into the equals method. This is called to verify if an
     * SSLConfig changed when the properties.
     *
     * @param config
     * @return boolean
     ***/
    @Override
    public boolean equals(Object config) {
        if (config instanceof SSLConfig) {
            SSLConfig sslconfig = (SSLConfig) config;
            if (size() != sslconfig.size()) {
                return false;
            }

            Enumeration<?> e = propertyNames();

            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();
                String value = getProperty(name);
                String otherValue = sslconfig.getProperty(name);

                if (value != null && (otherValue == null || !value.equals(otherValue))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Set the given name/value pair as a property. The process flag determines
     * whether to check for and handle unicode characters in the provided
     * value.
     *
     * @param name
     * @param value
     * @param processEscapeSequences
     * @return Object - value stored
     */
    public Object setProperty(String name, String value, boolean processEscapeSequences) {
        String newVal = value;

        if (newVal != null && processEscapeSequences) {
            // Check for Unicode characters in the value
            int idx = newVal.indexOf("\\u");
            while (idx != -1 && idx < newVal.length()) {
                // Get beginning and ending index of the unicode character and make sure it is not greater than the length of the string
                int idx1 = (idx + 2 < newVal.length()) ? idx + 2 : newVal.length();
                int idx2 = (idx + 6 < newVal.length()) ? idx + 6 : newVal.length();

                String digits = newVal.substring(idx1, idx2);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Potential unicode character detected: '\\u" + digits + "'");

                // If the \\u can't have four digits following (i.e. \\uxxxx), stop right here it is not a Unicode character
                if (digits != null && digits.length() == 4) {
                    // Now get the four Unicode digits and their integer value
                    try {
                        char c = (char) Integer.parseInt(digits, 16);
                        newVal = newVal.substring(0, idx) + c + newVal.substring(idx + 6, newVal.length());
                    } catch (Exception e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "Error in unicode format", e);
                        // If an exception occurs, throw a java.lang.IllegalArgumentException
                        // which is consistent with Properties file processing
                        throw new IllegalArgumentException("Malformed \\uxxxx encoding. digits=" + digits);
                    }
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "Error in unicode format");
                    // Throw the following exception to be consistent with Propeties file processing
                    throw new IllegalArgumentException("Malformed \\uxxxx encoding. digits=" + digits);
                }
                idx = newVal.indexOf("\\u", idx + 1);
            }

            // Check for Escaped '=' Character
            idx = newVal.indexOf("\\=");
            while (idx != -1 && idx < newVal.length()) {
                newVal = newVal.substring(0, idx) + "=" + newVal.substring(idx + 2, newVal.length());
                idx = newVal.indexOf("\\=");
            }
            // Check for Escaped ":" Character
            idx = newVal.indexOf("\\:");
            while (idx != -1 && idx < newVal.length()) {
                newVal = newVal.substring(0, idx) + ":" + newVal.substring(idx + 2, newVal.length());
                idx = newVal.indexOf("\\:");
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() && value != null && !value.equals(newVal))
            Tr.debug(tc, "Property value changed to: " + newVal);
        return super.setProperty(name, newVal);
    }
}
