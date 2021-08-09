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
package com.ibm.ws.container.service.metadata.internal;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import test.common.ComponentContextMockery;

import com.ibm.ws.container.service.metadata.ApplicationMetaDataListener;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.runtime.metadata.ApplicationMetaData;
import com.ibm.wsspi.adaptable.module.Container;

@RunWith(JMock.class)
public class ApplicationMetaDataListenerTest {
    private static final String REF_LISTENERS = "applicationMetaDataListeners";

    private final Mockery mockery = new Mockery();
    private final ComponentContextMockery ccMockery = new ComponentContextMockery(mockery);
    private final ApplicationMetaData metaData = new TestApplicationMetaData();
    private final Container app = mockery.mock(Container.class);
    private final MetaDataEventChecker<ApplicationMetaData> event = new MetaDataEventChecker<ApplicationMetaData>(metaData, app);
    private final MetaDataEventChecker<ApplicationMetaData> nullEvent = new MetaDataEventChecker<ApplicationMetaData>(metaData, null);

    @Test
    public void testApplicationMetaDataCreated() throws MetaDataException {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        final ApplicationMetaDataListener listener = mockery.mock(ApplicationMetaDataListener.class);
        ServiceReference<ApplicationMetaDataListener> listenerRef = ccMockery.mockService(cc, REF_LISTENERS, listener);
        mockery.checking(new Expectations() {
            {
                one(listener).applicationMetaDataCreated(with(event));
            }
        });

        MetaDataServiceImpl mdsi = new MetaDataServiceImpl();
        mdsi.activate(cc);
        mdsi.addApplicationMetaDataListener(listenerRef);
        mdsi.fireApplicationMetaDataCreated(metaData, app);
    }

    @Test
    public void testApplicationMetaDataDestroyed() {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        final ApplicationMetaDataListener listener = mockery.mock(ApplicationMetaDataListener.class);
        ServiceReference<ApplicationMetaDataListener> listenerRef = ccMockery.mockService(cc, REF_LISTENERS, listener);
        mockery.checking(new Expectations() {
            {
                one(listener).applicationMetaDataDestroyed(with(nullEvent));
            }
        });

        MetaDataServiceImpl mdsi = new MetaDataServiceImpl();
        mdsi.activate(cc);
        mdsi.addApplicationMetaDataListener(listenerRef);
        mdsi.fireApplicationMetaDataDestroyed(metaData);
    }

    private void testApplicationMetaDataThrows(final Throwable throwable) throws MetaDataException {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        final ApplicationMetaDataListener throwsListener = mockery.mock(ApplicationMetaDataListener.class, "throws");
        ServiceReference<ApplicationMetaDataListener> throwsListenerRef = ccMockery.mockService(cc, REF_LISTENERS, throwsListener);
        final ApplicationMetaDataListener destroyListener = mockery.mock(ApplicationMetaDataListener.class, "destroy");
        ServiceReference<ApplicationMetaDataListener> destroyListenerRef = ccMockery.mockService(cc, REF_LISTENERS, destroyListener);
        mockery.checking(new Expectations() {
            {
                one(throwsListener).applicationMetaDataCreated(with(event));
                will(throwException(throwable));
                one(throwsListener).applicationMetaDataDestroyed(with(nullEvent));
                will(throwException(new Error()));
                one(destroyListener).applicationMetaDataDestroyed(with(nullEvent));
            }
        });

        MetaDataServiceImpl mdsi = new MetaDataServiceImpl();
        mdsi.activate(cc);
        mdsi.addApplicationMetaDataListener(throwsListenerRef);
        mdsi.addApplicationMetaDataListener(destroyListenerRef);
        mdsi.fireApplicationMetaDataCreated(metaData, app);
    }

    @Test(expected = MetaDataException.class)
    public void testApplicationMetaDataCreatedThrows() throws MetaDataException {
        testApplicationMetaDataThrows(new MetaDataException("test"));
    }

    @Test(expected = MetaDataException.class)
    public void testApplicationMetaDataCreatedThrowsError() throws MetaDataException {
        testApplicationMetaDataThrows(new Error());
    }

    @Test
    public void testRemoveApplicationMetaDataListener() throws MetaDataException {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        ApplicationMetaDataListener listener = mockery.mock(ApplicationMetaDataListener.class);
        ServiceReference<ApplicationMetaDataListener> listenerRef = ccMockery.mockService(cc, REF_LISTENERS, listener);

        MetaDataServiceImpl mdsi = new MetaDataServiceImpl();
        mdsi.activate(cc);
        mdsi.addApplicationMetaDataListener(listenerRef);
        mdsi.removeApplicationMetaDataListener(listenerRef);
        mdsi.fireApplicationMetaDataCreated(metaData, app);
    }
}
