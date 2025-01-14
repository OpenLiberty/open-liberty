/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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
package com.ibm.ws.wssecurity.internal;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.wssecurity.cxf.interceptor.WSSecurityLibertyPluginInterceptor;
import com.ibm.ws.wssecurity.cxf.validator.UsernameTokenValidator;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

@Component(configurationPid = "com.ibm.ws.wssecurity.client.config",
           configurationPolicy = ConfigurationPolicy.OPTIONAL, //really?
           service = { WSSecurityClientConfiguration.class, ConfigurationListener.class },
           immediate = true,
           property = { "service.vendor=IBM" })
public class WSSecurityClientConfiguration implements ConfigurationListener {

    private static final TraceComponent tc = Tr.register(WSSecurityClientConfiguration.class, WSSecurityConstants.TR_GROUP, WSSecurityConstants.TR_RESOURCE_BUNDLE);
    protected volatile ConfigurationAdmin configAdmin;

    private volatile SecurityService securityService;

    static final String KEY_ID = "id";

    static final String CFG_KEY_USER = "user";
    static final String CFG_KEY_USER_NAME = "name";
    static final String CFG_KEY_PASSWORD = "password";
    static final String CFG_KEY_PASSWORD_VALUE = "value";
    static final String CFG_KEY_CALLBACK = "callback";

    static final String CFG_KEY_NAME = "name";
    static final String CFG_KEY_PROVIDER = "provider";
    static final String CFG_KEY_ENTRY = "entry";
    static final String CFG_KEY_ENTRY_KEY = "key";
    static final String CFG_KEY_ENTRY_VALUE = "value";

    static final String WSSEC = "ws-security";
    static final String CXF_USER_NAME = WSSEC + ".username";
    static final String CXF_USER_PASSWORD = WSSEC + ".password";
    static final String CXF_CBH = WSSEC + ".callback-handler";

    static final String[] SPECIAL_CFG_KEYS = { "component.name", "component.id", "config.source", "config.id", "id",
                                               "service.factoryPid", "service.vendor", "service.pid" };

    private volatile String cfgUser;
    private volatile SerializableProtectedString cfgPassword;
    private volatile String cfgCallback;

    private volatile Map<String, Object> defaultConfigMap = Collections.synchronizedMap(new HashMap<String, Object>());

    private volatile Map<String, Object> properties;
    private final Set<String> pids = new HashSet<String>();

    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected void setConfigurationAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    protected void unsetConfigurationAdmin(ConfigurationAdmin configAdmin) {
        if (this.configAdmin == configAdmin) {
            this.configAdmin = null;
        }
    }

    @Reference(policy = ReferencePolicy.DYNAMIC)
    protected void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    protected void unsetSecurityService(SecurityService securityService) {
        if (this.securityService == securityService) {
            this.securityService = null;
        }
    }

    @Activate
    protected void activate(Map<String, Object> properties) {
        this.properties = properties;
        setAndValidateProperties();
        WSSecurityLibertyPluginInterceptor.setClientBindingsConfiguration(defaultConfigMap);
        UsernameTokenValidator.setSecurityService(securityService);
    }

    @Modified
    protected void modify(Map<String, Object> newProperties) {
        this.properties = newProperties;
        internalModify();
    }

    /**
     *
     */
    private synchronized void internalModify() {
        cfgUser = null;
        cfgPassword = null;
        cfgCallback = null;
        defaultConfigMap.clear();
        setAndValidateProperties();
        WSSecurityLibertyPluginInterceptor.setClientBindingsConfiguration(defaultConfigMap);//dynamic update
    }

    @Deactivate
    protected void deactivate() {
        UsernameTokenValidator.setSecurityService(null);
        WSSecurityLibertyPluginInterceptor.setClientBindingsConfiguration(null);

        cfgUser = null;
        cfgPassword = null;
        cfgCallback = null;
        defaultConfigMap.clear();
    }

    /**
     */
    private void setAndValidateProperties() {

        pids.clear();
        String id = (String) properties.get(KEY_ID);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Default config id = ", id);
        }

        Set<Entry<String, Object>> entrySet = properties.entrySet();
        Iterator<Entry<String, Object>> entryIt = entrySet.iterator();

        while (entryIt.hasNext()) {
            Map.Entry<String, Object> entry = entryIt.next();
            String entry_key = entry.getKey();
            if ("signatureProperties".equals(entry_key)) {
                try {
                    String sigPropsConfigPid = (String) entry.getValue();
                    pids.add(sigPropsConfigPid);
                    Map<String, Object> signaturePropertyMap = convertToMap(sigPropsConfigPid);
                    if (signaturePropertyMap != null && !signaturePropertyMap.isEmpty()) {
                        for (String key : SPECIAL_CFG_KEYS) {
                            signaturePropertyMap.remove(key);
                        }
                        if (newConfigSpecified(signaturePropertyMap)) {
                            signaturePropertyMap.remove(WSSecurityConstants.WSS4J_CRYPTO_PROVIDER);
                            signaturePropertyMap.putIfAbsent(WSSecurityConstants.WSS4J_2_CRYPTO_PROVIDER, WSSecurityConstants.WSS4J_2_CRYPTO_PROVIDER_NAME);
                            defaultConfigMap.put(WSSecurityConstants.SEC_SIG_PROPS, signaturePropertyMap); //v3
                        } else {
                            defaultConfigMap.put(WSSecurityConstants.CXF_SIG_PROPS, signaturePropertyMap); //v3 - backward compatibility
                        }

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Object sigProp = signaturePropertyMap.get(WSSecurityConstants.WSS4J_2_KS_TYPE) != null ? signaturePropertyMap.get(WSSecurityConstants.WSS4J_2_KS_TYPE) : signaturePropertyMap.get(WSSecurityConstants.WSS4J_KS_TYPE);
                            Tr.debug(tc, "signature configuration type = ", sigProp);
                            sigProp = signaturePropertyMap.get(WSSecurityConstants.WSS4J_2_KS_ALIAS) != null ? signaturePropertyMap.get(WSSecurityConstants.WSS4J_2_KS_ALIAS) : signaturePropertyMap.get(WSSecurityConstants.WSS4J_KS_ALIAS);
                            Tr.debug(tc, "signature configuration alias = ", sigProp);
                            sigProp = signaturePropertyMap.get(WSSecurityConstants.WSS4J_2_KS_FILE) != null ? signaturePropertyMap.get(WSSecurityConstants.WSS4J_2_KS_FILE) : signaturePropertyMap.get(WSSecurityConstants.WSS4J_KS_FILE);
                            Tr.debug(tc, "signature configuration ks file = ", sigProp);
                            sigProp = signaturePropertyMap.get(WSSecurityConstants.WSS4J_2_KS_PASSWORD) != null ? signaturePropertyMap.get(WSSecurityConstants.WSS4J_2_KS_PASSWORD) : signaturePropertyMap.get(WSSecurityConstants.WSS4J_KS_PASSWORD);
                            Tr.debug(tc, "signature configuration password = ", sigProp);
                            sigProp = signaturePropertyMap.get(WSSecurityConstants.WSS4J_2_CRYPTO_PROVIDER) != null ? signaturePropertyMap.get(WSSecurityConstants.WSS4J_2_CRYPTO_PROVIDER) : signaturePropertyMap.get(WSSecurityConstants.WSS4J_CRYPTO_PROVIDER);
                            Tr.debug(tc, "signature configuration provider = ", sigProp);
                        }

                        // Log a message if the signature algorithm is not secure
                        String algorithm = (String) signaturePropertyMap.get("signatureAlgorithm");
                        if (algorithm == null || algorithm.isEmpty()) {
                            algorithm = WSSecurityConstants.WSSEC_DEFAULT_SIGNATURE_ALGORITHM;
                        }
                        if (CryptoUtils.isAlgorithmInsecure(algorithm)) {
                            CryptoUtils.logInsecureAlgorithm("wsSecurityClient.signatureProperties.signatureAlgorithm", algorithm);
                        }
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Empty ws-security client signature configuration ", sigPropsConfigPid);
                        }
                    }
                } catch (IOException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Invalid ws-security client signature configuration: " + e);
                    }
                }

            } else if ("encryptionProperties".equals(entry_key)) {
                try {
                    String encPropsConfigPid = (String) entry.getValue();//String encPropsConfig = (String) properties.get(entry_key);
                    pids.add(encPropsConfigPid);
                    Map<String, Object> encryptionPropertyMap = convertToMap(encPropsConfigPid);
                    if (encryptionPropertyMap != null && !encryptionPropertyMap.isEmpty()) {
                        for (String key : SPECIAL_CFG_KEYS) {
                            encryptionPropertyMap.remove(key);
                        }
                        if (newConfigSpecified(encryptionPropertyMap)) {
                            encryptionPropertyMap.remove(WSSecurityConstants.WSS4J_CRYPTO_PROVIDER);
                            encryptionPropertyMap.putIfAbsent(WSSecurityConstants.WSS4J_2_CRYPTO_PROVIDER, WSSecurityConstants.WSS4J_2_CRYPTO_PROVIDER_NAME);
                            defaultConfigMap.put(WSSecurityConstants.SEC_ENC_PROPS, encryptionPropertyMap); //v3
                        } else {
                            defaultConfigMap.put(WSSecurityConstants.CXF_ENC_PROPS, encryptionPropertyMap); //v3 - backward compatibility
                        }

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Object encProp = encryptionPropertyMap.get(WSSecurityConstants.WSS4J_2_KS_TYPE) != null ? encryptionPropertyMap.get(WSSecurityConstants.WSS4J_2_KS_TYPE) : encryptionPropertyMap.get(WSSecurityConstants.WSS4J_KS_TYPE);
                            Tr.debug(tc, "encryption configuration type = ", encProp);
                            encProp = encryptionPropertyMap.get(WSSecurityConstants.WSS4J_2_KS_ALIAS) != null ? encryptionPropertyMap.get(WSSecurityConstants.WSS4J_2_KS_ALIAS) : encryptionPropertyMap.get(WSSecurityConstants.WSS4J_KS_ALIAS);
                            Tr.debug(tc, "encryption configuration alias = ", encProp);
                            encProp = encryptionPropertyMap.get(WSSecurityConstants.WSS4J_2_KS_FILE) != null ? encryptionPropertyMap.get(WSSecurityConstants.WSS4J_2_KS_FILE) : encryptionPropertyMap.get(WSSecurityConstants.WSS4J_KS_FILE);
                            Tr.debug(tc, "encryption configuration ks file = ", encProp);
                            encProp = encryptionPropertyMap.get(WSSecurityConstants.WSS4J_2_KS_PASSWORD) != null ? encryptionPropertyMap.get(WSSecurityConstants.WSS4J_2_KS_PASSWORD) : encryptionPropertyMap.get(WSSecurityConstants.WSS4J_KS_PASSWORD);
                            Tr.debug(tc, "encryption configuration password = ", encProp);
                            encProp = encryptionPropertyMap.get(WSSecurityConstants.WSS4J_2_CRYPTO_PROVIDER) != null ? encryptionPropertyMap.get(WSSecurityConstants.WSS4J_2_CRYPTO_PROVIDER) : encryptionPropertyMap.get(WSSecurityConstants.WSS4J_CRYPTO_PROVIDER);
                            Tr.debug(tc, "encryption configuration provider = ", encProp);
                        }
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Empty ws-security client encryption configuration ", encPropsConfigPid);
                        }
                    }
                } catch (IOException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Invalid ws-security client encryption configuration: " + e);
                    }
                }
            } else if (entry_key != null) {
                //skip special config keys
                if (entry_key.startsWith(".")
                    || entry_key.startsWith("config.")
                    || entry_key.startsWith("service.")
                    || entry_key.equals("id")
                    || entry_key.startsWith("osgi.ds.")) {
                    continue;
                }
                Object entry_value = entry.getValue();//(String) properties.get(entry_key);
                if (entry_value != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "ws-security client configuration entry key = ", entry_key);
                        Tr.debug(tc, "ws-security client configuration entry value = ", entry_value);
                    }
                    defaultConfigMap.put(entry_key, entry_value);
                    if (CXF_USER_NAME.equals(entry_key)) {
                        this.cfgUser = (String) entry_value;
                    } else if (CXF_USER_PASSWORD.equals(entry_key)) {
                        this.cfgPassword = (SerializableProtectedString) entry_value;
                    } else if (CXF_CBH.equals(entry_key)) {
                        this.cfgCallback = (String) entry_value;
                    }
                }

            }
        }
        if (defaultConfigMap.isEmpty()) {
            Tr.info(tc, "WSSECURITY_NO_CONFIG_DEFINED");
        }
    }

    /**
     * @param signature or encryption propertyMap
     * @return
     */
    private boolean newConfigSpecified(Map<String, Object> propertyMap) {
        Set<String> keys = propertyMap.keySet();
        for (String key : keys) {
            if (key.contains(WSSecurityConstants.WSS4J_2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return user name for this configuration.
     */
    String getUser() {
        return cfgUser;
    }

    /**
     * @return password for this configuration.
     */
    SerializableProtectedString getPassword() {
        return cfgPassword;
    }

    /**
     * @return callback class name for this configuration.
     */
    String getCallback() {
        return cfgCallback;
    }

    Map<String, Object> getDefaultConfiguration() {
        return defaultConfigMap;
    }

    private Map<String, Object> convertToMap(String pid) throws IOException {
        Map<String, Object> map = new HashMap<String, Object>();
        try {
            Configuration[] configs = configAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=" + pid + ")");
            if (configs != null && configs.length != 0) {
                Configuration config = configAdmin.getConfiguration(pid);
                Dictionary<String, Object> dictionary = config.getProperties();

                if (dictionary != null) {
                    // convert Dictionary to Map
                    Enumeration<String> keys = dictionary.keys();

                    while (keys.hasMoreElements()) {
                        String strKey = keys.nextElement();
                        map.put(strKey, dictionary.get(strKey));
                    }
                    return map;
                }
            }
        } catch (InvalidSyntaxException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Syntax error accesssing configuration for pid " + pid + ": " + e.getMessage());
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "No configuration for pid " + pid);
        }
        return map;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.cm.ConfigurationListener#configurationEvent(org.osgi.service.cm.ConfigurationEvent)
     */
    @Override
    public synchronized void configurationEvent(ConfigurationEvent event) {
        if (event.getType() == ConfigurationEvent.CM_UPDATED && pids.contains(event.getPid())) {
            internalModify();
        }
    }
}
