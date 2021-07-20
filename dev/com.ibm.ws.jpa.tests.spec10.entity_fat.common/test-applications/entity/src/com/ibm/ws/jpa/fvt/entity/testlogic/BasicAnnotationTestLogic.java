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

package com.ibm.ws.jpa.fvt.entity.testlogic;

import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnitUtil;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.entity.entities.IAttributeConfigFieldEntity;
import com.ibm.ws.jpa.fvt.entity.support.SerializableClass;
import com.ibm.ws.jpa.fvt.entity.testlogic.enums.BasicAnnotationEntityEnum;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class BasicAnnotationTestLogic extends AbstractTestLogic {
    /*
     * 6 Points
     */
    public void testEagerFetchFunction(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("BasicAnnotationTestLogic.testEagerFetchFunction(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityName = (String) testExecCtx.getProperties().get("EntityName");
        BasicAnnotationEntityEnum targetEntityType = BasicAnnotationEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("BasicAnnotationTestLogic.testEagerFetchFunction(): Begin");
            ////cleanupDatabase(jpaCleanupResource);

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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=1)...");
            IAttributeConfigFieldEntity new_entity = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(1);

            String eagerString = "I am an eager String.";

            System.out.println("Setting the stringValEager field to \"" + eagerString + "\"...");
            new_entity.setStringValEager(eagerString);

            // Set all of the fields that cannot be null to some initialized value.
            new_entity.setStringValOptional("I'm not optional");
            new_entity.setUniqueString("I am unique.");
            new_entity.setUniqueConstraintString("I too am unique");
            SerializableClass serializableClass = new SerializableClass();
            serializableClass.setSomeInt(42);
            new_entity.setNotNullable(serializableClass);

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
            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=1)...");
            IAttributeConfigFieldEntity find_entity = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), 1);
            System.out.println("Object returned by find: " + find_entity);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity);
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

            PersistenceUnitUtil puu = jpaResource.getEm().getEntityManagerFactory().getPersistenceUnitUtil();

            System.out.println("Verifying that the stringValEager attribute of the entity returned by find() is available...");
            Assert.assertTrue("Assert that " + targetEntityType.getEntityName() +
                              " (id=1)'s 'StringValEager' field is loaded.",
                              puu.isLoaded(find_entity, "stringValEager"));
//            log.assertPass("Extra assert to maintain point count.");

            System.out.println("Ending test.");
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("BasicAnnotationTestLogic.testEagerFetchFunction(): End");
        }
    }

    /*
     * 5 Points
     */
    public void testLazyFetchFunction(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                      Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("BasicAnnotationTestLogic.testLazyFetchFunction(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityName = (String) testExecCtx.getProperties().get("EntityName");
        BasicAnnotationEntityEnum targetEntityType = BasicAnnotationEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("BasicAnnotationTestLogic.testLazyFetchFunction(): Begin");
            ////cleanupDatabase(jpaCleanupResource);

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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=1)...");
            IAttributeConfigFieldEntity new_entity = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(1);

            String lazyString = "I am a lazy String.";

            System.out.println("Setting the stringValLazy field to \"" + lazyString + "\"...");
            new_entity.setStringValLazy(lazyString);

            // Set all of the fields that cannot be null to some initialized value.
            new_entity.setStringValOptional("I'm not optional");
            new_entity.setUniqueString("I am unique.");
            new_entity.setUniqueConstraintString("I too am unique");
            SerializableClass serializableClass = new SerializableClass();
            serializableClass.setSomeInt(42);
            new_entity.setNotNullable(serializableClass);

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=1)...");
            IAttributeConfigFieldEntity find_entity = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), 1);

            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Object returned by find: " + find_entity);

            PersistenceUnitUtil puu = jpaResource.getEm().getEntityManagerFactory().getPersistenceUnitUtil();

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity);
            Assert.assertNotSame(
                                 "Assert find did not return the original object",
                                 new_entity,
                                 find_entity);
            Assert.assertFalse(
                               "Assert entity returned by find is NOT managed by the persistence context.",
                               jpaResource.getEm().contains(find_entity));
            Assert.assertEquals(
                                "Assert that the entity's id is 1",
                                find_entity.getId(),
                                1);

            // Note that the spec states that marking a persistent field LAZY means that it is a hint to the provider,
            // rather then a non-negotiable dictation.  OpenJPA honors the lazy behavior, while EclipseLink still
            // loads the String. (change introduced by task 130951)
            Assert.assertFalse("Assert that " + targetEntityType.getEntityName() +
                               " (id=1)'s 'StringValLazy' field is NOT loaded.",
                               puu.isLoaded(find_entity, "stringValLazy"));

//            if (getJPAProviderImpl(jpaResource) == JPAProviderImpl.ECLIPSELINK) {
//            	Assert.assertNotNull("With Eclipselink, assert find_entity.getStringValLazy() does not return null "
//            			+ " (to detect if a later change alters its behavior.)",
//            			find_entity.getStringValLazy());
//            } else {
//            	Assert.assertNull("Assert find_entity.getStringValLazy() returns null", find_entity.getStringValLazy());
//            }

            System.out.println("Ending test.");
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("BasicAnnotationTestLogic.testLazyFetchFunction(): End");
        }
    }

    /*
     * 1 Point
     */
    public void testNonOptionalFunction(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                        Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("BasicAnnotationTestLogic.testNonOptionalFunction(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityName = (String) testExecCtx.getProperties().get("EntityName");
        BasicAnnotationEntityEnum targetEntityType = BasicAnnotationEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("BasicAnnotationTestLogic.testNonOptionalFunction(): Begin");
            ////cleanupDatabase(jpaCleanupResource);

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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=1)...");
            IAttributeConfigFieldEntity new_entity = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(1);

            System.out.println("NOT setting any attributes on the entity...");
            // new_entity.setStringValOptional("I'm not optional");  // pasted as a comment to show how other tests avoid falling on their face.

            // Other then UniqueString, which also cannot be set null, but not by virtue of the optional field...
            new_entity.setUniqueString("I am unique.");
            new_entity.setUniqueConstraintString("I too am unique");
            SerializableClass serializableClass = new SerializableClass();
            serializableClass.setSomeInt(42);
            new_entity.setNotNullable(serializableClass);

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            try {
                System.out.println("Attempting to commit transaction, an PersistenceException should be thrown.");
                jpaResource.getTj().commitTransaction();
                Assert.fail("Transaction Commit completed without an PersistenceException being thrown.");
            } catch (Throwable t) {
                // The specification states that optional is a hint to the persistence provider whether the value of the field is
                // allowed to be null.  It does not specify an exact behavior regarding enforcement of this attribute.

                // log.assertPass
                System.out.println("The transaction commit threw an Exception, as expected.");
            } finally {
                if (jpaResource.getTj().isTransactionActive()) {
                    System.out.println("Rolling back the transaction...");
                    jpaResource.getTj().rollbackTransaction();
                }
            }

            System.out.println("Ending test.");
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("BasicAnnotationTestLogic.testNonOptionalFunction(): End");
        }
    }

    /*
     * 5 Points
     */
    public void testColumnNameOverrideFunction(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                               Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("BasicAnnotationTestLogic.testColumnNameOverrideFunction(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityName = (String) testExecCtx.getProperties().get("EntityName");
        BasicAnnotationEntityEnum targetEntityType = BasicAnnotationEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("BasicAnnotationTestLogic.testColumnNameOverrideFunction(): Begin");
            ////cleanupDatabase(jpaCleanupResource);

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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=1)...");
            IAttributeConfigFieldEntity new_entity = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(1);

            System.out.println("Setting intValColumnName to 42...");
            new_entity.setIntValColumnName(42);

            // Set all of the fields that cannot be null to some initialized value.
            new_entity.setStringValOptional("I'm not optional");
            new_entity.setUniqueString("I am unique.");
            new_entity.setUniqueConstraintString("I too am unique");
            SerializableClass serializableClass = new SerializableClass();
            serializableClass.setSomeInt(42);
            new_entity.setNotNullable(serializableClass);

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
            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=1)...");
            IAttributeConfigFieldEntity find_entity = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), 1);
            System.out.println("Object returned by find: " + find_entity);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity);
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

            Assert.assertEquals("Assert find_entity.intValColumnName() == " + new_entity.getIntValColumnName() + ").",
                                new_entity.getIntValColumnName(),
                                find_entity.getIntValColumnName());

            jpaResource.getTj().rollbackTransaction();

            System.out.println("Ending test.");
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("BasicAnnotationTestLogic.testColumnNameOverrideFunction(): End");
        }
    }

    /*
     * 6 Points
     */
    public void testNullableFunction(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                     Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("BasicAnnotationTestLogic.testNullableFunction(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityName = (String) testExecCtx.getProperties().get("EntityName");
        BasicAnnotationEntityEnum targetEntityType = BasicAnnotationEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("BasicAnnotationTestLogic.testNullableFunction(): Begin");
            ////cleanupDatabase(jpaCleanupResource);

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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=1)...");
            IAttributeConfigFieldEntity new_entity = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(1);

            System.out.println("Setting stringValColumnNullable field...");
            //String stringValColumnNullable = "Some String";
            SerializableClass serializableClass = new SerializableClass();
            serializableClass.setSomeInt(42);
            new_entity.setNotNullable(serializableClass);

            // Set all of the fields marked non-optional to some initialized value.
            System.out.println("Initializing non-optional attributes...");
            new_entity.setStringValOptional("I'm not optional");
            new_entity.setUniqueString("I am unique.");
            new_entity.setUniqueConstraintString("I too am unique");

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
            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=1)...");
            IAttributeConfigFieldEntity find_entity = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), 1);
            System.out.println("Object returned by find: " + find_entity);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity);
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

            Assert.assertEquals("Assert find_entity.intValColumnName() == " + new_entity.getIntValColumnName() + ").",
                                new_entity.getIntValColumnName(),
                                find_entity.getIntValColumnName());

            System.out.println("Going to attempt to set stringValColumnNullable to null, should fail.");
            find_entity.setNotNullable(null);

            try {
                System.out.println("Attempting to commit transaction, a PersistenceException should be thrown.");
                jpaResource.getTj().commitTransaction();
                Assert.fail("Transaction Commit completed without an Exception being thrown.");
            } catch (Throwable t) {
                // Caught an Exception, check if PersistenceException is in the Exception Chain
                System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for PersistenceException...");
                assertExceptionIsInChain(PersistenceException.class, t);
            } finally {
                if (jpaResource.getTj().isTransactionActive()) {
                    System.out.println("Rolling back the transaction...");
                    jpaResource.getTj().rollbackTransaction();
                }
            }
            System.out.println("Ending test.");
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("BasicAnnotationTestLogic.testNullableFunction(): End");
        }
    }

    /*
     * 9 Points
     */
    public void testUniqueFunction(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                   Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("BasicAnnotationTestLogic.testUniqueFunction(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityName = (String) testExecCtx.getProperties().get("EntityName");
        BasicAnnotationEntityEnum targetEntityType = BasicAnnotationEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("BasicAnnotationTestLogic.testUniqueFunction(): Begin");
            ////cleanupDatabase(jpaCleanupResource);

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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=1)...");
            IAttributeConfigFieldEntity new_entity1 = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
            new_entity1.setId(1);

            new_entity1.setUniqueString("A Unique String");

            // Set all of the fields marked non-optional to some initialized value.
            new_entity1.setStringValOptional("I'm not optional");
            new_entity1.setUniqueConstraintString("I too am unique");
            SerializableClass serializableClass = new SerializableClass();
            serializableClass.setSomeInt(42);
            new_entity1.setNotNullable(serializableClass);

            System.out.println("Persisting " + new_entity1);
            jpaResource.getEm().persist(new_entity1);

            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=2)...");
            IAttributeConfigFieldEntity new_entity2 = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
            new_entity2.setId(2);

            new_entity2.setUniqueString("Another Unique String");
            // Set all of the fields marked non-optional to some initialized value.
            new_entity2.setStringValOptional("I'm not optional");
            new_entity2.setUniqueConstraintString("I am unique as well");
            SerializableClass serializableClass2 = new SerializableClass();
            serializableClass2.setSomeInt(42);
            new_entity2.setNotNullable(serializableClass2);

            System.out.println("Persisting " + new_entity2);
            jpaResource.getEm().persist(new_entity2);

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
            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=1)...");
            IAttributeConfigFieldEntity find_entity1 = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), 1);
            System.out.println("Object returned by find: " + find_entity1);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity1);
            Assert.assertNotSame(
                                 "Assert find did not return the original object",
                                 new_entity1,
                                 find_entity1);
            Assert.assertTrue(
                              "Assert entity returned by find is managed by the persistence context.",
                              jpaResource.getEm().contains(find_entity1));
            Assert.assertEquals(
                                "Assert that the entity's id is 1",
                                find_entity1.getId(),
                                1);

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=2)...");
            IAttributeConfigFieldEntity find_entity2 = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), 2);
            System.out.println("Object returned by find: " + find_entity2);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity2);
            Assert.assertNotSame(
                                 "Assert find did not return the original object",
                                 new_entity2,
                                 find_entity2);
            Assert.assertTrue(
                              "Assert entity returned by find is managed by the persistence context.",
                              jpaResource.getEm().contains(find_entity2));
            Assert.assertEquals(
                                "Assert that the entity's id is 2",
                                find_entity2.getId(),
                                2);

            System.out.println("Editing IAttributeConfigFieldEntity(id=2) so its uniqueString field equals IAttributeConfigFieldEntity(id=1)...");
            find_entity2.setUniqueString("A Unique String");

            try {
                System.out.println("Attempting to commit transaction, a PersistenceException should be thrown.");
                jpaResource.getTj().commitTransaction();
                Assert.fail("Transaction Commit completed without an Exception being thrown.");
            } catch (Throwable t) {
                // Caught an Exception, check if PersistenceException is in the Exception Chain
                System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for PersistenceException...");
                assertExceptionIsInChain(PersistenceException.class, t);
            } finally {
                if (jpaResource.getTj().isTransactionActive()) {
                    System.out.println("Rolling back the transaction...");
                    jpaResource.getTj().rollbackTransaction();
                }
            }
            System.out.println("Ending test.");
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("BasicAnnotationTestLogic.testUniqueFunction(): End");
        }
    }

    /*
     * 5 Points
     */
    public void testAttributeTableFunction(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                           Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("BasicAnnotationTestLogic.testAttributeTableFunction(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityName = (String) testExecCtx.getProperties().get("EntityName");
        BasicAnnotationEntityEnum targetEntityType = BasicAnnotationEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("BasicAnnotationTestLogic.testAttributeTableFunction(): Begin");
            ////cleanupDatabase(jpaCleanupResource);

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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=1)...");
            IAttributeConfigFieldEntity new_entity1 = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
            new_entity1.setId(1);

            System.out.println("Setting intValColumnTable to 42...");
            new_entity1.setIntValColumnTable(42);

            // Set all of the fields marked non-optional to some initialized value.
            new_entity1.setStringValOptional("I'm not optional");
            new_entity1.setUniqueString("I am unique.");
            new_entity1.setUniqueConstraintString("I too am unique");
            SerializableClass serializableClass = new SerializableClass();
            serializableClass.setSomeInt(42);
            new_entity1.setNotNullable(serializableClass);

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
            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=1)...");
            IAttributeConfigFieldEntity find_entity1 = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), 1);
            System.out.println("Object returned by find: " + find_entity1);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity1);
            Assert.assertNotSame(
                                 "Assert find did not return the original object",
                                 new_entity1,
                                 find_entity1);
            Assert.assertTrue(
                              "Assert entity returned by find is managed by the persistence context.",
                              jpaResource.getEm().contains(find_entity1));
            Assert.assertEquals(
                                "Assert that the entity's id is 1",
                                find_entity1.getId(),
                                1);
            Assert.assertEquals("Assert intValColumnTable is 42", 42, find_entity1.getIntValColumnTable());

            System.out.println("Ending test.");
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("BasicAnnotationTestLogic.testAttributeTableFunction(): End");
        }
    }

    /*
     * 6 Points
     */
    public void testColumnLengthFunction(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                         Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("BasicAnnotationTestLogic.testColumnLengthFunction(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityName = (String) testExecCtx.getProperties().get("EntityName");
        BasicAnnotationEntityEnum targetEntityType = BasicAnnotationEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("BasicAnnotationTestLogic.testColumnLengthFunction(): Begin");
            ////cleanupDatabase(jpaCleanupResource);

            {
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
                System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=1)...");
                IAttributeConfigFieldEntity new_entity1 = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
                new_entity1.setId(1);

                new_entity1.setStringValColumnLength("I am far too long for what this entity will support");

                // Set all of the fields marked non-optional to some initialized value.
                new_entity1.setStringValOptional("I'm not optional");
                new_entity1.setUniqueConstraintString("I too am unique");
                SerializableClass serializableClass = new SerializableClass();
                serializableClass.setSomeInt(42);
                new_entity1.setNotNullable(serializableClass);

                System.out.println("Persisting " + new_entity1);
                jpaResource.getEm().persist(new_entity1);

                try {
                    System.out.println("Attempting to commit transaction, a PersistenceException should be thrown.");
                    jpaResource.getTj().commitTransaction();
                    Assert.fail("Transaction Commit completed without an Exception being thrown.");
                } catch (Throwable t) {
                    // Caught an Exception, check if PersistenceException is in the Exception Chain
                    System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for PersistenceException...");
                    assertExceptionIsInChain(PersistenceException.class, t);
                } finally {
                    if (jpaResource.getTj().isTransactionActive()) {
                        System.out.println("Rolling back the transaction...");
                        jpaResource.getTj().rollbackTransaction();
                    }
                }
                // Clear persistence context
                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();
            }

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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=1)...");
            IAttributeConfigFieldEntity new_entity1 = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
            new_entity1.setId(1);

            new_entity1.setStringValColumnLength("OK Size");

            // Set all of the fields marked non-optional to some initialized value.
            new_entity1.setStringValOptional("I'm not optional");
            new_entity1.setUniqueString("I am unique.");
            new_entity1.setUniqueConstraintString("I too am unique");
            SerializableClass serializableClass = new SerializableClass();
            serializableClass.setSomeInt(42);
            new_entity1.setNotNullable(serializableClass);

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
            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=1)...");
            IAttributeConfigFieldEntity find_entity1 = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), 1);
            System.out.println("Object returned by find: " + find_entity1);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity1);
            Assert.assertNotSame(
                                 "Assert find did not return the original object",
                                 new_entity1,
                                 find_entity1);
            Assert.assertTrue(
                              "Assert entity returned by find is managed by the persistence context.",
                              jpaResource.getEm().contains(find_entity1));
            Assert.assertEquals(
                                "Assert that the entity's id is 1",
                                find_entity1.getId(),
                                1);

            find_entity1.setStringValColumnLength("I am far too long for what this entity will support");

            try {
                System.out.println("Attempting to commit transaction, a PersistenceException should be thrown.");
                jpaResource.getTj().commitTransaction();
                Assert.fail("Transaction Commit completed without an Exception being thrown.");
            } catch (Throwable t) {
                // Caught an Exception, check if PersistenceException is in the Exception Chain
                System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for PersistenceException...");
                assertExceptionIsInChain(PersistenceException.class, t);
            } finally {
                if (jpaResource.getTj().isTransactionActive()) {
                    System.out.println("Rolling back the transaction...");
                    jpaResource.getTj().rollbackTransaction();
                }
            }
            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Ending test.");
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("BasicAnnotationTestLogic.testColumnLengthFunction(): End");
        }
    }

    /*
     * 9 Points
     */
    public void testUniqueConstraintsFunction(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                              Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("BasicAnnotationTestLogic.testUniqueConstraintsFunction(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityName = (String) testExecCtx.getProperties().get("EntityName");
        BasicAnnotationEntityEnum targetEntityType = BasicAnnotationEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("BasicAnnotationTestLogic.testUniqueConstraintsFunction(): Begin");
            ////cleanupDatabase(jpaCleanupResource);

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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=1)...");
            IAttributeConfigFieldEntity new_entity1 = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
            new_entity1.setId(1);

            new_entity1.setUniqueConstraintString("A Unique String");

            // Set all of the fields marked non-optional to some initialized value.
            System.out.println("Initializing other non-optional attributes on IAttributeConfigFieldEntity(id=1)...");
            new_entity1.setStringValOptional("I'm not optional");
            new_entity1.setUniqueString("I too am unique");
            SerializableClass serializableClass = new SerializableClass();
            serializableClass.setSomeInt(42);
            new_entity1.setNotNullable(serializableClass);

            System.out.println("Persisting " + new_entity1);
            jpaResource.getEm().persist(new_entity1);

            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=2)...");
            IAttributeConfigFieldEntity new_entity2 = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
            new_entity2.setId(2);

            new_entity2.setUniqueConstraintString("Another Unique String");

            // Set all of the fields marked non-optional to some initialized value.
            System.out.println("Initializing other non-optional attributes on IAttributeConfigFieldEntity(id=1)...");
            new_entity2.setStringValOptional("I'm not optional");
            new_entity2.setUniqueString("I am unique as well");
            SerializableClass serializableClass2 = new SerializableClass();
            serializableClass2.setSomeInt(42);
            new_entity2.setNotNullable(serializableClass2);

            System.out.println("Persisting " + new_entity2);
            jpaResource.getEm().persist(new_entity2);

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
            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=1)...");
            IAttributeConfigFieldEntity find_entity1 = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), 1);
            System.out.println("Object returned by find: " + find_entity1);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity1);
            Assert.assertNotSame(
                                 "Assert find did not return the original object",
                                 new_entity1,
                                 find_entity1);
            Assert.assertTrue(
                              "Assert entity returned by find is managed by the persistence context.",
                              jpaResource.getEm().contains(find_entity1));
            Assert.assertEquals(
                                "Assert that the entity's id is 1",
                                find_entity1.getId(),
                                1);

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=2)...");
            IAttributeConfigFieldEntity find_entity2 = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), 2);
            System.out.println("Object returned by find: " + find_entity2);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity2);
            Assert.assertNotSame(
                                 "Assert find did not return the original object",
                                 new_entity2,
                                 find_entity2);
            Assert.assertTrue(
                              "Assert entity returned by find is managed by the persistence context.",
                              jpaResource.getEm().contains(find_entity2));
            Assert.assertEquals(
                                "Assert that the entity's id is 2",
                                find_entity2.getId(),
                                2);

            System.out.println("Editing IAttributeConfigFieldEntity(id=2) so its uniqueConstraintString field equals IAttributeConfigFieldEntity(id=1)...");
            find_entity2.setUniqueConstraintString("A Unique String");

            try {
                System.out.println("Attempting to commit transaction, a PersistenceException should be thrown.");
                jpaResource.getTj().commitTransaction();
                Assert.fail("Transaction Commit completed without an Exception being thrown.");
            } catch (Throwable t) {
                // Caught an Exception, check if PersistenceException is in the Exception Chain
                System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for PersistenceException...");
                assertExceptionIsInChain(PersistenceException.class, t);
            } finally {
                if (jpaResource.getTj().isTransactionActive()) {
                    System.out.println("Rolling back the transaction...");
                    jpaResource.getTj().rollbackTransaction();
                }
            }

            System.out.println("Ending test.");
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("BasicAnnotationTestLogic.testUniqueConstraintsFunction(): End");
        }
    }

    public void testTemplate(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                             Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("BasicAnnotationTestLogic.testTemplate(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
//        JPAResource jpaCleanupResource = testExecResources.getJpaResourceMap().get("cleanup");
//        if (jpaCleanupResource == null) {
//            Assert.fail("Missing JPAResource 'cleanup').  Cannot execute the test.");
//            return;
//        }
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        // Fetch target entity type from test parameters
        String entityName = (String) testExecCtx.getProperties().get("EntityName");
        BasicAnnotationEntityEnum targetEntityType = BasicAnnotationEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("BasicAnnotationTestLogic.testTemplate(): Begin");
            ////cleanupDatabase(jpaCleanupResource);

            System.out.println("Ending test.");
        } finally {
            System.out.println("BasicAnnotationTestLogic.testTemplate(): End");
        }
    }

//    protected void cleanupDatabase(JPAResource jpaResource) {
//        // Cleanup the database for executing the test
//        System.out.println("Cleaning up database before executing test...");
//        cleanupDatabase(jpaResource.getEm(), jpaResource.getTj(), BasicAnnotationEntityEnum.values());
//        System.out.println("Database cleanup complete.\n");
//    }
}
