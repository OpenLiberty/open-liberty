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
package com.ibm.ws.microprofile.openapi.impl.model.info;

import java.util.Objects;

import org.eclipse.microprofile.openapi.models.info.License;

import com.ibm.ws.microprofile.openapi.Constants;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;

/**
 * License
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.md#licenseObject"
 */

public class LicenseImpl implements License {
    private String name = null;
    private String url = null;
    private java.util.Map<String, Object> extensions = null;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public License name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public License url(String url) {
        this.url = url;
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
        LicenseImpl license = (LicenseImpl) o;
        return Objects.equals(this.name, license.name) &&
               Objects.equals(this.url, license.url) &&
               Objects.equals(this.extensions, license.extensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, url, extensions);
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
        sb.append("class License {\n");
        sb = (!toIndentedString(name).equals(Constants.NULL_VALUE)) ? sb.append("    name: ").append(toIndentedString(name)).append("\n") : sb.append("");
        sb = (!toIndentedString(url).equals(Constants.NULL_VALUE)) ? sb.append("    url: ").append(toIndentedString(url)).append("\n") : sb.append("");
        sb = (!toIndentedString(extensions).equals(Constants.NULL_VALUE)) ? sb.append("    extensions: ").append(OpenAPIUtils.mapToString(extensions)).append("\n") : sb.append("");

        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

}
