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
package mpRestClient11.async;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

@Provider
public class InsufficientFundsExceptionMapper implements ExceptionMapper<InsufficientFundsException>, ResponseExceptionMapper<InsufficientFundsException> {

    /* (non-Javadoc)
     * @see org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper#toThrowable(javax.ws.rs.core.Response)
     */
    @Override
    public InsufficientFundsException toThrowable(Response response) {
        if (response.getStatus() == 413) {
            return new InsufficientFundsException();
        }
        return null;
    }

    /* (non-Javadoc)
     * @see javax.ws.rs.ext.ExceptionMapper#toResponse(java.lang.Throwable)
     */
    @Override
    public Response toResponse(InsufficientFundsException ex) {
        return Response.status(413).build();
    }

}
