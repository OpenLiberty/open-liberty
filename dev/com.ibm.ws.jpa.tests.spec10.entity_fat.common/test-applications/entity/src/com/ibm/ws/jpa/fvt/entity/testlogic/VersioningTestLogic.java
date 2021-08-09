/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.entity.testlogic;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.persistence.OptimisticLockException;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.entity.entities.IVersionedEntity;
import com.ibm.ws.jpa.fvt.entity.testlogic.enums.EntityVersionEntityEnum;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class VersioningTestLogic extends AbstractTestLogic {
    private final int entity_pkey = 1;

    private final int initial_intVal = 42;
    private final String initial_strVal = "Latveria";

    private final int user1change_intVal = 84;
    private final String user1change_strVal = "Elbonia";

    private final int user2change_intVal = 64;
    private final String user2change_strVal = "Dagobah";

    /**
     * Basic Version Field Test
     *
     * 10 POINTS
     */
    public void testVersioning001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("VersioningTestLogic.testVersioning001(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        JPAResource jpaResource2 = testExecResources.getJpaResourceMap().get("test-jpa-resource2");
        if (jpaResource2 == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource2').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityName = (String) testExecCtx.getProperties().get("EntityName");
        EntityVersionEntityEnum targetEntityType = EntityVersionEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("VersioningTestLogic.testVersioning001(): Begin");
            //cleanupDatabase(jpaCleanupResource);

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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=1)...");
            IVersionedEntity new_entity = (IVersionedEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(entity_pkey);
            new_entity.setIntVal(initial_intVal);
            new_entity.setStringVal(initial_strVal);

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            System.out.println("Post-Commit Entity State: " + new_entity);

            System.out.println("**************************************************");

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Fetch Entity with JPA Resource #1
            System.out.println("Beginning new transaction with JPA Resource #1...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=1) with JPA Resource #1...");
            IVersionedEntity find_entity1 = (IVersionedEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), 1);
            System.out.println("Object returned by find: " + find_entity1);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity1);
            Assert.assertNotSame(
                                 "Assert find did not return the original object",
                                 new_entity,
                                 find_entity1);
            Assert.assertTrue(
                              "Assert entity returned by find is managed by the persistence context.",
                              jpaResource.getEm().contains(find_entity1));
            Assert.assertEquals(
                                "Assert that the entity's id is 1",
                                find_entity1.getId(),
                                1);

            // Fetch Entity with JPA Resource #2
            System.out.println("Beginning new transaction with JPA Resource #2...");
            jpaResource2.getTj().beginTransaction();
            if (jpaResource2.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource2.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=1) with JPA Resource #2...");
            IVersionedEntity find_entity2 = (IVersionedEntity) jpaResource2.getEm().find(resolveEntityClass(targetEntityType), 1);
            System.out.println("Object returned by find: " + find_entity2);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity2);
            Assert.assertNotSame(
                                 "Assert find did not return the original object",
                                 new_entity,
                                 find_entity2);
            Assert.assertNotSame(
                                 "Assert find did not return the same object as JPA Resource #1's find operation.",
                                 find_entity1,
                                 find_entity2);
            Assert.assertTrue(
                              "Assert entity returned by find is managed by the persistence context.",
                              jpaResource2.getEm().contains(find_entity2));
            Assert.assertEquals(
                                "Assert that the entity's id is 1",
                                find_entity2.getId(),
                                1);

            System.out.println(
                               "Acquired references to IVersionedEntity(id=1) using two separate EntityManagers. " +
                               "This means that we have two copies of the same entity originating from two separate persistence contexts " +
                               "Now we will modify each of the entities' persistable fields' contents, and merge them back into " +
                               "the database.  The merge for each will be performed sequentially, and each merge operation will " +
                               "be contained in a transaction which will be committed immediately after the merge operation. " +
                               "The first merge operation should complete successfully.  The second merge operation should fail " +
                               "because the version field will have changed due to the first merge operation.");

            System.out.println("Altering persistable fields for IVersionedEntity(id=1) with JPA Service #1");
            find_entity1.setIntVal(user1change_intVal);
            find_entity1.setStringVal(user1change_strVal);
            System.out.println("Version id for IVersionedEntity(id=1) with JPA Service #1:" + find_entity1.getVersionObj());

            // EclipseLink only obtains Oracle timestamp to 1 second precision.
            // We need to sleep for greater than that amount before commit in order to ensure it writes a unique version based on timestamp.
            if (jpaResource.getEm().getProperties().containsKey("eclipselink.target-server")
                && find_entity1.getVersionObj() instanceof java.util.Date) {
                // Need to sleep only if the version field is a timestamp type.
                long timeBeforeSleep = System.currentTimeMillis();
                try {
                    long versionSleepTimer = 2000; // milliseconds
                    String versionSleepTimeOverride = AccessController.doPrivileged(new PrivilegedAction<String>() {
                        @Override
                        public String run() {
                            return System.getProperty("VersionSleepTimer", "");
                        }
                    }).trim();
                    if (!versionSleepTimeOverride.equals("")) {
                        // Version sleep timer was overridden
                        try {
                            Long newTimeL = new Long(versionSleepTimeOverride);
                            versionSleepTimer = newTimeL.longValue();
                        } catch (NumberFormatException nfe) {
                            System.out.println("Found an invalid VersionSleepTimer property: \"" + versionSleepTimeOverride + "\"");
                        }
                    }
                    System.out.println("Going to sleep for " + versionSleepTimer + " ms...");
                    System.out.println("Current Time Before Sleep: " + new java.util.Date());

                    Thread.sleep(versionSleepTimer);
                } catch (InterruptedException e) {
                    System.out.println("Something threw a thread interrupt at this thread.");
                }

                long timeAfterSleep = System.currentTimeMillis();
                System.out.println("Current Time After Sleep: " + new java.util.Date());
                System.out.println("Time spent sleeping: " + (timeAfterSleep - timeBeforeSleep) + " ms.");

                System.out.println("**************************************************");
            }

            System.out.println("JPA Resource #1 Committing transaction...");
            System.out.println("Current Time Before Commit: " + new java.util.Date());
            jpaResource.getTj().commitTransaction();
            System.out.println("Current Time After Commit: " + new java.util.Date());

            System.out.println("EM#1 Entity State after EM-TX1 Commit: " + find_entity1);
            System.out.println("EM#2 Entity State after EM-TX1 Commit: " + find_entity2);

            {
                // What's stored on the database may not be exactly the same as the Date object, due to precision.
                // Dump what was stored on the database.

                System.out.println("Log what timestamp value was saved to database from JPA Resource #1's actions: ");

                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Fetch Entity with JPA Resource #1
                System.out.println("Beginning new transaction with JPA Resource #1...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }
                System.out.println("Finding " + targetEntityType.getEntityName() + " (id=1) with JPA Resource #1...");
                IVersionedEntity find_entity1_postcommit = (IVersionedEntity) jpaResource.getEm()
                                .find(resolveEntityClass(targetEntityType), 1);
                System.out.println("Object returned by find: " + find_entity1_postcommit);

                System.out.println("Rolling back Transaction...");
                jpaResource.getTj().rollbackTransaction();
            }

            System.out.println("**************************************************");

            System.out.println("Altering persistable fields for IVersionedEntity(id=1) with JPA Service #2");
            find_entity2.setIntVal(user2change_intVal);
            find_entity2.setStringVal(user2change_strVal);
            System.out.println("Version id for IVersionedEntity(id=1) with JPA Service #2:" + find_entity2.getVersionObj());

            try {
                System.out.println("Attempting to commit transaction with JPA Resource #2, an OptimisticLockException should be thrown.");
                System.out.println("EM#1 Entity Version Field State before EM-TX2 Commit: " + find_entity1.getVersionObj());
                System.out.println("EM#2 Entity Version Field State before EM-TX2 Commit: " + find_entity2.getVersionObj());

                System.out.println("Current Time Before Commit: " + new java.util.Date());
                jpaResource2.getTj().commitTransaction();
                System.out.println("Current Time After Commit: " + new java.util.Date());
                Assert.fail("Transaction Commit completed without an Exception being thrown.");
            } catch (Throwable t) {
                // Caught an Exception, check if IllegalStateException is in the Exception Chain
                System.out.println("Current Time After Commit: " + new java.util.Date());

                System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for OptimisticLockException...");
                assertExceptionIsInChain(OptimisticLockException.class, t);
            } finally {
                System.out.println("EM#1 Entity State after EM-TX2 Commit: " + find_entity1);
                System.out.println("EM#2 Entity State after EM-TX2 Commit: " + find_entity2);

                if (jpaResource.getTj().isTransactionActive()) {
                    System.out.println("Rolling back the transaction...");
                    jpaResource.getTj().rollbackTransaction();
                }
            }

            System.out.println("Ending test.");
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("VersioningTestLogic.testVersioning001(): End");
        }
    }

    public void testTemplate(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                             Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("VersioningTestLogic.testTemplate(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
        if (jpaCleanupResource == null) {
            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
            return;
        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityAName = (String) testExecCtx.getProperties().get("EntityAName");
        EntityVersionEntityEnum targetEntityAType = EntityVersionEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("VersioningTestLogic.testTemplate(): Begin");
            //cleanupDatabase(jpaCleanupResource);

            System.out.println("Ending test.");
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("VersioningTestLogic.testTemplate(): End");
        }
    }

    protected void cleanupDatabase(JPAResource jpaResource) {
        // Cleanup the database for executing the test
        System.out.println("Cleaning up database before executing test...");
        cleanupDatabase(jpaResource.getEm(), jpaResource.getTj(), EntityVersionEntityEnum.values());
        System.out.println("Database cleanup complete.\n");
    }
}
