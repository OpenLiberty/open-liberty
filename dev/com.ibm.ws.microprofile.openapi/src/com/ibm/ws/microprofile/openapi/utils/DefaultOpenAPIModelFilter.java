/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
 * Default implementation of OpenAPIModelFilter. Sub-classes can
 * override methods for portions of the model they are interested in.
 */
public class DefaultOpenAPIModelFilter implements OpenAPIModelFilter {

    /** {@inheritDoc} */
    @Override
    public void visitOpenAPI(Context context) {

    }

    /** {@inheritDoc} */
    @Override
    public Components visitComponents(Context context, Components components) {
        return components;
    }

    /** {@inheritDoc} */
    @Override
    public Object visitExtension(Context context, String key, Object extension) {
        return extension;
    }

    /** {@inheritDoc} */
    @Override
    public ExternalDocumentation visitExternalDocumentation(Context context, ExternalDocumentation extDocs) {
        return extDocs;
    }

    /** {@inheritDoc} */
    @Override
    public Info visitInfo(Context context, Info info) {
        return info;
    }

    /** {@inheritDoc} */
    @Override
    public Paths visitPaths(Context context, Paths paths) {
        return paths;
    }

    /** {@inheritDoc} */
    @Override
    public SecurityRequirement visitSecurityRequirement(Context context, SecurityRequirement sr) {
        return sr;
    }

    /** {@inheritDoc} */
    @Override
    public Server visitServer(Context context, Server server) {
        return server;
    }

    /** {@inheritDoc} */
    @Override
    public Tag visitTag(Context context, Tag tag) {
        return tag;
    }

    /** {@inheritDoc} */
    @Override
    public Callback visitCallback(Context context, String key, Callback callback) {
        return callback;
    }

    /** {@inheritDoc} */
    @Override
    public Example visitExample(Context context, Example example) {
        return example;
    }

    /** {@inheritDoc} */
    @Override
    public Example visitExample(Context context, String key, Example example) {
        return example;
    }

    /** {@inheritDoc} */
    @Override
    public Header visitHeader(Context context, String key, Header header) {
        return header;
    }

    /** {@inheritDoc} */
    @Override
    public Link visitLink(Context context, String key, Link link) {
        return link;
    }

    /** {@inheritDoc} */
    @Override
    public Parameter visitParameter(Context context, Parameter p) {
        return p;
    }

    /** {@inheritDoc} */
    @Override
    public Parameter visitParameter(Context context, String key, Parameter p) {
        return p;
    }

    /** {@inheritDoc} */
    @Override
    public RequestBody visitRequestBody(Context context, RequestBody rb) {
        return rb;
    }

    /** {@inheritDoc} */
    @Override
    public RequestBody visitRequestBody(Context context, String key, RequestBody rb) {
        return rb;
    }

    /** {@inheritDoc} */
    @Override
    public APIResponses visitResponses(Context context, APIResponses responses) {
        return responses;
    }

    /** {@inheritDoc} */
    @Override
    public APIResponse visitResponse(Context context, String key, APIResponse response) {
        return response;
    }

    /** {@inheritDoc} */
    @Override
    public Schema visitSchema(Context context, Schema schema) {
        return schema;
    }

    /** {@inheritDoc} */
    @Override
    public Schema visitSchema(Context context, String key, Schema schema) {
        return schema;
    }

    /** {@inheritDoc} */
    @Override
    public SecurityScheme visitSecurityScheme(Context context, String key, SecurityScheme scheme) {
        return scheme;
    }

    /** {@inheritDoc} */
    @Override
    public PathItem visitPathItem(Context context, String key, PathItem item) {
        return item;
    }

    /** {@inheritDoc} */
    @Override
    public Operation visitOperation(Context context, Operation operation) {
        return operation;
    }

    /** {@inheritDoc} */
    @Override
    public MediaType visitMediaType(Context context, String key, MediaType mediaType) {
        return mediaType;
    }

    /** {@inheritDoc} */
    @Override
    public Encoding visitEncoding(Context context, String key, Encoding encoding) {
        return encoding;
    }

    /** {@inheritDoc} */
    @Override
    public OAuthFlows visitOAuthFlows(Context context, OAuthFlows authFlows) {
        return authFlows;
    }

    /** {@inheritDoc} */
    @Override
    public OAuthFlow visitOAuthFlow(Context context, OAuthFlow authFlow) {
        return authFlow;
    }

    /** {@inheritDoc} */
    @Override
    public Discriminator visitDiscriminator(Context context, Discriminator d) {
        return d;
    }

    /** {@inheritDoc} */
    @Override
    public XML visitXML(Context context, XML xml) {
        return xml;
    }

    /** {@inheritDoc} */
    @Override
    public Scopes visitScopes(Context context, Scopes scopes) {
        return scopes;
    }

    /** {@inheritDoc} */
    @Override
    public Contact visitContact(Context context, Contact contact) {
        return contact;
    }

    /** {@inheritDoc} */
    @Override
    public License visitLicense(Context context, License license) {
        return license;
    }

    /** {@inheritDoc} */
    @Override
    public ServerVariables visitServerVariables(Context context, ServerVariables svs) {
        return svs;
    }

    /** {@inheritDoc} */
    @Override
    public ServerVariable visitServerVariable(Context context, String key, ServerVariable sv) {
        return sv;
    }

}
