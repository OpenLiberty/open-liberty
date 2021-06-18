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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;
import javax.persistence.EntityManager;
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

            Query q = em.createQuery("SELECT MIN(n.itemInteger1) FROM NoResultEntityOLGH8014 n");
            Object res = q.getSingleResult();
            if (JPAProviderImpl.OPENJPA.equals(getJPAProviderImpl(jpaResource))) {
                /*
                 * TODO: OpenJPA bug: NULL is not returned and instead Integer(0) is returned
                 */
                Assert.assertEquals(new Integer(0), res);
            } else {
                //The specification defined assertion
                Assert.assertEquals(null, res);
            }
            q = em.createQuery("SELECT MIN(n.itemFloat1) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            if (JPAProviderImpl.OPENJPA.equals(getJPAProviderImpl(jpaResource))) {
                /*
                 * TODO: OpenJPA bug: NULL is not returned and instead Float(0) is returned
                 */
                Assert.assertEquals(new Float(0), res);
            } else {
                //The specification defined assertion
                Assert.assertEquals(null, res);
            }

            q = em.createQuery("SELECT MAX(n.itemInteger1) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            if (JPAProviderImpl.OPENJPA.equals(getJPAProviderImpl(jpaResource))) {
                /*
                 * TODO: OpenJPA bug: NULL is not returned and instead Integer(0) is returned
                 */
                Assert.assertEquals(new Integer(0), res);
            } else {
                //The specification defined assertion
                Assert.assertEquals(null, res);
            }
            q = em.createQuery("SELECT MAX(n.itemFloat1) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            if (JPAProviderImpl.OPENJPA.equals(getJPAProviderImpl(jpaResource))) {
                /*
                 * TODO: OpenJPA bug: NULL is not returned and instead Float(0) is returned
                 */
                Assert.assertEquals(new Float(0), res);
            } else {
                //The specification defined assertion
                Assert.assertEquals(null, res);
            }

            q = em.createQuery("SELECT AVG(n.itemInteger1) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            if (JPAProviderImpl.OPENJPA.equals(getJPAProviderImpl(jpaResource))) {
                /*
                 * TODO: OpenJPA bug: NULL is not returned and instead Integer(0) is returned
                 */
                Assert.assertEquals(new Integer(0), res);
            } else {
                //The specification defined assertion
                Assert.assertEquals(null, res);
            }
            q = em.createQuery("SELECT AVG(n.itemFloat1) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            if (JPAProviderImpl.OPENJPA.equals(getJPAProviderImpl(jpaResource))) {
                /*
                 * TODO: OpenJPA bug: NULL is not returned and instead Float(0) is returned
                 */
                Assert.assertEquals(new Float(0), res);
            } else {
                //The specification defined assertion
                Assert.assertEquals(null, res);
            }

            q = em.createQuery("SELECT SUM(n.itemInteger1) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            if (JPAProviderImpl.OPENJPA.equals(getJPAProviderImpl(jpaResource))) {
                /*
                 * TODO: OpenJPA bug: NULL is not returned and instead Long(0) is returned
                 */
                Assert.assertEquals(new Long(0), res);
            } else {
                //The specification defined assertion
                Assert.assertEquals(null, res);
            }
            q = em.createQuery("SELECT SUM(n.itemFloat1) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            if (JPAProviderImpl.OPENJPA.equals(getJPAProviderImpl(jpaResource))) {
                /*
                 * TODO: OpenJPA bug: NULL is not returned and instead Float(0) is returned
                 */
                Assert.assertEquals(new Float(0), res);
            } else {
                //The specification defined assertion
                Assert.assertEquals(null, res);
            }

            q = em.createQuery("SELECT COUNT(n.itemInteger1) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            Assert.assertEquals(new Long(0), res);
            q = em.createQuery("SELECT COUNT(n.itemFloat1) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            Assert.assertEquals(new Long(0), res);
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

            Query q = em.createQuery("SELECT MIN(n.itemInteger2) FROM NoResultEntityOLGH8014 n");
            Object res = q.getSingleResult();
            if (JPAProviderImpl.OPENJPA.equals(getJPAProviderImpl(jpaResource))) {
                /*
                 * TODO: OpenJPA bug: NULL is not returned and instead Integer(0) is returned
                 */
                Assert.assertEquals(new Integer(0), res);
            } else {
                //The specification defined assertion
                Assert.assertEquals(null, res);
            }
            q = em.createQuery("SELECT MIN(n.itemFloat2) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            if (JPAProviderImpl.OPENJPA.equals(getJPAProviderImpl(jpaResource))) {
                /*
                 * TODO: OpenJPA bug: NULL is not returned and instead Float(0) is returned
                 */
                Assert.assertEquals(new Float(0), res);
            } else {
                //The specification defined assertion
                Assert.assertEquals(null, res);
            }

            q = em.createQuery("SELECT MAX(n.itemInteger2) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            if (JPAProviderImpl.OPENJPA.equals(getJPAProviderImpl(jpaResource))) {
                /*
                 * TODO: OpenJPA bug: NULL is not returned and instead Integer(0) is returned
                 */
                Assert.assertEquals(new Integer(0), res);
            } else {
                //The specification defined assertion
                Assert.assertEquals(null, res);
            }
            q = em.createQuery("SELECT MAX(n.itemFloat2) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            if (JPAProviderImpl.OPENJPA.equals(getJPAProviderImpl(jpaResource))) {
                /*
                 * TODO: OpenJPA bug: NULL is not returned and instead Float(0) is returned
                 */
                Assert.assertEquals(new Float(0), res);
            } else {
                //The specification defined assertion
                Assert.assertEquals(null, res);
            }

            q = em.createQuery("SELECT AVG(n.itemInteger2) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            if (JPAProviderImpl.OPENJPA.equals(getJPAProviderImpl(jpaResource))) {
                /*
                 * TODO: OpenJPA bug: NULL is not returned and instead Integer(0) is returned
                 */
                Assert.assertEquals(new Integer(0), res);
            } else {
                //The specification defined assertion
                Assert.assertEquals(null, res);
            }

            q = em.createQuery("SELECT SUM(n.itemInteger2) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            if (JPAProviderImpl.OPENJPA.equals(getJPAProviderImpl(jpaResource))) {
                /*
                 * TODO: OpenJPA bug: NULL is not returned and instead Long(0) is returned
                 */
                Assert.assertEquals(new Long(0), res);
            } else {
                //The specification defined assertion
                Assert.assertEquals(null, res);
            }
            q = em.createQuery("SELECT SUM(n.itemFloat2) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            if (JPAProviderImpl.OPENJPA.equals(getJPAProviderImpl(jpaResource))) {
                /*
                 * TODO: OpenJPA bug: NULL is not returned and instead Float(0) is returned
                 */
                Assert.assertEquals(new Float(0), res);
            } else {
                //The specification defined assertion
                Assert.assertEquals(null, res);
            }
            q = em.createQuery("SELECT SUM(n.itemBigInteger1) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            Assert.assertEquals(null, res);

            //Test SUM for java.math.BigDecimal
            q = em.createQuery("SELECT SUM(n.itemBigDecimal1) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            Assert.assertEquals(null, res);

            q = em.createQuery("SELECT COUNT(n.itemInteger2) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            Assert.assertEquals(new Long(0), res);
            q = em.createQuery("SELECT COUNT(n.itemFloat2) FROM NoResultEntityOLGH8014 n");
            res = q.getSingleResult();
            Assert.assertEquals(new Long(0), res);
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

            Query q = em.createQuery("SELECT MIN(se.itemInteger1) FROM SimpleEntityOLGH8014 se");
            Object res = q.getSingleResult();
            Assert.assertEquals(new Integer(15), res);
            q = em.createQuery("SELECT MIN(se.itemFloat1) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals(new Float(13.13), res);

            q = em.createQuery("SELECT MAX(se.itemInteger1) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals(new Integer(25), res);
            q = em.createQuery("SELECT MAX(se.itemFloat1) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals(new Float(23.23), res);

            q = em.createQuery("SELECT AVG(se.itemInteger1) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            if (JPAProviderImpl.OPENJPA.equals(getJPAProviderImpl(jpaResource))) {
                /*
                 * TODO: OpenJPA bug: incorrectly returns result of type Integer when the JPA 2.0 specification
                 * clearly states the return type should be Double
                 */
                Assert.assertEquals(new Integer(20), res);
            } else {
                //The specification defined assertion
                Assert.assertEquals(new Double(20).doubleValue(), (double) res, 0.5);
            }
            q = em.createQuery("SELECT AVG(se.itemFloat1) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            if (JPAProviderImpl.OPENJPA.equals(getJPAProviderImpl(jpaResource))) {
                /*
                 * TODO: OpenJPA bug: incorrectly returns result of type Integer when the JPA 2.0 specification
                 * clearly states the return type should be Double
                 */
                Assert.assertEquals(new Float(18.18), res);
            } else {
                //The specification defined assertion
                Assert.assertEquals(new Double(18.18).doubleValue(), (double) res, 0.01);
            }

            q = em.createQuery("SELECT SUM(se.itemInteger1) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals(new Long(40), res);
            q = em.createQuery("SELECT SUM(se.itemFloat1) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            if (JPAProviderImpl.OPENJPA.equals(getJPAProviderImpl(jpaResource))) {
                /*
                 * TODO: OpenJPA bug: incorrectly returns result of type Float when the JPA 2.0 specification
                 * clearly states the return type should be Double
                 */
                Assert.assertEquals(new Float(36.36), res);
            } else {
                //The specification defined assertion
                Assert.assertEquals(new Double(36.36).doubleValue(), (double) res, 0.5);
            }

            q = em.createQuery("SELECT COUNT(se.itemInteger1) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals(new Long(2), res);
            q = em.createQuery("SELECT COUNT(se.itemFloat1) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals(new Long(2), res);
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

            // Test MIN function
            Query q = em.createQuery("SELECT MIN(se.itemInteger2) FROM SimpleEntityOLGH8014 se");
            Object res = q.getSingleResult();
            Assert.assertEquals(new Integer(16), res);
            q = em.createQuery("SELECT MIN(se.itemFloat2) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals(new Float(14.15), res);
            q = em.createQuery("SELECT MIN(se.itemBigInteger1) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals(BigInteger.valueOf(12), res);
            q = em.createQuery("SELECT MIN(se.itemBigDecimal1) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals(BigDecimal.valueOf(11.11).setScale(6).doubleValue(), ((BigDecimal) res).doubleValue(), 0.2);

            // Test MAX function
            q = em.createQuery("SELECT MAX(se.itemInteger2) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals(new Integer(26), res);
            q = em.createQuery("SELECT MAX(se.itemFloat2) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals(new Float(24.25), res);
            q = em.createQuery("SELECT MAX(se.itemBigInteger1) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals(BigInteger.valueOf(22), res);
            q = em.createQuery("SELECT MAX(se.itemBigDecimal1) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals(BigDecimal.valueOf(21.210000).setScale(6).doubleValue(), ((BigDecimal) res).doubleValue(), 0.5);

            // Test AVG function
            q = em.createQuery("SELECT AVG(se.itemInteger2) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            if (JPAProviderImpl.OPENJPA.equals(getJPAProviderImpl(jpaResource))) {
                /*
                 * TODO: OpenJPA bug: incorrectly returns result of type Integer when the JPA 2.0 specification
                 * clearly states the return type should be Double
                 */
                Assert.assertEquals(new Integer(21), res);
            } else {
                //The specification defined assertion
                Assert.assertEquals(new Double(21).doubleValue(), (double) res, 0.5);
            }
            q = em.createQuery("SELECT AVG(se.itemFloat2) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            if (JPAProviderImpl.OPENJPA.equals(getJPAProviderImpl(jpaResource))) {
                /*
                 * TODO: OpenJPA bug: incorrectly returns result of type Integer when the JPA 2.0 specification
                 * clearly states the return type should be Double
                 */
                Assert.assertEquals(new Float(19.2), res);
            } else {
                //The specification defined assertion
                Assert.assertEquals(new Double(19.2).doubleValue(), (double) res, 0.5);
            }

            // Test SUM function
            q = em.createQuery("SELECT SUM(se.itemInteger2) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals(new Long(42), res);
            q = em.createQuery("SELECT SUM(se.itemFloat2) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            if (JPAProviderImpl.OPENJPA.equals(getJPAProviderImpl(jpaResource))) {
                /*
                 * TODO: OpenJPA bug: incorrectly returns result of type Float when the JPA 2.0 specification
                 * clearly states the return type should be Double
                 */
                Assert.assertEquals(new Float(38.4).floatValue(), (float) res, 0.5);
            } else {
                //The specification defined assertion
                Assert.assertEquals(new Double(38.4).doubleValue(), (double) res, 0.5);
            }
            q = em.createQuery("SELECT SUM(se.itemBigInteger1) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals(BigInteger.valueOf(34), res);
            q = em.createQuery("SELECT SUM(se.itemBigDecimal1) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals(BigDecimal.valueOf(32.32).setScale(6).doubleValue(), ((BigDecimal) res).doubleValue(), 0.5);

            // Test COUNT function
            q = em.createQuery("SELECT COUNT(se.itemInteger2) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals(new Long(2), res);
            q = em.createQuery("SELECT COUNT(se.itemFloat2) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals(new Long(2), res);
            q = em.createQuery("SELECT COUNT(se.itemBigInteger1) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals(new Long(2), res);
            q = em.createQuery("SELECT COUNT(se.itemBigDecimal1) FROM SimpleEntityOLGH8014 se");
            res = q.getSingleResult();
            Assert.assertEquals(new Long(2), res);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            t.printStackTrace();
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }
}
