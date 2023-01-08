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

public class DatatypeSupportTestLogic extends AbstractTestLogic {

    /*
     * 12 Points
     */
    public void testJavaPrimitiveSupport(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                         Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("DatatypeSupportTestLogic.testJavaPrimitiveSupport(): Missing context and/or resources.  Cannot execute the test.");
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
            System.out.println("DatatypeSupportTestLogic.testJavaPrimitiveSupport(): Begin");

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

            // Verify that the original data stored in the entity came back correctly
            System.out.println("Verifying IDatatypeSupportTestEntity(id=1) has the correct values in its attributes...");

            // Verify byteAttrDefault
            System.out.println("Introspecting byteAttrDefault: " + find_entity.getByteAttrDefault());
            Assert.assertEquals("Assert byteAttrDefault matches the expected value [" + 42 + "]", (byte) 42, find_entity.getByteAttrDefault());

            // Verify intAttrDefault
            System.out.println("Introspecting intAttrDefault: " + find_entity.getIntAttrDefault());
            Assert.assertEquals("Assert intAttrDefault matches the expected value [" + Integer.MAX_VALUE + "]", Integer.MAX_VALUE, find_entity.getIntAttrDefault());

            // Verify shortAttrDefault
            System.out.println("Introspecting shortAttrDefault: " + find_entity.getShortAttrDefault());
            Assert.assertEquals("Assert shortAttrDefault matches the expected value [" + Short.MAX_VALUE + "]", Short.MAX_VALUE, find_entity.getShortAttrDefault());

            // Verify longAttrDefault
            System.out.println("Introspecting longAttrDefault: " + find_entity.getLongAttrDefault());
            Assert.assertEquals("Assert longAttrDefault matches the expected value [" + Long.MAX_VALUE + "]", Long.MAX_VALUE, find_entity.getLongAttrDefault());

            // Verify booleanAttrDefault
            System.out.println("Introspecting booleanAttrDefault: " + find_entity.isBooleanAttrDefault());
            Assert.assertEquals("Assert booleanAttrDefault matches the expected value [" + true + "]", true, find_entity.isBooleanAttrDefault());

            // Verify charAttrDefault
            System.out.println("Introspecting charAttrDefault: " + find_entity.getCharAttrDefault());
            Assert.assertEquals("Assert charAttrDefault matches the expected value [" + 'Z' + "]", 'Z', find_entity.getCharAttrDefault());

            // Verify floatAttrDefault
            System.out.println("Introspecting floatAttrDefault: " + find_entity.getFloatAttrDefault());
            Assert.assertEquals("Assert floatAttrDefault matches the expected value [" + 1.1f + "]", //Float.MAX_VALUE + "]",
                                1.1f, //Float.MAX_VALUE,
                                find_entity.getFloatAttrDefault(), 0.1);

            // Verify doubleAttrDefault
            System.out.println("Introspecting doubleAttrDefault: " + find_entity.getDoubleAttrDefault());
            Assert.assertEquals("Assert doubleAttrDefault matches the expected value [" + 1.1d + "]", //Double.MAX_VALUE + "]",
                                1.1d, //Double.MAX_VALUE,
                                find_entity.getDoubleAttrDefault(), 0.1);

            System.out.println("Rolling Back transaction...");
            jpaResource.getTj().rollbackTransaction();

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
            System.out.println("DatatypeSupportTestLogic.testJavaPrimitiveSupport(): End");
        }
    }

    /*
     * 12 Points
     */
    public void testJavaWrapperSupport(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                       Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("DatatypeSupportTestLogic.testJavaWrapperSupport(): Missing context and/or resources.  Cannot execute the test.");
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
            System.out.println("DatatypeSupportTestLogic.testJavaWrapperSupport(): Begin");

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

            // Verify that the original data stored in the entity came back correctly
            System.out.println("Verifying IDatatypeSupportTestEntity(id=1) has the correct values in its attributes...");

            // Verify byteWrapperAttrDefault
            System.out.println("Introspecting byteWrapperAttrDefault: " + find_entity.getByteWrapperAttrDefault());
            Assert.assertEquals("Assert byteAttrDefault matches the expected value [" + new Byte((byte) 42) + "]", new Byte((byte) 42), find_entity.getByteWrapperAttrDefault());

            // Verify intWrapperAttrDefault
            System.out.println("Introspecting IntegerWrapperAttrDefault: " + find_entity.getIntegerWrapperAttrDefault());
            Assert.assertEquals("Assert intAttrDefault matches the expected value [" + Integer.MAX_VALUE + "]", new Integer(Integer.MAX_VALUE),
                                find_entity.getIntegerWrapperAttrDefault());

            // Verify shortAttrDefault
            System.out.println("Introspecting shortWrapperAttrDefault: " + find_entity.getShortWrapperAttrDefault());
            Assert.assertEquals("Assert shortWrapperAttrDefault matches the expected value [" + Short.MAX_VALUE + "]", new Short(Short.MAX_VALUE),
                                find_entity.getShortWrapperAttrDefault());

            // Verify longWrapperAttrDefault
            System.out.println("Introspecting longWrapperAttrDefault: " + find_entity.getLongWrapperAttrDefault());
            Assert.assertEquals("Assert longWrapperAttrDefault matches the expected value [" + Long.MAX_VALUE + "]", new Long(Long.MAX_VALUE),
                                find_entity.getLongWrapperAttrDefault());

            // Verify booleanWrapperAttrDefault
            System.out.println("Introspecting booleanWrapperAttrDefault: " + find_entity.getBooleanWrapperAttrDefault());
            Assert.assertEquals("Assert booleanAttrDefault matches the expected value [" + true + "]", new Boolean(true), find_entity.getBooleanWrapperAttrDefault());

            // Verify charWrapperAttrDefault
            System.out.println("Introspecting charWrapperAttrDefault: " + find_entity.getCharacterWrapperAttrDefault());
            Assert.assertEquals("Assert characterWrapperAttrDefault matches the expected value [" + 'Z' + "]", new Character('Z'), find_entity.getCharacterWrapperAttrDefault());

            // Verify floatWrapperAttrDefault
            System.out.println("Introspecting floatWrapperAttrDefault: " + find_entity.getFloatWrapperAttrDefault());
            Assert.assertEquals("Assert floatWrapperAttrDefault matches the expected value [" + 1.1f + "]", // Float.MAX_VALUE + "]",
                                new Float(1.1f), // new Float(Float.MAX_VALUE),
                                find_entity.getFloatWrapperAttrDefault());

            // Verify doubleWrapperAttrDefault
            System.out.println("Introspecting doubleWrapperAttrDefault: " + find_entity.getDoubleWrapperAttrDefault());
            Assert.assertEquals("Assert doubleWrapperAttrDefault matches the expected value [" + 1.1d + "]", //Double.MAX_VALUE + "]",
                                new Double(1.1d), //new Double(Double.MAX_VALUE),
                                find_entity.getDoubleWrapperAttrDefault());

            System.out.println("Rolling Back transaction...");
            jpaResource.getTj().rollbackTransaction();

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
            System.out.println("DatatypeSupportTestLogic.testJavaWrapperSupport(): End");
        }
    }

    /*
     * 6 Points
     */
    public void testLargeNumericTypeSupport(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                            Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("DatatypeSupportTestLogic.testLargeNumericTypeSupport(): Missing context and/or resources.  Cannot execute the test.");
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
            System.out.println("DatatypeSupportTestLogic.testLargeNumericTypeSupport(): Begin");

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

            // Verify bigIntegerAttrDefault
            System.out.println("Introspecting bigIntegerAttrDefault: " + find_entity.getBigIntegerAttrDefault());
            Assert.assertEquals("Asesert bigIntegerAttrDefault matches the expected value [327683276832768]", new BigInteger("327683276832768"),
                                find_entity.getBigIntegerAttrDefault());

            // Verify bigDecimalAttrDefault
            System.out.println("Introspecting bigDecimalAttrDefault: " + find_entity.getBigDecimalAttrDefault());

            BigDecimal roundedValue = find_entity.getBigDecimalAttrDefault().round(new MathContext(3, java.math.RoundingMode.DOWN));
            System.out.println("Rounding bigDecimalAttrDefault down to 2 decimal places: " + roundedValue);
            Assert.assertTrue("Assert bigDecimalAttrDefault matches the expected value [" + find_entity.getBigDecimalAttrDefault() + "]",
                              roundedValue.compareTo(find_entity.getBigDecimalAttrDefault()) == 0);

            System.out.println("Rolling Back transaction...");
            jpaResource.getTj().rollbackTransaction();

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
            System.out.println("DatatypeSupportTestLogic.testLargeNumericTypeSupport(): End");
        }
    }

    /*
     * 7 Points
     *
     */
    public void testCharArraySupport(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                     Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("DatatypeSupportTestLogic.testCharArraySupport(): Missing context and/or resources.  Cannot execute the test.");
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
            System.out.println("DatatypeSupportTestLogic.testCharArraySupport(): Begin");

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

            // Verify that the original data stored in the entity came back correctly
            System.out.println("Verifying IDatatypeSupportTestEntity(id=1) has the correct values in its attributes...");

            // Verify charArrayAttrDefault
            System.out.println("Introspecting charArrayAttrDefault...");
            char[] charArrayRet = find_entity.getCharArrayAttrDefault();
            Assert.assertNotNull("DatatypeSupportTestEntity(id=1).getCharArrayAttrDefault() returned a null value.", charArrayRet);
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
            Character[] charWrapperArrayRet = find_entity.getCharWrapperArrayAttrDefault();
            Assert.assertNotNull("IDatatypeSupportTestEntity(id=1).getCharacterWrapperArrayAttrDefault() returned a null value.",
                                 charWrapperArrayRet);
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

            System.out.println("Rolling Back transaction...");
            jpaResource.getTj().rollbackTransaction();

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
            System.out.println("DatatypeSupportTestLogic.testCharArraySupport(): End");
        }
    }

    /*
     * 7 Points
     *
     */
    public void testByteArraySupport(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                     Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("DatatypeSupportTestLogic.testByteArraySupport(): Missing context and/or resources.  Cannot execute the test.");
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
            System.out.println("DatatypeSupportTestLogic.testByteArraySupport(): Begin");

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

            // Verify that the original data stored in the entity came back correctly
            System.out.println("Verifying IDatatypeSupportTestEntity(id=1) has the correct values in its attributes...");

            // Verify byteArrayAttrDefault
            System.out.println("Introspecting byteArrayAttrDefault...");
            byte[] byteArrayRet = find_entity.getByteArrayAttrDefault();
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
            Byte[] byteWrapperArrayRet = find_entity.getByteWrapperArrayAttrDefault();
            Assert.assertNotNull("IDatatypeSupportTestEntity(id=1).getByteWrapperArrayAttrDefault() returned a null value.", byteWrapperArrayRet);
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

            System.out.println("Rolling Back transaction...");
            jpaResource.getTj().rollbackTransaction();

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
            System.out.println("DatatypeSupportTestLogic.testByteArraySupport(): End");
        }
    }

    /*
     * 6 Points
     *
     */
    public void testStringSupport(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                  Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("DatatypeSupportTestLogic.testStringSupport(): Missing context and/or resources.  Cannot execute the test.");
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
            System.out.println("DatatypeSupportTestLogic.testStringSupport(): Begin");

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

            // Verify that the original data stored in the entity came back correctly
            System.out.println("Verifying IDatatypeSupportTestEntity(id=1) has the correct values in its attributes...");

            // Verify stringAttrDefault
            System.out.println("Introspecting stringAttrDefault: " + find_entity.getStringAttrDefault());
            Assert.assertEquals("Assert stringAttrDefault matches the expected value \"" + testString + "\"", testString, find_entity.getStringAttrDefault());

//            log.assertPass
            System.out.println("Characters have matched expectations.");

            System.out.println("Rolling Back transaction...");
            jpaResource.getTj().rollbackTransaction();

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
            System.out.println("DatatypeSupportTestLogic.testStringSupport(): End");
        }
    }

    /*
     * 6 Points
     *
     */
    public void testTemporalTypeSupport(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                        Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("DatatypeSupportTestLogic.testTemporalTypeSupport(): Missing context and/or resources.  Cannot execute the test.");
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
            System.out.println("DatatypeSupportTestLogic.testTemporalTypeSupport(): Begin");

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

            // Verify utilDateAttrDefault
            System.out.println("Introspecting utilDateAttrDefault: " + find_entity.getUtilDateAttrDefault());
            Assert.assertEquals("Assert utilDateAttrDefault matches the expected value [" + todaysDate + "]", todaysDate, find_entity.getUtilDateAttrDefault());

            // Verify utilCalendarAttrDefault
            System.out.println("Introspecting utilCalendarAttrDefault: " + find_entity.getUtilCalendarAttrDefault());
            Assert.assertEquals("Assert  utilCalendarAttrDefault  matches the expected value [" + calendar + "]", calendar, find_entity.getUtilCalendarAttrDefault());

            System.out.println("Rolling Back transaction...");
            jpaResource.getTj().rollbackTransaction();

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
            System.out.println("DatatypeSupportTestLogic.testTemporalTypeSupport(): End");
        }
    }

    /*
     * 7 Points
     *
     */
    public void testJDBCTemporalTypeSupport(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                            Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("DatatypeSupportTestLogic.testJDBCTemporalTypeSupport(): Missing context and/or resources.  Cannot execute the test.");
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
            System.out.println("DatatypeSupportTestLogic.testJDBCTemporalTypeSupport(): Begin");

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

            // Verify sqlDateAttrDefault
            System.out.println("Introspecting sqlDateAttrDefault: " + find_entity.getSqlDateAttrDefault());
            System.out.println("Result of find_entity.getSqlDateAttrDefault().getTime() = " + find_entity.getSqlDateAttrDefault().getTime());
            Assert.assertEquals("Assert sqlDateAttrDefault matches the expected value [" + todaysDate + "]", todaysDate, find_entity.getSqlDateAttrDefault());

            // Verify sqlTimeAttrDefault
            System.out.println("Introspecting sqlTimeAttrDefault: " + find_entity.getSqlTimeAttrDefault());
            System.out.println("Result of find_entity.getSqlTimeAttrDefault().getTime() = " + find_entity.getSqlTimeAttrDefault().getTime());
            Assert.assertEquals("Assert sqlTimeAttrDefault matches the expected value [" + currentTime + "]", currentTime, find_entity.getSqlTimeAttrDefault());

            // Verify sqlTimestampAttrDefault
            System.out.println("Introspecting sqlTimestampAttrDefault: " + find_entity.getSqlTimestampAttrDefault());
            System.out.println("Result of find_entity.getSqlTimestampAttrDefault().getTime() = " + find_entity.getSqlTimestampAttrDefault().getTime());
            Assert.assertEquals("Assert sqlTimestampAttrDefault matches the expected value [" + currentTimestamp + "]", currentTimestamp, find_entity.getSqlTimestampAttrDefault());

            System.out.println("Rolling Back transaction...");
            jpaResource.getTj().rollbackTransaction();

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
            System.out.println("DatatypeSupportTestLogic.testJDBCTemporalTypeSupport(): End");
        }
    }

    /*
     * 5 Points
     */
    public void testEnumeratedTypeSupport(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                          Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("DatatypeSupportTestLogic.testEnumeratedTypeSupport(): Missing context and/or resources.  Cannot execute the test.");
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
            System.out.println("DatatypeSupportTestLogic.testEnumeratedTypeSupport(): Begin");

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

            System.out.println("Introspecting enumeration: " + find_entity.getEnumeration());
            Assert.assertEquals("Assert of enumeration on the entity matches match the expected value [" + Constants.TestEnumeration.VALUE_ONE + "]",
                                Constants.TestEnumeration.VALUE_ONE, find_entity.getEnumeration());

            System.out.println("Rolling Back transaction...");
            jpaResource.getTj().rollbackTransaction();

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
            System.out.println("DatatypeSupportTestLogic.testEnumeratedTypeSupport(): End");
        }
    }

    /*
     * 5 Points
     */
    public void testSerializableTypeSupport(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                            Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("DatatypeSupportTestLogic.testSerializableTypeSupport(): Missing context and/or resources.  Cannot execute the test.");
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
            System.out.println("DatatypeSupportTestLogic.testSerializableTypeSupport(): Begin");

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
            byte byteArray[] = new byte[] { (byte) 33, (byte) 45, (byte) 68, (byte) 128, (byte) 250 };
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

            System.out.println("Introspecting enumeration: " + find_entity.getSerializableClass());
            Assert.assertEquals("Assert serializable class on the entity matches match the expected value [" + sc + "]" +
                                " (is " + find_entity.getSerializableClass() + ")", sc, find_entity.getSerializableClass());

            System.out.println("Rolling Back transaction...");
            jpaResource.getTj().rollbackTransaction();

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
            System.out.println("DatatypeSupportTestLogic.testSerializableTypeSupport(): End");
        }
    }
}
