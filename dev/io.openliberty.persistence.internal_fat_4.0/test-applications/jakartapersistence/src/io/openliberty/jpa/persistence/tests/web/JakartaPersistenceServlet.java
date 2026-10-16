/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.persistence.tests.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Statement;
import jakarta.persistence.TypedQuery;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.jpa.persistence.tests.models.Product;
import io.openliberty.jpa.persistence.tests.models.SimpleEmployee;

/**
 * Basic smoke-tests for JPA 4.0 / Hibernate 8 on Liberty using
 * {@code persistenceContainer-4.0}.
 *
 * Each test manages its own transaction via {@link UserTransaction} so the
 * tests are fully independent and ordering does not matter.
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JakartaPersistence40")
public class JakartaPersistenceServlet extends FATServlet {

    @PersistenceContext(unitName = "JakartaPersistenceUnit")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    // -----------------------------------------------------------------------
    // SimpleEmployee — basic CRUD
    // -----------------------------------------------------------------------

    /** Persist a SimpleEmployee and find it by primary key. */
    @Test
    public void testPersistAndFindEmployee() throws Exception {
        tx.begin();
        SimpleEmployee emp = new SimpleEmployee("Alice", 80_000L);
        em.persist(emp);
        tx.commit();

        assertNotNull("id should have been assigned", emp.getId());

        SimpleEmployee found = em.find(SimpleEmployee.class, emp.getId());
        assertNotNull("entity should exist after persist", found);
        assertEquals("Alice", found.getName());
        assertEquals(80_000L, found.getSalary());
    }

    /** Update a field and verify the change is visible in a fresh find. */
    @Test
    public void testUpdateEmployee() throws Exception {
        cleanup(SimpleEmployee.class);
        tx.begin();
        SimpleEmployee emp = new SimpleEmployee("Bob", 50_000L);
        em.persist(emp);
        tx.commit();

        Long id = emp.getId();

        tx.begin();
        SimpleEmployee managed = em.find(SimpleEmployee.class, id);
        managed.setSalary(65_000L);
        tx.commit();

        SimpleEmployee reloaded = em.find(SimpleEmployee.class, id);
        assertEquals(65_000L, reloaded.getSalary());
    }

    /** Remove an entity and verify it is gone. */
    @Test
    public void testRemoveEmployee() throws Exception {
        cleanup(SimpleEmployee.class);
        tx.begin();
        SimpleEmployee emp = new SimpleEmployee("Carol", 40_000L);
        em.persist(emp);
        tx.commit();

        Long id = emp.getId();

        tx.begin();
        SimpleEmployee managed = em.find(SimpleEmployee.class, id);
        em.remove(managed);
        tx.commit();

        SimpleEmployee gone = em.find(SimpleEmployee.class, id);
        assertNull("entity should be gone after remove", gone);
    }

    /** JPQL query with a named parameter. */
    @Test
    public void testJPQLQueryEmployee() throws Exception {
        cleanup(SimpleEmployee.class);
        tx.begin();
        em.persist(new SimpleEmployee("Dave",   90_000L));
        em.persist(new SimpleEmployee("Eve",   120_000L));
        em.persist(new SimpleEmployee("Frank",  55_000L));
        tx.commit();

        TypedQuery<SimpleEmployee> q = em.createQuery(
            "SELECT e FROM SimpleEmployee e WHERE e.salary > :minSalary",
            SimpleEmployee.class);
        q.setParameter("minSalary", 80_000L);
        List<SimpleEmployee> results = q.getResultList();

        assertEquals("expected two employees earning > 80k", 2, results.size());
    }

    /** JPQL aggregate — count all employees. */
    @Test
    public void testCountEmployees() throws Exception {
        cleanup(SimpleEmployee.class);
        tx.begin();
        em.persist(new SimpleEmployee("G1", 1L));
        em.persist(new SimpleEmployee("G2", 2L));
        tx.commit();

        Long count = em.createQuery("SELECT COUNT(e) FROM SimpleEmployee e", Long.class)
                       .getSingleResult();

        assertTrue("expected at least 2 employees", count >= 2L);
    }

    // -----------------------------------------------------------------------
    // Product — optimistic locking (@Version)
    // -----------------------------------------------------------------------

    /** Persist a Product and verify the version starts at 0. */
    @Test
    public void testPersistProduct() throws Exception {
        cleanup(Product.class);
        tx.begin();
        Product p = new Product("Widget", 9.99);
        em.persist(p);
        tx.commit();

        assertNotNull(p.getId());
        assertEquals("initial version must be 0", 0L, p.getVersion());
    }

    /** Update a Product and verify @Version is incremented by the provider. */
    @Test
    public void testOptimisticLockVersionIncrement() throws Exception {
        cleanup(Product.class);
        tx.begin();
        Product p = new Product("Gadget", 49.99);
        em.persist(p);
        tx.commit();

        Long id = p.getId();

        tx.begin();
        Product managed = em.find(Product.class, id);
        managed.setPrice(54.99);
        tx.commit();

        Product updated = em.find(Product.class, id);
        assertEquals("version should have been incremented to 1", 1L, updated.getVersion());
        assertEquals(54.99, updated.getPrice(), 0.001);
    }

    /** JPQL — find all products below a price threshold. */
    @Test
    public void testQueryProductsByPrice() throws Exception {
        cleanup(Product.class);
        tx.begin();
        em.persist(new Product("Cheap",     4.99));
        em.persist(new Product("Medium",   24.99));
        em.persist(new Product("Expensive",99.99));
        tx.commit();

        List<Product> cheap = em.createQuery(
            "SELECT p FROM Product p WHERE p.price < :max ORDER BY p.price",
            Product.class)
            .setParameter("max", 10.0)
            .getResultList();

        assertEquals(1, cheap.size());
        assertEquals("Cheap", cheap.get(0).getName());
    }

    // -----------------------------------------------------------------------
    // JPA 4.0 — get() / find() semantics
    // -----------------------------------------------------------------------

    /**
     * get() returns the entity when it exists — same result as find().
     */
    @Test
    public void testGetReturnsEntityWhenPresent() throws Exception {
        cleanup(SimpleEmployee.class);
        SimpleEmployee emp1 = new SimpleEmployee("Alby", 45_000L);
        SimpleEmployee emp2 = new SimpleEmployee("Bruno", 70_000L);
        tx.begin();
        em.persist(emp1);
        em.persist(emp2);
        tx.commit();

        tx.begin();
        SimpleEmployee found = em.get(SimpleEmployee.class, emp2.getId());
        tx.commit();
        assertNotNull("get() must return a non-null entity for an existing id", found);
        assertEquals("Bruno", found.getName());
        assertEquals(70_000L, found.getSalary());
    }

    /**
     * get() throws EntityNotFoundException when the entity does not exist,
     * whereas find() would return null.
     */
    @Test
    public void testGetAndFindForMissingId() throws Exception {
        cleanup(SimpleEmployee.class);
        SimpleEmployee emp = new SimpleEmployee("Antony", 50_000L);
        tx.begin();
        em.persist(emp);
        tx.commit();

        Long nonExistentId = Long.MAX_VALUE;

        tx.begin();
        SimpleEmployee notFound = em.find(SimpleEmployee.class, nonExistentId);
        tx.commit();
        assertNull("find() must return null for a missing entity", notFound);

        tx.begin();
        try {
            em.get(SimpleEmployee.class, nonExistentId);
            fail("get() must throw EntityNotFoundException for a missing entity");
        } catch (EntityNotFoundException e) {
            // expected
        } finally {
            if (tx.getStatus() == Status.STATUS_ACTIVE)
                tx.rollback();
        }
    }

    // -----------------------------------------------------------------------
    // JPA 4.0 — findMultiple()
    // -----------------------------------------------------------------------

    /**
     * findMultiple() returns one entity per id in the same order.
     * Missing ids produce null slots in the list.
     */
    @Test
    public void testFindMultipleAllPresent() throws Exception {
        cleanup(SimpleEmployee.class);
        SimpleEmployee e1 = new SimpleEmployee("Thomas", 10_000L);
        SimpleEmployee e2 = new SimpleEmployee("Edwin", 20_000L);
        SimpleEmployee e3 = new SimpleEmployee("Margaret", 30_000L);
        tx.begin();
        em.persist(e1);
        em.persist(e2);
        em.persist(e3);
        tx.commit();

        tx.begin();
        List<SimpleEmployee> results = em.findMultiple(SimpleEmployee.class,
                Arrays.asList(e1.getId(), e2.getId(), e3.getId()));
        tx.commit();

        assertEquals("findMultiple() must return exactly 3 entries", 3, results.size());
        assertNotNull("slot 0 must be non-null", results.get(0));
        assertNotNull("slot 1 must be non-null", results.get(1));
        assertNotNull("slot 2 must be non-null", results.get(2));
        assertEquals("Thomas", results.get(0).getName());
        assertEquals("Edwin", results.get(1).getName());
        assertEquals("Margaret", results.get(2).getName());
    }

    /**
     * findMultiple() places null in the result list for an id that does not exist.
     */
    @Test
    public void testFindMultipleSomeMissing() throws Exception {
        cleanup(SimpleEmployee.class);
        tx.begin();
        SimpleEmployee e1 = new SimpleEmployee("Leonard", 11_000L);
        SimpleEmployee e2 = new SimpleEmployee("Patricia", 22_000L);
        em.persist(e1);
        em.persist(e2);
        tx.commit();

        Long missingId = Long.MAX_VALUE;

        tx.begin();
        List<SimpleEmployee> results = em.findMultiple(SimpleEmployee.class,
                Arrays.asList(e1.getId(), missingId, e2.getId()));
        tx.commit();

        assertEquals("findMultiple() must return 3 slots (one per id)", 3, results.size());
        assertNotNull("slot 0 (present) must be non-null", results.get(0));
        assertNull("slot 1 (missing) must be null", results.get(1));
        assertNotNull("slot 2 (present) must be non-null", results.get(2));
    }

    /**
     * findMultiple() with an empty id list returns an empty list, not null.
     */
    @Test
    public void testFindMultipleEmptyList() throws Exception {
        cleanup(SimpleEmployee.class);
        tx.begin();
        em.persist(new SimpleEmployee("Walter", 15_000L));
        em.persist(new SimpleEmployee("Harriet", 25_000L));
        tx.commit();

        tx.begin();
        List<SimpleEmployee> results = em.findMultiple(SimpleEmployee.class, List.of());
        tx.commit();

        assertNotNull("findMultiple() with empty input must return an empty list, not null", results);
        assertTrue("result list must be empty", results.isEmpty());
    }

    // -----------------------------------------------------------------------
    // JPA 4.0 — getMultiple()
    // -----------------------------------------------------------------------

    /**
     * getMultiple() returns the full list when all requested ids exist.
     */
    @Test
    public void testGetMultipleAllPresent() throws Exception {
        cleanup(SimpleEmployee.class);
        tx.begin();
        SimpleEmployee e1 = new SimpleEmployee("Raymond", 1_000L);
        SimpleEmployee e2 = new SimpleEmployee("Dorothy", 2_000L);
        em.persist(e1);
        em.persist(e2);
        tx.commit();

        tx.begin();
        List<SimpleEmployee> results = em.getMultiple(SimpleEmployee.class,
                Arrays.asList(e1.getId(), e2.getId()));
        tx.commit();

        assertEquals("getMultiple() must return 2 entries", 2, results.size());
        assertFalse("result list must not contain null entries", results.contains(null));
        assertEquals("Raymond", results.get(0).getName());
        assertEquals("Dorothy", results.get(1).getName());
    }

    /**
     * getMultiple() throws {@link EntityNotFoundException} when any requested id
     * is not found, unlike findMultiple() which places null in the slot.
     */
    @Test
    public void testGetMultipleThrowsForMissingId() throws Exception {
        cleanup(SimpleEmployee.class);
        SimpleEmployee e1 = new SimpleEmployee("Bernard", 5_000L);
        tx.begin();
        em.persist(e1);
        tx.commit();

        Long missingId = Long.MAX_VALUE;

        tx.begin();
        try {
            em.getMultiple(SimpleEmployee.class, Arrays.asList(e1.getId(), missingId));
            fail("getMultiple() must throw EntityNotFoundException when any id is missing");
        } catch (EntityNotFoundException e) {
            // expected
        } finally {
            if (tx.getStatus() == Status.STATUS_ACTIVE)
                tx.rollback();
        }
    }

    /**
     * getMultiple() with an empty id list returns an empty list, not null —
     * even when matching rows exist in the database.
     */
    @Test
    public void testGetMultipleEmptyList() throws Exception {
        cleanup(SimpleEmployee.class);
        tx.begin();
        em.persist(new SimpleEmployee("Clarence", 9_000L));
        em.persist(new SimpleEmployee("Mildred", 8_000L));
        tx.commit();

        tx.begin();
        List<SimpleEmployee> results = em.getMultiple(SimpleEmployee.class, List.of());
        tx.commit();

        assertNotNull("getMultiple() with empty input must return an empty list, not null", results);
        assertTrue("result list must be empty", results.isEmpty());
    }

    // -----------------------------------------------------------------------
    // JPA 4.0 — TypedQuery.getResultCount()
    // -----------------------------------------------------------------------

    @Test
    public void testGetResultCount() throws Exception {
        cleanup(Product.class);
        tx.begin();
        em.persist(new Product("Laptop", 1200.00));
        em.persist(new Product("Wireless Mouse", 25.50));
        em.persist(new Product("Mechanical Keyboard", 85.00));
        em.persist(new Product("4K Monitor", 350.00));
        tx.commit();

        TypedQuery<Product> allProductsQuery = em.createQuery("SELECT p FROM Product p", Product.class);
        long totalCount = allProductsQuery.getResultCount();
        assertEquals("getResultCount() should return 4 for the total number of products", 4L, totalCount);

        TypedQuery<Product> expensiveProductsQuery = em.createQuery("SELECT p FROM Product p WHERE p.price > 100.0", Product.class);
        long expensiveCount = expensiveProductsQuery.getResultCount();
        assertEquals("getResultCount() should return 2 for products priced over 100.0", 2L, expensiveCount);
    }

    @Test
    public void testQueryGetSingleResultOrNull() throws Exception {
        cleanup(SimpleEmployee.class);
        tx.begin();
        SimpleEmployee emp = new SimpleEmployee("UniqueEmployee", 95_000L);
        em.persist(emp);
        tx.commit();

        // 1. Found case -> returns entity
        SimpleEmployee found = em.createQuery(
            "SELECT e FROM SimpleEmployee e WHERE e.name = :name", SimpleEmployee.class)
            .setParameter("name", "UniqueEmployee")
            .getSingleResultOrNull();
        assertNotNull("expected entity to be found", found);
        assertEquals("UniqueEmployee", found.getName());

        // 2. Not found case -> returns null instead of NoResultException
        SimpleEmployee notFound = em.createQuery(
            "SELECT e FROM SimpleEmployee e WHERE e.name = :name", SimpleEmployee.class)
            .setParameter("name", "NonExistentEmployee")
            .getSingleResultOrNull();
        assertNull("expected null when no match found", notFound);
    }

    @Test
    public void testPersistenceUnitUtilAndEntityState() throws Exception {
        cleanup(SimpleEmployee.class);
        tx.begin();
        SimpleEmployee emp = new SimpleEmployee("StateTestEmp", 60_000L);
        em.persist(emp);
        assertTrue("entity should be managed during active transaction", em.contains(emp));
        tx.commit();

        Long id = emp.getId();

        // PersistenceUnitUtil inspection works on detached or managed entities
        Object identifier = em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(emp);
        assertEquals(id, identifier);
        assertTrue(em.getEntityManagerFactory().getPersistenceUnitUtil().isLoaded(emp));

        tx.begin();
        SimpleEmployee managed = em.find(SimpleEmployee.class, id);
        assertTrue("entity should be managed after find", em.contains(managed));
        em.detach(managed);
        assertFalse("entity should not be managed after detach", em.contains(managed));
        tx.commit();
    }

    @Test
    public void testBulkUpdateAndClear() throws Exception {
        cleanup(SimpleEmployee.class);
        tx.begin();
        em.persist(new SimpleEmployee("EmpA", 40_000L));
        em.persist(new SimpleEmployee("EmpB", 45_000L));
        tx.commit();

        tx.begin();
        int updatedCount = em.createQuery(
            "UPDATE SimpleEmployee e SET e.salary = e.salary + 5000 WHERE e.salary < :threshold")
            .setParameter("threshold", 50_000L)
            .executeUpdate();
        assertEquals("expected 2 employees updated", 2, updatedCount);
        tx.commit();

        em.clear();

        tx.begin();
        List<SimpleEmployee> updatedList = em.createQuery(
            "SELECT e FROM SimpleEmployee e ORDER BY e.name", SimpleEmployee.class)
            .getResultList();
        assertEquals(45_000L, updatedList.get(0).getSalary());
        assertEquals(50_000L, updatedList.get(1).getSalary());
        tx.commit();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void cleanup(Class<?> entityClass) throws Exception {
        tx.begin();
        Statement stmt = em.createStatement("DELETE FROM " + entityClass.getSimpleName() + " e");
        stmt.execute();
        tx.commit();
    }
}
