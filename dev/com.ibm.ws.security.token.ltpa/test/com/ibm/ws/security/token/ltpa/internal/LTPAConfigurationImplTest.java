/*******************************************************************************
 * Copyright (c) 2012, 2023 IBM Corporation and others.
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
package com.ibm.ws.security.token.ltpa.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.filemonitor.LTPAFileMonitor;
import com.ibm.ws.security.token.ltpa.LTPAConfiguration;
import com.ibm.ws.security.token.ltpa.LTPAKeyInfoManager;
import com.ibm.wsspi.kernel.filemonitor.FileMonitor;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;
import com.ibm.wsspi.security.ltpa.TokenFactory;

import test.common.SharedOutputManager;

/**
 *
 */
@SuppressWarnings("unchecked")
public class LTPAConfigurationImplTest {

    private static SharedOutputManager outputMgr;

    private static final String PATH_TO_FILE = "/path/to/file";
    private static final String PATH_TO_ANOTHER_FILE = "/path/to/another/file";
    private static final String DEFAULT_CONFIG_LOCATION_DIR = "${server.config.dir}/resources/security/";
    private static final String DEFAULT_CONFIG_LOCATION = "${server.config.dir}/resources/security/ltpa.keys";
    private static final String DEFAULT_OUTPUT_LOCATION = "${server.output.dir}/resources/security/ltpa.keys";
    private static final String RESOLVED_DEFAULT_CONFIG_LOCATION = "testServerName/resources/security/ltpa.keys";
    private static final String RESOLVED_DEFAULT_CONFIG_LOCATION_DIR = "testServerName/resources/security/";
    private static final String RESOLVED_DEFAULT_OUTPUT_LOCATION = "testServerName/resources/security/ltpa.keys";
    private static final String PWD = "pwd";
    private static final String ANOTHER_PWD = "anotherPwd";
    private static final boolean DEFAULT_MONITOR_DIR_VALUE = false;

    private final Mockery mock = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final BundleContext bundleContext = mock.mock(BundleContext.class);
    private final ServiceReference<WsLocationAdmin> locateServiceRef = mock.mock(ServiceReference.class, "locateServiceRef");
    private final WsLocationAdmin locateService = mock.mock(WsLocationAdmin.class);
    private final ServiceReference<ExecutorService> executorServiceRef = mock.mock(ServiceReference.class, "executorServiceRef");
    private final ExecutorService executorService = mock.mock(ExecutorService.class);
    private final ServiceRegistration<LTPAConfiguration> registration = mock.mock(ServiceRegistration.class);
    private final ServiceRegistration<FileMonitor> fileMonitorRegistration = mock.mock(ServiceRegistration.class, "fileMonitorRegistration");
    private final TokenFactory tokenFactory = mock.mock(TokenFactory.class);
    private final ServiceReference<LTPAKeysChangeNotifier> ltpaKeysChangeNotifierRef = mock.mock(ServiceReference.class, "ltpaKeysChangeNotifierRef");
    private final LTPAKeysChangeNotifier ltpaKeysChangeNotifier = mock.mock(LTPAKeysChangeNotifier.class);

    //private final LocalFileResource keysFileInServerConfig2 = mock.mock(LocalFileResource.class);
    private final WsResource keysFileInServerConfig = mock.mock(WsResource.class, "keysFileInServerConfig");
    private final WsResource parentResource = mock.mock(WsResource.class, "parentResource");
    //keysFileInDirectory

    private LTPAConfigurationImplTestDouble ltpaConfig;
    private Map<String, Object> props;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        props = createProps(DEFAULT_CONFIG_LOCATION, PWD, 120L, 0L, DEFAULT_MONITOR_DIR_VALUE, 0L);

        mock.checking(new Expectations() {
            {
                allowing(cc).getBundleContext();
                will(returnValue(bundleContext));
                allowing(cc).locateService(LTPAConfigurationImpl.KEY_LOCATION_SERVICE, locateServiceRef);
                will(returnValue(locateService));
                allowing(cc).locateService(LTPAConfigurationImpl.KEY_EXECUTOR_SERVICE, executorServiceRef);
                will(returnValue(executorService));
                allowing(cc).locateService(LTPAConfigurationImpl.KEY_CHANGE_SERVICE, ltpaKeysChangeNotifierRef);
                will(returnValue(ltpaKeysChangeNotifier));
            }
        });

        setupExecutorServiceExpectations(1);
        setupLocationServiceExpectations(1);
        //setupFileMonitorRegistrationsExpectations(1);

        ltpaConfig = createActivatedLTPAConfigurationImpl();
    }

    private Map<String, Object> createProps(String filePath, String password, long expiration, long monitorInterval, boolean monitorDirectory, long expDiffAllowed) {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(LTPAConfiguration.CFG_KEY_IMPORT_FILE, filePath);
        props.put(LTPAConfiguration.CFG_KEY_PASSWORD, new SerializableProtectedString(password.toCharArray()));
        props.put(LTPAConfiguration.CFG_KEY_TOKEN_EXPIRATION, expiration);
        props.put(LTPAConfiguration.CFG_KEY_MONITOR_INTERVAL, monitorInterval);
        props.put(LTPAConfiguration.CFG_KEY_MONITOR_DIRECTORY, monitorDirectory);
        props.put(LTPAConfigurationImpl.KEY_EXP_DIFF_ALLOWED, expDiffAllowed);
        return props;
    }

    private void setupLocationServiceExpectations(final int numberOfInvocations) {
        mock.checking(new Expectations() {
            {
                exactly(numberOfInvocations).of(locateService).resolveResource(DEFAULT_CONFIG_LOCATION);
                will(returnValue(keysFileInServerConfig));

                exactly(numberOfInvocations).of(keysFileInServerConfig).getParent();
                will(returnValue(parentResource));

                exactly(numberOfInvocations).of(parentResource).toRepositoryPath();
                will(returnValue(DEFAULT_CONFIG_LOCATION_DIR));

                exactly(numberOfInvocations).of(locateService).resolveString(DEFAULT_CONFIG_LOCATION_DIR);
                will(returnValue(RESOLVED_DEFAULT_CONFIG_LOCATION_DIR));

                exactly(numberOfInvocations).of(keysFileInServerConfig).getName();
                will(returnValue("ltpa.keys"));

                //exactly(numberOfInvocations).of(locateService).resolveString(DEFAULT_OUTPUT_LOCATION);
                //will(returnValue(RESOLVED_DEFAULT_OUTPUT_LOCATION));

            }
        });
    }

    private void setupLocationServiceExpectationsNew(final int numberOfInvocations) {
        mock.checking(new Expectations() {
            {
                exactly(numberOfInvocations).of(locateService).resolveResource(DEFAULT_CONFIG_LOCATION);
                will(returnValue(keysFileInServerConfig));

                exactly(numberOfInvocations).of(keysFileInServerConfig).getParent();
                will(returnValue(parentResource));

                exactly(numberOfInvocations).of(parentResource).toRepositoryPath();
                will(returnValue(DEFAULT_CONFIG_LOCATION_DIR));

                exactly(numberOfInvocations).of(locateService).resolveString(DEFAULT_CONFIG_LOCATION_DIR);
                will(returnValue(RESOLVED_DEFAULT_CONFIG_LOCATION_DIR));

                exactly(numberOfInvocations).of(keysFileInServerConfig).getName();
                will(returnValue("ltpa.keys"));

                if (DEFAULT_MONITOR_DIR_VALUE)
                    exactly(numberOfInvocations).of(locateService).resolveResource(RESOLVED_DEFAULT_CONFIG_LOCATION_DIR);
                //wsLocationAdmin.resolveResource("testServerName/resources/security/")
            }
        });
    }

    private void setupExecutorServiceExpectations(final int numberOfInvocations) {
        mock.checking(new Expectations() {
            {
                exactly(numberOfInvocations).of(executorService).execute(with(any(Runnable.class)));
            }
        });
    }

    private LTPAConfigurationImplTestDouble createActivatedLTPAConfigurationImpl() {
        LTPAConfigurationImplTestDouble ltpaConfig = new LTPAConfigurationImplTestDouble();
        ltpaConfig.setExecutorService(executorServiceRef);
        ltpaConfig.setLocationService(locateServiceRef);
        ltpaConfig.setLtpaKeysChangeNotifier(ltpaKeysChangeNotifierRef);
        ltpaConfig.activate(cc, props);
        return ltpaConfig;
    }

    class LTPAConfigurationImplTestDouble extends LTPAConfigurationImpl {

        public boolean wasUnsetFileMonitorRegistrationCalled = false;
        public boolean wasSetFileMonitorRegistrationCalled = false;

        @Override
        protected void unsetFileMonitorRegistration() {
            super.unsetFileMonitorRegistration();
            wasUnsetFileMonitorRegistrationCalled = true;
        }

        @Override
        protected void setFileMonitorRegistration(ServiceRegistration<FileMonitor> ltpaFileMonitorRegistration) {
            super.setFileMonitorRegistration(ltpaFileMonitorRegistration);
            wasSetFileMonitorRegistrationCalled = true;
        }
    }

    @After
    public void tearDown() {
        ltpaConfig.deactivate(cc);
        ltpaConfig.unsetExecutorService(executorServiceRef);
        ltpaConfig.unsetLocationService(locateServiceRef);
        outputMgr.resetStreams();
        mock.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownClass() throws MalformedURLException {
        outputMgr.restoreStreams();
    }

    protected void addPathToAnotherFileExpectations() {
        mock.checking(new Expectations() {
            {
                one(locateService).resolveResource(PATH_TO_ANOTHER_FILE);
                will(returnValue(keysFileInServerConfig));

                one(keysFileInServerConfig).getParent();
                will(returnValue(parentResource));

                one(parentResource).toRepositoryPath();
                will(returnValue(PATH_TO_ANOTHER_FILE));

                one(locateService).resolveString(PATH_TO_ANOTHER_FILE);
                will(returnValue(PATH_TO_ANOTHER_FILE));

                one(keysFileInServerConfig).getName();
                will(returnValue(""));
            }
        });
    }

    @Test
    public void deactivateUnregister() {
        mock.checking(new Expectations() {
            {
                one(registration).unregister();
                one(fileMonitorRegistration).unregister();
            }
        });
        ltpaConfig.setRegistration(registration);
        ltpaConfig.setFileMonitorRegistration(fileMonitorRegistration);
        ltpaConfig.deactivate(cc);
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.ltpa.internal.LTPAConfigurationImpl#getTokenFactory()}.
     */
    @Test
    public void getTokenFactory() {
        ltpaConfig.setTokenFactory(tokenFactory);
        assertSame("Should be the same TokenFactory instance",
                   tokenFactory, ltpaConfig.getTokenFactory());
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.ltpa.internal.LTPAConfigurationImpl#getLTPAKeyInfoManager()}.
     */
    @Test
    public void getLTPAKeyInfoManager() {
        LTPAKeyInfoManager ltpaKeyInfoManager = new LTPAKeyInfoManager();
        ltpaConfig.setLTPAKeyInfoManager(ltpaKeyInfoManager);
        assertSame("Should be the same TokenFactory instance",
                   ltpaKeyInfoManager, ltpaConfig.getLTPAKeyInfoManager());
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.ltpa.internal.LTPAConfigurationImpl#getPrimaryKeyFile()}.
     */
    @Test
    public void getKeyFile() {
        assertEquals("Key file value was not the expected value",
                     RESOLVED_DEFAULT_CONFIG_LOCATION, ltpaConfig.getPrimaryKeyFile());
    }

    /**
     * Tests that the file monitor is registered and set in the LTPAConfigImpl object.
     */
    @Test
    public void fileMonitorRegistration() throws Exception {
        setupExecutorServiceExpectations(1);
        setupLocationServiceExpectations(1);
        setupFileMonitorRegistrationsExpectations(1);

        props.put(LTPAConfiguration.CFG_KEY_MONITOR_INTERVAL, 5L);
        LTPAConfigurationImplTestDouble ltpaConfig = createActivatedLTPAConfigurationImpl();

        assertTrue("The LTPA file monitor registration must be set.", ltpaConfig.wasSetFileMonitorRegistrationCalled);
    }

    @SuppressWarnings("deprecation")
    private void setupFileMonitorRegistrationsExpectations(final int numberOfInvocations) {
        mock.checking(new Expectations() {
            {
                exactly(numberOfInvocations).of(bundleContext).registerService(with(FileMonitor.class), with(any(LTPAFileMonitor.class)),
                                                                               (Hashtable<String, Object>) with(any(Hashtable.class)));
            }
        });
    }

    /**
     * Tests that there is no file monitor registered by default.
     */
    @Test
    public void fileMonitorRegistration_notCreatedByDefault() throws Exception {
        assertFalse("The LTPA file monitor registration must not be set.", ltpaConfig.wasSetFileMonitorRegistrationCalled);
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.ltpa.internal.LTPAConfigurationImpl#getPrimaryKeyPassword()}.
     */
    @Test
    public void getKeyPassword() {
        assertEquals("Key file value was not the expected value",
                     PWD, ltpaConfig.getPrimaryKeyPassword());
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.ltpa.internal.LTPAConfigurationImpl#getTokenExpiration()}.
     */
    @Test
    public void getTokenExpiration() {
        assertEquals("Key file value was not the expected value",
                     120, ltpaConfig.getTokenExpiration());
    }

    @Test
    public void modified() {
        setupExecutorServiceExpectations(1);
        //setupLocationServiceExpectations(1);

        addPathToAnotherFileExpectations();

        props.put(LTPAConfiguration.CFG_KEY_IMPORT_FILE, PATH_TO_ANOTHER_FILE);
        ltpaConfig.modified(props);
    }

    @Test
    public void modified_monitorIntervalSet_everythingElseTheSame_keysNotReloaded() {
        setupExecutorServiceExpectations(0);
        setupLocationServiceExpectations(1);
        setupFileMonitorRegistrationsExpectations(1);

        props.put(LTPAConfiguration.CFG_KEY_MONITOR_INTERVAL, 5000L);
        ltpaConfig.modified(props);
    }

    @Test
    public void modified_monitorIntervalSame_fileChanged_unregistersListenerAndCreatesKeys() throws Exception {
        setupExecutorServiceExpectations(2);
        setupLocationServiceExpectations(1);
        setupFileMonitorRegistrationsExpectations(2);

        props.put(LTPAConfiguration.CFG_KEY_MONITOR_INTERVAL, 5L);
        LTPAConfigurationImplTestDouble ltpaConfig = createActivatedLTPAConfigurationImpl();

        addPathToAnotherFileExpectations();

        props.put(LTPAConfiguration.CFG_KEY_IMPORT_FILE, PATH_TO_ANOTHER_FILE);
        ltpaConfig.modified(props);
        assertTrue("The old file monitor must be unset.", ltpaConfig.wasUnsetFileMonitorRegistrationCalled);
        assertTrue("Expected CWWKS4107A message was not logged",
                   outputMgr.checkForStandardOut("CWWKS4107A:.*" + PATH_TO_ANOTHER_FILE));
    }

    @Test
    public void modified_fileAndMonitorIntervalChanged_unregistersListenerAndCreatesKeys() throws Exception {
        setupExecutorServiceExpectations(2);
        setupLocationServiceExpectations(1);
        setupFileMonitorRegistrationsExpectations(2);

        props.put(LTPAConfiguration.CFG_KEY_MONITOR_INTERVAL, 5L);
        LTPAConfigurationImplTestDouble ltpaConfig = createActivatedLTPAConfigurationImpl();

        addPathToAnotherFileExpectations();

        props.put(LTPAConfiguration.CFG_KEY_IMPORT_FILE, PATH_TO_ANOTHER_FILE);
        props.put(LTPAConfiguration.CFG_KEY_MONITOR_INTERVAL, 10L);
        ltpaConfig.modified(props);
        assertTrue("The old file monitor must be unset.", ltpaConfig.wasUnsetFileMonitorRegistrationCalled);
        assertTrue("Expected CWWKS4107A message was not logged",
                   outputMgr.checkForStandardOut("CWWKS4107A:.*" + PATH_TO_ANOTHER_FILE));
    }

    @Test
    public void modified_fileChanged_monitorIntervalSetToZero_unregistersListenerAndCreatesKeys() throws Exception {
        setupExecutorServiceExpectations(2);
        setupLocationServiceExpectations(1);
        setupFileMonitorRegistrationsExpectations(1);

        props.put(LTPAConfiguration.CFG_KEY_MONITOR_INTERVAL, 5L);
        LTPAConfigurationImplTestDouble ltpaConfig = createActivatedLTPAConfigurationImpl();

        addPathToAnotherFileExpectations();

        props.put(LTPAConfiguration.CFG_KEY_IMPORT_FILE, PATH_TO_ANOTHER_FILE);
        props.put(LTPAConfiguration.CFG_KEY_MONITOR_INTERVAL, 0L);
        ltpaConfig.modified(props);
        assertTrue("The old file monitor must be unset.", ltpaConfig.wasUnsetFileMonitorRegistrationCalled);
        assertTrue("Expected CWWKS4107A message was not logged",
                   outputMgr.checkForStandardOut("CWWKS4107A:.*" + PATH_TO_ANOTHER_FILE));
    }

    @Test
    public void modified_monitorIntervalSetToZero_everythingElseTheSame_unregistersListener() throws Exception {
        setupExecutorServiceExpectations(1);
        setupLocationServiceExpectations(2);
        setupFileMonitorRegistrationsExpectations(1);

        props.put(LTPAConfiguration.CFG_KEY_MONITOR_INTERVAL, 5L);
        LTPAConfigurationImplTestDouble ltpaConfig = createActivatedLTPAConfigurationImpl();

        props.put(LTPAConfiguration.CFG_KEY_MONITOR_INTERVAL, 0L);
        ltpaConfig.modified(props);
        assertTrue("The old file monitor must be unset.", ltpaConfig.wasUnsetFileMonitorRegistrationCalled);
        assertFalse("Message CWWKS4107A was not expected", outputMgr.checkForStandardOut("CWWKS4107A:.*"));
    }

    @Test
    public void modified_passwordChanged_doNotUnregisterOrCreateKeys() throws Exception {
        setupExecutorServiceExpectations(1);
        setupLocationServiceExpectations(2);
        setupFileMonitorRegistrationsExpectations(1);

        props.put(LTPAConfiguration.CFG_KEY_MONITOR_INTERVAL, 5L);
        LTPAConfigurationImplTestDouble ltpaConfig = createActivatedLTPAConfigurationImpl();

        props.put(LTPAConfiguration.CFG_KEY_PASSWORD, new SerializableProtectedString(ANOTHER_PWD.toCharArray()));
        ltpaConfig.modified(props);
        assertFalse("The old file monitor must not be unset.", ltpaConfig.wasUnsetFileMonitorRegistrationCalled);
    }

    @Test
    public void configReady() throws Exception {
        setupExecutorServiceExpectations(1);
        setupLocationServiceExpectations(1);

        LTPAConfigurationImpl ltpaConfig = createActivatedLTPAConfigurationImpl();

        mock.checking(new Expectations() {
            {
                one(ltpaKeysChangeNotifier).notifyListeners();
            }
        });

        ltpaConfig.configReady();
    }

    @Test
    public void keyFileFromConfigDirWhenDefaultLocationNotOverridden() {
        setupExecutorServiceExpectations(1);
        mock.checking(new Expectations() {
            {
                one(locateService).resolveResource(RESOLVED_DEFAULT_OUTPUT_LOCATION);
                will(returnValue(keysFileInServerConfig));

                one(keysFileInServerConfig).getParent();
                will(returnValue(parentResource));

                one(parentResource).toRepositoryPath();
                will(returnValue(DEFAULT_CONFIG_LOCATION_DIR));

                one(locateService).resolveString(DEFAULT_CONFIG_LOCATION_DIR);
                will(returnValue(RESOLVED_DEFAULT_CONFIG_LOCATION_DIR));

                one(keysFileInServerConfig).getName();
                will(returnValue("ltpa.keys"));
            }
        });

        props.put(LTPAConfiguration.CFG_KEY_IMPORT_FILE, RESOLVED_DEFAULT_OUTPUT_LOCATION);
        LTPAConfigurationImplTestDouble ltpaConfig = createActivatedLTPAConfigurationImpl();
        assertEquals("Key file value was not the expected value",
                     RESOLVED_DEFAULT_CONFIG_LOCATION, ltpaConfig.getPrimaryKeyFile());
        //Key file value was not the expected value expected:<[testServerName/resources/security/ltpa.keys]> but was:<[]>
    }

    @Test
    public void keyFileFromOutputDirWhenDefaultLocationNotOverriddenAndKeysFileNotInConfigDir() {
        setupExecutorServiceExpectations(1);
        mock.checking(new Expectations() {
            {
                one(locateService).resolveResource(RESOLVED_DEFAULT_OUTPUT_LOCATION);
                will(returnValue(keysFileInServerConfig));

                one(keysFileInServerConfig).getParent();
                will(returnValue(parentResource));

                one(parentResource).toRepositoryPath();
                will(returnValue(RESOLVED_DEFAULT_CONFIG_LOCATION_DIR));

                one(locateService).resolveString(RESOLVED_DEFAULT_CONFIG_LOCATION_DIR);
                will(returnValue(RESOLVED_DEFAULT_CONFIG_LOCATION_DIR));

                one(keysFileInServerConfig).getName();
                will(returnValue("ltpa.keys"));
            }
        });

        props.put(LTPAConfiguration.CFG_KEY_IMPORT_FILE, RESOLVED_DEFAULT_OUTPUT_LOCATION);
        LTPAConfigurationImplTestDouble ltpaConfig = createActivatedLTPAConfigurationImpl();
        assertEquals("Key file value was not the expected value",
                     RESOLVED_DEFAULT_OUTPUT_LOCATION, ltpaConfig.getPrimaryKeyFile());
    }

}
