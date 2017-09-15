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
package com.ibm.ws.container.service.state.internal;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import test.common.ComponentContextMockery;

import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.container.service.state.ApplicationStateListener;
import com.ibm.ws.container.service.state.StateChangeException;

@RunWith(JMock.class)
public class ApplicationStateChangeListenerTest {
    private static final String REF_LISTENERS = "applicationStateListeners";

    private final Mockery mockery = new Mockery();
    private final ComponentContextMockery ccMockery = new ComponentContextMockery(mockery);

    @Test
    public void testApplicationStateStarted() throws StateChangeException {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        final ApplicationStateListener listener = mockery.mock(ApplicationStateListener.class);
        final ApplicationInfo info = mockery.mock(ApplicationInfo.class);

        ServiceReference<ApplicationStateListener> listenerRef = ccMockery.mockService(cc, REF_LISTENERS, listener);
        mockery.checking(new Expectations() {
            {

                one(listener).applicationStarted(with(info));
            }
        });

        StateChangeServiceImpl scs = new StateChangeServiceImpl();

        scs.activate(cc);
        scs.addApplicationStateListener(listenerRef);
        scs.fireApplicationStarted(info);
    }

    @Test
    public void testApplicationStateStarting() throws StateChangeException {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        final ApplicationStateListener listener = mockery.mock(ApplicationStateListener.class);
        final ApplicationInfo info = mockery.mock(ApplicationInfo.class);

        ServiceReference<ApplicationStateListener> listenerRef = ccMockery.mockService(cc, REF_LISTENERS, listener);
        mockery.checking(new Expectations() {
            {

                one(listener).applicationStarting(with(info));
            }
        });

        StateChangeServiceImpl scs = new StateChangeServiceImpl();

        scs.activate(cc);
        scs.addApplicationStateListener(listenerRef);
        scs.fireApplicationStarting(info);
    }

    @Test
    public void testApplicationStateStopped() throws StateChangeException {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        final ApplicationStateListener listener = mockery.mock(ApplicationStateListener.class);
        final ApplicationInfo info = mockery.mock(ApplicationInfo.class);

        ServiceReference<ApplicationStateListener> listenerRef = ccMockery.mockService(cc, REF_LISTENERS, listener);
        mockery.checking(new Expectations() {
            {

                one(listener).applicationStopped(with(info));
            }
        });

        StateChangeServiceImpl scs = new StateChangeServiceImpl();

        scs.activate(cc);
        scs.addApplicationStateListener(listenerRef);
        scs.fireApplicationStopped(info);
    }

    @Test
    public void testApplicationStateStopping() throws StateChangeException {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        final ApplicationStateListener listener = mockery.mock(ApplicationStateListener.class);
        final ApplicationInfo info = mockery.mock(ApplicationInfo.class);

        ServiceReference<ApplicationStateListener> listenerRef = ccMockery.mockService(cc, REF_LISTENERS, listener);
        mockery.checking(new Expectations() {
            {

                one(listener).applicationStopping(with(info));
            }
        });

        StateChangeServiceImpl scs = new StateChangeServiceImpl();

        scs.activate(cc);
        scs.addApplicationStateListener(listenerRef);
        scs.fireApplicationStopping(info);
    }

}
