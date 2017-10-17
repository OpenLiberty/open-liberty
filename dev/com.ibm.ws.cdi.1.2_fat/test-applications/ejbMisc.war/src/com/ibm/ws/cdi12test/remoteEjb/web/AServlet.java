/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */

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
