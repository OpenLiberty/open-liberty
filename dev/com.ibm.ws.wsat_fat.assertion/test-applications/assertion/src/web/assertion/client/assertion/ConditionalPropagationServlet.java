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
import java.io.StringReader;
import java.net.URL;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

import org.w3c.dom.Document;

import com.ibm.tx.jta.ut.util.TxTestUtils;

import web.assertion.cmt.CmtDriver;
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
 * <li>{@code op} = invoke | mixed | fault | cmt | count | init (default invoke)</li>
 * <li>{@code service} = Required | Optional | None | Dispatch | DispatchRequired. {@code Dispatch} and
 * {@code DispatchRequired} call the Optional / Required endpoint through a {@link Dispatch} client created
 * without any WSDL, so the client cannot determine the target's policy.</li>
 * <li>{@code tran} = GlobalCommit | GlobalRollback | NoTran</li>
 * <li>{@code key} = unique key. The endpoint inserts a row with this key; {@code op=count} reports how many exist.</li>
 * <li>{@code seq} (op=mixed) = underscore separated list of REQ | OPT | NONE, e.g. {@code REQ_NONE_REQ}. The
 * calls are made in order inside one transaction. Call <i>i</i> (0-based) uses the row key {@code <key>-<i>}.
 * The result is {@code OK:<reply0>|<reply1>|...}.</li>
 * <li>{@code op=fault}: in one global transaction, call the Required endpoint (key {@code <key>-req}), then
 * call the no-assertion endpoint with a key that makes it throw a SOAP fault, and catch that fault. Then
 * commit. The result is {@code OK:<reply>;caught=<class>;status=<UserTransaction status after the fault>}.</li>
 * <li>{@code op=cmt}: call {@code service} from an EJB or CDI bean (see {@link CmtDriver}). {@code kind} = EJB | BMT | CDI,
 * {@code attr} = REQUIRED | REQUIRES_NEW | MANDATORY | SUPPORTS | NOT_SUPPORTED | NEVER, {@code tran} = the transaction
 * the bean is called in (as above), {@code rollback} = true to roll back the bean's own transaction.</li>
 * <li>{@code baseurl}</li>
 * </ul>
 */
@WebServlet({ "/ConditionalPropagationServlet" })
public class ConditionalPropagationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String NS = "http://server.assertion.web/";

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
                case "mixed":
                    result = invokeMixed(request.getParameter("seq"), request.getParameter("tran"),
                                         request.getParameter("key"), param(request, "baseurl", "http://localhost:8010"));
                    break;
                case "cmt":
                    result = invokeCmt(request.getParameter("kind"), request.getParameter("attr"), request.getParameter("service"),
                                       request.getParameter("tran"), Boolean.parseBoolean(request.getParameter("rollback")),
                                       request.getParameter("key"), param(request, "baseurl", "http://localhost:8010"));
                    break;
                case "fault":
                    result = invokeFaultIsolation(request.getParameter("key"), param(request, "baseurl", "http://localhost:8010"));
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
                if ("Dispatch".equals(service)) {
                    reply = dispatch("HelloImplAssertionOptionalService", key, baseURL);
                } else if ("DispatchRequired".equals(service)) {
                    reply = dispatch("HelloImplAssertionRequiredService", key, baseURL);
                } else {
                    reply = proxy(service, baseURL).sayHelloToOther(key);
                }
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

    /**
     * Call {@code service} from an EJB or CDI bean (see {@link CmtDriver}), optionally inside a global transaction
     * begun here, so that the bean's transaction attribute decides which transaction, if any, the call is made in.
     */
    private String invokeCmt(String kind, String attr, String service, String tran, boolean rollback, String key, String baseURL) {
        final boolean global = "GlobalCommit".equals(tran) || "GlobalRollback".equals(tran);
        boolean begun = false;
        try {
            final Hello hello = proxy(service, baseURL);
            if (global) {
                userTransaction.begin();
                begun = true;
            }

            final String reply;
            try {
                reply = CmtDriver.call(kind, attr, hello, key, rollback);
            } catch (Exception e) {
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

    /**
     * Make several calls, in the order given by {@code seq}, inside a single global transaction.
     */
    private String invokeMixed(String seq, String tran, String key, String baseURL) {
        boolean begun = false;
        try {
            userTransaction.begin();
            begun = true;

            final StringBuilder replies = new StringBuilder();
            final String[] steps = seq.split("_");
            for (int i = 0; i < steps.length; i++) {
                final String service;
                switch (steps[i]) {
                    case "REQ":
                        service = "Required";
                        break;
                    case "OPT":
                        service = "Optional";
                        break;
                    case "NONE":
                        service = "None";
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown step in seq: " + steps[i]);
                }
                if (i > 0) {
                    replies.append('|');
                }
                try {
                    replies.append(proxy(service, baseURL).sayHelloToOther(key + "-" + i));
                } catch (RuntimeException e) {
                    rollbackQuietly();
                    throw e;
                }
            }

            if ("GlobalCommit".equals(tran)) {
                userTransaction.commit();
            } else {
                userTransaction.rollback();
            }
            return "OK:" + replies;
        } catch (Exception e) {
            e.printStackTrace();
            if (begun) {
                rollbackQuietly();
            }
            return "EXCEPTION:" + e.getClass().getSimpleName() + ":" + e.getMessage();
        }
    }

    /**
     * An application fault from a no-assertion endpoint must not poison a global transaction that has
     * enlisted a Required endpoint. The no-assertion endpoint throws when the key starts with {@code FAULT-}.
     */
    private String invokeFaultIsolation(String key, String baseURL) {
        boolean begun = false;
        try {
            userTransaction.begin();
            begun = true;

            final String reply = proxy("Required", baseURL).sayHelloToOther(key + "-req");

            String caught = "none";
            try {
                proxy("None", baseURL).sayHelloToOther("FAULT-" + key);
            } catch (Exception e) {
                caught = e.getClass().getSimpleName();
            }

            final int status = userTransaction.getStatus();
            userTransaction.commit();
            begun = false;
            return "OK:" + reply + ";caught=" + caught + ";status=" + status;
        } catch (Exception e) {
            e.printStackTrace();
            if (begun) {
                rollbackQuietly();
            }
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
            case "PolicyAtOperation":
            case "PolicyAtPort":
            case "Coexisting": {
                // Endpoints whose WS-AT assertion is attached at a different level of the WSDL, or alongside
                // another policy assertion. The client reads the endpoint's live WSDL.
                final String impl = "Coexisting".equals(service) ? "HelloImplCoexistingPolicies" : "HelloImpl" + service;
                name = impl + "Service";
                proxy = Service.create(new URL(baseURL + "/assertion/" + name + "?wsdl"), new QName(NS, name))
                                .getPort(new QName(NS, impl + "Port"), Hello.class);
                break;
            }
            case "StaleRequired":
            case "StaleOptional": {
                // The endpoint advertises a WS-AT assertion, but this client uses its own packaged copy of the
                // WSDL, which has none. The packaged copy is the one CXF builds the client's policy from.
                name = "StaleRequired".equals(service) ? "HelloImplAssertionRequiredService" : "HelloImplAssertionOptionalService";
                final URL packaged = getServletContext().getResource("/WEB-INF/wsdl/noPolicyAssertion.wsdl");
                proxy = Service.create(packaged, new QName(NS, "HelloImplNoAssertionService"))
                                .getPort(new QName(NS, "HelloImplNoAssertionPort"), Hello.class);
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

    /**
     * Call {@code sayHelloToOther} on the endpoint {@code endpoint} with a {@link Dispatch} client created
     * without a WSDL. The endpoint advertises a WS-AT assertion, but this client never reads it, so
     * its effective policy cannot be determined.
     */
    private String dispatch(String endpoint, String key, String baseURL) throws Exception {
        final String ns = "http://server.assertion.web/";
        final QName portName = new QName(ns, "DispatchNoWsdlPort");
        final Service service = Service.create(new QName(ns, "DispatchNoWsdlService"));
        service.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, baseURL + "/assertion/" + endpoint);
        final Dispatch<Source> dispatch = service.createDispatch(portName, Source.class, Service.Mode.PAYLOAD);
        TxTestUtils.setTimeouts(dispatch.getRequestContext());

        final Source request = new StreamSource(new StringReader("<ns:sayHelloToOther xmlns:ns=\"" + ns + "\"><arg0>" + key + "</arg0></ns:sayHelloToOther>"));
        final DOMResult response = new DOMResult();
        TransformerFactory.newInstance().newTransformer().transform(dispatch.invoke(request), response);
        return ((Document) response.getNode()).getElementsByTagNameNS("*", "return").item(0).getTextContent();
    }

    private static String param(HttpServletRequest r, String name, String dflt) {
        final String v = r.getParameter(name);
        return v == null || v.isEmpty() ? dflt : v;
    }
}
