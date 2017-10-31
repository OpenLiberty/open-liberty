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
package testservlet40.war.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.PushBuilder;

import javax.servlet.http.HttpSession;

/**
 *
 */
@WebServlet("/PushBuilderAPIServlet")
public class PushBuilderAPIServlet extends HttpServlet {

    /**  */
    private static final long serialVersionUID = 1L;

    public PushBuilderAPIServlet() {

    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        PrintWriter pw = res.getWriter();

        pw.println("PushBuilder API Tests");

        Enumeration<String> reqHeaderNames = req.getHeaderNames();
        while (reqHeaderNames.hasMoreElements()) {
            String name = reqHeaderNames.nextElement();
            pw.println("Req Header : " + name + ":" + req.getHeader(name));

        }

        PushBuilder pb = req.newPushBuilder();

        Set<String> headerNames = pb.getHeaderNames();
        for (String headerName : headerNames) {
            pw.println("PB Header : " + headerName + ":" + pb.getHeader(headerName));
        }

        // Test method
        try {
            pb.method(null);
            pw.println("FAIL : pb.method(null) did not throw an NPE");
        } catch (java.lang.NullPointerException exc) {
            pw.println("PASS : pb.method(null) threw an NPE");
        }

        String[] invalidMethods = { "", "POST", "PUT", "DELETE", "CONNECT", "OPTIONS", "TRACE" };
        for (String invalidMethod : invalidMethods) {
            try {
                pb.method(invalidMethod);
                pw.println("FAIL : pb.method(" + invalidMethod + ") did not throw an IAE");
            } catch (java.lang.IllegalArgumentException exc) {
                pw.println("PASS : pb.method(" + invalidMethod + ") threw an IAE");
            }
        }

        String testMethod = "GET";
        pb.method(testMethod);
        String method = pb.getMethod();
        pw.println((method.equals(testMethod) ? "PASS : method correct." : "FAIL : method incorrect, expected " + testMethod + " but was " + method));

        // Test queryString
        String testQueryString = "test=queryString";
        pb.queryString(testQueryString);
        String queryString = pb.getQueryString();
        pw.println((queryString.equals(testQueryString) ? "PASS : queryString correct." : "FAIL : queryString incorrect, expected " + testQueryString + " but was " + queryString));

        // Test sessionId
        pw.println((pb.getSessionId() == null) ? "PASS : sessionId initialized to null." : "FAIL : sessionId not initialized to null " + pb.getSessionId());
        String testSessionId = "testSessionID";
        pb.sessionId(testSessionId);
        String sessionId = pb.getSessionId();
        pw.println((sessionId.equals(testSessionId) ? "PASS : sessionId correct." : "FAIL : sessionId incorrect, expected " + testSessionId + " but was "
                                                                                    + sessionId));

        HttpSession sess = req.getSession(true);
        PushBuilder pb1 = req.newPushBuilder();
        pw.println(((sess.getId().equals(pb1.getSessionId())) ? "PASS : sessionId initialized correctly : " + sess.getId() : "FAIL : initial sessionId was " + pb1.getSessionId()
                                                                                                                             + " but expected " + sess.getId()));
        // Test path
        String testPath = "/MyPushedResource";
        pb.path(testPath);
        String path = pb.getPath();
        pw.println((path.equals(testPath) ? "PASS : path correct." : "FAIL : path incorrect, expected " + testPath + " but was " + path));

        try {
            pb.method("GET");
            pb.path(null);
            pb.push();
            pw.println("FAIL : pb.push() with a null path did not throw an ISE");
        } catch (java.lang.IllegalStateException exc) {
            pw.println("PASS : pb.push() with a null path threw an ISE");
        }

        pb.path("/testpath");
        pb.queryString("test=queryString");
        try {
            pb.push();
            pw.println("PASS : pb.push() (not implemented yet) did not throw an ISE");
        } catch (java.lang.IllegalStateException exc) {
            pw.println("FAIL : pb.push() (not implemented yet) threw an ISE : " + exc.getMessage());
        }

        pw.println((pb.getPath() == null ? "PASS : path cleared after push." : "FAIL : path not cleared after push, was : " + pb.getPath()));
        pw.println((pb.getQueryString() == null ? "PASS : quuery string cleared after push." : "FAIL : query string not cleared after push, was : " + pb.getQueryString()));

    }

}
