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
package com.ibm.ws.jaxrs.fat.exceptionMappingWithOT;


import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.core.MediaType;

@Provider
public class MyExceptionHandler implements ExceptionMapper<Exception> {

        @Override
        public Response toResponse(Exception e) {
            System.out.println("MyExceptionHandler.toResponse()");
            return Response.status(Response.Status.GONE)
                           .entity("Exception was mapped!")
                           .type(MediaType.TEXT_PLAIN)
                           .build();
        }

}