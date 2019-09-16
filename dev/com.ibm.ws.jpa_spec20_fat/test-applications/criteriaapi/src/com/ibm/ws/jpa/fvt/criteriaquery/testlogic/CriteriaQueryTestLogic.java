/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.criteriaquery.testlogic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.criteriaquery.entity.Entity0001;
import com.ibm.ws.jpa.fvt.criteriaquery.entity.Entity0002;
import com.ibm.ws.jpa.fvt.criteriaquery.entity.Entity0003;
import com.ibm.ws.jpa.fvt.criteriaquery.entity.Entity0004;
import com.ibm.ws.jpa.fvt.criteriaquery.entity.Entity0005;
import com.ibm.ws.jpa.fvt.criteriaquery.entity.Entity0006;
import com.ibm.ws.jpa.fvt.criteriaquery.entity.Entity0007;
import com.ibm.ws.jpa.fvt.criteriaquery.entity.Entity0008;
import com.ibm.ws.jpa.fvt.criteriaquery.entity.Entity0009;
import com.ibm.ws.jpa.fvt.criteriaquery.entity.Entity0010;
import com.ibm.ws.jpa.fvt.criteriaquery.entity.Entity0011;
import com.ibm.ws.jpa.fvt.criteriaquery.entity.Entity0012;
import com.ibm.ws.jpa.fvt.criteriaquery.entity.Entity0013;
import com.ibm.ws.jpa.fvt.criteriaquery.entity.Entity0014;
import com.ibm.ws.jpa.fvt.criteriaquery.entity.Entity0015;
import com.ibm.ws.jpa.fvt.criteriaquery.entity.Entity0016;
import com.ibm.ws.jpa.fvt.criteriaquery.entity.Entity0017;
import com.ibm.ws.jpa.fvt.criteriaquery.entity.Entity0018;
import com.ibm.ws.jpa.fvt.criteriaquery.entity.Entity0019;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

/**
 *
 */
public class CriteriaQueryTestLogic extends AbstractTestLogic {
    public void testCriteriaQuery_byte(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        EntityManager em = jpaResource.getEm();
        TransactionJacket tx = jpaResource.getTj();

        // Execute Test Case
        try {
            Entity0001 entity = new Entity0001((byte) 01, "Entity0001_STRING01", "Entity0001_STRING02", "Entity0001_STRING03");

            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();
            em.persist(entity);
            tx.commitTransaction();

            em.clear();
            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Entity0001> cq = cb.createQuery(Entity0001.class);
            Root<Entity0001> root = cq.from(Entity0001.class);
            cq.select(root).where(cb.equal(root.get("entity0001_id"), (byte) 01));
            TypedQuery<Entity0001> tq = em.createQuery(cq);
            Entity0001 findEntity = tq.getSingleResult();
            System.out.println("Object returned by query: " + findEntity);

            assertNotNull("Did not find entity in criteria query", findEntity);
            assertTrue("Should not receive original object from query", entity != findEntity);
            assertTrue("Entity returned by find was not contained in the persistence context.", em.contains(findEntity));
            assertEquals(findEntity.getEntity0001_id(), (byte) 01);
            assertEquals(findEntity.getEntity0001_string01(), "Entity0001_STRING01");
            assertEquals(findEntity.getEntity0001_string02(), "Entity0001_STRING02");
            assertEquals(findEntity.getEntity0001_string03(), "Entity0001_STRING03");

            tx.commitTransaction();

            // TODO add metamodel key testing for each test case
            // see http://openjpa.apache.org/builds/2.2.2/apache-openjpa/docs/manual#d5e5275
            // for info on how to generate metamodel classes

            // Find using metamodel key
//            findEntity0001 = null;
//            Predicate condition0001 = cb0001.equal(root0001.get(Entity0001_.entity0001_id), 1);
//            cq0001.select(root0001).where(condition0001);
//            tq0001 = jpaRW.getEm().createQuery(cq0001);
//            findEntity0001 = tq0001.getSingleResult();
//            results.addInfo("Object returned by query: " + findEntity0001);
//            results.assertNotNull("Assert that the query operation did not return null", findEntity0001);
//            if (findEntity0001 == null) {
//                return results;
//            }
//            else {
//                results.addInfo     ( "Perform parent verifications...");
//                results.assertTrue  ( "Assert find did not return the original object", newEntity0001 != findEntity0001);
//                results.assertTrue  ( "Assert entity returned by find is managed by the persistence context.", jpaRW.getEm().contains(findEntity0001));
//                results.assertEQ    ( "Assert for the entity id",     findEntity0001.getEntity0001_id(), (byte)01);
//                results.assertEquals( "Assert for the entity fields", findEntity0001.getEntity0001_string01(), "ENTITY0001_STRING01");
//                results.assertEquals( "Assert for the entity fields", findEntity0001.getEntity0001_string02(), "ENTITY0001_STRING02");
//                results.assertEquals( "Assert for the entity fields", findEntity0001.getEntity0001_string03(), "ENTITY0001_STRING03");
//            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCriteriaQuery_Byte(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        EntityManager em = jpaResource.getEm();
        TransactionJacket tx = jpaResource.getTj();

        // Execute Test Case
        try {
            Entity0002 entity = new Entity0002(new Byte((byte) 02), "Entity0002_STRING01", "Entity0002_STRING02", "Entity0002_STRING03");

            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();
            em.persist(entity);
            tx.commitTransaction();

            em.clear();
            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Entity0002> cq = cb.createQuery(Entity0002.class);
            Root<Entity0002> root = cq.from(Entity0002.class);
            cq.select(root);
            TypedQuery<Entity0002> tq = em.createQuery(cq);
            Entity0002 findEntity = tq.getSingleResult();
            System.out.println("Object returned by query: " + findEntity);

            assertNotNull("Did not find entity in criteria query", findEntity);
            assertTrue("Should not receive original object from query", entity != findEntity);
            assertTrue("Entity returned by find was not contained in the persistence context.", em.contains(findEntity));
            assertEquals(findEntity.getEntity0002_id(), new Byte((byte) 02));
            assertEquals(findEntity.getEntity0002_string01(), "Entity0002_STRING01");
            assertEquals(findEntity.getEntity0002_string02(), "Entity0002_STRING02");
            assertEquals(findEntity.getEntity0002_string03(), "Entity0002_STRING03");

            tx.commitTransaction();
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCriteriaQuery_char(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        EntityManager em = jpaResource.getEm();
        TransactionJacket tx = jpaResource.getTj();

        // Execute Test Case
        try {
            Entity0003 entity = new Entity0003('3', "Entity0003_STRING01", "Entity0003_STRING02", "Entity0003_STRING03");

            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();
            em.persist(entity);
            tx.commitTransaction();

            em.clear();
            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Entity0003> cq = cb.createQuery(Entity0003.class);
            Root<Entity0003> root = cq.from(Entity0003.class);
            cq.select(root);
            TypedQuery<Entity0003> tq = em.createQuery(cq);
            Entity0003 findEntity = tq.getSingleResult();
            System.out.println("Object returned by query: " + findEntity);

            assertNotNull("Did not find entity in criteria query", findEntity);
            assertTrue("Should not receive original object from query", entity != findEntity);
            assertTrue("Entity returned by find was not contained in the persistence context.", em.contains(findEntity));
            assertEquals(findEntity.getEntity0003_id(), '3');
            assertEquals(findEntity.getEntity0003_string01(), "Entity0003_STRING01");
            assertEquals(findEntity.getEntity0003_string02(), "Entity0003_STRING02");
            assertEquals(findEntity.getEntity0003_string03(), "Entity0003_STRING03");

            tx.commitTransaction();
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCriteriaQuery_Character(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                            Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        EntityManager em = jpaResource.getEm();
        TransactionJacket tx = jpaResource.getTj();

        // Execute Test Case
        try {
            Entity0004 entity = new Entity0004(new Character('4'), "Entity0004_STRING01", "Entity0004_STRING02", "Entity0004_STRING03");

            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();
            em.persist(entity);
            tx.commitTransaction();

            em.clear();
            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Entity0004> cq = cb.createQuery(Entity0004.class);
            Root<Entity0004> root = cq.from(Entity0004.class);
            cq.select(root);
            TypedQuery<Entity0004> tq = em.createQuery(cq);
            Entity0004 findEntity = tq.getSingleResult();
            System.out.println("Object returned by query: " + findEntity);

            assertNotNull("Did not find entity in criteria query", findEntity);
            assertTrue("Should not receive original object from query", entity != findEntity);
            assertTrue("Entity returned by find was not contained in the persistence context.", em.contains(findEntity));
            assertEquals(findEntity.getEntity0004_id(), new Character('4'));
            assertEquals(findEntity.getEntity0004_string01(), "Entity0004_STRING01");
            assertEquals(findEntity.getEntity0004_string02(), "Entity0004_STRING02");
            assertEquals(findEntity.getEntity0004_string03(), "Entity0004_STRING03");

            tx.commitTransaction();
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCriteriaQuery_String(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                         Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        EntityManager em = jpaResource.getEm();
        TransactionJacket tx = jpaResource.getTj();

        // Execute Test Case
        try {
            Entity0005 entity = new Entity0005("5", "Entity0005_STRING01", "Entity0005_STRING02", "Entity0005_STRING03");

            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();
            em.persist(entity);
            tx.commitTransaction();

            em.clear();
            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Entity0005> cq = cb.createQuery(Entity0005.class);
            Root<Entity0005> root = cq.from(Entity0005.class);
            cq.select(root);
            TypedQuery<Entity0005> tq = em.createQuery(cq);
            Entity0005 findEntity = tq.getSingleResult();
            System.out.println("Object returned by query: " + findEntity);

            assertNotNull("Did not find entity in criteria query", findEntity);
            assertTrue("Should not receive original object from query", entity != findEntity);
            assertTrue("Entity returned by find was not contained in the persistence context.", em.contains(findEntity));
            assertEquals(findEntity.getEntity0005_id(), "5");
            assertEquals(findEntity.getEntity0005_string01(), "Entity0005_STRING01");
            assertEquals(findEntity.getEntity0005_string02(), "Entity0005_STRING02");
            assertEquals(findEntity.getEntity0005_string03(), "Entity0005_STRING03");

            tx.commitTransaction();
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCriteriaQuery_double(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                         Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        EntityManager em = jpaResource.getEm();
        TransactionJacket tx = jpaResource.getTj();

        // Execute Test Case
        try {
            Entity0006 entity = new Entity0006(06.06D, "Entity0006_STRING01", "Entity0006_STRING02", "Entity0006_STRING03");

            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();
            em.persist(entity);
            tx.commitTransaction();

            em.clear();
            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Entity0006> cq = cb.createQuery(Entity0006.class);
            Root<Entity0006> root = cq.from(Entity0006.class);
            cq.select(root);
            TypedQuery<Entity0006> tq = em.createQuery(cq);
            Entity0006 findEntity = tq.getSingleResult();
            System.out.println("Object returned by query: " + findEntity);

            assertNotNull("Did not find entity in criteria query", findEntity);
            assertTrue("Should not receive original object from query", entity != findEntity);
            assertTrue("Entity returned by find was not contained in the persistence context.", em.contains(findEntity));
            assertEquals(findEntity.getEntity0006_id(), 06.06D, 0.01);
            assertEquals(findEntity.getEntity0006_string01(), "Entity0006_STRING01");
            assertEquals(findEntity.getEntity0006_string02(), "Entity0006_STRING02");
            assertEquals(findEntity.getEntity0006_string03(), "Entity0006_STRING03");

            tx.commitTransaction();
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCriteriaQuery_Double(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                         Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        EntityManager em = jpaResource.getEm();
        TransactionJacket tx = jpaResource.getTj();

        // Execute Test Case
        try {
            Entity0007 entity = new Entity0007(new Double(07.07D), "Entity0007_STRING01", "Entity0007_STRING02", "Entity0007_STRING03");

            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();
            em.persist(entity);
            tx.commitTransaction();

            em.clear();
            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Entity0007> cq = cb.createQuery(Entity0007.class);
            Root<Entity0007> root = cq.from(Entity0007.class);
            cq.select(root);
            TypedQuery<Entity0007> tq = em.createQuery(cq);
            Entity0007 findEntity = tq.getSingleResult();
            System.out.println("Object returned by query: " + findEntity);

            assertNotNull("Did not find entity in criteria query", findEntity);
            assertTrue("Should not receive original object from query", entity != findEntity);
            assertTrue("Entity returned by find was not contained in the persistence context.", em.contains(findEntity));
            assertEquals(findEntity.getEntity0007_id(), new Double(07.07D));
            assertEquals(findEntity.getEntity0007_string01(), "Entity0007_STRING01");
            assertEquals(findEntity.getEntity0007_string02(), "Entity0007_STRING02");
            assertEquals(findEntity.getEntity0007_string03(), "Entity0007_STRING03");

            tx.commitTransaction();
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCriteriaQuery_float(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                        Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        EntityManager em = jpaResource.getEm();
        TransactionJacket tx = jpaResource.getTj();

        // Execute Test Case
        try {
            Entity0008 entity = new Entity0008(08.08f, "Entity0008_STRING01", "Entity0008_STRING02", "Entity0008_STRING03");

            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();
            em.persist(entity);
            tx.commitTransaction();

            em.clear();
            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Entity0008> cq = cb.createQuery(Entity0008.class);
            Root<Entity0008> root = cq.from(Entity0008.class);
            cq.select(root);
            TypedQuery<Entity0008> tq = em.createQuery(cq);
            Entity0008 findEntity = tq.getSingleResult();
            System.out.println("Object returned by query: " + findEntity);

            assertNotNull("Did not find entity in criteria query", findEntity);
            assertTrue("Should not receive original object from query", entity != findEntity);
            assertTrue("Entity returned by find was not contained in the persistence context.", em.contains(findEntity));
            assertEquals(findEntity.getEntity0008_id(), 08.08f, 0.01);
            assertEquals(findEntity.getEntity0008_string01(), "Entity0008_STRING01");
            assertEquals(findEntity.getEntity0008_string02(), "Entity0008_STRING02");
            assertEquals(findEntity.getEntity0008_string03(), "Entity0008_STRING03");

            tx.commitTransaction();
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCriteriaQuery_Float(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                        Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        EntityManager em = jpaResource.getEm();
        TransactionJacket tx = jpaResource.getTj();

        // Execute Test Case
        try {
            Entity0009 entity = new Entity0009(new Float(09.09f), "Entity0009_STRING01", "Entity0009_STRING02", "Entity0009_STRING03");

            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();
            em.persist(entity);
            tx.commitTransaction();

            em.clear();
            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Entity0009> cq = cb.createQuery(Entity0009.class);
            Root<Entity0009> root = cq.from(Entity0009.class);
            cq.select(root);
            TypedQuery<Entity0009> tq = em.createQuery(cq);
            Entity0009 findEntity = tq.getSingleResult();
            System.out.println("Object returned by query: " + findEntity);

            assertNotNull("Did not find entity in criteria query", findEntity);
            assertTrue("Should not receive original object from query", entity != findEntity);
            assertTrue("Entity returned by find was not contained in the persistence context.", em.contains(findEntity));
            assertEquals(findEntity.getEntity0009_id(), new Float(09.09f));
            assertEquals(findEntity.getEntity0009_string01(), "Entity0009_STRING01");
            assertEquals(findEntity.getEntity0009_string02(), "Entity0009_STRING02");
            assertEquals(findEntity.getEntity0009_string03(), "Entity0009_STRING03");

            tx.commitTransaction();
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCriteriaQuery_int(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                      Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        EntityManager em = jpaResource.getEm();
        TransactionJacket tx = jpaResource.getTj();

        // Execute Test Case
        try {
            Entity0010 entity = new Entity0010(10, "Entity0010_STRING01", "Entity0010_STRING02", "Entity0010_STRING03");

            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();
            em.persist(entity);
            tx.commitTransaction();

            em.clear();
            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Entity0010> cq = cb.createQuery(Entity0010.class);
            Root<Entity0010> root = cq.from(Entity0010.class);
            cq.select(root);
            TypedQuery<Entity0010> tq = em.createQuery(cq);
            Entity0010 findEntity = tq.getSingleResult();
            System.out.println("Object returned by query: " + findEntity);

            assertNotNull("Did not find entity in criteria query", findEntity);
            assertTrue("Should not receive original object from query", entity != findEntity);
            assertTrue("Entity returned by find was not contained in the persistence context.", em.contains(findEntity));
            assertEquals(findEntity.getEntity0010_id(), 10);
            assertEquals(findEntity.getEntity0010_string01(), "Entity0010_STRING01");
            assertEquals(findEntity.getEntity0010_string02(), "Entity0010_STRING02");
            assertEquals(findEntity.getEntity0010_string03(), "Entity0010_STRING03");

            tx.commitTransaction();
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCriteriaQuery_Integer(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                          Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        EntityManager em = jpaResource.getEm();
        TransactionJacket tx = jpaResource.getTj();

        // Execute Test Case
        try {
            Entity0011 entity = new Entity0011(new Integer(11), "Entity0011_STRING01", "Entity0011_STRING02", "Entity0011_STRING03");

            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();
            em.persist(entity);
            tx.commitTransaction();

            em.clear();
            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Entity0011> cq = cb.createQuery(Entity0011.class);
            Root<Entity0011> root = cq.from(Entity0011.class);
            cq.select(root);
            TypedQuery<Entity0011> tq = em.createQuery(cq);
            Entity0011 findEntity = tq.getSingleResult();
            System.out.println("Object returned by query: " + findEntity);

            assertNotNull("Did not find entity in criteria query", findEntity);
            assertTrue("Should not receive original object from query", entity != findEntity);
            assertTrue("Entity returned by find was not contained in the persistence context.", em.contains(findEntity));
            assertEquals(findEntity.getEntity0011_id(), new Integer(11));
            assertEquals(findEntity.getEntity0011_string01(), "Entity0011_STRING01");
            assertEquals(findEntity.getEntity0011_string02(), "Entity0011_STRING02");
            assertEquals(findEntity.getEntity0011_string03(), "Entity0011_STRING03");

            tx.commitTransaction();
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCriteriaQuery_long(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        EntityManager em = jpaResource.getEm();
        TransactionJacket tx = jpaResource.getTj();

        // Execute Test Case
        try {
            Entity0012 entity = new Entity0012(12L, "Entity0012_STRING01", "Entity0012_STRING02", "Entity0012_STRING03");

            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();
            em.persist(entity);
            tx.commitTransaction();

            em.clear();
            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Entity0012> cq = cb.createQuery(Entity0012.class);
            Root<Entity0012> root = cq.from(Entity0012.class);
            cq.select(root);
            TypedQuery<Entity0012> tq = em.createQuery(cq);
            Entity0012 findEntity = tq.getSingleResult();
            System.out.println("Object returned by query: " + findEntity);

            assertNotNull("Did not find entity in criteria query", findEntity);
            assertTrue("Should not receive original object from query", entity != findEntity);
            assertTrue("Entity returned by find was not contained in the persistence context.", em.contains(findEntity));
            assertEquals(findEntity.getEntity0012_id(), 12L);
            assertEquals(findEntity.getEntity0012_string01(), "Entity0012_STRING01");
            assertEquals(findEntity.getEntity0012_string02(), "Entity0012_STRING02");
            assertEquals(findEntity.getEntity0012_string03(), "Entity0012_STRING03");

            tx.commitTransaction();
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCriteriaQuery_Long(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        EntityManager em = jpaResource.getEm();
        TransactionJacket tx = jpaResource.getTj();

        // Execute Test Case
        try {
            Entity0013 entity = new Entity0013(new Long(13L), "Entity0013_STRING01", "Entity0013_STRING02", "Entity0013_STRING03");

            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();
            em.persist(entity);
            tx.commitTransaction();

            em.clear();
            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Entity0013> cq = cb.createQuery(Entity0013.class);
            Root<Entity0013> root = cq.from(Entity0013.class);
            cq.select(root);
            TypedQuery<Entity0013> tq = em.createQuery(cq);
            Entity0013 findEntity = tq.getSingleResult();
            System.out.println("Object returned by query: " + findEntity);

            assertNotNull("Did not find entity in criteria query", findEntity);
            assertTrue("Should not receive original object from query", entity != findEntity);
            assertTrue("Entity returned by find was not contained in the persistence context.", em.contains(findEntity));
            assertEquals(findEntity.getEntity0013_id(), new Long(13L));
            assertEquals(findEntity.getEntity0013_string01(), "Entity0013_STRING01");
            assertEquals(findEntity.getEntity0013_string02(), "Entity0013_STRING02");
            assertEquals(findEntity.getEntity0013_string03(), "Entity0013_STRING03");

            tx.commitTransaction();
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCriteriaQuery_short(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                        Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        EntityManager em = jpaResource.getEm();
        TransactionJacket tx = jpaResource.getTj();

        // Execute Test Case
        try {
            Entity0014 entity = new Entity0014((short) 14, "Entity0014_STRING01", "Entity0014_STRING02", "Entity0014_STRING03");

            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();
            em.persist(entity);
            tx.commitTransaction();

            em.clear();
            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Entity0014> cq = cb.createQuery(Entity0014.class);
            Root<Entity0014> root = cq.from(Entity0014.class);
            cq.select(root);
            TypedQuery<Entity0014> tq = em.createQuery(cq);
            Entity0014 findEntity = tq.getSingleResult();
            System.out.println("Object returned by query: " + findEntity);

            assertNotNull("Did not find entity in criteria query", findEntity);
            assertTrue("Should not receive original object from query", entity != findEntity);
            assertTrue("Entity returned by find was not contained in the persistence context.", em.contains(findEntity));
            assertEquals(findEntity.getEntity0014_id(), (short) 14);
            assertEquals(findEntity.getEntity0014_string01(), "Entity0014_STRING01");
            assertEquals(findEntity.getEntity0014_string02(), "Entity0014_STRING02");
            assertEquals(findEntity.getEntity0014_string03(), "Entity0014_STRING03");

            tx.commitTransaction();
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCriteriaQuery_Short(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                        Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        EntityManager em = jpaResource.getEm();
        TransactionJacket tx = jpaResource.getTj();

        // Execute Test Case
        try {
            Entity0015 entity = new Entity0015(new Short((short) 15), "Entity0015_STRING01", "Entity0015_STRING02", "Entity0015_STRING03");

            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();
            em.persist(entity);
            tx.commitTransaction();

            em.clear();
            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Entity0015> cq = cb.createQuery(Entity0015.class);
            Root<Entity0015> root = cq.from(Entity0015.class);
            cq.select(root);
            TypedQuery<Entity0015> tq = em.createQuery(cq);
            Entity0015 findEntity = tq.getSingleResult();
            System.out.println("Object returned by query: " + findEntity);

            assertNotNull("Did not find entity in criteria query", findEntity);
            assertTrue("Should not receive original object from query", entity != findEntity);
            assertTrue("Entity returned by find was not contained in the persistence context.", em.contains(findEntity));
            assertEquals(findEntity.getEntity0015_id(), new Short((short) 15));
            assertEquals(findEntity.getEntity0015_string01(), "Entity0015_STRING01");
            assertEquals(findEntity.getEntity0015_string02(), "Entity0015_STRING02");
            assertEquals(findEntity.getEntity0015_string03(), "Entity0015_STRING03");

            tx.commitTransaction();
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCriteriaQuery_BigDecimal(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                             Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        EntityManager em = jpaResource.getEm();
        TransactionJacket tx = jpaResource.getTj();

        // Execute Test Case
        try {
            Entity0016 entity = new Entity0016(new BigDecimal("16.000000000000016"), "Entity0016_STRING01", "Entity0016_STRING02", "Entity0016_STRING03");

            assertEquals(entity.getEntity0016_id(), new BigDecimal("16.000000000000016"));

            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();
            em.persist(entity);
            tx.commitTransaction();

            em.clear();
            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();

            /*
             * Debug info to show that find also fails. Not part of test normally
             * Entity0016 entity0016 = em.find(Entity0016.class, new BigDecimal("16.000000000000016"));
             * assertNotNull(entity0016);
             * assertEquals(new BigDecimal("16.000000000000016"), entity0016.getEntity0016_id());
             */

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Entity0016> cq = cb.createQuery(Entity0016.class);
            Root<Entity0016> root = cq.from(Entity0016.class);
            cq.select(root);
            TypedQuery<Entity0016> tq = em.createQuery(cq);
            Entity0016 findEntity = tq.getSingleResult();
            System.out.println("Object returned by query: " + findEntity);

            assertNotNull("Did not find entity in criteria query", findEntity);
            assertTrue("Should not receive original object from query", entity != findEntity);
            assertTrue("Entity returned by find was not contained in the persistence context.", em.contains(findEntity));

            // Give some tolerance on the value here
            BigDecimal diff = new BigDecimal("16.000000000000016").subtract(findEntity.getEntity0016_id()).abs();
            if (diff.compareTo(new BigDecimal("0.01")) >= 0)
                throw new Exception("Expected " + new BigDecimal("16.000000000000016") + " but instead got " + findEntity.getEntity0016_id());
            assertEquals(findEntity.getEntity0016_string01(), "Entity0016_STRING01");
            assertEquals(findEntity.getEntity0016_string02(), "Entity0016_STRING02");
            assertEquals(findEntity.getEntity0016_string03(), "Entity0016_STRING03");

            tx.commitTransaction();
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCriteriaQuery_BigInteger(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                             Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        EntityManager em = jpaResource.getEm();
        TransactionJacket tx = jpaResource.getTj();

        // Execute Test Case
        try {
            Entity0017 entity = new Entity0017(new BigInteger("170000000000000000"), "Entity0017_STRING01", "Entity0017_STRING02", "Entity0017_STRING03");

            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();
            em.persist(entity);
            tx.commitTransaction();

            em.clear();
            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Entity0017> cq = cb.createQuery(Entity0017.class);
            Root<Entity0017> root = cq.from(Entity0017.class);
            cq.select(root);
            TypedQuery<Entity0017> tq = em.createQuery(cq);
            Entity0017 findEntity = tq.getSingleResult();
            System.out.println("Object returned by query: " + findEntity);

            assertNotNull("Did not find entity in criteria query", findEntity);
            assertTrue("Should not receive original object from query", entity != findEntity);
            assertTrue("Entity returned by find was not contained in the persistence context.", em.contains(findEntity));
            assertEquals(findEntity.getEntity0017_id(), new BigInteger("170000000000000000"));
            assertEquals(findEntity.getEntity0017_string01(), "Entity0017_STRING01");
            assertEquals(findEntity.getEntity0017_string02(), "Entity0017_STRING02");
            assertEquals(findEntity.getEntity0017_string03(), "Entity0017_STRING03");

            tx.commitTransaction();
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCriteriaQuery_JavaUtilDate(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                               Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        EntityManager em = jpaResource.getEm();
        TransactionJacket tx = jpaResource.getTj();

        // Execute Test Case
        try {
            Entity0018 entity = new Entity0018(new java.util.Date(18, 18, 18), "Entity0018_STRING01", "Entity0018_STRING02", "Entity0018_STRING03");

            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();
            em.persist(entity);
            tx.commitTransaction();

            em.clear();
            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Entity0018> cq = cb.createQuery(Entity0018.class);
            Root<Entity0018> root = cq.from(Entity0018.class);
            cq.select(root);
            TypedQuery<Entity0018> tq = em.createQuery(cq);
            Entity0018 findEntity = tq.getSingleResult();
            System.out.println("Object returned by query: " + findEntity);

            assertNotNull("Did not find entity in criteria query", findEntity);
            assertTrue("Should not receive original object from query", entity != findEntity);
            assertTrue("Entity returned by find was not contained in the persistence context.", em.contains(findEntity));
            assertEquals(findEntity.getEntity0018_id(), new java.util.Date(18, 18, 18));
            assertEquals(findEntity.getEntity0018_string01(), "Entity0018_STRING01");
            assertEquals(findEntity.getEntity0018_string02(), "Entity0018_STRING02");
            assertEquals(findEntity.getEntity0018_string03(), "Entity0018_STRING03");

            tx.commitTransaction();
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCriteriaQuery_JavaSqlDate(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                              Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        EntityManager em = jpaResource.getEm();
        TransactionJacket tx = jpaResource.getTj();

        // Execute Test Case
        try {
            Entity0019 entity = new Entity0019(new java.sql.Date(19, 19, 19), "Entity0019_STRING01", "Entity0019_STRING02", "Entity0019_STRING03");

            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();
            em.persist(entity);
            tx.commitTransaction();

            em.clear();
            tx.beginTransaction();
            if (jpaResource.getPcCtxInfo().getPcType() == PersistenceContextType.APPLICATION_MANAGED_JTA)
                em.joinTransaction();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Entity0019> cq = cb.createQuery(Entity0019.class);
            Root<Entity0019> root = cq.from(Entity0019.class);
            cq.select(root);
            TypedQuery<Entity0019> tq = em.createQuery(cq);
            Entity0019 findEntity = tq.getSingleResult();
            System.out.println("Object returned by query: " + findEntity);

            assertNotNull("Did not find entity in criteria query", findEntity);
            assertTrue("Should not receive original object from query", entity != findEntity);
            assertTrue("Entity returned by find was not contained in the persistence context.", em.contains(findEntity));
            assertEquals(findEntity.getEntity0019_id(), new java.sql.Date(19, 19, 19));
            assertEquals(findEntity.getEntity0019_string01(), "Entity0019_STRING01");
            assertEquals(findEntity.getEntity0019_string02(), "Entity0019_STRING02");
            assertEquals(findEntity.getEntity0019_string03(), "Entity0019_STRING03");

            tx.commitTransaction();
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void template(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                         Object managedComponentObject) {
        final String testName = getTestName();

        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail(testName + ": Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        final JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));
        final String dbProductVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductVersion") == null) ? "UNKNOWN" : (String) testProps.get("dbProductVersion"));
        final String jdbcDriverVersion = (testProps == null) ? "UNKNOWN" : ((testProps.get("jdbcDriverVersion") == null) ? "UNKNOWN" : (String) testProps.get("jdbcDriverVersion"));

        EntityManager em = jpaResource.getEm();
        TransactionJacket tx = jpaResource.getTj();

        // Execute Test Case
        try {

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }
}
