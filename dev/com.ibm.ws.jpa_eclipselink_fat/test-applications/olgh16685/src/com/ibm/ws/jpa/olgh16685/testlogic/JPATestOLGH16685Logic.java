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

package com.ibm.ws.jpa.olgh16685.testlogic;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.junit.Assert;

import com.ibm.ws.jpa.olgh16685.model.CriteriaCarOLGH16685;
import com.ibm.ws.jpa.olgh16685.model.CriteriaCarOLGH16685_;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class JPATestOLGH16685Logic extends AbstractTestLogic {

    /**
     * Test that select clause can contain parameter markers in JPQL and CriteriaBuilder API
     */
    public void testJPQLCriteriaSelectClauseParameters(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            em.clear();

            // First test JPQL
            TypedQuery<Object[]> query = em.createQuery("SELECT c.id, ?1 FROM CriteriaCarOLGH16685 c WHERE c.id = ?2", Object[].class);
            query.setParameter(1, "TEST");
            query.setParameter(2, "ID1");
            query.getResultList();

            final CriteriaBuilder criteriabuilder = em.getCriteriaBuilder();
            final CriteriaQuery<Object[]> criteriaquery = criteriabuilder.createQuery(Object[].class);
            Root<CriteriaCarOLGH16685> root = criteriaquery.from(CriteriaCarOLGH16685.class);
            criteriaquery.multiselect(root.get(CriteriaCarOLGH16685_.id), criteriabuilder.parameter(String.class, "stringValue"));
            criteriaquery.where(criteriabuilder.equal(root.get(CriteriaCarOLGH16685_.id), criteriabuilder.parameter(String.class, "idValue")));

            query = em.createQuery(criteriaquery);
            query.setParameter("stringValue", "TEST");
            query.setParameter("idValue", "ID1");
            query.getResultList();
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
