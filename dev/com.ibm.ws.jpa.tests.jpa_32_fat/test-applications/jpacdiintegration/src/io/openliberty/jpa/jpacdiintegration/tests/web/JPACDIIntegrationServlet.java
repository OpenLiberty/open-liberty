/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.jpacdiintegration.tests.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.jpa.jpacdiintegration.tests.models.TestEntity;
import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;

/**
 * Main test servlet for testing the jpa cdi integration changes in jpa 3.2.
 *
 * This servlet contains tests that verify the proper behavior of EntityManager injection
 * and usage in various CDI contexts.
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JPACDI32")
public class JPACDIIntegrationServlet extends FATServlet {
    
    @Inject
    private EntityManager defaultEM; // Default TransactionScoped

    @Inject
    private EntityManager defaultEM1;

    @Inject
    private EntityManager defaultEM2;
    
    @Inject
    @ShortScoped
    private EntityManager shortScopedEM; // Dependent scope (shorter than TransactionScoped)

    @Inject
    @LongScoped
    private EntityManager longScopedEM; // ApplicationScoped (longer than TransactionScoped)
    
    
    @Inject
    private Instance<NestedEntityManagerTest> nestedEMTestInstance;

    @Resource
    private UserTransaction tx;

    @Test
    public void alwaysPasses() {
        assertTrue(true);
    }

    @Test
    public void testEntityManagerInjection() throws Exception {
        // Start a transaction since the default EntityManager is TransactionScoped
        tx.begin();
        try {
            assertNotNull(defaultEM);
            System.out.println("EntityManager injected via @Inject");
            assertTrue(defaultEM.isOpen());
            System.out.println("EntityManager @Inject test passed!");
        } finally {
            tx.commit();
        }
    }

    @Test
    public void testOperationOutsideTransactionThrows() throws Exception {
        tx.begin();
        tx.commit();
        try {
            defaultEM.find(TestEntity.class, 1L);
            fail("Expected ContextNotActiveException but no exception was thrown");
        } catch (ContextNotActiveException e) {
            // Expected exception
            System.out.println("Correctly caught TransactionRequiredException when no transaction is active");
        }
    }

    @Test
    public void testSameScopedEntityManagerInstancesAreEqual() throws Exception {
        tx.begin();
        try {
            assertTrue("Expected same EM instance within same scope", defaultEM1 == defaultEM2);
        } finally {
            tx.commit();
        }
    }

    @Test
    public void testDifferentScopesProduceDifferentInstances() {
        assertFalse("Short and long scoped EMs should differ", shortScopedEM == longScopedEM);
    }

    @Test
    public void testLongScopedEntityManagerPersistsAcrossTransactions() throws Exception {
        tx.begin();
        TestEntity e = new TestEntity("persistent");
        longScopedEM.persist(e);
        tx.commit();

        tx.begin();
        TestEntity found = longScopedEM.find(TestEntity.class, e.getId());
        assertNotNull("Entity should be found across transactions", found);
        tx.commit();
    }

    @Test
    public void testNestedBeanEntityManagerPropagation() throws Exception {
        io.openliberty.jpa.jpacdiintegration.tests.web.NestedEntityManagerTest nested = nestedEMTestInstance.get();
        assertNotNull("Nested bean should be available", nested);
        nested.createEntities();
        nested.verifyEntities();
        nested.updateEntities();
        nested.verifyUpdates();
    }   


    @Test
    public void testEntityManagerJoinsTransactionAutomatically() throws Exception {
        tx.begin();
        assertTrue("EM should join transaction automatically", defaultEM.isJoinedToTransaction());
        tx.commit();
    }

    @Test
    public void testRollbackDisablesEntityManagerContext() throws Exception {
        tx.begin();
        TestEntity e = new TestEntity("rollback");
        defaultEM.persist(e);
        tx.rollback();

        try {
            defaultEM.find(TestEntity.class, e.getId());
            fail("Expected ContextNotActiveException or some exception because context is inactive");
        } catch (ContextNotActiveException ex) {
            // this is acceptable behavior
        }
    }

    @Test
    public void testEntityManagerWithShorterScope() throws Exception {
        // Start a transaction to ensure we have an active context
        tx.begin();
        try {
            assertNotNull(shortScopedEM);
            assertTrue(shortScopedEM.isOpen());
            System.out.println("Short-scoped EntityManager injected successfully");
            
            shortScopedEM.clear();
            System.out.println("Short-scoped EntityManager used within transaction");
            EntityManager em1 = shortScopedEM;
            
            // verify that the EntityManager is still usable within the transaction
            assertTrue(em1.isOpen());
            System.out.println("Short-scoped EntityManager is open within transaction");
        
            tx.commit();
            System.out.println("Transaction committed");
            
            tx.begin();
            assertTrue(shortScopedEM.isOpen());
            System.out.println("Short-scoped EntityManager is still open after transaction");
            System.out.println("Short-scoped EntityManager test passed!");
            tx.commit();
        } catch (Exception e) {
            if (tx.getStatus() != jakarta.transaction.Status.STATUS_NO_TRANSACTION) {
                tx.rollback();
            }
            throw e;
        }
    }

    @Test
    public void testEntityManagerWithLongerScope() throws Exception {
        tx.begin();
        try {
            assertNotNull(longScopedEM);
            assertTrue(longScopedEM.isOpen());
            System.out.println("Long-scoped EntityManager injected successfully");
            
            longScopedEM.clear();
            System.out.println("Long-scoped EntityManager used within transaction");
            tx.commit();
            System.out.println("Transaction committed");
            
            tx.begin();
            // The EntityManager should still be open after the transaction
            // because it's ApplicationScoped (longer than TransactionScoped)
            assertTrue(longScopedEM.isOpen());
            System.out.println("Long-scoped EntityManager is still open after transaction");
            
            longScopedEM.clear();
            tx.commit();
            System.out.println("Long-scoped EntityManager used in a second transaction");
        } catch (Exception e) {
            if (tx.getStatus() != jakarta.transaction.Status.STATUS_NO_TRANSACTION) {
                tx.rollback();
            }
            throw e;
        }
    }

    @Test
    public void testMultipleEntityManagersFromSamePU() throws Exception {
        assertNotNull("Default EM should not be injected", defaultEM);
        assertNotNull("ShortScoped EM should not be injected", shortScopedEM);

        try {
            tx.begin();

            TestEntity e1 = new TestEntity("FromDefault");
            defaultEM.persist(e1);

            if (!defaultEM.isJoinedToTransaction()) {
                defaultEM.joinTransaction();
            }
            defaultEM.flush();
            
            TestEntity e2 = new TestEntity("FromShortScoped");
            shortScopedEM.persist(e2);

            if (!shortScopedEM.isJoinedToTransaction()) {
                shortScopedEM.joinTransaction();
            }
            shortScopedEM.flush();

            tx.commit();

            tx.begin();

            defaultEM.clear();
            shortScopedEM.clear();

            Long id1 = e1.getId();
            Long id2 = e2.getId();

            TestEntity found1 = defaultEM.find(TestEntity.class, id1);
            TestEntity found2 = shortScopedEM.find(TestEntity.class, id2);

            assertNotNull("Entity persisted by defaultEM should be found", found1);
            assertNotNull("Entity persisted by shortScopedEM should be found", found2);

            assertEquals("FromDefault", found1.getName());
            assertEquals("FromShortScoped", found2.getName());

            TestEntity cross1 = shortScopedEM.find(TestEntity.class, id1);
            TestEntity cross2 = defaultEM.find(TestEntity.class, id2);

            assertNotNull("shortScopedEM should be able to find entity from defaultEM", cross1);
            assertNotNull("defaultEM should be able to find entity from shortScopedEM", cross2);

            tx.commit();

            tx.begin();

            defaultEM.clear();
            shortScopedEM.clear();

            TestEntity rem1 = defaultEM.find(TestEntity.class, id1);
            if (rem1 != null) {
                defaultEM.remove(rem1);
            }
            
            TestEntity rem2 = shortScopedEM.find(TestEntity.class, id2);
            if (rem2 != null) {
                shortScopedEM.remove(rem2);
            }

            tx.commit();

        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            if (tx.getStatus() != Status.STATUS_NO_TRANSACTION) {
                try {
                    tx.rollback();
                } catch (Exception rbEx) {
                    rbEx.printStackTrace(System.err);
                }
            }
            throw ex;
        }
    }

    @Test
    public void testEntityManagerClosure() throws Exception {
    // Start a transaction to ensure the @TransactionScoped bean is active
    tx.begin();
    try {
        assertNotNull(defaultEM);
        assertTrue(defaultEM.isOpen());
        System.out.println("EntityManager is open within transaction.");
        tx.commit();
        assertFalse(defaultEM.isOpen());
        System.out.println("EntityManager is closed after transaction commit.");
    } catch (IllegalStateException | ContextNotActiveException e) {
        //Excepted exception, because we tried to access defaultEM after transaction is commited.
    }
}


}
