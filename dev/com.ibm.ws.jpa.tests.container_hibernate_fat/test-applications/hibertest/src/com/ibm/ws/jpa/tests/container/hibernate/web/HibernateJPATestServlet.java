/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.tests.container.hibernate.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.persistence.SynchronizationType;
import javax.servlet.annotation.WebServlet;
import javax.transaction.RollbackException;
import javax.transaction.UserTransaction;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.junit.Test;

import com.ibm.ws.jpa.tests.container.hibernate.entity.AvgSnowfall;
import com.ibm.ws.testtooling.vehicle.web.JPADBTestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JPAContainerTestServlet")
public class HibernateJPATestServlet extends JPADBTestServlet {

    @Resource
    private UserTransaction tx;

    @PersistenceContext(unitName = "HIBER_UNIT")
    private EntityManager em;

    @PersistenceUnit(unitName = "HIBER_UNIT")
    private EntityManagerFactory emf;

    @PersistenceUnit(unitName = "HIBER_UNIT_RL")
    private EntityManagerFactory emfRL;

    @PersistenceContext(unitName = "HIBER_UNIT", synchronization = SynchronizationType.UNSYNCHRONIZED)
    private EntityManager emUnsync;

    @Test
    public void testBeanValidation() throws Exception {
        tx.begin();

        AvgSnowfall e = new AvgSnowfall();
        e.setCity("Grand Marais, Minnesota is located along the north shore of Lake Superior"); // too many characters
        e.setAmount(543); // value too large
        em.persist(e);

        try {
            tx.commit();
        } catch (RollbackException x) {
            if (x.getCause() instanceof ConstraintViolationException) {
                ConstraintViolationException cvx = (ConstraintViolationException) x.getCause();
                Set<ConstraintViolation<?>> violations = cvx.getConstraintViolations();
                assertEquals("Unexpected number of violations " + violations, 2, violations.size());

                ConstraintViolation<?> amountViolation = null;
                for (ConstraintViolation<?> v : violations)
                    if ("amount".equals(v.getPropertyPath().toString()))
                        amountViolation = v;
                assertNotNull("missing violation for amount in " + violations, amountViolation);
                assertEquals(543, amountViolation.getInvalidValue());

                ConstraintViolation<?> cityViolation = null;
                for (ConstraintViolation<?> v : violations)
                    if ("city".equals(v.getPropertyPath().toString()))
                        cityViolation = v;
                assertNotNull("missing violation for city in " + violations, cityViolation);
                assertEquals("Grand Marais, Minnesota is located along the north shore of Lake Superior", cityViolation.getInvalidValue());
            } else
                throw x;
        }

        AvgSnowfall found = em.find(AvgSnowfall.class, "Grand Marais");
        assertNull(found);

        // should work fine with valid values
        tx.begin();
        e.setCity("Grand Marais");
        e.setAmount(43);
        em.persist(e);
        tx.commit();

        found = em.find(AvgSnowfall.class, "Grand Marais");
        assertEquals(43, found.getAmount());
    }

    @Test
    public void testEntityManagerFactory() throws Exception {
        Map<String, Object> props = emf.getProperties();
        System.out.println("Hibernate entity manager factory properties are: " + props);
        assertEquals("HIBER_UNIT", props.get("hibernate.ejb.persistenceUnitName"));
        assertTrue(props.get("hibernate.connection.datasource").toString().startsWith("com.ibm.ws.rsadapter.jdbc.WSJdbcDataSource@"));

        Map<String, Object> emProps = Collections.<String, Object> singletonMap("javax.persistence.lock.timeout", 30000);
        EntityManager em1 = emf.createEntityManager(SynchronizationType.SYNCHRONIZED, emProps);
        EntityManager em2 = emf.createEntityManager(SynchronizationType.UNSYNCHRONIZED);

        assertFalse(em1.isJoinedToTransaction());
        assertFalse(em2.isJoinedToTransaction());

        tx.begin();

        assertFalse(em2.isJoinedToTransaction());
        em2.joinTransaction();
        assertTrue(em2.isJoinedToTransaction());

        AvgSnowfall e1 = new AvgSnowfall();
        e1.setCity("Caledonia");
        e1.setAmount(38);
        AvgSnowfall e2 = new AvgSnowfall();
        e2.setCity("Faribault");
        e2.setAmount(42);

        em1.persist(e1);
        em2.persist(e2);

        assertTrue(em1.isJoinedToTransaction());
        assertTrue(em2.isJoinedToTransaction());

        tx.commit();

        assertFalse(em1.isJoinedToTransaction());
        assertFalse(em2.isJoinedToTransaction());

        // Intentionally reversing what the entity managers query for - they should be able to see the data that the other committed
        AvgSnowfall found1 = em1.find(AvgSnowfall.class, "Faribault");
        AvgSnowfall found2 = em2.find(AvgSnowfall.class, "Caledonia");

        assertEquals(42, found1.getAmount());
        assertEquals(38, found2.getAmount());

        props = em1.getProperties();
        System.out.println("Hibernate entity manager[1] properties are: " + props);
        assertEquals(30000, props.get("javax.persistence.lock.timeout"));

        props = em2.getProperties();
        System.out.println("Hibernate entity manager[2] properties are: " + props);
        assertEquals(-1, props.get("javax.persistence.lock.timeout"));

        em1.close();
        em2.close();
    }

    /**
     * Verify we can perform JSE-like usage of Hibernate by creating an emf and persisting an entity.
     */
    @Test
    public void testJSEDataSourcePersistenceUnit() throws Exception {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("HIBER_UNIT_RL");
        EntityManager em = emf.createEntityManager();

        em.getTransaction().begin();
        AvgSnowfall e = new AvgSnowfall();
        e.setCity("Rochester");
        e.setAmount(48);
        em.persist(e);
        em.getTransaction().commit();

        AvgSnowfall found = em.find(AvgSnowfall.class, "Rochester");
        assertEquals(48, found.getAmount());

        em.close();
    }

    @Test
    public void testPersistenceContext() throws Exception {
        tx.begin();

        assertTrue(em.isJoinedToTransaction());

        AvgSnowfall e = new AvgSnowfall();
        e.setCity("Duluth");
        e.setAmount(85);
        em.persist(e);

        AvgSnowfall found = em.find(AvgSnowfall.class, "Duluth");
        assertEquals(85, found.getAmount());

        tx.commit();
    }

    @Test
    public void testResourceLocal() throws Exception {
        EntityManager emRL = emfRL.createEntityManager();
        EntityTransaction etx = emRL.getTransaction();

        etx.begin();
        AvgSnowfall e = new AvgSnowfall();
        e.setCity("Eveleth");
        e.setAmount(60);
        emRL.persist(e);
        e = new AvgSnowfall();
        e.setCity("Hibbing");
        e.setAmount(60);
        emRL.persist(e);
        etx.rollback();

        assertNull(emRL.find(AvgSnowfall.class, "Eveleth"));
        assertNull(emRL.find(AvgSnowfall.class, "Hibbing"));

        etx.begin();
        e = new AvgSnowfall();
        e.setCity("Wabasha");
        e.setAmount(38);
        emRL.persist(e);
        e = new AvgSnowfall();
        e.setCity("Red Wing");
        e.setAmount(33);
        emRL.persist(e);
        etx.commit();

        AvgSnowfall found;
        found = emRL.find(AvgSnowfall.class, "Wabasha");
        assertEquals(38, found.getAmount());
        found = emRL.find(AvgSnowfall.class, "Red Wing");
        assertEquals(33, found.getAmount());

        emRL.close();
    }

    @Test
    public void testRollback() throws Exception {
        tx.begin();

        AvgSnowfall e = new AvgSnowfall();
        e.setCity("Bemidji");
        e.setAmount(41);
        em.persist(e);

        tx.rollback();

        AvgSnowfall found = em.find(AvgSnowfall.class, "Bemidji");
        assertNull(found);
    }

    @Test
    public void testUnsynchronized() throws Exception {
        tx.begin();

        assertFalse(emUnsync.isJoinedToTransaction());

        emUnsync.joinTransaction();

        assertTrue(emUnsync.isJoinedToTransaction());

        AvgSnowfall e = new AvgSnowfall();
        e.setCity("Owatonna");
        e.setAmount(42);
        emUnsync.persist(e);

        tx.commit();

        emUnsync.clear();
        AvgSnowfall found = emUnsync.find(AvgSnowfall.class, "Owatonna");
        assertEquals(42, found.getAmount());
    }

    @Test
    public void testUpdateConfig_JPA21Feature() throws Exception {
        tx.begin();

        assertTrue(em.isJoinedToTransaction());

        AvgSnowfall e = new AvgSnowfall();
        e.setCity("International Falls");
        e.setAmount(71);
        em.persist(e);

        AvgSnowfall found = em.find(AvgSnowfall.class, "International Falls");
        assertEquals(71, found.getAmount());

        tx.commit();
    }

    @Test
    public void testUpdateConfig_ProviderInGlobalLibrary() throws Exception {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("HIBER_UNIT_RL");
        EntityManager em = emf.createEntityManager();

        em.getTransaction().begin();
        AvgSnowfall e = new AvgSnowfall();
        e.setCity("Mankato");
        e.setAmount(39);
        em.persist(e);
        em.getTransaction().commit();

        AvgSnowfall found = em.find(AvgSnowfall.class, "Mankato");
        assertEquals(39, found.getAmount());

        em.close();
    }
}
