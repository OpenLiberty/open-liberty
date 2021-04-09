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

package com.ibm.ws.jpa.olgh8820.testlogic;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;

import org.junit.Assert;

import com.ibm.ws.testtooling.database.DatabaseVendor;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH8820Logic extends AbstractTestLogic {

    public void testStoredProcedureOrderWithIndexParameter(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));

        final boolean isOracle = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.ORACLE);
        final boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2);
        final boolean isMySQL = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.MYSQL);

        // TODO: Add support for more database platforms
        if (!isOracle || !isDB2 || !isMySQL) {
            System.out.println("This test does not support database platform " + dbProductName);
            return;
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            //Test simple order
            StoredProcedureQuery storedProcedure = em.createStoredProcedureQuery("simple_order_procedure");
            storedProcedure.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter(4, String.class, ParameterMode.OUT);
            storedProcedure.setParameter(1, "One");
            storedProcedure.setParameter(2, "Two");
            storedProcedure.setParameter(3, "Three");
            storedProcedure.execute();

            String returnValue = (String) storedProcedure.getOutputParameterValue(4);
            Assert.assertEquals("One: One Two: Two Three: Three", returnValue);

            //Make sure changing the order does change the result
            em = jpaResource.getEm();
            storedProcedure = em.createStoredProcedureQuery("simple_order_procedure");
            storedProcedure.registerStoredProcedureParameter(2, String.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter(1, String.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter(3, String.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter(4, String.class, ParameterMode.OUT);
            storedProcedure.setParameter(2, "Two");
            storedProcedure.setParameter(1, "One");
            storedProcedure.setParameter(3, "Three");
            storedProcedure.execute();

            returnValue = (String) storedProcedure.getOutputParameterValue(4);
            Assert.assertEquals("One: Two Two: One Three: Three", returnValue);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testStoredProcedureOrderWithNamedParameter(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));

        final boolean isOracle = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.ORACLE);
        final boolean isDB2 = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2);
        final boolean isMySQL = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.MYSQL);

        // TODO: Add support for more database platforms
        if (!isOracle || !isDB2 || !isMySQL) {
            System.out.println("This test does not support database platform " + dbProductName);
            return;
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();

            //Test simple order
            StoredProcedureQuery storedProcedure = em.createStoredProcedureQuery("simple_order_procedure");
            storedProcedure.registerStoredProcedureParameter("in_param_one", String.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter("in_param_two", String.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter("in_param_three", String.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter("out_param_one", String.class, ParameterMode.OUT);
            storedProcedure.setParameter("in_param_one", "One");
            storedProcedure.setParameter("in_param_two", "Two");
            storedProcedure.setParameter("in_param_three", "Three");
            storedProcedure.execute();

            String returnValue = (String) storedProcedure.getOutputParameterValue("out_param_one");
            Assert.assertEquals("One: One Two: Two Three: Three", returnValue);

            //Make sure changing the order doesn't change the result
            em = jpaResource.getEm();
            storedProcedure = em.createStoredProcedureQuery("simple_order_procedure");
            storedProcedure.registerStoredProcedureParameter("out_param_one", String.class, ParameterMode.OUT);
            storedProcedure.registerStoredProcedureParameter("in_param_two", String.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter("in_param_one", String.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter("in_param_three", String.class, ParameterMode.IN);
            storedProcedure.setParameter("in_param_two", "Two");
            storedProcedure.setParameter("in_param_one", "One");
            storedProcedure.setParameter("in_param_three", "Three");
            storedProcedure.execute();

            returnValue = (String) storedProcedure.getOutputParameterValue("out_param_one");
            Assert.assertEquals("One: One Two: Two Three: Three", returnValue);
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
