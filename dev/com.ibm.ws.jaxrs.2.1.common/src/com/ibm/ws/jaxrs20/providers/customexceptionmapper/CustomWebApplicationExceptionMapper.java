/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.providers.customexceptionmapper;

import java.util.HashMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.apache.cxf.jaxrs.impl.WebApplicationExceptionMapper;

/**
 * 
 * This is used to do our own error code mapping.
 */
public class CustomWebApplicationExceptionMapper extends WebApplicationExceptionMapper
                implements ExceptionMapper<WebApplicationException> {
    private static HashMap<String, Integer> customErrorCodeMap = new HashMap<String, Integer>();

    //uncomment below method to add your custom error code mapping
    //Usage:put className:MethodName:position in stack trace and your expected error code into the map

//    static {
//
//        customErrorCodeMap.put("org.apache.cxf.jaxrs.provider.SourceProvider:readFrom:2", 415);
//        customErrorCodeMap.put("org.apache.cxf.jaxrs.provider.BinaryDataProvider:readFrom:2", 415);
//
//    }

    @Override
    public Response toResponse(WebApplicationException exception) {
        if (!customErrorCodeMap.isEmpty())
        {
            StackTraceElement[] st = exception.getStackTrace();
            int length = st.length;
            for (int i = 0; i < length; i++) {
                StackTraceElement s = st[i];
                String classandMethodName = s.getClassName() + ":" + s.getMethodName() + ":" + i;
                if (customErrorCodeMap.keySet().contains(classandMethodName))
                {
                    return Response.status(customErrorCodeMap.get(classandMethodName)).build();
                }
            }
        }

        return super.toResponse(exception);
    }
}
