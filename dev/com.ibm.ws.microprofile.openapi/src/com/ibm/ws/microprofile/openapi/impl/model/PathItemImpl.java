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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.servers.Server;

import com.ibm.ws.microprofile.openapi.Constants;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;

/**
 * PathItem
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.md#pathItemObject"
 */

public class PathItemImpl implements PathItem {
    private String summary = null;
    private String description = null;
    private Operation get = null;
    private Operation put = null;
    private Operation post = null;
    private Operation delete = null;
    private Operation options = null;
    private Operation head = null;
    private Operation patch = null;
    private Operation trace = null;
    private List<Server> servers = null;
    private List<Parameter> parameters = null;
    private String $ref = null;
    private java.util.Map<String, Object> extensions = null;

    @Override
    public String getSummary() {
        return summary;
    }

    @Override
    public void setSummary(String summary) {
        this.summary = summary;
    }

    @Override
    public PathItem summary(String summary) {
        this.summary = summary;
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
    public PathItem description(String description) {
        this.description = description;
        return this;
    }

    @Override
    public Operation getGET() {
        return get;
    }

    @Override
    public void setGET(Operation get) {
        this.get = get;
    }

    @Override
    public PathItem GET(Operation get) {
        this.get = get;
        return this;
    }

    @Override
    public Operation getPUT() {
        return put;
    }

    @Override
    public void setPUT(Operation put) {
        this.put = put;
    }

    @Override
    public PathItem PUT(Operation put) {
        this.put = put;
        return this;
    }

    @Override
    public Operation getPOST() {
        return post;
    }

    @Override
    public void setPOST(Operation post) {
        this.post = post;
    }

    @Override
    public PathItem POST(Operation post) {
        this.post = post;
        return this;
    }

    @Override
    public Operation getDELETE() {
        return delete;
    }

    @Override
    public void setDELETE(Operation delete) {
        this.delete = delete;
    }

    @Override
    public PathItem DELETE(Operation delete) {
        this.delete = delete;
        return this;
    }

    @Override
    public Operation getOPTIONS() {
        return options;
    }

    @Override
    public void setOPTIONS(Operation options) {
        this.options = options;
    }

    @Override
    public PathItem OPTIONS(Operation options) {
        this.options = options;
        return this;
    }

    @Override
    public Operation getHEAD() {
        return head;
    }

    @Override
    public void setHEAD(Operation head) {
        this.head = head;
    }

    @Override
    public PathItem HEAD(Operation head) {
        this.head = head;
        return this;
    }

    @Override
    public Operation getPATCH() {
        return patch;
    }

    @Override
    public void setPATCH(Operation patch) {
        this.patch = patch;
    }

    @Override
    public PathItem PATCH(Operation patch) {
        this.patch = patch;
        return this;
    }

    @Override
    public Operation getTRACE() {
        return trace;
    }

    @Override
    public void setTRACE(Operation trace) {
        this.trace = trace;
    }

    @Override
    public PathItem TRACE(Operation trace) {
        this.trace = trace;
        return this;
    }

    @Override
    public List<Operation> readOperations() {
        List<Operation> allOperations = new ArrayList<>();
        if (this.get != null) {
            allOperations.add(this.get);
        }
        if (this.put != null) {
            allOperations.add(this.put);
        }
        if (this.head != null) {
            allOperations.add(this.head);
        }
        if (this.post != null) {
            allOperations.add(this.post);
        }
        if (this.delete != null) {
            allOperations.add(this.delete);
        }
        if (this.patch != null) {
            allOperations.add(this.patch);
        }
        if (this.options != null) {
            allOperations.add(this.options);
        }
        if (this.trace != null) {
            allOperations.add(this.trace);
        }

        return allOperations;
    }

    @Override
    public Map<PathItem.HttpMethod, Operation> readOperationsMap() {
        Map<PathItem.HttpMethod, Operation> result = new LinkedHashMap<>();

        if (this.get != null) {
            result.put(PathItem.HttpMethod.GET, this.get);
        }
        if (this.put != null) {
            result.put(PathItem.HttpMethod.PUT, this.put);
        }
        if (this.post != null) {
            result.put(PathItem.HttpMethod.POST, this.post);
        }
        if (this.delete != null) {
            result.put(PathItem.HttpMethod.DELETE, this.delete);
        }
        if (this.patch != null) {
            result.put(PathItem.HttpMethod.PATCH, this.patch);
        }
        if (this.head != null) {
            result.put(PathItem.HttpMethod.HEAD, this.head);
        }
        if (this.options != null) {
            result.put(PathItem.HttpMethod.OPTIONS, this.options);
        }
        if (this.trace != null) {
            result.put(PathItem.HttpMethod.TRACE, this.trace);
        }

        return result;
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
    public PathItem servers(List<Server> servers) {
        this.servers = servers;
        return this;
    }

    @Override
    public PathItem addServer(Server server) {
        if (this.servers == null) {
            this.servers = new ArrayList<Server>();
        }
        this.servers.add(server);
        return this;
    }

    @Override
    public List<Parameter> getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public PathItem parameters(List<Parameter> parameters) {
        this.parameters = parameters;
        return this;
    }

    @Override
    public PathItem addParameter(Parameter parameter) {
        if (this.parameters == null) {
            this.parameters = new ArrayList<Parameter>();
        }
        this.parameters.add(parameter);
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
    public void setRef(String $ref) {
        this.$ref = $ref;
    }

    @Override
    public PathItem ref(String $ref) {
        this.$ref = $ref;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PathItemImpl)) {
            return false;
        }

        PathItemImpl pathItem = (PathItemImpl) o;

        if (summary != null ? !summary.equals(pathItem.summary) : pathItem.summary != null) {
            return false;
        }
        if (description != null ? !description.equals(pathItem.description) : pathItem.description != null) {
            return false;
        }
        if (get != null ? !get.equals(pathItem.get) : pathItem.get != null) {
            return false;
        }
        if (put != null ? !put.equals(pathItem.put) : pathItem.put != null) {
            return false;
        }
        if (post != null ? !post.equals(pathItem.post) : pathItem.post != null) {
            return false;
        }
        if (delete != null ? !delete.equals(pathItem.delete) : pathItem.delete != null) {
            return false;
        }
        if (options != null ? !options.equals(pathItem.options) : pathItem.options != null) {
            return false;
        }
        if (head != null ? !head.equals(pathItem.head) : pathItem.head != null) {
            return false;
        }
        if (patch != null ? !patch.equals(pathItem.patch) : pathItem.patch != null) {
            return false;
        }
        if (trace != null ? !trace.equals(pathItem.trace) : pathItem.trace != null) {
            return false;
        }
        if (servers != null ? !servers.equals(pathItem.servers) : pathItem.servers != null) {
            return false;
        }
        if (parameters != null ? !parameters.equals(pathItem.parameters) : pathItem.parameters != null) {
            return false;
        }
        if ($ref != null ? !$ref.equals(pathItem.$ref) : pathItem.$ref != null) {
            return false;
        }
        return extensions != null ? extensions.equals(pathItem.extensions) : pathItem.extensions == null;

    }

    @Override
    public int hashCode() {
        int result = summary != null ? summary.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (get != null ? get.hashCode() : 0);
        result = 31 * result + (put != null ? put.hashCode() : 0);
        result = 31 * result + (post != null ? post.hashCode() : 0);
        result = 31 * result + (delete != null ? delete.hashCode() : 0);
        result = 31 * result + (options != null ? options.hashCode() : 0);
        result = 31 * result + (head != null ? head.hashCode() : 0);
        result = 31 * result + (patch != null ? patch.hashCode() : 0);
        result = 31 * result + (trace != null ? trace.hashCode() : 0);
        result = 31 * result + (servers != null ? servers.hashCode() : 0);
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        result = 31 * result + ($ref != null ? $ref.hashCode() : 0);
        result = 31 * result + (extensions != null ? extensions.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class PathItem {\n");

        sb = (summary != null) ? sb.append("    summary: ").append(toIndentedString(summary)).append("\n") : sb.append("");
        sb = (description != null) ? sb.append("    description: ").append(toIndentedString(description)).append("\n") : sb.append("");
        sb = (get != null) ? sb.append("    get: ").append(toIndentedString(get)).append("\n") : sb.append("");
        sb = (put != null) ? sb.append("    put: ").append(toIndentedString(put)).append("\n") : sb.append("");
        sb = (post != null) ? sb.append("    post: ").append(toIndentedString(post)).append("\n") : sb.append("");
        sb = (delete != null) ? sb.append("    delete: ").append(toIndentedString(delete)).append("\n") : sb.append("");
        sb = (options != null) ? sb.append("    options: ").append(toIndentedString(options)).append("\n") : sb.append("");
        sb = (head != null) ? sb.append("    head: ").append(toIndentedString(head)).append("\n") : sb.append("");
        sb = (patch != null) ? sb.append("    patch: ").append(toIndentedString(patch)).append("\n") : sb.append("");
        sb = (trace != null) ? sb.append("    trace: ").append(toIndentedString(trace)).append("\n") : sb.append("");
        sb = (servers != null) ? sb.append("    servers: ").append(toIndentedString(servers)).append("\n") : sb.append("");
        sb = (parameters != null) ? sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n") : sb.append("");
        sb = ($ref != null) ? sb.append("    $ref: ").append(toIndentedString($ref)).append("\n") : sb.append("");
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
