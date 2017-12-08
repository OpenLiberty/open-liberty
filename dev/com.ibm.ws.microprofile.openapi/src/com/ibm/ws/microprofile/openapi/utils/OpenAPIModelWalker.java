/*
* IBM Confidential
*
* OCO Source Materials
*
* Copyright IBM Corp. 2017
*
* The source code for this program is not published or otherwise divested
* of its trade secrets, irrespective of what has been deposited with the
* U.S. Copyright Office.
*/
package com.ibm.ws.microprofile.openapi.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
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
    }

    static final class Walker implements Context {

        private final OpenAPI openAPI;
        private final OpenAPIModelVisitor visitor;
        private final Deque<Object> ancestors = new ArrayDeque<>();
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

            final Components components = openAPI.getComponents();
            if (components != null) {
                traverseComponents(components);
            }

            final Map<String, Object> extensions = openAPI.getExtensions();
            if (extensions != null) {
                extensions.forEach((k, v) -> {
                    if (k != null && v != null) {
                        traverseExtension(k, v);
                    }
                });
            }

            final ExternalDocumentation extDocs = openAPI.getExternalDocs();
            if (extDocs != null) {
                traverseExternalDocs(extDocs);
            }

            final Info info = openAPI.getInfo();
            if (info != null) {
                traverseInfo(info);
            }

            final Paths paths = openAPI.getPaths();
            if (paths != null) {
                traversePaths(paths);
            }

            final List<SecurityRequirement> security = openAPI.getSecurity();
            if (security != null) {
                security.stream().forEach((v) -> {
                    traverseSecurityRequirement(v);
                });
            }

            final List<Server> servers = openAPI.getServers();
            if (servers != null) {
                servers.stream().forEach((v) -> {
                    traverseServer(v);
                });
            }

            final List<Tag> tags = openAPI.getTags();
            if (tags != null) {
                tags.stream().forEach((v) -> {
                    traverseTag(v);
                });
            }

            ancestors.pop();

            // Clean up
            ancestors.clear();
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
                callbacks.forEach((k, v) -> {
                    traverseCallback(k, v);
                });
            }

            final Map<String, Example> examples = components.getExamples();
            if (examples != null) {
                examples.forEach((k, v) -> {
                    traverseExample(k, v);
                });
            }

            final Map<String, Object> extensions = components.getExtensions();
            if (extensions != null) {
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
            }

            final Map<String, Header> headers = components.getHeaders();
            if (headers != null) {
                headers.forEach((k, v) -> {
                    traverseHeader(k, v);
                });
            }

            final Map<String, Link> links = components.getLinks();
            if (links != null) {
                links.forEach((k, v) -> {
                    traverseLink(k, v);
                });
            }

            final Map<String, Parameter> parameters = components.getParameters();
            if (parameters != null) {
                parameters.forEach((k, v) -> {
                    traverseParameter(k, v);
                });
            }

            final Map<String, RequestBody> requestBodies = components.getRequestBodies();
            if (requestBodies != null) {
                requestBodies.forEach((k, v) -> {
                    traverseRequestBody(k, v);
                });
            }

            final Map<String, APIResponse> responses = components.getResponses();
            if (responses != null) {
                responses.forEach((k, v) -> {
                    traverseResponse(k, v);
                });
            }

            @SuppressWarnings("rawtypes")
            final Map<String, Schema> schemas = components.getSchemas();
            if (schemas != null) {
                schemas.forEach((k, v) -> {
                    traverseSchema(k, v);
                });
            }

            final Map<String, SecurityScheme> schemes = components.getSecuritySchemes();
            if (schemes != null) {
                schemes.forEach((k, v) -> {
                    traverseSecurityScheme(k, v);
                });
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
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
            }

            callback.forEach((k, v) -> {
                traversePathItem(k, v);
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
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
            }

            final Operation[] operations = { item.getDELETE(), item.getGET(),
                                             item.getHEAD(), item.getOPTIONS(),
                                             item.getPATCH(), item.getPOST(),
                                             item.getPUT(), item.getTRACE() };
            Arrays.stream(operations).forEach((v) -> {
                traverseOperation(v);
            });

            final List<Parameter> parameters = item.getParameters();
            if (parameters != null) {
                parameters.stream().forEach((v) -> {
                    traverseParameter(null, v);
                });
            }

            final List<Server> servers = item.getServers();
            if (servers != null) {
                servers.stream().forEach((v) -> {
                    traverseServer(v);
                });
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
                callbacks.forEach((k, v) -> {
                    traverseCallback(k, v);
                });
            }

            final Map<String, Object> extensions = operation.getExtensions();
            if (extensions != null) {
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
            }

            final ExternalDocumentation extDocs = operation.getExternalDocs();
            if (extDocs != null) {
                traverseExternalDocs(extDocs);
            }

            final List<Parameter> parameters = operation.getParameters();
            if (parameters != null) {
                parameters.stream().forEach((v) -> {
                    traverseParameter(null, v);
                });
            }

            final RequestBody rb = operation.getRequestBody();
            if (rb != null) {
                traverseRequestBody(null, rb);
            }

            final APIResponses responses = operation.getResponses();
            if (responses != null) {
                traverseResponses(responses);
            }

            final List<SecurityRequirement> security = operation.getSecurity();
            if (security != null) {
                security.stream().forEach((v) -> {
                    traverseSecurityRequirement(v);
                });
            }

            final List<Server> servers = operation.getServers();
            if (servers != null) {
                servers.stream().forEach((v) -> {
                    traverseServer(v);
                });
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
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
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
                content.forEach((k, v) -> {
                    traverseMediaType(k, v);
                });
            }

            final Map<String, Example> examples = header.getExamples();
            if (examples != null) {
                examples.forEach((k, v) -> {
                    traverseExample(k, v);
                });
            }

            final Map<String, Object> extensions = header.getExtensions();
            if (extensions != null) {
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
            }

            final Schema schema = header.getSchema();
            if (schema != null) {
                traverseSchema(null, schema);
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
                encoding.forEach((k, v) -> {
                    traverseEncoding(k, v);
                });
            }

            final Map<String, Example> examples = mediaType.getExamples();
            if (examples != null) {
                examples.forEach((k, v) -> {
                    traverseExample(k, v);
                });
            }

            final Map<String, Object> extensions = mediaType.getExtensions();
            if (extensions != null) {
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
            }

            final Schema schema = mediaType.getSchema();
            if (schema != null) {
                traverseSchema(null, schema);
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
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
            }

            final Map<String, Header> headers = encoding.getHeaders();
            if (headers != null) {
                headers.forEach((k, v) -> {
                    traverseHeader(k, v);
                });
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
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
            }

            final Server server = link.getServer();
            if (server != null) {
                link.setServer(server);
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
                content.forEach((k, v) -> {
                    traverseMediaType(k, v);
                });
            }

            final Map<String, Example> examples = p.getExamples();
            if (examples != null) {
                examples.forEach((k, v) -> {
                    traverseExample(k, v);
                });
            }

            final Map<String, Object> extensions = p.getExtensions();
            if (extensions != null) {
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
            }

            final Schema schema = p.getSchema();
            if (schema != null) {
                traverseSchema(null, schema);
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
                content.forEach((k, v) -> {
                    traverseMediaType(k, v);
                });
            }

            final Map<String, Object> extensions = rb.getExtensions();
            if (extensions != null) {
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
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
                traverseResponse(k, v);
            });

            final APIResponse defaultResponse = responses.getDefault();
            if (defaultResponse != null) {
                traverseResponse("default", defaultResponse);
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
                content.forEach((k, v) -> {
                    traverseMediaType(k, v);
                });
            }

            final Map<String, Object> extensions = response.getExtensions();
            if (extensions != null) {
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
            }

            final Map<String, Header> headers = response.getHeaders();
            if (headers != null) {
                headers.forEach((k, v) -> {
                    traverseHeader(k, v);
                });
            }

            final Map<String, Link> links = response.getLinks();
            if (links != null) {
                links.forEach((k, v) -> {
                    traverseLink(k, v);
                });
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
                traverseSchema(key, (Schema) addProps);
            }

            final Discriminator d = schema.getDiscriminator();
            if (d != null) {
                traverseDiscriminator(d);
            }

            final Map<String, Object> extensions = schema.getExtensions();
            if (extensions != null) {
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
            }

            final ExternalDocumentation extDocs = schema.getExternalDocs();
            if (extDocs != null) {
                traverseExternalDocs(extDocs);
            }

            final Schema notSchema = schema.getNot();
            if (notSchema != null) {
                traverseSchema(key, notSchema);
            }

            @SuppressWarnings("rawtypes")
            final Map<String, Schema> schemas = schema.getProperties();
            if (schemas != null) {
                schemas.forEach((k, v) -> {
                    traverseSchema(k, v);
                });
            }

            final XML xml = schema.getXml();
            if (xml != null) {
                traverseXML(xml);
            }

            @SuppressWarnings("rawtypes")
            final List<Schema> nestedSchemas;
            if (schema.getType() == SchemaType.ARRAY) {
                final Schema arraySchema = schema;
                @SuppressWarnings("rawtypes")
                final Schema items = arraySchema.getItems();
                nestedSchemas = (items != null) ? Collections.singletonList(items) : null;
            } else if (SchemaProcessor.isComposedSchema(schema)) {
                // 'allOf', 'anyOf' and 'oneOf' really should be mutually exclusive but it's
                // possible the user populated more than one of the fields. The walker's job
                // is to traverse the entire data structure, so we just pass everything it
                // finds to the visitor.
                final Schema composedSchema = schema;
                @SuppressWarnings("rawtypes")
                List<Schema> _nestedSchemas = new ArrayList<>();
                @SuppressWarnings("rawtypes")
                List<Schema> allOf = composedSchema.getAllOf();
                if (allOf != null) {
                    _nestedSchemas.addAll(allOf);
                }
                @SuppressWarnings("rawtypes")
                List<Schema> anyOf = composedSchema.getAnyOf();
                if (anyOf != null) {
                    _nestedSchemas.addAll(anyOf);
                }
                @SuppressWarnings("rawtypes")
                List<Schema> oneOf = composedSchema.getOneOf();
                if (oneOf != null) {
                    _nestedSchemas.addAll(oneOf);
                }
                nestedSchemas = (!_nestedSchemas.isEmpty()) ? _nestedSchemas : null;
            } else {
                nestedSchemas = null;
            }
            if (nestedSchemas != null) {
                nestedSchemas.stream().forEach((v) -> {
                    traverseSchema(null, v);
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
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
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
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
            }

            final OAuthFlows authFlows = scheme.getFlows();
            if (authFlows != null) {
                traverseOAuthFlows(authFlows);
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
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
            }

            final OAuthFlow[] _authFlows = { authFlows.getAuthorizationCode(), authFlows.getClientCredentials(),
                                             authFlows.getImplicit(), authFlows.getPassword() };
            Arrays.stream(_authFlows).forEach((v) -> {
                traverseOAuthFlow(v);
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
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
            }

            final Scopes scopes = authFlow.getScopes();
            if (scopes != null) {
                traverseScopes(scopes);
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
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
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
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
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
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
            }

            final License license = info.getLicense();
            if (license != null) {
                traverseLicense(license);
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
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
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
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
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
                traversePathItem(k, v);
            });

            final Map<String, Object> extensions = paths.getExtensions();
            if (extensions != null) {
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
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
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
            }

            final ServerVariables svs = server.getVariables();
            if (svs != null) {
                traverseServerVariables(svs);
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
                traverseServerVariable(k, v);
            });

            final Map<String, Object> extensions = svs.getExtensions();
            if (extensions != null) {
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
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
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
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
                extensions.forEach((k, v) -> {
                    traverseExtension(k, v);
                });
            }

            final ExternalDocumentation extDocs = tag.getExternalDocs();
            if (extDocs != null) {
                traverseExternalDocs(extDocs);
            }

            ancestors.pop();
        }
    }
}
