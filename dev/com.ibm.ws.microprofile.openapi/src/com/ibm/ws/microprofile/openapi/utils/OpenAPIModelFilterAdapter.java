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
 *
 */
public final class OpenAPIModelFilterAdapter implements OpenAPIModelFilter {

    private final OpenAPIModelVisitor visitor;

    public OpenAPIModelFilterAdapter(OpenAPIModelVisitor visitor) {
        this.visitor = visitor;
    }

    /** {@inheritDoc} */
    @Override
    public void visitOpenAPI(Context context) {
        visitor.visitOpenAPI(context);
    }

    /** {@inheritDoc} */
    @Override
    public Components visitComponents(Context context, Components components) {
        visitor.visitComponents(context, components);
        return components;
    }

    /** {@inheritDoc} */
    @Override
    public Object visitExtension(Context context, String key, Object extension) {
        visitor.visitExtension(context, key, extension);
        return extension;
    }

    /** {@inheritDoc} */
    @Override
    public ExternalDocumentation visitExternalDocumentation(Context context, ExternalDocumentation extDocs) {
        visitor.visitExternalDocumentation(context, extDocs);
        return extDocs;
    }

    /** {@inheritDoc} */
    @Override
    public Info visitInfo(Context context, Info info) {
        visitor.visitInfo(context, info);
        return info;
    }

    /** {@inheritDoc} */
    @Override
    public Paths visitPaths(Context context, Paths paths) {
        visitor.visitPaths(context, paths);
        return paths;
    }

    /** {@inheritDoc} */
    @Override
    public SecurityRequirement visitSecurityRequirement(Context context, SecurityRequirement sr) {
        visitor.visitSecurityRequirement(context, sr);
        return sr;
    }

    /** {@inheritDoc} */
    @Override
    public Server visitServer(Context context, Server server) {
        visitor.visitServer(context, server);
        return server;
    }

    /** {@inheritDoc} */
    @Override
    public Tag visitTag(Context context, Tag tag) {
        visitor.visitTag(context, tag);
        return tag;
    }

    /** {@inheritDoc} */
    @Override
    public Callback visitCallback(Context context, String key, Callback callback) {
        visitor.visitCallback(context, key, callback);
        return callback;
    }

    /** {@inheritDoc} */
    @Override
    public Example visitExample(Context context, Example example) {
        visitor.visitExample(context, example);
        return example;
    }

    /** {@inheritDoc} */
    @Override
    public Example visitExample(Context context, String key, Example example) {
        visitor.visitExample(context, key, example);
        return example;
    }

    /** {@inheritDoc} */
    @Override
    public Header visitHeader(Context context, String key, Header header) {
        visitor.visitHeader(context, key, header);
        return header;
    }

    /** {@inheritDoc} */
    @Override
    public Link visitLink(Context context, String key, Link link) {
        visitor.visitLink(context, key, link);
        return link;
    }

    /** {@inheritDoc} */
    @Override
    public Parameter visitParameter(Context context, Parameter p) {
        visitor.visitParameter(context, p);
        return p;
    }

    /** {@inheritDoc} */
    @Override
    public Parameter visitParameter(Context context, String key, Parameter p) {
        visitor.visitParameter(context, key, p);
        return p;
    }

    /** {@inheritDoc} */
    @Override
    public RequestBody visitRequestBody(Context context, RequestBody rb) {
        visitor.visitRequestBody(context, rb);
        return rb;
    }

    /** {@inheritDoc} */
    @Override
    public RequestBody visitRequestBody(Context context, String key, RequestBody rb) {
        visitor.visitRequestBody(context, key, rb);
        return rb;
    }

    /** {@inheritDoc} */
    @Override
    public APIResponses visitResponses(Context context, APIResponses responses) {
        visitor.visitResponses(context, responses);
        return responses;
    }

    /** {@inheritDoc} */
    @Override
    public APIResponse visitResponse(Context context, String key, APIResponse response) {
        visitor.visitResponse(context, key, response);
        return response;
    }

    /** {@inheritDoc} */
    @Override
    public Schema visitSchema(Context context, Schema schema) {
        visitor.visitSchema(context, schema);
        return schema;
    }

    /** {@inheritDoc} */
    @Override
    public Schema visitSchema(Context context, String key, Schema schema) {
        visitor.visitSchema(context, key, schema);
        return schema;
    }

    /** {@inheritDoc} */
    @Override
    public SecurityScheme visitSecurityScheme(Context context, String key, SecurityScheme scheme) {
        visitor.visitSecurityScheme(context, key, scheme);
        return scheme;
    }

    /** {@inheritDoc} */
    @Override
    public PathItem visitPathItem(Context context, String key, PathItem item) {
        visitor.visitPathItem(context, key, item);
        return item;
    }

    /** {@inheritDoc} */
    @Override
    public Operation visitOperation(Context context, Operation operation) {
        visitor.visitOperation(context, operation);
        return operation;
    }

    /** {@inheritDoc} */
    @Override
    public MediaType visitMediaType(Context context, String key, MediaType mediaType) {
        visitor.visitMediaType(context, key, mediaType);
        return mediaType;
    }

    /** {@inheritDoc} */
    @Override
    public Encoding visitEncoding(Context context, String key, Encoding encoding) {
        visitor.visitEncoding(context, key, encoding);
        return encoding;
    }

    /** {@inheritDoc} */
    @Override
    public OAuthFlows visitOAuthFlows(Context context, OAuthFlows authFlows) {
        visitor.visitOAuthFlows(context, authFlows);
        return authFlows;
    }

    /** {@inheritDoc} */
    @Override
    public OAuthFlow visitOAuthFlow(Context context, OAuthFlow authFlow) {
        visitor.visitOAuthFlow(context, authFlow);
        return authFlow;
    }

    /** {@inheritDoc} */
    @Override
    public Discriminator visitDiscriminator(Context context, Discriminator d) {
        visitor.visitDiscriminator(context, d);
        return d;
    }

    /** {@inheritDoc} */
    @Override
    public XML visitXML(Context context, XML xml) {
        visitor.visitXML(context, xml);
        return xml;
    }

    /** {@inheritDoc} */
    @Override
    public Scopes visitScopes(Context context, Scopes scopes) {
        visitor.visitScopes(context, scopes);
        return scopes;
    }

    /** {@inheritDoc} */
    @Override
    public Contact visitContact(Context context, Contact contact) {
        visitor.visitContact(context, contact);
        return contact;
    }

    /** {@inheritDoc} */
    @Override
    public License visitLicense(Context context, License license) {
        visitor.visitLicense(context, license);
        return license;
    }

    /** {@inheritDoc} */
    @Override
    public ServerVariables visitServerVariables(Context context, ServerVariables svs) {
        visitor.visitServerVariables(context, svs);
        return svs;
    }

    /** {@inheritDoc} */
    @Override
    public ServerVariable visitServerVariable(Context context, String key, ServerVariable sv) {
        visitor.visitServerVariable(context, key, sv);
        return sv;
    }

}
