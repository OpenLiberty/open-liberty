/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.entitymanager.testlogic;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.EntityManager;

import org.junit.Assert;

import com.ibm.ws.jpa.entitymanager.model.JPA10EntityManagerEntityA;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class EntityManagerLogic extends AbstractTestLogic {

    /**
     * Verify that calling EntityManager.remove() will result in the removal of the entity from the database. (3.2.3)
     */
    public void testRemove001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                              Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 10;

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing find(" + JPA10EntityManagerEntityA.class + ", " + id + ") operation");
            JPA10EntityManagerEntityA entityFind1A = em.find(JPA10EntityManagerEntityA.class, id);
            Assert.assertNotNull("find(" + JPA10EntityManagerEntityA.class + ", " + id + ") did not return an entity.", entityFind1A);
            Assert.assertTrue(entityFind1A + " is not managed by the persistence context.", em.contains(entityFind1A));

            System.out.println("Performing remove(" + entityFind1A + ") operation");
            em.remove(entityFind1A);

            // JPA spec; 3.2.8:
            // contains method returns false:
            //     If the remove method has been called on the entity, or the remove operation has been cascaded to it.
            Assert.assertFalse(entityFind1A + " is managed by the persistence context.", em.contains(entityFind1A));

            // JPA spec; 3.2.3:
            // A removed entity X will be removed from the database at or before transaction commit or as a result of the flush operation.
            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity was removed from the database after transaction commit
            System.out.println("Performing find(" + JPA10EntityManagerEntityA.class + ", " + id + ") operation");
            JPA10EntityManagerEntityA entityFind1B = em.find(JPA10EntityManagerEntityA.class, id);
            Assert.assertNull("find(" + JPA10EntityManagerEntityA.class + ", " + id + ") returned an entity.", entityFind1B);

            // Put the entity back after the test removed it
            // Portable applications should not pass removed entities that have been detached
            // from the persistence context to further Entity-Manager operations. (3.2.6)
            JPA10EntityManagerEntityA entityPersist1A = new JPA10EntityManagerEntityA();
            entityPersist1A.setId(entityFind1A.getId());
            entityPersist1A.setStrData(entityFind1A.getStrData());
            entityPersist1A.setEntityC(entityFind1A.getEntityC());
            entityPersist1A.setEntityCLazy(entityFind1A.getEntityCLazy());

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing persist(" + entityPersist1A + ") operation");
            em.persist(entityPersist1A);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Performing find(" + JPA10EntityManagerEntityA.class.getSimpleName() + ", " + id + ") operation");
            JPA10EntityManagerEntityA entityFind1C = em.find(JPA10EntityManagerEntityA.class, id);
            Assert.assertNotNull("find(" + JPA10EntityManagerEntityA.class.getSimpleName() + ", " + id + ") did not return an entity.", entityFind1C);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Verify that calling EntityManager.remove() on a detached entity, an IllegalArgumentException will be thrown by the remove operation
     * (or the transaction commit will fail). (3.2.3)
     */
    public void testRemove002(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                              Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // TODO: OpenJPA Bug: OpenJPA does not throw the exception required by the JPA specification
        if (JPAProviderImpl.OPENJPA.equals(getJPAProviderImpl(jpaResource))) {
            return;
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 20;

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing find(" + JPA10EntityManagerEntityA.class + ", " + id + ") operation");
            JPA10EntityManagerEntityA entityFind1A = em.find(JPA10EntityManagerEntityA.class, id);
            Assert.assertNotNull("find(" + JPA10EntityManagerEntityA.class + ", " + id + ") did not return an entity.", entityFind1A);
            Assert.assertTrue(entityFind1A + " is not managed by the persistence context.", em.contains(entityFind1A));

            System.out.println("Performing detach(" + entityFind1A + ") operation");
            em.detach(entityFind1A);

            boolean removeFailed = false;
            try {
                System.out.println("Performing remove(" + entityFind1A + ") operation");
                em.remove(entityFind1A);
            } catch (IllegalArgumentException e) {
                // Expected if the instance is not an entity or is a detached entity
                System.out.println("remove(" + entityFind1A + ") failed with exception: " + e);
                removeFailed = true;
            }

            // JPA spec; 3.2.8:
            // contains method returns false:
            //     If the remove method has been called on the entity, or the remove operation has been cascaded to it.
            Assert.assertFalse(entityFind1A + " is managed by the persistence context.", em.contains(entityFind1A));

            boolean commitFailed = false;
            try {
                System.out.println("Committing transaction...");
                if (tj.isTransactionActive()) {
                    tj.commitTransaction();
                }
            } catch (Exception e) {
                // Expected if the instance is not an entity or is a detached entity
                System.out.println("transaction commit failed with exception: " + e);
                if (e.getCause() != null) {
                    System.out.println("exception cause: " + e.getCause());
                }
                commitFailed = true;
            }

            em.clear();

            // Verify the entity was not removed from the database after transaction commit
            System.out.println("Performing find(" + JPA10EntityManagerEntityA.class + ", " + id + ") operation");
            entityFind1A = em.find(JPA10EntityManagerEntityA.class, id);
            Assert.assertNotNull("find(" + JPA10EntityManagerEntityA.class + ", " + id + ") returned an entity.", entityFind1A);

            // JPA spec; 3.2.3:
            // If X is a detached entity, an IllegalArgumentException will be thrown by the remove operation (or the transaction commit will fail).
            Assert.assertTrue("Neither remove() nor transaction commit failed", removeFailed || commitFailed);
        } finally {
            System.out.println(testName + ": End");
        }
    }
}
