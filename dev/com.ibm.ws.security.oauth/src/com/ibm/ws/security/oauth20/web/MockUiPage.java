/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.web;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class MockUiPage {
    HttpServletRequest _req;
    HttpServletResponse _resp;

    public MockUiPage(HttpServletRequest req, HttpServletResponse resp) {
        _req = req;
        _resp = resp;
    }

    @SuppressWarnings("resource")
    public void render() {
        try {
            PrintWriter pw = null;
            try {
                pw = _resp == null ? null : _resp.getWriter();
            } catch (IOException e) {
                return;
            }
            pw.append("<html><body><pre><tt>");
            pw.append("<br>todo: real UI goes here.<br>");
            pw.append("<br>" + "The access token is: " + _req.getAttribute("ui_token"));
            pw.append("<br>" + "The authheader is: " + _req.getAttribute("ui_authheader"));
            pw.append("<br><br>Go forth and make those rest calls.");
            pw.append("<br> + the logout parameter is: " + _req.getParameter("logout"));
            if (_req.getParameter("logout") != null) {
                pw.append("<br> LOGGING OUT");
                _req.logout();
                _req.getSession().invalidate();
            }
            pw.append("</tt></pre></body></html>");
            pw.flush();
        } catch (Exception ex) {

        }
    }
}
