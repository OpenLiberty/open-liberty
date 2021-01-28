/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.ejbjaxrsinwar.nointerfaceview;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Providers;

public class EJBWithJAXRSFieldInjectionResource extends AbstractEJBWithJAXRSInjectionResource {

    @Context
    private UriInfo uriInfoField;

    @Context
    private Request requestField;

    @Context
    private HttpHeaders headersField;

    @Context
    private Providers providersField;

    @Context
    private SecurityContext securityContextField;

    @Context
    private HttpServletRequest servletRequestField;

    @Context
    private HttpServletResponse servletResponseField;

    @Context
    private ServletConfig servletConfigField;

    @Context
    private ServletContext servletContextField;

    @Context
    private Application applicationField;

    @Override
    protected HttpHeaders getHttpHeaders() {
        return headersField;
    }

    @Override
    protected Providers getProviders() {
        return providersField;
    }

    @Override
    protected Request getRequest() {
        return requestField;
    }

    @Override
    protected SecurityContext getSecurityContext() {
        return securityContextField;
    }

    @Override
    protected ServletConfig getServletConfig() {
        return servletConfigField;
    }

    @Override
    protected ServletContext getServletContext() {
        return servletContextField;
    }

    @Override
    protected HttpServletRequest getServletRequest() {
        return servletRequestField;
    }

    @Override
    protected HttpServletResponse getServletResponse() {
        return servletResponseField;
    }

    @Override
    protected UriInfo getUriInfo() {
        return uriInfoField;
    }

    @Override
    public Application getApplication() {
        return applicationField;
    }

}
