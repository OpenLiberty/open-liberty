/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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
package com.ibm.ws.jaxrs20.cdi12.fat.test;

import static org.junit.Assert.assertEquals;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class BeanValidation12Test extends AbstractTest {

    @Server("com.ibm.ws.jaxrs20.cdi12.fat.beanvalidation")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        appname = "beanvalidation";
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname, "com.ibm.ws.jaxrs20.cdi12.fat.beanvalidation");
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKW1001W");
    }

    @Before
    public void preTest() {
        serverRef = server;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }

    @Test
    public void testIsNotViolatedInPerRequestWithCDI12_BeanValidation() throws Exception {
        runGetMethod("/rest/perrequest/book?id=123", 200, "I am a Student. 123", true);
    }

    @Test
    public void testIsViolatedInPerRequestWithCDI12_BeanValidation() throws Exception {
        runGetMethod("/rest/perrequest/book", 400, "I am a Student. null", true);
    }

    @Test
    public void testIsNotViolatedInSingletonWithCDI12_BeanValidation() throws Exception {
        runGetMethod("/rest/singleton/book?id=123", 200, "Hello from SimpleBean 123", true);
//        String uri = TestUtils.getBaseTestUri(appname, PARAM_URL_PATTERN, "/singleton/book?id=124");
    }

    @Test
    public void testIsViolatedInSingletonWithCDI12_BeanValidation() throws Exception {
        runGetMethod("/rest/singleton/book", 400, "Hello from SimpleBean null", true);
//        String uri = TestUtils.getBaseTestUri(appname, PARAM_URL_PATTERN, "/singleton/book");
    }

    @Test
    public void testIsViolatedInPerRequestWithCDI12Leak_BeanValidation() throws Exception {

        for (int i = 0; i < 10; i++) {
            runGetMethod("/rest/perrequestleak/book", 400, "I am a Student. null", true);
        }
        StringBuilder lines = runGetMethod("/rest/perrequestleak/size", 200, "", false);
        String result = lines.toString().trim();

        assertEquals(1, Integer.parseInt(result));
    }
}