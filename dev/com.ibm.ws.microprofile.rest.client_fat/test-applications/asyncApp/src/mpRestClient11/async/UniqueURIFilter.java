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

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.USER-1)
public class UniqueURIFilter implements ClientRequestFilter {

    private static AtomicInteger counter = new AtomicInteger(1);
    @Override
    public void filter(ClientRequestContext ctx) throws IOException {
        String uri = ctx.getUri().toString();
        char queryMarker = uri.contains("?") ? '&' : '?';
        ctx.setUri(URI.create(uri + queryMarker + "REQUEST_ID=" + counter.getAndIncrement()));

    }

}
