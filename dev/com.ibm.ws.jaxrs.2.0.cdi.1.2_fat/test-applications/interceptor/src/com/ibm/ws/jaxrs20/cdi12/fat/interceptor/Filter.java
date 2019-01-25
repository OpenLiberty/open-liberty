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
package com.ibm.ws.jaxrs20.cdi12.fat.interceptor;

import java.io.IOException;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

@ApplicationScoped
@Provider
public class Filter implements ContainerRequestFilter, ContainerResponseFilter {

    Logger LOG = Logger.getLogger(Filter.class.getName());

    @Loggable
    @Override
    public void filter(ContainerRequestContext arg0) throws IOException {
        LOG.info("inside request filter method");
    }

    @Loggable
    @Override
    public void filter(ContainerRequestContext arg0, ContainerResponseContext arg1) throws IOException {
        LOG.info("inside response filter method");
    }

}
