/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.embeddable.nested.testlogic;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.persistence.EntityManager;

import org.junit.Assert;

import com.ibm.ws.jpa.embeddable.nested.model.Embeddable01;
import com.ibm.ws.jpa.embeddable.nested.model.Embeddable04a;
import com.ibm.ws.jpa.embeddable.nested.model.Embeddable11;
import com.ibm.ws.jpa.embeddable.nested.model.Entity01;
import com.ibm.ws.jpa.embeddable.nested.model.Entity02;
import com.ibm.ws.jpa.embeddable.nested.model.Entity04;
import com.ibm.ws.jpa.embeddable.nested.model.Entity06;
import com.ibm.ws.jpa.embeddable.nested.model.Entity07;
import com.ibm.ws.jpa.embeddable.nested.model.Entity08;
import com.ibm.ws.jpa.embeddable.nested.model.Entity09;
import com.ibm.ws.jpa.embeddable.nested.model.Entity10;
import com.ibm.ws.jpa.embeddable.nested.model.Entity11;
import com.ibm.ws.jpa.embeddable.nested.model.IEntity01;
import com.ibm.ws.jpa.embeddable.nested.model.IEntity02;
import com.ibm.ws.jpa.embeddable.nested.model.IEntity04;
import com.ibm.ws.jpa.embeddable.nested.model.IEntity06;
import com.ibm.ws.jpa.embeddable.nested.model.IEntity07;
import com.ibm.ws.jpa.embeddable.nested.model.IEntity08;
import com.ibm.ws.jpa.embeddable.nested.model.IEntity09;
import com.ibm.ws.jpa.embeddable.nested.model.IEntity10;
import com.ibm.ws.jpa.embeddable.nested.model.IEntity11;
import com.ibm.ws.jpa.embeddable.nested.model.XMLEntity01;
import com.ibm.ws.jpa.embeddable.nested.model.XMLEntity02;
import com.ibm.ws.jpa.embeddable.nested.model.XMLEntity04;
import com.ibm.ws.jpa.embeddable.nested.model.XMLEntity07;
import com.ibm.ws.jpa.embeddable.nested.model.XMLEntity08;
import com.ibm.ws.jpa.embeddable.nested.model.XMLEntity09;
import com.ibm.ws.jpa.embeddable.nested.model.XMLEntity10;
import com.ibm.ws.jpa.embeddable.nested.model.XMLEntity11;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

/**
 * Testing logic to validate the following specification section(s):
 *
 *
 * 11.1.12 ElementCollection Annotation
 *
 * 1. Portable applications should not expect the order of a list to be maintained across
 * persistence contexts unless the OrderColumn construct is used or unless the OrderBy
 * construct is used and the modifications to the list observe the specified ordering.
 *
 *
 * 11.1.38 OrderBy Annotation
 *
 * 1. A property or field name specified as an orderby_item must correspond to a basic persistent
 * property or field of the associated class or embedded class within it. The properties or
 * fields used in the ordering must correspond to columns for which comparison operators are
 * supported.
 *
 * 2. The dot (".") notation is used to refer to an attribute within an embedded attribute. The
 * value of each identifier used with the dot notation is the name of the respective embedded
 * field or property.
 *
 * 3. The OrderBy annotation may be applied to an element collection. When OrderBy is applied to
 * an element collection of basic type, the ordering will be by value of the basic objects and
 * the property_or_field_name is not used. When specifying an ordering over an element
 * collection of embeddable type, the dot notation must be used to specify the attribute or
 * attributes that determine the ordering.
 *
 * 4. The OrderBy annotation is not used when an order column is specified. See section 11.1.39.
 *
 */
public class EmbeddableNestedLogic extends AbstractTestLogic {

    /**
     * Test Logic: testScenario01
     *
     * Description: Test of single entity with single embeddable
     *
     * Performs these basic CRUD operations:
     *
     * 01. Create a new instance of the entity class
     * 02. Persist the new entity in the database
     * 03. Verify the entity was saved in the database
     * 04. Update the entity
     * 05. Verify the updated entity was saved in the database
     * 06. Delete the entity from the database
     * 07. Verify the deletion was successful
     *
     * UML:
     * +----------------+
     * | Entity01 |
     * +-------+--------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +-------+--------+
     * | Embeddable01 |
     * +----------------+
     *
     * @throws Exception
     *
     */
    public void testEmbeddableNested01(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 1;

            IEntity01 newEntity = new Entity01();
            newEntity.setId(id);
            newEntity.setEnt01_str01("string01 DATA");
            newEntity.setEnt01_str02("string02 DATA");
            newEntity.setEnt01_str03("string03 DATA");
            newEntity.setEmb01_int01(1);
            newEntity.setEmb01_int02(2);
            newEntity.setEmb01_int03(3);

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing persist(" + newEntity + ") operation");
            em.persist(newEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity01 findEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(em.contains(findEntity));
            Assert.assertEquals(findEntity.getId(), id);
            Assert.assertEquals(findEntity.getEnt01_str01(), "string01 DATA");
            Assert.assertEquals(findEntity.getEnt01_str02(), "string02 DATA");
            Assert.assertEquals(findEntity.getEnt01_str03(), "string03 DATA");
            Assert.assertEquals(findEntity.getEmb01_int01(), 1);
            Assert.assertEquals(findEntity.getEmb01_int02(), 2);
            Assert.assertEquals(findEntity.getEmb01_int03(), 3);

            // Update the entity
            findEntity.setEnt01_str01("UPDATED string01 DATA");
            findEntity.setEnt01_str02("UPDATED string02 DATA");
            findEntity.setEnt01_str03("UPDATED string03 DATA");
            findEntity.setEmb01_int01(4);
            findEntity.setEmb01_int02(5);
            findEntity.setEmb01_int03(6);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity update was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity01 updatedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", updatedEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedEntity);
            Assert.assertTrue(findEntity != updatedEntity);
            Assert.assertTrue(em.contains(updatedEntity));
            Assert.assertEquals(updatedEntity.getId(), id);
            Assert.assertEquals(updatedEntity.getEnt01_str01(), "UPDATED string01 DATA");
            Assert.assertEquals(updatedEntity.getEnt01_str02(), "UPDATED string02 DATA");
            Assert.assertEquals(updatedEntity.getEnt01_str03(), "UPDATED string03 DATA");
            Assert.assertEquals(updatedEntity.getEmb01_int01(), 4);
            Assert.assertEquals(updatedEntity.getEmb01_int02(), 5);
            Assert.assertEquals(updatedEntity.getEmb01_int03(), 6);

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedEntity + ") operation");
            em.remove(updatedEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            Object findRemovedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNull("find(" + newEntity.getClass() + ", " + id + ") returned an entity.", findRemovedEntity);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testEmbeddableNested02(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 1;

            IEntity01 newEntity = new XMLEntity01();
            newEntity.setId(id);
            newEntity.setEnt01_str01("string01 DATA");
            newEntity.setEnt01_str02("string02 DATA");
            newEntity.setEnt01_str03("string03 DATA");
            newEntity.setEmb01_int01(1);
            newEntity.setEmb01_int02(2);
            newEntity.setEmb01_int03(3);

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing persist(" + newEntity + ") operation");
            em.persist(newEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity01 findEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(em.contains(findEntity));
            Assert.assertEquals(findEntity.getId(), id);
            Assert.assertEquals(findEntity.getEnt01_str01(), "string01 DATA");
            Assert.assertEquals(findEntity.getEnt01_str02(), "string02 DATA");
            Assert.assertEquals(findEntity.getEnt01_str03(), "string03 DATA");
            Assert.assertEquals(findEntity.getEmb01_int01(), 1);
            Assert.assertEquals(findEntity.getEmb01_int02(), 2);
            Assert.assertEquals(findEntity.getEmb01_int03(), 3);

            // Update the entity
            findEntity.setEnt01_str01("UPDATED string01 DATA");
            findEntity.setEnt01_str02("UPDATED string02 DATA");
            findEntity.setEnt01_str03("UPDATED string03 DATA");
            findEntity.setEmb01_int01(4);
            findEntity.setEmb01_int02(5);
            findEntity.setEmb01_int03(6);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity update was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity01 updatedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", updatedEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedEntity);
            Assert.assertTrue(findEntity != updatedEntity);
            Assert.assertTrue(em.contains(updatedEntity));
            Assert.assertEquals(updatedEntity.getId(), id);
            Assert.assertEquals(updatedEntity.getEnt01_str01(), "UPDATED string01 DATA");
            Assert.assertEquals(updatedEntity.getEnt01_str02(), "UPDATED string02 DATA");
            Assert.assertEquals(updatedEntity.getEnt01_str03(), "UPDATED string03 DATA");
            Assert.assertEquals(updatedEntity.getEmb01_int01(), 4);
            Assert.assertEquals(updatedEntity.getEmb01_int02(), 5);
            Assert.assertEquals(updatedEntity.getEmb01_int03(), 6);

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedEntity + ") operation");
            em.remove(updatedEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            Object findRemovedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNull("find(" + newEntity.getClass() + ", " + id + ") returned an entity.", findRemovedEntity);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Test Logic: testScenario02
     *
     * Description: Test of single entity with nested embeddables
     *
     * Performs these basic CRUD operations:
     *
     * 01. Create a new instance of the entity class
     * 02. Persist the new entity in the database
     * 03. Verify the entity was saved in the database
     * 04. Update the entity
     * 05. Verify the updated entity was saved in the database
     * 06. Delete the entity from the database
     * 07. Verify the deletion was successful
     *
     * UML:
     * +-----------------+
     * | Entity02 |
     * +--------+--------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +--------+--------+
     * | Embeddable02a |
     * +--------+--------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +--------+--------+
     * | Embeddable02b |
     * +-----------------+
     *
     */
    public void testEmbeddableNested03(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 2;

            IEntity02 newEntity = new Entity02();
            newEntity.setId(id);
            newEntity.setEnt02_str01("string01 DATA");
            newEntity.setEnt02_str02("string02 DATA");
            newEntity.setEnt02_str03("string03 DATA");
            newEntity.setEmb02a_int01(1);
            newEntity.setEmb02a_int02(2);
            newEntity.setEmb02a_int03(3);
            newEntity.setEmb02b_int04(4);
            newEntity.setEmb02b_int05(5);
            newEntity.setEmb02b_int06(6);

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing persist(" + newEntity + ") operation");
            em.persist(newEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity02 findEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(em.contains(findEntity));
            Assert.assertEquals(findEntity.getId(), id);
            Assert.assertEquals(findEntity.getEnt02_str01(), "string01 DATA");
            Assert.assertEquals(findEntity.getEnt02_str02(), "string02 DATA");
            Assert.assertEquals(findEntity.getEnt02_str03(), "string03 DATA");
            Assert.assertEquals(findEntity.getEmb02a_int01(), 1);
            Assert.assertEquals(findEntity.getEmb02a_int02(), 2);
            Assert.assertEquals(findEntity.getEmb02a_int03(), 3);
            Assert.assertEquals(findEntity.getEmb02b_int04(), 4);
            Assert.assertEquals(findEntity.getEmb02b_int05(), 5);
            Assert.assertEquals(findEntity.getEmb02b_int06(), 6);

            // Update the entity
            findEntity.setEnt02_str01("UPDATED string01 DATA");
            findEntity.setEnt02_str02("UPDATED string02 DATA");
            findEntity.setEnt02_str03("UPDATED string03 DATA");
            findEntity.setEmb02a_int01(7);
            findEntity.setEmb02a_int02(8);
            findEntity.setEmb02a_int03(9);
            findEntity.setEmb02b_int04(10);
            findEntity.setEmb02b_int05(11);
            findEntity.setEmb02b_int06(12);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity update was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity02 updatedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", updatedEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedEntity);
            Assert.assertTrue(findEntity != updatedEntity);
            Assert.assertTrue(em.contains(updatedEntity));
            Assert.assertEquals(updatedEntity.getId(), id);
            Assert.assertEquals(updatedEntity.getEnt02_str01(), "UPDATED string01 DATA");
            Assert.assertEquals(updatedEntity.getEnt02_str02(), "UPDATED string02 DATA");
            Assert.assertEquals(updatedEntity.getEnt02_str03(), "UPDATED string03 DATA");
            Assert.assertEquals(updatedEntity.getEmb02a_int01(), 7);
            Assert.assertEquals(updatedEntity.getEmb02a_int02(), 8);
            Assert.assertEquals(updatedEntity.getEmb02a_int03(), 9);
            Assert.assertEquals(updatedEntity.getEmb02b_int04(), 10);
            Assert.assertEquals(updatedEntity.getEmb02b_int05(), 11);
            Assert.assertEquals(updatedEntity.getEmb02b_int06(), 12);

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedEntity + ") operation");
            em.remove(updatedEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            Object findRemovedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNull("find(" + newEntity.getClass() + ", " + id + ") returned an entity.", findRemovedEntity);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testEmbeddableNested04(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 2;

            IEntity02 newEntity = new XMLEntity02();
            newEntity.setId(id);
            newEntity.setEnt02_str01("string01 DATA");
            newEntity.setEnt02_str02("string02 DATA");
            newEntity.setEnt02_str03("string03 DATA");
            newEntity.setEmb02a_int01(1);
            newEntity.setEmb02a_int02(2);
            newEntity.setEmb02a_int03(3);
            newEntity.setEmb02b_int04(4);
            newEntity.setEmb02b_int05(5);
            newEntity.setEmb02b_int06(6);

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing persist(" + newEntity + ") operation");
            em.persist(newEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity02 findEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(em.contains(findEntity));
            Assert.assertEquals(findEntity.getId(), id);
            Assert.assertEquals(findEntity.getEnt02_str01(), "string01 DATA");
            Assert.assertEquals(findEntity.getEnt02_str02(), "string02 DATA");
            Assert.assertEquals(findEntity.getEnt02_str03(), "string03 DATA");
            Assert.assertEquals(findEntity.getEmb02a_int01(), 1);
            Assert.assertEquals(findEntity.getEmb02a_int02(), 2);
            Assert.assertEquals(findEntity.getEmb02a_int03(), 3);
            Assert.assertEquals(findEntity.getEmb02b_int04(), 4);
            Assert.assertEquals(findEntity.getEmb02b_int05(), 5);
            Assert.assertEquals(findEntity.getEmb02b_int06(), 6);

            // Update the entity
            findEntity.setEnt02_str01("UPDATED string01 DATA");
            findEntity.setEnt02_str02("UPDATED string02 DATA");
            findEntity.setEnt02_str03("UPDATED string03 DATA");
            findEntity.setEmb02a_int01(7);
            findEntity.setEmb02a_int02(8);
            findEntity.setEmb02a_int03(9);
            findEntity.setEmb02b_int04(10);
            findEntity.setEmb02b_int05(11);
            findEntity.setEmb02b_int06(12);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity update was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity02 updatedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", updatedEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedEntity);
            Assert.assertTrue(findEntity != updatedEntity);
            Assert.assertTrue(em.contains(updatedEntity));
            Assert.assertEquals(updatedEntity.getId(), id);
            Assert.assertEquals(updatedEntity.getEnt02_str01(), "UPDATED string01 DATA");
            Assert.assertEquals(updatedEntity.getEnt02_str02(), "UPDATED string02 DATA");
            Assert.assertEquals(updatedEntity.getEnt02_str03(), "UPDATED string03 DATA");
            Assert.assertEquals(updatedEntity.getEmb02a_int01(), 7);
            Assert.assertEquals(updatedEntity.getEmb02a_int02(), 8);
            Assert.assertEquals(updatedEntity.getEmb02a_int03(), 9);
            Assert.assertEquals(updatedEntity.getEmb02b_int04(), 10);
            Assert.assertEquals(updatedEntity.getEmb02b_int05(), 11);
            Assert.assertEquals(updatedEntity.getEmb02b_int06(), 12);

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedEntity + ") operation");
            em.remove(updatedEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            Object findRemovedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNull("find(" + newEntity.getClass() + ", " + id + ") returned an entity.", findRemovedEntity);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Test Logic: testScenario04
     *
     * Description: Test of single entity with nested embeddables
     *
     * Performs these basic CRUD operations:
     *
     * 01. Create a new instance of the entity class
     * 02. Persist the new entity in the database
     * 03. Verify the entity was saved in the database
     * 04. Update the entity
     * 05. Verify the updated entity was saved in the database
     * 06. Delete the entity from the database
     * 07. Verify the deletion was successful
     *
     * UML:
     * +-----------------+
     * | Entity04 |
     * +-----------------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +-----------------+
     * | Embeddable04a |
     * +-----------------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +-----------------+
     * | Embeddable04b |
     * +-----------------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +-----------------+
     * | Embeddable04c |
     * +-----------------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +-----------------+
     * | Embeddable04d |
     * +-----------------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +-----------------+
     * | Embeddable04e |
     * +-----------------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +-----------------+
     * | Embeddable04f |
     * +-----------------+
     *
     */
    public void testEmbeddableNested05(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 4;

            IEntity04 newEntity = new Entity04();
            newEntity.setId(id);
            newEntity.setEnt04_str01("string01 DATA");
            newEntity.setEnt04_str02("string02 DATA");
            newEntity.setEnt04_str03("string03 DATA");
            newEntity.setEmb04a_int01(1);
            newEntity.setEmb04a_int02(2);
            newEntity.setEmb04a_int03(3);
            newEntity.setEmb04b_str04("string04 DATA");
            newEntity.setEmb04b_str05("string05 DATA");
            newEntity.setEmb04b_str06("string06 DATA");
            newEntity.setEmb04c_int07(4);
            newEntity.setEmb04c_int08(5);
            newEntity.setEmb04c_int09(6);
            newEntity.setEmb04d_str10("string07 DATA");
            newEntity.setEmb04d_str11("string08 DATA");
            newEntity.setEmb04d_str12("string09 DATA");
            newEntity.setEmb04e_int13(7);
            newEntity.setEmb04e_int14(8);
            newEntity.setEmb04e_int15(9);
            newEntity.setEmb04f_str16("string10 DATA");
            newEntity.setEmb04f_str17("string11 DATA");
            newEntity.setEmb04f_str18("string12 DATA");

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing persist(" + newEntity + ") operation");
            em.persist(newEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity04 findEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(em.contains(findEntity));
            Assert.assertEquals(findEntity.getId(), id);
            Assert.assertEquals(findEntity.getEnt04_str01(), "string01 DATA");
            Assert.assertEquals(findEntity.getEnt04_str02(), "string02 DATA");
            Assert.assertEquals(findEntity.getEnt04_str03(), "string03 DATA");
            Assert.assertEquals(findEntity.getEmb04a_int01(), 1);
            Assert.assertEquals(findEntity.getEmb04a_int02(), 2);
            Assert.assertEquals(findEntity.getEmb04a_int03(), 3);
            Assert.assertEquals(findEntity.getEmb04b_str04(), "string04 DATA");
            Assert.assertEquals(findEntity.getEmb04b_str05(), "string05 DATA");
            Assert.assertEquals(findEntity.getEmb04b_str06(), "string06 DATA");
            Assert.assertEquals(findEntity.getEmb04c_int07(), 4);
            Assert.assertEquals(findEntity.getEmb04c_int08(), 5);
            Assert.assertEquals(findEntity.getEmb04c_int09(), 6);
            Assert.assertEquals(findEntity.getEmb04d_str10(), "string07 DATA");
            Assert.assertEquals(findEntity.getEmb04d_str11(), "string08 DATA");
            Assert.assertEquals(findEntity.getEmb04d_str12(), "string09 DATA");
            Assert.assertEquals(findEntity.getEmb04e_int13(), 7);
            Assert.assertEquals(findEntity.getEmb04e_int14(), 8);
            Assert.assertEquals(findEntity.getEmb04e_int15(), 9);
            Assert.assertEquals(findEntity.getEmb04f_str16(), "string10 DATA");
            Assert.assertEquals(findEntity.getEmb04f_str17(), "string11 DATA");
            Assert.assertEquals(findEntity.getEmb04f_str18(), "string12 DATA");

            // Update the entity
            findEntity.setEnt04_str01("UPDATED string01 DATA");
            findEntity.setEnt04_str02("UPDATED string02 DATA");
            findEntity.setEnt04_str03("UPDATED string03 DATA");
            findEntity.setEmb04a_int01(10);
            findEntity.setEmb04a_int02(11);
            findEntity.setEmb04a_int03(12);
            findEntity.setEmb04b_str04("UPDATED string04 DATA");
            findEntity.setEmb04b_str05("UPDATED string05 DATA");
            findEntity.setEmb04b_str06("UPDATED string06 DATA");
            findEntity.setEmb04c_int07(13);
            findEntity.setEmb04c_int08(14);
            findEntity.setEmb04c_int09(15);
            findEntity.setEmb04d_str10("UPDATED string07 DATA");
            findEntity.setEmb04d_str11("UPDATED string08 DATA");
            findEntity.setEmb04d_str12("UPDATED string09 DATA");
            findEntity.setEmb04e_int13(16);
            findEntity.setEmb04e_int14(17);
            findEntity.setEmb04e_int15(18);
            findEntity.setEmb04f_str16("UPDATED string10 DATA");
            findEntity.setEmb04f_str17("UPDATED string11 DATA");
            findEntity.setEmb04f_str18("UPDATED string12 DATA");

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity update was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity04 updatedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", updatedEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedEntity);
            Assert.assertTrue(findEntity != updatedEntity);
            Assert.assertTrue(em.contains(updatedEntity));
            Assert.assertEquals(updatedEntity.getId(), id);
            Assert.assertEquals(updatedEntity.getEnt04_str01(), "UPDATED string01 DATA");
            Assert.assertEquals(updatedEntity.getEnt04_str02(), "UPDATED string02 DATA");
            Assert.assertEquals(updatedEntity.getEnt04_str03(), "UPDATED string03 DATA");
            Assert.assertEquals(updatedEntity.getEmb04a_int01(), 10);
            Assert.assertEquals(updatedEntity.getEmb04a_int02(), 11);
            Assert.assertEquals(updatedEntity.getEmb04a_int03(), 12);
            Assert.assertEquals(updatedEntity.getEmb04b_str04(), "UPDATED string04 DATA");
            Assert.assertEquals(updatedEntity.getEmb04b_str05(), "UPDATED string05 DATA");
            Assert.assertEquals(updatedEntity.getEmb04b_str06(), "UPDATED string06 DATA");
            Assert.assertEquals(updatedEntity.getEmb04c_int07(), 13);
            Assert.assertEquals(updatedEntity.getEmb04c_int08(), 14);
            Assert.assertEquals(updatedEntity.getEmb04c_int09(), 15);
            Assert.assertEquals(updatedEntity.getEmb04d_str10(), "UPDATED string07 DATA");
            Assert.assertEquals(updatedEntity.getEmb04d_str11(), "UPDATED string08 DATA");
            Assert.assertEquals(updatedEntity.getEmb04d_str12(), "UPDATED string09 DATA");
            Assert.assertEquals(updatedEntity.getEmb04e_int13(), 16);
            Assert.assertEquals(updatedEntity.getEmb04e_int14(), 17);
            Assert.assertEquals(updatedEntity.getEmb04e_int15(), 18);
            Assert.assertEquals(updatedEntity.getEmb04f_str16(), "UPDATED string10 DATA");
            Assert.assertEquals(updatedEntity.getEmb04f_str17(), "UPDATED string11 DATA");
            Assert.assertEquals(updatedEntity.getEmb04f_str18(), "UPDATED string12 DATA");

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedEntity + ") operation");
            em.remove(updatedEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            Object findRemovedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNull("find(" + newEntity.getClass() + ", " + id + ") returned an entity.", findRemovedEntity);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testEmbeddableNested06(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 4;

            IEntity04 newEntity = new XMLEntity04();
            newEntity.setId(id);
            newEntity.setEnt04_str01("string01 DATA");
            newEntity.setEnt04_str02("string02 DATA");
            newEntity.setEnt04_str03("string03 DATA");
            newEntity.setEmb04a_int01(1);
            newEntity.setEmb04a_int02(2);
            newEntity.setEmb04a_int03(3);
            newEntity.setEmb04b_str04("string04 DATA");
            newEntity.setEmb04b_str05("string05 DATA");
            newEntity.setEmb04b_str06("string06 DATA");
            newEntity.setEmb04c_int07(4);
            newEntity.setEmb04c_int08(5);
            newEntity.setEmb04c_int09(6);
            newEntity.setEmb04d_str10("string07 DATA");
            newEntity.setEmb04d_str11("string08 DATA");
            newEntity.setEmb04d_str12("string09 DATA");
            newEntity.setEmb04e_int13(7);
            newEntity.setEmb04e_int14(8);
            newEntity.setEmb04e_int15(9);
            newEntity.setEmb04f_str16("string10 DATA");
            newEntity.setEmb04f_str17("string11 DATA");
            newEntity.setEmb04f_str18("string12 DATA");

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing persist(" + newEntity + ") operation");
            em.persist(newEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity04 findEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(em.contains(findEntity));
            Assert.assertEquals(findEntity.getId(), id);
            Assert.assertEquals(findEntity.getEnt04_str01(), "string01 DATA");
            Assert.assertEquals(findEntity.getEnt04_str02(), "string02 DATA");
            Assert.assertEquals(findEntity.getEnt04_str03(), "string03 DATA");
            Assert.assertEquals(findEntity.getEmb04a_int01(), 1);
            Assert.assertEquals(findEntity.getEmb04a_int02(), 2);
            Assert.assertEquals(findEntity.getEmb04a_int03(), 3);
            Assert.assertEquals(findEntity.getEmb04b_str04(), "string04 DATA");
            Assert.assertEquals(findEntity.getEmb04b_str05(), "string05 DATA");
            Assert.assertEquals(findEntity.getEmb04b_str06(), "string06 DATA");
            Assert.assertEquals(findEntity.getEmb04c_int07(), 4);
            Assert.assertEquals(findEntity.getEmb04c_int08(), 5);
            Assert.assertEquals(findEntity.getEmb04c_int09(), 6);
            Assert.assertEquals(findEntity.getEmb04d_str10(), "string07 DATA");
            Assert.assertEquals(findEntity.getEmb04d_str11(), "string08 DATA");
            Assert.assertEquals(findEntity.getEmb04d_str12(), "string09 DATA");
            Assert.assertEquals(findEntity.getEmb04e_int13(), 7);
            Assert.assertEquals(findEntity.getEmb04e_int14(), 8);
            Assert.assertEquals(findEntity.getEmb04e_int15(), 9);
            Assert.assertEquals(findEntity.getEmb04f_str16(), "string10 DATA");
            Assert.assertEquals(findEntity.getEmb04f_str17(), "string11 DATA");
            Assert.assertEquals(findEntity.getEmb04f_str18(), "string12 DATA");

            // Update the entity
            findEntity.setEnt04_str01("UPDATED string01 DATA");
            findEntity.setEnt04_str02("UPDATED string02 DATA");
            findEntity.setEnt04_str03("UPDATED string03 DATA");
            findEntity.setEmb04a_int01(10);
            findEntity.setEmb04a_int02(11);
            findEntity.setEmb04a_int03(12);
            findEntity.setEmb04b_str04("UPDATED string04 DATA");
            findEntity.setEmb04b_str05("UPDATED string05 DATA");
            findEntity.setEmb04b_str06("UPDATED string06 DATA");
            findEntity.setEmb04c_int07(13);
            findEntity.setEmb04c_int08(14);
            findEntity.setEmb04c_int09(15);
            findEntity.setEmb04d_str10("UPDATED string07 DATA");
            findEntity.setEmb04d_str11("UPDATED string08 DATA");
            findEntity.setEmb04d_str12("UPDATED string09 DATA");
            findEntity.setEmb04e_int13(16);
            findEntity.setEmb04e_int14(17);
            findEntity.setEmb04e_int15(18);
            findEntity.setEmb04f_str16("UPDATED string10 DATA");
            findEntity.setEmb04f_str17("UPDATED string11 DATA");
            findEntity.setEmb04f_str18("UPDATED string12 DATA");

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity update was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity04 updatedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", updatedEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedEntity);
            Assert.assertTrue(findEntity != updatedEntity);
            Assert.assertTrue(em.contains(updatedEntity));
            Assert.assertEquals(updatedEntity.getId(), id);
            Assert.assertEquals(updatedEntity.getEnt04_str01(), "UPDATED string01 DATA");
            Assert.assertEquals(updatedEntity.getEnt04_str02(), "UPDATED string02 DATA");
            Assert.assertEquals(updatedEntity.getEnt04_str03(), "UPDATED string03 DATA");
            Assert.assertEquals(updatedEntity.getEmb04a_int01(), 10);
            Assert.assertEquals(updatedEntity.getEmb04a_int02(), 11);
            Assert.assertEquals(updatedEntity.getEmb04a_int03(), 12);
            Assert.assertEquals(updatedEntity.getEmb04b_str04(), "UPDATED string04 DATA");
            Assert.assertEquals(updatedEntity.getEmb04b_str05(), "UPDATED string05 DATA");
            Assert.assertEquals(updatedEntity.getEmb04b_str06(), "UPDATED string06 DATA");
            Assert.assertEquals(updatedEntity.getEmb04c_int07(), 13);
            Assert.assertEquals(updatedEntity.getEmb04c_int08(), 14);
            Assert.assertEquals(updatedEntity.getEmb04c_int09(), 15);
            Assert.assertEquals(updatedEntity.getEmb04d_str10(), "UPDATED string07 DATA");
            Assert.assertEquals(updatedEntity.getEmb04d_str11(), "UPDATED string08 DATA");
            Assert.assertEquals(updatedEntity.getEmb04d_str12(), "UPDATED string09 DATA");
            Assert.assertEquals(updatedEntity.getEmb04e_int13(), 16);
            Assert.assertEquals(updatedEntity.getEmb04e_int14(), 17);
            Assert.assertEquals(updatedEntity.getEmb04e_int15(), 18);
            Assert.assertEquals(updatedEntity.getEmb04f_str16(), "UPDATED string10 DATA");
            Assert.assertEquals(updatedEntity.getEmb04f_str17(), "UPDATED string11 DATA");
            Assert.assertEquals(updatedEntity.getEmb04f_str18(), "UPDATED string12 DATA");

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedEntity + ") operation");
            em.remove(updatedEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            Object findRemovedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNull("find(" + newEntity.getClass() + ", " + id + ") returned an entity.", findRemovedEntity);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Test Logic: testScenario06
     *
     * Description: Test of single entity with multiple embeddables, and multiple nested embeddalbes
     *
     * Performs these basic CRUD operations:
     *
     * 01. Create a new instance of the entity class
     * 02. Persist the new entity in the database
     * 03. Verify the entity was saved in the database
     * 04. Update the entity
     * 05. Verify the updated entity was saved in the database
     * 06. Delete the entity from the database
     * 07. Verify the deletion was successful
     *
     * UML:
     * +-----------------+
     * | Embeddable01 |
     * +--------+--------+
     * 1 |
     * |
     * |
     * 1 <*>
     * +--------+--------+ 1 1 +-----------------+
     * | Entity06 |<*>--------| Embeddable02a |
     * +--------+--------+ +--------+--------+
     * 1 <*> 1 <*>
     * | |
     * | |
     * 1 | 1 |
     * +--------+--------+ +--------+--------+
     * | Embeddable04a | | Embeddable02b |
     * +--------+--------+ +-----------------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +--------+--------+
     * | Embeddable04b |
     * +--------+--------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +--------+--------+
     * | Embeddable04c |
     * +--------+--------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +--------+--------+
     * | Embeddable04d |
     * +--------+--------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +--------+--------+
     * | Embeddable04e |
     * +--------+--------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +--------+--------+
     * | Embeddable04f |
     * +-----------------+
     *
     */
    public void testEmbeddableNested07(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 6;

            IEntity06 newEntity = new Entity06();
            newEntity.setId(id);
            newEntity.setEnt06_str01("string01 DATA");
            newEntity.setEnt06_str02("string02 DATA");
            newEntity.setEnt06_str03("string03 DATA");
            newEntity.setEmb01_int01(1);
            newEntity.setEmb01_int02(2);
            newEntity.setEmb01_int03(3);
            newEntity.setEmb04a_int01(4);
            newEntity.setEmb04a_int02(5);
            newEntity.setEmb04a_int03(6);
            newEntity.setEmb04b_str04("string04 DATA");
            newEntity.setEmb04b_str05("string05 DATA");
            newEntity.setEmb04b_str06("string06 DATA");
            newEntity.setEmb04c_int07(7);
            newEntity.setEmb04c_int08(8);
            newEntity.setEmb04c_int09(9);
            newEntity.setEmb04d_str10("string07 DATA");
            newEntity.setEmb04d_str11("string08 DATA");
            newEntity.setEmb04d_str12("string09 DATA");
            newEntity.setEmb04e_int13(10);
            newEntity.setEmb04e_int14(11);
            newEntity.setEmb04e_int15(12);
            newEntity.setEmb04f_str16("string10 DATA");
            newEntity.setEmb04f_str17("string11 DATA");
            newEntity.setEmb04f_str18("string12 DATA");

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing persist(" + newEntity + ") operation");
            em.persist(newEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity06 findEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(em.contains(findEntity));
            Assert.assertEquals(findEntity.getId(), id);
            Assert.assertEquals(findEntity.getEnt06_str01(), "string01 DATA");
            Assert.assertEquals(findEntity.getEnt06_str02(), "string02 DATA");
            Assert.assertEquals(findEntity.getEnt06_str03(), "string03 DATA");
            Assert.assertEquals(findEntity.getEmb01_int01(), 1);
            Assert.assertEquals(findEntity.getEmb01_int02(), 2);
            Assert.assertEquals(findEntity.getEmb01_int03(), 3);
            Assert.assertEquals(findEntity.getEmb04a_int01(), 4);
            Assert.assertEquals(findEntity.getEmb04a_int02(), 5);
            Assert.assertEquals(findEntity.getEmb04a_int03(), 6);
            Assert.assertEquals(findEntity.getEmb04b_str04(), "string04 DATA");
            Assert.assertEquals(findEntity.getEmb04b_str05(), "string05 DATA");
            Assert.assertEquals(findEntity.getEmb04b_str06(), "string06 DATA");
            Assert.assertEquals(findEntity.getEmb04c_int07(), 7);
            Assert.assertEquals(findEntity.getEmb04c_int08(), 8);
            Assert.assertEquals(findEntity.getEmb04c_int09(), 9);
            Assert.assertEquals(findEntity.getEmb04d_str10(), "string07 DATA");
            Assert.assertEquals(findEntity.getEmb04d_str11(), "string08 DATA");
            Assert.assertEquals(findEntity.getEmb04d_str12(), "string09 DATA");
            Assert.assertEquals(findEntity.getEmb04e_int13(), 10);
            Assert.assertEquals(findEntity.getEmb04e_int14(), 11);
            Assert.assertEquals(findEntity.getEmb04e_int15(), 12);
            Assert.assertEquals(findEntity.getEmb04f_str16(), "string10 DATA");
            Assert.assertEquals(findEntity.getEmb04f_str17(), "string11 DATA");
            Assert.assertEquals(findEntity.getEmb04f_str18(), "string12 DATA");

            // Update the entity
            findEntity.setEnt06_str01("UPDATED string01 DATA");
            findEntity.setEnt06_str02("UPDATED string02 DATA");
            findEntity.setEnt06_str03("UPDATED string03 DATA");
            findEntity.setEmb01_int01(10);
            findEntity.setEmb01_int02(11);
            findEntity.setEmb01_int03(12);
            findEntity.setEmb04a_int01(10);
            findEntity.setEmb04a_int02(11);
            findEntity.setEmb04a_int03(12);
            findEntity.setEmb04b_str04("UPDATED string04 DATA");
            findEntity.setEmb04b_str05("UPDATED string05 DATA");
            findEntity.setEmb04b_str06("UPDATED string06 DATA");
            findEntity.setEmb04c_int07(13);
            findEntity.setEmb04c_int08(14);
            findEntity.setEmb04c_int09(15);
            findEntity.setEmb04d_str10("UPDATED string07 DATA");
            findEntity.setEmb04d_str11("UPDATED string08 DATA");
            findEntity.setEmb04d_str12("UPDATED string09 DATA");
            findEntity.setEmb04e_int13(16);
            findEntity.setEmb04e_int14(17);
            findEntity.setEmb04e_int15(18);
            findEntity.setEmb04f_str16("UPDATED string10 DATA");
            findEntity.setEmb04f_str17("UPDATED string11 DATA");
            findEntity.setEmb04f_str18("UPDATED string12 DATA");

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity update was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity06 updatedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", updatedEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedEntity);
            Assert.assertTrue(findEntity != updatedEntity);
            Assert.assertTrue(em.contains(updatedEntity));
            Assert.assertEquals(updatedEntity.getId(), id);
            Assert.assertEquals(updatedEntity.getEnt06_str01(), "UPDATED string01 DATA");
            Assert.assertEquals(updatedEntity.getEnt06_str02(), "UPDATED string02 DATA");
            Assert.assertEquals(updatedEntity.getEnt06_str03(), "UPDATED string03 DATA");
            Assert.assertEquals(updatedEntity.getEmb01_int01(), 10);
            Assert.assertEquals(updatedEntity.getEmb01_int02(), 11);
            Assert.assertEquals(updatedEntity.getEmb01_int03(), 12);
            Assert.assertEquals(updatedEntity.getEmb04a_int01(), 10);
            Assert.assertEquals(updatedEntity.getEmb04a_int02(), 11);
            Assert.assertEquals(updatedEntity.getEmb04a_int03(), 12);
            Assert.assertEquals(updatedEntity.getEmb04b_str04(), "UPDATED string04 DATA");
            Assert.assertEquals(updatedEntity.getEmb04b_str05(), "UPDATED string05 DATA");
            Assert.assertEquals(updatedEntity.getEmb04b_str06(), "UPDATED string06 DATA");
            Assert.assertEquals(updatedEntity.getEmb04c_int07(), 13);
            Assert.assertEquals(updatedEntity.getEmb04c_int08(), 14);
            Assert.assertEquals(updatedEntity.getEmb04c_int09(), 15);
            Assert.assertEquals(updatedEntity.getEmb04d_str10(), "UPDATED string07 DATA");
            Assert.assertEquals(updatedEntity.getEmb04d_str11(), "UPDATED string08 DATA");
            Assert.assertEquals(updatedEntity.getEmb04d_str12(), "UPDATED string09 DATA");
            Assert.assertEquals(updatedEntity.getEmb04e_int13(), 16);
            Assert.assertEquals(updatedEntity.getEmb04e_int14(), 17);
            Assert.assertEquals(updatedEntity.getEmb04e_int15(), 18);
            Assert.assertEquals(updatedEntity.getEmb04f_str16(), "UPDATED string10 DATA");
            Assert.assertEquals(updatedEntity.getEmb04f_str17(), "UPDATED string11 DATA");
            Assert.assertEquals(updatedEntity.getEmb04f_str18(), "UPDATED string12 DATA");

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedEntity + ") operation");
            em.remove(updatedEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            Object findRemovedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNull("find(" + newEntity.getClass() + ", " + id + ") returned an entity.", findRemovedEntity);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testEmbeddableNested08(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 6;

            IEntity06 newEntity = new Entity06();
            newEntity.setId(id);
            newEntity.setEnt06_str01("string01 DATA");
            newEntity.setEnt06_str02("string02 DATA");
            newEntity.setEnt06_str03("string03 DATA");
            newEntity.setEmb01_int01(1);
            newEntity.setEmb01_int02(2);
            newEntity.setEmb01_int03(3);
            newEntity.setEmb04a_int01(4);
            newEntity.setEmb04a_int02(5);
            newEntity.setEmb04a_int03(6);
            newEntity.setEmb04b_str04("string04 DATA");
            newEntity.setEmb04b_str05("string05 DATA");
            newEntity.setEmb04b_str06("string06 DATA");
            newEntity.setEmb04c_int07(7);
            newEntity.setEmb04c_int08(8);
            newEntity.setEmb04c_int09(9);
            newEntity.setEmb04d_str10("string07 DATA");
            newEntity.setEmb04d_str11("string08 DATA");
            newEntity.setEmb04d_str12("string09 DATA");
            newEntity.setEmb04e_int13(10);
            newEntity.setEmb04e_int14(11);
            newEntity.setEmb04e_int15(12);
            newEntity.setEmb04f_str16("string10 DATA");
            newEntity.setEmb04f_str17("string11 DATA");
            newEntity.setEmb04f_str18("string12 DATA");

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing persist(" + newEntity + ") operation");
            em.persist(newEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity06 findEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(em.contains(findEntity));
            Assert.assertEquals(findEntity.getId(), id);
            Assert.assertEquals(findEntity.getEnt06_str01(), "string01 DATA");
            Assert.assertEquals(findEntity.getEnt06_str02(), "string02 DATA");
            Assert.assertEquals(findEntity.getEnt06_str03(), "string03 DATA");
            Assert.assertEquals(findEntity.getEmb01_int01(), 1);
            Assert.assertEquals(findEntity.getEmb01_int02(), 2);
            Assert.assertEquals(findEntity.getEmb01_int03(), 3);
            Assert.assertEquals(findEntity.getEmb04a_int01(), 4);
            Assert.assertEquals(findEntity.getEmb04a_int02(), 5);
            Assert.assertEquals(findEntity.getEmb04a_int03(), 6);
            Assert.assertEquals(findEntity.getEmb04b_str04(), "string04 DATA");
            Assert.assertEquals(findEntity.getEmb04b_str05(), "string05 DATA");
            Assert.assertEquals(findEntity.getEmb04b_str06(), "string06 DATA");
            Assert.assertEquals(findEntity.getEmb04c_int07(), 7);
            Assert.assertEquals(findEntity.getEmb04c_int08(), 8);
            Assert.assertEquals(findEntity.getEmb04c_int09(), 9);
            Assert.assertEquals(findEntity.getEmb04d_str10(), "string07 DATA");
            Assert.assertEquals(findEntity.getEmb04d_str11(), "string08 DATA");
            Assert.assertEquals(findEntity.getEmb04d_str12(), "string09 DATA");
            Assert.assertEquals(findEntity.getEmb04e_int13(), 10);
            Assert.assertEquals(findEntity.getEmb04e_int14(), 11);
            Assert.assertEquals(findEntity.getEmb04e_int15(), 12);
            Assert.assertEquals(findEntity.getEmb04f_str16(), "string10 DATA");
            Assert.assertEquals(findEntity.getEmb04f_str17(), "string11 DATA");
            Assert.assertEquals(findEntity.getEmb04f_str18(), "string12 DATA");

            // Update the entity
            findEntity.setEnt06_str01("UPDATED string01 DATA");
            findEntity.setEnt06_str02("UPDATED string02 DATA");
            findEntity.setEnt06_str03("UPDATED string03 DATA");
            findEntity.setEmb01_int01(10);
            findEntity.setEmb01_int02(11);
            findEntity.setEmb01_int03(12);
            findEntity.setEmb04a_int01(10);
            findEntity.setEmb04a_int02(11);
            findEntity.setEmb04a_int03(12);
            findEntity.setEmb04b_str04("UPDATED string04 DATA");
            findEntity.setEmb04b_str05("UPDATED string05 DATA");
            findEntity.setEmb04b_str06("UPDATED string06 DATA");
            findEntity.setEmb04c_int07(13);
            findEntity.setEmb04c_int08(14);
            findEntity.setEmb04c_int09(15);
            findEntity.setEmb04d_str10("UPDATED string07 DATA");
            findEntity.setEmb04d_str11("UPDATED string08 DATA");
            findEntity.setEmb04d_str12("UPDATED string09 DATA");
            findEntity.setEmb04e_int13(16);
            findEntity.setEmb04e_int14(17);
            findEntity.setEmb04e_int15(18);
            findEntity.setEmb04f_str16("UPDATED string10 DATA");
            findEntity.setEmb04f_str17("UPDATED string11 DATA");
            findEntity.setEmb04f_str18("UPDATED string12 DATA");

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity update was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity06 updatedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", updatedEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedEntity);
            Assert.assertTrue(findEntity != updatedEntity);
            Assert.assertTrue(em.contains(updatedEntity));
            Assert.assertEquals(updatedEntity.getId(), id);
            Assert.assertEquals(updatedEntity.getEnt06_str01(), "UPDATED string01 DATA");
            Assert.assertEquals(updatedEntity.getEnt06_str02(), "UPDATED string02 DATA");
            Assert.assertEquals(updatedEntity.getEnt06_str03(), "UPDATED string03 DATA");
            Assert.assertEquals(updatedEntity.getEmb01_int01(), 10);
            Assert.assertEquals(updatedEntity.getEmb01_int02(), 11);
            Assert.assertEquals(updatedEntity.getEmb01_int03(), 12);
            Assert.assertEquals(updatedEntity.getEmb04a_int01(), 10);
            Assert.assertEquals(updatedEntity.getEmb04a_int02(), 11);
            Assert.assertEquals(updatedEntity.getEmb04a_int03(), 12);
            Assert.assertEquals(updatedEntity.getEmb04b_str04(), "UPDATED string04 DATA");
            Assert.assertEquals(updatedEntity.getEmb04b_str05(), "UPDATED string05 DATA");
            Assert.assertEquals(updatedEntity.getEmb04b_str06(), "UPDATED string06 DATA");
            Assert.assertEquals(updatedEntity.getEmb04c_int07(), 13);
            Assert.assertEquals(updatedEntity.getEmb04c_int08(), 14);
            Assert.assertEquals(updatedEntity.getEmb04c_int09(), 15);
            Assert.assertEquals(updatedEntity.getEmb04d_str10(), "UPDATED string07 DATA");
            Assert.assertEquals(updatedEntity.getEmb04d_str11(), "UPDATED string08 DATA");
            Assert.assertEquals(updatedEntity.getEmb04d_str12(), "UPDATED string09 DATA");
            Assert.assertEquals(updatedEntity.getEmb04e_int13(), 16);
            Assert.assertEquals(updatedEntity.getEmb04e_int14(), 17);
            Assert.assertEquals(updatedEntity.getEmb04e_int15(), 18);
            Assert.assertEquals(updatedEntity.getEmb04f_str16(), "UPDATED string10 DATA");
            Assert.assertEquals(updatedEntity.getEmb04f_str17(), "UPDATED string11 DATA");
            Assert.assertEquals(updatedEntity.getEmb04f_str18(), "UPDATED string12 DATA");

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedEntity + ") operation");
            em.remove(updatedEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            Object findRemovedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNull("find(" + newEntity.getClass() + ", " + id + ") returned an entity.", findRemovedEntity);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Test Logic: testScenario07
     *
     * Description: Test of single entity with multiple embeddables, and multiple nested embeddalbes
     *
     * Performs these basic CRUD operations:
     *
     * 01. Create a new instance of the entity class
     * 02. Persist the new entity in the database
     * 03. Verify the entity was saved in the database
     * 04. Update the entity
     * 05. Verify the updated entity was saved in the database
     * 06. Delete the entity from the database
     * 07. Verify the deletion was successful
     *
     * UML:
     * +-----------------+ 1
     * | Embeddable01 |--------------------+
     * +--------+--------+ |
     * 1 | |
     * | |
     * | | 1
     * 1 <*> <*>
     * +--------+--------+ 1 1 +--------+--------+
     * | Entity07 |<*>--------| Embeddable07a |
     * +--------+--------+ +--------+--------+
     * 1 <*> 1 <*>
     * | |
     * | |
     * 1 | 1 |
     * +--------+--------+ 1 1 +--------+--------+
     * | Embeddable04a |--------<*>| Embeddable07b |
     * +--------+--------+ +-----------------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +--------+--------+
     * | Embeddable04b |
     * +--------+--------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +--------+--------+
     * | Embeddable04c |
     * +--------+--------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +--------+--------+
     * | Embeddable04d |
     * +--------+--------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +--------+--------+
     * | Embeddable04e |
     * +--------+--------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +--------+--------+
     * | Embeddable04f |
     * +-----------------+
     *
     */
    public void testEmbeddableNested09(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 7;

            IEntity07 newEntity = new Entity07();
            newEntity.setId(id);
            newEntity.setEnt07_str01("string01 DATA");
            newEntity.setEnt07_str02("string02 DATA");
            newEntity.setEnt07_str03("string03 DATA");
            newEntity.setEmb01_int01(1);
            newEntity.setEmb01_int02(2);
            newEntity.setEmb01_int03(3);
            newEntity.setEmb04a_int01(4);
            newEntity.setEmb04a_int02(5);
            newEntity.setEmb04a_int03(6);
            newEntity.setEmb04b_str04("string04 DATA");
            newEntity.setEmb04b_str05("string05 DATA");
            newEntity.setEmb04b_str06("string06 DATA");
            newEntity.setEmb04c_int07(7);
            newEntity.setEmb04c_int08(8);
            newEntity.setEmb04c_int09(9);
            newEntity.setEmb04d_str10("string07 DATA");
            newEntity.setEmb04d_str11("string08 DATA");
            newEntity.setEmb04d_str12("string09 DATA");
            newEntity.setEmb04e_int13(10);
            newEntity.setEmb04e_int14(11);
            newEntity.setEmb04e_int15(12);
            newEntity.setEmb04f_str16("string10 DATA");
            newEntity.setEmb04f_str17("string11 DATA");
            newEntity.setEmb04f_str18("string12 DATA");
            newEntity.setEmb07a_int01(13);
            newEntity.setEmb07a_int02(14);
            newEntity.setEmb07a_int03(15);
            newEntity.setEmb07a_emb01_int01(16);
            newEntity.setEmb07a_emb01_int02(17);
            newEntity.setEmb07a_emb01_int03(18);
            newEntity.setEmb07b_int04(19);
            newEntity.setEmb07b_int05(20);
            newEntity.setEmb07b_int06(21);
            newEntity.setEmb07b_emb04a_int01(22);
            newEntity.setEmb07b_emb04a_int02(23);
            newEntity.setEmb07b_emb04a_int03(24);
            newEntity.setEmb07b_emb04b_str04("emb04_string04 DATA");
            newEntity.setEmb07b_emb04b_str05("emb04_string05 DATA");
            newEntity.setEmb07b_emb04b_str06("emb04_string06 DATA");
            newEntity.setEmb07b_emb04c_int07(25);
            newEntity.setEmb07b_emb04c_int08(26);
            newEntity.setEmb07b_emb04c_int09(27);
            newEntity.setEmb07b_emb04d_str10("emb04_string07 DATA");
            newEntity.setEmb07b_emb04d_str11("emb04_string08 DATA");
            newEntity.setEmb07b_emb04d_str12("emb04_string09 DATA");
            newEntity.setEmb07b_emb04e_int13(28);
            newEntity.setEmb07b_emb04e_int14(29);
            newEntity.setEmb07b_emb04e_int15(30);
            newEntity.setEmb07b_emb04f_str16("emb04_string10 DATA");
            newEntity.setEmb07b_emb04f_str17("emb04_string11 DATA");
            newEntity.setEmb07b_emb04f_str18("emb04_string12 DATA");

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing persist(" + newEntity + ") operation");
            em.persist(newEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity07 findEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(em.contains(findEntity));
            Assert.assertEquals(findEntity.getId(), id);
            Assert.assertEquals(findEntity.getEnt07_str01(), "string01 DATA");
            Assert.assertEquals(findEntity.getEnt07_str02(), "string02 DATA");
            Assert.assertEquals(findEntity.getEnt07_str03(), "string03 DATA");
            Assert.assertEquals(findEntity.getEmb01_int01(), 1);
            Assert.assertEquals(findEntity.getEmb01_int02(), 2);
            Assert.assertEquals(findEntity.getEmb01_int03(), 3);
            Assert.assertEquals(findEntity.getEmb04a_int01(), 4);
            Assert.assertEquals(findEntity.getEmb04a_int02(), 5);
            Assert.assertEquals(findEntity.getEmb04a_int03(), 6);
            Assert.assertEquals(findEntity.getEmb04b_str04(), "string04 DATA");
            Assert.assertEquals(findEntity.getEmb04b_str05(), "string05 DATA");
            Assert.assertEquals(findEntity.getEmb04b_str06(), "string06 DATA");
            Assert.assertEquals(findEntity.getEmb04c_int07(), 7);
            Assert.assertEquals(findEntity.getEmb04c_int08(), 8);
            Assert.assertEquals(findEntity.getEmb04c_int09(), 9);
            Assert.assertEquals(findEntity.getEmb04d_str10(), "string07 DATA");
            Assert.assertEquals(findEntity.getEmb04d_str11(), "string08 DATA");
            Assert.assertEquals(findEntity.getEmb04d_str12(), "string09 DATA");
            Assert.assertEquals(findEntity.getEmb04e_int13(), 10);
            Assert.assertEquals(findEntity.getEmb04e_int14(), 11);
            Assert.assertEquals(findEntity.getEmb04e_int15(), 12);
            Assert.assertEquals(findEntity.getEmb04f_str16(), "string10 DATA");
            Assert.assertEquals(findEntity.getEmb04f_str17(), "string11 DATA");
            Assert.assertEquals(findEntity.getEmb04f_str18(), "string12 DATA");
            Assert.assertEquals(findEntity.getEmb07a_int01(), 13);
            Assert.assertEquals(findEntity.getEmb07a_int02(), 14);
            Assert.assertEquals(findEntity.getEmb07a_int03(), 15);
            Assert.assertEquals(findEntity.getEmb07a_emb01_int01(), 16);
            Assert.assertEquals(findEntity.getEmb07a_emb01_int02(), 17);
            Assert.assertEquals(findEntity.getEmb07a_emb01_int03(), 18);
            Assert.assertEquals(findEntity.getEmb07b_int04(), 19);
            Assert.assertEquals(findEntity.getEmb07b_int05(), 20);
            Assert.assertEquals(findEntity.getEmb07b_int06(), 21);
            Assert.assertEquals(findEntity.getEmb07b_emb04a_int01(), 22);
            Assert.assertEquals(findEntity.getEmb07b_emb04a_int02(), 23);
            Assert.assertEquals(findEntity.getEmb07b_emb04a_int03(), 24);
            Assert.assertEquals(findEntity.getEmb07b_emb04b_str04(), "emb04_string04 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04b_str05(), "emb04_string05 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04b_str06(), "emb04_string06 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04c_int07(), 25);
            Assert.assertEquals(findEntity.getEmb07b_emb04c_int08(), 26);
            Assert.assertEquals(findEntity.getEmb07b_emb04c_int09(), 27);
            Assert.assertEquals(findEntity.getEmb07b_emb04d_str10(), "emb04_string07 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04d_str11(), "emb04_string08 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04d_str12(), "emb04_string09 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04e_int13(), 28);
            Assert.assertEquals(findEntity.getEmb07b_emb04e_int14(), 29);
            Assert.assertEquals(findEntity.getEmb07b_emb04e_int15(), 30);
            Assert.assertEquals(findEntity.getEmb07b_emb04f_str16(), "emb04_string10 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04f_str17(), "emb04_string11 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04f_str18(), "emb04_string12 DATA");

            // Update the entity
            findEntity.setEnt07_str01("UPDATED string01 DATA");
            findEntity.setEnt07_str02("UPDATED string02 DATA");
            findEntity.setEnt07_str03("UPDATED string03 DATA");
            findEntity.setEmb01_int01(31);
            findEntity.setEmb01_int02(32);
            findEntity.setEmb01_int03(33);
            findEntity.setEmb04a_int01(34);
            findEntity.setEmb04a_int02(35);
            findEntity.setEmb04a_int03(36);
            findEntity.setEmb04b_str04("UPDATED string04 DATA");
            findEntity.setEmb04b_str05("UPDATED string05 DATA");
            findEntity.setEmb04b_str06("UPDATED string06 DATA");
            findEntity.setEmb04c_int07(37);
            findEntity.setEmb04c_int08(38);
            findEntity.setEmb04c_int09(39);
            findEntity.setEmb04d_str10("UPDATED string07 DATA");
            findEntity.setEmb04d_str11("UPDATED string08 DATA");
            findEntity.setEmb04d_str12("UPDATED string09 DATA");
            findEntity.setEmb04e_int13(40);
            findEntity.setEmb04e_int14(41);
            findEntity.setEmb04e_int15(42);
            findEntity.setEmb04f_str16("UPDATED string10 DATA");
            findEntity.setEmb04f_str17("UPDATED string11 DATA");
            findEntity.setEmb04f_str18("UPDATED string12 DATA");
            findEntity.setEmb07a_int01(43);
            findEntity.setEmb07a_int02(44);
            findEntity.setEmb07a_int03(45);
            findEntity.setEmb07a_emb01_int01(46);
            findEntity.setEmb07a_emb01_int02(47);
            findEntity.setEmb07a_emb01_int03(48);
            findEntity.setEmb07b_int04(49);
            findEntity.setEmb07b_int05(50);
            findEntity.setEmb07b_int06(51);
            findEntity.setEmb07b_emb04a_int01(52);
            findEntity.setEmb07b_emb04a_int02(53);
            findEntity.setEmb07b_emb04a_int03(54);
            findEntity.setEmb07b_emb04b_str04("UPDATED emb04_string04 DATA");
            findEntity.setEmb07b_emb04b_str05("UPDATED emb04_string05 DATA");
            findEntity.setEmb07b_emb04b_str06("UPDATED emb04_string06 DATA");
            findEntity.setEmb07b_emb04c_int07(55);
            findEntity.setEmb07b_emb04c_int08(56);
            findEntity.setEmb07b_emb04c_int09(57);
            findEntity.setEmb07b_emb04d_str10("UPDATED emb04_string07 DATA");
            findEntity.setEmb07b_emb04d_str11("UPDATED emb04_string08 DATA");
            findEntity.setEmb07b_emb04d_str12("UPDATED emb04_string09 DATA");
            findEntity.setEmb07b_emb04e_int13(58);
            findEntity.setEmb07b_emb04e_int14(59);
            findEntity.setEmb07b_emb04e_int15(60);
            findEntity.setEmb07b_emb04f_str16("UPDATED emb04_string10 DATA");
            findEntity.setEmb07b_emb04f_str17("UPDATED emb04_string11 DATA");
            findEntity.setEmb07b_emb04f_str18("UPDATED emb04_string12 DATA");

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity update was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity07 updatedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", updatedEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedEntity);
            Assert.assertTrue(findEntity != updatedEntity);
            Assert.assertTrue(em.contains(updatedEntity));
            Assert.assertEquals(updatedEntity.getId(), id);
            Assert.assertEquals(updatedEntity.getEnt07_str01(), "UPDATED string01 DATA");
            Assert.assertEquals(updatedEntity.getEnt07_str02(), "UPDATED string02 DATA");
            Assert.assertEquals(updatedEntity.getEnt07_str03(), "UPDATED string03 DATA");
            Assert.assertEquals(updatedEntity.getEmb01_int01(), 31);
            Assert.assertEquals(updatedEntity.getEmb01_int02(), 32);
            Assert.assertEquals(updatedEntity.getEmb01_int03(), 33);
            Assert.assertEquals(updatedEntity.getEmb04a_int01(), 34);
            Assert.assertEquals(updatedEntity.getEmb04a_int02(), 35);
            Assert.assertEquals(updatedEntity.getEmb04a_int03(), 36);
            Assert.assertEquals(updatedEntity.getEmb04b_str04(), "UPDATED string04 DATA");
            Assert.assertEquals(updatedEntity.getEmb04b_str05(), "UPDATED string05 DATA");
            Assert.assertEquals(updatedEntity.getEmb04b_str06(), "UPDATED string06 DATA");
            Assert.assertEquals(updatedEntity.getEmb04c_int07(), 37);
            Assert.assertEquals(updatedEntity.getEmb04c_int08(), 38);
            Assert.assertEquals(updatedEntity.getEmb04c_int09(), 39);
            Assert.assertEquals(updatedEntity.getEmb04d_str10(), "UPDATED string07 DATA");
            Assert.assertEquals(updatedEntity.getEmb04d_str11(), "UPDATED string08 DATA");
            Assert.assertEquals(updatedEntity.getEmb04d_str12(), "UPDATED string09 DATA");
            Assert.assertEquals(updatedEntity.getEmb04e_int13(), 40);
            Assert.assertEquals(updatedEntity.getEmb04e_int14(), 41);
            Assert.assertEquals(updatedEntity.getEmb04e_int15(), 42);
            Assert.assertEquals(updatedEntity.getEmb04f_str16(), "UPDATED string10 DATA");
            Assert.assertEquals(updatedEntity.getEmb04f_str17(), "UPDATED string11 DATA");
            Assert.assertEquals(updatedEntity.getEmb04f_str18(), "UPDATED string12 DATA");
            Assert.assertEquals(updatedEntity.getEmb07a_int01(), 43);
            Assert.assertEquals(updatedEntity.getEmb07a_int02(), 44);
            Assert.assertEquals(updatedEntity.getEmb07a_int03(), 45);
            Assert.assertEquals(findEntity.getEmb07a_emb01_int01(), 46);
            Assert.assertEquals(findEntity.getEmb07a_emb01_int02(), 47);
            Assert.assertEquals(findEntity.getEmb07a_emb01_int03(), 48);
            Assert.assertEquals(updatedEntity.getEmb07b_int04(), 49);
            Assert.assertEquals(updatedEntity.getEmb07b_int05(), 50);
            Assert.assertEquals(updatedEntity.getEmb07b_int06(), 51);
            Assert.assertEquals(findEntity.getEmb07b_emb04a_int01(), 52);
            Assert.assertEquals(findEntity.getEmb07b_emb04a_int02(), 53);
            Assert.assertEquals(findEntity.getEmb07b_emb04a_int03(), 54);
            Assert.assertEquals(findEntity.getEmb07b_emb04b_str04(), "UPDATED emb04_string04 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04b_str05(), "UPDATED emb04_string05 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04b_str06(), "UPDATED emb04_string06 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04c_int07(), 55);
            Assert.assertEquals(findEntity.getEmb07b_emb04c_int08(), 56);
            Assert.assertEquals(findEntity.getEmb07b_emb04c_int09(), 57);
            Assert.assertEquals(findEntity.getEmb07b_emb04d_str10(), "UPDATED emb04_string07 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04d_str11(), "UPDATED emb04_string08 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04d_str12(), "UPDATED emb04_string09 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04e_int13(), 58);
            Assert.assertEquals(findEntity.getEmb07b_emb04e_int14(), 59);
            Assert.assertEquals(findEntity.getEmb07b_emb04e_int15(), 60);
            Assert.assertEquals(findEntity.getEmb07b_emb04f_str16(), "UPDATED emb04_string10 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04f_str17(), "UPDATED emb04_string11 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04f_str18(), "UPDATED emb04_string12 DATA");

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedEntity + ") operation");
            em.remove(updatedEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            Object findRemovedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNull("find(" + newEntity.getClass() + ", " + id + ") returned an entity.", findRemovedEntity);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testEmbeddableNested10(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 7;

            IEntity07 newEntity = new XMLEntity07();
            newEntity.setId(id);
            newEntity.setEnt07_str01("string01 DATA");
            newEntity.setEnt07_str02("string02 DATA");
            newEntity.setEnt07_str03("string03 DATA");
            newEntity.setEmb01_int01(1);
            newEntity.setEmb01_int02(2);
            newEntity.setEmb01_int03(3);
            newEntity.setEmb04a_int01(4);
            newEntity.setEmb04a_int02(5);
            newEntity.setEmb04a_int03(6);
            newEntity.setEmb04b_str04("string04 DATA");
            newEntity.setEmb04b_str05("string05 DATA");
            newEntity.setEmb04b_str06("string06 DATA");
            newEntity.setEmb04c_int07(7);
            newEntity.setEmb04c_int08(8);
            newEntity.setEmb04c_int09(9);
            newEntity.setEmb04d_str10("string07 DATA");
            newEntity.setEmb04d_str11("string08 DATA");
            newEntity.setEmb04d_str12("string09 DATA");
            newEntity.setEmb04e_int13(10);
            newEntity.setEmb04e_int14(11);
            newEntity.setEmb04e_int15(12);
            newEntity.setEmb04f_str16("string10 DATA");
            newEntity.setEmb04f_str17("string11 DATA");
            newEntity.setEmb04f_str18("string12 DATA");
            newEntity.setEmb07a_int01(13);
            newEntity.setEmb07a_int02(14);
            newEntity.setEmb07a_int03(15);
            newEntity.setEmb07a_emb01_int01(16);
            newEntity.setEmb07a_emb01_int02(17);
            newEntity.setEmb07a_emb01_int03(18);
            newEntity.setEmb07b_int04(19);
            newEntity.setEmb07b_int05(20);
            newEntity.setEmb07b_int06(21);
            newEntity.setEmb07b_emb04a_int01(22);
            newEntity.setEmb07b_emb04a_int02(23);
            newEntity.setEmb07b_emb04a_int03(24);
            newEntity.setEmb07b_emb04b_str04("emb04_string04 DATA");
            newEntity.setEmb07b_emb04b_str05("emb04_string05 DATA");
            newEntity.setEmb07b_emb04b_str06("emb04_string06 DATA");
            newEntity.setEmb07b_emb04c_int07(25);
            newEntity.setEmb07b_emb04c_int08(26);
            newEntity.setEmb07b_emb04c_int09(27);
            newEntity.setEmb07b_emb04d_str10("emb04_string07 DATA");
            newEntity.setEmb07b_emb04d_str11("emb04_string08 DATA");
            newEntity.setEmb07b_emb04d_str12("emb04_string09 DATA");
            newEntity.setEmb07b_emb04e_int13(28);
            newEntity.setEmb07b_emb04e_int14(29);
            newEntity.setEmb07b_emb04e_int15(30);
            newEntity.setEmb07b_emb04f_str16("emb04_string10 DATA");
            newEntity.setEmb07b_emb04f_str17("emb04_string11 DATA");
            newEntity.setEmb07b_emb04f_str18("emb04_string12 DATA");

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing persist(" + newEntity + ") operation");
            em.persist(newEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity07 findEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(em.contains(findEntity));
            Assert.assertEquals(findEntity.getId(), id);
            Assert.assertEquals(findEntity.getEnt07_str01(), "string01 DATA");
            Assert.assertEquals(findEntity.getEnt07_str02(), "string02 DATA");
            Assert.assertEquals(findEntity.getEnt07_str03(), "string03 DATA");
            Assert.assertEquals(findEntity.getEmb01_int01(), 1);
            Assert.assertEquals(findEntity.getEmb01_int02(), 2);
            Assert.assertEquals(findEntity.getEmb01_int03(), 3);
            Assert.assertEquals(findEntity.getEmb04a_int01(), 4);
            Assert.assertEquals(findEntity.getEmb04a_int02(), 5);
            Assert.assertEquals(findEntity.getEmb04a_int03(), 6);
            Assert.assertEquals(findEntity.getEmb04b_str04(), "string04 DATA");
            Assert.assertEquals(findEntity.getEmb04b_str05(), "string05 DATA");
            Assert.assertEquals(findEntity.getEmb04b_str06(), "string06 DATA");
            Assert.assertEquals(findEntity.getEmb04c_int07(), 7);
            Assert.assertEquals(findEntity.getEmb04c_int08(), 8);
            Assert.assertEquals(findEntity.getEmb04c_int09(), 9);
            Assert.assertEquals(findEntity.getEmb04d_str10(), "string07 DATA");
            Assert.assertEquals(findEntity.getEmb04d_str11(), "string08 DATA");
            Assert.assertEquals(findEntity.getEmb04d_str12(), "string09 DATA");
            Assert.assertEquals(findEntity.getEmb04e_int13(), 10);
            Assert.assertEquals(findEntity.getEmb04e_int14(), 11);
            Assert.assertEquals(findEntity.getEmb04e_int15(), 12);
            Assert.assertEquals(findEntity.getEmb04f_str16(), "string10 DATA");
            Assert.assertEquals(findEntity.getEmb04f_str17(), "string11 DATA");
            Assert.assertEquals(findEntity.getEmb04f_str18(), "string12 DATA");
            Assert.assertEquals(findEntity.getEmb07a_int01(), 13);
            Assert.assertEquals(findEntity.getEmb07a_int02(), 14);
            Assert.assertEquals(findEntity.getEmb07a_int03(), 15);
            Assert.assertEquals(findEntity.getEmb07a_emb01_int01(), 16);
            Assert.assertEquals(findEntity.getEmb07a_emb01_int02(), 17);
            Assert.assertEquals(findEntity.getEmb07a_emb01_int03(), 18);
            Assert.assertEquals(findEntity.getEmb07b_int04(), 19);
            Assert.assertEquals(findEntity.getEmb07b_int05(), 20);
            Assert.assertEquals(findEntity.getEmb07b_int06(), 21);
            Assert.assertEquals(findEntity.getEmb07b_emb04a_int01(), 22);
            Assert.assertEquals(findEntity.getEmb07b_emb04a_int02(), 23);
            Assert.assertEquals(findEntity.getEmb07b_emb04a_int03(), 24);
            Assert.assertEquals(findEntity.getEmb07b_emb04b_str04(), "emb04_string04 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04b_str05(), "emb04_string05 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04b_str06(), "emb04_string06 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04c_int07(), 25);
            Assert.assertEquals(findEntity.getEmb07b_emb04c_int08(), 26);
            Assert.assertEquals(findEntity.getEmb07b_emb04c_int09(), 27);
            Assert.assertEquals(findEntity.getEmb07b_emb04d_str10(), "emb04_string07 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04d_str11(), "emb04_string08 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04d_str12(), "emb04_string09 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04e_int13(), 28);
            Assert.assertEquals(findEntity.getEmb07b_emb04e_int14(), 29);
            Assert.assertEquals(findEntity.getEmb07b_emb04e_int15(), 30);
            Assert.assertEquals(findEntity.getEmb07b_emb04f_str16(), "emb04_string10 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04f_str17(), "emb04_string11 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04f_str18(), "emb04_string12 DATA");

            // Update the entity
            findEntity.setEnt07_str01("UPDATED string01 DATA");
            findEntity.setEnt07_str02("UPDATED string02 DATA");
            findEntity.setEnt07_str03("UPDATED string03 DATA");
            findEntity.setEmb01_int01(31);
            findEntity.setEmb01_int02(32);
            findEntity.setEmb01_int03(33);
            findEntity.setEmb04a_int01(34);
            findEntity.setEmb04a_int02(35);
            findEntity.setEmb04a_int03(36);
            findEntity.setEmb04b_str04("UPDATED string04 DATA");
            findEntity.setEmb04b_str05("UPDATED string05 DATA");
            findEntity.setEmb04b_str06("UPDATED string06 DATA");
            findEntity.setEmb04c_int07(37);
            findEntity.setEmb04c_int08(38);
            findEntity.setEmb04c_int09(39);
            findEntity.setEmb04d_str10("UPDATED string07 DATA");
            findEntity.setEmb04d_str11("UPDATED string08 DATA");
            findEntity.setEmb04d_str12("UPDATED string09 DATA");
            findEntity.setEmb04e_int13(40);
            findEntity.setEmb04e_int14(41);
            findEntity.setEmb04e_int15(42);
            findEntity.setEmb04f_str16("UPDATED string10 DATA");
            findEntity.setEmb04f_str17("UPDATED string11 DATA");
            findEntity.setEmb04f_str18("UPDATED string12 DATA");
            findEntity.setEmb07a_int01(43);
            findEntity.setEmb07a_int02(44);
            findEntity.setEmb07a_int03(45);
            findEntity.setEmb07a_emb01_int01(46);
            findEntity.setEmb07a_emb01_int02(47);
            findEntity.setEmb07a_emb01_int03(48);
            findEntity.setEmb07b_int04(49);
            findEntity.setEmb07b_int05(50);
            findEntity.setEmb07b_int06(51);
            findEntity.setEmb07b_emb04a_int01(52);
            findEntity.setEmb07b_emb04a_int02(53);
            findEntity.setEmb07b_emb04a_int03(54);
            findEntity.setEmb07b_emb04b_str04("UPDATED emb04_string04 DATA");
            findEntity.setEmb07b_emb04b_str05("UPDATED emb04_string05 DATA");
            findEntity.setEmb07b_emb04b_str06("UPDATED emb04_string06 DATA");
            findEntity.setEmb07b_emb04c_int07(55);
            findEntity.setEmb07b_emb04c_int08(56);
            findEntity.setEmb07b_emb04c_int09(57);
            findEntity.setEmb07b_emb04d_str10("UPDATED emb04_string07 DATA");
            findEntity.setEmb07b_emb04d_str11("UPDATED emb04_string08 DATA");
            findEntity.setEmb07b_emb04d_str12("UPDATED emb04_string09 DATA");
            findEntity.setEmb07b_emb04e_int13(58);
            findEntity.setEmb07b_emb04e_int14(59);
            findEntity.setEmb07b_emb04e_int15(60);
            findEntity.setEmb07b_emb04f_str16("UPDATED emb04_string10 DATA");
            findEntity.setEmb07b_emb04f_str17("UPDATED emb04_string11 DATA");
            findEntity.setEmb07b_emb04f_str18("UPDATED emb04_string12 DATA");

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity update was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity07 updatedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", updatedEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedEntity);
            Assert.assertTrue(findEntity != updatedEntity);
            Assert.assertTrue(em.contains(updatedEntity));
            Assert.assertEquals(updatedEntity.getId(), id);
            Assert.assertEquals(updatedEntity.getEnt07_str01(), "UPDATED string01 DATA");
            Assert.assertEquals(updatedEntity.getEnt07_str02(), "UPDATED string02 DATA");
            Assert.assertEquals(updatedEntity.getEnt07_str03(), "UPDATED string03 DATA");
            Assert.assertEquals(updatedEntity.getEmb01_int01(), 31);
            Assert.assertEquals(updatedEntity.getEmb01_int02(), 32);
            Assert.assertEquals(updatedEntity.getEmb01_int03(), 33);
            Assert.assertEquals(updatedEntity.getEmb04a_int01(), 34);
            Assert.assertEquals(updatedEntity.getEmb04a_int02(), 35);
            Assert.assertEquals(updatedEntity.getEmb04a_int03(), 36);
            Assert.assertEquals(updatedEntity.getEmb04b_str04(), "UPDATED string04 DATA");
            Assert.assertEquals(updatedEntity.getEmb04b_str05(), "UPDATED string05 DATA");
            Assert.assertEquals(updatedEntity.getEmb04b_str06(), "UPDATED string06 DATA");
            Assert.assertEquals(updatedEntity.getEmb04c_int07(), 37);
            Assert.assertEquals(updatedEntity.getEmb04c_int08(), 38);
            Assert.assertEquals(updatedEntity.getEmb04c_int09(), 39);
            Assert.assertEquals(updatedEntity.getEmb04d_str10(), "UPDATED string07 DATA");
            Assert.assertEquals(updatedEntity.getEmb04d_str11(), "UPDATED string08 DATA");
            Assert.assertEquals(updatedEntity.getEmb04d_str12(), "UPDATED string09 DATA");
            Assert.assertEquals(updatedEntity.getEmb04e_int13(), 40);
            Assert.assertEquals(updatedEntity.getEmb04e_int14(), 41);
            Assert.assertEquals(updatedEntity.getEmb04e_int15(), 42);
            Assert.assertEquals(updatedEntity.getEmb04f_str16(), "UPDATED string10 DATA");
            Assert.assertEquals(updatedEntity.getEmb04f_str17(), "UPDATED string11 DATA");
            Assert.assertEquals(updatedEntity.getEmb04f_str18(), "UPDATED string12 DATA");
            Assert.assertEquals(updatedEntity.getEmb07a_int01(), 43);
            Assert.assertEquals(updatedEntity.getEmb07a_int02(), 44);
            Assert.assertEquals(updatedEntity.getEmb07a_int03(), 45);
            Assert.assertEquals(findEntity.getEmb07a_emb01_int01(), 46);
            Assert.assertEquals(findEntity.getEmb07a_emb01_int02(), 47);
            Assert.assertEquals(findEntity.getEmb07a_emb01_int03(), 48);
            Assert.assertEquals(updatedEntity.getEmb07b_int04(), 49);
            Assert.assertEquals(updatedEntity.getEmb07b_int05(), 50);
            Assert.assertEquals(updatedEntity.getEmb07b_int06(), 51);
            Assert.assertEquals(findEntity.getEmb07b_emb04a_int01(), 52);
            Assert.assertEquals(findEntity.getEmb07b_emb04a_int02(), 53);
            Assert.assertEquals(findEntity.getEmb07b_emb04a_int03(), 54);
            Assert.assertEquals(findEntity.getEmb07b_emb04b_str04(), "UPDATED emb04_string04 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04b_str05(), "UPDATED emb04_string05 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04b_str06(), "UPDATED emb04_string06 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04c_int07(), 55);
            Assert.assertEquals(findEntity.getEmb07b_emb04c_int08(), 56);
            Assert.assertEquals(findEntity.getEmb07b_emb04c_int09(), 57);
            Assert.assertEquals(findEntity.getEmb07b_emb04d_str10(), "UPDATED emb04_string07 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04d_str11(), "UPDATED emb04_string08 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04d_str12(), "UPDATED emb04_string09 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04e_int13(), 58);
            Assert.assertEquals(findEntity.getEmb07b_emb04e_int14(), 59);
            Assert.assertEquals(findEntity.getEmb07b_emb04e_int15(), 60);
            Assert.assertEquals(findEntity.getEmb07b_emb04f_str16(), "UPDATED emb04_string10 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04f_str17(), "UPDATED emb04_string11 DATA");
            Assert.assertEquals(findEntity.getEmb07b_emb04f_str18(), "UPDATED emb04_string12 DATA");

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedEntity + ") operation");
            em.remove(updatedEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            Object findRemovedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNull("find(" + newEntity.getClass() + ", " + id + ") returned an entity.", findRemovedEntity);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Test Logic: testScenario08
     *
     * Description: Test of single entity with a collection of embeddables
     *
     * Performs these basic CRUD operations:
     *
     * 01. Create a new instance of the entity class
     * 02. Persist the new entity in the database
     * 03. Verify the entity was saved in the database
     * 04. Update the entity
     * 05. Verify the updated entity was saved in the database
     * 06. Delete the entity from the database
     * 07. Verify the deletion was successful
     *
     * UML:
     * +---------------------+
     * | Set<Embeddable01> |
     * +----------+----------+
     * 1 |
     * |
     * |
     * 1 <*>
     * +--------+--------+
     * | Entity08 |
     * +-----------------+
     *
     */
    public void testEmbeddableNested11(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 8;

            IEntity08 newEntity = new Entity08();
            newEntity.setId(id);
            newEntity.setEnt08_str01("string01 DATA");
            newEntity.setEnt08_str02("string02 DATA");
            newEntity.setEnt08_str03("string03 DATA");
            Set<String> stringSet = new HashSet<String>();
            stringSet.add("element01");
            stringSet.add("element02");
            stringSet.add("element03");
            stringSet.add("element04");
            stringSet.add("element05");
            stringSet.add("element06");
            stringSet.add("element07");
            stringSet.add("element08");
            stringSet.add("element09");
            newEntity.setEnt08_set01(stringSet);
            Set<Integer> integerSet = new HashSet<Integer>();
            integerSet.add(1);
            integerSet.add(2);
            integerSet.add(3);
            integerSet.add(4);
            integerSet.add(5);
            integerSet.add(6);
            integerSet.add(7);
            integerSet.add(8);
            integerSet.add(9);
            integerSet.add(10);
            newEntity.setEnt08_set02(integerSet);
            Embeddable01 newEmbeddable01a = new Embeddable01();
            Embeddable01 newEmbeddable01b = new Embeddable01();
            Embeddable01 newEmbeddable01c = new Embeddable01();
            Embeddable01 newEmbeddable01d = new Embeddable01();
            Embeddable01 newEmbeddable01e = new Embeddable01();
            Embeddable01 newEmbeddable01f = new Embeddable01();
            newEmbeddable01a.setEmb01_int01(1);
            newEmbeddable01a.setEmb01_int02(2);
            newEmbeddable01a.setEmb01_int03(3);
            newEmbeddable01b.setEmb01_int01(4);
            newEmbeddable01b.setEmb01_int02(5);
            newEmbeddable01b.setEmb01_int03(6);
            newEmbeddable01c.setEmb01_int01(7);
            newEmbeddable01c.setEmb01_int02(8);
            newEmbeddable01c.setEmb01_int03(9);
            newEmbeddable01d.setEmb01_int01(10);
            newEmbeddable01d.setEmb01_int02(11);
            newEmbeddable01d.setEmb01_int03(12);
            newEmbeddable01e.setEmb01_int01(13);
            newEmbeddable01e.setEmb01_int02(14);
            newEmbeddable01e.setEmb01_int03(15);
            newEmbeddable01f.setEmb01_int01(16);
            newEmbeddable01f.setEmb01_int02(17);
            newEmbeddable01f.setEmb01_int03(18);
            Set<Embeddable01> embeddable01Set = new HashSet<Embeddable01>();
            embeddable01Set.add(newEmbeddable01a);
            embeddable01Set.add(newEmbeddable01b);
            embeddable01Set.add(newEmbeddable01c);
            embeddable01Set.add(newEmbeddable01d);
            embeddable01Set.add(newEmbeddable01e);
            embeddable01Set.add(newEmbeddable01f);
            newEntity.setEnt08_set03(embeddable01Set);

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing persist(" + newEntity + ") operation");
            em.persist(newEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity08 findEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(em.contains(findEntity));
            Assert.assertEquals(findEntity.getId(), id);
            Assert.assertEquals(findEntity.getEnt08_str01(), "string01 DATA");
            Assert.assertEquals(findEntity.getEnt08_str02(), "string02 DATA");
            Assert.assertEquals(findEntity.getEnt08_str03(), "string03 DATA");

            Set<String> findSet01 = findEntity.getEnt08_set01();
            Assert.assertEquals(findSet01.size(), stringSet.size());
            Assert.assertTrue(findSet01.containsAll(stringSet));

            Set<Integer> findSet02 = findEntity.getEnt08_set02();
            Assert.assertEquals(findSet02.size(), integerSet.size());
            Assert.assertTrue(findSet02.containsAll(integerSet));

            Set<Embeddable01> findSet03 = findEntity.getEnt08_set03();
            Assert.assertEquals(findSet03.size(), embeddable01Set.size());
            Assert.assertTrue(findSet03.containsAll(embeddable01Set));

            // Update the entity
            findEntity.setEnt08_str01("UPDATED string01 DATA");
            findEntity.setEnt08_str02("UPDATED string02 DATA");
            findEntity.setEnt08_str03("UPDATED string03 DATA");
            findSet01 = findEntity.getEnt08_set01();
            findSet01.remove("element01");
            findSet01.remove("element03");
            findSet01.remove("element05");
            findSet01.remove("element07");
            findSet01.remove("element09");
            findSet01.add("element10");
            findSet01.add("element11");
            findSet01.add("element12");
            findEntity.setEnt08_set01(findSet01);
            findSet02 = findEntity.getEnt08_set02();
            findSet02.clear();
            findEntity.setEnt08_set02(findSet02);
            Set<Embeddable01> tempSet03 = new HashSet<Embeddable01>();
            for (Embeddable01 embeddable01 : findSet03) {
                if (embeddable01.getEmb01_int01() == 1) {
                    embeddable01.setEmb01_int01(8001);
                    embeddable01.setEmb01_int02(8002);
                    embeddable01.setEmb01_int03(8003);
                    tempSet03.add(embeddable01);
                }
                if (embeddable01.getEmb01_int01() == 7) {
                    tempSet03.add(embeddable01);
                }
                if (embeddable01.getEmb01_int01() == 13) {
                    tempSet03.add(embeddable01);
                }
            }
            findEntity.setEnt08_set03(tempSet03);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity update was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity08 updatedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", updatedEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedEntity);
            Assert.assertTrue(findEntity != updatedEntity);
            Assert.assertTrue(em.contains(updatedEntity));
            Assert.assertEquals(updatedEntity.getId(), id);
            Assert.assertEquals(updatedEntity.getEnt08_str01(), "UPDATED string01 DATA");
            Assert.assertEquals(updatedEntity.getEnt08_str02(), "UPDATED string02 DATA");
            Assert.assertEquals(updatedEntity.getEnt08_str03(), "UPDATED string03 DATA");

            Set<String> updatedSet01 = updatedEntity.getEnt08_set01();
            Assert.assertEquals(updatedSet01.size(), findSet01.size());
            Assert.assertTrue(updatedSet01.containsAll(findSet01));

            Set<Integer> updatedSet02 = updatedEntity.getEnt08_set02();
            Assert.assertEquals(updatedSet02.size(), findSet02.size());
            Assert.assertTrue(updatedSet02.containsAll(findSet02));

            Set<Embeddable01> updatedSet03 = updatedEntity.getEnt08_set03();
            Assert.assertEquals(updatedSet03.size(), tempSet03.size());
            //
            // assertTrue( "Assert for the entity field ent08_set03 contents",
            //                      updatedSet03.containsAll(tempSet03));
            //
            // Should be able to verify the contents of updatedSet03 using tempSet03 without having to iterate through
            // the entire set. But for some reason tempSet03 is getting zero'ed out as a result of the update commit.
            //
            for (Embeddable01 embeddable01 : updatedSet03) {
                if (embeddable01.getEmb01_int01() == 8001) {
                    Assert.assertEquals(embeddable01.getEmb01_int01(), 8001);
                    Assert.assertEquals(embeddable01.getEmb01_int02(), 8002);
                    Assert.assertEquals(embeddable01.getEmb01_int03(), 8003);
                } else if (embeddable01.getEmb01_int01() == 7) {
                    Assert.assertEquals(embeddable01.getEmb01_int01(), 7);
                    Assert.assertEquals(embeddable01.getEmb01_int02(), 8);
                    Assert.assertEquals(embeddable01.getEmb01_int03(), 9);
                } else if (embeddable01.getEmb01_int01() == 13) {
                    Assert.assertEquals(embeddable01.getEmb01_int01(), 13);
                    Assert.assertEquals(embeddable01.getEmb01_int02(), 14);
                    Assert.assertEquals(embeddable01.getEmb01_int03(), 15);
                } else {
                    Assert.fail();
                }
            }

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedEntity + ") operation");
            em.remove(updatedEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            Object findRemovedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNull("find(" + newEntity.getClass() + ", " + id + ") returned an entity.", findRemovedEntity);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testEmbeddableNested12(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 8;

            IEntity08 newEntity = new XMLEntity08();
            newEntity.setId(id);
            newEntity.setEnt08_str01("string01 DATA");
            newEntity.setEnt08_str02("string02 DATA");
            newEntity.setEnt08_str03("string03 DATA");
            Set<String> stringSet = new HashSet<String>();
            stringSet.add("element01");
            stringSet.add("element02");
            stringSet.add("element03");
            stringSet.add("element04");
            stringSet.add("element05");
            stringSet.add("element06");
            stringSet.add("element07");
            stringSet.add("element08");
            stringSet.add("element09");
            newEntity.setEnt08_set01(stringSet);
            Set<Integer> integerSet = new HashSet<Integer>();
            integerSet.add(1);
            integerSet.add(2);
            integerSet.add(3);
            integerSet.add(4);
            integerSet.add(5);
            integerSet.add(6);
            integerSet.add(7);
            integerSet.add(8);
            integerSet.add(9);
            integerSet.add(10);
            newEntity.setEnt08_set02(integerSet);
            Embeddable01 newEmbeddable01a = new Embeddable01();
            Embeddable01 newEmbeddable01b = new Embeddable01();
            Embeddable01 newEmbeddable01c = new Embeddable01();
            Embeddable01 newEmbeddable01d = new Embeddable01();
            Embeddable01 newEmbeddable01e = new Embeddable01();
            Embeddable01 newEmbeddable01f = new Embeddable01();
            newEmbeddable01a.setEmb01_int01(1);
            newEmbeddable01a.setEmb01_int02(2);
            newEmbeddable01a.setEmb01_int03(3);
            newEmbeddable01b.setEmb01_int01(4);
            newEmbeddable01b.setEmb01_int02(5);
            newEmbeddable01b.setEmb01_int03(6);
            newEmbeddable01c.setEmb01_int01(7);
            newEmbeddable01c.setEmb01_int02(8);
            newEmbeddable01c.setEmb01_int03(9);
            newEmbeddable01d.setEmb01_int01(10);
            newEmbeddable01d.setEmb01_int02(11);
            newEmbeddable01d.setEmb01_int03(12);
            newEmbeddable01e.setEmb01_int01(13);
            newEmbeddable01e.setEmb01_int02(14);
            newEmbeddable01e.setEmb01_int03(15);
            newEmbeddable01f.setEmb01_int01(16);
            newEmbeddable01f.setEmb01_int02(17);
            newEmbeddable01f.setEmb01_int03(18);
            Set<Embeddable01> embeddable01Set = new HashSet<Embeddable01>();
            embeddable01Set.add(newEmbeddable01a);
            embeddable01Set.add(newEmbeddable01b);
            embeddable01Set.add(newEmbeddable01c);
            embeddable01Set.add(newEmbeddable01d);
            embeddable01Set.add(newEmbeddable01e);
            embeddable01Set.add(newEmbeddable01f);
            newEntity.setEnt08_set03(embeddable01Set);

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing persist(" + newEntity + ") operation");
            em.persist(newEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity08 findEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(em.contains(findEntity));
            Assert.assertEquals(findEntity.getId(), id);
            Assert.assertEquals(findEntity.getEnt08_str01(), "string01 DATA");
            Assert.assertEquals(findEntity.getEnt08_str02(), "string02 DATA");
            Assert.assertEquals(findEntity.getEnt08_str03(), "string03 DATA");

            Set<String> findSet01 = findEntity.getEnt08_set01();
            Assert.assertEquals(findSet01.size(), stringSet.size());
            Assert.assertTrue(findSet01.containsAll(stringSet));

            Set<Integer> findSet02 = findEntity.getEnt08_set02();
            Assert.assertEquals(findSet02.size(), integerSet.size());
            Assert.assertTrue(findSet02.containsAll(integerSet));

            Set<Embeddable01> findSet03 = findEntity.getEnt08_set03();
            Assert.assertEquals(findSet03.size(), embeddable01Set.size());
            Assert.assertTrue(findSet03.containsAll(embeddable01Set));

            // Update the entity
            findEntity.setEnt08_str01("UPDATED string01 DATA");
            findEntity.setEnt08_str02("UPDATED string02 DATA");
            findEntity.setEnt08_str03("UPDATED string03 DATA");
            findSet01 = findEntity.getEnt08_set01();
            findSet01.remove("element01");
            findSet01.remove("element03");
            findSet01.remove("element05");
            findSet01.remove("element07");
            findSet01.remove("element09");
            findSet01.add("element10");
            findSet01.add("element11");
            findSet01.add("element12");
            findEntity.setEnt08_set01(findSet01);
            findSet02 = findEntity.getEnt08_set02();
            findSet02.clear();
            findEntity.setEnt08_set02(findSet02);
            Set<Embeddable01> tempSet03 = new HashSet<Embeddable01>();
            for (Embeddable01 embeddable01 : findSet03) {
                if (embeddable01.getEmb01_int01() == 1) {
                    embeddable01.setEmb01_int01(8001);
                    embeddable01.setEmb01_int02(8002);
                    embeddable01.setEmb01_int03(8003);
                    tempSet03.add(embeddable01);
                }
                if (embeddable01.getEmb01_int01() == 7) {
                    tempSet03.add(embeddable01);
                }
                if (embeddable01.getEmb01_int01() == 13) {
                    tempSet03.add(embeddable01);
                }
            }
            findEntity.setEnt08_set03(tempSet03);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity update was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity08 updatedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", updatedEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedEntity);
            Assert.assertTrue(findEntity != updatedEntity);
            Assert.assertTrue(em.contains(updatedEntity));
            Assert.assertEquals(updatedEntity.getId(), id);
            Assert.assertEquals(updatedEntity.getEnt08_str01(), "UPDATED string01 DATA");
            Assert.assertEquals(updatedEntity.getEnt08_str02(), "UPDATED string02 DATA");
            Assert.assertEquals(updatedEntity.getEnt08_str03(), "UPDATED string03 DATA");

            Set<String> updatedSet01 = updatedEntity.getEnt08_set01();
            Assert.assertEquals(updatedSet01.size(), findSet01.size());
            Assert.assertTrue(updatedSet01.containsAll(findSet01));

            Set<Integer> updatedSet02 = updatedEntity.getEnt08_set02();
            Assert.assertEquals(updatedSet02.size(), findSet02.size());
            Assert.assertTrue(updatedSet02.containsAll(findSet02));

            Set<Embeddable01> updatedSet03 = updatedEntity.getEnt08_set03();
            Assert.assertEquals(updatedSet03.size(), tempSet03.size());
            //
            // assertTrue( "Assert for the entity field ent08_set03 contents",
            //                      updatedSet03.containsAll(tempSet03));
            //
            // Should be able to verify the contents of updatedSet03 using tempSet03 without having to iterate through
            // the entire set. But for some reason tempSet03 is getting zero'ed out as a result of the update commit.
            //
            for (Embeddable01 embeddable01 : updatedSet03) {
                if (embeddable01.getEmb01_int01() == 8001) {
                    Assert.assertEquals(embeddable01.getEmb01_int01(), 8001);
                    Assert.assertEquals(embeddable01.getEmb01_int02(), 8002);
                    Assert.assertEquals(embeddable01.getEmb01_int03(), 8003);
                } else if (embeddable01.getEmb01_int01() == 7) {
                    Assert.assertEquals(embeddable01.getEmb01_int01(), 7);
                    Assert.assertEquals(embeddable01.getEmb01_int02(), 8);
                    Assert.assertEquals(embeddable01.getEmb01_int03(), 9);
                } else if (embeddable01.getEmb01_int01() == 13) {
                    Assert.assertEquals(embeddable01.getEmb01_int01(), 13);
                    Assert.assertEquals(embeddable01.getEmb01_int02(), 14);
                    Assert.assertEquals(embeddable01.getEmb01_int03(), 15);
                } else {
                    Assert.fail();
                }
            }

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedEntity + ") operation");
            em.remove(updatedEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            Object findRemovedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNull("find(" + newEntity.getClass() + ", " + id + ") returned an entity.", findRemovedEntity);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Test Logic: testScenario09
     *
     * Description: Test of single entity with a collection of embeddables
     *
     * Performs these basic CRUD operations:
     *
     * 01. Create a new instance of the entity class
     * 02. Persist the new entity in the database
     * 03. Verify the entity was saved in the database
     * 04. Update the entity
     * 05. Verify the updated entity was saved in the database
     * 06. Delete the entity from the database
     * 07. Verify the deletion was successful
     *
     * UML:
     * +----------------------+
     * | List<Embeddable01> |
     * +----------+-----------+
     * 1 |
     * |
     * |
     * 1 <*>
     * +--------+--------+
     * | Entity09 |
     * +-----------------+
     *
     */
    public void testEmbeddableNested13(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 9;

            IEntity09 newEntity = new Entity09();
            newEntity.setId(id);
            newEntity.setEnt09_str01("string01 DATA");
            newEntity.setEnt09_str02("string02 DATA");
            newEntity.setEnt09_str03("string03 DATA");
            List<String> stringList = new ArrayList<String>();
            stringList.add("element01");
            stringList.add("element02");
            stringList.add("element03");
            stringList.add("element04");
            stringList.add("element05");
            stringList.add("element06");
            stringList.add("element07");
            stringList.add("element08");
            stringList.add("element09");
            newEntity.setEnt09_list01(stringList);
            List<Integer> integerList = new ArrayList<Integer>();
            integerList.add(1);
            integerList.add(2);
            integerList.add(3);
            integerList.add(4);
            integerList.add(5);
            integerList.add(6);
            integerList.add(7);
            integerList.add(8);
            integerList.add(9);
            integerList.add(10);
            newEntity.setEnt09_list02(integerList);
            Embeddable01 newEmbeddable01a = new Embeddable01();
            Embeddable01 newEmbeddable01b = new Embeddable01();
            Embeddable01 newEmbeddable01c = new Embeddable01();
            Embeddable01 newEmbeddable01d = new Embeddable01();
            Embeddable01 newEmbeddable01e = new Embeddable01();
            Embeddable01 newEmbeddable01f = new Embeddable01();
            newEmbeddable01a.setEmb01_int01(1);
            newEmbeddable01a.setEmb01_int02(2);
            newEmbeddable01a.setEmb01_int03(3);
            newEmbeddable01b.setEmb01_int01(4);
            newEmbeddable01b.setEmb01_int02(5);
            newEmbeddable01b.setEmb01_int03(6);
            newEmbeddable01c.setEmb01_int01(7);
            newEmbeddable01c.setEmb01_int02(8);
            newEmbeddable01c.setEmb01_int03(9);
            newEmbeddable01d.setEmb01_int01(10);
            newEmbeddable01d.setEmb01_int02(11);
            newEmbeddable01d.setEmb01_int03(12);
            newEmbeddable01e.setEmb01_int01(13);
            newEmbeddable01e.setEmb01_int02(14);
            newEmbeddable01e.setEmb01_int03(15);
            newEmbeddable01f.setEmb01_int01(16);
            newEmbeddable01f.setEmb01_int02(17);
            newEmbeddable01f.setEmb01_int03(18);
            List<Embeddable01> embeddable01List = new ArrayList<Embeddable01>();
            embeddable01List.add(newEmbeddable01a);
            embeddable01List.add(newEmbeddable01b);
            embeddable01List.add(newEmbeddable01c);
            embeddable01List.add(newEmbeddable01d);
            embeddable01List.add(newEmbeddable01e);
            embeddable01List.add(newEmbeddable01f);
            newEntity.setEnt09_list03(embeddable01List);

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing persist(" + newEntity + ") operation");
            em.persist(newEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity09 findEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(em.contains(findEntity));
            Assert.assertEquals(findEntity.getId(), id);
            Assert.assertEquals(findEntity.getEnt09_str01(), "string01 DATA");
            Assert.assertEquals(findEntity.getEnt09_str02(), "string02 DATA");
            Assert.assertEquals(findEntity.getEnt09_str03(), "string03 DATA");

            List<String> findList01 = findEntity.getEnt09_list01();
            Assert.assertEquals(findList01.size(), stringList.size());
            Assert.assertTrue(findList01.containsAll(stringList));

            List<Integer> findList02 = findEntity.getEnt09_list02();
            Assert.assertEquals(findList02.size(), integerList.size());
            Assert.assertTrue(findList02.containsAll(integerList));

            List<Embeddable01> findList03 = findEntity.getEnt09_list03();
            Assert.assertEquals(findList03.size(), embeddable01List.size());
            Assert.assertTrue(findList03.containsAll(embeddable01List));
            //
            // Ensure order is based on @OrderBy("emb01_int03 DESC")
            //
            int element = 0;
            Iterator<Embeddable01> iterator = findList03.iterator();
            while (iterator.hasNext()) {
                element++;
                Embeddable01 embeddable01 = iterator.next();
                switch (element) {
                    case 1:
                        Assert.assertEquals(embeddable01.getEmb01_int03(), 18);
                        break;
                    case 2:
                        Assert.assertEquals(embeddable01.getEmb01_int03(), 15);
                        break;
                    case 3:
                        Assert.assertEquals(embeddable01.getEmb01_int03(), 12);
                        break;
                    case 4:
                        Assert.assertEquals(embeddable01.getEmb01_int03(), 9);
                        break;
                    case 5:
                        Assert.assertEquals(embeddable01.getEmb01_int03(), 6);
                        break;
                    case 6:
                        Assert.assertEquals(embeddable01.getEmb01_int03(), 3);
                        break;
                    default:
                        Assert.fail();
                        break;
                }
            }

            // Update the entity
            findEntity.setEnt09_str01("UPDATED string01 DATA");
            findEntity.setEnt09_str02("UPDATED string02 DATA");
            findEntity.setEnt09_str03("UPDATED string03 DATA");
            findList01 = findEntity.getEnt09_list01();
            findList01.remove("element01");
            findList01.remove("element03");
            findList01.remove("element05");
            findList01.remove("element07");
            findList01.remove("element09");
            findList01.add("element10");
            findList01.add("element11");
            findList01.add("element12");
            findEntity.setEnt09_list01(findList01);
            findList02 = findEntity.getEnt09_list02();
            findList02.clear();
            findEntity.setEnt09_list02(findList02);
            List<Embeddable01> tempList03 = new ArrayList<Embeddable01>();
            for (Embeddable01 embeddable01 : findList03) {
                if (embeddable01.getEmb01_int01() == 1) {
                    embeddable01.setEmb01_int01(9001);
                    embeddable01.setEmb01_int02(9002);
                    embeddable01.setEmb01_int03(9003);
                    tempList03.add(embeddable01);
                }
                if (embeddable01.getEmb01_int01() == 7) {
                    tempList03.add(embeddable01);
                }
                if (embeddable01.getEmb01_int01() == 13) {
                    tempList03.add(embeddable01);
                }
            }
            findEntity.setEnt09_list03(tempList03);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity update was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity09 updatedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", updatedEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedEntity);
            Assert.assertTrue(findEntity != updatedEntity);
            Assert.assertTrue(em.contains(updatedEntity));
            Assert.assertEquals(updatedEntity.getId(), id);
            Assert.assertEquals(updatedEntity.getEnt09_str01(), "UPDATED string01 DATA");
            Assert.assertEquals(updatedEntity.getEnt09_str02(), "UPDATED string02 DATA");
            Assert.assertEquals(updatedEntity.getEnt09_str03(), "UPDATED string03 DATA");

            List<String> updatedList01 = updatedEntity.getEnt09_list01();
            Assert.assertEquals(updatedList01.size(), findList01.size());
            Assert.assertTrue(updatedList01.containsAll(findList01));

            List<Integer> updatedList02 = updatedEntity.getEnt09_list02();
            Assert.assertEquals(updatedList02.size(), findList02.size());
            Assert.assertTrue(updatedList02.containsAll(findList02));

            List<Embeddable01> updatedList03 = updatedEntity.getEnt09_list03();
            Assert.assertEquals(updatedList03.size(), tempList03.size());
            //
            // Ensure order is based on @OrderBy("emb01_int03 DESC")
            // and ensure contents have changed properly
            //
            element = 0;
            iterator = updatedList03.iterator();
            while (iterator.hasNext()) {
                element++;
                Embeddable01 embeddable01 = iterator.next();
                switch (element) {
                    case 1:
                        Assert.assertEquals(embeddable01.getEmb01_int01(), 9001);
                        Assert.assertEquals(embeddable01.getEmb01_int02(), 9002);
                        Assert.assertEquals(embeddable01.getEmb01_int03(), 9003);
                        break;
                    case 2:
                        Assert.assertEquals(embeddable01.getEmb01_int01(), 13);
                        Assert.assertEquals(embeddable01.getEmb01_int02(), 14);
                        Assert.assertEquals(embeddable01.getEmb01_int03(), 15);
                        break;
                    case 3:
                        Assert.assertEquals(embeddable01.getEmb01_int01(), 7);
                        Assert.assertEquals(embeddable01.getEmb01_int02(), 8);
                        Assert.assertEquals(embeddable01.getEmb01_int03(), 9);
                        break;
                    default:
                        Assert.fail();
                        break;
                }
            }

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedEntity + ") operation");
            em.remove(updatedEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            Object findRemovedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNull("find(" + newEntity.getClass() + ", " + id + ") returned an entity.", findRemovedEntity);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testEmbeddableNested14(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 9;

            IEntity09 newEntity = new XMLEntity09();
            newEntity.setId(id);
            newEntity.setEnt09_str01("string01 DATA");
            newEntity.setEnt09_str02("string02 DATA");
            newEntity.setEnt09_str03("string03 DATA");
            List<String> stringList = new ArrayList<String>();
            stringList.add("element01");
            stringList.add("element02");
            stringList.add("element03");
            stringList.add("element04");
            stringList.add("element05");
            stringList.add("element06");
            stringList.add("element07");
            stringList.add("element08");
            stringList.add("element09");
            newEntity.setEnt09_list01(stringList);
            List<Integer> integerList = new ArrayList<Integer>();
            integerList.add(1);
            integerList.add(2);
            integerList.add(3);
            integerList.add(4);
            integerList.add(5);
            integerList.add(6);
            integerList.add(7);
            integerList.add(8);
            integerList.add(9);
            integerList.add(10);
            newEntity.setEnt09_list02(integerList);
            Embeddable01 newEmbeddable01a = new Embeddable01();
            Embeddable01 newEmbeddable01b = new Embeddable01();
            Embeddable01 newEmbeddable01c = new Embeddable01();
            Embeddable01 newEmbeddable01d = new Embeddable01();
            Embeddable01 newEmbeddable01e = new Embeddable01();
            Embeddable01 newEmbeddable01f = new Embeddable01();
            newEmbeddable01a.setEmb01_int01(1);
            newEmbeddable01a.setEmb01_int02(2);
            newEmbeddable01a.setEmb01_int03(3);
            newEmbeddable01b.setEmb01_int01(4);
            newEmbeddable01b.setEmb01_int02(5);
            newEmbeddable01b.setEmb01_int03(6);
            newEmbeddable01c.setEmb01_int01(7);
            newEmbeddable01c.setEmb01_int02(8);
            newEmbeddable01c.setEmb01_int03(9);
            newEmbeddable01d.setEmb01_int01(10);
            newEmbeddable01d.setEmb01_int02(11);
            newEmbeddable01d.setEmb01_int03(12);
            newEmbeddable01e.setEmb01_int01(13);
            newEmbeddable01e.setEmb01_int02(14);
            newEmbeddable01e.setEmb01_int03(15);
            newEmbeddable01f.setEmb01_int01(16);
            newEmbeddable01f.setEmb01_int02(17);
            newEmbeddable01f.setEmb01_int03(18);
            List<Embeddable01> embeddable01List = new ArrayList<Embeddable01>();
            embeddable01List.add(newEmbeddable01a);
            embeddable01List.add(newEmbeddable01b);
            embeddable01List.add(newEmbeddable01c);
            embeddable01List.add(newEmbeddable01d);
            embeddable01List.add(newEmbeddable01e);
            embeddable01List.add(newEmbeddable01f);
            newEntity.setEnt09_list03(embeddable01List);

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing persist(" + newEntity + ") operation");
            em.persist(newEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity09 findEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(em.contains(findEntity));
            Assert.assertEquals(findEntity.getId(), id);
            Assert.assertEquals(findEntity.getEnt09_str01(), "string01 DATA");
            Assert.assertEquals(findEntity.getEnt09_str02(), "string02 DATA");
            Assert.assertEquals(findEntity.getEnt09_str03(), "string03 DATA");

            List<String> findList01 = findEntity.getEnt09_list01();
            Assert.assertEquals(findList01.size(), stringList.size());
            Assert.assertTrue(findList01.containsAll(stringList));

            List<Integer> findList02 = findEntity.getEnt09_list02();
            Assert.assertEquals(findList02.size(), integerList.size());
            Assert.assertTrue(findList02.containsAll(integerList));

            List<Embeddable01> findList03 = findEntity.getEnt09_list03();
            Assert.assertEquals(findList03.size(), embeddable01List.size());
            Assert.assertTrue(findList03.containsAll(embeddable01List));
            //
            // Ensure order is based on @OrderBy("emb01_int03 DESC")
            //
            int element = 0;
            Iterator<Embeddable01> iterator = findList03.iterator();
            while (iterator.hasNext()) {
                element++;
                Embeddable01 embeddable01 = iterator.next();
                switch (element) {
                    case 1:
                        Assert.assertEquals(embeddable01.getEmb01_int03(), 18);
                        break;
                    case 2:
                        Assert.assertEquals(embeddable01.getEmb01_int03(), 15);
                        break;
                    case 3:
                        Assert.assertEquals(embeddable01.getEmb01_int03(), 12);
                        break;
                    case 4:
                        Assert.assertEquals(embeddable01.getEmb01_int03(), 9);
                        break;
                    case 5:
                        Assert.assertEquals(embeddable01.getEmb01_int03(), 6);
                        break;
                    case 6:
                        Assert.assertEquals(embeddable01.getEmb01_int03(), 3);
                        break;
                    default:
                        Assert.fail();
                        break;
                }
            }

            // Update the entity
            findEntity.setEnt09_str01("UPDATED string01 DATA");
            findEntity.setEnt09_str02("UPDATED string02 DATA");
            findEntity.setEnt09_str03("UPDATED string03 DATA");
            findList01 = findEntity.getEnt09_list01();
            findList01.remove("element01");
            findList01.remove("element03");
            findList01.remove("element05");
            findList01.remove("element07");
            findList01.remove("element09");
            findList01.add("element10");
            findList01.add("element11");
            findList01.add("element12");
            findEntity.setEnt09_list01(findList01);
            findList02 = findEntity.getEnt09_list02();
            findList02.clear();
            findEntity.setEnt09_list02(findList02);
            List<Embeddable01> tempList03 = new ArrayList<Embeddable01>();
            for (Embeddable01 embeddable01 : findList03) {
                if (embeddable01.getEmb01_int01() == 1) {
                    embeddable01.setEmb01_int01(9001);
                    embeddable01.setEmb01_int02(9002);
                    embeddable01.setEmb01_int03(9003);
                    tempList03.add(embeddable01);
                }
                if (embeddable01.getEmb01_int01() == 7) {
                    tempList03.add(embeddable01);
                }
                if (embeddable01.getEmb01_int01() == 13) {
                    tempList03.add(embeddable01);
                }
            }
            findEntity.setEnt09_list03(tempList03);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity update was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity09 updatedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", updatedEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedEntity);
            Assert.assertTrue(findEntity != updatedEntity);
            Assert.assertTrue(em.contains(updatedEntity));
            Assert.assertEquals(updatedEntity.getId(), id);
            Assert.assertEquals(updatedEntity.getEnt09_str01(), "UPDATED string01 DATA");
            Assert.assertEquals(updatedEntity.getEnt09_str02(), "UPDATED string02 DATA");
            Assert.assertEquals(updatedEntity.getEnt09_str03(), "UPDATED string03 DATA");

            List<String> updatedList01 = updatedEntity.getEnt09_list01();
            Assert.assertEquals(updatedList01.size(), findList01.size());
            Assert.assertTrue(updatedList01.containsAll(findList01));

            List<Integer> updatedList02 = updatedEntity.getEnt09_list02();
            Assert.assertEquals(updatedList02.size(), findList02.size());
            Assert.assertTrue(updatedList02.containsAll(findList02));

            List<Embeddable01> updatedList03 = updatedEntity.getEnt09_list03();
            Assert.assertEquals(updatedList03.size(), tempList03.size());
            //
            // Ensure order is based on @OrderBy("emb01_int03 DESC")
            // and ensure contents have changed properly
            //
            element = 0;
            iterator = updatedList03.iterator();
            while (iterator.hasNext()) {
                element++;
                Embeddable01 embeddable01 = iterator.next();
                switch (element) {
                    case 1:
                        Assert.assertEquals(embeddable01.getEmb01_int01(), 9001);
                        Assert.assertEquals(embeddable01.getEmb01_int02(), 9002);
                        Assert.assertEquals(embeddable01.getEmb01_int03(), 9003);
                        break;
                    case 2:
                        Assert.assertEquals(embeddable01.getEmb01_int01(), 13);
                        Assert.assertEquals(embeddable01.getEmb01_int02(), 14);
                        Assert.assertEquals(embeddable01.getEmb01_int03(), 15);
                        break;
                    case 3:
                        Assert.assertEquals(embeddable01.getEmb01_int01(), 7);
                        Assert.assertEquals(embeddable01.getEmb01_int02(), 8);
                        Assert.assertEquals(embeddable01.getEmb01_int03(), 9);
                        break;
                    default:
                        Assert.fail();
                        break;
                }
            }

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedEntity + ") operation");
            em.remove(updatedEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            Object findRemovedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNull("find(" + newEntity.getClass() + ", " + id + ") returned an entity.", findRemovedEntity);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Test Logic: testScenario10
     *
     * Description: Test of single entity with a collection of nested embeddables,
     * and nested embeddables of the same embeddable type
     *
     * Performs these basic CRUD operations:
     *
     * 01. Create a new instance of the entity class
     * 02. Persist the new entity in the database
     * 03. Verify the entity was saved in the database
     * 04. Update the entity
     * 05. Verify the updated entity was saved in the database
     * 06. Delete the entity from the database
     * 07. Verify the deletion was successful
     *
     * UML:
     * +-------------------------+
     * | Vector<Embeddable04a> |
     * +------------+------------+
     * 1 |
     * |
     * |
     * 1 <*>
     * +--------+--------+
     * | Entity10 |
     * +--------+--------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +--------+--------+
     * | Embeddable04a |
     * +--------+--------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +--------+--------+
     * | Embeddable04b |
     * +--------+--------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +--------+--------+
     * | Embeddable04c |
     * +--------+--------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +--------+--------+
     * | Embeddable04d |
     * +--------+--------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +--------+--------+
     * | Embeddable04e |
     * +--------+--------+
     * 1 <*>
     * |
     * |
     * 1 |
     * +--------+--------+
     * | Embeddable04f |
     * +-----------------+
     *
     */
    public void testEmbeddableNested15(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 10;

            IEntity10 newEntity = new Entity10();
            newEntity.setId(id);
            newEntity.setEnt10_str01("string01 DATA");
            newEntity.setEnt10_str02("string02 DATA");
            newEntity.setEnt10_str03("string03 DATA");
            Vector<String> stringVector = new Vector<String>();
            stringVector.add("element01");
            stringVector.add("element02");
            stringVector.add("element03");
            stringVector.add("element04");
            stringVector.add("element05");
            stringVector.add("element06");
            stringVector.add("element07");
            stringVector.add("element08");
            stringVector.add("element09");
            stringVector.add("element09");
            newEntity.setEnt10_vector01(stringVector);
            Vector<Integer> integerVector = new Vector<Integer>();
            integerVector.add(1);
            integerVector.add(2);
            integerVector.add(3);
            integerVector.add(4);
            integerVector.add(5);
            integerVector.add(6);
            integerVector.add(7);
            integerVector.add(8);
            integerVector.add(9);
            integerVector.add(10);
            newEntity.setEnt10_vector02(integerVector);
            Embeddable04a newEmbeddable04a1 = new Embeddable04a();
            Embeddable04a newEmbeddable04a2 = new Embeddable04a();
            Embeddable04a newEmbeddable04a3 = new Embeddable04a();
            Embeddable04a newEmbeddable04a4 = new Embeddable04a();
            Embeddable04a newEmbeddable04a5 = new Embeddable04a();
            Embeddable04a newEmbeddable04a6 = new Embeddable04a();
            newEmbeddable04a1.setEmb04a_int01(11);
            newEmbeddable04a1.setEmb04a_int02(12);
            newEmbeddable04a1.setEmb04a_int03(13);
            newEmbeddable04a1.getEmbeddable04b().setEmb04b_str04("1 string04 DATA");
            newEmbeddable04a1.getEmbeddable04b().setEmb04b_str05("1 string05 DATA");
            newEmbeddable04a1.getEmbeddable04b().setEmb04b_str06("1 string06 DATA");
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().setEmb04c_int07(14);
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().setEmb04c_int08(15);
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().setEmb04c_int09(16);
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str10("1 string07 DATA");
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str11("1 string08 DATA");
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str12("1 string09 DATA");
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int13(17);
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int14(18);
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int15(19);
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str16("1 string10 DATA");
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str17("1 string11 DATA");
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str18("1 string12 DATA");
            newEmbeddable04a2.setEmb04a_int01(21);
            newEmbeddable04a2.setEmb04a_int02(22);
            newEmbeddable04a2.setEmb04a_int03(23);
            newEmbeddable04a2.getEmbeddable04b().setEmb04b_str04("2 string04 DATA");
            newEmbeddable04a2.getEmbeddable04b().setEmb04b_str05("2 string05 DATA");
            newEmbeddable04a2.getEmbeddable04b().setEmb04b_str06("2 string06 DATA");
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().setEmb04c_int07(24);
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().setEmb04c_int08(25);
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().setEmb04c_int09(26);
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str10("2 string07 DATA");
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str11("2 string08 DATA");
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str12("2 string09 DATA");
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int13(27);
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int14(28);
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int15(29);
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str16("2 string10 DATA");
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str17("2 string11 DATA");
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str18("2 string12 DATA");
            newEmbeddable04a3.setEmb04a_int01(31);
            newEmbeddable04a3.setEmb04a_int02(32);
            newEmbeddable04a3.setEmb04a_int03(33);
            newEmbeddable04a3.getEmbeddable04b().setEmb04b_str04("3 string04 DATA");
            newEmbeddable04a3.getEmbeddable04b().setEmb04b_str05("3 string05 DATA");
            newEmbeddable04a3.getEmbeddable04b().setEmb04b_str06("3 string06 DATA");
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().setEmb04c_int07(34);
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().setEmb04c_int08(35);
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().setEmb04c_int09(36);
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str10("3 string07 DATA");
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str11("3 string08 DATA");
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str12("3 string09 DATA");
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int13(37);
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int14(38);
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int15(39);
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str16("3 string10 DATA");
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str17("3 string11 DATA");
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str18("3 string12 DATA");
            newEmbeddable04a4.setEmb04a_int01(41);
            newEmbeddable04a4.setEmb04a_int02(42);
            newEmbeddable04a4.setEmb04a_int03(43);
            newEmbeddable04a4.getEmbeddable04b().setEmb04b_str04("4 string04 DATA");
            newEmbeddable04a4.getEmbeddable04b().setEmb04b_str05("4 string05 DATA");
            newEmbeddable04a4.getEmbeddable04b().setEmb04b_str06("4 string06 DATA");
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().setEmb04c_int07(44);
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().setEmb04c_int08(45);
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().setEmb04c_int09(46);
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str10("4 string07 DATA");
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str11("4 string08 DATA");
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str12("4 string09 DATA");
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int13(47);
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int14(48);
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int15(49);
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str16("4 string10 DATA");
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str17("4 string11 DATA");
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str18("4 string12 DATA");
            newEmbeddable04a5.setEmb04a_int01(51);
            newEmbeddable04a5.setEmb04a_int02(52);
            newEmbeddable04a5.setEmb04a_int03(53);
            newEmbeddable04a5.getEmbeddable04b().setEmb04b_str04("5 string04 DATA");
            newEmbeddable04a5.getEmbeddable04b().setEmb04b_str05("5 string05 DATA");
            newEmbeddable04a5.getEmbeddable04b().setEmb04b_str06("5 string06 DATA");
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().setEmb04c_int07(54);
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().setEmb04c_int08(55);
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().setEmb04c_int09(56);
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str10("5 string07 DATA");
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str11("5 string08 DATA");
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str12("5 string09 DATA");
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int13(57);
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int14(58);
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int15(59);
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str16("5 string10 DATA");
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str17("5 string11 DATA");
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str18("5 string12 DATA");
            newEmbeddable04a6.setEmb04a_int01(61);
            newEmbeddable04a6.setEmb04a_int02(62);
            newEmbeddable04a6.setEmb04a_int03(63);
            newEmbeddable04a6.getEmbeddable04b().setEmb04b_str04("6 string04 DATA");
            newEmbeddable04a6.getEmbeddable04b().setEmb04b_str05("6 string05 DATA");
            newEmbeddable04a6.getEmbeddable04b().setEmb04b_str06("6 string06 DATA");
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().setEmb04c_int07(64);
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().setEmb04c_int08(65);
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().setEmb04c_int09(66);
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str10("6 string07 DATA");
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str11("6 string08 DATA");
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str12("6 string09 DATA");
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int13(67);
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int14(68);
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int15(69);
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str16("6 string10 DATA");
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str17("6 string11 DATA");
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str18("6 string12 DATA");
            Vector<Embeddable04a> embeddable04aVector = new Vector<Embeddable04a>();
            embeddable04aVector.add(newEmbeddable04a1);
            embeddable04aVector.add(newEmbeddable04a2);
            embeddable04aVector.add(newEmbeddable04a3);
            embeddable04aVector.add(newEmbeddable04a4);
            embeddable04aVector.add(newEmbeddable04a5);
            embeddable04aVector.add(newEmbeddable04a6);
            newEntity.setEnt10_vector03(embeddable04aVector);
            newEntity.setEmb04a_int01(71);
            newEntity.setEmb04a_int02(72);
            newEntity.setEmb04a_int03(73);
            newEntity.setEmb04b_str04("7 string04 DATA");
            newEntity.setEmb04b_str05("7 string05 DATA");
            newEntity.setEmb04b_str06("7 string06 DATA");
            newEntity.setEmb04c_int07(74);
            newEntity.setEmb04c_int08(75);
            newEntity.setEmb04c_int09(76);
            newEntity.setEmb04d_str10("7 string07 DATA");
            newEntity.setEmb04d_str11("7 string08 DATA");
            newEntity.setEmb04d_str12("7 string09 DATA");
            newEntity.setEmb04e_int13(77);
            newEntity.setEmb04e_int14(78);
            newEntity.setEmb04e_int15(79);
            newEntity.setEmb04f_str16("7 string10 DATA");
            newEntity.setEmb04f_str17("7 string11 DATA");
            newEntity.setEmb04f_str18("7 string12 DATA");

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing persist(" + newEntity + ") operation");
            em.persist(newEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity10 findEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(em.contains(findEntity));
            Assert.assertEquals(findEntity.getId(), id);

            Assert.assertEquals(findEntity.getEnt10_str01(), "string01 DATA");
            Assert.assertEquals(findEntity.getEnt10_str02(), "string02 DATA");
            Assert.assertEquals(findEntity.getEnt10_str03(), "string03 DATA");

            Vector<String> findVector01 = findEntity.getEnt10_vector01();
            Assert.assertEquals(findVector01.size(), stringVector.size());
            Assert.assertTrue(findVector01.containsAll(stringVector));

            Vector<Integer> findVector02 = findEntity.getEnt10_vector02();
            Assert.assertEquals(findVector02.size(), integerVector.size());
            Assert.assertTrue(findVector02.containsAll(integerVector));

            Vector<Embeddable04a> findVector03 = findEntity.getEnt10_vector03();
            Assert.assertEquals(findVector03.size(), embeddable04aVector.size());
            Assert.assertTrue(findVector03.containsAll(embeddable04aVector));
            //
            // Ensure order is based on @OrderBy("embeddable04b.embeddable04c.embeddable04d.embeddable04e.emb04e_int14 DESC, " +
            //                                   "embeddable04b.embeddable04c.embeddable04d.emb04d_str10 DESC, " +
            //                                   "embeddable04b.embeddable04c.emb04c_int09 DESC, " +
            //                                   "embeddable04b.emb04b_str05 DESC, " +
            //                                   "emb04a_int01 DESC")
            //
            int element = 0;
            Iterator<Embeddable04a> iterator = findVector03.iterator();
            while (iterator.hasNext()) {
                element++;
                Embeddable04a embeddable04a = iterator.next();
                switch (element) {
                    case 1:
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int14(), 68);
                        break;
                    case 2:
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int14(), 58);
                        break;
                    case 3:
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int14(), 48);
                        break;
                    case 4:
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int14(), 38);
                        break;
                    case 5:
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int14(), 28);
                        break;
                    case 6:
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int14(), 18);
                        break;
                    default:
                        Assert.fail();
                        break;
                }
            }

            Assert.assertEquals(findEntity.getEmb04a_int01(), 71);
            Assert.assertEquals(findEntity.getEmb04a_int02(), 72);
            Assert.assertEquals(findEntity.getEmb04a_int03(), 73);
            Assert.assertEquals(findEntity.getEmb04b_str04(), "7 string04 DATA");
            Assert.assertEquals(findEntity.getEmb04b_str05(), "7 string05 DATA");
            Assert.assertEquals(findEntity.getEmb04b_str06(), "7 string06 DATA");
            Assert.assertEquals(findEntity.getEmb04c_int07(), 74);
            Assert.assertEquals(findEntity.getEmb04c_int08(), 75);
            Assert.assertEquals(findEntity.getEmb04c_int09(), 76);
            Assert.assertEquals(findEntity.getEmb04d_str10(), "7 string07 DATA");
            Assert.assertEquals(findEntity.getEmb04d_str11(), "7 string08 DATA");
            Assert.assertEquals(findEntity.getEmb04d_str12(), "7 string09 DATA");
            Assert.assertEquals(findEntity.getEmb04e_int13(), 77);
            Assert.assertEquals(findEntity.getEmb04e_int14(), 78);
            Assert.assertEquals(findEntity.getEmb04e_int15(), 79);
            Assert.assertEquals(findEntity.getEmb04f_str16(), "7 string10 DATA");
            Assert.assertEquals(findEntity.getEmb04f_str17(), "7 string11 DATA");
            Assert.assertEquals(findEntity.getEmb04f_str18(), "7 string12 DATA");

            // Update the entity
            findEntity.setEnt10_str01("UPDATED string01 DATA");
            findEntity.setEnt10_str02("UPDATED string02 DATA");
            findEntity.setEnt10_str03("UPDATED string03 DATA");
            findVector01 = findEntity.getEnt10_vector01();
            findVector01.remove("element01");
            findVector01.remove("element03");
            findVector01.remove("element05");
            findVector01.remove("element07");
            findVector01.remove("element09");
            findVector01.add("element10");
            findVector01.add("element11");
            findVector01.add("element12");
            findEntity.setEnt10_vector01(findVector01);
            findVector02 = findEntity.getEnt10_vector02();
            findVector02.clear();
            findEntity.setEnt10_vector02(findVector02);
            Vector<Embeddable04a> tempVector03 = new Vector<Embeddable04a>();
            for (Embeddable04a embeddable04a : findVector03) {
                if (embeddable04a.getEmb04a_int01() == 11) {
                    embeddable04a.setEmb04a_int01(111);
                    embeddable04a.setEmb04a_int02(112);
                    embeddable04a.setEmb04a_int03(113);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().setEmb04c_int07(114);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().setEmb04c_int08(115);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().setEmb04c_int09(116);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int13(117);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int14(118);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int15(119);
                    tempVector03.add(embeddable04a);
                }
                if (embeddable04a.getEmb04a_int01() == 31) {
                    embeddable04a.setEmb04a_int01(331);
                    embeddable04a.setEmb04a_int02(332);
                    embeddable04a.setEmb04a_int03(333);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().setEmb04c_int07(334);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().setEmb04c_int08(335);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().setEmb04c_int09(336);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int13(337);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int14(338);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int15(339);
                    tempVector03.add(embeddable04a);
                }
                if (embeddable04a.getEmb04a_int01() == 51) {
                    embeddable04a.setEmb04a_int01(551);
                    embeddable04a.setEmb04a_int02(552);
                    embeddable04a.setEmb04a_int03(553);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().setEmb04c_int07(554);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().setEmb04c_int08(555);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().setEmb04c_int09(556);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int13(557);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int14(558);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int15(559);
                    tempVector03.add(embeddable04a);
                }
            }
            findEntity.setEnt10_vector03(tempVector03);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity update was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity10 updatedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", updatedEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedEntity);
            Assert.assertTrue(findEntity != updatedEntity);
            Assert.assertTrue(em.contains(updatedEntity));
            Assert.assertEquals(updatedEntity.getId(), id);
            Assert.assertEquals(updatedEntity.getEnt10_str01(), "UPDATED string01 DATA");
            Assert.assertEquals(updatedEntity.getEnt10_str02(), "UPDATED string02 DATA");
            Assert.assertEquals(updatedEntity.getEnt10_str03(), "UPDATED string03 DATA");

            Vector<String> updatedVector01 = updatedEntity.getEnt10_vector01();
            Assert.assertEquals(updatedVector01.size(), findVector01.size());
            Assert.assertTrue(updatedVector01.containsAll(findVector01));

            Vector<Integer> updatedVector02 = updatedEntity.getEnt10_vector02();
            Assert.assertEquals(updatedVector02.size(), findVector02.size());
            Assert.assertTrue(updatedVector02.containsAll(findVector02));

            Vector<Embeddable04a> updatedVector03 = updatedEntity.getEnt10_vector03();
            Assert.assertEquals(updatedVector03.size(), tempVector03.size());
            //
            // Ensure order is based on @OrderBy("embeddable04b.embeddable04c.embeddable04d.embeddable04e.emb04e_int14 DESC, " +
            //                                   "embeddable04b.embeddable04c.embeddable04d.emb04d_str10 DESC, " +
            //                                   "embeddable04b.embeddable04c.emb04c_int09 DESC, " +
            //                                   "embeddable04b.emb04b_str05 DESC, " +
            //                                   "emb04a_int01 DESC")
            // and ensure contents have changed properly
            //
            element = 0;
            iterator = updatedVector03.iterator();
            while (iterator.hasNext()) {
                element++;
                Embeddable04a embeddable04a = iterator.next();
                switch (element) {
                    case 1:
                        Assert.assertEquals(embeddable04a.getEmb04a_int01(), 551);
                        Assert.assertEquals(embeddable04a.getEmb04a_int02(), 552);
                        Assert.assertEquals(embeddable04a.getEmb04a_int03(), 553);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmb04b_str04(), "5 string04 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmb04b_str05(), "5 string05 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmb04b_str06(), "5 string06 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmb04c_int07(), 554);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmb04c_int08(), 555);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmb04c_int09(), 556);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmb04d_str10(), "5 string07 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmb04d_str11(), "5 string08 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmb04d_str12(), "5 string09 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int13(), 557);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int14(), 558);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int15(), 559);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().getEmb04f_str16(),
                                            "5 string10 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().getEmb04f_str17(),
                                            "5 string11 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().getEmb04f_str18(),
                                            "5 string12 DATA");
                        break;
                    case 2:
                        Assert.assertEquals(embeddable04a.getEmb04a_int01(), 331);
                        Assert.assertEquals(embeddable04a.getEmb04a_int02(), 332);
                        Assert.assertEquals(embeddable04a.getEmb04a_int03(), 333);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmb04b_str04(), "3 string04 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmb04b_str05(), "3 string05 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmb04b_str06(), "3 string06 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmb04c_int07(), 334);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmb04c_int08(), 335);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmb04c_int09(), 336);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmb04d_str10(), "3 string07 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmb04d_str11(), "3 string08 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmb04d_str12(), "3 string09 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int13(), 337);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int14(), 338);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int15(), 339);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().getEmb04f_str16(),
                                            "3 string10 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().getEmb04f_str17(),
                                            "3 string11 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().getEmb04f_str18(),
                                            "3 string12 DATA");
                        break;
                    case 3:
                        Assert.assertEquals(embeddable04a.getEmb04a_int01(), 111);
                        Assert.assertEquals(embeddable04a.getEmb04a_int02(), 112);
                        Assert.assertEquals(embeddable04a.getEmb04a_int03(), 113);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmb04b_str04(), "1 string04 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmb04b_str05(), "1 string05 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmb04b_str06(), "1 string06 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmb04c_int07(), 114);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmb04c_int08(), 115);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmb04c_int09(), 116);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmb04d_str10(), "1 string07 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmb04d_str11(), "1 string08 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmb04d_str12(), "1 string09 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int13(), 117);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int14(), 118);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int15(), 119);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().getEmb04f_str16(),
                                            "1 string10 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().getEmb04f_str17(),
                                            "1 string11 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().getEmb04f_str18(),
                                            "1 string12 DATA");
                        break;
                    default:
                        Assert.fail();
                        break;
                }
            }

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedEntity + ") operation");
            em.remove(updatedEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            Object findRemovedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNull("find(" + newEntity.getClass() + ", " + id + ") returned an entity.", findRemovedEntity);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testEmbeddableNested16(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 10;

            IEntity10 newEntity = new XMLEntity10();
            newEntity.setId(id);
            newEntity.setEnt10_str01("string01 DATA");
            newEntity.setEnt10_str02("string02 DATA");
            newEntity.setEnt10_str03("string03 DATA");
            Vector<String> stringVector = new Vector<String>();
            stringVector.add("element01");
            stringVector.add("element02");
            stringVector.add("element03");
            stringVector.add("element04");
            stringVector.add("element05");
            stringVector.add("element06");
            stringVector.add("element07");
            stringVector.add("element08");
            stringVector.add("element09");
            stringVector.add("element09");
            newEntity.setEnt10_vector01(stringVector);
            Vector<Integer> integerVector = new Vector<Integer>();
            integerVector.add(1);
            integerVector.add(2);
            integerVector.add(3);
            integerVector.add(4);
            integerVector.add(5);
            integerVector.add(6);
            integerVector.add(7);
            integerVector.add(8);
            integerVector.add(9);
            integerVector.add(10);
            newEntity.setEnt10_vector02(integerVector);
            Embeddable04a newEmbeddable04a1 = new Embeddable04a();
            Embeddable04a newEmbeddable04a2 = new Embeddable04a();
            Embeddable04a newEmbeddable04a3 = new Embeddable04a();
            Embeddable04a newEmbeddable04a4 = new Embeddable04a();
            Embeddable04a newEmbeddable04a5 = new Embeddable04a();
            Embeddable04a newEmbeddable04a6 = new Embeddable04a();
            newEmbeddable04a1.setEmb04a_int01(11);
            newEmbeddable04a1.setEmb04a_int02(12);
            newEmbeddable04a1.setEmb04a_int03(13);
            newEmbeddable04a1.getEmbeddable04b().setEmb04b_str04("1 string04 DATA");
            newEmbeddable04a1.getEmbeddable04b().setEmb04b_str05("1 string05 DATA");
            newEmbeddable04a1.getEmbeddable04b().setEmb04b_str06("1 string06 DATA");
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().setEmb04c_int07(14);
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().setEmb04c_int08(15);
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().setEmb04c_int09(16);
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str10("1 string07 DATA");
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str11("1 string08 DATA");
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str12("1 string09 DATA");
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int13(17);
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int14(18);
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int15(19);
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str16("1 string10 DATA");
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str17("1 string11 DATA");
            newEmbeddable04a1.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str18("1 string12 DATA");
            newEmbeddable04a2.setEmb04a_int01(21);
            newEmbeddable04a2.setEmb04a_int02(22);
            newEmbeddable04a2.setEmb04a_int03(23);
            newEmbeddable04a2.getEmbeddable04b().setEmb04b_str04("2 string04 DATA");
            newEmbeddable04a2.getEmbeddable04b().setEmb04b_str05("2 string05 DATA");
            newEmbeddable04a2.getEmbeddable04b().setEmb04b_str06("2 string06 DATA");
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().setEmb04c_int07(24);
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().setEmb04c_int08(25);
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().setEmb04c_int09(26);
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str10("2 string07 DATA");
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str11("2 string08 DATA");
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str12("2 string09 DATA");
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int13(27);
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int14(28);
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int15(29);
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str16("2 string10 DATA");
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str17("2 string11 DATA");
            newEmbeddable04a2.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str18("2 string12 DATA");
            newEmbeddable04a3.setEmb04a_int01(31);
            newEmbeddable04a3.setEmb04a_int02(32);
            newEmbeddable04a3.setEmb04a_int03(33);
            newEmbeddable04a3.getEmbeddable04b().setEmb04b_str04("3 string04 DATA");
            newEmbeddable04a3.getEmbeddable04b().setEmb04b_str05("3 string05 DATA");
            newEmbeddable04a3.getEmbeddable04b().setEmb04b_str06("3 string06 DATA");
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().setEmb04c_int07(34);
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().setEmb04c_int08(35);
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().setEmb04c_int09(36);
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str10("3 string07 DATA");
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str11("3 string08 DATA");
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str12("3 string09 DATA");
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int13(37);
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int14(38);
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int15(39);
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str16("3 string10 DATA");
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str17("3 string11 DATA");
            newEmbeddable04a3.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str18("3 string12 DATA");
            newEmbeddable04a4.setEmb04a_int01(41);
            newEmbeddable04a4.setEmb04a_int02(42);
            newEmbeddable04a4.setEmb04a_int03(43);
            newEmbeddable04a4.getEmbeddable04b().setEmb04b_str04("4 string04 DATA");
            newEmbeddable04a4.getEmbeddable04b().setEmb04b_str05("4 string05 DATA");
            newEmbeddable04a4.getEmbeddable04b().setEmb04b_str06("4 string06 DATA");
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().setEmb04c_int07(44);
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().setEmb04c_int08(45);
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().setEmb04c_int09(46);
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str10("4 string07 DATA");
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str11("4 string08 DATA");
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str12("4 string09 DATA");
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int13(47);
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int14(48);
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int15(49);
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str16("4 string10 DATA");
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str17("4 string11 DATA");
            newEmbeddable04a4.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str18("4 string12 DATA");
            newEmbeddable04a5.setEmb04a_int01(51);
            newEmbeddable04a5.setEmb04a_int02(52);
            newEmbeddable04a5.setEmb04a_int03(53);
            newEmbeddable04a5.getEmbeddable04b().setEmb04b_str04("5 string04 DATA");
            newEmbeddable04a5.getEmbeddable04b().setEmb04b_str05("5 string05 DATA");
            newEmbeddable04a5.getEmbeddable04b().setEmb04b_str06("5 string06 DATA");
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().setEmb04c_int07(54);
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().setEmb04c_int08(55);
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().setEmb04c_int09(56);
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str10("5 string07 DATA");
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str11("5 string08 DATA");
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str12("5 string09 DATA");
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int13(57);
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int14(58);
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int15(59);
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str16("5 string10 DATA");
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str17("5 string11 DATA");
            newEmbeddable04a5.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str18("5 string12 DATA");
            newEmbeddable04a6.setEmb04a_int01(61);
            newEmbeddable04a6.setEmb04a_int02(62);
            newEmbeddable04a6.setEmb04a_int03(63);
            newEmbeddable04a6.getEmbeddable04b().setEmb04b_str04("6 string04 DATA");
            newEmbeddable04a6.getEmbeddable04b().setEmb04b_str05("6 string05 DATA");
            newEmbeddable04a6.getEmbeddable04b().setEmb04b_str06("6 string06 DATA");
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().setEmb04c_int07(64);
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().setEmb04c_int08(65);
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().setEmb04c_int09(66);
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str10("6 string07 DATA");
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str11("6 string08 DATA");
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().setEmb04d_str12("6 string09 DATA");
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int13(67);
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int14(68);
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int15(69);
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str16("6 string10 DATA");
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str17("6 string11 DATA");
            newEmbeddable04a6.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().setEmb04f_str18("6 string12 DATA");
            Vector<Embeddable04a> embeddable04aVector = new Vector<Embeddable04a>();
            embeddable04aVector.add(newEmbeddable04a1);
            embeddable04aVector.add(newEmbeddable04a2);
            embeddable04aVector.add(newEmbeddable04a3);
            embeddable04aVector.add(newEmbeddable04a4);
            embeddable04aVector.add(newEmbeddable04a5);
            embeddable04aVector.add(newEmbeddable04a6);
            newEntity.setEnt10_vector03(embeddable04aVector);
            newEntity.setEmb04a_int01(71);
            newEntity.setEmb04a_int02(72);
            newEntity.setEmb04a_int03(73);
            newEntity.setEmb04b_str04("7 string04 DATA");
            newEntity.setEmb04b_str05("7 string05 DATA");
            newEntity.setEmb04b_str06("7 string06 DATA");
            newEntity.setEmb04c_int07(74);
            newEntity.setEmb04c_int08(75);
            newEntity.setEmb04c_int09(76);
            newEntity.setEmb04d_str10("7 string07 DATA");
            newEntity.setEmb04d_str11("7 string08 DATA");
            newEntity.setEmb04d_str12("7 string09 DATA");
            newEntity.setEmb04e_int13(77);
            newEntity.setEmb04e_int14(78);
            newEntity.setEmb04e_int15(79);
            newEntity.setEmb04f_str16("7 string10 DATA");
            newEntity.setEmb04f_str17("7 string11 DATA");
            newEntity.setEmb04f_str18("7 string12 DATA");

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing persist(" + newEntity + ") operation");
            em.persist(newEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity10 findEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(em.contains(findEntity));
            Assert.assertEquals(findEntity.getId(), id);

            Assert.assertEquals(findEntity.getEnt10_str01(), "string01 DATA");
            Assert.assertEquals(findEntity.getEnt10_str02(), "string02 DATA");
            Assert.assertEquals(findEntity.getEnt10_str03(), "string03 DATA");

            Vector<String> findVector01 = findEntity.getEnt10_vector01();
            Assert.assertEquals(findVector01.size(), stringVector.size());
            Assert.assertTrue(findVector01.containsAll(stringVector));

            Vector<Integer> findVector02 = findEntity.getEnt10_vector02();
            Assert.assertEquals(findVector02.size(), integerVector.size());
            Assert.assertTrue(findVector02.containsAll(integerVector));

            Vector<Embeddable04a> findVector03 = findEntity.getEnt10_vector03();
            Assert.assertEquals(findVector03.size(), embeddable04aVector.size());
            Assert.assertTrue(findVector03.containsAll(embeddable04aVector));
            //
            // Ensure order is based on @OrderBy("embeddable04b.embeddable04c.embeddable04d.embeddable04e.emb04e_int14 DESC, " +
            //                                   "embeddable04b.embeddable04c.embeddable04d.emb04d_str10 DESC, " +
            //                                   "embeddable04b.embeddable04c.emb04c_int09 DESC, " +
            //                                   "embeddable04b.emb04b_str05 DESC, " +
            //                                   "emb04a_int01 DESC")
            //
            int element = 0;
            Iterator<Embeddable04a> iterator = findVector03.iterator();
            while (iterator.hasNext()) {
                element++;
                Embeddable04a embeddable04a = iterator.next();
                switch (element) {
                    case 1:
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int14(), 68);
                        break;
                    case 2:
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int14(), 58);
                        break;
                    case 3:
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int14(), 48);
                        break;
                    case 4:
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int14(), 38);
                        break;
                    case 5:
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int14(), 28);
                        break;
                    case 6:
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int14(), 18);
                        break;
                    default:
                        Assert.fail();
                        break;
                }
            }

            Assert.assertEquals(findEntity.getEmb04a_int01(), 71);
            Assert.assertEquals(findEntity.getEmb04a_int02(), 72);
            Assert.assertEquals(findEntity.getEmb04a_int03(), 73);
            Assert.assertEquals(findEntity.getEmb04b_str04(), "7 string04 DATA");
            Assert.assertEquals(findEntity.getEmb04b_str05(), "7 string05 DATA");
            Assert.assertEquals(findEntity.getEmb04b_str06(), "7 string06 DATA");
            Assert.assertEquals(findEntity.getEmb04c_int07(), 74);
            Assert.assertEquals(findEntity.getEmb04c_int08(), 75);
            Assert.assertEquals(findEntity.getEmb04c_int09(), 76);
            Assert.assertEquals(findEntity.getEmb04d_str10(), "7 string07 DATA");
            Assert.assertEquals(findEntity.getEmb04d_str11(), "7 string08 DATA");
            Assert.assertEquals(findEntity.getEmb04d_str12(), "7 string09 DATA");
            Assert.assertEquals(findEntity.getEmb04e_int13(), 77);
            Assert.assertEquals(findEntity.getEmb04e_int14(), 78);
            Assert.assertEquals(findEntity.getEmb04e_int15(), 79);
            Assert.assertEquals(findEntity.getEmb04f_str16(), "7 string10 DATA");
            Assert.assertEquals(findEntity.getEmb04f_str17(), "7 string11 DATA");
            Assert.assertEquals(findEntity.getEmb04f_str18(), "7 string12 DATA");

            // Update the entity
            findEntity.setEnt10_str01("UPDATED string01 DATA");
            findEntity.setEnt10_str02("UPDATED string02 DATA");
            findEntity.setEnt10_str03("UPDATED string03 DATA");
            findVector01 = findEntity.getEnt10_vector01();
            findVector01.remove("element01");
            findVector01.remove("element03");
            findVector01.remove("element05");
            findVector01.remove("element07");
            findVector01.remove("element09");
            findVector01.add("element10");
            findVector01.add("element11");
            findVector01.add("element12");
            findEntity.setEnt10_vector01(findVector01);
            findVector02 = findEntity.getEnt10_vector02();
            findVector02.clear();
            findEntity.setEnt10_vector02(findVector02);
            Vector<Embeddable04a> tempVector03 = new Vector<Embeddable04a>();
            for (Embeddable04a embeddable04a : findVector03) {
                if (embeddable04a.getEmb04a_int01() == 11) {
                    embeddable04a.setEmb04a_int01(111);
                    embeddable04a.setEmb04a_int02(112);
                    embeddable04a.setEmb04a_int03(113);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().setEmb04c_int07(114);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().setEmb04c_int08(115);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().setEmb04c_int09(116);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int13(117);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int14(118);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int15(119);
                    tempVector03.add(embeddable04a);
                }
                if (embeddable04a.getEmb04a_int01() == 31) {
                    embeddable04a.setEmb04a_int01(331);
                    embeddable04a.setEmb04a_int02(332);
                    embeddable04a.setEmb04a_int03(333);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().setEmb04c_int07(334);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().setEmb04c_int08(335);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().setEmb04c_int09(336);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int13(337);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int14(338);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int15(339);
                    tempVector03.add(embeddable04a);
                }
                if (embeddable04a.getEmb04a_int01() == 51) {
                    embeddable04a.setEmb04a_int01(551);
                    embeddable04a.setEmb04a_int02(552);
                    embeddable04a.setEmb04a_int03(553);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().setEmb04c_int07(554);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().setEmb04c_int08(555);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().setEmb04c_int09(556);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int13(557);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int14(558);
                    embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().setEmb04e_int15(559);
                    tempVector03.add(embeddable04a);
                }
            }
            findEntity.setEnt10_vector03(tempVector03);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity update was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity10 updatedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", updatedEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedEntity);
            Assert.assertTrue(findEntity != updatedEntity);
            Assert.assertTrue(em.contains(updatedEntity));
            Assert.assertEquals(updatedEntity.getId(), id);
            Assert.assertEquals(updatedEntity.getEnt10_str01(), "UPDATED string01 DATA");
            Assert.assertEquals(updatedEntity.getEnt10_str02(), "UPDATED string02 DATA");
            Assert.assertEquals(updatedEntity.getEnt10_str03(), "UPDATED string03 DATA");

            Vector<String> updatedVector01 = updatedEntity.getEnt10_vector01();
            Assert.assertEquals(updatedVector01.size(), findVector01.size());
            Assert.assertTrue(updatedVector01.containsAll(findVector01));

            Vector<Integer> updatedVector02 = updatedEntity.getEnt10_vector02();
            Assert.assertEquals(updatedVector02.size(), findVector02.size());
            Assert.assertTrue(updatedVector02.containsAll(findVector02));

            Vector<Embeddable04a> updatedVector03 = updatedEntity.getEnt10_vector03();
            Assert.assertEquals(updatedVector03.size(), tempVector03.size());
            //
            // Ensure order is based on @OrderBy("embeddable04b.embeddable04c.embeddable04d.embeddable04e.emb04e_int14 DESC, " +
            //                                   "embeddable04b.embeddable04c.embeddable04d.emb04d_str10 DESC, " +
            //                                   "embeddable04b.embeddable04c.emb04c_int09 DESC, " +
            //                                   "embeddable04b.emb04b_str05 DESC, " +
            //                                   "emb04a_int01 DESC")
            // and ensure contents have changed properly
            //
            element = 0;
            iterator = updatedVector03.iterator();
            while (iterator.hasNext()) {
                element++;
                Embeddable04a embeddable04a = iterator.next();
                switch (element) {
                    case 1:
                        Assert.assertEquals(embeddable04a.getEmb04a_int01(), 551);
                        Assert.assertEquals(embeddable04a.getEmb04a_int02(), 552);
                        Assert.assertEquals(embeddable04a.getEmb04a_int03(), 553);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmb04b_str04(), "5 string04 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmb04b_str05(), "5 string05 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmb04b_str06(), "5 string06 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmb04c_int07(), 554);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmb04c_int08(), 555);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmb04c_int09(), 556);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmb04d_str10(), "5 string07 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmb04d_str11(), "5 string08 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmb04d_str12(), "5 string09 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int13(), 557);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int14(), 558);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int15(), 559);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().getEmb04f_str16(),
                                            "5 string10 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().getEmb04f_str17(),
                                            "5 string11 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().getEmb04f_str18(),
                                            "5 string12 DATA");
                        break;
                    case 2:
                        Assert.assertEquals(embeddable04a.getEmb04a_int01(), 331);
                        Assert.assertEquals(embeddable04a.getEmb04a_int02(), 332);
                        Assert.assertEquals(embeddable04a.getEmb04a_int03(), 333);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmb04b_str04(), "3 string04 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmb04b_str05(), "3 string05 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmb04b_str06(), "3 string06 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmb04c_int07(), 334);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmb04c_int08(), 335);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmb04c_int09(), 336);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmb04d_str10(), "3 string07 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmb04d_str11(), "3 string08 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmb04d_str12(), "3 string09 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int13(), 337);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int14(), 338);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int15(), 339);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().getEmb04f_str16(),
                                            "3 string10 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().getEmb04f_str17(),
                                            "3 string11 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().getEmb04f_str18(),
                                            "3 string12 DATA");
                        break;
                    case 3:
                        Assert.assertEquals(embeddable04a.getEmb04a_int01(), 111);
                        Assert.assertEquals(embeddable04a.getEmb04a_int02(), 112);
                        Assert.assertEquals(embeddable04a.getEmb04a_int03(), 113);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmb04b_str04(), "1 string04 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmb04b_str05(), "1 string05 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmb04b_str06(), "1 string06 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmb04c_int07(), 114);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmb04c_int08(), 115);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmb04c_int09(), 116);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmb04d_str10(), "1 string07 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmb04d_str11(), "1 string08 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmb04d_str12(), "1 string09 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int13(), 117);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int14(), 118);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmb04e_int15(), 119);
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().getEmb04f_str16(),
                                            "1 string10 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().getEmb04f_str17(),
                                            "1 string11 DATA");
                        Assert.assertEquals(embeddable04a.getEmbeddable04b().getEmbeddable04c().getEmbeddable04d().getEmbeddable04e().getEmbeddable04f().getEmb04f_str18(),
                                            "1 string12 DATA");
                        break;
                    default:
                        Assert.fail();
                        break;
                }
            }

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedEntity + ") operation");
            em.remove(updatedEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            Object findRemovedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNull("find(" + newEntity.getClass() + ", " + id + ") returned an entity.", findRemovedEntity);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Test Logic: testScenario11
     *
     * Description: Test of single entity with various collection of embeddablbes, used
     * to test dupicates and nulls for each of the collection types
     *
     * Performs these basic CRUD operations:
     *
     * 01. Create a new instance of the entity class
     * 02. Persist the new entity in the database
     * 03. Verify the entity was saved in the database
     * 04. Update the entity
     * 05. Verify the updated entity was saved in the database
     * 06. Delete the entity from the database
     * 07. Verify the deletion was successful
     *
     * UML:
     * +----------------------+
     * | List<Embeddable11> |
     * +----------+-----------+
     * 1 |
     * |
     * |
     * |
     * 1 <*>
     * +---------------------+ 1 1 +--------+--------+ 1 1 +-------------------------------+
     * | Set<Embeddable11> |------------<*>| Entity11 |<*>------------| Map<Timestamp,Embeddable11> |
     * +---------------------+ +--+-----------+--+ +-------------------------------+
     * 1 <*> <*> 1
     * | |
     * | |
     * | |
     * 1 | | 1
     * +-------------------------+--+ +---+--------------------+
     * | LinkedList<Embeddable11> | | Vector<Embeddable11> |
     * +----------------------------+ +------------------------+
     *
     */
    public void testEmbeddableNested17(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 11;

            IEntity11 newEntity = new Entity11();
            newEntity.setId(id);
            newEntity.setEnt11_str01(null);
            newEntity.setEnt11_str02(null);
            newEntity.setEnt11_str03(null);
            List<Embeddable11> embeddable11List = new ArrayList<Embeddable11>();
            LinkedList<Embeddable11> embeddable11LinkedList = new LinkedList<Embeddable11>();
            Set<Embeddable11> embeddable11Set = new HashSet<Embeddable11>();
            Map<Timestamp, Embeddable11> embeddable11Map = new HashMap<Timestamp, Embeddable11>();
            Vector<Embeddable11> embeddable11Vector = new Vector<Embeddable11>();
            embeddable11List.add(null); // List does allow nulls -- database column(s) must support nulls as well
//          embeddable11List.add(null);                  // List does allow duplicates
            embeddable11LinkedList.add(null); // LinkedList does allow nulls -- database column(s) must support nulls as well
//          embeddable11LinkedList.add(null);            // LinkedList does allow duplicates
            embeddable11Set.add(null); // Set does allow nulls -- database column(s) must support nulls as well
//          embeddable11Set.add(null);                   // Set does NOT allow duplicates
            embeddable11Map.put(null, null); // Map does allow nulls -- database column(s) must support nulls as well
//          embeddable11Map.put(null,null);              // Map does NOT allow duplicates
            embeddable11Vector.add(null); // Vector does allow nulls -- database column(s) must support nulls as well
//          embeddable11Vector.add(null);                // Vector does allow duplicates
            newEntity.setEnt11_list(embeddable11List);
            newEntity.setEnt11_llist(embeddable11LinkedList);
            newEntity.setEnt11_map(embeddable11Map);
            newEntity.setEnt11_set(embeddable11Set);
            newEntity.setEnt11_vector(embeddable11Vector);

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing persist(" + newEntity + ") operation");
            em.persist(newEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity11 findEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(em.contains(findEntity));
            Assert.assertEquals(findEntity.getId(), id);
            Assert.assertEquals(findEntity.getEnt11_str01(), null);
            Assert.assertEquals(findEntity.getEnt11_str02(), null);
            Assert.assertEquals(findEntity.getEnt11_str03(), null);

            List<Embeddable11> findList = findEntity.getEnt11_list();
            Assert.assertEquals(findList.size(), embeddable11List.size());
            for (Embeddable11 embeddable11 : findList) {
                Assert.assertEquals(embeddable11.getEmb11_str01(), null);
                Assert.assertEquals(embeddable11.getEmb11_str02(), null);
                Assert.assertEquals(embeddable11.getEmb11_str03(), null);
                Assert.assertEquals(embeddable11.getEmb11_int01(), null);
                Assert.assertEquals(embeddable11.getEmb11_int02(), null);
                Assert.assertEquals(embeddable11.getEmb11_int03(), null);
            }

            LinkedList<Embeddable11> findLinkedList = findEntity.getEnt11_llist();
            Assert.assertEquals(findLinkedList.size(), embeddable11LinkedList.size());
            for (Embeddable11 embeddable11 : findLinkedList) {
                Assert.assertEquals(embeddable11.getEmb11_str01(), null);
                Assert.assertEquals(embeddable11.getEmb11_str02(), null);
                Assert.assertEquals(embeddable11.getEmb11_str03(), null);
                Assert.assertEquals(embeddable11.getEmb11_int01(), null);
                Assert.assertEquals(embeddable11.getEmb11_int02(), null);
                Assert.assertEquals(embeddable11.getEmb11_int03(), null);
            }

            Map<Timestamp, Embeddable11> findMap = findEntity.getEnt11_map();
            Assert.assertEquals(findMap.size(), embeddable11Map.size());
            Assert.assertTrue(findMap.containsKey((null)));
            Embeddable11 embeddable11Find = findMap.get(null);
            Assert.assertEquals(embeddable11Find.getEmb11_str01(), null);
            Assert.assertEquals(embeddable11Find.getEmb11_str02(), null);
            Assert.assertEquals(embeddable11Find.getEmb11_str03(), null);
            Assert.assertEquals(embeddable11Find.getEmb11_int01(), null);
            Assert.assertEquals(embeddable11Find.getEmb11_int02(), null);
            Assert.assertEquals(embeddable11Find.getEmb11_int03(), null);

            Set<Embeddable11> findSet = findEntity.getEnt11_set();
            Assert.assertEquals(findSet.size(), embeddable11Set.size());
            for (Embeddable11 embeddable11 : findSet) {
                Assert.assertEquals(embeddable11.getEmb11_str01(), null);
                Assert.assertEquals(embeddable11.getEmb11_str02(), null);
                Assert.assertEquals(embeddable11.getEmb11_str03(), null);
                Assert.assertEquals(embeddable11.getEmb11_int01(), null);
                Assert.assertEquals(embeddable11.getEmb11_int02(), null);
                Assert.assertEquals(embeddable11.getEmb11_int03(), null);
            }

            Vector<Embeddable11> findVector = findEntity.getEnt11_vector();
            Assert.assertEquals(findVector.size(), embeddable11Vector.size());
            for (Embeddable11 embeddable11 : findVector) {
                Assert.assertEquals(embeddable11.getEmb11_str01(), null);
                Assert.assertEquals(embeddable11.getEmb11_str02(), null);
                Assert.assertEquals(embeddable11.getEmb11_str03(), null);
                Assert.assertEquals(embeddable11.getEmb11_int01(), null);
                Assert.assertEquals(embeddable11.getEmb11_int02(), null);
                Assert.assertEquals(embeddable11.getEmb11_int03(), null);
            }

            // Update the entity
            embeddable11List.add(null); // List does allow duplicates
            embeddable11List.add(null);
            embeddable11List.add(null);
            embeddable11LinkedList.add(null); // LinkedList does allow duplicates
            embeddable11LinkedList.add(null);
            embeddable11LinkedList.add(null);
            embeddable11Set.add(null); // Set does NOT allow duplicates
            embeddable11Set.add(null);
            embeddable11Set.add(null);
            embeddable11Map.put(null, null); // Map does NOT allow duplicates
            embeddable11Map.put(null, null);
            embeddable11Map.put(null, null);
            embeddable11Vector.add(null); // Vector does allow duplicates
            embeddable11Vector.add(null);
            embeddable11Vector.add(null);
            findEntity.setEnt11_list(embeddable11List);
            findEntity.setEnt11_llist(embeddable11LinkedList);
            findEntity.setEnt11_map(embeddable11Map);
            findEntity.setEnt11_set(embeddable11Set);
            findEntity.setEnt11_vector(embeddable11Vector);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity update was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity11 updatedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", updatedEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedEntity);
            Assert.assertTrue(findEntity != updatedEntity);
            Assert.assertTrue(em.contains(updatedEntity));
            Assert.assertEquals(updatedEntity.getId(), id);
            Assert.assertEquals(findEntity.getEnt11_str01(), null);
            Assert.assertEquals(findEntity.getEnt11_str02(), null);
            Assert.assertEquals(findEntity.getEnt11_str03(), null);

            List<Embeddable11> updatedList = updatedEntity.getEnt11_list();
            Assert.assertEquals(updatedList.size(), embeddable11List.size());
            for (Embeddable11 embeddable11 : updatedList) {
                Assert.assertEquals(embeddable11.getEmb11_str01(), null);
                Assert.assertEquals(embeddable11.getEmb11_str02(), null);
                Assert.assertEquals(embeddable11.getEmb11_str03(), null);
                Assert.assertEquals(embeddable11.getEmb11_int01(), null);
                Assert.assertEquals(embeddable11.getEmb11_int02(), null);
                Assert.assertEquals(embeddable11.getEmb11_int03(), null);
            }

            LinkedList<Embeddable11> updatedLinkedList = updatedEntity.getEnt11_llist();
            Assert.assertEquals(updatedLinkedList.size(), embeddable11LinkedList.size());
            for (Embeddable11 embeddable11 : updatedLinkedList) {
                Assert.assertEquals(embeddable11.getEmb11_str01(), null);
                Assert.assertEquals(embeddable11.getEmb11_str02(), null);
                Assert.assertEquals(embeddable11.getEmb11_str03(), null);
                Assert.assertEquals(embeddable11.getEmb11_int01(), null);
                Assert.assertEquals(embeddable11.getEmb11_int02(), null);
                Assert.assertEquals(embeddable11.getEmb11_int03(), null);
            }

            Map<Timestamp, Embeddable11> updatedMap = updatedEntity.getEnt11_map();
            Assert.assertEquals(updatedMap.size(), embeddable11Map.size());
            Assert.assertTrue(updatedMap.containsKey((null)));
            Embeddable11 embeddable11Updated = updatedMap.get(null);
            Assert.assertEquals(embeddable11Updated.getEmb11_str01(), null);
            Assert.assertEquals(embeddable11Updated.getEmb11_str02(), null);
            Assert.assertEquals(embeddable11Updated.getEmb11_str03(), null);
            Assert.assertEquals(embeddable11Updated.getEmb11_int01(), null);
            Assert.assertEquals(embeddable11Updated.getEmb11_int02(), null);
            Assert.assertEquals(embeddable11Updated.getEmb11_int03(), null);

            Set<Embeddable11> updatedSet = updatedEntity.getEnt11_set();
            Assert.assertEquals(updatedSet.size(), embeddable11Set.size());
            for (Embeddable11 embeddable11 : updatedSet) {
                Assert.assertEquals(embeddable11.getEmb11_str01(), null);
                Assert.assertEquals(embeddable11.getEmb11_str02(), null);
                Assert.assertEquals(embeddable11.getEmb11_str03(), null);
                Assert.assertEquals(embeddable11.getEmb11_int01(), null);
                Assert.assertEquals(embeddable11.getEmb11_int02(), null);
                Assert.assertEquals(embeddable11.getEmb11_int03(), null);
            }

            Vector<Embeddable11> updatedVector = updatedEntity.getEnt11_vector();
            Assert.assertEquals(updatedVector.size(), embeddable11Vector.size());
            for (Embeddable11 embeddable11 : updatedVector) {
                Assert.assertEquals(embeddable11.getEmb11_str01(), null);
                Assert.assertEquals(embeddable11.getEmb11_str02(), null);
                Assert.assertEquals(embeddable11.getEmb11_str03(), null);
                Assert.assertEquals(embeddable11.getEmb11_int01(), null);
                Assert.assertEquals(embeddable11.getEmb11_int02(), null);
                Assert.assertEquals(embeddable11.getEmb11_int03(), null);
            }

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedEntity + ") operation");
            em.remove(updatedEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            Object findRemovedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNull("find(" + newEntity.getClass() + ", " + id + ") returned an entity.", findRemovedEntity);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testEmbeddableNested18(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 11;

            IEntity11 newEntity = new XMLEntity11();
            newEntity.setId(id);
            newEntity.setEnt11_str01(null);
            newEntity.setEnt11_str02(null);
            newEntity.setEnt11_str03(null);
            List<Embeddable11> embeddable11List = new ArrayList<Embeddable11>();
            LinkedList<Embeddable11> embeddable11LinkedList = new LinkedList<Embeddable11>();
            Set<Embeddable11> embeddable11Set = new HashSet<Embeddable11>();
            Map<Timestamp, Embeddable11> embeddable11Map = new HashMap<Timestamp, Embeddable11>();
            Vector<Embeddable11> embeddable11Vector = new Vector<Embeddable11>();
            embeddable11List.add(null); // List does allow nulls -- database column(s) must support nulls as well
//          embeddable11List.add(null);                  // List does allow duplicates
            embeddable11LinkedList.add(null); // LinkedList does allow nulls -- database column(s) must support nulls as well
//          embeddable11LinkedList.add(null);            // LinkedList does allow duplicates
            embeddable11Set.add(null); // Set does allow nulls -- database column(s) must support nulls as well
//          embeddable11Set.add(null);                   // Set does NOT allow duplicates
            embeddable11Map.put(null, null); // Map does allow nulls -- database column(s) must support nulls as well
//          embeddable11Map.put(null,null);              // Map does NOT allow duplicates
            embeddable11Vector.add(null); // Vector does allow nulls -- database column(s) must support nulls as well
//          embeddable11Vector.add(null);                // Vector does allow duplicates
            newEntity.setEnt11_list(embeddable11List);
            newEntity.setEnt11_llist(embeddable11LinkedList);
            newEntity.setEnt11_map(embeddable11Map);
            newEntity.setEnt11_set(embeddable11Set);
            newEntity.setEnt11_vector(embeddable11Vector);

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            System.out.println("Performing persist(" + newEntity + ") operation");
            em.persist(newEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity11 findEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", findEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != findEntity);
            Assert.assertTrue(em.contains(findEntity));
            Assert.assertEquals(findEntity.getId(), id);
            Assert.assertEquals(findEntity.getEnt11_str01(), null);
            Assert.assertEquals(findEntity.getEnt11_str02(), null);
            Assert.assertEquals(findEntity.getEnt11_str03(), null);

            List<Embeddable11> findList = findEntity.getEnt11_list();
            Assert.assertEquals(findList.size(), embeddable11List.size());
            for (Embeddable11 embeddable11 : findList) {
                Assert.assertEquals(embeddable11.getEmb11_str01(), null);
                Assert.assertEquals(embeddable11.getEmb11_str02(), null);
                Assert.assertEquals(embeddable11.getEmb11_str03(), null);
                Assert.assertEquals(embeddable11.getEmb11_int01(), null);
                Assert.assertEquals(embeddable11.getEmb11_int02(), null);
                Assert.assertEquals(embeddable11.getEmb11_int03(), null);
            }

            LinkedList<Embeddable11> findLinkedList = findEntity.getEnt11_llist();
            Assert.assertEquals(findLinkedList.size(), embeddable11LinkedList.size());
            for (Embeddable11 embeddable11 : findLinkedList) {
                Assert.assertEquals(embeddable11.getEmb11_str01(), null);
                Assert.assertEquals(embeddable11.getEmb11_str02(), null);
                Assert.assertEquals(embeddable11.getEmb11_str03(), null);
                Assert.assertEquals(embeddable11.getEmb11_int01(), null);
                Assert.assertEquals(embeddable11.getEmb11_int02(), null);
                Assert.assertEquals(embeddable11.getEmb11_int03(), null);
            }

            Map<Timestamp, Embeddable11> findMap = findEntity.getEnt11_map();
            Assert.assertEquals(findMap.size(), embeddable11Map.size());
            Assert.assertTrue(findMap.containsKey((null)));
            Embeddable11 embeddable11Find = findMap.get(null);
            Assert.assertEquals(embeddable11Find.getEmb11_str01(), null);
            Assert.assertEquals(embeddable11Find.getEmb11_str02(), null);
            Assert.assertEquals(embeddable11Find.getEmb11_str03(), null);
            Assert.assertEquals(embeddable11Find.getEmb11_int01(), null);
            Assert.assertEquals(embeddable11Find.getEmb11_int02(), null);
            Assert.assertEquals(embeddable11Find.getEmb11_int03(), null);

            Set<Embeddable11> findSet = findEntity.getEnt11_set();
            Assert.assertEquals(findSet.size(), embeddable11Set.size());
            for (Embeddable11 embeddable11 : findSet) {
                Assert.assertEquals(embeddable11.getEmb11_str01(), null);
                Assert.assertEquals(embeddable11.getEmb11_str02(), null);
                Assert.assertEquals(embeddable11.getEmb11_str03(), null);
                Assert.assertEquals(embeddable11.getEmb11_int01(), null);
                Assert.assertEquals(embeddable11.getEmb11_int02(), null);
                Assert.assertEquals(embeddable11.getEmb11_int03(), null);
            }

            Vector<Embeddable11> findVector = findEntity.getEnt11_vector();
            Assert.assertEquals(findVector.size(), embeddable11Vector.size());
            for (Embeddable11 embeddable11 : findVector) {
                Assert.assertEquals(embeddable11.getEmb11_str01(), null);
                Assert.assertEquals(embeddable11.getEmb11_str02(), null);
                Assert.assertEquals(embeddable11.getEmb11_str03(), null);
                Assert.assertEquals(embeddable11.getEmb11_int01(), null);
                Assert.assertEquals(embeddable11.getEmb11_int02(), null);
                Assert.assertEquals(embeddable11.getEmb11_int03(), null);
            }

            // Update the entity
            embeddable11List.add(null); // List does allow duplicates
            embeddable11List.add(null);
            embeddable11List.add(null);
            embeddable11LinkedList.add(null); // LinkedList does allow duplicates
            embeddable11LinkedList.add(null);
            embeddable11LinkedList.add(null);
            embeddable11Set.add(null); // Set does NOT allow duplicates
            embeddable11Set.add(null);
            embeddable11Set.add(null);
            embeddable11Map.put(null, null); // Map does NOT allow duplicates
            embeddable11Map.put(null, null);
            embeddable11Map.put(null, null);
            embeddable11Vector.add(null); // Vector does allow duplicates
            embeddable11Vector.add(null);
            embeddable11Vector.add(null);
            findEntity.setEnt11_list(embeddable11List);
            findEntity.setEnt11_llist(embeddable11LinkedList);
            findEntity.setEnt11_map(embeddable11Map);
            findEntity.setEnt11_set(embeddable11Set);
            findEntity.setEnt11_vector(embeddable11Vector);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify the entity update was saved to the database
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            IEntity11 updatedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNotNull("find(" + newEntity.getClass() + ", " + id + ") did not return an entity.", updatedEntity);

            // Perform content verifications
            Assert.assertTrue(newEntity != updatedEntity);
            Assert.assertTrue(findEntity != updatedEntity);
            Assert.assertTrue(em.contains(updatedEntity));
            Assert.assertEquals(updatedEntity.getId(), id);
            Assert.assertEquals(findEntity.getEnt11_str01(), null);
            Assert.assertEquals(findEntity.getEnt11_str02(), null);
            Assert.assertEquals(findEntity.getEnt11_str03(), null);

            List<Embeddable11> updatedList = updatedEntity.getEnt11_list();
            Assert.assertEquals(updatedList.size(), embeddable11List.size());
            for (Embeddable11 embeddable11 : updatedList) {
                Assert.assertEquals(embeddable11.getEmb11_str01(), null);
                Assert.assertEquals(embeddable11.getEmb11_str02(), null);
                Assert.assertEquals(embeddable11.getEmb11_str03(), null);
                Assert.assertEquals(embeddable11.getEmb11_int01(), null);
                Assert.assertEquals(embeddable11.getEmb11_int02(), null);
                Assert.assertEquals(embeddable11.getEmb11_int03(), null);
            }

            LinkedList<Embeddable11> updatedLinkedList = updatedEntity.getEnt11_llist();
            Assert.assertEquals(updatedLinkedList.size(), embeddable11LinkedList.size());
            for (Embeddable11 embeddable11 : updatedLinkedList) {
                Assert.assertEquals(embeddable11.getEmb11_str01(), null);
                Assert.assertEquals(embeddable11.getEmb11_str02(), null);
                Assert.assertEquals(embeddable11.getEmb11_str03(), null);
                Assert.assertEquals(embeddable11.getEmb11_int01(), null);
                Assert.assertEquals(embeddable11.getEmb11_int02(), null);
                Assert.assertEquals(embeddable11.getEmb11_int03(), null);
            }

            Map<Timestamp, Embeddable11> updatedMap = updatedEntity.getEnt11_map();
            Assert.assertEquals(updatedMap.size(), embeddable11Map.size());
            Assert.assertTrue(updatedMap.containsKey((null)));
            Embeddable11 embeddable11Updated = updatedMap.get(null);
            Assert.assertEquals(embeddable11Updated.getEmb11_str01(), null);
            Assert.assertEquals(embeddable11Updated.getEmb11_str02(), null);
            Assert.assertEquals(embeddable11Updated.getEmb11_str03(), null);
            Assert.assertEquals(embeddable11Updated.getEmb11_int01(), null);
            Assert.assertEquals(embeddable11Updated.getEmb11_int02(), null);
            Assert.assertEquals(embeddable11Updated.getEmb11_int03(), null);

            Set<Embeddable11> updatedSet = updatedEntity.getEnt11_set();
            Assert.assertEquals(updatedSet.size(), embeddable11Set.size());
            for (Embeddable11 embeddable11 : updatedSet) {
                Assert.assertEquals(embeddable11.getEmb11_str01(), null);
                Assert.assertEquals(embeddable11.getEmb11_str02(), null);
                Assert.assertEquals(embeddable11.getEmb11_str03(), null);
                Assert.assertEquals(embeddable11.getEmb11_int01(), null);
                Assert.assertEquals(embeddable11.getEmb11_int02(), null);
                Assert.assertEquals(embeddable11.getEmb11_int03(), null);
            }

            Vector<Embeddable11> updatedVector = updatedEntity.getEnt11_vector();
            Assert.assertEquals(updatedVector.size(), embeddable11Vector.size());
            for (Embeddable11 embeddable11 : updatedVector) {
                Assert.assertEquals(embeddable11.getEmb11_str01(), null);
                Assert.assertEquals(embeddable11.getEmb11_str02(), null);
                Assert.assertEquals(embeddable11.getEmb11_str03(), null);
                Assert.assertEquals(embeddable11.getEmb11_int01(), null);
                Assert.assertEquals(embeddable11.getEmb11_int02(), null);
                Assert.assertEquals(embeddable11.getEmb11_int03(), null);
            }

            // Delete the entity from the database
            System.out.println("Performing remove(" + updatedEntity + ") operation");
            em.remove(updatedEntity);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            em.clear();

            // Verify the entity remove was successful
            System.out.println("Performing find(" + newEntity.getClass() + ", " + id + ") operation");
            Object findRemovedEntity = em.find(newEntity.getClass(), id);
            Assert.assertNull("find(" + newEntity.getClass() + ", " + id + ") returned an entity.", findRemovedEntity);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }
}
