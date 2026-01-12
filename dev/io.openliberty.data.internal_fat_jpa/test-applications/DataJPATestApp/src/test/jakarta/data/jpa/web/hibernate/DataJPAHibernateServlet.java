/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web.hibernate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Resource;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.inject.Inject;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;

import javax.naming.InitialContext;

import org.junit.Test;

import componenttest.app.FATServlet;
import test.jakarta.data.jpa.web.Business;

/**
 * For tests that only run on the Hibernate Persistence provider.
 * Also creates a datastore reference to the persistence unit used for the common set of tests.
 */
@PersistenceUnit(name = "java:app/env/data/DataStoreRef",
                 unitName = "HibernatePersistenceUnit")
@PersistenceUnit(name = "java:comp/env/persistence/HibernatePersistenceUnitRef",
                 unitName = "HibernatePersistenceUnit")
@SuppressWarnings("serial")
@WebServlet("/DataJPAEclipseLinkServlet")
public class DataJPAHibernateServlet extends FATServlet {

    @PersistenceContext(unitName = "HibernatePersistenceUnit")
    EntityManager cmEntityManger;

    @Resource
    UserTransaction transaction;

    @Inject
    Companies companies;

    /**
     * Define a repository method that has a query that orders the SELECT clause
     * after the FROM clause. Ensure that a count query can be inferred and
     * used to compute the correct total count of results.
     */
    @Test
    public void testCountQuery_FROM_SELECT() {

        Order<Business> sorts = Order.by(Sort.asc("location.address.zip"),
                                         Sort.asc("name"));
        PageRequest page1Req = PageRequest.ofSize(5);

        CursoredPage<Business> page1 = companies.all(page1Req, sorts);

        assertEquals(15, page1.totalElements());
    }

    /**
     * Define a repository method that has a query that orders the SELECT clause
     * after the FROM clause and also has an ORDER clause. Ensure that a count
     * query can be inferred and used to compute the correct total count of
     * results.
     */
    @Test
    public void testCountQuery_FROM_SELECT_ORDER() {

        PageRequest page1Req = PageRequest.ofSize(5);
        Page<Business> page1 = companies.alphabetized(page1Req);

        assertEquals(15, page1.totalElements());
    }

    /**
     * Define a repository method that has a query that orders the SELECT clause
     * after the FROM and WHERE clauses. Ensure that a count query can be
     * inferred and used to compute the correct total count of results.
     */
    @Test
    public void testCountQuery_FROM_WHERE_SELECT() {

        PageRequest pageReq = PageRequest.ofSize(2);
        Page<String> page1 = companies.withStreetNamePattern("%th %", pageReq);

        assertEquals(4, page1.totalElements());
    }

    /**
     * Define a repository method that has a query that orders the SELECT clause
     * after the FROM and WHERE clauses, but before the ORDER BY clause. Ensure
     * that a count query can be inferred and used to compute the correct total
     * count of results.
     */
    @Test
    public void testCountQuery_FROM_WHERE_SELECT_ORDER() {

        PageRequest pageReq = PageRequest.ofSize(8);
        Page<Business> page1 = companies.inCity("Rochester", pageReq);

        assertEquals(13, page1.totalElements());
    }

    /**
     * Define a repository method that has a query that orders the SELECT clause
     * after WHERE clause. Ensure that a count query can be inferred and used to
     * compute the correct total count of results.
     */
    @Test
    public void testCountQuery_WHERE_SELECT() {

        Order<Business> sorts = Order.by(Sort.desc("name"));
        PageRequest pageReq = PageRequest.ofSize(6);

        Page<Business> page1 = companies.namedLike("%____ ____%",
                                                   sorts,
                                                   pageReq);

        assertEquals(8, page1.totalElements());
    }

    /**
     * Define a repository method that has a query that orders the SELECT clause
     * after WHERE clause, but before the ORDER clause. Ensure that a count query
     * can be inferred and used to compute the correct total count of results.
     */
    @Test
    public void testCountQuery_WHERE_SELECT_ORDER() {

        PageRequest pageReq = PageRequest.ofSize(7);

        Page<Business> page1 = companies.westOfBroadway("Rochester",
                                                        pageReq);

        assertEquals(10, page1.totalElements());
    }

    /**
     * Reproduces a migration issue (33205) from EclipseLink to Hibernate
     * where Hibernate does not tolerate an ElementCollection of type
     * java.util.ArrayList, which EclipseLink does tolerate.
     */
    @Test
    public void testEntityWithArrayListAttribute() throws Exception {
        EntityManagerFactory emf = InitialContext
                        .doLookup("java:comp/env/persistence/HibernatePersistenceUnitRef");
        UserTransaction tx = InitialContext
                        .doLookup("java:comp/UserTransaction");

        EntityWithArrayList entity = new EntityWithArrayList();
        entity.setId("TestEntityWithArrayListAttribute");
        entity.setLongList(new ArrayList<>(List.of(1L, 2L, 3L)));

        EntityManager em = null;

        tx.begin();
        try {
            em = emf.createEntityManager();
            em.setCacheRetrieveMode(CacheRetrieveMode.BYPASS);
            assertEquals(true, em.isJoinedToTransaction());

            em.persist(entity);
        } finally {
            if (tx.getStatus() == Status.STATUS_ACTIVE)
                tx.commit();
            else
                tx.rollback();
            em.clear();
            em.close();
        }

        em = emf.createEntityManager();
        try {
            entity = em.find(EntityWithArrayList.class,
                             "TestEntityWithArrayListAttribute");
        } finally {
            em.close();
        }

        assertEquals(List.of(1L, 2L, 3L),
                     entity.getLongList());
    }

    /**
     * Reproduces a migration issue (33290) from EclipseLink to Hibernate
     * where Hibernate does not tolerate multiple ElementCollections
     * when usinga load graph, which EclipseLink does tolerate.
     * To reproduce 33290, uncomment ElementCollection on the
     * EntityWithTwoElementCollections class.
     */
    @Test
    public void testEntityWithTwoElementCollections() throws Exception {
        EntityManagerFactory emf = InitialContext
                        .doLookup("java:comp/env/persistence/HibernatePersistenceUnitRef");
        UserTransaction tx = InitialContext
                        .doLookup("java:comp/UserTransaction");

        EntityWithTwoElementCollections entity = new EntityWithTwoElementCollections();
        entity.setId(2);
        entity.setLazyList1(List.of("elements", "of", "first", "list"));
        entity.setLazyList2(List.of("the", "second", "list"));

        EntityManager em = null;
        EntityGraph<EntityWithTwoElementCollections> graph = null;

        tx.begin();
        try {
            em = emf.createEntityManager();
            em.setCacheRetrieveMode(CacheRetrieveMode.BYPASS);
            assertEquals(true, em.isJoinedToTransaction());

            graph = em.createEntityGraph(EntityWithTwoElementCollections.class);
            graph.addAttributeNode("lazyList1");
            graph.addAttributeNode("lazyList2");

            em.persist(entity);
        } finally {
            if (tx.getStatus() == Status.STATUS_ACTIVE)
                tx.commit();
            else
                tx.rollback();
            em.clear();
            em.close();
        }

        em = emf.createEntityManager();
        try {
            String jpql = "SELECT e FROM EntityWithTwoElementCollections e WHERE e.id=?1";
            jakarta.persistence.Query query = em.createQuery(jpql);
            query.setHint("jakarta.persistence.loadgraph", graph);
            query.setParameter(1, 2);
            List<?> results = query.getResultList();
            assertEquals(1, results.size());
            entity = (EntityWithTwoElementCollections) results.get(0);
        } finally {
            em.close();
        }

        assertEquals(List.of("elements", "of", "first", "list"),
                     entity.getLazyList1());

        assertEquals(List.of("the", "second", "list"),
                     entity.getLazyList2());
    }

    /**
     * Reproduces an issue where updates are made and a flush is requested
     * during a transaction but not honored, so if the entity is detached
     * after that point, the updates are lost.
     */
    @Test
    public void testFlushAndDetachDuringTransaction() throws Exception {
        EntityManagerFactory emf = InitialContext
                        .doLookup("java:comp/env/persistence/HibernatePersistenceUnitRef");
        UserTransaction tx = InitialContext
                        .doLookup("java:comp/UserTransaction");

        SimpleEntity entity = new SimpleEntity();
        entity.id = 2;
        entity.value = "new";

        EntityManager em = null;

        tx.begin();
        try {
            em = emf.createEntityManager();
            em.setCacheRetrieveMode(CacheRetrieveMode.BYPASS);
            assertEquals(true, em.isJoinedToTransaction());

            em.persist(entity);
        } finally {
            if (tx.getStatus() == Status.STATUS_ACTIVE)
                tx.commit();
            else
                tx.rollback();
            em.clear();
            em.close();
        }

        tx.begin();
        try {
            em = emf.createEntityManager();
            em.setCacheRetrieveMode(CacheRetrieveMode.BYPASS);
            assertEquals(true, em.isJoinedToTransaction());

            entity = em.find(SimpleEntity.class, 2);
            entity.value = "flushed";

            entity = em.merge(entity);

            // JPA 3.2 spec states:
            // "The flush method can be used by the application to force synchronization."
            // EntityManager Javadoc states:
            // "The client may force an immediate flush to occur by calling flush()."
            // EntityManager.flush is defined as:
            // "Synchronize changes held in the persistence context to the underlying database."

            em.flush(); // this is ignored

            // EntityManager.detach Javadoc says:
            // "Unflushed changes made to the entity, if any, including deletion
            // of the entity, will never be synchronized to the database."
            // However, we did invoke flush.
            //em.detach(entity);

            // EntityManager.clear Javadoc says:
            // "Changes made to entities that have not already been flushed to
            // the database will never be made persistent."
            // However, we did invoke flush.
            em.clear();
        } finally {
            if (tx.getStatus() == Status.STATUS_ACTIVE)
                tx.commit();
            else
                tx.rollback();
            em.clear();
            em.close();
        }

        try {
            em = emf.createEntityManager();
            em.setCacheRetrieveMode(CacheRetrieveMode.BYPASS);

            entity = em.find(SimpleEntity.class, 2);
        } finally {
            em.clear();
            em.close();
        }

        // TODO enable once #33544 is fixed
        // assertEquals("flushed", entity.value);
    }

    /**
     * Reproduces an issue where a previously detached entity is merged to the
     * persistence context in order to make an update, but Hibernate never
     * writes the update to the database, even when the transaction commits.
     * This can also be used to reproduce a similar error where instead of a
     * detached entity, the entity is fetched during the same transaction and
     * then updated, with Hibernate similarly never writing the update to the
     * database, even when the transaction commits. However, if we instead
     * create a new instance of the entity and merge it to the persistence
     * context, then Hibernate does write the update to the database.
     */
    @Test
    public void testMergeDetachedEntity() throws Exception {
        EntityManagerFactory emf = InitialContext
                        .doLookup("java:comp/env/persistence/HibernatePersistenceUnitRef");
        UserTransaction tx = InitialContext
                        .doLookup("java:comp/UserTransaction");

        SimpleEntity entity = new SimpleEntity();
        entity.id = 1;
        entity.value = "new";

        EntityManager em = null;

        tx.begin();
        try {
            em = emf.createEntityManager();
            em.setCacheRetrieveMode(CacheRetrieveMode.BYPASS);
            assertEquals(true, em.isJoinedToTransaction());

            em.persist(entity);
            // em.flush(); // unnecessary due to commit
        } finally {
            if (tx.getStatus() == Status.STATUS_ACTIVE)
                tx.commit();
            else
                tx.rollback();
            em.clear();
            em.close();
        }

        try {
            em = emf.createEntityManager();
            em.setCacheRetrieveMode(CacheRetrieveMode.BYPASS);

            entity = em.find(SimpleEntity.class, 1);
        } finally {
            em.clear();
            em.close();
        }

        assertEquals("new", entity.value);

        entity = new SimpleEntity(); // workaround
        entity.id = 1; // workaround
        // TODO remove this line and 2 lines above once #33232 is fixed, or to reproduce
        entity.value = "merged";

        tx.begin();
        try {
            em = emf.createEntityManager();
            em.setCacheRetrieveMode(CacheRetrieveMode.BYPASS);
            assertEquals(true, em.isJoinedToTransaction());

            SimpleEntity found = em.find(SimpleEntity.class, 1, LockModeType.PESSIMISTIC_WRITE);
            //found.value = entity.value; // alternative to merge that doesn't work either
            //entity = found;

            entity = em.merge(entity);
            // em.flush(); // should be unnecessary due to commit
        } finally {
            if (tx.getStatus() == Status.STATUS_ACTIVE)
                tx.commit();
            else
                tx.rollback();
            em.clear();
            em.close();
        }

        try {
            em = emf.createEntityManager();
            em.setCacheRetrieveMode(CacheRetrieveMode.BYPASS);

            entity = em.find(SimpleEntity.class, 1);
        } finally {
            em.clear();
            em.close();
        }

        assertEquals("merged", entity.value);
    }

    @Test
    public void testMergeDetachedEntityAppManaged() throws Exception {
        EntityManagerFactory emf = InitialContext
                        .doLookup("java:comp/env/persistence/HibernatePersistenceUnitRef");
        UserTransaction tx = InitialContext
                        .doLookup("java:comp/UserTransaction");

        SimpleEntity original = new SimpleEntity();
        original.id = 100;
        original.value = "new";

        EntityManager em = emf.createEntityManager();
        assertNotNull(em);

        tx.begin();
        try {
            em.joinTransaction();
            assertTrue("Entity manager should have been joined to transaction",
                       em.isJoinedToTransaction());

            em.persist(original);
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            fail("Transaction rollback");
        }

        // The original entity is now detached
        // The original entity should have been persisted to the database.

        //Another part of the application modifies the original entity
        original.value = "modified";

        // Now we want to persist this change to the database
        // so we have to re-attach the detached entity.

        SimpleEntity merged = null;

        tx.begin();
        try {
            em.joinTransaction();
            assertTrue("Entity manager should have been joined to transaction",
                       em.isJoinedToTransaction());

            merged = em.merge(original);
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            fail("Transaction rollback");
        }

        // Check make sure the merged entity shows the update.
        assertNotNull(merged);
        assertEquals("modified", merged.value);

        // The merged entity is now detached
        // The merged entity should have been updated after commit

        // Use an isolated entity manager to find the entity in the database
        // to avoid caching in the persistent context

        SimpleEntity found = null;

        tx.begin();
        try (EntityManager isolated = emf.createEntityManager()) {
            assertTrue("Entity manager should have been joined to transaction",
                       isolated.isJoinedToTransaction());

            found = isolated.find(SimpleEntity.class, 100);
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            fail("Transaction rollback");
        }

        // Check to make sure
        // - The entity exists in the database
        // - The entity was updated in the database
        assertNotNull(found);

        //TODO enable once https://hibernate.atlassian.net/browse/HHH-19995 is fixed
//        assertEquals("modified", found.value); //Fail: expected:<[modified]> but was:<[new]>
    }

    @Test
    public void testMergeDetachedEntityContainerManaged() throws Exception {
        EntityManagerFactory emf = InitialContext
                        .doLookup("java:comp/env/persistence/HibernatePersistenceUnitRef");

        SimpleEntity original = new SimpleEntity();
        original.id = 101;
        original.value = "new";

        assertNotNull(cmEntityManger);

        transaction.begin();
        try {
            assertTrue("Entity manager should have been joined to transaction",
                       cmEntityManger.isJoinedToTransaction());

            cmEntityManger.persist(original);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            fail("Transaction rollback");
        }

        // The original entity is now detached
        // The original entity should have been persisted to the database.

        //Another part of the application modifies the original entity
        original.value = "modified";

        // Now we want to persist this change to the database
        // so we have to re-attach the detached entity.

        SimpleEntity merged = null;

        transaction.begin();
        try {
            assertTrue("Entity manager should have been joined to transaction",
                       cmEntityManger.isJoinedToTransaction());

            merged = cmEntityManger.merge(original);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            fail("Transaction rollback");
        }

        // Check make sure the merged entity shows the update.
        assertNotNull(merged);
        assertEquals("modified", merged.value);

        // The merged entity is now detached
        // The merged entity should have been updated after commit

        // Use an isolated entity manager to find the entity in the database
        // to avoid caching in the persistent context

        SimpleEntity found = null;

        transaction.begin();
        try (EntityManager isolated = emf.createEntityManager()) {
            assertTrue("Entity manager should have been joined to transaction",
                       isolated.isJoinedToTransaction());

            found = isolated.find(SimpleEntity.class, 101);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            fail("Transaction rollback");
        }

        // Check to make sure
        // - The entity exists in the database
        // - The entity was updated in the database
        assertNotNull(found);

        //TODO enable once https://hibernate.atlassian.net/browse/HHH-19995 is fixed
//        assertEquals("modified", found.value); //Fail: expected:<[modified]> but was:<[new]>
    }
}
