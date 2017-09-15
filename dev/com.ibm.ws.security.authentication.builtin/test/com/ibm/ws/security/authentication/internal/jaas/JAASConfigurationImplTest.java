/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.internal.jaas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

import com.ibm.ws.kernel.boot.security.LoginModuleProxy;
import com.ibm.ws.security.jaas.common.JAASLoginContextEntry;
import com.ibm.ws.security.jaas.common.JAASLoginModuleConfig;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

@SuppressWarnings("unchecked")
public class JAASConfigurationImplTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    protected final JAASLoginContextEntry jaasLoginContextEntry = mock.mock(JAASLoginContextEntry.class);
    protected final ServiceReference<JAASLoginContextEntry> jaasLoginContextEntryRef = mock.mock(ServiceReference.class, "jaasLoginContextEntryRef");
    protected final ConcurrentServiceReferenceMap<String, JAASLoginContextEntry> jaasLoginContextEntries = new ConcurrentServiceReferenceMap<String, JAASLoginContextEntry>("jaasLoginContextEntry");

    protected final JAASLoginModuleConfig jaasLoginModuleConfig = mock.mock(JAASLoginModuleConfig.class);
    protected final ServiceReference<JAASLoginModuleConfig> jaasLoginModuleConfigRef = mock.mock(ServiceReference.class, "jaasLoginModuleConfigRef");
    private final JAASLoginModuleConfig anotherJaasLoginModuleConfig = mock.mock(JAASLoginModuleConfig.class, "anotherJaasLoginModuleConfig");
    private final ServiceReference<JAASLoginModuleConfig> anotherJaasLoginModuleConfigRef = mock.mock(ServiceReference.class, "anotherJaasLoginModuleConfigRef");

    protected final ConcurrentServiceReferenceMap<String, JAASLoginModuleConfig> jaasLoginModuleConfigs = new ConcurrentServiceReferenceMap<String, JAASLoginModuleConfig>("jaasLoginModuleConfig");
    Map<String, Object> properties = null;
    private JAASConfigurationImpl jaasConfigurationImpl;

    static final Map<String, Map<String, String>> defaultEntries = new HashMap<String, Map<String, String>>();
    static final Map<String, String> defaultLoginModules = new HashMap<String, String>();

    public static final String CFG_KEY_ENTRY = "entry";
    public static final String CFG_KEY_LOGIN_MODULE_REF = "loginModuleRef";

    public static final String PROXY = "proxy";
    public static final String HASHTABLE = "hashtable";
    public static final String USERNAME_AND_PASSWORD = "userNameAndPassword";
    public static final String CERTIFICATE = "certificate";
    public static final String TOKEN = "token";
    public static final String IDENTITY_ASSERTION = "identityAssertion";

    public static final String HASHTABLE_CLASS_NAME = "com.ibm.ws.security.authentication.jaas.modules.HashtableLoginModule";
    public static final String USERNAME_AND_PASSWORD_CLASS_NAME = "com.ibm.ws.security.authentication.jaas.modules.UsernameAndPasswordLoginModule";
    public static final String CERTIFICATE_CLASS_NAME = "com.ibm.ws.security.authentication.jaas.modules.CertificateLoginModule";
    public static final String TOKEN_CLASS_NAME = "com.ibm.ws.security.authentication.jaas.modules.TokenLoginModule";
    public static final String IDENTITY_ASSERTION_CLASS_NAME = "com.ibm.wsspi.security.common.auth.module.IdentityAssertionLoginModule";
    public static final String PROXY_CLASS_NAME = "com.ibm.ws.security.authentication.internal.jaas.modules.WSLoginModuleProxy";

    private static final int numberOfDefaultEntries = 6;
    @Rule
    public TestName testName = new TestName();

    @Before
    public void setUp() {
        mock.checking(new Expectations() {
            {
                allowing(jaasLoginContextEntryRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(jaasLoginContextEntryRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(jaasLoginModuleConfigRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(jaasLoginModuleConfigRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(anotherJaasLoginModuleConfigRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                allowing(anotherJaasLoginModuleConfigRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(1));
            }
        });
        jaasLoginModuleConfigs.putReference("customLoginModule", jaasLoginModuleConfigRef);
        jaasLoginContextEntries.putReference("myLoginEntry", jaasLoginContextEntryRef);
        jaasConfigurationImpl = new JAASConfigurationImpl();
        jaasConfigurationImpl.setJaasLoginContextEntries(jaasLoginContextEntries);
//        jaasConfigurationImpl.setJaasLoginModuleConfigs(jaasLoginModuleConfigs);
    }

    @After
    public void tearDown() {
        outputMgr.assertContextStatisfied(mock);
    }

    @Test
    public void testGetLoginModules_null() throws Exception {

        List<JAASLoginModuleConfig> loginModuleRefs = new ArrayList<JAASLoginModuleConfig>();

        List<AppConfigurationEntry> appConfigurationEntries = jaasConfigurationImpl.getLoginModules(loginModuleRefs);
        assertTrue(appConfigurationEntries.isEmpty());
    }

    @Test
    public void testCreateAppConfigurationEntry() throws Exception {

        mock.checking(new Expectations() {
            {
                allowing(jaasLoginModuleConfig).getClassName();
                will(returnValue(TOKEN_CLASS_NAME));

                allowing(jaasLoginModuleConfig).getControlFlag();
                will(returnValue(LoginModuleControlFlag.REQUIRED));

                allowing(jaasLoginModuleConfig).getOptions();
                will(returnValue(Collections.<String, Object> emptyMap()));

            }
        });
        AppConfigurationEntry loginModuleEntry = jaasConfigurationImpl.createAppConfigurationEntry(jaasLoginModuleConfig);
        assertEquals(TOKEN_CLASS_NAME, loginModuleEntry.getLoginModuleName());
        assertEquals(LoginModuleControlFlag.REQUIRED, loginModuleEntry.getControlFlag());
        assertTrue(loginModuleEntry.getOptions().isEmpty());
    }

    @Test
    public void getSharedLibrariesClassLoader() throws Exception {
        final ComponentContext componentContext = mock.mock(ComponentContext.class);

        jaasLoginModuleConfigs.putReference("jaasLoginModuleConfigRef", jaasLoginModuleConfigRef);
        jaasLoginModuleConfigs.putReference("anotherJaasLoginModuleConfigRef", anotherJaasLoginModuleConfigRef);
        final String className = "customLoginModule";
        final Map<String, Object> options = new HashMap<String, Object>();
        options.put(LoginModuleProxy.KERNEL_DELEGATE, className);
        mock.checking(new Expectations() {
            {
                allowing(componentContext).locateService("jaasLoginModuleConfig", jaasLoginModuleConfigRef);
                will(returnValue(jaasLoginModuleConfig));
                allowing(componentContext).locateService("jaasLoginModuleConfig", anotherJaasLoginModuleConfigRef);
                will(returnValue(anotherJaasLoginModuleConfig));
                allowing(jaasLoginModuleConfig).getClassName();
                will(returnValue(className));
                allowing(jaasLoginModuleConfig).getControlFlag();
                will(returnValue(LoginModuleControlFlag.REQUIRED));
                allowing(jaasLoginModuleConfig).getOptions();
                will(returnValue(options));
                allowing(jaasLoginModuleConfig).isDefaultLoginModule();
                will(returnValue(false));
            }
        });
        jaasLoginModuleConfigs.activate(componentContext);
        jaasLoginModuleConfigs.putReference("customLoginModule", jaasLoginModuleConfigRef);
        jaasLoginContextEntries.putReference("myLoginEntry", jaasLoginContextEntryRef);
        jaasConfigurationImpl.createAppConfigurationEntry(jaasLoginModuleConfig);
    }
}
