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

package com.ibm.ws.jpa.olgh10068.testlogic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh10068.model.SimpleEntityOLGH10068;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH10068Logic extends AbstractTestLogic {

    public void testCriteriaBuilder_IN_ClauseLimit(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            try {
                final CriteriaBuilder builder = em.getCriteriaBuilder();
                final CriteriaQuery<SimpleEntityOLGH10068> query = builder.createQuery(SimpleEntityOLGH10068.class);
                Root<SimpleEntityOLGH10068> root = query.from(SimpleEntityOLGH10068.class);
                query.where(root.get("content").in(builder.parameter(List.class, "parameterList")));

                Query q = em.createQuery(query);

                Assert.assertEquals(org.eclipse.persistence.internal.jpa.EntityManagerImpl.class.getCanonicalName(),
                                    em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class).getClass().getCanonicalName());

                org.eclipse.persistence.jpa.JpaEntityManager eclEm = em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class);
                org.eclipse.persistence.platform.database.DatabasePlatform platform = eclEm.getDatabaseSession().getPlatform();

                //Create a list longer than the limit
                int limit = platform.getINClauseLimit() + 10;
                List<String> parameterList = new ArrayList<String>();
                for (int p = 0; p < limit; p++) {
                    parameterList.add("" + p);
                }
                q.setParameter("parameterList", parameterList);

                q.getResultList();
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

    public void testCriteriaBuilder_NOTIN_ClauseLimit(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            try {
                final CriteriaBuilder builder = em.getCriteriaBuilder();
                final CriteriaQuery<SimpleEntityOLGH10068> query = builder.createQuery(SimpleEntityOLGH10068.class);
                Root<SimpleEntityOLGH10068> root = query.from(SimpleEntityOLGH10068.class);
                query.where(root.get("content").in(builder.parameter(List.class, "parameterList")).not());

                Query q = em.createQuery(query);

                Assert.assertEquals(org.eclipse.persistence.internal.jpa.EntityManagerImpl.class.getCanonicalName(),
                                    em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class).getClass().getCanonicalName());

                org.eclipse.persistence.jpa.JpaEntityManager eclEm = em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class);
                org.eclipse.persistence.platform.database.DatabasePlatform platform = eclEm.getDatabaseSession().getPlatform();

                //Create a list longer than the limit
                int limit = platform.getINClauseLimit() + 10;
                List<String> parameterList = new ArrayList<String>();
                for (int p = 0; p < limit; p++) {
                    parameterList.add("" + p);
                }
                q.setParameter("parameterList", parameterList);

                q.getResultList();
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

    public void testJPQL_IN_ClauseLimit(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            try {
                final CriteriaBuilder builder = em.getCriteriaBuilder();
                final CriteriaQuery<SimpleEntityOLGH10068> query = builder.createQuery(SimpleEntityOLGH10068.class);
                Root<SimpleEntityOLGH10068> root = query.from(SimpleEntityOLGH10068.class);
                query.where(root.get("content").in(builder.parameter(List.class, "parameterList")).not());

                Query q = em.createQuery("select t0.id from SimpleEntityOLGH10068 t0 "
                                         + "where t0.id <> :parameterLong and t0.content in :parameterList");

                Assert.assertEquals(org.eclipse.persistence.internal.jpa.EntityManagerImpl.class.getCanonicalName(),
                                    em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class).getClass().getCanonicalName());

                org.eclipse.persistence.jpa.JpaEntityManager eclEm = em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class);
                org.eclipse.persistence.platform.database.DatabasePlatform platform = eclEm.getDatabaseSession().getPlatform();

                //Create a list longer than the limit
                int limit = platform.getINClauseLimit() + 10;
                List<String> parameterList = new ArrayList<String>();
                for (int p = 0; p < limit; p++) {
                    parameterList.add("" + p);
                }
                q.setParameter("parameterList", parameterList);
                q.setParameter("parameterLong", 1L);

                q.getResultList();
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

    public void testJPQL_NOTIN_ClauseLimit(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            try {
                final CriteriaBuilder builder = em.getCriteriaBuilder();
                final CriteriaQuery<SimpleEntityOLGH10068> query = builder.createQuery(SimpleEntityOLGH10068.class);
                Root<SimpleEntityOLGH10068> root = query.from(SimpleEntityOLGH10068.class);
                query.where(root.get("content").in(builder.parameter(List.class, "parameterList")).not());

                Query q = em.createQuery("select t0.id from SimpleEntityOLGH10068 t0 "
                                         + "where t0.id <> :parameterLong and t0.content not in :parameterList");

                Assert.assertEquals(org.eclipse.persistence.internal.jpa.EntityManagerImpl.class.getCanonicalName(),
                                    em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class).getClass().getCanonicalName());

                org.eclipse.persistence.jpa.JpaEntityManager eclEm = em.unwrap(org.eclipse.persistence.jpa.JpaEntityManager.class);
                org.eclipse.persistence.platform.database.DatabasePlatform platform = eclEm.getDatabaseSession().getPlatform();

                //Create a list longer than the limit
                int limit = platform.getINClauseLimit() + 10;
                List<String> parameterList = new ArrayList<String>();
                for (int p = 0; p < limit; p++) {
                    parameterList.add("" + p);
                }
                q.setParameter("parameterList", parameterList);
                q.setParameter("parameterLong", 1L);

                q.getResultList();
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
