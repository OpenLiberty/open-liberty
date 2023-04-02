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
 * <li>oidcLogin</li>
 * </ul>
 */
public class OidcLogin extends ConfigElement {

    private String authFilterRef;
    private String authorizationEndpoint;
    private String clientId;
    private String clientSecret;
    private String discoveryEndpoint;
    private String discoveryPollingRate;
    private String displayName;
    private String groupNameAttribute;
    private Boolean hostNameVerificationEnabled;
    private Boolean isClientSideRedirectSupported;
    private String issuer;
    private String jwkClientId;
    private String jwkClientSecret;
    private String jwksUri;
    private String keyManagementKeyAlias;
    private Boolean mapToUserRegistry;
    private String realmNameAttribute;
    private String redirectToRPHostAndPort;
    private String responseType;
    private String scope;
    private String sslRef;
    private String tokenEndpoint;
    private String tokenEndpointAuthMethod;
    private String trustAliasName;
    private Boolean useSystemPropertiesForHttpClientConnections;
    private String userInfoEndpoint;
    private Boolean userInfoEndpointEnabled;
    private String userNameAttribute;
    private String userUniqueIdAttribute;
    private String website;
    private Boolean userApiNeedsSpecialHeader; // INTERNAL
    private Jwt jwt;
    private TokenParameter tokenParameter;

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
     * @return the discoveryEndpoint
     */
    public String getDiscoveryEndpoint() {
        return discoveryEndpoint;
    }

    /**
     * @param discoveryEndpoint the discoveryEndpoint to set
     */
    @XmlAttribute(name = "discoveryEndpoint")
    public void setDiscoveryEndpoint(String discoveryEndpoint) {
        this.discoveryEndpoint = discoveryEndpoint;
    }

    /**
     * @return the discoveryPollingRate
     */
    public String getDiscoveryPollingRate() {
        return discoveryPollingRate;
    }

    /**
     * @param discoveryPollingRate the discoveryPollingRate to set
     */
    @XmlAttribute(name = "discoveryPollingRate")
    public void setDiscoveryPollingRate(String discoveryPollingRate) {
        this.discoveryPollingRate = discoveryPollingRate;
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
     * @param isClientSideRedirectSupported the isClientSideRedirectSupported to set
     */
    @XmlAttribute(name = "isClientSideRedirectSupported")
    public void setIsClientSideRedirectSupported(Boolean isClientSideRedirectSupported) {
        this.isClientSideRedirectSupported = isClientSideRedirectSupported;
    }

    /**
     * @return the hostNameVerificationEnabled
     */
    public Boolean getHostNameVerificationEnabled() {
        return hostNameVerificationEnabled;
    }

    /**
     * @param hostNameVerificationEnabled the hostNameVerificationEnabled to set
     */
    @XmlAttribute(name = "hostNameVerificationEnabled")
    public void setHostNameVerificationEnabled(Boolean hostNameVerificationEnabled) {
        this.hostNameVerificationEnabled = hostNameVerificationEnabled;
    }

    /**
     * @return the issuer
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * @param issuer the issuer to set
     */
    @XmlAttribute(name = "issuer")
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * @return the jwkClientId
     */
    public String getJwkClientId() {
        return jwkClientId;
    }

    /**
     * @param jwkClientId the jwkClientId to set
     */
    @XmlAttribute(name = "jwkClientId")
    public void setJwkClientId(String jwkClientId) {
        this.jwkClientId = jwkClientId;
    }

    /**
     * @return the jwkClientSecret
     */
    public String getJwkClientSecret() {
        return jwkClientSecret;
    }

    /**
     * @param jwkClientSecret the jwkClientSecret to set
     */
    @XmlAttribute(name = "jwkClientSecret")
    public void setJwkClientSecret(String jwkClientSecret) {
        this.jwkClientSecret = jwkClientSecret;
    }

    /**
     * @return the jwksUri
     */
    public String getJwksUri() {
        return jwksUri;
    }

    /**
     * @param jwksUri the jwksUri to set
     */
    @XmlAttribute(name = "jwksUri")
    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
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
     * @return the keyManagementKeyAlias
     */
    public String getKeyManagementKeyAlias() {
        return keyManagementKeyAlias;
    }

    /**
     * @param keyManagementKeyAlias the keyManagementKeyAlias to set
     */
    @XmlAttribute(name = "keyManagementKeyAlias")
    public void setKeyManagementKeyAlias(String keyManagementKeyAlias) {
        this.keyManagementKeyAlias = keyManagementKeyAlias;
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
     * @return the tokenParameter
     */
    public TokenParameter getTokenParameter() {
        return tokenParameter;
    }

    /**
     * @param tokenParameter the tokenParameter to set
     */
    @XmlElement(name = "tokenParameter")
    public void setTokenParameter(TokenParameter tokenParameter) {
        this.tokenParameter = tokenParameter;
    }

    /**
     * @return the trustAliasName
     */
    public String getTrustAliasName() {
        return trustAliasName;
    }

    /**
     * @param trustAliasName the trustAliasName to set
     */
    @XmlAttribute(name = "trustAliasName")
    public void setTrustAliasName(String trustAliasName) {
        this.trustAliasName = trustAliasName;
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

    /**
     * @return the userInfoEndpoint
     */
    public String getUserInfoEndpoint() {
        return userInfoEndpoint;
    }

    /**
     * @param userInfoEndpoint the userInfoEndpoint to set
     */
    @XmlAttribute(name = "userInfoEndpoint")
    public void setUserInfoEndpoint(String userInfoEndpoint) {
        this.userInfoEndpoint = userInfoEndpoint;
    }

    /**
     * @return the userInfoEndpointEnabled
     */
    public Boolean getUserInfoEndpointEnabled() {
        return userInfoEndpointEnabled;
    }

    /**
     * @param userInfoEndpointEnabled the userInfoEndpointEnabled to set
     */
    @XmlAttribute(name = "userInfoEndpointEnabled")
    public void setUserInfoEndpointEnabled(Boolean userInfoEndpointEnabled) {
        this.userInfoEndpointEnabled = userInfoEndpointEnabled;
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

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append(getClass().getSimpleName()).append("{ ");

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
        if (discoveryEndpoint != null) {
            sb.append("discoveryEndpoint=\"").append(discoveryEndpoint).append("\" ");
        }
        if (discoveryPollingRate != null) {
            sb.append("discoveryPollingRate=\"").append(discoveryPollingRate).append("\" ");
        }
        if (displayName != null) {
            sb.append("displayName=\"").append(displayName).append("\" ");
        }
        if (groupNameAttribute != null) {
            sb.append("groupNameAttribute=\"").append(groupNameAttribute).append("\" ");
        }
        if (hostNameVerificationEnabled != null) {
            sb.append("hostNameVerificationEnabled=\"").append(hostNameVerificationEnabled).append("\" ");
        }
        if (isClientSideRedirectSupported != null) {
            sb.append("isClientSideRedirectSupported=\"").append(isClientSideRedirectSupported).append("\" ");
        }
        if (issuer != null) {
            sb.append("issuer=\"").append(issuer).append("\" ");
        }
        if (jwkClientId != null) {
            sb.append("jwkClientId=\"").append(jwkClientId).append("\" ");
        }
        if (jwkClientSecret != null) {
            sb.append("jwkClientSecret=\"").append(jwkClientSecret).append("\" ");
        }
        if (jwksUri != null) {
            sb.append("jwksUri=\"").append(jwksUri).append("\" ");
        }
        if (jwt != null) {
            sb.append("jwt=\"").append(jwt).append("\" ");
        }
        if (keyManagementKeyAlias != null) {
            sb.append("keyManagementKeyAlias=\"").append(keyManagementKeyAlias).append("\" ");
        }
        if (mapToUserRegistry != null) {
            sb.append("mapToUserRegistry=\"").append(mapToUserRegistry).append("\" ");
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
        if (tokenParameter != null) {
            sb.append("tokenParameter=\"").append(tokenParameter).append("\" ");
        }
        if (trustAliasName != null) {
            sb.append("trustAliasName=\"").append(trustAliasName).append("\" ");
        }
        if (userApiNeedsSpecialHeader != null) {
            sb.append("userApiNeedsSpecialHeader=\"").append(userApiNeedsSpecialHeader).append("\" ");
        }
        if (userInfoEndpointEnabled != null) {
            sb.append("userInfoEndpointEnabled=\"").append(userInfoEndpointEnabled).append("\" ");
        }
        if (userInfoEndpoint != null) {
            sb.append("userInfoEndpoint=\"").append(userInfoEndpoint).append("\" ");
        }
        if (userNameAttribute != null) {
            sb.append("userNameAttribute=\"").append(userNameAttribute).append("\" ");
        }
        if (userUniqueIdAttribute != null) {
            sb.append("userUniqueIdAttribute=\"").append(userUniqueIdAttribute).append("\" ");
        }
        if (useSystemPropertiesForHttpClientConnections != null) {
            sb.append("useSystemPropertiesForHttpClientConnections=\"").append(useSystemPropertiesForHttpClientConnections).append("\" ");
        }
        if (website != null) {
            sb.append("website=\"").append(website).append("\" ");
        }

        sb.append("}");

        return sb.toString();
    }
}
