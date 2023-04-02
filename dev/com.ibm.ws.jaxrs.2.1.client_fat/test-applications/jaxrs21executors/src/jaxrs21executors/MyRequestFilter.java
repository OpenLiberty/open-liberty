/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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
            System.out.println("Request: thread = " + Thread.currentThread().getName());
            System.out.println("Request: TCCL = " + Thread.currentThread().getContextClassLoader());
            String jndiResult = (String) new InitialContext().lookup("java:module/ModuleName");
            System.out.println("Request: jndiResult = " + jndiResult);
            reqCtx.abortWith(Response.ok(jndiResult).build());
        } catch (NamingException e) {
            e.printStackTrace();
            reqCtx.abortWith(Response.serverError().entity(e.toString()).build());
        }
    }

}
