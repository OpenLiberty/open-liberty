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

package com.ibm.ws.jpa.olgh19176.testlogic;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.EntityManager;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh19176.model.WeavedEntityAOLGH19176;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH19176Logic extends AbstractTestLogic {

    public void testHiddenAttribute(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        //TODO: Disable test until EclipseLink 3.0 is updated to include the fix
        //TODO: Disable test until EclipseLink 2.7 is updated to include the fix
        if ((isUsingJPA22Feature() || isUsingJPA30Feature()) && JPAProviderImpl.ECLIPSELINK.equals(getJPAProviderImpl(jpaResource))) {
            return;
        }

        long id = 123l;

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + WeavedEntityAOLGH19176.class + " (id=" + id + ")...");
            final WeavedEntityAOLGH19176 e1 = em.find(WeavedEntityAOLGH19176.class, id);

            Assert.assertNotNull(e1);
            Assert.assertNotNull(e1.getHiddenAttribute());

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();
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
