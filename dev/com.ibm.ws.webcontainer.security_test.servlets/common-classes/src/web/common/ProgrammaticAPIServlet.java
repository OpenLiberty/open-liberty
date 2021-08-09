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

package web.common;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Form Logout Servlet
 */
public class ProgrammaticAPIServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    private static boolean loggedOut = false;

    public ProgrammaticAPIServlet() {
        super("ProgrammaticAPIServlet");
    }

    @Override
    protected void performTask(HttpServletRequest req,
                               HttpServletResponse resp, StringBuffer sb) throws ServletException, IOException {
        // Values before any method call
        writeLine(sb, "Start initial values");
        printProgrammaticApiValues(req, sb);
        writeLine(sb, "End initial values");

        // Get parameters from URL link
        String loginUser = req.getParameter("user");
        String loginPassword = req.getParameter("password");
        String testMethod = req.getParameter("testMethod");
        if (loginUser == null || loginPassword == null || testMethod == null) {
            writeLine(sb, "Usage: ?testMethod=<method>&user=<user>&password=<password>");
        }
        writeLine(sb, "Passed in from URL: testMethod:[" + testMethod
                      + "] user:[" + loginUser + "] password:["
                      + loginPassword + "]");
        //System.out.println("performTask Passed in from URL: testMethod:[" + testMethod
        //                   + "] user:[" + loginUser + "]");
        if (testMethod != null) {
            String[] method = testMethod.split(",");
            for (int i = 0; i < method.length; i++) {
                writeLine(sb, "STARTTEST" + (i + 1));
                writeLine(sb, "method: " + method[i]);
                try {
                    callMethod(req, resp, sb, method[i]);
                } catch (ServletException e) {
                    writeLine(sb, "ServletException: " + e.getMessage());
                }
                writeLine(sb, "ENDTEST" + (i + 1));
            }
        }
    }

    private void callMethod(HttpServletRequest req, HttpServletResponse resp,
                            StringBuffer sb, String testMethod) throws ServletException, IOException {
        if (testMethod.contains("login")) {
            String user = req.getParameter("user");
            String password = req.getParameter("password");
            //System.out.println("callMethod calling req.login");
            req.login(user, password);
        } else if (testMethod.contains("logout_once")) {
            if (loggedOut == false) {
                //System.out.println("callMethod logout_once calling req.logout");
                req.logout();
                loggedOut = true;
            } else {
                //System.out.println("callMethod logout_once clearing loggedOut flag");
                loggedOut = false;
            }
        } else if (testMethod.contains("logout")) {
            //System.out.println("callMethod calling req.logout");
            req.logout();
        } else if (testMethod.contains("authenticate")) {
            //System.out.println("callMethod calling req.authenticate");
            boolean result = req.authenticate(resp);
            writeLine(sb, "Authenticate result: " + result);
        }

        printProgrammaticApiValues(req, sb);
    }

}
