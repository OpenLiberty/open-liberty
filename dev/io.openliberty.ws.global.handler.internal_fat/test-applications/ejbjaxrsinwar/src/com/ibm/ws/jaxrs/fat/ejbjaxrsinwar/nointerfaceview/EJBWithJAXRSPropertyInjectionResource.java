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

public class EJBWithJAXRSPropertyInjectionResource extends AbstractEJBWithJAXRSInjectionResource {

    private UriInfo uriInfoField;

    private Request requestField;

    private HttpHeaders headersField;

    private Providers providersField;

    private SecurityContext securityContextField;

    private HttpServletRequest servletRequestField;

    private HttpServletResponse servletResponseField;

    private ServletConfig servletConfigField;

    private ServletContext servletContextField;

    private Application applicationField;

    @Context
    public void setUriInfo(UriInfo field) {
        uriInfoField = field;
    }

    @Context
    public void setRequest(Request field) {
        requestField = field;
    }

    @Context
    public void setHeaders(HttpHeaders field) {
        headersField = field;
    }

    @Context
    public void setProviders(Providers field) {
        providersField = field;
    }

    @Context
    public void setSecurityContext(SecurityContext field) {
        securityContextField = field;
    }

    @Context
    public void setHttpRequest(HttpServletRequest field) {
        servletRequestField = field;
    }

    @Context
    public void setHttpResponse(HttpServletResponse field) {
        servletResponseField = field;
    }

    @Context
    public void setConfig(ServletConfig field) {
        servletConfigField = field;
    }

    @Context
    public void setContext(ServletContext field) {
        servletContextField = field;
    }

    @Context
    public void setApplicationField(Application applicationField) {
        this.applicationField = applicationField;
    }

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
    protected Application getApplication() {
        return applicationField;
    }
}
