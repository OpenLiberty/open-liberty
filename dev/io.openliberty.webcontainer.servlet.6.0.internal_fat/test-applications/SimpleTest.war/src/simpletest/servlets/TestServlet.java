/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package simpletest.servlets;

import java.io.IOException;

import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test Servlet that just prints a message in the Response.
 *
 */
@WebServlet("/TestServlet")
public class TestServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = TestServlet.class.getName();

    public TestServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream sos = response.getOutputStream();

        sos.println("Hello from the TestServlet!!");

        displayRequest60APIs(request);

        testSessionAPIs(request, response);
    }

    private void displayRequest60APIs(HttpServletRequest request) {
        StringBuilder sBuilder = new StringBuilder();
        ServletConnection sConn = request.getServletConnection();

        sBuilder.append("Testing 3 ServletRequest APIs \n");
        sBuilder.append(addDivider());
        sBuilder.append("   request.getRequestID() [" + request.getRequestId() + "] \n");
        sBuilder.append("   request.getProtocolRequestId() [" + request.getProtocolRequestId() + "] \n");
        sBuilder.append("   request.getServletConnection() [" + sConn + "] \n");
        sBuilder.append(addDivider());
        LOG(sBuilder.toString());

        sBuilder.setLength(0);
        sBuilder.append("Testing 4 ServletConnection APIs \n");
        sBuilder.append(addDivider());
        sBuilder.append("   ServletConnection getConnectionId() [" + sConn.getConnectionId() + "] \n");
        sBuilder.append("   ServletConnection getProtocol() [" + sConn.getProtocol() + "] \n");
        sBuilder.append("   ServletConnection getProtocalConnectionId() [" + sConn.getProtocolConnectionId() + "] \n");
        sBuilder.append("   ServletConnection isSecure() [" + sConn.isSecure() + "] \n");
        sBuilder.append(addDivider());

        LOG(sBuilder.toString());
    }

    private void testSessionAPIs(HttpServletRequest request, HttpServletResponse response) {
        LOG(" Test Session APIs");
        LOG(addDivider());

        Cookie wcCookie = new Cookie("WebContainerCookieName", "WCCookieValue");
        wcCookie.setPath("/WebContainerPath");
        wcCookie.setDomain("WCDomain");
        wcCookie.setHttpOnly(true);
        wcCookie.setSecure(true);

        response.addCookie(wcCookie);

        Cookie wcCookieAtt = new Cookie("WebContainerCookieViaSetAttribute", "WCCookieValueViaSetAttribute");
        wcCookieAtt.setPath("/Path_viaSetPath");
        wcCookieAtt.setDomain("Domain_viaSetDomain");
        wcCookieAtt.setHttpOnly(true);
        wcCookieAtt.setAttribute("WCATT_myAtt", "myATTValue_viasetAttribute");
        wcCookieAtt.setAttribute("Domain", "Domain_viaSetAttribute");
        wcCookieAtt.setAttribute("Path", "Path_viaSetAttribute");

        wcCookieAtt.setDomain("Domain_viaSetDomain2ndCall");

        wcCookieAtt.setAttribute("SameSite", "SuperLax");

        response.addCookie(wcCookieAtt);

    }

    public static void LOG(String s) {
        System.out.println(CLASS_NAME + s);
    }

    public static String addDivider() {
        return ("=============================\n");
    }
}
