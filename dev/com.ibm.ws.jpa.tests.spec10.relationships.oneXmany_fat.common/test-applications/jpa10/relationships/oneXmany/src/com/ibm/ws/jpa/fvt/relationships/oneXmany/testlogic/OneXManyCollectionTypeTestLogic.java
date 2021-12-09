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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.IContainerTypeEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.IContainerTypeEntityB;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class OneXManyCollectionTypeTestLogic extends AbstractTestLogic {
    private int entityBIdList[] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
    private String entityBNameList[] = { "Jarid", "Jared", "Urrvano", "Keri", "Paul", "Mark", "Nathan", "Selorm", "Josh",
                                         "Joe" };

    /**
     * Executes test for OneXMany relationship using a Collection-type.
     *
     * Test Algorithm:
     * 1) Start a new transaction
     * 2) Create a new IContainerTypeEntityA(id=1)
     * 3) Create new instances of IContainerTypeEntityB
     * 4) Add all IContainerTypeEntityB's to the genericizedCollectionType field in IContainerTypeEntityA(id=1)
     * 5) Commit the transaction
     * 6) Clear the persistence context, and fetch genericizedCollectionType collection from the database
     * 7) Verify that each IContainerTypeEntityB is present in the collection
     *
     * Test passes if all steps execute correctly.
     *
     * 8 POINTS
     */
    @SuppressWarnings("unchecked")
    public void testCollectionType(
                                   TestExecutionContext testExecCtx,
                                   TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXManyCollectionTypeTestLogic.testCollectionType(): Missing context and/or resources.  Cannot execute the test.");
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
        OneXManyCollectionTypeEntityEnum targetEntityAType = OneXManyCollectionTypeEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXManyCollectionTypeEntityEnum targetEntityBType = OneXManyCollectionTypeEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        String useGenericCollectionStr = (String) testExecCtx.getProperties().get("UseGenericCollection");
        boolean useGenericCollection = useGenericCollectionStr.equalsIgnoreCase("true") ||
                                       useGenericCollectionStr.equalsIgnoreCase("yes");

        // Execute Test Case
        try {
            System.out.println("OneXManyCollectionTypeTestLogic.testCollectionType(): Begin");

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
            IContainerTypeEntityA new_entityA = (IContainerTypeEntityA) constructNewEntityObject(targetEntityAType);
            new_entityA.setId(1);
            new_entityA.setName("Dr. Peabody");

            System.out.println("Persisting " + new_entityA);
            jpaResource.getEm().persist(new_entityA);

            System.out.println("Creating " + entityBIdList.length + " instances of " + targetEntityBType.getEntityName() + "...");

            for (int index = 0; index < entityBIdList.length; index++) {
                int id = entityBIdList[index];
                String name = entityBNameList[index];

                System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() + " (id=" + id + ")...");
                IContainerTypeEntityB new_entityB = (IContainerTypeEntityB) constructNewEntityObject(targetEntityBType);
                new_entityB.setId(id);
                new_entityB.setName(name);

                System.out.println("Persisting " + new_entityB);
                jpaResource.getEm().persist(new_entityB);

                System.out.println("Establishing relationship between " + targetEntityAType.getEntityName() + "(id=1)" +
                                   " and " + targetEntityBType.getEntityName() + " (id=" + id + "), using " +
                                   ((useGenericCollection == true) ? "Generic Collection" : "Non-Generic Collection") +
                                   "...");
                if (useGenericCollection) {
                    new_entityA.insertGenericizedCollectionTypeField(new_entityB);
                } else {
                    new_entityA.insertUngenericizedCollectionTypeField(new_entityB);
                }
            }

            System.out.println("All entities created, relationship established.  Committing transaction...");
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

            System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=1)...");
            IContainerTypeEntityA find_entityA = (IContainerTypeEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), 1);
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
            Assert.assertEquals("Assert that the entity's name is 'Dr. Peabody'",
                                find_entityA.getName(),
                                "Dr. Peabody");

            System.out.println("Fetching " + ((useGenericCollection == true) ? "Generic" : "Non-Generic") + " Collection...");
            Collection<IContainerTypeEntityB> relationalCollection = (useGenericCollection) ? find_entityA.getGenericizedCollectionTypeCollectionField() : find_entityA
                            .getUngenericizedCollectionTypeCollectionField();
            Assert.assertNotNull("Assert null was not returned when getting the Collection.", relationalCollection);
            Assert.assertEquals("Assert that the collection has " + entityBIdList.length + " elements.",
                                entityBIdList.length,
                                relationalCollection.size());

            // Initalize scoreboard...
            int uniqueEntityBEntriesFound = 0;
            boolean resultsArr[] = new boolean[entityBIdList.length];
            for (int index = 0; index < entityBIdList.length; index++)
                resultsArr[index] = false;
            System.out.println("Verifying that all of the expected entities are in the collection...");

            for (IContainerTypeEntityB entityB : relationalCollection) {
                System.out.println("Found " + entityB);

                // Search the array for the id/name pair
                for (int index = 0; index < entityBIdList.length; index++) {
                    if ((entityB.getId() == entityBIdList[index]) && (entityB.getName().equals(entityBNameList[index]))) {
                        // Made a match
                        System.out.println("Matched with the list of expected entities at index " + index);

                        if (resultsArr[index] == true) {
                            // Somehow, and entity appeared twice in the collection/iterator.
                            System.out.println("THIS ENTITY HAS BEEN AlREADY MARKED AS FOUND.");
                        } else {
                            uniqueEntityBEntriesFound++;
                        }

                        // Mark this enity (data pair) as found.
                        resultsArr[index] = true;
                    }
                }
            }

            // Cycled through the contents of the Collection, check if everything that is expected to be in there was found.
            System.out.println("Unique entity entries found in the Collection: " + uniqueEntityBEntriesFound + " (should be " + entityBIdList.length + ")");
            System.out.println("Entity B entries found: ");
            for (int index = 0; index < entityBIdList.length; index++) {
                if (resultsArr[index] == true) {
                    System.out.println("EntityB id=" + entityBIdList[index] + ", name=" + entityBNameList[index]);
                }
            }
            if (uniqueEntityBEntriesFound != entityBIdList.length) {
                System.out.println("Entity B entries NOT found: ");
                for (int index = 0; index < entityBIdList.length; index++) {
                    if (resultsArr[index] == false) {
                        System.out.println("EntityB id=" + entityBIdList[index] + ", name=" + entityBNameList[index]);
                    }
                }
            }

            System.out.println("Rolling Back transaction...");
            jpaResource.getTj().rollbackTransaction();

            if (!(uniqueEntityBEntriesFound == entityBIdList.length)) {
                // Test Failed
                Assert.fail("Not all test requirements were met.  Please examine the log for details.");
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("OneXManyCollectionTypeTestLogic.testCollectionType(): End");
        }
    }

    /**
     * Executes test for OneXMany relationship using a Set-type.
     *
     * Test Algorithm:
     * 1) Start a new transaction
     * 2) Create a new IContainerTypeEntityA(id=1)
     * 3) Create new instances of IContainerTypeEntityB
     * 4) Add all IContainerTypeEntityB's to the (un)genericizedSetType field in IContainerTypeEntityA(id=1)
     * 5) Commit the transaction
     * 6) Clear the persistence context, and fetch (un)genericizedSetType set from the database
     * 7) Verify that each IContainerTypeEntityB is present in the set
     *
     * Test passes if all steps execute correctly.
     */
    @SuppressWarnings("unchecked")
    public void testSetType(
                            TestExecutionContext testExecCtx,
                            TestExecutionResources testExecResources,
                            Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXManyCollectionTypeTestLogic.testSetType(): Missing context and/or resources.  Cannot execute the test.");
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
        OneXManyCollectionTypeEntityEnum targetEntityAType = OneXManyCollectionTypeEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXManyCollectionTypeEntityEnum targetEntityBType = OneXManyCollectionTypeEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        String useGenericSetStr = (String) testExecCtx.getProperties().get("UseGenericSet");
        boolean useGenericSet = useGenericSetStr.equalsIgnoreCase("true") || useGenericSetStr.equalsIgnoreCase("yes");

        // Execute Test Case
        try {
            System.out.println("OneXManyCollectionTypeTestLogic.testSetType(): Begin");

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
            IContainerTypeEntityA new_entityA = (IContainerTypeEntityA) constructNewEntityObject(targetEntityAType);
            new_entityA.setId(1);
            new_entityA.setName("Dr. Peabody");

            System.out.println("Persisting " + new_entityA);
            jpaResource.getEm().persist(new_entityA);

            System.out.println("Creating " + entityBIdList.length + " instances of " + targetEntityBType.getEntityName() + "...");

            for (int index = 0; index < entityBIdList.length; index++) {
                int id = entityBIdList[index];
                String name = entityBNameList[index];

                System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() + " (id=" + id + ")...");
                IContainerTypeEntityB new_entityB = (IContainerTypeEntityB) constructNewEntityObject(targetEntityBType);
                new_entityB.setId(id);
                new_entityB.setName(name);

                System.out.println("Persisting " + new_entityB);
                jpaResource.getEm().persist(new_entityB);

                System.out.println("Establishing relationship between " + targetEntityAType.getEntityName() + "(id=1)" +
                                   " and " + targetEntityBType.getEntityName() + " (id=" + id + "), using " +
                                   ((useGenericSet == true) ? "Generic Set" : "Non-Generic Set") +
                                   "...");
                if (useGenericSet) {
                    new_entityA.insertGenericizedSetTypeField(new_entityB);
                } else {
                    new_entityA.insertUngenericizedSetTypeField(new_entityB);
                }
            }

            System.out.println("All entities created, relationship established.  Committing transaction...");
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

            System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=1)...");
            IContainerTypeEntityA find_entityA = (IContainerTypeEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), 1);
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
            Assert.assertEquals("Assert that the entity's name is 'Dr. Peabody'",
                                find_entityA.getName(),
                                "Dr. Peabody");

            System.out.println("Fetching " + ((useGenericSet == true) ? "Generic" : "Non-Generic") + " Set...");
            Set<IContainerTypeEntityB> relationalSet = (useGenericSet) ? find_entityA.getGenericizedSetTypeCollectionField() : find_entityA
                            .getUngenericizedSetTypeCollectionField();
            Assert.assertNotNull("Assert null was not returned when getting the Set.", relationalSet);
            Assert.assertEquals("Assert that the set has " + entityBIdList.length + " elements.",
                                entityBIdList.length,
                                relationalSet.size());

            // Initalize scoreboard...
            int uniqueEntityBEntriesFound = 0;
            boolean resultsArr[] = new boolean[entityBIdList.length];
            for (int index = 0; index < entityBIdList.length; index++)
                resultsArr[index] = false;
            System.out.println("Verifying that all of the expected entities are in the Set...");

            for (IContainerTypeEntityB entityB : relationalSet) {
                System.out.println("Found " + entityB);

                // Search the array for the id/name pair
                for (int index = 0; index < entityBIdList.length; index++) {
                    if ((entityB.getId() == entityBIdList[index]) && (entityB.getName().equals(entityBNameList[index]))) {
                        // Made a match
                        System.out.println("Matched with the list of expected entities at index " + index);

                        if (resultsArr[index] == true) {
                            // Somehow, and entity appeared twice in the collection/iterator.
                            System.out.println("THIS ENTITY HAS BEEN AlREADY MARKED AS FOUND.");
                        } else {
                            uniqueEntityBEntriesFound++;
                        }

                        // Mark this enity (data pair) as found.
                        resultsArr[index] = true;
                    }
                }
            }

            // Cycled through the contents of the Collection, check if everything that is expected to be in there was found.
            System.out.println("Unique entity entries found in the Set: " + uniqueEntityBEntriesFound + " (should be " + entityBIdList.length + ")");
            System.out.println("Entity B entries found: ");
            for (int index = 0; index < entityBIdList.length; index++) {
                if (resultsArr[index] == true) {
                    System.out.println("EntityB id=" + entityBIdList[index] + ", name=" + entityBNameList[index]);
                }
            }
            if (uniqueEntityBEntriesFound != entityBIdList.length) {
                System.out.println("Entity B entries NOT found: ");
                for (int index = 0; index < entityBIdList.length; index++) {
                    if (resultsArr[index] == false) {
                        System.out.println("EntityB id=" + entityBIdList[index] + ", name=" + entityBNameList[index]);
                    }
                }
            }

            System.out.println("Rolling Back transaction...");
            jpaResource.getTj().rollbackTransaction();

            if (!(uniqueEntityBEntriesFound == entityBIdList.length)) {
                // Test Failed
                Assert.fail("Not all test requirements were met.  Please examine the log for details.");
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("OneXManyCollectionTypeTestLogic.testSetType(): End");
        }
    }

    /**
     * Executes test for OneXMany relationship using a List-type.
     *
     * Test Algorithm:
     * 1) Start a new transaction
     * 2) Create a new IContainerTypeEntityA(id=1)
     * 3) Create new instances of IContainerTypeEntityB
     * 4) Add all IContainerTypeEntityB's to the (un)genericizedListType field in IContainerTypeEntityA(id=1)
     * 5) Commit the transaction
     * 6) Clear the persistence context, and fetch (un)genericizedListType set from the database
     * 7) Verify that each IContainerTypeEntityB is present in the set
     *
     * Test passes if all steps execute correctly.
     */
    @SuppressWarnings("unchecked")
    public void testListType(
                             TestExecutionContext testExecCtx,
                             TestExecutionResources testExecResources,
                             Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXManyCollectionTypeTestLogic.testListType(): Missing context and/or resources.  Cannot execute the test.");
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
        OneXManyCollectionTypeEntityEnum targetEntityAType = OneXManyCollectionTypeEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXManyCollectionTypeEntityEnum targetEntityBType = OneXManyCollectionTypeEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        String useGenericListStr = (String) testExecCtx.getProperties().get("UseGenericList");
        boolean useGenericList = useGenericListStr.equalsIgnoreCase("true") || useGenericListStr.equalsIgnoreCase("yes");

        // Execute Test Case
        try {
            System.out.println("OneXManyCollectionTypeTestLogic.testListType(): Begin");

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
            IContainerTypeEntityA new_entityA = (IContainerTypeEntityA) constructNewEntityObject(targetEntityAType);
            new_entityA.setId(1);
            new_entityA.setName("Dr. Peabody");

            System.out.println("Persisting " + new_entityA);
            jpaResource.getEm().persist(new_entityA);

            System.out.println("Creating " + entityBIdList.length + " instances of " + targetEntityBType.getEntityName() + "...");

            for (int index = 0; index < entityBIdList.length; index++) {
                int id = entityBIdList[index];
                String name = entityBNameList[index];

                System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() + " (id=" + id + ")...");
                IContainerTypeEntityB new_entityB = (IContainerTypeEntityB) constructNewEntityObject(targetEntityBType);
                new_entityB.setId(id);
                new_entityB.setName(name);

                System.out.println("Persisting " + new_entityB);
                jpaResource.getEm().persist(new_entityB);

                System.out.println("Establishing relationship between " + targetEntityAType.getEntityName() + "(id=1)" +
                                   " and " + targetEntityBType.getEntityName() + " (id=" + id + "), using " +
                                   ((useGenericList == true) ? "Generic List" : "Non-Generic List") +
                                   "...");
                if (useGenericList) {
                    new_entityA.insertGenericizedListTypeField(new_entityB);
                } else {
                    new_entityA.insertUngenericizedListTypeField(new_entityB);
                }
            }

            System.out.println("All entities created, relationship established.  Committing transaction...");
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

            System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=1)...");
            IContainerTypeEntityA find_entityA = (IContainerTypeEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), 1);
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
            Assert.assertEquals("Assert that the entity's name is 'Dr. Peabody'",
                                find_entityA.getName(),
                                "Dr. Peabody");

            System.out.println("Fetching " + ((useGenericList == true) ? "Generic" : "Non-Generic") + " List...");
            List<IContainerTypeEntityB> relationalList = (useGenericList) ? find_entityA.getGenericizedListTypeCollectionField() : find_entityA
                            .getUngenericizedListTypeCollectionField();
            Assert.assertNotNull("Assert null was not returned when getting the List.", relationalList);
            Assert.assertEquals("Assert that the List has " + entityBIdList.length + " elements.",
                                entityBIdList.length,
                                relationalList.size());

            // Initalize scoreboard...
            int uniqueEntityBEntriesFound = 0;
            boolean resultsArr[] = new boolean[entityBIdList.length];
            for (int index = 0; index < entityBIdList.length; index++)
                resultsArr[index] = false;
            System.out.println("Verifying that all of the expected entities are in the List...");

            for (IContainerTypeEntityB entityB : relationalList) {
                System.out.println("Found " + entityB);

                // Search the array for the id/name pair
                for (int index = 0; index < entityBIdList.length; index++) {
                    if ((entityB.getId() == entityBIdList[index]) && (entityB.getName().equals(entityBNameList[index]))) {
                        // Made a match
                        System.out.println("Matched with the list of expected entities at index " + index);

                        if (resultsArr[index] == true) {
                            // Somehow, and entity appeared twice in the collection/iterator.
                            System.out.println("THIS ENTITY HAS BEEN AlREADY MARKED AS FOUND.");
                        } else {
                            uniqueEntityBEntriesFound++;
                        }

                        // Mark this enity (data pair) as found.
                        resultsArr[index] = true;
                    }
                }
            }

            // Cycled through the contents of the Collection, check if everything that is expected to be in there was found.
            System.out.println("Unique entity entries found in the List: " + uniqueEntityBEntriesFound + " (should be " + entityBIdList.length + ")");
            System.out.println("Entity B entries found: ");
            for (int index = 0; index < entityBIdList.length; index++) {
                if (resultsArr[index] == true) {
                    System.out.println("EntityB id=" + entityBIdList[index] + ", name=" + entityBNameList[index]);
                }
            }
            if (uniqueEntityBEntriesFound != entityBIdList.length) {
                System.out.println("Entity B entries NOT found: ");
                for (int index = 0; index < entityBIdList.length; index++) {
                    if (resultsArr[index] == false) {
                        System.out.println("EntityB id=" + entityBIdList[index] + ", name=" + entityBNameList[index]);
                    }
                }
            }

            System.out.println("Rolling Back transaction...");
            jpaResource.getTj().rollbackTransaction();

            if (!(uniqueEntityBEntriesFound == entityBIdList.length)) {
                // Test Failed
                Assert.fail("Not all test requirements were met.  Please examine the log for details.");
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("OneXManyCollectionTypeTestLogic.testListType(): End");
        }
    }

    /**
     * Executes test for OneXMany relationship using a List-type with name-field ordering.
     *
     * Test Algorithm:
     * 1) Start a new transaction
     * 2) Create a new IContainerTypeEntityA(id=1)
     * 3) Create several new instance of IContainerTypeEntityB(id=1 through 100). For each, generate
     * a string of random length with random characters.
     * 4) Add each IContainerTypeEntityB to the orderedListType field in IContainerTypeEntityA(id=1)
     * 5) Commit the transaction
     * 6) Clear the persistence context, and fetch IContainerTypeEntityA(id=1) from the database
     * 7) Fetch the instance of List from IContainerTypeEntityA(id=1).genericizedListType
     * 8) Iterate through the List obtained in step 7. All of the entries should be sorted by
     * IContainerTypeEntityB.name in ascending order.
     *
     * Test passes if all steps execute correctly.
     */
    @SuppressWarnings("unchecked")
    public void testOrderedListType(
                                    TestExecutionContext testExecCtx,
                                    TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXManyCollectionTypeTestLogic.testOrderedListType(): Missing context and/or resources.  Cannot execute the test.");
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
        OneXManyCollectionTypeEntityEnum targetEntityAType = OneXManyCollectionTypeEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXManyCollectionTypeEntityEnum targetEntityBType = OneXManyCollectionTypeEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXManyCollectionTypeTestLogic.testOrderedListType(): Begin");

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
            IContainerTypeEntityA new_entityA = (IContainerTypeEntityA) constructNewEntityObject(targetEntityAType);
            new_entityA.setId(1);
            new_entityA.setName("Dr. Peabody");

            System.out.println("Persisting " + new_entityA);
            jpaResource.getEm().persist(new_entityA);

            System.out.println("Creating " + entityBIdList.length + " instances of " + targetEntityBType.getEntityName() + "...");

            for (int index = 0; index < entityBIdList.length; index++) {
                int id = entityBIdList[index];
                String name = entityBNameList[index];

                System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() + " (id=" + id + ")...");
                IContainerTypeEntityB new_entityB = (IContainerTypeEntityB) constructNewEntityObject(targetEntityBType);
                new_entityB.setId(id);
                new_entityB.setName(name);

                System.out.println("Persisting " + new_entityB);
                jpaResource.getEm().persist(new_entityB);

                System.out.println("Establishing relationship between " + targetEntityAType.getEntityName() + "(id=1)" +
                                   " and " + targetEntityBType.getEntityName() + " (id=" + id + "), using Ordered List");
                new_entityA.insertOrderedListTypeField(new_entityB);
            }

            System.out.println("All entities created, relationship established.  Committing transaction...");
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

            System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=1)...");
            IContainerTypeEntityA find_entityA = (IContainerTypeEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), 1);
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
            Assert.assertEquals("Assert that the entity's name is 'Dr. Peabody'",
                                find_entityA.getName(),
                                "Dr. Peabody");

            System.out.println("Fetching Ordered List...");
            List<IContainerTypeEntityB> relationalList = find_entityA.getOrderedListTypeCollectionField();
            Assert.assertNotNull("Assert null was not returned when getting the List.", relationalList);
            Assert.assertEquals("Assert that the List has " + entityBIdList.length + " elements.",
                                entityBIdList.length,
                                relationalList.size());

            // Initalize scoreboard...
            int uniqueEntityBEntriesFound = 0;
            boolean resultsArr[] = new boolean[entityBIdList.length];
            for (int index = 0; index < entityBIdList.length; index++)
                resultsArr[index] = false;
            System.out.println("Verifying that all of the expected entities are in the List...");

            String previousName = null;
            for (IContainerTypeEntityB entityB : relationalList) {
                System.out.println("Found " + entityB);

                if (previousName == null) {
                    // This is the first entry we're looking at in the list.
                    previousName = entityB.getName();
                } else {
                    /// This is not the first entry in the list, so make sure this name is lexically greater then the last.
                    int resultCode = entityB.getName().compareTo(previousName);

                    if (resultCode == 0) {
                        // Names are exactly the same, this is okay.
                        System.out.println("Current IContainerTypeEntityB's name field matches the previous IContainerTypeEntityB.");
                    } else if (resultCode > 0) {
                        // The current name is lexically greater then the previous name, which is what we're looking for
                        System.out.println("Current IContainerTypeEntityB's name field is greater then the previous IContainerTypeEntityB.");
                        previousName = entityB.getName();
                    } else {
                        // The current name is lexically inferrior to the previous name, this is a problem.
                        Assert.fail("Current IContainerTypeEntityB's name field is INFERRIOR then the previous IContainerTypeEntityB.");
                    }
                }

                // Search the array for the id/name pair
                for (int index = 0; index < entityBIdList.length; index++) {
                    if ((entityB.getId() == entityBIdList[index]) && (entityB.getName().equals(entityBNameList[index]))) {
                        // Made a match
                        System.out.println("Matched with the list of expected entities at index " + index);

                        if (resultsArr[index] == true) {
                            // Somehow, and entity appeared twice in the collection/iterator.
                            System.out.println("THIS ENTITY HAS BEEN AlREADY MARKED AS FOUND.");
                        } else {
                            uniqueEntityBEntriesFound++;
                        }

                        // Mark this enity (data pair) as found.
                        resultsArr[index] = true;
                    }
                }
            }

            // Cycled through the contents of the Collection, check if everything that is expected to be in there was found.
            System.out.println("Unique entity entries found in the List: " + uniqueEntityBEntriesFound + " (should be " + entityBIdList.length + ")");
            System.out.println("Entity B entries found: ");
            for (int index = 0; index < entityBIdList.length; index++) {
                if (resultsArr[index] == true) {
                    System.out.println("EntityB id=" + entityBIdList[index] + ", name=" + entityBNameList[index]);
                }
            }
            if (uniqueEntityBEntriesFound != entityBIdList.length) {
                System.out.println("Entity B entries NOT found: ");
                for (int index = 0; index < entityBIdList.length; index++) {
                    if (resultsArr[index] == false) {
                        System.out.println("EntityB id=" + entityBIdList[index] + ", name=" + entityBNameList[index]);
                    }
                }
            }

            System.out.println("Rolling Back transaction...");
            jpaResource.getTj().rollbackTransaction();

            if (!(uniqueEntityBEntriesFound == entityBIdList.length)) {
                // Test Failed
                Assert.fail("Not all test requirements were met.  Please examine the log for details.");
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("OneXManyCollectionTypeTestLogic.testOrderedListType(): End");
        }
    }

    /**
     * Executes test for OneXMany relationship using a Map-type.
     *
     * Test Algorithm:
     * 1) Start a new transaction
     * 2) Create a new IContainerTypeEntityA(id=1)
     * 3) Create new instances of IContainerTypeEntityB
     * 4) Add all IContainerTypeEntityB's to the (un)genericizedMapType field in IContainerTypeEntityA(id=1)
     * 5) Commit the transaction
     * 6) Clear the persistence context, and fetch (un)genericizedMapType set from the database
     * 7) Verify that each IContainerTypeEntityB is present in the set
     *
     * Test passes if all steps execute correctly.
     */
    @SuppressWarnings({ "rawtypes" })
    public void testMapType(
                            TestExecutionContext testExecCtx,
                            TestExecutionResources testExecResources,
                            Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXManyCollectionTypeTestLogic.testMapType(): Missing context and/or resources.  Cannot execute the test.");
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
        OneXManyCollectionTypeEntityEnum targetEntityAType = OneXManyCollectionTypeEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXManyCollectionTypeEntityEnum targetEntityBType = OneXManyCollectionTypeEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        String useGenericMapStr = (String) testExecCtx.getProperties().get("UseGenericMap");
        boolean useGenericMap = useGenericMapStr.equalsIgnoreCase("true") || useGenericMapStr.equalsIgnoreCase("yes");

        // Execute Test Case
        try {
            System.out.println("OneXManyCollectionTypeTestLogic.testMapType(): Begin");

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
            IContainerTypeEntityA new_entityA = (IContainerTypeEntityA) constructNewEntityObject(targetEntityAType);
            new_entityA.setId(1);
            new_entityA.setName("Dr. Peabody");

            System.out.println("Persisting " + new_entityA);
            jpaResource.getEm().persist(new_entityA);

            System.out.println("Creating " + entityBIdList.length + " instances of " + targetEntityBType.getEntityName() + "...");

            for (int index = 0; index < entityBIdList.length; index++) {
                int id = entityBIdList[index];
                String name = entityBNameList[index];

                System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() + " (id=" + id + ")...");
                IContainerTypeEntityB new_entityB = (IContainerTypeEntityB) constructNewEntityObject(targetEntityBType);
                new_entityB.setId(id);
                new_entityB.setName(name);

                System.out.println("Persisting " + new_entityB);
                jpaResource.getEm().persist(new_entityB);

                System.out.println("Establishing relationship between " + targetEntityAType.getEntityName() + "(id=1)" +
                                   " and " + targetEntityBType.getEntityName() + " (id=" + id + "), using " +
                                   ((useGenericMap == true) ? "Generic Map" : "Non-Generic Map") +
                                   "...");
                if (useGenericMap) {
                    new_entityA.insertGenericizedMapTypeField(new_entityB);
                } else {
//                    new_entityA.insertUngenericizedMapTypeField(new_entityB);
                }
            }

            System.out.println("All entities created, relationship established.  Committing transaction...");
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

            System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=1)...");
            IContainerTypeEntityA find_entityA = (IContainerTypeEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), 1);
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
            Assert.assertEquals("Assert that the entity's name is 'Dr. Peabody'",
                                find_entityA.getName(),
                                "Dr. Peabody");

            System.out.println("Fetching " + ((useGenericMap == true) ? "Generic" : "Non-Generic") + " Map...");
            Map relationalMap = (useGenericMap) ? find_entityA.getGenericizedMapTypeCollectionField() : null;
//                            find_entityA.getUngenericizedMapTypeCollectionField();
            Assert.assertNotNull("Assert null was not returned when getting the Map.", relationalMap);
            Assert.assertEquals("Assert that the Map has " + entityBIdList.length + " elements.",
                                entityBIdList.length,
                                relationalMap.size());

            // Initalize scoreboard...
            int uniqueEntityBEntriesFound = 0;
            boolean resultsArr[] = new boolean[entityBIdList.length];
            for (int index = 0; index < entityBIdList.length; index++)
                resultsArr[index] = false;
            System.out.println("Verifying that all of the expected entities are in the Map...");

            Set keySet = relationalMap.keySet();
            for (Object key : keySet) {
                IContainerTypeEntityB entityB = (IContainerTypeEntityB) relationalMap.get(key);
                System.out.println("Found " + entityB);

                // Search the array for the id/name pair
                for (int index = 0; index < entityBIdList.length; index++) {
                    if ((entityB.getId() == entityBIdList[index]) && (entityB.getName().equals(entityBNameList[index]))) {
                        // Made a match
                        System.out.println("Matched with the list of expected entities at index " + index);

                        if (resultsArr[index] == true) {
                            // Somehow, and entity appeared twice in the collection/iterator.
                            System.out.println("THIS ENTITY HAS BEEN AlREADY MARKED AS FOUND.");
                        } else {
                            uniqueEntityBEntriesFound++;
                        }

                        // Mark this enity (data pair) as found.
                        resultsArr[index] = true;
                    }
                }
            }

            // Cycled through the contents of the Collection, check if everything that is expected to be in there was found.
            System.out.println("Unique entity entries found in the Map: " + uniqueEntityBEntriesFound + " (should be " + entityBIdList.length + ")");
            System.out.println("Entity B entries found: ");
            for (int index = 0; index < entityBIdList.length; index++) {
                if (resultsArr[index] == true) {
                    System.out.println("EntityB id=" + entityBIdList[index] + ", name=" + entityBNameList[index]);
                }
            }
            if (uniqueEntityBEntriesFound != entityBIdList.length) {
                System.out.println("Entity B entries NOT found: ");
                for (int index = 0; index < entityBIdList.length; index++) {
                    if (resultsArr[index] == false) {
                        System.out.println("EntityB id=" + entityBIdList[index] + ", name=" + entityBNameList[index]);
                    }
                }
            }

            System.out.println("Rolling Back transaction...");
            jpaResource.getTj().rollbackTransaction();

            if (!(uniqueEntityBEntriesFound == entityBIdList.length)) {
                // Test Failed
                Assert.fail("Not all test requirements were met.  Please examine the log for details.");
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("OneXManyCollectionTypeTestLogic.testMapType(): End");
        }
    }

    /**
     * Executes test for OneXMany relationship using a Map-type that has the key-name specified.
     *
     * Test Algorithm:
     * 1) Start a new transaction
     * 2) Create a new IContainerTypeEntityA(id=1)
     * 3) Create new instances of IContainerTypeEntityB
     * 4) Add all IContainerTypeEntityB's to the (un)genericizedMapWithKeyType field in IContainerTypeEntityA(id=1)
     * 5) Commit the transaction
     * 6) Clear the persistence context, and fetch (un)genericizedMapWithKeyType set from the database
     * 7) Verify that each IContainerTypeEntityB is present in the set
     *
     * Test passes if all steps execute correctly.
     */
    @SuppressWarnings({ "rawtypes" })
    public void testMapWithKeyType(
                                   TestExecutionContext testExecCtx,
                                   TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXManyCollectionTypeTestLogic.testMapWithKeyType(): Missing context and/or resources.  Cannot execute the test.");
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
        OneXManyCollectionTypeEntityEnum targetEntityAType = OneXManyCollectionTypeEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXManyCollectionTypeEntityEnum targetEntityBType = OneXManyCollectionTypeEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        String useGenericMapStr = (String) testExecCtx.getProperties().get("UseGenericMap");
        boolean useGenericMap = useGenericMapStr.equalsIgnoreCase("true") || useGenericMapStr.equalsIgnoreCase("yes");

        // Execute Test Case
        try {
            System.out.println("OneXManyCollectionTypeTestLogic.testMapWithKeyType(): Begin");

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
            IContainerTypeEntityA new_entityA = (IContainerTypeEntityA) constructNewEntityObject(targetEntityAType);
            new_entityA.setId(1);
            new_entityA.setName("Dr. Peabody");

            System.out.println("Persisting " + new_entityA);
            jpaResource.getEm().persist(new_entityA);

            System.out.println("Creating " + entityBIdList.length + " instances of " + targetEntityBType.getEntityName() + "...");

            for (int index = 0; index < entityBIdList.length; index++) {
                int id = entityBIdList[index];
                String name = entityBNameList[index];

                System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() + " (id=" + id + ")...");
                IContainerTypeEntityB new_entityB = (IContainerTypeEntityB) constructNewEntityObject(targetEntityBType);
                new_entityB.setId(id);
                new_entityB.setName(name);

                System.out.println("Persisting " + new_entityB);
                jpaResource.getEm().persist(new_entityB);

                System.out.println("Establishing relationship between " + targetEntityAType.getEntityName() + "(id=1)" +
                                   " and " + targetEntityBType.getEntityName() + " (id=" + id + "), using " +
                                   ((useGenericMap == true) ? "Generic Map" : "Non-Generic Map") +
                                   "...");
                if (useGenericMap) {
                    new_entityA.insertGenericizedMapWithKeyTypeField(new_entityB);
                } else {
                    new_entityA.insertUngenericizedMapWithKeyTypeField(new_entityB);
                }
            }

            System.out.println("All entities created, relationship established.  Committing transaction...");
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

            System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=1)...");
            IContainerTypeEntityA find_entityA = (IContainerTypeEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), 1);
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
            Assert.assertEquals("Assert that the entity's name is 'Dr. Peabody'",
                                find_entityA.getName(),
                                "Dr. Peabody");

            System.out.println("Fetching " + ((useGenericMap == true) ? "Generic" : "Non-Generic") + " Map...");
            Map relationalMap = (useGenericMap) ? find_entityA.getGenericizedMapWithKeyTypeCollectionField() : find_entityA.getUngenericizedMapWithKeyTypeCollectionField();
            Assert.assertNotNull("Assert null was not returned when getting the Map.", relationalMap);
            Assert.assertEquals("Assert that the Map has " + entityBIdList.length + " elements.",
                                entityBIdList.length,
                                relationalMap.size());

            // Initalize scoreboard...
            int uniqueEntityBEntriesFound = 0;
            boolean resultsArr[] = new boolean[entityBIdList.length];
            for (int index = 0; index < entityBIdList.length; index++)
                resultsArr[index] = false;
            System.out.println("Verifying that all of the expected entities are in the Map...");

            Set keySet = relationalMap.keySet();
            for (Object key : keySet) {
                IContainerTypeEntityB entityB = (IContainerTypeEntityB) relationalMap.get(key);
                System.out.println("Found " + entityB);

                // Search the array for the id/name pair
                for (int index = 0; index < entityBIdList.length; index++) {
                    if ((entityB.getId() == entityBIdList[index]) && (entityB.getName().equals(entityBNameList[index]))) {
                        // Made a match
                        System.out.println("Matched with the list of expected entities at index " + index);

                        if (resultsArr[index] == true) {
                            // Somehow, and entity appeared twice in the collection/iterator.
                            System.out.println("THIS ENTITY HAS BEEN AlREADY MARKED AS FOUND.");
                        } else {
                            uniqueEntityBEntriesFound++;
                        }

                        // Mark this enity (data pair) as found.
                        resultsArr[index] = true;
                    }
                }
            }

            // Cycled through the contents of the Collection, check if everything that is expected to be in there was found.
            System.out.println("Unique entity entries found in the Map: " + uniqueEntityBEntriesFound + " (should be " + entityBIdList.length + ")");
            System.out.println("Entity B entries found: ");
            for (int index = 0; index < entityBIdList.length; index++) {
                if (resultsArr[index] == true) {
                    System.out.println("EntityB id=" + entityBIdList[index] + ", name=" + entityBNameList[index]);
                }
            }
            if (uniqueEntityBEntriesFound != entityBIdList.length) {
                System.out.println("Entity B entries NOT found: ");
                for (int index = 0; index < entityBIdList.length; index++) {
                    if (resultsArr[index] == false) {
                        System.out.println("EntityB id=" + entityBIdList[index] + ", name=" + entityBNameList[index]);
                    }
                }
            }

            System.out.println("Rolling Back transaction...");
            jpaResource.getTj().rollbackTransaction();

            if (!(uniqueEntityBEntriesFound == entityBIdList.length)) {
                // Test Failed
                Assert.fail("Not all test requirements were met.  Please examine the log for details.");
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("OneXManyCollectionTypeTestLogic.testMapWithKeyType(): End");
        }
    }
}
