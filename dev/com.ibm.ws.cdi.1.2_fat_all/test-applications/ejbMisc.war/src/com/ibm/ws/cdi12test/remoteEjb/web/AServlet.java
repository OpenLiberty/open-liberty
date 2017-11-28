/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.cdi12test.remoteEjb.web;

import java.io.IOException;

import javax.ejb.EJB;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.cdi12test.remoteEjb.api.EJBEvent;
import com.ibm.ws.cdi12test.remoteEjb.api.RemoteInterface;

@WebServlet("/AServlet")
public class AServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @EJB
    RemoteInterface test;

    @Inject
    Event<EJBEvent> anEvent;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        anEvent.fire(new EJBEvent());
        response.getWriter().println("observed=" + test.observed());
    }

}
