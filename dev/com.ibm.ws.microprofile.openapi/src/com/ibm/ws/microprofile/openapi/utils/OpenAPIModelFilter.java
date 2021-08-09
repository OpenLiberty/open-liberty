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
 * Filtering visitor callback interface for the OpenAPIModelWalker.
 */
public interface OpenAPIModelFilter {

    public void visitOpenAPI(Context context);

    public Components visitComponents(Context context, Components components);

    public Object visitExtension(Context context, String key, Object extension);

    public ExternalDocumentation visitExternalDocumentation(Context context, ExternalDocumentation extDocs);

    public Info visitInfo(Context context, Info info);

    public Paths visitPaths(Context context, Paths paths);

    public SecurityRequirement visitSecurityRequirement(Context context, SecurityRequirement sr);

    public Server visitServer(Context context, Server server);

    public Tag visitTag(Context context, Tag tag);

    public Callback visitCallback(Context context, String key, Callback callback);

    public Example visitExample(Context context, Example example);

    public Example visitExample(Context context, String key, Example example);

    public Header visitHeader(Context context, String key, Header header);

    public Link visitLink(Context context, String key, Link link);

    public Parameter visitParameter(Context context, Parameter p);

    public Parameter visitParameter(Context context, String key, Parameter p);

    public RequestBody visitRequestBody(Context context, RequestBody rb);

    public RequestBody visitRequestBody(Context context, String key, RequestBody rb);

    public APIResponses visitResponses(Context context, APIResponses responses);

    public APIResponse visitResponse(Context context, String key, APIResponse response);

    public Schema visitSchema(Context context, Schema schema);

    public Schema visitSchema(Context context, String key, Schema schema);

    public SecurityScheme visitSecurityScheme(Context context, String key, SecurityScheme scheme);

    public PathItem visitPathItem(Context context, String key, PathItem item);

    public Operation visitOperation(Context context, Operation operation);

    public MediaType visitMediaType(Context context, String key, MediaType mediaType);

    public Encoding visitEncoding(Context context, String key, Encoding encoding);

    public OAuthFlows visitOAuthFlows(Context context, OAuthFlows authFlows);

    public OAuthFlow visitOAuthFlow(Context context, OAuthFlow authFlow);

    public Discriminator visitDiscriminator(Context context, Discriminator d);

    public XML visitXML(Context context, XML xml);

    public Scopes visitScopes(Context context, Scopes scopes);

    public Contact visitContact(Context context, Contact contact);

    public License visitLicense(Context context, License license);

    public ServerVariables visitServerVariables(Context context, ServerVariables svs);

    public ServerVariable visitServerVariable(Context context, String key, ServerVariable sv);

}
