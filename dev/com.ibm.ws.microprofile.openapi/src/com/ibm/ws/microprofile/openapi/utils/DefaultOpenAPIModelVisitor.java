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

import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
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
import org.eclipse.microprofile.openapi.models.media.Encoding;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
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

import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 * Default implementation of OpenAPIModelVistor. Sub-classes can
 * override methods for portions of the model they are interested in.
 */
public class DefaultOpenAPIModelVisitor implements OpenAPIModelVisitor {

    /** {@inheritDoc} */
    @Override
    public void visitOpenAPI(Context context) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitComponents(Context context, Components components) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitExtension(Context context, String key, Object extension) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitExternalDocumentation(Context context, ExternalDocumentation extDocs) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitInfo(Context context, Info info) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitPaths(Context context, Paths paths) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitSecurityRequirement(Context context, SecurityRequirement sr) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitServer(Context context, Server server) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitTag(Context context, Tag tag) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitCallback(Context context, String key, Callback callback) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitExample(Context context, Example example) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitExample(Context context, String key, Example example) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitHeader(Context context, String key, Header header) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitLink(Context context, String key, Link link) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitParameter(Context context, Parameter p) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitParameter(Context context, String key, Parameter p) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitRequestBody(Context context, RequestBody rb) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitRequestBody(Context context, String key, RequestBody rb) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitResponses(Context context, APIResponses responses) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitResponse(Context context, String key, APIResponse response) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitSchema(Context context, Schema schema) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitSchema(Context context, String key, Schema schema) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitSecurityScheme(Context context, String key, SecurityScheme scheme) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitPathItem(Context context, String key, PathItem item) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitOperation(Context context, Operation operation) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitMediaType(Context context, String key, MediaType mediaType) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitEncoding(Context context, String key, Encoding encoding) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitOAuthFlows(Context context, OAuthFlows authFlows) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitOAuthFlow(Context context, OAuthFlow authFlow) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitDiscriminator(Context context, Discriminator d) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitXML(Context context, XML xml) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitScopes(Context context, Scopes scopes) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitContact(Context context, Contact contact) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitLicense(Context context, License license) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitServerVariables(Context context, ServerVariables svs) {
        // TODO Auto-generated method stub

    }

    /** {@inheritDoc} */
    @Override
    public void visitServerVariable(Context context, String key, ServerVariable sv) {
        // TODO Auto-generated method stub

    }
}
