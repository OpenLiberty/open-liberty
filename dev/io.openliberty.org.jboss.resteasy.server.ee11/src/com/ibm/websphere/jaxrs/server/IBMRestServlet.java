/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package com.ibm.websphere.jaxrs.server;

import java.io.IOException;

import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;

import com.ibm.websphere.ras.annotation.Trivial;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Trivial
@MultipartConfig
public class IBMRestServlet extends HttpServlet30Dispatcher {
    private static final long serialVersionUID = -7916305366621576524L;

    @Override
    protected void doTrace(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        service("TRACE", request, response);
    }
}
