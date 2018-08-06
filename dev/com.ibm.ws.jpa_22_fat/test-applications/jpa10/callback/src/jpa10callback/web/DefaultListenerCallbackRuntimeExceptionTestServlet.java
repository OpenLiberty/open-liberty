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

package jpa10callback.web;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;
import jpa10callback.AbstractCallbackListener.ProtectionType;
import jpa10callback.entity.defaultlistener.EntitySupportingDefaultCallbacks;
import jpa10callback.entity.defaultlistener.XMLEntitySupportingDefaultCallbacks;
import jpa10callback.entity.listener.ano.AnoListenerEntity;
import jpa10callback.logic.CallbackRuntimeExceptionTestLogic;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/DefaultCallbackRuntimeTestCallback")
public class DefaultListenerCallbackRuntimeExceptionTestServlet extends FATServlet {
    @PersistenceContext(unitName = "Callback-DefaultListener_JTA")
    private EntityManager em;

    @PersistenceUnit(unitName = "Callback-DefaultListener_JTA")
    private EntityManagerFactory emfJTA;

    @PersistenceUnit(unitName = "Callback-DefaultListener_RL")
    private EntityManagerFactory emfRL;

    @PersistenceUnit(unitName = "Cleanup")
    private EntityManagerFactory emfCleanup;

    @Resource
    private UserTransaction tx;

    private CallbackRuntimeExceptionTestLogic ctl = new CallbackRuntimeExceptionTestLogic();

    @PostConstruct
    private void initFAT() {
        EntityManager em = emfCleanup.createEntityManager();

        try {
            // Poke the persistence unit to force table creation
            em.find(AnoListenerEntity.class, 1);
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            if (em != null) {
                try {
                    em.close();
                } catch (Throwable t) {
                }
            }
        }
    }

    /**
     * Test Callback Function when a RuntimeException is thrown by the callback method.
     * Verify when appropriate that the transaction is still active and is marked for rollback.
     *
     * Combination Patterns:
     * Callback Method Protection Type: Package, Private, Protected, Public
     * Entity Declaration: Annotation, XML-ORM
     * Persistence Context Type: AM-JTA, AM-RL, CM-TS
     *
     */

    // Annotated Entities

    // Package Protection
    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_PackageProtection_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = EntitySupportingDefaultCallbacks.class;
        try {
            ctl.testCallbackRuntimeException002(em, tx, targetEntityClass, ProtectionType.PT_PACKAGE);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_PackageProtection_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = EntitySupportingDefaultCallbacks.class;
        try {
            ctl.testCallbackRuntimeException002(em, null, targetEntityClass, ProtectionType.PT_PACKAGE);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_PackageProtection_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = EntitySupportingDefaultCallbacks.class;
        ctl.testCallbackRuntimeException002(em, tx, targetEntityClass, ProtectionType.PT_PACKAGE);
    }

    // Private Protection
    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_PrivateProtection_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = EntitySupportingDefaultCallbacks.class;
        try {
            ctl.testCallbackRuntimeException002(em, tx, targetEntityClass, ProtectionType.PT_PRIVATE);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_PrivateProtection_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = EntitySupportingDefaultCallbacks.class;
        try {
            ctl.testCallbackRuntimeException002(em, null, targetEntityClass, ProtectionType.PT_PRIVATE);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_PrivateProtection_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = EntitySupportingDefaultCallbacks.class;
        ctl.testCallbackRuntimeException002(em, tx, targetEntityClass, ProtectionType.PT_PRIVATE);
    }

    // Protected Protection
    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_ProtectedProtection_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = EntitySupportingDefaultCallbacks.class;
        try {
            ctl.testCallbackRuntimeException002(em, tx, targetEntityClass, ProtectionType.PT_PROTECTED);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_ProtectedProtection_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = EntitySupportingDefaultCallbacks.class;
        try {
            ctl.testCallbackRuntimeException002(em, null, targetEntityClass, ProtectionType.PT_PROTECTED);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_ProtectedProtection_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = EntitySupportingDefaultCallbacks.class;
        ctl.testCallbackRuntimeException002(em, tx, targetEntityClass, ProtectionType.PT_PROTECTED);
    }

    // Public Protection
    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_PublicProtection_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = EntitySupportingDefaultCallbacks.class;
        try {
            ctl.testCallbackRuntimeException002(em, tx, targetEntityClass, ProtectionType.PT_PUBLIC);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_PublicProtection_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = EntitySupportingDefaultCallbacks.class;
        try {
            ctl.testCallbackRuntimeException002(em, null, targetEntityClass, ProtectionType.PT_PUBLIC);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_PublicProtection_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = EntitySupportingDefaultCallbacks.class;
        ctl.testCallbackRuntimeException002(em, tx, targetEntityClass, ProtectionType.PT_PUBLIC);
    }

    // XML Entities

    // Package Protection
    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_PackageProtection_XML_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = XMLEntitySupportingDefaultCallbacks.class;
        try {
            ctl.testCallbackRuntimeException002(em, tx, targetEntityClass, ProtectionType.PT_PACKAGE);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_PackageProtection_XML_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = XMLEntitySupportingDefaultCallbacks.class;
        try {
            ctl.testCallbackRuntimeException002(em, null, targetEntityClass, ProtectionType.PT_PACKAGE);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_PackageProtection_XML_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = XMLEntitySupportingDefaultCallbacks.class;
        ctl.testCallbackRuntimeException002(em, tx, targetEntityClass, ProtectionType.PT_PACKAGE);
    }

    // Private Protection
    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_PrivateProtection_XML_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = XMLEntitySupportingDefaultCallbacks.class;
        try {
            ctl.testCallbackRuntimeException002(em, tx, targetEntityClass, ProtectionType.PT_PRIVATE);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_PrivateProtection_XML_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = XMLEntitySupportingDefaultCallbacks.class;
        try {
            ctl.testCallbackRuntimeException002(em, null, targetEntityClass, ProtectionType.PT_PRIVATE);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_PrivateProtection_XML_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = XMLEntitySupportingDefaultCallbacks.class;
        ctl.testCallbackRuntimeException002(em, tx, targetEntityClass, ProtectionType.PT_PRIVATE);
    }

    // Protected Protection
    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_ProtectedProtection_XML_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = XMLEntitySupportingDefaultCallbacks.class;
        try {
            ctl.testCallbackRuntimeException002(em, tx, targetEntityClass, ProtectionType.PT_PROTECTED);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_ProtectedProtection_XML_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = XMLEntitySupportingDefaultCallbacks.class;
        try {
            ctl.testCallbackRuntimeException002(em, null, targetEntityClass, ProtectionType.PT_PROTECTED);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_ProtectedProtection_XML_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = XMLEntitySupportingDefaultCallbacks.class;
        ctl.testCallbackRuntimeException002(em, tx, targetEntityClass, ProtectionType.PT_PROTECTED);
    }

    // Public Protection
    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_PublicProtection_XML_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = XMLEntitySupportingDefaultCallbacks.class;
        try {
            ctl.testCallbackRuntimeException002(em, tx, targetEntityClass, ProtectionType.PT_PUBLIC);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_PublicProtection_XML_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = XMLEntitySupportingDefaultCallbacks.class;
        try {
            ctl.testCallbackRuntimeException002(em, null, targetEntityClass, ProtectionType.PT_PUBLIC);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_RuntimeException_002_EntitySupportingDefaultCallbacks_PublicProtection_XML_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = XMLEntitySupportingDefaultCallbacks.class;
        ctl.testCallbackRuntimeException002(em, tx, targetEntityClass, ProtectionType.PT_PUBLIC);
    }

    // Support
    private void closeEM(EntityManager em) {
        if (em != null) {
            try {
                em.close();
            } catch (Throwable t) {
            }
        }

    }
}
