/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package remoteApp.basic;

import java.io.IOException;
import java.util.logging.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static Logger LOG = Logger.getLogger(LoggingFilter.class.getName());

    @Override
    public void filter(ContainerRequestContext arg0) throws IOException {
        LOG.info("incoming request " + arg0.getMethod() + " " + arg0.getUriInfo().getPath());
    }

    @Override
    public void filter(ContainerRequestContext arg0, ContainerResponseContext arg1) throws IOException {
        LOG.info("outgoing response " + arg0.getMethod() + " " + arg0.getUriInfo().getPath() + " " + 
                 arg1.getStatus() + " " + arg1.getEntity());
    }
}
