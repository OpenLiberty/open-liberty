package jpa22injection.web;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import jpa22injection.entity.InjectEntityA;
import jpa22injection.entity.InjectEntityB;
import junit.framework.Assert;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestJPA22Injection")
@PersistenceContext(name = "em1", unitName = "JPAPU-1")
@PersistenceContext(name = "em2", unitName = "JPAPU-2")
@PersistenceUnit(name = "em1RL", unitName = "JPAPU-1RL")
@PersistenceUnit(name = "em2RL", unitName = "JPAPU-2RL")
public class JPAInjectionTestServlet extends FATServlet {
    @Resource
    private UserTransaction tx;

    /**
     * Verify that the @PersistenceContext annotation is repeatable, and that consumable
     * EntityManagers are provided in the scenario that this capability is utilized in.
     * 
     */
    @Test
    @Mode(TestMode.LITE)
    public void testInjectPersistenceContext() throws Exception {
        final String testName = "testInjectPersistenceContext";
        System.out.println("STARTING " + testName + " ...");

        try {
            InitialContext ic = new InitialContext();
            Assert.assertNotNull(ic);

            EntityManager em1 = (EntityManager) ic.lookup("java:comp/env/em1");
            Assert.assertNotNull(em1);

            EntityManager em2 = (EntityManager) ic.lookup("java:comp/env/em2");
            Assert.assertNotNull(em2);

            // Persist entities which are appropriate to their respective persistence units
            tx.begin();
            InjectEntityA entA1 = new InjectEntityA();
            entA1.setStrData("A1");
            em1.persist(entA1);

            InjectEntityB entB1 = new InjectEntityB();
            entB1.setStrData("B1");
            em2.persist(entB1);
            tx.commit();

            em1.clear();
            em2.clear();

            // Verify persistence
            InjectEntityA findA1 = em1.find(InjectEntityA.class, entA1.getId());
            Assert.assertNotNull(findA1);

            InjectEntityB findB1 = em2.find(InjectEntityB.class, entB1.getId());
            Assert.assertNotNull(findB1);

            // Confirm that correct persistence units are associated with the persistence contexts
            // by attempting to use an entity that is not a member of the associated persistence unit
            try {
                InjectEntityA findA1_wrong = em2.find(InjectEntityA.class, entA1.getId());
                Assert.fail("No exception was thrown.");
            } catch (IllegalArgumentException iae) {
                // Expected
            }

            try {
                InjectEntityB findB1_wrong = em1.find(InjectEntityB.class, entB1.getId());
                Assert.fail("No exception was thrown.");
            } catch (IllegalArgumentException iae) {
                // Expected
            }
        } finally {
            System.out.println("ENDING " + testName);
        }
    }

    /**
     * Verify that the @PersistenceUnit annotation is repeatable, and that consumable
     * EntityManagerFactory's are provided in the scenario that this capability is utilized in.
     * 
     */
    @Test
    @Mode(TestMode.LITE)
    public void testInjectPersistenceUnit() throws Exception {
        final String testName = "testInjectPersistenceUnit";
        System.out.println("STARTING " + testName + " ...");

        try {
            InitialContext ic = new InitialContext();
            Assert.assertNotNull(ic);

            EntityManagerFactory emf1 = (EntityManagerFactory) ic.lookup("java:comp/env/em1RL");
            Assert.assertNotNull(emf1);
            EntityManager em1 = emf1.createEntityManager();

            EntityManagerFactory emf2 = (EntityManagerFactory) ic.lookup("java:comp/env/em2RL");
            Assert.assertNotNull(emf2);
            EntityManager em2 = emf2.createEntityManager();

            // Persist entities which are appropriate to their respective persistence units
            em1.getTransaction().begin();
            InjectEntityA entA1 = new InjectEntityA();
            entA1.setStrData("A1");
            em1.persist(entA1);
            em1.getTransaction().commit();

            em2.getTransaction().begin();
            InjectEntityB entB1 = new InjectEntityB();
            entB1.setStrData("B1");
            em2.persist(entB1);
            em2.getTransaction().commit();

            em1.clear();
            em2.clear();

            // Verify persistence
            InjectEntityA findA1 = em1.find(InjectEntityA.class, entA1.getId());
            Assert.assertNotNull(findA1);

            InjectEntityB findB1 = em2.find(InjectEntityB.class, entB1.getId());
            Assert.assertNotNull(findB1);

            // Confirm that correct persistence units are associated with the persistence contexts
            // by attempting to use an entity that is not a member of the associated persistence unit
            try {
                InjectEntityA findA1_wrong = em2.find(InjectEntityA.class, entA1.getId());
                Assert.fail("No exception was thrown.");
            } catch (IllegalArgumentException iae) {
                // Expected
            }

            try {
                InjectEntityB findB1_wrong = em1.find(InjectEntityB.class, entB1.getId());
                Assert.fail("No exception was thrown.");
            } catch (IllegalArgumentException iae) {
                // Expected
            }

            em1.close();
            em2.close();
        } finally {
            System.out.println("ENDING " + testName);
        }
    }
}
