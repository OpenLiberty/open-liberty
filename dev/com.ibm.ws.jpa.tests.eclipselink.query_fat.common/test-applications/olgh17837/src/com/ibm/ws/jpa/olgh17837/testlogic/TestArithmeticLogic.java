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

public class TestArithmeticLogic extends AbstractTestLogic {

    public void testABS1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ABS(?1) = s.intVal1");
            query.setParameter(1, -36);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(-36) = INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(?) = INTVAL1)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ABS(-36) = s.intVal1");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(-36) = INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(?) = INTVAL1)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.where(cb.equal(cb.abs(intParam1), root.get(OLGH17837Entity_.intVal1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, -36);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(-36) = INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(?) = INTVAL1)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.equal(cb2.abs(cb2.literal(-36)), root2.get(OLGH17837Entity_.intVal1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(-36) = INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(?) = INTVAL1)", sql.remove(0));
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

    public void testABS1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ABS(?1) = s.intVal1");
            query.setParameter(1, -36);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(-36) = INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(?) = INTVAL1)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ABS(-36) = s.intVal1");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(-36) = INTVAL1)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(?) = INTVAL1)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(-36) = INTVAL1)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(?) = INTVAL1)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.where(cb.equal(cb.abs(intParam1), root.get(OLGH17837Entity_.intVal1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, -36);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(-36) = INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(?) = INTVAL1)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.equal(cb2.abs(cb2.literal(-36)), root2.get(OLGH17837Entity_.intVal1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(-36) = INTVAL1)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(?) = INTVAL1)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(-36) = INTVAL1)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(?) = INTVAL1)", sql.remove(0));
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

    public void testABS1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ABS(?1) = s.intVal1");
            query.setParameter(1, -36);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(-36) = INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(?) = INTVAL1)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ABS(-36) = s.intVal1");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(-36) = INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(?) = INTVAL1)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.where(cb.equal(cb.abs(intParam1), root.get(OLGH17837Entity_.intVal1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, -36);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(-36) = INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(?) = INTVAL1)", sql.remove(0));
            }

            // ABS test #1 with CriteriaBuilder literal values
            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.equal(cb2.abs(cb2.literal(-36)), root2.get(OLGH17837Entity_.intVal1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(-36) = INTVAL1)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (ABS(?) = INTVAL1)", sql.remove(0));
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

    public void testADD1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1, (?1 + ?2) FROM OLGH17837Entity s WHERE s.intVal1 = (?1 + ?2)");
            query.setParameter(1, 4);
            query.setParameter(2, 2);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (4 + 2) FROM OLGH17837ENTITY WHERE (INTVAL1 = (4 + 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, (4 + 2) FROM OLGH17837Entity s WHERE s.intVal1 = (4 + 2)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (4 + 2) FROM OLGH17837ENTITY WHERE (INTVAL1 = (4 + 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, (4 + ?1) FROM OLGH17837Entity s WHERE s.intVal1 = (?1 + 4)");
            query.setParameter(1, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (4 + 2) FROM OLGH17837ENTITY WHERE (INTVAL1 = (2 + 4))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1), cb.sum(intParam1, intParam2));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), cb.sum(intParam1, intParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 4);
            query.setParameter(intParam2, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (4 + 2) FROM OLGH17837ENTITY WHERE (INTVAL1 = (4 + 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + ?))", sql.remove(0));
            }

            // Arithmetic test #1 with CriteriaBuilder literal values
            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1), cb2.sum(cb2.literal(4), 2));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), cb2.sum(cb2.literal(4), 2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (4 + 2) FROM OLGH17837ENTITY WHERE (INTVAL1 = (4 + 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + ?))", sql.remove(0));
            }

            // Arithmetic test #1 with a mixture of literal values and CriteriaBuilder parameters
            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1), cb3.sum(2, intParam3));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.intVal1), cb3.sum(intParam3, 2)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 4);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (2 + 4) FROM OLGH17837ENTITY WHERE (INTVAL1 = (4 + 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + ?))", sql.remove(0));
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

    public void testADD1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1, (?1 + ?2) FROM OLGH17837Entity s WHERE s.intVal1 = (?1 + ?2)");
            query.setParameter(1, 4);
            query.setParameter(2, 2);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (? + 2) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, (4 + 2) FROM OLGH17837Entity s WHERE s.intVal1 = (4 + 2)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, (? + 2) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + 2))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, (4 + 2) FROM OLGH17837ENTITY WHERE (INTVAL1 = (4 + 2))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1, (4 + ?1) FROM OLGH17837Entity s WHERE s.intVal1 = (?1 + 4)");
            query.setParameter(1, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, (? + 2) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + 4))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, (4 + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + 4))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1), cb.sum(intParam1, intParam2));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), cb.sum(intParam1, intParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 4);
            query.setParameter(intParam2, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (? + 2) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1), cb2.sum(cb2.literal(4), 2));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), cb2.sum(cb2.literal(4), 2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, (? + 2) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + 2))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, (4 + 2) FROM OLGH17837ENTITY WHERE (INTVAL1 = (4 + 2))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1), cb3.sum(2, intParam3));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.intVal1), cb3.sum(intParam3, 2)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 4);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, (? + 4) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + 2))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1, (2 + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + 2))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + ?))", sql.remove(0));
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

    public void testADD1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1, (?1 + ?2) FROM OLGH17837Entity s WHERE s.intVal1 = (?1 + ?2)");
            query.setParameter(1, 4);
            query.setParameter(2, 2);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (? + 2) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, (4 + 2) FROM OLGH17837Entity s WHERE s.intVal1 = (4 + 2)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (? + 2) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, (4 + ?1) FROM OLGH17837Entity s WHERE s.intVal1 = (?1 + 4)");
            query.setParameter(1, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (? + 2) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + 4))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1), cb.sum(intParam1, intParam2));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), cb.sum(intParam1, intParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 4);
            query.setParameter(intParam2, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (? + 2) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1), cb2.sum(cb2.literal(4), 2));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), cb2.sum(cb2.literal(4), 2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (? + 2) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam3 = cb3.parameter(Integer.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1), cb3.sum(2, intParam3));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.intVal1), cb3.sum(intParam3, 2)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam3, 4);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT INTVAL1, (? + 4) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, (? + ?) FROM OLGH17837ENTITY WHERE (INTVAL1 = (? + ?))", sql.remove(0));
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

    public void testMOD1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = MOD(?1, ?2)");
            query.setParameter(1, 36);
            query.setParameter(2, 10);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(36, 10))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = MOD(36, 10)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(36, 10))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(?, ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), cb.mod(intParam1, intParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 36);
            query.setParameter(intParam2, 10);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(36, 10))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), cb2.mod(36, cb2.literal(10))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(36, 10))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(?, ?))", sql.remove(0));
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

    public void testMOD1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = MOD(?1, ?2)");
            query.setParameter(1, 36);
            query.setParameter(2, 10);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(36, 10))", sql.remove(0));
            } else if (isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(?, 10))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = MOD(36, 10)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(36, 10))", sql.remove(0));
                } else if (isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(?, 10))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(?, ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(36, 10))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(?, ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), cb.mod(intParam1, intParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 36);
            query.setParameter(intParam2, 10);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(36, 10))", sql.remove(0));
            } else if (isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(?, 10))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), cb2.mod(36, cb2.literal(10))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(36, 10))", sql.remove(0));
                } else if (isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(?, 10))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(?, ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(36, 10))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(?, ?))", sql.remove(0));
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

    public void testMOD1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
        boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DERBY);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = MOD(?1, ?2)");
            query.setParameter(1, 36);
            query.setParameter(2, 10);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(36, 10))", sql.remove(0));
            } else if (isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(?, 10))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = MOD(36, 10)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(36, 10))", sql.remove(0));
            } else if (isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(?, 10))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(?, ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), cb.mod(intParam1, intParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 36);
            query.setParameter(intParam2, 10);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(36, 10))", sql.remove(0));
            } else if (isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(?, 10))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), cb2.mod(36, cb2.literal(10))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(36, 10))", sql.remove(0));
            } else if (isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(?, 10))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = MOD(?, ?))", sql.remove(0));
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

    public void testSQRT1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = SQRT(?1)");
            query.setParameter(1, 36);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(36))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = SQRT(36)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(36))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), cb.sqrt(intParam1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 36);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(36))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(?))", sql.remove(0));
            }

            // SQRT test #1 with CriteriaBuilder literal values
            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), cb2.sqrt(cb2.literal(36))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(36))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(?))", sql.remove(0));
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

    public void testSQRT1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = SQRT(?1)");
            query.setParameter(1, 36);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(36))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = SQRT(36)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(36))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(36))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), cb.sqrt(intParam1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 36);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(36))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(?))", sql.remove(0));
            }

            // SQRT test #1 with CriteriaBuilder literal values
            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), cb2.sqrt(cb2.literal(36))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(36))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(36))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(?))", sql.remove(0));
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

    public void testSQRT1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = SQRT(?1)");
            query.setParameter(1, 36);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(36))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = SQRT(36)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(36))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(?))", sql.remove(0));
            }

            // -----------------------

            // SQRT test #1 with CriteriaBuilder parameters
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), cb.sqrt(intParam1)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 36);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(36))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(?))", sql.remove(0));
            }

            // SQRT test #1 with CriteriaBuilder literal values
            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), cb2.sqrt(cb2.literal(36))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(36))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = SQRT(?))", sql.remove(0));
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

    public void testSUB1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.strVal1, ?2 FROM OLGH17837Entity s WHERE s.intVal1 != (?1 - ?2)");
            query.setParameter(1, 4);
            query.setParameter(2, 2);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (4 - 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1, ? FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1, 2 FROM OLGH17837Entity s WHERE s.intVal1 != (4 - 2)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (4 - 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1, ? FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1, ?2 FROM OLGH17837Entity s WHERE s.intVal1 != (4 - ?2)");
            query.setParameter(2, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (4 - 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1, ? FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1), intParam2);
            cquery.where(cb.notEqual(root.get(OLGH17837Entity_.intVal1), cb.diff(intParam1, intParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 4);
            query.setParameter(intParam2, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (4 - 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1, ? FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1), cb2.literal(2));
            cquery2.where(cb2.notEqual(root2.get(OLGH17837Entity_.intVal1), cb2.diff(cb2.literal(4), cb2.literal(2))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (4 - 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1, ? FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam6 = cb3.parameter(Integer.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal1), intParam6);
            cquery3.where(cb3.notEqual(root3.get(OLGH17837Entity_.intVal1), cb3.diff(cb3.literal(4), intParam6)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam6, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (4 - 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1, ? FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
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

    public void testSUB1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.strVal1, ?2 FROM OLGH17837Entity s WHERE s.intVal1 != (?1 - ?2)");
            query.setParameter(1, 4);
            query.setParameter(2, 2);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - 2))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1, ? FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1, 2 FROM OLGH17837Entity s WHERE s.intVal1 != (4 - 2)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDerby) {
                    Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - 2))", sql.remove(0));
                } else if (isDB2Z || isDB2) {
                    Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1, ? FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (4 - 2))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1, ? FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.strVal1, ?2 FROM OLGH17837Entity s WHERE s.intVal1 != (4 - ?2)");
            query.setParameter(2, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDerby) {
                    Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - 2))", sql.remove(0));
                } else if (isDB2Z || isDB2) {
                    Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1, ? FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (4 - ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1, ? FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1), intParam2);
            cquery.where(cb.notEqual(root.get(OLGH17837Entity_.intVal1), cb.diff(intParam1, intParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 4);
            query.setParameter(intParam2, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - 2))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1, ? FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1), cb2.literal(2));
            cquery2.where(cb2.notEqual(root2.get(OLGH17837Entity_.intVal1), cb2.diff(cb2.literal(4), cb2.literal(2))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDerby) {
                    Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - 2))", sql.remove(0));
                } else if (isDB2Z || isDB2) {
                    Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1, ? FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (4 - 2))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1, ? FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
                }
            }

            // Arithmetic test #2 with a mixture of literal values and CriteriaBuilder parameters
            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam6 = cb3.parameter(Integer.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal1), intParam6);
            cquery3.where(cb3.notEqual(root3.get(OLGH17837Entity_.intVal1), cb3.diff(cb3.literal(4), intParam6)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam6, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDerby) {
                    Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - 2))", sql.remove(0));
                } else if (isDB2Z || isDB2) {
                    Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1, ? FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (4 - ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1, ? FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
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

    public void testSUB1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            Query query = em.createQuery("SELECT s.strVal1, ?2 FROM OLGH17837Entity s WHERE s.intVal1 != (?1 - ?2)");
            query.setParameter(1, 4);
            query.setParameter(2, 2);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - 2))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1, ? FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1, 2 FROM OLGH17837Entity s WHERE s.intVal1 != (4 - 2)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - 2))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1, ? FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1, ?2 FROM OLGH17837Entity s WHERE s.intVal1 != (4 - ?2)");
            query.setParameter(2, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - 2))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1, ? FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1), intParam2);
            cquery.where(cb.notEqual(root.get(OLGH17837Entity_.intVal1), cb.diff(intParam1, intParam2)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 4);
            query.setParameter(intParam2, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - 2))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1, ? FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1), cb2.literal(2));
            cquery2.where(cb2.notEqual(root2.get(OLGH17837Entity_.intVal1), cb2.diff(cb2.literal(4), cb2.literal(2))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - 2))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1, ? FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam6 = cb3.parameter(Integer.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal1), intParam6);
            cquery3.where(cb3.notEqual(root3.get(OLGH17837Entity_.intVal1), cb3.diff(cb3.literal(4), intParam6)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam6, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDerby) {
                Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - 2))", sql.remove(0));
            } else if (isDB2Z || isDB2) {
                Assert.assertEquals("SELECT STRVAL1, 2 FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1, ? FROM OLGH17837ENTITY WHERE (INTVAL1 <> (? - ?))", sql.remove(0));
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
