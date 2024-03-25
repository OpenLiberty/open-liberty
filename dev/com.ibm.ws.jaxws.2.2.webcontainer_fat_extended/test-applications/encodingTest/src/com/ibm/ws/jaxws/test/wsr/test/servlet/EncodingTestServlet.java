/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.test.wsr.test.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;

import com.ibm.ws.jaxws.test.wsr.server.stub.People;
import com.ibm.ws.jaxws.test.wsr.server.stub.PeopleService;

/**
 *
 */
@WebServlet("/EncodingTestServlet")
public class EncodingTestServlet extends HttpServlet {

    private static final long serialVersionUID = -1176036061013556217L;

    @Resource(name = "services/EncodingTestService")
    private PeopleService service;

    private static final String SERVICE_URL = new StringBuilder().append("http://localhost:").append(Integer.getInteger("bvt.prop.HTTP_default")).append("/encodingApp/PeopleService").toString();

    /*
     * Tests if response encoding is default(UTF-8) encoding when another encoding set explicitly
     * Default is UTF-8
     *
     * @Param People proxy Web Service Interface
     *
     * @Return String failure message if any, if not "Pass"
     */
    private String defaultEncodedResponseReturnTest(People proxy) {
        BindingProvider bp = (BindingProvider) proxy;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, SERVICE_URL);
        String returnValue = "Pass";

        Map<String, List<String>> reqHeaders = new HashMap<String, List<String>>();
        reqHeaders.put("Content-Type", Collections.singletonList("application/soap+xml"));
        bp.getRequestContext().put(MessageContext.HTTP_REQUEST_HEADERS, reqHeaders);

        // Testing if charset UTF-8 is not set. We need to send a request without any charset
        if (!doesNotContainsCharset((Map<String, List<String>>) bp.getRequestContext().get(MessageContext.HTTP_REQUEST_HEADERS), "charset")) {
            returnValue = "There is an encoding set. we suupposed to not have any charset set";
        }

        // encoding in Japanese -> \\u30a8\\u30f3\\u30b3\\u30fc\\u30c7\\u30a3\\u30f3\\u30b0 <-( エンコーディング )
        String result = proxy.hello("UTF-8 encoded message!! ");

        // We expect to get default charset(UTF-8)
        if (doesNotContainsCharset((Map<String, List<String>>) bp.getResponseContext().get(MessageContext.HTTP_RESPONSE_HEADERS), "UTF-8")) {
            returnValue = "Default encoding (UTF-8) is failed to be set for request that doesn't contains any charset";
        }
        return returnValue;
    }

    /*
     * Tests if request encoding matches response encoding
     *
     * @Param People proxy Web Service Interface
     *
     * @Return String failure message if any, if not "Pass"
     */
    private String setEncodedResponseReturnTest(People proxy) {
        BindingProvider bp = (BindingProvider) proxy;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, SERVICE_URL);
        String returnValue = "Pass";

        Map<String, List<String>> reqHeaders = new HashMap<String, List<String>>();
        reqHeaders.put("Content-Type", Collections.singletonList("application/soap+xml; charset=ISO-8859-1"));
        bp.getRequestContext().put(MessageContext.HTTP_REQUEST_HEADERS, reqHeaders);

        // Testing if charset is set successfully to request
        if (doesNotContainsCharset((Map<String, List<String>>) bp.getRequestContext().get(MessageContext.HTTP_REQUEST_HEADERS), "ISO-8859-1")) {
            returnValue = "ISO_8859_1 encoding is failed to be set to request context";
        }

        String result = proxy.hello("ISO_8859_1 encoded message!!");

        // We expect to get same charset that we set
        if (doesNotContainsCharset((Map<String, List<String>>) bp.getResponseContext().get(MessageContext.HTTP_RESPONSE_HEADERS), "ISO-8859-1")) {
            returnValue = "Explicit set of ISO_8859_1 is failed to overwrite default encoding for response context";
        }
        return returnValue;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        runTests(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        runTests(req, resp);
    }

    public void runTests(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String target = req.getParameter("target");
        People proxy = service.getBillPort();
        String returnValue = "Pass";

        PrintWriter writer = resp.getWriter();

        if ("defaultEncodedResponseReturnTest".equals(target)) {
            returnValue = defaultEncodedResponseReturnTest(proxy);
        } else if ("setEncodedResponseReturnTest".equals(target)) {
            returnValue = setEncodedResponseReturnTest(proxy);
        } else {
            throw new ServletException("Target method is not set");
        }
        writer.write(returnValue);
        writer.close();
    }

    private boolean doesNotContainsCharset(Map<String, List<String>> context, String charset) {
        if (context != null) {
            List<String> headersList = context.get("Content-Type");
            for (String s : headersList) {
                if (s.contains(charset)) {
                    return false;
                }
            }
        }
        return true;
    }
}
