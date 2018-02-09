/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.microprofile.openapi.impl.parser.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.info.Contact;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.info.License;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Discriminator;
import org.eclipse.microprofile.openapi.models.media.Encoding;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.eclipse.microprofile.openapi.models.media.XML;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.OAuthFlow;
import org.eclipse.microprofile.openapi.models.security.OAuthFlows;
import org.eclipse.microprofile.openapi.models.security.Scopes;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.eclipse.microprofile.openapi.models.servers.ServerVariables;
import org.eclipse.microprofile.openapi.models.tags.Tag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.ibm.ws.microprofile.openapi.impl.core.util.Json;
import com.ibm.ws.microprofile.openapi.impl.model.ComponentsImpl;
import com.ibm.ws.microprofile.openapi.impl.model.ExternalDocumentationImpl;
import com.ibm.ws.microprofile.openapi.impl.model.OpenAPIImpl;
import com.ibm.ws.microprofile.openapi.impl.model.OperationImpl;
import com.ibm.ws.microprofile.openapi.impl.model.PathItemImpl;
import com.ibm.ws.microprofile.openapi.impl.model.PathsImpl;
import com.ibm.ws.microprofile.openapi.impl.model.callbacks.CallbackImpl;
import com.ibm.ws.microprofile.openapi.impl.model.examples.ExampleImpl;
import com.ibm.ws.microprofile.openapi.impl.model.headers.HeaderImpl;
import com.ibm.ws.microprofile.openapi.impl.model.info.ContactImpl;
import com.ibm.ws.microprofile.openapi.impl.model.info.InfoImpl;
import com.ibm.ws.microprofile.openapi.impl.model.info.LicenseImpl;
import com.ibm.ws.microprofile.openapi.impl.model.links.LinkImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.ContentImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.DiscriminatorImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.EncodingImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.MediaTypeImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.SchemaImpl;
import com.ibm.ws.microprofile.openapi.impl.model.media.XMLImpl;
import com.ibm.ws.microprofile.openapi.impl.model.parameters.ParameterImpl;
import com.ibm.ws.microprofile.openapi.impl.model.parameters.RequestBodyImpl;
import com.ibm.ws.microprofile.openapi.impl.model.responses.APIResponseImpl;
import com.ibm.ws.microprofile.openapi.impl.model.responses.APIResponsesImpl;
import com.ibm.ws.microprofile.openapi.impl.model.security.OAuthFlowImpl;
import com.ibm.ws.microprofile.openapi.impl.model.security.OAuthFlowsImpl;
import com.ibm.ws.microprofile.openapi.impl.model.security.ScopesImpl;
import com.ibm.ws.microprofile.openapi.impl.model.security.SecurityRequirementImpl;
import com.ibm.ws.microprofile.openapi.impl.model.security.SecuritySchemeImpl;
import com.ibm.ws.microprofile.openapi.impl.model.servers.ServerImpl;
import com.ibm.ws.microprofile.openapi.impl.model.servers.ServerVariableImpl;
import com.ibm.ws.microprofile.openapi.impl.model.servers.ServerVariablesImpl;
import com.ibm.ws.microprofile.openapi.impl.model.tags.TagImpl;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.SwaggerParseResult;

public class OpenAPIDeserializer {

    protected static Set<String> ROOT_KEYS = new LinkedHashSet<>(Arrays.asList("openapi", "info", "servers", "paths", "components", "security", "tags", "externalDocs"));
    protected static Set<String> INFO_KEYS = new LinkedHashSet<>(Arrays.asList("title", "description", "termsOfService", "contact", "license", "version"));
    protected static Set<String> CONTACT_KEYS = new LinkedHashSet<>(Arrays.asList("name", "url", "email"));
    protected static Set<String> LICENSE_KEYS = new LinkedHashSet<>(Arrays.asList("name", "url"));
    protected static Set<String> TAG_KEYS = new LinkedHashSet<>(Arrays.asList("description", "name", "externalDocs"));
    protected static Set<String> RESPONSE_KEYS = new LinkedHashSet<>(Arrays.asList("$ref", "description", "headers", "content", "links"));
    protected static Set<String> SERVER_KEYS = new LinkedHashSet<>(Arrays.asList("url", "description", "variables"));
    protected static Set<String> SERVER_VARIABLE_KEYS = new LinkedHashSet<>(Arrays.asList("enum", "default", "description"));
    protected static Set<String> PATHITEM_KEYS = new LinkedHashSet<>(Arrays.asList("$ref", "summary", "description", "get", "put", "post", "delete", "head", "patch", "options",
                                                                                   "trace", "servers", "parameters"));
    protected static Set<String> OPERATION_KEYS = new LinkedHashSet<>(Arrays.asList("tags", "summary", "description", "externalDocs", "operationId", "parameters", "requestBody",
                                                                                    "responses", "callbacks", "deprecated", "security", "servers"));
    protected static Set<String> PARAMETER_KEYS = new LinkedHashSet<>(Arrays.asList("$ref", "name", "in", "description", "required", "deprecated", "allowEmptyValue", "style",
                                                                                    "explode", "allowReserved", "schema", "example", "examples", "content"));
    protected static Set<String> REQUEST_BODY_KEYS = new LinkedHashSet<>(Arrays.asList("$ref", "description", "content", "required"));
    protected static Set<String> SECURITY_SCHEME_KEYS = new LinkedHashSet<>(Arrays.asList("$ref", "type", "name", "in", "description", "flows", "scheme", "bearerFormat",
                                                                                          "openIdConnectUrl"));
    protected static Set<String> EXTERNAL_DOCS_KEYS = new LinkedHashSet<>(Arrays.asList("description", "url"));
    protected static Set<String> COMPONENTS_KEYS = new LinkedHashSet<>(Arrays.asList("schemas", "responses", "parameters", "examples", "requestBodies", "headers",
                                                                                     "securitySchemes", "links", "callbacks"));
    protected static Set<String> SCHEMA_KEYS = new LinkedHashSet<>(Arrays.asList("$ref", "title", "multipleOf", "maximum", "format", "exclusiveMaximum", "minimum",
                                                                                 "exclusiveMinimum", "maxLength", "minLength", "pattern", "maxItems", "minItems", "uniqueItems",
                                                                                 "maxProperties", "minProperties", "required", "enum", "type", "allOf", "oneOf", "anyOf", "not",
                                                                                 "items", "properties", "additionalProperties", "description", "format", "default", "nullable",
                                                                                 "discriminator", "readOnly", "writeOnly", "xml", "externalDocs", "example", "deprecated"));
    protected static Set<String> EXAMPLE_KEYS = new LinkedHashSet<>(Arrays.asList("$ref", "summary", "description", "value", "externalValue"));
    protected static Set<String> HEADER_KEYS = new LinkedHashSet<>(Arrays.asList("$ref", "name", "in", "description", "required", "deprecated", "allowEmptyValue", "style",
                                                                                 "explode", "allowReserved", "schema", "example", "examples", "content"));
    protected static Set<String> LINK_KEYS = new LinkedHashSet<>(Arrays.asList("$ref", "operationRef", "operationId", "parameters", "requestBody", "description", "server"));
    protected static Set<String> MEDIATYPE_KEYS = new LinkedHashSet<>(Arrays.asList("schema", "example", "examples", "encoding"));
    protected static Set<String> XML_KEYS = new LinkedHashSet<>(Arrays.asList("name", "namespace", "prefix", "attribute", "wrapped"));
    protected static Set<String> OAUTHFLOW_KEYS = new LinkedHashSet<>(Arrays.asList("authorizationUrl", "tokenUrl", "refreshUrl", "scopes"));
    protected static Set<String> OAUTHFLOWS_KEYS = new LinkedHashSet<>(Arrays.asList("implicit", "password", "clientCredentials", "authorizationCode"));
    protected static Set<String> ENCODING_KEYS = new LinkedHashSet<>(Arrays.asList("contentType", "headers", "style", "explode", "allowReserved"));

    private static final String QUERY_PARAMETER = "query";
    private static final String COOKIE_PARAMETER = "cookie";
    private static final String PATH_PARAMETER = "path";
    private static final String HEADER_PARAMETER = "header";

    public SwaggerParseResult deserialize(JsonNode rootNode, OpenAPI startingModel) {
        SwaggerParseResult result = new SwaggerParseResult();
        try {
            ParseResult rootParse = new ParseResult();
            OpenAPI api = parseRoot(rootNode, rootParse, startingModel);
            result.setOpenAPI(api);
            result.setMessages(rootParse.getMessages());
        } catch (Exception e) {
            result.setMessages(Arrays.asList(e.getMessage()));

        }
        return result;
    }

    public OpenAPI parseRoot(JsonNode node, ParseResult result, OpenAPI startingModel) {
        String location = "";
        OpenAPI openAPI = new OpenAPIImpl();
        if (startingModel != null)
            openAPI = startingModel;
        if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
            ObjectNode rootNode = (ObjectNode) node;

            // required
            String value = getString("openapi", rootNode, true, location, result);

            // we don't even try if the version isn't there
            if (value == null || !value.startsWith("3.0")) {
                return null;
            }
            openAPI.setOpenapi(value);

            ObjectNode obj = getObject("info", rootNode, true, location, result);
            if (obj != null) {
                Info info = getInfo(obj, "info", result);
                openAPI.setInfo(info);
            }

            obj = getObject("paths", rootNode, true, location, result);
            if (obj != null) {
                Paths paths = getPaths(obj, "paths", result);
                openAPI.setPaths(paths);
            }

            obj = getObject("components", rootNode, false, location, result);
            if (obj != null) {
                Components components = getComponents(obj, "components", result);
                openAPI.setComponents(components);
            }

            ArrayNode array = getArray("servers", rootNode, false, location, result);
            if (array != null && array.size() > 0) {
                openAPI.setServers(getServersList(array, String.format("%s.%s'", location, "servers"), result));
            }
            // Don't add server if not specified by user - Liberty servers will be added in that case
            /*
             * else {
             * Server defaultServer = new ServerImpl();
             * defaultServer.setUrl("/");
             * List<Server> servers = new ArrayList<>();
             * servers.add(defaultServer);
             * openAPI.setServers(servers);
             * }
             */

            obj = getObject("externalDocs", rootNode, false, location, result);
            if (obj != null) {
                ExternalDocumentation externalDocs = getExternalDocs(obj, "externalDocs", result);
                openAPI.setExternalDocs(externalDocs);
            }

            array = getArray("tags", rootNode, false, location, result);
            if (array != null && array.size() > 0) {
                openAPI.setTags(getTagList(array, "tags", result));
            }

            array = getArray("security", rootNode, false, location, result);
            if (array != null && array.size() > 0) {
                List<SecurityRequirement> securityRequirements = getSecurityRequirementsList(array, "security", result);
                if (securityRequirements != null && securityRequirements.size() > 0) {
                    openAPI.setSecurity(securityRequirements);
                }
            }

            Map<String, Object> extensions = getExtensions(rootNode);
            if (extensions != null && extensions.size() > 0) {
                openAPI.setExtensions(extensions);
            }

            Set<String> keys = getKeys(rootNode);
            for (String key : keys) {
                if (!ROOT_KEYS.contains(key) && !key.startsWith("x-")) {
                    result.extra(location, key, node.get(key));
                }
            }

        } else {
            result.invalidType(location, "openapi", "object", node);
            result.invalid();
            return null;
        }

        return openAPI;
    }

    public Map<String, Object> getExtensions(ObjectNode node) {

        Map<String, Object> extensions = new LinkedHashMap<>();

        Set<String> keys = getKeys(node);
        for (String key : keys) {
            if (key.startsWith("x-")) {
                extensions.put(key, Json.mapper().convertValue(node.get(key), Object.class));
            }
        }
        return extensions;

    }

    public Components getComponents(ObjectNode obj, String location, ParseResult result) {
        if (obj == null) {
            return null;
        }
        Components components = new ComponentsImpl();

        ObjectNode node = getObject("schemas", obj, false, location, result);
        if (node != null) {
            components.setSchemas(getSchemas(node, String.format("%s.%s'", location, "schemas"), result));
        }

        node = getObject("responses", obj, false, location, result);
        if (node != null) {
            components.setResponses(getResponses(node, String.format("%s.%s'", location, "responses"), result));
        }

        node = getObject("parameters", obj, false, location, result);
        if (node != null) {
            components.setParameters(getParameters(node, String.format("%s.%s'", location, "parameters"), result));
        }
        node = getObject("examples", obj, false, location, result);
        if (node != null) {
            components.setExamples(getExamples(node, String.format("%s.%s'", location, "examples"), result));
        }

        node = getObject("requestBodies", obj, false, location, result);
        if (node != null) {
            components.setRequestBodies(getRequestBodies(node, String.format("%s.%s'", location, "requestBodies"), result));
        }

        node = getObject("headers", obj, false, location, result);
        if (node != null) {
            components.setHeaders(getHeaders(node, String.format("%s.%s'", location, "headers"), result));
        }

        node = getObject("securitySchemes", obj, false, location, result);
        if (node != null) {
            components.setSecuritySchemes(getSecuritySchemes(node, String.format("%s.%s'", location, "securitySchemes"), result));
        }

        node = getObject("links", obj, false, location, result);
        if (node != null) {
            components.setLinks(getLinks(node, String.format("%s.%s'", location, "links"), result));
        }

        node = getObject("callbacks", obj, false, location, result);
        if (node != null) {
            components.setCallbacks(getCallbacks(node, String.format("%s.%s'", location, "callbacks"), result));
        }
        components.setExtensions(new LinkedHashMap<>());

        Map<String, Object> extensions = getExtensions(obj);
        if (extensions != null && extensions.size() > 0) {
            components.setExtensions(extensions);
        }

        Set<String> keys = getKeys(obj);
        for (String key : keys) {
            if (!COMPONENTS_KEYS.contains(key) && !key.startsWith("x-")) {
                result.extra(location, key, obj.get(key));
            }
        }

        return components;
    }

    public List<Tag> getTagList(ArrayNode obj, String location, ParseResult result) {
        if (obj == null) {
            return null;
        }
        List<Tag> tags = new ArrayList<>();
        for (JsonNode item : obj) {
            if (item.getNodeType().equals(JsonNodeType.OBJECT)) {
                Tag tag = getTag((ObjectNode) item, location, result);
                if (tag != null) {
                    tags.add(tag);
                }
            }
        }
        return tags;
    }

    public Tag getTag(ObjectNode obj, String location, ParseResult result) {
        if (obj == null) {
            return null;
        }

        Tag tag = new TagImpl();

        String value = getString("name", obj, true, location, result);
        if (StringUtils.isNotBlank(value)) {
            tag.setName(value);
        }

        value = getString("description", obj, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            tag.setDescription(value);
        }

        ObjectNode docs = getObject("externalDocs", obj, false, location, result);
        ExternalDocumentation externalDocs = getExternalDocs(docs, String.format("%s.%s'", location, "externalDocs"), result);
        if (externalDocs != null) {
            tag.setExternalDocs(externalDocs);
        }

        Map<String, Object> extensions = getExtensions(obj);
        if (extensions != null && extensions.size() > 0) {
            tag.setExtensions(extensions);
        }

        Set<String> keys = getKeys(obj);
        for (String key : keys) {
            if (!TAG_KEYS.contains(key) && !key.startsWith("x-")) {
                result.extra(location, key, obj.get(key));
            }
        }

        return tag;
    }

    public List<Server> getServersList(ArrayNode obj, String location, ParseResult result) {

        List<Server> servers = new ArrayList<>();
        if (obj == null) {
            return null;

        }
        for (JsonNode item : obj) {
            if (item.getNodeType().equals(JsonNodeType.OBJECT)) {
                Server server = getServer((ObjectNode) item, location, result);
                if (server != null) {
                    servers.add(server);
                }

                // Don't add server if not specified by user - Liberty servers will be added in that case
                /*
                 * else {
                 * Server defaultServer = new ServerImpl();
                 * defaultServer.setUrl("/");
                 * servers.add(defaultServer);
                 * }
                 */
            }
        }

        if (servers.isEmpty()) {
            //No servers were added, return null
            return null;
        }

        return servers;
    }

    public Server getServer(ObjectNode obj, String location, ParseResult result) {
        if (obj == null) {
            return null;
        }

        Server server = new ServerImpl();

        String value = getString("url", obj, true, location, result);
        if (StringUtils.isNotBlank(value)) {
            server.setUrl(value);
        }

        value = getString("description", obj, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            server.setDescription(value);
        }
        if (obj.get("variables") != null) {
            ObjectNode variables = getObject("variables", obj, false, location, result);
            ServerVariables serverVariables = getServerVariables(variables, String.format("%s.%s'", location, "variables"), result);
            if (serverVariables != null && serverVariables.size() > 0) {
                server.setVariables(serverVariables);
            }
        }

        Map<String, Object> extensions = getExtensions(obj);
        if (extensions != null && extensions.size() > 0) {
            server.setExtensions(extensions);
        }

        Set<String> keys = getKeys(obj);
        for (String key : keys) {
            if (!SERVER_KEYS.contains(key) && !key.startsWith("x-")) {
                result.extra(location, key, obj.get(key));
            }
        }

        return server;
    }

    public ServerVariables getServerVariables(ObjectNode obj, String location, ParseResult result) {
        ServerVariables serverVariables = new ServerVariablesImpl();
        if (obj == null) {
            return null;
        }

        Set<String> serverKeys = getKeys(obj);
        for (String serverName : serverKeys) {
            JsonNode serverValue = obj.get(serverName);
            ObjectNode server = (ObjectNode) serverValue;
            ServerVariable serverVariable = getServerVariable(server, String.format("%s.%s'", location, serverName), result);
            serverVariables.addServerVariable(serverName, serverVariable);
        }

        return serverVariables;
    }

    public ServerVariable getServerVariable(ObjectNode obj, String location, ParseResult result) {
        if (obj == null) {
            return null;
        }

        ServerVariable serverVariable = new ServerVariableImpl();

        ArrayNode arrayNode = getArray("enum", obj, false, location, result);
        if (arrayNode != null) {
            List<String> _enum = new ArrayList<>();
            for (JsonNode n : arrayNode) {
                if (n.isValueNode()) {
                    _enum.add(n.asText());
                    serverVariable.setEnumeration(_enum);
                } else {
                    result.invalidType(location, "enum", "value", n);
                }
            }
        }
        String value = getString("default", obj, true, String.format("%s.%s'", location, "default"), result);
        if (StringUtils.isNotBlank(value)) {
            serverVariable.setDefaultValue(value);
        }

        value = getString("description", obj, false, String.format("%s.%s'", location, "description"), result);
        if (StringUtils.isNotBlank(value)) {
            serverVariable.setDescription(value);
        }

        Map<String, Object> extensions = getExtensions(obj);
        if (extensions != null && extensions.size() > 0) {
            serverVariable.setExtensions(extensions);
        }

        Set<String> keys = getKeys(obj);
        for (String key : keys) {
            if (!SERVER_VARIABLE_KEYS.contains(key) && !key.startsWith("x-")) {
                result.extra(location, key, obj.get(key));
            }
        }

        return serverVariable;
    }

    //PathsObject

    public Paths getPaths(ObjectNode obj, String location, ParseResult result) {
        final Paths paths = new PathsImpl();
        if (obj == null) {
            return null;
        }
        Set<String> pathKeys = getKeys(obj);
        for (String pathName : pathKeys) {
            JsonNode pathValue = obj.get(pathName);
            if (pathName.startsWith("x-")) {
                result.unsupported(location, pathName, pathValue);
            } else {
                if (!pathValue.getNodeType().equals(JsonNodeType.OBJECT)) {
                    result.invalidType(location, pathName, "object", pathValue);
                } else {
                    ObjectNode path = (ObjectNode) pathValue;
                    PathItem pathObj = getPathItem(path, String.format("%s.%s'", location, pathName), result);
                    paths.put(pathName, pathObj);
                }
            }
        }
        return paths;
    }

    public PathItem getPathItem(ObjectNode obj, String location, ParseResult result) {

        PathItem pathItem = new PathItemImpl();

        if (obj.get("$ref") != null) {
            JsonNode ref = obj.get("$ref");

            if (ref.getNodeType().equals(JsonNodeType.STRING)) {
                pathItem.setRef(ref.asText());
                return pathItem.ref((ref.asText()));
            } else if (ref.getNodeType().equals(JsonNodeType.OBJECT)) {
                ObjectNode node = (ObjectNode) ref;

                // extra keys
                Set<String> keys = getKeys(node);
                for (String key : keys) {
                    result.extra(location, key, node.get(key));
                }
            }
            return null;
        }

        String value = getString("summary", obj, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            pathItem.setSummary(value);
        }

        value = getString("description", obj, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            pathItem.setDescription(value);
        }

        ArrayNode parameters = getArray("parameters", obj, false, location, result);
        if (parameters != null && parameters.size() > 0) {
            pathItem.setParameters(getParameterList(parameters, location, result));
        }

        ArrayNode servers = getArray("servers", obj, false, location, result);
        if (servers != null && servers.size() > 0) {
            pathItem.setServers(getServersList(servers, location, result));
        }

        ObjectNode node = getObject("get", obj, false, location, result);
        if (node != null) {
            Operation operation = getOperation(node, location + "(get)", result);
            if (operation != null) {
                pathItem.setGET(operation);
            }
        }
        node = getObject("put", obj, false, location, result);
        if (node != null) {
            Operation operation = getOperation(node, location + "(put)", result);
            if (operation != null) {
                pathItem.setPUT(operation);
            }
        }
        node = getObject("post", obj, false, location, result);
        if (node != null) {
            Operation operation = getOperation(node, location + "(post)", result);
            if (operation != null) {
                pathItem.setPOST(operation);
            }
        }
        node = getObject("head", obj, false, location, result);
        if (node != null) {
            Operation operation = getOperation(node, location + "(head)", result);
            if (operation != null) {
                pathItem.setHEAD(operation);
            }
        }
        node = getObject("delete", obj, false, location, result);
        if (node != null) {
            Operation operation = getOperation(node, location + "(delete)", result);
            if (operation != null) {
                pathItem.setDELETE(operation);
            }
        }
        node = getObject("patch", obj, false, location, result);
        if (node != null) {
            Operation operation = getOperation(node, location + "(patch)", result);
            if (operation != null) {
                pathItem.setPATCH(operation);
            }
        }
        node = getObject("options", obj, false, location, result);
        if (node != null) {
            Operation operation = getOperation(node, location + "(options)", result);
            if (operation != null) {
                pathItem.setOPTIONS(operation);
            }
        }

        Map<String, Object> extensions = getExtensions(obj);
        if (extensions != null && extensions.size() > 0) {
            pathItem.setExtensions(extensions);
        }

        Set<String> keys = getKeys(obj);
        for (String key : keys) {
            if (!PATHITEM_KEYS.contains(key) && !key.startsWith("x-")) {
                result.extra(location, key, obj.get(key));
            }
        }

        return pathItem;
    }

    public ExternalDocumentation getExternalDocs(ObjectNode node, String location, ParseResult result) {
        ExternalDocumentation externalDocs = null;

        if (node != null) {
            externalDocs = new ExternalDocumentationImpl();

            String value = getString("description", node, false, location, result);
            if (StringUtils.isNotBlank(value)) {
                externalDocs.description(value);
            }

            value = getString("url", node, true, location, result);
            if (StringUtils.isNotBlank(value)) {
                externalDocs.url(value);
            }

            Map<String, Object> extensions = getExtensions(node);
            if (extensions != null && extensions.size() > 0) {
                externalDocs.setExtensions(extensions);
            }

            Set<String> keys = getKeys(node);
            for (String key : keys) {
                if (!EXTERNAL_DOCS_KEYS.contains(key) && !key.startsWith("x-")) {
                    result.extra(location, key, node.get(key));
                }
            }
        }

        return externalDocs;
    }

    public String getString(String key, ObjectNode node, boolean required, String location, ParseResult result) {
        String value = null;
        JsonNode v = node.get(key);
        if (node == null || v == null) {
            if (required) {
                result.missing(location, key);
                result.invalid();
            }
        } else if (!v.isValueNode()) {
            result.invalidType(location, key, "string", node);
        } else {
            value = v.asText();
        }
        return value;
    }

    public Set<String> getKeys(ObjectNode node) {
        Set<String> keys = new LinkedHashSet<>();
        if (node == null) {
            return keys;
        }

        Iterator<String> it = node.fieldNames();
        while (it.hasNext()) {
            keys.add(it.next());
        }

        return keys;
    }

    public ObjectNode getObject(String key, ObjectNode node, boolean required, String location, ParseResult result) {
        JsonNode value = node.get(key);
        ObjectNode object = null;
        if (value == null) {
            if (required) {
                result.missing(location, key);
                result.invalid();
            }
        } else if (!value.getNodeType().equals(JsonNodeType.OBJECT)) {
            result.invalidType(location, key, "object", value);
            if (required) {
                result.invalid();
            }
        } else {
            object = (ObjectNode) value;
        }
        return object;
    }

    public Info getInfo(ObjectNode node, String location, ParseResult result) {
        if (node == null)
            return null;

        Info info = new InfoImpl();

        String value = getString("title", node, true, location, result);
        if (StringUtils.isNotBlank(value)) {
            info.setTitle(value);
        }

        value = getString("description", node, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            info.setDescription(value);
        }

        value = getString("termsOfService", node, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            info.setTermsOfService(value);
        }

        ObjectNode obj = getObject("contact", node, false, "contact", result);
        Contact contact = getContact(obj, String.format("%s.%s'", location, "contact"), result);
        if (obj != null) {
            info.setContact(contact);
        }
        obj = getObject("license", node, false, location, result);
        License license = getLicense(obj, String.format("%s.%s'", location, "license"), result);
        if (obj != null) {
            info.setLicense(license);
        }

        value = getString("version", node, true, location, result);
        if (StringUtils.isNotBlank(value)) {
            info.setVersion(value);
        }

        Map<String, Object> extensions = getExtensions(node);
        if (extensions != null && extensions.size() > 0) {
            info.setExtensions(extensions);
        }

        Set<String> keys = getKeys(node);
        for (String key : keys) {
            if (!INFO_KEYS.contains(key) && !key.startsWith("x-")) {
                result.extra(location, key, node.get(key));
            }
        }

        return info;
    }

    public License getLicense(ObjectNode node, String location, ParseResult result) {
        if (node == null)
            return null;

        License license = new LicenseImpl();

        String value = getString("name", node, true, location, result);
        if (StringUtils.isNotBlank(value)) {
            license.setName(value);
        }

        value = getString("url", node, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            license.setUrl(value);
        }

        Map<String, Object> extensions = getExtensions(node);
        if (extensions != null && extensions.size() > 0) {
            license.setExtensions(extensions);
        }

        Set<String> keys = getKeys(node);
        for (String key : keys) {
            if (!LICENSE_KEYS.contains(key) && !key.startsWith("x-")) {
                result.extra(location, key, node.get(key));
            }
        }

        return license;
    }

    public Contact getContact(ObjectNode node, String location, ParseResult result) {
        if (node == null)
            return null;

        Contact contact = new ContactImpl();

        String value = getString("name", node, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            contact.setName(value);
        }

        value = getString("url", node, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            contact.setUrl(value);
        }

        value = getString("email", node, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            contact.setEmail(value);
        }

        Map<String, Object> extensions = getExtensions(node);
        if (extensions != null && extensions.size() > 0) {
            contact.setExtensions(extensions);
        }

        Set<String> keys = getKeys(node);
        for (String key : keys) {
            if (!CONTACT_KEYS.contains(key) && !key.startsWith("x-")) {
                result.extra(location, key, node.get(key));
            }
        }

        return contact;
    }

    public Content getContent(ObjectNode node, String location, ParseResult result) {
        if (node == null) {
            return null;
        }
        Content content = new ContentImpl();

        Set<String> keys = getKeys(node);
        for (String key : keys) {
            MediaType mediaType = getMediaType((ObjectNode) node.get(key), location, result);
            if (mediaType != null) {
                content.addMediaType(key, mediaType);
            }
        }

        return content;
    }

    public MediaType getMediaType(ObjectNode contentNode, String location, ParseResult result) {
        if (contentNode == null) {
            return null;
        }
        MediaType mediaType = new MediaTypeImpl();

        ObjectNode schemaObject = getObject("schema", contentNode, false, location, result);
        if (schemaObject != null) {
            mediaType.setSchema(getSchema(schemaObject, String.format("%s.%s'", location, "schema"), result));
        }

        ObjectNode encodingObject = getObject("encoding", contentNode, false, location, result);
        if (encodingObject != null) {
            mediaType.setEncoding(getEncodingMap(encodingObject, String.format("%s.%s'", location, "encoding"), result));
        }
        Map<String, Object> extensions = getExtensions(contentNode);
        if (extensions != null && extensions.size() > 0) {
            mediaType.setExtensions(extensions);
        }

        ObjectNode examplesObject = getObject("examples", contentNode, false, location, result);
        if (examplesObject != null) {
            mediaType.setExamples(getExamples(examplesObject, String.format("%s.%s'", location, "examples"), result));
        }

        Object example = getAnyExample("example", contentNode, location, result);
        if (example != null) {
            mediaType.setExample(example);
        }

        Set<String> keys = getKeys(contentNode);
        for (String key : keys) {
            if (!MEDIATYPE_KEYS.contains(key) && !key.startsWith("x-")) {
                result.extra(location, key, contentNode.get(key));
            }
        }

        return mediaType;
    }

    public Map<String, Encoding> getEncodingMap(ObjectNode node, String location, ParseResult result) {
        if (node == null) {
            return null;
        }
        Map<String, Encoding> encodings = new LinkedHashMap<>();
        Set<String> keys = getKeys(node);
        for (String key : keys) {
            Encoding encoding = getEncoding((ObjectNode) node.get(key), location, result);
            if (encoding != null) {
                encodings.put(key, encoding);
            }
        }
        return encodings;
    }

    public Encoding getEncoding(ObjectNode node, String location, ParseResult result) {
        if (node == null) {
            return null;
        }

        Encoding encoding = new EncodingImpl();

        String value = getString("contentType", node, true, location, result);
        encoding.setContentType(value);

        value = getString("style", node, false, location, result);

        if (StringUtils.isBlank(value)) {
            encoding.setStyle(Encoding.Style.FORM);
        } else {
            if (value.equals(Encoding.Style.FORM.toString())) {
                encoding.setStyle(Encoding.Style.FORM);
            } else if (value.equals(Encoding.Style.DEEP_OBJECT.toString())) {
                encoding.setStyle(Encoding.Style.DEEP_OBJECT);
            } else if (value.equals(Encoding.Style.PIPE_DELIMITED.toString())) {
                encoding.setStyle(Encoding.Style.PIPE_DELIMITED);
            } else if (value.equals(Encoding.Style.SPACE_DELIMITED.toString())) {
                encoding.setStyle(Encoding.Style.SPACE_DELIMITED);
            } else {
                result.invalidType(location, "style", "string", node);
            }
        }

        Boolean explode = getBoolean("explode", node, false, location, result);
        if (explode != null) {
            encoding.setExplode(explode);
        }

        Boolean allowReserved = getBoolean("allowReserved", node, false, location, result);
        if (allowReserved != null) {
            encoding.setAllowReserved(allowReserved);
        }
        ObjectNode headersObject = getObject("headers", node, false, location, result);
        if (headersObject != null) {
            encoding.setHeaders(getHeaders(headersObject, location, result));
        }

        Map<String, Object> extensions = getExtensions(node);
        if (extensions != null && extensions.size() > 0) {
            encoding.setExtensions(extensions);
        }

        Set<String> keys = getKeys(node);
        for (String key : keys) {
            if (!ENCODING_KEYS.contains(key) && !key.startsWith("x-")) {
                result.extra(location, key, node.get(key));
            }
        }

        return encoding;
    }

    public Map<String, Link> getLinks(ObjectNode obj, String location, ParseResult result) {
        if (obj == null) {
            return null;
        }
        Map<String, Link> links = new LinkedHashMap<>();

        Set<String> linkKeys = getKeys(obj);
        for (String linkName : linkKeys) {
            JsonNode linkValue = obj.get(linkName);
            if (!linkValue.getNodeType().equals(JsonNodeType.OBJECT)) {
                result.invalidType(location, linkName, "object", linkValue);
            } else {
                ObjectNode link = (ObjectNode) linkValue;
                Link linkObj = getLink(link, String.format("%s.%s'", location, linkName), result);
                if (linkObj != null) {
                    links.put(linkName, linkObj);
                }
            }

        }
        return links;
    }

    public Link getLink(ObjectNode linkNode, String location, ParseResult result) {
        if (linkNode == null) {
            return null;
        }

        Link link = new LinkImpl();

        JsonNode ref = linkNode.get("$ref");
        if (ref != null) {
            if (ref.getNodeType().equals(JsonNodeType.STRING)) {
                link.setRef(ref.asText());
                return link.ref(ref.asText());
            } else {
                result.invalidType(location, "$ref", "string", linkNode);
                return null;
            }
        }

        String value = getString("operationRef", linkNode, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            link.setOperationRef(value);
        }

        value = getString("operationId", linkNode, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            link.setOperationId(value);
        }

        ObjectNode parametersObject = getObject("parameters", linkNode, false, location, result);
        if (parametersObject != null) {
            link.setParameters(getLinkParameters(parametersObject, location, result));
        }

//        ObjectNode headerObject = getObject("headers", linkNode, false, location, result);
//        if (headerObject != null) {
//            link.setHeaders(getHeaders(headerObject, location, result));
//        }

        ObjectNode serverObject = getObject("server", linkNode, false, location, result);
        if (serverObject != null) {
            link.setServer(getServer(serverObject, location, result));
        }

        value = getString("description", linkNode, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            link.setDescription(value);
        }

        Map<String, Object> extensions = getExtensions(linkNode);
        if (extensions != null && extensions.size() > 0) {
            link.setExtensions(extensions);
        }

        Set<String> keys = getKeys(linkNode);
        for (String key : keys) {
            if (!LINK_KEYS.contains(key) && !key.startsWith("x-")) {
                result.extra(location, key, linkNode.get(key));
            }
        }

        return link;
    }

    private Map<String, Object> getLinkParameters(ObjectNode parametersObject, String location, ParseResult result) {

        Map<String, Object> linkParameters = new LinkedHashMap<>();

        Set<String> keys = getKeys(parametersObject);
        for (String name : keys) {
            JsonNode value = parametersObject.get(name);
            linkParameters.put(name, value.asText());
        }

        return linkParameters;
    }

    public Map<String, Callback> getCallbacks(ObjectNode node, String location, ParseResult result) {
        if (node == null) {
            return null;
        }
        Map<String, Callback> callbacks = new LinkedHashMap<>();
        Set<String> keys = getKeys(node);
        for (String key : keys) {
            Callback callback = getCallback((ObjectNode) node.get(key), location, result);
            if (callback != null) {
                callbacks.put(key, callback);
            }
        }
        return callbacks;
    }

    public Callback getCallback(ObjectNode node, String location, ParseResult result) {
        if (node == null) {
            return null;
        }

        Callback callback = new CallbackImpl();

        Set<String> keys = getKeys(node);
        for (String name : keys) {
            JsonNode value = node.get(name);
            if (node != null) {
                JsonNode ref = node.get("$ref");
                if (ref != null) {
                    if (ref.getNodeType().equals(JsonNodeType.STRING)) {
                        PathItem pathItem = new PathItemImpl();
                        return callback.addPathItem(name, pathItem.ref(ref.asText()));
                    } else {
                        result.invalidType(location, "$ref", "string", node);
                        return null;
                    }
                }
                callback.addPathItem(name, getPathItem((ObjectNode) value, location, result));

                Map<String, Object> extensions = getExtensions(node);
                if (extensions != null && extensions.size() > 0) {
                    callback.setExtensions(extensions);
                }
            }
        }

        return callback;
    }

    public XML getXml(ObjectNode node, String location, ParseResult result) {
        if (node == null) {
            return null;
        }
        XML xml = new XMLImpl();

        String value = getString("name", node, false, String.format("%s.%s'", location, "name"), result);
        if (StringUtils.isNotBlank(value)) {
            xml.setName(value);
        }

        value = getString("namespace", node, false, String.format("%s.%s'", location, "namespace"), result);
        if (StringUtils.isNotBlank(value)) {
            xml.setNamespace(value);
        }

        value = getString("prefix", node, false, String.format("%s.%s'", location, "prefix"), result);
        if (StringUtils.isNotBlank(value)) {
            xml.setPrefix(value);
        }

        Boolean attribute = getBoolean("attribute", node, false, location, result);
        if (attribute != null) {
            xml.setAttribute(attribute);
        }

        Boolean wrapped = getBoolean("wrapped", node, false, location, result);
        if (wrapped != null) {
            xml.setWrapped(wrapped);
        }

        Map<String, Object> extensions = getExtensions(node);
        if (extensions != null && extensions.size() > 0) {
            xml.setExtensions(extensions);
        }

        Set<String> keys = getKeys(node);
        for (String key : keys) {
            if (!XML_KEYS.contains(key) && !key.startsWith("x-")) {
                result.extra(location, key, node.get(key));
            }
        }

        return xml;

    }

    public ArrayNode getArray(String key, ObjectNode node, boolean required, String location, ParseResult result) {
        JsonNode value = node.get(key);
        ArrayNode arrayNode = null;
        if (value == null) {
            if (required) {
                result.missing(location, key);
                result.invalid();
            }
        } else if (!value.getNodeType().equals(JsonNodeType.ARRAY)) {
            result.invalidType(location, key, "array", value);
        } else {
            arrayNode = (ArrayNode) value;
        }
        return arrayNode;
    }

    public Boolean getBoolean(String key, ObjectNode node, boolean required, String location, ParseResult result) {
        Boolean value = null;
        JsonNode v = node.get(key);
        if (node == null || v == null) {
            if (required) {
                result.missing(location, key);
                result.invalid();
            }
        } else {
            if (v.getNodeType().equals(JsonNodeType.BOOLEAN)) {
                value = v.asBoolean();
            } else if (v.getNodeType().equals(JsonNodeType.STRING)) {
                String stringValue = v.textValue();
                return Boolean.parseBoolean(stringValue);
            }
        }
        return value;
    }

    public BigDecimal getBigDecimal(String key, ObjectNode node, boolean required, String location, ParseResult result) {
        BigDecimal value = null;
        JsonNode v = node.get(key);
        if (node == null || v == null) {
            if (required) {
                result.missing(location, key);
                result.invalid();
            }
        } else if (v.getNodeType().equals(JsonNodeType.NUMBER)) {
            value = new BigDecimal(v.asText());
        } else if (!v.isValueNode()) {
            result.invalidType(location, key, "double", node);
        }
        return value;
    }

    public Integer getInteger(String key, ObjectNode node, boolean required, String location, ParseResult result) {
        Integer value = null;
        JsonNode v = node.get(key);
        if (node == null || v == null) {
            if (required) {
                result.missing(location, key);
                result.invalid();
            }
        } else if (v.getNodeType().equals(JsonNodeType.NUMBER)) {
            value = v.intValue();
        } else if (!v.isValueNode()) {
            result.invalidType(location, key, "integer", node);
        }
        return value;
    }

    public Map<String, Parameter> getParameters(ObjectNode obj, String location, ParseResult result) {
        if (obj == null) {
            return null;
        }
        Map<String, Parameter> parameters = new LinkedHashMap<>();

        Set<String> parameterKeys = getKeys(obj);
        for (String parameterName : parameterKeys) {
            JsonNode parameterValue = obj.get(parameterName);
            if (parameterValue.getNodeType().equals(JsonNodeType.OBJECT)) {
                ObjectNode parameterObj = (ObjectNode) parameterValue;
                if (parameterObj != null) {
                    Parameter parameter = getParameter(parameterObj, String.format("%s.%s'", location, parameterName), result);
                    if (parameter != null) {
                        parameters.put(parameterName, parameter);
                    }
                }
            }
        }
        return parameters;
    }

    public List<Parameter> getParameterList(ArrayNode obj, String location, ParseResult result) {
        List<Parameter> parameters = new ArrayList<>();
        if (obj == null) {
            return parameters;
        }
        for (JsonNode item : obj) {
            if (item.getNodeType().equals(JsonNodeType.OBJECT)) {
                Parameter parameter = getParameter((ObjectNode) item, location, result);
                if (parameter != null) {
                    parameters.add(parameter);
                }
            }
        }
        return parameters;
    }

    public Parameter getParameter(ObjectNode obj, String location, ParseResult result) {
        if (obj == null) {
            return null;
        }

        Parameter parameter = null;

        JsonNode ref = obj.get("$ref");
        if (ref != null) {
            if (ref.getNodeType().equals(JsonNodeType.STRING)) {
                parameter = new ParameterImpl();
                parameter.setRef(ref.asText());
                return (parameter.ref(ref.asText()));
            } else {
                result.invalidType(location, "$ref", "string", obj);
                return null;
            }
        }

        String l = null;
        JsonNode ln = obj.get("name");
        if (ln != null) {
            l = ln.asText();
        } else {
            l = "['unknown']";
        }
        location += ".[" + l + "]";

        String value = getString("in", obj, true, location, result);

        if (StringUtils.isBlank(value)) {
            return null;
        }

        if (QUERY_PARAMETER.equals(value)) {
            parameter = new ParameterImpl().in(In.QUERY);
        } else if (HEADER_PARAMETER.equals(value)) {
            parameter = new ParameterImpl().in(In.HEADER);
        } else if (PATH_PARAMETER.equals(value)) {
            parameter = new ParameterImpl().in(In.PATH);
        } else if (COOKIE_PARAMETER.equals(value)) {
            parameter = new ParameterImpl().in(In.COOKIE);
        }

        if (parameter == null) {
            result.invalidType(location, "in", "string", obj);
            return null;
        }

        value = getString("name", obj, true, location, result);
        if (StringUtils.isNotBlank(value)) {
            parameter.setName(value);
        }

        value = getString("description", obj, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            parameter.setDescription(value);
        }

        Boolean required = getBoolean("required", obj, false, location, result);
        if (required != null) {
            parameter.setRequired(required);
        } else {
            parameter.setRequired(false);
        }

        Boolean deprecated = getBoolean("deprecated", obj, false, location, result);
        if (deprecated != null) {
            parameter.setDeprecated(deprecated);
        }

        Boolean allowEmptyValue = getBoolean("allowEmptyValue", obj, false, location, result);
        if (allowEmptyValue != null) {
            parameter.setAllowEmptyValue(allowEmptyValue);
        }

        value = getString("style", obj, false, location, result);
        setStyle(value, parameter, location, obj, result);

        Boolean explode = getBoolean("explode", obj, false, location, result);
        if (explode != null) {
            parameter.setExplode(explode);
        } else if (parameter.getStyle() != null && parameter.getStyle().equals(Parameter.Style.FORM)) {
            parameter.setExplode(Boolean.TRUE);
        } else {
            parameter.setExplode(Boolean.FALSE);
        }

        ObjectNode parameterObject = getObject("schema", obj, false, location, result);
        if (parameterObject != null) {
            parameter.setSchema(getSchema(parameterObject, String.format("%s.%s'", location, "schemas"), result));
        }

        ObjectNode examplesObject = getObject("examples", obj, false, location, result);
        if (examplesObject != null) {
            parameter.setExamples(getExamples(examplesObject, String.format("%s.%s'", location, "examples"), result));
        }

        Object example = getAnyExample("example", obj, location, result);
        if (example != null) {
            parameter.setExample(example);
        }

        ObjectNode contentNode = getObject("content", obj, false, location, result);
        if (contentNode != null) {
            parameter.setContent(getContent(contentNode, String.format("%s.%s'", location, "content"), result));
        }

        Map<String, Object> extensions = getExtensions(obj);
        if (extensions != null && extensions.size() > 0) {
            parameter.setExtensions(extensions);
        }

        Set<String> keys = getKeys(obj);
        for (String key : keys) {
            if (!PARAMETER_KEYS.contains(key) && !key.startsWith("x-")) {
                result.extra(location, key, obj.get(key));
            }
        }

        return parameter;
    }

    public Map<String, Header> getHeaders(ObjectNode obj, String location, ParseResult result) {
        if (obj == null) {
            return null;
        }
        Map<String, Header> headers = new LinkedHashMap<>();

        Set<String> headerKeys = getKeys(obj);
        for (String headerName : headerKeys) {
            JsonNode headerValue = obj.get(headerName);
            if (!headerValue.getNodeType().equals(JsonNodeType.OBJECT)) {
                result.invalidType(location, headerName, "object", headerValue);
            } else {
                ObjectNode header = (ObjectNode) headerValue;
                Header headerObj = getHeader(header, String.format("%s.%s'", location, headerName), result);
                if (headerObj != null) {
                    headers.put(headerName, headerObj);
                }
            }

        }

        return headers;
    }

    public Header getHeader(ObjectNode headerNode, String location, ParseResult result) {
        if (headerNode == null) {
            return null;
        }

        Header header = new HeaderImpl();

        JsonNode ref = headerNode.get("$ref");
        if (ref != null) {
            if (ref.getNodeType().equals(JsonNodeType.STRING)) {
                header.setRef(ref.asText());
                return header.ref(ref.asText());
            } else {
                result.invalidType(location, "$ref", "string", headerNode);
                return null;
            }
        }

        String value = getString("description", headerNode, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            header.setDescription(value);
        }

        Boolean required = getBoolean("required", headerNode, false, location, result);
        if (required != null) {
            header.setRequired(required);
        }

        Boolean deprecated = getBoolean("deprecated", headerNode, false, location, result);
        if (deprecated != null) {
            header.setDeprecated(deprecated);
        }

        Boolean allowEmptyValue = getBoolean("allowEmptyValue", headerNode, false, location, result);
        if (allowEmptyValue != null) {
            header.setAllowEmptyValue(allowEmptyValue);
        }

        Boolean explode = getBoolean("explode", headerNode, false, location, result);
        if (explode != null) {
            header.setExplode(explode);
        } else {
            header.setExplode(Boolean.FALSE);
        }

        header.setStyle(Header.Style.SIMPLE);

        ObjectNode headerObject = getObject("schema", headerNode, false, location, result);
        if (headerObject != null) {
            header.setSchema(getSchema(headerObject, location, result));
        }

        ObjectNode examplesObject = getObject("examples", headerNode, false, location, result);
        if (examplesObject != null) {
            header.setExamples(getExamples(examplesObject, location, result));
        }

        Object example = getAnyExample("example", headerNode, location, result);
        if (example != null) {
            header.setExample(example);
        }

        ObjectNode contentNode = getObject("content", headerNode, false, location, result);
        if (contentNode != null) {
            header.setContent(getContent(contentNode, String.format("%s.%s'", location, "content"), result));
        }

        Map<String, Object> extensions = getExtensions(headerNode);
        if (extensions != null && extensions.size() > 0) {
            header.setExtensions(extensions);
        }

        Set<String> oAuthFlowKeys = getKeys(headerNode);
        for (String key : oAuthFlowKeys) {
            if (!HEADER_KEYS.contains(key) && !key.startsWith("x-")) {
                result.extra(location, key, headerNode.get(key));
            }
        }

        return header;
    }

    public Object getAnyExample(String nodeKey, ObjectNode node, String location, ParseResult result) {
        JsonNode example = node.get(nodeKey);
        if (example != null) {
            if (example.getNodeType().equals(JsonNodeType.STRING)) {
                String value = getString(nodeKey, node, false, location, result);
                if (StringUtils.isNotBlank(value)) {
                    return value;
                }
            }
            if (example.getNodeType().equals(JsonNodeType.NUMBER)) {
                Integer integerExample = getInteger(nodeKey, node, false, location, result);
                if (integerExample != null) {
                    return integerExample;
                } else {
                    BigDecimal bigDecimalExample = getBigDecimal(nodeKey, node, false, location, result);
                    if (bigDecimalExample != null) {
                        return bigDecimalExample;

                    }
                }
            } else if (example.getNodeType().equals(JsonNodeType.OBJECT)) {
                ObjectNode objectValue = getObject(nodeKey, node, false, location, result);
                if (objectValue != null) {
                    return objectValue;
                }
            } else if (example.getNodeType().equals(JsonNodeType.ARRAY)) {
                ArrayNode arrayValue = getArray(nodeKey, node, false, location, result);
                if (arrayValue != null) {
                    return arrayValue;
                }
            }
        }
        return null;
    }

    public Map<String, SecurityScheme> getSecuritySchemes(ObjectNode obj, String location, ParseResult result) {
        if (obj == null) {
            return null;
        }
        Map<String, SecurityScheme> securitySchemes = new LinkedHashMap<>();

        Set<String> securitySchemeKeys = getKeys(obj);
        for (String securitySchemeName : securitySchemeKeys) {
            JsonNode securitySchemeValue = obj.get(securitySchemeName);
            if (!securitySchemeValue.getNodeType().equals(JsonNodeType.OBJECT)) {
                result.invalidType(location, securitySchemeName, "object", securitySchemeValue);
            } else {
                ObjectNode securityScheme = (ObjectNode) securitySchemeValue;
                SecurityScheme securitySchemeObj = getSecurityScheme(securityScheme, String.format("%s.%s'", location, securitySchemeName), result);
                if (securityScheme != null) {
                    securitySchemes.put(securitySchemeName, securitySchemeObj);
                }
            }
        }
        return securitySchemes;
    }

    public SecurityScheme getSecurityScheme(ObjectNode node, String location, ParseResult result) {
        if (node == null) {
            return null;
        }

        SecurityScheme securityScheme = new SecuritySchemeImpl();

        JsonNode ref = node.get("$ref");
        if (ref != null) {
            if (ref.getNodeType().equals(JsonNodeType.STRING)) {
                securityScheme.setRef(ref.asText());
                return securityScheme.ref(ref.asText());
            } else {
                result.invalidType(location, "$ref", "string", node);
                return null;
            }
        }

        boolean descriptionRequired, bearerFormatRequired, nameRequired, inRequired, schemeRequired, flowsRequired, openIdConnectRequired;
        descriptionRequired = bearerFormatRequired = nameRequired = inRequired = schemeRequired = flowsRequired = openIdConnectRequired = false;

        String value = getString("type", node, true, location, result);
        if (StringUtils.isNotBlank(value)) {
            if (SecurityScheme.Type.APIKEY.toString().equals(value)) {
                securityScheme.setType(SecurityScheme.Type.APIKEY);
                nameRequired = inRequired = true;
            } else if (SecurityScheme.Type.HTTP.toString().equals(value)) {
                securityScheme.setType(SecurityScheme.Type.HTTP);
                schemeRequired = true;
            } else if (SecurityScheme.Type.OAUTH2.toString().equals(value)) {
                securityScheme.setType(SecurityScheme.Type.OAUTH2);
                flowsRequired = true;
            } else if (SecurityScheme.Type.OPENIDCONNECT.toString().equals(value)) {
                securityScheme.setType(SecurityScheme.Type.OPENIDCONNECT);
                openIdConnectRequired = true;
            } else {
                result.invalidType(location + ".type", "type", "http|apiKey|oauth2|openIdConnect ", node);
            }
        }
        value = getString("description", node, descriptionRequired, location, result);
        if (StringUtils.isNotBlank(value)) {
            securityScheme.setDescription(value);
        }

        value = getString("name", node, nameRequired, location, result);
        if (StringUtils.isNotBlank(value)) {
            securityScheme.setName(value);
        }

        value = getString("in", node, inRequired, location, result);
        if (StringUtils.isNotBlank(value)) {
            if (QUERY_PARAMETER.equals(value)) {
                securityScheme.setIn(SecurityScheme.In.QUERY);
            } else if (HEADER_PARAMETER.equals(value)) {
                securityScheme.setIn(SecurityScheme.In.HEADER);
            }
        }

        value = getString("scheme", node, schemeRequired, location, result);
        if (StringUtils.isNotBlank(value)) {
            securityScheme.setScheme(value);
        }

        value = getString("bearerFormat", node, bearerFormatRequired, location, result);
        if (StringUtils.isNotBlank(value)) {
            securityScheme.setBearerFormat(value);
        }

        ObjectNode flowsObject = getObject("flows", node, flowsRequired, location, result);
        if (flowsObject != null) {
            securityScheme.setFlows(getOAuthFlows(flowsObject, location, result));
        }

        value = getString("openIdConnectUrl", node, openIdConnectRequired, location, result);
        if (StringUtils.isNotBlank(value)) {
            securityScheme.setOpenIdConnectUrl(value);
        }

        Map<String, Object> extensions = getExtensions(node);
        if (extensions != null && extensions.size() > 0) {
            securityScheme.setExtensions(extensions);
        }

        Set<String> securitySchemeKeys = getKeys(node);
        for (String key : securitySchemeKeys) {
            if (!SECURITY_SCHEME_KEYS.contains(key) && !key.startsWith("x-")) {
                result.extra(location, key, node.get(key));
            }
        }

        return securityScheme;
    }

    public OAuthFlows getOAuthFlows(ObjectNode node, String location, ParseResult result) {
        if (node == null) {
            return null;
        }

        OAuthFlows oAuthFlows = new OAuthFlowsImpl();

        ObjectNode objectNode = getObject("implicit", node, false, location, result);
        if (objectNode != null) {
            oAuthFlows.setImplicit(getOAuthFlow("implicit", objectNode, location, result));
        }

        objectNode = getObject("password", node, false, location, result);
        if (objectNode != null) {
            oAuthFlows.setPassword(getOAuthFlow("password", objectNode, location, result));
        }

        objectNode = getObject("clientCredentials", node, false, location, result);
        if (objectNode != null) {
            oAuthFlows.setClientCredentials(getOAuthFlow("clientCredentials", objectNode, location, result));
        }

        objectNode = getObject("authorizationCode", node, false, location, result);
        if (objectNode != null) {
            oAuthFlows.setAuthorizationCode(getOAuthFlow("authorizationCode", objectNode, location, result));
        }

        Map<String, Object> extensions = getExtensions(node);
        if (extensions != null && extensions.size() > 0) {
            oAuthFlows.setExtensions(extensions);
        }

        Set<String> oAuthFlowKeys = getKeys(node);
        for (String key : oAuthFlowKeys) {
            if (!OAUTHFLOWS_KEYS.contains(key) && !key.startsWith("x-")) {
                result.extra(location, key, node.get(key));
            }
        }

        return oAuthFlows;
    }

    public OAuthFlow getOAuthFlow(String oAuthFlowType, ObjectNode node, String location, ParseResult result) {
        if (node == null) {
            return null;
        }

        OAuthFlow oAuthFlow = new OAuthFlowImpl();

        boolean authorizationUrlRequired, tokenUrlRequired, refreshUrlRequired, scopesRequired;
        authorizationUrlRequired = tokenUrlRequired = refreshUrlRequired = false;
        scopesRequired = true;
        switch (oAuthFlowType) {
            case "implicit":
                authorizationUrlRequired = true;
                break;
            case "password":
                tokenUrlRequired = true;
                break;
            case "clientCredentials":
                tokenUrlRequired = true;
                break;
            case "authorizationCode":
                authorizationUrlRequired = tokenUrlRequired = true;
                break;
        }

        String value = getString("authorizationUrl", node, authorizationUrlRequired, location, result);
        if (StringUtils.isNotBlank(value)) {
            oAuthFlow.setAuthorizationUrl(value);
        }

        value = getString("tokenUrl", node, tokenUrlRequired, location, result);
        if (StringUtils.isNotBlank(value)) {
            oAuthFlow.setTokenUrl(value);
        }

        value = getString("refreshUrl", node, refreshUrlRequired, location, result);
        if (StringUtils.isNotBlank(value)) {
            oAuthFlow.setRefreshUrl(value);
        }

        ObjectNode scopesObject = getObject("scopes", node, scopesRequired, location, result);

        Scopes scope = new ScopesImpl();
        Set<String> keys = getKeys(scopesObject);
        for (String name : keys) {
            JsonNode scopeValue = scopesObject.get(name);
            if (scopesObject != null) {
                scope.addScope(name, scopeValue.asText());
                oAuthFlow.setScopes(scope);
            }
        }

        Map<String, Object> extensions = getExtensions(node);
        if (extensions != null && extensions.size() > 0) {
            oAuthFlow.setExtensions(extensions);
        }

        Set<String> oAuthFlowKeys = getKeys(node);
        for (String key : oAuthFlowKeys) {
            if (!OAUTHFLOW_KEYS.contains(key) && !key.startsWith("x-")) {
                result.extra(location, key, node.get(key));
            }
        }

        return oAuthFlow;
    }

    public Map<String, Schema> getSchemas(ObjectNode obj, String location, ParseResult result) {
        if (obj == null) {
            return null;
        }
        Map<String, Schema> schemas = new LinkedHashMap<>();

        Set<String> schemaKeys = getKeys(obj);
        for (String schemaName : schemaKeys) {
            JsonNode schemaValue = obj.get(schemaName);
            if (!schemaValue.getNodeType().equals(JsonNodeType.OBJECT)) {
                result.invalidType(location, schemaName, "object", schemaValue);
            } else {
                ObjectNode schema = (ObjectNode) schemaValue;
                Schema schemaObj = getSchema(schema, String.format("%s.%s'", location, schemaName), result);
                if (schemaObj != null) {
                    schemas.put(schemaName, schemaObj);
                }
            }
        }

        return schemas;
    }

    public Discriminator getDiscriminator(ObjectNode node, String location, ParseResult result) {
        Discriminator discriminator = new DiscriminatorImpl();

        String value = getString("propertyName", node, true, location, result);
        if (StringUtils.isNotBlank(value)) {
            discriminator.setPropertyName(value);
        }

        ObjectNode mappingNode = getObject("mapping", node, false, location, result);
        if (mappingNode != null) {
            Map<String, String> mapping = new LinkedHashMap<>();
            Set<String> keys = getKeys(mappingNode);
            for (String key : keys) {
                mapping.put(key, mappingNode.get(key).asText());
            }
            discriminator.setMapping(mapping);
        }

        return discriminator;

    }

    public Schema getSchema(ObjectNode node, String location, ParseResult result) {
        if (node == null) {
            return null;
        }

        Schema schema = null;
        ArrayNode oneOfArray = getArray("oneOf", node, false, location, result);
        ArrayNode allOfArray = getArray("allOf", node, false, location, result);
        ArrayNode anyOfArray = getArray("anyOf", node, false, location, result);
        ObjectNode itemsNode = getObject("items", node, false, location, result);

        if (allOfArray != null) {
            Schema allOfList = new SchemaImpl();
            for (JsonNode n : allOfArray) {
                if (n.isObject()) {
                    schema = getSchema((ObjectNode) n, location, result);
                    allOfList.addAllOf(schema);
                }
            }
            schema = allOfList;
        } else if (anyOfArray != null) {
            Schema anyOfList = new SchemaImpl();
            for (JsonNode n : anyOfArray) {
                if (n.isObject()) {
                    schema = getSchema((ObjectNode) n, location, result);
                    anyOfList.addAnyOf(schema);
                }
            }
            schema = anyOfList;
        } else if (oneOfArray != null) {
            Schema oneOfList = new SchemaImpl();
            for (JsonNode n : oneOfArray) {
                if (n.isObject()) {
                    schema = getSchema((ObjectNode) n, location, result);
                    oneOfList.addOneOf(schema);
                }
            }
            schema = oneOfList;
        } else if (itemsNode != null) {
            Schema items = new SchemaImpl().type(SchemaType.ARRAY);
            if (itemsNode.getNodeType().equals(JsonNodeType.OBJECT)) {
                items.setItems(getSchema(itemsNode, location, result));
            } else if (itemsNode.getNodeType().equals(JsonNodeType.ARRAY)) {
                for (JsonNode n : itemsNode) {
                    if (n.isValueNode()) {
                        items.setItems(getSchema(itemsNode, location, result));
                    }
                }
            }
            schema = items;
        } else {
            schema = SchemaTypeUtil.createSchemaByType(node);
        }

        JsonNode ref = node.get("$ref");
        if (ref != null) {
            if (ref.getNodeType().equals(JsonNodeType.STRING)) {
                schema.setRef(ref.asText());
                return schema.ref(ref.asText());
            } else {
                result.invalidType(location, "$ref", "string", node);
                return null;
            }
        }

        String value = getString("title", node, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            schema.setTitle(value);
        }

        ObjectNode discriminatorNode = getObject("discriminator", node, false, location, result);
        if (discriminatorNode != null) {
            schema.setDiscriminator(getDiscriminator(discriminatorNode, location, result));
        }

        BigDecimal bigDecimal = getBigDecimal("multipleOf", node, false, location, result);
        if (bigDecimal != null) {
            schema.setMultipleOf(bigDecimal);
        }

        bigDecimal = getBigDecimal("maximum", node, false, location, result);
        if (bigDecimal != null) {
            schema.setMaximum(bigDecimal);
        }

        Boolean bool = getBoolean("exclusiveMaximum", node, false, location, result);
        if (bool != null) {
            schema.setExclusiveMaximum(bool);
        }

        bigDecimal = getBigDecimal("minimum", node, false, location, result);
        if (bigDecimal != null) {
            schema.setMinimum(bigDecimal);
        }

        bool = getBoolean("exclusiveMinimum", node, false, location, result);
        if (bool != null) {
            schema.setExclusiveMinimum(bool);
        }

        Integer integer = getInteger("minLength", node, false, location, result);
        if (integer != null) {
            schema.setMinLength(integer);
        }

        integer = getInteger("maxLength", node, false, location, result);
        if (integer != null) {
            schema.setMaxLength(integer);
        }

        String pattern = getString("pattern", node, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            schema.setPattern(pattern);
        }

        integer = getInteger("maxItems", node, false, location, result);
        if (integer != null) {
            schema.setMaxItems(integer);
        }
        integer = getInteger("minItems", node, false, location, result);
        if (integer != null) {
            schema.setMinItems(integer);
        }

        bool = getBoolean("uniqueItems", node, false, location, result);
        if (bool != null) {
            schema.setUniqueItems(bool);
        }

        integer = getInteger("maxProperties", node, false, location, result);
        if (integer != null) {
            schema.setMaxProperties(integer);
        }

        integer = getInteger("minProperties", node, false, location, result);
        if (integer != null) {
            schema.setMinProperties(integer);
        }

        ArrayNode required = getArray("required", node, false, location, result);
        if (required != null) {
            List<String> requiredList = new ArrayList<>();
            for (JsonNode n : required) {
                if (n.getNodeType().equals(JsonNodeType.STRING)) {
                    requiredList.add(((TextNode) n).textValue());
                } else {
                    result.invalidType(location, "required", "string", n);
                }
            }
            if (requiredList.size() > 0) {
                schema.setRequired(requiredList);
            }
        }

        ArrayNode enumArray = getArray("enum", node, false, location, result);
        if (enumArray != null) {
            for (JsonNode n : enumArray) {
                if (n.isNumber()) {
                    schema.addEnumeration(String.valueOf(n.numberValue()));
                } else if (n.isValueNode()) {
                    schema.addEnumeration(n.asText());
                } else {
                    result.invalidType(location, "enum", "value", n);
                }
            }
        }

        value = getString("type", node, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            schema.setType(SchemaType.valueOf(value.toUpperCase()));
        }

        ObjectNode notObj = getObject("not", node, false, location, result);
        if (notObj != null) {
            Schema not = getSchema(notObj, location, result);
            if (not != null) {
                schema.setNot(not);
            }
        }

        Map<String, Schema> properties = new LinkedHashMap<>();
        ObjectNode propertiesObj = getObject("properties", node, false, location, result);
        Schema property = null;

        Set<String> keys = getKeys(propertiesObj);
        for (String name : keys) {
            JsonNode propertyValue = propertiesObj.get(name);
            if (!propertyValue.getNodeType().equals(JsonNodeType.OBJECT)) {
                result.invalidType(location, "properties", "object", propertyValue);
            } else {
                if (propertiesObj != null) {
                    property = getSchema((ObjectNode) propertyValue, location, result);
                    if (property != null) {
                        properties.put(name, property);
                    }
                }
            }
        }
        if (propertiesObj != null) {
            schema.setProperties(properties);
        }

        ObjectNode additionalPropertiesObj = getObject("additionalProperties", node, false, location, result);
        if (additionalPropertiesObj != null) {
            Schema additionalProperties = getSchema(additionalPropertiesObj, location, result);
            if (additionalProperties != null) {
                schema.setAdditionalProperties(additionalProperties);
            }
        }
        value = getString("description", node, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            schema.setDescription(value);
        }

        value = getString("format", node, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            schema.setFormat(value);
        }

        value = getString("default", node, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            schema.setDefaultValue(value);
        }

        //discriminator

        bool = getBoolean("nullable", node, false, location, result);
        if (bool != null) {
            schema.setNullable(bool);
        }

        bool = getBoolean("readOnly", node, false, location, result);
        if (bool != null) {
            schema.setReadOnly(bool);
        }

        bool = getBoolean("writeOnly", node, false, location, result);
        if (bool != null) {
            schema.setWriteOnly(bool);
        }

        ObjectNode xmlNode = getObject("xml", node, false, location, result);
        if (xmlNode != null) {
            XML xml = getXml(xmlNode, location, result);
            if (xml != null) {
                schema.setXml(xml);
            }
        }

        ObjectNode externalDocs = getObject("externalDocs", node, false, location, result);
        if (externalDocs != null) {
            ExternalDocumentation docs = getExternalDocs(externalDocs, location, result);
            if (docs != null) {
                schema.setExternalDocs(docs);
            }
        }

        Object example = getAnyExample("example", node, location, result);
        if (example != null) {
            schema.setExample(example);
        }

        bool = getBoolean("deprecated", node, false, location, result);
        if (bool != null) {
            schema.setDeprecated(bool);
        }

        Map<String, Object> extensions = getExtensions(node);
        if (extensions != null && extensions.size() > 0) {
            schema.setExtensions(extensions);
        }

        Set<String> schemaKeys = getKeys(node);
        for (String key : schemaKeys) {
            if (!SCHEMA_KEYS.contains(key) && !key.startsWith("x-")) {
                result.extra(location, key, node.get(key));
            }
        }

        return schema;

    }

    public Map<String, Example> getExamples(ObjectNode obj, String location, ParseResult result) {
        if (obj == null) {
            return null;
        }
        Map<String, Example> examples = new LinkedHashMap<>();

        Set<String> exampleKeys = getKeys(obj);
        for (String exampleName : exampleKeys) {
            JsonNode exampleValue = obj.get(exampleName);
            if (!exampleValue.getNodeType().equals(JsonNodeType.OBJECT)) {
                result.invalidType(location, exampleName, "object", exampleValue);
            } else {
                ObjectNode example = (ObjectNode) exampleValue;
                if (example != null) {
                    Example exampleObj = getExample(example, String.format("%s.%s'", location, exampleName), result);
                    if (exampleObj != null) {
                        examples.put(exampleName, exampleObj);
                    }
                }
            }
        }
        return examples;
    }

    public List<Example> getExampleList(ArrayNode obj, String location, ParseResult result) {
        List<Example> examples = new ArrayList<>();
        if (obj == null) {
            return examples;
        }
        for (JsonNode item : obj) {
            if (item.getNodeType().equals(JsonNodeType.OBJECT)) {
                Example example = getExample((ObjectNode) item, location, result);
                if (example != null) {
                    examples.add(example);
                }
            }
        }
        return examples;
    }

    public Example getExample(ObjectNode node, String location, ParseResult result) {
        if (node == null)
            return null;

        Example example = new ExampleImpl();

        JsonNode ref = node.get("$ref");
        if (ref != null) {
            if (ref.getNodeType().equals(JsonNodeType.STRING)) {
                example.setRef(ref.asText());
                return example.ref((ref.asText()));
            } else {
                result.invalidType(location, "$ref", "string", node);
                return null;
            }
        }

        String value = getString("summary", node, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            example.setSummary(value);
        }

        value = getString("description", node, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            example.setDescription(value);
        }

        Object sample = getAnyExample("value", node, location, result);
        if (sample != null) {
            example.setValue(sample);
        }

        value = getString("externalValue", node, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            example.setExternalValue(value);
        }

        Map<String, Object> extensions = getExtensions(node);
        if (extensions != null && extensions.size() > 0) {
            example.setExtensions(extensions);
        }

        Set<String> keys = getKeys(node);
        for (String key : keys) {
            if (!EXAMPLE_KEYS.contains(key) && !key.startsWith("x-")) {
                result.extra(location, key, node.get(key));
            }
        }

        return example;
    }

    public void setStyle(String value, Parameter parameter, String location, ObjectNode obj, ParseResult result) {
        if (StringUtils.isBlank(value)) {
            if (QUERY_PARAMETER.equals(parameter.getIn()) || COOKIE_PARAMETER.equals(parameter.getIn())) {
                parameter.setStyle(Parameter.Style.FORM);
            } else if (PATH_PARAMETER.equals(parameter.getIn()) || HEADER_PARAMETER.equals(parameter.getIn())) {
                parameter.setStyle(Parameter.Style.SIMPLE);
            }
        } else {
            if (value.equals(Parameter.Style.FORM.toString())) {
                parameter.setStyle(Parameter.Style.FORM);
            } else if (value.equals(Parameter.Style.DEEPOBJECT.toString())) {
                parameter.setStyle(Parameter.Style.DEEPOBJECT);
            } else if (value.equals(Parameter.Style.LABEL.toString())) {
                parameter.setStyle(Parameter.Style.LABEL);
            } else if (value.equals(Parameter.Style.MATRIX.toString())) {
                parameter.setStyle(Parameter.Style.MATRIX);
            } else if (value.equals(Parameter.Style.PIPEDELIMITED.toString())) {
                parameter.setStyle(Parameter.Style.PIPEDELIMITED);
            } else if (value.equals(Parameter.Style.SIMPLE.toString())) {
                parameter.setStyle(Parameter.Style.SIMPLE);
            } else if (value.equals(Parameter.Style.SPACEDELIMITED.toString())) {
                parameter.setStyle(Parameter.Style.SPACEDELIMITED);
            } else {
                result.invalidType(location, "style", "string", obj);
            }
        }
    }

    public APIResponses getResponses(ObjectNode node, String location, ParseResult result) {
        if (node == null) {
            return null;
        }

        APIResponses apiResponses = new APIResponsesImpl();
        Set<String> keys = getKeys(node);

        for (String key : keys) {
            ObjectNode obj = getObject(key, node, false, String.format("%s.%s'", location, "responses"), result);
            if (obj != null) {
                APIResponse response = getResponse(obj, String.format("%s.%s'", location, key), result);
                if (response != null) {
                    apiResponses.put(key, response);
                }
            }
        }

        return apiResponses;
    }

    public APIResponse getResponse(ObjectNode node, String location, ParseResult result) {
        if (node == null) {
            return null;
        }

        APIResponse apiResponse = new APIResponseImpl();
        JsonNode ref = node.get("$ref");
        if (ref != null) {
            if (ref.getNodeType().equals(JsonNodeType.STRING)) {
                apiResponse.setRef(ref.asText());
                return apiResponse.ref((ref.asText()));
            } else {
                result.invalidType(location, "$ref", "string", node);
                return null;
            }
        }

        String value = getString("description", node, true, location, result);
        if (StringUtils.isNotBlank(value)) {
            apiResponse.description(value);
        }

        ObjectNode headerObject = getObject("headers", node, false, location, result);
        if (headerObject != null) {
            Map<String, Header> headers = getHeaders(headerObject, location, result);
            if (headers != null && headers.size() > 0) {
                apiResponse.setHeaders(headers);
            }
        }

        ObjectNode linksObj = getObject("links", node, false, location, result);
        if (linksObj != null) {
            Map<String, Link> links = getLinks(linksObj, location, result);
            if (links != null && links.size() > 0) {
                apiResponse.setLinks(links);
            }
        }

        ObjectNode contentObject = getObject("content", node, false, location, result);
        if (contentObject != null) {
            apiResponse.setContent(getContent(contentObject, String.format("%s.%s'", location, "content"), result));
        }

        Map<String, Object> extensions = getExtensions(node);
        if (extensions != null && extensions.size() > 0) {
            apiResponse.setExtensions(extensions);
        }

        Set<String> keys = getKeys(node);
        for (String key : keys) {
            if (!RESPONSE_KEYS.contains(key) && !key.startsWith("x-")) {
                result.extra(location, key, node.get(key));
            }
        }

        return apiResponse;
    }

    public List<String> getTagsStrings(ArrayNode nodes, String location, ParseResult result) {
        if (nodes == null)
            return null;

        List<String> tags = new ArrayList<>();

        for (JsonNode node : nodes) {
            if (node.getNodeType().equals(JsonNodeType.STRING)) {
                tags.add(node.textValue());
            }
        }
        return tags;
    }

    public Operation getOperation(ObjectNode obj, String location, ParseResult result) {
        if (obj == null) {
            return null;
        }
        Operation operation = new OperationImpl();

        ArrayNode array = getArray("tags", obj, false, location, result);
        List<String> tags = getTagsStrings(array, String.format("%s.%s'", location, "tags"), result);
        if (tags != null) {
            operation.setTags(tags);
        }
        String value = getString("summary", obj, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            operation.setSummary(value);
        }

        value = getString("description", obj, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            operation.setDescription(value);
        }

        ObjectNode externalDocs = getObject("externalDocs", obj, false, location, result);
        ExternalDocumentation docs = getExternalDocs(externalDocs, String.format("%s.%s'", location, "externalDocs"), result);
        if (docs != null) {
            operation.setExternalDocs(docs);
        }
        value = getString("operationId", obj, false, location, result);
        if (StringUtils.isNotBlank(value)) {
            operation.operationId(value);
        }

        ArrayNode parameters = getArray("parameters", obj, false, location, result);
        if (parameters != null) {
            operation.setParameters(getParameterList(parameters, String.format("%s.%s'", location, "parameters"), result));
        }

        final ObjectNode requestObjectNode = getObject("requestBody", obj, false, location, result);
        if (requestObjectNode != null) {
            operation.setRequestBody(getRequestBody(requestObjectNode, String.format("%s.%s'", location, "requestBody"), result));
        }

        ObjectNode responsesNode = getObject("responses", obj, true, location, result);
        APIResponses responses = getResponses(responsesNode, String.format("%s.%s'", location, "responses"), result);
        if (responses != null) {
            operation.setResponses(responses);
        }

        ObjectNode callbacksNode = getObject("callbacks", obj, false, location, result);
        Map<String, Callback> callbacks = getCallbacks(callbacksNode, String.format("%s.%s'", location, "callbacks"), result);
        if (callbacks != null) {
            operation.setCallbacks(callbacks);
        }

        Boolean deprecated = getBoolean("deprecated", obj, false, location, result);
        if (deprecated != null) {
            operation.setDeprecated(deprecated);
        }

        array = getArray("servers", obj, false, location, result);
        if (array != null && array.size() > 0) {
            operation.setServers(getServersList(array, String.format("%s.%s'", location, "servers"), result));
        }

        array = getArray("security", obj, false, location, result);
        if (array != null) {
            operation.setSecurity(getSecurityRequirementsList(array, String.format("%s.%s'", location, "security"), result));
        }

        Map<String, Object> extensions = getExtensions(obj);
        if (extensions != null && extensions.size() > 0) {
            operation.setExtensions(extensions);
        }

        Set<String> keys = getKeys(obj);
        for (String key : keys) {
            if (!OPERATION_KEYS.contains(key) && !key.startsWith("x-")) {
                result.extra(location, key, obj.get(key));
            }
        }

        return operation;
    }

    public List<SecurityRequirement> getSecurityRequirementsList(ArrayNode nodes, String location, ParseResult result) {
        if (nodes == null)
            return null;

        List<SecurityRequirement> securityRequirements = new ArrayList<>();

        for (JsonNode node : nodes) {
            if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
                SecurityRequirement securityRequirement = new SecurityRequirementImpl();
                Set<String> keys = getKeys((ObjectNode) node);
                for (String key : keys) {
                    if (key != null) {
                        JsonNode value = node.get(key);
                        if (key != null && JsonNodeType.ARRAY.equals(value.getNodeType())) {
                            ArrayNode arrayNode = (ArrayNode) value;
                            List<String> scopes = Stream.generate(arrayNode.elements()::next).map((n) -> n.asText()).limit(arrayNode.size()).collect(Collectors.toList());
                            securityRequirement.addScheme(key, scopes);
                            if (securityRequirement.size() > 0) {
                                securityRequirements.add(securityRequirement);
                            }
                        }
                    }
                }
            }
        }

        return securityRequirements;

    }

    public Map<String, RequestBody> getRequestBodies(ObjectNode obj, String location, ParseResult result) {
        if (obj == null) {
            return null;
        }
        Map<String, RequestBody> bodies = new LinkedHashMap<>();

        Set<String> bodyKeys = getKeys(obj);
        for (String bodyName : bodyKeys) {
            JsonNode bodyValue = obj.get(bodyName);
            if (!bodyValue.getNodeType().equals(JsonNodeType.OBJECT)) {
                result.invalidType(location, bodyName, "object", bodyValue);
            } else {
                ObjectNode bodyObj = (ObjectNode) bodyValue;
                RequestBody body = getRequestBody(bodyObj, String.format("%s.%s'", location, bodyName), result);
                if (body != null) {
                    bodies.put(bodyName, body);
                }
            }
        }
        return bodies;
    }

    protected RequestBody getRequestBody(ObjectNode node, String location, ParseResult result) {
        if (node == null) {
            return null;
        }
        final RequestBody body = new RequestBodyImpl();

        JsonNode ref = node.get("$ref");
        if (ref != null) {
            if (ref.getNodeType().equals(JsonNodeType.STRING)) {
                body.setRef(ref.asText());
                return body.ref(ref.asText());
            } else {
                result.invalidType(location, "$ref", "string", node);
                return null;
            }
        }

        final String description = getString("description", node, false, location, result);
        if (StringUtils.isNotBlank(description)) {
            body.setDescription(description);
        }

        final Boolean required = getBoolean("required", node, false, location, result);
        if (required != null) {
            body.setRequired(required);
        }

        final ObjectNode contentNode = getObject("content", node, true, location, result);
        if (contentNode != null) {
            body.setContent(getContent(contentNode, location + ".content", result));
        }

        Map<String, Object> extensions = getExtensions(node);
        if (extensions != null && extensions.size() > 0) {
            body.setExtensions(extensions);
        }

        Set<String> keys = getKeys(node);
        for (String key : keys) {
            if (!REQUEST_BODY_KEYS.contains(key) && !key.startsWith("x-")) {
                result.extra(location, key, node.get(key));
            }
        }

        return body;
    }

    protected static class ParseResult {
        private boolean valid = true;
        private final Map<Location, JsonNode> extra = new LinkedHashMap<>();
        private final Map<Location, JsonNode> unsupported = new LinkedHashMap<>();
        private final Map<Location, String> invalidType = new LinkedHashMap<>();
        private final List<Location> missing = new ArrayList<>();

        public ParseResult() {}

        public void unsupported(String location, String key, JsonNode value) {
            unsupported.put(new Location(location, key), value);
        }

        public void extra(String location, String key, JsonNode value) {
            extra.put(new Location(location, key), value);
        }

        public void missing(String location, String key) {
            missing.add(new Location(location, key));
        }

        public void invalidType(String location, String key, String expectedType, JsonNode value) {
            invalidType.put(new Location(location, key), expectedType);
        }

        public void invalid() {
            this.valid = false;
        }

        public boolean isValid() {
            return this.valid;
        }

        public List<String> getMessages() {
            List<String> messages = new ArrayList<String>();
            for (Location l : extra.keySet()) {
                String location = l.location.equals("") ? "" : l.location + ".";
                String message = "attribute " + location + l.key + " is unexpected";
                messages.add(message);
            }
            for (Location l : invalidType.keySet()) {
                String location = l.location.equals("") ? "" : l.location + ".";
                String message = "attribute " + location + l.key + " is not of type `" + invalidType.get(l) + "`";
                messages.add(message);
            }
            for (Location l : missing) {
                String location = l.location.equals("") ? "" : l.location + ".";
                String message = "attribute " + location + l.key + " is missing";
                messages.add(message);
            }
            for (Location l : unsupported.keySet()) {
                String location = l.location.equals("") ? "" : l.location + ".";
                String message = "attribute " + location + l.key + " is unsupported";
                messages.add(message);
            }
            return messages;
        }
    }

    protected static class Location {
        private final String location;

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Location))
                return false;

            Location location1 = (Location) o;

            if (location != null ? !location.equals(location1.location) : location1.location != null)
                return false;
            return !(key != null ? !key.equals(location1.key) : location1.key != null);

        }

        @Override
        public int hashCode() {
            int result = location != null ? location.hashCode() : 0;
            result = 31 * result + (key != null ? key.hashCode() : 0);
            return result;
        }

        private final String key;

        public Location(String location, String key) {
            this.location = location;
            this.key = key;
        }
    }

}