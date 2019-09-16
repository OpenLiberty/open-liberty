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
package com.ibm.wsspi.kernel.service.utils;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.ws.kernel.service.util.ServiceRegistrationModifier;
import com.ibm.ws.kernel.service.util.ServiceRegistrationModifier.ServicePropertySupplier;

/**
 *
 */
@RunWith(JMock.class)
public class ServiceRegistrationModifierTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @After
    public void checkMockIsSatisfied() {
        context.assertIsSatisfied();
    }

    Mockery context = new Mockery();

    final BundleContext mockBundleContext = context.mock(BundleContext.class);
    @SuppressWarnings("unchecked")
    final ServiceRegistration<Object> mockReg = context.mock(ServiceRegistration.class);
    final ServicePropertySupplier mockSupplier = context.mock(ServicePropertySupplier.class);

    @Test
    public void registerWithNoExistingRegistration() {
        final Object service = new Object();
        final Hashtable<String, Object> initialProps = new Hashtable<>();
        initialProps.put("test", "value");

        context.checking(new Expectations() {
            {
                one(mockSupplier).getServiceProperties();
                will(returnValue(initialProps));
                one(mockBundleContext).registerService(Object.class, service, initialProps);
                will(returnValue(mockReg));
                one(mockReg).unregister();
            }
        });

        ServiceRegistrationModifier<Object> modifier = new ServiceRegistrationModifier<Object>(Object.class, mockSupplier, service);
        modifier.registerOrUpdate(mockBundleContext);

        modifier.unregister();
        // unregister again should be no-op
        modifier.unregister();

        // calls to update should be a no-op after unregister
        modifier.registerOrUpdate(mockBundleContext);
        modifier.update();
    }

    @Test
    public void updateWithExistingRegistration() {
        final Object service = new Object();
        final Hashtable<String, Object> initialProps = new Hashtable<>();
        initialProps.put("test1", "value1");
        final Hashtable<String, Object> updateProps1 = new Hashtable<>(initialProps);
        updateProps1.put("test2", "value2");
        final Hashtable<String, Object> updateProps2 = new Hashtable<>(updateProps1);
        updateProps2.put("test3", "value3");

        context.checking(new Expectations() {
            {
                one(mockSupplier).getServiceProperties();
                will(returnValue(initialProps));
                one(mockBundleContext).registerService(Object.class, service, initialProps);
                will(returnValue(mockReg));

                one(mockSupplier).getServiceProperties();
                will(returnValue(updateProps1));
                one(mockReg).setProperties(updateProps1);

                one(mockSupplier).getServiceProperties();
                will(returnValue(updateProps2));
                one(mockReg).setProperties(updateProps2);

                // note updateProps2 is returned again on purpose to test that no setProperties is called
                one(mockSupplier).getServiceProperties();
                will(returnValue(updateProps2));
                one(mockSupplier).getServiceProperties();
                will(returnValue(updateProps2));

                one(mockReg).unregister();
            }
        });

        ServiceRegistrationModifier<Object> modifier = new ServiceRegistrationModifier<Object>(Object.class, mockSupplier, service);
        modifier.registerOrUpdate(mockBundleContext);

        // update with a non null context
        modifier.registerOrUpdate(mockBundleContext);

        // update with a null context
        modifier.registerOrUpdate(null);

        // update with the same props should be no-op
        modifier.registerOrUpdate(null);
        // update should also be a no-op
        modifier.update();

        modifier.unregister();
        // unregister again should be no-op
        modifier.unregister();
    }

    @Test
    public void multiThreadUpdate() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        try {
            final int numUpdates = 50;
            // set up a queue that feeds getServiceProperties in a specific order
            final Queue<Hashtable<String, Object>> queue = new ConcurrentLinkedQueue<>();
            Hashtable<String, Object> props = new Hashtable<>();
            props.put("initialKey", "initialValue");
            queue.add(props);
            for (int i = 0; i < numUpdates; i++) {
                props = new Hashtable<>(props);
                props.put("key" + i, "value" + i);
                queue.add(props);
            }

            final Object service = new Object();
            context.checking(new Expectations() {
                {
                    Iterator<Hashtable<String, Object>> iProps = queue.iterator();
                    Hashtable<String, Object> initialProps = iProps.next();
                    one(mockSupplier).getServiceProperties();
                    will(returnValue(initialProps));
                    one(mockBundleContext).registerService(Object.class, service, initialProps);
                    will(returnValue(mockReg));
                    while (iProps.hasNext()) {
                        Hashtable<String, Object> updateProps = iProps.next();
                        one(mockSupplier).getServiceProperties();
                        will(returnValue(updateProps));
                        one(mockReg).setProperties(updateProps);
                    }
                    one(mockReg).unregister();
                }
            });

            final ServiceRegistrationModifier<Object> modifier = new ServiceRegistrationModifier<Object>(Object.class, mockSupplier, service);
            modifier.registerOrUpdate(mockBundleContext);

            // This code is trying very hard to get several threads to all call update() "At the same time"
            // the point of the test is to ensure the order values from getServiceProperties are returned
            // in the same order used for calls to ServiceRegistration.setProperties.
            // This is possible to guarantee because the modifier picks a single thread to do all the calls
            // to setProperties.
            final CountDownLatch start = new CountDownLatch(1);
            final CountDownLatch done = new CountDownLatch(numUpdates);
            for (int i = 0; i < numUpdates; i++) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            start.await();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        }
                        modifier.update();
                        done.countDown();
                    }
                });
            }
            start.countDown();
            done.await(5, TimeUnit.SECONDS);
            modifier.unregister();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void unregisterWithNoExistingRegistration() {
        ServiceRegistrationModifier<Object> modifier = new ServiceRegistrationModifier<Object>(Object.class, mockSupplier, new Object());
        // this should be a no-op
        modifier.unregister();
    }

    @Test
    public void updateWithNoExistingRegistration() {
        ServiceRegistrationModifier<Object> modifier = new ServiceRegistrationModifier<Object>(Object.class, mockSupplier, new Object());
        // this should be a no-op
        modifier.update();
    }
}
