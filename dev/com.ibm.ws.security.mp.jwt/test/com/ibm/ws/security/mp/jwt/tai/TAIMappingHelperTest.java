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
package com.ibm.ws.security.mp.jwt.tai;

import static org.junit.Assert.assertEquals;

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

import com.ibm.ws.security.mp.jwt.error.MpJwtProcessingException;

import test.common.SharedOutputManager;

/**
 *
 */
public class TAIMappingHelperTest {
    protected final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.mp.jwt.*=all");
    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void before() {
        System.out.println("Entering test: " + testName.getMethodName());
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
        //        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /**
     * Test method for {@link com.ibm.ws.security.mp.jwt.tai.TAIMappingHelper#getRealm(java.lang.String)}.
     */
    @Test
    public void testGetRealm() {
        try {
            TAIMappingHelper helper = new TAIMappingHelper(null, null);
            String realm = helper.getRealm("/issuer");
            assertEquals("the realms do not match!", "/issuer", realm);
        } catch (MpJwtProcessingException e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void testIsRealmEndsWithSlash() {
        try {
            TAIMappingHelper helper = new TAIMappingHelper(null, null);
            String realm = helper.getRealm("http://localhost:9999/issuer/");
            assertEquals("the realms do not match!", "http://localhost:9999/issuer", realm);
        } catch (MpJwtProcessingException e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

    @Test
    public void testGetRealmWithOneChar() {
        try {
            TAIMappingHelper helper = new TAIMappingHelper(null, null);
            String realm = helper.getRealm("i/");
            assertEquals("the realms do not match!", "i", realm);
        } catch (MpJwtProcessingException e) {
            outputMgr.failWithThrowable(testName.getMethodName(), e);
        }
    }

}
