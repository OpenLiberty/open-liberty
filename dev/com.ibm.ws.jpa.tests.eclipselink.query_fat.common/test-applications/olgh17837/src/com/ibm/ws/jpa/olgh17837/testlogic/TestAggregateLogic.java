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

public class TestAggregateLogic extends AbstractTestLogic {

    public void testAvg1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT AVG(?1) FROM OLGH17837Entity s");
            query.setParameter(1, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT AVG(1) FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(cb.avg(intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.avg(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY", sql.remove(0));
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

    public void testAvg1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT AVG(?1) FROM OLGH17837Entity s");
            query.setParameter(1, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT AVG(1) FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(cb.avg(intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.avg(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY", sql.remove(0));
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

    public void testAvg1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT AVG(?1) FROM OLGH17837Entity s");
            query.setParameter(1, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT AVG(1) FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(cb.avg(intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.avg(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY", sql.remove(0));
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

    public void testAvg2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT AVG(?1) FROM OLGH17837Entity s HAVING ?2 < AVG(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (0 < AVG(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT AVG(1) FROM OLGH17837Entity s HAVING 0 < AVG(1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (0 < AVG(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT AVG(1) FROM OLGH17837Entity s HAVING ?1 < AVG(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (0 < AVG(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Double> intParam2 = cb.parameter(Double.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.multiselect(cb.avg(intParam1));
            cquery.having(cb.lessThan(intParam2, cb.avg(intParam3)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 0d);
            query.setParameter(intParam3, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (0.0 < AVG(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.avg(cb2.literal(1)));
            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.avg(cb2.literal(1))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (0.0 < AVG(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(cb3.avg(cb3.literal(1)));
            ParameterExpression<Double> intParam4 = cb3.parameter(Double.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.having(cb3.lessThan(intParam4, cb3.avg(intParam5)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 0d);
            query.setParameter(intParam5, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (0.0 < AVG(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
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

    public void testAvg2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT AVG(?1) FROM OLGH17837Entity s HAVING ?2 < AVG(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT AVG(1) FROM OLGH17837Entity s HAVING 0 < AVG(1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (0 < AVG(1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT AVG(1) FROM OLGH17837Entity s HAVING ?1 < AVG(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(1))", sql.remove(0));
                } else if (isDB2) {
                    Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Double> intParam2 = cb.parameter(Double.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.multiselect(cb.avg(intParam1));
            cquery.having(cb.lessThan(intParam2, cb.avg(intParam3)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 0d);
            query.setParameter(intParam3, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.avg(cb2.literal(1)));
            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.avg(cb2.literal(1))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (0.0 < AVG(1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(cb3.avg(cb3.literal(1)));
            ParameterExpression<Double> intParam4 = cb3.parameter(Double.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.having(cb3.lessThan(intParam4, cb3.avg(intParam5)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 0d);
            query.setParameter(intParam5, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(1))", sql.remove(0));
                } else if (isDB2) {
                    Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
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

    public void testAvg2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT AVG(?1) FROM OLGH17837Entity s HAVING ?2 < AVG(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT AVG(1) FROM OLGH17837Entity s HAVING 0 < AVG(1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT AVG(1) FROM OLGH17837Entity s HAVING ?1 < AVG(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Double> intParam2 = cb.parameter(Double.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.multiselect(cb.avg(intParam1));
            cquery.having(cb.lessThan(intParam2, cb.avg(intParam3)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 0d);
            query.setParameter(intParam3, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.avg(cb2.literal(1)));
            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.avg(cb2.literal(1))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(cb3.avg(cb3.literal(1)));
            ParameterExpression<Double> intParam4 = cb3.parameter(Double.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.having(cb3.lessThan(intParam4, cb3.avg(intParam5)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 0d);
            query.setParameter(intParam5, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(?))", sql.remove(0));
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

    public void testAvgDistinct1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT AVG(DISTINCT ?1) FROM OLGH17837Entity s");
            query.setParameter(1, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT AVG(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT AVG(DISTINCT 1) FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT AVG(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
            }

            // -----------------------

            /*
             * Currently, the JPA Criteria API does not support DISTINCT for Aggregates
             * TODO: https://github.com/eclipse-ee4j/jpa-api/issues/326
             */

//            CriteriaBuilder cb = em.getCriteriaBuilder();
//            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
//            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
//            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
//            cquery.multiselect(cb.avgDistinct(intParam1));
//
//            query = em.createQuery(cquery);
//            query.setParameter(intParam1, 1);
//            query.getResultList();
//            sql = SQLCallListener.getAndClearCallList();
//            Assert.assertEquals(1, sql.size());
//            if(isDB2Z || isDB2 ||  isDerby) {
//                Assert.assertEquals("SELECT AVG(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
//            } else {
//                Assert.assertEquals("SELECT AVG(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
//            }
//
//            CriteriaBuilder cb2 = em.getCriteriaBuilder();
//            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
//            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
//            cquery2.multiselect(cb2.avgDistinct(cb2.literal(1)));
//
//            query = em.createQuery(cquery2);
//            query.getResultList();
//            sql = SQLCallListener.getAndClearCallList();
//            Assert.assertEquals(1, sql.size());
//            if(isDB2Z || isDB2 || isDerby) {
//                Assert.assertEquals("SELECT AVG(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
//            } else {
//                Assert.assertEquals("SELECT AVG(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
//            }
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

    public void testAvgDistinct1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT AVG(DISTINCT ?1) FROM OLGH17837Entity s");
            query.setParameter(1, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT AVG(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT AVG(DISTINCT 1) FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT AVG(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
            }

            // -----------------------

            /*
             * Currently, the JPA Criteria API does not support DISTINCT for Aggregates
             * TODO: https://github.com/eclipse-ee4j/jpa-api/issues/326
             */

//            CriteriaBuilder cb = em.getCriteriaBuilder();
//            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
//            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
//            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
//            cquery.multiselect(cb.avgDistinct(intParam1));
//
//            query = em.createQuery(cquery);
//            query.setParameter(intParam1, 1);
//            query.getResultList();
//            sql = SQLCallListener.getAndClearCallList();
//            Assert.assertEquals(1, sql.size());
//            if(isDB2Z || isDB2 || isDerby) {
//                Assert.assertEquals("SELECT AVG(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
//            } else {
//                Assert.assertEquals("SELECT AVG(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
//            }
//
//            CriteriaBuilder cb2 = em.getCriteriaBuilder();
//            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
//            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
//            cquery2.multiselect(cb2.avgDistinct(cb2.literal(1)));
//
//            query = em.createQuery(cquery2);
//            query.getResultList();
//            sql = SQLCallListener.getAndClearCallList();
//            Assert.assertEquals(1, sql.size());
//            if(isDB2Z || isDB2 || isDerby) {
//                Assert.assertEquals("SELECT AVG(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
//            } else {
//                Assert.assertEquals("SELECT AVG(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
//            }
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

    public void testAvgDistinct1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT AVG(DISTINCT ?1) FROM OLGH17837Entity s");
            query.setParameter(1, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT AVG(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT AVG(DISTINCT 1) FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT AVG(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
            }

            // -----------------------

            /*
             * Currently, the JPA Criteria API does not support DISTINCT for Aggregates
             * TODO: https://github.com/eclipse-ee4j/jpa-api/issues/326
             */

//            CriteriaBuilder cb = em.getCriteriaBuilder();
//            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
//            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
//            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
//            cquery.multiselect(cb.avgDistinct(intParam1));
//
//            query = em.createQuery(cquery);
//            query.setParameter(intParam1, 1);
//            query.getResultList();
//            sql = SQLCallListener.getAndClearCallList();
//            Assert.assertEquals(1, sql.size());
//            if(isDB2Z || isDB2 || isDerby) {
//                Assert.assertEquals("SELECT AVG(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
//            } else {
//                Assert.assertEquals("SELECT AVG(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
//            }
//
//            CriteriaBuilder cb2 = em.getCriteriaBuilder();
//            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
//            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
//            cquery2.multiselect(cb2.avgDistinct(cb2.literal(1)));
//
//            query = em.createQuery(cquery2);
//            query.getResultList();
//            sql = SQLCallListener.getAndClearCallList();
//            Assert.assertEquals(1, sql.size());
//            if(isDB2Z || isDB2 || isDerby) {
//                Assert.assertEquals("SELECT AVG(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
//            } else {
//                Assert.assertEquals("SELECT AVG(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
//            }
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

    public void testAvgDistinct2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT AVG(?1) FROM OLGH17837Entity s HAVING ?2 < AVG(DISTINCT ?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (0 < AVG(DISTINCT(1)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(?)))", sql.remove(0));
            }

            query = em.createQuery("SELECT AVG(1) FROM OLGH17837Entity s HAVING 0 < AVG(DISTINCT 1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (0 < AVG(DISTINCT(1)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(?)))", sql.remove(0));
            }

            query = em.createQuery("SELECT AVG(1) FROM OLGH17837Entity s HAVING ?1 < AVG(DISTINCT ?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (0 < AVG(DISTINCT(1)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(?)))", sql.remove(0));
            }

            // -----------------------

            /*
             * Currently, the JPA Criteria API does not support DISTINCT for Aggregates
             * TODO: https://github.com/eclipse-ee4j/jpa-api/issues/326
             */

//            CriteriaBuilder cb = em.getCriteriaBuilder();
//            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
//            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
//            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
//            ParameterExpression<Double> intParam2 = cb.parameter(Double.class);
//            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
//            cquery.multiselect(cb.avg(intParam1));
//            cquery.having(cb.lessThan(intParam2, cb.avgDistinct(intParam3)));
//
//            query = em.createQuery(cquery);
//            query.setParameter(intParam1, 1);
//            query.setParameter(intParam2, 0d);
//            query.setParameter(intParam3, 1);
//            query.getResultList();
//            sql = SQLCallListener.getAndClearCallList();
//            Assert.assertEquals(1, sql.size());
//            if(isDB2Z || isDB2 ||  isDerby) {
//                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (0 < AVG(DISTINCT(1)))", sql.remove(0));
//            } else {
//                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(?)))", sql.remove(0));
//            }
//
//            CriteriaBuilder cb2 = em.getCriteriaBuilder();
//            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
//            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
//            cquery2.multiselect(cb2.avg(cb2.literal(1)));
//            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.avgDistinct(cb2.literal(1))));
//
//            query = em.createQuery(cquery2);
//            query.getResultList();
//            sql = SQLCallListener.getAndClearCallList();
//            Assert.assertEquals(1, sql.size());
//            if(isDB2Z || isDB2 ||  isDerby) {
//                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (0 < AVG(DISTINCT(1)))", sql.remove(0));
//            } else {
//                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(?)))", sql.remove(0));
//            }
//
//            CriteriaBuilder cb3 = em.getCriteriaBuilder();
//            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
//            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
//            cquery3.multiselect(cb3.avg(cb3.literal(1)));
//            ParameterExpression<Double> intParam4 = cb3.parameter(Double.class);
//            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
//            cquery3.having(cb3.lessThan(intParam4, cb3.avgDistinct(intParam5)));
//
//            query = em.createQuery(cquery3);
//            query.setParameter(intParam4, 0d);
//            query.setParameter(intParam5, 1);
//            query.getResultList();
//            sql = SQLCallListener.getAndClearCallList();
//            Assert.assertEquals(1, sql.size());
//            if(isDB2Z || isDB2 ||  isDerby) {
//                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (0 < AVG(DISTINCT(1)))", sql.remove(0));
//            } else {
//                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(?)))", sql.remove(0));
//            }
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

    public void testAvgDistinct2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT AVG(?1) FROM OLGH17837Entity s HAVING ?2 < AVG(DISTINCT ?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(1)))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(1)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(?)))", sql.remove(0));
            }

            query = em.createQuery("SELECT AVG(1) FROM OLGH17837Entity s HAVING 0 < AVG(DISTINCT 1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2) {
                    Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(1)))", sql.remove(0));
                } else if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(1)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(?)))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (0 < AVG(DISTINCT(1)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(?)))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT AVG(1) FROM OLGH17837Entity s HAVING ?1 < AVG(DISTINCT ?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(1)))", sql.remove(0));
                } else if (isDB2) {
                    Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(1)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(?)))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(1)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(?)))", sql.remove(0));
                }
            }

            // -----------------------

            /*
             * Currently, the JPA Criteria API does not support DISTINCT for Aggregates
             * TODO: https://github.com/eclipse-ee4j/jpa-api/issues/326
             */

//            CriteriaBuilder cb = em.getCriteriaBuilder();
//            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
//            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
//            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
//            ParameterExpression<Double> intParam2 = cb.parameter(Double.class);
//            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
//            cquery.multiselect(cb.avg(intParam1));
//            cquery.having(cb.lessThan(intParam2, cb.avgDistinct(intParam3)));
//
//            query = em.createQuery(cquery);
//            query.setParameter(intParam1, 1);
//            query.setParameter(intParam2, 0d);
//            query.setParameter(intParam3, 1);
//            query.getResultList();
//            sql = SQLCallListener.getAndClearCallList();
//            Assert.assertEquals(1, sql.size());
//            if(isDB2Z || isDB2 || isDerby) {
//                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(1)))", sql.remove(0));
//            } else {
//                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(?)))", sql.remove(0));
//            }
//
//            CriteriaBuilder cb2 = em.getCriteriaBuilder();
//            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
//            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
//            cquery2.multiselect(cb2.avg(cb2.literal(1)));
//            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.avgDistinct(cb2.literal(1))));
//
//            query = em.createQuery(cquery2);
//            query.getResultList();
//            sql = SQLCallListener.getAndClearCallList();
//            Assert.assertEquals(1, sql.size());
//            if(isDB2Z || isDB2 || isDerby) {
//                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (0 < AVG(DISTINCT(1)))", sql.remove(0));
//            } else {
//                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(?)))", sql.remove(0));
//            }
//
//            CriteriaBuilder cb3 = em.getCriteriaBuilder();
//            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
//            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
//            cquery3.multiselect(cb3.avg(cb3.literal(1)));
//            ParameterExpression<Double> intParam4 = cb3.parameter(Double.class);
//            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
//            cquery3.having(cb3.lessThan(intParam4, cb3.avgDistinct(intParam5)));
//
//            query = em.createQuery(cquery3);
//            query.setParameter(intParam4, 0d);
//            query.setParameter(intParam5, 1);
//            query.getResultList();
//            sql = SQLCallListener.getAndClearCallList();
//            Assert.assertEquals(1, sql.size());
//            if(isDB2Z || isDB2 || isDerby) {
//                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(1)))", sql.remove(0));
//            } else {
//                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(?)))", sql.remove(0));
//            }
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

    public void testAvgDistinct2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT AVG(?1) FROM OLGH17837Entity s HAVING ?2 < AVG(DISTINCT ?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(1)))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(1)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(?)))", sql.remove(0));
            }

            query = em.createQuery("SELECT AVG(1) FROM OLGH17837Entity s HAVING 0 < AVG(DISTINCT 1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(1)))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(1)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(?)))", sql.remove(0));
            }

            query = em.createQuery("SELECT AVG(1) FROM OLGH17837Entity s HAVING ?1 < AVG(DISTINCT ?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(1)))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(1)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(?)))", sql.remove(0));
            }

            // -----------------------

            /*
             * Currently, the JPA Criteria API does not support DISTINCT for Aggregates
             * TODO: https://github.com/eclipse-ee4j/jpa-api/issues/326
             */

//            CriteriaBuilder cb = em.getCriteriaBuilder();
//            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
//            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
//            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
//            ParameterExpression<Double> intParam2 = cb.parameter(Double.class);
//            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
//            cquery.multiselect(cb.avg(intParam1));
//            cquery.having(cb.lessThan(intParam2, cb.avgDistinct(intParam3)));
//
//            query = em.createQuery(cquery);
//            query.setParameter(intParam1, 1);
//            query.setParameter(intParam2, 0d);
//            query.setParameter(intParam3, 1);
//            query.getResultList();
//            sql = SQLCallListener.getAndClearCallList();
//            Assert.assertEquals(1, sql.size());
//            if(isDB2Z || isDB2 || isDerby) {
//                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(1)))", sql.remove(0));
//            } else {
//                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(?)))", sql.remove(0));
//            }
//
//            CriteriaBuilder cb2 = em.getCriteriaBuilder();
//            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
//            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
//            cquery2.multiselect(cb2.avg(cb2.literal(1)));
//            cquery2.having(cb2.lessThan(cb2.literal(0d), cb2.avgDistinct(cb2.literal(1))));
//
//            query = em.createQuery(cquery2);
//            query.getResultList();
//            sql = SQLCallListener.getAndClearCallList();
//            Assert.assertEquals(1, sql.size());
//            if(isDB2Z || isDB2 || isDerby) {
//                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(1)))", sql.remove(0));
//            } else {
//                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(?)))", sql.remove(0));
//            }
//
//            CriteriaBuilder cb3 = em.getCriteriaBuilder();
//            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
//            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
//            cquery3.multiselect(cb3.avg(cb3.literal(1)));
//            ParameterExpression<Double> intParam4 = cb3.parameter(Double.class);
//            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
//            cquery3.having(cb3.lessThan(intParam4, cb3.avgDistinct(intParam5)));
//
//            query = em.createQuery(cquery3);
//            query.setParameter(intParam4, 0d);
//            query.setParameter(intParam5, 1);
//            query.getResultList();
//            sql = SQLCallListener.getAndClearCallList();
//            Assert.assertEquals(1, sql.size());
//            if(isDB2Z || isDB2 || isDerby) {
//                Assert.assertEquals("SELECT AVG(1) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(1)))", sql.remove(0));
//            } else {
//                Assert.assertEquals("SELECT AVG(?) FROM OLGH17837ENTITY HAVING (? < AVG(DISTINCT(?)))", sql.remove(0));
//            }
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

    public void testCount1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT COUNT(?1) FROM OLGH17837Entity s");
            query.setParameter(1, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT COUNT(1) FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(cb.count(intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.count(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY", sql.remove(0));
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

    public void testCount1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT COUNT(?1) FROM OLGH17837Entity s");
            query.setParameter(1, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT COUNT(1) FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(cb.count(intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.count(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY", sql.remove(0));
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

    public void testCount1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT COUNT(?1) FROM OLGH17837Entity s");
            query.setParameter(1, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT COUNT(1) FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(cb.count(intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.count(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY", sql.remove(0));
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

    public void testCount2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT COUNT(?1) FROM OLGH17837Entity s HAVING ?2 < COUNT(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (0 < COUNT(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT COUNT(1) FROM OLGH17837Entity s HAVING 0 < COUNT(1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (0 < COUNT(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT COUNT(1) FROM OLGH17837Entity s HAVING ?1 < COUNT(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (0 < COUNT(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Long> intParam2 = cb.parameter(Long.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.multiselect(cb.count(intParam1));
            cquery.having(cb.lessThan(intParam2, cb.count(intParam3)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 0L);
            query.setParameter(intParam3, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (0 < COUNT(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.count(cb2.literal(1)));
            cquery2.having(cb2.lessThan(cb2.literal(0L), cb2.count(cb2.literal(1))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (0 < COUNT(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(cb3.count(cb3.literal(1)));
            ParameterExpression<Long> intParam4 = cb3.parameter(Long.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.having(cb3.lessThan(intParam4, cb3.count(intParam5)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 0L);
            query.setParameter(intParam5, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (0 < COUNT(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(?))", sql.remove(0));
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

    public void testCount2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT COUNT(?1) FROM OLGH17837Entity s HAVING ?2 < COUNT(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT COUNT(1) FROM OLGH17837Entity s HAVING 0 < COUNT(1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (0 < COUNT(1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT COUNT(1) FROM OLGH17837Entity s HAVING ?1 < COUNT(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Long> intParam2 = cb.parameter(Long.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.multiselect(cb.count(intParam1));
            cquery.having(cb.lessThan(intParam2, cb.count(intParam3)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 0L);
            query.setParameter(intParam3, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.count(cb2.literal(1)));
            cquery2.having(cb2.lessThan(cb2.literal(0L), cb2.count(cb2.literal(1))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (0 < COUNT(1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(cb3.count(cb3.literal(1)));
            ParameterExpression<Long> intParam4 = cb3.parameter(Long.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.having(cb3.lessThan(intParam4, cb3.count(intParam5)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 0L);
            query.setParameter(intParam5, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(?))", sql.remove(0));
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

    public void testCount2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT COUNT(?1) FROM OLGH17837Entity s HAVING ?2 < COUNT(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT COUNT(1) FROM OLGH17837Entity s HAVING 0 < COUNT(1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT COUNT(1) FROM OLGH17837Entity s HAVING ?1 < COUNT(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Long> intParam2 = cb.parameter(Long.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.multiselect(cb.count(intParam1));
            cquery.having(cb.lessThan(intParam2, cb.count(intParam3)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 0L);
            query.setParameter(intParam3, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.count(cb2.literal(1)));
            cquery2.having(cb2.lessThan(cb2.literal(0L), cb2.count(cb2.literal(1))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(cb3.count(cb3.literal(1)));
            ParameterExpression<Long> intParam4 = cb3.parameter(Long.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.having(cb3.lessThan(intParam4, cb3.count(intParam5)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 0L);
            query.setParameter(intParam5, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(?))", sql.remove(0));
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

    public void testCountDistinct1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT COUNT(DISTINCT ?1) FROM OLGH17837Entity s");
            query.setParameter(1, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT COUNT(DISTINCT 1) FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(cb.countDistinct(intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.countDistinct(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
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

    public void testCountDistinct1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT COUNT(DISTINCT ?1) FROM OLGH17837Entity s");
            query.setParameter(1, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT COUNT(DISTINCT 1) FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(cb.countDistinct(intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.countDistinct(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
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

    public void testCountDistinct1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT COUNT(DISTINCT ?1) FROM OLGH17837Entity s");
            query.setParameter(1, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT COUNT(DISTINCT 1) FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(cb.countDistinct(intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.countDistinct(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(DISTINCT(1)) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(DISTINCT(?)) FROM OLGH17837ENTITY", sql.remove(0));
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

    public void testCountDistinct2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT COUNT(?1) FROM OLGH17837Entity s HAVING ?2 < COUNT(DISTINCT ?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (0 < COUNT(DISTINCT(1)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(?)))", sql.remove(0));
            }

            query = em.createQuery("SELECT COUNT(1) FROM OLGH17837Entity s HAVING 0 < COUNT(DISTINCT 1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (0 < COUNT(DISTINCT(1)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(?)))", sql.remove(0));
            }

            query = em.createQuery("SELECT COUNT(1) FROM OLGH17837Entity s HAVING ?1 < COUNT(DISTINCT ?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (0 < COUNT(DISTINCT(1)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(?)))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Long> intParam2 = cb.parameter(Long.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.multiselect(cb.count(intParam1));
            cquery.having(cb.lessThan(intParam2, cb.countDistinct(intParam3)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 0L);
            query.setParameter(intParam3, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (0 < COUNT(DISTINCT(1)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(?)))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.count(cb2.literal(1)));
            cquery2.having(cb2.lessThan(cb2.literal(0L), cb2.countDistinct(cb2.literal(1))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (0 < COUNT(DISTINCT(1)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(?)))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(cb3.count(cb3.literal(1)));
            ParameterExpression<Long> intParam4 = cb3.parameter(Long.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.having(cb3.lessThan(intParam4, cb3.countDistinct(intParam5)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 0L);
            query.setParameter(intParam5, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (0 < COUNT(DISTINCT(1)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(?)))", sql.remove(0));
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

    public void testCountDistinct2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT COUNT(?1) FROM OLGH17837Entity s HAVING ?2 < COUNT(DISTINCT ?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(1)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(?)))", sql.remove(0));
            }

            query = em.createQuery("SELECT COUNT(1) FROM OLGH17837Entity s HAVING 0 < COUNT(DISTINCT 1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(1)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(?)))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (0 < COUNT(DISTINCT(1)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(?)))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT COUNT(1) FROM OLGH17837Entity s HAVING ?1 < COUNT(DISTINCT ?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(1)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(?)))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Long> intParam2 = cb.parameter(Long.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.multiselect(cb.count(intParam1));
            cquery.having(cb.lessThan(intParam2, cb.countDistinct(intParam3)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 0L);
            query.setParameter(intParam3, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(1)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(?)))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.count(cb2.literal(1)));
            cquery2.having(cb2.lessThan(cb2.literal(0L), cb2.countDistinct(cb2.literal(1))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(1)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(?)))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (0 < COUNT(DISTINCT(1)))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(?)))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(cb3.count(cb3.literal(1)));
            ParameterExpression<Long> intParam4 = cb3.parameter(Long.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.having(cb3.lessThan(intParam4, cb3.countDistinct(intParam5)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 0L);
            query.setParameter(intParam5, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(1)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(?)))", sql.remove(0));
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

    public void testCountDistinct2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT COUNT(?1) FROM OLGH17837Entity s HAVING ?2 < COUNT(DISTINCT ?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(1)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(?)))", sql.remove(0));
            }

            query = em.createQuery("SELECT COUNT(1) FROM OLGH17837Entity s HAVING 0 < COUNT(DISTINCT 1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(1)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(?)))", sql.remove(0));
            }

            query = em.createQuery("SELECT COUNT(1) FROM OLGH17837Entity s HAVING ?1 < COUNT(DISTINCT ?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(1)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(?)))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Long> intParam2 = cb.parameter(Long.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.multiselect(cb.count(intParam1));
            cquery.having(cb.lessThan(intParam2, cb.countDistinct(intParam3)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 0L);
            query.setParameter(intParam3, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(1)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(?)))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.count(cb2.literal(1)));
            cquery2.having(cb2.lessThan(cb2.literal(0L), cb2.countDistinct(cb2.literal(1))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(1)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(?)))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(cb3.count(cb3.literal(1)));
            ParameterExpression<Long> intParam4 = cb3.parameter(Long.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.having(cb3.lessThan(intParam4, cb3.countDistinct(intParam5)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 0L);
            query.setParameter(intParam5, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT COUNT(1) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(1)))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT COUNT(?) FROM OLGH17837ENTITY HAVING (? < COUNT(DISTINCT(?)))", sql.remove(0));
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

    public void testDistinct1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT DISTINCT(?1) FROM OLGH17837Entity s WHERE s.strVal1 = ?2");
            query.setParameter(1, "HELLO");
            query.setParameter(2, "WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT DISTINCT 'HELLO' FROM OLGH17837ENTITY WHERE (STRVAL1 = 'WORLD')", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT DISTINCT ? FROM OLGH17837ENTITY WHERE (STRVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT DISTINCT('HELLO') FROM OLGH17837Entity s WHERE s.strVal1 = 'WORLD'");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT DISTINCT 'HELLO' FROM OLGH17837ENTITY WHERE (STRVAL1 = 'WORLD')", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT DISTINCT ? FROM OLGH17837ENTITY WHERE (STRVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            cquery.multiselect(strParam1).distinct(true);
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), strParam2));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.setParameter(strParam2, "WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT DISTINCT 'HELLO' FROM OLGH17837ENTITY WHERE (STRVAL1 = 'WORLD')", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT DISTINCT ? FROM OLGH17837ENTITY WHERE (STRVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.literal("HELLO")).distinct(true);
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), "WORLD"));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT DISTINCT 'HELLO' FROM OLGH17837ENTITY WHERE (STRVAL1 = 'WORLD')", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT DISTINCT ? FROM OLGH17837ENTITY WHERE (STRVAL1 = ?)", sql.remove(0));
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

    public void testDistinct1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT DISTINCT(?1) FROM OLGH17837Entity s WHERE s.strVal1 = ?2");
            query.setParameter(1, "HELLO");
            query.setParameter(2, "WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT DISTINCT 'HELLO' FROM OLGH17837ENTITY WHERE (STRVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT DISTINCT ? FROM OLGH17837ENTITY WHERE (STRVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT DISTINCT('HELLO') FROM OLGH17837Entity s WHERE s.strVal1 = 'WORLD'");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT DISTINCT 'HELLO' FROM OLGH17837ENTITY WHERE (STRVAL1 = ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT DISTINCT ? FROM OLGH17837ENTITY WHERE (STRVAL1 = ?)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT DISTINCT 'HELLO' FROM OLGH17837ENTITY WHERE (STRVAL1 = 'WORLD')", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT DISTINCT ? FROM OLGH17837ENTITY WHERE (STRVAL1 = ?)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            cquery.multiselect(strParam1).distinct(true);
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), strParam2));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.setParameter(strParam2, "WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT DISTINCT 'HELLO' FROM OLGH17837ENTITY WHERE (STRVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT DISTINCT ? FROM OLGH17837ENTITY WHERE (STRVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.literal("HELLO")).distinct(true);
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), "WORLD"));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT DISTINCT 'HELLO' FROM OLGH17837ENTITY WHERE (STRVAL1 = ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT DISTINCT ? FROM OLGH17837ENTITY WHERE (STRVAL1 = ?)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT DISTINCT 'HELLO' FROM OLGH17837ENTITY WHERE (STRVAL1 = 'WORLD')", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT DISTINCT ? FROM OLGH17837ENTITY WHERE (STRVAL1 = ?)", sql.remove(0));
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

    public void testDistinct1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT DISTINCT(?1) FROM OLGH17837Entity s WHERE s.strVal1 = ?2");
            query.setParameter(1, "HELLO");
            query.setParameter(2, "WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT DISTINCT 'HELLO' FROM OLGH17837ENTITY WHERE (STRVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT DISTINCT ? FROM OLGH17837ENTITY WHERE (STRVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT DISTINCT('HELLO') FROM OLGH17837Entity s WHERE s.strVal1 = 'WORLD'");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT DISTINCT 'HELLO' FROM OLGH17837ENTITY WHERE (STRVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT DISTINCT ? FROM OLGH17837ENTITY WHERE (STRVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            cquery.multiselect(strParam1).distinct(true);
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), strParam2));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.setParameter(strParam2, "WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT DISTINCT 'HELLO' FROM OLGH17837ENTITY WHERE (STRVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT DISTINCT ? FROM OLGH17837ENTITY WHERE (STRVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.literal("HELLO")).distinct(true);
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), "WORLD"));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT DISTINCT 'HELLO' FROM OLGH17837ENTITY WHERE (STRVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT DISTINCT ? FROM OLGH17837ENTITY WHERE (STRVAL1 = ?)", sql.remove(0));
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

    public void testMax1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT MAX(?1) FROM OLGH17837Entity s HAVING ?2 < MAX(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MAX(1) FROM OLGH17837ENTITY HAVING (0 < MAX(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MAX(?) FROM OLGH17837ENTITY HAVING (? < MAX(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT MAX(1) FROM OLGH17837Entity s HAVING 0 < MAX(1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MAX(1) FROM OLGH17837ENTITY HAVING (0 < MAX(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MAX(?) FROM OLGH17837ENTITY HAVING (? < MAX(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT MAX(1) FROM OLGH17837Entity s HAVING ?1 < MAX(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MAX(1) FROM OLGH17837ENTITY HAVING (0 < MAX(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MAX(?) FROM OLGH17837ENTITY HAVING (? < MAX(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.multiselect(cb.max(intParam1));
            cquery.having(cb.lessThan(intParam2, cb.max(intParam3)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 0);
            query.setParameter(intParam3, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MAX(1) FROM OLGH17837ENTITY HAVING (0 < MAX(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MAX(?) FROM OLGH17837ENTITY HAVING (? < MAX(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.max(cb2.literal(1)));
            cquery2.having(cb2.lessThan(cb2.literal(0), cb2.max(cb2.literal(1))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MAX(1) FROM OLGH17837ENTITY HAVING (0 < MAX(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MAX(?) FROM OLGH17837ENTITY HAVING (? < MAX(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(cb3.max(cb3.literal(1)));
            ParameterExpression<Integer> intParam4 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.having(cb3.lessThan(intParam4, cb3.max(intParam5)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 0);
            query.setParameter(intParam5, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MAX(1) FROM OLGH17837ENTITY HAVING (0 < MAX(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MAX(?) FROM OLGH17837ENTITY HAVING (? < MAX(?))", sql.remove(0));
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

    public void testMax1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT MAX(?1) FROM OLGH17837Entity s HAVING ?2 < MAX(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MAX(1) FROM OLGH17837ENTITY HAVING (? < MAX(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MAX(?) FROM OLGH17837ENTITY HAVING (? < MAX(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT MAX(1) FROM OLGH17837Entity s HAVING 0 < MAX(1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT MAX(1) FROM OLGH17837ENTITY HAVING (? < MAX(1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT MAX(?) FROM OLGH17837ENTITY HAVING (? < MAX(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT MAX(1) FROM OLGH17837ENTITY HAVING (0 < MAX(1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT MAX(?) FROM OLGH17837ENTITY HAVING (? < MAX(?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT MAX(1) FROM OLGH17837Entity s HAVING ?1 < MAX(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MAX(1) FROM OLGH17837ENTITY HAVING (? < MAX(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MAX(?) FROM OLGH17837ENTITY HAVING (? < MAX(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.multiselect(cb.max(intParam1));
            cquery.having(cb.lessThan(intParam2, cb.max(intParam3)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 0);
            query.setParameter(intParam3, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MAX(1) FROM OLGH17837ENTITY HAVING (? < MAX(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MAX(?) FROM OLGH17837ENTITY HAVING (? < MAX(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.max(cb2.literal(1)));
            cquery2.having(cb2.lessThan(cb2.literal(0), cb2.max(cb2.literal(1))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT MAX(1) FROM OLGH17837ENTITY HAVING (? < MAX(1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT MAX(?) FROM OLGH17837ENTITY HAVING (? < MAX(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT MAX(1) FROM OLGH17837ENTITY HAVING (0 < MAX(1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT MAX(?) FROM OLGH17837ENTITY HAVING (? < MAX(?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(cb3.max(cb3.literal(1)));
            ParameterExpression<Integer> intParam4 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.having(cb3.lessThan(intParam4, cb3.max(intParam5)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 0);
            query.setParameter(intParam5, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MAX(1) FROM OLGH17837ENTITY HAVING (? < MAX(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MAX(?) FROM OLGH17837ENTITY HAVING (? < MAX(?))", sql.remove(0));
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

    public void testMax1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT MAX(?1) FROM OLGH17837Entity s HAVING ?2 < MAX(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MAX(1) FROM OLGH17837ENTITY HAVING (? < MAX(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MAX(?) FROM OLGH17837ENTITY HAVING (? < MAX(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT MAX(1) FROM OLGH17837Entity s HAVING 0 < MAX(1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MAX(1) FROM OLGH17837ENTITY HAVING (? < MAX(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MAX(?) FROM OLGH17837ENTITY HAVING (? < MAX(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT MAX(1) FROM OLGH17837Entity s HAVING ?1 < MAX(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MAX(1) FROM OLGH17837ENTITY HAVING (? < MAX(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MAX(?) FROM OLGH17837ENTITY HAVING (? < MAX(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.multiselect(cb.max(intParam1));
            cquery.having(cb.lessThan(intParam2, cb.max(intParam3)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 0);
            query.setParameter(intParam3, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MAX(1) FROM OLGH17837ENTITY HAVING (? < MAX(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MAX(?) FROM OLGH17837ENTITY HAVING (? < MAX(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.max(cb2.literal(1)));
            cquery2.having(cb2.lessThan(cb2.literal(0), cb2.max(cb2.literal(1))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MAX(1) FROM OLGH17837ENTITY HAVING (? < MAX(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MAX(?) FROM OLGH17837ENTITY HAVING (? < MAX(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(cb3.max(cb3.literal(1)));
            ParameterExpression<Integer> intParam4 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.having(cb3.lessThan(intParam4, cb3.max(intParam5)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 0);
            query.setParameter(intParam5, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MAX(1) FROM OLGH17837ENTITY HAVING (? < MAX(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MAX(?) FROM OLGH17837ENTITY HAVING (? < MAX(?))", sql.remove(0));
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

    public void testMax2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING MAX(s.intVal1) > ?2");
            query.setParameter(2, 8);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > ?)", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING MAX(s.intVal1) > 8");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > ?)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > 8)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > ?)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> longValue = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.groupBy(root.get(OLGH17837Entity_.intVal1));
            cquery.having(cb.greaterThan(cb.max(root.get(OLGH17837Entity_.intVal1)), longValue));

            query = em.createQuery(cquery);
            query.setParameter(longValue, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > ?)", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.groupBy(root2.get(OLGH17837Entity_.intVal1));
            cquery2.having(cb2.greaterThan(cb2.max(root2.get(OLGH17837Entity_.intVal1)), 8));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > ?)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > 8)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > ?)", sql.remove(0));
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

    public void testMax2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING MAX(s.intVal1) > ?2");
            query.setParameter(2, 8);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > ?)", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING MAX(s.intVal1) > 8");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > ?)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > 8)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > ?)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> longValue = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.groupBy(root.get(OLGH17837Entity_.intVal1));
            cquery.having(cb.greaterThan(cb.max(root.get(OLGH17837Entity_.intVal1)), longValue));

            query = em.createQuery(cquery);
            query.setParameter(longValue, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > ?)", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.groupBy(root2.get(OLGH17837Entity_.intVal1));
            cquery2.having(cb2.greaterThan(cb2.max(root2.get(OLGH17837Entity_.intVal1)), 8));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > ?)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > 8)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > ?)", sql.remove(0));
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

    public void testMax2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING MAX(s.intVal1) > ?2");
            query.setParameter(2, 8);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > ?)", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING MAX(s.intVal1) > 8");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > ?)", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> longValue = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.groupBy(root.get(OLGH17837Entity_.intVal1));
            cquery.having(cb.greaterThan(cb.max(root.get(OLGH17837Entity_.intVal1)), longValue));

            query = em.createQuery(cquery);
            query.setParameter(longValue, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > ?)", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.groupBy(root2.get(OLGH17837Entity_.intVal1));
            cquery2.having(cb2.greaterThan(cb2.max(root2.get(OLGH17837Entity_.intVal1)), 8));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MAX(INTVAL1) > ?)", sql.remove(0));
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

    public void testMin1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT MIN(?1) FROM OLGH17837Entity s HAVING ?2 < MIN(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MIN(1) FROM OLGH17837ENTITY HAVING (0 < MIN(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MIN(?) FROM OLGH17837ENTITY HAVING (? < MIN(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT MIN(1) FROM OLGH17837Entity s HAVING 0 < MIN(1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MIN(1) FROM OLGH17837ENTITY HAVING (0 < MIN(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MIN(?) FROM OLGH17837ENTITY HAVING (? < MIN(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT MIN(1) FROM OLGH17837Entity s HAVING ?1 < MIN(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MIN(1) FROM OLGH17837ENTITY HAVING (0 < MIN(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MIN(?) FROM OLGH17837ENTITY HAVING (? < MIN(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.multiselect(cb.min(intParam1));
            cquery.having(cb.lessThan(intParam2, cb.min(intParam3)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 0);
            query.setParameter(intParam3, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MIN(1) FROM OLGH17837ENTITY HAVING (0 < MIN(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MIN(?) FROM OLGH17837ENTITY HAVING (? < MIN(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.min(cb2.literal(1)));
            cquery2.having(cb2.lessThan(cb2.literal(0), cb2.min(cb2.literal(1))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MIN(1) FROM OLGH17837ENTITY HAVING (0 < MIN(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MIN(?) FROM OLGH17837ENTITY HAVING (? < MIN(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(cb3.min(cb3.literal(1)));
            ParameterExpression<Integer> intParam4 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.having(cb3.lessThan(intParam4, cb3.min(intParam5)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 0);
            query.setParameter(intParam5, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MIN(1) FROM OLGH17837ENTITY HAVING (0 < MIN(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MIN(?) FROM OLGH17837ENTITY HAVING (? < MIN(?))", sql.remove(0));
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

    public void testMin1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT MIN(?1) FROM OLGH17837Entity s HAVING ?2 < MIN(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MIN(1) FROM OLGH17837ENTITY HAVING (? < MIN(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MIN(?) FROM OLGH17837ENTITY HAVING (? < MIN(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT MIN(1) FROM OLGH17837Entity s HAVING 0 < MIN(1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT MIN(1) FROM OLGH17837ENTITY HAVING (? < MIN(1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT MIN(?) FROM OLGH17837ENTITY HAVING (? < MIN(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT MIN(1) FROM OLGH17837ENTITY HAVING (0 < MIN(1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT MIN(?) FROM OLGH17837ENTITY HAVING (? < MIN(?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT MIN(1) FROM OLGH17837Entity s HAVING ?1 < MIN(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MIN(1) FROM OLGH17837ENTITY HAVING (? < MIN(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MIN(?) FROM OLGH17837ENTITY HAVING (? < MIN(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.multiselect(cb.min(intParam1));
            cquery.having(cb.lessThan(intParam2, cb.min(intParam3)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 0);
            query.setParameter(intParam3, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MIN(1) FROM OLGH17837ENTITY HAVING (? < MIN(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MIN(?) FROM OLGH17837ENTITY HAVING (? < MIN(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.min(cb2.literal(1)));
            cquery2.having(cb2.lessThan(cb2.literal(0), cb2.min(cb2.literal(1))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT MIN(1) FROM OLGH17837ENTITY HAVING (? < MIN(1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT MIN(?) FROM OLGH17837ENTITY HAVING (? < MIN(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT MIN(1) FROM OLGH17837ENTITY HAVING (0 < MIN(1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT MIN(?) FROM OLGH17837ENTITY HAVING (? < MIN(?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(cb3.min(cb3.literal(1)));
            ParameterExpression<Integer> intParam4 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.having(cb3.lessThan(intParam4, cb3.min(intParam5)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 0);
            query.setParameter(intParam5, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MIN(1) FROM OLGH17837ENTITY HAVING (? < MIN(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MIN(?) FROM OLGH17837ENTITY HAVING (? < MIN(?))", sql.remove(0));
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

    public void testMin1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT MIN(?1) FROM OLGH17837Entity s HAVING ?2 < MIN(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MIN(1) FROM OLGH17837ENTITY HAVING (? < MIN(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MIN(?) FROM OLGH17837ENTITY HAVING (? < MIN(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT MIN(1) FROM OLGH17837Entity s HAVING 0 < MIN(1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MIN(1) FROM OLGH17837ENTITY HAVING (? < MIN(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MIN(?) FROM OLGH17837ENTITY HAVING (? < MIN(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT MIN(1) FROM OLGH17837Entity s HAVING ?1 < MIN(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MIN(1) FROM OLGH17837ENTITY HAVING (? < MIN(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MIN(?) FROM OLGH17837ENTITY HAVING (? < MIN(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.multiselect(cb.min(intParam1));
            cquery.having(cb.lessThan(intParam2, cb.min(intParam3)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 0);
            query.setParameter(intParam3, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MIN(1) FROM OLGH17837ENTITY HAVING (? < MIN(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MIN(?) FROM OLGH17837ENTITY HAVING (? < MIN(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.min(cb2.literal(1)));
            cquery2.having(cb2.lessThan(cb2.literal(0), cb2.min(cb2.literal(1))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MIN(1) FROM OLGH17837ENTITY HAVING (? < MIN(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MIN(?) FROM OLGH17837ENTITY HAVING (? < MIN(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(cb3.min(cb3.literal(1)));
            ParameterExpression<Integer> intParam4 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.having(cb3.lessThan(intParam4, cb3.min(intParam5)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 0);
            query.setParameter(intParam5, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT MIN(1) FROM OLGH17837ENTITY HAVING (? < MIN(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT MIN(?) FROM OLGH17837ENTITY HAVING (? < MIN(?))", sql.remove(0));
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

    public void testMin2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING MIN(s.intVal1) > ?2");
            query.setParameter(2, 8);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MIN(INTVAL1) > ?)", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING MIN(s.intVal1) > 8");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MIN(INTVAL1) > ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MIN(INTVAL1) > 8)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MIN(INTVAL1) > ?)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> longValue = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.groupBy(root.get(OLGH17837Entity_.intVal1));
            cquery.having(cb.greaterThan(cb.min(root.get(OLGH17837Entity_.intVal1)), longValue));

            query = em.createQuery(cquery);
            query.setParameter(longValue, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MIN(INTVAL1) > ?)", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.groupBy(root2.get(OLGH17837Entity_.intVal1));
            cquery2.having(cb2.greaterThan(cb2.min(root2.get(OLGH17837Entity_.intVal1)), 8));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MIN(INTVAL1) > ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MIN(INTVAL1) > 8)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MIN(INTVAL1) > ?)", sql.remove(0));
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

    public void testMin2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING MIN(s.intVal1) > ?2");
            query.setParameter(2, 8);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MIN(INTVAL1) > ?)", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING MIN(s.intVal1) > 8");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MIN(INTVAL1) > ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MIN(INTVAL1) > ?)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MIN(INTVAL1) > 8)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MIN(INTVAL1) > ?)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> longValue = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.groupBy(root.get(OLGH17837Entity_.intVal1));
            cquery.having(cb.greaterThan(cb.min(root.get(OLGH17837Entity_.intVal1)), longValue));

            query = em.createQuery(cquery);
            query.setParameter(longValue, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MIN(INTVAL1) > ?)", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.groupBy(root2.get(OLGH17837Entity_.intVal1));
            cquery2.having(cb2.greaterThan(cb2.min(root2.get(OLGH17837Entity_.intVal1)), 8));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MIN(INTVAL1) > ?)", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MIN(INTVAL1) > 8)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MIN(INTVAL1) > ?)", sql.remove(0));
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

    public void testMin2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING MIN(s.intVal1) > ?2");
            query.setParameter(2, 8);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MIN(INTVAL1) > ?)", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s GROUP BY s.intVal1 HAVING MIN(s.intVal1) > 8");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MIN(INTVAL1) > ?)", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> longValue = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.groupBy(root.get(OLGH17837Entity_.intVal1));
            cquery.having(cb.greaterThan(cb.min(root.get(OLGH17837Entity_.intVal1)), longValue));

            query = em.createQuery(cquery);
            query.setParameter(longValue, 8);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MIN(INTVAL1) > ?)", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.groupBy(root2.get(OLGH17837Entity_.intVal1));
            cquery2.having(cb2.greaterThan(cb2.min(root2.get(OLGH17837Entity_.intVal1)), 8));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY GROUP BY INTVAL1 HAVING (MIN(INTVAL1) > ?)", sql.remove(0));
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

    public void testSum1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT SUM(?1) FROM OLGH17837Entity s");
            query.setParameter(1, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT SUM(1) FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(cb.sum(intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.sum(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY", sql.remove(0));
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

    public void testSum1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT SUM(?1) FROM OLGH17837Entity s");
            query.setParameter(1, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT SUM(1) FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(cb.sum(intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.sum(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY", sql.remove(0));
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

    public void testSum1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT SUM(?1) FROM OLGH17837Entity s");
            query.setParameter(1, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT SUM(1) FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(cb.sum(intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.sum(cb2.literal(1)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY", sql.remove(0));
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

    public void testSum2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT SUM(?1) FROM OLGH17837Entity s HAVING ?2 < SUM(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (0 < SUM(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT SUM(1) FROM OLGH17837Entity s HAVING 0 < SUM(1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (0 < SUM(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT SUM(1) FROM OLGH17837Entity s HAVING ?1 < SUM(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (0 < SUM(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.multiselect(cb.sum(intParam1));
            cquery.having(cb.lessThan(intParam2, cb.sum(intParam3)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 0);
            query.setParameter(intParam3, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (0 < SUM(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.sum(cb2.literal(1)));
            cquery2.having(cb2.lessThan(cb2.literal(0), cb2.sum(cb2.literal(1))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (0 < SUM(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(cb3.sum(cb3.literal(1)));
            ParameterExpression<Integer> intParam4 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.having(cb3.lessThan(intParam4, cb3.sum(intParam5)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 0);
            query.setParameter(intParam5, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (0 < SUM(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
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

    public void testSum2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT SUM(?1) FROM OLGH17837Entity s HAVING ?2 < SUM(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (? < SUM(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT SUM(1) FROM OLGH17837Entity s HAVING 0 < SUM(1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (? < SUM(1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (0 < SUM(1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT SUM(1) FROM OLGH17837Entity s HAVING ?1 < SUM(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (? < SUM(1))", sql.remove(0));
                } else if (isDB2) {
                    Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (? < SUM(1))", sql.remove(0));
                } else if (isDB2) {
                    Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.multiselect(cb.sum(intParam1));
            cquery.having(cb.lessThan(intParam2, cb.sum(intParam3)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 0);
            query.setParameter(intParam3, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (? < SUM(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.sum(cb2.literal(1)));
            cquery2.having(cb2.lessThan(cb2.literal(0), cb2.sum(cb2.literal(1))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (? < SUM(1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (0 < SUM(1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(cb3.sum(cb3.literal(1)));
            ParameterExpression<Integer> intParam4 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.having(cb3.lessThan(intParam4, cb3.sum(intParam5)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 0);
            query.setParameter(intParam5, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (? < SUM(1))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (? < SUM(1))", sql.remove(0));
                } else if (isDB2) {
                    Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
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

    public void testSum2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            Query query = em.createQuery("SELECT SUM(?1) FROM OLGH17837Entity s HAVING ?2 < SUM(?3)");
            query.setParameter(1, 1);
            query.setParameter(2, 0);
            query.setParameter(3, 1);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (? < SUM(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT SUM(1) FROM OLGH17837Entity s HAVING 0 < SUM(1)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (? < SUM(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT SUM(1) FROM OLGH17837Entity s HAVING ?1 < SUM(?2)");
            query.setParameter(1, 0);
            query.setParameter(2, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (? < SUM(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.multiselect(cb.sum(intParam1));
            cquery.having(cb.lessThan(intParam2, cb.sum(intParam3)));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 1);
            query.setParameter(intParam2, 0);
            query.setParameter(intParam3, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (? < SUM(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.sum(cb2.literal(1)));
            cquery2.having(cb2.lessThan(cb2.literal(0), cb2.sum(cb2.literal(1))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (? < SUM(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(cb3.sum(cb3.literal(1)));
            ParameterExpression<Integer> intParam4 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> intParam5 = cb3.parameter(Integer.class);
            cquery3.having(cb3.lessThan(intParam4, cb3.sum(intParam5)));

            query = em.createQuery(cquery3);
            query.setParameter(intParam4, 0);
            query.setParameter(intParam5, 1);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT SUM(1) FROM OLGH17837ENTITY HAVING (? < SUM(1))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUM(?) FROM OLGH17837ENTITY HAVING (? < SUM(?))", sql.remove(0));
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
