/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxws.test.wsr.server.stub.People;
import com.ibm.ws.jaxws.test.wsr.server.stub.PeopleService;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests addition of LoggingFeature when trace and debug are on.
 * Basically we supposed to see SOAP request and response in XML format in log file(s)
 * Tests aimed to run with LibertyLoggingInInterceptor and LibertyLoggingOutInterceptor
 * for jaxws-2.3 and FeatureLogging with jaxws-2.3 and xmlWs-3.0
 *
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class LoggingTest {

    @Server("LoggingServer")
    public static LibertyServer server;

    private static URL WSDL_URL;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.buildDefaultApp("helloServer", "com.ibm.ws.jaxws.test.wsr.server",
                                                      "com.ibm.ws.jaxws.test.wsr.server.impl",
                                                      "com.ibm.ws.jaxws.test.wsr.server.stub",
                                                      "com.ibm.ws.jaxws.fat.util");
        ShrinkHelper.exportDropinAppToServer(server, app);

        server.startServer("LoggingServer.log");

        assertNotNull("Application hello does not appear to have started.", server.waitForStringInLog("CWWKZ0001I:.*helloServer"));
        WSDL_URL = new URL(new StringBuilder().append("http://").append(server.getHostname()).append(":").append(server.getHttpDefaultPort()).append("/helloServer/PeopleService?wsdl").toString());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKW0056W");
        }
    }

    /**
     * Test if LoggingFeature works with a basic JAX-WS web service
     *
     * @throws Exception
     */
    @Test
    public void testFeatureLog() throws Exception {
        PeopleService service = new PeopleService(WSDL_URL);
        People bill = service.getBillPort();
        String result = bill.hello("World");
        assertTrue(result.contains("Hello World"));
        assertNotNull(server.waitForStringInLog("<return>Hello World</return>"));
    }

    /**
     * Test if LoggingFeature works with a basic JAX-WS web service from a dispatch client
     *
     * @throws Exception
     */
    @Test
    public void testFeatureLogDispatch() throws Exception {
        //dispatch client --no need stubs
        QName qs = new QName("http://server.wsr.test.jaxws.ws.ibm.com", "PeopleService");
        QName qp = new QName("http://server.wsr.test.jaxws.ws.ibm.com", "BillPort");

        // invoke the basic Service creator directly, don't use anything generated.
        Service service = Service.create(qs);
        service.addPort(qp, SOAPBinding.SOAP11HTTP_BINDING, WSDL_URL.toString());

        // now create a dispatch object from it
        Dispatch<Source> dispatch = service.createDispatch(qp, Source.class, Service.Mode.PAYLOAD);

        String msgString = "<ser:hello xmlns:ser=\"http://server.wsr.test.jaxws.ws.ibm.com\"> <arg0>from dispatch World</arg0> </ser:hello>";

        if (dispatch == null) {
            throw new RuntimeException("dispatch  is null!");
        }

        dispatch.invoke(new StreamSource(new StringReader(msgString)));

        assertNotNull(server.waitForStringInLog("<return>Hello from dispatch World</return>"));
    }

}
