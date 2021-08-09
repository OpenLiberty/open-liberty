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

import com.ibm.ws.container.service.metadata.MetaDataException;
import com.ibm.ws.container.service.metadata.MethodMetaDataListener;
import com.ibm.ws.runtime.metadata.MethodMetaData;

@RunWith(JMock.class)
public class MethodMetaDataListenerTest {
    private static final String REF_LISTENERS = "methodMetaDataListeners";

    private final Mockery mockery = new Mockery();
    private final ComponentContextMockery ccMockery = new ComponentContextMockery(mockery);
    private final MethodMetaData metaData = new TestMethodMetaData();
    private final MetaDataEventChecker<MethodMetaData> event = new MetaDataEventChecker<MethodMetaData>(metaData, null);

    @Test
    public void testMethodMetaDataCreated() throws MetaDataException {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        final MethodMetaDataListener listener = mockery.mock(MethodMetaDataListener.class);
        ServiceReference<MethodMetaDataListener> listenerRef = ccMockery.mockService(cc, REF_LISTENERS, listener);
        mockery.checking(new Expectations() {
            {
                one(listener).methodMetaDataCreated(with(event));
            }
        });

        MetaDataServiceImpl mdsi = new MetaDataServiceImpl();
        mdsi.activate(cc);
        mdsi.addMethodMetaDataListener(listenerRef);
        mdsi.fireMethodMetaDataCreated(metaData);
    }

    @Test
    public void testMethodMetaDataDestroyed() {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        final MethodMetaDataListener listener = mockery.mock(MethodMetaDataListener.class);
        ServiceReference<MethodMetaDataListener> listenerRef = ccMockery.mockService(cc, REF_LISTENERS, listener);
        mockery.checking(new Expectations() {
            {
                one(listener).methodMetaDataDestroyed(with(event));
            }
        });

        MetaDataServiceImpl mdsi = new MetaDataServiceImpl();
        mdsi.activate(cc);
        mdsi.addMethodMetaDataListener(listenerRef);
        mdsi.fireMethodMetaDataDestroyed(metaData);
    }

    private void testMethodMetaDataThrows(final Throwable throwable) throws MetaDataException {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        final MethodMetaDataListener throwsListener = mockery.mock(MethodMetaDataListener.class, "throws");
        ServiceReference<MethodMetaDataListener> throwsListenerRef = ccMockery.mockService(cc, REF_LISTENERS, throwsListener);
        final MethodMetaDataListener destroyListener = mockery.mock(MethodMetaDataListener.class, "destroy");
        ServiceReference<MethodMetaDataListener> destroyListenerRef = ccMockery.mockService(cc, REF_LISTENERS, destroyListener);
        mockery.checking(new Expectations() {
            {
                one(throwsListener).methodMetaDataCreated(with(event));
                will(throwException(throwable));
                one(throwsListener).methodMetaDataDestroyed(with(event));
                will(throwException(new Error()));
                one(destroyListener).methodMetaDataDestroyed(with(event));
            }
        });

        MetaDataServiceImpl mdsi = new MetaDataServiceImpl();
        mdsi.activate(cc);
        mdsi.addMethodMetaDataListener(throwsListenerRef);
        mdsi.addMethodMetaDataListener(destroyListenerRef);
        mdsi.fireMethodMetaDataCreated(metaData);
    }

    @Test(expected = MetaDataException.class)
    public void testMethodMetaDataCreatedThrows() throws MetaDataException {
        testMethodMetaDataThrows(new MetaDataException("test"));
    }

    @Test(expected = MetaDataException.class)
    public void testMethodMetaDataCreatedThrowsError() throws MetaDataException {
        testMethodMetaDataThrows(new Error());
    }

    @Test
    public void testRemoveMethodMetaDataListener() throws MetaDataException {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        MethodMetaDataListener listener = mockery.mock(MethodMetaDataListener.class);
        ServiceReference<MethodMetaDataListener> listenerRef = ccMockery.mockService(cc, REF_LISTENERS, listener);

        MetaDataServiceImpl mdsi = new MetaDataServiceImpl();
        mdsi.activate(cc);
        mdsi.addMethodMetaDataListener(listenerRef);
        mdsi.removeMethodMetaDataListener(listenerRef);
        mdsi.fireMethodMetaDataCreated(metaData);
    }
}
