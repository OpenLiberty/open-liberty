/*******************************************************************************
 * Copyright (c) 2022.2024 IBM Corporation and others.
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
import javax.persistence.criteria.CriteriaBuilder.In;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh17837.model.OLGH17837Entity;
import com.ibm.ws.jpa.olgh17837.model.OLGH17837Entity_;
import com.ibm.ws.jpa.query.sqlcapture.SQLCallListener;
import com.ibm.ws.testtooling.database.DatabaseVendor;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

@SuppressWarnings("rawtypes")
public class TestComparisonLogic extends AbstractTestLogic {

    public void testEquals1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            System.out.println("Testing JPQL with parameters...");
            em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = (?1)").setParameter(1, 4).getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.get(0));

            System.out.println("Testing JPQL with literals...");
            em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = (4)").getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.get(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = 4)", sql.get(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.get(0));
                }
            }

            // -----------------------

            System.out.println("Testing CriteriaBuilder with parameters...");
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<Long> longValue = cb.parameter(Long.class);
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), longValue));

            em.createQuery(cquery).setParameter(longValue, (long) 4).getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.get(0));

            System.out.println("Testing CriteriaBuilder with literals...");
            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), 4));

            em.createQuery(cquery2).getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.get(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = 4)", sql.get(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.get(0));
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

    public void testEquals1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            System.out.println("Testing JPQL with parameters...");
            em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = (?1)").setParameter(1, 4).getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.get(0));

            System.out.println("Testing JPQL with literals...");
            em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = (4)").getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.get(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = 4)", sql.get(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.get(0));
                }
            }

            // -----------------------

            System.out.println("Testing CriteriaBuilder with parameters...");
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<Long> longValue = cb.parameter(Long.class);
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), longValue));

            em.createQuery(cquery).setParameter(longValue, (long) 4).getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.get(0));

            System.out.println("Testing CriteriaBuilder with literals...");
            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), 4));

            em.createQuery(cquery2).getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.get(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = 4)", sql.get(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.get(0));
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

    public void testEquals1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            System.out.println("Testing JPQL with parameters...");
            em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = (?1)").setParameter(1, 4).getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.get(0));

            System.out.println("Testing JPQL with literals...");
            em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = (4)").getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.get(0));

            // -----------------------

            System.out.println("Testing CriteriaBuilder with parameters...");
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<Long> longValue = cb.parameter(Long.class);
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), longValue));

            em.createQuery(cquery).setParameter(longValue, (long) 4).getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.get(0));

            System.out.println("Testing CriteriaBuilder with literals...");
            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), 4));

            em.createQuery(cquery2).getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.get(0));
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

    public void testEquals2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1, ?1 + ?2 FROM OLGH17837Entity s WHERE ?1 = (s.intVal1 + ABS(?2))");
            query.setParameter(1, 4);
            query.setParameter(2, -2);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (4 + -2) FROM OLGH17837ENTITY WHERE (4 = (INTVAL1 + ABS(-2)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, 4 + -2 FROM OLGH17837Entity s WHERE 4 = (s.intVal1 + ABS(-2))");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (4 + -2) FROM OLGH17837ENTITY WHERE (4 = (INTVAL1 + ABS(-2)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, 4 + ?2 FROM OLGH17837Entity s WHERE 4 = (s.intVal1 + ABS(?2))");
            query.setParameter(2, -2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (4 + -2) FROM OLGH17837ENTITY WHERE (4 = (INTVAL1 + ABS(-2)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1), cb.sum(intParam1, intParam2));
            cquery.where(cb.equal(intParam1, cb.sum(root.get(OLGH17837Entity_.intVal1), cb.abs(intParam2))));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 4);
            query.setParameter(intParam2, -2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (4 + -2) FROM OLGH17837ENTITY WHERE (4 = (INTVAL1 + ABS(-2)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1), cb2.sum(4, cb2.literal(-2)));
            cquery2.where(cb2.equal(cb2.literal(4), cb2.sum(root2.get(OLGH17837Entity_.intVal1), cb2.abs(cb2.literal(-2)))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (4 + -2) FROM OLGH17837ENTITY WHERE (4 = (INTVAL1 + ABS(-2)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam6 = cb3.parameter(Integer.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1), cb3.sum(4, intParam6));
            cquery3.where(cb3.equal(cb3.literal(4), cb3.sum(root3.get(OLGH17837Entity_.intVal1), cb3.abs(intParam6))));

            query = em.createQuery(cquery3);
            query.setParameter(intParam6, -2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (4 + -2) FROM OLGH17837ENTITY WHERE (4 = (INTVAL1 + ABS(-2)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
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

    public void testEquals2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1, ?1 + ?2 FROM OLGH17837Entity s WHERE ?1 = (s.intVal1 + ABS(?2))");
            query.setParameter(1, 4);
            query.setParameter(2, -2);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(-2)))", sql.remove(0));
            } else if (isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (? + -2) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, 4 + -2 FROM OLGH17837Entity s WHERE 4 = (s.intVal1 + ABS(-2))");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(-2)))", sql.remove(0));
                } else if (isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, (? + -2) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, (4 + -2) FROM OLGH17837ENTITY WHERE (4 = (INTVAL1 + ABS(-2)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1, 4 + ?2 FROM OLGH17837Entity s WHERE 4 = (s.intVal1 + ABS(?2))");
            query.setParameter(2, -2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(-2)))", sql.remove(0));
                } else if (isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, (? + -2) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
                }
            } else {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1, (4 + ?) FROM OLGH17837ENTITY WHERE (4 = (INTVAL1 + ABS(-2)))", sql.remove(0));
                } else if (isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, (4 + ?) FROM OLGH17837ENTITY WHERE (4 = (INTVAL1 + ABS(?)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1), cb.sum(intParam1, intParam2));
            cquery.where(cb.equal(intParam1, cb.sum(root.get(OLGH17837Entity_.intVal1), cb.abs(intParam2))));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 4);
            query.setParameter(intParam2, -2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(-2)))", sql.remove(0));
            } else if (isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (? + -2) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1), cb2.sum(4, cb2.literal(-2)));
            cquery2.where(cb2.equal(cb2.literal(4), cb2.sum(root2.get(OLGH17837Entity_.intVal1), cb2.abs(cb2.literal(-2)))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(-2)))", sql.remove(0));
                } else if (isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, (? + -2) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, (4 + -2) FROM OLGH17837ENTITY WHERE (4 = (INTVAL1 + ABS(-2)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam6 = cb3.parameter(Integer.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1), cb3.sum(4, intParam6));
            cquery3.where(cb3.equal(cb3.literal(4), cb3.sum(root3.get(OLGH17837Entity_.intVal1), cb3.abs(intParam6))));

            query = em.createQuery(cquery3);
            query.setParameter(intParam6, -2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(-2)))", sql.remove(0));
                } else if (isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, (? + -2) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
                }
            } else {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1, (4 + ?) FROM OLGH17837ENTITY WHERE (4 = (INTVAL1 + ABS(-2)))", sql.remove(0));
                } else if (isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, (4 + ?) FROM OLGH17837ENTITY WHERE (4 = (INTVAL1 + ABS(?)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
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

    public void testEquals2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1, ?1 + ?2 FROM OLGH17837Entity s WHERE ?1 = (s.intVal1 + ABS(?2))");
            query.setParameter(1, 4);
            query.setParameter(2, -2);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(-2)))", sql.remove(0));
            } else if (isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (? + -2) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, 4 + -2 FROM OLGH17837Entity s WHERE 4 = (s.intVal1 + ABS(-2))");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(-2)))", sql.remove(0));
            } else if (isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (? + -2) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, 4 + ?2 FROM OLGH17837Entity s WHERE 4 = (s.intVal1 + ABS(?2))");
            query.setParameter(2, -2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(-2)))", sql.remove(0));
            } else if (isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (? + -2) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1), cb.sum(intParam1, intParam2));
            cquery.where(cb.equal(intParam1, cb.sum(root.get(OLGH17837Entity_.intVal1), cb.abs(intParam2))));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 4);
            query.setParameter(intParam2, -2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(-2)))", sql.remove(0));
            } else if (isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (? + -2) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1), cb2.sum(4, cb2.literal(-2)));
            cquery2.where(cb2.equal(cb2.literal(4), cb2.sum(root2.get(OLGH17837Entity_.intVal1), cb2.abs(cb2.literal(-2)))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(-2)))", sql.remove(0));
            } else if (isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (? + -2) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam6 = cb3.parameter(Integer.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1), cb3.sum(4, intParam6));
            cquery3.where(cb3.equal(cb3.literal(4), cb3.sum(root3.get(OLGH17837Entity_.intVal1), cb3.abs(intParam6))));

            query = em.createQuery(cquery3);
            query.setParameter(intParam6, -2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(-2)))", sql.remove(0));
            } else if (isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (? + -2) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (? = (INTVAL1 + ABS(?)))", sql.remove(0));
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

    public void testEquals3_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 = (?1)");
            query.setParameter(1, 4);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 = ?)", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 = (4)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 = ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 = 4)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 = ?)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<Long> longValue = cb.parameter(Long.class);
            cquery.groupBy(root.get(OLGH17837Entity_.intVal1));
            cquery.having(cb.equal(root.get(OLGH17837Entity_.intVal1), longValue));

            query = em.createQuery(cquery);
            query.setParameter(longValue, (long) 4);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 = ?)", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.groupBy(root2.get(OLGH17837Entity_.intVal1));
            cquery2.having(cb2.equal(root2.get(OLGH17837Entity_.intVal1), 4));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 = ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 = 4)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 = ?)", sql.remove(0));
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

    public void testEquals3_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 = (?1)");
            query.setParameter(1, 4);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 = ?)", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 = (4)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 = ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 = 4)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 = ?)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<Long> longValue = cb.parameter(Long.class);
            cquery.groupBy(root.get(OLGH17837Entity_.intVal1));
            cquery.having(cb.equal(root.get(OLGH17837Entity_.intVal1), longValue));

            query = em.createQuery(cquery);
            query.setParameter(longValue, (long) 4);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 = ?)", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.groupBy(root2.get(OLGH17837Entity_.intVal1));
            cquery2.having(cb2.equal(root2.get(OLGH17837Entity_.intVal1), 4));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 = ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 = 4)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 = ?)", sql.remove(0));
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

    public void testEquals3_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 = (?1)");
            query.setParameter(1, 4);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 = ?)", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 = (4)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 = ?)", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<Long> longValue = cb.parameter(Long.class);
            cquery.groupBy(root.get(OLGH17837Entity_.intVal1));
            cquery.having(cb.equal(root.get(OLGH17837Entity_.intVal1), longValue));

            query = em.createQuery(cquery);
            query.setParameter(longValue, (long) 4);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 = ?)", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.groupBy(root2.get(OLGH17837Entity_.intVal1));
            cquery2.having(cb2.equal(root2.get(OLGH17837Entity_.intVal1), 4));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 = ?)", sql.remove(0));
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

    public void testLessThan1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE (?1 + ?2) < (s.intVal1)");
            query.setParameter(1, 5);
            query.setParameter(2, 10);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((5 + 10) < INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + ?) < INTVAL1)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE (5 + 10) < (s.intVal1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((5 + 10) < INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + ?) < INTVAL1)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE (5 + ?2) < (s.intVal1)");
            query.setParameter(2, 10);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((5 + 10) < INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + ?) < INTVAL1)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.where(cb.lessThan(cb.sum(intParam1, intParam2), root.get(OLGH17837Entity_.intVal1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 5);
            query.setParameter(intParam2, 10);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((5 + 10) < INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + ?) < INTVAL1)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.lessThan(cb2.sum(cb2.literal(5), 10), root2.get(OLGH17837Entity_.intVal1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((5 + 10) < INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + ?) < INTVAL1)", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal1));
            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            cquery3.where(cb3.lessThan(cb3.sum(5, intParam3), root3.get(OLGH17837Entity_.intVal1)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 10);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((5 + 10) < INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + ?) < INTVAL1)", sql.remove(0));
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

    public void testLessThan1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE (?1 + ?2) < (s.intVal1)");
            query.setParameter(1, 5);
            query.setParameter(2, 10);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + 10) < INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + ?) < INTVAL1)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE (5 + 10) < (s.intVal1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + 10) < INTVAL1)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + ?) < INTVAL1)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((5 + 10) < INTVAL1)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + ?) < INTVAL1)", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE (5 + ?2) < (s.intVal1)");
            query.setParameter(2, 10);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + 10) < INTVAL1)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + ?) < INTVAL1)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((5 + ?) < INTVAL1)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + ?) < INTVAL1)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.where(cb.lessThan(cb.sum(intParam1, intParam2), root.get(OLGH17837Entity_.intVal1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 5);
            query.setParameter(intParam2, 10);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + 10) < INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + ?) < INTVAL1)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.lessThan(cb2.sum(cb2.literal(5), 10), root2.get(OLGH17837Entity_.intVal1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + 10) < INTVAL1)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + ?) < INTVAL1)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((5 + 10) < INTVAL1)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + ?) < INTVAL1)", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal1));
            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            cquery3.where(cb3.lessThan(cb3.sum(5, intParam3), root3.get(OLGH17837Entity_.intVal1)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 10);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + 10) < INTVAL1)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + ?) < INTVAL1)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((5 + ?) < INTVAL1)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + ?) < INTVAL1)", sql.remove(0));
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

    public void testLessThan1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DERBY);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE (?1 + ?2) < (s.intVal1)");
            query.setParameter(1, 5);
            query.setParameter(2, 10);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + 10) < INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + ?) < INTVAL1)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE (5 + 10) < (s.intVal1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + 10) < INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + ?) < INTVAL1)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE (5 + ?2) < (s.intVal1)");
            query.setParameter(2, 10);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + 10) < INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + ?) < INTVAL1)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.where(cb.lessThan(cb.sum(intParam1, intParam2), root.get(OLGH17837Entity_.intVal1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 5);
            query.setParameter(intParam2, 10);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + 10) < INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + ?) < INTVAL1)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.lessThan(cb2.sum(cb2.literal(5), 10), root2.get(OLGH17837Entity_.intVal1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + 10) < INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + ?) < INTVAL1)", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal1));
            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            cquery3.where(cb3.lessThan(cb3.sum(5, intParam3), root3.get(OLGH17837Entity_.intVal1)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 10);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + 10) < INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE ((? + ?) < INTVAL1)", sql.remove(0));
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

    public void testLike1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.strVal1 LIKE ?1");
            query.setParameter(1, "%ORL%");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ?", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.strVal1 LIKE '%ORL%'");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ?", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE '%ORL%'", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ?", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.where(cb.like(root.get(OLGH17837Entity_.strVal1), strParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "%ORL%");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ?", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.like(root2.get(OLGH17837Entity_.strVal1), "%ORL%"));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ?", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE '%ORL%'", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ?", sql.remove(0));
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

    public void testLike1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.strVal1 LIKE ?1");
            query.setParameter(1, "%ORL%");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ?", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.strVal1 LIKE '%ORL%'");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ?", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE '%ORL%'", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ?", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.where(cb.like(root.get(OLGH17837Entity_.strVal1), strParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "%ORL%");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ?", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.like(root2.get(OLGH17837Entity_.strVal1), "%ORL%"));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ?", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE '%ORL%'", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ?", sql.remove(0));
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

    public void testLike1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.strVal1 LIKE ?1");
            query.setParameter(1, "%ORL%");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ?", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.strVal1 LIKE '%ORL%'");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ?", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.where(cb.like(root.get(OLGH17837Entity_.strVal1), strParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "%ORL%");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ?", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.like(root2.get(OLGH17837Entity_.strVal1), "%ORL%"));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ?", sql.remove(0));
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

    public void testLike2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 LIKE ?2");
            query.setParameter(1, "WORLD");
            query.setParameter(2, "%ORL%");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE 'WORLD' LIKE '%ORL%'", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ?", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 'WORLD' LIKE '%ORL%'");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE 'WORLD' LIKE '%ORL%'", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ?", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 'WORLD' LIKE ?1");
            query.setParameter(1, "%ORL%");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE 'WORLD' LIKE '%ORL%'", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ?", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            cquery.where(cb.like(strParam1, strParam2));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "WORLD");
            query.setParameter(strParam2, "%ORL%");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE 'WORLD' LIKE '%ORL%'", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ?", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.like(cb2.literal("WORLD"), cb2.literal("%ORL%")));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE 'WORLD' LIKE '%ORL%'", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ?", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam3 = cb3.parameter(String.class);
            cquery3.where(cb3.like(cb3.literal("WORLD"), strParam3));

            query = em.createQuery(cquery3);
            query.setParameter(strParam3, "%ORL%");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE 'WORLD' LIKE '%ORL%'", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ?", sql.remove(0));
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

    public void testLike2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 LIKE ?2");
            query.setParameter(1, "WORLD");
            query.setParameter(2, "%ORL%");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ?", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 'WORLD' LIKE '%ORL%'");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ?", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE 'WORLD' LIKE '%ORL%'", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ?", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 'WORLD' LIKE ?1");
            query.setParameter(1, "%ORL%");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ?", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE 'WORLD' LIKE ?", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ?", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            cquery.where(cb.like(strParam1, strParam2));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "WORLD");
            query.setParameter(strParam2, "%ORL%");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ?", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.like(cb2.literal("WORLD"), cb2.literal("%ORL%")));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ?", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE 'WORLD' LIKE '%ORL%'", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ?", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam3 = cb3.parameter(String.class);
            cquery3.where(cb3.like(cb3.literal("WORLD"), strParam3));

            query = em.createQuery(cquery3);
            query.setParameter(strParam3, "%ORL%");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ?", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE 'WORLD' LIKE ?", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ?", sql.remove(0));
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

    public void testLike2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 LIKE ?2");
            query.setParameter(1, "WORLD");
            query.setParameter(2, "%ORL%");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ?", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 'WORLD' LIKE '%ORL%'");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ?", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 'WORLD' LIKE ?1");
            query.setParameter(1, "%ORL%");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ?", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            cquery.where(cb.like(strParam1, strParam2));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "WORLD");
            query.setParameter(strParam2, "%ORL%");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ?", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.like(cb2.literal("WORLD"), cb2.literal("%ORL%")));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ?", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));
            ParameterExpression<String> strParam3 = cb3.parameter(String.class);
            cquery3.where(cb3.like(cb3.literal("WORLD"), strParam3));

            query = em.createQuery(cquery3);
            query.setParameter(strParam3, "%ORL%");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ?", sql.remove(0));
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

    public void testNotLike1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.strVal1 NOT LIKE ?1");
            query.setParameter(1, "%ORL%");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT (STRVAL1 LIKE ?)", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.strVal1 NOT LIKE '%ORL%'");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT (STRVAL1 LIKE ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT (STRVAL1 LIKE '%ORL%')", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT (STRVAL1 LIKE ?)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.where(cb.notLike(root.get(OLGH17837Entity_.strVal1), strParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "%ORL%");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 NOT LIKE ?", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.notLike(root2.get(OLGH17837Entity_.strVal1), "%ORL%"));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 NOT LIKE ?", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 NOT LIKE '%ORL%'", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 NOT LIKE ?", sql.remove(0));
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

    public void testNotLike1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.strVal1 NOT LIKE ?1");
            query.setParameter(1, "%ORL%");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT (STRVAL1 LIKE ?)", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.strVal1 NOT LIKE '%ORL%'");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT (STRVAL1 LIKE ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT (STRVAL1 LIKE '%ORL%')", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT (STRVAL1 LIKE ?)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.where(cb.notLike(root.get(OLGH17837Entity_.strVal1), strParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "%ORL%");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 NOT LIKE ?", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.notLike(root2.get(OLGH17837Entity_.strVal1), "%ORL%"));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 NOT LIKE ?", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 NOT LIKE '%ORL%'", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 NOT LIKE ?", sql.remove(0));
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

    public void testNotLike1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.strVal1 NOT LIKE ?1");
            query.setParameter(1, "%ORL%");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT (STRVAL1 LIKE ?)", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.strVal1 NOT LIKE '%ORL%'");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT (STRVAL1 LIKE ?)", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.where(cb.notLike(root.get(OLGH17837Entity_.strVal1), strParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "%ORL%");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 NOT LIKE ?", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.notLike(root2.get(OLGH17837Entity_.strVal1), "%ORL%"));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 NOT LIKE ?", sql.remove(0));
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

    public void testLikeEscape1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.strVal1 LIKE ?1 ESCAPE ?2");
            query.setParameter(1, "HELLO");
            query.setParameter(2, 'R');
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE 'HELLO' ESCAPE 'R'", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ? ESCAPE ?", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.strVal1 LIKE 'HELLO' ESCAPE 'R'");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE 'HELLO' ESCAPE 'R'", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ? ESCAPE ?", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<Character> chaParam1 = cb.parameter(Character.class);
            cquery.where(cb.like(root.get(OLGH17837Entity_.strVal1), strParam1, chaParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.setParameter(chaParam1, 'R');
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE 'HELLO' ESCAPE 'R'", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ? ESCAPE ?", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.like(root2.get(OLGH17837Entity_.strVal1), "HELLO", 'R'));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE 'HELLO' ESCAPE 'R'", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ? ESCAPE ?", sql.remove(0));
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

    public void testLikeEscape1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.strVal1 LIKE ?1 ESCAPE ?2");
            query.setParameter(1, "HELLO");
            query.setParameter(2, 'R');
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ? ESCAPE ?", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ? ESCAPE ?", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.strVal1 LIKE 'HELLO' ESCAPE 'R'");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ? ESCAPE ?", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE 'HELLO' ESCAPE 'R'", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ? ESCAPE ?", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<Character> chaParam1 = cb.parameter(Character.class);
            cquery.where(cb.like(root.get(OLGH17837Entity_.strVal1), strParam1, chaParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.setParameter(chaParam1, 'R');
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ? ESCAPE ?", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ? ESCAPE ?", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.like(root2.get(OLGH17837Entity_.strVal1), "HELLO", 'R'));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ? ESCAPE ?", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE 'HELLO' ESCAPE 'R'", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ? ESCAPE ?", sql.remove(0));
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

    public void testLikeEscape1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.strVal1 LIKE ?1 ESCAPE ?2");
            query.setParameter(1, "HELLO");
            query.setParameter(2, 'R');
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ? ESCAPE ?", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ? ESCAPE ?", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.strVal1 LIKE 'HELLO' ESCAPE 'R'");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ? ESCAPE ?", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ? ESCAPE ?", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<Character> chaParam1 = cb.parameter(Character.class);
            cquery.where(cb.like(root.get(OLGH17837Entity_.strVal1), strParam1, chaParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.setParameter(chaParam1, 'R');
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ? ESCAPE ?", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ? ESCAPE ?", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.like(root2.get(OLGH17837Entity_.strVal1), "HELLO", 'R'));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ? ESCAPE ?", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE STRVAL1 LIKE ? ESCAPE ?", sql.remove(0));
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

    public void testLikeEscape2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 LIKE ?2 ESCAPE ?3");
            query.setParameter(1, "HELLO");
            query.setParameter(2, "WORLD");
            query.setParameter(3, 'A');
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE 'HELLO' LIKE 'WORLD' ESCAPE 'A'", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ? ESCAPE ?", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 'HELLO' LIKE 'WORLD' ESCAPE 'A'");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE 'HELLO' LIKE 'WORLD' ESCAPE 'A'", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ? ESCAPE ?", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 'HELLO' LIKE ?1 ESCAPE ?2");
            query.setParameter(1, "WORLD");
            query.setParameter(2, 'A');
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE 'HELLO' LIKE 'WORLD' ESCAPE 'A'", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ? ESCAPE ?", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            ParameterExpression<Character> chaParam1 = cb.parameter(Character.class);
            cquery.where(cb.like(strParam1, strParam2, chaParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.setParameter(strParam2, "WORLD");
            query.setParameter(chaParam1, 'A');
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE 'HELLO' LIKE 'WORLD' ESCAPE 'A'", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ? ESCAPE ?", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.like(cb2.literal("HELLO"), cb2.literal("WORLD"), cb2.literal('A')));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE 'HELLO' LIKE 'WORLD' ESCAPE 'A'", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ? ESCAPE ?", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam3 = cb3.parameter(String.class);
            ParameterExpression<Character> chaParam2 = cb.parameter(Character.class);
            cquery3.where(cb3.like(cb3.literal("HELLO"), strParam3, chaParam2));

            query = em.createQuery(cquery3);
            query.setParameter(strParam3, "WORLD");
            query.setParameter(chaParam2, 'A');
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE 'HELLO' LIKE 'WORLD' ESCAPE 'A'", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ? ESCAPE ?", sql.remove(0));
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

    public void testLikeEscape2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        /*
         * MIXED DB2 z/OS installations do not support parameters or literals in the ESCAPE clause with the format this test uses
         */
        if (isDB2Z) {
            return;
        }

// Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 LIKE ?2 ESCAPE ?3");
            query.setParameter(1, "HELLO");
            query.setParameter(2, "WORLD");
            query.setParameter(3, 'A');
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ? ESCAPE ?", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 'HELLO' LIKE 'WORLD' ESCAPE 'A'");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ? ESCAPE ?", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE 'HELLO' LIKE 'WORLD' ESCAPE 'A'", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ? ESCAPE ?", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 'HELLO' LIKE ?1 ESCAPE ?2");
            query.setParameter(1, "WORLD");
            query.setParameter(2, 'A');
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ? ESCAPE ?", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE 'HELLO' LIKE ? ESCAPE ?", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ? ESCAPE ?", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            ParameterExpression<Character> chaParam1 = cb.parameter(Character.class);
            cquery.where(cb.like(strParam1, strParam2, chaParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.setParameter(strParam2, "WORLD");
            query.setParameter(chaParam1, 'A');
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ? ESCAPE ?", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.like(cb2.literal("HELLO"), cb2.literal("WORLD"), cb2.literal('A')));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ? ESCAPE ?", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE 'HELLO' LIKE 'WORLD' ESCAPE 'A'", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ? ESCAPE ?", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam3 = cb3.parameter(String.class);
            ParameterExpression<Character> chaParam2 = cb.parameter(Character.class);
            cquery3.where(cb3.like(cb3.literal("HELLO"), strParam3, chaParam2));

            query = em.createQuery(cquery3);
            query.setParameter(strParam3, "WORLD");
            query.setParameter(chaParam2, 'A');
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ? ESCAPE ?", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE 'HELLO' LIKE ? ESCAPE ?", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ? ESCAPE ?", sql.remove(0));
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

    public void testLikeEscape2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        /*
         * MIXED DB2 z/OS installations do not support parameters or literals in the ESCAPE clause with the format this test uses
         */
        if (isDB2Z) {
            return;
        }

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 LIKE ?2 ESCAPE ?3");
            query.setParameter(1, "HELLO");
            query.setParameter(2, "WORLD");
            query.setParameter(3, 'A');
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ? ESCAPE ?", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 'HELLO' LIKE 'WORLD' ESCAPE 'A'");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ? ESCAPE ?", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 'HELLO' LIKE ?1 ESCAPE ?2");
            query.setParameter(1, "WORLD");
            query.setParameter(2, 'A');
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ? ESCAPE ?", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            ParameterExpression<Character> chaParam1 = cb.parameter(Character.class);
            cquery.where(cb.like(strParam1, strParam2, chaParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.setParameter(strParam2, "WORLD");
            query.setParameter(chaParam1, 'A');
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ? ESCAPE ?", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.like(cb2.literal("HELLO"), cb2.literal("WORLD"), cb2.literal('A')));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ? ESCAPE ?", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam3 = cb3.parameter(String.class);
            ParameterExpression<Character> chaParam2 = cb.parameter(Character.class);
            cquery3.where(cb3.like(cb3.literal("HELLO"), strParam3, chaParam2));

            query = em.createQuery(cquery3);
            query.setParameter(strParam3, "WORLD");
            query.setParameter(chaParam2, 'A');
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ? LIKE ? ESCAPE ?", sql.remove(0));
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

    public void testInCollection1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 IN ?1");
            query.setParameter(1, java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21));
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?,?,?,?,?,?,?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 IN (1, 2, 3, 5, 8, 13, 21)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (1, 2, 3, 5, 8, 13, 21))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 IN (1, ?1, 3, 5, 8, ?3, ?2)");
            query.setParameter(1, 2);
            query.setParameter(2, 21);
            query.setParameter(3, 13);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (1, ?, 3, 5, 8, ?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<List> colValue = cb.parameter(List.class);
            cquery.where(root.get(OLGH17837Entity_.intVal1).in(colValue));

            query = em.createQuery(cquery);
            List<Integer> intCollection2 = java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21);
            query.setParameter(colValue, intCollection2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?,?,?,?,?,?,?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            List<Integer> intCollection3 = java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21);
            cquery2.where(root2.get(OLGH17837Entity_.intVal1).in(intCollection3));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (1, 2, 3, 5, 8, 13, 21))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));
            In<Integer> in = cb3.in(root3.get(OLGH17837Entity_.intVal1)).value(1).value(3).value(5).value(8);
            ParameterExpression<List> colValue2 = cb3.parameter(List.class);
            cquery3.where(cb3.or(in, root3.get(OLGH17837Entity_.intVal1).in(colValue2)));

            query = em.createQuery(cquery3);
            query.setParameter(colValue2, java.util.Arrays.asList(2, 13, 21));
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ((INTVAL1 IN (?, ?, ?, ?)) OR (INTVAL1 IN (?,?,?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ((INTVAL1 IN (1, 3, 5, 8)) OR (INTVAL1 IN (?,?,?)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ((INTVAL1 IN (?, ?, ?, ?)) OR (INTVAL1 IN (?,?,?)))", sql.remove(0));
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

    public void testInCollection1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 IN ?1");
            query.setParameter(1, java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21));
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?,?,?,?,?,?,?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 IN (1, 2, 3, 5, 8, 13, 21)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (1, 2, 3, 5, 8, 13, 21))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 IN (1, ?1, 3, 5, 8, ?3, ?2)");
            query.setParameter(1, 2);
            query.setParameter(2, 21);
            query.setParameter(3, 13);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (1, ?, 3, 5, 8, ?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<List> colValue = cb.parameter(List.class);
            cquery.where(root.get(OLGH17837Entity_.intVal1).in(colValue));

            query = em.createQuery(cquery);
            List<Integer> intCollection2 = java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21);
            query.setParameter(colValue, intCollection2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?,?,?,?,?,?,?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            List<Integer> intCollection3 = java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21);
            cquery2.where(root2.get(OLGH17837Entity_.intVal1).in(intCollection3));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (1, 2, 3, 5, 8, 13, 21))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));
            In<Integer> in = cb3.in(root3.get(OLGH17837Entity_.intVal1)).value(1).value(3).value(5).value(8);
            ParameterExpression<List> colValue2 = cb3.parameter(List.class);
            cquery3.where(cb3.or(in, root3.get(OLGH17837Entity_.intVal1).in(colValue2)));

            query = em.createQuery(cquery3);
            query.setParameter(colValue2, java.util.Arrays.asList(2, 13, 21));
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ((INTVAL1 IN (?, ?, ?, ?)) OR (INTVAL1 IN (?,?,?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ((INTVAL1 IN (1, 3, 5, 8)) OR (INTVAL1 IN (?,?,?)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ((INTVAL1 IN (?, ?, ?, ?)) OR (INTVAL1 IN (?,?,?)))", sql.remove(0));
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

    public void testInCollection1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 IN ?1");
            query.setParameter(1, java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21));
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?,?,?,?,?,?,?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 IN (1, 2, 3, 5, 8, 13, 21)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 IN (1, ?1, 3, 5, 8, ?3, ?2)");
            query.setParameter(1, 2);
            query.setParameter(2, 21);
            query.setParameter(3, 13);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<List> colValue = cb.parameter(List.class);
            cquery.where(root.get(OLGH17837Entity_.intVal1).in(colValue));

            query = em.createQuery(cquery);
            List<Integer> intCollection2 = java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21);
            query.setParameter(colValue, intCollection2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?,?,?,?,?,?,?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            List<Integer> intCollection3 = java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21);
            cquery2.where(root2.get(OLGH17837Entity_.intVal1).in(intCollection3));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));
            In<Integer> in = cb3.in(root3.get(OLGH17837Entity_.intVal1)).value(1).value(3).value(5).value(8);
            ParameterExpression<List> colValue2 = cb3.parameter(List.class);
            cquery3.where(cb3.or(in, root3.get(OLGH17837Entity_.intVal1).in(colValue2)));

            query = em.createQuery(cquery3);
            query.setParameter(colValue2, java.util.Arrays.asList(2, 13, 21));
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ((INTVAL1 IN (?, ?, ?, ?)) OR (INTVAL1 IN (?,?,?)))", sql.remove(0));
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

    public void testInCollection2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 IN (?1, ?2, ?3, ?4, ?5)");
            query.setParameter(1, 1);
            query.setParameter(2, 2);
            query.setParameter(3, 3);
            query.setParameter(4, 5);
            query.setParameter(5, 8);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 IN (1, 2, 3, 5, 8)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (1, 2, 3, 5, 8))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 IN (1, ?1, 3, 5, ?3)");
            query.setParameter(1, 2);
            query.setParameter(3, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (1, ?, 3, 5, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue3 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue4 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue5 = cb.parameter(Integer.class);
            cquery.where(root.get(OLGH17837Entity_.intVal1).in(intValue1, intValue2, intValue3, intValue4, intValue5));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 1);
            query.setParameter(intValue2, 2);
            query.setParameter(intValue3, 3);
            query.setParameter(intValue4, 5);
            query.setParameter(intValue5, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(root2.get(OLGH17837Entity_.intVal1).in(1, 2, 3, 5, 8));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (1, 2, 3, 5, 8))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue6 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intValue7 = cb3.parameter(Integer.class);

            In<Integer> in = cb3.in(root3.get(OLGH17837Entity_.intVal1));
            in = in.value(1);
            in = in.value(intValue6);
            in = in.value(3).value(5);
            in = in.value(intValue7);
            cquery3.where(in);

            query = em.createQuery(cquery3);
            query.setParameter(intValue6, 2);
            query.setParameter(intValue7, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (1, ?, 3, 5, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
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

    public void testInCollection2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 IN (?1, ?2, ?3, ?4, ?5)");
            query.setParameter(1, 1);
            query.setParameter(2, 2);
            query.setParameter(3, 3);
            query.setParameter(4, 5);
            query.setParameter(5, 8);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 IN (1, 2, 3, 5, 8)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (1, 2, 3, 5, 8))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 IN (1, ?1, 3, 5, ?3)");
            query.setParameter(1, 2);
            query.setParameter(3, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (1, ?, 3, 5, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue3 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue4 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue5 = cb.parameter(Integer.class);
            cquery.where(root.get(OLGH17837Entity_.intVal1).in(intValue1, intValue2, intValue3, intValue4, intValue5));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 1);
            query.setParameter(intValue2, 2);
            query.setParameter(intValue3, 3);
            query.setParameter(intValue4, 5);
            query.setParameter(intValue5, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(root2.get(OLGH17837Entity_.intVal1).in(1, 2, 3, 5, 8));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (1, 2, 3, 5, 8))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue6 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intValue7 = cb3.parameter(Integer.class);

            In<Integer> in = cb3.in(root3.get(OLGH17837Entity_.intVal1));
            in = in.value(1);
            in = in.value(intValue6);
            in = in.value(3).value(5);
            in = in.value(intValue7);
            cquery3.where(in);

            query = em.createQuery(cquery3);
            query.setParameter(intValue6, 2);
            query.setParameter(intValue7, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (1, ?, 3, 5, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
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

    public void testInCollection2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 IN (?1, ?2, ?3, ?4, ?5)");
            query.setParameter(1, 1);
            query.setParameter(2, 2);
            query.setParameter(3, 3);
            query.setParameter(4, 5);
            query.setParameter(5, 8);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 IN (1, 2, 3, 5, 8)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 IN (1, ?1, 3, 5, ?3)");
            query.setParameter(1, 2);
            query.setParameter(3, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue3 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue4 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue5 = cb.parameter(Integer.class);
            cquery.where(root.get(OLGH17837Entity_.intVal1).in(intValue1, intValue2, intValue3, intValue4, intValue5));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 1);
            query.setParameter(intValue2, 2);
            query.setParameter(intValue3, 3);
            query.setParameter(intValue4, 5);
            query.setParameter(intValue5, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));

            // IN function test #2 with CriteriaBuilder literal values
            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(root2.get(OLGH17837Entity_.intVal1).in(1, 2, 3, 5, 8));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue6 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intValue7 = cb3.parameter(Integer.class);

            In<Integer> in = cb3.in(root3.get(OLGH17837Entity_.intVal1));
            in = in.value(1);
            in = in.value(intValue6);
            in = in.value(3).value(5);
            in = in.value(intValue7);
            cquery3.where(in);

            query = em.createQuery(cquery3);
            query.setParameter(intValue6, 2);
            query.setParameter(intValue7, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
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

    public void testInCollection3_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 IN ?1");
            query.setParameter(1, java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21));
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?,?,?,?,?,?,?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 IN (1, 2, 3, 5, 8, 13, 21)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (1, 2, 3, 5, 8, 13, 21))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 IN (1, ?1, 3, 5, 8, ?3, ?2)");
            query.setParameter(1, 2);
            query.setParameter(2, 21);
            query.setParameter(3, 13);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (1, ?, 3, 5, 8, ?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<List> colValue = cb.parameter(List.class);
            cquery.groupBy(root.get(OLGH17837Entity_.intVal1));
            cquery.having(root.get(OLGH17837Entity_.intVal1).in(colValue));

            query = em.createQuery(cquery);
            List<Integer> intCollection2 = java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21);
            query.setParameter(colValue, intCollection2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?,?,?,?,?,?,?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            List<Integer> intCollection3 = java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21);
            cquery2.groupBy(root2.get(OLGH17837Entity_.intVal1));
            cquery2.having(root2.get(OLGH17837Entity_.intVal1).in(intCollection3));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (1, 2, 3, 5, 8, 13, 21))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));
            In<Integer> in = cb3.in(root3.get(OLGH17837Entity_.intVal1)).value(1).value(3).value(5).value(8);
            ParameterExpression<List> colValue2 = cb3.parameter(List.class);
            cquery3.groupBy(root3.get(OLGH17837Entity_.intVal1));
            cquery3.having(cb3.or(in, root3.get(OLGH17837Entity_.intVal1).in(colValue2)));

            query = em.createQuery(cquery3);
            query.setParameter(colValue2, java.util.Arrays.asList(2, 13, 21));
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING ((INTVAL1 IN (?, ?, ?, ?)) OR (INTVAL1 IN (?,?,?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING ((INTVAL1 IN (1, 3, 5, 8)) OR (INTVAL1 IN (?,?,?)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING ((INTVAL1 IN (?, ?, ?, ?)) OR (INTVAL1 IN (?,?,?)))", sql.remove(0));
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

    public void testInCollection3_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 IN ?1");
            query.setParameter(1, java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21));
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?,?,?,?,?,?,?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 IN (1, 2, 3, 5, 8, 13, 21)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (1, 2, 3, 5, 8, 13, 21))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 IN (1, ?1, 3, 5, 8, ?3, ?2)");
            query.setParameter(1, 2);
            query.setParameter(2, 21);
            query.setParameter(3, 13);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (1, ?, 3, 5, 8, ?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<List> colValue = cb.parameter(List.class);
            cquery.groupBy(root.get(OLGH17837Entity_.intVal1));
            cquery.having(root.get(OLGH17837Entity_.intVal1).in(colValue));

            query = em.createQuery(cquery);
            List<Integer> intCollection2 = java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21);
            query.setParameter(colValue, intCollection2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?,?,?,?,?,?,?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            List<Integer> intCollection3 = java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21);
            cquery2.groupBy(root2.get(OLGH17837Entity_.intVal1));
            cquery2.having(root2.get(OLGH17837Entity_.intVal1).in(intCollection3));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (1, 2, 3, 5, 8, 13, 21))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));
            In<Integer> in = cb3.in(root3.get(OLGH17837Entity_.intVal1)).value(1).value(3).value(5).value(8);
            ParameterExpression<List> colValue2 = cb3.parameter(List.class);
            cquery3.groupBy(root3.get(OLGH17837Entity_.intVal1));
            cquery3.having(cb3.or(in, root3.get(OLGH17837Entity_.intVal1).in(colValue2)));

            query = em.createQuery(cquery3);
            query.setParameter(colValue2, java.util.Arrays.asList(2, 13, 21));
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING ((INTVAL1 IN (?, ?, ?, ?)) OR (INTVAL1 IN (?,?,?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING ((INTVAL1 IN (1, 3, 5, 8)) OR (INTVAL1 IN (?,?,?)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING ((INTVAL1 IN (?, ?, ?, ?)) OR (INTVAL1 IN (?,?,?)))", sql.remove(0));
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

    public void testInCollection3_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 IN ?1");
            query.setParameter(1, java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21));
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?,?,?,?,?,?,?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 IN (1, 2, 3, 5, 8, 13, 21)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 IN (1, ?1, 3, 5, 8, ?3, ?2)");
            query.setParameter(1, 2);
            query.setParameter(2, 21);
            query.setParameter(3, 13);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<List> colValue = cb.parameter(List.class);
            cquery.groupBy(root.get(OLGH17837Entity_.intVal1));
            cquery.having(root.get(OLGH17837Entity_.intVal1).in(colValue));

            query = em.createQuery(cquery);
            List<Integer> intCollection2 = java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21);
            query.setParameter(colValue, intCollection2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?,?,?,?,?,?,?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            List<Integer> intCollection3 = java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21);
            cquery2.groupBy(root2.get(OLGH17837Entity_.intVal1));
            cquery2.having(root2.get(OLGH17837Entity_.intVal1).in(intCollection3));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));
            In<Integer> in = cb3.in(root3.get(OLGH17837Entity_.intVal1)).value(1).value(3).value(5).value(8);
            ParameterExpression<List> colValue2 = cb3.parameter(List.class);
            cquery3.groupBy(root3.get(OLGH17837Entity_.intVal1));
            cquery3.having(cb3.or(in, root3.get(OLGH17837Entity_.intVal1).in(colValue2)));

            query = em.createQuery(cquery3);
            query.setParameter(colValue2, java.util.Arrays.asList(2, 13, 21));
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING ((INTVAL1 IN (?, ?, ?, ?)) OR (INTVAL1 IN (?,?,?)))", sql.remove(0));
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

    public void testInCollection4_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 IN (?1, ?2, ?3, ?4, ?5)");
            query.setParameter(1, 1);
            query.setParameter(2, 2);
            query.setParameter(3, 3);
            query.setParameter(4, 5);
            query.setParameter(5, 8);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 IN (1, 2, 3, 5, 8)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (1, 2, 3, 5, 8))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 IN (1, ?1, 3, 5, ?3)");
            query.setParameter(1, 2);
            query.setParameter(3, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (1, ?, 3, 5, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue3 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue4 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue5 = cb.parameter(Integer.class);
            cquery.groupBy(root.get(OLGH17837Entity_.intVal1));
            cquery.having(root.get(OLGH17837Entity_.intVal1).in(intValue1, intValue2, intValue3, intValue4, intValue5));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 1);
            query.setParameter(intValue2, 2);
            query.setParameter(intValue3, 3);
            query.setParameter(intValue4, 5);
            query.setParameter(intValue5, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.groupBy(root2.get(OLGH17837Entity_.intVal1));
            cquery2.having(root2.get(OLGH17837Entity_.intVal1).in(1, 2, 3, 5, 8));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (1, 2, 3, 5, 8))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));
            cquery3.groupBy(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue6 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intValue7 = cb3.parameter(Integer.class);

            In<Integer> in = cb3.in(root3.get(OLGH17837Entity_.intVal1));
            in = in.value(1);
            in = in.value(intValue6);
            in = in.value(3).value(5);
            in = in.value(intValue7);
            cquery3.having(in);

            query = em.createQuery(cquery3);
            query.setParameter(intValue6, 2);
            query.setParameter(intValue7, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (1, ?, 3, 5, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
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

    public void testInCollection4_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 IN (?1, ?2, ?3, ?4, ?5)");
            query.setParameter(1, 1);
            query.setParameter(2, 2);
            query.setParameter(3, 3);
            query.setParameter(4, 5);
            query.setParameter(5, 8);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 IN (1, 2, 3, 5, 8)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (1, 2, 3, 5, 8))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 IN (1, ?1, 3, 5, ?3)");
            query.setParameter(1, 2);
            query.setParameter(3, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (1, ?, 3, 5, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue3 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue4 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue5 = cb.parameter(Integer.class);
            cquery.groupBy(root.get(OLGH17837Entity_.intVal1));
            cquery.having(root.get(OLGH17837Entity_.intVal1).in(intValue1, intValue2, intValue3, intValue4, intValue5));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 1);
            query.setParameter(intValue2, 2);
            query.setParameter(intValue3, 3);
            query.setParameter(intValue4, 5);
            query.setParameter(intValue5, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.groupBy(root2.get(OLGH17837Entity_.intVal1));
            cquery2.having(root2.get(OLGH17837Entity_.intVal1).in(1, 2, 3, 5, 8));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (1, 2, 3, 5, 8))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));
            cquery3.groupBy(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue6 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intValue7 = cb3.parameter(Integer.class);

            In<Integer> in = cb3.in(root3.get(OLGH17837Entity_.intVal1));
            in = in.value(1);
            in = in.value(intValue6);
            in = in.value(3).value(5);
            in = in.value(intValue7);
            cquery3.having(in);

            query = em.createQuery(cquery3);
            query.setParameter(intValue6, 2);
            query.setParameter(intValue7, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (1, ?, 3, 5, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
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

    public void testInCollection4_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 IN (?1, ?2, ?3, ?4, ?5)");
            query.setParameter(1, 1);
            query.setParameter(2, 2);
            query.setParameter(3, 3);
            query.setParameter(4, 5);
            query.setParameter(5, 8);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 IN (1, 2, 3, 5, 8)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING s.intVal1 IN (1, ?1, 3, 5, ?3)");
            query.setParameter(1, 2);
            query.setParameter(3, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue3 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue4 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue5 = cb.parameter(Integer.class);
            cquery.groupBy(root.get(OLGH17837Entity_.intVal1));
            cquery.having(root.get(OLGH17837Entity_.intVal1).in(intValue1, intValue2, intValue3, intValue4, intValue5));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 1);
            query.setParameter(intValue2, 2);
            query.setParameter(intValue3, 3);
            query.setParameter(intValue4, 5);
            query.setParameter(intValue5, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.groupBy(root2.get(OLGH17837Entity_.intVal1));
            cquery2.having(root2.get(OLGH17837Entity_.intVal1).in(1, 2, 3, 5, 8));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));
            cquery3.groupBy(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue6 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intValue7 = cb3.parameter(Integer.class);

            In<Integer> in = cb3.in(root3.get(OLGH17837Entity_.intVal1));
            in = in.value(1);
            in = in.value(intValue6);
            in = in.value(3).value(5);
            in = in.value(intValue7);
            cquery3.having(in);

            query = em.createQuery(cquery3);
            query.setParameter(intValue6, 2);
            query.setParameter(intValue7, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (INTVAL1 IN (?, ?, ?, ?, ?))", sql.remove(0));
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

    public void testInSubquery1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?2)");
            query.setParameter(1, 5);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?1)");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.intVal2), intValue2));

            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            cquery.where(intValue1.in(subquery));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.intVal2), 9));

            cquery2.where(cb2.literal(5).in(subquery2));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue3 = cb3.parameter(Integer.class);
            Subquery<Integer> subquery3 = cquery3.subquery(Integer.class);
            Root<OLGH17837Entity> subroot3 = subquery3.from(OLGH17837Entity.class);
            subquery3.select(subroot3.get(OLGH17837Entity_.intVal2));
            subquery3.where(cb3.equal(subroot3.get(OLGH17837Entity_.intVal2), intValue3));

            cquery3.where(cb3.literal(5).in(subquery3));

            query = em.createQuery(cquery3);
            query.setParameter(intValue3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
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

    public void testInSubquery1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?2)");
            query.setParameter(1, 5);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?1)");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.intVal2), intValue2));

            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            cquery.where(intValue1.in(subquery));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.intVal2), 9));

            cquery2.where(cb2.literal(5).in(subquery2));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue3 = cb3.parameter(Integer.class);
            Subquery<Integer> subquery3 = cquery3.subquery(Integer.class);
            Root<OLGH17837Entity> subroot3 = subquery3.from(OLGH17837Entity.class);
            subquery3.select(subroot3.get(OLGH17837Entity_.intVal2));
            subquery3.where(cb3.equal(subroot3.get(OLGH17837Entity_.intVal2), intValue3));

            cquery3.where(cb3.literal(5).in(subquery3));

            query = em.createQuery(cquery3);
            query.setParameter(intValue3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
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

    public void testInSubquery1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?2)");
            query.setParameter(1, 5);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?1)");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.intVal2), intValue2));

            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            cquery.where(intValue1.in(subquery));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.intVal2), 9));

            cquery2.where(cb2.literal(5).in(subquery2));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue3 = cb3.parameter(Integer.class);
            Subquery<Integer> subquery3 = cquery3.subquery(Integer.class);
            Root<OLGH17837Entity> subroot3 = subquery3.from(OLGH17837Entity.class);
            subquery3.select(subroot3.get(OLGH17837Entity_.intVal2));
            subquery3.where(cb3.equal(subroot3.get(OLGH17837Entity_.intVal2), intValue3));

            cquery3.where(cb3.literal(5).in(subquery3));

            query = em.createQuery(cquery3);
            query.setParameter(intValue3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
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

    public void testInSubquery2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?2)");
            query.setParameter(1, 5);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.setParameter(1, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.intVal2), intValue2));

            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            cquery.where(cb.in(intValue1).value(subquery));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.intVal2), 9));

            cquery2.where(cb2.in(cb2.literal(5)).value(subquery2));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery3 = cquery3.subquery(Integer.class);
            Root<OLGH17837Entity> subroot3 = subquery3.from(OLGH17837Entity.class);
            subquery3.select(subroot3.get(OLGH17837Entity_.intVal2));
            subquery3.where(cb3.equal(subroot3.get(OLGH17837Entity_.intVal2), 9));

            ParameterExpression<Integer> intValue3 = cb3.parameter(Integer.class);
            cquery3.where(cb3.in(intValue3).value(subquery3));

            query = em.createQuery(cquery3);
            query.setParameter(intValue3, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
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

    public void testInSubquery2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?2)");
            query.setParameter(1, 5);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.setParameter(1, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.intVal2), intValue2));

            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            cquery.where(cb.in(intValue1).value(subquery));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.intVal2), 9));

            cquery2.where(cb2.in(cb2.literal(5)).value(subquery2));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery3 = cquery3.subquery(Integer.class);
            Root<OLGH17837Entity> subroot3 = subquery3.from(OLGH17837Entity.class);
            subquery3.select(subroot3.get(OLGH17837Entity_.intVal2));
            subquery3.where(cb3.equal(subroot3.get(OLGH17837Entity_.intVal2), 9));

            ParameterExpression<Integer> intValue3 = cb3.parameter(Integer.class);
            cquery3.where(cb3.in(intValue3).value(subquery3));

            query = em.createQuery(cquery3);
            query.setParameter(intValue3, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
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

    public void testInSubquery2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?2)");
            query.setParameter(1, 5);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.setParameter(1, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.intVal2), intValue2));

            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            cquery.where(cb.in(intValue1).value(subquery));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.intVal2), 9));

            cquery2.where(cb2.in(cb2.literal(5)).value(subquery2));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery3 = cquery3.subquery(Integer.class);
            Root<OLGH17837Entity> subroot3 = subquery3.from(OLGH17837Entity.class);
            subquery3.select(subroot3.get(OLGH17837Entity_.intVal2));
            subquery3.where(cb3.equal(subroot3.get(OLGH17837Entity_.intVal2), 9));

            ParameterExpression<Integer> intValue3 = cb3.parameter(Integer.class);
            cquery3.where(cb3.in(intValue3).value(subquery3));

            query = em.createQuery(cquery3);
            query.setParameter(intValue3, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));
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

    public void testInSubquery3_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 IN (?2, ?3, ?4))");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 7);
            query.setParameter(4, 8);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 IN (6, 7, 8))");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (6, 7, 8)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                        sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 IN (6, 7, ?4))");
            query.setParameter(1, 5);
            query.setParameter(4, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (6, 7, ?)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                        sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue3 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue4 = cb.parameter(Integer.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.in(subroot.get(OLGH17837Entity_.intVal2)).value(intValue2).value(intValue3).value(intValue4));

            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            cquery.where(cb.in(intValue1).value(subquery));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 6);
            query.setParameter(intValue3, 7);
            query.setParameter(intValue4, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.in(subroot2.get(OLGH17837Entity_.intVal2)).value(6).value(7).value(8));

            cquery2.where(cb2.in(cb2.literal(5)).value(subquery2));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (6, 7, 8)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                        sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue6 = cb3.parameter(Integer.class);
            Subquery<Integer> subquery3 = cquery3.subquery(Integer.class);
            Root<OLGH17837Entity> subroot3 = subquery3.from(OLGH17837Entity.class);
            subquery3.select(subroot3.get(OLGH17837Entity_.intVal2));
            subquery3.where(cb3.equal(subroot3.get(OLGH17837Entity_.intVal2), 9));
            subquery3.where(cb3.in(subroot3.get(OLGH17837Entity_.intVal2)).value(6).value(7).value(intValue6));

            ParameterExpression<Integer> intValue5 = cb3.parameter(Integer.class);
            cquery3.where(cb3.in(intValue5).value(subquery3));

            query = em.createQuery(cquery3);
            query.setParameter(intValue5, 5);
            query.setParameter(intValue6, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (6, 7, ?)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                        sql.remove(0));
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

    public void testInSubquery3_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 IN (?2, ?3, ?4))");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 7);
            query.setParameter(4, 8);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 IN (6, 7, 8))");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (6, 7, 8)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                        sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 IN (6, 7, ?4))");
            query.setParameter(1, 5);
            query.setParameter(4, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (6, 7, ?)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                        sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue3 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue4 = cb.parameter(Integer.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.in(subroot.get(OLGH17837Entity_.intVal2)).value(intValue2).value(intValue3).value(intValue4));

            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            cquery.where(cb.in(intValue1).value(subquery));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 6);
            query.setParameter(intValue3, 7);
            query.setParameter(intValue4, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.in(subroot2.get(OLGH17837Entity_.intVal2)).value(6).value(7).value(8));

            cquery2.where(cb2.in(cb2.literal(5)).value(subquery2));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (6, 7, 8)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                        sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue6 = cb3.parameter(Integer.class);
            Subquery<Integer> subquery3 = cquery3.subquery(Integer.class);
            Root<OLGH17837Entity> subroot3 = subquery3.from(OLGH17837Entity.class);
            subquery3.select(subroot3.get(OLGH17837Entity_.intVal2));
            subquery3.where(cb3.equal(subroot3.get(OLGH17837Entity_.intVal2), 9));
            subquery3.where(cb3.in(subroot3.get(OLGH17837Entity_.intVal2)).value(6).value(7).value(intValue6));

            ParameterExpression<Integer> intValue5 = cb3.parameter(Integer.class);
            cquery3.where(cb3.in(intValue5).value(subquery3));

            query = em.createQuery(cquery3);
            query.setParameter(intValue5, 5);
            query.setParameter(intValue6, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (6, 7, ?)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                        sql.remove(0));
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

    public void testInSubquery3_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 IN (?2, ?3, ?4))");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 7);
            query.setParameter(4, 8);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 IN (6, 7, 8))");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 IN (6, 7, ?4))");
            query.setParameter(1, 5);
            query.setParameter(4, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue3 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue4 = cb.parameter(Integer.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.in(subroot.get(OLGH17837Entity_.intVal2)).value(intValue2).value(intValue3).value(intValue4));

            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            cquery.where(cb.in(intValue1).value(subquery));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 6);
            query.setParameter(intValue3, 7);
            query.setParameter(intValue4, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.in(subroot2.get(OLGH17837Entity_.intVal2)).value(6).value(7).value(8));

            cquery2.where(cb2.in(cb2.literal(5)).value(subquery2));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue6 = cb3.parameter(Integer.class);
            Subquery<Integer> subquery3 = cquery3.subquery(Integer.class);
            Root<OLGH17837Entity> subroot3 = subquery3.from(OLGH17837Entity.class);
            subquery3.select(subroot3.get(OLGH17837Entity_.intVal2));
            subquery3.where(cb3.equal(subroot3.get(OLGH17837Entity_.intVal2), 9));
            subquery3.where(cb3.in(subroot3.get(OLGH17837Entity_.intVal2)).value(6).value(7).value(intValue6));

            ParameterExpression<Integer> intValue5 = cb3.parameter(Integer.class);
            cquery3.where(cb3.in(intValue5).value(subquery3));

            query = em.createQuery(cquery3);
            query.setParameter(intValue5, 5);
            query.setParameter(intValue6, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                sql.remove(0));
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

    public void testNotInCollection1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT IN ?1");
            query.setParameter(1, java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21));
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?,?,?,?,?,?,?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT IN (1, 2, 3, 5, 8, 13, 21)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (1, 2, 3, 5, 8, 13, 21))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT IN (1, ?1, 3, 5, 8, ?3, ?2)");
            query.setParameter(1, 2);
            query.setParameter(2, 21);
            query.setParameter(3, 13);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (1, ?, 3, 5, 8, ?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<List> colValue = cb.parameter(List.class);
            cquery.where(root.get(OLGH17837Entity_.intVal1).in(colValue).not());

            query = em.createQuery(cquery);
            List<Integer> intCollection2 = java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21);
            query.setParameter(colValue, intCollection2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (?,?,?,?,?,?,?)))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            List<Integer> intCollection3 = java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21);
            cquery2.where(root2.get(OLGH17837Entity_.intVal1).in(intCollection3).not());

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (?, ?, ?, ?, ?, ?, ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (1, 2, 3, 5, 8, 13, 21)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (?, ?, ?, ?, ?, ?, ?)))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));
            In<Integer> in = cb3.in(root3.get(OLGH17837Entity_.intVal1)).value(1).value(3).value(5).value(8);
            ParameterExpression<List> colValue2 = cb3.parameter(List.class);
            cquery3.where(cb3.or(cb3.not(in), root3.get(OLGH17837Entity_.intVal1).in(colValue2).not()));

            query = em.createQuery(cquery3);
            query.setParameter(colValue2, java.util.Arrays.asList(2, 13, 21));
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (NOT ((INTVAL1 IN (?, ?, ?, ?))) OR NOT ((INTVAL1 IN (?,?,?))))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (NOT ((INTVAL1 IN (1, 3, 5, 8))) OR NOT ((INTVAL1 IN (?,?,?))))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (NOT ((INTVAL1 IN (?, ?, ?, ?))) OR NOT ((INTVAL1 IN (?,?,?))))", sql.remove(0));
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

    public void testNotInCollection1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT IN ?1");
            query.setParameter(1, java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21));
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?,?,?,?,?,?,?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT IN (1, 2, 3, 5, 8, 13, 21)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (1, 2, 3, 5, 8, 13, 21))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT IN (1, ?1, 3, 5, 8, ?3, ?2)");
            query.setParameter(1, 2);
            query.setParameter(2, 21);
            query.setParameter(3, 13);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (1, ?, 3, 5, 8, ?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<List> colValue = cb.parameter(List.class);
            cquery.where(root.get(OLGH17837Entity_.intVal1).in(colValue).not());

            query = em.createQuery(cquery);
            List<Integer> intCollection2 = java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21);
            query.setParameter(colValue, intCollection2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (?,?,?,?,?,?,?)))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            List<Integer> intCollection3 = java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21);
            cquery2.where(root2.get(OLGH17837Entity_.intVal1).in(intCollection3).not());

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (?, ?, ?, ?, ?, ?, ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (1, 2, 3, 5, 8, 13, 21)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (?, ?, ?, ?, ?, ?, ?)))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));
            In<Integer> in = cb3.in(root3.get(OLGH17837Entity_.intVal1)).value(1).value(3).value(5).value(8);
            ParameterExpression<List> colValue2 = cb3.parameter(List.class);
            cquery3.where(cb3.or(cb3.not(in), root3.get(OLGH17837Entity_.intVal1).in(colValue2).not()));

            query = em.createQuery(cquery3);
            query.setParameter(colValue2, java.util.Arrays.asList(2, 13, 21));
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (NOT ((INTVAL1 IN (?, ?, ?, ?))) OR NOT ((INTVAL1 IN (?,?,?))))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (NOT ((INTVAL1 IN (1, 3, 5, 8))) OR NOT ((INTVAL1 IN (?,?,?))))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (NOT ((INTVAL1 IN (?, ?, ?, ?))) OR NOT ((INTVAL1 IN (?,?,?))))", sql.remove(0));
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

    public void testNotInCollection1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT IN ?1");
            query.setParameter(1, java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21));
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?,?,?,?,?,?,?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT IN (1, 2, 3, 5, 8, 13, 21)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT IN (1, ?1, 3, 5, 8, ?3, ?2)");
            query.setParameter(1, 2);
            query.setParameter(2, 21);
            query.setParameter(3, 13);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?, ?, ?, ?, ?, ?, ?))", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<List> colValue = cb.parameter(List.class);
            cquery.where(root.get(OLGH17837Entity_.intVal1).in(colValue).not());

            query = em.createQuery(cquery);
            List<Integer> intCollection2 = java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21);
            query.setParameter(colValue, intCollection2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (?,?,?,?,?,?,?)))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            List<Integer> intCollection3 = java.util.Arrays.asList(1, 2, 3, 5, 8, 13, 21);
            cquery2.where(root2.get(OLGH17837Entity_.intVal1).in(intCollection3).not());

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (?, ?, ?, ?, ?, ?, ?)))", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));
            In<Integer> in = cb3.in(root3.get(OLGH17837Entity_.intVal1)).value(1).value(3).value(5).value(8);
            ParameterExpression<List> colValue2 = cb3.parameter(List.class);
            cquery3.where(cb3.or(cb3.not(in), root3.get(OLGH17837Entity_.intVal1).in(colValue2).not()));

            query = em.createQuery(cquery3);
            query.setParameter(colValue2, java.util.Arrays.asList(2, 13, 21));
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (NOT ((INTVAL1 IN (?, ?, ?, ?))) OR NOT ((INTVAL1 IN (?,?,?))))", sql.remove(0));
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

    public void testNotInCollection2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT IN (?1, ?2, ?3, ?4, ?5)");
            query.setParameter(1, 1);
            query.setParameter(2, 2);
            query.setParameter(3, 3);
            query.setParameter(4, 5);
            query.setParameter(5, 8);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?, ?, ?, ?, ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT IN (1, 2, 3, 5, 8)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (1, 2, 3, 5, 8))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT IN (1, ?1, 3, 5, ?3)");
            query.setParameter(1, 2);
            query.setParameter(3, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (1, ?, 3, 5, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue3 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue4 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue5 = cb.parameter(Integer.class);
            cquery.where(root.get(OLGH17837Entity_.intVal1).in(intValue1, intValue2, intValue3, intValue4, intValue5).not());

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 1);
            query.setParameter(intValue2, 2);
            query.setParameter(intValue3, 3);
            query.setParameter(intValue4, 5);
            query.setParameter(intValue5, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (?, ?, ?, ?, ?)))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(root2.get(OLGH17837Entity_.intVal1).in(1, 2, 3, 5, 8).not());

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (?, ?, ?, ?, ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (1, 2, 3, 5, 8)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (?, ?, ?, ?, ?)))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue6 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intValue7 = cb3.parameter(Integer.class);

            In<Integer> in = cb3.in(root3.get(OLGH17837Entity_.intVal1));
            in = in.value(1);
            in = in.value(intValue6);
            in = in.value(3).value(5);
            in = in.value(intValue7);
            cquery3.where(in.not());

            query = em.createQuery(cquery3);
            query.setParameter(intValue6, 2);
            query.setParameter(intValue7, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (?, ?, ?, ?, ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (1, ?, 3, 5, ?)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (?, ?, ?, ?, ?)))", sql.remove(0));
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

    public void testNotInCollection2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT IN (?1, ?2, ?3, ?4, ?5)");
            query.setParameter(1, 1);
            query.setParameter(2, 2);
            query.setParameter(3, 3);
            query.setParameter(4, 5);
            query.setParameter(5, 8);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?, ?, ?, ?, ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT IN (1, 2, 3, 5, 8)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (1, 2, 3, 5, 8))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT IN (1, ?1, 3, 5, ?3)");
            query.setParameter(1, 2);
            query.setParameter(3, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?, ?, ?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (1, ?, 3, 5, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?, ?, ?, ?, ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue3 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue4 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue5 = cb.parameter(Integer.class);
            cquery.where(root.get(OLGH17837Entity_.intVal1).in(intValue1, intValue2, intValue3, intValue4, intValue5).not());

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 1);
            query.setParameter(intValue2, 2);
            query.setParameter(intValue3, 3);
            query.setParameter(intValue4, 5);
            query.setParameter(intValue5, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (?, ?, ?, ?, ?)))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(root2.get(OLGH17837Entity_.intVal1).in(1, 2, 3, 5, 8).not());

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (?, ?, ?, ?, ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (1, 2, 3, 5, 8)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (?, ?, ?, ?, ?)))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue6 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intValue7 = cb3.parameter(Integer.class);

            In<Integer> in = cb3.in(root3.get(OLGH17837Entity_.intVal1));
            in = in.value(1);
            in = in.value(intValue6);
            in = in.value(3).value(5);
            in = in.value(intValue7);
            cquery3.where(in.not());

            query = em.createQuery(cquery3);
            query.setParameter(intValue6, 2);
            query.setParameter(intValue7, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (?, ?, ?, ?, ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (1, ?, 3, 5, ?)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (?, ?, ?, ?, ?)))", sql.remove(0));
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

    public void testNotInCollection2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT IN (?1, ?2, ?3, ?4, ?5)");
            query.setParameter(1, 1);
            query.setParameter(2, 2);
            query.setParameter(3, 3);
            query.setParameter(4, 5);
            query.setParameter(5, 8);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?, ?, ?, ?, ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT IN (1, 2, 3, 5, 8)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?, ?, ?, ?, ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT IN (1, ?1, 3, 5, ?3)");
            query.setParameter(1, 2);
            query.setParameter(3, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 NOT IN (?, ?, ?, ?, ?))", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue3 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue4 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue5 = cb.parameter(Integer.class);
            cquery.where(root.get(OLGH17837Entity_.intVal1).in(intValue1, intValue2, intValue3, intValue4, intValue5).not());

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 1);
            query.setParameter(intValue2, 2);
            query.setParameter(intValue3, 3);
            query.setParameter(intValue4, 5);
            query.setParameter(intValue5, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (?, ?, ?, ?, ?)))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(root2.get(OLGH17837Entity_.intVal1).in(1, 2, 3, 5, 8).not());

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (?, ?, ?, ?, ?)))", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue6 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intValue7 = cb3.parameter(Integer.class);

            In<Integer> in = cb3.in(root3.get(OLGH17837Entity_.intVal1));
            in = in.value(1);
            in = in.value(intValue6);
            in = in.value(3).value(5);
            in = in.value(intValue7);
            cquery3.where(in.not());

            query = em.createQuery(cquery3);
            query.setParameter(intValue6, 2);
            query.setParameter(intValue7, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 IN (?, ?, ?, ?, ?)))", sql.remove(0));
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

    public void testNotInSubquery1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?2)");
            query.setParameter(1, 5);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))",
                                        sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?1)");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))",
                                        sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.intVal2), intValue2));

            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            cquery.where(cb.not(intValue1.in(subquery)));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.intVal2), 9));

            cquery2.where(cb2.not(cb2.literal(5).in(subquery2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue3 = cb3.parameter(Integer.class);
            Subquery<Integer> subquery3 = cquery3.subquery(Integer.class);
            Root<OLGH17837Entity> subroot3 = subquery3.from(OLGH17837Entity.class);
            subquery3.select(subroot3.get(OLGH17837Entity_.intVal2));
            subquery3.where(cb3.equal(subroot3.get(OLGH17837Entity_.intVal2), intValue3));

            cquery3.where(cb3.not(cb3.literal(5).in(subquery3)));

            query = em.createQuery(cquery3);
            query.setParameter(intValue3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
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

    public void testNotInSubquery1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?2)");
            query.setParameter(1, 5);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))",
                                        sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?1)");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))",
                                        sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.intVal2), intValue2));

            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            cquery.where(cb.not(intValue1.in(subquery)));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.intVal2), 9));

            cquery2.where(cb2.not(cb2.literal(5).in(subquery2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue3 = cb3.parameter(Integer.class);
            Subquery<Integer> subquery3 = cquery3.subquery(Integer.class);
            Root<OLGH17837Entity> subroot3 = subquery3.from(OLGH17837Entity.class);
            subquery3.select(subroot3.get(OLGH17837Entity_.intVal2));
            subquery3.where(cb3.equal(subroot3.get(OLGH17837Entity_.intVal2), intValue3));

            cquery3.where(cb3.not(cb3.literal(5).in(subquery3)));

            query = em.createQuery(cquery3);
            query.setParameter(intValue3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
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

    public void testNotInSubquery1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?2)");
            query.setParameter(1, 5);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?1)");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.intVal2), intValue2));

            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            cquery.where(cb.not(intValue1.in(subquery)));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.intVal2), 9));

            cquery2.where(cb2.not(cb2.literal(5).in(subquery2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue3 = cb3.parameter(Integer.class);
            Subquery<Integer> subquery3 = cquery3.subquery(Integer.class);
            Root<OLGH17837Entity> subroot3 = subquery3.from(OLGH17837Entity.class);
            subquery3.select(subroot3.get(OLGH17837Entity_.intVal2));
            subquery3.where(cb3.equal(subroot3.get(OLGH17837Entity_.intVal2), intValue3));

            cquery3.where(cb3.not(cb3.literal(5).in(subquery3)));

            query = em.createQuery(cquery3);
            query.setParameter(intValue3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
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

    public void testNotInSubquery2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?2)");
            query.setParameter(1, 5);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))",
                                        sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.setParameter(1, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))",
                                        sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.intVal2), intValue2));

            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            cquery.where(cb.not(cb.in(intValue1).value(subquery)));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.intVal2), 9));

            cquery2.where(cb2.not(cb2.in(cb2.literal(5)).value(subquery2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery3 = cquery3.subquery(Integer.class);
            Root<OLGH17837Entity> subroot3 = subquery3.from(OLGH17837Entity.class);
            subquery3.select(subroot3.get(OLGH17837Entity_.intVal2));
            subquery3.where(cb3.equal(subroot3.get(OLGH17837Entity_.intVal2), 9));

            ParameterExpression<Integer> intValue3 = cb3.parameter(Integer.class);
            cquery3.where(cb3.not(cb3.in(intValue3).value(subquery3)));

            query = em.createQuery(cquery3);
            query.setParameter(intValue3, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
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

    public void testNotInSubquery2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?2)");
            query.setParameter(1, 5);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))",
                                        sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.setParameter(1, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))",
                                        sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.intVal2), intValue2));

            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            cquery.where(cb.not(cb.in(intValue1).value(subquery)));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.intVal2), 9));

            cquery2.where(cb2.not(cb2.in(cb2.literal(5)).value(subquery2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery3 = cquery3.subquery(Integer.class);
            Root<OLGH17837Entity> subroot3 = subquery3.from(OLGH17837Entity.class);
            subquery3.select(subroot3.get(OLGH17837Entity_.intVal2));
            subquery3.where(cb3.equal(subroot3.get(OLGH17837Entity_.intVal2), 9));

            ParameterExpression<Integer> intValue3 = cb3.parameter(Integer.class);
            cquery3.where(cb3.not(cb3.in(intValue3).value(subquery3)));

            query = em.createQuery(cquery3);
            query.setParameter(intValue3, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
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

    public void testNotInSubquery2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?2)");
            query.setParameter(1, 5);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.setParameter(1, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?))", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.intVal2), intValue2));

            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            cquery.where(cb.not(cb.in(intValue1).value(subquery)));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.intVal2), 9));

            cquery2.where(cb2.not(cb2.in(cb2.literal(5)).value(subquery2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery3 = cquery3.subquery(Integer.class);
            Root<OLGH17837Entity> subroot3 = subquery3.from(OLGH17837Entity.class);
            subquery3.select(subroot3.get(OLGH17837Entity_.intVal2));
            subquery3.where(cb3.equal(subroot3.get(OLGH17837Entity_.intVal2), 9));

            ParameterExpression<Integer> intValue3 = cb3.parameter(Integer.class);
            cquery3.where(cb3.not(cb3.in(intValue3).value(subquery3)));

            query = em.createQuery(cquery3);
            query.setParameter(intValue3, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
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

    public void testNotInSubquery3_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 IN (?2, ?3, ?4))");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 7);
            query.setParameter(4, 8);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 IN (6, 7, 8))");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (6, 7, 8)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                        sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 IN (6, 7, ?4))");
            query.setParameter(1, 5);
            query.setParameter(4, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (6, 7, ?)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                        sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue3 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue4 = cb.parameter(Integer.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.not(cb.in(subroot.get(OLGH17837Entity_.intVal2)).value(intValue2).value(intValue3).value(intValue4)));

            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            cquery.where(cb.in(intValue1).value(subquery).not());

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 6);
            query.setParameter(intValue3, 7);
            query.setParameter(intValue4, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE NOT ((t1.INTVAL2 IN (?, ?, ?)))))",
                                sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.not(cb2.in(subroot2.get(OLGH17837Entity_.intVal2)).value(6).value(7).value(8)));

            cquery2.where(cb2.in(cb2.literal(5)).value(subquery2).not());

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE NOT ((t1.INTVAL2 IN (?, ?, ?)))))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE NOT ((t1.INTVAL2 IN (6, 7, 8)))))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE NOT ((t1.INTVAL2 IN (?, ?, ?)))))",
                                        sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue6 = cb3.parameter(Integer.class);
            Subquery<Integer> subquery3 = cquery3.subquery(Integer.class);
            Root<OLGH17837Entity> subroot3 = subquery3.from(OLGH17837Entity.class);
            subquery3.select(subroot3.get(OLGH17837Entity_.intVal2));
            subquery3.where(cb3.equal(subroot3.get(OLGH17837Entity_.intVal2), 9));
            subquery3.where(cb3.not(cb3.in(subroot3.get(OLGH17837Entity_.intVal2)).value(6).value(7).value(intValue6)));

            ParameterExpression<Integer> intValue5 = cb3.parameter(Integer.class);
            cquery3.where(cb3.in(intValue5).value(subquery3).not());

            query = em.createQuery(cquery3);
            query.setParameter(intValue5, 5);
            query.setParameter(intValue6, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE NOT ((t1.INTVAL2 IN (?, ?, ?)))))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE NOT ((t1.INTVAL2 IN (6, 7, ?)))))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE NOT ((t1.INTVAL2 IN (?, ?, ?)))))",
                                        sql.remove(0));
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

    public void testNotInSubquery3_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 IN (?2, ?3, ?4))");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 7);
            query.setParameter(4, 8);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 IN (6, 7, 8))");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE 5 NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (6, 7, 8)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                        sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 IN (6, 7, ?4))");
            query.setParameter(1, 5);
            query.setParameter(4, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (6, 7, ?)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                        sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue3 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue4 = cb.parameter(Integer.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.not(cb.in(subroot.get(OLGH17837Entity_.intVal2)).value(intValue2).value(intValue3).value(intValue4)));

            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            cquery.where(cb.in(intValue1).value(subquery).not());

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 6);
            query.setParameter(intValue3, 7);
            query.setParameter(intValue4, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE NOT ((t1.INTVAL2 IN (?, ?, ?)))))",
                                sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.not(cb2.in(subroot2.get(OLGH17837Entity_.intVal2)).value(6).value(7).value(8)));

            cquery2.where(cb2.in(cb2.literal(5)).value(subquery2).not());

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE NOT ((t1.INTVAL2 IN (?, ?, ?)))))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (5 IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE NOT ((t1.INTVAL2 IN (6, 7, 8)))))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE NOT ((t1.INTVAL2 IN (?, ?, ?)))))",
                                        sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue6 = cb3.parameter(Integer.class);
            Subquery<Integer> subquery3 = cquery3.subquery(Integer.class);
            Root<OLGH17837Entity> subroot3 = subquery3.from(OLGH17837Entity.class);
            subquery3.select(subroot3.get(OLGH17837Entity_.intVal2));
            subquery3.where(cb3.equal(subroot3.get(OLGH17837Entity_.intVal2), 9));
            subquery3.where(cb3.not(cb3.in(subroot3.get(OLGH17837Entity_.intVal2)).value(6).value(7).value(intValue6)));

            ParameterExpression<Integer> intValue5 = cb3.parameter(Integer.class);
            cquery3.where(cb3.in(intValue5).value(subquery3).not());

            query = em.createQuery(cquery3);
            query.setParameter(intValue5, 5);
            query.setParameter(intValue6, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE NOT ((t1.INTVAL2 IN (?, ?, ?)))))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE NOT ((t1.INTVAL2 IN (6, 7, ?)))))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE NOT ((t1.INTVAL2 IN (?, ?, ?)))))",
                                        sql.remove(0));
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

    public void testNotInSubquery3_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 IN (?2, ?3, ?4))");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 7);
            query.setParameter(4, 8);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 IN (6, 7, 8))");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 NOT IN (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 IN (6, 7, ?4))");
            query.setParameter(1, 5);
            query.setParameter(4, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE ? NOT IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 IN (?, ?, ?)))",
                                sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue3 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intValue4 = cb.parameter(Integer.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.not(cb.in(subroot.get(OLGH17837Entity_.intVal2)).value(intValue2).value(intValue3).value(intValue4)));

            ParameterExpression<Integer> intValue1 = cb.parameter(Integer.class);
            cquery.where(cb.in(intValue1).value(subquery).not());

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 6);
            query.setParameter(intValue3, 7);
            query.setParameter(intValue4, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE NOT ((t1.INTVAL2 IN (?, ?, ?)))))",
                                sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.not(cb2.in(subroot2.get(OLGH17837Entity_.intVal2)).value(6).value(7).value(8)));

            cquery2.where(cb2.in(cb2.literal(5)).value(subquery2).not());

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE NOT ((t1.INTVAL2 IN (?, ?, ?)))))",
                                sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue6 = cb3.parameter(Integer.class);
            Subquery<Integer> subquery3 = cquery3.subquery(Integer.class);
            Root<OLGH17837Entity> subroot3 = subquery3.from(OLGH17837Entity.class);
            subquery3.select(subroot3.get(OLGH17837Entity_.intVal2));
            subquery3.where(cb3.equal(subroot3.get(OLGH17837Entity_.intVal2), 9));
            subquery3.where(cb3.not(cb3.in(subroot3.get(OLGH17837Entity_.intVal2)).value(6).value(7).value(intValue6)));

            ParameterExpression<Integer> intValue5 = cb3.parameter(Integer.class);
            cquery3.where(cb3.in(intValue5).value(subquery3).not());

            query = em.createQuery(cquery3);
            query.setParameter(intValue5, 5);
            query.setParameter(intValue6, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (? IN (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE NOT ((t1.INTVAL2 IN (?, ?, ?)))))",
                                sql.remove(0));
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

    public void testBetween1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 BETWEEN ?1 AND ?2");
            query.setParameter(1, 0);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 BETWEEN 0 AND 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN 0 AND 9)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 BETWEEN 0 AND ?1");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN 0 AND ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.where(cb.between(root.get(OLGH17837Entity_.intVal1), intParam1, intParam2));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 0);
            query.setParameter(intParam2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.between(root2.get(OLGH17837Entity_.intVal1), 0, 9));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN 0 AND 9)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            cquery3.where(cb3.between(root3.get(OLGH17837Entity_.intVal1), cb3.literal(0), intParam3));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN 0 AND ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));
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

    public void testBetween1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 BETWEEN ?1 AND ?2");
            query.setParameter(1, 0);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 BETWEEN 0 AND 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN 0 AND 9)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 BETWEEN 0 AND ?1");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN 0 AND ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.where(cb.between(root.get(OLGH17837Entity_.intVal1), intParam1, intParam2));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 0);
            query.setParameter(intParam2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.between(root2.get(OLGH17837Entity_.intVal1), 0, 9));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN 0 AND 9)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            cquery3.where(cb3.between(root3.get(OLGH17837Entity_.intVal1), cb3.literal(0), intParam3));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN 0 AND ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));
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

    public void testBetween1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 BETWEEN ?1 AND ?2");
            query.setParameter(1, 0);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 BETWEEN 0 AND 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 BETWEEN 0 AND ?1");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.where(cb.between(root.get(OLGH17837Entity_.intVal1), intParam1, intParam2));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 0);
            query.setParameter(intParam2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.between(root2.get(OLGH17837Entity_.intVal1), 0, 9));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            cquery3.where(cb3.between(root3.get(OLGH17837Entity_.intVal1), cb3.literal(0), intParam3));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 BETWEEN ? AND ?)", sql.remove(0));
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

    public void testBetween2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 BETWEEN s.intVal1 AND ?2");
            query.setParameter(1, 4);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 BETWEEN s.intVal1 AND 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (4 BETWEEN INTVAL1 AND 9)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 BETWEEN s.intVal1 AND ?1");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (4 BETWEEN INTVAL1 AND ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.where(cb.between(intParam1, root.get(OLGH17837Entity_.intVal1), intParam2));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 4);
            query.setParameter(intParam2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.between(cb2.literal(4), root2.get(OLGH17837Entity_.intVal1), cb2.literal(9)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (4 BETWEEN INTVAL1 AND 9)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            cquery3.where(cb3.between(cb3.literal(4), root3.get(OLGH17837Entity_.intVal1), intParam3));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (4 BETWEEN INTVAL1 AND ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));
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

    public void testBetween2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 BETWEEN s.intVal1 AND ?2");
            query.setParameter(1, 4);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 BETWEEN s.intVal1 AND 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (4 BETWEEN INTVAL1 AND 9)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 BETWEEN s.intVal1 AND ?1");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (4 BETWEEN INTVAL1 AND ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.where(cb.between(intParam1, root.get(OLGH17837Entity_.intVal1), intParam2));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 4);
            query.setParameter(intParam2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.between(cb2.literal(4), root2.get(OLGH17837Entity_.intVal1), cb2.literal(9)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (4 BETWEEN INTVAL1 AND 9)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            cquery3.where(cb3.between(cb3.literal(4), root3.get(OLGH17837Entity_.intVal1), intParam3));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (4 BETWEEN INTVAL1 AND ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));
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

    public void testBetween2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 BETWEEN s.intVal1 AND ?2");
            query.setParameter(1, 4);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 BETWEEN s.intVal1 AND 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 BETWEEN s.intVal1 AND ?1");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.where(cb.between(intParam1, root.get(OLGH17837Entity_.intVal1), intParam2));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 4);
            query.setParameter(intParam2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.between(cb2.literal(4), root2.get(OLGH17837Entity_.intVal1), cb2.literal(9)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            cquery3.where(cb3.between(cb3.literal(4), root3.get(OLGH17837Entity_.intVal1), intParam3));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN INTVAL1 AND ?)", sql.remove(0));
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

    public void testBetween3_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 BETWEEN ?2 AND ?3");
            query.setParameter(1, 4);
            query.setParameter(2, 2);
            query.setParameter(3, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (4 BETWEEN 2 AND 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 BETWEEN 2 AND 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (4 BETWEEN 2 AND 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 BETWEEN ?1 AND ?2");
            query.setParameter(1, 2);
            query.setParameter(2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (4 BETWEEN 2 AND 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.where(cb.between(intParam1, intParam2, intParam3));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 4);
            query.setParameter(intParam2, 2);
            query.setParameter(intParam3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (4 BETWEEN 2 AND 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.between(cb2.literal(4), cb2.literal(2), cb2.literal(9)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (4 BETWEEN 2 AND 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND ?)", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam4 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.where(cb3.between(cb3.literal(4), intParam4, intParam5));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 2);
            query.setParameter(intParam5, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (4 BETWEEN 2 AND 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND ?)", sql.remove(0));
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

    public void testBetween3_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 BETWEEN ?2 AND ?3");
            query.setParameter(1, 4);
            query.setParameter(2, 2);
            query.setParameter(3, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 BETWEEN 2 AND 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND 9)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND ?)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (4 BETWEEN 2 AND 9)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND ?)", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 BETWEEN ?1 AND ?2");
            query.setParameter(1, 2);
            query.setParameter(2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND 9)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND ?)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (4 BETWEEN ? AND ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND ?)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.where(cb.between(intParam1, intParam2, intParam3));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 4);
            query.setParameter(intParam2, 2);
            query.setParameter(intParam3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.between(cb2.literal(4), cb2.literal(2), cb2.literal(9)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND 9)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND ?)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (4 BETWEEN 2 AND 9)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND ?)", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam4 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.where(cb3.between(cb3.literal(4), intParam4, intParam5));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 2);
            query.setParameter(intParam5, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND 9)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND ?)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (4 BETWEEN ? AND ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND ?)", sql.remove(0));
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

    public void testBetween3_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 BETWEEN ?2 AND ?3");
            query.setParameter(1, 4);
            query.setParameter(2, 2);
            query.setParameter(3, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 BETWEEN 2 AND 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 BETWEEN ?1 AND ?2");
            query.setParameter(1, 2);
            query.setParameter(2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.where(cb.between(intParam1, intParam2, intParam3));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 4);
            query.setParameter(intParam2, 2);
            query.setParameter(intParam3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.between(cb2.literal(4), cb2.literal(2), cb2.literal(9)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND ?)", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam4 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.where(cb3.between(cb3.literal(4), intParam4, intParam5));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 2);
            query.setParameter(intParam5, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND 9)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? BETWEEN ? AND ?)", sql.remove(0));
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

    public void testNotBetween1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT BETWEEN ?1 AND ?2");
            query.setParameter(1, 0);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT BETWEEN 0 AND 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN 0 AND 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT BETWEEN 0 AND ?1");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN 0 AND ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.where(cb.between(root.get(OLGH17837Entity_.intVal1), intParam1, intParam2).not());

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 0);
            query.setParameter(intParam2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.between(root2.get(OLGH17837Entity_.intVal1), 0, 9).not());

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN 0 AND 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            cquery3.where(cb3.between(root3.get(OLGH17837Entity_.intVal1), cb3.literal(0), intParam3).not());

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN 0 AND ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));
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

    public void testNotBetween1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT BETWEEN ?1 AND ?2");
            query.setParameter(1, 0);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT BETWEEN 0 AND 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN 0 AND 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT BETWEEN 0 AND ?1");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN 0 AND ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.where(cb.between(root.get(OLGH17837Entity_.intVal1), intParam1, intParam2).not());

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 0);
            query.setParameter(intParam2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.between(root2.get(OLGH17837Entity_.intVal1), 0, 9).not());

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN 0 AND 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            cquery3.where(cb3.between(root3.get(OLGH17837Entity_.intVal1), cb3.literal(0), intParam3).not());

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN 0 AND ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));
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

    public void testNotBetween1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT BETWEEN ?1 AND ?2");
            query.setParameter(1, 0);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT BETWEEN 0 AND 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 NOT BETWEEN 0 AND ?1");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.where(cb.between(root.get(OLGH17837Entity_.intVal1), intParam1, intParam2).not());

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 0);
            query.setParameter(intParam2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.between(root2.get(OLGH17837Entity_.intVal1), 0, 9).not());

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            cquery3.where(cb3.between(root3.get(OLGH17837Entity_.intVal1), cb3.literal(0), intParam3).not());

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((INTVAL1 BETWEEN ? AND ?))", sql.remove(0));
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

    public void testNotBetween2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 NOT BETWEEN s.intVal1 AND ?2");
            query.setParameter(1, 4);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 NOT BETWEEN s.intVal1 AND 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((4 BETWEEN INTVAL1 AND 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 NOT BETWEEN s.intVal1 AND ?1");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((4 BETWEEN INTVAL1 AND ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.where(cb.not(cb.between(intParam1, root.get(OLGH17837Entity_.intVal1), intParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 4);
            query.setParameter(intParam2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.not(cb2.between(cb2.literal(4), root2.get(OLGH17837Entity_.intVal1), cb2.literal(9))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((4 BETWEEN INTVAL1 AND 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            cquery3.where(cb3.not(cb3.between(cb3.literal(4), root3.get(OLGH17837Entity_.intVal1), intParam3)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((4 BETWEEN INTVAL1 AND ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));
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

    public void testNotBetween2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 NOT BETWEEN s.intVal1 AND ?2");
            query.setParameter(1, 4);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 NOT BETWEEN s.intVal1 AND 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((4 BETWEEN INTVAL1 AND 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 NOT BETWEEN s.intVal1 AND ?1");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((4 BETWEEN INTVAL1 AND ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.where(cb.not(cb.between(intParam1, root.get(OLGH17837Entity_.intVal1), intParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 4);
            query.setParameter(intParam2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.not(cb2.between(cb2.literal(4), root2.get(OLGH17837Entity_.intVal1), cb2.literal(9))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((4 BETWEEN INTVAL1 AND 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            cquery3.where(cb3.not(cb3.between(cb3.literal(4), root3.get(OLGH17837Entity_.intVal1), intParam3)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((4 BETWEEN INTVAL1 AND ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));
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

    public void testNotBetween2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 NOT BETWEEN s.intVal1 AND ?2");
            query.setParameter(1, 4);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 NOT BETWEEN s.intVal1 AND 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 NOT BETWEEN s.intVal1 AND ?1");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.where(cb.not(cb.between(intParam1, root.get(OLGH17837Entity_.intVal1), intParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 4);
            query.setParameter(intParam2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.not(cb2.between(cb2.literal(4), root2.get(OLGH17837Entity_.intVal1), cb2.literal(9))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            cquery3.where(cb3.not(cb3.between(cb3.literal(4), root3.get(OLGH17837Entity_.intVal1), intParam3)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN INTVAL1 AND ?))", sql.remove(0));
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

    public void testNotBetween3_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 NOT BETWEEN ?2 AND ?3");
            query.setParameter(1, 4);
            query.setParameter(2, 2);
            query.setParameter(3, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((4 BETWEEN 2 AND 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 NOT BETWEEN 2 AND 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((4 BETWEEN 2 AND 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 NOT BETWEEN ?1 AND ?2");
            query.setParameter(1, 2);
            query.setParameter(2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((4 BETWEEN 2 AND 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.where(cb.between(intParam1, intParam2, intParam3).not());

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 4);
            query.setParameter(intParam2, 2);
            query.setParameter(intParam3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((4 BETWEEN 2 AND 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.between(cb2.literal(4), cb2.literal(2), cb2.literal(9)).not());

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((4 BETWEEN 2 AND 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam4 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.where(cb3.between(cb3.literal(4), intParam4, intParam5).not());

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 2);
            query.setParameter(intParam5, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((4 BETWEEN 2 AND 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND ?))", sql.remove(0));
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

    public void testNotBetween3_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 NOT BETWEEN ?2 AND ?3");
            query.setParameter(1, 4);
            query.setParameter(2, 2);
            query.setParameter(3, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 NOT BETWEEN 2 AND 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((4 BETWEEN 2 AND 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 NOT BETWEEN ?1 AND ?2");
            query.setParameter(1, 2);
            query.setParameter(2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((4 BETWEEN ? AND ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.where(cb.between(intParam1, intParam2, intParam3).not());

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 4);
            query.setParameter(intParam2, 2);
            query.setParameter(intParam3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.between(cb2.literal(4), cb2.literal(2), cb2.literal(9)).not());

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((4 BETWEEN 2 AND 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam4 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.where(cb3.between(cb3.literal(4), intParam4, intParam5).not());

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 2);
            query.setParameter(intParam5, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND 9))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((4 BETWEEN ? AND ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND ?))", sql.remove(0));
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

    public void testNotBetween3_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.DERBY);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 NOT BETWEEN ?2 AND ?3");
            query.setParameter(1, 4);
            query.setParameter(2, 2);
            query.setParameter(3, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 NOT BETWEEN 2 AND 9");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 4 NOT BETWEEN ?1 AND ?2");
            query.setParameter(1, 2);
            query.setParameter(2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.where(cb.between(intParam1, intParam2, intParam3).not());

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 4);
            query.setParameter(intParam2, 2);
            query.setParameter(intParam3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND ?))", sql.remove(0));
            }

            // CASE test #1 with CriteriaBuilder literal values
            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.between(cb2.literal(4), cb2.literal(2), cb2.literal(9)).not());

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intParam4 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.where(cb3.between(cb3.literal(4), intParam4, intParam5).not());

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 2);
            query.setParameter(intParam5, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND 9))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE NOT ((? BETWEEN ? AND ?))", sql.remove(0));
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

    public void testIsNull1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 IS NULL");
            query.setParameter(1, "HELLO");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NULL)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NULL)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 'HELLO' IS NULL");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NULL)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NULL)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.where(cb.isNull(strParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NULL)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NULL)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.isNull(cb2.literal("HELLO")));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NULL)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NULL)", sql.remove(0));
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

    public void testIsNull1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 IS NULL");
            query.setParameter(1, "HELLO");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NULL)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NULL)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 'HELLO' IS NULL");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NULL)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NULL)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NULL)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NULL)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.where(cb.isNull(strParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NULL)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NULL)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.isNull(cb2.literal("HELLO")));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NULL)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NULL)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NULL)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NULL)", sql.remove(0));
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

    public void testIsNull1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 IS NULL");
            query.setParameter(1, "HELLO");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NULL)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NULL)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 'HELLO' IS NULL");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NULL)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NULL)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.where(cb.isNull(strParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NULL)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NULL)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.isNull(cb2.literal("HELLO")));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NULL)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NULL)", sql.remove(0));
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

    public void testIsNotNull1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 IS NOT NULL");
            query.setParameter(1, "HELLO");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NOT NULL)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NOT NULL)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 'HELLO' IS NOT NULL");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NOT NULL)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NOT NULL)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.where(cb.isNotNull(strParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NOT NULL)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NOT NULL)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.isNotNull(cb2.literal("HELLO")));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NOT NULL)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NOT NULL)", sql.remove(0));
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

    public void testIsNotNull1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 IS NOT NULL");
            query.setParameter(1, "HELLO");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NOT NULL)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NOT NULL)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 'HELLO' IS NOT NULL");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NOT NULL)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NOT NULL)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NOT NULL)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NOT NULL)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.where(cb.isNotNull(strParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NOT NULL)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NOT NULL)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.isNotNull(cb2.literal("HELLO")));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NOT NULL)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NOT NULL)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NOT NULL)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NOT NULL)", sql.remove(0));
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

    public void testIsNotNull1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 IS NOT NULL");
            query.setParameter(1, "HELLO");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NOT NULL)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NOT NULL)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 'HELLO' IS NOT NULL");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NOT NULL)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NOT NULL)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.where(cb.isNotNull(strParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NOT NULL)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NOT NULL)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            cquery2.where(cb2.isNotNull(cb2.literal("HELLO")));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE ('HELLO' IS NOT NULL)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? IS NOT NULL)", sql.remove(0));
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

    public void testExists1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = ?1)");
            query.setParameter(1, "HELLO");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = 'HELLO')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strValue1 = cb.parameter(String.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.strVal1), strValue1));

            cquery.where(cb.exists(subquery));

            query = em.createQuery(cquery);
            query.setParameter(strValue1, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.strVal1), "HELLO"));

            cquery2.where(cb2.exists(subquery2));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO'))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
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

    public void testExists1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = ?1)");
            query.setParameter(1, "HELLO");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = 'HELLO')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO'))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strValue1 = cb.parameter(String.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.strVal1), strValue1));

            cquery.where(cb.exists(subquery));

            query = em.createQuery(cquery);
            query.setParameter(strValue1, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.strVal1), "HELLO"));

            cquery2.where(cb2.exists(subquery2));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO'))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
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

    public void testExists1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = ?1)");
            query.setParameter(1, "HELLO");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = 'HELLO')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strValue1 = cb.parameter(String.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.strVal1), strValue1));

            cquery.where(cb.exists(subquery));

            query = em.createQuery(cquery);
            query.setParameter(strValue1, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.strVal1), "HELLO"));

            cquery2.where(cb2.exists(subquery2));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
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

    public void testExists2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT CASE WHEN EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = ?1) THEN ?2 ELSE ?3 END FROM OLGH17837Entity s");
            query.setParameter(1, "HELLO");
            query.setParameter(2, true);
            query.setParameter(3, false);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE  WHEN EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO')) THEN 1 ELSE 0 END FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE  WHEN EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            }

            query = em.createQuery("SELECT CASE WHEN EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = 'HELLO') THEN TRUE ELSE FALSE END FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE  WHEN EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO')) THEN 1 ELSE 0 END FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE  WHEN EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);

            ParameterExpression<String> strValue1 = cb.parameter(String.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.strVal1), strValue1));

            ParameterExpression<Boolean> resultParam1 = cb.parameter(Boolean.class);
            ParameterExpression<Boolean> resultParam2 = cb.parameter(Boolean.class);

            Expression<Object> selectCase = cb.selectCase()
                            .when(cb.exists(subquery), resultParam1)
                            .otherwise(resultParam2);

            cquery.select(selectCase);

            query = em.createQuery(cquery);
            query.setParameter(strValue1, "HELLO");
            query.setParameter(resultParam1, true);
            query.setParameter(resultParam2, false);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE WHEN EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO')) THEN 1 ELSE 0 END  FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE WHEN EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END  FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.strVal1), "HELLO"));

            Expression<Object> selectCase2 = cb2.selectCase()
                            .when(cb2.exists(subquery2), true)
                            .otherwise(false);

            cquery2.select(selectCase2);

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE WHEN EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO')) THEN 1 ELSE 0 END  FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE WHEN EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END  FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
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

    public void testExists2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT CASE WHEN EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = ?1) THEN ?2 ELSE ?3 END FROM OLGH17837Entity s");
            query.setParameter(1, "HELLO");
            query.setParameter(2, true);
            query.setParameter(3, false);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE  WHEN EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN 1 ELSE 0 END FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE  WHEN EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            }

            query = em.createQuery("SELECT CASE WHEN EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = 'HELLO') THEN TRUE ELSE FALSE END FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE  WHEN EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN 1 ELSE 0 END FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE  WHEN EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE  WHEN EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO')) THEN 1 ELSE 0 END FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE  WHEN EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);

            ParameterExpression<String> strValue1 = cb.parameter(String.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.strVal1), strValue1));

            ParameterExpression<Boolean> resultParam1 = cb.parameter(Boolean.class);
            ParameterExpression<Boolean> resultParam2 = cb.parameter(Boolean.class);

            Expression<Object> selectCase = cb.selectCase()
                            .when(cb.exists(subquery), resultParam1)
                            .otherwise(resultParam2);

            cquery.select(selectCase);

            query = em.createQuery(cquery);
            query.setParameter(strValue1, "HELLO");
            query.setParameter(resultParam1, true);
            query.setParameter(resultParam2, false);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE WHEN EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN 1 ELSE 0 END  FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE WHEN EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END  FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.strVal1), "HELLO"));

            Expression<Object> selectCase2 = cb2.selectCase()
                            .when(cb2.exists(subquery2), true)
                            .otherwise(false);

            cquery2.select(selectCase2);

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE WHEN EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN 1 ELSE 0 END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE WHEN EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE WHEN EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO')) THEN 1 ELSE 0 END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE WHEN EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
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

    public void testExists2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT CASE WHEN EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = ?1) THEN ?2 ELSE ?3 END FROM OLGH17837Entity s");
            query.setParameter(1, "HELLO");
            query.setParameter(2, true);
            query.setParameter(3, false);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE  WHEN EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN 1 ELSE 0 END FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE  WHEN EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            }

            query = em.createQuery("SELECT CASE WHEN EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = 'HELLO') THEN TRUE ELSE FALSE END FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE  WHEN EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN 1 ELSE 0 END FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE  WHEN EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);

            ParameterExpression<String> strValue1 = cb.parameter(String.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.strVal1), strValue1));

            ParameterExpression<Boolean> resultParam1 = cb.parameter(Boolean.class);
            ParameterExpression<Boolean> resultParam2 = cb.parameter(Boolean.class);

            Expression<Object> selectCase = cb.selectCase()
                            .when(cb.exists(subquery), resultParam1)
                            .otherwise(resultParam2);

            cquery.select(selectCase);

            query = em.createQuery(cquery);
            query.setParameter(strValue1, "HELLO");
            query.setParameter(resultParam1, true);
            query.setParameter(resultParam2, false);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE WHEN EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN 1 ELSE 0 END  FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE WHEN EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END  FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.strVal1), "HELLO"));

            Expression<Object> selectCase2 = cb2.selectCase()
                            .when(cb2.exists(subquery2), true)
                            .otherwise(false);

            cquery2.select(selectCase2);

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE WHEN EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN 1 ELSE 0 END  FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE WHEN EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END  FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
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

    public void testNotExists1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE NOT EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = ?1)");
            query.setParameter(1, "HELLO");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO'))",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE NOT EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = 'HELLO')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO'))",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strValue1 = cb.parameter(String.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.strVal1), strValue1));

            cquery.where(cb.not(cb.exists(subquery)));

            query = em.createQuery(cquery);
            query.setParameter(strValue1, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA22Feature() || isUsingJPA30Feature() || isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)))",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.strVal1), "HELLO"));

            cquery2.where(cb2.not(cb2.exists(subquery2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)))",
                                    sql.remove(0));
            } else if (isUsingJPA22Feature() || isUsingJPA30Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO')))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)))",
                                        sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO'))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))",
                                        sql.remove(0));
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

    public void testNotExists1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE NOT EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = ?1)");
            query.setParameter(1, "HELLO");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE NOT EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = 'HELLO')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO'))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strValue1 = cb.parameter(String.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.strVal1), strValue1));

            cquery.where(cb.not(cb.exists(subquery)));

            query = em.createQuery(cquery);
            query.setParameter(strValue1, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA22Feature() || isUsingJPA30Feature() || isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)))",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.strVal1), "HELLO"));

            cquery2.where(cb2.not(cb2.exists(subquery2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)))",
                                    sql.remove(0));
            } else if (isUsingJPA22Feature() || isUsingJPA30Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO')))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)))",
                                        sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO'))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))",
                                        sql.remove(0));
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

    public void testNotExists1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE NOT EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = ?1)");
            query.setParameter(1, "HELLO");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE NOT EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = 'HELLO')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<String> strValue1 = cb.parameter(String.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.strVal1), strValue1));

            cquery.where(cb.not(cb.exists(subquery)));

            query = em.createQuery(cquery);
            query.setParameter(strValue1, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA22Feature() || isUsingJPA30Feature() || isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)))",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.strVal1), "HELLO"));

            cquery2.where(cb2.not(cb2.exists(subquery2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA22Feature() || isUsingJPA30Feature() || isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)))",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE NOT EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))", sql.remove(0));
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

    public void testNotExists2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT CASE WHEN NOT EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = ?1) THEN ?2 ELSE ?3 END FROM OLGH17837Entity s");
            query.setParameter(1, "HELLO");
            query.setParameter(2, true);
            query.setParameter(3, false);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE  WHEN NOT EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO')) THEN 1 ELSE 0 END FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE  WHEN NOT EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            }

            query = em.createQuery("SELECT CASE WHEN NOT EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = 'HELLO') THEN TRUE ELSE FALSE END FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE  WHEN NOT EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO')) THEN 1 ELSE 0 END FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE  WHEN NOT EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);

            ParameterExpression<String> strValue1 = cb.parameter(String.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.strVal1), strValue1));

            ParameterExpression<Boolean> resultParam1 = cb.parameter(Boolean.class);
            ParameterExpression<Boolean> resultParam2 = cb.parameter(Boolean.class);

            Expression<Object> selectCase = cb.selectCase()
                            .when(cb.not(cb.exists(subquery)), resultParam1)
                            .otherwise(resultParam2);

            cquery.select(selectCase);

            query = em.createQuery(cquery);
            query.setParameter(strValue1, "HELLO");
            query.setParameter(resultParam1, true);
            query.setParameter(resultParam2, false);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA22Feature() || isUsingJPA30Feature() || isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE WHEN NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO'))) THEN 1 ELSE 0 END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE WHEN NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))) THEN ? ELSE ? END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE WHEN NOT EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO')) THEN 1 ELSE 0 END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE WHEN NOT EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                }
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.strVal1), "HELLO"));

            Expression<Object> selectCase2 = cb2.selectCase()
                            .when(cb2.not(cb2.exists(subquery2)), true)
                            .otherwise(false);

            cquery2.select(selectCase2);

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA22Feature() || isUsingJPA30Feature() || isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE WHEN NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO'))) THEN 1 ELSE 0 END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE WHEN NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))) THEN ? ELSE ? END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE WHEN NOT EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO')) THEN 1 ELSE 0 END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE WHEN NOT EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
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

    public void testNotExists2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT CASE WHEN NOT EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = ?1) THEN ?2 ELSE ?3 END FROM OLGH17837Entity s");
            query.setParameter(1, "HELLO");
            query.setParameter(2, true);
            query.setParameter(3, false);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE  WHEN NOT EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN 1 ELSE 0 END FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE  WHEN NOT EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            }

            query = em.createQuery("SELECT CASE WHEN NOT EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = 'HELLO') THEN TRUE ELSE FALSE END FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE  WHEN NOT EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN 1 ELSE 0 END FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE  WHEN NOT EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE  WHEN NOT EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO')) THEN 1 ELSE 0 END FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE  WHEN NOT EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);

            ParameterExpression<String> strValue1 = cb.parameter(String.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.strVal1), strValue1));

            ParameterExpression<Boolean> resultParam1 = cb.parameter(Boolean.class);
            ParameterExpression<Boolean> resultParam2 = cb.parameter(Boolean.class);

            Expression<Object> selectCase = cb.selectCase()
                            .when(cb.not(cb.exists(subquery)), resultParam1)
                            .otherwise(resultParam2);

            cquery.select(selectCase);

            query = em.createQuery(cquery);
            query.setParameter(strValue1, "HELLO");
            query.setParameter(resultParam1, true);
            query.setParameter(resultParam2, false);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA22Feature() || isUsingJPA30Feature() || isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE WHEN NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))) THEN 1 ELSE 0 END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE WHEN NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))) THEN ? ELSE ? END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE WHEN NOT EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN 1 ELSE 0 END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE WHEN NOT EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                }
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.strVal1), "HELLO"));

            Expression<Object> selectCase2 = cb2.selectCase()
                            .when(cb2.not(cb2.exists(subquery2)), true)
                            .otherwise(false);

            cquery2.select(selectCase2);

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE WHEN NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))) THEN 1 ELSE 0 END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE WHEN NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))) THEN ? ELSE ? END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                }
            } else if (isUsingJPA22Feature() || isUsingJPA30Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE WHEN NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO'))) THEN 1 ELSE 0 END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE WHEN NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))) THEN ? ELSE ? END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE WHEN NOT EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = 'HELLO')) THEN 1 ELSE 0 END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE WHEN NOT EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
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

    public void testNotExists2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT CASE WHEN NOT EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = ?1) THEN ?2 ELSE ?3 END FROM OLGH17837Entity s");
            query.setParameter(1, "HELLO");
            query.setParameter(2, true);
            query.setParameter(3, false);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE  WHEN NOT EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN 1 ELSE 0 END FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE  WHEN NOT EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            }

            query = em.createQuery("SELECT CASE WHEN NOT EXISTS (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.strVal1 = 'HELLO') THEN TRUE ELSE FALSE END FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE  WHEN NOT EXISTS (SELECT 1 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN 1 ELSE 0 END FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE  WHEN NOT EXISTS (SELECT ? FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END FROM OLGH17837ENTITY t0",
                                    sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery = cb.createQuery(Object.class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);

            ParameterExpression<String> strValue1 = cb.parameter(String.class);
            Subquery<Integer> subquery = cquery.subquery(Integer.class);
            Root<OLGH17837Entity> subroot = subquery.from(OLGH17837Entity.class);
            subquery.select(subroot.get(OLGH17837Entity_.intVal2));
            subquery.where(cb.equal(subroot.get(OLGH17837Entity_.strVal1), strValue1));

            ParameterExpression<Boolean> resultParam1 = cb.parameter(Boolean.class);
            ParameterExpression<Boolean> resultParam2 = cb.parameter(Boolean.class);

            Expression<Object> selectCase = cb.selectCase()
                            .when(cb.not(cb.exists(subquery)), resultParam1)
                            .otherwise(resultParam2);

            cquery.select(selectCase);

            query = em.createQuery(cquery);
            query.setParameter(strValue1, "HELLO");
            query.setParameter(resultParam1, true);
            query.setParameter(resultParam2, false);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA22Feature() || isUsingJPA30Feature() || isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE WHEN NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))) THEN 1 ELSE 0 END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE WHEN NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))) THEN ? ELSE ? END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE WHEN NOT EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN 1 ELSE 0 END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE WHEN NOT EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                }
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object> cquery2 = cb2.createQuery(Object.class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.strVal1), "HELLO"));

            Expression<Object> selectCase2 = cb2.selectCase()
                            .when(cb2.not(cb2.exists(subquery2)), true)
                            .otherwise(false);

            cquery2.select(selectCase2);

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA22Feature() || isUsingJPA30Feature() || isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE WHEN NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))) THEN 1 ELSE 0 END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE WHEN NOT (EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?))) THEN ? ELSE ? END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE WHEN NOT EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN 1 ELSE 0 END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE WHEN NOT EXISTS (SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.STRVAL1 = ?)) THEN ? ELSE ? END  FROM OLGH17837ENTITY t0",
                                        sql.remove(0));
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

    public void testCase1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = CASE s.intVal2 WHEN ?1 THEN ?2 WHEN ?3 THEN ?4 ELSE ?5 END");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 15);
            query.setParameter(4, 16);
            query.setParameter(5, 26);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN 5 THEN 6 WHEN 15 THEN 16 ELSE 26 END)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = CASE s.intVal2 WHEN 5 THEN 6 WHEN 15 THEN 16 ELSE 26 END");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN 5 THEN 6 WHEN 15 THEN 16 ELSE 26 END)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = CASE s.intVal2 WHEN ?1 THEN ?2 WHEN 15 THEN 16 ELSE 26 END");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN 5 THEN 6 WHEN 15 THEN 16 ELSE 26 END)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Expression<Object> selectCase2 = cb2.selectCase(root2.get(OLGH17837Entity_.intVal2))
                            .when(5, 6)
                            .when(15, 16)
                            .otherwise(26);

            Predicate pred2 = cb2.equal(root2.get(OLGH17837Entity_.intVal1), selectCase2);
            cquery2.where(pred2);

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN 5 THEN 6 WHEN 15 THEN 16 ELSE 26 END)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END)", sql.remove(0));
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

    public void testCase1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = CASE s.intVal2 WHEN ?1 THEN ?2 WHEN ?3 THEN ?4 ELSE ?5 END");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 15);
            query.setParameter(4, 16);
            query.setParameter(5, 26);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN 6 WHEN ? THEN 16 ELSE 26 END)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = CASE s.intVal2 WHEN 5 THEN 6 WHEN 15 THEN 16 ELSE 26 END");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN 6 WHEN ? THEN 16 ELSE 26 END)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN 5 THEN 6 WHEN 15 THEN 16 ELSE 26 END)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END)", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = CASE s.intVal2 WHEN ?1 THEN ?2 WHEN 15 THEN 16 ELSE 26 END");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN 6 WHEN ? THEN 16 ELSE 26 END)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN 6 WHEN 15 THEN 16 ELSE 26 END)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Expression<Object> selectCase2 = cb2.selectCase(root2.get(OLGH17837Entity_.intVal2))
                            .when(5, 6)
                            .when(15, 16)
                            .otherwise(26);

            Predicate pred2 = cb2.equal(root2.get(OLGH17837Entity_.intVal1), selectCase2);
            cquery2.where(pred2);

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN 6 WHEN ? THEN 16 ELSE 26 END)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN 5 THEN 6 WHEN 15 THEN 16 ELSE 26 END)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END)", sql.remove(0));
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

    public void testCase1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = CASE s.intVal2 WHEN ?1 THEN ?2 WHEN ?3 THEN ?4 ELSE ?5 END");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 15);
            query.setParameter(4, 16);
            query.setParameter(5, 26);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN 6 WHEN ? THEN 16 ELSE 26 END)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = CASE s.intVal2 WHEN 5 THEN 6 WHEN 15 THEN 16 ELSE 26 END");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN 6 WHEN ? THEN 16 ELSE 26 END)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = CASE s.intVal2 WHEN ?1 THEN ?2 WHEN 15 THEN 16 ELSE 26 END");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN 6 WHEN ? THEN 16 ELSE 26 END)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Expression<Object> selectCase2 = cb2.selectCase(root2.get(OLGH17837Entity_.intVal2))
                            .when(5, 6)
                            .when(15, 16)
                            .otherwise(26);

            Predicate pred2 = cb2.equal(root2.get(OLGH17837Entity_.intVal1), selectCase2);
            cquery2.where(pred2);

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN 6 WHEN ? THEN 16 ELSE 26 END)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END)", sql.remove(0));
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

    public void testCase2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = CASE WHEN s.intVal2 = ?1 THEN ?2 WHEN s.intVal2 = ?3 THEN ?4 ELSE ?5 END");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 15);
            query.setParameter(4, 16);
            query.setParameter(5, 26);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE  WHEN (INTVAL2 = 5) THEN 6 WHEN (INTVAL2 = 15) THEN 16 ELSE 26 END)",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE  WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = CASE WHEN s.intVal2 = 5 THEN 6 WHEN s.intVal2 = 15 THEN 16 ELSE 26 END");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE  WHEN (INTVAL2 = 5) THEN 6 WHEN (INTVAL2 = 15) THEN 16 ELSE 26 END)",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE  WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = CASE WHEN s.intVal2 = ?1 THEN ?2 WHEN s.intVal2 = 15 THEN 16 ELSE 26 END");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE  WHEN (INTVAL2 = 5) THEN 6 WHEN (INTVAL2 = 15) THEN 16 ELSE 26 END)",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE  WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> checkParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> checkParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam3 = cb.parameter(Integer.class);

            Expression<Object> selectCase = cb.selectCase()
                            .when(cb.equal(root.get(OLGH17837Entity_.intVal2), checkParam1), resultParam1)
                            .when(cb.equal(root.get(OLGH17837Entity_.intVal2), checkParam2), resultParam2)
                            .otherwise(resultParam3);

            Predicate pred = cb.equal(root.get(OLGH17837Entity_.intVal1), selectCase);
            cquery.where(pred);

            query = em.createQuery(cquery);
            query.setParameter(checkParam1, 5);
            query.setParameter(resultParam1, 6);
            query.setParameter(checkParam2, 15);
            query.setParameter(resultParam2, 16);
            query.setParameter(resultParam3, 26);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE WHEN (INTVAL2 = 5) THEN 6 WHEN (INTVAL2 = 15) THEN 16 ELSE 26 END )",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END )", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Expression<Object> selectCase2 = cb2.selectCase()
                            .when(cb2.equal(root2.get(OLGH17837Entity_.intVal2), 5), 6)
                            .when(cb2.equal(root2.get(OLGH17837Entity_.intVal2), 15), 16)
                            .otherwise(26);

            Predicate pred2 = cb2.equal(root2.get(OLGH17837Entity_.intVal1), selectCase2);
            cquery2.where(pred2);

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE WHEN (INTVAL2 = 5) THEN 6 WHEN (INTVAL2 = 15) THEN 16 ELSE 26 END )",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END )", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> checkParam3 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> resultParam4 = cb3.parameter(Integer.class);

            Expression<Object> selectCase3 = cb3.selectCase()
                            .when(cb3.equal(root3.get(OLGH17837Entity_.intVal2), checkParam3), resultParam4)
                            .when(cb3.equal(root3.get(OLGH17837Entity_.intVal2), 15), 16)
                            .otherwise(26);

            Predicate pred3 = cb3.equal(root3.get(OLGH17837Entity_.intVal1), selectCase3);
            cquery3.where(pred3);

            query = em.createQuery(cquery3);
            query.setParameter(checkParam3, 5);
            query.setParameter(resultParam4, 6);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE WHEN (INTVAL2 = 5) THEN 6 WHEN (INTVAL2 = 15) THEN 16 ELSE 26 END )",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END )", sql.remove(0));
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

    public void testCase2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = CASE WHEN s.intVal2 = ?1 THEN ?2 WHEN s.intVal2 = ?3 THEN ?4 ELSE ?5 END");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 15);
            query.setParameter(4, 16);
            query.setParameter(5, 26);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE  WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END)",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE  WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = CASE WHEN s.intVal2 = 5 THEN 6 WHEN s.intVal2 = 15 THEN 16 ELSE 26 END");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE  WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END)",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE  WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END)",
                                        sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE  WHEN (INTVAL2 = 5) THEN 6 WHEN (INTVAL2 = 15) THEN 16 ELSE 26 END)",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE  WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END)",
                                        sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = CASE WHEN s.intVal2 = ?1 THEN ?2 WHEN s.intVal2 = 15 THEN 16 ELSE 26 END");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE  WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END)",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE  WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END)",
                                        sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE  WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = 15) THEN 16 ELSE 26 END)",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE  WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END)",
                                        sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> checkParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> checkParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam3 = cb.parameter(Integer.class);

            Expression<Object> selectCase = cb.selectCase()
                            .when(cb.equal(root.get(OLGH17837Entity_.intVal2), checkParam1), resultParam1)
                            .when(cb.equal(root.get(OLGH17837Entity_.intVal2), checkParam2), resultParam2)
                            .otherwise(resultParam3);

            Predicate pred = cb.equal(root.get(OLGH17837Entity_.intVal1), selectCase);
            cquery.where(pred);

            query = em.createQuery(cquery);
            query.setParameter(checkParam1, 5);
            query.setParameter(resultParam1, 6);
            query.setParameter(checkParam2, 15);
            query.setParameter(resultParam2, 16);
            query.setParameter(resultParam3, 26);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END )",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END )", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Expression<Object> selectCase2 = cb2.selectCase()
                            .when(cb2.equal(root2.get(OLGH17837Entity_.intVal2), 5), 6)
                            .when(cb2.equal(root2.get(OLGH17837Entity_.intVal2), 15), 16)
                            .otherwise(26);

            Predicate pred2 = cb2.equal(root2.get(OLGH17837Entity_.intVal1), selectCase2);
            cquery2.where(pred2);

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END )",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END )",
                                        sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE WHEN (INTVAL2 = 5) THEN 6 WHEN (INTVAL2 = 15) THEN 16 ELSE 26 END )",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END )",
                                        sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> checkParam3 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> resultParam4 = cb3.parameter(Integer.class);

            Expression<Object> selectCase3 = cb3.selectCase()
                            .when(cb3.equal(root3.get(OLGH17837Entity_.intVal2), checkParam3), resultParam4)
                            .when(cb3.equal(root3.get(OLGH17837Entity_.intVal2), 15), 16)
                            .otherwise(26);

            Predicate pred3 = cb3.equal(root3.get(OLGH17837Entity_.intVal1), selectCase3);
            cquery3.where(pred3);

            query = em.createQuery(cquery3);
            query.setParameter(checkParam3, 5);
            query.setParameter(resultParam4, 6);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END )",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END )",
                                        sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = 15) THEN 16 ELSE 26 END )",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END )",
                                        sql.remove(0));
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

    public void testCase2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = CASE WHEN s.intVal2 = ?1 THEN ?2 WHEN s.intVal2 = ?3 THEN ?4 ELSE ?5 END");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 15);
            query.setParameter(4, 16);
            query.setParameter(5, 26);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE  WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END)",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE  WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = CASE WHEN s.intVal2 = 5 THEN 6 WHEN s.intVal2 = 15 THEN 16 ELSE 26 END");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE  WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END)",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE  WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = CASE WHEN s.intVal2 = ?1 THEN ?2 WHEN s.intVal2 = 15 THEN 16 ELSE 26 END");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE  WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END)",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE  WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> checkParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> checkParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam3 = cb.parameter(Integer.class);

            Expression<Object> selectCase = cb.selectCase()
                            .when(cb.equal(root.get(OLGH17837Entity_.intVal2), checkParam1), resultParam1)
                            .when(cb.equal(root.get(OLGH17837Entity_.intVal2), checkParam2), resultParam2)
                            .otherwise(resultParam3);

            Predicate pred = cb.equal(root.get(OLGH17837Entity_.intVal1), selectCase);
            cquery.where(pred);

            query = em.createQuery(cquery);
            query.setParameter(checkParam1, 5);
            query.setParameter(resultParam1, 6);
            query.setParameter(checkParam2, 15);
            query.setParameter(resultParam2, 16);
            query.setParameter(resultParam3, 26);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END )",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END )", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Expression<Object> selectCase2 = cb2.selectCase()
                            .when(cb2.equal(root2.get(OLGH17837Entity_.intVal2), 5), 6)
                            .when(cb2.equal(root2.get(OLGH17837Entity_.intVal2), 15), 16)
                            .otherwise(26);

            Predicate pred2 = cb2.equal(root2.get(OLGH17837Entity_.intVal1), selectCase2);
            cquery2.where(pred2);

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END )",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END )", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> checkParam3 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> resultParam4 = cb3.parameter(Integer.class);

            Expression<Object> selectCase3 = cb3.selectCase()
                            .when(cb3.equal(root3.get(OLGH17837Entity_.intVal2), checkParam3), resultParam4)
                            .when(cb3.equal(root3.get(OLGH17837Entity_.intVal2), 15), 16)
                            .otherwise(26);

            Predicate pred3 = cb3.equal(root3.get(OLGH17837Entity_.intVal1), selectCase3);
            cquery3.where(pred3);

            query = em.createQuery(cquery3);
            query.setParameter(checkParam3, 5);
            query.setParameter(resultParam4, 6);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END )",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = CASE WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END )", sql.remove(0));
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

    public void testCase3_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT CASE s.intVal2 WHEN ?1 THEN ?2 WHEN ?3 THEN ?4 ELSE ?5 END FROM OLGH17837Entity s WHERE s.intVal1 = ?6");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 15);
            query.setParameter(4, 16);
            query.setParameter(5, 26);
            query.setParameter(6, 99);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE INTVAL2 WHEN 5 THEN 6 WHEN 15 THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = 99)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT CASE s.intVal2 WHEN 5 THEN 6 WHEN 15 THEN 16 ELSE 26 END FROM OLGH17837Entity s WHERE s.intVal1 = 99");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE INTVAL2 WHEN 5 THEN 6 WHEN 15 THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = 99)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT CASE s.intVal2 WHEN ?1 THEN ?2 WHEN 15 THEN 16 ELSE 26 END FROM OLGH17837Entity s WHERE s.intVal1 = ?3");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 99);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE INTVAL2 WHEN 5 THEN 6 WHEN 15 THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = 99)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);

            Expression<Object> selectCase2 = cb2.selectCase(root2.get(OLGH17837Entity_.intVal2))
                            .when(5, 6)
                            .when(15, 16)
                            .otherwise(26);
            cquery2.multiselect(selectCase2);

            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), 99));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE INTVAL2 WHEN 5 THEN 6 WHEN 15 THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = 99)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
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

    public void testCase3_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT CASE s.intVal2 WHEN ?1 THEN ?2 WHEN ?3 THEN ?4 ELSE ?5 END FROM OLGH17837Entity s WHERE s.intVal1 = ?6");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 15);
            query.setParameter(4, 16);
            query.setParameter(5, 26);
            query.setParameter(6, 99);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN 6 WHEN ? THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT CASE s.intVal2 WHEN 5 THEN 6 WHEN 15 THEN 16 ELSE 26 END FROM OLGH17837Entity s WHERE s.intVal1 = 99");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN 6 WHEN ? THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE INTVAL2 WHEN 5 THEN 6 WHEN 15 THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = 99)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT CASE s.intVal2 WHEN ?1 THEN ?2 WHEN 15 THEN 16 ELSE 26 END FROM OLGH17837Entity s WHERE s.intVal1 = ?3");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 99);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN 6 WHEN ? THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN 6 WHEN 15 THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);

            Expression<Object> selectCase2 = cb2.selectCase(root2.get(OLGH17837Entity_.intVal2))
                            .when(5, 6)
                            .when(15, 16)
                            .otherwise(26);
            cquery2.multiselect(selectCase2);

            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), 99));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN 6 WHEN ? THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE INTVAL2 WHEN 5 THEN 6 WHEN 15 THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = 99)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
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

    public void testCase3_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT CASE s.intVal2 WHEN ?1 THEN ?2 WHEN ?3 THEN ?4 ELSE ?5 END FROM OLGH17837Entity s WHERE s.intVal1 = ?6");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 15);
            query.setParameter(4, 16);
            query.setParameter(5, 26);
            query.setParameter(6, 99);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN 6 WHEN ? THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT CASE s.intVal2 WHEN 5 THEN 6 WHEN 15 THEN 16 ELSE 26 END FROM OLGH17837Entity s WHERE s.intVal1 = 99");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN 6 WHEN ? THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT CASE s.intVal2 WHEN ?1 THEN ?2 WHEN 15 THEN 16 ELSE 26 END FROM OLGH17837Entity s WHERE s.intVal1 = ?3");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 99);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN 6 WHEN ? THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);

            Expression<Object> selectCase2 = cb2.selectCase(root2.get(OLGH17837Entity_.intVal2))
                            .when(5, 6)
                            .when(15, 16)
                            .otherwise(26);
            cquery2.multiselect(selectCase2);

            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), 99));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN 6 WHEN ? THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE INTVAL2 WHEN ? THEN ? WHEN ? THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
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

    public void testCase4_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT CASE WHEN s.intVal2 = ?1 THEN ?2 WHEN s.intVal2 = ?3 THEN ?4 ELSE ?5 END FROM OLGH17837Entity s WHERE s.intVal1 = ?6");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 15);
            query.setParameter(4, 16);
            query.setParameter(5, 26);
            query.setParameter(6, 99);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE  WHEN (INTVAL2 = 5) THEN 6 WHEN (INTVAL2 = 15) THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = 99)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE  WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT CASE WHEN s.intVal2 = 5 THEN 6 WHEN s.intVal2 = 15 THEN 16 ELSE 26 END FROM OLGH17837Entity s WHERE s.intVal1 = 99");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE  WHEN (INTVAL2 = 5) THEN 6 WHEN (INTVAL2 = 15) THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = 99)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE  WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT CASE WHEN s.intVal2 = ?1 THEN ?2 WHEN s.intVal2 = 15 THEN 16 ELSE 26 END FROM OLGH17837Entity s WHERE s.intVal1 = ?3");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 99);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE  WHEN (INTVAL2 = 5) THEN 6 WHEN (INTVAL2 = 15) THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = 99)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE  WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);

            ParameterExpression<Integer> checkParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> checkParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam3 = cb.parameter(Integer.class);

            Expression<Object> selectCase = cb.selectCase()
                            .when(cb.equal(root.get(OLGH17837Entity_.intVal2), checkParam1), resultParam1)
                            .when(cb.equal(root.get(OLGH17837Entity_.intVal2), checkParam2), resultParam2)
                            .otherwise(resultParam3);
            cquery.multiselect(selectCase);

            ParameterExpression<Integer> checkParam3 = cb.parameter(Integer.class);
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), checkParam3));

            query = em.createQuery(cquery);
            query.setParameter(checkParam1, 5);
            query.setParameter(resultParam1, 6);
            query.setParameter(checkParam2, 15);
            query.setParameter(resultParam2, 16);
            query.setParameter(resultParam3, 26);
            query.setParameter(checkParam3, 99);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE WHEN (INTVAL2 = 5) THEN 6 WHEN (INTVAL2 = 15) THEN 16 ELSE 26 END  FROM OLGH17837ENTITY WHERE (INTVAL1 = 99)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END  FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);

            Expression<Object> selectCase2 = cb2.selectCase()
                            .when(cb2.equal(root2.get(OLGH17837Entity_.intVal2), 5), 6)
                            .when(cb2.equal(root2.get(OLGH17837Entity_.intVal2), 15), 16)
                            .otherwise(26);
            cquery2.multiselect(selectCase2);

            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), 99));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE WHEN (INTVAL2 = 5) THEN 6 WHEN (INTVAL2 = 15) THEN 16 ELSE 26 END  FROM OLGH17837ENTITY WHERE (INTVAL1 = 99)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END  FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);

            ParameterExpression<Integer> checkParam4 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> resultParam4 = cb3.parameter(Integer.class);

            Expression<Object> selectCase3 = cb3.selectCase()
                            .when(cb3.equal(root3.get(OLGH17837Entity_.intVal2), checkParam4), resultParam4)
                            .when(cb3.equal(root3.get(OLGH17837Entity_.intVal2), 15), 16)
                            .otherwise(26);
            cquery3.multiselect(selectCase3);

            ParameterExpression<Integer> checkParam5 = cb3.parameter(Integer.class);
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.intVal1), checkParam5));

            query = em.createQuery(cquery3);
            query.setParameter(checkParam4, 5);
            query.setParameter(resultParam4, 6);
            query.setParameter(checkParam5, 99);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE WHEN (INTVAL2 = 5) THEN 6 WHEN (INTVAL2 = 15) THEN 16 ELSE 26 END  FROM OLGH17837ENTITY WHERE (INTVAL1 = 99)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END  FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
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

    public void testCase4_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT CASE WHEN s.intVal2 = ?1 THEN ?2 WHEN s.intVal2 = ?3 THEN ?4 ELSE ?5 END FROM OLGH17837Entity s WHERE s.intVal1 = ?6");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 15);
            query.setParameter(4, 16);
            query.setParameter(5, 26);
            query.setParameter(6, 99);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE  WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE  WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT CASE WHEN s.intVal2 = 5 THEN 6 WHEN s.intVal2 = 15 THEN 16 ELSE 26 END FROM OLGH17837Entity s WHERE s.intVal1 = 99");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE  WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE  WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE  WHEN (INTVAL2 = 5) THEN 6 WHEN (INTVAL2 = 15) THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = 99)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE  WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT CASE WHEN s.intVal2 = ?1 THEN ?2 WHEN s.intVal2 = 15 THEN 16 ELSE 26 END FROM OLGH17837Entity s WHERE s.intVal1 = ?3");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 99);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE  WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE  WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE  WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = 15) THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE  WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);

            ParameterExpression<Integer> checkParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> checkParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam3 = cb.parameter(Integer.class);

            Expression<Object> selectCase = cb.selectCase()
                            .when(cb.equal(root.get(OLGH17837Entity_.intVal2), checkParam1), resultParam1)
                            .when(cb.equal(root.get(OLGH17837Entity_.intVal2), checkParam2), resultParam2)
                            .otherwise(resultParam3);
            cquery.multiselect(selectCase);

            ParameterExpression<Integer> checkParam3 = cb.parameter(Integer.class);
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), checkParam3));

            query = em.createQuery(cquery);
            query.setParameter(checkParam1, 5);
            query.setParameter(resultParam1, 6);
            query.setParameter(checkParam2, 15);
            query.setParameter(resultParam2, 16);
            query.setParameter(resultParam3, 26);
            query.setParameter(checkParam3, 99);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END  FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END  FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);

            Expression<Object> selectCase2 = cb2.selectCase()
                            .when(cb2.equal(root2.get(OLGH17837Entity_.intVal2), 5), 6)
                            .when(cb2.equal(root2.get(OLGH17837Entity_.intVal2), 15), 16)
                            .otherwise(26);
            cquery2.multiselect(selectCase2);

            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), 99));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END  FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END  FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE WHEN (INTVAL2 = 5) THEN 6 WHEN (INTVAL2 = 15) THEN 16 ELSE 26 END  FROM OLGH17837ENTITY WHERE (INTVAL1 = 99)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END  FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);

            ParameterExpression<Integer> checkParam4 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> resultParam4 = cb3.parameter(Integer.class);

            Expression<Object> selectCase3 = cb3.selectCase()
                            .when(cb3.equal(root3.get(OLGH17837Entity_.intVal2), checkParam4), resultParam4)
                            .when(cb3.equal(root3.get(OLGH17837Entity_.intVal2), 15), 16)
                            .otherwise(26);
            cquery3.multiselect(selectCase3);

            ParameterExpression<Integer> checkParam5 = cb3.parameter(Integer.class);
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.intVal1), checkParam5));

            query = em.createQuery(cquery3);
            query.setParameter(checkParam4, 5);
            query.setParameter(resultParam4, 6);
            query.setParameter(checkParam5, 99);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END  FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END  FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CASE WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = 15) THEN 16 ELSE 26 END  FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CASE WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END  FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
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

    public void testCase4_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT CASE WHEN s.intVal2 = ?1 THEN ?2 WHEN s.intVal2 = ?3 THEN ?4 ELSE ?5 END FROM OLGH17837Entity s WHERE s.intVal1 = ?6");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 15);
            query.setParameter(4, 16);
            query.setParameter(5, 26);
            query.setParameter(6, 99);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE  WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE  WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT CASE WHEN s.intVal2 = 5 THEN 6 WHEN s.intVal2 = 15 THEN 16 ELSE 26 END FROM OLGH17837Entity s WHERE s.intVal1 = 99");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE  WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE  WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT CASE WHEN s.intVal2 = ?1 THEN ?2 WHEN s.intVal2 = 15 THEN 16 ELSE 26 END FROM OLGH17837Entity s WHERE s.intVal1 = ?3");
            query.setParameter(1, 5);
            query.setParameter(2, 6);
            query.setParameter(3, 99);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE  WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE  WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);

            ParameterExpression<Integer> checkParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> checkParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam3 = cb.parameter(Integer.class);

            Expression<Object> selectCase = cb.selectCase()
                            .when(cb.equal(root.get(OLGH17837Entity_.intVal2), checkParam1), resultParam1)
                            .when(cb.equal(root.get(OLGH17837Entity_.intVal2), checkParam2), resultParam2)
                            .otherwise(resultParam3);
            cquery.multiselect(selectCase);

            ParameterExpression<Integer> checkParam3 = cb.parameter(Integer.class);
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), checkParam3));

            query = em.createQuery(cquery);
            query.setParameter(checkParam1, 5);
            query.setParameter(resultParam1, 6);
            query.setParameter(checkParam2, 15);
            query.setParameter(resultParam2, 16);
            query.setParameter(resultParam3, 26);
            query.setParameter(checkParam3, 99);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END  FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END  FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);

            Expression<Object> selectCase2 = cb2.selectCase()
                            .when(cb2.equal(root2.get(OLGH17837Entity_.intVal2), 5), 6)
                            .when(cb2.equal(root2.get(OLGH17837Entity_.intVal2), 15), 16)
                            .otherwise(26);
            cquery2.multiselect(selectCase2);

            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), 99));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END  FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END  FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);

            ParameterExpression<Integer> checkParam4 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> resultParam4 = cb3.parameter(Integer.class);

            Expression<Object> selectCase3 = cb3.selectCase()
                            .when(cb3.equal(root3.get(OLGH17837Entity_.intVal2), checkParam4), resultParam4)
                            .when(cb3.equal(root3.get(OLGH17837Entity_.intVal2), 15), 16)
                            .otherwise(26);
            cquery3.multiselect(selectCase3);

            ParameterExpression<Integer> checkParam5 = cb3.parameter(Integer.class);
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.intVal1), checkParam5));

            query = em.createQuery(cquery3);
            query.setParameter(checkParam4, 5);
            query.setParameter(resultParam4, 6);
            query.setParameter(checkParam5, 99);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CASE WHEN (INTVAL2 = ?) THEN 6 WHEN (INTVAL2 = ?) THEN 16 ELSE 26 END  FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CASE WHEN (INTVAL2 = ?) THEN ? WHEN (INTVAL2 = ?) THEN ? ELSE ? END  FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
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

    public void testNullIf1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT 1 FROM OLGH17837Entity s WHERE ?1 = NULLIF(s.strVal1, ?2)");
            query.setParameter(1, "HELLO");
            query.setParameter(2, "WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE ('HELLO' = NULLIF(STRVAL1, 'WORLD'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(STRVAL1, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM OLGH17837Entity s WHERE 'HELLO' = NULLIF(s.strVal1, 'WORLD')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE ('HELLO' = NULLIF(STRVAL1, 'WORLD'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(STRVAL1, ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            cquery.multiselect(cb.literal(1));
            cquery.where(cb.equal(strParam1, cb.nullif(root.get(OLGH17837Entity_.strVal1), strParam2)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.setParameter(strParam2, "WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE ('HELLO' = NULLIF(STRVAL1, 'WORLD'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(STRVAL1, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.where(cb2.equal(cb2.literal("HELLO"), cb2.nullif(root2.get(OLGH17837Entity_.strVal1), "WORLD")));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE ('HELLO' = NULLIF(STRVAL1, 'WORLD'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(STRVAL1, ?))", sql.remove(0));
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

    public void testNullIf1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT 1 FROM OLGH17837Entity s WHERE ?1 = NULLIF(s.strVal1, ?2)");
            query.setParameter(1, "HELLO");
            query.setParameter(2, "WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE (? = NULLIF(STRVAL1, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(STRVAL1, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM OLGH17837Entity s WHERE 'HELLO' = NULLIF(s.strVal1, 'WORLD')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE (? = NULLIF(STRVAL1, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(STRVAL1, ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE ('HELLO' = NULLIF(STRVAL1, 'WORLD'))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(STRVAL1, ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            cquery.multiselect(cb.literal(1));
            cquery.where(cb.equal(strParam1, cb.nullif(root.get(OLGH17837Entity_.strVal1), strParam2)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.setParameter(strParam2, "WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE (? = NULLIF(STRVAL1, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(STRVAL1, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.where(cb2.equal(cb2.literal("HELLO"), cb2.nullif(root2.get(OLGH17837Entity_.strVal1), "WORLD")));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE (? = NULLIF(STRVAL1, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(STRVAL1, ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE ('HELLO' = NULLIF(STRVAL1, 'WORLD'))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(STRVAL1, ?))", sql.remove(0));
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

    public void testNullIf1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT 1 FROM OLGH17837Entity s WHERE ?1 = NULLIF(s.strVal1, ?2)");
            query.setParameter(1, "HELLO");
            query.setParameter(2, "WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE (? = NULLIF(STRVAL1, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(STRVAL1, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM OLGH17837Entity s WHERE 'HELLO' = NULLIF(s.strVal1, 'WORLD')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE (? = NULLIF(STRVAL1, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(STRVAL1, ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            cquery.multiselect(cb.literal(1));
            cquery.where(cb.equal(strParam1, cb.nullif(root.get(OLGH17837Entity_.strVal1), strParam2)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.setParameter(strParam2, "WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE (? = NULLIF(STRVAL1, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(STRVAL1, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.where(cb2.equal(cb2.literal("HELLO"), cb2.nullif(root2.get(OLGH17837Entity_.strVal1), "WORLD")));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE (? = NULLIF(STRVAL1, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(STRVAL1, ?))", sql.remove(0));
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

    public void testNullIf2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT 1 FROM OLGH17837Entity s WHERE ?1 = NULLIF(?2, s.strVal1)");
            query.setParameter(1, "HELLO");
            query.setParameter(2, "WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE ('HELLO' = NULLIF('WORLD', STRVAL1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(?, STRVAL1))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM OLGH17837Entity s WHERE 'HELLO' = NULLIF('WORLD', s.strVal1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE ('HELLO' = NULLIF('WORLD', STRVAL1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(?, STRVAL1))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            cquery.multiselect(cb.literal(1));
            cquery.where(cb.equal(strParam1, cb.nullif(strParam2, root.get(OLGH17837Entity_.strVal1))));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.setParameter(strParam2, "WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE ('HELLO' = NULLIF('WORLD', STRVAL1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(?, STRVAL1))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.where(cb2.equal(cb2.literal("HELLO"), cb2.<String> nullif(cb2.literal("WORLD"), root2.get(OLGH17837Entity_.strVal1))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE ('HELLO' = NULLIF('WORLD', STRVAL1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(?, STRVAL1))", sql.remove(0));
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

    public void testNullIf2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT 1 FROM OLGH17837Entity s WHERE ?1 = NULLIF(?2, s.strVal1)");
            query.setParameter(1, "HELLO");
            query.setParameter(2, "WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE (? = NULLIF(?, STRVAL1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(?, STRVAL1))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM OLGH17837Entity s WHERE 'HELLO' = NULLIF('WORLD', s.strVal1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE (? = NULLIF(?, STRVAL1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(?, STRVAL1))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE ('HELLO' = NULLIF('WORLD', STRVAL1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(?, STRVAL1))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            cquery.multiselect(cb.literal(1));
            cquery.where(cb.equal(strParam1, cb.nullif(strParam2, root.get(OLGH17837Entity_.strVal1))));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.setParameter(strParam2, "WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE (? = NULLIF(?, STRVAL1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(?, STRVAL1))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.where(cb2.equal(cb2.literal("HELLO"), cb2.<String> nullif(cb2.literal("WORLD"), root2.get(OLGH17837Entity_.strVal1))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE (? = NULLIF(?, STRVAL1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(?, STRVAL1))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE ('HELLO' = NULLIF('WORLD', STRVAL1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(?, STRVAL1))", sql.remove(0));
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

    public void testNullIf2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT 1 FROM OLGH17837Entity s WHERE ?1 = NULLIF(?2, s.strVal1)");
            query.setParameter(1, "HELLO");
            query.setParameter(2, "WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE (? = NULLIF(?, STRVAL1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(?, STRVAL1))", sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM OLGH17837Entity s WHERE 'HELLO' = NULLIF('WORLD', s.strVal1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE (? = NULLIF(?, STRVAL1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(?, STRVAL1))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            cquery.multiselect(cb.literal(1));
            cquery.where(cb.equal(strParam1, cb.nullif(strParam2, root.get(OLGH17837Entity_.strVal1))));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.setParameter(strParam2, "WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE (? = NULLIF(?, STRVAL1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(?, STRVAL1))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.where(cb2.equal(cb2.literal("HELLO"), cb2.<String> nullif(cb2.literal("WORLD"), root2.get(OLGH17837Entity_.strVal1))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY WHERE (? = NULLIF(?, STRVAL1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY WHERE (? = NULLIF(?, STRVAL1))", sql.remove(0));
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

    public void testNullIf3_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT NULLIF(?1, ?2) FROM OLGH17837Entity s");
            query.setParameter(1, "HELLO");
            query.setParameter(2, "WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT NULLIF('HELLO', 'WORLD') FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT NULLIF(?, ?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT NULLIF('HELLO', 'WORLD') FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT NULLIF('HELLO', 'WORLD') FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT NULLIF(?, ?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT NULLIF(?1, 'WORLD') FROM OLGH17837Entity s");
            query.setParameter(1, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT NULLIF('HELLO', 'WORLD') FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT NULLIF(?, ?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            cquery.multiselect(cb.nullif(strParam1, strParam2));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.setParameter(strParam2, "WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT NULLIF('HELLO', 'WORLD') FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT NULLIF(?, ?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.<String> nullif(cb2.literal("HELLO"), cb2.literal("WORLD")));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT NULLIF('HELLO', 'WORLD') FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT NULLIF(?, ?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam3 = cb3.parameter(String.class);
            cquery3.multiselect(cb3.nullif(strParam3, cb3.literal("WORLD")));

            query = em.createQuery(cquery3);
            query.setParameter(strParam3, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT NULLIF('HELLO', 'WORLD') FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT NULLIF(?, ?) FROM OLGH17837ENTITY", sql.remove(0));
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

    public void testNullIf3_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT NULLIF(?1, ?2) FROM OLGH17837Entity s");
            query.setParameter(1, "HELLO");
            query.setParameter(2, "WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT NULLIF(?, 'WORLD') FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT NULLIF(?, ?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT NULLIF('HELLO', 'WORLD') FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT NULLIF(?, 'WORLD') FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT NULLIF(?, ?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT NULLIF('HELLO', 'WORLD') FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT NULLIF(?, ?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT NULLIF(?1, 'WORLD') FROM OLGH17837Entity s");
            query.setParameter(1, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT NULLIF(?, 'WORLD') FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT NULLIF(?, ?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            cquery.multiselect(cb.nullif(strParam1, strParam2));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.setParameter(strParam2, "WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT NULLIF(?, 'WORLD') FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT NULLIF(?, ?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.<String> nullif(cb2.literal("HELLO"), cb2.literal("WORLD")));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT NULLIF(?, 'WORLD') FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT NULLIF(?, ?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT NULLIF('HELLO', 'WORLD') FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT NULLIF(?, ?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam3 = cb3.parameter(String.class);
            cquery3.multiselect(cb3.nullif(strParam3, cb3.literal("WORLD")));

            query = em.createQuery(cquery3);
            query.setParameter(strParam3, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT NULLIF(?, 'WORLD') FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT NULLIF(?, ?) FROM OLGH17837ENTITY", sql.remove(0));
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

    public void testNullIf3_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT NULLIF(?1, ?2) FROM OLGH17837Entity s");
            query.setParameter(1, "HELLO");
            query.setParameter(2, "WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT NULLIF(?, 'WORLD') FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT NULLIF(?, ?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT NULLIF('HELLO', 'WORLD') FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT NULLIF(?, 'WORLD') FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT NULLIF(?, ?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT NULLIF(?1, 'WORLD') FROM OLGH17837Entity s");
            query.setParameter(1, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT NULLIF(?, 'WORLD') FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT NULLIF(?, ?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            cquery.multiselect(cb.nullif(strParam1, strParam2));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.setParameter(strParam2, "WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT NULLIF(?, 'WORLD') FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT NULLIF(?, ?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.nullif(cb2.<String> literal("HELLO"), cb2.literal("WORLD")));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT NULLIF(?, 'WORLD') FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT NULLIF(?, ?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam3 = cb3.parameter(String.class);
            cquery3.multiselect(cb3.nullif(strParam3, cb3.literal("WORLD")));

            query = em.createQuery(cquery3);
            query.setParameter(strParam3, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT NULLIF(?, 'WORLD') FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT NULLIF(?, ?) FROM OLGH17837ENTITY", sql.remove(0));
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

    public void testCoalesce1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.strVal2 FROM OLGH17837Entity s WHERE COALESCE(s.intVal1, ?3) = s.intVal2");
            query.setParameter(3, 5);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));

            query = em.createQuery("SELECT s.strVal2 FROM OLGH17837Entity s WHERE COALESCE(s.intVal1, 5) = s.intVal2");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, 5) = INTVAL2)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal2));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            CriteriaBuilder.Coalesce<Integer> coalesce = cb.coalesce();
            coalesce.value(root.get(OLGH17837Entity_.intVal1));
            coalesce.value(intParam1);
            cquery.where(cb.equal(coalesce, root.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal2));

            ParameterExpression<Integer> intParam2 = cb2.parameter(Integer.class);
            Expression<Integer> coalesce2 = cb2.coalesce(root2.get(OLGH17837Entity_.intVal1), intParam2);
            cquery2.where(cb2.equal(coalesce2, root2.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery2);
            query.setParameter(intParam2, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal2));

            CriteriaBuilder.Coalesce<Integer> coalesce3 = cb3.coalesce();
            coalesce3.value(root3.get(OLGH17837Entity_.intVal1));
            coalesce3.value(cb3.literal(5));
            cquery3.where(cb3.equal(coalesce3, root3.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, 5) = INTVAL2)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));
                }
            }

            CriteriaBuilder cb4 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery4 = cb4.createQuery(Object[].class);
            Root<OLGH17837Entity> root4 = cquery4.from(OLGH17837Entity.class);
            cquery4.multiselect(root4.get(OLGH17837Entity_.strVal2));

            Expression<Integer> coalesce4 = cb4.coalesce(root4.get(OLGH17837Entity_.intVal1), cb4.literal(5));
            cquery4.where(cb4.equal(coalesce4, root4.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery4);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, 5) = INTVAL2)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));
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

    public void testCoalesce1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.strVal2 FROM OLGH17837Entity s WHERE COALESCE(s.intVal1, ?3) = s.intVal2");
            query.setParameter(3, 5);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));

            query = em.createQuery("SELECT s.strVal2 FROM OLGH17837Entity s WHERE COALESCE(s.intVal1, 5) = s.intVal2");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, 5) = INTVAL2)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal2));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            CriteriaBuilder.Coalesce<Integer> coalesce = cb.coalesce();
            coalesce.value(root.get(OLGH17837Entity_.intVal1));
            coalesce.value(intParam1);
            cquery.where(cb.equal(coalesce, root.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal2));

            ParameterExpression<Integer> intParam2 = cb2.parameter(Integer.class);
            Expression<Integer> coalesce2 = cb2.coalesce(root2.get(OLGH17837Entity_.intVal1), intParam2);
            cquery2.where(cb2.equal(coalesce2, root2.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery2);
            query.setParameter(intParam2, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal2));

            CriteriaBuilder.Coalesce<Integer> coalesce3 = cb3.coalesce();
            coalesce3.value(root3.get(OLGH17837Entity_.intVal1));
            coalesce3.value(cb3.literal(5));
            cquery3.where(cb3.equal(coalesce3, root3.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, 5) = INTVAL2)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));
                }
            }

            CriteriaBuilder cb4 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery4 = cb4.createQuery(Object[].class);
            Root<OLGH17837Entity> root4 = cquery4.from(OLGH17837Entity.class);
            cquery4.multiselect(root4.get(OLGH17837Entity_.strVal2));

            Expression<Integer> coalesce4 = cb4.coalesce(root4.get(OLGH17837Entity_.intVal1), cb4.literal(5));
            cquery4.where(cb4.equal(coalesce4, root4.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery4);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, 5) = INTVAL2)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));
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

    public void testCoalesce1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.strVal2 FROM OLGH17837Entity s WHERE COALESCE(s.intVal1, ?3) = s.intVal2");
            query.setParameter(3, 5);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));

            query = em.createQuery("SELECT s.strVal2 FROM OLGH17837Entity s WHERE COALESCE(s.intVal1, 5) = s.intVal2");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal2));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            CriteriaBuilder.Coalesce<Integer> coalesce = cb.coalesce();
            coalesce.value(root.get(OLGH17837Entity_.intVal1));
            coalesce.value(intParam1);
            cquery.where(cb.equal(coalesce, root.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal2));

            ParameterExpression<Integer> intParam2 = cb2.parameter(Integer.class);
            Expression<Integer> coalesce2 = cb2.coalesce(root2.get(OLGH17837Entity_.intVal1), intParam2);
            cquery2.where(cb2.equal(coalesce2, root2.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery2);
            query.setParameter(intParam2, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal2));

            CriteriaBuilder.Coalesce<Integer> coalesce3 = cb3.coalesce();
            coalesce3.value(root3.get(OLGH17837Entity_.intVal1));
            coalesce3.value(cb3.literal(5));
            cquery3.where(cb3.equal(coalesce3, root3.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));

            CriteriaBuilder cb4 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery4 = cb4.createQuery(Object[].class);
            Root<OLGH17837Entity> root4 = cquery4.from(OLGH17837Entity.class);
            cquery4.multiselect(root4.get(OLGH17837Entity_.strVal2));

            Expression<Integer> coalesce4 = cb4.coalesce(root4.get(OLGH17837Entity_.intVal1), cb4.literal(5));
            cquery4.where(cb4.equal(coalesce4, root4.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery4);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(INTVAL1, ?) = INTVAL2)", sql.remove(0));
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

    public void testCoalesce2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT COALESCE(s.intVal1, ?3) FROM OLGH17837Entity s");
            query.setParameter(3, 5);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));

            query = em.createQuery("SELECT COALESCE(s.intVal1, 5) FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT COALESCE(INTVAL1, 5) FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            CriteriaBuilder.Coalesce<Integer> coalesce = cb.coalesce();
            coalesce.value(root.get(OLGH17837Entity_.intVal1));
            coalesce.value(intParam1);
            cquery.multiselect(coalesce);

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);

            ParameterExpression<Integer> intParam2 = cb2.parameter(Integer.class);
            Expression<Integer> coalesce2 = cb2.coalesce(root2.get(OLGH17837Entity_.intVal1), intParam2);
            cquery2.multiselect(coalesce2);

            query = em.createQuery(cquery2);
            query.setParameter(intParam2, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);

            CriteriaBuilder.Coalesce<Integer> coalesce3 = cb3.coalesce();
            coalesce3.value(root3.get(OLGH17837Entity_.intVal1));
            coalesce3.value(cb3.literal(5));
            cquery3.multiselect(coalesce3);

            query = em.createQuery(cquery3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT COALESCE(INTVAL1, 5) FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            }

            CriteriaBuilder cb4 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery4 = cb4.createQuery(Object[].class);
            Root<OLGH17837Entity> root4 = cquery4.from(OLGH17837Entity.class);

            Expression<Integer> coalesce4 = cb4.coalesce(root4.get(OLGH17837Entity_.intVal1), cb4.literal(5));
            cquery4.multiselect(coalesce4);

            query = em.createQuery(cquery4);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT COALESCE(INTVAL1, 5) FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
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

    public void testCoalesce2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT COALESCE(s.intVal1, ?3) FROM OLGH17837Entity s");
            query.setParameter(3, 5);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));

            query = em.createQuery("SELECT COALESCE(s.intVal1, 5) FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT COALESCE(INTVAL1, 5) FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            CriteriaBuilder.Coalesce<Integer> coalesce = cb.coalesce();
            coalesce.value(root.get(OLGH17837Entity_.intVal1));
            coalesce.value(intParam1);
            cquery.multiselect(coalesce);

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);

            ParameterExpression<Integer> intParam2 = cb2.parameter(Integer.class);
            Expression<Integer> coalesce2 = cb2.coalesce(root2.get(OLGH17837Entity_.intVal1), intParam2);
            cquery2.multiselect(coalesce2);

            query = em.createQuery(cquery2);
            query.setParameter(intParam2, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);

            CriteriaBuilder.Coalesce<Integer> coalesce3 = cb3.coalesce();
            coalesce3.value(root3.get(OLGH17837Entity_.intVal1));
            coalesce3.value(cb3.literal(5));
            cquery3.multiselect(coalesce3);

            query = em.createQuery(cquery3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT COALESCE(INTVAL1, 5) FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            }

            CriteriaBuilder cb4 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery4 = cb4.createQuery(Object[].class);
            Root<OLGH17837Entity> root4 = cquery4.from(OLGH17837Entity.class);

            Expression<Integer> coalesce4 = cb4.coalesce(root4.get(OLGH17837Entity_.intVal1), cb4.literal(5));
            cquery4.multiselect(coalesce4);

            query = em.createQuery(cquery4);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT COALESCE(INTVAL1, 5) FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
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

    public void testCoalesce2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT COALESCE(s.intVal1, ?3) FROM OLGH17837Entity s");
            query.setParameter(3, 5);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));

            query = em.createQuery("SELECT COALESCE(s.intVal1, 5) FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery = cb.createQuery(Integer[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            CriteriaBuilder.Coalesce<Integer> coalesce = cb.coalesce();
            coalesce.value(root.get(OLGH17837Entity_.intVal1));
            coalesce.value(intParam1);
            cquery.multiselect(coalesce);

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);

            ParameterExpression<Integer> intParam2 = cb2.parameter(Integer.class);
            Expression<Integer> coalesce2 = cb2.coalesce(root2.get(OLGH17837Entity_.intVal1), intParam2);
            cquery2.multiselect(coalesce2);

            query = em.createQuery(cquery2);
            query.setParameter(intParam2, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);

            CriteriaBuilder.Coalesce<Integer> coalesce3 = cb3.coalesce();
            coalesce3.value(root3.get(OLGH17837Entity_.intVal1));
            coalesce3.value(cb3.literal(5));
            cquery3.multiselect(coalesce3);

            query = em.createQuery(cquery3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));

            CriteriaBuilder cb4 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery4 = cb4.createQuery(Object[].class);
            Root<OLGH17837Entity> root4 = cquery4.from(OLGH17837Entity.class);

            Expression<Integer> coalesce4 = cb4.coalesce(root4.get(OLGH17837Entity_.intVal1), cb4.literal(5));
            cquery4.multiselect(coalesce4);

            query = em.createQuery(cquery4);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT COALESCE(INTVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
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

    public void testCoalesce3_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.strVal2 FROM OLGH17837Entity s WHERE COALESCE(?1, ?2, ?3, ?4, ?5) = s.intVal2");
            query.setParameter(1, 2);
            query.setParameter(2, 7);
            query.setParameter(3, 5);
            query.setParameter(4, 9);
            query.setParameter(5, 12);
            Exception exception = null;
            List<String> sql = null;
            try {
                query.getResultList();
            } catch (Exception e) {
                exception = e;
            } finally {
                sql = SQLCallListener.getAndClearCallList();
                Assert.assertEquals(1, sql.size());
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertNotNull("Expected query '" + sql.remove(0) + "' to fail", exception);
                } else {
                    Assert.assertNull(exception);
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, ?) = INTVAL2)", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.strVal2 FROM OLGH17837Entity s WHERE COALESCE(NULL, 7, 5, 9, 12) = s.intVal2");
            exception = null;
            try {
                query.getResultList();
            } catch (Exception e) {
                exception = e;
            } finally {
                sql = SQLCallListener.getAndClearCallList();
                Assert.assertEquals(1, sql.size());
                if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                    if (isDB2Z || isDB2 || isDerby) {
                        // DB2 z throws `DB2 SQL Error: SQLCODE=-206, SQLSTATE=42703` because a "NULL" value is being passed
                        // Derby throws `ERROR 42X01: Syntax error: Encountered "NULL" at line 1, column 50.`
                        Assert.assertNotNull("Expected query '" + sql.remove(0) + "' to fail", exception);
                    } else {
                        Assert.assertNull(exception);
                        Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, ?) = INTVAL2)", sql.remove(0));
                    }
                } else {
                    if (isDB2Z || isDerby) {
                        // DB2 z throws `DB2 SQL Error: SQLCODE=-206, SQLSTATE=42703` because a "NULL" value is being passed
                        // Derby throws `ERROR 42X01: Syntax error: Encountered "NULL" at line 1, column 50.`
                        Assert.assertNotNull("Expected query '" + sql.remove(0) + "' to fail", exception);
                    } else if (isDB2) {
                        Assert.assertNull(exception);
                        Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(NULL, 7, 5, 9, 12) = INTVAL2)", sql.remove(0));
                    } else {
                        Assert.assertNull(exception);
                        Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, ?) = INTVAL2)", sql.remove(0));
                    }
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal2));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam4 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam5 = cb.parameter(Integer.class);
            CriteriaBuilder.Coalesce<Integer> coalesce = cb.coalesce();
            coalesce.value(intParam1);
            coalesce.value(intParam2);
            coalesce.value(intParam3);
            coalesce.value(intParam4);
            coalesce.value(intParam5);
            cquery.where(cb.equal(coalesce, root.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, null);
            query.setParameter(intParam2, 7);
            query.setParameter(intParam3, 5);
            query.setParameter(intParam4, 9);
            query.setParameter(intParam5, 12);
            exception = null;
            try {
                query.getResultList();
            } catch (Exception e) {
                exception = e;
            } finally {
                sql = SQLCallListener.getAndClearCallList();
                Assert.assertEquals(1, sql.size());
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertNotNull("Expected query '" + sql.remove(0) + "' to fail", exception);
                } else {
                    Assert.assertNull(exception);
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, ?) = INTVAL2)", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal2));

            CriteriaBuilder.Coalesce<Integer> coalesce3 = cb3.coalesce();
            coalesce3.value(cb3.nullLiteral(Integer.class));
            coalesce3.value(cb3.literal(7));
            coalesce3.value(cb3.literal(5));
            coalesce3.value(cb3.literal(9));
            coalesce3.value(cb3.literal(12));
            cquery3.where(cb3.equal(coalesce3, root3.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery3);
            exception = null;
            try {
                query.getResultList();
            } catch (Exception e) {
                exception = e;
            } finally {
                sql = SQLCallListener.getAndClearCallList();
                Assert.assertEquals(1, sql.size());
                if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                    if (isDB2Z || isDB2 || isDerby) {
                        // DB2 z throws `DB2 SQL Error: SQLCODE=-206, SQLSTATE=42703` because a "NULL" value is being passed
                        // Derby throws `ERROR 42X01: Syntax error: Encountered "NULL" at line 1, column 50.`
                        Assert.assertNotNull("Expected query '" + sql.remove(0) + "' to fail", exception);
                    } else {
                        Assert.assertNull(exception);
                        Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, ?) = INTVAL2)", sql.remove(0));
                    }
                } else {
                    if (isDB2Z || isDerby) {
                        // DB2 z throws `DB2 SQL Error: SQLCODE=-206, SQLSTATE=42703` because a "NULL" value is being passed
                        // Derby throws `ERROR 42X01: Syntax error: Encountered "NULL" at line 1, column 50.`
                        Assert.assertNotNull("Expected query '" + sql.remove(0) + "' to fail", exception);
                    } else if (isDB2) {
                        Assert.assertNull(exception);
                        Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(NULL, 7, 5, 9, 12) = INTVAL2)", sql.remove(0));
                    } else {
                        Assert.assertNull(exception);
                        Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, ?) = INTVAL2)", sql.remove(0));
                    }
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

    public void testCoalesce3_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.strVal2 FROM OLGH17837Entity s WHERE COALESCE(?1, ?2, ?3, ?4, ?5) = s.intVal2");
            query.setParameter(1, 2);
            query.setParameter(2, 7);
            query.setParameter(3, 5);
            query.setParameter(4, 9);
            query.setParameter(5, 12);
            Exception exception = null;
            List<String> sql = null;
            try {
                query.getResultList();
            } catch (Exception e) {
                exception = e;
            } finally {
                sql = SQLCallListener.getAndClearCallList();
                Assert.assertEquals(1, sql.size());
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertNull(exception);
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, 12) = INTVAL2)", sql.remove(0));
                } else {
                    Assert.assertNull(exception);
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, ?) = INTVAL2)", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.strVal2 FROM OLGH17837Entity s WHERE COALESCE(NULL, 7, 5, 9, 12) = s.intVal2");
            exception = null;
            try {
                query.getResultList();
            } catch (Exception e) {
                exception = e;
            } finally {
                sql = SQLCallListener.getAndClearCallList();
                Assert.assertEquals(1, sql.size());
                if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                    if (isDB2Z || isDB2 || isDerby) {
                        Assert.assertNull(exception);
                        Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, 12) = INTVAL2)", sql.remove(0));
                    } else {
                        Assert.assertNull(exception);
                        Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, ?) = INTVAL2)", sql.remove(0));
                    }
                } else {
                    if (isDB2Z || isDerby) {
                        // DB2 z throws `DB2 SQL Error: SQLCODE=-206, SQLSTATE=42703` because a "NULL" value is being passed
                        // Derby throws `ERROR 42X01: Syntax error: Encountered "NULL" at line 1, column 50.`
                        Assert.assertNotNull("Expected query '" + sql.remove(0) + "' to fail", exception);
                    } else if (isDB2) {
                        Assert.assertNull(exception);
                        Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(NULL, 7, 5, 9, 12) = INTVAL2)", sql.remove(0));
                    } else {
                        Assert.assertNull(exception);
                        Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, ?) = INTVAL2)", sql.remove(0));
                    }
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal2));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam4 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam5 = cb.parameter(Integer.class);
            CriteriaBuilder.Coalesce<Integer> coalesce = cb.coalesce();
            coalesce.value(intParam1);
            coalesce.value(intParam2);
            coalesce.value(intParam3);
            coalesce.value(intParam4);
            coalesce.value(intParam5);
            cquery.where(cb.equal(coalesce, root.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, null);
            query.setParameter(intParam2, 7);
            query.setParameter(intParam3, 5);
            query.setParameter(intParam4, 9);
            query.setParameter(intParam5, 12);
            exception = null;
            try {
                query.getResultList();
            } catch (Exception e) {
                exception = e;
            } finally {
                sql = SQLCallListener.getAndClearCallList();
                Assert.assertEquals(1, sql.size());
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertNull(exception);
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, 12) = INTVAL2)", sql.remove(0));
                } else {
                    Assert.assertNull(exception);
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, ?) = INTVAL2)", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal2));

            CriteriaBuilder.Coalesce<Integer> coalesce3 = cb3.coalesce();
            coalesce3.value(cb3.nullLiteral(Integer.class));
            coalesce3.value(cb3.literal(7));
            coalesce3.value(cb3.literal(5));
            coalesce3.value(cb3.literal(9));
            coalesce3.value(cb3.literal(12));
            cquery3.where(cb3.equal(coalesce3, root3.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery3);
            exception = null;
            try {
                query.getResultList();
            } catch (Exception e) {
                exception = e;
            } finally {
                sql = SQLCallListener.getAndClearCallList();
                Assert.assertEquals(1, sql.size());
                if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                    if (isDB2Z || isDB2 || isDerby) {
                        Assert.assertNull(exception);
                        Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, 12) = INTVAL2)", sql.remove(0));
                    } else {
                        Assert.assertNull(exception);
                        Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, ?) = INTVAL2)", sql.remove(0));
                    }
                } else {
                    if (isDB2Z || isDerby) {
                        // DB2 z throws `DB2 SQL Error: SQLCODE=-206, SQLSTATE=42703` because a "NULL" value is being passed
                        // Derby throws `ERROR 42X01: Syntax error: Encountered "NULL" at line 1, column 50.`
                        Assert.assertNotNull("Expected query '" + sql.remove(0) + "' to fail", exception);
                    } else if (isDB2) {
                        Assert.assertNull(exception);
                        Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(NULL, 7, 5, 9, 12) = INTVAL2)", sql.remove(0));
                    } else {
                        Assert.assertNull(exception);
                        Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, ?) = INTVAL2)", sql.remove(0));
                    }
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

    public void testCoalesce3_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.strVal2 FROM OLGH17837Entity s WHERE COALESCE(?1, ?2, ?3, ?4, ?5) = s.intVal2");
            query.setParameter(1, 2);
            query.setParameter(2, 7);
            query.setParameter(3, 5);
            query.setParameter(4, 9);
            query.setParameter(5, 12);
            Exception exception = null;
            List<String> sql = null;
            try {
                query.getResultList();
            } catch (Exception e) {
                exception = e;
            } finally {
                sql = SQLCallListener.getAndClearCallList();
                Assert.assertEquals(1, sql.size());
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertNull(exception);
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, 12) = INTVAL2)", sql.remove(0));
                } else {
                    Assert.assertNull(exception);
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, ?) = INTVAL2)", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.strVal2 FROM OLGH17837Entity s WHERE COALESCE(NULL, 7, 5, 9, 12) = s.intVal2");
            exception = null;
            try {
                query.getResultList();
            } catch (Exception e) {
                exception = e;
            } finally {
                sql = SQLCallListener.getAndClearCallList();
                Assert.assertEquals(1, sql.size());
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertNull(exception);
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, 12) = INTVAL2)", sql.remove(0));
                } else {
                    Assert.assertNull(exception);
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, ?) = INTVAL2)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal2));

            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam4 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam5 = cb.parameter(Integer.class);
            CriteriaBuilder.Coalesce<Integer> coalesce = cb.coalesce();
            coalesce.value(intParam1);
            coalesce.value(intParam2);
            coalesce.value(intParam3);
            coalesce.value(intParam4);
            coalesce.value(intParam5);
            cquery.where(cb.equal(coalesce, root.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, null);
            query.setParameter(intParam2, 7);
            query.setParameter(intParam3, 5);
            query.setParameter(intParam4, 9);
            query.setParameter(intParam5, 12);
            exception = null;
            try {
                query.getResultList();
            } catch (Exception e) {
                exception = e;
            } finally {
                sql = SQLCallListener.getAndClearCallList();
                Assert.assertEquals(1, sql.size());
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertNull(exception);
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, 12) = INTVAL2)", sql.remove(0));
                } else {
                    Assert.assertNull(exception);
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, ?) = INTVAL2)", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal2));

            CriteriaBuilder.Coalesce<Integer> coalesce3 = cb3.coalesce();
            coalesce3.value(cb3.nullLiteral(Integer.class));
            coalesce3.value(cb3.literal(7));
            coalesce3.value(cb3.literal(5));
            coalesce3.value(cb3.literal(9));
            coalesce3.value(cb3.literal(12));
            cquery3.where(cb3.equal(coalesce3, root3.get(OLGH17837Entity_.intVal2)));

            query = em.createQuery(cquery3);
            exception = null;
            try {
                query.getResultList();
            } catch (Exception e) {
                exception = e;
            } finally {
                sql = SQLCallListener.getAndClearCallList();
                Assert.assertEquals(1, sql.size());
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertNull(exception);
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, 12) = INTVAL2)", sql.remove(0));
                } else {
                    Assert.assertNull(exception);
                    Assert.assertEquals("SELECT STRVAL2 FROM OLGH17837ENTITY WHERE (COALESCE(?, ?, ?, ?, ?) = INTVAL2)", sql.remove(0));
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
}
