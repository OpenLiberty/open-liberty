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
import java.util.logging.Logger;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.spi.RestClientBuilderListener;

public class RestClientAuditLogger implements RestClientBuilderListener {
    static Logger _log = Logger.getLogger(RestClientAuditLogger.class.getName());
    
    @Override
    public void onNewBuilder(RestClientBuilder builder) {
        builder.register(UniqueURIFilter.class)
               .register(new ClientRequestFilter() {
            @Override
            public void filter(ClientRequestContext ctx) throws IOException {
                _log.info("New request to: " + ctx.getUri());
            }});
    }
}
