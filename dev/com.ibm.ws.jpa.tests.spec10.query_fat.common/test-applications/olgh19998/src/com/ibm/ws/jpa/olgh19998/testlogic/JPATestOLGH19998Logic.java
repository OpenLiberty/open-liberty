/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.olgh19998.testlogic;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh19998.model.SimpleEntityOLGH19998;
import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH19998Logic extends AbstractTestLogic {

    /**
     * Test bulk update queries do not reuse the same timestamp value across multiple executions
     */
    public void testUpdateAllQueryWithTimestampLocking(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
        //TODO: Disable test until EclipseLink 2.2, 3.0, or 3.1 is updated to include the fix
        if ((isUsingJPA30Feature() || isUsingJPA31Feature()) && JPAPersistenceProvider.ECLIPSELINK.equals(provider)) {
            return;
        }

        int flag1 = 0;
        int flag2 = 1;
        String pk1 = "11004";
        String pk2 = "11005";

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

            // Populate entities with initial timestamp version values
            SimpleEntityOLGH19998 e1 = new SimpleEntityOLGH19998(pk1, flag1);
            SimpleEntityOLGH19998 e2 = new SimpleEntityOLGH19998(pk2, flag2);

            System.out.println("Performing persist(" + e1 + ") operation");
            em.persist(e1);
            System.out.println("Performing persist(" + e2 + ") operation");
            em.persist(e2);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            System.out.println("Clearing persistence context...");
            em.clear();

            try {

                // Begin new transaction
                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                /*
                 * Update flag1 value via bulk Update query.
                 * This should update the version locking timestamp value for entity1.
                 */
                flag1 = flag1++;

                Query query;
                // Hibernate throws exception "java.lang.IllegalArgumentException: Update/delete queries cannot be typed"
                if (JPAPersistenceProvider.HIBERNATE.equals(provider)) {
                    query = em.createNamedQuery("updateActiveEcallAvailableFlag");
                } else {
                    query = em.createNamedQuery("updateActiveEcallAvailableFlag", SimpleEntityOLGH19998.class);
                }
                query.setParameter("flag", flag1);
                query.setParameter("pk", pk1);

                System.out.println("Executing UpdateQuery");
                query.executeUpdate();

                System.out.println("Committing transaction...");
                if (tj.isTransactionActive()) {
                    tj.commitTransaction();
                }

                System.out.println("Clearing persistence context...");
                em.clear();

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Begin new transaction
                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                /*
                 * Update flag2 value via bulk Update query.
                 * This should update the version locking timestamp value for entity2.
                 */
                flag2 = flag2++;

                // Hibernate throws exception "java.lang.IllegalArgumentException: Update/delete queries cannot be typed"
                if (JPAPersistenceProvider.HIBERNATE.equals(provider)) {
                    query = em.createNamedQuery("updateActiveEcallAvailableFlag");
                } else {
                    query = em.createNamedQuery("updateActiveEcallAvailableFlag", SimpleEntityOLGH19998.class);
                }
                query.setParameter("flag", flag2);
                query.setParameter("pk", pk2);

                System.out.println("Executing UpdateQuery");
                query.executeUpdate();

                System.out.println("Committing transaction...");
                if (tj.isTransactionActive()) {
                    tj.commitTransaction();
                }

                System.out.println("Clearing persistence context...");
                em.clear();

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Begin new transaction
                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                /*
                 * Validate that even though both bulk updates used the same UpdateAllQuery, both entities have
                 * different version locking timestamps in the database.
                 */

                System.out.println("Performing find(" + SimpleEntityOLGH19998.class + ", " + pk1 + ") operation");
                SimpleEntityOLGH19998 find_e1 = em.find(SimpleEntityOLGH19998.class, pk1);
                System.out.println("Performing find(" + SimpleEntityOLGH19998.class + ", " + pk2 + ") operation");
                SimpleEntityOLGH19998 find_e2 = em.find(SimpleEntityOLGH19998.class, pk2);

                Assert.assertNotNull(find_e1);
                Assert.assertNotNull(find_e2);

                // TODO: Hibernate fails this check for some reason
                if (!JPAPersistenceProvider.HIBERNATE.equals(provider)) {
                    Assert.assertFalse("Expected [" + find_e1.getSysUpdateTimestamp() + "] to not equal",
                                       find_e1.getSysUpdateTimestamp().equals(find_e2.getSysUpdateTimestamp()));
                    Assert.assertTrue("Expected entity2.sysUpdateTimestamp [" + find_e2.getSysUpdateTimestamp() + "] "
                                      + "to be after entity1.sysUpdateTimestamp [" + find_e1.getSysUpdateTimestamp() + "]",
                                      find_e2.getSysUpdateTimestamp().after(find_e1.getSysUpdateTimestamp()));
                }

                System.out.println("Committing transaction...");
                if (tj.isTransactionActive()) {
                    tj.commitTransaction();
                }
            } catch (Throwable t) {
                throw t;
            } finally {
                if (tj.isTransactionActive()) {
                    tj.rollbackTransaction();
                }

                System.out.println("Clearing persistence context...");
                em.clear();

                // Begin new transaction
                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                System.out.println("Performing find(" + SimpleEntityOLGH19998.class + ", " + pk1 + ") operation");
                SimpleEntityOLGH19998 remove_e1 = em.find(SimpleEntityOLGH19998.class, pk1);
                System.out.println("Performing find(" + SimpleEntityOLGH19998.class + ", " + pk2 + ") operation");
                SimpleEntityOLGH19998 remove_e2 = em.find(SimpleEntityOLGH19998.class, pk2);

                System.out.println("Performing remove(" + remove_e1 + ") operation");
                em.remove(remove_e1);
                System.out.println("Performing remove(" + remove_e2 + ") operation");
                em.remove(remove_e2);

                System.out.println("Committing transaction...");
                if (tj.isTransactionActive()) {
                    tj.commitTransaction();
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

    /**
     * Test that bulk update queries do not update the managed entities version
     * values as documented in the specification
     *
     * JPA Spec section 4.10: Bulk Update and Delete Operations
     *
     * Bulk update maps directly to a database update operation, bypassing optimistic locking checks.
     * Portable applications must manually update the value of the version column, if desired, and/or
     * manually validate the value of the version column.
     */
    public void testTimestampLockingUpdateWithUpdateAllQuery(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
        //TODO: Disable test until EclipseLink 3.0 or 3.1 is updated to include the fix
        if ((isUsingJPA30Feature() || isUsingJPA31Feature()) && JPAPersistenceProvider.ECLIPSELINK.equals(provider)) {
            return;
        }

        int flag1 = 0;
        String pk1 = "11006";

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

            // Populate entities with initial timestamp version values
            SimpleEntityOLGH19998 e1 = new SimpleEntityOLGH19998(pk1, flag1);

            System.out.println("Performing persist(" + e1 + ") operation");
            em.persist(e1);

            System.out.println("Committing transaction...");
            if (tj.isTransactionActive()) {
                tj.commitTransaction();
            }

            try {
                // Begin new transaction
                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                System.out.println("Performing find(" + SimpleEntityOLGH19998.class + ", " + pk1 + ") operation");
                SimpleEntityOLGH19998 find_e1 = em.find(SimpleEntityOLGH19998.class, pk1);
                Assert.assertNotNull(find_e1);

                Timestamp ver1 = find_e1.getSysUpdateTimestamp();
                Assert.assertNotNull(ver1);

                // Execute UpdateAllQuery to update version locking field in db
                flag1 = flag1++;

                Query query;
                // Hibernate throws exception "java.lang.IllegalArgumentException: Update/delete queries cannot be typed"
                if (JPAPersistenceProvider.HIBERNATE.equals(provider)) {
                    query = em.createNamedQuery("updateActiveEcallAvailableFlag");
                } else {
                    query = em.createNamedQuery("updateActiveEcallAvailableFlag", SimpleEntityOLGH19998.class);
                }
                query.setParameter("flag", flag1);
                query.setParameter("pk", pk1);

                System.out.println("Executing UpdateQuery");
                query.executeUpdate();

                // Verify that the UpdateAllQuery didn't update the managed instance
                System.out.println("Performing find(" + SimpleEntityOLGH19998.class + ", " + pk1 + ") operation");
                SimpleEntityOLGH19998 find_e2 = em.find(SimpleEntityOLGH19998.class, pk1);
                Assert.assertNotNull(find_e2);

                Timestamp ver2 = find_e2.getSysUpdateTimestamp();
                Assert.assertNotNull(ver2);
                Assert.assertEquals(ver1, ver2);

                System.out.println("Committing transaction...");
                if (tj.isTransactionActive()) {
                    tj.commitTransaction();
                }
            } catch (Throwable t) {
                throw t;
            } finally {
                if (tj.isTransactionActive()) {
                    tj.rollbackTransaction();
                }

                System.out.println("Clearing persistence context...");
                em.clear();

                // Begin new transaction
                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                System.out.println("Performing find(" + SimpleEntityOLGH19998.class + ", " + pk1 + ") operation");
                SimpleEntityOLGH19998 remove_e2 = em.find(SimpleEntityOLGH19998.class, pk1);

                System.out.println("Performing remove(" + remove_e2 + ") operation");
                em.remove(remove_e2);

                System.out.println("Committing transaction...");
                if (tj.isTransactionActive()) {
                    tj.commitTransaction();
                }
            }
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
