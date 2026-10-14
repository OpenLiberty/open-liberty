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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityListener;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.PrePersist;
import jakarta.persistence.TypedQuery;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.jpa.persistence.tests.models.AuditListener;
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

        tx.begin();
        SimpleEmployee gone = em.find(SimpleEmployee.class, id);
        assertNull("entity should be gone after remove", gone);
        tx.commit();
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

        tx.begin();
        TypedQuery<SimpleEmployee> q = em.createQuery(
            "SELECT e FROM SimpleEmployee e WHERE e.salary > :minSalary",
            SimpleEmployee.class);
        q.setParameter("minSalary", 80_000L);
        List<SimpleEmployee> results = q.getResultList();
        tx.commit();

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

        tx.begin();
        Long count = em.createQuery("SELECT COUNT(e) FROM SimpleEmployee e", Long.class)
                       .getSingleResult();
        tx.commit();

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

        tx.begin();
        Product updated = em.find(Product.class, id);
        assertEquals("version should have been incremented to 1", 1L, updated.getVersion());
        assertEquals(54.99, updated.getPrice(), 0.001);
        tx.commit();
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

        tx.begin();
        List<Product> cheap = em.createQuery(
            "SELECT p FROM Product p WHERE p.price < :max ORDER BY p.price",
            Product.class)
            .setParameter("max", 10.0)
            .getResultList();
        tx.commit();

        assertEquals(1, cheap.size());
        assertEquals("Cheap", cheap.get(0).getName());
    }

    // -----------------------------------------------------------------------
    // JPA 4.0 — TypedQuery.getResultCount()
    // -----------------------------------------------------------------------

    /** Verify getResultCount() returns total count without and with WHERE clause. */
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

        TypedQuery<Product> expensiveProductsQuery = em.createQuery(
            "SELECT p FROM Product p WHERE p.price > 100.0", Product.class);
        long expensiveCount = expensiveProductsQuery.getResultCount();
        assertEquals("getResultCount() should return 2 for products priced over 100.0", 2L, expensiveCount);
    }

    /** Verify getResultCount() with named parameters and when no records match. */
    @Test
    public void testGetResultCountWithParameters() throws Exception {
        cleanup(SimpleEmployee.class);
        tx.begin();
        em.persist(new SimpleEmployee("Alice", 80_000L));
        em.persist(new SimpleEmployee("Bob", 50_000L));
        em.persist(new SimpleEmployee("Carol", 120_000L));
        em.persist(new SimpleEmployee("David", 95_000L));
        tx.commit();

        TypedQuery<SimpleEmployee> query = em.createQuery(
            "SELECT e FROM SimpleEmployee e WHERE e.salary >= :minSalary", SimpleEmployee.class);
        query.setParameter("minSalary", 80_000L);
        long count = query.getResultCount();
        assertEquals("getResultCount() should return 3 for employees with salary >= 80000", 3L, count);

        // When no rows match
        query.setParameter("minSalary", 200_000L);
        long zeroCount = query.getResultCount();
        assertEquals("getResultCount() should return 0 when no rows match", 0L, zeroCount);
    }

    /** Verify getResultCount() returns total count even when pagination (firstResult/maxResults) is set. */
    @Test
    public void testGetResultCountWithPagination() throws Exception {
        cleanup(Product.class);
        tx.begin();
        em.persist(new Product("Item 1", 10.0));
        em.persist(new Product("Item 2", 20.0));
        em.persist(new Product("Item 3", 30.0));
        em.persist(new Product("Item 4", 40.0));
        em.persist(new Product("Item 5", 50.0));
        tx.commit();

        TypedQuery<Product> paginatedQuery = em.createQuery(
            "SELECT p FROM Product p ORDER BY p.price", Product.class);
        paginatedQuery.setFirstResult(1);
        paginatedQuery.setMaxResults(2);

        // getResultList() returns page size of 2
        List<Product> page = paginatedQuery.getResultList();
        assertEquals("getResultList() should return paginated results size", 2, page.size());

        // getResultCount() returns total count (5) regardless of pagination
        long totalCount = paginatedQuery.getResultCount();
        assertEquals("getResultCount() should return total count of 5 regardless of pagination", 5L, totalCount);
    }

    // -----------------------------------------------------------------------
    // JPA 4.0 — PessimisticLockScope.FETCHED
    // -----------------------------------------------------------------------

    /** Verify PessimisticLockScope enum values and valueOf for FETCHED. */
    @Test
    public void testPessimisticLockScopeFetchedEnum() throws Exception {
        assertNotNull("PessimisticLockScope.FETCHED must be defined", PessimisticLockScope.FETCHED);
        assertEquals(PessimisticLockScope.FETCHED, PessimisticLockScope.valueOf("FETCHED"));
        assertEquals("FETCHED", PessimisticLockScope.FETCHED.name());

        List<PessimisticLockScope> scopes = Arrays.asList(PessimisticLockScope.values());
        assertTrue("PessimisticLockScope must contain NORMAL", scopes.contains(PessimisticLockScope.NORMAL));
        assertTrue("PessimisticLockScope must contain EXTENDED", scopes.contains(PessimisticLockScope.EXTENDED));
        assertTrue("PessimisticLockScope must contain FETCHED", scopes.contains(PessimisticLockScope.FETCHED));
    }

    /** Verify setting jakarta.persistence.lock.scope hint on TypedQuery with PessimisticLockScope.FETCHED and String "FETCHED". */
    @Test
    public void testQuerySetHintLockScopeFetched() throws Exception {
        TypedQuery<Product> query = em.createQuery("SELECT p FROM Product p", Product.class);

        // Test fluent setHint with PessimisticLockScope.FETCHED enum
        TypedQuery<Product> returnedQuery = query.setHint("jakarta.persistence.lock.scope", PessimisticLockScope.FETCHED);
        assertNotNull("setHint with PessimisticLockScope.FETCHED should return non-null query", returnedQuery);

        // Test fluent setHint with String "FETCHED"
        returnedQuery = query.setHint("jakarta.persistence.lock.scope", "FETCHED");
        assertNotNull("setHint with String FETCHED should return non-null query", returnedQuery);
    }

    /** Verify setting jakarta.persistence.lock.scope property on EntityManager with PessimisticLockScope.FETCHED. */
    @Test
    public void testEntityManagerPropertyLockScopeFetched() throws Exception {
        em.setProperty("jakarta.persistence.lock.scope", PessimisticLockScope.FETCHED);
        Map<String, Object> props = em.getProperties();
        assertNotNull("EntityManager properties must not be null", props);

        em.setProperty("jakarta.persistence.lock.scope", "FETCHED");
        assertNotNull("EntityManager properties must not be null after setting String property", em.getProperties());
    }

    /** Verify passing PessimisticLockScope.FETCHED in properties map to EntityManager.find. */
    @Test
    public void testFindWithLockScopeProperty() throws Exception {
        cleanup(Product.class);
        tx.begin();
        Product p = new Product("Tablet", 399.99);
        em.persist(p);
        tx.commit();

        Long id = p.getId();

        tx.begin();
        Map<String, Object> props = Collections.singletonMap(
            "jakarta.persistence.lock.scope", PessimisticLockScope.FETCHED);
        Product found = em.find(Product.class, id, props);
        assertNotNull("entity should be found successfully with lock scope property", found);
        assertEquals("Tablet", found.getName());
        tx.commit();
    }

    // -----------------------------------------------------------------------
    // JPA 4.0 — lockScope on @NamedQuery and setLockScope() on TypedQuery
    // -----------------------------------------------------------------------

    /** Verify TypedQuery.setLockScope() and TypedQuery.getLockScope() with PessimisticLockScope. */
    @Test
    public void testTypedQuerySetAndGetLockScope() throws Exception {
        TypedQuery<Product> query = em.createQuery("SELECT p FROM Product p", Product.class);

        // Set lockScope to FETCHED
        query.setLockScope(PessimisticLockScope.FETCHED);
        assertEquals("getLockScope() should return FETCHED", PessimisticLockScope.FETCHED, query.getLockScope());

        // Set lockScope to EXTENDED
        query.setLockScope(PessimisticLockScope.EXTENDED);
        assertEquals("getLockScope() should return EXTENDED", PessimisticLockScope.EXTENDED, query.getLockScope());

        // Set lockScope to NORMAL
        query.setLockScope(PessimisticLockScope.NORMAL);
        assertEquals("getLockScope() should return NORMAL", PessimisticLockScope.NORMAL, query.getLockScope());
    }

    /** Verify @NamedQuery configured with lockScope = PessimisticLockScope.FETCHED. */
    @Test
    public void testNamedQueryWithLockScopeFetched() throws Exception {
        cleanup(Product.class);
        tx.begin();
        em.persist(new Product("Alpha", 10.0));
        em.persist(new Product("Beta", 20.0));
        tx.commit();

        TypedQuery<Product> namedQuery = em.createNamedQuery("Product.findAllWithLockScope", Product.class);
        assertEquals("NamedQuery lockScope should be FETCHED", PessimisticLockScope.FETCHED, namedQuery.getLockScope());

        List<Product> results = namedQuery.getResultList();
        assertEquals(2, results.size());
        assertEquals("Alpha", results.get(0).getName());
    }

    /** Verify @NamedQuery configured with lockScope = PessimisticLockScope.EXTENDED. */
    @Test
    public void testNamedQueryWithLockScopeExtended() throws Exception {
        cleanup(Product.class);
        tx.begin();
        em.persist(new Product("SpecialProduct", 99.0));
        tx.commit();

        TypedQuery<Product> namedQuery = em.createNamedQuery("Product.findByNameWithExtendedScope", Product.class);
        assertEquals("NamedQuery lockScope should be EXTENDED", PessimisticLockScope.EXTENDED, namedQuery.getLockScope());

        namedQuery.setParameter("name", "SpecialProduct");
        List<Product> results = namedQuery.getResultList();
        assertEquals(1, results.size());
        assertEquals("SpecialProduct", results.get(0).getName());
    }

    // -----------------------------------------------------------------------
    // JPA 4.0 — Entity Listeners enhancements
    // -----------------------------------------------------------------------

    /** Verify @EntityListener annotation is present on AuditListener class. */
    @Test
    public void testEntityListenerAnnotation() throws Exception {
        assertTrue("@EntityListener must be present on AuditListener",
            AuditListener.class.isAnnotationPresent(EntityListener.class));
    }

    /**
     * Verify an entity listener may have multiple callbacks of the same type (@PrePersist)
     * with different parameter types as per JPA 4.0 Section 3.10.1.
     */
    @Test
    public void testMultipleCallbacksOfSameTypeOnEntityListener() throws Exception {
        cleanup(SimpleEmployee.class);
        cleanup(Product.class);
        AuditListener.reset();

        tx.begin();
        SimpleEmployee emp = new SimpleEmployee("ListenerUser", 70_000L);
        em.persist(emp);
        tx.commit();

        assertEquals("SimpleEmployee @PrePersist callback should be executed", 1, AuditListener.employeePrePersistCount.get());
        assertEquals("Product @PrePersist callback should NOT be executed yet", 0, AuditListener.productPrePersistCount.get());
        assertEquals("@PostPersist callback should be executed", 1, AuditListener.postPersistCount.get());

        tx.begin();
        Product product = new Product("ListenerProduct", 49.99);
        em.persist(product);
        tx.commit();

        assertEquals("Product @PrePersist callback should be executed", 1, AuditListener.productPrePersistCount.get());
        assertEquals("@PostPersist callback count should now be 2", 2, AuditListener.postPersistCount.get());
    }

    /** Verify EntityManagerFactory.addListener() registers dynamic listener callback. */
    @Test
    public void testEntityManagerFactoryAddListener() throws Exception {
        EntityManagerFactory emf = em.getEntityManagerFactory();
        assertNotNull("EntityManagerFactory should not be null", emf);

        AtomicInteger dynamicPrePersistCount = new AtomicInteger(0);

        // Register dynamic listener callback via EntityManagerFactory.addListener()
        emf.addListener(Product.class, PrePersist.class, p -> dynamicPrePersistCount.incrementAndGet());

        cleanup(Product.class);
        tx.begin();
        Product product = new Product("DynamicListenerProduct", 199.99);
        em.persist(product);
        tx.commit();

        assertTrue("dynamic listener callback should have been invoked", dynamicPrePersistCount.get() >= 1);
    }

    /** Verify @EntityListeners targets include PACKAGE and MODULE as per JPA 4.0. */
    @Test
    public void testEntityListenersTargetAllowedOnPackageAndModule() throws Exception {
        Target target = EntityListeners.class.getAnnotation(Target.class);
        assertNotNull("@Target annotation on @EntityListeners must not be null", target);

        List<ElementType> targets = Arrays.asList(target.value());
        assertTrue("@EntityListeners should target TYPE", targets.contains(ElementType.TYPE));
        assertTrue("@EntityListeners should target PACKAGE", targets.contains(ElementType.PACKAGE));
        assertTrue("@EntityListeners should target MODULE", targets.contains(ElementType.MODULE));
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
