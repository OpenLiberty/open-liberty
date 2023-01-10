/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.testlogic;

import java.lang.reflect.InvocationTargetException;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities.EntListTestEntity;
import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities.MFDRelationalEntA;
import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities.MFDRelationalMMB;
import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities.MFDRelationalMOB;
import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities.MFDRelationalOMB;
import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities.MFDRelationalOOB;
import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.entities.MappingFileEntity;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class MappingFileDefaultsTestLogic extends AbstractTestLogic {

    /**
     * (Using MFDEntity* types):
     * Verify that mapping file defaults for schema and catalog are observed by all entities in the persistence unit.
     * Verify that defining a package mapping default entry applies to entity class names.
     *
     * (Using MFDMappedSuperclass* types):
     * Verify that defining a package mapping default entry applies to mapped superclass class names.
     *
     * (Using MFDNFQEmbeddable* types):
     * Verify that defining a package mapping default entry applies to embeddable class names.
     *
     * (12 POINTS)
     */
    public void executeBasicMappingFileDefaultTest(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                   Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("MappingFileDefaultsTestLogic.executeBasicMappingFileDefaultTest(): Missing context and/or resources.  Cannot execute the test.");
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
        MappingFileDefaultsEntityEnum targetEntityAType = MappingFileDefaultsEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        MappingFileDefaultsEntityEnum targetEntityBType = MappingFileDefaultsEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        String name_1 = "Dr. Doom";
        String street_1 = "1024 Archvillain Lane";
        String state_1 = "Latveria";
        String zip_1 = "12345";

        // Execute Test Case
        try {
            System.out.println("MappingFileDefaultsTestLogic.executeBasicMappingFileDefaultTest(): Begin");
            //cleanupDatabase(jpaCleanupResource, log);

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
            MappingFileEntity entityFQ = (MappingFileEntity) constructNewEntityObject(targetEntityAType);
            entityFQ.setId(1);
            entityFQ.setName(name_1);
            entityFQ.setStreet(street_1);
            entityFQ.setState(state_1);
            entityFQ.setZip(zip_1);

            System.out.println("Persisting the entity defined with the fully qualified classname...");
            System.out.println("Persisting " + entityFQ);
            jpaResource.getEm().persist(entityFQ);

            System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() + " (id=1)...");
            MappingFileEntity entityNFQ = (MappingFileEntity) constructNewEntityObject(targetEntityBType);
            entityNFQ.setId(1);
            entityNFQ.setName(name_1);
            entityNFQ.setStreet(street_1);
            entityNFQ.setState(state_1);
            entityNFQ.setZip(zip_1);

            System.out.println("Persisting the entity defined with the non-fully qualified classname...");
            System.out.println("Persisting " + entityNFQ);
            jpaResource.getEm().persist(entityNFQ);

            System.out.println("Both entities created, Committing transaction...");
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

            System.out.println("Finding the entity defined with the fully qualified classname...");
            MappingFileEntity entityFQ_find = (MappingFileEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), 1);
            Assert.assertNotNull("The find operation returned a null value.", entityFQ_find);
            Assert.assertNotSame("The find operation returned the same object as the original", entityFQ, entityFQ_find);
            Assert.assertEquals(
                                "Assert " + targetEntityAType.getEntityName() + " (id=1) has expected name_1 value.",
                                entityFQ.getName(),
                                name_1);
            Assert.assertEquals(
                                "Assert " + targetEntityAType.getEntityName() + " (id=1) has expected street_1 value.",
                                entityFQ.getStreet(),
                                street_1);
            Assert.assertEquals(
                                "Assert " + targetEntityAType.getEntityName() + " (id=1) has expected state_1 value.",
                                entityFQ.getState(),
                                state_1);
            Assert.assertEquals(
                                "Assert " + targetEntityAType.getEntityName() + " (id=1) has expected zip_1 value.",
                                entityFQ.getZip(),
                                zip_1);

            System.out.println("Finding the entity defined with the non-fully qualified classname...");
            MappingFileEntity entityNFQ_find = (MappingFileEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityBType), 1);
            Assert.assertNotNull("The find operation returned a null value.", entityNFQ_find);
            Assert.assertNotSame("The find operation returned the same object as the original", entityNFQ, entityNFQ_find);
            Assert.assertEquals(
                                "Assert " + targetEntityBType.getEntityName() + " (id=1) has expected name_1 value.",
                                entityNFQ_find.getName(),
                                name_1);
            Assert.assertEquals(
                                "Assert " + targetEntityBType.getEntityName() + " (id=1) has expected street_1 value.",
                                entityNFQ_find.getStreet(),
                                street_1);
            Assert.assertEquals(
                                "Assert " + targetEntityBType.getEntityName() + " (id=1) has expected state_1 value.",
                                entityNFQ_find.getState(),
                                state_1);
            Assert.assertEquals(
                                "Assert " + targetEntityBType.getEntityName() + " (id=1) has expected zip_1 value.",
                                entityNFQ_find.getZip(),
                                zip_1);

            System.out.println("Ending test.");
        } catch (ClassNotFoundException | SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException
                        | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            System.out.println("MappingFileDefaultsTestLogic.executeBasicMappingFileDefaultTest(): End");
        }
    }

    /**
     * Verify that defining a package mapping default entry applies to entity-listener class names.
     *
     * (2 POINTS)
     */
    public void executeListenerTest(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                    Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("MappingFileDefaultsTestLogic.executeListenerTest(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("MappingFileDefaultsTestLogic.executeListenerTest(): Begin");
            //cleanupDatabase(jpaCleanupResource, log);

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
            MappingFileDefaultsEntityEnum targetEntityType = MappingFileDefaultsEntityEnum.EntListTestEntity;
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=1)...");
            EntListTestEntity new_entity = (EntListTestEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(1);
            new_entity.setName("Dr. Doom");

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // At this point, all of the PrePersist lifecycle methods should have fired, setting their
            // respective transient fields on the entity to true.  Verify that each one has been
            // set true.  if it has, then Entity Listeners using a non-fully-qualified classname
            // had successfully fired.
            System.out.println("Verifying that all four entity listeners fired...");

            Assert.assertTrue("Fully-qualified Listener did not fire.", new_entity.isEntityListenerFQ());
            Assert.assertTrue("Non-fully-qualified Listener did not fire.", new_entity.isEntityListenerNFQ());

            System.out.println("Ending test.");
        } catch (ClassNotFoundException | SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException
                        | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            System.out.println("MappingFileDefaultsTestLogic.executeListenerTest(): End");
        }
    }

    /**
     * Verify that defining a package mapping default entry applies to target-entity class names for many-to-one elements.
     *
     * (1 POINT)
     */
    public void testManyToOneTargetEntityClass(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                               Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("MappingFileDefaultsTestLogic.testManyToOneTargetEntityClass(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("MappingFileDefaultsTestLogic.testManyToOneTargetEntityClass(): Begin");
            // cleanupDatabase(jpaCleanupResource, log);

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
            MappingFileDefaultsEntityEnum targetEntityAType = MappingFileDefaultsEntityEnum.MFDRelationalEntA;
            System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() + " (id=1)...");
            MFDRelationalEntA new_entityA = (MFDRelationalEntA) constructNewEntityObject(targetEntityAType);
            new_entityA.setId(1);
            new_entityA.setName("Dr. Doom");

            System.out.println("Persisting " + new_entityA);
            jpaResource.getEm().persist(new_entityA);

            MappingFileDefaultsEntityEnum targetEntityBType = MappingFileDefaultsEntityEnum.MFDRelationalMOB;
            System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() + " (id=1)...");
            MFDRelationalMOB new_entityB = (MFDRelationalMOB) constructNewEntityObject(targetEntityBType);
            new_entityB.setId(1);
            new_entityB.setName("Doombot");

            System.out.println("Persisting " + new_entityB);
            jpaResource.getEm().persist(new_entityB);

            System.out.println("Establishing many-to-one relationship...");
            new_entityA.setManyXOneEntityB(new_entityB);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

//            log.assertPass(
//                           "Successfully committed transaction.  No exceptions were thrown, meaning MFDRelationalMOB, defined with a non-fully qualified\n" +
//                           "entity, was accepted by the JPA Implementation.");

            System.out.println("Ending test.");
        } catch (ClassNotFoundException | SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException
                        | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            System.out.println("MappingFileDefaultsTestLogic.testManyToOneTargetEntityClass(): End");
        }
    }

    /**
     * Verify that defining a package mapping default entry applies to target-entity class names for one-to-one elements.
     *
     * (1 POINT)
     */
    public void testOneToOneTargetEntityClass(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                              Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("MappingFileDefaultsTestLogic.testOneToOneTargetEntityClass(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("MappingFileDefaultsTestLogic.testOneToOneTargetEntityClass(): Begin");
            //cleanupDatabase(jpaCleanupResource, log);

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
            MappingFileDefaultsEntityEnum targetEntityAType = MappingFileDefaultsEntityEnum.MFDRelationalEntA;
            System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() + " (id=1)...");
            MFDRelationalEntA new_entityA = (MFDRelationalEntA) constructNewEntityObject(targetEntityAType);
            new_entityA.setId(1);
            new_entityA.setName("Dr. Doom");

            System.out.println("Persisting " + new_entityA);
            jpaResource.getEm().persist(new_entityA);

            MappingFileDefaultsEntityEnum targetEntityBType = MappingFileDefaultsEntityEnum.MFDRelationalOOB;
            System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() + " (id=1)...");
            MFDRelationalOOB new_entityB = (MFDRelationalOOB) constructNewEntityObject(targetEntityBType);
            new_entityB.setId(1);
            new_entityB.setName("Doombot");

            System.out.println("Persisting " + new_entityB);
            jpaResource.getEm().persist(new_entityB);

            System.out.println("Establishing one-to-one relationship...");
            new_entityA.setOneXoneEntityB(new_entityB);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

//            log.assertPass(
//                           "Successfully committed transaction.  No exceptions were thrown, meaning MFDRelationalOOB, defined with a non-fully qualified\n" +
//                           "entity, was accepted by the JPA Implementation.");

            System.out.println("Ending test.");
        } catch (ClassNotFoundException | SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException
                        | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            System.out.println("MappingFileDefaultsTestLogic.testOneToOneTargetEntityClass(): End");
        }
    }

    /**
     * Verify that defining a package mapping default entry applies to target-entity class names for one-to-many elements.
     *
     */
    public void testOneToManyTargetEntityClass(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                               Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("MappingFileDefaultsTestLogic.testOneToManyTargetEntityClass(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("MappingFileDefaultsTestLogic.testOneToManyTargetEntityClass(): Begin");
            //cleanupDatabase(jpaCleanupResource, log);

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
            MappingFileDefaultsEntityEnum targetEntityAType = MappingFileDefaultsEntityEnum.MFDRelationalEntA;
            System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() + " (id=1)...");
            MFDRelationalEntA new_entityA = (MFDRelationalEntA) constructNewEntityObject(targetEntityAType);
            new_entityA.setId(1);
            new_entityA.setName("Dr. Doom");

            System.out.println("Persisting " + new_entityA);
            jpaResource.getEm().persist(new_entityA);

            MappingFileDefaultsEntityEnum targetEntityBType = MappingFileDefaultsEntityEnum.MFDRelationalOMB;
            System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() + " (id=1)...");
            MFDRelationalOMB new_entityB = (MFDRelationalOMB) constructNewEntityObject(targetEntityBType);
            new_entityB.setId(1);
            new_entityB.setName("Doombot");

            System.out.println("Persisting " + new_entityB);
            jpaResource.getEm().persist(new_entityB);

            System.out.println("Establishing one-to-many relationship...");
            new_entityA.getOneXmanyEntityBCollection().add(new_entityB);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

//            log.assertPass(
//                           "Successfully committed transaction.  No exceptions were thrown, meaning MFDRelationalOMB, defined with a non-fully qualified\n" +
//                           "entity, was accepted by the JPA Implementation.");

            System.out.println("Ending test.");
        } catch (ClassNotFoundException | SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException
                        | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            System.out.println("MappingFileDefaultsTestLogic.testOneToManyTargetEntityClass(): End");
        }
    }

    /**
     * Verify that defining a package mapping default entry applies to target-entity class names for
     * many-to-many elements.
     *
     */
    public void testManyToManyTargetEntityClass(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("MappingFileDefaultsTestLogic.testManyToManyTargetEntityClass(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("MappingFileDefaultsTestLogic.testManyToManyTargetEntityClass(): Begin");
            // cleanupDatabase(jpaCleanupResource, log);

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
            MappingFileDefaultsEntityEnum targetEntityAType = MappingFileDefaultsEntityEnum.MFDRelationalEntA;
            System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() + " (id=1)...");
            MFDRelationalEntA new_entityA = (MFDRelationalEntA) constructNewEntityObject(targetEntityAType);
            new_entityA.setId(1);
            new_entityA.setName("Dr. Doom");

            System.out.println("Persisting " + new_entityA);
            jpaResource.getEm().persist(new_entityA);

            MappingFileDefaultsEntityEnum targetEntityBType = MappingFileDefaultsEntityEnum.MFDRelationalMMB;
            System.out.println("Creating new object instance of " + targetEntityBType.getEntityName() + " (id=1)...");
            MFDRelationalMMB new_entityB = (MFDRelationalMMB) constructNewEntityObject(targetEntityBType);
            new_entityB.setId(1);
            new_entityB.setName("Doombot");

            System.out.println("Persisting " + new_entityB);
            jpaResource.getEm().persist(new_entityB);

            System.out.println("Establishing many-to-many relationship...");
            new_entityA.getOneXmanyEntityBCollection().add(new_entityB);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

//            log.assertPass(
//                           "Successfully committed transaction.  No exceptions were thrown, meaning MFDRelationalMMB, defined with a non-fully qualified\n" +
//                           "entity, was accepted by the JPA Implementation.");

            System.out.println("Ending test.");
        } catch (ClassNotFoundException | SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException
                        | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            System.out.println("MappingFileDefaultsTestLogic.testManyToManyTargetEntityClass(): End");
        }
    }

    public void testTemplate(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                             Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("MappingFileDefaultsTestLogic.testTemplate(): Missing context and/or resources.  Cannot execute the test.");
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
        MappingFileDefaultsEntityEnum targetEntityAType = MappingFileDefaultsEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        MappingFileDefaultsEntityEnum targetEntityBType = MappingFileDefaultsEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("MappingFileDefaultsTestLogic.testTemplate(): Begin");
            //cleanupDatabase(jpaCleanupResource, log);

            System.out.println("Ending test.");
        } finally {
            System.out.println("MappingFileDefaultsTestLogic.testTemplate(): End");
        }
    }

//    protected void cleanupDatabase(JPAResource jpaResource) {
//        // Cleanup the database for executing the test
//        System.out.println("Cleaning up database before executing test...");
//        cleanupDatabase(jpaResource.getEm(), jpaResource.getTj(), MappingFileDefaultsEntityEnum.values(), log);
//        System.out.println("Database cleanup complete.\n");
//    }
}
