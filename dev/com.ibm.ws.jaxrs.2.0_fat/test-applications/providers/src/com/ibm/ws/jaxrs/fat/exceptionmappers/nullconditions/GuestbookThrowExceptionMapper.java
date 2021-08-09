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
package com.ibm.ws.jaxrs.fat.exceptionmappers.nullconditions;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class GuestbookThrowExceptionMapper implements ExceptionMapper<GuestbookThrowException> {

    @Override
    public Response toResponse(GuestbookThrowException arg0) {
        /*
         * throwing exception/error in here should cause a HTTP 500 status to
         * occur
         */

        if (arg0.getMessage().contains("exception")) {
            throw new GuestbookNullException();
        } else {
            throw new Error("error");
        }

        // TODO: throw this inside a subresource locator
    }

}
