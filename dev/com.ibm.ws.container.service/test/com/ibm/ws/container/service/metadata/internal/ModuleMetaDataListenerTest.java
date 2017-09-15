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
import com.ibm.ws.container.service.metadata.ModuleMetaDataListener;
import com.ibm.ws.runtime.metadata.ModuleMetaData;
import com.ibm.wsspi.adaptable.module.Container;

@RunWith(JMock.class)
public class ModuleMetaDataListenerTest {
    private static final String REF_LISTENERS = "moduleMetaDataListeners";

    private final Mockery mockery = new Mockery();
    private final ComponentContextMockery ccMockery = new ComponentContextMockery(mockery);
    private final ModuleMetaData metaData = new TestModuleMetaData();
    private final Container mod = mockery.mock(Container.class);
    private final MetaDataEventChecker<ModuleMetaData> event = new MetaDataEventChecker<ModuleMetaData>(metaData, mod);
    private final MetaDataEventChecker<ModuleMetaData> nullEvent = new MetaDataEventChecker<ModuleMetaData>(metaData, null);

    @Test
    public void testModuleMetaDataCreated() throws MetaDataException {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        final ModuleMetaDataListener listener = mockery.mock(ModuleMetaDataListener.class);
        ServiceReference<ModuleMetaDataListener> listenerRef = ccMockery.mockService(cc, REF_LISTENERS, listener);
        mockery.checking(new Expectations() {
            {
                one(listener).moduleMetaDataCreated(with(event));
            }
        });

        MetaDataServiceImpl mdsi = new MetaDataServiceImpl();
        mdsi.activate(cc);
        mdsi.addModuleMetaDataListener(listenerRef);
        mdsi.fireModuleMetaDataCreated(metaData, mod);
    }

    @Test
    public void testModuleMetaDataDestroyed() {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        final ModuleMetaDataListener listener = mockery.mock(ModuleMetaDataListener.class);
        ServiceReference<ModuleMetaDataListener> listenerRef = ccMockery.mockService(cc, REF_LISTENERS, listener);
        mockery.checking(new Expectations() {
            {
                one(listener).moduleMetaDataDestroyed(with(nullEvent));
            }
        });

        MetaDataServiceImpl mdsi = new MetaDataServiceImpl();
        mdsi.activate(cc);
        mdsi.addModuleMetaDataListener(listenerRef);
        mdsi.fireModuleMetaDataDestroyed(metaData);
    }

    private void testModuleMetaDataThrows(final Throwable throwable) throws MetaDataException {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        final ModuleMetaDataListener throwsListener = mockery.mock(ModuleMetaDataListener.class, "throws");
        ServiceReference<ModuleMetaDataListener> throwsListenerRef = ccMockery.mockService(cc, REF_LISTENERS, throwsListener);
        final ModuleMetaDataListener destroyListener = mockery.mock(ModuleMetaDataListener.class, "destroy");
        ServiceReference<ModuleMetaDataListener> destroyListenerRef = ccMockery.mockService(cc, REF_LISTENERS, destroyListener);
        mockery.checking(new Expectations() {
            {
                one(throwsListener).moduleMetaDataCreated(with(event));
                will(throwException(throwable));
                one(throwsListener).moduleMetaDataDestroyed(with(nullEvent));
                will(throwException(new Error()));
                one(destroyListener).moduleMetaDataDestroyed(with(nullEvent));
            }
        });

        MetaDataServiceImpl mdsi = new MetaDataServiceImpl();
        mdsi.activate(cc);
        mdsi.addModuleMetaDataListener(throwsListenerRef);
        mdsi.addModuleMetaDataListener(destroyListenerRef);
        mdsi.fireModuleMetaDataCreated(metaData, mod);
    }

    @Test(expected = MetaDataException.class)
    public void testModuleMetaDataCreatedThrows() throws MetaDataException {
        testModuleMetaDataThrows(new MetaDataException("test"));
    }

    @Test(expected = MetaDataException.class)
    public void testModuleMetaDataCreatedThrowsError() throws MetaDataException {
        testModuleMetaDataThrows(new Error());
    }

    @Test
    public void testRemoveModuleMetaDataListener() throws MetaDataException {
        ComponentContext cc = mockery.mock(ComponentContext.class);
        ModuleMetaDataListener listener = mockery.mock(ModuleMetaDataListener.class);
        ServiceReference<ModuleMetaDataListener> listenerRef = ccMockery.mockService(cc, REF_LISTENERS, listener);

        MetaDataServiceImpl mdsi = new MetaDataServiceImpl();
        mdsi.activate(cc);
        mdsi.addModuleMetaDataListener(listenerRef);
        mdsi.removeModuleMetaDataListener(listenerRef);
        mdsi.fireModuleMetaDataCreated(metaData, mod);
    }
}
