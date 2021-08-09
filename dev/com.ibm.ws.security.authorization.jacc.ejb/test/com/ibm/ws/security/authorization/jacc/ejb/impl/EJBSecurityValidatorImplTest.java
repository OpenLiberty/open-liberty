/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.authorization.jacc.ejb.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ejb.EnterpriseBean;
import javax.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import javax.security.jacc.EJBRoleRefPermission;
import javax.xml.rpc.handler.MessageContext;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import test.common.SharedOutputManager;

public class EJBSecurityValidatorImplTest {
    static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery context = new JUnit4Mockery();
    private final EnterpriseBean eBean = context.mock(EnterpriseBean.class);
    private final Context ic = context.mock(Context.class);
    private final SessionContext sc = context.mock(SessionContext.class);
    private final MessageContext mc = context.mock(MessageContext.class);

    @After
    public void tearDown() throws Exception {
        context.assertIsSatisfied();
    }

    /**
     * Tests checkResourceConstraints method
     * Expected result: false
     */
    @Test
    public void checkResourceConstraintsNormal() {
        final String contextId = "test#context#Id";
        final List<Object> methodParameters = new ArrayList<Object>();
        final String parm1 = "parm1";
        methodParameters.add(parm1);
        final String beanName = "beanName";
        final String role = "ejbRole";
        final Subject subject = new Subject();
        final EJBRoleRefPermission ejbPerm = new EJBRoleRefPermission(beanName, role);
        EJBSecurityValidatorImpl esv = new EJBSecurityValidatorImpl();
        assertFalse(esv.checkResourceConstraints(contextId, methodParameters, eBean, ejbPerm, subject));
    }

    /**
     * Tests checkResourceConstraints method with null method params.
     * Expected result: false
     */
    @Test
    public void checkResourceConstraintsNullEmptyMethodParams() {
        final String contextId = "test#context#Id";
        final List<Object> methodParameters = new ArrayList<Object>();
        final String beanName = "beanName";
        final String role = "ejbRole";
        final Subject subject = new Subject();
        final EJBRoleRefPermission ejbPerm = new EJBRoleRefPermission(beanName, role);
        EJBSecurityValidatorImpl esv = new EJBSecurityValidatorImpl();
        assertFalse(esv.checkResourceConstraints(contextId, null, eBean, ejbPerm, subject));
        assertFalse(esv.checkResourceConstraints(contextId, methodParameters, eBean, ejbPerm, subject));
    }

    /**
     * Tests checkResourceConstraints method with null bean
     * Expected result: false
     */
    @Test
    public void checkResourceConstraintsNullBean() {
        final String contextId = "test#context#Id";
        final List<Object> methodParameters = new ArrayList<Object>();
        final String parm1 = "parm1";
        methodParameters.add(parm1);
        final String beanName = "beanName";
        final String role = "ejbRole";
        final Subject subject = new Subject();
        final EJBRoleRefPermission ejbPerm = new EJBRoleRefPermission(beanName, role);
        EJBSecurityValidatorImpl esv = new EJBSecurityValidatorImpl();
        assertFalse(esv.checkResourceConstraints(contextId, methodParameters, null, ejbPerm, subject));
    }

    /**
     * Tests checkResourceConstraints method with invalid object
     * Expected result: false
     */
    @Test
    public void checkResourceConstraintsInvalidBean() {
        final String contextId = "test#context#Id";
        final List<Object> methodParameters = new ArrayList<Object>();
        final String parm1 = "parm1";
        methodParameters.add(parm1);
        final String beanName = "beanName";
        final String role = "ejbRole";
        final Subject subject = new Subject();
        final EJBRoleRefPermission ejbPerm = new EJBRoleRefPermission(beanName, role);
        EJBSecurityValidatorImpl esv = new EJBSecurityValidatorImpl();
        assertFalse(esv.checkResourceConstraints(contextId, methodParameters, new String("invalid"), ejbPerm, subject));
    }

    /**
     * Tests checkResourceConstraints method
     * Expected result: true
     */
    @Test
    public void checkResourceConstraintsNullSubject() {
        final String contextId = "test#context#Id";
        final List<Object> methodParameters = new ArrayList<Object>();
        final String beanName = "beanName";
        final String role = "ejbRole";
        final Principal principal = new X500Principal("cn=data");
        final Set<Principal> principals = new HashSet<Principal>();
        final Set<?> credentials = new HashSet<String>();
        principals.add(principal);
        final Subject subject = new Subject(false, principals, credentials, credentials);
        final EJBRoleRefPermission ejbPerm = new EJBRoleRefPermission(beanName, role);
        EJBSecurityValidatorImpl esv = new EJBSecurityValidatorImpl();
        assertFalse(esv.checkResourceConstraints(contextId, methodParameters, eBean, ejbPerm, subject));
    }

    /**
     * Tests getMessageContext method
     * Expected result: null if MessageContext doesn't exist
     */
    @Test
    public void getMessageContextNullMC() {
        try {
            context.checking(new Expectations() {
                {
                    allowing(ic).lookup("java:comp/EJBContext");
                    will(returnValue(null));
                }
            });
        } catch (NamingException e) {
            fail("NamingException is caught." + e);
        }
        EJBSecurityValidatorImpl esv = new EJBSecurityValidatorImpl();
        assertNull(esv.getMessageContext(ic));
    }

    /**
     * Tests getMessageContext method
     * Expected result: valid if MessageContext exists
     */
    @Test
    public void getMessageContextValidMC() {
        try {
            context.checking(new Expectations() {
                {
                    allowing(ic).lookup("java:comp/EJBContext");
                    will(returnValue(sc));
                    allowing(sc).getMessageContext();
                    will(returnValue(mc));
                }
            });
        } catch (NamingException e) {
            fail("NamingException is caught." + e);
        }
        EJBSecurityValidatorImpl esv = new EJBSecurityValidatorImpl();
        assertEquals(mc, esv.getMessageContext(ic));
    }

    /**
     * Tests getMessageContext method
     * Expected result: null when IllegalStateException
     */
    @Test
    public void getMessageContextISE() {
        try {
            context.checking(new Expectations() {
                {
                    allowing(ic).lookup("java:comp/EJBContext");
                    will(returnValue(sc));
                    allowing(sc).getMessageContext();
                    will(throwException(new IllegalStateException()));
                }
            });
        } catch (NamingException e) {
            fail("NamingException is caught." + e);
        }
        EJBSecurityValidatorImpl esv = new EJBSecurityValidatorImpl();
        assertNull(esv.getMessageContext(ic));
    }

}
