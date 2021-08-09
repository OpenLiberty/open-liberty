/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.jwk.impl;

import com.ibm.json.java.JSONObject;
import java.io.InputStream;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import test.common.SharedOutputManager;

public class Jose4jRsaJWKTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.common.*=all");

    private final String UTF_8 = "UTF-8";
    private final String RSA = "RSA";
    private final String RS256 = "RS256";
    private final String HS256 = "HS256";

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    public interface MockInterface {
    }

    final MockInterface mockInterface = mockery.mock(MockInterface.class);

    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Before
    public void beforeTest() {
        System.out.println("Entering test: " + testName.getMethodName());
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    /**
     * Method(s) under test:
     * <ul>
     * <li></li>
     * </ul>
     */
    @Test
    public void testGetInstance() {
        try {
            Jose4jRsaJWK result = Jose4jRsaJWK.getInstance(512, RS256, null, RSA);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void testGetJsonInstance() {
        try {
            JSONObject jwkObject;
            try (InputStream jwkStream = this.getClass().getResourceAsStream("jwk_test.json")) {
                jwkObject = JSONObject.parse(jwkStream);
            }

            Jose4jRsaJWK.getInstance(jwkObject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }
}
