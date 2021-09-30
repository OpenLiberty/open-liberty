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

package com.ibm.ws.jpa.beanvalidation20.testlogic;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.validation.ConstraintViolationException;

import org.junit.Assert;

import com.ibm.ws.jpa.beanvalidation20.model.SimpleBeanVal20Entity;
import com.ibm.ws.jpa.beanvalidation20.model.SimpleBeanVal20XMLEntity;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.tranjacket.TransactionJacket;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class BeanValidation20Logic extends AbstractTestLogic {

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
    public void testBeanValidationAnno001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            em.clear();

            try {
                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                SimpleBeanVal20Entity entity = new SimpleBeanVal20Entity();
                entity.setId(id);
                entity.setEmail("valid_email@somewhere.com");

                em.persist(entity);

                tj.commitTransaction();

                Assert.assertEquals("Email not set", "valid_email@somewhere.com", entity.getEmail());

                // Begin new transaction
                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                System.out.println("Performing merge operation: " + entity);
                entity = em.merge(entity);

                System.out.println("Performing remove operation: " + entity);
                em.remove(entity);

                System.out.println("Committing transaction...");
                if (tj.isTransactionActive()) {
                    tj.commitTransaction();
                }
            } finally {
                if (tj.isTransactionActive()) {
                    tj.markTransactionForRollback();
                }
            }
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testBeanValidationXML001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            int id = 11;

            em.clear();

            try {
                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                SimpleBeanVal20XMLEntity entity = new SimpleBeanVal20XMLEntity();
                entity.setId(id);
                entity.setEmail("valid_email@somewhere.com");

                em.persist(entity);

                tj.commitTransaction();

                Assert.assertEquals("Email not set", "valid_email@somewhere.com", entity.getEmail());

                // Begin new transaction
                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                System.out.println("Performing merge operation: " + entity);
                entity = em.merge(entity);

                System.out.println("Performing remove operation: " + entity);
                em.remove(entity);

                System.out.println("Committing transaction...");
                if (tj.isTransactionActive()) {
                    tj.commitTransaction();
                }
            } finally {
                if (tj.isTransactionActive()) {
                    tj.markTransactionForRollback();
                }
            }
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testBeanValidationAnno002(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            int id = 12;
            String validationMessage = "SimpleBeanVal20Entity.email is null";
            boolean onCommit = false;

            em.clear();

            try {
                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                SimpleBeanVal20Entity entity = new SimpleBeanVal20Entity();
                entity.setId(id);
                entity.setEmail(null);

                em.persist(entity);

                onCommit = true;
                tj.commitTransaction();

                Assert.fail("Bean Validation did not occur: null email should not be allowed");
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

    public void testBeanValidationXML002(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            int id = 13;
            String validationMessage = "SimpleBeanVal20XMLEntity.email is null";
            boolean onCommit = false;

            em.clear();

            try {
                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                SimpleBeanVal20XMLEntity entity = new SimpleBeanVal20XMLEntity();
                entity.setId(id);
                entity.setEmail(null);

                em.persist(entity);

                onCommit = true;
                tj.commitTransaction();

                Assert.fail("Bean Validation did not occur: null email should not be allowed");
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

    public void testBeanValidationAnno003(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            int id = 14;
            String validationMessage = "SimpleBeanVal20Entity.email is not well formed";
            boolean onCommit = false;

            em.clear();

            try {
                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                SimpleBeanVal20Entity entity = new SimpleBeanVal20Entity();
                entity.setId(id);
                entity.setEmail("not_valid_email");

                em.persist(entity);

                onCommit = true;
                tj.commitTransaction();

                Assert.fail("Bean Validation did not occur: invalid email should not be allowed");
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

    public void testBeanValidationXML003(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            int id = 15;
            String validationMessage = "SimpleBeanVal20XMLEntity.email is not well formed";
            boolean onCommit = false;

            em.clear();

            try {
                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                SimpleBeanVal20XMLEntity entity = new SimpleBeanVal20XMLEntity();
                entity.setId(id);
                entity.setEmail("not_valid_email");

                em.persist(entity);

                onCommit = true;
                tj.commitTransaction();

                Assert.fail("Bean Validation did not occur: invalid email should not be allowed");
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

    public void testBeanValidationAnno004(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            int id = 16;

            em.clear();

            try {
                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                SimpleBeanVal20Entity entity = new SimpleBeanVal20Entity();
                entity.setId(id);
                entity.setEmail("valid_email@somewhere.com"); // Must set email since it is @NotNull as well

                // Value should always be into the future to be accepted for validation
                // Hibernate only accepts values Instant.MAX < x < Instant.MIN because they convert to Timestamp:
                //    https://hibernate.atlassian.net/browse/HHH-13482
                java.time.Instant instant = java.time.Instant.now().plusSeconds(100000);
                entity.setFutureInstant(instant);

                em.persist(entity);

                tj.commitTransaction();

                // Begin new transaction
                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                System.out.println("Finding " + SimpleBeanVal20Entity.class + " (id=" + id + ")...");
                SimpleBeanVal20Entity find_entity = em.find(SimpleBeanVal20Entity.class, id);
                System.out.println("Object returned by find: " + find_entity);
                Assert.assertNotNull("Assert that the find operation did not return null", find_entity);
                Assert.assertEquals("FutureInstant not set", instant, find_entity.getFutureInstant());

                System.out.println("Performing merge operation: " + find_entity);
                find_entity = em.merge(find_entity);

                System.out.println("Performing remove operation: " + find_entity);
                em.remove(find_entity);

                System.out.println("Committing transaction...");
                if (tj.isTransactionActive()) {
                    tj.commitTransaction();
                }
            } finally {
                if (tj.isTransactionActive()) {
                    tj.markTransactionForRollback();
                }
            }
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testBeanValidationXML004(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            int id = 17;

            em.clear();

            try {
                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                SimpleBeanVal20XMLEntity entity = new SimpleBeanVal20XMLEntity();
                entity.setId(id);
                entity.setEmail("valid_email@somewhere.com"); // Must set email since it is @NotNull as well

                // Value should always be into the future to be accepted for validation
                // Hibernate only accepts values Instant.MAX < x < Instant.MIN because they convert to Timestamp:
                //    https://hibernate.atlassian.net/browse/HHH-13482
                java.time.Instant instant = java.time.Instant.now().plusSeconds(100000);
                entity.setFutureInstant(instant);

                em.persist(entity);

                tj.commitTransaction();

                // Begin new transaction
                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                System.out.println("Finding " + SimpleBeanVal20XMLEntity.class + " (id=" + id + ")...");
                SimpleBeanVal20XMLEntity find_entity = em.find(SimpleBeanVal20XMLEntity.class, id);
                System.out.println("Object returned by find: " + find_entity);
                Assert.assertNotNull("Assert that the find operation did not return null", find_entity);
                Assert.assertEquals("FutureInstant not set", instant, find_entity.getFutureInstant());

                System.out.println("Performing merge operation: " + find_entity);
                find_entity = em.merge(find_entity);

                System.out.println("Performing remove operation: " + find_entity);
                em.remove(find_entity);

                System.out.println("Committing transaction...");
                if (tj.isTransactionActive()) {
                    tj.commitTransaction();
                }
            } finally {
                if (tj.isTransactionActive()) {
                    tj.markTransactionForRollback();
                }
            }
        } finally {
            System.out.println(testName + ": End");
        }
    }

    public void testBeanValidationAnno005(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            int id = 18;
            String validationMessage = "SimpleBeanVal20Entity.futureInstant must be in the future";
            boolean onCommit = false;

            em.clear();

            try {
                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                SimpleBeanVal20Entity entity = new SimpleBeanVal20Entity();
                entity.setId(id);
                entity.setEmail("valid_email@somewhere.com"); // Must set email since it is @NotNull as well

                // Value should always be into the future to be accepted for validation
                // Hibernate only accepts values Instant.MAX < x < Instant.MIN because they convert to Timestamp:
                //    https://hibernate.atlassian.net/browse/HHH-13482
                java.time.Instant instant = java.time.Instant.now().minusSeconds(100000);
                entity.setFutureInstant(instant);

                em.persist(entity);

                onCommit = true;
                tj.commitTransaction();

                Assert.fail("Bean Validation did not occur: invalid Instant should not be allowed");
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

    public void testBeanValidationXML005(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
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

            int id = 19;
            String validationMessage = "SimpleBeanVal20XMLEntity.futureInstant must be in the future";
            boolean onCommit = false;

            em.clear();

            try {
                System.out.println("Beginning new transaction...");
                tj.beginTransaction();
                if (tj.isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    em.joinTransaction();
                }

                SimpleBeanVal20XMLEntity entity = new SimpleBeanVal20XMLEntity();
                entity.setId(id);
                entity.setEmail("valid_email@somewhere.com"); // Must set email since it is @NotNull as well

                // Value should always be into the future to be accepted for validation
                // Hibernate only accepts values Instant.MAX < x < Instant.MIN because they convert to Timestamp:
                //    https://hibernate.atlassian.net/browse/HHH-13482
                java.time.Instant instant = java.time.Instant.now().minusSeconds(100000);
                entity.setFutureInstant(instant);

                em.persist(entity);

                onCommit = true;
                tj.commitTransaction();

                Assert.fail("Bean Validation did not occur: invalid Instant should not be allowed");
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
