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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.servlet.annotation.WebServlet;
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

        tx.begin();
        SimpleEmployee found = em.find(SimpleEmployee.class, emp.getId());
        assertNotNull("entity should exist after persist", found);
        assertEquals("Alice", found.getName());
        assertEquals(80_000L, found.getSalary());
        tx.commit();

        cleanup(SimpleEmployee.class);
    }

    /** Update a field and verify the change is visible in a fresh find. */
    @Test
    public void testUpdateEmployee() throws Exception {
        tx.begin();
        SimpleEmployee emp = new SimpleEmployee("Bob", 50_000L);
        em.persist(emp);
        tx.commit();

        Long id = emp.getId();

        tx.begin();
        SimpleEmployee managed = em.find(SimpleEmployee.class, id);
        managed.setSalary(65_000L);
        tx.commit();

        tx.begin();
        SimpleEmployee reloaded = em.find(SimpleEmployee.class, id);
        assertEquals(65_000L, reloaded.getSalary());
        tx.commit();

        cleanup(SimpleEmployee.class);
    }

    /** Remove an entity and verify it is gone. */
    @Test
    public void testRemoveEmployee() throws Exception {
        tx.begin();
        SimpleEmployee emp = new SimpleEmployee("Carol", 40_000L);
        em.persist(emp);
        tx.commit();

        Long id = emp.getId();

        tx.begin();
        SimpleEmployee managed = em.find(SimpleEmployee.class, id);
        em.remove(managed);
        tx.commit();

        tx.begin();
        SimpleEmployee gone = em.find(SimpleEmployee.class, id);
        assertNull("entity should be gone after remove", gone);
        tx.commit();

        cleanup(SimpleEmployee.class);
    }

    /** JPQL query with a named parameter. */
    @Test
    public void testJPQLQueryEmployee() throws Exception {
        tx.begin();
        em.persist(new SimpleEmployee("Dave",   90_000L));
        em.persist(new SimpleEmployee("Eve",   120_000L));
        em.persist(new SimpleEmployee("Frank",  55_000L));
        tx.commit();

        tx.begin();
        TypedQuery<SimpleEmployee> q = em.createQuery(
            "SELECT e FROM SimpleEmployee e WHERE e.salary > :minSalary",
            SimpleEmployee.class);
        q.setParameter("minSalary", 80_000L);
        List<SimpleEmployee> results = q.getResultList();
        tx.commit();

        assertEquals("expected two employees earning > 80k", 2, results.size());

        cleanup(SimpleEmployee.class);
    }

    /** JPQL aggregate — count all employees. */
    @Test
    public void testCountEmployees() throws Exception {
        tx.begin();
        em.persist(new SimpleEmployee("G1", 1L));
        em.persist(new SimpleEmployee("G2", 2L));
        tx.commit();

        tx.begin();
        Long count = em.createQuery("SELECT COUNT(e) FROM SimpleEmployee e", Long.class)
                       .getSingleResult();
        tx.commit();

        assertTrue("expected at least 2 employees", count >= 2L);

        cleanup(SimpleEmployee.class);
    }

    // -----------------------------------------------------------------------
    // Product — optimistic locking (@Version)
    // -----------------------------------------------------------------------

    /** Persist a Product and verify the version starts at 0. */
    @Test
    public void testPersistProduct() throws Exception {
        tx.begin();
        Product p = new Product("Widget", 9.99);
        em.persist(p);
        tx.commit();

        assertNotNull(p.getId());
        assertEquals("initial version must be 0", 0L, p.getVersion());

        cleanup(Product.class);
    }

    /** Update a Product and verify @Version is incremented by the provider. */
    @Test
    public void testOptimisticLockVersionIncrement() throws Exception {
        tx.begin();
        Product p = new Product("Gadget", 49.99);
        em.persist(p);
        tx.commit();

        Long id = p.getId();

        tx.begin();
        Product managed = em.find(Product.class, id);
        managed.setPrice(54.99);
        tx.commit();

        tx.begin();
        Product updated = em.find(Product.class, id);
        assertEquals("version should have been incremented to 1", 1L, updated.getVersion());
        assertEquals(54.99, updated.getPrice(), 0.001);
        tx.commit();

        cleanup(Product.class);
    }

    /** JPQL — find all products below a price threshold. */
    @Test
    public void testQueryProductsByPrice() throws Exception {
        tx.begin();
        em.persist(new Product("Cheap",     4.99));
        em.persist(new Product("Medium",   24.99));
        em.persist(new Product("Expensive",99.99));
        tx.commit();

        tx.begin();
        List<Product> cheap = em.createQuery(
            "SELECT p FROM Product p WHERE p.price < :max ORDER BY p.price",
            Product.class)
            .setParameter("max", 10.0)
            .getResultList();
        tx.commit();

        assertEquals(1, cheap.size());
        assertEquals("Cheap", cheap.get(0).getName());

        cleanup(Product.class);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void cleanup(Class<?> entityClass) throws Exception {
        tx.begin();
        em.createQuery("DELETE FROM " + entityClass.getSimpleName() + " e").executeUpdate();
        tx.commit();
    }
}
