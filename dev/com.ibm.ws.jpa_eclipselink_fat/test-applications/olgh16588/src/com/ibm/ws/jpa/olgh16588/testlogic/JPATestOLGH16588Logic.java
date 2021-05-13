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

package com.ibm.ws.jpa.olgh16588.testlogic;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.Assert;

import com.ibm.ws.jpa.query.sqlcapture.SQLListener;
import com.ibm.ws.testtooling.database.DatabaseVendor;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH16588Logic extends AbstractTestLogic {

    public void testJPQLAggregateCollection(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        //TODO: Disable test until EclipseLink 3.0/2.7 are updated to include the fix
        if ((isUsingJPA30Feature() || isUsingJPA22Feature()) && JPAProviderImpl.ECLIPSELINK.equals(getJPAProviderImpl(jpaResource))) {
            return;
        }

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));

        final boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DERBY);
        final boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2);

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            SQLListener.getAndClear();
            /*
             * ContactInfo.previousAddresses should end up being an AggregateCollectionMapping
             */
            Query queryEmbed = em.createQuery("SELECT p.city FROM SpecEmployeeOLGH16588 e JOIN e.contactInfo.previousAddresses p WHERE e.contactInfo.primaryAddress.zipcode = ?1");
            queryEmbed.setParameter(1, "95054");

            final List<?> resultList = queryEmbed.getResultList();
            Assert.assertNotNull(resultList);

            List<String> sql = SQLListener.getAndClear();
            Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
            if (isDerby || isDB2) {
                String expected = "SELECT t0.CITY FROM PREV_ADDRESSES t0, SPECEMPLOYEEOLGH16588 t1 WHERE ((t1.ZIPCODE = ?) AND (t0.SpecEmployeeOLGH16588_ID = t1.ID))";
                Assert.assertEquals(expected, sql.get(0));
            } // TODO: other databases

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testJPQLJoin(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        //TODO: Disable test until EclipseLink 3.0/2.7 are updated to include the fix
        if ((isUsingJPA30Feature() || isUsingJPA22Feature()) && JPAProviderImpl.ECLIPSELINK.equals(getJPAProviderImpl(jpaResource))) {
            return;
        }

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));

        final boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DERBY);
        final boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2);

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            SQLListener.getAndClear();

            // According to the JPA Spec, section 4.4.4, the following two queries are equivalent
            Query queryEmbed = em.createQuery("SELECT p.vendor FROM SpecEmployeeOLGH16588 e JOIN e.contactInfo.phones p WHERE e.contactInfo.primaryAddress.zipcode = ?1");
            queryEmbed.setParameter(1, "95051");

            List<?> resultList = queryEmbed.getResultList();
            Assert.assertNotNull(resultList);

            List<String> sql = SQLListener.getAndClear();

            Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
            if (isDerby || isDB2) {
                String expected = "SELECT t0.VENDOR FROM SPECPHONEOLGH16588 t0, SPECEMPLOYEEOLGH16588_SPECPHONEOLGH16588 t2, SPECEMPLOYEEOLGH16588 t1 WHERE ((t1.ZIPCODE = ?) AND ((t2.SpecEmployeeOLGH16588_ID = t1.ID) AND (t0.ID = t2.phones_ID)))";
                Assert.assertEquals(expected, sql.get(0));
            } // TODO: other databases

            queryEmbed = em.createQuery("SELECT p.vendor FROM SpecEmployeeOLGH16588 e JOIN e.contactInfo c JOIN c.phones p WHERE e.contactInfo.primaryAddress.zipcode = ?1");
            queryEmbed.setParameter(1, "95052");

            resultList = queryEmbed.getResultList();
            Assert.assertNotNull(resultList);

            sql = SQLListener.getAndClear();

            Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
            if (isDerby || isDB2) {
                String expected = "SELECT t0.VENDOR FROM SPECPHONEOLGH16588 t0, SPECEMPLOYEEOLGH16588_SPECPHONEOLGH16588 t2, SPECEMPLOYEEOLGH16588 t1 WHERE ((t1.ZIPCODE = ?) AND ((t2.SpecEmployeeOLGH16588_ID = t1.ID) AND (t0.ID = t2.phones_ID)))";
                Assert.assertEquals(expected, sql.get(0));
            } // TODO: other databases

        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testJPQLNestedEmbeddable(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        //TODO: Disable test until EclipseLink 3.0/2.7 are updated to include the fix
        if ((isUsingJPA30Feature() || isUsingJPA22Feature()) && JPAProviderImpl.ECLIPSELINK.equals(getJPAProviderImpl(jpaResource))) {
            return;
        }

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));

        final boolean isDerby = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DERBY);
        final boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2);

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            SQLListener.getAndClear();

            // Test to make sure that moving around the dot notation for nested embeddables is valid and doesn't change the query
            Query queryEmbed = em.createQuery("SELECT c.primaryAddress.city FROM SpecEmployeeOLGH16588 e JOIN e.contactInfo c WHERE e.contactInfo.primaryAddress.zipcode = ?1");
            queryEmbed.setParameter(1, "95053");

            List<?> resultList = queryEmbed.getResultList();
            Assert.assertNotNull(resultList);

            List<String> sql = SQLListener.getAndClear();

            Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
            if (isDerby || isDB2) {
                String expected = "SELECT CITY FROM SPECEMPLOYEEOLGH16588 WHERE (ZIPCODE = ?)";
                Assert.assertEquals(expected, sql.get(0));
            } // TODO: other databases

            queryEmbed = em.createQuery("SELECT p.city FROM SpecEmployeeOLGH16588 e JOIN e.contactInfo.primaryAddress p WHERE e.contactInfo.primaryAddress.zipcode = ?1");
            queryEmbed.setParameter(1, "95054");

            resultList = queryEmbed.getResultList();
            Assert.assertNotNull(resultList);

            sql = SQLListener.getAndClear();

            Assert.assertEquals("Expected 1 line of SQL to have been generated.", 1, sql.size());
            if (isDerby || isDB2) {
                String expected = "SELECT CITY FROM SPECEMPLOYEEOLGH16588 WHERE (ZIPCODE = ?)";
                Assert.assertEquals(expected, sql.get(0));
            } // TODO: other databases

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
