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
package com.ibm.ws.security.jaas.common.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.annotation.Annotation;
import java.util.Collections;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import test.common.SharedOutputManager;

import com.ibm.ws.security.jaas.common.JAASLoginModuleConfig;

/**
 *
 */
@SuppressWarnings("unchecked")
public class JAASLoginContextEntryImplTest {
    private static SharedOutputManager outputMgr;
    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final ServiceReference<ConfigurationAdmin> configAdminRef = mock.mock(ServiceReference.class);
    private final ConfigurationAdmin configAdmin = mock.mock(ConfigurationAdmin.class);

    static final String CFG_KEY_ID = "id";
    static final String CFG_KEY_LOGIN_MODULE_REF = "loginModuleRef";
    static final String SYSTEM_WEB_INBOUND = "SYSTEM_WEB_INBOUND";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(cc).locateService(JAASLoginModuleConfig.KEY_CONFIGURATION_ADMIN, configAdminRef);
                will(returnValue(configAdmin));
            }
        });
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
        outputMgr.resetStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    @Test
    public void testConstructor() throws Exception {
        JAASLoginContextEntryImpl jaasLoginContextEntryImpl = new JAASLoginContextEntryImpl();
        assertNotNull("We have an instance of jaasLoginContextEntryImpl", jaasLoginContextEntryImpl);
    }

    @Test
    public void testProcessLoginContextEntryProps() {
        EntryConfig entryConfig = new EntryConfig() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String name() {
                return SYSTEM_WEB_INBOUND;
            }

            @Override
            public String[] loginModuleRef() {
                return new String[] { "custom", "hashtable" };
            }

            @Override
            public String JaasLoginModuleConfig_target() {
                return null;
            }

            @Override
            public String JaasLoginModuleConfig_cardinality_minimum() {
                return null;
            }

            @Override
            public String id() {
                return SYSTEM_WEB_INBOUND;
            }
        };
        JAASLoginContextEntryImpl jaasLoginContextEntryImpl = new JAASLoginContextEntryImpl();
        JAASLoginModuleConfig custom = new JAASLoginModuleConfigImpl();
        JAASLoginModuleConfig hashtable = new JAASLoginModuleConfigImpl();
        jaasLoginContextEntryImpl.setJaasLoginModuleConfig(custom, Collections.<String, Object> singletonMap("service.pid", "custom"));
        jaasLoginContextEntryImpl.setJaasLoginModuleConfig(hashtable, Collections.<String, Object> singletonMap("service.pid", "hashtable"));
        jaasLoginContextEntryImpl.activate(entryConfig);
        assertEquals("ID is system.WEB_INBOUND", SYSTEM_WEB_INBOUND, jaasLoginContextEntryImpl.getId());
        assertEquals("There are two login module refs", 2, jaasLoginContextEntryImpl.getLoginModules().size());
        assertEquals("The first login module ref is custom", custom, jaasLoginContextEntryImpl.getLoginModules().get(0));
        assertEquals("The second login module ref is hashtable", hashtable, jaasLoginContextEntryImpl.getLoginModules().get(1));
    }
}
