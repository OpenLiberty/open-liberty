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

package web.dynamicannotationconflict.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.HttpConstraintElement;
import javax.servlet.HttpMethodConstraintElement;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.ServletSecurityElement;
import javax.servlet.annotation.ServletSecurity.TransportGuarantee;

/**
 * ServletRegistrationContextListenerConflict
 *
 */
public class ServletRegistrationContextListenerConflict implements ServletContextListener {

    /**
     * Default constructor.
     */
    public ServletRegistrationContextListenerConflict() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @see ServletContextListener#contextDestroyed(ServletContextEvent)
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {}

    /**
     * @see ServletContextListener#contextInitialized(ServletContextEvent)
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext sc = sce.getServletContext();
        Dynamic dynamicReg = null;
        ServletRegistration servletReg = null;
        Set<String> listOfConflicts = null;

        //Test with servlet name and URL conflict that's already defined in web.xml
        //Catch expected exceptions, web.xml takes precedent
        try {
            dynamicReg = sc.addServlet("DynamicAnnotationConflict1", "web.dynamicannotationconflict.DynamicAnnotationConflict1");
            if (dynamicReg != null) {
                throw new RuntimeException("Servlet name already exists, but non-null value was returned: " + dynamicReg);
            } else {
                //Expect null
                System.out.println("Got expected null due to web.xml conflict for DynamicAnnotationConflict1");
            }
        } catch (Exception e) {
            System.out.println("Got unexpected exception for DynamicAnnotationConflict1: " + e.getMessage());
        }

        //Test with servlet name and URL conflict that's already defined in static annotation
        //Catch expected exceptions, static annotation takes precedent
        try {
            dynamicReg = sc.addServlet("DynamicAnnotationConflict2", "web.dynamicannotationconflict.DynamicAnnotationConflict2");
            if (dynamicReg != null) {
                throw new RuntimeException("Servlet name already exists, but non-null value was returned: " + dynamicReg);
            } else {
                //Expect null
                System.out.println("Got expected null due to web.xml conflict for DynamicAnnotationConflict2");
            }
        } catch (Exception e) {
            System.out.println("Got unexpected exception for DynamicAnnotationConflict2: " + e.getMessage());
        }

        //Test with RunAs conflict that's already defined in web.xml, and security constraint that should apply to all servlets in the
        //DynamicAnnotationConflict3 class.
        //RunAs web.xml should take precedence, and security constraints for dynamic should take effect
        //  Predefined in web.xml: All methods are unprotected, RunAs Manager (user99)
        try {
            servletReg = sc.getServletRegistration("DynamicAnnotationConflict3");
            //New URL should follow RunAs and security constraints from Web.xml
            servletReg.addMapping("/DynamicAnnotationConflict3New");
            ((ServletRegistration.Dynamic) servletReg).setRunAsRole("Employee");
            HttpConstraintElement constraint3 = new HttpConstraintElement(TransportGuarantee.NONE, new String[] { "Employee" });
            ServletSecurityElement servletSecurity3 = new ServletSecurityElement(constraint3);
            listOfConflicts = ((ServletRegistration.Dynamic) servletReg).setServletSecurity(servletSecurity3);
            sc.setAttribute("DynamicAnnotationConflict3", listOfConflicts);
        } catch (Exception e) {
            System.out.println("Got unexpected exception for DynamicAnnotationConflict3: " + e.getMessage());
        }

        //Test with security constraint conflict that's already defined in static annotation - use getServletRegistration()
        //Security constraint in dynamic injection should take precedent, though RunAs from static annotation takes precedent
        //  Dynamic: All methods require Employee (user1) role, RunAs Employee (user98)
        try {
            servletReg = sc.getServletRegistration("DynamicAnnotationConflict4");
            //New URL would follow security constraints from dynamic
            servletReg.addMapping("/DynamicAnnotationConflict4a");
            HttpConstraintElement constraint4a = new HttpConstraintElement(TransportGuarantee.NONE, new String[] { "Employee" });
            ServletSecurityElement servletSecurity4a = new ServletSecurityElement(constraint4a);
            listOfConflicts = ((ServletRegistration.Dynamic) servletReg).setServletSecurity(servletSecurity4a);
            sc.setAttribute("DynamicAnnotationConflict4", listOfConflicts);
        } catch (Exception e) {
            System.out.println("Got unexpected exception for DynamicAnnotationConflict4: " + e.getMessage());
        }

        //Test with RunAs conflict that's already defined in static annotation - use addServlet()
        //RunAs in dynamic injection should take precedent
        //  Dynamic: All methods require Employee (user1) role, RunAs Manager (user99)
        try {
            dynamicReg = sc.addServlet("DynamicAnnotationConflict4b", "web.dynamicannotationconflict.DynamicAnnotationConflict4");
            //New URL would follow RunAs and security constraints from dynamic
            dynamicReg.addMapping("/DynamicAnnotationConflict4b");
            dynamicReg.setRunAsRole("Manager");
            HttpConstraintElement constraint4 = new HttpConstraintElement(TransportGuarantee.NONE, new String[] { "Employee" });
            ServletSecurityElement servletSecurity4 = new ServletSecurityElement(constraint4);
            listOfConflicts = dynamicReg.setServletSecurity(servletSecurity4);
            sc.setAttribute("DynamicAnnotationConflict4", listOfConflicts);
        } catch (Exception e) {
            System.out.println("Got unexpected exception for DynamicAnnotationConflict4b: " + e.getMessage());
        }

        //Test with RunAs and security constraint conflict that's already defined in web.xml and static annotation
        //RunAs and security constraint in web.xml should take precedent
        //  Predefined in web.xml: All methods are unprotected, RunAs Manager (user99)
        try {
            servletReg = sc.getServletRegistration("DynamicAnnotationConflict5");
            ((ServletRegistration.Dynamic) servletReg).setRunAsRole("Employee");
            HttpConstraintElement constraint5 = new HttpConstraintElement(TransportGuarantee.NONE, new String[] { "Employee" });
            ServletSecurityElement servletSecurity5 = new ServletSecurityElement(constraint5);
            listOfConflicts = ((ServletRegistration.Dynamic) servletReg).setServletSecurity(servletSecurity5);
            sc.setAttribute("DynamicAnnotationConflict5", listOfConflicts);
        } catch (Exception e) {
            System.out.println("Got unexpected exception for DynamicAnnotationConflict5: " + e.getMessage());
        }

        //Test with wildcard so no exact match with web.xml or servlet
        //Web.xml constraint is followed for /DynamicAnnotationConflict6/a
        //Static annotation constraint is followed for /DynamicAnnotationConflict6/b
        //Dynamic annotation constraint is followed for /DynamicAnnotationConflict6/c
        //  All methods (POST, CUSTOM) requires Manager (user2) role, GET requires SSL and Employee (user1) role for Servlet30DynConflict6/c
        try {
            dynamicReg = sc.addServlet("DynamicAnnotationConflict6", "web.dynamicannotationconflict.DynamicAnnotationConflict6");
            dynamicReg.addMapping("/DynamicAnnotationConflict6/*");
            HttpConstraintElement constraint6 = new HttpConstraintElement(TransportGuarantee.NONE, new String[] { "Manager" });
            List<HttpMethodConstraintElement> methodConstraints6 = new ArrayList<HttpMethodConstraintElement>();
            methodConstraints6.add(new HttpMethodConstraintElement("GET", new HttpConstraintElement(TransportGuarantee.CONFIDENTIAL, new String[] { "Employee" })));
            ServletSecurityElement servletSecurity6 = new ServletSecurityElement(constraint6, methodConstraints6);
            listOfConflicts = dynamicReg.setServletSecurity(servletSecurity6);
            sc.setAttribute("DynamicAnnotationConflict6", listOfConflicts);
        } catch (Exception e) {
            System.out.println("Got unexpected exception for DynamicAnnotationConflict6: " + e.getMessage());
        }

        //Test with getServletRegistration() where servlet is defined in web.xml, and constraint is defined in static annotation
        //Constraint in static annotation should take precedent. URL conflict in addMapping will be ignored
        try {
            servletReg = sc.getServletRegistration("DynamicAnnotationConflict7");
            servletReg.addMapping("/DynamicAnnotationConflict7");
        } catch (Exception e) {
            System.out.println("Got unexpected exception for DynamicAnnotationConflict7: " + e.getMessage());
        }

        //Test with getServletRegistration() where servlet and URL are defined in web.xml, and constraint is defined in static annotation
        //Constraint in static annotation should take precedent with existing URL and new URL specified here
        try {
            servletReg = sc.getServletRegistration("DynamicAnnotationConflict8");
            servletReg.addMapping("/DynamicAnnotationConflict8/b");
        } catch (Exception e) {
            System.out.println("Got unexpected exception for DynamicAnnotationConflict8: " + e.getMessage());
        }

        //Test with getServletRegistration() where servlet, URL, and constraint are defined in web.xml, and constraint is defined in static annotation for additional URLs
        //Constraint in web.xml should take precedent with existing URL, while constraint in static annotation should take precedent with new URL
        try {
            servletReg = sc.getServletRegistration("DynamicAnnotationConflict9");
            servletReg.addMapping("/DynamicAnnotationConflict9/b");
        } catch (Exception e) {
            System.out.println("Got unexpected exception for DynamicAnnotationConflict9: " + e.getMessage());
        }

        //Test scenario where we expect exception to make sure we handle exception ok and doesn't affect other servlets
        try {
            servletReg = sc.getServletRegistration("DynamicAnnotationConflict10");
            servletReg.addMapping("/DynamicAnnotationConflict10");
        } catch (Exception e) {
            System.out.println("Got expected exception for DynamicAnnotationConflict10: " + e.getMessage());
        }

    }

}
