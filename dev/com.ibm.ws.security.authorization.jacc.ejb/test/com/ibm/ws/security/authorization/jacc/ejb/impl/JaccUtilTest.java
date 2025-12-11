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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ejs.container.BeanMetaData;
import com.ibm.ejs.container.EJBMethodInfoImpl;
import com.ibm.ws.ejbcontainer.EJBMethodInterface;
import com.ibm.ws.ejbcontainer.EJBMethodMetaData;
import com.ibm.ws.ejbcontainer.InternalConstants;
import com.ibm.ws.security.authorization.jacc.MethodInfo;
import com.ibm.ws.security.authorization.jacc.RoleInfo;

import test.common.SharedOutputManager;

public class JaccUtilTest {
    static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery context = new JUnit4Mockery();
    private final EJBMethodMetaData emmd = context.mock(EJBMethodMetaData.class);

    /**
     * Tests checkDataConstraints method. null param
     * Expected result: null
     */
    @Test
    public void convertMethodInfoListNull() {
        assertNull(JaccUtil.convertMethodInfoList(null));
    }

    /**
     * Tests checkDataConstraints method. Empty param
     * Expected result: null
     */
    @Test
    public void convertMethodInfoListEmpty() {
        List<EJBMethodMetaData> emmds = new ArrayList<EJBMethodMetaData>();
        assertNull(JaccUtil.convertMethodInfoList(emmds));
    }

    /**
     * Tests checkDataConstraints method normal role.
     * Expected result: valid output
     */
    @Test
    public void convertMethodInfoListOneRole() {
        final String METHOD_NAME = "endsWith";
        final String METHOD_INTERFACE_NAME = EJBMethodInterface.forValue(InternalConstants.METHOD_INTF_LOCAL).specName();

        Class<?>[] cArg = new Class[1];
        cArg[0] = String.class;
        Method method = null;
        final String PARAM_NAME = (String.class).getName();
        try {
            method = (String.class).getDeclaredMethod(METHOD_NAME, cArg);
        } catch (Exception e) {
            fail("An exception is caught while setting up the testcase : " + e);
        }
        final Method METHOD = method;
        final String ROLE = "Role1";
        List<String> roles = new ArrayList<String>();
        roles.add(ROLE);
        final List<String> ROLES = roles;

        context.checking(new Expectations() {
            {
                one(emmd).getMethodName();
                will(returnValue(METHOD_NAME));
                one(emmd).getEJBMethodInterface();
                will(returnValue(EJBMethodInterface.forValue(InternalConstants.METHOD_INTF_LOCAL)));
                one(emmd).getMethod();
                will(returnValue(METHOD));
                one(emmd).getRolesAllowed();
                will(returnValue(ROLES));
                one(emmd).isDenyAll();
                will(returnValue(false));
                one(emmd).isPermitAll();
                will(returnValue(false));
            }
        });

        List<EJBMethodMetaData> emmds = new ArrayList<EJBMethodMetaData>();
        emmds.add(emmd);
        Map<RoleInfo, List<MethodInfo>> methodMap = JaccUtil.convertMethodInfoList(emmds);
        assertNotNull(methodMap);
        assertEquals(1, methodMap.size());
        for (Entry<RoleInfo, List<MethodInfo>> e : methodMap.entrySet()) {
            RoleInfo ri = e.getKey();
            assertEquals(ROLE, ri.getRoleName());
            List<MethodInfo> mis = e.getValue();
            assertEquals(1, mis.size());
            MethodInfo mi = mis.get(0);
            assertEquals(METHOD_NAME, mi.getMethodName());
            assertEquals(METHOD_INTERFACE_NAME, mi.getMethodInterfaceName());
            List<String> outputParams = mi.getParamList();
            assertEquals(1, outputParams.size());
            assertEquals(PARAM_NAME, outputParams.get(0));
        }
    }

    /**
     * Tests checkDataConstraints method normal roles.
     * Expected result: valid output
     */
    @Test
    public void convertMethodInfoListTwoRoles() {
        final String METHOD_NAME = "endsWith";
        final String METHOD_INTERFACE_NAME = EJBMethodInterface.forValue(InternalConstants.METHOD_INTF_LOCAL).specName();

        Class<?>[] cArg = new Class[1];
        cArg[0] = String.class;
        Method method = null;
        final String PARAM_NAME = (String.class).getName();
        try {
            method = (String.class).getDeclaredMethod(METHOD_NAME, cArg);
        } catch (Exception e) {
            fail("An exception is caught while setting up the testcase : " + e);
        }
        final Method METHOD = method;
        final String ROLE = "Role";
        final String ROLE1 = "Role1";
        final String ROLE2 = "Role2";
        List<String> roles = new ArrayList<String>();
        roles.add(ROLE1);
        roles.add(ROLE2);
        final List<String> ROLES = roles;

        context.checking(new Expectations() {
            {
                one(emmd).getMethodName();
                will(returnValue(METHOD_NAME));
                one(emmd).getEJBMethodInterface();
                will(returnValue(EJBMethodInterface.forValue(InternalConstants.METHOD_INTF_LOCAL)));
                one(emmd).getMethod();
                will(returnValue(METHOD));
                one(emmd).getRolesAllowed();
                will(returnValue(ROLES));
                one(emmd).isDenyAll();
                will(returnValue(false));
                one(emmd).isPermitAll();
                will(returnValue(false));
            }
        });

        List<EJBMethodMetaData> emmds = new ArrayList<EJBMethodMetaData>();
        emmds.add(emmd);
        Map<RoleInfo, List<MethodInfo>> methodMap = JaccUtil.convertMethodInfoList(emmds);
        assertNotNull(methodMap);
        assertEquals(2, methodMap.size());
        for (Entry<RoleInfo, List<MethodInfo>> e : methodMap.entrySet()) {
            RoleInfo ri = e.getKey();
            assertTrue(ri.getRoleName().startsWith(ROLE));
            List<MethodInfo> mis = e.getValue();
            assertEquals(1, mis.size());
            MethodInfo mi = mis.get(0);
            assertEquals(METHOD_NAME, mi.getMethodName());
            assertEquals(METHOD_INTERFACE_NAME, mi.getMethodInterfaceName());
            List<String> outputParams = mi.getParamList();
            assertEquals(1, outputParams.size());
            assertEquals(PARAM_NAME, outputParams.get(0));
        }
    }

    /**
     * Tests checkDataConstraints method deny all.
     * Expected result: valid output
     */
    @Test
    public void convertMethodInfoListDenyAll() {
        final String METHOD_NAME = "endsWith";
        final String METHOD_INTERFACE_NAME = EJBMethodInterface.forValue(InternalConstants.METHOD_INTF_LOCAL).specName();

        Class<?>[] cArg = new Class[1];
        cArg[0] = String.class;
        Method method = null;
        final String PARAM_NAME = (String.class).getName();
        try {
            method = (String.class).getDeclaredMethod(METHOD_NAME, cArg);
        } catch (Exception e) {
            fail("An exception is caught while setting up the testcase : " + e);
        }
        final Method METHOD = method;

        context.checking(new Expectations() {
            {
                one(emmd).getMethodName();
                will(returnValue(METHOD_NAME));
                one(emmd).getEJBMethodInterface();
                will(returnValue(EJBMethodInterface.forValue(InternalConstants.METHOD_INTF_LOCAL)));
                one(emmd).getMethod();
                will(returnValue(METHOD));
                one(emmd).getRolesAllowed();
                will(returnValue(null));
                exactly(2).of(emmd).isDenyAll();
                will(returnValue(true));
            }
        });

        List<EJBMethodMetaData> emmds = new ArrayList<EJBMethodMetaData>();
        emmds.add(emmd);
        Map<RoleInfo, List<MethodInfo>> methodMap = JaccUtil.convertMethodInfoList(emmds);
        assertNotNull(methodMap);
        assertEquals(1, methodMap.size());
        for (Entry<RoleInfo, List<MethodInfo>> e : methodMap.entrySet()) {
            RoleInfo ri = e.getKey();
            assertTrue(ri.isDenyAll());
            List<MethodInfo> mis = e.getValue();
            assertEquals(1, mis.size());
            MethodInfo mi = mis.get(0);
            assertEquals(METHOD_NAME, mi.getMethodName());
            assertEquals(METHOD_INTERFACE_NAME, mi.getMethodInterfaceName());
            List<String> outputParams = mi.getParamList();
            assertEquals(1, outputParams.size());
            assertEquals(PARAM_NAME, outputParams.get(0));
        }
    }

    /**
     * Tests checkDataConstraints method permit all.
     * Expected result: valid output
     */
    @Test
    public void convertMethodInfoListPermitAll() {
        final String METHOD_NAME = "endsWith";
        final String METHOD_INTERFACE_NAME = EJBMethodInterface.forValue(InternalConstants.METHOD_INTF_LOCAL).specName();

        Class<?>[] cArg = new Class[1];
        cArg[0] = String.class;
        Method method = null;
        final String PARAM_NAME = (String.class).getName();
        try {
            method = (String.class).getDeclaredMethod(METHOD_NAME, cArg);
        } catch (Exception e) {
            fail("An exception is caught while setting up the testcase : " + e);
        }
        final Method METHOD = method;

        context.checking(new Expectations() {
            {
                one(emmd).getMethodName();
                will(returnValue(METHOD_NAME));
                one(emmd).getEJBMethodInterface();
                will(returnValue(EJBMethodInterface.forValue(InternalConstants.METHOD_INTF_LOCAL)));
                one(emmd).getMethod();
                will(returnValue(METHOD));
                one(emmd).getRolesAllowed();
                will(returnValue(null));
                exactly(2).of(emmd).isDenyAll();
                will(returnValue(false));
                one(emmd).isPermitAll();
                will(returnValue(true));
            }
        });

        List<EJBMethodMetaData> emmds = new ArrayList<EJBMethodMetaData>();
        emmds.add(emmd);
        Map<RoleInfo, List<MethodInfo>> methodMap = JaccUtil.convertMethodInfoList(emmds);
        assertNotNull(methodMap);
        assertEquals(1, methodMap.size());
        for (Entry<RoleInfo, List<MethodInfo>> e : methodMap.entrySet()) {
            RoleInfo ri = e.getKey();
            assertTrue(ri.isPermitAll());
            List<MethodInfo> mis = e.getValue();
            assertEquals(1, mis.size());
            MethodInfo mi = mis.get(0);
            assertEquals(METHOD_NAME, mi.getMethodName());
            assertEquals(METHOD_INTERFACE_NAME, mi.getMethodInterfaceName());
            List<String> outputParams = mi.getParamList();
            assertEquals(1, outputParams.size());
            assertEquals(PARAM_NAME, outputParams.get(0));
        }
    }

    /**
     * Tests mergeMethodInfos
     * Expected result: valid output
     */
    @Test
    public void mergeMethodInfosnull() {
        BeanMetaData bmd = new BeanMetaData(1);

        assertNull(JaccUtil.mergeMethodInfos(bmd));
    }

    /**
     * Tests mergeMethodInfos
     * Expected result: valid output
     */
    @Test
    public void mergeMethodInfosNormal() {
        BeanMetaData bmd = new BeanMetaData(1);
        EJBMethodInfoImpl emi = new EJBMethodInfoImpl(1);
        EJBMethodInfoImpl[] emis = new EJBMethodInfoImpl[1];
        emis[0] = emi;
        bmd.homeMethodInfos = emis;
        bmd.localHomeMethodInfos = emis;
        bmd.methodInfos = emis;
        bmd.localMethodInfos = emis;
        bmd.timedMethodInfos = emis;
        bmd.wsEndpointMethodInfos = emis;
        bmd.lifecycleInterceptorMethodInfos = emis;
        int ITEMS = 7;

        List<EJBMethodMetaData> output = JaccUtil.mergeMethodInfos(bmd);
        assertEquals(ITEMS, output.size());
        for (EJBMethodMetaData md : output) {
            assertEquals(emi, md);
        }
    }

    /**
     * Tests putMethodInfo
     * Expected result: valid output
     */
    @Test
    public void putMethodInfoNormal() {
        Map<RoleInfo, List<MethodInfo>> mm = new HashMap<RoleInfo, List<MethodInfo>>();
        RoleInfo ri = RoleInfo.DENY_ALL;
        MethodInfo mi = new MethodInfo("methodA", "methodA", null);
        MethodInfo miNew = new MethodInfo("methodB", "methodB", new ArrayList<String>());
        List<MethodInfo> mis = new ArrayList<MethodInfo>();
        mis.add(mi);
        mm.put(ri, mis);
        JaccUtil.putMethodInfo(mm, ri, miNew);
        assertEquals(2, mm.get(ri).size());
    }

}
