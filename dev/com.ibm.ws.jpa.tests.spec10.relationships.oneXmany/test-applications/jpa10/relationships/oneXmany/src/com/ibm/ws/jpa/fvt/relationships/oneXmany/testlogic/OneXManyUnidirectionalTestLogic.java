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

package com.ibm.ws.jpa.fvt.relationships.oneXmany.testlogic;

import java.util.Collection;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.IEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.IEntityB;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class OneXManyUnidirectionalTestLogic extends AbstractTestLogic {
    /**
     * Verify basic One-to-Many service by creating EntityA and EntityB, and
     * add EntityB to the collection EntityA.defaultRelationship.
     *
     * Test verifies function performing the following:
     * 1) Create new EntityB(id=1)
     * 2) Create new EntityA(id=1), add EntityB(id=1) to defaultRelationship.
     * 3) Keep other (optional) OneXMany collections empty.
     * 4) Clear persistence context (if necessary), and find EntityA(id=1).
     * 5) Verify field data in EntityA(id=1)
     * 6) EntityA.defaultRelationship should contain EntityB(id=1). Extract it
     * and verify its field data.
     * 7) Test passes if all these conditions are met.
     *
     * This test case confirms the following function verification:
     * 1) Optional relational fields can be empty without persistence errors
     * (all fields except defaultRelationship are empty in EntityA(id=1))
     * 2) A traversable OneXMany association between EntityA to EntityB(id=1) is
     * established, and EntityB is accessible through the relational
     * reference defaultRelationship in EntityA.
     *
     * Points: 10
     */
    public void testOneXManyUni001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXManyUnidirectionalTestLogic.testOneXManyUni001(): Missing context and/or resources.  Cannot execute the test.");
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
        OneXManyUnidirectionalEntityEnum targetEntityAType = OneXManyUnidirectionalEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXManyUnidirectionalEntityEnum targetEntityBType = OneXManyUnidirectionalEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXManyUnidirectionalTestLogic.testOneXManyUni001(): Begin");

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
                               targetEntityBType.getEntityName() + " via the 'direct' relationship field...");
            new_entityA.insertDefaultRelationshipField(new_entityB);

            System.out.println("Persisting " + new_entityA);
            jpaResource.getEm().persist(new_entityA);

            System.out.println("Both entities created, relationship established.  Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Start a transaction, many-relationship types, using collections, are lazy loaded
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

            // Examine the defaultRelationship field of EntityA.  It should not be null, should have an id=1, and
            // its name field should have a value of "Entity B". (6 points)
            System.out.println(
                               "Looking in " + targetEntityAType.getEntityName() +
                               "(id=1)'s defaultRelationship collection for " +
                               targetEntityBType.getEntityName() + "...");
            Collection drCollection = find_entityA.getDefaultRelationshipCollectionField();
            Assert.assertNotNull(
                                 "Assert that " + targetEntityAType.getEntityName() + "(id=1).defaultRelationship is not null.",
                                 drCollection);
            Assert.assertEquals(
                                "Assert that " + targetEntityAType.getEntityName() + "(id=1).defaultRelationship.size() == 1",
                                1,
                                drCollection.size());

            IEntityB dr_entityB = (IEntityB) drCollection.iterator().next();
            Assert.assertNotNull(
                                 "Assert that an " + targetEntityBType.getEntityName() + " was extracted from the defaultRelationship.",
                                 dr_entityB);
            Assert.assertNotSame(
                                 "Assert the extracted " + targetEntityBType.getEntityName() + " is not the same as the  original object",
                                 new_entityB,
                                 dr_entityB);
            Assert.assertTrue(
                              "Assert the extracted " + targetEntityBType.getEntityName() + " is managed by the persistence context.",
                              jpaResource.getEm().contains(dr_entityB));
            Assert.assertEquals(
                                "Assert the extracted " + targetEntityBType.getEntityName() + "'s id is 1",
                                dr_entityB.getId(),
                                1);

            System.out.println("Testing complete, rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();

            System.out.println("Ending test.");
        } finally {
            System.out.println("OneXManyUnidirectionalTestLogic.testOneXManyUni001(): End");
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
     * 1) Create an unpersisted UniEntityB(id=1).
     * 2) Create an unpersisted UniEntityA(id=1), add UniEntityB(id=1) to defaultRelationship.
     * 3) In a transaction, persist only UniEntityA(id=1) to the database.
     * Note that no persist command is invoked on UniEntityB(id=1), and since, by default,
     * persist operations are not cascaded across entity relationships, the persist operation
     * on UniEntityA(id=1) should not cause UniEntityB(id=1) to become persisted/managed.
     * 4) Clear persistence context, forcing all entities to become detached.
     * 5) Find UniEntityA(id=1), this should hit the database, returning an entity
     * with no entries in defaultRelationship because UniEntityB(id=1) was never persisted.
     *
     * Remove:
     *
     * By default, removing the entity on the owning side of the OneXMany relationship does
     * not cause the entity on the inverse side of the relationship to become removed as well.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), adding UniEntityB(id=1) to defaultRelationship.
     * 3) Remove UniEntityA(id=1), verify that UniEntityB(id=1) still exists. Test point passes if it does.
     *
     * If the entity on the inverse side of the relationship is removed, it should not remove the entity
     * on the owning of the relationship. Also, since the OneXMany relationship is optional, the relationship
     * field collection should be empty on fresh instances of the owning entity from find().
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), add UniEntityB(id=1) to defaultRelationship.
     * 3) Remove UniEntityB(id=1)
     * 4) Clear the persistence context
     * 5) Find UniEntityA(id=1). It should still exist (especially given that it is a unidirectional
     * relationship), and defaultRelationship should be empty.
     *
     * Merge:
     *
     * Merge will attempt to update the managed entity to point to managed versions of entities referenced by
     * the detached entity.
     *
     * 1) Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), add UniEntityB(id=1) to defaultRelationship.
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
     * 2) Create new UniEntityA(id=1), add UniEntityB(id=1) to defaultRelationship.
     * 3) Clear the persistence context
     * 4) In a single transaction, find UniEntityB(id=1) and remove it. This deletes UniEntityB(id=1) from the database.
     * Note: the original detached UniEntityB(id=1) reference still exists, and is referenced by the detached copy of
     * UniEntityA(id=1).
     * 5) Modify the name field of UniEntityA(id=1) and attempt to merge it. An IllegalArgumentException because the
     * detached entity still has a references in its defaultRelationship field to UniEntityB(id=1), which no longer
     * exists in the database.
     *
     * Refresh:
     *
     * Refresh operations are, by default, not cascaded across entity relationships. Without the REFRESH cascade
     * option, a refresh operation will stop at the source entity.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), add UniEntityB(id=1) to defaultRelationship.
     * 3) Start a new transaction
     * 4) With managed copies of the two entities, edit UniEntityA(id=1) and UniEntityA(id=1).defaultRelationship's name fields.
     * 5) Invoke EntityManager.refresh() on UniEntityA(id=1)
     * 6) Verify that UniEntityA(id=1)'s name field has been reverted to the value it had when it was created.
     * 7) Verify that UniEntityB(id=1) still has the new value
     *
     */
    public void testOneXManyUni002(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXManyUnidirectionalTestLogic.testOneXManyUni002(): Missing context and/or resources.  Cannot execute the test.");
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
        OneXManyUnidirectionalEntityEnum targetEntityAType = OneXManyUnidirectionalEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXManyUnidirectionalEntityEnum targetEntityBType = OneXManyUnidirectionalEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXManyUnidirectionalTestLogic.testOneXManyUni002(): Begin");

            // ----------------------------------------------------------------------------------------------------
            // Verify Default Default Persist Cascade Behavior (1 POINT)
            // Both entities in the relationship need to have persist() invoked on them in order to be stored on
            // the database.  By default, persisting the owning side of the relationship does not automatically
            // persist the entity on the inverse side, and vice versa.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify Default Default Persist Cascade Behavior:");
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
                                   targetEntityBType.getEntityName() + " via the 'direct' relationship field...");
                new_entityA.insertDefaultRelationshipField(new_entityB);

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
            // Verify Default Default Remove Cascade Behavior (3 POINTS)
            // By default, removing the entity on the owning side of the oneXmany relationship does
            // not cause the entity on the inverse side of the relationship to become removed as well.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify Default Default Remove Cascade Behavior:");
                System.out.println(
                                   "By default, removing the entity on the owning side of the oneXmany relationship does "
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
                                   targetEntityBType.getEntityName() + " via the 'direct' relationship field...");
                new_entityA.insertDefaultRelationshipField(new_entityB);

                System.out.println("Persisting " + new_entityA);
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Both entities created, relationship established.  Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Entities have been persisted to the database, with a one-to-many relationship between
                // EntityA and EntityB established.  Now remove EntityA from the database.  Because, by default,
                // remove operations are not cascaded across entity relationships, EntityB should survive
                // EntityA's removal.
                System.out.println(
                                   "Entities have been persisted to the database, with a one-to-many relationship between " +
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
            // Verify Default Default Remove Cascade (INVERSE) Behavior (3 POINTS)
            // If the entity on the inverse side of the relationship is removed, it should not remove the entity
            // on the owning of the relationship.  Also, since the oneXone relationship is optional, the relationship
            // field should be set null on fresh instances of the owning entity from find().

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify Default Default Remove Cascade (INVERSE) Behavior:");
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
                                   targetEntityBType.getEntityName() + " via the 'direct' relationship field...");
                new_entityA.insertDefaultRelationshipField(new_entityB);

                System.out.println("Persisting " + new_entityA);
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Both entities created, relationship established.  Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Entities have been persisted to the database, with a one-to-many relationship between
                // EntityA and EntityB established.  Now remove EntityA from the database.  Because, by default,
                // remove operations are not cascaded across entity relationships, EntityA should survive
                // EntityB's removal.

                System.out.println(
                                   "Entities have been persisted to the databae, with a one-to-many relationship between " +
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
            // Verify Default Default Merge Cascade Behavior (14 POINTS)
            // Merge will attempt to update the managed entity to point to managed versions of entities referenced by
            // the detached entity.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify Default Default Merge Cascade Behavior:");
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
                                   targetEntityBType.getEntityName() + " via the 'direct' relationship field...");
                new_entityA.insertDefaultRelationshipField(new_entityB);

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
                Collection dcCollection = mergedEntityA.getDefaultRelationshipCollectionField();
                Assert.assertNotNull(
                                     "Assert " + targetEntityAType.getEntityName() + ".getDefaultRelationshipCollectionField() " +
                                     " is not null",
                                     dcCollection);
                Assert.assertEquals("Assert the collection has a size of 1.", 1, dcCollection.size());

                System.out.println(
                                   "Extracting " + targetEntityBType.getEntityName() + " from the merged " +
                                   targetEntityAType.getEntityName() + "'s DefaultRelationshipCollectionField collection.");
                IEntityB entityBFromMergedEntityA = (IEntityB) dcCollection.iterator().next();
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
            // Verify Default Default Refresh Cascade Behavior (10 POINTS)
            // Refresh operations are, by default, not cascaded across entity relationships.  Without the REFRESH
            // cascade option, a refresh operation will stop at the source entity.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify Default Default Refresh Cascade Behavior:");
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
                                   targetEntityBType.getEntityName() + " via the 'direct' relationship field...");
                new_entityA.insertDefaultRelationshipField(new_entityB);

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
                                   targetEntityAType.getEntityName() + "'s DefaultRelationshipCollectionField collection.");
                Collection dcCollection = find_entityA.getDefaultRelationshipCollectionField();
                IEntityB entityBFromEntityA = (IEntityB) dcCollection.iterator().next();
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
            System.out.println("OneXManyUnidirectionalTestLogic.testOneXManyUni002(): End");
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
     * 3) Add UniEntityB(id=1) to cascadeAll.
     * 4) Start a new transaction, persist UniEntityA(id=1), and commit the transaction.
     * 5) Clear persistence context, forcing all entities to become detached.
     * 6) Find UniEntityA(id=1), this should hit the database, returning an entity
     * with a reference in cascadeAll to UniEntityB(id=1) because the persist operation
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
     * 2) Create new UniEntityA(id=1), add UniEntityB(id=1) to cascadeAll.
     * 3) Remove UniEntityA(id=1), verify that UniEntityB(id=1) also no longer exists. Test point passes if true.
     *
     * Merge:
     *
     * With cascade type set to ALL, merge operations will cascade across the entity relationship. So changes to
     * the target entity (UniEntityA(id=1)) and changes to all entities with relationship cascade attributes of
     * ALL (and MERGE) will be merged into the persistence context.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), add UniEntityB(id=1) to cascadeAll.
     * 3) Clear the persistence context, causing all entities to become detached
     * 4) Modify UniEntityA(id=1)'s name field. Modify UniEntityB(id=1)'s name field.
     * 5) Merge UniEntityA(id=1) into the persistence context. Examine the UniEntityB(id=1) referenced by the cascadeAll
     * field of the entity returned from the merge() operation. Both entities should be managed, and both
     * entities should contain the changes from step 4.
     *
     * Refresh:
     *
     * With cascade type set to ALL, refresh operations will cascade across entity relationships. So changes
     * to the target entity (UniEntityA(id=1)) and changes to all entities with relationship cascade attributes of
     * ALL (and REFRESH) will have their contents reset to match the data in the database.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), add UniEntityB(id=1) to cascadeAll.
     * 3) Start a new transaction
     * 4) With managed copies of the two entities, edit UniEntityA(id=1) and UniEntityA(id=1).cascadeAll's name fields.
     * 5) Invoke EntityManager.refresh() on UniEntityA(id=1)
     * 6) Verify that UniEntityA(id=1)'s name field has been reverted to the value it had when it was created.
     * 7) Verify that UniEntityB(id=1)'s name field has been reverted to the value it had when it was created.
     *
     */
    @SuppressWarnings("rawtypes")
    public void testOneXManyUni003(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXManyUnidirectionalTestLogic.testOneXManyUni003(): Missing context and/or resources.  Cannot execute the test.");
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
        OneXManyUnidirectionalEntityEnum targetEntityAType = OneXManyUnidirectionalEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXManyUnidirectionalEntityEnum targetEntityBType = OneXManyUnidirectionalEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXManyUnidirectionalTestLogic.testOneXManyUni003(): Begin");

            // ----------------------------------------------------------------------------------------------------
            // Verify Cascade: ALL Persist Behavior (8 POINTS)
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
                                   + "mplementation will automatically invoke EntityManager.persist() on all of the EntityB entity "
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
                new_entityA.insertCascadeAllField(new_entityB);

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
                System.out.println("Extracting " + targetEntityBType.getEntityName() + " from the merged " +
                                   targetEntityAType.getEntityName() + "'s CascadeAllCollectionField collection.");
                Collection caCollection = find_entityA.getCascadeAllCollectionField();
                Assert.assertNotNull("Assert that CascadeAllCollectionField is not null.", caCollection);
                Assert.assertEquals("Assert that CascadeAllCollectionField has size of 1.", 1, caCollection.size());

                IEntityB entityBFromEntityACACollection = (IEntityB) caCollection.iterator().next();
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
                new_entityA.insertCascadeAllField(new_entityB);

                System.out.println("Persisting " + new_entityA);
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Both entities created, relationship established.  Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Entities have been persisted to the database, with a one-to-many relationship between
                // EntityA and EntityB established.  Now remove EntityA from the database.  Because of the CASCADE Cfg
                // remove operations are cascaded across entity relationships, EntityB should NOT survive
                // EntityA's removal.
                System.out.println(
                                   "Entities have been persisted to the database, with a one-to-many relationship between " +
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
            // Verify Cascade: ALL Merge Behavior (14 POINTS)
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
                new_entityA.insertCascadeAllField(new_entityB);

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
                Collection caCollection = mergedEntityA.getCascadeAllCollectionField();
                Assert.assertNotNull(
                                     "Assert " + targetEntityAType.getEntityName() + ".getCascadeAllCollectionField() " +
                                     " is not null",
                                     caCollection);
                Assert.assertEquals("Assert the collection has a size of 1.", 1, caCollection.size());

                System.out.println(
                                   "Extracting " + targetEntityBType.getEntityName() + " from the merged " +
                                   targetEntityAType.getEntityName() + "'s CascadeAllRelationshipCollectionField collection.");
                IEntityB entityBFromMergedEntityA = (IEntityB) caCollection.iterator().next();
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
                new_entityA.insertCascadeAllField(new_entityB);

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
                                   targetEntityAType.getEntityName() + "'s CascadeAllRelationshipCollectionField collection.");
                Collection caCollection = find_entityA.getCascadeAllCollectionField();
                IEntityB entityBFromEntityA = (IEntityB) caCollection.iterator().next();
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
            System.out.println("OneXManyUnidirectionalTestLogic.testOneXManyUni003(): End");
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
     * 3) Add UniEntityB(id=1) to cascadePersist.
     * 4) Start a new transaction, persist UniEntityA(id=1), and commit the transaction.
     * 5) Clear persistence context, forcing all entities to become detached.
     * 6) Find UniEntityA(id=1), this should hit the database, returning an entity
     * with a reference in cascadePersist to UniEntityB(id=1) because the persist operation
     * was cascaded.
     *
     */
    @SuppressWarnings("rawtypes")
    public void testOneXManyUni004(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXManyUnidirectionalTestLogic.testOneXManyUni004(): Missing context and/or resources.  Cannot execute the test.");
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
        OneXManyUnidirectionalEntityEnum targetEntityAType = OneXManyUnidirectionalEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXManyUnidirectionalEntityEnum targetEntityBType = OneXManyUnidirectionalEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXManyUnidirectionalTestLogic.testOneXManyUni004(): Begin");

            // ----------------------------------------------------------------------------------------------------
            // Verify Cascade: ALL Persist Behavior (8 POINTS)
            // With cascade type set to PERSIST, persist operations are cascaded across the entity relationship.
            // This means if EntityA is the target if an EntityManager.persist() operation, the JPA
            // implementation will automatically invoke EntityManager.persist() on all of the UniEntityB entity
            // relationships that are marked with Cascade type PERSIST.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify Cascade: PERSIST Persist Behavior:");
                System.out.println(
                                   "With cascade type set to PERSIST, persist operations are cascaded across the entity relationship.  "
                                   + "This means if EntityA is the target if an EntityManager.persist() operation, the JPA "
                                   + "mplementation will automatically invoke EntityManager.persist() on all of the EntityB entity "
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
                new_entityA.insertCascadePersistField(new_entityB);

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
                System.out.println("Extracting " + targetEntityBType.getEntityName() + " from the merged " +
                                   targetEntityAType.getEntityName() + "'s CascadePersistCollectionField collection.");
                Collection cpCollection = find_entityA.getCascadePersistCollectionField();
                Assert.assertNotNull("Assert that CascadePersistCollectionField is not null.", cpCollection);
                Assert.assertEquals("Assert that CascadePersistCollectionField has size of 1.", 1, cpCollection.size());

                IEntityB entityBFromEntityACPCollection = (IEntityB) cpCollection.iterator().next();
                Assert.assertNotNull("Assert the extraction from the collection did not return a null", entityBFromEntityACPCollection);
                Assert.assertTrue(
                                  "Assert that " + targetEntityBType.getEntityName() + " is managed.",
                                  jpaResource.getEm().contains(entityBFromEntityACPCollection));
                Assert.assertNotSame("Assert that this is not the original entity object",
                                     new_entityB, entityBFromEntityACPCollection);

                System.out.println("Rolling back transaction...");
                jpaResource.getTj().rollbackTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();
            }

            // ----------------------------------------------------------------------------------------------------
            // Verify CASCADE:PERSIST Remove Cascade Behavior (3 POINTS)
            // With CASCADE:PERSIST only, removing the entity on the owning side of the oneXmany relationship does
            // not cause the entity on the inverse side of the relationship to become removed as well.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify CASCADE:PERSIST Remove Cascade Behavior:");
                System.out.println(
                                   "With CASCADE:PERSIST only, removing the entity on the owning side of the oneXmany relationship does "
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
                                   targetEntityBType.getEntityName() + " via the 'cascadePersist' relationship field...");
                new_entityA.insertCascadePersistField(new_entityB);

                System.out.println("Persisting " + new_entityA);
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Both entities created, relationship established.  Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Entities have been persisted to the database, with a one-to-many relationship between
                // EntityA and EntityB established.  Now remove EntityA from the database.  Because
                // remove operations are not cascaded across entity relationships, EntityB should survive
                // EntityA's removal.
                System.out.println(
                                   "Entities have been persisted to the database, with a one-to-many relationship between " +
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
            // Verify CASCADE:PERSIST Remove Cascade (INVERSE) Behavior (3 POINTS)
            // If the entity on the inverse side of the relationship is removed, it should not remove the entity
            // on the owning of the relationship.  Also, since the oneXone relationship is optional, the relationship
            // field should be set null on fresh instances of the owning entity from find().

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify Default Default Remove Cascade (INVERSE) Behavior:");
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
                                   targetEntityBType.getEntityName() + " via the 'cascadePersist' relationship field...");
                new_entityA.insertCascadePersistField(new_entityB);

                System.out.println("Persisting " + new_entityA);
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Both entities created, relationship established.  Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Entities have been persisted to the database, with a one-to-many relationship between
                // EntityA and EntityB established.  Now remove EntityA from the database.  Because
                // remove operations are not cascaded across entity relationships, EntityA should survive
                // EntityB's removal.

                System.out.println(
                                   "Entities have been persisted to the databae, with a one-to-many relationship between " +
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
            // Verify CASCADE:PERSIST Merge Cascade Behavior (14 POINTS)
            // Merge will attempt to update the managed entity to point to managed versions of entities referenced by
            // the detached entity.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify CASCADE:PERSIST Merge Cascade Behavior:");
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
                                   targetEntityBType.getEntityName() + " via the 'cascadePersist' relationship field...");
                new_entityA.insertCascadePersistField(new_entityB);

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
                                   "Merge EntityA(id=" + pkey + ") into the persistence context and verify that the cascadePersist field of the " +
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
                Collection cpCollection = mergedEntityA.getCascadePersistCollectionField();
                Assert.assertNotNull(
                                     "Assert " + targetEntityAType.getEntityName() + ".getCascadePersistRelationshipCollectionField() " +
                                     " is not null",
                                     cpCollection);
                Assert.assertEquals("Assert the collection has a size of 1.", 1, cpCollection.size());

                System.out.println(
                                   "Extracting " + targetEntityBType.getEntityName() + " from the merged " +
                                   targetEntityAType.getEntityName() + "'s CascadePersistRelationshipCollectionField collection.");
                IEntityB entityBFromMergedEntityA = (IEntityB) cpCollection.iterator().next();
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
            // Verify CASCADE:PERSIST Refresh Cascade Behavior (10 POINTS)
            // Refresh operations are, by default, not cascaded across entity relationships.  Without the REFRESH
            // cascade option, a refresh operation will stop at the source entity.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify CASCADE:PERSIST Refresh Cascade Behavior:");
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
                                   targetEntityBType.getEntityName() + " via the 'cascadePersist' relationship field...");
                new_entityA.insertCascadePersistField(new_entityB);

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
                                   targetEntityAType.getEntityName() + "'s CascadePersistRelationshipCollectionField collection.");
                Collection cpCollection = find_entityA.getCascadePersistCollectionField();
                IEntityB entityBFromEntityA = (IEntityB) cpCollection.iterator().next();
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
                                   "in its cascadePersist field, so the changes to that entity should remain.");
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
            System.out.println("OneXManyUnidirectionalTestLogic.testOneXManyUni004(): End");
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
     * 2) Create new UniEntityA(id=1), add UniEntityB(id=1) to cascadeRemove.
     * 3) Remove UniEntityA(id=1), verify that UniEntityB(id=1) also no longer exists. Test point passes if true.
     *
     *
     */
    @SuppressWarnings("rawtypes")
    public void testOneXManyUni005(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXManyUnidirectionalTestLogic.testOneXManyUni005(): Missing context and/or resources.  Cannot execute the test.");
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
        OneXManyUnidirectionalEntityEnum targetEntityAType = OneXManyUnidirectionalEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXManyUnidirectionalEntityEnum targetEntityBType = OneXManyUnidirectionalEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXManyUnidirectionalTestLogic.testOneXManyUni005(): Begin");

            // ----------------------------------------------------------------------------------------------------
            // Verify CASCADE:REMOVE Persist Cascade Behavior (1 POINT)
            // Both entities in the relationship need to have persist() invoked on them in order to be stored on
            // the database.  By default, persisting the owning side of the relationship does not automatically
            // persist the entity on the inverse side, and vice versa.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify CASCADE:REMOVE Persist Cascade Behavior:");
                System.out.println(
                                   "Both entities in the relationship need to have persist() invoked on them in order to be stored "
                                   + "on the database.  Persisting the owning side of the relationship does not "
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
                                   targetEntityBType.getEntityName() + " via the 'cascadeRemove' relationship field...");
                new_entityA.insertCascadeRemoveField(new_entityB);

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
            // Verify Cascade:REMOVE Remove Behavior (4 POINTS) +1
            // With cascade type set to REMOVE, remove operations are cascaded across the entity relationship.
            // This means if EntityA is the target of an EntityManager.remove() operation, the JPA
            // implementation will automatically invoke EntityManager.remove() on all of the UniEntityB entity
            // relationships that are marked with Cascade type ALL (or REMOVE).

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify Cascade: REMOVE Remove Behavior");
                System.out.println(
                                   "With cascade type set to REMOVE, remove operations are cascaded across the entity relationship.  "
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
                new_entityA.insertCascadeRemoveField(new_entityB);

                System.out.println("Persisting " + new_entityA);
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Both entities created, relationship established.  Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Entities have been persisted to the database, with a one-to-many relationship between
                // EntityA and EntityB established.  Now remove EntityA from the database.  Because of the CASCADE Cfg
                // remove operations are cascaded across entity relationships, EntityB should NOT survive
                // EntityA's removal.
                System.out.println(
                                   "Entities have been persisted to the database, with a one-to-many relationship between " +
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

            // ----------------------------------------------------------------------------------------------------
            // Verify CASCADE:REMOVE Merge Cascade Behavior (14 POINTS)
            // Merge will attempt to update the managed entity to point to managed versions of entities referenced by
            // the detached entity.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify CASCADE:REMOVE Merge Cascade Behavior:");
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
                                   targetEntityBType.getEntityName() + " via the 'cascadeRemove' relationship field...");
                new_entityA.insertCascadeRemoveField(new_entityB);

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

                // Merge EntityA(id=4) into the persistence context and verify that the cascadeRemove field of the copy of
                // EntityA(id=4) returned by the merge operation reflects the state of EntityB(id=4) in the database
                // (name field should contain original value)
                System.out.println(
                                   "Merge EntityA(id=" + pkey + ") into the persistence context and verify that the cascadeRemove field of the " +
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
                Collection crCollection = mergedEntityA.getCascadeRemoveCollectionField();
                Assert.assertNotNull(
                                     "Assert " + targetEntityAType.getEntityName() + ".getCascadeRemoveCollectionField() " +
                                     " is not null",
                                     crCollection);
                Assert.assertEquals("Assert the collection has a size of 1.", 1, crCollection.size());

                System.out.println(
                                   "Extracting " + targetEntityBType.getEntityName() + " from the merged " +
                                   targetEntityAType.getEntityName() + "'s CascadeRemoveRelationshipCollectionField collection.");
                IEntityB entityBFromMergedEntityA = (IEntityB) crCollection.iterator().next();
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
            // Verify CASCADE:REMOVE Refresh Cascade Behavior (10 POINTS)
            // Refresh operations are, by default, not cascaded across entity relationships.  Without the REFRESH
            // cascade option, a refresh operation will stop at the source entity.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify CASCADE:REMOVE Refresh Cascade Behavior:");
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
                                   targetEntityBType.getEntityName() + " via the 'cascadeRemove' relationship field...");
                new_entityA.insertCascadeRemoveField(new_entityB);

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
                                   targetEntityAType.getEntityName() + "'s CascadeRemoveRelationshipCollectionField collection.");
                Collection crCollection = find_entityA.getCascadeRemoveCollectionField();
                IEntityB entityBFromEntityA = (IEntityB) crCollection.iterator().next();
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
            System.out.println("OneXManyUnidirectionalTestLogic.testOneXManyUni005(): End");
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
     * 2) Create new UniEntityA(id=1), add UniEntityB(id=1) to cascadeMerge.
     * 3) Clear the persistence context, causing all entities to become detached
     * 4) Modify UniEntityA(id=1)'s name field. Modify UniEntityB(id=1)'s name field.
     * 5) Merge UniEntityA(id=1) into the persistence context. Examine the UniEntityB(id=1) referenced by the cascadeMerge
     * field of the entity returned from the merge() operation. Both entities should be managed, and both
     * entities should contain the changes from step 4.
     *
     */
    @SuppressWarnings("rawtypes")
    public void testOneXManyUni006(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXManyUnidirectionalTestLogic.testOneXManyUni006(): Missing context and/or resources.  Cannot execute the test.");
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
        OneXManyUnidirectionalEntityEnum targetEntityAType = OneXManyUnidirectionalEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXManyUnidirectionalEntityEnum targetEntityBType = OneXManyUnidirectionalEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXManyUnidirectionalTestLogic.testOneXManyUni006(): Begin");

            // ----------------------------------------------------------------------------------------------------
            // Verify CASCADE:MERGE Persist Cascade Behavior (1 POINT)
            // Both entities in the relationship need to have persist() invoked on them in order to be stored on
            // the database.  By default, persisting the owning side of the relationship does not automatically
            // persist the entity on the inverse side, and vice versa.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify CASCADE:MERGE Persist Cascade Behavior:");
                System.out.println(
                                   "Both entities in the relationship need to have persist() invoked on them in order to be stored "
                                   + "on the database.  Persisting the owning side of the relationship does not "
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
                                   targetEntityBType.getEntityName() + " via the 'cascadeMerge' relationship field...");
                new_entityA.insertCascadeMergeField(new_entityB);

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
            // Verify CASCADE:MERGE Remove Cascade Behavior (3 POINTS)
            // Removing the entity on the owning side of the oneXmany relationship does
            // not cause the entity on the inverse side of the relationship to become removed as well.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify CASCADE:MERGE Remove Cascade Behavior:");
                System.out.println(
                                   "Removing the entity on the owning side of the oneXmany relationship does "
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
                                   targetEntityBType.getEntityName() + " via the 'cascadeMerge' relationship field...");
                new_entityA.insertCascadeMergeField(new_entityB);

                System.out.println("Persisting " + new_entityA);
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Both entities created, relationship established.  Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Entities have been persisted to the database, with a one-to-many relationship between
                // EntityA and EntityB established.  Now remove EntityA from the database.  Because
                // remove operations are not cascaded across entity relationships, EntityB should survive
                // EntityA's removal.
                System.out.println(
                                   "Entities have been persisted to the database, with a one-to-many relationship between " +
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
            // Verify CASCADE:MERGE Remove Cascade (INVERSE) Behavior (3 POINTS)
            // If the entity on the inverse side of the relationship is removed, it should not remove the entity
            // on the owning of the relationship.  Also, since the oneXone relationship is optional, the relationship
            // field should be set null on fresh instances of the owning entity from find().

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify CASCADE:MERGE Remove Cascade (INVERSE) Behavior:");
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
                                   targetEntityBType.getEntityName() + " via the 'cascadeMerge' relationship field...");
                new_entityA.insertCascadeMergeField(new_entityB);

                System.out.println("Persisting " + new_entityA);
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Both entities created, relationship established.  Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Entities have been persisted to the database, with a one-to-many relationship between
                // EntityA and EntityB established.  Now remove EntityA from the database.  Because, by default,
                // remove operations are not cascaded across entity relationships, EntityA should survive
                // EntityB's removal.

                System.out.println(
                                   "Entities have been persisted to the database, with a one-to-many relationship between " +
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
            // Verify Cascade:MERGE Merge Behavior (14 POINTS)
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
                new_entityA.insertCascadeMergeField(new_entityB);

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

                // Merge EntityA(id=4) into the persistence context and verify that the cascadeMerge field of the copy of
                // EntityA(id=4) returned by the merge operation reflects the state of EntityB(id=4) in the database
                // (name field should contain mutated value)
                System.out.println(
                                   "Merge EntityA(id=" + pkey + ") into the persistence context and verify that the cascadeMerge field of the " +
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
                Collection cmCollection = mergedEntityA.getCascadeMergeCollectionField();
                Assert.assertNotNull(
                                     "Assert " + targetEntityAType.getEntityName() + ".getCascadeMergeCollectionField() " +
                                     " is not null",
                                     cmCollection);
                Assert.assertEquals("Assert the collection has a size of 1.", 1, cmCollection.size());

                System.out.println(
                                   "Extracting " + targetEntityBType.getEntityName() + " from the merged " +
                                   targetEntityAType.getEntityName() + "'s CascadeMergeRelationshipCollectionField collection.");
                IEntityB entityBFromMergedEntityA = (IEntityB) cmCollection.iterator().next();
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
            // Verify Cascade:MERGE Refresh Cascade Behavior (10 POINTS)
            // Refresh operations are, by default, not cascaded across entity relationships.  Without the REFRESH
            // cascade option, a refresh operation will stop at the source entity.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify Cascade:MERGE Refresh Cascade Behavior:");
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
                                   targetEntityBType.getEntityName() + " via the 'cascadeMerge' relationship field...");
                new_entityA.insertCascadeMergeField(new_entityB);

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
                                   targetEntityAType.getEntityName() + "'s CascadeMergeRelationshipCollectionField collection.");
                Collection cmCollection = find_entityA.getCascadeMergeCollectionField();
                IEntityB entityBFromEntityA = (IEntityB) cmCollection.iterator().next();
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
                // The refresh operation should not cascade to the entity referenced in its cascadeMerge field, so the
                // changes to that entity should remain.
                System.out.println(
                                   "Now, invoking the refresh() operation on EntityA.  Its values should be reset to what " +
                                   "is in the database.  The refresh operation should not cascade to the entity referenced " +
                                   "in its cascadeMerge field, so the changes to that entity should remain.");
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
            System.out.println("OneXManyUnidirectionalTestLogic.testOneXManyUni006(): End");
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
     * 2) Create new UniEntityA(id=1), add UniEntityB(id=1) to cascadeRefresh.
     * 3) Start a new transaction
     * 4) With managed copies of the two entities, edit UniEntityA(id=1) and UniEntityA(id=1).cascadeRefresh's name fields.
     * 5) Invoke EntityManager.refresh() on UniEntityA(id=1)
     * 6) Verify that UniEntityA(id=1)'s name field has been reverted to the value it had when it was created.
     * 7) Verify that UniEntityB(id=1)'s name field has been reverted to the value it had when it was created.
     *
     */
    @SuppressWarnings("rawtypes")
    public void testOneXManyUni007(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXManyUnidirectionalTestLogic.testOneXManyUni007(): Missing context and/or resources.  Cannot execute the test.");
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
        OneXManyUnidirectionalEntityEnum targetEntityAType = OneXManyUnidirectionalEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXManyUnidirectionalEntityEnum targetEntityBType = OneXManyUnidirectionalEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXManyUnidirectionalTestLogic.testOneXManyUni007(): Begin");

            // ----------------------------------------------------------------------------------------------------
            // Verify CASCADE:REFRESH Persist Cascade Behavior (1 POINT)
            // Both entities in the relationship need to have persist() invoked on them in order to be stored on
            // the database.  By default, persisting the owning side of the relationship does not automatically
            // persist the entity on the inverse side, and vice versa.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify CASCADE:REFRESH Persist Cascade Behavior:");
                System.out.println(
                                   "Both entities in the relationship need to have persist() invoked on them in order to be stored "
                                   + "on the database.  Persisting the owning side of the relationship does not "
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
                                   targetEntityBType.getEntityName() + " via the 'cascadeRefresh' relationship field...");
                new_entityA.insertCascadeRefreshField(new_entityB);

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
            // Verify CASCADE:REFRESH Remove Cascade Behavior (3 POINTS)
            // Removing the entity on the owning side of the oneXmany relationship does
            // not cause the entity on the inverse side of the relationship to become removed as well.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify CASCADE:REFRESH Remove Cascade Behavior:");
                System.out.println(
                                   "Removing the entity on the owning side of the oneXmany relationship does "
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
                                   targetEntityBType.getEntityName() + " via the 'cascadeRefresh' relationship field...");
                new_entityA.insertCascadeRefreshField(new_entityB);

                System.out.println("Persisting " + new_entityA);
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Both entities created, relationship established.  Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Entities have been persisted to the database, with a one-to-many relationship between
                // EntityA and EntityB established.  Now remove EntityA from the database.  Because ,
                // remove operations are not cascaded across entity relationships, EntityB should survive
                // EntityA's removal.
                System.out.println(
                                   "Entities have been persisted to the database, with a one-to-many relationship between " +
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
            // Verify CASCADE:REFRESH Remove Cascade (INVERSE) Behavior (3 POINTS)
            // If the entity on the inverse side of the relationship is removed, it should not remove the entity
            // on the owning of the relationship.  Also, since the oneXone relationship is optional, the relationship
            // field should be set null on fresh instances of the owning entity from find().

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify CASCADE:REFRESH Remove Cascade (INVERSE) Behavior:");
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
                                   targetEntityBType.getEntityName() + " via the 'cascadeRefresh' relationship field...");
                new_entityA.insertCascadeRefreshField(new_entityB);

                System.out.println("Persisting " + new_entityA);
                jpaResource.getEm().persist(new_entityA);

                System.out.println("Both entities created, relationship established.  Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Entities have been persisted to the database, with a one-to-many relationship between
                // EntityA and EntityB established.  Now remove EntityA from the database.  Because
                // remove operations are not cascaded across entity relationships, EntityA should survive
                // EntityB's removal.

                System.out.println(
                                   "Entities have been persisted to the databae, with a one-to-many relationship between " +
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
            // Verify CASCADE:REFRESH Merge Cascade Behavior (14 POINTS)
            // Merge will attempt to update the managed entity to point to managed versions of entities referenced by
            // the detached entity.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify CASCADE:REFRESH Merge Cascade Behavior:");
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
                                   targetEntityBType.getEntityName() + " via the 'cascadeRefresh' relationship field...");
                new_entityA.insertCascadeRefreshField(new_entityB);

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
                                   "Merge EntityA(id=" + pkey + ") into the persistence context and verify that the cascadeRefresh field of the " +
                                   "copy of " + targetEntityAType.getEntityName() + " returned by the merge operation reflects " +
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
                Collection crCollection = mergedEntityA.getCascadeRefreshCollectionField();
                Assert.assertNotNull(
                                     "Assert " + targetEntityAType.getEntityName() + ".getCascadeRefreshCollectionField() " +
                                     " is not null",
                                     crCollection);
                Assert.assertEquals("Assert the collection has a size of 1.", 1, crCollection.size());

                System.out.println(
                                   "Extracting " + targetEntityBType.getEntityName() + " from the merged " +
                                   targetEntityAType.getEntityName() + "'s CascadeRefreshRelationshipCollectionField collection.");
                IEntityB entityBFromMergedEntityA = (IEntityB) crCollection.iterator().next();
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
            // Verify Cascade: REFRESH Refresh Behavior (10 POINTS)
            // With cascade type set to REFRESH, refresh operations will cascade across entity relationships.  So changes
            // to the target entity (EntityA(id=1)) and changes to all entities with relationship cascade attributes
            // REFRESH will have their contents reset to match the data in the database.

            {
                System.out.println("----------------------------------------------------------------------------------------------------");
                System.out.println("Verify Cascade: REFRESH Refresh Behavior:");
                System.out.println(
                                   "With cascade type set to REFRESH, refresh operations will cascade across entity relationships.  "
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
                new_entityA.insertCascadeRefreshField(new_entityB);

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
                                   targetEntityAType.getEntityName() + "'s CascadeRefreshRelationshipCollectionField collection.");
                Collection crCollection = find_entityA.getCascadeRefreshCollectionField();
                IEntityB entityBFromEntityA = (IEntityB) crCollection.iterator().next();
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
            System.out.println("OneXManyUnidirectionalTestLogic.testOneXManyUni007(): End");
        }
    }

    /**
     * Cardinality Test: Verify that multiple entities can be stored in the relational collection. (7 POINTS)
     *
     * 1) Create UniEntityB(id=1), save to database.
     * 2) Create UniEntityB(id=2), save to database.
     * 3) Create new UniEntityA(id=1), add UniEntityB(id=1) and UniEntityB(id=2) to defaultRelationship, and save to database.
     * 4) Create new UniEntityA(id=2), add UniEntityB(id=1) and UniEntityB(id=2) to defaultRelationship, and save to database.
     * 5) Clear the persistence context, and verify EntityA's database state. Test passes if EntityA's
     * defaultRelationship collections have references to both UniEntityB.
     *
     */
    @SuppressWarnings({ "unchecked" })
    public void testCardinality001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXManyUnidirectionalTestLogic.testCardinality001(): Missing context and/or resources.  Cannot execute the test.");
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
        OneXManyUnidirectionalEntityEnum targetEntityAType = OneXManyUnidirectionalEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXManyUnidirectionalEntityEnum targetEntityBType = OneXManyUnidirectionalEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXManyUnidirectionalTestLogic.testCardinality001(): Begin");

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
            IEntityB new_entityB1 = (IEntityB) constructNewEntityObject(targetEntityBType);
            new_entityB1.setId(1);
            new_entityB1.setName("Entity B");

            System.out.println("Persisting " + new_entityB1);
            jpaResource.getEm().persist(new_entityB1);

            System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() + " (id=2)...");
            IEntityB new_entityB2 = (IEntityB) constructNewEntityObject(targetEntityBType);
            new_entityB2.setId(2);
            new_entityB2.setName("Entity B");

            System.out.println("Persisting " + new_entityB2);
            jpaResource.getEm().persist(new_entityB2);

            System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() + " (id=1)...");
            IEntityA new_entityA1 = (IEntityA) constructNewEntityObject(targetEntityAType);
            new_entityA1.setId(1);
            new_entityA1.setName("Entity A");

            System.out.println("Persisting " + new_entityA1);
            jpaResource.getEm().persist(new_entityA1);

            System.out.println("Establish object relationships...");

            System.out.println("Creating relationship between " + targetEntityAType.getEntityName() + "(id=1) and " +
                               targetEntityBType.getEntityName() + "(id=1) via the 'default' relationship field...");
            new_entityA1.insertDefaultRelationshipField(new_entityB1);

            System.out.println("Creating relationship between " + targetEntityAType.getEntityName() + "(id=1) and " +
                               targetEntityBType.getEntityName() + "(id=2) via the 'default' relationship field...");
            new_entityA1.insertDefaultRelationshipField(new_entityB2);

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

            System.out.println("Finding " + targetEntityAType.getEntityName() + "(id=1)...");
            IEntityA find_entityA1 = (IEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), 1);
            System.out.println("Object returned by find: " + find_entityA1);

            // Verify that em.find() returned an object. (1 point)
            Assert.assertNotNull("Assert that the find operation did not return null", find_entityA1);

            //  Perform basic verifications (3 points)
            Assert.assertNotSame(
                                 "Assert find did not return the original object",
                                 new_entityA1,
                                 find_entityA1);
            Assert.assertTrue(
                              "Assert entity returned by find is managed by the persistence context.",
                              jpaResource.getEm().contains(find_entityA1));
            Assert.assertEquals(
                                "Assert that the entity's id is 1",
                                find_entityA1.getId(),
                                1);

            // Now, examine the one-to-many relationship across the defaultRelationship relational field
            // on both EntityA entities.  Both should point to the same (two) EntityB objects.
            System.out.println("Examining the defaultRelationship Collections on both instances of EntityA...");

            Collection<IEntityB> dcCollectionA1 = find_entityA1.getDefaultRelationshipCollectionField();
            Assert.assertNotNull(
                                 "Assert " + targetEntityAType.getEntityName() +
                                 "(id=1)'s DefaultRelationshipCollectionField is not null.",
                                 dcCollectionA1);
            Assert.assertEquals(
                                "Assert " + targetEntityAType.getEntityName() +
                                "(id=1)'s DefaultRelationshipCollectionField is 2.",
                                2,
                                dcCollectionA1.size());

            boolean[] entityA1Collection = { false, false };

            for (IEntityB entityB : dcCollectionA1) {
                int targetIndex = entityB.getId() - 1;
                if (targetIndex < 0 || targetIndex >= entityA1Collection.length) {
                    Assert.fail("Found unexpected Entity in " + targetEntityAType.getEntityName()
                                + "(id=1)'s DefaultRelationshipCollectionField: " + entityB);
                } else {
                    entityA1Collection[targetIndex] = true;
                }
            }

            int bIndex1 = 1;
            for (boolean bool : entityA1Collection) {
                Assert.assertTrue(
                                  "Assert " + targetEntityAType.getEntityName() + "(id=1) contains a reference to " +
                                  targetEntityAType.getEntityName() + "(id=" + bIndex1 + ")",
                                  bool);
                bIndex1++;
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("OneXManyUnidirectionalTestLogic.testCardinality001(): End");
        }
    }
}
