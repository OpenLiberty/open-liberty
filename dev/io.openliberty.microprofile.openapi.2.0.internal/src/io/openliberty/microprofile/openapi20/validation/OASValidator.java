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
package io.openliberty.microprofile.openapi20.validation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
import org.eclipse.microprofile.openapi.models.media.Discriminator;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi20.utils.OpenAPIModelVisitor;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker;
import io.openliberty.microprofile.openapi20.utils.ValidationMessageConstants;
import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent;
import io.openliberty.microprofile.openapi20.validation.OASValidationResult.ValidationEvent.Severity;

/**
 * OpenAPI model validator. Checks structural constraints on the model and reports errors for violations.
 */
public final class OASValidator implements OpenAPIModelVisitor, ValidationHelper {

    private OASValidationResult result;
    private final Set<String> operationIds = new HashSet<>();
    private final Map<String, Set<String>> linkOperationIds = new HashMap<String, Set<String>>();
    private static final TraceComponent tc = Tr.register(OASValidator.class);

    public OASValidationResult validate(OpenAPI model) {
        result = new OASValidationResult();
        operationIds.clear();
        linkOperationIds.clear();
        OpenAPIModelWalker walker = new OpenAPIModelWalker(model);
        walker.accept(this);
        validateLinkOperationIds();
        final OASValidationResult _result = result;
        result = null;
        return _result;
    }

    /** {@inheritDoc} */
    @Override
    public void addValidationEvent(ValidationEvent event) {
        if (result != null && event != null) {
            result.getEvents().add(event);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean addOperationId(String operationId) {
        return !operationIds.add(operationId);
    }

    /** {@inheritDoc} */
    @Override
    public void addLinkOperationId(String operationId, String location) {
        if (linkOperationIds.containsKey(operationId)) {
            linkOperationIds.get(operationId).add(location);
        } else {
            Set<String> locations = new HashSet<String>();
            locations.add(location);
            linkOperationIds.put(operationId, locations);
        }
    }

    public void validateLinkOperationIds() {
        for (String k : linkOperationIds.keySet()) {
            if (!operationIds.contains(k)) {
                final String message = Tr.formatMessage(tc, ValidationMessageConstants.LINK_OPERATION_ID_INVALID, k);
                for (String location : linkOperationIds.get(k)) {
                    addValidationEvent(new ValidationEvent(Severity.ERROR, location, message));
                }
            }
        }
    }

    @Override
    public void visitOpenAPI(Context context) {
        final OpenAPIValidator v = OpenAPIValidator.getInstance();
        v.validate(this, context, context.getModel());
    }

    @Override
    public Components visitComponents(Context context, Components components) {
        final ComponentsValidator v = ComponentsValidator.getInstance();
        v.validate(this, context, components);
        return components;
    }

    @Override
    public Object visitExtension(Context context, String key, Object extension) {
        final ExtensionValidator v = ExtensionValidator.getInstance();
        v.validate(this, context, key, extension);
        return extension;
    }

    @Override
    public ExternalDocumentation visitExternalDocumentation(Context context, ExternalDocumentation extDocs) {
        final ExternalDocumentationValidator v = ExternalDocumentationValidator.getInstance();
        v.validate(this, context, extDocs);
        return extDocs;
    }

    @Override
    public Info visitInfo(Context context, Info info) {
        final InfoValidator v = InfoValidator.getInstance();
        v.validate(this, context, info);
        return info;
    }

    @Override
    public Paths visitPaths(Context context, Paths paths) {
        final PathsValidator v = PathsValidator.getInstance();
        v.validate(this, context, paths);
        return paths;
    }

    @Override
    public SecurityRequirement visitSecurityRequirement(Context context, SecurityRequirement sr) {
        final SecurityRequirementValidator v = SecurityRequirementValidator.getInstance();
        v.validate(this, context, sr);
        return sr;
    }

    @Override
    public Server visitServer(Context context, Server server) {
        final ServerValidator v = ServerValidator.getInstance();
        v.validate(this, context, server);
        return server;
    }

    @Override
    public Tag visitTag(Context context, Tag tag) {
        final TagValidator v = TagValidator.getInstance();
        v.validate(this, context, tag);
        return tag;
    }

    @Override
    public Callback visitCallback(Context context, String key, Callback callback) {
        final CallbackValidator v = CallbackValidator.getInstance();
        v.validate(this, context, key, callback);
        return callback;
    }

    @Override
    public Example visitExample(Context context, Example example) {
        final ExampleValidator v = ExampleValidator.getInstance();
        v.validate(this, context, example);
        return example;
    }

    @Override
    public Example visitExample(Context context, String key, Example example) {
        final ExampleValidator v = ExampleValidator.getInstance();
        v.validate(this, context, key, example);
        return example;
    }

    @Override
    public Header visitHeader(Context context, String key, Header header) {
        final HeaderValidator v = HeaderValidator.getInstance();
        v.validate(this, context, key, header);
        return header;
    }

    @Override
    public Link visitLink(Context context, String key, Link link) {
        final LinkValidator v = LinkValidator.getInstance();
        v.validate(this, context, key, link);
        return link;
    }

    @Override
    public Parameter visitParameter(Context context, Parameter p) {
        final ParameterValidator v = ParameterValidator.getInstance();
        v.validate(this, context, p);
        return p;
    }

    @Override
    public Parameter visitParameter(Context context, String key, Parameter p) {
        final ParameterValidator v = ParameterValidator.getInstance();
        v.validate(this, context, key, p);
        return p;
    }

    @Override
    public RequestBody visitRequestBody(Context context, RequestBody rb) {
        final RequestBodyValidator v = RequestBodyValidator.getInstance();
        v.validate(this, context, rb);
        return rb;
    }

    @Override
    public RequestBody visitRequestBody(Context context, String key, RequestBody rb) {
        final RequestBodyValidator v = RequestBodyValidator.getInstance();
        v.validate(this, context, key, rb);
        return rb;
    }

    @Override
    public APIResponses visitResponses(Context context, APIResponses responses) {
        final ResponsesValidator v = ResponsesValidator.getInstance();
        v.validate(this, context, responses);
        return responses;
    }

    @Override
    public APIResponse visitResponse(Context context, String key, APIResponse response) {
        final ResponseValidator v = ResponseValidator.getInstance();
        v.validate(this, context, key, response);
        return response;
    }

    @Override
    public Schema visitSchema(Context context, Schema schema) {
        final SchemaValidator v = SchemaValidator.getInstance();
        v.validate(this, context, schema);
        return schema;
    }

    @Override
    public Schema visitSchema(Context context, String key, Schema schema) {
        final SchemaValidator v = SchemaValidator.getInstance();
        v.validate(this, context, key, schema);
        return schema;
    }

    @Override
    public SecurityScheme visitSecurityScheme(Context context, String key, SecurityScheme scheme) {
        final SecuritySchemeValidator v = SecuritySchemeValidator.getInstance();
        v.validate(this, context, key, scheme);
        return scheme;
    }

    @Override
    public PathItem visitPathItem(Context context, String key, PathItem item) {
        final PathItemValidator v = PathItemValidator.getInstance();
        v.validate(this, context, key, item);
        return item;
    }

    @Override
    public Operation visitOperation(Context context, Operation operation) {
        final OperationValidator v = OperationValidator.getInstance();
        v.validate(this, context, operation);
        return operation;
    }

    @Override
    public MediaType visitMediaType(Context context, String key, MediaType mediaType) {
        final MediaTypeValidator v = MediaTypeValidator.getInstance();
        v.validate(this, context, key, mediaType);
        return mediaType;
    }

    @Override
    public OAuthFlows visitOAuthFlows(Context context, OAuthFlows authFlows) {
        final OAuthFlowsValidator v = OAuthFlowsValidator.getInstance();
        v.validate(this, context, authFlows);
        return authFlows;
    }

    @Override
    public OAuthFlow visitOAuthFlow(Context context, OAuthFlow authFlow) {
        final OAuthFlowValidator v = OAuthFlowValidator.getInstance();
        v.validate(this, context, authFlow);
        return authFlow;
    }

    @Override
    public Discriminator visitDiscriminator(Context context, Discriminator d) {
        final DiscriminatorValidator v = DiscriminatorValidator.getInstance();
        v.validate(this, context, d);
        return d;
    }

    @Override
    public XML visitXML(Context context, XML xml) {
        final XMLValidator v = XMLValidator.getInstance();
        v.validate(this, context, xml);
        return xml;
    }

    @Override
    public Contact visitContact(Context context, Contact contact) {
        final ContactValidator v = ContactValidator.getInstance();
        v.validate(this, context, contact);
        return contact;
    }

    @Override
    public License visitLicense(Context context, License license) {
        final LicenseValidator v = LicenseValidator.getInstance();
        v.validate(this, context, license);
        return license;
    }

    @Override
    public ServerVariable visitServerVariable(Context context, String key, ServerVariable sv) {
        final ServerVariableValidator v = ServerVariableValidator.getInstance();
        v.validate(this, context, key, sv);
        return sv;
    }
}