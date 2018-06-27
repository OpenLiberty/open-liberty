/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package fat.tx.jta.hibernate.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.annotation.WebServlet;
import javax.sql.DataSource;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.UserTransaction;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/HibernateTxTestServlet")
public class HibernateTxTestServlet extends FATServlet {

    @Resource(lookup = "jdbc/derby")
    DataSource ds;

    @Resource
    UserTransaction tx;

    @Test
    public void testLibertyJtaPlatform() throws Exception {
        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                        .applySetting(AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta")
                        //                        .applySetting(AvailableSettings.DATASOURCE, ds)
                        .applySetting(AvailableSettings.DIALECT, "org.hibernate.dialect.DerbyTenSevenDialect")
                        .applySetting(AvailableSettings.JPA_JTA_DATASOURCE, "java:comp/env/jdbc/derby")
                        //                        .applySetting("javax.persistence.schema-generation.database.action", "drop-and-create")
                        .build();

        assertEquals("org.hibernate.engine.transaction.jta.platform.internal.WebSphereLibertyJtaPlatform",
                     serviceRegistry.getService(JtaPlatform.class).getClass().getCanonicalName());
    }

    @Test
    public void testHibernate() throws Exception {
        StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                        .applySetting(AvailableSettings.TRANSACTION_COORDINATOR_STRATEGY, "jta")
                        .applySetting(AvailableSettings.DATASOURCE, ds)
                        .applySetting(AvailableSettings.DIALECT, "org.hibernate.dialect.DerbyTenSevenDialect")
                        .applySetting(AvailableSettings.JPA_JTA_DATASOURCE, "java:comp/env/jdbc/derby")
                        .applySetting("javax.persistence.schema-generation.database.action", "drop-and-create")
                        .build();

        System.out.println("Using JtaPlatform: " + serviceRegistry.getService(JtaPlatform.class));

        Metadata metadata = new MetadataSources(serviceRegistry)
                        .addAnnotatedClass(Widget.class)
                        .getMetadataBuilder()
                        .build();

        SessionFactory sessionFactory = metadata.getSessionFactoryBuilder().build();

        Session session = sessionFactory.openSession();
        System.out.println("Opened session");
        try {
            Transaction tran = session.getTransaction();
            session.getTransaction().begin();
            System.out.println("Began tran: " + tran);
            assertTrue(session.isJoinedToTransaction());

            LoggingSync sync = new LoggingSync();
            session.getTransaction().registerSynchronization(sync);

            Widget w = new Widget();
            w.id = 1;
            session.save(w);
            System.out.println("Saved widget with id=" + w.id);

            w = new Widget();
            w.id = 2;
            session.save(w);
            System.out.println("Saved widget with id=" + w.id);
            session.getTransaction().commit();
            sync.verifyCalled(true, true, Status.STATUS_COMMITTED);

            session.getTransaction().begin();
            LoggingSync sync2 = new LoggingSync();
            session.getTransaction().registerSynchronization(sync2);
            w = new Widget();
            w.id = 3;
            session.save(w);
            System.out.println("rolling back widget with id=" + w.id);
            session.getTransaction().rollback();
            sync2.verifyCalled(false, true, Status.STATUS_UNKNOWN);
            sync.verifyCalled(true, true, Status.STATUS_COMMITTED);

            session.getTransaction().begin();
            Widget w3 = session.find(Widget.class, 3);
            assertNull("Was expecting Widget with id=3 to be rolled back, but found it in the database!", w3);
            session.getTransaction().commit();
        } finally {
            session.close();
            sessionFactory.close();
        }
    }

    @Test
    public void testJPA() throws Exception {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("test_pu");
        EntityManager em = emf.createEntityManager();

        tx.begin();
        em.joinTransaction();
        assertTrue(em.isJoinedToTransaction());
        Widget w = new Widget();
        w.id = 4;
        em.persist(w);
        tx.commit();

        tx.begin();
        em.joinTransaction();
        Widget w4 = em.find(Widget.class, 4);
        assertNotNull(w4);
        assertEquals(4, w4.id);

        Widget w5 = new Widget();
        w5.id = 5;
        em.persist(w5);
        tx.rollback();

        tx.begin();
        em.joinTransaction();
        Widget rolledBack = em.find(Widget.class, 5);
        assertNull(rolledBack);
        tx.commit();
    }

    private class LoggingSync implements Synchronization {

        private volatile int beforeCount = 0;
        private volatile int afterCount = 0;
        private volatile int status = -1;

        @Override
        public void beforeCompletion() {
            beforeCount++;
            System.out.println("Before completion #" + beforeCount);

        }

        @Override
        public void afterCompletion(int status) {
            afterCount++;
            this.status = status;
            System.out.println("After completion #" + afterCount + " with status: " + status);
        }

        public void verifyCalled(boolean before, boolean after, int status) {
            if (before)
                assertEquals(1, beforeCount);
            if (after)
                assertEquals(1, afterCount);
            if (status != -1)
                assertEquals(status, this.status);
        }
    }
}
