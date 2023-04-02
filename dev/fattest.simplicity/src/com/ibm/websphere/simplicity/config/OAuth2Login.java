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
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

/**
 * Configuration for the following top-level elements:
 *
 * <ul>
 * <li>oauth2Login</li>
 * </ul>
 */
public class OAuth2Login extends ConfigElement {

    private String accessTokenHeaderName;
    private Boolean accessTokenRequired;
    private Boolean accessTokenSupported;
    private String authFilterRef;
    private String authorizationEndpoint;
    private String clientId;
    private String clientSecret;
    private String displayName;
    private String groupNameAttribute;
    private Boolean isClientSideRedirectSupported;
    private Boolean mapToUserRegistry;
    private String realmName;
    private String realmNameAttribute;
    private String redirectToRPHostAndPort;
    private String responseType;
    private String scope;
    private String sslRef;
    private String tokenEndpoint;
    private String tokenEndpointAuthMethod;
    private Boolean useSystemPropertiesForHttpClientConnections;
    private String userApi;
    private String userApiToken;
    private String userApiType;
    private String userNameAttribute;
    private String userUniqueIdAttribute;
    private String website;
    private Boolean userApiNeedsSpecialHeader; // INTERNAL
    private Jwt jwt;

    /**
     * @return the accessTokenHeaderName
     */
    public String getAccessTokenHeaderName() {
        return accessTokenHeaderName;
    }

    /**
     * @param accessTokenHeaderName the accessTokenHeaderName to set
     */
    @XmlAttribute(name = "accessTokenHeaderName")
    public void setAccessTokenHeaderName(String accessTokenHeaderName) {
        this.accessTokenHeaderName = accessTokenHeaderName;
    }

    /**
     * @return the accessTokenRequired
     */
    public Boolean getAccessTokenRequired() {
        return accessTokenRequired;
    }

    /**
     * @param accessTokenRequired the accessTokenRequired to set
     */
    @XmlAttribute(name = "accessTokenRequired")
    public void setAccessTokenRequired(Boolean accessTokenRequired) {
        this.accessTokenRequired = accessTokenRequired;
    }

    /**
     * @return the accessTokenSupported
     */
    public Boolean getAccessTokenSupported() {
        return accessTokenSupported;
    }

    /**
     * @param accessTokenSupported the accessTokenSupported to set
     */
    @XmlAttribute(name = "accessTokenSupported")
    public void setAccessTokenSupported(Boolean accessTokenSupported) {
        this.accessTokenSupported = accessTokenSupported;
    }

    /**
     * @return the authFilterRef
     */
    public String getAuthFilterRef() {
        return authFilterRef;
    }

    /**
     * @param authFilterRef the authFilterRef to set
     */
    @XmlAttribute(name = "authFilterRef")
    public void setAuthFilterRef(String authFilterRef) {
        this.authFilterRef = authFilterRef;
    }

    /**
     * @return the authorizationEndpoint
     */
    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    /**
     * @param authorizationEndpoint the authorizationEndpoint to set
     */
    @XmlAttribute(name = "authorizationEndpoint")
    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    /**
     * @return the clientId
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * @param clientId the clientId to set
     */
    @XmlAttribute(name = "clientId")
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * @return the clientSecret
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * @param clientSecret the clientSecret to set
     */
    @XmlAttribute(name = "clientSecret")
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /**
     * @return the displayName
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @param displayName the displayName to set
     */
    @XmlAttribute(name = "displayName")
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * @return the groupNameAttribute
     */
    public String getGroupNameAttribute() {
        return groupNameAttribute;
    }

    /**
     * @param groupNameAttribute the groupNameAttribute to set
     */
    @XmlAttribute(name = "groupNameAttribute")
    public void setGroupNameAttribute(String groupNameAttribute) {
        this.groupNameAttribute = groupNameAttribute;
    }

    /**
     * @return the isClientSideRedirectSupported
     */
    public Boolean getIsClientSideRedirectSupported() {
        return isClientSideRedirectSupported;
    }

    /**
     * @return the jwt
     */
    public Jwt getJwt() {
        return jwt;
    }

    /**
     * @param jwt the jwt to set
     */
    @XmlElement(name = "jwt")
    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    /**
     * @param isClientSideRedirectSupported the isClientSideRedirectSupported to set
     */
    @XmlAttribute(name = "isClientSideRedirectSupported")
    public void setIsClientSideRedirectSupported(Boolean isClientSideRedirectSupported) {
        this.isClientSideRedirectSupported = isClientSideRedirectSupported;
    }

    /**
     * @return the mapToUserRegistry
     */
    public Boolean getMapToUserRegistry() {
        return mapToUserRegistry;
    }

    /**
     * @param mapToUserRegistry the mapToUserRegistry to set
     */
    @XmlAttribute(name = "mapToUserRegistry")
    public void setMapToUserRegistry(Boolean mapToUserRegistry) {
        this.mapToUserRegistry = mapToUserRegistry;
    }

    /**
     * @return the realmName
     */
    public String getRealmName() {
        return realmName;
    }

    /**
     * @param realmName the realmName to set
     */
    @XmlAttribute(name = "realmName")
    public void setRealmName(String realmName) {
        this.realmName = realmName;
    }

    /**
     * @return the realmNameAttribute
     */
    public String getRealmNameAttribute() {
        return realmNameAttribute;
    }

    /**
     * @param realmNameAttribute the realmNameAttribute to set
     */
    @XmlAttribute(name = "realmNameAttribute")
    public void setRealmNameAttribute(String realmNameAttribute) {
        this.realmNameAttribute = realmNameAttribute;
    }

    /**
     * @return the redirectToRPHostAndPort
     */
    public String getRedirectToRPHostAndPort() {
        return redirectToRPHostAndPort;
    }

    /**
     * @param redirectToRPHostAndPort the redirectToRPHostAndPort to set
     */
    @XmlAttribute(name = "redirectToRPHostAndPort")
    public void setRedirectToRPHostAndPort(String redirectToRPHostAndPort) {
        this.redirectToRPHostAndPort = redirectToRPHostAndPort;
    }

    /**
     * @return the responseType
     */
    public String getResponseType() {
        return responseType;
    }

    /**
     * @param responseType the responseType to set
     */
    @XmlAttribute(name = "responseType")
    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    /**
     * @return the scope
     */
    public String getScope() {
        return scope;
    }

    /**
     * @param scope the scope to set
     */
    @XmlAttribute(name = "scope")
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * @return the sslRef
     */
    public String getSslRef() {
        return sslRef;
    }

    /**
     * @param sslRef the sslRef to set
     */
    @XmlAttribute(name = "sslRef")
    public void setSslRef(String sslRef) {
        this.sslRef = sslRef;
    }

    /**
     * @return the tokenEndpoint
     */
    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    /**
     * @param tokenEndpoint the tokenEndpoint to set
     */
    @XmlAttribute(name = "tokenEndpoint")
    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    /**
     * @return the tokenEndpointAuthMethod
     */
    public String getTokenEndpointAuthMethod() {
        return tokenEndpointAuthMethod;
    }

    /**
     * @param tokenEndpointAuthMethod the tokenEndpointAuthMethod to set
     */
    @XmlAttribute(name = "tokenEndpointAuthMethod")
    public void setTokenEndpointAuthMethod(String tokenEndpointAuthMethod) {
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
    }

    /**
     * @return the useSystemPropertiesForHttpClientConnections
     */
    public Boolean getUseSystemPropertiesForHttpClientConnections() {
        return useSystemPropertiesForHttpClientConnections;
    }

    /**
     * @param useSystemPropertiesForHttpClientConnections the useSystemPropertiesForHttpClientConnections to set
     */
    @XmlAttribute(name = "useSystemPropertiesForHttpClientConnections")
    public void setUseSystemPropertiesForHttpClientConnections(Boolean useSystemPropertiesForHttpClientConnections) {
        this.useSystemPropertiesForHttpClientConnections = useSystemPropertiesForHttpClientConnections;
    }

    /**
     * @return the userApi
     */
    public String getUserApi() {
        return userApi;
    }

    /**
     * @param userApi the userApi to set
     */
    @XmlAttribute(name = "userApi")
    public void setUserApi(String userApi) {
        this.userApi = userApi;
    }

    /**
     * @return the userApiToken
     */
    public String getUserApiToken() {
        return userApiToken;
    }

    /**
     * @param userApiToken the userApiToken to set
     */
    @XmlAttribute(name = "userApiToken")
    public void setUserApiToken(String userApiToken) {
        this.userApiToken = userApiToken;
    }

    /**
     * @return the userApiType
     */
    public String getUserApiType() {
        return userApiType;
    }

    /**
     * @param userApiType the userApiType to set
     */
    @XmlAttribute(name = "userApiType")
    public void setUserApiType(String userApiType) {
        this.userApiType = userApiType;
    }

    /**
     * @return the userNameAttribute
     */
    public String getUserNameAttribute() {
        return userNameAttribute;
    }

    /**
     * @param userNameAttribute the userNameAttribute to set
     */
    @XmlAttribute(name = "userNameAttribute")
    public void setUserNameAttribute(String userNameAttribute) {
        this.userNameAttribute = userNameAttribute;
    }

    /**
     * @return the userUniqueIdAttribute
     */
    public String getUserUniqueIdAttribute() {
        return userUniqueIdAttribute;
    }

    /**
     * @param userUniqueIdAttribute the userUniqueIdAttribute to set
     */
    @XmlAttribute(name = "userUniqueIdAttribute")
    public void setUserUniqueIdAttribute(String userUniqueIdAttribute) {
        this.userUniqueIdAttribute = userUniqueIdAttribute;
    }

    /**
     * @return the website
     */
    public String getWebsite() {
        return website;
    }

    /**
     * @param website the website to set
     */
    @XmlAttribute(name = "website")
    public void setWebsite(String website) {
        this.website = website;
    }

    /**
     * @return the userApiNeedsSpecialHeader
     */
    public Boolean getUserApiNeedsSpecialHeader() {
        return userApiNeedsSpecialHeader;
    }

    /**
     * @param userApiNeedsSpecialHeader the userApiNeedsSpecialHeader to set
     */
    @XmlAttribute(name = "userApiNeedsSpecialHeader")
    public void setUserApiNeedsSpecialHeader(Boolean userApiNeedsSpecialHeader) {
        this.userApiNeedsSpecialHeader = userApiNeedsSpecialHeader;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

        if (accessTokenHeaderName != null) {
            sb.append("accessTokenHeaderName=\"").append(accessTokenHeaderName).append("\" ");
        }
        if (accessTokenRequired != null) {
            sb.append("accessTokenRequired=\"").append(accessTokenRequired).append("\" ");
        }
        if (accessTokenSupported != null) {
            sb.append("accessTokenSupported=\"").append(accessTokenSupported).append("\" ");
        }
        if (authFilterRef != null) {
            sb.append("authFilterRef=\"").append(authFilterRef).append("\" ");
        }
        if (authorizationEndpoint != null) {
            sb.append("authorizationEndpoint=\"").append(authorizationEndpoint).append("\" ");
        }
        if (clientId != null) {
            sb.append("clientId=\"").append(clientId).append("\" ");
        }
        if (clientSecret != null) {
            sb.append("clientSecret=\"").append(clientSecret).append("\" ");
        }
        if (displayName != null) {
            sb.append("displayName=\"").append(displayName).append("\" ");
        }
        if (groupNameAttribute != null) {
            sb.append("groupNameAttribute=\"").append(groupNameAttribute).append("\" ");
        }
        if (isClientSideRedirectSupported != null) {
            sb.append("isClientSideRedirectSupported=\"").append(isClientSideRedirectSupported).append("\" ");
        }
        if (jwt != null) {
            sb.append("jwt=\"").append(jwt).append("\" ");
        }
        if (mapToUserRegistry != null) {
            sb.append("mapToUserRegistry=\"").append(mapToUserRegistry).append("\" ");
        }
        if (realmName != null) {
            sb.append("realmName=\"").append(realmName).append("\" ");
        }
        if (realmNameAttribute != null) {
            sb.append("realmNameAttribute=\"").append(realmNameAttribute).append("\" ");
        }
        if (redirectToRPHostAndPort != null) {
            sb.append("redirectToRPHostAndPort=\"").append(redirectToRPHostAndPort).append("\" ");
        }
        if (responseType != null) {
            sb.append("responseType=\"").append(responseType).append("\" ");
        }
        if (scope != null) {
            sb.append("scope=\"").append(scope).append("\" ");
        }
        if (sslRef != null) {
            sb.append("sslRef=\"").append(sslRef).append("\" ");
        }
        if (tokenEndpoint != null) {
            sb.append("tokenEndpoint=\"").append(tokenEndpoint).append("\" ");
        }
        if (tokenEndpointAuthMethod != null) {
            sb.append("tokenEndpointAuthMethod=\"").append(tokenEndpointAuthMethod).append("\" ");
        }
        if (useSystemPropertiesForHttpClientConnections != null) {
            sb.append("useSystemPropertiesForHttpClientConnections=\"").append(useSystemPropertiesForHttpClientConnections).append("\" ");
        }
        if (userApi != null) {
            sb.append("userApi=\"").append(userApi).append("\" ");
        }
        if (userApiToken != null) {
            sb.append("userApiToken=\"").append(userApiToken).append("\" ");
        }
        if (userApiType != null) {
            sb.append("userApiType=\"").append(userApiType).append("\" ");
        }
        if (userNameAttribute != null) {
            sb.append("userNameAttribute=\"").append(userNameAttribute).append("\" ");
        }
        if (userUniqueIdAttribute != null) {
            sb.append("userUniqueIdAttribute=\"").append(userUniqueIdAttribute).append("\" ");
        }
        if (userApiNeedsSpecialHeader != null) {
            sb.append("userApiNeedsSpecialHeader=\"").append(userApiNeedsSpecialHeader).append("\" ");
        }
        if (website != null) {
            sb.append("website=\"").append(website).append("\" ");
        }

        sb.append("}");

        return sb.toString();
    }
}
