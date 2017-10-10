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

package web.formloginpagesessionfalse;

import java.io.IOException;
import java.io.PrintWriter;

import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.cred.WSCredential;

@SuppressWarnings("serial")
/**
 * Form Logout Servlet
 */
public class FormLogoutPageSessionFalseServlet extends HttpServlet {

    @Override
    public void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if ("CUSTOM".equalsIgnoreCase(req.getMethod()))
            doCustom(req, res);
        else
            super.service(req, res);
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        writer.println("FormLoginPageSessionFalseServlet");
        performTask(request, response, writer);
        writer.flush();
        writer.close();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        writer.println("FormLoginPageSessionFalseServlet");
        performTask(request, response, writer);
        writer.flush();
        writer.close();
    }

    public void doCustom(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        writer.println("FormLoginPageSessionFalseServlet");
        performTask(request, response, writer);
        writer.flush();
        writer.close();
    }

    public void performTask(javax.servlet.http.HttpServletRequest request,
                            javax.servlet.http.HttpServletResponse resp, PrintWriter writer) {

        // values for test to check before logged out
        writer.println("Values before logout method:");

        // Todo: Add checks in FAT tests for these return values
        writer.println("Before logout getAuthType: " + request.getAuthType());
        writer.println("Before logout getRemoteUser: "
                       + request.getRemoteUser());
        writer.println("Before logout getUserPrincipal: "
                       + request.getUserPrincipal());
        if (request.getUserPrincipal() != null) {
            writer.println("getUserPrincipal().getName(): "
                           + request.getUserPrincipal().getName());
        }
        writer.println("Before logout isUserInRole(Employee): "
                       + request.isUserInRole("Employee"));
        writer.println("Before logout isUserInRole(Manager): "
                       + request.isUserInRole("Manager"));
        String role = request.getParameter("role");
        if (role == null) {
            writer.println("You can customize the isUserInRole call with the follow paramter: ?role=name");
        }
        writer.println("isUserInRole(" + role + "): "
                       + request.isUserInRole(role));

        // TODO DELETE THIS
        try {
            // Get the CallerSubject
            Subject callerSubject = WSSubject.getCallerSubject();
            writer.println("Before logout callerSubject: " + callerSubject);

            // Get the public credential from the CallerSubject
            WSCredential callerCredential = null;
            if (callerSubject != null) {
                callerCredential = (WSCredential) callerSubject.getPublicCredentials().iterator().next();
                if (callerCredential != null) {
                    writer.println("Before logout Caller Subject Public Credential: "
                                   + callerCredential);
                } else {
                    writer.println("Before logout Caller Subject Public Credential is Null!");
                }
            } else {
                writer.println("Before logout Caller Subject is Null!");
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }

        // log out
        try {
            writer.println("End values before logout method");
            writer.println("Logout" + "</BR>");
            request.logout();
        } catch (Exception e) {
            writer.println("Error message " + e.getMessage() + "</BR>");
        }

        // debug
        writer.println("Values after logout method:");
        writer.println("After logout getAuthType: " + request.getAuthType());
        writer.println("After logout getRemoteUser: "
                       + request.getRemoteUser());
        writer.println("After logout getUserPrincipal: "
                       + request.getUserPrincipal());
        // writer.println("getUserPrincipal().getName(): " +
        // request.getUserPrincipal().getName());
        writer.println("After logout isUserInRole(Employee): "
                       + request.isUserInRole("Employee"));
        writer.println("After logout isUserInRole(Manager): "
                       + request.isUserInRole("Manager"));

        try {
            // Get the CallerSubject
            Subject callerSubject = WSSubject.getCallerSubject();
            writer.println("After logout callerSubject: " + callerSubject);

            // Get the public credential from the CallerSubject
            WSCredential callerCredential = null;
            if (callerSubject != null) {
                callerCredential = (WSCredential) callerSubject.getPublicCredentials().iterator().next();
                if (callerCredential != null) {
                    writer.println("After logout" + callerCredential);
                } else {
                    writer.println("After logout Caller Subject Public Credential is Null!");
                }
            } else {
                writer.println("After logout Caller Subject is Null.");
            }
            writer.println("End values after logout method");

        } catch (NoClassDefFoundError ne) {
            // For OSGI App testing (EBA file), we expect this exception for all packages that are not public
            writer.println("NoClassDefFoundError for SubjectManager: " + ne);
            writer.println("End values after logout method");
        } catch (Throwable t) {
            writer.println("End values after logout method");
            t.printStackTrace();
        }

    }

}
