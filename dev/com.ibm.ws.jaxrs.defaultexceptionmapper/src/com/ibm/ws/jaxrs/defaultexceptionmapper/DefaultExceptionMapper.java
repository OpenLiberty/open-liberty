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

package com.ibm.ws.jaxrs.defaultexceptionmapper;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.ibm.ws.jaxrs.defaultexceptionmapper.internal.DefaultExceptionMapperCallbackTracker;

@Provider
public class DefaultExceptionMapper implements ExceptionMapper<Throwable> {
    static final long serialVersionUID = 9083611544695926229L;

    @Context
    private ResourceInfo resourceInfo;

    private final List<DefaultExceptionMapperCallback> callbacks = new LinkedList<>();

    public DefaultExceptionMapper() {
        this.callbacks.addAll(DefaultExceptionMapperCallbackTracker.getCallbacks());
    }

    @Override
    public Response toResponse(Throwable t) {
        Response response;
        if (t instanceof WebApplicationException && ((WebApplicationException) t).getResponse() != null) {
            response = ((WebApplicationException) t).getResponse();
        } else {
            response = Response.serverError().build();
        }
        Map<String, Object> callbackHeaders = new HashMap<>();
        Map<String, Object> callbackHeader;
        for (DefaultExceptionMapperCallback callback : this.callbacks) {
            callbackHeader = callback.onDefaultMappedException(t, response.getStatus(), this.resourceInfo);
            if (callbackHeader != null) {
                callbackHeaders.putAll(callbackHeader);
            }
        }
        if (!callbackHeaders.isEmpty()) {
            Response.ResponseBuilder builder = Response.fromResponse(response);
            for (Map.Entry<String, Object> entry : callbackHeaders.entrySet())
                builder = builder.header(entry.getKey(), entry.getValue());
            response = builder.build();
        }
        return response;
    }
}
