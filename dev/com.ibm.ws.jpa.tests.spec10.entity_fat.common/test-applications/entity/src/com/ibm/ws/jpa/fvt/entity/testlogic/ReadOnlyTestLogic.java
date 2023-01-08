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

import com.ibm.ws.jpa.fvt.entity.entities.IReadOnlyEntity;
import com.ibm.ws.jpa.fvt.entity.testlogic.enums.ReadOnlyEntityEnum;
import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class ReadOnlyTestLogic extends AbstractTestLogic {
    private int pkey_id = 1;
    private int orig_intVal = 42;
    private int orig_noInsertIntVal = 100;
    private int orig_noUpdatableIntVal = 200;
    private int orig_readOnlyIntVal = 400;

    private int update_intVal = 84;
    private int update_noInsertIntVal = 200;
    private int update_noUpdatableIntVal = 400;
    private int update_readOnlyIntVal = 800;

    /**
     * 22 Points
     */
    public void testReadOnly001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("ReadOnlyTestLogic.testReadOnly001(): Missing context and/or resources.  Cannot execute the test.");
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
        ReadOnlyEntityEnum targetEntityType = ReadOnlyEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        // TODO: Hibernate does not support "insertable=false" for primitive types as they try to populate NULL values on em.find()
        JPAPersistenceProvider provider = JPAPersistenceProvider.resolveJPAPersistenceProvider(jpaResource);
        if (JPAPersistenceProvider.HIBERNATE.equals(provider)) {
            return;
        }

        // Execute Test Case
        try {
            System.out.println("ReadOnlyTestLogic.testReadOnly001(): Begin");

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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=" + pkey_id + ")...");
            IReadOnlyEntity new_entity = (IReadOnlyEntity) constructNewEntityObject(targetEntityType);

            StringBuffer sb = new StringBuffer();
            sb.append("Initializing Fields:\n");
            sb.append("  id =").append(pkey_id).append('\n');
            sb.append("  intVal =").append(orig_intVal).append('\n');
            sb.append("  noInsertIntVal =").append(orig_noInsertIntVal).append('\n');
            sb.append("  noUpdatableIntVal =").append(orig_noUpdatableIntVal).append('\n');
            sb.append("  readOnlyIntVal =").append(orig_readOnlyIntVal).append('\n');
            System.out.println(new String(sb));

            new_entity.setId(pkey_id);
            new_entity.setIntVal(orig_intVal);
            new_entity.setNoInsertIntVal(orig_noInsertIntVal);
            new_entity.setNoUpdatableIntVal(orig_noUpdatableIntVal);
            new_entity.setReadOnlyIntVal(orig_readOnlyIntVal);

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("The object instance should contain all the new values, since JPA does not guerentee the in-memory " +
                               "state of the entity object.");
            {
                // 5 Points
                int expectedIntVal = orig_intVal;
                int expectedNoInsertIntVal = orig_noInsertIntVal;
                int expectedNopdatableIntVal = orig_noUpdatableIntVal;
                int expectedReadOnlyIntVal = orig_readOnlyIntVal;
                Assert.assertEquals("Assert that the entity's id is " + pkey_id,
                                    new_entity.getId(),
                                    pkey_id);

                // Test Persistable Values
                Assert.assertEquals("Assert intVal == " + expectedIntVal, expectedIntVal, new_entity.getIntVal());
                Assert.assertEquals("Assert NoInsertIntVal == " + expectedIntVal, expectedNoInsertIntVal, new_entity.getNoInsertIntVal());
                Assert.assertEquals("Assert NopdatableIntVal == " + expectedIntVal, expectedNopdatableIntVal, new_entity.getNoUpdatableIntVal());
                Assert.assertEquals("Assert ReadOnlyIntVal == " + expectedIntVal, expectedReadOnlyIntVal, new_entity.getReadOnlyIntVal());
            }

            System.out.println("Load the entity from the database and test its persistent values.");
            {
                // 8 Points
                int expectedIntVal = orig_intVal;
                int expectedNoInsertIntVal = 0;
                int expectedNopdatableIntVal = orig_noUpdatableIntVal;
                int expectedReadOnlyIntVal = 0;

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Fetch Entity
                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }
                System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + pkey_id + ")...");
                IReadOnlyEntity find_entity1 = (IReadOnlyEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), pkey_id);
                jpaResource.getEm().refresh(find_entity1); // Deals with datacache if enabled to force DB fetch
                System.out.println("Object returned by find: " + find_entity1);

                Assert.assertNotNull("Assert that the find operation did not return null", find_entity1);
                Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity1);
                Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity1));
                Assert.assertEquals("Assert that the entity's id is " + pkey_id, find_entity1.getId(), pkey_id);

                // Test Persistable Values
                Assert.assertEquals("Assert intVal == " + expectedIntVal, expectedIntVal, find_entity1.getIntVal());
                Assert.assertEquals("Assert NoInsertIntVal == " + expectedIntVal, expectedNoInsertIntVal, find_entity1.getNoInsertIntVal());
                Assert.assertEquals("Assert NopdatableIntVal == " + expectedIntVal, expectedNopdatableIntVal, find_entity1.getNoUpdatableIntVal());
                Assert.assertEquals("Assert ReadOnlyIntVal == " + expectedIntVal, expectedReadOnlyIntVal, find_entity1.getReadOnlyIntVal());

                System.out.println("Rolling Back transaction...");
                jpaResource.getTj().rollbackTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();
            }

            System.out.println("Mutate the entity...");
            {
                // 1 Point
                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + pkey_id + ")...");
                IReadOnlyEntity find_entity1 = (IReadOnlyEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), pkey_id);
                jpaResource.getEm().refresh(find_entity1); // Deals with datacache if enabled to force DB fetch
                System.out.println("Object returned by find: " + find_entity1);

                Assert.assertNotNull("Assert that the find operation did not return null", find_entity1);

                sb.setLength(0);
                sb.append("Updating Fields:\n");
                sb.append("  intVal =").append(orig_intVal).append('\n');
                sb.append("  noInsertIntVal =").append(orig_noInsertIntVal).append('\n');
                sb.append("  noUpdatableIntVal =").append(orig_noUpdatableIntVal).append('\n');
                sb.append("  readOnlyIntVal =").append(orig_readOnlyIntVal).append('\n');
                System.out.println(new String(sb));

                find_entity1.setId(pkey_id);
                find_entity1.setIntVal(update_intVal);
                find_entity1.setNoInsertIntVal(update_noInsertIntVal);
                find_entity1.setNoUpdatableIntVal(update_noUpdatableIntVal);
                find_entity1.setReadOnlyIntVal(update_readOnlyIntVal);

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();
            }

            System.out.println("Load the entity from the database and test its persistent values.");
            {
                // 8 Points
                int expectedIntVal = update_intVal;
                int expectedNoInsertIntVal = update_noInsertIntVal;
                int expectedNopdatableIntVal = orig_noUpdatableIntVal;
                int expectedReadOnlyIntVal = 0;

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();

                // Fetch Entity
                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }
                System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + pkey_id + ")...");
                IReadOnlyEntity find_entity1 = (IReadOnlyEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), pkey_id);
                jpaResource.getEm().refresh(find_entity1); // Deals with datacache if enabled to force DB fetch
                System.out.println("Object returned by find: " + find_entity1);

                Assert.assertNotNull("Assert that the find operation did not return null", find_entity1);
                Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity1);
                Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity1));
                Assert.assertEquals("Assert that the entity's id is " + pkey_id, find_entity1.getId(), pkey_id);

                // Test Persistable Values
                Assert.assertEquals("Assert intVal == " + expectedIntVal, expectedIntVal, find_entity1.getIntVal());
                Assert.assertEquals("Assert NoInsertIntVal == " + expectedIntVal, expectedNoInsertIntVal, find_entity1.getNoInsertIntVal());
                Assert.assertEquals("Assert NopdatableIntVal == " + expectedIntVal, expectedNopdatableIntVal, find_entity1.getNoUpdatableIntVal());
                Assert.assertEquals("Assert ReadOnlyIntVal == " + expectedIntVal, expectedReadOnlyIntVal, find_entity1.getReadOnlyIntVal());

                System.out.println("Rolling Back transaction...");
                jpaResource.getTj().rollbackTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();
            }

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + pkey_id + ")...");
            IReadOnlyEntity find_remove_entity = (IReadOnlyEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), pkey_id);
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
            System.out.println("ReadOnlyTestLogic.testReadOnly001(): End");
        }
    }
}
