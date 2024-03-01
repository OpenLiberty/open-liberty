/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.beanvalidation;

import static org.junit.Assert.assertNotNull;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxws.cdi.beanvalidation.stubs.BeanValidationWebService;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 *
 * This class tests the integration of a Bean Validation with a JAX-WS Web Service, BeanValidationWebService, it tests both method parameter validation and code based Configuration
 * validation.
 *
 * The test class extends @see AbstractBeanValidationTest.
 * The Web Service uses the annotation to inject the Validator.
 * The Web Service is published at http://localhost:port/beanValidation/BeanValidationWebService
 * The Web Service uses JAXB classes in the com.ibm.ws.jaxws.cdi.beanvalidation.stubs package
 *
 *
 * These constraints set on the request parameter: @NotNull @Size(min = 15, max = 125) String testString
 * Currently, JAX-WS does not support method parameter validation, so this code does nothing, and the tests pass validation regardless of input.
 *
 *
 * TODO - Add two way validation tests.
 */
@RunWith(FATRunner.class)
public class BeanValidationTest extends AbstractBeanValidationTest {

    private static final String APP_NAME = "beanValidation";

    private static URL WSDL_URL;
    private static QName qname = new QName("http://beanvalidation.jaxws.ws.ibm.com/", "BeanValidationWebService");
    private static QName portName = new QName("http://beanvalidation.jaxws.ws.ibm.com/", "BeanValidationWebServicePort");
    private static Service service;
    private static BeanValidationWebService proxy;

    @Server("beanValidationServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME, "com.ibm.ws.jaxws.beanvalidation", "com.ibm.ws.jaxws.cdi.beanvalidation.stubs");

        ShrinkHelper.exportDropinAppToServer(server, app);

        server.startServer("beanValidationServer.log");
        System.out.println("Starting Server");

        try {
            WSDL_URL = new URL(new StringBuilder().append("http://")
                            .append(server.getHostname())
                            .append(":")
                            .append(server.getHttpDefaultPort())
                            .append("/beanValidation/BeanValidationWebService?wsdl")
                            .toString());

            service = Service.create(WSDL_URL, qname);

            proxy = service.getPort(portName, BeanValidationWebService.class);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        assertNotNull("Application hello does not appear to have started.", server.waitForStringInLog("CWWKZ0001I:.*" + APP_NAME));

        setLibertyServer(server);
    }

    @AfterClass
    public static void tearDown() throws Exception {

        if (server != null && server.isStarted()) {
            server.stopServer("CWWKW0056W");
        }
    }

    @Test
    public void testOneWayRequestWithBeanValidation() throws Exception {

        testOneWayRequestWithBeanValidation(proxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationWithNullFailure() throws Exception {

        testOneWayRequestWithBeanValidationWithNullFailure(proxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationOnMethodParameters() throws Exception {
        testOneWayRequestWithBeanValidationOnMethodParameters(proxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationOnMethedParametersNullFailure() throws Exception {

        testOneWayRequestWithBeanValidationOnMethodParametersNullFailure(proxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationJAXB() throws Exception {

        testOneWayRequestWithBeanValidationJAXB(proxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationJAXBNullArg0NullFailure() throws Exception {

        testOneWayRequestWithBeanValidationJAXBNullArg0NullFailure(proxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationJAXBArg1() throws Exception {

        testOneWayRequestWithBeanValidationJAXBArg1(proxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationJAXBArg1MinFailure() throws Exception {

        testOneWayRequestWithBeanValidationJAXBArg1MinFailure(proxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationJAXBArg1MaxFailure() throws Exception {

        testOneWayRequestWithBeanValidationJAXBArg1MaxFailure(proxy);
    }

}
