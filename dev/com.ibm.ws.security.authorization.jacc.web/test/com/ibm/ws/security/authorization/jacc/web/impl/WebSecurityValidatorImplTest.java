/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.authorization.jacc.web.impl;

import static org.junit.Assert.assertFalse;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyConfigurationFactory;
import javax.security.jacc.WebResourcePermission;
import javax.security.jacc.WebUserDataPermission;
import javax.servlet.http.HttpServletRequest;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

public class WebSecurityValidatorImplTest {
    static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery context = new JUnit4Mockery();
    private final HttpServletRequest req = context.mock(HttpServletRequest.class);
    private final PolicyConfiguration pc = context.mock(PolicyConfiguration.class);

    private PolicyConfigurationFactory pcf = null;

    @Before
    public void setUp() {
        pcf = new DummyPolicyConfigurationFactory(pc);
    }

    @After
    public void tearDown() throws Exception {
        context.assertIsSatisfied();
    }

    /**
     * Tests checkDataConstraints method
     * Expected result: true
     */
    @Test
    public void checkDataConstraintsNormal() {
        final String contextId = "test#context#Id";
        final String uriName = "/context/index.html";
        final String methodName = "POST";
        final String[] mna = new String[] { methodName };
        final WebUserDataPermission wudPerm = new WebUserDataPermission(uriName, mna, null);
        WebSecurityValidatorImpl wsv = new WebSecurityValidatorImpl();
        assertFalse(wsv.checkDataConstraints(contextId, req, wudPerm));
    }

    /**
     * Tests checkDataConstraints method
     * with invalid httpservletrequest object.
     * Expected result: false
     */
    @Test
    public void checkDataConstraintsInvalidObject() {
        final String contextId = "test#context#Id";
        final String uriName = "/context/index.html";
        final String methodName = "POST";
        final String[] mna = new String[] { methodName };
        final WebUserDataPermission wudPerm = new WebUserDataPermission(uriName, mna, null);
        WebSecurityValidatorImpl wsv = new WebSecurityValidatorImpl();
        assertFalse(wsv.checkDataConstraints(contextId, new String(), wudPerm));
        assertFalse(wsv.checkDataConstraints(contextId, null, wudPerm));
    }

    /**
     * Tests checkResourceConstraints method
     * Expected result: true
     */
    @Test
    public void checkResourceConstraintsNormal() {
        final String contextId = "test#context#Id";
        final String uriName = "/context/index.html";
        final String methodName = "POST";
        final String[] mna = new String[] { methodName };
        final Principal principal = new X500Principal("cn=data");
        final Set<Principal> principals = new HashSet<Principal>();
        final Set<?> credentials = new HashSet<String>();
        principals.add(principal);
        final Subject subject = new Subject(false, principals, credentials, credentials);
        final WebResourcePermission webPerm = new WebResourcePermission(uriName, mna);
        WebSecurityValidatorImpl wsv = new WebSecurityValidatorImpl();
        assertFalse(wsv.checkResourceConstraints(contextId, req, webPerm, subject));
    }

    /**
     * Tests checkResourceConstraints method
     * with invalid objects
     * Expected result: false
     */
    @Test
    public void checkResourceConstraintsInvalidObject() {
        final String contextId = "test#context#Id";
        final String uriName = "/context/index.html";
        final String methodName = "POST";
        final String[] mna = new String[] { methodName };
        final Principal principal = new X500Principal("cn=data");
        final Set<Principal> principals = new HashSet<Principal>();
        final Set<?> credentials = new HashSet<String>();
        principals.add(principal);
        final Subject subject = new Subject(false, principals, credentials, credentials);
        final WebResourcePermission webPerm = new WebResourcePermission(uriName, mna);
        WebSecurityValidatorImpl wsv = new WebSecurityValidatorImpl();
        assertFalse(wsv.checkResourceConstraints(contextId, new String(), webPerm, subject));
        assertFalse(wsv.checkResourceConstraints(contextId, null, webPerm, subject));
    }

}
