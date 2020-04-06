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

package com.ibm.ws.jpa.olgh9018.testlogic;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Map;

import javax.persistence.EntityManager;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh9018.model.SimpleEntityOLGH9018;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

/**
 * The JPA Specification is very loose with respect to unwrap:
 * "Return an object of the specified type to allow access to the * provider-specific API.
 * If the provider's EntityManager implementation does not support the specified class, the
 * PersistenceException is thrown."
 *
 */
public class JPATestOLGH9018Logic extends AbstractTestLogic {

    public void testUnwrapConnectionNoTrans(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();

            Connection c = em.unwrap(Connection.class);
            if (getJPAProviderImpl(jpaResource).equals(JPAProviderImpl.OPENJPA)) {
                //OpenJPA will return a Connection even when outside of a transaction boundary
                Assert.assertNotNull(c);
            } else {
                //EclipseLink will return null when outside of a transaction boundary
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

    public void testUnwrapConnectionInTrans(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final boolean isAMJTA = PersistenceContextType.APPLICATION_MANAGED_JTA == jpaResource.getPcCtxInfo().getPcType();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            // em.unwrap(Connection.class) is expected to return a Connection when inside a tx boundary
            tj.beginTransaction();
            if (isAMJTA) {
                em.joinTransaction();
            }
            Connection c = em.unwrap(Connection.class);
            tj.rollbackTransaction();
            Assert.assertNotNull(c);

            // Assert that it is no longer available when tx is done.
            c = em.unwrap(Connection.class);
            if (getJPAProviderImpl(jpaResource).equals(JPAProviderImpl.OPENJPA)) {
                //OpenJPA will return a Connection even when outside of a transaction boundary
                Assert.assertNotNull(c);
            } else {
                //EclipseLink will return null when outside of a transaction boundary
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

    public void testUnwrapConnectionInTransWithOp(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final boolean isAMJTA = PersistenceContextType.APPLICATION_MANAGED_JTA == jpaResource.getPcCtxInfo().getPcType();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            // em.unwrap(Connection.class) is expected to return a Connection when inside a tx boundary
            // Test with running find() before unwrap() to verify that doesn't change behavior
            tj.beginTransaction();
            if (isAMJTA) {
                em.joinTransaction();
            }
            em.find(SimpleEntityOLGH9018.class, -1);
            Connection c = em.unwrap(Connection.class);
            tj.rollbackTransaction();
            Assert.assertNotNull(c);

            // Assert that it is no longer available when tx is done.
            c = em.unwrap(Connection.class);
            if (getJPAProviderImpl(jpaResource).equals(JPAProviderImpl.OPENJPA)) {
                //OpenJPA will return a Connection even when outside of a transaction boundary
                Assert.assertNotNull(c);
            } else {
                //EclipseLink will return null when outside of a transaction boundary
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
}
