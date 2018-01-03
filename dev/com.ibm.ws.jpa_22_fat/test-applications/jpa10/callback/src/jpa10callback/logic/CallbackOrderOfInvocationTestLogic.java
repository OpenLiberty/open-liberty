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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Assert;

import jpa10callback.AbstractCallbackListener;
import jpa10callback.AbstractCallbackListener.ProtectionType;
import jpa10callback.CallbackRecord;
import jpa10callback.CallbackRecord.CallbackLifeCycle;
import jpa10callback.entity.ICallbackEntity;
import jpa10callback.listeners.orderofinvocation.a1.AnoCallbackListenerPackageA1;
import jpa10callback.listeners.orderofinvocation.a2.AnoCallbackListenerPackageA2;
import jpa10callback.listeners.orderofinvocation.b1.AnoCallbackListenerPackageB1;
import jpa10callback.listeners.orderofinvocation.b2.AnoCallbackListenerPackageB2;
import jpa10callback.listeners.orderofinvocation.c1.AnoCallbackListenerPackageC1;
import jpa10callback.listeners.orderofinvocation.c2.AnoCallbackListenerPackageC2;
import jpa10callback.listeners.orderofinvocation.defaultlistener.g1.DefaultCallbackListenerPackageG1;
import jpa10callback.listeners.orderofinvocation.defaultlistener.g1.DefaultCallbackListenerPrivateG1;
import jpa10callback.listeners.orderofinvocation.defaultlistener.g1.DefaultCallbackListenerProtectedG1;
import jpa10callback.listeners.orderofinvocation.defaultlistener.g1.DefaultCallbackListenerPublicG1;
import jpa10callback.listeners.orderofinvocation.defaultlistener.g2.DefaultCallbackListenerPackageG2;
import jpa10callback.listeners.orderofinvocation.defaultlistener.g2.DefaultCallbackListenerPrivateG2;
import jpa10callback.listeners.orderofinvocation.defaultlistener.g2.DefaultCallbackListenerProtectedG2;
import jpa10callback.listeners.orderofinvocation.defaultlistener.g2.DefaultCallbackListenerPublicG2;

public class CallbackOrderOfInvocationTestLogic {
    private final static PrintStream out = System.out;
    private final static Random rand = new Random();
    private final static AtomicInteger nextId = new AtomicInteger(rand.nextInt());

    /**
     * Verify that the order of invocation of callback methods, as defined by the JPA Specification
     * section 3.5.4, is demonstrated:
     *
     * Default Listener, invoked in the order they are defined in the XML Mapping File
     * Entity Listeners defined by the EntityListener annotation on an Entity class or Mapped Superclass (in the order of appearance in the annotation).
     * With inheritance, the order of invocation starts at the highest superclass defining an EntityListener, moving down to the leaf entity class.
     * Lifecycle methods defined by entity classes and mapped superclasses are invoked in the order from highest superclass to the leaf entity class
     *
     * To verify this, the test will execute in the following environment:
     *
     * Default Entity Listener: DefaultListener1 and DefaultListener2, defined in that order in the XML Mapping File
     * Abstract Entity using Table-Per-Class inheritance methodology, with the following:
     * EntityListenerA1, EntityListenerA2, defined in that order
     * Callback methods for each lifecycle type (A_PrePersist, A_PostPersist, etc.)
     * Mapped Superclass with the following:
     * EntityListenerB1, EntityListenerB2, defined in that order
     * Callback methods for each lifecycle type (B_PrePersist, B_PostPersist, etc.)
     * Leaf entity with the following:
     * EntityListenerC1, EntityListenerC2, defined in that order
     * Callback methods for each lifecycle type (C_PrePersist, C_PostPersist, etc.)
     *
     * For each callback type, the following invocation order is expected:
     * DefaultCallbackListener[ProtType]G1
     * DefaultCallbackListener[ProtType]G2
     * [EntType]CallbackListener[ProtType]A1
     * [EntType]CallbackListener[ProtType]A2
     * [EntType]CallbackListener[ProtType]B1
     * [EntType]CallbackListener[ProtType]B2
     * [EntType]CallbackListener[ProtType]C1
     * [EntType]CallbackListener[ProtType]C2
     * [EntType]OOIRoot[ProtType]Entity
     * [EntType]OOIMSC[ProtType]Entity
     * [EntType]OOILeaf[ProtType]Entity
     *
     * Where [ProtType] = Package|Private|Protected|Public
     * Where [EntType] = Ano|XML
     *
     */
    public void testOrderOfInvocation001(EntityManager em, UserTransaction tx, Class<?> entityClass, ProtectionType listenerProtectionType) throws Exception {
        final String testName = "testOrderOfInvocation001";
        out.println("Starting " + testName + ": jta=" + (tx != null) + " , entityClass=" + entityClass);

        final int id = nextId.getAndIncrement();
        try {
            Assert.assertNotNull(em);
            Assert.assertNotNull(entityClass);
            Assert.assertNotNull(listenerProtectionType);

            testPrePersistLifecycle(entityClass, em, tx, listenerProtectionType, id);
            testPostPersistLifecycle(entityClass, em, tx, listenerProtectionType, id);
            testPostLoadLifecycle(entityClass, em, tx, listenerProtectionType, id);
            testPreUpdateLifecycle(entityClass, em, tx, listenerProtectionType, id);
            testPostUpdateLifecycle(entityClass, em, tx, listenerProtectionType, id);
            testPreRemoveLifecycle(entityClass, em, tx, listenerProtectionType, id);
            testPostRemoveLifecycle(entityClass, em, tx, listenerProtectionType, id);

        } finally {
        }
    }

    /*
     * Test Strategy:
     * - Start Transaction
     * - Create Unpersisted Callback Entity
     * - Verify Callback ordering matches expectations.
     *
     */
    private void testPrePersistLifecycle(
                                         Class targetEntityType,
                                         EntityManager em,
                                         UserTransaction tx,
                                         ProtectionType protectionType,
                                         final int id) throws Exception {
        out.println("Testing @PrePersist Order of Invocation behavior...");
        resetListeners();
        AbstractCallbackListener.setGlobalCallbackEventFilter(CallbackLifeCycle.PrePersist);
        AbstractCallbackListener.setGlobalCallbackProtectionTypeFilter(protectionType);

        // Roll back any active transaction
        cleanup(em, tx);

        // Clear persistence context
        out.println("Clearing persistence context...");
        em.clear();

        try {
            // 1) Create Unpersisted Callback Entity
            out.println("1) Create Unpersisted Callback Entity");

            // Begin new transaction
            beginTx(em, tx);

            out.println("Creating new object instance of " + targetEntityType + "...");
            ICallbackEntity entity_persist = (ICallbackEntity) targetEntityType.getConstructor().newInstance();
            entity_persist.setId(id);
            entity_persist.setName("CallbackEntity-" + id);

            out.println("3) Calling em.persist on " + targetEntityType + " ...");
            em.persist(entity_persist);

            // @PrePersist should have fired, examine the callback invocation order
            out.println("@PrePersist should have fired, examining the callback invocation order...");
            List<CallbackRecord> eventList = AbstractCallbackListener.getGlobalCallbackEventList();

            out.println("Callback events observed:");
            for (CallbackRecord cr : eventList) {
                out.println(cr.toString());
            }

            List<CallbackRecord> expectedEventList = generateExpectedOrderOfInvocationList(targetEntityType,
                                                                                           protectionType, CallbackLifeCycle.PrePersist);
            out.println("Callback events expected:");
            for (CallbackRecord cr : expectedEventList) {
                out.println(cr.toString());
            }

            out.println("Comparing expected vs actual event lists...");
            Assert.assertEquals("Verify event lists are same size.", expectedEventList.size(), eventList.size());
            if (expectedEventList.size() == eventList.size()) {
                for (int index = 0; index < expectedEventList.size(); index++) {
                    CallbackRecord expectedCR = expectedEventList.get(index);
                    CallbackRecord actualCR = eventList.get(index);

                    out.println("Comparing " + expectedCR.getCallerClassName() + "." + expectedCR.getCallerMethodName() +
                                "() vs " +
                                actualCR.getCallerClassName() + "." + actualCR.getCallerMethodName() + "()");

                    Assert.assertTrue("Comparing CallbackRecord at index " + index,
                                      ((expectedCR.getCallerClassName().equals(actualCR.getCallerClassName())) &&
                                       (expectedCR.getCallerMethodName().equals(actualCR.getCallerMethodName()))));
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
     * - Create Unpersisted Callback Entity
     * - Commit Transaction
     * - Verify Callback ordering matches expectations.
     *
     */
    private void testPostPersistLifecycle(
                                          Class targetEntityType,
                                          EntityManager em,
                                          UserTransaction tx,
                                          ProtectionType protectionType,
                                          final int id) throws Exception {
        out.println("Testing @PostPersist Order of Invocation behavior...");
        resetListeners();
        AbstractCallbackListener.setGlobalCallbackEventFilter(CallbackLifeCycle.PostPersist);
        AbstractCallbackListener.setGlobalCallbackProtectionTypeFilter(protectionType);

        // Roll back any active transaction
        cleanup(em, tx);

        // Clear persistence context
        out.println("Clearing persistence context...");
        em.clear();

        try {
            // 1) Create Unpersisted Callback Entity
            out.println("1) Create Unpersisted Callback Entity");

            // Begin new transaction
            beginTx(em, tx);

            out.println("Creating new object instance of " + targetEntityType + "...");
            ICallbackEntity entity_persist = (ICallbackEntity) targetEntityType.getConstructor().newInstance();
            entity_persist.setId(id);
            entity_persist.setName("CallbackEntity-" + id);

            out.println("3) Calling em.persist on " + targetEntityType + " ...");
            em.persist(entity_persist);

            out.println("Committing transaction...");
            commitTx(em, tx);

            // @PrePersist should have fired, examine the callback invocation order
            out.println("@PostPersist should have fired, examining the callback invocation order...");
            List<CallbackRecord> eventList = AbstractCallbackListener.getGlobalCallbackEventList();

            out.println("Callback events observed:");
            for (CallbackRecord cr : eventList) {
                out.println(cr.toString());
            }

            List<CallbackRecord> expectedEventList = generateExpectedOrderOfInvocationList(targetEntityType,
                                                                                           protectionType, CallbackLifeCycle.PostPersist);
            out.println("Callback events expected:");
            for (CallbackRecord cr : expectedEventList) {
                out.println(cr.toString());
            }

            out.println("Comparing expected vs actual event lists...");
            Assert.assertEquals("Verify event lists are same size.", expectedEventList.size(), eventList.size());
            if (expectedEventList.size() == eventList.size()) {
                for (int index = 0; index < expectedEventList.size(); index++) {
                    CallbackRecord expectedCR = expectedEventList.get(index);
                    CallbackRecord actualCR = eventList.get(index);

                    Assert.assertTrue("Comparing CallbackRecord at index " + index,
                                      ((expectedCR.getCallerClassName().equals(actualCR.getCallerClassName())) &&
                                       (expectedCR.getCallerMethodName().equals(actualCR.getCallerMethodName()))));
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
     * - Find Entity
     * - Verify Callback ordering matches expectations.
     * - Rollback Transaction
     *
     */
    private void testPostLoadLifecycle(
                                       Class targetEntityType,
                                       EntityManager em,
                                       UserTransaction tx,
                                       ProtectionType protectionType,
                                       final int id) throws Exception {
        out.println("Testing @PostLoad Order of Invocation behavior...");
        resetListeners();
        AbstractCallbackListener.setGlobalCallbackEventFilter(CallbackLifeCycle.PostLoad);
        AbstractCallbackListener.setGlobalCallbackProtectionTypeFilter(protectionType);

        // Roll back any active transaction
        cleanup(em, tx);

        // Clear persistence context
        out.println("Clearing persistence context...");
        em.clear();

        try {
            // 1) Find Callback Entity
            out.println("1) Find Callback Entity");

            // Begin new transaction
            beginTx(em, tx);

            out.println("Performing find operation on " + targetEntityType + " ...");
            @SuppressWarnings("unused")
            ICallbackEntity entity = (ICallbackEntity) em.find(targetEntityType, id);

            // @PostLoad should have fired, examine the callback invocation order
            out.println("@PostLoad should have fired, examining the callback invocation order...");
            List<CallbackRecord> eventList = AbstractCallbackListener.getGlobalCallbackEventList();

            out.println("Callback events observed:");
            for (CallbackRecord cr : eventList) {
                out.println(cr.toString());
            }

            List<CallbackRecord> expectedEventList = generateExpectedOrderOfInvocationList(targetEntityType,
                                                                                           protectionType, CallbackLifeCycle.PostLoad);
            out.println("Callback events expected:");
            for (CallbackRecord cr : expectedEventList) {
                out.println(cr.toString());
            }

            out.println("Comparing expected vs actual event lists...");
            Assert.assertEquals("Verify event lists are same size.", expectedEventList.size(), eventList.size());
            if (expectedEventList.size() == eventList.size()) {
                for (int index = 0; index < expectedEventList.size(); index++) {
                    CallbackRecord expectedCR = expectedEventList.get(index);
                    CallbackRecord actualCR = eventList.get(index);

                    Assert.assertTrue("Comparing CallbackRecord at index " + index,
                                      ((expectedCR.getCallerClassName().equals(actualCR.getCallerClassName())) &&
                                       (expectedCR.getCallerMethodName().equals(actualCR.getCallerMethodName()))));
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
     * - Find and Dirty Entity
     * - Verify Callback ordering matches expectations.
     * - Rollback Transaction
     *
     */
    private void testPreUpdateLifecycle(
                                        Class targetEntityType,
                                        EntityManager em,
                                        UserTransaction tx,
                                        ProtectionType protectionType,
                                        final int id) throws Exception {
        out.println("Testing @PreUpdate Order of Invocation behavior...");
        resetListeners();
        AbstractCallbackListener.setGlobalCallbackEventFilter(CallbackLifeCycle.PreUpdate);
        AbstractCallbackListener.setGlobalCallbackProtectionTypeFilter(protectionType);

        // Roll back any active transaction
        cleanup(em, tx);

        // Clear persistence context
        out.println("Clearing persistence context...");
        em.clear();

        try {
            // 1) Find Callback Entity
            out.println("1) Find Callback Entity");

            // Begin new transaction
            beginTx(em, tx);

            out.println("Performing find operation on " + targetEntityType + " ...");
            ICallbackEntity entity = (ICallbackEntity) em.find(targetEntityType, id);
            Assert.assertNotNull("Assert find() did not return null.", entity);

            out.println("2) Dirty the entity....");
            entity.setName("Dirty Name");

            out.println("3) Committing transaction...");
            commitTx(em, tx);

            // @PreUpdate should have fired, examine the callback invocation order
            out.println("@PreUpdate should have fired, examining the callback invocation order...");
            List<CallbackRecord> eventList = AbstractCallbackListener.getGlobalCallbackEventList();

            out.println("Callback events observed:");
            for (CallbackRecord cr : eventList) {
                out.println(cr.toString());
            }

            List<CallbackRecord> expectedEventList = generateExpectedOrderOfInvocationList(targetEntityType,
                                                                                           protectionType, CallbackLifeCycle.PreUpdate);
            out.println("Callback events expected:");
            for (CallbackRecord cr : expectedEventList) {
                out.println(cr.toString());
            }

            out.println("Comparing expected vs actual event lists...");
            Assert.assertEquals("Verify event lists are same size.", expectedEventList.size(), eventList.size());
            if (expectedEventList.size() == eventList.size()) {
                for (int index = 0; index < expectedEventList.size(); index++) {
                    CallbackRecord expectedCR = expectedEventList.get(index);
                    CallbackRecord actualCR = eventList.get(index);

                    Assert.assertTrue("Comparing CallbackRecord at index " + index,
                                      ((expectedCR.getCallerClassName().equals(actualCR.getCallerClassName())) &&
                                       (expectedCR.getCallerMethodName().equals(actualCR.getCallerMethodName()))));
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
     * - Find and Dirty Entity
     * - Verify Callback ordering matches expectations.
     * - Rollback Transaction
     *
     */
    private void testPostUpdateLifecycle(
                                         Class targetEntityType,
                                         EntityManager em,
                                         UserTransaction tx,
                                         ProtectionType protectionType,
                                         final int id) throws Exception {
        out.println("Testing @PostUpdate Order of Invocation behavior...");
        resetListeners();
        AbstractCallbackListener.setGlobalCallbackEventFilter(CallbackLifeCycle.PostUpdate);
        AbstractCallbackListener.setGlobalCallbackProtectionTypeFilter(protectionType);

        // Roll back any active transaction
        cleanup(em, tx);

        // Clear persistence context
        out.println("Clearing persistence context...");
        em.clear();

        try {
            // 1) Find Callback Entity
            out.println("1) Find Callback Entity");

            // Begin new transaction
            beginTx(em, tx);

            out.println("Performing find operation on " + targetEntityType + " ...");
            ICallbackEntity entity = (ICallbackEntity) em.find(targetEntityType, id);
            Assert.assertNotNull("Assert find() did not return null.", entity);

            out.println("2) Dirty the entity....");
            entity.setName("Another Dirty Name");

            out.println("3) Committing transaction...");
            commitTx(em, tx);

            // @PreUpdate should have fired, examine the callback invocation order
            out.println("@PostUpdate should have fired, examining the callback invocation order...");
            List<CallbackRecord> eventList = AbstractCallbackListener.getGlobalCallbackEventList();

            out.println("Callback events observed:");
            for (CallbackRecord cr : eventList) {
                out.println(cr.toString());
            }

            List<CallbackRecord> expectedEventList = generateExpectedOrderOfInvocationList(targetEntityType,
                                                                                           protectionType, CallbackLifeCycle.PostUpdate);
            out.println("Callback events expected:");
            for (CallbackRecord cr : expectedEventList) {
                out.println(cr.toString());
            }

            out.println("Comparing expected vs actual event lists...");
            Assert.assertEquals("Verify event lists are same size.", expectedEventList.size(), eventList.size());
            if (expectedEventList.size() == eventList.size()) {
                for (int index = 0; index < expectedEventList.size(); index++) {
                    CallbackRecord expectedCR = expectedEventList.get(index);
                    CallbackRecord actualCR = eventList.get(index);

                    Assert.assertTrue("Comparing CallbackRecord at index " + index,
                                      ((expectedCR.getCallerClassName().equals(actualCR.getCallerClassName())) &&
                                       (expectedCR.getCallerMethodName().equals(actualCR.getCallerMethodName()))));
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
     * - Find and mark entity for removal
     * - Verify Callback ordering matches expectations.
     * - Rollback Transaction
     *
     */
    private void testPreRemoveLifecycle(
                                        Class targetEntityType,
                                        EntityManager em,
                                        UserTransaction tx,
                                        ProtectionType protectionType,
                                        final int id) throws Exception {
        out.println("Testing @PreRemove Order of Invocation behavior...");
        resetListeners();
        AbstractCallbackListener.setGlobalCallbackEventFilter(CallbackLifeCycle.PreRemove);
        AbstractCallbackListener.setGlobalCallbackProtectionTypeFilter(protectionType);

        // Roll back any active transaction
        cleanup(em, tx);

        // Clear persistence context
        out.println("Clearing persistence context...");
        em.clear();

        try {
            // 1) Find Callback Entity
            out.println("1) Find Callback Entity");

            // Begin new transaction
            beginTx(em, tx);

            out.println("Performing find operation on " + targetEntityType + " ...");
            ICallbackEntity entity = (ICallbackEntity) em.find(targetEntityType, id);
            Assert.assertNotNull("Assert find() did not return null.", entity);

            out.println("2) Removing the entity....");
            em.remove(entity);

            // @PreUpdate should have fired, examine the callback invocation order
            out.println("@PreRemove should have fired, examining the callback invocation order...");
            List<CallbackRecord> eventList = AbstractCallbackListener.getGlobalCallbackEventList();

            out.println("Callback events observed:");
            for (CallbackRecord cr : eventList) {
                out.println(cr.toString());
            }

            List<CallbackRecord> expectedEventList = generateExpectedOrderOfInvocationList(targetEntityType,
                                                                                           protectionType, CallbackLifeCycle.PreRemove);
            out.println("Callback events expected:");
            for (CallbackRecord cr : expectedEventList) {
                out.println(cr.toString());
            }

            out.println("Comparing expected vs actual event lists...");
            Assert.assertEquals("Verify event lists are same size.", expectedEventList.size(), eventList.size());
            if (expectedEventList.size() == eventList.size()) {
                for (int index = 0; index < expectedEventList.size(); index++) {
                    CallbackRecord expectedCR = expectedEventList.get(index);
                    CallbackRecord actualCR = eventList.get(index);

                    Assert.assertTrue("Comparing CallbackRecord at index " + index,
                                      ((expectedCR.getCallerClassName().equals(actualCR.getCallerClassName())) &&
                                       (expectedCR.getCallerMethodName().equals(actualCR.getCallerMethodName()))));
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
     * - Find and mark entity for removal
     * - Commit Transaction
     * - Verify Callback ordering matches expectations.
     *
     */
    private void testPostRemoveLifecycle(
                                         Class targetEntityType,
                                         EntityManager em,
                                         UserTransaction tx,
                                         ProtectionType protectionType,
                                         final int id) throws Exception {
        out.println("Testing @PostRemove Order of Invocation behavior...");
        resetListeners();
        AbstractCallbackListener.setGlobalCallbackEventFilter(CallbackLifeCycle.PostRemove);
        AbstractCallbackListener.setGlobalCallbackProtectionTypeFilter(protectionType);

        // Roll back any active transaction
        cleanup(em, tx);

        // Clear persistence context
        out.println("Clearing persistence context...");
        em.clear();

        try {
            // 1) Find Callback Entity
            out.println("1) Find Callback Entity");

            // Begin new transaction
            out.println("Beginning new transaction...");
            beginTx(em, tx);

            out.println("Performing find operation on " + targetEntityType + " ...");
            ICallbackEntity entity = (ICallbackEntity) em.find(targetEntityType, id);
            Assert.assertNotNull("Assert find() did not return null.", entity);

            out.println("2) Removing the entity....");
            em.remove(entity);

            out.println("3) Committing transaction...");
            commitTx(em, tx);

            // @PreUpdate should have fired, examine the callback invocation order
            out.println("@PostRemove should have fired, examining the callback invocation order...");
            List<CallbackRecord> eventList = AbstractCallbackListener.getGlobalCallbackEventList();

            out.println("Callback events observed:");
            for (CallbackRecord cr : eventList) {
                out.println(cr.toString());
            }

            List<CallbackRecord> expectedEventList = generateExpectedOrderOfInvocationList(targetEntityType,
                                                                                           protectionType, CallbackLifeCycle.PostRemove);
            out.println("Callback events expected:");
            for (CallbackRecord cr : expectedEventList) {
                out.println(cr.toString());
            }

            out.println("Comparing expected vs actual event lists...");
            Assert.assertEquals("Verify event lists are same size.", expectedEventList.size(), eventList.size());
            if (expectedEventList.size() == eventList.size()) {
                for (int index = 0; index < expectedEventList.size(); index++) {
                    CallbackRecord expectedCR = expectedEventList.get(index);
                    CallbackRecord actualCR = eventList.get(index);

                    Assert.assertTrue("Comparing CallbackRecord at index " + index,
                                      ((expectedCR.getCallerClassName().equals(actualCR.getCallerClassName())) &&
                                       (expectedCR.getCallerMethodName().equals(actualCR.getCallerMethodName()))));
                }
            }
        } finally {
            // Cleanup
            resetListeners();
            cleanup(em, tx);
        }
    }

    private void resetListeners() {
        // Reset Global Event List
        AbstractCallbackListener.resetGlobalCallbackEventList();

        // Default Listeners
        DefaultCallbackListenerPackageG1.getSingleton().resetCallbackData();
        DefaultCallbackListenerPackageG2.getSingleton().resetCallbackData();
        DefaultCallbackListenerPrivateG1.getSingleton().resetCallbackData();
        DefaultCallbackListenerPrivateG2.getSingleton().resetCallbackData();
        DefaultCallbackListenerProtectedG1.getSingleton().resetCallbackData();
        DefaultCallbackListenerProtectedG2.getSingleton().resetCallbackData();
        DefaultCallbackListenerPublicG1.getSingleton().resetCallbackData();
        DefaultCallbackListenerPublicG2.getSingleton().resetCallbackData();

        // Entity Listeners
        AnoCallbackListenerPackageA1.getSingleton().resetCallbackData();
        AnoCallbackListenerPackageA2.getSingleton().resetCallbackData();
        AnoCallbackListenerPackageB1.getSingleton().resetCallbackData();
        AnoCallbackListenerPackageB2.getSingleton().resetCallbackData();
        AnoCallbackListenerPackageC1.getSingleton().resetCallbackData();
        AnoCallbackListenerPackageC2.getSingleton().resetCallbackData();
    }

    private List<CallbackRecord> generateExpectedOrderOfInvocationList(Class targetEntityType,
                                                                       ProtectionType protectionType,
                                                                       CallbackLifeCycle lifecycleType) {
        final String basePackageName = "jpa10callback.";

        String entType = (targetEntityType.toString().toLowerCase().contains("ano")) ? "Ano" : "XML";

        String protType = "";
        switch (protectionType) {
            case PT_PACKAGE:
                protType = "Package";
                break;
            case PT_PRIVATE:
                protType = "Private";
                break;
            case PT_PROTECTED:
                protType = "Protected";
                break;
            case PT_PUBLIC:
                protType = "Public";
                break;
        }

        ArrayList<CallbackRecord> expectedEventList = new ArrayList<CallbackRecord>();

        // DefaultCallbackListener[ProtType]G1
        {
            String callerClassName = basePackageName + "listeners.orderofinvocation.defaultlistener.g1." +
                                     "DefaultCallbackListener" + protType + "G1";
            String callerMethodName = "";
            switch (lifecycleType) {
                case PrePersist:
                    callerMethodName = "prePersist";
                    break;
                case PostPersist:
                    callerMethodName = "postPersist";
                    break;
                case PreRemove:
                    callerMethodName = "preRemove";
                    break;
                case PostRemove:
                    callerMethodName = "postRemove";
                    break;
                case PreUpdate:
                    callerMethodName = "preUpdate";
                    break;
                case PostUpdate:
                    callerMethodName = "postUpdate";
                    break;
                case PostLoad:
                    callerMethodName = "postLoad";
                    break;
            }
            CallbackRecord cr = new CallbackRecord(lifecycleType, callerClassName, callerMethodName);
            expectedEventList.add(cr);
        }

        // DefaultCallbackListener[ProtType]G2
        {
            String callerClassName = basePackageName + "listeners.orderofinvocation.defaultlistener.g2." +
                                     "DefaultCallbackListener" + protType + "G2";
            String callerMethodName = "";
            switch (lifecycleType) {
                case PrePersist:
                    callerMethodName = "prePersist";
                    break;
                case PostPersist:
                    callerMethodName = "postPersist";
                    break;
                case PreRemove:
                    callerMethodName = "preRemove";
                    break;
                case PostRemove:
                    callerMethodName = "postRemove";
                    break;
                case PreUpdate:
                    callerMethodName = "preUpdate";
                    break;
                case PostUpdate:
                    callerMethodName = "postUpdate";
                    break;
                case PostLoad:
                    callerMethodName = "postLoad";
                    break;
            }
            CallbackRecord cr = new CallbackRecord(lifecycleType, callerClassName, callerMethodName);
            expectedEventList.add(cr);
        }

        // [EntType]CallbackListener[ProtType]A1
        {
            String callerClassName = basePackageName + "listeners.orderofinvocation.a1." +
                                     entType + "CallbackListener" + protType + "A1";
            String callerMethodName = "";
            switch (lifecycleType) {
                case PrePersist:
                    callerMethodName = "prePersistCallback";
                    break;
                case PostPersist:
                    callerMethodName = "postPersistCallback";
                    break;
                case PreRemove:
                    callerMethodName = "preRemoveCallback";
                    break;
                case PostRemove:
                    callerMethodName = "postRemoveCallback";
                    break;
                case PreUpdate:
                    callerMethodName = "preUpdateCallback";
                    break;
                case PostUpdate:
                    callerMethodName = "postUpdateCallback";
                    break;
                case PostLoad:
                    callerMethodName = "postLoadCallback";
                    break;
            }
            CallbackRecord cr = new CallbackRecord(lifecycleType, callerClassName, callerMethodName);
            expectedEventList.add(cr);
        }

        // [EntType]CallbackListener[ProtType]A2
        {
            String callerClassName = basePackageName + "listeners.orderofinvocation.a2." +
                                     entType + "CallbackListener" + protType + "A2";
            String callerMethodName = "";
            switch (lifecycleType) {
                case PrePersist:
                    callerMethodName = "prePersistCallback";
                    break;
                case PostPersist:
                    callerMethodName = "postPersistCallback";
                    break;
                case PreRemove:
                    callerMethodName = "preRemoveCallback";
                    break;
                case PostRemove:
                    callerMethodName = "postRemoveCallback";
                    break;
                case PreUpdate:
                    callerMethodName = "preUpdateCallback";
                    break;
                case PostUpdate:
                    callerMethodName = "postUpdateCallback";
                    break;
                case PostLoad:
                    callerMethodName = "postLoadCallback";
                    break;
            }
            CallbackRecord cr = new CallbackRecord(lifecycleType, callerClassName, callerMethodName);
            expectedEventList.add(cr);
        }

        // [EntType]CallbackListener[ProtType]B1
        {
            String callerClassName = basePackageName + "listeners.orderofinvocation.b1." +
                                     entType + "CallbackListener" + protType + "B1";
            String callerMethodName = "";
            switch (lifecycleType) {
                case PrePersist:
                    callerMethodName = "prePersistCallback";
                    break;
                case PostPersist:
                    callerMethodName = "postPersistCallback";
                    break;
                case PreRemove:
                    callerMethodName = "preRemoveCallback";
                    break;
                case PostRemove:
                    callerMethodName = "postRemoveCallback";
                    break;
                case PreUpdate:
                    callerMethodName = "preUpdateCallback";
                    break;
                case PostUpdate:
                    callerMethodName = "postUpdateCallback";
                    break;
                case PostLoad:
                    callerMethodName = "postLoadCallback";
                    break;
            }
            CallbackRecord cr = new CallbackRecord(lifecycleType, callerClassName, callerMethodName);
            expectedEventList.add(cr);
        }

        // [EntType]CallbackListener[ProtType]B2
        {
            String callerClassName = basePackageName + "listeners.orderofinvocation.b2." +
                                     entType + "CallbackListener" + protType + "B2";
            String callerMethodName = "";
            switch (lifecycleType) {
                case PrePersist:
                    callerMethodName = "prePersistCallback";
                    break;
                case PostPersist:
                    callerMethodName = "postPersistCallback";
                    break;
                case PreRemove:
                    callerMethodName = "preRemoveCallback";
                    break;
                case PostRemove:
                    callerMethodName = "postRemoveCallback";
                    break;
                case PreUpdate:
                    callerMethodName = "preUpdateCallback";
                    break;
                case PostUpdate:
                    callerMethodName = "postUpdateCallback";
                    break;
                case PostLoad:
                    callerMethodName = "postLoadCallback";
                    break;
            }
            CallbackRecord cr = new CallbackRecord(lifecycleType, callerClassName, callerMethodName);
            expectedEventList.add(cr);
        }

        // [EntType]CallbackListener[ProtType]C1
        {
            String callerClassName = basePackageName + "listeners.orderofinvocation.c1." +
                                     entType + "CallbackListener" + protType + "C1";
            String callerMethodName = "";
            switch (lifecycleType) {
                case PrePersist:
                    callerMethodName = "prePersistCallback";
                    break;
                case PostPersist:
                    callerMethodName = "postPersistCallback";
                    break;
                case PreRemove:
                    callerMethodName = "preRemoveCallback";
                    break;
                case PostRemove:
                    callerMethodName = "postRemoveCallback";
                    break;
                case PreUpdate:
                    callerMethodName = "preUpdateCallback";
                    break;
                case PostUpdate:
                    callerMethodName = "postUpdateCallback";
                    break;
                case PostLoad:
                    callerMethodName = "postLoadCallback";
                    break;
            }
            CallbackRecord cr = new CallbackRecord(lifecycleType, callerClassName, callerMethodName);
            expectedEventList.add(cr);
        }

        // [EntType]CallbackListener[ProtType]C2
        {
            String callerClassName = basePackageName + "listeners.orderofinvocation.c2." +
                                     entType + "CallbackListener" + protType + "C2";
            String callerMethodName = "";
            switch (lifecycleType) {
                case PrePersist:
                    callerMethodName = "prePersistCallback";
                    break;
                case PostPersist:
                    callerMethodName = "postPersistCallback";
                    break;
                case PreRemove:
                    callerMethodName = "preRemoveCallback";
                    break;
                case PostRemove:
                    callerMethodName = "postRemoveCallback";
                    break;
                case PreUpdate:
                    callerMethodName = "preUpdateCallback";
                    break;
                case PostUpdate:
                    callerMethodName = "postUpdateCallback";
                    break;
                case PostLoad:
                    callerMethodName = "postLoadCallback";
                    break;
            }
            CallbackRecord cr = new CallbackRecord(lifecycleType, callerClassName, callerMethodName);
            expectedEventList.add(cr);
        }

        // [EntType]OOIRoot[ProtType]Entity
        {
            String callerClassName = basePackageName + "entity.orderofinvocation." +
                                     entType.toLowerCase() + "." +
                                     entType + "OOIRoot" + protType + "Entity";
            String callerMethodName = "";
            switch (lifecycleType) {
                case PrePersist:
                    callerMethodName = "entityAPrePersist";
                    break;
                case PostPersist:
                    callerMethodName = "entityAPostPersist";
                    break;
                case PreRemove:
                    callerMethodName = "entityAPreRemove";
                    break;
                case PostRemove:
                    callerMethodName = "entityAPostRemove";
                    break;
                case PreUpdate:
                    callerMethodName = "entityAPreUpdate";
                    break;
                case PostUpdate:
                    callerMethodName = "entityAPostUpdate";
                    break;
                case PostLoad:
                    callerMethodName = "entityAPostLoad";
                    break;
            }
            CallbackRecord cr = new CallbackRecord(lifecycleType, callerClassName, callerMethodName);
            expectedEventList.add(cr);
        }

        // [EntType]OOIMSC[ProtType]Entity
        {
            String callerClassName = basePackageName + "entity.orderofinvocation." +
                                     entType.toLowerCase() + "." +
                                     entType + "OOI" + protType + "MSC";
            String callerMethodName = "";
            switch (lifecycleType) {
                case PrePersist:
                    callerMethodName = "entityBPrePersist";
                    break;
                case PostPersist:
                    callerMethodName = "entityBPostPersist";
                    break;
                case PreRemove:
                    callerMethodName = "entityBPreRemove";
                    break;
                case PostRemove:
                    callerMethodName = "entityBPostRemove";
                    break;
                case PreUpdate:
                    callerMethodName = "entityBPreUpdate";
                    break;
                case PostUpdate:
                    callerMethodName = "entityBPostUpdate";
                    break;
                case PostLoad:
                    callerMethodName = "entityBPostLoad";
                    break;
            }
            CallbackRecord cr = new CallbackRecord(lifecycleType, callerClassName, callerMethodName);
            expectedEventList.add(cr);
        }

        // [EntType]OOILeaf[ProtType]Entity
        {
            String callerClassName = basePackageName + "entity.orderofinvocation." +
                                     entType.toLowerCase() + "." +
                                     entType + "OOILeaf" + protType + "Entity";
            String callerMethodName = "";
            switch (lifecycleType) {
                case PrePersist:
                    callerMethodName = "entityCPrePersist";
                    break;
                case PostPersist:
                    callerMethodName = "entityCPostPersist";
                    break;
                case PreRemove:
                    callerMethodName = "entityCPreRemove";
                    break;
                case PostRemove:
                    callerMethodName = "entityCPostRemove";
                    break;
                case PreUpdate:
                    callerMethodName = "entityCPreUpdate";
                    break;
                case PostUpdate:
                    callerMethodName = "entityCPostUpdate";
                    break;
                case PostLoad:
                    callerMethodName = "entityCPostLoad";
                    break;
            }
            CallbackRecord cr = new CallbackRecord(lifecycleType, callerClassName, callerMethodName);
            expectedEventList.add(cr);
        }

        return expectedEventList;
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

    private void beginTx(EntityManager em, UserTransaction tx) throws SystemException, NotSupportedException {
        out.println("Beginning new transaction...");
        if (tx == null) {
            em.getTransaction().begin();
        } else {
            tx.begin();
            em.joinTransaction();
        }
    }

    private void commitTx(EntityManager em,
                          UserTransaction tx) throws SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException, SystemException {
        out.println("Committing transaction...");
        if (tx == null) {
            em.getTransaction().commit();
        } else {
            tx.commit();
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
