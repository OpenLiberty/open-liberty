/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.callback.testlogic;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.callback.AbstractCallbackListener;
import com.ibm.ws.jpa.fvt.callback.AbstractCallbackListener.ProtectionType;
import com.ibm.ws.jpa.fvt.callback.CallbackRecord;
import com.ibm.ws.jpa.fvt.callback.CallbackRecord.CallbackLifeCycle;
import com.ibm.ws.jpa.fvt.callback.entities.ICallbackEntity;
import com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.a1.AnoCallbackListenerPackageA1;
import com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.a2.AnoCallbackListenerPackageA2;
import com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.b1.AnoCallbackListenerPackageB1;
import com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.b2.AnoCallbackListenerPackageB2;
import com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.c1.AnoCallbackListenerPackageC1;
import com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.c2.AnoCallbackListenerPackageC2;
import com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.defaultlistener.g1.DefaultCallbackListenerPackageG1;
import com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.defaultlistener.g1.DefaultCallbackListenerPrivateG1;
import com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.defaultlistener.g1.DefaultCallbackListenerProtectedG1;
import com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.defaultlistener.g1.DefaultCallbackListenerPublicG1;
import com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.defaultlistener.g2.DefaultCallbackListenerPackageG2;
import com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.defaultlistener.g2.DefaultCallbackListenerPrivateG2;
import com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.defaultlistener.g2.DefaultCallbackListenerProtectedG2;
import com.ibm.ws.jpa.fvt.callback.listeners.orderofinvocation.defaultlistener.g2.DefaultCallbackListenerPublicG2;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class CallbackOrderOfInvocationTestLogic extends AbstractTestLogic {

    /**
     * Verify that the order of invocation of callback methods, as defined by the JPA Specification
     * section 3.5.4, is demonstrated:
     * <p>
     * Default Listener, invoked in the order they are defined in the XML Mapping File
     * Entity Listeners defined by the EntityListener annotation on an Entity class or Mapped Superclass (in the order of appearance in the annotation).
     * With inheritance, the order of invocation starts at the highest superclass defining an EntityListener, moving down to the leaf entity class.
     * Lifecycle methods defined by entity classes and mapped superclasses are invoked in the order from highest superclass to the leaf entity class
     * <p>
     * To verify this, the test will execute in the following environment:
     * <p><ul>
     * <li>Default Entity Listener: DefaultListener1 and DefaultListener2, defined in that order in the XML Mapping File
     * <li>Abstract Entity using Table-Per-Class inheritance methodology, with the following: EntityListenerA1, EntityListenerA2, defined in that order
     * <li>Callback methods for each lifecycle type (A_PrePersist, A_PostPersist, etc.)
     * <li>Mapped Superclass with the following: EntityListenerB1, EntityListenerB2, defined in that order
     * <li>Callback methods for each lifecycle type (B_PrePersist, B_PostPersist, etc.)
     * <li>Leaf entity with the following: EntityListenerC1, EntityListenerC2, defined in that order
     * <li>Callback methods for each lifecycle type (C_PrePersist, C_PostPersist, etc.)
     * </ul><p>
     *
     * For each callback type, the following invocation order is expected:
     * <p><ul>
     * <li>DefaultCallbackListener[ProtType]G1
     * <li>DefaultCallbackListener[ProtType]G2
     * <li>[EntType]CallbackListener[ProtType]A1
     * <li>[EntType]CallbackListener[ProtType]A2
     * <li>[EntType]CallbackListener[ProtType]B1
     * <li>[EntType]CallbackListener[ProtType]B2
     * <li>[EntType]CallbackListener[ProtType]C1
     * <li>[EntType]CallbackListener[ProtType]C2
     * <li>[EntType]OOIRoot[ProtType]Entity
     * <li>[EntType]OOIMSC[ProtType]Entity
     * <li>[EntType]OOILeaf[ProtType]Entity
     * </ul><p>
     * Where [ProtType] = Package|Private|Protected|Public
     * Where [EntType] = Ano|XML
     * <p>
     * Points: 88
     */
    public void testOrderOfInvocation001(
                                         TestExecutionContext testExecCtx,
                                         TestExecutionResources testExecResources,
                                         Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testOrderOfInvocation001: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityName = (String) testExecCtx.getProperties().get("EntityName");
        CallbackOOIEntityEnum targetEntityType = CallbackOOIEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid entity type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        // Fetch method protection type for the listener to test
        String protectionType = (String) testExecCtx.getProperties().get("ListenerMethodProtectionType");
        ProtectionType listenerProtectionType = ProtectionType.valueOf(protectionType);
        if (listenerProtectionType == null) {
            // Unknown protection type
            Assert.fail("Invalid listener protection type specified ('" + protectionType + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("CallbackTestLogic.testOrderOfInvocation001(): Begin");
            AbstractCallbackListener.setTargetPostLoadLifeCycleWithRuntimeException(null);

            testPrePersistLifecycle(targetEntityType, jpaResource, listenerProtectionType); // 12 points
            testPostPersistLifecycle(targetEntityType, jpaResource, listenerProtectionType); // 12 points
            testPostLoadLifecycle(targetEntityType, jpaResource, listenerProtectionType); // 12 points
            testPreUpdateLifecycle(targetEntityType, jpaResource, listenerProtectionType); // 13 points
            testPostUpdateLifecycle(targetEntityType, jpaResource, listenerProtectionType); // 13 points
            testPreRemoveLifecycle(targetEntityType, jpaResource, listenerProtectionType); // 13 points
            testPostRemoveLifecycle(targetEntityType, jpaResource, listenerProtectionType); // 13 points

            System.out.println("Ending test.");
        } finally {
            System.out.println("CallbackTestLogic.testOrderOfInvocation001(): End");
            AbstractCallbackListener.resetGlobalCallbackEventList();
        }
    }

    /**
     * Test Strategy:
     * <p><ul>
     * <li>Start Transaction
     * <li>Create Unpersisted Callback Entity
     * <li>Verify Callback ordering matches expectations.
     * </ul><p>
     * Sub-Points: 12
     */
    private void testPrePersistLifecycle(
                                         CallbackOOIEntityEnum targetEntityType,
                                         JPAResource jpaResource,
                                         ProtectionType protectionType) throws Throwable {
        System.out.println("Testing @PrePersist Order of Invocation behavior...");

        AbstractCallbackListener.setGlobalCallbackEventFilter(CallbackLifeCycle.PrePersist);
        AbstractCallbackListener.setGlobalCallbackProtectionTypeFilter(protectionType);

        // Roll back any active transaction
        if (jpaResource.getTj().isTransactionActive()) {
            jpaResource.getTj().rollbackTransaction();
        }

        // Clear persistence context
        System.out.println("Clearing persistence context...");
        jpaResource.getEm().clear();

        int id = 1;

        try {
            // 1) Create Unpersisted Callback Entity
            System.out.println("1) Create Unpersisted Callback Entity");

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + "...");
            ICallbackEntity entity_persist = (ICallbackEntity) constructNewEntityObject(targetEntityType);
            entity_persist.setId(id);
            entity_persist.setName("PrePersist-CallbackEntity-" + id);

            System.out.println("3) Calling em.persist on " + targetEntityType.getEntityName() + "(id=" + id + ") ...");
            jpaResource.getEm().persist(entity_persist);

            // @PrePersist should have fired, examine the callback invocation order
            System.out.println("@PrePersist should have fired, examining the callback invocation order...");
            List<CallbackRecord> eventList = AbstractCallbackListener.getGlobalCallbackEventList();

            System.out.println("Callback events observed:");
            for (CallbackRecord cr : eventList) {
                System.out.println(cr.toString());
            }

            List<CallbackRecord> expectedEventList = generateExpectedOrderOfInvocationList(targetEntityType,
                                                                                           protectionType, CallbackLifeCycle.PrePersist);
            System.out.println("Callback events expected:");
            for (CallbackRecord cr : expectedEventList) {
                System.out.println(cr.toString());
            }

            System.out.println("Comparing expected vs actual event lists...");
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

            if (jpaResource.getTj().isTransactionActive()) {
                System.out.println("Rolling back transaction...");
                jpaResource.getTj().rollbackTransaction();
            }
        }
    }

    /**
     * Test Strategy:
     * <p><ul>
     * <li>Start Transaction
     * <li>Create Unpersisted Callback Entity
     * <li>Commit Transaction
     * <li>Verify Callback ordering matches expectations.
     * </ul><p>
     * Sub-Points: 12
     */
    private void testPostPersistLifecycle(
                                          CallbackOOIEntityEnum targetEntityType,
                                          JPAResource jpaResource,
                                          ProtectionType protectionType) throws Throwable {
        System.out.println("Testing @PostPersist Order of Invocation behavior...");

        AbstractCallbackListener.setGlobalCallbackEventFilter(CallbackLifeCycle.PostPersist);
        AbstractCallbackListener.setGlobalCallbackProtectionTypeFilter(protectionType);

        // Roll back any active transaction
        if (jpaResource.getTj().isTransactionActive()) {
            jpaResource.getTj().rollbackTransaction();
        }

        // Clear persistence context
        System.out.println("Clearing persistence context...");
        jpaResource.getEm().clear();

        int id = 2;

        try {
            // 1) Create Unpersisted Callback Entity
            System.out.println("1) Create Unpersisted Callback Entity");

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + "...");
            ICallbackEntity entity_persist = (ICallbackEntity) constructNewEntityObject(targetEntityType);
            entity_persist.setId(id);
            entity_persist.setName("PostPersist-CallbackEntity-" + id);

            System.out.println("3) Calling em.persist on " + targetEntityType.getEntityName() + "(id=" + id + ") ...");
            jpaResource.getEm().persist(entity_persist);

            System.out.println("Committing transaction...");
            if (jpaResource.getTj().isTransactionActive()) {
                jpaResource.getTj().commitTransaction();
            }

            // @PrePersist should have fired, examine the callback invocation order
            System.out.println("@PostPersist should have fired, examining the callback invocation order...");
            List<CallbackRecord> eventList = AbstractCallbackListener.getGlobalCallbackEventList();

            System.out.println("Callback events observed:");
            for (CallbackRecord cr : eventList) {
                System.out.println(cr.toString());
            }

            List<CallbackRecord> expectedEventList = generateExpectedOrderOfInvocationList(targetEntityType,
                                                                                           protectionType, CallbackLifeCycle.PostPersist);
            System.out.println("Callback events expected:");
            for (CallbackRecord cr : expectedEventList) {
                System.out.println(cr.toString());
            }

            System.out.println("Comparing expected vs actual event lists...");
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

            if (jpaResource.getTj().isTransactionActive()) {
                System.out.println("Rolling back transaction...");
                jpaResource.getTj().rollbackTransaction();
            }
        }
    }

    /**
     * Test Strategy:
     * <p><ul>
     * <li>Start Transaction
     * <li>Find Entity
     * <li>Verify Callback ordering matches expectations.
     * <li>Rollback Transaction
     * </ul><p>
     * Sub-Points: 12
     */
    private void testPostLoadLifecycle(
                                       CallbackOOIEntityEnum targetEntityType,
                                       JPAResource jpaResource,
                                       ProtectionType protectionType) throws Throwable {
        System.out.println("Testing @PostLoad Order of Invocation behavior...");

        AbstractCallbackListener.setGlobalCallbackEventFilter(CallbackLifeCycle.PostLoad);
        AbstractCallbackListener.setGlobalCallbackProtectionTypeFilter(protectionType);

        // Roll back any active transaction
        if (jpaResource.getTj().isTransactionActive()) {
            jpaResource.getTj().rollbackTransaction();
        }

        // Clear persistence context
        System.out.println("Clearing persistence context...");
        jpaResource.getEm().clear();

        int id = 2;

        try {
            // 1) Find Callback Entity
            System.out.println("1) Find Callback Entity");

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Performing find operation on " + targetEntityType.getEntityName() + "(id=" + id + ") ...");
            ICallbackEntity entity = (ICallbackEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            Assert.assertNotNull("Assert find() did not return null.", entity);

            // @PostLoad should have fired, examine the callback invocation order
            System.out.println("@PostLoad should have fired, examining the callback invocation order...");
            List<CallbackRecord> eventList = AbstractCallbackListener.getGlobalCallbackEventList();

            System.out.println("Callback events observed:");
            for (CallbackRecord cr : eventList) {
                System.out.println(cr.toString());
            }

            List<CallbackRecord> expectedEventList = generateExpectedOrderOfInvocationList(targetEntityType,
                                                                                           protectionType, CallbackLifeCycle.PostLoad);
            System.out.println("Callback events expected:");
            for (CallbackRecord cr : expectedEventList) {
                System.out.println(cr.toString());
            }

            System.out.println("Comparing expected vs actual event lists...");
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

            if (jpaResource.getTj().isTransactionActive()) {
                System.out.println("Rolling back transaction...");
                jpaResource.getTj().rollbackTransaction();
            }
        }
    }

    /**
     * Test Strategy:
     * <p><ul>
     * <li>Start Transaction
     * <li>Find and Dirty Entity
     * <li>Verify Callback ordering matches expectations.
     * <li>Rollback Transaction
     * </ul><p>
     * Sub-Points: 13
     */
    private void testPreUpdateLifecycle(
                                        CallbackOOIEntityEnum targetEntityType,
                                        JPAResource jpaResource,
                                        ProtectionType protectionType) throws Throwable {
        System.out.println("Testing @PreUpdate Order of Invocation behavior...");
        resetListeners();
        AbstractCallbackListener.setGlobalCallbackEventFilter(CallbackLifeCycle.PreUpdate);
        AbstractCallbackListener.setGlobalCallbackProtectionTypeFilter(protectionType);

        // Roll back any active transaction
        if (jpaResource.getTj().isTransactionActive()) {
            jpaResource.getTj().rollbackTransaction();
        }

        // Clear persistence context
        System.out.println("Clearing persistence context...");
        jpaResource.getEm().clear();

        int id = 2;

        try {
            // 1) Find Callback Entity
            System.out.println("1) Find Callback Entity");

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Performing find operation on " + targetEntityType.getEntityName() + "(id=" + id + ") ...");
            ICallbackEntity entity = (ICallbackEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            Assert.assertNotNull("Assert find() did not return null.", entity);

            System.out.println("2) Dirty the entity....");
            entity.setName("PreUpdate-CallbackEntity-" + id);

            System.out.println("3) Committing transaction...");
            if (jpaResource.getTj().isTransactionActive()) {
                jpaResource.getTj().commitTransaction();
            }

            // @PreUpdate should have fired, examine the callback invocation order
            System.out.println("@PreUpdate should have fired, examining the callback invocation order...");
            List<CallbackRecord> eventList = AbstractCallbackListener.getGlobalCallbackEventList();

            System.out.println("Callback events observed:");
            for (CallbackRecord cr : eventList) {
                System.out.println(cr.toString());
            }

            List<CallbackRecord> expectedEventList = generateExpectedOrderOfInvocationList(targetEntityType,
                                                                                           protectionType, CallbackLifeCycle.PreUpdate);
            System.out.println("Callback events expected:");
            for (CallbackRecord cr : expectedEventList) {
                System.out.println(cr.toString());
            }

            System.out.println("Comparing expected vs actual event lists...");
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

            if (jpaResource.getTj().isTransactionActive()) {
                System.out.println("Rolling back transaction...");
                jpaResource.getTj().rollbackTransaction();
            }
        }
    }

    /**
     * Test Strategy:
     * <p><ul>
     * <li>Start Transaction
     * <li>Find and Dirty Entity
     * <li>Verify Callback ordering matches expectations.
     * <li>Rollback Transaction
     * </ul><p>
     * Sub-Points: 13
     */
    private void testPostUpdateLifecycle(
                                         CallbackOOIEntityEnum targetEntityType,
                                         JPAResource jpaResource,
                                         ProtectionType protectionType) throws Throwable {
        System.out.println("Testing @PostUpdate Order of Invocation behavior...");
        resetListeners();
        AbstractCallbackListener.setGlobalCallbackEventFilter(CallbackLifeCycle.PostUpdate);
        AbstractCallbackListener.setGlobalCallbackProtectionTypeFilter(protectionType);

        // Roll back any active transaction
        if (jpaResource.getTj().isTransactionActive()) {
            jpaResource.getTj().rollbackTransaction();
        }

        // Clear persistence context
        System.out.println("Clearing persistence context...");
        jpaResource.getEm().clear();

        int id = 2;

        try {
            // 1) Find Callback Entity
            System.out.println("1) Find Callback Entity");

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Performing find operation on " + targetEntityType.getEntityName() + "(id=" + id + ") ...");
            ICallbackEntity entity = (ICallbackEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            Assert.assertNotNull("Assert find() did not return null.", entity);

            System.out.println("2) Dirty the entity....");
            entity.setName("PostUpdate-CallbackEntity-" + id);

            System.out.println("3) Committing transaction...");
            if (jpaResource.getTj().isTransactionActive()) {
                jpaResource.getTj().commitTransaction();
            }

            // @PreUpdate should have fired, examine the callback invocation order
            System.out.println("@PostUpdate should have fired, examining the callback invocation order...");
            List<CallbackRecord> eventList = AbstractCallbackListener.getGlobalCallbackEventList();

            System.out.println("Callback events observed:");
            for (CallbackRecord cr : eventList) {
                System.out.println(cr.toString());
            }

            List<CallbackRecord> expectedEventList = generateExpectedOrderOfInvocationList(targetEntityType,
                                                                                           protectionType, CallbackLifeCycle.PostUpdate);
            System.out.println("Callback events expected:");
            for (CallbackRecord cr : expectedEventList) {
                System.out.println(cr.toString());
            }

            System.out.println("Comparing expected vs actual event lists...");
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

            if (jpaResource.getTj().isTransactionActive()) {
                System.out.println("Rolling back transaction...");
                jpaResource.getTj().rollbackTransaction();
            }
        }
    }

    /**
     * Test Strategy:
     * <p><ul>
     * <li>Start Transaction
     * <li>Find and mark entity for removal
     * <li>Verify Callback ordering matches expectations.
     * <li>Rollback Transaction
     * </ul><p>
     * Sub-Points: 13
     */
    private void testPreRemoveLifecycle(
                                        CallbackOOIEntityEnum targetEntityType,
                                        JPAResource jpaResource,
                                        ProtectionType protectionType) throws Throwable {
        System.out.println("Testing @PreRemove Order of Invocation behavior...");
        resetListeners();
        AbstractCallbackListener.setGlobalCallbackEventFilter(CallbackLifeCycle.PreRemove);
        AbstractCallbackListener.setGlobalCallbackProtectionTypeFilter(protectionType);

        // Roll back any active transaction
        if (jpaResource.getTj().isTransactionActive()) {
            jpaResource.getTj().rollbackTransaction();
        }

        // Clear persistence context
        System.out.println("Clearing persistence context...");
        jpaResource.getEm().clear();

        int id = 2;

        try {
            // 1) Find Callback Entity
            System.out.println("1) Find Callback Entity");

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Performing find operation on " + targetEntityType.getEntityName() + "(id=" + id + ") ...");
            ICallbackEntity entity = (ICallbackEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            Assert.assertNotNull("Assert find() did not return null.", entity);

            System.out.println("2) Removing the entity....");
            jpaResource.getEm().remove(entity);

            // @PreUpdate should have fired, examine the callback invocation order
            System.out.println("@PreRemove should have fired, examining the callback invocation order...");
            List<CallbackRecord> eventList = AbstractCallbackListener.getGlobalCallbackEventList();

            System.out.println("Callback events observed:");
            for (CallbackRecord cr : eventList) {
                System.out.println(cr.toString());
            }

            List<CallbackRecord> expectedEventList = generateExpectedOrderOfInvocationList(targetEntityType,
                                                                                           protectionType, CallbackLifeCycle.PreRemove);
            System.out.println("Callback events expected:");
            for (CallbackRecord cr : expectedEventList) {
                System.out.println(cr.toString());
            }

            System.out.println("Comparing expected vs actual event lists...");
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

            if (jpaResource.getTj().isTransactionActive()) {
                System.out.println("Rolling back transaction...");
                jpaResource.getTj().rollbackTransaction();
            }
        }
    }

    /**
     * Test Strategy:
     * <p><ul>
     * <li>Start Transaction
     * <li>Find and mark entity for removal
     * <li>Commit Transaction
     * <li>Verify Callback ordering matches expectations.
     * </ul><p>
     * Sub-Points: 13
     */
    private void testPostRemoveLifecycle(
                                         CallbackOOIEntityEnum targetEntityType,
                                         JPAResource jpaResource,
                                         ProtectionType protectionType) throws Throwable {
        System.out.println("Testing @PostRemove Order of Invocation behavior...");
        resetListeners();
        AbstractCallbackListener.setGlobalCallbackEventFilter(CallbackLifeCycle.PostRemove);
        AbstractCallbackListener.setGlobalCallbackProtectionTypeFilter(protectionType);

        // Roll back any active transaction
        if (jpaResource.getTj().isTransactionActive()) {
            jpaResource.getTj().rollbackTransaction();
        }

        // Clear persistence context
        System.out.println("Clearing persistence context...");
        jpaResource.getEm().clear();

        int id = 2;

        try {
            // 1) Find Callback Entity
            System.out.println("1) Find Callback Entity");

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Performing find operation on " + targetEntityType.getEntityName() + "(id=" + id + ") ...");
            ICallbackEntity entity = (ICallbackEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            Assert.assertNotNull("Assert find() did not return null.", entity);

            System.out.println("2) Removing the entity....");
            jpaResource.getEm().remove(entity);

            System.out.println("3) Committing transaction...");
            if (jpaResource.getTj().isTransactionActive()) {
                jpaResource.getTj().commitTransaction();
            }

            // @PreUpdate should have fired, examine the callback invocation order
            System.out.println("@PostRemove should have fired, examining the callback invocation order...");
            List<CallbackRecord> eventList = AbstractCallbackListener.getGlobalCallbackEventList();

            System.out.println("Callback events observed:");
            for (CallbackRecord cr : eventList) {
                System.out.println(cr.toString());
            }

            List<CallbackRecord> expectedEventList = generateExpectedOrderOfInvocationList(targetEntityType,
                                                                                           protectionType, CallbackLifeCycle.PostRemove);
            System.out.println("Callback events expected:");
            for (CallbackRecord cr : expectedEventList) {
                System.out.println(cr.toString());
            }

            System.out.println("Comparing expected vs actual event lists...");
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

            if (jpaResource.getTj().isTransactionActive()) {
                System.out.println("Rolling back transaction...");
                jpaResource.getTj().rollbackTransaction();
            }
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

    @SuppressWarnings("incomplete-switch")
    private List<CallbackRecord> generateExpectedOrderOfInvocationList(CallbackOOIEntityEnum targetEntityType,
                                                                       ProtectionType protectionType, CallbackLifeCycle lifecycleType) {
        final String basePackageName = "com.ibm.ws.jpa.fvt.callback.";

        String entType = (targetEntityType.toString().toLowerCase().startsWith("ano")) ? "Ano" : "XML";

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
            String callerClassName = basePackageName + "entities.orderofinvocation." +
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
            String callerClassName = basePackageName + "entities.orderofinvocation." +
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
            String callerClassName = basePackageName + "entities.orderofinvocation." +
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
}
