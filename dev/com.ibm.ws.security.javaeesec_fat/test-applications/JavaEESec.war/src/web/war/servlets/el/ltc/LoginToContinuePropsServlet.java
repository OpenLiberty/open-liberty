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
package web.war.servlets.el.ltc;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.HttpMethodConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.annotation.ServletSecurity.EmptyRoleSemantic;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

@WebServlet("/LoginToContinueProps")
public class LoginToContinuePropsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    public LoginToContinuePropsServlet() {
        super();
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        handleRequest("GET", req, res);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        handleRequest("POST", req, res);
    }

    public void handleRequest(String type, HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String errorPage = req.getParameter("errorPage");
        String loginPage = req.getParameter("loginPage");
        String _useForwardToLogin = req.getParameter("useForwardToLogin");
        Boolean useForwardToLogin = Boolean.TRUE;
        if (_useForwardToLogin != null && _useForwardToLogin.equalsIgnoreCase("false")) {
            useForwardToLogin = Boolean.FALSE;
        }
        LoginToContinuePropsBean bean = new LoginToContinuePropsBean();
        bean.setErrorPage(errorPage);
        bean.setLoginPage(loginPage);
        bean.setUseForwardToLogin(useForwardToLogin);

        PrintWriter writer = res.getWriter();
        StringBuffer sb = new StringBuffer();
        sb.append("ServletName: LoginToContinueTest, Request type : ").append(type);
        sb.append(", errorPage : ").append(errorPage).append(", loginPage : ").append(loginPage).append(", userForwardToLogin : ").append(useForwardToLogin);
        writer.write(sb.toString());
    }
}
