/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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

package com.ibm.ws.jpa.fvt.entity.testlogic;

import java.io.Serializable;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import javax.persistence.OptimisticLockException;
import javax.persistence.Query;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.entity.entities.IVersionedEntity;
import com.ibm.ws.jpa.fvt.entity.testlogic.enums.EntityVersionEntityEnum;
import com.ibm.ws.testtooling.database.DatabaseVendor;
import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class VersioningTestLogic extends AbstractTestLogic {

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

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Fetch target entity type from test parameters
        String entityName = (String) testProps.get("EntityName");
        EntityVersionEntityEnum targetEntityType = EntityVersionEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        JPAPersistenceProvider provider = JPAPersistenceProvider.resolveJPAPersistenceProvider(jpaResource);

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final boolean isDB2ZOS = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);

        /*
         * TODO: Disabling these tests until the issue is is delivered:
         * https://github.com/OpenLiberty/open-liberty/issues/23949
         * https://github.com/OpenLiberty/open-liberty/issues/23952
         */
        if (isDB2ZOS && JPAPersistenceProvider.ECLIPSELINK.equals(provider)
            && (isUsingJPA21Feature() || isUsingJPA31Feature())) {
            System.out.println("Skipping test; platform (" + dbProductName + ", " + dbProductVersion + ")");
            return;
        }

        if (JPAPersistenceProvider.HIBERNATE.equals(provider)) {
            // TODO: Hibernate fails with "org.hibernate.exception.LockAcquisitionException: could not execute statement".
            System.out.println("Skipping test; platform (" + dbProductName + ", " + dbProductVersion + ")");
            return;
        }

        final int entity_pkey = 1;

        final int initial_intVal = 42;
        final String initial_strVal = "Latveria";

        final int user1change_intVal = 84;
        final String user1change_strVal = "Elbonia";

        final int user2change_intVal = 64;
        final String user2change_strVal = "Dagobah";

        // Execute Test Case
        try {
            System.out.println("VersioningTestLogic.testVersioning001(): Begin");

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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=" + entity_pkey + ")...");
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

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + entity_pkey + ") with JPA Resource #1...");
            IVersionedEntity find_entity1 = (IVersionedEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), entity_pkey);
            System.out.println("Object returned by find: " + find_entity1);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity1);
            Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity1);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity1));
            Assert.assertEquals("Assert that the entity's id is " + entity_pkey, find_entity1.getId(), entity_pkey);

            // Fetch Entity with JPA Resource #2
            System.out.println("Beginning new transaction with JPA Resource #2...");
            jpaResource2.getTj().beginTransaction();
            if (jpaResource2.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource2.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + entity_pkey + ") with JPA Resource #2...");
            IVersionedEntity find_entity2 = (IVersionedEntity) jpaResource2.getEm().find(resolveEntityClass(targetEntityType), entity_pkey);
            System.out.println("Object returned by find: " + find_entity2);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity2);
            Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity2);
            Assert.assertNotSame("Assert find did not return the same object as JPA Resource #1's find operation.", find_entity1, find_entity2);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource2.getEm().contains(find_entity2));
            Assert.assertEquals("Assert that the entity's id is " + entity_pkey, find_entity2.getId(), entity_pkey);

            System.out.println("Acquired references to IVersionedEntity(id=" + entity_pkey + ") using two separate EntityManagers. " +
                               "This means that we have two copies of the same entity originating from two separate persistence contexts " +
                               "Now we will modify each of the entities' persistable fields' contents, and merge them back into " +
                               "the database.  The merge for each will be performed sequentially, and each merge operation will " +
                               "be contained in a transaction which will be committed immediately after the merge operation. " +
                               "The first merge operation should complete successfully.  The second merge operation should fail " +
                               "because the version field will have changed due to the first merge operation.");

            System.out.println("Altering persistable fields for IVersionedEntity(id=" + entity_pkey + ") with JPA Service #1");
            find_entity1.setIntVal(user1change_intVal);
            find_entity1.setStringVal(user1change_strVal);
            System.out.println("Version id for IVersionedEntity(id=" + entity_pkey + ") with JPA Service #1:" + find_entity1.getVersionObj());

            // EclipseLink only obtains Oracle timestamp to 1 second precision.
            // We need to sleep for greater than that amount before commit in order to ensure it writes a unique version based on timestamp.
            if (true) {
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
                System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + entity_pkey + ") with JPA Resource #1...");
                IVersionedEntity find_entity1_postcommit = (IVersionedEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), entity_pkey);
                System.out.println("Object returned by find: " + find_entity1_postcommit);

                System.out.println("Rolling back Transaction...");
                jpaResource.getTj().rollbackTransaction();
            }

            System.out.println("**************************************************");

            System.out.println("Altering persistable fields for IVersionedEntity(id=" + entity_pkey + ") with JPA Service #2");
            find_entity2.setIntVal(user2change_intVal);
            find_entity2.setStringVal(user2change_strVal);
            System.out.println("Version id for IVersionedEntity(id=" + entity_pkey + ") with JPA Service #2:" + find_entity2.getVersionObj());

            try {
                System.out.println("Attempting to commit transaction with JPA Resource #2, an OptimisticLockException should be thrown.");
                System.out.println("EM#1 Entity Version Field State before EM-TX2 Commit: " + find_entity1.getVersionObj());
                System.out.println("EM#2 Entity Version Field State before EM-TX2 Commit: " + find_entity2.getVersionObj());

                System.out.println("Current Time Before Commit: " + new java.util.Date());
                jpaResource2.getTj().commitTransaction();
                System.out.println("Current Time After Commit: " + new java.util.Date());
                Assert.fail("Transaction Commit completed without an Exception being thrown.");
            } catch (AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                System.out.println("Current Time After Commit: " + new java.util.Date());

                // Caught an Exception, check if OptimisticLockException is in the Exception Chain
                System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for OptimisticLockException...");
                Throwable root = containsCauseByException(OptimisticLockException.class, t);
                Assert.assertNotNull("Throwable stack did not contain " + OptimisticLockException.class, root);
            } finally {
                System.out.println("EM#1 Entity State after EM-TX2 Commit: " + find_entity1);
                System.out.println("EM#2 Entity State after EM-TX2 Commit: " + find_entity2);
            }

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();
            jpaResource2.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + entity_pkey + ")...");
            IVersionedEntity find_remove_entity = (IVersionedEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), entity_pkey);
            System.out.println("Object returned by find: " + find_remove_entity);

            Assert.assertNotNull("Assert that the find operation did not return null", find_remove_entity);

            System.out.println("Removing entity...");
            jpaResource.getEm().remove(find_remove_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

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

    /**
     * Update query version test
     *
     * 10 POINTS
     */
    public void testVersioning002(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("VersioningTestLogic.testVersioning002(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
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

        // Fetch target entity type from test parameters
        String entityName = (String) testProps.get("EntityName");
        EntityVersionEntityEnum targetEntityType = EntityVersionEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        JPAPersistenceProvider provider = JPAPersistenceProvider.resolveJPAPersistenceProvider(jpaResource);

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final boolean isDB2ZOS = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);

        /*
         * TODO: Disabling these tests until the issue is is delivered:
         * https://github.com/OpenLiberty/open-liberty/issues/23949
         * https://github.com/OpenLiberty/open-liberty/issues/23952
         */
        if (isDB2ZOS && JPAPersistenceProvider.ECLIPSELINK.equals(provider)
            && (isUsingJPA21Feature() || isUsingJPA31Feature())) {
            System.out.println("Skipping test; platform (" + dbProductName + ", " + dbProductVersion + ")");
            return;
        }

        final int entity_one_pkey = 2;
        final int entity_two_pkey = 3;

        final int initial_one_intVal = 32;
        final String initial_one_strVal = "Europa";
        final int initial_two_intVal = 33;
        final String initial_two_strVal = "Ganymede";

        final int changed_one_intVal = 64;
        final String changed_one_strVal = "Io";
        final int changed_two_intVal = 65;
        final String changed_two_strVal = "Callisto";

        // Execute Test Case
        try {
            System.out.println("VersioningTestLogic.testVersioning002(): Begin");

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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=" + entity_one_pkey + ")...");
            IVersionedEntity new_entity_one = (IVersionedEntity) constructNewEntityObject(targetEntityType);
            new_entity_one.setId(entity_one_pkey);
            new_entity_one.setIntVal(initial_one_intVal);
            new_entity_one.setStringVal(initial_one_strVal);

            System.out.println("Persisting " + new_entity_one);
            jpaResource.getEm().persist(new_entity_one);

            // Construct a new entity instances
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=" + entity_two_pkey + ")...");
            IVersionedEntity new_entity_two = (IVersionedEntity) constructNewEntityObject(targetEntityType);
            new_entity_two.setId(entity_two_pkey);
            new_entity_two.setIntVal(initial_two_intVal);
            new_entity_two.setStringVal(initial_two_strVal);

            System.out.println("Persisting " + new_entity_two);
            jpaResource.getEm().persist(new_entity_two);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            System.out.println("Post-Commit Entity 1 State: " + new_entity_one);
            System.out.println("Post-Commit Entity 2 State: " + new_entity_two);

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("**************************************************");

            try {
                // Update Entity 1
                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                String queryStr = "UPDATE " + targetEntityType.getEntityName() + " e SET e.intVal = :intV, e.stringVal = :strV WHERE e.id = :pk";
                System.out.println("Create query '" + queryStr + "' (id=" + entity_one_pkey + ")...");
                Query query = jpaResource.getEm().createQuery(queryStr);
                query.setParameter("intV", changed_one_intVal);
                query.setParameter("strV", changed_one_strVal);
                query.setParameter("pk", entity_one_pkey);

                System.out.println("Executing query...");
                int updated = query.executeUpdate();
                Assert.assertEquals("No update was performed", 1, updated);

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // wait so that timestamp values are generated in future values
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Update Entity 2
                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                String queryStr2 = "UPDATE " + targetEntityType.getEntityName() + " e SET e.intVal = :intV, e.stringVal = :strV WHERE e.id = :pk";
                System.out.println("Create query '" + queryStr2 + "' (id=" + entity_two_pkey + ")...");
                query = jpaResource.getEm().createQuery(queryStr2);
                query.setParameter("intV", changed_two_intVal);
                query.setParameter("strV", changed_two_strVal);
                query.setParameter("pk", entity_two_pkey);

                System.out.println("Executing query...");
                updated = query.executeUpdate();
                Assert.assertEquals("No update was performed", 1, updated);

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Update Entity 2
                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                // Validate
                System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + entity_one_pkey + ")...");
                IVersionedEntity find_entity1 = (IVersionedEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), entity_one_pkey);
                System.out.println("Object returned by find: " + find_entity1);

                Assert.assertNotNull("Assert that the find operation did not return null", find_entity1);
                Assert.assertNotSame("Assert find did not return the original object", new_entity_one, find_entity1);
                Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity1));
                Assert.assertEquals("Assert that the entity's id is " + entity_one_pkey, find_entity1.getId(), entity_one_pkey);

                System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + entity_two_pkey + ")...");
                IVersionedEntity find_entity2 = (IVersionedEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), entity_two_pkey);
                System.out.println("Object returned by find: " + find_entity2);

                Assert.assertNotNull("Assert that the find operation did not return null", find_entity2);
                Assert.assertNotSame("Assert find did not return the original object", new_entity_two, find_entity2);
                Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity2));
                Assert.assertEquals("Assert that the entity's id is " + entity_two_pkey, find_entity2.getId(), entity_two_pkey);

                Assert.assertNotNull(find_entity1.getVersionObj());

                switch (provider) {
                    case DEFAULT:
                    case ECLIPSELINK:
                    case OPENJPA:
                        // EclipseLink and OpenJPA updates the version value during bulk update queries
                        Assert.assertFalse("Expected " + find_entity1.getVersionObj() + " would not equal " + find_entity2.getVersionObj(),
                                           find_entity1.getVersionObj().equals(find_entity2.getVersionObj()));
                        if (find_entity1.getVersionObj() instanceof java.sql.Timestamp &&
                            find_entity2.getVersionObj() instanceof java.sql.Timestamp) {
                            Assert.assertTrue("Expected find_entity2.getVersionObj() [" + find_entity2.getVersionObj() + "] "
                                              + "to be after find_entity2.getVersionObj() [" + find_entity1.getVersionObj() + "]",
                                              ((java.sql.Timestamp) find_entity2.getVersionObj()).after((java.sql.Timestamp) find_entity1.getVersionObj()));
                        }
                        break;
                    case HIBERNATE:
                        // HIBERNATE does not update the version value during bulk update queries
                        // For Timestamp versions, the original value is not generated the same for each entity
                        if (!(find_entity1.getVersionObj() instanceof java.sql.Timestamp &&
                              find_entity2.getVersionObj() instanceof java.sql.Timestamp)) {
                            Assert.assertTrue("Expected " + find_entity1.getVersionObj() + " would equal " + find_entity2.getVersionObj(),
                                              find_entity1.getVersionObj().equals(find_entity2.getVersionObj()));
                        }
                        break;
                }

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();
            } catch (Throwable t) {
                if (jpaResource.getTj().isTransactionActive()) {
                    jpaResource.getTj().rollbackTransaction();
                }

                t.printStackTrace();
                throw t;
            } finally {
                // Clean up
                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + entity_one_pkey + ")...");
                IVersionedEntity find_remove_entity1 = (IVersionedEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), entity_one_pkey);
                System.out.println("Object returned by find: " + find_remove_entity1);

                Assert.assertNotNull("Assert that the find operation did not return null", find_remove_entity1);

                System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + entity_two_pkey + ")...");
                IVersionedEntity find_remove_entity2 = (IVersionedEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), entity_two_pkey);
                System.out.println("Object returned by find: " + find_remove_entity2);

                Assert.assertNotNull("Assert that the find operation did not return null", find_remove_entity2);

                System.out.println("Removing entity...");
                jpaResource.getEm().remove(find_remove_entity1);
                jpaResource.getEm().remove(find_remove_entity2);

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();
            }

            System.out.println("Ending test.");
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("VersioningTestLogic.testVersioning002(): End");
        }
    }

    /**
     * Test that bulk update queries do not update the managed entities version
     * values as documented in the specification
     *
     * JPA Spec section 4.10: Bulk Update and Delete Operations
     *
     * Bulk update maps directly to a database update operation, bypassing optimistic locking checks.
     * Portable applications must manually update the value of the version column, if desired, and/or
     * manually validate the value of the version column.
     */
    public void testVersioning003(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("VersioningTestLogic.testVersioning003(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
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

        // Fetch target entity type from test parameters
        String entityName = (String) testProps.get("EntityName");
        EntityVersionEntityEnum targetEntityType = EntityVersionEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        JPAPersistenceProvider provider = JPAPersistenceProvider.resolveJPAPersistenceProvider(jpaResource);

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));

        final boolean isDB2ZOS = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);

        /*
         * TODO: Disabling these tests until the issue is is delivered:
         * https://github.com/OpenLiberty/open-liberty/issues/23949
         * https://github.com/OpenLiberty/open-liberty/issues/23952
         */
        if (isDB2ZOS && JPAPersistenceProvider.ECLIPSELINK.equals(provider)
            && (isUsingJPA21Feature() || isUsingJPA31Feature())) {
            System.out.println("Skipping test; platform (" + dbProductName + ", " + dbProductVersion + ")");
            return;
        }

        final int entity_one_pkey = 4;

        final int initial_one_intVal = 32;
        final String initial_one_strVal = "Europa";

        final int changed_one_intVal = 64;
        final String changed_one_strVal = "Io";

        // Execute Test Case
        try {
            System.out.println("VersioningTestLogic.testVersioning003(): Begin");

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            // Construct a new entity instances
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=" + entity_one_pkey + ")...");
            IVersionedEntity new_entity_one = (IVersionedEntity) constructNewEntityObject(targetEntityType);
            new_entity_one.setId(entity_one_pkey);
            new_entity_one.setIntVal(initial_one_intVal);
            new_entity_one.setStringVal(initial_one_strVal);

            System.out.println("Persisting " + new_entity_one);
            jpaResource.getEm().persist(new_entity_one);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            System.out.println("Post-Commit Entity 1 State: " + new_entity_one);

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("**************************************************");

            try {
                // Update Entity 1
                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + entity_one_pkey + ")...");
                IVersionedEntity find_entity1 = (IVersionedEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), entity_one_pkey);
                System.out.println("Object returned by find: " + find_entity1);

                Assert.assertNotNull("Assert that the find operation did not return null", find_entity1);

                Object version1 = find_entity1.getVersionObj();
                Assert.assertNotNull("Assert that the find version object is not null", version1);

                /*
                 * JPA Spec 3.3.2:
                 * Note that when a new transaction is begun, the managed objects in an
                 * extended persistence context are not reloaded from the database.
                 *
                 * Test that for extended scoped, committing and starting a new transaction behaves the same
                 */
                if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.CONTAINER_MANAGED_ES) {
                    System.out.println("Committing transaction...");
                    jpaResource.getTj().commitTransaction();

                    // Update Entity 1
                    System.out.println("Beginning new transaction...");
                    jpaResource.getTj().beginTransaction();
                    if (jpaResource.getTj().isApplicationManaged()) {
                        System.out.println("Joining entitymanager to JTA transaction...");
                        jpaResource.getEm().joinTransaction();
                    }
                }

                String queryStr = "UPDATE " + targetEntityType.getEntityName() + " e SET e.intVal = :intV, e.stringVal = :strV WHERE e.id = :pk";
                System.out.println("Create query '" + queryStr + "' (id=" + entity_one_pkey + ")...");
                Query query = jpaResource.getEm().createQuery(queryStr);
                query.setParameter("intV", changed_one_intVal);
                query.setParameter("strV", changed_one_strVal);
                query.setParameter("pk", entity_one_pkey);

                System.out.println("Executing query...");
                int updated = query.executeUpdate();
                Assert.assertEquals("No update was performed", 1, updated);

                /*
                 * JPA Spec 3.3.2:
                 * Note that when a new transaction is begun, the managed objects in an
                 * extended persistence context are not reloaded from the database.
                 *
                 * Test that for extended scoped, committing and starting a new transaction behaves the same
                 */
                if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.CONTAINER_MANAGED_ES) {
                    System.out.println("Committing transaction...");
                    jpaResource.getTj().commitTransaction();

                    // Update Entity 1
                    System.out.println("Beginning new transaction...");
                    jpaResource.getTj().beginTransaction();
                    if (jpaResource.getTj().isApplicationManaged()) {
                        System.out.println("Joining entitymanager to JTA transaction...");
                        jpaResource.getEm().joinTransaction();
                    }
                }

                System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + entity_one_pkey + ")...");
                IVersionedEntity find2_entity1 = (IVersionedEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), entity_one_pkey);
                System.out.println("Object returned by find: " + find2_entity1);

                Assert.assertNotNull("Assert that the find operation did not return null", find2_entity1);
                Assert.assertNotSame("Assert find did not return the original object", new_entity_one, find2_entity1);
                Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find2_entity1));
                Assert.assertEquals("Assert that the entity's id is " + entity_one_pkey, find_entity1.getId(), entity_one_pkey);

                Object version2 = find2_entity1.getVersionObj();
                Assert.assertNotNull(version2);
                Assert.assertEquals(version1, version2);

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();
            } catch (Throwable t) {
                if (jpaResource.getTj().isTransactionActive()) {
                    jpaResource.getTj().rollbackTransaction();
                }

                t.printStackTrace();
                throw t;
            } finally {
                // Clean up
                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + entity_one_pkey + ")...");
                IVersionedEntity find_remove_entity1 = (IVersionedEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), entity_one_pkey);
                System.out.println("Object returned by find: " + find_remove_entity1);

                Assert.assertNotNull("Assert that the find operation did not return null", find_remove_entity1);

                System.out.println("Removing entity...");
                jpaResource.getEm().remove(find_remove_entity1);

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();
            }

            System.out.println("Ending test.");
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("VersioningTestLogic.testVersioning003(): End");
        }
    }
}
