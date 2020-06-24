/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.transaction.web.XAServlet;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Example Shrinkwrap FAT project:
 * <li> Application packaging is done in the @BeforeClass, instead of ant scripting.
 * <li> Injects servers via @Server annotation. Annotation value corresponds to the
 * server directory name in 'publish/servers/%annotation_value%' where ports get
 * assigned to the LibertyServer instance when the 'testports.properties' does not
 * get used.
 * <li> Specifies an @RunWith(FATRunner.class) annotation. Traditionally this has been
 * added to bytecode automatically by ant.
 * <li> Uses the @TestServlet annotation to define test servlets. Notice that not all @Test
 * methods are defined in this class. All of the @Test methods are defined on the test
 * servlet referenced by the annotation, and will be run whenever this test class runs.
 */
@RunWith(FATRunner.class)
public class XATest extends FATServletClient {

    public static final String APP_NAME = "transaction";
    public static final String SERVLET_NAME = APP_NAME + "/XAServlet";

    @Server("com.ibm.ws.transaction")
    @TestServlet(servlet = XAServlet.class, contextRoot = APP_NAME)
    public static LibertyServer server;

    public static XATestBase xa = new XATestBase(APP_NAME, SERVLET_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        xa.setup(server);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        xa.tearDown();
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.EE8_FEATURES, SkipForRepeat.EE9_FEATURES })
    public void testSetTransactionTimeoutReturnsTrue() throws Exception {
        xa.testSetTransactionTimeoutReturnsTrue(testName);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.EE8_FEATURES, SkipForRepeat.EE9_FEATURES })
    public void testSetTransactionTimeoutReturnsFalse() throws Exception {
        xa.testSetTransactionTimeoutReturnsFalse(testName);
    }

    @Test
    @SkipForRepeat({ SkipForRepeat.EE8_FEATURES, SkipForRepeat.EE9_FEATURES })
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testSetTransactionTimeoutThrowsException() throws Exception {
        xa.testSetTransactionTimeoutThrowsException(testName);
    }
}
