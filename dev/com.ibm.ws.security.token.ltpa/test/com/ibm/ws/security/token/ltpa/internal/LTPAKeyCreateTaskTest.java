/*******************************************************************************
 * Copyright (c) 2012, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa.internal;

import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

import com.ibm.ws.security.token.ltpa.LTPAConfiguration;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

/**
 *
 */
@SuppressWarnings("unchecked")
public class LTPAKeyCreateTaskTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    private static final String DEFAULT_OUTPUT_LOCATION = "${server.output.dir}/resources/security/ltpa.keys";
    private static final String RESOLVED_DEFAULT_OUTPUT_LOCATION = "testServerName/resources/security/ltpa.keys";
    private static String TEST_FILE_NAME = "testFileName";

    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final BundleContext bc = mock.mock(BundleContext.class);
    private final ServiceReference<ExecutorService> executorServiceRef = mock.mock(ServiceReference.class, "executorServiceRef");
    private final ExecutorService executorService = mock.mock(ExecutorService.class);
    private final ServiceReference<WsLocationAdmin> locationServiceRef = mock.mock(ServiceReference.class, "locationServiceRef");
    private final WsLocationAdmin locationService = mock.mock(WsLocationAdmin.class);
    private Map<String, Object> props;

    private class LTPAKeyCreatorDouble extends LTPAKeyCreateTask {
        LTPAKeyCreatorDouble(WsLocationAdmin locService, LTPAConfigurationImpl config) {
            super(locService, config);
        }

        @Override
        void createRequiredCollaborators() throws Exception {}
    }

    @Before
    public void setUp() {
        setupCommonExpectations();
        setupProps();
    }

    private void setupCommonExpectations() {
        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(LTPAConfigurationImpl.KEY_LOCATION_SERVICE, locationServiceRef);
                will(returnValue(locationService));
                allowing(cc).locateService(LTPAConfigurationImpl.KEY_EXECUTOR_SERVICE, executorServiceRef);
                will(returnValue(executorService));
            }
        });
    }

    private void setupProps() {
        props = new HashMap<String, Object>();
        props.put(LTPAConfiguration.CFG_KEY_IMPORT_FILE, TEST_FILE_NAME);
        props.put(LTPAConfiguration.CFG_KEY_PASSWORD, new SerializableProtectedString("notUsed".toCharArray()));
        props.put(LTPAConfiguration.CFG_KEY_TOKEN_EXPIRATION, 0L);
        props.put(LTPAConfiguration.CFG_KEY_MONITOR_INTERVAL, 0L);
    }

    private void setupLocationServiceExpecatations() {
        mock.checking(new Expectations() {
            {
                one(locationService).resolveString(DEFAULT_OUTPUT_LOCATION);
                will(returnValue(RESOLVED_DEFAULT_OUTPUT_LOCATION));
            }
        });
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.ltpa.internal.LTPAKeyCreateTask#call()}.
     *
     * This method tests the behaviour when call is "finished" after we the
     * component has been deactivated.
     *
     */
    @Test
    public void call_afterDeactivate() throws Exception {
        mock.checking(new Expectations() {
            {
                one(executorService).execute(with(any(Runnable.class)));
            }
        });
        setupLocationServiceExpecatations();

        LTPAConfigurationImpl config = setupActivatedLTPAConfiguration(new LTPAConfigurationImpl() {
            @Override
            public void configReady() {
                throw new IllegalStateException();
            }
        });
        config.deactivate(cc);

        LTPAKeyCreateTask creator = new LTPAKeyCreatorDouble(null, config);

        creator.run();
    }

    private LTPAConfigurationImpl setupActivatedLTPAConfiguration(LTPAConfigurationImpl config) {
        config.setExecutorService(executorServiceRef);
        config.setLocationService(locationServiceRef);
        config.activate(cc, props);
        return config;
    }

    /**
     * Test method for {@link com.ibm.ws.security.token.ltpa.internal.LTPAKeyCreateTask#call()}.
     *
     * This method is a bit on the gross side, triggering an NPE, but all we care about
     * is the message that gets output.
     */
    @Test
    public void callLogsError() {
        mock.checking(new Expectations() {
            {
                allowing(cc).getBundleContext();
                one(executorService).execute(with(any(Runnable.class)));
            }
        });
        setupLocationServiceExpecatations();

        LTPAConfigurationImpl config = setupActivatedLTPAConfiguration(new LTPAConfigurationImpl());

        LTPAKeyCreateTask creator = new LTPAKeyCreateTask(null, config);

        creator.run();
        assertTrue("Expected error message was not logged",
                   outputMgr.checkForStandardErr("CWWKS4106E: LTPA configuration error. Unable to create or read LTPA key file: " + TEST_FILE_NAME));
    }

    @Test
    public void call_successful() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(cc).getBundleContext();
                will(returnValue(bc));
                one(executorService).execute(with(any(Runnable.class)));
            }
        });
        setupLocationServiceExpecatations();

        final LTPAConfigurationImplDouble config = (LTPAConfigurationImplDouble) setupActivatedLTPAConfiguration(new LTPAConfigurationImplDouble());

        LTPAKeyCreateTask creator = new LTPAKeyCreatorDouble(null, config);
        mock.checking(new Expectations() {
            {
                one(bc).registerService(with(LTPAConfiguration.class),
                                        with(config),
                                        with(any(Dictionary.class)));
            }
        });

        creator.run();

        // Check for the first part of CWWKS4105I
        String expectedMessage = "CWWKS4105I: LTPA configuration is ready after";
        assertTrue("Expected first part CWWKS4105I message was not logged",
                   outputMgr.checkForMessages(expectedMessage));

        // Check for the second part of CWWKS4105I
        expectedMessage = "seconds.";
        assertTrue("Expected second part CWWKS4105I message was not logged",
                   outputMgr.checkForMessages(expectedMessage));

        // Check that the configReady method was called on the config object
        assertTrue("The configuration must be ready.", config.wasConfigReadyCalled);
    }

    private class LTPAConfigurationImplDouble extends LTPAConfigurationImpl {
        public boolean wasConfigReadyCalled = false;

        @Override
        protected void configReady() {
            wasConfigReadyCalled = true;
        }
    }
}
