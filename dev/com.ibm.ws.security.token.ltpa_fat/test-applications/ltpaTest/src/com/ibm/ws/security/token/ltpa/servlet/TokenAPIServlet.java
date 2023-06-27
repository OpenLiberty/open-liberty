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
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.security.token.ValidationResult;

@SuppressWarnings("serial")
public class TokenAPIServlet extends HttpServlet {

    public static final String NULL_TOKEN = "NullToken";
    public static final String INVALID_TOKEN = "InvalidToken";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        Bundle bundle = FrameworkUtil.getBundle(HttpServlet.class);
        BundleContext bundleContext = bundle.getBundleContext();

        try {
            testValidateToken(req, bundleContext, writer);
        } catch (Throwable e) {
            e.printStackTrace(writer);
        }

        writer.flush();
        writer.close();
    }

    private void testValidateToken(HttpServletRequest req, BundleContext ctx, PrintWriter writer) throws Exception {
        ServiceReference<TokenManager> tokenManagerReference = ctx.getServiceReference(TokenManager.class);
        TokenManager tm = ctx.getService(tokenManagerReference);
        try {
            if (tm != null) {

                byte[] tb = null;
                String uniqID = req.getParameter("uniqueID");

                if (uniqID.equals(INVALID_TOKEN)) {
                    tb = INVALID_TOKEN.getBytes();

                } else if (!uniqID.equals(NULL_TOKEN)) {
                    String customRealm = req.getParameter("customRealm");
                    Map<String, Object> tokenData = new HashMap<String, Object>();
                    tokenData.put("unique_id", uniqID);
                    com.ibm.wsspi.security.token.SingleSignonToken t1 = tm.createSSOToken(tokenData);
                    if (customRealm != null) {
                        t1.addAttribute(AttributeNameConstants.WSCREDENTIAL_REALM, customRealm);
                    }
                    tb = t1.getBytes();
                }

                ValidationResult result = com.ibm.wsspi.security.token.WSSecurityPropagationHelper.validateToken(tb);
                printValidationResults(result, writer);
            }
        } catch (Exception e) {
            throw new Exception("Invalid Token: " + e.getMessage());
        }

    }

    private void printValidationResults(ValidationResult result, PrintWriter writer) {
        writer.println("UniqueId Output: " + result.getUniqueId());
        writer.println("User from uniqueId: " + result.getUserFromUniqueId());
        writer.println("Realm from uniqueId: " + result.getRealmFromUniqueId());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();
        pw.print("use GET method");
        resp.setStatus(200);
    }
}
