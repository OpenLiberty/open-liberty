/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.persistence.Cache;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.ws.jpa.cache.model.JPA20EMEntityA;
import com.ibm.ws.jpa.cache.model.JPA20EMEntityB;
import com.ibm.ws.jpa.cache.model.JPA20EMEntityC;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

/*
 * Skip for Oracle because they do not support TransactionIsolation=read-uncommitted
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/RefreshCacheTestServlet")
public class RefreshCacheTestServlet extends JPATestServlet {
    // Can switch the value of this constant to enable validation of @preUpdate
    // on exception paths if EclipseLink is ever updated to support this behavior.
    public static final boolean ECLIPSELINK_VALIDATE_PREUPDATE_ON_EXCEPTION = false;

    public final static MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    public final static ObjectName fatServerInfoMBeanObjectName;

    static {
        ObjectName on = null;
        try {
            on = new ObjectName("WebSphereFAT:name=ServerInfo");
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            fatServerInfoMBeanObjectName = on;
        }
    }

    @PersistenceContext(unitName = "CACHE_JEE_DC_TRUE_REFRESH_TEST")
    EntityManager em;

    @PersistenceContext(unitName = "CACHE_JEE_DC_FALSE_REFRESH_TEST")
    EntityManager em_COPY;

    @PersistenceUnit(unitName = "CACHE_JEE_DC_FALSE_REFRESH_TEST_COPY")
    EntityManagerFactory emf;

    @Resource
    ManagedExecutorService exec;

    @Resource
    UserTransaction tx;

    private int idMain = 5;
    private int version_OpenJPA = 1; // Keep track of the version of the main entity within OpenJPA tests
    private int version_EL = 1; // Keep track of the version of the main entity within EclipseLink tests

    @Override
    public void init() throws ServletException {
        try {
            populateDatabase();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    };

    protected Set<String> getInstalledFeatures() {
        HashSet<String> retVal = new HashSet<String>();

        try {
            Set<String> instFeatureSet = (Set<String>) mbeanServer.getAttribute(fatServerInfoMBeanObjectName, "InstalledFeatures");
            if (instFeatureSet != null) {
                retVal.addAll(instFeatureSet);
            }
        } catch (Throwable t) {
        }
        return retVal;
    }

    protected boolean isUsingJPA20Feature() {
        Set<String> instFeatureSet = getInstalledFeatures();
        return instFeatureSet.contains("jpa-2.0");
    }

    protected boolean isUsingJPA21Feature() {
        Set<String> instFeatureSet = getInstalledFeatures();
        return instFeatureSet.contains("jpa-2.1");
    }

    protected boolean isUsingJPA22Feature() {
        Set<String> instFeatureSet = getInstalledFeatures();
        return instFeatureSet.contains("jpa-2.2");
    }

    protected boolean isUsingJPA21ContainerFeature(boolean onlyContainerFeature) {
        Set<String> instFeatureSet = getInstalledFeatures();
        if (onlyContainerFeature && instFeatureSet.contains("jpa-2.1"))
            return false;
        return instFeatureSet.contains("jpaContainer-2.1");
    }

    protected boolean isUsingJPA22ContainerFeature(boolean onlyContainerFeature) {
        Set<String> instFeatureSet = getInstalledFeatures();
        if (onlyContainerFeature && instFeatureSet.contains("jpa-2.2"))
            return false;
        return instFeatureSet.contains("jpaContainer-2.2");
    }

    protected void populateDatabase() throws Exception {
        // Populate database
        tx.begin();
        em.clear();

        // JPA20EMEntityA
        JPA20EMEntityA entityA = new JPA20EMEntityA();
        entityA.setId(idMain);
        entityA.setStrData("Entity A Data");
        em.persist(entityA);

        // JPA20EMEntityB(id=1)
        JPA20EMEntityB entityB1 = new JPA20EMEntityB();
        entityB1.setId(1);
        entityB1.setStrData("Entity B1 Data");
        em.persist(entityB1);

        // JPA20EMEntityB(id=2)
        JPA20EMEntityB entityB2 = new JPA20EMEntityB();
        entityB2.setId(2);
        entityB2.setStrData("Entity B2 Data");
        em.persist(entityB2);

        // JPA20EMEntityC(id=1)
        JPA20EMEntityC entityC = new JPA20EMEntityC();
        entityC.setId(1);
        entityC.setStrData("Entity C Data");
        em.persist(entityC);

        // Set up relationships for the test.
        entityA.getEntityBList().add(entityB1);
        entityB1.getEntityAList().add(entityA);
        entityA.getEntityBList().add(entityB2);
        entityB2.getEntityAList().add(entityA);

        entityA.setEntityCLazy(entityC);
        entityC.setEntityALazy(entityA);

        tx.commit();
        em.clear();
    }

    /**
     * Method to reset Entity A data back to "Entity A Data" after every test method.
     */
    protected void resetEntity() throws Exception {
        tx.begin();
        em.createQuery("UPDATE JPA20EMEntityA e SET e.strData = \'Entity A Data\' WHERE e.id = " + idMain).executeUpdate();
        tx.commit();
    }

    @Test
    public void jpa_jpa20_cache_testRefreshC001() throws Exception {
        final String dbProduct = getDbProductName();
        if (dbProduct != null && dbProduct.toLowerCase().equals("oracle")) {
            System.out.println("Test is not supported on Oracle.");
            return;
        }

        final boolean isOpenJPA = isUsingJPA20Feature();

        if (isOpenJPA) {
            testRefreshC001_OpenJPA();
        } else {
            testRefreshC001_EL();
        }
    }

    @Test
    public void jpa_jpa20_cache_testRefreshC002() throws Exception {
        final String dbProduct = getDbProductName();
        if (dbProduct != null && dbProduct.toLowerCase().equals("oracle")) {
            System.out.println("Test is not supported on Oracle.");
            return;
        }

        final boolean isOpenJPA = isUsingJPA20Feature();

        if (isOpenJPA) {
            testRefreshC002_OpenJPA();
        } else {
            testRefreshC002_EL();
        }
    }

    @Test
    public void jpa_jpa20_cache_testRefreshC003() throws Exception {
        final String dbProduct = getDbProductName();
        if (dbProduct != null && dbProduct.toLowerCase().equals("oracle")) {
            System.out.println("Test is not supported on Oracle.");
            return;
        }

        final boolean isOpenJPA = isUsingJPA20Feature();

        if (isOpenJPA) {
            testRefreshC003_OpenJPA();
        } else {
            testRefreshC003_EL();
        }
    }

    @Test
    public void jpa_jpa20_cache_testRefreshC004() throws Exception {
        final String dbProduct = getDbProductName();
        if (dbProduct != null && dbProduct.toLowerCase().equals("oracle")) {
            System.out.println("Test is not supported on Oracle.");
            return;
        }

        final boolean isOpenJPA = isUsingJPA20Feature();

        if (isOpenJPA) {
            testRefreshC004_OpenJPA();
        } else {
            testRefreshC004_EL();
        }
    }

    /**
     * Verify that without any properties specified, that a call to em.refresh() will, by default, not update
     * the contents of the data cache. [Basic persistence provider sanity testing]
     */
    public void testRefreshC001_OpenJPA() throws Exception {
        try {
            tx.begin();
            em.clear();

            // Evict the contents of the cache (if the Cache object is obtainable)
            Cache cache = em.getEntityManagerFactory().getCache();
            if (cache != null) {
                cache.evictAll();
            }

            // Call em.find() for JPA20EMEntityA, specifying NO LockModeType
            JPA20EMEntityA entityA = em.find(JPA20EMEntityA.class, idMain, LockModeType.NONE);

            assertNotNull(entityA);
            assertTrue("Entity should have strData \"Entity A Data\" (is \" " + entityA.getStrData() + "\")",
                       "Entity A Data".equals(entityA.getStrData()));
            assertEquals("Find() should have returned JPA20EMEntityA(id=" + idMain + "), instead was (" + entityA.getId() + ")", idMain, entityA.getId());
            assertTrue("Entity should be attached to the persistence unit but is not.", em.contains(entityA));
            assertTrue("Entity should have LockModeType.NONE (is " + em.getLockMode(entityA) + ")",
                       (LockModeType.NONE == em.getLockMode(entityA) || em.getLockMode(entityA) == null));
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") has 2 JPA20EMEntityB's allocated to it " +
                       "(expecting 2, found " + entityA.getEntityBList().size() + ")",
                       entityA.getEntityBList().size() == 2);
            assertTrue("JPA20EMEntityA(id=" + idMain + ") should have a version of " + version_OpenJPA + " (is " + entityA.getVersion() + ").",
                       entityA.getVersion() == version_OpenJPA);

            // If a Cache object is available, make sure that the above entity is cached in it.
            if (cache == null) {
                System.out.println("No Cache object is available, so nothing to check against.");
            } else {
                assertTrue("JPAEntityA(id=" + idMain + ") is not in the cache, but should be at this point.", cache.contains(JPA20EMEntityA.class, idMain));
            }

            //Update Entity in database without altering entityA
            em_COPY.createQuery("UPDATE JPA20EMEntityA e SET e.strData = \'Modified by B\' WHERE e.id = " + idMain).executeUpdate();
            version_OpenJPA++; //Altering the entity increments the version

            //Make sure entityA was not altered by the query
            assertTrue("Entity should have strData \"Entity A Data\" (is \" " + entityA.getStrData() + "\")",
                       "Entity A Data".equals(entityA.getStrData()));
            em.refresh(entityA);
            // Verify the entity updated by em.refresh()
            assertTrue("Entity should have strData \"Modified by B\" (is \"" + entityA.getStrData() + "\")",
                       "Modified by B".equals(entityA.getStrData()));
            assertTrue("Entity should be attached to the persistence unit but is not.", em.contains(entityA));
            assertTrue("Entity should have LockModeType.NONE (is " + em.getLockMode(entityA) + ")",
                       (LockModeType.NONE == em.getLockMode(entityA) || em.getLockMode(entityA) == null));
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") has 2 JPA20EMEntityB's allocated to it " +
                       "(expecting 2, found " + entityA.getEntityBList().size() + ")",
                       entityA.getEntityBList().size() == 2);
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") now has a version of " + version_OpenJPA + " (is " + entityA.getVersion() + ").",
                       entityA.getVersion() == version_OpenJPA);

            // If a Cache object is available, make sure that the above entity is cached in it.
            if (cache == null) {
                System.out.println("No Cache object is available, so nothing to check against.");
            } else {
                assertTrue("JPAEntityA(id=" + idMain + ") should be in the cache at this point.", cache.contains(JPA20EMEntityA.class, idMain));
            }

            tx.commit();
            tx.begin();
            em.clear();

            // Verify that JPA20EMEntityA is in the cache still
            if (cache == null) {
                System.out.println("No Cache object is available, so nothing to check against.");
            } else {
                assertTrue("JPAEntityA(id=" + idMain + ") should be in the cache at this point.", cache.contains(JPA20EMEntityA.class, idMain));
            }

            /*
             * Verify that the refresh() did not update the cache, as the default behavior dictated by the spec is:
             *
             * Insert/update entity data into cache when read
             * from database and when committed into database:
             * this is the default behavior. Does not force refresh
             * of already cached items when reading from database.
             */
            entityA = em.find(JPA20EMEntityA.class, idMain);

            // Verify the entity returned by em.find()
            assertNotNull(entityA);
            assertTrue("Assert that the entity has strData \"Entity A Data\" (is \" " + entityA.getStrData() + "\")",
                       "Entity A Data".equals(entityA.getStrData()));
            assertEquals("Find() should have returned JPA20EMEntityA(id=" + idMain + "), instead was " + entityA.getId() + ")", idMain, entityA.getId());
            assertTrue("Entity should be attached to the persistence unit but is not.", em.contains(entityA));
            assertTrue("Entity should have LockModeType.NONE (is " + em.getLockMode(entityA) + ")",
                       (LockModeType.NONE == em.getLockMode(entityA) || em.getLockMode(entityA) == null));
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") has 2 JPA20EMEntityB's allocated to it " +
                       "(expecting 2, found " + entityA.getEntityBList().size() + ")",
                       entityA.getEntityBList().size() == 2);
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") now has a version of " + (version_OpenJPA - 1) + " (is " + entityA.getVersion() + ").",
                       entityA.getVersion() == version_OpenJPA - 1);

            // If a Cache object is available, make sure that the above entity is cached in it.
            if (cache == null) {
                System.out.println("No Cache object is available, so nothing to check against.");
            } else {
                assertTrue("JPAEntityA(id=" + idMain + ") should be in the cache at this point.", cache.contains(JPA20EMEntityA.class, idMain));
            }
            tx.commit();
            resetEntity();
            version_OpenJPA++;
        } catch (Exception e) {
            try {
                tx.rollback();
            } catch (Throwable t) {
                // Swallow
            }
            throw (e);
        }
    }

    public void testRefreshC001_EL() throws Exception {
        try {
            tx.begin();
            em.clear();

            // Evict the contents of the cache (if the Cache object is obtainable)
            Cache cache = em.getEntityManagerFactory().getCache();
            if (cache != null) {
                cache.evictAll();
            }

            // Call em.find() for JPA20EMEntityA, specifying NO LockModeType
            JPA20EMEntityA entityA = em.find(JPA20EMEntityA.class, idMain, LockModeType.NONE);

            assertNotNull(entityA);
            assertTrue("Entity should have strData \"Entity A Data\" (is \" " + entityA.getStrData() + "\")",
                       "Entity A Data".equals(entityA.getStrData()));
            assertEquals("Find() should have returned JPA20EMEntityA(id=" + idMain + "), instead was (" + entityA.getId() + ")", idMain, entityA.getId());
            assertTrue("Entity should be attached to the persistence unit but is not.", em.contains(entityA));
            assertTrue("Entity should have LockModeType.NONE (is " + em.getLockMode(entityA) + ")",
                       (LockModeType.NONE == em.getLockMode(entityA) || em.getLockMode(entityA) == null));
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") has 2 JPA20EMEntityB's allocated to it " +
                       "(expecting 2, found " + entityA.getEntityBList().size() + ")",
                       entityA.getEntityBList().size() == 2);
            assertTrue("JPA20EMEntityA(id=" + idMain + ") should have a version of " + version_EL + " (is " + entityA.getVersion() + ").",
                       entityA.getVersion() == version_EL);

            // If a Cache object is available, make sure that the above entity is cached in it.
            if (cache == null) {
                System.out.println("No Cache object is available, so nothing to check against.");
            } else {
                assertTrue("JPAEntityA(id=" + idMain + ") is not in the cache, but should be at this point.", cache.contains(JPA20EMEntityA.class, idMain));
            }

            //Update Entity in database without altering entityA
            UpdateEntityTask updateTask = new UpdateEntityTask(idMain);
            Future<Void> future = exec.submit(updateTask);
            version_EL++;

            future.get(100, TimeUnit.SECONDS);

            //Make sure entityA was not altered by the query
            assertTrue("Entity should have strData \"Entity A Data\" (is \" " + entityA.getStrData() + "\")",
                       "Entity A Data".equals(entityA.getStrData()));
            em.refresh(entityA);
            // Verify the entity updated by em.refresh()
            assertTrue("Entity should have strData \"Modified by B\" (is \"" + entityA.getStrData() + "\")",
                       "Modified by B".equals(entityA.getStrData()));
            assertTrue("Entity should be attached to the persistence unit but is not.", em.contains(entityA));
            assertTrue("Entity should have LockModeType.NONE (is " + em.getLockMode(entityA) + ")",
                       (LockModeType.NONE == em.getLockMode(entityA) || em.getLockMode(entityA) == null));
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") has 2 JPA20EMEntityB's allocated to it " +
                       "(expecting 2, found " + entityA.getEntityBList().size() + ")",
                       entityA.getEntityBList().size() == 2);
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") now has a version of " + version_EL + " (is " + entityA.getVersion() + ").",
                       entityA.getVersion() == version_EL);

            // If a Cache object is available, make sure that the above entity is cached in it.
            if (cache == null) {
                System.out.println("No Cache object is available, so nothing to check against.");
            } else {
                assertTrue("JPAEntityA(id=" + idMain + ") should be in the cache at this point.", cache.contains(JPA20EMEntityA.class, idMain));
            }

            tx.commit();
            resetEntity();
            version_EL++;
        } catch (Exception e) {
            try {
                tx.rollback();
            } catch (Throwable t) {
                // Swallow
            }
            throw (e);
        }
    }

    /*
     * Verify that a call to em.refresh() with StoreCacheMode=USE will not update the contents of the data cache.
     *
     */
    public void testRefreshC002_OpenJPA() throws Exception {
        try {
            tx.begin();
            em.clear();

            // Evict the contents of the cache (if the Cache object is obtainable)
            Cache cache = em.getEntityManagerFactory().getCache();
            if (cache != null) {
                cache.evictAll();
            }

            // Call em.find() for JPA20EMEntityA, specifying NO LockModeType
            JPA20EMEntityA entityA = em.find(JPA20EMEntityA.class, idMain, LockModeType.NONE);

            assertNotNull(entityA);
            assertTrue("Assert that the entity has strData \"Entity A Data\" (is \"" + entityA.getStrData() + "\")",
                       "Entity A Data".equals(entityA.getStrData()));
            assertEquals("Find() should have returned JPA20EMEntityA(id=" + idMain + "), instead was " + entityA.getId() + ")", idMain, entityA.getId());
            assertTrue("Entity should be attached to the persistence unit but is not.", em.contains(entityA));
            assertTrue("Entity should have LockModeType.NONE (is " + em.getLockMode(entityA) + ")",
                       (LockModeType.NONE == em.getLockMode(entityA) || em.getLockMode(entityA) == null));
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") has 2 JPA20EMEntityB's allocated to it " +
                       "(expecting 2, found " + entityA.getEntityBList().size() + ")",
                       entityA.getEntityBList().size() == 2);
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") now has a version of " + version_OpenJPA + " (is " + entityA.getVersion() + ").",
                       entityA.getVersion() == version_OpenJPA);

            // If a Cache object is available, make sure that the above entity is cached in it.
            if (cache == null) {
                System.out.println("No Cache object is available, so nothing to check against.");
            } else {
                assertTrue("JPAEntityA(id=" + idMain + ") is not in the cache, but should be at this point.", cache.contains(JPA20EMEntityA.class, idMain));
            }

            //Update Entity in database without altering entityA
            em_COPY.createQuery("UPDATE JPA20EMEntityA e SET e.strData = \'Modified by B\' WHERE e.id = " + idMain).executeUpdate();
            version_OpenJPA++;

            //Make sure entityA was not altered by the query
            assertTrue("Entity should have strData \"Entity A Data\" (is \" " + entityA.getStrData() + "\")",
                       "Entity A Data".equals(entityA.getStrData()));

            // Invoke em.refresh(), it should pick up the changes made by the second PU because refresh() should always
            // go to the database.
            java.util.Map<String, Object> cacheStoreModeMap = new java.util.HashMap<String, Object>();
            cacheStoreModeMap.put("javax.persistence.cache.storeMode", CacheStoreMode.USE);
            em.refresh(entityA, cacheStoreModeMap);

            // Verify the entity updated by em.refresh()
            assertTrue("Assert that the entity has strData \"Modified by B\" (is \" " + entityA.getStrData() + "\")",
                       "Modified by B".equals(entityA.getStrData()));
            assertTrue("Entity should be attached to the persistence unit but is not.", em.contains(entityA));
            assertTrue("Entity should have LockModeType.NONE (is " + em.getLockMode(entityA) + ")",
                       (LockModeType.NONE == em.getLockMode(entityA) || em.getLockMode(entityA) == null));
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") has 2 JPA20EMEntityB's allocated to it " +
                       "(expecting 2, found " + entityA.getEntityBList().size() + ")",
                       entityA.getEntityBList().size() == 2);
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") now has a version of " + version_OpenJPA + " (is " + entityA.getVersion() + ").",
                       entityA.getVersion() == version_OpenJPA);

            // If a Cache object is available, make sure that the above entity is cached in it.
            if (cache == null) {
                System.out.println("No Cache object is available, so nothing to check against.");
            } else {
                assertTrue("JPAEntityA(id=" + idMain + ") should be in the cache at this point.", cache.contains(JPA20EMEntityA.class, idMain));
            }

            tx.commit();
            tx.begin();
            em.clear();

            // Verify that JPA20EMEntityA is in the cache still
            if (cache == null) {
                System.out.println("No Cache object is available, so nothing to check against.");
            } else {
                assertTrue("JPAEntityA(id=" + idMain + ") should be in the cache at this point.", cache.contains(JPA20EMEntityA.class, idMain));
            }

            /*
             * Verify that the refresh() did not update the cache, as the behavior for StoreCacheMode.USE
             * as dictated by the spec is:
             *
             * Insert/update entity data into cache when read
             * from database and when committed into database:
             * this is the default behavior. Does not force refresh
             * of already cached items when reading from database.
             */

            entityA = em.find(JPA20EMEntityA.class, idMain);

            // Verify the entity returned by em.find()
            assertNotNull(entityA);
            assertTrue("Assert that the entity has strData \"Entity A Data\" (is \" " + entityA.getStrData() + "\")",
                       "Entity A Data".equals(entityA.getStrData()));
            assertEquals("Find() should have returned JPA20EMEntityA(id=" + idMain + "), instead was " + entityA.getId() + ")", idMain, entityA.getId());
            assertTrue("Entity should be attached to the persistence unit but is not.", em.contains(entityA));
            assertTrue("Entity should have LockModeType.NONE (is " + em.getLockMode(entityA) + ")",
                       (LockModeType.NONE == em.getLockMode(entityA) || em.getLockMode(entityA) == null));
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") has 2 JPA20EMEntityB's allocated to it " +
                       "(expecting 2, found " + entityA.getEntityBList().size() + ")",
                       entityA.getEntityBList().size() == 2);
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") now has a version of " + (version_OpenJPA - 1) + " (is " + entityA.getVersion() + ").",
                       entityA.getVersion() == version_OpenJPA - 1);

            // If a Cache object is available, make sure that the above entity is cached in it.
            if (cache == null) {
                System.out.println("No Cache object is available, so nothing to check against.");
            } else {
                assertTrue("JPAEntityA(id=" + idMain + ") is not in the cache, but should be at this point.", cache.contains(JPA20EMEntityA.class, idMain));
            }

            tx.commit();
            resetEntity();
            version_OpenJPA++;
        } catch (Exception e) {
            try {
                tx.rollback();
            } catch (Throwable t) {
                // Swallow
            }
            throw (e);
        }
    }

    public void testRefreshC002_EL() throws Exception {
        final String dbProduct = getDbProductName();
        if (dbProduct != null && dbProduct.toLowerCase().equals("oracle")) {
            System.out.println("Test is not supported on Oracle.");
            return;
        }

        try {
            tx.begin();
            em.clear();

            // Evict the contents of the cache (if the Cache object is obtainable)
            Cache cache = em.getEntityManagerFactory().getCache();
            if (cache != null) {
                cache.evictAll();
            }

            // Call em.find() for JPA20EMEntityA, specifying NO LockModeType
            JPA20EMEntityA entityA = em.find(JPA20EMEntityA.class, idMain, LockModeType.NONE);

            assertNotNull(entityA);
            assertTrue("Assert that the entity has strData \"Entity A Data\" (is \"" + entityA.getStrData() + "\")",
                       "Entity A Data".equals(entityA.getStrData()));
            assertEquals("Find() should have returned JPA20EMEntityA(id=" + idMain + "), instead was " + entityA.getId() + ")", idMain, entityA.getId());
            assertTrue("Entity should be attached to the persistence unit but is not.", em.contains(entityA));
            assertTrue("Entity should have LockModeType.NONE (is " + em.getLockMode(entityA) + ")",
                       (LockModeType.NONE == em.getLockMode(entityA) || em.getLockMode(entityA) == null));
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") has 2 JPA20EMEntityB's allocated to it " +
                       "(expecting 2, found " + entityA.getEntityBList().size() + ")",
                       entityA.getEntityBList().size() == 2);
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") now has a version of " + version_EL + " (is " + entityA.getVersion() + ").",
                       entityA.getVersion() == version_EL);

            // If a Cache object is available, make sure that the above entity is cached in it.
            if (cache == null) {
                System.out.println("No Cache object is available, so nothing to check against.");
            } else {
                assertTrue("JPAEntityA(id=" + idMain + ") is not in the cache, but should be at this point.", cache.contains(JPA20EMEntityA.class, idMain));
            }

            //Update Entity in database without altering entityA
            UpdateEntityTask updateTask = new UpdateEntityTask(idMain);
            Future<Void> future = exec.submit(updateTask);
            version_EL++;

            future.get(100, TimeUnit.SECONDS);

            //Make sure entityA was not altered by the query
            assertTrue("Entity should have strData \"Entity A Data\" (is \" " + entityA.getStrData() + "\")",
                       "Entity A Data".equals(entityA.getStrData()));

            // Invoke em.refresh(), it should pick up the changes made by the second PU because refresh() should always
            // go to the database.
            java.util.Map<String, Object> cacheStoreModeMap = new java.util.HashMap<String, Object>();
            cacheStoreModeMap.put("javax.persistence.cache.storeMode", CacheStoreMode.USE);
            em.refresh(entityA, cacheStoreModeMap);

            // Verify the entity updated by em.refresh()
            assertTrue("Assert that the entity has strData \"Modified by B\" (is \" " + entityA.getStrData() + "\")",
                       "Modified by B".equals(entityA.getStrData()));
            assertTrue("Entity should be attached to the persistence unit but is not.", em.contains(entityA));
            assertTrue("Entity should have LockModeType.NONE (is " + em.getLockMode(entityA) + ")",
                       (LockModeType.NONE == em.getLockMode(entityA) || em.getLockMode(entityA) == null));
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") has 2 JPA20EMEntityB's allocated to it " +
                       "(expecting 2, found " + entityA.getEntityBList().size() + ")",
                       entityA.getEntityBList().size() == 2);
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") now has a version of " + version_EL + " (is " + entityA.getVersion() + ").",
                       entityA.getVersion() == version_EL);

            // If a Cache object is available, make sure that the above entity is cached in it.
            if (cache == null) {
                System.out.println("No Cache object is available, so nothing to check against.");
            } else {
                assertTrue("JPAEntityA(id=" + idMain + ") should be in the cache at this point.", cache.contains(JPA20EMEntityA.class, idMain));
            }
            tx.commit();
            resetEntity();
            version_EL++;
        } catch (Exception e) {
            try {
                tx.rollback();
            } catch (Throwable t) {
                // Swallow
            }
            throw (e);
        }
    }

    /*
     * Verify that a call to em.refresh() with StoreCacheMode=BYPASS will not update
     * the contents of the data cache.
     */

    public void testRefreshC003_OpenJPA() throws Exception {
        try {
            tx.begin();
            em.clear();

            // Evict the contents of the cache (if the Cache object is obtainable)
            Cache cache = em.getEntityManagerFactory().getCache();
            if (cache != null) {
                cache.evictAll();
            }

            // Call em.find() for JPA20EMEntityA, specifying NO LockModeType
            JPA20EMEntityA entityA = em.find(JPA20EMEntityA.class, idMain, LockModeType.NONE);

            // Verify the entity returned by em.find()
            assertNotNull(entityA);
            assertTrue("Assert that the entity has strData \"Entity A Data\" (is \" " + entityA.getStrData() + "\")",
                       "Entity A Data".equals(entityA.getStrData()));
            assertEquals("Find() should have returned JPA20EMEntityA(id=" + idMain + "), instead was " + entityA.getId() + ")", idMain, entityA.getId());
            assertTrue("Entity should be attached to the persistence unit but is not.", em.contains(entityA));
            assertTrue("Entity should have LockModeType.NONE (is " + em.getLockMode(entityA) + ")",
                       (LockModeType.NONE == em.getLockMode(entityA) || em.getLockMode(entityA) == null));
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") has 2 JPA20EMEntityB's allocated to it " +
                       "(expecting 2, found " + entityA.getEntityBList().size() + ")",
                       entityA.getEntityBList().size() == 2);
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") now has a version of " + version_OpenJPA + " (is " + entityA.getVersion() + ").",
                       entityA.getVersion() == version_OpenJPA);

            // If a Cache object is available, make sure that the above entity is cached in it.
            if (cache == null) {
                System.out.println("No Cache object is available, so nothing to check against.");
            } else {
                assertTrue("JPAEntityA(id=" + idMain + ") is not in the cache, but should be at this point.", cache.contains(JPA20EMEntityA.class, idMain));
            }

            //Update Entity in database without altering entityA
            em_COPY.createQuery("UPDATE JPA20EMEntityA e SET e.strData = \'Modified by B\' WHERE e.id = " + idMain).executeUpdate();
            version_OpenJPA++;

            //Make sure entityA was not altered by the query
            assertTrue("Entity should have strData \"Entity A Data\" (is \" " + entityA.getStrData() + "\")",
                       "Entity A Data".equals(entityA.getStrData()));

            // Invoke em.refresh(), it should pick up the changes made by Client B because refresh() should always
            // go to the database.
            java.util.Map<String, Object> cacheStoreModeMap = new java.util.HashMap<String, Object>();
            cacheStoreModeMap.put("javax.persistence.cache.storeMode", CacheStoreMode.BYPASS);
            em.refresh(entityA, cacheStoreModeMap);

            // Verify the entity updated by em.refresh()

            assertTrue("Assert that the entity has strData \"Modified by B\" (is \" " + entityA.getStrData() + "\")",
                       "Modified by B".equals(entityA.getStrData()));
            assertTrue("Entity should be attached to the persistence unit but is not.", em.contains(entityA));
            assertTrue("Entity should have LockModeType.NONE (is " + em.getLockMode(entityA) + ")",
                       (LockModeType.NONE == em.getLockMode(entityA) || em.getLockMode(entityA) == null));
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") has 2 JPA20EMEntityB's allocated to it " +
                       "(expecting 2, found " + entityA.getEntityBList().size() + ")",
                       entityA.getEntityBList().size() == 2);
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") now has a version of " + version_OpenJPA + " (is " + entityA.getVersion() + ").",
                       entityA.getVersion() == version_OpenJPA);

            // If a Cache object is available, make sure that the above entity is cached in it.
            if (cache == null) {
                System.out.println("No Cache object is available, so nothing to check against.");
            } else {
                assertTrue("JPAEntityA(id=" + idMain + ") is not in the cache, but should be at this point.", cache.contains(JPA20EMEntityA.class, idMain));
            }

            tx.commit();
            tx.begin();
            em.clear();

            // Verify that JPA20EMEntityA is in the cache still
            if (cache == null) {
                System.out.println("No Cache object is available, so nothing to check against.");
            } else {
                assertTrue("JPAEntityA(id=" + idMain + ") is not in the cache, but should be at this point.", cache.contains(JPA20EMEntityA.class, idMain));
            }

            /*
             * Verify that the refresh() did not update the cache, as the behavior of StoreCacheMode.BYPASS
             * as dictated by the spec is:
             *
             * Don't insert into the cache.
             */

            entityA = em.find(JPA20EMEntityA.class, idMain);

            // Verify the entity returned by em.find()
            assertNotNull(entityA);
            assertTrue("Assert that the entity has strData \"Entity A Data\" (is \" " + entityA.getStrData() + "\")",
                       "Entity A Data".equals(entityA.getStrData()));
            assertEquals("Find() should have returned JPA20EMEntityA(id=" + idMain + "), instead was " + entityA.getId() + ")", idMain, entityA.getId());
            assertTrue("Entity should be attached to the persistence unit but is not.", em.contains(entityA));
            assertTrue("Entity should have LockModeType.NONE (is " + em.getLockMode(entityA) + ")",
                       (LockModeType.NONE == em.getLockMode(entityA) || em.getLockMode(entityA) == null));
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") has 2 JPA20EMEntityB's allocated to it " +
                       "(expecting 2, found " + entityA.getEntityBList().size() + ")",
                       entityA.getEntityBList().size() == 2);
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") now has a version of " + (version_OpenJPA - 1) + " (is " + entityA.getVersion() + ").",
                       entityA.getVersion() == version_OpenJPA - 1);

            // If a Cache object is available, make sure that the above entity is cached in it.
            if (cache == null) {
                System.out.println("No Cache object is available, so nothing to check against.");
            } else {
                assertTrue("JPAEntityA(id=" + idMain + ") is not in the cache, but should be at this point.", cache.contains(JPA20EMEntityA.class, idMain));
            }

            tx.commit();
            resetEntity();
            version_OpenJPA++;
        } catch (Exception e) {
            try {
                tx.rollback();
            } catch (Throwable t) {
                // Swallow
            }
            throw (e);
        }
    }

    public void testRefreshC003_EL() throws Exception {
        final String dbProduct = getDbProductName();
        if (dbProduct != null && dbProduct.toLowerCase().equals("oracle")) {
            System.out.println("Test is not supported on Oracle.");
            return;
        }

        try {
            tx.begin();
            em.clear();

            // Evict the contents of the cache (if the Cache object is obtainable)
            Cache cache = em.getEntityManagerFactory().getCache();
            if (cache != null) {
                cache.evictAll();
            }

            // Call em.find() for JPA20EMEntityA, specifying NO LockModeType
            JPA20EMEntityA entityA = em.find(JPA20EMEntityA.class, idMain, LockModeType.NONE);

            // Verify the entity returned by em.find()
            assertNotNull(entityA);
            assertTrue("Assert that the entity has strData \"Entity A Data\" (is \" " + entityA.getStrData() + "\")",
                       "Entity A Data".equals(entityA.getStrData()));
            assertEquals("Find() should have returned JPA20EMEntityA(id=" + idMain + "), instead was " + entityA.getId() + ")", idMain, entityA.getId());
            assertTrue("Entity should be attached to the persistence unit but is not.", em.contains(entityA));
            assertTrue("Entity should have LockModeType.NONE (is " + em.getLockMode(entityA) + ")",
                       (LockModeType.NONE == em.getLockMode(entityA) || em.getLockMode(entityA) == null));
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") has 2 JPA20EMEntityB's allocated to it " +
                       "(expecting 2, found " + entityA.getEntityBList().size() + ")",
                       entityA.getEntityBList().size() == 2);
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") now has a version of " + version_EL + " (is " + entityA.getVersion() + ").",
                       entityA.getVersion() == version_EL);

            // If a Cache object is available, make sure that the above entity is cached in it.
            if (cache == null) {
                System.out.println("No Cache object is available, so nothing to check against.");
            } else {
                assertTrue("JPAEntityA(id=" + idMain + ") is not in the cache, but should be at this point.", cache.contains(JPA20EMEntityA.class, idMain));
            }

            //Update Entity in database without altering entityA
            UpdateEntityTask updateTask = new UpdateEntityTask(idMain);
            Future<Void> future = exec.submit(updateTask);
            version_EL++;

            future.get(100, TimeUnit.SECONDS);

            //Make sure entityA was not altered by the query
            assertTrue("Entity should have strData \"Entity A Data\" (is \" " + entityA.getStrData() + "\")",
                       "Entity A Data".equals(entityA.getStrData()));

            // Invoke em.refresh(), it should pick up the changes made by Client B because refresh() should always
            // go to the database.
            java.util.Map<String, Object> cacheStoreModeMap = new java.util.HashMap<String, Object>();
            cacheStoreModeMap.put("javax.persistence.cache.storeMode", CacheStoreMode.BYPASS);
            em.refresh(entityA, cacheStoreModeMap);

            // Verify the entity updated by em.refresh()
            assertTrue("Assert that the entity has strData \"Modified by B\" (is \" " + entityA.getStrData() + "\")",
                       "Modified by B".equals(entityA.getStrData()));
            assertTrue("Entity should be attached to the persistence unit but is not.", em.contains(entityA));
            assertTrue("Entity should have LockModeType.NONE (is " + em.getLockMode(entityA) + ")",
                       (LockModeType.NONE == em.getLockMode(entityA) || em.getLockMode(entityA) == null));
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") has 2 JPA20EMEntityB's allocated to it " +
                       "(expecting 2, found " + entityA.getEntityBList().size() + ")",
                       entityA.getEntityBList().size() == 2);
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") now has a version of " + version_EL + " (is " + entityA.getVersion() + ").",
                       entityA.getVersion() == version_EL);

            // If a Cache object is available, make sure that the above entity is cached in it.
            if (cache == null) {
                System.out.println("No Cache object is available, so nothing to check against.");
            } else {
                assertTrue("JPAEntityA(id=" + idMain + ") is not in the cache, but should be at this point.", cache.contains(JPA20EMEntityA.class, idMain));
            }

            tx.commit();
            resetEntity();
            version_EL++;
        } catch (Exception e) {
            try {
                tx.rollback();
            } catch (Throwable t) {
                // Swallow
            }
            throw (e);
        }
    }

    /*
     * Verify that that a call to em.refresh() with StoreCacheMode=REFRESH will update
     * the contents of the data cache. [Basic persistence provider sanity testing]
     */

    public void testRefreshC004_OpenJPA() throws Exception {
        try {
            tx.begin();
            em.clear();

            // Evict the contents of the cache (if the Cache object is obtainable)
            Cache cache = em.getEntityManagerFactory().getCache();
            if (cache != null) {
                cache.evictAll();
            }

            // Call em.find() for JPA20EMEntityA, specifying NO LockModeType
            JPA20EMEntityA entityA = em.find(JPA20EMEntityA.class, idMain, LockModeType.NONE);

            // Verify the entity returned by em.find()
            assertNotNull(entityA);
            assertTrue("Assert that the entity has strData \"Entity A Data\" (is \" " + entityA.getStrData() + "\")",
                       "Entity A Data".equals(entityA.getStrData()));
            assertEquals("Find() should have returned JPA20EMEntityA(id=" + idMain + "), instead was " + entityA.getId() + ")", idMain, entityA.getId());
            assertTrue("Entity should be attached to the persistence unit but is not.", em.contains(entityA));
            assertTrue("Entity should have LockModeType.NONE (is " + em.getLockMode(entityA) + ")",
                       (LockModeType.NONE == em.getLockMode(entityA) || em.getLockMode(entityA) == null));
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") has 2 JPA20EMEntityB's allocated to it " +
                       "(expecting 2, found " + entityA.getEntityBList().size() + ")",
                       entityA.getEntityBList().size() == 2);
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") now has a version of " + version_OpenJPA + " (is " + entityA.getVersion() + ").",
                       entityA.getVersion() == version_OpenJPA);

            // If a Cache object is available, make sure that the above entity is cached in it.
            if (cache == null) {
                System.out.println("No Cache object is available, so nothing to check against.");
            } else {
                assertTrue("JPAEntityA(id=" + idMain + ") is not in the cache, but should be at this point.", cache.contains(JPA20EMEntityA.class, idMain));
            }

            //Update Entity in database without altering entityA
            em.createQuery("UPDATE JPA20EMEntityA e SET e.strData = \'Modified by B\' WHERE e.id = " + idMain).executeUpdate();
            version_OpenJPA++;

            //Make sure entityA was not altered by the query
            assertTrue("Entity should have strData \"Entity A Data\" (is \" " + entityA.getStrData() + "\")",
                       "Entity A Data".equals(entityA.getStrData()));

            // Invoke em.refresh(), it should pick up the changes made by the second PU because refresh() should always
            // go to the database.
            java.util.Map<String, Object> cacheStoreModeMap = new java.util.HashMap<String, Object>();
            cacheStoreModeMap.put("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH);
            em.refresh(entityA, cacheStoreModeMap);
            // Verify the entity updated by em.refresh()
            assertTrue("Assert that the entity has strData \"Modified by B\" (is \" " + entityA.getStrData() + "\")",
                       "Modified by B".equals(entityA.getStrData()));
            assertTrue("Entity should be attached to the persistence unit but is not.", em.contains(entityA));
            assertTrue("Entity should have LockModeType.NONE (is " + em.getLockMode(entityA) + ")",
                       (LockModeType.NONE == em.getLockMode(entityA) || em.getLockMode(entityA) == null));
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") has 2 JPA20EMEntityB's allocated to it " +
                       "(expecting 2, found " + entityA.getEntityBList().size() + ")",
                       entityA.getEntityBList().size() == 2);
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") now has a version of " + version_OpenJPA + " (is " + entityA.getVersion() + ").",
                       entityA.getVersion() == version_OpenJPA);

            // If a Cache object is available, make sure that the above entity is cached in it.
            if (cache == null) {
                System.out.println("No Cache object is available, so nothing to check against.");
            } else {
                assertTrue("JPAEntityA(id=" + idMain + ") is not in the cache, but should be at this point.", cache.contains(JPA20EMEntityA.class, idMain));
            }
            tx.commit();
            tx.begin();
            em.clear();

            /*
             * Verify that the refresh() did update the cache, as the behavior of StoreCacheMode.REFRESH dictated by the spec is:
             *
             * Insert/update entity data into cache when read
             * from database and when committed into database
             * Forces refresh of cache for items read from database.
             */

            entityA = em.find(JPA20EMEntityA.class, idMain);

            // Verify the entity returned by em.find()
            assertNotNull(entityA);
            assertTrue("Assert that the entity has strData \"Modified by B\" (is \" " + entityA.getStrData() + "\")",
                       "Modified by B".equals(entityA.getStrData()));
            assertEquals("Find() should have returned JPA20EMEntityA(id=" + idMain + "), instead was " + entityA.getId() + ")", idMain, entityA.getId());
            assertTrue("Entity should be attached to the persistence unit but is not.", em.contains(entityA));
            assertTrue("Entity should have LockModeType.NONE (is " + em.getLockMode(entityA) + ")",
                       (LockModeType.NONE == em.getLockMode(entityA) || em.getLockMode(entityA) == null));
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") has 2 JPA20EMEntityB's allocated to it " +
                       "(expecting 2, found " + entityA.getEntityBList().size() + ")",
                       entityA.getEntityBList().size() == 2);
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") now has a version of " + version_OpenJPA + " (is " + entityA.getVersion() + ").",
                       entityA.getVersion() == version_OpenJPA);

            // If a Cache object is available, make sure that the above entity is cached in it.
            if (cache == null) {
                System.out.println("No Cache object is available, so nothing to check against.");
            } else {
                assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") IS in the cache.", cache.contains(JPA20EMEntityA.class, idMain));
            }

            tx.commit();
            resetEntity();
            version_OpenJPA++;
        } catch (Exception e) {
            try {
                tx.rollback();
            } catch (Throwable t) {
                // Swallow
            }
            throw (e);
        }
    }

    public void testRefreshC004_EL() throws Exception {
        final String dbProduct = getDbProductName();
        if (dbProduct != null && dbProduct.toLowerCase().equals("oracle")) {
            System.out.println("Test is not supported on Oracle.");
            return;
        }

        try {
            tx.begin();
            em.clear();

            // Evict the contents of the cache (if the Cache object is obtainable)
            Cache cache = em.getEntityManagerFactory().getCache();
            if (cache != null) {
                cache.evictAll();
            }

            // Call em.find() for JPA20EMEntityA, specifying NO LockModeType
            JPA20EMEntityA entityA = em.find(JPA20EMEntityA.class, idMain, LockModeType.NONE);

            // Verify the entity returned by em.find()
            assertNotNull(entityA);
            assertTrue("Assert that the entity has strData \"Entity A Data\" (is \" " + entityA.getStrData() + "\")",
                       "Entity A Data".equals(entityA.getStrData()));
            assertEquals("Find() should have returned JPA20EMEntityA(id=" + idMain + "), instead was " + entityA.getId() + ")", idMain, entityA.getId());
            assertTrue("Entity should be attached to the persistence unit but is not.", em.contains(entityA));
            assertTrue("Entity should have LockModeType.NONE (is " + em.getLockMode(entityA) + ")",
                       (LockModeType.NONE == em.getLockMode(entityA) || em.getLockMode(entityA) == null));
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") has 2 JPA20EMEntityB's allocated to it " +
                       "(expecting 2, found " + entityA.getEntityBList().size() + ")",
                       entityA.getEntityBList().size() == 2);
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") now has a version of " + version_EL + " (is " + entityA.getVersion() + ").",
                       entityA.getVersion() == version_EL);

            // If a Cache object is available, make sure that the above entity is cached in it.
            if (cache == null) {
                System.out.println("No Cache object is available, so nothing to check against.");
            } else {
                assertTrue("JPAEntityA(id=" + idMain + ") is not in the cache, but should be at this point.", cache.contains(JPA20EMEntityA.class, idMain));
            }

            //Update Entity in database without altering entityA
            UpdateEntityTask updateTask = new UpdateEntityTask(idMain);
            Future<Void> future = exec.submit(updateTask);
            version_EL++;

            future.get(100, TimeUnit.SECONDS);

            //Make sure entityA was not altered by the query
            assertTrue("Entity should have strData \"Entity A Data\" (is \" " + entityA.getStrData() + "\")",
                       "Entity A Data".equals(entityA.getStrData()));

            // Invoke em.refresh(), it should pick up the changes made by the second PU because refresh() should always
            // go to the database.
            java.util.Map<String, Object> cacheStoreModeMap = new java.util.HashMap<String, Object>();
            cacheStoreModeMap.put("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH);
            em.refresh(entityA, cacheStoreModeMap);
            // Verify the entity updated by em.refresh()

            assertTrue("Assert that the entity has strData \"Modified by B\" (is \" " + entityA.getStrData() + "\")",
                       "Modified by B".equals(entityA.getStrData()));
            assertTrue("Entity should be attached to the persistence unit but is not.", em.contains(entityA));
            assertTrue("Entity should have LockModeType.NONE (is " + em.getLockMode(entityA) + ")",
                       (LockModeType.NONE == em.getLockMode(entityA) || em.getLockMode(entityA) == null));
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") has 2 JPA20EMEntityB's allocated to it " +
                       "(expecting 2, found " + entityA.getEntityBList().size() + ")",
                       entityA.getEntityBList().size() == 2);
            assertTrue("Assert that JPA20EMEntityA(id=" + idMain + ") now has a version of " + version_EL + " (is " + entityA.getVersion() + ").",
                       entityA.getVersion() == version_EL);

            // If a Cache object is available, make sure that the above entity is cached in it.
            if (cache == null) {
                System.out.println("No Cache object is available, so nothing to check against.");
            } else {
                assertTrue("JPAEntityA(id=" + idMain + ") is not in the cache, but should be at this point.", cache.contains(JPA20EMEntityA.class, idMain));
            }

            tx.commit();
            resetEntity();
            version_EL++;
        } catch (Exception e) {
            try {
                tx.rollback();
            } catch (Throwable t) {
                // Swallow
            }
            throw (e);
        }
    }
}
