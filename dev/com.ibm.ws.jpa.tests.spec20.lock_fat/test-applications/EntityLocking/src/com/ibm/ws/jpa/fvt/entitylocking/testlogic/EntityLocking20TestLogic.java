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

package com.ibm.ws.jpa.fvt.entitylocking.testlogic;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.LockModeType;
import javax.persistence.OptimisticLockException;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.entitylocking.entities.LockEntityA;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

/**
 *
 */
public class EntityLocking20TestLogic extends AbstractTestLogic {

    /**
     *
     *
     * @param testExecCtx
     * @param testExecResources
     * @param managedComponentObject
     */
    public void testReadLock001(
                                TestExecutionContext testExecCtx,
                                TestExecutionResources testExecResources,
                                Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        AbstractClientB cliBTL = null;
        // Execute Test Case
        try {
            System.out.println(testName + ": Begin");

            EntityManager em = jpaResource.getEm();
            em.clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding LockEntityA(id=1) with Optimistic Read locking...");
            LockEntityA entityA = jpaResource.getEm().find(LockEntityA.class, 1, LockModeType.READ);
            Assert.assertNotNull("Assert the find operation did not return null.", entityA);
            int firstVersion = entityA.getVersion();

            jpaResource.getTj().commitTransaction();

            // Verify that the version was not changed.
            em.clear();
            LockEntityA entityAFind = jpaResource.getEm().find(LockEntityA.class, 1);
            Assert.assertNotNull(entityAFind);;
            Assert.assertEquals(firstVersion, entityAFind.getVersion());
        } finally {
            if (jpaResource.getTj().isTransactionActive()) {
                try {
                    System.out.println("Rolling back post-test execution active transaction.");
                    jpaResource.getTj().rollbackTransaction();
                } catch (Throwable t) {
                }
            }
            System.out.println(testName + ": End");
        }

    }

    /**
     *
     *
     * @param testExecCtx
     * @param testExecResources
     * @param managedComponentObject
     */
    public void testReadLock001A(
                                 TestExecutionContext testExecCtx,
                                 TestExecutionResources testExecResources,
                                 Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        AbstractClientB cliBTL = null;
        // Execute Test Case
        try {
            System.out.println(testName + ": Begin");

            EntityManager em = jpaResource.getEm();
            em.clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding LockEntityA(id=1) with Optimistic Read locking...");
            LockEntityA entityA = jpaResource.getEm().find(LockEntityA.class, 1, LockModeType.OPTIMISTIC);
            Assert.assertNotNull("Assert the find operation did not return null.", entityA);
            int firstVersion = entityA.getVersion();

            jpaResource.getTj().commitTransaction();

            // Verify that the version was not changed.
            em.clear();
            LockEntityA entityAFind = jpaResource.getEm().find(LockEntityA.class, 1);
            Assert.assertNotNull(entityAFind);;
            Assert.assertEquals(firstVersion, entityAFind.getVersion());
        } finally {
            if (jpaResource.getTj().isTransactionActive()) {
                try {
                    System.out.println("Rolling back post-test execution active transaction.");
                    jpaResource.getTj().rollbackTransaction();
                } catch (Throwable t) {
                }
            }
            System.out.println(testName + ": End");
        }

    }

    /**
     * Verifies that the (optimistic) READ lock verifies that it checks the version during tx beforeCompletion.
     *
     * @param testExecCtx
     * @param testExecResources
     * @param managedComponentObject
     */
    public void testReadLock002(
                                TestExecutionContext testExecCtx,
                                TestExecutionResources testExecResources,
                                Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        JPAResource jpa2Resource = testExecResources.getJpaResourceMap().get("test-jpa-2-resource");
        if (jpa2Resource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-2-resource').  Cannot execute the test.");
            return;
        }

        AbstractClientB cliBTL = null;
        // Execute Test Case
        try {
            System.out.println(testName + ": Begin");

            EntityManager em = jpaResource.getEm();
            em.clear();

            // Create Client B
            Map<String, CountDownLatch> cdlMap = new HashMap<String, CountDownLatch>();
            cdlMap.put("clientb-start", new CountDownLatch(1));
            cdlMap.put("clientb-fetch-complete", new CountDownLatch(1));
            cdlMap.put("clientb-commit-complete", new CountDownLatch(1));
            cdlMap.put("clientb-complete", new CountDownLatch(1));
            cliBTL = new AbstractClientB(jpa2Resource, cdlMap) {
                @Override
                public void run() {
                    EntityManager em = null;

                    try {
                        em = jpa2Resource.getEmf().createEntityManager();
                        final EntityTransaction et = em.getTransaction();

                        System.out.println("Client-B on thread " + tId() + " waiting for start signal.");
                        waitForSignal("clientb-start");
                        System.out.println("Client-B on thread " + tId() + " received start signal.");
                        checkAbort();

                        System.out.println("Client-B on thread " + tId() + " beginning transaction ...");
                        et.begin();

                        System.out.println("Client-B on thread " + tId() + " Finding LockEntityA ...");
                        LockEntityA entityA = em.find(LockEntityA.class, 1);
                        Assert.assertNotNull("Assert the em.find() operation returned a LockEntityA.", entityA);

                        System.out.println("Client-B on thread " + tId() + " sending signal clientb-fetch-complete ...");
                        sendSignal("clientb-fetch-complete");
                        checkAbort();

                        System.out.println("Client-B on thread " + tId() + " mutating LockEntityA ...");
                        String entityAStrData = entityA.getStrData();
                        entityA.setStrData(entityAStrData + "-ClientBMutated");

                        checkAbort();
                        System.out.println("Client-B on thread " + tId() + " committing transaction ...");
                        et.commit();

                        System.out.println("Client-B on thread " + tId() + " sending signal clientb-commit-complete ...");
                        sendSignal("clientb-commit-complete");

                        System.out.println("Marking Client B as ending successfully.");
                        endedSuccessfully = true;
                        System.out.flush();
                    } catch (Throwable t) {
                        exception = t;
                        System.out.println("Client-B on thread " + tId() + " encountered Exception:");
                        t.printStackTrace();
                    } finally {
                        System.out.println("Client-B on thread " + tId() + " sending signal clientb-complete ...");
                        System.out.flush();
                        sendSignal("clientb-complete");

                        try {
                            if (em != null && em.getTransaction().isActive()) {
                                System.out.println("Client B on thread " + tId() + " Rolling Back Transaction in finally {}.");
                                em.getTransaction().rollback();
                            }
                            em.close();
                        } catch (Throwable t) {
                        }
                        System.out.println("Client B on thread " + tId() + " exiting.");
                        System.out.flush();
                    }
                }

            };
            final Thread cliBThread = new Thread(cliBTL);
            cliBThread.start();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding LockEntityA(id=1) with Optimistic Read locking...");
            LockEntityA entityA = jpaResource.getEm().find(LockEntityA.class, 1, LockModeType.READ);
            Assert.assertNotNull("Assert the find operation did not return null.", entityA);

            // Signal Client B to go.
            cliBTL.sendSignal("clientb-start");

            // Wait for Client B to signal that it successfully fetched (expected)
            cliBTL.waitForSignal("clientb-fetch-complete");
            System.out.println("Received Client-B Fetch Complete message.");

            // Wait for Client B to mutate entity and commit
            cliBTL.waitForSignal("clientb-commit-complete");
            System.out.println("Received Client-B Commit Complete message.");

            // Wait for Client B to complete
            cliBTL.waitForSignal("clientb-complete");
            System.out.println("Received Client-B Execution Complete message.");

            if (cliBTL.getException() != null) {
                // If Client B threw an exception, then rethrow it so it gets logged.
                throw cliBTL.getException();
            }
            Assert.assertTrue("Client B did not exit successfully.", cliBTL.getEndedSuccessfully());

            // Client B had updated the entity, which should have also incremented its version field
            // So since an Optimistic Read Lock was acquired on the entity, committing the transaction
            // should call for a version check during the pre-completion phase.
            try {
                System.out.println("Committing transaction (expecting OptimisticLockException) ...");
                jpaResource.getTj().commitTransaction();
                Assert.fail("No OptimisticLockException was thrown.");
            } catch (Throwable t) {
                // Check if OptimisticLockException is in the Exception Chain.
                assertExceptionIsInChain(OptimisticLockException.class, t);
            }
        } catch (java.lang.AssertionError ae) {
            abort(cliBTL);
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            abort(cliBTL);
            throw t;
        } finally {
            if (jpaResource.getTj().isTransactionActive()) {
                try {
                    System.out.println("Rolling back post-test execution active transaction.");
                    jpaResource.getTj().rollbackTransaction();
                } catch (Throwable t) {
                }
            }
            System.out.println(testName + ": End");
        }

    }

    /**
     * Verifies that the (optimistic) READ lock verifies that it checks the version during tx beforeCompletion.
     *
     * @param testExecCtx
     * @param testExecResources
     * @param managedComponentObject
     */
    public void testReadLock002A(
                                 TestExecutionContext testExecCtx,
                                 TestExecutionResources testExecResources,
                                 Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        JPAResource jpa2Resource = testExecResources.getJpaResourceMap().get("test-jpa-2-resource");
        if (jpa2Resource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-2-resource').  Cannot execute the test.");
            return;
        }

        AbstractClientB cliBTL = null;
        // Execute Test Case
        try {
            System.out.println(testName + ": Begin");

            EntityManager em = jpaResource.getEm();
            em.clear();

            // Create Client B
            Map<String, CountDownLatch> cdlMap = new HashMap<String, CountDownLatch>();
            cdlMap.put("clientb-start", new CountDownLatch(1));
            cdlMap.put("clientb-fetch-complete", new CountDownLatch(1));
            cdlMap.put("clientb-commit-complete", new CountDownLatch(1));
            cdlMap.put("clientb-complete", new CountDownLatch(1));
            cliBTL = new AbstractClientB(jpa2Resource, cdlMap) {
                @Override
                public void run() {
                    EntityManager em = null;

                    try {
                        em = jpa2Resource.getEmf().createEntityManager();
                        final EntityTransaction et = em.getTransaction();

                        System.out.println("Client-B on thread " + tId() + " waiting for start signal.");
                        waitForSignal("clientb-start");
                        System.out.println("Client-B on thread " + tId() + " received start signal.");
                        checkAbort();

                        System.out.println("Client-B on thread " + tId() + " beginning transaction ...");
                        et.begin();

                        System.out.println("Client-B on thread " + tId() + " Finding LockEntityA ...");
                        LockEntityA entityA = em.find(LockEntityA.class, 1);
                        Assert.assertNotNull("Assert the em.find() operation returned a LockEntityA.", entityA);

                        System.out.println("Client-B on thread " + tId() + " sending signal clientb-fetch-complete ...");
                        sendSignal("clientb-fetch-complete");
                        checkAbort();

                        System.out.println("Client-B on thread " + tId() + " mutating LockEntityA ...");
                        String entityAStrData = entityA.getStrData();
                        entityA.setStrData(entityAStrData + "-ClientBMutated");

                        checkAbort();
                        System.out.println("Client-B on thread " + tId() + " committing transaction ...");
                        et.commit();

                        System.out.println("Client-B on thread " + tId() + " sending signal clientb-commit-complete ...");
                        sendSignal("clientb-commit-complete");

                        System.out.println("Marking Client B as ending successfully.");
                        endedSuccessfully = true;
                        System.out.flush();
                    } catch (Throwable t) {
                        exception = t;
                        System.out.println("Client-B on thread " + tId() + " encountered Exception:");
                        t.printStackTrace();
                    } finally {
                        System.out.println("Client-B on thread " + tId() + " sending signal clientb-complete ...");
                        System.out.flush();
                        sendSignal("clientb-complete");

                        try {
                            if (em != null && em.getTransaction().isActive()) {
                                System.out.println("Client B on thread " + tId() + " Rolling Back Transaction in finally {}.");
                                em.getTransaction().rollback();
                            }
                            em.close();
                        } catch (Throwable t) {
                        }
                        System.out.println("Client B on thread " + tId() + " exiting.");
                        System.out.flush();
                    }
                }

            };
            final Thread cliBThread = new Thread(cliBTL);
            cliBThread.start();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding LockEntityA(id=1) with Optimistic Read locking...");
            LockEntityA entityA = jpaResource.getEm().find(LockEntityA.class, 1, LockModeType.OPTIMISTIC);
            Assert.assertNotNull("Assert the find operation did not return null.", entityA);

            // Signal Client B to go.
            cliBTL.sendSignal("clientb-start");

            // Wait for Client B to signal that it successfully fetched (expected)
            cliBTL.waitForSignal("clientb-fetch-complete");
            System.out.println("Received Client-B Fetch Complete message.");

            // Wait for Client B to mutate entity and commit
            cliBTL.waitForSignal("clientb-commit-complete");
            System.out.println("Received Client-B Commit Complete message.");

            // Wait for Client B to complete
            cliBTL.waitForSignal("clientb-complete");
            System.out.println("Received Client-B Execution Complete message.");

            if (cliBTL.getException() != null) {
                // If Client B threw an exception, then rethrow it so it gets logged.
                throw cliBTL.getException();
            }
            Assert.assertTrue("Client B did not exit successfully.", cliBTL.getEndedSuccessfully());

            // Client B had updated the entity, which should have also incremented its version field
            // So since an Optimistic Read Lock was acquired on the entity, committing the transaction
            // should call for a version check during the pre-completion phase.
            try {
                System.out.println("Committing transaction (expecting OptimisticLockException) ...");
                jpaResource.getTj().commitTransaction();
                Assert.fail("No OptimisticLockException was thrown.");
            } catch (Throwable t) {
                // Check if OptimisticLockException is in the Exception Chain.
                assertExceptionIsInChain(OptimisticLockException.class, t);
            }
        } catch (java.lang.AssertionError ae) {
            abort(cliBTL);
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            abort(cliBTL);
            throw t;
        } finally {
            if (jpaResource.getTj().isTransactionActive()) {
                try {
                    System.out.println("Rolling back post-test execution active transaction.");
                    jpaResource.getTj().rollbackTransaction();
                } catch (Throwable t) {
                }
            }
            System.out.println(testName + ": End");
        }

    }

    /**
     *
     *
     * @param testExecCtx
     * @param testExecResources
     * @param managedComponentObject
     */
    public void testPessimisticReadLock001(
                                           TestExecutionContext testExecCtx,
                                           TestExecutionResources testExecResources,
                                           Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        AbstractClientB cliBTL = null;
        // Execute Test Case
        try {
            System.out.println(testName + ": Begin");

            EntityManager em = jpaResource.getEm();
            em.clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding LockEntityA(id=1) with Pessimistic Read locking...");
            LockEntityA entityA = jpaResource.getEm().find(LockEntityA.class, 1, LockModeType.PESSIMISTIC_READ);
            Assert.assertNotNull("Assert the find operation did not return null.", entityA);
            int firstVersion = entityA.getVersion();

            jpaResource.getTj().commitTransaction();

            // Verify that the version was not changed.
            em.clear();
            LockEntityA entityAFind = jpaResource.getEm().find(LockEntityA.class, 1);
            Assert.assertNotNull(entityAFind);;
            Assert.assertEquals(firstVersion, entityAFind.getVersion());
        } finally {
            if (jpaResource.getTj().isTransactionActive()) {
                try {
                    System.out.println("Rolling back post-test execution active transaction.");
                    jpaResource.getTj().rollbackTransaction();
                } catch (Throwable t) {
                }
            }
            System.out.println(testName + ": End");
        }

    }

    /**
     *
     *
     * @param testExecCtx
     * @param testExecResources
     * @param managedComponentObject
     */
    public void testWriteLock001(
                                 TestExecutionContext testExecCtx,
                                 TestExecutionResources testExecResources,
                                 Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        AbstractClientB cliBTL = null;
        // Execute Test Case
        try {
            System.out.println(testName + ": Begin");

            EntityManager em = jpaResource.getEm();
            em.clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding LockEntityA(id=1) with Optimistic Write locking...");
            LockEntityA entityA = jpaResource.getEm().find(LockEntityA.class, 1, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            Assert.assertNotNull("Assert the find operation did not return null.", entityA);
            int firstVersion = entityA.getVersion();

            jpaResource.getTj().commitTransaction();

            // Verify that the version was changed.
            em.clear();
            LockEntityA entityAFind = jpaResource.getEm().find(LockEntityA.class, 1);
            Assert.assertNotNull(entityAFind);;
            Assert.assertEquals(firstVersion + 1, entityAFind.getVersion());
        } finally {
            if (jpaResource.getTj().isTransactionActive()) {
                try {
                    System.out.println("Rolling back post-test execution active transaction.");
                    jpaResource.getTj().rollbackTransaction();
                } catch (Throwable t) {
                }
            }
            System.out.println(testName + ": End");
        }

    }

    /**
     *
     *
     * @param testExecCtx
     * @param testExecResources
     * @param managedComponentObject
     */
    public void testWriteLock001A(
                                  TestExecutionContext testExecCtx,
                                  TestExecutionResources testExecResources,
                                  Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        AbstractClientB cliBTL = null;
        // Execute Test Case
        try {
            System.out.println(testName + ": Begin");

            EntityManager em = jpaResource.getEm();
            em.clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding LockEntityA(id=1) with Optimistic Write locking...");
            LockEntityA entityA = jpaResource.getEm().find(LockEntityA.class, 1, LockModeType.WRITE);
            Assert.assertNotNull("Assert the find operation did not return null.", entityA);
            int firstVersion = entityA.getVersion();

            jpaResource.getTj().commitTransaction();

            // Verify that the version was changed.
            em.clear();
            LockEntityA entityAFind = jpaResource.getEm().find(LockEntityA.class, 1);
            Assert.assertNotNull(entityAFind);;
            Assert.assertEquals(firstVersion + 1, entityAFind.getVersion());
        } finally {
            if (jpaResource.getTj().isTransactionActive()) {
                try {
                    System.out.println("Rolling back post-test execution active transaction.");
                    jpaResource.getTj().rollbackTransaction();
                } catch (Throwable t) {
                }
            }
            System.out.println(testName + ": End");
        }

    }

    /**
     * Verifies that the (optimistic) OPTIMISTIC_FORCE_INCREMENT lock verifies that it checks the version during tx beforeCompletion.
     *
     * @param testExecCtx
     * @param testExecResources
     * @param managedComponentObject
     */
    public void testWriteLock002(
                                 TestExecutionContext testExecCtx,
                                 TestExecutionResources testExecResources,
                                 Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        JPAResource jpa2Resource = testExecResources.getJpaResourceMap().get("test-jpa-2-resource");
        if (jpa2Resource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-2-resource').  Cannot execute the test.");
            return;
        }

        AbstractClientB cliBTL = null;
        // Execute Test Case
        try {
            System.out.println(testName + ": Begin");

            EntityManager em = jpaResource.getEm();
            em.clear();

            // Create Client B
            Map<String, CountDownLatch> cdlMap = new HashMap<String, CountDownLatch>();
            cdlMap.put("clientb-start", new CountDownLatch(1));
            cdlMap.put("clientb-fetch-complete", new CountDownLatch(1));
            cdlMap.put("clientb-commit-complete", new CountDownLatch(1));
            cdlMap.put("clientb-complete", new CountDownLatch(1));
            cliBTL = new AbstractClientB(jpa2Resource, cdlMap) {
                @Override
                public void run() {
                    EntityManager em = null;

                    try {
                        em = jpa2Resource.getEmf().createEntityManager();
                        final EntityTransaction et = em.getTransaction();

                        System.out.println("Client-B on thread " + tId() + " waiting for start signal.");
                        waitForSignal("clientb-start");
                        System.out.println("Client-B on thread " + tId() + " received start signal.");
                        checkAbort();

                        System.out.println("Client-B on thread " + tId() + " beginning transaction ...");
                        et.begin();

                        System.out.println("Client-B on thread " + tId() + " Finding LockEntityA ...");
                        LockEntityA entityA = em.find(LockEntityA.class, 1);
                        Assert.assertNotNull("Assert the em.find() operation returned a LockEntityA.", entityA);

                        System.out.println("Client-B on thread " + tId() + " sending signal clientb-fetch-complete ...");
                        sendSignal("clientb-fetch-complete");
                        checkAbort();

                        System.out.println("Client-B on thread " + tId() + " mutating LockEntityA ...");
                        String entityAStrData = entityA.getStrData();
                        entityA.setStrData(entityAStrData + "-ClientBMutated");

                        checkAbort();
                        System.out.println("Client-B on thread " + tId() + " committing transaction ...");
                        et.commit();

                        System.out.println("Client-B on thread " + tId() + " sending signal clientb-commit-complete ...");
                        sendSignal("clientb-commit-complete");

                        System.out.println("Marking Client B as ending successfully.");
                        endedSuccessfully = true;
                        System.out.flush();
                    } catch (Throwable t) {
                        exception = t;
                        System.out.println("Client-B on thread " + tId() + " encountered Exception:");
                        t.printStackTrace();
                    } finally {
                        System.out.println("Client-B on thread " + tId() + " sending signal clientb-complete ...");
                        System.out.flush();
                        sendSignal("clientb-complete");

                        try {
                            if (em != null && em.getTransaction().isActive()) {
                                System.out.println("Client B on thread " + tId() + " Rolling Back Transaction in finally {}.");
                                em.getTransaction().rollback();
                            }
                            em.close();
                        } catch (Throwable t) {
                        }
                        System.out.println("Client B on thread " + tId() + " exiting.");
                        System.out.flush();
                    }
                }

            };
            final Thread cliBThread = new Thread(cliBTL);
            cliBThread.start();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding LockEntityA(id=1) with Optimistic Read locking...");
            LockEntityA entityA = jpaResource.getEm().find(LockEntityA.class, 1, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
            Assert.assertNotNull("Assert the find operation did not return null.", entityA);

            // Signal Client B to go.
            cliBTL.sendSignal("clientb-start");

            // Wait for Client B to signal that it successfully fetched (expected)
            cliBTL.waitForSignal("clientb-fetch-complete");
            System.out.println("Received Client-B Fetch Complete message.");

            // Wait for Client B to mutate entity and commit
            cliBTL.waitForSignal("clientb-commit-complete");
            System.out.println("Received Client-B Commit Complete message.");

            // Wait for Client B to complete
            cliBTL.waitForSignal("clientb-complete");
            System.out.println("Received Client-B Execution Complete message.");

            if (cliBTL.getException() != null) {
                // If Client B threw an exception, then rethrow it so it gets logged.
                throw cliBTL.getException();
            }
            Assert.assertTrue("Client B did not exit successfully.", cliBTL.getEndedSuccessfully());

            // Client B had updated the entity, which should have also incremented its version field
            // So since an Optimistic Read Lock was acquired on the entity, committing the transaction
            // should call for a version check during the pre-completion phase.
            try {
                System.out.println("Committing transaction (expecting OptimisticLockException) ...");
                jpaResource.getTj().commitTransaction();
                Assert.fail("No OptimisticLockException was thrown.");
            } catch (Throwable t) {
                // Check if OptimisticLockException is in the Exception Chain.
                assertExceptionIsInChain(OptimisticLockException.class, t);
            }
        } catch (java.lang.AssertionError ae) {
            abort(cliBTL);
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            abort(cliBTL);
            throw t;
        } finally {
            if (jpaResource.getTj().isTransactionActive()) {
                try {
                    System.out.println("Rolling back post-test execution active transaction.");
                    jpaResource.getTj().rollbackTransaction();
                } catch (Throwable t) {
                }
            }
            System.out.println(testName + ": End");
        }

    }

    /**
     * Verifies that the (optimistic) WRITE lock verifies that it checks the version during tx beforeCompletion.
     *
     * @param testExecCtx
     * @param testExecResources
     * @param managedComponentObject
     */
    public void testWriteLock002A(
                                  TestExecutionContext testExecCtx,
                                  TestExecutionResources testExecResources,
                                  Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        JPAResource jpa2Resource = testExecResources.getJpaResourceMap().get("test-jpa-2-resource");
        if (jpa2Resource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-2-resource').  Cannot execute the test.");
            return;
        }

        AbstractClientB cliBTL = null;
        // Execute Test Case
        try {
            System.out.println(testName + ": Begin");

            EntityManager em = jpaResource.getEm();
            em.clear();

            // Create Client B
            Map<String, CountDownLatch> cdlMap = new HashMap<String, CountDownLatch>();
            cdlMap.put("clientb-start", new CountDownLatch(1));
            cdlMap.put("clientb-fetch-complete", new CountDownLatch(1));
            cdlMap.put("clientb-commit-complete", new CountDownLatch(1));
            cdlMap.put("clientb-complete", new CountDownLatch(1));
            cliBTL = new AbstractClientB(jpa2Resource, cdlMap) {
                @Override
                public void run() {
                    EntityManager em = null;

                    try {
                        em = jpa2Resource.getEmf().createEntityManager();
                        final EntityTransaction et = em.getTransaction();

                        System.out.println("Client-B on thread " + tId() + " waiting for start signal.");
                        waitForSignal("clientb-start");
                        System.out.println("Client-B on thread " + tId() + " received start signal.");
                        checkAbort();

                        System.out.println("Client-B on thread " + tId() + " beginning transaction ...");
                        et.begin();

                        System.out.println("Client-B on thread " + tId() + " Finding LockEntityA ...");
                        LockEntityA entityA = em.find(LockEntityA.class, 1);
                        Assert.assertNotNull("Assert the em.find() operation returned a LockEntityA.", entityA);

                        System.out.println("Client-B on thread " + tId() + " sending signal clientb-fetch-complete ...");
                        sendSignal("clientb-fetch-complete");
                        checkAbort();

                        System.out.println("Client-B on thread " + tId() + " mutating LockEntityA ...");
                        String entityAStrData = entityA.getStrData();
                        entityA.setStrData(entityAStrData + "-ClientBMutated");

                        checkAbort();
                        System.out.println("Client-B on thread " + tId() + " committing transaction ...");
                        et.commit();

                        System.out.println("Client-B on thread " + tId() + " sending signal clientb-commit-complete ...");
                        sendSignal("clientb-commit-complete");

                        System.out.println("Marking Client B as ending successfully.");
                        endedSuccessfully = true;
                        System.out.flush();
                    } catch (Throwable t) {
                        exception = t;
                        System.out.println("Client-B on thread " + tId() + " encountered Exception:");
                        t.printStackTrace();
                    } finally {
                        System.out.println("Client-B on thread " + tId() + " sending signal clientb-complete ...");
                        System.out.flush();
                        sendSignal("clientb-complete");

                        try {
                            if (em != null && em.getTransaction().isActive()) {
                                System.out.println("Client B on thread " + tId() + " Rolling Back Transaction in finally {}.");
                                em.getTransaction().rollback();
                            }
                            em.close();
                        } catch (Throwable t) {
                        }
                        System.out.println("Client B on thread " + tId() + " exiting.");
                        System.out.flush();
                    }
                }

            };
            final Thread cliBThread = new Thread(cliBTL);
            cliBThread.start();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding LockEntityA(id=1) with Optimistic Read locking...");
            LockEntityA entityA = jpaResource.getEm().find(LockEntityA.class, 1, LockModeType.WRITE);
            Assert.assertNotNull("Assert the find operation did not return null.", entityA);

            // Signal Client B to go.
            cliBTL.sendSignal("clientb-start");

            // Wait for Client B to signal that it successfully fetched (expected)
            cliBTL.waitForSignal("clientb-fetch-complete");
            System.out.println("Received Client-B Fetch Complete message.");

            // Wait for Client B to mutate entity and commit
            cliBTL.waitForSignal("clientb-commit-complete");
            System.out.println("Received Client-B Commit Complete message.");

            // Wait for Client B to complete
            cliBTL.waitForSignal("clientb-complete");
            System.out.println("Received Client-B Execution Complete message.");

            if (cliBTL.getException() != null) {
                // If Client B threw an exception, then rethrow it so it gets logged.
                throw cliBTL.getException();
            }
            Assert.assertTrue("Client B did not exit successfully.", cliBTL.getEndedSuccessfully());

            // Client B had updated the entity, which should have also incremented its version field
            // So since an Optimistic Read Lock was acquired on the entity, committing the transaction
            // should call for a version check during the pre-completion phase.
            try {
                System.out.println("Committing transaction (expecting OptimisticLockException) ...");
                jpaResource.getTj().commitTransaction();
                Assert.fail("No OptimisticLockException was thrown.");
            } catch (Throwable t) {
                // Check if OptimisticLockException is in the Exception Chain.
                assertExceptionIsInChain(OptimisticLockException.class, t);
            }
        } catch (java.lang.AssertionError ae) {
            abort(cliBTL);
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            abort(cliBTL);
            throw t;
        } finally {
            if (jpaResource.getTj().isTransactionActive()) {
                try {
                    System.out.println("Rolling back post-test execution active transaction.");
                    jpaResource.getTj().rollbackTransaction();
                } catch (Throwable t) {
                }
            }
            System.out.println(testName + ": End");
        }

    }

    /**
     *
     *
     * @param testExecCtx
     * @param testExecResources
     * @param managedComponentObject
     */
    public void testPessimisticWriteLock001(
                                            TestExecutionContext testExecCtx,
                                            TestExecutionResources testExecResources,
                                            Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        AbstractClientB cliBTL = null;
        // Execute Test Case
        try {
            System.out.println(testName + ": Begin");

            EntityManager em = jpaResource.getEm();
            em.clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding LockEntityA(id=1) with Pessimistic Read locking...");
            LockEntityA entityA = jpaResource.getEm().find(LockEntityA.class, 1, LockModeType.PESSIMISTIC_WRITE);
            Assert.assertNotNull("Assert the find operation did not return null.", entityA);
            int firstVersion = entityA.getVersion();

            jpaResource.getTj().commitTransaction();

            // Verify that the version was not changed.
            em.clear();
            LockEntityA entityAFind = jpaResource.getEm().find(LockEntityA.class, 1);
            Assert.assertNotNull(entityAFind);;
            Assert.assertEquals(firstVersion, entityAFind.getVersion());
        } finally {
            if (jpaResource.getTj().isTransactionActive()) {
                try {
                    System.out.println("Rolling back post-test execution active transaction.");
                    jpaResource.getTj().rollbackTransaction();
                } catch (Throwable t) {
                }
            }
            System.out.println(testName + ": End");
        }

    }

    /**
     *
     *
     * @param testExecCtx
     * @param testExecResources
     * @param managedComponentObject
     */
    public void testPessimisticForceIncrementLock001(
                                                     TestExecutionContext testExecCtx,
                                                     TestExecutionResources testExecResources,
                                                     Object managedComponentObject) throws Throwable {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println(testName + ": Begin");

            EntityManager em = jpaResource.getEm();
            em.clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();

            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding LockEntityA(id=1) with Pessimistic Read locking...");
            LockEntityA entityA = jpaResource.getEm().find(LockEntityA.class, 1, LockModeType.PESSIMISTIC_FORCE_INCREMENT);
            Assert.assertNotNull("Assert the find operation did not return null.", entityA);
            final int firstVersion = entityA.getVersion();

            jpaResource.getTj().commitTransaction();

            // Verify that the version increment actually happened.
            em.clear();
            LockEntityA entityAFind = jpaResource.getEm().find(LockEntityA.class, 1);
            Assert.assertNotNull(entityAFind);;
            Assert.assertEquals(firstVersion + 1, entityAFind.getVersion());
        } finally {
            if (jpaResource.getTj().isTransactionActive()) {
                try {
                    System.out.println("Rolling back post-test execution active transaction.");
                    jpaResource.getTj().rollbackTransaction();
                } catch (Throwable t) {
                }
            }
            System.out.println(testName + ": End");
        }

    }

    private static void abort(AbstractClientB cli) {
        if (cli != null) {
            cli.abort();
        }
    }

    private abstract class AbstractClientB implements Runnable {
        protected JPAResource jpa2Resource;
        protected Map<String, CountDownLatch> cdlMap;
        protected volatile Throwable exception = null;
        protected volatile boolean endedSuccessfully = false;
        protected volatile boolean abort = false;
        private String threadId = null;

        protected AbstractClientB(JPAResource jpa2Resource, Map<String, CountDownLatch> cdlMap) {
            this.jpa2Resource = jpa2Resource;
            this.cdlMap = cdlMap;
        }

        public Throwable getException() {
            return exception;
        }

        public boolean getEndedSuccessfully() {
            return endedSuccessfully;
        }

        public void waitForSignal(String cdl) throws InterruptedException {
            waitForSignal(cdl, 30);
        }

        public void waitForSignal(String cdl, long seconds) throws InterruptedException {
            CountDownLatch countDownLatch = cdlMap.get(cdl);
            if (!countDownLatch.await(seconds, TimeUnit.SECONDS)) {
                // Timer ran out
                throw new InterruptedException("CountDownLatch Timer \"" + cdl + "\" for " + seconds + "s had expired.");
            }
        }

        public void sendSignal(String cdl) {
            CountDownLatch countDownLatch = cdlMap.get(cdl);
            if (countDownLatch.getCount() != 0) {
                countDownLatch.countDown();
            }
        }

        public void abort() {
            abort = true;
        }

        protected void checkAbort() {
            if (abort)
                throw new IllegalStateException("Client execution has been aborted.");
        }

        protected String tId() {
            if (threadId == null) {
                Thread ct = Thread.currentThread();
                threadId = ct.getName() + "/" + ct.getId();
            }
            return threadId;
        }
    }
}
