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

import java.util.Calendar;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.entity.entities.IPKEntity;
import com.ibm.ws.jpa.fvt.entity.testlogic.enums.PKEntityEnum;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class PKEntityTestLogic extends AbstractTestLogic {
    private static final byte standardBytePKValue = (byte) 42;
    private static final Byte standardByteWrapperPKValue = new Byte((byte) 42);
    private static final char standardCharPKValue = 'J';
    private static final Character standardCharWrapperPKValue = new Character('J');
    private static final int standardIntPKValue = 42;
    private static final Integer standardIntWrapperPKValue = new Integer(42);
    private static final long standardLongPKValue = 42;
    private static final Long standardLongWrapperPKValue = new Long(42);
    private static final short standardShortPKValue = 42;
    private static final Short standardShortWrapperPKValue = new Short((short) 42);
    private static final String standardStringPKValue = "PK String";
    private static final java.util.Date standardJavaUtilDatePKValue; // = new java.util.Date(System.currentTimeMillis());
    private static final java.sql.Date standardJavaSqlDatePKValue = java.sql.Date.valueOf("2007-01-29");

    static {
        // Doc on java.sql.Date (which Temporal(Date) maps to) states:
        // To conform with the definition of SQL DATE, the millisecond values wrapped by a
        // java.sql.Date instance must be 'normalized' by setting the hours, minutes, seconds,
        // and milliseconds to zero in the particular time zone with which the instance is associated.
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        standardJavaUtilDatePKValue = calendar.getTime();
    }

    /**
     * 11 Points
     */
    public void testPKEntity001(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                                Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("PKEntityTestLogic.testPKEntity001(): Missing context and/or resources.  Cannot execute the test.");
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
        PKEntityEnum targetEntityType = PKEntityEnum.resolveEntityByName(entityName);
        if (targetEntityType == null) {
            // Oops, unknown type
            Assert.fail("Invalid Entity-A type specified ('" + entityName + "').  Cannot execute the test.");
            return;
        }

        // Execute Test Case
        try {
            System.out.println("PKEntityTestLogic.testPKEntity001(): Begin");

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
            System.out.println("Creating new object instance of " + targetEntityType.getEntityName() + "...");
            IPKEntity new_entity = (IPKEntity) constructNewEntityObject(targetEntityType);
            setPrimaryKey(new_entity, targetEntityType);
            new_entity.setIntVal(8192);

            System.out.println("Persisting " + new_entity);
            jpaResource.getEm().persist(new_entity);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Fetch Entity
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }
            System.out.println("Finding " + targetEntityType.getEntityName() + "...");
            IPKEntity find_entity1 = (IPKEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), getPrimaryKeyValue(targetEntityType));
            jpaResource.getEm().refresh(find_entity1); // Deals with datacache if enabled to force DB fetch
            System.out.println("Object returned by find: " + find_entity1);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity1);
            Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity1);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity1));
            Assert.assertEquals("Assert that the entity's id is " + getPrimaryKeyValue(targetEntityType),
                                getPrimaryKey(find_entity1, targetEntityType),
                                getPrimaryKeyValue(targetEntityType));
            Assert.assertEquals("Assert that " + targetEntityType.getEntityName() + "'s intVal == 8192", 8192, find_entity1.getIntVal());

            System.out.println("Mutate intVal to 16384...");
            find_entity1.setIntVal(16384);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            // Fetch Entity
            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                jpaResource.getEm().joinTransaction();
            }
            System.out.println("Finding " + targetEntityType.getEntityName() + "...");
            IPKEntity find_entity2 = (IPKEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), getPrimaryKeyValue(targetEntityType));
            jpaResource.getEm().refresh(find_entity2); // Deals with datacache if enabled to force DB fetch
            System.out.println("Object returned by find: " + find_entity2);

            Assert.assertNotNull("Assert that the find operation did not return null", find_entity2);
            Assert.assertNotSame("Assert find did not return the original object", new_entity, find_entity2);
            Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(find_entity2));
            Assert.assertEquals("Assert that the entity's id is " + getPrimaryKeyValue(targetEntityType),
                                getPrimaryKey(find_entity2, targetEntityType),
                                getPrimaryKeyValue(targetEntityType));
            Assert.assertEquals("Assert that " + targetEntityType.getEntityName() + "'s intVal == 16384", 16384, find_entity2.getIntVal());

            System.out.println("Removing entity...");
            jpaResource.getEm().remove(find_entity2);

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            jpaResource.getEm().clear();

            System.out.println("Finding " + targetEntityType.getEntityName() + "...");
            IPKEntity find_entity3 = (IPKEntity) jpaResource.getEm().find(resolveEntityClass(targetEntityType), getPrimaryKeyValue(targetEntityType));
            System.out.println("Object returned by find: " + find_entity3);

            Assert.assertNull("Assert that the find operation did return null", find_entity3);

            System.out.println("Ending test.");
        } catch (AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        } finally {
            System.out.println("PKEntityTestLogic.testPKEntity001(): End");
        }
    }

    private Object getPrimaryKeyValue(PKEntityEnum entityType) {
        switch (entityType) {
            case PKEntityByte:
            case XMLPKEntityByte:
                return standardBytePKValue;
            case PKEntityByteWrapper:
            case XMLPKEntityByteWrapper:
                return standardByteWrapperPKValue;
            case PKEntityChar:
            case XMLPKEntityChar:
                return standardCharPKValue;
            case PKEntityCharWrapper:
            case XMLPKEntityCharWrapper:
                return standardCharWrapperPKValue;
            case PKEntityInt:
            case XMLPKEntityInt:
                return standardIntPKValue;
            case PKEntityIntWrapper:
            case XMLPKEntityIntWrapper:
                return standardIntWrapperPKValue;
            case PKEntityLong:
            case XMLPKEntityLong:
                return standardLongPKValue;
            case PKEntityLongWrapper:
            case XMLPKEntityLongWrapper:
                return standardLongWrapperPKValue;
            case PKEntityShort:
            case XMLPKEntityShort:
                return standardShortPKValue;
            case PKEntityShortWrapper:
            case XMLPKEntityShortWrapper:
                return standardShortWrapperPKValue;
            case PKEntityString:
            case XMLPKEntityString:
                return standardStringPKValue;
            case PKEntityJavaSqlDate:
            case XMLPKEntityJavaSqlDate:
                return standardJavaSqlDatePKValue;
            case PKEntityJavaUtilDate:
            case XMLPKEntityJavaUtilDate:
                return standardJavaUtilDatePKValue;
            default:
                return null;
        }
    }

    private void setPrimaryKey(IPKEntity entity, PKEntityEnum entityType) {
        switch (entityType) {
            case PKEntityByte:
            case XMLPKEntityByte:
                entity.setBytePK(standardBytePKValue);
            case PKEntityByteWrapper:
            case XMLPKEntityByteWrapper:
                entity.setByteWrapperPK(standardByteWrapperPKValue);
            case PKEntityChar:
            case XMLPKEntityChar:
                entity.setCharPK(standardCharPKValue);
            case PKEntityCharWrapper:
            case XMLPKEntityCharWrapper:
                entity.setCharacterWrapperPK(standardCharWrapperPKValue);
            case PKEntityInt:
            case XMLPKEntityInt:
                entity.setIntPK(standardIntPKValue);
            case PKEntityIntWrapper:
            case XMLPKEntityIntWrapper:
                entity.setIntegerWrapperPK(standardIntWrapperPKValue);
            case PKEntityLong:
            case XMLPKEntityLong:
                entity.setLongPK(standardLongPKValue);
            case PKEntityLongWrapper:
            case XMLPKEntityLongWrapper:
                entity.setLongWrapperPK(standardLongWrapperPKValue);
            case PKEntityShort:
            case XMLPKEntityShort:
                entity.setShortPK(standardShortPKValue);
            case PKEntityShortWrapper:
            case XMLPKEntityShortWrapper:
                entity.setShortWrapperPK(standardShortWrapperPKValue);
            case PKEntityString:
            case XMLPKEntityString:
                entity.setStringPK(standardStringPKValue);
            case PKEntityJavaSqlDate:
            case XMLPKEntityJavaSqlDate:
                entity.setJavaSqlDatePK(standardJavaSqlDatePKValue);
            case PKEntityJavaUtilDate:
            case XMLPKEntityJavaUtilDate:
                entity.setJavaUtilDatePK(standardJavaUtilDatePKValue);
            default:
        }
    }

    private Object getPrimaryKey(IPKEntity entity, PKEntityEnum entityType) {
        switch (entityType) {
            case PKEntityByte:
            case XMLPKEntityByte:
                return entity.getBytePK();
            case PKEntityByteWrapper:
            case XMLPKEntityByteWrapper:
                return entity.getByteWrapperPK();
            case PKEntityChar:
            case XMLPKEntityChar:
                return entity.getCharPK();
            case PKEntityCharWrapper:
            case XMLPKEntityCharWrapper:
                return entity.getCharacterWrapperPK();
            case PKEntityInt:
            case XMLPKEntityInt:
                return entity.getIntPK();
            case PKEntityIntWrapper:
            case XMLPKEntityIntWrapper:
                return entity.getIntegerWrapperPK();
            case PKEntityLong:
            case XMLPKEntityLong:
                return entity.getLongPK();
            case PKEntityLongWrapper:
            case XMLPKEntityLongWrapper:
                return entity.getLongWrapperPK();
            case PKEntityShort:
            case XMLPKEntityShort:
                return entity.getShortPK();
            case PKEntityShortWrapper:
            case XMLPKEntityShortWrapper:
                return entity.getShortWrapperPK();
            case PKEntityString:
            case XMLPKEntityString:
                return entity.getStringPK();
            case PKEntityJavaSqlDate:
            case XMLPKEntityJavaSqlDate:
                return entity.getJavaSqlDatePK();
            case PKEntityJavaUtilDate:
            case XMLPKEntityJavaUtilDate:
                return entity.getJavaUtilDatePK();
            default:
                return null;
        }
    }
}
