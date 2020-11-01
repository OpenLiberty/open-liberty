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

package com.ibm.ws.jpa.olgh14457.testlogic;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.junit.Assert;

import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH14457Logic extends AbstractTestLogic {

    public void testCaseExpressionReturnType(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                             Object managedComponentObject) {
        final String testName = getTestName();

        if (isUsingJPA30Feature()) {
            System.out.println("testEmptyAggregateFunctionsWithPrimitives is not intended for jpa-3.0");
            return;
        }

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

        //TODO: Disable test until EclipseLink 2.7 is updated
        //TODO: Disable test until EclipseLink 3.0 is updated to include the fix
        if ((isUsingJPA22Feature() || isUsingJPA30Feature()) && getJPAProviderImpl(jpaResource).equals(JPAProviderImpl.ECLIPSELINK)) {
            return;
        }

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();

            TypedQuery<Boolean> query = em.createQuery("SELECT ("
                                                       + "CASE "
                                                       + "WHEN t.itemInteger1 = 1 THEN TRUE "
                                                       + "ELSE FALSE "
                                                       + "END "
                                                       + ") "
                                                       + "FROM SimpleEntityOLGH14457 t ORDER BY t.itemInteger1 ASC", Boolean.class);

            List<Boolean> boolList = query.getResultList();
            Assert.assertNotNull(boolList);
            Assert.assertEquals(2, boolList.size());
            Assert.assertEquals(true, boolList.get(0));
            Assert.assertEquals(false, boolList.get(1));
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
