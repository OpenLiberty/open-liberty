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
package com.ibm.ws.microprofile.openapi.impl.validation;

import java.util.HashSet;
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
import org.eclipse.microprofile.openapi.models.servers.ServerVariables;
import org.eclipse.microprofile.openapi.models.tags.Tag;

import com.ibm.ws.microprofile.openapi.impl.validation.OASValidationResult.ValidationEvent;
import com.ibm.ws.microprofile.openapi.utils.DefaultOpenAPIModelVisitor;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 * OpenAPI model validator. Checks structural constraints on the model and reports errors for violations.
 */
public final class OASValidator extends DefaultOpenAPIModelVisitor implements ValidationHelper {

    private OASValidationResult result;
    private final Set<String> operationIds = new HashSet<>();

    public OASValidationResult validate(OpenAPI model) {
        result = new OASValidationResult();
        operationIds.clear();
        OpenAPIModelWalker walker = new OpenAPIModelWalker(model);
        walker.accept(this);
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

    @Override
    public void visitOpenAPI(Context context) {
        final OpenAPIValidator v = OpenAPIValidator.getInstance();
        v.validate(this, context, context.getModel());
    }

    @Override
    public void visitComponents(Context context, Components components) {
        final ComponentsValidator v = ComponentsValidator.getInstance();
        v.validate(this, context, components);
    }

    @Override
    public void visitExtension(Context context, String key, Object extension) {
        final ExtensionValidator v = ExtensionValidator.getInstance();
        v.validate(this, context, key, extension);
    }

    @Override
    public void visitExternalDocumentation(Context context, ExternalDocumentation extDocs) {
        final ExternalDocumentationValidator v = ExternalDocumentationValidator.getInstance();
        v.validate(this, context, extDocs);
    }

    @Override
    public void visitInfo(Context context, Info info) {
        final InfoValidator v = InfoValidator.getInstance();
        v.validate(this, context, info);
    }

    @Override
    public void visitPaths(Context context, Paths paths) {
        final PathsValidator v = PathsValidator.getInstance();
        v.validate(this, context, paths);
    }

    @Override
    public void visitSecurityRequirement(Context context, SecurityRequirement sr) {
        final SecurityRequirementValidator v = SecurityRequirementValidator.getInstance();
        v.validate(this, context, sr);
    }

    @Override
    public void visitServer(Context context, Server server) {
        final ServerValidator v = ServerValidator.getInstance();
        v.validate(this, context, server);
    }

    @Override
    public void visitTag(Context context, Tag tag) {
        final TagValidator v = TagValidator.getInstance();
        v.validate(this, context, tag);
    }

    @Override
    public void visitCallback(Context context, String key, Callback callback) {
        final CallbackValidator v = CallbackValidator.getInstance();
        v.validate(this, context, key, callback);
    }

    @Override
    public void visitExample(Context context, Example example) {
        final ExampleValidator v = ExampleValidator.getInstance();
        v.validate(this, context, example);
    }

    @Override
    public void visitExample(Context context, String key, Example example) {
        final ExampleValidator v = ExampleValidator.getInstance();
        v.validate(this, context, key, example);
    }

    @Override
    public void visitHeader(Context context, String key, Header header) {
        final HeaderValidator v = HeaderValidator.getInstance();
        v.validate(this, context, key, header);
    }

    @Override
    public void visitLink(Context context, String key, Link link) {
        final LinkValidator v = LinkValidator.getInstance();
        v.validate(this, context, key, link);
    }

    @Override
    public void visitParameter(Context context, Parameter p) {
        final ParameterValidator v = ParameterValidator.getInstance();
        v.validate(this, context, p);
    }

    @Override
    public void visitParameter(Context context, String key, Parameter p) {
        final ParameterValidator v = ParameterValidator.getInstance();
        v.validate(this, context, key, p);
    }

    @Override
    public void visitRequestBody(Context context, RequestBody rb) {
        final RequestBodyValidator v = RequestBodyValidator.getInstance();
        v.validate(this, context, rb);
    }

    @Override
    public void visitRequestBody(Context context, String key, RequestBody rb) {
        final RequestBodyValidator v = RequestBodyValidator.getInstance();
        v.validate(this, context, key, rb);
    }

    @Override
    public void visitResponses(Context context, APIResponses responses) {
        final ResponsesValidator v = ResponsesValidator.getInstance();
        v.validate(this, context, responses);
    }

    @Override
    public void visitResponse(Context context, String key, APIResponse response) {
        final ResponseValidator v = ResponseValidator.getInstance();
        v.validate(this, context, key, response);
    }

    @Override
    public void visitSchema(Context context, Schema schema) {
        final SchemaValidator v = SchemaValidator.getInstance();
        v.validate(this, context, schema);
    }

    @Override
    public void visitSchema(Context context, String key, Schema schema) {
        final SchemaValidator v = SchemaValidator.getInstance();
        v.validate(this, context, key, schema);
    }

    @Override
    public void visitSecurityScheme(Context context, String key, SecurityScheme scheme) {
        final SecuritySchemeValidator v = SecuritySchemeValidator.getInstance();
        v.validate(this, context, key, scheme);
    }

    @Override
    public void visitPathItem(Context context, String key, PathItem item) {
        final PathItemValidator v = PathItemValidator.getInstance();
        v.validate(this, context, key, item);
    }

    @Override
    public void visitOperation(Context context, Operation operation) {
        final OperationValidator v = OperationValidator.getInstance();
        v.validate(this, context, operation);
    }

    @Override
    public void visitMediaType(Context context, String key, MediaType mediaType) {
        final MediaTypeValidator v = MediaTypeValidator.getInstance();
        v.validate(this, context, key, mediaType);
    }

    @Override
    public void visitOAuthFlows(Context context, OAuthFlows authFlows) {
        final OAuthFlowsValidator v = OAuthFlowsValidator.getInstance();
        v.validate(this, context, authFlows);
    }

    @Override
    public void visitOAuthFlow(Context context, OAuthFlow authFlow) {
        final OAuthFlowValidator v = OAuthFlowValidator.getInstance();
        v.validate(this, context, authFlow);
    }

    @Override
    public void visitDiscriminator(Context context, Discriminator d) {
        final DiscriminatorValidator v = DiscriminatorValidator.getInstance();
        v.validate(this, context, d);
    }

    @Override
    public void visitXML(Context context, XML xml) {
        final XMLValidator v = XMLValidator.getInstance();
        v.validate(this, context, xml);
    }

    @Override
    public void visitContact(Context context, Contact contact) {
        final ContactValidator v = ContactValidator.getInstance();
        v.validate(this, context, contact);
    }

    @Override
    public void visitLicense(Context context, License license) {
        final LicenseValidator v = LicenseValidator.getInstance();
        v.validate(this, context, license);
    }

    @Override
    public void visitServerVariables(Context context, ServerVariables svs) {
        final ServerVariablesValidator v = ServerVariablesValidator.getInstance();
        v.validate(this, context, svs);
    }

    @Override
    public void visitServerVariable(Context context, String key, ServerVariable sv) {
        final ServerVariableValidator v = ServerVariableValidator.getInstance();
        v.validate(this, context, key, sv);
    }
}
