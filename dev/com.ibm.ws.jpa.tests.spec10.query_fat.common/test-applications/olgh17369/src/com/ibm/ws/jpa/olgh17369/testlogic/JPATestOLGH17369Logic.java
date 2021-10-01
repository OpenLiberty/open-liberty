/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.olgh17369.testlogic;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.SimpleCase;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh17369.model.SimpleEntityOLGH17369;
import com.ibm.ws.jpa.olgh17369.model.SimpleEntityOLGH17369_;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH17369Logic extends AbstractTestLogic {

    public void testQueryCaseLiterals1(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // test 1
            TypedQuery<SimpleEntityOLGH17369> query = em.createQuery(""
                                                                     + "SELECT t FROM SimpleEntityOLGH17369 t "
                                                                     + "WHERE t.itemString1 = ( "
                                                                     + "CASE t.itemInteger1 "
                                                                     + "WHEN 1000 THEN '047010' "
                                                                     + "WHEN 100 THEN '023010' "
                                                                     + "ELSE '033020' "
                                                                     + "END )", SimpleEntityOLGH17369.class);

            List<SimpleEntityOLGH17369> dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(0, dto01.size());

            // Equivalent CriteriaBuilder
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SimpleEntityOLGH17369> cquery = cb.createQuery(SimpleEntityOLGH17369.class);
            Root<SimpleEntityOLGH17369> root = cquery.from(SimpleEntityOLGH17369.class);
            cquery.select(root);

            Expression<Object> selectCase = cb.selectCase(root.get(SimpleEntityOLGH17369_.itemInteger1))
                            .when(1000, "047010")
                            .when(100, "023010")
                            .otherwise("033020");
            cquery.where(cb.equal(root.get(SimpleEntityOLGH17369_.itemString1), selectCase));

            query = em.createQuery(cquery);
            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(0, dto01.size());

            // test 2
            query = em.createQuery(""
                                   + "SELECT t FROM SimpleEntityOLGH17369 t "
                                   + "WHERE t.itemString1 = ( "
                                   + "CASE t.itemInteger1 "
                                   + "WHEN 1 THEN 'A' "
                                   + "WHEN 100 THEN 'B' "
                                   + "ELSE 'C' "
                                   + "END )", SimpleEntityOLGH17369.class);

            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("A", dto01.get(0).getItemString1());
            Assert.assertEquals("B", dto01.get(0).getItemString2());
            Assert.assertEquals("C", dto01.get(0).getItemString3());
            Assert.assertEquals("D", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(1), dto01.get(0).getItemInteger1());

            // Equivalent CriteriaBuilder
            cb = em.getCriteriaBuilder();
            cquery = cb.createQuery(SimpleEntityOLGH17369.class);
            root = cquery.from(SimpleEntityOLGH17369.class);
            cquery.select(root);

            selectCase = cb.selectCase(root.get(SimpleEntityOLGH17369_.itemInteger1))
                            .when(1, "A")
                            .when(100, "B")
                            .otherwise("C");
            cquery.where(cb.equal(root.get(SimpleEntityOLGH17369_.itemString1), selectCase));

            query = em.createQuery(cquery);
            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("A", dto01.get(0).getItemString1());
            Assert.assertEquals("B", dto01.get(0).getItemString2());
            Assert.assertEquals("C", dto01.get(0).getItemString3());
            Assert.assertEquals("D", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(1), dto01.get(0).getItemInteger1());
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testQueryCaseParameters1(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // test 1
            TypedQuery<SimpleEntityOLGH17369> query = em.createQuery(""
                                                                     + "SELECT t FROM SimpleEntityOLGH17369 t "
                                                                     + "WHERE t.itemString1 = ( "
                                                                     + "CASE t.itemInteger1 "
                                                                     + "WHEN ?1 THEN ?2 "
                                                                     + "WHEN ?3 THEN ?4 "
                                                                     + "ELSE ?5 "
                                                                     + "END )", SimpleEntityOLGH17369.class);
            query.setParameter(1, 1000);
            query.setParameter(2, "047010");
            query.setParameter(3, 100);
            query.setParameter(4, "023010");
            query.setParameter(5, "033020");

            List<SimpleEntityOLGH17369> dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(0, dto01.size());

            // Equivalent CriteriaBuilder
//            CriteriaBuilder cb = em.getCriteriaBuilder();
//            CriteriaQuery<SimpleEntityOLGH17369> cquery = cb.createQuery(SimpleEntityOLGH17369.class);
//            Root<SimpleEntityOLGH17369> root = cquery.from(SimpleEntityOLGH17369.class);
//            cquery.multiselect(root);
//
//            ParameterExpression<Integer> checkParam1 = cb.parameter(Integer.class);
//            ParameterExpression<Integer> checkParam2 = cb.parameter(Integer.class);
//            ParameterExpression<String> resultParam1 = cb.parameter(String.class);
//            ParameterExpression<String> resultParam2 = cb.parameter(String.class);
//            ParameterExpression<String> resultParam3 = cb.parameter(String.class);
//
//            // Currently unsupported by the JPA API
//            // https://github.com/eclipse-ee4j/jpa-api/issues/315
////            Expression<Object> selectCase = cb.selectCase(root.get(SimpleEntityOLGH17369_.itemInteger1))
////                .when(checkParam1, resultParam1)
////                .when(checkParam2, resultParam2)
////                .otherwise(resultParam3);
////            Predicate pred = cb.equal(root.get(SimpleEntityOLGH17369_.itemString1), selectCase);
////            cquery.where(pred);
//
//            query = em.createQuery(cquery);
//            query.setParameter(checkParam1, 1000);
//            query.setParameter(resultParam1, "047010");
//            query.setParameter(checkParam2, 100);
//            query.setParameter(resultParam2, "023010");
//            query.setParameter(resultParam3, "033020");
//
//            dto01 = query.getResultList();
//            Assert.assertNotNull(dto01);
//            Assert.assertEquals(0, dto01.size());

            // test 2
            query = em.createQuery(""
                                   + "SELECT t FROM SimpleEntityOLGH17369 t "
                                   + "WHERE t.itemString1 = ( "
                                   + "CASE t.itemInteger1 "
                                   + "WHEN ?1 THEN ?2 "
                                   + "WHEN ?3 THEN ?4 "
                                   + "ELSE ?5 "
                                   + "END )", SimpleEntityOLGH17369.class);
            query.setParameter(1, 1);
            query.setParameter(2, "A");
            query.setParameter(3, 100);
            query.setParameter(4, "B");
            query.setParameter(5, "C");

            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("A", dto01.get(0).getItemString1());
            Assert.assertEquals("B", dto01.get(0).getItemString2());
            Assert.assertEquals("C", dto01.get(0).getItemString3());
            Assert.assertEquals("D", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(1), dto01.get(0).getItemInteger1());

            // Equivalent CriteriaBuilder
//            cb = em.getCriteriaBuilder();
//            cquery = cb.createQuery(SimpleEntityOLGH17369.class);
//            root = cquery.from(SimpleEntityOLGH17369.class);
//            cquery.multiselect(root);
//
//            checkParam1 = cb.parameter(Integer.class);
//            checkParam2 = cb.parameter(Integer.class);
//            resultParam1 = cb.parameter(String.class);
//            resultParam2 = cb.parameter(String.class);
//            resultParam3 = cb.parameter(String.class);
//
//            // Currently unsupported by the JPA API
//            // https://github.com/eclipse-ee4j/jpa-api/issues/315
////            selectCase = cb.selectCase(root.get(SimpleEntityOLGH17369_.itemInteger1))
////                .when(checkParam1, resultParam1)
////                .when(checkParam2, resultParam2)
////                .otherwise(resultParam3);
////            pred = cb.equal(root.get(SimpleEntityOLGH17369_.itemString1), selectCase);
////            cquery.where(pred);
//
//            query = em.createQuery(cquery);
//            query.setParameter(checkParam1, 1);
//            query.setParameter(resultParam1, "A");
//            query.setParameter(checkParam2, 100);
//            query.setParameter(resultParam2, "B");
//            query.setParameter(resultParam3, "C");
//
//            dto01 = query.getResultList();
//            Assert.assertNotNull(dto01);
//            Assert.assertEquals(1, dto01.size());
//
//            Assert.assertEquals("A", dto01.get(0).getItemString1());
//            Assert.assertEquals("B", dto01.get(0).getItemString2());
//            Assert.assertEquals("C", dto01.get(0).getItemString3());
//            Assert.assertEquals("D", dto01.get(0).getItemString4());
//            Assert.assertEquals(new Integer(1), dto01.get(0).getItemInteger1());
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testQueryCaseLiterals2(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // test 1
            TypedQuery<SimpleEntityOLGH17369> query = em.createQuery(""
                                                                     + "SELECT t FROM SimpleEntityOLGH17369 t "
                                                                     + "WHERE t.itemString1 = ( "
                                                                     + "CASE "
                                                                     + "WHEN t.itemInteger1 = 1000 THEN '047010' "
                                                                     + "WHEN t.itemInteger1 = 100 THEN '023010' "
                                                                     + "ELSE '033020' "
                                                                     + "END )", SimpleEntityOLGH17369.class);

            List<SimpleEntityOLGH17369> dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(0, dto01.size());

            // Equivalent CriteriaBuilder
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SimpleEntityOLGH17369> cquery = cb.createQuery(SimpleEntityOLGH17369.class);
            Root<SimpleEntityOLGH17369> root = cquery.from(SimpleEntityOLGH17369.class);
            cquery.select(root);

            Expression<String> selectCase = cb.<String> selectCase()
                            .when(cb.equal(root.get(SimpleEntityOLGH17369_.itemInteger1), 1000), "047010")
                            .when(cb.equal(root.get(SimpleEntityOLGH17369_.itemInteger1), 100), "023010")
                            .otherwise("033020");
            cquery.where(cb.equal(root.get(SimpleEntityOLGH17369_.itemString1), selectCase));

            query = em.createQuery(cquery);
            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(0, dto01.size());

            // test 2
            query = em.createQuery(""
                                   + "SELECT t FROM SimpleEntityOLGH17369 t "
                                   + "WHERE t.itemString1 = ( "
                                   + "CASE "
                                   + "WHEN t.itemInteger1 = 1 THEN 'A' "
                                   + "WHEN t.itemInteger1 = 100 THEN 'B' "
                                   + "ELSE 'C' "
                                   + "END )", SimpleEntityOLGH17369.class);

            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("A", dto01.get(0).getItemString1());
            Assert.assertEquals("B", dto01.get(0).getItemString2());
            Assert.assertEquals("C", dto01.get(0).getItemString3());
            Assert.assertEquals("D", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(1), dto01.get(0).getItemInteger1());

            // Equivalent CriteriaBuilder
            cb = em.getCriteriaBuilder();
            cquery = cb.createQuery(SimpleEntityOLGH17369.class);
            root = cquery.from(SimpleEntityOLGH17369.class);
            cquery.select(root);

            selectCase = cb.<String> selectCase()
                            .when(cb.and(
                                         cb.equal(root.get(SimpleEntityOLGH17369_.itemInteger1), 1),
                                         cb.equal(root.get(SimpleEntityOLGH17369_.KeyString), "Key01")),
                                  "A")
                            .when(cb.equal(root.get(SimpleEntityOLGH17369_.itemInteger1), 100), "B")
                            .otherwise("C");
            cquery.where(cb.equal(root.get(SimpleEntityOLGH17369_.itemString1), selectCase));

            query = em.createQuery(cquery);
            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("A", dto01.get(0).getItemString1());
            Assert.assertEquals("B", dto01.get(0).getItemString2());
            Assert.assertEquals("C", dto01.get(0).getItemString3());
            Assert.assertEquals("D", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(1), dto01.get(0).getItemInteger1());
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testQueryCaseParameters2(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // test 1
            TypedQuery<SimpleEntityOLGH17369> query = em.createQuery(""
                                                                     + "SELECT t FROM SimpleEntityOLGH17369 t "
                                                                     + "WHERE t.itemString1 = ( "
                                                                     + "CASE "
                                                                     + "WHEN t.itemInteger1 = ?1 THEN ?2 "
                                                                     + "WHEN t.itemInteger1 = ?3 THEN ?4 "
                                                                     + "ELSE ?5 "
                                                                     + "END )", SimpleEntityOLGH17369.class);
            query.setParameter(1, 1000);
            query.setParameter(2, "047010");
            query.setParameter(3, 100);
            query.setParameter(4, "023010");
            query.setParameter(5, "033020");

            List<SimpleEntityOLGH17369> dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(0, dto01.size());

            // Equivalent CriteriaBuilder
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SimpleEntityOLGH17369> cquery = cb.createQuery(SimpleEntityOLGH17369.class);
            Root<SimpleEntityOLGH17369> root = cquery.from(SimpleEntityOLGH17369.class);
            cquery.select(root);

            ParameterExpression<Integer> checkParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> checkParam2 = cb.parameter(Integer.class);
            ParameterExpression<String> resultParam1 = cb.parameter(String.class);
            ParameterExpression<String> resultParam2 = cb.parameter(String.class);
            ParameterExpression<String> resultParam3 = cb.parameter(String.class);

            Expression<String> selectCase = cb.<String> selectCase()
                            .when(cb.equal(root.get(SimpleEntityOLGH17369_.itemInteger1), checkParam1), resultParam1)
                            .when(cb.equal(root.get(SimpleEntityOLGH17369_.itemInteger1), checkParam2), resultParam2)
                            .otherwise(resultParam3);
            cquery.where(cb.equal(root.get(SimpleEntityOLGH17369_.itemString1), selectCase));

            query = em.createQuery(cquery);
            query.setParameter(checkParam1, 1000);
            query.setParameter(resultParam1, "047010");
            query.setParameter(checkParam2, 100);
            query.setParameter(resultParam2, "023010");
            query.setParameter(resultParam3, "033020");

            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(0, dto01.size());

            // test 2
            query = em.createQuery(""
                                   + "SELECT t FROM SimpleEntityOLGH17369 t "
                                   + "WHERE t.itemString1 = ( "
                                   + "CASE "
                                   + "WHEN t.itemInteger1 = ?1 THEN ?2 "
                                   + "WHEN t.itemInteger1 = ?3 THEN ?4 "
                                   + "ELSE ?5 "
                                   + "END )", SimpleEntityOLGH17369.class);
            query.setParameter(1, 1);
            query.setParameter(2, "A");
            query.setParameter(3, 100);
            query.setParameter(4, "B");
            query.setParameter(5, "C");

            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("A", dto01.get(0).getItemString1());
            Assert.assertEquals("B", dto01.get(0).getItemString2());
            Assert.assertEquals("C", dto01.get(0).getItemString3());
            Assert.assertEquals("D", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(1), dto01.get(0).getItemInteger1());

            // Equivalent CriteriaBuilder
            cb = em.getCriteriaBuilder();
            cquery = cb.createQuery(SimpleEntityOLGH17369.class);
            root = cquery.from(SimpleEntityOLGH17369.class);
            cquery.select(root);

            checkParam1 = cb.parameter(Integer.class);
            checkParam2 = cb.parameter(Integer.class);
            resultParam1 = cb.parameter(String.class);
            resultParam2 = cb.parameter(String.class);
            resultParam3 = cb.parameter(String.class);

            selectCase = cb.<String> selectCase()
                            .when(cb.equal(root.get(SimpleEntityOLGH17369_.itemInteger1), checkParam1), resultParam1)
                            .when(cb.equal(root.get(SimpleEntityOLGH17369_.itemInteger1), checkParam2), resultParam2)
                            .otherwise(resultParam3);
            cquery.where(cb.equal(root.get(SimpleEntityOLGH17369_.itemString1), selectCase));

            query = em.createQuery(cquery);
            query.setParameter(checkParam1, 1);
            query.setParameter(resultParam1, "A");
            query.setParameter(checkParam2, 100);
            query.setParameter(resultParam2, "B");
            query.setParameter(resultParam3, "C");

            dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(1, dto01.size());

            Assert.assertEquals("A", dto01.get(0).getItemString1());
            Assert.assertEquals("B", dto01.get(0).getItemString2());
            Assert.assertEquals("C", dto01.get(0).getItemString3());
            Assert.assertEquals("D", dto01.get(0).getItemString4());
            Assert.assertEquals(new Integer(1), dto01.get(0).getItemInteger1());
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testQueryCaseLiterals3(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // test 1
            TypedQuery<Integer> query = em.createQuery(""
                                                       + "SELECT ("
                                                       + "CASE t.itemString2 "
                                                       + "WHEN 'A' THEN 42 "
                                                       + "WHEN 'B' THEN 100 "
                                                       + "ELSE 0 "
                                                       + "END "
                                                       + ") "
                                                       + "FROM SimpleEntityOLGH17369 t", Integer.class);

            List<Integer> intList = query.getResultList();
            Assert.assertNotNull(intList);
            Assert.assertEquals(2, intList.size());
            Assert.assertEquals(new Integer(100), intList.get(0));
            Assert.assertEquals(new Integer(100), intList.get(1));

            // test equivalent CriteriaBuilder
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer> cquery = cb.createQuery(Integer.class);
            Root<SimpleEntityOLGH17369> root = cquery.from(SimpleEntityOLGH17369.class);

            SimpleCase<String, Integer> selectCase = cb.selectCase(root.get(SimpleEntityOLGH17369_.itemString2));
            selectCase.when("A", 42)
                            .when("B", 100)
                            .otherwise(0);

            cquery.select(selectCase);

            query = em.createQuery(cquery);

            intList = query.getResultList();
            Assert.assertNotNull(intList);
            Assert.assertEquals(2, intList.size());
            Assert.assertEquals(new Integer(100), intList.get(0));
            Assert.assertEquals(new Integer(100), intList.get(1));
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testQueryCaseParameters3(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // test 1
            TypedQuery<Number> query = em.createQuery(""
                                                      + "SELECT ("
                                                      + "CASE t.itemString2 "
                                                      + "WHEN ?1 THEN ?2 "
                                                      + "WHEN ?3 THEN ?4 "
                                                      + "ELSE ?5 "
                                                      + "END "
                                                      + ") "
                                                      + "FROM SimpleEntityOLGH17369 t", Number.class);
            query.setParameter(1, "A");
            query.setParameter(2, 42);
            query.setParameter(3, "B");
            query.setParameter(4, 100);
            query.setParameter(5, 0);

            List<Number> intList = query.getResultList();
            Assert.assertNotNull(intList);
            Assert.assertEquals(2, intList.size());
            Assert.assertEquals(new Integer(100).intValue(), intList.get(0).intValue());
            Assert.assertEquals(new Integer(100).intValue(), intList.get(1).intValue());

            // test equivalent CriteriaBuilder
//            CriteriaBuilder cb = em.getCriteriaBuilder();
//            CriteriaQuery<Number> cquery = cb.createQuery(Number.class);
//            Root<SimpleEntityOLGH17369> root = cquery.from(SimpleEntityOLGH17369.class);
//
//            ParameterExpression<String> checkParam1 = cb.parameter(String.class);
//            ParameterExpression<String> checkParam2 = cb.parameter(String.class);
//            ParameterExpression<Integer> resultParam1 = cb.parameter(Integer.class);
//            ParameterExpression<Integer> resultParam2 = cb.parameter(Integer.class);
//            ParameterExpression<Integer> resultParam3 = cb.parameter(Integer.class);
//
//            // Currently unsupported by the JPA API
//            // https://github.com/eclipse-ee4j/jpa-api/issues/315
////            SimpleCase<String, Integer> selectCase = cb.selectCase(root.get(SimpleEntityOLGH17369_.itemString2));
////            selectCase.when(checkParam1, resultParam1)
////                .when(checkParam2, resultParam2)
////                .otherwise(resultParam3);
////
////            cquery.select(selectCase);
//
//            query = em.createQuery(cquery);
//            query.setParameter(checkParam1, "A");
//            query.setParameter(resultParam1, 42);
//            query.setParameter(checkParam2, "B");
//            query.setParameter(resultParam2, 100);
//            query.setParameter(resultParam3, 0);
//
//            intList = query.getResultList();
//            Assert.assertNotNull(intList);
//            Assert.assertEquals(2, intList.size());
//            Assert.assertEquals(new Integer(100).intValue(), intList.get(0).intValue());
//            Assert.assertEquals(new Integer(100).intValue(), intList.get(1).intValue());
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testQueryCaseLiterals4(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // test 1
            TypedQuery<Boolean> query = em.createQuery(""
                                                       + "SELECT ("
                                                       + "CASE "
                                                       + "WHEN t.itemInteger1 = 1 THEN TRUE "
                                                       + "ELSE FALSE "
                                                       + "END "
                                                       + ") "
                                                       + "FROM SimpleEntityOLGH17369 t ORDER BY t.itemInteger1 ASC", Boolean.class);

            List<Boolean> boolList = query.getResultList();
            Assert.assertNotNull(boolList);
            Assert.assertEquals(2, boolList.size());

            // test equivalent CriteriaBuilder
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Boolean> cquery = cb.createQuery(Boolean.class);
            Root<SimpleEntityOLGH17369> root = cquery.from(SimpleEntityOLGH17369.class);

            SimpleCase<Integer, Boolean> selectCase = cb.selectCase(root.get(SimpleEntityOLGH17369_.itemInteger1));
            selectCase.when(1, true)
                            .otherwise(false);

            cquery.select(selectCase);
            cquery.orderBy(cb.asc(root.get(SimpleEntityOLGH17369_.itemInteger1)));

            query = em.createQuery(cquery);

            boolList = query.getResultList();
            Assert.assertNotNull(boolList);
            Assert.assertEquals(2, boolList.size());
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testQueryCaseParameters4(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // test 1
            TypedQuery<Boolean> query = em.createQuery(""
                                                       + "SELECT ("
                                                       + "CASE "
                                                       + "WHEN t.itemInteger1 = ?1 THEN ?2 "
                                                       + "ELSE ?3 "
                                                       + "END "
                                                       + ") "
                                                       + "FROM SimpleEntityOLGH17369 t ORDER BY t.itemInteger1 ASC", Boolean.class);
            query.setParameter(1, 1);
            query.setParameter(2, true);
            query.setParameter(3, false);

            List<Boolean> boolList = query.getResultList();
            Assert.assertNotNull(boolList);
            Assert.assertEquals(2, boolList.size());

            // test equivalent CriteriaBuilder
//            CriteriaBuilder cb = em.getCriteriaBuilder();
//            CriteriaQuery<Boolean> cquery = cb.createQuery(Boolean.class);
//            Root<SimpleEntityOLGH17369> root = cquery.from(SimpleEntityOLGH17369.class);
//
//            ParameterExpression<Integer> checkParam1 = cb.parameter(Integer.class);
//            ParameterExpression<Boolean> resultParam1 = cb.parameter(Boolean.class);
//            ParameterExpression<Boolean> resultParam2 = cb.parameter(Boolean.class);
//
//            // Currently unsupported by the JPA API
//            // https://github.com/eclipse-ee4j/jpa-api/issues/315
////            SimpleCase<Integer, Boolean> selectCase = cb.selectCase(root.get(SimpleEntityOLGH17369_.itemInteger1));
////            selectCase.when(checkParam1, resultParam1)
////                .otherwise(resultParam2);
////
////            cquery.select(selectCase);
//            cquery.orderBy(cb.asc(root.get(SimpleEntityOLGH17369_.itemInteger1)));
//
//            query = em.createQuery(cquery);
//            query.setParameter(checkParam1, 1);
//            query.setParameter(resultParam1, true);
//            query.setParameter(resultParam2, false);
//
//            boolList = query.getResultList();
//            Assert.assertNotNull(boolList);
//            Assert.assertEquals(2, boolList.size());
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
