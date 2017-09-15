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

import com.ibm.ws.container.service.metadata.ComponentMetaDataListener;
import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.runtime.metadata.ComponentMetaData;

@RunWith(JMock.class)
public class ComponentMetaDataListenerTest {
    private static final String REF_LISTENERS = "componentMetaDataListeners";

    private final Mockery mockery = new Mockery();
    private final ComponentContextMockery ccMockery = new ComponentContextMockery(mockery);
    private final ComponentMetaData metaData = new TestComponentMetaData();
    private final MetaDataEventChecker<ComponentMetaData> event = new MetaDataEventChecker<ComponentMetaData>(metaData, null);

    @Test
    public void testComponentMetaDataCreated() throws MetaDataException {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        final ComponentMetaDataListener listener = mockery.mock(ComponentMetaDataListener.class);
        ServiceReference<ComponentMetaDataListener> listenerRef = ccMockery.mockService(cc, REF_LISTENERS, listener);
        mockery.checking(new Expectations() {
            {
                one(listener).componentMetaDataCreated(with(event));
            }
        });

        MetaDataServiceImpl mdsi = new MetaDataServiceImpl();
        mdsi.activate(cc);
        mdsi.addComponentMetaDataListener(listenerRef);
        mdsi.fireComponentMetaDataCreated(metaData);
    }

    @Test
    public void testComponentMetaDataDestroyed() {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        final ComponentMetaDataListener listener = mockery.mock(ComponentMetaDataListener.class);
        ServiceReference<ComponentMetaDataListener> listenerRef = ccMockery.mockService(cc, REF_LISTENERS, listener);
        mockery.checking(new Expectations() {
            {
                one(listener).componentMetaDataDestroyed(with(event));
            }
        });

        MetaDataServiceImpl mdsi = new MetaDataServiceImpl();
        mdsi.activate(cc);
        mdsi.addComponentMetaDataListener(listenerRef);
        mdsi.fireComponentMetaDataDestroyed(metaData);
    }

    private void testComponentMetaDataThrows(final Throwable throwable) throws MetaDataException {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        final ComponentMetaDataListener throwsListener = mockery.mock(ComponentMetaDataListener.class, "throws");
        ServiceReference<ComponentMetaDataListener> throwsListenerRef = ccMockery.mockService(cc, REF_LISTENERS, throwsListener);
        final ComponentMetaDataListener destroyListener = mockery.mock(ComponentMetaDataListener.class, "destroy");
        ServiceReference<ComponentMetaDataListener> destroyListenerRef = ccMockery.mockService(cc, REF_LISTENERS, destroyListener);
        mockery.checking(new Expectations() {
            {
                one(throwsListener).componentMetaDataCreated(with(event));
                will(throwException(throwable));
                one(throwsListener).componentMetaDataDestroyed(with(event));
                will(throwException(new Error()));
                one(destroyListener).componentMetaDataDestroyed(with(event));
            }
        });

        MetaDataServiceImpl mdsi = new MetaDataServiceImpl();
        mdsi.activate(cc);
        mdsi.addComponentMetaDataListener(throwsListenerRef);
        mdsi.addComponentMetaDataListener(destroyListenerRef);
        mdsi.fireComponentMetaDataCreated(metaData);
    }

    @Test(expected = MetaDataException.class)
    public void testComponentMetaDataCreatedThrows() throws MetaDataException {
        testComponentMetaDataThrows(new MetaDataException("test"));
    }

    @Test(expected = MetaDataException.class)
    public void testComponentMetaDataCreatedThrowsError() throws MetaDataException {
        testComponentMetaDataThrows(new Error());
    }

    @Test
    public void testRemoveComponentMetaDataListener() throws MetaDataException {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        ComponentMetaDataListener listener = mockery.mock(ComponentMetaDataListener.class);
        ServiceReference<ComponentMetaDataListener> listenerRef = ccMockery.mockService(cc, REF_LISTENERS, listener);

        MetaDataServiceImpl mdsi = new MetaDataServiceImpl();
        mdsi.activate(cc);
        mdsi.addComponentMetaDataListener(listenerRef);
        mdsi.removeComponentMetaDataListener(listenerRef);
        mdsi.fireComponentMetaDataCreated(metaData);
    }
}
