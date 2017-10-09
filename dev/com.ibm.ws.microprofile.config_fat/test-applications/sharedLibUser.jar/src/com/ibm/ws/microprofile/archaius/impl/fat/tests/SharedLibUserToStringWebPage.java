/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.archaius.impl.fat.tests;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

//defaultsGetConfigPathSharedLib
@WebServlet("/")
public class SharedLibUserToStringWebPage extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = SharedLibUserToStringWebPage.class.getName();

    public SharedLibUserToStringWebPage() {}

    protected void log(String method, String msg, Throwable thrown) {
        System.out.println(CLASS_NAME + "." + method + "(): " + msg);
        if (thrown != null) {
            thrown.printStackTrace(System.out);
        }
    }

    protected void log(String method, String msg) {
        this.log(method, msg, null);
    }

    @Override
    public void init() throws ServletException {
        // Servlet initialization code here
        super.init();
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("text/plain");
        PrintWriter pw = response.getWriter();

        try {
            String result = runTest(request);
            pw.println(result);
        } catch (Throwable t) {
            pw.println(t.getMessage());
        }
    }

    /**
     * @param request
     * @return
     */
    private String runTest(HttpServletRequest request) {

        String msg = PingableSharedLibClass.ping();
        Config c = ConfigProvider.getConfig();
        String v = c.getValue("defaultSources.sharedLib.config.properties", String.class);
        if ("sharedLibPropertiesDefaultValue".equals(v) && msg.contains("loadable")) {
            return "PASSED " + v;
        } else {
            return "FAILED" + v;
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}

    @Override
    public void destroy() {
        super.destroy();
    }
}
