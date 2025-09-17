/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.org.apache.jasper.expressionLanguage60.fat.tests;

import static componenttest.custom.junit.runner.Mode.TestMode.FULL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;

/**
 * Test the Expression Language 6.0 BeanELResolver.
 *
 * This test will use a server which does not set jakarta.el.BeanSupport.useStandalone=true in the
 * bootstrap.properties. Setting this property ensures the BeanELResolver does not have
 * any dependency on the java.beans.* package. Since the property is not set for this test
 * the java.beans.* package will be used and the TestBeanBeanInfo will be used to determine
 * the read/write methods for the TestBean. A PropertyNotFoundException is not expected when trying to resolve
 * the "test" property of the TestBean because non standard read/write methods are used but are defined in the
 * TestBeanBeanInfo.
 */
@RunWith(FATRunner.class)
@Mode(FULL)
public class EL60BeanELResolverBeanSupportFullTest {

    private static final String APP_NAME = "EL60BeanELResolverTest";

    @Server("expressionLanguage60_beanELResolverBeanSupportFullServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war", "io.openliberty.el60.fat.beanelresolver.servlets", "io.openliberty.el60.fat.beanelresolver.beans");

        server.startServer(EL60BeanELResolverBeanSupportFullTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void testNonStandardGetMethod() throws Exception {
        HttpUtils.findStringInReadyUrl(server, "/" + APP_NAME + "/EL60BeanELResolverTestServlet", "Hi from TestBean!");
    }
}
