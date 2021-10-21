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
package jaxrs21executors;

import java.io.IOException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Response;

public class MyRequestFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext reqCtx) throws IOException {
        try {
            String jndiResult = (String) new InitialContext().lookup("java:module/ModuleName");
            System.out.println("Request: jndiResult = " + jndiResult);
            System.out.println("Request: thread = " + Thread.currentThread().getName());
            System.out.println("Request: TCCL = " + Thread.currentThread().getContextClassLoader());
            reqCtx.abortWith(Response.ok(jndiResult).build());
        } catch (NamingException e) {
            e.printStackTrace();
            reqCtx.abortWith(Response.serverError().entity(e.toString()).build());
        }
    }

}
