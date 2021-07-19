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

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.callback.AbstractCallbackListener;
import com.ibm.ws.jpa.fvt.callback.AbstractCallbackListener.ProtectionType;
import com.ibm.ws.jpa.fvt.callback.CallbackRecord.CallbackLifeCycle;
import com.ibm.ws.jpa.fvt.callback.CallbackRuntimeException;
import com.ibm.ws.jpa.fvt.callback.entities.ICallbackEntity;
import com.ibm.ws.jpa.fvt.callback.listeners.ano.AnoCallbackListenerPackage;
import com.ibm.ws.jpa.fvt.callback.listeners.ano.AnoCallbackListenerPrivate;
import com.ibm.ws.jpa.fvt.callback.listeners.ano.AnoCallbackListenerProtected;
import com.ibm.ws.jpa.fvt.callback.listeners.ano.AnoCallbackListenerPublic;
import com.ibm.ws.jpa.fvt.callback.listeners.defaultlistener.DefaultCallbackListenerPackage;
import com.ibm.ws.jpa.fvt.callback.listeners.defaultlistener.DefaultCallbackListenerPrivate;
import com.ibm.ws.jpa.fvt.callback.listeners.defaultlistener.DefaultCallbackListenerProtected;
import com.ibm.ws.jpa.fvt.callback.listeners.defaultlistener.DefaultCallbackListenerPublic;
import com.ibm.ws.jpa.fvt.callback.listeners.xml.XMLCallbackListenerProtected;
import com.ibm.ws.jpa.fvt.callback.listeners.xml.XMLCallbackListenerPublic;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class CallbackRuntimeExceptionTestLogic extends AbstractTestLogic {
    /**
     * Test when a RuntimeException is thrown by a callback method on entity classes.
     * Supports testing of entities declared by annotation and
     * XML, and supports stand-alone entity classes and entities that gain callback methods from
     * mapped superclasses.
     * <p>
     * Points: 15
     */
    public void testCallbackRuntimeException001(
                                                TestExecutionContext testExecCtx,
                                                TestExecutionResources testExecResources,
                                                Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testCallbackRuntimeException001: Missing context and/or resources.  Cannot execute the test.");
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
        CallbackEntityEnum targetEntityType = CallbackEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid entity type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("CallbackTestLogic.testCallbackRuntimeException001(): Begin");
            AbstractCallbackListener.setTargetPostLoadLifeCycleWithRuntimeException(null);

            System.out.println("Testing JPA Persist Life Cycle Methods...");
            testPrePersistLifecycle(targetEntityType, jpaResource, false, null); // 3 points
            testPostPersistLifecycle(targetEntityType, jpaResource, false, null); // 1 point

            ICallbackEntity testEntity = (ICallbackEntity) constructNewEntityObject(targetEntityType);
            testEntity.setId(3);
            testEntity.setName("Update-Remove-CallbackEntity-3");
            try {
                // Create an instance of CallbackEntity for update and remove lifecycle tests
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    jpaResource.getEm().joinTransaction();
                }

                System.out.println("Populating for Update and Remove tests...");
                jpaResource.getEm().persist(testEntity);
                jpaResource.getTj().commitTransaction();
                jpaResource.getEm().clear();

                System.out.println("Testing JPA Update Life Cycle Methods...");
                testPreUpdateLifecycle(targetEntityType, jpaResource, false, null); // 2 points
                testPostUpdateLifecycle(targetEntityType, jpaResource, false, null); // 2 points

                System.out.println("Testing JPA Remove Life Cycle Methods...");
                testPreRemoveLifecycle(targetEntityType, jpaResource, false, null); // 2 points
                testPostRemoveLifecycle(targetEntityType, jpaResource, false, null); // 2 points

                System.out.println("Testing JPA Load Life Cycle Methods...");
                testPostLoadLifecycle(targetEntityType, jpaResource, false, null); // 3 points
            } finally {
                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                System.out.println("Removing " + testEntity + " to clean up test");
                testEntity = jpaResource.getEm().merge(testEntity);
                jpaResource.getEm().remove(testEntity);
                jpaResource.getTj().commitTransaction();
                jpaResource.getEm().clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("CallbackTestLogic.testCallbackRuntimeException001(): End");
            AbstractCallbackListener.resetGlobalCallbackEventList();
        }
    }

    /**
     * Test when a RuntimeException is thrown by a callback method on default listener classes.
     * <p>
     * Points: 15
     */
    public void testCallbackRuntimeException002(
                                                TestExecutionContext testExecCtx,
                                                TestExecutionResources testExecResources,
                                                Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testCallbackRuntimeException002: Missing context and/or resources.  Cannot execute the test.");
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
        CallbackEntityEnum targetEntityType = CallbackEntityEnum.resolveEntityByName(entityName);
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
            System.out.println("CallbackTestLogic.testCallbackRuntimeException002(): Begin");
            AbstractCallbackListener.setTargetPostLoadLifeCycleWithRuntimeException(null);

            System.out.println("Testing JPA Persist Life Cycle Methods...");
            testPrePersistLifecycle(targetEntityType, jpaResource, true, listenerProtectionType); // 3 points
            testPostPersistLifecycle(targetEntityType, jpaResource, true, listenerProtectionType); // 1 point

            ICallbackEntity testEntity = (ICallbackEntity) constructNewEntityObject(targetEntityType);
            testEntity.setId(3);
            testEntity.setName("Update-Remove-CallbackEntity-3");
            try {
                // Create an instance of CallbackEntity for update and remove lifecycle tests
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    jpaResource.getEm().joinTransaction();
                }

                System.out.println("Populating for Update and Remove tests...");
                jpaResource.getEm().persist(testEntity);
                jpaResource.getTj().commitTransaction();
                jpaResource.getEm().clear();

                System.out.println("Testing JPA Update Life Cycle Methods...");
                testPreUpdateLifecycle(targetEntityType, jpaResource, true, listenerProtectionType); // 2 points
                testPostUpdateLifecycle(targetEntityType, jpaResource, true, listenerProtectionType); // 2 points

                System.out.println("Testing JPA Remove Life Cycle Methods...");
                testPreRemoveLifecycle(targetEntityType, jpaResource, true, listenerProtectionType); // 2 points
                testPostRemoveLifecycle(targetEntityType, jpaResource, true, listenerProtectionType); // 2 points

                System.out.println("Testing JPA Load Life Cycle Methods...");
                testPostLoadLifecycle(targetEntityType, jpaResource, true, listenerProtectionType); // 3 points
            } finally {
                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                System.out.println("Removing " + testEntity + " to clean up test");
                testEntity = jpaResource.getEm().merge(testEntity);
                jpaResource.getEm().remove(testEntity);
                jpaResource.getTj().commitTransaction();
                jpaResource.getEm().clear();
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("CallbackTestLogic.testCallbackRuntimeException002(): End");
            AbstractCallbackListener.resetGlobalCallbackEventList();
        }
    }

    /**
     * Test when a RuntimeException is thrown by a callback method on entity-defined listener classes.
     * <p>
     * Points: 15
     */
    public void testCallbackRuntimeException003(
                                                TestExecutionContext testExecCtx,
                                                TestExecutionResources testExecResources,
                                                Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testCallbackRuntimeException003: Missing context and/or resources.  Cannot execute the test.");
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
        CallbackEntityEnum targetEntityType = CallbackEntityEnum.resolveEntityByName(entityName);
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
            System.out.println("CallbackTestLogic.testCallbackRuntimeException003(): Begin");
            AbstractCallbackListener.setTargetPostLoadLifeCycleWithRuntimeException(null);

            System.out.println("Testing JPA Persist Life Cycle Methods...");
            testPrePersistLifecycle(targetEntityType, jpaResource, false, listenerProtectionType); // 3 points
            testPostPersistLifecycle(targetEntityType, jpaResource, false, listenerProtectionType); // 1 point

            ICallbackEntity testEntity = (ICallbackEntity) constructNewEntityObject(targetEntityType);
            testEntity.setId(3);
            testEntity.setName("Update-Remove-CallbackEntity-3");

            try {
                // Create an instance of CallbackEntity for update and remove lifecycle tests
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    jpaResource.getEm().joinTransaction();
                }

                System.out.println("Populating for Update and Remove tests...");
                jpaResource.getEm().persist(testEntity);
                jpaResource.getTj().commitTransaction();
                jpaResource.getEm().clear();

                System.out.println("Testing JPA Update Life Cycle Methods...");
                testPreUpdateLifecycle(targetEntityType, jpaResource, false, listenerProtectionType); // 2 points
                testPostUpdateLifecycle(targetEntityType, jpaResource, false, listenerProtectionType); // 2 points

                System.out.println("Testing JPA Remove Life Cycle Methods...");
                testPreRemoveLifecycle(targetEntityType, jpaResource, false, listenerProtectionType); // 2 points
                testPostRemoveLifecycle(targetEntityType, jpaResource, false, listenerProtectionType); // 2 points

                System.out.println("Testing JPA Load Life Cycle Methods...");
                testPostLoadLifecycle(targetEntityType, jpaResource, false, listenerProtectionType); // 3 points
            } finally {
                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                System.out.println("Removing " + testEntity + " to clean up test");
                testEntity = jpaResource.getEm().merge(testEntity);
                jpaResource.getEm().remove(testEntity);
                jpaResource.getTj().commitTransaction();
                jpaResource.getEm().clear();
            }
            System.out.println("Ending test.");
        } finally {
            System.out.println("CallbackTestLogic.testCallbackRuntimeException003(): End");
            AbstractCallbackListener.resetGlobalCallbackEventList();
        }
    }

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
                                                               CallbackEntityEnum targetEntityType) {
        if (targetEntityType.toString().toLowerCase().startsWith("ano")) {
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

    /**
     * Test Strategy:
     * <p><ul>
     * <li>Start Transaction
     * <li>Create Unmanaged Callback Entity
     * <li>Mark the new Entity object to throw a CallbackRuntimeException when the PrePersist callback lifecycle method is invoked.
     * <li>Call em.persist() to try persist the new entity
     * <li>This should throw a CallbackRuntime Exception and mark the transaction for rollback
     * <li>Catch the Exception, make sure it is CallbackRuntimeException
     * <li>Check the Transaction and make sure it is marked for rollback
     * <li>Roll back the Transaction (cleanup)
     * <li>
     * </ul><p>
     * Sub-Test passes if the persist operation throws a CallbackRuntimeException, the transaction remains
     * active, and is marked for rollback.
     * <p>
     * Sub-Points: 3
     */
    private void testPrePersistLifecycle(
                                         CallbackEntityEnum targetEntityType,
                                         JPAResource jpaResource,
                                         boolean targetDefaultListener,
                                         ProtectionType listenerProtectionType) throws Throwable {
        System.out.println("Testing @PrePersist Exception behavior...");

        // Roll back any active transaction
        if (jpaResource.getTj().isTransactionActive()) {
            jpaResource.getTj().rollbackTransaction();
        }

        // Clear persistence context
        System.out.println("Clearing persistence context...");
        jpaResource.getEm().clear();

        AbstractCallbackListener targetListener = null;
        String targetListenerName = "";
        int id = 1;

        try {
            if (listenerProtectionType != null) {
                if (targetDefaultListener) {
                    targetListener = fetchTargetListener(listenerProtectionType);
                } else {
                    targetListener = fetchTargetEntityListener(listenerProtectionType, targetEntityType);
                }
                targetListenerName = targetListener.getClass().getSimpleName();
            }

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("1) Creating new object instance of " + targetEntityType.getEntityName() + "...");
            ICallbackEntity entity_persist = (ICallbackEntity) constructNewEntityObject(targetEntityType);
            entity_persist.setId(id);
            entity_persist.setName("PrePersist-CallbackEntity-" + id);

            // Configure to throw a CallbackRuntimeException during the @PrePersist callback.
            if (targetListener == null) {
                System.out.println("2) Configuring the entity object to throw a CallbackRuntimeException during the @PrePersist callback.");
                ((AbstractCallbackListener) entity_persist).setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PrePersist);
            } else {
                System.out.println("2) Configuring " + targetListenerName +
                                   " listener to throw a CallbackRuntimeException during the @PrePersist callback.");
                targetListener.setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PrePersist);
            }

            try {
                System.out.println("3) Calling em.persist (should fail with a CallbackRuntimeException.");
                jpaResource.getEm().persist(entity_persist);

                // Remove the managed instance so that it doesn't get committed to the database
                jpaResource.getEm().remove(entity_persist);
                Assert.fail("No Exception was thrown by the em.persist() operation.");
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Caught an Exception, check if CallbackRuntimeException is in the Exception Chain
                System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for CallbackRuntimeException...");
                assertExceptionIsInChain(CallbackRuntimeException.class, t);
            }

            // 4) Check if transaction is still active
            Assert.assertTrue("4) Assert transaction is still active.", jpaResource.getTj().isTransactionActive());

            // 5) Check if transaction is marked for rollback
            Assert.assertTrue("5) Assert transaction is marked for rollback.", jpaResource.getTj().isTransactionMarkedForRollback());
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
     * <li>Create Unmanaged Callback Entity
     * <li>Mark the new Entity object to throw a CallbackRuntimeException when the PostPersist callback lifecycle method is invoked.
     * <li>Call em.persist() to try persist the new entity
     * <li>Depending on the JPA implementation, the postpersist method may be called by the persist() operation, or it may be called when the transaction is being committed.
     * </ul><p>
     * Sub-Test passes if the persist operation (or transaction commit) throws a CallbackRuntimeException.
     * <p>
     * Sub-Points: 1
     */
    private void testPostPersistLifecycle(
                                          CallbackEntityEnum targetEntityType,
                                          JPAResource jpaResource,
                                          boolean targetDefaultListener,
                                          ProtectionType listenerProtectionType) throws Throwable {
        System.out.println("Testing @PostPersist Exception behavior...");

        // Roll back any active transaction
        if (jpaResource.getTj().isTransactionActive()) {
            jpaResource.getTj().rollbackTransaction();
        }

        // Clear persistence context
        System.out.println("Clearing persistence context...");
        jpaResource.getEm().clear();

        AbstractCallbackListener targetListener = null;
        String targetListenerName = "";
        int id = 2;

        try {
            if (listenerProtectionType != null) {
                if (targetDefaultListener) {
                    targetListener = fetchTargetListener(listenerProtectionType);
                } else {
                    targetListener = fetchTargetEntityListener(listenerProtectionType, targetEntityType);
                }
                targetListenerName = targetListener.getClass().getSimpleName();
            }

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("1) Creating new object instance of " + targetEntityType.getEntityName() + "...");
            ICallbackEntity entity_persist = (ICallbackEntity) constructNewEntityObject(targetEntityType);
            entity_persist.setId(id);
            entity_persist.setName("PostPersist-CallbackEntity-" + id);

            // Configure the entity object to throw a CallbackRuntimeException during the @PostPersist callback.
            if (targetListener == null) {
                System.out.println("2) Configuring the entity object to throw a CallbackRuntimeException during the @PostPersist callback.");
                ((AbstractCallbackListener) entity_persist).setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PostPersist);
            } else {
                System.out.println("2) Configuring the " + targetListenerName +
                                   " listener to throw a CallbackRuntimeException during the @PostPersist callback.");
                targetListener.setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PostPersist);
            }

            boolean exceptionThrownByPersistOperation = true;
            try {
                System.out.println("3) Calling em.persist (may fail with a CallbackRuntimeException.");
                jpaResource.getEm().persist(entity_persist);

                System.out.println("The em.persist() operation did not throw a CallbackRuntimeException.");
                System.out.println("This means the Exception must be thrown by the transaction commit operation.");
                exceptionThrownByPersistOperation = false;

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                Assert.fail("No Exception was thrown by either the em.persist() or tran-commit operations.");
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Caught an Exception, check if CallbackRuntimeException is in the Exception Chain
                System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for CallbackRuntimeException...");
                assertExceptionIsInChain(CallbackRuntimeException.class, t);
            }

            // If the persist() operation did throw the Exception, then we have to check if the tran is still
            // active and is marked for rollback.
            if (exceptionThrownByPersistOperation == true) {
                System.out.println("The CallbackRuntimeException was thrown by the em.persist() operation,");
                System.out.println("so verify that the transaction is active and marked for rollback.");
                System.out.println("Is transaction still active = " + jpaResource.getTj().isTransactionActive());
                System.out.println("Is transaction marked for rollback = " + jpaResource.getTj().isTransactionActive());
                if (jpaResource.getTj().isTransactionActive() == false ||
                    jpaResource.getTj().isTransactionActive() == false) {
                    Assert.fail("One or both of the criteria failed.");
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
     * <li>Find CallbackEntity(id=3)
     * <li>Dirty the CallbackEntity - this may fire the @PreUpdate callback on some JPA implementations.
     * <li>(If not fired by the update) Commit the transaction, this should fire the @PreUpdate callback
     * <li>Check that the Exception thrown by the update or transaction commit operation contains CallbackRuntimeException in its chain.
     * <li>If the CallbackRuntimeException was thrown by the update operation, check that the transaction is still active and marked for rollback.
     * </ul><p>
     *
     * Sub-Points: 2
     */
    private void testPreUpdateLifecycle(
                                        CallbackEntityEnum targetEntityType,
                                        JPAResource jpaResource,
                                        boolean targetDefaultListener,
                                        ProtectionType listenerProtectionType) throws Throwable {
        System.out.println("Testing @PreUpdate Exception behavior...");

        // Roll back any active transaction
        if (jpaResource.getTj().isTransactionActive()) {
            jpaResource.getTj().rollbackTransaction();
        }

        // Clear persistence context
        System.out.println("Clearing persistence context...");
        jpaResource.getEm().clear();

        AbstractCallbackListener targetListener = null;
        String targetListenerName = "";
        int id = 3;

        try {
            if (listenerProtectionType != null) {
                if (targetDefaultListener) {
                    targetListener = fetchTargetListener(listenerProtectionType);
                } else {
                    targetListener = fetchTargetEntityListener(listenerProtectionType, targetEntityType);
                }
                targetListenerName = targetListener.getClass().getSimpleName();
            }

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            // 1) Load Callback Entity
            System.out.println("1) Load " + targetEntityType.getEntityName() + "(id=" + id + ")...");
            ICallbackEntity entity = (ICallbackEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            Assert.assertNotNull("Assert find() did not return null.", entity);

            // Configure the entity object to throw a CallbackRuntimeException during the @PreUpdate callback.
            if (targetListener == null) {
                System.out.println("2) Configuring the entity object to throw a CallbackRuntimeException during the @PreUpdate callback.");
                ((AbstractCallbackListener) entity).setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PreUpdate);
            } else {
                System.out.println("2) Configuring the " + targetListenerName +
                                   " listener to throw a CallbackRuntimeException during the @PreUpdate callback.");
                targetListener.setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PreUpdate);
            }

            boolean exceptionThrownByUpdate = true;
            try {
                System.out.println("3) Dirty the entity (may fail with a CallbackRuntimeException.");
                entity.setName("Dirty Name");

                System.out.println("The update did not throw a CallbackRuntimeException.");
                System.out.println("This means the Exception must be thrown by the transaction commit operation.");
                exceptionThrownByUpdate = false;

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                Assert.fail("No Exception was thrown by either the update or tran-commit operations.");
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Caught an Exception, check if CallbackRuntimeException is in the Exception Chain
                System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for CallbackRuntimeException...");
                assertExceptionIsInChain(CallbackRuntimeException.class, t);
            }

            // If the update operation did throw the Exception, then we have to check if the tran is still
            // active and is marked for rollback.
            if (exceptionThrownByUpdate == true) {
                System.out.println("The CallbackRuntimeException was thrown by the update operation,");
                System.out.println("so verify that the transaction is active and marked for rollback.");
                System.out.println("Is transaction still active = " + jpaResource.getTj().isTransactionActive());
                System.out.println("Is transaction marked for rollback = " + jpaResource.getTj().isTransactionActive());
                if (jpaResource.getTj().isTransactionActive() == false ||
                    jpaResource.getTj().isTransactionActive() == false) {
                    Assert.fail("One or both of the criteria failed.");
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
     * <li>Find CallbackEntity(id=3)
     * <li>Dirty the CallbackEntity.
     * <li>Commit the transaction, this should fire the @PostUpdate callback
     * <li>Check that the Exception thrown by the update or transaction commit operation contains CallbackRuntimeException in its chain.
     * <li>If the CallbackRuntimeException was thrown by the update operation, check that the transaction is still active and marked for rollback.
     * </ul><p>
     *
     * Sub-Points: 2
     */
    private void testPostUpdateLifecycle(
                                         CallbackEntityEnum targetEntityType,
                                         JPAResource jpaResource,
                                         boolean targetDefaultListener,
                                         ProtectionType listenerProtectionType) throws Throwable {
        System.out.println("Testing @PostUpdate Exception behavior...");

        // Roll back any active transaction
        if (jpaResource.getTj().isTransactionActive()) {
            jpaResource.getTj().rollbackTransaction();
        }

        // Clear persistence context
        System.out.println("Clearing persistence context...");
        jpaResource.getEm().clear();

        AbstractCallbackListener targetListener = null;
        String targetListenerName = "";
        int id = 3;

        try {
            if (listenerProtectionType != null) {
                if (targetDefaultListener) {
                    targetListener = fetchTargetListener(listenerProtectionType);
                } else {
                    targetListener = fetchTargetEntityListener(listenerProtectionType, targetEntityType);
                }
                targetListenerName = targetListener.getClass().getSimpleName();
            }

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            // 1) Load Callback Entity
            System.out.println("1) Load " + targetEntityType.getEntityName() + "(id=" + id + ")...");
            ICallbackEntity entity = (ICallbackEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            Assert.assertNotNull("Assert find() did not return null.", entity);

            // Configure the entity object to throw a CallbackRuntimeException during the @PostUpdate callback.
            if (targetListener == null) {
                System.out.println("2) Configuring the entity object to throw a CallbackRuntimeException during the @PostUpdate callback.");
                ((AbstractCallbackListener) entity).setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PostUpdate);
            } else {
                System.out.println("2) Configuring the " + targetListenerName +
                                   " listener to throw a CallbackRuntimeException during the @PostUpdate callback.");
                targetListener.setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PostUpdate);
            }

            try {
                System.out.println("3) Dirty the entity.");
                entity.setName("Dirty Name");

                System.out.println("Committing transaction (Should throw Exception) ...");
                jpaResource.getTj().commitTransaction();

                Assert.fail("No Exception was thrown by tran commit operation.");
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Caught an Exception, check if CallbackRuntimeException is in the Exception Chain
                System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for CallbackRuntimeException...");
                assertExceptionIsInChain(CallbackRuntimeException.class, t);
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
     * <li>Find CallbackEntity(id=3)
     * <li>Mark the CallbackEntity for removal - this may fire the @PreRemove callback on some JPA implementations.
     * <li>(If not fired by the remove op) Commit the transaction, this should fire the @PreRemove callback
     * <li>Check that the Exception thrown by the update or transaction commit operation contains CallbackRuntimeException in its chain.
     * <li>If the CallbackRuntimeException was thrown by the remove operation, check that the transaction is still active and marked for rollback.
     * </ul><p>
     *
     * Sub-Points: 2
     */
    private void testPreRemoveLifecycle(
                                        CallbackEntityEnum targetEntityType,
                                        JPAResource jpaResource,
                                        boolean targetDefaultListener,
                                        ProtectionType listenerProtectionType) throws Throwable {
        System.out.println("Testing @PreRemove Exception behavior...");

        // Roll back any active transaction
        if (jpaResource.getTj().isTransactionActive()) {
            jpaResource.getTj().rollbackTransaction();
        }

        // Clear persistence context
        System.out.println("Clearing persistence context...");
        jpaResource.getEm().clear();

        AbstractCallbackListener targetListener = null;
        String targetListenerName = "";
        int id = 3;

        try {
            if (listenerProtectionType != null) {
                if (targetDefaultListener) {
                    targetListener = fetchTargetListener(listenerProtectionType);
                } else {
                    targetListener = fetchTargetEntityListener(listenerProtectionType, targetEntityType);
                }
                targetListenerName = targetListener.getClass().getSimpleName();
            }

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            // 1) Load Callback Entity
            System.out.println("1) Load " + targetEntityType.getEntityName() + "(id=" + id + ")...");
            ICallbackEntity entity = (ICallbackEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            Assert.assertNotNull("Assert find() did not return null.", entity);

            // Configure the entity object to throw a CallbackRuntimeException during the @PreRemove callback.
            if (targetListener == null) {
                System.out.println("2) Configuring the entity object to throw a CallbackRuntimeException during the @PreRemove callback.");
                ((AbstractCallbackListener) entity).setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PreRemove);
            } else {
                System.out.println("2) Configuring the " + targetListenerName +
                                   " listener to throw a CallbackRuntimeException during the @PreRemove callback.");
                targetListener.setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PreRemove);
            }

            boolean exceptionThrownByRemove = true;
            try {
                System.out.println("3) Mark the entity for removal (may fail with a CallbackRuntimeException.");
                jpaResource.getEm().remove(entity);

                System.out.println("The remove did not throw a CallbackRuntimeException.");
                System.out.println("This means the Exception must be thrown by the transaction commit operation.");
                exceptionThrownByRemove = false;

                System.out.println("Committing transaction...");
                jpaResource.getTj().commitTransaction();

                Assert.fail("No Exception was thrown by either the em.remove() or tran-commit operations.");
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Caught an Exception, check if CallbackRuntimeException is in the Exception Chain
                System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for CallbackRuntimeException...");
                assertExceptionIsInChain(CallbackRuntimeException.class, t);
            }

            // If the remove operation did throw the Exception, then we have to check if the tran is still
            // active and is marked for rollback.
            if (exceptionThrownByRemove == true) {
                System.out.println("The CallbackRuntimeException was thrown by the remove operation,");
                System.out.println("so verify that the transaction is active and marked for rollback.");
                System.out.println("Is transaction still active = " + jpaResource.getTj().isTransactionActive());
                System.out.println("Is transaction marked for rollback = " + jpaResource.getTj().isTransactionActive());
                if (jpaResource.getTj().isTransactionActive() == false ||
                    jpaResource.getTj().isTransactionActive() == false) {
                    Assert.fail("One or both of the criteria failed.");
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
     * <li>Find CallbackEntity(id=3)
     * <li>Mark the CallbackEntity for removal
     * <li>Commit the transaction, this should fire the @PostRemove callback
     * <li>Check that the Exception thrown by the transaction commit operation contains CallbackRuntimeException in its chain.
     * <li>If the CallbackRuntimeException was thrown by the update operation, check that the transaction is still active and marked for rollback.
     * </ul><p>
     *
     * Sub-Points: 2
     */
    private void testPostRemoveLifecycle(
                                         CallbackEntityEnum targetEntityType,
                                         JPAResource jpaResource,
                                         boolean targetDefaultListener,
                                         ProtectionType listenerProtectionType) throws Throwable {
        System.out.println("Testing @PostRemove Exception behavior...");

        // Roll back any active transaction
        if (jpaResource.getTj().isTransactionActive()) {
            jpaResource.getTj().rollbackTransaction();
        }

        // Clear persistence context
        System.out.println("Clearing persistence context...");
        jpaResource.getEm().clear();

        AbstractCallbackListener targetListener = null;
        String targetListenerName = "";
        int id = 3;

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
            System.out.println("1) Load " + targetEntityType.getEntityName() + "(id=" + id + ")...");

            // Begin new transaction
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            ICallbackEntity entity = (ICallbackEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            Assert.assertNotNull("Assert find() did not return null.", entity);

            // Configure the entity object to throw a CallbackRuntimeException during the @PostRemove callback.
            if (targetListener == null) {
                System.out.println("2) Configuring the entity object to throw a CallbackRuntimeException during the @PostRemove callback.");
                ((AbstractCallbackListener) entity).setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PostRemove);
            } else {
                System.out.println("2) Configuring the " + targetListenerName +
                                   " listener to throw a CallbackRuntimeException during the @PostRemove callback.");
                targetListener.setRuntimeExceptionLifecycleTarget(CallbackLifeCycle.PostRemove);
            }

            try {
                System.out.println("3) Remove the entity.");
                jpaResource.getEm().remove(entity);

                System.out.println("Committing transaction (Should throw Exception) ...");
                jpaResource.getTj().commitTransaction();

                Assert.fail("No Exception was thrown by tran commit operation.");
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Caught an Exception, check if CallbackRuntimeException is in the Exception Chain
                System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for CallbackRuntimeException...");
                assertExceptionIsInChain(CallbackRuntimeException.class, t);
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
     * <li>Set AbstractCallbackListener to throw Exception on PostLoad events
     * <li>Start Transaction
     * <li>Find CallbackEntity(id=3) - should trigger the Exception
     * <li>Verify that the transaction is active and marked for rollback.
     * </ul><p>
     *
     * Sub-Points: 3
     */
    private void testPostLoadLifecycle(
                                       CallbackEntityEnum targetEntityType,
                                       JPAResource jpaResource,
                                       boolean targetDefaultListener,
                                       ProtectionType listenerProtectionType) throws Throwable {
        System.out.println("Testing @PostLoad Exception behavior...");

        // Roll back any active transaction
        if (jpaResource.getTj().isTransactionActive()) {
            jpaResource.getTj().rollbackTransaction();
        }

        // Clear persistence context
        System.out.println("Clearing persistence context...");
        jpaResource.getEm().clear();

        int id = 3;

        try {
            // 1) Set CallbackListener to throw CallbackRuntimeException on PostLoad callback invocation
            System.out.println("1) Set CallbackListener to throw CallbackRuntimeException on PostLoad callback invocation");
            AbstractCallbackListener.setTargetPostLoadLifeCycleWithRuntimeException(
                                                                                    (listenerProtectionType == null) ? ProtectionType.ALL : listenerProtectionType);

            try {
                // 2) Load Callback Entity
                System.out.println("2) Load " + targetEntityType.getEntityName() + "(id=" + id + ") (should throw Exception) ...");

                // Begin new transaction
                System.out.println("Beginning new transaction...");
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                ICallbackEntity entity = (ICallbackEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
                Assert.assertNotNull("Assert find() did not return null.", entity);

                Assert.fail("No Exception was thrown by find operation.");
            } catch (java.lang.AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Caught an Exception, check if CallbackRuntimeException is in the Exception Chain
                System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for CallbackRuntimeException...");
                assertExceptionIsInChain(CallbackRuntimeException.class, t);
            }

            // 3) Check if transaction is still active
            Assert.assertTrue("3) Assert transaction is still active.", jpaResource.getTj().isTransactionActive());

            // 4) Check if transaction is marked for rollback
            Assert.assertTrue("4) Assert transaction is marked for rollback.", jpaResource.getTj().isTransactionMarkedForRollback());
        } finally {
            AbstractCallbackListener.setTargetPostLoadLifeCycleWithRuntimeException(null);

            resetListeners();

            // Cleanup
            if (jpaResource.getTj().isTransactionActive()) {
                System.out.println("Rolling back transaction...");
                jpaResource.getTj().rollbackTransaction();
            }
        }
    }
}
