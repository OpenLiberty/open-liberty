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

import org.junit.Test;

import com.ibm.ws.testtooling.vehicle.web.JPADBTestServlet;

import io.openliberty.jpa.tests.jpa31.models.QueryEntity;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.servlet.annotation.WebServlet;
import jakarta.transaction.UserTransaction;
import junit.framework.Assert;

@WebServlet(urlPatterns = "/TestNewQueryMathFunctionsServlet")
public class TestNewQueryMathFunctionsServlet extends JPADBTestServlet {
    private static final long serialVersionUID = 3866874746696149833L;

    private final static String PUNAME = "QueryFeatures";

    @PersistenceUnit(unitName = PUNAME + "_JTA")
    private EntityManagerFactory emfJta;

    @PersistenceUnit(unitName = PUNAME + "_RL")
    private EntityManagerFactory emfRl;

    @PersistenceContext(unitName = PUNAME + "_JTA")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @PostConstruct
    private void initializeFAT() {
        // Populate the database

        try (EntityManager iem = emfRl.createEntityManager()) {
            try {
                iem.getTransaction().begin();
                iem.createNativeQuery("DELETE FROM QUERYENTITY").executeUpdate();
            } finally {
                iem.getTransaction().commit();
            }

            try {
                iem.getTransaction().begin();
                QueryEntity qe1 = new QueryEntity(1, 42, 42, 42.1f, 42.1d, "42");
                QueryEntity qe2 = new QueryEntity(2, -42, -42, -42.1f, -42.1d, "-42");
                QueryEntity qe3 = new QueryEntity(3, 0, 0, 0.0f, 0.0d, "0");
                QueryEntity qe4 = new QueryEntity(4, 1, 1, 1.0f, 1.0d, "1");
                iem.persist(qe1);
                iem.persist(qe2);
                iem.persist(qe3);
                iem.persist(qe4);
            } finally {
                iem.getTransaction().commit();
            }
        }

    }

    /*
     * The ABS, CEILING, and FLOOR functions accept a numeric argument and return a number (integer, float, or double) of the same type as the argument.
     */

    @Test
    public void testCEILINGFunction_JPQL() {
        em.clear();

        Query q = null;

        Double dResult = 0.0d;
        Float fResult = 0.0f;

        q = em.createQuery("SELECT CEILING(a.floatVal) FROM QueryEntity a WHERE a.id = 1");
        fResult = (Float) q.getSingleResult();
        Assert.assertEquals(43.0f, fResult);

        q = em.createQuery("SELECT CEILING(a.doubleVal) FROM QueryEntity a WHERE a.id = 1");
        dResult = (Double) q.getSingleResult();
        Assert.assertEquals(43.0d, dResult);

        q = em.createQuery("SELECT CEILING(a.floatVal) FROM QueryEntity a WHERE a.id = 2");
        fResult = (Float) q.getSingleResult();
        Assert.assertEquals(-42.0f, fResult, 0.01);

        q = em.createQuery("SELECT CEILING(a.doubleVal) FROM QueryEntity a WHERE a.id = 2");
        dResult = (Double) q.getSingleResult();
        Assert.assertEquals(-42.0d, dResult, 0.01);

        q = em.createQuery("SELECT CEILING(a.floatVal) FROM QueryEntity a WHERE a.id = 3");
        fResult = (Float) q.getSingleResult();
        Assert.assertEquals(0.0f, fResult, 0.01);

        q = em.createQuery("SELECT CEILING(a.doubleVal) FROM QueryEntity a WHERE a.id = 3");
        dResult = (Double) q.getSingleResult();
        Assert.assertEquals(0.0d, dResult, 0.01);

    }

    @Test
    public void testFunction_CEILING_Criteria() {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        ExecCriteriaQuery ecq = (int id, String fieldName, Class fieldType) -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery cq = cb.createQuery(fieldType);
            Root<QueryEntity> root = cq.from(QueryEntity.class);
            cq.select(cb.ceiling(root.get(fieldName)));
            cq.where(cb.equal(root.get("id"), id));
            return em.createQuery(cq).getSingleResult();
        };

        em.clear();

        Double dResult = 0.0d;
        Float fResult = 0.0f;

        fResult = (Float) ecq.execCriteriaQuery(1, "floatVal", Float.class);
        Assert.assertEquals(43.0f, fResult, 0.01);

        dResult = (Double) ecq.execCriteriaQuery(1, "doubleVal", Double.class);
        Assert.assertEquals(43.0d, dResult, 0.01);

        fResult = (Float) ecq.execCriteriaQuery(2, "floatVal", Float.class);
        Assert.assertEquals(-42.0f, fResult, 0.01);

        dResult = (Double) ecq.execCriteriaQuery(2, "doubleVal", Double.class);
        Assert.assertEquals(-42.0d, dResult, 0.01);

        fResult = (Float) ecq.execCriteriaQuery(3, "floatVal", Float.class);
        Assert.assertEquals(0.0f, fResult, 0.01);

        dResult = (Double) ecq.execCriteriaQuery(3, "doubleVal", Double.class);
        Assert.assertEquals(0.0d, dResult, 0.01);

    }

    @Test
    public void testFLOORFunction_JPQL() {
        em.clear();

        Query q = null;

        Double dResult = 0.0d;
        Float fResult = 0.0f;

        q = em.createQuery("SELECT FLOOR(a.floatVal) FROM QueryEntity a WHERE a.id = 1");
        fResult = (Float) q.getSingleResult();
        Assert.assertEquals(42.0f, fResult);

        q = em.createQuery("SELECT FLOOR(a.doubleVal) FROM QueryEntity a WHERE a.id = 1");
        dResult = (double) q.getSingleResult();
        Assert.assertEquals(42.0d, dResult);

        q = em.createQuery("SELECT FLOOR(a.floatVal) FROM QueryEntity a WHERE a.id = 2");
        fResult = (Float) q.getSingleResult();
        Assert.assertEquals(-43.0f, fResult, 0.01);

        q = em.createQuery("SELECT FLOOR(a.doubleVal) FROM QueryEntity a WHERE a.id = 2");
        dResult = (double) q.getSingleResult();
        Assert.assertEquals(-43.0d, dResult, 0.01);

        q = em.createQuery("SELECT FLOOR(a.floatVal) FROM QueryEntity a WHERE a.id = 3");
        fResult = (Float) q.getSingleResult();
        Assert.assertEquals(0.0f, fResult, 0.01);

        q = em.createQuery("SELECT FLOOR(a.doubleVal) FROM QueryEntity a WHERE a.id = 3");
        dResult = (double) q.getSingleResult();
        Assert.assertEquals(0.0d, dResult, 0.01);
    }

    @Test
    public void testFunction_FLOOR_Criteria() {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        ExecCriteriaQuery ecq = (int id, String fieldName, Class fieldType) -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery cq = cb.createQuery(fieldType);
            Root<QueryEntity> root = cq.from(QueryEntity.class);
            cq.select(cb.floor(root.get(fieldName)));
            cq.where(cb.equal(root.get("id"), id));
            return em.createQuery(cq).getSingleResult();
        };

        em.clear();

        Double dResult = 0.0d;
        Float fResult = 0.0f;

        fResult = (Float) ecq.execCriteriaQuery(1, "floatVal", Float.class);
        Assert.assertEquals(42.0f, fResult, 0.01);

        dResult = (Double) ecq.execCriteriaQuery(1, "doubleVal", Double.class);
        Assert.assertEquals(42.0d, dResult, 0.01);

        fResult = (Float) ecq.execCriteriaQuery(2, "floatVal", Float.class);
        Assert.assertEquals(-43.0f, fResult, 0.01);

        dResult = (Double) ecq.execCriteriaQuery(2, "doubleVal", Double.class);
        Assert.assertEquals(-43.0d, dResult, 0.01);

        fResult = (Float) ecq.execCriteriaQuery(3, "floatVal", Float.class);
        Assert.assertEquals(0.0f, fResult, 0.01);

        dResult = (Double) ecq.execCriteriaQuery(3, "doubleVal", Double.class);
        Assert.assertEquals(0.0d, dResult, 0.01);

    }

    // The SQRT, EXP, and LN functions accept a numeric argument and return a double.

    @Test
    public void testEXPFunction_JPQL() {
        em.clear();

        Query q = null;

        double dResult = 0.0d;

        q = em.createQuery("SELECT EXP(a.intVal) FROM QueryEntity a WHERE a.id = 4");
        dResult = (double) q.getSingleResult();
        Assert.assertEquals(2.718281828459045d, dResult, 0.01);

        q = em.createQuery("SELECT EXP(a.longVal) FROM QueryEntity a WHERE a.id = 4");
        dResult = (double) q.getSingleResult();
        Assert.assertEquals(2.718281828459045d, dResult, 0.01);

        q = em.createQuery("SELECT EXP(a.floatVal) FROM QueryEntity a WHERE a.id = 4");
        dResult = (double) q.getSingleResult();
        Assert.assertEquals(2.718281828459045d, dResult, 0.01);

        q = em.createQuery("SELECT EXP(a.doubleVal) FROM QueryEntity a WHERE a.id = 4");
        dResult = (double) q.getSingleResult();
        Assert.assertEquals(2.718281828459045d, dResult, 0.01);

        q = em.createQuery("SELECT EXP(a.intVal) FROM QueryEntity a WHERE a.id = 3");
        dResult = (double) q.getSingleResult();
        Assert.assertEquals(1.0d, dResult, 0.01);

        q = em.createQuery("SELECT EXP(a.longVal) FROM QueryEntity a WHERE a.id = 3");
        dResult = (double) q.getSingleResult();
        Assert.assertEquals(1.0d, dResult, 0.01);

        q = em.createQuery("SELECT EXP(a.floatVal) FROM QueryEntity a WHERE a.id = 3");
        dResult = (double) q.getSingleResult();
        Assert.assertEquals(1.0d, dResult, 0.01);

        q = em.createQuery("SELECT EXP(a.doubleVal) FROM QueryEntity a WHERE a.id = 3");
        dResult = (double) q.getSingleResult();
        Assert.assertEquals(1.0d, dResult, 0.01);
    }

    @Test
    public void testFunction_EXP_Criteria() {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        ExecCriteriaQuery ecq = (int id, String fieldName, Class fieldType) -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery cq = cb.createQuery(fieldType);
            Root<QueryEntity> root = cq.from(QueryEntity.class);
            cq.select(cb.exp(root.get(fieldName)));
            cq.where(cb.equal(root.get("id"), id));
            return em.createQuery(cq).getSingleResult();
        };

        em.clear();

        Double dResult = 0.0d;

        dResult = (Double) ecq.execCriteriaQuery(4, "intVal", Double.class);
        Assert.assertEquals(2.718281828459045d, dResult, 0.01);

        dResult = (Double) ecq.execCriteriaQuery(4, "longVal", Double.class);
        Assert.assertEquals(2.718281828459045d, dResult, 0.01);

        dResult = (Double) ecq.execCriteriaQuery(4, "floatVal", Double.class);
        Assert.assertEquals(2.718281828459045d, dResult, 0.01);

        dResult = (Double) ecq.execCriteriaQuery(4, "doubleVal", Double.class);
        Assert.assertEquals(2.718281828459045d, dResult, 0.01);

        dResult = (Double) ecq.execCriteriaQuery(3, "intVal", Double.class);
        Assert.assertEquals(1.0d, dResult, 0.01);

        dResult = (Double) ecq.execCriteriaQuery(3, "floatVal", Double.class);
        Assert.assertEquals(1.0d, dResult, 0.01);

        dResult = (Double) ecq.execCriteriaQuery(3, "doubleVal", Double.class);
        Assert.assertEquals(1.0d, dResult, 0.01);

    }

    @Test
    public void testLNFunction_JPQL() {
        em.clear();

        Query q = null;

        double dResult = 0.0d;

        q = em.createQuery("SELECT LN(a.intVal) FROM QueryEntity a WHERE a.id = 4");
        dResult = (double) q.getSingleResult();
        Assert.assertEquals(0.0d, dResult, 0.01);

        q = em.createQuery("SELECT LN(a.longVal) FROM QueryEntity a WHERE a.id = 4");
        dResult = (double) q.getSingleResult();
        Assert.assertEquals(0.0d, dResult, 0.01);

        q = em.createQuery("SELECT LN(a.floatVal) FROM QueryEntity a WHERE a.id = 4");
        dResult = (double) q.getSingleResult();
        Assert.assertEquals(0.0d, dResult, 0.01);

        q = em.createQuery("SELECT LN(a.doubleVal) FROM QueryEntity a WHERE a.id = 4");
        dResult = (double) q.getSingleResult();
        Assert.assertEquals(0.0d, dResult, 0.01);
    }

    @Test
    public void testFunction_LN_Criteria() {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        ExecCriteriaQuery ecq = (int id, String fieldName, Class fieldType) -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery cq = cb.createQuery(fieldType);
            Root<QueryEntity> root = cq.from(QueryEntity.class);
            cq.select(cb.ln(root.get(fieldName)));
            cq.where(cb.equal(root.get("id"), id));
            return em.createQuery(cq).getSingleResult();
        };

        em.clear();

        Double dResult = 0.0d;

        dResult = (Double) ecq.execCriteriaQuery(4, "intVal", Double.class);
        Assert.assertEquals(0.0d, dResult, 0.01);

        dResult = (Double) ecq.execCriteriaQuery(4, "longVal", Double.class);
        Assert.assertEquals(0.0d, dResult, 0.01);

        dResult = (Double) ecq.execCriteriaQuery(4, "floatVal", Double.class);
        Assert.assertEquals(0.0d, dResult, 0.01);

        dResult = (Double) ecq.execCriteriaQuery(4, "doubleVal", Double.class);
        Assert.assertEquals(0.0d, dResult, 0.01);

    }

    // The POWER function accepts two numeric arguments and returns a double.

    @Test
    public void testPowerFunction_JPQL() {
        em.clear();

        Query q = null;

        double dResult = 0.0d;

        q = em.createQuery("SELECT POWER(a.intVal, 2.0) FROM QueryEntity a WHERE a.id = 1");
        dResult = (double) q.getSingleResult();
        Assert.assertEquals(1764.0d, dResult, 0.01);

        q = em.createQuery("SELECT POWER(a.longVal, 2.0) FROM QueryEntity a WHERE a.id = 1");
        dResult = (double) q.getSingleResult();
        Assert.assertEquals(1764.0d, dResult, 0.01);

        q = em.createQuery("SELECT POWER(a.floatVal, 2.0) FROM QueryEntity a WHERE a.id = 1");
        dResult = (double) q.getSingleResult();
        Assert.assertEquals(1772.41d, dResult, 0.01);

        q = em.createQuery("SELECT POWER(a.doubleVal, 2.0) FROM QueryEntity a WHERE a.id = 1");
        dResult = (double) q.getSingleResult();
        Assert.assertEquals(1772.41d, dResult, 0.01);
    }

    @Test
    public void testFunction_Power_Criteria() {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        ExecCriteriaQueryWithDoubleSecondArg ecq = (int id, String fieldName, Class fieldType, double arg2) -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery cq = cb.createQuery(fieldType);
            Root<QueryEntity> root = cq.from(QueryEntity.class);
            cq.select(cb.power(root.get(fieldName), arg2));
            cq.where(cb.equal(root.get("id"), id));
            return em.createQuery(cq).getSingleResult();
        };

        em.clear();
        Double dResult = 0.0d;

        dResult = (Double) ecq.execCriteriaQuery(1, "intVal", Float.class, 2.0d);
        Assert.assertEquals(1764.0d, dResult, 0.01);

        dResult = (Double) ecq.execCriteriaQuery(1, "longVal", Float.class, 2.0d);
        Assert.assertEquals(1764.0d, dResult, 0.01);

        dResult = (Double) ecq.execCriteriaQuery(1, "floatVal", Float.class, 2.0d);
        Assert.assertEquals(1772.41d, dResult, 0.01);

        dResult = (Double) ecq.execCriteriaQuery(1, "doubleVal", Double.class, 2.0d);
        Assert.assertEquals(1772.41d, dResult, 0.01);

    }

    // The ROUND function accepts a numeric argument and an integer argument and returns a number of the same type as the first argument.

    @Test
    public void testRoundFunction_JPQL() {
        em.clear();

        Query q = null;

        Double dResult = 0.0d;
        Float fResult = 0.0f;

        q = em.createQuery("SELECT ROUND(a.floatVal, 0) FROM QueryEntity a WHERE a.id = 1");
        fResult = (Float) q.getSingleResult();
        Assert.assertEquals(42.0f, fResult, 0.01);

        q = em.createQuery("SELECT ROUND(a.doubleVal, 0) FROM QueryEntity a WHERE a.id = 1");
        dResult = (Double) q.getSingleResult();
        Assert.assertEquals(42.0d, dResult, 0.01);

        q = em.createQuery("SELECT ROUND(a.floatVal, 0) FROM QueryEntity a WHERE a.id = 2");
        fResult = (Float) q.getSingleResult();
        Assert.assertEquals(-42.0f, fResult, 0.01);

        q = em.createQuery("SELECT ROUND(a.doubleVal, 0) FROM QueryEntity a WHERE a.id = 2");
        dResult = (Double) q.getSingleResult();
        Assert.assertEquals(-42.0d, dResult, 0.01);
    }

    @Test
    public void testFunction_Round_Criteria() {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        ExecCriteriaQueryWithIntegerSecondArg ecq = (int id, String fieldName, Class fieldType, int arg2) -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery cq = cb.createQuery(fieldType);
            Root<QueryEntity> root = cq.from(QueryEntity.class);
            cq.select(cb.round(root.get(fieldName), arg2));
            cq.where(cb.equal(root.get("id"), id));
            return em.createQuery(cq).getSingleResult();
        };

        em.clear();
        Double dResult = 0.0d;
        Float fResult = 0.0f;

        fResult = (Float) ecq.execCriteriaQuery(1, "floatVal", Float.class, 0);
        Assert.assertEquals(42.0f, fResult, 0.01);

        dResult = (Double) ecq.execCriteriaQuery(1, "doubleVal", Double.class, 0);
        Assert.assertEquals(42.0d, dResult, 0.01);

        fResult = (Float) ecq.execCriteriaQuery(2, "floatVal", Float.class, 0);
        Assert.assertEquals(-42.0f, fResult, 0.01);

        dResult = (Double) ecq.execCriteriaQuery(2, "doubleVal", Double.class, 0);
        Assert.assertEquals(-42.0d, dResult, 0.01);

    }

    // The SIGN function accepts a numeric argument and returns an integer.

    @Test
    public void testSignFunction_JPQL() {
        em.clear();

        Query q = null;

        int iResult = 0;
        q = em.createQuery("SELECT SIGN(a.floatVal) FROM QueryEntity a WHERE a.id = 1");
        iResult = (int) q.getSingleResult();
        Assert.assertEquals(1, iResult);

        q = em.createQuery("SELECT SIGN(a.doubleVal) FROM QueryEntity a WHERE a.id = 1");
        iResult = (int) q.getSingleResult();
        Assert.assertEquals(1, iResult);

        q = em.createQuery("SELECT SIGN(a.floatVal) FROM QueryEntity a WHERE a.id = 2");
        iResult = (int) q.getSingleResult();
        Assert.assertEquals(-1, iResult);

        q = em.createQuery("SELECT SIGN(a.doubleVal) FROM QueryEntity a WHERE a.id = 2");
        iResult = (int) q.getSingleResult();
        Assert.assertEquals(-1, iResult);

        q = em.createQuery("SELECT SIGN(a.floatVal) FROM QueryEntity a WHERE a.id = 3");
        iResult = (int) q.getSingleResult();
        Assert.assertEquals(0, iResult);

        q = em.createQuery("SELECT SIGN(a.doubleVal) FROM QueryEntity a WHERE a.id = 3");
        iResult = (int) q.getSingleResult();
        Assert.assertEquals(0, iResult);
    }

    @Test
    public void testFunction_Sign_Criteria() {
        @SuppressWarnings({ "rawtypes", "unchecked" })
        ExecCriteriaQuery ecq = (int id, String fieldName, Class fieldType) -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery cq = cb.createQuery(fieldType);
            Root<QueryEntity> root = cq.from(QueryEntity.class);
            cq.select(cb.sign(root.get(fieldName)));
            cq.where(cb.equal(root.get("id"), id));
            return em.createQuery(cq).getSingleResult();
        };

        em.clear();
        Integer iResult = 0;

        iResult = (Integer) ecq.execCriteriaQuery(1, "floatVal", Float.class);
        Assert.assertEquals(Integer.valueOf(1), iResult);

        iResult = (Integer) ecq.execCriteriaQuery(1, "doubleVal", Double.class);
        Assert.assertEquals(Integer.valueOf(1), iResult);

        iResult = (Integer) ecq.execCriteriaQuery(2, "floatVal", Float.class);
        Assert.assertEquals(Integer.valueOf(-1), iResult);

        iResult = (Integer) ecq.execCriteriaQuery(2, "doubleVal", Double.class);
        Assert.assertEquals(Integer.valueOf(-1), iResult);

        iResult = (Integer) ecq.execCriteriaQuery(3, "floatVal", Float.class);
        Assert.assertEquals(Integer.valueOf(0), iResult);

        iResult = (Integer) ecq.execCriteriaQuery(3, "doubleVal", Double.class);
        Assert.assertEquals(Integer.valueOf(0), iResult);

    }

    private interface ExecCriteriaQuery {
        Object execCriteriaQuery(int id, String fieldName, Class fieldType);
    }

    private interface ExecCriteriaQueryWithDoubleSecondArg {
        Object execCriteriaQuery(int id, String fieldName, Class fieldType, double arg2);
    }

    private interface ExecCriteriaQueryWithIntegerSecondArg {
        Object execCriteriaQuery(int id, String fieldName, Class fieldType, int arg2);
    }
}
