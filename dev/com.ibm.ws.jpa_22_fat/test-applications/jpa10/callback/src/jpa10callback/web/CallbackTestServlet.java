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
import jpa10callback.entity.entitydeclared.ano.CallbackPackageEntity;
import jpa10callback.entity.entitydeclared.ano.CallbackPrivateEntity;
import jpa10callback.entity.entitydeclared.ano.CallbackProtectedEntity;
import jpa10callback.entity.entitydeclared.ano.CallbackPublicEntity;
import jpa10callback.entity.entitydeclared.mappedsuperclass.ano.CallbackPackageMSCEntity;
import jpa10callback.entity.entitydeclared.mappedsuperclass.ano.CallbackPrivateMSCEntity;
import jpa10callback.entity.entitydeclared.mappedsuperclass.ano.CallbackProtectedMSCEntity;
import jpa10callback.entity.entitydeclared.mappedsuperclass.ano.CallbackPublicMSCEntity;
import jpa10callback.entity.entitydeclared.mappedsuperclass.xml.XMLCallbackPackageMSCEntity;
import jpa10callback.entity.entitydeclared.mappedsuperclass.xml.XMLCallbackPrivateMSCEntity;
import jpa10callback.entity.entitydeclared.mappedsuperclass.xml.XMLCallbackProtectedMSCEntity;
import jpa10callback.entity.entitydeclared.mappedsuperclass.xml.XMLCallbackPublicMSCEntity;
import jpa10callback.entity.entitydeclared.xml.XMLCallbackPackageEntity;
import jpa10callback.entity.entitydeclared.xml.XMLCallbackPrivateEntity;
import jpa10callback.entity.entitydeclared.xml.XMLCallbackProtectedEntity;
import jpa10callback.entity.entitydeclared.xml.XMLCallbackPublicEntity;
import jpa10callback.entity.listener.ano.AnoListenerEntity;
import jpa10callback.entity.listener.ano.AnoListenerExcludeMSCEntity;
import jpa10callback.entity.listener.ano.AnoListenerMSCEntity;
import jpa10callback.entity.listener.xml.XMLListenerEntity;
import jpa10callback.entity.listener.xml.XMLListenerExcludeMSCEntity;
import jpa10callback.entity.listener.xml.XMLListenerMSCEntity;
import jpa10callback.logic.CallbackTestLogic;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestCallback")
public class CallbackTestServlet extends FATServlet {
    @PersistenceContext(unitName = "Callback_JTA")
    private EntityManager em;

    @PersistenceUnit(unitName = "Callback_JTA")
    private EntityManagerFactory emfJTA;

    @PersistenceUnit(unitName = "Callback_RL")
    private EntityManagerFactory emfRL;

    @PersistenceUnit(unitName = "Cleanup")
    private EntityManagerFactory emfCleanup;

    @Resource
    private UserTransaction tx;

    private CallbackTestLogic ctl = new CallbackTestLogic();

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
     * Test callback methods on entity classes. Supports testing of entities declared by annotation and
     * XML, and supports stand-alone entity classes and entities that gain callback methods from
     * mapped superclasses.
     *
     */

    // Test with JPA entities defined by annotation

    // Package Protection
    @Test
    public void jpa10_Callback001_EntityDeclared_PackageProtection_Ano_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = CallbackPackageEntity.class;
        try {
            ctl.testCallback001(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_PackageProtection_Ano_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = CallbackPackageEntity.class;
        try {
            ctl.testCallback001(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_PackageProtection_Ano_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = CallbackPackageEntity.class;
        ctl.testCallback001(em, tx, targetEntityClass);
    }

    // Private Protection
    @Test
    public void jpa10_Callback001_EntityDeclared_PrivateProtection_Ano_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = CallbackPrivateEntity.class;
        try {
            ctl.testCallback001(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_PrivateProtection_Ano_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = CallbackPrivateEntity.class;
        try {
            ctl.testCallback001(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_PrivateProtection_Ano_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = CallbackPrivateEntity.class;
        ctl.testCallback001(em, tx, targetEntityClass);
    }

    // Protected Protection
    @Test
    public void jpa10_Callback001_EntityDeclared_ProtectedProtection_Ano_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = CallbackProtectedEntity.class;
        try {
            ctl.testCallback001(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_ProtectedProtection_Ano_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = CallbackProtectedEntity.class;
        try {
            ctl.testCallback001(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_ProtectedProtection_Ano_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = CallbackProtectedEntity.class;
        ctl.testCallback001(em, tx, targetEntityClass);
    }

    // Public Protection
    @Test
    public void jpa10_Callback001_EntityDeclared_PublicProtection_Ano_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = CallbackPublicEntity.class;
        try {
            ctl.testCallback001(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_PublicProtection_Ano_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = CallbackPublicEntity.class;
        try {
            ctl.testCallback001(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_PublicProtection_Ano_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = CallbackPublicEntity.class;
        ctl.testCallback001(em, tx, targetEntityClass);
    }

    // Test with JPA entities defined by XML
    // Package Protection
    @Test
    public void jpa10_Callback001_EntityDeclared_PackageProtection_XML_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = XMLCallbackPackageEntity.class;
        try {
            ctl.testCallback001(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_PackageProtection_XML_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = XMLCallbackPackageEntity.class;
        try {
            ctl.testCallback001(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_PackageProtection_XML_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = XMLCallbackPackageEntity.class;
        ctl.testCallback001(em, tx, targetEntityClass);
    }

    // Private Protection
    @Test
    public void jpa10_Callback001_EntityDeclared_PrivateProtection_XML_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = XMLCallbackPrivateEntity.class;
        try {
            ctl.testCallback001(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_PrivateProtection_XML_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = XMLCallbackPrivateEntity.class;
        try {
            ctl.testCallback001(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_PrivateProtection_XML_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = XMLCallbackPrivateEntity.class;
        ctl.testCallback001(em, tx, targetEntityClass);
    }

    // Protected Protection
    @Test
    public void jpa10_Callback001_EntityDeclared_ProtectedProtection_XML_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = XMLCallbackProtectedEntity.class;
        try {
            ctl.testCallback001(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_ProtectedProtection_XML_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = XMLCallbackProtectedEntity.class;
        try {
            ctl.testCallback001(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_ProtectedProtection_XML_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = XMLCallbackProtectedEntity.class;
        ctl.testCallback001(em, tx, targetEntityClass);
    }

    // Public Protection
    @Test
    public void jpa10_Callback001_EntityDeclared_PublicProtection_XML_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = XMLCallbackPublicEntity.class;
        try {
            ctl.testCallback001(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_PublicProtection_XML_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = XMLCallbackPublicEntity.class;
        try {
            ctl.testCallback001(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_PublicProtection_XML_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = XMLCallbackPublicEntity.class;
        ctl.testCallback001(em, tx, targetEntityClass);
    }

    // Test Basic Callback Function.  Verify that a callback method on an entity where callback methods are defined
    // on a MappedSuperclass are called when appropriate in an its lifecycle.

    // Package Protection
    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_PackageProtection_Ano_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = CallbackPackageMSCEntity.class;
        try {
            ctl.testCallback001(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_PackageProtection_Ano_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = CallbackPackageMSCEntity.class;
        try {
            ctl.testCallback001(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_PackageProtection_Ano_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = CallbackPackageMSCEntity.class;
        ctl.testCallback001(em, tx, targetEntityClass);
    }

    // Private Protection
    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_PrivateProtection_Ano_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = CallbackPrivateMSCEntity.class;
        try {
            ctl.testCallback001(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_PrivateProtection_Ano_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = CallbackPrivateMSCEntity.class;
        try {
            ctl.testCallback001(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_PrivateProtection_Ano_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = CallbackPrivateMSCEntity.class;
        ctl.testCallback001(em, tx, targetEntityClass);
    }

    // Protected Protection
    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_ProtectedProtection_Ano_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = CallbackProtectedMSCEntity.class;
        try {
            ctl.testCallback001(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_ProtectedProtection_Ano_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = CallbackProtectedMSCEntity.class;
        try {
            ctl.testCallback001(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_ProtectedProtection_Ano_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = CallbackProtectedMSCEntity.class;
        ctl.testCallback001(em, tx, targetEntityClass);
    }

    // Public Protection
    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_PublicProtection_Ano_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = CallbackPublicMSCEntity.class;
        try {
            ctl.testCallback001(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_PublicProtection_Ano_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = CallbackPublicMSCEntity.class;
        try {
            ctl.testCallback001(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_PublicProtection_Ano_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = CallbackPublicMSCEntity.class;
        ctl.testCallback001(em, tx, targetEntityClass);
    }

    // Test with JPA entities defined by XML
    // Package Protection
    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_PackageProtection_XML_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = XMLCallbackPackageMSCEntity.class;
        try {
            ctl.testCallback001(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_PackageProtection_XML_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = XMLCallbackPackageMSCEntity.class;
        try {
            ctl.testCallback001(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_PackageProtection_XML_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = XMLCallbackPackageMSCEntity.class;
        ctl.testCallback001(em, tx, targetEntityClass);
    }

    // Private Protection
    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_PrivateProtection_XML_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = XMLCallbackPrivateMSCEntity.class;
        try {
            ctl.testCallback001(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_PrivateProtection_XML_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = XMLCallbackPrivateMSCEntity.class;
        try {
            ctl.testCallback001(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_PrivateProtection_XML_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = XMLCallbackPrivateMSCEntity.class;
        ctl.testCallback001(em, tx, targetEntityClass);
    }

    // Protected Protection
    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_ProtectedProtection_XML_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = XMLCallbackProtectedMSCEntity.class;
        try {
            ctl.testCallback001(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_ProtectedProtection_XML_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = XMLCallbackProtectedMSCEntity.class;
        try {
            ctl.testCallback001(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_ProtectedProtection_XML_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = XMLCallbackProtectedMSCEntity.class;
        ctl.testCallback001(em, tx, targetEntityClass);
    }

    // Public Protection
    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_PublicProtection_XML_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = XMLCallbackPublicMSCEntity.class;
        try {
            ctl.testCallback001(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_PublicProtection_XML_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = XMLCallbackPublicMSCEntity.class;
        try {
            ctl.testCallback001(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback001_EntityDeclared_MSC_PublicProtection_XML_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = XMLCallbackPublicMSCEntity.class;
        ctl.testCallback001(em, tx, targetEntityClass);
    }

    /**
     * Test callback methods on entity/msc-declared listener classes.
     *
     */

    @Test
    public void jpa10_Callback004_TestCallbackListener_Ano_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = AnoListenerEntity.class;
        try {
            ctl.testCallback004(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback004_TestCallbackListener_Ano_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = AnoListenerEntity.class;
        try {
            ctl.testCallback004(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback004_TestCallbackListener_Ano_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = AnoListenerEntity.class;
        ctl.testCallback004(em, tx, targetEntityClass);
    }

    @Test
    public void jpa10_Callback004_TestCallbackListener_Ano_MSC_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = AnoListenerMSCEntity.class;
        try {
            ctl.testCallback004(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback004_TestCallbackListener_Ano_MSC_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = AnoListenerMSCEntity.class;
        try {
            ctl.testCallback004(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback004_TestCallbackListener_Ano_MSC_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = AnoListenerMSCEntity.class;
        ctl.testCallback004(em, tx, targetEntityClass);
    }

    @Test
    public void jpa10_Callback004_TestCallbackListener_XML_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = XMLListenerEntity.class;
        try {
            ctl.testCallback004(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback004_TestCallbackListener_XML_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = XMLListenerEntity.class;
        try {
            ctl.testCallback004(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback004_TestCallbackListener_XML_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = XMLListenerEntity.class;
        ctl.testCallback004(em, tx, targetEntityClass);
    }

    @Test
    public void jpa10_Callback004_TestCallbackListener_XML_MSC_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = XMLListenerMSCEntity.class;
        try {
            ctl.testCallback004(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback004_TestCallbackListener_XML_MSC_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = XMLListenerMSCEntity.class;
        try {
            ctl.testCallback004(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback004_TestCallbackListener_XML_MSC_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = XMLListenerMSCEntity.class;
        ctl.testCallback004(em, tx, targetEntityClass);
    }

    /*
     * Test Exclude Mapped-Superclass Defined Listeners Function. Verify that an entity marked with
     *
     * @ExcludeSuperclassListeners or the exclude-superclass-listeners XML element do not fire default listener
     * lifecycle methods.
     *
     * Combination Patterns:
     * Callback Method Protection Type: Package, Private, Protected, Public
     * Entity Declaration: Annotation, XML-ORM
     * Persistence Context Type: AM-JTA, AM-RL, CM-TS
     */

    @Test
    public void jpa10_Callback005_TestCallbackListener_Ano_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = AnoListenerExcludeMSCEntity.class;
        try {
            ctl.testCallback005(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback005_TestCallbackListener_Ano_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = AnoListenerExcludeMSCEntity.class;
        try {
            ctl.testCallback005(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback005_TestCallbackListener_Ano_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = AnoListenerExcludeMSCEntity.class;
        ctl.testCallback005(em, tx, targetEntityClass);
    }

    @Test
    public void jpa10_Callback005_TestCallbackListener_XML_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = XMLListenerExcludeMSCEntity.class;
        try {
            ctl.testCallback005(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback005_TestCallbackListener_XML_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = XMLListenerExcludeMSCEntity.class;
        try {
            ctl.testCallback005(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback005_TestCallbackListener_XML_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = XMLListenerExcludeMSCEntity.class;
        ctl.testCallback005(em, tx, targetEntityClass);
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
