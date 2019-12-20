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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Resource;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.persistence.Cache;
import javax.persistence.CacheRetrieveMode;
import javax.persistence.CacheStoreMode;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.ws.jpa.cache.model.Employee;

import componenttest.app.FATServlet;

//TestScenarioC004(a-c) is not running in OpenJPA because of a defect
//that is currently being worked on. This test will be enabled
//in ChangeSet 219418.

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/FindCacheTestServlet")
public class FindCacheTestServlet extends FATServlet {
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

    @PersistenceContext(unitName = "CACHE_JEE_DCTRUE")
    EntityManager em_SCM_ALL;

    @PersistenceContext(unitName = "CACHE_JEE_DCTRUE_COPY")
    EntityManager em_SCM_ALL_COPY;

    @PersistenceContext(unitName = "CACHE_JEE_DCFALSE")
    EntityManager em_SCM_NONE;

    @PersistenceContext(unitName = "CACHE_JEE_DCTRUE_SCM_ENASEL")
    EntityManager em_SCM_ENASEL;

    @PersistenceContext(unitName = "CACHE_JEE_DCTRUE_SCM_ENASEL_COPY")
    EntityManager em_SCM_ENASEL_COPY;

    @PersistenceContext(unitName = "CACHE_JEE_DCTRUE_SCM_DISSEL")
    EntityManager em_SCM_DISSEL;

    @PersistenceContext(unitName = "CACHE_JEE_DCTRUE_SCM_DISSEL_COPY")
    EntityManager em_SCM_DISSEL_COPY;

    @Resource
    UserTransaction tx;

    @Override
    public void init() throws ServletException {
        try {
            populateDatabase();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    };

    // Initialize Lock Mode Variation Set
    private static LockModeType[] lockModeTypeTestArr = { null, LockModeType.NONE, LockModeType.OPTIMISTIC,
                                                          LockModeType.OPTIMISTIC_FORCE_INCREMENT, LockModeType.PESSIMISTIC_FORCE_INCREMENT,
                                                          LockModeType.PESSIMISTIC_FORCE_INCREMENT, LockModeType.PESSIMISTIC_READ,
                                                          LockModeType.PESSIMISTIC_WRITE, LockModeType.READ, LockModeType.WRITE };

    // Initialize Property Variations Set
    private static java.util.ArrayList<java.util.HashMap<String, String>> testAssertionVariationSet = new java.util.ArrayList<java.util.HashMap<String, String>>();

    // Populate testAssertionVariationSet
    static {
        java.util.HashMap<String, String> testAssertionVariationMap = null;

        testAssertionVariationMap = new java.util.HashMap<String, String>();
        testAssertionVariationMap.put("employeePK", "1");
        testAssertionVariationMap.put("CacheRetrieveMode", "USE");
        testAssertionVariationMap.put("CacheStoreMode", "");
        testAssertionVariationSet.add(testAssertionVariationMap);

        testAssertionVariationMap = new java.util.HashMap<String, String>();
        testAssertionVariationMap.put("employeePK", "2");
        testAssertionVariationMap.put("CacheRetrieveMode", "BYPASS");
        testAssertionVariationMap.put("CacheStoreMode", "");
        testAssertionVariationSet.add(testAssertionVariationMap);

        testAssertionVariationMap = new java.util.HashMap<String, String>();
        testAssertionVariationMap.put("employeePK", "3");
        testAssertionVariationMap.put("CacheRetrieveMode", "");
        testAssertionVariationMap.put("CacheStoreMode", "USE");
        testAssertionVariationSet.add(testAssertionVariationMap);

        testAssertionVariationMap = new java.util.HashMap<String, String>();
        testAssertionVariationMap.put("employeePK", "4");
        testAssertionVariationMap.put("CacheRetrieveMode", "");
        testAssertionVariationMap.put("CacheStoreMode", "BYPASS");
        testAssertionVariationSet.add(testAssertionVariationMap);

        testAssertionVariationMap = new java.util.HashMap<String, String>();
        testAssertionVariationMap.put("employeePK", "5");
        testAssertionVariationMap.put("CacheRetrieveMode", "");
        testAssertionVariationMap.put("CacheStoreMode", "REFRESH");
        testAssertionVariationSet.add(testAssertionVariationMap);

        testAssertionVariationMap = new java.util.HashMap<String, String>();
        testAssertionVariationMap.put("employeePK", "6");
        testAssertionVariationMap.put("CacheRetrieveMode", "USE");
        testAssertionVariationMap.put("CacheStoreMode", "USE");
        testAssertionVariationSet.add(testAssertionVariationMap);

        testAssertionVariationMap = new java.util.HashMap<String, String>();
        testAssertionVariationMap.put("employeePK", "7");
        testAssertionVariationMap.put("CacheRetrieveMode", "USE");
        testAssertionVariationMap.put("CacheStoreMode", "BYPASS");
        testAssertionVariationSet.add(testAssertionVariationMap);

        testAssertionVariationMap = new java.util.HashMap<String, String>();
        testAssertionVariationMap.put("employeePK", "8");
        testAssertionVariationMap.put("CacheRetrieveMode", "USE");
        testAssertionVariationMap.put("CacheStoreMode", "REFRESH");
        testAssertionVariationSet.add(testAssertionVariationMap);

        testAssertionVariationMap = new java.util.HashMap<String, String>();
        testAssertionVariationMap.put("employeePK", "9");
        testAssertionVariationMap.put("CacheRetrieveMode", "BYPASS");
        testAssertionVariationMap.put("CacheStoreMode", "USE");
        testAssertionVariationSet.add(testAssertionVariationMap);

        testAssertionVariationMap = new java.util.HashMap<String, String>();
        testAssertionVariationMap.put("employeePK", "10");
        testAssertionVariationMap.put("CacheRetrieveMode", "BYPASS");
        testAssertionVariationMap.put("CacheStoreMode", "BYPASS");
        testAssertionVariationSet.add(testAssertionVariationMap);

        testAssertionVariationMap = new java.util.HashMap<String, String>();
        testAssertionVariationMap.put("employeePK", "11");
        testAssertionVariationMap.put("CacheRetrieveMode", "BYPASS");
        testAssertionVariationMap.put("CacheStoreMode", "REFRESH");
        testAssertionVariationSet.add(testAssertionVariationMap);
    }

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

    private void populateDatabase() throws Exception {
        tx.begin();
        em_SCM_ALL.clear();

        Employee emp1 = new Employee();
        emp1.setFirstName("John");
        emp1.setLastName("Smith");
        emp1.setId(1);
        em_SCM_ALL.persist(emp1);

        Employee emp2 = new Employee();
        emp2.setFirstName("Aaron");
        emp2.setLastName("Webb");
        emp2.setId(2);
        em_SCM_ALL.persist(emp2);

        Employee emp3 = new Employee();
        emp3.setFirstName("Jack");
        emp3.setLastName("Callahan");
        emp3.setId(3);
        em_SCM_ALL.persist(emp3);

        Employee emp4 = new Employee();
        emp4.setFirstName("Matthew");
        emp4.setLastName("Carter");
        emp4.setId(4);
        em_SCM_ALL.persist(emp4);

        Employee emp5 = new Employee();
        emp5.setFirstName("Elizabeth");
        emp5.setLastName("Jackson");
        emp5.setId(5);
        em_SCM_ALL.persist(emp5);

        Employee emp6 = new Employee();
        emp6.setFirstName("Jesse");
        emp6.setLastName("Wilson");
        emp6.setId(6);
        em_SCM_ALL.persist(emp6);

        Employee emp7 = new Employee();
        emp7.setFirstName("Blake");
        emp7.setLastName("Harper");
        emp7.setId(7);
        em_SCM_ALL.persist(emp7);

        Employee emp8 = new Employee();
        emp8.setFirstName("Brandon");
        emp8.setLastName("Ewing");
        emp8.setId(8);
        em_SCM_ALL.persist(emp8);

        Employee emp9 = new Employee();
        emp9.setFirstName("Kevin");
        emp9.setLastName("Waters");
        emp9.setId(9);
        em_SCM_ALL.persist(emp9);

        Employee emp10 = new Employee();
        emp10.setFirstName("Peter");
        emp10.setLastName("Davis");
        emp10.setId(10);
        em_SCM_ALL.persist(emp10);

        Employee emp11 = new Employee();
        emp11.setFirstName("Mike");
        emp11.setLastName("Johnson");
        emp11.setId(11);
        em_SCM_ALL.persist(emp11);

        Employee emp12 = new Employee();
        emp12.setFirstName("Rachel");
        emp12.setLastName("Campbell");
        emp12.setId(12);
        em_SCM_ALL.persist(emp12);

        tx.commit();
        em_SCM_ALL.clear();
    }

    /*
     * Datacache Properties Tests
     */

    /**
     * Verify that caching is disabled with the persistence.xml setting 'shared-cache-mode' is set to 'NONE'.
     * [Basic persistence provider sanity testing] - needed to provide a base line for the property tests
     * without having to rely on other FATs for proof.
     */
    @Test
    public void jpa_jpa20_cache_testScenarioC001() throws Exception {
        int id = 1;
        try {
            tx.begin();

            // Evict the contents of the cache (if the Cache object is obtainable)
            Cache cache = em_SCM_NONE.getEntityManagerFactory().getCache();
            if (cache != null) {
                cache.evictAll();
            }

            // Perform a generic find
            Employee emp = em_SCM_NONE.find(Employee.class, id);
            assertNotNull("Find() did not successfully return an entity object", emp);

            // If a Cache object is available, make sure that the above entity is not cached in it.
            assertNotNull(cache);
            assertFalse("Employee(id=" + id + ") should NOT be in the cache.", cache.contains(Employee.class, id));

            tx.commit();
        } catch (Exception e) {
            try {
                tx.rollback();
            } catch (Throwable t) {
                // Swallow
            }
            throw (e);
        }
    }

    /**
     * Invoke testScenarioC002() with shared-cache-mode == ALL
     */
    @Test
    public void jpa_jpa20_cache_testScenarioC002a() throws Exception {
        testScenarioC002(em_SCM_ALL);
    }

    /**
     * Invoke testScenarioC002() with shared-cache-mode == ENABLE_SELECTIVE
     */
    @Test
    public void jpa_jpa20_cache_testScenarioC002b() throws Exception {
        testScenarioC002(em_SCM_ENASEL);
    }

    /**
     * Invoke testScenarioC002() with shared-cache-mode == DISABLE_SELECTIVE
     */
    @Test
    public void jpa_jpa20_cache_testScenarioC002c() throws Exception {
        testScenarioC002(em_SCM_DISSEL);
    }

    /**
     * Verify that caching is enabled and that all entities are cached with the persistence.xml setting
     * 'shared-cache-mode' set to 'ALL|ENABLE_SELECTIVE|DISABLE_SELECTIVE'.
     * [Basic persistence provider sanity testing] - needed to provide a base line for the property tests
     * without having to rely on other FATs for proof.
     */
    private void testScenarioC002(EntityManager emanager) throws Exception {
        int id = 2;
        try {
            tx.begin();

            // Evict the contents of the cache (if the Cache object is obtainable)
            Cache cache = emanager.getEntityManagerFactory().getCache();
            if (cache != null) {
                cache.evictAll();
            }

            // Perform a generic find
            Employee emp = emanager.find(Employee.class, id);
            assertNotNull("Find() did not successfully return an entity object", emp);
            // If a Cache object is available, make sure that the above entity is cached in it.
            assertNotNull(cache);
            assertTrue(cache.contains(Employee.class, id));

            tx.commit();
        } catch (Exception e) {
            try {
                tx.rollback();
            } catch (Throwable t) {
                // Swallow
            }
            throw (e);
        }
    }

    /**
     * Verify that with a shared-cache-mode of NONE, that both javax.persistence.cache.retrieveMode and
     * javax.persistence.cache.storeMode are ignored. Specifying these settings in em.find()'s property map
     * should not cause an Exception to be thrown, and it should not cause em.find() to fail in returning the
     * requested entity.
     */
    @Test
    public void jpa_jpa20_cache_testScenarioC003() throws Exception {
        try {
            for (LockModeType lmt : lockModeTypeTestArr) {
                tx.begin();

                // Evict the contents of the cache (if the Cache object is obtainable)
                Cache cache = em_SCM_NONE.getEntityManagerFactory().getCache();
                if (cache != null) {
                    cache.evictAll();
                }

                for (java.util.HashMap<String, String> testAssertionVariation : testAssertionVariationSet) {
                    java.util.Map<String, Object> emFindPropMap = new java.util.HashMap<String, Object>();

                    String pkid = testAssertionVariation.get("employeePK");
                    String crm = testAssertionVariation.get("CacheRetrieveMode");
                    String csm = testAssertionVariation.get("CacheStoreMode");

                    int empid = Integer.parseInt(pkid);

                    if ((crm != null) && !("".equals(crm))) {
                        if ("USE".equalsIgnoreCase(crm)) {
                            emFindPropMap.put("javax.persistence.cache.retrieveMode", CacheRetrieveMode.USE);
                        } else if ("BYPASS".equalsIgnoreCase(crm)) {
                            emFindPropMap.put("javax.persistence.cache.retrieveMode", CacheRetrieveMode.BYPASS);
                        }
                    }
                    if ((csm != null) && !("".equals(csm))) {
                        if ("USE".equalsIgnoreCase(csm)) {
                            emFindPropMap.put("javax.persistence.cache.storeMode", CacheStoreMode.USE);
                        } else if ("BYPASS".equalsIgnoreCase(csm)) {
                            emFindPropMap.put("javax.persistence.cache.storeMode", CacheStoreMode.BYPASS);
                        } else if ("REFRESH".equalsIgnoreCase(csm)) {
                            emFindPropMap.put("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH);
                        }
                    }

                    Employee employee = null;
                    if (lmt == null) {
                        employee = em_SCM_NONE.find(Employee.class, empid, emFindPropMap);
                    } else {
                        employee = em_SCM_NONE.find(Employee.class, empid, lmt, emFindPropMap);
                    }
                    assertNotNull("Find() did not return an entity object", employee);
                    assertEquals("The entity returned should have id=" + employee.getId() + ", has id=" + empid, empid, employee.getId());
                    // If a Cache object is available, make sure that the above entity is not cached in it.
                    assertNotNull(cache);
                    assertFalse(cache.contains(Employee.class, empid));
                }
                tx.commit();
            }
        } catch (Exception e) {
            try {
                tx.rollback();
            } catch (Throwable t) {
                // Swallow
            }
            throw (e);
        }
    }

    /**
     * Invoke testScenarioC004 with shared-cache-mode of ALL
     */
    @Test
    public void jpa_jpa20_cache_testScenarioC004a() throws Exception {
        if (isUsingJPA20Feature()) {
            return; // Not for OpenJPA
        }
        testScenarioC004(em_SCM_ALL, em_SCM_ALL_COPY);
    }

    /**
     * Invoke testScenarioC004 with shared-cache-mode of ENABLE_SELECTIVE
     */
    @Test
    public void jpa_jpa20_cache_testScenarioC004b() throws Exception {
        if (isUsingJPA20Feature()) {
            return; // Not for OpenJPA
        }
        testScenarioC004(em_SCM_ENASEL, em_SCM_ENASEL_COPY);
    }

    /**
     * Invoke testScenarioC004 with shared-cache-mode of DISABLE_SELECTIVE
     */
    @Test
    public void jpa_jpa20_cache_testScenarioC004c() throws Exception {
        if (isUsingJPA20Feature()) {
            return; // Not for OpenJPA
        }
        testScenarioC004(em_SCM_DISSEL, em_SCM_DISSEL_COPY);
    }

    /**
     * With shared-cache-modes of ALL|ENABLE_SELECTIVE|DISABLE_SELECTIVE, clear the contents of the cache.
     * Invoke em.find() for entity e1, and clear the persistence context. Using a different
     * persistence unit with caching turned off, mutate e1, so that the cached copy of e1 becomes stale.
     * Call em.find() with javax.persistence.cache.retrieveMode set to USE, and
     * verify that the mutation done by the second persistence unit is not visible. Clear the persistence context.
     * Call em.find() with javax.persistence.cache.retrieveMode set to BYPASS, and verify that the mutation done by
     * the second persistence unit IS VISIBLE.
     */
    private void testScenarioC004(EntityManager emanager, EntityManager emCopy) throws Exception {
        int id = 11;
        try {
            tx.begin();
            Cache cache = emanager.getEntityManagerFactory().getCache();
            if (cache != null) {
                cache.evictAll();
            }
            Employee emp = emanager.find(Employee.class, id);
            assertNotNull(emp);
            tx.commit();
            //Clear the persistence context so the entity is not managed by the EntityManager anymore
            emanager.clear();
            // If a Cache object is available, make sure that the above entity is cached in it.
            assertNotNull(cache);
            assertTrue("Employee Entity should be in the cache but is not:", cache.contains(Employee.class, id));
            assertFalse("Employee Entity should not be managed by the EM here: ", emanager.contains(emp));

            tx.begin();
            Employee employeeCopy = emCopy.find(Employee.class, id);
            String newName = "M";
            employeeCopy.setFirstName(newName);
            tx.commit();
            emCopy.clear();

            tx.begin();
            java.util.Map<String, Object> retrieveModeUseMap = new java.util.HashMap<String, Object>();
            retrieveModeUseMap.put("javax.persistence.cache.retrieveMode", CacheRetrieveMode.USE);
            Employee emp_rmUse = emanager.find(Employee.class, id, retrieveModeUseMap);
            emanager.clear();
            assertNotNull("Find() did not return an object", emp_rmUse);
            assertEquals("Find() should have returned Employee with id=(" + id + ")", id, emp_rmUse.getId());
            assertFalse("Entity should not be attached to the EntityManager here", emanager.contains(emp_rmUse));
            assertTrue("Entity should be in the cache, but is not", cache.contains(Employee.class, id));
            assertTrue("Entity has lost its pre-mutation state " +
                       "(firstName should == \"" + emp.getFirstName() + "\", is \"" + emp_rmUse.getFirstName() + "\"",
                       emp.getFirstName().equals(emp_rmUse.getFirstName()));

            java.util.Map<String, Object> retrieveModeBypassMap = new java.util.HashMap<String, Object>();
            retrieveModeBypassMap.put("javax.persistence.cache.retrieveMode", CacheRetrieveMode.BYPASS);
            Employee emp_rmBypass = emanager.find(Employee.class, id, retrieveModeBypassMap);

            emanager.clear();
            assertNotNull("Find() did not return an object", emp_rmBypass);
            assertEquals("Find() should have returned Employee with id=(" + id + ")", id, emp_rmBypass.getId());
            assertFalse("Entity is still attached to EntityManager", emanager.contains(emp_rmBypass));
            assertTrue("Entity should be in the cache but is not", cache.contains(Employee.class, id));
            assertTrue("Entity has lost its pre-mutation state " +
                       "(firstName should == \"" + newName + "\", is " + emp_rmBypass.getFirstName(),
                       newName.equals(emp_rmBypass.getFirstName()));

            tx.commit();
        } catch (Exception e) {
            try {
                tx.rollback();
            } catch (Throwable t) {
                // Swallow
            }
            throw (e);
        }
    }

    /**
     * Declaration: e1 is of entity type that is selected to be cached. Invoke em.find()
     * for entity e1 and clear the persistence context. Verify e1 is in the datacache. Begin a transaction, then call em.find()
     * for entity e1 with no properties (expecting default behavior javax.persistence.cache.storeMode == USE.)
     * Mutate e1 (creating e1') and commit the transaction. Call em.find() for entity e1 with javax.persistence.cache.retrieveMode set to USE
     * and verify that the mutation of e1' is returned from the cache.
     */
    @Test
    public void jpa_jpa20_cache_testScenarioC005() throws Exception {
        int id = 5;
        try {
            Cache cache = em_SCM_ALL.getEntityManagerFactory().getCache();
            if (cache != null) {
                cache.evictAll();
            }
            em_SCM_ALL.clear();
            tx.begin();

            // Call em.find() for entity e1 with no properties
            // (expecting default behavior javax.persistence.cache.storeMode == USE.)
            Employee emp = em_SCM_ALL.find(Employee.class, id);
            assertNotNull("Find() did not return an object", emp);
            assertEquals("Find() should have returned Employee with id=(" + id + ")", id, emp.getId());
            // If a Cache object is available, make sure that the above entity is cached in it.
            assertNotNull(cache);
            assertTrue("Entity should be in the cache but is not", cache.contains(Employee.class, id));

            // Mutate e1 (creating e1'), and commit Transaction t1.
            String newName = "M";
            emp.setFirstName(newName);
            tx.commit();
            em_SCM_ALL.clear();

            // Call em.find() for entity e1 with javax.persistence.cache.retrieveMode set to USE and verify that
            // the mutation of e1' is returned from the cache.
            tx.begin();
            java.util.Map<String, Object> retrieveModeUseMap = new java.util.HashMap<String, Object>();
            retrieveModeUseMap.put("javax.persistence.cache.retrieveMode", CacheRetrieveMode.USE);
            Employee emp_rmUse = em_SCM_ALL.find(Employee.class, id, retrieveModeUseMap);

            em_SCM_ALL.clear();
            assertNotNull("Find() did not return an object", emp_rmUse);
            assertEquals("Find() should have returned Employee with id=(" + id + ")", id, emp_rmUse.getId());
            assertFalse("Entity is still attached to EntityManager", em_SCM_ALL.contains(emp_rmUse));
            assertTrue("Entity should be in the cache but is not", cache.contains(Employee.class, id));
            assertTrue("Entity still has its pre-mutation state, should have changed " +
                       "(firstName should == '" + newName + "', is '" + emp_rmUse.getFirstName() + "'",
                       newName.equals(emp_rmUse.getFirstName()));

            tx.commit();
        } catch (Exception e) {
            try {
                tx.rollback();
            } catch (Throwable t) {
                // Swallow
            }
            throw (e);
        }
    }

    /**
     * CDeclaration: e1 is of entity type that is selected to be cached. Invoke em.find() for entity e1
     * and clear the persistence context, then verify it is in the cache. Begin transaction and call em.find() for entity e1 with
     * javax.persistence.cache.storeMode set to USE. Mutate e1 (creating e1') and commit transaction. Call em.find() for entity e1
     * with javax.persistence.cache.retrieveMode set to USE and verify that the mutation of e1' is returned from the cache.
     */
    @Test
    public void jpa_jpa20_cache_testScenarioC006() throws Exception {
        int id = 6;
        try {
            Cache cache = em_SCM_ALL.getEntityManagerFactory().getCache();
            if (cache != null) {
                cache.evictAll();
            }
            tx.begin();

            //  Call em.find() for entity e1 with javax.persistence.cache.storeMode set to USE.
            java.util.Map<String, Object> storeModeUseMap = new java.util.HashMap<String, Object>();
            storeModeUseMap.put("javax.persistence.cache.storeMode", CacheStoreMode.USE);
            Employee emp_smUse = em_SCM_ALL.find(Employee.class, id, storeModeUseMap);
            assertNotNull("Find() did not return an object", emp_smUse);
            assertEquals("Find() should have returned Employee(" + id + ")", id, emp_smUse.getId());
            // If a Cache object is available, make sure that the above entity is cached in it.
            assertNotNull(cache);
            assertTrue("Entity should be in the cache but is not", cache.contains(Employee.class, id));

            // Mutate e1 (creating e1'), and commit Transaction t1.
            String newName = "M";
            emp_smUse.setFirstName(newName);
            tx.commit();
            em_SCM_ALL.clear();

            tx.begin();
            cache = em_SCM_ALL.getEntityManagerFactory().getCache();

            // Call em.find() for entity e1 with javax.persistence.cache.retrieveMode set to USE and verify
            // that the mutation of e1' is returned from the cache.
            java.util.Map<String, Object> retrieveModeUseMap = new java.util.HashMap<String, Object>();
            retrieveModeUseMap.put("javax.persistence.cache.retrieveMode", CacheRetrieveMode.USE);
            Employee emp_rmUse = em_SCM_ALL.find(Employee.class, id, retrieveModeUseMap);

            em_SCM_ALL.clear();
            assertNotNull("Find() did not return an object", emp_rmUse);
            assertEquals("Find() did not return Employee", id, emp_rmUse.getId());
            assertFalse("Entity is still attached to EntityManager", em_SCM_ALL.contains(emp_rmUse));
            assertTrue("Entity should be in the cache but is not", cache.contains(Employee.class, id));
            assertTrue("Entity still has its pre-mutation state, should have changed " +
                       "(firstName should == \"" + newName + "\", is " + emp_rmUse.getFirstName(),
                       newName.equals(emp_rmUse.getFirstName()));

            tx.commit();
        } catch (Exception e) {
            try {
                tx.rollback();
            } catch (Throwable t) {
                // Swallow
            }
            throw (e);
        }
    }

    /**
     * Find entities e1 and f1 with em.find() where javax.persistence.cache.storeMode is set to REFRESH,
     * then verify their presence in the datacache. With a different persistence unit, and caching turned off, mutate e1 into e1', so that
     * the cached copy of e1 becomes stale. With the original persistance unit, mutate entity f1 into f1', and commit the transaction.
     * Clear the persistence context, and perform em.find() of entity e1 with javax.persistence.cache.retrieveMode
     * set to USE. Verify that the entity returned by the em.find() operation is e1' since the commitment of f1'
     * should have refreshed e1' in the cache automatically.
     */

    @Test
    public void jpa_jpa20_cache_testScenarioC008() throws Exception {
        int id = 8;
        int id2 = 9;
        try {
            tx.begin();
            Cache cache = em_SCM_ALL.getEntityManagerFactory().getCache();
            if (cache != null) {
                cache.evictAll();
            }

            //  Invoke em.find() for entity e1 and entity f1 with javax.persistence.cache.storeMode set to REFRESH
            //  and verify their presence in the datacache.
            java.util.Map<String, Object> storeModeRefreshMap = new java.util.HashMap<String, Object>();
            storeModeRefreshMap.put("javax.persistence.cache.storeMode", CacheStoreMode.REFRESH);
            Employee emp_smRefresh = em_SCM_ALL.find(Employee.class, id, storeModeRefreshMap);
            assertNotNull("Find() did not return an object", emp_smRefresh);
            assertEquals("Find() should have returned Employee with id=(" + id + ")", id, emp_smRefresh.getId());
            // If a Cache object is available, make sure that the above entity is cached in it.
            assertNotNull(cache);
            assertTrue("Entity should be in the cache but is not", cache.contains(Employee.class, id));
            assertTrue("Entity has lost its pre-mutation state " +
                       "(firstName should == \"Brandon\", is " + emp_smRefresh.getFirstName(),
                       "Brandon".equals(emp_smRefresh.getFirstName()));

            Employee emp2_smRefresh = em_SCM_ALL.find(Employee.class, id2, storeModeRefreshMap);
            assertNotNull("Find() did not return an object", emp2_smRefresh);
            assertEquals("Find() did not return Employee(id=" + id2 + ")", id2, emp2_smRefresh.getId());
            assertTrue("Entity should be in the cache but is not", cache.contains(Employee.class, id2));
            assertTrue("Entity has lost its pre-mutation state " +
                       "(firstName should == \"Kevin\", is " + emp2_smRefresh.getFirstName(),
                       "Kevin".equals(emp2_smRefresh.getFirstName()));
            tx.commit();
            tx.begin();

            //Use a different persistence unit with caching turned off to mutate e1 into e1'. This will result in the cached copy of e1 becoming stale
            Employee empCopy = em_SCM_NONE.find(Employee.class, id);
            String newName1 = "M";
            empCopy.setFirstName(newName1);
            // Mutate entity f1 into f1', and commit the transaction.
            String newName2 = "J";
            emp2_smRefresh.setFirstName(newName2);
            tx.commit();
            //  Verify that the entity returned by the em.find() operation is e1' since the commitment of f1'
            // should have refreshed e1' in the cache automatically.
            em_SCM_ALL.clear();
            tx.begin();

            // Call em.find() for entity e1 with javax.persistence.cache.retrieveMode set to USE and verify
            // that the mutation of e1' is returned from the cache
            Employee emp_rmUse = em_SCM_ALL.find(Employee.class, id, storeModeRefreshMap);

            em_SCM_ALL.clear();
            assertNotNull("Find() did not return an object", emp_rmUse);
            assertEquals("Find() should have returned Employee with id=(" + id + ")", id, emp_rmUse.getId());
            assertFalse("Entity is still attached to EntityManager", em_SCM_ALL.contains(emp_rmUse));
            assertTrue("Entity should be in the cache but is not", cache.contains(Employee.class, id));
            assertTrue("Entity still has its pre-mutation state, should have changed " +
                       "(firstName should == \"" + newName1 + "\", is " + emp_rmUse.getFirstName() + "\"",
                       newName1.equals(emp_rmUse.getFirstName()));

            tx.commit();
        } catch (Exception e) {
            try {
                tx.rollback();
            } catch (Throwable t) {
                // Swallow
            }
            throw (e);
        }
    }

    /**
     * While using an EntityManager instantiated with the property javax.persistence.cache.retrieveMode set
     * to BYPASS, clear the contents of the cache.. Invoke em.find() for entity e1, and clear
     * the persistence context. Verify that the entity e1 is not in the datacache..
     * Call em.find() with javax.persistence.cache.retrieveMode set to USE, and verify that the
     * entity e1 is in the datacache.
     */
    @Test
    public void jpa_jpa20_cache_testScenarioC012() throws Exception {
        int id = 12;
        try {
            tx.begin();
            em_SCM_ALL.clear();

            // Evict the contents of the cache (fail if the Cache object is unobtainable)
            Cache cache = em_SCM_ALL.getEntityManagerFactory().getCache();
            if (cache != null) {
                cache.evictAll();
            }

            // Invoke em.find() for entity e1, and clear the persistence context.
            // Verify that the entity e1 is not in the datacache.
            java.util.Map<String, Object> storeModeBypassMap = new java.util.HashMap<String, Object>();
            storeModeBypassMap.put("javax.persistence.cache.storeMode", CacheStoreMode.BYPASS);
            Employee emp = em_SCM_ALL.find(Employee.class, id, storeModeBypassMap);
            em_SCM_ALL.clear();
            assertNotNull("Find() did not return an object", emp);
            assertEquals(id, emp.getId());
            assertFalse("Entity is still attached to EntityManager", em_SCM_ALL.contains(emp));
            // If a Cache object is available, make sure that the above entity is cached in it.
            assertNotNull(cache);
            assertFalse("Entity should not be in the cache here.", cache.contains(Employee.class, id));

            // Call em.find() with javax.persistence.cache.retrieveMode set to USE,
            // and verify that the mutation of e1' is returned from the cache
            java.util.Map<String, Object> retrieveModeUseMap = new java.util.HashMap<String, Object>();
            retrieveModeUseMap.put("javax.persistence.cache.retrieveMode", CacheRetrieveMode.USE);
            Employee emp_rmUse = em_SCM_ALL.find(Employee.class, id, retrieveModeUseMap);

            em_SCM_ALL.clear();
            assertNotNull("Find() did not return an object", emp_rmUse);
            assertEquals("Find() should have returned Employee with id=(" + id + ")", id, emp_rmUse.getId());
            assertFalse("Entity is still attached to EntityManager", em_SCM_ALL.contains(emp_rmUse));
            assertTrue("Entity should be in the cache but is not", cache.contains(Employee.class, id));
            assertTrue("Entity has lost its pre-mutation state " +
                       "(firstName should == " + emp.getFirstName() + ", is " + emp_rmUse.getFirstName(),
                       emp.getFirstName().equals(emp_rmUse.getFirstName()));
            tx.commit();
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
