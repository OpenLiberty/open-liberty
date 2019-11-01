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

package com.ibm.ws.jpa.olgh9035.testlogic;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.EntityManager;

import org.junit.Assert;

import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH9035Logic extends AbstractTestLogic {

    public void testPlatformDetection_DB2ZOS(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        //Only run this test on DB2ZOS
        if (!isDB2ForZOS(dbProductName)) {
            return;
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            try {
                //Check that we are using EclipseLink
                Assert.assertEquals(org.eclipse.persistence.internal.jpa.EntityManagerImpl.class.getCanonicalName(),
                                    em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class).getClass().getCanonicalName());

                org.eclipse.persistence.jpa.JpaEntityManager eclEm = em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class);
                org.eclipse.persistence.internal.databaseaccess.Platform platform = eclEm.getDatabaseSession().getDatasourcePlatform();

                //Test that EclipseLink detected DB2ZPlatform
                Assert.assertEquals(org.eclipse.persistence.platform.database.DB2ZPlatform.class.getName(), platform.getClass().getName());
            } finally {
                if (tj.isTransactionActive()) {
                    tj.rollbackTransaction();
                }
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

    public void testPlatformDetection_DB2I(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        //Only run this test on DB2-i series
        if (!isDB2ForISeries(dbProductName)) {
            return;
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            try {
                //Check that we are using EclipseLink
                Assert.assertEquals(org.eclipse.persistence.internal.jpa.EntityManagerImpl.class.getCanonicalName(),
                                    em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class).getClass().getCanonicalName());

                org.eclipse.persistence.jpa.JpaEntityManager eclEm = em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class);
                org.eclipse.persistence.internal.databaseaccess.Platform platform = eclEm.getDatabaseSession().getDatasourcePlatform();

                //Test that EclipseLink detected DB2MainframePlatform
                Assert.assertEquals(org.eclipse.persistence.platform.database.DB2MainframePlatform.class.getName(), platform.getClass().getName());
            } finally {
                if (tj.isTransactionActive()) {
                    tj.rollbackTransaction();
                }
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

    public void testPlatformDetection_MySQL(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        //Only run this test on MySQL
        if (!isMySQL(dbProductName)) {
            return;
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            try {
                //Check that we are using EclipseLink
                Assert.assertEquals(org.eclipse.persistence.internal.jpa.EntityManagerImpl.class.getCanonicalName(),
                                    em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class).getClass().getCanonicalName());

                org.eclipse.persistence.jpa.JpaEntityManager eclEm = em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class);
                org.eclipse.persistence.internal.databaseaccess.Platform platform = eclEm.getDatabaseSession().getDatasourcePlatform();

                //Test that EclipseLink detected MySQLPlatform
                Assert.assertEquals(org.eclipse.persistence.platform.database.MySQLPlatform.class.getName(), platform.getClass().getName());
            } finally {
                if (tj.isTransactionActive()) {
                    tj.rollbackTransaction();
                }
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

    public void testPlatformDetection_Derby(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        //Only run this test on Derby
        if (!isDerby(dbProductName)) {
            return;
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            try {
                //Check that we are using EclipseLink
                Assert.assertEquals(org.eclipse.persistence.internal.jpa.EntityManagerImpl.class.getCanonicalName(),
                                    em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class).getClass().getCanonicalName());

                org.eclipse.persistence.jpa.JpaEntityManager eclEm = em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class);
                org.eclipse.persistence.internal.databaseaccess.Platform platform = eclEm.getDatabaseSession().getDatasourcePlatform();

                //Test that EclipseLink detected JavaDBPlatform
                Assert.assertEquals(org.eclipse.persistence.platform.database.JavaDBPlatform.class.getName(), platform.getClass().getName());
            } finally {
                if (tj.isTransactionActive()) {
                    tj.rollbackTransaction();
                }
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

    public void testPlatformDetection_Oracle(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        //Only run this test on Oracle
        if (!isOracle(dbProductName)) {
            return;
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            try {
                //Check that we are using EclipseLink
                Assert.assertEquals(org.eclipse.persistence.internal.jpa.EntityManagerImpl.class.getCanonicalName(),
                                    em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class).getClass().getCanonicalName());

                org.eclipse.persistence.jpa.JpaEntityManager eclEm = em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class);
                org.eclipse.persistence.internal.databaseaccess.Platform platform = eclEm.getDatabaseSession().getDatasourcePlatform();

                //Test that EclipseLink detected the correct OracleXPlatform
                //Note: At this time, we are not using the Oracle extension bundle, so this test should only
                // check that we are detecting the core platform packages
                Assert.assertEquals("org.eclipse.persistence.platform.database.Oracle" + dbProductVersion + "Platform", platform.getClass().getName());
            } finally {
                if (tj.isTransactionActive()) {
                    tj.rollbackTransaction();
                }
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

    public void testPlatformDetection_Hana(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        //Only run this test on Hana
        if (!isHana(dbProductName)) {
            return;
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            try {
                //Check that we are using EclipseLink
                Assert.assertEquals(org.eclipse.persistence.internal.jpa.EntityManagerImpl.class.getCanonicalName(),
                                    em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class).getClass().getCanonicalName());

                org.eclipse.persistence.jpa.JpaEntityManager eclEm = em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class);
                org.eclipse.persistence.internal.databaseaccess.Platform platform = eclEm.getDatabaseSession().getDatasourcePlatform();

                //Test that EclipseLink detected the HANAPlatform
                Assert.assertEquals(org.eclipse.persistence.platform.database.HANAPlatform.class.getName(), platform.getClass().getName());
            } finally {
                if (tj.isTransactionActive()) {
                    tj.rollbackTransaction();
                }
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
