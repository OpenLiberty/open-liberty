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

package web.metadatacompletetruewebxml.listener;

import java.util.Set;

import javax.servlet.HttpConstraintElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;

/**
 * MetadataCompleteTrueWebXMLListener
 *
 */
public class MetadataCompleteTrueWebXMLListener implements ServletContextListener {

    /**
     * Default constructor.
     */
    public MetadataCompleteTrueWebXMLListener() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @see ServletContextListener#contextDestroyed(ServletContextEvent)
     */
    public void contextDestroyed(ServletContextEvent sce) {}

    /**
     * @see ServletContextListener#contextInitialized(ServletContextEvent)
     */
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext sc = sce.getServletContext();
        Dynamic registration = null;
        Set<String> listOfConflicts = null;

        //Even though metadata-complete=true in web.xml, does not affect dynamic annotations
        //All methods are denied
        try {
            registration = sc.addServlet("MetadataCompleteTrueWebXML3", "web.MetadataCompleteTrueWebXML3");
            registration.addMapping("/MetadataCompleteTrueWebXML3");
            HttpConstraintElement constraint1 = new HttpConstraintElement(EmptyRoleSemantic.DENY);
            ServletSecurityElement servletSecurity1 = new ServletSecurityElement(constraint1);
            listOfConflicts = registration.setServletSecurity(servletSecurity1);
            sc.setAttribute("MetadataCompleteTrueWebXML3", listOfConflicts);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
