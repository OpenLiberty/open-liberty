/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.olgh10515.testlogic;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh10515.model.SimpleEntityOLGH10515;
import com.ibm.ws.jpa.olgh10515.model.SimpleEntityOLGH10515_;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH10515Logic extends AbstractTestLogic {

    public void testJoinOnCollectionTable(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            try {
                final CriteriaBuilder builder = em.getCriteriaBuilder();
                final CriteriaQuery<SimpleEntityOLGH10515> criteriaQuery = builder.createQuery(SimpleEntityOLGH10515.class);
                final Root<SimpleEntityOLGH10515> from = criteriaQuery.from(SimpleEntityOLGH10515.class);

                from.fetch(SimpleEntityOLGH10515_.origin);

                final TypedQuery<SimpleEntityOLGH10515> query = em.createQuery(criteriaQuery);

                int resultMax = 2;
                query.setMaxResults(resultMax);

                List<SimpleEntityOLGH10515> res = query.getResultList();
                Assert.assertEquals(resultMax, res.size());
            } finally {
                if (tj.isTransactionActive()) {
                    tj.rollbackTransaction();
                }
            }

            em = jpaResource.getEm();
            tj = jpaResource.getTj();

            try {
                final CriteriaBuilder builder = em.getCriteriaBuilder();
                final CriteriaQuery<SimpleEntityOLGH10515> criteriaQuery = builder.createQuery(SimpleEntityOLGH10515.class);
                final Root<SimpleEntityOLGH10515> from = criteriaQuery.from(SimpleEntityOLGH10515.class);

                from.fetch(SimpleEntityOLGH10515_.origin);

                final TypedQuery<SimpleEntityOLGH10515> query = em.createQuery(criteriaQuery);

                int resultMax = 2;
                query.setMaxResults(resultMax);

                List<SimpleEntityOLGH10515> res = query.getResultList();
                Assert.assertEquals(resultMax, res.size());
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
