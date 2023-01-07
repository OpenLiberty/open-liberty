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

package com.ibm.ws.jpa.fat.dsoverride.web;

import static com.ibm.websphere.simplicity.config.DataSourceProperties.DATADIRECT_SQLSERVER;
import static com.ibm.websphere.simplicity.config.DataSourceProperties.DB2_I_NATIVE;
import static com.ibm.websphere.simplicity.config.DataSourceProperties.DB2_I_TOOLBOX;
import static com.ibm.websphere.simplicity.config.DataSourceProperties.DB2_JCC;
import static com.ibm.websphere.simplicity.config.DataSourceProperties.INFORMIX_JCC;
import static com.ibm.websphere.simplicity.config.DataSourceProperties.INFORMIX_JDBC;
import static com.ibm.websphere.simplicity.config.DataSourceProperties.MICROSOFT_SQLSERVER;
import static com.ibm.websphere.simplicity.config.DataSourceProperties.ORACLE_JDBC;
import static com.ibm.websphere.simplicity.config.DataSourceProperties.ORACLE_UCP;
import static com.ibm.websphere.simplicity.config.DataSourceProperties.SYBASE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceUnit;
import javax.persistence.Query;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.simplicity.config.dsprops.testrules.SkipIfDataSourceProperties;
import com.ibm.ws.jpa.fat.dsoverride.entity.DSOverrideEntity;

import componenttest.app.FATServlet;

@SuppressWarnings({ "serial", "unused" })
public class DSOverrideTestServlet extends FATServlet {
    private final static String qStr = "SELECT d FROM DSOverrideEntity d WHERE d.id = ?1";

    @PersistenceUnit(unitName = "DSOverride")
    private EntityManagerFactory emf;

    @PersistenceUnit(unitName = "DSOverride_pointsToAltDB")
    private EntityManagerFactory emfAlt;

    @PersistenceUnit(unitName = "DSOverride_ECL_AUDIT")
    private EntityManagerFactory emfAudit;

    @Resource
    private UserTransaction tx;

    @Resource(name = "jdbc/AltJTADataSource")
    private DataSource altJTADataSource;

    private static final AtomicLong atomicID = new AtomicLong(0);

    private static final boolean isUsingJavaxPersistence;

    static {
        boolean usingJavaEE = false;
        try {
            Class.forName("jakarta.persistence.EntityManager");
        } catch (Throwable e) {
            usingJavaEE = true;
        }
        isUsingJavaxPersistence = usingJavaEE;
    }

    /**
     * Create the table structures needed for the test. It's noted that when overriding the datasource,
     * Eclipselink does not detect missing tables and autocreate them.
     */
    @Override
    public void init() {
        final long id = atomicID.incrementAndGet();

        try {
            populateRow(emf, null, id, "data");
            populateRow(emfAlt, null, id, "lore");
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private String getJTADatasourcePropertyKeyName() {
        return (isUsingJavaxPersistence) ? "javax.persistence.jtaDataSource" : "jakarta.persistence.jtaDataSource";
    }

    private String getNonJTADatasourcePropertyKeyName() {
        return (isUsingJavaxPersistence) ? "javax.persistence.nonJtaDataSource" : "jakarta.persistence.nonJtaDataSource";
    }

    /**
     * Verify that setting "javax.persistence.JtaDataSource" without ECL's exclusive connection properties
     * just doesn't work.
     *
     * @throws Throwable
     */
    @Test
    @SkipIfDataSourceProperties({ DB2_I_NATIVE, DB2_I_TOOLBOX, DB2_JCC, INFORMIX_JCC, INFORMIX_JDBC, SYBASE, ORACLE_JDBC, ORACLE_UCP, DATADIRECT_SQLSERVER, MICROSOFT_SQLSERVER })
    public void testDSOverride001() throws Throwable {
        final long id = atomicID.incrementAndGet();
        System.out.println("Start executing DSOverrideTestServlet.testDSOverride001");

        try {
            // Create a new Entity using the persistence unit's default datasource
            DSOverrideEntity newEntity = populateRow(emf, null, id, "data");

            // Verify that the entity exists in the database
            DSOverrideEntity findNormal = validateRowExistanceByFind(emf, null, id, "data", null, null, true);

            // Search for the new Entity using an alternate datasource addressing a different database
            // Without the appropriate extra eclipselink properties, the DSOverride should not work
            // -- that is, should find the new row.
            Map<String, Object> map = new HashMap<String, Object>();
            map.put(getJTADatasourcePropertyKeyName(), altJTADataSource);
            validateRowExistanceByFind(emf, map, id, "data", null, null, true);

            // Test again with an EM without the override properties, to ensure that the EMF has not been unexpectedly altered.
            DSOverrideEntity findEntityNormAgain = validateRowExistanceByFind(emf, null, id, "data", Arrays.asList(findNormal), null, true);
        } finally {
            System.out.println("End executing DSOverrideTestServlet.testDSOverride001");
        }
    }

    /**
     * Verify that setting "javax.persistence.JtaDataSource" without ECL's exclusive connection properties
     * just doesn't work. Variant testing with JPQL Query.
     *
     * @throws Throwable
     */
    @Test
    @SkipIfDataSourceProperties({ DB2_I_NATIVE, DB2_I_TOOLBOX, DB2_JCC, INFORMIX_JCC, INFORMIX_JDBC, SYBASE, ORACLE_JDBC, ORACLE_UCP, DATADIRECT_SQLSERVER, MICROSOFT_SQLSERVER })
    public void testDSOverride002() throws Throwable {
        final long id = atomicID.incrementAndGet();
        System.out.println("Start executing DSOverrideTestServlet.testDSOverride002");

        try {
            // Create a new Entity using the persistence unit's default datasource
            DSOverrideEntity newEntity = populateRow(emf, null, id, "data");

            // Verify that the entity exists in the database
            DSOverrideEntity findNormal = validateRowExistanceByFind(emf, null, id, "data", null, null, true);

            // Search for the new Entity using an alternate datasource addressing a different database
            // Without the appropriate extra eclipselink properties, the DSOverride should not work
            // -- that is, should find the new row.
            Map<String, Object> map = new HashMap<String, Object>();
            map.put(getJTADatasourcePropertyKeyName(), altJTADataSource);

            validateRowExistanceByQuery(emf, map, id, "data", null, null, null, true);

            // Test again with an EM without the override properties, to ensure that the EMF has not been unexpectedly altered.
            validateRowExistanceByQuery(emf, null, id, "data", null, null, null, true);
        } finally {
            System.out.println("End executing DSOverrideTestServlet.testDSOverride002");
        }
    }

    /**
     * Verify that setting "javax.persistence.JtaDataSource" with ECL's exclusive connection properties
     * with a DataSource instance works. Variant: Performing the find on the DSOverride EMF without a JTA tran.
     *
     * @throws Throwable
     */
    @Test
    @SkipIfDataSourceProperties({ DB2_I_NATIVE, DB2_I_TOOLBOX, DB2_JCC, INFORMIX_JCC, INFORMIX_JDBC, SYBASE, ORACLE_JDBC, ORACLE_UCP, DATADIRECT_SQLSERVER, MICROSOFT_SQLSERVER })
    public void testDSOverride003() throws Throwable {
        final long id = atomicID.incrementAndGet();
        System.out.println("Start executing DSOverrideTestServlet.testDSOverride003");

        try {
            // Create a new Entity using the persistence unit's default datasource
            DSOverrideEntity newEntity = populateRow(emf, null, id, "data");

            // Verify that the entity exists in the database
            DSOverrideEntity findNormal = validateRowExistanceByFind(emf, null, id, "data", null, null, true);

            // Search for the new Entity using an alternate datasource addressing a different database
            // With the appropriate extra eclipselink properties, the DSOverride should work
            // -- that is, should not find the new row.
            Map<String, Object> map = new HashMap<String, Object>();
            map.put(getJTADatasourcePropertyKeyName(), altJTADataSource);
            map.put("eclipselink.jdbc.exclusive-connection.mode", "Always");
            validateRowNonExistanceByFind(emf, map, id, false);

            // Test again with an EM without the override properties, to ensure that the EMF has not been unexpectedly altered.
            DSOverrideEntity findEntityNormAgain = validateRowExistanceByFind(emf, null, id, "data", Arrays.asList(findNormal), null, true);
        } finally {
            System.out.println("End executing DSOverrideTestServlet.testDSOverride003");
        }
    }

    /**
     * Verify that setting "javax.persistence.JtaDataSource" with ECL's exclusive connection properties
     * with a DataSource instance works. Variant: Performing the find on the DSOverride EMF within a JTA tran.
     *
     * @throws Throwable
     */
    @Test
    @SkipIfDataSourceProperties({ DB2_I_NATIVE, DB2_I_TOOLBOX, DB2_JCC, INFORMIX_JCC, INFORMIX_JDBC, SYBASE, ORACLE_JDBC, ORACLE_UCP, DATADIRECT_SQLSERVER, MICROSOFT_SQLSERVER })
    public void testDSOverride004() throws Throwable {
        final long id = atomicID.incrementAndGet();
        System.out.println("Start executing DSOverrideTestServlet.testDSOverride004");

        try {
            // Create a new Entity using the persistence unit's default datasource
            DSOverrideEntity newEntity = populateRow(emf, null, id, "data");

            // Verify that the entity exists in the database
            DSOverrideEntity findNormal = validateRowExistanceByFind(emf, null, id, "data", null, null, true);

            // Search for the new Entity using an alternate datasource addressing a different database
            // With the appropriate extra eclipselink properties, the DSOverride should work
            // -- that is, should not find the new row.
            Map<String, Object> map = new HashMap<String, Object>();
            map.put(getJTADatasourcePropertyKeyName(), altJTADataSource);
            map.put("eclipselink.jdbc.exclusive-connection.mode", "Always");
            validateRowNonExistanceByFind(emf, map, id, true);

            // Test again with an EM without the override properties, to ensure that the EMF has not been unexpectedly altered.
            DSOverrideEntity findEntityNormAgain = validateRowExistanceByFind(emf, null, id, "data", Arrays.asList(findNormal), null, true);
        } finally {
            System.out.println("End executing DSOverrideTestServlet.testDSOverride004");
        }
    }

    /**
     * Verify that setting "javax.persistence.JtaDataSource" with ECL's exclusive connection properties
     * with a DataSource instance works. Variant: Testing with PESSIMISTIC_READ lock.
     *
     * @throws Throwable
     */
    @Test
    @SkipIfDataSourceProperties({ DB2_I_NATIVE, DB2_I_TOOLBOX, DB2_JCC, INFORMIX_JCC, INFORMIX_JDBC, SYBASE, ORACLE_JDBC, ORACLE_UCP, DATADIRECT_SQLSERVER, MICROSOFT_SQLSERVER })
    public void testDSOverride005() throws Throwable {
        final long id = atomicID.incrementAndGet();
        System.out.println("Start executing DSOverrideTestServlet.testDSOverride005");

        EntityManager em = null;
        EntityManager emDSOvrd = null;
        try {
            // Create a new Entity using the persistence unit's default datasource
            DSOverrideEntity newEntity = populateRow(emf, null, id, "data");

            // Verify that the entity exists in the database
            DSOverrideEntity findNormal = validateRowExistanceByFind(emf, null, id, "data", null, null, true);

            // Search for the new Entity using an alternate datasource addressing a different database
            // With the appropriate extra eclipselink properties, the DSOverride should work
            // -- that is, should not find the new row.
            tx.begin();
            Map<String, Object> map = new HashMap<String, Object>();
            map.put(getJTADatasourcePropertyKeyName(), altJTADataSource);
            map.put("eclipselink.jdbc.exclusive-connection.mode", "Always");
            emDSOvrd = emf.createEntityManager(map);
            emDSOvrd.joinTransaction();

            DSOverrideEntity findEntity = emDSOvrd.find(DSOverrideEntity.class, id, LockModeType.PESSIMISTIC_READ);
            System.out.println("Find operation returned: " + findEntity);

            emDSOvrd.close();
            tx.rollback();

            // Find should have returned null.
            assertNull("Should not find the entity on the alternate database.", findEntity);

            // Test again with an EM without the override properties, to ensure that the EMF has not been unexpectedly altered.
            DSOverrideEntity findEntityNormAgain = validateRowExistanceByFind(emf, null, id, "data", Arrays.asList(findNormal), null, true);

            // Test again with an EM without the override properties, to ensure that the EMF has not been unexpectedly altered.  With lock
            tx.begin();
            em = emf.createEntityManager();
            findEntityNormAgain = em.find(DSOverrideEntity.class, id, LockModeType.PESSIMISTIC_READ);
            System.out.println("Find-Normal operation returned: " + findEntityNormAgain);
            tx.rollback();
            em.close();

            // Verify that an entity has been found again
            assertNotNull(findEntityNormAgain);
            assertEquals(id, findEntityNormAgain.getId());
            assertEquals("data", findEntityNormAgain.getStrData());
        } finally {
            System.out.println("End executing DSOverrideTestServlet.testDSOverride005");
        }
    }

    /**
     * Verify that setting "javax.persistence.JtaDataSource" with ECL's exclusive connection properties
     * with a DataSource instance works. Variant: Testing with PESSIMISTIC_READ lock, with both a regular
     * EntityManager and a DSOverride EntityManager engaging the same JTA transaction.
     *
     * @throws Throwable
     */
    @Test
    @SkipIfDataSourceProperties({ DB2_I_NATIVE, DB2_I_TOOLBOX, DB2_JCC, INFORMIX_JCC, INFORMIX_JDBC, SYBASE, ORACLE_JDBC, ORACLE_UCP, DATADIRECT_SQLSERVER, MICROSOFT_SQLSERVER })
    public void testDSOverride006() throws Throwable {
        final long id = atomicID.incrementAndGet();
        System.out.println("Start executing DSOverrideTestServlet.testDSOverride006");

        EntityManager em = null;
        EntityManager emDSOvrd = null;
        try {
            // Create a new Entity using the persistence unit's default datasource
            DSOverrideEntity newEntity = populateRow(emf, null, id, "data");

            // Verify that the entity exists in the database
            DSOverrideEntity findNormal = validateRowExistanceByFind(emf, null, id, "data", null, null, true);

            // Search for the new Entity using an alternate datasource addressing a different database
            // With the appropriate extra eclipselink properties, the DSOverride should work
            // -- that is, should not find the new row.
            tx.begin();
            Map<String, Object> map = new HashMap<String, Object>();
            map.put(getJTADatasourcePropertyKeyName(), altJTADataSource);
            map.put("eclipselink.jdbc.exclusive-connection.mode", "Always");
            emDSOvrd = emf.createEntityManager(map);
            emDSOvrd.joinTransaction();

            DSOverrideEntity findEntity = emDSOvrd.find(DSOverrideEntity.class, id, LockModeType.PESSIMISTIC_READ);
            System.out.println("Find operation returned: " + findEntity);

            tx.rollback();
            emDSOvrd.clear();

            // Find should have returned null.
            assertNull("Should not find the entity on the alternate database.", findEntity);

            // Test again with an EM without the override properties, to ensure that the EMF has not been unexpectedly altered.
            tx.begin();
            em = emf.createEntityManager();
            em.joinTransaction();
            DSOverrideEntity findEntityNormAgain = em.find(DSOverrideEntity.class, id);

            // Verify that an entity has been found again
            assertNotNull(findEntityNormAgain);
            assertEquals(id, findEntityNormAgain.getId());
            assertEquals("data", findEntityNormAgain.getStrData());

            emDSOvrd.joinTransaction();
            DSOverrideEntity findEntityAgain = emDSOvrd.find(DSOverrideEntity.class, id, LockModeType.PESSIMISTIC_READ);
            assertNull("Should not find the entity on the alternate database.", findEntityAgain);

            // Test again with an EM without the override properties, to ensure that the EMF has not been unexpectedly altered.  With lock
            DSOverrideEntity findEntityNormAgain2 = em.find(DSOverrideEntity.class, id, LockModeType.PESSIMISTIC_READ);
            tx.rollback();
            em.close();
            emDSOvrd.close();

            // Verify that an entity has been found again
            assertNotNull(findEntityNormAgain2);
            assertEquals(id, findEntityNormAgain2.getId());
            assertEquals("data", findEntityNormAgain2.getStrData());
            assertSame(findEntityNormAgain, findEntityNormAgain2);
        } finally {
            System.out.println("End executing DSOverrideTestServlet.testDSOverride006");
        }
    }

    /**
     * Verify that setting "javax.persistence.JtaDataSource" with ECL's exclusive connection properties
     * with a DataSource instance works. Variant: Setting the property in the persistence unit definition
     * rather than a Map provided to the createEntityManager op. Performing the find on the DSOverride EMF without a JTA tran.
     *
     * @throws Throwable
     */
    @Test
    @SkipIfDataSourceProperties({ DB2_I_NATIVE, DB2_I_TOOLBOX, DB2_JCC, INFORMIX_JCC, INFORMIX_JDBC, SYBASE, ORACLE_JDBC, ORACLE_UCP, DATADIRECT_SQLSERVER, MICROSOFT_SQLSERVER })
    public void testDSOverride007() throws Throwable {
        final long id = atomicID.incrementAndGet();
        System.out.println("Start executing DSOverrideTestServlet.testDSOverride007");

        try {
            // Create a new Entity using the persistence unit's default datasource
            DSOverrideEntity newEntity = populateRow(emfAudit, null, id, "data");

            // Verify that the entity exists in the database
            DSOverrideEntity findNormal = validateRowExistanceByFind(emfAudit, null, id, "data", null, null, true);

            // Search for the new Entity using an alternate datasource addressing a different database
            // -- should result in a null find result (ie, can't find an entity by that identity)
            Map<String, Object> map = new HashMap<String, Object>();
            map.put(getJTADatasourcePropertyKeyName(), altJTADataSource);
            validateRowNonExistanceByFind(emfAudit, map, id, false);

            // Test again with an EM without the override properties, to ensure that the EMF has not been unexpectedly altered.
            DSOverrideEntity findEntityNormAgain = validateRowExistanceByFind(emf, null, id, "data", Arrays.asList(findNormal), null, true);
        } finally {
            System.out.println("End executing DSOverrideTestServlet.testDSOverride007");
        }
    }

    /**
     * Verify that setting "javax.persistence.JtaDataSource" with ECL's exclusive connection properties
     * with a DataSource instance works. Variant: Setting the property in the persistence unit definition
     * rather than a Map provided to the createEntityManager op. Performing the find on the DSOverride EMF within a JTA tran.
     *
     * @throws Throwable
     */
    @Test
    @SkipIfDataSourceProperties({ DB2_I_NATIVE, DB2_I_TOOLBOX, DB2_JCC, INFORMIX_JCC, INFORMIX_JDBC, SYBASE, ORACLE_JDBC, ORACLE_UCP, DATADIRECT_SQLSERVER, MICROSOFT_SQLSERVER })
    public void testDSOverride008() throws Throwable {
        final long id = atomicID.incrementAndGet();
        System.out.println("Start executing DSOverrideTestServlet.testDSOverride008");

        try {
            // Create a new Entity using the persistence unit's default datasource
            DSOverrideEntity newEntity = populateRow(emfAudit, null, id, "data");

            // Verify that the entity exists in the database
            DSOverrideEntity findNormal = validateRowExistanceByFind(emfAudit, null, id, "data", null, null, true);

            // Search for the new Entity using an alternate datasource addressing a different database
            // -- should result in a null find result (ie, can't find an entity by that identity)
            Map<String, Object> map = new HashMap<String, Object>();
            map.put(getJTADatasourcePropertyKeyName(), altJTADataSource);
            validateRowNonExistanceByFind(emfAudit, map, id, true);

            // Test again with an EM without the override properties, to ensure that the EMF has not been unexpectedly altered.
            DSOverrideEntity findEntityNormAgain = validateRowExistanceByFind(emf, null, id, "data", Arrays.asList(findNormal), null, true);
        } finally {
            System.out.println("End executing DSOverrideTestServlet.testDSOverride008");
        }
    }

    /**
     * Verify that setting "javax.persistence.JtaDataSource" with ECL's exclusive connection properties
     * with a DataSource instance works. Variant: Setting the property in the persistence unit definition
     * rather than a Map provided to the createEntityManager op. Performing the find on the DSOverride EMF
     * within a JTA tran. Using PESSIMISTIC READ locking.
     *
     * @throws Throwable
     */
    @Test
    @SkipIfDataSourceProperties({ DB2_I_NATIVE, DB2_I_TOOLBOX, DB2_JCC, INFORMIX_JCC, INFORMIX_JDBC, SYBASE, ORACLE_JDBC, ORACLE_UCP, DATADIRECT_SQLSERVER, MICROSOFT_SQLSERVER })
    public void testDSOverride009() throws Throwable {
        final long id = atomicID.incrementAndGet();
        System.out.println("Start executing DSOverrideTestServlet.testDSOverride009");

        EntityManager em = null;
        EntityManager emDSOvrd = null;
        try {
            // Create a new Entity using the persistence unit's default datasource
            DSOverrideEntity newEntity = populateRow(emfAudit, null, id, "data");

            // Verify that the entity exists in the database
            DSOverrideEntity findNormal = validateRowExistanceByFind(emfAudit, null, id, "data", null, null, true);

            // Search for the new Entity using an alternate datasource addressing a different database
            // -- should result in a null find result (ie, can't find an entity by that identity)
            tx.begin();
            Map<String, Object> map = new HashMap<String, Object>();
            map.put(getJTADatasourcePropertyKeyName(), altJTADataSource);
            emDSOvrd = emfAudit.createEntityManager(map);
            emDSOvrd.joinTransaction();

            DSOverrideEntity findEntity = emDSOvrd.find(DSOverrideEntity.class, id, LockModeType.PESSIMISTIC_READ);

            emDSOvrd.close();
            tx.rollback();

            // Find should have returned null, because the row was inserted into a table in a
            // different database.
            assertNull("Should not find the entity on the alternate database.", findEntity);

            // Test again with an EM without the override properties, to ensure that the EMF has not been unexpectedly altered.
            DSOverrideEntity findEntityNormAgain = validateRowExistanceByFind(emf, null, id, "data", null, null, true);

            // Test again with an EM without the override properties, to ensure that the EMF has not been unexpectedly altered.  With lock
            tx.begin();
            em = emf.createEntityManager();
            findEntityNormAgain = em.find(DSOverrideEntity.class, id, LockModeType.PESSIMISTIC_READ);
            tx.rollback();
            em.close();

            // Verify that an entity has been found again
            assertNotNull(findEntityNormAgain);
            assertEquals(id, findEntityNormAgain.getId());
            assertEquals("data", findEntityNormAgain.getStrData());
        } finally {
            System.out.println("End executing DSOverrideTestServlet.testDSOverride009");
        }
    }

    /**
     * Verify that setting "javax.persistence.JtaDataSource" with ECL's exclusive connection properties
     * with a DataSource instance works. Dual insertions of same ID on both tables, verifying that no
     * collissions occur. Reads on both EMs performed in and outside of JTA transactions.
     *
     * @throws Throwable
     */
    @Test
    @SkipIfDataSourceProperties({ DB2_I_NATIVE, DB2_I_TOOLBOX, DB2_JCC, INFORMIX_JCC, INFORMIX_JDBC, SYBASE, ORACLE_JDBC, ORACLE_UCP, DATADIRECT_SQLSERVER, MICROSOFT_SQLSERVER })
    public void testDSOverride010() throws Throwable {
        final long id = atomicID.incrementAndGet();
        System.out.println("Start executing DSOverrideTestServlet.testDSOverride010");

        try {
            // Create a new Entity using the persistence unit's default datasource
            tx.begin();

            EntityManager em1 = emf.createEntityManager();
            em1.joinTransaction();

            Map<String, Object> map = new HashMap<String, Object>();
            map.put(getJTADatasourcePropertyKeyName(), altJTADataSource);
            map.put("eclipselink.jdbc.exclusive-connection.mode", "Always");
            EntityManager em2 = emf.createEntityManager(map);
            em2.joinTransaction();

            DSOverrideEntity newEntity1 = new DSOverrideEntity(id, "data-1");
            em1.persist(newEntity1);

            DSOverrideEntity newEntity2 = new DSOverrideEntity(id, "data-2");
            em2.persist(newEntity2);

            tx.commit();

            em1.clear();
            em2.clear();

            // Read JTA Tran
            tx.begin();
            em1.joinTransaction();
            em2.joinTransaction();

            DSOverrideEntity find1 = em1.find(DSOverrideEntity.class, id);
            DSOverrideEntity find2 = em2.find(DSOverrideEntity.class, id);

            assertNotSame(find1, newEntity1);
            assertNotSame(find2, newEntity2);
            assertNotSame(find1, find2);
            assertNotNull(find1);
            assertNotNull(find2);
            assertEquals(find1.getId(), id);
            assertEquals(find2.getId(), id);
            assertEquals(find1.getStrData(), "data-1");
            assertEquals(find2.getStrData(), "data-2");

            tx.rollback();
            em1.clear();
            em2.clear();

            // Read without JTA Tran
            DSOverrideEntity find1a = em1.find(DSOverrideEntity.class, id);
            DSOverrideEntity find2a = em2.find(DSOverrideEntity.class, id);

            assertNotSame(find1a, find1);
            assertNotSame(find2a, find2);
            assertNotSame(find1a, find2a);
            assertNotNull(find1a);
            assertNotNull(find2a);
            assertEquals(find1a.getId(), id);
            assertEquals(find2a.getId(), id);
            assertEquals(find1a.getStrData(), "data-1");
            assertEquals(find2a.getStrData(), "data-2");

            em1.close();
            em2.close();

        } finally {
            System.out.println("End executing DSOverrideTestServlet.testDSOverride010");
        }
    }

    /**
     * Verify that setting "javax.persistence.JtaDataSource" with ECL's exclusive connection properties
     * with a DataSource instance works. Dual insertions of same ID on both tables, verifying that no
     * collissions occur. Reads on both EMs performed in and outside of JTA transactions. Variant
     * using PESSIMISTIC READs.
     *
     * @throws Throwable
     */
    @Test
    @SkipIfDataSourceProperties({ DB2_I_NATIVE, DB2_I_TOOLBOX, DB2_JCC, INFORMIX_JCC, INFORMIX_JDBC, SYBASE, ORACLE_JDBC, ORACLE_UCP, DATADIRECT_SQLSERVER, MICROSOFT_SQLSERVER })
    public void testDSOverride011() throws Throwable {
        final long id = atomicID.incrementAndGet();
        System.out.println("Start executing DSOverrideTestServlet.testDSOverride011");

        try {
            // Create a new Entity using the persistence unit's default datasource
            tx.begin();

            EntityManager em1 = emf.createEntityManager();
            em1.joinTransaction();

            Map<String, Object> map = new HashMap<String, Object>();
            map.put(getJTADatasourcePropertyKeyName(), altJTADataSource);
            map.put("eclipselink.jdbc.exclusive-connection.mode", "Always");
            EntityManager em2 = emf.createEntityManager(map);
            em2.joinTransaction();

            DSOverrideEntity newEntity1 = new DSOverrideEntity(id, "data-1");
            em1.persist(newEntity1);

            DSOverrideEntity newEntity2 = new DSOverrideEntity(id, "data-2");
            em2.persist(newEntity2);

            tx.commit();

            em1.clear();
            em2.clear();

            // Read JTA Tran
            tx.begin();
            em1.joinTransaction();
            em2.joinTransaction();

            DSOverrideEntity find1 = em1.find(DSOverrideEntity.class, id, LockModeType.PESSIMISTIC_READ);
            DSOverrideEntity find2 = em2.find(DSOverrideEntity.class, id, LockModeType.PESSIMISTIC_READ);

            assertNotSame(find1, newEntity1);
            assertNotSame(find2, newEntity2);
            assertNotSame(find1, find2);
            assertNotNull(find1);
            assertNotNull(find2);
            assertEquals(find1.getId(), id);
            assertEquals(find2.getId(), id);
            assertEquals(find1.getStrData(), "data-1");
            assertEquals(find2.getStrData(), "data-2");

            tx.rollback();
            em1.clear();
            em2.clear();

            // Read with JTA Tran
            tx.begin();
            em1.joinTransaction();
            em2.joinTransaction();
            DSOverrideEntity find1a = em1.find(DSOverrideEntity.class, id, LockModeType.PESSIMISTIC_READ);
            DSOverrideEntity find2a = em2.find(DSOverrideEntity.class, id, LockModeType.PESSIMISTIC_READ);

            assertNotSame(find1a, find1);
            assertNotSame(find2a, find2);
            assertNotSame(find1a, find2a);
            assertNotNull(find1a);
            assertNotNull(find2a);
            assertEquals(find1a.getId(), id);
            assertEquals(find2a.getId(), id);
            assertEquals(find1a.getStrData(), "data-1");
            assertEquals(find2a.getStrData(), "data-2");

            tx.rollback();
            em1.close();
            em2.close();

        } finally {
            System.out.println("End executing DSOverrideTestServlet.testDSOverride011");
        }
    }

    /**
     * Verify that setting "javax.persistence.JtaDataSource" with ECL's exclusive connection properties
     * with a DataSource instance works. Dual insertions of same ID on both tables, verifying that no
     * collissions occur. Reads on both EMs performed in and outside of JTA transactions. Variant
     * using PESSIMISTIC READs and JPQL Query.
     *
     * @throws Throwable
     */
    @Test
    @SkipIfDataSourceProperties({ DB2_I_NATIVE, DB2_I_TOOLBOX, DB2_JCC, INFORMIX_JCC, INFORMIX_JDBC, SYBASE, ORACLE_JDBC, ORACLE_UCP, DATADIRECT_SQLSERVER, MICROSOFT_SQLSERVER })
    public void testDSOverride012() throws Throwable {
        final long id = atomicID.incrementAndGet();
        System.out.println("Start executing DSOverrideTestServlet.testDSOverride012");

        try {
            // Create a new Entity using the persistence unit's default datasource
            tx.begin();

            EntityManager em1 = emf.createEntityManager();
            em1.joinTransaction();

            Map<String, Object> map = new HashMap<String, Object>();
            map.put(getJTADatasourcePropertyKeyName(), altJTADataSource);
            map.put("eclipselink.jdbc.exclusive-connection.mode", "Always");
            EntityManager em2 = emf.createEntityManager(map);
            em2.joinTransaction();

            DSOverrideEntity newEntity1 = new DSOverrideEntity(id, "data-1");
            em1.persist(newEntity1);

            DSOverrideEntity newEntity2 = new DSOverrideEntity(id, "data-2");
            em2.persist(newEntity2);

            tx.commit();

            em1.clear();
            em2.clear();

            // Read JTA Tran
            tx.begin();
            em1.joinTransaction();
            em2.joinTransaction();

            DSOverrideEntity find1 = findByQuery(em1, id, LockModeType.PESSIMISTIC_READ, null);
            DSOverrideEntity find2 = findByQuery(em2, id, LockModeType.PESSIMISTIC_READ, null);

            assertNotSame(find1, newEntity1);
            assertNotSame(find2, newEntity2);
            assertNotSame(find1, find2);
            assertNotNull(find1);
            assertNotNull(find2);
            assertEquals(find1.getId(), id);
            assertEquals(find2.getId(), id);
            assertEquals(find1.getStrData(), "data-1");
            assertEquals(find2.getStrData(), "data-2");

            tx.rollback();
            em1.clear();
            em2.clear();

            // Read with JTA Tran
            tx.begin();
            em1.joinTransaction();
            em2.joinTransaction();
            DSOverrideEntity find1a = findByQuery(em1, id, LockModeType.PESSIMISTIC_READ, null);
            DSOverrideEntity find2a = findByQuery(em2, id, LockModeType.PESSIMISTIC_READ, null);

            assertNotSame(find1a, find1);
            assertNotSame(find2a, find2);
            assertNotSame(find1a, find2a);
            assertNotNull(find1a);
            assertNotNull(find2a);
            assertEquals(find1a.getId(), id);
            assertEquals(find2a.getId(), id);
            assertEquals(find1a.getStrData(), "data-1");
            assertEquals(find2a.getStrData(), "data-2");

            tx.rollback();
            em1.close();
            em2.close();

        } finally {
            System.out.println("End executing DSOverrideTestServlet.testDSOverride012");
        }
    }

    /**
     * Verify that setting "javax.persistence.JtaDataSource" with ECL's exclusive connection properties
     * with a DataSource instance works. Dual insertions of same ID on both tables, verifying that no
     * collissions occur. Reads on both EMs performed in and outside of JTA transactions. Variant
     * using PESSIMISTIC WRITE
     *
     * @throws Throwable
     */
    @Test
    @SkipIfDataSourceProperties({ DB2_I_NATIVE, DB2_I_TOOLBOX, DB2_JCC, INFORMIX_JCC, INFORMIX_JDBC, SYBASE, ORACLE_JDBC, ORACLE_UCP, DATADIRECT_SQLSERVER, MICROSOFT_SQLSERVER })
    public void testDSOverride013() throws Throwable {
        final long id = atomicID.incrementAndGet();
        System.out.println("Start executing DSOverrideTestServlet.testDSOverride013");

        try {
            // Create a new Entity using the persistence unit's default datasource
            tx.begin();

            EntityManager em1 = emf.createEntityManager();
            em1.joinTransaction();

            Map<String, Object> map = new HashMap<String, Object>();
            map.put(getJTADatasourcePropertyKeyName(), altJTADataSource);
            map.put("eclipselink.jdbc.exclusive-connection.mode", "Always");
            EntityManager em2 = emf.createEntityManager(map);
            em2.joinTransaction();

            DSOverrideEntity newEntity1 = new DSOverrideEntity(id, "data-1");
            em1.persist(newEntity1);

            DSOverrideEntity newEntity2 = new DSOverrideEntity(id, "data-2");
            em2.persist(newEntity2);

            tx.commit();

            em1.clear();
            em2.clear();

            // Read JTA Tran
            tx.begin();
            em1.joinTransaction();
            em2.joinTransaction();

            Map<String, Object> lockTimeoutMap = new HashMap<String, Object>();
            lockTimeoutMap.put("javax.persistence.lock.timeout", 0);

            DSOverrideEntity find1 = em1.find(DSOverrideEntity.class, id, LockModeType.PESSIMISTIC_WRITE, lockTimeoutMap);
            DSOverrideEntity find2 = em2.find(DSOverrideEntity.class, id, LockModeType.PESSIMISTIC_WRITE, lockTimeoutMap);

            assertNotSame(find1, newEntity1);
            assertNotSame(find2, newEntity2);
            assertNotSame(find1, find2);
            assertNotNull(find1);
            assertNotNull(find2);
            assertEquals(find1.getId(), id);
            assertEquals(find2.getId(), id);
            assertEquals(find1.getStrData(), "data-1");
            assertEquals(find2.getStrData(), "data-2");

            tx.rollback();
            em1.clear();
            em2.clear();

            tx.begin();
            em1.joinTransaction();
            em2.joinTransaction();

            DSOverrideEntity find2a = em2.find(DSOverrideEntity.class, id, LockModeType.PESSIMISTIC_WRITE, lockTimeoutMap);
            DSOverrideEntity find1a = em1.find(DSOverrideEntity.class, id, LockModeType.PESSIMISTIC_WRITE, lockTimeoutMap);

            assertNotSame(find1a, find1);
            assertNotSame(find2a, find2);
            assertNotSame(find1a, find2a);
            assertNotNull(find1a);
            assertNotNull(find2a);
            assertEquals(find1a.getId(), id);
            assertEquals(find2a.getId(), id);
            assertEquals(find1a.getStrData(), "data-1");
            assertEquals(find2a.getStrData(), "data-2");

            em1.close();
            em2.close();

        } finally {
            System.out.println("End executing DSOverrideTestServlet.testDSOverride013");
        }
    }

    /**
     * Verify that setting "javax.persistence.JtaDataSource" with ECL's exclusive connection properties
     * with a DataSource instance works. Dual insertions of same ID on both tables, verifying that no
     * collissions occur. Reads on both EMs performed in and outside of JTA transactions. Variant
     * using PESSIMISTIC WRITE with JPQL Query.
     *
     * @throws Throwable
     */
    @Test
    @SkipIfDataSourceProperties({ DB2_I_NATIVE, DB2_I_TOOLBOX, DB2_JCC, INFORMIX_JCC, INFORMIX_JDBC, SYBASE, ORACLE_JDBC, ORACLE_UCP, DATADIRECT_SQLSERVER, MICROSOFT_SQLSERVER })
    public void testDSOverride014() throws Throwable {
        final long id = atomicID.incrementAndGet();
        System.out.println("Start executing DSOverrideTestServlet.testDSOverride014");

        try {
            // Create a new Entity using the persistence unit's default datasource
            tx.begin();

            EntityManager em1 = emf.createEntityManager();
            em1.joinTransaction();

            Map<String, Object> map = new HashMap<String, Object>();
            map.put(getJTADatasourcePropertyKeyName(), altJTADataSource);
            map.put("eclipselink.jdbc.exclusive-connection.mode", "Always");
            EntityManager em2 = emf.createEntityManager(map);
            em2.joinTransaction();

            DSOverrideEntity newEntity1 = new DSOverrideEntity(id, "data-1");
            em1.persist(newEntity1);

            DSOverrideEntity newEntity2 = new DSOverrideEntity(id, "data-2");
            em2.persist(newEntity2);

            tx.commit();

            em1.clear();
            em2.clear();

            // Read JTA Tran
            tx.begin();
            em1.joinTransaction();
            em2.joinTransaction();

            Map<String, Object> lockTimeoutMap = new HashMap<String, Object>();
            lockTimeoutMap.put("javax.persistence.lock.timeout", 0);

            DSOverrideEntity find1 = findByQuery(em1, id, LockModeType.PESSIMISTIC_WRITE, lockTimeoutMap);
            DSOverrideEntity find2 = findByQuery(em2, id, LockModeType.PESSIMISTIC_WRITE, lockTimeoutMap);

            assertNotSame(find1, newEntity1);
            assertNotSame(find2, newEntity2);
            assertNotSame(find1, find2);
            assertNotNull(find1);
            assertNotNull(find2);
            assertEquals(find1.getId(), id);
            assertEquals(find2.getId(), id);
            assertEquals(find1.getStrData(), "data-1");
            assertEquals(find2.getStrData(), "data-2");

            tx.rollback();
            em1.clear();
            em2.clear();

            tx.begin();
            em1.joinTransaction();
            em2.joinTransaction();

            DSOverrideEntity find2a = findByQuery(em2, id, LockModeType.PESSIMISTIC_WRITE, lockTimeoutMap);
            DSOverrideEntity find1a = findByQuery(em1, id, LockModeType.PESSIMISTIC_WRITE, lockTimeoutMap);

            assertNotSame(find1a, find1);
            assertNotSame(find2a, find2);
            assertNotSame(find1a, find2a);
            assertNotNull(find1a);
            assertNotNull(find2a);
            assertEquals(find1a.getId(), id);
            assertEquals(find2a.getId(), id);
            assertEquals(find1a.getStrData(), "data-1");
            assertEquals(find2a.getStrData(), "data-2");

            em1.close();
            em2.close();

        } finally {
            System.out.println("End executing DSOverrideTestServlet.testDSOverride014");
        }
    }

    /**
     * Verify that setting "javax.persistence.JtaDataSource" with ECL's exclusive connection properties
     * with a DataSource instance works. Dual insertions of same ID on both tables, verifying that no
     * collissions occur. Remove on the unmodified EM and verification that the remove does not affect the
     * overridden EM.
     *
     * @throws Throwable
     */
    @Test
    @SkipIfDataSourceProperties({ DB2_I_NATIVE, DB2_I_TOOLBOX, DB2_JCC, INFORMIX_JCC, INFORMIX_JDBC, SYBASE, ORACLE_JDBC, ORACLE_UCP, DATADIRECT_SQLSERVER, MICROSOFT_SQLSERVER })
    public void testDSOverride015() throws Throwable {
        final long id = atomicID.incrementAndGet();
        System.out.println("Start executing DSOverrideTestServlet.testDSOverride015");

        try {
            // Create a new Entity using the persistence unit's default datasource
            tx.begin();

            EntityManager em1 = emf.createEntityManager();
            em1.joinTransaction();

            Map<String, Object> map = new HashMap<String, Object>();
            map.put(getJTADatasourcePropertyKeyName(), altJTADataSource);
            map.put("eclipselink.jdbc.exclusive-connection.mode", "Always");
            EntityManager em2 = emf.createEntityManager(map);
            em2.joinTransaction();

            DSOverrideEntity newEntity1 = new DSOverrideEntity(id, "data-1");
            em1.persist(newEntity1);

            DSOverrideEntity newEntity2 = new DSOverrideEntity(id, "data-2");
            em2.persist(newEntity2);

            tx.commit();

            em1.clear();
            em2.clear();

            // Read JTA Tran
            tx.begin();
            em1.joinTransaction();
            em2.joinTransaction();

            DSOverrideEntity find1 = em1.find(DSOverrideEntity.class, id);
            DSOverrideEntity find2 = em2.find(DSOverrideEntity.class, id);

            assertNotSame(find1, newEntity1);
            assertNotSame(find2, newEntity2);
            assertNotSame(find1, find2);
            assertNotNull(find1);
            assertNotNull(find2);
            assertEquals(find1.getId(), id);
            assertEquals(find2.getId(), id);
            assertEquals(find1.getStrData(), "data-1");
            assertEquals(find2.getStrData(), "data-2");

            // Kill find1
            em1.remove(find1);

            tx.commit();
            em1.clear();
            em2.clear();

            // Read without JTA Tran
            DSOverrideEntity find1a = em1.find(DSOverrideEntity.class, id);
            DSOverrideEntity find2a = em2.find(DSOverrideEntity.class, id);

            assertNotSame(find2a, find2);
            assertNull(find1a);
            assertNotNull(find2a);
            assertEquals(find2a.getId(), id);
            assertEquals(find2a.getStrData(), "data-2");

            em1.close();
            em2.close();

        } finally {
            System.out.println("End executing DSOverrideTestServlet.testDSOverride015");
        }
    }

    /**
     * Verify that setting "javax.persistence.JtaDataSource" with ECL's exclusive connection properties
     * with a DataSource instance works. Dual insertions of same ID on both tables, verifying that no
     * collissions occur. Remove on the unmodified EM and verification that the remove does not affect the
     * overridden EM. Verified by Query.
     *
     * @throws Throwable
     */
    @Test
    @SkipIfDataSourceProperties({ DB2_I_NATIVE, DB2_I_TOOLBOX, DB2_JCC, INFORMIX_JCC, INFORMIX_JDBC, SYBASE, ORACLE_JDBC, ORACLE_UCP, DATADIRECT_SQLSERVER, MICROSOFT_SQLSERVER })
    public void testDSOverride016() throws Throwable {
        final long id = atomicID.incrementAndGet();
        System.out.println("Start executing DSOverrideTestServlet.testDSOverride016");

        try {
            // Create a new Entity using the persistence unit's default datasource
            tx.begin();

            EntityManager em1 = emf.createEntityManager();
            em1.joinTransaction();

            Map<String, Object> map = new HashMap<String, Object>();
            map.put(getJTADatasourcePropertyKeyName(), altJTADataSource);
            map.put("eclipselink.jdbc.exclusive-connection.mode", "Always");
            EntityManager em2 = emf.createEntityManager(map);
            em2.joinTransaction();

            DSOverrideEntity newEntity1 = new DSOverrideEntity(id, "data-1");
            em1.persist(newEntity1);

            DSOverrideEntity newEntity2 = new DSOverrideEntity(id, "data-2");
            em2.persist(newEntity2);

            tx.commit();

            em1.clear();
            em2.clear();

            // Read JTA Tran
            tx.begin();
            em1.joinTransaction();
            em2.joinTransaction();

            DSOverrideEntity find1 = findByQuery(em1, id);
            DSOverrideEntity find2 = findByQuery(em2, id);

            assertNotSame(find1, newEntity1);
            assertNotSame(find2, newEntity2);
            assertNotSame(find1, find2);
            assertNotNull(find1);
            assertNotNull(find2);
            assertEquals(find1.getId(), id);
            assertEquals(find2.getId(), id);
            assertEquals(find1.getStrData(), "data-1");
            assertEquals(find2.getStrData(), "data-2");

            // Kill find1
            em1.remove(find1);

            tx.commit();
            em1.clear();
            em2.clear();

            // Read without JTA Tran
            DSOverrideEntity find1a = null;
            try {
                find1a = findByQuery(em1, id);
            } catch (NoResultException nre) {
                // Expected
            }
            DSOverrideEntity find2a = findByQuery(em2, id);

            assertNotSame(find2a, find2);
            assertNull(find1a);
            assertNotNull(find2a);
            assertEquals(find2a.getId(), id);
            assertEquals(find2a.getStrData(), "data-2");

            em1.close();
            em2.close();

        } finally {
            System.out.println("End executing DSOverrideTestServlet.testDSOverride016");
        }
    }

    /**
     * Verify that setting "javax.persistence.JtaDataSource" with ECL's exclusive connection properties
     * with a DataSource instance works. Dual insertions of same ID on both tables, verifying that no
     * collissions occur. Remove on the overridden EM and verification that the remove does not affect the
     * unmodified EM.
     *
     * @throws Throwable
     */
    @Test
    @SkipIfDataSourceProperties({ DB2_I_NATIVE, DB2_I_TOOLBOX, DB2_JCC, INFORMIX_JCC, INFORMIX_JDBC, SYBASE, ORACLE_JDBC, ORACLE_UCP, DATADIRECT_SQLSERVER, MICROSOFT_SQLSERVER })
    public void testDSOverride017() throws Throwable {
        final long id = atomicID.incrementAndGet();
        System.out.println("Start executing DSOverrideTestServlet.testDSOverride017");

        try {
            // Create a new Entity using the persistence unit's default datasource
            tx.begin();

            EntityManager em1 = emf.createEntityManager();
            em1.joinTransaction();

            Map<String, Object> map = new HashMap<String, Object>();
            map.put(getJTADatasourcePropertyKeyName(), altJTADataSource);
            map.put("eclipselink.jdbc.exclusive-connection.mode", "Always");
            EntityManager em2 = emf.createEntityManager(map);
            em2.joinTransaction();

            DSOverrideEntity newEntity1 = new DSOverrideEntity(id, "data-1");
            em1.persist(newEntity1);

            DSOverrideEntity newEntity2 = new DSOverrideEntity(id, "data-2");
            em2.persist(newEntity2);

            tx.commit();

            em1.clear();
            em2.clear();

            // Read JTA Tran
            tx.begin();
            em1.joinTransaction();
            em2.joinTransaction();

            DSOverrideEntity find1 = em1.find(DSOverrideEntity.class, id);
            DSOverrideEntity find2 = em2.find(DSOverrideEntity.class, id);

            assertNotSame(find1, newEntity1);
            assertNotSame(find2, newEntity2);
            assertNotSame(find1, find2);
            assertNotNull(find1);
            assertNotNull(find2);
            assertEquals(find1.getId(), id);
            assertEquals(find2.getId(), id);
            assertEquals(find1.getStrData(), "data-1");
            assertEquals(find2.getStrData(), "data-2");

            // Kill find1
            em2.remove(find2);

            tx.commit();
            em1.clear();
            em2.clear();

            // Read without JTA Tran
            DSOverrideEntity find1a = em1.find(DSOverrideEntity.class, id);
            DSOverrideEntity find2a = em2.find(DSOverrideEntity.class, id);

            assertNotSame(find1a, find1);
            assertNull(find2a);
            assertNotNull(find1a);
            assertEquals(find1a.getId(), id);
            assertEquals(find1a.getStrData(), "data-1");

            em1.close();
            em2.close();

        } finally {
            System.out.println("End executing DSOverrideTestServlet.testDSOverride017");
        }
    }

    /**
     * Verify that setting "javax.persistence.JtaDataSource" with ECL's exclusive connection properties
     * with a DataSource instance works. Dual insertions of same ID on both tables, verifying that no
     * collissions occur. Mutation (data change) on default EntityManager invoked, which should not be
     * observed on DSOverride EntityManager.
     *
     * @throws Throwable
     */
    @Test
    @SkipIfDataSourceProperties({ DB2_I_NATIVE, DB2_I_TOOLBOX, DB2_JCC, INFORMIX_JCC, INFORMIX_JDBC, SYBASE, ORACLE_JDBC, ORACLE_UCP, DATADIRECT_SQLSERVER, MICROSOFT_SQLSERVER })
    public void testDSOverride018() throws Throwable {
        final long id = atomicID.incrementAndGet();
        System.out.println("Start executing DSOverrideTestServlet.testDSOverride018");

        try {
            // Create a new Entity using the persistence unit's default datasource
            tx.begin();

            EntityManager em1 = emf.createEntityManager();
            em1.joinTransaction();

            Map<String, Object> map = new HashMap<String, Object>();
            map.put(getJTADatasourcePropertyKeyName(), altJTADataSource);
            map.put("eclipselink.jdbc.exclusive-connection.mode", "Always");
            EntityManager em2 = emf.createEntityManager(map);
            em2.joinTransaction();

            DSOverrideEntity newEntity1 = new DSOverrideEntity(id, "data-1");
            em1.persist(newEntity1);

            DSOverrideEntity newEntity2 = new DSOverrideEntity(id, "data-2");
            em2.persist(newEntity2);

            tx.commit();

            em1.clear();
            em2.clear();

            // Read JTA Tran
            tx.begin();
            em1.joinTransaction();
            em2.joinTransaction();

            DSOverrideEntity find1 = em1.find(DSOverrideEntity.class, id);
            DSOverrideEntity find2 = em2.find(DSOverrideEntity.class, id);

            assertNotSame(find1, newEntity1);
            assertNotSame(find2, newEntity2);
            assertNotSame(find1, find2);
            assertNotNull(find1);
            assertNotNull(find2);
            assertEquals(find1.getId(), id);
            assertEquals(find2.getId(), id);
            assertEquals(find1.getStrData(), "data-1");
            assertEquals(find2.getStrData(), "data-2");

            find1.setStrData("data-1a");
            tx.commit();
            em1.clear();
            em2.clear();

            // Read without JTA Tran
            DSOverrideEntity find1a = em1.find(DSOverrideEntity.class, id);
            DSOverrideEntity find2a = em2.find(DSOverrideEntity.class, id);

            assertNotSame(find1a, find1);
            assertNotSame(find2a, find2);
            assertNotSame(find1a, find2a);
            assertNotNull(find1a);
            assertNotNull(find2a);
            assertEquals(find1a.getId(), id);
            assertEquals(find2a.getId(), id);
            assertEquals(find1a.getStrData(), "data-1a");
            assertEquals(find2a.getStrData(), "data-2");

            em1.clear();
            em2.clear();

            // Read with JTA Tran
            tx.begin();
            em1.joinTransaction();
            em2.joinTransaction();

            find1a = em1.find(DSOverrideEntity.class, id);
            find2a = em2.find(DSOverrideEntity.class, id);

            assertNotSame(find1a, find1);
            assertNotSame(find2a, find2);
            assertNotSame(find1a, find2a);
            assertNotNull(find1a);
            assertNotNull(find2a);
            assertEquals(find1a.getId(), id);
            assertEquals(find2a.getId(), id);
            assertEquals(find1a.getStrData(), "data-1a");
            assertEquals(find2a.getStrData(), "data-2");

            tx.rollback();

            em1.close();
            em2.close();

        } finally {
            System.out.println("End executing DSOverrideTestServlet.testDSOverride018");
        }
    }

    /**
     * Verify that setting "javax.persistence.JtaDataSource" with ECL's exclusive connection properties
     * with a DataSource instance works. Dual insertions of same ID on both tables, verifying that no
     * collissions occur. Mutation (data change) on DSOverride EntityManager invoked, which should not be
     * observed on default EntityManager.
     *
     * @throws Throwable
     */
    @Test
    @SkipIfDataSourceProperties({ DB2_I_NATIVE, DB2_I_TOOLBOX, DB2_JCC, INFORMIX_JCC, INFORMIX_JDBC, SYBASE, ORACLE_JDBC, ORACLE_UCP, DATADIRECT_SQLSERVER, MICROSOFT_SQLSERVER })
    public void testDSOverride019() throws Throwable {
        final long id = atomicID.incrementAndGet();
        System.out.println("Start executing DSOverrideTestServlet.testDSOverride019");

        try {
            // Create a new Entity using the persistence unit's default datasource
            tx.begin();

            EntityManager em1 = emf.createEntityManager();
            em1.joinTransaction();

            Map<String, Object> map = new HashMap<String, Object>();
            map.put(getJTADatasourcePropertyKeyName(), altJTADataSource);
            map.put("eclipselink.jdbc.exclusive-connection.mode", "Always");
            EntityManager em2 = emf.createEntityManager(map);
            em2.joinTransaction();

            DSOverrideEntity newEntity1 = new DSOverrideEntity(id, "data-1");
            em1.persist(newEntity1);

            DSOverrideEntity newEntity2 = new DSOverrideEntity(id, "data-2");
            em2.persist(newEntity2);

            tx.commit();

            em1.clear();
            em2.clear();

            // Read JTA Tran
            tx.begin();
            em1.joinTransaction();
            em2.joinTransaction();

            DSOverrideEntity find1 = em1.find(DSOverrideEntity.class, id);
            DSOverrideEntity find2 = em2.find(DSOverrideEntity.class, id);

            assertNotSame(find1, newEntity1);
            assertNotSame(find2, newEntity2);
            assertNotSame(find1, find2);
            assertNotNull(find1);
            assertNotNull(find2);
            assertEquals(find1.getId(), id);
            assertEquals(find2.getId(), id);
            assertEquals(find1.getStrData(), "data-1");
            assertEquals(find2.getStrData(), "data-2");

            find2.setStrData("data-2a");
            tx.commit();
            em1.clear();
            em2.clear();

            // Read without JTA Tran
            DSOverrideEntity find1a = em1.find(DSOverrideEntity.class, id);
            DSOverrideEntity find2a = em2.find(DSOverrideEntity.class, id);

            assertNotSame(find1a, find1);
            assertNotSame(find2a, find2);
            assertNotSame(find1a, find2a);
            assertNotNull(find1a);
            assertNotNull(find2a);
            assertEquals(find1a.getId(), id);
            assertEquals(find2a.getId(), id);
            assertEquals(find1a.getStrData(), "data-1");
            assertEquals(find2a.getStrData(), "data-2a");

            em1.clear();
            em2.clear();

            // Read with JTA Tran
            tx.begin();
            em1.joinTransaction();
            em2.joinTransaction();

            find1a = em1.find(DSOverrideEntity.class, id);
            find2a = em2.find(DSOverrideEntity.class, id);

            assertNotSame(find1a, find1);
            assertNotSame(find2a, find2);
            assertNotSame(find1a, find2a);
            assertNotNull(find1a);
            assertNotNull(find2a);
            assertEquals(find1a.getId(), id);
            assertEquals(find2a.getId(), id);
            assertEquals(find1a.getStrData(), "data-1");
            assertEquals(find2a.getStrData(), "data-2a");

            tx.rollback();

            em1.close();
            em2.close();

        } finally {
            System.out.println("End executing DSOverrideTestServlet.testDSOverride019");
        }
    }

    private static DSOverrideEntity findByQuery(EntityManager em, long id) {
        return findByQuery(em, id, null, null);
    }

    private static DSOverrideEntity findByQuery(EntityManager em, long id, Map<String, Object> props) {
        return findByQuery(em, id, null, props);
    }

    private static DSOverrideEntity findByQuery(EntityManager em, long id, LockModeType lockMode, Map<String, Object> props) {
        Query q = em.createQuery(qStr);
        if (lockMode != null) {
            q.setLockMode(lockMode);
        }
        if (props != null && !props.isEmpty()) {
            for (String key : props.keySet()) {
                q.setHint(key, props.get(key));
            }

        }
        q.setParameter(1, id);

        DSOverrideEntity findEntity = (DSOverrideEntity) q.getSingleResult();
        System.out.println("Query find operation returned: " + findEntity);
        return findEntity;
    }

    private DSOverrideEntity populateRow(EntityManagerFactory emf, Map<String, Object> emMap,
                                         long id, String strData) throws Exception {
        tx.begin();
        EntityManager em = null;

        try {
            em = (emMap != null && !emMap.isEmpty()) ? emf.createEntityManager(emMap) : emf.createEntityManager();
            em.joinTransaction();

            DSOverrideEntity newEntity = new DSOverrideEntity(id, strData);
            em.persist(newEntity);

            tx.commit();

            return newEntity;
        } finally {
            em.close();
        }
    }

    private DSOverrideEntity validateRowExistanceByFind(EntityManagerFactory emf, Map<String, Object> emMap,
                                                        long id, String strData,
                                                        List<DSOverrideEntity> notSame, List<DSOverrideEntity> same,
                                                        boolean startTran) {
        EntityManager em = null;

        try {
            if (startTran) {
                tx.begin();
            }
            em = (emMap == null || emMap.isEmpty()) ? emf.createEntityManager() : emf.createEntityManager(emMap);
            DSOverrideEntity find = em.find(DSOverrideEntity.class, id);
            if (startTran) {
                tx.rollback();
            }

            assertNotNull(find);
            assertEquals(id, find.getId());
            assertEquals(strData, find.getStrData());

            if (notSame != null && !notSame.isEmpty()) {
                for (DSOverrideEntity ns : notSame) {
                    assertNotSame(ns, find);
                }
            }

            if (same != null && !same.isEmpty()) {
                for (DSOverrideEntity s : same) {
                    assertSame(s, find);
                }
            }

            return find;
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            em.close();
        }
    }

    private DSOverrideEntity validateRowExistanceByQuery(EntityManagerFactory emf, Map<String, Object> emMap,
                                                         long id, String strData, Map<String, Object> queryHints,
                                                         List<DSOverrideEntity> notSame, List<DSOverrideEntity> same,
                                                         boolean startTran) {
        EntityManager em = null;

        try {
            if (startTran) {
                tx.begin();
            }
            em = (emMap == null || emMap.isEmpty()) ? emf.createEntityManager() : emf.createEntityManager(emMap);
            DSOverrideEntity find = (queryHints == null || queryHints.isEmpty()) ? findByQuery(em, id) : findByQuery(em, id, queryHints);
            if (startTran) {
                tx.rollback();
            }

            assertNotNull(find); // Query.getSingleResult() would throw NoResultException if not found.
            assertEquals(id, find.getId());
            assertEquals(strData, find.getStrData());

            if (notSame != null && !notSame.isEmpty()) {
                for (DSOverrideEntity ns : notSame) {
                    assertNotSame(ns, find);
                }
            }

            if (same != null && !same.isEmpty()) {
                for (DSOverrideEntity s : same) {
                    assertSame(s, find);
                }
            }

            return find;
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            em.close();
        }
    }

    private void validateRowNonExistanceByFind(EntityManagerFactory emf, Map<String, Object> emMap, long id, boolean startTran) {
        EntityManager em = null;

        try {
            if (startTran) {
                tx.begin();
            }
            em = (emMap == null || emMap.isEmpty()) ? emf.createEntityManager() : emf.createEntityManager(emMap);
            DSOverrideEntity find = em.find(DSOverrideEntity.class, id);
            if (startTran) {
                tx.rollback();
            }

            // Find should not have returned null.
            assertNull(find);
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            em.close();
        }
    }

    private void validateRowNonExistanceByQuery(EntityManagerFactory emf, Map<String, Object> emMap,
                                                long id, Map<String, Object> queryHints, boolean startTran) {
        EntityManager em = null;

        try {
            if (startTran) {
                tx.begin();
            }
            em = (emMap == null || emMap.isEmpty()) ? emf.createEntityManager() : emf.createEntityManager(emMap);

            try {
                DSOverrideEntity find = (queryHints == null || queryHints.isEmpty()) ? findByQuery(em, id) : findByQuery(em, id, queryHints);
                fail("Did not throw NoResultException.");
            } catch (NoResultException nre) {
                // Expected.
            } finally {
                if (startTran) {
                    tx.rollback();
                }
            }
        } catch (java.lang.AssertionError ae) {
            throw ae;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            em.close();
        }
    }
}
