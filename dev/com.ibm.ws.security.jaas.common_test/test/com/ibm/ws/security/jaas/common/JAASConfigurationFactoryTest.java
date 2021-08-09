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
package com.ibm.ws.security.jaas.common;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.jaas.common.internal.JAASSecurityConfiguration;
import com.ibm.ws.security.jaas.config.JAASLoginConfig;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

/**
 *
 */
public class JAASConfigurationFactoryTest {
    private final Mockery mock = new JUnit4Mockery();

    protected final ConcurrentServiceReferenceMap<String, JAASLoginModuleConfig> jaasLoginModuleConfigs = new ConcurrentServiceReferenceMap<String, JAASLoginModuleConfig>("jaasLoginModuleConfig");
    protected final ConcurrentServiceReferenceMap<String, JAASLoginContextEntry> jaasLoginContextEntries = new ConcurrentServiceReferenceMap<String, JAASLoginContextEntry>("jaasLoginContextEntry");
    protected final AtomicServiceReference<ClassLoadingService> classLoadingRef = new AtomicServiceReference<ClassLoadingService>("classLoadingSvc");
    protected final ServiceReference<JAASLoginConfig> jaasLoginConfigRef = mock.mock(ServiceReference.class, "jaasLoginConfigRef");
    private final ServiceReference<JAASConfiguration> jaasConfigurationRef = mock.mock(ServiceReference.class, "testJaasConfigurationRef");
    private final ServiceReference<JAASLoginContextEntry> jaasLoginContextEntryRef = mock.mock(ServiceReference.class, "testJaasLoginContextEntryRef");
    private final JAASConfiguration jaasConfiguration = mock.mock(JAASConfiguration.class, "testJaasConfiguration");
    private final JAASLoginConfig jaasLoginConfig = mock.mock(JAASLoginConfig.class, "jaasLoginConfig");
    private final String loginContextName = "testLoginContextEntryID";
    private final Map<String, ?> options = new HashMap<String, Object>();

    private final static Map<String, List<AppConfigurationEntry>> jaasConfigurationEntries = new HashMap<String, List<AppConfigurationEntry>>();
    private final ComponentContext componentContext = createComponentContextMock();

    @Before
    public void setUp() {
        AppConfigurationEntry appConfigurationEntry = new AppConfigurationEntry("com.ibm.ws.security.jaas.common.internal.test.modules.TestLoginModule", LoginModuleControlFlag.REQUIRED, options);
        List<AppConfigurationEntry> appConfigurationEntries = new ArrayList<AppConfigurationEntry>();
        appConfigurationEntries.add(appConfigurationEntry);
        jaasConfigurationEntries.put(loginContextName, appConfigurationEntries);

        mock.checking(new Expectations() {
            {
                allowing(jaasLoginContextEntryRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(jaasLoginContextEntryRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
            }
        });

        jaasLoginContextEntries.putReference(loginContextName, jaasLoginContextEntryRef);
    }

    /**
     * Individual teardown after each test.
     * 
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        Configuration.setConfiguration(null);
        jaasConfigurationEntries.clear();
        mock.assertIsSatisfied();
    }

    private ComponentContext createComponentContextMock() {
        final ComponentContext componentContextMock = mock.mock(ComponentContext.class, "testComponentContext");
        mock.checking(new Expectations() {
            {
                allowing(componentContextMock).locateService(JAASConfigurationFactory.KEY_JAAS_CONFIGURATION, jaasConfigurationRef);
                will(returnValue(jaasConfiguration));
            }
        });
        return componentContextMock;
    }

    @Test
    public void testinstallJaasConfigurationEntries() {
        JAASConfigurationFactory jaasConfigurationFactory = new JAASConfigurationFactory();
        mock.checking(new Expectations() {
            {
                allowing(componentContext).locateService(JAASConfigurationFactory.KEY_JAAS_LOGIN_CONFIG, jaasLoginConfigRef);
                will(returnValue(jaasLoginConfig));
                allowing(jaasLoginConfig).getEntries();
                will(returnValue(null));

                allowing(jaasConfiguration).setJaasLoginContextEntries(jaasLoginContextEntries);
//                allowing(jaasConfiguration).setJaasLoginModuleConfigs(jaasLoginModuleConfigs);
                allowing(jaasConfiguration).getEntries();
                will(returnValue(jaasConfigurationEntries));
            }
        });
        jaasConfigurationFactory.setJAASConfiguration(jaasConfigurationRef);
        jaasConfigurationFactory.setJAASLoginConfig(jaasLoginConfigRef);
        jaasConfigurationFactory.activate(componentContext);

        jaasConfigurationFactory.installJAASConfiguration(jaasLoginContextEntries);
        Configuration configuration = Configuration.getConfiguration();
        assertTrue("The configuration must be a JAASConfiguration", configuration instanceof JAASSecurityConfiguration);

        JAASSecurityConfiguration jaasConfiguration = jaasConfigurationFactory.getJaasConfiguration();

        AppConfigurationEntry[] appConfigurationEntries = jaasConfiguration.getAppConfigurationEntry(loginContextName);
        assertNotNull("There must be an AppConfigurationEntry[] for " + loginContextName, appConfigurationEntries);
    }

    @Test
    public void testInstallJAASConfigurationFromJAASConfigFile() {
        final ComponentContext componentContextMock1 = mock.mock(ComponentContext.class, "testComponentContext1");
        JAASConfigurationFactory jaasConfigurationFactory = new JAASConfigurationFactory();
        jaasConfigurationFactory.setJAASLoginConfig(jaasLoginConfigRef);

        mock.checking(new Expectations() {
            {
                allowing(componentContextMock1).locateService(JAASConfigurationFactory.KEY_JAAS_LOGIN_CONFIG, jaasLoginConfigRef);
                will(returnValue(jaasLoginConfig));
                allowing(jaasLoginConfig).getEntries();
                will(returnValue(jaasConfigurationEntries));
            }
        });
        jaasConfigurationFactory.activate(componentContextMock1);
        jaasConfigurationFactory.installJAASConfigurationFromJAASConfigFile();
        Configuration configuration = Configuration.getConfiguration();
        assertTrue("The configuration must be a JAASConfiguration", configuration instanceof JAASSecurityConfiguration);

        JAASSecurityConfiguration jaasConfiguration = jaasConfigurationFactory.getJaasConfiguration();

        AppConfigurationEntry[] appConfigurationEntries = jaasConfiguration.getAppConfigurationEntry(loginContextName);
        assertNotNull("There must be an AppConfigurationEntry[] for " + loginContextName, appConfigurationEntries);
    }
}
