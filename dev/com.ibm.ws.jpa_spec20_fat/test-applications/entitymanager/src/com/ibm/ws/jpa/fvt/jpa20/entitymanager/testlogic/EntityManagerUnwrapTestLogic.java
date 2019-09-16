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

package com.ibm.ws.jpa.fvt.jpa20.entitymanager.testlogic;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Map;

import javax.persistence.EntityManager;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.jpa20.entitymanager.model.Employee;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

/**
 *
 */
public class EntityManagerUnwrapTestLogic extends AbstractTestLogic {
    private boolean isSupportedDatabase(String dbProductName, String dbProductVersion, String jdbcDriverVersion) {
        return true;
    }

    /**
     * The JPA Specification is very loose with respect to unwrap:
     * "Return an object of the specified type to allow access to the * provider-specific API.
     * If the provider's EntityManager implementation does not support the specified class, the
     * PersistenceException is thrown."
     *
     * This means this test case is vendor specific.
     *
     */
    public void testUnwrapConnection(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        if (isSupportedDatabase(dbProductName, dbProductVersion, jdbcDriverVersion) == false) {
            // This test does not currently support the target database vendor.
            return;
        }

        final String lDbProductName = dbProductName.toLowerCase();
        final boolean isAMJTA = PersistenceContextType.APPLICATION_MANAGED_JTA == jpaResource.getPcCtxInfo().getPcType();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            final JPAProviderImpl jpaPvdr = this.getJPAProviderImpl(em);
            Connection c = null;
            if (JPAProviderImpl.ECLIPSELINK == jpaPvdr) {
                // em.unwrap(Connection.class) is expected to return null when outside of a tx boundary
                c = em.unwrap(Connection.class);
                Assert.assertNull(c);

                // em.unwrap(Connection.class) is expected to return a Connection when inside a tx boundary
                tj.beginTransaction();
                if (isAMJTA) {
                    em.joinTransaction();
                }
                c = em.unwrap(Connection.class);
                tj.rollbackTransaction();
                Assert.assertNotNull(c);

                // Assert that it is no longer available when tx is done.
                c = em.unwrap(Connection.class);
                Assert.assertNull(c);

                // em.unwrap(Connection.class) is expected to return a Connection when inside a tx boundary
                // Test with running find() before unwrap() to verify that doesn't change behavior
                tj.beginTransaction();
                if (isAMJTA) {
                    em.joinTransaction();
                }
                em.find(Employee.class, -1);
                c = em.unwrap(Connection.class);
                tj.rollbackTransaction();
                Assert.assertNotNull(c);

                // Assert that it is no longer available when tx is done.
                c = em.unwrap(Connection.class);
                Assert.assertNull(c);

            }

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
     * Eclipselink offers a capability, "eclipselink.jdbc.exclusive-connection.mode", which was not functioning
     * correctly until fixed with Eclipselink bug Bug 547173. This test case verifies behavior in EE mode.
     *
     */
    public void testUnwrapExclusiveConnection(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        if (isSupportedDatabase(dbProductName, dbProductVersion, jdbcDriverVersion) == false) {
            // This test does not currently support the target database vendor.
            return;
        }

        // TODO: Skip this test until the Eclipselink 2.7 shipped with Open Liberty has been updated to include this fix
        // Eclipselink Bug 547173
        if (isUsingJPA22Feature()) {
            System.out.println("Detected that the active feature is JPA 2.2, skipping this test until fix is integrated.");
            return;
        }

        final String lDbProductName = dbProductName.toLowerCase();
        final boolean isAMJTA = PersistenceContextType.APPLICATION_MANAGED_JTA == jpaResource.getPcCtxInfo().getPcType();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            final JPAProviderImpl jpaPvdr = this.getJPAProviderImpl(em);
            Connection c = null;
            if (JPAProviderImpl.ECLIPSELINK == jpaPvdr) {
                // em.unwrap(Connection.class) is expected to return null when outside of a tx boundary
                c = em.unwrap(Connection.class);
                Assert.assertNull(c);

                // em.unwrap(Connection.class) is expected to return a Connection when inside a tx boundary
                // Before the fix, this would not work (would return null)
                tj.beginTransaction();
                if (isAMJTA) {
                    em.joinTransaction();
                }
                c = em.unwrap(Connection.class);
                tj.rollbackTransaction();
                Assert.assertNotNull(c);

                // Verify that the Connection is still available
                c = em.unwrap(Connection.class);
                Assert.assertNotNull(c);

                // em.unwrap(Connection.class) is expected to return a Connection when inside a tx boundary
                // Test with running find() before unwrap() to verify that doesn't change behavior
                // Before the fix, poking the connection with an operation was the only way to get unwrap() to work right.
                tj.beginTransaction();
                if (isAMJTA) {
                    em.joinTransaction();
                }
                em.find(Employee.class, -1);
                c = em.unwrap(Connection.class);
                tj.rollbackTransaction();
                Assert.assertNotNull(c);

            }

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void template(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        if (isSupportedDatabase(dbProductName, dbProductVersion, jdbcDriverVersion) == false) {
            // This test does not currently support the target database vendor.
            return;
        }

        final String lDbProductName = dbProductName.toLowerCase();
        final boolean isAMJTA = PersistenceContextType.APPLICATION_MANAGED_JTA == jpaResource.getPcCtxInfo().getPcType();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }
}
