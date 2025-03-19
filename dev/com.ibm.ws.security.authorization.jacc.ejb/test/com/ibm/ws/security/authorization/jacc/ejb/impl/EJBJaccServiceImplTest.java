/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.ws.security.authorization.jacc.ejb.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.Policy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EnterpriseBean;
import javax.security.auth.Subject;
import javax.security.jacc.EJBMethodPermission;
import javax.security.jacc.EJBRoleRefPermission;
import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyConfigurationFactory;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.authorization.jacc.JaccService;
import com.ibm.ws.security.authorization.jacc.MethodInfo;
import com.ibm.ws.security.authorization.jacc.PolicyConfigurationManager;
import com.ibm.ws.security.authorization.jacc.RoleInfo;
import com.ibm.ws.security.authorization.jacc.common.PolicyConfigurationManagerImpl;
import com.ibm.ws.security.authorization.jacc.common.PolicyProxy;
import com.ibm.ws.security.authorization.jacc.common.ProviderServiceProxy;
import com.ibm.ws.security.authorization.jacc.ejb.EJBSecurityPropagator;
import com.ibm.ws.security.authorization.jacc.internal.JaccServiceImpl;
import com.ibm.ws.security.authorization.jacc.internal.JaccServiceTestUtil;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.security.authorization.jacc.ProviderService;

import test.common.SharedOutputManager;

public class EJBJaccServiceImplTest {
    static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery context = new JUnit4Mockery();
    private final ComponentContext cc = context.mock(ComponentContext.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<ProviderServiceProxy> jaccProviderServiceProxyRef = context.mock(ServiceReference.class, "providerServiceProxyRef");
    private final ProviderServiceProxy jaccProviderServiceProxy = context.mock(ProviderServiceProxy.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<ProviderService> jaccProviderServiceRef = context.mock(ServiceReference.class, "providerServiceRef");
    private final ProviderService jaccProviderService = context.mock(ProviderService.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<WsLocationAdmin> wsLocationAdminRef = context.mock(ServiceReference.class, "wsLocationAdminRef");
    private final WsLocationAdmin wsLocationAdmin = context.mock(WsLocationAdmin.class);
    private final PolicyConfiguration pc = context.mock(PolicyConfiguration.class);
    private final EnterpriseBean eBean = context.mock(EnterpriseBean.class);
    private final EJBSecurityPropagator esp = context.mock(EJBSecurityPropagator.class);
    private final ServiceReference<JaccService> jaccServiceRef = context.mock(ServiceReference.class, "jaccServiceRef");
    private final EJBJaccServiceImpl ejbJaccService = new EJBJaccServiceImpl();

    private final Policy policy = Policy.getPolicy();
    private final PolicyProxy policyProxy = context.mock(PolicyProxy.class);
    private final PolicyConfigurationFactory pcf = new DummyPolicyConfigurationFactory(pc);
    private final PolicyConfigurationManager pcm = new PolicyConfigurationManagerImpl();

    private static final String JACC_FACTORY = "javax.security.jacc.PolicyConfigurationFactory.provider";
    private static final String JACC_FACTORY_EE9 = "jakarta.security.jacc.PolicyConfigurationFactory.provider";
    private static final String JACC_POLICY_PROVIDER = "javax.security.jacc.policy.provider";
    private static final String JACC_POLICY_PROVIDER_EE9 = "jakarta.security.jacc.policy.provider";
    private static final String JACC_FACTORY_IMPL = "com.ibm.ws.security.authorization.jacc.internal.DummyPolicyConfigurationFactory";
    private static final String JACC_POLICY_PROVIDER_IMPL = "com.ibm.ws.security.authorization.jacc.internal.DummyPolicy";
    private static final String JACC_EJB_METHOD_ARGUMENT = "RequestMethodArgumentsRequired";

    private final String origPp = System.getProperty(JACC_POLICY_PROVIDER);
    private final String origPpEe9 = System.getProperty(JACC_POLICY_PROVIDER_EE9);
    private final String origFn = System.getProperty(JACC_FACTORY);
    private final String origFnEe9 = System.getProperty(JACC_FACTORY_EE9);

    @After
    public void tearDown() throws Exception {
        // clean up.
        if (origPp != null) {
            System.setProperty(JACC_POLICY_PROVIDER, origPp);
        } else {
            System.clearProperty(JACC_POLICY_PROVIDER);
        }
        if (origPpEe9 != null) {
            System.setProperty(JACC_POLICY_PROVIDER_EE9, origPpEe9);
        } else {
            System.clearProperty(JACC_POLICY_PROVIDER_EE9);
        }
        if (origFn != null) {
            System.getProperty(JACC_FACTORY, origFn);
        } else {
            System.clearProperty(JACC_FACTORY);
        }
        if (origFnEe9 != null) {
            System.getProperty(JACC_FACTORY_EE9, origFnEe9);
        } else {
            System.clearProperty(JACC_FACTORY_EE9);
        }
        Policy.setPolicy(policy);
    }

    /**
     * Tests propagateEJBRoles method
     * Expected result: no exception.
     */
    @Test
    public void propagateEJBRoles() {
        final String appName = "applicationName";
        final String moduleName = "moduleName";
        final String beanName = "testBean";

        try {
            ejbJaccService.propagateEJBRoles(esp, appName, moduleName, beanName, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests propagateEJBRoles method
     * Expected result: no exception.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void propagateEJBRolesValid() {
        final String directory = "/wlp/test";
        final String name = "jaccServer";
        final String appName = "applicationName";
        final String moduleName = "moduleName";
        final String beanName = "testBean";
        final Map<String, String> rl = new HashMap<String, String>();
        final Map<RoleInfo, List<MethodInfo>> mm = new HashMap<RoleInfo, List<MethodInfo>>();
        final JaccServiceImpl jaccService = JaccServiceTestUtil.createJaccService(pcm);

        context.checking(new Expectations() {
            {
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER);
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER_EE9);
                will(returnValue(JACC_POLICY_PROVIDER_IMPL));
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY);
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY_EE9);
                will(returnValue(JACC_FACTORY_IMPL));
                allowing(cc).locateService("jaccProviderServiceProxy", jaccProviderServiceProxyRef);
                will(returnValue(jaccProviderServiceProxy));
                allowing(cc).locateService("jaccProviderService", jaccProviderServiceRef);
                will(returnValue(jaccProviderService));
                allowing(cc).locateService("locationAdmin", wsLocationAdminRef);
                will(returnValue(wsLocationAdmin));
                allowing(jaccProviderServiceProxy).getPolicyProxy(pcm);
                will(returnValue(policyProxy));
                allowing(jaccProviderServiceProxy).getPolicyConfigFactory();
                will(returnValue(pcf));
                allowing(jaccProviderService).getPolicy();
                will(returnValue(policy));
                allowing(policyProxy).setPolicy();
                allowing(policyProxy).refresh();
                allowing(jaccProviderService).getPolicyConfigFactory();
                will(returnValue(pcf));
                allowing(jaccProviderServiceProxy).getPolicyName();
                will(returnValue(JACC_POLICY_PROVIDER_IMPL));
                allowing(jaccProviderServiceProxy).getFactoryName();
                will(returnValue(JACC_FACTORY_IMPL));
                allowing(wsLocationAdmin).resolveString("${wlp.user.dir}");
                will(returnValue(directory));
                allowing(wsLocationAdmin).getServerName();
                will(returnValue(name));
                allowing(cc).locateService("jaccService", jaccServiceRef);
                will(returnValue(jaccService));
                allowing(esp).propagateEJBRoles(with(any(String.class)), with(any(String.class)), with(any(String.class)), with(any(HashMap.class)), with(any(HashMap.class)),
                                                with(any(PolicyConfigurationManager.class)));
            }
        });
        try {
            JaccServiceTestUtil.initJaccService(jaccService, jaccProviderServiceProxyRef, jaccProviderServiceRef, wsLocationAdminRef, cc);
            ejbJaccService.setJaccService(jaccServiceRef);
            ejbJaccService.activate(cc);
            ejbJaccService.propagateEJBRoles(esp, appName, moduleName, beanName, rl, mm);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests isAuthorized method
     * Expected result: false if there is no permission defined.
     */
    @Test
    public void isAuthorized() {
        final String appName = "applicationName";
        final String moduleName = "moduleName";
        final String beanName = "testBean";
        final String methodName = "testMethod";
        final String methodInterface = "String";
        final String ms1 = null;
        final List<Object> methodParameters = null;
        final Subject subject = new Subject();

        try {
            assertFalse(ejbJaccService.isAuthorized(appName, moduleName, beanName, methodName, methodInterface, ms1, methodParameters, eBean, subject));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests isAuthorized method
     * Expected result: false if there is no permission defined.
     */
    @Test
    public void isAuthorizedEjbValid() {
        final String appName = "applicationName";
        final String moduleName = "moduleName";
        final String beanName = "testBean";
        final String methodName = "testMethod";
        final String methodInterface = "String";
        final String ms2 = "aaa:bbb,ccc,ddd";
        final String ms3 = "aaa";
        final String ms4 = "aaa:";
        final List<Object> mp = new ArrayList<Object>();
        final String directory = "/wlp/test";
        final String name = "jaccServer";
        final Subject subject = new Subject();
        final JaccServiceImpl jaccService = JaccServiceTestUtil.createJaccService(pcm);

        context.checking(new Expectations() {
            {
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER);
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER_EE9);
                will(returnValue(JACC_POLICY_PROVIDER_IMPL));
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY);
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY_EE9);
                will(returnValue(JACC_FACTORY_IMPL));
                allowing(cc).locateService("jaccProviderServiceProxy", jaccProviderServiceProxyRef);
                will(returnValue(jaccProviderServiceProxy));
                allowing(cc).locateService("jaccProviderService", jaccProviderServiceRef);
                will(returnValue(jaccProviderService));
                allowing(cc).locateService("locationAdmin", wsLocationAdminRef);
                will(returnValue(wsLocationAdmin));
                allowing(jaccProviderServiceProxy).getPolicyProxy(pcm);
                will(returnValue(policyProxy));
                allowing(jaccProviderServiceProxy).getPolicyConfigFactory();
                will(returnValue(pcf));
                allowing(jaccProviderService).getPolicy();
                will(returnValue(policy));
                allowing(policyProxy).setPolicy();
                allowing(policyProxy).refresh();
                allowing(jaccProviderService).getPolicyConfigFactory();
                will(returnValue(pcf));
                allowing(jaccProviderServiceProxy).getPolicyName();
                will(returnValue(JACC_POLICY_PROVIDER_IMPL));
                allowing(jaccProviderServiceProxy).getFactoryName();
                will(returnValue(JACC_FACTORY_IMPL));
                allowing(wsLocationAdmin).resolveString("${wlp.user.dir}");
                will(returnValue(directory));
                allowing(wsLocationAdmin).getServerName();
                will(returnValue(name));
                allowing(cc).locateService("jaccService", jaccServiceRef);
                will(returnValue(jaccService));
                allowing(policyProxy).implies(with(any(String.class)), with(any(Subject.class)), with(any(EJBMethodPermission.class)));
                will(returnValue(true));
            }
        });

        try {
            JaccServiceTestUtil.initJaccService(jaccService, jaccProviderServiceProxyRef, jaccProviderServiceRef, wsLocationAdminRef, cc);
            ejbJaccService.setJaccService(jaccServiceRef);
            ejbJaccService.activate(cc);
            assertTrue(ejbJaccService.isAuthorized(appName, moduleName, beanName, methodName, methodInterface, ms2, mp, eBean, subject));
            // different method signature
            assertTrue(ejbJaccService.isAuthorized(appName, moduleName, beanName, methodName, methodInterface, ms3, mp, eBean, subject));
            assertTrue(ejbJaccService.isAuthorized(appName, moduleName, beanName, methodName, methodInterface, ms4, mp, eBean, subject));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests isSubjectInRole method
     * Expected result: false if there is no permission defined.
     */
    @Test
    public void isEjbSubjectInRole() {
        final String appName = "applicationName";
        final String moduleName = "moduleName";
        final String beanName = "testBean";
        final String methodName = "testMethod";
        final List<Object> mp = null;
        final String role = "allRole";
        final Subject subject = new Subject();
        try {
            // this is for null check
            assertFalse(ejbJaccService.isSubjectInRole(appName, moduleName, beanName, methodName, mp, role, eBean, subject));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests isSubjectInRole method
     * Expected result: false if there is no permission defined.
     */
    @Test
    public void isEjbSubjectInRoleValid() {
        final String appName = "applicationName";
        final String moduleName = "moduleName";
        final String beanName = "testBean";
        final String methodName = "testMethod";
        final List<Object> mp = new ArrayList<Object>();
        final String role = "allRole";
        final String directory = "/wlp/test";
        final String name = "jaccServer";
        final Subject subject = new Subject();
        final JaccServiceImpl jaccService = JaccServiceTestUtil.createJaccService(pcm);

        context.checking(new Expectations() {
            {
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER);
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER_EE9);
                will(returnValue(JACC_POLICY_PROVIDER_IMPL));
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY);
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY_EE9);
                will(returnValue(JACC_FACTORY_IMPL));
                allowing(cc).locateService("jaccProviderServiceProxy", jaccProviderServiceProxyRef);
                will(returnValue(jaccProviderServiceProxy));
                allowing(cc).locateService("jaccProviderService", jaccProviderServiceRef);
                will(returnValue(jaccProviderService));
                allowing(cc).locateService("locationAdmin", wsLocationAdminRef);
                will(returnValue(wsLocationAdmin));
                allowing(jaccProviderServiceProxy).getPolicyProxy(pcm);
                will(returnValue(policyProxy));
                allowing(jaccProviderServiceProxy).getPolicyConfigFactory();
                will(returnValue(pcf));
                allowing(jaccProviderService).getPolicy();
                will(returnValue(policy));
                allowing(policyProxy).setPolicy();
                allowing(policyProxy).refresh();
                allowing(jaccProviderService).getPolicyConfigFactory();
                will(returnValue(pcf));
                allowing(jaccProviderServiceProxy).getPolicyName();
                will(returnValue(JACC_POLICY_PROVIDER_IMPL));
                allowing(jaccProviderServiceProxy).getFactoryName();
                will(returnValue(JACC_FACTORY_IMPL));
                allowing(wsLocationAdmin).resolveString("${wlp.user.dir}");
                will(returnValue(directory));
                allowing(wsLocationAdmin).getServerName();
                will(returnValue(name));
                allowing(cc).locateService("jaccService", jaccServiceRef);
                will(returnValue(jaccService));
                allowing(policyProxy).implies(with(any(String.class)), with(any(Subject.class)), with(any(EJBRoleRefPermission.class)));
                will(returnValue(true));
            }
        });

        try {
            JaccServiceTestUtil.initJaccService(jaccService, jaccProviderServiceProxyRef, jaccProviderServiceRef, wsLocationAdminRef, cc);
            ejbJaccService.setJaccService(jaccServiceRef);
            ejbJaccService.activate(cc);
            assertTrue(ejbJaccService.isSubjectInRole(appName, moduleName, beanName, methodName, mp, role, eBean, subject));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception is caught : " + e);
        }
    }

    private void areRequestMethodArgumentsRequired(Object value, boolean expectedValue) {
        final JaccServiceImpl jaccService = JaccServiceTestUtil.createJaccService(pcm);
        context.checking(new Expectations() {
            {
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER);
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER_EE9);
                will(returnValue(JACC_POLICY_PROVIDER_IMPL));
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY);
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY_EE9);
                will(returnValue(JACC_FACTORY_IMPL));
                allowing(cc).locateService("jaccProviderServiceProxy", jaccProviderServiceProxyRef);
                will(returnValue(jaccProviderServiceProxy));
                allowing(cc).locateService("jaccProviderService", jaccProviderServiceRef);
                will(returnValue(jaccProviderService));
                allowing(cc).locateService("locationAdmin", wsLocationAdminRef);
                will(returnValue(wsLocationAdmin));
                allowing(jaccProviderServiceProxy).getPolicyProxy(pcm);
                will(returnValue(policyProxy));
                allowing(jaccProviderServiceProxy).getPolicyConfigFactory();
                will(returnValue(pcf));
                allowing(jaccProviderService).getPolicy();
                will(returnValue(policy));
                allowing(policyProxy).setPolicy();
                allowing(policyProxy).refresh();
                allowing(jaccProviderService).getPolicyConfigFactory();
                will(returnValue(pcf));
                allowing(jaccProviderServiceProxy).getPolicyName();
                will(returnValue(JACC_POLICY_PROVIDER_IMPL));
                allowing(jaccProviderServiceProxy).getFactoryName();
                will(returnValue(JACC_FACTORY_IMPL));
                allowing(jaccProviderServiceProxy).getProperty(JACC_EJB_METHOD_ARGUMENT);
                will(returnValue(value));
                allowing(jaccProviderServiceRef).getProperty(JACC_EJB_METHOD_ARGUMENT);
                will(returnValue(value));
                allowing(cc).locateService("jaccService", jaccServiceRef);
                will(returnValue(jaccService));
            }
        });

        try {
            JaccServiceTestUtil.initJaccService(jaccService, jaccProviderServiceProxyRef, jaccProviderServiceRef, wsLocationAdminRef, cc);
            ejbJaccService.setJaccService(jaccServiceRef);
            ejbJaccService.activate(cc);
            assertEquals(expectedValue, ejbJaccService.areRequestMethodArgumentsRequired());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests areRequestMethodArgumentsRequired method
     * Expected result: true
     */
    @Test
    public void areRequestMethodArgumentsRequiredTrue() {
        areRequestMethodArgumentsRequired("true", true);
    }

    /**
     * Tests areRequestMethodArgumentsRequired method
     * Expected result: false
     */
    @Test
    public void areRequestMethodArgumentsRequiredFalseNull() {
        areRequestMethodArgumentsRequired(null, false);
    }

    /**
     * Tests areRequestMethodArgumentsRequired method
     * Expected result: false
     */
    @Test
    public void areRequestMethodArgumentsRequiredFalseInvalidObject() {
        areRequestMethodArgumentsRequired(new Object(), false);
    }

    /**
     * Tests areRequestMethodArgumentsRequired method
     * Expected result: false
     */
    @Test
    public void areRequestMethodArgumentsRequiredFalse() {
        areRequestMethodArgumentsRequired("false", false);
    }
}
