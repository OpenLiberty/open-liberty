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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

/**
 *
 */
@RunWith(JMock.class)
public class ConcurrentServiceReferenceSetMapTest {

    Mockery context = new JUnit4Mockery();
    @SuppressWarnings("unchecked")
    ServiceReference<String> mockServiceReference1 = context.mock(ServiceReference.class, "ref1");
    @SuppressWarnings("unchecked")
    ServiceReference<String> mockServiceReference2 = context.mock(ServiceReference.class, "ref2");
    ComponentContext mockComponentContext = context.mock(ComponentContext.class);

    @Before
    public void setUp() {
        context.checking(new Expectations() {
            {
                allowing(mockServiceReference1).getProperty(Constants.SERVICE_ID);
                will(returnValue(0L));
                allowing(mockServiceReference1).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
                allowing(mockServiceReference2).getProperty(Constants.SERVICE_ID);
                will(returnValue(1L));
                allowing(mockServiceReference2).getProperty(Constants.SERVICE_RANKING);
                will(returnValue(0));
            }
        });
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSetMap#deactivate()}.
     */
    @Test
    public void deactivateEmptyMap() {
        ConcurrentServiceReferenceSetMap<String, String> map = new ConcurrentServiceReferenceSetMap<String, String>("refName");
        map.activate(mockComponentContext);

        assertTrue("The map should be empty if nothing has been explicitly put",
                   map.isEmpty());

        map.deactivate(mockComponentContext);
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSetMap#deactivate()}.
     */
    @Test
    public void deactivatePopulatedMap() {
        ConcurrentServiceReferenceSetMap<String, String> map = new ConcurrentServiceReferenceSetMap<String, String>("refName");
        map.putReference("key1", mockServiceReference1);
        map.putReference("key2", mockServiceReference2);
        map.activate(mockComponentContext);

        assertFalse("The map should not be empty since 2 references were put",
                    map.isEmpty());

        context.checking(new Expectations() {
            {
                one(mockComponentContext).locateService("refName", mockServiceReference1);
            }
        });

        Iterator<String> services = map.getServices("key1");
        assertNotNull("a. getServices should never return null", services);
        assertTrue("b. The service should be resolved as activate and a valid reference exist",
                   services.hasNext());
        assertNotNull("c. The service should be resolved as activate and a valid reference exist",
                      services.next());

        context.assertIsSatisfied();

        context.checking(new Expectations() {
            {
                one(mockComponentContext).locateService("refName", mockServiceReference2);
            }
        });

        services = map.getServices("key2");
        assertNotNull("a. getServices should never return null", services);
        assertTrue("b. The service should be resolved as activate and a valid reference exist",
                   services.hasNext());
        assertNotNull("b. The service should be resolved as activate and a valid reference exist",
                      services.next());
        context.assertIsSatisfied();

        map.deactivate(mockComponentContext);
        assertFalse("Deactivation should not cause the map to be cleared",
                    map.isEmpty());
        assertNull("The service should be null once the Map is deactivated",
                   map.getServices("key1"));
        assertNull("The service should be null once the Map is deactivated",
                   map.getServices("key2"));
    }

    @Test
    public void deactivateIteratorHasNext() {
        ConcurrentServiceReferenceSetMap<String, String> map = new ConcurrentServiceReferenceSetMap<String, String>("refName");
        map.putReference("key1", mockServiceReference1);
        map.activate(mockComponentContext);
        Iterator<String> iterator = map.getServices("key1");
        map.deactivate(mockComponentContext);

        assertFalse("Deactivation should cause iterator to return empty",
                    iterator.hasNext());
    }

    @Test(expected = NoSuchElementException.class)
    public void deactivateIteratorNext() {
        ConcurrentServiceReferenceSetMap<String, String> map = new ConcurrentServiceReferenceSetMap<String, String>("refName");
        map.putReference("key1", mockServiceReference1);
        map.activate(mockComponentContext);
        Iterator<String> iterator = map.getServices("key1");
        map.deactivate(mockComponentContext);
        iterator.next();
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSetMap#putReference(java.lang.Object, org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void putReferenceNullKeyReturnsFalse() {
        ConcurrentServiceReferenceSetMap<String, String> map = new ConcurrentServiceReferenceSetMap<String, String>("refName");
        assertFalse("A null key should not NPE",
                    map.putReference(null, mockServiceReference1));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSetMap#putReference(java.lang.Object, org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void putReferenceNullValueReturnsFalse() {
        ConcurrentServiceReferenceSetMap<String, String> map = new ConcurrentServiceReferenceSetMap<String, String>("refName");
        assertFalse("A null value should not NPE",
                    map.putReference("key", null));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSetMap#putReference(java.lang.Object, org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void putReferenceReturnsFalseIfNotPreviousPut() {
        ConcurrentServiceReferenceSetMap<String, String> map = new ConcurrentServiceReferenceSetMap<String, String>("refName");
        assertFalse("put should return false since no previous put was done",
                    map.putReference("key", mockServiceReference1));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSetMap#putReference(java.lang.Object, org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void putReferenceReturnsTrueIfPreviousServiceExistedOnPut() {
        ConcurrentServiceReferenceSetMap<String, String> map = new ConcurrentServiceReferenceSetMap<String, String>("refName");
        assertFalse(map.putReference("key", mockServiceReference1));
        assertTrue("put should return true since this is replacing a previous value",
                   map.putReference("key", mockServiceReference1));
        context.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSetMap#removeReference(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void removeReferenceNullKey() {
        ConcurrentServiceReferenceSetMap<String, String> map = new ConcurrentServiceReferenceSetMap<String, String>("refName");
        assertFalse("A null key should not NPE",
                    map.removeReference(null, mockServiceReference1));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSetMap#removeReference(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void removeReferenceNullValue() {
        ConcurrentServiceReferenceSetMap<String, String> map = new ConcurrentServiceReferenceSetMap<String, String>("refName");
        assertFalse("A null value should not NPE",
                    map.removeReference("key", null));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSetMap#removeReference(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void removeReferenceBeforePut() {
        ConcurrentServiceReferenceSetMap<String, String> map = new ConcurrentServiceReferenceSetMap<String, String>("refName");
        assertFalse("remove should return false since nothing was removed",
                    map.removeReference("key", mockServiceReference1));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSetMap#removeReference(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void removeReferenceAfterPut() {
        ConcurrentServiceReferenceSetMap<String, String> map = new ConcurrentServiceReferenceSetMap<String, String>("refName");
        assertFalse(map.putReference("key", mockServiceReference1));
        assertTrue("remove should return true since the reference was removed",
                   map.removeReference("key", mockServiceReference1));
        context.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSetMap#removeReference(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void removeReferenceAfterUpdate() {
        ConcurrentServiceReferenceSetMap<String, String> map = new ConcurrentServiceReferenceSetMap<String, String>("refName");
        assertFalse(map.putReference("key", mockServiceReference1));
        assertFalse(map.putReference("key", mockServiceReference2));
        assertTrue("remove should return false since the reference is still in the set",
                   map.removeReference("key", mockServiceReference1));
        context.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSetMap#isEmpty()}.
     */
    @Test
    public void isEmptyByDefault() {
        ConcurrentServiceReferenceSetMap<String, String> map = new ConcurrentServiceReferenceSetMap<String, String>("refName");
        assertTrue("map should be empty if no puts have been done",
                   map.isEmpty());
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSetMap#isEmpty()}.
     */
    @Test
    public void isEmptyAfterPut() {
        ConcurrentServiceReferenceSetMap<String, String> map = new ConcurrentServiceReferenceSetMap<String, String>("refName");
        map.putReference("key1", mockServiceReference1);
        assertFalse("map should not be empty with one put",
                    map.isEmpty());
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSetMap#isEmpty()}.
     */
    @Test
    public void isEmptyAfterPutAndRemove() {
        ConcurrentServiceReferenceSetMap<String, String> map = new ConcurrentServiceReferenceSetMap<String, String>("refName");
        map.putReference("key1", mockServiceReference1);
        assertFalse("map should not be empty with one put",
                    map.isEmpty());
        map.removeReference("key1", mockServiceReference1);
        assertTrue("map should be empty after all references removed",
                   map.isEmpty());
        context.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSetMap#getService(java.lang.Object)}.
     */
    @Test
    public void getServiceWithNullContextReturnsNull() {
        ConcurrentServiceReferenceSetMap<String, String> map = new ConcurrentServiceReferenceSetMap<String, String>("refName");
        assertNull("A null service is returned if the context is null",
                   map.getServices("ignored"));

        map.activate(mockComponentContext);
        map.deactivate(mockComponentContext);

        assertNull("A null service is returned if the context is null",
                   map.getServices("ignored"));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSetMap#getService(java.lang.Object)}.
     */
    @Test
    public void getServiceWithNullKeyReturnsNull() {
        ConcurrentServiceReferenceSetMap<String, String> map = new ConcurrentServiceReferenceSetMap<String, String>("refName");
        map.activate(mockComponentContext);
        assertNull("A null key should return a null",
                   map.getServices(null));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSetMap#getService(java.lang.Object)}.
     */
    @Test
    public void getServiceWithInvalidKeyReturnsNull() {
        ConcurrentServiceReferenceSetMap<String, String> map = new ConcurrentServiceReferenceSetMap<String, String>("refName");
        map.activate(mockComponentContext);
        assertNull("An invalid key should return a null",
                   map.getServices("invalidKey"));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSetMap#getService(java.lang.Object)}.
     */
    @Test
    public void getServiceWithValidKeyReturnsService() {
        ConcurrentServiceReferenceSetMap<String, String> map = new ConcurrentServiceReferenceSetMap<String, String>("refName");
        map.putReference("key1", mockServiceReference1);
        map.activate(mockComponentContext);

        context.checking(new Expectations() {
            {
                one(mockComponentContext).locateService("refName", mockServiceReference1);
            }
        });
        Iterator<String> services = map.getServices("key1");
        assertNotNull("a. getServices should never return null", services);

        assertTrue("a. The service should be resolved as activate and a valid reference exist",
                   services.hasNext());
        assertNotNull("b. The service should be resolved as activate and a valid reference exist",
                      services.next());
        context.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSetMap#getService(java.lang.Object)}.
     */
    @Test
    public void getServiceDeactivatedReturnsNull() {
        ConcurrentServiceReferenceSetMap<String, String> map = new ConcurrentServiceReferenceSetMap<String, String>("refName");
        map.putReference("key1", mockServiceReference1);
        map.activate(mockComponentContext);

        context.checking(new Expectations() {
            {
                one(mockComponentContext).locateService("refName", mockServiceReference1);
            }
        });

        Iterator<String> services = map.getServices("key1");
        assertNotNull("a. getServices should never return null", services);
        assertTrue("b. The service should be resolved as activate and a valid reference exist",
                   services.hasNext());
        assertNotNull("c. The service should be resolved as activate and a valid reference exist",
                      services.next());

        context.assertIsSatisfied();

        map.deactivate(mockComponentContext);
        assertNull("After deactivate, services should be null",
                   map.getServices("key1"));

    }

    //TODO: test cases for getServicesWithReferences

}
