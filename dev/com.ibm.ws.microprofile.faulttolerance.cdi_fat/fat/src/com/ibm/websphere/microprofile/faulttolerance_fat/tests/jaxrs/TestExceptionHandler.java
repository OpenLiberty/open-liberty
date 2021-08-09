/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.microprofile.faulttolerance_fat.tests.jaxrs;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Exception for our test exception to prevent the container complaining about unhandled exceptions being thrown
 */
@Provider
public class TestExceptionHandler implements ExceptionMapper<TestException> {

    @Override
    public Response toResponse(TestException exception) {
        Response response = Response.serverError()
                        .type(MediaType.TEXT_PLAIN_TYPE)
                        .entity(exception.toString())
                        .build();
        return response;
    }

}
