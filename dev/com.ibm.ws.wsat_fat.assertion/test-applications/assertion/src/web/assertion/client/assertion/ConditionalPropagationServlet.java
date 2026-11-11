/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package web.assertion.client.assertion;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;
import javax.xml.ws.BindingProvider;

import com.ibm.tx.jta.ut.util.TxTestUtils;

import web.assertion.server.TxProbe;

/**
 * Drives the WS-AT conditional propagation tests. Always writes a single line:
 * <ul>
 * <li>{@code OK:<reply>} - the call returned; {@code <reply>} ends in {@code tx=ACTIVE} if the
 * endpoint saw a global transaction (WS-AT context propagated) or {@code tx=NONE} if it did not.</li>
 * <li>{@code EXCEPTION:<class>:<message>} - the call (or begin/complete) threw.</li>
 * <li>{@code COUNT:<n>} / {@code INIT:done} for the {@code count} and {@code init} operations.</li>
 * </ul>
 *
 * <p>Parameters:
 * <ul>
 * <li>{@code op} = invoke | count | init (default invoke)</li>
 * <li>{@code service} = Required | Optional | None</li>
 * <li>{@code tran} = GlobalCommit | GlobalRollback | NoTran</li>
 * <li>{@code key} = unique key. The endpoint inserts a row with this key; {@code op=count} reports how many exist.</li>
 * <li>{@code baseurl}</li>
 * </ul>
 */
@WebServlet({ "/ConditionalPropagationServlet" })
public class ConditionalPropagationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Resource
    UserTransaction userTransaction;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String op = param(request, "op", "invoke");
        String result;
        try {
            switch (op) {
                case "init":
                    TxProbe.init();
                    result = "INIT:done";
                    break;
                case "count":
                    result = "COUNT:" + TxProbe.count(request.getParameter("key"));
                    break;
                default:
                    result = invoke(request.getParameter("service"), request.getParameter("tran"),
                                    request.getParameter("key"), param(request, "baseurl", "http://localhost:8010"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            result = "EXCEPTION:" + e.getClass().getSimpleName() + ":" + e.getMessage();
        }
        System.out.println("ConditionalPropagationServlet " + request.getQueryString() + " -> " + result);
        response.getWriter().println(result.replace('\n', ' '));
    }

    private String invoke(String service, String tran, String key, String baseURL) {
        final boolean global = "GlobalCommit".equals(tran) || "GlobalRollback".equals(tran);
        boolean begun = false;
        try {
            if (global) {
                userTransaction.begin();
                begun = true;
            }

            final String reply;
            try {
                reply = proxy(service, baseURL).sayHelloToOther(key);
            } catch (RuntimeException e) {
                if (begun) {
                    rollbackQuietly();
                }
                throw e;
            }

            if (global) {
                if ("GlobalCommit".equals(tran)) {
                    userTransaction.commit();
                } else {
                    userTransaction.rollback();
                }
            }
            return "OK:" + reply;
        } catch (Exception e) {
            e.printStackTrace();
            return "EXCEPTION:" + e.getClass().getSimpleName() + ":" + e.getMessage();
        }
    }

    private void rollbackQuietly() {
        try {
            userTransaction.rollback();
        } catch (Exception e) {
            System.out.println("rollback after failure: " + e);
        }
    }

    private Hello proxy(String service, String baseURL) throws Exception {
        final Hello proxy;
        final String name;
        switch (service) {
            case "Required": {
                name = "HelloImplAssertionRequiredService";
                proxy = new HelloImplAssertionRequiredService(new URL(baseURL + "/assertion/" + name + "?wsdl")).getHelloImplAssertionRequiredPort();
                break;
            }
            case "Optional": {
                name = "HelloImplAssertionOptionalService";
                proxy = new HelloImplAssertionOptionalService(new URL(baseURL + "/assertion/" + name + "?wsdl")).getHelloImplAssertionOptionalPort();
                break;
            }
            case "None": {
                name = "HelloImplNoAssertionService";
                proxy = new HelloImplNoAssertionService(new URL(baseURL + "/assertion/" + name + "?wsdl")).getHelloImplNoAssertionPort();
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown service: " + service);
        }
        final Map<String, Object> ctx = ((BindingProvider) proxy).getRequestContext();
        ctx.put("javax.xml.ws.service.endpoint.address", baseURL + "/assertion/" + name);
        TxTestUtils.setTimeouts(ctx);
        return proxy;
    }

    private static String param(HttpServletRequest r, String name, String dflt) {
        final String v = r.getParameter(name);
        return v == null || v.isEmpty() ? dflt : v;
    }
}
