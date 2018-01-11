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
import javax.transaction.UserTransaction;

import org.junit.Assert;

import jpa10callback.AbstractCallbackListener;
import jpa10callback.CallbackRecord;
import jpa10callback.CallbackRecord.CallbackLifeCycle;
import jpa10callback.entity.ICallbackEntity;
import jpa10callback.listeners.ano.AnoCallbackListenerPackage;
import jpa10callback.listeners.ano.AnoCallbackListenerPrivate;
import jpa10callback.listeners.ano.AnoCallbackListenerProtected;
import jpa10callback.listeners.ano.AnoCallbackListenerPublic;
import jpa10callback.listeners.defaultlistener.DefaultCallbackListenerPackage;
import jpa10callback.listeners.defaultlistener.DefaultCallbackListenerPrivate;
import jpa10callback.listeners.defaultlistener.DefaultCallbackListenerProtected;
import jpa10callback.listeners.defaultlistener.DefaultCallbackListenerPublic;
import jpa10callback.listeners.xml.XMLCallbackListenerPackage;
import jpa10callback.listeners.xml.XMLCallbackListenerPrivate;
import jpa10callback.listeners.xml.XMLCallbackListenerProtected;
import jpa10callback.listeners.xml.XMLCallbackListenerPublic;

public class CallbackTestLogic {
    private final static PrintStream out = System.out;
    private final static Random rand = new Random();
    private final static AtomicInteger nextId = new AtomicInteger(rand.nextInt());

    /**
     * Test callback methods on entity classes. Supports testing of entities declared by annotation and
     * XML, and supports stand-alone entity classes and entities that gain callback methods from
     * mapped superclasses.
     *
     */
    public void testCallback001(EntityManager em, UserTransaction tx, Class<?> entityClass) throws Exception {
        final String testName = "testCallback001";
        out.println("Starting " + testName + ": jta=" + (tx != null) + " , entityClass=" + entityClass);

        final int id = nextId.getAndIncrement();
        try {
            Assert.assertNotNull(em);
            Assert.assertNotNull(entityClass);

            AbstractCallbackListener.setTargetPostLoadLifeCycleWithRuntimeException(null);

            // Test @PrePersist and @PostPersist
            out.println("1) Test @PrePersist and @PostPersist");
            em.clear();

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
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet.",
                                0, ((AbstractCallbackListener) entity_persist).getFiredLifeCycleSet().size());

            out.println("Persisting " + entity_persist + " ...");
            em.persist(entity_persist);
            Assert.assertTrue("Assert @PrePersist has fired.",
                              (((AbstractCallbackListener) entity_persist).getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist)));

            out.println("Committing transaction...");
            if (tx == null) {
                em.getTransaction().commit();
            } else {
                tx.commit();
            }
            Assert.assertTrue("Assert @PostPersist has fired.",
                              (((AbstractCallbackListener) entity_persist).getFiredLifeCycleSet().contains(CallbackLifeCycle.PostPersist)));

            // Test @PostLoad
            out.println("2) Test @PostLoad");
            em.clear();

            out.println("Beginning new transaction...");
            if (tx == null) {
                em.getTransaction().begin();
            } else {
                tx.begin();
                em.joinTransaction();
            }

            out.println("Finding " + entityClass.getName() + " with id = " + id + " ...");
            ICallbackEntity entity_find = (ICallbackEntity) em.find(entityClass, id);
            Assert.assertNotNull("Assert find() did not return null.", entity_find);
            Assert.assertTrue("Assert @PostLoad has fired.",
                              (((AbstractCallbackListener) entity_find).getFiredLifeCycleSet().contains(CallbackLifeCycle.PostLoad)));

            out.println("Callbacks Observed:");
            for (CallbackRecord cr : ((AbstractCallbackListener) entity_find).getCallbackEventList()) {
                out.println(cr.toString());
            }

            // Test @PreUpdate and @PostUpdate
            // Note when @PreUpdate fires is vendor specific, it could fire each time the object's persistent fields
            // are changed, or it could be fired when the transaction is in the process of committing.
            out.println("3) Test @PreUpdate and @PostUpdate");
            out.println("Updating " + entityClass + "(id=" + id + ")'s name field...");
            entity_find.setName("Mutated Name");

            out.println("Committing transaction...");
            if (tx == null) {
                em.getTransaction().commit();
            } else {
                tx.commit();
            }
            Assert.assertTrue("Assert @PreUpdate has fired.",
                              (((AbstractCallbackListener) entity_find).getFiredLifeCycleSet().contains(CallbackLifeCycle.PreUpdate)));
            Assert.assertTrue("Assert @PostUpdate has fired.",
                              (((AbstractCallbackListener) entity_find).getFiredLifeCycleSet().contains(CallbackLifeCycle.PostUpdate)));

            out.println("Callbacks Observed:");
            for (CallbackRecord cr : ((AbstractCallbackListener) entity_find).getCallbackEventList()) {
                out.println(cr.toString());
            }

            // Test @PreRemove and @PostRemove
            out.println("4) Test @PreRemove and @PostRemove");

            // Clear persistence context
            out.println("Clearing persistence context...");
            em.clear();

            out.println("Beginning new transaction...");
            if (tx == null) {
                em.getTransaction().begin();
            } else {
                tx.begin();
                em.joinTransaction();
            }

            out.println("Finding " + entityClass.getName() + " with id = " + id + " ...");
            ICallbackEntity entity_remove = (ICallbackEntity) em.find(entityClass, id);
            Assert.assertNotNull("Assert find() did not return null.", entity_remove);

            out.println("Calling remove() on " + entityClass.getName() + " with id = " + id + " ...");
            em.remove(entity_remove);
            Assert.assertTrue("Assert @PreRemove has fired.",
                              (((AbstractCallbackListener) entity_remove).getFiredLifeCycleSet().contains(CallbackLifeCycle.PreRemove)));

            out.println("Callbacks Observed:");
            for (CallbackRecord cr : ((AbstractCallbackListener) entity_remove).getCallbackEventList()) {
                out.println(cr.toString());
            }

            out.println("Committing transaction...");
            if (tx == null) {
                em.getTransaction().commit();
            } else {
                tx.commit();
            }
            Assert.assertTrue("Assert @PostRemove has fired.",
                              (((AbstractCallbackListener) entity_remove).getFiredLifeCycleSet().contains(CallbackLifeCycle.PostRemove)));

            out.println("Callbacks Observed:");
            for (CallbackRecord cr : ((AbstractCallbackListener) entity_remove).getCallbackEventList()) {
                out.println(cr.toString());
            }
        } finally {
            out.println("Ending " + testName);
        }
    }

    /**
     * Test callback methods on default listener classes.
     *
     */
    public void testCallback002(EntityManager em, UserTransaction tx, Class<?> entityClass) throws Exception {
        final String testName = "testCallback002";
        out.println("Starting " + testName + ": jta=" + (tx != null) + " , entityClass=" + entityClass);

        final int id = nextId.getAndIncrement();
        try {
            Assert.assertNotNull(em);
            Assert.assertNotNull(entityClass);

            resetDefaultListeners();
            em.clear();

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

            // Assert no lifecycle callbacks have been fired yet (4 points)
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for DefaultCallbackListenerPackage",
                                0, DefaultCallbackListenerPackage.getSingleton().getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for DefaultCallbackListenerPrivate",
                                0, DefaultCallbackListenerPrivate.getSingleton().getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for DefaultCallbackListenerProtected",
                                0, DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for DefaultCallbackListenerPublic",
                                0, DefaultCallbackListenerPublic.getSingleton().getFiredLifeCycleSet().size());

            out.println("Persisting " + entity_persist + " ...");
            em.persist(entity_persist);

            // Assert @PrePersist has fired (4 points)
            Assert.assertTrue("Assert @PrePersist has fired for DefaultCallbackListenerPackage.",
                              DefaultCallbackListenerPackage.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertTrue("Assert @PrePersist has fired for DefaultCallbackListenerPrivate.",
                              DefaultCallbackListenerPrivate.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertTrue("Assert @PrePersist has fired for DefaultCallbackListenerProtected.",
                              DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertTrue("Assert @PrePersist has fired for DefaultCallbackListenerProtected.",
                              DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));

            out.println("Committing transaction...");
            if (tx == null) {
                em.getTransaction().commit();
            } else {
                tx.commit();
            }

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
            out.println("Callbacks Observed:");
            out.println("DefaultCallbackListenerPackage Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPackage.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerPrivate Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPrivate.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }

            // Test @PostLoad
            out.println("2) Test @PostLoad");
            resetDefaultListeners();
            em.clear();

            out.println("Beginning new transaction...");
            if (tx == null) {
                em.getTransaction().begin();
            } else {
                tx.begin();
                em.joinTransaction();
            }

            out.println("Finding " + entityClass.getName() + " with id = " + id + " ...");
            ICallbackEntity entity_find = (ICallbackEntity) em.find(entityClass, id);
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
            out.println("Callbacks Observed:");
            out.println("DefaultCallbackListenerPackage Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPackage.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerPrivate Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPrivate.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }

            // Test @PreUpdate and @PostUpdate
            // Note when @PreUpdate fires is vendor specific, it could fire each time the object's persistent fields
            // are changed, or it could be fired when the transaction is in the process of committing.
            out.println("3) Test @PreUpdate and @PostUpdate");
            resetDefaultListeners();

            out.println("Updating " + entityClass + "(id=" + id + ")'s name field...");
            entity_find.setName("Mutated Name");

            out.println("Committing transaction...");
            if (tx == null) {
                em.getTransaction().commit();
            } else {
                tx.commit();
            }

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
            out.println("Callbacks Observed:");
            out.println("DefaultCallbackListenerPackage Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPackage.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerPrivate Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPrivate.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }

            // Test @PreRemove and @PostRemove
            out.println("4) Test @PreRemove and @PostRemove");
            resetDefaultListeners();
            em.clear();

            out.println("Beginning new transaction...");
            if (tx == null) {
                em.getTransaction().begin();
            } else {
                tx.begin();
                em.joinTransaction();
            }

            out.println("Finding " + entityClass.getName() + " with id = " + id + " ...");
            ICallbackEntity entity_remove = (ICallbackEntity) em.find(entityClass, id);
            Assert.assertNotNull("Assert find() did not return null.", entity_remove);

            out.println("Calling remove() on " + entityClass.getName() + " with id = " + id + " ...");
            em.remove(entity_remove);

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
            out.println("Callbacks Observed:");
            out.println("DefaultCallbackListenerPackage Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPackage.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerPrivate Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPrivate.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }

            out.println("Committing transaction...");
            if (tx == null) {
                em.getTransaction().commit();
            } else {
                tx.commit();
            }

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
            out.println("Callbacks Observed:");
            out.println("DefaultCallbackListenerPackage Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPackage.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerPrivate Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPrivate.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
        } finally {
            out.println("Ending " + testName);
        }
    }

    /**
     * Verify that default listener classes will not fire for entities that request exclusion.
     *
     */
    public void testCallback003(EntityManager em, UserTransaction tx, Class<?> entityClass) throws Exception {
        final String testName = "testCallback003";
        out.println("Starting " + testName + ": jta=" + (tx != null) + " , entityClass=" + entityClass);

        final int id = nextId.getAndIncrement();
        try {
            Assert.assertNotNull(em);
            Assert.assertNotNull(entityClass);

            // Test @PrePersist and @PostPersist
            out.println("1) Test @PrePersist and @PostPersist");
            resetDefaultListeners();
            em.clear();

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

            // Assert no lifecycle callbacks have been fired yet (4 points)
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for DefaultCallbackListenerPackage",
                                0, DefaultCallbackListenerPackage.getSingleton().getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for DefaultCallbackListenerPrivate",
                                0, DefaultCallbackListenerPrivate.getSingleton().getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for DefaultCallbackListenerProtected",
                                0, DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for DefaultCallbackListenerPublic",
                                0, DefaultCallbackListenerPublic.getSingleton().getFiredLifeCycleSet().size());

            out.println("Persisting " + entity_persist + " ...");
            em.persist(entity_persist);

            // Assert @PrePersist has NOT fired (4 points)
            Assert.assertFalse("Assert @PrePersist has NOT fired for DefaultCallbackListenerPackage.",
                               DefaultCallbackListenerPackage.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertFalse("Assert @PrePersist has NOT fired for DefaultCallbackListenerPrivate.",
                               DefaultCallbackListenerPrivate.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertFalse("Assert @PrePersist has NOT fired for DefaultCallbackListenerProtected.",
                               DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertFalse("Assert @PrePersist has NOT fired for DefaultCallbackListenerProtected.",
                               DefaultCallbackListenerProtected.getSingleton().getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));

            out.println("Committing transaction...");
            if (tx == null) {
                em.getTransaction().commit();
            } else {
                tx.commit();
            }

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
            out.println("Callbacks Observed:");
            out.println("DefaultCallbackListenerPackage Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPackage.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerPrivate Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPrivate.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }

            // Test @PostLoad
            out.println("2) Test @PostLoad");
            resetDefaultListeners();
            em.clear();

            out.println("Beginning new transaction...");
            if (tx == null) {
                em.getTransaction().begin();
            } else {
                tx.begin();
                em.joinTransaction();
            }

            out.println("Finding " + entityClass.getName() + " with id = " + id + " ...");
            ICallbackEntity entity_find = (ICallbackEntity) em.find(entityClass, id);
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
            out.println("Callbacks Observed:");
            out.println("DefaultCallbackListenerPackage Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPackage.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerPrivate Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPrivate.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }

            // Test @PreUpdate and @PostUpdate
            // Note when @PreUpdate fires is vendor specific, it could fire each time the object's persistent fields
            // are changed, or it could be fired when the transaction is in the process of committing.
            out.println("3) Test @PreUpdate and @PostUpdate");
            resetDefaultListeners();

            out.println("Updating " + entityClass + "(id=" + id + ")'s name field...");
            entity_find.setName("Mutated Name");

            out.println("Committing transaction...");
            if (tx == null) {
                em.getTransaction().commit();
            } else {
                tx.commit();
            }

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
            out.println("Callbacks Observed:");
            out.println("DefaultCallbackListenerPackage Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPackage.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerPrivate Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPrivate.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }

            // Test @PreRemove and @PostRemove
            out.println("4) Test @PreRemove and @PostRemove");
            resetDefaultListeners();
            em.clear();

            out.println("Beginning new transaction...");
            if (tx == null) {
                em.getTransaction().begin();
            } else {
                tx.begin();
                em.joinTransaction();
            }

            out.println("Finding " + entityClass.getName() + " with id = " + id + " ...");
            ICallbackEntity entity_remove = (ICallbackEntity) em.find(entityClass, id);
            Assert.assertNotNull("Assert find() did not return null.", entity_remove);

            out.println("Calling remove() on " + entityClass.getName() + " with id = " + id + " ...");
            em.remove(entity_remove);

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
            out.println("Callbacks Observed:");
            out.println("DefaultCallbackListenerPackage Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPackage.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerPrivate Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPrivate.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }

            out.println("Committing transaction...");
            if (tx == null) {
                em.getTransaction().commit();
            } else {
                tx.commit();
            }

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
            out.println("Callbacks Observed:");
            out.println("DefaultCallbackListenerPackage Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPackage.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerPrivate Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerPrivate.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println("DefaultCallbackListenerProtected Callbacks Observed:");
            for (CallbackRecord cr : (DefaultCallbackListenerProtected.getSingleton()).getCallbackEventList()) {
                out.println(cr.toString());
            }
        } finally {
            out.println("Ending " + testName);
        }
    }

    /**
     * Test callback methods on entity/msc-declared listener classes.
     *
     */
    public void testCallback004(EntityManager em, UserTransaction tx, Class<?> entityClass) throws Exception {
        final String testName = "testCallback004";
        out.println("Starting " + testName + ": jta=" + (tx != null) + " , entityClass=" + entityClass);

        final int id = nextId.getAndIncrement();
        try {
            Assert.assertNotNull(em);
            Assert.assertNotNull(entityClass);

            AbstractCallbackListener callbackListenerPackage = null;
            AbstractCallbackListener callbackListenerPrivate = null;
            AbstractCallbackListener callbackListenerProtected = null;
            AbstractCallbackListener callbackListenerPublic = null;

            if (entityClass.getName().contains("Ano")) {
                out.println("Identifying Annotation-Declared Listener singletons...");
                callbackListenerPackage = AnoCallbackListenerPackage.getSingleton();
                callbackListenerPrivate = AnoCallbackListenerPrivate.getSingleton();
                callbackListenerProtected = AnoCallbackListenerProtected.getSingleton();
                callbackListenerPublic = AnoCallbackListenerPublic.getSingleton();
            } else {
                out.println("Identifying XML-Declared Listener singletons...");
                callbackListenerPackage = XMLCallbackListenerPackage.getSingleton();
                callbackListenerPrivate = XMLCallbackListenerPrivate.getSingleton();
                callbackListenerProtected = XMLCallbackListenerProtected.getSingleton();
                callbackListenerPublic = XMLCallbackListenerPublic.getSingleton();
            }

            // Test @PrePersist and @PostPersist
            out.println("1) Test @PrePersist and @PostPersist");
            resetEntityListeners();
            em.clear();

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

            // Assert no lifecycle callbacks have been fired yet (4 points)
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for " + callbackListenerPackage.getClass().getSimpleName(),
                                0, callbackListenerPackage.getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for " + callbackListenerPrivate.getClass().getSimpleName(),
                                0, callbackListenerPrivate.getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for " + callbackListenerProtected.getClass().getSimpleName(),
                                0, callbackListenerProtected.getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for " + callbackListenerPublic.getClass().getSimpleName(),
                                0, callbackListenerPublic.getFiredLifeCycleSet().size());

            out.println("Persisting " + entity_persist + " ...");
            em.persist(entity_persist);

            // Assert @PrePersist has fired (4 points)
            Assert.assertTrue("Assert @PrePersist has fired for " + callbackListenerPackage.getClass().getSimpleName(),
                              callbackListenerPackage.getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertTrue("Assert @PrePersist has fired for " + callbackListenerPrivate.getClass().getSimpleName(),
                              callbackListenerPrivate.getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertTrue("Assert @PrePersist has fired for " + callbackListenerProtected.getClass().getSimpleName(),
                              callbackListenerProtected.getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertTrue("Assert @PrePersist has fired for " + callbackListenerPublic.getClass().getSimpleName(),
                              callbackListenerPublic.getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));

            out.println("Committing transaction...");
            if (tx == null) {
                em.getTransaction().commit();
            } else {
                tx.commit();
            }

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
            out.println("Callbacks Observed:");
            out.println(callbackListenerPackage.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPackage.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerPrivate.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPrivate.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerProtected.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerProtected.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerPublic.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPublic.getCallbackEventList()) {
                out.println(cr.toString());
            }

            // Test @PostLoad
            out.println("2) Test @PostLoad");
            resetEntityListeners();

            em.clear();

            out.println("Beginning new transaction...");
            if (tx == null) {
                em.getTransaction().begin();
            } else {
                tx.begin();
                em.joinTransaction();
            }

            out.println("Finding " + entityClass.getName() + " with id = " + id + " ...");
            ICallbackEntity entity_find = (ICallbackEntity) em.find(entityClass, id);
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
            out.println("Callbacks Observed:");
            out.println(callbackListenerPackage.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPackage.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerPrivate.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPrivate.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerProtected.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerProtected.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerPublic.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPublic.getCallbackEventList()) {
                out.println(cr.toString());
            }

            // Test @PreUpdate and @PostUpdate
            // Note when @PreUpdate fires is vendor specific, it could fire each time the object's persistent fields
            // are changed, or it could be fired when the transaction is in the process of committing.
            out.println("3) Test @PreUpdate and @PostUpdate");
            resetEntityListeners();

            out.println("Updating " + entityClass + "(id=" + id + ")'s name field...");
            entity_find.setName("Mutated Name");

            out.println("Committing transaction...");
            if (tx == null) {
                em.getTransaction().commit();
            } else {
                tx.commit();
            }

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
            out.println("Callbacks Observed:");
            out.println(callbackListenerPackage.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPackage.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerPrivate.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPrivate.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerProtected.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerProtected.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerPublic.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPublic.getCallbackEventList()) {
                out.println(cr.toString());
            }

            // Test @PreRemove and @PostRemove
            out.println("4) Test @PreRemove and @PostRemove");
            resetEntityListeners();
            em.clear();

            out.println("Beginning new transaction...");
            if (tx == null) {
                em.getTransaction().begin();
            } else {
                tx.begin();
                em.joinTransaction();
            }

            out.println("Finding " + entityClass.getName() + " with id = " + id + " ...");
            ICallbackEntity entity_remove = (ICallbackEntity) em.find(entityClass, id);
            Assert.assertNotNull("Assert find() did not return null.", entity_remove);

            out.println("Calling remove() on " + entityClass.getName() + " with id = " + id + " ...");
            em.remove(entity_remove);

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
            out.println("Callbacks Observed:");
            out.println(callbackListenerPackage.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPackage.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerPrivate.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPrivate.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerProtected.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerProtected.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerPublic.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPublic.getCallbackEventList()) {
                out.println(cr.toString());
            }

            out.println("Committing transaction...");
            if (tx == null) {
                em.getTransaction().commit();
            } else {
                tx.commit();
            }

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
            out.println("Callbacks Observed:");
            out.println(callbackListenerPackage.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPackage.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerPrivate.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPrivate.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerProtected.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerProtected.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerPublic.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPublic.getCallbackEventList()) {
                out.println(cr.toString());
            }
        } finally {
            out.println("Ending " + testName);
        }
    }

    /**
     * Verify that mapped-superclass defined listener classes will not fire for entities that request exclusion.
     *
     */
    public void testCallback005(EntityManager em, UserTransaction tx, Class<?> entityClass) throws Exception {
        final String testName = "testCallback005";
        out.println("Starting " + testName + ": jta=" + (tx != null) + " , entityClass=" + entityClass);

        final int id = nextId.getAndIncrement();
        try {
            Assert.assertNotNull(em);
            Assert.assertNotNull(entityClass);

            AbstractCallbackListener callbackListenerPackage = null;
            AbstractCallbackListener callbackListenerPrivate = null;
            AbstractCallbackListener callbackListenerProtected = null;
            AbstractCallbackListener callbackListenerPublic = null;

            if (entityClass.getName().contains("Ano")) {
                out.println("Identifying Annotation-Declared Listener singletons...");
                callbackListenerPackage = AnoCallbackListenerPackage.getSingleton();
                callbackListenerPrivate = AnoCallbackListenerPrivate.getSingleton();
                callbackListenerProtected = AnoCallbackListenerProtected.getSingleton();
                callbackListenerPublic = AnoCallbackListenerPublic.getSingleton();
            } else {
                out.println("Identifying XML-Declared Listener singletons...");
                callbackListenerPackage = XMLCallbackListenerPackage.getSingleton();
                callbackListenerPrivate = XMLCallbackListenerPrivate.getSingleton();
                callbackListenerProtected = XMLCallbackListenerProtected.getSingleton();
                callbackListenerPublic = XMLCallbackListenerPublic.getSingleton();
            }

            // Test @PrePersist and @PostPersist
            out.println("1) Test @PrePersist and @PostPersist");
            resetEntityListeners();

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

            // Assert no lifecycle callbacks have been fired yet (4 points)
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for " + callbackListenerPackage.getClass().getSimpleName(),
                                0, callbackListenerPackage.getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for " + callbackListenerPrivate.getClass().getSimpleName(),
                                0, callbackListenerPrivate.getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for " + callbackListenerProtected.getClass().getSimpleName(),
                                0, callbackListenerProtected.getFiredLifeCycleSet().size());
            Assert.assertEquals("Assert no lifecycle callbacks have been fired yet for " + callbackListenerPublic.getClass().getSimpleName(),
                                0, callbackListenerPublic.getFiredLifeCycleSet().size());

            out.println("Persisting " + entity_persist + " ...");
            em.persist(entity_persist);

            // Assert @PrePersist has NOT fired (4 points)
            Assert.assertFalse("Assert @PrePersist has NOT fired for " + callbackListenerPackage.getClass().getSimpleName(),
                               callbackListenerPackage.getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertFalse("Assert @PrePersist has NOT fired for " + callbackListenerPrivate.getClass().getSimpleName(),
                               callbackListenerPrivate.getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertFalse("Assert @PrePersist has NOT fired for " + callbackListenerProtected.getClass().getSimpleName(),
                               callbackListenerProtected.getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));
            Assert.assertFalse("Assert @PrePersist has NOT fired for " + callbackListenerPublic.getClass().getSimpleName(),
                               callbackListenerPublic.getFiredLifeCycleSet().contains(CallbackLifeCycle.PrePersist));

            out.println("Committing transaction...");
            if (tx == null) {
                em.getTransaction().commit();
            } else {
                tx.commit();
            }

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
            out.println("Callbacks Observed:");
            out.println(callbackListenerPackage.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPackage.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerPrivate.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPrivate.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerProtected.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerProtected.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerPublic.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPublic.getCallbackEventList()) {
                out.println(cr.toString());
            }

            // Test @PostLoad
            out.println("2) Test @PostLoad");
            resetEntityListeners();
            em.clear();

            out.println("Beginning new transaction...");
            if (tx == null) {
                em.getTransaction().begin();
            } else {
                tx.begin();
                em.joinTransaction();
            }

            out.println("Finding " + entityClass.getName() + " with id = " + id + " ...");
            ICallbackEntity entity_find = (ICallbackEntity) em.find(entityClass, id);
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
            out.println("Callbacks Observed:");
            out.println(callbackListenerPackage.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPackage.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerPrivate.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPrivate.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerProtected.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerProtected.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerPublic.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPublic.getCallbackEventList()) {
                out.println(cr.toString());
            }

            // Test @PreUpdate and @PostUpdate
            // Note when @PreUpdate fires is vendor specific, it could fire each time the object's persistent fields
            // are changed, or it could be fired when the transaction is in the process of committing.
            out.println("3) Test @PreUpdate and @PostUpdate");
            resetEntityListeners();

            out.println("Updating " + entityClass + "(id=" + id + ")'s name field...");
            entity_find.setName("Mutated Name");

            out.println("Committing transaction...");
            if (tx == null) {
                em.getTransaction().commit();
            } else {
                tx.commit();
            }

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
            out.println("Callbacks Observed:");
            out.println(callbackListenerPackage.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPackage.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerPrivate.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPrivate.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerProtected.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerProtected.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerPublic.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPublic.getCallbackEventList()) {
                out.println(cr.toString());
            }

            // Test @PreRemove and @PostRemove
            out.println("4) Test @PreRemove and @PostRemove");
            resetEntityListeners();
            em.clear();

            out.println("Beginning new transaction...");
            if (tx == null) {
                em.getTransaction().begin();
            } else {
                tx.begin();
                em.joinTransaction();
            }

            out.println("Finding " + entityClass.getName() + " with id = " + id + " ...");
            ICallbackEntity entity_remove = (ICallbackEntity) em.find(entityClass, id);
            Assert.assertNotNull("Assert find() did not return null.", entity_remove);

            out.println("Calling remove() on " + entityClass.getName() + " with id = " + id + " ...");
            em.remove(entity_remove);

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
            out.println("Callbacks Observed:");
            out.println(callbackListenerPackage.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPackage.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerPrivate.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPrivate.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerProtected.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerProtected.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerPublic.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPublic.getCallbackEventList()) {
                out.println(cr.toString());
            }

            out.println("Committing transaction...");
            if (tx == null) {
                em.getTransaction().commit();
            } else {
                tx.commit();
            }

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
            out.println("Callbacks Observed:");
            out.println(callbackListenerPackage.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPackage.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerPrivate.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPrivate.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerProtected.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerProtected.getCallbackEventList()) {
                out.println(cr.toString());
            }
            out.println(callbackListenerPublic.getClass().getSimpleName() + " Callbacks Observed:");
            for (CallbackRecord cr : callbackListenerPublic.getCallbackEventList()) {
                out.println(cr.toString());
            }
        } finally {
            out.println("Ending " + testName);
        }
    }

    // Support

    private void resetDefaultListeners() {
        out.println("Resetting Default Listener Callback Statistics...");

        DefaultCallbackListenerPackage.reset();
        DefaultCallbackListenerPrivate.reset();
        DefaultCallbackListenerProtected.reset();
        DefaultCallbackListenerPublic.reset();

        out.println("Default Listener Callback Statistics Reset.");
    }

    private void resetEntityListeners() {
        out.println("Resetting Entity Listener Callback Statistics...");

        AnoCallbackListenerPackage.reset();
        AnoCallbackListenerPrivate.reset();
        AnoCallbackListenerProtected.reset();
        AnoCallbackListenerPublic.reset();

        XMLCallbackListenerPackage.reset();
        XMLCallbackListenerPrivate.reset();
        XMLCallbackListenerProtected.reset();
        XMLCallbackListenerPublic.reset();

        out.println("Entity Listener Callback Statistics Reset.");
    }
}
