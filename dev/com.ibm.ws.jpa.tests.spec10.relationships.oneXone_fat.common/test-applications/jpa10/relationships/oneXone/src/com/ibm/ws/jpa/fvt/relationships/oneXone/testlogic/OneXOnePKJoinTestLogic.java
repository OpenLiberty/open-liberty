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

import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.IPKJoinEntityA;
import com.ibm.ws.jpa.fvt.relationships.oneXone.entities.IPKJoinEntityB;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class OneXOnePKJoinTestLogic extends AbstractTestLogic {
    public void testPKJoin001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                              Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("OneXOnePKJoinTestLogic.testPKJoin001(): Missing context and/or resources.  Cannot execute the test.");
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
        OneXOnePKJoinEntityEnum targetEntityAType = OneXOnePKJoinEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        OneXOnePKJoinEntityEnum targetEntityBType = OneXOnePKJoinEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("OneXOnePKJoinTestLogic.testPKJoin001(): Begin");

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
            IPKJoinEntityA new_entityA1 = (IPKJoinEntityA) constructNewEntityObject(targetEntityAType);
            new_entityA1.setId(1);
            new_entityA1.setStrVal("Latveria");

            System.out.println("Persisting " + new_entityA1);
            jpaResource.getEm().persist(new_entityA1);

            System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() + " (id=2)...");
            IPKJoinEntityA new_entityA2 = (IPKJoinEntityA) constructNewEntityObject(targetEntityAType);
            new_entityA2.setId(2);
            new_entityA2.setStrVal("Elbonia");

            System.out.println("Persisting " + new_entityA2);
            jpaResource.getEm().persist(new_entityA2);

            System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() + " (id=1)...");
            IPKJoinEntityB new_entityB1 = (IPKJoinEntityB) constructNewEntityObject(targetEntityBType);
            new_entityB1.setId(1);
            new_entityB1.setIntVal(007);

            System.out.println("Persisting " + new_entityB1);
            jpaResource.getEm().persist(new_entityB1);

            System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() + " (id=2)...");
            IPKJoinEntityB new_entityB2 = (IPKJoinEntityB) constructNewEntityObject(targetEntityBType);
            new_entityB2.setId(2);
            new_entityB2.setIntVal(42);

            System.out.println("Persisting " + new_entityB2);
            jpaResource.getEm().persist(new_entityB2);

            System.out.println("All entities created.  Committing transaction...");
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
            IPKJoinEntityA find_entityA1 = (IPKJoinEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), 1);
            System.out.println("Object returned by find: " + find_entityA1);

            // Perform basic verifications on EntityA(id=1) (4 points)
            Assert.assertNotNull("Assert that the find operation did not return null", find_entityA1);
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

            // Perform basic verifications on EntityA(id=2) (4 points)
            System.out.println("Finding " + targetEntityAType.getEntityName() + " (id=2)...");
            IPKJoinEntityA find_entityA2 = (IPKJoinEntityA) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), 2);
            System.out.println("Object returned by find: " + find_entityA2);
            Assert.assertNotNull("Assert that the find operation did not return null", find_entityA2);
            Assert.assertNotSame(
                                 "Assert find did not return the original object",
                                 new_entityA2,
                                 find_entityA2);
            Assert.assertTrue(
                              "Assert entity returned by find is managed by the persistence context.",
                              jpaResource.getEm().contains(find_entityA2));
            Assert.assertEquals(
                                "Assert that the entity's id is 2",
                                find_entityA2.getId(),
                                2);

            // Verify that the PK-Join had established the 1:1 relationship between the entities
            System.out.println("Validated IPKJoinEntityA(id=1) and IPKJoinEntityA(id=2).  Invoking their getter methods for field entityB");
            System.out.println("To acquire references to entities IPKJoinEntityB(id=1) and IPKJoinEntityB(id=2)...");

            IPKJoinEntityB entityB1_find = find_entityA1.getIPKJoinEntityB();
            IPKJoinEntityB entityB2_find = find_entityA2.getIPKJoinEntityB();

            Assert.assertNotNull("Assert that IPKJoinEntityA(id=1).getIPKJoinEntityB() did not return null", entityB1_find);
            Assert.assertNotSame(
                                 "Assert that IPKJoinEntityA(id=1).getIPKJoinEntityB() did not return the original object",
                                 new_entityB1,
                                 entityB1_find);
            Assert.assertTrue(
                              "Assert that IPKJoinEntityA(id=1).getIPKJoinEntityB() is managed by the persistence context.",
                              jpaResource.getEm().contains(entityB1_find));
            Assert.assertEquals(
                                "Assert that IPKJoinEntityA(id=1).getIPKJoinEntityB()'s id is 1",
                                entityB1_find.getId(),
                                1);

            Assert.assertNotNull("Assert that IPKJoinEntityA(id=1).getIPKJoinEntityB() did not return null", entityB2_find);
            Assert.assertNotSame(
                                 "Assert that IPKJoinEntityA(id=2).getIPKJoinEntityB() did not return the original object",
                                 new_entityB2,
                                 entityB2_find);
            Assert.assertTrue(
                              "Assert that IPKJoinEntityA(id=2).getIPKJoinEntityB() is managed by the persistence context.",
                              jpaResource.getEm().contains(entityB2_find));
            Assert.assertEquals(
                                "Assert that IPKJoinEntityA(id=2).getIPKJoinEntityB()'s id is 2",
                                entityB2_find.getId(),
                                2);

            System.out.println("Testing complete, rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();

            System.out.println("Ending test.");
        } finally {
            System.out.println("OneXOnePKJoinTestLogic.testPKJoin001(): End");
        }
    }
}
