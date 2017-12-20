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
package com.ibm.ws.microprofile.openapi.impl.model.links;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.servers.Server;

/**
 * Link
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.md#linkObject"
 */

public class LinkImpl implements Link {
    private String operationRef = null;
    private String operationId = null;
    private Map<String, Object> parameters = null;
    private Object requestBody = null;
    private final Map<String, Header> headers = null;
    private String description = null;
    private String $ref = null;
    private java.util.Map<String, Object> extensions = null;
    private Server server;

    @Override
    public Server getServer() {
        return server;
    }

    @Override
    public void setServer(Server server) {
        this.server = server;
    }

    @Override
    public Link server(Server server) {
        this.setServer(server);
        return this;
    }

    @Override
    public String getOperationRef() {
        return operationRef;
    }

    @Override
    public void setOperationRef(String operationRef) {
        this.operationRef = operationRef;
    }

    @Override
    public Link operationRef(String operationRef) {
        this.operationRef = operationRef;
        return this;
    }

    @Override
    public Object getRequestBody() {
        return requestBody;
    }

    @Override
    public void setRequestBody(Object requestBody) {
        this.requestBody = requestBody;
    }

    @Override
    public Link requestBody(Object requestBody) {
        this.requestBody = requestBody;
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
    public Link operationId(String operationId) {
        this.operationId = operationId;
        return this;
    }

    @Override
    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    @Override
    public Link addParameter(String name, Object parameter) {
        if (this.parameters == null) {
            this.parameters = new HashMap<>();
        }
        this.parameters.put(name, parameter);

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
    public Link description(String description) {
        this.description = description;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LinkImpl)) {
            return false;
        }

        LinkImpl link = (LinkImpl) o;

        if (operationRef != null ? !operationRef.equals(link.operationRef) : link.operationRef != null) {
            return false;
        }
        if (operationId != null ? !operationId.equals(link.operationId) : link.operationId != null) {
            return false;
        }
        if (parameters != null ? !parameters.equals(link.parameters) : link.parameters != null) {
            return false;
        }

        if (requestBody != null ? !requestBody.equals(link.requestBody) : link.requestBody != null) {
            return false;
        }

        if (headers != null ? !headers.equals(link.headers) : link.headers != null) {
            return false;
        }
        if (description != null ? !description.equals(link.description) : link.description != null) {
            return false;
        }

        if ($ref != null ? !$ref.equals(link.$ref) : link.$ref != null) {
            return false;
        }

        if (extensions != null ? !extensions.equals(link.extensions) : link.extensions != null) {
            return false;
        }
        return server != null ? server.equals(link.server) : link.server == null;

    }

    @Override
    public int hashCode() {
        int result = operationRef != null ? operationRef.hashCode() : 0;
        result = 31 * result + (operationId != null ? operationId.hashCode() : 0);
        result = 31 * result + (parameters != null ? parameters.hashCode() : 0);
        result = 31 * result + (requestBody != null ? requestBody.hashCode() : 0);
        result = 31 * result + (headers != null ? headers.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + ($ref != null ? $ref.hashCode() : 0);
        result = 31 * result + (extensions != null ? extensions.hashCode() : 0);
        result = 31 * result + (server != null ? server.hashCode() : 0);
        return result;
    }

    @Override
    public String getRef() {
        return $ref;
    }

    @Override
    public void setRef(String ref) {
        if (ref != null && (ref.indexOf(".") == -1 && ref.indexOf("/") == -1)) {
            ref = "#/components/links/" + ref;
        }
        this.$ref = ref;
    }

    @Override
    public Link ref(String $ref) {
        setRef($ref);
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
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Link {\n");

        sb.append("    operationRef: ").append(toIndentedString(operationRef)).append("\n");
        sb.append("    operationId: ").append(toIndentedString(operationId)).append("\n");
        sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
        sb.append("    requestBody: ").append(toIndentedString(requestBody)).append("\n");
        sb.append("    headers: ").append(toIndentedString(headers)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    $ref: ").append(toIndentedString($ref)).append("\n");
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

    /** {@inheritDoc} */
    @Override
    public Link parameters(Map<String, Object> parameters) {
        this.parameters = parameters;
        return this;
    }

}
