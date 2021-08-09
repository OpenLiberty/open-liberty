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
import java.util.Map;
import java.util.Objects;

import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.servers.Server;

import com.ibm.ws.microprofile.openapi.model.utils.OpenAPIUtils;

/**
 * Operation
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.md#operationObject"
 */

public class OperationImpl implements Operation {
    private List<String> tags = null;
    private String summary = null;
    private String description = null;
    private ExternalDocumentation externalDocs = null;
    private String operationId = null;
    private List<Parameter> parameters = null;
    private RequestBody requestBody = null;
    private APIResponses responses = null;
    private Map<String, Callback> callbacks = null;
    private Boolean deprecated = null;
    private List<SecurityRequirement> security = null;
    private List<Server> servers = null;
    private java.util.Map<String, Object> extensions = null;

    @Override
    public List<String> getTags() {
        return tags;
    }

    @Override
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    @Override
    public Operation tags(List<String> tags) {
        this.tags = tags;
        return this;
    }

    @Override
    public Operation addTag(String tag) {
        if (this.tags == null) {
            this.tags = new ArrayList<String>();
        }
        this.tags.add(tag);
        return this;
    }

    @Override
    public String getSummary() {
        return summary;
    }

    @Override
    public void setSummary(String summary) {
        this.summary = summary;
    }

    @Override
    public Operation summary(String summary) {
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
    public Operation description(String description) {
        this.description = description;
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
    public Operation externalDocs(ExternalDocumentation externalDocs) {
        this.externalDocs = externalDocs;
        return this;
    }

    @Override
    public String getOperationId() {
        return operationId;
    }

    @Override
    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    @Override
    public Operation operationId(String operationId) {
        this.operationId = operationId;
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
    public Operation parameters(List<Parameter> parameters) {
        this.parameters = parameters;
        return this;
    }

    @Override
    public Operation addParameter(Parameter parameter) {
        if (this.parameters == null) {
            this.parameters = new ArrayList<Parameter>();
        }
        this.parameters.add(parameter);
        return this;
    }

    @Override
    public RequestBody getRequestBody() {
        return requestBody;
    }

    @Override
    public void setRequestBody(RequestBody requestBody) {
        this.requestBody = requestBody;
    }

    @Override
    public Operation requestBody(RequestBody requestBody) {
        this.requestBody = requestBody;
        return this;
    }

    @Override
    public APIResponses getResponses() {
        return responses;
    }

    @Override
    public void setResponses(APIResponses responses) {
        this.responses = responses;
    }

    @Override
    public Operation responses(APIResponses responses) {
        this.responses = responses;
        return this;
    }

    @Override
    public Map<String, Callback> getCallbacks() {
        return callbacks;
    }

    @Override
    public void setCallbacks(Map<String, Callback> callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    public Operation callbacks(Map<String, Callback> callbacks) {
        this.callbacks = callbacks;
        return this;
    }

    @Override
    public Boolean getDeprecated() {
        return deprecated;
    }

    @Override
    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    @Override
    public Operation deprecated(Boolean deprecated) {
        this.deprecated = deprecated;
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
    public Operation security(List<SecurityRequirement> security) {
        this.security = security;
        return this;
    }

    @Override
    public Operation addSecurityRequirement(SecurityRequirement securityReq) {
        if (this.security == null) {
            this.security = new ArrayList<SecurityRequirement>();
        }
        this.security.add(securityReq);
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
    public Operation servers(List<Server> servers) {
        this.servers = servers;
        return this;
    }

    @Override
    public Operation addServer(Server server) {
        if (this.servers == null) {
            this.servers = new ArrayList<Server>();
        }
        this.servers.add(server);
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
        OperationImpl operation = (OperationImpl) o;
        return Objects.equals(this.tags, operation.tags) &&
               Objects.equals(this.summary, operation.summary) &&
               Objects.equals(this.description, operation.description) &&
               Objects.equals(this.externalDocs, operation.externalDocs) &&
               Objects.equals(this.operationId, operation.operationId) &&
               Objects.equals(this.parameters, operation.parameters) &&
               Objects.equals(this.requestBody, operation.requestBody) &&
               Objects.equals(this.responses, operation.responses) &&
               Objects.equals(this.callbacks, operation.callbacks) &&
               Objects.equals(this.deprecated, operation.deprecated) &&
               Objects.equals(this.security, operation.security) &&
               Objects.equals(this.servers, operation.servers) &&
               Objects.equals(this.extensions, operation.extensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tags, summary, description, externalDocs, operationId, parameters, requestBody, responses, callbacks, deprecated, security, servers, extensions);
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

    public void addExtension_compat(String name, Object value) {
        addExtension(name, value);
    }

    @Override
    public void setExtensions(java.util.Map<String, Object> extensions) {
        this.extensions = extensions;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Operation {\n");
        sb = (tags != null) ? sb.append("    tags: ").append(toIndentedString(tags)).append("\n") : sb.append("");
        sb = (summary != null) ? sb.append("    summary: ").append(toIndentedString(summary)).append("\n") : sb.append("");
        sb = (description != null) ? sb.append("    description: ").append(toIndentedString(description)).append("\n") : sb.append("");
        sb = (externalDocs != null) ? sb.append("    externalDocs: ").append(toIndentedString(externalDocs)).append("\n") : sb.append("");
        sb = (operationId != null) ? sb.append("    operationId: ").append(toIndentedString(operationId)).append("\n") : sb.append("");
        sb = (parameters != null) ? sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n") : sb.append("");
        sb = (requestBody != null) ? sb.append("    requestBody: ").append(toIndentedString(requestBody)).append("\n") : sb.append("");
        sb = (responses != null) ? sb.append("    responses: ").append(toIndentedString(responses)).append("\n") : sb.append("");
        sb = (callbacks != null) ? sb.append("    callbacks: ").append(OpenAPIUtils.mapToString(callbacks)).append("\n") : sb.append("");
        sb = (deprecated != null) ? sb.append("    deprecated: ").append(toIndentedString(deprecated)).append("\n") : sb.append("");
        sb = (security != null) ? sb.append("    security: ").append(toIndentedString(security)).append("\n") : sb.append("");
        sb = (servers != null) ? sb.append("    servers: ").append(toIndentedString(servers)).append("\n") : sb.append("");
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
