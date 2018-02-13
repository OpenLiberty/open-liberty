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
package mpRestClient10.multiClientCdi;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class TestClientFilter implements ClientRequestFilter {

    /*
     * (non-Javadoc)
     *
     * @see javax.ws.rs.client.ClientRequestFilter#filter(javax.ws.rs.client.ClientRequestContext)
     */
    @Override
    public void filter(ClientRequestContext context) throws IOException {
        String uri = context.getUri().toString();
        if (uri.endsWith("A")) {
            context.abortWith(Response.ok("ResultA").build());
        } else if (uri.endsWith("B")) {
            context.abortWith(Response.ok("ResultB").build());
        } else {
            context.abortWith(Response.status(404).build());
        }
    }

}
