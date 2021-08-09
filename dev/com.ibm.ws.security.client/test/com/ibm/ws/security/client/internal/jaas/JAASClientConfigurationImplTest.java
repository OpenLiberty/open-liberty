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
package com.ibm.ws.security.client.internal.jaas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.kernel.boot.security.LoginModuleProxy;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.ws.security.jaas.common.JAASLoginContextEntry;
import com.ibm.ws.security.jaas.common.JAASLoginModuleConfig;
import com.ibm.ws.security.jaas.common.internal.JAASLoginModuleConfigImpl;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

/**
 *
 */
public class JAASClientConfigurationImplTest {
    protected static SharedOutputManager outputMgr;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final String loginModuleProxyId = JAASLoginModuleConfigImpl.PROXY;
    private static final String loginModuleProxyClassName = JAASLoginModuleConfigImpl.WSLOGIN_MODULE_PROXY;
    private static Map<String, Object> options = new HashMap<String, Object>();
    private static final Class<?> loginModuleDelegate = JAASClientConfigurationImpl.WSCLIENTLOGIN_MODULE_IMPL_CLASS;
    private static final String controlFlag = "REQUIRED";
    private static final String customLoginModuleClassName = "customLoginModule";
    private static final String customLoginContextEntryName = "myLoginEntry";
    private static final String loginModulePid = "pid";

    JAASClientConfigurationImpl jaasClientConfigurationImpl = null;

    private final JAASLoginContextEntry jaasLoginContextEntry = mock.mock(JAASLoginContextEntry.class);
    private final JAASLoginContextEntry jaasLoginContextEntryDuplicate = mock.mock(JAASLoginContextEntry.class, "jaasLoginContextEntryDuplicate");
    private final ConcurrentServiceReferenceMap<String, JAASLoginContextEntry> jaasLoginContextEntries = mock.mock(ConcurrentServiceReferenceMap.class, "jaasLoginContextEntries");
    private final Iterator<JAASLoginContextEntry> lceIterator = mock.mock(Iterator.class, "lceIterator");

    private final ConcurrentServiceReferenceMap<String, JAASLoginModuleConfig> jaasLoginModuleConfigs = mock.mock(ConcurrentServiceReferenceMap.class, "jaasLoginModuleConfigs");
    private final JAASLoginModuleConfig jaasLoginModuleConfig = mock.mock(JAASLoginModuleConfig.class);

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();

    }

    @Before
    public void setUp() {
        jaasClientConfigurationImpl = new JAASClientConfigurationImpl();
        options.put(JAASLoginModuleConfigImpl.DELEGATE, loginModuleDelegate);
    }

    /**
     * Final teardown work when class is exiting.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     * 
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
        mock.assertIsSatisfied();
    }

    @Test
    public void testGetEntries_empty() throws Exception {
        assertTrue("There should be no entries by default.", jaasClientConfigurationImpl.getEntries().isEmpty());
    }

    @Test
    public void testGetEntries() throws Exception {
        jaasClientConfigurationImpl.setJaasLoginContextEntries(jaasLoginContextEntries);
        createMockExpectations();
        final Map<String, Object> options = new HashMap<String, Object>();
        final List<String> loginModulePids = new ArrayList<String>();
        loginModulePids.add(loginModulePid);
        options.put(LoginModuleProxy.KERNEL_DELEGATE, customLoginModuleClassName);
        mock.checking(new Expectations() {
            {
                one(lceIterator).hasNext();
                will(returnValue(false));
            }
        });
        jaasClientConfigurationImpl.createAppConfigurationEntry(jaasLoginModuleConfig, customLoginContextEntryName);
        List<AppConfigurationEntry> appConfEntries = jaasClientConfigurationImpl.getEntries().get(customLoginContextEntryName);
        String actualLoginModuleName = appConfEntries.get(0).getLoginModuleName();
        assertEquals("We should have an appConfiguration entry name for the login context entry " + customLoginContextEntryName + " with a login module name "
                     + customLoginModuleClassName, customLoginModuleClassName,
                     actualLoginModuleName);
    }

    @Test
    public void testGetEntries_warnDuplicateID() throws Exception {
        jaasClientConfigurationImpl.setJaasLoginContextEntries(jaasLoginContextEntries);
        createMockExpectations();

        final Map<String, Object> options = new HashMap<String, Object>();
        final List<JAASLoginModuleConfig> loginModules = new ArrayList<JAASLoginModuleConfig>();
        loginModules.add(jaasLoginModuleConfig);
        options.put(LoginModuleProxy.KERNEL_DELEGATE, customLoginModuleClassName);
        mock.checking(new Expectations() {
            {
                //duplicate entry
                one(lceIterator).hasNext();
                will(returnValue(true));
                one(lceIterator).next();
                will(returnValue(jaasLoginContextEntryDuplicate));
                allowing(jaasLoginContextEntryDuplicate).getEntryName();
                will(returnValue(customLoginContextEntryName));
                allowing(jaasLoginContextEntryDuplicate).getLoginModules();
                will(returnValue(loginModules));
                allowing(jaasLoginContextEntryDuplicate).getId();
                will(returnValue(customLoginContextEntryName));
                one(lceIterator).hasNext();
                will(returnValue(false));
            }
        });
        jaasClientConfigurationImpl.createAppConfigurationEntry(jaasLoginModuleConfig, customLoginContextEntryName);
        List<AppConfigurationEntry> appConfEntries = jaasClientConfigurationImpl.getEntries().get(customLoginContextEntryName);
        String actualLoginModuleName = appConfEntries.get(0).getLoginModuleName();
        assertEquals("We should have an appConfiguration entry name for the login context entry " + customLoginContextEntryName + " with a login module name "
                     + customLoginModuleClassName, customLoginModuleClassName,
                     actualLoginModuleName);

        //should get warning message for duplicate login context entry
        assertTrue("Expected warning message was not logged",
                   outputMgr.checkForStandardOut("CWWKS1169W:.*" + customLoginContextEntryName + ".*"));
    }

    @Test
    public void testGetLoginModules_empty() throws Exception {
        List<JAASLoginModuleConfig> loginModuleRefs = new ArrayList<JAASLoginModuleConfig>();
        List<AppConfigurationEntry> appConfigurationEntries = jaasClientConfigurationImpl.getLoginModules(loginModuleRefs, JaasLoginConfigConstants.CLIENT_CONTAINER);
        assertTrue(appConfigurationEntries.isEmpty());
    }

    @Test
    public void testCreateAppConfigurationEntry() throws Exception {
        Map<String, Object> expectedOptions = new HashMap<String, Object>(options);
        expectedOptions.put(JAASClientConfigurationImpl.WAS_IGNORE_CLIENT_CONTAINER_DD, false);
        AppConfigurationEntry loginModuleEntry = jaasClientConfigurationImpl.createAppConfigurationEntry(jaasLoginModuleConfig(), JaasLoginConfigConstants.CLIENT_CONTAINER);
        assertEquals("The login module name should be " + loginModuleProxyClassName, loginModuleProxyClassName, loginModuleEntry.getLoginModuleName());
        assertEquals("The login module control flag should be " + controlFlag, LoginModuleControlFlag.REQUIRED, loginModuleEntry.getControlFlag());
        assertEquals("The login module options should be " + expectedOptions, expectedOptions, loginModuleEntry.getOptions());
    }

    @Test
    public void testCreateAppConfigurationEntryForWSLogin() throws Exception {
        Map<String, Object> expectedOptions = new HashMap<String, Object>(options);
        expectedOptions.put(JAASClientConfigurationImpl.WAS_IGNORE_CLIENT_CONTAINER_DD, true);
        AppConfigurationEntry loginModuleEntry = jaasClientConfigurationImpl.createAppConfigurationEntry(jaasLoginModuleConfig(), JaasLoginConfigConstants.APPLICATION_WSLOGIN);
        assertEquals("The login module name should be " + loginModuleProxyClassName, loginModuleProxyClassName, loginModuleEntry.getLoginModuleName());
        assertEquals("The login module control flag should be " + controlFlag, LoginModuleControlFlag.REQUIRED, loginModuleEntry.getControlFlag());
        assertEquals("The login module options should be " + expectedOptions, expectedOptions, loginModuleEntry.getOptions());
    }

    private JAASLoginModuleConfig jaasLoginModuleConfig() {

        mock.checking(new Expectations() {
            {
                allowing(jaasLoginModuleConfig).getClassName();
                will(returnValue(loginModuleProxyClassName));

                allowing(jaasLoginModuleConfig).getControlFlag();
                will(returnValue(LoginModuleControlFlag.REQUIRED));

                allowing(jaasLoginModuleConfig).getOptions();
                will(returnValue(options));

            }
        });

        return jaasLoginModuleConfig;
    }

    private void createMockExpectations() {
        final Map<String, Object> options = new HashMap<String, Object>();
        options.put(LoginModuleProxy.KERNEL_DELEGATE, customLoginModuleClassName);
        final List<JAASLoginModuleConfig> loginModules = new ArrayList<JAASLoginModuleConfig>();
        loginModules.add(jaasLoginModuleConfig);
        mock.checking(new Expectations() {
            {
                allowing(jaasLoginModuleConfig).getClassName();
                will(returnValue(customLoginModuleClassName));
                allowing(jaasLoginModuleConfig).getControlFlag();
                will(returnValue(LoginModuleControlFlag.REQUIRED));
                allowing(jaasLoginModuleConfig).getOptions();
                will(returnValue(options));
                allowing(jaasLoginContextEntries).getServices();
                will(returnValue(lceIterator));
                one(lceIterator).hasNext();
                will(returnValue(true));
                one(lceIterator).next();
                will(returnValue(jaasLoginContextEntry));
                allowing(jaasLoginContextEntry).getEntryName();
                will(returnValue(customLoginContextEntryName));
                allowing(jaasLoginContextEntry).getLoginModules();
                will(returnValue(loginModules));
                allowing(jaasLoginModuleConfigs).getService(loginModulePid);
                will(returnValue(jaasLoginModuleConfig));
                allowing(jaasLoginContextEntry).getId();
                will(returnValue(customLoginContextEntryName));
            }
        });
    }
}
