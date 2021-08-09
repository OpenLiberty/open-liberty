/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwtsso;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.ws.security.jwtsso.utils.ConfigUtils;

import test.common.SharedOutputManager;

public class JwtSsoTest {

    /************ begin plumbing ********** */

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance()
            .trace("com.ibm.ws.security.jwtsso.*=all");

    @Rule
    public final TestName testName = new TestName();

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.resetStreams();
        outputMgr.restoreStreams();
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void beforeTest() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        outputMgr.resetStreams();
    }

    /************** end plumbing **************/

    @Test
    public void doesNothing() throws Exception {
    }

    @SuppressWarnings("static-access")
    @Test
    public void checkCookieValidation() {
        ConfigUtils cu = new ConfigUtils();
        String cName = " cookie";
        String cName2 = "cookie ";
        String cName3 = "cookie\n";
        String normal = "myCookie";
        String result1 = cu.validateCookieName(cName, true);
        String result2 = cu.validateCookieName(cName2, true);
        String result3 = cu.validateCookieName(cName3, true);
        String result4 = cu.validateCookieName(normal, true);
        assertTrue("cookie name " + cName + " should be switched to default but was not ", result1.equals(cu.CFG_DEFAULT_COOKIENAME));
        assertTrue("cookie name " + cName2 + " should be switched to default but was not ", result2.equals(cu.CFG_DEFAULT_COOKIENAME));
        assertTrue("cookie name " + cName3 + " should be switched to default but was not ", result3.equals(cu.CFG_DEFAULT_COOKIENAME));
        assertFalse("cookie name " + normal + " should not be switched to default but was ", result4.equals(cu.CFG_DEFAULT_COOKIENAME));

    }

}