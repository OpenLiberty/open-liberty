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

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.ICompoundPKOneXOneEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.ICompoundPKOneXOneEntityB;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class OneXOneCompoundPKTestLogic extends AbstractTestLogic {
    /**
     * Verify that the JPA provider can manage OneXOne relationships where the
     * entity on the inverse side of the relationship has a compound (ID Class) primary key.
     * Entities and IdClass are defined in annotation.
     *
     * Test Strategy:
     *
     * 1) Start a new transaction
     * 2) Create ICompoundPKOneXOneEntityA(id=1)
     * Create ICompoundPKOneXOneEntityB(id=1)
     * Set ICompoundPKOneXOneEntityA(id=1) to reference ICompoundPKOneXOneEntityB(id=1) in
     * a OneXOne relationship
     * 3) Commit the transaction
     * 4) Clear the persistence context
     * 5) Find ICompoundPKOneXOneEntityA(id=1), access ICompoundPKOneXOneEntityB(id=1) from
     * ICompoundPKOneXOneEntityA(id=1)'s OneXOne relationship field.
     *
     * Test passes if the relationship properly references ICompoundPKOneXOneEntityB(id=1)
     *
     * 12 POINTS
     */
    public void testOneXOneCompoundPK001(
                                         TestExecutionContext testExecCtx,
                                         TestExecutionResources testExecResources,
                                         Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXOneCompoundPKTestLogic.testOneXOneCompoundPK001(): Missing context and/or resources.  Cannot execute the test.");
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
        OneXOneCompoundPKEntityEnum targetEntityAType = OneXOneCompoundPKEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXOneCompoundPKEntityEnum targetEntityBType = OneXOneCompoundPKEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXOneCompoundPKTestLogic.testOneXOneCompoundPK001(): Begin");

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
            ICompoundPKOneXOneEntityA new_entityA = (ICompoundPKOneXOneEntityA) constructNewEntityObject(targetEntityAType);
            new_entityA.setId(1);
            new_entityA.setUserName("username");
            new_entityA.setPassword("password");

            System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() + " (id=1)...");
            ICompoundPKOneXOneEntityB new_entityB = (ICompoundPKOneXOneEntityB) constructNewEntityObject(targetEntityBType);
            new_entityB.setIdField(1);
            new_entityB.setCountryField("Latveria");
            new_entityB.setName("Professor Plum");

            System.out.println("Persisting " + new_entityA);
            jpaResource.getEm().persist(new_entityA);

            System.out.println("Persisting " + new_entityB);
            jpaResource.getEm().persist(new_entityB);

            System.out.println("Creating relationship between " + targetEntityAType.getEntityName() + " and " +
                               targetEntityBType.getEntityName() + " via the 'direct' relationship field...");
            new_entityA.setIdentityField(new_entityB);

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

            System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=1)...");
            ICompoundPKOneXOneEntityA find_entityA = (ICompoundPKOneXOneEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), 1);
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

            ICompoundPKOneXOneEntityB dr_entityB = find_entityA.getIdentityField();
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
        } finally {
            System.out.println("OneXOneCompoundPKTestLogic.testOneXOneCompoundPK001(): End");
        }
    }
}
