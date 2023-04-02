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
package com.ibm.ws.wssecurity.cxf.validator;

import static org.junit.Assert.*;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import java.util.Date;
import org.junit.rules.TestRule;
import test.common.SharedOutputManager;

/**
 *
 */
public class UsernameTokenValidatorTest {
    
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery();

    UsernameTokenValidator unvalidator = new UsernameTokenValidator();

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.trace("*=all");
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.trace("*=all=disabled");
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        outputMgr.resetStreams();
    }

    /**
     * Test method for {@link com.ibm.ws.wssecurity.cxf.validator.UsernameTokenValidator#verifyCreated(java.lang.String, int, int)}.
     */
    @Test
    public void testVerifyCreated() {
        //fail("Not yet implemented");
    }

    /**
     * Test method for {@link com.ibm.ws.wssecurity.cxf.validator.UsernameTokenValidator#convertDate(java.lang.String)}.
     */
    @Test
    public void testConvertDateMissingMonth() {
        String strTimeStamp = "2016-14T20:40:43.883Z";

        try {
            Date date = UsernameTokenValidator.convertDate(strTimeStamp);
            assertNull(date);

        } catch (org.apache.wss4j.common.ext.WSSecurityException e) {
          assertTrue("Exception as expected: ",
                      e.getMessage().contains("Unparseable date: \"2016-14T20:40:43.883Z\""));
        }

    }
    
    @Test
    public void testConvertDateSuccess() {
        String strTimeStamp = "2016-12-14T20:40:43.883Z";

        try {
            Date date = unvalidator.convertDate(strTimeStamp);
            assertNotNull(date);

        } catch (org.apache.wss4j.common.ext.WSSecurityException e) {
          assertFalse("Exception not expected: ",
                      e.getMessage().contains("Unparseable date: \"2016-12-14T20:40:43.883Z\""));
        }

    }
    
    @Test
    public void testConvertDateMissingMilliSeconds_shouldWork() {
        String strTimeStamp = "2016-12-14T20:40:43Z";

        try {
            Date date = unvalidator.convertDate(strTimeStamp);
            assertNotNull(date);

        } catch (org.apache.wss4j.common.ext.WSSecurityException e) {
          assertFalse("Exception not expected: ",
                      e.getMessage().contains("Unparseable date: \"2016-12-14T20:40:43Z\""));
        }

    }
}
