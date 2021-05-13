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

package com.ibm.ws.jpa.olgh16772.testlogic;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaBuilder.Trimspec;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.eclipse.persistence.config.HintValues;
import org.eclipse.persistence.config.QueryHints;
import org.junit.Assert;

import com.ibm.ws.jpa.olgh16772.model.TrimEntityOLGH16772;
import com.ibm.ws.jpa.olgh16772.model.TrimEntityOLGH16772_;
import com.ibm.ws.jpa.query.sqlcapture.SQLListener;
import com.ibm.ws.testtooling.database.DatabaseVendor;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH16772Logic extends AbstractTestLogic {

    public void testWhereTrim(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                              Object managedComponentObject) throws Throwable {
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

        //TODO: Disable test until EclipseLink 3.0/2.7 are updated to include the fix
        if ((isUsingJPA30Feature() || isUsingJPA22Feature()) && JPAProviderImpl.ECLIPSELINK.equals(getJPAProviderImpl(jpaResource))) {
            return;
        }

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));

        final boolean isOracle = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.ORACLE);
        final boolean isSQLServer = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.SQLSERVER);
        final boolean isSybase = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.SYBASE);
        final boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DERBY);
        final boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2);

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            try {
                SQLListener.getAndClear();

                System.out.println("Creating JPQL query");
                Query jpqlQuery = em.createQuery("SELECT e.strVal1 FROM TrimEntityOLGH16772 e WHERE (e.strVal1 = TRIM('A' FROM 'AAHELLO WORDAAAAA'))");
                // Set parameter binding off so that we can validate the arguments
                jpqlQuery.setHint(QueryHints.BIND_PARAMETERS, HintValues.FALSE);

                System.out.println("Executing JPQL query");
                List<?> resultList = jpqlQuery.getResultList();
                Assert.assertNotNull(resultList);

                List<String> sql = SQLListener.getAndClear();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());

                if (isSybase) {
                    String expected = "SELECT STRVAL1 FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = STR_REPLACE('A', 'AAHELLO WORDAAAAA', NULL)))";
                    Assert.assertEquals(expected, sql.get(0));
                } else if (isSQLServer) {
                    String expected = "SELECT STRVAL1 FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = RTRIM('A' FROM LTRIM('A' FROM 'AAHELLO WORDAAAAA')))";
                    Assert.assertEquals(expected, sql.get(0));
                } else {
                    String expected = "SELECT STRVAL1 FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = TRIM('A' FROM 'AAHELLO WORDAAAAA'))";
                    Assert.assertEquals(expected, sql.get(0));
                }

                System.out.println("Creating CriteriaBuilder query");
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
                Root<TrimEntityOLGH16772> root = cquery.from(TrimEntityOLGH16772.class);
                cquery.multiselect(root.get(TrimEntityOLGH16772_.strVal1));
                cquery.where(cb.equal(root.get(TrimEntityOLGH16772_.strVal1), cb.trim(cb.literal('A'), cb.literal("AAHELLO WORDAAAAA"))));

                Query query = em.createQuery(cquery);
                // Set parameter binding off so that we can validate the arguments
                query.setHint(QueryHints.BIND_PARAMETERS, HintValues.FALSE);

                System.out.println("Executing CriteriaBuilder query");
                resultList = query.getResultList();
                Assert.assertNotNull(resultList);

                sql = SQLListener.getAndClear();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());

                if (isSybase) {
                    String expected = "SELECT STRVAL1 FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = STR_REPLACE('A', 'AAHELLO WORDAAAAA', NULL)))";
                    Assert.assertEquals(expected, sql.get(0));
                } else if (isSQLServer) {
                    String expected = "SELECT STRVAL1 FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = RTRIM('A' FROM LTRIM('A' FROM 'AAHELLO WORDAAAAA')))";
                    Assert.assertEquals(expected, sql.get(0));
                } else {
                    String expected = "SELECT STRVAL1 FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = TRIM('A' FROM 'AAHELLO WORDAAAAA'))";
                    Assert.assertEquals(expected, sql.get(0));
                }
            } finally {
                if (tj.isTransactionActive()) {
                    tj.rollbackTransaction();
                }
            }
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testWhereLeftTrim(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) throws Throwable {
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

        //TODO: Disable test until EclipseLink 3.0/2.7 are updated to include the fix
        if ((isUsingJPA30Feature() || isUsingJPA22Feature()) && JPAProviderImpl.ECLIPSELINK.equals(getJPAProviderImpl(jpaResource))) {
            return;
        }

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));

        final boolean isMySQL = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.MYSQL);
        final boolean isOracle = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.ORACLE);
        final boolean isSQLServer = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.SQLSERVER);
        final boolean isSybase = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.SYBASE);
        final boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DERBY);
        final boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2);

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            try {
                SQLListener.getAndClear();

                System.out.println("Creating JPQL query");
                Query jpqlQuery = em.createQuery("SELECT e.strVal1 FROM TrimEntityOLGH16772 e WHERE (e.strVal1 = TRIM(LEADING 'A' FROM 'AAHELLO WORDAAAAA'))");
                // Set parameter binding off so that we can validate the arguments
                jpqlQuery.setHint(QueryHints.BIND_PARAMETERS, HintValues.FALSE);

                System.out.println("Executing JPQL query");
                List<?> resultList = jpqlQuery.getResultList();
                Assert.assertNotNull(resultList);

                List<String> sql = SQLListener.getAndClear();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());

                if (isMySQL || isDB2 || isDerby) {
                    String expected = "SELECT STRVAL1 FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAHELLO WORDAAAAA'))";
                    Assert.assertEquals(expected, sql.get(0));
                } else {
                    String expected = "SELECT STRVAL1 FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = LTRIM('A', 'AAHELLO WORDAAAAA'))";
                    Assert.assertEquals(expected, sql.get(0));
                }

                System.out.println("Creating CriteriaBuilder query");
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
                Root<TrimEntityOLGH16772> root = cquery.from(TrimEntityOLGH16772.class);
                cquery.multiselect(root.get(TrimEntityOLGH16772_.strVal1));
                cquery.where(cb.equal(root.get(TrimEntityOLGH16772_.strVal1), cb.trim(Trimspec.LEADING, cb.literal('A'), cb.literal("AAHELLO WORDAAAAA"))));

                Query query = em.createQuery(cquery);
                // Set parameter binding off so that we can validate the arguments
                query.setHint(QueryHints.BIND_PARAMETERS, HintValues.FALSE);

                System.out.println("Executing CriteriaBuilder query");
                resultList = query.getResultList();
                Assert.assertNotNull(resultList);

                sql = SQLListener.getAndClear();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());

                if (isMySQL || isDB2 || isDerby) {
                    String expected = "SELECT STRVAL1 FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = TRIM(LEADING 'A' FROM 'AAHELLO WORDAAAAA'))";
                    Assert.assertEquals(expected, sql.get(0));
                } else {
                    String expected = "SELECT STRVAL1 FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = LTRIM('A', 'AAHELLO WORDAAAAA'))";
                    Assert.assertEquals(expected, sql.get(0));
                }
            } finally {
                if (tj.isTransactionActive()) {
                    tj.rollbackTransaction();
                }
            }
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testWhereRightTrim(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
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

        //TODO: Disable test until EclipseLink 3.0/2.7 are updated to include the fix
        if ((isUsingJPA30Feature() || isUsingJPA22Feature()) && JPAProviderImpl.ECLIPSELINK.equals(getJPAProviderImpl(jpaResource))) {
            return;
        }

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));

        final boolean isMySQL = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.MYSQL);
        final boolean isOracle = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.ORACLE);
        final boolean isSQLServer = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.SQLSERVER);
        final boolean isSybase = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.SYBASE);
        final boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DERBY);
        final boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2);

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            try {
                SQLListener.getAndClear();

                System.out.println("Creating JPQL query");
                Query jpqlQuery = em.createQuery("SELECT e.strVal1 FROM TrimEntityOLGH16772 e WHERE (e.strVal1 = TRIM(TRAILING 'A' FROM 'AAHELLO WORDAAAAA'))");
                // Set parameter binding off so that we can validate the arguments
                jpqlQuery.setHint(QueryHints.BIND_PARAMETERS, HintValues.FALSE);

                System.out.println("Executing JPQL query");
                List<?> resultList = jpqlQuery.getResultList();
                Assert.assertNotNull(resultList);

                List<String> sql = SQLListener.getAndClear();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());

                if (isMySQL || isDB2 || isDerby) {
                    String expected = "SELECT STRVAL1 FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM 'AAHELLO WORDAAAAA'))";
                    Assert.assertEquals(expected, sql.get(0));
                } else {
                    String expected = "SELECT STRVAL1 FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = RTRIM('A', 'AAHELLO WORDAAAAA'))";
                    Assert.assertEquals(expected, sql.get(0));
                }

                System.out.println("Creating CriteriaBuilder query");
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
                Root<TrimEntityOLGH16772> root = cquery.from(TrimEntityOLGH16772.class);
                cquery.multiselect(root.get(TrimEntityOLGH16772_.strVal1));
                cquery.where(cb.equal(root.get(TrimEntityOLGH16772_.strVal1), cb.trim(Trimspec.TRAILING, cb.literal('A'), cb.literal("AAHELLO WORDAAAAA"))));

                Query query = em.createQuery(cquery);
                // Set parameter binding off so that we can validate the arguments
                query.setHint(QueryHints.BIND_PARAMETERS, HintValues.FALSE);

                System.out.println("Executing CriteriaBuilder query");
                resultList = query.getResultList();
                Assert.assertNotNull(resultList);

                sql = SQLListener.getAndClear();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());

                if (isMySQL || isDB2 || isDerby) {
                    String expected = "SELECT STRVAL1 FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = TRIM(TRAILING 'A' FROM 'AAHELLO WORDAAAAA'))";
                    Assert.assertEquals(expected, sql.get(0));
                } else {
                    String expected = "SELECT STRVAL1 FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = RTRIM('A', 'AAHELLO WORDAAAAA'))";
                    Assert.assertEquals(expected, sql.get(0));
                }
            } finally {
                if (tj.isTransactionActive()) {
                    tj.rollbackTransaction();
                }
            }
        } finally {
            System.out.println(testName + ": End");
        }
    }

    // -------------------------

    public void testSelectTrim(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                               Object managedComponentObject) throws Throwable {
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

        //TODO: Disable test until EclipseLink 3.0/2.7 are updated to include the fix
        if ((isUsingJPA30Feature() || isUsingJPA22Feature()) && JPAProviderImpl.ECLIPSELINK.equals(getJPAProviderImpl(jpaResource))) {
            return;
        }

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));

        final boolean isMySQL = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.MYSQL);
        final boolean isOracle = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.ORACLE);
        final boolean isSQLServer = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.SQLSERVER);
        final boolean isSybase = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.SYBASE);
        final boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DERBY);
        final boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2);

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            try {
                SQLListener.getAndClear();

                System.out.println("Creating JPQL query");
                Query jpqlQuery = em.createQuery("SELECT TRIM('A' FROM 'AAHELLO WORDAAAAA') FROM TrimEntityOLGH16772 e WHERE (e.strVal1 = 'HELLO')");
                // Set parameter binding off so that we can validate the arguments
                jpqlQuery.setHint(QueryHints.BIND_PARAMETERS, HintValues.FALSE);

                System.out.println("Executing JPQL query");
                List<?> resultList = jpqlQuery.getResultList();
                Assert.assertNotNull(resultList);

                List<String> sql = SQLListener.getAndClear();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());

                if (isSybase) {
                    String expected = "SELECT STR_REPLACE('A', 'AAHELLO WORDAAAAA', NULL) FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = 'HELLO'))";
                    Assert.assertEquals(expected, sql.get(0));
                } else if (isSQLServer) {
                    String expected = "SELECT RTRIM('A' FROM LTRIM('A' FROM 'AAHELLO WORDAAAAA')) FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = 'HELLO')";
                    Assert.assertEquals(expected, sql.get(0));
                } else {
                    String expected = "SELECT TRIM('A' FROM 'AAHELLO WORDAAAAA') FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = 'HELLO')";
                    Assert.assertEquals(expected, sql.get(0));
                }

                System.out.println("Creating CriteriaBuilder query");
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
                Root<TrimEntityOLGH16772> root = cquery.from(TrimEntityOLGH16772.class);
                cquery.multiselect(cb.trim(cb.literal('A'), cb.literal("AAHELLO WORDAAAAA")));
                cquery.where(cb.equal(root.get(TrimEntityOLGH16772_.strVal1), cb.literal("HELLO")));

                Query query = em.createQuery(cquery);
                // Set parameter binding off so that we can validate the arguments
                query.setHint(QueryHints.BIND_PARAMETERS, HintValues.FALSE);

                System.out.println("Executing CriteriaBuilder query");
                resultList = query.getResultList();
                Assert.assertNotNull(resultList);

                sql = SQLListener.getAndClear();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());

                if (isSybase) {
                    String expected = "SELECT STR_REPLACE('A', 'AAHELLO WORDAAAAA', NULL) FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = 'HELLO'))";
                    Assert.assertEquals(expected, sql.get(0));
                } else if (isSQLServer) {
                    String expected = "SELECT RTRIM('A' FROM LTRIM('A' FROM 'AAHELLO WORDAAAAA')) FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = 'HELLO')";
                    Assert.assertEquals(expected, sql.get(0));
                } else {
                    String expected = "SELECT TRIM('A' FROM 'AAHELLO WORDAAAAA') FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = 'HELLO')";
                    Assert.assertEquals(expected, sql.get(0));
                }
            } finally {
                if (tj.isTransactionActive()) {
                    tj.rollbackTransaction();
                }
            }
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testSelectLeftTrim(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) throws Throwable {
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

        //TODO: Disable test until EclipseLink 3.0/2.7 are updated to include the fix
        if ((isUsingJPA30Feature() || isUsingJPA22Feature()) && JPAProviderImpl.ECLIPSELINK.equals(getJPAProviderImpl(jpaResource))) {
            return;
        }

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));

        final boolean isMySQL = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.MYSQL);
        final boolean isOracle = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.ORACLE);
        final boolean isSQLServer = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.SQLSERVER);
        final boolean isSybase = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.SYBASE);
        final boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DERBY);
        final boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2);

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            try {
                SQLListener.getAndClear();

                System.out.println("Creating JPQL query");
                Query jpqlQuery = em.createQuery("SELECT TRIM(LEADING 'A' FROM 'AAHELLO WORDAAAAA') FROM TrimEntityOLGH16772 e WHERE (e.strVal1 = 'HELLO')");
                // Set parameter binding off so that we can validate the arguments
                jpqlQuery.setHint(QueryHints.BIND_PARAMETERS, HintValues.FALSE);

                System.out.println("Executing JPQL query");
                List<?> resultList = jpqlQuery.getResultList();
                Assert.assertNotNull(resultList);

                List<String> sql = SQLListener.getAndClear();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());

                if (isMySQL || isDB2 || isDerby) {
                    String expected = "SELECT TRIM(LEADING 'A' FROM 'AAHELLO WORDAAAAA') FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = 'HELLO')";
                    Assert.assertEquals(expected, sql.get(0));
                } else {
                    String expected = "SELECT LTRIM('A', 'AAHELLO WORDAAAAA') FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = 'HELLO')";
                    Assert.assertEquals(expected, sql.get(0));
                }

                System.out.println("Creating CriteriaBuilder query");
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
                Root<TrimEntityOLGH16772> root = cquery.from(TrimEntityOLGH16772.class);
                cquery.multiselect(cb.trim(Trimspec.LEADING, cb.literal('A'), cb.literal("AAHELLO WORDAAAAA")));
                cquery.where(cb.equal(root.get(TrimEntityOLGH16772_.strVal1), cb.literal("HELLO")));

                Query query = em.createQuery(cquery);
                // Set parameter binding off so that we can validate the arguments
                query.setHint(QueryHints.BIND_PARAMETERS, HintValues.FALSE);

                System.out.println("Executing CriteriaBuilder query");
                resultList = query.getResultList();
                Assert.assertNotNull(resultList);

                sql = SQLListener.getAndClear();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());

                if (isMySQL || isDB2 || isDerby) {
                    String expected = "SELECT TRIM(LEADING 'A' FROM 'AAHELLO WORDAAAAA') FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = 'HELLO')";
                    Assert.assertEquals(expected, sql.get(0));
                } else {
                    String expected = "SELECT LTRIM('A', 'AAHELLO WORDAAAAA') FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = 'HELLO')";
                    Assert.assertEquals(expected, sql.get(0));
                }
            } finally {
                if (tj.isTransactionActive()) {
                    tj.rollbackTransaction();
                }
            }
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testSelectRightTrim(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                    Object managedComponentObject) throws Throwable {
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

        //TODO: Disable test until EclipseLink 3.0/2.7 are updated to include the fix
        if ((isUsingJPA30Feature() || isUsingJPA22Feature()) && JPAProviderImpl.ECLIPSELINK.equals(getJPAProviderImpl(jpaResource))) {
            return;
        }

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));

        final boolean isMySQL = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.MYSQL);
        final boolean isOracle = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.ORACLE);
        final boolean isSQLServer = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.SQLSERVER);
        final boolean isSybase = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.SYBASE);
        final boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DERBY);
        final boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2);

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            try {
                SQLListener.getAndClear();

                System.out.println("Creating JPQL query");
                Query jpqlQuery = em.createQuery("SELECT TRIM(TRAILING 'A' FROM 'AAHELLO WORDAAAAA') FROM TrimEntityOLGH16772 e WHERE (e.strVal1 = 'HELLO')");
                // Set parameter binding off so that we can validate the arguments
                jpqlQuery.setHint(QueryHints.BIND_PARAMETERS, HintValues.FALSE);

                System.out.println("Executing JPQL query");
                List<?> resultList = jpqlQuery.getResultList();
                Assert.assertNotNull(resultList);

                List<String> sql = SQLListener.getAndClear();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());

                if (isMySQL || isDB2 || isDerby) {
                    String expected = "SELECT TRIM(TRAILING 'A' FROM 'AAHELLO WORDAAAAA') FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = 'HELLO')";
                    Assert.assertEquals(expected, sql.get(0));
                } else {
                    String expected = "SELECT RTRIM('A', 'AAHELLO WORDAAAAA') FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = 'HELLO')";
                    Assert.assertEquals(expected, sql.get(0));
                }

                System.out.println("Creating CriteriaBuilder query");
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<Object[]> cquery = cb.createQuery(Object[].class);
                Root<TrimEntityOLGH16772> root = cquery.from(TrimEntityOLGH16772.class);
                cquery.multiselect(cb.trim(Trimspec.TRAILING, cb.literal('A'), cb.literal("AAHELLO WORDAAAAA")));
                cquery.where(cb.equal(root.get(TrimEntityOLGH16772_.strVal1), cb.literal("HELLO")));

                Query query = em.createQuery(cquery);
                // Set parameter binding off so that we can validate the arguments
                query.setHint(QueryHints.BIND_PARAMETERS, HintValues.FALSE);

                System.out.println("Executing CriteriaBuilder query");
                resultList = query.getResultList();
                Assert.assertNotNull(resultList);

                sql = SQLListener.getAndClear();
                Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());

                if (isMySQL || isDB2 || isDerby) {
                    String expected = "SELECT TRIM(TRAILING 'A' FROM 'AAHELLO WORDAAAAA') FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = 'HELLO')";
                    Assert.assertEquals(expected, sql.get(0));
                } else {
                    String expected = "SELECT RTRIM('A', 'AAHELLO WORDAAAAA') FROM TRIMENTITYOLGH16772 WHERE (STRVAL1 = 'HELLO')";
                    Assert.assertEquals(expected, sql.get(0));
                }
            } finally {
                if (tj.isTransactionActive()) {
                    tj.rollbackTransaction();
                }
            }
        } finally {
            System.out.println(testName + ": End");
        }
    }
}
