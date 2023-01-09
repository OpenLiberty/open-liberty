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

package com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.metadatacomplete.testlogic;

import java.lang.reflect.InvocationTargetException;

import javax.persistence.PersistenceException;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.metadatacomplete.entities.IMDCEntity;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class MetadataCompleteTestLogic extends AbstractTestLogic {
    /**
     * (6 POINTS)
     *
     */
    public void executeMetadataCompleteTest001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                               Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("MetadataCompleteTestLogic.executeMetadataCompleteTest001(): Missing context and/or resources.  Cannot execute the test.");
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
        MetadataCompleteEntityEnum targetEntityAType = MetadataCompleteEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("MetadataCompleteTestLogic.executeMetadataCompleteTest001(): Begin");
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
            System.out.println("Creating new object instance of " + targetEntityAType.getEntityName() + " (id=1)...");
            IMDCEntity new_entity = (IMDCEntity) constructNewEntityObject(targetEntityAType);
            new_entity.setId(1);
            new_entity.setName("Grossman"); // Annotation is set to limit size to 12 characters

            System.out.println("Persisting the entity defined with the fully qualified classname...");
            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Committing transaction...");
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
            IMDCEntity entity_find1 = (IMDCEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), 1);
            Assert.assertNotNull("The find operation returned a null value.", entity_find1);
            Assert.assertNotSame("The find operation returned the same object as the original", new_entity, entity_find1);

            System.out.println("Setting name to a length greater then twelve but lesser then 30...");
            entity_find1.setName("12345678901234567890");

            System.out.println("Committing transaction (should succeed)...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Transaction commit was successful.  Verifying that the string length limit set by the XML is present.");

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding the entity defined with the fully qualified classname...");
            IMDCEntity entity_find2 = (IMDCEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityAType), 1);
            Assert.assertNotNull("The find operation returned a null value.", entity_find2);
            Assert.assertNotSame("The find operation returned the same object as the original", new_entity, entity_find2);
            Assert.assertNotSame("The find operation returned the same object as the first find", entity_find1, entity_find2);

            System.out.println("Setting name to a length greater then 30...");
            entity_find2.setName("1234567890123456789012345678901234567890");

            try {
                System.out.println("Committing transaction (should throw an Exception)...");
                jpaResource.getTj().commitTransaction();

                // No Exception was thrown, this has failed.
                Assert.fail("Transaction Commit completed without an Exception being thrown.");
            } catch (Throwable t) {
                // Caught an Exception, check if IllegalStateException is in the Exception Chain
                System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for PersistenceException...");
                assertExceptionIsInChain(PersistenceException.class, t);
            } finally {
                if (jpaResource.getTj().isTransactionActive()) {
                    System.out.println("Rolling back the transaction...");
                    jpaResource.getTj().rollbackTransaction();
                }
            }

            System.out.println("Ending test.");
        } catch (ClassNotFoundException | SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException
                        | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            System.out.println("MetadataCompleteTestLogic.executeMetadataCompleteTest001(): End");
        }
    }

    public void testTemplate(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                             Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("MetadataCompleteTestLogic.testTemplate(): Missing context and/or resources.  Cannot execute the test.");
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
        MetadataCompleteEntityEnum targetEntityAType = MetadataCompleteEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        MetadataCompleteEntityEnum targetEntityBType = MetadataCompleteEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("MetadataCompleteTestLogic.testTemplate(): Begin");
            //cleanupDatabase(jpaCleanupResource, log);

            System.out.println("Ending test.");
        } finally {
            System.out.println("MetadataCompleteTestLogic.testTemplate(): End");
        }
    }

//    protected void cleanupDatabase(JPAResource jpaResource) {
//        // Cleanup the database for executing the test
//        System.out.println("Cleaning up database before executing test...");
//        cleanupDatabase(jpaResource.getEm(), jpaResource.getTj(), MetadataCompleteEntityEnum.values(), log);
//        System.out.println("Database cleanup complete.\n");
//    }
}
