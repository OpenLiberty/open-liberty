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
package com.ibm.ws.microprofile.openapi.impl.model.responses;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;

/**
 * ApiResponse
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/3.0.0-rc2/versions/3.0.md#responseObject"
 */

public class APIResponseImpl implements APIResponse {
    private String description = null;
    private Map<String, Header> headers = null;
    private Content content = null;
    private java.util.Map<String, Link> links = null;
    private java.util.Map<String, Object> extensions = null;
    private String $ref = null;

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public APIResponse description(String description) {
        this.description = description;
        return this;
    }

    @Override
    public Map<String, Header> getHeaders() {
        return headers;
    }

    @Override
    public void setHeaders(Map<String, Header> headers) {
        this.headers = headers;
    }

    @Override
    public APIResponse headers(Map<String, Header> headers) {
        this.headers = headers;
        return this;
    }

    @Override
    public APIResponse addHeader(String name, Header header) {
        if (this.headers == null) {
            headers = new HashMap<>();
        }
        headers.put(name, header);
        return this;
    }

    @Override
    public Content getContent() {
        return content;
    }

    @Override
    public void setContent(Content content) {
        this.content = content;
    }

    @Override
    public APIResponse content(Content content) {
        this.content = content;
        return this;
    }

    @Override
    public java.util.Map<String, Link> getLinks() {
        return links;
    }

    @Override
    public void setLinks(java.util.Map<String, Link> links) {
        this.links = links;
    }

    @Override
    public APIResponse addLink(String name, Link link) {
        if (this.links == null) {
            this.links = new HashMap<>();
        }
        this.links.put(name, link);
        return this;
    }

    @Override
    public String getRef() {
        return $ref;
    }

    @Override
    public void setRef(String $ref) {
        if ($ref != null && ($ref.indexOf(".") == -1 && $ref.indexOf("/") == -1)) {
            $ref = "#/components/responses/" + $ref;
        }
        this.$ref = $ref;
    }

    @Override
    public APIResponse ref(String $ref) {
        setRef($ref);
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
        APIResponseImpl apiResponse = (APIResponseImpl) o;
        return Objects.equals(this.description, apiResponse.description) &&
               Objects.equals(this.headers, apiResponse.headers) &&
               Objects.equals(this.content, apiResponse.content) &&
               Objects.equals(this.links, apiResponse.links) &&
               Objects.equals(this.extensions, apiResponse.extensions) &&
               Objects.equals(this.$ref, apiResponse.$ref);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, headers, content, links, extensions, $ref);
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
        sb.append("class ApiResponse {\n");

        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    headers: ").append(toIndentedString(headers)).append("\n");
        sb.append("    content: ").append(toIndentedString(content)).append("\n");
        sb.append("    links: ").append(toIndentedString(links)).append("\n");
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

    /** {@inheritDoc} */
    @Override
    public APIResponse links(Map<String, Link> links) {
        this.links = links;
        return this;
    }

}
