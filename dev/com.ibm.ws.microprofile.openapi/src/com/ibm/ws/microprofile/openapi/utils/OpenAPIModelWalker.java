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
package com.ibm.ws.microprofile.openapi.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

import com.ibm.ws.microprofile.openapi.impl.parser.processors.SchemaProcessor;

/**
 * This class does a complete traversal of the OpenAPI model
 * and reports each of the objects to the OpenAPIModelVisitor
 * passed in by the user.
 */
public final class OpenAPIModelWalker {

    private final OpenAPI openAPI;

    public OpenAPIModelWalker(OpenAPI openAPI) {
        this.openAPI = openAPI;
    }

    public void accept(OpenAPIModelVisitor visitor) {
        if (visitor != null) {
            new Walker(openAPI, visitor).traverseOpenAPI();
        }
    }

    public interface Context {

        public OpenAPI getModel();

        public Object getParent();

        public String getLocation();

        public String getLocation(String suffix);
    }

    static final class Walker implements Context {

        private final OpenAPI openAPI;
        private final OpenAPIModelVisitor visitor;
        private final Deque<Object> ancestors = new ArrayDeque<>();
        private final Deque<String> pathSegments = new ArrayDeque<>();
        private final IdentityHashMap<Object, Object> traversedObjects = new IdentityHashMap<>();

        public Walker(OpenAPI openAPI, OpenAPIModelVisitor visitor) {
            this.openAPI = openAPI;
            this.visitor = visitor;
        }

        @Override
        public OpenAPI getModel() {
            return openAPI;
        }

        @Override
        public Object getParent() {
            return ancestors.peek();
        }

        @Override
        public String getLocation() {
            return getLocation(null);
        }

        @Override
        public String getLocation(String suffix) {
            final Iterator<String> i = pathSegments.descendingIterator();
            final StringBuilder sb = new StringBuilder();
            boolean first = true;
            while (i.hasNext()) {
                if (!first) {
                    sb.append('/');
                }
                sb.append(i.next());
                first = false;
            }
            if (suffix != null && !suffix.isEmpty()) {
                sb.append('/');
                sb.append(suffix);
            }
            return sb.toString();
        }

        // Traversal methods call this method to check whether
        // they have already traversed this object before.
        public boolean isTraversed(Object o) {
            if (o == null) {
                return true;
            }
            return traversedObjects.put(o, o) != null;
        }

        public void traverseOpenAPI() {
            visitor.visitOpenAPI(this);
            ancestors.push(openAPI);
            pathSegments.push("#");

            final Components components = openAPI.getComponents();
            if (components != null) {
                pathSegments.push("components");
                traverseComponents(components);
                pathSegments.pop();
            }

            final Map<String, Object> extensions = openAPI.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    if (k != null && v != null) {
                        pathSegments.push(k);
                        traverseExtension(k, v);
                        pathSegments.pop();
                    }
                });
                pathSegments.pop();
            }

            final ExternalDocumentation extDocs = openAPI.getExternalDocs();
            if (extDocs != null) {
                pathSegments.push("externalDocs");
                traverseExternalDocs(extDocs);
                pathSegments.pop();
            }

            final Info info = openAPI.getInfo();
            if (info != null) {
                pathSegments.push("info");
                traverseInfo(info);
                pathSegments.pop();
            }

            final Paths paths = openAPI.getPaths();
            if (paths != null) {
                pathSegments.push("paths");
                traversePaths(paths);
                pathSegments.pop();
            }

            final List<SecurityRequirement> security = openAPI.getSecurity();
            if (security != null) {
                pathSegments.push("security");
                security.stream().forEach((v) -> {
                    traverseSecurityRequirement(v);
                });
                pathSegments.pop();
            }

            final List<Server> servers = openAPI.getServers();
            if (servers != null) {
                pathSegments.push("servers");
                servers.stream().forEach((v) -> {
                    traverseServer(v);
                });
                pathSegments.pop();
            }

            final List<Tag> tags = openAPI.getTags();
            if (tags != null) {
                pathSegments.push("tags");
                tags.stream().forEach((v) -> {
                    traverseTag(v);
                });
                pathSegments.pop();
            }

            ancestors.pop();
            pathSegments.pop();

            // Clean up
            ancestors.clear();
            pathSegments.clear();
            traversedObjects.clear();
        }

        public void traverseComponents(Components components) {
            if (isTraversed(components)) {
                return;
            }
            visitor.visitComponents(this, components);
            ancestors.push(components);

            final Map<String, Callback> callbacks = components.getCallbacks();
            if (callbacks != null) {
                pathSegments.push("callbacks");
                callbacks.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseCallback(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Map<String, Example> examples = components.getExamples();
            if (examples != null) {
                pathSegments.push("examples");
                examples.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExample(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Map<String, Object> extensions = components.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Map<String, Header> headers = components.getHeaders();
            if (headers != null) {
                pathSegments.push("headers");
                headers.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseHeader(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Map<String, Link> links = components.getLinks();
            if (links != null) {
                pathSegments.push("links");
                links.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseLink(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Map<String, Parameter> parameters = components.getParameters();
            if (parameters != null) {
                pathSegments.push("parameters");
                parameters.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseParameter(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Map<String, RequestBody> requestBodies = components.getRequestBodies();
            if (requestBodies != null) {
                pathSegments.push("requestBodies");
                requestBodies.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseRequestBody(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Map<String, APIResponse> responses = components.getResponses();
            if (responses != null) {
                pathSegments.push("responses");
                responses.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseResponse(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Map<String, Schema> schemas = components.getSchemas();
            if (schemas != null) {
                pathSegments.push("schemas");
                schemas.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseSchema(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Map<String, SecurityScheme> schemes = components.getSecuritySchemes();
            if (schemes != null) {
                pathSegments.push("securitySchemes");
                schemes.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseSecurityScheme(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            ancestors.pop();
        }

        public void traverseCallback(String key, Callback callback) {
            if (isTraversed(callback)) {
                return;
            }
            visitor.visitCallback(this, key, callback);
            ancestors.push(callback);

            final Map<String, Object> extensions = callback.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            callback.forEach((k, v) -> {
                pathSegments.push(k);
                traversePathItem(k, v);
                pathSegments.pop();
            });

            ancestors.pop();
        }

        public void traversePathItem(String key, PathItem item) {
            if (isTraversed(item)) {
                return;
            }
            visitor.visitPathItem(this, key, item);
            ancestors.push(item);

            final Map<String, Object> extensions = item.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final class OperationProperty {
                final Operation o;
                final String name;

                OperationProperty(Operation o, String name) {
                    this.o = o;
                    this.name = name;
                }
            }
            final OperationProperty[] operations = { new OperationProperty(item.getDELETE(), "DELETE"),
                                                     new OperationProperty(item.getGET(), "GET"),
                                                     new OperationProperty(item.getHEAD(), "HEAD"),
                                                     new OperationProperty(item.getOPTIONS(), "OPTIONS"),
                                                     new OperationProperty(item.getPATCH(), "PATCH"),
                                                     new OperationProperty(item.getPOST(), "POST"),
                                                     new OperationProperty(item.getPUT(), "PUT"),
                                                     new OperationProperty(item.getTRACE(), "TRACE") };
            Arrays.stream(operations).forEach((v) -> {
                pathSegments.push(v.name);
                traverseOperation(v.o);
                pathSegments.pop();
            });

            final List<Parameter> parameters = item.getParameters();
            if (parameters != null) {
                pathSegments.push("parameters");
                parameters.stream().forEach((v) -> {
                    traverseParameter(null, v);
                });
                pathSegments.pop();
            }

            final List<Server> servers = item.getServers();
            if (servers != null) {
                pathSegments.push("servers");
                servers.stream().forEach((v) -> {
                    traverseServer(v);
                });
                pathSegments.pop();
            }

            ancestors.pop();
        }

        public void traverseOperation(Operation operation) {
            if (isTraversed(operation)) {
                return;
            }
            visitor.visitOperation(this, operation);
            ancestors.push(operation);

            final Map<String, Callback> callbacks = operation.getCallbacks();
            if (callbacks != null) {
                pathSegments.push("callbacks");
                callbacks.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseCallback(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Map<String, Object> extensions = operation.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final ExternalDocumentation extDocs = operation.getExternalDocs();
            if (extDocs != null) {
                pathSegments.push("externalDocs");
                traverseExternalDocs(extDocs);
                pathSegments.pop();
            }

            final List<Parameter> parameters = operation.getParameters();
            if (parameters != null) {
                pathSegments.push("parameters");
                parameters.stream().forEach((v) -> {
                    traverseParameter(null, v);
                });
                pathSegments.pop();
            }

            final RequestBody rb = operation.getRequestBody();
            if (rb != null) {
                pathSegments.push("requestBody");
                traverseRequestBody(null, rb);
                pathSegments.pop();
            }

            final APIResponses responses = operation.getResponses();
            if (responses != null) {
                pathSegments.push("responses");
                traverseResponses(responses);
                pathSegments.pop();
            }

            final List<SecurityRequirement> security = operation.getSecurity();
            if (security != null) {
                pathSegments.push("security");
                security.stream().forEach((v) -> {
                    traverseSecurityRequirement(v);
                });
                pathSegments.pop();
            }

            final List<Server> servers = operation.getServers();
            if (servers != null) {
                pathSegments.push("servers");
                servers.stream().forEach((v) -> {
                    traverseServer(v);
                });
                pathSegments.pop();
            }

            ancestors.pop();
        }

        public void traverseExample(String key, Example example) {
            if (isTraversed(example)) {
                return;
            }
            if (key != null) {
                visitor.visitExample(this, key, example);
            } else {
                visitor.visitExample(this, example);
            }
            ancestors.push(example);

            final Map<String, Object> extensions = example.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            ancestors.pop();
        }

        public void traverseHeader(String key, Header header) {
            if (isTraversed(header)) {
                return;
            }
            visitor.visitHeader(this, key, header);
            ancestors.push(header);

            final Content content = header.getContent();
            if (content != null) {
                pathSegments.push("content");
                content.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseMediaType(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Map<String, Example> examples = header.getExamples();
            if (examples != null) {
                pathSegments.push("examples");
                examples.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExample(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Map<String, Object> extensions = header.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Schema schema = header.getSchema();
            if (schema != null) {
                pathSegments.push("schema");
                traverseSchema(null, schema);
                pathSegments.pop();
            }

            ancestors.pop();
        }

        public void traverseMediaType(String key, MediaType mediaType) {
            if (isTraversed(mediaType)) {
                return;
            }
            visitor.visitMediaType(this, key, mediaType);
            ancestors.push(mediaType);

            final Map<String, Encoding> encoding = mediaType.getEncoding();
            if (encoding != null) {
                pathSegments.push("encoding");
                encoding.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseEncoding(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Map<String, Example> examples = mediaType.getExamples();
            if (examples != null) {
                pathSegments.push("examples");
                examples.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExample(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Map<String, Object> extensions = mediaType.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Schema schema = mediaType.getSchema();
            if (schema != null) {
                pathSegments.push("schema");
                traverseSchema(null, schema);
                pathSegments.pop();
            }

            ancestors.pop();
        }

        public void traverseEncoding(String key, Encoding encoding) {
            if (isTraversed(encoding)) {
                return;
            }
            visitor.visitEncoding(this, key, encoding);
            ancestors.push(encoding);

            final Map<String, Object> extensions = encoding.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Map<String, Header> headers = encoding.getHeaders();
            if (headers != null) {
                pathSegments.push("headers");
                headers.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseHeader(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            ancestors.pop();
        }

        public void traverseLink(String key, Link link) {
            if (isTraversed(link)) {
                return;
            }
            visitor.visitLink(this, key, link);
            ancestors.push(link);

            final Map<String, Object> extensions = link.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Server server = link.getServer();
            if (server != null) {
                pathSegments.push("server");
                link.setServer(server);
                pathSegments.pop();
            }

            ancestors.pop();
        }

        public void traverseParameter(String key, Parameter p) {
            if (isTraversed(p)) {
                return;
            }
            if (key != null) {
                visitor.visitParameter(this, key, p);
            } else {
                visitor.visitParameter(this, p);
            }
            ancestors.push(p);

            final Content content = p.getContent();
            if (content != null) {
                pathSegments.push("content");
                content.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseMediaType(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Map<String, Example> examples = p.getExamples();
            if (examples != null) {
                pathSegments.push("examples");
                examples.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExample(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Map<String, Object> extensions = p.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Schema schema = p.getSchema();
            if (schema != null) {
                pathSegments.push("schema");
                traverseSchema(null, schema);
                pathSegments.pop();
            }

            ancestors.pop();
        }

        public void traverseRequestBody(String key, RequestBody rb) {
            if (isTraversed(rb)) {
                return;
            }
            if (key != null) {
                visitor.visitRequestBody(this, key, rb);
            } else {
                visitor.visitRequestBody(this, rb);
            }
            ancestors.push(rb);

            final Content content = rb.getContent();
            if (content != null) {
                pathSegments.push("content");
                content.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseMediaType(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Map<String, Object> extensions = rb.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            ancestors.pop();
        }

        public void traverseResponses(APIResponses responses) {
            if (isTraversed(responses)) {
                return;
            }
            visitor.visitResponses(this, responses);
            ancestors.push(responses);

            responses.forEach((k, v) -> {
                pathSegments.push(k);
                traverseResponse(k, v);
                pathSegments.pop();
            });

            final APIResponse defaultResponse = responses.getDefault();
            if (defaultResponse != null) {
                pathSegments.push("default");
                traverseResponse("default", defaultResponse);
                pathSegments.pop();
            }

            ancestors.pop();
        }

        public void traverseResponse(String key, APIResponse response) {
            if (isTraversed(response)) {
                return;
            }
            visitor.visitResponse(this, key, response);
            ancestors.push(response);

            final Content content = response.getContent();
            if (content != null) {
                pathSegments.push("content");
                content.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseMediaType(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Map<String, Object> extensions = response.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Map<String, Header> headers = response.getHeaders();
            if (headers != null) {
                pathSegments.push("headers");
                headers.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseHeader(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Map<String, Link> links = response.getLinks();
            if (links != null) {
                pathSegments.push("links");
                links.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseLink(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            ancestors.pop();
        }

        public void traverseSchema(String key, Schema schema) {
            if (isTraversed(schema)) {
                return;
            }
            if (key != null) {
                visitor.visitSchema(this, key, schema);
            } else {
                visitor.visitSchema(this, schema);
            }
            ancestors.push(schema);

            final Object addProps = schema.getAdditionalProperties();
            if (addProps != null && addProps instanceof Schema) {
                pathSegments.push("additionalProperties");
                traverseSchema(null, (Schema) addProps);
                pathSegments.pop();
            }

            final Discriminator d = schema.getDiscriminator();
            if (d != null) {
                pathSegments.push("discriminator");
                traverseDiscriminator(d);
                pathSegments.pop();
            }

            final Map<String, Object> extensions = schema.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final ExternalDocumentation extDocs = schema.getExternalDocs();
            if (extDocs != null) {
                pathSegments.push("externalDocs");
                traverseExternalDocs(extDocs);
                pathSegments.pop();
            }

            final Schema notSchema = schema.getNot();
            if (notSchema != null) {
                pathSegments.push("not");
                traverseSchema(null, notSchema);
                pathSegments.pop();
            }

            final Map<String, Schema> schemas = schema.getProperties();
            if (schemas != null) {
                pathSegments.push("properties");
                schemas.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseSchema(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final XML xml = schema.getXml();
            if (xml != null) {
                pathSegments.push("xml");
                traverseXML(xml);
                pathSegments.pop();
            }

            final class SchemaProperty {
                final Schema s;
                final String name;

                SchemaProperty(Schema s, String name) {
                    this.s = s;
                    this.name = name;
                }
            }
            final List<SchemaProperty> nestedSchemas;
            if (schema.getType() == SchemaType.ARRAY) {
                final Schema arraySchema = schema;
                final Schema items = arraySchema.getItems();
                nestedSchemas = (items != null) ? Collections.singletonList(new SchemaProperty(items, "items")) : null;
            } else if (SchemaProcessor.isComposedSchema(schema)) {
                // 'allOf', 'anyOf' and 'oneOf' really should be mutually exclusive but it's
                // possible the user populated more than one of the fields. The walker's job
                // is to traverse the entire data structure, so we just pass everything it
                // finds to the visitor.
                final Schema composedSchema = schema;
                List<SchemaProperty> _nestedSchemas = new ArrayList<>();
                List<Schema> allOf = composedSchema.getAllOf();
                if (allOf != null) {
                    allOf.forEach((v) -> {
                        _nestedSchemas.add(new SchemaProperty(v, "allOf"));
                    });
                }
                List<Schema> anyOf = composedSchema.getAnyOf();
                if (anyOf != null) {
                    anyOf.forEach((v) -> {
                        _nestedSchemas.add(new SchemaProperty(v, "anyOf"));
                    });
                }
                List<Schema> oneOf = composedSchema.getOneOf();
                if (oneOf != null) {
                    oneOf.forEach((v) -> {
                        _nestedSchemas.add(new SchemaProperty(v, "oneOf"));
                    });
                }
                nestedSchemas = (!_nestedSchemas.isEmpty()) ? _nestedSchemas : null;
            } else {
                nestedSchemas = null;
            }
            if (nestedSchemas != null) {
                nestedSchemas.stream().forEach((v) -> {
                    pathSegments.push(v.name);
                    traverseSchema(null, v.s);
                    pathSegments.pop();
                });
            }

            ancestors.pop();
        }

        public void traverseDiscriminator(Discriminator d) {
            if (isTraversed(d)) {
                return;
            }
            visitor.visitDiscriminator(this, d);
        }

        public void traverseXML(XML xml) {
            if (isTraversed(xml)) {
                return;
            }
            visitor.visitXML(this, xml);
            ancestors.push(xml);

            final Map<String, Object> extensions = xml.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            ancestors.pop();
        }

        public void traverseSecurityScheme(String key, SecurityScheme scheme) {
            if (isTraversed(scheme)) {
                return;
            }
            visitor.visitSecurityScheme(this, key, scheme);
            ancestors.push(scheme);

            final Map<String, Object> extensions = scheme.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final OAuthFlows authFlows = scheme.getFlows();
            if (authFlows != null) {
                pathSegments.push("flows");
                traverseOAuthFlows(authFlows);
                pathSegments.pop();
            }

            ancestors.pop();
        }

        public void traverseOAuthFlows(OAuthFlows authFlows) {
            if (isTraversed(authFlows)) {
                return;
            }
            visitor.visitOAuthFlows(this, authFlows);
            ancestors.push(authFlows);

            final Map<String, Object> extensions = authFlows.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final class OAuthFlowProperty {

                final OAuthFlow o;
                final String name;

                OAuthFlowProperty(OAuthFlow o, String name) {
                    this.o = o;
                    this.name = name;
                }
            }
            final OAuthFlowProperty[] _authFlows = { new OAuthFlowProperty(authFlows.getAuthorizationCode(), "authorizationCode"),
                                                     new OAuthFlowProperty(authFlows.getClientCredentials(), "clientCredentials"),
                                                     new OAuthFlowProperty(authFlows.getImplicit(), "implicit"),
                                                     new OAuthFlowProperty(authFlows.getPassword(), "password") };
            Arrays.stream(_authFlows).forEach((v) -> {
                pathSegments.push(v.name);
                traverseOAuthFlow(v.o);
                pathSegments.pop();
            });

            ancestors.pop();
        }

        public void traverseOAuthFlow(OAuthFlow authFlow) {
            if (isTraversed(authFlow)) {
                return;
            }
            visitor.visitOAuthFlow(this, authFlow);
            ancestors.push(authFlow);

            final Map<String, Object> extensions = authFlow.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final Scopes scopes = authFlow.getScopes();
            if (scopes != null) {
                pathSegments.push("scopes");
                traverseScopes(scopes);
                pathSegments.pop();
            }

            ancestors.pop();
        }

        public void traverseScopes(Scopes scopes) {
            if (isTraversed(scopes)) {
                return;
            }
            visitor.visitScopes(this, scopes);
            ancestors.push(scopes);

            final Map<String, Object> extensions = scopes.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            ancestors.pop();
        }

        public void traverseExtension(String key, Object extension) {
            visitor.visitExtension(this, key, extension);
        }

        public void traverseExternalDocs(ExternalDocumentation extDocs) {
            if (isTraversed(extDocs)) {
                return;
            }
            visitor.visitExternalDocumentation(this, extDocs);
            ancestors.push(extDocs);

            final Map<String, Object> extensions = extDocs.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            ancestors.pop();
        }

        public void traverseInfo(Info info) {
            if (isTraversed(info)) {
                return;
            }
            visitor.visitInfo(this, info);
            ancestors.push(info);

            final Contact contact = info.getContact();
            if (contact != null) {
                traverseContact(contact);
            }

            final Map<String, Object> extensions = info.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final License license = info.getLicense();
            if (license != null) {
                pathSegments.push("license");
                traverseLicense(license);
                pathSegments.pop();
            }

            ancestors.pop();
        }

        public void traverseContact(Contact contact) {
            if (isTraversed(contact)) {
                return;
            }
            visitor.visitContact(this, contact);
            ancestors.push(contact);

            final Map<String, Object> extensions = contact.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            ancestors.pop();
        }

        public void traverseLicense(License license) {
            if (isTraversed(license)) {
                return;
            }
            visitor.visitLicense(this, license);
            ancestors.push(license);

            final Map<String, Object> extensions = license.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            ancestors.pop();
        }

        public void traversePaths(Paths paths) {
            if (isTraversed(paths)) {
                return;
            }
            visitor.visitPaths(this, paths);
            ancestors.push(paths);

            paths.forEach((k, v) -> {
                pathSegments.push(k);
                traversePathItem(k, v);
                pathSegments.pop();
            });

            final Map<String, Object> extensions = paths.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            ancestors.pop();
        }

        public void traverseSecurityRequirement(SecurityRequirement sr) {
            if (isTraversed(sr)) {
                return;
            }
            visitor.visitSecurityRequirement(this, sr);
        }

        public void traverseServer(Server server) {
            if (isTraversed(server)) {
                return;
            }
            visitor.visitServer(this, server);
            ancestors.push(server);

            final Map<String, Object> extensions = server.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final ServerVariables svs = server.getVariables();
            if (svs != null) {
                pathSegments.push("variables");
                traverseServerVariables(svs);
                pathSegments.pop();
            }
            ancestors.pop();
        }

        public void traverseServerVariables(ServerVariables svs) {
            if (isTraversed(svs)) {
                return;
            }
            visitor.visitServerVariables(this, svs);
            ancestors.push(svs);

            svs.forEach((k, v) -> {
                pathSegments.push(k);
                traverseServerVariable(k, v);
                pathSegments.pop();
            });

            final Map<String, Object> extensions = svs.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            ancestors.pop();
        }

        public void traverseServerVariable(String key, ServerVariable sv) {
            if (isTraversed(sv)) {
                return;
            }
            visitor.visitServerVariable(this, key, sv);
            ancestors.push(sv);

            final Map<String, Object> extensions = sv.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            ancestors.pop();
        }

        public void traverseTag(Tag tag) {
            if (isTraversed(tag)) {
                return;
            }
            visitor.visitTag(this, tag);
            ancestors.push(tag);

            final Map<String, Object> extensions = tag.getExtensions();
            if (extensions != null) {
                pathSegments.push("extensions");
                extensions.forEach((k, v) -> {
                    pathSegments.push(k);
                    traverseExtension(k, v);
                    pathSegments.pop();
                });
                pathSegments.pop();
            }

            final ExternalDocumentation extDocs = tag.getExternalDocs();
            if (extDocs != null) {
                pathSegments.push("externalDocs");
                traverseExternalDocs(extDocs);
                pathSegments.pop();
            }

            ancestors.pop();
        }
    }
}
