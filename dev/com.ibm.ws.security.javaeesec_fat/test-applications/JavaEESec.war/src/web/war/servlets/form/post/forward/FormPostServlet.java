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
package web.war.servlets.form.post.redirect;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.security.enterprise.authentication.mechanism.http.FormAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;

@FormAuthenticationMechanismDefinition(loginToContinue = @LoginToContinue(errorPage="/loginError.jsp", loginPage="/login.jsp", useForwardToLogin=false, useForwardToLoginExpression=""))
@WebServlet("/FormPostServlet")
@ServletSecurity(httpMethodConstraints = {@HttpMethodConstraint(value = "POST", rolesAllowed = "grantedgroup")})
public class FormPostServlet extends HttpServlet {

    public void init(ServletConfig config) throws ServletException
    {
       super.init(config);
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        PrintWriter out = res.getWriter();
        out.println("ServletName: FormPostServlet : GET method is called.");
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        processRequest( req, res );
    }

    void processRequest(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String op = req.getParameter("operation");
        PrintWriter out = res.getWriter();
        out.println("ServletName: FormPostServlet");
        if (op != null && op.equals("Add")) {
            out.println("RemoteUser : " + req.getRemoteUser() + ", firstName : " + req.getParameter("firstName") + ", lastName : "  + req.getParameter("lastName")  + ", eMailAddr : " + req.getParameter("eMailAddr") + ", phoneNum : "  + req.getParameter("phoneNum"));
        } else {
            // do nothing.
            out.println("RemoteUser : " + req.getRemoteUser() + ", opeation : " + op); 
        }
    }
}

