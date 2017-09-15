/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.registry.internal;

import java.util.Collections;

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

import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.registry.UserRegistryChangeListener;

import test.common.SharedOutputManager;

/**
 *
 */
@SuppressWarnings("unchecked")
public class UserRegistryServiceImplNotificationTest {
    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    /**
     * Using the test rule will drive capture/restore and will dump on error..
     * Notice this is not a static variable, though it is being assigned a value we
     * allocated statically. -- the normal-variable-ness is for before/after processing
     */
    @Rule
    public TestRule managerRule = outputMgr;

    private final Mockery mock = new JUnit4Mockery();
    private final ComponentContext cc = mock.mock(ComponentContext.class);
    private final ServiceReference<UserRegistryChangeListener> listener1Ref = mock.mock(ServiceReference.class, "listener1Ref");
    private final UserRegistryChangeListener listener1 = mock.mock(UserRegistryChangeListener.class, "listener1");
    private final ServiceReference<UserRegistryChangeListener> listener2Ref = mock.mock(ServiceReference.class, "listener2Ref");
    private final UserRegistryChangeListener listener2 = mock.mock(UserRegistryChangeListener.class, "listener2");
    private final ServiceReference<UserRegistry> userRegistryRef = mock.mock(ServiceReference.class, "userRegistryRef");

    private UserRegistryServiceImpl service;

    @Before
    public void setUp() {
        mock.checking(new Expectations() {
            {
                allowing(listener1Ref).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                allowing(listener1Ref).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(listener2Ref).getProperty(Constants.SERVICE_ID);
                will(returnValue(2L));
                allowing(listener2Ref).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));

                allowing(cc).locateService(UserRegistryServiceImpl.KEY_LISTENER, listener1Ref);
                will(returnValue(listener1));
                allowing(cc).locateService(UserRegistryServiceImpl.KEY_LISTENER, listener2Ref);
                will(returnValue(listener2));
            }
        });

        service = new UserRegistryServiceImpl();
        service.setUserRegistryChangeListener(listener1Ref);
        service.setUserRegistryChangeListener(listener2Ref);
        service.activate(cc, Collections.<String, Object> emptyMap());
    }

    @After
    public void tearDown() {
        service.deactivate(cc);
        service.unsetUserRegistryChangeListener(listener1Ref);
        service.unsetUserRegistryChangeListener(listener2Ref);
        mock.assertIsSatisfied();
    }

    @Test
    public void setConfiguration() {
        final UserRegistry userRegistry = mock.mock(UserRegistry.class);
        mock.checking(new Expectations() {
            {
                one(userRegistryRef).getProperty(UserRegistryServiceImpl.KEY_CONFIG_ID);
                will(returnValue("configId1"));
                one(userRegistryRef).getProperty("service.id");
                will(returnValue(123L));
                one(userRegistryRef).getProperty("service.ranking");
                will(returnValue(1));
                one(userRegistryRef).getProperty(UserRegistryServiceImpl.KEY_TYPE);
                will(returnValue("type1"));
                one(listener1).notifyOfUserRegistryChange();
                one(listener2).notifyOfUserRegistryChange();
                one(cc).locateService("UserRegistry", userRegistryRef);
                will(returnValue(userRegistry));
                exactly(2).of(userRegistry).getRealm();
                will(returnValue("testRealm"));
            }
        });

        service.setUserRegistry(userRegistryRef);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryServiceImpl#updateConfiguration(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void updateConfiguration() {
        mock.checking(new Expectations() {
            {
                one(listener1).notifyOfUserRegistryChange();
                one(listener2).notifyOfUserRegistryChange();
            }
        });
        service.updatedUserRegistry(null);
    }

    @Test
    public void unsetConfiguration() {
        mock.checking(new Expectations() {
            {
                one(userRegistryRef).getProperty(UserRegistryServiceImpl.KEY_CONFIG_ID);
                will(returnValue("configId1"));
                one(userRegistryRef).getProperty("service.id");
                will(returnValue(123L));
                one(userRegistryRef).getProperty("service.ranking");
                will(returnValue(1));
                one(listener1).notifyOfUserRegistryChange();
                one(listener2).notifyOfUserRegistryChange();
                one(userRegistryRef).getProperty(UserRegistryServiceImpl.REGISTRY_TYPE);
                will(returnValue("LDAP"));
            }
        });

        service.unsetUserRegistry(userRegistryRef);
    }

    /**
     * Test method for {@link com.ibm.ws.security.registry.internal.UserRegistryServiceImpl#modify(java.util.Map)}.
     */
    @Test
    public void modify() {
        mock.checking(new Expectations() {
            {
                one(listener1).notifyOfUserRegistryChange();
                one(listener2).notifyOfUserRegistryChange();
            }
        });
        service.modify(Collections.<String, Object> emptyMap());
    }

}
