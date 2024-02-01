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
import javax.persistence.criteria.CriteriaBuilder.Trimspec;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
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

public class TestFunctionLogic extends AbstractTestLogic {

    public void testAll1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                 Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 = ALL (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?2)");
            query.setParameter(1, 5);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 = ALL (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 = ALL (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?1)");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
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
            cquery.where(cb.equal(intValue1, cb.all(subquery)));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.intVal2), 9));

            cquery2.where(cb2.equal(cb2.literal(5), cb2.all(subquery2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
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

            cquery3.where(cb3.equal(cb3.literal(5), cb3.all(subquery3)));

            query = em.createQuery(cquery3);
            query.setParameter(intValue3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
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

    public void testAll1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                     Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 = ALL (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?2)");
            query.setParameter(1, 5);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 = ALL (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 = ALL (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?1)");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
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
            cquery.where(cb.equal(intValue1, cb.all(subquery)));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.intVal2), 9));

            cquery2.where(cb2.equal(cb2.literal(5), cb2.all(subquery2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
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

            cquery3.where(cb3.equal(cb3.literal(5), cb3.all(subquery3)));

            query = em.createQuery(cquery3);
            query.setParameter(intValue3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
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

    public void testAll1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                 Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 = ALL (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?2)");
            query.setParameter(1, 5);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 = ALL (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 = ALL (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?1)");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

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
            cquery.where(cb.equal(intValue1, cb.all(subquery)));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.intVal2), 9));

            cquery2.where(cb2.equal(cb2.literal(5), cb2.all(subquery2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue3 = cb3.parameter(Integer.class);
            Subquery<Integer> subquery3 = cquery3.subquery(Integer.class);
            Root<OLGH17837Entity> subroot3 = subquery3.from(OLGH17837Entity.class);
            subquery3.select(subroot3.get(OLGH17837Entity_.intVal2));
            subquery3.where(cb3.equal(subroot3.get(OLGH17837Entity_.intVal2), intValue3));

            cquery3.where(cb3.equal(cb3.literal(5), cb3.all(subquery3)));

            query = em.createQuery(cquery3);
            query.setParameter(intValue3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = ALL(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testAny1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                 Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 > ANY (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?2)");
            query.setParameter(1, 5);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 > ANY (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 > ANY (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?1)");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
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
            cquery.where(cb.greaterThan(intValue1, cb.any(subquery)));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.intVal2), 9));

            cquery2.where(cb2.greaterThan(cb2.literal(5), cb2.any(subquery2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
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

            cquery3.where(cb3.greaterThan(cb3.literal(5), cb3.any(subquery3)));

            query = em.createQuery(cquery3);
            query.setParameter(intValue3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
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

    public void testAny1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                     Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 > ANY (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?2)");
            query.setParameter(1, 5);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 > ANY (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 > ANY (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?1)");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
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
            cquery.where(cb.greaterThan(intValue1, cb.any(subquery)));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.intVal2), 9));

            cquery2.where(cb2.greaterThan(cb2.literal(5), cb2.any(subquery2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
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

            cquery3.where(cb3.greaterThan(cb3.literal(5), cb3.any(subquery3)));

            query = em.createQuery(cquery3);
            query.setParameter(intValue3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
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

    public void testAny1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                 Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 > ANY (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?2)");
            query.setParameter(1, 5);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 > ANY (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 > ANY (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?1)");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

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
            cquery.where(cb.greaterThan(intValue1, cb.any(subquery)));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.intVal2), 9));

            cquery2.where(cb2.greaterThan(cb2.literal(5), cb2.any(subquery2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue3 = cb3.parameter(Integer.class);
            Subquery<Integer> subquery3 = cquery3.subquery(Integer.class);
            Root<OLGH17837Entity> subroot3 = subquery3.from(OLGH17837Entity.class);
            subquery3.select(subroot3.get(OLGH17837Entity_.intVal2));
            subquery3.where(cb3.equal(subroot3.get(OLGH17837Entity_.intVal2), intValue3));

            cquery3.where(cb3.greaterThan(cb3.literal(5), cb3.any(subquery3)));

            query = em.createQuery(cquery3);
            query.setParameter(intValue3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? > ANY(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCast1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT CAST(?1 AS CHAR(2)) FROM OLGH17837Entity s");
            query.setParameter(1, 65);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CAST(? AS CHAR(2)) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CAST(? AS CHAR(2)) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT CAST(65 AS CHAR(2)) FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT CAST(? AS CHAR(2)) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CAST(65 AS CHAR(2)) FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CAST(? AS CHAR(2)) FROM OLGH17837ENTITY", sql.remove(0));
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

    public void testCast1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                      Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT CAST(?1 AS CHAR(2)) FROM OLGH17837Entity s");
            query.setParameter(1, 65);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT CAST(? AS CHAR(2)) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CAST(? AS CHAR(2)) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT CAST(65 AS CHAR(2)) FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT CAST(? AS CHAR(2)) FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT CAST(65 AS CHAR(2)) FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CAST(? AS CHAR(2)) FROM OLGH17837ENTITY", sql.remove(0));
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

    public void testCast1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                  Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT CAST(?1 AS CHAR(2)) FROM OLGH17837Entity s");
            query.setParameter(1, 65);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT CAST(? AS CHAR(2)) FROM OLGH17837ENTITY", sql.remove(0));

            query = em.createQuery("SELECT CAST(65 AS CHAR(2)) FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT CAST(? AS CHAR(2)) FROM OLGH17837ENTITY", sql.remove(0));
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testConcat1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = CONCAT(?1, ?2, ?3)");
            query.setParameter(1, "HELLO");
            query.setParameter(2, " ");
            query.setParameter(3, "WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR('HELLO' || ' ') || 'WORLD'))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = ? || ? || ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = CONCAT(CONCAT(?, ?), ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = CONCAT('HELLO', ' ', 'WORLD')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR('HELLO' || ' ') || 'WORLD'))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = ? || ? || ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = CONCAT(CONCAT(?, ?), ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = CONCAT(?1, ' ', s.strVal2)");
            query.setParameter(1, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR('HELLO' || ' ') || STRVAL2))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = ? || ? || STRVAL2)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = CONCAT(CONCAT(?, ?), STRVAL2))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            ParameterExpression<String> strParam3 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.concat(cb.concat(strParam1, strParam2), strParam3)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.setParameter(strParam2, " ");
            query.setParameter(strParam3, "WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR('HELLO' || ' ') || 'WORLD'))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = ? || ? || ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = CONCAT(CONCAT(?, ?), ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.concat(cb2.concat("HELLO", cb2.literal(" ")), "WORLD")));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR('HELLO' || ' ') || 'WORLD'))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = ? || ? || ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = CONCAT(CONCAT(?, ?), ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal1));
            ParameterExpression<String> strParam4 = cb3.parameter(String.class);
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.strVal1), cb3.concat(strParam4, cb3.concat(" ", root3.get(OLGH17837Entity_.strVal2)))));

            query = em.createQuery(cquery3);
            query.setParameter(strParam4, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(? || VARCHAR(? || STRVAL2)))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = ? || ? || STRVAL2)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = CONCAT(?, CONCAT(?, STRVAL2)))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(? || VARCHAR(' ' || STRVAL2)))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = ? || ? || STRVAL2)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = CONCAT(?, CONCAT(?, STRVAL2)))", sql.remove(0));
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

    public void testConcat1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                        Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = CONCAT(?1, ?2, ?3)");
            query.setParameter(1, "HELLO");
            query.setParameter(2, " ");
            query.setParameter(3, "WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR(? || ' ') || ?))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR(? || ?) || ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = ? || ? || ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = CONCAT(CONCAT(?, ?), ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = CONCAT('HELLO', ' ', 'WORLD')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR(? || ?) || ?))", sql.remove(0));
                } else if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR(? || ' ') || ?))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = ? || ? || ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = CONCAT(CONCAT(?, ?), ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR('HELLO' || ' ') || 'WORLD'))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = ? || ? || ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = CONCAT(CONCAT(?, ?), ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = CONCAT(?1, ' ', s.strVal2)");
            query.setParameter(1, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR(? || ?) || STRVAL2))", sql.remove(0));
                } else if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR(? || ' ') || STRVAL2))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = ? || ? || STRVAL2)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = CONCAT(CONCAT(?, ?), STRVAL2))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR(? || ' ') || STRVAL2))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = ? || ? || STRVAL2)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = CONCAT(CONCAT(?, ?), STRVAL2))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            ParameterExpression<String> strParam3 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.concat(cb.concat(strParam1, strParam2), strParam3)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.setParameter(strParam2, " ");
            query.setParameter(strParam3, "WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR(? || ' ') || ?))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR(? || ?) || ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = ? || ? || ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = CONCAT(CONCAT(?, ?), ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.concat(cb2.concat("HELLO", cb2.literal(" ")), "WORLD")));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR(? || ' ') || ?))", sql.remove(0));
                } else if (isDB2) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR(? || ?) || ?))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = ? || ? || ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = CONCAT(CONCAT(?, ?), ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR('HELLO' || ' ') || 'WORLD'))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = ? || ? || ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = CONCAT(CONCAT(?, ?), ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal1));
            ParameterExpression<String> strParam4 = cb3.parameter(String.class);
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.strVal1), cb3.concat(strParam4, cb3.concat(" ", root3.get(OLGH17837Entity_.strVal2)))));

            query = em.createQuery(cquery3);
            query.setParameter(strParam4, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(? || VARCHAR(? || STRVAL2)))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = ? || ? || STRVAL2)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = CONCAT(?, CONCAT(?, STRVAL2)))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(? || VARCHAR(' ' || STRVAL2)))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = ? || ? || STRVAL2)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = CONCAT(?, CONCAT(?, STRVAL2)))", sql.remove(0));
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

    public void testConcat1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                    Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = CONCAT(?1, ?2, ?3)");
            query.setParameter(1, "HELLO");
            query.setParameter(2, " ");
            query.setParameter(3, "WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR(? || ' ') || ?))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR(? || ?) || ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = ? || ? || ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = CONCAT(CONCAT(?, ?), ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = CONCAT('HELLO', ' ', 'WORLD')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR(? || ' ') || ?))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR(? || ?) || ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = ? || ? || ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = CONCAT(CONCAT(?, ?), ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = CONCAT(?1, ' ', s.strVal2)");
            query.setParameter(1, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR(? || ' ') || STRVAL2))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR(? || ?) || STRVAL2))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = ? || ? || STRVAL2)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = CONCAT(CONCAT(?, ?), STRVAL2))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            ParameterExpression<String> strParam3 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.concat(cb.concat(strParam1, strParam2), strParam3)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.setParameter(strParam2, " ");
            query.setParameter(strParam3, "WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR(? || ' ') || ?))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR(? || ?) || ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = ? || ? || ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = CONCAT(CONCAT(?, ?), ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.concat(cb2.concat("HELLO", cb2.literal(" ")), "WORLD")));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR(? || ' ') || ?))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(VARCHAR(? || ?) || ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = ? || ? || ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = CONCAT(CONCAT(?, ?), ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal1));
            ParameterExpression<String> strParam4 = cb3.parameter(String.class);
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.strVal1), cb3.concat(strParam4, cb3.concat(" ", root3.get(OLGH17837Entity_.strVal2)))));

            query = em.createQuery(cquery3);
            query.setParameter(strParam4, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = VARCHAR(? || VARCHAR(? || STRVAL2)))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = ? || ? || STRVAL2)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = CONCAT(?, CONCAT(?, STRVAL2)))", sql.remove(0));
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

    public void testConcat2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT CONCAT(s.strVal1, ?1) FROM OLGH17837Entity s");
            query.setParameter(1, "HELLO");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT VARCHAR(STRVAL1 || ?) FROM OLGH17837ENTITY", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 || ? FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CONCAT(STRVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT CONCAT(s.strVal1, 'HELLO') FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT VARCHAR(STRVAL1 || ?) FROM OLGH17837ENTITY", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 || ? FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CONCAT(STRVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT VARCHAR(STRVAL1 || 'HELLO') FROM OLGH17837ENTITY", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 || ? FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CONCAT(STRVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT CONCAT(s.strVal1, 'HELLO', ?1) FROM OLGH17837Entity s");
            query.setParameter(1, "WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT VARCHAR(VARCHAR(STRVAL1 || ?) || ?) FROM OLGH17837ENTITY", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 || ? || ? FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CONCAT(CONCAT(STRVAL1, ?), ?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT VARCHAR(VARCHAR(STRVAL1 || 'HELLO') || ?) FROM OLGH17837ENTITY", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 || ? || ? FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CONCAT(CONCAT(STRVAL1, ?), ?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(cb.concat(root.get(OLGH17837Entity_.strVal1), strParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT VARCHAR(STRVAL1 || ?) FROM OLGH17837ENTITY", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 || ? FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CONCAT(STRVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.concat(root2.get(OLGH17837Entity_.strVal1), "HELLO"));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT VARCHAR(STRVAL1 || ?) FROM OLGH17837ENTITY", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 || ? FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CONCAT(STRVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT VARCHAR(STRVAL1 || 'HELLO') FROM OLGH17837ENTITY", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 || ? FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CONCAT(STRVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam2 = cb3.parameter(String.class);
            cquery3.multiselect(cb3.concat(cb3.concat(root3.get(OLGH17837Entity_.strVal1), "HELLO"), strParam2));

            query = em.createQuery(cquery3);
            query.setParameter(strParam2, "WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT VARCHAR(VARCHAR(STRVAL1 || ?) || ?) FROM OLGH17837ENTITY", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 || ? || ? FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CONCAT(CONCAT(STRVAL1, ?), ?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT VARCHAR(VARCHAR(STRVAL1 || 'HELLO') || ?) FROM OLGH17837ENTITY", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 || ? || ? FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CONCAT(CONCAT(STRVAL1, ?), ?) FROM OLGH17837ENTITY", sql.remove(0));
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

    public void testConcat2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                        Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT CONCAT(s.strVal1, ?1) FROM OLGH17837Entity s");
            query.setParameter(1, "HELLO");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT VARCHAR(STRVAL1 || ?) FROM OLGH17837ENTITY", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 || ? FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CONCAT(STRVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT CONCAT(s.strVal1, 'HELLO') FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT VARCHAR(STRVAL1 || ?) FROM OLGH17837ENTITY", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 || ? FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CONCAT(STRVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT VARCHAR(STRVAL1 || 'HELLO') FROM OLGH17837ENTITY", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 || ? FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CONCAT(STRVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT CONCAT(s.strVal1, 'HELLO', ?1) FROM OLGH17837Entity s");
            query.setParameter(1, "WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT VARCHAR(VARCHAR(STRVAL1 || ?) || ?) FROM OLGH17837ENTITY", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 || ? || ? FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CONCAT(CONCAT(STRVAL1, ?), ?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT VARCHAR(VARCHAR(STRVAL1 || 'HELLO') || ?) FROM OLGH17837ENTITY", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 || ? || ? FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CONCAT(CONCAT(STRVAL1, ?), ?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(cb.concat(root.get(OLGH17837Entity_.strVal1), strParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT VARCHAR(STRVAL1 || ?) FROM OLGH17837ENTITY", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 || ? FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CONCAT(STRVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.concat(root2.get(OLGH17837Entity_.strVal1), "HELLO"));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT VARCHAR(STRVAL1 || ?) FROM OLGH17837ENTITY", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 || ? FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CONCAT(STRVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT VARCHAR(STRVAL1 || 'HELLO') FROM OLGH17837ENTITY", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 || ? FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CONCAT(STRVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam2 = cb3.parameter(String.class);
            cquery3.multiselect(cb3.concat(cb3.concat(root3.get(OLGH17837Entity_.strVal1), "HELLO"), strParam2));

            query = em.createQuery(cquery3);
            query.setParameter(strParam2, "WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT VARCHAR(VARCHAR(STRVAL1 || ?) || ?) FROM OLGH17837ENTITY", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 || ? || ? FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CONCAT(CONCAT(STRVAL1, ?), ?) FROM OLGH17837ENTITY", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT VARCHAR(VARCHAR(STRVAL1 || 'HELLO') || ?) FROM OLGH17837ENTITY", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 || ? || ? FROM OLGH17837ENTITY", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT CONCAT(CONCAT(STRVAL1, ?), ?) FROM OLGH17837ENTITY", sql.remove(0));
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

    public void testConcat2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                    Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT CONCAT(s.strVal1, ?1) FROM OLGH17837Entity s");
            query.setParameter(1, "HELLO");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT VARCHAR(STRVAL1 || ?) FROM OLGH17837ENTITY", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 || ? FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CONCAT(STRVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT CONCAT(s.strVal1, 'HELLO') FROM OLGH17837Entity s");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT VARCHAR(STRVAL1 || ?) FROM OLGH17837ENTITY", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 || ? FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CONCAT(STRVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            query = em.createQuery("SELECT CONCAT(s.strVal1, 'HELLO', ?1) FROM OLGH17837Entity s");
            query.setParameter(1, "WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT VARCHAR(VARCHAR(STRVAL1 || ?) || ?) FROM OLGH17837ENTITY", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 || ? || ? FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CONCAT(CONCAT(STRVAL1, ?), ?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(cb.concat(root.get(OLGH17837Entity_.strVal1), strParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT VARCHAR(STRVAL1 || ?) FROM OLGH17837ENTITY", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 || ? FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CONCAT(STRVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.concat(root2.get(OLGH17837Entity_.strVal1), "HELLO"));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT VARCHAR(STRVAL1 || ?) FROM OLGH17837ENTITY", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 || ? FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CONCAT(STRVAL1, ?) FROM OLGH17837ENTITY", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam2 = cb3.parameter(String.class);
            cquery3.multiselect(cb3.concat(cb3.concat(root3.get(OLGH17837Entity_.strVal1), "HELLO"), strParam2));

            query = em.createQuery(cquery3);
            query.setParameter(strParam2, "WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT VARCHAR(VARCHAR(STRVAL1 || ?) || ?) FROM OLGH17837ENTITY", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 || ? || ? FROM OLGH17837ENTITY", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT CONCAT(CONCAT(STRVAL1, ?), ?) FROM OLGH17837ENTITY", sql.remove(0));
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

    public void testLeftTrim1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                      Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(LEADING ?2)");
            query.setParameter(2, "  HELLO WORD ");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(LEADING '  HELLO WORD ')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.trim(Trimspec.LEADING, strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "  HELLO WORD ");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.trim(Trimspec.LEADING, cb2.literal("  HELLO WORD "))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?))", sql.remove(0));
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

    public void testLeftTrim1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                          Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(LEADING ?2)");
            query.setParameter(2, "  HELLO WORD ");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(LEADING '  HELLO WORD ')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM('  HELLO WORD '))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM('  HELLO WORD '))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.trim(Trimspec.LEADING, strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "  HELLO WORD ");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.trim(Trimspec.LEADING, cb2.literal("  HELLO WORD "))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM('  HELLO WORD '))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM('  HELLO WORD '))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?))", sql.remove(0));
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

    public void testLeftTrim1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                      Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(LEADING ?2)");
            query.setParameter(2, "  HELLO WORD ");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(LEADING '  HELLO WORD ')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.trim(Trimspec.LEADING, strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "  HELLO WORD ");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.trim(Trimspec.LEADING, cb2.literal("  HELLO WORD "))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?))", sql.remove(0));
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

    public void testLeftTrim2_1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                        Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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
        boolean isOracle = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.ORACLE);
        boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.POSTGRES);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(LEADING ?1 FROM ?2)");
            query.setParameter(1, 'A');
            query.setParameter(2, "AAAHELLO WORDAA");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING ? FROM ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING ? FROM ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(LEADING 'A' FROM ?2)");
            query.setParameter(2, "AAAHELLO WORDAA");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING ? FROM ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Character> charParam1 = cb.parameter(Character.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.trim(Trimspec.LEADING, charParam1, strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(charParam1, 'A');
            query.setParameter(strParam1, "AAAHELLO WORDAA");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING ? FROM ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.trim(Trimspec.LEADING, cb2.literal('A'), cb2.literal("AAAHELLO WORDAA"))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING ? FROM ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam2 = cb3.parameter(String.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal1));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.strVal1), cb3.trim(Trimspec.LEADING, cb3.literal('A'), strParam2)));

            query = em.createQuery(cquery3);
            query.setParameter(strParam2, "AAAHELLO WORDAA");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING ? FROM ?))", sql.remove(0));
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

    public void testLeftTrim2_1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                            Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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
        boolean isOracle = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.ORACLE);
        boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.POSTGRES);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(LEADING ?1 FROM ?2)");
            query.setParameter(1, 'A');
            query.setParameter(2, "AAAHELLO WORDAA");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM ?))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING ? FROM ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
                } else if (isDB2) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM ?))", sql.remove(0));
                } else if (isOracle || isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING ? FROM ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
                } else if (isOracle || isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING ? FROM ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(LEADING 'A' FROM ?2)");
            query.setParameter(2, "AAAHELLO WORDAA");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
                } else if (isDB2) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM ?))", sql.remove(0));
                } else if (isOracle || isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING ? FROM ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
                } else if (isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM ?))", sql.remove(0));
                } else if (isOracle || isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING ? FROM ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Character> charParam1 = cb.parameter(Character.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.trim(Trimspec.LEADING, charParam1, strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(charParam1, 'A');
            query.setParameter(strParam1, "AAAHELLO WORDAA");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM ?))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING ? FROM ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.trim(Trimspec.LEADING, cb2.literal('A'), cb2.literal("AAAHELLO WORDAA"))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
                } else if (isDB2) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM ?))", sql.remove(0));
                } else if (isOracle || isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING ? FROM ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
                } else if (isOracle || isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING ? FROM ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam2 = cb3.parameter(String.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal1));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.strVal1), cb3.trim(Trimspec.LEADING, cb3.literal('A'), strParam2)));

            query = em.createQuery(cquery3);
            query.setParameter(strParam2, "AAAHELLO WORDAA");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
                } else if (isDB2) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM ?))", sql.remove(0));
                } else if (isOracle || isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING ? FROM ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
                } else if (isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM ?))", sql.remove(0));
                } else if (isOracle || isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING ? FROM ?))", sql.remove(0));
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

    public void testLeftTrim2_1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                        Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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
        boolean isOracle = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.ORACLE);
        boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.POSTGRES);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(LEADING ?1 FROM ?2)");
            query.setParameter(1, 'A');
            query.setParameter(2, "AAAHELLO WORDAA");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM ?))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING ? FROM ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM ?))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING ? FROM ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(LEADING 'A' FROM ?2)");
            query.setParameter(2, "AAAHELLO WORDAA");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM ?))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING ? FROM ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Character> charParam1 = cb.parameter(Character.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.trim(Trimspec.LEADING, charParam1, strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(charParam1, 'A');
            query.setParameter(strParam1, "AAAHELLO WORDAA");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM ?))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING ? FROM ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.trim(Trimspec.LEADING, cb2.literal('A'), cb2.literal("AAAHELLO WORDAA"))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM ?))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING ? FROM ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam2 = cb3.parameter(String.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal1));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.strVal1), cb3.trim(Trimspec.LEADING, cb3.literal('A'), strParam2)));

            query = em.createQuery(cquery3);
            query.setParameter(strParam2, "AAAHELLO WORDAA");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING 'A' FROM ?))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(LEADING ? FROM ?))", sql.remove(0));
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

    public void testLength1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?2 = LENGTH(?1)");
            query.setParameter(1, "HELLO WORLD");
            query.setParameter(2, 11);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (11 = LENGTH('HELLO WORLD'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? = LENGTH(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 11 = LENGTH('HELLO WORLD')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (11 = LENGTH('HELLO WORLD'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? = LENGTH(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.where(cb.equal(intParam1, cb.length(strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO WORLD");
            query.setParameter(intParam1, 11);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (11 = LENGTH('HELLO WORLD'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? = LENGTH(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.equal(cb2.literal(11), cb2.length(cb2.literal("HELLO WORLD"))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (11 = LENGTH('HELLO WORLD'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? = LENGTH(?))", sql.remove(0));
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

    public void testLength1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                        Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?2 = LENGTH(?1)");
            query.setParameter(1, "HELLO WORLD");
            query.setParameter(2, 11);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? = LENGTH('HELLO WORLD'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? = LENGTH(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 11 = LENGTH('HELLO WORLD')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? = LENGTH('HELLO WORLD'))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? = LENGTH(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (11 = LENGTH('HELLO WORLD'))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? = LENGTH(?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.where(cb.equal(intParam1, cb.length(strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO WORLD");
            query.setParameter(intParam1, 11);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? = LENGTH('HELLO WORLD'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? = LENGTH(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.equal(cb2.literal(11), cb2.length(cb2.literal("HELLO WORLD"))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? = LENGTH('HELLO WORLD'))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? = LENGTH(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (11 = LENGTH('HELLO WORLD'))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? = LENGTH(?))", sql.remove(0));
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

    public void testLength1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                    Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?2 = LENGTH(?1)");
            query.setParameter(1, "HELLO WORLD");
            query.setParameter(2, 11);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? = LENGTH('HELLO WORLD'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? = LENGTH(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 11 = LENGTH('HELLO WORLD')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? = LENGTH('HELLO WORLD'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? = LENGTH(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.where(cb.equal(intParam1, cb.length(strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO WORLD");
            query.setParameter(intParam1, 11);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? = LENGTH('HELLO WORLD'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? = LENGTH(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.equal(cb2.literal(11), cb2.length(cb2.literal("HELLO WORLD"))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? = LENGTH('HELLO WORLD'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (? = LENGTH(?))", sql.remove(0));
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

    public void testLength2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.intVal1, ?1 FROM OLGH17837Entity s ORDER BY LENGTH(?1)");
            query.setParameter(1, "HELLO WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, 'HELLO WORLD' FROM OLGH17837ENTITY ORDER BY LENGTH('HELLO WORLD')", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, ? FROM OLGH17837ENTITY ORDER BY LENGTH(?)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, 'HELLO WORLD' FROM OLGH17837Entity s ORDER BY LENGTH('HELLO WORLD')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, 'HELLO WORLD' FROM OLGH17837ENTITY ORDER BY LENGTH('HELLO WORLD')", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, ? FROM OLGH17837ENTITY ORDER BY LENGTH(?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1), strParam1);
            cquery.orderBy(cb.desc(cb.length(strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, 'HELLO WORLD' FROM OLGH17837ENTITY ORDER BY LENGTH('HELLO WORLD') DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, ? FROM OLGH17837ENTITY ORDER BY LENGTH(?) DESC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1), cb2.literal("HELLO WORLD"));
            cquery2.orderBy(cb2.desc(cb2.length(cb2.literal("HELLO WORLD"))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, 'HELLO WORLD' FROM OLGH17837ENTITY ORDER BY LENGTH('HELLO WORLD') DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, ? FROM OLGH17837ENTITY ORDER BY LENGTH(?) DESC", sql.remove(0));
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

    public void testLength2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                        Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.intVal1, ?1 FROM OLGH17837Entity s ORDER BY LENGTH(?1)");
            query.setParameter(1, "HELLO WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, 'HELLO WORLD' FROM OLGH17837ENTITY ORDER BY LENGTH('HELLO WORLD')", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, ? FROM OLGH17837ENTITY ORDER BY LENGTH(?)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, 'HELLO WORLD' FROM OLGH17837Entity s ORDER BY LENGTH('HELLO WORLD')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, 'HELLO WORLD' FROM OLGH17837ENTITY ORDER BY LENGTH('HELLO WORLD')", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, ? FROM OLGH17837ENTITY ORDER BY LENGTH(?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1), strParam1);
            cquery.orderBy(cb.desc(cb.length(strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, 'HELLO WORLD' FROM OLGH17837ENTITY ORDER BY LENGTH('HELLO WORLD') DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, ? FROM OLGH17837ENTITY ORDER BY LENGTH(?) DESC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1), cb2.literal("HELLO WORLD"));
            cquery2.orderBy(cb2.desc(cb2.length(cb2.literal("HELLO WORLD"))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, 'HELLO WORLD' FROM OLGH17837ENTITY ORDER BY LENGTH('HELLO WORLD') DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, ? FROM OLGH17837ENTITY ORDER BY LENGTH(?) DESC", sql.remove(0));
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

    public void testLength2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                    Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.intVal1, ?1 FROM OLGH17837Entity s ORDER BY LENGTH(?1)");
            query.setParameter(1, "HELLO WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, 'HELLO WORLD' FROM OLGH17837ENTITY ORDER BY LENGTH('HELLO WORLD')", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, ? FROM OLGH17837ENTITY ORDER BY LENGTH(?)", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1, 'HELLO WORLD' FROM OLGH17837Entity s ORDER BY LENGTH('HELLO WORLD')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, 'HELLO WORLD' FROM OLGH17837ENTITY ORDER BY LENGTH('HELLO WORLD')", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, ? FROM OLGH17837ENTITY ORDER BY LENGTH(?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1), strParam1);
            cquery.orderBy(cb.desc(cb.length(strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, 'HELLO WORLD' FROM OLGH17837ENTITY ORDER BY LENGTH('HELLO WORLD') DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, ? FROM OLGH17837ENTITY ORDER BY LENGTH(?) DESC", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1), cb2.literal("HELLO WORLD"));
            cquery2.orderBy(cb2.desc(cb2.length(cb2.literal("HELLO WORLD"))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1, 'HELLO WORLD' FROM OLGH17837ENTITY ORDER BY LENGTH('HELLO WORLD') DESC", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1, ? FROM OLGH17837ENTITY ORDER BY LENGTH(?) DESC", sql.remove(0));
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

    public void testLocate1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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
        boolean isOracle = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.ORACLE);
        boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.POSTGRES);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = LOCATE(?1, ?2)");
            query.setParameter(1, "HI");
            query.setParameter(2, "ABCDEFGHIJKLMNOP");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = STRPOS(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = STRPOS(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = LOCATE(?2, 'ABCDEFGHIJKLMNOP')");
            query.setParameter(2, "HI");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = STRPOS(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), cb.locate(strParam1, strParam2)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "ABCDEFGHIJKLMNOP");
            query.setParameter(strParam2, "HI");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = STRPOS(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), cb2.locate(cb2.literal("ABCDEFGHIJKLMNOP"), "HI")));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = STRPOS(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam4 = cb3.parameter(String.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.intVal1), cb3.locate(cb3.literal("ABCDEFGHIJKLMNOP"), strParam4)));

            query = em.createQuery(cquery3);
            query.setParameter(strParam4, "HI");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = STRPOS(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
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

    public void testLocate1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                        Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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
        boolean isOracle = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.ORACLE);
        boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.POSTGRES);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = LOCATE(?1, ?2)");
            query.setParameter(1, "HI");
            query.setParameter(2, "ABCDEFGHIJKLMNOP");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = STRPOS(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
                } else if (isOracle) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = STRPOS(?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
                } else if (isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
                } else if (isOracle) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = STRPOS(?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = LOCATE(?2, 'ABCDEFGHIJKLMNOP')");
            query.setParameter(2, "HI");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
                } else if (isOracle) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = STRPOS(?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
                } else if (isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
                } else if (isOracle) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = STRPOS(?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), cb.locate(strParam1, strParam2)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "ABCDEFGHIJKLMNOP");
            query.setParameter(strParam2, "HI");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = STRPOS(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), cb2.locate(cb2.literal("ABCDEFGHIJKLMNOP"), "HI")));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
                } else if (isOracle) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = STRPOS(?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
                } else if (isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
                } else if (isOracle) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = STRPOS(?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam4 = cb3.parameter(String.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.intVal1), cb3.locate(cb3.literal("ABCDEFGHIJKLMNOP"), strParam4)));

            query = em.createQuery(cquery3);
            query.setParameter(strParam4, "HI");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
                } else if (isOracle) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = STRPOS(?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
                } else if (isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
                } else if (isOracle) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = STRPOS(?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
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

    public void testLocate1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                    Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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
        boolean isOracle = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.ORACLE);
        boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.POSTGRES);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = LOCATE(?1, ?2)");
            query.setParameter(1, "HI");
            query.setParameter(2, "ABCDEFGHIJKLMNOP");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = STRPOS(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = STRPOS(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = LOCATE(?2, 'ABCDEFGHIJKLMNOP')");
            query.setParameter(2, "HI");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = STRPOS(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), cb.locate(strParam1, strParam2)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "ABCDEFGHIJKLMNOP");
            query.setParameter(strParam2, "HI");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = STRPOS(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), cb2.locate(cb2.literal("ABCDEFGHIJKLMNOP"), "HI")));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = STRPOS(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam4 = cb3.parameter(String.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.intVal1), cb3.locate(cb3.literal("ABCDEFGHIJKLMNOP"), strParam4)));

            query = em.createQuery(cquery3);
            query.setParameter(strParam4, "HI");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('HI', 'ABCDEFGHIJKLMNOP'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = STRPOS(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?))", sql.remove(0));
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

    public void testLocate2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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
        boolean isOracle = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.ORACLE);
        boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.POSTGRES);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = LOCATE(?1, ?2, ?3)");
            query.setParameter(1, "X");
            query.setParameter(2, "OXOOOOOXXOOOOOOXX");
            query.setParameter(3, 3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = COALESCE(NULLIF(STRPOS(SUBSTRING(? FROM ?), ?), 0) - 1 + ?, 0))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = COALESCE(NULLIF(STRPOS(SUBSTRING(? FROM ?), ?), 0) - 1 + ?, 0))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = LOCATE('X', ?1, 3)");
            query.setParameter(1, "OXOOOOOXXOOOOOOXX");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = COALESCE(NULLIF(STRPOS(SUBSTRING(? FROM ?), ?), 0) - 1 + ?, 0))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?, ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), cb.locate(strParam2, strParam1, intParam3)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "X");
            query.setParameter(strParam2, "OXOOOOOXXOOOOOOXX");
            query.setParameter(intParam3, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = COALESCE(NULLIF(STRPOS(SUBSTRING(? FROM ?), ?), 0) - 1 + ?, 0))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), cb2.locate(cb2.literal("OXOOOOOXXOOOOOOXX"), "X", 3)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = COALESCE(NULLIF(STRPOS(SUBSTRING(? FROM ?), ?), 0) - 1 + ?, 0))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam5 = cb3.parameter(String.class);
            ParameterExpression<Integer> intParam6 = cb3.parameter(Integer.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.intVal1), cb3.locate(strParam5, cb3.literal("X"), intParam6)));

            query = em.createQuery(cquery3);
            query.setParameter(strParam5, "OXOOOOOXXOOOOOOXX");
            query.setParameter(intParam6, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = COALESCE(NULLIF(STRPOS(SUBSTRING(? FROM ?), ?), 0) - 1 + ?, 0))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?, ?))", sql.remove(0));
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

    public void testLocate2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                        Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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
        boolean isOracle = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.ORACLE);
        boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.POSTGRES);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = LOCATE(?1, ?2, ?3)");
            query.setParameter(1, "X");
            query.setParameter(2, "OXOOOOOXXOOOOOOXX");
            query.setParameter(3, 3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = COALESCE(NULLIF(STRPOS(SUBSTRING(? FROM ?), ?), 0) - 1 + ?, 0))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3))", sql.remove(0));
                } else if (isOracle) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?, ?))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = COALESCE(NULLIF(STRPOS(SUBSTRING(? FROM ?), ?), 0) - 1 + ?, 0))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?, ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3))", sql.remove(0));
                } else if (isOracle) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?, ?))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = COALESCE(NULLIF(STRPOS(SUBSTRING(? FROM ?), ?), 0) - 1 + ?, 0))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?, ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = LOCATE('X', ?1, 3)");
            query.setParameter(1, "OXOOOOOXXOOOOOOXX");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3))", sql.remove(0));
                } else if (isOracle) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?, ?))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = COALESCE(NULLIF(STRPOS(SUBSTRING(? FROM ?), ?), 0) - 1 + ?, 0))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?, ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3))", sql.remove(0));
                } else if (isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', ?, 3))", sql.remove(0));
                } else if (isOracle) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?, ?))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = COALESCE(NULLIF(STRPOS(SUBSTRING(? FROM ?), ?), 0) - 1 + ?, 0))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?, ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), cb.locate(strParam2, strParam1, intParam3)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "X");
            query.setParameter(strParam2, "OXOOOOOXXOOOOOOXX");
            query.setParameter(intParam3, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = COALESCE(NULLIF(STRPOS(SUBSTRING(? FROM ?), ?), 0) - 1 + ?, 0))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), cb2.locate(cb2.literal("OXOOOOOXXOOOOOOXX"), "X", 3)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3))", sql.remove(0));
                } else if (isOracle) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?, ?))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = COALESCE(NULLIF(STRPOS(SUBSTRING(? FROM ?), ?), 0) - 1 + ?, 0))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?, ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3))", sql.remove(0));
                } else if (isOracle) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?, ?))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = COALESCE(NULLIF(STRPOS(SUBSTRING(? FROM ?), ?), 0) - 1 + ?, 0))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?, ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam5 = cb3.parameter(String.class);
            ParameterExpression<Integer> intParam6 = cb3.parameter(Integer.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.intVal1), cb3.locate(strParam5, cb3.literal("X"), intParam6)));

            query = em.createQuery(cquery3);
            query.setParameter(strParam5, "OXOOOOOXXOOOOOOXX");
            query.setParameter(intParam6, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3))", sql.remove(0));
                } else if (isOracle) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?, ?))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = COALESCE(NULLIF(STRPOS(SUBSTRING(? FROM ?), ?), 0) - 1 + ?, 0))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?, ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3))", sql.remove(0));
                } else if (isDB2 || isDerby) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', ?, ?))", sql.remove(0));
                } else if (isOracle) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?, ?))", sql.remove(0));
                } else if (isPostgres) {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = COALESCE(NULLIF(STRPOS(SUBSTRING(? FROM ?), ?), 0) - 1 + ?, 0))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?, ?))", sql.remove(0));
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

    public void testLocate2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                    Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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
        boolean isOracle = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.ORACLE);
        boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.POSTGRES);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = LOCATE(?1, ?2, ?3)");
            query.setParameter(1, "X");
            query.setParameter(2, "OXOOOOOXXOOOOOOXX");
            query.setParameter(3, 3);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = COALESCE(NULLIF(STRPOS(SUBSTRING(? FROM ?), ?), 0) - 1 + ?, 0))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = COALESCE(NULLIF(STRPOS(SUBSTRING(? FROM ?), ?), 0) - 1 + ?, 0))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE s.intVal1 = LOCATE('X', ?1, 3)");
            query.setParameter(1, "OXOOOOOXXOOOOOOXX");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = COALESCE(NULLIF(STRPOS(SUBSTRING(? FROM ?), ?), 0) - 1 + ?, 0))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?, ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<String> strParam2 = cb.parameter(String.class);
            ParameterExpression<Integer> intParam3 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.intVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), cb.locate(strParam2, strParam1, intParam3)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "X");
            query.setParameter(strParam2, "OXOOOOOXXOOOOOOXX");
            query.setParameter(intParam3, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = COALESCE(NULLIF(STRPOS(SUBSTRING(? FROM ?), ?), 0) - 1 + ?, 0))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), cb2.locate(cb2.literal("OXOOOOOXXOOOOOOXX"), "X", 3)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = COALESCE(NULLIF(STRPOS(SUBSTRING(? FROM ?), ?), 0) - 1 + ?, 0))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam5 = cb3.parameter(String.class);
            ParameterExpression<Integer> intParam6 = cb3.parameter(Integer.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.intVal1), cb3.locate(strParam5, cb3.literal("X"), intParam6)));

            query = em.createQuery(cquery3);
            query.setParameter(strParam5, "OXOOOOOXXOOOOOOXX");
            query.setParameter(intParam6, 3);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE('X', 'OXOOOOOXXOOOOOOXX', 3))", sql.remove(0));
            } else if (isOracle) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = INSTR(?, ?, ?))", sql.remove(0));
            } else if (isPostgres) {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = COALESCE(NULLIF(STRPOS(SUBSTRING(? FROM ?), ?), 0) - 1 + ?, 0))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT INTVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = LOCATE(?, ?, ?))", sql.remove(0));
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

    public void testLower1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = LOWER(?1)");
            query.setParameter(1, "HELLO WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LCASE('HELLO WORLD'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LOWER(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = LOWER('HELLO WORLD')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LCASE('HELLO WORLD'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LOWER(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.lower(strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LCASE('HELLO WORLD'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LOWER(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.lower(cb2.literal("HELLO WORLD"))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LCASE('HELLO WORLD'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LOWER(?))", sql.remove(0));
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

    public void testLower1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = LOWER(?1)");
            query.setParameter(1, "HELLO WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LCASE(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LOWER(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = LOWER('HELLO WORLD')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LCASE(?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LOWER(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LCASE('HELLO WORLD'))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LOWER(?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.lower(strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LCASE(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LOWER(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.lower(cb2.literal("HELLO WORLD"))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LCASE(?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LOWER(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LCASE('HELLO WORLD'))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LOWER(?))", sql.remove(0));
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

    public void testLower1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                   Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = LOWER(?1)");
            query.setParameter(1, "HELLO WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LCASE(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LOWER(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = LOWER('HELLO WORLD')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LCASE(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LOWER(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.lower(strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LCASE(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LOWER(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.lower(cb2.literal("HELLO WORLD"))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LCASE(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = LOWER(?))", sql.remove(0));
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

    public void testRightTrim1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(TRAILING ?2)");
            query.setParameter(2, "  HELLO WORD ");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(TRAILING '  HELLO WORD ')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.trim(Trimspec.TRAILING, strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "  HELLO WORD ");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.trim(Trimspec.TRAILING, cb2.literal("  HELLO WORD "))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?))", sql.remove(0));
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

    public void testRightTrim1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                           Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(TRAILING ?2)");
            query.setParameter(2, "  HELLO WORD ");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(TRAILING '  HELLO WORD ')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM('  HELLO WORD '))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM('  HELLO WORD '))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?))", sql.remove(0));
                }
            }

            // -----------------------

            // Trim test #1 with CriteriaBuilder parameters
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.trim(Trimspec.TRAILING, strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "  HELLO WORD ");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.trim(Trimspec.TRAILING, cb2.literal("  HELLO WORD "))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM('  HELLO WORD '))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM('  HELLO WORD '))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?))", sql.remove(0));
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

    public void testRightTrim1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                       Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(TRAILING ?2)");
            query.setParameter(2, "  HELLO WORD ");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(TRAILING '  HELLO WORD ')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.trim(Trimspec.TRAILING, strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "  HELLO WORD ");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.trim(Trimspec.TRAILING, cb2.literal("  HELLO WORD "))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?))", sql.remove(0));
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

    public void testRightTrim2_1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                         Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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
        boolean isOracle = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.ORACLE);
        boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.POSTGRES);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(TRAILING ?1 FROM ?2)");
            query.setParameter(1, 'A');
            query.setParameter(2, "AAAHELLO WORDAA");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING ? FROM ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(TRAILING 'A' FROM 'AAAHELLO WORDAA')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING ? FROM ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Character> chaParam1 = cb.parameter(Character.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.trim(Trimspec.TRAILING, chaParam1, strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(chaParam1, 'A');
            query.setParameter(strParam1, "AAAHELLO WORDAA");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING ? FROM ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.trim(Trimspec.TRAILING, 'A', cb2.literal("AAAHELLO WORDAA"))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING ? FROM ?))", sql.remove(0));
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

    public void testRightTrim2_1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                             Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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
        boolean isOracle = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.ORACLE);
        boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.POSTGRES);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(TRAILING ?1 FROM ?2)");
            query.setParameter(1, 'A');
            query.setParameter(2, "AAAHELLO WORDAA");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM ?))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING ? FROM ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(TRAILING 'A' FROM 'AAAHELLO WORDAA')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
                } else if (isDB2) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM ?))", sql.remove(0));
                } else if (isOracle || isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING ? FROM ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
                } else if (isOracle || isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING ? FROM ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Character> chaParam1 = cb.parameter(Character.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.trim(Trimspec.TRAILING, chaParam1, strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(chaParam1, 'A');
            query.setParameter(strParam1, "AAAHELLO WORDAA");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM ?))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING ? FROM ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.trim(Trimspec.TRAILING, 'A', cb2.literal("AAAHELLO WORDAA"))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
                } else if (isDB2) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM ?))", sql.remove(0));
                } else if (isOracle || isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING ? FROM ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
                } else if (isOracle || isPostgres) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING ? FROM ?))", sql.remove(0));
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

    public void testRightTrim2_1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                         Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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
        boolean isOracle = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.ORACLE);
        boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, dbProductVersion, DatabaseVendor.POSTGRES);

        // Execute Test Case
        try {
            SQLCallListener.clearCalls();

            EntityManager em = jpaResource.getEm();

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(TRAILING ?1 FROM ?2)");
            query.setParameter(1, 'A');
            query.setParameter(2, "AAAHELLO WORDAA");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM ?))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING ? FROM ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(TRAILING 'A' FROM 'AAAHELLO WORDAA')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM ?))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING ? FROM ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Character> chaParam1 = cb.parameter(Character.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.trim(Trimspec.TRAILING, chaParam1, strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(chaParam1, 'A');
            query.setParameter(strParam1, "AAAHELLO WORDAA");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM ?))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING ? FROM ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.trim(Trimspec.TRAILING, 'A', cb2.literal("AAAHELLO WORDAA"))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM ?))", sql.remove(0));
            } else if (isOracle || isPostgres) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = RTRIM(?, ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(TRAILING ? FROM ?))", sql.remove(0));
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

    public void testSize1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT 1 FROM OLGH17837Entity s WHERE SIZE(s.colVal1) = ?1");
            query.setParameter(1, 36);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = 36)",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = ?)",
                                    sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM OLGH17837Entity s WHERE SIZE(s.colVal1) = 36");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = 36)",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = ?)",
                                    sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(cb.literal(1));
            cquery.where(cb.equal(cb.size(root.get(OLGH17837Entity_.colVal1)), intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 36);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = 36)",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = ?)",
                                    sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.where(cb2.equal(cb2.size(root2.get(OLGH17837Entity_.colVal1)), cb2.literal(36)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = 36)",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = ?)",
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

    public void testSize1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                      Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT 1 FROM OLGH17837Entity s WHERE SIZE(s.colVal1) = ?1");
            query.setParameter(1, 36);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = ?)",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = ?)",
                                    sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM OLGH17837Entity s WHERE SIZE(s.colVal1) = 36");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = ?)",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = ?)",
                                        sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = 36)",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = ?)",
                                        sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(cb.literal(1));
            cquery.where(cb.equal(cb.size(root.get(OLGH17837Entity_.colVal1)), intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 36);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = ?)",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = ?)",
                                    sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.where(cb2.equal(cb2.size(root2.get(OLGH17837Entity_.colVal1)), cb2.literal(36)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = ?)",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = ?)",
                                        sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = 36)",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = ?)",
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

    public void testSize1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                  Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT 1 FROM OLGH17837Entity s WHERE SIZE(s.colVal1) = ?1");
            query.setParameter(1, 36);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = ?)",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = ?)",
                                    sql.remove(0));
            }

            query = em.createQuery("SELECT 1 FROM OLGH17837Entity s WHERE SIZE(s.colVal1) = 36");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = ?)",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = ?)",
                                    sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(cb.literal(1));
            cquery.where(cb.equal(cb.size(root.get(OLGH17837Entity_.colVal1)), intParam1));

            query = em.createQuery(cquery);
            query.setParameter(intParam1, 36);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = ?)",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = ?)",
                                    sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.literal(1));
            cquery2.where(cb2.equal(cb2.size(root2.get(OLGH17837Entity_.colVal1)), cb2.literal(36)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT 1 FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = ?)",
                                    sql.remove(0));
            } else {
                Assert.assertEquals("SELECT ? FROM OLGH17837ENTITY t0 WHERE ((SELECT COUNT(t1.ID) FROM COLTABLE1 t2, OLGH17837ENTITY t1 WHERE (t2.ent_id = t0.ID)) = ?)",
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

    public void testSome1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 = SOME (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?2)");
            query.setParameter(1, 5);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 = SOME (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 = SOME (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?1)");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
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
            cquery.where(cb.equal(intValue1, cb.some(subquery)));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.intVal2), 9));

            cquery2.where(cb2.equal(cb2.literal(5), cb2.some(subquery2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
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

            cquery3.where(cb3.equal(cb3.literal(5), cb3.some(subquery3)));

            query = em.createQuery(cquery3);
            query.setParameter(intValue3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
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

    public void testSome1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                      Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 = SOME (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?2)");
            query.setParameter(1, 5);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 = SOME (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 = SOME (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?1)");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
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
            cquery.where(cb.equal(intValue1, cb.some(subquery)));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.intVal2), 9));

            cquery2.where(cb2.equal(cb2.literal(5), cb2.some(subquery2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = 9)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
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

            cquery3.where(cb3.equal(cb3.literal(5), cb3.some(subquery3)));

            query = em.createQuery(cquery3);
            query.setParameter(intValue3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                    sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (5 = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
                                        sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))",
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

    public void testSome1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                  Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE ?1 = SOME (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?2)");
            query.setParameter(1, 5);
            query.setParameter(2, 9);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 = SOME (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = 9)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            query = em.createQuery("SELECT s.intVal1 FROM OLGH17837Entity s WHERE 5 = SOME (SELECT u.intVal2 FROM OLGH17837Entity u WHERE u.intVal2 = ?1)");
            query.setParameter(1, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

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
            cquery.where(cb.equal(intValue1, cb.some(subquery)));

            query = em.createQuery(cquery);
            query.setParameter(intValue1, 5);
            query.setParameter(intValue2, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery2 = cb2.createQuery(Integer[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.intVal1));

            Subquery<Integer> subquery2 = cquery2.subquery(Integer.class);
            Root<OLGH17837Entity> subroot2 = subquery2.from(OLGH17837Entity.class);
            subquery2.select(subroot2.get(OLGH17837Entity_.intVal2));
            subquery2.where(cb2.equal(subroot2.get(OLGH17837Entity_.intVal2), 9));

            cquery2.where(cb2.equal(cb2.literal(5), cb2.some(subquery2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer[]> cquery3 = cb3.createQuery(Integer[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.intVal1));

            ParameterExpression<Integer> intValue3 = cb3.parameter(Integer.class);
            Subquery<Integer> subquery3 = cquery3.subquery(Integer.class);
            Root<OLGH17837Entity> subroot3 = subquery3.from(OLGH17837Entity.class);
            subquery3.select(subroot3.get(OLGH17837Entity_.intVal2));
            subquery3.where(cb3.equal(subroot3.get(OLGH17837Entity_.intVal2), intValue3));

            cquery3.where(cb3.equal(cb3.literal(5), cb3.some(subquery3)));

            query = em.createQuery(cquery3);
            query.setParameter(intValue3, 9);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT t0.INTVAL1 FROM OLGH17837ENTITY t0 WHERE (? = SOME(SELECT t1.INTVAL2 FROM OLGH17837ENTITY t1 WHERE (t1.INTVAL2 = ?)))", sql.remove(0));
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testSubstring1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = SUBSTRING(?1, ?2, ?3)");
            query.setParameter(1, "HELLO WORLD");
            query.setParameter(2, 1);
            query.setParameter(3, 5);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR('HELLO WORLD', 1, 5))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(?, ?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = SUBSTRING('HELLO WORLD', 1, 5)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR('HELLO WORLD', 1, 5))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(?, ?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = SUBSTRING('HELLO WORLD', 1, ?3)");
            query.setParameter(3, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR('HELLO WORLD', 1, 5))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(?, ?, ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<Integer> strParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> strParam3 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.substring(strParam1, strParam2, strParam3)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO WORLD");
            query.setParameter(strParam2, 1);
            query.setParameter(strParam3, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR('HELLO WORLD', 1, 5))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(?, ?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.substring(cb2.literal("HELLO WORLD"), 1, 5)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR('HELLO WORLD', 1, 5))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(?, ?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal1));
            ParameterExpression<Integer> strParam4 = cb3.parameter(Integer.class);
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.strVal1), cb3.substring(cb3.literal("HELLO WORLD"), cb3.literal(1), strParam4)));

            query = em.createQuery(cquery3);
            query.setParameter(strParam4, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR('HELLO WORLD', 1, 5))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(?, ?, ?))", sql.remove(0));
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

    public void testSubstring1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                           Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = SUBSTRING(?1, ?2, ?3)");
            query.setParameter(1, "HELLO WORLD");
            query.setParameter(2, 1);
            query.setParameter(3, 5);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(?, ?, ?))", sql.remove(0));

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = SUBSTRING('HELLO WORLD', 1, 5)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR('HELLO WORLD', 1, 5))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(?, ?, ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = SUBSTRING('HELLO WORLD', 1, ?3)");
            query.setParameter(3, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR('HELLO WORLD', 1, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(?, ?, ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<Integer> strParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> strParam3 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.substring(strParam1, strParam2, strParam3)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO WORLD");
            query.setParameter(strParam2, 1);
            query.setParameter(strParam3, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(?, ?, ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.substring(cb2.literal("HELLO WORLD"), 1, 5)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR('HELLO WORLD', 1, 5))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(?, ?, ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal1));
            ParameterExpression<Integer> strParam4 = cb3.parameter(Integer.class);
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.strVal1), cb3.substring(cb3.literal("HELLO WORLD"), cb3.literal(1), strParam4)));

            query = em.createQuery(cquery3);
            query.setParameter(strParam4, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(?, ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR('HELLO WORLD', 1, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(?, ?, ?))", sql.remove(0));
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

    public void testSubstring1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                       Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = SUBSTRING(?1, ?2, ?3)");
            query.setParameter(1, "HELLO WORLD");
            query.setParameter(2, 1);
            query.setParameter(3, 5);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(?, ?, ?))", sql.remove(0));

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = SUBSTRING('HELLO WORLD', 1, 5)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(?, ?, ?))", sql.remove(0));

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = SUBSTRING('HELLO WORLD', 1, ?3)");
            query.setParameter(3, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(?, ?, ?))", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<Integer> strParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> strParam3 = cb.parameter(Integer.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.substring(strParam1, strParam2, strParam3)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO WORLD");
            query.setParameter(strParam2, 1);
            query.setParameter(strParam3, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(?, ?, ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.substring(cb2.literal("HELLO WORLD"), 1, 5)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(?, ?, ?))", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal1));
            ParameterExpression<Integer> strParam4 = cb3.parameter(Integer.class);
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.strVal1), cb3.substring(cb3.literal("HELLO WORLD"), cb3.literal(1), strParam4)));

            query = em.createQuery(cquery3);
            query.setParameter(strParam4, 5);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(?, ?, ?))", sql.remove(0));
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testSubstring2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT SUBSTRING(?1, ?2, ?3), s.strVal2 FROM OLGH17837Entity s WHERE s.strVal1 = SUBSTRING(SUBSTRING(s.strVal2, ?2, ?3), ?2, ?4)");
            query.setParameter(1, "HELLO WORLD");
            query.setParameter(2, 1);
            query.setParameter(3, 5);
            query.setParameter(4, 2);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SUBSTR('HELLO WORLD', 1, 5), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, 1, 5), 1, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUBSTR(?, ?, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, ?, ?), ?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT SUBSTRING('HELLO WORLD', 1, 5), s.strVal2 FROM OLGH17837Entity s WHERE s.strVal1 = SUBSTRING(SUBSTRING(s.strVal2, 1, 5), 1, 2)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SUBSTR('HELLO WORLD', 1, 5), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, 1, 5), 1, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUBSTR(?, ?, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, ?, ?), ?, ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT SUBSTRING('HELLO WORLD', 1, ?2), s.strVal2 FROM OLGH17837Entity s WHERE s.strVal1 = SUBSTRING(SUBSTRING(s.strVal2, 1, ?2), 1, ?3)");
            query.setParameter(2, 5);
            query.setParameter(3, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SUBSTR('HELLO WORLD', 1, 5), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, 1, 5), 1, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUBSTR(?, ?, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, ?, ?), ?, ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<Integer> strParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> strParam3 = cb.parameter(Integer.class);
            ParameterExpression<Integer> strParam4 = cb.parameter(Integer.class);
            cquery.multiselect(cb.substring(strParam1, strParam2, strParam3), root.get(OLGH17837Entity_.strVal2));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.substring(cb.substring(root.get(OLGH17837Entity_.strVal2), strParam2, strParam3), strParam2, strParam4)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO WORLD");
            query.setParameter(strParam2, 1);
            query.setParameter(strParam3, 5);
            query.setParameter(strParam4, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SUBSTR('HELLO WORLD', 1, 5), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, 1, 5), 1, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUBSTR(?, ?, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, ?, ?), ?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.substring(cb2.literal("HELLO WORLD"), 1, 5), root2.get(OLGH17837Entity_.strVal2));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.substring(cb2.substring(root2.get(OLGH17837Entity_.strVal2), 1, 5), 1, 2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SUBSTR('HELLO WORLD', 1, 5), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, 1, 5), 1, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUBSTR(?, ?, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, ?, ?), ?, ?))", sql.remove(0));
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal1));
            ParameterExpression<Integer> strParam5 = cb.parameter(Integer.class);
            ParameterExpression<Integer> strParam6 = cb3.parameter(Integer.class);
            cquery3.multiselect(cb3.substring(cb3.literal("HELLO WORLD"), cb3.literal(1), strParam5), root3.get(OLGH17837Entity_.strVal2));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.strVal1),
                                    cb3.substring(cb3.substring(root3.get(OLGH17837Entity_.strVal2), cb3.literal(1), strParam5), cb3.literal(1), strParam6)));

            query = em.createQuery(cquery3);
            query.setParameter(strParam5, 5);
            query.setParameter(strParam6, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT SUBSTR('HELLO WORLD', 1, 5), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, 1, 5), 1, 2))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT SUBSTR(?, ?, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, ?, ?), ?, ?))", sql.remove(0));
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

    public void testSubstring2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                           Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT SUBSTRING(?1, ?2, ?3), s.strVal2 FROM OLGH17837Entity s WHERE s.strVal1 = SUBSTRING(SUBSTRING(s.strVal2, ?2, ?3), ?2, ?4)");
            query.setParameter(1, "HELLO WORLD");
            query.setParameter(2, 1);
            query.setParameter(3, 5);
            query.setParameter(4, 2);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT SUBSTR(?, ?, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, ?, ?), ?, ?))", sql.remove(0));

            query = em.createQuery("SELECT SUBSTRING('HELLO WORLD', 1, 5), s.strVal2 FROM OLGH17837Entity s WHERE s.strVal1 = SUBSTRING(SUBSTRING(s.strVal2, 1, 5), 1, 2)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT SUBSTR(?, ?, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, ?, ?), ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT SUBSTR('HELLO WORLD', 1, 5), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, 1, 5), 1, 2))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT SUBSTR(?, ?, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, ?, ?), ?, ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT SUBSTRING('HELLO WORLD', 1, ?2), s.strVal2 FROM OLGH17837Entity s WHERE s.strVal1 = SUBSTRING(SUBSTRING(s.strVal2, 1, ?2), 1, ?3)");
            query.setParameter(2, 5);
            query.setParameter(3, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT SUBSTR(?, ?, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, ?, ?), ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT SUBSTR('HELLO WORLD', 1, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, 1, ?), 1, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT SUBSTR(?, ?, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, ?, ?), ?, ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<Integer> strParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> strParam3 = cb.parameter(Integer.class);
            ParameterExpression<Integer> strParam4 = cb.parameter(Integer.class);
            cquery.multiselect(cb.substring(strParam1, strParam2, strParam3), root.get(OLGH17837Entity_.strVal2));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.substring(cb.substring(root.get(OLGH17837Entity_.strVal2), strParam2, strParam3), strParam2, strParam4)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO WORLD");
            query.setParameter(strParam2, 1);
            query.setParameter(strParam3, 5);
            query.setParameter(strParam4, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT SUBSTR(?, ?, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, ?, ?), ?, ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.substring(cb2.literal("HELLO WORLD"), 1, 5), root2.get(OLGH17837Entity_.strVal2));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.substring(cb2.substring(root2.get(OLGH17837Entity_.strVal2), 1, 5), 1, 2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT SUBSTR(?, ?, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, ?, ?), ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT SUBSTR('HELLO WORLD', 1, 5), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, 1, 5), 1, 2))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT SUBSTR(?, ?, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, ?, ?), ?, ?))", sql.remove(0));
                }
            }

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal1));
            ParameterExpression<Integer> strParam5 = cb.parameter(Integer.class);
            ParameterExpression<Integer> strParam6 = cb3.parameter(Integer.class);
            cquery3.multiselect(cb3.substring(cb3.literal("HELLO WORLD"), cb3.literal(1), strParam5), root3.get(OLGH17837Entity_.strVal2));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.strVal1),
                                    cb3.substring(cb3.substring(root3.get(OLGH17837Entity_.strVal2), cb3.literal(1), strParam5), cb3.literal(1), strParam6)));

            query = em.createQuery(cquery3);
            query.setParameter(strParam5, 5);
            query.setParameter(strParam6, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                Assert.assertEquals("SELECT SUBSTR(?, ?, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, ?, ?), ?, ?))", sql.remove(0));
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT SUBSTR('HELLO WORLD', 1, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, 1, ?), 1, ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT SUBSTR(?, ?, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, ?, ?), ?, ?))", sql.remove(0));
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

    public void testSubstring2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                       Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT SUBSTRING(?1, ?2, ?3), s.strVal2 FROM OLGH17837Entity s WHERE s.strVal1 = SUBSTRING(SUBSTRING(s.strVal2, ?2, ?3), ?2, ?4)");
            query.setParameter(1, "HELLO WORLD");
            query.setParameter(2, 1);
            query.setParameter(3, 5);
            query.setParameter(4, 2);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT SUBSTR(?, ?, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, ?, ?), ?, ?))", sql.remove(0));

            query = em.createQuery("SELECT SUBSTRING('HELLO WORLD', 1, 5), s.strVal2 FROM OLGH17837Entity s WHERE s.strVal1 = SUBSTRING(SUBSTRING(s.strVal2, 1, 5), 1, 2)");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT SUBSTR(?, ?, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, ?, ?), ?, ?))", sql.remove(0));

            query = em.createQuery("SELECT SUBSTRING('HELLO WORLD', 1, ?2), s.strVal2 FROM OLGH17837Entity s WHERE s.strVal1 = SUBSTRING(SUBSTRING(s.strVal2, 1, ?2), 1, ?3)");
            query.setParameter(2, 5);
            query.setParameter(3, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT SUBSTR(?, ?, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, ?, ?), ?, ?))", sql.remove(0));

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<Integer> strParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> strParam3 = cb.parameter(Integer.class);
            ParameterExpression<Integer> strParam4 = cb.parameter(Integer.class);
            cquery.multiselect(cb.substring(strParam1, strParam2, strParam3), root.get(OLGH17837Entity_.strVal2));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.substring(cb.substring(root.get(OLGH17837Entity_.strVal2), strParam2, strParam3), strParam2, strParam4)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO WORLD");
            query.setParameter(strParam2, 1);
            query.setParameter(strParam3, 5);
            query.setParameter(strParam4, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT SUBSTR(?, ?, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, ?, ?), ?, ?))", sql.remove(0));

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.substring(cb2.literal("HELLO WORLD"), 1, 5), root2.get(OLGH17837Entity_.strVal2));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.substring(cb2.substring(root2.get(OLGH17837Entity_.strVal2), 1, 5), 1, 2)));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT SUBSTR(?, ?, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, ?, ?), ?, ?))", sql.remove(0));

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal1));
            ParameterExpression<Integer> strParam5 = cb.parameter(Integer.class);
            ParameterExpression<Integer> strParam6 = cb3.parameter(Integer.class);
            cquery3.multiselect(cb3.substring(cb3.literal("HELLO WORLD"), cb3.literal(1), strParam5), root3.get(OLGH17837Entity_.strVal2));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.strVal1),
                                    cb3.substring(cb3.substring(root3.get(OLGH17837Entity_.strVal2), cb3.literal(1), strParam5), cb3.literal(1), strParam6)));

            query = em.createQuery(cquery3);
            query.setParameter(strParam5, 5);
            query.setParameter(strParam6, 2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            Assert.assertEquals("SELECT SUBSTR(?, ?, ?), STRVAL2 FROM OLGH17837ENTITY WHERE (STRVAL1 = SUBSTR(SUBSTR(STRVAL2, ?, ?), ?, ?))", sql.remove(0));
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testTrim1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(?2)");
            query.setParameter(2, "  HELLO WORD ");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM('  HELLO WORD ')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.trim(strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "  HELLO WORD ");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.trim(cb2.literal("  HELLO WORD "))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(?))", sql.remove(0));
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

    public void testTrim1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                      Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(?2)");
            query.setParameter(2, "  HELLO WORD ");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM('  HELLO WORD ')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('  HELLO WORD '))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('  HELLO WORD '))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.trim(strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "  HELLO WORD ");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.trim(cb2.literal("  HELLO WORD "))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('  HELLO WORD '))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('  HELLO WORD '))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(?))", sql.remove(0));
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

    public void testTrim1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                  Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(?2)");
            query.setParameter(2, "  HELLO WORD ");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM('  HELLO WORD ')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.trim(strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "  HELLO WORD ");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.trim(cb2.literal("  HELLO WORD "))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('  HELLO WORD '))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(?))", sql.remove(0));
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

    public void testTrim2_1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(?1 FROM ?2)");
            query.setParameter(1, 'A');
            query.setParameter(2, "AAAHELLO WORDAA");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(? FROM ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM('A' FROM 'AAAHELLO WORDAA')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(? FROM ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM('A' FROM ?2)");
            query.setParameter(2, "AAAHELLO WORDAA");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(? FROM ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Character> charParam1 = cb.parameter(Character.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.trim(charParam1, strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(charParam1, 'A');
            query.setParameter(strParam1, "AAAHELLO WORDAA");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(? FROM ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.trim(cb2.literal('A'), cb2.literal("AAAHELLO WORDAA"))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(? FROM ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM('A' FROM ?2)");
            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam2 = cb3.parameter(String.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal1));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.strVal1), cb3.trim(cb3.literal('A'), strParam2)));

            query = em.createQuery(cquery3);
            query.setParameter(strParam2, "AAAHELLO WORDAA");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(? FROM ?))", sql.remove(0));
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

    public void testTrim2_1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                        Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(?1 FROM ?2)");
            query.setParameter(1, 'A');
            query.setParameter(2, "AAAHELLO WORDAA");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(? FROM ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM('A' FROM 'AAAHELLO WORDAA')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
                } else if (isDB2) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(? FROM ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(? FROM ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM('A' FROM ?2)");
            query.setParameter(2, "AAAHELLO WORDAA");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
                } else if (isDB2) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(? FROM ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
                } else if (isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(? FROM ?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Character> charParam1 = cb.parameter(Character.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.trim(charParam1, strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(charParam1, 'A');
            query.setParameter(strParam1, "AAAHELLO WORDAA");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(? FROM ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.trim(cb2.literal('A'), cb2.literal("AAAHELLO WORDAA"))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
                } else if (isDB2) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(? FROM ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(? FROM ?))", sql.remove(0));
                }
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM('A' FROM ?2)");
            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam2 = cb3.parameter(String.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal1));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.strVal1), cb3.trim(cb3.literal('A'), strParam2)));

            query = em.createQuery(cquery3);
            query.setParameter(strParam2, "AAAHELLO WORDAA");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
                } else if (isDB2) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(? FROM ?))", sql.remove(0));
                }
            } else {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
                } else if (isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM ?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(? FROM ?))", sql.remove(0));
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

    public void testTrim2_1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                    Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM(?1 FROM ?2)");
            query.setParameter(1, 'A');
            query.setParameter(2, "AAAHELLO WORDAA");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(? FROM ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM('A' FROM 'AAAHELLO WORDAA')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(? FROM ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM('A' FROM ?2)");
            query.setParameter(2, "AAAHELLO WORDAA");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(? FROM ?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<Character> charParam1 = cb.parameter(Character.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.trim(charParam1, strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(charParam1, 'A');
            query.setParameter(strParam1, "AAAHELLO WORDAA");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(? FROM ?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.trim(cb2.literal('A'), cb2.literal("AAAHELLO WORDAA"))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(? FROM ?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = TRIM('A' FROM ?2)");
            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam2 = cb3.parameter(String.class);
            cquery3.multiselect(root3.get(OLGH17837Entity_.strVal1));
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.strVal1), cb3.trim(cb3.literal('A'), strParam2)));

            query = em.createQuery(cquery3);
            query.setParameter(strParam2, "AAAHELLO WORDAA");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM 'AAAHELLO WORDAA'))", sql.remove(0));
            } else if (isDB2) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM('A' FROM ?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = TRIM(? FROM ?))", sql.remove(0));
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

    public void testTrim2_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT TRIM(?1), s.strVal1 FROM OLGH17837Entity s WHERE s.intVal1 = ?2");
            query.setParameter(1, "  HELLO WORD ");
            query.setParameter(2, 23);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT TRIM('  HELLO WORD '), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = 23)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT TRIM(?), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT TRIM('  HELLO WORD '), s.strVal1 FROM OLGH17837Entity s WHERE s.intVal1 = 23");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT TRIM('  HELLO WORD '), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = 23)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT TRIM(?), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(cb.trim(strParam1), root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), intParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "  HELLO WORD ");
            query.setParameter(intParam1, 23);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT TRIM('  HELLO WORD '), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = 23)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT TRIM(?), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.trim(cb2.literal("  HELLO WORD ")), root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), 23));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT TRIM('  HELLO WORD '), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = 23)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT TRIM(?), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
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

    public void testTrim2_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                      Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT TRIM(?1), s.strVal1 FROM OLGH17837Entity s WHERE s.intVal1 = ?2");
            query.setParameter(1, "  HELLO WORD ");
            query.setParameter(2, 23);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT TRIM('  HELLO WORD '), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT TRIM(?), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT TRIM('  HELLO WORD '), s.strVal1 FROM OLGH17837Entity s WHERE s.intVal1 = 23");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT TRIM('  HELLO WORD '), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT TRIM(?), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT TRIM('  HELLO WORD '), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = 23)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT TRIM(?), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(cb.trim(strParam1), root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), intParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "  HELLO WORD ");
            query.setParameter(intParam1, 23);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT TRIM('  HELLO WORD '), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT TRIM(?), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.trim(cb2.literal("  HELLO WORD ")), root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), 23));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z) {
                    Assert.assertEquals("SELECT TRIM('  HELLO WORD '), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT TRIM(?), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT TRIM('  HELLO WORD '), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = 23)", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT TRIM(?), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
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

    public void testTrim2_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                  Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT TRIM(?1), s.strVal1 FROM OLGH17837Entity s WHERE s.intVal1 = ?2");
            query.setParameter(1, "  HELLO WORD ");
            query.setParameter(2, 23);
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT TRIM('  HELLO WORD '), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT TRIM(?), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            query = em.createQuery("SELECT TRIM('  HELLO WORD '), s.strVal1 FROM OLGH17837Entity s WHERE s.intVal1 = 23");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT TRIM('  HELLO WORD '), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT TRIM(?), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            ParameterExpression<Integer> intParam1 = cb.parameter(Integer.class);
            cquery.multiselect(cb.trim(strParam1), root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.intVal1), intParam1));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "  HELLO WORD ");
            query.setParameter(intParam1, 23);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT TRIM('  HELLO WORD '), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT TRIM(?), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(cb2.trim(cb2.literal("  HELLO WORD ")), root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.intVal1), 23));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z) {
                Assert.assertEquals("SELECT TRIM('  HELLO WORD '), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT TRIM(?), STRVAL1 FROM OLGH17837ENTITY WHERE (INTVAL1 = ?)", sql.remove(0));
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

    public void testUpper1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = UPPER(?1)");
            query.setParameter(1, "HELLO WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UCASE('HELLO WORLD'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UPPER(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = UPPER('HELLO WORLD')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UCASE('HELLO WORLD'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UPPER(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.upper(strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UCASE('HELLO WORLD'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UPPER(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.upper(cb2.literal("HELLO WORLD"))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UCASE('HELLO WORLD'))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UPPER(?))", sql.remove(0));
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

    public void testUpper1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = UPPER(?1)");
            query.setParameter(1, "HELLO WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UCASE(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UPPER(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = UPPER('HELLO WORLD')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UCASE(?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UPPER(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UCASE('HELLO WORLD'))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UPPER(?))", sql.remove(0));
                }
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.upper(strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UCASE(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UPPER(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.upper(cb2.literal("HELLO WORLD"))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UCASE(?))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UPPER(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UCASE('HELLO WORLD'))", sql.remove(0));
                } else {
                    Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UPPER(?))", sql.remove(0));
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

    public void testUpper1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                   Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
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

            Query query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = UPPER(?1)");
            query.setParameter(1, "HELLO WORLD");
            query.getResultList();
            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UCASE(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UPPER(?))", sql.remove(0));
            }

            query = em.createQuery("SELECT s.strVal1 FROM OLGH17837Entity s WHERE s.strVal1 = UPPER('HELLO WORLD')");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UCASE(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UPPER(?))", sql.remove(0));
            }

            // -----------------------

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);
            ParameterExpression<String> strParam1 = cb.parameter(String.class);
            cquery.multiselect(root.get(OLGH17837Entity_.strVal1));
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal1), cb.upper(strParam1)));

            query = em.createQuery(cquery);
            query.setParameter(strParam1, "HELLO WORLD");
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UCASE(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UPPER(?))", sql.remove(0));
            }

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery2 = cb2.createQuery(Object[].class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.multiselect(root2.get(OLGH17837Entity_.strVal1));
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal1), cb2.upper(cb2.literal("HELLO WORLD"))));

            query = em.createQuery(cquery2);
            query.getResultList();
            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UCASE(?))", sql.remove(0));
            } else {
                Assert.assertEquals("SELECT STRVAL1 FROM OLGH17837ENTITY WHERE (STRVAL1 = UPPER(?))", sql.remove(0));
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
