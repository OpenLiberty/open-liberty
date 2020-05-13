/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.jpa20.querylockmode.testlogic;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.persistence.LockModeType;
import javax.persistence.Query;

import org.junit.Assert;

import com.ibm.ws.jpa.fvt.jpa20.querylockmode.model.*;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.testlogic.AbstractTestLogic;
import com.ibm.ws.testtooling.vehicle.resources.JPAResource;
import com.ibm.ws.testtooling.vehicle.resources.TestExecutionResources;

public class QueryLockModeTestLogic extends AbstractTestLogic {
    @SuppressWarnings("deprecation")
    private java.util.Date javaUtilDate1 = new java.util.Date(50, 19, 20);
    @SuppressWarnings("deprecation")
    private java.util.Date javaUtilDate2 = new java.util.Date(50, 20, 21);
    @SuppressWarnings("deprecation")
    private java.util.Date javaUtilDate3 = new java.util.Date(50, 21, 22);
    @SuppressWarnings("deprecation")
    private java.sql.Date javaSqlDate1 = new java.sql.Date(51, 20, 21);
    @SuppressWarnings("deprecation")
    private java.sql.Date javaSqlDate2 = new java.sql.Date(51, 21, 22);
    @SuppressWarnings("deprecation")
    private java.sql.Date javaSqlDate3 = new java.sql.Date(51, 22, 23);

    private static byte ByteOffset = 123;

    /**
     * Test Logic: testScenario01
     *
     * Description: Test various Query LockMode scenarios using the permutations below. The general idea is to iterate
     * through each of the entities in the Common Datamodel, lock each entity, modify it, then commit the
     * transaction.
     *
     * 01. Single threaded
     * 02. Single entity manager
     * 03. A single transaction for each entity
     * 04. Default locking (i.e., openjpa.LockManager=mixed)
     * 05. All values for LockModeType:
     * READ,
     * WRITE,
     * OPTIMISTIC,
     * OPTIMISTIC_FORCE_INCREMENT,
     * PESSIMISTIC_READ,
     * PESSIMISTIC_WRITE,
     * PESSIMISTIC_FORCE_INCREMENT,
     * NONE
     * 06. All version field types:
     * int
     * Integer
     * long
     * Long
     * short
     * Short
     * Timestamp
     *
     *
     * <p><b>UML:</b>
     *
     * <pre>
     *
     * +--------------+ +--------------+
     * | | | |
     * | Entity0401 |--------------->| Entity0001 |
     * | | 1 1 | |
     * +--------------+ +--------------+
     *
     *
     * +--------------+ +--------------+
     * | | | |
     * | Entity0402 |--------------->| Entity0002 |
     * | | 1 1 | |
     * +--------------+ +--------------+
     *
     *
     * +--------------+ +--------------+
     * | | | |
     * | Entity0403 |--------------->| Entity0004 |
     * | | 1 1 | |
     * +--------------+ +--------------+
     *
     *
     * .
     * .
     * .
     *
     * +--------------+ +--------------+
     * | | | |
     * | Entity0419 |--------------->| Entity0019 |
     * | | 1 1 | |
     * +--------------+ +--------------+
     *
     * .
     * .
     * .
     */

    public void testScenario01(
                               TestExecutionContext testExecCtx,
                               TestExecutionResources testExecResources,
                               Object managedComponentObject) {
        // Verify parameters
        if (testExecCtx == null || testExecResources == null) {
            Assert.fail("QueryLockModeTestLogic.testScenario01(): Missing context and/or resources.  Cannot execute the test.");
            return;
        }

        JPAResource jpaResource = testExecResources.getJpaResourceMap().get("test-jpa-resource");
        if (jpaResource == null) {
            Assert.fail("Missing JPAResource 'test-jpa-resource').  Cannot execute the test.");
            return;
        }

        final Class<?> delegateClass = jpaResource.getEm().getDelegate().getClass();
        final String delegateClassStr = delegateClass.getName().toLowerCase();
        final boolean isOpenJPA = delegateClassStr.contains("org.apache.openjpa") || delegateClassStr.contains("com.ibm.websphere.persistence")
                                  || delegateClassStr.contains("com.ibm.ws.persistence");
        System.out.println("isOpenJPA = " + isOpenJPA);

        String lockModeTypeStr = (String) testExecCtx.getProperties().get("LockModeType");
        LockModeType lockModeType = LockModeType.valueOf(lockModeTypeStr);

        // Execute Test Case
        try {
            System.out.println("QueryLockModeTestLogic.testScenario01(): Begin");

//            for (LockModeType lockModeType : LockModeType.values()) {

            System.out.println("####################################################################################");
            System.out.println("##                                                                                ##");
            System.out.println("## Initialize database before executing this testcase                             ##");
            System.out.println("##                                                                                ##");
            System.out.println("####################################################################################");
//                initializeDatabase(testExecCtx, testExecResources, managedComponentObject);

            System.out.println("####################################################################################");
            System.out.println("##                                                                                ##");
            System.out.println("## LockModeType." + lockModeType);
            System.out.println("##                                                                                ##");
            System.out.println("####################################################################################");

            Entity0001 findEntity0001 = null;
            Entity0002 findEntity0002 = null;
            Entity0003 findEntity0003 = null;
            Entity0004 findEntity0004 = null;
            Entity0005 findEntity0005 = null;
            Entity0006 findEntity0006 = null;
            Entity0007 findEntity0007 = null;
            Entity0008 findEntity0008 = null;
            Entity0009 findEntity0009 = null;
            Entity0010 findEntity0010 = null;
            Entity0011 findEntity0011 = null;
            Entity0012 findEntity0012 = null;
            Entity0013 findEntity0013 = null;
            Entity0014 findEntity0014 = null;
            Entity0015 findEntity0015 = null;
            Entity0016 findEntity0016 = null;
            Entity0017 findEntity0017 = null;
            Entity0018 findEntity0018 = null;
            Entity0019 findEntity0019 = null;

            for (QueryLockModeEntityEnum entity : QueryLockModeEntityEnum.values()) {

                System.out.println("Clearing persistence context...");
                jpaResource.getEm().clear();
                // Begin a new transaction, to ensure the entity returned by find is managed
                // by the persistence context in all environments, including CM-TS.
                System.out.println("Beginning new transaction for: " + entity);
                jpaResource.getTj().beginTransaction();
                if (jpaResource.getTj().isApplicationManaged()) {
                    System.out.println("Joining entitymanager to JTA transaction...");
                    jpaResource.getEm().joinTransaction();
                }

                switch (entity) {

                    case Entity0001:
                        Query selectEntity0001 = jpaResource.getEm().createQuery("SELECT e FROM Entity0001 e WHERE e.entity0001_id = :id_0001");
                        selectEntity0001.setParameter("id_0001", (byte) 01);
                        findEntity0001 = (Entity0001) selectEntity0001.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0001);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0001);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0001));
                        Assert.assertEquals("Assert for the entity id", findEntity0001.getEntity0001_id(), (byte) 01);
                        Assert.assertEquals("Assert for the entity fields", "ENTITY0001_STRING01", findEntity0001.getEntity0001_string01());
                        Assert.assertEquals("Assert for the entity fields", "ENTITY0001_STRING02", findEntity0001.getEntity0001_string02());
                        Assert.assertEquals("Assert for the entity fields", "ENTITY0001_STRING03", findEntity0001.getEntity0001_string03());

                        //
                        // Update, commit, verify
                        //
                        Query updateEntity0001 = jpaResource.getEm()
                                        .createQuery("UPDATE Entity0001 e SET e.entity0001_string01 = :string01_0001, e.entity0001_string03 = :string03_0001 WHERE e.entity0001_id = :id_0001");
                        updateEntity0001.setParameter("id_0001", (byte) 01);
                        updateEntity0001.setParameter("string01_0001", "ENTITY0001_STRING01_UPDATED");
                        updateEntity0001.setParameter("string03_0001", "ENTITY0001_STRING03_UPDATED");
                        if (isOpenJPA)
                            updateEntity0001.setLockMode(lockModeType);
                        updateEntity0001.executeUpdate();
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        /* d643829 */ jpaResource.getEm().refresh(findEntity0001);
                        selectEntity0001.setLockMode(LockModeType.NONE);
                        findEntity0001 = null;
                        findEntity0001 = (Entity0001) selectEntity0001.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0001);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0001);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0001));
                        Assert.assertEquals("Assert for the entity id", findEntity0001.getEntity0001_id(), (byte) 01);
                        Assert.assertEquals("Assert for the entity fields", "ENTITY0001_STRING01_UPDATED", findEntity0001.getEntity0001_string01());
                        Assert.assertEquals("Assert for the entity fields", "ENTITY0001_STRING02", findEntity0001.getEntity0001_string02());
                        Assert.assertEquals("Assert for the entity fields", "ENTITY0001_STRING03_UPDATED", findEntity0001.getEntity0001_string03());
                        break;

                    case Entity0401:
                        Query selectEntity0401 = jpaResource.getEm().createQuery("SELECT e FROM Entity0401 e WHERE e.entity0401_id = :id_0401");
                        selectEntity0401.setParameter("id_0401", findEntity0001);
                        Entity0401 findEntity0401 = (Entity0401) selectEntity0401.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0401);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0401);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0401));
                        Assert.assertEquals("Assert for the entity id", findEntity0401.getEntity0401_id().getEntity0001_id(), (byte) 01);
                        Assert.assertEquals("Assert for the entity fields", findEntity0401.getEntity0401_string01(), "ENTITY0401_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0401.getEntity0401_string02(), "ENTITY0401_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0401.getEntity0401_string03(), "ENTITY0401_STRING03");
                        //
                        // Update, commit, verify
                        //
                        selectEntity0401.setLockMode(lockModeType);
                        findEntity0401.setEntity0401_string01("ENTITY0401_STRING01_UPDATED");
                        selectEntity0401.setLockMode(lockModeType);
                        findEntity0401.setEntity0401_string03("ENTITY0401_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0401.setLockMode(LockModeType.NONE);
                        findEntity0401 = null;
                        findEntity0401 = (Entity0401) selectEntity0401.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0401);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0401);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0401));
                        Assert.assertEquals("Assert for the entity id", findEntity0401.getEntity0401_id().getEntity0001_id(), (byte) 01);
                        Assert.assertEquals("Assert for the entity fields", findEntity0401.getEntity0401_string01(), "ENTITY0401_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0401.getEntity0401_string02(), "ENTITY0401_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0401.getEntity0401_string03(), "ENTITY0401_STRING03_UPDATED");
                        break;

                    case Entity0002:
                        Query selectEntity0002 = jpaResource.getEm().createQuery("SELECT e FROM Entity0002 e WHERE e.entity0002_id = :id_0002");
                        selectEntity0002.setParameter("id_0002", (byte) 02);
                        findEntity0002 = (Entity0002) selectEntity0002.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0002);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0002);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0002));
                        Assert.assertEquals("Assert for the entity id", (byte) findEntity0002.getEntity0002_id(), (byte) 02);
                        Assert.assertEquals("Assert for the entity fields", findEntity0002.getEntity0002_string01(), "ENTITY0002_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0002.getEntity0002_string02(), "ENTITY0002_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0002.getEntity0002_string03(), "ENTITY0002_STRING03");
                        //
                        // Update, commit, verify
                        //
                        Query updateEntity0002 = jpaResource.getEm()
                                        .createQuery("UPDATE Entity0002 e SET e.entity0002_string01 = :string01_0002, e.entity0002_string03 = :string03_0002 WHERE e.entity0002_id = :id_0002");
                        updateEntity0002.setParameter("id_0002", (byte) 02);
                        updateEntity0002.setParameter("string01_0002", "ENTITY0002_STRING01_UPDATED");
                        updateEntity0002.setParameter("string03_0002", "ENTITY0002_STRING03_UPDATED");
                        if (isOpenJPA)
                            updateEntity0002.setLockMode(lockModeType);
                        updateEntity0002.executeUpdate();
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        /* d643829 */ jpaResource.getEm().refresh(findEntity0002);
                        selectEntity0002.setLockMode(LockModeType.NONE);
                        findEntity0002 = null;
                        findEntity0002 = (Entity0002) selectEntity0002.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0002);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0002);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0002));
                        Assert.assertEquals("Assert for the entity id", (byte) findEntity0002.getEntity0002_id(), (byte) 02);
                        Assert.assertEquals("Assert for the entity fields", findEntity0002.getEntity0002_string01(), "ENTITY0002_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0002.getEntity0002_string02(), "ENTITY0002_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0002.getEntity0002_string03(), "ENTITY0002_STRING03_UPDATED");
                        break;

                    case Entity0402:
                        Query selectEntity0402 = jpaResource.getEm().createQuery("SELECT e FROM Entity0402 e WHERE e.entity0402_id = :id_0402");
                        selectEntity0402.setParameter("id_0402", findEntity0002);
                        Entity0402 findEntity0402 = (Entity0402) selectEntity0402.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0402);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0402);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0402));
                        Assert.assertEquals("Assert for the entity id", (byte) findEntity0402.getEntity0402_id().getEntity0002_id(), (byte) 02);
                        Assert.assertEquals("Assert for the entity fields", findEntity0402.getEntity0402_string01(), "ENTITY0402_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0402.getEntity0402_string02(), "ENTITY0402_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0402.getEntity0402_string03(), "ENTITY0402_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0402.setEntity0402_string01("ENTITY0402_STRING01_UPDATED");
                        findEntity0402.setEntity0402_string03("ENTITY0402_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0402.setLockMode(LockModeType.NONE);
                        findEntity0402 = null;
                        findEntity0402 = (Entity0402) selectEntity0402.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0402);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0402);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0402));
                        Assert.assertEquals("Assert for the entity id", (byte) findEntity0402.getEntity0402_id().getEntity0002_id(), (byte) 02);
                        Assert.assertEquals("Assert for the entity fields", findEntity0402.getEntity0402_string01(), "ENTITY0402_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0402.getEntity0402_string02(), "ENTITY0402_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0402.getEntity0402_string03(), "ENTITY0402_STRING03_UPDATED");
                        break;

                    case Entity0003:
                        Query selectEntity0003 = jpaResource.getEm().createQuery("SELECT e FROM Entity0003 e WHERE e.entity0003_id = :id_0003");
                        selectEntity0003.setParameter("id_0003", '3');
                        findEntity0003 = (Entity0003) selectEntity0003.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0003);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0003);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0003));
                        Assert.assertEquals("Assert for the entity id", findEntity0003.getEntity0003_id(), '3');
                        Assert.assertEquals("Assert for the entity fields", findEntity0003.getEntity0003_string01(), "ENTITY0003_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0003.getEntity0003_string02(), "ENTITY0003_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0003.getEntity0003_string03(), "ENTITY0003_STRING03");
                        //
                        // Update, commit, verify
                        //
                        Query updateEntity0003 = jpaResource.getEm()
                                        .createQuery("UPDATE Entity0003 e SET e.entity0003_string01 = :string01_0003, e.entity0003_string03 = :string03_0003 WHERE e.entity0003_id = :id_0003");
                        updateEntity0003.setParameter("id_0003", '3');
                        updateEntity0003.setParameter("string01_0003", "ENTITY0003_STRING01_UPDATED");
                        updateEntity0003.setParameter("string03_0003", "ENTITY0003_STRING03_UPDATED");
                        if (isOpenJPA)
                            updateEntity0003.setLockMode(lockModeType);
                        updateEntity0003.executeUpdate();
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        /* d643829 */ jpaResource.getEm().refresh(findEntity0003);
                        selectEntity0003.setLockMode(LockModeType.NONE);
                        findEntity0003 = null;
                        findEntity0003 = (Entity0003) selectEntity0003.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0003);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0003);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0003));
                        Assert.assertEquals("Assert for the entity id", findEntity0003.getEntity0003_id(), '3');
                        Assert.assertEquals("Assert for the entity fields", findEntity0003.getEntity0003_string01(), "ENTITY0003_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0003.getEntity0003_string02(), "ENTITY0003_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0003.getEntity0003_string03(), "ENTITY0003_STRING03_UPDATED");
                        break;

                    case Entity0403:
                        Query selectEntity0403 = jpaResource.getEm().createQuery("SELECT e FROM Entity0403 e WHERE e.entity0403_id = :id_0403");
                        selectEntity0403.setParameter("id_0403", findEntity0003);
                        Entity0403 findEntity0403 = (Entity0403) selectEntity0403.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0403);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0403);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0403));
                        Assert.assertEquals("Assert for the entity id", findEntity0403.getEntity0403_id().getEntity0003_id(), '3');
                        Assert.assertEquals("Assert for the entity fields", findEntity0403.getEntity0403_string01(), "ENTITY0403_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0403.getEntity0403_string02(), "ENTITY0403_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0403.getEntity0403_string03(), "ENTITY0403_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0403.setEntity0403_string01("ENTITY0403_STRING01_UPDATED");
                        findEntity0403.setEntity0403_string03("ENTITY0403_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0403.setLockMode(LockModeType.NONE);
                        findEntity0403 = null;
                        findEntity0403 = (Entity0403) selectEntity0403.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0403);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0403);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0403));
                        Assert.assertEquals("Assert for the entity id", findEntity0403.getEntity0403_id().getEntity0003_id(), '3');
                        Assert.assertEquals("Assert for the entity fields", findEntity0403.getEntity0403_string01(), "ENTITY0403_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0403.getEntity0403_string02(), "ENTITY0403_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0403.getEntity0403_string03(), "ENTITY0403_STRING03_UPDATED");
                        break;

                    case Entity0004:
                        Query selectEntity0004 = jpaResource.getEm().createQuery("SELECT e FROM Entity0004 e WHERE e.entity0004_id = :id_0004");
                        selectEntity0004.setParameter("id_0004", '4');
                        findEntity0004 = (Entity0004) selectEntity0004.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0004);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0004);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0004));
                        Assert.assertEquals("Assert for the entity id", findEntity0004.getEntity0004_id(), new Character('4'));
                        Assert.assertEquals("Assert for the entity fields", findEntity0004.getEntity0004_string01(), "ENTITY0004_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0004.getEntity0004_string02(), "ENTITY0004_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0004.getEntity0004_string03(), "ENTITY0004_STRING03");
                        //
                        // Update, commit, verify
                        //
                        Query updateEntity0004 = jpaResource.getEm()
                                        .createQuery("UPDATE Entity0004 e SET e.entity0004_string01 = :string01_0004, e.entity0004_string03 = :string03_0004 WHERE e.entity0004_id = :id_0004");
                        updateEntity0004.setParameter("id_0004", '4');
                        updateEntity0004.setParameter("string01_0004", "ENTITY0004_STRING01_UPDATED");
                        updateEntity0004.setParameter("string03_0004", "ENTITY0004_STRING03_UPDATED");
                        if (isOpenJPA)
                            updateEntity0004.setLockMode(lockModeType);
                        updateEntity0004.executeUpdate();
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        /* d643829 */ jpaResource.getEm().refresh(findEntity0004);
                        selectEntity0004.setLockMode(LockModeType.NONE);
                        findEntity0004 = null;
                        findEntity0004 = (Entity0004) selectEntity0004.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0004);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0004);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0004));
                        Assert.assertEquals("Assert for the entity id", findEntity0004.getEntity0004_id(), new Character('4'));
                        Assert.assertEquals("Assert for the entity fields", findEntity0004.getEntity0004_string01(), "ENTITY0004_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0004.getEntity0004_string02(), "ENTITY0004_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0004.getEntity0004_string03(), "ENTITY0004_STRING03_UPDATED");
                        break;

                    case Entity0404:
                        Query selectEntity0404 = jpaResource.getEm().createQuery("SELECT e FROM Entity0404 e WHERE e.entity0404_id = :id_0404");
                        selectEntity0404.setParameter("id_0404", findEntity0004);
                        Entity0404 findEntity0404 = (Entity0404) selectEntity0404.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0404);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0404);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0404));
                        Assert.assertEquals("Assert for the entity id", findEntity0404.getEntity0404_id().getEntity0004_id(), new Character('4'));
                        Assert.assertEquals("Assert for the entity fields", findEntity0404.getEntity0404_string01(), "ENTITY0404_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0404.getEntity0404_string02(), "ENTITY0404_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0404.getEntity0404_string03(), "ENTITY0404_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0404.setEntity0404_string01("ENTITY0404_STRING01_UPDATED");
                        findEntity0404.setEntity0404_string03("ENTITY0404_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0404.setLockMode(LockModeType.NONE);
                        findEntity0404 = null;
                        findEntity0404 = (Entity0404) selectEntity0404.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0404);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0404);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0404));
                        Assert.assertEquals("Assert for the entity id", findEntity0404.getEntity0404_id().getEntity0004_id(), new Character('4'));
                        Assert.assertEquals("Assert for the entity fields", findEntity0404.getEntity0404_string01(), "ENTITY0404_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0404.getEntity0404_string02(), "ENTITY0404_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0404.getEntity0404_string03(), "ENTITY0404_STRING03_UPDATED");
                        break;

                    case Entity0005:
                        Query selectEntity0005 = jpaResource.getEm().createQuery("SELECT e FROM Entity0005 e WHERE e.entity0005_id = :id_0005");
                        selectEntity0005.setParameter("id_0005", "ENTITY0005_ID");
                        findEntity0005 = (Entity0005) selectEntity0005.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0005);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0005);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0005));
                        Assert.assertEquals("Assert for the entity id", findEntity0005.getEntity0005_id(), "ENTITY0005_ID");
                        Assert.assertEquals("Assert for the entity fields", findEntity0005.getEntity0005_string01(), "ENTITY0005_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0005.getEntity0005_string02(), "ENTITY0005_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0005.getEntity0005_string03(), "ENTITY0005_STRING03");
                        //
                        // Update, commit, verify
                        //
                        Query updateEntity0005 = jpaResource.getEm()
                                        .createQuery("UPDATE Entity0005 e SET e.entity0005_string01 = :string01_0005, e.entity0005_string03 = :string03_0005 WHERE e.entity0005_id = :id_0005");
                        updateEntity0005.setParameter("id_0005", "ENTITY0005_ID");
                        updateEntity0005.setParameter("string01_0005", "ENTITY0005_STRING01_UPDATED");
                        updateEntity0005.setParameter("string03_0005", "ENTITY0005_STRING03_UPDATED");
                        if (isOpenJPA)
                            updateEntity0005.setLockMode(lockModeType);
                        updateEntity0005.executeUpdate();
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        /* d643829 */ jpaResource.getEm().refresh(findEntity0005);
                        selectEntity0005.setLockMode(LockModeType.NONE);
                        findEntity0005 = null;
                        findEntity0005 = (Entity0005) selectEntity0005.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0005);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0005);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0005));
                        Assert.assertEquals("Assert for the entity id", findEntity0005.getEntity0005_id(), "ENTITY0005_ID");
                        Assert.assertEquals("Assert for the entity fields", findEntity0005.getEntity0005_string01(), "ENTITY0005_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0005.getEntity0005_string02(), "ENTITY0005_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0005.getEntity0005_string03(), "ENTITY0005_STRING03_UPDATED");
                        break;

                    case Entity0405:
                        Query selectEntity0405 = jpaResource.getEm().createQuery("SELECT e FROM Entity0405 e WHERE e.entity0405_id = :id_0405");
                        selectEntity0405.setParameter("id_0405", findEntity0005);
                        Entity0405 findEntity0405 = (Entity0405) selectEntity0405.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0405);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0405);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0405));
                        Assert.assertEquals("Assert for the entity id", findEntity0405.getEntity0405_id().getEntity0005_id(), "ENTITY0005_ID");
                        Assert.assertEquals("Assert for the entity fields", findEntity0405.getEntity0405_string01(), "ENTITY0405_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0405.getEntity0405_string02(), "ENTITY0405_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0405.getEntity0405_string03(), "ENTITY0405_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0405.setEntity0405_string01("ENTITY0405_STRING01_UPDATED");
                        findEntity0405.setEntity0405_string03("ENTITY0405_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0405.setLockMode(LockModeType.NONE);
                        findEntity0405 = null;
                        findEntity0405 = (Entity0405) selectEntity0405.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0405);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0405);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0405));
                        Assert.assertEquals("Assert for the entity id", findEntity0405.getEntity0405_id().getEntity0005_id(), "ENTITY0005_ID");
                        Assert.assertEquals("Assert for the entity fields", findEntity0405.getEntity0405_string01(), "ENTITY0405_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0405.getEntity0405_string02(), "ENTITY0405_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0405.getEntity0405_string03(), "ENTITY0405_STRING03_UPDATED");
                        break;

                    case Entity0006:
                        Query selectEntity0006 = jpaResource.getEm().createQuery("SELECT e FROM Entity0006 e WHERE e.entity0006_id = :id_0006");
                        selectEntity0006.setParameter("id_0006", 0006.0006D);
                        findEntity0006 = (Entity0006) selectEntity0006.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0006);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0006);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0006));
                        Assert.assertEquals("Assert for the entity id", findEntity0006.getEntity0006_id(), 0006.0006D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0006.getEntity0006_string01(), "ENTITY0006_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0006.getEntity0006_string02(), "ENTITY0006_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0006.getEntity0006_string03(), "ENTITY0006_STRING03");
                        //
                        // Update, commit, verify
                        //
                        Query updateEntity0006 = jpaResource.getEm()
                                        .createQuery("UPDATE Entity0006 e SET e.entity0006_string01 = :string01_0006, e.entity0006_string03 = :string03_0006 WHERE e.entity0006_id = :id_0006");
                        updateEntity0006.setParameter("id_0006", 0006.0006D);
                        updateEntity0006.setParameter("string01_0006", "ENTITY0006_STRING01_UPDATED");
                        updateEntity0006.setParameter("string03_0006", "ENTITY0006_STRING03_UPDATED");
                        if (isOpenJPA)
                            updateEntity0006.setLockMode(lockModeType);
                        updateEntity0006.executeUpdate();
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        /* d643829 */ jpaResource.getEm().refresh(findEntity0006);
                        selectEntity0006.setLockMode(LockModeType.NONE);
                        findEntity0006 = null;
                        findEntity0006 = (Entity0006) selectEntity0006.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0006);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0006);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0006));
                        Assert.assertEquals("Assert for the entity id", findEntity0006.getEntity0006_id(), 0006.0006D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0006.getEntity0006_string01(), "ENTITY0006_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0006.getEntity0006_string02(), "ENTITY0006_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0006.getEntity0006_string03(), "ENTITY0006_STRING03_UPDATED");
                        break;

                    case Entity0406:
                        Query selectEntity0406 = jpaResource.getEm().createQuery("SELECT e FROM Entity0406 e WHERE e.entity0406_id = :id_0406");
                        selectEntity0406.setParameter("id_0406", findEntity0006);
                        Entity0406 findEntity0406 = (Entity0406) selectEntity0406.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0406);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0406);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0406));
                        Assert.assertEquals("Assert for the entity id", findEntity0406.getEntity0406_id().getEntity0006_id(), 0006.0006D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0406.getEntity0406_string01(), "ENTITY0406_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0406.getEntity0406_string02(), "ENTITY0406_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0406.getEntity0406_string03(), "ENTITY0406_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0406.setEntity0406_string01("ENTITY0406_STRING01_UPDATED");
                        findEntity0406.setEntity0406_string03("ENTITY0406_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0406.setLockMode(LockModeType.NONE);
                        findEntity0406 = null;
                        findEntity0406 = (Entity0406) selectEntity0406.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0406);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0406);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0406));
                        Assert.assertEquals("Assert for the entity id", findEntity0406.getEntity0406_id().getEntity0006_id(), 0006.0006D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0406.getEntity0406_string01(), "ENTITY0406_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0406.getEntity0406_string02(), "ENTITY0406_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0406.getEntity0406_string03(), "ENTITY0406_STRING03_UPDATED");
                        break;

                    case Entity0007:
                        Query selectEntity0007 = jpaResource.getEm().createQuery("SELECT e FROM Entity0007 e WHERE e.entity0007_id = :id_0007");
                        selectEntity0007.setParameter("id_0007", 0007.0007D);
                        findEntity0007 = (Entity0007) selectEntity0007.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0007);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0007);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0007));
                        Assert.assertEquals("Assert for the entity id", findEntity0007.getEntity0007_id(), 0007.0007D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0007.getEntity0007_string01(), "ENTITY0007_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0007.getEntity0007_string02(), "ENTITY0007_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0007.getEntity0007_string03(), "ENTITY0007_STRING03");
                        //
                        // Update, commit, verify
                        //
                        Query updateEntity0007 = jpaResource.getEm()
                                        .createQuery("UPDATE Entity0007 e SET e.entity0007_string01 = :string01_0007, e.entity0007_string03 = :string03_0007 WHERE e.entity0007_id = :id_0007");
                        updateEntity0007.setParameter("id_0007", 0007.0007D);
                        updateEntity0007.setParameter("string01_0007", "ENTITY0007_STRING01_UPDATED");
                        updateEntity0007.setParameter("string03_0007", "ENTITY0007_STRING03_UPDATED");
                        if (isOpenJPA)
                            updateEntity0007.setLockMode(lockModeType);
                        updateEntity0007.executeUpdate();
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        /* d643829 */ jpaResource.getEm().refresh(findEntity0007);
                        selectEntity0007.setLockMode(LockModeType.NONE);
                        findEntity0007 = null;
                        findEntity0007 = (Entity0007) selectEntity0007.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0007);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0007);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0007));
                        Assert.assertEquals("Assert for the entity id", findEntity0007.getEntity0007_id(), 0007.0007D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0007.getEntity0007_string01(), "ENTITY0007_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0007.getEntity0007_string02(), "ENTITY0007_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0007.getEntity0007_string03(), "ENTITY0007_STRING03_UPDATED");
                        break;

                    case Entity0407:
                        Query selectEntity0407 = jpaResource.getEm().createQuery("SELECT e FROM Entity0407 e WHERE e.entity0407_id = :id_0407");
                        selectEntity0407.setParameter("id_0407", findEntity0007);
                        Entity0407 findEntity0407 = (Entity0407) selectEntity0407.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0407);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0407);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0407));
                        Assert.assertEquals("Assert for the entity id", findEntity0407.getEntity0407_id().getEntity0007_id(), 0007.0007D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0407.getEntity0407_string01(), "ENTITY0407_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0407.getEntity0407_string02(), "ENTITY0407_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0407.getEntity0407_string03(), "ENTITY0407_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0407.setEntity0407_string01("ENTITY0407_STRING01_UPDATED");
                        findEntity0407.setEntity0407_string03("ENTITY0407_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0407.setLockMode(LockModeType.NONE);
                        findEntity0407 = null;
                        findEntity0407 = (Entity0407) selectEntity0407.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0407);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0407);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0407));
                        Assert.assertEquals("Assert for the entity id", findEntity0407.getEntity0407_id().getEntity0007_id(), 0007.0007D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0407.getEntity0407_string01(), "ENTITY0407_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0407.getEntity0407_string02(), "ENTITY0407_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0407.getEntity0407_string03(), "ENTITY0407_STRING03_UPDATED");
                        break;

                    case Entity0008:
                        Query selectEntity0008 = jpaResource.getEm().createQuery("SELECT e FROM Entity0008 e WHERE e.entity0008_id = :id_0008");
                        selectEntity0008.setParameter("id_0008", 0008.0008F);
                        findEntity0008 = (Entity0008) selectEntity0008.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0008);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0008);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0008));
                        Assert.assertEquals("Assert for the entity id", findEntity0008.getEntity0008_id(), 0008.0008F, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0008.getEntity0008_string01(), "ENTITY0008_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0008.getEntity0008_string02(), "ENTITY0008_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0008.getEntity0008_string03(), "ENTITY0008_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0008.setEntity0008_string01("ENTITY0008_STRING01_UPDATED");
                        findEntity0008.setEntity0008_string03("ENTITY0008_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0008.setLockMode(LockModeType.NONE);
                        //d642065                  findEntity0008 = null;
//                          findEntity0008 = (Entity0008) selectEntity0008.getSingleResult();
//                          System.out.println("Object returned by find: " + findEntity0008);
//                          Assert.assertNotNull("Assert that the find operation did not return null", findEntity0008);
//
//                          System.out.println     ( "Perform parent verifications...");
//                          Assert.assertTrue  ( "Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0008));
//                          Assert.assertEquals    ( "Assert for the entity id",     findEntity0008.getEntity0008_id(), 0008.0008F);
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0008.getEntity0008_string01(), "ENTITY0008_STRING01_UPDATED");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0008.getEntity0008_string02(), "ENTITY0008_STRING02");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0008.getEntity0008_string03(), "ENTITY0008_STRING03_UPDATED");
                        break;

                    case Entity0408:
                        Query selectEntity0408 = jpaResource.getEm().createQuery("SELECT e FROM Entity0408 e WHERE e.entity0408_id = :id_0408");
                        selectEntity0408.setParameter("id_0408", findEntity0008);
                        Entity0408 findEntity0408 = (Entity0408) selectEntity0408.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0408);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0408);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0408));
                        Assert.assertEquals("Assert for the entity id", findEntity0408.getEntity0408_id().getEntity0008_id(), 0008.0008F, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0408.getEntity0408_string01(), "ENTITY0408_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0408.getEntity0408_string02(), "ENTITY0408_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0408.getEntity0408_string03(), "ENTITY0408_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0408.setEntity0408_string01("ENTITY0408_STRING01_UPDATED");
                        findEntity0408.setEntity0408_string03("ENTITY0408_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0408.setLockMode(LockModeType.NONE);
                        //d642065                  findEntity0408 = null;
//                          findEntity0408 = (Entity0408) selectEntity0408.getSingleResult();
//                          System.out.println("Object returned by find: " + findEntity0408);
//                          Assert.assertNotNull("Assert that the find operation did not return null", findEntity0408);
//
//                          System.out.println     ( "Perform child verifications...");
//                          Assert.assertTrue  ( "Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0408));
//                          Assert.assertEquals    ( "Assert for the entity id",     findEntity0408.getEntity0408_id().getEntity0008_id(), 0008.0008F);
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0408.getEntity0408_string01(), "ENTITY0408_STRING01_UPDATED");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0408.getEntity0408_string02(), "ENTITY0408_STRING02");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0408.getEntity0408_string03(), "ENTITY0408_STRING03_UPDATED");
                        break;

                    case Entity0009:
                        Query selectEntity0009 = jpaResource.getEm().createQuery("SELECT e FROM Entity0009 e WHERE e.entity0009_id = :id_0009");
                        selectEntity0009.setParameter("id_0009", 0009.0009F);
                        findEntity0009 = (Entity0009) selectEntity0009.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0009);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0009);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0009));
                        Assert.assertEquals("Assert for the entity id", findEntity0009.getEntity0009_id(), 0009.0009F, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0009.getEntity0009_string01(), "ENTITY0009_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0009.getEntity0009_string02(), "ENTITY0009_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0009.getEntity0009_string03(), "ENTITY0009_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0009.setEntity0009_string01("ENTITY0009_STRING01_UPDATED");
                        findEntity0009.setEntity0009_string03("ENTITY0009_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0009.setLockMode(LockModeType.NONE);
                        //d642065                  findEntity0009 = null;
//                          findEntity0009 = (Entity0009) selectEntity0009.getSingleResult();
//                          System.out.println("Object returned by find: " + findEntity0009);
//                          Assert.assertNotNull("Assert that the find operation did not return null", findEntity0009);
//
//                          System.out.println     ( "Perform parent verifications...");
//                          Assert.assertTrue  ( "Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0009));
//                          Assert.assertEquals    ( "Assert for the entity id",     findEntity0009.getEntity0009_id(), 0009.0009F);
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0009.getEntity0009_string01(), "ENTITY0009_STRING01_UPDATED");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0009.getEntity0009_string02(), "ENTITY0009_STRING02");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0009.getEntity0009_string03(), "ENTITY0009_STRING03_UPDATED");
                        break;

                    case Entity0409:
                        Query selectEntity0409 = jpaResource.getEm().createQuery("SELECT e FROM Entity0409 e WHERE e.entity0409_id = :id_0409");
                        selectEntity0409.setParameter("id_0409", findEntity0009);
                        Entity0409 findEntity0409 = (Entity0409) selectEntity0409.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0409);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0409);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0409));
                        Assert.assertEquals("Assert for the entity id", findEntity0409.getEntity0409_id().getEntity0009_id(), 0009.0009F, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0409.getEntity0409_string01(), "ENTITY0409_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0409.getEntity0409_string02(), "ENTITY0409_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0409.getEntity0409_string03(), "ENTITY0409_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0409.setEntity0409_string01("ENTITY0409_STRING01_UPDATED");
                        findEntity0409.setEntity0409_string03("ENTITY0409_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0409.setLockMode(LockModeType.NONE);
                        //d642065                  findEntity0409 = null;
//                          findEntity0409 = (Entity0409) selectEntity0409.getSingleResult();
//                          System.out.println("Object returned by find: " + findEntity0409);
//                          Assert.assertNotNull("Assert that the find operation did not return null", findEntity0409);
//
//                          System.out.println     ( "Perform child verifications...");
//                          Assert.assertTrue  ( "Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0409));
//                          Assert.assertEquals    ( "Assert for the entity id",     findEntity0409.getEntity0409_id().getEntity0009_id(), 0009.0009F);
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0409.getEntity0409_string01(), "ENTITY0409_STRING01_UPDATED");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0409.getEntity0409_string02(), "ENTITY0409_STRING02");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0409.getEntity0409_string03(), "ENTITY0409_STRING03_UPDATED");
                        break;

                    case Entity0010:
                        Query selectEntity0010 = jpaResource.getEm().createQuery("SELECT e FROM Entity0010 e WHERE e.entity0010_id = :id_0010");
                        selectEntity0010.setParameter("id_0010", 10);
                        findEntity0010 = (Entity0010) selectEntity0010.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0010);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0010);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0010));
                        Assert.assertEquals("Assert for the entity id", findEntity0010.getEntity0010_id(), 10);
                        Assert.assertEquals("Assert for the entity fields", findEntity0010.getEntity0010_string01(), "ENTITY0010_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0010.getEntity0010_string02(), "ENTITY0010_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0010.getEntity0010_string03(), "ENTITY0010_STRING03");
                        //
                        // Update, commit, verify
                        //
                        Query updateEntity0010 = jpaResource.getEm()
                                        .createQuery("UPDATE Entity0010 e SET e.entity0010_string01 = :string01_0010, e.entity0010_string03 = :string03_0010 WHERE e.entity0010_id = :id_0010");
                        updateEntity0010.setParameter("id_0010", 10);
                        updateEntity0010.setParameter("string01_0010", "ENTITY0010_STRING01_UPDATED");
                        updateEntity0010.setParameter("string03_0010", "ENTITY0010_STRING03_UPDATED");
                        if (isOpenJPA)
                            updateEntity0010.setLockMode(lockModeType);
                        updateEntity0010.executeUpdate();
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        /* d643829 */ jpaResource.getEm().refresh(findEntity0010);
                        selectEntity0010.setLockMode(LockModeType.NONE);
                        findEntity0010 = null;
                        findEntity0010 = (Entity0010) selectEntity0010.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0010);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0010);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0010));
                        Assert.assertEquals("Assert for the entity id", findEntity0010.getEntity0010_id(), 10);
                        Assert.assertEquals("Assert for the entity fields", findEntity0010.getEntity0010_string01(), "ENTITY0010_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0010.getEntity0010_string02(), "ENTITY0010_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0010.getEntity0010_string03(), "ENTITY0010_STRING03_UPDATED");
                        break;

                    case Entity0410:
                        Query selectEntity0410 = jpaResource.getEm().createQuery("SELECT e FROM Entity0410 e WHERE e.entity0410_id = :id_0410");
                        selectEntity0410.setParameter("id_0410", findEntity0010);
                        Entity0410 findEntity0410 = (Entity0410) selectEntity0410.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0410);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0410);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0410));
                        Assert.assertEquals("Assert for the entity id", findEntity0410.getEntity0410_id().getEntity0010_id(), 10);
                        Assert.assertEquals("Assert for the entity fields", findEntity0410.getEntity0410_string01(), "ENTITY0410_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0410.getEntity0410_string02(), "ENTITY0410_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0410.getEntity0410_string03(), "ENTITY0410_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0410.setEntity0410_string01("ENTITY0410_STRING01_UPDATED");
                        findEntity0410.setEntity0410_string03("ENTITY0410_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0410.setLockMode(LockModeType.NONE);
                        findEntity0410 = null;
                        findEntity0410 = (Entity0410) selectEntity0410.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0410);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0410);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0410));
                        Assert.assertEquals("Assert for the entity id", findEntity0410.getEntity0410_id().getEntity0010_id(), 10);
                        Assert.assertEquals("Assert for the entity fields", findEntity0410.getEntity0410_string01(), "ENTITY0410_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0410.getEntity0410_string02(), "ENTITY0410_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0410.getEntity0410_string03(), "ENTITY0410_STRING03_UPDATED");
                        break;

                    case Entity0011:
                        Query selectEntity0011 = jpaResource.getEm().createQuery("SELECT e FROM Entity0011 e WHERE e.entity0011_id = :id_0011");
                        selectEntity0011.setParameter("id_0011", 11);
                        findEntity0011 = (Entity0011) selectEntity0011.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0011);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0011);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0011));
                        Assert.assertEquals("Assert for the entity id", (int) findEntity0011.getEntity0011_id(), 11);
                        Assert.assertEquals("Assert for the entity fields", findEntity0011.getEntity0011_string01(), "ENTITY0011_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0011.getEntity0011_string02(), "ENTITY0011_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0011.getEntity0011_string03(), "ENTITY0011_STRING03");
                        //
                        // Update, commit, verify
                        //
                        Query updateEntity0011 = jpaResource.getEm()
                                        .createQuery("UPDATE Entity0011 e SET e.entity0011_string01 = :string01_0011, e.entity0011_string03 = :string03_0011 WHERE e.entity0011_id = :id_0011");
                        updateEntity0011.setParameter("id_0011", 11);
                        updateEntity0011.setParameter("string01_0011", "ENTITY0011_STRING01_UPDATED");
                        updateEntity0011.setParameter("string03_0011", "ENTITY0011_STRING03_UPDATED");
                        if (isOpenJPA)
                            updateEntity0011.setLockMode(lockModeType);
                        updateEntity0011.executeUpdate();
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        /* d643829 */ jpaResource.getEm().refresh(findEntity0011);
                        selectEntity0011.setLockMode(LockModeType.NONE);
                        findEntity0011 = null;
                        findEntity0011 = (Entity0011) selectEntity0011.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0011);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0011);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0011));
                        Assert.assertEquals("Assert for the entity id", (int) findEntity0011.getEntity0011_id(), 11);
                        Assert.assertEquals("Assert for the entity fields", findEntity0011.getEntity0011_string01(), "ENTITY0011_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0011.getEntity0011_string02(), "ENTITY0011_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0011.getEntity0011_string03(), "ENTITY0011_STRING03_UPDATED");
                        break;

                    case Entity0411:
                        Query selectEntity0411 = jpaResource.getEm().createQuery("SELECT e FROM Entity0411 e WHERE e.entity0411_id = :id_0411");
                        selectEntity0411.setParameter("id_0411", findEntity0011);
                        Entity0411 findEntity0411 = (Entity0411) selectEntity0411.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0411);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0411);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0411));
                        Assert.assertEquals("Assert for the entity id", (int) findEntity0411.getEntity0411_id().getEntity0011_id(), 11);
                        Assert.assertEquals("Assert for the entity fields", findEntity0411.getEntity0411_string01(), "ENTITY0411_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0411.getEntity0411_string02(), "ENTITY0411_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0411.getEntity0411_string03(), "ENTITY0411_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0411.setEntity0411_string01("ENTITY0411_STRING01_UPDATED");
                        findEntity0411.setEntity0411_string03("ENTITY0411_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0411.setLockMode(LockModeType.NONE);
                        findEntity0411 = null;
                        findEntity0411 = (Entity0411) selectEntity0411.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0411);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0411);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0411));
                        Assert.assertEquals("Assert for the entity id", (int) findEntity0411.getEntity0411_id().getEntity0011_id(), 11);
                        Assert.assertEquals("Assert for the entity fields", findEntity0411.getEntity0411_string01(), "ENTITY0411_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0411.getEntity0411_string02(), "ENTITY0411_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0411.getEntity0411_string03(), "ENTITY0411_STRING03_UPDATED");
                        break;

                    case Entity0012:
                        Query selectEntity0012 = jpaResource.getEm().createQuery("SELECT e FROM Entity0012 e WHERE e.entity0012_id = :id_0012");
                        selectEntity0012.setParameter("id_0012", 12L);
                        findEntity0012 = (Entity0012) selectEntity0012.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0012);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0012);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0012));
                        Assert.assertEquals("Assert for the entity id", findEntity0012.getEntity0012_id(), 12L);
                        Assert.assertEquals("Assert for the entity fields", findEntity0012.getEntity0012_string01(), "ENTITY0012_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0012.getEntity0012_string02(), "ENTITY0012_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0012.getEntity0012_string03(), "ENTITY0012_STRING03");
                        //
                        // Update, commit, verify
                        //
                        Query updateEntity0012 = jpaResource.getEm()
                                        .createQuery("UPDATE Entity0012 e SET e.entity0012_string01 = :string01_0012, e.entity0012_string03 = :string03_0012 WHERE e.entity0012_id = :id_0012");
                        updateEntity0012.setParameter("id_0012", 12L);
                        updateEntity0012.setParameter("string01_0012", "ENTITY0012_STRING01_UPDATED");
                        updateEntity0012.setParameter("string03_0012", "ENTITY0012_STRING03_UPDATED");
                        if (isOpenJPA)
                            updateEntity0012.setLockMode(lockModeType);
                        updateEntity0012.executeUpdate();
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        /* d643829 */ jpaResource.getEm().refresh(findEntity0012);
                        selectEntity0012.setLockMode(LockModeType.NONE);
                        findEntity0012 = null;
                        findEntity0012 = (Entity0012) selectEntity0012.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0012);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0012);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0012));
                        Assert.assertEquals("Assert for the entity id", findEntity0012.getEntity0012_id(), 12L);
                        Assert.assertEquals("Assert for the entity fields", findEntity0012.getEntity0012_string01(), "ENTITY0012_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0012.getEntity0012_string02(), "ENTITY0012_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0012.getEntity0012_string03(), "ENTITY0012_STRING03_UPDATED");
                        break;

                    case Entity0412:
                        Query selectEntity0412 = jpaResource.getEm().createQuery("SELECT e FROM Entity0412 e WHERE e.entity0412_id = :id_0412");
                        selectEntity0412.setParameter("id_0412", findEntity0012);
                        Entity0412 findEntity0412 = (Entity0412) selectEntity0412.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0412);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0412);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0412));
                        Assert.assertEquals("Assert for the entity id", findEntity0412.getEntity0412_id().getEntity0012_id(), 12L);
                        Assert.assertEquals("Assert for the entity fields", findEntity0412.getEntity0412_string01(), "ENTITY0412_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0412.getEntity0412_string02(), "ENTITY0412_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0412.getEntity0412_string03(), "ENTITY0412_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0412.setEntity0412_string01("ENTITY0412_STRING01_UPDATED");
                        findEntity0412.setEntity0412_string03("ENTITY0412_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0412.setLockMode(LockModeType.NONE);
                        findEntity0412 = null;
                        findEntity0412 = (Entity0412) selectEntity0412.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0412);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0412);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0412));
                        Assert.assertEquals("Assert for the entity id", findEntity0412.getEntity0412_id().getEntity0012_id(), 12L);
                        Assert.assertEquals("Assert for the entity fields", findEntity0412.getEntity0412_string01(), "ENTITY0412_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0412.getEntity0412_string02(), "ENTITY0412_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0412.getEntity0412_string03(), "ENTITY0412_STRING03_UPDATED");
                        break;

                    case Entity0013:
                        Query selectEntity0013 = jpaResource.getEm().createQuery("SELECT e FROM Entity0013 e WHERE e.entity0013_id = :id_0013");
                        selectEntity0013.setParameter("id_0013", 13L);
                        findEntity0013 = (Entity0013) selectEntity0013.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0013);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0013);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0013));
                        Assert.assertEquals("Assert for the entity id", (long) findEntity0013.getEntity0013_id(), 13L);
                        Assert.assertEquals("Assert for the entity fields", findEntity0013.getEntity0013_string01(), "ENTITY0013_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0013.getEntity0013_string02(), "ENTITY0013_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0013.getEntity0013_string03(), "ENTITY0013_STRING03");
                        //
                        // Update, commit, verify
                        //
                        Query updateEntity0013 = jpaResource.getEm()
                                        .createQuery("UPDATE Entity0013 e SET e.entity0013_string01 = :string01_0013, e.entity0013_string03 = :string03_0013 WHERE e.entity0013_id = :id_0013");
                        updateEntity0013.setParameter("id_0013", 13L);
                        updateEntity0013.setParameter("string01_0013", "ENTITY0013_STRING01_UPDATED");
                        updateEntity0013.setParameter("string03_0013", "ENTITY0013_STRING03_UPDATED");
                        if (isOpenJPA)
                            updateEntity0013.setLockMode(lockModeType);
                        updateEntity0013.executeUpdate();
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        /* d643829 */ jpaResource.getEm().refresh(findEntity0013);
                        selectEntity0013.setLockMode(LockModeType.NONE);
                        findEntity0013 = null;
                        findEntity0013 = (Entity0013) selectEntity0013.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0013);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0013);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0013));
                        Assert.assertEquals("Assert for the entity id", (long) findEntity0013.getEntity0013_id(), 13L);
                        Assert.assertEquals("Assert for the entity fields", findEntity0013.getEntity0013_string01(), "ENTITY0013_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0013.getEntity0013_string02(), "ENTITY0013_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0013.getEntity0013_string03(), "ENTITY0013_STRING03_UPDATED");
                        break;

                    case Entity0413:
                        Query selectEntity0413 = jpaResource.getEm().createQuery("SELECT e FROM Entity0413 e WHERE e.entity0413_id = :id_0413");
                        selectEntity0413.setParameter("id_0413", findEntity0013);
                        Entity0413 findEntity0413 = (Entity0413) selectEntity0413.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0413);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0413);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0413));
                        Assert.assertEquals("Assert for the entity id", (long) findEntity0413.getEntity0413_id().getEntity0013_id(), 13L);
                        Assert.assertEquals("Assert for the entity fields", findEntity0413.getEntity0413_string01(), "ENTITY0413_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0413.getEntity0413_string02(), "ENTITY0413_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0413.getEntity0413_string03(), "ENTITY0413_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0413.setEntity0413_string01("ENTITY0413_STRING01_UPDATED");
                        findEntity0413.setEntity0413_string03("ENTITY0413_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0413.setLockMode(LockModeType.NONE);
                        findEntity0413 = null;
                        findEntity0413 = (Entity0413) selectEntity0413.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0413);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0413);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0413));
                        Assert.assertEquals("Assert for the entity id", (long) findEntity0413.getEntity0413_id().getEntity0013_id(), 13L);
                        Assert.assertEquals("Assert for the entity fields", findEntity0413.getEntity0413_string01(), "ENTITY0413_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0413.getEntity0413_string02(), "ENTITY0413_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0413.getEntity0413_string03(), "ENTITY0413_STRING03_UPDATED");
                        break;

                    case Entity0014:
                        Query selectEntity0014 = jpaResource.getEm().createQuery("SELECT e FROM Entity0014 e WHERE e.entity0014_id = :id_0014");
                        selectEntity0014.setParameter("id_0014", (short) 14);
                        findEntity0014 = (Entity0014) selectEntity0014.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0014);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0014);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0014));
                        Assert.assertEquals("Assert for the entity id", findEntity0014.getEntity0014_id(), (short) 14);
                        Assert.assertEquals("Assert for the entity fields", findEntity0014.getEntity0014_string01(), "ENTITY0014_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0014.getEntity0014_string02(), "ENTITY0014_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0014.getEntity0014_string03(), "ENTITY0014_STRING03");
                        //
                        // Update, commit, verify
                        //
                        Query updateEntity0014 = jpaResource.getEm()
                                        .createQuery("UPDATE Entity0014 e SET e.entity0014_string01 = :string01_0014, e.entity0014_string03 = :string03_0014 WHERE e.entity0014_id = :id_0014");
                        updateEntity0014.setParameter("id_0014", (short) 14);
                        updateEntity0014.setParameter("string01_0014", "ENTITY0014_STRING01_UPDATED");
                        updateEntity0014.setParameter("string03_0014", "ENTITY0014_STRING03_UPDATED");
                        if (isOpenJPA)
                            updateEntity0014.setLockMode(lockModeType);
                        updateEntity0014.executeUpdate();
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        /* d643829 */ jpaResource.getEm().refresh(findEntity0014);
                        selectEntity0014.setLockMode(LockModeType.NONE);
                        findEntity0014 = null;
                        findEntity0014 = (Entity0014) selectEntity0014.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0014);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0014);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0014));
                        Assert.assertEquals("Assert for the entity id", findEntity0014.getEntity0014_id(), (short) 14);
                        Assert.assertEquals("Assert for the entity fields", findEntity0014.getEntity0014_string01(), "ENTITY0014_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0014.getEntity0014_string02(), "ENTITY0014_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0014.getEntity0014_string03(), "ENTITY0014_STRING03_UPDATED");
                        break;

                    case Entity0414:
                        Query selectEntity0414 = jpaResource.getEm().createQuery("SELECT e FROM Entity0414 e WHERE e.entity0414_id = :id_0414");
                        selectEntity0414.setParameter("id_0414", findEntity0014);
                        Entity0414 findEntity0414 = (Entity0414) selectEntity0414.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0414);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0414);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0414));
                        Assert.assertEquals("Assert for the entity id", findEntity0414.getEntity0414_id().getEntity0014_id(), (short) 14);
                        Assert.assertEquals("Assert for the entity fields", findEntity0414.getEntity0414_string01(), "ENTITY0414_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0414.getEntity0414_string02(), "ENTITY0414_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0414.getEntity0414_string03(), "ENTITY0414_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0414.setEntity0414_string01("ENTITY0414_STRING01_UPDATED");
                        findEntity0414.setEntity0414_string03("ENTITY0414_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0414.setLockMode(LockModeType.NONE);
                        findEntity0414 = null;
                        findEntity0414 = (Entity0414) selectEntity0414.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0414);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0414);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0414));
                        Assert.assertEquals("Assert for the entity id", findEntity0414.getEntity0414_id().getEntity0014_id(), (short) 14);
                        Assert.assertEquals("Assert for the entity fields", findEntity0414.getEntity0414_string01(), "ENTITY0414_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0414.getEntity0414_string02(), "ENTITY0414_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0414.getEntity0414_string03(), "ENTITY0414_STRING03_UPDATED");
                        break;

                    case Entity0015:
                        Query selectEntity0015 = jpaResource.getEm().createQuery("SELECT e FROM Entity0015 e WHERE e.entity0015_id = :id_0015");
                        selectEntity0015.setParameter("id_0015", (short) 15);
                        findEntity0015 = (Entity0015) selectEntity0015.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0015);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0015);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0015));
                        Assert.assertEquals("Assert for the entity id", (short) findEntity0015.getEntity0015_id(), (short) 15);
                        Assert.assertEquals("Assert for the entity fields", findEntity0015.getEntity0015_string01(), "ENTITY0015_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0015.getEntity0015_string02(), "ENTITY0015_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0015.getEntity0015_string03(), "ENTITY0015_STRING03");
                        //
                        // Update, commit, verify
                        //
                        Query updateEntity0015 = jpaResource.getEm()
                                        .createQuery("UPDATE Entity0015 e SET e.entity0015_string01 = :string01_0015, e.entity0015_string03 = :string03_0015 WHERE e.entity0015_id = :id_0015");
                        updateEntity0015.setParameter("id_0015", (short) 15);
                        updateEntity0015.setParameter("string01_0015", "ENTITY0015_STRING01_UPDATED");
                        updateEntity0015.setParameter("string03_0015", "ENTITY0015_STRING03_UPDATED");
                        if (isOpenJPA)
                            updateEntity0015.setLockMode(lockModeType);
                        updateEntity0015.executeUpdate();
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        /* d643829 */ jpaResource.getEm().refresh(findEntity0015);
                        selectEntity0015.setLockMode(LockModeType.NONE);
                        findEntity0015 = null;
                        findEntity0015 = (Entity0015) selectEntity0015.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0015);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0015);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0015));
                        Assert.assertEquals("Assert for the entity id", (short) findEntity0015.getEntity0015_id(), (short) 15);
                        Assert.assertEquals("Assert for the entity fields", findEntity0015.getEntity0015_string01(), "ENTITY0015_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0015.getEntity0015_string02(), "ENTITY0015_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0015.getEntity0015_string03(), "ENTITY0015_STRING03_UPDATED");
                        break;

                    case Entity0415:
                        Query selectEntity0415 = jpaResource.getEm().createQuery("SELECT e FROM Entity0415 e WHERE e.entity0415_id = :id_0415");
                        selectEntity0415.setParameter("id_0415", findEntity0015);
                        Entity0415 findEntity0415 = (Entity0415) selectEntity0415.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0415);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0415);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0415));
                        Assert.assertEquals("Assert for the entity id", (short) findEntity0415.getEntity0415_id().getEntity0015_id(), (short) 15);
                        Assert.assertEquals("Assert for the entity fields", findEntity0415.getEntity0415_string01(), "ENTITY0415_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0415.getEntity0415_string02(), "ENTITY0415_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0415.getEntity0415_string03(), "ENTITY0415_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0415.setEntity0415_string01("ENTITY0415_STRING01_UPDATED");
                        findEntity0415.setEntity0415_string03("ENTITY0415_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0415.setLockMode(LockModeType.NONE);
                        findEntity0415 = null;
                        findEntity0415 = (Entity0415) selectEntity0415.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0415);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0415);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0415));
                        Assert.assertEquals("Assert for the entity id", (short) findEntity0415.getEntity0415_id().getEntity0015_id(), (short) 15);
                        Assert.assertEquals("Assert for the entity fields", findEntity0415.getEntity0415_string01(), "ENTITY0415_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0415.getEntity0415_string02(), "ENTITY0415_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0415.getEntity0415_string03(), "ENTITY0415_STRING03_UPDATED");
                        break;

                    case Entity0016:
                        Query selectEntity0016 = jpaResource.getEm().createQuery("SELECT e FROM Entity0016 e WHERE e.entity0016_id = :id_0016");
                        selectEntity0016.setParameter("id_0016", new BigDecimal("0016.001616"));
                        findEntity0016 = (Entity0016) selectEntity0016.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0016);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0016);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0016));
                        Assert.assertTrue("Assert for the entity id", findEntity0016.getEntity0016_id().compareTo(new BigDecimal("0016.001616")) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0016.getEntity0016_string01(), "ENTITY0016_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0016.getEntity0016_string02(), "ENTITY0016_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0016.getEntity0016_string03(), "ENTITY0016_STRING03");
                        //
                        // Update, commit, verify
                        //
                        Query updateEntity0016 = jpaResource.getEm()
                                        .createQuery("UPDATE Entity0016 e SET e.entity0016_string01 = :string01_0016, e.entity0016_string03 = :string03_0016 WHERE e.entity0016_id = :id_0016");
                        updateEntity0016.setParameter("id_0016", new BigDecimal("0016.001616"));
                        updateEntity0016.setParameter("string01_0016", "ENTITY0016_STRING01_UPDATED");
                        updateEntity0016.setParameter("string03_0016", "ENTITY0016_STRING03_UPDATED");
                        if (isOpenJPA)
                            updateEntity0016.setLockMode(lockModeType);
                        updateEntity0016.executeUpdate();
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        /* d643829 */ jpaResource.getEm().refresh(findEntity0016);
                        selectEntity0016.setLockMode(LockModeType.NONE);
                        findEntity0016 = null;
                        findEntity0016 = (Entity0016) selectEntity0016.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0016);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0016);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0016));
                        Assert.assertTrue("Assert for the entity id", findEntity0016.getEntity0016_id().compareTo(new BigDecimal("0016.001616")) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0016.getEntity0016_string01(), "ENTITY0016_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0016.getEntity0016_string02(), "ENTITY0016_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0016.getEntity0016_string03(), "ENTITY0016_STRING03_UPDATED");
                        break;

                    case Entity0416:
                        Query selectEntity0416 = jpaResource.getEm().createQuery("SELECT e FROM Entity0416 e WHERE e.entity0416_id = :id_0416");
                        selectEntity0416.setParameter("id_0416", findEntity0016);
                        Entity0416 findEntity0416 = (Entity0416) selectEntity0416.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0416);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0416);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0416));
                        Assert.assertTrue("Assert for the entity id", findEntity0416.getEntity0416_id().getEntity0016_id().compareTo(new BigDecimal("0016.001616")) == 0);
                        Assert.assertEquals("Assert for the entity id", findEntity0416.getEntity0416_id().getEntity0016_string01(), "ENTITY0016_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity id", findEntity0416.getEntity0416_id().getEntity0016_string02(), "ENTITY0016_STRING02");
                        Assert.assertEquals("Assert for the entity id", findEntity0416.getEntity0416_id().getEntity0016_string03(), "ENTITY0016_STRING03_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0416.getEntity0416_string01(), "ENTITY0416_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0416.getEntity0416_string02(), "ENTITY0416_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0416.getEntity0416_string03(), "ENTITY0416_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0416.setEntity0416_string01("ENTITY0416_STRING01_UPDATED");
                        findEntity0416.setEntity0416_string03("ENTITY0416_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0416.setLockMode(LockModeType.NONE);
                        findEntity0416 = null;
                        findEntity0416 = (Entity0416) selectEntity0416.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0416);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0416);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0416));
                        Assert.assertTrue("Assert for the entity id", findEntity0416.getEntity0416_id().getEntity0016_id().compareTo(new BigDecimal("0016.001616")) == 0);
                        Assert.assertEquals("Assert for the entity id", findEntity0416.getEntity0416_id().getEntity0016_string01(), "ENTITY0016_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity id", findEntity0416.getEntity0416_id().getEntity0016_string02(), "ENTITY0016_STRING02");
                        Assert.assertEquals("Assert for the entity id", findEntity0416.getEntity0416_id().getEntity0016_string03(), "ENTITY0016_STRING03_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0416.getEntity0416_string01(), "ENTITY0416_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0416.getEntity0416_string02(), "ENTITY0416_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0416.getEntity0416_string03(), "ENTITY0416_STRING03_UPDATED");
                        break;

                    case Entity0017:
                        Query selectEntity0017 = jpaResource.getEm().createQuery("SELECT e FROM Entity0017 e WHERE e.entity0017_id = :id_0017");
                        selectEntity0017.setParameter("id_0017", new BigInteger("00170017"));
                        findEntity0017 = (Entity0017) selectEntity0017.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0017);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0017);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0017));
                        Assert.assertEquals("Assert for the entity id", findEntity0017.getEntity0017_id(), new BigInteger("00170017"));
                        Assert.assertEquals("Assert for the entity fields", findEntity0017.getEntity0017_string01(), "ENTITY0017_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0017.getEntity0017_string02(), "ENTITY0017_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0017.getEntity0017_string03(), "ENTITY0017_STRING03");
                        //
                        // Update, commit, verify
                        //
                        Query updateEntity0017 = jpaResource.getEm()
                                        .createQuery("UPDATE Entity0017 e SET e.entity0017_string01 = :string01_0017, e.entity0017_string03 = :string03_0017 WHERE e.entity0017_id = :id_0017");
                        updateEntity0017.setParameter("id_0017", new BigInteger("00170017"));
                        updateEntity0017.setParameter("string01_0017", "ENTITY0017_STRING01_UPDATED");
                        updateEntity0017.setParameter("string03_0017", "ENTITY0017_STRING03_UPDATED");
                        if (isOpenJPA)
                            updateEntity0017.setLockMode(lockModeType);
                        updateEntity0017.executeUpdate();
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        /* d643829 */ jpaResource.getEm().refresh(findEntity0017);
                        selectEntity0017.setLockMode(LockModeType.NONE);
                        findEntity0017 = null;
                        findEntity0017 = (Entity0017) selectEntity0017.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0017);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0017);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0017));
                        Assert.assertEquals("Assert for the entity id", findEntity0017.getEntity0017_id(), new BigInteger("00170017"));
                        Assert.assertEquals("Assert for the entity fields", findEntity0017.getEntity0017_string01(), "ENTITY0017_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0017.getEntity0017_string02(), "ENTITY0017_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0017.getEntity0017_string03(), "ENTITY0017_STRING03_UPDATED");
                        break;

                    case Entity0417:
                        Query selectEntity0417 = jpaResource.getEm().createQuery("SELECT e FROM Entity0417 e WHERE e.entity0417_id = :id_0417");
                        selectEntity0417.setParameter("id_0417", findEntity0017);
                        Entity0417 findEntity0417 = (Entity0417) selectEntity0417.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0417);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0417);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0417));
                        Assert.assertEquals("Assert for the entity id", findEntity0417.getEntity0417_id().getEntity0017_id(), new BigInteger("00170017"));
                        Assert.assertEquals("Assert for the entity fields", findEntity0417.getEntity0417_string01(), "ENTITY0417_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0417.getEntity0417_string02(), "ENTITY0417_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0417.getEntity0417_string03(), "ENTITY0417_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0417.setEntity0417_string01("ENTITY0417_STRING01_UPDATED");
                        findEntity0417.setEntity0417_string03("ENTITY0417_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0417.setLockMode(LockModeType.NONE);
                        findEntity0417 = null;
                        findEntity0417 = (Entity0417) selectEntity0417.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0417);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0417);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0417));
                        Assert.assertEquals("Assert for the entity id", findEntity0417.getEntity0417_id().getEntity0017_id(), new BigInteger("00170017"));
                        Assert.assertEquals("Assert for the entity fields", findEntity0417.getEntity0417_string01(), "ENTITY0417_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0417.getEntity0417_string02(), "ENTITY0417_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0417.getEntity0417_string03(), "ENTITY0417_STRING03_UPDATED");
                        break;

                    case Entity0018:
                        Query selectEntity0018 = jpaResource.getEm().createQuery("SELECT e FROM Entity0018 e WHERE e.entity0018_id = :id_0018");
                        selectEntity0018.setParameter("id_0018", javaUtilDate1);
                        findEntity0018 = (Entity0018) selectEntity0018.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0018);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0018);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0018));
                        Assert.assertTrue("Assert for the entity id", findEntity0018.getEntity0018_id().compareTo(javaUtilDate1) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0018.getEntity0018_string01(), "ENTITY0018_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0018.getEntity0018_string02(), "ENTITY0018_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0018.getEntity0018_string03(), "ENTITY0018_STRING03");
                        //
                        // Update, commit, verify
                        //
                        Query updateEntity0018 = jpaResource.getEm()
                                        .createQuery("UPDATE Entity0018 e SET e.entity0018_string01 = :string01_0018, e.entity0018_string03 = :string03_0018 WHERE e.entity0018_id = :id_0018");
                        updateEntity0018.setParameter("id_0018", javaUtilDate1);
                        updateEntity0018.setParameter("string01_0018", "ENTITY0018_STRING01_UPDATED");
                        updateEntity0018.setParameter("string03_0018", "ENTITY0018_STRING03_UPDATED");
                        if (isOpenJPA)
                            updateEntity0018.setLockMode(lockModeType);
                        updateEntity0018.executeUpdate();
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        /* d643829 */ jpaResource.getEm().refresh(findEntity0018);
                        selectEntity0018.setLockMode(LockModeType.NONE);
                        findEntity0018 = null;
                        findEntity0018 = (Entity0018) selectEntity0018.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0018);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0018);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0018));
                        Assert.assertTrue("Assert for the entity id", findEntity0018.getEntity0018_id().compareTo(javaUtilDate1) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0018.getEntity0018_string01(), "ENTITY0018_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0018.getEntity0018_string02(), "ENTITY0018_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0018.getEntity0018_string03(), "ENTITY0018_STRING03_UPDATED");
                        break;

                    case Entity0418:
                        Query selectEntity0418 = jpaResource.getEm().createQuery("SELECT e FROM Entity0418 e WHERE e.entity0418_id = :id_0418");
                        selectEntity0418.setParameter("id_0418", findEntity0018);
                        Entity0418 findEntity0418 = (Entity0418) selectEntity0418.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0418);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0418);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0418));
                        Assert.assertTrue("Assert for the entity id", findEntity0418.getEntity0418_id().getEntity0018_id().compareTo(javaUtilDate1) == 0);
                        Assert.assertEquals("Assert for the entity id", findEntity0418.getEntity0418_id().getEntity0018_string01(), "ENTITY0018_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity id", findEntity0418.getEntity0418_id().getEntity0018_string02(), "ENTITY0018_STRING02");
                        Assert.assertEquals("Assert for the entity id", findEntity0418.getEntity0418_id().getEntity0018_string03(), "ENTITY0018_STRING03_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0418.getEntity0418_string01(), "ENTITY0418_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0418.getEntity0418_string02(), "ENTITY0418_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0418.getEntity0418_string03(), "ENTITY0418_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0418.setEntity0418_string01("ENTITY0418_STRING01_UPDATED");
                        findEntity0418.setEntity0418_string03("ENTITY0418_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0418.setLockMode(LockModeType.NONE);
                        findEntity0418 = null;
                        findEntity0418 = (Entity0418) selectEntity0418.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0418);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0418);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0418));
                        Assert.assertTrue("Assert for the entity id", findEntity0418.getEntity0418_id().getEntity0018_id().compareTo(javaUtilDate1) == 0);
                        Assert.assertEquals("Assert for the entity id", findEntity0418.getEntity0418_id().getEntity0018_string01(), "ENTITY0018_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity id", findEntity0418.getEntity0418_id().getEntity0018_string02(), "ENTITY0018_STRING02");
                        Assert.assertEquals("Assert for the entity id", findEntity0418.getEntity0418_id().getEntity0018_string03(), "ENTITY0018_STRING03_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0418.getEntity0418_string01(), "ENTITY0418_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0418.getEntity0418_string02(), "ENTITY0418_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0418.getEntity0418_string03(), "ENTITY0418_STRING03_UPDATED");
                        break;

                    case Entity0019:
                        Query selectEntity0019 = jpaResource.getEm().createQuery("SELECT e FROM Entity0019 e WHERE e.entity0019_id = :id_0019");
                        selectEntity0019.setParameter("id_0019", javaSqlDate1);
                        findEntity0019 = (Entity0019) selectEntity0019.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0019);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0019);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0019));
                        Assert.assertTrue("Assert for the entity id", findEntity0019.getEntity0019_id().compareTo(javaSqlDate1) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0019.getEntity0019_string01(), "ENTITY0019_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0019.getEntity0019_string02(), "ENTITY0019_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0019.getEntity0019_string03(), "ENTITY0019_STRING03");
                        //
                        // Update, commit, verify
                        //
                        Query updateEntity0019 = jpaResource.getEm()
                                        .createQuery("UPDATE Entity0019 e SET e.entity0019_string01 = :string01_0019, e.entity0019_string03 = :string03_0019 WHERE e.entity0019_id = :id_0019");
                        updateEntity0019.setParameter("id_0019", javaSqlDate1);
                        updateEntity0019.setParameter("string01_0019", "ENTITY0019_STRING01_UPDATED");
                        updateEntity0019.setParameter("string03_0019", "ENTITY0019_STRING03_UPDATED");
                        if (isOpenJPA)
                            updateEntity0019.setLockMode(lockModeType);
                        updateEntity0019.executeUpdate();
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        /* d643829 */ jpaResource.getEm().refresh(findEntity0019);
                        selectEntity0019.setLockMode(LockModeType.NONE);
                        findEntity0019 = null;
                        findEntity0019 = (Entity0019) selectEntity0019.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0019);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0019);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0019));
                        Assert.assertTrue("Assert for the entity id", findEntity0019.getEntity0019_id().compareTo(javaSqlDate1) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0019.getEntity0019_string01(), "ENTITY0019_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0019.getEntity0019_string02(), "ENTITY0019_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0019.getEntity0019_string03(), "ENTITY0019_STRING03_UPDATED");
                        break;

                    case Entity0419:
                        Query selectEntity0419 = jpaResource.getEm().createQuery("SELECT e FROM Entity0419 e WHERE e.entity0419_id = :id_0419");
                        selectEntity0419.setParameter("id_0419", findEntity0019);
                        Entity0419 findEntity0419 = (Entity0419) selectEntity0419.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0419);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0419);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0419));
                        Assert.assertTrue("Assert for the entity id", findEntity0419.getEntity0419_id().getEntity0019_id().compareTo(javaSqlDate1) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0419.getEntity0419_string01(), "ENTITY0419_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0419.getEntity0419_string02(), "ENTITY0419_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0419.getEntity0419_string03(), "ENTITY0419_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0419.setEntity0419_string01("ENTITY0419_STRING01_UPDATED");
                        findEntity0419.setEntity0419_string03("ENTITY0419_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0419.setLockMode(LockModeType.NONE);
                        findEntity0419 = null;
                        findEntity0419 = (Entity0419) selectEntity0419.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0419);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0419);

                        System.out.println("Perform child verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0419));
                        Assert.assertTrue("Assert for the entity id", findEntity0419.getEntity0419_id().getEntity0019_id().compareTo(javaSqlDate1) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0419.getEntity0419_string01(), "ENTITY0419_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0419.getEntity0419_string02(), "ENTITY0419_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0419.getEntity0419_string03(), "ENTITY0419_STRING03_UPDATED");
                        break;

                    case Entity0101:
                        Query selectEntity0101 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0101 e WHERE e.entity0101_id1 = :id1_0101 AND e.entity0101_id2 = :id2_0101");
                        selectEntity0101.setParameter("id1_0101", (byte) 101);
                        selectEntity0101.setParameter("id2_0101", (byte) 102);
                        Entity0101 findEntity0101 = (Entity0101) selectEntity0101.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0101);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0101);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0101));
                        Assert.assertEquals("Assert for the entity id1", findEntity0101.getEntity0101_id1(), (byte) 101);
                        Assert.assertEquals("Assert for the entity id2", findEntity0101.getEntity0101_id2(), (byte) 102);
                        Assert.assertEquals("Assert for the entity fields", findEntity0101.getEntity0101_string01(), "ENTITY0101_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0101.getEntity0101_string02(), "ENTITY0101_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0101.getEntity0101_string03(), "ENTITY0101_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0101.setEntity0101_string01("ENTITY0101_STRING01_UPDATED");
                        findEntity0101.setEntity0101_string03("ENTITY0101_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0101.setLockMode(LockModeType.NONE);
                        findEntity0101 = null;
                        findEntity0101 = (Entity0101) selectEntity0101.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0101);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0101);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0101));
                        Assert.assertEquals("Assert for the entity id1", findEntity0101.getEntity0101_id1(), (byte) 101);
                        Assert.assertEquals("Assert for the entity id2", findEntity0101.getEntity0101_id2(), (byte) 102);
                        Assert.assertEquals("Assert for the entity fields", findEntity0101.getEntity0101_string01(), "ENTITY0101_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0101.getEntity0101_string02(), "ENTITY0101_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0101.getEntity0101_string03(), "ENTITY0101_STRING03_UPDATED");
                        break;

                    case Entity0102:
                        Query selectEntity0102 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0102 e WHERE e.entity0102_id1 = :id1_0102 AND e.entity0102_id2 = :id2_0102");
                        selectEntity0102.setParameter("id1_0102", (byte) 102);
                        selectEntity0102.setParameter("id2_0102", (byte) 103);
                        Entity0102 findEntity0102 = (Entity0102) selectEntity0102.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0102);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0102);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0102));
                        Assert.assertEquals("Assert for the entity id1", (byte) findEntity0102.getEntity0102_id1(), (byte) 102);
                        Assert.assertEquals("Assert for the entity id2", (byte) findEntity0102.getEntity0102_id2(), (byte) 103);
                        Assert.assertEquals("Assert for the entity fields", findEntity0102.getEntity0102_string01(), "ENTITY0102_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0102.getEntity0102_string02(), "ENTITY0102_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0102.getEntity0102_string03(), "ENTITY0102_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0102.setEntity0102_string01("ENTITY0102_STRING01_UPDATED");
                        findEntity0102.setEntity0102_string03("ENTITY0102_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0102.setLockMode(LockModeType.NONE);
                        findEntity0102 = null;
                        findEntity0102 = (Entity0102) selectEntity0102.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0102);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0102);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0102));
                        Assert.assertEquals("Assert for the entity id1", (byte) findEntity0102.getEntity0102_id1(), (byte) 102);
                        Assert.assertEquals("Assert for the entity id2", (byte) findEntity0102.getEntity0102_id2(), (byte) 103);
                        Assert.assertEquals("Assert for the entity fields", findEntity0102.getEntity0102_string01(), "ENTITY0102_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0102.getEntity0102_string02(), "ENTITY0102_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0102.getEntity0102_string03(), "ENTITY0102_STRING03_UPDATED");
                        break;

                    case Entity0103:
                        Query selectEntity0103 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0103 e WHERE e.entity0103_id1 = :id1_0103 AND e.entity0103_id2 = :id2_0103");
                        selectEntity0103.setParameter("id1_0103", '3');
                        selectEntity0103.setParameter("id2_0103", '4');
                        Entity0103 findEntity0103 = (Entity0103) selectEntity0103.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0103);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0103);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0103));
                        Assert.assertEquals("Assert for the entity id1", findEntity0103.getEntity0103_id1(), '3');
                        Assert.assertEquals("Assert for the entity id2", findEntity0103.getEntity0103_id2(), '4');
                        Assert.assertEquals("Assert for the entity fields", findEntity0103.getEntity0103_string01(), "ENTITY0103_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0103.getEntity0103_string02(), "ENTITY0103_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0103.getEntity0103_string03(), "ENTITY0103_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0103.setEntity0103_string01("ENTITY0103_STRING01_UPDATED");
                        findEntity0103.setEntity0103_string03("ENTITY0103_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0103.setLockMode(LockModeType.NONE);
                        findEntity0103 = null;
                        findEntity0103 = (Entity0103) selectEntity0103.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0103);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0103);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0103));
                        Assert.assertEquals("Assert for the entity id1", findEntity0103.getEntity0103_id1(), '3');
                        Assert.assertEquals("Assert for the entity id2", findEntity0103.getEntity0103_id2(), '4');
                        Assert.assertEquals("Assert for the entity fields", findEntity0103.getEntity0103_string01(), "ENTITY0103_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0103.getEntity0103_string02(), "ENTITY0103_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0103.getEntity0103_string03(), "ENTITY0103_STRING03_UPDATED");
                        break;

                    case Entity0104:
                        Query selectEntity0104 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0104 e WHERE e.entity0104_id1 = :id1_0104 AND e.entity0104_id2 = :id2_0104");
                        selectEntity0104.setParameter("id1_0104", '4');
                        selectEntity0104.setParameter("id2_0104", '5');
                        Entity0104 findEntity0104 = (Entity0104) selectEntity0104.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0104);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0104);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0104));
                        Assert.assertEquals("Assert for the entity id1", findEntity0104.getEntity0104_id1(), new Character('4'));
                        Assert.assertEquals("Assert for the entity id2", findEntity0104.getEntity0104_id2(), new Character('5'));
                        Assert.assertEquals("Assert for the entity fields", findEntity0104.getEntity0104_string01(), "ENTITY0104_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0104.getEntity0104_string02(), "ENTITY0104_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0104.getEntity0104_string03(), "ENTITY0104_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0104.setEntity0104_string01("ENTITY0104_STRING01_UPDATED");
                        findEntity0104.setEntity0104_string03("ENTITY0104_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0104.setLockMode(LockModeType.NONE);
                        findEntity0104 = null;
                        findEntity0104 = (Entity0104) selectEntity0104.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0104);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0104);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0104));
                        Assert.assertEquals("Assert for the entity id1", findEntity0104.getEntity0104_id1(), new Character('4'));
                        Assert.assertEquals("Assert for the entity id2", findEntity0104.getEntity0104_id2(), new Character('5'));
                        Assert.assertEquals("Assert for the entity fields", findEntity0104.getEntity0104_string01(), "ENTITY0104_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0104.getEntity0104_string02(), "ENTITY0104_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0104.getEntity0104_string03(), "ENTITY0104_STRING03_UPDATED");
                        break;

                    case Entity0105:
                        Query selectEntity0105 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0105 e WHERE e.entity0105_id1 = :id1_0105 AND e.entity0105_id2 = :id2_0105");
                        selectEntity0105.setParameter("id1_0105", "ENTITY0105_ID1");
                        selectEntity0105.setParameter("id2_0105", "ENTITY0105_ID2");
                        Entity0105 findEntity0105 = (Entity0105) selectEntity0105.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0105);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0105);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0105));
                        Assert.assertEquals("Assert for the entity id1", findEntity0105.getEntity0105_id1(), "ENTITY0105_ID1");
                        Assert.assertEquals("Assert for the entity id2", findEntity0105.getEntity0105_id2(), "ENTITY0105_ID2");
                        Assert.assertEquals("Assert for the entity fields", findEntity0105.getEntity0105_string01(), "ENTITY0105_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0105.getEntity0105_string02(), "ENTITY0105_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0105.getEntity0105_string03(), "ENTITY0105_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0105.setEntity0105_string01("ENTITY0105_STRING01_UPDATED");
                        findEntity0105.setEntity0105_string03("ENTITY0105_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0105.setLockMode(LockModeType.NONE);
                        findEntity0105 = null;
                        findEntity0105 = (Entity0105) selectEntity0105.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0105);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0105);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0105));
                        Assert.assertEquals("Assert for the entity id1", findEntity0105.getEntity0105_id1(), "ENTITY0105_ID1");
                        Assert.assertEquals("Assert for the entity id2", findEntity0105.getEntity0105_id2(), "ENTITY0105_ID2");
                        Assert.assertEquals("Assert for the entity fields", findEntity0105.getEntity0105_string01(), "ENTITY0105_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0105.getEntity0105_string02(), "ENTITY0105_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0105.getEntity0105_string03(), "ENTITY0105_STRING03_UPDATED");
                        break;

                    case Entity0106:
                        Query selectEntity0106 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0106 e WHERE e.entity0106_id1 = :id1_0106 AND e.entity0106_id2 = :id2_0106");
                        selectEntity0106.setParameter("id1_0106", 0106.0106D);
                        selectEntity0106.setParameter("id2_0106", 0107.0107D);
                        Entity0106 findEntity0106 = (Entity0106) selectEntity0106.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0106);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0106);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0106));
                        Assert.assertEquals("Assert for the entity id1", findEntity0106.getEntity0106_id1(), 0106.0106D, 0.1);
                        Assert.assertEquals("Assert for the entity id2", findEntity0106.getEntity0106_id2(), 0107.0107D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0106.getEntity0106_string01(), "ENTITY0106_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0106.getEntity0106_string02(), "ENTITY0106_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0106.getEntity0106_string03(), "ENTITY0106_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0106.setEntity0106_string01("ENTITY0106_STRING01_UPDATED");
                        findEntity0106.setEntity0106_string03("ENTITY0106_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0106.setLockMode(LockModeType.NONE);
                        findEntity0106 = null;
                        findEntity0106 = (Entity0106) selectEntity0106.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0106);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0106);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0106));
                        Assert.assertEquals("Assert for the entity id1", findEntity0106.getEntity0106_id1(), 0106.0106D, 0.1);
                        Assert.assertEquals("Assert for the entity id2", findEntity0106.getEntity0106_id2(), 0107.0107D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0106.getEntity0106_string01(), "ENTITY0106_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0106.getEntity0106_string02(), "ENTITY0106_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0106.getEntity0106_string03(), "ENTITY0106_STRING03_UPDATED");
                        break;

                    case Entity0107:
                        Query selectEntity0107 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0107 e WHERE e.entity0107_id1 = :id1_0107 AND e.entity0107_id2 = :id2_0107");
                        selectEntity0107.setParameter("id1_0107", 0107.0107D);
                        selectEntity0107.setParameter("id2_0107", 0108.0108D);
                        Entity0107 findEntity0107 = (Entity0107) selectEntity0107.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0107);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0107);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0107));
                        Assert.assertEquals("Assert for the entity id1", findEntity0107.getEntity0107_id1(), 0107.0107D, 0.1);
                        Assert.assertEquals("Assert for the entity id2", findEntity0107.getEntity0107_id2(), 0108.0108D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0107.getEntity0107_string01(), "ENTITY0107_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0107.getEntity0107_string02(), "ENTITY0107_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0107.getEntity0107_string03(), "ENTITY0107_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0107.setEntity0107_string01("ENTITY0107_STRING01_UPDATED");
                        findEntity0107.setEntity0107_string03("ENTITY0107_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0107.setLockMode(LockModeType.NONE);
                        findEntity0107 = null;
                        findEntity0107 = (Entity0107) selectEntity0107.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0107);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0107);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0107));
                        Assert.assertEquals("Assert for the entity id1", findEntity0107.getEntity0107_id1(), 0107.0107D, 0.1);
                        Assert.assertEquals("Assert for the entity id2", findEntity0107.getEntity0107_id2(), 0108.0108D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0107.getEntity0107_string01(), "ENTITY0107_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0107.getEntity0107_string02(), "ENTITY0107_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0107.getEntity0107_string03(), "ENTITY0107_STRING03_UPDATED");
                        break;

                    case Entity0108:
                        Query selectEntity0108 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0108 e WHERE e.entity0108_id1 = :id1_0108 AND e.entity0108_id2 = :id2_0108");
                        selectEntity0108.setParameter("id1_0108", 0108.0108F);
                        selectEntity0108.setParameter("id2_0108", 0109.0109F);
                        Entity0108 findEntity0108 = (Entity0108) selectEntity0108.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0108);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0108);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0108));
                        Assert.assertEquals("Assert for the entity id1", findEntity0108.getEntity0108_id1(), 0108.0108F, 0.1);
                        Assert.assertEquals("Assert for the entity id2", findEntity0108.getEntity0108_id2(), 0109.0109F, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0108.getEntity0108_string01(), "ENTITY0108_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0108.getEntity0108_string02(), "ENTITY0108_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0108.getEntity0108_string03(), "ENTITY0108_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0108.setEntity0108_string01("ENTITY0108_STRING01_UPDATED");
                        findEntity0108.setEntity0108_string03("ENTITY0108_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0108.setLockMode(LockModeType.NONE);
                        //d642065                  findEntity0108 = null;
//                          findEntity0108 = (Entity0108) selectEntity0108.getSingleResult();
//                          System.out.println("Object returned by find: " + findEntity0108);
//                          Assert.assertNotNull("Assert that the find operation did not return null", findEntity0108);
//
//                          System.out.println     ( "Perform parent verifications...");
//                          Assert.assertTrue  ( "Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0108));
//                          Assert.assertEquals    ( "Assert for the entity id1",    findEntity0108.getEntity0108_id1(), 0108.0108F);
//                          Assert.assertEquals    ( "Assert for the entity id2",    findEntity0108.getEntity0108_id2(), 0109.0109F);
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0108.getEntity0108_string01(), "ENTITY0108_STRING01_UPDATED");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0108.getEntity0108_string02(), "ENTITY0108_STRING02");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0108.getEntity0108_string03(), "ENTITY0108_STRING03_UPDATED");
                        break;

                    case Entity0109:
                        Query selectEntity0109 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0109 e WHERE e.entity0109_id1 = :id1_0109 AND e.entity0109_id2 = :id2_0109");
                        selectEntity0109.setParameter("id1_0109", 0109.0109F);
                        selectEntity0109.setParameter("id2_0109", 0110.0110F);
                        Entity0109 findEntity0109 = (Entity0109) selectEntity0109.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0109);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0109);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0109));
                        Assert.assertEquals("Assert for the entity id", findEntity0109.getEntity0109_id1(), 0109.0109F, 0.1);
                        Assert.assertEquals("Assert for the entity id", findEntity0109.getEntity0109_id2(), 0110.0110F, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0109.getEntity0109_string01(), "ENTITY0109_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0109.getEntity0109_string02(), "ENTITY0109_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0109.getEntity0109_string03(), "ENTITY0109_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0109.setEntity0109_string01("ENTITY0109_STRING01_UPDATED");
                        findEntity0109.setEntity0109_string03("ENTITY0109_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0109.setLockMode(LockModeType.NONE);
                        //d642065                  findEntity0109 = null;
//                          findEntity0109 = (Entity0109) selectEntity0109.getSingleResult();
//                          System.out.println("Object returned by find: " + findEntity0109);
//                          Assert.assertNotNull("Assert that the find operation did not return null", findEntity0109);
//
//                          System.out.println     ( "Perform parent verifications...");
//                          Assert.assertTrue  ( "Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0109));
//                          Assert.assertEquals    ( "Assert for the entity id",     findEntity0109.getEntity0109_id1(), 0109.0109F);
//                          Assert.assertEquals    ( "Assert for the entity id",     findEntity0109.getEntity0109_id2(), 0110.0110F);
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0109.getEntity0109_string01(), "ENTITY0109_STRING01_UPDATED");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0109.getEntity0109_string02(), "ENTITY0109_STRING02");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0109.getEntity0109_string03(), "ENTITY0109_STRING03_UPDATED");
                        break;

                    case Entity0110:
                        Query selectEntity0110 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0110 e WHERE e.entity0110_id1 = :id1_0110 AND e.entity0110_id2 = :id2_0110");
                        selectEntity0110.setParameter("id1_0110", 110);
                        selectEntity0110.setParameter("id2_0110", 111);
                        Entity0110 findEntity0110 = (Entity0110) selectEntity0110.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0110);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0110);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0110));
                        Assert.assertEquals("Assert for the entity id1", findEntity0110.getEntity0110_id1(), 110);
                        Assert.assertEquals("Assert for the entity id2", findEntity0110.getEntity0110_id2(), 111);
                        Assert.assertEquals("Assert for the entity fields", findEntity0110.getEntity0110_string01(), "ENTITY0110_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0110.getEntity0110_string02(), "ENTITY0110_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0110.getEntity0110_string03(), "ENTITY0110_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0110.setEntity0110_string01("ENTITY0110_STRING01_UPDATED");
                        findEntity0110.setEntity0110_string03("ENTITY0110_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0110.setLockMode(LockModeType.NONE);
                        findEntity0110 = null;
                        findEntity0110 = (Entity0110) selectEntity0110.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0110);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0110);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0110));
                        Assert.assertEquals("Assert for the entity id1", findEntity0110.getEntity0110_id1(), 110);
                        Assert.assertEquals("Assert for the entity id2", findEntity0110.getEntity0110_id2(), 111);
                        Assert.assertEquals("Assert for the entity fields", findEntity0110.getEntity0110_string01(), "ENTITY0110_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0110.getEntity0110_string02(), "ENTITY0110_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0110.getEntity0110_string03(), "ENTITY0110_STRING03_UPDATED");
                        break;

                    case Entity0111:
                        Query selectEntity0111 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0111 e WHERE e.entity0111_id1 = :id1_0111 AND e.entity0111_id2 = :id2_0111");
                        selectEntity0111.setParameter("id1_0111", 111);
                        selectEntity0111.setParameter("id2_0111", 112);
                        Entity0111 findEntity0111 = (Entity0111) selectEntity0111.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0111);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0111);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0111));
                        Assert.assertEquals("Assert for the entity id1", (int) findEntity0111.getEntity0111_id1(), 111);
                        Assert.assertEquals("Assert for the entity id2", (int) findEntity0111.getEntity0111_id2(), 112);
                        Assert.assertEquals("Assert for the entity fields", findEntity0111.getEntity0111_string01(), "ENTITY0111_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0111.getEntity0111_string02(), "ENTITY0111_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0111.getEntity0111_string03(), "ENTITY0111_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0111.setEntity0111_string01("ENTITY0111_STRING01_UPDATED");
                        findEntity0111.setEntity0111_string03("ENTITY0111_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0111.setLockMode(LockModeType.NONE);
                        findEntity0111 = null;
                        findEntity0111 = (Entity0111) selectEntity0111.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0111);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0111);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0111));
                        Assert.assertEquals("Assert for the entity id1", (int) findEntity0111.getEntity0111_id1(), 111);
                        Assert.assertEquals("Assert for the entity id2", (int) findEntity0111.getEntity0111_id2(), 112);
                        Assert.assertEquals("Assert for the entity fields", findEntity0111.getEntity0111_string01(), "ENTITY0111_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0111.getEntity0111_string02(), "ENTITY0111_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0111.getEntity0111_string03(), "ENTITY0111_STRING03_UPDATED");
                        break;

                    case Entity0112:
                        Query selectEntity0112 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0112 e WHERE e.entity0112_id1 = :id1_0112 AND e.entity0112_id2 = :id2_0112");
                        selectEntity0112.setParameter("id1_0112", 112L);
                        selectEntity0112.setParameter("id2_0112", 113L);
                        Entity0112 findEntity0112 = (Entity0112) selectEntity0112.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0112);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0112);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0112));
                        Assert.assertEquals("Assert for the entity id1", findEntity0112.getEntity0112_id1(), 112L);
                        Assert.assertEquals("Assert for the entity id2", findEntity0112.getEntity0112_id2(), 113L);
                        Assert.assertEquals("Assert for the entity fields", findEntity0112.getEntity0112_string01(), "ENTITY0112_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0112.getEntity0112_string02(), "ENTITY0112_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0112.getEntity0112_string03(), "ENTITY0112_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0112.setEntity0112_string01("ENTITY0112_STRING01_UPDATED");
                        findEntity0112.setEntity0112_string03("ENTITY0112_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0112.setLockMode(LockModeType.NONE);
                        findEntity0112 = null;
                        findEntity0112 = (Entity0112) selectEntity0112.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0112);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0112);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0112));
                        Assert.assertEquals("Assert for the entity id1", findEntity0112.getEntity0112_id1(), 112L);
                        Assert.assertEquals("Assert for the entity id2", findEntity0112.getEntity0112_id2(), 113L);
                        Assert.assertEquals("Assert for the entity fields", findEntity0112.getEntity0112_string01(), "ENTITY0112_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0112.getEntity0112_string02(), "ENTITY0112_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0112.getEntity0112_string03(), "ENTITY0112_STRING03_UPDATED");
                        break;

                    case Entity0113:
                        Query selectEntity0113 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0113 e WHERE e.entity0113_id1 = :id1_0113 AND e.entity0113_id2 = :id2_0113");
                        selectEntity0113.setParameter("id1_0113", 113L);
                        selectEntity0113.setParameter("id2_0113", 114L);
                        Entity0113 findEntity0113 = (Entity0113) selectEntity0113.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0113);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0113);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0113));
                        Assert.assertEquals("Assert for the entity id1", (long) findEntity0113.getEntity0113_id1(), 113L);
                        Assert.assertEquals("Assert for the entity id2", (long) findEntity0113.getEntity0113_id2(), 114L);
                        Assert.assertEquals("Assert for the entity fields", findEntity0113.getEntity0113_string01(), "ENTITY0113_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0113.getEntity0113_string02(), "ENTITY0113_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0113.getEntity0113_string03(), "ENTITY0113_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0113.setEntity0113_string01("ENTITY0113_STRING01_UPDATED");
                        findEntity0113.setEntity0113_string03("ENTITY0113_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0113.setLockMode(LockModeType.NONE);
                        findEntity0113 = null;
                        findEntity0113 = (Entity0113) selectEntity0113.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0113);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0113);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0113));
                        Assert.assertEquals("Assert for the entity id1", (long) findEntity0113.getEntity0113_id1(), 113L);
                        Assert.assertEquals("Assert for the entity id2", (long) findEntity0113.getEntity0113_id2(), 114L);
                        Assert.assertEquals("Assert for the entity fields", findEntity0113.getEntity0113_string01(), "ENTITY0113_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0113.getEntity0113_string02(), "ENTITY0113_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0113.getEntity0113_string03(), "ENTITY0113_STRING03_UPDATED");
                        break;

                    case Entity0114:
                        Query selectEntity0114 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0114 e WHERE e.entity0114_id1 = :id1_0114 AND e.entity0114_id2 = :id2_0114");
                        selectEntity0114.setParameter("id1_0114", (short) 114);
                        selectEntity0114.setParameter("id2_0114", (short) 115);
                        Entity0114 findEntity0114 = (Entity0114) selectEntity0114.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0114);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0114);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0114));
                        Assert.assertEquals("Assert for the entity id1", findEntity0114.getEntity0114_id1(), (short) 114);
                        Assert.assertEquals("Assert for the entity id2", findEntity0114.getEntity0114_id2(), (short) 115);
                        Assert.assertEquals("Assert for the entity fields", findEntity0114.getEntity0114_string01(), "ENTITY0114_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0114.getEntity0114_string02(), "ENTITY0114_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0114.getEntity0114_string03(), "ENTITY0114_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0114.setEntity0114_string01("ENTITY0114_STRING01_UPDATED");
                        findEntity0114.setEntity0114_string03("ENTITY0114_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0114.setLockMode(LockModeType.NONE);
                        findEntity0114 = null;
                        findEntity0114 = (Entity0114) selectEntity0114.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0114);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0114);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0114));
                        Assert.assertEquals("Assert for the entity id1", findEntity0114.getEntity0114_id1(), (short) 114);
                        Assert.assertEquals("Assert for the entity id2", findEntity0114.getEntity0114_id2(), (short) 115);
                        Assert.assertEquals("Assert for the entity fields", findEntity0114.getEntity0114_string01(), "ENTITY0114_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0114.getEntity0114_string02(), "ENTITY0114_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0114.getEntity0114_string03(), "ENTITY0114_STRING03_UPDATED");
                        break;

                    case Entity0115:
                        Query selectEntity0115 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0115 e WHERE e.entity0115_id1 = :id1_0115 AND e.entity0115_id2 = :id2_0115");
                        selectEntity0115.setParameter("id1_0115", (short) 115);
                        selectEntity0115.setParameter("id2_0115", (short) 116);
                        Entity0115 findEntity0115 = (Entity0115) selectEntity0115.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0115);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0115);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0115));
                        Assert.assertEquals("Assert for the entity id1", (short) findEntity0115.getEntity0115_id1(), (short) 115);
                        Assert.assertEquals("Assert for the entity id2", (short) findEntity0115.getEntity0115_id2(), (short) 116);
                        Assert.assertEquals("Assert for the entity fields", findEntity0115.getEntity0115_string01(), "ENTITY0115_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0115.getEntity0115_string02(), "ENTITY0115_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0115.getEntity0115_string03(), "ENTITY0115_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0115.setEntity0115_string01("ENTITY0115_STRING01_UPDATED");
                        findEntity0115.setEntity0115_string03("ENTITY0115_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0115.setLockMode(LockModeType.NONE);
                        findEntity0115 = null;
                        findEntity0115 = (Entity0115) selectEntity0115.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0115);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0115);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0115));
                        Assert.assertEquals("Assert for the entity id1", (short) findEntity0115.getEntity0115_id1(), (short) 115);
                        Assert.assertEquals("Assert for the entity id2", (short) findEntity0115.getEntity0115_id2(), (short) 116);
                        Assert.assertEquals("Assert for the entity fields", findEntity0115.getEntity0115_string01(), "ENTITY0115_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0115.getEntity0115_string02(), "ENTITY0115_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0115.getEntity0115_string03(), "ENTITY0115_STRING03_UPDATED");
                        break;

                    case Entity0116:
                        Query selectEntity0116 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0116 e WHERE e.entity0116_id1 = :id1_0116 AND e.entity0116_id2 = :id2_0116");
                        selectEntity0116.setParameter("id1_0116", new BigDecimal("0116.011616"));
                        selectEntity0116.setParameter("id2_0116", new BigDecimal("0117.011717"));
                        Entity0116 findEntity0116 = (Entity0116) selectEntity0116.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0116);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0116);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0116));
                        Assert.assertTrue("Assert for the entity id1", findEntity0116.getEntity0116_id1().compareTo(new BigDecimal("0116.011616")) == 0);
                        Assert.assertTrue("Assert for the entity id2", findEntity0116.getEntity0116_id2().compareTo(new BigDecimal("0117.011717")) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0116.getEntity0116_string01(), "ENTITY0116_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0116.getEntity0116_string02(), "ENTITY0116_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0116.getEntity0116_string03(), "ENTITY0116_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0116.setEntity0116_string01("ENTITY0116_STRING01_UPDATED");
                        findEntity0116.setEntity0116_string03("ENTITY0116_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0116.setLockMode(LockModeType.NONE);
                        findEntity0116 = null;
                        findEntity0116 = (Entity0116) selectEntity0116.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0116);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0116);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0116));
                        Assert.assertTrue("Assert for the entity id1", findEntity0116.getEntity0116_id1().compareTo(new BigDecimal("0116.011616")) == 0);
                        Assert.assertTrue("Assert for the entity id2", findEntity0116.getEntity0116_id2().compareTo(new BigDecimal("0117.011717")) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0116.getEntity0116_string01(), "ENTITY0116_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0116.getEntity0116_string02(), "ENTITY0116_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0116.getEntity0116_string03(), "ENTITY0116_STRING03_UPDATED");
                        break;

                    case Entity0117:
                        Query selectEntity0117 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0117 e WHERE e.entity0117_id1 = :id1_0117 AND e.entity0117_id2 = :id2_0117");
                        selectEntity0117.setParameter("id1_0117", new BigInteger("01170117"));
                        selectEntity0117.setParameter("id2_0117", new BigInteger("01180118"));
                        Entity0117 findEntity0117 = (Entity0117) selectEntity0117.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0117);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0117);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0117));
                        Assert.assertEquals("Assert for the entity id1", findEntity0117.getEntity0117_id1(), new BigInteger("01170117"));
                        Assert.assertEquals("Assert for the entity id2", findEntity0117.getEntity0117_id2(), new BigInteger("01180118"));
                        Assert.assertEquals("Assert for the entity fields", findEntity0117.getEntity0117_string01(), "ENTITY0117_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0117.getEntity0117_string02(), "ENTITY0117_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0117.getEntity0117_string03(), "ENTITY0117_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0117.setEntity0117_string01("ENTITY0117_STRING01_UPDATED");
                        findEntity0117.setEntity0117_string03("ENTITY0117_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0117.setLockMode(LockModeType.NONE);
                        findEntity0117 = null;
                        findEntity0117 = (Entity0117) selectEntity0117.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0117);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0117);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0117));
                        Assert.assertEquals("Assert for the entity id1", findEntity0117.getEntity0117_id1(), new BigInteger("01170117"));
                        Assert.assertEquals("Assert for the entity id2", findEntity0117.getEntity0117_id2(), new BigInteger("01180118"));
                        Assert.assertEquals("Assert for the entity fields", findEntity0117.getEntity0117_string01(), "ENTITY0117_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0117.getEntity0117_string02(), "ENTITY0117_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0117.getEntity0117_string03(), "ENTITY0117_STRING03_UPDATED");
                        break;

                    case Entity0118:
                        Query selectEntity0118 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0118 e WHERE e.entity0118_id1 = :id1_0118 AND e.entity0118_id2 = :id2_0118");
                        selectEntity0118.setParameter("id1_0118", javaUtilDate1);
                        selectEntity0118.setParameter("id2_0118", javaUtilDate2);
                        Entity0118 findEntity0118 = (Entity0118) selectEntity0118.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0118);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0118);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0118));
                        Assert.assertTrue("Assert for the entity id1", findEntity0118.getEntity0118_id1().compareTo(javaUtilDate1) == 0);
                        Assert.assertTrue("Assert for the entity id2", findEntity0118.getEntity0118_id2().compareTo(javaUtilDate2) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0118.getEntity0118_string01(), "ENTITY0118_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0118.getEntity0118_string02(), "ENTITY0118_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0118.getEntity0118_string03(), "ENTITY0118_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0118.setEntity0118_string01("ENTITY0118_STRING01_UPDATED");
                        findEntity0118.setEntity0118_string03("ENTITY0118_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0118.setLockMode(LockModeType.NONE);
                        findEntity0118 = null;
                        findEntity0118 = (Entity0118) selectEntity0118.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0118);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0118);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0118));
                        Assert.assertTrue("Assert for the entity id1", findEntity0118.getEntity0118_id1().compareTo(javaUtilDate1) == 0);
                        Assert.assertTrue("Assert for the entity id2", findEntity0118.getEntity0118_id2().compareTo(javaUtilDate2) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0118.getEntity0118_string01(), "ENTITY0118_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0118.getEntity0118_string02(), "ENTITY0118_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0118.getEntity0118_string03(), "ENTITY0118_STRING03_UPDATED");
                        break;

                    case Entity0119:
                        Query selectEntity0119 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0119 e WHERE e.entity0119_id1 = :id1_0119 AND e.entity0119_id2 = :id2_0119");
                        selectEntity0119.setParameter("id1_0119", javaSqlDate1);
                        selectEntity0119.setParameter("id2_0119", javaSqlDate2);
                        Entity0119 findEntity0119 = (Entity0119) selectEntity0119.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0119);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0119);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0119));
                        Assert.assertTrue("Assert for the entity id1", findEntity0119.getEntity0119_id1().compareTo(javaSqlDate1) == 0);
                        Assert.assertTrue("Assert for the entity id2", findEntity0119.getEntity0119_id2().compareTo(javaSqlDate2) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0119.getEntity0119_string01(), "ENTITY0119_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0119.getEntity0119_string02(), "ENTITY0119_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0119.getEntity0119_string03(), "ENTITY0119_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0119.setEntity0119_string01("ENTITY0119_STRING01_UPDATED");
                        findEntity0119.setEntity0119_string03("ENTITY0119_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0119.setLockMode(LockModeType.NONE);
                        findEntity0119 = null;
                        findEntity0119 = (Entity0119) selectEntity0119.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0119);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0119);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0119));
                        Assert.assertTrue("Assert for the entity id1", findEntity0119.getEntity0119_id1().compareTo(javaSqlDate1) == 0);
                        Assert.assertTrue("Assert for the entity id2", findEntity0119.getEntity0119_id2().compareTo(javaSqlDate2) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0119.getEntity0119_string01(), "ENTITY0119_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0119.getEntity0119_string02(), "ENTITY0119_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0119.getEntity0119_string03(), "ENTITY0119_STRING03_UPDATED");
                        break;

                    case Entity0201:
                        Query selectEntity0201 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0201 e WHERE e.entity0201_id1 = :id1_0201 AND e.entity0201_id2 = :id2_0201 AND e.entity0201_id3 = :id3_0201");
                        selectEntity0201.setParameter("id1_0201", (byte) (201 - ByteOffset));
                        selectEntity0201.setParameter("id2_0201", (byte) (202 - ByteOffset));
                        selectEntity0201.setParameter("id3_0201", (byte) (203 - ByteOffset));
                        Entity0201 findEntity0201 = (Entity0201) selectEntity0201.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0201);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0201);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0201));
                        Assert.assertEquals("Assert for the entity id1", findEntity0201.getEntity0201_id1(), (byte) (201 - ByteOffset));
                        Assert.assertEquals("Assert for the entity id2", findEntity0201.getEntity0201_id2(), (byte) (202 - ByteOffset));
                        Assert.assertEquals("Assert for the entity id3", findEntity0201.getEntity0201_id3(), (byte) (203 - ByteOffset));
                        Assert.assertEquals("Assert for the entity fields", findEntity0201.getEntity0201_string01(), "ENTITY0201_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0201.getEntity0201_string02(), "ENTITY0201_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0201.getEntity0201_string03(), "ENTITY0201_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0201.setEntity0201_string01("ENTITY0201_STRING01_UPDATED");
                        findEntity0201.setEntity0201_string03("ENTITY0201_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0201.setLockMode(LockModeType.NONE);
                        findEntity0201 = null;
                        findEntity0201 = (Entity0201) selectEntity0201.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0201);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0201);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0201));
                        Assert.assertEquals("Assert for the entity id1", findEntity0201.getEntity0201_id1(), (byte) (201 - ByteOffset));
                        Assert.assertEquals("Assert for the entity id2", findEntity0201.getEntity0201_id2(), (byte) (202 - ByteOffset));
                        Assert.assertEquals("Assert for the entity id3", findEntity0201.getEntity0201_id3(), (byte) (203 - ByteOffset));
                        Assert.assertEquals("Assert for the entity fields", findEntity0201.getEntity0201_string01(), "ENTITY0201_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0201.getEntity0201_string02(), "ENTITY0201_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0201.getEntity0201_string03(), "ENTITY0201_STRING03_UPDATED");
                        break;

                    case Entity0202:
                        Query selectEntity0202 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0202 e WHERE e.entity0202_id1 = :id1_0202 AND e.entity0202_id2 = :id2_0202 AND e.entity0202_id3 = :id3_0202");
                        selectEntity0202.setParameter("id1_0202", (byte) (202 - ByteOffset));
                        selectEntity0202.setParameter("id2_0202", (byte) (203 - ByteOffset));
                        selectEntity0202.setParameter("id3_0202", (byte) (204 - ByteOffset));
                        Entity0202 findEntity0202 = (Entity0202) selectEntity0202.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0202);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0202);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0202));
                        Assert.assertEquals("Assert for the entity id1", (byte) findEntity0202.getEntity0202_id1(), (byte) (202 - ByteOffset));
                        Assert.assertEquals("Assert for the entity id2", (byte) findEntity0202.getEntity0202_id2(), (byte) (203 - ByteOffset));
                        Assert.assertEquals("Assert for the entity id3", (byte) findEntity0202.getEntity0202_id3(), (byte) (204 - ByteOffset));
                        Assert.assertEquals("Assert for the entity fields", findEntity0202.getEntity0202_string01(), "ENTITY0202_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0202.getEntity0202_string02(), "ENTITY0202_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0202.getEntity0202_string03(), "ENTITY0202_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0202.setEntity0202_string01("ENTITY0202_STRING01_UPDATED");
                        findEntity0202.setEntity0202_string03("ENTITY0202_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0202.setLockMode(LockModeType.NONE);
                        findEntity0202 = null;
                        findEntity0202 = (Entity0202) selectEntity0202.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0202);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0202);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0202));
                        Assert.assertEquals("Assert for the entity id1", (byte) findEntity0202.getEntity0202_id1(), (byte) (202 - ByteOffset));
                        Assert.assertEquals("Assert for the entity id2", (byte) findEntity0202.getEntity0202_id2(), (byte) (203 - ByteOffset));
                        Assert.assertEquals("Assert for the entity id3", (byte) findEntity0202.getEntity0202_id3(), (byte) (204 - ByteOffset));
                        Assert.assertEquals("Assert for the entity fields", findEntity0202.getEntity0202_string01(), "ENTITY0202_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0202.getEntity0202_string02(), "ENTITY0202_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0202.getEntity0202_string03(), "ENTITY0202_STRING03_UPDATED");
                        break;

                    case Entity0203:
                        Query selectEntity0203 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0203 e WHERE e.entity0203_id1 = :id1_0203 AND e.entity0203_id2 = :id2_0203 AND e.entity0203_id3 = :id3_0203");
                        selectEntity0203.setParameter("id1_0203", '3');
                        selectEntity0203.setParameter("id2_0203", '4');
                        selectEntity0203.setParameter("id3_0203", '5');
                        Entity0203 findEntity0203 = (Entity0203) selectEntity0203.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0203);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0203);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0203));
                        Assert.assertEquals("Assert for the entity id1", findEntity0203.getEntity0203_id1(), '3');
                        Assert.assertEquals("Assert for the entity id2", findEntity0203.getEntity0203_id2(), '4');
                        Assert.assertEquals("Assert for the entity id3", findEntity0203.getEntity0203_id3(), '5');
                        Assert.assertEquals("Assert for the entity fields", findEntity0203.getEntity0203_string01(), "ENTITY0203_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0203.getEntity0203_string02(), "ENTITY0203_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0203.getEntity0203_string03(), "ENTITY0203_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0203.setEntity0203_string01("ENTITY0203_STRING01_UPDATED");
                        findEntity0203.setEntity0203_string03("ENTITY0203_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0203.setLockMode(LockModeType.NONE);
                        findEntity0203 = null;
                        findEntity0203 = (Entity0203) selectEntity0203.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0203);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0203);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0203));
                        Assert.assertEquals("Assert for the entity id1", findEntity0203.getEntity0203_id1(), '3');
                        Assert.assertEquals("Assert for the entity id2", findEntity0203.getEntity0203_id2(), '4');
                        Assert.assertEquals("Assert for the entity id3", findEntity0203.getEntity0203_id3(), '5');
                        Assert.assertEquals("Assert for the entity fields", findEntity0203.getEntity0203_string01(), "ENTITY0203_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0203.getEntity0203_string02(), "ENTITY0203_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0203.getEntity0203_string03(), "ENTITY0203_STRING03_UPDATED");
                        break;

                    case Entity0204:
                        Query selectEntity0204 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0204 e WHERE e.entity0204_id1 = :id1_0204 AND e.entity0204_id2 = :id2_0204 AND e.entity0204_id3 = :id3_0204");
                        selectEntity0204.setParameter("id1_0204", '4');
                        selectEntity0204.setParameter("id2_0204", '5');
                        selectEntity0204.setParameter("id3_0204", '6');
                        Entity0204 findEntity0204 = (Entity0204) selectEntity0204.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0204);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0204);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0204));
                        Assert.assertEquals("Assert for the entity id1", findEntity0204.getEntity0204_id1(), new Character('4'));
                        Assert.assertEquals("Assert for the entity id2", findEntity0204.getEntity0204_id2(), new Character('5'));
                        Assert.assertEquals("Assert for the entity id3", findEntity0204.getEntity0204_id3(), new Character('6'));
                        Assert.assertEquals("Assert for the entity fields", findEntity0204.getEntity0204_string01(), "ENTITY0204_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0204.getEntity0204_string02(), "ENTITY0204_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0204.getEntity0204_string03(), "ENTITY0204_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0204.setEntity0204_string01("ENTITY0204_STRING01_UPDATED");
                        findEntity0204.setEntity0204_string03("ENTITY0204_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0204.setLockMode(LockModeType.NONE);
                        findEntity0204 = null;
                        findEntity0204 = (Entity0204) selectEntity0204.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0204);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0204);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0204));
                        Assert.assertEquals("Assert for the entity id1", findEntity0204.getEntity0204_id1(), new Character('4'));
                        Assert.assertEquals("Assert for the entity id2", findEntity0204.getEntity0204_id2(), new Character('5'));
                        Assert.assertEquals("Assert for the entity id3", findEntity0204.getEntity0204_id3(), new Character('6'));
                        Assert.assertEquals("Assert for the entity fields", findEntity0204.getEntity0204_string01(), "ENTITY0204_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0204.getEntity0204_string02(), "ENTITY0204_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0204.getEntity0204_string03(), "ENTITY0204_STRING03_UPDATED");
                        break;

                    case Entity0205:
                        Query selectEntity0205 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0205 e WHERE e.entity0205_id1 = :id1_0205 AND e.entity0205_id2 = :id2_0205 AND e.entity0205_id3 = :id3_0205");
                        selectEntity0205.setParameter("id1_0205", "ENTITY0205_ID1");
                        selectEntity0205.setParameter("id2_0205", "ENTITY0205_ID2");
                        selectEntity0205.setParameter("id3_0205", "ENTITY0205_ID3");
                        Entity0205 findEntity0205 = (Entity0205) selectEntity0205.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0205);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0205);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0205));
                        Assert.assertEquals("Assert for the entity id1", findEntity0205.getEntity0205_id1(), "ENTITY0205_ID1");
                        Assert.assertEquals("Assert for the entity id2", findEntity0205.getEntity0205_id2(), "ENTITY0205_ID2");
                        Assert.assertEquals("Assert for the entity id3", findEntity0205.getEntity0205_id3(), "ENTITY0205_ID3");
                        Assert.assertEquals("Assert for the entity fields", findEntity0205.getEntity0205_string01(), "ENTITY0205_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0205.getEntity0205_string02(), "ENTITY0205_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0205.getEntity0205_string03(), "ENTITY0205_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0205.setEntity0205_string01("ENTITY0205_STRING01_UPDATED");
                        findEntity0205.setEntity0205_string03("ENTITY0205_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0205.setLockMode(LockModeType.NONE);
                        findEntity0205 = null;
                        findEntity0205 = (Entity0205) selectEntity0205.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0205);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0205);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0205));
                        Assert.assertEquals("Assert for the entity id1", findEntity0205.getEntity0205_id1(), "ENTITY0205_ID1");
                        Assert.assertEquals("Assert for the entity id2", findEntity0205.getEntity0205_id2(), "ENTITY0205_ID2");
                        Assert.assertEquals("Assert for the entity id3", findEntity0205.getEntity0205_id3(), "ENTITY0205_ID3");
                        Assert.assertEquals("Assert for the entity fields", findEntity0205.getEntity0205_string01(), "ENTITY0205_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0205.getEntity0205_string02(), "ENTITY0205_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0205.getEntity0205_string03(), "ENTITY0205_STRING03_UPDATED");
                        break;

                    case Entity0206:
                        Query selectEntity0206 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0206 e WHERE e.entity0206_id1 = :id1_0206 AND e.entity0206_id2 = :id2_0206 AND e.entity0206_id3 = :id3_0206");
                        selectEntity0206.setParameter("id1_0206", 0206.0206D);
                        selectEntity0206.setParameter("id2_0206", 0207.0207D);
                        selectEntity0206.setParameter("id3_0206", 0208.0208D);
                        Entity0206 findEntity0206 = (Entity0206) selectEntity0206.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0206);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0206);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0206));
                        Assert.assertEquals("Assert for the entity id1", findEntity0206.getEntity0206_id1(), 0206.0206D, 0.1);
                        Assert.assertEquals("Assert for the entity id2", findEntity0206.getEntity0206_id2(), 0207.0207D, 0.1);
                        Assert.assertEquals("Assert for the entity id3", findEntity0206.getEntity0206_id3(), 0208.0208D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0206.getEntity0206_string01(), "ENTITY0206_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0206.getEntity0206_string02(), "ENTITY0206_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0206.getEntity0206_string03(), "ENTITY0206_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0206.setEntity0206_string01("ENTITY0206_STRING01_UPDATED");
                        findEntity0206.setEntity0206_string03("ENTITY0206_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0206.setLockMode(LockModeType.NONE);
                        findEntity0206 = null;
                        findEntity0206 = (Entity0206) selectEntity0206.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0206);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0206);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0206));
                        Assert.assertEquals("Assert for the entity id1", findEntity0206.getEntity0206_id1(), 0206.0206D, 0.1);
                        Assert.assertEquals("Assert for the entity id2", findEntity0206.getEntity0206_id2(), 0207.0207D, 0.1);
                        Assert.assertEquals("Assert for the entity id3", findEntity0206.getEntity0206_id3(), 0208.0208D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0206.getEntity0206_string01(), "ENTITY0206_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0206.getEntity0206_string02(), "ENTITY0206_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0206.getEntity0206_string03(), "ENTITY0206_STRING03_UPDATED");
                        break;

                    case Entity0207:
                        Query selectEntity0207 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0207 e WHERE e.entity0207_id1 = :id1_0207 AND e.entity0207_id2 = :id2_0207 AND e.entity0207_id3 = :id3_0207");
                        selectEntity0207.setParameter("id1_0207", 0207.0207D);
                        selectEntity0207.setParameter("id2_0207", 0208.0208D);
                        selectEntity0207.setParameter("id3_0207", 0209.0209D);
                        Entity0207 findEntity0207 = (Entity0207) selectEntity0207.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0207);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0207);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0207));
                        Assert.assertEquals("Assert for the entity id1", findEntity0207.getEntity0207_id1(), 0207.0207D, 0.1);
                        Assert.assertEquals("Assert for the entity id2", findEntity0207.getEntity0207_id2(), 0208.0208D, 0.1);
                        Assert.assertEquals("Assert for the entity id3", findEntity0207.getEntity0207_id3(), 0209.0209D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0207.getEntity0207_string01(), "ENTITY0207_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0207.getEntity0207_string02(), "ENTITY0207_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0207.getEntity0207_string03(), "ENTITY0207_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0207.setEntity0207_string01("ENTITY0207_STRING01_UPDATED");
                        findEntity0207.setEntity0207_string03("ENTITY0207_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0207.setLockMode(LockModeType.NONE);
                        findEntity0207 = null;
                        findEntity0207 = (Entity0207) selectEntity0207.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0207);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0207);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0207));
                        Assert.assertEquals("Assert for the entity id1", findEntity0207.getEntity0207_id1(), 0207.0207D, 0.1);
                        Assert.assertEquals("Assert for the entity id2", findEntity0207.getEntity0207_id2(), 0208.0208D, 0.1);
                        Assert.assertEquals("Assert for the entity id3", findEntity0207.getEntity0207_id3(), 0209.0209D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0207.getEntity0207_string01(), "ENTITY0207_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0207.getEntity0207_string02(), "ENTITY0207_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0207.getEntity0207_string03(), "ENTITY0207_STRING03_UPDATED");
                        break;

                    case Entity0208:
                        Query selectEntity0208 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0208 e WHERE e.entity0208_id1 = :id1_0208 AND e.entity0208_id2 = :id2_0208 AND e.entity0208_id3 = :id3_0208");
                        selectEntity0208.setParameter("id1_0208", 0208.0208F);
                        selectEntity0208.setParameter("id2_0208", 0209.0209F);
                        selectEntity0208.setParameter("id3_0208", 0210.0210F);
                        Entity0208 findEntity0208 = (Entity0208) selectEntity0208.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0208);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0208);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0208));
                        Assert.assertEquals("Assert for the entity id1", findEntity0208.getEntity0208_id1(), 0208.0208F, 0.1);
                        Assert.assertEquals("Assert for the entity id2", findEntity0208.getEntity0208_id2(), 0209.0209F, 0.1);
                        Assert.assertEquals("Assert for the entity id3", findEntity0208.getEntity0208_id3(), 0210.0210F, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0208.getEntity0208_string01(), "ENTITY0208_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0208.getEntity0208_string02(), "ENTITY0208_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0208.getEntity0208_string03(), "ENTITY0208_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0208.setEntity0208_string01("ENTITY0208_STRING01_UPDATED");
                        findEntity0208.setEntity0208_string03("ENTITY0208_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0208.setLockMode(LockModeType.NONE);
                        //d642065                  findEntity0208 = null;
//                          findEntity0208 = (Entity0208) selectEntity0208.getSingleResult();
//                          System.out.println("Object returned by find: " + findEntity0208);
//                          Assert.assertNotNull("Assert that the find operation did not return null", findEntity0208);
//
//                          System.out.println     ( "Perform parent verifications...");
//                          Assert.assertTrue  ( "Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0208));
//                          Assert.assertEquals    ( "Assert for the entity id1",    findEntity0208.getEntity0208_id1(), 0208.0208F);
//                          Assert.assertEquals    ( "Assert for the entity id2",    findEntity0208.getEntity0208_id2(), 0209.0209F);
//                          Assert.assertEquals    ( "Assert for the entity id3",    findEntity0208.getEntity0208_id3(), 0210.0210F);
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0208.getEntity0208_string01(), "ENTITY0208_STRING01_UPDATED");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0208.getEntity0208_string02(), "ENTITY0208_STRING02");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0208.getEntity0208_string03(), "ENTITY0208_STRING03_UPDATED");
                        break;

                    case Entity0209:
                        Query selectEntity0209 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0209 e WHERE e.entity0209_id1 = :id1_0209 AND e.entity0209_id2 = :id2_0209 AND e.entity0209_id3 = :id3_0209");
                        selectEntity0209.setParameter("id1_0209", 0209.0209F);
                        selectEntity0209.setParameter("id2_0209", 0210.0210F);
                        selectEntity0209.setParameter("id3_0209", 0211.0211F);
                        Entity0209 findEntity0209 = (Entity0209) selectEntity0209.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0209);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0209);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0209));
                        Assert.assertEquals("Assert for the entity id1", findEntity0209.getEntity0209_id1(), 0209.0209F, 0.1);
                        Assert.assertEquals("Assert for the entity id2", findEntity0209.getEntity0209_id2(), 0210.0210F, 0.1);
                        Assert.assertEquals("Assert for the entity id3", findEntity0209.getEntity0209_id3(), 0211.0211F, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0209.getEntity0209_string01(), "ENTITY0209_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0209.getEntity0209_string02(), "ENTITY0209_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0209.getEntity0209_string03(), "ENTITY0209_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0209.setEntity0209_string01("ENTITY0209_STRING01_UPDATED");
                        findEntity0209.setEntity0209_string03("ENTITY0209_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0209.setLockMode(LockModeType.NONE);
                        //d642065                  findEntity0209 = null;
//                          findEntity0208 = (Entity0208) selectEntity0208.getSingleResult();
//                          System.out.println("Object returned by find: " + findEntity0209);
//                          Assert.assertNotNull("Assert that the find operation did not return null", findEntity0209);
//
//                          System.out.println     ( "Perform parent verifications...");
//                          Assert.assertTrue  ( "Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0209));
//                          Assert.assertEquals    ( "Assert for the entity id1",    findEntity0209.getEntity0209_id1(), 0209.0209F);
//                          Assert.assertEquals    ( "Assert for the entity id2",    findEntity0209.getEntity0209_id2(), 0210.0210F);
//                          Assert.assertEquals    ( "Assert for the entity id3",    findEntity0209.getEntity0209_id3(), 0211.0211F);
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0209.getEntity0209_string01(), "ENTITY0209_STRING01_UPDATED");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0209.getEntity0209_string02(), "ENTITY0209_STRING02");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0209.getEntity0209_string03(), "ENTITY0209_STRING03_UPDATED");
                        break;

                    case Entity0210:
                        Query selectEntity0210 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0210 e WHERE e.entity0210_id1 = :id1_0210 AND e.entity0210_id2 = :id2_0210 AND e.entity0210_id3 = :id3_0210");
                        selectEntity0210.setParameter("id1_0210", 210);
                        selectEntity0210.setParameter("id2_0210", 211);
                        selectEntity0210.setParameter("id3_0210", 212);
                        Entity0210 findEntity0210 = (Entity0210) selectEntity0210.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0210);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0210);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0210));
                        Assert.assertEquals("Assert for the entity id1", findEntity0210.getEntity0210_id1(), 210);
                        Assert.assertEquals("Assert for the entity id2", findEntity0210.getEntity0210_id2(), 211);
                        Assert.assertEquals("Assert for the entity id3", findEntity0210.getEntity0210_id3(), 212);
                        Assert.assertEquals("Assert for the entity fields", findEntity0210.getEntity0210_string01(), "ENTITY0210_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0210.getEntity0210_string02(), "ENTITY0210_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0210.getEntity0210_string03(), "ENTITY0210_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0210.setEntity0210_string01("ENTITY0210_STRING01_UPDATED");
                        findEntity0210.setEntity0210_string03("ENTITY0210_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0210.setLockMode(LockModeType.NONE);
                        findEntity0210 = null;
                        findEntity0210 = (Entity0210) selectEntity0210.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0210);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0210);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0210));
                        Assert.assertEquals("Assert for the entity id1", findEntity0210.getEntity0210_id1(), 210);
                        Assert.assertEquals("Assert for the entity id2", findEntity0210.getEntity0210_id2(), 211);
                        Assert.assertEquals("Assert for the entity id3", findEntity0210.getEntity0210_id3(), 212);
                        Assert.assertEquals("Assert for the entity fields", findEntity0210.getEntity0210_string01(), "ENTITY0210_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0210.getEntity0210_string02(), "ENTITY0210_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0210.getEntity0210_string03(), "ENTITY0210_STRING03_UPDATED");
                        break;

                    case Entity0211:
                        Query selectEntity0211 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0211 e WHERE e.entity0211_id1 = :id1_0211 AND e.entity0211_id2 = :id2_0211 AND e.entity0211_id3 = :id3_0211");
                        selectEntity0211.setParameter("id1_0211", 211);
                        selectEntity0211.setParameter("id2_0211", 212);
                        selectEntity0211.setParameter("id3_0211", 213);
                        Entity0211 findEntity0211 = (Entity0211) selectEntity0211.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0211);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0211);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0211));
                        Assert.assertEquals("Assert for the entity id1", (int) findEntity0211.getEntity0211_id1(), 211);
                        Assert.assertEquals("Assert for the entity id2", (int) findEntity0211.getEntity0211_id2(), 212);
                        Assert.assertEquals("Assert for the entity id3", (int) findEntity0211.getEntity0211_id3(), 213);
                        Assert.assertEquals("Assert for the entity fields", findEntity0211.getEntity0211_string01(), "ENTITY0211_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0211.getEntity0211_string02(), "ENTITY0211_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0211.getEntity0211_string03(), "ENTITY0211_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0211.setEntity0211_string01("ENTITY0211_STRING01_UPDATED");
                        findEntity0211.setEntity0211_string03("ENTITY0211_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0211.setLockMode(LockModeType.NONE);
                        findEntity0211 = null;
                        findEntity0211 = (Entity0211) selectEntity0211.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0211);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0211);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0211));
                        Assert.assertEquals("Assert for the entity id1", (int) findEntity0211.getEntity0211_id1(), 211);
                        Assert.assertEquals("Assert for the entity id2", (int) findEntity0211.getEntity0211_id2(), 212);
                        Assert.assertEquals("Assert for the entity id3", (int) findEntity0211.getEntity0211_id3(), 213);
                        Assert.assertEquals("Assert for the entity fields", findEntity0211.getEntity0211_string01(), "ENTITY0211_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0211.getEntity0211_string02(), "ENTITY0211_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0211.getEntity0211_string03(), "ENTITY0211_STRING03_UPDATED");
                        break;

                    case Entity0212:
                        Query selectEntity0212 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0212 e WHERE e.entity0212_id1 = :id1_0212 AND e.entity0212_id2 = :id2_0212 AND e.entity0212_id3 = :id3_0212");
                        selectEntity0212.setParameter("id1_0212", 212L);
                        selectEntity0212.setParameter("id2_0212", 213L);
                        selectEntity0212.setParameter("id3_0212", 214L);
                        Entity0212 findEntity0212 = (Entity0212) selectEntity0212.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0212);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0212);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0212));
                        Assert.assertEquals("Assert for the entity id1", findEntity0212.getEntity0212_id1(), 212L);
                        Assert.assertEquals("Assert for the entity id2", findEntity0212.getEntity0212_id2(), 213L);
                        Assert.assertEquals("Assert for the entity id3", findEntity0212.getEntity0212_id3(), 214L);
                        Assert.assertEquals("Assert for the entity fields", findEntity0212.getEntity0212_string01(), "ENTITY0212_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0212.getEntity0212_string02(), "ENTITY0212_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0212.getEntity0212_string03(), "ENTITY0212_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0212.setEntity0212_string01("ENTITY0212_STRING01_UPDATED");
                        findEntity0212.setEntity0212_string03("ENTITY0212_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0212.setLockMode(LockModeType.NONE);
                        findEntity0212 = null;
                        findEntity0212 = (Entity0212) selectEntity0212.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0212);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0212);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0212));
                        Assert.assertEquals("Assert for the entity id1", findEntity0212.getEntity0212_id1(), 212L);
                        Assert.assertEquals("Assert for the entity id2", findEntity0212.getEntity0212_id2(), 213L);
                        Assert.assertEquals("Assert for the entity id3", findEntity0212.getEntity0212_id3(), 214L);
                        Assert.assertEquals("Assert for the entity fields", findEntity0212.getEntity0212_string01(), "ENTITY0212_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0212.getEntity0212_string02(), "ENTITY0212_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0212.getEntity0212_string03(), "ENTITY0212_STRING03_UPDATED");
                        break;

                    case Entity0213:
                        Query selectEntity0213 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0213 e WHERE e.entity0213_id1 = :id1_0213 AND e.entity0213_id2 = :id2_0213 AND e.entity0213_id3 = :id3_0213");
                        selectEntity0213.setParameter("id1_0213", 213L);
                        selectEntity0213.setParameter("id2_0213", 214L);
                        selectEntity0213.setParameter("id3_0213", 215L);
                        Entity0213 findEntity0213 = (Entity0213) selectEntity0213.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0213);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0213);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0213));
                        Assert.assertEquals("Assert for the entity id1", (long) findEntity0213.getEntity0213_id1(), 213L);
                        Assert.assertEquals("Assert for the entity id2", (long) findEntity0213.getEntity0213_id2(), 214L);
                        Assert.assertEquals("Assert for the entity id3", (long) findEntity0213.getEntity0213_id3(), 215L);
                        Assert.assertEquals("Assert for the entity fields", findEntity0213.getEntity0213_string01(), "ENTITY0213_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0213.getEntity0213_string02(), "ENTITY0213_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0213.getEntity0213_string03(), "ENTITY0213_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0213.setEntity0213_string01("ENTITY0213_STRING01_UPDATED");
                        findEntity0213.setEntity0213_string03("ENTITY0213_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0213.setLockMode(LockModeType.NONE);
                        findEntity0213 = null;
                        findEntity0213 = (Entity0213) selectEntity0213.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0213);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0213);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0213));
                        Assert.assertEquals("Assert for the entity id1", (long) findEntity0213.getEntity0213_id1(), 213L);
                        Assert.assertEquals("Assert for the entity id2", (long) findEntity0213.getEntity0213_id2(), 214L);
                        Assert.assertEquals("Assert for the entity id3", (long) findEntity0213.getEntity0213_id3(), 215L);
                        Assert.assertEquals("Assert for the entity fields", findEntity0213.getEntity0213_string01(), "ENTITY0213_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0213.getEntity0213_string02(), "ENTITY0213_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0213.getEntity0213_string03(), "ENTITY0213_STRING03_UPDATED");
                        break;

                    case Entity0214:
                        Query selectEntity0214 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0214 e WHERE e.entity0214_id1 = :id1_0214 AND e.entity0214_id2 = :id2_0214 AND e.entity0214_id3 = :id3_0214");
                        selectEntity0214.setParameter("id1_0214", (short) 214);
                        selectEntity0214.setParameter("id2_0214", (short) 215);
                        selectEntity0214.setParameter("id3_0214", (short) 216);
                        Entity0214 findEntity0214 = (Entity0214) selectEntity0214.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0214);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0214);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0214));
                        Assert.assertEquals("Assert for the entity id1", findEntity0214.getEntity0214_id1(), (short) 214);
                        Assert.assertEquals("Assert for the entity id2", findEntity0214.getEntity0214_id2(), (short) 215);
                        Assert.assertEquals("Assert for the entity id3", findEntity0214.getEntity0214_id3(), (short) 216);
                        Assert.assertEquals("Assert for the entity fields", findEntity0214.getEntity0214_string01(), "ENTITY0214_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0214.getEntity0214_string02(), "ENTITY0214_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0214.getEntity0214_string03(), "ENTITY0214_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0214.setEntity0214_string01("ENTITY0214_STRING01_UPDATED");
                        findEntity0214.setEntity0214_string03("ENTITY0214_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0214.setLockMode(LockModeType.NONE);
                        findEntity0214 = null;
                        findEntity0214 = (Entity0214) selectEntity0214.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0214);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0214);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0214));
                        Assert.assertEquals("Assert for the entity id1", findEntity0214.getEntity0214_id1(), (short) 214);
                        Assert.assertEquals("Assert for the entity id2", findEntity0214.getEntity0214_id2(), (short) 215);
                        Assert.assertEquals("Assert for the entity id3", findEntity0214.getEntity0214_id3(), (short) 216);
                        Assert.assertEquals("Assert for the entity fields", findEntity0214.getEntity0214_string01(), "ENTITY0214_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0214.getEntity0214_string02(), "ENTITY0214_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0214.getEntity0214_string03(), "ENTITY0214_STRING03_UPDATED");
                        break;

                    case Entity0215:
                        Query selectEntity0215 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0215 e WHERE e.entity0215_id1 = :id1_0215 AND e.entity0215_id2 = :id2_0215 AND e.entity0215_id3 = :id3_0215");
                        selectEntity0215.setParameter("id1_0215", (short) 215);
                        selectEntity0215.setParameter("id2_0215", (short) 216);
                        selectEntity0215.setParameter("id3_0215", (short) 217);
                        Entity0215 findEntity0215 = (Entity0215) selectEntity0215.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0215);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0215);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0215));
                        Assert.assertEquals("Assert for the entity id1", (short) findEntity0215.getEntity0215_id1(), (short) 215);
                        Assert.assertEquals("Assert for the entity id2", (short) findEntity0215.getEntity0215_id2(), (short) 216);
                        Assert.assertEquals("Assert for the entity id3", (short) findEntity0215.getEntity0215_id3(), (short) 217);
                        Assert.assertEquals("Assert for the entity fields", findEntity0215.getEntity0215_string01(), "ENTITY0215_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0215.getEntity0215_string02(), "ENTITY0215_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0215.getEntity0215_string03(), "ENTITY0215_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0215.setEntity0215_string01("ENTITY0215_STRING01_UPDATED");
                        findEntity0215.setEntity0215_string03("ENTITY0215_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0215.setLockMode(LockModeType.NONE);
                        findEntity0215 = null;
                        findEntity0215 = (Entity0215) selectEntity0215.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0215);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0215);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0215));
                        Assert.assertEquals("Assert for the entity id1", (short) findEntity0215.getEntity0215_id1(), (short) 215);
                        Assert.assertEquals("Assert for the entity id2", (short) findEntity0215.getEntity0215_id2(), (short) 216);
                        Assert.assertEquals("Assert for the entity id3", (short) findEntity0215.getEntity0215_id3(), (short) 217);
                        Assert.assertEquals("Assert for the entity fields", findEntity0215.getEntity0215_string01(), "ENTITY0215_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0215.getEntity0215_string02(), "ENTITY0215_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0215.getEntity0215_string03(), "ENTITY0215_STRING03_UPDATED");
                        break;

                    case Entity0216:
                        Query selectEntity0216 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0216 e WHERE e.entity0216_id1 = :id1_0216 AND e.entity0216_id2 = :id2_0216 AND e.entity0216_id3 = :id3_0216");
                        selectEntity0216.setParameter("id1_0216", new BigDecimal("0216.021616"));
                        selectEntity0216.setParameter("id2_0216", new BigDecimal("0217.021717"));
                        selectEntity0216.setParameter("id3_0216", new BigDecimal("0218.021818"));
                        Entity0216 findEntity0216 = (Entity0216) selectEntity0216.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0216);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0216);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0216));
                        Assert.assertTrue("Assert for the entity id1", findEntity0216.getEntity0216_id1().compareTo(new BigDecimal("0216.021616")) == 0);
                        Assert.assertTrue("Assert for the entity id2", findEntity0216.getEntity0216_id2().compareTo(new BigDecimal("0217.021717")) == 0);
                        Assert.assertTrue("Assert for the entity id3", findEntity0216.getEntity0216_id3().compareTo(new BigDecimal("0218.021818")) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0216.getEntity0216_string01(), "ENTITY0216_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0216.getEntity0216_string02(), "ENTITY0216_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0216.getEntity0216_string03(), "ENTITY0216_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0216.setEntity0216_string01("ENTITY0216_STRING01_UPDATED");
                        findEntity0216.setEntity0216_string03("ENTITY0216_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0216.setLockMode(LockModeType.NONE);
                        findEntity0216 = null;
                        findEntity0216 = (Entity0216) selectEntity0216.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0216);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0216);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0216));
                        Assert.assertTrue("Assert for the entity id1", findEntity0216.getEntity0216_id1().compareTo(new BigDecimal("0216.021616")) == 0);
                        Assert.assertTrue("Assert for the entity id2", findEntity0216.getEntity0216_id2().compareTo(new BigDecimal("0217.021717")) == 0);
                        Assert.assertTrue("Assert for the entity id3", findEntity0216.getEntity0216_id3().compareTo(new BigDecimal("0218.021818")) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0216.getEntity0216_string01(), "ENTITY0216_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0216.getEntity0216_string02(), "ENTITY0216_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0216.getEntity0216_string03(), "ENTITY0216_STRING03_UPDATED");
                        break;

                    case Entity0217:
                        Query selectEntity0217 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0217 e WHERE e.entity0217_id1 = :id1_0217 AND e.entity0217_id2 = :id2_0217 AND e.entity0217_id3 = :id3_0217");
                        selectEntity0217.setParameter("id1_0217", new BigInteger("02170217"));
                        selectEntity0217.setParameter("id2_0217", new BigInteger("02180218"));
                        selectEntity0217.setParameter("id3_0217", new BigInteger("02190219"));
                        Entity0217 findEntity0217 = (Entity0217) selectEntity0217.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0217);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0217);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0217));
                        Assert.assertEquals("Assert for the entity id1", findEntity0217.getEntity0217_id1(), new BigInteger("02170217"));
                        Assert.assertEquals("Assert for the entity id2", findEntity0217.getEntity0217_id2(), new BigInteger("02180218"));
                        Assert.assertEquals("Assert for the entity id3", findEntity0217.getEntity0217_id3(), new BigInteger("02190219"));
                        Assert.assertEquals("Assert for the entity fields", findEntity0217.getEntity0217_string01(), "ENTITY0217_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0217.getEntity0217_string02(), "ENTITY0217_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0217.getEntity0217_string03(), "ENTITY0217_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0217.setEntity0217_string01("ENTITY0217_STRING01_UPDATED");
                        findEntity0217.setEntity0217_string03("ENTITY0217_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0217.setLockMode(LockModeType.NONE);
                        findEntity0217 = null;
                        findEntity0217 = (Entity0217) selectEntity0217.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0217);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0217);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0217));
                        Assert.assertEquals("Assert for the entity id1", findEntity0217.getEntity0217_id1(), new BigInteger("02170217"));
                        Assert.assertEquals("Assert for the entity id2", findEntity0217.getEntity0217_id2(), new BigInteger("02180218"));
                        Assert.assertEquals("Assert for the entity id3", findEntity0217.getEntity0217_id3(), new BigInteger("02190219"));
                        Assert.assertEquals("Assert for the entity fields", findEntity0217.getEntity0217_string01(), "ENTITY0217_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0217.getEntity0217_string02(), "ENTITY0217_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0217.getEntity0217_string03(), "ENTITY0217_STRING03_UPDATED");
                        break;

                    case Entity0218:
                        Query selectEntity0218 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0218 e WHERE e.entity0218_id1 = :id1_0218 AND e.entity0218_id2 = :id2_0218 AND e.entity0218_id3 = :id3_0218");
                        selectEntity0218.setParameter("id1_0218", javaUtilDate1);
                        selectEntity0218.setParameter("id2_0218", javaUtilDate2);
                        selectEntity0218.setParameter("id3_0218", javaUtilDate3);
                        Entity0218 findEntity0218 = (Entity0218) selectEntity0218.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0218);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0218);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0218));
                        Assert.assertTrue("Assert for the entity id1", findEntity0218.getEntity0218_id1().compareTo(javaUtilDate1) == 0);
                        Assert.assertTrue("Assert for the entity id2", findEntity0218.getEntity0218_id2().compareTo(javaUtilDate2) == 0);
                        Assert.assertTrue("Assert for the entity id3", findEntity0218.getEntity0218_id3().compareTo(javaUtilDate3) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0218.getEntity0218_string01(), "ENTITY0218_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0218.getEntity0218_string02(), "ENTITY0218_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0218.getEntity0218_string03(), "ENTITY0218_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0218.setEntity0218_string01("ENTITY0218_STRING01_UPDATED");
                        findEntity0218.setEntity0218_string03("ENTITY0218_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0218.setLockMode(LockModeType.NONE);
                        findEntity0218 = null;
                        findEntity0218 = (Entity0218) selectEntity0218.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0218);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0218);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0218));
                        Assert.assertTrue("Assert for the entity id1", findEntity0218.getEntity0218_id1().compareTo(javaUtilDate1) == 0);
                        Assert.assertTrue("Assert for the entity id2", findEntity0218.getEntity0218_id2().compareTo(javaUtilDate2) == 0);
                        Assert.assertTrue("Assert for the entity id3", findEntity0218.getEntity0218_id3().compareTo(javaUtilDate3) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0218.getEntity0218_string01(), "ENTITY0218_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0218.getEntity0218_string02(), "ENTITY0218_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0218.getEntity0218_string03(), "ENTITY0218_STRING03_UPDATED");
                        break;

                    case Entity0219:
                        Query selectEntity0219 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0219 e WHERE e.entity0219_id1 = :id1_0219 AND e.entity0219_id2 = :id2_0219 AND e.entity0219_id3 = :id3_0219");
                        selectEntity0219.setParameter("id1_0219", javaSqlDate1);
                        selectEntity0219.setParameter("id2_0219", javaSqlDate2);
                        selectEntity0219.setParameter("id3_0219", javaSqlDate3);
                        Entity0219 findEntity0219 = (Entity0219) selectEntity0219.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0219);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0219);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0219));
                        Assert.assertTrue("Assert for the entity id1", findEntity0219.getEntity0219_id1().compareTo(javaSqlDate1) == 0);
                        Assert.assertTrue("Assert for the entity id2", findEntity0219.getEntity0219_id2().compareTo(javaSqlDate2) == 0);
                        Assert.assertTrue("Assert for the entity id3", findEntity0219.getEntity0219_id3().compareTo(javaSqlDate3) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0219.getEntity0219_string01(), "ENTITY0219_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0219.getEntity0219_string02(), "ENTITY0219_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0219.getEntity0219_string03(), "ENTITY0219_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0219.setEntity0219_string01("ENTITY0219_STRING01_UPDATED");
                        findEntity0219.setEntity0219_string03("ENTITY0219_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0219.setLockMode(LockModeType.NONE);
                        findEntity0219 = null;
                        findEntity0219 = (Entity0219) selectEntity0219.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0219);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0219);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0219));
                        Assert.assertTrue("Assert for the entity id1", findEntity0219.getEntity0219_id1().compareTo(javaSqlDate1) == 0);
                        Assert.assertTrue("Assert for the entity id2", findEntity0219.getEntity0219_id2().compareTo(javaSqlDate2) == 0);
                        Assert.assertTrue("Assert for the entity id3", findEntity0219.getEntity0219_id3().compareTo(javaSqlDate3) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0219.getEntity0219_string01(), "ENTITY0219_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0219.getEntity0219_string02(), "ENTITY0219_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0219.getEntity0219_string03(), "ENTITY0219_STRING03_UPDATED");
                        break;

                    case Entity0301:
                        Query selectEntity0301 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0301 e WHERE e.entity0301_id1 = :id1_0301 AND e.entity0301_id2 = :id2_0301 AND e.entity0301_id3 = :id3_0301");
                        selectEntity0301.setParameter("id1_0301", (byte) 01);
                        selectEntity0301.setParameter("id2_0301", (byte) 02);
                        selectEntity0301.setParameter("id3_0301", '3');
                        Entity0301 findEntity0301 = (Entity0301) selectEntity0301.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0301);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0301);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0301));
                        Assert.assertEquals("Assert for the entity id1", findEntity0301.getEntity0301_id1(), (byte) 01);
                        Assert.assertEquals("Assert for the entity id2", (byte) findEntity0301.getEntity0301_id2(), (byte) 02);
                        Assert.assertEquals("Assert for the entity id3", findEntity0301.getEntity0301_id3(), '3');
                        Assert.assertEquals("Assert for the entity fields", findEntity0301.getEntity0301_string01(), "ENTITY0301_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0301.getEntity0301_string02(), "ENTITY0301_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0301.getEntity0301_string03(), "ENTITY0301_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0301.setEntity0301_string01("ENTITY0301_STRING01_UPDATED");
                        findEntity0301.setEntity0301_string03("ENTITY0301_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0301.setLockMode(LockModeType.NONE);
                        findEntity0301 = null;
                        findEntity0301 = (Entity0301) selectEntity0301.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0301);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0301);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0301));
                        Assert.assertEquals("Assert for the entity id1", findEntity0301.getEntity0301_id1(), (byte) 01);
                        Assert.assertEquals("Assert for the entity id2", (byte) findEntity0301.getEntity0301_id2(), (byte) 02);
                        Assert.assertEquals("Assert for the entity id3", findEntity0301.getEntity0301_id3(), '3');
                        Assert.assertEquals("Assert for the entity fields", findEntity0301.getEntity0301_string01(), "ENTITY0301_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0301.getEntity0301_string02(), "ENTITY0301_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0301.getEntity0301_string03(), "ENTITY0301_STRING03_UPDATED");
                        break;

                    case Entity0302:
                        Query selectEntity0302 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0302 e WHERE e.entity0302_id1 = :id1_0302 AND e.entity0302_id2 = :id2_0302 AND e.entity0302_id3 = :id3_0302");
                        selectEntity0302.setParameter("id1_0302", (byte) 02);
                        selectEntity0302.setParameter("id2_0302", '3');
                        selectEntity0302.setParameter("id3_0302", '4');
                        Entity0302 findEntity0302 = (Entity0302) selectEntity0302.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0302);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0302);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0302));
                        Assert.assertEquals("Assert for the entity id1", (byte) findEntity0302.getEntity0302_id1(), (byte) 02);
                        Assert.assertEquals("Assert for the entity id2", findEntity0302.getEntity0302_id2(), '3');
                        Assert.assertEquals("Assert for the entity id3", findEntity0302.getEntity0302_id3(), new Character('4'));
                        Assert.assertEquals("Assert for the entity fields", findEntity0302.getEntity0302_string01(), "ENTITY0302_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0302.getEntity0302_string02(), "ENTITY0302_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0302.getEntity0302_string03(), "ENTITY0302_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0302.setEntity0302_string01("ENTITY0302_STRING01_UPDATED");
                        findEntity0302.setEntity0302_string03("ENTITY0302_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0302.setLockMode(LockModeType.NONE);
                        findEntity0302 = null;
                        findEntity0302 = (Entity0302) selectEntity0302.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0302);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0302);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0302));
                        Assert.assertEquals("Assert for the entity id1", (byte) findEntity0302.getEntity0302_id1(), (byte) 02);
                        Assert.assertEquals("Assert for the entity id2", findEntity0302.getEntity0302_id2(), '3');
                        Assert.assertEquals("Assert for the entity id3", findEntity0302.getEntity0302_id3(), new Character('4'));
                        Assert.assertEquals("Assert for the entity fields", findEntity0302.getEntity0302_string01(), "ENTITY0302_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0302.getEntity0302_string02(), "ENTITY0302_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0302.getEntity0302_string03(), "ENTITY0302_STRING03_UPDATED");
                        break;

                    case Entity0303:
                        Query selectEntity0303 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0303 e WHERE e.entity0303_id1 = :id1_0303 AND e.entity0303_id2 = :id2_0303 AND e.entity0303_id3 = :id3_0303");
                        selectEntity0303.setParameter("id1_0303", '3');
                        selectEntity0303.setParameter("id2_0303", '4');
                        selectEntity0303.setParameter("id3_0303", "ENTITY0303_ID3");
                        Entity0303 findEntity0303 = (Entity0303) selectEntity0303.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0303);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0303);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0303));
                        Assert.assertEquals("Assert for the entity id1", findEntity0303.getEntity0303_id1(), '3');
                        Assert.assertEquals("Assert for the entity id2", findEntity0303.getEntity0303_id2(), new Character('4'));
                        Assert.assertEquals("Assert for the entity id3", findEntity0303.getEntity0303_id3(), "ENTITY0303_ID3");
                        Assert.assertEquals("Assert for the entity fields", findEntity0303.getEntity0303_string01(), "ENTITY0303_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0303.getEntity0303_string02(), "ENTITY0303_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0303.getEntity0303_string03(), "ENTITY0303_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0303.setEntity0303_string01("ENTITY0303_STRING01_UPDATED");
                        findEntity0303.setEntity0303_string03("ENTITY0303_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0303.setLockMode(LockModeType.NONE);
                        findEntity0303 = null;
                        findEntity0303 = (Entity0303) selectEntity0303.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0303);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0303);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0303));
                        Assert.assertEquals("Assert for the entity id1", findEntity0303.getEntity0303_id1(), '3');
                        Assert.assertEquals("Assert for the entity id2", findEntity0303.getEntity0303_id2(), new Character('4'));
                        Assert.assertEquals("Assert for the entity id3", findEntity0303.getEntity0303_id3(), "ENTITY0303_ID3");
                        Assert.assertEquals("Assert for the entity fields", findEntity0303.getEntity0303_string01(), "ENTITY0303_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0303.getEntity0303_string02(), "ENTITY0303_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0303.getEntity0303_string03(), "ENTITY0303_STRING03_UPDATED");
                        break;

                    case Entity0304:
                        Query selectEntity0304 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0304 e WHERE e.entity0304_id1 = :id1_0304 AND e.entity0304_id2 = :id2_0304 AND e.entity0304_id3 = :id3_0304");
                        selectEntity0304.setParameter("id1_0304", '4');
                        selectEntity0304.setParameter("id2_0304", "ENTITY0304_ID2");
                        selectEntity0304.setParameter("id3_0304", 0304.0304D);
                        Entity0304 findEntity0304 = (Entity0304) selectEntity0304.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0304);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0304);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0304));
                        Assert.assertEquals("Assert for the entity id1", findEntity0304.getEntity0304_id1(), new Character('4'));
                        Assert.assertEquals("Assert for the entity id2", findEntity0304.getEntity0304_id2(), "ENTITY0304_ID2");
                        Assert.assertEquals("Assert for the entity id3", findEntity0304.getEntity0304_id3(), 0304.0304D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0304.getEntity0304_string01(), "ENTITY0304_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0304.getEntity0304_string02(), "ENTITY0304_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0304.getEntity0304_string03(), "ENTITY0304_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0304.setEntity0304_string01("ENTITY0304_STRING01_UPDATED");
                        findEntity0304.setEntity0304_string03("ENTITY0304_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0304.setLockMode(LockModeType.NONE);
                        findEntity0304 = null;
                        findEntity0304 = (Entity0304) selectEntity0304.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0304);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0304);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0304));
                        Assert.assertEquals("Assert for the entity id1", findEntity0304.getEntity0304_id1(), new Character('4'));
                        Assert.assertEquals("Assert for the entity id2", findEntity0304.getEntity0304_id2(), "ENTITY0304_ID2");
                        Assert.assertEquals("Assert for the entity id3", findEntity0304.getEntity0304_id3(), 0304.0304D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0304.getEntity0304_string01(), "ENTITY0304_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0304.getEntity0304_string02(), "ENTITY0304_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0304.getEntity0304_string03(), "ENTITY0304_STRING03_UPDATED");
                        break;

                    case Entity0305:
                        Query selectEntity0305 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0305 e WHERE e.entity0305_id1 = :id1_0305 AND e.entity0305_id2 = :id2_0305 AND e.entity0305_id3 = :id3_0305");
                        selectEntity0305.setParameter("id1_0305", "ENTITY0305_ID1");
                        selectEntity0305.setParameter("id2_0305", 0305.0305D);
                        selectEntity0305.setParameter("id3_0305", 0306.0306D);
                        Entity0305 findEntity0305 = (Entity0305) selectEntity0305.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0305);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0305);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0305));
                        Assert.assertEquals("Assert for the entity id1", findEntity0305.getEntity0305_id1(), "ENTITY0305_ID1");
                        Assert.assertEquals("Assert for the entity id2", findEntity0305.getEntity0305_id2(), 0305.0305D, 0.1);
                        Assert.assertEquals("Assert for the entity id3", findEntity0305.getEntity0305_id3(), 0306.0306D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0305.getEntity0305_string01(), "ENTITY0305_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0305.getEntity0305_string02(), "ENTITY0305_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0305.getEntity0305_string03(), "ENTITY0305_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0305.setEntity0305_string01("ENTITY0305_STRING01_UPDATED");
                        findEntity0305.setEntity0305_string03("ENTITY0305_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0305.setLockMode(LockModeType.NONE);
                        findEntity0305 = null;
                        findEntity0305 = (Entity0305) selectEntity0305.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0305);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0305);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0305));
                        Assert.assertEquals("Assert for the entity id1", findEntity0305.getEntity0305_id1(), "ENTITY0305_ID1");
                        Assert.assertEquals("Assert for the entity id2", findEntity0305.getEntity0305_id2(), 0305.0305D, 0.1);
                        Assert.assertEquals("Assert for the entity id3", findEntity0305.getEntity0305_id3(), 0306.0306D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0305.getEntity0305_string01(), "ENTITY0305_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0305.getEntity0305_string02(), "ENTITY0305_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0305.getEntity0305_string03(), "ENTITY0305_STRING03_UPDATED");
                        break;

                    case Entity0306:
                        Query selectEntity0306 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0306 e WHERE e.entity0306_id1 = :id1_0306 AND e.entity0306_id2 = :id2_0306 AND e.entity0306_id3 = :id3_0306");
                        selectEntity0306.setParameter("id1_0306", 0306.0306D);
                        selectEntity0306.setParameter("id2_0306", 0307.0307D);
                        selectEntity0306.setParameter("id3_0306", 0308.0308F);
                        Entity0306 findEntity0306 = (Entity0306) selectEntity0306.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0306);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0306);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0306));
                        Assert.assertEquals("Assert for the entity id1", findEntity0306.getEntity0306_id1(), 0306.0306D, 0.1);
                        Assert.assertEquals("Assert for the entity id2", findEntity0306.getEntity0306_id2(), 0307.0307D, 0.1);
                        Assert.assertEquals("Assert for the entity id3", findEntity0306.getEntity0306_id3(), 0308.0308F, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0306.getEntity0306_string01(), "ENTITY0306_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0306.getEntity0306_string02(), "ENTITY0306_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0306.getEntity0306_string03(), "ENTITY0306_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0306.setEntity0306_string01("ENTITY0306_STRING01_UPDATED");
                        findEntity0306.setEntity0306_string03("ENTITY0306_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0306.setLockMode(LockModeType.NONE);
                        //d642065                  findEntity0306 = null;
//                          findEntity0306 = (Entity0306) selectEntity0306.getSingleResult();
//                          System.out.println("Object returned by find: " + findEntity0306);
//                          Assert.assertNotNull("Assert that the find operation did not return null", findEntity0306);
//
//                          System.out.println     ( "Perform parent verifications...");
//                          Assert.assertTrue  ( "Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0306));
//                          Assert.assertEquals    ( "Assert for the entity id1",    findEntity0306.getEntity0306_id1(), 0306.0306D);
//                          Assert.assertEquals    ( "Assert for the entity id2",    findEntity0306.getEntity0306_id2(), 0307.0307D);
//                          Assert.assertEquals    ( "Assert for the entity id3",    findEntity0306.getEntity0306_id3(), 0308.0308F);
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0306.getEntity0306_string01(), "ENTITY0306_STRING01_UPDATED");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0306.getEntity0306_string02(), "ENTITY0306_STRING02");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0306.getEntity0306_string03(), "ENTITY0306_STRING03_UPDATED");
                        break;

                    case Entity0307:
                        Query selectEntity0307 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0307 e WHERE e.entity0307_id1 = :id1_0307 AND e.entity0307_id2 = :id2_0307 AND e.entity0307_id3 = :id3_0307");
                        selectEntity0307.setParameter("id1_0307", 0307.0307D);
                        selectEntity0307.setParameter("id2_0307", 0308.0308F);
                        selectEntity0307.setParameter("id3_0307", 0309.0309F);
                        Entity0307 findEntity0307 = (Entity0307) selectEntity0307.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0307);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0307);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0307));
                        Assert.assertEquals("Assert for the entity id1", findEntity0307.getEntity0307_id1(), 0307.0307D, 0.1);
                        Assert.assertEquals("Assert for the entity id2", findEntity0307.getEntity0307_id2(), 0308.0308F, 0.1);
                        Assert.assertEquals("Assert for the entity id3", findEntity0307.getEntity0307_id3(), 0309.0309F, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0307.getEntity0307_string01(), "ENTITY0307_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0307.getEntity0307_string02(), "ENTITY0307_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0307.getEntity0307_string03(), "ENTITY0307_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0307.setEntity0307_string01("ENTITY0307_STRING01_UPDATED");
                        findEntity0307.setEntity0307_string03("ENTITY0307_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0307.setLockMode(LockModeType.NONE);
                        //d642065                  findEntity0307 = null;
//                          findEntity0307 = (Entity0307) selectEntity0307.getSingleResult();
//                          System.out.println("Object returned by find: " + findEntity0307);
//                          Assert.assertNotNull("Assert that the find operation did not return null", findEntity0307);
//
//                          System.out.println     ( "Perform parent verifications...");
//                          Assert.assertTrue  ( "Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0307));
//                          Assert.assertEquals    ( "Assert for the entity id1",    findEntity0307.getEntity0307_id1(), 0307.0307D);
//                          Assert.assertEquals    ( "Assert for the entity id2",    findEntity0307.getEntity0307_id2(), 0308.0308F);
//                          Assert.assertEquals    ( "Assert for the entity id3",    findEntity0307.getEntity0307_id3(), 0309.0309F);
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0307.getEntity0307_string01(), "ENTITY0307_STRING01_UPDATED");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0307.getEntity0307_string02(), "ENTITY0307_STRING02");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0307.getEntity0307_string03(), "ENTITY0307_STRING03_UPDATED");
                        break;

                    case Entity0308:
                        Query selectEntity0308 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0308 e WHERE e.entity0308_id1 = :id1_0308 AND e.entity0308_id2 = :id2_0308 AND e.entity0308_id3 = :id3_0308");
                        selectEntity0308.setParameter("id1_0308", 0308.0308F);
                        selectEntity0308.setParameter("id2_0308", 0309.0309F);
                        selectEntity0308.setParameter("id3_0308", 310);
                        Entity0308 findEntity0308 = (Entity0308) selectEntity0308.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0308);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0308);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0308));
                        Assert.assertEquals("Assert for the entity id1", findEntity0308.getEntity0308_id1(), 0308.0308F, 0.1);
                        Assert.assertEquals("Assert for the entity id2", findEntity0308.getEntity0308_id2(), 0309.0309F, 0.1);
                        Assert.assertEquals("Assert for the entity id3", findEntity0308.getEntity0308_id3(), 310);
                        Assert.assertEquals("Assert for the entity fields", findEntity0308.getEntity0308_string01(), "ENTITY0308_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0308.getEntity0308_string02(), "ENTITY0308_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0308.getEntity0308_string03(), "ENTITY0308_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0308.setEntity0308_string01("ENTITY0308_STRING01_UPDATED");
                        findEntity0308.setEntity0308_string03("ENTITY0308_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0308.setLockMode(LockModeType.NONE);
                        //d642065                  findEntity0308 = null;
//                          findEntity0308 = (Entity0308) selectEntity0308.getSingleResult();
//                          System.out.println("Object returned by find: " + findEntity0308);
//                          Assert.assertNotNull("Assert that the find operation did not return null", findEntity0308);
//
//                          System.out.println     ( "Perform parent verifications...");
//                          Assert.assertTrue  ( "Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0308));
//                          Assert.assertEquals    ( "Assert for the entity id1",    findEntity0308.getEntity0308_id1(), 0308.0308F);
//                          Assert.assertEquals    ( "Assert for the entity id2",    findEntity0308.getEntity0308_id2(), 0309.0309F);
//                          Assert.assertEquals    ( "Assert for the entity id3",    findEntity0308.getEntity0308_id3(), 310);
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0308.getEntity0308_string01(), "ENTITY0308_STRING01_UPDATED");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0308.getEntity0308_string02(), "ENTITY0308_STRING02");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0308.getEntity0308_string03(), "ENTITY0308_STRING03_UPDATED");
                        break;

                    case Entity0309:
                        Query selectEntity0309 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0309 e WHERE e.entity0309_id1 = :id1_0309 AND e.entity0309_id2 = :id2_0309 AND e.entity0309_id3 = :id3_0309");
                        selectEntity0309.setParameter("id1_0309", 0309.0309F);
                        selectEntity0309.setParameter("id2_0309", 310);
                        selectEntity0309.setParameter("id3_0309", 311);
                        Entity0309 findEntity0309 = (Entity0309) selectEntity0309.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0309);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0309);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0309));
                        Assert.assertEquals("Assert for the entity id1", findEntity0309.getEntity0309_id1(), 0309.0309F, 0.1);
                        Assert.assertEquals("Assert for the entity id2", findEntity0309.getEntity0309_id2(), 310);
                        Assert.assertEquals("Assert for the entity id3", (int) findEntity0309.getEntity0309_id3(), 311);
                        Assert.assertEquals("Assert for the entity fields", findEntity0309.getEntity0309_string01(), "ENTITY0309_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0309.getEntity0309_string02(), "ENTITY0309_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0309.getEntity0309_string03(), "ENTITY0309_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0309.setEntity0309_string01("ENTITY0309_STRING01_UPDATED");
                        findEntity0309.setEntity0309_string03("ENTITY0309_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0309.setLockMode(LockModeType.NONE);
                        //d642065                  findEntity0309 = null;
//                          findEntity0309 = (Entity0309) selectEntity0309.getSingleResult();
//                          System.out.println("Object returned by find: " + findEntity0309);
//                          Assert.assertNotNull("Assert that the find operation did not return null", findEntity0309);
//
//                          System.out.println     ( "Perform parent verifications...");
//                          Assert.assertTrue  ( "Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0309));
//                          Assert.assertEquals    ( "Assert for the entity id1",    findEntity0309.getEntity0309_id1(), 0309.0309F);
//                          Assert.assertEquals    ( "Assert for the entity id2",    findEntity0309.getEntity0309_id2(), 310);
//                          Assert.assertEquals    ( "Assert for the entity id3",    findEntity0309.getEntity0309_id3(), 311);
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0309.getEntity0309_string01(), "ENTITY0309_STRING01_UPDATED");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0309.getEntity0309_string02(), "ENTITY0309_STRING02");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0309.getEntity0309_string03(), "ENTITY0309_STRING03_UPDATED");
                        break;

                    case Entity0310:
                        Query selectEntity0310 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0310 e WHERE e.entity0310_id1 = :id1_0310 AND e.entity0310_id2 = :id2_0310 AND e.entity0310_id3 = :id3_0310");
                        selectEntity0310.setParameter("id1_0310", 310);
                        selectEntity0310.setParameter("id2_0310", 311);
                        selectEntity0310.setParameter("id3_0310", 312L);
                        Entity0310 findEntity0310 = (Entity0310) selectEntity0310.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0310);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0310);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0310));
                        Assert.assertEquals("Assert for the entity id1", findEntity0310.getEntity0310_id1(), 310);
                        Assert.assertEquals("Assert for the entity id2", (int) findEntity0310.getEntity0310_id2(), 311);
                        Assert.assertEquals("Assert for the entity id3", findEntity0310.getEntity0310_id3(), 312L);
                        Assert.assertEquals("Assert for the entity fields", findEntity0310.getEntity0310_string01(), "ENTITY0310_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0310.getEntity0310_string02(), "ENTITY0310_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0310.getEntity0310_string03(), "ENTITY0310_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0310.setEntity0310_string01("ENTITY0310_STRING01_UPDATED");
                        findEntity0310.setEntity0310_string03("ENTITY0310_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0310.setLockMode(LockModeType.NONE);
                        findEntity0310 = null;
                        findEntity0310 = (Entity0310) selectEntity0310.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0310);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0310);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0310));
                        Assert.assertEquals("Assert for the entity id1", findEntity0310.getEntity0310_id1(), 310);
                        Assert.assertEquals("Assert for the entity id2", (int) findEntity0310.getEntity0310_id2(), 311);
                        Assert.assertEquals("Assert for the entity id3", findEntity0310.getEntity0310_id3(), 312L);
                        Assert.assertEquals("Assert for the entity fields", findEntity0310.getEntity0310_string01(), "ENTITY0310_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0310.getEntity0310_string02(), "ENTITY0310_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0310.getEntity0310_string03(), "ENTITY0310_STRING03_UPDATED");
                        break;

                    case Entity0311:
                        Query selectEntity0311 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0311 e WHERE e.entity0311_id1 = :id1_0311 AND e.entity0311_id2 = :id2_0311 AND e.entity0311_id3 = :id3_0311");
                        selectEntity0311.setParameter("id1_0311", 311);
                        selectEntity0311.setParameter("id2_0311", 312L);
                        selectEntity0311.setParameter("id3_0311", 313L);
                        Entity0311 findEntity0311 = (Entity0311) selectEntity0311.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0311);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0311);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0311));
                        Assert.assertEquals("Assert for the entity id1", (int) findEntity0311.getEntity0311_id1(), 311);
                        Assert.assertEquals("Assert for the entity id2", findEntity0311.getEntity0311_id2(), 312L);
                        Assert.assertEquals("Assert for the entity id3", (long) findEntity0311.getEntity0311_id3(), 313L);
                        Assert.assertEquals("Assert for the entity fields", findEntity0311.getEntity0311_string01(), "ENTITY0311_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0311.getEntity0311_string02(), "ENTITY0311_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0311.getEntity0311_string03(), "ENTITY0311_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0311.setEntity0311_string01("ENTITY0311_STRING01_UPDATED");
                        findEntity0311.setEntity0311_string03("ENTITY0311_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0311.setLockMode(LockModeType.NONE);
                        findEntity0311 = null;
                        findEntity0311 = (Entity0311) selectEntity0311.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0311);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0311);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0311));
                        Assert.assertEquals("Assert for the entity id1", (int) findEntity0311.getEntity0311_id1(), 311);
                        Assert.assertEquals("Assert for the entity id2", findEntity0311.getEntity0311_id2(), 312L);
                        Assert.assertEquals("Assert for the entity id3", (long) findEntity0311.getEntity0311_id3(), 313L);
                        Assert.assertEquals("Assert for the entity fields", findEntity0311.getEntity0311_string01(), "ENTITY0311_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0311.getEntity0311_string02(), "ENTITY0311_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0311.getEntity0311_string03(), "ENTITY0311_STRING03_UPDATED");
                        break;

                    case Entity0312:
                        Query selectEntity0312 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0312 e WHERE e.entity0312_id1 = :id1_0312 AND e.entity0312_id2 = :id2_0312 AND e.entity0312_id3 = :id3_0312");
                        selectEntity0312.setParameter("id1_0312", 312L);
                        selectEntity0312.setParameter("id2_0312", 313L);
                        selectEntity0312.setParameter("id3_0312", (short) 314);
                        Entity0312 findEntity0312 = (Entity0312) selectEntity0312.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0312);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0312);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0312));
                        Assert.assertEquals("Assert for the entity id1", findEntity0312.getEntity0312_id1(), 312L);
                        Assert.assertEquals("Assert for the entity id2", (long) findEntity0312.getEntity0312_id2(), 313L);
                        Assert.assertEquals("Assert for the entity id3", findEntity0312.getEntity0312_id3(), (short) 314);
                        Assert.assertEquals("Assert for the entity fields", findEntity0312.getEntity0312_string01(), "ENTITY0312_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0312.getEntity0312_string02(), "ENTITY0312_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0312.getEntity0312_string03(), "ENTITY0312_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0312.setEntity0312_string01("ENTITY0312_STRING01_UPDATED");
                        findEntity0312.setEntity0312_string03("ENTITY0312_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0312.setLockMode(LockModeType.NONE);
                        findEntity0312 = null;
                        findEntity0312 = (Entity0312) selectEntity0312.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0312);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0312);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0312));
                        Assert.assertEquals("Assert for the entity id1", findEntity0312.getEntity0312_id1(), 312L);
                        Assert.assertEquals("Assert for the entity id2", (long) findEntity0312.getEntity0312_id2(), 313L);
                        Assert.assertEquals("Assert for the entity id3", findEntity0312.getEntity0312_id3(), (short) 314);
                        Assert.assertEquals("Assert for the entity fields", findEntity0312.getEntity0312_string01(), "ENTITY0312_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0312.getEntity0312_string02(), "ENTITY0312_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0312.getEntity0312_string03(), "ENTITY0312_STRING03_UPDATED");
                        break;

                    case Entity0313:
                        Query selectEntity0313 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0313 e WHERE e.entity0313_id1 = :id1_0313 AND e.entity0313_id2 = :id2_0313 AND e.entity0313_id3 = :id3_0313");
                        selectEntity0313.setParameter("id1_0313", 313L);
                        selectEntity0313.setParameter("id2_0313", (short) 314);
                        selectEntity0313.setParameter("id3_0313", (short) 315);
                        Entity0313 findEntity0313 = (Entity0313) selectEntity0313.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0313);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0313);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0313));
                        Assert.assertEquals("Assert for the entity id1", (long) findEntity0313.getEntity0313_id1(), 313L);
                        Assert.assertEquals("Assert for the entity id2", findEntity0313.getEntity0313_id2(), (short) 314);
                        Assert.assertEquals("Assert for the entity id3", (short) findEntity0313.getEntity0313_id3(), (short) 315);
                        Assert.assertEquals("Assert for the entity fields", findEntity0313.getEntity0313_string01(), "ENTITY0313_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0313.getEntity0313_string02(), "ENTITY0313_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0313.getEntity0313_string03(), "ENTITY0313_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0313.setEntity0313_string01("ENTITY0313_STRING01_UPDATED");
                        findEntity0313.setEntity0313_string03("ENTITY0313_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0313.setLockMode(LockModeType.NONE);
                        findEntity0313 = null;
                        findEntity0313 = (Entity0313) selectEntity0313.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0313);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0313);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0313));
                        Assert.assertEquals("Assert for the entity id1", (long) findEntity0313.getEntity0313_id1(), 313L);
                        Assert.assertEquals("Assert for the entity id2", findEntity0313.getEntity0313_id2(), (short) 314);
                        Assert.assertEquals("Assert for the entity id3", (short) findEntity0313.getEntity0313_id3(), (short) 315);
                        Assert.assertEquals("Assert for the entity fields", findEntity0313.getEntity0313_string01(), "ENTITY0313_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0313.getEntity0313_string02(), "ENTITY0313_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0313.getEntity0313_string03(), "ENTITY0313_STRING03_UPDATED");
                        break;

                    case Entity0314:
                        Query selectEntity0314 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0314 e WHERE e.entity0314_id1 = :id1_0314 AND e.entity0314_id2 = :id2_0314 AND e.entity0314_id3 = :id3_0314");
                        selectEntity0314.setParameter("id1_0314", (short) 314);
                        selectEntity0314.setParameter("id2_0314", (short) 315);
                        selectEntity0314.setParameter("id3_0314", new BigDecimal("0316.031616"));
                        Entity0314 findEntity0314 = (Entity0314) selectEntity0314.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0314);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0314);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0314));
                        Assert.assertEquals("Assert for the entity id1", findEntity0314.getEntity0314_id1(), (short) 314);
                        Assert.assertEquals("Assert for the entity id2", (short) findEntity0314.getEntity0314_id2(), (short) 315);
                        Assert.assertTrue("Assert for the entity id3", findEntity0314.getEntity0314_id3().compareTo(new BigDecimal("0316.031616")) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0314.getEntity0314_string01(), "ENTITY0314_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0314.getEntity0314_string02(), "ENTITY0314_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0314.getEntity0314_string03(), "ENTITY0314_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0314.setEntity0314_string01("ENTITY0314_STRING01_UPDATED");
                        findEntity0314.setEntity0314_string03("ENTITY0314_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0314.setLockMode(LockModeType.NONE);
                        findEntity0314 = null;
                        findEntity0314 = (Entity0314) selectEntity0314.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0314);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0314);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0314));
                        Assert.assertEquals("Assert for the entity id1", findEntity0314.getEntity0314_id1(), (short) 314);
                        Assert.assertEquals("Assert for the entity id2", (short) findEntity0314.getEntity0314_id2(), (short) 315);
                        Assert.assertTrue("Assert for the entity id3", findEntity0314.getEntity0314_id3().compareTo(new BigDecimal("0316.031616")) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0314.getEntity0314_string01(), "ENTITY0314_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0314.getEntity0314_string02(), "ENTITY0314_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0314.getEntity0314_string03(), "ENTITY0314_STRING03_UPDATED");
                        break;

                    case Entity0315:
                        Query selectEntity0315 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0315 e WHERE e.entity0315_id1 = :id1_0315 AND e.entity0315_id2 = :id2_0315 AND e.entity0315_id3 = :id3_0315");
                        selectEntity0315.setParameter("id1_0315", (short) 315);
                        selectEntity0315.setParameter("id2_0315", new BigDecimal("0316.031616"));
                        selectEntity0315.setParameter("id3_0315", new BigInteger("03170317"));
                        Entity0315 findEntity0315 = (Entity0315) selectEntity0315.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0315);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0315);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0315));
                        Assert.assertEquals("Assert for the entity id1", (short) findEntity0315.getEntity0315_id1(), (short) 315);
                        Assert.assertTrue("Assert for the entity id2", findEntity0315.getEntity0315_id2().compareTo(new BigDecimal("0316.031616")) == 0);
                        Assert.assertEquals("Assert for the entity id3", findEntity0315.getEntity0315_id3(), new BigInteger("03170317"));
                        Assert.assertEquals("Assert for the entity fields", findEntity0315.getEntity0315_string01(), "ENTITY0315_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0315.getEntity0315_string02(), "ENTITY0315_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0315.getEntity0315_string03(), "ENTITY0315_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0315.setEntity0315_string01("ENTITY0315_STRING01_UPDATED");
                        findEntity0315.setEntity0315_string03("ENTITY0315_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0315.setLockMode(LockModeType.NONE);
                        findEntity0315 = null;
                        findEntity0315 = (Entity0315) selectEntity0315.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0315);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0315);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0315));
                        Assert.assertEquals("Assert for the entity id1", (short) findEntity0315.getEntity0315_id1(), (short) 315);
                        Assert.assertTrue("Assert for the entity id2", findEntity0315.getEntity0315_id2().compareTo(new BigDecimal("0316.031616")) == 0);
                        Assert.assertEquals("Assert for the entity id3", findEntity0315.getEntity0315_id3(), new BigInteger("03170317"));
                        Assert.assertEquals("Assert for the entity fields", findEntity0315.getEntity0315_string01(), "ENTITY0315_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0315.getEntity0315_string02(), "ENTITY0315_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0315.getEntity0315_string03(), "ENTITY0315_STRING03_UPDATED");
                        break;

                    case Entity0316:
                        Query selectEntity0316 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0316 e WHERE e.entity0316_id1 = :id1_0316 AND e.entity0316_id2 = :id2_0316 AND e.entity0316_id3 = :id3_0316");
                        selectEntity0316.setParameter("id1_0316", new BigDecimal("0316.031616"));
                        selectEntity0316.setParameter("id2_0316", new BigInteger("03170317"));
                        selectEntity0316.setParameter("id3_0316", javaUtilDate3);
                        Entity0316 findEntity0316 = (Entity0316) selectEntity0316.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0316);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0316);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0316));
                        Assert.assertTrue("Assert for the entity id1", findEntity0316.getEntity0316_id1().compareTo(new BigDecimal("0316.031616")) == 0);
                        Assert.assertEquals("Assert for the entity id2", findEntity0316.getEntity0316_id2(), new BigInteger("03170317"));
                        Assert.assertTrue("Assert for the entity id3", findEntity0316.getEntity0316_id3().compareTo(javaUtilDate3) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0316.getEntity0316_string01(), "ENTITY0316_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0316.getEntity0316_string02(), "ENTITY0316_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0316.getEntity0316_string03(), "ENTITY0316_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0316.setEntity0316_string01("ENTITY0316_STRING01_UPDATED");
                        findEntity0316.setEntity0316_string03("ENTITY0316_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0316.setLockMode(LockModeType.NONE);
                        findEntity0316 = null;
                        findEntity0316 = (Entity0316) selectEntity0316.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0316);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0316);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0316));
                        Assert.assertTrue("Assert for the entity id1", findEntity0316.getEntity0316_id1().compareTo(new BigDecimal("0316.031616")) == 0);
                        Assert.assertEquals("Assert for the entity id2", findEntity0316.getEntity0316_id2(), new BigInteger("03170317"));
                        Assert.assertTrue("Assert for the entity id3", findEntity0316.getEntity0316_id3().compareTo(javaUtilDate3) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0316.getEntity0316_string01(), "ENTITY0316_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0316.getEntity0316_string02(), "ENTITY0316_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0316.getEntity0316_string03(), "ENTITY0316_STRING03_UPDATED");
                        break;

                    case Entity0317:
                        Query selectEntity0317 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0317 e WHERE e.entity0317_id1 = :id1_0317 AND e.entity0317_id2 = :id2_0317 AND e.entity0317_id3 = :id3_0317");
                        selectEntity0317.setParameter("id1_0317", new BigInteger("03170317"));
                        selectEntity0317.setParameter("id2_0317", javaUtilDate2);
                        selectEntity0317.setParameter("id3_0317", javaSqlDate3);
                        Entity0317 findEntity0317 = (Entity0317) selectEntity0317.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0317);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0317);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0317));
                        Assert.assertEquals("Assert for the entity id1", findEntity0317.getEntity0317_id1(), new BigInteger("03170317"));
                        Assert.assertTrue("Assert for the entity id2", findEntity0317.getEntity0317_id2().compareTo(javaUtilDate2) == 0);
                        Assert.assertTrue("Assert for the entity id3", findEntity0317.getEntity0317_id3().compareTo(javaSqlDate3) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0317.getEntity0317_string01(), "ENTITY0317_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0317.getEntity0317_string02(), "ENTITY0317_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0317.getEntity0317_string03(), "ENTITY0317_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0317.setEntity0317_string01("ENTITY0317_STRING01_UPDATED");
                        findEntity0317.setEntity0317_string03("ENTITY0317_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0317.setLockMode(LockModeType.NONE);
                        findEntity0317 = null;
                        findEntity0317 = (Entity0317) selectEntity0317.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0317);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0317);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0317));
                        Assert.assertEquals("Assert for the entity id1", findEntity0317.getEntity0317_id1(), new BigInteger("03170317"));
                        Assert.assertTrue("Assert for the entity id2", findEntity0317.getEntity0317_id2().compareTo(javaUtilDate2) == 0);
                        Assert.assertTrue("Assert for the entity id3", findEntity0317.getEntity0317_id3().compareTo(javaSqlDate3) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0317.getEntity0317_string01(), "ENTITY0317_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0317.getEntity0317_string02(), "ENTITY0317_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0317.getEntity0317_string03(), "ENTITY0317_STRING03_UPDATED");
                        break;

                    case Entity0318:
                        Query selectEntity0318 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0318 e WHERE e.entity0318_id1 = :id1_0318 AND e.entity0318_id2 = :id2_0318 AND e.entity0318_id3 = :id3_0318");
                        selectEntity0318.setParameter("id1_0318", javaUtilDate1);
                        selectEntity0318.setParameter("id2_0318", javaSqlDate2);
                        selectEntity0318.setParameter("id3_0318", javaUtilDate3);
                        Entity0318 findEntity0318 = (Entity0318) selectEntity0318.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0318);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0318);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0318));
                        Assert.assertTrue("Assert for the entity id1", findEntity0318.getEntity0318_id1().compareTo(javaUtilDate1) == 0);
                        Assert.assertTrue("Assert for the entity id2", findEntity0318.getEntity0318_id2().compareTo(javaSqlDate2) == 0);
                        Assert.assertTrue("Assert for the entity id3", findEntity0318.getEntity0318_id3().compareTo(javaUtilDate3) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0318.getEntity0318_string01(), "ENTITY0318_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0318.getEntity0318_string02(), "ENTITY0318_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0318.getEntity0318_string03(), "ENTITY0318_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0318.setEntity0318_string01("ENTITY0318_STRING01_UPDATED");
                        findEntity0318.setEntity0318_string03("ENTITY0318_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0318.setLockMode(LockModeType.NONE);
                        findEntity0318 = null;
                        findEntity0318 = (Entity0318) selectEntity0318.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0318);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0318);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0318));
                        Assert.assertTrue("Assert for the entity id1", findEntity0318.getEntity0318_id1().compareTo(javaUtilDate1) == 0);
                        Assert.assertTrue("Assert for the entity id2", findEntity0318.getEntity0318_id2().compareTo(javaSqlDate2) == 0);
                        Assert.assertTrue("Assert for the entity id3", findEntity0318.getEntity0318_id3().compareTo(javaUtilDate3) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0318.getEntity0318_string01(), "ENTITY0318_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0318.getEntity0318_string02(), "ENTITY0318_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0318.getEntity0318_string03(), "ENTITY0318_STRING03_UPDATED");
                        break;

                    case Entity0319:
                        Query selectEntity0319 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0319 e WHERE e.entity0319_id1 = :id1_0319 AND e.entity0319_id2 = :id2_0319 AND e.entity0319_id3 = :id3_0319");
                        selectEntity0319.setParameter("id1_0319", javaSqlDate1);
                        selectEntity0319.setParameter("id2_0319", javaUtilDate2);
                        selectEntity0319.setParameter("id3_0319", javaSqlDate3);
                        Entity0319 findEntity0319 = (Entity0319) selectEntity0319.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0319);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0319);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0319));
                        Assert.assertTrue("Assert for the entity id1", findEntity0319.getEntity0319_id1().compareTo(javaSqlDate1) == 0);
                        Assert.assertTrue("Assert for the entity id2", findEntity0319.getEntity0319_id2().compareTo(javaUtilDate2) == 0);
                        Assert.assertTrue("Assert for the entity id3", findEntity0319.getEntity0319_id3().compareTo(javaSqlDate3) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0319.getEntity0319_string01(), "ENTITY0319_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0319.getEntity0319_string02(), "ENTITY0319_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0319.getEntity0319_string03(), "ENTITY0319_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0319.setEntity0319_string01("ENTITY0319_STRING01_UPDATED");
                        findEntity0319.setEntity0319_string03("ENTITY0319_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0319.setLockMode(LockModeType.NONE);
                        findEntity0319 = null;
                        findEntity0319 = (Entity0319) selectEntity0319.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0319);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0319);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0319));
                        Assert.assertTrue("Assert for the entity id1", findEntity0319.getEntity0319_id1().compareTo(javaSqlDate1) == 0);
                        Assert.assertTrue("Assert for the entity id2", findEntity0319.getEntity0319_id2().compareTo(javaUtilDate2) == 0);
                        Assert.assertTrue("Assert for the entity id3", findEntity0319.getEntity0319_id3().compareTo(javaSqlDate3) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0319.getEntity0319_string01(), "ENTITY0319_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0319.getEntity0319_string02(), "ENTITY0319_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0319.getEntity0319_string03(), "ENTITY0319_STRING03_UPDATED");
                        break;

                    case Entity0320:
                        Query selectEntity0320 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0320 e WHERE e.entity0320_id1 = :id1_0320 AND e.entity0320_id2 = :id2_0320 AND e.entity0320_id3 = :id3_0320");
                        selectEntity0320.setParameter("id1_0320", (byte) 20);
                        selectEntity0320.setParameter("id2_0320", '1');
                        selectEntity0320.setParameter("id3_0320", "ENTITY0320_ID3");
                        Entity0320 findEntity0320 = (Entity0320) selectEntity0320.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0320);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0320);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0320));
                        Assert.assertEquals("Assert for the entity id1", findEntity0320.getEntity0320_id1(), (byte) 20);
                        Assert.assertEquals("Assert for the entity id2", findEntity0320.getEntity0320_id2(), '1');
                        Assert.assertEquals("Assert for the entity id3", findEntity0320.getEntity0320_id3(), "ENTITY0320_ID3");
                        Assert.assertEquals("Assert for the entity fields", findEntity0320.getEntity0320_string01(), "ENTITY0320_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0320.getEntity0320_string02(), "ENTITY0320_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0320.getEntity0320_string03(), "ENTITY0320_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0320.setEntity0320_string01("ENTITY0320_STRING01_UPDATED");
                        findEntity0320.setEntity0320_string03("ENTITY0320_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0320.setLockMode(LockModeType.NONE);
                        findEntity0320 = null;
                        findEntity0320 = (Entity0320) selectEntity0320.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0320);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0320);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0320));
                        Assert.assertEquals("Assert for the entity id1", findEntity0320.getEntity0320_id1(), (byte) 20);
                        Assert.assertEquals("Assert for the entity id2", findEntity0320.getEntity0320_id2(), '1');
                        Assert.assertEquals("Assert for the entity id3", findEntity0320.getEntity0320_id3(), "ENTITY0320_ID3");
                        Assert.assertEquals("Assert for the entity fields", findEntity0320.getEntity0320_string01(), "ENTITY0320_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0320.getEntity0320_string02(), "ENTITY0320_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0320.getEntity0320_string03(), "ENTITY0320_STRING03_UPDATED");
                        break;

                    case Entity0321:
                        Query selectEntity0321 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0321 e WHERE e.entity0321_id1 = :id1_0321 AND e.entity0321_id2 = :id2_0321 AND e.entity0321_id3 = :id3_0321");
                        selectEntity0321.setParameter("id1_0321", (byte) 21);
                        selectEntity0321.setParameter("id2_0321", '2');
                        selectEntity0321.setParameter("id3_0321", 0323.0323D);
                        Entity0321 findEntity0321 = (Entity0321) selectEntity0321.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0321);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0321);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0321));
                        Assert.assertEquals("Assert for the entity id1", (byte) findEntity0321.getEntity0321_id1(), (byte) 21);
                        Assert.assertEquals("Assert for the entity id2", findEntity0321.getEntity0321_id2(), new Character('2'));
                        Assert.assertEquals("Assert for the entity id3", findEntity0321.getEntity0321_id3(), 0323.0323D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0321.getEntity0321_string01(), "ENTITY0321_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0321.getEntity0321_string02(), "ENTITY0321_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0321.getEntity0321_string03(), "ENTITY0321_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0321.setEntity0321_string01("ENTITY0321_STRING01_UPDATED");
                        findEntity0321.setEntity0321_string03("ENTITY0321_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0321.setLockMode(LockModeType.NONE);
                        findEntity0321 = null;
                        findEntity0321 = (Entity0321) selectEntity0321.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0321);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0321);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0321));
                        Assert.assertEquals("Assert for the entity id1", (byte) findEntity0321.getEntity0321_id1(), (byte) 21);
                        Assert.assertEquals("Assert for the entity id2", findEntity0321.getEntity0321_id2(), new Character('2'));
                        Assert.assertEquals("Assert for the entity id3", findEntity0321.getEntity0321_id3(), 0323.0323D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0321.getEntity0321_string01(), "ENTITY0321_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0321.getEntity0321_string02(), "ENTITY0321_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0321.getEntity0321_string03(), "ENTITY0321_STRING03_UPDATED");
                        break;

                    case Entity0322:
                        Query selectEntity0322 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0322 e WHERE e.entity0322_id1 = :id1_0322 AND e.entity0322_id2 = :id2_0322 AND e.entity0322_id3 = :id3_0322");
                        selectEntity0322.setParameter("id1_0322", '2');
                        selectEntity0322.setParameter("id2_0322", "ENTITY0322_ID2");
                        selectEntity0322.setParameter("id3_0322", 0323.0323D);
                        Entity0322 findEntity0322 = (Entity0322) selectEntity0322.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0322);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0322);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0322));
                        Assert.assertEquals("Assert for the entity id1", findEntity0322.getEntity0322_id1(), '2');
                        Assert.assertEquals("Assert for the entity id2", findEntity0322.getEntity0322_id2(), "ENTITY0322_ID2");
                        Assert.assertEquals("Assert for the entity id3", findEntity0322.getEntity0322_id3(), 0323.0323D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0322.getEntity0322_string01(), "ENTITY0322_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0322.getEntity0322_string02(), "ENTITY0322_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0322.getEntity0322_string03(), "ENTITY0322_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0322.setEntity0322_string01("ENTITY0322_STRING01_UPDATED");
                        findEntity0322.setEntity0322_string03("ENTITY0322_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0322.setLockMode(LockModeType.NONE);
                        findEntity0322 = null;
                        findEntity0322 = (Entity0322) selectEntity0322.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0322);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0322);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0322));
                        Assert.assertEquals("Assert for the entity id1", findEntity0322.getEntity0322_id1(), '2');
                        Assert.assertEquals("Assert for the entity id2", findEntity0322.getEntity0322_id2(), "ENTITY0322_ID2");
                        Assert.assertEquals("Assert for the entity id3", findEntity0322.getEntity0322_id3(), 0323.0323D, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0322.getEntity0322_string01(), "ENTITY0322_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0322.getEntity0322_string02(), "ENTITY0322_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0322.getEntity0322_string03(), "ENTITY0322_STRING03_UPDATED");
                        break;

                    case Entity0323:
                        Query selectEntity0323 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0323 e WHERE e.entity0323_id1 = :id1_0323 AND e.entity0323_id2 = :id2_0323 AND e.entity0323_id3 = :id3_0323");
                        selectEntity0323.setParameter("id1_0323", '3');
                        selectEntity0323.setParameter("id2_0323", 0324.0324D);
                        selectEntity0323.setParameter("id3_0323", 0325.0325F);
                        Entity0323 findEntity0323 = (Entity0323) selectEntity0323.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0323);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0323);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0323));
                        Assert.assertEquals("Assert for the entity id1", findEntity0323.getEntity0323_id1(), new Character('3'));
                        Assert.assertEquals("Assert for the entity id2", findEntity0323.getEntity0323_id2(), 0324.0324D, 0.1);
                        Assert.assertEquals("Assert for the entity id3", findEntity0323.getEntity0323_id3(), 0325.0325F, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0323.getEntity0323_string01(), "ENTITY0323_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0323.getEntity0323_string02(), "ENTITY0323_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0323.getEntity0323_string03(), "ENTITY0323_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0323.setEntity0323_string01("ENTITY0323_STRING01_UPDATED");
                        findEntity0323.setEntity0323_string03("ENTITY0323_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0323.setLockMode(LockModeType.NONE);
                        findEntity0323 = null;
                        findEntity0323 = (Entity0323) selectEntity0323.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0323);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0323);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0323));
                        Assert.assertEquals("Assert for the entity id1", findEntity0323.getEntity0323_id1(), new Character('3'));
                        Assert.assertEquals("Assert for the entity id2", findEntity0323.getEntity0323_id2(), 0324.0324D, 0.1);
                        Assert.assertEquals("Assert for the entity id3", findEntity0323.getEntity0323_id3(), 0325.0325F, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0323.getEntity0323_string01(), "ENTITY0323_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0323.getEntity0323_string02(), "ENTITY0323_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0323.getEntity0323_string03(), "ENTITY0323_STRING03_UPDATED");
                        break;

                    case Entity0324:
                        Query selectEntity0324 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0324 e WHERE e.entity0324_id1 = :id1_0324 AND e.entity0324_id2 = :id2_0324 AND e.entity0324_id3 = :id3_0324");
                        selectEntity0324.setParameter("id1_0324", "ENTITY0324_ID1");
                        selectEntity0324.setParameter("id2_0324", 0324.0324D);
                        selectEntity0324.setParameter("id3_0324", 0325.0325F);
                        Entity0324 findEntity0324 = (Entity0324) selectEntity0324.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0324);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0324);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0324));
                        Assert.assertEquals("Assert for the entity id1", findEntity0324.getEntity0324_id1(), "ENTITY0324_ID1");
                        Assert.assertEquals("Assert for the entity id2", findEntity0324.getEntity0324_id2(), 0324.0324D, 0.1);
                        Assert.assertEquals("Assert for the entity id3", findEntity0324.getEntity0324_id3(), 0325.0325F, 0.1);
                        Assert.assertEquals("Assert for the entity fields", findEntity0324.getEntity0324_string01(), "ENTITY0324_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0324.getEntity0324_string02(), "ENTITY0324_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0324.getEntity0324_string03(), "ENTITY0324_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0324.setEntity0324_string01("ENTITY0324_STRING01_UPDATED");
                        findEntity0324.setEntity0324_string03("ENTITY0324_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0324.setLockMode(LockModeType.NONE);
                        //d642065                  findEntity0324 = null;
//                          findEntity0324 = (Entity0324) selectEntity0324.getSingleResult();
//                          System.out.println("Object returned by find: " + findEntity0324);
//                          Assert.assertNotNull("Assert that the find operation did not return null", findEntity0324);
//
//                          System.out.println     ( "Perform parent verifications...");
//                          Assert.assertTrue  ( "Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0324));
//                          Assert.assertEquals( "Assert for the entity id1",    findEntity0324.getEntity0324_id1(), "ENTITY0324_ID1");
//                          Assert.assertEquals    ( "Assert for the entity id2",    findEntity0324.getEntity0324_id2(), 0324.0324D);
//                          Assert.assertEquals    ( "Assert for the entity id3",    findEntity0324.getEntity0324_id3(), 0325.0325F);
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0324.getEntity0324_string01(), "ENTITY0324_STRING01_UPDATED");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0324.getEntity0324_string02(), "ENTITY0324_STRING02");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0324.getEntity0324_string03(), "ENTITY0324_STRING03_UPDATED");
                        break;

                    case Entity0325:
                        Query selectEntity0325 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0325 e WHERE e.entity0325_id1 = :id1_0325 AND e.entity0325_id2 = :id2_0325 AND e.entity0325_id3 = :id3_0325");
                        selectEntity0325.setParameter("id1_0325", 0325.0325D);
                        selectEntity0325.setParameter("id2_0325", 0326.0326F);
                        selectEntity0325.setParameter("id3_0325", 327);
                        Entity0325 findEntity0325 = (Entity0325) selectEntity0325.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0325);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0325);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0325));
                        Assert.assertEquals("Assert for the entity id1", findEntity0325.getEntity0325_id1(), 0325.0325D, 0.1);
                        Assert.assertEquals("Assert for the entity id2", findEntity0325.getEntity0325_id2(), 0326.0326F, 0.1);
                        Assert.assertEquals("Assert for the entity id3", findEntity0325.getEntity0325_id3(), 327);
                        Assert.assertEquals("Assert for the entity fields", findEntity0325.getEntity0325_string01(), "ENTITY0325_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0325.getEntity0325_string02(), "ENTITY0325_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0325.getEntity0325_string03(), "ENTITY0325_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0325.setEntity0325_string01("ENTITY0325_STRING01_UPDATED");
                        findEntity0325.setEntity0325_string03("ENTITY0325_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0325.setLockMode(LockModeType.NONE);
                        //d642065                  findEntity0325 = null;
//                          findEntity0307 = (Entity0307) selectEntity0307.getSingleResult();
//                          System.out.println("Object returned by find: " + findEntity0325);
//                          Assert.assertNotNull("Assert that the find operation did not return null", findEntity0325);
//
//                          System.out.println     ( "Perform parent verifications...");
//                          Assert.assertTrue  ( "Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0325));
//                          Assert.assertEquals    ( "Assert for the entity id1",    findEntity0325.getEntity0325_id1(), 0325.0325D);
//                          Assert.assertEquals    ( "Assert for the entity id2",    findEntity0325.getEntity0325_id2(), 0326.0326F);
//                          Assert.assertEquals    ( "Assert for the entity id3",    findEntity0325.getEntity0325_id3(), 327);
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0325.getEntity0325_string01(), "ENTITY0325_STRING01_UPDATED");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0325.getEntity0325_string02(), "ENTITY0325_STRING02");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0325.getEntity0325_string03(), "ENTITY0325_STRING03_UPDATED");
                        break;

                    case Entity0326:
                        Query selectEntity0326 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0326 e WHERE e.entity0326_id1 = :id1_0326 AND e.entity0326_id2 = :id2_0326 AND e.entity0326_id3 = :id3_0326");
                        selectEntity0326.setParameter("id1_0326", 0326.0326D);
                        selectEntity0326.setParameter("id2_0326", 0327.0327F);
                        selectEntity0326.setParameter("id3_0326", 328);
                        Entity0326 findEntity0326 = (Entity0326) selectEntity0326.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0326);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0326);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0326));
                        Assert.assertEquals("Assert for the entity id1", findEntity0326.getEntity0326_id1(), 0326.0326D, 0.1);
                        Assert.assertEquals("Assert for the entity id2", findEntity0326.getEntity0326_id2(), 0327.0327F, 0.1);
                        Assert.assertEquals("Assert for the entity id3", (int) findEntity0326.getEntity0326_id3(), 328);
                        Assert.assertEquals("Assert for the entity fields", findEntity0326.getEntity0326_string01(), "ENTITY0326_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0326.getEntity0326_string02(), "ENTITY0326_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0326.getEntity0326_string03(), "ENTITY0326_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0326.setEntity0326_string01("ENTITY0326_STRING01_UPDATED");
                        findEntity0326.setEntity0326_string03("ENTITY0326_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0326.setLockMode(LockModeType.NONE);
                        //d642065                  findEntity0326 = null;
//                          findEntity0326 = (Entity0326) selectEntity0326.getSingleResult();
//                          System.out.println("Object returned by find: " + findEntity0326);
//                          Assert.assertNotNull("Assert that the find operation did not return null", findEntity0326);
//
//                          System.out.println     ( "Perform parent verifications...");
//                          Assert.assertTrue  ( "Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0326));
//                          Assert.assertEquals    ( "Assert for the entity id1",    findEntity0326.getEntity0326_id1(), 0326.0326D);
//                          Assert.assertEquals    ( "Assert for the entity id2",    findEntity0326.getEntity0326_id2(), 0327.0327F);
//                          Assert.assertEquals    ( "Assert for the entity id3",    findEntity0326.getEntity0326_id3(), 328);
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0326.getEntity0326_string01(), "ENTITY0326_STRING01_UPDATED");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0326.getEntity0326_string02(), "ENTITY0326_STRING02");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0326.getEntity0326_string03(), "ENTITY0326_STRING03_UPDATED");
                        break;

                    case Entity0327:
                        Query selectEntity0327 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0327 e WHERE e.entity0327_id1 = :id1_0327 AND e.entity0327_id2 = :id2_0327 AND e.entity0327_id3 = :id3_0327");
                        selectEntity0327.setParameter("id1_0327", 0327.0327F);
                        selectEntity0327.setParameter("id2_0327", 328);
                        selectEntity0327.setParameter("id3_0327", 329L);
                        Entity0327 findEntity0327 = (Entity0327) selectEntity0327.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0327);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0327);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0327));
                        Assert.assertEquals("Assert for the entity id1", findEntity0327.getEntity0327_id1(), 0327.0327F, 0.1);
                        Assert.assertEquals("Assert for the entity id2", findEntity0327.getEntity0327_id2(), 328);
                        Assert.assertEquals("Assert for the entity id3", findEntity0327.getEntity0327_id3(), 329L);
                        Assert.assertEquals("Assert for the entity fields", findEntity0327.getEntity0327_string01(), "ENTITY0327_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0327.getEntity0327_string02(), "ENTITY0327_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0327.getEntity0327_string03(), "ENTITY0327_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0327.setEntity0327_string01("ENTITY0327_STRING01_UPDATED");
                        findEntity0327.setEntity0327_string03("ENTITY0327_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0327.setLockMode(LockModeType.NONE);
                        //d642065                  findEntity0327 = null;
//                          findEntity0327 = (Entity0327) selectEntity0327.getSingleResult();
//                          System.out.println("Object returned by find: " + findEntity0327);
//                          Assert.assertNotNull("Assert that the find operation did not return null", findEntity0327);
//
//                          System.out.println     ( "Perform parent verifications...");
//                          Assert.assertTrue  ( "Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0327));
//                          Assert.assertEquals    ( "Assert for the entity id1",    findEntity0327.getEntity0327_id1(), 0327.0327F);
//                          Assert.assertEquals    ( "Assert for the entity id2",    findEntity0327.getEntity0327_id2(), 328);
//                          Assert.assertEquals    ( "Assert for the entity id3",    findEntity0327.getEntity0327_id3(), 329L);
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0327.getEntity0327_string01(), "ENTITY0327_STRING01_UPDATED");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0327.getEntity0327_string02(), "ENTITY0327_STRING02");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0327.getEntity0327_string03(), "ENTITY0327_STRING03_UPDATED");
                        break;

                    case Entity0328:
                        Query selectEntity0328 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0328 e WHERE e.entity0328_id1 = :id1_0328 AND e.entity0328_id2 = :id2_0328 AND e.entity0328_id3 = :id3_0328");
                        selectEntity0328.setParameter("id1_0328", 0328.0328F);
                        selectEntity0328.setParameter("id2_0328", 329);
                        selectEntity0328.setParameter("id3_0328", 330L);
                        Entity0328 findEntity0328 = (Entity0328) selectEntity0328.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0328);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0328);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0328));
                        Assert.assertEquals("Assert for the entity id1", findEntity0328.getEntity0328_id1(), 0328.0328F, 0.1);
                        Assert.assertEquals("Assert for the entity id2", (int) findEntity0328.getEntity0328_id2(), 329);
                        Assert.assertEquals("Assert for the entity id3", (long) findEntity0328.getEntity0328_id3(), 330L);
                        Assert.assertEquals("Assert for the entity fields", findEntity0328.getEntity0328_string01(), "ENTITY0328_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0328.getEntity0328_string02(), "ENTITY0328_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0328.getEntity0328_string03(), "ENTITY0328_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0328.setEntity0328_string01("ENTITY0328_STRING01_UPDATED");
                        findEntity0328.setEntity0328_string03("ENTITY0328_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0328.setLockMode(LockModeType.NONE);
                        //d642065                  findEntity0328 = null;
//                          findEntity0328 = (Entity0328) selectEntity0328.getSingleResult();
//                          System.out.println("Object returned by find: " + findEntity0328);
//                          Assert.assertNotNull("Assert that the find operation did not return null", findEntity0328);
//
//                          System.out.println     ( "Perform parent verifications...");
//                          Assert.assertTrue  ( "Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0328));
//                          Assert.assertEquals    ( "Assert for the entity id1",    findEntity0328.getEntity0328_id1(), 0328.0328F);
//                          Assert.assertEquals    ( "Assert for the entity id2",    findEntity0328.getEntity0328_id2(), 329);
//                          Assert.assertEquals    ( "Assert for the entity id3",    findEntity0328.getEntity0328_id3(), 330L);
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0328.getEntity0328_string01(), "ENTITY0328_STRING01_UPDATED");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0328.getEntity0328_string02(), "ENTITY0328_STRING02");
//                          Assert.assertEquals( "Assert for the entity fields", findEntity0328.getEntity0328_string03(), "ENTITY0328_STRING03_UPDATED");
                        break;

                    case Entity0329:
                        Query selectEntity0329 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0329 e WHERE e.entity0329_id1 = :id1_0329 AND e.entity0329_id2 = :id2_0329 AND e.entity0329_id3 = :id3_0329");
                        selectEntity0329.setParameter("id1_0329", 329);
                        selectEntity0329.setParameter("id2_0329", 330L);
                        selectEntity0329.setParameter("id3_0329", (short) 331);
                        Entity0329 findEntity0329 = (Entity0329) selectEntity0329.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0329);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0329);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0329));
                        Assert.assertEquals("Assert for the entity id1", findEntity0329.getEntity0329_id1(), 329);
                        Assert.assertEquals("Assert for the entity id2", findEntity0329.getEntity0329_id2(), 330L);
                        Assert.assertEquals("Assert for the entity id3", findEntity0329.getEntity0329_id3(), (short) 331);
                        Assert.assertEquals("Assert for the entity fields", findEntity0329.getEntity0329_string01(), "ENTITY0329_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0329.getEntity0329_string02(), "ENTITY0329_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0329.getEntity0329_string03(), "ENTITY0329_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0329.setEntity0329_string01("ENTITY0329_STRING01_UPDATED");
                        findEntity0329.setEntity0329_string03("ENTITY0329_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0329.setLockMode(LockModeType.NONE);
                        findEntity0329 = null;
                        findEntity0329 = (Entity0329) selectEntity0329.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0329);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0329);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0329));
                        Assert.assertEquals("Assert for the entity id1", findEntity0329.getEntity0329_id1(), 329);
                        Assert.assertEquals("Assert for the entity id2", findEntity0329.getEntity0329_id2(), 330L);
                        Assert.assertEquals("Assert for the entity id3", findEntity0329.getEntity0329_id3(), (short) 331);
                        Assert.assertEquals("Assert for the entity fields", findEntity0329.getEntity0329_string01(), "ENTITY0329_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0329.getEntity0329_string02(), "ENTITY0329_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0329.getEntity0329_string03(), "ENTITY0329_STRING03_UPDATED");
                        break;

                    case Entity0330:
                        Query selectEntity0330 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0330 e WHERE e.entity0330_id1 = :id1_0330 AND e.entity0330_id2 = :id2_0330 AND e.entity0330_id3 = :id3_0330");
                        selectEntity0330.setParameter("id1_0330", 330);
                        selectEntity0330.setParameter("id2_0330", 331L);
                        selectEntity0330.setParameter("id3_0330", (short) 332);
                        Entity0330 findEntity0330 = (Entity0330) selectEntity0330.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0330);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0330);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0330));
                        Assert.assertEquals("Assert for the entity id1", (int) findEntity0330.getEntity0330_id1(), 330);
                        Assert.assertEquals("Assert for the entity id2", (long) findEntity0330.getEntity0330_id2(), 331L);
                        Assert.assertEquals("Assert for the entity id3", (short) findEntity0330.getEntity0330_id3(), (short) 332);
                        Assert.assertEquals("Assert for the entity fields", findEntity0330.getEntity0330_string01(), "ENTITY0330_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0330.getEntity0330_string02(), "ENTITY0330_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0330.getEntity0330_string03(), "ENTITY0330_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0330.setEntity0330_string01("ENTITY0330_STRING01_UPDATED");
                        findEntity0330.setEntity0330_string03("ENTITY0330_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0330.setLockMode(LockModeType.NONE);
                        findEntity0330 = null;
                        findEntity0330 = (Entity0330) selectEntity0330.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0330);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0330);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0330));
                        Assert.assertEquals("Assert for the entity id1", (int) findEntity0330.getEntity0330_id1(), 330);
                        Assert.assertEquals("Assert for the entity id2", (long) findEntity0330.getEntity0330_id2(), 331L);
                        Assert.assertEquals("Assert for the entity id3", (short) findEntity0330.getEntity0330_id3(), (short) 332);
                        Assert.assertEquals("Assert for the entity fields", findEntity0330.getEntity0330_string01(), "ENTITY0330_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0330.getEntity0330_string02(), "ENTITY0330_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0330.getEntity0330_string03(), "ENTITY0330_STRING03_UPDATED");
                        break;

                    case Entity0331:
                        Query selectEntity0331 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0331 e WHERE e.entity0331_id1 = :id1_0331 AND e.entity0331_id2 = :id2_0331 AND e.entity0331_id3 = :id3_0331");
                        selectEntity0331.setParameter("id1_0331", 331L);
                        selectEntity0331.setParameter("id2_0331", (short) 332);
                        selectEntity0331.setParameter("id3_0331", new BigDecimal("0333.033333"));
                        Entity0331 findEntity0331 = (Entity0331) selectEntity0331.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0331);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0331);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0331));
                        Assert.assertEquals("Assert for the entity id1", findEntity0331.getEntity0331_id1(), 331L);
                        Assert.assertEquals("Assert for the entity id2", findEntity0331.getEntity0331_id2(), (short) 332);
                        Assert.assertTrue("Assert for the entity id3", findEntity0331.getEntity0331_id3().compareTo(new BigDecimal("0333.033333")) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0331.getEntity0331_string01(), "ENTITY0331_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0331.getEntity0331_string02(), "ENTITY0331_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0331.getEntity0331_string03(), "ENTITY0331_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0331.setEntity0331_string01("ENTITY0331_STRING01_UPDATED");
                        findEntity0331.setEntity0331_string03("ENTITY0331_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0331.setLockMode(LockModeType.NONE);
                        findEntity0331 = null;
                        findEntity0331 = (Entity0331) selectEntity0331.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0331);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0331);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0331));
                        Assert.assertEquals("Assert for the entity id1", findEntity0331.getEntity0331_id1(), 331L);
                        Assert.assertEquals("Assert for the entity id2", findEntity0331.getEntity0331_id2(), (short) 332);
                        Assert.assertTrue("Assert for the entity id3", findEntity0331.getEntity0331_id3().compareTo(new BigDecimal("0333.033333")) == 0);
                        Assert.assertEquals("Assert for the entity fields", findEntity0331.getEntity0331_string01(), "ENTITY0331_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0331.getEntity0331_string02(), "ENTITY0331_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0331.getEntity0331_string03(), "ENTITY0331_STRING03_UPDATED");
                        break;

                    case Entity0332:
                        Query selectEntity0332 = jpaResource.getEm()
                                        .createQuery("SELECT e FROM Entity0332 e WHERE e.entity0332_id1 = :id1_0332 AND e.entity0332_id2 = :id2_0332 AND e.entity0332_id3 = :id3_0332");
                        selectEntity0332.setParameter("id1_0332", 332L);
                        selectEntity0332.setParameter("id2_0332", (short) 333);
                        selectEntity0332.setParameter("id3_0332", new BigInteger("03340334"));
                        Entity0332 findEntity0332 = (Entity0332) selectEntity0332.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0332);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0332);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0332));
                        Assert.assertEquals("Assert for the entity id1", (long) findEntity0332.getEntity0332_id1(), 332L);
                        Assert.assertEquals("Assert for the entity id2", (short) findEntity0332.getEntity0332_id2(), (short) 333);
                        Assert.assertEquals("Assert for the entity id3", findEntity0332.getEntity0332_id3(), new BigInteger("03340334"));
                        Assert.assertEquals("Assert for the entity fields", findEntity0332.getEntity0332_string01(), "ENTITY0332_STRING01");
                        Assert.assertEquals("Assert for the entity fields", findEntity0332.getEntity0332_string02(), "ENTITY0332_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0332.getEntity0332_string03(), "ENTITY0332_STRING03");
                        //
                        // Update, commit, verify
                        //
                        findEntity0332.setEntity0332_string01("ENTITY0332_STRING01_UPDATED");
                        findEntity0332.setEntity0332_string03("ENTITY0332_STRING03_UPDATED");
                        System.out.println("Committing transaction for: " + entity);
                        jpaResource.getTj().commitTransaction();
                        selectEntity0332.setLockMode(LockModeType.NONE);
                        findEntity0332 = null;
                        findEntity0332 = (Entity0332) selectEntity0332.getSingleResult();
                        System.out.println("Object returned by find: " + findEntity0332);
                        Assert.assertNotNull("Assert that the find operation did not return null", findEntity0332);

                        System.out.println("Perform parent verifications...");
                        Assert.assertTrue("Assert entity returned by find is managed by the persistence context.", jpaResource.getEm().contains(findEntity0332));
                        Assert.assertEquals("Assert for the entity id1", (long) findEntity0332.getEntity0332_id1(), 332L);
                        Assert.assertEquals("Assert for the entity id2", (short) findEntity0332.getEntity0332_id2(), (short) 333);
                        Assert.assertEquals("Assert for the entity id3", findEntity0332.getEntity0332_id3(), new BigInteger("03340334"));
                        Assert.assertEquals("Assert for the entity fields", findEntity0332.getEntity0332_string01(), "ENTITY0332_STRING01_UPDATED");
                        Assert.assertEquals("Assert for the entity fields", findEntity0332.getEntity0332_string02(), "ENTITY0332_STRING02");
                        Assert.assertEquals("Assert for the entity fields", findEntity0332.getEntity0332_string03(), "ENTITY0332_STRING03_UPDATED");
                        break;

                    default:
                        Assert.fail("Invalid entity type specified ('" + entity + "').  Cannot execute the test.");
                        return;
                }
            }
//            }
            System.out.println("Ending test.");
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            // Catch any Exceptions thrown by the test case for proper error logging.
            t.printStackTrace();
            Assert.fail("Caught an unexpected Exception during test execution." + t);
        } finally {
            System.out.println("QueryLockModeTestLogic.testScenario01(): End");
        }
    }
}
