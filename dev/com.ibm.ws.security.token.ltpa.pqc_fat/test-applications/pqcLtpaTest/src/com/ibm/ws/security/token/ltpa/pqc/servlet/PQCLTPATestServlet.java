/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

package com.ibm.ws.security.token.ltpa.pqc.servlet;

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

/**
 * Test servlet for PQC LTPA token functionality.
 * Tests token creation with Post-Quantum Cryptography (ML-DSA) signatures.
 */
@SuppressWarnings("serial")
public class PQCLTPATestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        Bundle bundle = FrameworkUtil.getBundle(HttpServlet.class);
        BundleContext bundleContext = bundle.getBundleContext();

        try {
            String testType = request.getParameter("test");
            
            if ("pqc".equals(testType)) {
                testPQCTokenCreation(bundleContext, writer);
            } else if ("hybrid".equals(testType)) {
                testHybridTokenCreation(bundleContext, writer);
            } else {
                testBasicTokenCreation(bundleContext, writer);
            }
            writer.println("Test Passed");
        } catch (Throwable e) {
            writer.println("Test Failed");
            e.printStackTrace(writer);
        }

        writer.flush();
        writer.close();
    }

    /**
     * Get TokenManager from OSGi service registry
     */
    private TokenManager getTokenManager(BundleContext ctx) throws Exception {
        ServiceReference<TokenManager> tokenManagerReference = ctx.getServiceReference(TokenManager.class);
        TokenManager tm = ctx.getService(tokenManagerReference);
        if (tm == null) {
            throw new Exception("TokenManager service is null");
        }
        return tm;
    }

    /**
     * Test basic LTPA token creation (backward compatibility)
     */
    private void testBasicTokenCreation(BundleContext ctx, PrintWriter writer) throws Exception {
        TokenManager tm = getTokenManager(ctx);

        try {
            Map<String, Object> tokenData = new HashMap<String, Object>();
            tokenData.put("unique_id", "testuser");
            tokenData.put("realm", "BasicRealm");
            tm.createToken("Ltpa2", tokenData);
            writer.println("Basic token created: SUCCESS");
        } catch (Exception e) {
            throw new Exception("Error creating basic token: " + e.getMessage(), e);
        }
    }

    /**
     * Test PQC LTPA token creation with ML-DSA signature
     */
    private void testPQCTokenCreation(BundleContext ctx, PrintWriter writer) throws Exception {
        TokenManager tm = getTokenManager(ctx);

        try {
            Map<String, Object> tokenData = new HashMap<String, Object>();
            tokenData.put("unique_id", "pqcuser");
            tokenData.put("realm", "PQCRealm");
            tokenData.put("pqc.enabled", "true");
            tokenData.put("pqc.algorithm", "ML-DSA-65");
            tm.createToken("Ltpa2", tokenData);
            writer.println("PQC token created: SUCCESS");
        } catch (Exception e) {
            throw new Exception("Error creating PQC token: " + e.getMessage(), e);
        }
    }

    /**
     * Test hybrid mode token creation (RSA + ML-DSA)
     */
    private void testHybridTokenCreation(BundleContext ctx, PrintWriter writer) throws Exception {
        TokenManager tm = getTokenManager(ctx);

        try {
            Map<String, Object> tokenData = new HashMap<String, Object>();
            tokenData.put("unique_id", "hybriduser");
            tokenData.put("realm", "HybridRealm");
            tokenData.put("pqc.enabled", "true");
            tokenData.put("pqc.hybrid.mode", "true");
            tokenData.put("pqc.algorithm", "ML-DSA-87");
            tm.createToken("Ltpa2", tokenData);
            writer.println("Hybrid token created: SUCCESS");
        } catch (Exception e) {
            throw new Exception("Error creating hybrid token: " + e.getMessage(), e);
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
