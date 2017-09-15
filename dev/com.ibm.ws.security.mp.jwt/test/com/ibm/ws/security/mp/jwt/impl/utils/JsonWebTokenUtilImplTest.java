/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.impl.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.Principal;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.Subject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import test.common.SharedOutputManager;

public class JsonWebTokenUtilImplTest {
    protected final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.mp.jwt.*=all");

    private final JsonWebToken jwt1 = mockery.mock(JsonWebToken.class, "jwt1");
    private final JsonWebToken jwt2 = mockery.mock(JsonWebToken.class, "jwt2");
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

    /**************************** getJsonWebTokenPrincipal *************************/
    @Test
    public void getJsonWebTokenPrincipal_MpJsonWebToken() {
        Subject subject = new Subject();
        JsonWebTokenUtilImpl jwtui = new JsonWebTokenUtilImpl();
        assertNull(jwtui.getJsonWebTokenPrincipal(subject));
    }

    //TODO getJsoNWebTokenPrincipal method need to throw some exception if there are multiple JsonWebToken principal.
    @Test
    @Ignore
    public void getJsonWebTokenPrincipal_MultipleJsonWebToken() {
        final String jwtUser1 = "jwtUser1";
        final String jwtUser2 = "jwtUser2";
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(jwt1);
        principals.add(jwt2);
        Subject subject = new Subject(true, principals, new HashSet<Object>(), new HashSet<Object>());
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwt1).getName();
                    will(returnValue(jwtUser1));
                    one(jwt2).getName();
                    will(returnValue(jwtUser2));
                }
            });
            JsonWebTokenUtilImpl jwtui = new JsonWebTokenUtilImpl();
            jwtui.getJsonWebTokenPrincipal(subject);
            fail("IllegalStateException should be caught.");
        } catch (IllegalStateException e) {
            verifyException(e, "");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void getJsonWebTokenPrincipal_OneJsonWebToken() {
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(jwt1);
        Subject subject = new Subject(true, principals, new HashSet<Object>(), new HashSet<Object>());
        try {
            JsonWebTokenUtilImpl jwtui = new JsonWebTokenUtilImpl();
            Principal principal = jwtui.getJsonWebTokenPrincipal(subject);
            assertEquals("the JsonWebToken does not match.", jwt1, principal);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    /**************************** addJsonWebToken *************************/
    // TODO: the current code does not check whether subject is null before accessing it.
    // TODO: THere is a hashtable lookup but there is no null key check.

    @Test
    public void addJsonWebToken_NullCstomProperties() {
        Subject subject = new Subject();
        try {
            JsonWebTokenUtilImpl jwtui = new JsonWebTokenUtilImpl();
            jwtui.addJsonWebToken(subject, null, null);
            assertEquals("the subject should not be altered.", new Subject(), subject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addJsonWebToken_NoJsonWebTokenInCustomPrioerties() {
        Subject subject = new Subject();
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("invalid", jwt1);
        try {
            JsonWebTokenUtilImpl jwtui = new JsonWebTokenUtilImpl();
            jwtui.addJsonWebToken(subject, props, "key");
            assertEquals("the subject should not be altered.", new Subject(), subject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void addJsonWebToken_Success() {
        Subject subject = new Subject();
        String key = "key";
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(key, jwt1);
        try {
            JsonWebTokenUtilImpl jwtui = new JsonWebTokenUtilImpl();
            jwtui.addJsonWebToken(subject, props, key);
            Set<JsonWebToken> principals = subject.getPrincipals(JsonWebToken.class);
            assertFalse("Principal should exist.", principals.isEmpty());
            assertEquals("Principal should be one", 1, principals.size());
            assertEquals("Principal should be the same as the one in the properties", principals.iterator().next(), jwt1);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }
    /**************************** cloneJsonWebToken *************************/
    //TODO: the runtime code is not ready yet.

    /************************* helper methods ********************/
    private void verifyException(Exception e, String errorMsgRegex) {
        String errorMsg = e.getLocalizedMessage();
        Pattern pattern = Pattern.compile(errorMsgRegex);
        Matcher m = pattern.matcher(errorMsg);
        assertTrue("Exception message did not match expected expression. Expected: [" + errorMsgRegex + "]. Message was: [" + errorMsg + "]", m.find());
    }

}