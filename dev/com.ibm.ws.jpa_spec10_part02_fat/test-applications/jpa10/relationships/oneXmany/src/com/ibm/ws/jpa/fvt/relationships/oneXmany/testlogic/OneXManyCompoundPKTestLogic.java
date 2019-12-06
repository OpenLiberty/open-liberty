/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.ICompoundPKOneXManyEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXmany.entities.ICompoundPKOneXManyEntityB;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class OneXManyCompoundPKTestLogic extends AbstractTestLogic {
    /**
     * Verify that the JPA provider can manage OneXMany relationships where the
     * entity on the inverse side of the relationship has a compound (ID Class) primary key.
     * Entities and IdClass are defined in annotation.
     *
     * Test Strategy:
     *
     * 1) Start a new transaction
     * 2) Create ICompoundPKOneXManyEntityA(id=1)
     * Create ICompoundPKOneXManyEntityB(id=1)
     * Set ICompoundPKOneXManyEntityA(id=1) to reference ICompoundPKOneXManyEntityB(id=1) in
     * a OneXMany relationship
     * 3) Commit the transaction
     * 4) Clear the persistence context
     * 5) Find ICompoundPKOneXManyEntityA(id=1), access ICompoundPKOneXManyEntityB(id=1) from
     * ICompoundPKOneXManyEntityA(id=1)'s OneXMany relationship field.
     *
     * Test passes if the relationship properly references ICompoundPKOneXManyEntityB(id=1)
     *
     * 14 POINTS
     */
    @SuppressWarnings("rawtypes")
    public void testOneXManyCompoundPK001(
                                          TestExecutionContext testExecCtx,
                                          TestExecutionResources testExecResources,
                                          Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXManyCompoundPKTestLogic.testOneXManyCompoundPK001(): Missing context and/or resources.  Cannot execute the test.");
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
        OneXManyCompoundPKEntityEnum targetEntityAType = OneXManyCompoundPKEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXManyCompoundPKEntityEnum targetEntityBType = OneXManyCompoundPKEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXManyCompoundPKTestLogic.testOneXManyCompoundPK001(): Begin");
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
            System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() + " (id=1)...");
            ICompoundPKOneXManyEntityA new_entityA = (ICompoundPKOneXManyEntityA) constructNewEntityObject(targetEntityAType);
            new_entityA.setId(1);
            new_entityA.setUserName("username");
            new_entityA.setPassword("password");

            System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() + " (id=1)...");
            ICompoundPKOneXManyEntityB new_entityB = (ICompoundPKOneXManyEntityB) constructNewEntityObject(targetEntityBType);
            new_entityB.setIdField(1);
            new_entityB.setCountryField("Latveria");
            new_entityB.setName("Professor Plum");

            System.out.println("Persisting " + new_entityA);
            jpaResource.getEm().persist(new_entityA);

            System.out.println("Persisting " + new_entityB);
            jpaResource.getEm().persist(new_entityB);

            System.out.println("Creating relationship between " + targetEntityAType.getEntityName() + " and " +
                               targetEntityBType.getEntityName() + " via the 'direct' relationship field...");
            new_entityA.insertIdentityField(new_entityB);

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
            ICompoundPKOneXManyEntityA find_entityA = (ICompoundPKOneXManyEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), 1);
            System.out.println("Object returned by find: " + find_entityA);

            // Verify that em.find() returned an object. (1 point)
            Assert.assertNotNull("Assert that the find operation did not return null", find_entityA);
            if (find_entityA == null) {
                // If the find returned null, then terminate the remainder of the test.
                Assert.fail("Find returned null, cancelling the remainder of the test.");
                return;
            }

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
            Assert.assertEquals(
                                "Assert that the entity's username is 'username'",
                                find_entityA.getUserName(),
                                "username");
            Assert.assertEquals(
                                "Assert that the entity's password is 'password'",
                                find_entityA.getPassword(),
                                "password");

            System.out.println("Accessing " + targetEntityBType.getEntityName() + "(id=1) through " +
                               targetEntityAType.getEntityName() + "'s identity field...");
            Collection iCollection = find_entityA.getIdentityCollectionField();
            Assert.assertNotNull(
                                 "Assert that " + targetEntityAType.getEntityName() + "(id=1).getIdentityCollectionField() is not null.",
                                 iCollection);
            Assert.assertEquals(
                                "Assert that " + targetEntityAType.getEntityName() + "(id=1).IdentityCollectionField.size() == 1",
                                1,
                                iCollection.size());

            ICompoundPKOneXManyEntityB dr_entityB = (ICompoundPKOneXManyEntityB) iCollection.iterator().next();
            Assert.assertNotNull(
                                 "Assert that an " + targetEntityBType.getEntityName() + " was extracted from the identity relationship.",
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
                                dr_entityB.getIdField(),
                                1);
            Assert.assertEquals(
                                "Assert that the extracted entity's country is 'Latveria'",
                                dr_entityB.getCountryField(),
                                "Latveria");
            Assert.assertEquals(
                                "Assert that the extracted entity's password is 'Professor Plum'",
                                dr_entityB.getName(),
                                "Professor Plum");

            System.out.println("Testing complete, rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();

            System.out.println("Ending test.");
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println("OneXManyCompoundPKTestLogic.testOneXManyCompoundPK001(): End");
        }
    }

    public void testTemplate(
                             TestExecutionContext testExecCtx,
                             TestExecutionResources testExecResources,
                             Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXManyCompoundPKTestLogic.testTemplate(): Missing context and/or resources.  Cannot execute the test.");
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
        OneXManyCompoundPKEntityEnum targetEntityAType = OneXManyCompoundPKEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXManyCompoundPKEntityEnum targetEntityBType = OneXManyCompoundPKEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXManyCompoundPKTestLogic.testTemplate(): Begin");
            //cleanupDatabase(jpaCleanupResource);

            System.out.println("Ending test.");
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println("OneXManyCompoundPKTestLogic.testTemplate(): End");
        }
    }

    protected void cleanupDatabase(JPAResource jpaResource) {
        // Cleanup the database for executing the test
        System.out.println("Cleaning up database before executing test...");
        cleanupDatabase(jpaResource.getEm(), jpaResource.getTj(), OneXManyCompoundPKEntityEnum.values());
        System.out.println("Database cleanup complete.\n");
    }
}
