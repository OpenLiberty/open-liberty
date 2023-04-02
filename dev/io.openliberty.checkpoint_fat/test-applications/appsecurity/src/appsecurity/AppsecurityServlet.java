/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package appsecurity;

import java.io.IOException;
import java.io.OutputStream;

import javax.inject.Inject;
import javax.security.enterprise.SecurityContext;
import javax.security.enterprise.authentication.mechanism.http.FormAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = "/home")
@FormAuthenticationMechanismDefinition(
                                       loginToContinue = @LoginToContinue(errorPage = "/error.html",
                                                                          loginPage = "/welcome.html"))
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { "user", "admin" },
                                         transportGuarantee = ServletSecurity.TransportGuarantee.CONFIDENTIAL))
public class AppsecurityServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    public static final String ADMIN = "admin";
    public static final String USER = "user";
    
    @Inject
    private SecurityContext securityContext;
    
    @Inject
    private AppsecurityBean bean;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream outstream = response.getOutputStream();
        if (securityContext.isCallerInRole(ADMIN)) {       
            outstream.println("GROUP: " + ADMIN + ", NAME: " + bean.getUsername() + ", ROLES: " + bean.getRoles());
        } else if (securityContext.isCallerInRole(USER)) {
            outstream.println("GROUP: " + USER + ", NAME: " + bean.getUsername() + ", ROLES: " + bean.getRoles());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}