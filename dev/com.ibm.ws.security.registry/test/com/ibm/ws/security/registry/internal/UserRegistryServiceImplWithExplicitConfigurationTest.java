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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
 * Test the UserRegistryService in the context of configuration
 * data inside the server.xml.
 */
@SuppressWarnings("unchecked")
public class UserRegistryServiceImplWithExplicitConfigurationTest {

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
    private final Hashtable<String, Object> dict = new Hashtable<String, Object>();
    private final Map<String, Object> basicConfigMap = new HashMap<String, Object>();
    private final ServiceReference<UserRegistry> ur1Ref = mock.mock(ServiceReference.class, "basic1 config ref");
    private final UserRegistry ur1 = mock.mock(UserRegistry.class, "ur1");
    private final ServiceReference<UserRegistry> ur2Ref = mock.mock(ServiceReference.class, "basic2 config ref");
    private final UserRegistry ur2 = mock.mock(UserRegistry.class, "ur2");
    private UserRegistryServiceImpl service;

    /**
     * Perform some setup to simulate the service starting.
     */
    @Before
    public void setUp() {
        dict.put(UserRegistryServiceImpl.CFG_KEY_REFID, new String[] { "basic1" });

        basicConfigMap.put(UserRegistryService.REGISTRY_TYPE, "Basic");


        mock.checking(new Expectations() {
            {

                allowing(ur1Ref).getProperty(UserRegistryServiceImpl.KEY_CONFIG_ID);
                will(returnValue("basic1"));
                allowing(ur1Ref).getProperty(UserRegistryService.REGISTRY_TYPE);
                will(returnValue("Basic"));
                allowing(ur1Ref).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(ur1Ref).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(ur2Ref).getProperty(UserRegistryServiceImpl.KEY_CONFIG_ID);
                will(returnValue("basic2"));
                allowing(ur2Ref).getProperty(UserRegistryService.REGISTRY_TYPE);
                will(returnValue("Basic"));
                allowing(ur2Ref).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(ur2Ref).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(componentContext).locateService(UserRegistryServiceImpl.KEY_USER_REGISTRY, ur1Ref);
                will(returnValue(ur1));
                allowing(componentContext).locateService(UserRegistryServiceImpl.KEY_USER_REGISTRY, ur2Ref);
                will(returnValue(ur2));

            }
        });

        service = new UserRegistryServiceImpl();
        service.setUserRegistry(ur1Ref);
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
     * Check that the expected exception and error message are logged
     * when the configured refId is not defined.
     */
    private void isUserRegistryConfigured_checkExpectedMissingRefIdExceptionIsThrownAndLogged() {
        try {
            service.isUserRegistryConfigured();
            fail("Expected RegistryException when no refId is configured");
        } catch (RegistryException e) {
            assertEquals("Exception contained the wrong message",
                         "CWWKS3000E: A configuration exception has occurred. There is no configured refId parameter for the userRegistry configuration.",
                         e.getMessage());

            assertTrue(
                       "Expected error message was not logged",
                       outputMgr.checkForStandardErr("CWWKS3000E: A configuration exception has occurred. There is no configured refId parameter for the userRegistry configuration."));
        }
    }

    /**
     * If no configured refId is available (null), a RegistryException shall be thrown.
     * This test is invalid. Due to metatype, the refId String[] will never be null when URS is supplied with configuration
     * properties from confg admin.
     */
    @Test
    @Ignore
    public void isUserRegistryConfigured_noConfiguredRefId() throws Exception {
        dict.remove(UserRegistryServiceImpl.CFG_KEY_REFID);
        service.activate(componentContext, dict);

        isUserRegistryConfigured_checkExpectedMissingRefIdExceptionIsThrownAndLogged();
    }

    /**
     * If no configured refId is available (empty array), a RegistryException shall be thrown.
     */
    @Test
    public void isUserRegistryConfigured_zeroConfiguredRefId() throws Exception {
        dict.put(UserRegistryServiceImpl.CFG_KEY_REFID, new String[] {});
        service.activate(componentContext, dict);

        isUserRegistryConfigured_checkExpectedMissingRefIdExceptionIsThrownAndLogged();
    }

    /**
     * Check that the correct message is contained in the exception and logged
     * when the / one of reference IDs is invalid.
     *
     * @param id
     * @param e
     */
    private void isUserRegistryConfigured_checkExpectedInvalidReferenceExceptionAndMessageLogged(String id) {
        try {
            service.isUserRegistryConfigured();
            fail("Expected RegistryException when the refId is invalid");
        } catch (RegistryException e) {
            assertEquals("Exception contained the wrong message",
                         "CWWKS3001E: A configuration exception has occurred. The requested UserRegistry instance with ID " + id + " could not be found.",
                         e.getMessage());
            assertTrue("Expected error message was not logged",
                       outputMgr.checkForStandardErr("CWWKS3001E: A configuration exception has occurred. The requested UserRegistry instance with ID " + id
                                                     + " could not be found."));
        }
    }

    /**
     * If the configured refId is invalid, a RegistryException shall be thrown.
     */
    @Test
    public void isUserRegistryConfigured_invalidConfiguredRefId() throws Exception {
        dict.put(UserRegistryServiceImpl.CFG_KEY_REFID, new String[] { "ldap1" });
        service.activate(componentContext, dict);

        isUserRegistryConfigured_checkExpectedInvalidReferenceExceptionAndMessageLogged("ldap1");
        try {
            service.isUserRegistryConfigured();
            fail("Expected RegistryException when the refId is invalid");
        } catch (RegistryException e) {

        }
    }

    /**
     * If a single configured refId is available, a UserRegistry instance shall be returned.
     */
    @Test
    public void isUserRegistryConfigured_oneRefId() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(ur1).getRealm();
                will(returnValue("testRealm"));
            }
        });
        service.activate(componentContext, dict);
        assertTrue("Configuration is valid so should answer true",
                   service.isUserRegistryConfigured());
    }

    /**
     * If multiple configured refId are available, a UserRegistry instance
     * shall be returned which is an instance of the UserRegistryProxy.
     */
    @Test
    public void isUserRegistryConfigured_multipleRefId() throws Exception {
        dict.put(UserRegistryServiceImpl.CFG_KEY_REFID, new String[] { "basic1", "basic2" });
        mock.checking(new Expectations() {
            {
                allowing(ur2).getRealm();
                will(returnValue("testRealm2"));
            }
        });
        service.setUserRegistry(ur2Ref);
        service.activate(componentContext, dict);
        assertTrue("UserRegistry configuration is valid and should return true",
                   service.isUserRegistryConfigured());
    }

    /**
     * If multiple configured refId are available, but one refId is invalid,
     * a RegistryException shall be thrown.
     */
    @Test
    public void isUserRegistryConfigured_multipleButMissingRefId() throws Exception {
        dict.put(UserRegistryServiceImpl.CFG_KEY_REFID, new String[] { "basic1", "ldap1" });
        mock.checking(new Expectations() {
            {
            }
        });

        service.activate(componentContext, dict);
        isUserRegistryConfigured_checkExpectedInvalidReferenceExceptionAndMessageLogged("ldap1");
    }

    /**
     * Check that the expected exception and error message are logged
     * when the configured refId is not defined.
     */
    private void getUserRegistry_checkExpectedMissingRefIdExceptionIsThrownAndLogged() {
        try {
            service.getUserRegistry();
            fail("Expected RegistryException when no refId is configured");
        } catch (RegistryException e) {
            assertEquals("Exception contained the wrong message",
                         "CWWKS3000E: A configuration exception has occurred. There is no configured refId parameter for the userRegistry configuration.",
                         e.getMessage());

            assertTrue(
                       "Expected error message was not logged",
                       outputMgr.checkForStandardErr("CWWKS3000E: A configuration exception has occurred. There is no configured refId parameter for the userRegistry configuration."));
        }
    }

    /**
     * If no configured refId is available (null), a RegistryException shall be thrown.
     * This test is invalid. Due to metatype, the refId String[] will never be null when URS is supplied with configuration
     * properties from confg admin.
     */
    @Test
    @Ignore
    public void getUserRegistry_noConfiguredRefId() throws Exception {
        dict.remove(UserRegistryServiceImpl.CFG_KEY_REFID);
        service.activate(componentContext, dict);

        getUserRegistry_checkExpectedMissingRefIdExceptionIsThrownAndLogged();
    }

    /**
     * If no configured refId is available (empty array), a RegistryException shall be thrown.
     */
    @Test
    public void getUserRegistry_zeroConfiguredRefId() throws Exception {
        dict.put(UserRegistryServiceImpl.CFG_KEY_REFID, new String[] {});
        service.activate(componentContext, dict);

        getUserRegistry_checkExpectedMissingRefIdExceptionIsThrownAndLogged();
    }

    /**
     * Check that the correct message is contained in the exception and logged
     * when the / one of reference IDs is invalid.
     *
     * @param id
     * @param e
     */
    private void getUserRegistry_checkExpectedInvalidReferenceExceptionAndMessageLogged(String id) {
        try {
            service.getUserRegistry();
            fail("Expected RegistryException when the refId is invalid");
        } catch (RegistryException e) {
            assertEquals("Exception contained the wrong message",
                         "CWWKS3001E: A configuration exception has occurred. The requested UserRegistry instance with ID " + id + " could not be found.",
                         e.getMessage());
            assertTrue("Expected error message was not logged",
                       outputMgr.checkForStandardErr("CWWKS3001E: A configuration exception has occurred. The requested UserRegistry instance with ID " + id
                                                     + " could not be found."));
        }
    }

    /**
     * If the configured refId is invalid, a RegistryException shall be thrown.
     */
    @Test
    public void getUserRegistry_invalidConfiguredRefId() throws Exception {
        dict.put(UserRegistryServiceImpl.CFG_KEY_REFID, new String[] { "ldap1" });
        service.activate(componentContext, dict);

        getUserRegistry_checkExpectedInvalidReferenceExceptionAndMessageLogged("ldap1");
    }

    /**
     * If a single configured refId is available, a UserRegistry instance shall be returned.
     */
    @Test
    public void getUserRegistry_oneRefId() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(ur1).getRealm();
                will(returnValue("testRealm"));
            }
        });
        service.activate(componentContext, dict);
        assertNotNull("Should see a valid instance returned", service.getUserRegistry());
    }

    /**
     * If a single configured refId is available, a UserRegistry instance shall be returned.
     * If that refId is then unavailable, a RegistryException shall be thrown.
     */
    @Test
    public void getUserRegistry_setUnsetUserRegistry() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(ur1).getRealm();
                will(returnValue("testRealm"));
            }
        });
        service.activate(componentContext, dict);
        assertNotNull("Should see a valid instance returned", service.getUserRegistry());

        service.unsetUserRegistry(ur1Ref);
        getUserRegistry_checkExpectedInvalidReferenceExceptionAndMessageLogged("basic1");
    }

    /**
     * @param e
     */
    private void checkExpectedNoFactoryTypeExceptionAndMessage(String type, RegistryException e) {
        assertEquals("Exception contained the wrong message",
                     "CWWKS3002E: A configuration exception has occurred. The requested UserRegistry factory of type " + type + " could not be found.",
                     e.getMessage());

        assertTrue("Expected error message was not logged",
                   outputMgr.checkForStandardErr("CWWKS3002E: A configuration exception has occurred. The requested UserRegistry factory of type " + type
                                                 + " could not be found."));
    }

    /**
     * If a single configured refId is available, a UserRegistry instance shall be returned.
     * If that refId is then unavailable, a RegistryException shall be thrown.
     */
    @Test
    public void getUserRegistry_setUnsetFactory() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(ur1).getRealm();
                will(returnValue("testRealm"));
            }
        });
        service.activate(componentContext, dict);
        assertNotNull("Should see a valid instance returned", service.getUserRegistry());

    }

    /**
     * If multiple configured refId are available, a UserRegistry instance
     * shall be returned which is an instance of the UserRegistryProxy.
     */
    @Test
    public void getUserRegistry_multipleRefId() throws Exception {
        dict.put(UserRegistryServiceImpl.CFG_KEY_REFID, new String[] { "basic1", "basic2" });
        service.setUserRegistry(ur2Ref);
        service.activate(componentContext, dict);
        UserRegistry reg = service.getUserRegistry();
        assertNotNull("Should see a valid instance returned", reg);
        assertTrue("Instance should be a UserRegistryProxy", reg instanceof UserRegistryProxy);
    }

    /**
     * If multiple configured refId are available, a UserRegistry instance
     * shall be returned which is an instance of the UserRegistryProxy.
     */
    @Test
    public void getUserRegistry_multipleSameRefIdDoesntCause2ndLookup() throws Exception {
        dict.put(UserRegistryServiceImpl.CFG_KEY_REFID, new String[] { "basic1", "basic1" });
        service.activate(componentContext, dict);
        UserRegistry reg = service.getUserRegistry();
        assertNotNull("Should see a valid instance returned", reg);
        assertTrue("Instance should be a UserRegistryProxy", reg instanceof UserRegistryProxy);
    }

    /**
     * If multiple configured refId are available, but one refId is invalid,
     * a RegistryException shall be thrown.
     */
    @Test
    public void getUserRegistry_multipleButMissingRefId() throws Exception {
        dict.put(UserRegistryServiceImpl.CFG_KEY_REFID, new String[] { "basic1", "ldap1" });
        service.activate(componentContext, dict);

        getUserRegistry_checkExpectedInvalidReferenceExceptionAndMessageLogged("ldap1");
    }

    /**
     * Null id is not supported.
     */
    @Test(expected = IllegalArgumentException.class)
    public void getUserRegistryId_nullId() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(ur1).getRealm();
                will(returnValue("testRealm"));
            }
        });
        service.activate(componentContext, dict);
        service.getUserRegistry(null);
    }

    /**
     * If no such id is available, a RegistryException shall be thrown.
     */
    @Test
    public void getUserRegistryId_noSuchID() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(ur1).getRealm();
                will(returnValue("testRealm"));
            }
        });
        service.activate(componentContext, dict);
        String id = "ldap1";
        try {
            service.getUserRegistry(id);
            fail("Expected RegistryException when the refId is invalid");
        } catch (RegistryException e) {
            assertEquals("Exception contained the wrong message",
                         "CWWKS3001E: A configuration exception has occurred. The requested UserRegistry instance with ID " + id + " could not be found.",
                         e.getMessage());
            assertTrue("Expected error message was not logged",
                       outputMgr.checkForStandardErr("CWWKS3001E: A configuration exception has occurred. The requested UserRegistry instance with ID " + id
                                                     + " could not be found."));
        }
    }

    /**
     * If a matching id is available, a UserRegistry instance shall be returned.
     */
    @Test
    public void getUserRegistryId_matchingID() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(ur1).getRealm();
                will(returnValue("testRealm"));
            }
        });
        service.activate(componentContext, dict);
        assertNotNull("Should see a valid instance returned", service.getUserRegistry("basic1"));
    }

    /**
     * If a matching id is unavailable, a RegistryException shall be thrown.
     */
    @Test
    public void getUserRegistryId_setUnsetUserRegistry() throws Exception {
        mock.checking(new Expectations() {
            {
                allowing(ur1).getRealm();
                will(returnValue("testRealm"));
            }
        });
        service.activate(componentContext, dict);
        assertNotNull("Should see a valid instance returned", service.getUserRegistry("basic1"));

        service.unsetUserRegistry(ur1Ref);

        getUserRegistry_checkExpectedInvalidReferenceExceptionAndMessageLogged("basic1");
    }


}
