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
package mpGraphQL10.basicQuery;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

public class LoggingFilter implements ClientResponseFilter, ClientRequestFilter {


    private final static Logger LOG = Logger.getLogger(LoggingFilter.class.getName());

    @Override
    public void filter(ClientRequestContext reqCtx) throws IOException {
        Object entity = reqCtx.getEntity();
        LOG.log(Level.INFO, "Request entity={0}", new Object[]{entity});

    }

    @Override
    public void filter(ClientRequestContext reqCtx, ClientResponseContext resCtx) throws IOException {
        int status = resCtx.getStatus();
        String entity;
        byte[] buf = new byte[1024];
        BufferedReader br = new BufferedReader(new InputStreamReader(resCtx.getEntityStream()));
        StringBuilder entityBuilder = new StringBuilder();
        String line = br.readLine();
        while (line != null) {
            entityBuilder.append(line).append(System.lineSeparator());
            line = br.readLine();
        }
        entity = entityBuilder.toString();
        ByteArrayInputStream bais = new ByteArrayInputStream(entity.getBytes());
        resCtx.setEntityStream(bais);

        LOG.log(Level.INFO, "Response status={0} entity={1}", new Object[]{status, entity});
    }

}
