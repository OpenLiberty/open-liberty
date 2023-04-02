/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package com.ibm.ws.jpa.fvt.derivedidentity.testlogic;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Assert;

import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class DerivedIdentityTestLogic extends AbstractTestLogic {
    /**
     * Test Logic: testScenario01
     *
     * Description: Test various basic derived identities with entities that have only simple and derived primary keys
     *
     * Performs these basic derived identity scenarios:
     *
     * 01. Create instances of the entity class(es) with all relationships fully populated
     * 02. Test a merge
     * 03. Verify the entity was saved in the database
     * 04. Update the entity
     * 05. Verify the updated entity was saved in the database
     * 06. Delete the entity from the database
     * 07. Verify the deletion was successful
     * 07. Verify the deletion was successful
     *
     * public void testMerge() {
     * public void testPersist() {
     * public void testQueryRootLevel() {
     * public void testQueryIntermediateLevel() {
     * public void testQueryLeafLevel() {
     * public void testFindRootNode() {
     * public void testFindIntermediateNode() {
     * public void testFindLeafNode() {
     * public void testUpdate() {
     * public void testDeleteRoot() {
     * public void testDeleteLeafObtainedByQuery() {
     * public void testDeleteLeafObtainedByFind() {
     *
     * @OneToOne
     *           private AllFieldTypes selfOneOne;
     * @OneToMany
     *            private List<AllFieldTypes> selfOneMany = new ArrayList<AllFieldTypes>();
     *
     *
     *
     *
     *
     *            <p><b>UML:</b>
     *
     *            <pre>
     *
     *          +--------------+                +--------------+
     *          |              |                |              |
     *          |  Entity0401  |--------------->|  Entity0001  |
     *          |              |  1          1  |              |
     *          +--------------+                +--------------+
     *
     *
     *          +--------------+                +--------------+
     *          |              |                |              |
     *          |  Entity0402  |--------------->|  Entity0002  |
     *          |              |  1          1  |              |
     *          +--------------+                +--------------+
     *
     *
     *          +--------------+                +--------------+
     *          |              |                |              |
     *          |  Entity0403  |--------------->|  Entity0004  |
     *          |              |  1          1  |              |
     *          +--------------+                +--------------+
     *
     *
     *                                .
     *                                .
     *                                .
     *
     *          +--------------+                +--------------+
     *          |              |                |              |
     *          |  Entity0419  |--------------->|  Entity0019  |
     *          |              |  1          1  |              |
     *          +--------------+                +--------------+
     *
     *            </pre>
     */
    public void testScenario01(TestExecutionContext testExecCtx, TestExecutionResources testExecResources,
                               Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("testScenario01: Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        // Fetch JPA Resources
        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        EntityManager em = jpaResource.getEm();

        // Execute Test Case
        try {
            System.out.println("DerivedIdentityTestLogic.testScenario01(): Begin");

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            em.clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            HashMap<DerivedIdentityEntityEnum, Object> parentEntitiesMap = new HashMap<DerivedIdentityEntityEnum, Object>();
            HashMap<DerivedIdentityEntityEnum, Object> childEntitiesMap = new HashMap<DerivedIdentityEntityEnum, Object>();

            System.out.println("########################################################################################");
            System.out.println("##                                                                                    ##");
            System.out.println("## Populating database...                                                             ##");
            System.out.println("##                                                                                    ##");
            System.out.println("########################################################################################");

            // Parent Entities
            parentEntitiesMap.put(DerivedIdentityEntityEnum.Entity0001,
                                  createDIEEParentEntity(DerivedIdentityEntityEnum.Entity0001, (byte) 01, byte.class, em));
            parentEntitiesMap.put(DerivedIdentityEntityEnum.Entity0002,
                                  createDIEEParentEntity(DerivedIdentityEntityEnum.Entity0002, (byte) 02, byte.class, em));
            parentEntitiesMap.put(DerivedIdentityEntityEnum.Entity0003,
                                  createDIEEParentEntity(DerivedIdentityEntityEnum.Entity0003, '3', char.class, em));
            parentEntitiesMap.put(DerivedIdentityEntityEnum.Entity0004,
                                  createDIEEParentEntity(DerivedIdentityEntityEnum.Entity0004, '4', char.class, em));
//            parentEntitiesMap.put(
//            		DerivedIdentityEntityEnum.Entity0004,
//            		createDIEEParentEntity(DerivedIdentityEntityEnum.Entity0004, '4', char.class, jpaResource.getEm()));
            parentEntitiesMap.put(DerivedIdentityEntityEnum.Entity0005,
                                  createDIEEParentEntity(DerivedIdentityEntityEnum.Entity0005, "ENTITY0005_ID", String.class, em));
            parentEntitiesMap.put(DerivedIdentityEntityEnum.Entity0006,
                                  createDIEEParentEntity(DerivedIdentityEntityEnum.Entity0006, 6600.0066D, double.class, em));
            parentEntitiesMap.put(DerivedIdentityEntityEnum.Entity0007,
                                  createDIEEParentEntity(DerivedIdentityEntityEnum.Entity0007, 7700.0077D, double.class, em));
            parentEntitiesMap.put(DerivedIdentityEntityEnum.Entity0008,
                                  createDIEEParentEntity(DerivedIdentityEntityEnum.Entity0008, 8800.0088F, float.class, em));
            parentEntitiesMap.put(DerivedIdentityEntityEnum.Entity0009,
                                  createDIEEParentEntity(DerivedIdentityEntityEnum.Entity0009, 9900.0099F, float.class, em));
            parentEntitiesMap.put(DerivedIdentityEntityEnum.Entity0010,
                                  createDIEEParentEntity(DerivedIdentityEntityEnum.Entity0010, 10, int.class, em));
            parentEntitiesMap.put(DerivedIdentityEntityEnum.Entity0011,
                                  createDIEEParentEntity(DerivedIdentityEntityEnum.Entity0011, 11, int.class, em));
            parentEntitiesMap.put(DerivedIdentityEntityEnum.Entity0012,
                                  createDIEEParentEntity(DerivedIdentityEntityEnum.Entity0012, 12L, long.class, em));
            parentEntitiesMap.put(DerivedIdentityEntityEnum.Entity0013,
                                  createDIEEParentEntity(DerivedIdentityEntityEnum.Entity0013, 13L, long.class, em));
            parentEntitiesMap.put(DerivedIdentityEntityEnum.Entity0014,
                                  createDIEEParentEntity(DerivedIdentityEntityEnum.Entity0014, (short) 14, short.class, em));
            parentEntitiesMap.put(DerivedIdentityEntityEnum.Entity0015,
                                  createDIEEParentEntity(DerivedIdentityEntityEnum.Entity0015, (short) 15, short.class, em));
            parentEntitiesMap.put(DerivedIdentityEntityEnum.Entity0016,
                                  createDIEEParentEntity(DerivedIdentityEntityEnum.Entity0016, new BigDecimal("1600.161616"), BigDecimal.class, em));
            parentEntitiesMap.put(DerivedIdentityEntityEnum.Entity0017,
                                  createDIEEParentEntity(DerivedIdentityEntityEnum.Entity0017, new BigInteger("17000017"), BigInteger.class, em));
            parentEntitiesMap.put(DerivedIdentityEntityEnum.Entity0018,
                                  createDIEEParentEntity(DerivedIdentityEntityEnum.Entity0018, new java.util.Date(50, 18, 18, 0, 0, 0 /* 18,18,18 */), java.util.Date.class,
                                                         em));
            parentEntitiesMap.put(DerivedIdentityEntityEnum.Entity0019,
                                  createDIEEParentEntity(DerivedIdentityEntityEnum.Entity0019, new java.sql.Date(50, 19, 19), java.sql.Date.class, em));

            // Child Entities
            childEntitiesMap.put(DerivedIdentityEntityEnum.Entity0401,
                                 createDIEEChildEntity(DerivedIdentityEntityEnum.Entity0401, parentEntitiesMap.get(DerivedIdentityEntityEnum.Entity0001), em));
            childEntitiesMap.put(DerivedIdentityEntityEnum.Entity0402,
                                 createDIEEChildEntity(DerivedIdentityEntityEnum.Entity0402, parentEntitiesMap.get(DerivedIdentityEntityEnum.Entity0002), em));
            childEntitiesMap.put(DerivedIdentityEntityEnum.Entity0403,
                                 createDIEEChildEntity(DerivedIdentityEntityEnum.Entity0403, parentEntitiesMap.get(DerivedIdentityEntityEnum.Entity0003), em));
            childEntitiesMap.put(DerivedIdentityEntityEnum.Entity0404,
                                 createDIEEChildEntity(DerivedIdentityEntityEnum.Entity0404, parentEntitiesMap.get(DerivedIdentityEntityEnum.Entity0004), em));
            childEntitiesMap.put(DerivedIdentityEntityEnum.Entity0405,
                                 createDIEEChildEntity(DerivedIdentityEntityEnum.Entity0405, parentEntitiesMap.get(DerivedIdentityEntityEnum.Entity0005), em));
            childEntitiesMap.put(DerivedIdentityEntityEnum.Entity0406,
                                 createDIEEChildEntity(DerivedIdentityEntityEnum.Entity0406, parentEntitiesMap.get(DerivedIdentityEntityEnum.Entity0006), em));
            childEntitiesMap.put(DerivedIdentityEntityEnum.Entity0407,
                                 createDIEEChildEntity(DerivedIdentityEntityEnum.Entity0407, parentEntitiesMap.get(DerivedIdentityEntityEnum.Entity0007), em));
            childEntitiesMap.put(DerivedIdentityEntityEnum.Entity0408,
                                 createDIEEChildEntity(DerivedIdentityEntityEnum.Entity0408, parentEntitiesMap.get(DerivedIdentityEntityEnum.Entity0008), em));
            childEntitiesMap.put(DerivedIdentityEntityEnum.Entity0409,
                                 createDIEEChildEntity(DerivedIdentityEntityEnum.Entity0409, parentEntitiesMap.get(DerivedIdentityEntityEnum.Entity0009), em));
            childEntitiesMap.put(DerivedIdentityEntityEnum.Entity0410,
                                 createDIEEChildEntity(DerivedIdentityEntityEnum.Entity0410, parentEntitiesMap.get(DerivedIdentityEntityEnum.Entity0010), em));
            childEntitiesMap.put(DerivedIdentityEntityEnum.Entity0411,
                                 createDIEEChildEntity(DerivedIdentityEntityEnum.Entity0411, parentEntitiesMap.get(DerivedIdentityEntityEnum.Entity0011), em));
            childEntitiesMap.put(DerivedIdentityEntityEnum.Entity0412,
                                 createDIEEChildEntity(DerivedIdentityEntityEnum.Entity0412, parentEntitiesMap.get(DerivedIdentityEntityEnum.Entity0012), em));
            childEntitiesMap.put(DerivedIdentityEntityEnum.Entity0413,
                                 createDIEEChildEntity(DerivedIdentityEntityEnum.Entity0413, parentEntitiesMap.get(DerivedIdentityEntityEnum.Entity0013), em));
            childEntitiesMap.put(DerivedIdentityEntityEnum.Entity0414,
                                 createDIEEChildEntity(DerivedIdentityEntityEnum.Entity0414, parentEntitiesMap.get(DerivedIdentityEntityEnum.Entity0014), em));
            childEntitiesMap.put(DerivedIdentityEntityEnum.Entity0415,
                                 createDIEEChildEntity(DerivedIdentityEntityEnum.Entity0415, parentEntitiesMap.get(DerivedIdentityEntityEnum.Entity0015), em));
            childEntitiesMap.put(DerivedIdentityEntityEnum.Entity0416,
                                 createDIEEChildEntity(DerivedIdentityEntityEnum.Entity0416, parentEntitiesMap.get(DerivedIdentityEntityEnum.Entity0016), em));
            childEntitiesMap.put(DerivedIdentityEntityEnum.Entity0417,
                                 createDIEEChildEntity(DerivedIdentityEntityEnum.Entity0417, parentEntitiesMap.get(DerivedIdentityEntityEnum.Entity0017), em));
            childEntitiesMap.put(DerivedIdentityEntityEnum.Entity0418,
                                 createDIEEChildEntity(DerivedIdentityEntityEnum.Entity0418, parentEntitiesMap.get(DerivedIdentityEntityEnum.Entity0018), em));
            childEntitiesMap.put(DerivedIdentityEntityEnum.Entity0419,
                                 createDIEEChildEntity(DerivedIdentityEntityEnum.Entity0419, parentEntitiesMap.get(DerivedIdentityEntityEnum.Entity0019), em));

            System.out.println("Committing transaction...");
            jpaResource.getTj().commitTransaction();

            System.out.println("########################################################################################");
            System.out.println("##                                                                                    ##");
            System.out.println("## Find the parent/child entities in the database using the id of the parent          ##");
            System.out.println("##                                                                                    ##");
            System.out.println("########################################################################################");

            // Clear persistence context
            System.out.println("Clearing persistence context...");
            em.clear();

            System.out.println("Beginning new transaction...");
            jpaResource.getTj().beginTransaction();
            if (jpaResource.getTj().isApplicationManaged()) {
                System.out.println("Joining entitymanager to JTA transaction...");
                em.joinTransaction();
            }

            // Verify Parent Entities ( 19 * 7 = 133 points)
            for (DerivedIdentityEntityEnum diee : parentEntitiesMap.keySet()) {
                Object origEntity = parentEntitiesMap.get(diee);
                assertParentEntityPersistenceState(diee, origEntity, em);
            }

            // Verify Child Entities ( 19 * 7 = 133 points)
            for (DerivedIdentityEntityEnum diee : childEntitiesMap.keySet()) {
                Object origEntity = childEntitiesMap.get(diee);
                assertChildEntityPersistenceState(diee, origEntity, em);
            }

            System.out.println("Ending test.");
        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            System.out.println("DerivedIdentityTestLogic.testScenario01(): End");
        }
    }

    private Object createDIEEParentEntity(DerivedIdentityEntityEnum diee, Object pkey, Class pkeyType,
                                          EntityManager em) throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Class entityType = resolveEntityClass(diee);
        String entityNumber = diee.getEntityName().substring("Entity".length());

        Method setIdMethod = getMethod(entityType, "setEntity" + entityNumber + "_id", null); // entityType.getMethod("setEntity" + entityNumber + "_id", pkeyType);
        Method setStr1Method = getMethod(entityType, "setEntity" + entityNumber + "_string01", null); //entityType.getMethod("setEntity" + entityNumber + "_string01", String.class);
        Method setStr2Method = getMethod(entityType, "setEntity" + entityNumber + "_string02", null); //entityType.getMethod("setEntity" + entityNumber + "_string02", String.class);
        Method setStr3Method = getMethod(entityType, "setEntity" + entityNumber + "_string03", null); //entityType.getMethod("setEntity" + entityNumber + "_string03", String.class);

        Object newEntity = constructNewEntityObject(diee);

        setIdMethod.invoke(newEntity, pkey);
        setStr1Method.invoke(newEntity, "ENTITY" + entityNumber + "_STRING01");
        setStr2Method.invoke(newEntity, "ENTITY" + entityNumber + "_STRING02");
        setStr3Method.invoke(newEntity, "ENTITY" + entityNumber + "_STRING03");

        em.persist(newEntity);

        return newEntity;
    }

    private Object createDIEEChildEntity(DerivedIdentityEntityEnum diee, Object parent,
                                         EntityManager em) throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Class entityType = resolveEntityClass(diee);
        String entityNumber = diee.getEntityName().substring("Entity".length());

        Method setIdMethod = getMethod(entityType, "setEntity" + entityNumber + "_id", null); // entityType.getMethod("setEntity" + entityNumber + "_id", parent.getClass());
        Method setStr1Method = getMethod(entityType, "setEntity" + entityNumber + "_string01", null); //entityType.getMethod("setEntity" + entityNumber + "_string01", String.class);
        Method setStr2Method = getMethod(entityType, "setEntity" + entityNumber + "_string02", null); //entityType.getMethod("setEntity" + entityNumber + "_string02", String.class);
        Method setStr3Method = getMethod(entityType, "setEntity" + entityNumber + "_string03", null); //entityType.getMethod("setEntity" + entityNumber + "_string03", String.class);

        Object newEntity = constructNewEntityObject(diee);

        setIdMethod.invoke(newEntity, parent);
        setStr1Method.invoke(newEntity, "ENTITY" + entityNumber + "_STRING01");
        setStr2Method.invoke(newEntity, "ENTITY" + entityNumber + "_STRING02");
        setStr3Method.invoke(newEntity, "ENTITY" + entityNumber + "_STRING03");

        em.persist(newEntity);

        return newEntity;
    }

    /*
     * Total Points: 7
     */
    private void assertParentEntityPersistenceState(DerivedIdentityEntityEnum diee, Object original,
                                                    EntityManager em) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, SecurityException, NoSuchMethodException {
        System.out.println("Testing " + diee.getEntityName() + " ...");
        Class entityType = resolveEntityClass(diee);
        String entityNumber = diee.getEntityName().substring("Entity".length());

        Method getIdMethod = getMethod(entityType, "getEntity" + entityNumber + "_id", null); // entityType.getMethod("getEntity" + entityNumber + "_id", entityType);
        Method getStr1Method = getMethod(entityType, "getEntity" + entityNumber + "_string01", null); // entityType.getMethod("getEntity" + entityNumber + "_string01", String.class);
        Method getStr2Method = getMethod(entityType, "getEntity" + entityNumber + "_string02", null); // entityType.getMethod("getEntity" + entityNumber + "_string02", String.class);
        Method getStr3Method = getMethod(entityType, "getEntity" + entityNumber + "_string03", null); // entityType.getMethod("getEntity" + entityNumber + "_string03", String.class);

        // If the Entity is a 'parent' entity (Entity00xx), then use the pk value from the original object.
        // If the Entity is a 'child' entity (Entity04xx), then the pk from the associate original parent needs
        // to be extracted to perform the find.
        Object pk = getIdMethod.invoke(original);

        System.out.println("Invoking em.find() for " + entityType + " pk=" + pk + " ...");
        Object emFind = em.find(resolveEntityClass(diee), pk);

        Assert.assertNotNull("Assert that em.find() returned an object.", emFind);
        if (emFind == null) {
            return;
        }
        Assert.assertNotSame("Assert that em.find() did not return the original object.", original, emFind);
        Assert.assertTrue("Assert that the entity is managed by the PCtx.", em.contains(emFind));

        Assert.assertTrue(
                          "Assert that the pk is equal to " + pk + " (is " + getIdMethod.invoke(emFind) + ")",
                          pk.equals(getIdMethod.invoke(emFind)));

        Assert.assertTrue("Assert that pc-field Entity" + entityNumber + "_string01 has the expected value.",
                          getStr1Method.invoke(original).equals(getStr1Method.invoke(emFind)));
        Assert.assertTrue("Assert that pc-field Entity" + entityNumber + "_string02 has the expected value.",
                          getStr2Method.invoke(original).equals(getStr2Method.invoke(emFind)));
        Assert.assertTrue("Assert that pc-field Entity" + entityNumber + "_string03 has the expected value.",
                          getStr3Method.invoke(original).equals(getStr3Method.invoke(emFind)));

    }

    /*
     * Total Points: 7
     */
    private void assertChildEntityPersistenceState(DerivedIdentityEntityEnum diee, Object original,
                                                   EntityManager em) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, SecurityException, NoSuchMethodException {
        System.out.println("Testing " + diee.getEntityName() + " ...");

        Class entityType = resolveEntityClass(diee);
        String entityNumber = diee.getEntityName().substring("Entity".length());

        // Derive the parent entity type from the child entity type
        char[] entityName = diee.getEntityName().toCharArray();
        char[] childEntityName = Arrays.copyOf(entityName, entityName.length);
        childEntityName[7] = '0';

        DerivedIdentityEntityEnum pdiee = DerivedIdentityEntityEnum.valueOf(new String(childEntityName));
        String parentEntityNumber = pdiee.getEntityName().substring("Entity".length());
        Class pClass = resolveEntityClass(pdiee);

        Method getIdMethod = getMethod(entityType, "getEntity" + entityNumber + "_id", null); // entityType.getMethod("getEntity" + entityNumber + "_id", entityType);
        Method parentGetIdMethod = getMethod(pClass, "getEntity" + parentEntityNumber + "_id", null);

        Method getStr1Method = getMethod(entityType, "getEntity" + entityNumber + "_string01", null); // entityType.getMethod("getEntity" + entityNumber + "_string01", String.class);
        Method getStr2Method = getMethod(entityType, "getEntity" + entityNumber + "_string02", null); // entityType.getMethod("getEntity" + entityNumber + "_string02", String.class);
        Method getStr3Method = getMethod(entityType, "getEntity" + entityNumber + "_string03", null); // entityType.getMethod("getEntity" + entityNumber + "_string03", String.class);

        Object oid = getIdMethod.invoke(original);
        Object pk = parentGetIdMethod.invoke(oid);

        System.out.println("Invoking em.find() for " + entityType + " pk=" + pk + " ...");
        Object emFind = em.find(resolveEntityClass(diee), pk);

        Assert.assertNotNull("Assert that em.find() returned an object.", emFind);
        if (emFind == null) {
            return;
        }
        Assert.assertNotSame("Assert that em.find() did not return the original object.", original, emFind);
        Assert.assertTrue("Assert that the entity is managed by the PCtx.", em.contains(emFind));

        Object emFindOid = getIdMethod.invoke(emFind);
        Object emFindPk = parentGetIdMethod.invoke(emFindOid);

        Assert.assertTrue("Assert that the pk is equal to " + pk + " (is " + emFindPk + ")", pk.equals(emFindPk));

        Assert.assertTrue("Assert that pc-field Entity" + entityNumber + "_string01 has the expected value.",
                          getStr1Method.invoke(original).equals(getStr1Method.invoke(emFind)));
        Assert.assertTrue("Assert that pc-field Entity" + entityNumber + "_string02 has the expected value.",
                          getStr2Method.invoke(original).equals(getStr2Method.invoke(emFind)));
        Assert.assertTrue("Assert that pc-field Entity" + entityNumber + "_string03 has the expected value.",
                          getStr3Method.invoke(original).equals(getStr3Method.invoke(emFind)));
    }

    private Method getMethod(Class cls, String methodName, List<Class> argsTypes) {
        if (cls == null || methodName == null || methodName.isEmpty()) {
            return null;
        }

        Method[] methods = cls.getMethods();
        for (Method method : methods) {
            if (!method.getName().equals(methodName)) {
                continue;
            }

            if (argsTypes == null) {
                // Not caring about arguments, just return the first method whose name matches.  Use with care.
                return method;
            }

            Class[] argsTypesArr = (Class[]) argsTypes.toArray();
            Class<?>[] argumentArr = method.getParameterTypes();

            // Test for method with no arguments
            if (argumentArr.length == 0 && argsTypesArr.length == 0) {
                return method;
            }

            if (Arrays.equals(argumentArr, argsTypesArr)) {
                return method;
            }
        }

        return null;
    }
}
