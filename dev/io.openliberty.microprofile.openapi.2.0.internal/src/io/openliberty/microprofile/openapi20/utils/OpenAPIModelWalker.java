/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.eclipse.microprofile.openapi.models.tags.Tag;

/**
 * This class implements the visitor design pattern. It does a complete traversal of the OpenAPI model and reports each
 * of the objects to the OpenAPIModelVisitor or OpenAPIModelFilter passed in by the user to the accept() methods.
 */
public final class OpenAPIModelWalker {

    private final OpenAPI openAPI;

    public OpenAPIModelWalker(OpenAPI openAPI) {
        this.openAPI = openAPI;
    }

    public void accept(OpenAPIModelVisitor visitor) {
        accept(visitor, true);
    }

    public void accept(OpenAPIModelVisitor visitor, boolean previsit) {
        if (visitor != null && openAPI != null) {
            new Walker(openAPI, visitor, previsit).traverseOpenAPI();
        }
    }

    /**
     * Current context of the OpenAPIModelWalker. An instance of Context
     * is passed to every method of the visitor or filter.
     */
    public interface Context {

        /**
         * Returns the OpenAPI model.
         */
        public OpenAPI getModel();

        /**
         * Returns the parent of the current object being visited.
         */
        public Object getParent();

        /**
         * Returns the location of the current object being visited.
         * The format of this location is a JSON pointer.
         */
        public String getLocation();

        /**
         * Returns the location of the current object being visited
         * with the suffix parameter appended to the end of the
         * string. The format of this location is a JSON pointer.
         */
        public String getLocation(String suffix);
    }

    /**
     * This class is responsible for traversing the OpenAPI model. Generally each object in the model has a traverse()
     * method associated with it that visits that object and recursively traverses through its children. The caller of
     * each traverse() method checks the return value and is responsible for mutation of the parent object if the return
     * value is different than the parameter that was passed in.
     *
     * Each traverse() method checks the "previsit" flag to determine whether it invokes the visitor/filter before or
     * after it traverses through all of its children.
     *
     * This class contains many lambda expressions that iterate over maps and lists. The convention for maps is 
     * "(k, v) ->", where k is the key and v is the value of an entry in the map. The convention for lists is "(v) ->",
     * where v is the value of an entry in the list.
     */
    static final class Walker implements Context {

        // The OpenAPI model being walked.
        private final OpenAPI openAPI;

        // The filter or visitor provided by the user of OpenAPIModelWalker.
        private final OpenAPIModelVisitor visitor;

        // Flag indicating whether objects are visited before or after their children.
        private final boolean previsit;

        // A stack containing the ancestor objects of the object currently being traversed.
        private final Deque<Object> ancestors = new LinkedList<>();

        // A stack containing path segments for the object currently being traversed.
        private final Deque<String> pathSegments = new LinkedList<>();

        // A map containing all objects that have already been traversed.
        private final IdentityHashMap<Object, Object> traversedObjects = new IdentityHashMap<>();

        public Walker(OpenAPI openAPI, OpenAPIModelVisitor visitor, boolean previsit) {
            this.openAPI = openAPI;
            this.visitor = visitor;
            this.previsit = previsit;
//            this(openAPI, new OpenAPIModelFilterAdapter(visitor), previsit);
        }

//        public Walker(OpenAPI openAPI, OpenAPIModelFilter visitor, boolean previsit) {
//            this.openAPI = openAPI;
//            this.visitor = visitor;
//            this.previsit = previsit;
//        }

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

        /**
         * The getLocation method returns the location as a JSON ponter.
         * 
         * See definition for JSON pointer here: https://tools.ietf.org/html/rfc6901
         * 
         * @param suffix
         *          The suffix to be appended to the location
         * @return String 
         *          The location
         */
        @Override
        public String getLocation(String suffix) {
            final Iterator<String> i = pathSegments.descendingIterator();
            final StringBuilder sb = new StringBuilder();
            boolean first = true;
            while (i.hasNext()) {
                if (!first) {
                    sb.append('/');
                }
                sb.append(escapeJSONPointerPathSegment(i.next()));
                first = false;
            }
            if (suffix != null && !suffix.isEmpty()) {
                sb.append('/');
                sb.append(escapeJSONPointerPathSegment(suffix));
            }
            return sb.toString();
        }

        /**
         * The escapeJSONPointerPathSegment method escapes special characters in the JSON pointer path segment.
         * 
         * JSON pointer escaping rules:
         * 
         *   - Replace ~ with ~0
         *   - Replace / with ~1
         * 
         * @param pathSegment
         *          The JSON pointer path segment to escape.
         * @return String
         *          The escaped JSON pointer path segment
         */
        private String escapeJSONPointerPathSegment(String pathSegment) {
            pathSegment = String.valueOf(pathSegment);
            return pathSegment.replace("~", "~0").replace("/", "~1");
        }

        /**
         * The isTraversed method returns true if the object has already been traversed, false otherwise. Traversal 
         * methods call this method to check whether they have already traversed this object before.
         * 
         * @param obj
         *          The object to check
         * @return boolean
         *          True if the object has already been traversed, false otherwise
         */
        public boolean isTraversed(Object obj) {
            if (obj == null) {
                return true;
            }
            return traversedObjects.put(obj, obj) != null;
        }

        public void traverseOpenAPI() {
            pathSegments.push("#");
            if (previsit) {
                visitor.visitOpenAPI(this);
            }
            ancestors.push(openAPI);

            final Components components = openAPI.getComponents();
            if (components != null) {
                pathSegments.push("components");
                final Components c = traverseComponents(components);
                if (c != components) {
                    openAPI.setComponents(c);
                }
                pathSegments.pop();
            }

            final Map<String, Object> extensions = openAPI.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            final ExternalDocumentation extDocs = openAPI.getExternalDocs();
            if (extDocs != null) {
                pathSegments.push("externalDocs");
                final ExternalDocumentation e = traverseExternalDocs(extDocs);
                if (e != extDocs) {
                    openAPI.setExternalDocs(e);
                }
                pathSegments.pop();
            }

            final Info info = openAPI.getInfo();
            if (info != null) {
                pathSegments.push("info");
                final Info i = traverseInfo(info);
                if (i != info) {
                    openAPI.setInfo(info);
                }
                pathSegments.pop();
            }

            final Paths paths = openAPI.getPaths();
            if (paths != null) {
                pathSegments.push("paths");
                final Paths p = traversePaths(paths);
                if (p != paths) {
                    openAPI.setPaths(p);
                }
                pathSegments.pop();
            }

            final List<SecurityRequirement> security = openAPI.getSecurity();
            if (security != null) {
                processSecurityRequirements(security);
            }

            final List<Server> servers = openAPI.getServers();
            if (servers != null) {
                processServers(servers);
            }

            final List<Tag> tags = openAPI.getTags();
            if (tags != null) {
                processTags(tags);
            }

            ancestors.pop();
            if (!previsit) {
                visitor.visitOpenAPI(this);
            }
            pathSegments.pop();

            // Clean up
            ancestors.clear();
            pathSegments.clear();
            traversedObjects.clear();
        }

        public Components traverseComponents(Components components) {
            if (isTraversed(components)) {
                return components;
            }
            if (previsit) {
                components = visitor.visitComponents(this, components);
                if (components == null) {
                    return null;
                }
            }
            ancestors.push(components);

            final Map<String, Callback> callbacks = components.getCallbacks();
            if (callbacks != null) {
                processCallbacks(callbacks);
            }

            final Map<String, Example> examples = components.getExamples();
            if (examples != null) {
                processExamples(examples);
            }

            final Map<String, Object> extensions = components.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            final Map<String, Header> headers = components.getHeaders();
            if (headers != null) {
                processHeaders(headers);
            }

            final Map<String, Link> links = components.getLinks();
            if (links != null) {
                processLinks(links);
            }

            final Map<String, Parameter> parameters = components.getParameters();
            if (parameters != null) {
                processParameters(parameters);
            }

            final Map<String, RequestBody> requestBodies = components.getRequestBodies();
            if (requestBodies != null) {
                processRequestBodies(requestBodies);
            }

            final Map<String, APIResponse> responses = components.getResponses();
            if (responses != null) {
                processResponses(responses);
            }

            final Map<String, Schema> schemas = components.getSchemas();
            if (schemas != null) {
                processSchemas(schemas, "schemas");
            }

            final Map<String, SecurityScheme> schemes = components.getSecuritySchemes();
            if (schemes != null) {
                processSecuritySchemes(schemes);
            }

            ancestors.pop();
            if (!previsit) {
                components = visitor.visitComponents(this, components);
            }
            return components;
        }

        public Callback traverseCallback(String key, Callback callback) {
            if (isTraversed(callback)) {
                return callback;
            }
            if (previsit) {
                callback = visitor.visitCallback(this, key, callback);
                if (callback == null) {
                    return null;
                }
            }
            ancestors.push(callback);

            final Map<String, Object> extensions = callback.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            Map<String, PathItem> pathItems = callback.getPathItems();
            if (pathItems != null) {
                final Map<String, PathItem> updates = map();
                pathItems.forEach((k, v) -> {
                    pathSegments.push(k);
                    final PathItem p = traversePathItem(k, v);
                    if (p != v) {
                        updates.put(k, p);
                    }
                    pathSegments.pop();
                });

                for (Entry<String, PathItem> entry : updates.entrySet()) {
                    if (entry.getValue() != null) {
                        callback.addPathItem(entry.getKey(), entry.getValue());
                    } else {
                        callback.removePathItem(entry.getKey());
                    }
                } // FOR
            }

            ancestors.pop();
            if (!previsit) {
                callback = visitor.visitCallback(this, key, callback);
            }
            return callback;
        }

        public PathItem traversePathItem(String key, PathItem item) {
            if (isTraversed(item)) {
                return item;
            }
            if (previsit) {
                item = visitor.visitPathItem(this, key, item);
                if (item == null) {
                    return null;
                }
            }
            ancestors.push(item);

            final Map<String, Object> extensions = item.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            final class OperationProperty {
                final Operation o;
                final String name;

                OperationProperty(Operation o, String name) {
                    this.o = o;
                    this.name = name;
                }
            }
            final OperationProperty[] operations = { new OperationProperty(item.getDELETE(), "delete"),
                                                     new OperationProperty(item.getGET(), "get"),
                                                     new OperationProperty(item.getHEAD(), "head"),
                                                     new OperationProperty(item.getOPTIONS(), "options"),
                                                     new OperationProperty(item.getPATCH(), "patch"),
                                                     new OperationProperty(item.getPOST(), "post"),
                                                     new OperationProperty(item.getPUT(), "put"),
                                                     new OperationProperty(item.getTRACE(), "trace") };
            final PathItem _item = item;
            Arrays.stream(operations).forEach((v) -> {
                pathSegments.push(v.name);
                final Operation o = traverseOperation(v.o);
                if (o != v.o) {
                    switch (v.name) {
                        case "delete":
                            _item.setDELETE(o);
                            break;
                        case "get":
                            _item.setGET(o);
                            break;
                        case "head":
                            _item.setHEAD(o);
                            break;
                        case "options":
                            _item.setOPTIONS(o);
                            break;
                        case "patch":
                            _item.setPATCH(o);
                            break;
                        case "post":
                            _item.setPOST(o);
                            break;
                        case "put":
                            _item.setPUT(o);
                            break;
                        case "trace":
                            _item.setTRACE(o);
                            break;
                    }
                }
                pathSegments.pop();
            });

            final List<Parameter> parameters = item.getParameters();
            if (parameters != null) {
                processParameters(parameters);
            }

            final List<Server> servers = item.getServers();
            if (servers != null) {
                processServers(servers);
            }

            ancestors.pop();
            if (!previsit) {
                item = visitor.visitPathItem(this, key, item);
            }
            return item;
        }

        public Operation traverseOperation(Operation operation) {
            if (isTraversed(operation)) {
                return operation;
            }
            if (previsit) {
                operation = visitor.visitOperation(this, operation);
                if (operation == null) {
                    return null;
                }
            }
            ancestors.push(operation);

            final Map<String, Callback> callbacks = operation.getCallbacks();
            if (callbacks != null) {
                processCallbacks(callbacks);
            }

            final Map<String, Object> extensions = operation.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            final ExternalDocumentation extDocs = operation.getExternalDocs();
            if (extDocs != null) {
                pathSegments.push("externalDocs");
                final ExternalDocumentation e = traverseExternalDocs(extDocs);
                if (e != extDocs) {
                    operation.setExternalDocs(e);
                }
                pathSegments.pop();
            }

            final List<Parameter> parameters = operation.getParameters();
            if (parameters != null) {
                processParameters(parameters);
            }

            final RequestBody rb = operation.getRequestBody();
            if (rb != null) {
                pathSegments.push("requestBody");
                final RequestBody r = traverseRequestBody(null, rb);
                if (r != rb) {
                    operation.setRequestBody(r);
                }
                pathSegments.pop();
            }

            final APIResponses responses = operation.getResponses();
            if (responses != null) {
                pathSegments.push("responses");
                final APIResponses r = traverseResponses(responses);
                if (r != responses) {
                    operation.setResponses(r);
                }
                pathSegments.pop();
            }

            final List<SecurityRequirement> security = operation.getSecurity();
            if (security != null) {
                processSecurityRequirements(security);
            }

            final List<Server> servers = operation.getServers();
            if (servers != null) {
                processServers(servers);
            }

            ancestors.pop();
            if (!previsit) {
                operation = visitor.visitOperation(this, operation);
            }
            return operation;
        }

        public Example traverseExample(String key, Example example) {
            if (isTraversed(example)) {
                return example;
            }
            if (previsit) {
                if (key != null) {
                    example = visitor.visitExample(this, key, example);
                } else {
                    example = visitor.visitExample(this, example);
                }
                if (example == null) {
                    return null;
                }
            }
            ancestors.push(example);

            final Map<String, Object> extensions = example.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            ancestors.pop();
            if (!previsit) {
                if (key != null) {
                    example = visitor.visitExample(this, key, example);
                } else {
                    example = visitor.visitExample(this, example);
                }
            }
            return example;
        }

        public Header traverseHeader(String key, Header header) {
            if (isTraversed(header)) {
                return header;
            }
            if (previsit) {
                header = visitor.visitHeader(this, key, header);
                if (header == null) {
                    return null;
                }
            }
            ancestors.push(header);

            final Content content = header.getContent();
            if (content != null) {
                processContent(content);
            }

            final Map<String, Example> examples = header.getExamples();
            if (examples != null) {
                processExamples(examples);
            }

            final Map<String, Object> extensions = header.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            final Schema schema = header.getSchema();
            if (schema != null) {
                pathSegments.push("schema");
                final Schema s = traverseSchema(null, schema);
                if (s != schema) {
                    header.setSchema(s);
                }
                pathSegments.pop();
            }

            ancestors.pop();
            if (!previsit) {
                header = visitor.visitHeader(this, key, header);
            }
            return header;
        }

        public MediaType traverseMediaType(String key, MediaType mediaType) {
            if (isTraversed(mediaType)) {
                return mediaType;
            }
            if (previsit) {
                mediaType = visitor.visitMediaType(this, key, mediaType);
                if (mediaType == null) {
                    return null;
                }
            }
            ancestors.push(mediaType);

            final Map<String, Encoding> encodings = mediaType.getEncoding();
            if (encodings != null) {
                processEncodings(encodings, "encoding");
            }

            final Map<String, Example> examples = mediaType.getExamples();
            if (examples != null) {
                processExamples(examples);
            }

            final Map<String, Object> extensions = mediaType.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            final Schema schema = mediaType.getSchema();
            if (schema != null) {
                pathSegments.push("schema");
                final Schema s = traverseSchema(null, schema);
                if (s != schema) {
                    mediaType.setSchema(schema);
                }
                pathSegments.pop();
            }

            ancestors.pop();
            if (!previsit) {
                mediaType = visitor.visitMediaType(this, key, mediaType);
            }
            return mediaType;
        }

        public Encoding traverseEncoding(String key, Encoding encoding) {
            if (isTraversed(encoding)) {
                return encoding;
            }
            if (previsit) {
                encoding = visitor.visitEncoding(this, key, encoding);
                if (encoding == null) {
                    return null;
                }
            }
            ancestors.push(encoding);

            final Map<String, Object> extensions = encoding.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            final Map<String, Header> headers = encoding.getHeaders();
            if (headers != null) {
                processHeaders(headers);
            }

            ancestors.pop();
            if (!previsit) {
                encoding = visitor.visitEncoding(this, key, encoding);
            }
            return encoding;
        }

        public Link traverseLink(String key, Link link) {
            if (isTraversed(link)) {
                return link;
            }
            if (previsit) {
                link = visitor.visitLink(this, key, link);
                if (link == null) {
                    return null;
                }
            }
            ancestors.push(link);

            final Map<String, Object> extensions = link.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            final Server server = link.getServer();
            if (server != null) {
                pathSegments.push("server");
                final Server s = traverseServer(server);
                if (s != server) {
                    link.setServer(s);
                }
                pathSegments.pop();
            }

            ancestors.pop();
            if (!previsit) {
                link = visitor.visitLink(this, key, link);
            }
            return link;
        }

        public Parameter traverseParameter(String key, Parameter p) {
            if (isTraversed(p)) {
                return p;
            }
            if (previsit) {
                if (key != null) {
                    p = visitor.visitParameter(this, key, p);
                } else {
                    p = visitor.visitParameter(this, p);
                }
                if (p == null) {
                    return null;
                }
            }
            ancestors.push(p);

            final Content content = p.getContent();
            if (content != null) {
                processContent(content);
            }

            final Map<String, Example> examples = p.getExamples();
            if (examples != null) {
                processExamples(examples);
            }

            final Map<String, Object> extensions = p.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            final Schema schema = p.getSchema();
            if (schema != null) {
                pathSegments.push("schema");
                final Schema s = traverseSchema(null, schema);
                if (s != schema) {
                    p.setSchema(s);
                }
                pathSegments.pop();
            }

            ancestors.pop();
            if (!previsit) {
                if (key != null) {
                    p = visitor.visitParameter(this, key, p);
                } else {
                    p = visitor.visitParameter(this, p);
                }
            }
            return p;
        }

        public RequestBody traverseRequestBody(String key, RequestBody rb) {
            if (isTraversed(rb)) {
                return rb;
            }
            if (previsit) {
                if (key != null) {
                    rb = visitor.visitRequestBody(this, key, rb);
                } else {
                    rb = visitor.visitRequestBody(this, rb);
                }
                if (rb == null) {
                    return null;
                }
            }
            ancestors.push(rb);

            final Content content = rb.getContent();
            if (content != null) {
                processContent(content);
            }

            final Map<String, Object> extensions = rb.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            ancestors.pop();
            if (!previsit) {
                if (key != null) {
                    rb = visitor.visitRequestBody(this, key, rb);
                } else {
                    rb = visitor.visitRequestBody(this, rb);
                }
            }
            return rb;
        }

        public APIResponses traverseResponses(APIResponses responses) {
            if (isTraversed(responses)) {
                return responses;
            }
            if (previsit) {
                responses = visitor.visitResponses(this, responses);
                if (responses == null) {
                    return null;
                }
            }
            ancestors.push(responses);

            Map<String, APIResponse> apiResponses = responses.getAPIResponses();
            if (apiResponses != null) {
                final Map<String, APIResponse> updates = map();
                apiResponses.forEach((k, v) -> {
                    pathSegments.push(k);
                    final APIResponse r = traverseResponse(k, v);
                    if (r != v) {
                        updates.put(k, r);
                    }
                    pathSegments.pop();
                });
                
                for (Entry<String, APIResponse> entry : updates.entrySet()) {
                    if (entry.getValue() != null) {
                        responses.addAPIResponse(entry.getKey(), entry.getValue());
                    } else {
                        responses.removeAPIResponse(entry.getKey());
                    }
                } // FOR
            }

            final APIResponse defaultResponse = responses.getDefaultValue();
            if (defaultResponse != null) {
                pathSegments.push("default");
                final APIResponse r = traverseResponse("default", defaultResponse);
                if (r != defaultResponse) {
                    responses.setDefaultValue(r);
                }
                pathSegments.pop();
            }

            ancestors.pop();
            if (!previsit) {
                responses = visitor.visitResponses(this, responses);
            }
            return responses;
        }

        public APIResponse traverseResponse(String key, APIResponse response) {
            if (isTraversed(response)) {
                return response;
            }
            if (previsit) {
                response = visitor.visitResponse(this, key, response);
                if (response == null) {
                    return null;
                }
            }
            ancestors.push(response);

            final Content content = response.getContent();
            if (content != null) {
                processContent(content);
            }

            final Map<String, Object> extensions = response.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            final Map<String, Header> headers = response.getHeaders();
            if (headers != null) {
                processHeaders(headers);
            }

            final Map<String, Link> links = response.getLinks();
            if (links != null) {
                processLinks(links);
            }

            ancestors.pop();
            if (!previsit) {
                response = visitor.visitResponse(this, key, response);
            }
            return response;
        }

        public Schema traverseSchema(String key, Schema schema) {
            if (isTraversed(schema)) {
                return schema;
            }
            if (previsit) {
                if (key != null) {
                    schema = visitor.visitSchema(this, key, schema);
                } else {
                    schema = visitor.visitSchema(this, schema);
                }
                if (schema == null) {
                    return null;
                }
            }
            ancestors.push(schema);

            final Schema addProps = schema.getAdditionalPropertiesSchema();
            if (addProps != null) {
                pathSegments.push("additionalProperties");
                final Schema s = traverseSchema(null, (Schema) addProps);
                if (s != addProps) {
                    schema.setAdditionalPropertiesSchema(s);
                }
                pathSegments.pop();
            }

            final Discriminator d = schema.getDiscriminator();
            if (d != null) {
                pathSegments.push("discriminator");
                final Discriminator disc = traverseDiscriminator(d);
                if (disc != d) {
                    schema.setDiscriminator(disc);
                }
                pathSegments.pop();
            }

            final Map<String, Object> extensions = schema.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            final ExternalDocumentation extDocs = schema.getExternalDocs();
            if (extDocs != null) {
                pathSegments.push("externalDocs");
                final ExternalDocumentation e = traverseExternalDocs(extDocs);
                if (e != extDocs) {
                    schema.setExternalDocs(e);
                }
                pathSegments.pop();
            }

            final Schema notSchema = schema.getNot();
            if (notSchema != null) {
                pathSegments.push("not");
                final Schema s = traverseSchema(null, notSchema);
                if (s != notSchema) {
                    schema.setNot(s);
                }
                pathSegments.pop();
            }

            final Map<String, Schema> schemas = schema.getProperties();
            if (schemas != null) {
                processSchemas(schemas, "properties");
            }

            final XML xml = schema.getXml();
            if (xml != null) {
                pathSegments.push("xml");
                final XML x = traverseXML(xml);
                if (x != xml) {
                    schema.setXml(x);
                }
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
            } else if (isComposedSchema(schema)) {
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
                final Schema _schema = schema;
                nestedSchemas.stream().forEach((v) -> {
                    pathSegments.push(v.name);
                    final Schema s = traverseSchema(null, v.s);
                    if (s != v.s) {
                        switch (v.name) {
                            case "items":
                                _schema.setItems(s);
                                break;
                            case "allOf":
                                final List<Schema> allOf = _schema.getAllOf();
                                allOf.remove(v.s);
                                if (s != null) {
                                    allOf.add(s);
                                }
                                break;
                            case "anyOf":
                                final List<Schema> anyOf = _schema.getAnyOf();
                                anyOf.remove(v.s);
                                if (s != null) {
                                    anyOf.add(s);
                                }
                                break;
                            case "oneOf":
                                final List<Schema> oneOf = _schema.getOneOf();
                                oneOf.remove(v.s);
                                if (s != null) {
                                    oneOf.add(s);
                                }
                                break;
                        }
                    }
                    pathSegments.pop();
                });
            }

            ancestors.pop();
            if (!previsit) {
                if (key != null) {
                    schema = visitor.visitSchema(this, key, schema);
                } else {
                    schema = visitor.visitSchema(this, schema);
                }
            }
            return schema;
        }

        public Discriminator traverseDiscriminator(Discriminator d) {
            if (isTraversed(d)) {
                return d;
            }
            d = visitor.visitDiscriminator(this, d);
            return d;
        }

        public XML traverseXML(XML xml) {
            if (isTraversed(xml)) {
                return xml;
            }
            if (previsit) {
                xml = visitor.visitXML(this, xml);
                if (xml == null) {
                    return null;
                }
            }
            ancestors.push(xml);

            final Map<String, Object> extensions = xml.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            ancestors.pop();
            if (!previsit) {
                xml = visitor.visitXML(this, xml);
            }
            return xml;
        }

        public SecurityScheme traverseSecurityScheme(String key, SecurityScheme scheme) {
            if (isTraversed(scheme)) {
                return scheme;
            }
            if (previsit) {
                scheme = visitor.visitSecurityScheme(this, key, scheme);
                if (scheme == null) {
                    return null;
                }
            }
            ancestors.push(scheme);

            final Map<String, Object> extensions = scheme.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            final OAuthFlows authFlows = scheme.getFlows();
            if (authFlows != null) {
                pathSegments.push("flows");
                final OAuthFlows o = traverseOAuthFlows(authFlows);
                if (o != authFlows) {
                    scheme.setFlows(o);
                }
                pathSegments.pop();
            }

            ancestors.pop();
            if (!previsit) {
                scheme = visitor.visitSecurityScheme(this, key, scheme);
            }
            return scheme;
        }

        public OAuthFlows traverseOAuthFlows(OAuthFlows authFlows) {
            if (isTraversed(authFlows)) {
                return authFlows;
            }
            if (previsit) {
                authFlows = visitor.visitOAuthFlows(this, authFlows);
                if (authFlows == null) {
                    return null;
                }
            }
            ancestors.push(authFlows);

            final Map<String, Object> extensions = authFlows.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
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
            final OAuthFlows afs = authFlows;
            Arrays.stream(_authFlows).forEach((v) -> {
                pathSegments.push(v.name);
                final OAuthFlow o = traverseOAuthFlow(v.o);
                if (o != v.o) {
                    switch (v.name) {
                        case "authorizationCode":
                            afs.setAuthorizationCode(o);
                            break;
                        case "clientCredentials":
                            afs.setClientCredentials(o);
                            break;
                        case "implicit":
                            afs.setImplicit(o);
                            break;
                        case "password":
                            afs.setPassword(o);
                            break;
                    }
                }
                pathSegments.pop();
            });

            ancestors.pop();
            if (!previsit) {
                authFlows = visitor.visitOAuthFlows(this, authFlows);
            }
            return authFlows;
        }

        public OAuthFlow traverseOAuthFlow(OAuthFlow authFlow) {
            if (isTraversed(authFlow)) {
                return authFlow;
            }
            if (previsit) {
                authFlow = visitor.visitOAuthFlow(this, authFlow);
                if (authFlow == null) {
                    return null;
                }
            }
            ancestors.push(authFlow);

            final Map<String, Object> extensions = authFlow.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            ancestors.pop();
            if (!previsit) {
                authFlow = visitor.visitOAuthFlow(this, authFlow);
            }
            return authFlow;
        }

        public Object traverseExtension(String key, Object extension) {
            extension = visitor.visitExtension(this, key, extension);
            return extension;
        }

        public ExternalDocumentation traverseExternalDocs(ExternalDocumentation extDocs) {
            if (isTraversed(extDocs)) {
                return extDocs;
            }
            if (previsit) {
                extDocs = visitor.visitExternalDocumentation(this, extDocs);
                if (extDocs == null) {
                    return null;
                }
            }
            ancestors.push(extDocs);

            final Map<String, Object> extensions = extDocs.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            ancestors.pop();
            if (!previsit) {
                extDocs = visitor.visitExternalDocumentation(this, extDocs);
            }
            return extDocs;
        }

        public Info traverseInfo(Info info) {
            if (isTraversed(info)) {
                return info;
            }
            if (previsit) {
                info = visitor.visitInfo(this, info);
                if (info == null) {
                    return null;
                }
            }
            ancestors.push(info);

            final Contact contact = info.getContact();
            if (contact != null) {
                pathSegments.push("contact");
                final Contact c = traverseContact(contact);
                if (c != contact) {
                    info.setContact(c);
                }
                pathSegments.pop();
            }

            final Map<String, Object> extensions = info.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            final License license = info.getLicense();
            if (license != null) {
                pathSegments.push("license");
                final License l = traverseLicense(license);
                if (l != license) {
                    info.setLicense(l);
                }
                pathSegments.pop();
            }

            ancestors.pop();
            if (!previsit) {
                info = visitor.visitInfo(this, info);
            }
            return info;
        }

        public Contact traverseContact(Contact contact) {
            if (isTraversed(contact)) {
                return contact;
            }
            if (previsit) {
                contact = visitor.visitContact(this, contact);
                if (contact == null) {
                    return null;
                }
            }
            ancestors.push(contact);

            final Map<String, Object> extensions = contact.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            ancestors.pop();
            if (!previsit) {
                contact = visitor.visitContact(this, contact);
            }
            return contact;
        }

        public License traverseLicense(License license) {
            if (isTraversed(license)) {
                return license;
            }
            if (previsit) {
                license = visitor.visitLicense(this, license);
                if (license == null) {
                    return null;
                }
            }
            ancestors.push(license);

            final Map<String, Object> extensions = license.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            ancestors.pop();
            if (!previsit) {
                license = visitor.visitLicense(this, license);
            }
            return license;
        }

        public Paths traversePaths(Paths paths) {
            if (isTraversed(paths)) {
                return paths;
            }
            if (previsit) {
                paths = visitor.visitPaths(this, paths);
                if (paths == null) {
                    return null;
                }
            }
            ancestors.push(paths);

            Map<String, PathItem> pathItems = paths.getPathItems();
            if (pathItems != null) {
                final Map<String, PathItem> updates = map();
                pathItems.forEach((k, v) -> {
                    pathSegments.push(k);
                    final PathItem p = traversePathItem(k, v);
                    if (p != v) {
                        updates.put(k, p);
                    }
                    pathSegments.pop();
                });
                
                for (Entry<String, PathItem> entry : updates.entrySet()) {
                    if (entry.getValue() != null) {
                        paths.addPathItem(entry.getKey(), entry.getValue());
                    } else {
                        paths.removePathItem(entry.getKey());
                    }
                } // FOR
            }

            final Map<String, Object> extensions = paths.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            ancestors.pop();
            if (!previsit) {
                paths = visitor.visitPaths(this, paths);
            }
            return paths;
        }

        public SecurityRequirement traverseSecurityRequirement(SecurityRequirement sr) {
            if (isTraversed(sr)) {
                return sr;
            }
            sr = visitor.visitSecurityRequirement(this, sr);
            return sr;
        }

        public Server traverseServer(Server server) {
            if (isTraversed(server)) {
                return server;
            }
            if (previsit) {
                server = visitor.visitServer(this, server);
                if (server == null) {
                    return null;
                }
            }
            ancestors.push(server);

            final Map<String, Object> extensions = server.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            final Map<String, ServerVariable> serverVariables = server.getVariables();
            if (serverVariables != null) {
                pathSegments.push("variables");
                final Map<String, ServerVariable> updates = map();
                serverVariables.forEach((k, v) -> {
                    pathSegments.push(k);
                    final ServerVariable s = traverseServerVariable(k, v);
                    if (s != v) {
                        updates.put(k, s);
                    }
                    pathSegments.pop();
                });

                for (Entry<String, ServerVariable> entry : updates.entrySet()) {
                    if (entry.getValue() != null) {
                        server.addVariable(entry.getKey(), entry.getValue());
                    } else {
                        server.removeVariable(entry.getKey());
                    }
                } // FOR
                pathSegments.pop();
            }

            ancestors.pop();
            if (!previsit) {
                server = visitor.visitServer(this, server);
            }
            return server;
        }

        public ServerVariable traverseServerVariable(String key, ServerVariable sv) {
            if (isTraversed(sv)) {
                return sv;
            }
            if (previsit) {
                sv = visitor.visitServerVariable(this, key, sv);
                if (sv == null) {
                    return null;
                }
            }
            ancestors.push(sv);

            final Map<String, Object> extensions = sv.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            ancestors.pop();
            if (!previsit) {
                sv = visitor.visitServerVariable(this, key, sv);
            }
            return sv;
        }

        public Tag traverseTag(Tag tag) {
            if (isTraversed(tag)) {
                return tag;
            }
            if (previsit) {
                tag = visitor.visitTag(this, tag);
                if (tag == null) {
                    return null;
                }
            }
            ancestors.push(tag);

            final Map<String, Object> extensions = tag.getExtensions();
            if (extensions != null) {
                processExtensions(extensions);
            }

            final ExternalDocumentation extDocs = tag.getExternalDocs();
            if (extDocs != null) {
                pathSegments.push("externalDocs");
                final ExternalDocumentation e = traverseExternalDocs(extDocs);
                if (e != extDocs) {
                    tag.setExternalDocs(e);
                }
                pathSegments.pop();
            }

            ancestors.pop();
            if (!previsit) {
                tag = visitor.visitTag(this, tag);
            }
            return tag;
        }

        private void processParameters(final List<Parameter> parameters) {
            pathSegments.push("parameters");
            for (int i = 0; i < parameters.size(); ++i) {
                final Parameter v = parameters.get(i);
                final Parameter p = traverseParameter(null, v);
                if (p != v) {
                    i = updateList(parameters, i, p);
                }
            }
            pathSegments.pop();
        }

        private void processSecurityRequirements(final List<SecurityRequirement> security) {
            pathSegments.push("security");
            for (int i = 0; i < security.size(); ++i) {
                final SecurityRequirement v = security.get(i);
                final SecurityRequirement s = traverseSecurityRequirement(v);
                if (s != v) {
                    i = updateList(security, i, s);
                }
            }
            pathSegments.pop();
        }

        private void processServers(final List<Server> servers) {
            pathSegments.push("servers");
            for (int i = 0; i < servers.size(); ++i) {
                final Server v = servers.get(i);
                final Server s = traverseServer(v);
                if (s != v) {
                    i = updateList(servers, i, s);
                }
            }
            pathSegments.pop();
        }

        private void processTags(final List<Tag> tags) {
            pathSegments.push("tags");
            for (int i = 0; i < tags.size(); ++i) {
                final Tag v = tags.get(i);
                final Tag t = traverseTag(v);
                if (t != v) {
                    i = updateList(tags, i, t);
                }
            }
            pathSegments.pop();
        }

        private void processCallbacks(final Map<String, Callback> callbacks) {
            pathSegments.push("callbacks");
            final Map<String, Callback> updates = map();
            callbacks.forEach((k, v) -> {
                pathSegments.push(k);
                final Callback c = traverseCallback(k, v);
                if (c != v) {
                    updates.put(k, c);
                }
                pathSegments.pop();
            });
            if (updates.size() > 0) {
                updateMap(callbacks, updates);
            }
            pathSegments.pop();
        }

        private void processContent(final Content content) {
            pathSegments.push("content");
            Map<String, MediaType> mediaTypes = content.getMediaTypes();
            if (mediaTypes != null) {
                final Map<String, MediaType> updates = map();
                mediaTypes.forEach((k, v) -> {
                    pathSegments.push(k);
                    final MediaType m = traverseMediaType(k, v);
                    if (m != v) {
                        updates.put(k, m);
                    }
                    pathSegments.pop();
                });
                
                for (Entry<String, MediaType> entry : updates.entrySet()) {
                    if (entry.getValue() != null) {
                        content.addMediaType(entry.getKey(), entry.getValue());
                    } else {
                        content.removeMediaType(entry.getKey());
                    }
                } // FOR
            }
            pathSegments.pop();
        }

        private void processEncodings(final Map<String, Encoding> encodings, String propertyName) {
            pathSegments.push(propertyName);
            final Map<String, Encoding> updates = map();
            encodings.forEach((k, v) -> {
                pathSegments.push(k);
                final Encoding e = traverseEncoding(k, v);
                if (e != v) {
                    updates.put(k, e);
                }
                pathSegments.pop();
            });
            if (updates.size() > 0) {
                updateMap(encodings, updates);
            }
            pathSegments.pop();
        }

        private void processExamples(final Map<String, Example> examples) {
            pathSegments.push("examples");
            final Map<String, Example> updates = map();
            examples.forEach((k, v) -> {
                pathSegments.push(k);
                final Example e = traverseExample(k, v);
                if (e != v) {
                    updates.put(k, e);
                }
                pathSegments.pop();
            });
            if (updates.size() > 0) {
                updateMap(examples, updates);
            }
            pathSegments.pop();
        }

        private void processExtensions(final Map<String, Object> extensions) {
            pathSegments.push("extensions");
            final Map<String, Object> updates = map();
            extensions.forEach((k, v) -> {
                if (k != null && v != null) {
                    pathSegments.push(k);
                    final Object o = traverseExtension(k, v);
                    if (o != v) {
                        updates.put(k, o);
                    }
                    pathSegments.pop();
                }
            });
            if (updates.size() > 0) {
                updateMap(extensions, updates);
            }
            pathSegments.pop();
        }

        private void processHeaders(final Map<String, Header> headers) {
            pathSegments.push("headers");
            final Map<String, Header> updates = map();
            headers.forEach((k, v) -> {
                pathSegments.push(k);
                final Header h = traverseHeader(k, v);
                if (h != v) {
                    updates.put(k, h);
                }
                pathSegments.pop();
            });
            if (updates.size() > 0) {
                updateMap(headers, updates);
            }
            pathSegments.pop();
        }

        private void processLinks(final Map<String, Link> links) {
            pathSegments.push("links");
            final Map<String, Link> updates = map();
            links.forEach((k, v) -> {
                pathSegments.push(k);
                final Link l = traverseLink(k, v);
                if (l != v) {
                    updates.put(k, l);
                }
                pathSegments.pop();
            });
            if (updates.size() > 0) {
                updateMap(links, updates);
            }
            pathSegments.pop();
        }

        private void processParameters(final Map<String, Parameter> parameters) {
            pathSegments.push("parameters");
            final Map<String, Parameter> updates = map();
            parameters.forEach((k, v) -> {
                pathSegments.push(k);
                final Parameter p = traverseParameter(k, v);
                if (p != v) {
                    updates.put(k, p);
                }
                pathSegments.pop();
            });
            if (updates.size() > 0) {
                updateMap(parameters, updates);
            }
            pathSegments.pop();
        }

        private void processRequestBodies(final Map<String, RequestBody> requestBodies) {
            pathSegments.push("requestBodies");
            final Map<String, RequestBody> updates = map();
            requestBodies.forEach((k, v) -> {
                pathSegments.push(k);
                final RequestBody r = traverseRequestBody(k, v);
                if (r != v) {
                    updates.put(k, r);
                }
                pathSegments.pop();
            });
            if (updates.size() > 0) {
                updateMap(requestBodies, updates);
            }
            pathSegments.pop();
        }

        private void processResponses(final Map<String, APIResponse> responses) {
            pathSegments.push("responses");
            final Map<String, APIResponse> updates = map();
            responses.forEach((k, v) -> {
                pathSegments.push(k);
                final APIResponse r = traverseResponse(k, v);
                if (r != v) {
                    updates.put(k, r);
                }
                pathSegments.pop();
            });
            if (updates.size() > 0) {
                updateMap(responses, updates);
            }
            pathSegments.pop();
        }

        private void processSchemas(final Map<String, Schema> schemas, String propertyName) {
            pathSegments.push(propertyName);
            final Map<String, Schema> updates = map();
            schemas.forEach((k, v) -> {
                pathSegments.push(k);
                final Schema s = traverseSchema(k, v);
                if (s != v) {
                    updates.put(k, s);
                }
                pathSegments.pop();
            });
            if (updates.size() > 0) {
                updateMap(schemas, updates);
            }
            pathSegments.pop();
        }

        private void processSecuritySchemes(final Map<String, SecurityScheme> schemes) {
            pathSegments.push("securitySchemes");
            final Map<String, SecurityScheme> updates = map();
            schemes.forEach((k, v) -> {
                pathSegments.push(k);
                final SecurityScheme s = traverseSecurityScheme(k, v);
                if (s != v) {
                    updates.put(k, s);
                }
                pathSegments.pop();
            });
            if (updates.size() > 0) {
                updateMap(schemes, updates);
            }
            pathSegments.pop();
        }

        private <V> int updateList(List<V> list, int index, V update) {
            if (update != null) {
                list.set(index, update);
                return index;
            } else {
                list.remove(index);
                return index - 1;
            }
        }

        private <K, V> Map<K, V> map() {
            return new HashMap<>();
        }

        private <K, V> void updateMap(Map<K, V> map, Map<K, V> updates) {
            updates.forEach((k, v) -> {
                if (v != null) {
                    map.put(k, v);
                } else {
                    map.remove(k);
                }
            });
        }

        private boolean isComposedSchema(Schema schema) {
            if (schema.getAllOf() != null)
                return true;
            if (schema.getAnyOf() != null)
                return true;
            if (schema.getOneOf() != null)
                return true;
            return false;
        }
    }
}
