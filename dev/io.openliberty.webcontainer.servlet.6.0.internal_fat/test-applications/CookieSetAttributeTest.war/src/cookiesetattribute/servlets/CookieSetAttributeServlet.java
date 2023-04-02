/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package cookiesetattribute.servlets;

import java.io.IOException;
import java.util.logging.Logger;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Test Cookie.setAttribute()
 * Test predefined setter
 * Test to make sure setAttribute() can override predefined setter
 * Test addHeader and setHeader to Set-Cookie
 *
 * SameSite=Lax is added from the server config
 *
 * /CookieSetAttributeServlet?testName=override|addAndSetHeaders|setAttributeSameSite|basic
 */
@WebServlet("/CookieSetAttributeServlet")
public class CookieSetAttributeServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = CookieSetAttributeServlet.class.getName();
    private static final Logger LOG = Logger.getLogger(CLASS_NAME);

    public CookieSetAttributeServlet() {
        super();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ServletOutputStream sos = response.getOutputStream();
        String testName = request.getParameter("testName");

        if (testName == null || testName.isEmpty()) {
            testName = "basic";
        }

        // Override the default setPath, setDomain with the setAttribute("Path") setAttribute("Domain")
        if (testName.equals("override")) {
            testOverride(response);
        }
        // addHeader, setHeader
        else if (testName.equals("addAndSetHeaders")) {
            testAddSetHeader(response);
        }
        //setAttribute for SameSite
        else if (testName.equals("setAttributeSameSite")) {
            testSetAttributeSameSite(response);
        }
        // Test all default cookie setter
        else {
            testBasic(response);
        }

        sos.print("Hello from the CookieSetAttributeServlet. Main response data are in the Set-Cookie headers");
    }

    /*
     * Generates header
     * Set-Cookie: CookieSetAttributeServlet=TestCookieBasic; Path=BasicPath; Domain=basicdomain; Secure; HttpOnly; SameSite=Lax; basicsetattribute1=BasicAttributeValue1;
     * basicsetattribute2=BasicAttributeValue2
     */
    private void testBasic(HttpServletResponse response) {
        LOG.info(addDivider());
        LOG.info(" testBasic : Cookie predefined settter and setAttributes");

        Cookie wcCookieAtt = new Cookie("CookieSetAttributeServlet", "TestCookieBasic");
        wcCookieAtt.setPath("BasicPath");
        wcCookieAtt.setDomain("BasicDomain");
        wcCookieAtt.setHttpOnly(true);
        wcCookieAtt.setAttribute("BasicSetAttribute1", "BasicAttributeValue1");
        wcCookieAtt.setAttribute("BasicSetAttribute2", "BasicAttributeValue2");
        wcCookieAtt.setSecure(true);

        response.addCookie(wcCookieAtt);

        LOG.info(" testBasic END.");
        LOG.info(addDivider());
    }

    /*
     * Create cookie with default setPath, setDomain; then use setAttribute to override Path and Domain
     * setAttribute then replaced its value
     *
     * Generates header:
     * Set-Cookie: CookieSetAttributeServlet=TestCookieOverride; Path=Path_viaSetAttribute; Domain=Domain_viaSetAttribute; HttpOnly; SameSite=Lax;
     * basicsetattribute1=BasicAttributeValue1;
     * basicsetattribute2=BasicAttributeValue2; basicsetattribute3=BasicAttributeValueREPLACED
     */
    private void testOverride(HttpServletResponse response) {
        LOG.info(addDivider());
        LOG.info(" testOverride: override default setPath, setDomain with setAttribute");

        Cookie wcCookieAtt = new Cookie("CookieSetAttributeServlet", "TestCookieOverride");
        wcCookieAtt.setPath("BasicPath");
        wcCookieAtt.setDomain("BasicDomain");
        wcCookieAtt.setHttpOnly(true);
        wcCookieAtt.setAttribute("BasicSetAttribute1", "BasicAttributeValue1");
        wcCookieAtt.setAttribute("BasicSetAttribute2", "BasicAttributeValue2");
        wcCookieAtt.setAttribute("BasicSetAttribute3", "BasicAttributeValueORIGINAL");

        //override Path and Domain
        wcCookieAtt.setAttribute("Domain", "Domain_viaSetAttribute");
        wcCookieAtt.setAttribute("Path", "Path_viaSetAttribute");

        //override the previous setAttribute BasicSetAttribute3 with new value
        wcCookieAtt.setAttribute("BasicSetAttribute3", "BasicAttributeValueREPLACED");

        response.addCookie(wcCookieAtt);

        LOG.info(" testOverride END.");
        LOG.info(addDivider());
    }

    /*
     * Generates headers:
     * Set-Cookie: Cookie_viaSetHeader=CookieValue_viaSetHeader; Secure; SameSite=None
     * Set-Cookie: randomAttributeA=myAttValueA; SameSite=Lax
     * Set-Cookie: Cookie_viaAddHeader=CookieValue_viaAddHeader; Secure; SameSite=None
     * Set-Cookie: randomAttributeB=myAttValueB; SameSite=Lax
     */
    private void testAddSetHeader(HttpServletResponse response) {
        LOG.info(addDivider());
        LOG.info(" testAddSetHeader");

        Cookie wcCookieAtt = new Cookie("CookieSetAttributeServlet", "TestAddSetHeader");
        wcCookieAtt.setAttribute("SetAttributeName", "SetAttributeValue");
        response.addCookie(wcCookieAtt);

        response.setHeader("Set-Cookie", "Cookie_viaSetHeader=CookieValue_viaSetHeader; Secure; SameSite=None; randomAttributeA=myAttValueA");
        response.addHeader("Set-Cookie", "Cookie_viaAddHeader=CookieValue_viaAddHeader; Secure; SameSite=None; randomAttributeB=myAttValueB");

        LOG.info(" testAddSetHeader END.");
        LOG.info(addDivider());
    }

    /*
     * Test cookie setAttribute("SameSite","Strict") which override the server setting for samesite *="Lax"
     * Generates header
     * Set-Cookie: CookieSetAttributeServlet=TestSetAttributeSameSite; HttpOnly; SameSite=Strict
     */
    private void testSetAttributeSameSite(HttpServletResponse response) {
        LOG.info(addDivider());
        LOG.info(" testSetAttributeSameSite : Cookie setAttributes SameSite");

        Cookie wcCookieAtt = new Cookie("CookieSetAttributeServlet", "TestSetAttributeSameSite");
        wcCookieAtt.setHttpOnly(true);
        wcCookieAtt.setAttribute("SAMESITE", "Strict");

        response.addCookie(wcCookieAtt);

        LOG.info(" testSetAttributeSameSite END.");
        LOG.info(addDivider());
    }

    private String addDivider() {
        return ("=============================\n");
    }
}
