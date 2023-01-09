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

package com.ibm.ws.jpa.fat.defaultds.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.persistence.SynchronizationType;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.ws.jpa.fat.defaultds.entity.DSEntity;

import componenttest.app.FATServlet;

/**
 *
 */
public class Spec21DDSServlet extends FATServlet {
    private static final String CLASS_NAME = Spec21DDSServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    @PersistenceUnit(unitName = "DefDS")
    private EntityManagerFactory emfDDS;

    @PersistenceContext(unitName = "DefDS")
    private EntityManager emDDS;

    @Resource
    private UserTransaction tx;

    @PostConstruct
    public void postConstruct() {
        System.out.println("Spec21DDSServlet postConstruct");
        System.out.println("Spec21DDSServlet.emfDDS = " + emfDDS);
        System.out.println("Spec21DDSServlet.emDDS = " + emDDS);
    }

//    @Override
//    protected void doGet(HttpServletRequest request,
//                         HttpServletResponse response) throws ServletException, IOException {
//        PrintWriter writer = response.getWriter();
//        String method = request.getParameter("testMethod");
//        if (method != null && method.length() > 0) {
//            try {
//                Method mthd = this.getClass().getMethod(method, new Class<?>[] { PrintWriter.class });
//                if (mthd != null) {
//                    mthd.invoke(this, new Object[] { writer });
//                } else {
//                    writer.println("ERROR: Test method " + method + " not found on servlet " + this.getServletName());
//                }
//            } catch (Exception e) {
//                writer.println("ERROR: Caught exception attempting to call test method " + method + " on servlet " + this.getServletName());
//                e.printStackTrace(writer);
//            }
//        }
//        writer.flush();
//        writer.close();
//    }

    @Test
    public void testDefaultDataSource001() {
        EntityManager em = null;
        try {
            System.out.println("Starting testDefaultDataSource001...");
            System.out.println("Starting testDefaultDataSource001...");
            System.out.println("Acquiring EntityManager...");
            em = emfDDS.createEntityManager();

            System.out.println("Beginning Transaction...");
            tx.begin();
            em.joinTransaction();

            System.out.println("Creating DSEntity...");
            DSEntity newEntity = new DSEntity();
            newEntity.setStrData("Some Data");

            System.out.println("Persisting DSEntity...");
            em.persist(newEntity);
            em.flush();

            System.out.println("New Entity: " + newEntity);

            tx.commit();

            System.out.println("SUCCESS:testDefaultDataSource001");
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("ERROR: Caught unexpected Exception: " + t);
        } finally {
            try {
                tx.rollback();
            } catch (Throwable t) {
                // Swallow
            }

            if (em != null) {
                try {
                    em.close();
                } catch (Throwable t) {
                    // Swallow
                }
            }

            System.out.println("Ending testDefaultDataSource001...");
            System.out.println("Ending testDefaultDataSource001...");
        }
    }

    @Test
    public void testDefaultDataSource002() {
        EntityManager em = null;
        try {
            System.out.println("Starting testDefaultDataSource002...");
            System.out.println("Starting testDefaultDataSource002...");
            System.out.println("Acquiring EntityManager...");
            em = emDDS;

            System.out.println("Beginning Transaction...");
            tx.begin();
            em.joinTransaction();

            System.out.println("Creating DSEntity...");
            DSEntity newEntity = new DSEntity();
            newEntity.setStrData("Some Data");

            System.out.println("Persisting DSEntity...");
            em.persist(newEntity);
            em.flush();

            System.out.println("New Entity: " + newEntity);

            tx.commit();

            System.out.println("SUCCESS:testDefaultDataSource002");
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("ERROR: Caught unexpected Exception: " + t);
        } finally {
            try {
                tx.rollback();
            } catch (Throwable t) {
                // Swallow
            }

            System.out.println("Ending testDefaultDataSource002...");
            System.out.println("Ending testDefaultDataSource002...");
        }
    }

    // Use JPA 2.1 API to have default data source enlist in transaction when creating entity manager
    @Test
    public void testDefaultDataSource003() {
        EntityManager em = null;
        try {
            System.out.println("Starting testDefaultDataSource003...");
            System.out.println("Starting testDefaultDataSource003...");

            System.out.println("Beginning Transaction...");
            tx.begin();

            System.out.println("Acquiring EntityManager(SYNCHRONIZED)...");
            em = emfDDS.createEntityManager(SynchronizationType.SYNCHRONIZED);

            System.out.println("Creating DSEntity...");
            DSEntity newEntity = new DSEntity();
            newEntity.setStrData("String-Data-03a");

            System.out.println("Persisting DSEntity...");
            em.persist(newEntity);

            System.out.println("New Entity: " + newEntity);

            System.out.println("Comitting Transaction...");
            tx.commit();

            em.clear();

            CriteriaBuilder b = em.getCriteriaBuilder();
            CriteriaQuery<DSEntity> q = b.createQuery(DSEntity.class);
            Expression<String> strData = q.from(DSEntity.class).get("strData");
            List<DSEntity> results = em.createQuery(q.where(b.like(strData, "String-Data-03_"))).getResultList();

            if (results.size() != 1) {
                System.out.println("ERROR: Exactly one entity should have committed. Found " + results);
                return;
            }

            String value = results.get(0).getStrData();
            if (!"String-Data-03a".equals(value)) {
                System.out.println("ERROR: Unexpected value committed " + value);
                return;
            }

            System.out.println("SUCCESS:testDefaultDataSource003");
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("ERROR: Caught unexpected Exception: " + t);
        } finally {
            try {
                tx.rollback();
            } catch (Throwable t) {
                // Swallow
            }

            System.out.println("Ending testDefaultDataSource003...");
            System.out.println("Ending testDefaultDataSource003...");
            if (em != null)
                em.close();
        }
    }

    // Use JPA 2.1 API to have default data source NOT enlist in transaction when creating entity manager
    @Test
    public void testDefaultDataSource004() {
        EntityManager em = null;
        try {
            System.out.println("Starting testDefaultDataSource004...");
            System.out.println("Starting testDefaultDataSource004...");

            System.out.println("Beginning Transaction...");
            tx.begin();

            System.out.println("Acquiring EntityManager(UNSYNCHRONIZED)...");
            em = emfDDS.createEntityManager(SynchronizationType.UNSYNCHRONIZED);

            System.out.println("Creating DSEntity...");
            DSEntity newEntity = new DSEntity();
            newEntity.setStrData("String-Data-04a");

            System.out.println("Persisting DSEntity...");
            em.persist(newEntity);

            System.out.println("New Entity: " + newEntity);

            System.out.println("Comitting Transaction...");
            tx.commit();

            em.clear();

            CriteriaBuilder b = em.getCriteriaBuilder();
            CriteriaQuery<DSEntity> q = b.createQuery(DSEntity.class);
            Expression<String> strData = q.from(DSEntity.class).get("strData");
            List<DSEntity> results = em.createQuery(q.where(b.like(strData, "String-Data-04"))).getResultList();

            if (!results.isEmpty()) {
                System.out.println("ERROR: No entities should have committed. Found " + results);
                return;
            }

            System.out.println("SUCCESS:testDefaultDataSource004");
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("ERROR: Caught unexpected Exception: " + t);
        } finally {
            try {
                tx.rollback();
            } catch (Throwable t) {
                // Swallow
            }

            System.out.println("Ending testDefaultDataSource004...");
            System.out.println("Ending testDefaultDataSource004...");
            if (em != null)
                em.close();
        }
    }

    // Serialize & deserialize entity manager factory that uses the default data source,
    // and then invoke a JPA 2.1 to unwrap as a JPA provider-specific interface.
    @Test
    public void testDefaultDataSource005() {
        EntityManager em = null;
        try {
            System.out.println("Starting testDefaultDataSource005...");
            System.out.println("Starting testDefaultDataSource005...");

            System.out.println("Serialize entity manager factory");
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteOutput);
            out.writeObject(emfDDS);
            out.flush();
            byte[] bytes = byteOutput.toByteArray();
            out.close();

            System.out.println("Deserialize entity manager factory");
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
            EntityManagerFactory emf = emfDDS; // TODO 207748 enable deserialize once working: (EntityManagerFactory) in.readObject();
            in.close();

            System.out.println("Beginning Transaction...");
            tx.begin();

            // Use a JPA 2.1 API - createEntityManager(SynchronizationType, map)
            System.out.println("Acquiring EntityManager(SYNCHRONIZED, querytimeout=2min)...");
            em = emf.createEntityManager(SynchronizationType.SYNCHRONIZED,
                                         Collections.singletonMap("javax.persistence.query.timeout", TimeUnit.MINUTES.toMillis(2)));

            Long queryTimeout = (Long) em.getProperties().get("javax.persistence.query.timeout");
            if (!Long.valueOf(120000).equals(queryTimeout)) {
                System.out.println("ERROR: Unexpected value of persistence property javax.persistence.query.timeout: " + queryTimeout);
                return;
            }

            System.out.println("Creating DSEntity...");
            DSEntity newEntity = new DSEntity();
            newEntity.setStrData("String-Data-05a");

            System.out.println("Persisting DSEntity...");
            em.persist(newEntity);

            System.out.println("New Entity: " + newEntity);

            // Use another JPA 2.1 API - unwrap
            boolean isConnected = emf.unwrap(org.eclipse.persistence.jpa.JpaEntityManagerFactory.class).getDatabaseSession().isConnected();
            System.out.println("isConnected? " + isConnected);

            System.out.println("Committing Transaction...");
            tx.commit();

            em.clear();

            TypedQuery<DSEntity> query = em.createQuery("SELECT e FROM DSEntity e WHERE e.strData=:s", DSEntity.class);
            query.setParameter("s", "String-Data-05a");
            List<DSEntity> results = query.getResultList();

            if (results.size() != 1) {
                System.out.println("ERROR: Exactly one entity should have committed. Found " + results);
                return;
            }

            String value = results.get(0).getStrData();
            if (!"String-Data-05a".equals(value)) {
                System.out.println("ERROR: Unexpected value committed " + value);
                return;
            }

            System.out.println("SUCCESS:testDefaultDataSource005");
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("ERROR: Caught unexpected Exception: " + t);
        } finally {
            try {
                tx.rollback();
            } catch (Throwable t) {
                // Swallow
            }

            System.out.println("Ending testDefaultDataSource005...");
            System.out.println("Ending testDefaultDataSource005...");
            if (em != null)
                em.close();
        }
    }
}
