/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.annotationoverride.testlogic;

import java.lang.reflect.InvocationTargetException;

import javax.persistence.PersistenceException;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.annotationoverride.entities.GeneralAnnotationOverrideEntity;
import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.annotationoverride.entities.TableSchemaOverrideEntity;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class AnnotationOverrideTestLogic extends AbstractTestLogic {

    /**
     * Verify that table schema annotations can be overriden by their XML counterparts.
     *
     * 4 POINTS
     */
    public void testAnnotationOverride001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                          Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("AnnotationOverrideTestLogic.testAnnotationOverride001(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("AnnotationOverrideTestLogic.testAnnotationOverride001(): Begin");
            //cleanupDatabase(jpaCleanupResource, log);

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Construct a new entity instances
            AnnotationOverrideEntityEnum targetEntityType = AnnotationOverrideEntityEnum.TableSchemaOverrideEntity;
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=1)...");
            TableSchemaOverrideEntity new_entity = (TableSchemaOverrideEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(1);
            new_entity.setName("Dr. Doom");

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Fetching and verifying persistable contents of " + targetEntityType.getEntityName() + " (id=1)...");

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=1)...");
            TableSchemaOverrideEntity find_entity = (TableSchemaOverrideEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), 1);
            System.out.println("Object returned by find: " + find_entity);

            // Verify that em.find() returned an object. (1 point)
            Assert.assertNotNull("Assert that the find operation did not return null", find_entity);
            if (find_entity == null) {
                // If the find returned null, then terminate the remainder of the test.
                Assert.fail("Find returned null, cancelling the remainder of the test.");
                return;
            }

            //  Perform basic verifications (3 points)
            Assert.assertNotSame(
                                 "Assert find did not return the original object",
                                 new_entity,
                                 find_entity);
            Assert.assertTrue(
                              "Assert entity returned by find is managed by the persistence context.",
                              jpaResource.getEm().contains(find_entity));
            Assert.assertEquals(
                                "Assert that the entity's id is 1",
                                find_entity.getId(),
                                1);

            System.out.println("The entity was able to be persisted and retrieved correctly.");

            System.out.println("Testing complete, rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();

            System.out.println("Ending test.");
        } catch (ClassNotFoundException | SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException
                        | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            System.out.println("AnnotationOverrideTestLogic.testAnnotationOverride001(): End");
        }
    }

    /**
     * Verify that eager/lazy loading annotations can be overridden by their XML counterparts.
     *
     * 5 Points
     */
    public void testAnnotationOverride002(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                          Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("AnnotationOverrideTestLogic.testAnnotationOverride002(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("AnnotationOverrideTestLogic.testAnnotationOverride002(): Begin");
            //cleanupDatabase(jpaCleanupResource, log);

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Construct a new entity instances
            AnnotationOverrideEntityEnum targetEntityType = AnnotationOverrideEntityEnum.GeneralAnnotationOverrideEntity;
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=1)...");
            GeneralAnnotationOverrideEntity new_entity = (GeneralAnnotationOverrideEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(1);
            new_entity.setName("Dr. Doom");

            System.out.println("Setting the AnnotatedEagerName attribute to \"Eager String\"");
            new_entity.setAnnotatedEagerName("Eager String");

            System.out.println("Setting the AnnotatedLazyName attribute to \"Lazy String\"");
            new_entity.setAnnotatedLazyName("Lazy String");

            // Set these values to avoid non-null column conflicts
            new_entity.setAnnotatedNonUniqueName("Some Value 1");
            new_entity.setAnnotatedUniqueName("Some Value 1");

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Fetching and verifying persistable contents of " + targetEntityType.getEntityName() + " (id=1)...");

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println(
                               "Finding " + targetEntityType.getEntityName() +
                               " (id=1), and then clearing the persistence context before any persistable fields are read.");
            GeneralAnnotationOverrideEntity find_entity = (GeneralAnnotationOverrideEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), 1);
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();
            System.out.println("Object returned by find: " + find_entity);

            // Verify that em.find() returned an object. (1 point)
            Assert.assertNotNull("Assert that the find operation did not return null", find_entity);

            //  Perform basic verifications (3 points)
            Assert.assertNotSame(
                                 "Assert find did not return the original object",
                                 new_entity,
                                 find_entity);
            Assert.assertEquals(
                                "Assert that the entity's id is 1",
                                find_entity.getId(),
                                1);

            // Examine the AnnotatedEagerName and AnnotatedLazyName persistable fields.  The attributes are annotated to be
            // eager and lazy accordingly.  HOWEVER, the XML Mapping file should override this to be lazy and eager (converse
            // to the attribute name).
            Assert.assertEquals(
                                "Assert that the Lazy annotation was overriden by the XML Eager setting.",
                                "Lazy String",
                                find_entity.getAnnotatedLazyName());

            // LAZY loading is a hint, and may not be honored by all persistence providers.  Eclipselink is one of those.
            if (jpaResource.getEm().getDelegate().getClass().getName().toLowerCase().contains("eclipse")) {
                // log.assertPass("Eclipselink tends to ignore LAZY loading for Strings, so not asserting.");
            } else {
                Assert.assertNull(
                                  "Assert that the Eager annotation was overridden by the XML Lazy seting.",
                                  find_entity.getAnnotatedEagerName());
            }

            System.out.println("Testing complete, rolling back transaction...");
            jpaResource.getTj().rollbackTransaction();

            System.out.println("Ending test.");
        } catch (ClassNotFoundException | SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException
                        | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            System.out.println("AnnotationOverrideTestLogic.testAnnotationOverride002(): End");
        }
    }

    /**
     * Verify that optionality annotations can be overridden by their XML counterparts.
     *
     * 1 POINT
     */
    public void testAnnotationOverride003(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                          Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("AnnotationOverrideTestLogic.testAnnotationOverride003(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("AnnotationOverrideTestLogic.testAnnotationOverride003(): Begin");
            //cleanupDatabase(jpaCleanupResource, log);

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Construct a new entity instances
            AnnotationOverrideEntityEnum targetEntityType = AnnotationOverrideEntityEnum.GeneralAnnotationOverrideEntity;
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=1)...");
            GeneralAnnotationOverrideEntity new_entity = (GeneralAnnotationOverrideEntity) constructNewEntityObject(targetEntityType);

            System.out.println(
                               "Setting primary key, but not setting the name field.  The name field is annotated to be " +
                               "non-optional, however the XML Mapping file should override this non-optional declaration, " +
                               "setting it to be an optional field.");

            new_entity.setId(1);
            new_entity.setName(null);

            // Set these values to avoid non-null column conflicts
            new_entity.setAnnotatedNonUniqueName("Some Value 1");
            new_entity.setAnnotatedUniqueName("Some Value 1");

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Committing transaction (No exception should be thrown)...");
            jpaResource.getTj().commitTransaction();

            //log.assertPass("No Exception was thrown, as expected.");

            System.out.println("Ending test.");
        } catch (ClassNotFoundException | SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException
                        | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            System.out.println("AnnotationOverrideTestLogic.testAnnotationOverride003(): End");
        }
    }

    /**
     * Verify that unique-attribute annotations can be overriden by their XML counterparts.
     *
     * 2 POINTS
     */
    public void testAnnotationOverride004(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                          Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("AnnotationOverrideTestLogic.testAnnotationOverride004(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        String annotatedUniqueName_1 = "Luke";
        String annotatedNonUniqueName_1 = "Leia";

        String annotatedUniqueName_2 = "Luke";
        String annotatedNonUniqueName_2 = "Han";

        String annotatedUniqueName_3 = "Chewbaca";
        String annotatedNonUniqueName_3 = "Leia";

        // Execute Test Case
        try {
            System.out.println("AnnotationOverrideTestLogic.testAnnotationOverride004(): Begin");
            //cleanupDatabase(jpaCleanupResource, log);

            AnnotationOverrideEntityEnum targetEntityType = AnnotationOverrideEntityEnum.GeneralAnnotationOverrideEntity;

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Construct a new entity instance
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=1)...");
            GeneralAnnotationOverrideEntity new_entity1 = (GeneralAnnotationOverrideEntity) constructNewEntityObject(targetEntityType);

            new_entity1.setId(1);
            new_entity1.setName("Name 1");

            System.out.println(
                               "Setting persistent attributes AnnotatedUniqueName and AnnotatedNonUniqueName to " +
                               "\"" + annotatedUniqueName_1 + "\" and \"" + annotatedNonUniqueName_1 + "\"...");
            new_entity1.setAnnotatedUniqueName(annotatedUniqueName_1);
            new_entity1.setAnnotatedNonUniqueName(annotatedNonUniqueName_1);

            System.out.println("Persisting " + new_entity1);
            jpaResource.getEm().persist(new_entity1);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            // Construct another new entity instance
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=2)...");
            GeneralAnnotationOverrideEntity new_entity2 = (GeneralAnnotationOverrideEntity) constructNewEntityObject(targetEntityType);

            new_entity2.setId(2);
            new_entity2.setName("Name 2");

            System.out.println(
                               "Setting persistent attributes AnnotatedUniqueName and AnnotatedNonUniqueName to " +
                               "\"" + annotatedUniqueName_2 + "\" and \"" + annotatedNonUniqueName_2 + "\"...");
            new_entity2.setAnnotatedUniqueName(annotatedUniqueName_2);
            new_entity2.setAnnotatedNonUniqueName(annotatedNonUniqueName_2);

            System.out.println("Persisting " + new_entity2);
            jpaResource.getEm().persist(new_entity2);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            //log.assertPass("No exception thrown, as expected.");

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            // Construct a new entity instance
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=3)...");
            GeneralAnnotationOverrideEntity new_entity3 = (GeneralAnnotationOverrideEntity) constructNewEntityObject(targetEntityType);

            new_entity3.setId(3);
            new_entity3.setName("Name 3");

            System.out.println(
                               "Setting persistent attributes AnnotatedUniqueName and AnnotatedNonUniqueName to " +
                               "\"" + annotatedUniqueName_3 + "\" and \"" + annotatedNonUniqueName_3 + "\"...");
            System.out.println(
                               "Since the unique=false setting for AnnotatedNonUniqueName was overridden to true by " +
                               "the XML Mapping file, this change should NOT be allowed.");
            new_entity3.setAnnotatedUniqueName(annotatedUniqueName_3);
            new_entity3.setAnnotatedNonUniqueName(annotatedNonUniqueName_3);

            System.out.println("Persisting " + new_entity3);
            jpaResource.getEm().persist(new_entity3);

            try {
                jpaResource.getTj().commitTransaction();

                // No Exception was thrown, this has failed.
                Assert.fail("Transaction Commit completed without an Exception being thrown.");
            } catch (Throwable t) {
                // Caught an Exception, check if IllegalStateException is in the Exception Chain
                System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for PersistenceException...");
                assertExceptionIsInChain(PersistenceException.class, t);
            } finally {
                if (jpaResource.getTj().isTransactionActive()) {
                    System.out.println("Rolling back the transaction...");
                    jpaResource.getTj().rollbackTransaction();
                }
            }

            System.out.println("Ending test.");
        } catch (ClassNotFoundException | SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException
                        | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            System.out.println("AnnotationOverrideTestLogic.testAnnotationOverride004(): End");
        }
    }

    /**
     * Verify that column (String) length annotations can be overridden by their XML counterparts.
     *
     * 4 POINTS
     */
    public void testAnnotationOverride005(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                          Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("AnnotationOverrideTestLogic.testAnnotationOverride005(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        String string_5len = "Start";
        String string_20len = "Supersized Fries Now";
        String string_45len = "The quick brown fox jumped over the lazy dog.";

        // Execute Test Case
        try {
            System.out.println("AnnotationOverrideTestLogic.testAnnotationOverride005(): Begin");
            //cleanupDatabase(jpaCleanupResource, log);

            AnnotationOverrideEntityEnum targetEntityType = AnnotationOverrideEntityEnum.GeneralAnnotationOverrideEntity;

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Construct a new entity instance
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=1)...");
            GeneralAnnotationOverrideEntity new_entity = (GeneralAnnotationOverrideEntity) constructNewEntityObject(targetEntityType);

            new_entity.setId(1);
            new_entity.setName("Dr. Doom");

            System.out.println("Initializing LengthBoundString persistable attribute to \"" + string_5len + "\", which is 5 chars.");;
            System.out.println(
                               "This ensures that the entity starts with a string length lower then the original annotation\n" +
                               "and the XML Mapping File set for max string size (10 and 20 accordingly).");
            new_entity.setLengthBoundString(string_5len);

            // Set these values to avoid non-null column conflicts
            new_entity.setAnnotatedNonUniqueName("Some Value 1");
            new_entity.setAnnotatedUniqueName("Some Value 1");

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println(
                               "Fetch GeneralAnnotationOverrideEntity(id=1) and set its LengthBoundString field to \"" +
                               string_20len + "\",\n which is 20 chars in size.  This would exceed the limit of 10 set by the annotation.\n" +
                               "However, the XML Mapping File should override the max stringlength to 20, which should allow this string.");

            GeneralAnnotationOverrideEntity find_entity1 = (GeneralAnnotationOverrideEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), 1);

            jpaResource.getEm().clear();
            System.out.println("Object returned by find: " + find_entity1);

            // Verify that em.find() returned an object. (1 point)
            Assert.assertNotNull("Assert that the find operation did not return null", find_entity1);

            System.out.println("Setting LengthBoundString to \"" + string_20len + "\"...");
            find_entity1.setLengthBoundString(string_20len);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

//            log.assertPass(
//                           "The entity with the change to LengthBoundString with a string of 20 characters long was allowed to\n" +
//                           "be saved to the database.  This is expected with the XML config properly overriding the annotation.");

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println(
                               "Fetch GeneralAnnotationOverrideEntity(id=1) and set its LengthBoundString field to \"" +
                               string_45len + "\",\n which is 45 chars in size.  This would exceed the limit of both the 10 set by the annotation.\n" +
                               "and the limit of 20 characters set by the XML Mapping File.  This should result in an Exception being thrown.");

            GeneralAnnotationOverrideEntity find_entity2 = (GeneralAnnotationOverrideEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), 1);

            jpaResource.getEm().clear();
            System.out.println("Object returned by find: " + find_entity2);

            // Verify that em.find() returned an object. (1 point)
            Assert.assertNotNull("Assert that the find operation did not return null", find_entity2);

            System.out.println("Setting LengthBoundString to \"" + string_45len + "\"...");
            find_entity2.setLengthBoundString(string_45len);

            try {
                jpaResource.getTj().commitTransaction();

                // No Exception was thrown, this has failed.
                Assert.fail("Transaction Commit completed without an Exception being thrown.");
            } catch (Throwable t) {
                // Caught an Exception, check if IllegalStateException is in the Exception Chain
                System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for PersistenceException...");
                assertExceptionIsInChain(PersistenceException.class, t);
            } finally {
                if (jpaResource.getTj().isTransactionActive()) {
                    System.out.println("Rolling back the transaction...");
                    jpaResource.getTj().rollbackTransaction();
                }
            }

            System.out.println("Ending test.");
        } catch (ClassNotFoundException | SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException
                        | InvocationTargetException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            System.out.println("AnnotationOverrideTestLogic.testAnnotationOverride005(): End");
        }
    }

    public void testTemplate(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                             Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("AnnotationOverrideTestLogic.testTemplate(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityAName = (String) testExecCtx.getProperties().get("EntityAName");
        AnnotationOverrideEntityEnum targetEntityAType = AnnotationOverrideEntityEnum.resolveEntityByName(entityAName);
        if (targetEntityAType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityAName + "').  Cannot execute the test.");
            return;
        }

        String entityBName = (String) testExecCtx.getProperties().get("EntityBName");
        AnnotationOverrideEntityEnum targetEntityBType = AnnotationOverrideEntityEnum.resolveEntityByName(entityBName);
        if (targetEntityBType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-B type specified ('" + entityBName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("AnnotationOverrideTestLogic.testTemplate(): Begin");
            //cleanupDatabase(jpaCleanupResource, log);

            System.out.println("Ending test.");
        } finally {
            System.out.println("AnnotationOverrideTestLogic.testTemplate(): End");
        }
    }

//    protected void cleanupDatabase(JPAResource jpaResource) {
//        // Cleanup the database for executing the test
//        System.out.println("Cleaning up database before executing test...");
//        cleanupDatabase(jpaResource.getEm(), jpaResource.getTj(), AnnotationOverrideEntityEnum.values(), log);
//        System.out.println("Database cleanup complete.\n");
//    }
}
