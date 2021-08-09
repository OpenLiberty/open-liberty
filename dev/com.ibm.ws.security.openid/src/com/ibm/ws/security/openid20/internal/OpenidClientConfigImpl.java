/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openid20.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.openid20.OpenidClientConfig;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.FilterUtils;

/**
 * Process the openID entry in the server.xml file
 */

public class OpenidClientConfigImpl implements OpenidClientConfig {
    private static final TraceComponent tc = Tr.register(OpenidClientConfigImpl.class);

    public static final String CFG_KEY_ALLOW_STATELESS = "allowStateless";
    public static final String CFG_KEY_MAP_IDENTITY_TO_REGISTRY_USER = "mapIdentityToRegistryUser";
    public static final String CFG_KEY_USE_CLIENT_IDENTITY = "useClientIdentity";
    public static final String CFG_KEY_AUTHENTICATION_MODE = "authenticationMode";
    public static final String CFG_KEY_MAX_ASSOCIATION_ATTEMPS = "maxAssociationAttempts";
    public static final String CFG_KEY_NONCE_VALID_TIME = "nonceValidTime";

    public static final String CFG_KEY_SHARED_KEY_ENCRYPTION_ENABLED = "sharedKeyEncryptionEnabled";
    public static final String CFG_KEY_HASH_ALGORITHM = "hashAlgorithm";
    public static final String CFG_KEY_SSL_REF = "sslRef";
    public static final String CFG_KEY_USER_INFO_REF = "userInfoRef";
    public static final String CFG_KEY_HTTPS_REQUIRED = "httpsRequired";
    public static final String CFG_KEY_SEARCH_NUMBER_OF_USER_INFO_TO_MAP = "searchNumberOfUserInfoToMap";
    public static final String CFG_KEY_FAILED_ASSOC_EXPIRE = "failedAssocExpire";
    public static final String CFG_KEY_CONNECT_TIMEOUT = "connectTimeout";
    public static final String CFG_KEY_SOCKET_TIMEOUT = "socketTimeout";

    public static final String CFG_KEY_HOST_NAME_VERIFICATION_ENABLED = "hostNameVerificationEnabled";
    public static final String CFG_KEY_MAX_DISCOVERY_CACHE_SIZE = "maxDiscoveryCacheSize";
    public static final String CFG_KEY_MAX_DISCOVER_RETRY = "maxDiscoverRetry";

    public static final String CFG_KEY_GROUP_IDENTIFIER = "groupIdentifier";
    public static final String CFG_KEY_REALM_IDENTIFIER = "realmIdentifier";
    public static final String CFG_KEY_CHARACTER_ENCODING = "characterEncoding";
    public static final String CFG_KEY_INCLUDE_USER_INFO_IN_SUBJECT = "includeUserInfoInSubject";
    public static final String CFG_KEY_INCLUDE_CUSTOM_CACHE_KEY_IN_SUBJECT = "includeCustomCacheKeyInSubject";
    public static final String CFG_KEY_PROVIDER_IDENTIFIER = "providerIdentifier";
    public static final String CFG_KEY_AUTH_FILTER_REF = "authFilterRef";
    public static final String CFG_KEY_ALLOW_BASIC_AUTHENTICATION = "allowBasicAuthentication";
    public static final String CFG_KEY_TRY_OPENID_IF_BASIC_AUTH_FAILS = "tryOpenIDIfBasicAuthFails";

    public static final String CFG_KEY_ALIAS = "alias";
    public static final String CFG_KEY_URI_TYPE = "uriType";
    public static final String CFG_KEY_COUNT = "count";
    public static final String CFG_KEY_REQUIRED = "required";

    public static final String ENCRYPTION_NO = "no-encryption";
    public static final String ENCRYPTION_DH_SHA1 = "DH-SHA1";
    public static final String ENCRYPTION_DH_SHA256 = "DH-SHA256";

    public static final String SIGNATURE_HMAC_SHA1 = "HMAC-SHA1";
    public static final String SIGNATURE_HMAC_SHA256 = "HMAC-SHA256";

    public static final String HASH_ALG_SHA1 = "SHA1";
    public static final String HASH_ALG_SHA256 = "SHA256";

    public static final String KEY_CONFIGURATION_ADMIN = "configurationAdmin";

    private static final Object KEY_ID = "id";
    private final AtomicServiceReference<ConfigurationAdmin> configAdminRef = new AtomicServiceReference<ConfigurationAdmin>(KEY_CONFIGURATION_ADMIN);

    boolean allowStateless;
    private int maxAssociationAttempts;
    private long nonceValidTime;
    private boolean httpsRequired;
    private boolean mapIdentityToRegistryUser;
    private boolean useClientIdentity;
    private String sessionEncryptionType;
    private String signatureAlgorithm;
    private String sslRef;

    private long failedAssocExpire;
    private long connectTimeout;
    private long socketTimeout;
    public boolean hostNameVerificationEnabled;

    private int searchNumberOfUserInfoToMap;
    private int maxDiscoveryCacheSize;
    private int maxDiscoverRetry;

    private boolean checkImmediate;
    private String groupIdentifier;
    private String realmIdentifier;
    private String characterEncoding;
    private boolean includeUserInfoInSubject;
    private boolean includeCustomCacheKeyInSubject;
    private String providerIdentifier;
    private String authFilterRef;
    private String authFilterId;
    private boolean allowBasicAuthentication;
    private boolean tryOpenIDIfBasicAuthFails;

    private List<UserInfo> userInfo = new ArrayList<UserInfo>();

    private String bundleLocation;

    public OpenidClientConfigImpl() {}

    @Reference(name = KEY_CONFIGURATION_ADMIN, service = ConfigurationAdmin.class)
    protected void setConfigurationAdmin(ServiceReference<ConfigurationAdmin> ref) {
        configAdminRef.setReference(ref);
    }

    protected void unsetConfigurationAdmin(ServiceReference<ConfigurationAdmin> ref) {
        configAdminRef.unsetReference(ref);
    }

    protected synchronized void activate(ComponentContext cc, Map<String, Object> props) {
        configAdminRef.activate(cc);
        this.bundleLocation = cc.getBundleContext().getBundle().getLocation();
        processConfigProps(props);
        Tr.info(tc, "OPENID_RP_CONFIG_PROCESSED");
    }

    protected synchronized void modify(Map<String, Object> props) {
        processConfigProps(props);
        Tr.info(tc, "OPENID_RP_CONFIG_MODIFIED");
    }

    protected synchronized void deactivate(ComponentContext cc) {
        configAdminRef.deactivate(cc);
        this.bundleLocation = null;
    }

    /**
     * 
     * @param props
     */
    private void processConfigProps(Map<String, Object> props) {
        if (props == null || props.isEmpty())
            return;
        mapIdentityToRegistryUser = (Boolean) props.get(CFG_KEY_MAP_IDENTITY_TO_REGISTRY_USER);
        useClientIdentity = (Boolean) props.get(CFG_KEY_USE_CLIENT_IDENTITY);
        connectTimeout = (Long) props.get(CFG_KEY_CONNECT_TIMEOUT);
        allowStateless = (Boolean) props.get(CFG_KEY_ALLOW_STATELESS);
        failedAssocExpire = (Long) props.get(CFG_KEY_FAILED_ASSOC_EXPIRE);
        nonceValidTime = (Long) props.get(CFG_KEY_NONCE_VALID_TIME);
        maxDiscoveryCacheSize = (Integer) props.get(CFG_KEY_MAX_DISCOVERY_CACHE_SIZE);
        maxDiscoverRetry = (Integer) props.get(CFG_KEY_MAX_DISCOVER_RETRY);
        searchNumberOfUserInfoToMap = (Integer) props.get(CFG_KEY_SEARCH_NUMBER_OF_USER_INFO_TO_MAP);
        maxAssociationAttempts = (Integer) props.get(CFG_KEY_MAX_ASSOCIATION_ATTEMPS);
        socketTimeout = (Long) props.get(CFG_KEY_SOCKET_TIMEOUT);
        sslRef = (String) props.get(CFG_KEY_SSL_REF);
        hostNameVerificationEnabled = (Boolean) props.get(CFG_KEY_HOST_NAME_VERIFICATION_ENABLED);
        userInfo = processUserInfo(props, CFG_KEY_USER_INFO_REF);
        httpsRequired = (Boolean) props.get(CFG_KEY_HTTPS_REQUIRED);

        String authenticationMode = (String) props.get(CFG_KEY_AUTHENTICATION_MODE);
        if (authenticationMode != null && "checkid_immediate".equalsIgnoreCase(authenticationMode)) {
            checkImmediate = true;
        } else {
            checkImmediate = false;
        }

        boolean sharedKeyEncryptionEnabled = (Boolean) props.get(CFG_KEY_SHARED_KEY_ENCRYPTION_ENABLED);
        String hashAlgorithm = (String) props.get(CFG_KEY_HASH_ALGORITHM);
        setSessionEncryptionType(sharedKeyEncryptionEnabled, hashAlgorithm);
        setSignatureAlgorithm(hashAlgorithm);

        groupIdentifier = (String) props.get(CFG_KEY_GROUP_IDENTIFIER);
        realmIdentifier = (String) props.get(CFG_KEY_REALM_IDENTIFIER);
        characterEncoding = (String) props.get(CFG_KEY_CHARACTER_ENCODING);
        includeUserInfoInSubject = (Boolean) props.get(CFG_KEY_INCLUDE_USER_INFO_IN_SUBJECT);
        includeCustomCacheKeyInSubject = (Boolean) props.get(CFG_KEY_INCLUDE_CUSTOM_CACHE_KEY_IN_SUBJECT);
        providerIdentifier = (String) props.get(CFG_KEY_PROVIDER_IDENTIFIER);
        authFilterRef = (String) props.get(CFG_KEY_AUTH_FILTER_REF);
        authFilterId = getAuthFilterId(authFilterRef);
        allowBasicAuthentication = (Boolean) props.get(CFG_KEY_ALLOW_BASIC_AUTHENTICATION);
        tryOpenIDIfBasicAuthFails = (Boolean) props.get(CFG_KEY_TRY_OPENID_IF_BASIC_AUTH_FAILS);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "mapIdentityToRegistryUser: " + mapIdentityToRegistryUser);
            Tr.debug(tc, "useClientIdentity: " + useClientIdentity);
            Tr.debug(tc, "connectTimeout: " + connectTimeout);
            Tr.debug(tc, "allowStateless: " + allowStateless);
            Tr.debug(tc, "failedAssocExpire: " + failedAssocExpire);
            Tr.debug(tc, "nonceValidTime: " + nonceValidTime);
            Tr.debug(tc, "maxDiscoveryCacheSize: " + maxDiscoveryCacheSize);
            Tr.debug(tc, "maxDiscoverRetry: " + maxDiscoverRetry);
            Tr.debug(tc, "searchNumberOfUserInfoToMap: " + searchNumberOfUserInfoToMap);
            Tr.debug(tc, "maxAssociationAttempts: " + maxAssociationAttempts);
            Tr.debug(tc, "socketTimeout: " + socketTimeout);
            Tr.debug(tc, "sslRef: " + sslRef);
            Tr.debug(tc, "hostNameVerificationEnabled: " + hostNameVerificationEnabled);
            Tr.debug(tc, "userInfo: " + userInfo);
            Tr.debug(tc, "httpsRequired: " + httpsRequired);
            Tr.debug(tc, "authenticationMode: " + authenticationMode);
            Tr.debug(tc, "checkImmediate: " + checkImmediate);
            Tr.debug(tc, "sharedKeyEncryptionEnabled: " + sharedKeyEncryptionEnabled);
            Tr.debug(tc, "hashAlgorithm: " + hashAlgorithm);
            Tr.debug(tc, "sessionEncryptionType: " + sessionEncryptionType);
            Tr.debug(tc, "signatureAlgorithm: " + signatureAlgorithm);
            Tr.debug(tc, "groupIdentifier: " + groupIdentifier);
            Tr.debug(tc, "realmIdentifier: " + realmIdentifier);
            Tr.debug(tc, "encoding: " + characterEncoding);
            Tr.debug(tc, "includeUserInfoInSubject: " + includeUserInfoInSubject);
            Tr.debug(tc, "includeCustomCacheKeyInSubject: " + includeCustomCacheKeyInSubject);
            Tr.debug(tc, "providerIdentifier: " + providerIdentifier);
            Tr.debug(tc, "authFilterRef: " + authFilterRef);
            Tr.debug(tc, "authFilterId: " + authFilterId);
            Tr.debug(tc, "allowBasicAuthentication: " + allowBasicAuthentication);
            Tr.debug(tc, "tryOpenIDIfBasicAuthFails: " + tryOpenIDIfBasicAuthFails);
        }

        validateConfig();
    }

    private void setSessionEncryptionType(Boolean sharedKeyEnc, String hashAlgorithmValue) {
        if (sharedKeyEnc) {
            if (HASH_ALG_SHA1.equalsIgnoreCase(hashAlgorithmValue)) {
                sessionEncryptionType = ENCRYPTION_DH_SHA1;
            } else {
                sessionEncryptionType = ENCRYPTION_DH_SHA256;
            }
        } else {
            sessionEncryptionType = ENCRYPTION_NO;
        }
    }

    private void setSignatureAlgorithm(String hashAlgorithmValue) {
        if (HASH_ALG_SHA1.equalsIgnoreCase(hashAlgorithmValue)) {
            signatureAlgorithm = SIGNATURE_HMAC_SHA1;
        } else {
            signatureAlgorithm = SIGNATURE_HMAC_SHA256;
        }
    }

    private void validateConfig() {
        if (!allowStateless && maxAssociationAttempts == 0) {
            maxAssociationAttempts = 4;
            Tr.warning(tc, "OPENID_RP_CONFIG_DISABLED_ASSOCIATION_AND_NOT_ALLOW_STATELESS_INVALID");
        }
    }

    /**
     * @param props
     * @return
     */
    private List<UserInfo> processUserInfo(Map<String, Object> props, String key) {
        String[] pids = (String[]) props.get(key);
        if (pids == null || pids.length == 0) {
            return null;
        }

        List<String> ps = Collections.emptyList();
        ps = Arrays.asList(pids);
        return getUserInfoAttrs(ps);
    }

    private List<UserInfo> getUserInfoAttrs(List<String> pids) {

        List<UserInfo> uiList = new ArrayList<UserInfo>();

        ConfigurationAdmin configAdmin = configAdminRef.getServiceWithException();
        Configuration config = null;
        for (String pid : pids) {
            try {
                // We do not want to create a missing pid, only find one that we were told exists
                Configuration[] configList = configAdmin.listConfigurations(FilterUtils.createPropertyFilter("service.pid", pid));
                if (configList != null && configList.length > 0) {
                    //bind the config to this bundle so no one else can steal it
                    config = configAdmin.getConfiguration(pid, bundleLocation);
                }

            } catch (InvalidSyntaxException e) {
            } catch (IOException e) {
            }

            if (config != null) {
                Dictionary<String, ?> props = config.getProperties();
                String alias = (String) props.get(CFG_KEY_ALIAS);
                String type = (String) props.get(CFG_KEY_URI_TYPE);
                int count = (Integer) props.get(CFG_KEY_COUNT);
                boolean required = (Boolean) props.get(CFG_KEY_REQUIRED);
                UserInfo u = new UserInfo(alias, type, count, required);
                uiList.add(u);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            if (uiList.isEmpty()) {
                Tr.debug(tc, "there is no userInfo.");
            } else {
                for (UserInfo ui : uiList) {
                    Tr.debug(tc, "alias=" + ui.getAlias() + " type=" + ui.getType() + " count=" + ui.getCount() + " required=" + ui.getRequired());
                }
            }
        }
        return uiList;
    }

    /**
     * @return the allowOnlyTrustedProvider
     */
    public boolean getAllowStateless() {
        return allowStateless;
    }

    /**
     * @return the nonceValidTime
     */
    public long getNonceValidTime() {
        return nonceValidTime;
    }

    /**
     * @return the maxDiscoveryCacheSize
     */
    public int getMaxDiscoveryCacheSize() {
        return maxDiscoveryCacheSize;
    }

    /**
     * @return the nonceValidTime
     */
    public int getMaxAssociationAttemps() {
        return maxAssociationAttempts;
    }

    /**
     * @return the sessionEncryptionType
     */
    public String getSessionEncryptionType() {
        return sessionEncryptionType;
    }

    /**
     * @return the signatureAlgorithm
     */
    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    /**
     * @return the sslRef
     */
    public String getSslRef() {
        return sslRef;
    }

    /**
     * @return the userInfo
     */
    public List<UserInfo> getUserInfo() {
        return userInfo;
    }

    /**
     * @return the failedAssocExpire
     */
    public long getFailedAssocExpire() {
        return failedAssocExpire;
    }

    /**
     * @return the connectTimeout
     */
    public long getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * @return the socketTimeout
     */
    public long getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * @return the hostNameVerificationEnabled
     */
    public boolean isHostNameVerificationEnabled() {
        return hostNameVerificationEnabled;
    }

    /**
     * @return the httpsRequired
     */
    public boolean ishttpsRequired() {
        return httpsRequired;
    }

    /**
     * @return the checkImmediate
     */
    public boolean isCheckImmediate() {
        return checkImmediate;
    }

    /**
     * @return the identityAssertionEnabled
     */
    public boolean isMapIdentityToRegistryUser() {
        return mapIdentityToRegistryUser;
    }

    /**
     * @return the useClientIdentity
     */
    public boolean isUseClientIdentity() {
        return useClientIdentity;
    }

    /**
     * @return the searchNumberOfUserInfoToMap
     */
    public int getSearchNumberOfUserInfoToMap() {
        return searchNumberOfUserInfoToMap;
    }

    public int getMaxDiscoverRetry() {
        return maxDiscoverRetry;
    }

    public String getGroupIdentifier() {
        return groupIdentifier;
    }

    public String getRealmIdentifier() {
        return realmIdentifier;
    }

    public String getCharacterEncoding() {
        return characterEncoding;
    }

    public boolean isIncludeUserInfoInSubject() {
        return includeUserInfoInSubject;
    }

    public boolean isIncludeCustomCacheKeyInSubject() {
        return includeCustomCacheKeyInSubject;
    }

    /** {@inheritDoc} */
    @Override
    public String getProviderIdentifier() {
        return providerIdentifier;
    }

    /** {@inheritDoc} */
    @Override
    public String getAuthFilterId() {
        return authFilterId;
    }

    private String getAuthFilterId(String authFilterRef) {
        if (authFilterRef == null || authFilterRef.isEmpty())
            return null;
        Configuration config = null;
        ConfigurationAdmin configAdmin = configAdminRef.getService();
        try {
            if (configAdmin != null)
                config = configAdmin.getConfiguration(authFilterRef, bundleLocation);
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Invalid authFilterRef configuration", e.getMessage());
            }
            return null;
        }
        if (config == null)
            return null;
        Dictionary<String, Object> props = config.getProperties();
        if (props == null)
            return null;
        String id = (String) props.get(KEY_ID);
        return id;
    }

    @Override
    public boolean allowBasicAuthentication() {
        return allowBasicAuthentication;
    }

    @Override
    public boolean isTryOpenIDIfBasicAuthFails() {
        return tryOpenIDIfBasicAuthFails;
    }

}
