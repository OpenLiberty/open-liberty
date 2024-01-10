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

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.handler.MessageContext;

import org.junit.Test;

import com.ibm.ws.jaxws.test.wsr.server.stub.People;

import componenttest.app.FATServlet;

/**
 *
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/EncodingTestServlet")
public class EncodingTestServlet extends FATServlet {

    private static final Logger LOG = Logger.getLogger("EncodingTestLogger");

    // Single service client parameters
    private static People proxy;
    private static BindingProvider bp;

    // Construct a single instance of the service client
    static {
        String SERVICE_URL = new StringBuilder().append("http://localhost:").append(Integer.getInteger("bvt.prop.HTTP_default")).append("/encodingApp/PeopleService").toString();

        QName qname = new QName("http://server.wsr.test.jaxws.ws.ibm.com", "PeopleService");
        QName portName = new QName("http://server.wsr.test.jaxws.ws.ibm.com", "PeopleService");
        Service service = Service.create(qname);
        proxy = service.getPort(portName, People.class);

        bp = (BindingProvider) proxy;
        bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, SERVICE_URL);
    }

    /*
     * Tests if request encoding matches response encoding
     */
    @Test
    public void setEncodedResponseReturnTest() throws Exception {

        Map<String, List<String>> reqHeaders = new HashMap<String, List<String>>();
        reqHeaders.put("Content-Type", Collections.singletonList("application/soap+xml; charset=ISO-8859-1"));
        bp.getRequestContext().put(MessageContext.HTTP_REQUEST_HEADERS, reqHeaders);

        // Testing if charset is set successfully to request
        assertTrue("ISO_8859_1 encoding is failed to be set to request context",
                   containsCharset((Map<String, List<String>>) bp.getRequestContext().get(MessageContext.HTTP_REQUEST_HEADERS), "ISO-8859-1"));

        String result = proxy.hello("ISO_8859_1 encoded message!!");

        // We expect to get same charset that we set
        assertTrue("Explicit set of ISO_8859_1 is failed to overwrite default encoding for response context",
                   containsCharset((Map<String, List<String>>) bp.getResponseContext().get(MessageContext.HTTP_RESPONSE_HEADERS), "ISO-8859-1"));

    }

    /*
     * Tests if response encoding is default(UTF-8) encoding when another encoding set explicitly
     * Default is UTF-8
     */
    @Test
    public void defaultEncodedResponseReturnTest() throws Exception {

        Map<String, List<String>> reqHeaders = new HashMap<String, List<String>>();
        reqHeaders.put("Content-Type", Collections.singletonList("application/soap+xml"));
        bp.getRequestContext().put(MessageContext.HTTP_REQUEST_HEADERS, reqHeaders);

        // Testing if charset UTF-8 is not set. We need to send a request without any charset
        assertTrue("There is an encoding set. we suupposed to not have any charset set",
                   containsCharset((Map<String, List<String>>) bp.getRequestContext().get(MessageContext.HTTP_REQUEST_HEADERS), ""));

        String result = proxy.hello("Default(UTF-8) encoded message!!");//\u6771\u42ac\u55b6\u696d\u90e8 <- Japanese characters

        // We expect to get default charset(UTF-8)
        assertTrue("Explicit set of ISO_8859_1 is failed to overwrite default encoding for response context",
                   containsCharset((Map<String, List<String>>) bp.getResponseContext().get(MessageContext.HTTP_RESPONSE_HEADERS), "UTF-8"));
    }

    private boolean containsCharset(Map<String, List<String>> context, String charset) {
        if (context != null) {
            List<String> headersList = context.get("Content-Type");
            for (String s : headersList) {
                if (s.contains(charset)) {
                    return true;
                }
            }
        }
        return false;
    }
}
