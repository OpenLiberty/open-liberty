/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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

package web.pu;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.crypto.PasswordUtil;

/**
 * Test Servlet to access security public APIs (packages websphere or wsspi)
 * current UserRegistry on each request.
 */
public class PasswordUtilServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    PrintWriter pw = null;

    /**
     * {@inheritDoc} GET handles method requests and calls the requested public API
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();
        String className = req.getParameter("class");
        if (className.equals("PasswordUtil")) {
            testPasswordUtil(req, pw);
        } else {
            pw.println("Usage: PasswordUtilServlet?class=CLASSNAME&method=METHODNAME&options...");
        }
        pw.flush();
        pw.close();
    }

    /**
     * invokes PasswordUtil API, ClassNotFoundException will be thrown if PasswordUtilities-1.0 is not on the feature list.
     *
     * @param req
     * @param pw
     */
    private void testPasswordUtil(HttpServletRequest req, PrintWriter pw) {
        String method = req.getParameter("method");
        String input = req.getParameter("input");
        String algorithm = req.getParameter("algorithm"); // e.g., "aes", "aes-128", "aes-256", "xor"
        String cryptoKey = req.getParameter("cryptoKey"); // custom encryption key for AES_V0 or AES_V1
        String aesKey = req.getParameter("aesKey");       // raw base64 AES-256 key for AES_V2
        String aesVersion = req.getParameter("aesVersion"); // "v0", "v1", "v2" or "AES_V0", "AES_V1", "AES_V2"

        try {
            if ("encode".equalsIgnoreCase(method)) {
                Map<String, String> props = new HashMap<>();
                String targetAlgo = algorithm;

                if (aesVersion != null) {
                    if ("v0".equalsIgnoreCase(aesVersion) || "AES_V0".equalsIgnoreCase(aesVersion)) {
                        targetAlgo = "aes-128";
                        if (cryptoKey != null) {
                            props.put(PasswordUtil.PROPERTY_CRYPTO_KEY, cryptoKey);
                        }
                    } else if ("v1".equalsIgnoreCase(aesVersion) || "AES_V1".equalsIgnoreCase(aesVersion)) {
                        targetAlgo = "aes-256";
                        if (cryptoKey != null) {
                            props.put(PasswordUtil.PROPERTY_CRYPTO_KEY, cryptoKey);
                        }
                    } else if ("v2".equalsIgnoreCase(aesVersion) || "AES_V2".equalsIgnoreCase(aesVersion)) {
                        targetAlgo = "aes";
                        if (aesKey != null) {
                            props.put(PasswordUtil.PROPERTY_AES_KEY, aesKey);
                        }
                    }
                } else {
                    if (cryptoKey != null) {
                        props.put(PasswordUtil.PROPERTY_CRYPTO_KEY, cryptoKey);
                    }
                    if (aesKey != null) {
                        props.put(PasswordUtil.PROPERTY_AES_KEY, aesKey);
                    }
                }

                String output;
                if (targetAlgo == null && props.isEmpty()) {
                    output = PasswordUtil.encode(input);
                } else if (targetAlgo != null && props.isEmpty()) {
                    output = PasswordUtil.encode(input, targetAlgo);
                } else {
                    if (targetAlgo == null) {
                        targetAlgo = "aes";
                    }
                    output = PasswordUtil.encode(input, targetAlgo, props);
                }
                pw.println("encode output is: " + output);
            } else if ("decode".equalsIgnoreCase(method)) {
                // If input had '+' decoded as spaces by URL decoding in query params, restore '+'
                String sanitizedInput = input;
                if (sanitizedInput != null) {
                    if (!sanitizedInput.startsWith("{") && sanitizedInput.startsWith("aes}")) {
                        sanitizedInput = "{" + sanitizedInput;
                    }
                    if (sanitizedInput.startsWith("{aes}") && sanitizedInput.contains(" ")) {
                        String prefix = "{aes}";
                        String body = sanitizedInput.substring(prefix.length()).replace(' ', '+');
                        sanitizedInput = prefix + body;
                    }
                }
                String output = PasswordUtil.decode(sanitizedInput);
                pw.println("decode output is: " + output);
            }
        } catch (Throwable e) {
            if (e instanceof NoClassDefFoundError) {
                pw.println("NoClassDefFoundError: " + e.getMessage());
            } else {
                pw.println("Unexpected Exception during processing: " + e.getMessage());
                e.printStackTrace(pw);
            }
        }
    }

    /**
     * {@inheritDoc} POST does nothing for this servlet.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        PrintWriter pw = resp.getWriter();
        pw.print("use GET method");
    }
}
