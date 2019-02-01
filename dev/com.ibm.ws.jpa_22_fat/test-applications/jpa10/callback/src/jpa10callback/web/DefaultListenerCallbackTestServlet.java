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
import jpa10callback.entity.defaultlistener.EntityNotSupportingDefaultCallbacks;
import jpa10callback.entity.defaultlistener.EntitySupportingDefaultCallbacks;
import jpa10callback.entity.defaultlistener.XMLEntityNotSupportingDefaultCallbacks;
import jpa10callback.entity.defaultlistener.XMLEntitySupportingDefaultCallbacks;
import jpa10callback.entity.listener.ano.AnoListenerEntity;
import jpa10callback.logic.CallbackTestLogic;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/DefaultTestCallback")
public class DefaultListenerCallbackTestServlet extends FATServlet {
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
     * Test callback methods on default listener classes.
     *
     */

    // Test with JPA entities defined by annotation

    @Test
    public void jpa10_Callback002_EntitySupportingDefaultCallbacks_Ano_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = EntitySupportingDefaultCallbacks.class;
        try {
            ctl.testCallback002(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback002_EntitySupportingDefaultCallbacks_Ano_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = EntitySupportingDefaultCallbacks.class;
        try {
            ctl.testCallback002(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback002_EntitySupportingDefaultCallbacks_Ano_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = EntitySupportingDefaultCallbacks.class;
        ctl.testCallback002(em, tx, targetEntityClass);
    }

    // Test with JPA entities defined by XML

    @Test
    public void jpa10_Callback002_EntitySupportingDefaultCallbacks_XML_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = XMLEntitySupportingDefaultCallbacks.class;
        try {
            ctl.testCallback002(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback002_EntitySupportingDefaultCallbacks_XML_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = XMLEntitySupportingDefaultCallbacks.class;
        try {
            ctl.testCallback002(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback002_EntitySupportingDefaultCallbacks_XML_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = XMLEntitySupportingDefaultCallbacks.class;
        ctl.testCallback002(em, tx, targetEntityClass);
    }

    /**
     * Verify that default listener classes will not fire for entities that request exclusion.
     *
     */

    // Test with JPA entities defined by annotation

    @Test
    public void jpa10_Callback003_EntitySupportingDefaultCallbacks_Ano_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = EntityNotSupportingDefaultCallbacks.class;
        try {
            ctl.testCallback003(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback003_EntitySupportingDefaultCallbacks_Ano_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = EntityNotSupportingDefaultCallbacks.class;
        try {
            ctl.testCallback003(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback003_EntitySupportingDefaultCallbacks_Ano_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = EntityNotSupportingDefaultCallbacks.class;
        ctl.testCallback003(em, tx, targetEntityClass);
    }

    // Test with JPA entities defined by XML

    @Test
    public void jpa10_Callback003_EntitySupportingDefaultCallbacks_XML_AMJTA_Web() throws Exception {
        final EntityManager em = emfJTA.createEntityManager();
        final Class<?> targetEntityClass = XMLEntityNotSupportingDefaultCallbacks.class;
        try {
            ctl.testCallback003(em, tx, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback003_EntitySupportingDefaultCallbacks_XML_AMRL_Web() throws Exception {
        final EntityManager em = emfRL.createEntityManager();
        final Class<?> targetEntityClass = XMLEntityNotSupportingDefaultCallbacks.class;
        try {
            ctl.testCallback003(em, null, targetEntityClass);
        } finally {
            closeEM(em);
        }
    }

    @Test
    public void jpa10_Callback003_EntitySupportingDefaultCallbacks_XML_CMTS_Web() throws Exception {
        final Class<?> targetEntityClass = XMLEntityNotSupportingDefaultCallbacks.class;
        ctl.testCallback003(em, tx, targetEntityClass);
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
