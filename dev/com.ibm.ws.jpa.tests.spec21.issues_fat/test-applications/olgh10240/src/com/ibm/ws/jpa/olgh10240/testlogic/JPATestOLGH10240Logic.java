/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.olgh10240.testlogic;

import java.io.Serializable;
import java.util.List;
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

public class JPATestOLGH10240Logic extends AbstractTestLogic {

    public void testCursorStoredProcedureIndexParameters(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
        final boolean isDB2LUW = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2LUW);

        // TODO: Add support for more database platforms
        if (!isOracle || !isDB2LUW) {
            System.out.println("This test does not support database platform " + dbProductName);
            return;
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            StoredProcedureQuery storedProcedure = em.createStoredProcedureQuery("simple_cursor_procedure");
            storedProcedure.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter(2, void.class, ParameterMode.REF_CURSOR);
            storedProcedure.setParameter(1, 64);
            storedProcedure.execute();

            List<Object[]> returnValue = (List<Object[]>) storedProcedure.getOutputParameterValue(2);

            Assert.assertEquals(1, returnValue.size());
            Object[] ret = returnValue.get(0);
            Assert.assertEquals(1, ret.length);
            Assert.assertEquals(ret[0], "StrTwo");
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testCursorStoredProcedureNamedParameters(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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
        final boolean isDB2LUW = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.DB2LUW);

        // TODO: Add support for more database platforms
        if (!isOracle || !isDB2LUW) {
            System.out.println("This test does not support database platform " + dbProductName);
            return;
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            StoredProcedureQuery storedProcedure = em.createStoredProcedureQuery("simple_cursor_procedure");
            storedProcedure.registerStoredProcedureParameter("in_param_one", Integer.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter("out_cursor_one", void.class, ParameterMode.REF_CURSOR);
            storedProcedure.setParameter("in_param_one", 128);
            storedProcedure.execute();

            List<Object[]> returnValue = (List<Object[]>) storedProcedure.getOutputParameterValue("out_cursor_one");

            Assert.assertEquals(1, returnValue.size());
            Object[] ret = returnValue.get(0);
            Assert.assertEquals(1, ret.length);
            Assert.assertEquals(ret[0], "StrThree");
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
