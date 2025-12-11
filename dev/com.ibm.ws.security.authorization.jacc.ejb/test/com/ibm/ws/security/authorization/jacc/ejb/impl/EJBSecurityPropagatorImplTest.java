/*******************************************************************************
 * Copyright (c) 2015, 2024 IBM Corporation and others.
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

package com.ibm.ws.security.authorization.jacc.ejb.impl;

import static org.junit.Assert.fail;

import java.security.Permissions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.jacc.EJBRoleRefPermission;
import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyConfigurationFactory;
import javax.security.jacc.PolicyContextException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.security.authorization.jacc.MethodInfo;
import com.ibm.ws.security.authorization.jacc.PolicyConfigurationManager;
import com.ibm.ws.security.authorization.jacc.RoleInfo;
import com.ibm.ws.security.authorization.jacc.common.PolicyConfigurationManagerImpl;

import test.common.SharedOutputManager;

public class EJBSecurityPropagatorImplTest {
    static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery context = new JUnit4Mockery();
    private final PolicyConfiguration pc = context.mock(PolicyConfiguration.class);
    private final String STARSTAR = "**";

    private PolicyConfigurationFactory pcf = null;
    private PolicyConfigurationManager pcm = null;

    @Before
    public void setUp() {
        pcf = new DummyPolicyConfigurationFactory(pc);
        pcm = new PolicyConfigurationManagerImpl();
        pcm.initialize(null, pcf);
    }

    @After
    public void tearDown() throws Exception {
        context.assertIsSatisfied();
    }

    /**
     * Tests propagateEJBRoleRefPermission method
     * Expected result: no exception.
     */
    @Test
    public void propagateEJBRoleRefPermissionsNormal() {
        final String contextId = "test#context#Id";
        final String appName = "applicationName";
        final String beanName = "bean";
        final Map<String, String> roleLinkMap = null;
        final Map<RoleInfo, List<MethodInfo>> methodMap = null;
        final EJBRoleRefPermission rrpStarstar = new EJBRoleRefPermission(beanName, STARSTAR);
        try {
            context.checking(new Expectations() {
                {
                    one(pc).addToRole(STARSTAR, rrpStarstar);
                }
            });
        } catch (PolicyContextException e) {
            fail("An exception is caught: " + e);
        }

        EJBSecurityPropagatorImpl esp = new EJBSecurityPropagatorImpl();
        esp.propagateEJBRoles(contextId, appName, beanName, roleLinkMap, methodMap, pcm);
        esp.processEJBRoles(pcf, contextId, pcm);
    }

    /**
     * Tests propagateRoleRefs method
     * Expected result: no exception.
     */
    @Test
    public void propagateRoleRefs() {
        final String contextId = "test#context#Id";
        final String appName = "applicationName";
        final String beanName = "bean";
        final Map<String, String> roleLinkMap = new HashMap<String, String>();
        final String refName = "refName";
        final String refLink = "refLink";
        roleLinkMap.put(refName, refLink);
        new ArrayList<String>();
        final Map<RoleInfo, List<MethodInfo>> methodMap = null;
        final EJBRoleRefPermission rrp = new EJBRoleRefPermission(beanName, refName);
        final EJBRoleRefPermission rrpLink = new EJBRoleRefPermission(beanName, refLink);
        final EJBRoleRefPermission rrpStarstar = new EJBRoleRefPermission(beanName, STARSTAR);
        try {
            context.checking(new Expectations() {
                {
                    one(pc).addToRole(refLink, rrp);
                    one(pc).addToRole(refLink, rrpLink);
                    one(pc).addToRole(STARSTAR, rrpStarstar);
                }
            });
        } catch (PolicyContextException e) {
            fail("An exception is caught: " + e);
        }

        EJBSecurityPropagatorImpl esp = new EJBSecurityPropagatorImpl();
        esp.propagateEJBRoles(contextId, appName, beanName, roleLinkMap, methodMap, pcm);
        esp.processEJBRoles(pcf, contextId, pcm);
    }

    /**
     * Tests propagateMethodPermissions method
     * Expected result: no exception.
     */
    @Test
    public void propagateMethodPermissionsValidRole() {
        final String appName = "applicationName";
        final String contextId = "test#context#Id";
        final String beanName = "bean";
        final String methodName = "methodA";
        final String methodInterfaceName = "methodInterfaceA";
        final Map<String, String> roleLinkMap = null;
        final String roleName = "roleName";
        new ArrayList<String>();
        final List<String> paramList = new ArrayList<String>();
        paramList.add("param1");
        paramList.add("param2");
        final Map<RoleInfo, List<MethodInfo>> methodMap = new HashMap<RoleInfo, List<MethodInfo>>();
        final RoleInfo ri = new RoleInfo(roleName);
        final MethodInfo mi1 = new MethodInfo(methodName, methodInterfaceName, paramList);
        // for variations for branches in EJBPermCollection method
        final MethodInfo mi2 = new MethodInfo("*", "Unspecified", paramList);
        final List<MethodInfo> mil = new ArrayList<MethodInfo>();
        mil.add(mi1);
        mil.add(mi2);
        methodMap.put(ri, mil);
        final EJBRoleRefPermission rrp = new EJBRoleRefPermission(beanName, roleName);
        final EJBRoleRefPermission rrpStarstar = new EJBRoleRefPermission(beanName, STARSTAR);
        try {
            context.checking(new Expectations() {
                {
                    one(pc).addToRole(with(equal(roleName)), with(any(Permissions.class)));
                    one(pc).addToRole(roleName, rrp);
                    one(pc).addToRole(STARSTAR, rrpStarstar);
                }
            });
        } catch (PolicyContextException e) {
            fail("An exception is caught: " + e);
        }

        EJBSecurityPropagatorImpl esp = new EJBSecurityPropagatorImpl();
        esp.propagateEJBRoles(contextId, appName, beanName, roleLinkMap, methodMap, pcm);
        esp.processEJBRoles(pcf, contextId, pcm);
    }

    /**
     * Tests propagateMethodPermissions method
     * Expected result: no exception.
     */
    @Test
    public void propagateMethodPermissionsValidDenyAll() {
        final String appName = "applicationName";
        final String contextId = "test#context#Id";
        final String beanName = "bean";
        final String methodName = "methodA";
        final String methodInterfaceName = "methodInterfaceA";
        final Map<String, String> roleLinkMap = null;
        new ArrayList<String>();
        final List<String> paramList = null;
        final Map<RoleInfo, List<MethodInfo>> methodMap = new HashMap<RoleInfo, List<MethodInfo>>();
        final RoleInfo ri = new RoleInfo();
        ri.setDenyAll();
        final MethodInfo mi = new MethodInfo(methodName, methodInterfaceName, paramList);
        final List<MethodInfo> mil = new ArrayList<MethodInfo>();
        mil.add(mi);
        methodMap.put(ri, mil);
        final EJBRoleRefPermission rrpStarstar = new EJBRoleRefPermission(beanName, STARSTAR);
        try {
            context.checking(new Expectations() {
                {
                    one(pc).addToExcludedPolicy(with(any(Permissions.class)));
                    one(pc).addToRole(STARSTAR, rrpStarstar);
                }
            });
        } catch (PolicyContextException e) {
            fail("An exception is caught: " + e);
        }

        EJBSecurityPropagatorImpl esp = new EJBSecurityPropagatorImpl();
        esp.propagateEJBRoles(contextId, appName, beanName, roleLinkMap, methodMap, pcm);
        esp.processEJBRoles(pcf, contextId, pcm);
    }

    /**
     * Tests propagateMethodPermissions method
     * Expected result: no exception.
     */
    @Test
    public void propagateMethodPermissionsValidPermitAll() {
        final String appName = "applicationName";
        final String contextId = "test#context#Id";
        final String beanName = "bean";
        final String methodName = "methodA";
        final String methodInterfaceName = "methodInterfaceA";
        final Map<String, String> roleLinkMap = null;
        new ArrayList<String>();
        final List<String> paramList = null;
        final Map<RoleInfo, List<MethodInfo>> methodMap = new HashMap<RoleInfo, List<MethodInfo>>();
        final RoleInfo ri = new RoleInfo();
        ri.setPermitAll();
        final MethodInfo mi = new MethodInfo(methodName, methodInterfaceName, paramList);
        final List<MethodInfo> mil = new ArrayList<MethodInfo>();
        mil.add(mi);
        methodMap.put(ri, mil);
        final EJBRoleRefPermission rrpStarstar = new EJBRoleRefPermission(beanName, STARSTAR);
        try {
            context.checking(new Expectations() {
                {
                    one(pc).addToUncheckedPolicy(with(any(Permissions.class)));
                    one(pc).addToRole(STARSTAR, rrpStarstar);
                }
            });
        } catch (PolicyContextException e) {
            fail("An exception is caught: " + e);
        }

        EJBSecurityPropagatorImpl esp = new EJBSecurityPropagatorImpl();
        esp.propagateEJBRoles(contextId, appName, beanName, roleLinkMap, methodMap, pcm);
        esp.processEJBRoles(pcf, contextId, pcm);
    }

}
