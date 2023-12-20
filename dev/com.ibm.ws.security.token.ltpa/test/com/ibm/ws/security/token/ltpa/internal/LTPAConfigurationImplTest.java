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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
    private static final String PATH_TO_DIR = "/path/to/";
    private static final String PATH_TO_ANOTHER_FILE = "/path/to/another/file";
    private static final String DEFAULT_CONFIG_LOCATION_DIR = "${server.config.dir}/resources/security/";
    private static final String DEFAULT_CONFIG_LOCATION = "${server.config.dir}/resources/security/ltpa.keys";
    private static final String DEFAULT_OUTPUT_LOCATION = "${server.output.dir}/resources/security/ltpa.keys";
    private static final String DEFAULT_VALIDATION_KEY_LOCATION = "${server.config.dir}/resources/security/validation.keys";
    private static final String RESOLVED_DEFAULT_CONFIG_LOCATION = "testServerName/resources/security/ltpa.keys";
    private static final String RESOLVED_DEFAULT_CONFIG_LOCATION_DIR = "testServerName/resources/security/";
    private static final String RESOLVED_DEFAULT_OUTPUT_LOCATION = "testServerName/resources/security/ltpa.keys";
    private static final String DEFAULT_VALIDATION_KEY_ELEMENT = "<validationKeys fileName=\"validation.keys\" password=\"pwd\" validUntilDate=\"2099-01-01T00:00:00Z\"/>";
    private static final String DEFAULT_VALIDATION_FILENAME = "validation.keys";
    private static final String DEFAULT_VALIDATION_PASSWORD = "pwd";
    private static final String DEFAULT_VALIDATION_VALID_UNTIL_DATE = "2099-01-01T00:00:00Z";
    private static final String PWD = "pwd";
    private static final String ANOTHER_PWD = "anotherPwd";
    private static final boolean DEFAULT_MONITOR_DIR_VALUE = false;
    private static final String DEFAULT_UPDATE_TRIGGER = "polled";

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

    private LTPAConfigurationImplTestDouble ltpaConfig;
    private Map<String, Object> props;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {
        props = createProps(PATH_TO_FILE, PWD, 120L, 0L, DEFAULT_MONITOR_DIR_VALUE, DEFAULT_UPDATE_TRIGGER,
                            0L, DEFAULT_VALIDATION_KEY_ELEMENT, DEFAULT_VALIDATION_FILENAME, DEFAULT_VALIDATION_PASSWORD, DEFAULT_VALIDATION_VALID_UNTIL_DATE);

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

    private Map<String, Object> createProps(String filePath, String password, long expiration, long monitorInterval, boolean monitorValidationKeysDir, String updateTrigger,
                                            long expDiffAllowed, String validationKey, String validationKeyFileName, String validationKeyPassword,
                                            String validationKeyValidUntilDate) {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(LTPAConfiguration.CFG_KEY_IMPORT_FILE, filePath);
        props.put(LTPAConfiguration.CFG_KEY_PASSWORD, new SerializableProtectedString(password.toCharArray()));
        props.put(LTPAConfiguration.CFG_KEY_TOKEN_EXPIRATION, expiration);
        props.put(LTPAConfiguration.CFG_KEY_MONITOR_INTERVAL, monitorInterval);
        props.put(LTPAConfiguration.CFG_KEY_MONITOR_VALIDATION_KEYS_DIR, monitorValidationKeysDir);
        props.put(LTPAConfiguration.CFG_KEY_UPDATE_TRIGGER, updateTrigger);
        props.put(LTPAConfigurationImpl.KEY_EXP_DIFF_ALLOWED, expDiffAllowed);

        // Create one validation key in props
        props.put(LTPAConfiguration.CFG_KEY_VALIDATION_KEYS + ".0." + LTPAConfiguration.CFG_KEY_VALIDATION_FILE_NAME, validationKeyFileName);
        props.put(LTPAConfiguration.CFG_KEY_VALIDATION_KEYS + ".0." + LTPAConfiguration.CFG_KEY_VALIDATION_PASSWORD,
                  new SerializableProtectedString(validationKeyPassword.toCharArray()));
        props.put(LTPAConfiguration.CFG_KEY_VALIDATION_KEYS + ".0." + LTPAConfiguration.CFG_KEY_VALIDATION_VALID_UNTIL_DATE, validationKeyValidUntilDate);

        return props;
    }

    private void setupLocationServiceExpectations(final int numberOfInvocations) {
        mock.checking(new Expectations() {
            {
                exactly(numberOfInvocations).of(locateService).resolveString(DEFAULT_OUTPUT_LOCATION);
                will(returnValue(RESOLVED_DEFAULT_OUTPUT_LOCATION));
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

        /**
         * @return the first validation key's file name
         */
        public String getFirstValidationKeyFileName() {
            return getValidationKeys().get(0).getProperty(CFG_KEY_VALIDATION_FILE_NAME);
        }

        /**
         * @return the first validation key's file password
         */
        public String getFirstValidationKeyPassword() {
            return getValidationKeys().get(0).getProperty(CFG_KEY_VALIDATION_PASSWORD);
        }

        /**
         * @return the first validation key's not use after date
         */
        public String getFirstValidationKeyValidUntilDate() {
            return getValidationKeys().get(0).getProperty(CFG_KEY_VALIDATION_VALID_UNTIL_DATE);
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
                     PATH_TO_FILE, ltpaConfig.getPrimaryKeyFile());
    }

    /**
     * Tests that the file monitor is registered and set in the LTPAConfigImpl object when monitorInterval is set.
     */
    @Test
    public void fileMonitorRegistration_monitorInterval() throws Exception {
        setupExecutorServiceExpectations(1);
        setupLocationServiceExpectations(1);
        setupFileMonitorRegistrationsExpectations(1);

        props.put(LTPAConfiguration.CFG_KEY_MONITOR_INTERVAL, 5L);
        LTPAConfigurationImplTestDouble ltpaConfig = createActivatedLTPAConfigurationImpl();

        assertTrue("The LTPA file monitor registration must be set.", ltpaConfig.wasSetFileMonitorRegistrationCalled);
    }

    /**
     * Tests that the file monitor is registered and set in the LTPAConfigImpl object when monitorValidationKeysDir is set.
     */
    @Test
    public void fileMonitorRegistration_monitorValidationKeysDir() throws Exception {
        setupExecutorServiceExpectations(1);
        setupLocationServiceExpectations(1);
        setupFileMonitorRegistrationsExpectations(1);

        props.put(LTPAConfiguration.CFG_KEY_MONITOR_VALIDATION_KEYS_DIR, true);
        LTPAConfigurationImplTestDouble ltpaConfig = createActivatedLTPAConfigurationImpl();

        assertTrue("The LTPA file monitor registration must be set.", ltpaConfig.wasSetFileMonitorRegistrationCalled);
    }

    /**
     * Tests that the file monitor is registered and set in the LTPAConfigImpl object when updateTrigger is set to mbean.
     */
    @Test
    public void fileMonitorRegistration_updateTriggerMbean() throws Exception {
        setupExecutorServiceExpectations(1);
        setupLocationServiceExpectations(1);
        setupFileMonitorRegistrationsExpectations(1);

        props.put(LTPAConfiguration.CFG_KEY_UPDATE_TRIGGER, "mbean");
        LTPAConfigurationImplTestDouble ltpaConfig = createActivatedLTPAConfigurationImpl();

        assertTrue("The LTPA file monitor registration must be set.", ltpaConfig.wasSetFileMonitorRegistrationCalled);
    }
    
    /**
     * Tests that the file monitor is not registered and set in the LTPAConfigImpl object when updateTrigger is set to disabled.
     */
    @Test
    public void fileMonitorRegistration_updateTriggerDisabled() throws Exception {
        setupExecutorServiceExpectations(1);
        setupLocationServiceExpectations(1);
        setupFileMonitorRegistrationsExpectations(0);

        props.put(LTPAConfiguration.CFG_KEY_UPDATE_TRIGGER, "disabled");
        LTPAConfigurationImplTestDouble ltpaConfig = createActivatedLTPAConfigurationImpl();

        assertTrue("The LTPA file monitor registration must not be set.", !ltpaConfig.wasSetFileMonitorRegistrationCalled);
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

    /**
     * Test method for {@link com.ibm.ws.security.token.ltpa.internal.LTPAConfigurationImpl#getMonitorInterval()}.
     */
    @Test
    public void getMonitorInterval() {
        assertEquals("The monitorInterval value was not the expected value",
                     0L, ltpaConfig.getMonitorInterval());
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.ltpa.internal.LTPAConfigurationImpl#getMonitorValidationKeysDir()}.
     */
    @Test
    public void getMonitorValidationKeysDir() {
        assertEquals("The monitorValidationKeysDir value was not the expected value",
                     DEFAULT_MONITOR_DIR_VALUE, ltpaConfig.getMonitorValidationKeysDir());
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.ltpa.internal.LTPAConfigurationImpl#getUpdateTrigger()}.
     */
    @Test
    public void getUpdateTrigger() {
        assertEquals("The updateTrigger value was not the expected value",
                     DEFAULT_UPDATE_TRIGGER, ltpaConfig.getUpdateTrigger());
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.ltpa.internal.LTPAConfigurationImpl#getValidationKeys()}.
     */
    @Test
    public void getValidationKeys() {
        assertEquals("The validationKeys value was not the expected value",
                     "[{fileName=/path/to/validation.keys, password=pwd, validUntilDate=2099-01-01T00:00:00Z}]", ltpaConfig.getValidationKeys().toString());
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.ltpa.internal.LTPAConfigurationImpl#getFirstValidationKeyFileName()}.
     */
    @Test
    public void getFirstValidationKeyFileName() {
        assertEquals("The first Validation Keys' file name value was not the expected value",
                     PATH_TO_DIR + DEFAULT_VALIDATION_FILENAME, ltpaConfig.getFirstValidationKeyFileName());
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.ltpa.internal.LTPAConfigurationImpl#getFirstValidationKeyPassword()}.
     */
    @Test
    public void getFirstValidationKeyPassword() {
        assertEquals("The first Validation Keys' password value was not the expected value",
                     DEFAULT_VALIDATION_PASSWORD, ltpaConfig.getFirstValidationKeyPassword());
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.ltpa.internal.LTPAConfigurationImpl#getFirstValidationKeyValidUntilDate()}.
     */
    @Test
    public void getFirstValidationKeyValidUntilDate() {
        assertEquals("The first Validation Keys' validUntilDate value was not the expected value",
                     DEFAULT_VALIDATION_VALID_UNTIL_DATE, ltpaConfig.getFirstValidationKeyValidUntilDate());
    }

    @Test
    public void modified() {
        setupExecutorServiceExpectations(1);
        setupLocationServiceExpectations(1);
        //setupFileMonitorRegistrationsExpectations(1);

        props.put(LTPAConfiguration.CFG_KEY_IMPORT_FILE, PATH_TO_ANOTHER_FILE);
        ltpaConfig.modified(props);
    }

    @Test
    public void modified_monitorIntervalSet_everythingElseTheSame_keysNotReloaded() {
        setupExecutorServiceExpectations(1);
        setupLocationServiceExpectations(1);
        setupFileMonitorRegistrationsExpectations(1);

        props.put(LTPAConfiguration.CFG_KEY_MONITOR_INTERVAL, 5000L);
        ltpaConfig.modified(props);

        assertTrue("Expected CWWKS4107A message was not logged", outputMgr.checkForStandardOut("CWWKS4107A:.*"));
    }

    @Test
    public void modified_monitorValidationKeysDirSet_everythingElseTheSame_keysReloaded() {
        setupExecutorServiceExpectations(1);
        setupLocationServiceExpectations(1);
        setupFileMonitorRegistrationsExpectations(1);

        props.put(LTPAConfiguration.CFG_KEY_MONITOR_VALIDATION_KEYS_DIR, true);
        ltpaConfig.modified(props);

        assertTrue("Expected CWWKS4107A message was not logged", outputMgr.checkForStandardOut("CWWKS4107A:.*"));
    }

    @Test
    public void modified_monitorIntervalSame_fileChanged_unregistersListenerAndCreatesKeys() throws Exception {
        setupExecutorServiceExpectations(2);
        setupLocationServiceExpectations(2);
        setupFileMonitorRegistrationsExpectations(2);

        props.put(LTPAConfiguration.CFG_KEY_MONITOR_INTERVAL, 5L);
        LTPAConfigurationImplTestDouble ltpaConfig = createActivatedLTPAConfigurationImpl();

        props.put(LTPAConfiguration.CFG_KEY_IMPORT_FILE, PATH_TO_ANOTHER_FILE);
        ltpaConfig.modified(props);
        assertTrue("The old file monitor must be unset.", ltpaConfig.wasUnsetFileMonitorRegistrationCalled);
        assertTrue("Expected CWWKS4107A message was not logged",
                   outputMgr.checkForStandardOut("CWWKS4107A:.*" + PATH_TO_ANOTHER_FILE));
    }

    @Test
    public void modified_monitorValidationKeysDirSame_fileChanged_unregistersListenerAndCreatesKeys() throws Exception {
        setupExecutorServiceExpectations(2);
        setupLocationServiceExpectations(2);
        setupFileMonitorRegistrationsExpectations(2);

        props.put(LTPAConfiguration.CFG_KEY_MONITOR_VALIDATION_KEYS_DIR, true);
        LTPAConfigurationImplTestDouble ltpaConfig = createActivatedLTPAConfigurationImpl();

        props.put(LTPAConfiguration.CFG_KEY_IMPORT_FILE, PATH_TO_ANOTHER_FILE);
        ltpaConfig.modified(props);
        assertTrue("The old file monitor must be unset.", ltpaConfig.wasUnsetFileMonitorRegistrationCalled);
        assertTrue("Expected CWWKS4107A message was not logged",
                   outputMgr.checkForStandardOut("CWWKS4107A:.*" + PATH_TO_ANOTHER_FILE));
    }

    @Test
    public void modified_fileAndMonitorIntervalChanged_unregistersListenerAndCreatesKeys() throws Exception {
        setupExecutorServiceExpectations(2);
        setupLocationServiceExpectations(2);
        setupFileMonitorRegistrationsExpectations(2);

        props.put(LTPAConfiguration.CFG_KEY_MONITOR_INTERVAL, 5L);
        LTPAConfigurationImplTestDouble ltpaConfig = createActivatedLTPAConfigurationImpl();

        props.put(LTPAConfiguration.CFG_KEY_IMPORT_FILE, PATH_TO_ANOTHER_FILE);
        props.put(LTPAConfiguration.CFG_KEY_MONITOR_INTERVAL, 10L);
        ltpaConfig.modified(props);
        assertTrue("The old file monitor must be unset.", ltpaConfig.wasUnsetFileMonitorRegistrationCalled);
        assertTrue("Expected CWWKS4107A message was not logged",
                   outputMgr.checkForStandardOut("CWWKS4107A:.*" + PATH_TO_ANOTHER_FILE));
    }

    @Test
    public void modified_fileAndMonitorValidationKeysDirChanged_unregistersListenerAndCreatesKeys() throws Exception {
        setupExecutorServiceExpectations(2);
        setupLocationServiceExpectations(2);
        setupFileMonitorRegistrationsExpectations(1);

        props.put(LTPAConfiguration.CFG_KEY_MONITOR_VALIDATION_KEYS_DIR, true);
        LTPAConfigurationImplTestDouble ltpaConfig = createActivatedLTPAConfigurationImpl();

        props.put(LTPAConfiguration.CFG_KEY_IMPORT_FILE, PATH_TO_ANOTHER_FILE);
        props.put(LTPAConfiguration.CFG_KEY_MONITOR_VALIDATION_KEYS_DIR, false);
        ltpaConfig.modified(props);
        assertTrue("The old file monitor must be unset.", ltpaConfig.wasUnsetFileMonitorRegistrationCalled);
        assertTrue("Expected CWWKS4107A message was not logged",
                   outputMgr.checkForStandardOut("CWWKS4107A:.*" + PATH_TO_ANOTHER_FILE));
    }

    @Test
    public void modified_fileChanged_monitorIntervalSetToZero_unregistersListenerAndCreatesKeys() throws Exception {
        setupExecutorServiceExpectations(2);
        setupLocationServiceExpectations(2);
        setupFileMonitorRegistrationsExpectations(1);

        props.put(LTPAConfiguration.CFG_KEY_MONITOR_INTERVAL, 5L);
        LTPAConfigurationImplTestDouble ltpaConfig = createActivatedLTPAConfigurationImpl();

        props.put(LTPAConfiguration.CFG_KEY_IMPORT_FILE, PATH_TO_ANOTHER_FILE);
        props.put(LTPAConfiguration.CFG_KEY_MONITOR_INTERVAL, 0L);
        ltpaConfig.modified(props);
        assertTrue("The old file monitor must be unset.", ltpaConfig.wasUnsetFileMonitorRegistrationCalled);
        assertTrue("Expected CWWKS4107A message was not logged",
                   outputMgr.checkForStandardOut("CWWKS4107A:.*" + PATH_TO_ANOTHER_FILE));
    }

    @Test
    public void modified_monitorIntervalSetToZero_everythingElseTheSame_unregistersListener() throws Exception {
        setupExecutorServiceExpectations(2);
        setupLocationServiceExpectations(2);
        setupFileMonitorRegistrationsExpectations(1);

        props.put(LTPAConfiguration.CFG_KEY_MONITOR_INTERVAL, 5L);
        LTPAConfigurationImplTestDouble ltpaConfig = createActivatedLTPAConfigurationImpl();

        props.put(LTPAConfiguration.CFG_KEY_MONITOR_INTERVAL, 0L);
        ltpaConfig.modified(props);
        assertTrue("The old file monitor must be unset.", ltpaConfig.wasUnsetFileMonitorRegistrationCalled);
        assertTrue("Expected CWWKS4107A message was not logged", outputMgr.checkForStandardOut("CWWKS4107A:.*"));
    }

    @Test
    public void modified_passwordChanged_doNotUnregisterOrCreateKeys() throws Exception {
        setupExecutorServiceExpectations(2);
        setupLocationServiceExpectations(2);
        setupFileMonitorRegistrationsExpectations(2);

        props.put(LTPAConfiguration.CFG_KEY_MONITOR_INTERVAL, 5L);
        LTPAConfigurationImplTestDouble ltpaConfig = createActivatedLTPAConfigurationImpl();

        props.put(LTPAConfiguration.CFG_KEY_PASSWORD, new SerializableProtectedString(ANOTHER_PWD.toCharArray()));
        ltpaConfig.modified(props);
        assertTrue("The old file monitor must be unset.", ltpaConfig.wasUnsetFileMonitorRegistrationCalled);
    }

    @Test
    public void configReady() throws Exception {
        setupExecutorServiceExpectations(1);
        setupLocationServiceExpectations(1);
        //setupFileMonitorRegistrationsExpectations(1);

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
        setupLocationServiceExpectations(1);
        //setupFileMonitorRegistrationsExpectations(1);
        final WsResource keysFileInServerConfig = mock.mock(WsResource.class);
        mock.checking(new Expectations() {
            {
                one(locateService).resolveResource(DEFAULT_CONFIG_LOCATION);
                will(returnValue(keysFileInServerConfig));
                one(locateService).resolveString(DEFAULT_CONFIG_LOCATION);
                will(returnValue(RESOLVED_DEFAULT_CONFIG_LOCATION));
                one(keysFileInServerConfig).exists();
                will(returnValue(true));
            }
        });

        props.put(LTPAConfiguration.CFG_KEY_IMPORT_FILE, RESOLVED_DEFAULT_OUTPUT_LOCATION);
        LTPAConfigurationImplTestDouble ltpaConfig = createActivatedLTPAConfigurationImpl();
        assertEquals("Key file value was not the expected value",
                     RESOLVED_DEFAULT_CONFIG_LOCATION, ltpaConfig.getPrimaryKeyFile());
    }

    @Test
    public void keyFileFromOutputDirWhenDefaultLocationNotOverriddenAndKeysFileNotInConfigDir() {
        setupExecutorServiceExpectations(1);
        setupLocationServiceExpectations(1);
        //setupFileMonitorRegistrationsExpectations(1);
        mock.checking(new Expectations() {
            {
                one(locateService).resolveResource(DEFAULT_CONFIG_LOCATION);
                will(returnValue(null));
            }
        });

        props.put(LTPAConfiguration.CFG_KEY_IMPORT_FILE, RESOLVED_DEFAULT_OUTPUT_LOCATION);
        LTPAConfigurationImplTestDouble ltpaConfig = createActivatedLTPAConfigurationImpl();
        assertEquals("Key file value was not the expected value",
                     RESOLVED_DEFAULT_OUTPUT_LOCATION, ltpaConfig.getPrimaryKeyFile());
    }

    @Test
    public void maskKeysPasswords_replacesPasswordWithMask() {
        setupExecutorServiceExpectations(1);
        setupLocationServiceExpectations(1);
        // setupFileMonitorRegistrationsExpectations(1);

        final String originalPassword = "{xor}Lz4sLCgwLTs=";
        final String maskedPassword = "*not null*";
        props.put(LTPAConfiguration.CFG_KEY_PASSWORD, new SerializableProtectedString(originalPassword.toCharArray()));

        LTPAConfigurationImplTestDouble ltpaConfig = createActivatedLTPAConfigurationImpl();

        Properties inputProps = new Properties();
        inputProps.setProperty("password", originalPassword);
        List<Properties> inputList = new ArrayList<>();
        inputList.add(inputProps);

        Properties expectedProps = new Properties();
        expectedProps.setProperty("password", maskedPassword);
        List<Properties> expectedList = new ArrayList<>();
        expectedList.add(expectedProps);

        // Act
        List<Properties> outputList = ltpaConfig.maskKeysPasswords(inputList);

        // Assert
        assertEquals("The password was not masked correctly", expectedList, outputList);   
    }
}
