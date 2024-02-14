/*******************************************************************************
 * Copyright (c) 2022,2024 IBM Corporation and others.
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

package com.ibm.ws.jpa.olgh17837.testlogic;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh17837.model.OLGH17837Entity;
import com.ibm.ws.jpa.olgh17837.model.OLGH17837Entity_;
import com.ibm.ws.jpa.query.sqlcapture.SQLCallListener;
import com.ibm.ws.testtooling.database.DatabaseVendor;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class TestOrderingLogic extends AbstractTestLogic {

    public void testAscending1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s ORDER BY ?1 ASC");
            query.setParameter(1, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? ASC", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s ORDER BY 1 ASC");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? ASC", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.orderBy(cb.asc(intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? ASC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.orderBy(cb2.asc(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? ASC", sql.remove(0));
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

    public void testAscending1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s ORDER BY ?1 ASC");
            query.setParameter(1, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? ASC", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s ORDER BY 1 ASC");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? ASC", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.orderBy(cb.asc(intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? ASC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.orderBy(cb2.asc(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? ASC", sql.remove(0));
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

    public void testAscending1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s ORDER BY ?1 ASC");
            query.setParameter(1, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? ASC", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s ORDER BY 1 ASC");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? ASC", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.orderBy(cb.asc(intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? ASC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.orderBy(cb2.asc(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? ASC", sql.remove(0));
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

    public void testAscending2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = ?1 ORDER BY ?2 ASC");
            query.setParameter(1, 36);
            query.setParameter(2, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = 36) ORDER BY 1 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? ASC", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = 36 ORDER BY 1 ASC");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = 36) ORDER BY 1 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? ASC", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = ?1 ORDER BY 1 ASC");
            query.setParameter(1, 36);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = 36) ORDER BY 1 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? ASC", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1), root.get(OLGH17837Entity_.intVal2));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), intParam1));
            cquery.orderBy(cb.desc(intParam2));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 36);
            query.setParameter(intParam2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = 36) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1), root2.get(OLGH17837Entity_.intVal2));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), cb2.literal(36)));
            cquery2.orderBy(cb2.desc(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = 36) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam4 = cb3.parameter(Integer.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1), root3.get(OLGH17837Entity_.intVal2));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.intVal1), intParam4));
            cquery3.orderBy(cb3.desc(cb3.literal(1)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 36);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = 36) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
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

    public void testAscending2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = ?1 ORDER BY ?2 ASC");
            query.setParameter(1, 36);
            query.setParameter(2, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? ASC", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = 36 ORDER BY 1 ASC");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 ASC", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? ASC", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = 36) ORDER BY 1 ASC", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? ASC", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = ?1 ORDER BY 1 ASC");
            query.setParameter(1, 36);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? ASC", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1), root.get(OLGH17837Entity_.intVal2));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), intParam1));
            cquery.orderBy(cb.desc(intParam2));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 36);
            query.setParameter(intParam2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1), root2.get(OLGH17837Entity_.intVal2));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), cb2.literal(36)));
            cquery2.orderBy(cb2.desc(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 DESC", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = 36) ORDER BY 1 DESC", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam4 = cb3.parameter(Integer.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1), root3.get(OLGH17837Entity_.intVal2));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.intVal1), intParam4));
            cquery3.orderBy(cb3.desc(cb3.literal(1)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 36);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
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

    public void testAscending2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = ?1 ORDER BY ?2 ASC");
            query.setParameter(1, 36);
            query.setParameter(2, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? ASC", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = 36 ORDER BY 1 ASC");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? ASC", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = ?1 ORDER BY 1 ASC");
            query.setParameter(1, 36);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? ASC", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1), root.get(OLGH17837Entity_.intVal2));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), intParam1));
            cquery.orderBy(cb.desc(intParam2));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 36);
            query.setParameter(intParam2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1), root2.get(OLGH17837Entity_.intVal2));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), cb2.literal(36)));
            cquery2.orderBy(cb2.desc(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam4 = cb3.parameter(Integer.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1), root3.get(OLGH17837Entity_.intVal2));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.intVal1), intParam4));
            cquery3.orderBy(cb3.desc(cb3.literal(1)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 36);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
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

    public void testDescending1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s ORDER BY ?1 DESC");
            query.setParameter(1, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? DESC", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s ORDER BY 1 DESC");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? DESC", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.orderBy(cb.desc(intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? DESC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.orderBy(cb2.desc(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? DESC", sql.remove(0));
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

    public void testDescending1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s ORDER BY ?1 DESC");
            query.setParameter(1, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? DESC", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s ORDER BY 1 DESC");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? DESC", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.orderBy(cb.desc(intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? DESC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.orderBy(cb2.desc(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? DESC", sql.remove(0));
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

    public void testDescending1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s ORDER BY ?1 DESC");
            query.setParameter(1, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? DESC", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s ORDER BY 1 DESC");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? DESC", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.orderBy(cb.desc(intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? DESC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.orderBy(cb2.desc(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? DESC", sql.remove(0));
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

    public void testDescending2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = ?1 ORDER BY ?2 DESC");
            query.setParameter(1, 36);
            query.setParameter(2, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = 36) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = 36 ORDER BY 1 DESC");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = 36) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = ?1 ORDER BY 1 DESC");
            query.setParameter(1, 36);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = 36) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1), root.get(OLGH17837Entity_.intVal2));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), intParam1));
            cquery.orderBy(cb.desc(intParam2));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 36);
            query.setParameter(intParam2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = 36) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1), root2.get(OLGH17837Entity_.intVal2));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), cb2.literal(36)));
            cquery2.orderBy(cb2.desc(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = 36) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam4 = cb3.parameter(Integer.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1), root3.get(OLGH17837Entity_.intVal2));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.intVal1), intParam4));
            cquery3.orderBy(cb3.desc(cb3.literal(1)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 36);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = 36) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
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

    public void testDescending2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = ?1 ORDER BY ?2 DESC");
            query.setParameter(1, 36);
            query.setParameter(2, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = 36 ORDER BY 1 DESC");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 DESC", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = 36) ORDER BY 1 DESC", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = ?1 ORDER BY 1 DESC");
            query.setParameter(1, 36);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1), root.get(OLGH17837Entity_.intVal2));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), intParam1));
            cquery.orderBy(cb.desc(intParam2));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 36);
            query.setParameter(intParam2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1), root2.get(OLGH17837Entity_.intVal2));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), cb2.literal(36)));
            cquery2.orderBy(cb2.desc(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 DESC", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = 36) ORDER BY 1 DESC", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam4 = cb3.parameter(Integer.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1), root3.get(OLGH17837Entity_.intVal2));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.intVal1), intParam4));
            cquery3.orderBy(cb3.desc(cb3.literal(1)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 36);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
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

    public void testDescending2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = ?1 ORDER BY ?2 DESC");
            query.setParameter(1, 36);
            query.setParameter(2, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = 36 ORDER BY 1 DESC");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = ?1 ORDER BY 1 DESC");
            query.setParameter(1, 36);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1), root.get(OLGH17837Entity_.intVal2));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), intParam1));
            cquery.orderBy(cb.desc(intParam2));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 36);
            query.setParameter(intParam2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1), root2.get(OLGH17837Entity_.intVal2));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), cb2.literal(36)));
            cquery2.orderBy(cb2.desc(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam4 = cb3.parameter(Integer.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1), root3.get(OLGH17837Entity_.intVal2));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.intVal1), intParam4));
            cquery3.orderBy(cb3.desc(cb3.literal(1)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 36);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY ? DESC", sql.remove(0));
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

    public void testOrderBy1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s ORDER BY ?1");
            query.setParameter(1, 1);
            Exception exception = null;
            List<String> sql = null;
            try {
                query.getResultList();
            } catch (Exception e) {
                exception = e;
            } finally {
                sql = SQLCallListener.getAndClearCallList();
                Assert.assertEquals(1, sql.size());
                if (isUsingJPA22Feature() || isUsingJPA30Feature() || isUsingJPA31Feature() || isUsingJPA32Feature()) {
                    if (isDB2Z || isDB2 || isDerby) {
                        Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1", sql.remove(0));
                    } else {
                        Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ?", sql.remove(0));
                    }
                } else {
                    if (isDB2Z || isDB2 || isDerby) {
                        Assert.assertNotNull("Expected query '" + sql.remove(0) + "' to fail", exception);
                    } else {
                        Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ?", sql.remove(0));
                    }
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s ORDER BY 1");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ?", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.orderBy(cb.desc(intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? DESC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.orderBy(cb2.desc(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? DESC", sql.remove(0));
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

    public void testOrderBy1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s ORDER BY ?1");
            query.setParameter(1, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ?", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s ORDER BY 1");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ?", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.orderBy(cb.desc(intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? DESC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.orderBy(cb2.desc(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? DESC", sql.remove(0));
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

    public void testOrderBy1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s ORDER BY ?1");
            query.setParameter(1, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ?", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s ORDER BY 1");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ?", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.orderBy(cb.desc(intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? DESC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.orderBy(cb2.desc(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY 1 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY ORDER BY ? DESC", sql.remove(0));
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

    public void testOrderBy2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s ORDER BY ?1, ?2");
            query.setParameter(1, 1);
            query.setParameter(2, 2);
            Exception exception = null;
            List<String> sql = null;
            try {
                query.getResultList();
            } catch (Exception e) {
                exception = e;
            } finally {
                sql = SQLCallListener.getAndClearCallList();
                Assert.assertEquals(1, sql.size());
                if (isUsingJPA22Feature() || isUsingJPA30Feature() || isUsingJPA31Feature() || isUsingJPA32Feature()) {
                    if (isDB2Z || isDB2 || isDerby) {
                        Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY 1, 2", sql.remove(0));
                    } else {
                        Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY ?, ?", sql.remove(0));
                    }
                } else {
                    if (isDB2Z || isDB2 || isDerby) {
                        Assert.assertNotNull("Expected query '" + sql.remove(0) + "' to fail", exception);
                    } else {
                        Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY ?, ?", sql.remove(0));
                    }
                }
            }

            query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s ORDER BY 1, 2");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY 1, 2", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY ?, ?", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s ORDER BY 1, ?1");
            query.setParameter(1, 2);
            exception = null;
            try {
                query.getResultList();
            } catch (Exception e) {
                exception = e;
            } finally {
                sql = SQLCallListener.getAndClearCallList();
                Assert.assertEquals(1, sql.size());
                if (isUsingJPA22Feature() || isUsingJPA30Feature() || isUsingJPA31Feature() || isUsingJPA32Feature()) {
                    if (isDB2Z || isDB2 || isDerby) {
                        Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY 1, 2", sql.remove(0));
                    } else {
                        Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY ?, ?", sql.remove(0));
                    }
                } else {
                    if (isDB2Z || isDB2 || isDerby) {
                        Assert.assertNotNull("Expected query '" + sql.remove(0) + "' to fail", exception);
                    } else {
                        Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY ?, ?", sql.remove(0));
                    }
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1), root.get(OLGH17837Entity_.intVal2));
            cquery.orderBy(cb.desc(intParam1), cb.asc(intParam2));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY 1 DESC, 2 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY ? DESC, ? ASC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1), root2.get(OLGH17837Entity_.intVal2));
            cquery2.orderBy(cb2.desc(cb2.literal(1)), cb2.asc(cb2.literal(2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY 1 DESC, 2 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY ? DESC, ? ASC", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1), root3.get(OLGH17837Entity_.intVal2));
            cquery3.orderBy(cb3.desc(cb3.literal(1)), cb3.asc(intParam3));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY 1 DESC, 2 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY ? DESC, ? ASC", sql.remove(0));
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

    public void testOrderBy2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s ORDER BY ?1, ?2");
            query.setParameter(1, 1);
            query.setParameter(2, 2);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY 1, 2", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY ?, ?", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s ORDER BY 1, 2");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY 1, 2", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY ?, ?", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s ORDER BY 1, ?1");
            query.setParameter(1, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY 1, 2", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY ?, ?", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1), root.get(OLGH17837Entity_.intVal2));
            cquery.orderBy(cb.desc(intParam1), cb.asc(intParam2));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY 1 DESC, 2 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY ? DESC, ? ASC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1), root2.get(OLGH17837Entity_.intVal2));
            cquery2.orderBy(cb2.desc(cb2.literal(1)), cb2.asc(cb2.literal(2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY 1 DESC, 2 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY ? DESC, ? ASC", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1), root3.get(OLGH17837Entity_.intVal2));
            cquery3.orderBy(cb3.desc(cb3.literal(1)), cb3.asc(intParam3));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY 1 DESC, 2 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY ? DESC, ? ASC", sql.remove(0));
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

    public void testOrderBy2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s ORDER BY ?1, ?2");
            query.setParameter(1, 1);
            query.setParameter(2, 2);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY 1, 2", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY ?, ?", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s ORDER BY 1, 2");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY 1, 2", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY ?, ?", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s ORDER BY 1, ?1");
            query.setParameter(1, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY 1, 2", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY ?, ?", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1), root.get(OLGH17837Entity_.intVal2));
            cquery.orderBy(cb.desc(intParam1), cb.asc(intParam2));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY 1 DESC, 2 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY ? DESC, ? ASC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1), root2.get(OLGH17837Entity_.intVal2));
            cquery2.orderBy(cb2.desc(cb2.literal(1)), cb2.asc(cb2.literal(2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY 1 DESC, 2 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY ? DESC, ? ASC", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1), root3.get(OLGH17837Entity_.intVal2));
            cquery3.orderBy(cb3.desc(cb3.literal(1)), cb3.asc(intParam3));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY 1 DESC, 2 ASC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY ORDER BY ? DESC, ? ASC", sql.remove(0));
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

    public void testOrderBy3_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = ?1 ORDER BY s.intVal2");
            query.setParameter(1, 5);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = 5 ORDER BY s.intVal2");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = 5) ORDER BY INTVAL2", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1), root.get(OLGH17837Entity_.intVal2));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), intParam1));
            cquery.orderBy(cb.desc(root.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2 DESC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1), root2.get(OLGH17837Entity_.intVal2));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), 5));
            cquery2.orderBy(cb2.desc(root2.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2 DESC", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = 5) ORDER BY INTVAL2 DESC", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2 DESC", sql.remove(0));
                }
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

    public void testOrderBy3_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = ?1 ORDER BY s.intVal2");
            query.setParameter(1, 5);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = 5 ORDER BY s.intVal2");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = 5) ORDER BY INTVAL2", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1), root.get(OLGH17837Entity_.intVal2));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), intParam1));
            cquery.orderBy(cb.desc(root.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2 DESC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1), root2.get(OLGH17837Entity_.intVal2));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), 5));
            cquery2.orderBy(cb2.desc(root2.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2 DESC", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = 5) ORDER BY INTVAL2 DESC", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2 DESC", sql.remove(0));
                }
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

    public void testOrderBy3_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = ?1 ORDER BY s.intVal2");
            query.setParameter(1, 5);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, s.intVal2 FROM OLGH17837Entity s WHERE s.intVal1 = 5 ORDER BY s.intVal2");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1), root.get(OLGH17837Entity_.intVal2));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), intParam1));
            cquery.orderBy(cb.desc(root.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2 DESC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1), root2.get(OLGH17837Entity_.intVal2));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), 5));
            cquery2.orderBy(cb2.desc(root2.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2 DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, INTVAL2 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?) ORDER BY INTVAL2 DESC", sql.remove(0));
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
