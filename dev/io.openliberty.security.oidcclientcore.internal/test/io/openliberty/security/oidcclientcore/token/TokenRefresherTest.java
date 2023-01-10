/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.security.oidcclientcore.token;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class TokenRefresherTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private static final String refreshToken = "QGCYpfziPZY2saAagbsf5jxbMucqcF3743euknBxzkUlof7uSv";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Test
    public void test_isAccessTokenExpired() throws Exception {
        TokenRefresher tokenRefresher = new TokenRefresher(null, null, true, false, refreshToken);
        assertTrue(tokenRefresher.isAccessTokenExpired());
        assertTrue(tokenRefresher.isTokenExpired());
    }

    @Test
    public void test_isIdTokenExpired() throws Exception {
        TokenRefresher tokenRefresher = new TokenRefresher(null, null, true, true, refreshToken);
        assertTrue(tokenRefresher.isIdTokenExpired());
        assertTrue(tokenRefresher.isTokenExpired());
    }
}
