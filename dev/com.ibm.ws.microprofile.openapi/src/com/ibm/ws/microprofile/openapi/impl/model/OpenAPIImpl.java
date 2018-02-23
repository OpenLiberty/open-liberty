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

package com.ibm.ws.microprofile.openapi.impl.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.tags.Tag;

import com.ibm.ws.microprofile.openapi.Constants;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;

/**
 * OpenAPI
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.0.md"
 */

public class OpenAPIImpl implements OpenAPI {
    private String openapi = "3.0.0";
    private Info info = null;
    private ExternalDocumentation externalDocs = null;
    private List<Server> servers = null;
    private List<SecurityRequirement> security = null;
    private List<Tag> tags = null;
    private Paths paths = null;
    private Components components = null;
    private java.util.Map<String, Object> extensions = null;

    @Override
    public String getOpenapi() {
        return openapi;
    }

    @Override
    public void setOpenapi(String openapi) {
        this.openapi = openapi;
    }

    @Override
    public OpenAPI openapi(String openapi) {
        this.openapi = openapi;
        return this;
    }

    @Override
    public Info getInfo() {
        return info;
    }

    @Override
    public void setInfo(Info info) {
        this.info = info;
    }

    @Override
    public OpenAPI info(Info info) {
        this.info = info;
        return this;
    }

    @Override
    public ExternalDocumentation getExternalDocs() {
        return externalDocs;
    }

    @Override
    public void setExternalDocs(ExternalDocumentation externalDocs) {
        this.externalDocs = externalDocs;
    }

    @Override
    public OpenAPI externalDocs(ExternalDocumentation externalDocs) {
        this.externalDocs = externalDocs;
        return this;
    }

    @Override
    public List<Server> getServers() {
        return servers;
    }

    @Override
    public void setServers(List<Server> servers) {
        this.servers = servers;
    }

    @Override
    public OpenAPI servers(List<Server> servers) {
        this.servers = servers;
        return this;
    }

    @Override
    public OpenAPI addServer(Server server) {
        if (this.servers == null) {
            this.servers = new ArrayList<Server>();
        }
        this.servers.add(server);
        return this;
    }

    @Override
    public List<SecurityRequirement> getSecurity() {
        return security;
    }

    @Override
    public void setSecurity(List<SecurityRequirement> security) {
        this.security = security;
    }

    @Override
    public OpenAPI security(List<SecurityRequirement> security) {
        this.security = security;
        return this;
    }

    @Override
    public OpenAPI addSecurityRequirement(SecurityRequirement securityReq) {
        if (this.security == null) {
            this.security = new ArrayList<SecurityRequirement>();
        }
        this.security.add(securityReq);
        return this;
    }

    @Override
    public List<Tag> getTags() {
        return tags;
    }

    @Override
    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    @Override
    public OpenAPI tags(List<Tag> tags) {
        this.tags = tags;
        return this;
    }

    @Override
    public OpenAPI addTag(Tag tag) {
        if (this.tags == null) {
            this.tags = new ArrayList<Tag>();
        }
        this.tags.add(tag);
        return this;
    }

    @Override
    public Paths getPaths() {
        return paths;
    }

    @Override
    public void setPaths(Paths paths) {
        this.paths = paths;
    }

    @Override
    public OpenAPI paths(Paths paths) {
        this.paths = paths;
        return this;
    }

    @Override
    public Components getComponents() {
        return components;
    }

    @Override
    public void setComponents(Components components) {
        this.components = components;
    }

    @Override
    public OpenAPI components(Components components) {
        this.components = components;
        return this;
    }

    /*
     * helpers
     */

    @Override
    public OpenAPI path(String name, PathItem path) {
        if (this.paths == null) {
            this.paths = new PathsImpl();
        }

        this.paths.addPathItem(name, path);
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
        OpenAPIImpl openAPI = (OpenAPIImpl) o;
        return Objects.equals(this.openapi, openAPI.openapi) &&
               Objects.equals(this.info, openAPI.info) &&
               Objects.equals(this.externalDocs, openAPI.externalDocs) &&
               Objects.equals(this.servers, openAPI.servers) &&
               Objects.equals(this.security, openAPI.security) &&
               Objects.equals(this.tags, openAPI.tags) &&
               Objects.equals(this.paths, openAPI.paths) &&
               Objects.equals(this.components, openAPI.components) &&
               Objects.equals(this.extensions, openAPI.extensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(openapi, info, externalDocs, servers, security, tags, paths, components, extensions);
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
        sb.append("class OpenAPI {\n");
        sb = (openapi != null) ? sb.append("    openapi: ").append(toIndentedString(openapi)).append("\n") : sb.append("");
        sb = (info != null) ? sb.append("    info: ").append(toIndentedString(info)).append("\n") : sb.append("");
        sb = (externalDocs != null) ? sb.append("    externalDocs: ").append(toIndentedString(externalDocs)).append("\n") : sb.append("");
        sb = (servers != null) ? sb.append("    servers: ").append(toIndentedString(servers)).append("\n") : sb.append("");
        sb = (security != null) ? sb.append("    security: ").append(toIndentedString(security)).append("\n") : sb.append("");
        sb = (tags != null) ? sb.append("    tags: ").append(toIndentedString(tags)).append("\n") : sb.append("");
        sb = (paths != null) ? sb.append("    paths: ").append(toIndentedString(paths)).append("\n") : sb.append("");
        sb = (components != null) ? sb.append("    components: ").append(toIndentedString(components)).append("\n") : sb.append("");
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
