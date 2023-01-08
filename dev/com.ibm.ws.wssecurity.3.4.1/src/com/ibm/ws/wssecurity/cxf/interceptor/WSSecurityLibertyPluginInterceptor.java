/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.wssecurity.cxf.interceptor;


import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.cache.CXFEHCacheReplayCache;
import org.apache.cxf.ws.security.tokenstore.EHCacheTokenStore;
import org.apache.cxf.ws.security.tokenstore.MemoryTokenStoreFactory;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreFactory;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.PolicyBasedWSS4JOutInterceptor;
import org.apache.wss4j.common.cache.EHCacheValue;
import org.apache.wss4j.common.cache.MemoryReplayCache;
import org.apache.wss4j.common.cache.ReplayCache;
import org.apache.wss4j.common.cache.WSS4JCacheUtil;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.wssecurity.cxf.validator.UsernameTokenValidator;
import com.ibm.ws.wssecurity.cxf.validator.Utils;
import com.ibm.ws.wssecurity.cxf.validator.WssSamlAssertionValidator;
import com.ibm.ws.wssecurity.internal.WSSecurityConstants;
import com.ibm.ws.wssecurity.signature.SignatureAlgorithms;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

import io.openliberty.wssecurity.WSSecurityFeatureHelper;


public class WSSecurityLibertyPluginInterceptor extends AbstractSoapInterceptor {

    private static final Map<String, Object> providerConfigMap = Collections.synchronizedMap(new HashMap<String, Object>());
    //new HashMap<String, Object>();
    private static final Map<String, Object> clientConfigMap = Collections.synchronizedMap(new HashMap<String, Object>());
    //new HashMap<String, Object>();
    private static final TraceComponent tc = Tr.register(WSSecurityLibertyPluginInterceptor.class,
                                                         WSSecurityConstants.TR_GROUP, WSSecurityConstants.TR_RESOURCE_BUNDLE);

    private static final String SIGNATURE_METHOD = "signatureAlgorithm";
    private static Map<String, Object> samlTokenConfigMap = null; //unmodifiableMap
    private static boolean signatureConfigChanged = false;
    private static boolean clientSignatureConfigChanged = false;

    public WSSecurityLibertyPluginInterceptor() {
        super(Phase.PRE_PROTOCOL);
        addBefore(PolicyBasedWSS4JInInterceptor.class.getName());
        addBefore(PolicyBasedWSS4JOutInterceptor.class.getName());
    }

    public static void setBindingsConfiguration(Map<String, Object> map) {
        signatureConfigChanged = true;
        if (map != null) {
            if (!providerConfigMap.isEmpty()) {
                providerConfigMap.clear();
            }
            providerConfigMap.putAll(map);
        } else {
            providerConfigMap.clear();
        }
    }

    public static void setClientBindingsConfiguration(Map<String, Object> map) {
        clientSignatureConfigChanged = true;
        if (map != null) {
            if (!clientConfigMap.isEmpty()) {
                clientConfigMap.clear();
            }
            clientConfigMap.putAll(map);
        } else {
            clientConfigMap.clear();
        }
    }

    /**
     * @param samlTokenConfigMap -- unmodifiableMap
     */
    public static void setSamlTokenConfiguration(Map<String, Object> tmpSamlTokenConfigMap) {
        samlTokenConfigMap = tmpSamlTokenConfigMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.cxf.interceptor.Interceptor#handleMessage(org.apache.cxf.message.Message)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void handleMessage(@Sensitive SoapMessage message) throws Fault {
        if (message == null) {
            return;
        }
        boolean isReq = MessageUtils.isRequestor(message);
        //boolean isOut = MessageUtils.isOutbound(message);

        if (isReq) { //client -
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "client side message = ", message);
            }
            //checkConfigMap(clientConfigMap);
            Set<String> client_config_keys = clientConfigMap.keySet();
            Iterator<String> keyIt = client_config_keys.iterator();
            //check whether user name is specified via request context
            boolean user_id_exists = false;
            if (message.getContextualProperty(WSSecurityConstants.CXF_USER_NAME) != null || message.getContextualProperty(WSSecurityConstants.SEC_USER_NAME) != null) {
                user_id_exists = true;
            }

            while (keyIt.hasNext()) {
                String key = keyIt.next();
                if (message.getContextualProperty(key) == null) {
                    //check whether config has password
                    if ((WSSecurityConstants.CXF_SIG_PROPS.equals(key) || WSSecurityConstants.SEC_SIG_PROPS.equals(key)) &&
                        message.getContextualProperty(WSSecurityConstants.SEC_SIG_PROPS) == null) {
                        Map<String, Object> tempMap = (Map<String, Object>) clientConfigMap.get(WSSecurityConstants.SEC_SIG_PROPS); //v3
                        if (tempMap == null) {
                            tempMap = (Map<String, Object>) clientConfigMap.get(WSSecurityConstants.CXF_SIG_PROPS); //v3
                        }
                        if (tempMap != null) {
                            Map<String, Object> sigPropsMap = new HashMap<String, Object>(tempMap);
                            Utils.modifyConfigMap(sigPropsMap);
                            Properties sigProps = new Properties();
                            sigProps.putAll(sigPropsMap);
                            message.setContextualProperty(key, sigProps);
                            if (clientSignatureConfigChanged) {
                                //message.setContextualProperty(WSSecurityConstants.CXF_SIG_CRYPTO, null);
                                message.setContextualProperty(WSSecurityConstants.SEC_SIG_CRYPTO, null);
                                clientSignatureConfigChanged = false;
                            }
                            message.setContextualProperty(WSSecurityConstants.SEC_SIG_CRYPTO, null); //TODO: look into introducing a new option
                            //message.setContextualProperty(WSSecurityConstants.CXF_SIG_CRYPTO, Utils.getCrypto(sigProps)); //@2020 TODO
                            SignatureAlgorithms.setAlgorithm(message, (String) tempMap.get(SIGNATURE_METHOD));
                        }
                    } else if ((WSSecurityConstants.CXF_ENC_PROPS.equals(key) || WSSecurityConstants.SEC_ENC_PROPS.equals(key)) &&
                               message.getContextualProperty(WSSecurityConstants.SEC_ENC_PROPS) == null) {
                        Map<String, Object> tempMap = (Map<String, Object>) clientConfigMap.get(WSSecurityConstants.SEC_ENC_PROPS); //v3
                        if (tempMap == null) {
                            tempMap = (Map<String, Object>) clientConfigMap.get(WSSecurityConstants.CXF_ENC_PROPS); //v3
                        }
                        if (tempMap != null) {
                            Map<String, Object> encPropsMap = new HashMap<String, Object>(tempMap);
                            Utils.modifyConfigMap(encPropsMap);
                            Properties encProps = new Properties();
                            encProps.putAll(encPropsMap);
                            message.setContextualProperty(key, encProps);
                            //message.setContextualProperty(WSSecurityConstants.CXF_ENC_CRYPTO, Utils.getCrypto(encProps)); //@2020 TODO
                        }
                    } else if (WSSecurityConstants.CXF_USER_PASSWORD.equals(key) || WSSecurityConstants.SEC_USER_PASSWORD.equals(key)) { //v3
                        //if user is specified via request context, 
                        //then don't bother checking for password in the server.xml
                        if (!user_id_exists) {
                            String pwd = null;
                            if (clientConfigMap.get(WSSecurityConstants.SEC_USER_PASSWORD) != null) {
                                pwd = Utils.changePasswordType((SerializableProtectedString) clientConfigMap.get(WSSecurityConstants.SEC_USER_PASSWORD));
                            } else if (clientConfigMap.get(WSSecurityConstants.CXF_USER_PASSWORD) != null) {
                                pwd = Utils.changePasswordType((SerializableProtectedString) clientConfigMap.get(WSSecurityConstants.CXF_USER_PASSWORD));
                            }

                            message.setContextualProperty(key, pwd);
                        }

                    } else {
                        message.setContextualProperty(key, clientConfigMap.get(key));
                    }

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Client Config attribute is set on message = ",
                                 key, ", value = ", clientConfigMap.get(key));
                    }
                }
                // set default samlCallbackHandler if none is set
                String samlCallbackHandler = (String) message.getContextualProperty(WSSecurityConstants.CXF_SAML_CALLBACK_HANDLER);
                if (samlCallbackHandler == null || samlCallbackHandler.isEmpty()) {
                    message.setContextualProperty(WSSecurityConstants.CXF_SAML_CALLBACK_HANDLER,
                                                  WSSecurityConstants.DEFAULT_SAML_CALLBACK_HANDLER);
                }
            }

        } else { //provider
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "provider side message = ", message);
            }
            // Handle UserNameTokenValidator
            Object validator = message.getContextualProperty(SecurityConstants.USERNAME_TOKEN_VALIDATOR);
            if (validator == null) {
                message.put(SecurityConstants.USERNAME_TOKEN_VALIDATOR, new UsernameTokenValidator());
            }

            // Handle saml assertion validator. We only handle saml20 
            validator = message.getContextualProperty(SecurityConstants.SAML2_TOKEN_VALIDATOR);
            if (validator == null) {
                // override SamlTokenValidator
                message.put(SecurityConstants.SAML2_TOKEN_VALIDATOR, new WssSamlAssertionValidator(samlTokenConfigMap));
                if (samlTokenConfigMap != null) {
                    String[] restrictions = (String[]) samlTokenConfigMap.get(WSSecurityConstants.KEY_audienceRestrictions);
                    if (restrictions == null || restrictions.length < 1) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "set audience restriction validation to false");
                        }
                        message.put(SecurityConstants.AUDIENCE_RESTRICTION_VALIDATION, false); //v3
                    }
                }

            }

            Set<String> provider_config_keys = providerConfigMap.keySet();
            Iterator<String> keyIt = provider_config_keys.iterator();

            while (keyIt.hasNext()) {
                String key = keyIt.next();

                //check whether config has password
                if ((WSSecurityConstants.CXF_SIG_PROPS.equals(key) || WSSecurityConstants.SEC_SIG_PROPS.equals(key)) &&
                    message.getContextualProperty(WSSecurityConstants.SEC_SIG_PROPS) == null) {
                    Map<String, Object> tempMap = (Map<String, Object>) providerConfigMap.get(WSSecurityConstants.SEC_SIG_PROPS); //v3
                    if (tempMap == null) {
                        tempMap = (Map<String, Object>) providerConfigMap.get(WSSecurityConstants.CXF_SIG_PROPS); //v3
                    }
                    if (tempMap != null) {
                        Map<String, Object> sigPropsMap = new HashMap<String, Object>(tempMap);
                        Utils.modifyConfigMap(sigPropsMap);
                        Properties sigProps = new Properties();
                        sigProps.putAll(sigPropsMap);
                        message.setContextualProperty(key, sigProps);
                        if (signatureConfigChanged) {
                            //message.setContextualProperty(WSSecurityConstants.CXF_SIG_CRYPTO, null);
                            message.setContextualProperty(WSSecurityConstants.SEC_SIG_CRYPTO, null);
                            signatureConfigChanged = false;
                        }
                        message.setContextualProperty(WSSecurityConstants.SEC_SIG_CRYPTO, null); //TODO: look into introducing a new option?
                        //message.setContextualProperty(WSSecurityConstants.CXF_SIG_CRYPTO, Utils.getCrypto(sigProps)); //@2020 TODO
                        SignatureAlgorithms.setAlgorithm(message, (String) tempMap.get(SIGNATURE_METHOD));
                    }
                } else if ((WSSecurityConstants.CXF_ENC_PROPS.equals(key) || WSSecurityConstants.SEC_ENC_PROPS.equals(key)) &&
                           message.getContextualProperty(WSSecurityConstants.SEC_ENC_PROPS) == null) {
                    Map<String, Object> tempMap = (Map<String, Object>) providerConfigMap.get(WSSecurityConstants.SEC_ENC_PROPS); //v3
                    if (tempMap == null) {
                        tempMap = (Map<String, Object>) providerConfigMap.get(WSSecurityConstants.CXF_ENC_PROPS); //v3
                    }
                    if (tempMap != null) {
                        Map<String, Object> encPropsMap = new HashMap<String, Object>(tempMap);
                        Utils.modifyConfigMap(encPropsMap);
                        Properties encProps = new Properties();
                        encProps.putAll(encPropsMap);
                        message.setContextualProperty(key, encProps);
                        //message.setContextualProperty(WSSecurityConstants.CXF_ENC_CRYPTO, Utils.getCrypto(encProps)); //@2020 TODO
                    }
                } else if (WSSecurityConstants.CXF_NONCE_CACHE_CONFIG_FILE.equals(key)) {
                    //handle ws-security.cache.config.file property
                    handleehcacheconfigfile(providerConfigMap, message);
                } else {
                    message.setContextualProperty(key, providerConfigMap.get(key));
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Provider Config attribute is set on message = ",
                             key, ", value = ", providerConfigMap.get(key));
                }
            }
        }
    }

    /**
     * @param providerconfigmap2
     * @param message
     */
    @FFDCIgnore (Exception.class)
    private void handleehcacheconfigfile(Map<String, Object> providerconfigmap2, @Sensitive SoapMessage message) {
        
        boolean ncache = false, tcache = false, scache = false;
        URL configfile = null;
        WSSecurityFeatureHelper helper = new WSSecurityFeatureHelper();
        if (!helper.isWSSecurityFeatureHelperServiceActive()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Ignoring cache file configuration property, this is not supported : ", SecurityConstants.CACHE_CONFIG_FILE); // TODO : informational
            }
            return;
        }
        try {
            
            ncache = isWSS4JCacheEnabled(SecurityConstants.ENABLE_NONCE_CACHE, message);
            tcache = isWSS4JCacheEnabled(SecurityConstants.ENABLE_TIMESTAMP_CACHE, message);
            scache = isWSS4JCacheEnabled(SecurityConstants.ENABLE_SAML_ONE_TIME_USE_CACHE, message);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "are caches enabled? (nonce, timestamp, saml one time use) : ", ncache, tcache, scache);
                Tr.debug(tc, "setting up cache config file property on the message : ", SecurityConstants.CACHE_CONFIG_FILE);
                Tr.debug(tc, "cache config file : ", providerconfigmap2.get(SecurityConstants.CACHE_CONFIG_FILE));
            }
            message.setContextualProperty(SecurityConstants.CACHE_CONFIG_FILE, providerconfigmap2.get(SecurityConstants.CACHE_CONFIG_FILE));
            configfile = SecurityUtils.getConfigFileURL(message, SecurityConstants.CACHE_CONFIG_FILE, null);
            if (configfile != null) {
                if (ncache && !ehcacheinstanceavailable(SecurityConstants.NONCE_CACHE_INSTANCE, message)) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "try creating nonce cache using oldconfig ");
                    }
                    helper.handleEhcache2Mapping(SecurityConstants.NONCE_CACHE_INSTANCE, configfile, message);
                    createwss4jcacheinstance(SecurityConstants.NONCE_CACHE_INSTANCE, message);
                }
                if (tcache && !ehcacheinstanceavailable(SecurityConstants.TIMESTAMP_CACHE_INSTANCE, message)) {
                    helper.handleEhcache2Mapping(SecurityConstants.TIMESTAMP_CACHE_INSTANCE, configfile, message);
                    createwss4jcacheinstance(SecurityConstants.TIMESTAMP_CACHE_INSTANCE, message);
                }
                if (scache && !ehcacheinstanceavailable(SecurityConstants.SAML_ONE_TIME_USE_CACHE_INSTANCE, message)) {
                    helper.handleEhcache2Mapping(SecurityConstants.SAML_ONE_TIME_USE_CACHE_INSTANCE, configfile, message);
                    createwss4jcacheinstance(SecurityConstants.SAML_ONE_TIME_USE_CACHE_INSTANCE, message);
                }
                if (!ehcacheinstanceavailable(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE, message)) {
                    StringBuilder cacheKey = new StringBuilder(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
                    String cacheIdentifier = (String) providerconfigmap2.get(SecurityConstants.CACHE_IDENTIFIER);
                    if (cacheIdentifier != null) {
                        cacheKey.append('-').append(cacheIdentifier);
                    }
                    helper.handleEhcache2Mapping(cacheKey.toString(), configfile, message);
                    createtokenstorecacheinstance(cacheKey.toString(), message);
                }
            }
        } catch (Exception e) {
            message.setContextualProperty(SecurityConstants.CACHE_CONFIG_FILE, providerconfigmap2.get(SecurityConstants.CACHE_CONFIG_FILE));
        }        
    }


    /**
     * @param tokenStoreCacheInstance
     * @param message
     */
    private void createtokenstorecacheinstance(String cacheKey, @Sensitive SoapMessage message) throws Exception {
        
        String key = "liberty:".concat(cacheKey);
        if (message.getContextualProperty(key) != null) {
            HashMap oldconfig = (HashMap) message.getContextualProperty(key);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc,  "getting the old config from the message, key =  " + key + ", disk store path = " + (String)oldconfig.get("getDiskStorePath")
                + ", ttl = " + (long)oldconfig.get("getTimeToLiveSeconds") + ", tti = " + (long)oldconfig.get("getTimeToIdleSeconds"));
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc,  "getting the old config from the message, heap =  " + (long)oldconfig.get("getMaxEntriesLocalHeap") + ", disk elements = " + (int)oldconfig.get("getMaxElementsOnDisk")
                + ", disk bytes? = " + (long)oldconfig.get("getMaxBytesLocalDisk") + ", overflow to disk = " + oldconfig.get("isOverflowToDisk"));
            }
            //TokenStoreUtils getTokenStore
            EndpointInfo info = message.getExchange().getEndpoint().getEndpointInfo();
            synchronized (info) {
                TokenStore tokenStore =
                    (TokenStore)message.getContextualProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
                if (tokenStore == null) {
                    tokenStore = (TokenStore)info.getProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE);
                }
                if (tokenStore == null) {
                    StringBuilder cachekey2 = new StringBuilder(cacheKey);
                    if (info.getName() != null) {
                        int hashcode = info.getName().toString().hashCode();
                        if (hashcode >= 0) {
                            cachekey2.append('-');
                        }
                        cachekey2.append(hashcode);
                    }                 
                    if (TokenStoreFactory.isEhCacheInstalled()) {
                        Bus bus = message.getExchange().getBus();
                        tokenStore = new EHCacheTokenStore(cachekey2.toString(), bus, oldconfig);                                
                    } else {
                        tokenStore = new MemoryTokenStoreFactory().newTokenStore(cachekey2.toString(), message);
                    }
                    info.setProperty(SecurityConstants.TOKEN_STORE_CACHE_INSTANCE, tokenStore);         
                }
            }
            message.remove(key);
        }
    }

    /**
     * @param nonceCacheInstance
     * @param message
     */
    private void createwss4jcacheinstance(String instanceKey, @Sensitive SoapMessage message) throws Exception {
        
        String key = "liberty:".concat(instanceKey);
        if (message.getContextualProperty(key) != null) {
            org.ehcache.CacheManager cacheManager = null;
            org.ehcache.Cache<String, EHCacheValue> cache = null;
            
            HashMap oldconfig = (HashMap) message.getContextualProperty(key);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc,  "getting the old config from the message, key =  " + key + ", disk store path = " + (String)oldconfig.get("getDiskStorePath")
                + ", ttl = " + (long)oldconfig.get("getTimeToLiveSeconds") + ", tti = " + (long)oldconfig.get("getTimeToIdleSeconds"));
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc,  "getting the old config from the message, heap =  " + (long)oldconfig.get("getMaxEntriesLocalHeap") + ", disk elements = " + (int)oldconfig.get("getMaxElementsOnDisk")
                + ", disk bytes? = " + (long)oldconfig.get("getMaxBytesLocalDisk") + ", overflow to disk = " + oldconfig.get("isOverflowToDisk"));
            }

            //WSS4JUtils getReplayCache
            ReplayCache replayCache = (ReplayCache)message.getContextualProperty(instanceKey);
            Endpoint ep = message.getExchange().getEndpoint();
            if (replayCache == null && ep != null && ep.getEndpointInfo() != null) {
                EndpointInfo info = ep.getEndpointInfo();
                synchronized (info) {
                    replayCache = (ReplayCache)info.getProperty(instanceKey);

                    if (replayCache == null) {
                        String cacheKey = instanceKey;
                        if (info.getName() != null) {
                            int hashcode = info.getName().toString().hashCode();
                            if (hashcode < 0) {
                                cacheKey += hashcode;
                            } else {
                                cacheKey += "-" + hashcode;
                            }
                        }
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc,  "creating new cache using oldconfig, cache key =  ", cacheKey);
                        }
                        if (WSS4JCacheUtil.isEhCacheInstalled()) {                
                            Bus bus = message.getExchange().getBus();
                            Path diskstorePath = null;
                            String path = (String)oldconfig.get("getDiskStorePath");
                            diskstorePath = Paths.get(path);
                            replayCache = new CXFEHCacheReplayCache(cacheKey, bus, diskstorePath, oldconfig);
                        } else {
                            replayCache = new MemoryReplayCache();
                        }

                       info.setProperty(instanceKey, replayCache) ;
                    }
                }
            }
            message.remove(key);
        }
        
    }

    /**
     * @param enableNonceCache 
     * @param message
     */
    private boolean isWSS4JCacheEnabled(String enablekey, @Sensitive SoapMessage message) {
        boolean specified = false;
        Object o = message.getContextualProperty(enablekey);
        if (o != null) {
            if (!PropertyUtils.isTrue(o)) {
                return false;
            }
            specified = true;
        }

        if (!specified && MessageUtils.isRequestor(message)) {
            return false;
        }
        return true;
    }

    /**
     * @param cachekey 
     * @param configfile
     * @param message 
     */
    /*@FFDCIgnore (Exception.class)
    private void parseehcachefile(String instanceKey, URL configfile, @Sensitive SoapMessage message) throws Exception{
        net.sf.ehcache.CacheManager cacheManager = null;

        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(net.sf.ehcache.store.DefaultElementValueComparator.class.getClassLoader());
            cacheManager = net.sf.ehcache.CacheManager.create(configfile);
            net.sf.ehcache.config.CacheConfiguration cc = null;
            net.sf.ehcache.config.Configuration config = null;
            config = cacheManager.getConfiguration();
            if (config != null) {
                String cacheKey = instanceKey;
                cc = cacheManager.getConfiguration().getCacheConfigurations().get(cacheKey);

            }
            if (cc == null) {
                cc = cacheManager.getConfiguration().getDefaultCacheConfiguration();
            }
            if (cc != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc,  "success getting cache using oldconfig!!!");
                }
                
                updateMessageCacheMap(instanceKey, message, cc, config);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "removing the cache config file property from the message : ", SecurityConstants.CACHE_CONFIG_FILE);
                }
                message.remove(SecurityConstants.CACHE_CONFIG_FILE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "cannot parse the file using the old cache apis, instancekey = " + instanceKey + ", url = " + configfile.getFile());
                Tr.debug(tc,  "Exception parsing the old ehcache config format = ", e.getMessage());
            }
            throw e;
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader); 
        }
        
    }*/

    /**
     * @param instanceKey 
     * @param message
     * @param cc
     * @param config 
     */
    /*
    private void updateMessageCacheMap(String instanceKey, @Sensitive SoapMessage message, net.sf.ehcache.config.CacheConfiguration cc, Configuration config) {
        String key = "liberty:".concat(instanceKey);
        if (cc != null) {
            Map<String, Object> configmap = new HashMap<String, Object>();
            
            configmap.put("getTimeToLiveSeconds", cc.getTimeToLiveSeconds());
            configmap.put("getTimeToIdleSeconds", cc.getTimeToIdleSeconds());
            configmap.put("getMaxEntriesLocalHeap", cc.getMaxEntriesLocalHeap());
            configmap.put("getMaxBytesLocalDisk", cc.getMaxBytesLocalDisk());
            configmap.put("isEternal", cc.isEternal());
            configmap.put("isOverflowToDisk", cc.isOverflowToDisk());
            configmap.put("getMaxElementsOnDisk", cc.getMaxElementsOnDisk());
            configmap.put("isDiskPersistent", cc.isDiskPersistent());
            configmap.put("getDiskExpiryThreadIntervalSeconds", cc.getDiskExpiryThreadIntervalSeconds());
            configmap.put("getMemoryStoreEvictionPolicy", cc.getMemoryStoreEvictionPolicy());
            String path = (String)config.getDiskStoreConfiguration().getOriginalPath();
            if ("java.io.tmpdir".equals(path)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc,  "updating diskstorepath, before the update = " , path);
                }
                Bus bus = message.getExchange().getBus();
                path = path + File.separator
                                + bus.getId();
                config.getDiskStoreConfiguration().setPath(path);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc,  "updating diskstorepath, after the update = " , path);
                }
            }
            configmap.put("getDiskStorePath", path);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc,  "updating message using oldconfig, key =  " , key);
                Tr.debug(tc,  "updating message using oldconfig, ttl, tti =  " , configmap.get("getTimeToLiveSeconds"), configmap.get("getTimeToIdleSeconds"));
                Tr.debug(tc,  "updating message using oldconfig, diskstorepath = " , configmap.get("getDiskStorePath"));
                Tr.debug(tc,  "updating message using oldconfig, diskelements = " , configmap.get("getMaxElementsOnDisk"));
                Tr.debug(tc,  "updating message using oldconfig, diskbytes = " , configmap.get("getMaxBytesLocalDisk"));
            }
            String[] ignored = {"getDiskExpiryThreadIntervalSeconds", "getMemoryStoreEvictionPolicy", "isOverflowToDisk"};
            Tr.info(tc, "using an old ehcache configuration and these properties will be ignored, " +  ignored[0] + 
                    "= " + (long)configmap.get("getDiskExpiryThreadIntervalSeconds") + ", " + ignored[1] + "= " + configmap.get("getMemoryStoreEvictionPolicy") 
                    + ", " + ignored[2] + "= " + configmap.get("isOverflowToDisk")
                   );
            message.setContextualProperty(key, configmap);
        }
        
    } */

    /**
     * @param message
     * @return
     */
    private boolean ehcacheinstanceavailable(String cachekey, @Sensitive SoapMessage message) {
        
        if (message.getContextualProperty(cachekey) != null) {
            return true;
        }
        Endpoint ep = message.getExchange().getEndpoint();
        if (ep != null && ep.getEndpointInfo() != null) {
            EndpointInfo info = ep.getEndpointInfo();
            synchronized (info) {
                if (info.getProperty(cachekey) != null) {
                    return true;
                }
            }
        }
        return false;
    }
    
}
