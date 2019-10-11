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

package com.ibm.ws.jpa.query.aggregate_functions.testlogic;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.junit.Assert;

import com.ibm.ws.jpa.query.aggregate_functions.model.NoResultEntity;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class APARAggregateFunctionsTestLogic extends AbstractTestLogic {

    /**
     * Complex test of the aggregate functions in JPQL.
     * For this test, there must be zero results in the entity table and the Entity state field
     * must be a primitive type.
     * JPA 2.1 specification; Section 4.8.5 states aggregate functions (MIN, MAX, AVG, & SUM)
     * must return a result of NULL if there are no values to apply the aggregate function to
     */
    public void testEmptyAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_ON(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // Check to make sure the table is empty first
            TypedQuery<NoResultEntity> checkQuery = em.createQuery("SELECT n FROM NoResultEntity n", NoResultEntity.class);
            List<NoResultEntity> checkResult = checkQuery.getResultList();
            Assert.assertEquals("Entity table NoResultEntity must be empty for this test", 0, checkResult.size());

            Query q = em.createQuery("SELECT MIN(n.primitive) FROM NoResultEntity n");
            Object res = q.getSingleResult();
            Assert.assertEquals("Result of MIN aggregate should have been NULL", null, res);

            q = em.createQuery("SELECT MAX(n.primitive) FROM NoResultEntity n");
            res = q.getSingleResult();
            Assert.assertEquals("Result of MAX aggregate should have been NULL", null, res);

            q = em.createQuery("SELECT AVG(n.primitive) FROM NoResultEntity n");
            res = q.getSingleResult();
            Assert.assertEquals("Result of AVG aggregate should have been NULL", null, res);

            q = em.createQuery("SELECT SUM(n.primitive) FROM NoResultEntity n");
            res = q.getSingleResult();
            Assert.assertEquals("Result of SUM aggregate should have been NULL", null, res);

            q = em.createQuery("SELECT COUNT(n.primitive) FROM NoResultEntity n");
            res = q.getSingleResult();
            Assert.assertEquals("Result of COUNT aggregate should have been a Long", new Long(0), res);
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
     * must be a primitive type.
     * JPA 2.1 specification; Section 4.8.5 states aggregate functions (MIN, MAX, AVG, & SUM)
     * must return a result of NULL if there are no values to apply the aggregate function to
     */
    public void testEmptyAggregateFunctionsWithPrimitives_ALLOW_NULL_MAX_MIN_OFF(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // Check to make sure the table is empty first
            TypedQuery<NoResultEntity> checkQuery = em.createQuery("SELECT n FROM NoResultEntity n", NoResultEntity.class);
            List<NoResultEntity> checkResult = checkQuery.getResultList();
            Assert.assertEquals("Entity table NoResultEntity must be empty for this test", 0, checkResult.size());

            Query q = em.createQuery("SELECT MIN(n.primitive) FROM NoResultEntity n");
            Object res = q.getSingleResult();
            Assert.assertEquals("Result of MIN aggregate should have been 0", 0, res);

            q = em.createQuery("SELECT MAX(n.primitive) FROM NoResultEntity n");
            res = q.getSingleResult();
            Assert.assertEquals("Result of MAX aggregate should have been 0", 0, res);

            q = em.createQuery("SELECT AVG(n.primitive) FROM NoResultEntity n");
            res = q.getSingleResult();
            Assert.assertEquals("Result of AVG aggregate should have been NULL", null, res);

            q = em.createQuery("SELECT SUM(n.primitive) FROM NoResultEntity n");
            res = q.getSingleResult();
            Assert.assertEquals("Result of SUM aggregate should have been NULL", null, res);

            q = em.createQuery("SELECT COUNT(n.primitive) FROM NoResultEntity n");
            res = q.getSingleResult();
            Assert.assertEquals("Result of COUNT aggregate should have been a Long", new Long(0), res);
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
    public void testEmptyAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_ON(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // Check to make sure the table is empty first
            TypedQuery<NoResultEntity> checkQuery = em.createQuery("SELECT n FROM NoResultEntity n", NoResultEntity.class);
            List<NoResultEntity> checkResult = checkQuery.getResultList();
            Assert.assertEquals("Entity table NoResultEntity must be empty for this test", 0, checkResult.size());

            Query q = em.createQuery("SELECT MIN(n.wrapper) FROM NoResultEntity n");
            Object res = q.getSingleResult();
            Assert.assertEquals("Result of MIN aggregate should have been NULL", null, res);

            q = em.createQuery("SELECT MAX(n.wrapper) FROM NoResultEntity n");
            res = q.getSingleResult();
            Assert.assertEquals("Result of MAX aggregate should have been NULL", null, res);

            q = em.createQuery("SELECT AVG(n.wrapper) FROM NoResultEntity n");
            res = q.getSingleResult();
            Assert.assertEquals("Result of AVG aggregate should have been NULL", null, res);

            q = em.createQuery("SELECT SUM(n.wrapper) FROM NoResultEntity n");
            res = q.getSingleResult();
            Assert.assertEquals("Result of SUM aggregate should have been NULL", null, res);

            q = em.createQuery("SELECT COUNT(n.wrapper) FROM NoResultEntity n");
            res = q.getSingleResult();
            Assert.assertEquals("Result of COUNT aggregate should have been a Long", new Long(0), res);
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
    public void testEmptyAggregateFunctionsWithWrappers_ALLOW_NULL_MAX_MIN_OFF(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // Check to make sure the table is empty first
            TypedQuery<NoResultEntity> checkQuery = em.createQuery("SELECT n FROM NoResultEntity n", NoResultEntity.class);
            List<NoResultEntity> checkResult = checkQuery.getResultList();
            Assert.assertEquals("Entity table NoResultEntity must be empty for this test", 0, checkResult.size());

            Query q = em.createQuery("SELECT MIN(n.primitive) FROM NoResultEntity n");
            Object res = q.getSingleResult();
            Assert.assertEquals("Result of MIN aggregate should have been 0", 0, res);

            q = em.createQuery("SELECT MAX(n.primitive) FROM NoResultEntity n");
            res = q.getSingleResult();
            Assert.assertEquals("Result of MAX aggregate should have been 0", 0, res);

            q = em.createQuery("SELECT AVG(n.primitive) FROM NoResultEntity n");
            res = q.getSingleResult();
            Assert.assertEquals("Result of AVG aggregate should have been NULL", null, res);

            q = em.createQuery("SELECT SUM(n.primitive) FROM NoResultEntity n");
            res = q.getSingleResult();
            Assert.assertEquals("Result of SUM aggregate should have been NULL", null, res);

            q = em.createQuery("SELECT COUNT(n.primitive) FROM NoResultEntity n");
            res = q.getSingleResult();
            Assert.assertEquals("Result of COUNT aggregate should have been a Long", new Long(0), res);
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            Query q = em.createQuery("SELECT MIN(se.itemInteger2) FROM SimpleEntity se");
            Object res = q.getSingleResult();
            Assert.assertEquals("Result of MIN aggregate should have been NULL", new Integer(0), res);

            Query q2 = em.createQuery("SELECT MAX(se.itemInteger2) FROM SimpleEntity se");
            Object res2 = q2.getSingleResult();
            Assert.assertEquals("Result of MAX aggregate should have been NULL", new Integer(0), res2);

            Query q3 = em.createQuery("SELECT AVG(se.itemInteger2) FROM SimpleEntity se");
            Object res3 = q3.getSingleResult();
            Assert.assertEquals("Result of AVG aggregate should have been NULL", new Double(0), res3);

            Query q4 = em.createQuery("SELECT SUM(se.itemInteger2) FROM SimpleEntity se");
            Object res4 = q4.getSingleResult();
            Assert.assertEquals("Result of SUM aggregate should have been NULL", new Long(0), res4);

            Query q5 = em.createQuery("SELECT COUNT(se.itemInteger2) FROM SimpleEntity se");
            Object res5 = q5.getSingleResult();
            Assert.assertEquals("Result of COUNT aggregate should have been a Long", new Long(2), res5);
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            Query q = em.createQuery("SELECT MIN(se.itemInteger1) FROM SimpleEntity se");
            Object res = q.getSingleResult();
            Assert.assertEquals("Result of MIN aggregate should have been NULL", new Integer(0), res);

            Query q2 = em.createQuery("SELECT MAX(se.itemInteger1) FROM SimpleEntity se");
            Object res2 = q2.getSingleResult();
            Assert.assertEquals("Result of MAX aggregate should have been NULL", new Integer(0), res2);

            Query q3 = em.createQuery("SELECT AVG(se.itemInteger1) FROM SimpleEntity se");
            Object res3 = q3.getSingleResult();
            Assert.assertEquals("Result of AVG aggregate should have been NULL", new Double(0), res3);

            Query q4 = em.createQuery("SELECT SUM(se.itemInteger1) FROM SimpleEntity se");
            Object res4 = q4.getSingleResult();
            Assert.assertEquals("Result of SUM aggregate should have been NULL", new Long(0), res4);

            Query q5 = em.createQuery("SELECT COUNT(se.itemInteger1) FROM SimpleEntity se");
            Object res5 = q5.getSingleResult();
            Assert.assertEquals("Result of COUNT aggregate should have been a Long", new Long(2), res5);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void template(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        // Execute Test Case
        try {

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
