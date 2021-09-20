/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package testaddjspfile.listeners;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;

/**
 *
 */
@WebListener
public class AddJspContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {

        ServletContext servletContext = event.getServletContext();
        String pathToApp = servletContext.getRealPath("/");
        System.out.println("Path to webxml.jsp : " + pathToApp);

        ServletRegistration.Dynamic jsp1registration = servletContext.addJspFile("jsp1", "/addJsp/one.jsp");
        jsp1registration.addMapping("/jsp1");

        ServletRegistration.Dynamic jsp2registration = servletContext.addJspFile("jsp2", "WEB-INF/two.jsp");
        jsp2registration.addMapping("/jsp2");

        servletContext.addJspFile("webxmlpartialone", "webxmlpartialone.jsp");

        ServletRegistration.Dynamic jspp2registration = servletContext.addJspFile("webxmlpartialtwo", "webxmlpartialtwo.jsp");
        jspp2registration.addMapping("/webxmlpartialfour");

        try {
            ServletRegistration.Dynamic jspnullregistration = servletContext.addJspFile(null, "/addJsp/one.jsp");
            System.out.println("TEST FAILED : AddJspContextListener registration of a jsp with servletname set to null did not throw an exception");
        } catch (IllegalArgumentException exc) {
            System.out.println("TEST PASSED : AddJspContextListener registration of a jsp with servletname set to null threw an exception : " + exc);

        }

        try {
            ServletRegistration.Dynamic jspemptyregistration = servletContext.addJspFile("", "/addJsp/one.jsp");
            System.out.println("TEST FAILED : AddJspContextListener registration if a jsp with servletname set to an empty string did not throw an exception");
        } catch (IllegalArgumentException exc) {
            System.out.println("TEST PASSED : AddJspContextListener registration of a jsp with servletname set to an empty string threw an exception : " + exc);
        }

    }

}
