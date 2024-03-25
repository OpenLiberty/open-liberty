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
package com.ibm.ws.jaxws.security.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 *
 * This class tests the security of EJB based Web Services, which are packaged
 * in a jar package.security constraints are configured in ibm-ws-bnd.xml
 */
@RunWith(FATRunner.class)
public class EJBInJarServiceSecurityWithBndTest extends EJBInJarServiceSecurityTest {

    @BeforeClass
    public static void setUp() throws Exception {
        init();
        // add ibm-ws-bnd.xml
        server.copyFileToLibertyServerRoot("apps/EJBInJarServiceSecurity.ear/EJBInJarServiceSecurity.jar/META-INF", "EJBWSSecurityFileStore/ibm-ws-bnd.xml");
        startServer("EJBInJarServiceSecurityWithBndTest.log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        stopServer();
    }

    @Mode(TestMode.FULL)
    @Test
    @AllowedFFDC({ "java.rmi.AccessException" })
    public void test_ejbws_in_jar_security_with_bndfile_nonsecure() throws Exception {

        runTest("user1", "user2pwd", "SayHelloService", "Hello user1 from ejb web service.", false);
        runTest("user1", "user1pwd", "SayHelloService", "Hello user1 from ejb web service.", false);
        runTest("user2", "user2pwd", "SayHelloService", "Hello user2 from ejb web service.", true);
        runTest("user3", "user3pwd", "SayHelloService", "Hello user3 from ejb web service.", true);
        runTest("user4", "user4pwd", "SayHelloService", "Hello user4 from ejb web service.", false);
    }

    @Mode(TestMode.FULL)
    @Test
    @AllowedFFDC({ "java.rmi.AccessException" })
    public void test_ejbws_in_jar_security_with_bndfile_secure() throws Exception {

        runTest("user1", "user2pwd", "SecuredSayHelloService", "Hello user1 from secured ejb web service.", false);
        runTest("user1", "user1pwd", "SecuredSayHelloService", "Hello user1 from secured ejb web service.", false);
        runTest("user2", "user2pwd", "SecuredSayHelloService", "Hello user2 from secured ejb web service.", true);
        runTest("user3", "user3pwd", "SecuredSayHelloService", "Hello user3 from secured ejb web service.", false);
        runTest("user4", "user4pwd", "SecuredSayHelloService", "Hello user4 from secured ejb web service.", false);
    }
}
