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

package com.ibm.ws.jpa.fvt.entity.testlogic;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.entity.entities.IIdClassEntity;
import com.ibm.ws.jpa.fvt.entity.support.PKey;
import com.ibm.ws.jpa.fvt.entity.testlogic.enums.IDClassEntityEnum;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class IDClassTestLogic extends AbstractTestLogic {
    private int standardId = 1;
    private String standardCountry = "Latveria";

    /*
     * 14 Points
     */
    public void testIDClass001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                               Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("IDClassTestLogic.testIDClass001(): Missing context and/or resources.  Cannot execute the test.");
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
        IDClassEntityEnum targetEntityType = IDClassEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("IDClassTestLogic.testIDClass001(): Begin");

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
            IIdClassEntity new_entity = (IIdClassEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(standardId);
            new_entity.setCountry(standardCountry);
            new_entity.setIntVal(4096);

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Verifying saved data...");

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + "...");
            PKey pkey = new PKey(standardId, standardCountry);
            IIdClassEntity find_entity1 = (IIdClassEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), pkey);
            System.out.println("Object returned by find: " + find_entity1);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity1);
            Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity1);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity1));
            Assert.assertEquals("Assert that the entity's id is " + standardId, find_entity1.getId(), standardId);
            Assert.assertEquals("Assert that the entity's country is " + standardCountry, find_entity1.getCountry(), standardCountry);
            Assert.assertEquals("Assert intVal field matches expected value.", new_entity.getIntVal(), find_entity1.getIntVal());

            System.out.println("Updating field data and persisting to database...");
            find_entity1.setIntVal(8192);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Verifying updated data...");

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + "...");
            IIdClassEntity find_entity2 = (IIdClassEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), pkey);
            System.out.println("Object returned by find: " + find_entity2);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity2);
            Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity2);
            Assert.assertNotSame("Assert find did not return the object from the first find", find_entity1, find_entity2);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity2));
            Assert.assertEquals("Assert that the entity's id is " + standardId, find_entity2.getId(), standardId);
            Assert.assertEquals("Assert that the entity's country is " + standardCountry, find_entity2.getCountry(), standardCountry);
            Assert.assertEquals("Assert intVal field matches expected value.", find_entity1.getIntVal(), find_entity2.getIntVal());

            System.out.println("Removing entity...");
            jpaResource.getEm().remove(find_entity2);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Finding " + targetEntityType.getEntityName() + "...");
            IIdClassEntity find_entity3 = (IIdClassEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), pkey);
            System.out.println("Object returned by find: " + find_entity3);

            Assert.assertNull("Assert that the find operation did return null", find_entity3);

            System.out.println("Ending test.");
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("IDClassTestLogic.testIDClass001(): End");
        }
    }
}
