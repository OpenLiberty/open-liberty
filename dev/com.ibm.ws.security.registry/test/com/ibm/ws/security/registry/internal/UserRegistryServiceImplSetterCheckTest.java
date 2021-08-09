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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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

import com.ibm.ws.security.registry.UserRegistry;

import test.common.SharedOutputManager;

@SuppressWarnings("unchecked")
public class UserRegistryServiceImplSetterCheckTest {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery();
    private final ServiceReference<UserRegistry> configRef1 = mock.mock(ServiceReference.class, "configRef1");
    private final ServiceReference<UserRegistry> configRef2 = mock.mock(ServiceReference.class, "configRef2");

    private UserRegistryServiceImpl service;

    @Before
    public void setUp() {
        mock.checking(new Expectations() {
            {

                allowing(configRef1).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(configRef1).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(configRef2).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(configRef2).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
            }
        });
    }

    @After
    public void tearDown() {
        mock.assertIsSatisfied();
    }

    /**
     * Validates that the expected error message was logged for the specified
     * type.
     *
     * @param type
     */
    private void checkNoTypeErrorMessageWasLogged(String serviceId) {
        assertTrue("Expected error message was not logged",
                   outputMgr.checkForStandardErr("CWWKS3004E: An internal exception has occurred. The service " + serviceId
                                                 + " does not define the registry type it implements."));
    }

    /**
     * In the case where a UserRegistryConfiguration service does not define
     * a type, log an error and ignore the service.
     */
    @Test
    public void config_noType() {
        service = new UserRegistryServiceImpl();

        mock.checking(new Expectations() {
            {
                one(configRef1).getProperty(UserRegistryServiceImpl.KEY_TYPE);
                will(returnValue(null));
                one(configRef1).getProperty(UserRegistryServiceImpl.KEY_CONFIG_ID);
                will(returnValue("configID1"));
                one(configRef1).getProperty(UserRegistryServiceImpl.KEY_COMPONENT_NAME);
                will(returnValue("test.config"));
            }
        });
        service.setUserRegistry(configRef1);
        assertEquals("No configuration should have been stored in the config map",
                     0, service.userRegistries.size());
        checkNoTypeErrorMessageWasLogged("test.config");
    }

    /**
     * Validates that the expected error message was logged for the specified
     * type.
     *
     * @param type
     */
    private void checkNoIdErrorMessageWasLogged(String type) {
        assertTrue("Expected error message was not logged",
                   outputMgr.checkForStandardErr("WWKS3003E: A configuration exception has occurred. A configuration for registry type " + type
                                                 + " does not define an ID."));
    }

    /**
     * In the case where a UserRegistryConfiguration service does not define
     * a config id, log an error and ignore the service.
     */
    @Test
    public void config_noId() {
        service = new UserRegistryServiceImpl();

        mock.checking(new Expectations() {
            {
                one(configRef1).getProperty(UserRegistryServiceImpl.KEY_TYPE);
                will(returnValue("type"));
                one(configRef1).getProperty(UserRegistryServiceImpl.KEY_CONFIG_ID);
                will(returnValue(null));
            }
        });
        service.setUserRegistry(configRef1);
        assertEquals("No configuration should have been stored in the config map",
                     0, service.userRegistries.size());
        checkNoIdErrorMessageWasLogged("type");
    }

    /**
     * In the case where a UserRegistryConfiguration service does not define
     * both a type and ID, log both errors and ignore the service.
     */
    @Test
    public void config_noIdAndNoType() {
        service = new UserRegistryServiceImpl();

        mock.checking(new Expectations() {
            {
                one(configRef1).getProperty(UserRegistryServiceImpl.KEY_TYPE);
                will(returnValue(null));
                one(configRef1).getProperty(UserRegistryServiceImpl.KEY_CONFIG_ID);
                will(returnValue(null));
                one(configRef1).getProperty(UserRegistryServiceImpl.KEY_COMPONENT_NAME);
                will(returnValue("test.config"));
            }
        });

        service.setUserRegistry(configRef1);
        assertEquals("No configuration should have been stored in the config map",
                     0, service.userRegistries.size());
        checkNoTypeErrorMessageWasLogged("test.config");
        checkNoIdErrorMessageWasLogged("UNKNOWN");
    }

    /**
     * In the case where two registry services are of different types but define the
     * same ID, validate that the 1st one set is the "current" one and that the 2nd
     * one is ignored (as this is a configuration error).
     */
    //BOGUS TEST.  config ids are unique.
//    @Test
//    public void config_multipleSameIDDifferentType() {
//        service = new UserRegistryServiceImpl();
//
//        mock.checking(new Expectations() {
//            {
//                exactly(3).of(configRef1).getProperty(UserRegistryServiceImpl.KEY_TYPE);
//                will(returnValue("type1"));
//                exactly(2).of(configRef1).getProperty(UserRegistryServiceImpl.KEY_CONFIG_ID);
//                will(returnValue("configID1"));
//                one(configRef2).getProperty(UserRegistryServiceImpl.KEY_TYPE);
//                will(returnValue("type2"));
//                one(configRef2).getProperty(UserRegistryServiceImpl.KEY_CONFIG_ID);
//                will(returnValue("configID1"));
//            }
//        });
//
//        service.setUserRegistry(configRef1);
//        service.setUserRegistry(configRef2);
//        assertEquals("There should be only one configuration in the config map",
//                     1, service.userRegistries.size());
//        assertSame("The ServiceReference should be the first set and the second was ignored",
//                   configRef1, service.userRegistries.getReference("configID1"));
//
//        assertTrue(
//                   "Expected error message was not logged",
//                   outputMgr.checkForStandardErr("CWWKS3007E: A configuration exception has occurred. A configuration of "
//                                                 + "type type2 with ID configID1 conflicts with configuration of type type1 with ID configID1. "
//                                                 + "Ignoring configuration of type type2 with ID configID1."));
//    }

    /**
     * In the case where two registry services are of the same type and define the
     * same ID, validate that the 2nd one set is the "current" one.
     */
    @Test
    public void config_multipleSameIDSameType() {
        service = new UserRegistryServiceImpl();

        mock.checking(new Expectations() {
            {
                exactly(1).of(configRef1).getProperty(UserRegistryServiceImpl.KEY_TYPE);
                will(returnValue("type1"));
                one(configRef1).getProperty(UserRegistryServiceImpl.KEY_CONFIG_ID);
                will(returnValue("configID1"));
                one(configRef2).getProperty(UserRegistryServiceImpl.KEY_TYPE);
                will(returnValue("type1"));
                one(configRef2).getProperty(UserRegistryServiceImpl.KEY_CONFIG_ID);
                will(returnValue("configID1"));
            }
        });

        service.setUserRegistry(configRef1);
        service.setUserRegistry(configRef2);
        assertEquals("There should be only one configuration in the config map",
                     1, service.userRegistries.size());
        assertSame("The ServiceReference should be the second set",
                   configRef2, service.userRegistries.getReference("configID1"));
    }

}
