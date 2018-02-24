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

import org.eclipse.microprofile.openapi.models.info.Contact;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.info.License;

import com.ibm.ws.microprofile.openapi.Constants;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;

/**
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.md#infoObject"
 */

public class InfoImpl implements Info {
    private String title = null;
    private String description = null;
    private String termsOfService = null;
    private Contact contact = null;
    private License license = null;
    private String version = null;
    private java.util.Map<String, Object> extensions = null;

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public Info title(String title) {
        this.title = title;
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
    public Info description(String description) {
        this.description = description;
        return this;
    }

    @Override
    public String getTermsOfService() {
        return termsOfService;
    }

    @Override
    public void setTermsOfService(String termsOfService) {
        this.termsOfService = termsOfService;
    }

    @Override
    public Info termsOfService(String termsOfService) {
        this.termsOfService = termsOfService;
        return this;
    }

    @Override
    public Contact getContact() {
        return contact;
    }

    @Override
    public void setContact(Contact contact) {
        this.contact = contact;
    }

    @Override
    public Info contact(Contact contact) {
        this.contact = contact;
        return this;
    }

    @Override
    public License getLicense() {
        return license;
    }

    @Override
    public void setLicense(License license) {
        this.license = license;
    }

    @Override
    public Info license(License license) {
        this.license = license;
        return this;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public Info version(String version) {
        this.version = version;
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
        InfoImpl info = (InfoImpl) o;
        return Objects.equals(this.title, info.title) &&
               Objects.equals(this.description, info.description) &&
               Objects.equals(this.termsOfService, info.termsOfService) &&
               Objects.equals(this.contact, info.contact) &&
               Objects.equals(this.license, info.license) &&
               Objects.equals(this.version, info.version) &&
               Objects.equals(this.extensions, info.extensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, description, termsOfService, contact, license, version, extensions);
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
        sb.append("class Info {\n");
        sb = (title != null) ? sb.append("    title: ").append(toIndentedString(title)).append("\n") : sb.append("");
        sb = (description != null) ? sb.append("    description: ").append(toIndentedString(description)).append("\n") : sb.append("");
        sb = (termsOfService != null) ? sb.append("    termsOfService: ").append(toIndentedString(termsOfService)).append("\n") : sb.append("");
        sb = (contact != null) ? sb.append("    contact: ").append(toIndentedString(contact)).append("\n") : sb.append("");
        sb = (license != null) ? sb.append("    license: ").append(toIndentedString(license)).append("\n") : sb.append("");
        sb = (version != null) ? sb.append("    version: ").append(toIndentedString(version)).append("\n") : sb.append("");
        sb = (extensions != null) ? sb.append("    extensions: ").append(OpenAPIUtils.mapToString(extensions)).append("\n") : sb.append("");

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
