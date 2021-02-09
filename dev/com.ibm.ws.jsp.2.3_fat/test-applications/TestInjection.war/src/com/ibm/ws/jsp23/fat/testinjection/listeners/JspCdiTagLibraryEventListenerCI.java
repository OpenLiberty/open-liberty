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
package com.ibm.ws.jsp23.fat.testinjection.listeners;

import javax.inject.Inject;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;

import com.ibm.ws.jsp23.fat.testinjection.beans.TestConstructorInjectionApplicationScoped;
import com.ibm.ws.jsp23.fat.testinjection.beans.TestConstructorInjectionDependentScoped;
import com.ibm.ws.jsp23.fat.testinjection.beans.TestConstructorInjectionRequestScoped;
import com.ibm.ws.jsp23.fat.testinjection.beans.TestConstructorInjectionSessionScoped;

public class JspCdiTagLibraryEventListenerCI implements ServletRequestListener {

    TestConstructorInjectionDependentScoped constructorInjection;
    TestConstructorInjectionRequestScoped constructorInjectionRequest;
    TestConstructorInjectionApplicationScoped constructorInjectionApplication;
    TestConstructorInjectionSessionScoped constructorInjectionSession;
    static public final String ATTRIBUTE_NAME = "JspCdiTagLibraryEventListenerCI";

    private int valueCI, valueCIRequest, valueCIApplication, valueCISession = 0;
    private String response = generateResponse();

    @Inject
    public JspCdiTagLibraryEventListenerCI(TestConstructorInjectionDependentScoped constructorInjection, TestConstructorInjectionRequestScoped constructorInjectionRequest,
                                           TestConstructorInjectionApplicationScoped constructorInjectionApplication,
                                           TestConstructorInjectionSessionScoped constructorInjectionSession) {
        this.constructorInjection = constructorInjection;
        this.constructorInjectionRequest = constructorInjectionRequest;
        this.constructorInjectionApplication = constructorInjectionApplication;
        this.constructorInjectionSession = constructorInjectionSession;
    }

    @Override
    public void requestDestroyed(ServletRequestEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void requestInitialized(ServletRequestEvent arg0) {
        ServletRequest req = arg0.getServletRequest();
        //Checking what jsp page was requested to avoid incrementing the index when it is another page the requested
        if (((HttpServletRequest) req).getRequestURI().toString().equals("/TestInjection/TagLibraryEventListenerCI.jsp"))
            if ("true".equals(req.getParameter("increment"))) {
                valueCI = constructorInjection.incrementAndGetIndex();
                valueCIRequest = constructorInjectionRequest.incrementAndGetIndex();
                valueCIApplication = constructorInjectionApplication.incrementAndGetIndex();
                valueCISession = constructorInjectionSession.incrementAndGetIndex();
                response = generateResponse();
                req.setAttribute(JspCdiTagLibraryEventListenerCI.ATTRIBUTE_NAME, response);
            } else
                req.setAttribute(JspCdiTagLibraryEventListenerCI.ATTRIBUTE_NAME, response);
    }

    private String generateResponse() {
        String response = "<ul>\n";
        response += "<li>TestConstructorInjection index: " + valueCI + "</li>\n";
        response += "<li>TestConstructorInjectionRequest index: " + valueCIRequest + "</li>\n";
        response += "<li>TestConstructorInjectionApplication index: " + valueCIApplication + "</li>\n";
        response += "<li>TestConstructorInjectionSession index: " + valueCISession + "</li>\n";
        response += "</ul>";

        return response;
    }

}
