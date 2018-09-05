/*******************************************************************************
 * Copyright (c) 2016, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.tai;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.security.WebTrustAssociationFailedException;
import com.ibm.websphere.security.jwt.Claims;
import com.ibm.websphere.security.jwt.JwtToken;
import com.ibm.websphere.security.social.UserProfile;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.jwk.subject.mapping.AttributeToSubject;
import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.jwt.builder.utils.BuilderUtils;
import com.ibm.ws.security.openidconnect.clients.common.UserInfoHelper;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.utils.CacheToken;
import com.ibm.ws.security.social.internal.utils.ClientConstants;
import com.ibm.ws.security.social.internal.utils.SocialHashUtils;
import com.ibm.wsspi.security.tai.TAIResult;
import com.ibm.wsspi.security.token.AttributeNameConstants;

public class TAISubjectUtils {

    public static final TraceComponent tc = Tr.register(TAISubjectUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private static Cache tokenCache = new Cache(50000, 600000L); // TODO: Determine if cache settings should be configurable.;

    TAIWebUtils taiWebUtils = new TAIWebUtils();
    TAIEncryptionUtils taiEncryptionUtils = new TAIEncryptionUtils();

    private String username = null;
    @Sensitive
    private String accessToken = null;
    private JwtToken jwt = null;
    private JwtToken issuedJwt = null;
    @Sensitive
    private Map<String, Object> userApiResponseTokens = null;
    private String userApiResponse = null;
    private String userInfo = null;

    public TAISubjectUtils(AuthorizationCodeAuthenticator authzCodeAuthenticator) {
        this(authzCodeAuthenticator.getAccessToken(), authzCodeAuthenticator.getJwt(), authzCodeAuthenticator.getIssuedJwt(), authzCodeAuthenticator.getTokens(), authzCodeAuthenticator.getUserApiResponse());
    }

    public TAISubjectUtils(@Sensitive String accessToken, JwtToken jwt, JwtToken issuedJwt, @Sensitive Map<String, Object> userApiResponseTokens, String userApiResponse) {
        this.accessToken = accessToken;
        this.jwt = jwt;
        this.issuedJwt = issuedJwt;
        this.userApiResponseTokens = userApiResponseTokens;
        this.userApiResponse = userApiResponse;
    }
    
    /**
     * called by oidc client authentication if userInfo available
     * @param userInfo
     */
    void setUserInfo(String userInfo){
        this.userInfo = userInfo;
    }

    /**
     * Populates a series of custom properties based on the user API response tokens/string and JWT used to instantiate the
     * object, builds a subject using those custom properties as private credentials, and returns a TAIResult with the produced
     * username and subject.
     */
    @FFDCIgnore(SettingCustomPropertiesException.class)
    public TAIResult createResult(HttpServletResponse res, SocialLoginConfig clientConfig) throws WebTrustAssociationFailedException, SocialLoginException {
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        try {
            customProperties = setAllCustomProperties(clientConfig);
        } catch (SettingCustomPropertiesException e) {
            // Error occurred populating subject builder properties; any error should have already been logged, so just return the result
            return taiWebUtils.sendToErrorPage(res, TAIResult.create(HttpServletResponse.SC_UNAUTHORIZED));
        }
        Subject subject = buildSubject(clientConfig, customProperties);
        return TAIResult.create(HttpServletResponse.SC_OK, username, subject);
    }

    Hashtable<String, Object> setAllCustomProperties(SocialLoginConfig config) throws SettingCustomPropertiesException {
        Hashtable<String, Object> customProperties = setUsernameAndCustomProperties(config);
        if (username == null) {
            Tr.error(tc, "USERNAME_NOT_FOUND", new Object[0]);
            throw new SettingCustomPropertiesException();
        }
        if (accessToken == null) {
            Tr.error(tc, "ACCESS_TOKEN_MISSING", new Object[] { username });
            throw new SettingCustomPropertiesException();
        }
        customProperties.put(AttributeNameConstants.WSCREDENTIAL_SECURITYNAME, username);
        customProperties.put(ClientConstants.ACCESS_TOKEN, accessToken);
        return customProperties;
    }

    Hashtable<String, Object> setUsernameAndCustomProperties(SocialLoginConfig config) throws SettingCustomPropertiesException {
        if (userApiResponse != null) {
            return setUsernameAndCustomPropertiesUsingAttributeToSubjectMapping(config);
        } else {
            return setUsernameAndCustomPropertiesUsingJwt(config);
        }
    }

    Hashtable<String, Object> setUsernameAndCustomPropertiesUsingAttributeToSubjectMapping(SocialLoginConfig config) throws SettingCustomPropertiesException {
        AttributeToSubject attributeToSubject = createAttributeToSubject(config);
        username = attributeToSubject.getMappedUser();

        if (!config.getMapToUserRegistry()) {
            return createCustomPropertiesFromSubjectMapping(config, attributeToSubject);
        }
        return new Hashtable<String, Object>();
    }

    AttributeToSubject createAttributeToSubject(SocialLoginConfig config) {
        return new AttributeToSubject(userApiResponse, config.getUserNameAttribute(),
                config.getUserUniqueIdAttribute(), config.getRealmName(), config.getRealmNameAttribute(),
                config.getGroupNameAttribute(), config.getMapToUserRegistry(), config.getUserApiResponseIdentifier());
    }

    Hashtable<String, Object> createCustomPropertiesFromSubjectMapping(SocialLoginConfig config, AttributeToSubject attributeToSubject) throws SettingCustomPropertiesException {
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();

        String realm = getRealm(attributeToSubject, config);
        String uniqueID = getUserAccessId(attributeToSubject, realm);

        customProperties.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, uniqueID);
        if (realm != null && !realm.isEmpty()) {
            customProperties.put(AttributeNameConstants.WSCREDENTIAL_REALM, realm);
        }
        List<String> groupsWithRealm = getGroupsListWithRealm(attributeToSubject, realm);
        if (!groupsWithRealm.isEmpty()) {
            customProperties.put(AttributeNameConstants.WSCREDENTIAL_GROUPS, groupsWithRealm);
        }
        return customProperties;
    }

    Hashtable<String, Object> setUsernameAndCustomPropertiesUsingJwt(SocialLoginConfig config) throws SettingCustomPropertiesException {
        // No user API response provided, so try to get a user name from the JWT
        setUserNameFromJwtClaims(config);
        if (username != null && !config.getMapToUserRegistry()) {
            return createCustomPropertiesFromConfig(config);
        }
        return new Hashtable<String, Object>();
    }

    void setUserNameFromJwtClaims(SocialLoginConfig config) {
        if (jwt != null) {
            Claims claims = jwt.getClaims();
            if (claims != null) {
                username = (String) claims.get(config.getUserNameAttribute());
            }
        }
    }

    Hashtable<String, Object> createCustomPropertiesFromConfig(SocialLoginConfig config) throws SettingCustomPropertiesException {
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        String realm = getRealm(config);
        customProperties.put(AttributeNameConstants.WSCREDENTIAL_REALM, realm);
        String uniqueID = "user:" + realm + "/" + username;
        customProperties.put(AttributeNameConstants.WSCREDENTIAL_UNIQUEID, uniqueID);
        return customProperties;
    }

    String getRealm(AttributeToSubject attributeToSubject, SocialLoginConfig config) throws SettingCustomPropertiesException {
        String realm = attributeToSubject.getMappedRealm();
        if (realm == null) {
            realm = getDefaultRealmFromAuthorizationEndpoint(config);
        }
        return realm;
    }

    String getRealm(SocialLoginConfig config) throws SettingCustomPropertiesException {
        String realm = config.getRealmName();
        if (realm == null) {
            realm = getDefaultRealmFromAuthorizationEndpoint(config);
        }
        return realm;
    }

    String getDefaultRealmFromAuthorizationEndpoint(SocialLoginConfig config) throws SettingCustomPropertiesException {
        String authzEndpoint = getAuthorizationEndpoint(config);
        if (!isValidAuthorizationEndpoint(authzEndpoint)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Authorization endpoint [" + authzEndpoint + "] is either empty or too short to be a valid URL");
            }
            Tr.error(tc, "REALM_NOT_FOUND");
            throw new SettingCustomPropertiesException();
        }
        return extractRealmFromAuthorizationEndpoint(authzEndpoint);
    }

    Subject buildSubject(SocialLoginConfig config, Hashtable<String, Object> customProperties) throws SocialLoginException {
        Subject subject = new Subject();
        if (jwt != null) {
            subject.getPrivateCredentials().add(jwt);
        }
        if (issuedJwt != null) {
            customProperties.put(ClientConstants.ISSUED_JWT_TOKEN, issuedJwt.compact());
        }
        subject.getPrivateCredentials().add(customProperties);

        String encryptedAccessToken = null;
        String accessTokenAlias = null;

        UserProfile userProfile = createUserProfile(config);
        if (userProfile != null) {
            encryptedAccessToken = userProfile.getEncryptedAccessToken();
            accessTokenAlias = userProfile.getAccessTokenAlias();
            subject.getPrivateCredentials().add(userProfile);
        }
        CacheToken cacheToken = createCacheToken(config);
        if (encryptedAccessToken != null) {
            tokenCache.put(encryptedAccessToken, cacheToken);
        }
        if (accessTokenAlias != null) {
            tokenCache.put(accessTokenAlias, cacheToken);
        }
        return subject;
    }

    @FFDCIgnore(Exception.class)
    UserProfile createUserProfile(SocialLoginConfig config) throws SocialLoginException {
        Hashtable<String, Object> customProperties = createCustomProperties(config, true);
        Claims claims = null;
        try {
            BuilderUtils builderUtils = new BuilderUtils();
            claims = builderUtils.parseJwtForClaims(userApiResponse);
        } catch (Exception e) {

        }
        
        return new UserProfile(jwt, customProperties, claims, userInfo);
    }

    Hashtable<String, Object> createCustomProperties(SocialLoginConfig config, boolean getRefreshAndIdTokens) throws SocialLoginException {
        if (userApiResponseTokens == null) {
            // No tokens provided means no access token
            throw new SocialLoginException("SOCIAL_LOGIN_RESULT_MISSING_ACCESS_TOKEN", null, new Object[0]);
        }
        Hashtable<String, Object> customProperties = new Hashtable<String, Object>();
        String accessToken = getAccessTokenAndAddCustomProp(customProperties);
        if (getRefreshAndIdTokens) {
            // TODO: Use a separate Map to build UserProfile properties
            addRefreshTokenCustomProp(customProperties);
            addIdTokenCustomProp(customProperties);
        }
        addAccessTokenLifetimeCustomProp(customProperties);
        addSocialMediaNameCustomProp(customProperties, config);
        addScopeCustomProp(customProperties, config);
        addEncryptedAccessTokenCustomProp(customProperties, config, accessToken);
        addAccessTokenAliasCustomProp(customProperties, accessToken);
        return customProperties;
    }

    String getAccessTokenAndAddCustomProp(Hashtable<String, Object> customProperties) throws SocialLoginException {
        String accessToken = (String) userApiResponseTokens.get(ClientConstants.ACCESS_TOKEN);
        if (accessToken == null) {
            throw new SocialLoginException("SOCIAL_LOGIN_RESULT_MISSING_ACCESS_TOKEN", null, new Object[0]);
        }
        customProperties.put(ClientConstants.ACCESS_TOKEN, accessToken);
        return accessToken;
    }

    void addRefreshTokenCustomProp(Hashtable<String, Object> customProperties) {
        String key = ClientConstants.REFRESH_TOKEN;
        String refreshToken = (String) userApiResponseTokens.get(key);
        addNonNullNonEmptyCustomProperty(customProperties, key, refreshToken);
    }

    void addIdTokenCustomProp(Hashtable<String, Object> customProperties) {
        String key = ClientConstants.ID_TOKEN;
        String idToken = (String) userApiResponseTokens.get(key);
        addNonNullNonEmptyCustomProperty(customProperties, key, idToken);
    }

    void addAccessTokenLifetimeCustomProp(Hashtable<String, Object> customProperties) {
        String key = ClientConstants.EXPIRES_IN;
        // TODO - Is this value required to be a long?
        Long accessTokenLifeTime = (Long) userApiResponseTokens.get(key);
        if (accessTokenLifeTime != null) {
            customProperties.put(key, accessTokenLifeTime);
        }
    }

    void addSocialMediaNameCustomProp(Hashtable<String, Object> customProperties, SocialLoginConfig config) {
        String socialMediaName = config.getUniqueId();
        addNonNullNonEmptyCustomProperty(customProperties, ClientConstants.SOCIAL_MEDIA, socialMediaName);
    }

    void addScopeCustomProp(Hashtable<String, Object> customProperties, SocialLoginConfig config) {
        String key = ClientConstants.SCOPE;
        String scope = (String) userApiResponseTokens.get(key);
        if (!addNonNullNonEmptyCustomProperty(customProperties, key, scope)) {
            scope = config.getScope();
            if (scope != null) {
                addNonNullNonEmptyCustomProperty(customProperties, key, scope);
            }
        }
    }

    @FFDCIgnore(SocialLoginException.class)
    void addEncryptedAccessTokenCustomProp(Hashtable<String, Object> customProperties, SocialLoginConfig config, String accessToken) throws SocialLoginException {
        String encryptedAccessToken = null;
        try {
            encryptedAccessToken = taiEncryptionUtils.getEncryptedAccessToken(config, accessToken);
        } catch (SocialLoginException e) {
            throw new SocialLoginException("ERROR_GETTING_ENCRYPTED_ACCESS_TOKEN", e, new Object[] { config.getUniqueId(), e.getLocalizedMessage() });
        }
        addNonNullNonEmptyCustomProperty(customProperties, ClientConstants.ENCRYPTED_TOKEN, encryptedAccessToken);
    }

    void addAccessTokenAliasCustomProp(Hashtable<String, Object> customProperties, String accessToken) {
        String accessTokenAlias = SocialHashUtils.digest(accessToken);
        addNonNullNonEmptyCustomProperty(customProperties, ClientConstants.ACCESS_TOKEN_ALIAS, accessTokenAlias);
    }

    CacheToken createCacheToken(SocialLoginConfig config) {
        CacheToken cacheToken = new CacheToken(accessToken, config.getUniqueId());
        String idToken = (userApiResponseTokens != null) ? (String) userApiResponseTokens.get(ClientConstants.ID_TOKEN) : null;
        if (idToken != null && !idToken.trim().isEmpty()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caching ID token");
            }
            cacheToken.setIdToken(idToken);
        }
        return cacheToken;
    }

    private String getUserAccessId(AttributeToSubject attributeToSubject, String realm) {
        String uniqueUser = attributeToSubject.getMappedUniqueUser();
        return new StringBuffer("user:").append(realm).append("/").append(uniqueUser).toString();
    }

    private List<String> getGroupsListWithRealm(AttributeToSubject attributeToSubject, String realm) {
        ArrayList<String> groupsWithRealm = new ArrayList<String>();
        ArrayList<String> groups = attributeToSubject.getMappedGroups();
        if (groups != null && !groups.isEmpty()) {
            Iterator<String> it = groups.iterator();
            while (it.hasNext()) {
                String group = "group:" + realm + "/" + it.next();
                groupsWithRealm.add(group);
            }
        }
        return groupsWithRealm;
    }

    @FFDCIgnore(SocialLoginException.class)
    private String getAuthorizationEndpoint(SocialLoginConfig config) {
        try {
            return taiWebUtils.getAuthorizationEndpoint(config);
        } catch (SocialLoginException e) {
            e.logErrorMessage();
            return null;
        }
    }

    private boolean isValidAuthorizationEndpoint(String authzEndpoint) {
        int httpsStrLen = "https://".length();
        return authzEndpoint != null && !authzEndpoint.isEmpty() && authzEndpoint.length() > httpsStrLen;
    }

    private String extractRealmFromAuthorizationEndpoint(String authzEndpoint) {
        // Assumes that the authorization endpoint must start with "https://"
        int httpsStrLen = "https://".length();
        String endpointWithSchemeRemoved = authzEndpoint.substring(httpsStrLen);
        int firstSlashIndex = endpointWithSchemeRemoved.indexOf("/", 0);
        String realm = null;
        if (firstSlashIndex > 0) {
            realm = authzEndpoint.substring(0, firstSlashIndex + httpsStrLen);
        } else {
            realm = authzEndpoint;
        }
        return realm;
    }

    private boolean addNonNullNonEmptyCustomProperty(Hashtable<String, Object> customProperties, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            customProperties.put(key, value);
            return true;
        }
        return false;
    }

    /**
     * Exception class used to track simple errors that don't need to be logged when setting the custom properties for the
     * subject.
     */
    static class SettingCustomPropertiesException extends Exception {
        private static final long serialVersionUID = 1L;
    }

}
