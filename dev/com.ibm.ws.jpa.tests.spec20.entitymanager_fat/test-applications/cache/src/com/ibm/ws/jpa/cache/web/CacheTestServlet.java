/*******************************************************************************
` * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.cache.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Resource;
import javax.persistence.Cache;
import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Ignore;
import org.junit.Test;

import com.ibm.ws.jpa.cache.model.CacheEntityCacheable;
import com.ibm.ws.jpa.cache.model.CacheEntityCacheableNot;
import com.ibm.ws.jpa.cache.model.CacheEntityCollection;
import com.ibm.ws.jpa.cache.model.CacheEntitySimple1;
import com.ibm.ws.jpa.cache.model.CacheEntitySimple2;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/CacheTestServlet")
public class CacheTestServlet extends FATServlet {

    // cache size is 20, softreference is 0 for OpenJPA
    final int cacheSize = 20;

    @PersistenceContext(unitName = "CACHE_JEE_DCTRUE_COPY_DROP_AND_CREATE")
    EntityManager em_CACHE_JEE_DCTRUE_DAC;

    @PersistenceContext(unitName = "CACHE_JEE_DCFALSE")
    EntityManager em_CACHE_JEE_DCFALSE;

    @PersistenceContext(unitName = "CACHE_JEE_DCTRUE_SCM_ENASEL")
    EntityManager em_CACHE_JEE_DCTRUE_SCM_ENASEL;

    @PersistenceContext(unitName = "CACHE_JEE_DCTRUE_SCM_DISSEL")
    EntityManager em_CACHE_JEE_DCTRUE_SCM_DISSEL;

    @PersistenceUnit(unitName = "CACHE_JEE_DCTRUE_COPY")
    EntityManagerFactory emf_CACHE_JEE_DCTRUE_COPY;

    @Resource
    private UserTransaction tx;

    @Override
    public void init() throws ServletException {
        super.init();
        // Poke the EntityManager to trigger drop-and-create before any tests start running
        em_CACHE_JEE_DCTRUE_DAC.clear();
    }

    @Override
    protected void before() throws Exception {
        em_CACHE_JEE_DCTRUE_DAC.clear();
        em_CACHE_JEE_DCTRUE_DAC.getEntityManagerFactory().getCache().evictAll();
    }

    /**
     * <h2>Test Logic: testCache001 - Test EntityManagerFactory.getCache()</h2>
     * <p>
     * Test getting the L2 cache via emf.getCache and oemf.getCache. Make sure
     * that no matter which way it is retrieved, it is an instance of both Cache
     * and StoreCache.
     * <p>
     * This should work whether openjpa.DataCache is set to true or set to
     * false in the persistence unit.
     * <p>
     * Note: Getting the cache does NOT mean that OpenJPA caching is actually active.
     */
    @Test
    public void jpa_jpa20_cache_testCache001() throws Exception {
        // Make sure the l2 cache is not null
        Cache l2cache = em_CACHE_JEE_DCTRUE_DAC.getEntityManagerFactory().getCache();
        assertNotNull(l2cache);

        // Get the l2 cache again and make sure it's the same
        Cache l2cache2 = em_CACHE_JEE_DCTRUE_DAC.getEntityManagerFactory().getCache();
        assertEquals(l2cache, l2cache2);
    }

    /**
     * <h2>Test Logic: testCache003 - Test all Cache methods with parameter variations on an "empty" cache</h2>
     * <p>
     * This test will cover combinations of parameters and test all methods on a cache
     * that has no entries.
     */
    @Test
    public void jpa_jpa20_cache_testCache003() {

        Cache l2cache = em_CACHE_JEE_DCTRUE_DAC.getEntityManagerFactory().getCache();

        assertFalse(l2cache.contains(CacheEntitySimple1.class, 1));

        try {
            l2cache.contains(null, null);
            fail("contains(null,null) did not throw a RuntimeException");
        } catch (RuntimeException expected) {
        }

        try {
            l2cache.contains(null, 1);
            fail("contains(null,1) did not throw a RuntimeException");
        } catch (RuntimeException expected) {
        }

        assertFalse("contains(CacheEntitySimple1, null) returned false",
                    l2cache.contains(CacheEntitySimple1.class, null));

        // Test evict(CacheEntitySimple1.class, 1) does nothing
        l2cache.evict(CacheEntitySimple1.class, 1);

        // Test evict(null, null) throws RuntimeException
        try {
            l2cache.evict(null, null);
            fail("evict(null,null) did not throw a RuntimeException");
        } catch (RuntimeException expected) {
        }

        // Test evict(null, 1) throws RuntimeException
        try {
            l2cache.evict(null, 1);
            fail("evict(null,1) did not throw a RuntimeException");
        } catch (RuntimeException expected) {
        }

        // Test evict(CacheEntitySimple1.class, null) is ignored
        l2cache.evict(CacheEntitySimple1.class, null);

        // Test evict(CacheEntitySimple1.class) is ignored
        l2cache.evict(CacheEntitySimple1.class);

        // Test evict(null) thows RuntimeException");
        try {
            l2cache.evict(null);
            fail("evict(null) did not throw a RuntimeException");
        } catch (RuntimeException expected) {
        }

        // Test evictAll() is ignored");
        l2cache.evictAll();
    }

    /**
     * <h4>Test Logic: testCache004 - Test Cache.contains(Class, key)</h4>
     * <p>
     * Test using contains(Class, key) to detect the presence of a particular
     * entity in the l2 cache with a number of combinations of entities.
     *
     */
    @Test
    public void jpa_jpa20_cache_testCache004() throws Exception {

        Cache l2cache = em_CACHE_JEE_DCTRUE_DAC.getEntityManagerFactory().getCache();

        // 1. contains should find an entity in the l2 cache
        CacheEntitySimple1 newEntitySimple1_test1 = new CacheEntitySimple1();
        newEntitySimple1_test1.setId(4001);
        newEntitySimple1_test1.setIntVal(4001);
        newEntitySimple1_test1.setStrVal("CacheEntitySimple1 4001");

        tx.begin();
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1_test1);

        assertFalse(l2cache.contains(CacheEntitySimple1.class, 4001));

        tx.commit();

        // Verify the entity is in the l2 cache after the commit
        assertTrue(l2cache.contains(CacheEntitySimple1.class, 4001));

        // 2. contains should not find entity if class matches, but not key
        //    Verify an entity of the same class, but a different key is NOT in the l2 cache
        assertFalse(l2cache.contains(CacheEntitySimple1.class, 4002));

        // 3. contains should not find entity if key matches, but not class
        //    Verify an entity of the same key, but a different class is NOT in the l2 cache
        assertFalse(l2cache.contains(CacheEntitySimple2.class, 4001));

        // 4. contains finds an entity if it is in the l2 cache, but then
        //    does not find the entity after it is evicted by class and key

        // Verify the entity is in the l2 cache after the commit");
        assertTrue(l2cache.contains(CacheEntitySimple1.class, 4001));
        l2cache.evict(CacheEntitySimple1.class, 4001);
        assertFalse(l2cache.contains(CacheEntitySimple1.class, 4001));

        // 5. put in two entities of different classes with the same key.
        //    contains should find each entity.  Then evict one entity.  contains
        //    should find the non-evicted, but not the evicted.  Then evict the other
        //    entity.  contains should not find either entity.

        CacheEntitySimple1 newEntitySimple1_test5 = new CacheEntitySimple1();
        CacheEntitySimple2 newEntitySimple2_test5 = new CacheEntitySimple2();

        newEntitySimple1_test5.setId(4003);
        newEntitySimple1_test5.setIntVal(4003);
        newEntitySimple1_test5.setStrVal("CacheEntitySimple1 4003");

        newEntitySimple2_test5.setId(4003);
        newEntitySimple2_test5.setIntVal(4003);
        newEntitySimple2_test5.setStrVal("CacheEntitySimple2 4003");

        tx.begin();
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1_test5);
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple2_test5);

        tx.commit();

        // Verify the entities are in the l2 cache after the commit and then evict one
        assertTrue(l2cache.contains(CacheEntitySimple1.class, 4003));
        assertTrue(l2cache.contains(CacheEntitySimple2.class, 4003));
        l2cache.evict(CacheEntitySimple1.class, 4003);
        assertFalse(l2cache.contains(CacheEntitySimple1.class, 4003));
        assertTrue(l2cache.contains(CacheEntitySimple2.class, 4003));

        // 6. put in two entities of the same class with different keys.
        //    contains should find both entities.  Then evict one entity. Contains
        //    should find the non-evicted, but not the evicted.  Then evict the other
        //    entity. Contains should not find either entity.

        CacheEntitySimple1 newEntitySimple1_test6a = new CacheEntitySimple1();
        CacheEntitySimple1 newEntitySimple1_test6b = new CacheEntitySimple1();

        // Persist the new entity to the database by starting a tran, persist, then commit
        tx.begin();

        newEntitySimple1_test6a.setId(4004);
        newEntitySimple1_test6a.setIntVal(4004);
        newEntitySimple1_test6a.setStrVal("CacheEntitySimple1 4004");
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1_test6a);

        newEntitySimple1_test6b.setId(4005);
        newEntitySimple1_test6b.setIntVal(4005);
        newEntitySimple1_test6b.setStrVal("CacheEntitySimple1 4005");
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1_test6b);

        tx.commit();

        // Verify the entities are in the l2 cache after the commit
        assertTrue(l2cache.contains(CacheEntitySimple1.class, 4004));
        assertTrue(l2cache.contains(CacheEntitySimple1.class, 4005));
        l2cache.evict(CacheEntitySimple1.class, 4004);
        assertFalse(l2cache.contains(CacheEntitySimple1.class, 4004));
        assertTrue(l2cache.contains(CacheEntitySimple1.class, 4005));

        // 7. contains should not find an entity after it is evicted, but
        //    should find an entity after it is found again.
        CacheEntitySimple1 newEntitySimple1_test7 = new CacheEntitySimple1();

        tx.begin();

        newEntitySimple1_test7.setId(4006);
        newEntitySimple1_test7.setIntVal(4006);
        newEntitySimple1_test7.setStrVal("CacheEntitySimple1 4006");
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1_test7);

        tx.commit();

        // "Verify the entities are in the l2 cache after the commit
        assertTrue(l2cache.contains(CacheEntitySimple1.class, 4006));
        l2cache.evict(CacheEntitySimple1.class, 4006);
        assertFalse(l2cache.contains(CacheEntitySimple1.class, 4006));

        em_CACHE_JEE_DCTRUE_DAC.clear();

        CacheEntitySimple1 findEntity = em_CACHE_JEE_DCTRUE_DAC.find(CacheEntitySimple1.class, 4006);

        assertNotNull(findEntity);

        assertEquals(findEntity.getId(), 4006);

        assertTrue(l2cache.contains(CacheEntitySimple1.class, 4006));

        // 8. contains finds an entity if it is in the l2 cache, but then
        //    does not find the entity after all entities by the class are evicted.
        CacheEntitySimple1 newEntitySimple1_test8a = new CacheEntitySimple1();
        CacheEntitySimple1 newEntitySimple1_test8b = new CacheEntitySimple1();

        tx.begin();

        newEntitySimple1_test8a.setId(4007);
        newEntitySimple1_test8a.setIntVal(4007);
        newEntitySimple1_test8a.setStrVal("CacheEntitySimple1 4007");
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1_test8a);

        newEntitySimple1_test8b.setId(4008);
        newEntitySimple1_test8b.setIntVal(4008);
        newEntitySimple1_test8b.setStrVal("CacheEntitySimple1 4008");
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1_test8b);

        tx.commit();
        // Verify the entities are in the l2 cache after the commit
        assertTrue(l2cache.contains(CacheEntitySimple1.class, 4007));
        assertTrue(l2cache.contains(CacheEntitySimple1.class, 4008));

        l2cache.evict(CacheEntitySimple1.class);

        assertFalse(l2cache.contains(CacheEntitySimple1.class, 4007));
        assertFalse(l2cache.contains(CacheEntitySimple1.class, 4008));

        // 9. contains finds an entity if it is in the l2 cache, but then
        //    does not find the entity after all entities are evicted.
        CacheEntitySimple1 newEntitySimple1_test9a = new CacheEntitySimple1();
        CacheEntitySimple1 newEntitySimple1_test9b = new CacheEntitySimple1();
        CacheEntitySimple2 newEntitySimple2_test9a = new CacheEntitySimple2();
        CacheEntitySimple2 newEntitySimple2_test9b = new CacheEntitySimple2();

        tx.begin();

        newEntitySimple1_test9a.setId(4009);
        newEntitySimple1_test9a.setIntVal(4009);
        newEntitySimple1_test9a.setStrVal("CacheEntitySimple1 4009");
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1_test9a);

        newEntitySimple1_test9b.setId(4010);
        newEntitySimple1_test9b.setIntVal(4010);
        newEntitySimple1_test9b.setStrVal("CacheEntitySimple1 4010");
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1_test9b);

        newEntitySimple2_test9a.setId(4011);
        newEntitySimple2_test9a.setIntVal(4011);
        newEntitySimple2_test9a.setStrVal("CacheEntitySimple2 4011");
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple2_test9a);

        newEntitySimple2_test9b.setId(4012);
        newEntitySimple2_test9b.setIntVal(4012);
        newEntitySimple2_test9b.setStrVal("CacheEntitySimple2 4012");
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple2_test9b);

        tx.commit();
        // Verify the entities are in the l2 cache after the commit
        assertTrue(l2cache.contains(CacheEntitySimple1.class, 4009));
        assertTrue(l2cache.contains(CacheEntitySimple1.class, 4010));
        assertTrue(l2cache.contains(CacheEntitySimple2.class, 4011));
        assertTrue(l2cache.contains(CacheEntitySimple2.class, 4012));

        l2cache.evictAll();

        assertFalse(l2cache.contains(CacheEntitySimple1.class, 4009));
        assertFalse(l2cache.contains(CacheEntitySimple1.class, 4010));
        assertFalse(l2cache.contains(CacheEntitySimple2.class, 4011));
        assertFalse(l2cache.contains(CacheEntitySimple2.class, 4012));

    }

    /**
     * <h4>testCache005 - Test Cache.evict(Class, key)</h4>
     * <p>
     * Test using evict(Class, key) to evict a particular entity from the l2
     * cache with a number of combinations of entities.
     */
    @Test
    public void jpa_jpa20_cache_testCache005() throws Exception {

        Cache l2cache = em_CACHE_JEE_DCTRUE_DAC.getEntityManagerFactory().getCache();

        // 1. use evict(Class, key) to evict an entity that should not be in the l2 cache. Should return successfully.
        // Verify the entity is NOT in the l2 cache
        assertFalse(l2cache.contains(CacheEntitySimple1.class, 9999));
        l2cache.evict(CacheEntitySimple1.class, 9999);
        System.out.println("evict worked when (CacheEntitySimple1, 9999) is not in the l2 cache");

        // 2. use evict(Class, key) with interleaving finds and evicts
        CacheEntitySimple1 newEntitySimple1_test2 = new CacheEntitySimple1();

        newEntitySimple1_test2.setId(5000);
        newEntitySimple1_test2.setIntVal(5000);
        newEntitySimple1_test2.setStrVal("CacheEntitySimple1  5000");

        tx.begin();
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1_test2);
        tx.commit();

        assertTrue(l2cache.contains(CacheEntitySimple1.class, 5000));
        l2cache.evict(CacheEntitySimple1.class, 5000);
        assertFalse(l2cache.contains(CacheEntitySimple1.class, 5000));

        em_CACHE_JEE_DCTRUE_DAC.clear();

        CacheEntitySimple1 findEntity = em_CACHE_JEE_DCTRUE_DAC.find(CacheEntitySimple1.class, 5000);

        assertNotNull(findEntity);

        assertTrue(l2cache.contains(CacheEntitySimple1.class, 5000));

        l2cache.evict(CacheEntitySimple1.class, 5000);

        assertFalse(l2cache.contains(CacheEntitySimple1.class, 5000));

        // 3. evict(Class, key) method should not fail if used to evict an entity again after it is evicted from the cache.
        em_CACHE_JEE_DCTRUE_DAC.clear();

        findEntity = em_CACHE_JEE_DCTRUE_DAC.find(CacheEntitySimple1.class, 5000);

        assertNotNull(findEntity);

        assertTrue(l2cache.contains(CacheEntitySimple1.class, 5000));

        l2cache.evict(CacheEntitySimple1.class, 5000);

        assertFalse(l2cache.contains(CacheEntitySimple1.class, 5000));

        l2cache.evict(CacheEntitySimple1.class, 5000);

        assertFalse(l2cache.contains(CacheEntitySimple1.class, 5000));

        // 4. put in two entities of the same class and two entities of different class.  Use same keys for the two
        //    different classes. Then evict one entity and check that the evicted entity is gone, but the other three
        //    entities are still there.  Then evict the second entity of a different class and different key and check that
        //    the other two entities are still there
        CacheEntitySimple1 newEntitySimple1_test4a = new CacheEntitySimple1();
        CacheEntitySimple1 newEntitySimple1_test4b = new CacheEntitySimple1();
        CacheEntitySimple2 newEntitySimple2_test4a = new CacheEntitySimple2();
        CacheEntitySimple2 newEntitySimple2_test4b = new CacheEntitySimple2();

        newEntitySimple1_test4a.setId(5001);
        newEntitySimple1_test4a.setIntVal(5001);
        newEntitySimple1_test4a.setStrVal("CacheEntitySimple1  5001");

        newEntitySimple1_test4b.setId(5002);
        newEntitySimple1_test4b.setIntVal(5002);
        newEntitySimple1_test4b.setStrVal("CacheEntitySimple1  5002");

        newEntitySimple2_test4a.setId(5001);
        newEntitySimple2_test4a.setIntVal(5001);
        newEntitySimple2_test4a.setStrVal("CacheEntitySimple2 5001");

        newEntitySimple2_test4b.setId(5002);
        newEntitySimple2_test4b.setIntVal(5002);
        newEntitySimple2_test4b.setStrVal("CacheEntitySimple2 5002");

        tx.begin();
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1_test4a);
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1_test4b);
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple2_test4a);
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple2_test4b);
        tx.commit();

        assertTrue(l2cache.contains(CacheEntitySimple1.class, 5001));
        assertTrue(l2cache.contains(CacheEntitySimple1.class, 5002));
        assertTrue(l2cache.contains(CacheEntitySimple2.class, 5001));
        assertTrue(l2cache.contains(CacheEntitySimple2.class, 5002));

        // Evict CacheEntitySimple1, 5001 and verify the others are still in the l2 cache
        l2cache.evict(CacheEntitySimple1.class, 5001);
        assertFalse(l2cache.contains(CacheEntitySimple1.class, 5001));
        assertTrue(l2cache.contains(CacheEntitySimple1.class, 5002));
        assertTrue(l2cache.contains(CacheEntitySimple2.class, 5001));
        assertTrue(l2cache.contains(CacheEntitySimple2.class, 5002));

        // Evict CacheEntitySimple2, 5002 and verify the rest  are still in the l2 cache
        l2cache.evict(CacheEntitySimple2.class, 5002);
        assertFalse(l2cache.contains(CacheEntitySimple1.class, 5001));
        assertTrue(l2cache.contains(CacheEntitySimple1.class, 5002));
        assertTrue(l2cache.contains(CacheEntitySimple2.class, 5001));
        assertFalse(l2cache.contains(CacheEntitySimple2.class, 5002));
    }

    /**
     * <h4>testCache006 - Test Cache.evict(Class)</h4>
     * <p>
     * Test using evict(Class) to evict all entities of a particular class from
     * the l2 cache.
     */
    @Test
    @Ignore
    // EclipseLink uses a 'Reference cache' and 'Weak cache' for the l2 cache.
    // Only the Ref cache size is configurable.  The Weak cache size will grow
    // as needed and gets cleaned up by the JVM.  Therefore, can't count on l2
    // cache size behavior for EL.
    public void jpa_jpa20_cache_testCache006() throws Exception {

        Cache l2cache = em_CACHE_JEE_DCTRUE_DAC.getEntityManagerFactory().getCache();

        // 1. Test evict(Class) if no entities of the class are in the cache
        l2cache.evict(CacheEntitySimple1.class);

        System.out.println("evict worked when (CacheEntitySimple1) is not in the l2 cache");

        // 2. Test evict(Class) if one entity of the class is in the cache
        CacheEntitySimple1 newEntitySimple1 = new CacheEntitySimple1();

        newEntitySimple1.setId(6000);
        newEntitySimple1.setIntVal(6000);
        newEntitySimple1.setStrVal("CacheEntitySimple1  6000");

        tx.begin();
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1);
        tx.commit();

        // Verify the entity is in the l2 cache then evict the class
        assertTrue(l2cache.contains(CacheEntitySimple1.class, 6000));
        l2cache.evict(CacheEntitySimple1.class);
        assertFalse(l2cache.contains(CacheEntitySimple1.class, 6000));

        // 3. Test evict(Class) if a few entities are in the cache
        int startEntryId3 = 6001;
        int numberEntries3 = 5;

        tx.begin();

        for (int i = startEntryId3; i < startEntryId3 + numberEntries3; i++) {
            CacheEntitySimple1 newEntitySimple1_test3 = new CacheEntitySimple1();
            newEntitySimple1_test3.setId(i);
            newEntitySimple1_test3.setIntVal(i);
            newEntitySimple1_test3.setStrVal("CacheEntitySimple1  " + i);
            em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1_test3);
        }

        tx.commit();

        for (int i = startEntryId3; i < startEntryId3 + numberEntries3; i++) {
            assertTrue(l2cache.contains(CacheEntitySimple1.class, i));
        }

        l2cache.evict(CacheEntitySimple1.class);

        for (int i = startEntryId3; i < startEntryId3 + numberEntries3; i++) {
            assertFalse(l2cache.contains(CacheEntitySimple1.class, i));
        }

        // 4. Test test evict(Class) if the cache is filled to the max with the class
        int startEntryId4 = 6500;
        int numberEntries4 = cacheSize;

        tx.begin();
        for (int i = startEntryId4; i < startEntryId4 + numberEntries4; i++) {
            CacheEntitySimple1 newEntitySimple1_test4 = new CacheEntitySimple1();
            newEntitySimple1_test4.setId(i);
            newEntitySimple1_test4.setIntVal(i);
            newEntitySimple1_test4.setStrVal("CacheEntitySimple1  " + i);
            em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1_test4);
        }
        tx.commit();

        // Verify the entities are in the l2 cache then evict the class

        for (int i = startEntryId4; i < startEntryId4 + numberEntries4; i++) {
            assertTrue(l2cache.contains(CacheEntitySimple1.class, i));
        }

        l2cache.evict(CacheEntitySimple1.class);

        for (int i = startEntryId4; i < startEntryId4 + numberEntries4; i++) {
            assertFalse(l2cache.contains(CacheEntitySimple1.class, i));
        }

        // 5. Test test evict(Class) if the number of cache entries exceed the max number
        int startEntryId5 = 6600;
        int numberEntries5 = cacheSize + 1;

        tx.begin();
        for (int i = startEntryId5; i < startEntryId5 + numberEntries5; i++) {
            CacheEntitySimple1 newEntitySimple1_test5 = new CacheEntitySimple1();
            newEntitySimple1_test5.setId(i);
            newEntitySimple1_test5.setIntVal(i);
            newEntitySimple1_test5.setStrVal("CacheEntitySimple1  " + i);
            em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1_test5);
        }
        tx.commit();

        // "Verify that only CACHE_SIZE entities are in the l2 cache then evict the class and verify that all are gone
        int found = 0;
        for (int i = startEntryId5; i < startEntryId5 + numberEntries5; i++) {
            if (l2cache.contains(CacheEntitySimple1.class, i)) {
                System.out.println("L2 cache does contain (CacheEntitySimple1, " + i + ")");
                ++found;
            } else {
                System.out.println("L2 cache does NOT contain (CacheEntitySimple1, " + i + ")");
            }
        }

        assertEquals(cacheSize, found);

        l2cache.evict(CacheEntitySimple1.class);

        found = 0;
        for (int i = startEntryId5; i < startEntryId5 + numberEntries5; i++) {
            assertFalse(l2cache.contains(CacheEntitySimple1.class, i));
        }
    }

    /**
     * <h4>testCache007 - Test Cache.evictAll()</h4>
     * <p>
     * Test using evictAll() to evict all entities from the l2 cache.
     */
    @Test
    @Ignore
    // EclipseLink uses a 'Reference cache' and 'Weak cache' for the l2 cache.
    // Only the Ref cache size is configurable.  The Weak cache size will grow
    // as needed and gets cleaned up by the JVM.  Therefore, can't count on l2
    // cache size behavior for EL.
    public void jpa_jpa20_cache_testCache007() throws Exception {

        Cache l2cache = em_CACHE_JEE_DCTRUE_DAC.getEntityManagerFactory().getCache();

        //  1. Test evictAll() if no entities are in the cache
        l2cache.evictAll();

        System.out.println("evictAll worked when there are no entities in the l2 cache");

        //  2. Test evictAll() if one entity is in the cache
        int startEntryId = 7200;
        CacheEntitySimple1 newEntitySimple1 = new CacheEntitySimple1();

        newEntitySimple1.setId(startEntryId);
        newEntitySimple1.setIntVal(startEntryId);
        newEntitySimple1.setStrVal("CacheEntitySimple1  " + startEntryId);

        tx.begin();
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1);
        tx.commit();

        // Verify the entity is in the l2 cache then evict all entities
        assertTrue(l2cache.contains(CacheEntitySimple1.class, startEntryId));
        l2cache.evictAll();
        assertFalse(l2cache.contains(CacheEntitySimple1.class, startEntryId));

        //  3. Test evictAll() if many entities of different class types are in the cache
        startEntryId = 7300;
        int numberEntries = 5;

        tx.begin();
        for (int i = startEntryId; i < startEntryId + numberEntries; i++) {
            newEntitySimple1 = new CacheEntitySimple1();
            newEntitySimple1.setId(i);
            newEntitySimple1.setIntVal(i);
            newEntitySimple1.setStrVal("CacheEntitySimple1  " + i);
            em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1);

            CacheEntitySimple2 newEntitySimple2 = new CacheEntitySimple2();
            newEntitySimple2.setId(i);
            newEntitySimple2.setIntVal(i);
            newEntitySimple2.setStrVal("CacheEntitySimple2 " + i);
            em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple2);
        }
        tx.commit();

        // Verify the entities are in the l2 cache then evict all the entities
        for (int i = startEntryId; i < startEntryId + numberEntries; i++) {
            assertTrue(l2cache.contains(CacheEntitySimple1.class, i));
            assertTrue(l2cache.contains(CacheEntitySimple2.class, i));
        }

        l2cache.evictAll();

        for (int i = startEntryId; i < startEntryId + numberEntries; i++) {
            assertFalse(l2cache.contains(CacheEntitySimple1.class, i));
            assertFalse(l2cache.contains(CacheEntitySimple2.class, i));
        }

        //  4. Test evictAll() if the cache is filled to the max
        startEntryId = 7400;
        numberEntries = cacheSize;
        tx.begin();
        for (int i = startEntryId; i < startEntryId + numberEntries; i++) {
            newEntitySimple1 = new CacheEntitySimple1();
            newEntitySimple1.setId(i);
            newEntitySimple1.setIntVal(i);
            newEntitySimple1.setStrVal("CacheEntitySimple1  " + i);
            em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1);
        }
        tx.commit();

        // Verify the entities are in the l2 cache then evict the class
        for (int i = startEntryId; i < startEntryId + numberEntries; i++) {
            assertTrue(l2cache.contains(CacheEntitySimple1.class, i));
        }

        l2cache.evictAll();

        for (int i = startEntryId; i < startEntryId + numberEntries; i++) {
            assertFalse(l2cache.contains(CacheEntitySimple1.class, i));
        }

        //  5. Test evictAll() if the number of cache entries exceed the max number allowed
        startEntryId = 7500;
        numberEntries = cacheSize + 5;

        tx.begin();
        for (int i = startEntryId; i < startEntryId + numberEntries; i++) {
            newEntitySimple1 = new CacheEntitySimple1();
            newEntitySimple1.setId(i);
            newEntitySimple1.setIntVal(i);
            newEntitySimple1.setStrVal("CacheEntitySimple1  " + i);
            em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1);
        }
        tx.commit();

        // Verify that only `cacheSize` entities are in the l2 cache then evict the class and verify that all are gone
        int found = 0;
        for (int i = startEntryId; i < startEntryId + numberEntries; i++) {
            if (l2cache.contains(CacheEntitySimple1.class, i)) {
                System.out.println("L2 cache does contain (CacheEntitySimple1, " + i + ")");
                ++found;
            } else {
                System.out.println("L2 cache does NOT contain (CacheEntitySimple1, " + i + ")");
            }
        }

        assertEquals(cacheSize, found);

        l2cache.evictAll();

        for (int i = startEntryId; i < startEntryId + numberEntries; i++) {
            assertFalse(l2cache.contains(CacheEntitySimple1.class, i));
        }
    }

    /**
     * <h4>testCache008 - Test creating, finding, deleting entities with relationships</h4>
     * <p>
     * Test various entity manager operations on an entity that has relationships
     * with other entities are added, found and removed from the cache.
     */
    @Test
    public void jpa_jpa20_cache_testCache008() throws Exception {

        Cache l2cache = em_CACHE_JEE_DCTRUE_DAC.getEntityManagerFactory().getCache();

        int startEntryId = 8000;

        final int Simple1ArrSize = 10;

        //  1. Create an entity and entities that it has a relationship to and check that
        //     contains() returns true for all the objects that were created
        tx.begin();

        CacheEntitySimple2 newEntitySimple2 = new CacheEntitySimple2();
        newEntitySimple2.setId(startEntryId);
        newEntitySimple2.setIntVal(startEntryId);
        newEntitySimple2.setStrVal("CacheEntitySimple2 " + startEntryId);

        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple2);

        ArrayList<CacheEntitySimple1> entitySimple1Collection = new ArrayList<CacheEntitySimple1>(Simple1ArrSize);

        for (int index = 0; index < Simple1ArrSize; index++) {
            CacheEntitySimple1 newEntitySimple1 = new CacheEntitySimple1();
            newEntitySimple1.setId(startEntryId + index);
            newEntitySimple1.setIntVal(startEntryId + index);
            newEntitySimple1.setStrVal("CacheEntitySimple1 " + (startEntryId + index));

            em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1);
            entitySimple1Collection.add(newEntitySimple1);
        }

        // Create CacheEntityCollection
        CacheEntityCollection newEntityCollection = new CacheEntityCollection();
        newEntityCollection.setId(startEntryId);
        newEntityCollection.setIntVal(startEntryId);
        newEntityCollection.setStrVal("CacheEntityCollection " + startEntryId);
        newEntityCollection.setEntitySimple2(newEntitySimple2);
        newEntityCollection.setEntitySimple1(entitySimple1Collection);
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntityCollection);

        tx.commit();

        // Check that all of the entities are in the cache
        assertTrue(l2cache.contains(CacheEntitySimple2.class, startEntryId));
        for (int index = 0; index < Simple1ArrSize; index++) {
            assertTrue(l2cache.contains(CacheEntitySimple1.class, (startEntryId + index)));
        }
        assertTrue(l2cache.contains(CacheEntityCollection.class, startEntryId));

        //  2. Clear the persistence context and check that all the entities are still in the cache.
        em_CACHE_JEE_DCTRUE_DAC.clear();

        assertTrue(l2cache.contains(CacheEntitySimple2.class, startEntryId));
        for (int index = 0; index < Simple1ArrSize; index++) {
            assertTrue(l2cache.contains(CacheEntitySimple1.class, (startEntryId + index)));
        }
        assertTrue(l2cache.contains(CacheEntityCollection.class, startEntryId));

        //  3. Evict the main entity and check that the related entities are in the l2 cache.
        l2cache.evict(CacheEntityCollection.class, startEntryId);

        assertTrue(l2cache.contains(CacheEntitySimple2.class, startEntryId));
        for (int index = 0; index < Simple1ArrSize; index++) {
            assertTrue(l2cache.contains(CacheEntitySimple1.class, (startEntryId + index)));
        }
        assertFalse(l2cache.contains(CacheEntityCollection.class, startEntryId));

        //  4. Evict the relationship objects from the l2 cache and check that all entries are gone.
        l2cache.evict(CacheEntitySimple2.class, startEntryId);
        for (int index = 0; index < Simple1ArrSize; index++) {
            l2cache.evict(CacheEntitySimple1.class, (startEntryId + index));
        }

        assertFalse(l2cache.contains(CacheEntitySimple2.class, startEntryId));
        for (int index = 0; index < Simple1ArrSize; index++) {
            assertFalse(l2cache.contains(CacheEntitySimple1.class, index));
        }
        assertFalse(l2cache.contains(CacheEntityCollection.class, startEntryId));

        //  5. Find the collection entity and check that the l2 cache contains the entity and the entities it has relationships with.
        em_CACHE_JEE_DCTRUE_DAC.clear();

        CacheEntityCollection findEntity = em_CACHE_JEE_DCTRUE_DAC.find(CacheEntityCollection.class, startEntryId);

        assertNotNull(findEntity);
        Collection<CacheEntitySimple1> findEntitySimple1Collection = findEntity.getEntitySimple1();

        assertTrue(l2cache.contains(CacheEntitySimple2.class, startEntryId));

        // Note that this call to size() forces the collection to be loaded.  Even when
        // fetchType LAZY is specified, it is only a hint to the JPA provider.  OpenJPA
        // seems to load despite being LAZY, and EclipseLink does.
        System.out.println("Collection size: " + findEntitySimple1Collection.size());

        for (int index = 0; index < Simple1ArrSize; index++) {
            assertTrue("Could not find entity " + (startEntryId + index) + " in the cache.",
                       l2cache.contains(CacheEntitySimple1.class, (startEntryId + index)));
        }
        assertTrue(l2cache.contains(CacheEntityCollection.class, startEntryId));

        //  6. Delete the collection entity and the related objects and check that there are no entities in the l2 cache.
        em_CACHE_JEE_DCTRUE_DAC.clear();

        tx.begin();

        CacheEntityCollection findEntityAgain = em_CACHE_JEE_DCTRUE_DAC.find(CacheEntityCollection.class, startEntryId);

        em_CACHE_JEE_DCTRUE_DAC.remove(findEntityAgain);

        tx.commit();

        assertFalse(l2cache.contains(CacheEntitySimple2.class, startEntryId));
        for (int index = 0; index < Simple1ArrSize; index++) {
            assertFalse(l2cache.contains(CacheEntitySimple1.class, (startEntryId + index)));
        }
        assertFalse(l2cache.contains(CacheEntityCollection.class, startEntryId));

    }

    /**
     * <h4>testCache009 - Test detaching and merging entities</h4>
     * <p>Detach/merge entities via em.detach, em.merge
     */
    @Test
    public void jpa_jpa20_cache_testCache009() throws Exception {

        int testEntryId = 9000;
        Cache l2cache = em_CACHE_JEE_DCTRUE_DAC.getEntityManagerFactory().getCache();

        //  1. Test em.detach() does not remove the entity from the L2 cache
        CacheEntitySimple1 newEntitySimple1 = new CacheEntitySimple1();
        CacheEntitySimple2 newEntitySimple2 = new CacheEntitySimple2();

        newEntitySimple1.setId(testEntryId);
        newEntitySimple1.setIntVal(testEntryId);
        newEntitySimple1.setStrVal("CacheEntitySimple1  " + testEntryId);

        newEntitySimple2.setId(testEntryId);
        newEntitySimple2.setIntVal(testEntryId);
        newEntitySimple2.setStrVal("CacheEntitySimple2 " + testEntryId);

        tx.begin();
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1);
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple2);
        tx.commit();

        // Verify the entities are in the l2 cache then detach one of the entities
        assertTrue(l2cache.contains(CacheEntitySimple1.class, testEntryId));
        assertTrue(l2cache.contains(CacheEntitySimple2.class, testEntryId));

        tx.begin();

        em_CACHE_JEE_DCTRUE_DAC.detach(newEntitySimple1);
        tx.commit();

        // Verify the entities are still in the l2 cache
        assertTrue(l2cache.contains(CacheEntitySimple1.class, testEntryId));
        assertTrue(l2cache.contains(CacheEntitySimple2.class, testEntryId));

        //  2. Test em.merge() does not remove the entity from the L2 cache
        tx.begin();
        em_CACHE_JEE_DCTRUE_DAC.merge(newEntitySimple1);
        tx.commit();

        // Verify the entities are in the l2 cache
        assertTrue(l2cache.contains(CacheEntitySimple1.class, testEntryId));
        assertTrue(l2cache.contains(CacheEntitySimple2.class, testEntryId));
    }

    /**
     * <h4>testCache010 - Test clearing entities from persistence context</h4>
     * <p>Clearing entities from the persistence context using em.clear
     * should not clear out the l2 cache.
     */
    @Test
    public void jpa_jpa20_cache_testCache010() throws Exception {

        int testEntryId = 10000;
        Cache l2cache = em_CACHE_JEE_DCTRUE_DAC.getEntityManagerFactory().getCache();

        //  1. Test em.clear() does not clear the L2 cache
        CacheEntitySimple1 newEntitySimple1 = new CacheEntitySimple1();
        CacheEntitySimple2 newEntitySimple2 = new CacheEntitySimple2();

        newEntitySimple1.setId(testEntryId);
        newEntitySimple1.setIntVal(testEntryId);
        newEntitySimple1.setStrVal("CacheEntitySimple1  " + testEntryId);

        newEntitySimple2.setId(testEntryId);
        newEntitySimple2.setIntVal(testEntryId);
        newEntitySimple2.setStrVal("CacheEntitySimple2 " + testEntryId);

        tx.begin();
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1);
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple2);
        tx.commit();

        // Verify the entities are in the l2 cache then clear the entity manager
        assertTrue(l2cache.contains(CacheEntitySimple1.class, testEntryId));
        assertTrue(l2cache.contains(CacheEntitySimple2.class, testEntryId));

        em_CACHE_JEE_DCTRUE_DAC.clear();

        // Verify the entities are still in the l2 cache
        assertTrue(l2cache.contains(CacheEntitySimple1.class, testEntryId));
        assertTrue(l2cache.contains(CacheEntitySimple2.class, testEntryId));
    }

    /**
     * <h4>testCache011 - Test deleting an entity</h4>
     * <p>Test that the entity is removed from the L2 cache when it is deleted.
     */
    @Test
    public void jpa_jpa20_cache_testCache011() throws Exception {

        int testEntryId = 11000;
        Cache l2cache = em_CACHE_JEE_DCTRUE_DAC.getEntityManagerFactory().getCache();

        //  1. Test em.delete() does remove entity from the L2 cache
        CacheEntitySimple1 newEntitySimple1 = new CacheEntitySimple1();
        CacheEntitySimple2 newEntitySimple2 = new CacheEntitySimple2();

        newEntitySimple1.setId(testEntryId);
        newEntitySimple1.setIntVal(testEntryId);
        newEntitySimple1.setStrVal("CacheEntitySimple1  " + testEntryId);

        newEntitySimple2.setId(testEntryId);
        newEntitySimple2.setIntVal(testEntryId);
        newEntitySimple2.setStrVal("CacheEntitySimple2 " + testEntryId);

        tx.begin();
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1);
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple2);
        tx.commit();

        // Verify the entities are in the l2 cache then clear the entity manager
        assertTrue(l2cache.contains(CacheEntitySimple1.class, testEntryId));
        assertTrue(l2cache.contains(CacheEntitySimple2.class, testEntryId));

        tx.begin();

        CacheEntitySimple1 findEntityAgain = em_CACHE_JEE_DCTRUE_DAC.find(CacheEntitySimple1.class, testEntryId);

        em_CACHE_JEE_DCTRUE_DAC.remove(findEntityAgain);

        tx.commit();

        // Verify the deleted entity is not in the l2 cache
        assertFalse(l2cache.contains(CacheEntitySimple1.class, testEntryId));
        assertTrue(l2cache.contains(CacheEntitySimple2.class, testEntryId));
    }

    /**
     * <h4>testCache012 - Test shared cache mode ALL</h4>
     * <p>Test that all entities are in the L2 cache no matter what Cachable setting is used.
     */
    @Test
    public void jpa_jpa20_cache_testCache012() throws Exception {

        int testEntryId = 12000;
        Cache l2cache = em_CACHE_JEE_DCTRUE_DAC.getEntityManagerFactory().getCache();

        //  1. Test when shared-cache-mode is ALL, all entities are in the L2 cache
        CacheEntitySimple1 newEntitySimple1 = new CacheEntitySimple1();
        CacheEntityCacheable newEntityCacheable = new CacheEntityCacheable();
        CacheEntityCacheableNot newEntityCacheableNot = new CacheEntityCacheableNot();

        newEntitySimple1.setId(testEntryId);
        newEntitySimple1.setIntVal(testEntryId);
        newEntitySimple1.setStrVal("CacheEntitySimple1  " + testEntryId);

        newEntityCacheable.setId(testEntryId);
        newEntityCacheable.setIntVal(testEntryId);
        newEntityCacheable.setStrVal("CacheEntityCacheable " + testEntryId);

        newEntityCacheableNot.setId(testEntryId);
        newEntityCacheableNot.setIntVal(testEntryId);
        newEntityCacheableNot.setStrVal("CacheEntityCacheableNot " + testEntryId);

        tx.begin();
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntitySimple1);
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntityCacheable);
        em_CACHE_JEE_DCTRUE_DAC.persist(newEntityCacheableNot);

        tx.commit();

        // Verify the entities are in the l2 cache
        assertTrue(l2cache.contains(CacheEntitySimple1.class, testEntryId));
        assertTrue(l2cache.contains(CacheEntityCacheable.class, testEntryId));
        assertTrue(l2cache.contains(CacheEntityCacheableNot.class, testEntryId));
    }

    /**
     * <h4>testCache013 - Test shared cache mode NONE</h4>
     * <p>Test that no entities are in the L2 cache no matter what Cachable setting is used.
     */
    @Test
    public void jpa_jpa20_cache_testCache013() throws Exception {

        int testEntryId = 13000;
        Cache l2cache = em_CACHE_JEE_DCFALSE.getEntityManagerFactory().getCache();

        //  1. Test when shared-cache-mode is NONE, no entities are in the L2 cache
        CacheEntitySimple1 newEntitySimple1 = new CacheEntitySimple1();
        CacheEntityCacheable newEntityCacheable = new CacheEntityCacheable();
        CacheEntityCacheableNot newEntityCacheableNot = new CacheEntityCacheableNot();

        newEntitySimple1.setId(testEntryId);
        newEntitySimple1.setIntVal(testEntryId);
        newEntitySimple1.setStrVal("CacheEntitySimple1  " + testEntryId);

        newEntityCacheable.setId(testEntryId);
        newEntityCacheable.setIntVal(testEntryId);
        newEntityCacheable.setStrVal("CacheEntityCacheable " + testEntryId);

        newEntityCacheableNot.setId(testEntryId);
        newEntityCacheableNot.setIntVal(testEntryId);
        newEntityCacheableNot.setStrVal("CacheEntityCacheableNot " + testEntryId);

        tx.begin();
        em_CACHE_JEE_DCFALSE.persist(newEntitySimple1);
        em_CACHE_JEE_DCFALSE.persist(newEntityCacheable);
        em_CACHE_JEE_DCFALSE.persist(newEntityCacheableNot);

        tx.commit();

        // Verify the entities are NOT in the l2 cache
        assertFalse(l2cache.contains(CacheEntitySimple1.class, testEntryId));
        assertFalse(l2cache.contains(CacheEntityCacheable.class, testEntryId));
        assertFalse(l2cache.contains(CacheEntityCacheableNot.class, testEntryId));
    }

    /**
     * <h4>testCache014 - Test shared cache mode ENABLE_SELECTIVE</h4>
     * <p>Test that only entities marked Cachable(true) are in the L2 cache.
     */
    @Test
    public void jpa_jpa20_cache_testCache014() throws Exception {

        int testEntryId = 14000;
        Cache l2cache = em_CACHE_JEE_DCTRUE_SCM_ENASEL.getEntityManagerFactory().getCache();

        //  1. Test when shared-cache-mode is ENABLE_SELECTIVE, only entries marked Cachable(true) are in the L2 cache
        CacheEntitySimple1 newEntitySimple1 = new CacheEntitySimple1();
        CacheEntityCacheable newEntityCacheable = new CacheEntityCacheable();
        CacheEntityCacheableNot newEntityCacheableNot = new CacheEntityCacheableNot();

        newEntitySimple1.setId(testEntryId);
        newEntitySimple1.setIntVal(testEntryId);
        newEntitySimple1.setStrVal("CacheEntitySimple1  " + testEntryId);

        newEntityCacheable.setId(testEntryId);
        newEntityCacheable.setIntVal(testEntryId);
        newEntityCacheable.setStrVal("CacheEntityCacheable " + testEntryId);

        newEntityCacheableNot.setId(testEntryId);
        newEntityCacheableNot.setIntVal(testEntryId);
        newEntityCacheableNot.setStrVal("CacheEntityCacheableNot " + testEntryId);

        tx.begin();
        em_CACHE_JEE_DCTRUE_SCM_ENASEL.persist(newEntitySimple1);
        em_CACHE_JEE_DCTRUE_SCM_ENASEL.persist(newEntityCacheable);
        em_CACHE_JEE_DCTRUE_SCM_ENASEL.persist(newEntityCacheableNot);

        tx.commit();

        // Verify that only entities marked Cacheable(true) are in the l2 cache
        assertFalse(l2cache.contains(CacheEntitySimple1.class, testEntryId));
        assertTrue(l2cache.contains(CacheEntityCacheable.class, testEntryId));
        assertFalse(l2cache.contains(CacheEntityCacheableNot.class, testEntryId));
    }

    /**
     * <h4>testCache015 - Test shared cache mode DISABLE_SELECTIVE</h4>
     * <p>Test that entities marked Cachable(false) are not in the L2 cache.
     */
    @Test
    public void jpa_jpa20_cache_testCache015() throws Exception {

        int testEntryId = 15000;
        Cache l2cache = em_CACHE_JEE_DCTRUE_SCM_DISSEL.getEntityManagerFactory().getCache();

        //  1. Test when shared-cache-mode is DISABLE_SELECTIVE, entries marked Cachable(false) are not in the L2 cache
        CacheEntitySimple1 newEntitySimple1 = new CacheEntitySimple1();
        CacheEntityCacheable newEntityCacheable = new CacheEntityCacheable();
        CacheEntityCacheableNot newEntityCacheableNot = new CacheEntityCacheableNot();

        newEntitySimple1.setId(testEntryId);
        newEntitySimple1.setIntVal(testEntryId);
        newEntitySimple1.setStrVal("CacheEntitySimple1  " + testEntryId);

        newEntityCacheable.setId(testEntryId);
        newEntityCacheable.setIntVal(testEntryId);
        newEntityCacheable.setStrVal("CacheEntityCacheable " + testEntryId);

        newEntityCacheableNot.setId(testEntryId);
        newEntityCacheableNot.setIntVal(testEntryId);
        newEntityCacheableNot.setStrVal("CacheEntityCacheableNot " + testEntryId);

        tx.begin();
        em_CACHE_JEE_DCTRUE_SCM_DISSEL.persist(newEntitySimple1);
        em_CACHE_JEE_DCTRUE_SCM_DISSEL.persist(newEntityCacheable);
        em_CACHE_JEE_DCTRUE_SCM_DISSEL.persist(newEntityCacheableNot);

        tx.commit();

        // Verify the entities marked Cacheable(false) are not in the l2 cache
        assertTrue(l2cache.contains(CacheEntitySimple1.class, testEntryId));
        assertTrue(l2cache.contains(CacheEntityCacheable.class, testEntryId));
        assertFalse(l2cache.contains(CacheEntityCacheableNot.class, testEntryId));
    }

    /**
     * <h4>testCache016 - Test CacheRetrieveMode and CacheStoreMore settings</h4>
     * <p>
     * Test CacheRetrieveMode and CacheStoreMode settings get/update
     * entities for the cache or for the database depending on the
     * setting.
     * <p>For this test, shared-cache-mode is set to ALL.
     */
    @Test
    public void jpa_jpa20_cache_testCache016() throws Exception {

        int testEntryId = 16000;
        int testEntryValCache = 1;
        int testEntryValDb = 2;
        EntityManager em = emf_CACHE_JEE_DCTRUE_COPY.createEntityManager();
        try {
            Cache l2cache = emf_CACHE_JEE_DCTRUE_COPY.getCache();

            //  1. Use CacheStoreMode.USE to put the  new entries in the L2 cache
            em.setProperty("javax.persistence.cache.storeMode", CacheStoreMode.USE);

            CacheEntitySimple1 newEntitySimple1 = new CacheEntitySimple1();
            CacheEntityCacheable newEntityCacheable = new CacheEntityCacheable();
            CacheEntityCacheableNot newEntityCacheableNot = new CacheEntityCacheableNot();

            newEntitySimple1.setId(testEntryId);
            newEntitySimple1.setIntVal(testEntryValCache);
            newEntitySimple1.setStrVal("CacheEntitySimple1  " + testEntryValCache);

            newEntityCacheable.setId(testEntryId);
            newEntityCacheable.setIntVal(testEntryValCache);
            newEntityCacheable.setStrVal("CacheEntityCacheable " + testEntryValCache);

            newEntityCacheableNot.setId(testEntryId);
            newEntityCacheableNot.setIntVal(testEntryValCache);
            newEntityCacheableNot.setStrVal("CacheEntityCacheableNot " + testEntryValCache);

            tx.begin();
            em.joinTransaction();
            em.persist(newEntitySimple1);
            em.persist(newEntityCacheable);
            em.persist(newEntityCacheableNot);
            tx.commit();

            // Verify all the entities are in the l2 cache
            assertTrue(l2cache.contains(CacheEntitySimple1.class, testEntryId));
            assertTrue(l2cache.contains(CacheEntityCacheable.class, testEntryId));
            assertTrue(l2cache.contains(CacheEntityCacheableNot.class, testEntryId));

            // 2. Use CacheStoreMode.BYPASS and update the entities in the database but not the L2 cache
            em.setProperty("javax.persistence.cache.storeMode", CacheStoreMode.BYPASS);

            tx.begin();
            em.joinTransaction();
            newEntitySimple1.setIntVal(testEntryValDb);
            newEntitySimple1.setStrVal("CacheEntitySimple1  " + testEntryValDb);

            newEntityCacheable.setIntVal(testEntryValDb);
            newEntityCacheable.setStrVal("CacheEntityCacheable " + testEntryValDb);

            newEntityCacheableNot.setIntVal(testEntryValDb);
            newEntityCacheableNot.setStrVal("CacheEntityCacheableNot " + testEntryValDb);
            tx.commit();

            // 3. Test when CacheRetrieveMode.USE is used, entities are retrieved from the cache
            em.clear();
            em.setProperty("javax.persistence.cache.retrieveMode", CacheRetrieveMode.USE);

            tx.begin();
            em.joinTransaction();
            CacheEntitySimple1 findEntitySimple1Cache = em.find(CacheEntitySimple1.class, testEntryId);
            CacheEntityCacheable findEntityCacheableCache = em.find(CacheEntityCacheable.class, testEntryId);
            CacheEntityCacheableNot findEntityCacheableNotCache = em.find(CacheEntityCacheableNot.class, testEntryId);
            tx.commit();

            System.out.println("simple1 intval cache: " + findEntitySimple1Cache.getIntVal());

            assertEquals(findEntitySimple1Cache.getId(), testEntryId);
            assertEquals(findEntitySimple1Cache.getIntVal(), testEntryValCache);
            assertEquals(findEntitySimple1Cache.getStrVal(), "CacheEntitySimple1  " + testEntryValCache);

            assertEquals(findEntityCacheableCache.getId(), testEntryId);
            assertEquals(findEntityCacheableCache.getIntVal(), testEntryValCache);
            assertEquals(findEntityCacheableCache.getStrVal(), "CacheEntityCacheable " + testEntryValCache);

            assertEquals(findEntityCacheableNotCache.getId(), testEntryId);
            assertEquals(findEntityCacheableNotCache.getIntVal(), testEntryValCache);
            assertEquals(findEntityCacheableNotCache.getStrVal(), "CacheEntityCacheableNot " + testEntryValCache);

            // 4. Test when CacheRetrieveMode.BYPASS is used, entities are retrieved from the database
            em.clear();
            em.setProperty("javax.persistence.cache.retrieveMode", CacheRetrieveMode.BYPASS);

            tx.begin();
            em.joinTransaction();
            CacheEntitySimple1 findEntitySimple1Db = em.find(CacheEntitySimple1.class, testEntryId);
            CacheEntityCacheable findEntityCacheableDb = em.find(CacheEntityCacheable.class, testEntryId);
            CacheEntityCacheableNot findEntityCacheableNotDb = em.find(CacheEntityCacheableNot.class, testEntryId);
            tx.commit();

            System.out.println("simple1 intval db: " + findEntitySimple1Db.getIntVal());

            assertEquals(findEntitySimple1Db.getId(), testEntryId);
            assertEquals(findEntitySimple1Db.getIntVal(), testEntryValDb);
            assertEquals(findEntitySimple1Db.getStrVal(), "CacheEntitySimple1  " + testEntryValDb);

            assertEquals(findEntityCacheableDb.getId(), testEntryId);
            assertEquals(findEntityCacheableDb.getIntVal(), testEntryValDb);
            assertEquals(findEntityCacheableDb.getStrVal(), "CacheEntityCacheable " + testEntryValDb);

            assertEquals(findEntityCacheableNotDb.getId(), testEntryId);
            assertEquals(findEntityCacheableNotDb.getIntVal(), testEntryValDb);
            assertEquals(findEntityCacheableNotDb.getStrVal(), "CacheEntityCacheableNot " + testEntryValDb);

            // 5. Test when CacheRetrieveMode.USE is used, CacheStoreMode.REFRESH is used,
            //    entities are retrieved from the cache that has been refreshed
            em.clear();

            em.setProperty("javax.persistence.cache.retrieveMode", CacheRetrieveMode.USE);

            em.setProperty("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH);

            tx.begin();
            em.joinTransaction();
            CacheEntitySimple1 findEntitySimple1CacheUpdated = em.find(CacheEntitySimple1.class, testEntryId);
            CacheEntityCacheable findEntityCacheableCacheUpdated = em.find(CacheEntityCacheable.class, testEntryId);
            CacheEntityCacheableNot findEntityCacheableNotCacheUpdated = em.find(CacheEntityCacheableNot.class, testEntryId);
            tx.commit();

            System.out.println("simple1 intval cache: " + findEntitySimple1CacheUpdated.getIntVal());

            assertEquals(findEntitySimple1CacheUpdated.getId(), testEntryId);
            assertEquals(findEntitySimple1CacheUpdated.getIntVal(), testEntryValDb);
            assertEquals(findEntitySimple1CacheUpdated.getStrVal(), "CacheEntitySimple1  " + testEntryValDb);

            assertEquals(findEntityCacheableCacheUpdated.getId(), testEntryId);
            assertEquals(findEntityCacheableCacheUpdated.getIntVal(), testEntryValDb);
            assertEquals(findEntityCacheableCacheUpdated.getStrVal(), "CacheEntityCacheable " + testEntryValDb);

            assertEquals(findEntityCacheableNotCacheUpdated.getId(), testEntryId);
            assertEquals(findEntityCacheableNotCacheUpdated.getIntVal(), testEntryValDb);
            assertEquals(findEntityCacheableNotCacheUpdated.getStrVal(), "CacheEntityCacheableNot " + testEntryValDb);
        } finally {
            em.close();
        }
    }
}
