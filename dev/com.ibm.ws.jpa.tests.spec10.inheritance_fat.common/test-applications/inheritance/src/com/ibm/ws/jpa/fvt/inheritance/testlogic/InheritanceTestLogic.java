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

package com.ibm.ws.jpa.fvt.inheritance.testlogic;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.inheritance.entities.ITreeLeaf1;
import com.ibm.ws.jpa.fvt.inheritance.entities.ITreeLeaf2;
import com.ibm.ws.jpa.fvt.inheritance.entities.ITreeLeaf3;
import com.ibm.ws.jpa.fvt.inheritance.entities.ITreeRoot;
import com.ibm.ws.jpa.fvt.inheritance.entities.msc.IMSC;
import com.ibm.ws.jpa.fvt.inheritance.entities.msc.IMSCEntity;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class InheritanceTestLogic extends AbstractTestLogic {

    /**
     * Test basic CRUD operations with the targeted entity type to verify basic inheritance.
     * <p>
     * Points: 13
     */
    public void testInheritance001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("InheritanceTestLogic.testInheritance001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityName = (String) testExecCtx.getProperties().get("EntityName");
        InheritanceEntityEnum targetEntityType = InheritanceEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid entity type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        int id = 1;

        // Execute Test Case
        try {
            System.out.println("InheritanceTestLogic.testInheritance001(): Begin");

            System.out.println("1) Create and persist " + targetEntityType.getEntityName() + " (id=" + id + ").");

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Construct a new entity instance
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + "...");
            ITreeRoot new_entity = (ITreeRoot) constructNewEntityObject(targetEntityType);
            new_entity.setId(id);
            new_entity.setName(targetEntityType.getEntityName() + "-" + id);

            if (new_entity instanceof ITreeLeaf1 || new_entity instanceof IMSCEntity) {
                ((ITreeLeaf1) new_entity).setIntVal(42);
            } else if (new_entity instanceof ITreeLeaf2) {
                ((ITreeLeaf2) new_entity).setFloatVal(42.0f);
            } else if (new_entity instanceof ITreeLeaf3) {
                ((ITreeLeaf3) new_entity).setStringVal1("String-Val1");
                ((ITreeLeaf3) new_entity).setStringVal2("String-Val2");
            }

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // 2) Verify the entity was saved to the database
            System.out.println("2) Verify the entity was saved to the database");

            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            ITreeRoot find_entity = (ITreeRoot) jpaResource.getEm().find(resolveEntityClass(targetEntityType), 1);
            System.out.println("Object returned by find: " + find_entity);

            // Verify that em.find() returned an object.
            Assert.assertNotNull("Assert that the find operation did not return null", find_entity);

            // Perform basic verifications (4 points)
            Assert.assertTrue("Assert find did not return the original object",
                              new_entity != find_entity);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.",
                              jpaResource.getEm().contains(find_entity));
            Assert.assertEquals("Assert that the entity's id is " + id,
                                find_entity.getId(),
                                id);
            Assert.assertEquals("Assert that the entity's name field is \"" + targetEntityType.getEntityName() + "-" + id + "\"",
                                find_entity.getName(),
                                targetEntityType.getEntityName() + "-" + id);

            // Perform Entity-type specific verifications (1 point)
            if (find_entity instanceof ITreeLeaf1 || new_entity instanceof IMSCEntity) {
                Assert.assertEquals("Assert intVal field == 42", 42, ((ITreeLeaf1) find_entity).getIntVal());
            } else if (find_entity instanceof ITreeLeaf2) {
                Assert.assertEquals("Asert floatVal field == 42.0f", 42.0f, ((ITreeLeaf2) find_entity).getFloatVal(), 0.1);
            } else if (find_entity instanceof ITreeLeaf3) {
                System.out.println(targetEntityType.getEntityName() + "(id=1).getStringVal1 = " +
                                   ((ITreeLeaf3) find_entity).getStringVal1());
                System.out.println(targetEntityType.getEntityName() + "(id=1).getStringVal2 = " +
                                   ((ITreeLeaf3) find_entity).getStringVal2());

                boolean passed = true;
                passed = passed && "String-Val1".equals(((ITreeLeaf3) find_entity).getStringVal1());
                passed = passed && "String-Val2".equals(((ITreeLeaf3) find_entity).getStringVal2());
                Assert.assertTrue("Assert StringVal1 and StringVal2 are correct.", passed);
            }

            // 3) Update the entity
            System.out.println("3) Updating the entity...");
            find_entity.setName("Updated-" + targetEntityType.getEntityName() + "-1");

            if (new_entity instanceof ITreeLeaf1 || new_entity instanceof IMSCEntity) {
                ((ITreeLeaf1) find_entity).setIntVal(420);
            } else if (new_entity instanceof ITreeLeaf2) {
                ((ITreeLeaf2) find_entity).setFloatVal(420.0f);
            } else if (new_entity instanceof ITreeLeaf3) {
                ((ITreeLeaf3) find_entity).setStringVal1("Updated-String-Val1");
                ((ITreeLeaf3) find_entity).setStringVal2("Updated-String-Val2");
            }

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // 4) Verify Update
            System.out.println("4) Verify the updates were saved to the database");

            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            ITreeRoot find2_entity = (ITreeRoot) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find2_entity);

            // Verify that em.find() returned an object.
            Assert.assertNotNull("Assert that the find operation did not return null", find2_entity);

            // Perform basic verifications (4 points)
            Assert.assertTrue("Assert find did not return the original object",
                              new_entity != find2_entity);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.",
                              jpaResource.getEm().contains(find2_entity));
            Assert.assertEquals("Assert that the entity's id is " + id,
                                find2_entity.getId(),
                                id);
            Assert.assertEquals("Assert that the entity's name field is \"" + find_entity.getName() + "\"",
                                find_entity.getName(),
                                find2_entity.getName());

            // Perform Entity-type specific verifications (1 point)
            if (find_entity instanceof ITreeLeaf1 || new_entity instanceof IMSCEntity) {
                Assert.assertEquals("Assert intVal field == " + ((ITreeLeaf1) find_entity).getIntVal(),
                                    ((ITreeLeaf1) find_entity).getIntVal(),
                                    ((ITreeLeaf1) find2_entity).getIntVal());
            } else if (find_entity instanceof ITreeLeaf2) {
                Assert.assertEquals("Asert floatVal field == " + ((ITreeLeaf2) find_entity).getFloatVal(),
                                    ((ITreeLeaf2) find_entity).getFloatVal(),
                                    ((ITreeLeaf2) find2_entity).getFloatVal(), 0.1);
            } else if (find_entity instanceof ITreeLeaf3) {
                System.out.println(targetEntityType.getEntityName() + "(id=1).getStringVal1 = " +
                                   ((ITreeLeaf3) find2_entity).getStringVal1());
                System.out.println(targetEntityType.getEntityName() + "(id=1).getStringVal2 = " +
                                   ((ITreeLeaf3) find2_entity).getStringVal2());

                boolean passed = true;
                passed = passed && ((ITreeLeaf3) find_entity).getStringVal1().equals(((ITreeLeaf3) find2_entity).getStringVal1());
                passed = passed && ((ITreeLeaf3) find_entity).getStringVal2().equals(((ITreeLeaf3) find2_entity).getStringVal2());
                Assert.assertTrue("Assert StringVal1 and StringVal2 are correct.", passed);
            }

            // 6) Delete the entity from the database
            System.out.println("5) Delete the entity from the database");

            System.out.println("Removing " + targetEntityType.getEntityName() + "(id=" + id + ")...");
            jpaResource.getEm().remove(find2_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // 7) Verify the entity remove was successful
            System.out.println("7) Verify the entity remove was successful");

            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Perform the find operation
            System.out.println("Finding " + targetEntityType.getEntityName() + "(id=" + id + ")...");
            ITreeRoot removed_entity = (ITreeRoot) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + removed_entity);

            // Verify that the entity could not be found.
            Assert.assertNull("Assert that the find operation did return null", removed_entity);

            System.out.println("Ending test.");
        } finally {
            System.out.println("InheritanceTestLogic.testInheritance001(): End");
        }
    }

    /**
     * Test basic CRUD operations with the targeted entity type to verify mapped superclass inheritance.
     * <p>
     * Points: 13
     */
    public void testMSCInheritance001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                      Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("InheritanceTestLogic.testMSCInheritance001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityName = (String) testExecCtx.getProperties().get("EntityName");
        InheritanceEntityEnum targetEntityType = InheritanceEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid entity type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        int id = 1;

        // Execute Test Case
        try {
            System.out.println("InheritanceTestLogic.testMSCInheritance001(): Begin");

            System.out.println("1) Create and persist " + targetEntityType.getEntityName() + " (id=" + id + ").");

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Construct a new entity instance
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + "...");
            IMSCEntity new_entity = (IMSCEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(id);
            new_entity.setName("Dr. Doom");
            new_entity.setDescription("Latveria");

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // 2) Verify the entity was saved to the database
            System.out.println("2) Verify the entity was saved to the database");

            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            IMSCEntity find_entity = (IMSCEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_entity);

            // Verify that em.find() returned an object.
            Assert.assertNotNull("Assert that the find operation did not return null", find_entity);

            // Perform basic verifications (4 points)
            Assert.assertTrue("Assert find did not return the original object",
                              new_entity != find_entity);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.",
                              jpaResource.getEm().contains(find_entity));
            Assert.assertEquals("Assert that the entity's id is " + id,
                                find_entity.getId(),
                                id);
            Assert.assertEquals("Assert that the entity's name field is \"Dr. Doom\"",
                                find_entity.getName(),
                                "Dr. Doom");
            Assert.assertEquals("Assert that the entity's description field is \"Latveria\"",
                                find_entity.getDescription(),
                                "Latveria");

            // 3) Update the entity
            System.out.println("3) Updating the entity...");
            find_entity.setName("Reed Richards");
            find_entity.setDescription("Baxtrr Building");

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // 4) Verify Update
            System.out.println("4) Verify the updates were saved to the database");

            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            IMSC find2_entity = (IMSC) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find2_entity);

            // Verify that em.find() returned an object.
            Assert.assertNotNull("Assert that the find operation did not return null", find2_entity);

            // Perform basic verifications (4 points)
            Assert.assertTrue("Assert find did not return the original object",
                              new_entity != find2_entity);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.",
                              jpaResource.getEm().contains(find2_entity));
            Assert.assertEquals("Assert that the entity's id is " + id,
                                find2_entity.getId(),
                                id);
            Assert.assertEquals("Assert that the entity's name field is \"Reed Richards\"",
                                "Reed Richards",
                                find2_entity.getName());
            Assert.assertEquals("Assert that the entity's description field is \"Latveria\"",
                                find_entity.getDescription(),
                                "Baxtrr Building");

            // 6) Delete the entity from the database
            System.out.println("5) Delete the entity from the database");

            System.out.println("Removing " + targetEntityType.getEntityName() + "(id=" + id + ")...");
            jpaResource.getEm().remove(find2_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // 7) Verify the entity remove was successful
            System.out.println("7) Verify the entity remove was successful");

            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Perform the find operation
            System.out.println("Finding " + targetEntityType.getEntityName() + "(id=" + id + ")...");
            ITreeRoot removed_entity = (ITreeRoot) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + removed_entity);

            // Verify that the entity could not be found.
            Assert.assertNull("Assert that the find operation did return null", removed_entity);

            System.out.println("Ending test.");
        } finally {
            System.out.println("InheritanceTestLogic.testMSCInheritance001(): End");
        }
    }
}
