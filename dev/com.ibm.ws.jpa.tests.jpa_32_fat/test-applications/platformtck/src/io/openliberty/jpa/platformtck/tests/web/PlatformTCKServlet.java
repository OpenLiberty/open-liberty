/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.platformtck.tests.web;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import componenttest.app.FATServlet;
import io.openliberty.jpa.platformtck.tests.models.TestEntity;
import jakarta.annotation.Resource;
import jakarta.enterprise.inject.Instance;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;
import jakarta.inject.Inject;
import org.junit.Ignore;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/PlatformTCK32")
public class PlatformTCKServlet extends FATServlet {
    @Inject
    private EntityManager defaultEM; // Default TransactionScoped
    
    @Inject
    @ShortScoped
    private EntityManager shortScopedEM; // Dependent scope (shorter than TransactionScoped)

    @Inject
    @LongScoped
    private EntityManager longScopedEM; // ApplicationScoped (longer than TransactionScoped)
    
    @Inject
    private Instance<MultipleEntityManagerTest> multipleEMTestInstance;
    
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
    public void testEntityManagerWithShorterScope() throws Exception {
        assertNotNull(shortScopedEM);
        assertTrue(shortScopedEM.isOpen());
        System.out.println("Short-scoped EntityManager injected successfully");
        
        tx.begin();
        shortScopedEM.clear();
        System.out.println("Short-scoped EntityManager used within transaction");
        EntityManager em1 = shortScopedEM;
        
        // In Dependent scope, each injection point gets a new instance
        // So we need to verify that the EntityManager is still usable within the transaction
        assertTrue(em1.isOpen());
        System.out.println("Short-scoped EntityManager is open within transaction");
        tx.commit();
        System.out.println("Transaction committed");
        // The EntityManager should still be open after the transaction
        // because it's not bound to the transaction scope
        assertTrue(shortScopedEM.isOpen());
        System.out.println("Short-scoped EntityManager is still open after transaction");  
        System.out.println("Short-scoped EntityManager test passed!");
    }

    @Test
    public void testEntityManagerWithLongerScope() throws Exception {
        assertNotNull(longScopedEM);
        assertTrue(longScopedEM.isOpen());
        System.out.println("Long-scoped EntityManager injected successfully");
        tx.begin();
        longScopedEM.clear();
        System.out.println("Long-scoped EntityManager used within transaction");
        tx.commit();
        System.out.println("Transaction committed"); 
        // The EntityManager should still be open after the transaction
        // because it's ApplicationScoped (longer than TransactionScoped)
        assertTrue(longScopedEM.isOpen());
        System.out.println("Long-scoped EntityManager is still open after transaction");
        // Start another transaction to verify the EntityManager is still usable
        tx.begin();
        longScopedEM.clear();
        tx.commit();
        System.out.println("Long-scoped EntityManager used in a second transaction");
    }

    @Test
    public void testMultipleEntityManagersFromSamePU() throws Exception {
        // Test injecting two EntityManagers from same Persistence Unit (one with qualifier, one without)
        assertNotNull("Default EntityManager should not be null", defaultEM);
        assertNotNull("ShortScoped EntityManager should not be null", shortScopedEM);
        
        // Verify both are open
        tx.begin();
        try {
            assertTrue("Default EntityManager should be open", defaultEM.isOpen());
            assertTrue("ShortScoped EntityManager should be open", shortScopedEM.isOpen());
            
            // Verify they are different instances
            assertFalse("Default and ShortScoped EntityManagers should be different instances",
                       defaultEM == shortScopedEM);
            
            // Use default EntityManager to persist an entity
            TestEntity entity1 = new TestEntity("DefaultEntityManager");
            defaultEM.persist(entity1);
            
            // Use qualified EntityManager to persist another entity
            TestEntity entity2 = new TestEntity("ShortScopedEntityManager");
            shortScopedEM.persist(entity2);
            
            tx.commit();
            tx.begin();
            
            // Verify both entities were persisted and can be found by either EntityManager
            TestEntity found1 = defaultEM.find(TestEntity.class, entity1.getId());
            TestEntity found2 = shortScopedEM.find(TestEntity.class, entity2.getId());
            
            assertNotNull("Entity should be found with default EntityManager", found1);
            assertNotNull("Entity should be found with ShortScoped EntityManager", found2);
            
            assertEquals("DefaultEntityManager", found1.getName());
            assertEquals("ShortScopedEntityManager", found2.getName());
            
            // Cross-check: verify that each EntityManager can find entities created by the other
            TestEntity crossCheck1 = shortScopedEM.find(TestEntity.class, entity1.getId());
            TestEntity crossCheck2 = defaultEM.find(TestEntity.class, entity2.getId());
            
            assertNotNull("ShortScoped EntityManager should find entity created by default EntityManager", crossCheck1);
            assertNotNull("Default EntityManager should find entity created by ShortScoped EntityManager", crossCheck2);
        
            defaultEM.remove(found1);
            shortScopedEM.remove(found2);
            
            tx.commit();
            System.out.println("Multiple EntityManagers from same PU test passed!");
        } catch (Exception e) {
            tx.rollback();
            throw e;
        }
    }

    @Test
    public void testMultipleEntityManagersInSingleBean() throws Exception {
        // Get an instance of our test bean
        MultipleEntityManagerTest multipleEMTest = multipleEMTestInstance.get();
        assertNotNull("MultipleEntityManagerTest instance should not be null", multipleEMTest);
        
        // Verify all EntityManagers are injected
        assertNotNull("Default EntityManager in test bean should not be null", multipleEMTest.getDefaultEM());
        assertNotNull("ShortScoped EntityManager in test bean should not be null", multipleEMTest.getShortScopedEM());
        assertNotNull("LongScoped EntityManager in test bean should not be null", multipleEMTest.getLongScopedEM());
        
        // Create entities using different EntityManagers
        multipleEMTest.createEntities();
        System.out.println("Created entities using different EntityManagers");
        
        // Verify entities can be found by all EntityManagers
        boolean verificationResult = multipleEMTest.verifyEntities();
        assertTrue("All EntityManagers should be able to find entities created by other EntityManagers", verificationResult);
        System.out.println("Verified all EntityManagers can find entities created by others");
        
        // Update entities using different EntityManagers
        multipleEMTest.updateEntities();
        System.out.println("Updated entities using different EntityManagers than the ones that created them");
        
        // Verify updates are visible to all EntityManagers
        boolean updateVerificationResult = multipleEMTest.verifyUpdates();
        assertTrue("All EntityManagers should see updates made by other EntityManagers", updateVerificationResult);
        System.out.println("Verified all EntityManagers can see updates made by others");
        
        // Clean up
        multipleEMTest.cleanupEntities();
        System.out.println("Cleaned up test entities");
        
        System.out.println("Multiple EntityManagers in single bean test passed!");
    }

    @Test
    public void testNestedEntityManagersInMultipleBeans() throws Exception {
        // Get an instance of our test bean
        NestedEntityManagerTest nestedEMTest = nestedEMTestInstance.get();
        assertNotNull("NestedEntityManagerTest instance should not be null", nestedEMTest);
        
        // Create entities using parent and child beans
        nestedEMTest.createEntities();
        System.out.println("Created entities using parent and child beans");
        
        // Verify entities can be found by all beans
        boolean verificationResult = nestedEMTest.verifyEntities();
        assertTrue("All beans should be able to find entities created by other beans", verificationResult);
        System.out.println("Verified all beans can find entities created by others");
        
        // Update entities using different beans than the ones that created them
        nestedEMTest.updateEntities();
        System.out.println("Updated entities using different beans than the ones that created them");
        
        // Verify updates are visible to all beans
        boolean updateVerificationResult = nestedEMTest.verifyUpdates();
        assertTrue("All beans should see updates made by other beans", updateVerificationResult);
        System.out.println("Verified all beans can see updates made by others");
        
        // Clean up
        nestedEMTest.cleanupEntities();
        System.out.println("Cleaned up test entities");
        
        System.out.println("Nested EntityManagers in multiple beans test passed!");
    }
}

// Made with Bob
