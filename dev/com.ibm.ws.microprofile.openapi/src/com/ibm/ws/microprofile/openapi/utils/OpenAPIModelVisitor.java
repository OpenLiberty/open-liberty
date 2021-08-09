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
 * Visitor callback interface for the OpenAPIModelWalker.
 */
public interface OpenAPIModelVisitor {

    public void visitOpenAPI(Context context);

    public void visitComponents(Context context, Components components);

    public void visitExtension(Context context, String key, Object extension);

    public void visitExternalDocumentation(Context context, ExternalDocumentation extDocs);

    public void visitInfo(Context context, Info info);

    public void visitPaths(Context context, Paths paths);

    public void visitSecurityRequirement(Context context, SecurityRequirement sr);

    public void visitServer(Context context, Server server);

    public void visitTag(Context context, Tag tag);

    public void visitCallback(Context context, String key, Callback callback);

    public void visitExample(Context context, Example example);

    public void visitExample(Context context, String key, Example example);

    public void visitHeader(Context context, String key, Header header);

    public void visitLink(Context context, String key, Link link);

    public void visitParameter(Context context, Parameter p);

    public void visitParameter(Context context, String key, Parameter p);

    public void visitRequestBody(Context context, RequestBody rb);

    public void visitRequestBody(Context context, String key, RequestBody rb);

    public void visitResponses(Context context, APIResponses responses);

    public void visitResponse(Context context, String key, APIResponse response);

    public void visitSchema(Context context, Schema schema);

    public void visitSchema(Context context, String key, Schema schema);

    public void visitSecurityScheme(Context context, String key, SecurityScheme scheme);

    public void visitPathItem(Context context, String key, PathItem item);

    public void visitOperation(Context context, Operation operation);

    public void visitMediaType(Context context, String key, MediaType mediaType);

    public void visitEncoding(Context context, String key, Encoding encoding);

    public void visitOAuthFlows(Context context, OAuthFlows authFlows);

    public void visitOAuthFlow(Context context, OAuthFlow authFlow);

    public void visitDiscriminator(Context context, Discriminator d);

    public void visitXML(Context context, XML xml);

    public void visitScopes(Context context, Scopes scopes);

    public void visitContact(Context context, Contact contact);

    public void visitLicense(Context context, License license);

    public void visitServerVariables(Context context, ServerVariables svs);

    public void visitServerVariable(Context context, String key, ServerVariable sv);

}
