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

package web.dynamicannotationpure.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.HttpConstraintElement;
import javax.servlet.HttpMethodConstraintElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;

/**
 * ServletRegistrationContextListener2
 *
 */
public class ServletRegistrationContextListenerPure implements ServletContextListener {

    /**
     * Default constructor.
     */
    public ServletRegistrationContextListenerPure() {
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

        //All methods are denied
        //Test with multiple setServletSecurity() methods
        try {
            registration = sc.addServlet("DynamicAnnotationPure1", "web.dynamicannotationpure.DynamicAnnotationPure1");
            registration.addMapping("/DynamicAnnotationPure1");
            //create constraint that will NOT be applied
            HttpConstraintElement constraint1a = new HttpConstraintElement(EmptyRoleSemantic.PERMIT);
            ServletSecurityElement servletSecurity1a = new ServletSecurityElement(constraint1a);
            listOfConflicts = registration.setServletSecurity(servletSecurity1a);
            //create constraint that will be applied since this is the last setSevletSecurity() method called
            HttpConstraintElement constraint1b = new HttpConstraintElement(EmptyRoleSemantic.DENY);
            ServletSecurityElement servletSecurity1b = new ServletSecurityElement(constraint1b);
            listOfConflicts = registration.setServletSecurity(servletSecurity1b);
            sc.setAttribute("DynamicAnnotationPure1", listOfConflicts);
        } catch (Exception e) {
            System.out.println("Got unexpected exception for DynamicAnnotationPure1: " + e.getMessage());
        }

        //All methods are unprotected, but requires SSL
        try {
            registration = sc.addServlet("DynamicAnnotationPure2", "web.dynamicannotationpure.DynamicAnnotationPure2");
            registration.addMapping("/DynamicAnnotationPure2");
            HttpConstraintElement constraint2 = new HttpConstraintElement(TransportGuarantee.CONFIDENTIAL);
            ServletSecurityElement servletSecurity2 = new ServletSecurityElement(constraint2);
            listOfConflicts = registration.setServletSecurity(servletSecurity2);
            sc.setAttribute("DynamicAnnotationPure2", listOfConflicts);
        } catch (Exception e) {
            System.out.println("Got unexpected exception for DynamicAnnotationPure2: " + e.getMessage());
        }

        //All methods (eg. CUSTOM) are denied access, except GET requires Manager (user2), POST requires Employee (user1) and SSL, runas Manager (user99)
        //@RunAs("Manager") predefined in servlet
        try {
            registration = sc.addServlet("DynamicAnnotationPure3", "web.dynamicannotationpure.DynamicAnnotationPure3");
            registration.addMapping("/DynamicAnnotationPure3b");
            registration.setRunAsRole("Manager");
            HttpConstraintElement constraint3 = new HttpConstraintElement(EmptyRoleSemantic.DENY);
            List<HttpMethodConstraintElement> methodConstraints3 = new ArrayList<HttpMethodConstraintElement>();
            methodConstraints3.add(new HttpMethodConstraintElement("GET", new HttpConstraintElement(TransportGuarantee.NONE, new String[] { "Manager" })));
            methodConstraints3.add(new HttpMethodConstraintElement("POST", new HttpConstraintElement(TransportGuarantee.CONFIDENTIAL, new String[] { "Employee" })));
            ServletSecurityElement servletSecurity3 = new ServletSecurityElement(constraint3, methodConstraints3);
            listOfConflicts = registration.setServletSecurity(servletSecurity3);
            sc.setAttribute("DynamicAnnotationPure3", listOfConflicts);
        } catch (Exception e) {
            System.out.println("Got unexpected exception for DynamicAnnotationPure3: " + e.getMessage());
        }

        //All methods (eg. CUSTOM) are unprotected, except GET requires DeclaredManagerDyn (user8), POST is denied
        //@DeclareRoles("DeclaredManagerDyn") predefined in servlet
        try {
            registration = sc.addServlet("DynamicAnnotationPure4", "web.dynamicannotationpure.DynamicAnnotationPure4");
            registration.addMapping("/DynamicAnnotationPure4b");
            HttpConstraintElement constraint4 = new HttpConstraintElement(EmptyRoleSemantic.PERMIT);
            List<HttpMethodConstraintElement> methodConstraints4 = new ArrayList<HttpMethodConstraintElement>();
            methodConstraints4.add(new HttpMethodConstraintElement("GET", new HttpConstraintElement(TransportGuarantee.NONE, new String[] { "DeclaredManagerDyn" })));
            methodConstraints4.add(new HttpMethodConstraintElement("POST", new HttpConstraintElement(EmptyRoleSemantic.DENY)));
            ServletSecurityElement servletSecurity4 = new ServletSecurityElement(constraint4, methodConstraints4);
            listOfConflicts = registration.setServletSecurity(servletSecurity4);
            sc.setAttribute("DynamicAnnotationPure4", listOfConflicts);
        } catch (Exception e) {
            System.out.println("Got unexpected exception for DynamicAnnotationPure4: " + e.getMessage());;
        }

        //All methods (eg. POST) require DeclaredManager (user7), except GET is unprotected, CUSTOM requires Employee (user1). @RunAs set to Manager (user99)
        //Test with multiple URLs
        //RunAs("Manager') predefined in web.xml
        //DeclaredManager predefined in web.xml
        try {
            registration = sc.addServlet("DynamicAnnotationPure5", "web.dynamicannotationpure.DynamicAnnotationPure5");
            registration.addMapping("/dynamicAnnotation/DynamicAnnotationPure5", "/DynamicAnnotationPure5");
            registration.setRunAsRole("Manager");
            HttpConstraintElement constraint5 = new HttpConstraintElement(TransportGuarantee.NONE, new String[] { "DeclaredManager" });
            List<HttpMethodConstraintElement> methodConstraints5 = new ArrayList<HttpMethodConstraintElement>();
            methodConstraints5.add(new HttpMethodConstraintElement("GET", new HttpConstraintElement()));
            methodConstraints5.add(new HttpMethodConstraintElement("CUSTOM", new HttpConstraintElement(TransportGuarantee.NONE, new String[] { "Employee" })));
            ServletSecurityElement servletSecurity5 = new ServletSecurityElement(constraint5, methodConstraints5);
            listOfConflicts = registration.setServletSecurity(servletSecurity5);
            sc.setAttribute("DynamicAnnotationPure5", listOfConflicts);
        } catch (Exception e) {
            System.out.println("Got unexpected exception for DynamicAnnotationPure5: " + e.getMessage());
        }

        //GET allows all roles, POST requires Manager (user2), CUSTOM requires Employee (user1) and SSL, RunAs set to Manager (user99)
        //Test with multiple URLs
        //RunAs("Manager') predefined in web.xml
        //RunAs("Manager") predefined in servlet
        try {
            registration = sc.addServlet("DynamicAnnotationPure6", "web.dynamicannotationpure.DynamicAnnotationPure6");
            //1st URL has conflict, follows web.xml constraints (all methods unprotected)
            registration.addMapping("/DynamicAnnotationPure6");
            //2nd URL is unique and follows constraint defined here
            registration.addMapping("/dynamicAnnotation/DynamicAnnotationPure6");
            registration.setRunAsRole("Manager");
            HttpConstraintElement constraint6 = new HttpConstraintElement();
            List<HttpMethodConstraintElement> methodConstraints6 = new ArrayList<HttpMethodConstraintElement>();
            methodConstraints6.add(new HttpMethodConstraintElement("GET", new HttpConstraintElement(TransportGuarantee.NONE, new String[] { "AllAuthenticated" })));
            methodConstraints6.add(new HttpMethodConstraintElement("POST", new HttpConstraintElement(TransportGuarantee.NONE, new String[] { "Manager" })));
            methodConstraints6.add(new HttpMethodConstraintElement("CUSTOM", new HttpConstraintElement(TransportGuarantee.CONFIDENTIAL, new String[] { "Employee" })));
            ServletSecurityElement servletSecurity6 = new ServletSecurityElement(constraint6, methodConstraints6);
            listOfConflicts = registration.setServletSecurity(servletSecurity6);
            sc.setAttribute("DynamicAnnotationPure6", listOfConflicts);
        } catch (Exception e) {
            System.out.println("Got unexpected exception for DynamicAnnotationPure6: " + e.getMessage());
        }

    }

}
