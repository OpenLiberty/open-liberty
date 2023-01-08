/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package io.openliberty.jpa.tests.jpa31.web;

import java.util.List;

import org.junit.Test;

import com.ibm.ws.testtooling.vehicle.web.JPADBTestServlet;

import io.openliberty.jpa.tests.jpa31.models.Case2Entity;
import io.openliberty.jpa.tests.jpa31.models.Case2Entity_;
import io.openliberty.jpa.tests.jpa31.models.Case3Entity;
import io.openliberty.jpa.tests.jpa31.models.Case3Entity_;
import io.openliberty.jpa.tests.jpa31.models.CaseEntity;
import io.openliberty.jpa.tests.jpa31.models.CaseEntity_;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaBuilder.SimpleCase;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;
import junit.framework.Assert;

@WebServlet(urlPatterns = "/TestCaseConditionExpressionServlet")
public class TestCaseConditionExpressionServlet extends JPADBTestServlet {
    private static final long serialVersionUID = 1L;

    private final static String PUNAME = "CASE_CONDITION";

    @PersistenceContext(unitName = PUNAME + "_JTA")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    /**
     * JPA 3.1 added support for Expressions as conditions in Criteria CASE expressions
     *
     * https://github.com/eclipse-ee4j/jpa-api/issues/315
     */
    @Test
    public void testJPATestOLGH14457Logic_CaseExpressionReturnTypeParameter() {
        final String testName = "testJPATestOLGH14457Logic_CaseExpressionReturnTypeParameter";

        try {
            // test JPQL
            TypedQuery<Boolean> query = em.createQuery(""
                                                       + "SELECT ("
                                                       + "CASE "
                                                       + "WHEN t.itemInteger1 = ?1 THEN ?2 "
                                                       + "ELSE ?3 "
                                                       + "END "
                                                       + ") "
                                                       + "FROM CaseEntity t ORDER BY t.itemInteger1 ASC", Boolean.class);
            query.setParameter(1, 1);
            query.setParameter(2, true);
            query.setParameter(3, false);

            List<Boolean> boolList = query.getResultList();
            Assert.assertNotNull(boolList);
            Assert.assertEquals(2, boolList.size());

            // test equivalent CriteriaBuilder
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Boolean> cquery = cb.createQuery(Boolean.class);
            Root<CaseEntity> root = cquery.from(CaseEntity.class);

            ParameterExpression<Integer> checkParam1 = cb.parameter(Integer.class);
            ParameterExpression<Boolean> resultParam1 = cb.parameter(Boolean.class);
            ParameterExpression<Boolean> resultParam2 = cb.parameter(Boolean.class);

            SimpleCase<Integer, Boolean> selectCase = cb.selectCase(root.get(CaseEntity_.itemInteger1));
            selectCase.when(checkParam1, resultParam1)
                            .otherwise(resultParam2);

            cquery.select(selectCase);
            cquery.orderBy(cb.asc(root.get(CaseEntity_.itemInteger1)));

            query = em.createQuery(cquery);
            query.setParameter(checkParam1, 1);
            query.setParameter(resultParam1, true);
            query.setParameter(resultParam2, false);

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

    /**
     * JPA 3.1 added support for Expressions as conditions in Criteria CASE expressions
     *
     * https://github.com/eclipse-ee4j/jpa-api/issues/315
     */
    @Test
    public void testComparisonLogic_testCase1_Default() {
        final String testName = "testComparisonLogic_testCase1_Default";

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Integer> cquery = cb.createQuery(Integer.class);
            Root<Case2Entity> root = cquery.from(Case2Entity.class);
            cquery.multiselect(root.get(Case2Entity_.intVal1));

            ParameterExpression<Integer> checkParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> checkParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam3 = cb.parameter(Integer.class);

            Expression<Object> selectCase = cb.selectCase(root.get(Case2Entity_.intVal2))
                            .when(checkParam1, resultParam1)
                            .when(checkParam2, resultParam2)
                            .otherwise(resultParam3);

            Predicate pred = cb.equal(root.get(Case2Entity_.intVal1), selectCase);
            cquery.where(pred);

            TypedQuery<Integer> query = em.createQuery(cquery);
            query.setParameter(checkParam1, 5);
            query.setParameter(resultParam1, 6);
            query.setParameter(checkParam2, 15);
            query.setParameter(resultParam2, 16);
            query.setParameter(resultParam3, 26);
            List<Integer> intList = query.getResultList();

            Assert.assertNotNull(intList);
            Assert.assertEquals(2, intList.size());

            // Equivalent CriteriaBuilder
            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Integer> cquery3 = cb3.createQuery(Integer.class);
            Root<Case2Entity> root3 = cquery3.from(Case2Entity.class);
            cquery3.multiselect(root3.get(Case2Entity_.intVal1));

            ParameterExpression<Integer> checkParam3 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> resultParam4 = cb3.parameter(Integer.class);

            Expression<Object> selectCase3 = cb3.selectCase(root3.get(Case2Entity_.intVal2))
                            .when(5, resultParam4)
                            .when(checkParam3, 16)
                            .otherwise(26);

            Predicate pred3 = cb3.equal(root3.get(Case2Entity_.intVal1), selectCase3);
            cquery3.where(pred3);

            query = em.createQuery(cquery3);
            query.setParameter(resultParam4, 6);
            query.setParameter(checkParam3, 15);
            List<Integer> intList2 = query.getResultList();

            Assert.assertNotNull(intList2);
            Assert.assertEquals(2, intList2.size());
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * JPA 3.1 added support for Expressions as conditions in Criteria CASE expressions
     *
     * https://github.com/eclipse-ee4j/jpa-api/issues/315
     */
    @Test
    public void testComparisonLogic_testCase1_PartialBind() {
        final String testName = "testComparisonLogic_testCase1_PartialBind";

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<Case2Entity> root = cquery.from(Case2Entity.class);
            cquery.multiselect(root.get(Case2Entity_.intVal1));

            ParameterExpression<Integer> checkParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> checkParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam3 = cb.parameter(Integer.class);

            Expression<Object> selectCase = cb.selectCase(root.get(Case2Entity_.intVal2))
                            .when(checkParam1, resultParam1)
                            .when(checkParam2, resultParam2)
                            .otherwise(resultParam3);

            Predicate pred = cb.equal(root.get(Case2Entity_.intVal1), selectCase);
            cquery.where(pred);

            TypedQuery<Object[]> query = em.createQuery(cquery);
            query.setParameter(checkParam1, 5);
            query.setParameter(resultParam1, 6);
            query.setParameter(checkParam2, 15);
            query.setParameter(resultParam2, 16);
            query.setParameter(resultParam3, 26);
            List<Object[]> intList = query.getResultList();

            Assert.assertNotNull(intList);
            Assert.assertEquals(2, intList.size());

            // Equivalent CriteriaBuilder
            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<Case2Entity> root3 = cquery3.from(Case2Entity.class);
            cquery3.multiselect(root3.get(Case2Entity_.intVal1));

            ParameterExpression<Integer> checkParam3 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> resultParam4 = cb3.parameter(Integer.class);

            Expression<Object> selectCase3 = cb3.selectCase(root3.get(Case2Entity_.intVal2))
                            .when(5, resultParam4)
                            .when(checkParam3, 16)
                            .otherwise(26);

            Predicate pred3 = cb3.equal(root3.get(Case2Entity_.intVal1), selectCase3);
            cquery3.where(pred3);

            query = em.createQuery(cquery3);
            query.setParameter(resultParam4, 6);
            query.setParameter(checkParam3, 15);
            List<Object[]> intList2 = query.getResultList();

            Assert.assertNotNull(intList2);
            Assert.assertEquals(2, intList2.size());
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * JPA 3.1 added support for Expressions as conditions in Criteria CASE expressions
     *
     * https://github.com/eclipse-ee4j/jpa-api/issues/315
     */
    @Test
    public void testComparisonLogic_testCase1_PartialBind_BindLiteral() {
        final String testName = "testComparisonLogic_testCase1_PartialBind_BindLiteral";

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<Case2Entity> root = cquery.from(Case2Entity.class);
            cquery.multiselect(root.get(Case2Entity_.intVal1));

            ParameterExpression<Integer> checkParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> checkParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam3 = cb.parameter(Integer.class);

            Expression<Object> selectCase = cb.selectCase(root.get(Case2Entity_.intVal2))
                            .when(checkParam1, resultParam1)
                            .when(checkParam2, resultParam2)
                            .otherwise(resultParam3);

            Predicate pred = cb.equal(root.get(Case2Entity_.intVal1), selectCase);
            cquery.where(pred);

            TypedQuery<Object[]> query = em.createQuery(cquery);
            query.setParameter(checkParam1, 5);
            query.setParameter(resultParam1, 6);
            query.setParameter(checkParam2, 15);
            query.setParameter(resultParam2, 16);
            query.setParameter(resultParam3, 26);
            List<Object[]> intList = query.getResultList();

            Assert.assertNotNull(intList);
            Assert.assertEquals(2, intList.size());

            // Equivalent CriteriaBuilder
            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<Case2Entity> root3 = cquery3.from(Case2Entity.class);
            cquery3.multiselect(root3.get(Case2Entity_.intVal1));

            ParameterExpression<Integer> checkParam3 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> resultParam4 = cb3.parameter(Integer.class);

            Expression<Object> selectCase3 = cb3.selectCase(root3.get(Case2Entity_.intVal2))
                            .when(5, resultParam4)
                            .when(checkParam3, 16)
                            .otherwise(26);

            Predicate pred3 = cb3.equal(root3.get(Case2Entity_.intVal1), selectCase3);
            cquery3.where(pred3);

            query = em.createQuery(cquery3);
            query.setParameter(resultParam4, 6);
            query.setParameter(checkParam3, 15);
            List<Object[]> intList2 = query.getResultList();

            Assert.assertNotNull(intList2);
            Assert.assertEquals(2, intList2.size());
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * JPA 3.1 added support for Expressions as conditions in Criteria CASE expressions
     *
     * https://github.com/eclipse-ee4j/jpa-api/issues/315
     */
    @Test
    public void testComparisonLogic_testCase3_Default() {
        final String testName = "testComparisonLogic_testCase3_Default";

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<Case2Entity> root = cquery.from(Case2Entity.class);

            ParameterExpression<Integer> checkParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> checkParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam3 = cb.parameter(Integer.class);

            Expression<Object> selectCase = cb.selectCase(root.get(Case2Entity_.intVal2))
                            .when(checkParam1, resultParam1)
                            .when(checkParam2, resultParam2)
                            .otherwise(resultParam3);
            cquery.multiselect(selectCase);

            ParameterExpression<Integer> checkParam3 = cb.parameter(Integer.class);
            cquery.where(cb.equal(root.get(Case2Entity_.intVal1), checkParam3));

            TypedQuery<Object[]> query = em.createQuery(cquery);
            query.setParameter(checkParam1, 5);
            query.setParameter(resultParam1, 6);
            query.setParameter(checkParam2, 15);
            query.setParameter(resultParam2, 16);
            query.setParameter(resultParam3, 26);
            query.setParameter(checkParam3, 99);
            List<Object[]> intList = query.getResultList();

            Assert.assertNotNull(intList);
            Assert.assertEquals(2, intList.size());

            // Equivalent CriteriaBuilder
            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<Case2Entity> root3 = cquery3.from(Case2Entity.class);

            ParameterExpression<Integer> checkParam4 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> resultParam4 = cb3.parameter(Integer.class);

            Expression<Object> selectCase3 = cb3.selectCase(root3.get(Case2Entity_.intVal2))
                            .when(5, resultParam4)
                            .when(checkParam4, 16)
                            .otherwise(26);
            cquery3.multiselect(selectCase3);

            ParameterExpression<Integer> checkParam5 = cb3.parameter(Integer.class);
            cquery3.where(cb3.equal(root3.get(Case2Entity_.intVal1), checkParam5));

            query = em.createQuery(cquery3);
            query.setParameter(resultParam4, 6);
            query.setParameter(checkParam4, 15);
            query.setParameter(checkParam5, 99);
            List<Object[]> intList2 = query.getResultList();

            Assert.assertNotNull(intList2);
            Assert.assertEquals(2, intList2.size());
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * JPA 3.1 added support for Expressions as conditions in Criteria CASE expressions
     *
     * https://github.com/eclipse-ee4j/jpa-api/issues/315
     */
    @Test
    public void testComparisonLogic_testCase3_PartialBind() {
        final String testName = "testComparisonLogic_testCase3_PartialBind";

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<Case2Entity> root = cquery.from(Case2Entity.class);

            ParameterExpression<Integer> checkParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> checkParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam3 = cb.parameter(Integer.class);

            Expression<Object> selectCase = cb.selectCase(root.get(Case2Entity_.intVal2))
                            .when(checkParam1, resultParam1)
                            .when(checkParam2, resultParam2)
                            .otherwise(resultParam3);
            cquery.multiselect(selectCase);

            ParameterExpression<Integer> checkParam3 = cb.parameter(Integer.class);
            cquery.where(cb.equal(root.get(Case2Entity_.intVal1), checkParam3));

            TypedQuery<Object[]> query = em.createQuery(cquery);
            query.setParameter(checkParam1, 5);
            query.setParameter(resultParam1, 6);
            query.setParameter(checkParam2, 15);
            query.setParameter(resultParam2, 16);
            query.setParameter(resultParam3, 26);
            query.setParameter(checkParam3, 99);
            List<Object[]> intList = query.getResultList();

            Assert.assertNotNull(intList);
            Assert.assertEquals(2, intList.size());

            // Equivalent CriteriaBuilder
            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<Case2Entity> root3 = cquery3.from(Case2Entity.class);

            ParameterExpression<Integer> checkParam4 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> resultParam4 = cb3.parameter(Integer.class);

            Expression<Object> selectCase3 = cb3.selectCase(root3.get(Case2Entity_.intVal2))
                            .when(5, resultParam4)
                            .when(checkParam4, 16)
                            .otherwise(26);
            cquery3.multiselect(selectCase3);

            ParameterExpression<Integer> checkParam5 = cb3.parameter(Integer.class);
            cquery3.where(cb3.equal(root3.get(Case2Entity_.intVal1), checkParam5));

            query = em.createQuery(cquery3);
            query.setParameter(resultParam4, 6);
            query.setParameter(checkParam4, 15);
            query.setParameter(checkParam5, 99);
            List<Object[]> intList2 = query.getResultList();

            Assert.assertNotNull(intList2);
            Assert.assertEquals(2, intList2.size());
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * JPA 3.1 added support for Expressions as conditions in Criteria CASE expressions
     *
     * https://github.com/eclipse-ee4j/jpa-api/issues/315
     */
    @Test
    public void testComparisonLogic_testCase3_PartialBind_BindLiteral() {
        final String testName = "testComparisonLogic_testCase3_PartialBind_BindLiteral";

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
            Root<Case2Entity> root = cquery.from(Case2Entity.class);

            ParameterExpression<Integer> checkParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> checkParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam3 = cb.parameter(Integer.class);

            Expression<Object> selectCase = cb.selectCase(root.get(Case2Entity_.intVal2))
                            .when(checkParam1, resultParam1)
                            .when(checkParam2, resultParam2)
                            .otherwise(resultParam3);
            cquery.multiselect(selectCase);

            ParameterExpression<Integer> checkParam3 = cb.parameter(Integer.class);
            cquery.where(cb.equal(root.get(Case2Entity_.intVal1), checkParam3));

            TypedQuery<Object[]> query = em.createQuery(cquery);
            query.setParameter(checkParam1, 5);
            query.setParameter(resultParam1, 6);
            query.setParameter(checkParam2, 15);
            query.setParameter(resultParam2, 16);
            query.setParameter(resultParam3, 26);
            query.setParameter(checkParam3, 99);
            List<Object[]> intList = query.getResultList();

            Assert.assertNotNull(intList);
            Assert.assertEquals(2, intList.size());

            // Equivalent CriteriaBuilder
            CriteriaBuilder cb3 = em.getCriteriaBuilder();
            CriteriaQuery<Object[]> cquery3 = cb3.createQuery(Object[].class);
            Root<Case2Entity> root3 = cquery3.from(Case2Entity.class);

            ParameterExpression<Integer> checkParam4 = cb3.parameter(Integer.class);
            ParameterExpression<Integer> resultParam4 = cb3.parameter(Integer.class);

            Expression<Object> selectCase3 = cb3.selectCase(root3.get(Case2Entity_.intVal2))
                            .when(5, resultParam4)
                            .when(checkParam4, 16)
                            .otherwise(26);
            cquery3.multiselect(selectCase3);

            ParameterExpression<Integer> checkParam5 = cb3.parameter(Integer.class);
            cquery3.where(cb3.equal(root3.get(Case2Entity_.intVal1), checkParam5));

            query = em.createQuery(cquery3);
            query.setParameter(resultParam4, 6);
            query.setParameter(checkParam4, 15);
            query.setParameter(checkParam5, 99);
            List<Object[]> intList2 = query.getResultList();

            Assert.assertNotNull(intList2);
            Assert.assertEquals(2, intList2.size());
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * JPA 3.1 added support for Expressions as conditions in Criteria CASE expressions
     *
     * https://github.com/eclipse-ee4j/jpa-api/issues/315
     */
    @Test
    public void testJPATestOLGH17369Logic_testQueryCaseParameters1() {
        final String testName = "testJPATestOLGH17369Logic_testQueryCaseParameters1";

        try {
            TypedQuery<Case3Entity> query = em.createQuery(""
                                                           + "SELECT t FROM Case3Entity t "
                                                           + "WHERE t.itemString1 = ( "
                                                           + "CASE t.itemInteger1 "
                                                           + "WHEN ?1 THEN ?2 "
                                                           + "WHEN ?3 THEN ?4 "
                                                           + "ELSE ?5 "
                                                           + "END )", Case3Entity.class);
            query.setParameter(1, 1000);
            query.setParameter(2, "047010");
            query.setParameter(3, 100);
            query.setParameter(4, "023010");
            query.setParameter(5, "033020");

            List<Case3Entity> dto01 = query.getResultList();
            Assert.assertNotNull(dto01);
            Assert.assertEquals(0, dto01.size());

            // test 2
            query = em.createQuery(""
                                   + "SELECT t FROM Case3Entity t "
                                   + "WHERE t.itemString1 = ( "
                                   + "CASE t.itemInteger1 "
                                   + "WHEN ?1 THEN ?2 "
                                   + "WHEN ?3 THEN ?4 "
                                   + "ELSE ?5 "
                                   + "END )", Case3Entity.class);
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
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Case3Entity> cquery = cb.createQuery(Case3Entity.class);
            Root<Case3Entity> root = cquery.from(Case3Entity.class);
            cquery.multiselect(root);

            ParameterExpression<Integer> checkParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> checkParam2 = cb.parameter(Integer.class);
            ParameterExpression<String> resultParam1 = cb.parameter(String.class);
            ParameterExpression<String> resultParam2 = cb.parameter(String.class);
            ParameterExpression<String> resultParam3 = cb.parameter(String.class);

            Expression<Object> selectCase = cb.selectCase(root.get(Case3Entity_.itemInteger1))
                            .when(checkParam1, resultParam1)
                            .when(checkParam2, resultParam2)
                            .otherwise(resultParam3);
            Predicate pred = cb.equal(root.get(Case3Entity_.itemString1), selectCase);
            cquery.where(pred);

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

    /**
     * JPA 3.1 added support for Expressions as conditions in Criteria CASE expressions
     *
     * https://github.com/eclipse-ee4j/jpa-api/issues/315
     */
    @Test
    public void testJPATestOLGH17369Logic_testQueryCaseParameters3() {
        final String testName = "testJPATestOLGH17369Logic_testQueryCaseParameters3";

        try {
            TypedQuery<Number> query = em.createQuery(""
                                                      + "SELECT ("
                                                      + "CASE t.itemString2 "
                                                      + "WHEN ?1 THEN ?2 "
                                                      + "WHEN ?3 THEN ?4 "
                                                      + "ELSE ?5 "
                                                      + "END "
                                                      + ") "
                                                      + "FROM Case3Entity t", Number.class);
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
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Number> cquery = cb.createQuery(Number.class);
            Root<Case3Entity> root = cquery.from(Case3Entity.class);

            ParameterExpression<String> checkParam1 = cb.parameter(String.class);
            ParameterExpression<String> checkParam2 = cb.parameter(String.class);
            ParameterExpression<Integer> resultParam1 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam2 = cb.parameter(Integer.class);
            ParameterExpression<Integer> resultParam3 = cb.parameter(Integer.class);

            SimpleCase<String, Integer> selectCase = cb.selectCase(root.get(Case3Entity_.itemString2));
            selectCase.when(checkParam1, resultParam1)
                            .when(checkParam2, resultParam2)
                            .otherwise(resultParam3);

            cquery.select(selectCase);

            query = em.createQuery(cquery);
            query.setParameter(checkParam1, "A");
            query.setParameter(resultParam1, 42);
            query.setParameter(checkParam2, "B");
            query.setParameter(resultParam2, 100);
            query.setParameter(resultParam3, 0);

            intList = query.getResultList();
            Assert.assertNotNull(intList);
            Assert.assertEquals(2, intList.size());
            Assert.assertEquals(new Integer(100).intValue(), intList.get(0).intValue());
            Assert.assertEquals(new Integer(100).intValue(), intList.get(1).intValue());
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * JPA 3.1 added support for Expressions as conditions in Criteria CASE expressions
     *
     * https://github.com/eclipse-ee4j/jpa-api/issues/315
     */
    @Test
    public void testJPATestOLGH17369Logic_testQueryCaseParameters4() {
        final String testName = "testJPATestOLGH17369Logic_testQueryCaseParameters4";

        try {
            TypedQuery<Boolean> query = em.createQuery(""
                                                       + "SELECT ("
                                                       + "CASE "
                                                       + "WHEN t.itemInteger1 = ?1 THEN ?2 "
                                                       + "ELSE ?3 "
                                                       + "END "
                                                       + ") "
                                                       + "FROM Case3Entity t ORDER BY t.itemInteger1 ASC", Boolean.class);
            query.setParameter(1, 1);
            query.setParameter(2, true);
            query.setParameter(3, false);

            List<Boolean> boolList = query.getResultList();
            Assert.assertNotNull(boolList);
            Assert.assertEquals(2, boolList.size());

            // test equivalent CriteriaBuilder
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Boolean> cquery = cb.createQuery(Boolean.class);
            Root<Case3Entity> root = cquery.from(Case3Entity.class);

            ParameterExpression<Integer> checkParam1 = cb.parameter(Integer.class);
            ParameterExpression<Boolean> resultParam1 = cb.parameter(Boolean.class);
            ParameterExpression<Boolean> resultParam2 = cb.parameter(Boolean.class);

            SimpleCase<Integer, Boolean> selectCase = cb.selectCase(root.get(Case3Entity_.itemInteger1));
            selectCase.when(checkParam1, resultParam1)
                            .otherwise(resultParam2);

            cquery.select(selectCase);
            cquery.orderBy(cb.asc(root.get(Case3Entity_.itemInteger1)));

            query = em.createQuery(cquery);
            query.setParameter(checkParam1, 1);
            query.setParameter(resultParam1, true);
            query.setParameter(resultParam2, false);

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
}
