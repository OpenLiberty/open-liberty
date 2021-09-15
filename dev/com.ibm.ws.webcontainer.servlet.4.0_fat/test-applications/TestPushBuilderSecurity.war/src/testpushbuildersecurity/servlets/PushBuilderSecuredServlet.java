/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package testpushbuildersecurity.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.PushBuilder;

/**
 * Test the push method of PushBuilder from a secured servlet.
 *
 * We need to make sure that a secured resource is pushed successfully from a secured servlet.
 */
@WebServlet("/PushBuilderSecuredServlet")
@ServletSecurity(
                 value = @HttpConstraint(
                                         rolesAllowed = {
                                                          "ADMIN_ROLE"
                                         }),
                 httpMethodConstraints = {
                                           @HttpMethodConstraint(value = "GET", rolesAllowed = {
                                                                                                 "ADMIN_ROLE"
                                           })
                 })
public class PushBuilderSecuredServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    public PushBuilderSecuredServlet() {

    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        PrintWriter pw = res.getWriter();

        pw.println("PushBuilder Test");

        PushBuilder pb = req.newPushBuilder();

        if (req.isSecure()) {
            pw.println("Request is secured");
        } else {
            pw.println("Request is not secured");
        }

        if (pb == null) {
            pw.println("req.newPushBuilder() returned null");
        } else {
            pw.println("req.newPushBuilder() returned a non-null value");
            pb.path("images/logo_horizontal_light_navy.png").addHeader("content-type", "image/png").push();

            res.setContentType("text/html;charset=UTF-8");
            pw.write("<html>" +
                     "<br/>" +
                     "<img src='images/logo_horizontal_light_navy.png'>" +
                     "</html>");
        }
    }

}
