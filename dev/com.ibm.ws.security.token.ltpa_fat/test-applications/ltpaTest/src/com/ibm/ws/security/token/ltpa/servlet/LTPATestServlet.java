/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

package com.ibm.ws.security.token.ltpa.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.security.token.TokenManager;

@SuppressWarnings("serial")
public class LTPATestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        Bundle bundle = FrameworkUtil.getBundle(HttpServlet.class);
        BundleContext bundleContext = bundle.getBundleContext();

        try {
            testGetTokenManager(bundleContext);
            writer.println("Test Passed");
        } catch (Throwable e) {
            e.printStackTrace(writer);
        }

        writer.flush();
        writer.close();
    }

    private void testGetTokenManager(BundleContext ctx) throws Exception {
        ServiceReference<TokenManager> tokenManagerReference = ctx.getServiceReference(TokenManager.class);
        TokenManager tm = ctx.getService(tokenManagerReference);

        try {
            if (tm != null) {
                Map<String, Object> tokenData = new HashMap<String, Object>();
                tokenData.put("unique_id", "foo");
                tm.createToken("Ltpa2", tokenData);
            }
        } catch (Exception e) {
            throw new Exception("Error creating the token: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
                    throws ServletException, IOException {
        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();
        pw.print("use GET method");
        resp.setStatus(200);
    }
}
