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
package com.ibm.ws.security.registry.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryService;

import test.common.SharedOutputManager;

/**
 * Test the UserRegistryService in the context of no configuration.
 */
@SuppressWarnings("unchecked")
public class UserRegistryServiceImplWithAutoDetectTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext componentContext = mock.mock(ComponentContext.class);
    private final Dictionary<String, Object> dict = new Hashtable<String, Object>();
    private final Map<String, Object> basicConfigMap = new HashMap<String, Object>();
    private final ServiceReference<UserRegistry> ur1Ref = mock.mock(ServiceReference.class, "basic1 config ref");
    private final UserRegistry ur1 = mock.mock(UserRegistry.class, "ur1");
    private final ServiceReference<UserRegistry> ef = mock.mock(ServiceReference.class, "basic2 config ref");
    private final UserRegistry ur2 = mock.mock(UserRegistry.class, "ur2");
    private UserRegistryServiceImpl service;

    /**
     * Perform some setup to simulate the service starting.
     */
    @Before
    public void setUp() {
        mock.checking(new Expectations() {
            {
                allowing(componentContext).getProperties();
                will(returnValue(dict));

                allowing(ur1Ref).getProperty(UserRegistryServiceImpl.KEY_CONFIG_ID);
                will(returnValue("basicConfig1"));
                allowing(ur1Ref).getProperty(UserRegistryService.REGISTRY_TYPE);
                will(returnValue("Basic"));
                allowing(ur1Ref).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(ur1Ref).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(ef).getProperty(UserRegistryServiceImpl.KEY_CONFIG_ID);
                will(returnValue("basicConfig2"));
                allowing(ef).getProperty(UserRegistryService.REGISTRY_TYPE);
                will(returnValue("Basic"));
                allowing(ef).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(ef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
            }
        });

        basicConfigMap.put(UserRegistryService.REGISTRY_TYPE, "Basic");

    }

    /**
     * Perform some cleanup to simulate the service stopping.
     */
    @After
    public void tearDown() {
        mock.assertIsSatisfied();

        service.deactivate(componentContext);
        service.unsetUserRegistry(ur1Ref);
        service = null;
    }

    /**
     * If no UserRegistryConfiguration service is available, return false.
     */
    @Test
    public void isUserRegistryConfigured_noServiceAvailable() throws Exception {
        service = new UserRegistryServiceImpl();
        service.activate(componentContext, Collections.<String, Object> emptyMap());

        assertFalse("UserRegistry is not configed",
                    service.isUserRegistryConfigured());
        assertFalse("No messages should be logged (error or otherwise)",
                    outputMgr.checkForMessages("CWWKS"));
    }

    /**
     * If one UserRegistryConfiguration service is available, return true.
     */
    @Test
    public void isUserRegistryConfigured_oneServiceAvailable() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(componentContext).locateService(UserRegistryServiceImpl.KEY_USER_REGISTRY, ur1Ref);
                will(returnValue(ur1));
                allowing(ur1).getRealm();
                will(returnValue("testRealm"));
            }
        });
        service = new UserRegistryServiceImpl();
        service.setUserRegistry(ur1Ref);
        service.activate(componentContext, Collections.<String, Object> emptyMap());

        assertTrue("Should see a valid instance returned", service.isUserRegistryConfigured());
        assertFalse("No messages should be logged (error or otherwise)",
                    outputMgr.checkForMessages("CWWKS"));
    }

    /**
     * If multiple UserRegistryConfiguration service is available, it used to throw an exception but now
     * returning false. This modification is made in order to eliminate ffdc data generation.
     */
    @Test
    public void isUserRegistryConfigured_multipleServiceAvailable() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(componentContext).locateService("UserRegistry", ef);
                will(returnValue(ur2));

                allowing(componentContext).locateService("UserRegistry", ur1Ref);
                will(returnValue(ur1));

                allowing(ur1).getRealm();
                will(returnValue("ur1"));
                allowing(ur2).getRealm();
                will(returnValue("ur2"));
            }
        });

        service = new UserRegistryServiceImpl();
        service.setUserRegistry(ur1Ref);
        service.setUserRegistry(ef);
        service.activate(componentContext, Collections.<String, Object> singletonMap("UserRegistry.cardinality.minimum", "2"));

        try {
            assertFalse("Should see a valid instance returned", service.isUserRegistryConfigured());
        } catch (RegistryException e) {
            fail("RegistryException no longer thrown when multiple services are available");
        }
    }

    /**
     * If no UserRegistryConfiguration service is available, throw a
     * RegistryException and log an error message.
     */
    @Test
    public void getUserRegistry_noServiceAvailable() throws Exception {
        service = new UserRegistryServiceImpl();
        service.activate(componentContext, Collections.<String, Object> emptyMap());

        try {
            service.getUserRegistry();
            fail("Expected RegistryException when no service is available");
        } catch (RegistryException e) {
            assertEquals("Exception contained the wrong message",
                         "CWWKS3005E: A configuration exception has occurred. No UserRegistry implementation service is available.  Ensure that you have a user registry configured.",
                         e.getMessage());

            assertTrue("Expected error message was not logged",
                       outputMgr.checkForStandardErr("CWWKS3005E: A configuration exception has occurred. No UserRegistry implementation service is available.  Ensure that you have a user registry configured."));
        }
    }

    /**
     * If one UserRegistryConfiguration service is available, return the instance.
     */
    @Test
    public void getUserRegistry_oneServiceAvailable() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(componentContext).locateService(UserRegistryServiceImpl.KEY_USER_REGISTRY, ur1Ref);
                will(returnValue(ur1));
                allowing(ur1).getRealm();
                will(returnValue("testRealm"));
            }
        });
        service = new UserRegistryServiceImpl();
        service.setUserRegistry(ur1Ref);
        service.activate(componentContext, Collections.<String, Object> emptyMap());

        assertNotNull("Should see a valid instance returned", service.getUserRegistry());
        assertFalse("No messages should be logged (error or otherwise)",
                    outputMgr.checkForMessages("CWWKS"));
    }

    /**
     * If multiple UserRegistryConfiguration service is available, throw a
     * RegistryException and log an error message.
     */
    @Test
    public void getUserRegistry_multipleServiceAvailable() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(componentContext).locateService("UserRegistry", ef);
                will(returnValue(ur2));

                allowing(componentContext).locateService("UserRegistry", ur1Ref);
                will(returnValue(ur1));

                allowing(ur1).getRealm();
                will(returnValue("ur1"));
                allowing(ur2).getRealm();
                will(returnValue("ur2"));
            }
        });

        service = new UserRegistryServiceImpl();
        service.setUserRegistry(ur1Ref);
        service.setUserRegistry(ef);
        service.activate(componentContext, Collections.<String, Object> singletonMap("UserRegistry.cardinality.minimum", "2"));

        try {
            service.getUserRegistry();
            fail("Expected RegistryException when multiple services are available");
        } catch (RegistryException e) {
            assertEquals(
                         "Exception contained the wrong message",
                         "CWWKS3006E: A configuration exception has occurred. There are multiple available UserRegistry implementation services; the system cannot determine which to use.",
                         e.getMessage());

            assertTrue(
                       "Expected error message was not logged",
                       outputMgr.checkForStandardErr("CWWKS3006E: A configuration exception has occurred. There are multiple available "
                                                     + "UserRegistry implementation services; the system cannot determine which to use."));
        }
    }
}
