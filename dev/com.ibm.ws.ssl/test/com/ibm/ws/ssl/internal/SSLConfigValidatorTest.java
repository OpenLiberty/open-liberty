/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ssl.internal;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.ssl.Constants;
import com.ibm.ws.ssl.config.WSKeyStore;

import test.common.SharedOutputManager;

/**
 *
 */
public class SSLConfigValidatorTest {
    private static final SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery();
    private final ConfigurationAdmin configAdmin = mock.mock(ConfigurationAdmin.class);
    private final Configuration config = mock.mock(Configuration.class);
    private final ScheduledExecutorService executorService = mock.mock(ScheduledExecutorService.class);
    private final ScheduledFuture<?> scheduled = mock.mock(ScheduledFuture.class);
    private SSLConfigValidator validator;

    @Before
    public void setUp() {
        validator = new SSLConfigValidator();
        validator.setConfigAdmin(configAdmin);
        validator.setExecutorService(executorService);
    }

    @After
    public void tearDown() {
        validator.unsetConfigAdmin(configAdmin);
        validator.unsetExecutorService(executorService);

        mock.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.SSLConfigValidator#validate(java.util.Map, java.util.Map, java.util.Map)}.
     */
    @Test
    public void validate_scheduled_delay_when_executor_available() {
        mock.checking(new Expectations() {
            {
                one(executorService).schedule(with(any(Runnable.class)), with(60L), with(TimeUnit.SECONDS));
            }
        });

        validator.validate(null, null, null);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.SSLConfigValidator#validate(java.util.Map, java.util.Map, java.util.Map)}.
     */
    @Test
    public void validate_schedule_cancels_previous_when_executor_available() {
        mock.checking(new Expectations() {
            {
                one(executorService).schedule(with(any(Runnable.class)), with(60L), with(TimeUnit.SECONDS));
                will(returnValue(scheduled));

                one(scheduled).cancel(false);

                one(executorService).schedule(with(any(Runnable.class)), with(60L), with(TimeUnit.SECONDS));
            }
        });

        validator.validate(null, null, null);
        validator.validate(null, null, null);
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.SSLConfigValidator#validate(java.util.Map, java.util.Map, java.util.Map)}.
     */
    @Test
    public void validate_immediate_execution_when_no_executor_available() {
        mock.checking(new Expectations() {
            {
                never(executorService).schedule(with(any(Runnable.class)), with(60L), with(TimeUnit.SECONDS));
            }
        });

        SSLConfigValidator validator = new SSLConfigValidator();
        validator.validate(Collections.<String, Object> emptyMap(), Collections.<String, Map<String, Object>> emptyMap(), Collections.<String, WSKeyStore> emptyMap());
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.SSLConfigValidator#validate(java.util.Map, java.util.Map, java.util.Map)}.
     *
     * @throws Exception
     */
    @Test
    public void validate_defaultSSLConfig_repertoire_not_defined() throws Exception {
        mock.checking(new Expectations() {
            {
                one(configAdmin).listConfigurations("(" + org.osgi.framework.Constants.SERVICE_PID + "=com.ibm.ws.ssl.repertoire*)");
                will(returnValue(null));
            }
        });

        validator.unsetExecutorService(executorService); // Force immediate run

        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.SSLPROP_DEFAULT_ALIAS, LibertyConstants.DEFAULT_SSL_CONFIG_ID);
        validator.validate(map, Collections.<String, Map<String, Object>> emptyMap(), Collections.<String, WSKeyStore> emptyMap());

        // No message issued - no action taken, nothing to validate
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.SSLConfigValidator#validate(java.util.Map, java.util.Map, java.util.Map)}.
     *
     * @throws Exception
     */
    @Test
    public void validate_defaultSSLConfig_repertoire_unsatisfied_no_defaultKeyStore() throws Exception {
        final Dictionary<String, Object> configProps = new Hashtable<String, Object>();
        configProps.put("id", "defaultSSLConfig");
        configProps.put("keyStoreRef", "defaultKeyStore");
        mock.checking(new Expectations() {
            {
                one(configAdmin).listConfigurations("(" + org.osgi.framework.Constants.SERVICE_PID + "=com.ibm.ws.ssl.repertoire*)");
                will(returnValue(new Configuration[] { config }));

                one(config).getProperties();
                will(returnValue(configProps));
            }
        });

        validator.unsetExecutorService(executorService); // Force immediate run

        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.SSLPROP_DEFAULT_ALIAS, LibertyConstants.DEFAULT_SSL_CONFIG_ID);
        validator.validate(map, Collections.<String, Map<String, Object>> emptyMap(), Collections.<String, WSKeyStore> emptyMap());

        assertTrue("Expected warning about defaultSSLConfig and defaultKeyStore availability (CWPKI0817W)",
                   outputMgr.checkForMessages("CWPKI0817W.*defaultKeyStore"));
    }

    /**
     * Test method for {@link com.ibm.ws.ssl.internal.SSLConfigValidator#validate(java.util.Map, java.util.Map, java.util.Map)}.
     *
     * @throws Exception
     */
    @Test
    public void validate_defaultSSLConfig_repertoire_unsatisfied_no_keystore() throws Exception {
        final Dictionary<String, Object> configProps = new Hashtable<String, Object>();
        configProps.put("id", "defaultSSLConfig");
        configProps.put("keyStoreRef", "otherKeyStore");
        mock.checking(new Expectations() {
            {
                one(configAdmin).listConfigurations("(" + org.osgi.framework.Constants.SERVICE_PID + "=com.ibm.ws.ssl.repertoire*)");
                will(returnValue(new Configuration[] { config }));

                one(config).getProperties();
                will(returnValue(configProps));
            }
        });

        validator.unsetExecutorService(executorService); // Force immediate run

        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Constants.SSLPROP_DEFAULT_ALIAS, LibertyConstants.DEFAULT_SSL_CONFIG_ID);
        validator.validate(map, Collections.<String, Map<String, Object>> emptyMap(), Collections.<String, WSKeyStore> emptyMap());

        assertTrue("Expected error about defaultSSLConfig pointing to undefined keystore (CWPKI0818E)",
                   outputMgr.checkForMessages("CWPKI0818E.*otherKeyStore"));
    }

}
