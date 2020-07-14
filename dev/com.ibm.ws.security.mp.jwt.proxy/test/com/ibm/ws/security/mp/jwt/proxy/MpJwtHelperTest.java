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
package com.ibm.ws.security.mp.jwt.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.security.Principal;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

public class MpJwtHelperTest {
    protected final Mockery context = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final ComponentContext cc = context.mock(ComponentContext.class);
    private final ServiceReference<JsonWebTokenUtil> jsonWebTokenUtilRef = context.mock(ServiceReference.class,
            "jsonWebTokenUtilRef");
    private final JsonWebTokenUtil jsonWebTokenUtil = context.mock(JsonWebTokenUtil.class, "jsonWebTokenUtil");
    private final JsonWebToken jwt = context.mock(JsonWebToken.class, "jwt");

    private MpJwtHelper mpJwtHelper;

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance()
            .trace("com.ibm.ws.security.mp.jwt.*=all");

    @Rule
    public final TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        System.out.println("Entering test: " + testName.getMethodName());
    }

    @After
    public void tearDown() throws Exception {
        context.assertIsSatisfied();
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    /**************************** getJsonWebTokenPrincipal *************************/
    @Test
    public void getJsonWebTokenPrincipal() {
        Set<Principal> principals = new HashSet<Principal>();
        principals.add(jwt);
        final Subject subject = new Subject(true, principals, new HashSet<Object>(), new HashSet<Object>());

        context.checking(new Expectations() {
            {
                one(cc).locateService("JsonWebTokenUtil", jsonWebTokenUtilRef);
                will(returnValue(jsonWebTokenUtil));
                one(jsonWebTokenUtil).getJsonWebTokenPrincipal(subject);
                will(returnValue(jwt));
            }
        });
        mpJwtHelper = new MpJwtHelper();
        mpJwtHelper.setJsonWebTokenUtil(jsonWebTokenUtilRef);
        mpJwtHelper.activate(cc);

        Principal output = mpJwtHelper.getJsonWebTokenPricipal(subject);

        mpJwtHelper.deactivate(cc);
        mpJwtHelper.unsetJsonWebTokenUtil(jsonWebTokenUtilRef);

        assertEquals("principal should not be null since JDK is 1.8.", jwt, output);
    }

    /**************************** addJsonWebToken *************************/
    @Test
    public void addJsonWebToken_nullCustomProps() {
        final Subject subject = new Subject();
        final String key = "key";
        context.checking(new Expectations() {
            {
                one(cc).locateService("JsonWebTokenUtil", jsonWebTokenUtilRef);
                will(returnValue(jsonWebTokenUtil));
                never(jsonWebTokenUtil).addJsonWebToken(subject, null, key);
            }
        });
        mpJwtHelper = new MpJwtHelper();
        mpJwtHelper.setJsonWebTokenUtil(jsonWebTokenUtilRef);
        mpJwtHelper.activate(cc);

        mpJwtHelper.addJsonWebToken(subject, null, key);

        mpJwtHelper.deactivate(cc);
        mpJwtHelper.unsetJsonWebTokenUtil(jsonWebTokenUtilRef);

    }

    @Test
    public void addJsonWebToken_success() {
        final Set<Principal> principals = new HashSet<Principal>();
        principals.add(jwt);
        final Subject subject = new Subject();
        final String key = "key";
        final Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(key, jwt);
        context.checking(new Expectations() {
            {
                one(cc).locateService("JsonWebTokenUtil", jsonWebTokenUtilRef);
                will(returnValue(jsonWebTokenUtil));
                one(jsonWebTokenUtil).addJsonWebToken(subject, props, key);
            }
        });
        mpJwtHelper = new MpJwtHelper();
        mpJwtHelper.setJsonWebTokenUtil(jsonWebTokenUtilRef);
        mpJwtHelper.activate(cc);

        mpJwtHelper.addJsonWebToken(subject, props, key);

        mpJwtHelper.deactivate(cc);
        mpJwtHelper.unsetJsonWebTokenUtil(jsonWebTokenUtilRef);

    }

    /**************************** cloneJsonWebToken *************************/
    @Test
    public void cloneJsonWebToken() {
        final Subject subject = new Subject();
        context.checking(new Expectations() {
            {
                one(cc).locateService("JsonWebTokenUtil", jsonWebTokenUtilRef);
                will(returnValue(jsonWebTokenUtil));
                one(jsonWebTokenUtil).cloneJsonWebToken(subject);
                will(returnValue(jwt));
            }
        });
        mpJwtHelper = new MpJwtHelper();
        mpJwtHelper.setJsonWebTokenUtil(jsonWebTokenUtilRef);
        mpJwtHelper.activate(cc);

        Principal output = mpJwtHelper.cloneJsonWebToken(subject);

        mpJwtHelper.deactivate(cc);
        mpJwtHelper.unsetJsonWebTokenUtil(jsonWebTokenUtilRef);
        assertEquals("the return value should not be null.", jwt, output);

    }

    /**************************** setJsonWebTokenUtil *************************/
    @Test
    public void setJsonWebTokenUtil() {
        mpJwtHelper = new MpJwtHelper();

        mpJwtHelper.setJsonWebTokenUtil(jsonWebTokenUtilRef);
        ServiceReference<JsonWebTokenUtil> output = mpJwtHelper.JsonWebTokenUtilRef.getReference();

        assertEquals("the reference should be null.", jsonWebTokenUtilRef, output);
    }

    /**************************** unsetJsonWebTokenUtil *************************/
    @Test
    public void unsetJsonWebTokenUtil() {
        mpJwtHelper = new MpJwtHelper();

        mpJwtHelper.JsonWebTokenUtilRef.setReference(jsonWebTokenUtilRef);
        mpJwtHelper.unsetJsonWebTokenUtil(jsonWebTokenUtilRef);
        ServiceReference<JsonWebTokenUtil> output = mpJwtHelper.JsonWebTokenUtilRef.getReference();

        assertNull("the reference should be null.", output);
    }

    /**************************** activate *************************/
    @Test
    public void activate() {
        context.checking(new Expectations() {
            {
                one(cc).locateService("JsonWebTokenUtil", jsonWebTokenUtilRef);
                will(returnValue(jsonWebTokenUtil));
            }
        });
        mpJwtHelper = new MpJwtHelper();

        mpJwtHelper.JsonWebTokenUtilRef.setReference(jsonWebTokenUtilRef);
        mpJwtHelper.activate(cc);
        mpJwtHelper.JsonWebTokenUtilRef.getService();
        mpJwtHelper.deactivate(cc);
        mpJwtHelper.JsonWebTokenUtilRef.unsetReference(jsonWebTokenUtilRef);
    }

    /**************************** deactivate *************************/
    @Test
    public void deactivate() {
        context.checking(new Expectations() {
            {
                one(cc).locateService("JsonWebTokenUtil", jsonWebTokenUtilRef);
                will(returnValue(jsonWebTokenUtil));
            }
        });
        mpJwtHelper = new MpJwtHelper();

        mpJwtHelper.JsonWebTokenUtilRef.setReference(jsonWebTokenUtilRef);
        mpJwtHelper.activate(cc);
        mpJwtHelper.JsonWebTokenUtilRef.getService();
        mpJwtHelper.deactivate(cc);
        mpJwtHelper.JsonWebTokenUtilRef.getService(); // this won't invoke locateService since it is deactivated.
        mpJwtHelper.JsonWebTokenUtilRef.unsetReference(jsonWebTokenUtilRef);
    }
}