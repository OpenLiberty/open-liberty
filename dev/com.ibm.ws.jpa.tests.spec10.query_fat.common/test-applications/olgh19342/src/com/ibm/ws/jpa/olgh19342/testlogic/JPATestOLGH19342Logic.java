/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

package com.ibm.ws.jpa.olgh19342.testlogic;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.Assert;

import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH19342Logic extends AbstractTestLogic {

    public void testCaseExpressionOperatorConcurrency(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        JPAPersistenceProvider provider = JPAPersistenceProvider.resolveJPAPersistenceProvider(jpaResource);

        //TODO: Hibernate expects Boolean type instead of Integer type
        if (JPAPersistenceProvider.HIBERNATE.equals(provider)
            && (isUsingJPA21ContainerFeature(true) || isUsingJPA22ContainerFeature(true))) {
            return;
        }

        // Execute Test Case
        try {
            final AtomicInteger count = new AtomicInteger();
            final AtomicInteger error = new AtomicInteger();

            final int threads = 100;
            final ExecutorService taskExecutor = Executors.newFixedThreadPool(threads);

            // Spawn 100 threads
            for (int i = 0; i < threads; i++) {
                taskExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        count.incrementAndGet();

                        EntityManager em = jpaResource.getEmf().createEntityManager();
                        try {
                            Query query = null;
                            // EclipseLink converts the result to an Integer value
                            if (JPAPersistenceProvider.ECLIPSELINK.equals(provider)) {
                                query = em.createNamedQuery("CONCURR_CASE_QUERY", Integer.class);
                            } else {
                                query = em.createNamedQuery("CONCURR_CASE_QUERY", Boolean.class);
                            }
                            query.setParameter("id", "Key30");
                            query.getSingleResult();
                        } catch (Exception e) {
                            // print the stack for the first exception
                            if (error.incrementAndGet() == 1) {
                                e.printStackTrace();
                            }
                        } finally {
                            if (em != null) {
                                if (em.getTransaction().isActive()) {
                                    em.getTransaction().rollback();;
                                }
                                em.close();
                            }
                        }
                    }
                });
            }
            taskExecutor.shutdown();
            taskExecutor.awaitTermination(5, TimeUnit.SECONDS);

            Assert.assertEquals("Expected no failures, but " + error.intValue() + "/" + count.intValue() + " threads failed", 0, error.intValue());
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
