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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;

import test.common.SharedOutputManager;
import test.utils.Utils;

/**
 *
 */
@RunWith(JMock.class)
public class ConcurrentServiceReferenceSetTest {

    static SharedOutputManager outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(Utils.TEST_DATA);
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
    }

    Mockery context = new Mockery();

    @SuppressWarnings("unchecked")
    ServiceReference<String> mockServiceReference1 = context.mock(ServiceReference.class, "1");

    @SuppressWarnings("unchecked")
    ServiceReference<String> mockServiceReference2 = context.mock(ServiceReference.class, "2");

    ComponentContext mockComponentContext = context.mock(ComponentContext.class);

    @Test
    public void testConcurrentServiceReferenceSet() {
        final String m = "testAtomicServiceReference";
        Iterator<String> serviceIterator;
        Iterator<ServiceAndServiceReferencePair<String>> serviceAndSRIterator;
        String returnedService;
        ServiceAndServiceReferencePair<String> returnedServiceAndReference;
        ConcurrentServiceReferenceSet<String> serviceSet = new ConcurrentServiceReferenceSet<String>("string");

        try {

            assertFalse("A-1 isActive should return false without context: " + serviceSet, serviceSet.isActive());
            assertTrue("A-2 list of services should be empty without registered refs: " + serviceSet, serviceSet.isEmpty());
            assertFalse("A-3 result should be false after attempt to add null ref: " + serviceSet, serviceSet.addReference(null));

            final String service = "Dummy Service.. obviously not invokable";
            context.checking(new Expectations() {
                {
                    one(mockComponentContext).locateService("string", mockServiceReference1);
                    will(returnValue(service));

                    atLeast(1).of(mockServiceReference1).getProperty(Constants.SERVICE_ID);
                    will(returnValue(0L));
                    atLeast(1).of(mockServiceReference1).getProperty(Constants.SERVICE_RANKING);
                    will(returnValue(0));
                }
            });

            serviceSet.addReference(mockServiceReference1);
            assertFalse("B-1 set of references should not be empty after reference was added: " + serviceSet, serviceSet.isEmpty());
            assertFalse("B-2 list of services should be empty before context is active: " + serviceSet, serviceSet.getServices().hasNext());

            serviceSet.activate(mockComponentContext);
            serviceIterator = serviceSet.getServices();
            serviceAndSRIterator = serviceSet.getServicesWithReferences();

            assertTrue("B-3 list of services should not be empty once activated: " + serviceSet, serviceIterator.hasNext());
            assertTrue("B-3b list of services and references should not be empty once activated: " + serviceSet, serviceAndSRIterator.hasNext());
            assertTrue("B-4 isActive should return true once activated: " + serviceSet, serviceSet.isActive());

            returnedService = serviceIterator.next();
            returnedServiceAndReference = serviceAndSRIterator.next();
            String foundService = serviceSet.getService(mockServiceReference1);

            assertSame("B-3 getService should return result of locateService: " + serviceSet, service, returnedService);
            assertSame("B-3b getService should return result of locateService: " + serviceSet, service, returnedServiceAndReference.getService());

            assertSame("B-3c getServiceReference should return result of mockServiceReference: " + serviceSet, mockServiceReference1,
                       returnedServiceAndReference.getServiceReference());

            assertSame("B-3d getService(reference) should return result locateService: " + serviceSet, service, foundService);

            serviceIterator = serviceSet.getServices();
            serviceAndSRIterator = serviceSet.getServicesWithReferences();

            serviceSet.deactivate(mockComponentContext);
            assertFalse("C-1 list of services should be empty without context: " + serviceSet, serviceSet.getServices().hasNext());
            assertFalse("C-1 iterator of services iterator should not have next without context", serviceIterator.hasNext());
            assertNoSuchElement("C-1 iterator of services iterator should throw without context", serviceIterator);
            assertFalse("C-1 list of services and refs should be empty without context: " + serviceSet, serviceSet.getServicesWithReferences().hasNext());
            assertFalse("C-1 iterator of services and refs should be empty without context", serviceIterator.hasNext());
            assertNoSuchElement("C-1 iterator of services and refs should throw without context", serviceIterator);
            serviceSet.removeReference(mockServiceReference1);
            assertTrue("C-2 set of references should be empty after reference was removed: " + serviceSet, serviceSet.isEmpty());

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    private void assertNoSuchElement(String message, Iterator<?> iterator) {
        try {
            iterator.next();
            fail(message);
        } catch (NoSuchElementException ex) {
            System.out.println("caught expected " + ex);
        }
    }

    private static String locateService(ServiceReference<String> ref) {
        return ((TestServiceReference) ref).getName();
    }

    private static TestServiceReference addReference(ConcurrentServiceReferenceSet<String> set, String name, int ranking) {
        TestServiceReference ref = new TestServiceReference(name);
        ref.ranking = ranking;
        Assert.assertFalse("initial reference should not replace", set.addReference(ref));
        return ref;
    }

    private static <T> List<T> list(Iterator<T> iterator) {
        List<T> list = new ArrayList<T>();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }

    private static <T> List<T> listServices(Iterator<ServiceAndServiceReferencePair<T>> iterator) {
        List<T> list = new ArrayList<T>();
        while (iterator.hasNext()) {
            list.add(iterator.next().getService());
        }
        return list;
    }

    @Test
    public void testRanking() {
        ConcurrentServiceReferenceSet<String> set = new ConcurrentServiceReferenceSet<String>("ref");
        TestComponentContext cc = new TestComponentContext();
        set.activate(cc);

        Assert.assertNull("empty set should return null", set.getHighestRankedService());
        Assert.assertNull("empty set should return null", set.getHighestRankedReference());
        Assert.assertFalse("empty set should have empty iterator", set.getServices().hasNext());
        Assert.assertFalse("empty set should have empty iterator", set.getServices().hasNext());

        TestServiceReference srA = addReference(set, "A", 1);
        Assert.assertEquals("single service should be returned", "A", set.getHighestRankedService());
        Assert.assertEquals("single service should be returned", "A", locateService(set.getHighestRankedReference()));
        Assert.assertEquals("single service should be returned", Arrays.asList("A"), list(set.getServices()));
        Assert.assertEquals("single service should be returned", Arrays.asList("A"), listServices(set.getServicesWithReferences()));

        TestServiceReference srB = addReference(set, "B", 2);
        Assert.assertEquals("highest ranked service should be returned", "B", set.getHighestRankedService());
        Assert.assertEquals("highest ranked service should be returned", "B", locateService(set.getHighestRankedReference()));
        Assert.assertEquals("highest ranked service should be returned first", Arrays.asList("B", "A"), list(set.getServices()));
        Assert.assertEquals("highest ranked service should be returned first", Arrays.asList("B", "A"), listServices(set.getServicesWithReferences()));

        Assert.assertTrue("reference should be removed", set.removeReference(srA));
        Assert.assertEquals("single service should be returned", "B", set.getHighestRankedService());
        Assert.assertEquals("single service should be returned", "B", locateService(set.getHighestRankedReference()));
        Assert.assertEquals("single service should be returned", Arrays.asList("B"), list(set.getServices()));
        Assert.assertEquals("single service should be returned", Arrays.asList("B"), listServices(set.getServicesWithReferences()));

        Assert.assertTrue("reference should be removed", set.removeReference(srB));
        Assert.assertNull("empty set should return null", set.getHighestRankedService());
        Assert.assertNull("empty set should return null", set.getHighestRankedReference());
        Assert.assertFalse("empty set should have empty iterator", set.getServices().hasNext());
        Assert.assertFalse("empty set should have empty iterator", set.getServices().hasNext());

        Assert.assertFalse("reference should not replace", set.addReference(srB));
        Assert.assertFalse("reference should not replace", set.addReference(srA));
        Assert.assertEquals("highest ranked service (added first) should be returned", "B", set.getHighestRankedService());
        Assert.assertEquals("highest ranked service (added first) should be returned", "B", locateService(set.getHighestRankedReference()));
        Assert.assertEquals("highest ranked service (added first) should be returned first", Arrays.asList("B", "A"), list(set.getServices()));
        Assert.assertEquals("highest ranked service (added first) should be returned first", Arrays.asList("B", "A"), listServices(set.getServicesWithReferences()));
    }

    @Test
    public void testUpdateRanking() {
        ConcurrentServiceReferenceSet<String> set = new ConcurrentServiceReferenceSet<String>("ref");
        TestComponentContext cc = new TestComponentContext();
        set.activate(cc);

        TestServiceReference srB = addReference(set, "B", 1);
        Assert.assertTrue("reference should replace", set.addReference(srB));
        Assert.assertEquals("single service should be returned", "B", set.getHighestRankedService());
        Assert.assertEquals("single service should be returned", "B", locateService(set.getHighestRankedReference()));
        Assert.assertEquals("single service should be returned", Arrays.asList("B"), list(set.getServices()));
        Assert.assertEquals("single service should be returned", Arrays.asList("B"), listServices(set.getServicesWithReferences()));

        addReference(set, "A", 2);
        Iterator<String> savedServices = set.getServices();
        Iterator<ServiceAndServiceReferencePair<String>> savedPairs = set.getServicesWithReferences();
        srB.ranking = 2;
        Assert.assertTrue("reference should replace", set.addReference(srB));
        Assert.assertEquals("highest ranked service should be returned", "B", set.getHighestRankedService());
        Assert.assertEquals("highest ranked service should be returned", "B", locateService(set.getHighestRankedReference()));
        Assert.assertEquals("pre-update services should return A before B", Arrays.asList("A", "B"), list(savedServices));
        Assert.assertEquals("pre-update services should return A before B", Arrays.asList("A", "B"), listServices(savedPairs));
        Assert.assertEquals("post-update services should return B before A", Arrays.asList("B", "A"), list(set.getServices()));
        Assert.assertEquals("post-update services should return B before a", Arrays.asList("B", "A"), listServices(set.getServicesWithReferences()));
    }

    private final Random rand = new Random();
    private final int numThreads = rand.nextInt(4) + 4;
    private int finThreads = 0;
    private boolean concurrentTestSuccessful = true;

    private final ArrayList<Throwable> exceptions = new ArrayList<Throwable>();
    private final ThreadGroup threadGroup = new ThreadGroup("testConcurrentThreads");
    private final ConcurrentServiceReferenceSet<String> concurrentSet = new ConcurrentServiceReferenceSet<String>("string");

    @Test
    public void testConcurrentThreads() {
        final String m_name = "testConcurrentThreads";

        ConcurrentServiceReferenceSetTestWorker test;

        // All we're trying to do here, in an informal way, is run with multiple threads
        // doing removes: the threads that are already running will be dealing with
        // this creation loop adding new elements and other threads removing elements.
        try {
            System.out.println(m_name + ": Testing concurrent threads: " + numThreads);

            concurrentSet.addReference(new TestServiceReference("initial"));
            concurrentSet.activate(new TestComponentContext());

            for (int i = 0; i < numThreads; i++) {
                String name;
                Thread t;

                name = new String("Thread-" + i);
                test = new ConcurrentServiceReferenceSetTestWorker(name, this);

                addReference(name);
                t = new Thread(threadGroup, test, name);

                t.start();
            }

            System.out.println(m_name + ": Waiting for threads to finish: " + activeThreads());
            waitForThreads();
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m_name, t);
        }

        if (!this.concurrentTestSuccessful) {
            System.out.println(m_name + ": Test encountered the following unexpected exceptions:");

            for (Throwable t : exceptions) {
                System.out.println(t);
                t.printStackTrace(System.out);
            }

            outputMgr.failWithThrowable(m_name, new Exception("Test encountered unexpected exceptions"));
        }
    }

    // ------------- Utility Methods --------------------------------------------------------

    private String activeThreads() {
        return Integer.toString(numThreads - finThreads);
    }

    public Iterator<String> getServices() {
        return concurrentSet.getServices();
    }

    public void addReference(String name) {
        if (concurrentSet.addReference(new TestServiceReference(name))) {
            System.out.println(Thread.currentThread().getName() + " REPLACED " + name);
        } else {
            System.out.println(Thread.currentThread().getName() + " ADDED " + name);
        }
    }

    public void removeReference(String name) {
        if (concurrentSet.removeReference(new TestServiceReference(name))) {
            System.out.println(Thread.currentThread().getName() + " REMOVED " + name);
        }
    }

    /**
     * Make main thread wait for connection threads to finish.
     */
    protected synchronized void waitForThreads() {
        try {
            if (finThreads < numThreads)
                wait();

            System.out.println("waitForThreads: Done");
        } catch (InterruptedException e) {
            System.out.println("waitForThreads: Interrupted");
            e.printStackTrace();
        }
    }

    /**
     * indicate that there is one less outstanding connection.
     */
    public synchronized void finishThread() {
        finThreads++;

        if (finThreads == numThreads)
            notifyAll();
    }

    public synchronized void setException(Throwable e) {
        this.exceptions.add(e);
        this.concurrentTestSuccessful = false;
    }

    static class TestComponentContext implements ComponentContext {
        AtomicInteger locateService = new AtomicInteger(0);
        Vector<String> invalidMethodCall = new Vector<String>();

        /** {@inheritDoc} */
        @SuppressWarnings("rawtypes")
        @Override
        public Dictionary getProperties() {
            invalidMethodCall.add("getProperties");
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public Object locateService(String name) {
            invalidMethodCall.add("locateService(name)");
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public Object locateService(String name, @SuppressWarnings("rawtypes") ServiceReference reference) {
            locateService.incrementAndGet();
            return ((TestServiceReference) reference).getName();
        }

        /** {@inheritDoc} */
        @Override
        public Object[] locateServices(String name) {
            invalidMethodCall.add("locateServices");
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public BundleContext getBundleContext() {
            invalidMethodCall.add("getBundleContext");
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public Bundle getUsingBundle() {
            invalidMethodCall.add("getUsingBundle");
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public ComponentInstance getComponentInstance() {
            invalidMethodCall.add("getComponentInstance");
            return null;
        }

        /** {@inheritDoc} */
        @Override
        public void enableComponent(String name) {
            invalidMethodCall.add("enableComponent");
        }

        /** {@inheritDoc} */
        @Override
        public void disableComponent(String name) {
            invalidMethodCall.add("disableComponent");
        }

        /** {@inheritDoc} */
        @SuppressWarnings("rawtypes")
        @Override
        public ServiceReference getServiceReference() {
            invalidMethodCall.add("getServiceReference");
            return null;
        }
    }
}
