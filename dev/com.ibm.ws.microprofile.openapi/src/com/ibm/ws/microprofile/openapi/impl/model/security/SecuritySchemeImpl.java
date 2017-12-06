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

import org.eclipse.microprofile.openapi.models.security.OAuthFlows;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * SecurityScheme
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.0.0-rc2/versions/3.0.md#securitySchemeObject"
 */

public class SecuritySchemeImpl implements SecurityScheme {
    private SecurityScheme.Type type = null;
    private String description = null;
    private String name = null;
    private String schemeName = null;
    private String $ref = null;

    private SecurityScheme.In in = null;
    private String scheme = null;
    private String bearerFormat = null;
    private OAuthFlows flows = null;
    private String openIdConnectUrl = null;
    private java.util.Map<String, Object> extensions = null;

    @Override
    public SecurityScheme.Type getType() {
        return type;
    }

    @Override
    public void setType(SecurityScheme.Type type) {
        this.type = type;
    }

    @Override
    public SecurityScheme type(SecurityScheme.Type type) {
        this.type = type;
        return this;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public SecurityScheme description(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public SecurityScheme name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public SecurityScheme.In getIn() {
        return in;
    }

    @Override
    public void setIn(SecurityScheme.In in) {
        this.in = in;
    }

    @Override
    public SecurityScheme in(SecurityScheme.In in) {
        this.in = in;
        return this;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    @Override
    public SecurityScheme scheme(String scheme) {
        this.scheme = scheme;
        return this;
    }

    @Override
    public String getBearerFormat() {
        return bearerFormat;
    }

    @Override
    public void setBearerFormat(String bearerFormat) {
        this.bearerFormat = bearerFormat;
    }

    @Override
    public SecurityScheme bearerFormat(String bearerFormat) {
        this.bearerFormat = bearerFormat;
        return this;
    }

    @Override
    public OAuthFlows getFlows() {
        return flows;
    }

    @Override
    public void setFlows(OAuthFlows flows) {
        this.flows = flows;
    }

    @Override
    public SecurityScheme flows(OAuthFlows flows) {
        this.flows = flows;
        return this;
    }

    @Override
    public String getOpenIdConnectUrl() {
        return openIdConnectUrl;
    }

    @Override
    public void setOpenIdConnectUrl(String openIdConnectUrl) {
        this.openIdConnectUrl = openIdConnectUrl;
    }

    @Override
    public SecurityScheme openIdConnectUrl(String openIdConnectUrl) {
        this.openIdConnectUrl = openIdConnectUrl;
        return this;
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
    public String getRef() {
        return $ref;
    }

    @Override
    public void setRef(String ref) {
        if ($ref != null && ($ref.indexOf(".") == -1 && $ref.indexOf("/") == -1)) {
            $ref = "#/components/securitySchemes/" + $ref;
        }
        this.$ref = ref;
    }

    @Override
    public SecurityScheme ref(String ref) {
        this.$ref = ref;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SecuritySchemeImpl)) {
            return false;
        }

        SecuritySchemeImpl that = (SecuritySchemeImpl) o;

        if (type != that.type) {
            return false;
        }
        if (description != null ? !description.equals(that.description) : that.description != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if ($ref != null ? !$ref.equals(that.$ref) : that.$ref != null) {
            return false;
        }
        if (in != that.in) {
            return false;
        }
        if (scheme != null ? !scheme.equals(that.scheme) : that.scheme != null) {
            return false;
        }
        if (bearerFormat != null ? !bearerFormat.equals(that.bearerFormat) : that.bearerFormat != null) {
            return false;
        }
        if (flows != null ? !flows.equals(that.flows) : that.flows != null) {
            return false;
        }
        if (openIdConnectUrl != null ? !openIdConnectUrl.equals(that.openIdConnectUrl) : that.openIdConnectUrl != null) {
            return false;
        }
        return extensions != null ? extensions.equals(that.extensions) : that.extensions == null;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + ($ref != null ? $ref.hashCode() : 0);
        result = 31 * result + (in != null ? in.hashCode() : 0);
        result = 31 * result + (scheme != null ? scheme.hashCode() : 0);
        result = 31 * result + (bearerFormat != null ? bearerFormat.hashCode() : 0);
        result = 31 * result + (flows != null ? flows.hashCode() : 0);
        result = 31 * result + (openIdConnectUrl != null ? openIdConnectUrl.hashCode() : 0);
        result = 31 * result + (extensions != null ? extensions.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class SecurityScheme {\n");

        sb.append("    type: ").append(toIndentedString(type)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    name: ").append(toIndentedString(name)).append("\n");
        sb.append("    in: ").append(toIndentedString(in)).append("\n");
        sb.append("    scheme: ").append(toIndentedString(scheme)).append("\n");
        sb.append("    bearerFormat: ").append(toIndentedString(bearerFormat)).append("\n");
        sb.append("    flows: ").append(toIndentedString(flows)).append("\n");
        sb.append("    openIdConnectUrl: ").append(toIndentedString(openIdConnectUrl)).append("\n");
        sb.append("    $ref: ").append(toIndentedString($ref)).append("\n");
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

    /**
     * @return the schemeName
     */
    @JsonIgnore
    public String getSchemeName() {
        return schemeName;
    }

    /**
     * @param schemeName the schemeName to set
     */
    public void setSchemeName(String schemeName) {
        this.schemeName = schemeName;
    }

}
