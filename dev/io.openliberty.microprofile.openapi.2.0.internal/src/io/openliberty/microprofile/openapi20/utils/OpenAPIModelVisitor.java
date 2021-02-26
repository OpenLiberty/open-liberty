/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse default License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.utils;

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
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.eclipse.microprofile.openapi.models.tags.Tag;

import io.openliberty.microprofile.openapi20.utils.OpenAPIModelWalker.Context;

/**
 * Visitor callback interface for the OpenAPIModelWalker.
 */
public interface OpenAPIModelVisitor {

    default void visitOpenAPI(Context context) {};

    default Components visitComponents(Context context, Components components) { return components; };

    default Object visitExtension(Context context, String key, Object extension) { return extension; };

    default ExternalDocumentation visitExternalDocumentation(Context context, ExternalDocumentation extDocs) { return extDocs; };

    default Info visitInfo(Context context, Info info) { return info; };

    default Paths visitPaths(Context context, Paths paths) { return paths; };

    default SecurityRequirement visitSecurityRequirement(Context context, SecurityRequirement secReq) { return secReq; };

    default Server visitServer(Context context, Server server) { return server; };

    default Tag visitTag(Context context, Tag tag) { return tag; };

    default Callback visitCallback(Context context, String key, Callback callback) {return callback; };

    default Example visitExample(Context context, Example example) { return example; };

    default Example visitExample(Context context, String key, Example example) { return example; };

    default Header visitHeader(Context context, String key, Header header) { return header; };

    default Link visitLink(Context context, String key, Link link) { return link; };

    default Parameter visitParameter(Context context, Parameter parameter) { return parameter; };

    default Parameter visitParameter(Context context, String key, Parameter parameter) { return parameter; };

    default RequestBody visitRequestBody(Context context, RequestBody requestBody) { return requestBody; };

    default RequestBody visitRequestBody(Context context, String key, RequestBody requestBody) { return requestBody; };

    default APIResponses visitResponses(Context context, APIResponses responses) { return responses; };

    default APIResponse visitResponse(Context context, String key, APIResponse response) { return response; };

    default Schema visitSchema(Context context, Schema schema) { return schema; };

    default Schema visitSchema(Context context, String key, Schema schema) { return schema; };

    default SecurityScheme visitSecurityScheme(Context context, String key, SecurityScheme scheme) { return scheme; };

    default PathItem visitPathItem(Context context, String key, PathItem pathItem) { return pathItem; };

    default Operation visitOperation(Context context, Operation operation) { return operation; };

    default MediaType visitMediaType(Context context, String key, MediaType mediaType) { return mediaType; };

    default Encoding visitEncoding(Context context, String key, Encoding encoding) { return encoding; };

    default OAuthFlows visitOAuthFlows(Context context, OAuthFlows authFlows) { return authFlows; };

    default OAuthFlow visitOAuthFlow(Context context, OAuthFlow authFlow) { return authFlow; };

    default Discriminator visitDiscriminator(Context context, Discriminator discriminator) {return discriminator; };

    default XML visitXML(Context context, XML xml) { return xml; };

    default Contact visitContact(Context context, Contact contact) { return contact; };

    default License visitLicense(Context context, License license) { return license; };

    default ServerVariable visitServerVariable(Context context, String key, ServerVariable serverVariable) {return serverVariable; };
}
