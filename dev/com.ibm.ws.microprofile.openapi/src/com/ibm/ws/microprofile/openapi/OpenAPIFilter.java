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
package com.ibm.ws.microprofile.openapi;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.tags.Tag;

import com.ibm.ws.microprofile.openapi.utils.DefaultOpenAPIModelFilter;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker;
import com.ibm.ws.microprofile.openapi.utils.OpenAPIModelWalker.Context;

/**
 *
 */
public class OpenAPIFilter extends DefaultOpenAPIModelFilter {

    public OASFilter filter;

    public OpenAPIFilter(OASFilter filter) {
        this.filter = filter;
    }

    public void filter(OpenAPI model) {
        OpenAPIModelWalker walker = new OpenAPIModelWalker(model);
        walker.accept(this, false);
    }

    /** {@inheritDoc} */
    @Override
    public void visitOpenAPI(Context context) {
        filter.filterOpenAPI(context.getModel());
    }

    /** {@inheritDoc} */
    @Override
    public Server visitServer(Context context, Server server) {
        return filter.filterServer(server);
    }

    /** {@inheritDoc} */
    @Override
    public Tag visitTag(Context context, Tag tag) {
        return filter.filterTag(tag);
    }

    /** {@inheritDoc} */
    @Override
    public Callback visitCallback(Context context, String key, Callback callback) {
        return filter.filterCallback(callback);
    }

    /** {@inheritDoc} */
    @Override
    public Header visitHeader(Context context, String key, Header header) {
        return filter.filterHeader(header);
    }

    /** {@inheritDoc} */
    @Override
    public Link visitLink(Context context, String key, Link link) {
        return filter.filterLink(link);
    }

    /** {@inheritDoc} */
    @Override
    public Parameter visitParameter(Context context, Parameter p) {
        return filter.filterParameter(p);
    }

    /** {@inheritDoc} */
    @Override
    public Parameter visitParameter(Context context, String key, Parameter p) {
        return filter.filterParameter(p);
    }

    /** {@inheritDoc} */
    @Override
    public RequestBody visitRequestBody(Context context, RequestBody rb) {
        return filter.filterRequestBody(rb);
    }

    /** {@inheritDoc} */
    @Override
    public RequestBody visitRequestBody(Context context, String key, RequestBody rb) {
        return filter.filterRequestBody(rb);
    }

    /** {@inheritDoc} */
    @Override
    public APIResponse visitResponse(Context context, String key, APIResponse response) {
        return filter.filterAPIResponse(response);
    }

    /** {@inheritDoc} */
    @Override
    public Schema visitSchema(Context context, Schema schema) {
        return filter.filterSchema(schema);
    }

    /** {@inheritDoc} */
    @Override
    public Schema visitSchema(Context context, String key, Schema schema) {
        return filter.filterSchema(schema);
    }

    /** {@inheritDoc} */
    @Override
    public SecurityScheme visitSecurityScheme(Context context, String key, SecurityScheme scheme) {
        return filter.filterSecurityScheme(scheme);
    }

    /** {@inheritDoc} */
    @Override
    public PathItem visitPathItem(Context context, String key, PathItem item) {
        return filter.filterPathItem(item);
    }

    /** {@inheritDoc} */
    @Override
    public Operation visitOperation(Context context, Operation operation) {
        return filter.filterOperation(operation);
    }
}
