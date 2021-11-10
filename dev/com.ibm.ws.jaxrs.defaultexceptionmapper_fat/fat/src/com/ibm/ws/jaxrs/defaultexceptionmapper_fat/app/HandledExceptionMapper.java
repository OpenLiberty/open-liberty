/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.defaultexceptionmapper_fat.app;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class HandledExceptionMapper implements ExceptionMapper<HandledException> {

    @Override
    public Response toResponse(HandledException ex) {
        return Response.status(400)
                       .type(MediaType.TEXT_PLAIN_TYPE)
                       .entity("Exception Handled")
                       .build();
    }

}
