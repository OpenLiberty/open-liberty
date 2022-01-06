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

package com.ibm.ws.jpa.olgh8294.testlogic;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.Assert;

import com.ibm.ws.jpa.query.sqlcapture.SQLListener;
import com.ibm.ws.testtooling.database.DatabaseVendor;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH8294Logic extends AbstractTestLogic {

    public void testCOALESCE_ForceBindJPQLParameters(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DERBY);
        final boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2);

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // Test 1
            // Test COALESCE function with positional parameter and typed parameter
            // Expecting query parameter binding to not be set
            {
                System.out.println("testCOALESCE_ForceBindJPQLParameters Test #001");
                SQLListener.getAndClearSQLList();
                String query01Str = "SELECT 1 FROM SimpleEntityOLGH8294 s WHERE ABS(COALESCE(s.itemInteger1, ?1)) >= ?2";
                Query query = em.createQuery(query01Str);
                query.setParameter(1, 0);
                query.setParameter(2, 99);

                final List<?> resultList = query.getResultList();
                Assert.assertNotNull(resultList);
                Assert.assertEquals("Expecting 40 entries in the result list", 40, resultList.size());

                List<String> sql = SQLListener.getAndClearSQLList();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
                if (isDerby || isDB2) {
                    String expected = "SELECT 1 FROM SIMPLEENTITYOLGH8294 WHERE (ABS(COALESCE(ITEM_INTEGER1, ?)) >= ?)";
                    Assert.assertEquals(expected, sql.get(0));
                } // TODO: other databases

                em.clear();
            }

            // Test 2
            // Test COALESCE function with literal and typed parameter
            // Expecting query parameter binding to not be set
            {
                System.out.println("testCOALESCE_ForceBindJPQLParameters Test #002");
                SQLListener.getAndClearSQLList();
                String query01Str = "SELECT 1 FROM SimpleEntityOLGH8294 s WHERE ABS(COALESCE(s.itemInteger1, 0)) >= 99";
                Query query = em.createQuery(query01Str);

                final List<?> resultList = query.getResultList();
                Assert.assertNotNull(resultList);
                Assert.assertEquals("Expecting 40 entries in the result list", 40, resultList.size());

                List<String> sql = SQLListener.getAndClearSQLList();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
                if (isDerby || isDB2) {
                    String expected = "SELECT 1 FROM SIMPLEENTITYOLGH8294 WHERE (ABS(COALESCE(ITEM_INTEGER1, 0)) >= 99)";
                    Assert.assertEquals(expected, sql.get(0));
                } // TODO: other databases

                em.clear();
            }

            // Test 3
            // Test COALESCE function with all arguments as parameters
            // Expecting query parameter binding to not be set
            {
                System.out.println("testCOALESCE_ForceBindJPQLParameters Test #003");
                SQLListener.getAndClearSQLList();
                String query01Str = "SELECT 1 FROM SimpleEntityOLGH8294 s WHERE ABS(COALESCE(?1, ?2)) >= ?3";
                Query query = em.createQuery(query01Str);
                query.setParameter(1, new Integer(1));
                query.setParameter(2, new Integer(20));
                query.setParameter(3, new Integer(300));

                try {
                    final List<?> resultList = query.getResultList();
                    Assert.assertNotNull(resultList);
                    Assert.assertEquals("Expecting 0 entries in the result list", 0, resultList.size());

                    if (isDerby || isDB2) {
                        // Expected a failure
                        Assert.fail("Query did not throw expected Exception on derby and db2 platforms.");
                    }

                    List<String> sql = SQLListener.getAndClearSQLList();
                    Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
                } catch (Throwable t) {
                    // If all Arguments of COALESCE are untyped parameters, this is expected to fail for DB2/z and DB2 LUW
                    // Derby: All the arguments to the COALESCE/VALUE function cannot be parameters.
                    //  The function needs at least one argument that is not a parameter. Error 42610.
                    if (isDerby || isDB2) {
                        // Expected
                    } else {
                        throw t;
                    }
                }

                em.clear();
            }

        } catch (java.lang.AssertionError /* | org.junit.internal.AssumptionViolatedException */ ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testABS_ForceBindJPQLParameters(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DERBY);
        final boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2);
        final boolean isDB2ZOS = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2ZOS);

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // Test 1
            // Test ABS function with parameter
            // Expecting query parameter binding to not be set
            {
                System.out.println("testABS_ForceBindJPQLParameters Test #001");
                SQLListener.getAndClearSQLList();
                String query01Str = "SELECT 2, COUNT(ABS(?1)) FROM SimpleEntityOLGH8294 s WHERE s.itemInteger1 = ABS(?1)";
                Query query = em.createQuery(query01Str);
                query.setParameter(1, -102);

                // Expecting exception on db2/z
                try {
                    final List<?> resultList = query.getResultList();
                    Assert.assertNotNull(resultList);
                    Assert.assertEquals("Expecting 1 entries in the result list", 1, resultList.size());

                    if (isDB2ZOS) {
                        // Expected this to fail.
                        Assert.fail("Query did not throw expected Exception on DB2 on Z platform.");
                    }

                    List<String> sql = SQLListener.getAndClearSQLList();
                    Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
                    if (isDerby || isDB2) {
                        String expected = "SELECT 2, COUNT(ABS(?)) FROM SIMPLEENTITYOLGH8294 WHERE (ITEM_INTEGER1 = ABS(?))";
                        Assert.assertEquals(expected, sql.get(0));
                    } // TODO: other databases
                } catch (Throwable t) {
                    // Expecting exception on db2/z
                    if (isDB2ZOS) {
                        // Expected
                    } else {
                        throw t;
                    }
                }

                em.clear();
            }

            // Test 2
            // Test ABS function with literal
            // Expecting query parameter binding to not be set
            {
                System.out.println("testABS_ForceBindJPQLParameters Test #002");
                SQLListener.getAndClearSQLList();
                String queryStr = "SELECT 2, COUNT(ABS(-3)) FROM SimpleEntityOLGH8294 s WHERE s.itemInteger1 = ABS(-3)";
                Query query = em.createQuery(queryStr);

                final List<?> resultList = query.getResultList();
                Assert.assertNotNull(resultList);

                List<String> sql = SQLListener.getAndClearSQLList();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
                if (isDerby || isDB2) {
                    String expected = "SELECT 2, COUNT(ABS(-3)) FROM SIMPLEENTITYOLGH8294 WHERE (ITEM_INTEGER1 = ABS(-3))";
                    Assert.assertEquals(expected, sql.get(0));
                } // TODO: other databases

                em.clear();
            }
        } catch (java.lang.AssertionError /* | org.junit.internal.AssumptionViolatedException */ ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCONCAT_ForceBindJPQLParameters(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DERBY);
        final boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2);
        final boolean isDB2ZOS = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2ZOS);

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // Test 1
            // Test string CONCAT with untyped parameter and literal
            {
                System.out.println("testCONCAT_ForceBindJPQLParameters Test #001");
                SQLListener.getAndClearSQLList();
                String queryStr = "SELECT 2 FROM SimpleEntityOLGH8294 s " +
                                  "WHERE s.itemString1 = TRIM(CONCAT(?1 , '-')) " +
                                  "AND s.itemString1 = TRIM(CONCAT(?2 , '-'))";
                Query query = em.createQuery(queryStr);
                query.setParameter(1, "1");
                query.setParameter(2, "99");

                final List<?> resultList = query.getResultList();
                Assert.assertNotNull(resultList);
                System.out.println("resultList size = " + resultList.size());
                Assert.assertEquals("Expecting 0 entries in the result list", 0, resultList.size());

                List<String> sql = SQLListener.getAndClearSQLList();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
                if (isDerby || isDB2) {
                    String expected = "SELECT 2 FROM SIMPLEENTITYOLGH8294 WHERE ((ITEM_STRING1 = TRIM(VARCHAR(? || '-'))) AND (ITEM_STRING1 = TRIM(VARCHAR(? || '-'))))";
                    Assert.assertEquals(expected, sql.get(0));
                } // TODO: other databases

                em.clear();
            }

            // Test 2
            // Test string CONCAT with untyped parameter and untyped parameter
            // Expect failure with derby/db2
            {
                System.out.println("testCONCAT_ForceBindJPQLParameters Test #002");
                SQLListener.getAndClearSQLList();
                String queryStr = "SELECT 2 FROM SimpleEntityOLGH8294 s WHERE " +
                                  " s.itemString1 = TRIM(CONCAT(?1 , ?1)) " +
                                  " AND s.itemString1 = TRIM(CONCAT(?2 , ?2))";
                Query query = em.createQuery(queryStr);
                query.setParameter(1, "1");
                query.setParameter(2, "99");

                try {
                    final List<?> resultList = query.getResultList();
                    if (isDerby || isDB2ZOS) {
                        // This is expected to fail on Derby and DB2 for z/OS
                    }

                    Assert.assertNotNull(resultList);

                    List<String> sql = SQLListener.getAndClearSQLList();
                    Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());

                    if (isDB2) {
                        String expected = "SELECT 2 FROM SIMPLEENTITYOLGH8294 WHERE ((ITEM_STRING1 = TRIM(VARCHAR(? || ?))) AND (ITEM_STRING1 = TRIM(VARCHAR(? || ?))))";
                        Assert.assertEquals(expected, sql.get(0));
                    } // TODO: other databases
                } catch (Throwable t) {
                    if (isDerby || isDB2ZOS) {
                        // This is expected to fail
                        // When all the operands of an IN predicate are untyped parameters, error on DB2/z
                        // Use as the left operand of an IN list is not allowed when all operands are untyped parameters, error 42X35 on Derby
                    } else {
                        throw t;
                    }
                }

                em.clear();
            }
        } catch (java.lang.AssertionError /* | org.junit.internal.AssumptionViolatedException */ ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testEXISTS_ForceBindJPQLParameters(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DERBY);
        final boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2);

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // Test 1
            // Test string parameter in sub query
            {
                System.out.println("testEXISTS_ForceBindJPQLParameters Test #001");
                SQLListener.getAndClearSQLList();
                String queryStr = "SELECT s FROM SimpleEntityOLGH8294 s " +
                                  "WHERE s.itemString1 = ?1  AND " +
                                  "EXISTS (SELECT 1 FROM SimpleEntityOLGH8294 e WHERE s.itemInteger1 = ?2 )";;
                Query query = em.createQuery(queryStr);
                query.setParameter(1, "Test");
                query.setParameter(2, 33);

                final List<?> resultList = query.getResultList();
                Assert.assertNotNull(resultList);
                System.out.println("resultList size = " + resultList.size());
//                Assert.assertEquals("Expecting 1 entries in the result list", 1, resultList.size());

                List<String> sql = SQLListener.getAndClearSQLList();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
                if (isDerby || isDB2) {
                    String expected = "SELECT t0.KEY_CHAR, t0.ITEM_BOOLEAN1, t0.ITEM_DATE1, t0.ITEM_INTEGER1, t0.ITEM_STRING1 FROM SIMPLEENTITYOLGH8294 t0 WHERE ((t0.ITEM_STRING1 = ?) AND EXISTS (SELECT 1 FROM SIMPLEENTITYOLGH8294 t1 WHERE (t0.ITEM_INTEGER1 = ?)) )";
                    Assert.assertEquals(expected, sql.get(0));
                } // TODO: other databases

                em.clear();
            }

            // Test 2
            // Test string literal in sub query
            {
                System.out.println("testEXISTS_ForceBindJPQLParameters Test #002");
                SQLListener.getAndClearSQLList();
                String queryStr = "SELECT s FROM SimpleEntityOLGH8294 s WHERE " +
                                  " s.itemString1 = 'Test' " +
                                  " AND EXISTS (SELECT 1 FROM SimpleEntityOLGH8294 e WHERE s.itemInteger1 = 33 )";
                Query query = em.createQuery(queryStr);

                final List<?> resultList = query.getResultList();
                Assert.assertNotNull(resultList);

                List<String> sql = SQLListener.getAndClearSQLList();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
                if (isDerby || isDB2) {
                    String expected = "SELECT t0.KEY_CHAR, t0.ITEM_BOOLEAN1, t0.ITEM_DATE1, t0.ITEM_INTEGER1, t0.ITEM_STRING1 FROM SIMPLEENTITYOLGH8294 t0 WHERE ((t0.ITEM_STRING1 = 'Test') AND EXISTS (SELECT 1 FROM SIMPLEENTITYOLGH8294 t1 WHERE (t0.ITEM_INTEGER1 = 33)) )";
                    Assert.assertEquals(expected, sql.get(0));
                } // TODO: other databases

                em.clear();
            }
        } catch (java.lang.AssertionError /* | org.junit.internal.AssumptionViolatedException */ ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testNUMERICALEXPRESSION_ForceBindJPQLParameters(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DERBY);
        final boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2);

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // Test 1
            // Test numerical expression with untyped parameters + typed parameters
            {
                System.out.println("testNUMERICALEXPRESSION_ForceBindJPQLParameters Test #001");
                SQLListener.getAndClearSQLList();
                String queryStr = "SELECT (s.itemInteger1 + ?4) FROM SimpleEntityOLGH8294 s " +
                                  "WHERE (s.itemInteger1 + ?4) > 1";
                Query query = em.createQuery(queryStr);
                query.setParameter(4, 2);

                final List<?> resultList = query.getResultList();
                Assert.assertNotNull(resultList);
                System.out.println("resultList size = " + resultList.size());
                Assert.assertEquals("Expecting 40 entries in the result list", 40, resultList.size());

                List<String> sql = SQLListener.getAndClearSQLList();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
                if (isDerby || isDB2) {
                    String expected = "SELECT (ITEM_INTEGER1 + ?) FROM SIMPLEENTITYOLGH8294 WHERE ((ITEM_INTEGER1 + ?) > 1)";
                    Assert.assertEquals(expected, sql.get(0));
                } // TODO: other databases

                em.clear();
            }

            // Test 2
            // Test numerical expression with parameters
            {
                System.out.println("testNUMERICALEXPRESSION_ForceBindJPQLParameters Test #002");
                SQLListener.getAndClearSQLList();
                String queryStr = "SELECT (?3 + ?4) FROM SimpleEntityOLGH8294 s WHERE (?3 + ?4) > 1 ";
                Query query = em.createQuery(queryStr);
                query.setParameter(3, 2);
                query.setParameter(4, 2);

                try {
                    final List<?> resultList = query.getResultList();
                    Assert.assertNotNull(resultList);

                    List<String> sql = SQLListener.getAndClearSQLList();
                    Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());

                    System.out.println("resultList size = " + resultList.size());

                    if (isDerby) {
                        Assert.fail("Expected failure  with database product " + dbProductName);
                    }
                } catch (Throwable t) {
                    if (isDerby) {
                        // This is expected to fail
                        // When all the operands of a numeric expression are untyped parameters, error 42X35 on Derby
                    } else {
                        throw t;
                    }
                }

                em.clear();
            }

            // Test 3
            // Test numerical expression with literals
            {
                System.out.println("testNUMERICALEXPRESSION_ForceBindJPQLParameters Test #003");
                SQLListener.getAndClearSQLList();
                String queryStr = "SELECT (s.itemInteger1 + 4) FROM SimpleEntityOLGH8294 s " +
                                  "WHERE ABS(s.itemInteger1 + 4) > 1";
                Query query = em.createQuery(queryStr);

                final List<?> resultList = query.getResultList();
                Assert.assertNotNull(resultList);
                System.out.println("resultList size = " + resultList.size());
                Assert.assertEquals("Expecting 40 entries in the result list", 40, resultList.size());

                List<String> sql = SQLListener.getAndClearSQLList();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
                if (isDerby || isDB2) {
                    String expected = "SELECT (ITEM_INTEGER1 + 4) FROM SIMPLEENTITYOLGH8294 WHERE (ABS((ITEM_INTEGER1 + 4)) > 1)";
                    Assert.assertEquals(expected, sql.get(0));
                } // TODO: other databases

                em.clear();
            }
        } catch (java.lang.AssertionError /* | org.junit.internal.AssumptionViolatedException */ ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testIN_ForceBindJPQLParameters(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DERBY);
        final boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2);
        final boolean isDB2ZOS = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2ZOS);

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // Test 1
            // Test all the operands of an IN predicate
            {
                System.out.println("testIN_ForceBindJPQLParameters Test #001");
                SQLListener.getAndClearSQLList();
                String queryStr = "SELECT 2 FROM SimpleEntityOLGH8294 s " +
                                  "WHERE ?1 IN (?2, ?3, ?4)";
                Query query = em.createQuery(queryStr);
                query.setParameter(1, 4);
                query.setParameter(2, 4);
                query.setParameter(3, 5);
                query.setParameter(4, 6);

                try {
                    final List<?> resultList = query.getResultList();

                    // Expect failure with derby and DB2/z
                    if (isDerby || isDB2ZOS) {
                        Assert.fail("Expected failure with database product " + dbProductName);
                    }

                    Assert.assertNotNull(resultList);
                    System.out.println("resultList size = " + resultList.size());
                    Assert.assertEquals("Expecting 40 entries in the result list", 40, resultList.size());

                    List<String> sql = SQLListener.getAndClearSQLList();
                    Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());

                    if (isDB2) {
                        String expected = "SELECT 2 FROM SIMPLEENTITYOLGH8294 WHERE (? IN (?, ?, ?))";
                        Assert.assertEquals(expected, sql.get(0));
                    } // TODO: other databases
                } catch (Throwable t) {
                    if (isDerby || isDB2ZOS) {
                        // This is expected to fail
                        // Use as the left operand of an IN list is not allowed when all operands are untyped parameters, error 42X35 on Derby                    } else {
                        // When all the operands of an IN predicate are untyped parameters, error on DB2/z
                    } else {
                        throw t;
                    }
                }

                em.clear();
            }

            // Test 2
            // Test the first and second operands of an IN predicate
            {
                System.out.println("testIN_ForceBindJPQLParameters Test #002");
                SQLListener.getAndClearSQLList();
                String queryStr = "SELECT 2 FROM SimpleEntityOLGH8294 s WHERE ?1 IN (?2, 'b', 'c')";
                Query query = em.createQuery(queryStr);
                query.setParameter(1, "a");
                query.setParameter(2, "a");

                final List<?> resultList = query.getResultList();
                Assert.assertNotNull(resultList);
                System.out.println("resultList size = " + resultList.size());
                Assert.assertEquals("Expecting 40 entries in the result list", 40, resultList.size());

                List<String> sql = SQLListener.getAndClearSQLList();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
                if (isDerby || isDB2) {
                    String expected = "SELECT 2 FROM SIMPLEENTITYOLGH8294 WHERE (? IN (?, 'b', 'c'))";
                    Assert.assertEquals(expected, sql.get(0));
                } // TODO: other databases

                em.clear();
            }

            // Test 3
            // Test the first operand of an IN predicate and zero or more operands of the IN list except for the first operand of the IN list
            {
                System.out.println("testIN_ForceBindJPQLParameters Test #003");
                SQLListener.getAndClearSQLList();
                String queryStr = "SELECT 2 FROM SimpleEntityOLGH8294 s " +
                                  "WHERE ?1 IN (5, ?2, 6)";
                Query query = em.createQuery(queryStr);
                query.setParameter(1, 4);
                query.setParameter(2, 4);

                final List<?> resultList = query.getResultList();
                Assert.assertNotNull(resultList);
                System.out.println("resultList size = " + resultList.size());
                Assert.assertEquals("Expecting 40 entries in the result list", 40, resultList.size());

                List<String> sql = SQLListener.getAndClearSQLList();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
                if (isDerby || isDB2) {
                    String expected = "SELECT 2 FROM SIMPLEENTITYOLGH8294 WHERE (? IN (5, ?, 6))";
                    Assert.assertEquals(expected, sql.get(0));
                } // TODO: other databases

                em.clear();
            }

            // Test 4
            // Test any or all operands of the IN list of the IN predicate and the first operand of the IN predicate is not an untyped parameter marker            {
            {
                System.out.println("testIN_ForceBindJPQLParameters Test #004");
                SQLListener.getAndClearSQLList();
                String queryStr = "SELECT 2 FROM SimpleEntityOLGH8294 s " +
                                  "WHERE s.itemString1 IN (?1, 'b', ?2)";
                Query query = em.createQuery(queryStr);
                query.setParameter(1, "a");
                query.setParameter(2, "c");

                final List<?> resultList = query.getResultList();
                Assert.assertNotNull(resultList);
                System.out.println("resultList size = " + resultList.size());
                Assert.assertEquals("Expecting 0 entries in the result list", 0, resultList.size());

                List<String> sql = SQLListener.getAndClearSQLList();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
                if (isDerby || isDB2) {
                    String expected = "SELECT 2 FROM SIMPLEENTITYOLGH8294 WHERE (ITEM_STRING1 IN (?, 'b', ?))";
                    Assert.assertEquals(expected, sql.get(0));
                } // TODO: other databases

                em.clear();

            }
        } catch (java.lang.AssertionError /* | org.junit.internal.AssumptionViolatedException */ ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testLIKE_ForceBindJPQLParameters(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DERBY);
        final boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2);

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // Test 1
            // Test the first operand of a LIKE predicate (the match-expression) is untyped parameter
            // when at least one other operand (the pattern-expression or escape-expression)
            // is not an untyped parameter marker
            {
                System.out.println("testLIKE_ForceBindJPQLParameters Test #001");
                SQLListener.getAndClearSQLList();
                String queryStr = "SELECT 1 FROM SimpleEntityOLGH8294 s " +
                                  "WHERE ?1 LIKE ?2 ESCAPE '_'";
                Query query = em.createQuery(queryStr);
                query.setParameter(1, "HELLO_WORLD");
                query.setParameter(2, "HELLO%");

                final List<?> resultList = query.getResultList();
                Assert.assertNotNull(resultList);
                System.out.println("resultList size = " + resultList.size());
                Assert.assertEquals("Expecting 40 entries in the result list", 40, resultList.size());

                List<String> sql = SQLListener.getAndClearSQLList();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
                if (isDerby || isDB2) {
                    String expected = "SELECT 1 FROM SIMPLEENTITYOLGH8294 WHERE ? LIKE ? ESCAPE '_'";
                    Assert.assertEquals(expected, sql.get(0));
                } // TODO: other databases

                em.clear();
            }

            // Test 2
            // Test the first operand of a LIKE predicate (the match-expression) is untyped parameter
            // when at least one other operand (the pattern-expression or escape-expression)
            // is not an untyped parameter marker
            {
                System.out.println("testLIKE_ForceBindJPQLParameters Test #002");
                SQLListener.getAndClearSQLList();
                String queryStr = "SELECT 1 FROM SimpleEntityOLGH8294 s " +
                                  "WHERE s.itemString1 LIKE ?2";
                Query query = em.createQuery(queryStr);
                query.setParameter(2, "HELLO%");

                final List<?> resultList = query.getResultList();
                Assert.assertNotNull(resultList);
                System.out.println("resultList size = " + resultList.size());
                Assert.assertEquals("Expecting 0 entries in the result list", 0, resultList.size());

                List<String> sql = SQLListener.getAndClearSQLList();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
                if (isDerby || isDB2) {
                    String expected = "SELECT 1 FROM SIMPLEENTITYOLGH8294 WHERE ITEM_STRING1 LIKE ?";
                    Assert.assertEquals(expected, sql.get(0));
                } // TODO: other databases

                em.clear();
            }
        } catch (java.lang.AssertionError /* | org.junit.internal.AssumptionViolatedException */ ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testSUBSTR_ForceBindJPQLParameters(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DERBY);
        final boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2);

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // Test 1
            // Test untyped parameter is first argument of SUBSTRING
            {
                System.out.println("testSUBSTR_ForceBindJPQLParameters Test #001");
                SQLListener.getAndClearSQLList();
                String queryStr = "SELECT 1 FROM SimpleEntityOLGH8294 s " +
                                  "WHERE TRIM(s.itemString1) = TRIM(SUBSTRING(?1, 1, 5))";
                Query query = em.createQuery(queryStr);
                query.setParameter(1, "HELLO WORLD");

                final List<?> resultList = query.getResultList();
                Assert.assertNotNull(resultList);
                System.out.println("resultList size = " + resultList.size());
                Assert.assertEquals("Expecting 0 entries in the result list", 0, resultList.size());

                List<String> sql = SQLListener.getAndClearSQLList();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
                if (isDerby || isDB2) {
                    String expected = "SELECT 1 FROM SIMPLEENTITYOLGH8294 WHERE (TRIM(ITEM_STRING1) = TRIM(SUBSTR(?, 1, 5)))";
                    Assert.assertEquals(expected, sql.get(0));
                } // TODO: other databases

                em.clear();
            }

            // Test 2
            // Test untyped parameter is first & second argument of SUBSTRING
            {
                System.out.println("testSUBSTR_ForceBindJPQLParameters Test #002");
                SQLListener.getAndClearSQLList();
                String queryStr = "SELECT 1 FROM SimpleEntityOLGH8294 s " +
                                  "WHERE TRIM(s.itemString1) = TRIM(SUBSTRING(?1, ?2, 5))";
                Query query = em.createQuery(queryStr);
                query.setParameter(1, "HELLO WORLD");
                query.setParameter(2, 1);

                final List<?> resultList = query.getResultList();
                Assert.assertNotNull(resultList);
                System.out.println("resultList size = " + resultList.size());
                Assert.assertEquals("Expecting 0 entries in the result list", 0, resultList.size());

                List<String> sql = SQLListener.getAndClearSQLList();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
                if (isDerby || isDB2) {
                    String expected = "SELECT 1 FROM SIMPLEENTITYOLGH8294 WHERE (TRIM(ITEM_STRING1) = TRIM(SUBSTR(?, ?, 5)))";
                    Assert.assertEquals(expected, sql.get(0));
                } // TODO: other databases

                em.clear();
            }

            // Test 3
            // Test untyped parameter is all arguments of SUBSTRING
            {
                System.out.println("testSUBSTR_ForceBindJPQLParameters Test #003");
                SQLListener.getAndClearSQLList();
                String queryStr = "SELECT 1 FROM SimpleEntityOLGH8294 s " +
                                  "WHERE s.itemString1 = SUBSTRING(?1, ?2, ?3)";
                Query query = em.createQuery(queryStr);
                query.setParameter(1, "HELLO WORLD");
                query.setParameter(2, 1);
                query.setParameter(3, 5);

                final List<?> resultList = query.getResultList();
                Assert.assertNotNull(resultList);
                System.out.println("resultList size = " + resultList.size());
                Assert.assertEquals("Expecting 0 entries in the result list", 0, resultList.size());

                List<String> sql = SQLListener.getAndClearSQLList();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
                if (isDerby || isDB2) {
                    String expected = "SELECT 1 FROM SIMPLEENTITYOLGH8294 WHERE (ITEM_STRING1 = SUBSTR(?, ?, ?))";
                    Assert.assertEquals(expected, sql.get(0));
                } // TODO: other databases

                em.clear();
            }

            // Test 4
            // Test SUBSTRING function with IN expression
            {
                System.out.println("testSUBSTR_ForceBindJPQLParameters Test #004");
                SQLListener.getAndClearSQLList();
                String queryStr = "SELECT 1 FROM SimpleEntityOLGH8294 s " +
                                  "WHERE SUBSTRING(s.itemString1, 1, ?1) NOT IN (?2, ?3, ?4, ?5)";
                Query query = em.createQuery(queryStr);
                query.setParameter(1, 5);
                query.setParameter(2, "TEST1");
                query.setParameter(3, "TEST2");
                query.setParameter(4, "HELLO");
                query.setParameter(5, "TEST3");

                final List<?> resultList = query.getResultList();
                Assert.assertNotNull(resultList);
                System.out.println("resultList size = " + resultList.size());
                Assert.assertEquals("Expecting 40 entries in the result list", 40, resultList.size());

                List<String> sql = SQLListener.getAndClearSQLList();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
                if (isDerby || isDB2) {
                    String expected = "SELECT 1 FROM SIMPLEENTITYOLGH8294 WHERE (SUBSTR(ITEM_STRING1, 1, ?) NOT IN (?, ?, ?, ?))";
                    Assert.assertEquals(expected, sql.get(0));
                } // TODO: other databases

                em.clear();
            }
        } catch (java.lang.AssertionError /* | org.junit.internal.AssumptionViolatedException */ ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }
}
