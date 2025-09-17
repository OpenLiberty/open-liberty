/*******************************************************************************
 * Copyright (c) 2015, 2025 IBM Corporation and others.
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

package com.ibm.ws.security.authorization.jacc.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.security.Policy;

import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyConfigurationFactory;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.authorization.jacc.PolicyConfigurationManager;
import com.ibm.ws.security.authorization.jacc.common.PolicyConfigurationManagerImpl;
import com.ibm.ws.security.authorization.jacc.common.PolicyProxy;
import com.ibm.ws.security.authorization.jacc.common.ProviderServiceProxy;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.library.Library;
import com.ibm.wsspi.security.authorization.jacc.ProviderService;

import io.openliberty.security.authorization.jacc.internal.proxy.ProviderServiceProxyImpl;
import io.openliberty.security.authorization.jacc.internal.proxy.ProxyTestUtil;
import test.common.SharedOutputManager;

public class JaccServiceImplTest {
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
    private final Library sl = context.mock(Library.class);

    private final Policy policy = Policy.getPolicy();
    private final PolicyProxy policyProxy = context.mock(PolicyProxy.class);
    private final PolicyConfigurationFactory pcf = new DummyPolicyConfigurationFactory(pc);
    private final PolicyConfigurationManager pcm = new PolicyConfigurationManagerImpl();
    private final ClassLoader scl = ClassLoader.getSystemClassLoader();

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
     * Tests initialization and clean up code
     */
    @Test
    public void initializationNormal() {

        context.checking(new Expectations() {
            {
                allowing(jaccProviderServiceRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(jaccProviderServiceRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER);
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER_EE9);
                will(returnValue(JACC_POLICY_PROVIDER_IMPL));
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY);
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY_EE9);
                will(returnValue(JACC_FACTORY_IMPL));
                allowing(wsLocationAdminRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(wsLocationAdminRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER);
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER_EE9);
                will(returnValue(null));
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY);
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY_EE9);
                will(returnValue(null));
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
                allowing(sl).getClassLoader();
                will(returnValue(scl));
            }
        });
        JaccServiceImpl jaccService = new JaccServiceImpl(pcm);
        jaccService.setJaccProviderServiceProxy(jaccProviderServiceProxyRef);
        ProviderServiceProxyImpl providerServiceProxy = new ProviderServiceProxyImpl();
        ProxyTestUtil.setProviderService(providerServiceProxy, jaccProviderServiceRef);
        jaccService.setLocationAdmin(wsLocationAdminRef);
        jaccService.activate(cc);
        assertEquals(JACC_POLICY_PROVIDER_IMPL, System.getProperty(JACC_POLICY_PROVIDER));
        assertEquals(JACC_POLICY_PROVIDER_IMPL, System.getProperty(JACC_POLICY_PROVIDER_EE9));
        assertEquals(JACC_FACTORY_IMPL, System.getProperty(JACC_FACTORY));
        assertEquals(JACC_FACTORY_IMPL, System.getProperty(JACC_FACTORY_EE9));
        jaccService.deactivate(cc);
        ProxyTestUtil.unsetProviderService(providerServiceProxy, jaccProviderServiceRef);
        jaccService.unsetJaccProviderServiceProxy(jaccProviderServiceProxyRef);
        jaccService.unsetLocationAdmin(wsLocationAdminRef);
    }

    /**
     * Tests initialization and clean up code
     */
    @Test
    public void initializationRestoreSystemProps() {

        context.checking(new Expectations() {
            {
                allowing(jaccProviderServiceRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(jaccProviderServiceRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER);
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER_EE9);
                will(returnValue(JACC_POLICY_PROVIDER_IMPL));
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY);
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY_EE9);
                will(returnValue(JACC_FACTORY_IMPL));
                allowing(wsLocationAdminRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(wsLocationAdminRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER);
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER_EE9);
                will(returnValue(null));
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY);
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY_EE9);
                will(returnValue(null));
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
                allowing(sl).getClassLoader();
                will(returnValue(scl));
            }
        });
        String tmpPp = "TempPolicyProvider";
        String tmpFn = "TempFactoryName";
        System.setProperty(JACC_POLICY_PROVIDER, tmpPp);
        System.setProperty(JACC_POLICY_PROVIDER_EE9, tmpPp);
        System.setProperty(JACC_FACTORY, tmpFn);
        System.setProperty(JACC_FACTORY_EE9, tmpFn);

        JaccServiceImpl jaccService = new JaccServiceImpl(pcm);
        jaccService.setJaccProviderServiceProxy(jaccProviderServiceProxyRef);
        ProviderServiceProxyImpl providerServiceProxy = new ProviderServiceProxyImpl();
        ProxyTestUtil.setProviderService(providerServiceProxy, jaccProviderServiceRef);
        jaccService.setLocationAdmin(wsLocationAdminRef);
        jaccService.activate(cc);
        assertEquals(JACC_POLICY_PROVIDER_IMPL, System.getProperty(JACC_POLICY_PROVIDER));
        assertEquals(JACC_POLICY_PROVIDER_IMPL, System.getProperty(JACC_POLICY_PROVIDER_EE9));
        assertEquals(JACC_FACTORY_IMPL, System.getProperty(JACC_FACTORY));
        assertEquals(JACC_FACTORY_IMPL, System.getProperty(JACC_FACTORY_EE9));
        jaccService.deactivate(cc);
        ProxyTestUtil.unsetProviderService(providerServiceProxy, jaccProviderServiceRef);
        jaccService.unsetJaccProviderServiceProxy(jaccProviderServiceProxyRef);
        jaccService.unsetLocationAdmin(wsLocationAdminRef);
        assertEquals(tmpPp, System.getProperty(JACC_POLICY_PROVIDER));
        assertEquals(tmpPp, System.getProperty(JACC_POLICY_PROVIDER_EE9));
        assertEquals(tmpFn, System.getProperty(JACC_FACTORY));
        assertEquals(tmpFn, System.getProperty(JACC_FACTORY_EE9));
    }

    /**
     * Tests initializeSystemProperties method by invoking setJaccProviderService method
     * expect result. System properties stay as it is.
     */
    @Test
    public void initializeSystemPropertiesSameSystemPolicy() {
        context.checking(new Expectations() {
            {
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER);
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER_EE9);
                will(returnValue(null));
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY);
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY_EE9);
                will(returnValue(null));
            }
        });
        String tmpPp = "TempPolicyProvider";
        String tmpFn = "TempFactoryName";

        System.setProperty(JACC_POLICY_PROVIDER, tmpPp);
        System.setProperty(JACC_POLICY_PROVIDER_EE9, tmpPp);
        System.setProperty(JACC_FACTORY, tmpFn);
        System.setProperty(JACC_FACTORY_EE9, tmpFn);

        JaccServiceImpl jaccService = new JaccServiceImpl(pcm);
        jaccService.setJaccProviderServiceProxy(jaccProviderServiceProxyRef);

        assertEquals(tmpPp, System.getProperty(JACC_POLICY_PROVIDER));
        assertEquals(tmpPp, System.getProperty(JACC_POLICY_PROVIDER_EE9));
        assertEquals(tmpFn, System.getProperty(JACC_FACTORY));
        assertEquals(tmpFn, System.getProperty(JACC_FACTORY_EE9));
    }

    /**
     * Tests initializeSystemProperties method by invoking setJaccProviderService method
     * expect result. Error
     */
    @Test
    public void initializeSystemPropertiesNoProperties() {
        context.checking(new Expectations() {
            {
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER);
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER_EE9);
                will(returnValue(null));
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY);
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY_EE9);
                will(returnValue(null));
            }
        });
        System.clearProperty(JACC_POLICY_PROVIDER);
        System.clearProperty(JACC_POLICY_PROVIDER_EE9);
        System.clearProperty(JACC_FACTORY);
        System.clearProperty(JACC_FACTORY_EE9);

        JaccServiceImpl jaccService = new JaccServiceImpl(pcm);
        jaccService.setJaccProviderServiceProxy(jaccProviderServiceProxyRef);

        assertNull(System.getProperty(JACC_POLICY_PROVIDER));
        assertNull(System.getProperty(JACC_POLICY_PROVIDER_EE9));
        assertNull(System.getProperty(JACC_FACTORY));
        assertNull(System.getProperty(JACC_FACTORY_EE9));

    }

    /**
     * Tests loadClasses method
     * Expected result: return false
     */
    @Test
    public void loadClassesNullPolicy() {
        context.checking(new Expectations() {
            {
//                allowing(jaccProviderServiceRef).getProperty(Constants.SERVICE_ID);
//                will(returnValue(0L));
//                allowing(jaccProviderServiceRef).getProperty(Constants.SERVICE_RANKING);
//                will(returnValue(0));
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER);
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER_EE9);
                will(returnValue(JACC_POLICY_PROVIDER_IMPL));
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY);
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY_EE9);
                will(returnValue(JACC_FACTORY_IMPL));
//                allowing(wsLocationAdminRef).getProperty(Constants.SERVICE_ID);
//                will(returnValue(0L));
//                allowing(wsLocationAdminRef).getProperty(Constants.SERVICE_RANKING);
//                will(returnValue(0));
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER);
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER_EE9);
                will(returnValue(null));
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY);
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY_EE9);
                will(returnValue(null));
                allowing(cc).locateService("jaccProviderServiceProxy", jaccProviderServiceProxyRef);
                will(returnValue(jaccProviderServiceProxy));
                allowing(cc).locateService("jaccProviderService", jaccProviderServiceRef);
                will(returnValue(jaccProviderService));
//                allowing(cc).locateService("locationAdmin", wsLocationAdminRef);
//                will(returnValue(wsLocationAdmin));
                allowing(jaccProviderServiceProxy).getPolicyProxy(pcm);
                will(returnValue(null));
                allowing(jaccProviderServiceProxy).getPolicyConfigFactory();
                will(returnValue(pcf));
                allowing(jaccProviderService).getPolicy();
                will(returnValue(null));
                allowing(jaccProviderService).getPolicyConfigFactory();
                will(returnValue(pcf));
                allowing(jaccProviderServiceProxy).getPolicyName();
                will(returnValue(null));
                allowing(jaccProviderServiceProxy).getFactoryName();
                will(returnValue(JACC_FACTORY_IMPL));
//                allowing(sl).getClassLoader();
//                will(returnValue(scl));
            }
        });
        JaccServiceImpl jaccService = new JaccServiceImpl(pcm);
        jaccService.setJaccProviderServiceProxy(jaccProviderServiceProxyRef);
        ProviderServiceProxyImpl providerServiceProxy = new ProviderServiceProxyImpl();
        ProxyTestUtil.setProviderService(providerServiceProxy, jaccProviderServiceRef);
        jaccService.setLocationAdmin(wsLocationAdminRef);
        jaccService.activate(cc);

        assertFalse(jaccService.loadClasses());
    }

    /**
     * Tests loadClasses method
     * Expected result: return false
     */
    @Test
    public void loadClassesNullFactory() {
        context.checking(new Expectations() {
            {
                allowing(jaccProviderServiceRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(jaccProviderServiceRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER);
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER_EE9);
                will(returnValue(JACC_POLICY_PROVIDER_IMPL));
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY);
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY_EE9);
                will(returnValue(JACC_FACTORY_IMPL));
                allowing(wsLocationAdminRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(wsLocationAdminRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER);
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER_EE9);
                will(returnValue(null));
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY);
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY_EE9);
                will(returnValue(null));
                allowing(cc).locateService("jaccProviderServiceProxy", jaccProviderServiceProxyRef);
                will(returnValue(jaccProviderServiceProxy));
                allowing(cc).locateService("jaccProviderService", jaccProviderServiceRef);
                will(returnValue(jaccProviderService));
                allowing(cc).locateService("locationAdmin", wsLocationAdminRef);
                will(returnValue(wsLocationAdmin));
                allowing(jaccProviderServiceProxy).getPolicyProxy(pcm);
                will(returnValue(policyProxy));
                allowing(jaccProviderServiceProxy).getPolicyConfigFactory();
                will(returnValue(null));
                allowing(jaccProviderService).getPolicy();
                will(returnValue(policy));
                allowing(policyProxy).setPolicy();
                allowing(policyProxy).refresh();
                allowing(jaccProviderService).getPolicyConfigFactory();
                will(returnValue(null));
                allowing(jaccProviderServiceProxy).getPolicyName();
                will(returnValue(JACC_POLICY_PROVIDER_IMPL));
                allowing(jaccProviderServiceProxy).getFactoryName();
                will(returnValue(null));
                allowing(sl).getClassLoader();
                will(returnValue(scl));
            }
        });
        JaccServiceImpl jaccService = new JaccServiceImpl(pcm);
        jaccService.setJaccProviderServiceProxy(jaccProviderServiceProxyRef);
        ProviderServiceProxyImpl providerServiceProxy = new ProviderServiceProxyImpl();
        ProxyTestUtil.setProviderService(providerServiceProxy, jaccProviderServiceRef);
        jaccService.setLocationAdmin(wsLocationAdminRef);
        jaccService.activate(cc);

        assertFalse(jaccService.loadClasses());
    }

}
