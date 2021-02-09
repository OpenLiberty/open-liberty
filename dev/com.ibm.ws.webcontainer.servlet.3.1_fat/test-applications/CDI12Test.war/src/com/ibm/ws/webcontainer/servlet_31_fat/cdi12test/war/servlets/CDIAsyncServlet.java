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
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncListener;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.webcontainer.servlet_31_fat.cdi12test.war.listeners.CDIAsyncListener;

@WebServlet(urlPatterns = "/CDIAsyncServlet", asyncSupported = true)
public class CDIAsyncServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(CDIAsyncServlet.class.getName());
    private static final String DISP1 = "doDispatch1";
    private static final String DISP2 = "doDispatch2";

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        String dispatch = (String) (req.getAttribute("dispatch"));
        LOG.info("AsyncForwardServlet : Entering. dispatch = " + dispatch);

        PrintWriter pw = res.getWriter();
        if (dispatch == null || dispatch.isEmpty()) {

            LOG.info("CDIAsyncServlet : About to forward the request.");
            pw.println("CDIAsyncServlet : About to forward the request.");
            req.setAttribute("dispatch", DISP1);
            RequestDispatcher disp = req.getRequestDispatcher("/CDIAsyncServlet");
            disp.forward(req, res);

        } else if (dispatch.equals(DISP1)) {
            // start async
            LOG.info("CDIAsyncServlet : About to start async do dispacth");
            pw.println("CDIAsyncServlet : About to start async do dispacth");
            AsyncContext ac = req.startAsync(req, res);

            AsyncListener acl = ac.createListener(CDIAsyncListener.class);
            ac.addListener(acl, req, res);
            req.setAttribute("dispatch", DISP2);
            ac.dispatch();
        } else if (dispatch.equals(DISP2)) {
            LOG.info("CDIAsyncServlet : in disptch about to start async and complete");
            pw.println("CDIAsyncServlet : in disptch about to start async and complete");
            AsyncContext ac = req.startAsync(req, res);
            ac.complete();
        }

    }

}
