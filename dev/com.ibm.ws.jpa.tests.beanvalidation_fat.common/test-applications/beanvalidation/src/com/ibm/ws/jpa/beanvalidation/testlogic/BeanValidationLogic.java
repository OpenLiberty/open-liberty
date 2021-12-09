/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.beanvalidation.testlogic;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.validation.ConstraintViolationException;

import org.junit.Assert;

import com.ibm.ws.jpa.beanvalidation.model.SimpleBeanValEntity;
import com.ibm.ws.jpa.beanvalidation.model.SimpleBeanValXMLEntity;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class BeanValidationLogic extends AbstractTestLogic {

    /**
     * Simple test to ensure bean validation is working with annotations
     *
     * According to the JPA specification, section 3.6.1.2:
     *
     * By default, the default Bean Validation group (the group Default) will be validated upon the pre-persist
     * and pre-update lifecycle validation events
     *
     * If the set of ConstraintViolation objects returned by the validate method is not empty, the persistence provider
     * must throw the javax.validation.ConstraintViolationException containing a reference to the returned set of
     * ConstraintViolation objects, and must mark the transaction for rollback if the persistence context is joined to the transaction.
     *
     * Hibernate:
     *
     * For Hibernate, a javax.transaction.RollbackException is thrown on transaction commit; not on the prePersist callback which is default.
     * According to Hibernate, this is by design as they lazily call prePersist callbacks on em.flush(), not on em.persist().
     * https://hibernate.atlassian.net/browse/HHH-8028
     *
     * OpenJPA:
     *
     * For OpenJPA, a javax.validation.ConstraintViolationException is thrown on em.persist(); which is the default.
     */
    public void testBeanValidationAnno(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 10;
            String validationMessage = "SimpleBeanValEntity.name is null";
            boolean onCommit = false;

            em.clear();

            try {
                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                SimpleBeanValEntity entity = new SimpleBeanValEntity();

                entity.setId(id);
                entity.setName(null);
                em.persist(entity);

                onCommit = true;
                tj.commitTransaction();

                Assert.fail("Bean Validation did not occur: null name should not be allowed");
            } catch (ConstraintViolationException cve) {
                // this is how the JPA Specification defines the failure should occur
                Assert.assertFalse("Exception was thrown on transaction commit, not entity persist", onCommit);
                Assert.assertEquals(validationMessage, cve.getConstraintViolations().iterator().next().getMessage());
                Assert.assertTrue("Transaction was not marked for rollback", tj.isTransactionMarkedForRollback());
            } catch (Throwable t) {
                // Hibernate fails on transaction commit; throwing a RollbackException with ConstraintViolationException as the cause
                Throwable root = containsCauseByException(ConstraintViolationException.class, t);
                Assert.assertNotNull("Throwable stack did not contain " + ConstraintViolationException.class, root);
                Assert.assertEquals(validationMessage, ((ConstraintViolationException) root).getConstraintViolations().iterator().next().getMessage());
            } finally {
                if (tj.isTransactionActive()) {
                    tj.markTransactionForRollback();
                }
            }
        } finally {
            System.out.println(testName + ": End");
        }
    }

    /**
     * Simple test to ensure bean validation is working with XML mappings
     */
    public void testBeanValidationXML(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

        // Execute Test Case
        try {
            EntityManager em = jpaResource.getEm();
            TransactionJacket tj = jpaResource.getTj();

            int id = 10;
            String validationMessage = "SimpleBeanValXMLEntity.name is null";
            boolean onCommit = false;

            em.clear();

            try {
                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                SimpleBeanValXMLEntity entity = new SimpleBeanValXMLEntity();

                entity.setId(id);
                entity.setName(null);
                em.persist(entity);

                onCommit = true;
                tj.commitTransaction();

                Assert.fail("Bean Validation did not occur: null name should not be allowed");
            } catch (ConstraintViolationException cve) {
                // this is how the JPA Specification defines the failure should occur
                Assert.assertFalse("Exception was thrown on transaction commit, not entity persist", onCommit);
                Assert.assertEquals(validationMessage, cve.getConstraintViolations().iterator().next().getMessage());
                Assert.assertTrue("Transaction was not marked for rollback", tj.isTransactionMarkedForRollback());
            } catch (Throwable t) {
                // Hibernate fails on transaction commit; throwing a RollbackException with ConstraintViolationException as the cause
                Throwable root = containsCauseByException(ConstraintViolationException.class, t);
                Assert.assertNotNull("Throwable stack did not contain " + ConstraintViolationException.class, root);
                Assert.assertEquals(validationMessage, ((ConstraintViolationException) root).getConstraintViolations().iterator().next().getMessage());
            } finally {
                if (tj.isTransactionActive()) {
                    tj.markTransactionForRollback();
                }
            }
        } finally {
            System.out.println(testName + ": End");
        }
    }
}
