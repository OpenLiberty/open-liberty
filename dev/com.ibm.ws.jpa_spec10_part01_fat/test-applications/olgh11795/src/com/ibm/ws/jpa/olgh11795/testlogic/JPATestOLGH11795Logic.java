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

package com.ibm.ws.jpa.olgh11795.testlogic;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh11795.model.SimpleEmbeddableOLGH11795;
import com.ibm.ws.jpa.olgh11795.model.SimpleParentEntityOLGH11795;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH11795Logic extends AbstractTestLogic {

    public void testJoinColumnWithSameDuplicateName(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        Long id = 42l;
        Long pid = 52l;

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();

            TypedQuery<SimpleEmbeddableOLGH11795> query = em.createQuery("SELECT s.parentRef FROM SimpleChildEntityOLGH11795 s WHERE s.id = :id", SimpleEmbeddableOLGH11795.class);
            query.setParameter("id", id);

            List<SimpleEmbeddableOLGH11795> res = query.getResultList();

            Assert.assertEquals(1, res.size());
            SimpleEmbeddableOLGH11795 e = res.get(0);
            Assert.assertNotNull(e);
            SimpleParentEntityOLGH11795 p = e.getParent();
            Assert.assertEquals(pid, p.getId());
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
