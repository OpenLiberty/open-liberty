/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.impl.model.security;

import java.util.Objects;

import org.eclipse.microprofile.openapi.models.security.OAuthFlow;
import org.eclipse.microprofile.openapi.models.security.Scopes;

/**
 * OAuthFlow
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.0.0-rc2/versions/3.0.md#oauthFlowsObject"
 */

public class OAuthFlowImpl implements OAuthFlow {
    private String authorizationUrl = null;
    private String tokenUrl = null;
    private String refreshUrl = null;
    private Scopes scopes = null;
    private java.util.Map<String, Object> extensions = null;

    @Override
    public String getAuthorizationUrl() {
        return authorizationUrl;
    }

    @Override
    public void setAuthorizationUrl(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
    }

    @Override
    public OAuthFlow authorizationUrl(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
        return this;
    }

    @Override
    public String getTokenUrl() {
        return tokenUrl;
    }

    @Override
    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    @Override
    public OAuthFlow tokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
        return this;
    }

    @Override
    public String getRefreshUrl() {
        return refreshUrl;
    }

    @Override
    public void setRefreshUrl(String refreshUrl) {
        this.refreshUrl = refreshUrl;
    }

    @Override
    public OAuthFlow refreshUrl(String refreshUrl) {
        this.refreshUrl = refreshUrl;
        return this;
    }

    @Override
    public Scopes getScopes() {
        return scopes;
    }

    @Override
    public void setScopes(Scopes scopes) {
        this.scopes = scopes;
    }

    @Override
    public OAuthFlow scopes(Scopes scopes) {
        this.scopes = scopes;
        return this;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OAuthFlowImpl oauthFlow = (OAuthFlowImpl) o;
        return Objects.equals(this.authorizationUrl, oauthFlow.authorizationUrl) &&
               Objects.equals(this.tokenUrl, oauthFlow.tokenUrl) &&
               Objects.equals(this.refreshUrl, oauthFlow.refreshUrl) &&
               Objects.equals(this.scopes, oauthFlow.scopes) &&
               Objects.equals(this.extensions, oauthFlow.extensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authorizationUrl, tokenUrl, refreshUrl, scopes, extensions);
    }

    @Override
    public java.util.Map<String, Object> getExtensions() {
        return extensions;
    }

    @Override
    public void addExtension(String name, Object value) {
        if (this.extensions == null) {
            this.extensions = new java.util.HashMap<>();
        }
        this.extensions.put(name, value);
    }

    @Override
    public void setExtensions(java.util.Map<String, Object> extensions) {
        this.extensions = extensions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class OAuthFlow {\n");

        sb.append("    authorizationUrl: ").append(toIndentedString(authorizationUrl)).append("\n");
        sb.append("    tokenUrl: ").append(toIndentedString(tokenUrl)).append("\n");
        sb.append("    refreshUrl: ").append(toIndentedString(refreshUrl)).append("\n");
        sb.append("    scopes: ").append(toIndentedString(scopes)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Converts the given object to string with each line indented by 4 spaces
     * (except the first line).
     * This method adds formatting to the general toString() method.
     *
     * @param o Java object to be represented as String
     * @return Formatted String representation of the object
     */

    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

}
