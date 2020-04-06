/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.olgh8014.testlogic;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh8014.model.NoResultEntityOLGH8014;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH8014Logic extends AbstractTestLogic {

    /**
     * Complex test of the aggregate functions in JPQL.
     * For this test, there must be zero results in the entity table and the Entity state field
     * must be a primitive type.
     *
     * JPA 2.1/2.0 specification; Section 4.8.5:
     * If SUM, AVG, MAX, or MIN is used, and there are no values to which the aggregate
     * function can be applied, the result of the aggregate function is NULL.
     *
     * However, the JPA Container should be disabling this by setting the persistence property:
     * "eclipselink.allow-null-max-min" = "false".
     */
    public void testEmptyAggregateFunctionsWithPrimitives(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            em.clear();

            // Check to make sure the table is empty first
            TypedQuery<NoResultEntityOLGH8014> checkQuery = em.createQuery("SELECT n FROM NoResultEntityOLGH8014 n", NoResultEntityOLGH8014.class);
            List<NoResultEntityOLGH8014> checkResult = checkQuery.getResultList();
            Assert.assertEquals("Entity table NoResultEntityOLGH8014 must be empty for this test", 0, checkResult.size());

            Query q = em.createQuery("SELECT MIN(n.primitive) FROM NoResultEntityOLGH8014 n");
            Object res = q.getSingleResult();
            //This should return NULL, but the JPA Container disables the specification compliant fix by
            // setting "eclipselink.allow-null-max-min" = "false".
            Assert.assertEquals("Result of MIN aggregate should have been Integer(0): ", new Integer(0), res);

            q = em.createQuery("SELECT MAX(n.primitive) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            //This should return NULL, but the JPA Container disables the specification compliant fix by
            // setting "eclipselink.allow-null-max-min" = "false".
            Assert.assertEquals("Result of MAX aggregate should have been Integer(0): ", new Integer(0), res);

            q = em.createQuery("SELECT AVG(n.primitive) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            Assert.assertEquals("Result of AVG aggregate should have been NULL: ", null, res);

            q = em.createQuery("SELECT SUM(n.primitive) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            Assert.assertEquals("Result of SUM aggregate should have been NULL: ", null, res);

            q = em.createQuery("SELECT COUNT(n.primitive) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            Assert.assertEquals("Result of COUNT aggregate should have been a Long(0): ", new Long(0), res);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Complex test of the aggregate functions in JPQL.
     * For this test, there must be zero results in the entity table and the Entity state field
     * must be a primitive wrapper type.
     * JPA 2.1 specification; Section 4.8.5 states aggregate functions (MIN, MAX, AVG, & SUM)
     * must return a result of NULL if there are no values to apply the aggregate function to
     */
    public void testEmptyAggregateFunctionsWithWrappers(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            em.clear();

            // Check to make sure the table is empty first
            TypedQuery<NoResultEntityOLGH8014> checkQuery = em.createQuery("SELECT n FROM NoResultEntityOLGH8014 n", NoResultEntityOLGH8014.class);
            List<NoResultEntityOLGH8014> checkResult = checkQuery.getResultList();
            Assert.assertEquals("Entity table NoResultEntityOLGH8014 must be empty for this test", 0, checkResult.size());

            Query q = em.createQuery("SELECT MIN(n.wrapper) FROM NoResultEntityOLGH8014 n");
            Object res = q.getSingleResult();
            Assert.assertEquals("Result of MIN aggregate should have been NULL: ", null, res);

            q = em.createQuery("SELECT MAX(n.wrapper) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            Assert.assertEquals("Result of MAX aggregate should have been NULL: ", null, res);

            q = em.createQuery("SELECT AVG(n.wrapper) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            Assert.assertEquals("Result of AVG aggregate should have been NULL: ", null, res);

            q = em.createQuery("SELECT SUM(n.wrapper) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            Assert.assertEquals("Result of SUM aggregate should have been NULL: ", null, res);

            q = em.createQuery("SELECT COUNT(n.wrapper) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            Assert.assertEquals("Result of COUNT aggregate should have been a Long(0): ", new Long(0), res);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Complex test of the aggregate functions in JPQL.
     * For this test, there must be >0 results in the entity table and the Entity state field
     * must be a primitive wrapper type.
     * This test is verification that aggregates return the correct result
     */
    public void testAggregateFunctionsWithPrimitives(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            em.clear();

            Query q = em.createQuery("SELECT MIN(se.itemInteger2) FROM SimpleEntityOLGH8014 se");
            Object res = q.getSingleResult();
            Assert.assertEquals("Result of MIN aggregate should have been Integer(0): ", new Integer(0), res);

            q = em.createQuery("SELECT MAX(se.itemInteger2) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals("Result of MAX aggregate should have been Integer(0): ", new Integer(0), res);

            q = em.createQuery("SELECT AVG(se.itemInteger2) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals("Result of AVG aggregate should have been Double(0): ", new Double(0), res);

            q = em.createQuery("SELECT SUM(se.itemInteger2) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals("Result of SUM aggregate should have been Long(0): ", new Long(0), res);

            q = em.createQuery("SELECT COUNT(se.itemInteger2) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals("Result of COUNT aggregate should have been a Long(2): ", new Long(2), res);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Complex test of the aggregate functions in JPQL.
     * For this test, there must be >0 results in the entity table and the Entity state field
     * must be a primitive wrapper type.
     * This test is verification that aggregates return the correct result
     */
    public void testAggregateFunctionsWithWrappers(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            em.clear();

            Query q = em.createQuery("SELECT MIN(se.itemInteger1) FROM SimpleEntityOLGH8014 se");
            Object res = q.getSingleResult();
            Assert.assertEquals("Result of MIN aggregate should have been Integer(0): ", new Integer(0), res);

            q = em.createQuery("SELECT MAX(se.itemInteger1) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals("Result of MAX aggregate should have been Integer(0): ", new Integer(0), res);

            q = em.createQuery("SELECT AVG(se.itemInteger1) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals("Result of AVG aggregate should have been Double(0): ", new Double(0), res);

            q = em.createQuery("SELECT SUM(se.itemInteger1) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals("Result of SUM aggregate should have been Long(0): ", new Long(0), res);

            q = em.createQuery("SELECT COUNT(se.itemInteger1) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals("Result of COUNT aggregate should have been a Long(2): ", new Long(2), res);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }
}
