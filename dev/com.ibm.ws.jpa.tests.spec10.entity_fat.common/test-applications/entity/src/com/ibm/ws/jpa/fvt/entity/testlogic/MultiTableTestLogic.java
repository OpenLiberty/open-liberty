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

import com.ibm.ws.jpa.fvt.entity.entities.IMultiTableEntity;
import com.ibm.ws.jpa.fvt.entity.testlogic.enums.MultiTableEntityEnum;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class MultiTableTestLogic extends AbstractTestLogic {

    /*
     * 18 Points
     *
     */
    public void testMultiTable001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("MultiTableTestLogic.testMultiTable001(): Missing context and/or resources.  Cannot execute the test.");
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
        MultiTableEntityEnum targetEntityType = MultiTableEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        int id = 1;

        // Execute Test Case
        try {
            System.out.println("MultiTableTestLogic.testMultiTable001(): Begin");

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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IMultiTableEntity new_entity = (IMultiTableEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(id);
            new_entity.setName("Dr. Doom");
            new_entity.setStreet("1024 Main Street");
            new_entity.setCity("Latverian Capital");
            new_entity.setState("Latveria");
            new_entity.setZip("12345");

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

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IMultiTableEntity find_entity1 = (IMultiTableEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_entity1);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity1);
            Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity1);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity1));
            Assert.assertEquals("Assert that the entity's id is " + id, find_entity1.getId(), id);

            Assert.assertEquals("Assert name field matches expected value.", new_entity.getName(), find_entity1.getName());
            Assert.assertEquals("Assert street field matches expected value.", new_entity.getStreet(), find_entity1.getStreet());
            Assert.assertEquals("Assert city field matches expected value.", new_entity.getCity(), find_entity1.getCity());
            Assert.assertEquals("Assert state field matches expected value.", new_entity.getState(), find_entity1.getState());
            Assert.assertEquals("Assert zip field matches expected value.", new_entity.getZip(), find_entity1.getZip());

            System.out.println("Updating field data and persisting to database...");
            find_entity1.setName("Soy Green");
            find_entity1.setStreet("Soylent Green Street");
            find_entity1.setCity("Soylentopolis");
            find_entity1.setState("SGLand");
            find_entity1.setZip("67890");

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Verifying modified data...");

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IMultiTableEntity find_entity2 = (IMultiTableEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_entity2);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity2);
            Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity2);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity2));
            Assert.assertEquals("Assert that the entity's id is " + id, find_entity2.getId(), id);

            Assert.assertEquals("Assert name field matches expected value.", find_entity1.getName(), find_entity2.getName());
            Assert.assertEquals("Assert street field matches expected value.", find_entity1.getStreet(), find_entity2.getStreet());
            Assert.assertEquals("Assert city field matches expected value.", find_entity1.getCity(), find_entity2.getCity());
            Assert.assertEquals("Assert state field matches expected value.", find_entity1.getState(), find_entity2.getState());
            Assert.assertEquals("Assert zip field matches expected value.", find_entity1.getZip(), find_entity2.getZip());

            System.out.println("Rolling back the transaction...");
            jpaResource.getTj().rollbackTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IMultiTableEntity find_remove_entity = (IMultiTableEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
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
            System.out.println("MultiTableTestLogic.testMultiTable001(): End");
        }
    }

}
