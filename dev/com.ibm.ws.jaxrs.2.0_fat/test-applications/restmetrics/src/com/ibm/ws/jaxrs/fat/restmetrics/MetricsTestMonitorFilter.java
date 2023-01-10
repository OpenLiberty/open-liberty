/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.restmetrics;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Monitor Class for RESTful Resource Methods.
 */
@Provider
@Priority(1)
public class MetricsTestMonitorFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @PostConstruct
    public void postConstruct() {
    }

    @PreDestroy
    public void preDestroy() {
    }

    /**
     * Method : filter(ContainerRequestContext)
     *
     * @param reqCtx = Container request context
     *
     *            This method will be called whenever a request arrives for a
     *            RESTful resource method.
     *            All it does is save the current time so that the response
     *            time can be calculated later.
     *
     */
    @Override
    public void filter(ContainerRequestContext reqCtx) throws IOException {
        System.out.println("ContainerRequestContext.filter() has been invoked for path: " + reqCtx.getUriInfo().getPath());

        if (reqCtx.getUriInfo().getPath().contains("abortTest")) {
            System.out.println("ContainerRequestContext.filter() aborted.");
            reqCtx.abortWith(Response.ok().build());
        }


    }

    /**
     * Method : filter(ContainerRequestContext, ContainerResponseContext)
     *
     * @param reqCtx = Container request context
     * @param respCtx = Container response context
     *
     *            This method will be called whenever a response arrives from a
     *            RESTful resource method.
     *            It will calculate the cumulative response time for the resource
     *            method and store the result in the REST_Stats MXBean.
     *
     */
     @Override
     public void filter(ContainerRequestContext reqCtx, ContainerResponseContext respCtx) throws IOException {
         System.out.println("ContainerResponseContext.filter() has been invoked for path: " + reqCtx.getUriInfo().getPath());
     }
  }

