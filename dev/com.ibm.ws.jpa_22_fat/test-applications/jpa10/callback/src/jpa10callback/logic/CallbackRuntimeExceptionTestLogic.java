/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package jpa10callback.logic;

import java.io.PrintStream;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManager;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Assert;

import jpa10callback.AbstractCallbackListener;
import jpa10callback.AbstractCallbackListener.ProtectionType;
import jpa10callback.CallbackRecord.CallbackLifeCycle;
import jpa10callback.CallbackRuntimeException;
import jpa10callback.entity.ICallbackEntity;
import jpa10callback.listeners.ano.AnoCallbackListenerPackage;
import jpa10callback.listeners.ano.AnoCallbackListenerPrivate;
import jpa10callback.listeners.ano.AnoCallbackListenerProtected;
import jpa10callback.listeners.ano.AnoCallbackListenerPublic;
import jpa10callback.listeners.defaultlistener.DefaultCallbackListenerPackage;
import jpa10callback.listeners.defaultlistener.DefaultCallbackListenerPrivate;
import jpa10callback.listeners.defaultlistener.DefaultCallbackListenerProtected;
import jpa10callback.listeners.defaultlistener.DefaultCallbackListenerPublic;
import jpa10callback.listeners.xml.XMLCallbackListenerProtected;
import jpa10callback.listeners.xml.XMLCallbackListenerPublic;

public class CallbackRuntimeExceptionTestLogic {
    private final static PrintStream out = System.out;
    private final static Random rand = new Random();
    private final static AtomicInteger nextId = new AtomicInteger(rand.nextInt());

    /**
     * Test when a RuntimeException is thrown by a callback method on entity classes.
     * Supports testing of entities declared by annotation and
     * XML, and supports stand-alone entity classes and entities that gain callback methods from
     * mapped superclasses.
     *
     */
    public void testCallbackRuntimeException001(EntityManager em, UserTransaction tx, Class<?> entityClass) throws Exception {
        final String testName = "testCallbackRuntimeException001";
        out.println("Starting " + testName + ": jta=" + (tx != null) + " , entityClass=" + entityClass);

        final int id = nextId.getAndIncrement();
        try {
            Assert.assertNotNull(em);
            Assert.assertNotNull(entityClass);

            AbstractCallbackListener.setTargetPostLoadLifeCycleWithRuntimeException(null);

            // Create an instance of CallbackEntity for update and remove lifecycle tests
            out.println("Beginning new transaction...");
            if (tx == null) {
                em.getTransaction().begin();
            } else {
                tx.begin();
                em.joinTransaction();
            }

            out.println("Creating new object instance of " + entityClass + "...");
            ICallbackEntity entity_persist = (ICallbackEntity) entityClass.getConstructor().newInstance();
            entity_persist.setId(id);
            entity_persist.setName("CallbackEntity-" + id);

            em.persist(entity_persist);

            out.println("Committing transaction...");
            if (tx == null) {
                em.getTransaction().commit();
            } else {
                tx.commit();
            }
            em.clear();

            final int lcID = nextId.getAndIncrement();

            out.println("Testing JPA Life Cycle Methods...");
            testPrePersistLifecycle(entityClass, em, tx, false, null, lcID);
            testPostPersistLifecycle(entityClass, em, tx, false, null, lcID);

            testPreUpdateLifecycle(entityClass, em, tx, false, null, id);
            testPostUpdateLifecycle(entityClass, em, tx, false, null, id);

            testPreRemoveLifecycle(entityClass, em, tx, false, null, id);
            testPostRemoveLifecycle(entityClass, em, tx, false, null, id);

            testPostLoadLifecycle(entityClass, em, tx, false, null, id);

        } finally {
            out.println("Ending " + testName);
        }
    }

    /**
     * Test when a RuntimeException is thrown by a callback method on default listener classes.
     *
     */
    public void testCallbackRuntimeException002(EntityManager em, UserTransaction tx, Class<?> entityClass, ProtectionType listenerProtectionType) throws Exception {
        final String testName = "testCallbackRuntimeException002";
        out.println("Starting " + testName + ": jta=" + (tx != null) + " , entityClass=" + entityClass);

        final int id = nextId.getAndIncrement();
        try {
            Assert.assertNotNull(em);
            Assert.assertNotNull(entityClass);

            AbstractCallbackListener.setTargetPostLoadLifeCycleWithRuntimeException(null);
            resetListeners();

            // Create an instance of CallbackEntity for update and remove lifecycle tests
            out.println("Beginning new transaction...");
            if (tx == null) {
                em.getTransaction().begin();
            } else {
                tx.begin();
                em.joinTransaction();
            }

            out.println("Creating new object instance of " + entityClass + "...");
            ICallbackEntity entity_persist = (ICallbackEntity) entityClass.getConstructor().newInstance();
            entity_persist.setId(id);
            entity_persist.setName("CallbackEntity-" + id);

            em.persist(entity_persist);

            out.println("Committing transaction...");
            if (tx == null) {
                em.getTransaction().commit();
            } else {
                tx.commit();
            }
            em.clear();

            final int lcID = nextId.getAndIncrement();

            out.println("Testing JPA Life Cycle Methods...");
            testPrePersistLifecycle(entityClass, em, tx, true, listenerProtectionType, lcID);
            testPostPersistLifecycle(entityClass, em, tx, true, listenerProtectionType, lcID);

            testPreUpdateLifecycle(entityClass, em, tx, true, listenerProtectionType, id);
            testPostUpdateLifecycle(entityClass, em, tx, true, listenerProtectionType, id);

            testPreRemoveLifecycle(entityClass, em, tx, true, listenerProtectionType, id);
            testPostRemoveLifecycle(entityClass, em, tx, true, listenerProtectionType, id);

            testPostLoadLifecycle(entityClass, em, tx, true, listenerProtectionType, id);
        } finally {
            out.println("Ending " + testName);
            AbstractCallbackListener.resetGlobalCallbackEventList();
        }
    }

    /**
     * Test when a RuntimeException is thrown by a callback method on entity-defined listener classes.
     *
     */
    public void testCallbackRuntimeException003(EntityManager em, UserTransaction tx, Class<?> entityClass, ProtectionType listenerProtectionType) throws Exception {
        final String testName = "testCallbackRuntimeException003";
        out.println("Starting " + testName + ": jta=" + (tx != null) + " , entityClass=" + entityClass);

        final int id = nextId.getAndIncrement();
        try {
            Assert.assertNotNull(em);
            Assert.assertNotNull(entityClass);

            AbstractCallbackListener.setTargetPostLoadLifeCycleWithRuntimeException(null);
            resetListeners();

            // Create an instance of CallbackEntity for update and remove lifecycle tests
            out.println("Beginning new transaction...");
            if (tx == null) {
                em.getTransaction().begin();
            } else {
                tx.begin();
                em.joinTransaction();
            }

            out.println("Creating new object instance of " + entityClass + "...");
            ICallbackEntity entity_persist = (ICallbackEntity) entityClass.getConstructor().newInstance();
            entity_persist.setId(id);
            entity_persist.setName("CallbackEntity-" + id);

            em.persist(entity_persist);

            out.println("Committing transaction...");
            if (tx == null) {
                em.getTransaction().commit();
            } else {
                tx.commit();
            }
            em.clear();

            final int lcID = nextId.getAndIncrement();

            out.println("Testing JPA Life Cycle Methods...");
            testPrePersistLifecycle(entityClass, em, tx, false, listenerProtectionType, lcID);
            testPostPersistLifecycle(entityClass, em, tx, false, listenerProtectionType, lcID);

            testPreUpdateLifecycle(entityClass, em, tx, false, listenerProtectionType, id);
            testPostUpdateLifecycle(entityClass, em, tx, false, listenerProtectionType, id);

            testPreRemoveLifecycle(entityClass, em, tx, false, listenerProtectionType, id);
            testPostRemoveLifecycle(entityClass, em, tx, false, listenerProtectionType, id);

            testPostLoadLifecycle(entityClass, em, tx, false, listenerProtectionType, id);
        } finally {
            out.println("Ending " + testName);
            AbstractCallbackListener.resetGlobalCallbackEventList();
        }
    }

    // Utility

    private AbstractCallbackListener fetchTargetListener(ProtectionType listenerProtectionType) {
        switch (listenerProtectionType) {
            case PT_PACKAGE:
                return DefaultCallbackListenerPackage.getSingleton();
            case PT_PRIVATE:
                return DefaultCallbackListenerPrivate.getSingleton();
            case PT_PROTECTED:
                return DefaultCallbackListenerProtected.getSingleton();
            case PT_PUBLIC:
                return DefaultCallbackListenerPublic.getSingleton();
            default:
                return null;
        }
    }

    private AbstractCallbackListener fetchTargetEntityListener(ProtectionType listenerProtectionType, boolean annotated) {
        if (annotated) {
            switch (listenerProtectionType) {
                case PT_PACKAGE:
                    return AnoCallbackListenerPackage.getSingleton();
                case PT_PRIVATE:
                    return AnoCallbackListenerPrivate.getSingleton();
                case PT_PROTECTED:
                    return AnoCallbackListenerProtected.getSingleton();
                case PT_PUBLIC:
                    return AnoCallbackListenerPublic.getSingleton();
                default:
                    return null;
            }
        } else {
            switch (listenerProtectionType) {
                case PT_PACKAGE:
                    return XMLCallbackListenerPublic.getSingleton();
                case PT_PRIVATE:
                    return XMLCallbackListenerPublic.getSingleton();
                case PT_PROTECTED:
                    return XMLCallbackListenerProtected.getSingleton();
                case PT_PUBLIC:
                    return XMLCallbackListenerPublic.getSingleton();
                default:
                    return null;
            }
        }
    }

    private AbstractCallbackListener fetchTargetEntityListener(ProtectionType listenerProtectionType,
                                                               Class<?> targetEntityType) {
        if (targetEntityType.toString().toLowerCase().contains("ano")) {
            return fetchTargetEntityListener(listenerProtectionType, true);
        } else {
            return fetchTargetEntityListener(listenerProtectionType, false);
        }
    }

    private void resetListeners() {
        for (ProtectionType listenerType : ProtectionType.values()) {
            if (fetchTargetListener(listenerType) != null) {
                fetchTargetListener(listenerType).setRuntimeExceptionLifecycleTarget(null);
            }
        }

        for (ProtectionType listenerType : ProtectionType.values()) {
            if (fetchTargetEntityListener(listenerType, true) != null) {
                fetchTargetEntityListener(listenerType, true).setRuntimeExceptionLifecycleTarget(null);
            }
            if (fetchTargetEntityListener(listenerType, false) != null) {
                fetchTargetEntityListener(listenerType, false).setRuntimeExceptionLifecycleTarget(null);
            }
        }
    }

    /*
     * Test Strategy:
     * - Start Transaction
     * - Create Unpersisted Callback Entity
     * - Mark the new Entity object to throw a CallbackRuntimeException when the PrePersist
     * callback lifecycle method is invoked.
     * - Call em.persist() to try persist the new entity
     * - This should throw a CallbackRuntime Exception and mark the transaction for rollback
     * - Catch the Exception, make sure it is CallbackRuntimeException
     * - Check the Transaction and make sure it is marked for rollback
     * - Roll back the Transaction (cleanup)
     *
     * Sub-Test passes if the persist operation throws a CallbackRuntimeException, the transaction remains
     * active, and is marked for rollback.
     *
     * Sub-Points: 3
     */
    @SuppressWarnings("unchecked")
    private void testPrePersistLifecycle(
                                         Class targetEntityType,
                                         EntityManager em,
                                         UserTransaction tx,
                                         boolean targetDefaultListener,
                                         ProtectionType listenerProtectionType,
                                         final int id) throws Exception {
        out.println("Testing @PrePersist Exception behavior...");
        resetListeners();

        // Clear persistence context
        out.println("Clearing persistence context...");
        em.clear();

        AbstractCallbackListener targetListener = null;
        String targetListenerName = "";

        try {
            if (listenerProtectionType != null) {
                if (targetDefaultListener) {
                    targetListener = fetchTargetListener(listenerProtectionType);
                } else {
                    targetListener = fetchTargetEntityListener(listenerProtectionType, targetEntityType);
                }
                targetListenerName = targetListener.getClass().getSimpleName();
            }

            // 1) Create Unpersisted Callback Entity
            out.println("1) Create Unpersisted Callback Entity");

            // Begin new transaction
            out.println("Beginning new transaction...");
            if (tx == null) {
                em.getTransaction().begin();
            } else {
                tx.begin();
                em.joinTransaction();
            }

            out.println("Creating new object instance of \"" + targetEntityType.getName() + "\" with id = \"" + id + "\" ...");
            ICallbackEntity entity_persist = (ICallbackEntity) targetEntityType.getConstructor().newInstance();
            entity_persist.setId(id);
            entity_persist.setName("CallbackEntity-" + id);

            // Configure to throw a CallbackRuntimeException during the @PrePersist callback.
            if (targetListener == null) {
                out.println("2) Configuring the entity object to throw a CallbackRuntimeException during the @PrePersist callback.");
                ((AbstractCallbackListener) entity_persist).setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PrePersist);
            } else {
                out.println("2) Configuring " + targetListenerName +
                            " listener to throw a CallbackRuntimeException during the @PrePersist callback.");
                targetListener.setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PrePersist);
            }

            try {
                out.println("3) Calling em.persist (should fail with a CallbackRuntimeException.");
                em.persist(entity_persist);

                Assert.fail("No Exception was thrown by the em.persist() operation.");
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                Assert.assertThat("Assert CallbackRuntimeException is in Exception chain.",
                                  t,
                                  getExceptionChainMatcher(CallbackRuntimeException.class));
            }

            // 4) Check if transaction is still active
            assertTransactionIsActive("4) Assert transaction is still active.", em, tx);

            // 5) Check if transaction is marked for rollback
            assertMarkedForRollback("5) Assert transaction is marked for rollback.", em, tx);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            throw t;
        } finally {
            // Cleanup
            resetListeners();
            cleanup(em, tx);
        }
    }

    /*
     * Test Strategy:
     * - Start Transaction
     * - Create Unpersisted Callback Entity
     * - Mark the new Entity object to throw a CallbackRuntimeException when the PostPersist
     * callback lifecycle method is invoked.
     * - Call em.persist() to try persist the new entity
     * - Depending on the JPA implementation, the postpersist method may be called by the persist() operation,
     * or it may be called when the transaction is being committed.
     *
     * Sub-Test passes if the persist operation (or transaction commit) throws a CallbackRuntimeException.
     *
     * Sub-Points: 1
     */
    private void testPostPersistLifecycle(
                                          Class targetEntityType,
                                          EntityManager em,
                                          UserTransaction tx,
                                          boolean targetDefaultListener,
                                          ProtectionType listenerProtectionType,
                                          final int id) throws Exception {
        out.println("Testing @PostPersist Exception behavior...");
        resetListeners();

        // Clear persistence context
        out.println("Clearing persistence context...");
        em.clear();

        AbstractCallbackListener targetListener = null;
        String targetListenerName = "";
        try {
            if (listenerProtectionType != null) {
                if (targetDefaultListener) {
                    targetListener = fetchTargetListener(listenerProtectionType);
                } else {
                    targetListener = fetchTargetEntityListener(listenerProtectionType, targetEntityType);
                }
                targetListenerName = targetListener.getClass().getSimpleName();
            }

            // 1) Create Unpersisted Callback Entity
            out.println("1) Create Unpersisted Callback Entity");

            // Begin new transaction
            out.println("Beginning new transaction...");
            if (tx == null) {
                em.getTransaction().begin();
            } else {
                tx.begin();
                em.joinTransaction();
            }

            out.println("Creating new object instance of \"" + targetEntityType.getName() + "\" with id = \"" + id + "\"...");
            ICallbackEntity entity_persist = (ICallbackEntity) targetEntityType.getConstructor().newInstance();
            entity_persist.setId(id);
            entity_persist.setName("CallbackEntity-" + id);

            // Configure the entity object to throw a CallbackRuntimeException during the @PostPersist callback.
            if (targetListener == null) {
                out.println("2) Configuring the entity object to throw a CallbackRuntimeException during the @PostPersist callback.");
                ((AbstractCallbackListener) entity_persist).setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PostPersist);
            } else {
                out.println("2) Configuring the " + targetListenerName +
                            " listener to throw a CallbackRuntimeException during the @PostPersist callback.");
                targetListener.setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PostPersist);
            }

            boolean exceptionThrownByPersistOperation = true;
            try {
                out.println("3) Calling em.persist (may fail with a CallbackRuntimeException.");
                em.persist(entity_persist);

                out.println("The em.persist() operation did not throw a CallbackRuntimeException.");
                out.println("This means the Exception must be thrown by the transaction commit operation.");
                exceptionThrownByPersistOperation = false;

                out.println("Committing transaction...");
                if (tx == null) {
                    em.getTransaction().commit();
                } else {
                    tx.commit();
                }

                Assert.fail("No Exception was thrown by either the em.persist() or tran-commit operations.");
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                Assert.assertThat("Assert CallbackRuntimeException is in Exception chain.",
                                  t,
                                  getExceptionChainMatcher(CallbackRuntimeException.class));
            }

            // If the persist() operation did throw the Exception, then we have to check if the tran is still
            // active and is marked for rollback.
            if (exceptionThrownByPersistOperation == true) {
                out.println("The CallbackRuntimeException was thrown by the em.persist() operation,");
                out.println("so verify that the transaction is active and marked for rollback.");
                out.println("Is transaction still active = " + isTransactionActive(em, tx));
                out.println("Is transaction marked for rollback = " + isTransactionMarkedForRollback(em, tx));
                if (isTransactionActive(em, tx) == false ||
                    isTransactionMarkedForRollback(em, tx) == false) {
                    Assert.fail("One or both of the criteria failed.");
                }
            }
        } finally {
            // Cleanup
            resetListeners();
            cleanup(em, tx);
        }
    }

    /*
     * Test Strategy:
     * - Start Transaction
     * - Find CallbackEntity(id=1)
     * - Dirty the CallbackEntity - this may fire the @PreUpdate callback on some JPA implementations.
     * - (If not fired by the update) Commit the transaction, this should fire the @PreUpdate callback
     *
     * - Check that the Exception thrown by the update or transaction commit operation contains
     * CallbackRuntimeException in its chain.
     *
     * - If the CallbackRuntimeException was thrown by the update operation, check that the transaction is
     * still active and marked for rollback.
     *
     * Sub-Points: 2
     */
    private void testPreUpdateLifecycle(
                                        Class targetEntityType,
                                        EntityManager em,
                                        UserTransaction tx,
                                        boolean targetDefaultListener,
                                        ProtectionType listenerProtectionType,
                                        final int id) throws Exception {
        out.println("Testing @PreUpdate Exception behavior...");
        resetListeners();

        // Clear persistence context
        out.println("Clearing persistence context...");
        em.clear();

        AbstractCallbackListener targetListener = null;
        String targetListenerName = "";
        try {
            if (listenerProtectionType != null) {
                if (targetDefaultListener) {
                    targetListener = fetchTargetListener(listenerProtectionType);
                } else {
                    targetListener = fetchTargetEntityListener(listenerProtectionType, targetEntityType);
                }
                targetListenerName = targetListener.getClass().getSimpleName();
            }

            // 1) Load Callback Entity
            out.println("1) Load \"" + targetEntityType.getName() + "\" with id = \"" + id + "\" ...");

            // Begin new transaction
            out.println("Beginning new transaction...");
            if (tx == null) {
                em.getTransaction().begin();
            } else {
                tx.begin();
                em.joinTransaction();
            }

            ICallbackEntity entity = (ICallbackEntity) em.find(targetEntityType, id);
            Assert.assertNotNull("Assert find() did not return null.", entity);

            // Configure the entity object to throw a CallbackRuntimeException during the @PreUpdate callback.
            if (targetListener == null) {
                out.println("2) Configuring the entity object to throw a CallbackRuntimeException during the @PreUpdate callback.");
                ((AbstractCallbackListener) entity).setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PreUpdate);
            } else {
                out.println("2) Configuring the " + targetListenerName +
                            " listener to throw a CallbackRuntimeException during the @PreUpdate callback.");
                targetListener.setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PreUpdate);
            }

            boolean exceptionThrownByUpdate = true;
            try {
                out.println("3) Dirty the entity (may fail with a CallbackRuntimeException.");
                entity.setName("Dirty Name");

                out.println("The update did not throw a CallbackRuntimeException.");
                out.println("This means the Exception must be thrown by the transaction commit operation.");
                exceptionThrownByUpdate = false;

                out.println("Committing transaction...");
                if (tx == null) {
                    em.getTransaction().commit();
                } else {
                    tx.commit();
                }

                Assert.fail("No Exception was thrown by either the update or tran-commit operations.");
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                Assert.assertThat("Assert CallbackRuntimeException is in Exception chain.",
                                  t,
                                  getExceptionChainMatcher(CallbackRuntimeException.class));
            }

            // If the update operation did throw the Exception, then we have to check if the tran is still
            // active and is marked for rollback.
            if (exceptionThrownByUpdate == true) {
                out.println("The CallbackRuntimeException was thrown by the update operation,");
                out.println("so verify that the transaction is active and marked for rollback.");
                out.println("Is transaction still active = " + isTransactionActive(em, tx));
                out.println("Is transaction marked for rollback = " + isTransactionMarkedForRollback(em, tx));
                if (isTransactionActive(em, tx) == false ||
                    isTransactionMarkedForRollback(em, tx) == false) {
                    Assert.fail("One or both of the criteria failed.");
                }
            }
        } finally {
            // Cleanup
            resetListeners();
            cleanup(em, tx);
        }
    }

    /*
     * Test Strategy:
     * - Start Transaction
     * - Find CallbackEntity(id=1)
     * - Dirty the CallbackEntity.
     * - Commit the transaction, this should fire the @PostUpdate callback
     *
     * - Check that the Exception thrown by the update or transaction commit operation contains
     * CallbackRuntimeException in its chain.
     *
     * - If the CallbackRuntimeException was thrown by the update operation, check that the transaction is
     * still active and marked for rollback.
     *
     * Sub-Points: 2
     */
    private void testPostUpdateLifecycle(
                                         Class targetEntityType,
                                         EntityManager em,
                                         UserTransaction tx,
                                         boolean targetDefaultListener,
                                         ProtectionType listenerProtectionType,
                                         final int id) throws Exception {
        out.println("Testing @PostUpdate Exception behavior...");
        resetListeners();

        // Clear persistence context
        out.println("Clearing persistence context...");
        em.clear();

        AbstractCallbackListener targetListener = null;
        String targetListenerName = "";
        try {
            if (listenerProtectionType != null) {
                if (targetDefaultListener) {
                    targetListener = fetchTargetListener(listenerProtectionType);
                } else {
                    targetListener = fetchTargetEntityListener(listenerProtectionType, targetEntityType);
                }
                targetListenerName = targetListener.getClass().getSimpleName();
            }

            // 1) Load Callback Entity
            out.println("1) Load \"" + targetEntityType.getName() + "\" with id = \"" + id + "\" ...");

            // Begin new transaction
            out.println("Beginning new transaction...");
            if (tx == null) {
                em.getTransaction().begin();
            } else {
                tx.begin();
                em.joinTransaction();
            }

            ICallbackEntity entity = (ICallbackEntity) em.find(targetEntityType, id);
            Assert.assertNotNull("Assert find() did not return null.", entity);

            // Configure the entity object to throw a CallbackRuntimeException during the @PostUpdate callback.
            if (targetListener == null) {
                out.println("2) Configuring the entity object to throw a CallbackRuntimeException during the @PostUpdate callback.");
                ((AbstractCallbackListener) entity).setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PostUpdate);
            } else {
                out.println("2) Configuring the " + targetListenerName +
                            " listener to throw a CallbackRuntimeException during the @PostUpdate callback.");
                targetListener.setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PostUpdate);
            }

            try {
                out.println("3) Dirty the entity.");
                entity.setName("Dirty Name");

                out.println("Committing transaction (Should throw Exception) ...");
                if (tx == null) {
                    em.getTransaction().commit();
                } else {
                    tx.commit();
                }

                Assert.fail("No Exception was thrown by tran commit operation.");
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                Assert.assertThat("Assert CallbackRuntimeException is in Exception chain.",
                                  t,
                                  getExceptionChainMatcher(CallbackRuntimeException.class));
            }
        } finally {
            // Cleanup
            resetListeners();
            cleanup(em, tx);
        }
    }

    /*
     * Test Strategy:
     * - Start Transaction
     * - Find CallbackEntity(id=1)
     * - Mark the CallbackEntity for removal - this may fire the @PreRemove callback on some JPA implementations.
     * - (If not fired by the remove op) Commit the transaction, this should fire the @PreRemove callback
     *
     * - Check that the Exception thrown by the update or transaction commit operation contains
     * CallbackRuntimeException in its chain.
     *
     * - If the CallbackRuntimeException was thrown by the remove operation, check that the transaction is
     * still active and marked for rollback.
     *
     * Sub-Points: 2
     */
    private void testPreRemoveLifecycle(
                                        Class targetEntityType,
                                        EntityManager em,
                                        UserTransaction tx,
                                        boolean targetDefaultListener,
                                        ProtectionType listenerProtectionType,
                                        final int id) throws Exception {
        out.println("Testing @PreRemove Exception behavior...");
        resetListeners();

        // Clear persistence context
        out.println("Clearing persistence context...");
        em.clear();

        AbstractCallbackListener targetListener = null;
        String targetListenerName = "";
        try {
            if (listenerProtectionType != null) {
                if (targetDefaultListener) {
                    targetListener = fetchTargetListener(listenerProtectionType);
                } else {
                    targetListener = fetchTargetEntityListener(listenerProtectionType, targetEntityType);
                }
                targetListenerName = targetListener.getClass().getSimpleName();
            }

            // 1) Load Callback Entity
            out.println("1) Load \"" + targetEntityType.getName() + "\" with id = \"" + id + "\" ...");

            // Begin new transaction
            out.println("Beginning new transaction...");
            if (tx == null) {
                em.getTransaction().begin();
            } else {
                tx.begin();
                em.joinTransaction();
            }

            ICallbackEntity entity = (ICallbackEntity) em.find(targetEntityType, id);
            Assert.assertNotNull("Assert find() did not return null.", entity);

            // Configure the entity object to throw a CallbackRuntimeException during the @PreRemove callback.
            if (targetListener == null) {
                out.println("2) Configuring the entity object to throw a CallbackRuntimeException during the @PreRemove callback.");
                ((AbstractCallbackListener) entity).setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PreRemove);
            } else {
                out.println("2) Configuring the " + targetListenerName +
                            " listener to throw a CallbackRuntimeException during the @PreRemove callback.");
                targetListener.setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PreRemove);
            }

            boolean exceptionThrownByRemove = true;
            try {
                out.println("3) Mark the entity for removal (may fail with a CallbackRuntimeException.");
                em.remove(entity);

                out.println("The remove did not throw a CallbackRuntimeException.");
                out.println("This means the Exception must be thrown by the transaction commit operation.");
                exceptionThrownByRemove = false;

                out.println("Committing transaction...");
                if (tx == null) {
                    em.getTransaction().commit();
                } else {
                    tx.commit();
                }

                Assert.fail("No Exception was thrown by either the em.remove() or tran-commit operations.");
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                Assert.assertThat("Assert CallbackRuntimeException is in Exception chain.",
                                  t,
                                  getExceptionChainMatcher(CallbackRuntimeException.class));
            }

            // If the remove operation did throw the Exception, then we have to check if the tran is still
            // active and is marked for rollback.
            if (exceptionThrownByRemove == true) {
                out.println("The CallbackRuntimeException was thrown by the remove operation,");
                out.println("so verify that the transaction is active and marked for rollback.");
                out.println("Is transaction still active = " + isTransactionActive(em, tx));
                out.println("Is transaction marked for rollback = " + isTransactionMarkedForRollback(em, tx));
                if (isTransactionActive(em, tx) == false ||
                    isTransactionMarkedForRollback(em, tx) == false) {
                    Assert.fail("One or both of the criteria failed.");
                }
            }
        } finally {
            // Cleanup
            resetListeners();
            cleanup(em, tx);
        }
    }

    /*
     * Test Strategy:
     * - Start Transaction
     * - Find CallbackEntity(id=1)
     * - Mark the CallbackEntity for removal
     * - Commit the transaction, this should fire the @PreRemove callback
     *
     * - Check that the Exception thrown by the transaction commit operation contains
     * CallbackRuntimeException in its chain.
     *
     * - If the CallbackRuntimeException was thrown by the update operation, check that the transaction is
     * still active and marked for rollback.
     *
     * Sub-Points: 2
     */
    private void testPostRemoveLifecycle(
                                         Class targetEntityType,
                                         EntityManager em,
                                         UserTransaction tx,
                                         boolean targetDefaultListener,
                                         ProtectionType listenerProtectionType,
                                         final int id) throws Exception {
        out.println("Testing @PostRemove Exception behavior...");
        resetListeners();

        // Clear persistence context
        out.println("Clearing persistence context...");
        em.clear();

        AbstractCallbackListener targetListener = null;
        String targetListenerName = "";
        try {
            if (listenerProtectionType != null) {
                if (targetDefaultListener) {
                    targetListener = fetchTargetListener(listenerProtectionType);
                } else {
                    targetListener = fetchTargetEntityListener(listenerProtectionType, targetEntityType);
                }
                targetListenerName = targetListener.getClass().getSimpleName();
            }

            // 1) Load Callback Entity
            out.println("1) Load \"" + targetEntityType.getName() + "\" with id = \"" + id + "\" ...");

            // Begin new transaction
            out.println("Beginning new transaction...");
            if (tx == null) {
                em.getTransaction().begin();
            } else {
                tx.begin();
                em.joinTransaction();
            }

            ICallbackEntity entity = (ICallbackEntity) em.find(targetEntityType, id);
            Assert.assertNotNull("Assert find() did not return null.", entity);

            // Configure the entity object to throw a CallbackRuntimeException during the @PostRemove callback.
            if (targetListener == null) {
                out.println("2) Configuring the entity object to throw a CallbackRuntimeException during the @PostRemove callback.");
                ((AbstractCallbackListener) entity).setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PostRemove);
            } else {
                out.println("2) Configuring the " + targetListenerName +
                            " listener to throw a CallbackRuntimeException during the @PostRemove callback.");
                targetListener.setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PostRemove);
            }

            try {
                out.println("3) Remove the entity.");
                em.remove(entity);

                out.println("Committing transaction (Should throw Exception) ...");
                if (tx == null) {
                    em.getTransaction().commit();
                } else {
                    tx.commit();
                }

                Assert.fail("No Exception was thrown by tran commit operation.");
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                Assert.assertThat("Assert CallbackRuntimeException is in Exception chain.",
                                  t,
                                  getExceptionChainMatcher(CallbackRuntimeException.class));
            }
        } finally {
            // Cleanup
            resetListeners();
            cleanup(em, tx);
        }
    }

    /*
     * Test Strategy:
     * - Set AbstractCallbackListener to throw Exception on PostLoad events
     * - Start Transaction
     * - Find CallbackEntity(id=1) - should trigger the Exception
     * - Verify that the transaction is active and marked for rollback.
     *
     * Sub-Points: 3
     */
    private void testPostLoadLifecycle(
                                       Class targetEntityType,
                                       EntityManager em,
                                       UserTransaction tx,
                                       boolean targetDefaultListener,
                                       ProtectionType listenerProtectionType,
                                       final int id) throws Exception {
        out.println("Testing @PostLoad Exception behavior...");
        resetListeners();

        // Clear persistence context
        out.println("Clearing persistence context...");
        em.clear();

        try {
            // 1) Set CallbackListener to throw CallbackRuntimeException on PostLoad callback invocation
            out.println("1) Set CallbackListener to throw CallbackRuntimeException on PostLoad callback invocation");
            AbstractCallbackListener.setTargetPostLoadLifeCycleWithRuntimeException(
                                                                                    (listenerProtectionType == null) ? ProtectionType.ALL : listenerProtectionType);

            try {
                // 2) Load Callback Entity
                out.println("2) Load \"" + targetEntityType.getName() + "\" with id = \"" + id + "\" (should throw Exception) ...");

                // Begin new transaction
                out.println("Beginning new transaction...");
                if (tx == null) {
                    em.getTransaction().begin();
                } else {
                    tx.begin();
                    em.joinTransaction();
                }

                ICallbackEntity entity = (ICallbackEntity) em.find(targetEntityType, id);
                Assert.fail("No Exception was thrown by find operation.");
            } catch (Throwable t) {
                Assert.assertThat("Assert CallbackRuntimeException is in Exception chain.",
                                  t,
                                  getExceptionChainMatcher(CallbackRuntimeException.class));
            }

            // 3) Check if transaction is still active
            assertTransactionIsActive("3) Assert transaction is still active.", em, tx);

            // 4) Check if transaction is marked for rollback
            assertMarkedForRollback("4) Assert transaction is marked for rollback.", em, tx);
        } finally {
            AbstractCallbackListener.setTargetPostLoadLifeCycleWithRuntimeException(null);

            // Cleanup
            cleanup(em, tx);
        }
    }

    private void cleanup(EntityManager em, UserTransaction tx) {
        try {
            if (tx == null) {
                if (em.getTransaction().isActive())
                    em.getTransaction().rollback();
            } else {
                int status = tx.getStatus();
                if (status != Status.STATUS_NO_TRANSACTION)
                    tx.rollback();
            }
        } catch (Throwable t) {
        }
    }

    private boolean isTransactionActive(EntityManager em, UserTransaction tx) throws SystemException {
        if (tx == null) {
            return em.getTransaction().isActive();
        } else {
            return tx.getStatus() != Status.STATUS_NO_TRANSACTION;
        }
    }

    private boolean isTransactionMarkedForRollback(EntityManager em, UserTransaction tx) throws SystemException {
        if (tx == null) {
            return em.getTransaction().getRollbackOnly();
        } else {
            return tx.getStatus() == Status.STATUS_MARKED_ROLLBACK;
        }
    }

    private void assertTransactionIsActive(String message, EntityManager em, UserTransaction tx) throws SystemException {
        Assert.assertTrue(message, isTransactionActive(em, tx));
    }

    private void assertMarkedForRollback(String message, EntityManager em, UserTransaction tx) throws SystemException {
        Assert.assertTrue(message, isTransactionMarkedForRollback(em, tx));
    }

    @SuppressWarnings("rawtypes")
    public Matcher getExceptionChainMatcher(final Class t) {
        return new BaseMatcher() {
            final protected Class<?> expectedThrowableClass = t;

            @Override
            public boolean matches(Object obj) {
                if (obj == null) {
                    return (expectedThrowableClass == null);
                }

                if (!(obj instanceof Throwable)) {
                    return false;
                }

                Throwable t = (Throwable) obj;
                while (t != null) {
                    if (expectedThrowableClass.equals(t.getClass())) {
                        return true;
                    }

                    out.println("getExceptionChainMatcher: looking for " + expectedThrowableClass +
                                " but found " + t.getClass() + "." +
                                (t.getCause() == null ? "" : "  Testing nested cause: " + t.getCause().getClass()));

                    t = t.getCause();
                }

                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(expectedThrowableClass.toString());
            }

        };
    }
}
