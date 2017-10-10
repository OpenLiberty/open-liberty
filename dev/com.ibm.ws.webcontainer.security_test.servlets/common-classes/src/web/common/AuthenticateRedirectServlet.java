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

@SuppressWarnings("serial")
/**
 * Authenticate and Redirect Servlet
 */
public class AuthenticateRedirectServlet extends BaseServlet {

    public AuthenticateRedirectServlet() {
        super("AuthenticateRedirectServlet");
    }

    @Override
    protected void performTask(HttpServletRequest request,
                               HttpServletResponse resp, StringBuffer sb) {
        // Values before any method call
        writeLine(sb, "Start initial values");
        printProgrammaticApiValues(request, sb);
        writeLine(sb, "End initial values");

        // Get parameters from URL link
        String loginUser = request.getParameter("user");
        String loginPassword = request.getParameter("password");
        String testMethod = request.getParameter("testMethod");
        String redirectMethod = request.getParameter("redirectMethod");
        String redirectPage = request.getParameter("redirectPage");
        if (loginUser == null || loginPassword == null || testMethod == null
            || redirectMethod == null || redirectPage == null) {
            writeLine(sb, "Usage: ?testMethod=<method>&redirMethod=<method>&redirURL=<page>&user=<user>&password=<password>");
        }
        writeLine(sb, "Passed in from URL: testMethod:[" + testMethod
                      + "] redirectMethod:[" + redirectMethod + "] redirectPage: ["
                      + redirectPage + "] user:[" + loginUser + "] password:["
                      + loginPassword + "]");

        // Make method calls
        if (testMethod != null) {
            String[] method = testMethod.split(",");
            for (int i = 0; i < method.length; i++) {
                writeLine(sb, "STARTTEST" + (i + 1));
                writeLine(sb, "method: " + method[i]);
                try {
                    callMethod(request, resp, sb, method[i]);
                } catch (ServletException e1) {
                    writeLine(sb, "ServletException: " + e1.getMessage());
                } catch (Exception e) {
                    writeLine(sb, "Unexpected exception: " + e.toString());
                }
                writeLine(sb, "ENDTEST" + (i + 1));
            }
        }

        // Redirect
        if (redirectPage != null) {
            writeLine(sb, "START_REDIRECT");
            try {
                String url = request.getContextPath() + "/" + redirectPage
                             + "?testMethod=" + redirectMethod + "&user="
                             + loginUser + "&password=" + loginPassword;
                writeLine(sb, "Redirect to: " + url);
                resp.sendRedirect(url);
            } catch (Exception e) {
                writeLine(sb, "Unexpected exception during redirect: "
                              + e.toString());
            }
            writeLine(sb, "END_REDIRECT");
        }
    }

    /**
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    private void callMethod(HttpServletRequest req, HttpServletResponse resp,
                            StringBuffer sb, String testMethod) throws ServletException, IOException {
        if (testMethod.contains("login")) {
            String user = req.getParameter("user");
            String password = req.getParameter("password");
            req.login(user, password);
        } else if (testMethod.contains("logout")) {
            req.logout();
        } else if (testMethod.contains("authenticate")) {
            boolean result = req.authenticate(resp);
            writeLine(sb, "Authenticate result: " + result);
        }
        printProgrammaticApiValues(req, sb);
    }

}
