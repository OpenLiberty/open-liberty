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

import com.ibm.ws.jpa.fvt.entity.entities.IEmbeddedObjectEntity;
import com.ibm.ws.jpa.fvt.entity.entities.embeddable.SimpleEmbeddableObject;
import com.ibm.ws.jpa.fvt.entity.testlogic.enums.EmbeddableEntityEnum;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class EmbeddableTestLogic extends AbstractTestLogic {
    private int pkey_id = 1;

    private int initial_localIntVal = 4096;
    private String initial_localStrVal = "Latveria";

    private int postupdate_localIntVal = 9000;
    private String postupdate_localStrVal = "Elbonia";

    // Initial embeddable object values
    private boolean initial_embeddable_booleanVal = false;
    private byte initial_embeddable_byteVal = (byte) 10;
    private char initial_embeddable_charVal = 'j';
    private float initial_embeddable_floatVal = (float) 3.14;
    private double initial_embeddable_doubleVal = 4.0;
    private int initial_embeddable_intVal = 32768;
    private long initial_embeddable_longVal = 16384;
    private short initial_embeddable_shortVal = 42;
    private String initial_embeddable_stringVal = "I am a string";

    // Post-Update embeddable object values
    private boolean postupdate_embeddable_booleanVal = true;
    private byte postupdate_embeddable_byteVal = (byte) 20;
    private char postupdate_embeddable_charVal = 'J';
    private float postupdate_embeddable_floatVal = (float) 20.1;
    private double postupdate_embeddable_doubleVal = 16.4;
    private int postupdate_embeddable_intVal = 65535;
    private long postupdate_embeddable_longVal = 65000000;
    private short postupdate_embeddable_shortVal = 24;
    private String postupdate_embeddable_stringVal = "I am a changed string";

    /*
     * 34 Points
     */
    public void testEmbeddable001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("EmbeddableTestLogic.testEmbeddable001(): Missing context and/or resources.  Cannot execute the test.");
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
        EmbeddableEntityEnum targetEntityType = EmbeddableEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("EmbeddableTestLogic.testEmbeddable001(): Begin");

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
            IEmbeddedObjectEntity new_entity = (IEmbeddedObjectEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(pkey_id);

            // Populate its local persistable fields
            System.out.println("Populating entity's local persistable fields...");
            new_entity.setLocalIntVal(initial_localIntVal);
            new_entity.setLocalStrVal(initial_localStrVal);

            // Populate the embedded object
            System.out.println("Populating entity's embedded object's persistable fields...");
            SimpleEmbeddableObject seo_new = new_entity.getEmbeddedObj();
            seo_new.setBooleanVal(initial_embeddable_booleanVal);
            seo_new.setByteVal(initial_embeddable_byteVal);
            seo_new.setCharVal(initial_embeddable_charVal);
            seo_new.setDoubleVal(initial_embeddable_doubleVal);
            seo_new.setFloatVal(initial_embeddable_floatVal);
            seo_new.setIntVal(initial_embeddable_intVal);
            seo_new.setLongVal(initial_embeddable_longVal);
            seo_new.setShortVal(initial_embeddable_shortVal);
            seo_new.setStringVal(initial_embeddable_stringVal);

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

            {
                // Verify the stored entity data (17 Points)
                System.out.println("Verify the stored entity data.");
                System.out.println("Finding " + targetEntityType.getEntityName() + "...");
                IEmbeddedObjectEntity find_entity1 = (IEmbeddedObjectEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), pkey_id);
                System.out.println("Object returned by find: " + find_entity1);

                Assert.assertNotNull("Assert that the find operation did not return null", find_entity1);
                Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity1);
                Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity1));
                Assert.assertEquals("Assert that the entity's id is " + pkey_id, find_entity1.getId(), pkey_id);
                Assert.assertEquals("Assert that the entity's localIntVal field is " + initial_localIntVal, find_entity1.getLocalIntVal(), initial_localIntVal);
                Assert.assertEquals("Assert that the entity's localStringVal field is " + initial_localStrVal, find_entity1.getLocalStrVal(), initial_localStrVal);

                System.out.println("Verifying Embeddable contents...");

                SimpleEmbeddableObject seo_find = find_entity1.getEmbeddedObj();
                Assert.assertNotNull("Asset getter method for embeddable object did not return null.", seo_find);
                Assert.assertNotSame("Assert that the Embeddable object returned by find_entity1 is not the same as the original.", seo_new, seo_find);

                Assert.assertEquals("Assert embeddable field booleanVal == " + initial_embeddable_booleanVal, initial_embeddable_booleanVal, seo_find.isBooleanVal());
                Assert.assertEquals("Assert embeddable field byteVal == " + initial_embeddable_byteVal, initial_embeddable_byteVal, seo_find.getByteVal());
                Assert.assertEquals("Assert embeddable field charVal == " + initial_embeddable_charVal, initial_embeddable_charVal, seo_find.getCharVal());
                Assert.assertEquals("Assert embeddable field doubleVal == " + initial_embeddable_doubleVal, initial_embeddable_doubleVal, seo_find.getDoubleVal(), 0.1);
                Assert.assertEquals("Assert embeddable field floatVal == " + initial_embeddable_floatVal, initial_embeddable_floatVal, seo_find.getFloatVal(), 0.1);
                Assert.assertEquals("Assert embeddable field intVal == " + initial_embeddable_intVal, initial_embeddable_intVal, seo_find.getIntVal());
                Assert.assertEquals("Assert embeddable field longVal == " + initial_embeddable_longVal, initial_embeddable_longVal, seo_find.getLongVal());
                Assert.assertEquals("Assert embeddable field shortVal == " + initial_embeddable_shortVal, initial_embeddable_shortVal, seo_find.getShortVal());
                Assert.assertEquals("Assert embeddable field stringVal == " + initial_embeddable_stringVal, initial_embeddable_stringVal, seo_find.getStringVal());

                // Verified that the initial values of the new entity were saved to the database.
                // Verify that they can be changed and those changes  are saved to the database.
                System.out.println("Updating the values of the entity and its embedded object...");

                // Update its local persistable fields
                System.out.println("Updating entity's local persistable fields...");
                find_entity1.setLocalIntVal(postupdate_localIntVal);
                find_entity1.setLocalStrVal(postupdate_localStrVal);

                // Update the embedded object
                System.out.println("Updating entity's embedded object's persistable fields...");
                seo_find.setBooleanVal(postupdate_embeddable_booleanVal);
                seo_find.setByteVal(postupdate_embeddable_byteVal);
                seo_find.setCharVal(postupdate_embeddable_charVal);
                seo_find.setDoubleVal(postupdate_embeddable_doubleVal);
                seo_find.setFloatVal(postupdate_embeddable_floatVal);
                seo_find.setIntVal(postupdate_embeddable_intVal);
                seo_find.setLongVal(postupdate_embeddable_longVal);
                seo_find.setShortVal(postupdate_embeddable_shortVal);
                seo_find.setStringVal(postupdate_embeddable_stringVal);

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();
            }

            {
                // Now verify that the changes to the entity were saved correctly to the database.  (17 Points)
                System.out.println("Verifying that the entity changes were saved to the database correctly...");
                System.out.println("Finding " + targetEntityType.getEntityName() + "...");
                IEmbeddedObjectEntity find_entity2 = (IEmbeddedObjectEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), pkey_id);
                System.out.println("Object returned by find: " + find_entity2);

                Assert.assertNotNull("Assert that the find operation did not return null", find_entity2);
                Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity2);
                Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity2));
                Assert.assertEquals("Assert that the entity's id is " + pkey_id, find_entity2.getId(), pkey_id);
                Assert.assertEquals("Assert that the entity's localIntVal field is " + postupdate_localIntVal, find_entity2.getLocalIntVal(), postupdate_localIntVal);
                Assert.assertEquals("Assert that the entity's localStringVal field is " + postupdate_localStrVal, find_entity2.getLocalStrVal(), postupdate_localStrVal);

                System.out.println("Verifying Embeddable contents...");

                SimpleEmbeddableObject seo_find = find_entity2.getEmbeddedObj();
                Assert.assertNotNull("Asset getter method for embeddable object did not return null.", seo_find);
                Assert.assertNotSame("Assert that the Embeddable object returned by find_entity1 is not the same as the original.", seo_new, seo_find);

                Assert.assertEquals("Assert embeddable field booleanVal == " + postupdate_embeddable_booleanVal, postupdate_embeddable_booleanVal, seo_find.isBooleanVal());
                Assert.assertEquals("Assert embeddable field byteVal == " + postupdate_embeddable_byteVal, postupdate_embeddable_byteVal, seo_find.getByteVal());
                Assert.assertEquals("Assert embeddable field charVal == " + postupdate_embeddable_charVal, postupdate_embeddable_charVal, seo_find.getCharVal());
                Assert.assertEquals("Assert embeddable field doubleVal == " + postupdate_embeddable_doubleVal, postupdate_embeddable_doubleVal, seo_find.getDoubleVal(), 0.1);
                Assert.assertEquals("Assert embeddable field floatVal == " + postupdate_embeddable_floatVal, postupdate_embeddable_floatVal, seo_find.getFloatVal(), 0.1);
                Assert.assertEquals("Assert embeddable field intVal == " + postupdate_embeddable_intVal, postupdate_embeddable_intVal, seo_find.getIntVal());
                Assert.assertEquals("Assert embeddable field longVal == " + postupdate_embeddable_longVal, postupdate_embeddable_longVal, seo_find.getLongVal());
                Assert.assertEquals("Assert embeddable field shortVal == " + postupdate_embeddable_shortVal, postupdate_embeddable_shortVal, seo_find.getShortVal());
                Assert.assertEquals("Assert embeddable field stringVal == " + postupdate_embeddable_stringVal, postupdate_embeddable_stringVal, seo_find.getStringVal());
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
            IEmbeddedObjectEntity find_remove_entity = (IEmbeddedObjectEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), pkey_id);
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
            System.out.println("EmbeddableTestLogic.testEmbeddable001(): End");
        }
    }
}
