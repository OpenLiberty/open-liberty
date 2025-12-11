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

import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.inject.Inject;
import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
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

}
