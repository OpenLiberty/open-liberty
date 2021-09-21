/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package testservlet40.servlets;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * ApplicationMBean and checks for the status of the
 * TestBadServletContextListener.war web application
 * which is expected to fail to start due to runtime exception.
 */
@WebServlet("/ApplicationMBeanServlet")
public class ApplicationMBeanServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(ApplicationMBeanServlet.class.getName());

    /**
     * Default constructor.
     */
    public ApplicationMBeanServlet() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String expectedAppName1 = "TestBadServletContextListener";
        String expectedAppName2 = "TestServlet40";
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        String appName = request.getParameter("AppName");
        if (appName == null) {
            response.getWriter().append("FAIL: Check request. appName needs to be passed as query param. URL = " + request.getRequestURL());
            return;
        }
        ObjectName myAppMBean;
        try {
            myAppMBean = new ObjectName("WebSphere:service=com.ibm.websphere.application.ApplicationMBean,name=" + appName);
            LOG.info("AppMBean name " + myAppMBean);

            if (mbs.isRegistered(myAppMBean)) {
                String state = (String) mbs.getAttribute(myAppMBean, "State");
                LOG.info("ApplicationMBeanServlet: The state for the application " + appName + " is " + state);
                if (state == null) {
                    response.getWriter().append("FAIL: state not returned from the app manager. Check server trace for any errors.");
                    return;
                }
                if (appName.equalsIgnoreCase(expectedAppName1)) {
                    if (state.equalsIgnoreCase("INSTALLED")) {
                        response.getWriter().append("PASS: INSTALLED");
                    } else if (state.equalsIgnoreCase("STARTED")) {
                        response.getWriter().append("FAIL: STARTED, make sure stopAppStartUponListenerException is set to true");
                    }
                } else if (appName.equalsIgnoreCase(expectedAppName2)) {
                    if (state.equalsIgnoreCase("STARTED")) {
                        response.getWriter().append("PASS: STARTED");
                    } else if (state.equalsIgnoreCase("INSTALLED")) {
                        response.getWriter().append("FAIL: INSTALLED, this should be STARTED");
                    }
                }
            } else {
                response.getWriter().append("FAIL: ApplicationMBeanServlet: not registered");
                LOG.info("ApplicationMBeanServlet: not registered");
            }
        } catch (MalformedObjectNameException | InstanceNotFoundException | AttributeNotFoundException | ReflectionException
                        | MBeanException e) {
            e.printStackTrace();
            response.getWriter().append("FAIL: exception in logs");
        }
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}
