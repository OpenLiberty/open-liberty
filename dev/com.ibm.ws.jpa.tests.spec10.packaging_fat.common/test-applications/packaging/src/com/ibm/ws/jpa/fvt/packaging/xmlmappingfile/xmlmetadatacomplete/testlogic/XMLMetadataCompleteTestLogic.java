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

package com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.xmlmetadatacomplete.testlogic;

import java.lang.reflect.InvocationTargetException;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.xmlmetadatacomplete.entities.XMLCompleteTestEntity;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class XMLMetadataCompleteTestLogic extends AbstractTestLogic {
    /**
     * Verify that annotation in an entity is ignored by the persistence provider with entities that are also
     * defined in the XML Mapping File.
     */
    public void executeXMLMetadataCompleteTest001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                  Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("XMLMetadataCompleteTestLogic.executeXMLMetadataCompleteTest001(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        XMLMetadataCompleteEntityEnum targetEntityAType = XMLMetadataCompleteEntityEnum.XMLCompleteTestEntity;

        // Execute Test Case
        try {
            System.out.println("XMLMetadataCompleteTestLogic.executeXMLMetadataCompleteTest001(): Begin");
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
            XMLCompleteTestEntity new_entity = (XMLCompleteTestEntity) constructNewEntityObject(targetEntityAType);
            new_entity.setId(1);

            System.out.println("Persisting the entity defined with the fully qualified classname...");
            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Table has fixed 'nonOptionalName' column.  If the annotation in the class was ignored, " +
                               "then the entity will be able to persisted to the database without incident.");

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // og.assertPass("No exception was thrown, so the @Column annotation for nonOptionalName was ignored.");

            System.out.println("Ending test.");
        } catch (ClassNotFoundException | SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException
                        | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            System.out.println("XMLMetadataCompleteTestLogic.executeXMLMetadataCompleteTest001(): End");
        }
    }

    public void testTemplate(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                             Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("XMLMetadataCompleteTestLogic.testTemplate(): Missing context and/or resources.  Cannot execute the test.");
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
        XMLMetadataCompleteEntityEnum targetEntityAType = XMLMetadataCompleteEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        XMLMetadataCompleteEntityEnum targetEntityBType = XMLMetadataCompleteEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("XMLMetadataCompleteTestLogic.testTemplate(): Begin");
            //cleanupDatabase(jpaCleanupResource, log);

            System.out.println("Ending test.");
        } finally {
            System.out.println("XMLMetadataCompleteTestLogic.testTemplate(): End");
        }
    }

//    protected void cleanupDatabase(JPAResource jpaResource) {
//        // Cleanup the database for executing the test
//        System.out.println("Cleaning up database before executing test...");
//        cleanupDatabase(jpaResource.getEm(), jpaResource.getTj(), XMLMetadataCompleteEntityEnum.values(), log);
//        System.out.println("Database cleanup complete.\n");
//    }
}
