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
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

public class MyResponseFilter implements ClientResponseFilter {

    @Override
    public void filter(ClientRequestContext reqCtx, ClientResponseContext resCtx) throws IOException {
        try {
            String jndiResult = (String) new InitialContext().lookup("java:module/ModuleName");
            System.out.println("Response: jndiResult = " + jndiResult);
            System.out.println("Response: thread = " + Thread.currentThread().getName());
            System.out.println("Response: TCCL = " + Thread.currentThread().getContextClassLoader());
            resCtx.getHeaders().putSingle("JNDI-Result", jndiResult);
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

}
