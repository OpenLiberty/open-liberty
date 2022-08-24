/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.jpa.tests.jpa31.web;

import java.util.UUID;

import org.junit.Test;

import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

import io.openliberty.jpa.tests.jpa31.models.EmbeddableUUID_ID;
import io.openliberty.jpa.tests.jpa31.models.UUIDAutoGenEntity;
import io.openliberty.jpa.tests.jpa31.models.UUIDEmbeddableIdEntity;
import io.openliberty.jpa.tests.jpa31.models.UUIDEntity;
import io.openliberty.jpa.tests.jpa31.models.UUIDIdClassEntity;
import io.openliberty.jpa.tests.jpa31.models.UUIDUUIDGenEntity;
import io.openliberty.jpa.tests.jpa31.models.UUID_IDClass;
import io.openliberty.jpa.tests.jpa31.models.XMLEmbeddableUUID_ID;
import io.openliberty.jpa.tests.jpa31.models.XMLUUIDAutoGenEntity;
import io.openliberty.jpa.tests.jpa31.models.XMLUUIDEmbeddableIdEntity;
import io.openliberty.jpa.tests.jpa31.models.XMLUUIDEntity;
import io.openliberty.jpa.tests.jpa31.models.XMLUUIDUUIDGenEntity;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;
import junit.framework.Assert;

@WebServlet(urlPatterns = "/TestUUIDEntityIDServlet")
public class TestUUIDEntityIDServlet extends JPATestServlet {
    private static final long serialVersionUID = 1L;

    private final static String PUNAME = "UUID";

    @PersistenceUnit(unitName = PUNAME + "_JTA")
    private EntityManagerFactory emfJta;

    @PersistenceUnit(unitName = PUNAME + "_RL")
    private EntityManagerFactory emfRl;

    @PersistenceContext(unitName = PUNAME + "_JTA")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    /**
     * Verify that an entity using a UUID type for its identity can be persisted to and fetched from the database.
     */
    @Test
    public void testBasicUUIDIdentity_JTA() {
        EntityManager tem = null;

        try {
            tem = emfJta.createEntityManager();
            Assert.assertNotNull(tem);
            Assert.assertTrue(tem.isOpen());

            UUID id = UUID.randomUUID();
            UUIDEntity entity = new UUIDEntity();
            entity.setId(id);
            entity.setStrData("Some string data");

            tx.begin();
            tem.joinTransaction();
            tem.persist(entity);
            tx.commit();

            System.out.println("Persisted entity " + entity);

            tem.clear();
            Assert.assertFalse(tem.contains(entity));

            UUIDEntity findEntity = em.find(UUIDEntity.class, id);
            Assert.assertNotNull(findEntity);
            Assert.assertEquals(id, findEntity.getId());
            Assert.assertNotSame(id, findEntity.getId());
            Assert.assertNotSame(entity, findEntity);

        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            if (tem != null && tem.isOpen()) {
                tem.close();
            }
        }
    }

    /**
     * Verify that an entity using a UUID type for its identity can be persisted to and fetched from the database. Entity defined in XML.
     */
    @Test
    public void testBasicUUIDIdentity_JTA_XML() {
        EntityManager tem = null;

        try {
            tem = emfJta.createEntityManager();
            Assert.assertNotNull(tem);
            Assert.assertTrue(tem.isOpen());

            UUID id = UUID.randomUUID();
            XMLUUIDEntity entity = new XMLUUIDEntity();
            entity.setId(id);
            entity.setStrData("Some string data");

            tx.begin();
            tem.joinTransaction();
            tem.persist(entity);
            tx.commit();

            System.out.println("Persisted entity " + entity);

            tem.clear();
            Assert.assertFalse(tem.contains(entity));

            XMLUUIDEntity findEntity = em.find(XMLUUIDEntity.class, id);
            Assert.assertNotNull(findEntity);
            Assert.assertEquals(id, findEntity.getId());
            Assert.assertNotSame(id, findEntity.getId());
            Assert.assertNotSame(entity, findEntity);

        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            if (tem != null && tem.isOpen()) {
                tem.close();
            }
        }
    }

    /**
     * Verify that an entity using a UUID in an embeddable id type for its identity can be persisted to and fetched from the database.
     */
    @Test
    public void testBasic_EmbeddableID_UUIDIdentity_JTA() {
        EntityManager tem = null;

        try {
            tem = emfJta.createEntityManager();
            Assert.assertNotNull(tem);
            Assert.assertTrue(tem.isOpen());

            UUID id = UUID.randomUUID();
            EmbeddableUUID_ID emb_id = new EmbeddableUUID_ID();
            emb_id.setId(id);

            UUIDEmbeddableIdEntity entity = new UUIDEmbeddableIdEntity();
            entity.setId(emb_id);
            entity.setStrData("Some string data");

            tx.begin();
            tem.joinTransaction();
            tem.persist(entity);
            tx.commit();

            System.out.println("Persisted entity " + entity);

            tem.clear();
            Assert.assertFalse(tem.contains(entity));

            EmbeddableUUID_ID find_emb_id = new EmbeddableUUID_ID();
            find_emb_id.setId(id);

            UUIDEmbeddableIdEntity findEntity = em.find(UUIDEmbeddableIdEntity.class, find_emb_id);
            Assert.assertNotNull(findEntity);
            Assert.assertEquals(emb_id, findEntity.getId());
            Assert.assertNotSame(emb_id, findEntity.getId());
            Assert.assertNotSame(entity, findEntity);

        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            if (tem != null && tem.isOpen()) {
                tem.close();
            }
        }
    }

    /**
     * Verify that an entity using a UUID in an embeddable id type for its identity can be persisted to and fetched from the database. XML variant.
     */
    @Test
    public void testBasic_EmbeddableID_UUIDIdentity_JTA_XML() {
        EntityManager tem = null;

        try {
            tem = emfJta.createEntityManager();
            Assert.assertNotNull(tem);
            Assert.assertTrue(tem.isOpen());

            UUID id = UUID.randomUUID();
            XMLEmbeddableUUID_ID emb_id = new XMLEmbeddableUUID_ID();
            emb_id.setEId(id);

            XMLUUIDEmbeddableIdEntity entity = new XMLUUIDEmbeddableIdEntity();
            entity.setId(emb_id);
            entity.setStrData("Some string data");

            tx.begin();
            tem.joinTransaction();
            tem.persist(entity);
            tx.commit();

            System.out.println("Persisted entity " + entity);

            tem.clear();
            Assert.assertFalse(tem.contains(entity));

            XMLEmbeddableUUID_ID find_emb_id = new XMLEmbeddableUUID_ID();
            find_emb_id.setEId(id);

            XMLUUIDEmbeddableIdEntity findEntity = em.find(XMLUUIDEmbeddableIdEntity.class, find_emb_id);
            Assert.assertNotNull(findEntity);
            Assert.assertEquals(emb_id, findEntity.getId());
            Assert.assertNotSame(emb_id, findEntity.getId());
            Assert.assertNotSame(entity, findEntity);

        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            if (tem != null && tem.isOpen()) {
                tem.close();
            }
        }
    }

    /**
     * Verify that an entity using a UUID in an IDClass type for its identity can be persisted to and fetched from the database.
     */
    @Test
    public void testBasic_IDClass_UUIDIdentity_JTA() {
        EntityManager tem = null;

        try {
            tem = emfJta.createEntityManager();
            Assert.assertNotNull(tem);
            Assert.assertTrue(tem.isOpen());

            UUID id = UUID.randomUUID();
            long lid = System.currentTimeMillis();

            UUIDIdClassEntity entity = new UUIDIdClassEntity();
            entity.setUuid_id(id);
            entity.setL_id(lid);
            entity.setStrData("Some string data");

            tx.begin();
            tem.joinTransaction();
            tem.persist(entity);
            tx.commit();

            System.out.println("Persisted entity " + entity);

            tem.clear();
            Assert.assertFalse(tem.contains(entity));

            UUID_IDClass find_idclass = new UUID_IDClass();
            find_idclass.setUuid_id(id);
            find_idclass.setL_id(lid);

            UUIDIdClassEntity findEntity = em.find(UUIDIdClassEntity.class, find_idclass);
            Assert.assertNotNull(findEntity);
            Assert.assertNotSame(id, findEntity.getUuid_id());
            Assert.assertNotSame(entity, findEntity);

        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            if (tem != null && tem.isOpen()) {
                tem.close();
            }
        }
    }

    /**
     * Verify that an entity using a UUID type for its identity can use the AUTO generator for primary key generation.
     */
    @Test
    public void testBasicUUIDIdentity_AUTO_Generator_JTA() {
        EntityManager tem = null;

        try {
            tem = emfJta.createEntityManager();
            Assert.assertNotNull(tem);
            Assert.assertTrue(tem.isOpen());

            UUIDAutoGenEntity entity = new UUIDAutoGenEntity();
            entity.setStrData("Some string data");

            tx.begin();
            tem.joinTransaction();
            tem.persist(entity);
            tx.commit();

            System.out.println("Persisted entity " + entity);
            UUID id = entity.getId();
            Assert.assertNotNull(id);

            tem.clear();
            Assert.assertFalse(tem.contains(entity));

            UUIDAutoGenEntity findEntity = em.find(UUIDAutoGenEntity.class, id);
            Assert.assertNotNull(findEntity);
            Assert.assertEquals(id, findEntity.getId());
            Assert.assertNotSame(id, findEntity.getId());
            Assert.assertNotSame(entity, findEntity);

        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            if (tem != null && tem.isOpen()) {
                tem.close();
            }
        }
    }

    /**
     * Verify that an entity using a UUID type for its identity can use the AUTO generator for primary key generation.
     */
    @Test
    public void testBasicUUIDIdentity_AUTO_Generator_JTA_XML() {
        EntityManager tem = null;

        try {
            tem = emfJta.createEntityManager();
            Assert.assertNotNull(tem);
            Assert.assertTrue(tem.isOpen());

            XMLUUIDAutoGenEntity entity = new XMLUUIDAutoGenEntity();
            entity.setStrData("Some string data");

            tx.begin();
            tem.joinTransaction();
            tem.persist(entity);
            tx.commit();

            System.out.println("Persisted entity " + entity);
            UUID id = entity.getId();
            Assert.assertNotNull(id);

            tem.clear();
            Assert.assertFalse(tem.contains(entity));

            XMLUUIDAutoGenEntity findEntity = em.find(XMLUUIDAutoGenEntity.class, id);
            Assert.assertNotNull(findEntity);
            Assert.assertEquals(id, findEntity.getId());
            Assert.assertNotSame(id, findEntity.getId());
            Assert.assertNotSame(entity, findEntity);

        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            if (tem != null && tem.isOpen()) {
                tem.close();
            }
        }
    }

    /**
     * Verify that an entity using a UUID type for its identity can use the UUID generator for primary key generation.
     */
    @Test
    public void testBasicUUIDIdentity_UUID_Generator_JTA() {
        EntityManager tem = null;

        try {
            tem = emfJta.createEntityManager();
            Assert.assertNotNull(tem);
            Assert.assertTrue(tem.isOpen());

            UUIDUUIDGenEntity entity = new UUIDUUIDGenEntity();
            entity.setStrData("Some string data");

            tx.begin();
            tem.joinTransaction();
            tem.persist(entity);
            tx.commit();

            System.out.println("Persisted entity " + entity);
            UUID id = entity.getId();
            Assert.assertNotNull(id);

            tem.clear();
            Assert.assertFalse(tem.contains(entity));

            UUIDUUIDGenEntity findEntity = em.find(UUIDUUIDGenEntity.class, id);
            Assert.assertNotNull(findEntity);
            Assert.assertEquals(id, findEntity.getId());
            Assert.assertNotSame(id, findEntity.getId());
            Assert.assertNotSame(entity, findEntity);

        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            if (tem != null && tem.isOpen()) {
                tem.close();
            }
        }
    }

    /**
     * Verify that an entity using a UUID type for its identity can use the UUID generator for primary key generation. XML Variant.
     */
    @Test
    public void testBasicUUIDIdentity_UUID_Generator_JTA_XML() {
        EntityManager tem = null;

        try {
            tem = emfJta.createEntityManager();
            Assert.assertNotNull(tem);
            Assert.assertTrue(tem.isOpen());

            XMLUUIDUUIDGenEntity entity = new XMLUUIDUUIDGenEntity();
            entity.setStrData("Some string data");

            tx.begin();
            tem.joinTransaction();
            tem.persist(entity);
            tx.commit();

            System.out.println("Persisted entity " + entity);
            UUID id = entity.getId();
            Assert.assertNotNull(id);

            tem.clear();
            Assert.assertFalse(tem.contains(entity));

            XMLUUIDUUIDGenEntity findEntity = em.find(XMLUUIDUUIDGenEntity.class, id);
            Assert.assertNotNull(findEntity);
            Assert.assertEquals(id, findEntity.getId());
            Assert.assertNotSame(id, findEntity.getId());
            Assert.assertNotSame(entity, findEntity);

        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            if (tem != null && tem.isOpen()) {
                tem.close();
            }
        }
    }
}
