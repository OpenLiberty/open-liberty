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
import javax.persistence.criteria.CriteriaUpdate;
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

public class TestUpdateLogic extends AbstractTestLogic {

    public void testUpdate1_Default(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
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

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            Query query = em.createQuery("UPDATE OLGH17837Entity s SET s.intVal1 = ?1 WHERE s.strVal2 = LOWER(?2)");
            query.setParameter(1, 9);
            query.setParameter(2, "HELLO");
            query.executeUpdate();

            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = 9 WHERE (STRVAL2 = LCASE('HELLO'))", sql.remove(0));
            } else {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LOWER(?))", sql.remove(0));
            }

            System.out.println("Rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            query = em.createQuery("UPDATE OLGH17837Entity s SET s.intVal1 = 9 WHERE s.strVal2 = LOWER('HELLO')");
            query.executeUpdate();

            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = 9 WHERE (STRVAL2 = LCASE('HELLO'))", sql.remove(0));
            } else {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LOWER(?))", sql.remove(0));
            }

            System.out.println("Rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            query = em.createQuery("UPDATE OLGH17837Entity s SET s.intVal1 = ?1 WHERE s.strVal2 = LOWER('HELLO')");
            query.setParameter(1, 9);
            query.executeUpdate();

            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = 9 WHERE (STRVAL2 = LCASE('HELLO'))", sql.remove(0));
            } else {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LOWER(?))", sql.remove(0));
            }

            System.out.println("Rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();

            // -----------------------

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaUpdate<OLGH17837Entity> cquery = cb.createCriteriaUpdate(OLGH17837Entity.class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);

            ParameterExpression<Integer> intValue = cb.parameter(Integer.class);
            ParameterExpression<String> strValue = cb.parameter(String.class);
            cquery.set(root.get(OLGH17837Entity_.intVal1), intValue);
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal2), cb.lower(strValue)));

            query = em.createQuery(cquery);
            query.setParameter(intValue, 9);
            query.setParameter(strValue, "HELLO");
            query.executeUpdate();

            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = 9 WHERE (STRVAL2 = LCASE('HELLO'))", sql.remove(0));
            } else {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LOWER(?))", sql.remove(0));
            }

            System.out.println("Rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaUpdate<OLGH17837Entity> cquery2 = cb2.createCriteriaUpdate(OLGH17837Entity.class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.set(root2.get(OLGH17837Entity_.intVal1), 9);
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal2), cb2.lower(cb2.literal("HELLO"))));

            query = em.createQuery(cquery2);
            query.executeUpdate();

            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = 9 WHERE (STRVAL2 = LCASE('HELLO'))", sql.remove(0));
            } else {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LOWER(?))", sql.remove(0));
            }

            System.out.println("Rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaUpdate<OLGH17837Entity> cquery3 = cb3.createCriteriaUpdate(OLGH17837Entity.class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);

            ParameterExpression<String> strValue1 = cb3.parameter(String.class);
            cquery3.set(root3.get(OLGH17837Entity_.intVal1), 9);
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.strVal2), cb2.lower(strValue1)));

            query = em.createQuery(cquery3);
            query.setParameter(strValue1, "HELLO");
            query.executeUpdate();

            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = 9 WHERE (STRVAL2 = LCASE('HELLO'))", sql.remove(0));
            } else {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LOWER(?))", sql.remove(0));
            }

            System.out.println("Rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testUpdate1_PartialBind(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                        Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
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

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            Query query = em.createQuery("UPDATE OLGH17837Entity s SET s.intVal1 = ?1 WHERE s.strVal2 = LOWER(?2)");
            query.setParameter(1, 9);
            query.setParameter(2, "HELLO");
            query.executeUpdate();

            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LCASE(?))", sql.remove(0));
            } else {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LOWER(?))", sql.remove(0));
            }

            System.out.println("Rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            query = em.createQuery("UPDATE OLGH17837Entity s SET s.intVal1 = 9 WHERE s.strVal2 = LOWER('HELLO')");
            query.executeUpdate();

            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LCASE(?))", sql.remove(0));
                } else {
                    Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LOWER(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = 9 WHERE (STRVAL2 = LCASE('HELLO'))", sql.remove(0));
                } else {
                    Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LOWER(?))", sql.remove(0));
                }
            }

            query = em.createQuery("UPDATE OLGH17837Entity s SET s.intVal1 = ?1 WHERE s.strVal2 = LOWER('HELLO')");
            query.setParameter(1, 9);
            query.executeUpdate();

            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LCASE(?))", sql.remove(0));
                } else {
                    Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LOWER(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LCASE('HELLO'))", sql.remove(0));
                } else {
                    Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LOWER(?))", sql.remove(0));
                }
            }

            System.out.println("Rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();

            // -----------------------

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaUpdate<OLGH17837Entity> cquery = cb.createCriteriaUpdate(OLGH17837Entity.class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);

            ParameterExpression<Integer> intValue = cb.parameter(Integer.class);
            ParameterExpression<String> strValue = cb.parameter(String.class);
            cquery.set(root.get(OLGH17837Entity_.intVal1), intValue);
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal2), cb.lower(strValue)));

            query = em.createQuery(cquery);
            query.setParameter(intValue, 9);
            query.setParameter(strValue, "HELLO");
            query.executeUpdate();

            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LCASE(?))", sql.remove(0));
            } else {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LOWER(?))", sql.remove(0));
            }

            System.out.println("Rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaUpdate<OLGH17837Entity> cquery2 = cb2.createCriteriaUpdate(OLGH17837Entity.class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.set(root2.get(OLGH17837Entity_.intVal1), 9);
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal2), cb2.lower(cb2.literal("HELLO"))));

            query = em.createQuery(cquery2);
            query.executeUpdate();

            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LCASE(?))", sql.remove(0));
                } else {
                    Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LOWER(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = 9 WHERE (STRVAL2 = LCASE('HELLO'))", sql.remove(0));
                } else {
                    Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LOWER(?))", sql.remove(0));
                }
            }

            System.out.println("Rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaUpdate<OLGH17837Entity> cquery3 = cb3.createCriteriaUpdate(OLGH17837Entity.class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);

            ParameterExpression<String> strValue1 = cb3.parameter(String.class);
            cquery3.set(root3.get(OLGH17837Entity_.intVal1), 9);
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.strVal2), cb2.lower(strValue1)));

            query = em.createQuery(cquery3);
            query.setParameter(strValue1, "HELLO");
            query.executeUpdate();

            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isUsingJPA31Feature() || isUsingJPA32Feature()) {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LCASE(?))", sql.remove(0));
                } else {
                    Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LOWER(?))", sql.remove(0));
                }
            } else {
                if (isDB2Z || isDB2 || isDerby) {
                    Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = 9 WHERE (STRVAL2 = LCASE(?))", sql.remove(0));
                } else {
                    Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LOWER(?))", sql.remove(0));
                }
            }

            System.out.println("Rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testUpdate1_PartialBind_BindLiteral(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                    Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
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

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            Query query = em.createQuery("UPDATE OLGH17837Entity s SET s.intVal1 = ?1 WHERE s.strVal2 = LOWER(?2)");
            query.setParameter(1, 9);
            query.setParameter(2, "HELLO");
            query.executeUpdate();

            List<String> sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LCASE(?))", sql.remove(0));
            } else {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LOWER(?))", sql.remove(0));
            }

            System.out.println("Rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            query = em.createQuery("UPDATE OLGH17837Entity s SET s.intVal1 = 9 WHERE s.strVal2 = LOWER('HELLO')");
            query.executeUpdate();

            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LCASE(?))", sql.remove(0));
            } else {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LOWER(?))", sql.remove(0));
            }

            System.out.println("Rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            query = em.createQuery("UPDATE OLGH17837Entity s SET s.intVal1 = ?1 WHERE s.strVal2 = LOWER('HELLO')");
            query.setParameter(1, 9);
            query.executeUpdate();

            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LCASE(?))", sql.remove(0));
            } else {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LOWER(?))", sql.remove(0));
            }

            System.out.println("Rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();

            // -----------------------

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaUpdate<OLGH17837Entity> cquery = cb.createCriteriaUpdate(OLGH17837Entity.class);
            Root<OLGH17837Entity> root = cquery.from(OLGH17837Entity.class);

            ParameterExpression<Integer> intValue = cb.parameter(Integer.class);
            ParameterExpression<String> strValue = cb.parameter(String.class);
            cquery.set(root.get(OLGH17837Entity_.intVal1), intValue);
            cquery.where(cb.equal(root.get(OLGH17837Entity_.strVal2), cb.lower(strValue)));

            query = em.createQuery(cquery);
            query.setParameter(intValue, 9);
            query.setParameter(strValue, "HELLO");
            query.executeUpdate();

            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LCASE(?))", sql.remove(0));
            } else {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LOWER(?))", sql.remove(0));
            }

            System.out.println("Rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            CriteriaBuilder cb2 = em.getCriteriaBuilder();
            CriteriaUpdate<OLGH17837Entity> cquery2 = cb2.createCriteriaUpdate(OLGH17837Entity.class);
            Root<OLGH17837Entity> root2 = cquery2.from(OLGH17837Entity.class);
            cquery2.set(root2.get(OLGH17837Entity_.intVal1), 9);
            cquery2.where(cb2.equal(root2.get(OLGH17837Entity_.strVal2), cb2.lower(cb2.literal("HELLO"))));

            query = em.createQuery(cquery2);
            query.executeUpdate();

            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LCASE(?))", sql.remove(0));
            } else {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LOWER(?))", sql.remove(0));
            }

            System.out.println("Rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaUpdate<OLGH17837Entity> cquery3 = cb3.createCriteriaUpdate(OLGH17837Entity.class);
            Root<OLGH17837Entity> root3 = cquery3.from(OLGH17837Entity.class);

            ParameterExpression<String> strValue1 = cb3.parameter(String.class);
            cquery3.set(root3.get(OLGH17837Entity_.intVal1), 9);
            cquery3.where(cb3.equal(root3.get(OLGH17837Entity_.strVal2), cb2.lower(strValue1)));

            query = em.createQuery(cquery3);
            query.setParameter(strValue1, "HELLO");
            query.executeUpdate();

            sql = SQLCallListener.getAndClearCallList();
            Assert.assertEquals(1, sql.size());
            if (isDB2Z || isDB2 || isDerby) {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LCASE(?))", sql.remove(0));
            } else {
                Assert.assertEquals("UPDATE OLGH17837ENTITY SET INTVAL1 = ? WHERE (STRVAL2 = LOWER(?))", sql.remove(0));
            }

            System.out.println("Rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();
        } catch (java.lang.AssertionError ae) {
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
