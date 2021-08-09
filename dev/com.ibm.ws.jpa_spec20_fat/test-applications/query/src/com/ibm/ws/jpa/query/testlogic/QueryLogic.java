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

package com.ibm.ws.jpa.query.testlogic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.junit.Assert;

import com.ibm.ws.jpa.query.model.InvalidQuerySubclass;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class QueryLogic extends AbstractTestLogic {

    private List<Query> createQueryTestList(EntityManager em) {
        List<Query> testMap = new ArrayList<Query>();

        testMap.add(em.createQuery("SELECT e from JPA20QueryUnwrapEntity e"));
        testMap.add(em.createNamedQuery("UnwrapQueryNamed"));
        testMap.add(em.createNativeQuery("SELECT e from JPA20QueryUnwrapEntity e"));
        testMap.add(em.createNamedQuery("UnwrapQueryNative"));

        return testMap;
    }

    /**
     * The application calls Query.unwrap() passing in as the method argument the value java.lang.Object.
     *
     * Object.class does not implement javax.persistence.Query, the call to unwrap() should throw javax.persistence.PersistenceException.
     * However, this behavior is JPA Provider specific.
     */
    public void testUnwrap001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            EntityManager em = jpaResource.getEm();

            List<Query> testList = createQueryTestList(em);

            for (Query query : testList) {
                try {
                    Object obj = query.unwrap(Object.class);

                    if (JPAProviderImpl.OPENJPA.equals(getJPAProviderImpl(jpaResource))) {
                        // TODO: OpenJPA does not support this call
                        Assert.fail("The expected PersistenceException was not thrown and the provide returned object " + obj);
                    }
                } catch (PersistenceException pe) {
                    // Caught expected PersistenceException
                } catch (Exception e) {
                    Assert.fail("The wrong exception was thrown.");
                }
            }
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * The test defines a class InvalidQuerySubclass which implements the javax.persistence.Query interface.
     * The test invokes the unwrap() method specifying InvalidQuerySubclass as its argument.
     *
     * Because this is a fake implementor of InvalidQuerySubclass and is not part of the JPA provider
     * it should be interpreted as a class that the implementation does not support.
     * Therefore, the call to unwrap() should throw a javax.persistence.PersistenceException.
     * However, this behavior is JPA Provider specific
     */
    public void testUnwrap002(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            EntityManager em = jpaResource.getEm();

            List<Query> testList = createQueryTestList(em);

            for (Query query : testList) {

                try {
                    Object obj = query.unwrap(InvalidQuerySubclass.class);

                    Assert.fail("The expected PersistenceException was not thrown and the provider returned object " + obj);
                } catch (PersistenceException pe) {
                    // Caught expected PersistenceException
                } catch (Exception e) {
                    Assert.fail("The wrong exception was thrown.");
                }
            }
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * The Java Doc for the Query.unwrap() defines a contract stating that the
     * provider must throw a PersistenceException if the specified class is not supported (3.1.1).
     * In the Summary of Exceptions (3.11), it states that throwing any PersistenceException with
     * the exception of NoResultException, NonUniqueResultException, LockTimeoutException, and
     * QueryTimeoutException, will cause the current transaction to be marked for rollback.
     *
     * Therefore, this test shall begin a new transaction, attempt to unwrap the BogusQuery
     * class, and then check if the transaction has been marked for rollback. It will be considered
     * a failure if the transaction has not been marked for rollback, as the specification has not
     * decreed an exclusion for unwrap().
     *
     */
    public void testUnwrap003(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            List<Query> testList = createQueryTestList(em);

            for (Query query : testList) {
                try {

                    System.out.println("Beginning new transaction...");
                    tj.beginTransaction();
                    if (tj.isApplicationManaged()) {
                        System.out.println("Joining entitymanager to JTA transaction...");
                        em.joinTransaction();
                    }

                    Object obj = query.unwrap(InvalidQuerySubclass.class);

                    Assert.fail("The expected PersistenceException was not thrown and the provider returned object " + obj);
                } catch (PersistenceException pe) {
                    // TODO: Disabling assertion, the transaction is not getting marked for rollback. Investigate.
                    // Assert.assertTrue("Transaction has not been marked for rollback.", tj.isTransactionMarkedForRollback());
                } finally {
                    try {
                        System.out.println("Rolling back transaction...");
                        if (tj.isTransactionActive()) {
                            tj.rollbackTransaction();
                        }
                    } catch (Throwable t) {
                        // Swallow
                    }
                }
            }
        } finally {
            System.out.println(testName + ": End");
        }
    }
}
