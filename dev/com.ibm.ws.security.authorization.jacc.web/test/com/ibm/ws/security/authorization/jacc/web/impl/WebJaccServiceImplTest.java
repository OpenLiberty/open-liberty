/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package com.ibm.ws.security.authorization.jacc.web.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.Policy;

import javax.security.auth.Subject;
import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyConfigurationFactory;
import javax.security.jacc.WebResourcePermission;
import javax.security.jacc.WebRoleRefPermission;
import javax.security.jacc.WebUserDataPermission;
import javax.servlet.http.HttpServletRequest;

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
import com.ibm.ws.security.authorization.jacc.PolicyConfigurationManager;
import com.ibm.ws.security.authorization.jacc.common.PolicyConfigurationManagerImpl;
import com.ibm.ws.security.authorization.jacc.common.PolicyProxy;
import com.ibm.ws.security.authorization.jacc.common.ProviderServiceProxy;
import com.ibm.ws.security.authorization.jacc.internal.JaccServiceImpl;
import com.ibm.ws.security.authorization.jacc.internal.JaccServiceTestUtil;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.security.authorization.jacc.ProviderService;

import test.common.SharedOutputManager;

public class WebJaccServiceImplTest {
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
    private final HttpServletRequest req = context.mock(HttpServletRequest.class);
    private final ServiceReference<JaccService> jaccServiceRef = context.mock(ServiceReference.class, "jaccServiceRef");
    private final WebJaccServiceImpl webJaccService = new WebJaccServiceImpl();

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
     * Tests propagateWebSecurity method
     * Expected result: no exception
     */
    @Test
    public void propagateWebConstraintsNull() {
        try {
            webJaccService.propagateWebConstraints(null, null, null);
        } catch (Exception e) {
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests isSSLRequire method
     * Expected result: true if there is some error in the parameter.
     */
    @Test
    public void isSSLRequired() {
        final String appName = "applicationName";
        final String moduleName = "moduleName";
        final String uriName = "/test/index.html";
        final String method = "GET";

        try {
            assertTrue(webJaccService.isSSLRequired(appName, moduleName, uriName, method, req));
        } catch (Exception e) {
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests isAccessExcluded method
     * Expected result: true if there is some error in the parameter.
     */
    @Test
    public void isAccessExcluded() {
        final String appName = "applicationName";
        final String moduleName = "moduleName";
        final String uriName = "/test/index.html";
        final String method = "GET";

        try {
            assertTrue(webJaccService.isAccessExcluded(appName, moduleName, uriName, method, req));
        } catch (Exception e) {
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests isSSLRequire method
     * Expected result: true if there is no permission defined.
     */
    @Test
    public void isSSlRequiredValid() {
        final String appName = "applicationName";
        final String moduleName = "moduleName";
        final String directory = "/wlp/test";
        final String name = "jaccServer";
        final String uriName = "/test/index.html";
        final String method = "GET";
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
                allowing(policyProxy).implies(with(any(String.class)), with(aNull(Subject.class)), with(any(WebUserDataPermission.class)));
                will(returnValue(false));
            }
        });

        try {
            JaccServiceTestUtil.initJaccService(jaccService, jaccProviderServiceProxyRef, jaccProviderServiceRef, wsLocationAdminRef, cc);
            webJaccService.setJaccService(jaccServiceRef);
            webJaccService.activate(cc);
            assertTrue(webJaccService.isSSLRequired(appName, moduleName, uriName, method, req));
        } catch (Exception e) {
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests isAuthorized method
     * Expected result: false if there is no permission defined.
     */
    @Test
    public void isAuthorizedWeb() {
        final String appName = "applicationName";
        final String moduleName = "moduleName";
        final String uriName = "/test/*";
        final String method = "GET";
        final Subject subject = new Subject();

        try {
            assertFalse(webJaccService.isAuthorized(appName, moduleName, uriName, method, req, subject));
        } catch (Exception e) {
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests isAuthorized method
     * Expected result: false if there is no permission defined.
     */
    @Test
    public void isAuthorizedWebDataValid() {
        final String appName = "applicationName";
        final String moduleName = "moduleName";
        final String directory = "/wlp/test";
        final String name = "jaccServer";
        final String uriName = "/test/*";
        final String method = "GET";
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
                allowing(policyProxy).implies(with(any(String.class)), with(any(Subject.class)), with(any(WebResourcePermission.class)));
                will(returnValue(false));
            }
        });

        try {
            JaccServiceTestUtil.initJaccService(jaccService, jaccProviderServiceProxyRef, jaccProviderServiceRef, wsLocationAdminRef, cc);
            webJaccService.setJaccService(jaccServiceRef);
            webJaccService.activate(cc);
            assertFalse(webJaccService.isAuthorized(appName, moduleName, uriName, method, req, subject));
        } catch (Exception e) {
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests isSubjectInRole method
     * Expected result: false if there is no permission defined.
     */
    @Test
    public void isWebSubjectInRole() {
        final String appName = "applicationName";
        final String moduleName = "moduleName";
        final String servletName = "servlet.class";
        final String role = "UserRole";
        final Subject subject = new Subject();
        try {
            assertFalse(webJaccService.isSubjectInRole(appName, moduleName, servletName, role, req, subject));
        } catch (Exception e) {
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests isSubjectInRole method
     * Expected result: false if there is no permission defined.
     */
    @Test
    public void isWebSubjectInRoleValid() {
        final String appName = "applicationName";
        final String moduleName = "moduleName";
        final String directory = "/wlp/test";
        final String name = "jaccServer";
        final String servletName = "servlet.class";
        final String role = "UserRole";
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
                allowing(policyProxy).implies(with(any(String.class)), with(any(Subject.class)), with(any(WebRoleRefPermission.class)));
                will(returnValue(true));
            }
        });

        try {
            JaccServiceTestUtil.initJaccService(jaccService, jaccProviderServiceProxyRef, jaccProviderServiceRef, wsLocationAdminRef, cc);
            webJaccService.setJaccService(jaccServiceRef);
            webJaccService.activate(cc);
            assertTrue(webJaccService.isSubjectInRole(appName, moduleName, servletName, role, req, subject));
        } catch (Exception e) {
            fail("Exception is caught : " + e);
        }
    }
}
