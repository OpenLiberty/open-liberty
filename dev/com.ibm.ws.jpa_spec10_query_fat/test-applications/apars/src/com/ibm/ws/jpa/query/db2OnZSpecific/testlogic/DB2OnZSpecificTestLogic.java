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

package com.ibm.ws.jpa.query.db2OnZSpecific.testlogic;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.StoredProcedureQuery;

import org.junit.Assert;

import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class DB2OnZSpecificTestLogic extends AbstractTestLogic {

    /**
     * Tests various permutations of the JPQL Max() function.
     *
     * @param testExecCtx
     * @param testExecResources
     * @param managedComponentObject
     */
    public void testStoredProcedureNamedParameter(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        final String lDbProductName = dbProductName.toLowerCase();

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // Auto pass if not testing DB2 on Z
            if (!isDB2ForZOS(dbProductVersion)) {
                System.out.println("Not testing against a DB/2 on z/OS database.  Auto-Pass.");
                return;
            }

            System.out.println("Determined that the target database is DB/2 on z/OS.");

            // This should be a stored procedure that already exists by default
            StoredProcedureQuery storedProcedure = em.createStoredProcedureQuery("GET_SYSTEM_INFO ");
            storedProcedure.registerStoredProcedureParameter("major_version", Integer.class, ParameterMode.INOUT);
            storedProcedure.registerStoredProcedureParameter("minor_version", Integer.class, ParameterMode.INOUT);
            storedProcedure.registerStoredProcedureParameter("requested_locale", String.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter("xml_input", String.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter("xml_filter", String.class, ParameterMode.IN);
            storedProcedure.registerStoredProcedureParameter("xml_output", String.class, ParameterMode.OUT);
            storedProcedure.registerStoredProcedureParameter("xml_message", String.class, ParameterMode.OUT);

            storedProcedure.setParameter("major_version", null);
            storedProcedure.setParameter("minor_version", null);
            storedProcedure.setParameter("requested_locale", null);
            storedProcedure.setParameter("xml_input", null);
            storedProcedure.setParameter("xml_filter", null);

            //This will throw an exception if anything is wrong
            storedProcedure.execute();

            //Some driver versions seem to return the wrong type and will throw a conversion exception here
            String xml_output = (String) storedProcedure.getOutputParameterValue("xml_output");
            String xml_message = (String) storedProcedure.getOutputParameterValue("xml_message");
            em.clear();

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
