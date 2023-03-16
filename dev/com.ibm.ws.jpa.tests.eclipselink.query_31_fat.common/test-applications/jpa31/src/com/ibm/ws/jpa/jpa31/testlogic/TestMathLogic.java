/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package com.ibm.ws.jpa.jpa31.testlogic;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

import com.ibm.ws.jpa.jpa31.model.JPA31Entity;
import com.ibm.ws.jpa.jpa31.model.JPA31Entity_;
import com.ibm.ws.jpa.query.sqlcapture.SQLCallListener;
import com.ibm.ws.testtooling.database.DatabaseVendor;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;

public class TestMathLogic extends AbstractTestLogic {

    public void testCEILING1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                     Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT CEILING(?1) FROM JPA31Entity s WHERE s.intVal1 = ?2");
            query.setParameter(1, 1.1);
            query.setParameter(2, 3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CEIL(1.1) FROM JPA31ENTITY WHERE (INTVAL1 = 3)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CEIL(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT CEILING(1.1) FROM JPA31Entity s WHERE s.intVal1 = 3");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CEIL(1.1) FROM JPA31ENTITY WHERE (INTVAL1 = 3)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CEIL(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.select(cb.ceiling(floatParam1));
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), intParam1));

            query = em.createQuery(cquery);
            query.setParameter(floatParam1, 1.1f);
            query.setParameter(intParam1, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CEIL(1.1) FROM JPA31ENTITY WHERE (INTVAL1 = 3)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CEIL(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.ceiling(cb2.literal(1.1f)));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.literal(3)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CEIL(1.1) FROM JPA31ENTITY WHERE (INTVAL1 = 3)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CEIL(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCEILING1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                         Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT CEILING(?1) FROM JPA31Entity s WHERE s.intVal1 = ?2");
            query.setParameter(1, 1.1);
            query.setParameter(2, 3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CEIL(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CEIL(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT CEILING(1.1) FROM JPA31Entity s WHERE s.intVal1 = 3");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CEIL(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CEIL(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.select(cb.ceiling(floatParam1));
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), intParam1));

            query = em.createQuery(cquery);
            query.setParameter(floatParam1, 1.1f);
            query.setParameter(intParam1, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CEIL(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CEIL(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.ceiling(cb2.literal(1.1f)));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.literal(3)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CEIL(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CEIL(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCEILING1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                     Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT CEILING(?1) FROM JPA31Entity s WHERE s.intVal1 = ?2");
            query.setParameter(1, 1.1);
            query.setParameter(2, 3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CEIL(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CEIL(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT CEILING(1.1) FROM JPA31Entity s WHERE s.intVal1 = 3");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CEIL(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CEIL(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.select(cb.ceiling(floatParam1));
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), intParam1));

            query = em.createQuery(cquery);
            query.setParameter(floatParam1, 1.1f);
            query.setParameter(intParam1, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CEIL(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CEIL(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.ceiling(cb2.literal(1.1f)));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.literal(3)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CEIL(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CEIL(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCEILING2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                     Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s WHERE s.intVal1 = CEILING(?2)");
            query.setParameter(1, 1);
            query.setParameter(2, 3.3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(3.3))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = CEILING(3.3)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(3.3))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            cquery.select(intParam1);
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), cb.ceiling(floatParam1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(floatParam1, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(3.3))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.literal(1));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.ceiling(cb2.literal(3.3f))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(3.3))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCEILING2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                         Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s WHERE s.intVal1 = CEILING(?2)");
            query.setParameter(1, 1);
            query.setParameter(2, 3.3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = CEILING(3.3)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            cquery.select(intParam1);
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), cb.ceiling(floatParam1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(floatParam1, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.literal(1));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.ceiling(cb2.literal(3.3f))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCEILING2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                     Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s WHERE s.intVal1 = CEILING(?2)");
            query.setParameter(1, 1);
            query.setParameter(2, 3.3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = CEILING(3.3)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            cquery.select(intParam1);
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), cb.ceiling(floatParam1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(floatParam1, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.literal(1));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.ceiling(cb2.literal(3.3f))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = CEIL(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCEILING3_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                     Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s HAVING ?2 < CEILING(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 9.9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < CEIL(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING 0 < CEILING(9.9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < CEIL(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING ?1 < CEILING(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 9.9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < CEIL(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Double> doubleParam1 = cb.parameter(Double.class);
            ParameterExpression<Double> doubleParam2 = cb.parameter(Double.class);
            cquery.multiselect(intParam1);
            cquery.having(cb.lessThan(doubleParam1, cb.ceiling(doubleParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(doubleParam1, 0d);
            query.setParameter(doubleParam2, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < CEIL(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.ceiling(cb2.literal(9.9d))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < CEIL(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            cquery3.multiselect(cb3.literal(1));
            ParameterExpression<Double> doubleParam3 = cb3.parameter(Double.class);
            ParameterExpression<Double> doubleParam4 = cb3.parameter(Double.class);
            cquery3.having(cb3.lessThan(doubleParam3, cb3.ceiling(doubleParam4)));

            query = em.createQuery(cquery3);
            query.setParameter(doubleParam3, 0d);
            query.setParameter(doubleParam4, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < CEIL(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCEILING3_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                         Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s HAVING ?2 < CEILING(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 9.9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING 0 < CEILING(9.9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING ?1 < CEILING(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 9.9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Double> doubleParam1 = cb.parameter(Double.class);
            ParameterExpression<Double> doubleParam2 = cb.parameter(Double.class);
            cquery.multiselect(intParam1);
            cquery.having(cb.lessThan(doubleParam1, cb.ceiling(doubleParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(doubleParam1, 0d);
            query.setParameter(doubleParam2, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.ceiling(cb2.literal(9.9d))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            cquery3.multiselect(cb3.literal(1));
            ParameterExpression<Double> doubleParam3 = cb3.parameter(Double.class);
            ParameterExpression<Double> doubleParam4 = cb3.parameter(Double.class);
            cquery3.having(cb3.lessThan(doubleParam3, cb3.ceiling(doubleParam4)));

            query = em.createQuery(cquery3);
            query.setParameter(doubleParam3, 0d);
            query.setParameter(doubleParam4, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCEILING3_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                     Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s HAVING ?2 < CEILING(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 9.9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING 0 < CEILING(9.9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING ?1 < CEILING(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 9.9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Double> doubleParam1 = cb.parameter(Double.class);
            ParameterExpression<Double> doubleParam2 = cb.parameter(Double.class);
            cquery.multiselect(intParam1);
            cquery.having(cb.lessThan(doubleParam1, cb.ceiling(doubleParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(doubleParam1, 0d);
            query.setParameter(doubleParam2, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.ceiling(cb2.literal(9.9d))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            cquery3.multiselect(cb3.literal(1));
            ParameterExpression<Double> doubleParam3 = cb3.parameter(Double.class);
            ParameterExpression<Double> doubleParam4 = cb3.parameter(Double.class);
            cquery3.having(cb3.lessThan(doubleParam3, cb3.ceiling(doubleParam4)));

            query = em.createQuery(cquery3);
            query.setParameter(doubleParam3, 0d);
            query.setParameter(doubleParam4, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < CEIL(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testFLOOR1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT FLOOR(?1) FROM JPA31Entity s WHERE s.intVal1 = ?2");
            query.setParameter(1, 1.1);
            query.setParameter(2, 3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT FLOOR(1.1) FROM JPA31ENTITY WHERE (INTVAL1 = 3)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT FLOOR(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT FLOOR(1.1) FROM JPA31Entity s WHERE s.intVal1 = 3");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT FLOOR(1.1) FROM JPA31ENTITY WHERE (INTVAL1 = 3)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT FLOOR(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.select(cb.floor(floatParam1));
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), intParam1));

            query = em.createQuery(cquery);
            query.setParameter(floatParam1, 1.1f);
            query.setParameter(intParam1, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT FLOOR(1.1) FROM JPA31ENTITY WHERE (INTVAL1 = 3)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT FLOOR(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.floor(cb2.literal(1.1f)));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.literal(3)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT FLOOR(1.1) FROM JPA31ENTITY WHERE (INTVAL1 = 3)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT FLOOR(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testFLOOR1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT FLOOR(?1) FROM JPA31Entity s WHERE s.intVal1 = ?2");
            query.setParameter(1, 1.1);
            query.setParameter(2, 3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT FLOOR(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT FLOOR(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT FLOOR(1.1) FROM JPA31Entity s WHERE s.intVal1 = 3");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT FLOOR(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT FLOOR(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.select(cb.floor(floatParam1));
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), intParam1));

            query = em.createQuery(cquery);
            query.setParameter(floatParam1, 1.1f);
            query.setParameter(intParam1, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT FLOOR(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT FLOOR(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.floor(cb2.literal(1.1f)));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.literal(3)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT FLOOR(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT FLOOR(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testFLOOR1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                   Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT FLOOR(?1) FROM JPA31Entity s WHERE s.intVal1 = ?2");
            query.setParameter(1, 1.1);
            query.setParameter(2, 3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT FLOOR(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT FLOOR(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT FLOOR(1.1) FROM JPA31Entity s WHERE s.intVal1 = 3");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT FLOOR(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT FLOOR(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.select(cb.floor(floatParam1));
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), intParam1));

            query = em.createQuery(cquery);
            query.setParameter(floatParam1, 1.1f);
            query.setParameter(intParam1, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT FLOOR(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT FLOOR(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.floor(cb2.literal(1.1f)));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.literal(3)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT FLOOR(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT FLOOR(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testFLOOR2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s WHERE s.intVal1 = FLOOR(?2)");
            query.setParameter(1, 1);
            query.setParameter(2, 3.3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(3.3))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = FLOOR(3.3)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(3.3))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            cquery.select(intParam1);
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), cb.floor(floatParam1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(floatParam1, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(3.3))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.literal(1));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.floor(cb2.literal(3.3f))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(3.3))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testFLOOR2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s WHERE s.intVal1 = FLOOR(?2)");
            query.setParameter(1, 1);
            query.setParameter(2, 3.3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = FLOOR(3.3)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            cquery.select(intParam1);
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), cb.floor(floatParam1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(floatParam1, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.literal(1));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.floor(cb2.literal(3.3f))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testFLOOR2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                   Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s WHERE s.intVal1 = FLOOR(?2)");
            query.setParameter(1, 1);
            query.setParameter(2, 3.3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = FLOOR(3.3)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            cquery.select(intParam1);
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), cb.floor(floatParam1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(floatParam1, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.literal(1));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.floor(cb2.literal(3.3f))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testFLOOR3_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s HAVING ?2 < FLOOR(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 9.9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < FLOOR(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING 0 < FLOOR(9.9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < FLOOR(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING ?1 < FLOOR(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 9.9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < FLOOR(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Double> doubleParam1 = cb.parameter(Double.class);
            ParameterExpression<Double> doubleParam2 = cb.parameter(Double.class);
            cquery.multiselect(intParam1);
            cquery.having(cb.lessThan(doubleParam1, cb.floor(doubleParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(doubleParam1, 0d);
            query.setParameter(doubleParam2, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < FLOOR(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.floor(cb2.literal(9.9d))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < FLOOR(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            cquery3.multiselect(cb3.literal(1));
            ParameterExpression<Double> doubleParam3 = cb3.parameter(Double.class);
            ParameterExpression<Double> doubleParam4 = cb3.parameter(Double.class);
            cquery3.having(cb3.lessThan(doubleParam3, cb3.floor(doubleParam4)));

            query = em.createQuery(cquery3);
            query.setParameter(doubleParam3, 0d);
            query.setParameter(doubleParam4, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < FLOOR(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testFLOOR3_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s HAVING ?2 < FLOOR(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 9.9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING 0 < FLOOR(9.9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING ?1 < FLOOR(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 9.9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Double> doubleParam1 = cb.parameter(Double.class);
            ParameterExpression<Double> doubleParam2 = cb.parameter(Double.class);
            cquery.multiselect(intParam1);
            cquery.having(cb.lessThan(doubleParam1, cb.floor(doubleParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(doubleParam1, 0d);
            query.setParameter(doubleParam2, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.floor(cb2.literal(9.9d))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            cquery3.multiselect(cb3.literal(1));
            ParameterExpression<Double> doubleParam3 = cb3.parameter(Double.class);
            ParameterExpression<Double> doubleParam4 = cb3.parameter(Double.class);
            cquery3.having(cb3.lessThan(doubleParam3, cb3.floor(doubleParam4)));

            query = em.createQuery(cquery3);
            query.setParameter(doubleParam3, 0d);
            query.setParameter(doubleParam4, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testFLOOR3_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                   Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s HAVING ?2 < FLOOR(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 9.9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING 0 < FLOOR(9.9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING ?1 < FLOOR(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 9.9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Double> doubleParam1 = cb.parameter(Double.class);
            ParameterExpression<Double> doubleParam2 = cb.parameter(Double.class);
            cquery.multiselect(intParam1);
            cquery.having(cb.lessThan(doubleParam1, cb.floor(doubleParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(doubleParam1, 0d);
            query.setParameter(doubleParam2, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.floor(cb2.literal(9.9d))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            cquery3.multiselect(cb3.literal(1));
            ParameterExpression<Double> doubleParam3 = cb3.parameter(Double.class);
            ParameterExpression<Double> doubleParam4 = cb3.parameter(Double.class);
            cquery3.having(cb3.lessThan(doubleParam3, cb3.floor(doubleParam4)));

            query = em.createQuery(cquery3);
            query.setParameter(doubleParam3, 0d);
            query.setParameter(doubleParam4, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testEXP1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                 Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT EXP(?1) FROM JPA31Entity s WHERE s.intVal1 = ?2");
            query.setParameter(1, 1.1);
            query.setParameter(2, 3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT EXP(1.1) FROM JPA31ENTITY WHERE (INTVAL1 = 3)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT EXP(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT EXP(1.1) FROM JPA31Entity s WHERE s.intVal1 = 3");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT EXP(1.1) FROM JPA31ENTITY WHERE (INTVAL1 = 3)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT EXP(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.select(cb.exp(floatParam1));
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), intParam1));

            query = em.createQuery(cquery);
            query.setParameter(floatParam1, 1.1f);
            query.setParameter(intParam1, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT EXP(1.1) FROM JPA31ENTITY WHERE (INTVAL1 = 3)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT EXP(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.exp(cb2.literal(1.1f)));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.literal(3)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT EXP(1.1) FROM JPA31ENTITY WHERE (INTVAL1 = 3)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT EXP(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testEXP1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                     Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT EXP(?1) FROM JPA31Entity s WHERE s.intVal1 = ?2");
            query.setParameter(1, 1.1);
            query.setParameter(2, 3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT EXP(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT EXP(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT EXP(1.1) FROM JPA31Entity s WHERE s.intVal1 = 3");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT EXP(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT EXP(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.select(cb.exp(floatParam1));
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), intParam1));

            query = em.createQuery(cquery);
            query.setParameter(floatParam1, 1.1f);
            query.setParameter(intParam1, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT EXP(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT EXP(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.exp(cb2.literal(1.1f)));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.literal(3)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT EXP(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT EXP(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testEXP1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                 Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT EXP(?1) FROM JPA31Entity s WHERE s.intVal1 = ?2");
            query.setParameter(1, 1.1);
            query.setParameter(2, 3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT EXP(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT EXP(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT EXP(1.1) FROM JPA31Entity s WHERE s.intVal1 = 3");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT EXP(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT EXP(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.select(cb.exp(floatParam1));
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), intParam1));

            query = em.createQuery(cquery);
            query.setParameter(floatParam1, 1.1f);
            query.setParameter(intParam1, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT EXP(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT EXP(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.exp(cb2.literal(1.1f)));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.literal(3)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT EXP(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT EXP(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testEXP2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                 Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s WHERE s.intVal1 = EXP(?2)");
            query.setParameter(1, 1);
            query.setParameter(2, 3.3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP(3.3))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = EXP(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = EXP(3.3)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP(3.3))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = EXP(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            cquery.select(intParam1);
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), cb.exp(floatParam1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(floatParam1, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP(3.3))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = EXP(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.literal(1));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.exp(cb2.literal(3.3f))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP(3.3))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = EXP(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testEXP2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                     Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s WHERE s.intVal1 = EXP(?2)");
            query.setParameter(1, 1);
            query.setParameter(2, 3.3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = EXP(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = EXP(3.3)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = EXP(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            cquery.select(intParam1);
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), cb.exp(floatParam1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(floatParam1, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = EXP(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.literal(1));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.exp(cb2.literal(3.3f))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = EXP(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testEXP2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                 Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s WHERE s.intVal1 = EXP(?2)");
            query.setParameter(1, 1);
            query.setParameter(2, 3.3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = EXP(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = EXP(3.3)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = EXP(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            cquery.select(intParam1);
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), cb.exp(floatParam1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(floatParam1, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = EXP(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.literal(1));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.exp(cb2.literal(3.3f))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = EXP(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testEXP3_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                 Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s HAVING ?2 < EXP(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 9.9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < EXP(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING 0 < EXP(9.9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < EXP(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING ?1 < EXP(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 9.9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < EXP(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Double> doubleParam1 = cb.parameter(Double.class);
            ParameterExpression<Double> doubleParam2 = cb.parameter(Double.class);
            cquery.multiselect(intParam1);
            cquery.having(cb.lessThan(doubleParam1, cb.exp(doubleParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(doubleParam1, 0d);
            query.setParameter(doubleParam2, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < EXP(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.exp(cb2.literal(9.9d))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < EXP(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            cquery3.multiselect(cb3.literal(1));
            ParameterExpression<Double> doubleParam3 = cb3.parameter(Double.class);
            ParameterExpression<Double> doubleParam4 = cb3.parameter(Double.class);
            cquery3.having(cb3.lessThan(doubleParam3, cb3.exp(doubleParam4)));

            query = em.createQuery(cquery3);
            query.setParameter(doubleParam3, 0d);
            query.setParameter(doubleParam4, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < EXP(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testEXP3_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                     Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s HAVING ?2 < EXP(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 9.9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING 0 < EXP(9.9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING ?1 < EXP(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 9.9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Double> doubleParam1 = cb.parameter(Double.class);
            ParameterExpression<Double> doubleParam2 = cb.parameter(Double.class);
            cquery.multiselect(intParam1);
            cquery.having(cb.lessThan(doubleParam1, cb.exp(doubleParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(doubleParam1, 0d);
            query.setParameter(doubleParam2, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.exp(cb2.literal(9.9d))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            cquery3.multiselect(cb3.literal(1));
            ParameterExpression<Double> doubleParam3 = cb3.parameter(Double.class);
            ParameterExpression<Double> doubleParam4 = cb3.parameter(Double.class);
            cquery3.having(cb3.lessThan(doubleParam3, cb3.exp(doubleParam4)));

            query = em.createQuery(cquery3);
            query.setParameter(doubleParam3, 0d);
            query.setParameter(doubleParam4, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testEXP3_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                 Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s HAVING ?2 < EXP(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 9.9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING 0 < EXP(9.9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING ?1 < EXP(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 9.9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Double> doubleParam1 = cb.parameter(Double.class);
            ParameterExpression<Double> doubleParam2 = cb.parameter(Double.class);
            cquery.multiselect(intParam1);
            cquery.having(cb.lessThan(doubleParam1, cb.exp(doubleParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(doubleParam1, 0d);
            query.setParameter(doubleParam2, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.exp(cb2.literal(9.9d))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            cquery3.multiselect(cb3.literal(1));
            ParameterExpression<Double> doubleParam3 = cb3.parameter(Double.class);
            ParameterExpression<Double> doubleParam4 = cb3.parameter(Double.class);
            cquery3.having(cb3.lessThan(doubleParam3, cb3.exp(doubleParam4)));

            query = em.createQuery(cquery3);
            query.setParameter(doubleParam3, 0d);
            query.setParameter(doubleParam4, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < EXP(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testLN1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT LN(?1) FROM JPA31Entity s WHERE s.intVal1 = ?2");
            query.setParameter(1, 1.1);
            query.setParameter(2, 3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT LN(1.1) FROM JPA31ENTITY WHERE (INTVAL1 = 3)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT LN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT LN(1.1) FROM JPA31Entity s WHERE s.intVal1 = 3");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT LN(1.1) FROM JPA31ENTITY WHERE (INTVAL1 = 3)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT LN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.select(cb.ln(floatParam1));
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), intParam1));

            query = em.createQuery(cquery);
            query.setParameter(floatParam1, 1.1f);
            query.setParameter(intParam1, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT LN(1.1) FROM JPA31ENTITY WHERE (INTVAL1 = 3)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT LN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.ln(cb2.literal(1.1f)));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.literal(3)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT LN(1.1) FROM JPA31ENTITY WHERE (INTVAL1 = 3)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT LN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testLN1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT LN(?1) FROM JPA31Entity s WHERE s.intVal1 = ?2");
            query.setParameter(1, 1.1);
            query.setParameter(2, 3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT LN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT LN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT LN(1.1) FROM JPA31Entity s WHERE s.intVal1 = 3");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT LN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT LN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.select(cb.ln(floatParam1));
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), intParam1));

            query = em.createQuery(cquery);
            query.setParameter(floatParam1, 1.1f);
            query.setParameter(intParam1, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT LN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT LN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.ln(cb2.literal(1.1f)));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.literal(3)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT LN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT LN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testLN1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT LN(?1) FROM JPA31Entity s WHERE s.intVal1 = ?2");
            query.setParameter(1, 1.1);
            query.setParameter(2, 3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT LN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT LN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT LN(1.1) FROM JPA31Entity s WHERE s.intVal1 = 3");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT LN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT LN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.select(cb.ln(floatParam1));
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), intParam1));

            query = em.createQuery(cquery);
            query.setParameter(floatParam1, 1.1f);
            query.setParameter(intParam1, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT LN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT LN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.ln(cb2.literal(1.1f)));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.literal(3)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT LN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT LN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testLN2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s WHERE s.intVal1 = LN(?2)");
            query.setParameter(1, 1);
            query.setParameter(2, 3.3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = LN(3.3))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = LN(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = LN(3.3)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = LN(3.3))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = LN(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            cquery.select(intParam1);
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), cb.ln(floatParam1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(floatParam1, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = LN(3.3))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = LN(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.literal(1));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.ln(cb2.literal(3.3f))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = LN(3.3))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = LN(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testLN2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s WHERE s.intVal1 = LN(?2)");
            query.setParameter(1, 1);
            query.setParameter(2, 3.3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = LN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = LN(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = LN(3.3)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = LN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = LN(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            cquery.select(intParam1);
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), cb.ln(floatParam1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(floatParam1, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = LN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = LN(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.literal(1));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.ln(cb2.literal(3.3f))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = LN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = LN(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testLN2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s WHERE s.intVal1 = LN(?2)");
            query.setParameter(1, 1);
            query.setParameter(2, 3.3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = LN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = LN(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = LN(3.3)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = LN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = LN(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            cquery.select(intParam1);
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), cb.ln(floatParam1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(floatParam1, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = LN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = LN(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.literal(1));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.ln(cb2.literal(3.3f))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = LN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = LN(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testLN3_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s HAVING ?2 < LN(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 9.9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < LN(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING 0 < LN(9.9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < LN(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING ?1 < LN(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 9.9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < LN(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Double> doubleParam1 = cb.parameter(Double.class);
            ParameterExpression<Double> doubleParam2 = cb.parameter(Double.class);
            cquery.multiselect(intParam1);
            cquery.having(cb.lessThan(doubleParam1, cb.ln(doubleParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(doubleParam1, 0d);
            query.setParameter(doubleParam2, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < LN(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.ln(cb2.literal(9.9d))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < LN(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            cquery3.multiselect(cb3.literal(1));
            ParameterExpression<Double> doubleParam3 = cb3.parameter(Double.class);
            ParameterExpression<Double> doubleParam4 = cb3.parameter(Double.class);
            cquery3.having(cb3.lessThan(doubleParam3, cb3.ln(doubleParam4)));

            query = em.createQuery(cquery3);
            query.setParameter(doubleParam3, 0d);
            query.setParameter(doubleParam4, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < LN(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testLN3_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s HAVING ?2 < LN(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 9.9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING 0 < LN(9.9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING ?1 < LN(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 9.9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Double> doubleParam1 = cb.parameter(Double.class);
            ParameterExpression<Double> doubleParam2 = cb.parameter(Double.class);
            cquery.multiselect(intParam1);
            cquery.having(cb.lessThan(doubleParam1, cb.ln(doubleParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(doubleParam1, 0d);
            query.setParameter(doubleParam2, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.ln(cb2.literal(9.9d))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            cquery3.multiselect(cb3.literal(1));
            ParameterExpression<Double> doubleParam3 = cb3.parameter(Double.class);
            ParameterExpression<Double> doubleParam4 = cb3.parameter(Double.class);
            cquery3.having(cb3.lessThan(doubleParam3, cb3.ln(doubleParam4)));

            query = em.createQuery(cquery3);
            query.setParameter(doubleParam3, 0d);
            query.setParameter(doubleParam4, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testLN3_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s HAVING ?2 < LN(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 9.9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING 0 < LN(9.9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING ?1 < LN(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 9.9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Double> doubleParam1 = cb.parameter(Double.class);
            ParameterExpression<Double> doubleParam2 = cb.parameter(Double.class);
            cquery.multiselect(intParam1);
            cquery.having(cb.lessThan(doubleParam1, cb.ln(doubleParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(doubleParam1, 0d);
            query.setParameter(doubleParam2, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.ln(cb2.literal(9.9d))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            cquery3.multiselect(cb3.literal(1));
            ParameterExpression<Double> doubleParam3 = cb3.parameter(Double.class);
            ParameterExpression<Double> doubleParam4 = cb3.parameter(Double.class);
            cquery3.having(cb3.lessThan(doubleParam3, cb3.ln(doubleParam4)));

            query = em.createQuery(cquery3);
            query.setParameter(doubleParam3, 0d);
            query.setParameter(doubleParam4, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < LN(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testPOWER1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT POWER(?1, ?2) FROM JPA31Entity s WHERE s.intVal1 = ?3");
            query.setParameter(1, 1.1);
            query.setParameter(2, 3);
            query.setParameter(3, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT EXP((3)*LN(1.1)) FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT POWER(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT POWER(1.1, 3) FROM JPA31Entity s WHERE s.intVal1 = 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT EXP((3)*LN(1.1)) FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT POWER(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT POWER(1.1, ?1) FROM JPA31Entity s WHERE s.intVal1 = 9");
            query.setParameter(1, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT EXP((3)*LN(1.1)) FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT POWER(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.select(cb.power(floatParam1, intParam1));
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), intParam2));

            query = em.createQuery(cquery);
            query.setParameter(floatParam1, 1.1f);
            query.setParameter(intParam1, 3);
            query.setParameter(intParam2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT EXP((3)*LN(1.1)) FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT POWER(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.power(cb2.literal(1.1f), cb2.literal(3)));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.literal(9)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT EXP((3)*LN(1.1)) FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT POWER(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery3 = cb3.createQuery(Object.class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery3.select(cb3.power(cb3.literal(1.1f), intParam3));
            cquery3.where(cb3.equal(root3.get(JPA31Entity_.intVal1), cb3.literal(9)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT EXP((3)*LN(1.1)) FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT POWER(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testPOWER1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT POWER(?1, ?2) FROM JPA31Entity s WHERE s.intVal1 = ?3");
            query.setParameter(1, 1.1);
            query.setParameter(2, 3);
            query.setParameter(3, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT EXP((?)*LN(?)) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT POWER(1.1, 3) FROM JPA31Entity s WHERE s.intVal1 = 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT EXP((?)*LN(?)) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT POWER(1.1, ?1) FROM JPA31Entity s WHERE s.intVal1 = 9");
            query.setParameter(1, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT EXP((?)*LN(?)) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.select(cb.power(floatParam1, intParam1));
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), intParam2));

            query = em.createQuery(cquery);
            query.setParameter(floatParam1, 1.1f);
            query.setParameter(intParam1, 3);
            query.setParameter(intParam2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT EXP((?)*LN(?)) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.power(cb2.literal(1.1f), cb2.literal(3)));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.literal(9)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT EXP((?)*LN(?)) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery3 = cb3.createQuery(Object.class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery3.select(cb3.power(cb3.literal(1.1f), intParam3));
            cquery3.where(cb3.equal(root3.get(JPA31Entity_.intVal1), cb3.literal(9)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT EXP((?)*LN(?)) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testPOWER1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                   Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT POWER(?1, ?2) FROM JPA31Entity s WHERE s.intVal1 = ?3");
            query.setParameter(1, 1.1);
            query.setParameter(2, 3);
            query.setParameter(3, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT EXP((?)*LN(?)) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT POWER(1.1, 3) FROM JPA31Entity s WHERE s.intVal1 = 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT EXP((?)*LN(?)) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT POWER(1.1, ?1) FROM JPA31Entity s WHERE s.intVal1 = 9");
            query.setParameter(1, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT EXP((?)*LN(?)) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.select(cb.power(floatParam1, intParam1));
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), intParam2));

            query = em.createQuery(cquery);
            query.setParameter(floatParam1, 1.1f);
            query.setParameter(intParam1, 3);
            query.setParameter(intParam2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT EXP((?)*LN(?)) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.power(cb2.literal(1.1f), cb2.literal(3)));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.literal(9)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT EXP((?)*LN(?)) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery3 = cb3.createQuery(Object.class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery3.select(cb3.power(cb3.literal(1.1f), intParam3));
            cquery3.where(cb3.equal(root3.get(JPA31Entity_.intVal1), cb3.literal(9)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT EXP((?)*LN(?)) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT POWER(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testPOWER2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s WHERE s.intVal1 = POWER(?2, ?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 3.3);
            query.setParameter(3, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP((9)*LN(3.3)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = POWER(3.3, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = POWER(3.3, 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP((9)*LN(3.3)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = POWER(3.3, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = POWER(?1, 9)");
            query.setParameter(1, 3.3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP((9)*LN(3.3)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = POWER(3.3, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.select(intParam1);
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), cb.power(floatParam1, intParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(floatParam1, 3.3f);
            query.setParameter(intParam2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP((9)*LN(3.3)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = POWER(3.3, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.literal(1));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.power(cb2.literal(3.3f), cb2.literal(9))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP((9)*LN(3.3)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = POWER(3.3, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery3 = cb3.createQuery(Object.class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam2 = cb3.parameter(Float.class);
            cquery3.select(cb3.literal(1));
            cquery3.where(cb3.equal(root3.get(JPA31Entity_.intVal1), cb3.power(floatParam2, cb3.literal(9))));

            query = em.createQuery(cquery3);
            query.setParameter(floatParam2, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP((9)*LN(3.3)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = POWER(3.3, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testPOWER2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s WHERE s.intVal1 = POWER(?2, ?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 3.3);
            query.setParameter(3, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = POWER(3.3, 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = POWER(?1, 9)");
            query.setParameter(1, 3.3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.select(intParam1);
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), cb.power(floatParam1, intParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(floatParam1, 3.3f);
            query.setParameter(intParam2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.literal(1));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.power(cb2.literal(3.3f), cb2.literal(9))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery3 = cb3.createQuery(Object.class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam2 = cb3.parameter(Float.class);
            cquery3.select(cb3.literal(1));
            cquery3.where(cb3.equal(root3.get(JPA31Entity_.intVal1), cb3.power(floatParam2, cb3.literal(9))));

            query = em.createQuery(cquery3);
            query.setParameter(floatParam2, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testPOWER2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                   Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s WHERE s.intVal1 = POWER(?2, ?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 3.3);
            query.setParameter(3, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = POWER(3.3, 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = POWER(?1, 9)");
            query.setParameter(1, 3.3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.select(intParam1);
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), cb.power(floatParam1, intParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(floatParam1, 3.3f);
            query.setParameter(intParam2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.literal(1));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.power(cb2.literal(3.3f), cb2.literal(9))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery3 = cb3.createQuery(Object.class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam2 = cb3.parameter(Float.class);
            cquery3.select(cb3.literal(1));
            cquery3.where(cb3.equal(root3.get(JPA31Entity_.intVal1), cb3.power(floatParam2, cb3.literal(9))));

            query = em.createQuery(cquery3);
            query.setParameter(floatParam2, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = POWER(?, ?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testPOWER3_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s HAVING ?2 < POWER(?3, ?4)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 9.9);
            query.setParameter(4, 2);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < EXP((2)*LN(9.9)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < POWER(9.9, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING 0 < POWER(9.9, 2)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < EXP((2)*LN(9.9)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < POWER(9.9, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING ?1 < POWER(?2, 2)");
            query.setParameter(1, 0);
            query.setParameter(2, 9.9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < EXP((2)*LN(9.9)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < POWER(9.9, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Double> doubleParam1 = cb.parameter(Double.class);
            ParameterExpression<Double> doubleParam2 = cb.parameter(Double.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(intParam1);
            cquery.having(cb.lessThan(doubleParam1, cb.power(doubleParam2, intParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(doubleParam1, 0d);
            query.setParameter(doubleParam2, 9.9d);
            query.setParameter(intParam2, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < EXP((2)*LN(9.9)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < POWER(9.9, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.power(cb2.literal(9.9d), cb2.literal(2))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < EXP((2)*LN(9.9)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < POWER(9.9, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            cquery3.multiselect(cb3.literal(1));
            ParameterExpression<Double> doubleParam3 = cb3.parameter(Double.class);
            ParameterExpression<Double> doubleParam4 = cb3.parameter(Double.class);
            cquery3.having(cb3.lessThan(doubleParam3, cb3.power(doubleParam4, cb2.literal(2))));

            query = em.createQuery(cquery3);
            query.setParameter(doubleParam3, 0d);
            query.setParameter(doubleParam4, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < EXP((2)*LN(9.9)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < POWER(9.9, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testPOWER3_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s HAVING ?2 < POWER(?3, ?4)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 9.9);
            query.setParameter(4, 2);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING 0 < POWER(9.9, 2)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING ?1 < POWER(?2, 2)");
            query.setParameter(1, 0);
            query.setParameter(2, 9.9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Double> doubleParam1 = cb.parameter(Double.class);
            ParameterExpression<Double> doubleParam2 = cb.parameter(Double.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(intParam1);
            cquery.having(cb.lessThan(doubleParam1, cb.power(doubleParam2, intParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(doubleParam1, 0d);
            query.setParameter(doubleParam2, 9.9d);
            query.setParameter(intParam2, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.power(cb2.literal(9.9d), cb2.literal(2))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            cquery3.multiselect(cb3.literal(1));
            ParameterExpression<Double> doubleParam3 = cb3.parameter(Double.class);
            ParameterExpression<Double> doubleParam4 = cb3.parameter(Double.class);
            cquery3.having(cb3.lessThan(doubleParam3, cb3.power(doubleParam4, cb2.literal(2))));

            query = em.createQuery(cquery3);
            query.setParameter(doubleParam3, 0d);
            query.setParameter(doubleParam4, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testPOWER3_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                   Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s HAVING ?2 < POWER(?3, ?4)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 9.9);
            query.setParameter(4, 2);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING 0 < POWER(9.9, 2)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING ?1 < POWER(?2, 2)");
            query.setParameter(1, 0);
            query.setParameter(2, 9.9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Double> doubleParam1 = cb.parameter(Double.class);
            ParameterExpression<Double> doubleParam2 = cb.parameter(Double.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(intParam1);
            cquery.having(cb.lessThan(doubleParam1, cb.power(doubleParam2, intParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(doubleParam1, 0d);
            query.setParameter(doubleParam2, 9.9d);
            query.setParameter(intParam2, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.power(cb2.literal(9.9d), cb2.literal(2))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            cquery3.multiselect(cb3.literal(1));
            ParameterExpression<Double> doubleParam3 = cb3.parameter(Double.class);
            ParameterExpression<Double> doubleParam4 = cb3.parameter(Double.class);
            cquery3.having(cb3.lessThan(doubleParam3, cb3.power(doubleParam4, cb2.literal(2))));

            query = em.createQuery(cquery3);
            query.setParameter(doubleParam3, 0d);
            query.setParameter(doubleParam4, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < EXP((?)*LN(?)))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < POWER(?, ?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testROUND1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);
        boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.POSTGRES);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ROUND(?1, ?2) FROM JPA31Entity s WHERE s.intVal1 = ?3");
            query.setParameter(1, 1.1);
            query.setParameter(2, 3);
            query.setParameter(3, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT FLOOR((1.1)*1e3+0.5)/1e3 FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT FLOOR((?)*10^(?)+0.5)/10^(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT ROUND(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ROUND(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT ROUND(1.1, 3) FROM JPA31Entity s WHERE s.intVal1 = 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT FLOOR((1.1)*1e3+0.5)/1e3 FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT FLOOR((?)*10^(?)+0.5)/10^(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT ROUND(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ROUND(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT ROUND(1.1, ?1) FROM JPA31Entity s WHERE s.intVal1 = 9");
            query.setParameter(1, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT FLOOR((1.1)*1e3+0.5)/1e3 FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT FLOOR((?)*10^(?)+0.5)/10^(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT ROUND(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ROUND(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.select(cb.round(floatParam1, 3));
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), intParam2));

            query = em.createQuery(cquery);
            query.setParameter(floatParam1, 1.1f);
            query.setParameter(intParam2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT FLOOR((1.1)*1e3+0.5)/1e3 FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT FLOOR((?)*10^(?)+0.5)/10^(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT ROUND(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ROUND(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.round(cb2.literal(1.1f), 3));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.literal(9)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT FLOOR((1.1)*1e3+0.5)/1e3 FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT FLOOR((?)*10^(?)+0.5)/10^(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT ROUND(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ROUND(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery3 = cb3.createQuery(Object.class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam2 = cb.parameter(Float.class);
            cquery3.select(cb3.round(floatParam2, 3));
            cquery3.where(cb3.equal(root3.get(JPA31Entity_.intVal1), cb3.literal(9)));

            query = em.createQuery(cquery3);
            query.setParameter(floatParam2, 1.1f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT FLOOR((1.1)*1e3+0.5)/1e3 FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT FLOOR((?)*10^(?)+0.5)/10^(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT ROUND(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ROUND(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testROUND1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);
        boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.POSTGRES);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/23566 is delivered
        if (isDB2 || isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ROUND(?1, ?2) FROM JPA31Entity s WHERE s.intVal1 = ?3");
            query.setParameter(1, 1.1);
            query.setParameter(2, 3);
            query.setParameter(3, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT FLOOR((1.1)*1e3+0.5)/1e3 FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT FLOOR((?)*10^(?)+0.5)/10^(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT ROUND(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT ROUND(?, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ROUND(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT ROUND(1.1, 3) FROM JPA31Entity s WHERE s.intVal1 = 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT FLOOR((1.1)*1e3+0.5)/1e3 FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT FLOOR((?)*10^(?)+0.5)/10^(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT ROUND(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT ROUND(?, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ROUND(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT ROUND(1.1, ?1) FROM JPA31Entity s WHERE s.intVal1 = 9");
            query.setParameter(1, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT FLOOR((1.1)*1e3+0.5)/1e3 FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT FLOOR((?)*10^(?)+0.5)/10^(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT ROUND(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT ROUND(?, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ROUND(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.select(cb.round(floatParam1, 3));
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), intParam2));

            query = em.createQuery(cquery);
            query.setParameter(floatParam1, 1.1f);
            query.setParameter(intParam2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT FLOOR((1.1)*1e3+0.5)/1e3 FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT FLOOR((?)*10^(?)+0.5)/10^(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT ROUND(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT ROUND(?, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ROUND(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.round(cb2.literal(1.1f), 3));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.literal(9)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT FLOOR((1.1)*1e3+0.5)/1e3 FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT FLOOR((?)*10^(?)+0.5)/10^(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT ROUND(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT ROUND(?, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ROUND(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery3 = cb3.createQuery(Object.class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam2 = cb.parameter(Float.class);
            cquery3.select(cb3.round(floatParam2, 3));
            cquery3.where(cb3.equal(root3.get(JPA31Entity_.intVal1), cb3.literal(9)));

            query = em.createQuery(cquery3);
            query.setParameter(floatParam2, 1.1f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT FLOOR((1.1)*1e3+0.5)/1e3 FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT FLOOR((?)*10^(?)+0.5)/10^(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT ROUND(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT ROUND(?, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ROUND(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testROUND1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                   Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);
        boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.POSTGRES);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/23566 is delivered
        if (isDB2 || isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ROUND(?1, ?2) FROM JPA31Entity s WHERE s.intVal1 = ?3");
            query.setParameter(1, 1.1);
            query.setParameter(2, 3);
            query.setParameter(3, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT FLOOR((1.1)*1e3+0.5)/1e3 FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT FLOOR((?)*10^(?)+0.5)/10^(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT ROUND(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT ROUND(?, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ROUND(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT ROUND(1.1, 3) FROM JPA31Entity s WHERE s.intVal1 = 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT FLOOR((1.1)*1e3+0.5)/1e3 FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT FLOOR((?)*10^(?)+0.5)/10^(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT ROUND(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT ROUND(?, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ROUND(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT ROUND(1.1, ?1) FROM JPA31Entity s WHERE s.intVal1 = 9");
            query.setParameter(1, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT FLOOR((1.1)*1e3+0.5)/1e3 FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT FLOOR((?)*10^(?)+0.5)/10^(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT ROUND(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT ROUND(?, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ROUND(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.select(cb.round(floatParam1, 3));
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), intParam2));

            query = em.createQuery(cquery);
            query.setParameter(floatParam1, 1.1f);
            query.setParameter(intParam2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT FLOOR((1.1)*1e3+0.5)/1e3 FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT FLOOR((?)*10^(?)+0.5)/10^(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT ROUND(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT ROUND(?, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ROUND(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.round(cb2.literal(1.1f), 3));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.literal(9)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT FLOOR((1.1)*1e3+0.5)/1e3 FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT FLOOR((?)*10^(?)+0.5)/10^(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT ROUND(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT ROUND(?, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ROUND(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery3 = cb3.createQuery(Object.class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam2 = cb.parameter(Float.class);
            cquery3.select(cb3.round(floatParam2, 3));
            cquery3.where(cb3.equal(root3.get(JPA31Entity_.intVal1), cb3.literal(9)));

            query = em.createQuery(cquery3);
            query.setParameter(floatParam2, 1.1f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT FLOOR((1.1)*1e3+0.5)/1e3 FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT FLOOR((?)*10^(?)+0.5)/10^(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT ROUND(1.1, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT ROUND(?, 3) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ROUND(?, ?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testROUND2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);
        boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.POSTGRES);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s WHERE s.intVal1 = ROUND(?2, ?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 3.3);
            query.setParameter(3, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((3.3)*1e9+0.5)/1e9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(3.3, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = ROUND(3.3, 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((3.3)*1e9+0.5)/1e9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(3.3, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = ROUND(?1, 9)");
            query.setParameter(1, 3.3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((3.3)*1e9+0.5)/1e9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(3.3, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            cquery.select(intParam1);
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), cb.round(floatParam1, 9)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(floatParam1, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((3.3)*1e9+0.5)/1e9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(3.3, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.literal(1));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.round(cb2.literal(3.3f), 9)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((3.3)*1e9+0.5)/1e9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(3.3, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery3 = cb3.createQuery(Object.class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam2 = cb3.parameter(Float.class);
            cquery3.select(cb3.literal(1));
            cquery3.where(cb3.equal(root3.get(JPA31Entity_.intVal1), cb3.round(floatParam2, 9)));

            query = em.createQuery(cquery3);
            query.setParameter(floatParam2, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((3.3)*1e9+0.5)/1e9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(3.3, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, ?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testROUND2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);
        boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.POSTGRES);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/23566 is delivered
        if (isDB2 || isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s WHERE s.intVal1 = ROUND(?2, ?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 3.3);
            query.setParameter(3, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((3.3)*1e9+0.5)/1e9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(3.3, 9))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = ROUND(3.3, 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((3.3)*1e9+0.5)/1e9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(3.3, 9))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = ROUND(?1, 9)");
            query.setParameter(1, 3.3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((3.3)*1e9+0.5)/1e9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(3.3, 9))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            cquery.select(intParam1);
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), cb.round(floatParam1, 9)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(floatParam1, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((3.3)*1e9+0.5)/1e9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(3.3, 9))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.literal(1));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.round(cb2.literal(3.3f), 9)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((3.3)*1e9+0.5)/1e9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(3.3, 9))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery3 = cb3.createQuery(Object.class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam2 = cb3.parameter(Float.class);
            cquery3.select(cb3.literal(1));
            cquery3.where(cb3.equal(root3.get(JPA31Entity_.intVal1), cb3.round(floatParam2, 9)));

            query = em.createQuery(cquery3);
            query.setParameter(floatParam2, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((3.3)*1e9+0.5)/1e9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(3.3, 9))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, ?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testROUND2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                   Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);
        boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.POSTGRES);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/23566 is delivered
        if (isDB2 || isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s WHERE s.intVal1 = ROUND(?2, ?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 3.3);
            query.setParameter(3, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((3.3)*1e9+0.5)/1e9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(3.3, 9))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = ROUND(3.3, 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((3.3)*1e9+0.5)/1e9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(3.3, 9))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = ROUND(?1, 9)");
            query.setParameter(1, 3.3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((3.3)*1e9+0.5)/1e9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(3.3, 9))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            cquery.select(intParam1);
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), cb.round(floatParam1, 9)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(floatParam1, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((3.3)*1e9+0.5)/1e9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(3.3, 9))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.literal(1));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.round(cb2.literal(3.3f), 9)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((3.3)*1e9+0.5)/1e9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(3.3, 9))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery3 = cb3.createQuery(Object.class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam2 = cb3.parameter(Float.class);
            cquery3.select(cb3.literal(1));
            cquery3.where(cb3.equal(root3.get(JPA31Entity_.intVal1), cb3.round(floatParam2, 9)));

            query = em.createQuery(cquery3);
            query.setParameter(floatParam2, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((3.3)*1e9+0.5)/1e9)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(3.3, 9))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = ROUND(?, ?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testROUND3_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);
        boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.POSTGRES);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s HAVING ?2 < ROUND(?3, ?4)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 9.9);
            query.setParameter(4, 2);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < FLOOR((9.9)*1e2+0.5)/1e2)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < ROUND(9.9, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < ROUND(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING 0 < ROUND(9.9, 2)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < FLOOR((9.9)*1e2+0.5)/1e2)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < ROUND(9.9, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < ROUND(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING ?1 < ROUND(?2, 2)");
            query.setParameter(1, 0);
            query.setParameter(2, 9.9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < FLOOR((9.9)*1e2+0.5)/1e2)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < ROUND(9.9, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < ROUND(?, ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Double> doubleParam1 = cb.parameter(Double.class);
            ParameterExpression<Double> doubleParam2 = cb.parameter(Double.class);
            cquery.multiselect(intParam1);
            cquery.having(cb.lessThan(doubleParam1, cb.round(doubleParam2, 2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(doubleParam1, 0d);
            query.setParameter(doubleParam2, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < FLOOR((9.9)*1e2+0.5)/1e2)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < ROUND(9.9, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < ROUND(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.round(cb2.literal(9.9d), 2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < FLOOR((9.9)*1e2+0.5)/1e2)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < ROUND(9.9, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < ROUND(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            cquery3.multiselect(cb3.literal(1));
            ParameterExpression<Double> doubleParam3 = cb3.parameter(Double.class);
            ParameterExpression<Double> doubleParam4 = cb3.parameter(Double.class);
            cquery3.having(cb3.lessThan(doubleParam3, cb3.round(doubleParam4, 2)));

            query = em.createQuery(cquery3);
            query.setParameter(doubleParam3, 0d);
            query.setParameter(doubleParam4, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < FLOOR((9.9)*1e2+0.5)/1e2)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0.0 < ROUND(9.9, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < ROUND(?, ?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testROUND3_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);
        boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.POSTGRES);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/23566 is delivered
        if (isDB2 || isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s HAVING ?2 < ROUND(?3, ?4)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 9.9);
            query.setParameter(4, 2);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR((9.9)*1e2+0.5)/1e2)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(9.9, 2))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(?, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < ROUND(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING 0 < ROUND(9.9, 2)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR((9.9)*1e2+0.5)/1e2)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(9.9, 2))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(?, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < ROUND(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING ?1 < ROUND(?2, 2)");
            query.setParameter(1, 0);
            query.setParameter(2, 9.9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR((9.9)*1e2+0.5)/1e2)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(9.9, 2))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(?, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < ROUND(?, ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Double> doubleParam1 = cb.parameter(Double.class);
            ParameterExpression<Double> doubleParam2 = cb.parameter(Double.class);
            cquery.multiselect(intParam1);
            cquery.having(cb.lessThan(doubleParam1, cb.round(doubleParam2, 2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(doubleParam1, 0d);
            query.setParameter(doubleParam2, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR((9.9)*1e2+0.5)/1e2)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(9.9, 2))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(?, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < ROUND(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.round(cb2.literal(9.9d), 2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR((9.9)*1e2+0.5)/1e2)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(9.9, 2))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(?, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < ROUND(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            cquery3.multiselect(cb3.literal(1));
            ParameterExpression<Double> doubleParam3 = cb3.parameter(Double.class);
            ParameterExpression<Double> doubleParam4 = cb3.parameter(Double.class);
            cquery3.having(cb3.lessThan(doubleParam3, cb3.round(doubleParam4, 2)));

            query = em.createQuery(cquery3);
            query.setParameter(doubleParam3, 0d);
            query.setParameter(doubleParam4, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR((9.9)*1e2+0.5)/1e2)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(9.9, 2))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(?, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < ROUND(?, ?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testROUND3_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                   Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);
        boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.POSTGRES);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/23566 is delivered
        if (isDB2 || isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s HAVING ?2 < ROUND(?3, ?4)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 9.9);
            query.setParameter(4, 2);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR((9.9)*1e2+0.5)/1e2)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(9.9, 2))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(?, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < ROUND(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING 0 < ROUND(9.9, 2)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR((9.9)*1e2+0.5)/1e2)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(9.9, 2))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(?, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < ROUND(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING ?1 < ROUND(?2, 2)");
            query.setParameter(1, 0);
            query.setParameter(2, 9.9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR((9.9)*1e2+0.5)/1e2)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(9.9, 2))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(?, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < ROUND(?, ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Double> doubleParam1 = cb.parameter(Double.class);
            ParameterExpression<Double> doubleParam2 = cb.parameter(Double.class);
            cquery.multiselect(intParam1);
            cquery.having(cb.lessThan(doubleParam1, cb.round(doubleParam2, 2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(doubleParam1, 0d);
            query.setParameter(doubleParam2, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR((9.9)*1e2+0.5)/1e2)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(9.9, 2))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(?, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < ROUND(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.round(cb2.literal(9.9d), 2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR((9.9)*1e2+0.5)/1e2)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(9.9, 2))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(?, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < ROUND(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            cquery3.multiselect(cb3.literal(1));
            ParameterExpression<Double> doubleParam3 = cb3.parameter(Double.class);
            ParameterExpression<Double> doubleParam4 = cb3.parameter(Double.class);
            cquery3.having(cb3.lessThan(doubleParam3, cb3.round(doubleParam4, 2)));

            query = em.createQuery(cquery3);
            query.setParameter(doubleParam3, 0d);
            query.setParameter(doubleParam4, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < FLOOR((9.9)*1e2+0.5)/1e2)", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < FLOOR((?)*10^(?)+0.5)/10^(?))", sql.remove(0));
            } else if (isDB2Z) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(9.9, 2))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < ROUND(?, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < ROUND(?, ?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testSIGN1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT SIGN(?1) FROM JPA31Entity s WHERE s.intVal1 = ?2");
            query.setParameter(1, 1.1);
            query.setParameter(2, 3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SIGN(1.1) FROM JPA31ENTITY WHERE (INTVAL1 = 3)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SIGN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT SIGN(1.1) FROM JPA31Entity s WHERE s.intVal1 = 3");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SIGN(1.1) FROM JPA31ENTITY WHERE (INTVAL1 = 3)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SIGN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.select(cb.sign(floatParam1));
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), intParam1));

            query = em.createQuery(cquery);
            query.setParameter(floatParam1, 1.1f);
            query.setParameter(intParam1, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SIGN(1.1) FROM JPA31ENTITY WHERE (INTVAL1 = 3)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SIGN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.sign(cb2.literal(1.1f)));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.literal(3)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SIGN(1.1) FROM JPA31ENTITY WHERE (INTVAL1 = 3)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SIGN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testSIGN1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                      Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT SIGN(?1) FROM JPA31Entity s WHERE s.intVal1 = ?2");
            query.setParameter(1, 1.1);
            query.setParameter(2, 3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SIGN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SIGN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT SIGN(1.1) FROM JPA31Entity s WHERE s.intVal1 = 3");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SIGN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SIGN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.select(cb.sign(floatParam1));
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), intParam1));

            query = em.createQuery(cquery);
            query.setParameter(floatParam1, 1.1f);
            query.setParameter(intParam1, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SIGN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SIGN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.sign(cb2.literal(1.1f)));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.literal(3)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SIGN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SIGN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testSIGN1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                  Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT SIGN(?1) FROM JPA31Entity s WHERE s.intVal1 = ?2");
            query.setParameter(1, 1.1);
            query.setParameter(2, 3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SIGN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SIGN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT SIGN(1.1) FROM JPA31Entity s WHERE s.intVal1 = 3");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SIGN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SIGN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.select(cb.sign(floatParam1));
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), intParam1));

            query = em.createQuery(cquery);
            query.setParameter(floatParam1, 1.1f);
            query.setParameter(intParam1, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SIGN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SIGN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.sign(cb2.literal(1.1f)));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.literal(3)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SIGN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SIGN(?) FROM JPA31ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testSIGN2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s WHERE s.intVal1 = SIGN(?2)");
            query.setParameter(1, 1);
            query.setParameter(2, 3.3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(3.3))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = SIGN(3.3)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(3.3))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            cquery.select(intParam1);
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), cb.sign(floatParam1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(floatParam1, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(3.3))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.literal(1));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.sign(cb2.literal(3.3f))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(3.3))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testSIGN2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                      Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s WHERE s.intVal1 = SIGN(?2)");
            query.setParameter(1, 1);
            query.setParameter(2, 3.3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = SIGN(3.3)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            cquery.select(intParam1);
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), cb.sign(floatParam1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(floatParam1, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.literal(1));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.sign(cb2.literal(3.3f))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testSIGN2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                  Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s WHERE s.intVal1 = SIGN(?2)");
            query.setParameter(1, 1);
            query.setParameter(2, 3.3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s WHERE s.intVal1 = SIGN(3.3)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Float> floatParam1 = cb.parameter(Float.class);
            cquery.select(intParam1);
            cquery.where(cb.equal(root.get(JPA31Entity_.intVal1), cb.sign(floatParam1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(floatParam1, 3.3f);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.select(cb2.literal(1));
            cquery2.where(cb2.equal(root2.get(JPA31Entity_.intVal1), cb2.sign(cb2.literal(3.3f))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY WHERE (INTVAL1 = SIGN(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testSIGN3_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s HAVING ?2 < SIGN(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 9.9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < SIGN(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING 0 < SIGN(9.9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < SIGN(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING ?1 < SIGN(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 9.9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < SIGN(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            ParameterExpression<Double> doubleParam2 = cb.parameter(Double.class);
            cquery.multiselect(intParam1);
            cquery.having(cb.lessThan(intParam2, cb.sign(doubleParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 0);
            query.setParameter(doubleParam2, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < SIGN(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.having(cb2.lessThan(cb2.literal(0), cb2.sign(cb2.literal(9.9d))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < SIGN(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            cquery3.multiselect(cb3.literal(1));
            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            ParameterExpression<Double> doubleParam4 = cb3.parameter(Double.class);
            cquery3.having(cb3.lessThan(intParam3, cb3.sign(doubleParam4)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 0);
            query.setParameter(doubleParam4, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (0 < SIGN(9.9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testSIGN3_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                      Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s HAVING ?2 < SIGN(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 9.9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING 0 < SIGN(9.9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING ?1 < SIGN(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 9.9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            ParameterExpression<Double> doubleParam2 = cb.parameter(Double.class);
            cquery.multiselect(intParam1);
            cquery.having(cb.lessThan(intParam2, cb.sign(doubleParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 0);
            query.setParameter(doubleParam2, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.having(cb2.lessThan(cb2.literal(0), cb2.sign(cb2.literal(9.9d))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            cquery3.multiselect(cb3.literal(1));
            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            ParameterExpression<Double> doubleParam4 = cb3.parameter(Double.class);
            cquery3.having(cb3.lessThan(intParam3, cb3.sign(doubleParam4)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 0);
            query.setParameter(doubleParam4, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testSIGN3_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                  Object managedComponentObject) throws Throwable {
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

        boolean isDB2Z = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2ZOS);
        boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DB2LUW);
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // TODO: Disable until https://github.com/OpenLiberty/open-liberty/issues/24603 is delivered
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT ?1 FROM JPA31Entity s HAVING ?2 < SIGN(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 9.9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING 0 < SIGN(9.9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM JPA31Entity s HAVING ?1 < SIGN(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 9.9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<JPA31Entity> root = cquery.from(JPA31Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            ParameterExpression<Double> doubleParam2 = cb.parameter(Double.class);
            cquery.multiselect(intParam1);
            cquery.having(cb.lessThan(intParam2, cb.sign(doubleParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 0);
            query.setParameter(doubleParam2, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<JPA31Entity> root2 = cquery2.from(JPA31Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.having(cb2.lessThan(cb2.literal(0), cb2.sign(cb2.literal(9.9d))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<JPA31Entity> root3 = cquery3.from(JPA31Entity.class);
            cquery3.multiselect(cb3.literal(1));
            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            ParameterExpression<Double> doubleParam4 = cb3.parameter(Double.class);
            cquery3.having(cb3.lessThan(intParam3, cb3.sign(doubleParam4)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 0);
            query.setParameter(doubleParam4, 9.9d);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM JPA31ENTITY HAVING (? < SIGN(?))", sql.remove(0));
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

}
