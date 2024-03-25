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
 * The Web Service, @see com.ibm.ws.jaxws.beanvalidation.InjectionValidatorBeanValidationWebServiceImpl, uses the @Resource annotation to inject the Validator API, which is used to
 * validate request parameters.
 * The Web Service, @see com.ibm.ws.jaxws.beanvalidation.InjectionValidatorFactoryBeanValidationWebServiceImpl, uses the @Resource annotation to inject the ValidatorFactory API,
 * which is used to validate request parameters.
 * The Validator Web Service is published at http://localhost:port/beanValidation/InjectionValidatorBeanValidationWebServiceImpl
 * The ValidatorFactory Web Service is published at http://localhost:port/beanValidation/InjectionValidatorFactoryBeanValidationWebServiceImpl
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
public class InjectionBeanValidationTest extends AbstractBeanValidationTest {

    private static final String APP_NAME = "beanValidation";

    private static URL VALIDATOR_WSDL_URL;
    private static URL VALIDATOR_FACTORY_WSDL_URL;

    private static QName validatorQName = new QName("http://beanvalidation.jaxws.ws.ibm.com/", "InjectionValidatorBeanValidationWebService");

    private static QName validatorFactoryQName = new QName("http://beanvalidation.jaxws.ws.ibm.com/", "InjectionValidatorFactoryBeanValidationWebService");

    private static QName validatorPortName = new QName("http://beanvalidation.jaxws.ws.ibm.com/", "InjectionValidatorBeanValidationWebServicePort");
    private static QName validatorFactortyPortName = new QName("http://beanvalidation.jaxws.ws.ibm.com/", "InjectionValidatorFactoryBeanValidationWebServicePort");

    private static Service validatorService;
    private static Service validatorFactoryService;
    private static BeanValidationWebService validatorFactoryProxy;
    private static BeanValidationWebService validatorProxy;

    @Server("beanValidationServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {

        WebArchive app = ShrinkHelper.buildDefaultApp(APP_NAME, "com.ibm.ws.jaxws.beanvalidation", "com.ibm.ws.jaxws.cdi.beanvalidation.stubs");

        ShrinkHelper.exportDropinAppToServer(server, app);

        server.startServer("beanValidationServer.log");
        System.out.println("Starting Server");

        try {
            VALIDATOR_WSDL_URL = new URL(new StringBuilder().append("http://")
                            .append(server.getHostname())
                            .append(":")
                            .append(server.getHttpDefaultPort())
                            .append("/beanValidation/InjectionValidatorBeanValidationWebService?wsdl")
                            .toString());

            VALIDATOR_FACTORY_WSDL_URL = new URL(new StringBuilder().append("http://")
                            .append(server.getHostname())
                            .append(":")
                            .append(server.getHttpDefaultPort())
                            .append("/beanValidation/InjectionValidatorFactoryBeanValidationWebService?wsdl")
                            .toString());

            validatorService = Service.create(VALIDATOR_WSDL_URL, validatorQName);

            validatorFactoryService = Service.create(VALIDATOR_FACTORY_WSDL_URL, validatorFactoryQName);

            validatorProxy = validatorService.getPort(validatorPortName, BeanValidationWebService.class);

            validatorFactoryProxy = validatorFactoryService.getPort(validatorFactortyPortName, BeanValidationWebService.class);

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
    public void testOneWayRequestWithBeanValidation_injectionValidator() throws Exception {

        testOneWayRequestWithBeanValidation(validatorProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationWithNullFailure_injectionValidator() throws Exception {

        testOneWayRequestWithBeanValidationWithNullFailure(validatorProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationOnMethodParameters_injectionValidator() throws Exception {
        testOneWayRequestWithBeanValidationOnMethodParameters(validatorProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationOnMethedParametersNullFailure_injectionValidator() throws Exception {

        testOneWayRequestWithBeanValidationOnMethodParametersNullFailure(validatorProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationJAXB_injectionValidator() throws Exception {

        testOneWayRequestWithBeanValidationJAXB(validatorProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationJAXBNullArg0NullFailure_injectionValidator() throws Exception {

        testOneWayRequestWithBeanValidationJAXBNullArg0NullFailure(validatorProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationJAXBArg1_injectionValidator() throws Exception {

        testOneWayRequestWithBeanValidationJAXBArg1(validatorProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationJAXBArg1MinFailure_injectionValidator() throws Exception {

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
    public void testOneWayRequestWithBeanValidationWithNullFailure_injectionValidatorFactory() throws Exception {

        testOneWayRequestWithBeanValidationWithNullFailure(validatorFactoryProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationOnMethodParameters_injectionValidatorFactory() throws Exception {
        testOneWayRequestWithBeanValidationOnMethodParameters(validatorFactoryProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationOnMethedParametersNullFailure_injectionValidatorFactory() throws Exception {

        testOneWayRequestWithBeanValidationOnMethodParametersNullFailure(validatorFactoryProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationJAXB_injectionValidatorFactory() throws Exception {

        testOneWayRequestWithBeanValidationJAXB(validatorFactoryProxy);

    }

    @Test
    public void testOneWayRequestWithBeanValidationJAXBNullArg0NullFailure_injectionValidatorFactory() throws Exception {

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
