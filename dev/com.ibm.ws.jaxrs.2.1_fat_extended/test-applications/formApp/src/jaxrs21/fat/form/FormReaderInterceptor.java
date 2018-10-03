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
package jaxrs21.fat.form;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

@Provider
public class FormReaderInterceptor implements ReaderInterceptor {
    private static final Logger LOG = Logger.getLogger(FormReaderInterceptor.class.getName());

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext ctx) throws IOException, WebApplicationException {
        BufferedReader br = new BufferedReader(new InputStreamReader(ctx.getInputStream()));
        String line;
        while ((line = br.readLine()) != null) {
            LOG.info("readLine: " + line);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream("value=MODIFIED".getBytes());
        LOG.info("set value=MODIFIED");
        ctx.setInputStream(bais);
        return ctx.proceed();
    }

}

