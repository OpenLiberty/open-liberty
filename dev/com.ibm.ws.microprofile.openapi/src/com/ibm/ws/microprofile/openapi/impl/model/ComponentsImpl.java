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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

import com.ibm.ws.microprofile.openapi.Constants;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIUtils;

/**
 * Components
 *
 * @see "https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.md#componentsObject"
 */
public class ComponentsImpl implements Components {

    private Map<String, Schema> schemas = null;
    private Map<String, APIResponse> responses = null;
    private Map<String, Parameter> parameters = null;
    private Map<String, Example> examples = null;
    private Map<String, RequestBody> requestBodies = null;
    private Map<String, Header> headers = null;
    private Map<String, SecurityScheme> securitySchemes = null;
    private Map<String, Link> links = null;
    private Map<String, Callback> callbacks = null;
    private java.util.Map<String, Object> extensions = null;

    @Override
    public Map<String, Schema> getSchemas() {
        return schemas;
    }

    @Override
    public void setSchemas(Map<String, Schema> schemas) {
        this.schemas = schemas;
    }

    @Override
    public Components schemas(Map<String, Schema> schemas) {
        this.schemas = schemas;
        return this;
    }

    @Override
    public Components addSchema(String key, Schema schema) {
        if (this.schemas == null) {
            this.schemas = new HashMap<String, Schema>();
        }
        this.schemas.put(key, schema);
        return this;
    }

    @Override
    public Map<String, APIResponse> getResponses() {
        return responses;
    }

    @Override
    public void setResponses(Map<String, APIResponse> responses) {
        this.responses = responses;
    }

    @Override
    public Components responses(Map<String, APIResponse> responses) {
        this.responses = responses;
        return this;
    }

    @Override
    public Components addResponse(String key, APIResponse response) {
        if (this.responses == null) {
            this.responses = new HashMap<String, APIResponse>();
        }
        this.responses.put(key, response);
        return this;
    }

    @Override
    public Map<String, Parameter> getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(Map<String, Parameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public Components parameters(Map<String, Parameter> parameters) {
        this.parameters = parameters;
        return this;
    }

    @Override
    public Components addParameter(String key, Parameter parameter) {
        if (this.parameters == null) {
            this.parameters = new HashMap<String, Parameter>();
        }
        this.parameters.put(key, parameter);
        return this;
    }

    @Override
    public Map<String, Example> getExamples() {
        return examples;
    }

    @Override
    public void setExamples(Map<String, Example> examples) {
        this.examples = examples;
    }

    @Override
    public Components examples(Map<String, Example> examples) {
        this.examples = examples;
        return this;
    }

    @Override
    public Components addExample(String key, Example example) {
        if (this.examples == null) {
            this.examples = new HashMap<String, Example>();
        }
        this.examples.put(key, example);
        return this;
    }

    @Override
    public Map<String, RequestBody> getRequestBodies() {
        return requestBodies;
    }

    @Override
    public void setRequestBodies(Map<String, RequestBody> requestBodies) {
        this.requestBodies = requestBodies;
    }

    @Override
    public Components requestBodies(Map<String, RequestBody> requestBodies) {
        this.requestBodies = requestBodies;
        return this;
    }

    @Override
    public Components addRequestBody(String key, RequestBody requestBody) {
        if (this.requestBodies == null) {
            this.requestBodies = new HashMap<String, RequestBody>();
        }
        this.requestBodies.put(key, requestBody);
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
    public Components headers(Map<String, Header> headers) {
        this.headers = headers;
        return this;
    }

    @Override
    public Components addHeader(String key, Header header) {
        if (this.headers == null) {
            this.headers = new HashMap<String, Header>();
        }
        this.headers.put(key, header);
        return this;
    }

    @Override
    public Map<String, SecurityScheme> getSecuritySchemes() {
        return securitySchemes;
    }

    @Override
    public void setSecuritySchemes(Map<String, SecurityScheme> securitySchemes) {
        this.securitySchemes = securitySchemes;
    }

    @Override
    public Components securitySchemes(Map<String, SecurityScheme> securitySchemes) {
        this.securitySchemes = securitySchemes;
        return this;
    }

    @Override
    public Components addSecurityScheme(String key, SecurityScheme securityScheme) {
        if (this.securitySchemes == null) {
            this.securitySchemes = new HashMap<String, SecurityScheme>();
        }
        this.securitySchemes.put(key, securityScheme);
        return this;
    }

    @Override
    public Map<String, Link> getLinks() {
        return links;
    }

    @Override
    public void setLinks(Map<String, Link> links) {
        this.links = links;
    }

    @Override
    public Components links(Map<String, Link> links) {
        this.links = links;
        return this;
    }

    @Override
    public Components addLink(String key, Link linksItem) {
        if (this.links == null) {
            this.links = new HashMap<String, Link>();
        }
        this.links.put(key, linksItem);
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
    public Components callbacks(Map<String, Callback> callbacks) {
        this.callbacks = callbacks;
        return this;
    }

    @Override
    public Components addCallback(String key, Callback callbacksItem) {
        if (this.callbacks == null) {
            this.callbacks = new HashMap<String, Callback>();
        }
        this.callbacks.put(key, callbacksItem);
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
        ComponentsImpl components = (ComponentsImpl) o;
        return Objects.equals(this.schemas, components.schemas) &&
               Objects.equals(this.responses, components.responses) &&
               Objects.equals(this.parameters, components.parameters) &&
               Objects.equals(this.examples, components.examples) &&
               Objects.equals(this.requestBodies, components.requestBodies) &&
               Objects.equals(this.headers, components.headers) &&
               Objects.equals(this.securitySchemes, components.securitySchemes) &&
               Objects.equals(this.links, components.links) &&
               Objects.equals(this.callbacks, components.callbacks) &&
               Objects.equals(this.extensions, components.extensions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(schemas, responses, parameters, examples, requestBodies, headers, securitySchemes, links, callbacks, extensions);
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
        sb.append("class Components {\n");

        sb = (schemas != null) ? sb.append("    schemas: ").append(OpenAPIUtils.mapToString(schemas)).append("\n") : sb.append("");
        sb = (responses != null) ? sb.append("    responses: ").append(OpenAPIUtils.mapToString(responses)).append("\n") : sb.append("");
        sb = (parameters != null) ? sb.append("    parameters: ").append(OpenAPIUtils.mapToString(parameters)).append("\n") : sb.append("");
        sb = (examples != null) ? sb.append("    examples: ").append(OpenAPIUtils.mapToString(examples)).append("\n") : sb.append("");
        sb = (requestBodies != null) ? sb.append("    requestBodies: ").append(OpenAPIUtils.mapToString(requestBodies)).append("\n") : sb.append("");
        sb = (headers != null) ? sb.append("    headers: ").append(OpenAPIUtils.mapToString(headers)).append("\n") : sb.append("");
        sb = (securitySchemes != null) ? sb.append("    securitySchemes: ").append(OpenAPIUtils.mapToString(securitySchemes)).append("\n") : sb.append("");
        sb = (links != null) ? sb.append("    links: ").append(OpenAPIUtils.mapToString(links)).append("\n") : sb.append("");
        sb = (callbacks != null) ? sb.append("    callbacks: ").append(OpenAPIUtils.mapToString(callbacks)).append("\n") : sb.append("");
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
