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
package mpRestClient11.produceConsume;

import java.io.IOException;
import java.util.logging.Logger;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

/**
 * Aborts with response showing "x:y" where x is the header value for Content-Type
 * and y is the header value for Accept.
 */
public class Filter implements ClientRequestFilter {
    private final static Logger _log = Logger.getLogger(Filter.class.getName());

    /* (non-Javadoc)
     * @see javax.ws.rs.client.ClientRequestFilter#filter(javax.ws.rs.client.ClientRequestContext)
     */
    @Override
    public void filter(ClientRequestContext ctx) throws IOException {
        String contentType = ctx.getHeaderString(HttpHeaders.CONTENT_TYPE);
        String accept = ctx.getHeaderString(HttpHeaders.ACCEPT);
        _log.info("filter Content-type: " + contentType + "   Accept: " + accept);
        ctx.abortWith(Response.ok()
                              .header("Sent-Accept", accept)
                              .header("Sent-ContentType", contentType)
                              .build());

    }

}
