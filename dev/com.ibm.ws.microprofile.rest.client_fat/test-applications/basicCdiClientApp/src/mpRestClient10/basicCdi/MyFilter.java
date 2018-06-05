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
package mpRestClient10.basicCdi;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

public class MyFilter implements ClientRequestFilter, ClientResponseFilter {

    static boolean requestFilterInvoked;
    static boolean responseFilterInvoked;

    /*
     * (non-Javadoc)
     * 
     * @see javax.ws.rs.client.ClientResponseFilter#filter(javax.ws.rs.client.ClientRequestContext, javax.ws.rs.client.ClientResponseContext)
     */
    @Override
    public void filter(ClientRequestContext arg0, ClientResponseContext arg1) throws IOException {
        responseFilterInvoked = true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.ws.rs.client.ClientRequestFilter#filter(javax.ws.rs.client.ClientRequestContext)
     */
    @Override
    public void filter(ClientRequestContext arg0) throws IOException {
        requestFilterInvoked = true;
    }

}
