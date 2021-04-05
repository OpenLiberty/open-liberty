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
import com.ibm.ws.jpa.fvt.callback.CallbackRecord;
import com.ibm.ws.jpa.fvt.callback.CallbackRecord.CallbackLifeCycle;
import com.ibm.ws.jpa.fvt.callback.entities.ICallbackEntity;
import com.ibm.ws.jpa.fvt.callback.listeners.ano.AnoCallbackListenerPackage;
import com.ibm.ws.jpa.fvt.callback.listeners.ano.AnoCallbackListenerPrivate;
import com.ibm.ws.jpa.fvt.callback.listeners.ano.AnoCallbackListenerProtected;
import com.ibm.ws.jpa.fvt.callback.listeners.ano.AnoCallbackListenerPublic;
import com.ibm.ws.jpa.fvt.callback.listeners.defaultlistener.DefaultCallbackListenerPackage;
import com.ibm.ws.jpa.fvt.callback.listeners.defaultlistener.DefaultCallbackListenerPrivate;
import com.ibm.ws.jpa.fvt.callback.listeners.defaultlistener.DefaultCallbackListenerProtected;
import com.ibm.ws.jpa.fvt.callback.listeners.defaultlistener.DefaultCallbackListenerPublic;
import com.ibm.ws.jpa.fvt.callback.listeners.xml.XMLCallbackListenerPackage;
import com.ibm.ws.jpa.fvt.callback.listeners.xml.XMLCallbackListenerPrivate;
import com.ibm.ws.jpa.fvt.callback.listeners.xml.XMLCallbackListenerProtected;
import com.ibm.ws.jpa.fvt.callback.listeners.xml.XMLCallbackListenerPublic;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class CallbackTestLogic extends AbstractTestLogic {

    /**
     * Test callback methods on entity classes. Supports testing of entities declared by annotation and
     * XML, and supports stand-alone entity classes and entities that gain callback methods from
     * mapped superclasses.
     * <p>
     * Points: 10
     */
    public void testCallback001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testCallback001: Missing context and/or resources.  Cannot execute the test.");
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

        int id = 1;

        // Execute Test Case
        try {
            System.out.println("CallbackTestLogic.testCallback001(): Begin");
            AbstractCallbackListener.setTargetPostLoadLifeCycleWithRuntimeException(null);

            // Test @PrePersist and @PostPersist
            System.out.println("1) Test @PrePersist and @PostPersist");

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + "...");
            ICallbackEntity entity_persist = (ICallbackEntity) constructNewEntityObject(targetEntityType);
            entity_persist.setId(id);
            entity_persist.setName("testCallback001-CallbackEntity-" + id);
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet.",
                                0, ((AbstractCallbackListener) entity_persist).getFiredLifeCycleSet().size());

            System.out.println("Persisting " + entity_persist + " ...");
            jpaResource.getEm().persist(entity_persist);
            Assert.assertTrue("Assert @PrePersist has fired.",
                              (((AbstractCallbackListener) entity_persist).getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist)));

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();
            Assert.assertTrue("Assert @PostPersist has fired.",
                              (((AbstractCallbackListener) entity_persist).getFiredLifeCycleSet().contains(CallbackLifeCycle.PostPersist)));

            System.out.println("Callbacks Observed:");
            for (CallbackRecord cr : ((AbstractCallbackListener) entity_persist).getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            // Test @PostLoad
            System.out.println("2) Test @PostLoad");

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + "(id=" + id + ")...");
            ICallbackEntity entity_find = (ICallbackEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            Assert.assertNotNull("Assert find() did not return null.", entity_find);
            Assert.assertTrue("Assert @PostLoad has fired.",
                              (((AbstractCallbackListener) entity_find).getFiredLifeCycleSet().contains(CallbackLifeCycle.PostLoad)));

            System.out.println("Callbacks Observed:");
            for (CallbackRecord cr : ((AbstractCallbackListener) entity_find).getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            // Test @PreUpdate and @PostUpdate
            // Note when @PreUpdate fires is vendor specific, it could fire each time the object's persistent fields
            // are changed, or it could be fired when the transaction is in the process of committing.
            System.out.println("3) Test @PreUpdate and @PostUpdate");
            System.out.println("Updating " + targetEntityType.getEntityName() + "(id=" + id + ")'s name field...");
            entity_find.setName("Mutated Name");

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();
            Assert.assertTrue("Assert @PreUpdate has fired.",
                              (((AbstractCallbackListener) entity_find).getFiredLifeCycleSet().contains(CallbackLifeCycle.PreUpdate)));
            Assert.assertTrue("Assert @PostUpdate has fired.",
                              (((AbstractCallbackListener) entity_find).getFiredLifeCycleSet().contains(CallbackLifeCycle.PostUpdate)));

            System.out.println("Callbacks Observed:");
            for (CallbackRecord cr : ((AbstractCallbackListener) entity_find).getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            // Test @PreRemove and @PostRemove
            System.out.println("4) Test @PreRemove and @PostRemove");

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + "(id=" + id + ")...");
            ICallbackEntity entity_remove = (ICallbackEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            Assert.assertNotNull("Assert find() did not return null.", entity_remove);

            System.out.println("Calling remove() on " + targetEntityType.getEntityName() + "(id=" + id + ") ...");
            jpaResource.getEm().remove(entity_remove);
            Assert.assertTrue("Assert @PreRemove has fired.",
                              (((AbstractCallbackListener) entity_remove).getFiredLifeCycleSet().contains(CallbackLifeCycle.PreRemove)));

            System.out.println("Callbacks Observed:");
            for (CallbackRecord cr : ((AbstractCallbackListener) entity_remove).getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();
            Assert.assertTrue("Assert @PostRemove has fired.",
                              (((AbstractCallbackListener) entity_remove).getFiredLifeCycleSet().contains(CallbackLifeCycle.PostRemove)));

            System.out.println("Callbacks Observed:");
            for (CallbackRecord cr : ((AbstractCallbackListener) entity_remove).getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("CallbackTestLogic.testCallback001(): End");
            AbstractCallbackListener.resetGlobalCallbackEventList();
        }
    }

    /**
     * Test callback methods on default listener classes.
     * <p>
     * Points: 34
     */
    public void testCallback002(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testCallback002: Missing context and/or resources.  Cannot execute the test.");
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

        int id = 2;

        // Execute Test Case
        try {
            System.out.println("CallbackTestLogic.testCallback002(): Begin");
            AbstractCallbackListener.setTargetPostLoadLifeCycleWithRuntimeException(null);

            // Test @PrePersist and @PostPersist
            System.out.println("1) Test @PrePersist and @PostPersist");
            resetDefaultListeners();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + "...");
            ICallbackEntity entity_persist = (ICallbackEntity) constructNewEntityObject(targetEntityType);
            entity_persist.setId(id);
            entity_persist.setName("testCallback002-CallbackEntity-" + id);

            // Assert no lifecycle callbacks have been fired yet (4 points)
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for DefaultCallbackListenerPackage",
                                0, DefaultCallbackListenerPackage.getSingleton().getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for DefaultCallbackListenerPrivate",
                                0, DefaultCallbackListenerPrivate.getSingleton().getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for DefaultCallbackListenerProtected",
                                0, DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for DefaultCallbackListenerPublic",
                                0, DefaultCallbackListenerPublic.getSingleton().getFiredLifeCycleSet().size());

            System.out.println("Persisting " + entity_persist + " ...");
            jpaResource.getEm().persist(entity_persist);

            // Assert @PrePersist has fired (4 points)
            Assert.assertTrue("Assert @PrePersist has fired for DefaultCallbackListenerPackage.",
                              DefaultCallbackListenerPackage.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertTrue("Assert @PrePersist has fired for DefaultCallbackListenerPrivate.",
                              DefaultCallbackListenerPrivate.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertTrue("Assert @PrePersist has fired for DefaultCallbackListenerProtected.",
                              DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertTrue("Assert @PrePersist has fired for DefaultCallbackListenerProtected.",
                              DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Assert @PostPersist has fired (4 points)
            Assert.assertTrue("Assert @PostPersist has fired for DefaultCallbackListenerPackage.",
                              DefaultCallbackListenerPackage.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostPersist));
            Assert.assertTrue("Assert @PostPersist has fired for DefaultCallbackListenerPrivate.",
                              DefaultCallbackListenerPrivate.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostPersist));
            Assert.assertTrue("Assert @PostPersist has fired for DefaultCallbackListenerProtected.",
                              DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostPersist));
            Assert.assertTrue("Assert @PostPersist has fired for DefaultCallbackListenerProtected.",
                              DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostPersist));

            // Callbacks Observed
            System.out.println("Callbacks Observed:");
            System.out.println("DefaultCallbackListenerPackage Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPackage.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerPrivate Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPrivate.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            // Test @PostLoad
            System.out.println("2) Test @PostLoad");
            resetDefaultListeners();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + "(id=" + id + ")...");
            ICallbackEntity entity_find = (ICallbackEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            Assert.assertNotNull("Assert find() did not return null.", entity_find);

            // Assert @PostLoad has fired (4 points)
            Assert.assertTrue("Assert @PostPersist has fired for DefaultCallbackListenerPackage.",
                              DefaultCallbackListenerPackage.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostLoad));
            Assert.assertTrue("Assert @PostPersist has fired for DefaultCallbackListenerPrivate.",
                              DefaultCallbackListenerPrivate.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostLoad));
            Assert.assertTrue("Assert @PostPersist has fired for DefaultCallbackListenerProtected.",
                              DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostLoad));
            Assert.assertTrue("Assert @PostPersist has fired for DefaultCallbackListenerProtected.",
                              DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostLoad));

            // Callbacks Observed
            System.out.println("Callbacks Observed:");
            System.out.println("DefaultCallbackListenerPackage Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPackage.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerPrivate Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPrivate.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            // Test @PreUpdate and @PostUpdate
            // Note when @PreUpdate fires is vendor specific, it could fire each time the object's persistent fields
            // are changed, or it could be fired when the transaction is in the process of committing.
            System.out.println("3) Test @PreUpdate and @PostUpdate");
            resetDefaultListeners();

            System.out.println("Updating " + targetEntityType.getEntityName() + "(id=" + id + ")'s name field...");
            entity_find.setName("Mutated Name");

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Assert @PreUpdate has fired (4 points)
            Assert.assertTrue("Assert @PreUpdate has fired for DefaultCallbackListenerPackage.",
                              DefaultCallbackListenerPackage.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PreUpdate));
            Assert.assertTrue("Assert @PreUpdate has fired for DefaultCallbackListenerPrivate.",
                              DefaultCallbackListenerPrivate.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PreUpdate));
            Assert.assertTrue("Assert @PreUpdate has fired for DefaultCallbackListenerProtected.",
                              DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PreUpdate));
            Assert.assertTrue("Assert @PreUpdate has fired for DefaultCallbackListenerProtected.",
                              DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PreUpdate));

            // Assert @PreUpdate has fired (4 points)
            Assert.assertTrue("Assert @PostUpdate has fired for DefaultCallbackListenerPackage.",
                              DefaultCallbackListenerPackage.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostUpdate));
            Assert.assertTrue("Assert @PostUpdate has fired for DefaultCallbackListenerPrivate.",
                              DefaultCallbackListenerPrivate.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostUpdate));
            Assert.assertTrue("Assert @PostUpdate has fired for DefaultCallbackListenerProtected.",
                              DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostUpdate));
            Assert.assertTrue("Assert @PostUpdate has fired for DefaultCallbackListenerProtected.",
                              DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostUpdate));

            // Callbacks Observed
            System.out.println("Callbacks Observed:");
            System.out.println("DefaultCallbackListenerPackage Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPackage.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerPrivate Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPrivate.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            // Test @PreRemove and @PostRemove
            System.out.println("4) Test @PreRemove and @PostRemove");
            resetDefaultListeners();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + "(id=" + id + ")...");
            ICallbackEntity entity_remove = (ICallbackEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), 2);
            Assert.assertNotNull("Assert find() did not return null.", entity_remove);

            System.out.println("Calling remove() on " + targetEntityType.getEntityName() + "(id=1) ...");
            jpaResource.getEm().remove(entity_remove);

            // Assert @PreRemove has fired (4 points)
            Assert.assertTrue("Assert @PreRemove has fired for DefaultCallbackListenerPackage.",
                              DefaultCallbackListenerPackage.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PreRemove));
            Assert.assertTrue("Assert @PreRemove has fired for DefaultCallbackListenerPrivate.",
                              DefaultCallbackListenerPrivate.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PreRemove));
            Assert.assertTrue("Assert @PreRemove has fired for DefaultCallbackListenerProtected.",
                              DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PreRemove));
            Assert.assertTrue("Assert @PreRemove has fired for DefaultCallbackListenerProtected.",
                              DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PreRemove));

            // Callbacks Observed
            System.out.println("Callbacks Observed:");
            System.out.println("DefaultCallbackListenerPackage Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPackage.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerPrivate Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPrivate.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Assert @PostRemove has fired (4 points)
            Assert.assertTrue("Assert @PostRemove has fired for DefaultCallbackListenerPackage.",
                              DefaultCallbackListenerPackage.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostRemove));
            Assert.assertTrue("Assert @PostRemove has fired for DefaultCallbackListenerPrivate.",
                              DefaultCallbackListenerPrivate.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostRemove));
            Assert.assertTrue("Assert @PostRemove has fired for DefaultCallbackListenerProtected.",
                              DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostRemove));
            Assert.assertTrue("Assert @PostRemove has fired for DefaultCallbackListenerProtected.",
                              DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostRemove));

            // Callbacks Observed
            System.out.println("Callbacks Observed:");
            System.out.println("DefaultCallbackListenerPackage Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPackage.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerPrivate Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPrivate.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("CallbackTestLogic.testCallback002(): End");
            AbstractCallbackListener.resetGlobalCallbackEventList();
        }
    }

    /**
     * Verify that default listener classes will not fire for entities that request exclusion.
     * <p>
     * Points: 34
     */
    public void testCallback003(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testCallback003: Missing context and/or resources.  Cannot execute the test.");
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

        int id = 3;

        // Execute Test Case
        try {
            System.out.println("CallbackTestLogic.testCallback003(): Begin");
            AbstractCallbackListener.setTargetPostLoadLifeCycleWithRuntimeException(null);

            // Test @PrePersist and @PostPersist
            System.out.println("1) Test @PrePersist and @PostPersist");
            resetDefaultListeners();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + "...");
            ICallbackEntity entity_persist = (ICallbackEntity) constructNewEntityObject(targetEntityType);
            entity_persist.setId(id);
            entity_persist.setName("testCallback003-CallbackEntity-" + id);

            // Assert no lifecycle callbacks have been fired yet (4 points)
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for DefaultCallbackListenerPackage",
                                0, DefaultCallbackListenerPackage.getSingleton().getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for DefaultCallbackListenerPrivate",
                                0, DefaultCallbackListenerPrivate.getSingleton().getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for DefaultCallbackListenerProtected",
                                0, DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for DefaultCallbackListenerPublic",
                                0, DefaultCallbackListenerPublic.getSingleton().getFiredLifeCycleSet().size());

            System.out.println("Persisting " + entity_persist + " ...");
            jpaResource.getEm().persist(entity_persist);

            // Assert @PrePersist has NOT fired (4 points)
            Assert.assertFalse("Assert @PrePersist has NOT fired for DefaultCallbackListenerPackage.",
                               DefaultCallbackListenerPackage.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertFalse("Assert @PrePersist has NOT fired for DefaultCallbackListenerPrivate.",
                               DefaultCallbackListenerPrivate.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertFalse("Assert @PrePersist has NOT fired for DefaultCallbackListenerProtected.",
                               DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertFalse("Assert @PrePersist has NOT fired for DefaultCallbackListenerProtected.",
                               DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Assert @PostPersist has NOT fired (4 points)
            Assert.assertFalse("Assert @PostPersist has NOT fired for DefaultCallbackListenerPackage.",
                               DefaultCallbackListenerPackage.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostPersist));
            Assert.assertFalse("Assert @PostPersist has NOT fired for DefaultCallbackListenerPrivate.",
                               DefaultCallbackListenerPrivate.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostPersist));
            Assert.assertFalse("Assert @PostPersist has NOT fired for DefaultCallbackListenerProtected.",
                               DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostPersist));
            Assert.assertFalse("Assert @PostPersist has NOT fired for DefaultCallbackListenerProtected.",
                               DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostPersist));

            // Callbacks Observed
            System.out.println("Callbacks Observed:");
            System.out.println("DefaultCallbackListenerPackage Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPackage.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerPrivate Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPrivate.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            // Test @PostLoad
            System.out.println("2) Test @PostLoad");
            resetDefaultListeners();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + "(id=" + id + ")...");
            ICallbackEntity entity_find = (ICallbackEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            Assert.assertNotNull("Assert find() did not return null.", entity_find);

            // Assert @PostLoad has NOT fired (4 points)
            Assert.assertFalse("Assert @PostPersist has NOT fired for DefaultCallbackListenerPackage.",
                               DefaultCallbackListenerPackage.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostLoad));
            Assert.assertFalse("Assert @PostPersist has NOT fired for DefaultCallbackListenerPrivate.",
                               DefaultCallbackListenerPrivate.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostLoad));
            Assert.assertFalse("Assert @PostPersist has NOT fired for DefaultCallbackListenerProtected.",
                               DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostLoad));
            Assert.assertFalse("Assert @PostPersist has NOT fired for DefaultCallbackListenerProtected.",
                               DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostLoad));

            // Callbacks Observed
            System.out.println("Callbacks Observed:");
            System.out.println("DefaultCallbackListenerPackage Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPackage.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerPrivate Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPrivate.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            // Test @PreUpdate and @PostUpdate
            // Note when @PreUpdate fires is vendor specific, it could fire each time the object's persistent fields
            // are changed, or it could be fired when the transaction is in the process of committing.
            System.out.println("3) Test @PreUpdate and @PostUpdate");
            resetDefaultListeners();

            System.out.println("Updating " + targetEntityType.getEntityName() + "(id=" + id + ")'s name field...");
            entity_find.setName("Mutated Name");

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Assert @PreUpdate has NOT fired (4 points)
            Assert.assertFalse("Assert @PreUpdate has NOT fired for DefaultCallbackListenerPackage.",
                               DefaultCallbackListenerPackage.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PreUpdate));
            Assert.assertFalse("Assert @PreUpdate has NOT fired for DefaultCallbackListenerPrivate.",
                               DefaultCallbackListenerPrivate.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PreUpdate));
            Assert.assertFalse("Assert @PreUpdate has NOT fired for DefaultCallbackListenerProtected.",
                               DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PreUpdate));
            Assert.assertFalse("Assert @PreUpdate has NOT fired for DefaultCallbackListenerProtected.",
                               DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PreUpdate));

            // Assert @PreUpdate has NOT fired (4 points)
            Assert.assertFalse("Assert @PostUpdate has NOT fired for DefaultCallbackListenerPackage.",
                               DefaultCallbackListenerPackage.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostUpdate));
            Assert.assertFalse("Assert @PostUpdate has NOT fired for DefaultCallbackListenerPrivate.",
                               DefaultCallbackListenerPrivate.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostUpdate));
            Assert.assertFalse("Assert @PostUpdate has NOT fired for DefaultCallbackListenerProtected.",
                               DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostUpdate));
            Assert.assertFalse("Assert @PostUpdate has NOT fired for DefaultCallbackListenerProtected.",
                               DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostUpdate));

            // Callbacks Observed
            System.out.println("Callbacks Observed:");
            System.out.println("DefaultCallbackListenerPackage Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPackage.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerPrivate Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPrivate.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            // Test @PreRemove and @PostRemove
            System.out.println("4) Test @PreRemove and @PostRemove");
            resetDefaultListeners();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + "(id=" + id + ")...");
            ICallbackEntity entity_remove = (ICallbackEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            Assert.assertNotNull("Assert find() did not return null.", entity_remove);

            System.out.println("Calling remove() on " + targetEntityType.getEntityName() + "(id=" + id + ") ...");
            jpaResource.getEm().remove(entity_remove);

            // Assert @PreRemove has NOT fired (4 points)
            Assert.assertFalse("Assert @PreRemove has NOT fired for DefaultCallbackListenerPackage.",
                               DefaultCallbackListenerPackage.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PreRemove));
            Assert.assertFalse("Assert @PreRemove has NOT fired for DefaultCallbackListenerPrivate.",
                               DefaultCallbackListenerPrivate.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PreRemove));
            Assert.assertFalse("Assert @PreRemove has NOT fired for DefaultCallbackListenerProtected.",
                               DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PreRemove));
            Assert.assertFalse("Assert @PreRemove has NOT fired for DefaultCallbackListenerProtected.",
                               DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PreRemove));

            // Callbacks Observed
            System.out.println("Callbacks Observed:");
            System.out.println("DefaultCallbackListenerPackage Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPackage.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerPrivate Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPrivate.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Assert @PostRemove has NOT fired (4 points)
            Assert.assertFalse("Assert @PostRemove has NOT fired for DefaultCallbackListenerPackage.",
                               DefaultCallbackListenerPackage.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostRemove));
            Assert.assertFalse("Assert @PostRemove has NOT fired for DefaultCallbackListenerPrivate.",
                               DefaultCallbackListenerPrivate.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostRemove));
            Assert.assertFalse("Assert @PostRemove has NOT fired for DefaultCallbackListenerProtected.",
                               DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostRemove));
            Assert.assertFalse("Assert @PostRemove has NOT fired for DefaultCallbackListenerProtected.",
                               DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PostRemove));

            // Callbacks Observed
            System.out.println("Callbacks Observed:");
            System.out.println("DefaultCallbackListenerPackage Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPackage.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerPrivate Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPrivate.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("CallbackTestLogic.testCallback003(): End");
            AbstractCallbackListener.resetGlobalCallbackEventList();
        }
    }

    /**
     * Test callback methods on entity/msc-declared listener classes.
     * <p>
     * Points: 34
     */
    public void testCallback004(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testCallback004: Missing context and/or resources.  Cannot execute the test.");
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

        AbstractCallbackListener callbackListenerPackage = null;
        AbstractCallbackListener callbackListenerPrivate = null;
        AbstractCallbackListener callbackListenerProtected = null;
        AbstractCallbackListener callbackListenerPublic = null;

        if (entityName.toLowerCase().startsWith("ano")) {
            System.out.println("Identifying Annotation-Declared Listener singletons...");
            callbackListenerPackage = AnoCallbackListenerPackage.getSingleton();
            callbackListenerPrivate = AnoCallbackListenerPrivate.getSingleton();
            callbackListenerProtected = AnoCallbackListenerProtected.getSingleton();
            callbackListenerPublic = AnoCallbackListenerPublic.getSingleton();
        } else {
            System.out.println("Identifying XML-Declared Listener singletons...");
            callbackListenerPackage = XMLCallbackListenerPackage.getSingleton();
            callbackListenerPrivate = XMLCallbackListenerPrivate.getSingleton();
            callbackListenerProtected = XMLCallbackListenerProtected.getSingleton();
            callbackListenerPublic = XMLCallbackListenerPublic.getSingleton();
        }

        int id = 4;

        // Execute Test Case
        try {
            System.out.println("CallbackTestLogic.testCallback004(): Begin");
            AbstractCallbackListener.setTargetPostLoadLifeCycleWithRuntimeException(null);

            // Test @PrePersist and @PostPersist
            System.out.println("1) Test @PrePersist and @PostPersist");
            resetEntityListeners();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + "...");
            ICallbackEntity entity_persist = (ICallbackEntity) constructNewEntityObject(targetEntityType);
            entity_persist.setId(id);
            entity_persist.setName("testCallback004-CallbackEntity-" + id);

            // Assert no lifecycle callbacks have been fired yet (4 points)
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for " + callbackListenerPackage.getClass().getSimpleName(),
                                0, callbackListenerPackage.getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for " + callbackListenerPrivate.getClass().getSimpleName(),
                                0, callbackListenerPrivate.getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for " + callbackListenerProtected.getClass().getSimpleName(),
                                0, callbackListenerProtected.getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for " + callbackListenerPublic.getClass().getSimpleName(),
                                0, callbackListenerPublic.getFiredLifeCycleSet().size());

            System.out.println("Persisting " + entity_persist + " ...");
            jpaResource.getEm().persist(entity_persist);

            // Assert @PrePersist has fired (4 points)
            Assert.assertTrue("Assert @PrePersist has fired for " + callbackListenerPackage.getClass().getSimpleName(),
                              callbackListenerPackage.getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertTrue("Assert @PrePersist has fired for " + callbackListenerPrivate.getClass().getSimpleName(),
                              callbackListenerPrivate.getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertTrue("Assert @PrePersist has fired for " + callbackListenerProtected.getClass().getSimpleName(),
                              callbackListenerProtected.getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertTrue("Assert @PrePersist has fired for " + callbackListenerPublic.getClass().getSimpleName(),
                              callbackListenerPublic.getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Assert @PostPersist has fired (4 points)
            Assert.assertTrue("Assert @PostPersist has fired for " + callbackListenerPackage.getClass().getSimpleName(),
                              callbackListenerPackage.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostPersist));
            Assert.assertTrue("Assert @PostPersist has fired for " + callbackListenerPrivate.getClass().getSimpleName(),
                              callbackListenerPrivate.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostPersist));
            Assert.assertTrue("Assert @PostPersist has fired for " + callbackListenerProtected.getClass().getSimpleName(),
                              callbackListenerProtected.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostPersist));
            Assert.assertTrue("Assert @PostPersist has fired for " + callbackListenerPublic.getClass().getSimpleName(),
                              callbackListenerPublic.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostPersist));

            // Callbacks Observed
            System.out.println("Callbacks Observed:");
            System.out.println(callbackListenerPackage.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPackage.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerPrivate.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPrivate.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerProtected.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerProtected.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerPublic.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPublic.getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            // Test @PostLoad
            System.out.println("2) Test @PostLoad");
            resetEntityListeners();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + "(id=" + id + ")...");
            ICallbackEntity entity_find = (ICallbackEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            Assert.assertNotNull("Assert find() did not return null.", entity_find);

            // Assert @PostLoad has fired (4 points)
            Assert.assertTrue("Assert @PostLoad has fired for " + callbackListenerPackage.getClass().getSimpleName(),
                              callbackListenerPackage.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostLoad));
            Assert.assertTrue("Assert @PostLoad has fired for " + callbackListenerPrivate.getClass().getSimpleName(),
                              callbackListenerPrivate.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostLoad));
            Assert.assertTrue("Assert @PostLoad has fired for " + callbackListenerProtected.getClass().getSimpleName(),
                              callbackListenerProtected.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostLoad));
            Assert.assertTrue("Assert @PostLoad has fired for " + callbackListenerPublic.getClass().getSimpleName(),
                              callbackListenerPublic.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostLoad));

            // Callbacks Observed
            System.out.println("Callbacks Observed:");
            System.out.println(callbackListenerPackage.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPackage.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerPrivate.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPrivate.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerProtected.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerProtected.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerPublic.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPublic.getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            // Test @PreUpdate and @PostUpdate
            // Note when @PreUpdate fires is vendor specific, it could fire each time the object's persistent fields
            // are changed, or it could be fired when the transaction is in the process of committing.
            System.out.println("3) Test @PreUpdate and @PostUpdate");
            resetEntityListeners();

            System.out.println("Updating " + targetEntityType.getEntityName() + "(id=" + id + ")'s name field...");
            entity_find.setName("Mutated Name");

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Assert @PreUpdate has fired (4 points)
            Assert.assertTrue("Assert @PreUpdate has fired for " + callbackListenerPackage.getClass().getSimpleName(),
                              callbackListenerPackage.getFiredLifeCycleSet().contains(CallbackLifeCycle.PreUpdate));
            Assert.assertTrue("Assert @PreUpdate has fired for " + callbackListenerPrivate.getClass().getSimpleName(),
                              callbackListenerPrivate.getFiredLifeCycleSet().contains(CallbackLifeCycle.PreUpdate));
            Assert.assertTrue("Assert @PreUpdate has fired for " + callbackListenerProtected.getClass().getSimpleName(),
                              callbackListenerProtected.getFiredLifeCycleSet().contains(CallbackLifeCycle.PreUpdate));
            Assert.assertTrue("Assert @PreUpdate has fired for " + callbackListenerPublic.getClass().getSimpleName(),
                              callbackListenerPublic.getFiredLifeCycleSet().contains(CallbackLifeCycle.PreUpdate));

            // Assert @PreUpdate has fired (4 points)
            Assert.assertTrue("Assert @PostUpdate has fired for DefaultCallbackListenerPackage.",
                              callbackListenerPackage.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostUpdate));
            Assert.assertTrue("Assert @PostUpdate has fired for DefaultCallbackListenerPrivate.",
                              callbackListenerPrivate.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostUpdate));
            Assert.assertTrue("Assert @PostUpdate has fired for DefaultCallbackListenerProtected.",
                              callbackListenerProtected.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostUpdate));
            Assert.assertTrue("Assert @PostUpdate has fired for DefaultCallbackListenerPublic.",
                              callbackListenerPublic.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostUpdate));

            // Callbacks Observed
            System.out.println("Callbacks Observed:");
            System.out.println(callbackListenerPackage.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPackage.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerPrivate.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPrivate.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerProtected.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerProtected.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerPublic.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPublic.getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            // Test @PreRemove and @PostRemove
            System.out.println("4) Test @PreRemove and @PostRemove");
            resetEntityListeners();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + "(id=" + id + ")...");
            ICallbackEntity entity_remove = (ICallbackEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            Assert.assertNotNull("Assert find() did not return null.", entity_remove);

            System.out.println("Calling remove() on " + targetEntityType.getEntityName() + "(id=" + id + ") ...");
            jpaResource.getEm().remove(entity_remove);

            // Assert @PreRemove has fired (4 points)
            Assert.assertTrue("Assert @PreRemove has fired for " + callbackListenerPackage.getClass().getSimpleName(),
                              callbackListenerPackage.getFiredLifeCycleSet().contains(CallbackLifeCycle.PreRemove));
            Assert.assertTrue("Assert @PreRemove has fired for " + callbackListenerPrivate.getClass().getSimpleName(),
                              callbackListenerPrivate.getFiredLifeCycleSet().contains(CallbackLifeCycle.PreRemove));
            Assert.assertTrue("Assert @PreRemove has fired for " + callbackListenerProtected.getClass().getSimpleName(),
                              callbackListenerProtected.getFiredLifeCycleSet().contains(CallbackLifeCycle.PreRemove));
            Assert.assertTrue("Assert @PreRemove has fired for " + callbackListenerPublic.getClass().getSimpleName(),
                              callbackListenerPublic.getFiredLifeCycleSet().contains(CallbackLifeCycle.PreRemove));

            // Callbacks Observed
            System.out.println("Callbacks Observed:");
            System.out.println(callbackListenerPackage.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPackage.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerPrivate.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPrivate.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerProtected.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerProtected.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerPublic.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPublic.getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Assert @PostRemove has fired (4 points)
            Assert.assertTrue("Assert @PostRemove has fired for " + callbackListenerPackage.getClass().getSimpleName(),
                              callbackListenerPackage.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostRemove));
            Assert.assertTrue("Assert @PostRemove has fired for " + callbackListenerPrivate.getClass().getSimpleName(),
                              callbackListenerPrivate.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostRemove));
            Assert.assertTrue("Assert @PostRemove has fired for " + callbackListenerProtected.getClass().getSimpleName(),
                              callbackListenerProtected.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostRemove));
            Assert.assertTrue("Assert @PostRemove has fired for " + callbackListenerPublic.getClass().getSimpleName(),
                              callbackListenerPublic.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostRemove));

            // Callbacks Observed
            System.out.println("Callbacks Observed:");
            System.out.println(callbackListenerPackage.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPackage.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerPrivate.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPrivate.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerProtected.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerProtected.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerPublic.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPublic.getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("CallbackTestLogic.testCallback004(): End");
            AbstractCallbackListener.resetGlobalCallbackEventList();
        }
    }

    /**
     * Verify that mapped-superclass defined listener classes will not fire for entities that request exclusion.
     * <p>
     * Points: 34
     */
    public void testCallback005(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                Object managedComponentObject) throws Throwable {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testCallback005: Missing context and/or resources.  Cannot execute the test.");
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

        AbstractCallbackListener callbackListenerPackage = null;
        AbstractCallbackListener callbackListenerPrivate = null;
        AbstractCallbackListener callbackListenerProtected = null;
        AbstractCallbackListener callbackListenerPublic = null;

        if (entityName.toLowerCase().startsWith("ano")) {
            System.out.println("Identifying Annotation-Declared Listener singletons...");
            callbackListenerPackage = AnoCallbackListenerPackage.getSingleton();
            callbackListenerPrivate = AnoCallbackListenerPrivate.getSingleton();
            callbackListenerProtected = AnoCallbackListenerProtected.getSingleton();
            callbackListenerPublic = AnoCallbackListenerPublic.getSingleton();
        } else {
            System.out.println("Identifying XML-Declared Listener singletons...");
            callbackListenerPackage = XMLCallbackListenerPackage.getSingleton();
            callbackListenerPrivate = XMLCallbackListenerPrivate.getSingleton();
            callbackListenerProtected = XMLCallbackListenerProtected.getSingleton();
            callbackListenerPublic = XMLCallbackListenerPublic.getSingleton();
        }

        int id = 5;

        // Execute Test Case
        try {
            System.out.println("CallbackTestLogic.testCallback005(): Begin");
            AbstractCallbackListener.setTargetPostLoadLifeCycleWithRuntimeException(null);

            // Test @PrePersist and @PostPersist
            System.out.println("1) Test @PrePersist and @PostPersist");
            resetEntityListeners();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + "...");
            ICallbackEntity entity_persist = (ICallbackEntity) constructNewEntityObject(targetEntityType);
            entity_persist.setId(id);
            entity_persist.setName("testCallback005-CallbackEntity-" + id);

            // Assert no lifecycle callbacks have been fired yet (4 points)
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for " + callbackListenerPackage.getClass().getSimpleName(),
                                0, callbackListenerPackage.getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for " + callbackListenerPrivate.getClass().getSimpleName(),
                                0, callbackListenerPrivate.getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for " + callbackListenerProtected.getClass().getSimpleName(),
                                0, callbackListenerProtected.getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for " + callbackListenerPublic.getClass().getSimpleName(),
                                0, callbackListenerPublic.getFiredLifeCycleSet().size());

            System.out.println("Persisting " + entity_persist + " ...");
            jpaResource.getEm().persist(entity_persist);

            // Assert @PrePersist has NOT fired (4 points)
            Assert.assertFalse("Assert @PrePersist has NOT fired for " + callbackListenerPackage.getClass().getSimpleName(),
                               callbackListenerPackage.getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertFalse("Assert @PrePersist has NOT fired for " + callbackListenerPrivate.getClass().getSimpleName(),
                               callbackListenerPrivate.getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertFalse("Assert @PrePersist has NOT fired for " + callbackListenerProtected.getClass().getSimpleName(),
                               callbackListenerProtected.getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertFalse("Assert @PrePersist has NOT fired for " + callbackListenerPublic.getClass().getSimpleName(),
                               callbackListenerPublic.getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Assert @PostPersist has NOT fired (4 points)
            Assert.assertFalse("Assert @PostPersist has NOT fired for " + callbackListenerPackage.getClass().getSimpleName(),
                               callbackListenerPackage.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostPersist));
            Assert.assertFalse("Assert @PostPersist has NOT fired for " + callbackListenerPrivate.getClass().getSimpleName(),
                               callbackListenerPrivate.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostPersist));
            Assert.assertFalse("Assert @PostPersist has NOT fired for " + callbackListenerProtected.getClass().getSimpleName(),
                               callbackListenerProtected.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostPersist));
            Assert.assertFalse("Assert @PostPersist has NOT fired for " + callbackListenerPublic.getClass().getSimpleName(),
                               callbackListenerPublic.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostPersist));

            // Callbacks Observed
            System.out.println("Callbacks Observed:");
            System.out.println(callbackListenerPackage.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPackage.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerPrivate.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPrivate.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerProtected.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerProtected.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerPublic.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPublic.getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            // Test @PostLoad
            System.out.println("2) Test @PostLoad");
            resetEntityListeners();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + "(id=" + id + ")...");
            ICallbackEntity entity_find = (ICallbackEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            Assert.assertNotNull("Assert find() did not return null.", entity_find);

            // Assert @PostLoad has NOT fired (4 points)
            Assert.assertFalse("Assert @PostPersist has NOT fired for " + callbackListenerPackage.getClass().getSimpleName(),
                               callbackListenerPackage.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostLoad));
            Assert.assertFalse("Assert @PostPersist has NOT fired for " + callbackListenerPrivate.getClass().getSimpleName(),
                               callbackListenerPrivate.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostLoad));
            Assert.assertFalse("Assert @PostPersist has NOT fired for " + callbackListenerProtected.getClass().getSimpleName(),
                               callbackListenerProtected.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostLoad));
            Assert.assertFalse("Assert @PostPersist has NOT fired for " + callbackListenerPublic.getClass().getSimpleName(),
                               callbackListenerPublic.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostLoad));

            // Callbacks Observed
            System.out.println("Callbacks Observed:");
            System.out.println(callbackListenerPackage.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPackage.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerPrivate.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPrivate.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerProtected.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerProtected.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerPublic.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPublic.getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            // Test @PreUpdate and @PostUpdate
            // Note when @PreUpdate fires is vendor specific, it could fire each time the object's persistent fields
            // are changed, or it could be fired when the transaction is in the process of committing.
            System.out.println("3) Test @PreUpdate and @PostUpdate");
            resetEntityListeners();

            System.out.println("Updating " + targetEntityType.getEntityName() + "(id=" + id + ")'s name field...");
            entity_find.setName("Mutated Name");

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Assert @PreUpdate has NOT fired (4 points)
            Assert.assertFalse("Assert @PreUpdate has NOT fired for " + callbackListenerPackage.getClass().getSimpleName(),
                               callbackListenerPackage.getFiredLifeCycleSet().contains(CallbackLifeCycle.PreUpdate));
            Assert.assertFalse("Assert @PreUpdate has NOT fired for " + callbackListenerPrivate.getClass().getSimpleName(),
                               callbackListenerPrivate.getFiredLifeCycleSet().contains(CallbackLifeCycle.PreUpdate));
            Assert.assertFalse("Assert @PreUpdate has NOT fired for " + callbackListenerProtected.getClass().getSimpleName(),
                               callbackListenerProtected.getFiredLifeCycleSet().contains(CallbackLifeCycle.PreUpdate));
            Assert.assertFalse("Assert @PreUpdate has NOT fired for " + callbackListenerPublic.getClass().getSimpleName(),
                               callbackListenerPublic.getFiredLifeCycleSet().contains(CallbackLifeCycle.PreUpdate));

            // Assert @PreUpdate has NOT fired (4 points)
            Assert.assertFalse("Assert @PostUpdate has NOT fired for " + callbackListenerPackage.getClass().getSimpleName(),
                               callbackListenerPackage.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostUpdate));
            Assert.assertFalse("Assert @PostUpdate has NOT fired for " + callbackListenerProtected.getClass().getSimpleName(),
                               callbackListenerPrivate.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostUpdate));
            Assert.assertFalse("Assert @PostUpdate has NOT fired for " + callbackListenerProtected.getClass().getSimpleName(),
                               callbackListenerProtected.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostUpdate));
            Assert.assertFalse("Assert @PostUpdate has NOT fired for " + callbackListenerPublic.getClass().getSimpleName(),
                               callbackListenerPublic.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostUpdate));

            // Callbacks Observed
            System.out.println("Callbacks Observed:");
            System.out.println(callbackListenerPackage.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPackage.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerPrivate.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPrivate.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerProtected.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerProtected.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerPublic.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPublic.getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            // Test @PreRemove and @PostRemove
            System.out.println("4) Test @PreRemove and @PostRemove");
            resetEntityListeners();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + "(id=" + id + ")...");
            ICallbackEntity entity_remove = (ICallbackEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            Assert.assertNotNull("Assert find() did not return null.", entity_remove);

            System.out.println("Calling remove() on " + targetEntityType.getEntityName() + "(id=" + id + ") ...");
            jpaResource.getEm().remove(entity_remove);

            // Assert @PreRemove has NOT fired (4 points)
            Assert.assertFalse("Assert @PreRemove has NOT fired for " + callbackListenerPackage.getClass().getSimpleName(),
                               callbackListenerPackage.getFiredLifeCycleSet().contains(CallbackLifeCycle.PreRemove));
            Assert.assertFalse("Assert @PreRemove has NOT fired for " + callbackListenerPrivate.getClass().getSimpleName(),
                               callbackListenerPrivate.getFiredLifeCycleSet().contains(CallbackLifeCycle.PreRemove));
            Assert.assertFalse("Assert @PreRemove has NOT fired for " + callbackListenerPublic.getClass().getSimpleName(),
                               callbackListenerProtected.getFiredLifeCycleSet().contains(CallbackLifeCycle.PreRemove));
            Assert.assertFalse("Assert @PreRemove has NOT fired for " + callbackListenerPublic.getClass().getSimpleName(),
                               callbackListenerPublic.getFiredLifeCycleSet().contains(CallbackLifeCycle.PreRemove));

            // Callbacks Observed
            System.out.println("Callbacks Observed:");
            System.out.println(callbackListenerPackage.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPackage.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerPrivate.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPrivate.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerProtected.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerProtected.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerPublic.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPublic.getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Assert @PostRemove has NOT fired (4 points)
            Assert.assertFalse("Assert @PostRemove has NOT fired for " + callbackListenerPackage.getClass().getSimpleName(),
                               callbackListenerPackage.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostRemove));
            Assert.assertFalse("Assert @PostRemove has NOT fired for " + callbackListenerProtected.getClass().getSimpleName(),
                               callbackListenerPrivate.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostRemove));
            Assert.assertFalse("Assert @PostRemove has NOT fired for " + callbackListenerProtected.getClass().getSimpleName(),
                               callbackListenerProtected.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostRemove));
            Assert.assertFalse("Assert @PostRemove has NOT fired for " + callbackListenerPublic.getClass().getSimpleName(),
                               callbackListenerPublic.getFiredLifeCycleSet().contains(CallbackLifeCycle.PostRemove));

            // Callbacks Observed
            System.out.println("Callbacks Observed:");
            System.out.println(callbackListenerPackage.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPackage.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerPrivate.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPrivate.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerProtected.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerProtected.getCallbackEventList()) {
                System.out.println(cr.toString());
            }
            System.out.println(callbackListenerPublic.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPublic.getCallbackEventList()) {
                System.out.println(cr.toString());
            }

            System.out.println("Ending test.");
        } finally {
            System.out.println("CallbackTestLogic.testCallback005(): End");
            AbstractCallbackListener.resetGlobalCallbackEventList();
        }
    }

    private void resetDefaultListeners() {
        System.out.println("Resetting Default Listener Callback Statistics...");

        DefaultCallbackListenerPackage.reset();
        DefaultCallbackListenerPrivate.reset();
        DefaultCallbackListenerProtected.reset();
        DefaultCallbackListenerPublic.reset();

        System.out.println("Default Listener Callback Statistics Reset.");
    }

    private void resetEntityListeners() {
        System.out.println("Resetting Entity Listener Callback Statistics...");

        AnoCallbackListenerPackage.reset();
        AnoCallbackListenerPrivate.reset();
        AnoCallbackListenerProtected.reset();
        AnoCallbackListenerPublic.reset();

        XMLCallbackListenerPackage.reset();
        XMLCallbackListenerPrivate.reset();
        XMLCallbackListenerProtected.reset();
        XMLCallbackListenerPublic.reset();

        System.out.println("Entity Listener Callback Statistics Reset.");
    }
}
