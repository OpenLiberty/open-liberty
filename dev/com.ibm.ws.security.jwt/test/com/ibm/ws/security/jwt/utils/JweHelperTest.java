/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.jmock.Expectations;
import org.jose4j.base64url.Base64;
import org.jose4j.jwe.JsonWebEncryption;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.security.jwt.InvalidTokenException;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class JweHelperTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.jwt.*=all:com.ibm.ws.security.common.*=all");

    static final String MSG_CTY_NOT_JWT_FOR_NESTED_JWS = "CWWKS6057E";

    private JweHelper helper = null;

    private final JsonWebEncryption jwe = mockery.mock(JsonWebEncryption.class);

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
        helper = new JweHelper();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        mockery.assertIsSatisfied();
    }

    @Test
    public void test_verifyContentType_ctyNull() {
        final String cty = null;
        mockery.checking(new Expectations() {
            {
                one(jwe).getContentTypeHeaderValue();
                will(returnValue(cty));
            }
        });
        try {
            helper.verifyContentType(jwe);
            fail("Should have thrown an InvalidTokenException but did not.");
        } catch (InvalidTokenException e) {
            verifyException(e, MSG_CTY_NOT_JWT_FOR_NESTED_JWS);
        }
    }

    @Test
    public void test_verifyContentType_ctyEmpty() {
        final String cty = "";
        mockery.checking(new Expectations() {
            {
                one(jwe).getContentTypeHeaderValue();
                will(returnValue(cty));
            }
        });
        try {
            helper.verifyContentType(jwe);
            fail("Should have thrown an InvalidTokenException but did not.");
        } catch (InvalidTokenException e) {
            verifyException(e, MSG_CTY_NOT_JWT_FOR_NESTED_JWS);
        }
    }

    @Test
    public void test_verifyContentType_ctyNotJwt() {
        final String cty = "nope";
        mockery.checking(new Expectations() {
            {
                one(jwe).getContentTypeHeaderValue();
                will(returnValue(cty));
            }
        });
        try {
            helper.verifyContentType(jwe);
            fail("Should have thrown an InvalidTokenException but did not.");
        } catch (InvalidTokenException e) {
            verifyException(e, MSG_CTY_NOT_JWT_FOR_NESTED_JWS);
        }
    }

    @Test
    public void test_verifyContentType_ctyIsJwt() throws InvalidTokenException {
        final String cty = "jwt";
        mockery.checking(new Expectations() {
            {
                one(jwe).getContentTypeHeaderValue();
                will(returnValue(cty));
            }
        });
        helper.verifyContentType(jwe);
    }

    @Test
    public void test_getKidFromJweString_justFourPeriods() {
        final String jwe = "....";
        String result = helper.getKidFromJweString(jwe);
        assertNull("Returned kid value should have been null, but was [" + result + "].", result);
    }

    @Test
    public void test_getKidFromJweString_onePeriod() {
        final String jwe = ".";
        String result = helper.getKidFromJweString(jwe);
        assertNull("Returned kid value should have been null, but was [" + result + "].", result);
    }

    @Test
    public void test_getKidFromJweString_notBase64Encoded() {
        final String jwe = "this should be the header....";
        String result = helper.getKidFromJweString(jwe);
        assertNull("Returned kid value should have been null, but was [" + result + "].", result);
    }

    @Test
    public void test_getKidFromJweString_notBase64EncodedJson() {
        final String jwe = "{\"kid\":\"some_id\"}....";
        String result = helper.getKidFromJweString(jwe);
        assertNull("Returned kid value should have been null, but was [" + result + "].", result);
    }

    @Test
    public void test_getKidFromJweString_notJson() {
        String encoded = Base64.encode("not json".getBytes());
        final String jwe = encoded + "....";
        String result = helper.getKidFromJweString(jwe);
        assertNull("Returned kid value should have been null, but was [" + result + "].", result);
    }

    @Test
    public void test_getKidFromJweString_emptyJson() {
        String encoded = Base64.encode("{}".getBytes());
        final String jwe = encoded + "....";
        String result = helper.getKidFromJweString(jwe);
        assertNull("Returned kid value should have been null, but was [" + result + "].", result);
    }

    @Test
    public void test_getKidFromJweString_missingKidEntry() {
        String encoded = Base64.encode("{\"alg\":\"RS256\"}".getBytes());
        final String jwe = encoded + "....";
        String result = helper.getKidFromJweString(jwe);
        assertNull("Returned kid value should have been null, but was [" + result + "].", result);
    }

    @Test
    public void test_getKidFromJweString_emptyKidEntry() {
        String kid = "";
        String encoded = Base64.encode(("{\"kid\":\"" + kid + "\"}").getBytes());
        final String jwe = encoded + "....";
        String result = helper.getKidFromJweString(jwe);
        assertEquals("Returned kid value did not match expected value.", kid, result);
    }

    @Test
    public void test_getKidFromJweString_nonEmptyKidEntry() {
        String kid = "this is the kid value";
        String encoded = Base64.encode(("{\"kid\":\"" + kid + "\"}").getBytes());
        final String jwe = encoded + "....";
        String result = helper.getKidFromJweString(jwe);
        assertEquals("Returned kid value did not match expected value.", kid, result);
    }

}
