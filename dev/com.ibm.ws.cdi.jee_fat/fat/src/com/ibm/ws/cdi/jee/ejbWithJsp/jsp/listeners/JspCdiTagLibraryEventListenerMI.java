/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
package com.ibm.ws.cdi.jee.ejbWithJsp.jsp.listeners;

import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

import com.ibm.ws.cdi.jee.ejbWithJsp.jsp.beans.TestMethodInjectionApplicationScoped;
import com.ibm.ws.cdi.jee.ejbWithJsp.jsp.beans.TestMethodInjectionDependentScoped;
import com.ibm.ws.cdi.jee.ejbWithJsp.jsp.beans.TestMethodInjectionRequestScoped;
import com.ibm.ws.cdi.jee.ejbWithJsp.jsp.beans.TestMethodInjectionSessionScoped;

public class JspCdiTagLibraryEventListenerMI implements ServletRequestListener {

    TestMethodInjectionDependentScoped methodInjection;
    TestMethodInjectionRequestScoped methodInjectionRequest;
    TestMethodInjectionApplicationScoped methodInjectionApplication;
    TestMethodInjectionSessionScoped methodInjectionSession;

    static public final String ATTRIBUTE_NAME = "JspCdiTagLibraryEventListenerMI";

    private int valueMI, valueMIRequest, valueMIApplication, valueMISession = 0;
    private String response = generateResponse();

    @Inject
    public void injectCorrespondingObjects(TestMethodInjectionDependentScoped methodInjection, TestMethodInjectionRequestScoped methodInjectionRequest,
                                           TestMethodInjectionApplicationScoped methodInjectionApplication,
                                           TestMethodInjectionSessionScoped methodInjectionSession) {
        this.methodInjection = methodInjection;
        this.methodInjectionRequest = methodInjectionRequest;
        this.methodInjectionApplication = methodInjectionApplication;
        this.methodInjectionSession = methodInjectionSession;
    }

    @Override
    public void requestDestroyed(ServletRequestEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void requestInitialized(ServletRequestEvent arg0) {
        ServletRequest req = arg0.getServletRequest();
        //Checking what jsp page was requested to avoid incrementing the index when it is another page the requested
        if (((HttpServletRequest) req).getRequestURI().toString().equals("/TestJSP2.3/TagLibraryEventListenerMI.jsp"))
            if ("true".equals(req.getParameter("increment"))) {
                valueMI = methodInjection.incrementAndGetIndex();
                valueMIRequest = methodInjectionRequest.incrementAndGetIndex();
                valueMIApplication = methodInjectionApplication.incrementAndGetIndex();
                valueMISession = methodInjectionSession.incrementAndGetIndex();
                response = generateResponse();
                req.setAttribute(JspCdiTagLibraryEventListenerMI.ATTRIBUTE_NAME, response);
            } else
                req.setAttribute(JspCdiTagLibraryEventListenerMI.ATTRIBUTE_NAME, response);
    }

    private String generateResponse() {
        String response = "<ul>\n";
        response += "<li>TestMethodInjection index: " + valueMI + "</li>\n";
        response += "<li>TestMethodInjectionRequest index: " + valueMIRequest + "</li>\n";
        response += "<li>TestMethodInjectionApplication index: " + valueMIApplication + "</li>\n";
        response += "<li>TestMethodInjectionSession index: " + valueMISession + "</li>\n";
        response += "</ul>";

        return response;
    }

}
