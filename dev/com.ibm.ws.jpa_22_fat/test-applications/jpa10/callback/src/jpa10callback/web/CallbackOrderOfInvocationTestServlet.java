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
import jpa10callback.entity.listener.ano.AnoListenerEntity;
import jpa10callback.entity.orderofinvocation.ano.AnoOOILeafPackageEntity;
import jpa10callback.entity.orderofinvocation.ano.AnoOOILeafPrivateEntity;
import jpa10callback.entity.orderofinvocation.ano.AnoOOILeafProtectedEntity;
import jpa10callback.entity.orderofinvocation.ano.AnoOOILeafPublicEntity;
import jpa10callback.entity.orderofinvocation.xml.XMLOOILeafPackageEntity;
import jpa10callback.entity.orderofinvocation.xml.XMLOOILeafPrivateEntity;
import jpa10callback.entity.orderofinvocation.xml.XMLOOILeafProtectedEntity;
import jpa10callback.entity.orderofinvocation.xml.XMLOOILeafPublicEntity;
import jpa10callback.logic.CallbackOrderOfInvocationTestLogic;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestCallbackOrderOfInvocation")
public class CallbackOrderOfInvocationTestServlet extends FATServlet {
    @PersistenceContext(unitName = "Callback-OrderOfInvocation_JTA")
    private EntityManager em;

    @PersistenceUnit(unitName = "Callback-OrderOfInvocation_JTA")
    private EntityManagerFactory emfJTA;

    @PersistenceUnit(unitName = "Callback-OrderOfInvocation_RL")
    private EntityManagerFactory emfRL;

    @PersistenceUnit(unitName = "Cleanup")
    private EntityManagerFactory emfCleanup;

    @Resource
    private UserTransaction tx;

    private CallbackOrderOfInvocationTestLogic ctl = new CallbackOrderOfInvocationTestLogic();

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
     * Test Order of Invocation
     *
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
     * Combination Patterns:
     * Callback Method Protection Type: Package, Private, Protected, Public
     * Entity Declaration: Annotation, XML-ORM
     * Persistence Context Type: AM-JTA, AM-RL, CM-TS
     *
     */

    // Test with JPA entities defined by annotation

    // Package Protection
    @Test
    public void jpa10_Callback_OrderOfInvocation_001_PackageProtection_Ano_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = AnoOOILeafPackageEntity.class;
        try {
            ctl.testOrderOfInvocation001(em, tx, targetEntityClass, ProtectionType.PT_PACKAGE);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_001_PackageProtection_Ano_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = AnoOOILeafPackageEntity.class;
        try {
            ctl.testOrderOfInvocation001(em, null, targetEntityClass, ProtectionType.PT_PACKAGE);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_001_PackageProtection_Ano_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = AnoOOILeafPackageEntity.class;
        ctl.testOrderOfInvocation001(em, tx, targetEntityClass, ProtectionType.PT_PACKAGE);
    }

    // Private Protection
    @Test
    public void jpa10_Callback_OrderOfInvocation_001_PrivateProtection_Ano_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = AnoOOILeafPrivateEntity.class;
        try {
            ctl.testOrderOfInvocation001(em, tx, targetEntityClass, ProtectionType.PT_PRIVATE);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_001_PrivateProtection_Ano_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = AnoOOILeafPrivateEntity.class;
        try {
            ctl.testOrderOfInvocation001(em, null, targetEntityClass, ProtectionType.PT_PRIVATE);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_001_PrivateProtection_Ano_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = AnoOOILeafPrivateEntity.class;
        ctl.testOrderOfInvocation001(em, tx, targetEntityClass, ProtectionType.PT_PRIVATE);
    }

    // Protected Protection
    @Test
    public void jpa10_Callback_OrderOfInvocation_001_ProtectedProtection_Ano_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = AnoOOILeafProtectedEntity.class;
        try {
            ctl.testOrderOfInvocation001(em, tx, targetEntityClass, ProtectionType.PT_PROTECTED);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_001_ProtectedProtection_Ano_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = AnoOOILeafProtectedEntity.class;
        try {
            ctl.testOrderOfInvocation001(em, null, targetEntityClass, ProtectionType.PT_PROTECTED);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_001_ProtectedProtection_Ano_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = AnoOOILeafProtectedEntity.class;
        ctl.testOrderOfInvocation001(em, tx, targetEntityClass, ProtectionType.PT_PROTECTED);
    }

    // Public Protection
    @Test
    public void jpa10_Callback_OrderOfInvocation_001_PublicProtection_Ano_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = AnoOOILeafPublicEntity.class;
        try {
            ctl.testOrderOfInvocation001(em, tx, targetEntityClass, ProtectionType.PT_PUBLIC);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_001_PublicProtection_Ano_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = AnoOOILeafPublicEntity.class;
        try {
            ctl.testOrderOfInvocation001(em, null, targetEntityClass, ProtectionType.PT_PUBLIC);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_001_PublicProtection_Ano_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = AnoOOILeafPublicEntity.class;
        ctl.testOrderOfInvocation001(em, tx, targetEntityClass, ProtectionType.PT_PUBLIC);
    }

    // Test with JPA entities defined by XML

    // Package Protection
    @Test
    public void jpa10_Callback_OrderOfInvocation_001_PackageProtection_XML_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = XMLOOILeafPackageEntity.class;
        try {
            ctl.testOrderOfInvocation001(em, tx, targetEntityClass, ProtectionType.PT_PACKAGE);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_001_PackageProtection_XML_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = XMLOOILeafPackageEntity.class;
        try {
            ctl.testOrderOfInvocation001(em, null, targetEntityClass, ProtectionType.PT_PACKAGE);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_001_PackageProtection_XML_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = XMLOOILeafPackageEntity.class;
        ctl.testOrderOfInvocation001(em, tx, targetEntityClass, ProtectionType.PT_PACKAGE);
    }

    // Private Protection
    @Test
    public void jpa10_Callback_OrderOfInvocation_001_PrivateProtection_XML_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = XMLOOILeafPrivateEntity.class;
        try {
            ctl.testOrderOfInvocation001(em, tx, targetEntityClass, ProtectionType.PT_PRIVATE);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_001_PrivateProtection_XML_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = XMLOOILeafPrivateEntity.class;
        try {
            ctl.testOrderOfInvocation001(em, null, targetEntityClass, ProtectionType.PT_PRIVATE);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_001_PrivateProtection_XML_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = XMLOOILeafPrivateEntity.class;
        ctl.testOrderOfInvocation001(em, tx, targetEntityClass, ProtectionType.PT_PRIVATE);
    }

    // Protected Protection
    @Test
    public void jpa10_Callback_OrderOfInvocation_001_ProtectedProtection_XML_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = XMLOOILeafProtectedEntity.class;
        try {
            ctl.testOrderOfInvocation001(em, tx, targetEntityClass, ProtectionType.PT_PROTECTED);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_001_ProtectedProtection_XML_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = XMLOOILeafProtectedEntity.class;
        try {
            ctl.testOrderOfInvocation001(em, null, targetEntityClass, ProtectionType.PT_PROTECTED);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_001_ProtectedProtection_XML_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = XMLOOILeafProtectedEntity.class;
        ctl.testOrderOfInvocation001(em, tx, targetEntityClass, ProtectionType.PT_PROTECTED);
    }

    // Public Protection
    @Test
    public void jpa10_Callback_OrderOfInvocation_001_PublicProtection_XML_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = XMLOOILeafPublicEntity.class;
        try {
            ctl.testOrderOfInvocation001(em, tx, targetEntityClass, ProtectionType.PT_PUBLIC);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_001_PublicProtection_XML_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = XMLOOILeafPublicEntity.class;
        try {
            ctl.testOrderOfInvocation001(em, null, targetEntityClass, ProtectionType.PT_PUBLIC);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_001_PublicProtection_XML_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = XMLOOILeafPublicEntity.class;
        ctl.testOrderOfInvocation001(em, tx, targetEntityClass, ProtectionType.PT_PUBLIC);
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
