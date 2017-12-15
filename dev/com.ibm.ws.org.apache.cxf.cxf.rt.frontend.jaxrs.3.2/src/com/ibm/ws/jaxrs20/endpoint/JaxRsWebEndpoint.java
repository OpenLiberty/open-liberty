/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.endpoint;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.jaxrs20.api.JaxRsProviderFactoryService;

/**
 *
 */
public interface JaxRsWebEndpoint {

    public void init(ServletConfig servletConfig, JaxRsProviderFactoryService jaxRsProviderFactoryService) throws ServletException;

    public void invoke(HttpServletRequest request, HttpServletResponse response) throws ServletException;

    public void destroy();

    public void setEndpointInfoAddress(String add);
}
