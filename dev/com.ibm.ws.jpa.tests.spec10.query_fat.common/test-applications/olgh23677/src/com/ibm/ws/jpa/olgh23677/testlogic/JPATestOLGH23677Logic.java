/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

package com.ibm.ws.jpa.olgh23677.testlogic;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh23677.model.SimpleEntityOLGH23677;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH23677Logic extends AbstractTestLogic {

    /**
     * Test bulk update queries do not reuse the same timestamp value across multiple executions
     */
    public void testNonBreakingSpace(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            TransactionJacket tj = jpaResource.getTj();

            System.out.println("Clearing persistence context...");
            em.clear();

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // test 1
            try {
                String queryString = "SELECT n FROM\u00A0SimpleEntityOLGH23677 n";
                TypedQuery<SimpleEntityOLGH23677> checkQuery = em.createQuery(queryString, SimpleEntityOLGH23677.class);

                List<SimpleEntityOLGH23677> checkResult = checkQuery.getResultList();
                Assert.assertEquals("Entity table SimpleEntityOLGH23677 must be empty", 0, checkResult.size());
            } finally {
                // Rollback the update
                System.out.println("Rolling back transaction...");
                if (tj.isTransactionActive()) {
                    tj.rollbackTransaction();
                }
            }

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            tj.beginTransaction();
            if (tj.isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // test 2
            try {
                String queryString = "SELECT n FROM SimpleEntityOLGH23677 n WHERE n.pk =\u00A0:pkparam";
                TypedQuery<SimpleEntityOLGH23677> checkQuery = em.createQuery(queryString, SimpleEntityOLGH23677.class);
                checkQuery.setParameter("pkparam", "test2");

                List<SimpleEntityOLGH23677> checkResult = checkQuery.getResultList();
                Assert.assertEquals("Entity table SimpleEntityOLGH23677 must be empty", 0, checkResult.size());
            } finally {
                // Rollback the update
                System.out.println("Rolling back transaction...");
                if (tj.isTransactionActive()) {
                    tj.rollbackTransaction();
                }
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
