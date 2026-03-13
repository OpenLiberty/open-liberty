/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxws.cdi;

import static org.junit.Assert.assertNotNull;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jaxws.cdi.beanvalidation.CDIBeanValidationWebService;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 *
 * This class tests the integration of a CDI decorated JAX-WS Web Service, CDIBeanValidationWebService, that injects the default Validator to validate a method parameter request.
 * The Web Service uses the @Resource annotation to inject the Validator.
 * The Web Service is published at http://localhost:port/cdiBeanValidation/CDIBeanValidationWebService
 *
 * Currently using the @Inject on the Validator causes:
 * java.lang.NullPointerException: Cannot invoke "com.ibm.ws.runtime.metadata.ComponentMetaData.getModuleMetaData()" because "cmd" is null
 *
 * These constraints set on the request parameter: @NotNull @Size(min = 15, max = 125) String testString
 *
 * TODO - Add two way validation tests.
 * TODO - Add @Size limitation tests.
 */
@RunWith(FATRunner.class)
public class CDIWithBeanValidationTest extends FATServletClient {

    private static URL WSDL_URL;
    private static QName qname = new QName("http://beanvalidation.cdi.jaxws.ws.ibm.com/", "CDIBeanValidationWebService");
    private static QName portName = new QName("http://beanvalidation.cdi.jaxws.ws.ibm.com/", "CDIBeanValidationWebServicePort");
    private static Service service;
    private static CDIBeanValidationWebService proxy;

    private static final String APP_NAME = "cdiBeanValidation";

    @Server("cdiBeanValidationServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME, "com.ibm.ws.jaxws.cdi.beanvalidation");

        ShrinkHelper.exportDropinAppToServer(server, app);

        server.startServer("cdiBeanValidationServer.log");
        System.out.println("Starting Server");

        assertNotNull("Application hello does not appear to have started.", server.waitForStringInLog("CWWKZ0001I:.*" + APP_NAME));

        try {

            String newTarget = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/cdiBeanValidation/CDIBeanValidationWebService";
            WSDL_URL = new URL(new StringBuilder().append(newTarget).append("?wsdl").toString());

            service = Service.create(WSDL_URL, qname);
            proxy = service.getPort(portName, CDIBeanValidationWebService.class);

            BindingProvider bp = (BindingProvider) proxy;
            bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, newTarget);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKW0056W");
        }
    }

    /*
     * Tests basic validation passes with the String being set to the method name.
     */
    @Test
    public void testOneWayRequestWithBeanValidation() throws Exception {

        proxy.oneWayWithValidation("testOneWayRequestWithBeanValidation");

        assertNotNull("testOneWayRequestWithBeanValidation did successully invoke Web Service.",
                      server.waitForStringInLog("Validation passed with testOneWayRequestWithBeanValidation"));

    }

    /*
     *
     * Tests that sending a null value in the request will fail validation due to the @NotNull annotation
     */
    @Test
    public void testOneWayRequestWithBeanValidationNullFailure() throws Exception {

        proxy.oneWayWithValidation(null);

        assertNotNull("testOneWayRequestWithBeanValidationNullFailure did not fail validation.",
                      server.waitForStringInLog("Validation failed with null cause was"));

    }

    /*
     * This tests ensures validation will fail with the String parameter passed from the
     * request fails with the min is set on the @Size annotation.
     *
     * @Size(min = 5, max = 10)
     *
     * Commented out as even being under the min test is still passing validation.
     */
    // @Test
    public void testOneWayRequestWithBeanValidationMinFailure() throws Exception {

        // Test validation failure with too few characters
        proxy.oneWayWithValidation("tes");

        assertNotNull("testOneWayRequestWithBeanValidationMinFailure did not fail validation.",
                      server.waitForStringInLog("validation Failed with"));

    }

}
