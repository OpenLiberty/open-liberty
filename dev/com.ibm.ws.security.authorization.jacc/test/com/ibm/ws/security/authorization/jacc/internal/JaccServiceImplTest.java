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

package com.ibm.ws.security.authorization.jacc.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.Policy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.EnterpriseBean;
import javax.security.auth.Subject;
import javax.security.jacc.EJBMethodPermission;
import javax.security.jacc.EJBRoleRefPermission;
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
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.authorization.jacc.MethodInfo;
import com.ibm.ws.security.authorization.jacc.RoleInfo;
import com.ibm.ws.security.authorization.jacc.ejb.EJBSecurityPropagator;
import com.ibm.ws.security.authorization.jacc.ejb.EJBSecurityValidator;
import com.ibm.ws.security.authorization.jacc.ejb.EJBService;
import com.ibm.ws.security.authorization.jacc.web.ServletService;
import com.ibm.ws.security.authorization.jacc.web.WebSecurityPropagator;
import com.ibm.ws.security.authorization.jacc.web.WebSecurityValidator;
import com.ibm.ws.webcontainer.security.metadata.SecurityConstraintCollection;
import com.ibm.ws.webcontainer.security.metadata.SecurityMetadata;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.library.Library;
import com.ibm.wsspi.security.authorization.jacc.ProviderService;
import com.ibm.wsspi.webcontainer.metadata.WebModuleMetaData;
import com.ibm.wsspi.webcontainer.webapp.WebAppConfig;

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
    private final ServiceReference<ProviderService> jaccProviderServiceRef = context.mock(ServiceReference.class, "providerServiceRef");
    private final ProviderService jaccProviderService = context.mock(ProviderService.class);
    @SuppressWarnings("unchecked")
    private final ServiceReference<WsLocationAdmin> wsLocationAdminRef = context.mock(ServiceReference.class, "wsLocationAdminRef");
    private final WsLocationAdmin wsLocationAdmin = context.mock(WsLocationAdmin.class);
    private final PolicyConfiguration pc = context.mock(PolicyConfiguration.class);
    private final WebAppConfig wac = context.mock(WebAppConfig.class);
    private final HttpServletRequest req = context.mock(HttpServletRequest.class);
    private final EnterpriseBean eBean = context.mock(EnterpriseBean.class);
    private final Library sl = context.mock(Library.class);
    private final WebModuleMetaData wmmd = context.mock(WebModuleMetaData.class);
    private final SecurityMetadata smd = context.mock(SecurityMetadata.class);
    private final SecurityConstraintCollection scc = context.mock(SecurityConstraintCollection.class);
    private final WebSecurityPropagator wsp = context.mock(WebSecurityPropagator.class);
    private final WebSecurityValidator wsv = context.mock(WebSecurityValidator.class);
    private final EJBSecurityPropagator esp = context.mock(EJBSecurityPropagator.class);
    private final EJBSecurityValidator esv = context.mock(EJBSecurityValidator.class);
    private final EJBService es = context.mock(EJBService.class);
    private final ServletService ss = context.mock(ServletService.class);
    private final List<String> servletList = new ArrayList<String>();
    private final Iterator<?> servletNames = servletList.iterator();
    private final List<String> roles = new ArrayList<String>();

    private final Policy policy = Policy.getPolicy();
    private final PolicyConfigurationFactory pcf = new DummyPolicyConfigurationFactory(pc);
    private final JaccServiceImpl jaccService = new JaccServiceImpl();
    private final ClassLoader scl = ClassLoader.getSystemClassLoader();

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
                allowing(cc).locateService("jaccProviderService", jaccProviderServiceRef);
                will(returnValue(jaccProviderService));
                allowing(cc).locateService("locationAdmin", wsLocationAdminRef);
                will(returnValue(wsLocationAdmin));
                allowing(jaccProviderService).getPolicy();
                will(returnValue(policy));
                allowing(jaccProviderService).getPolicyConfigFactory();
                will(returnValue(pcf));
                allowing(sl).getClassLoader();
                will(returnValue(scl));
            }
        });
        JaccServiceImpl jaccService = new JaccServiceImpl();
        jaccService.setJaccProviderService(jaccProviderServiceRef);
        jaccService.setLocationAdmin(wsLocationAdminRef);
        jaccService.activate(cc);
        assertEquals(JACC_POLICY_PROVIDER_IMPL, System.getProperty(JACC_POLICY_PROVIDER));
        assertEquals(JACC_POLICY_PROVIDER_IMPL, System.getProperty(JACC_POLICY_PROVIDER_EE9));
        assertEquals(JACC_FACTORY_IMPL, System.getProperty(JACC_FACTORY));
        assertEquals(JACC_FACTORY_IMPL, System.getProperty(JACC_FACTORY_EE9));
        jaccService.deactivate(cc);
        jaccService.unsetJaccProviderService(jaccProviderServiceRef);
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
                allowing(cc).locateService("jaccProviderService", jaccProviderServiceRef);
                will(returnValue(jaccProviderService));
                allowing(cc).locateService("locationAdmin", wsLocationAdminRef);
                will(returnValue(wsLocationAdmin));
                allowing(jaccProviderService).getPolicy();
                will(returnValue(policy));
                allowing(jaccProviderService).getPolicyConfigFactory();
                will(returnValue(pcf));
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

        JaccServiceImpl jaccService = new JaccServiceImpl();
        jaccService.setJaccProviderService(jaccProviderServiceRef);
        jaccService.setLocationAdmin(wsLocationAdminRef);
        jaccService.activate(cc);
        assertEquals(JACC_POLICY_PROVIDER_IMPL, System.getProperty(JACC_POLICY_PROVIDER));
        assertEquals(JACC_POLICY_PROVIDER_IMPL, System.getProperty(JACC_POLICY_PROVIDER_EE9));
        assertEquals(JACC_FACTORY_IMPL, System.getProperty(JACC_FACTORY));
        assertEquals(JACC_FACTORY_IMPL, System.getProperty(JACC_FACTORY_EE9));
        jaccService.deactivate(cc);
        jaccService.unsetJaccProviderService(jaccProviderServiceRef);
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

        JaccServiceImpl jaccService = new JaccServiceImpl();
        jaccService.setJaccProviderService(jaccProviderServiceRef);

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

        JaccServiceImpl jaccService = new JaccServiceImpl();
        jaccService.setJaccProviderService(jaccProviderServiceRef);

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
                allowing(cc).locateService("jaccProviderService", jaccProviderServiceRef);
                will(returnValue(jaccProviderService));
//                allowing(cc).locateService("locationAdmin", wsLocationAdminRef);
//                will(returnValue(wsLocationAdmin));
                allowing(jaccProviderService).getPolicy();
                will(returnValue(null));
                allowing(jaccProviderService).getPolicyConfigFactory();
                will(returnValue(pcf));
//                allowing(sl).getClassLoader();
//                will(returnValue(scl));
            }
        });
        JaccServiceImpl jaccService = new JaccServiceImpl();
        jaccService.setJaccProviderService(jaccProviderServiceRef);
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
                allowing(cc).locateService("jaccProviderService", jaccProviderServiceRef);
                will(returnValue(jaccProviderService));
                allowing(cc).locateService("locationAdmin", wsLocationAdminRef);
                will(returnValue(wsLocationAdmin));
                allowing(jaccProviderService).getPolicy();
                will(returnValue(policy));
                allowing(jaccProviderService).getPolicyConfigFactory();
                will(returnValue(null));
                allowing(sl).getClassLoader();
                will(returnValue(scl));
            }
        });
        JaccServiceImpl jaccService = new JaccServiceImpl();
        jaccService.setJaccProviderService(jaccProviderServiceRef);
        jaccService.setLocationAdmin(wsLocationAdminRef);
        jaccService.activate(cc);

        assertFalse(jaccService.loadClasses());
    }

    /**
     * Tests propagateWebSecurity method
     * Expected result: no exception
     */
    @Test
    public void propagateWebConstraintsNull() {
        try {
            JaccServiceImpl jaccService = new JaccServiceImpl();
            jaccService.propagateWebConstraints(null, null, null);
        } catch (Exception e) {
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests propagateWebConstraints method
     * Expected result: no exception even invalid object was supplied
     */
    @Test
    public void propagateWebConstraintsInvalidObject() {
        try {
            JaccServiceImpl jaccService = new JaccServiceImpl();
            jaccService.propagateWebConstraints("application", "module", "abc");
        } catch (Exception e) {
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests propagateWebConstraints method
     * Expected result: no exception.
     */
    @Test
    public void propagateWebConstraintsNormal() {
        final String directory = "/wlp/test";
        final String name = "jaccServer";
        context.checking(new Expectations() {
            {
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER);
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER_EE9);
                will(returnValue(JACC_POLICY_PROVIDER_IMPL));
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY);
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY_EE9);
                will(returnValue(JACC_FACTORY_IMPL));
                allowing(cc).locateService("jaccProviderService", jaccProviderServiceRef);
                will(returnValue(jaccProviderService));
                allowing(cc).locateService("locationAdmin", wsLocationAdminRef);
                will(returnValue(wsLocationAdmin));
                allowing(jaccProviderService).getPolicy();
                will(returnValue(policy));
                allowing(jaccProviderService).getPolicyConfigFactory();
                will(returnValue(pcf));
                allowing(wsLocationAdmin).resolveString("${wlp.user.dir}");
                will(returnValue(directory));
                allowing(wsLocationAdmin).getServerName();
                will(returnValue(name));
                allowing(wsp).propagateWebConstraints(with(any(PolicyConfigurationFactory.class)), with(any(String.class)), with(any(String.class)));
            }
        });
        try {
            JaccServiceImpl jaccService = new JaccServiceImpl();
            jaccService.setJaccProviderService(jaccProviderServiceRef);
            jaccService.setLocationAdmin(wsLocationAdminRef);
            jaccService.activate(cc);
            jaccService.propagateWebConstraints(wsp, "application", "module", "abc");
        } catch (Exception e) {
            e.printStackTrace();
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
            JaccServiceImpl jaccService = new JaccServiceImpl();
            assertTrue(jaccService.isSSLRequired(appName, moduleName, uriName, method, new Object()));
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
            JaccServiceImpl jaccService = new JaccServiceImpl();
            assertTrue(jaccService.isAccessExcluded(appName, moduleName, uriName, method, new Object()));
        } catch (Exception e) {
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests checkDataConstraints method
     * Expected result: true if there is some error in the parameter.
     */
    @Test
    public void checkDataConstraintsInvalid() {
        final String appName = "applicationName";
        final String moduleName = "moduleName";
        final String directory = "/wlp/test";
        final String name = "jaccServer";
        final String uriName = "/test/index.html";
        final String method = "GET";

        context.checking(new Expectations() {
            {
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER);
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER_EE9);
                will(returnValue(JACC_POLICY_PROVIDER_IMPL));
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY);
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY_EE9);
                will(returnValue(JACC_FACTORY_IMPL));
                allowing(cc).locateService("jaccProviderService", jaccProviderServiceRef);
                will(returnValue(jaccProviderService));
                allowing(cc).locateService("locationAdmin", wsLocationAdminRef);
                will(returnValue(wsLocationAdmin));
                allowing(jaccProviderService).getPolicy();
                will(returnValue(policy));
                allowing(jaccProviderService).getPolicyConfigFactory();
                will(returnValue(pcf));
                allowing(wsLocationAdmin).resolveString("${wlp.user.dir}");
                will(returnValue(directory));
                allowing(wsLocationAdmin).getServerName();
                will(returnValue(name));
                allowing(wsv).checkDataConstraints(with(any(String.class)), with(any(Object.class)), with(any(WebUserDataPermission.class)));
                will(returnValue(false));
            }
        });

        try {
            JaccServiceImpl jaccService = new JaccServiceImpl();
            jaccService.setJaccProviderService(jaccProviderServiceRef);
            jaccService.setLocationAdmin(wsLocationAdminRef);
            jaccService.activate(cc);
            assertFalse(jaccService.checkDataConstraints(wsv, appName, moduleName, uriName, method, new Object(), null));
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
        context.checking(new Expectations() {
            {
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER);
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER_EE9);
                will(returnValue(JACC_POLICY_PROVIDER_IMPL));
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY);
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY_EE9);
                will(returnValue(JACC_FACTORY_IMPL));
                allowing(cc).locateService("jaccProviderService", jaccProviderServiceRef);
                will(returnValue(jaccProviderService));
                allowing(cc).locateService("locationAdmin", wsLocationAdminRef);
                will(returnValue(wsLocationAdmin));
                allowing(jaccProviderService).getPolicy();
                will(returnValue(policy));
                allowing(jaccProviderService).getPolicyConfigFactory();
                will(returnValue(pcf));
                allowing(wsLocationAdmin).resolveString("${wlp.user.dir}");
                will(returnValue(directory));
                allowing(wsLocationAdmin).getServerName();
                will(returnValue(name));
            }
        });

        try {
            JaccServiceImpl jaccService = new JaccServiceImpl();
            jaccService.setJaccProviderService(jaccProviderServiceRef);
            jaccService.setLocationAdmin(wsLocationAdminRef);
            jaccService.activate(cc);
            assertTrue(jaccService.isSSLRequired(appName, moduleName, uriName, method, req));
            // this is for null check
            assertTrue(jaccService.isSSLRequired(appName, moduleName, uriName, method, req));
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
            JaccServiceImpl jaccService = new JaccServiceImpl();
            assertFalse(jaccService.isAuthorized(appName, moduleName, uriName, method, req, subject));
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
        context.checking(new Expectations() {
            {
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER);
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER_EE9);
                will(returnValue(JACC_POLICY_PROVIDER_IMPL));
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY);
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY_EE9);
                will(returnValue(JACC_FACTORY_IMPL));
                allowing(cc).locateService("jaccProviderService", jaccProviderServiceRef);
                will(returnValue(jaccProviderService));
                allowing(cc).locateService("locationAdmin", wsLocationAdminRef);
                will(returnValue(wsLocationAdmin));
                allowing(jaccProviderService).getPolicy();
                will(returnValue(policy));
                allowing(jaccProviderService).getPolicyConfigFactory();
                will(returnValue(pcf));
                allowing(wsLocationAdmin).resolveString("${wlp.user.dir}");
                will(returnValue(directory));
                allowing(wsLocationAdmin).getServerName();
                will(returnValue(name));
                allowing(wsv).checkResourceConstraints(with(any(String.class)), with(any(Object.class)), with(any(WebResourcePermission.class)), with(any(Subject.class)));
                will(returnValue(false));
            }
        });

        try {
            JaccServiceImpl jaccService = new JaccServiceImpl();
            jaccService.setJaccProviderService(jaccProviderServiceRef);
            jaccService.setLocationAdmin(wsLocationAdminRef);
            jaccService.activate(cc);
            assertFalse(jaccService.isAuthorized(wsv, appName, moduleName, uriName, method, req, subject));
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
            JaccServiceImpl jaccService = new JaccServiceImpl();
            assertFalse(jaccService.isSubjectInRole(appName, moduleName, servletName, role, req, subject));
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
        context.checking(new Expectations() {
            {
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER);
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER_EE9);
                will(returnValue(JACC_POLICY_PROVIDER_IMPL));
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY);
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY_EE9);
                will(returnValue(JACC_FACTORY_IMPL));
                allowing(cc).locateService("jaccProviderService", jaccProviderServiceRef);
                will(returnValue(jaccProviderService));
                allowing(cc).locateService("locationAdmin", wsLocationAdminRef);
                will(returnValue(wsLocationAdmin));
                allowing(jaccProviderService).getPolicy();
                will(returnValue(policy));
                allowing(jaccProviderService).getPolicyConfigFactory();
                will(returnValue(pcf));
                allowing(wsLocationAdmin).resolveString("${wlp.user.dir}");
                will(returnValue(directory));
                allowing(wsLocationAdmin).getServerName();
                will(returnValue(name));
                allowing(wsv).checkResourceConstraints(with(any(String.class)), with(any(Object.class)), with(any(WebRoleRefPermission.class)), with(any(Subject.class)));
                will(returnValue(true));
            }
        });

        try {
            JaccServiceImpl jaccService = new JaccServiceImpl();
            jaccService.setJaccProviderService(jaccProviderServiceRef);
            jaccService.setLocationAdmin(wsLocationAdminRef);
            jaccService.activate(cc);
            assertTrue(jaccService.isSubjectInRole(wsv, appName, moduleName, servletName, role, req, subject));
        } catch (Exception e) {
            fail("Exception is caught : " + e);
        }
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
            JaccServiceImpl jaccService = new JaccServiceImpl();
            jaccService.propagateEJBRoles(appName, moduleName, beanName, null, null);
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

        context.checking(new Expectations() {
            {
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER);
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER_EE9);
                will(returnValue(JACC_POLICY_PROVIDER_IMPL));
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY);
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY_EE9);
                will(returnValue(JACC_FACTORY_IMPL));
                allowing(cc).locateService("jaccProviderService", jaccProviderServiceRef);
                will(returnValue(jaccProviderService));
                allowing(cc).locateService("locationAdmin", wsLocationAdminRef);
                will(returnValue(wsLocationAdmin));
                allowing(jaccProviderService).getPolicy();
                will(returnValue(policy));
                allowing(jaccProviderService).getPolicyConfigFactory();
                will(returnValue(pcf));
                allowing(wsLocationAdmin).resolveString("${wlp.user.dir}");
                will(returnValue(directory));
                allowing(wsLocationAdmin).getServerName();
                will(returnValue(name));
                allowing(esp).propagateEJBRoles(with(any(String.class)), with(any(String.class)), with(any(String.class)), with(any(HashMap.class)), with(any(HashMap.class)));
            }
        });
        try {
            JaccServiceImpl jaccService = new JaccServiceImpl();
            jaccService.setJaccProviderService(jaccProviderServiceRef);
            jaccService.setLocationAdmin(wsLocationAdminRef);
            jaccService.activate(cc);
            jaccService.propagateEJBRoles(esp, appName, moduleName, beanName, rl, mm);
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
            JaccServiceImpl jaccService = new JaccServiceImpl();
            assertFalse(jaccService.isAuthorized(appName, moduleName, beanName, methodName, methodInterface, ms1, methodParameters, eBean, subject));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests isAuthorized method
     * Expected result: false if there is no permission defined.
     */
    @SuppressWarnings("unchecked")
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
        final String method = "GET";
        final Subject subject = new Subject();
        context.checking(new Expectations() {
            {
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER);
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER_EE9);
                will(returnValue(JACC_POLICY_PROVIDER_IMPL));
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY);
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY_EE9);
                will(returnValue(JACC_FACTORY_IMPL));
                allowing(cc).locateService("jaccProviderService", jaccProviderServiceRef);
                will(returnValue(jaccProviderService));
                allowing(cc).locateService("locationAdmin", wsLocationAdminRef);
                will(returnValue(wsLocationAdmin));
                allowing(jaccProviderService).getPolicy();
                will(returnValue(policy));
                allowing(jaccProviderService).getPolicyConfigFactory();
                will(returnValue(pcf));
                allowing(wsLocationAdmin).resolveString("${wlp.user.dir}");
                will(returnValue(directory));
                allowing(wsLocationAdmin).getServerName();
                will(returnValue(name));
                allowing(req).getMethod();
                will(returnValue(method));
                allowing(esv).checkResourceConstraints(with(any(String.class)), with(any(ArrayList.class)), with(any(EnterpriseBean.class)), with(any(EJBMethodPermission.class)),
                                                       with(any(Subject.class)));
                will(returnValue(true));
            }
        });

        try {
            JaccServiceImpl jaccService = new JaccServiceImpl();
            jaccService.setJaccProviderService(jaccProviderServiceRef);
            jaccService.setLocationAdmin(wsLocationAdminRef);
            jaccService.activate(cc);
            assertTrue(jaccService.isAuthorized(esv, appName, moduleName, beanName, methodName, methodInterface, ms2, mp, eBean, subject));
            // different method signature
            assertTrue(jaccService.isAuthorized(esv, appName, moduleName, beanName, methodName, methodInterface, ms3, mp, eBean, subject));
            assertTrue(jaccService.isAuthorized(esv, appName, moduleName, beanName, methodName, methodInterface, ms4, mp, eBean, subject));
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
            JaccServiceImpl jaccService = new JaccServiceImpl();
            assertFalse(jaccService.isSubjectInRole(appName, moduleName, beanName, methodName, mp, role, eBean, subject));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests isSubjectInRole method
     * Expected result: false if there is no permission defined.
     */
    @SuppressWarnings("unchecked")
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
        context.checking(new Expectations() {
            {
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER);
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER_EE9);
                will(returnValue(JACC_POLICY_PROVIDER_IMPL));
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY);
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY_EE9);
                will(returnValue(JACC_FACTORY_IMPL));
                allowing(cc).locateService("jaccProviderService", jaccProviderServiceRef);
                will(returnValue(jaccProviderService));
                allowing(cc).locateService("locationAdmin", wsLocationAdminRef);
                will(returnValue(wsLocationAdmin));
                allowing(jaccProviderService).getPolicy();
                will(returnValue(policy));
                allowing(jaccProviderService).getPolicyConfigFactory();
                will(returnValue(pcf));
                allowing(wsLocationAdmin).resolveString("${wlp.user.dir}");
                will(returnValue(directory));
                allowing(wsLocationAdmin).getServerName();
                will(returnValue(name));
                allowing(esv).checkResourceConstraints(with(any(String.class)), with(any(ArrayList.class)), with(any(EnterpriseBean.class)), with(any(EJBRoleRefPermission.class)),
                                                       with(any(Subject.class)));
                will(returnValue(true));
            }
        });

        try {
            JaccServiceImpl jaccService = new JaccServiceImpl();
            jaccService.setJaccProviderService(jaccProviderServiceRef);
            jaccService.setLocationAdmin(wsLocationAdminRef);
            jaccService.activate(cc);
            assertTrue(jaccService.isSubjectInRole(esv, appName, moduleName, beanName, methodName, mp, role, eBean, subject));
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
        final String value = "true";
        context.checking(new Expectations() {
            {
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER);
                allowing(jaccProviderServiceRef).getProperty(JACC_POLICY_PROVIDER_EE9);
                will(returnValue(JACC_POLICY_PROVIDER_IMPL));
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY);
                allowing(jaccProviderServiceRef).getProperty(JACC_FACTORY_EE9);
                will(returnValue(JACC_FACTORY_IMPL));
                allowing(cc).locateService("jaccProviderService", jaccProviderServiceRef);
                will(returnValue(jaccProviderService));
                allowing(cc).locateService("locationAdmin", wsLocationAdminRef);
                will(returnValue(wsLocationAdmin));
                allowing(jaccProviderService).getPolicy();
                will(returnValue(policy));
                allowing(jaccProviderService).getPolicyConfigFactory();
                will(returnValue(pcf));
                allowing(jaccProviderServiceRef).getProperty(JACC_EJB_METHOD_ARGUMENT);
                will(returnValue(value));
            }
        });

        try {
            JaccServiceImpl jaccService = new JaccServiceImpl();
            jaccService.setJaccProviderService(jaccProviderServiceRef);
            jaccService.setLocationAdmin(wsLocationAdminRef);
            jaccService.activate(cc);
            assertTrue(jaccService.areRequestMethodArgumentsRequired());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests areRequestMethodArgumentsRequired method
     * Expected result: false
     */
    @Test
    public void areRequestMethodArgumentsRequiredFalseNull() {
        context.checking(new Expectations() {
            {
                allowing(jaccProviderServiceRef).getProperty(JACC_EJB_METHOD_ARGUMENT);
                will(returnValue(null));
            }
        });

        try {
            JaccServiceImpl jaccService = new JaccServiceImpl();
            assertFalse(jaccService.areRequestMethodArgumentsRequired());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests areRequestMethodArgumentsRequired method
     * Expected result: false
     */
    @Test
    public void areRequestMethodArgumentsRequiredFalseInvalidObject() {
        context.checking(new Expectations() {
            {
                allowing(jaccProviderServiceRef).getProperty(JACC_EJB_METHOD_ARGUMENT);
                will(returnValue(new Object()));
            }
        });

        try {
            JaccServiceImpl jaccService = new JaccServiceImpl();
            assertFalse(jaccService.areRequestMethodArgumentsRequired());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests areRequestMethodArgumentsRequired method
     * Expected result: false
     */
    @Test
    public void areRequestMethodArgumentsRequiredFalse() {
        final String value = "false";
        context.checking(new Expectations() {
            {
                allowing(jaccProviderServiceRef).getProperty(JACC_EJB_METHOD_ARGUMENT);
                will(returnValue(value));
            }
        });

        try {
            JaccServiceImpl jaccService = new JaccServiceImpl();
            assertFalse(jaccService.areRequestMethodArgumentsRequired());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests getEsp method
     * Expected result: return an object when service is not null.
     */
    @Test
    public void getEsp() {
        context.checking(new Expectations() {
            {
                allowing(es).getPropagator();
                will(returnValue(esp));
            }
        });

        try {
            JaccServiceImpl jaccService = new JaccServiceImpl();
            assertEquals(jaccService.getEsp(es), esp);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests getEsv method
     * Expected result: return an object when service is not null.
     */
    @Test
    public void getEsv() {
        context.checking(new Expectations() {
            {
                allowing(es).getValidator();
                will(returnValue(esv));
            }
        });

        try {
            JaccServiceImpl jaccService = new JaccServiceImpl();
            assertEquals(jaccService.getEsv(es), esv);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests getWsp method
     * Expected result: return an object when service is not null.
     */
    @Test
    public void getWsp() {
        context.checking(new Expectations() {
            {
                allowing(ss).getPropagator();
                will(returnValue(wsp));
            }
        });

        try {
            JaccServiceImpl jaccService = new JaccServiceImpl();
            assertEquals(jaccService.getWsp(ss), wsp);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception is caught : " + e);
        }
    }

    /**
     * Tests getWsv method
     * Expected result: return an object when service is not null.
     */
    @Test
    public void getWsv() {
        context.checking(new Expectations() {
            {
                allowing(ss).getValidator();
                will(returnValue(wsv));
            }
        });

        try {
            JaccServiceImpl jaccService = new JaccServiceImpl();
            assertEquals(jaccService.getWsv(ss), wsv);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception is caught : " + e);
        }
    }

}
