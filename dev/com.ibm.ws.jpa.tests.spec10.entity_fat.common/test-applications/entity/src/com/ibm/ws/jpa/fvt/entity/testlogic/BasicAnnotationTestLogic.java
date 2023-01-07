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

package com.ibm.ws.jpa.fvt.entity.testlogic;

import java.io.Serializable;
import java.util.Map;

import javax.persistence.PersistenceException;
import javax.persistence.PersistenceUnitUtil;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.entity.entities.IAttributeConfigFieldEntity;
import com.ibm.ws.jpa.fvt.entity.support.SerializableClass;
import com.ibm.ws.jpa.fvt.entity.testlogic.enums.BasicAnnotationEntityEnum;
import com.ibm.ws.testtooling.database.DatabaseVendor;
import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;
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

        int id = 1;

        // Execute Test Case
        try {
            System.out.println("BasicAnnotationTestLogic.testEagerFetchFunction(): Begin");

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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity new_entity = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(id);

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
            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity find_entity = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_entity);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity);
            Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity));
            Assert.assertEquals("Assert that the entity's id is " + id, find_entity.getId(), id);

            PersistenceUnitUtil puu = jpaResource.getEm().getEntityManagerFactory().getPersistenceUnitUtil();

            System.out.println("Verifying that the stringValEager attribute of the entity returned by find() is available...");
            Assert.assertTrue("Assert that " + targetEntityType.getEntityName() + " (id=" + id + ")'s 'StringValEager' field is loaded.",
                              puu.isLoaded(find_entity, "stringValEager"));

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

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity find_remove_entity = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_remove_entity);

            Assert.assertNotNull("Assert that the find operation did not return null", find_remove_entity);

            System.out.println("Removing entity...");
            jpaResource.getEm().remove(find_remove_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

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

        // Process Test Properties
        final Map<String, Serializable> testProps = testExecCtx.getProperties();
        if (testProps != null) {
            for (String key : testProps.keySet()) {
                System.out.println("Test Property: " + key + " = " + testProps.get(key));
            }
        }

        JPAPersistenceProvider provider = JPAPersistenceProvider.resolveJPAPersistenceProvider(jpaResource);

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));

        final boolean isOracle = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.ORACLE);
        int id = 1;

        // Execute Test Case
        try {
            System.out.println("BasicAnnotationTestLogic.testLazyFetchFunction(): Begin");

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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity new_entity = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(id);

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

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity find_entity = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);

            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Object returned by find: " + find_entity);

            PersistenceUnitUtil puu = jpaResource.getEm().getEntityManagerFactory().getPersistenceUnitUtil();

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity);
            Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity);
            Assert.assertFalse("Assert entity returned by find is NOT managed by the persistence context.", jpaResource.getEm().contains(find_entity));
            Assert.assertEquals("Assert that the entity's id is " + id, find_entity.getId(), id);

            // Note that the spec states that marking a persistent field LAZY means that it is a hint to the provider,
            // rather then a non-negotiable dictation.
            // OpenJPA honors the lazy behavior
            // EclipseLink still loads the String. (change introduced by task 130951)
            switch (provider) {
                case ECLIPSELINK:
                    Assert.assertTrue(targetEntityType.getEntityName() + " (id=" + id + ")'s 'StringValLazy' field is loaded.",
                                      !puu.isLoaded(find_entity, "stringValLazy"));
                    break;
                case OPENJPA:
                    Assert.assertTrue(targetEntityType.getEntityName() + " (id=" + id + ")'s 'StringValLazy' field is loaded.",
                                      !puu.isLoaded(find_entity, "stringValLazy"));
                    break;
                case HIBERNATE:
                    Assert.assertTrue(targetEntityType.getEntityName() + " (id=" + id + ")'s 'StringValLazy' field is NOT loaded.",
                                      puu.isLoaded(find_entity, "stringValLazy"));
                    break;
            }

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity find_remove_entity2 = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_remove_entity2);

            Assert.assertNotNull("Assert that the find operation did not return null", find_remove_entity2);

            System.out.println("Removing entity...");
            jpaResource.getEm().remove(find_remove_entity2);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

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

        JPAPersistenceProvider provider = JPAPersistenceProvider.resolveJPAPersistenceProvider(jpaResource);

        int id = 1;
        boolean onPersist = false;
        boolean onCommit = false;

        // Execute Test Case
        try {
            System.out.println("BasicAnnotationTestLogic.testNonOptionalFunction(): Begin");

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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity new_entity = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(id);

            System.out.println("NOT setting 'stringValOptional' attribute on the entity...");
            // stringValOptional is set to 'optional = false' and some persistence providers throw an exception if not set
            // new_entity.setStringValOptional("I'm not optional");

            // These fields are set to 'nullable = false', so they cannot be left unset
            new_entity.setUniqueString("I am unique.");
            new_entity.setUniqueConstraintString("I too am unique");
            SerializableClass serializableClass = new SerializableClass();
            serializableClass.setSomeInt(42);
            new_entity.setNotNullable(serializableClass);

            try {
                System.out.println("Persisting " + new_entity);
                onPersist = true;
                jpaResource.getEm().persist(new_entity);
                onPersist = false;

                System.out.println("Attempting to commit transaction");
                onCommit = true;
                jpaResource.getTj().commitTransaction();

                /*
                 * The specification states that '@Basic(optional = false)' is a hint to the persistence provider
                 * whether the value of the field is allowed to be null. It does not specify an exact behavior
                 * regarding enforcement of this attribute.
                 *
                 * TODO: EclipseLink does not throw an exception and seems to ignore '@Basic(optional = false)'
                 * This may be a bug in EclipseLink and should be investigated.
                 */
                if (JPAPersistenceProvider.ECLIPSELINK.equals(provider)) {
                    // Cleanup since there was no failure
                    System.out.println("Beginning new transaction...");
                    jpaResource.getTj().beginTransaction();
                    if (jpaResource.getTj().isApplicationManaged()) {
                        System.out.println("Joining entitymanager to JTA transaction...");
                        jpaResource.getEm().joinTransaction();
                    }

                    System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
                    IAttributeConfigFieldEntity find_remove_entity2 = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
                    System.out.println("Object returned by find: " + find_remove_entity2);

                    Assert.assertNotNull("Assert that the find operation did not return null", find_remove_entity2);

                    System.out.println("Removing entity...");
                    jpaResource.getEm().remove(find_remove_entity2);

                    System.out.println("Committing transaction...");
                    jpaResource.getTj().commitTransaction();
                } else {
                    Assert.fail("Transaction Commit completed without a PersistenceException being thrown.");
                }
            } catch (Throwable t) {
                System.out.println("The transaction commit threw exception (" + t + ") for provider " + provider.name());
                switch (provider) {
                    case OPENJPA:
                        // OpenJPA fails on commit
                        Assert.assertTrue("The exception was not thrown on commit", onCommit);

                        // Caught an Exception, check if IllegalStateException is in the Exception Chain
                        System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for IllegalStateException...");
                        Throwable root = containsCauseByException(IllegalStateException.class, t);
                        Assert.assertNotNull("Throwable stack did not contain " + IllegalStateException.class, root);
                        break;
                    case HIBERNATE:
                        // Hibernate fails on persist, rather than commit
                        Assert.assertTrue("The exception was not thrown on persist", onPersist);

                        // Caught an Exception, check if PersistenceException is in the Exception Chain
                        System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for PersistenceException...");
                        Throwable root2 = containsCauseByException(PersistenceException.class, t);
                        Assert.assertNotNull("Throwable stack did not contain " + PersistenceException.class, root2);
                        break;
                    case ECLIPSELINK:
                        // EclipseLink should not have failed
                    default:
                        throw t;
                }
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

        int id = 1;

        // Execute Test Case
        try {
            System.out.println("BasicAnnotationTestLogic.testColumnNameOverrideFunction(): Begin");

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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity new_entity = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(id);

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
            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity find_entity = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_entity);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity);
            Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity));
            Assert.assertEquals("Assert that the entity's id is " + id, find_entity.getId(), id);

            Assert.assertEquals("Assert find_entity.intValColumnName() == " + new_entity.getIntValColumnName() + ").",
                                new_entity.getIntValColumnName(), find_entity.getIntValColumnName());

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

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity find_remove_entity2 = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_remove_entity2);

            Assert.assertNotNull("Assert that the find operation did not return null", find_remove_entity2);

            System.out.println("Removing entity...");
            jpaResource.getEm().remove(find_remove_entity2);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

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

        int id = 1;

        // Execute Test Case
        try {
            System.out.println("BasicAnnotationTestLogic.testNullableFunction(): Begin");

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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity new_entity = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(id);

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
            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity find_entity = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_entity);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity);
            Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity));
            Assert.assertEquals("Assert that the entity's id is " + id, find_entity.getId(), id);

            Assert.assertEquals("Assert find_entity.intValColumnName() == " + new_entity.getIntValColumnName() + ").",
                                new_entity.getIntValColumnName(), find_entity.getIntValColumnName());

            System.out.println("Going to attempt to set stringValColumnNullable to null, should fail.");
            find_entity.setNotNullable(null);

            try {
                System.out.println("Attempting to commit transaction, a PersistenceException should be thrown.");
                jpaResource.getTj().commitTransaction();
                Assert.fail("Transaction Commit completed without an Exception being thrown.");
            } catch (AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Caught an Exception, check if PersistenceException is in the Exception Chain
                System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for PersistenceException...");
                Throwable root = containsCauseByException(PersistenceException.class, t);
                Assert.assertNotNull("Throwable stack did not contain " + PersistenceException.class, root);
            } finally {
                if (jpaResource.getTj().isTransactionActive()) {
                    System.out.println("Rolling back the transaction...");
                    jpaResource.getTj().rollbackTransaction();
                }
            }

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity find_remove_entity2 = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_remove_entity2);

            Assert.assertNotNull("Assert that the find operation did not return null", find_remove_entity2);

            System.out.println("Removing entity...");
            jpaResource.getEm().remove(find_remove_entity2);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

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

        int id = 1;
        int id2 = 2;

        // Execute Test Case
        try {
            System.out.println("BasicAnnotationTestLogic.testUniqueFunction(): Begin");

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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity new_entity1 = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
            new_entity1.setId(id);

            new_entity1.setUniqueString("A Unique String");

            // Set all of the fields marked non-optional to some initialized value.
            new_entity1.setStringValOptional("I'm not optional");
            new_entity1.setUniqueConstraintString("I too am unique");
            SerializableClass serializableClass = new SerializableClass();
            serializableClass.setSomeInt(42);
            new_entity1.setNotNullable(serializableClass);

            System.out.println("Persisting " + new_entity1);
            jpaResource.getEm().persist(new_entity1);

            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=" + id2 + ")...");
            IAttributeConfigFieldEntity new_entity2 = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
            new_entity2.setId(id2);

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
            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity find_entity1 = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_entity1);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity1);
            Assert.assertNotSame("Assert find did not return the original object", new_entity1, find_entity1);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity1));
            Assert.assertEquals("Assert that the entity's id is " + id, find_entity1.getId(), id);

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id2 + ")...");
            IAttributeConfigFieldEntity find_entity2 = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id2);
            System.out.println("Object returned by find: " + find_entity2);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity2);
            Assert.assertNotSame("Assert find did not return the original object", new_entity2, find_entity2);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity2));
            Assert.assertEquals("Assert that the entity's id is " + id2, find_entity2.getId(), id2);

            System.out.println("Editing IAttributeConfigFieldEntity(id=" + id2 + ") so its uniqueString field equals IAttributeConfigFieldEntity(id=" + id + ")...");
            find_entity2.setUniqueString("A Unique String");

            try {
                System.out.println("Attempting to commit transaction, a PersistenceException should be thrown.");
                jpaResource.getTj().commitTransaction();
                Assert.fail("Transaction Commit completed without an Exception being thrown.");
            } catch (AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Caught an Exception, check if PersistenceException is in the Exception Chain
                System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for PersistenceException...");
                Throwable root = containsCauseByException(PersistenceException.class, t);
                Assert.assertNotNull("Throwable stack did not contain " + PersistenceException.class, root);
            } finally {
                if (jpaResource.getTj().isTransactionActive()) {
                    System.out.println("Rolling back the transaction...");
                    jpaResource.getTj().rollbackTransaction();
                }
            }

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity find_remove_entity1 = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_remove_entity1);

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity find_remove_entity2 = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id2);
            System.out.println("Object returned by find: " + find_remove_entity2);

            Assert.assertNotNull("Assert that the find operation did not return null", find_remove_entity1);
            Assert.assertNotNull("Assert that the find operation did not return null", find_remove_entity2);

            System.out.println("Removing entity...");
            jpaResource.getEm().remove(find_remove_entity1);

            System.out.println("Removing entity...");
            jpaResource.getEm().remove(find_remove_entity2);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

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

        int id = 1;

        // Execute Test Case
        try {
            System.out.println("BasicAnnotationTestLogic.testAttributeTableFunction(): Begin");

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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity new_entity1 = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
            new_entity1.setId(id);

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
            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity find_entity1 = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_entity1);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity1);
            Assert.assertNotSame("Assert find did not return the original object", new_entity1, find_entity1);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity1));
            Assert.assertEquals("Assert that the entity's id is " + id, find_entity1.getId(), id);
            Assert.assertEquals("Assert intValColumnTable is 42", 42, find_entity1.getIntValColumnTable());

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

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity find_entity2 = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_entity2);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity2);

            System.out.println("Removing entity...");
            jpaResource.getEm().remove(find_entity2);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

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

        JPAPersistenceProvider provider = JPAPersistenceProvider.resolveJPAPersistenceProvider(jpaResource);

        int id = 1;
        boolean onPersist = false;
        boolean onCommit = false;

        // Execute Test Case
        try {
            System.out.println("BasicAnnotationTestLogic.testColumnLengthFunction(): Begin");

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
                System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=" + id + ")...");
                IAttributeConfigFieldEntity new_entity1 = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
                new_entity1.setId(id);

                new_entity1.setStringValColumnLength("I am far too long for what this entity will support");

                // Set all of the fields marked non-optional to some initialized value.
                new_entity1.setStringValOptional("I'm not optional");
                new_entity1.setUniqueConstraintString("I too am unique");
                SerializableClass serializableClass = new SerializableClass();
                serializableClass.setSomeInt(42);
                new_entity1.setNotNullable(serializableClass);

                try {
                    System.out.println("Persisting " + new_entity1);
                    onPersist = true;
                    jpaResource.getEm().persist(new_entity1);
                    onPersist = false;

                    System.out.println("Attempting to commit transaction, a PersistenceException should be thrown.");
                    onCommit = true;
                    jpaResource.getTj().commitTransaction();
                    Assert.fail("Transaction Commit completed without an Exception being thrown.");
                } catch (AssertionError ae) {
                    throw ae;
                } catch (Throwable t) {
                    switch (provider) {
                        case ECLIPSELINK:
                        case OPENJPA:
                            Assert.assertTrue("The exception was not thrown on commit", onCommit);
                            break;
                        case HIBERNATE:
                            // Hibernate fails on persist; throwing PersistenceException (org.hibernate.PropertyValueException) as the cause
                            Assert.assertTrue("The exception was not thrown on persist", onPersist);
                            break;
                    }
                    // Caught an Exception, check if PersistenceException is in the Exception Chain
                    System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for PersistenceException...");
                    Throwable root = containsCauseByException(PersistenceException.class, t);
                    Assert.assertNotNull("Throwable stack did not contain " + PersistenceException.class, root);
                } finally {
                    if (jpaResource.getTj().isTransactionActive()) {
                        System.out.println("Rolling back the transaction...");
                        jpaResource.getTj().rollbackTransaction();
                    }
                }
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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity new_entity1 = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
            new_entity1.setId(id);

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
            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity find_entity1 = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_entity1);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity1);
            Assert.assertNotSame("Assert find did not return the original object", new_entity1, find_entity1);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity1));
            Assert.assertEquals("Assert that the entity's id is " + id, find_entity1.getId(), id);

            find_entity1.setStringValColumnLength("I am far too long for what this entity will support");

            try {
                System.out.println("Attempting to commit transaction, a PersistenceException should be thrown.");
                jpaResource.getTj().commitTransaction();
                Assert.fail("Transaction Commit completed without an Exception being thrown.");
            } catch (AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Caught an Exception, check if PersistenceException is in the Exception Chain
                System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for PersistenceException...");
                Throwable root = containsCauseByException(PersistenceException.class, t);
                Assert.assertNotNull("Throwable stack did not contain " + PersistenceException.class, root);
            } finally {
                if (jpaResource.getTj().isTransactionActive()) {
                    System.out.println("Rolling back the transaction...");
                    jpaResource.getTj().rollbackTransaction();
                }
            }

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity find_entity2 = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_entity2);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity2);

            System.out.println("Removing entity...");
            jpaResource.getEm().remove(find_entity2);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

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

        int id = 1;
        int id2 = 2;

        // Execute Test Case
        try {
            System.out.println("BasicAnnotationTestLogic.testUniqueConstraintsFunction(): Begin");

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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity new_entity1 = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
            new_entity1.setId(id);

            new_entity1.setUniqueConstraintString("A Unique String");

            // Set all of the fields marked non-optional to some initialized value.
            System.out.println("Initializing other non-optional attributes on IAttributeConfigFieldEntity(id=" + id + ")...");
            new_entity1.setStringValOptional("I'm not optional");
            new_entity1.setUniqueString("I too am unique");
            SerializableClass serializableClass = new SerializableClass();
            serializableClass.setSomeInt(42);
            new_entity1.setNotNullable(serializableClass);

            System.out.println("Persisting " + new_entity1);
            jpaResource.getEm().persist(new_entity1);

            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + " (id=" + id2 + ")...");
            IAttributeConfigFieldEntity new_entity2 = (IAttributeConfigFieldEntity) constructNewEntityObject(targetEntityType);
            new_entity2.setId(id2);

            new_entity2.setUniqueConstraintString("Another Unique String");

            // Set all of the fields marked non-optional to some initialized value.
            System.out.println("Initializing other non-optional attributes on IAttributeConfigFieldEntity(id=" + id + ")...");
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
            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity find_entity1 = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_entity1);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity1);
            Assert.assertNotSame("Assert find did not return the original object", new_entity1, find_entity1);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity1));
            Assert.assertEquals("Assert that the entity's id is " + id, find_entity1.getId(), id);

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id2 + ")...");
            IAttributeConfigFieldEntity find_entity2 = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id2);
            System.out.println("Object returned by find: " + find_entity2);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity2);
            Assert.assertNotSame("Assert find did not return the original object", new_entity2, find_entity2);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity2));
            Assert.assertEquals("Assert that the entity's id is " + id2, find_entity2.getId(), id2);

            System.out.println("Editing IAttributeConfigFieldEntity(id=" + id2 + ") so its uniqueConstraintString field equals IAttributeConfigFieldEntity(id=" + id + ")...");
            find_entity2.setUniqueConstraintString("A Unique String");

            try {
                System.out.println("Attempting to commit transaction, a PersistenceException should be thrown.");
                jpaResource.getTj().commitTransaction();

                Assert.fail("Transaction Commit completed without an Exception being thrown.");
            } catch (AssertionError ae) {
                throw ae;
            } catch (Throwable t) {
                // Caught an Exception, check if PersistenceException is in the Exception Chain
                System.out.println("Transaction commit did throw an Exception.  Searching Exception Chain for PersistenceException...");
                Throwable root = containsCauseByException(PersistenceException.class, t);
                Assert.assertNotNull("Throwable stack did not contain " + PersistenceException.class, root);
            } finally {
                if (jpaResource.getTj().isTransactionActive()) {
                    System.out.println("Rolling back the transaction...");
                    jpaResource.getTj().rollbackTransaction();
                }
            }

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity find_remove_entity1 = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_remove_entity1);

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IAttributeConfigFieldEntity find_remove_entity2 = (IAttributeConfigFieldEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id2);
            System.out.println("Object returned by find: " + find_remove_entity2);

            Assert.assertNotNull("Assert that the find operation did not return null", find_remove_entity1);
            Assert.assertNotNull("Assert that the find operation did not return null", find_remove_entity2);

            System.out.println("Removing entity...");
            jpaResource.getEm().remove(find_remove_entity1);

            System.out.println("Removing entity...");
            jpaResource.getEm().remove(find_remove_entity2);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

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
            System.out.println("BasicAnnotationTestLogic.testUniqueConstraintsFunction(): End");
        }
    }
}
