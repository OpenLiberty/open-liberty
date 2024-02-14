
/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package fats.cxf.basic.jaxws;

import static org.junit.Assert.assertTrue;

import javax.servlet.annotation.WebServlet;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/DefaultPackageTestServlet")
public class DefaultPackageTestServlet extends FATServlet {

    private static String serviceClientUrl = "";

    private static final int TIMEOUT = 300; //5 minutes
    // Construct a single instance of the service client
    static {
        serviceClientUrl = new StringBuilder().append("http://localhost:").append(Integer.getInteger("bvt.prop.HTTP_default")).append("/defaultpackage/EchoStringService").toString();

    }

    /*
     * Post a message and verify the returning message on the wire
     * The post message was taken from a PostMsgSender class
     */
    @Test
    public void testMsgOnWire() throws Exception {

        String thisMethod = "testMsgOnWire()";

        String xmlMsg = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                        + "<soapenv:Body>\n"
                        + "<a:echo xmlns:a=\"http://jaxws.basic.cxf.fats/\">test1</a:echo>\n"
                        + "</soapenv:Body>\n"
                        + "</soapenv:Envelope>";
        String tn = "http://schemas.xmlsoap.org/soap/envelope";

        /*
         * expected PostMsgSender.response:
         * <?xml version="1.0" encoding="UTF-8"?>
         * <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
         * <soapenv:Body>
         * <echoResponse xmlns="http://jaxws.basic.cxf.fats/">test1</echoResponse>
         * </soapenv:Body>
         * </soapenv:Envelope>
         */

        String soapaction = "";
        boolean ignoreContent = false;
        String actual = PostMsgSender.postToURL(serviceClientUrl, xmlMsg, soapaction, TIMEOUT, ignoreContent);

        System.out.println("Received: " + actual);

        assertTrue("Expected msg to contain targetNamespace \"" + tn
                   + "\", but instead the message is: " + actual + ".",
                   actual.indexOf(tn) != -1);
    }

}