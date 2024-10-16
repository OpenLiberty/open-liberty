/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.crypto.CryptoUtils;
import com.ibm.ws.security.SecurityService;
import com.ibm.ws.ssl.KeyStoreService;
import com.ibm.ws.wssecurity.cxf.interceptor.WSSecurityLibertyPluginInterceptor;
import com.ibm.ws.wssecurity.cxf.validator.UsernameTokenValidator;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.ssl.SSLSupport;

public class WSSecurityConfiguration implements ConfigurationListener {

    private static final TraceComponent tc = Tr.register(WSSecurityConfiguration.class,
                                                         WSSecurityConstants.TR_GROUP,
                                                         WSSecurityConstants.TR_RESOURCE_BUNDLE);
    protected volatile ConfigurationAdmin configAdmin;

    private volatile SecurityService securityService;
    static final String KEY_ID = "id";

    static final String CFG_KEY_USER = "user";

    static final String CFG_KEY_PASSWORD = "password";
    static final String CFG_KEY_PASSWORD_VALUE = "value";
    static final String CFG_KEY_CALLBACK = "callback";

    static final String CFG_KEY_NAME = "name";
    static final String CFG_KEY_PROVIDER = "provider";
    static final String CFG_KEY_ENTRY = "entry";
    static final String CFG_KEY_ENTRY_KEY = "key";
    static final String CFG_KEY_ENTRY_VALUE = "value";

    //protected static final String unknown_caller_token_name = "Caller token name specified is not valid.";

    static final String[] SPECIAL_CFG_KEYS = { "component.name", "component.id", "config.source", "config.id", "id", "service.vendor",
                                               "service.factoryPid", "service.pid" };

    public static final String KEY_KEYSTORE_SERVICE = "keyStoreService";
    private final AtomicServiceReference<KeyStoreService> keyStoreServiceRef = new AtomicServiceReference<KeyStoreService>(KEY_KEYSTORE_SERVICE);
    public static final String KEY_SSL_SUPPORT = "sslSupport";
    protected final AtomicServiceReference<SSLSupport> sslSupportRef = new AtomicServiceReference<SSLSupport>(KEY_SSL_SUPPORT);

    static final String KEY_samlToken = "samlToken";
    static final String KEY_wantAssertionsSigned = WSSecurityConstants.KEY_wantAssertionsSigned;
    static final String KEY_clockSkew = WSSecurityConstants.KEY_clockSkew;
    static final String KEY_requiredSubjectConfirmationMethod = WSSecurityConstants.KEY_requiredSubjectConfirmationMethod;
    static final String KEY_timeToLive = WSSecurityConstants.KEY_timeToLive;
    static final String KEY_audienceRestrictions = WSSecurityConstants.KEY_audienceRestrictions;
    //static final String KEY_trustEngine_x509cert = "x509Certificate";
    //static final String KEY_trustEngine_crl = "crl";
    //static final String KEY_trustEngine_trustAnchor = "trustAnchor";
    //static final String KEY_trustEngine_path = "path";
    //static final String KEY_trustedIssuers = "trustedIssuers";
    static Map<String, String> subjectConfirmationMethods = new HashMap<String, String>();
    static {
        subjectConfirmationMethods.put("bearer", "urn:oasis:names:tc:SAML:2.0:cm:bearer");
        subjectConfirmationMethods.put("sender-vouches", "urn:oasis:names:tc:SAML:2.0:cm:sender-vouches");
        subjectConfirmationMethods.put("holder-of-key", "urn:oasis:names:tc:SAML:2.0:cm:holder-of-key");
    }

    private volatile String cfgCallback;

    private volatile Map<String, Object> defaultConfigMap = Collections.synchronizedMap(new HashMap<String, Object>());

    private volatile Map<String, Object> properties;
    private final Set<String> pids = new HashSet<String>();

    // Collections.unmodifiableMap(original map) on both maps
    private volatile Map<String, Object> samlTokenConfigMap = null;
    private volatile Map<String, Object> defaultSamlTokenConfigMap = null;

    //////Trust Engine Metadata
    ////private List<String> samlX509List = Collections.synchronizedList(new ArrayList<String>());
    ////private List<String> samlCrlList = Collections.synchronizedList(new ArrayList<String>());
    ////String trustAnchorName = null;
    //private final boolean isSamlTrustEngineEnabled = false;
    //String[] trustedIssuers = null;

    protected void setConfigurationAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    protected void unsetConfigurationAdmin(ConfigurationAdmin configAdmin) {
        if (this.configAdmin == configAdmin) {
            this.configAdmin = null;
        }
    }

    protected void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    protected void unsetSecurityService(SecurityService securityService) {
        if (this.securityService == securityService) {
            this.securityService = null;
        }
    }

    protected void setKeyStoreService(ServiceReference<KeyStoreService> ref) {
        keyStoreServiceRef.setReference(ref);
    }

    protected void unsetKeyStoreService(ServiceReference<KeyStoreService> ref) {
        keyStoreServiceRef.unsetReference(ref);
    }

    protected void setSslSupport(ServiceReference<SSLSupport> ref) {
        sslSupportRef.setReference(ref);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "setSslSupport service.pid:" + ref.getProperty("service.pid"));
        }
    }

    protected void updatedSslSupport(ServiceReference<SSLSupport> ref) {
        sslSupportRef.setReference(ref);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "updatedtSslSupport service.pid:" + ref.getProperty("service.pid"));
        }
    }

    protected void unsetSslSupport(ServiceReference<SSLSupport> ref) {
        sslSupportRef.unsetReference(ref);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "unsetSslSupport service.pid:" + ref.getProperty("service.pid"));
        }
    }

    protected void activate(ComponentContext cc, Map<String, Object> properties) {
        //System.out.println("In the activate!!!" + properties);
        keyStoreServiceRef.activate(cc);
        sslSupportRef.activate(cc);
        this.properties = properties;
        internalModify();
        UsernameTokenValidator.setSecurityService(securityService);
    }

    protected void modified(ComponentContext cc, Map<String, Object> newProperties) {
        this.properties = newProperties;
        internalModify();
    }

    protected void deactivate(ComponentContext cc) {
        keyStoreServiceRef.deactivate(cc);
        sslSupportRef.deactivate(cc);
        UsernameTokenValidator.setSecurityService(null);
        WSSecurityLibertyPluginInterceptor.setBindingsConfiguration(null);
        WSSecurityLibertyPluginInterceptor.setSamlTokenConfiguration(null);
        // the WSSecurity is still using the properties from activate/modified
        cfgCallback = null;
        defaultConfigMap.clear();
        // reset the samlTokenConfigMap to default
        if (defaultSamlTokenConfigMap == null) {
            defaultSamlTokenConfigMap = processDefaultSamlToken();
        }
        samlTokenConfigMap = defaultSamlTokenConfigMap;
    }

    /**
     *
     */
    private synchronized void internalModify() {
        cfgCallback = null;
        defaultConfigMap.clear();
        setAndValidateProperties();
        WSSecurityLibertyPluginInterceptor.setBindingsConfiguration(defaultConfigMap);//dynamic update
        // This need  to be in the bottom of this method,
        // because it may get Exception during searching the samlTrustAnchors, samlX509Cert...
        try {
            processSamlToken();
        } catch (Exception e) {
            // TODO Error handling
            // No exception until we process the trustEngine, such as: x509 certificates
        }
        WSSecurityLibertyPluginInterceptor.setSamlTokenConfiguration(samlTokenConfigMap);
    }

    /**
     *
     */
    void processSamlToken() {
        // handle the samlToken configuration
        String samlToken = (String) properties.get(KEY_samlToken);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "samlToken pid:", samlToken);
        }
        if (samlToken == null || samlToken.isEmpty()) {
            if (defaultSamlTokenConfigMap == null) {
                defaultSamlTokenConfigMap = processDefaultSamlToken();
            }
            this.samlTokenConfigMap = defaultSamlTokenConfigMap;
            return;
        }
        try {
            Map<String, Object> samlTokenConfigMap = processSamlToken(samlToken);
            this.samlTokenConfigMap = Collections.unmodifiableMap(samlTokenConfigMap);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to initialize saml token configuration data: ", samlToken, e);
            }
        }
    }

    /**
     * @return
     */
    Map<String, Object> processDefaultSamlToken() {
        Map<String, Object> samlTokenConfigMap = new HashMap<String, Object>();

        samlTokenConfigMap.put(KEY_wantAssertionsSigned, true); // Boolean
        samlTokenConfigMap.put(KEY_clockSkew, 300L); // 5 minutes
        String shortMethod = "bearer";
        samlTokenConfigMap.put(KEY_requiredSubjectConfirmationMethod, subjectConfirmationMethods.get(shortMethod)); // String
        samlTokenConfigMap.put(KEY_timeToLive, 1800L); // 30minutes
        samlTokenConfigMap.put(KEY_audienceRestrictions, null);
        return Collections.unmodifiableMap(samlTokenConfigMap);
    }

    /**
     * @param string
     * @throws Exception
     */
    Map<String, Object> processSamlToken(String samlToken) throws Exception {
        Map<String, Object> samlTokenConfigMap = new HashMap<String, Object>();
        Configuration config = null;
        try {
            config = configAdmin.getConfiguration(samlToken);
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Invalid saml websso trust engine configuration", samlToken);
            }
            return samlTokenConfigMap;
        }
        Dictionary<String, Object> samlTokenProps = config.getProperties();

        samlTokenConfigMap.put(KEY_wantAssertionsSigned, samlTokenProps.get(KEY_wantAssertionsSigned)); // Boolean
        samlTokenConfigMap.put(KEY_clockSkew, (Long) samlTokenProps.get(KEY_clockSkew) / 1000); // Long seconds
        String shortMethod = trim((String) samlTokenProps.get(KEY_requiredSubjectConfirmationMethod));
        if (!("bearer".equalsIgnoreCase(shortMethod))) {
            //Tr.warning(tc, "The Specified Subject Confirmation method is not supported, bearer method will be used.");
            shortMethod = "bearer";
        }
        samlTokenConfigMap.put(KEY_requiredSubjectConfirmationMethod, subjectConfirmationMethods.get(shortMethod)); // String
        samlTokenConfigMap.put(KEY_timeToLive, (Long) samlTokenProps.get(KEY_timeToLive) / 1000); //Long seconds

        // Handle audienceRestrictions
        String[] audienceRestrictions = trim((String[]) samlTokenProps.get(KEY_audienceRestrictions));
        if (audienceRestrictions != null) {
            int iI = 0;
            for (; iI < audienceRestrictions.length; iI++) {
                try {
                    audienceRestrictions[iI] = URLDecoder.decode(audienceRestrictions[iI], "UTF-8");
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "audienceRestriction[" + iI + "] = " + audienceRestrictions[iI]);
                    }
                } catch (UnsupportedEncodingException e) {
                    throw new Exception(e); // handle the unexpected Exception
                }
            }
            if (iI == 0)
                audienceRestrictions = null;
        }
        samlTokenConfigMap.put(KEY_audienceRestrictions, audienceRestrictions);
        return samlTokenConfigMap;
    }

    /**
     */

    private void setAndValidateProperties() {

        pids.clear();
        //Configuration config = null;
        //String wss_default = (String) properties.get(CFG_KEY_NAME);
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
                    String sigPropsConfigPid = (String) entry.getValue();//(String) properties.get(entry_key);
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
                            CryptoUtils.logInsecureAlgorithm("wsSecurityProvider.signatureProperties.signatureAlgorithm", algorithm);
                        }
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Empty ws-security provider signature configuration ", sigPropsConfigPid);
                        }
                    }

                } catch (IOException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Invalid ws-security provider signature configuration: " + e);
                    }
                }

            } else if ("encryptionProperties".equals(entry_key)) {
                try {
                    String encPropsConfigPid = (String) entry.getValue();//(String) properties.get(entry_key);
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
                            Tr.debug(tc, "Empty ws-security provider encryption configuration ", encPropsConfigPid);
                        }
                    }

                } catch (IOException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Invalid ws-security provider encryption configuration: " + e);
                    }
                }
            } else if ("callerToken".equals(entry_key)) { //caller configuration
                try {
                    //String callerConfig = (String) entry.getValue();
                    String[] callerConfig = (String[]) entry.getValue();

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        int len = callerConfig.length;
                        while (len > 0) {
                            Tr.debug(tc, "caller configuration  = ", callerConfig[len - 1]);
                            len--;
                        }
                        //Tr.debug(tc, "caller configuration name = ",);
                    }

                    pids.add(callerConfig[0]);
                    Map<String, Object> callerConfigMap = convertToMap(callerConfig[0]);
                    if (callerConfigMap != null && !callerConfigMap.isEmpty()) {
                        for (String key : SPECIAL_CFG_KEYS) {
                            callerConfigMap.remove(key);
                        }
                        if (callerConfigMap.get(WSSecurityConstants.CALLER_NAME) != null) {
                            String callerName = (String) callerConfigMap.get(WSSecurityConstants.CALLER_NAME);
                            if (!callerName.isEmpty()) {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "caller configuration name = ", callerName);
                                }
                                if (!(WSSecurityConstants.UNT_CALLER_NAME.equalsIgnoreCase(callerName)) &&
                                    !(WSSecurityConstants.X509_CALLER_NAME.equalsIgnoreCase(callerName)) &&
                                    !(WSSecurityConstants.SAML_CALLER_NAME.equalsIgnoreCase(callerName))) {
                                    StringBuffer sb = new StringBuffer();
                                    sb.append("UsernameToken").append(", ").append("X509Token").append("samlToken");
                                    Tr.error(tc, "UNKNOWN_CALLER_TOKEN_NAME",
                                             new Object[] { callerName, sb.toString() });
                                }
                                defaultConfigMap.put(WSSecurityConstants.CALLER_CONFIG, callerConfigMap);
                            }

                        }
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Empty ws-security provider caller configuration ");
                        }
                    }
                } catch (IOException ioe) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Invalid ws-security provider caller configuration: " + ioe);
                    }
                }

            } else {
                //skip special config keys
                if (entry_key.startsWith(".")
                    || entry_key.startsWith("config.")
                    || entry_key.startsWith("service.")
                    || entry_key.equals("id")
                    || entry_key.startsWith("osgi.ds.")) {
                    continue;
                }
                Object entry_value = entry.getValue();//(String) properties.get(entry_key);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "ws-security provider configuration entry key = ", entry_key);
                    Tr.debug(tc, "ws-security provider configuration entry value = ", entry_value);
                }
                if (entry_value != null) {
                    //handle ws-security.cache.config.file property
                    if (WSSecurityConstants.CXF_NONCE_CACHE_CONFIG_FILE.equals(entry_key) || WSSecurityConstants.SEC_NONCE_CACHE_CONFIG_FILE.equals(entry_key)) {
                        String cache_file = (String) entry.getValue();
                        if (cache_file != null && !cache_file.isEmpty()) {
                            //Make sure that cache_file exists before prepending file:
                            cache_file = cache_file.replace('\\', '/');
                            File f = new File(cache_file);
                            if (f.exists()) {
                                // System.out.println("gkuo:get ws-security.cache.config.file File");
                                StringBuffer sb = new StringBuffer("file:///"); // Defect 102409
                                sb.append(cache_file);
                                defaultConfigMap.put(entry_key, sb.toString());
                            } else {
                                // System.out.println("gkuo:get ws-security.cache.config.file URL");
                                defaultConfigMap.put(entry_key, cache_file);
                            }
                        }
                    } else {
                        defaultConfigMap.put(entry_key, entry_value);
                    }
                    if (WSSecurityConstants.CXF_CBH.equals(entry_key)) {
                        this.cfgCallback = (String) entry_value;
                    }
                }

            }
        }
        if (defaultConfigMap.isEmpty()) {
            Tr.info(tc, "WSSECURITY_NO_CONFIG_DEFINED_PROV");
        } /*
           * else if (!(defaultConfigMap.containsKey(SecurityConstants.RETURN_SECURITY_ERROR))) {
           * defaultConfigMap.put(SecurityConstants.RETURN_SECURITY_ERROR, true); //v3
           * }
           */

    }

    //}

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

    String[] trim(final String[] originals) {
        if (originals == null || originals.length == 0)
            return null;
        String[] tmpResults = new String[originals.length];
        int iCnt = 0;
        for (int iI = 0; iI < originals.length; iI++) {
            String original = trim(originals[iI]);
            if (original != null)
                tmpResults[iCnt++] = original;
        }
        if (iCnt == 0)
            return null;
        String[] results = new String[iCnt];
        System.arraycopy(tmpResults, 0, results, 0, iCnt);
        return results;
    }

    String trim(String original) {
        if (original == null)
            return null;
        String result = original.trim();
        if (result.isEmpty())
            return null;
        return result;
    }

    //public Collection<X509Certificate> getSamlTrustAnchors() {
    //    Collection<X509Certificate> samlTrustAnchors = new ArrayList<X509Certificate>();
    //    if (isSamlTrustEngineEnabled) {
    //        try {
    //            searchTrustAnchors(samlTrustAnchors, trustAnchorName);
    //        } catch (Exception e) {
    //            // ignore it. The SSL keyStoreService may not be ready
    //        }
    //    }
    //    return samlTrustAnchors;
    //}

    //public boolean searchTrustAnchors(Collection<X509Certificate> trustAnchors, String trustAnchorName) throws Exception {
    //    if (trustAnchorName == null || trustAnchorName.isEmpty()) {
    //        trustAnchorName = getDefaultKeyStoreName("com.ibm.ssl.trustStoreName");
    //    }
    //
    //    KeyStoreService keyStoreService = keyStoreServiceRef.getService();
    //
    //    if (keyStoreService == null)
    //        return false;
    //    Collection<String> certNames;
    //    try {
    //        certNames = keyStoreService.getTrustedCertEntriesInKeyStore(trustAnchorName);
    //        for (String certName : certNames) {
    //            X509Certificate cert = keyStoreService.getX509CertificateFromKeyStore(trustAnchorName, certName);
    //            if (tc.isDebugEnabled()) {
    //                Tr.debug(tc, "getCert trustAnchorName:" + trustAnchorName + " certId:" + certName + " cert:" + cert);
    //            }
    //            trustAnchors.add(cert);
    //        }
    //    } catch (KeyStoreException e) {
    //        throw new Exception(e);
    //    } catch (CertificateException e) {
    //        throw new Exception(e);
    //    }
    //    return true;
    //}

    //public String getDefaultKeyStoreName(String propKey) {
    //    String keyStoreName = null;
    //    //config does not specify keystore, so try to get one from servers default ssl config.
    //    SSLSupport sslSupport = sslSupportRef.getService();
    //    JSSEHelper jsseHelper = sslSupport.getJSSEHelper();
    //    Properties props = null;
    //    if (jsseHelper != null) {
    //        try {
    //            props = jsseHelper.getProperties("", null, null, true);
    //        } catch (SSLException e) {
    //            // TODO Auto-generated catch block
    //            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
    //            // http://was.pok.ibm.com/xwiki/bin/view/Liberty/LoggingFFDC
    //            //e.printStackTrace();
    //            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
    //                Tr.debug(tc, "Exception getting jssehelper!!!");
    //            }
    //        }
    //        if (props != null) {
    //            keyStoreName = props.getProperty(propKey);
    //            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
    //                Tr.debug(tc, "KeyStore name from default ssl config = " + keyStoreName);
    //            }
    //        }
    //    }
    //    return keyStoreName;
    //}

}
