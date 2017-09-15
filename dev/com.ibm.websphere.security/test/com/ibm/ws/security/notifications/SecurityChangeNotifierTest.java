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
package com.ibm.ws.security.notifications;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

@SuppressWarnings("unchecked")
public class SecurityChangeNotifierTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private BaseSecurityChangeNotifier baseSecurityChangeNotifier;
    private final ComponentContext componentContext = mockery.mock(ComponentContext.class);
    private final ServiceReference<SecurityChangeListener> listener1Ref = mockery.mock(ServiceReference.class, "listener1Ref");
    private final ServiceReference<SecurityChangeListener> listener2Ref = mockery.mock(ServiceReference.class, "listener2Ref");
    private final SecurityChangeListener listener1 = mockery.mock(SecurityChangeListener.class, "listener1");
    private final SecurityChangeListener listener2 = mockery.mock(SecurityChangeListener.class, "listener2");

    @Before
    public void setUp() {
        createComponentContextExpectations();
        createListenersExpectations();
        baseSecurityChangeNotifier = createActivatedNotifierWithListeners();
    }

    private void createComponentContextExpectations() {
        mockery.checking(new Expectations() {
            {
                allowing(componentContext).locateService(BaseSecurityChangeNotifier.KEY_LISTENER, listener1Ref);
                will(returnValue(listener1));
                allowing(componentContext).locateService(BaseSecurityChangeNotifier.KEY_LISTENER, listener2Ref);
                will(returnValue(listener2));
            }
        });
    }

    private void createListenersExpectations() {
        createListenerExpectations(listener1Ref, 1L);
        createListenerExpectations(listener2Ref, 2L);
    }

    private void createListenerExpectations(final ServiceReference<SecurityChangeListener> listenerRef, final long serviceID) {
        mockery.checking(new Expectations() {
            {
                allowing(listenerRef).getProperty(Constants.SERVICE_ID);
                will(returnValue(serviceID));
                allowing(listenerRef).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
            }
        });
    }

    @After
    public void tearDown() {
        mockery.assertIsSatisfied();
    }

    @Test
    public void notifyChange() throws Exception {

        notificationExpectedFor(listener1);
        notificationExpectedFor(listener2);

        baseSecurityChangeNotifier.notifyListeners();
    }

    private BaseSecurityChangeNotifier createActivatedNotifierWithListeners() {
        BaseSecurityChangeNotifier baseSecurityChangeNotifier = new BaseSecurityChangeNotifier();
        baseSecurityChangeNotifier.setChangeListener(listener1Ref);
        baseSecurityChangeNotifier.setChangeListener(listener2Ref);
        baseSecurityChangeNotifier.activate(componentContext);
        return baseSecurityChangeNotifier;
    }

    @Test
    public void notifyChange_removeOneListener() throws Exception {
        baseSecurityChangeNotifier.unsetChangeListener(listener2Ref);

        notificationExpectedFor(listener1);
        notificationNotExpectedFor(listener2);

        baseSecurityChangeNotifier.notifyListeners();
    }

    @Test
    public void notifyChange_removeAllListeners() throws Exception {
        baseSecurityChangeNotifier.unsetChangeListener(listener1Ref);
        baseSecurityChangeNotifier.unsetChangeListener(listener2Ref);

        notificationNotExpectedFor(listener1);
        notificationNotExpectedFor(listener2);

        baseSecurityChangeNotifier.notifyListeners();
    }

    @Test
    public void deactivate() throws Exception {
        baseSecurityChangeNotifier.deactivate(componentContext);

        notificationNotExpectedFor(listener1);
        notificationNotExpectedFor(listener2);

        baseSecurityChangeNotifier.notifyListeners();
    }

    private void notificationExpectedFor(final SecurityChangeListener listener) {
        mockery.checking(new Expectations() {
            {
                one(listener).notifyChange();
            }
        });
    }

    private void notificationNotExpectedFor(final SecurityChangeListener listener) {
        mockery.checking(new Expectations() {
            {
                never(listener).notifyChange();
            }
        });
    }

}
