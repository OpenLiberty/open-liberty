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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

/**
 *
 */
public class ConcurrentServiceReferenceMapTest {

    Mockery context = new Mockery();
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
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#deactivate()}.
     */
    @Test
    public void deactivateEmptyMap() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.activate(mockComponentContext);

        assertTrue("The map should be empty if nothing has been explicitly put",
                   map.isEmpty());

        map.deactivate(mockComponentContext);
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#deactivate()}.
     */
    @Test
    public void deactivatePopulatedMap() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
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
        assertNotNull("The service should be resolved as activate and a valid reference exist",
                      map.getService("key1"));
        context.assertIsSatisfied();

        context.checking(new Expectations() {
            {
                one(mockComponentContext).locateService("refName", mockServiceReference2);
            }
        });
        assertNotNull("The service should be resolved as activate and a valid reference exist",
                      map.getService("key2"));
        context.assertIsSatisfied();

        map.deactivate(mockComponentContext);
        assertFalse("Deactivation should not cause the map to be cleared",
                    map.isEmpty());
        assertNull("The service should be null once the Map is deactivated",
                   map.getService("key1"));
        assertNull("The service should be null once the Map is deactivated",
                   map.getService("key2"));
    }

    @Test
    public void deactivateIteratorHasNext() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.activate(mockComponentContext);
        map.putReference("key1", mockServiceReference1);
        Iterator<String> iterator = map.getServices();
        map.deactivate(mockComponentContext);

        assertFalse("Deactivation should cause iterator to return empty",
                    iterator.hasNext());
    }

    @Test(expected = NoSuchElementException.class)
    public void deactivateIteratorNext() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.activate(mockComponentContext);
        map.putReference("key1", mockServiceReference1);
        Iterator<String> iterator = map.getServices();
        map.deactivate(mockComponentContext);
        iterator.next();
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#putReference(java.lang.Object, org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void putReferenceNullKeyReturnsFalse() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        assertFalse("A null key should not NPE",
                    map.putReference(null, mockServiceReference1));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#putReference(java.lang.Object, org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void putReferenceNullValueReturnsFalse() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        assertFalse("A null value should not NPE",
                    map.putReference("key", null));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#putReference(java.lang.Object, org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void putReferenceReturnsFalseIfNotPreviousPut() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        assertFalse("put should return false since no previous put was done",
                    map.putReference("key", mockServiceReference1));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#putReference(java.lang.Object, org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void putReferenceReturnsTrueIfPreviousServiceExistedOnPut() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        assertFalse(map.putReference("key", mockServiceReference1));
        assertTrue("put should return true since this is replacing a previous value",
                   map.putReference("key", mockServiceReference1));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#removeReference(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void removeReferenceNullKey() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        assertFalse("A null key should not NPE",
                    map.removeReference(null, mockServiceReference1));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#removeReference(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void removeReferenceNullValue() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        assertFalse("A null value should not NPE",
                    map.removeReference("key", null));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#removeReference(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void removeReferenceBeforePut() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        assertFalse("remove should return false since nothing was removed",
                    map.removeReference("key", mockServiceReference1));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#removeReference(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void removeReferenceAfterPut() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        assertFalse(map.putReference("key", mockServiceReference1));
        assertTrue("remove should return true since the reference was removed",
                   map.removeReference("key", mockServiceReference1));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#removeReference(org.osgi.framework.ServiceReference)}.
     */
    @Test
    public void removeReferenceAfterUpdate() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        assertFalse(map.putReference("key", mockServiceReference1));
        assertTrue(map.putReference("key", mockServiceReference2));
        assertFalse("remove should return false since the reference has already been replaced",
                    map.removeReference("key", mockServiceReference1));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#isEmpty()}.
     */
    @Test
    public void isEmptyByDefault() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        assertTrue("map should be empty if no puts have been done",
                   map.isEmpty());
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#isEmpty()}.
     */
    @Test
    public void isEmptyAfterPut() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.putReference("key1", mockServiceReference1);
        assertFalse("map should not be empty with one put",
                    map.isEmpty());
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#isEmpty()}.
     */
    @Test
    public void isEmptyAfterPutAndRemove() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.putReference("key1", mockServiceReference1);
        assertFalse("map should not be empty with one put",
                    map.isEmpty());
        map.removeReference("key1", mockServiceReference1);
        assertTrue("map should be empty after all references removed",
                   map.isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.utils.ConcurrentServiceReferenceMap#size()}.
     */
    @Test
    public void sizeIsZeroByDefault() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        assertEquals("map should have zero elements by default",
                     0, map.size());
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.utils.ConcurrentServiceReferenceMap#size()}.
     */
    @Test
    public void sizeIsOneAfterPut() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.putReference("key1", mockServiceReference1);
        assertEquals("map should have one element after put",
                     1, map.size());
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.utils.ConcurrentServiceReferenceMap#size()}.
     */
    @Test
    public void sizeIsTwoAfterPut() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.putReference("key1", mockServiceReference1);
        assertEquals("map should have one element after first put",
                     1, map.size());

        map.putReference("key2", mockServiceReference1);
        assertEquals("map should have two elements after second put",
                     2, map.size());
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.utils.ConcurrentServiceReferenceMap#size()}.
     */
    @Test
    public void sizeChangesAfterPutAndRemove() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.putReference("key1", mockServiceReference1);
        assertEquals("map should have one element after first put",
                     1, map.size());

        map.putReference("key2", mockServiceReference1);
        assertEquals("map should have two elements after second put",
                     2, map.size());

        map.removeReference("key1", mockServiceReference1);
        assertEquals("map should have one element after first remove",
                     1, map.size());

        map.removeReference("key2", mockServiceReference1);
        assertEquals("map should have no elements after second remove",
                     0, map.size());
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.utils.ConcurrentServiceReferenceMap#keySet()}.
     */
    @Test
    public void keySetIsEmptyByDefault() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        assertTrue("map should have zero keys by default",
                   map.keySet().isEmpty());
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.utils.ConcurrentServiceReferenceMap#keySet()}.
     */
    @Test
    public void keySetAfterPut() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.putReference("key1", mockServiceReference1);
        assertFalse("map should have one element after put",
                    map.keySet().isEmpty());
        assertEquals("map should have only one key",
                     1, map.keySet().size());
        assertTrue("map should have one key named key1",
                   map.keySet().contains("key1"));
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.utils.ConcurrentServiceReferenceMap#keySet()}.
     */
    @Test
    public void keySetAfterTwoPuts() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.putReference("key1", mockServiceReference1);
        assertEquals("map should have only one key",
                     1, map.keySet().size());
        assertTrue("map should have one key named key1",
                   map.keySet().contains("key1"));

        map.putReference("key2", mockServiceReference1);
        assertEquals("map should have two keys",
                     2, map.keySet().size());
        assertTrue("map should have one key named key1",
                   map.keySet().contains("key1"));
        assertTrue("map should have one key named key2",
                   map.keySet().contains("key2"));
    }

    /**
     * Test method for {@link com.ibm.ws.kernel.service.utils.ConcurrentServiceReferenceMap#keySet()}.
     */
    @Test
    public void keySetChangesAfterPutAndRemove() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.putReference("key1", mockServiceReference1);
        assertEquals("map should have only one key",
                     1, map.keySet().size());
        assertTrue("map should have one key named key1",
                   map.keySet().contains("key1"));

        map.putReference("key2", mockServiceReference1);
        assertEquals("map should have two keys",
                     2, map.keySet().size());
        assertTrue("map should have one key named key1",
                   map.keySet().contains("key1"));
        assertTrue("map should have one key named key2",
                   map.keySet().contains("key2"));

        map.removeReference("key1", mockServiceReference1);
        assertEquals("map should have only one key",
                     1, map.keySet().size());
        assertTrue("map should have one key named key2",
                   map.keySet().contains("key2"));

        map.removeReference("key2", mockServiceReference1);
        assertEquals("map should have no elements after second remove",
                     0, map.keySet().size());
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#getService(java.lang.Object)}.
     */
    @Test
    public void getServiceWithNullContextReturnsNull() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        assertNull("A null service is returned if the context is null",
                   map.getService("ignored"));

        map.activate(mockComponentContext);
        map.deactivate(mockComponentContext);

        assertNull("A null service is returned if the context is null",
                   map.getService("ignored"));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#getService(java.lang.Object)}.
     */
    @Test
    public void getServiceWithNullKeyReturnsNull() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.activate(mockComponentContext);
        assertNull("A null key should return a null",
                   map.getService(null));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#getService(java.lang.Object)}.
     */
    @Test
    public void getServiceWithInvalidKeyReturnsNull() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.activate(mockComponentContext);
        assertNull("An invalid key should return a null",
                   map.getService("invalidKey"));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#getService(java.lang.Object)}.
     */
    @Test
    public void getServiceWithValidKeyReturnsService() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.putReference("key1", mockServiceReference1);
        map.activate(mockComponentContext);

        context.checking(new Expectations() {
            {
                one(mockComponentContext).locateService("refName", mockServiceReference1);
            }
        });
        assertNotNull("The service should be resolved as activate and a valid reference exist",
                      map.getService("key1"));
        context.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#getService(java.lang.Object)}.
     */
    @Test
    public void getServiceDeactivatedReturnsNull() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.putReference("key1", mockServiceReference1);
        map.activate(mockComponentContext);

        context.checking(new Expectations() {
            {
                one(mockComponentContext).locateService("refName", mockServiceReference1);
            }
        });
        assertNotNull("The service should be resolved as activate and a valid reference exist",
                      map.getService("key1"));
        context.assertIsSatisfied();

        map.deactivate(mockComponentContext);
        assertNull("After deactivate, services should be null",
                   map.getService("key1"));

    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#getServiceWithException(Object)}.
     */
    @Test(expected = IllegalStateException.class)
    public void getServiceWithExceptionWithNullContext() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.getServiceWithException("ignored");
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#getServiceWithException(Object)}.
     */
    @Test(expected = IllegalStateException.class)
    public void getServiceWithWithExceptionNullContextAfterDeactivate() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");

        map.activate(mockComponentContext);
        map.deactivate(mockComponentContext);

        map.getServiceWithException("ignored");
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#getServiceWithException(Object)}.
     */
    @Test(expected = IllegalStateException.class)
    public void getServiceWithExceptionNullKey() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.activate(mockComponentContext);
        map.getServiceWithException(null);
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#getServiceWithException(Object)}.
     */
    @Test(expected = IllegalStateException.class)
    public void getServiceWithExceptionWithInvalidKey() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.activate(mockComponentContext);
        assertNull("An invalid key should return a null",
                   map.getServiceWithException("invalidKey"));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#getServiceWithException(Object)}.
     */
    @Test(expected = IllegalStateException.class)
    public void getServiceWithExceptionWithValidKeyButServiceIsNull() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.putReference("key1", mockServiceReference1);
        map.activate(mockComponentContext);

        context.checking(new Expectations() {
            {
                one(mockComponentContext).locateService("refName", mockServiceReference1);
                will(returnValue(null));
            }
        });
        map.getServiceWithException("key1");
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#getServiceWithException(Object)}.
     */
    @Test
    public void getServiceWithExceptionWithValidKey() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.putReference("key1", mockServiceReference1);
        map.activate(mockComponentContext);

        context.checking(new Expectations() {
            {
                one(mockComponentContext).locateService("refName", mockServiceReference1);
            }
        });
        assertNotNull("The service should be resolved as activate and a valid reference exist",
                      map.getServiceWithException("key1"));
        context.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#getService(java.lang.Object)}.
     */
    @Test(expected = IllegalStateException.class)
    public void getServiceWithExceptionDeactivatedReturnsNull() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.putReference("key1", mockServiceReference1);
        map.activate(mockComponentContext);

        context.checking(new Expectations() {
            {
                one(mockComponentContext).locateService("refName", mockServiceReference1);
            }
        });
        assertNotNull("The service should be resolved as activate and a valid reference exist",
                      map.getServiceWithException("key1"));
        context.assertIsSatisfied();

        map.deactivate(mockComponentContext);
        map.getServiceWithException("key1");

    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#getReference(java.lang.Object)}.
     */
    @Test
    public void getReferenceWithNullKey() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        assertNull("A null key should return a null",
                   map.getReference(null));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#getReference(java.lang.Object)}.
     */
    @Test
    public void getReferenceWithInvalidKey() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        assertNull("An invalid key should return a null",
                   map.getReference("invalidKey"));
    }

    /**
     * Test method for {@link com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap#getReference(java.lang.Object)}.
     */
    @Test
    public void getReferenceWithValidKey() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.putReference("key", mockServiceReference1);
        assertSame("A valid key should return the associated reference",
                   mockServiceReference1, map.getReference("key"));
    }

    /**
     * Test to make sure that {@link ConcurrentServiceReferenceMap#putReferenceIfAbsent(Object, ServiceReference)} allows you to add a reference to an empty map
     */
    @Test
    public void putIfAbsentWithEmptyMap() {
        // Add the reference
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        ServiceReference<String> returnedValue = map.putReferenceIfAbsent("key", mockServiceReference1);
        assertNull("There was no existing entry so put if absent should return null", returnedValue);
        assertSame("Put if absent should add the reference if it didn't already exist in the map",
                   mockServiceReference1, map.getReference("key"));
    }

    /**
     * Test to make sure that {@link ConcurrentServiceReferenceMap#putReferenceIfAbsent(Object, ServiceReference)} doesn't replace an existing entry but instead returns it
     */
    @Test
    public void putIfAbsentWithMatchingEntry() {
        // First add a reference under a key
        String KEY = "key";
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.putReference(KEY, mockServiceReference1);

        // Now try to put a second one, we should get the first one back from the method and when we call get
        ServiceReference<String> returnedValue = map.putReferenceIfAbsent(KEY, mockServiceReference2);
        assertSame("Put if absent should return the existing value if there is one",
                   mockServiceReference1, returnedValue);
        assertSame("Put if absent shouldn't replace an existing value",
                   mockServiceReference1, map.getReference("key"));
    }

    /**
     * Test to make sure that {@link ConcurrentServiceReferenceMap#putReferenceIfAbsent(Object, ServiceReference)} doesn't return an existing entry registered against a different
     * key
     */
    @Test
    public void putIfAbsentWithDifferentEntry() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        map.putReference("key", mockServiceReference1);

        // Now try to put a second one with a different key, we should get the null back from the method and when we call get we should get the second reference
        ServiceReference<String> returnedValue = map.putReferenceIfAbsent("different", mockServiceReference2);
        assertNull("Something was returned from put if absent when a new key was being used", returnedValue);
        assertSame("Put if absent hasn't put the new reference under the new key",
                   mockServiceReference2, map.getReference("different"));
    }

    /**
     * Test to make sure that {@link ConcurrentServiceReferenceMap#putReferenceIfAbsent(Object, ServiceReference)} returns null when the key is null
     */
    @Test
    public void putIfAbsentWithNullKey() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        ServiceReference<String> returnedValue = map.putReferenceIfAbsent(null, mockServiceReference1);
        assertNull("Put if absent should always return null when null is used as the key", returnedValue);
        assertNull("A service reference was registered for null", map.getReference(null));
    }

    /**
     * Test to make sure that {@link ConcurrentServiceReferenceMap#putReferenceIfAbsent(Object, ServiceReference)} doesn't add an entry when the reference is null
     */
    @Test
    public void putIfAbsentWithNullReference() {
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        ServiceReference<String> returnedValue = map.putReferenceIfAbsent("key", null);
        assertNull("Put if absent should return null when null is used as the reference and there is no entry", returnedValue);
        assertEquals("A null service reference was registered", 0, map.size());
    }

    /**
     * Test to make sure that {@link ConcurrentServiceReferenceMap#putReferenceIfAbsent(Object, ServiceReference)} doesn't add an entry when the reference is null but does return
     * the existing one if there is one
     */
    @Test
    public void putIfAbsentWithNullReferenceExistingKey() {
        // First add the test reference
        ConcurrentServiceReferenceMap<String, String> map = new ConcurrentServiceReferenceMap<String, String>("refName");
        final String KEY = "key";
        map.putReference(KEY, mockServiceReference1);

        // Now put a null reference, should get the first one back and not have it replaced
        ServiceReference<String> returnedValue = map.putReferenceIfAbsent(KEY, null);
        assertSame("Put if absent should return the old reference when null is used as the reference and there is an entry", mockServiceReference1, returnedValue);
        assertSame("The existing entry was replaced with a null value", mockServiceReference1, map.getReference(KEY));
    }

}
