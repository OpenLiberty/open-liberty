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
import com.ibm.ws.jaxws.cdi.beanvalidation.stubs.CDIBeanValidationWebService;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 *
 * This class tests the integration of a Bean Validation with a CDI decorated JAX-WS Web Service, BeanValidationWebService, it tests both method parameter validation and code based
 * Configuration
 * validation.
 * The Web Service, @see com.ibm.ws.jaxws.cdi.beanvalidation.CDIValidatorBeanValidationWebServiceImpl, uses the @Inject annotation to inject the Validator API, which is used to
 * validate request parameters.
 * The Web Service, @see com.ibm.ws.jaxws.cdi.beanvalidation.CDIInjectionValidatorFactoryBeanValidationWebServiceImpl, uses the @Resource annotation to inject the ValidatorFactory
 * API,
 * which is used to validate request parameters.
 * The Validator Web Service is published at http://localhost:port/cdiBeanValidation/CDIValidatorBeanValidationWebServiceImpl
 * The ValidatorFactory Web Service is published at http://localhost:port/cdiBeanValidation/CDIValidatorFactoryBeanValidationWebServiceImpl
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
public class CDIBeanValidationTest extends AbstractBeanValidationTest {

    private static final String APP_NAME = "cdiBeanValidation";

    private static URL VALIDATOR_WSDL_URL;
    private static URL VALIDATOR_FACTORY_WSDL_URL;

    private static QName validatorQName = new QName("http://beanvalidation.cdi.jaxws.ws.ibm.com/", "CDIValidatorBeanValidationWebService");

    private static QName validatorFactoryQName = new QName("http://beanvalidation.cdi.jaxws.ws.ibm.com/", "CDIValidatorFactoryBeanValidationWebService");

    private static QName validatorPortName = new QName("http://beanvalidation.cdi.jaxws.ws.ibm.com/", "CDIValidatorBeanValidationWebServicePort");
    private static QName validatorFactortyPortName = new QName("http://beanvalidation.cdi.jaxws.ws.ibm.com/", "CDIValidatorFactoryBeanValidationWebServicePort");

    private static Service validatorService;
    private static Service validatorFactoryService;
    private static CDIBeanValidationWebService validatorFactoryProxy;
    private static CDIBeanValidationWebService validatorProxy;

    @Server("cdiBeanValidationServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME, "com.ibm.ws.jaxws.cdi.beanvalidation", "com.ibm.ws.jaxws.cdi.beanvalidation.stubs");

        ShrinkHelper.exportDropinAppToServer(server, app);

        server.startServer("cdiBeanValidationServer.log");
        System.out.println("Starting Server");

        try {
            VALIDATOR_WSDL_URL = new URL(new StringBuilder().append("http://")
                            .append(server.getHostname())
                            .append(":")
                            .append(server.getHttpDefaultPort())
                            .append("/cdiBeanValidation/CDIValidatorBeanValidationWebService?wsdl")
                            .toString());

            VALIDATOR_FACTORY_WSDL_URL = new URL(new StringBuilder().append("http://")
                            .append(server.getHostname())
                            .append(":")
                            .append(server.getHttpDefaultPort())
                            .append("/cdiBeanValidation/CDIValidatorFactoryBeanValidationWebService?wsdl")
                            .toString());

            validatorService = Service.create(VALIDATOR_WSDL_URL, validatorQName);

            validatorFactoryService = Service.create(VALIDATOR_FACTORY_WSDL_URL, validatorFactoryQName);

            validatorProxy = validatorService.getPort(validatorPortName, CDIBeanValidationWebService.class);

            validatorFactoryProxy = validatorFactoryService.getPort(validatorFactortyPortName, CDIBeanValidationWebService.class);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        assertNotNull("Application hello does not appear to have started.", server.waitForStringInLog("CWWKZ0001I:.*" + APP_NAME));

        setLibertyServer(server);

        enableCDI();
    }

    @AfterClass
    public static void tearDown() throws Exception {

        if (server != null && server.isStarted()) {
            server.stopServer("CWWKW0056W");
        }
    }

    @Test
    public void testOneWayRequestWithBeanValidation_cdiValidator() throws Exception {

        testOneWayRequestWithBeanValidation(validatorProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationWithNullFailure_cdiValidator() throws Exception {

        testOneWayRequestWithBeanValidationWithNullFailure(validatorProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationOnMethodParameters_cdiValidator() throws Exception {
        testOneWayRequestWithBeanValidationOnMethodParameters(validatorProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationOnMethedParametersNullFailure_cdiValidator() throws Exception {

        testOneWayRequestWithBeanValidationOnMethodParametersNullFailure(validatorProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationJAXB_cdiValidator() throws Exception {

        testOneWayRequestWithBeanValidationJAXB(validatorProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationJAXBNullArg0NullFailure_cdiValidator() throws Exception {

        testOneWayRequestWithBeanValidationJAXBNullArg0NullFailure(validatorProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationJAXBArg1_cdiValidator() throws Exception {

        testOneWayRequestWithBeanValidationJAXBArg1(validatorProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationJAXBArg1MinFailure_cdiValidator() throws Exception {

        testOneWayRequestWithBeanValidationJAXBArg1MinFailure(validatorProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationJAXBArg1MaxFailure_injectionValidator() throws Exception {

        testOneWayRequestWithBeanValidationJAXBArg1MaxFailure(validatorProxy);
    }

    @Test
    public void testOneWayRequestWithBeanValidation_injectionValidatorFactory() throws Exception {

        testOneWayRequestWithBeanValidation(validatorFactoryProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationWithNullFailure_cdiValidatorFactory() throws Exception {

        testOneWayRequestWithBeanValidationWithNullFailure(validatorFactoryProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationOnMethodParameters_cdiValidatorFactory() throws Exception {
        testOneWayRequestWithBeanValidationOnMethodParameters(validatorFactoryProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationOnMethedParametersNullFailure_cdiValidatorFactory() throws Exception {

        testOneWayRequestWithBeanValidationOnMethodParametersNullFailure(validatorFactoryProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationJAXB_cdiValidatorFactory() throws Exception {

        testOneWayRequestWithBeanValidationJAXB(validatorFactoryProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationJAXBNullArg0NullFailure_cdiValidatorFactory() throws Exception {

        testOneWayRequestWithBeanValidationJAXBNullArg0NullFailure(validatorFactoryProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationJAXBArg1_injectionValidatorFactory() throws Exception {

        testOneWayRequestWithBeanValidationJAXBArg1(validatorFactoryProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationJAXBArg1MinFailure_injectionValidatorFactory() throws Exception {

        testOneWayRequestWithBeanValidationJAXBArg1MinFailure(validatorFactoryProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationJAXBArg1MaxFailure_injectionValidatorFactoryFactory() throws Exception {

        testOneWayRequestWithBeanValidationJAXBArg1MaxFailure(validatorFactoryProxy);
    }

}
