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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.text.DateFormat;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.entity.entities.IDatatypeSupportTestEntity;
import com.ibm.ws.jpa.fvt.entity.support.Constants;
import com.ibm.ws.jpa.fvt.entity.support.SerializableClass;
import com.ibm.ws.jpa.fvt.entity.testlogic.enums.DatatypeSupportEntityEnum;
import com.ibm.ws.testtooling.database.DatabaseVendor;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class SerializableTestLogic extends AbstractTestLogic {

    /*
     * 12 Points
     */
    public void testJavaPrimitiveSerializableSupport(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                     Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("DatatypeSupportTestLogic.testJavaPrimitiveSerializableSupport(): Missing context and/or resources.  Cannot execute the test.");
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
        DatatypeSupportEntityEnum targetEntityType = DatatypeSupportEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityName + "').  Cannot execute the test.");
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

        final boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.POSTGRES);
        int id = 1;

        // Execute Test Case
        try {
            System.out.println("DatatypeSupportTestLogic.testJavaPrimitiveSerializableSupport(): Begin");

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
            IDatatypeSupportTestEntity new_entity = (IDatatypeSupportTestEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(id);

            if (isPostgres) {
                new_entity.setCharacterWrapperAttrDefault(' ');
            }

            // byteAttrDefault
            System.out.println("Setting byteAttrDefault to 42...");
            new_entity.setByteAttrDefault((byte) 42);

            // intAttrDefault
            System.out.println("Setting intAttrDefault to " + Integer.MAX_VALUE + "...");
            new_entity.setIntAttrDefault(Integer.MAX_VALUE);

            // shortAttrDefault
            System.out.println("Setting shortAttrDefault to " + Short.MAX_VALUE + "...");
            new_entity.setShortAttrDefault(Short.MAX_VALUE);

            // longAttrDefault
            System.out.println("Setting longAttrDefault to " + Long.MAX_VALUE + "...");
            new_entity.setLongAttrDefault(Long.MAX_VALUE);

            // booleanAttrDefault
            System.out.println("Setting booleanAttrDefault to true...");
            new_entity.setBooleanAttrDefault(true);

            // charAttrDefault
            System.out.println("Setting charAttrDefault to 'Z'");
            new_entity.setCharAttrDefault('Z');

            // floatAttrDefault
            //System.out.println("Setting floatAttrDefault to " + Float.MAX_VALUE + "...");
            //new_entity.setFloatAttrDefault(Float.MAX_VALUE);
            System.out.println("Setting floatAttrDefault to " + 1.1f + "...");
            new_entity.setFloatAttrDefault(1.1f);

            // doubleAttrDefault
            //System.out.println("Setting doubleAttrDefault to " + Double.MAX_VALUE + "...");
            //new_entity.setDoubleAttrDefault(Double.MAX_VALUE);
            System.out.println("Setting doubleAttrDefault to " + 1.1d + "...");
            new_entity.setDoubleAttrDefault(1.1d);

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Test Serializablity by fetching a copy of the entity, serialize it into a byte array,
            // reconstruct the entity from the byte array, verify that its data is correct.

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }
            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IDatatypeSupportTestEntity find_entity = (IDatatypeSupportTestEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_entity);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity);
            Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity));
            Assert.assertEquals("Assert that the entity's id is " + id, find_entity.getId(), id);

            // Serialize the object, pass it through a "virtual channel", and reconstruct it.
            System.out.println("Serializing and deserializing the entity through a 'virtual' channel...");
            IDatatypeSupportTestEntity serializedEntity = (IDatatypeSupportTestEntity) transmitEntityThroughVirtualChannel(find_entity);

            //  Now, merge the entity back into the persistence context.
            System.out.println("Merging serialized-deserialized entity back into persistence context...");
            IDatatypeSupportTestEntity mergedEntity = jpaResource.getEm().merge(serializedEntity);

            // Verify that the original data stored in the entity came back correctly
            System.out.println("Verifying IDatatypeSupportTestEntity(id=1) has the correct values in its attributes...");

            // Verify byteAttrDefault
            System.out.println("Introspecting byteAttrDefault: " + mergedEntity.getByteAttrDefault());
            Assert.assertEquals("Assert byteAttrDefault matches the expected value [" + 42 + "]", (byte) 42, mergedEntity.getByteAttrDefault());

            // Verify intAttrDefault
            System.out.println("Introspecting intAttrDefault: " + mergedEntity.getIntAttrDefault());
            Assert.assertEquals("Assert intAttrDefault matches the expected value [" + Integer.MAX_VALUE + "]", Integer.MAX_VALUE, mergedEntity.getIntAttrDefault());

            // Verify shortAttrDefault
            System.out.println("Introspecting shortAttrDefault: " + mergedEntity.getShortAttrDefault());
            Assert.assertEquals("Assert shortAttrDefault matches the expected value [" + Short.MAX_VALUE + "]", Short.MAX_VALUE, mergedEntity.getShortAttrDefault());

            // Verify longAttrDefault
            System.out.println("Introspecting longAttrDefault: " + mergedEntity.getLongAttrDefault());
            Assert.assertEquals("Assert longAttrDefault matches the expected value [" + Long.MAX_VALUE + "]", Long.MAX_VALUE, mergedEntity.getLongAttrDefault());

            // Verify booleanAttrDefault
            System.out.println("Introspecting booleanAttrDefault: " + mergedEntity.isBooleanAttrDefault());
            Assert.assertEquals("Assert booleanAttrDefault matches the expected value [" + true + "]", true, mergedEntity.isBooleanAttrDefault());

            // Verify charAttrDefault
            System.out.println("Introspecting charAttrDefault: " + mergedEntity.getCharAttrDefault());
            Assert.assertEquals("Assert charAttrDefault matches the expected value [" + 'Z' + "]", 'Z', mergedEntity.getCharAttrDefault());

            // Verify floatAttrDefault
            System.out.println("Introspecting floatAttrDefault: " + mergedEntity.getFloatAttrDefault());
            Assert.assertEquals("Assert floatAttrDefault matches the expected value [" + 1.1f + "]", //Float.MAX_VALUE + "]",
                                1.1f, //Float.MAX_VALUE,
                                mergedEntity.getFloatAttrDefault(), 0.1);

            // Verify doubleAttrDefault
            System.out.println("Introspecting doubleAttrDefault: " + mergedEntity.getDoubleAttrDefault());
            Assert.assertEquals("Assert doubleAttrDefault matches the expected value [" + 1.1d + "]", //Double.MAX_VALUE + "]",
                                1.1d, //Double.MAX_VALUE,
                                mergedEntity.getDoubleAttrDefault(), 0.1);

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IDatatypeSupportTestEntity find_remove_entity = (IDatatypeSupportTestEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
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
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("DatatypeSupportTestLogic.testJavaPrimitiveSerializableSupport(): End");
        }
    }

    /*
     * 12 Points
     */
    public void testJavaWrapperSerializableSupport(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                   Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("DatatypeSupportTestLogic.testJavaWrapperSerializableSupport(): Missing context and/or resources.  Cannot execute the test.");
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
        DatatypeSupportEntityEnum targetEntityType = DatatypeSupportEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityName + "').  Cannot execute the test.");
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

        final boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.POSTGRES);
//        if (isPostgres) {
//            // TODO: Address this in Eclipselink
//            /*
//             * Will fail with:
//             * Caused by: javax.persistence.PersistenceException: Exception [EclipseLink-4002] (Eclipse Persistence Services - 2.6.8.WAS-v20210604-ce829afecd):
//             * org.eclipse.persistence.exceptions.DatabaseException
//             * Internal Exception: org.postgresql.util.PSQLException: ERROR: invalid byte sequence for encoding "UTF8": 0x00
//             * Error Code: 0
//             * Call: INSERT INTO SerialDatatypeSupPropTE (ID, BIGDECIMALATTRDEFAULT, BIGINTEGERATTRDEFAULT, BOOLEANATTRDEFAULT, BOOLEANWRAPPERATTRDEFAULT, BYTEARRAYATTRDEFAULT,
//             * BYTEATTRDEFAULT, BYTEWRAPPERARRAYATTRDEFAULT, BYTEWRAPPERATTRDEFAULT, CHARARRAYATTRDEFAULT, CHARATTRDEFAULT, CHARWRAPPERARRAYATTRDEFAULT,
//             * CHARACTERWRAPPERATTRDEFAULT, DOUBLEATTRDEFAULT, DOUBLEWRAPPERATTRDEFAULT, ENUMERATION, FLOATATTRDEFAULT, FLOATWRAPPERATTRDEFAULT, INTATTRDEFAULT,
//             * INTEGERWRAPPERATTRDEFAULT, LONGATTRDEFAULT, LONGWRAPPERATTRDEFAULT, SERIALIZABLECLASS, SHORTATTRDEFAULT, SHORTWRAPPERATTRDEFAULT, SQLDATEATTRDEFAULT,
//             * SQLTIMEATTRDEFAULT, SQLTIMESTAMPATTRDEFAULT, STRINGATTRDEFAULT, UTILCALENDARATTRDEFAULT, UTILDATEATTRDEFAULT) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
//             * ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
//             * bind => [1, null, null, false, true, null, 0, null, 42, null, , null, Z, 0.0, 1.1, null, 0.0, 1.1, 0, 2147483647, 0, 9223372036854775807, null, 0, 32767, null, null,
//             * null, null, null, null]
//             * Query: InsertObjectQuery(SerializableDatatypeSupportPropertyTestEntity [id=1])
//             *
//             */
//            return;
//        }

        int id = 1;

        // Execute Test Case
        try {
            System.out.println("DatatypeSupportTestLogic.testJavaWrapperSerializableSupport(): Begin");

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
            IDatatypeSupportTestEntity new_entity = (IDatatypeSupportTestEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(id);

            if (isPostgres) {
                new_entity.setCharAttrDefault(' '); // prevent postgres PSQLException: ERROR: invalid byte sequence for encoding "UTF8": 0x00
            }

            // byteAttrDefault
            System.out.println("Setting byteWrapperAttrDefault to 42...");
            new_entity.setByteWrapperAttrDefault(new Byte((byte) 42));

            // intWrapperAttrDefault
            System.out.println("Setting inttegerWrapperAttrDefault to " + Integer.MAX_VALUE + "...");
            new_entity.setIntegerWrapperAttrDefault(new Integer(Integer.MAX_VALUE));

            // shortWrapperAttrDefault
            System.out.println("Setting shortWrapperAttrDefault to " + Short.MAX_VALUE + "...");
            new_entity.setShortWrapperAttrDefault(new Short(Short.MAX_VALUE));

            // longWrapperAttrDefault
            System.out.println("Setting longWrapperAttrDefault to " + Long.MAX_VALUE + "...");
            new_entity.setLongWrapperAttrDefault(new Long(Long.MAX_VALUE));

            // booleanWrapperAttrDefault
            System.out.println("Setting booleanWrapperAttrDefault to true...");
            new_entity.setBooleanWrapperAttrDefault(new Boolean(true));

            // charWrapperAttrDefault
            System.out.println("Setting charWrapperAttrDefault to 'Z'");
            new_entity.setCharacterWrapperAttrDefault(new Character('Z'));

            // floatWrapperAttrDefault
            System.out.println("Setting floatWrapperAttrDefault to " + 1.1f + "..."); // Float.MAX_VALUE + "...");
            new_entity.setFloatWrapperAttrDefault(new Float(1.1f)); //(new Float(Float.MAX_VALUE));

            // doubleWrapperAttrDefault
            System.out.println("Setting doubleWrapperAttrDefault to " + 1.1d + "..."); //Double.MAX_VALUE + "...");
            new_entity.setDoubleWrapperAttrDefault(new Double(1.1d)); //(new Double(Double.MAX_VALUE));

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Test Serializablity by fetching a copy of the entity, serialize it into a byte array,
            // reconstruct the entity from the byte array, verify that its data is correct.

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }
            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IDatatypeSupportTestEntity find_entity = (IDatatypeSupportTestEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_entity);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity);
            Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity));
            Assert.assertEquals("Assert that the entity's id is " + id, find_entity.getId(), id);

            // Serialize the object, pass it through a "virtual channel", and reconstruct it.
            System.out.println("Serializing and deserializing the entity through a 'virtual' channel...");
            IDatatypeSupportTestEntity serializedEntity = (IDatatypeSupportTestEntity) transmitEntityThroughVirtualChannel(find_entity);

            //  Now, merge the entity back into the persistence context.
            System.out.println("Merging serialized-deserialized entity back into persistence context...");
            IDatatypeSupportTestEntity mergedEntity = jpaResource.getEm().merge(serializedEntity);

            // Verify that the original data stored in the entity came back correctly
            System.out.println("Verifying IDatatypeSupportTestEntity(id=1) has the correct values in its attributes...");

            // Verify byteWrapperAttrDefault
            System.out.println("Introspecting byteWrapperAttrDefault: " + mergedEntity.getByteWrapperAttrDefault());
            Assert.assertEquals("Assert byteAttrDefault matches the expected value [" + new Byte((byte) 42) + "]", new Byte((byte) 42), mergedEntity.getByteWrapperAttrDefault());

            // Verify intWrapperAttrDefault
            System.out.println("Introspecting IntegerWrapperAttrDefault: " + mergedEntity.getIntegerWrapperAttrDefault());
            Assert.assertEquals("Assert intAttrDefault matches the expected value [" + Integer.MAX_VALUE + "]", new Integer(Integer.MAX_VALUE),
                                mergedEntity.getIntegerWrapperAttrDefault());

            // Verify shortAttrDefault
            System.out.println("Introspecting shortWrapperAttrDefault: " + mergedEntity.getShortWrapperAttrDefault());
            Assert.assertEquals("Assert shortWrapperAttrDefault matches the expected value [" + Short.MAX_VALUE + "]", new Short(Short.MAX_VALUE),
                                mergedEntity.getShortWrapperAttrDefault());

            // Verify longWrapperAttrDefault
            System.out.println("Introspecting longWrapperAttrDefault: " + mergedEntity.getLongWrapperAttrDefault());
            Assert.assertEquals("Assert longWrapperAttrDefault matches the expected value [" + Long.MAX_VALUE + "]", new Long(Long.MAX_VALUE),
                                mergedEntity.getLongWrapperAttrDefault());

            // Verify booleanWrapperAttrDefault
            System.out.println("Introspecting booleanWrapperAttrDefault: " + mergedEntity.getBooleanWrapperAttrDefault());
            Assert.assertEquals("Assert booleanAttrDefault matches the expected value [" + true + "]", new Boolean(true), mergedEntity.getBooleanWrapperAttrDefault());

            // Verify charWrapperAttrDefault
            System.out.println("Introspecting charWrapperAttrDefault: " + mergedEntity.getCharacterWrapperAttrDefault());
            Assert.assertEquals("Assert characterWrapperAttrDefault matches the expected value [" + 'Z' + "]", new Character('Z'), mergedEntity.getCharacterWrapperAttrDefault());

            // Verify floatWrapperAttrDefault
            System.out.println("Introspecting floatWrapperAttrDefault: " + mergedEntity.getFloatWrapperAttrDefault());
            Assert.assertEquals("Assert floatWrapperAttrDefault matches the expected value [" + 1.1f + "]", // Float.MAX_VALUE + "]",
                                new Float(1.1f), // new Float(Float.MAX_VALUE),
                                mergedEntity.getFloatWrapperAttrDefault());

            // Verify doubleWrapperAttrDefault
            System.out.println("Introspecting doubleWrapperAttrDefault: " + mergedEntity.getDoubleWrapperAttrDefault());
            Assert.assertEquals("Assert doubleWrapperAttrDefault matches the expected value [" + 1.1d + "]", //Double.MAX_VALUE + "]",
                                new Double(1.1d), //new Double(Double.MAX_VALUE),
                                mergedEntity.getDoubleWrapperAttrDefault());

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IDatatypeSupportTestEntity find_remove_entity = (IDatatypeSupportTestEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
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
            System.out.println("DatatypeSupportTestLogic.testJavaWrapperSerializableSupport(): End");
        }
    }

    /*
     * 6 Points
     */
    public void testLargeNumericTypeSerializableSupport(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                        Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("DatatypeSupportTestLogic.testLargeNumericTypeSerializableSupport(): Missing context and/or resources.  Cannot execute the test.");
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
        DatatypeSupportEntityEnum targetEntityType = DatatypeSupportEntityEnum.resolveEntityByName(entityName);
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));

        final boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.POSTGRES);
        int id = 1;

        // Execute Test Case
        try {
            System.out.println("DatatypeSupportTestLogic.testLargeNumericTypeSerializableSupport(): Begin");

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
            IDatatypeSupportTestEntity new_entity = (IDatatypeSupportTestEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(id);

            if (isPostgres) {
                new_entity.setCharAttrDefault(' ');
                new_entity.setCharacterWrapperAttrDefault(' ');
            }

            // bigIntegerAttrDefault
            System.out.println("Setting bigIntegerAttrDefault to 327683276832768...");
            new_entity.setBigIntegerAttrDefault(new BigInteger("327683276832768"));

            // bigDecimalAttrDefault
            System.out.println("Setting bigDecimalAttrDefault to 3.14...");
            MathContext mc = new MathContext(3);
            BigDecimal bd = new BigDecimal(3.14, mc);
            new_entity.setBigDecimalAttrDefault(bd);

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Test Serializablity by fetching a copy of the entity, serialize it into a byte array,
            // reconstruct the entity from the byte array, verify that its data is correct.

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }
            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IDatatypeSupportTestEntity find_entity = (IDatatypeSupportTestEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_entity);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity);
            Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity));
            Assert.assertEquals("Assert that the entity's id is " + id, find_entity.getId(), id);

            // Serialize the object, pass it through a "virtual channel", and reconstruct it.
            System.out.println("Serializing and deserializing the entity through a 'virtual' channel...");
            IDatatypeSupportTestEntity serializedEntity = (IDatatypeSupportTestEntity) transmitEntityThroughVirtualChannel(find_entity);

            //  Now, merge the entity back into the persistence context.
            System.out.println("Merging serialized-deserialized entity back into persistence context...");
            IDatatypeSupportTestEntity mergedEntity = jpaResource.getEm().merge(serializedEntity);

            // Verify that the original data stored in the entity came back correctly
            System.out.println("Verifying IDatatypeSupportTestEntity(id=1) has the correct values in its attributes...");

            // Verify bigIntegerAttrDefault
            System.out.println("Introspecting bigIntegerAttrDefault: " + mergedEntity.getBigIntegerAttrDefault());
            Assert.assertEquals("Asesert bigIntegerAttrDefault matches the expected value [327683276832768]", new BigInteger("327683276832768"),
                                mergedEntity.getBigIntegerAttrDefault());

            // Verify bigDecimalAttrDefault
            System.out.println("Introspecting bigDecimalAttrDefault: " + mergedEntity.getBigDecimalAttrDefault());

            BigDecimal roundedValue = mergedEntity.getBigDecimalAttrDefault().round(new MathContext(3, java.math.RoundingMode.DOWN));
            System.out.println("Rounding bigDecimalAttrDefault down to 2 decimal places: " + roundedValue);
            Assert.assertTrue("Assert bigDecimalAttrDefault matches the expected value [" + mergedEntity.getBigDecimalAttrDefault() + "]",
                              roundedValue.compareTo(mergedEntity.getBigDecimalAttrDefault()) == 0);

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IDatatypeSupportTestEntity find_remove_entity = (IDatatypeSupportTestEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
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
            System.out.println("DatatypeSupportTestLogic.testLargeNumericTypeSerializableSupport(): End");
        }
    }

    /*
     * 7 Points
     *
     */
    public void testCharArraySerializableSupport(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                 Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("DatatypeSupportTestLogic.testCharArraySerializableSupport(): Missing context and/or resources.  Cannot execute the test.");
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
        DatatypeSupportEntityEnum targetEntityType = DatatypeSupportEntityEnum.resolveEntityByName(entityName);
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));

        final boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.POSTGRES);
        int id = 1;

        // Execute Test Case
        try {
            System.out.println("DatatypeSupportTestLogic.testCharArraySerializableSupport(): Begin");

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
            IDatatypeSupportTestEntity new_entity = (IDatatypeSupportTestEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(id);

            if (isPostgres) {
                new_entity.setCharAttrDefault(' ');
                new_entity.setCharacterWrapperAttrDefault(' ');
            }

            // charArrayAttrDefault
            System.out.println("Setting charArrayAttrDefault...");
            char charArray[] = new char[] { 'a', 'A', 'j', 'G', 'Z' };
            new_entity.setCharArrayAttrDefault(charArray);

            // charWrapperArrayAttrDefault
            System.out.println("Setting charWrapperArrayAttrDefault...");
            Character charWrapperArray[] = new Character[] { new Character('a'), new Character('A'),
                                                             new Character('j'), new Character('G'), new Character('Z') };
            new_entity.setCharWrapperArrayAttrDefault(charWrapperArray);

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Test Serializablity by fetching a copy of the entity, serialize it into a byte array,
            // reconstruct the entity from the byte array, verify that its data is correct.

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }
            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=1)...");
            IDatatypeSupportTestEntity find_entity = (IDatatypeSupportTestEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), 1);
            System.out.println("Object returned by find: " + find_entity);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity);
            Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity));
            Assert.assertEquals("Assert that the entity's id is " + id, find_entity.getId(), id);

            // Serialize the object, pass it through a "virtual channel", and reconstruct it.
            System.out.println("Serializing and deserializing the entity through a 'virtual' channel...");
            IDatatypeSupportTestEntity serializedEntity = (IDatatypeSupportTestEntity) transmitEntityThroughVirtualChannel(find_entity);

            //  Now, merge the entity back into the persistence context.
            System.out.println("Merging serialized-deserialized entity back into persistence context...");
            IDatatypeSupportTestEntity mergedEntity = jpaResource.getEm().merge(serializedEntity);

            // Verify that the original data stored in the entity came back correctly
            System.out.println("Verifying IDatatypeSupportTestEntity(id=1) has the correct values in its attributes...");

            // Verify charArrayAttrDefault
            System.out.println("Introspecting charArrayAttrDefault...");
            char[] charArrayRet = mergedEntity.getCharArrayAttrDefault();
            Assert.assertNotNull("DatatypeSupportTestEntity(id=" + id + ").getCharArrayAttrDefault() returned a null value.", charArrayRet);
            for (int index = 0; index < charArrayRet.length; index++) {
                if (charArrayRet[index] != charArray[index]) {
                    // One of the chars returned did not match
                    Assert.fail("One or more of the chars in the returned char array did not match.\n" +
                                "At index " + index + " a value of " + charArrayRet[index] + "was found\n" +
                                "(was expecting " + charArray[index] + ")");

                }
            }

            // Verify charWrapperArrayAttrDefault
            System.out.println("Introspecting characterWrapperArrayAttrDefault...");
            Character[] charWrapperArrayRet = mergedEntity.getCharWrapperArrayAttrDefault();
            Assert.assertNotNull("IDatatypeSupportTestEntity(id=" + id + ").getCharacterWrapperArrayAttrDefault() returned a null value.", charWrapperArrayRet);
            for (int index = 0; index < charWrapperArrayRet.length; index++) {
                if (!charWrapperArrayRet[index].equals(charWrapperArray[index])) {
                    // One of the Characters returned did not match
                    Assert.fail("One or more of the Characters in the returned Character array did not match.\n" +
                                "At index " + index + " a value of " + charWrapperArrayRet[index] + "was found\n" +
                                "(was expecting " + charWrapperArray[index] + ")");

                }
            }

//            log.assertPass
            System.out.println("Characters have matched expectations.");

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IDatatypeSupportTestEntity find_remove_entity = (IDatatypeSupportTestEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
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
            System.out.println("DatatypeSupportTestLogic.testCharArraySerializableSupport(): End");
        }
    }

    /*
     * 7 Points
     *
     */
    public void testByteArraySerializableSupport(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                 Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("DatatypeSupportTestLogic.testByteArraySerializableSupport(): Missing context and/or resources.  Cannot execute the test.");
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
        DatatypeSupportEntityEnum targetEntityType = DatatypeSupportEntityEnum.resolveEntityByName(entityName);
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));

        final boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.POSTGRES);
        int id = 1;

        // Execute Test Case
        try {
            System.out.println("DatatypeSupportTestLogic.testByteArraySerializableSupport(): Begin");

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
            IDatatypeSupportTestEntity new_entity = (IDatatypeSupportTestEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(id);

            if (isPostgres) {
                new_entity.setCharAttrDefault(' ');
                new_entity.setCharacterWrapperAttrDefault(' ');
            }

            // byteArrayAttrDefault
            System.out.println("Setting byteArrayAttrDefault...");
            byte byteArray[] = new byte[] { (byte) 33, (byte) 45, (byte) 68, (byte) 128, (byte) 250, };
            new_entity.setByteArrayAttrDefault(byteArray);

            // byteWrapperArrayAttrDefault
            System.out.println("Setting byteArrayAttrDefault...");
            Byte byteWrapperArray[] = new Byte[] { new Byte((byte) 33), new Byte((byte) 45),
                                                   new Byte((byte) 68), new Byte((byte) 128), new Byte((byte) 250) };
            new_entity.setByteWrapperArrayAttrDefault(byteWrapperArray);

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Test Serializablity by fetching a copy of the entity, serialize it into a byte array,
            // reconstruct the entity from the byte array, verify that its data is correct.

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }
            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IDatatypeSupportTestEntity find_entity = (IDatatypeSupportTestEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_entity);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity);
            Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity));
            Assert.assertEquals("Assert that the entity's id is " + id, find_entity.getId(), id);

            // Serialize the object, pass it through a "virtual channel", and reconstruct it.
            System.out.println("Serializing and deserializing the entity through a 'virtual' channel...");
            IDatatypeSupportTestEntity serializedEntity = (IDatatypeSupportTestEntity) transmitEntityThroughVirtualChannel(find_entity);

            //  Now, merge the entity back into the persistence context.
            System.out.println("Merging serialized-deserialized entity back into persistence context...");
            IDatatypeSupportTestEntity mergedEntity = jpaResource.getEm().merge(serializedEntity);

            // Verify that the original data stored in the entity came back correctly
            System.out.println("Verifying IDatatypeSupportTestEntity(id=" + id + ") has the correct values in its attributes...");

            // Verify byteArrayAttrDefault
            System.out.println("Introspecting byteArrayAttrDefault...");
            byte[] byteArrayRet = mergedEntity.getByteArrayAttrDefault();
            Assert.assertNotNull("IDatatypeSupportTestEntity(id=1).getByteArrayAttrDefault() returned a null value.", byteArrayRet);
            for (int index = 0; index < byteArrayRet.length; index++) {
                if (byteArrayRet[index] != byteArray[index]) {
                    // One of the bytes returned did not match
                    Assert.fail("One or more of the bytes in the returned byte array did not match.\n" +
                                "At index " + index + " a value of " + byteArrayRet[index] + "was found\n" +
                                "(was expecting " + byteArray[index] + ")");

                }
            }

            // Verify byteWrapperArrayAttrDefault
            System.out.println("Introspecting byteWrapperArrayAttrDefault...");
            Byte[] byteWrapperArrayRet = mergedEntity.getByteWrapperArrayAttrDefault();
            Assert.assertNotNull("IDatatypeSupportTestEntity(id=" + id + ").getByteWrapperArrayAttrDefault() returned a null value.", byteWrapperArrayRet);
            for (int index = 0; index < byteWrapperArrayRet.length; index++) {
                if (!byteWrapperArrayRet[index].equals(byteWrapperArray[index])) {
                    // One of the Bytes returned did not match
                    Assert.fail("One or more of the Bytes in the returned Byte array did not match.\n" +
                                "At index " + index + " a value of " + byteWrapperArrayRet[index] + "was found\n" +
                                "(was expecting " + byteWrapperArray[index] + ")");

                }
            }

//            log.assertPass
            System.out.println("Characters have matched expectations.");

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IDatatypeSupportTestEntity find_remove_entity = (IDatatypeSupportTestEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
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
            System.out.println("DatatypeSupportTestLogic.testByteArraySerializableSupport(): End");
        }
    }

    /*
     * 6 Points
     *
     */
    public void testStringSerializableSupport(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                              Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("DatatypeSupportTestLogic.testStringSerializableSupport(): Missing context and/or resources.  Cannot execute the test.");
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
        DatatypeSupportEntityEnum targetEntityType = DatatypeSupportEntityEnum.resolveEntityByName(entityName);
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));

        final boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.POSTGRES);
        int id = 1;

        // Execute Test Case
        try {
            System.out.println("DatatypeSupportTestLogic.testStringSerializableSupport(): Begin");

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
            IDatatypeSupportTestEntity new_entity = (IDatatypeSupportTestEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(id);

            if (isPostgres) {
                new_entity.setCharAttrDefault(' ');
                new_entity.setCharacterWrapperAttrDefault(' ');
            }

            System.out.println("Setting stringAttrDefault to \"Test String\"...");
            String testString = "Test String";
            new_entity.setStringAttrDefault(testString);

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Test Serializablity by fetching a copy of the entity, serialize it into a byte array,
            // reconstruct the entity from the byte array, verify that its data is correct.

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }
            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IDatatypeSupportTestEntity find_entity = (IDatatypeSupportTestEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_entity);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity);
            Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity));
            Assert.assertEquals("Assert that the entity's id is " + id, find_entity.getId(), id);

            // Serialize the object, pass it through a "virtual channel", and reconstruct it.
            System.out.println("Serializing and deserializing the entity through a 'virtual' channel...");
            IDatatypeSupportTestEntity serializedEntity = (IDatatypeSupportTestEntity) transmitEntityThroughVirtualChannel(find_entity);

            //  Now, merge the entity back into the persistence context.
            System.out.println("Merging serialized-deserialized entity back into persistence context...");
            IDatatypeSupportTestEntity mergedEntity = jpaResource.getEm().merge(serializedEntity);

            // Verify that the original data stored in the entity came back correctly
            System.out.println("Verifying IDatatypeSupportTestEntity(id=1) has the correct values in its attributes...");

            // Verify stringAttrDefault
            System.out.println("Introspecting stringAttrDefault: " + mergedEntity.getStringAttrDefault());
            Assert.assertEquals("Assert stringAttrDefault matches the expected value \"" + testString + "\" " +
                                " (is " + mergedEntity.getStringAttrDefault() + ")", testString, mergedEntity.getStringAttrDefault());

//            log.assertPass
            System.out.println("Characters have matched expectations.");

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IDatatypeSupportTestEntity find_remove_entity = (IDatatypeSupportTestEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
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
            System.out.println("DatatypeSupportTestLogic.testStringSerializableSupport(): End");
        }
    }

    /*
     * 6 Points
     *
     */
    public void testTemporalTypeSerializableSupport(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                    Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("DatatypeSupportTestLogic.testTemporalTypeSerializableSupport(): Missing context and/or resources.  Cannot execute the test.");
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
        DatatypeSupportEntityEnum targetEntityType = DatatypeSupportEntityEnum.resolveEntityByName(entityName);
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));

        final boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.POSTGRES);
        int id = 1;

        // Execute Test Case
        try {
            System.out.println("DatatypeSupportTestLogic.testTemporalTypeSerializableSupport(): Begin");

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
            IDatatypeSupportTestEntity new_entity = (IDatatypeSupportTestEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(id);

            if (isPostgres) {
                new_entity.setCharAttrDefault(' ');
                new_entity.setCharacterWrapperAttrDefault(' ');
            }

            //utilDateAttrDefault
            System.out.println("Setting utilDateAttrDefault to today's date...");
            java.util.Date todaysDate = null;
//            try {
            todaysDate = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.ENGLISH).parse("Jan 12, 2007");
//            } catch (ParseException e) {
//                log.addException("Encountered Exception creating Date object.", e);
//                return;
//            }
            new_entity.setUtilDateAttrDefault(todaysDate);

            // utilCalendarAttrDefault
            System.out.println("Setting utilCalendarAttrDefault to today's date...");
            java.util.Calendar calendar = new java.util.GregorianCalendar(2001, java.util.Calendar.JUNE, 6);
            new_entity.setUtilCalendarAttrDefault(calendar);

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Test Serializablity by fetching a copy of the entity, serialize it into a byte array,
            // reconstruct the entity from the byte array, verify that its data is correct.

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }
            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IDatatypeSupportTestEntity find_entity = (IDatatypeSupportTestEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_entity);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity);
            Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity));
            Assert.assertEquals("Assert that the entity's id is " + id, find_entity.getId(), id);

            // Serialize the object, pass it through a "virtual channel", and reconstruct it.
            System.out.println("Serializing and deserializing the entity through a 'virtual' channel...");
            IDatatypeSupportTestEntity serializedEntity = (IDatatypeSupportTestEntity) transmitEntityThroughVirtualChannel(find_entity);

            //  Now, merge the entity back into the persistence context.
            System.out.println("Merging serialized-deserialized entity back into persistence context...");
            IDatatypeSupportTestEntity mergedEntity = jpaResource.getEm().merge(serializedEntity);

            // Verify utilDateAttrDefault
            System.out.println("Introspecting utilDateAttrDefault: " + mergedEntity.getUtilDateAttrDefault());
            Assert.assertEquals("Assert utilDateAttrDefault matches the expected value [" + todaysDate + "]", todaysDate, mergedEntity.getUtilDateAttrDefault());

            // Verify utilCalendarAttrDefault
            System.out.println("Introspecting utilCalendarAttrDefault: " + mergedEntity.getUtilCalendarAttrDefault());
            Assert.assertEquals("Assert  utilCalendarAttrDefault  matches the expected value [" + calendar + "]", calendar, mergedEntity.getUtilCalendarAttrDefault());

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IDatatypeSupportTestEntity find_remove_entity = (IDatatypeSupportTestEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
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
            System.out.println("DatatypeSupportTestLogic.testTemporalTypeSerializableSupport(): End");
        }
    }

    /*
     * 7 Points
     *
     */
    public void testJDBCTemporalTypeSerializableSupport(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                        Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("DatatypeSupportTestLogic.testJDBCTemporalTypeSerializableSupport(): Missing context and/or resources.  Cannot execute the test.");
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
        DatatypeSupportEntityEnum targetEntityType = DatatypeSupportEntityEnum.resolveEntityByName(entityName);
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));

        final boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.POSTGRES);
        int id = 1;

        // Execute Test Case
        try {
            System.out.println("DatatypeSupportTestLogic.testJDBCTemporalTypeSerializableSupport(): Begin");

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
            IDatatypeSupportTestEntity new_entity = (IDatatypeSupportTestEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(id);

            if (isPostgres) {
                new_entity.setCharAttrDefault(' ');
                new_entity.setCharacterWrapperAttrDefault(' ');
            }

            long currentTimeMillis = System.currentTimeMillis();
            System.out.println("Current Time in Milliseconds = " + currentTimeMillis);

            // sqlDateAttrDefault
            System.out.println("Setting sqlDateAttrDefault to 2007-01-29...");

            java.sql.Date todaysDate = java.sql.Date.valueOf("2007-01-29");
            System.out.println("Result of todaysDate.getTime() = " + todaysDate.getTime());
            new_entity.setSqlDateAttrDefault(todaysDate);

            // sqlTimeAttrDefault
            System.out.println("Setting sqlTimeAttrDefault to 16:30:00...");
            java.sql.Time currentTime = java.sql.Time.valueOf("16:30:00");
            System.out.println("Result of currentTime.getTime() = " + currentTime.getTime());
            new_entity.setSqlTimeAttrDefault(currentTime);

            // sqlTimestampAttrDefault yyyy-mm-dd hh:mm:ss.fffffffff
            System.out.println("Setting sqlTimestampAttrDefault to \'2007-01-29 16:30:00.00\'...");
            java.sql.Timestamp currentTimestamp = java.sql.Timestamp.valueOf("2007-01-29 16:30:00.00");
            System.out.println("Result of currentTimestamp.getTime() = " + currentTimestamp.getTime());
            new_entity.setSqlTimestampAttrDefault(currentTimestamp);

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Test Serializablity by fetching a copy of the entity, serialize it into a byte array,
            // reconstruct the entity from the byte array, verify that its data is correct.

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }
            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IDatatypeSupportTestEntity find_entity = (IDatatypeSupportTestEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_entity);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity);
            Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity));
            Assert.assertEquals("Assert that the entity's id is " + id, find_entity.getId(), id);

            // Serialize the object, pass it through a "virtual channel", and reconstruct it.
            System.out.println("Serializing and deserializing the entity through a 'virtual' channel...");
            IDatatypeSupportTestEntity serializedEntity = (IDatatypeSupportTestEntity) transmitEntityThroughVirtualChannel(find_entity);

            //  Now, merge the entity back into the persistence context.
            System.out.println("Merging serialized-deserialized entity back into persistence context...");
            IDatatypeSupportTestEntity mergedEntity = jpaResource.getEm().merge(serializedEntity);

            // Verify sqlDateAttrDefault
            System.out.println("Introspecting sqlDateAttrDefault: " + mergedEntity.getSqlDateAttrDefault());
            System.out.println("Result of find_entity.getSqlDateAttrDefault().getTime() = " + mergedEntity.getSqlDateAttrDefault().getTime());
            Assert.assertEquals("Assert sqlDateAttrDefault matches the expected value [" + todaysDate + "]", todaysDate, mergedEntity.getSqlDateAttrDefault());

            // Verify sqlTimeAttrDefault
            System.out.println("Introspecting sqlTimeAttrDefault: " + mergedEntity.getSqlTimeAttrDefault());
            System.out.println("Result of find_entity.getSqlTimeAttrDefault().getTime() = " + mergedEntity.getSqlTimeAttrDefault().getTime());
            Assert.assertEquals("Assert sqlTimeAttrDefault matches the expected value [" + currentTime + "]", currentTime, mergedEntity.getSqlTimeAttrDefault());

            // Verify sqlTimestampAttrDefault
            System.out.println("Introspecting sqlTimestampAttrDefault: " + mergedEntity.getSqlTimestampAttrDefault());
            System.out.println("Result of find_entity.getSqlTimestampAttrDefault().getTime() = " + mergedEntity.getSqlTimestampAttrDefault().getTime());
            Assert.assertEquals("Assert sqlTimestampAttrDefault matches the expected value [" + currentTimestamp + "]", currentTimestamp,
                                mergedEntity.getSqlTimestampAttrDefault());

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IDatatypeSupportTestEntity find_remove_entity = (IDatatypeSupportTestEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
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
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("DatatypeSupportTestLogic.testJDBCTemporalTypeSerializableSupport(): End");
        }
    }

    /*
     * 5 Points
     */
    public void testEnumeratedTypeSerializableSupport(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                      Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("DatatypeSupportTestLogic.testEnumeratedTypeSerializableSupport(): Missing context and/or resources.  Cannot execute the test.");
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
        DatatypeSupportEntityEnum targetEntityType = DatatypeSupportEntityEnum.resolveEntityByName(entityName);
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));

        final boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.POSTGRES);
        int id = 1;

        // Execute Test Case
        try {
            System.out.println("DatatypeSupportTestLogic.testEnumeratedTypeSerializableSupport(): Begin");

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
            IDatatypeSupportTestEntity new_entity = (IDatatypeSupportTestEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(id);

            if (isPostgres) {
                new_entity.setCharAttrDefault(' ');
                new_entity.setCharacterWrapperAttrDefault(' ');
            }

            System.out.println("Setting enumeration to " + Constants.TestEnumeration.VALUE_ONE);
            new_entity.setEnumeration(Constants.TestEnumeration.VALUE_ONE);

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
            IDatatypeSupportTestEntity find_entity = (IDatatypeSupportTestEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_entity);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity);
            Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity));
            Assert.assertEquals("Assert that the entity's id is " + id, find_entity.getId(), id);

            // Test Serializablity by fetching a copy of the entity, serialize it into a byte array,
            // reconstruct the entity from the byte array, verify that its data is correct.

            // Serialize the object, pass it through a "virtual channel", and reconstruct it.
            System.out.println("Serializing and deserializing the entity through a 'virtual' channel...");
            IDatatypeSupportTestEntity serializedEntity = (IDatatypeSupportTestEntity) transmitEntityThroughVirtualChannel(find_entity);

            //  Now, merge the entity back into the persistence context.
            System.out.println("Merging serialized-deserialized entity back into persistence context...");
            IDatatypeSupportTestEntity mergedEntity = jpaResource.getEm().merge(serializedEntity);

            System.out.println("Introspecting enumeration: " + mergedEntity.getEnumeration());
            Assert.assertEquals("Assert of enumeration on the entity matches match the expected value [" + Constants.TestEnumeration.VALUE_ONE + "]",
                                Constants.TestEnumeration.VALUE_ONE, mergedEntity.getEnumeration());

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IDatatypeSupportTestEntity find_remove_entity = (IDatatypeSupportTestEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
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
            System.out.println("DatatypeSupportTestLogic.testEnumeratedTypeSerializableSupport(): End");
        }
    }

    private Object transmitEntityThroughVirtualChannel(Object obj) {
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos = null;
        ByteArrayInputStream bais = null;
        ObjectInputStream ois = null;

        try {
            System.out.println("Serializing object " + obj + " into an array of bytes...");
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.close();

            byte serializedBytes[] = baos.toByteArray();
            System.out.println("Serialization complete, result is a byte array of length " + serializedBytes.length);
            baos.close();
            baos = null; // Set to null for garbage collection
            oos = null;

            System.out.println("Transforming byte array back into an object...");

            bais = new ByteArrayInputStream(serializedBytes);
            ois = new ObjectInputStream(bais);
            Object restoredObject = ois.readObject();
            ois.close();
            ois = null;
            bais = null;

            return restoredObject;
        } catch (IOException e) {
            // Any errors here, encapsulate in a RuntimeException and rethrow.
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            // Any errors here, encapsulate in a RuntimeException and rethrow.
            throw new RuntimeException(e);
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (Exception e) {
                }
                oos = null;
            }

            if (baos != null) {
                try {
                    baos.close();
                } catch (Exception e) {
                }
                baos = null;
            }

            if (ois != null) {
                try {
                    ois.close();
                } catch (Exception e) {
                }
                ois = null;
            }

            if (bais != null) {
                try {
                    bais.close();
                } catch (Exception e) {
                }
                bais = null;
            }
        }
    }

    /*
     * 5 Points
     */
    public void testSerializableTypeSerializableSupport(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                                        Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("DatatypeSupportTestLogic.testSerializableTypeSerializableSupport(): Missing context and/or resources.  Cannot execute the test.");
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
        DatatypeSupportEntityEnum targetEntityType = DatatypeSupportEntityEnum.resolveEntityByName(entityName);
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

        final String dbProductName = (testProps == null) ? "UNKNOWN" : ((testProps.get("dbProductName") == null) ? "UNKNOWN" : (String) testProps.get("dbProductName"));

        final boolean isPostgres = DatabaseVendor.checkDBProductName(dbProductName, DatabaseVendor.POSTGRES);
        int id = 1;

        // Execute Test Case
        try {
            System.out.println("DatatypeSupportTestLogic.testSerializableTypeSerializableSupport(): Begin");

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
            IDatatypeSupportTestEntity new_entity = (IDatatypeSupportTestEntity) constructNewEntityObject(targetEntityType);
            new_entity.setId(id);

            if (isPostgres) {
                new_entity.setCharAttrDefault(' ');
                new_entity.setCharacterWrapperAttrDefault(' ');
            }

            // serializableClass
            SerializableClass sc = new SerializableClass();
            byte byteArray[] = new byte[] { (byte) 33, (byte) 45, (byte) 68, (byte) 128, (byte) 250, };
            sc.setSomeBytes(byteArray);
            sc.setSomeInt(42);
            sc.setSomeString("Some String");

            System.out.println("Setting serializableClass to " + sc);
            new_entity.setSerializableClass(sc);

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Test Serializablity by fetching a copy of the entity, serialize it into a byte array,
            // reconstruct the entity from the byte array, verify that its data is correct.

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }
            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IDatatypeSupportTestEntity find_entity = (IDatatypeSupportTestEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
            System.out.println("Object returned by find: " + find_entity);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity);
            Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity));
            Assert.assertEquals("Assert that the entity's id is " + id, find_entity.getId(), id);

            // Serialize the object, pass it through a "virtual channel", and reconstruct it.
            System.out.println("Serializing and deserializing the entity through a 'virtual' channel...");
            IDatatypeSupportTestEntity serializedEntity = (IDatatypeSupportTestEntity) transmitEntityThroughVirtualChannel(find_entity);

            //  Now, merge the entity back into the persistence context.
            System.out.println("Merging serialized-deserialized entity back into persistence context...");
            IDatatypeSupportTestEntity mergedEntity = jpaResource.getEm().merge(serializedEntity);

            System.out.println("Introspecting enumeration: " + mergedEntity.getSerializableClass());
            Assert.assertEquals("Assert serializable class on the entity matches match the expected value [" + sc + "] " +
                                " (is [" + mergedEntity.getSerializableClass() + "])", sc, mergedEntity.getSerializableClass());

            System.out.println("Finding " + targetEntityType.getEntityName() + " (id=" + id + ")...");
            IDatatypeSupportTestEntity find_remove_entity = (IDatatypeSupportTestEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), id);
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
            System.out.println("DatatypeSupportTestLogic.testSerializableTypeSerializableSupport(): End");
        }
    }
}
