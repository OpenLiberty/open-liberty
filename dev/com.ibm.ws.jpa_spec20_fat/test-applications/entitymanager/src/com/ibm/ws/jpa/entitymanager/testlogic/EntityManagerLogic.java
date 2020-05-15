/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import org.junit.Assert;

import com.ibm.ws.jpa.entitymanager.model.JPA20EntityManagerDetachEntity;
import com.ibm.ws.jpa.entitymanager.model.JPA20EntityManagerEntityA;
import com.ibm.ws.jpa.entitymanager.model.JPA20EntityManagerEntityB;
import com.ibm.ws.jpa.entitymanager.model.JPA20EntityManagerEntityC;
import com.ibm.ws.jpa.entitymanager.model.JPA20EntityManagerFindEntity;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class EntityManagerLogic extends AbstractTestLogic {

    /**
     * Verify that calling EntityManager.detach() causes the targeted entity to become detached
     * from the persistence context. (3.2.6)
     */
    public void testDetach001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                              Object managedComponentObject) {
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

            System.out.println("Performing find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") operation");
            JPA20EntityManagerEntityA entityFind = em.find(JPA20EntityManagerEntityA.class, id);
            Assert.assertNotNull("find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") did not return an entity.", entityFind);
            Assert.assertTrue(entityFind + " is not managed by the persistence context.", em.contains(entityFind));

            System.out.println("Performing detach(" + entityFind + ") operation");
            em.detach(entityFind);
            Assert.assertFalse(entityFind + " is managed by the persistence context.", em.contains(entityFind));

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Verify that calling EntityManager.detach() causes the targeted entity and entity relationships with
     * cascade all and detach to become detached from the persistence context. (3.2.6)
     */
    public void testDetach002(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                              Object managedComponentObject) {
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

            int sharedID = 20;

            em.clear();
            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing find(" + JPA20EntityManagerDetachEntity.class.getSimpleName() + ", " + sharedID + ") operation");
            JPA20EntityManagerDetachEntity entity = em.find(JPA20EntityManagerDetachEntity.class, sharedID);

            // Verify JPA20EntityManagerDetachEntity(id=20)
            Assert.assertNotNull("find(" + JPA20EntityManagerDetachEntity.class.getSimpleName() + ", " + sharedID + ") did not return an entity.", entity);
            Assert.assertTrue(entity + " is not managed by the persistence context.", em.contains(entity));

            // Verify One to One entity relationships (entAO2O, entAO2O_CA, entAO2O_CD)
            Assert.assertNotNull(entity.getEntAO2O());
            Assert.assertTrue(entity.getEntAO2O() + " is not managed by the persistence context.", em.contains(entity.getEntAO2O()));
            Assert.assertNotNull(entity.getEntAO2O_CA());
            Assert.assertTrue(entity.getEntAO2O_CA() + " is not managed by the persistence context.", em.contains(entity.getEntAO2O_CA()));
            Assert.assertNotNull(entity.getEntAO2O_CD());
            Assert.assertTrue(entity.getEntAO2O_CD() + " is not managed by the persistence context.", em.contains(entity.getEntAO2O_CD()));

            // Verify Many to One entity relationships (entAM2O, entAM2O_CA, entAM2O_CD)
            Assert.assertNotNull(entity.getEntAM2O());
            Assert.assertTrue(entity.getEntAM2O() + " is not managed by the persistence context.", em.contains(entity.getEntAM2O()));
            Assert.assertNotNull(entity.getEntAM2O_CA());
            Assert.assertTrue(entity.getEntAM2O_CA() + " is not managed by the persistence context.", em.contains(entity.getEntAM2O_CA()));
            Assert.assertNotNull(entity.getEntAM2O_CD());
            Assert.assertTrue(entity.getEntAM2O_CD() + " is not managed by the persistence context.", em.contains(entity.getEntAM2O_CD()));

            // Verify One to Many entity relationships (entAM2O, entAM2O_CA, entAM2O_CD)
            HashSet<Integer> pkSet = new HashSet<Integer>(10);

            pkSet.clear();
            int index = 131;
            pkSet.add(new Integer(index));
            pkSet.add(new Integer(index + 1));
            Assert.assertTrue("JPA20EntityManagerDetachEntity.entAO2M should have 2 members in its relationship " +
                              "(has " + entity.getEntAO2MList().size() + ")",
                              entity.getEntAO2MList().size() == 2);
            for (JPA20EntityManagerEntityA entA : entity.getEntAO2MList()) {
                Integer pk = new Integer(entA.getId());
                Assert.assertTrue(pkSet.contains(pk));
                Assert.assertTrue(em.contains(entA));
                pkSet.remove(pk);
            }

            if (pkSet.size() != 0) {
                Assert.fail("The following identities of JPA20EntityManagerEntityA were missing from the relationship: " + pkSet);
            }

            pkSet.clear();
            index = 141;
            pkSet.add(new Integer(index));
            pkSet.add(new Integer(index + 1));
            Assert.assertTrue("JPA20EntityManagerDetachEntity.entAO2M should have 2 members in its relationship " +
                              "(has " + entity.getEntAO2MList_CA().size() + ")",
                              entity.getEntAO2MList_CA().size() == 2);
            for (JPA20EntityManagerEntityA entA : entity.getEntAO2MList_CA()) {
                Integer pk = new Integer(entA.getId());
                Assert.assertTrue(pkSet.contains(pk));
                Assert.assertTrue(em.contains(entA));
                pkSet.remove(pk);
            }
            if (pkSet.size() != 0) {
                Assert.fail("The following identities of JPA20EntityManagerEntityA were missing from the relationship: " + pkSet);
            }

            pkSet.clear();
            index = 151;
            pkSet.add(new Integer(index));
            pkSet.add(new Integer(index + 1));
            Assert.assertTrue("JPA20EntityManagerDetachEntity.entAO2M should have 2 members in its relationship " +
                              "(has " + entity.getEntAO2MList_CD().size() + ")",
                              entity.getEntAO2MList_CD().size() == 2);
            for (JPA20EntityManagerEntityA entA : entity.getEntAO2MList_CD()) {
                Integer pk = new Integer(entA.getId());
                Assert.assertTrue(pkSet.contains(pk));
                Assert.assertTrue(em.contains(entA));
                pkSet.remove(pk);
            }
            if (pkSet.size() != 0) {
                Assert.fail("The following identities of JPA20EntityManagerEntityA were missing from the relationship: " + pkSet);
            }

            // Verify Many to Many entity relationships (entAM2O, entAM2O_CA, entAM2O_CD)
            pkSet.clear();
            index = 101;
            pkSet.add(new Integer(index));
            pkSet.add(new Integer(index + 1));
            Assert.assertTrue("JPA20EntityManagerDetachEntity.entAO2M should have 2 members in its relationship " +
                              "(has " + entity.getEntAM2MList().size() + ")",
                              entity.getEntAM2MList().size() == 2);
            for (JPA20EntityManagerEntityA entA : entity.getEntAM2MList()) {
                Integer pk = new Integer(entA.getId());
                Assert.assertTrue(pkSet.contains(pk));
                Assert.assertTrue(em.contains(entA));
                pkSet.remove(pk);
            }
            if (pkSet.size() != 0) {
                Assert.fail("The following identities of JPA20EntityManagerEntityA were missing from the relationship: " + pkSet);
            }

            pkSet.clear();
            index = 111;
            pkSet.add(new Integer(index));
            pkSet.add(new Integer(index + 1));
            Assert.assertTrue("JPA20EntityManagerDetachEntity.entAO2M should have 2 members in its relationship " +
                              "(has " + entity.getEntAM2MList_CA().size() + ")",
                              entity.getEntAM2MList_CA().size() == 2);
            for (JPA20EntityManagerEntityA entA : entity.getEntAM2MList_CA()) {
                Integer pk = new Integer(entA.getId());
                Assert.assertTrue(pkSet.contains(pk));
                Assert.assertTrue(em.contains(entA));
                pkSet.remove(pk);
            }
            if (pkSet.size() != 0) {
                Assert.fail("The following identities of JPA20EntityManagerEntityA were missing from the relationship: " + pkSet);
            }

            pkSet.clear();
            index = 121;
            pkSet.add(new Integer(index));
            pkSet.add(new Integer(index + 1));
            Assert.assertTrue("JPA20EntityManagerDetachEntity.entAO2M should have 2 members in its relationship " +
                              "(has " + entity.getEntAM2MList_CD().size() + ")",
                              entity.getEntAM2MList_CD().size() == 2);
            for (JPA20EntityManagerEntityA entA : entity.getEntAM2MList_CD()) {
                Integer pk = new Integer(entA.getId());
                Assert.assertTrue(pkSet.contains(pk));
                Assert.assertTrue(em.contains(entA));
                pkSet.remove(pk);
            }
            if (pkSet.size() != 0) {
                Assert.fail("The following identities of JPA20EntityManagerEntityA were missing from the relationship: " + pkSet);
            }

            // Verified that all of the entities expected are present, and that they are all managed by the
            // persistence context.  Now call em.detach on JPA20EntityManagerDetachEntity(id=20) and examine which
            // entities were detached because of the cascade, and which ones are left managed by the persistence
            // context.
            System.out.println("2) Performing detach operation");
            em.detach(entity);

            Assert.assertFalse(entity + " is managed by the persistence context.", em.contains(entity));

            //  Verify One to One entity relationships (entAO2O, entAO2O_CA, entAO2O_CD)
            Assert.assertTrue(entity.getEntAO2O() + " is not managed by the persistence context.", em.contains(entity.getEntAO2O()));
            Assert.assertFalse(entity.getEntAO2O_CA() + " is managed by the persistence context.", em.contains(entity.getEntAO2O_CA()));
            Assert.assertFalse(entity.getEntAO2O_CD() + " is managed by the persistence context.", em.contains(entity.getEntAO2O_CD()));

            // Verify Many to One entity relationships (entAM2O, entAM2O_CA, entAM2O_CD)
            Assert.assertTrue("JPA20EntityManagerDetachEntity(id=" + sharedID + ").entAM2O is detached",
                              em.contains(entity.getEntAM2O()));
            Assert.assertTrue(entity.getEntAM2O() + " is not managed by the persistence context.", em.contains(entity.getEntAM2O()));
            Assert.assertFalse(entity.getEntAM2O_CA() + " is managed by the persistence context.", em.contains(entity.getEntAM2O_CA()));
            Assert.assertFalse(entity.getEntAM2O_CD() + " is managed by the persistence context.", em.contains(entity.getEntAM2O_CD()));

            // Verify One to Many entity relationships (entAM2O, entAM2O_CA, entAM2O_CD)
            for (JPA20EntityManagerEntityA entA : entity.getEntAO2MList()) {
                Integer pk = new Integer(entA.getId());
                Assert.assertTrue(entA + " is not managed by the persistence context.", em.contains(entA));
                pkSet.remove(pk);
            }

            for (JPA20EntityManagerEntityA entA : entity.getEntAO2MList_CA()) {
                Integer pk = new Integer(entA.getId());
                Assert.assertFalse(entA + " is managed by the persistence context.", em.contains(entA));
                pkSet.remove(pk);
            }

            for (JPA20EntityManagerEntityA entA : entity.getEntAO2MList_CD()) {
                Integer pk = new Integer(entA.getId());
                Assert.assertFalse(entA + " is managed by the persistence context.", em.contains(entA));
                pkSet.remove(pk);
            }

            // Verify Many to Many entity relationships (entAM2M, entAM2M_CA, entAM2M_CD)
            for (JPA20EntityManagerEntityA entA : entity.getEntAO2MList()) {
                Integer pk = new Integer(entA.getId());
                Assert.assertTrue(entA + " is not managed by the persistence context.", em.contains(entA));
                pkSet.remove(pk);
            }

            for (JPA20EntityManagerEntityA entA : entity.getEntAO2MList_CA()) {
                Integer pk = new Integer(entA.getId());
                Assert.assertFalse(entA + " is managed by the persistence context.", em.contains(entA));
                pkSet.remove(pk);
            }

            for (JPA20EntityManagerEntityA entA : entity.getEntAO2MList_CD()) {
                Integer pk = new Integer(entA.getId());
                Assert.assertFalse(entA + " is managed by the persistence context.", em.contains(entA));
                pkSet.remove(pk);
            }

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Verify that calling EntityManager.detach() on a new entity will not result with
     * the entity being persisted to the database, unless it was flushed before being detached. (3.2.6)
     */
    public void testDetach003(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                              Object managedComponentObject) {
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

            int id = 30, id2 = 31;

            // This test expects no populated test data.
            // Make sure the entity was not left in the database by previous tests.
            System.out.println("Performing find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") operation");
            JPA20EntityManagerEntityA find1 = em.find(JPA20EntityManagerEntityA.class, id);
            Assert.assertNull("find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") returned an entity.", find1);
            System.out.println("Performing find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id2 + ") operation");
            JPA20EntityManagerEntityA find2 = em.find(JPA20EntityManagerEntityA.class, id2);
            Assert.assertNull("find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id2 + ") returned an entity.", find2);

            em.clear();

            // Create the entity object, persist it, and then detach it without any flush(). At the same time,
            // create a second entity object that is not targeted for detach; it should still be managed
            // when the first entity object is detached.
            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            JPA20EntityManagerEntityA entity = new JPA20EntityManagerEntityA();
            entity.setId(id);
            entity.setStrData("A String");
            em.persist(entity);

            JPA20EntityManagerEntityA entity2 = new JPA20EntityManagerEntityA();
            entity2.setId(id2);
            entity2.setStrData("A String");
            em.persist(entity2);

            Assert.assertTrue(entity + " is not managed by the persistence context.", em.contains(entity));
            Assert.assertTrue(entity2 + " is not managed by the persistence context.", em.contains(entity2));

            System.out.println("Performing detach(" + entity + ") operation");
            em.detach(entity);

            Assert.assertFalse(entity + " is managed by the persistence context.", em.contains(entity));
            Assert.assertTrue(entity2 + " is not managed by the persistence context.", em.contains(entity2));

            System.out.println("Performing flush() operation");
            em.flush();
            System.out.println("Performing detach(" + entity2 + ") operation");
            em.detach(entity2);

            Assert.assertFalse(entity2 + " is managed by the persistence context.", em.contains(entity2));

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // The first entity was detached before the flush(), so it should not exist
            System.out.println("Performing find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") operation");
            JPA20EntityManagerEntityA entityFind1 = em.find(JPA20EntityManagerEntityA.class, id);
            Assert.assertNull(entityFind1);
            // The second entity was detached after the flush(), so it should exist
            System.out.println("Performing find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id2 + ") operation");
            JPA20EntityManagerEntityA entityFind2 = em.find(JPA20EntityManagerEntityA.class, id2);
            Assert.assertNotNull(entityFind2);

            // Clean up what was created in the database by this test
            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id2 + ") operation");
            JPA20EntityManagerEntityA entityRemove2 = em.find(JPA20EntityManagerEntityA.class, id2);
            Assert.assertNotNull(entityRemove2);

            System.out.println("Performing remove(" + entityRemove2 + ") operation");
            em.remove(entityRemove2);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Performing find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id2 + ") operation");
            entityRemove2 = em.find(JPA20EntityManagerEntityA.class, id2);
            Assert.assertNull("find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id2 + ") returned an entity.", entityRemove2);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Verify that calling EntityManager.detach() on a mutated entity will result in the mutations not being
     * persisted to the database. (3.2.6)
     */
    public void testDetach004(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                              Object managedComponentObject) {
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

            int id = 40;

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") operation");
            JPA20EntityManagerEntityA entityFind1A = em.find(JPA20EntityManagerEntityA.class, id);
            Assert.assertNotNull("find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") did not return an entity.", entityFind1A);
            Assert.assertTrue(entityFind1A + " is not managed by the persistence context.", em.contains(entityFind1A));
            Assert.assertTrue(entityFind1A + " should have strVal \"A String\"" +
                              " (is " + entityFind1A.getStrData() + ")",
                              "A String".equals(entityFind1A.getStrData()));

//          Mutating the persistent state of JPA20EntityManagerEntityA(id=1) to have strData \"Mutated\"...
            entityFind1A.setStrData("Mutated");

//          Calling em.detach(entity), which should cause JPA20EntityManagerEntityA(id=" + id + ") to become detached.
            System.out.println("Performing detach(" + entityFind1A + ") operation");
            em.detach(entityFind1A);

            Assert.assertFalse(entityFind1A + " is managed by the persistence context.", em.contains(entityFind1A));

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Performing find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") operation");
            JPA20EntityManagerEntityA entityFind1B = em.find(JPA20EntityManagerEntityA.class, id);
            Assert.assertNotNull("find(" + JPA20EntityManagerEntityA.class.getSimpleName() + "," + id + ") did not return an entity.", entityFind1B);

            Assert.assertTrue(entityFind1B + " should have strVal \"A String\"" +
                              " (is " + entityFind1B.getStrData() + ")",
                              "A String".equals(entityFind1B.getStrData()));
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Verify that calling EntityManager.detach() on a mutated entity, following a flush(), will result in the mutations
     * being persisted to the database. (3.2.6)
     */
    public void testDetach005(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                              Object managedComponentObject) {
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

            int id = 50;

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") operation");
            JPA20EntityManagerEntityA entityFind1A = em.find(JPA20EntityManagerEntityA.class, id);
            Assert.assertNotNull("find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") did not return an entity.", entityFind1A);
            Assert.assertTrue(entityFind1A + " is not managed by the persistence context.", em.contains(entityFind1A));
            Assert.assertTrue(entityFind1A + " should have strVal \"A String\"" +
                              " (is \"" + entityFind1A.getStrData() + "\")",
                              "A String".equals(entityFind1A.getStrData()));

//          Mutating the persistent states of JPA20EntityManagerEntityA(id=" + id + ") to have strData \"Mutated\"...
            entityFind1A.setStrData("Mutated");

            System.out.println("Performing flush() operation");
            em.flush();

//          Calling em.detach(entity), which should cause JPA20EntityManagerEntityA(id=" + id + ") to become detached."
            System.out.println("Performing detach(" + entityFind1A + ") operation");
            em.detach(entityFind1A);

            Assert.assertFalse(entityFind1A + " is managed by the persistence context.", em.contains(entityFind1A));

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") operation");
            JPA20EntityManagerEntityA entityFind1B = em.find(JPA20EntityManagerEntityA.class, id);
            Assert.assertNotNull("find(" + JPA20EntityManagerEntityA.class.getSimpleName() + "," + id + ") did not return an entity.", entityFind1B);
            Assert.assertTrue(entityFind1B + " is not managed by the persistence context.", em.contains(entityFind1B));
            Assert.assertTrue(entityFind1B + " should have strVal \"Mutated\"" +
                              " (is \"" + entityFind1B.getStrData() + "\")",
                              "Mutated".equals(entityFind1B.getStrData()));

            // Revert the change made to the database
            entityFind1B.setStrData("A String");

            System.out.println("Performing flush() operation");
            em.flush();

//          Calling em.detach(entity), which should cause JPA20EntityManagerEntityA(id=" + id + ") to become detached."
            System.out.println("Performing detach(" + entityFind1B + ") operation");
            em.detach(entityFind1B);

            Assert.assertFalse(entityFind1B + " is managed by the persistence context.", em.contains(entityFind1B));

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Performing find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") operation");
            JPA20EntityManagerEntityA entityFind1C = em.find(JPA20EntityManagerEntityA.class, id);
            Assert.assertNotNull("find(" + JPA20EntityManagerEntityA.class.getSimpleName() + "," + id + ") did not return an entity.", entityFind1C);
            Assert.assertTrue(entityFind1C + " is not managed by the persistence context.", em.contains(entityFind1C));
            Assert.assertTrue(entityFind1C + " should have strVal \"A String\"" +
                              " (is " + entityFind1C.getStrData() + ")",
                              "A String".equals(entityFind1C.getStrData()));
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Verify that calling EntityManager.detach() on a removed entity will result in the removal not being
     * persisted to the database. (3.2.6)
     */
    public void testDetach006(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                              Object managedComponentObject) {
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

            int id = 60;

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") operation");
            JPA20EntityManagerEntityA entityFind1A = em.find(JPA20EntityManagerEntityA.class, id);
            Assert.assertNotNull("find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") did not return an entity.", entityFind1A);
            Assert.assertTrue(entityFind1A + " is not managed by the persistence context.", em.contains(entityFind1A));

            System.out.println("Performing remove(" + entityFind1A + ") operation");
            em.remove(entityFind1A);

            System.out.println("Performing detach(" + entityFind1A + ") operation");
            em.detach(entityFind1A);

            // JPA spec; 3.2.8:
            // contains method returns false:
            //     If the instance is detached.
            Assert.assertFalse(entityFind1A + " is managed by the persistence context.", em.contains(entityFind1A));

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Performing find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") operation");
            entityFind1A = em.find(JPA20EntityManagerEntityA.class, id);
            Assert.assertNotNull("find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") did not return an entity.", entityFind1A);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Verify that calling EntityManager.detach() on a removed entity, following a flush(), will result in the removal
     * being persisted to the database. (3.2.6)
     */
    public void testDetach007(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                              Object managedComponentObject) {
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

            int id = 70;

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") operation");
            JPA20EntityManagerEntityA entityFind1A = em.find(JPA20EntityManagerEntityA.class, id);
            Assert.assertNotNull("find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") did not return an entity.", entityFind1A);
            Assert.assertTrue(entityFind1A + " is not managed by the persistence context.", em.contains(entityFind1A));

            System.out.println("Performing remove(" + entityFind1A + ") operation");
            em.remove(entityFind1A);

            System.out.println("Performing flush() operation");
            em.flush();

            System.out.println("Performing detach(" + entityFind1A + ") operation");
            em.detach(entityFind1A);

            Assert.assertFalse(entityFind1A + " is managed by the persistence context.", em.contains(entityFind1A));

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Performing find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") operation");
            JPA20EntityManagerEntityA entityFind1B = em.find(JPA20EntityManagerEntityA.class, id);
            Assert.assertNull("find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") returned an entity.", entityFind1B);

            // Put the entity back after the test removed it
            // Portable applications should not pass removed entities that have been detached
            // from the persistence context to further Entity-Manager operations. (3.2.6)
            JPA20EntityManagerEntityA entityPersist1A = new JPA20EntityManagerEntityA();
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

            System.out.println("Performing find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") operation");
            JPA20EntityManagerEntityA entityFind1C = em.find(JPA20EntityManagerEntityA.class, id);
            Assert.assertNotNull("find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") did not return an entity.", entityFind1C);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Verify that calling EntityManager.detach() on a removed entity will still cascade the detach operation
     * across relationships marked with DETACH or ALL CascadeType (3.2.6)
     */
    public void testDetach008(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                              Object managedComponentObject) {
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

            int sharedID = 20;

            try {
                em.clear();
                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                System.out.println("Performing find(" + JPA20EntityManagerDetachEntity.class.getSimpleName() + ", " + sharedID + ") operation");
                JPA20EntityManagerDetachEntity entity = em.find(JPA20EntityManagerDetachEntity.class, sharedID);

                Assert.assertNotNull(entity);
                Assert.assertTrue(entity + " is not managed by the persistence context.", em.contains(entity));

                // Verify One to One entity relationships (entAO2O, entAO2O_CA, entAO2O_CD)
                Assert.assertNotNull(entity.getEntAO2O());
                Assert.assertTrue("JPA20EntityManagerDetachEntity(id=" + sharedID + ").entAO2O is detached",
                                  em.contains(entity.getEntAO2O()));
                Assert.assertNotNull(entity.getEntAO2O_CA());
                Assert.assertTrue("JPA20EntityManagerDetachEntity(id=" + sharedID + ").entAO2O_CA is detached",
                                  em.contains(entity.getEntAO2O_CA()));
                Assert.assertNotNull(entity.getEntAO2O_CD());
                Assert.assertTrue("JPA20EntityManagerDetachEntity(id=" + sharedID + ").entAO2O_CD is detached",
                                  em.contains(entity.getEntAO2O_CD()));

                // Verify Many to One entity relationships (entAM2O, entAM2O_CA, entAM2O_CD)
                Assert.assertNotNull(entity.getEntAM2O());
                Assert.assertTrue("JPA20EntityManagerDetachEntity(id=" + sharedID + ").entAM2O is detached",
                                  em.contains(entity.getEntAM2O()));
                Assert.assertNotNull(entity.getEntAM2O_CA());
                Assert.assertTrue("JPA20EntityManagerDetachEntity(id=" + sharedID + ").entAM2O_CA is not detached",
                                  em.contains(entity.getEntAM2O_CA()));
                Assert.assertNotNull(entity.getEntAM2O_CD());
                Assert.assertTrue("JPA20EntityManagerDetachEntity(id=" + sharedID + ").entAM2O_CD is not detached",
                                  em.contains(entity.getEntAM2O_CD()));

                // Verify One to Many entity relationships (entAM2O, entAM2O_CA, entAM2O_CD)
                HashSet<Integer> pkSet = new HashSet<Integer>(10);

                pkSet.clear();
                int index = 131;
                pkSet.add(new Integer(index));
                pkSet.add(new Integer(index + 1));
                Assert.assertTrue("JPA20EntityManagerDetachEntity.entAO2M should have 2 members in its relationship " +
                                  "(has " + entity.getEntAO2MList().size() + ")",
                                  entity.getEntAO2MList().size() == 2);
                for (JPA20EntityManagerEntityA entA : entity.getEntAO2MList()) {
                    Integer pk = new Integer(entA.getId());
                    Assert.assertTrue(pkSet.contains(pk));
                    Assert.assertTrue(em.contains(entA));
                    pkSet.remove(pk);
                }
                if (pkSet.size() != 0) {
                    Assert.fail("The following identities of JPA20EntityManagerEntityA were missing from the relationship: " + pkSet);
                }

                pkSet.clear();
                index = 141;
                pkSet.add(new Integer(index));
                pkSet.add(new Integer(index + 1));
                Assert.assertTrue("JPA20EntityManagerDetachEntity.entAO2M should have 2 members in its relationship " +
                                  "(has " + entity.getEntAO2MList_CA().size() + ")",
                                  entity.getEntAO2MList_CA().size() == 2);
                for (JPA20EntityManagerEntityA entA : entity.getEntAO2MList_CA()) {
                    Integer pk = new Integer(entA.getId());
                    Assert.assertTrue(pkSet.contains(pk));
                    Assert.assertTrue(em.contains(entA));
                    pkSet.remove(pk);
                }
                if (pkSet.size() != 0) {
                    Assert.fail("The following identities of JPA20EntityManagerEntityA were missing from the relationship: " + pkSet);
                }

                pkSet.clear();
                index = 151;
                pkSet.add(new Integer(index));
                pkSet.add(new Integer(index + 1));
                Assert.assertTrue("JPA20EntityManagerDetachEntity.entAO2M_CD should have 2 members in its relationship " +
                                  "(has " + entity.getEntAO2MList_CD().size() + ")",
                                  entity.getEntAO2MList_CD().size() == 2);
                for (JPA20EntityManagerEntityA entA : entity.getEntAO2MList_CD()) {
                    Integer pk = new Integer(entA.getId());
                    Assert.assertTrue(pkSet.contains(pk));
                    Assert.assertTrue(em.contains(entA));
                    pkSet.remove(pk);
                }
                if (pkSet.size() != 0) {
                    Assert.fail("The following identities of JPA20EntityManagerEntityA were missing from the relationship: " + pkSet);
                }

                // Verify Many to Many entity relationships (entAM2O, entAM2O_CA, entAM2O_CD)
                pkSet.clear();
                index = 101;
                pkSet.add(new Integer(index));
                pkSet.add(new Integer(index + 1));
                Assert.assertTrue("JPA20EntityManagerDetachEntity.entAM2M should have 2 members in its relationship " +
                                  "(has " + entity.getEntAM2MList().size() + ")",
                                  entity.getEntAM2MList().size() == 2);
                for (JPA20EntityManagerEntityA entA : entity.getEntAM2MList()) {
                    Integer pk = new Integer(entA.getId());
                    Assert.assertTrue(pkSet.contains(pk));
                    Assert.assertTrue(em.contains(entA));
                    pkSet.remove(pk);
                }
                if (pkSet.size() != 0) {
                    Assert.fail("The following identities of JPA20EntityManagerEntityA were missing from the relationship: " + pkSet);
                }

                pkSet.clear();
                index = 111;
                pkSet.add(new Integer(index));
                pkSet.add(new Integer(index + 1));
                Assert.assertTrue("JPA20EntityManagerDetachEntity.entAM2M_CA should have 2 members in its relationship " +
                                  "(has " + entity.getEntAM2MList_CA().size() + ")",
                                  entity.getEntAM2MList_CA().size() == 2);
                for (JPA20EntityManagerEntityA entA : entity.getEntAM2MList_CA()) {
                    Integer pk = new Integer(entA.getId());
                    Assert.assertTrue(pkSet.contains(pk));
                    Assert.assertTrue(em.contains(entA));
                    pkSet.remove(pk);
                }
                if (pkSet.size() != 0) {
                    Assert.fail("The following identities of JPA20EntityManagerEntityA were missing from the relationship: " + pkSet);
                }

                pkSet.clear();
                index = 121;
                pkSet.add(new Integer(index));
                pkSet.add(new Integer(index + 1));
                Assert.assertTrue("JPA20EntityManagerDetachEntity.entAM2M_CD should have 2 members in its relationship " +
                                  "(has " + entity.getEntAM2MList_CD().size() + ")",
                                  entity.getEntAM2MList_CD().size() == 2);
                for (JPA20EntityManagerEntityA entA : entity.getEntAM2MList_CD()) {
                    Integer pk = new Integer(entA.getId());
                    Assert.assertTrue(pkSet.contains(pk));
                    Assert.assertTrue(em.contains(entA));
                    pkSet.remove(pk);
                }
                if (pkSet.size() != 0) {
                    Assert.fail("The following identities of JPA20EntityManagerEntityA were missing from the relationship: " + pkSet);
                }

                // Verified that all of the entities expected are present, and that they are all managed by the
                // persistence context.  Now call em.remove, then em.detach on JPA20EntityManagerDetachEntity(id=" + id + ") and examine which
                // entities were detached because of the cascade, and which ones are left managed by the persistence
                // context.
                System.out.println("2) Performing remove operation");
                em.remove(entity);
                System.out.println("3) Performing detach operation");
                em.detach(entity);

                Assert.assertFalse("JPA20EntityManagerDetachEntity(id=" + sharedID + ") is not detached.", em.contains(entity));

                //  Verify One to One entity relationships (entAO2O, entAO2O_CA, entAO2O_CD)
                Assert.assertTrue("JPA20EntityManagerDetachEntity(id=" + sharedID + ").entAO2O is detached",
                                  em.contains(entity.getEntAO2O()));
                Assert.assertFalse("JPA20EntityManagerDetachEntity(id=" + sharedID + ").entAO2O_CA is not detached",
                                   em.contains(entity.getEntAO2O_CA()));
                Assert.assertFalse("JPA20EntityManagerDetachEntity(id=" + sharedID + ").entAO2O_CD is not detached",
                                   em.contains(entity.getEntAO2O_CD()));

                // Verify Many to One entity relationships (entAM2O, entAM2O_CA, entAM2O_CD)
                Assert.assertTrue("JPA20EntityManagerDetachEntity(id=" + sharedID + ").entAM2O is detached",
                                  em.contains(entity.getEntAM2O()));
                Assert.assertFalse("JPA20EntityManagerDetachEntity(id=" + sharedID + ").entAM2O_CA is not detached",
                                   em.contains(entity.getEntAM2O_CA()));
                Assert.assertFalse("JPA20EntityManagerDetachEntity(id=" + sharedID + ").entAM2O_CD is not detached",
                                   em.contains(entity.getEntAM2O_CD()));

                // Verify One to Many entity relationships (entAM2O, entAM2O_CA, entAM2O_CD)
                for (JPA20EntityManagerEntityA entA : entity.getEntAO2MList()) {
                    Integer pk = new Integer(entA.getId());
                    Assert.assertTrue(em.contains(entA));
                    pkSet.remove(pk);
                }

                for (JPA20EntityManagerEntityA entA : entity.getEntAO2MList_CA()) {
                    Integer pk = new Integer(entA.getId());
                    Assert.assertFalse(em.contains(entA));
                    pkSet.remove(pk);
                }

                for (JPA20EntityManagerEntityA entA : entity.getEntAO2MList_CD()) {
                    Integer pk = new Integer(entA.getId());
                    Assert.assertFalse(em.contains(entA));
                    pkSet.remove(pk);
                }

                // Verify Many to Many entity relationships (entAM2M, entAM2M_CA, entAM2M_CD)
                for (JPA20EntityManagerEntityA entA : entity.getEntAO2MList()) {
                    Integer pk = new Integer(entA.getId());
                    Assert.assertTrue(em.contains(entA));
                    pkSet.remove(pk);
                }

                for (JPA20EntityManagerEntityA entA : entity.getEntAO2MList_CA()) {
                    Integer pk = new Integer(entA.getId());
                    Assert.assertFalse(em.contains(entA));
                    pkSet.remove(pk);
                }

                for (JPA20EntityManagerEntityA entA : entity.getEntAO2MList_CD()) {
                    Integer pk = new Integer(entA.getId());
                    Assert.assertFalse(em.contains(entA));
                    pkSet.remove(pk);
                }

                System.out.println("Committing transaction...");
                if (tj.isTransactionActive()) {
                    tj.commitTransaction();
                }
            } catch (Exception e) {
                try {
                    tj.rollbackTransaction();
                } catch (Throwable t) {
                    // Swallow
                }
                throw (e);
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Verify that passing null to EntityManager.detach() throws an IllegalArgumentException
     */
    public void testDetach009(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                              Object managedComponentObject) {
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

            try {
                System.out.println("Performing detach(" + null + ") operation");
                em.detach(null);
                Assert.fail("No Exception was thrown by the call to em.detach().");
            } catch (IllegalArgumentException iae) {
                //Caught the expected IllegalArgumentException.
            } catch (Exception e) {
                Assert.fail("detach(null) should have thrown an IllegalArgumentException");
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Verify that entities referring to an entity targeted by an em.detach() operation in a relationship still
     * reference the detached entity.
     */
    public void testDetach010(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                              Object managedComponentObject) {
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

            int id = 100;

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Find JPA20EntityManagerEntityA, and read through its relationships to ensure all entities are rehydrated
            System.out.println("Performing find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") operation");
            JPA20EntityManagerEntityA entityA = em.find(JPA20EntityManagerEntityA.class, id);
            Assert.assertNotNull("find(" + JPA20EntityManagerEntityA.class.getSimpleName() + ", " + id + ") did not return an entity.", entityA);
            Assert.assertTrue(entityA + " is not managed by the persistence context.", em.contains(entityA));

            JPA20EntityManagerEntityB entityB = entityA.getEntityBList().get(0);
            Assert.assertNotNull("JPA20EntityManagerEntityA(id=" + id + ") should have a reference to JPA20EntityManagerEntityB(id=" + id + ")", entityB);
            Assert.assertTrue(entityB + " is not managed by the persistence context.", em.contains(entityB));
            if (entityB != null) {
                // Preload the lazy loaded field.
                entityB.getEntityAList().get(0);
            }

            JPA20EntityManagerEntityC entityC = entityA.getEntityC();
            Assert.assertNotNull("JPA20EntityManagerEntityA(id=" + id + ") should have a reference to JPA20EntityManagerEntityC(id=" + id + ")", entityC);
            Assert.assertTrue(entityC + " is not managed by the persistence context.", em.contains(entityC));

            System.out.println("Performing detach(" + entityA + ") operation");
            em.detach(entityA);
            Assert.assertFalse(entityA + " is managed by the persistence context.", em.contains(entityA));

            // Verify JPA20EntityManagerEntityB(id=" + id + ") refers to the detached JPA20EntityManagerEntityA instance
            Assert.assertTrue("JPA20EntityManagerEntityB(id=" + id + ") should refer to the detached JPA20EntityManagerEntityA(id=" + id + ")",
                              entityB.getEntityAList().get(0) == entityA);

            // Verify JPA20EntityManagerEntityC refers to the detached JPA20EntityManagerEntityA instance
            Assert.assertTrue("JPA20EntityManagerEntityC(id=" + id + ") should refer to the detached JPA20EntityManagerEntityA(id=" + id + ")",
                              entityC.getEntityA() == entityA);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Verify that setting the properties argument to null does not throw any exceptions or disrupt the successful
     * operation of the find() command. Variants: test with find(Class, Object, Map) and
     * find(Class, Object, Map, LockModeType).
     */
    public void testFind001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                            Object managedComponentObject) {
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

            int emp1_ID = 1;
            int emp2_ID = 2;

            Map<String, Object> nullMap = null;
            try {

                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                // Test find(Class, Object, Map)
                System.out.println("Performing find(" + JPA20EntityManagerFindEntity.class + ", " + emp1_ID + ") operation");
                JPA20EntityManagerFindEntity emp1 = em.find(JPA20EntityManagerFindEntity.class, emp1_ID, nullMap);
                Assert.assertNotNull("Find() did not successfully return an entity object", emp1);

                // Test find(Class, Object, Map, LockModeType)
                System.out.println("Performing find(" + JPA20EntityManagerFindEntity.class + ", " + emp2_ID + ") operation");
                JPA20EntityManagerFindEntity emp2 = em.find(JPA20EntityManagerFindEntity.class, emp2_ID, LockModeType.PESSIMISTIC_WRITE, nullMap);
                Assert.assertNotNull("Find() did not successfully return an entity object", emp2);

                System.out.println("Committing transaction...");
                if (tj.isTransactionActive()) {
                    tj.commitTransaction();
                }
            } catch (Exception e) {
                try {
                    tj.rollbackTransaction();
                } catch (Throwable t) {
                    // Swallow
                }
                throw (e);
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Verify that setting the properties argument to an unrecognized value does not throw any exceptions or
     * disrupt the successful operation of the find() command. Variants: test with find(Class, Object, Map) and
     * find(Class, Object, Map, LockModeType).
     */
    public void testFind002(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                            Object managedComponentObject) {
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

            int emp1_ID = 1;
            int emp2_ID = 2;

            Map<String, Object> bogusMap = new HashMap<String, Object>();
            bogusMap.put("bogus.key", "bogus.value");
            try {

                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                // Test find(Class, Object, Map)
                System.out.println("Performing find(" + JPA20EntityManagerFindEntity.class + ", " + emp1_ID + ") operation");
                JPA20EntityManagerFindEntity emp1 = em.find(JPA20EntityManagerFindEntity.class, emp1_ID, bogusMap);
                Assert.assertNotNull("Find() did not successfully return an entity object", emp1);

                // Test find(Class, Object, Map, LockModeType)
                System.out.println("Performing find(" + JPA20EntityManagerFindEntity.class + ", " + emp2_ID + ") operation");
                JPA20EntityManagerFindEntity emp2 = em.find(JPA20EntityManagerFindEntity.class, emp2_ID, LockModeType.PESSIMISTIC_WRITE, bogusMap);
                Assert.assertNotNull("Find() did not successfully return an entity object", emp2);

                System.out.println("Committing transaction...");
                if (tj.isTransactionActive()) {
                    tj.commitTransaction();
                }
            } catch (Exception e) {
                try {
                    tj.rollbackTransaction();
                } catch (Throwable t) {
                    // Swallow
                }
                throw (e);
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Verify that setting the properties argument to an empty map does not throw any exceptions or
     * disrupt the successful operation of the find() command. Variants: test with find(Class, Object, Map) and
     * find(Class, Object, Map, LockModeType).
     */
    public void testFind003(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                            Object managedComponentObject) {
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

            int emp1_ID = 1;
            int emp2_ID = 2;

            Map<String, Object> emptyMap = new HashMap<String, Object>();
            try {

                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                // Test find(Class, Object, Map)
                System.out.println("Performing find(" + JPA20EntityManagerFindEntity.class + ", " + emp1_ID + ") operation");
                JPA20EntityManagerFindEntity emp1 = em.find(JPA20EntityManagerFindEntity.class, emp1_ID, emptyMap);
                Assert.assertNotNull("Find() did not successfully return an entity object", emp1);

                // Test find(Class, Object, Map, LockModeType)
                System.out.println("Performing find(" + JPA20EntityManagerFindEntity.class + ", " + emp2_ID + ") operation");
                JPA20EntityManagerFindEntity emp2 = em.find(JPA20EntityManagerFindEntity.class, emp2_ID, LockModeType.PESSIMISTIC_WRITE, emptyMap);
                Assert.assertNotNull("Find() did not successfully return an entity object", emp2);

                System.out.println("Committing transaction...");
                if (tj.isTransactionActive()) {
                    tj.commitTransaction();
                }
            } catch (Exception e) {
                try {
                    tj.rollbackTransaction();
                } catch (Throwable t) {
                    // Swallow
                }
                throw (e);
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }
}
