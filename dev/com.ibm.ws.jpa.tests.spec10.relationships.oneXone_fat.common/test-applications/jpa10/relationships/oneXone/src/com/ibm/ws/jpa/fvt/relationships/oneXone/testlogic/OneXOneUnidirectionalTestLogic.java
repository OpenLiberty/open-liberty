/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.oneXone.testlogic;

import javax.persistence.PersistenceException;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.ICardinalEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.ICardinalEntityB;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.IEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.IEntityB;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.INoOptEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.INoOptEntityB;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class OneXOneUnidirectionalTestLogic extends AbstractTestLogic {
    /**
     * Verify basic One-to-One service by creating UniEntityA and UniEntityB, and
     * link UniEntityA.defaultRelationship to the instance of UniEntityB.
     *
     * Test verifies function performing the following:
     * 1) Create new UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting defaultRelationship to UniEntityB(id=1).
     * 3) Keep other (optional) OneXOne fields set to null.
     * 4) Clear persistence context (if necessary), and find UniEntityA(id=1).
     * 5) Verify field data in UniEntityA(id=1)
     * 6) Verify field data in UniEntityA.defaultRelationship (should reference UniEntityB(id=1))
     * 7) Test passes if all these conditions are met.
     *
     * This test case confirms the following function verification:
     * 1) Optional relational fields can be set to null without persistence errors
     * (all fields except defaultRelationship are null in UniEntityA(id=1))
     * 2) A traversable OneXOne association between UniEntityA to UniEntityB(id=1) is
     * established, and UniEntityB is accessible through the relational
     * reference defaultRelationship in UniEntityA.
     * 3) The default FetchMode, EAGER, for defaultRelationship should make all of UniEntityB(id=1)'s
     * data available for access, even after the entities have been detached
     * from the persistence context.
     *
     * 9 POINTS
     */
    public void testOneXOneUni001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXOneUnidirectionalTestLogic.testOneXOneUni001(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityAName = (String) testExecCtx.getProperties().get("EntityAName");
        OneXOneUnidirectionalEntityEnum targetEntityAType = OneXOneUnidirectionalEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXOneUnidirectionalEntityEnum targetEntityBType = OneXOneUnidirectionalEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXOneUnidirectionalTestLogic.testOneXOneUni001(): Begin");

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Construct a new entity instances
            System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() + " (id=1)...");
            IEntityB new_entityB = (IEntityB) constructNewEntityObject(targetEntityBType);
            new_entityB.setId(1);
            new_entityB.setName("Entity B");

            System.out.println("Persisting " + new_entityB);
            jpaResource.getEm().persist(new_entityB);

            System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() + " (id=1)...");
            IEntityA new_entityA = (IEntityA) constructNewEntityObject(targetEntityAType);
            new_entityA.setId(1);
            new_entityA.setName("Entity A");

            System.out.println("Creating relationship between " + targetEntityAType.getEntityName() + " and " +
                               targetEntityBType.getEntityName() + " via the 'b1' relationship field...");
            new_entityA.setB1Field(new_entityB);

            System.out.println("Persisting " + new_entityA);
            jpaResource.getEm().persist(new_entityA);

            System.out.println("Both entities created, relationship established.  Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Start a transaction
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=1)...");
            IEntityA find_entityA = (IEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), 1);
            System.out.println("Object returned by find: " + find_entityA);

            // Verify that em.find() returned an object. (1 point)
            Assert.assertNotNull("Assert that the find operation did not return null", find_entityA);

            //  Perform basic verifications (3 points)
            Assert.assertNotSame(
                                 "Assert find did not return the original object",
                                 new_entityA,
                                 find_entityA);
            Assert.assertTrue(
                              "Assert entity returned by find is managed by the persistence context.",
                              jpaResource.getEm().contains(find_entityA));
            Assert.assertEquals(
                                "Assert that the entity's id is 1",
                                find_entityA.getId(),
                                1);

            // Clear persistence context to verify default eager loading behavior
            System.out.println("Clearing persistence context to verify default eager loading behavior...");
            jpaResource.getEm().clear();

            Assert.assertFalse("Assert that the entity is now detached.", jpaResource.getEm().contains(find_entityA));

            // Examine the b1 field of EntityA.  It should not be null, should have an id=1, and
            // its name field should have a value of "Entity B".
            System.out.println(
                               "Fetching " + targetEntityBType.getEntityName() +
                               "(id=1) from " + targetEntityBType.getEntityName() + "(id=1)'s b1 field...");

            IEntityB dr_entityB = find_entityA.getB1Field();
            Assert.assertNotNull(
                                 "Assert that an " + targetEntityBType.getEntityName() + " was extracted from the b1.",
                                 dr_entityB);
            Assert.assertNotSame(
                                 "Assert the extracted " + targetEntityBType.getEntityName() + " is not the same as the  original object",
                                 new_entityB,
                                 dr_entityB);
            Assert.assertFalse(
                               "Assert the extracted " + targetEntityBType.getEntityName() + " is not managed by the persistence context.",
                               jpaResource.getEm().contains(dr_entityB));
            Assert.assertEquals(
                                "Assert the extracted " + targetEntityBType.getEntityName() + "'s id is 1",
                                dr_entityB.getId(),
                                1);

            System.out.println("Testing complete, rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();

            System.out.println("Ending test.");
        } finally {
            System.out.println("OneXOneUnidirectionalTestLogic.testOneXOneUni001(): End");
        }
    }

    /**
     * Verify JoinColumn table name annotation
     *
     * Test verifies function performing the following:
     * 1) Create new UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting b2 to UniEntityB(id=1).
     * 3) Keep other (optional) OneXOne fields set to null.
     * 4) Clear persistence context (if necessary), and find UniEntityA(id=1).
     * 5) Verify field data in UniEntityA(id=1)
     * 6) Verify field data in UniEntityA.b2 (should reference UniEntityB(id=1))
     * 7) Test passes if all these conditions are met.
     *
     * This test case is virtually idential to testOneXOneUni001, only that the target
     * field used is marked with a column-name override in the JoinColumn annotation.
     */
    public void testOneXOneUni002(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXOneUnidirectionalTestLogic.testOneXOneUni002(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityAName = (String) testExecCtx.getProperties().get("EntityAName");
        OneXOneUnidirectionalEntityEnum targetEntityAType = OneXOneUnidirectionalEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXOneUnidirectionalEntityEnum targetEntityBType = OneXOneUnidirectionalEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXOneUnidirectionalTestLogic.testOneXOneUni002(): Begin");

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Construct a new entity instances
            System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() + " (id=1)...");
            IEntityB new_entityB = (IEntityB) constructNewEntityObject(targetEntityBType);
            new_entityB.setId(1);
            new_entityB.setName("Entity B");

            System.out.println("Persisting " + new_entityB);
            jpaResource.getEm().persist(new_entityB);

            System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() + " (id=1)...");
            IEntityA new_entityA = (IEntityA) constructNewEntityObject(targetEntityAType);
            new_entityA.setId(1);
            new_entityA.setName("Entity A");

            System.out.println("Creating relationship between " + targetEntityAType.getEntityName() + " and " +
                               targetEntityBType.getEntityName() + " via the 'b2' relationship field...");
            new_entityA.setB2Field(new_entityB);

            System.out.println("Persisting " + new_entityA);
            jpaResource.getEm().persist(new_entityA);

            System.out.println("Both entities created, relationship established.  Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Start a transaction
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=1)...");
            IEntityA find_entityA = (IEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), 1);
            System.out.println("Object returned by find: " + find_entityA);

            // Verify that em.find() returned an object. (1 point)
            Assert.assertNotNull("Assert that the find operation did not return null", find_entityA);

            //  Perform basic verifications (3 points)
            Assert.assertNotSame(
                                 "Assert find did not return the original object",
                                 new_entityA,
                                 find_entityA);
            Assert.assertTrue(
                              "Assert entity returned by find is managed by the persistence context.",
                              jpaResource.getEm().contains(find_entityA));
            Assert.assertEquals(
                                "Assert that the entity's id is 1",
                                find_entityA.getId(),
                                1);

            // Clear persistence context to verify default eager loading behavior
            System.out.println("Clearing persistence context to verify default eager loading behavior...");
            jpaResource.getEm().clear();

            Assert.assertFalse("Assert that the entity is now detached.", jpaResource.getEm().contains(find_entityA));

            // Examine the b2 field of EntityA.  It should not be null, should have an id=1, and
            // its name field should have a value of "Entity B".
            System.out.println(
                               "Fetching " + targetEntityBType.getEntityName() +
                               "(id=1) from " + targetEntityBType.getEntityName() + "(id=1)'s b2 field...");

            IEntityB dr_entityB = find_entityA.getB2Field();
            Assert.assertNotNull(
                                 "Assert that an " + targetEntityBType.getEntityName() + " was extracted from the b2 Relationship.",
                                 dr_entityB);
            Assert.assertNotSame(
                                 "Assert the extracted " + targetEntityBType.getEntityName() + " is not the same as the  original object",
                                 new_entityB,
                                 dr_entityB);
            Assert.assertFalse(
                               "Assert the extracted " + targetEntityBType.getEntityName() + " is not managed by the persistence context.",
                               jpaResource.getEm().contains(dr_entityB));
            Assert.assertEquals(
                                "Assert the extracted " + targetEntityBType.getEntityName() + "'s id is 1",
                                dr_entityB.getId(),
                                1);

            System.out.println("Testing complete, rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();

            System.out.println("Ending test.");
        } finally {
            System.out.println("OneXOneUnidirectionalTestLogic.testOneXOneUni002(): End");
        }
    }

    /**
     * Verify that optionality is enforced (a OneXOne relational reference cannot be set null
     * if its OneToOne annotation has the element/value pair optional=false).
     *
     * Test verifies function performing the following:
     * - Verify that an entity with a non-optional field cannot be created and saved to the db
     * with a null value in the non-optional field
     * 1) Create a new UniEntityANoOpt(id=1), setting all relational fields to null
     * 2) A persistence exception should be thrown while trying to persist UniEntityA(id=1)
     * to the database. Test Point passes if an exception is thrown on transaction commit.
     * - Verify that an existing entity with a non-optional field cannot change the optional field
     * to a null value and save it to the database.
     * 1) Create a new Create new UniEntityB(id=1)
     * 2) Create a new UniEntityANoOpt(id=1), setting b to UniEntityB(id=1). Save to the database
     * (not expecting any exceptions)
     * 3) Clear the persistence context, find UniEntityANoOpt(id=1)
     * 4) Set the b field on the UniEntityANoOpt(id=1) returned by find() to null. An exception
     * should be thrown when the transaction intended to save the changes to the db attempts to
     * commit. Test point passes if an exception is thrown.
     *
     * Michael D. Dick [ 4:55:46 PM ]
     * Back to Q1 : OpenJPA will throw an org.apache.openjpa.utilInvalidStateException,
     * which is not a subclass of PersistenceException.
     *
     * 3 POINTS
     */
    public void testOneXOneUni003(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXOneUnidirectionalTestLogic.testOneXOneUni003(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityAName = (String) testExecCtx.getProperties().get("EntityAName");
        OneXOneUnidirectionalEntityEnum targetEntityAType = OneXOneUnidirectionalEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXOneUnidirectionalEntityEnum targetEntityBType = OneXOneUnidirectionalEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXOneUnidirectionalTestLogic.testOneXOneUni003(): Begin");

            {
                System.out.println("Verify that an entity with a non-optional relationship cannot be persisted.");

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() + " (id=1)...");
                INoOptEntityA new_entityA = (INoOptEntityA) constructNewEntityObject(targetEntityAType);
                new_entityA.setId(1);
                new_entityA.setName("Entity A");

                System.out.println("Persisting " + new_entityA);
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Committing transaction (Should throw an IllegalStateException...");
                try {
                    jpaResource.getTj().commitTransaction();

                    // No Exception was thrown, this has failed.
                    Assert.fail("Transaction Commit completed without an Exception being thrown.");
                } catch (java.lang.AssertionError ae) {
                    throw ae;
                } catch (Throwable t) {
                    // Caught an Exception, check if IllegalStateException is in the Exception Chain
                    System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for IllegalStateException...");
                    assertExceptionIsInChain(IllegalStateException.class, t);
                } finally {
                    if (jpaResource.getTj().isTransactionActive()) {
                        System.out.println("Rolling back the transaction...");
                        jpaResource.getTj().rollbackTransaction();
                    }
                }
            }

            {
                // Verify that an existing entity with a non-optional field cannot change the optional field
                System.out.println("Verify that an existing entity with a non-optional field cannot set it null.");

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Construct a new entity instances
                System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() + " (id=1)...");
                INoOptEntityA new_entityA = (INoOptEntityA) constructNewEntityObject(targetEntityAType);
                new_entityA.setId(1);
                new_entityA.setName("Entity A");

                System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() + " (id=1)...");
                INoOptEntityB new_entityB = (INoOptEntityB) constructNewEntityObject(targetEntityBType);
                new_entityB.setId(1);
                new_entityB.setName("Entity B");

                System.out.println("Persisting " + new_entityB);
                jpaResource.getEm().persist(new_entityB);

                System.out.println("Creating relationship between " + targetEntityAType.getEntityName() + " and " +
                                   targetEntityBType.getEntityName() + " via the 'b' relationship field...");
                new_entityA.setBField(new_entityB);

                System.out.println("Persisting " + new_entityA);
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Both entities created, relationship established.  Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                System.out.println("Attempting to remove the reference to INoOptEntityB(id=1) in INoOptEntityA(id=1)'s OneXOne relational field.");
                System.out.println("An exception should be thrown because the relationship is not optional, so cannot be set null.");

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=1)...");
                INoOptEntityA find_entityA = (INoOptEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), 1);
                System.out.println("Object returned by find: " + find_entityA);

                // Verify that em.find() returned an object. (1 point)
                Assert.assertNotNull("Assert that the find operation did not return null", find_entityA);

                try {
                    System.out.println("Setting its OneXOne relationship with INoOptEntityB to null...");
                    find_entityA.setBField(null);

                    System.out.println("Attempting to commit transaction, an exception should be thrown.");
                    jpaResource.getTj().commitTransaction();
                    Assert.fail("Transaction Commit completed without an Exception being thrown.");
                } catch (java.lang.AssertionError ae) {
                    throw ae;
                } catch (Throwable t) {
                    // Caught an Exception, check if IllegalStateException is in the Exception Chain
                    System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for IllegalStateException...");
                    assertExceptionIsInChain(IllegalStateException.class, t);
                } finally {
                    if (jpaResource.getTj().isTransactionActive()) {
                        System.out.println("Rolling back the transaction...");
                        jpaResource.getTj().rollbackTransaction();
                    }
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("OneXOneUnidirectionalTestLogic.testOneXOneUni003(): End");
        }
    }

    /**
     * Verify relational field LAZY fetch behavior.
     *
     * Test verifies function performing the following:
     * 1) Create new UniEntityB(id=1)
     * 3) Create new UniEntityA(id=1), setting Lazy to UniEntity(id=1)].
     * Keep other (optional) OneXOne fields set to null.
     * 4) Clear the persistence context, forcing all entities to become detached.
     * 5) Acquire a fresh, managed copy of UniEntityA(id=1) by invoking
     * find UniEntityA(id=1) and access UniEntityA(id=1).Lazy's name field. The
     * data in UniEntityB(id=1) should be accessible. TranScoped PC's will need
     * to take special care to perform this work under a live persistence context
     * (ie, find and data retrieval operations are in the same transaction)
     * 6) Clear the persistence context again, forcing all entties to become detached.
     * 7) Acquire a new copy of UniEntityA(id=1), and clear the persistence context again
     * BEFORE attempting to access any fields. This forces the entity to become detached.
     * 6) The data should not be available since Lazy was never accessed.
     * 7) Try to access Lazy, which should be null because it is lazy loaded, and was never
     * accessed before the entity was detached.
     *
     * Test passes if :
     * 1) UniEntityB(id=1)'s name field could be accessed while the entity
     * is not detached
     * 2) If UniEntityB(id=1)'s name field could NOT be accessed if the entity was
     * never previously accessed before becomming a detached entity.
     *
     * 9 POINTS
     */
    public void testOneXOneUni004(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXOneUnidirectionalTestLogic.testOneXOneUni004(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityAName = (String) testExecCtx.getProperties().get("EntityAName");
        OneXOneUnidirectionalEntityEnum targetEntityAType = OneXOneUnidirectionalEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXOneUnidirectionalEntityEnum targetEntityBType = OneXOneUnidirectionalEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXOneUnidirectionalTestLogic.testOneXOneUni004(): Begin");

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Construct a new entity instances
            System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() + " (id=1)...");
            IEntityA new_entityA = (IEntityA) constructNewEntityObject(targetEntityAType);
            new_entityA.setId(1);
            new_entityA.setName("Entity A");

            System.out.println("Persisting " + new_entityA);
            jpaResource.getEm().persist(new_entityA);

            System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() + " (id=1)...");
            IEntityB new_entityB = (IEntityB) constructNewEntityObject(targetEntityBType);
            new_entityB.setId(1);
            new_entityB.setName("Entity B");

            System.out.println("Persisting " + new_entityB);
            jpaResource.getEm().persist(new_entityB);

            System.out.println("Creating relationship between " + targetEntityAType.getEntityName() + " and " +
                               targetEntityBType.getEntityName() + " via the 'b4' relationship field...");
            new_entityA.setB4Field(new_entityB);

            System.out.println("Both entities created, relationship established.  Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Verify that lazy loaded data can be successfully read when the entity is managed.

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=1)...");
            IEntityA find_entityA = (IEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), 1);
            System.out.println("Object returned by find: " + find_entityA);
            Assert.assertNotNull("Assert that the find operation did not return null", find_entityA);
            Assert.assertNotSame("Assert find did not return the original object,", new_entityA, find_entityA);
            Assert.assertEquals("Assert the entity has the expected identity.", 1, find_entityA.getId());

            System.out.println("Examining the IEntityB associated with EntityA(id=1)'s lazy relationship field...");
            IEntityB entityLazy = find_entityA.getB4Field();
            Assert.assertNotNull(
                                 "Assert that " + targetEntityAType.getEntityName() + "'s lazy relationship is not null.",
                                 entityLazy);
            Assert.assertEquals("Assert the entity has the expected identity.", 1, entityLazy.getId());

            System.out.println("Rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Verify that lazy is not available if the field was not read while the entity was managed.

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=1)...");
            IEntityA find_entityA2 = (IEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), 1);
            System.out.println("Object returned by find: " + find_entityA2);
            Assert.assertNotNull("Assert that the find operation did not return null", find_entityA2);
            Assert.assertNotSame("Assert find did not return the original object,", new_entityA, find_entityA2);
            Assert.assertEquals("Assert the entity has the expected identity.", 1, find_entityA2.getId());

            System.out.println("Rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Examining the IEntityB associated with EntityA(id=1)'s lazy relationship field (should be null)...");
            IEntityB entityLazy2 = find_entityA2.getB4Field();

            // Note that the spec states that marking a persistent field LAZY means that it is a hint to the provider,
            // rather then a non-negotiable dictation.  OpenJPA honors the lazy behavior, while EclipseLink still
            // loads the field.
            String delegateClassStr = jpaResource.getEm().getDelegate().getClass().getName();
            System.out.println("EntityManager delegate = " + delegateClassStr);
            if (delegateClassStr != null && delegateClassStr.toLowerCase().contains("eclipse")) {
                Assert.assertNotNull(
                                     "With Eclipselink, assert find_entityA2.getB4Field() does not return null "
                                     + " (to detect if a later change alters its behavior.)",
                                     entityLazy2);
            } else {
                Assert.assertNull(
                                  "Assert that " + targetEntityAType.getEntityName() + "'s lazy relationship is null.",
                                  entityLazy2);
            }

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Ending test.");
        } finally {
            System.out.println("OneXOneUnidirectionalTestLogic.testOneXOneUni004(): End");
        }
    }

    /**
     * Test Cascade, Type: Default (no cascade options specified) Verification
     *
     * Verify cascade: none behavior (default). By this:
     *
     * Persist:
     *
     * Both entities in the relationship need to have persist() invoked on them
     * in order to be stored on the database. By default, persisting the owning side
     * of the relationship does not automatically persist the entity on the inverse
     * side, and vice versa.
     *
     * 1) In a new transaction, create an unpersisted UniEntityB(id=1).
     * and a persisted UniEntityA(id=1), setting defaultRelationship to UniEntityB(id=1).
     * 2) Commit the transaction. An IllegalStateException should be thrown, because
     * b1 is referencing an entity that is not managed and does not exist on the database.
     *
     * Remove:
     *
     * By default, removing the entity on the owning side of the oneXone relationship does
     * not cause the entity on the inverse side of the relationship to become removed as well.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting defaultRelationship to UniEntityB(id=1).
     * 3) Remove UniEntityA(id=1), verify that UniEntityB(id=1) still exists. Test point passes if it does.
     *
     * If the entity on the inverse side of the relationship is removed, it should not remove the entity
     * on the owning of the relationship. Also, since the oneXone relationship is optional, the relationship
     * field should be set null on fresh instances of the owning entity from find().
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting defaultRelationship to UniEntityB(id=1).
     * 3) Remove UniEntityB(id=1)
     * 4) Clear the persistence context
     * 5) Find UniEntityA(id=1). It should still exist (especially given that it is a unidirectional
     * relationship), and defaultRelationship should be null.
     *
     * Merge:
     *
     * Merge will attempt to update the managed entity to point to managed versions of entities referenced by
     * the detached entity.
     *
     * 1) Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting defaultRelationship to UniEntityB(id=1).
     * 3) Clear the persistence context, causing all entities to become detached
     * 4) Perform the following operations:
     * Modify the name field on UniEntityB(id=1)
     * Modify the name field on UniEntityA(id=1)
     * 5) Merge UniEntityA(id=1) into the persistence context.
     * 6) Verify the change to UniEntityA(id=1)'s name field. Verify that UniEntityA(id=1)'s defaultRelationship field references
     * a managed copy of UniEntityB(id=1) and that UniEntityB(id=1)'s name field contains the original value.
     * UniEntityB(id=1)'s name field should not have been changed because EntityManager.merge() was never called
     * on it directly, and merge by default does not cascade across entity relationships.
     *
     * If an entity being merged points to a removed entity, an IllegalArgumentException should be thrown.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting defaultRelationship to UniEntityB(id=1).
     * 3) Clear the persistence context
     * 4) In a single transaction, find UniEntityB(id=1) and remove it. This deletes UniEntityB(id=1) from the database.
     * Note: the original detached UniEntityB(id=1) reference still exists, and is referenced by the detached copy of
     * UniEntityA(id=1).
     * 5) Modify the name field of UniEntityA(id=1) and attempt to merge it. An IllegalArgumentException because the
     * detached entity still has a references in its defaultRelationship field to UniEntityB(id=1), which no longer exists in the database.
     *
     * Lazy loaded relationships, if not triggered while the entity was managed, are ignored during merge().
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting lazy to UniEntityB(id=1).
     * 3) Clear the persistence context, causing all entities to become detached
     * 4) Find UniEntityA(id=1), but do not access the lazy field.
     * 5) Clear the persistence context, causing all entities to become detached
     * 6) Modify UniEntityA(id=1)'s name field, and set lazy to null.
     * 7) Merge UniEntityA(id=1) into the persistence context. The field lazy should still refer to UniEntityB(id=1) because
     * it was never triggered on the original entity before it became detached.
     *
     * On the other hand, if a lazy-loaded relationship is loaded before the entity is detached, then changes to that
     * relationship are merged.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting lazy to UniEntityB(id=1).
     * 3) Clear the persistence context, causing all entities to become detached
     * 4) Find UniEntityA(id=1), and access the lazy field, loading the entity on the inverse side of the relationship
     * into the persistence context.
     * 5) Clear the persistence context, causing all entities to become detached
     * 6) Modify UniEntityA(id=1)'s name field, and set lazy to null.
     * 7) Merge UniEntityA(id=1) into the persistence context. The field lazy should now be null since lazy was accessed
     * before the entity became detached.
     *
     * Refresh:
     *
     * Refresh operations are, by default, not cascaded across entity relationships. Without the REFRESH cascade
     * option, a refresh operation will stop at the soure tntity.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting defaultRelationship to UniEntityB(id=1).
     * 3) Start a new transaction
     * 4) With managed copies of the two entities, edit UniEntityA(id=1) and UniEntityA(id=1).defaultRelationship's name fields.
     * 5) Invoke EntityManager.refresh() on UniEntityA(id=1)
     * 6) Verify that UniEntityA(id=1)'s name field has been reverted to the value it had when it was created.
     * 7) Verify that UniEntityB(id=1) still has the new value
     *
     * 29 POINTS
     */
    public void testOneXOneUni005(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXOneUnidirectionalTestLogic.testOneXOneUni005(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityAName = (String) testExecCtx.getProperties().get("EntityAName");
        OneXOneUnidirectionalEntityEnum targetEntityAType = OneXOneUnidirectionalEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXOneUnidirectionalEntityEnum targetEntityBType = OneXOneUnidirectionalEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXOneUnidirectionalTestLogic.testOneXOneUni005(): Begin");

            // ----------------------------------------------------------------------------------------------------
            // Verify Default Persist Cascade Behavior (1 POINT)
            // Both entities in the relationship need to have persist() invoked on them in order to be stored on
            // the database.  By default, persisting the owning side of the relationship does not automatically
            // persist the entity on the inverse side, and vice versa.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify Default Persist Cascade Behavior:");
                System.out.println(
                                   "Both entities in the relationship need to have persist() invoked on them in order to be stored "
                                   + "on the database.  By default, persisting the owning side of the relationship does not "
                                   + "automatically persist the entity on the inverse side, and vice versa.");

                int pkey = 1;

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Construct a new entity instances
                System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityB new_entityB = (IEntityB) constructNewEntityObject(targetEntityBType);
                new_entityB.setId(pkey);
                new_entityB.setName("Entity B");

                System.out.println("NOT Persisting " + new_entityB + "...");

                System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityA new_entityA = (IEntityA) constructNewEntityObject(targetEntityAType);
                new_entityA.setId(pkey);
                new_entityA.setName("Entity A");

                System.out.println("Creating relationship between " + targetEntityAType.getEntityName() + " and " +
                                   targetEntityBType.getEntityName() + " via the 'b1' relationship field...");
                new_entityA.setB1Field(new_entityB);

                System.out.println("Persisting " + new_entityA + " (persist should not cascade) ...");
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Committing transaction (IllegalStateException should be thrown)...");
                try {
                    jpaResource.getTj().commitTransaction();

                    // No Exception was thrown, this has failed.
                    Assert.fail("Transaction Commit completed without an Exception being thrown.");
                } catch (java.lang.AssertionError ae) {
                    throw ae;
                } catch (Throwable t) {
                    // Caught an Exception, check if IllegalStateException is in the Exception Chain
                    System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for IllegalStateException...");
                    assertExceptionIsInChain(IllegalStateException.class, t);
                } finally {
                    if (jpaResource.getTj().isTransactionActive()) {
                        System.out.println("Rolling back the transaction...");
                        jpaResource.getTj().rollbackTransaction();
                    }
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();
            }

            // ----------------------------------------------------------------------------------------------------
            // Verify Default Remove Cascade Behavior (3 POINTS)
            // By default, removing the entity on the owning side of the oneXone relationship does
            // not cause the entity on the inverse side of the relationship to become removed as well.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify Default Remove Cascade Behavior:");
                System.out.println(
                                   "By default, removing the entity on the owning side of the oneXone relationship does "
                                   + "not cause the entity on the inverse side of the relationship to become removed as well.");

                System.out.println("Create new entities for verifying remove cascade behavior with...");
                int pkey = 2;

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Construct a new entity instances
                System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityB new_entityB = (IEntityB) constructNewEntityObject(targetEntityBType);
                new_entityB.setId(pkey);
                new_entityB.setName("Entity B");

                System.out.println("Persisting " + new_entityB);
                jpaResource.getEm().persist(new_entityB);

                System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityA new_entityA = (IEntityA) constructNewEntityObject(targetEntityAType);
                new_entityA.setId(pkey);
                new_entityA.setName("Entity A");

                System.out.println("Creating relationship between " + targetEntityAType.getEntityName() + " and " +
                                   targetEntityBType.getEntityName() + " via the 'b1' relationship field...");
                new_entityA.setB1Field(new_entityB);

                System.out.println("Persisting " + new_entityA);
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Both entities created, relationship established.  Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Entities have been persisted to the database, with a one-to-one relationship between
                // EntityA and EntityB established.  Now remove EntityA from the database.  Because, by default,
                // remove operations are not cascaded across entity relationships, EntityB should survive
                // EntityA's removal.
                System.out.println(
                                   "Entities have been persisted to the database, with a one-to-one relationship between " +
                                   targetEntityAType.getEntityName() + " and " + targetEntityBType.getEntityName() +
                                   " established.  The relationship is configured to not cascade remove operations, so " +
                                   targetEntityBType.getEntityName() + " should survive " +
                                   targetEntityAType.getEntityName() + "'s removal.");

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=" + pkey + ")...");
                IEntityA find_entityA = (IEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), pkey);
                System.out.println("Object returned by find: " + find_entityA);
                Assert.assertNotNull("Assert that the find operation did not return null", find_entityA);

                System.out.println("Removing " + targetEntityAType.getEntityName() + " (id=" + pkey + ")...");
                jpaResource.getEm().remove(find_entityA);

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Verify that EntityA has been removed, and that EntityB has not been removed.
                System.out.println("Verify that " + targetEntityAType.getEntityName() + " has been removed, and that " +
                                   targetEntityBType.getEntityName() + " has not been removed");

                System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=" + pkey + ")...");
                IEntityA find_entityA2 = (IEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), pkey);
                System.out.println("Object returned by find: " + find_entityA2);
                Assert.assertNull("Assert that the find operation did return null", find_entityA2);

                System.out.println("Finding " + targetEntityBType.getEntityName() + " (id=" + pkey + ")...");
                IEntityB find_entityB = (IEntityB) jpaResource.getEm().find(resolveEntityClass(targetEntityBType), pkey);
                System.out.println("Object returned by find: " + find_entityB);
                Assert.assertNotNull("Assert that the find operation did not return null", find_entityB);

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();
            }

            // ----------------------------------------------------------------------------------------------------
            // Verify Default Remove Cascade (INVERSE) Behavior (3 POINTS)
            // If the entity on the inverse side of the relationship is removed, it should not remove the entity
            // on the owning of the relationship.  Also, since the oneXone relationship is optional, the relationship
            // field should be set null on fresh instances of the owning entity from find().

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify Default Remove Cascade (INVERSE) Behavior:");
                System.out.println(
                                   "If the entity on the inverse side of the relationship is removed, it should not remove the entity "
                                   + "on the owning of the relationship.  Also, since the oneXone relationship is optional, the relationship "
                                   + "field should be set null on fresh instances of the owning entity from find().");

                System.out.println("Create new entities for verifying remove cascade behavior with...");
                int pkey = 3;

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Construct a new entity instances
                System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityB new_entityB = (IEntityB) constructNewEntityObject(targetEntityBType);
                new_entityB.setId(pkey);
                new_entityB.setName("Entity B");

                System.out.println("Persisting " + new_entityB);
                jpaResource.getEm().persist(new_entityB);

                System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityA new_entityA = (IEntityA) constructNewEntityObject(targetEntityAType);
                new_entityA.setId(pkey);
                new_entityA.setName("Entity A");

                System.out.println("Creating relationship between " + targetEntityAType.getEntityName() + " and " +
                                   targetEntityBType.getEntityName() + " via the 'b1' relationship field...");
                new_entityA.setB1Field(new_entityB);

                System.out.println("Persisting " + new_entityA);
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Both entities created, relationship established.  Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Entities have been persisted to the database, with a one-to-one relationship between
                // EntityA and EntityB established.  Now remove EntityA from the database.  Because, by default,
                // remove operations are not cascaded across entity relationships, EntityA should survive
                // EntityB's removal.

                System.out.println(
                                   "Entities have been persisted to the databae, with a one-to-one relationship between " +
                                   "remove operations are not cascaded across entity relationships, " +
                                   targetEntityAType.getEntityName() + " should survive " +
                                   targetEntityBType.getEntityName() + "'s removal.");

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                System.out.println("Finding " + targetEntityBType.getEntityName() + " (id=" + pkey + ")...");
                IEntityB find_entityB = (IEntityB) jpaResource.getEm().find(resolveEntityClass(targetEntityBType), pkey);
                System.out.println("Object returned by find: " + find_entityB);
                Assert.assertNotNull("Assert that the find operation did not return null", find_entityB);

                System.out.println("Removing " + targetEntityBType.getEntityName() + " (id=" + pkey + ")...");
                jpaResource.getEm().remove(find_entityB);

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Verify that EntityB has been removed, and that EntityA has not been removed.
                System.out.println("Verify that " + targetEntityBType.getEntityName() + " has been removed, and that " +
                                   targetEntityAType.getEntityName() + " has not been removed");

                System.out.println("Finding " + targetEntityBType.getEntityName() + " (id=" + pkey + ")...");
                IEntityB find_entityB2 = (IEntityB) jpaResource.getEm().find(resolveEntityClass(targetEntityBType), pkey);
                System.out.println("Object returned by find: " + find_entityB2);
                Assert.assertNull("Assert that the find operation did return null", find_entityB2);

                System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=" + pkey + ")...");
                IEntityA find_entityA = (IEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), pkey);
                System.out.println("Object returned by find: " + find_entityA);
                Assert.assertNotNull("Assert that the find operation did not return null", find_entityA);

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

            }

            // ----------------------------------------------------------------------------------------------------
            // Verify Default Merge Cascade Behavior (12 POINTS)
            // Merge will attempt to update the managed entity to point to managed versions of entities referenced by
            // the detached entity.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify Default Merge Cascade Behavior:");
                System.out.println(
                                   "Merge will attempt to update the managed entity to point to managed versions of entities "
                                   + "referenced by the detached entity.");

                System.out.println("Create new entities for verifying merge cascade behavior with...");
                int pkey = 4;

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Construct a new entity instances
                System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityB new_entityB = (IEntityB) constructNewEntityObject(targetEntityBType);
                new_entityB.setId(pkey);
                new_entityB.setName("Entity B");

                System.out.println("Persisting " + new_entityB);
                jpaResource.getEm().persist(new_entityB);

                System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityA new_entityA = (IEntityA) constructNewEntityObject(targetEntityAType);
                new_entityA.setId(pkey);
                new_entityA.setName("Entity A");

                System.out.println("Creating relationship between " + targetEntityAType.getEntityName() + " and " +
                                   targetEntityBType.getEntityName() + " via the 'b1' relationship field...");
                new_entityA.setB1Field(new_entityB);

                System.out.println("Persisting " + new_entityA);
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Both entities created, relationship established.  Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Mutate persistent data on both entities.
                System.out.println("Mutating persistent data on both entities...");
                new_entityA.setName("New Entity A Name");
                new_entityB.setName("New Entity B Name");

                // Merge EntityA(id=4) into the persistence context and verify that the default field of the copy of
                // EntityA(id=4) returned by the merge operation reflects the state of EntityB(id=4) in the database
                // (name field should contain original value)
                System.out.println(
                                   "Merge EntityA(id=" + pkey + ") into the persistence context and verify that the default field of the " +
                                   "copy of " + targetEntityAType.getEntityName() + "returned by the merge operation reflects " +
                                   "the state of " + targetEntityBType.getEntityName() + "(id=" + pkey + ") in the database " +
                                   "(name field should contain original value).");

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                IEntityA mergedEntityA = jpaResource.getEm().merge(new_entityA);
                Assert.assertNotNull("Assert em.merge() did not return a null value.", mergedEntityA);
                Assert.assertNotSame("Assert em.merge() did not return the original entity object",
                                     new_entityA, mergedEntityA);
                Assert.assertTrue("Assert object returned by merge() is not detached.", jpaResource.getEm().contains(mergedEntityA));
                Assert.assertEquals(
                                    "Assert " + targetEntityAType.getEntityName() + " returned by merge() has the updated field.",
                                    "New Entity A Name",
                                    mergedEntityA.getName());

                // Verify that the EntityB referenced by the merged EntityA contains data unmodified from when it was
                // persisted.
                System.out.println(
                                   "Extracting " + targetEntityBType.getEntityName() + " from the merged " +
                                   targetEntityAType.getEntityName() + "'s default b1 field.");
                IEntityB entityBFromMergedEntityA = mergedEntityA.getB1Field();
                Assert.assertNotNull("Assert the extraction from the collection did not return a null", entityBFromMergedEntityA);
                Assert.assertTrue(
                                  "Assert that " + targetEntityBType.getEntityName() + " is managed.",
                                  jpaResource.getEm().contains(entityBFromMergedEntityA));
                Assert.assertNotSame("Assert that this is not the original entity object",
                                     new_entityB, entityBFromMergedEntityA);
                Assert.assertEquals(
                                    "Assert that the entity's name is the same as when it was persisted.",
                                    "Entity B",
                                    entityBFromMergedEntityA.getName());

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Verify that the database state is correct.
                System.out.println("Verify that the database state is correct...");

                System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=" + pkey + ")...");
                IEntityA find_entityA = (IEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), pkey);
                System.out.println("Object returned by find: " + find_entityA);
                Assert.assertNotNull("Assert that the find operation did not return null", find_entityA);
                Assert.assertEquals(
                                    "Assert " + targetEntityAType.getEntityName() + " has the updated field.",
                                    "New Entity A Name",
                                    find_entityA.getName());

                System.out.println("Finding " + targetEntityBType.getEntityName() + " (id=" + pkey + ")...");
                IEntityB find_entityB2 = (IEntityB) jpaResource.getEm().find(resolveEntityClass(targetEntityBType), pkey);
                System.out.println("Object returned by find: " + find_entityB2);
                Assert.assertNotNull("Assert that the find operation did not return null", find_entityB2);
                Assert.assertEquals(
                                    "Assert that the entity's name is the same as when it was persisted.",
                                    "Entity B",
                                    find_entityB2.getName());

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();
            }

            // ----------------------------------------------------------------------------------------------------
            // Verify Default Refresh Cascade Behavior (10 POINTS)
            // Refresh operations are, by default, not cascaded across entity relationships.  Without the REFRESH
            // cascade option, a refresh operation will stop at the source entity.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify Default Refresh Cascade Behavior:");
                System.out.println(
                                   "Refresh operations are, by default, not cascaded across entity relationships.  "
                                   + "Without the REFRESH cascade option, a refresh operation will stop at the source entity.");

                System.out.println("Create new entities for verifying refresh cascade behavior with...");
                int pkey = 5;

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Construct a new entity instances
                System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityB new_entityB = (IEntityB) constructNewEntityObject(targetEntityBType);
                new_entityB.setId(pkey);
                new_entityB.setName("Entity B");

                System.out.println("Persisting " + new_entityB);
                jpaResource.getEm().persist(new_entityB);

                System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityA new_entityA = (IEntityA) constructNewEntityObject(targetEntityAType);
                new_entityA.setId(pkey);
                new_entityA.setName("Entity A");

                System.out.println("Creating relationship between " + targetEntityAType.getEntityName() + " and " +
                                   targetEntityBType.getEntityName() + " via the 'b1' relationship field...");
                new_entityA.setB1Field(new_entityB);

                System.out.println("Persisting " + new_entityA);
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Both entities created, relationship established.  Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // The test needs managed instances of the entities to perform field changes to and then refresh
                // In order to test situations where the entity manager is transaction-scoped, we must clear the
                // persistence context, start a new transaction, fetch managed instances of the entities via find,
                // make a change to the name fields of both entities, and invoke refresh on the entity on the
                // owner side of the relationship.  The refresh operation should not cascade to the entity on
                // the inverse side of the relationship, so the change to its name field should remain.

                System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=" + pkey + ")...");
                IEntityA find_entityA = (IEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), pkey);
                System.out.println("Object returned by find: " + find_entityA);
                Assert.assertNotNull("Assert that the find operation did not return null", find_entityA);
                Assert.assertTrue("Assert that the entity is managed.", jpaResource.getEm().contains(find_entityA));
                Assert.assertNotSame(
                                     "Assert that the find operation did not return the original object.",
                                     new_entityA,
                                     find_entityA);

                // Extract EntityB from EntityA's relationship
                System.out.println(
                                   "Extracting " + targetEntityBType.getEntityName() + " from the merged " +
                                   targetEntityAType.getEntityName() + "'s b1 relationship field.");
                IEntityB entityBFromEntityA = find_entityA.getB1Field();
                Assert.assertNotNull("Assert the extraction from the collection did not return a null", entityBFromEntityA);
                Assert.assertTrue(
                                  "Assert that " + targetEntityBType.getEntityName() + " is managed.",
                                  jpaResource.getEm().contains(entityBFromEntityA));
                Assert.assertNotSame("Assert that this is not the original entity object",
                                     new_entityB, entityBFromEntityA);

                // Mutate persistent data on both entities.
                System.out.println("Mutating persistent data on both entities...");
                find_entityA.setName("New Entity A Name");
                entityBFromEntityA.setName("New Entity B Name");

                Assert.assertEquals(
                                    "Assert mutation took hold in " + targetEntityAType.getEntityName() + "...",
                                    "New Entity A Name",
                                    find_entityA.getName());
                Assert.assertEquals(
                                    "Assert mutation took hold in " + targetEntityBType.getEntityName() + "...",
                                    "New Entity B Name",
                                    entityBFromEntityA.getName());

                // Now, invoke the refresh operation on EntityA.  Its values should be reset to what is in the database.
                // The refresh operation should not cascade to the entity referenced in its default field, so the
                // changes to that entity should remain.
                System.out.println(
                                   "Now, invoking the refresh() operation on EntityA.  Its values should be reset to what " +
                                   "is in the database.  The refresh operation should not cascade to the entity referenced " +
                                   "in its default field, so the changes to that entity should remain.");
                jpaResource.getEm().refresh(find_entityA);

                Assert.assertEquals(
                                    "Assert mutation in " + targetEntityAType.getEntityName() + " was undone by refresh()...",
                                    "Entity A",
                                    find_entityA.getName());
                Assert.assertEquals(
                                    "Assert mutation remains in " + targetEntityBType.getEntityName() + "...",
                                    "New Entity B Name",
                                    entityBFromEntityA.getName());
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("OneXOneUnidirectionalTestLogic.testOneXOneUni005(): End");
        }
    }

    /**
     * Test Cascade, Type: ALL (all cascade options specified) Verification
     *
     * Verify cascade: all. By this:
     *
     * Persist:
     *
     * With cascade type set to ALL, persist operations are cascaded across the
     * entity relationship. This means if UniEntityA is the target if an
     * EntityManager.persist() operation, the JPA implementation will automatically
     * invoke EntityManager.persist() on all of the UniEntityB entity relationships
     * that are marked with Cascade type ALL (or PERSIST).
     *
     * 1) Create an unpersisted UniEntityB(id=1)
     * 2) Create an unpersisted UniEntityA(id=1)
     * 3) Set CascadeAll to UniEntityB(id=1).
     * 4) Start a new transaction, persist UniEntityA(id=1), and commit the transaction.
     * 5) Clear persistence context, forcing all entities to become detached.
     * 6) Find UniEntityA(id=1), this should hit the database, returning an entity
     * with a reference in CascadeAll to UniEntityB(id=1) because the persist operation
     * was cascaded.
     *
     * Remove:
     *
     * With cascade type set to ALL, remove operations are cascaded across the entity
     * relationship. This means if UniEntityA is the target of an EntityManager.remove()
     * operation, the JPA implementation will automatically invoke EntityManager.remove()
     * on all of the UniEntityB entity relationships that are marked with Cascade type ALL
     * (or REMOVE).
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting CascadeAll to UniEntityB(id=1).
     * 3) Remove UniEntityA(id=1), verify that UniEntityB(id=1) also no longer exists. Test point passes if true.
     *
     * Merge:
     *
     * With cascade type set to ALL, merge operations will cascade across the entity relationship. So changes to
     * the target entity (UniEntityA(id=1)) and changes to all entities with relationship cascade attributes of
     * ALL (and MERGE) will be merged into the persistence context.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting CascadeAll to UniEntityB(id=1).
     * 3) Clear the persistence context, causing all entities to become detached
     * 4) Modify UniEntityA(id=1)'s name field. Modify UniEntityB(id=1)'s name field.
     * 5) Merge UniEntityA(id=1) into the persistence context. Examine the UniEntityB(id=1) referenced by the CascadeAll
     * field of the entity returned from the merge() operation. Both entities should be managed, and both
     * entities should contain the changes from step 4.
     *
     * Refresh:
     *
     * with cascade type set to ALL, refresh operations will cascade across entity relationships. So changes
     * to the target entity (UniEntityA(id=1)) and changes to all entities with relationship cascade attributes of
     * ALL (and REFRESH) will have their contents reset to match the data in the database.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting CascadeAll to UniEntityB(id=1).
     * 3) Start a new transaction
     * 4) With managed copies of the two entities, edit UniEntityA(id=1) and UniEntityA(id=1).CascadeAll's name fields.
     * 5) Invoke EntityManager.refresh() on UniEntityA(id=1)
     * 6) Verify that UniEntityA(id=1)'s name field has been reverted to the value it had when it was created.
     * 7) Verify that UniEntityB(id=1)'s name field has been reverted to the value it had when it was created.
     *
     * 32 POINTS
     */
    public void testOneXOneUni006(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXOneUnidirectionalTestLogic.testOneXOneUni006(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityAName = (String) testExecCtx.getProperties().get("EntityAName");
        OneXOneUnidirectionalEntityEnum targetEntityAType = OneXOneUnidirectionalEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXOneUnidirectionalEntityEnum targetEntityBType = OneXOneUnidirectionalEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXOneUnidirectionalTestLogic.testOneXOneUni006(): Begin");

            // ----------------------------------------------------------------------------------------------------
            // Verify Cascade: ALL Persist Behavior (6 POINTS)
            // With cascade type set to ALL, persist operations are cascaded across the entity relationship.
            // This means if EntityA is the target if an EntityManager.persist() operation, the JPA
            // implementation will automatically invoke EntityManager.persist() on all of the UniEntityB entity
            // relationships that are marked with Cascade type ALL (or PERSIST).

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify Cascade: ALL Persist Behavior:");
                System.out.println(
                                   "With cascade type set to ALL, persist operations are cascaded across the entity relationship.  "
                                   + "This means if EntityA is the target if an EntityManager.persist() operation, the JPA "
                                   + "implementation will automatically invoke EntityManager.persist() on all of the EntityB entity "
                                   + "relationships that are marked with Cascade type ALL.");

                int pkey = 1;

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Construct a new entity instances
                System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityB new_entityB = (IEntityB) constructNewEntityObject(targetEntityBType);
                new_entityB.setId(pkey);
                new_entityB.setName("Entity B");

                System.out.println("NOT Persisting " + new_entityB + "...");

                System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityA new_entityA = (IEntityA) constructNewEntityObject(targetEntityAType);
                new_entityA.setId(pkey);
                new_entityA.setName("Entity A");

                System.out.println("Creating relationship between " + targetEntityAType.getEntityName() + " and " +
                                   targetEntityBType.getEntityName() + " via the 'cascadeAll' relationship field...");
                new_entityA.setB5caField(new_entityB);

                System.out.println("Persisting " + new_entityA + " (persist should cascade) ...");
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Committing transaction (no Exception should not be thrown)...");
                jpaResource.getTj().commitTransaction();

                System.out.println("Clear persistence context, then reload " + targetEntityAType.getEntityName() +
                                   " to verify that both entities have been persisted and the relationship is intact.");

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=" + pkey + ")...");
                IEntityA find_entityA = (IEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), pkey);
                System.out.println("Object returned by find: " + find_entityA);
                Assert.assertNotNull("Assert that the find operation did not return null", find_entityA);
                Assert.assertTrue("Assert that the entity is managed.", jpaResource.getEm().contains(find_entityA));
                Assert.assertNotSame(
                                     "Assert that the find operation did not return the original object.",
                                     new_entityA,
                                     find_entityA);

                // Extract EntityB from EntityA's relationship
                System.out.println(
                                   "Extracting " + targetEntityBType.getEntityName() + " from the merged " +
                                   targetEntityAType.getEntityName() + "'s cascadeAll relationship field.");
                IEntityB entityBFromEntityACACollection = find_entityA.getB5caField();

                Assert.assertNotNull("Assert the extraction from the collection did not return a null", entityBFromEntityACACollection);
                Assert.assertTrue(
                                  "Assert that " + targetEntityBType.getEntityName() + " is managed.",
                                  jpaResource.getEm().contains(entityBFromEntityACACollection));
                Assert.assertNotSame("Assert that this is not the original entity object",
                                     new_entityB, entityBFromEntityACACollection);

                System.out.println("Rolling back transaction...");
                jpaResource.getTj().rollbackTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();
            }

            // ----------------------------------------------------------------------------------------------------
            // Verify Cascade: ALL Remove Behavior (4 POINTS)
            // With cascade type set to ALL, remove operations are cascaded across the entity relationship.
            // This means if EntityA is the target of an EntityManager.remove() operation, the JPA
            // implementation will automatically invoke EntityManager.remove() on all of the UniEntityB entity
            // relationships that are marked with Cascade type ALL (or REMOVE).

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify Cascade: ALL Remove Behavior");
                System.out.println(
                                   "With cascade type set to ALL, remove operations are cascaded across the entity relationship.  "
                                   + "This means if EntityA is the target of an EntityManager.remove() operation, the JPA "
                                   + "implementation will automatically invoke EntityManager.remove() on all of the UniEntityB "
                                   + "entity relationships that are marked with Cascade type ALL.");

                System.out.println("Create new entities for verifying remove cascade behavior with...");
                int pkey = 2;

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Construct a new entity instances
                System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityB new_entityB = (IEntityB) constructNewEntityObject(targetEntityBType);
                new_entityB.setId(pkey);
                new_entityB.setName("Entity B");

                System.out.println("Persisting " + new_entityB);
                jpaResource.getEm().persist(new_entityB);

                System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityA new_entityA = (IEntityA) constructNewEntityObject(targetEntityAType);
                new_entityA.setId(pkey);
                new_entityA.setName("Entity A");

                System.out.println("Creating relationship between " + targetEntityAType.getEntityName() + " and " +
                                   targetEntityBType.getEntityName() + " via the 'cascadeAll' relationship field...");
                new_entityA.setB5caField(new_entityB);

                System.out.println("Persisting " + new_entityA);
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Both entities created, relationship established.  Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Entities have been persisted to the database, with a one-to-one relationship between
                // EntityA and EntityB established.  Now remove EntityA from the database.  Because of the CASCADE Cfg
                // remove operations are cascaded across entity relationships, EntityB should NOT survive
                // EntityA's removal.
                System.out.println(
                                   "Entities have been persisted to the database, with a one-to-one relationship between " +
                                   targetEntityAType.getEntityName() + " and " + targetEntityBType.getEntityName() +
                                   "established.  Since the relationship is configured with CASCADE ALL, the remove operation " +
                                   "on " + targetEntityAType.getEntityName() + " should cascade across the relationship, " +
                                   " causing " + targetEntityBType.getEntityName() + " to also become removed.");

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=" + pkey + ")...");
                IEntityA find_entityA = (IEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), pkey);
                System.out.println("Object returned by find: " + find_entityA);
                Assert.assertNotNull("Assert that the find operation did not return null", find_entityA);

                System.out.println("Finding " + targetEntityBType.getEntityName() + " (id=" + pkey + ")...");
                IEntityB find_entityB = (IEntityB) jpaResource.getEm().find(resolveEntityClass(targetEntityBType), pkey);
                System.out.println("Object returned by find: " + find_entityB);
                Assert.assertNotNull("Assert that the find operation did not return null", find_entityB);

                System.out.println("Removing " + targetEntityAType.getEntityName() + " (id=" + pkey + ")...");
                jpaResource.getEm().remove(find_entityA);

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Verify that EntityA has been removed, and that EntityB has not been removed.
                System.out.println("Verify that " + targetEntityAType.getEntityName() + " has been removed, and that " +
                                   targetEntityBType.getEntityName() + " has also been removed.");

                System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=" + pkey + ")...");
                IEntityA find_entityA2 = (IEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), pkey);
                System.out.println("Object returned by find: " + find_entityA2);
                Assert.assertNull("Assert that the find operation did return null", find_entityA2);

                System.out.println("Finding " + targetEntityBType.getEntityName() + " (id=" + pkey + ")...");
                IEntityB find_entityB2 = (IEntityB) jpaResource.getEm().find(resolveEntityClass(targetEntityBType), pkey);
                System.out.println("Object returned by find: " + find_entityB2);
                Assert.assertNull("Assert that the find operation did return null", find_entityB2);

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();
            }

            // ----------------------------------------------------------------------------------------------------
            // Verify Cascade: ALL Merge Behavior (12 POINTS)
            // With cascade type set to ALL, merge operations will cascade across the entity relationship.  So changes to
            // the target entity (EntityA(id=1)) and changes to all entities with relationship cascade attributes of
            // ALL (and MERGE) will be merged into the persistence context.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify Cascade: ALL Merge Behavior:");
                System.out.println(
                                   "With cascade type set to ALL, merge operations will cascade across the entity relationship.  "
                                   + "So changes to the target entity (EntityA(id=1)) and changes to all entities with "
                                   + "relationship cascade attributes of ALL will be merged into the persistence context.");

                System.out.println("Create new entities for verifying merge cascade behavior with...");
                int pkey = 4;

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Construct a new entity instances
                System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityB new_entityB = (IEntityB) constructNewEntityObject(targetEntityBType);
                new_entityB.setId(pkey);
                new_entityB.setName("Entity B");

                System.out.println("Persisting " + new_entityB);
                jpaResource.getEm().persist(new_entityB);

                System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityA new_entityA = (IEntityA) constructNewEntityObject(targetEntityAType);
                new_entityA.setId(pkey);
                new_entityA.setName("Entity A");

                System.out.println("Creating relationship between " + targetEntityAType.getEntityName() + " and " +
                                   targetEntityBType.getEntityName() + " via the 'cascadeAll' relationship field...");
                new_entityA.setB5caField(new_entityB);

                System.out.println("Persisting " + new_entityA);
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Both entities created, relationship established.  Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Mutate persistent data on both entities.
                System.out.println("Mutating persistent data on both entities...");
                new_entityA.setName("New Entity A Name");
                new_entityB.setName("New Entity B Name");

                // Merge EntityA(id=4) into the persistence context and verify that the default field of the copy of
                // EntityA(id=4) returned by the merge operation reflects the state of EntityB(id=4) in the database
                // (name field should contain mutated value)
                System.out.println(
                                   "Merge EntityA(id=" + pkey + ") into the persistence context and verify that the cascadeAll field of the " +
                                   "copy of " + targetEntityAType.getEntityName() + " returned by the merge operation reflects " +
                                   "the state of " + targetEntityBType.getEntityName() + "(id=" + pkey + ") that was changed " +
                                   "(name field should contain mutated value).");

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                IEntityA mergedEntityA = jpaResource.getEm().merge(new_entityA);
                Assert.assertNotNull("Assert em.merge() did not return a null value.", mergedEntityA);
                Assert.assertNotSame("Assert em.merge() did not return the original entity object",
                                     new_entityA, mergedEntityA);
                Assert.assertTrue("Assert object returned by merge() is not detached.", jpaResource.getEm().contains(mergedEntityA));
                Assert.assertEquals(
                                    "Assert " + targetEntityAType.getEntityName() + " returned by merge() has the updated field.",
                                    "New Entity A Name",
                                    mergedEntityA.getName());

                // Verify that the EntityB referenced by the merged EntityA contains mutated data
                System.out.println(
                                   "Extracting " + targetEntityBType.getEntityName() + " from the merged " +
                                   targetEntityAType.getEntityName() + "'s cascadeAll relationship field.");
                IEntityB entityBFromMergedEntityA = mergedEntityA.getB5caField();

                Assert.assertNotNull("Assert the extraction from the collection did not return a null", entityBFromMergedEntityA);
                Assert.assertTrue(
                                  "Assert that " + targetEntityBType.getEntityName() + " is managed.",
                                  jpaResource.getEm().contains(entityBFromMergedEntityA));
                Assert.assertNotSame("Assert that this is not the original entity object",
                                     new_entityB, entityBFromMergedEntityA);
                Assert.assertEquals(
                                    "Assert that the entity's name contains the mutated value.",
                                    "New Entity B Name",
                                    entityBFromMergedEntityA.getName());

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Verify that the database state is correct.
                System.out.println("Verify that the database state is correct...");

                System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=" + pkey + ")...");
                IEntityA find_entityA = (IEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), pkey);
                System.out.println("Object returned by find: " + find_entityA);
                Assert.assertNotNull("Assert that the find operation did not return null", find_entityA);
                Assert.assertEquals(
                                    "Assert " + targetEntityAType.getEntityName() + " has the updated field.",
                                    "New Entity A Name",
                                    find_entityA.getName());

                System.out.println("Finding " + targetEntityBType.getEntityName() + " (id=" + pkey + ")...");
                IEntityB find_entityB2 = (IEntityB) jpaResource.getEm().find(resolveEntityClass(targetEntityBType), pkey);
                System.out.println("Object returned by find: " + find_entityB2);
                Assert.assertNotNull("Assert that the find operation did not return null", find_entityB2);
                Assert.assertEquals(
                                    "Assert that the entity's name is the mutated version.",
                                    "New Entity B Name",
                                    find_entityB2.getName());

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();
            }

            // ----------------------------------------------------------------------------------------------------
            // Verify Cascade: ALL Refresh Behavior (10 POINTS)
            // With cascade type set to ALL, refresh operations will cascade across entity relationships.  So changes
            // to the target entity (EntityA(id=1)) and changes to all entities with relationship cascade attributes
            // of ALL (and REFRESH) will have their contents reset to match the data in the database.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify Cascade: ALL Refresh Behavior:");
                System.out.println(
                                   "With cascade type set to ALL, refresh operations will cascade across entity relationships.  "
                                   + "So changes to the target entity (EntityA(id=1)) and changes to all entities with relationship "
                                   + "cascade attributes of ALL will have their contents reset to match the data in the database.");

                System.out.println("Create new entities for verifying refresh cascade behavior with...");
                int pkey = 5;

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Construct a new entity instances
                System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityB new_entityB = (IEntityB) constructNewEntityObject(targetEntityBType);
                new_entityB.setId(pkey);
                new_entityB.setName("Entity B");

                System.out.println("Persisting " + new_entityB);
                jpaResource.getEm().persist(new_entityB);

                System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityA new_entityA = (IEntityA) constructNewEntityObject(targetEntityAType);
                new_entityA.setId(pkey);
                new_entityA.setName("Entity A");

                System.out.println("Creating relationship between " + targetEntityAType.getEntityName() + " and " +
                                   targetEntityBType.getEntityName() + " via the 'cascadeAll' relationship field...");
                new_entityA.setB5caField(new_entityB);

                System.out.println("Persisting " + new_entityA);
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Both entities created, relationship established.  Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // The test needs managed instances of the entities to perform field changes to and then refresh
                // In order to test situations where the entity manager is transaction-scoped, we must clear the
                // persistence context, start a new transaction, fetch managed instances of the entities via find,
                // make a change to the name fields of both entities, and invoke refresh on the entity on the
                // owner side of the relationship.  The refresh operation should cascade to the entity on
                // the inverse side of the relationship, so the change to its name field should be lost.

                System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=" + pkey + ")...");
                IEntityA find_entityA = (IEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), pkey);
                System.out.println("Object returned by find: " + find_entityA);
                Assert.assertNotNull("Assert that the find operation did not return null", find_entityA);
                Assert.assertTrue("Assert that the entity is managed.", jpaResource.getEm().contains(find_entityA));
                Assert.assertNotSame(
                                     "Assert that the find operation did not return the original object.",
                                     new_entityA,
                                     find_entityA);

                // Extract EntityB from EntityA's relationship
                System.out.println(
                                   "Extracting " + targetEntityBType.getEntityName() + " from the merged " +
                                   targetEntityAType.getEntityName() + "'s cascadeAll relationship field.");
                IEntityB entityBFromEntityA = find_entityA.getB5caField();

                Assert.assertNotNull("Assert the extraction from the collection did not return a null", entityBFromEntityA);
                Assert.assertTrue(
                                  "Assert that " + targetEntityBType.getEntityName() + " is managed.",
                                  jpaResource.getEm().contains(entityBFromEntityA));
                Assert.assertNotSame("Assert that this is not the original entity object",
                                     new_entityB, entityBFromEntityA);

                // Mutate persistent data on both entities.
                System.out.println("Mutating persistent data on both entities...");
                find_entityA.setName("New Entity A Name");
                entityBFromEntityA.setName("New Entity B Name");

                Assert.assertEquals(
                                    "Assert mutation took hold in " + targetEntityAType.getEntityName() + "...",
                                    "New Entity A Name",
                                    find_entityA.getName());
                Assert.assertEquals(
                                    "Assert mutation took hold in " + targetEntityBType.getEntityName() + "...",
                                    "New Entity B Name",
                                    entityBFromEntityA.getName());

                // Now, invoke the refresh operation on EntityA.  Its values should be reset to what is in the database.
                // The refresh operation should cascade to the entity referenced in its cascadeAll field, so the
                // changes to that entity should also be lost.
                System.out.println(
                                   "Now, invoking the refresh() operation on EntityA.  Its values should be reset to what " +
                                   "is in the database.  The refresh operation should cascade to the entity referenced " +
                                   "in its cascadeAll field, so the changes to that entity should be lost.");
                jpaResource.getEm().refresh(find_entityA);

                Assert.assertEquals(
                                    "Assert mutation in " + targetEntityAType.getEntityName() + " was undone by refresh()...",
                                    "Entity A",
                                    find_entityA.getName());
                Assert.assertEquals(
                                    "Assert mutation in " + targetEntityBType.getEntityName() + " was undone...",
                                    "Entity B",
                                    entityBFromEntityA.getName());
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("OneXOneUnidirectionalTestLogic.testOneXOneUni006(): End");
        }
    }

    /**
     * Test Cascade, Type: PERSIST Verification
     *
     * Verify cascade: persist. By this:
     *
     * Persist:
     *
     * With cascade type set to PERSIST, persist operations are cascaded across the
     * entity relationship. This means if UniEntityA is the target if an
     * EntityManager.persist() operation, the JPA implementation will automatically
     * invoke EntityManager.persist() on all of the UniEntityB entity relationships
     * that are marked with Cascade type PERSIST.
     *
     * 1) Create an unpersisted UniEntityB(id=1)
     * 2) Create an unpersisted UniEntityA(id=1)
     * 3) Set CascadePersist to UniEntityB(id=1).
     * 4) Start a new transaction, persist UniEntityA(id=1), and commit the transaction.
     * 5) Clear persistence context, forcing all entities to become detached.
     * 6) Find UniEntityA(id=1), this should hit the database, returning an entity
     * with a reference in CascadePersist to UniEntityB(id=1) because the persist operation
     * was cascaded.
     *
     * 6 POINTS
     */
    public void testOneXOneUni007(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXOneUnidirectionalTestLogic.testOneXOneUni007(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityAName = (String) testExecCtx.getProperties().get("EntityAName");
        OneXOneUnidirectionalEntityEnum targetEntityAType = OneXOneUnidirectionalEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXOneUnidirectionalEntityEnum targetEntityBType = OneXOneUnidirectionalEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXOneUnidirectionalTestLogic.testOneXOneUni007(): Begin");

            // ----------------------------------------------------------------------------------------------------
            // Verify Cascade: PERSIST Persist Behavior (6 POINTS)
            // With cascade type set to PERSIST, persist operations are cascaded across the entity relationship.
            // This means if EntityA is the target if an EntityManager.persist() operation, the JPA
            // implementation will automatically invoke EntityManager.persist() on all of the UniEntityB entity
            // relationships that are marked with Cascade type PERSIST.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify Cascade: PERSIST Persist Behavior:");
                System.out.println(
                                   "With cascade type set to ALL, persist operations are cascaded across the entity relationship.  "
                                   + "This means if EntityA is the target if an EntityManager.persist() operation, the JPA "
                                   + "implementation will automatically invoke EntityManager.persist() on all of the EntityB entity "
                                   + "relationships that are marked with Cascade type PERSIST.");

                int pkey = 1;

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Construct a new entity instances
                System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityB new_entityB = (IEntityB) constructNewEntityObject(targetEntityBType);
                new_entityB.setId(pkey);
                new_entityB.setName("Entity B");

                System.out.println("NOT Persisting " + new_entityB + "...");

                System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityA new_entityA = (IEntityA) constructNewEntityObject(targetEntityAType);
                new_entityA.setId(pkey);
                new_entityA.setName("Entity A");

                System.out.println("Creating relationship between " + targetEntityAType.getEntityName() + " and " +
                                   targetEntityBType.getEntityName() + " via the 'cascadePersist' relationship field...");
                new_entityA.setB5cpField(new_entityB);

                System.out.println("Persisting " + new_entityA + " (persist should cascade) ...");
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Committing transaction (no Exception should not be thrown)...");
                jpaResource.getTj().commitTransaction();

                System.out.println("Clear persistence context, then reload " + targetEntityAType.getEntityName() +
                                   " to verify that both entities have been persisted and the relationship is intact.");

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=" + pkey + ")...");
                IEntityA find_entityA = (IEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), pkey);
                System.out.println("Object returned by find: " + find_entityA);
                Assert.assertNotNull("Assert that the find operation did not return null", find_entityA);
                Assert.assertTrue("Assert that the entity is managed.", jpaResource.getEm().contains(find_entityA));
                Assert.assertNotSame(
                                     "Assert that the find operation did not return the original object.",
                                     new_entityA,
                                     find_entityA);

                // Extract EntityB from EntityA's relationship
                System.out.println(
                                   "Extracting " + targetEntityBType.getEntityName() + " from the merged " +
                                   targetEntityAType.getEntityName() + "'s cascadePersist relationship field.");
                IEntityB entityBFromEntityACACollection = find_entityA.getB5cpField();

                Assert.assertNotNull("Assert the extraction from the collection did not return a null", entityBFromEntityACACollection);
                Assert.assertTrue(
                                  "Assert that " + targetEntityBType.getEntityName() + " is managed.",
                                  jpaResource.getEm().contains(entityBFromEntityACACollection));
                Assert.assertNotSame("Assert that this is not the original entity object",
                                     new_entityB, entityBFromEntityACACollection);

                System.out.println("Rolling back transaction...");
                jpaResource.getTj().rollbackTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("OneXOneUnidirectionalTestLogic.testOneXOneUni007(): End");
        }
    }

    /**
     * Test Cascade, Type: REMOVE Verification
     *
     * Verify cascade: remove. By this:
     *
     * Remove:
     *
     * With cascade type set to REMOVE, remove operations are cascaded across the entity
     * relationship. This means if UniEntityA is the target of an EntityManager.remove()
     * operation, the JPA implementation will automatically invoke EntityManager.remove()
     * on all of the UniEntityB entity relationships that are marked with Cascade type REMOVE.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting CascadeRemove to UniEntityB(id=1).
     * 3) Remove UniEntityA(id=1), verify that UniEntityB(id=1) also no longer exists. Test point passes if true.
     *
     * 4 POINTS
     */
    public void testOneXOneUni008(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXOneUnidirectionalTestLogic.testOneXOneUni008(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityAName = (String) testExecCtx.getProperties().get("EntityAName");
        OneXOneUnidirectionalEntityEnum targetEntityAType = OneXOneUnidirectionalEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXOneUnidirectionalEntityEnum targetEntityBType = OneXOneUnidirectionalEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXOneUnidirectionalTestLogic.testOneXOneUni008(): Begin");

            // ----------------------------------------------------------------------------------------------------
            // Verify Cascade: REMOVE Remove Behavior (4 POINTS)
            // With cascade type set to REMOVE, remove operations are cascaded across the entity relationship.
            // This means if EntityA is the target of an EntityManager.remove() operation, the JPA
            // implementation will automatically invoke EntityManager.remove() on all of the UniEntityB entity
            // relationships that are marked with Cascade type REMOVE.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify Cascade: REMOVE Remove Behavior");
                System.out.println(
                                   "With cascade type set to ALL, remove operations are cascaded across the entity relationship.  "
                                   + "This means if EntityA is the target of an EntityManager.remove() operation, the JPA "
                                   + "implementation will automatically invoke EntityManager.remove() on all of the UniEntityB "
                                   + "entity relationships that are marked with Cascade type REMOVE.");

                System.out.println("Create new entities for verifying remove cascade behavior with...");
                int pkey = 2;

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Construct a new entity instances
                System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityB new_entityB = (IEntityB) constructNewEntityObject(targetEntityBType);
                new_entityB.setId(pkey);
                new_entityB.setName("Entity B");

                System.out.println("Persisting " + new_entityB);
                jpaResource.getEm().persist(new_entityB);

                System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityA new_entityA = (IEntityA) constructNewEntityObject(targetEntityAType);
                new_entityA.setId(pkey);
                new_entityA.setName("Entity A");

                System.out.println("Creating relationship between " + targetEntityAType.getEntityName() + " and " +
                                   targetEntityBType.getEntityName() + " via the 'cascadeRemove' relationship field...");
                new_entityA.setB5rmField(new_entityB);

                System.out.println("Persisting " + new_entityA);
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Both entities created, relationship established.  Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Entities have been persisted to the database, with a one-to-one relationship between
                // EntityA and EntityB established.  Now remove EntityA from the database.  Because of the CASCADE Cfg
                // remove operations are cascaded across entity relationships, EntityB should NOT survive
                // EntityA's removal.
                System.out.println(
                                   "Entities have been persisted to the database, with a one-to-one relationship between " +
                                   targetEntityAType.getEntityName() + " and " + targetEntityBType.getEntityName() +
                                   "established.  Since the relationship is configured with CASCADE REMOVE, the remove operation " +
                                   "on " + targetEntityAType.getEntityName() + " should cascade across the relationship, " +
                                   " causing " + targetEntityBType.getEntityName() + " to also become removed.");

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=" + pkey + ")...");
                IEntityA find_entityA = (IEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), pkey);
                System.out.println("Object returned by find: " + find_entityA);
                Assert.assertNotNull("Assert that the find operation did not return null", find_entityA);

                System.out.println("Finding " + targetEntityBType.getEntityName() + " (id=" + pkey + ")...");
                IEntityB find_entityB = (IEntityB) jpaResource.getEm().find(resolveEntityClass(targetEntityBType), pkey);
                System.out.println("Object returned by find: " + find_entityB);
                Assert.assertNotNull("Assert that the find operation did not return null", find_entityB);

                System.out.println("Removing " + targetEntityAType.getEntityName() + " (id=" + pkey + ")...");
                jpaResource.getEm().remove(find_entityA);

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Verify that EntityA has been removed, and that EntityB has not been removed.
                System.out.println("Verify that " + targetEntityAType.getEntityName() + " has been removed, and that " +
                                   targetEntityBType.getEntityName() + " has also been removed.");

                System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=" + pkey + ")...");
                IEntityA find_entityA2 = (IEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), pkey);
                System.out.println("Object returned by find: " + find_entityA2);
                Assert.assertNull("Assert that the find operation did return null", find_entityA2);

                System.out.println("Finding " + targetEntityBType.getEntityName() + " (id=" + pkey + ")...");
                IEntityB find_entityB2 = (IEntityB) jpaResource.getEm().find(resolveEntityClass(targetEntityBType), pkey);
                System.out.println("Object returned by find: " + find_entityB2);
                Assert.assertNull("Assert that the find operation did return null", find_entityB2);

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("OneXOneUnidirectionalTestLogic.testOneXOneUni008(): End");
        }
    }

    /**
     * Test Cascade, Type: MERGE Verification
     *
     * Verify cascade: merge. By this:
     *
     * Merge:
     *
     * With cascade type set to MERGE, merge operations will cascade across the entity relationship. So changes to
     * the target entity (UniEntityA(id=1)) and changes to all entities with relationship cascade attributes of
     * MERGE will be merged into the persistence context.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting CascadeMerge to UniEntityB(id=1).
     * 3) Clear the persistence context, causing all entities to become detached
     * 4) Modify UniEntityA(id=1)'s name field. Modify UniEntityB(id=1)'s name field.
     * 5) Merge UniEntityA(id=1) into the persistence context. Examine the UniEntityB(id=1) referenced by CascadeMerge
     * field of the entity returned from the merge() operation. Both entities should be managed, and both
     * entities should contain the changes from step 4.
     *
     * 12 POINTS
     */
    public void testOneXOneUni009(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXOneUnidirectionalTestLogic.testOneXOneUni009(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityAName = (String) testExecCtx.getProperties().get("EntityAName");
        OneXOneUnidirectionalEntityEnum targetEntityAType = OneXOneUnidirectionalEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXOneUnidirectionalEntityEnum targetEntityBType = OneXOneUnidirectionalEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXOneUnidirectionalTestLogic.testOneXOneUni009(): Begin");

            // ----------------------------------------------------------------------------------------------------
            // Verify Cascade: MERGE Merge Behavior (12 POINTS)
            // With cascade type set to MERGE, merge operations will cascade across the entity relationship.  So changes to
            // the target entity (EntityA(id=1)) and changes to all entities with relationship cascade attributes of
            // MERGE will be merged into the persistence context.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify Cascade: MERGE Merge Behavior:");
                System.out.println(
                                   "With cascade type set to MERGE, merge operations will cascade across the entity relationship.  "
                                   + "So changes to the target entity (EntityA(id=1)) and changes to all entities with "
                                   + "relationship cascade attributes of MERGE will be merged into the persistence context.");

                System.out.println("Create new entities for verifying merge cascade behavior with...");
                int pkey = 4;

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Construct a new entity instances
                System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityB new_entityB = (IEntityB) constructNewEntityObject(targetEntityBType);
                new_entityB.setId(pkey);
                new_entityB.setName("Entity B");

                System.out.println("Persisting " + new_entityB);
                jpaResource.getEm().persist(new_entityB);

                System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityA new_entityA = (IEntityA) constructNewEntityObject(targetEntityAType);
                new_entityA.setId(pkey);
                new_entityA.setName("Entity A");

                System.out.println("Creating relationship between " + targetEntityAType.getEntityName() + " and " +
                                   targetEntityBType.getEntityName() + " via the 'cascadeMerge' relationship field...");
                new_entityA.setB5cmField(new_entityB);

                System.out.println("Persisting " + new_entityA);
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Both entities created, relationship established.  Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Mutate persistent data on both entities.
                System.out.println("Mutating persistent data on both entities...");
                new_entityA.setName("New Entity A Name");
                new_entityB.setName("New Entity B Name");

                // Merge EntityA(id=4) into the persistence context and verify that the default field of the copy of
                // EntityA(id=4) returned by the merge operation reflects the state of EntityB(id=4) in the database
                // (name field should contain mutated value)
                System.out.println(
                                   "Merge EntityA(id=" + pkey + ") into the persistence context and verify that the cascadeAll field of the " +
                                   "copy of " + targetEntityAType.getEntityName() + " returned by the merge operation reflects " +
                                   "the state of " + targetEntityBType.getEntityName() + "(id=" + pkey + ") that was changed " +
                                   "(name field should contain mutated value).");

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                IEntityA mergedEntityA = jpaResource.getEm().merge(new_entityA);
                Assert.assertNotNull("Assert em.merge() did not return a null value.", mergedEntityA);
                Assert.assertNotSame("Assert em.merge() did not return the original entity object",
                                     new_entityA, mergedEntityA);
                Assert.assertTrue("Assert object returned by merge() is not detached.", jpaResource.getEm().contains(mergedEntityA));
                Assert.assertEquals(
                                    "Assert " + targetEntityAType.getEntityName() + " returned by merge() has the updated field.",
                                    "New Entity A Name",
                                    mergedEntityA.getName());

                // Verify that the EntityB referenced by the merged EntityA contains mutated data
                System.out.println(
                                   "Extracting " + targetEntityBType.getEntityName() + " from the merged " +
                                   targetEntityAType.getEntityName() + "'s cascadeMerge relationship field.");
                IEntityB entityBFromMergedEntityA = mergedEntityA.getB5cmField();

                Assert.assertNotNull("Assert the extraction from the collection did not return a null", entityBFromMergedEntityA);
                Assert.assertTrue(
                                  "Assert that " + targetEntityBType.getEntityName() + " is managed.",
                                  jpaResource.getEm().contains(entityBFromMergedEntityA));
                Assert.assertNotSame("Assert that this is not the original entity object",
                                     new_entityB, entityBFromMergedEntityA);
                Assert.assertEquals(
                                    "Assert that the entity's name contains the mutated value.",
                                    "New Entity B Name",
                                    entityBFromMergedEntityA.getName());

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Verify that the database state is correct.
                System.out.println("Verify that the database state is correct...");

                System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=" + pkey + ")...");
                IEntityA find_entityA = (IEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), pkey);
                System.out.println("Object returned by find: " + find_entityA);
                Assert.assertNotNull("Assert that the find operation did not return null", find_entityA);
                Assert.assertEquals(
                                    "Assert " + targetEntityAType.getEntityName() + " has the updated field.",
                                    "New Entity A Name",
                                    find_entityA.getName());

                System.out.println("Finding " + targetEntityBType.getEntityName() + " (id=" + pkey + ")...");
                IEntityB find_entityB2 = (IEntityB) jpaResource.getEm().find(resolveEntityClass(targetEntityBType), pkey);
                System.out.println("Object returned by find: " + find_entityB2);
                Assert.assertNotNull("Assert that the find operation did not return null", find_entityB2);
                Assert.assertEquals(
                                    "Assert that the entity's name is the mutated version.",
                                    "New Entity B Name",
                                    find_entityB2.getName());

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("OneXOneUnidirectionalTestLogic.testOneXOneUni009(): End");
        }
    }

    /**
     * Test Cascade, Type: REFRESH Verification
     *
     * Verify cascade: refresh. By this:
     *
     * Refresh:
     *
     * With cascade type set to REFRESH, refresh operations will cascade across entity relationships. So changes
     * to the target entity (UniEntityA(id=1)) and changes to all entities with relationship cascade attributes of
     * REFRESH will have their contents reset to match the data in the database.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting CascadeRefresh to UniEntityB(id=1).
     * 3) Start a new transaction
     * 4) With managed copies of the two entities, edit UniEntityA(id=1) and UniEntityA(id=1).CascadeRefresh's name fields.
     * 5) Invoke EntityManager.refresh() on UniEntityA(id=1)
     * 6) Verify that UniEntityA(id=1)'s name field has been reverted to the value it had when it was created.
     * 7) Verify that UniEntityB(id=1)'s name field has been reverted to the value it had when it was created.
     *
     * 10 POINTS
     */
    public void testOneXOneUni010(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXOneUnidirectionalTestLogic.testOneXOneUni010(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityAName = (String) testExecCtx.getProperties().get("EntityAName");
        OneXOneUnidirectionalEntityEnum targetEntityAType = OneXOneUnidirectionalEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXOneUnidirectionalEntityEnum targetEntityBType = OneXOneUnidirectionalEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXOneUnidirectionalTestLogic.testOneXOneUni010(): Begin");

            // ----------------------------------------------------------------------------------------------------
            // Verify Cascade: REFRESH Refresh Behavior (10 POINTS)
            // With cascade type set to ALL, refresh operations will cascade across entity relationships.  So changes
            // to the target entity (EntityA(id=1)) and changes to all entities with relationship cascade attributes
            // of REFRESH will have their contents reset to match the data in the database.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify Cascade: REFRESH Refresh Behavior:");
                System.out.println(
                                   "With cascade type set to ALL, refresh operations will cascade across entity relationships.  "
                                   + "So changes to the target entity (EntityA(id=1)) and changes to all entities with relationship "
                                   + "cascade attributes of REFRESH will have their contents reset to match the data in the database.");

                System.out.println("Create new entities for verifying refresh cascade behavior with...");
                int pkey = 5;

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Construct a new entity instances
                System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityB new_entityB = (IEntityB) constructNewEntityObject(targetEntityBType);
                new_entityB.setId(pkey);
                new_entityB.setName("Entity B");

                System.out.println("Persisting " + new_entityB);
                jpaResource.getEm().persist(new_entityB);

                System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() +
                                   " (id=" + pkey + ")...");
                IEntityA new_entityA = (IEntityA) constructNewEntityObject(targetEntityAType);
                new_entityA.setId(pkey);
                new_entityA.setName("Entity A");

                System.out.println("Creating relationship between " + targetEntityAType.getEntityName() + " and " +
                                   targetEntityBType.getEntityName() + " via the 'cascadeRefresh' relationship field...");
                new_entityA.setB5rfField(new_entityB);

                System.out.println("Persisting " + new_entityA);
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Both entities created, relationship established.  Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // The test needs managed instances of the entities to perform field changes to and then refresh
                // In order to test situations where the entity manager is transaction-scoped, we must clear the
                // persistence context, start a new transaction, fetch managed instances of the entities via find,
                // make a change to the name fields of both entities, and invoke refresh on the entity on the
                // owner side of the relationship.  The refresh operation should cascade to the entity on
                // the inverse side of the relationship, so the change to its name field should be lost.

                System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=" + pkey + ")...");
                IEntityA find_entityA = (IEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), pkey);
                System.out.println("Object returned by find: " + find_entityA);
                Assert.assertNotNull("Assert that the find operation did not return null", find_entityA);
                Assert.assertTrue("Assert that the entity is managed.", jpaResource.getEm().contains(find_entityA));
                Assert.assertNotSame(
                                     "Assert that the find operation did not return the original object.",
                                     new_entityA,
                                     find_entityA);

                // Extract EntityB from EntityA's relationship
                System.out.println(
                                   "Extracting " + targetEntityBType.getEntityName() + " from the merged " +
                                   targetEntityAType.getEntityName() + "'s cascadeRefresh relationship field.");
                IEntityB entityBFromEntityA = find_entityA.getB5rfField();

                Assert.assertNotNull("Assert the extraction from the collection did not return a null", entityBFromEntityA);
                Assert.assertTrue(
                                  "Assert that " + targetEntityBType.getEntityName() + " is managed.",
                                  jpaResource.getEm().contains(entityBFromEntityA));
                Assert.assertNotSame("Assert that this is not the original entity object",
                                     new_entityB, entityBFromEntityA);

                // Mutate persistent data on both entities.
                System.out.println("Mutating persistent data on both entities...");
                find_entityA.setName("New Entity A Name");
                entityBFromEntityA.setName("New Entity B Name");

                Assert.assertEquals(
                                    "Assert mutation took hold in " + targetEntityAType.getEntityName() + "...",
                                    "New Entity A Name",
                                    find_entityA.getName());
                Assert.assertEquals(
                                    "Assert mutation took hold in " + targetEntityBType.getEntityName() + "...",
                                    "New Entity B Name",
                                    entityBFromEntityA.getName());

                // Now, invoke the refresh operation on EntityA.  Its values should be reset to what is in the database.
                // The refresh operation should cascade to the entity referenced in its cascadeAll field, so the
                // changes to that entity should also be lost.
                System.out.println(
                                   "Now, invoking the refresh() operation on EntityA.  Its values should be reset to what " +
                                   "is in the database.  The refresh operation should cascade to the entity referenced " +
                                   "in its cascadeAll field, so the changes to that entity should be lost.");
                jpaResource.getEm().refresh(find_entityA);

                Assert.assertEquals(
                                    "Assert mutation in " + targetEntityAType.getEntityName() + " was undone by refresh()...",
                                    "Entity A",
                                    find_entityA.getName());
                Assert.assertEquals(
                                    "Assert mutation in " + targetEntityBType.getEntityName() + " was undone...",
                                    "Entity B",
                                    entityBFromEntityA.getName());
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("OneXOneUnidirectionalTestLogic.testOneXOneUni010(): End");
        }
    }

    /**
     * Cardinality Test: Verify that no more then one entity on the owning side of the relationship can have a relationship
     * with a specific instance of an entity on the inverse side of the relationship. Entities are all declared in XML.
     *
     * 1) Create UniEntityB(id=1), save to database.
     * 2) Create new UniEntityA(id=1), setting b1 to UniEntityB(id=1), and save to database.
     * 3) Create new UniEntityA(id=2), setting b1 to UniEntityB(id=1), and save to database. An exception should be thrown
     * because one instance of UniEntityA already has a One-to-One relationship established with UniEntityB(id=1).
     *
     * 1 POINT
     */
    public void testCardinality001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXOneUnidirectionalTestLogic.testCardinality001(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityAName = (String) testExecCtx.getProperties().get("EntityAName");
        OneXOneUnidirectionalEntityEnum targetEntityAType = OneXOneUnidirectionalEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXOneUnidirectionalEntityEnum targetEntityBType = OneXOneUnidirectionalEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXOneUnidirectionalTestLogic.testCardinality001(): Begin");

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Construct new entity instances
            System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() + " (id=1)...");
            ICardinalEntityB new_entityB1 = (ICardinalEntityB) constructNewEntityObject(targetEntityBType);
            new_entityB1.setId(1);
            new_entityB1.setName("Entity B");

            System.out.println("Persisting " + new_entityB1);
            jpaResource.getEm().persist(new_entityB1);

            System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() + " (id=1)...");
            ICardinalEntityA new_entityA1 = (ICardinalEntityA) constructNewEntityObject(targetEntityAType);
            new_entityA1.setId(1);
            new_entityA1.setName("Entity A");

            System.out.println("Persisting " + new_entityA1);
            jpaResource.getEm().persist(new_entityA1);

            System.out.println("Creating relationship between " + targetEntityAType.getEntityName() + "(id=1) and " +
                               targetEntityBType.getEntityName() + "(id=1) via the 'b' relationship field...");
            new_entityA1.setBField(new_entityB1);

            System.out.println("All entities created, relationships established.  Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() + " (id=2)...");
            ICardinalEntityA new_entityA2 = (ICardinalEntityA) constructNewEntityObject(targetEntityAType);
            new_entityA2.setId(2);
            new_entityA2.setName("Entity B");

            System.out.println("Persisting " + new_entityA2);
            jpaResource.getEm().persist(new_entityA2);

            ICardinalEntityB find_entityB = (ICardinalEntityB) jpaResource.getEm().find(resolveEntityClass(targetEntityBType), 1);
            System.out.println("Creating relationship between " + targetEntityAType.getEntityName() + "(id=2) and " +
                               targetEntityBType.getEntityName() + "(id=1) via the 'b' relationship field...");
            new_entityA2.setBField(find_entityB);

            System.out.println("New entity created, relationships established.  Committing transaction (PersistenceException should be thrown)...");
            try {
                jpaResource.getTj().commitTransaction();

                // No Exception was thrown, this has failed.
                Assert.fail("Transaction Commit completed without an Exception being thrown.");
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Caught an Exception, check if PersistenceException is in the Exception Chain
                System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for PersistenceException...");
                assertExceptionIsInChain(PersistenceException.class, t);
            } finally {
                if (jpaResource.getTj().isTransactionActive()) {
                    System.out.println("Rolling back the transaction...");
                    jpaResource.getTj().rollbackTransaction();
                }
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("OneXOneUnidirectionalTestLogic.testCardinality001(): End");
        }
    }
}
