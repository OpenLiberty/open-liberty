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
import org.eclipse.microprofile.openapi.models.security.OAuthFlows;

import com.ibm.ws.microprofile.openapi.Constants;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;

/**
 * OAuthFlows
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.0.0-rc2/versions/3.0.md#oauthFlowsObject"
 */

public class OAuthFlowsImpl implements OAuthFlows {
    private OAuthFlow implicit = null;
    private OAuthFlow password = null;
    private OAuthFlow clientCredentials = null;
    private OAuthFlow authorizationCode = null;
    private java.util.Map<String, Object> extensions = null;

    @Override
    public OAuthFlow getImplicit() {
        return implicit;
    }

    @Override
    public void setImplicit(OAuthFlow implicit) {
        this.implicit = implicit;
    }

    @Override
    public OAuthFlows implicit(OAuthFlow implicit) {
        this.implicit = implicit;
        return this;
    }

    @Override
    public OAuthFlow getPassword() {
        return password;
    }

    @Override
    public void setPassword(OAuthFlow password) {
        this.password = password;
    }

    @Override
    public OAuthFlows password(OAuthFlow password) {
        this.password = password;
        return this;
    }

    @Override
    public OAuthFlow getClientCredentials() {
        return clientCredentials;
    }

    @Override
    public void setClientCredentials(OAuthFlow clientCredentials) {
        this.clientCredentials = clientCredentials;
    }

    @Override
    public OAuthFlows clientCredentials(OAuthFlow clientCredentials) {
        this.clientCredentials = clientCredentials;
        return this;
    }

    @Override
    public OAuthFlow getAuthorizationCode() {
        return authorizationCode;
    }

    @Override
    public void setAuthorizationCode(OAuthFlow authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    @Override
    public OAuthFlows authorizationCode(OAuthFlow authorizationCode) {
        this.authorizationCode = authorizationCode;
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
        OAuthFlowsImpl oauthFlows = (OAuthFlowsImpl) o;
        return Objects.equals(this.implicit, oauthFlows.implicit) &&
               Objects.equals(this.password, oauthFlows.password) &&
               Objects.equals(this.clientCredentials, oauthFlows.clientCredentials) &&
               Objects.equals(this.authorizationCode, oauthFlows.authorizationCode) &&
               Objects.equals(this.extensions, oauthFlows.extensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(implicit, password, clientCredentials, authorizationCode, extensions);
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
        sb.append("class OAuthFlows {\n");

        sb = !toIndentedString(implicit).equals(Constants.NULL_VALUE) ? sb.append("    implicit: ").append(toIndentedString(implicit)).append("\n") : sb.append("");
        sb = !toIndentedString(password).equals(Constants.NULL_VALUE) ? sb.append("    password: ").append(toIndentedString(password)).append("\n") : sb.append("");
        sb = !toIndentedString(clientCredentials).equals(Constants.NULL_VALUE) ? sb.append("    clientCredentials: ").append(toIndentedString(clientCredentials)).append("\n") : sb.append("");
        sb = !toIndentedString(authorizationCode).equals(Constants.NULL_VALUE) ? sb.append("    authorizationCode: ").append(toIndentedString(authorizationCode)).append("\n") : sb.append("");
        sb = !toIndentedString(extensions).equals(Constants.NULL_VALUE) ? sb.append("    extensions: ").append(OpenAPIUtils.mapToString(extensions)).append("\n") : sb.append("");
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
