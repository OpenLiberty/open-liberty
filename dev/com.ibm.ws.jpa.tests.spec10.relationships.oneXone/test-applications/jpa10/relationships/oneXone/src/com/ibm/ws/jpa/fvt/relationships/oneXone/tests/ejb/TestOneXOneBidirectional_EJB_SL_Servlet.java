/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.oneXone.tests.ejb;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

import componenttest.annotation.ExpectedFFDC;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOneXOneBidirectional_EJB_SL_Servlet")
public class TestOneXOneBidirectional_EJB_SL_Servlet extends EJBTestVehicleServlet {
    private final String testLogicClassName = "com.ibm.ws.jpa.fvt.relationships.oneXone.testlogic.OneXOneBidirectionalTestLogic";

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    private final static String ejbJNDIName = "ejb/OneXOneSLEJB";

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OneXOne_Bi_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OneXOne_Bi_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OneXOne_Bi_CMTS"));
    }

    /*
     * Verify basic One-to-One service by creating BiEntityA and BiEntityB, and
     * link BiEntityA.defaultRelationship to the instance of BiEntityB.
     *
     * Test verifies function performing the following:
     * 1) Create new BiEntityB(id=1)
     * 2) Create new BiEntityA(id=1), setting defaultRelationship to BiEntityB(id=1).
     * 3) Keep other (optional) OneXOne fields set to null.
     * 4) Clear persistence context (if necessary), and find BiEntityA(id=1).
     * 5) Verify field data in BiEntityA(id=1)
     * 6) Verify field data in BiEntityA.defaultRelationship (should reference BiEntityB(id=1))
     * 7) Test passes if all these conditions are met.
     *
     * This test case confirms the following function verification:
     * 1) Optional relational fields can be set to null without persistence errors
     * (all fields except defaultRelationship are null in BiEntityA(id=1))
     * 2) A traversable OneXOne association between BiEntityA to BiEntityB(id=1) is
     * established, and BiEntityB is accessible through the relational
     * reference defaultRelationship in BiEntityA.
     * 3) The default FetchMode, EAGER, for defaultRelationship should make all of BiEntityB(id=1)'s
     * data available for access, even after the entities have been detached
     * from the persistence context.
     */

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_001_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_001_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_001_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_001_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_001_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_001_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_001_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_001_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_001_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_001_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_001_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_001_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Verify JoinColumn table name annotation
     *
     * Test verifies function performing the following:
     * 1) Create new BiEntityB(id=1)
     * 2) Create new BiEntityA(id=1), setting overrideColumnNameRelationship to BiEntityB(id=1).
     * 3) Keep other (optional) OneXOne fields set to null.
     * 4) Clear persistence context (if necessary), and find BiEntityA(id=1).
     * 5) Verify field data in BiEntityA(id=1)
     * 6) Verify field data in BiEntityA.b2 (should reference BiEntityB(id=1))
     * 7) Test passes if all these conditions are met.
     *
     * This test case is virtually idential to testOneXOneUni001, only that the target
     * field used is marked with a column-name override in the JoinColumn annotation.
     */

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_002_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_002_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B2");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_002_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_002_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B2");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_002_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_002_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B2");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_002_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_002_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B2");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_002_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_002_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B2");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_002_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_002_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B2");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Verify that optionality is enforced (a OneXOne relational reference cannot be set null
     * if its OneToOne annotation has the element/value pair optional=false).
     *
     * Test verifies function performing the following:
     * - Verify that an entity with a non-optional field cannot be created and saved to the db
     * with a null value in the non-optional field
     * 1) Create a new BiEntityANoOpt(id=1), setting all relational fields to null
     * 2) A persistence exception should be thrown while trying to persist BiEntityA(id=1)
     * to the database. Test Point passes if an exception is thrown on transaction commit.
     * - Verify that an existing entity with a non-optional field cannot change the optional field
     * to a null value and save it to the database.
     * 1) Create a new Create new BiEntityB(id=1)
     * 2) Create a new BiEntityANoOpt(id=1), setting b to BiEntityB(id=1). Save to the database
     * (not expecting any exceptions)
     * 3) Clear the persistence context, find BiEntityANoOpt(id=1)
     * 4) Set the b field on the BiEntityANoOpt(id=1) returned by find() to null. An exception
     * should be thrown when the transaction intended to save the changes to the db attempts to
     * commit. Test point passes if an exception is thrown.
     */

//    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_003_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_003_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OONoOptBiEntityA");
        properties.put("EntityBName", "OONoOptBiEntityB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

//    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_003_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_003_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOONoOptBiEntityA");
        properties.put("EntityBName", "XMLOONoOptBiEntityB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

//    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_003_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_003_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OONoOptBiEntityA");
        properties.put("EntityBName", "OONoOptBiEntityB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

//    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_003_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_003_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOONoOptBiEntityA");
        properties.put("EntityBName", "XMLOONoOptBiEntityB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

//    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_003_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_003_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OONoOptBiEntityA");
        properties.put("EntityBName", "OONoOptBiEntityB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

//    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_003_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_003_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOONoOptBiEntityA");
        properties.put("EntityBName", "XMLOONoOptBiEntityB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Verify relational field LAZY fetch behavior.
     *
     * Test verifies function performing the following:
     * 1) Create new BiEntityB(id=1)
     * 3) Create new BiEntityA(id=1), setting Lazy to BiEntity(id=1)].
     * Keep other (optional) OneXOne fields set to null.
     * 4) Clear the persistence context, forcing all entities to become detached.
     * 5) Acquire a fresh, managed copy of BiEntityA(id=1) by invoking
     * find BiEntityA(id=1) and access BiEntityA(id=1).Lazy's name field. The
     * data in BiEntityB(id=1) should be accessible. TranScoped PC's will need
     * to take special care to perform this work under a live persistence context
     * (ie, find and data retrieval operations are in the same transaction)
     * 6) Clear the persistence context again, forcing all entties to become detached.
     * 7) Acquire a new copy of BiEntityA(id=1), and clear the persistence context again
     * BEFORE attempting to access any fields. This forces the entity to become detached.
     * 6) The data should not be available since Lazy was never accessed.
     * 7) Try to access Lazy, which should be null because it is lazy loaded, and was never
     * accessed before the entity was detached.
     *
     * Test passes if :
     * 1) BiEntityB(id=1)'s name field could be accessed while the entity
     * is not detached
     * 2) If BiEntityB(id=1)'s name field could NOT be accessed if the entity was
     * never previously accessed before becomming a detached entity.
     *
     */

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_004_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_004_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B4");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_004_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_004_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B4");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_004_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_004_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B4");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_004_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_004_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B4");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_004_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_004_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B4");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_004_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_004_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B4");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Test Cascade, Type: Default (no cascade options specified) Verification
     *
     * Verify cascade: none behavior (default). By this:
     *
     * Persist:
     *
     * Both entities in the relationship need to have persist() invoked on them
     * in order to be stored on the database. By default, persisting the owning side
     * of the relationship does not automatically persist the entity on the inverse
     * side, and vice versa.
     *
     * 1) In a new transaction, create an unpersisted BiEntityB(id=1).
     * and a persisted BiEntityA(id=1), setting defaultRelationship to BiEntityB(id=1).
     * 2) Commit the transaction. An IllegalStateException should be thrown, because
     * b1 is referencing an entity that is not managed and does not exist on the database.
     *
     * Remove:
     *
     * By default, removing the entity on the owning side of the oneXone relationship does
     * not cause the entity on the inverse side of the relationship to become removed as well.
     *
     * 1) Create Create BiEntityB(id=1)
     * 2) Create new BiEntityA(id=1), setting defaultRelationship to BiEntityB(id=1).
     * 3) Remove BiEntityA(id=1), verify that BiEntityB(id=1) still exists. Test point passes if it does.
     *
     * If the entity on the inverse side of the relationship is removed, it should not remove the entity
     * on the owning of the relationship. Also, since the oneXone relationship is optional, the relationship
     * field should be set null on fresh instances of the owning entity from find().
     *
     * 1) Create Create BiEntityB(id=1)
     * 2) Create new BiEntityA(id=1), setting defaultRelationship to BiEntityB(id=1).
     * 3) Remove BiEntityB(id=1)
     * 4) Clear the persistence context
     * 5) Find BiEntityA(id=1). It should still exist (especially given that it is a unidirectional
     * relationship), and defaultRelationship should be null.
     *
     * Merge:
     *
     * Merge will attempt to update the managed entity to point to managed versions of entities referenced by
     * the detached entity.
     *
     * 1) Create BiEntityB(id=1)
     * 2) Create new BiEntityA(id=1), setting defaultRelationship to BiEntityB(id=1).
     * 3) Clear the persistence context, causing all entities to become detached
     * 4) Perform the following operations:
     * Modify the name field on BiEntityB(id=1)
     * Modify the name field on BiEntityA(id=1)
     * 5) Merge BiEntityA(id=1) into the persistence context.
     * 6) Verify the change to BiEntityA(id=1)'s name field. Verify that BiEntityA(id=1)'s defaultRelationship field references
     * a managed copy of BiEntityB(id=1) and that BiEntityB(id=1)'s name field contains the original value.
     * BiEntityB(id=1)'s name field should not have been changed because EntityManager.merge() was never called
     * on it directly, and merge by default does not cascade across entity relationships.
     *
     * If an entity being merged points to a removed entity, an IllegalArgumentException should be thrown.
     *
     * 1) Create Create BiEntityB(id=1)
     * 2) Create new BiEntityA(id=1), setting defaultRelationship to BiEntityB(id=1).
     * 3) Clear the persistence context
     * 4) In a single transaction, find BiEntityB(id=1) and remove it. This deletes BiEntityB(id=1) from the database.
     * Note: the original detached BiEntityB(id=1) reference still exists, and is referenced by the detached copy of
     * BiEntityA(id=1).
     * 5) Modify the name field of BiEntityA(id=1) and attempt to merge it. An IllegalArgumentException because the
     * detached entity still has a references in its defaultRelationship field to BiEntityB(id=1), which no longer exists in the database.
     *
     * Lazy loaded relationships, if not triggered while the entity was managed, are ignored during merge().
     *
     * 1) Create Create BiEntityB(id=1)
     * 2) Create new BiEntityA(id=1), setting lazy to BiEntityB(id=1).
     * 3) Clear the persistence context, causing all entities to become detached
     * 4) Find BiEntityA(id=1), but do not access the lazy field.
     * 5) Clear the persistence context, causing all entities to become detached
     * 6) Modify BiEntityA(id=1)'s name field, and set lazy to null.
     * 7) Merge BiEntityA(id=1) into the persistence context. The field lazy should still refer to BiEntityB(id=1) because
     * it was never triggered on the original entity before it became detached.
     *
     * On the other hand, if a lazy-loaded relationship is loaded before the entity is detached, then changes to that
     * relationship are merged.
     *
     * 1) Create Create BiEntityB(id=1)
     * 2) Create new BiEntityA(id=1), setting lazy to BiEntityB(id=1).
     * 3) Clear the persistence context, causing all entities to become detached
     * 4) Find BiEntityA(id=1), and access the lazy field, loading the entity on the inverse side of the relationship
     * into the persistence context.
     * 5) Clear the persistence context, causing all entities to become detached
     * 6) Modify BiEntityA(id=1)'s name field, and set lazy to null.
     * 7) Merge BiEntityA(id=1) into the persistence context. The field lazy should now be null since lazy was accessed
     * before the entity became detached.
     *
     * Refresh:
     *
     * Refresh operations are, by default, not cascaded across entity relationships. Without the REFRESH cascade
     * option, a refresh operation will stop at the soure tntity.
     *
     * 1) Create Create BiEntityB(id=1)
     * 2) Create new BiEntityA(id=1), setting defaultRelationship to BiEntityB(id=1).
     * 3) Start a new transaction
     * 4) With managed copies of the two entities, edit BiEntityA(id=1) and BiEntityA(id=1).defaultRelationship's name fields.
     * 5) Invoke EntityManager.refresh() on BiEntityA(id=1)
     * 6) Verify that BiEntityA(id=1)'s name field has been reverted to the value it had when it was created.
     * 7) Verify that BiEntityB(id=1) still has the new value
     */

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Bidirectional_005_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_005_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Bidirectional_005_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_005_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_005_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_005_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_005_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_005_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Bidirectional_005_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_005_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Bidirectional_005_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_005_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Test Cascade, Type: ALL (all cascade options specified) Verification
     *
     * Verify cascade: all. By this:
     *
     * Persist:
     *
     * With cascade type set to ALL, persist operations are cascaded across the
     * entity relationship. This means if BiEntityA is the target if an
     * EntityManager.persist() operation, the JPA implementation will automatically
     * invoke EntityManager.persist() on all of the BiEntityB entity relationships
     * that are marked with Cascade type ALL (or PERSIST).
     *
     * 1) Create an unpersisted BiEntityB(id=1)
     * 2) Create an unpersisted BiEntityA(id=1)
     * 3) Set CascadeAll to BiEntityB(id=1).
     * 4) Start a new transaction, persist BiEntityA(id=1), and commit the transaction.
     * 5) Clear persistence context, forcing all entities to become detached.
     * 6) Find BiEntityA(id=1), this should hit the database, returning an entity
     * with a reference in CascadeAll to BiEntityB(id=1) because the persist operation
     * was cascaded.
     *
     * Remove:
     *
     * With cascade type set to ALL, remove operations are cascaded across the entity
     * relationship. This means if BiEntityA is the target of an EntityManager.remove()
     * operation, the JPA implementation will automatically invoke EntityManager.remove()
     * on all of the BiEntityB entity relationships that are marked with Cascade type ALL
     * (or REMOVE).
     *
     * 1) Create Create BiEntityB(id=1)
     * 2) Create new BiEntityA(id=1), setting CascadeAll to BiEntityB(id=1).
     * 3) Remove BiEntityA(id=1), verify that BiEntityB(id=1) also no longer exists. Test point passes if true.
     *
     * Merge:
     *
     * With cascade type set to ALL, merge operations will cascade across the entity relationship. So changes to
     * the target entity (BiEntityA(id=1)) and changes to all entities with relationship cascade attributes of
     * ALL (and MERGE) will be merged into the persistence context.
     *
     * 1) Create Create BiEntityB(id=1)
     * 2) Create new BiEntityA(id=1), setting CascadeAll to BiEntityB(id=1).
     * 3) Clear the persistence context, causing all entities to become detached
     * 4) Modify BiEntityA(id=1)'s name field. Modify BiEntityB(id=1)'s name field.
     * 5) Merge BiEntityA(id=1) into the persistence context. Examine the BiEntityB(id=1) referenced by the CascadeAll
     * field of the entity returned from the merge() operation. Both entities should be managed, and both
     * entities should contain the changes from step 4.
     *
     * Refresh:
     *
     * with cascade type set to ALL, refresh operations will cascade across entity relationships. So changes
     * to the target entity (BiEntityA(id=1)) and changes to all entities with relationship cascade attributes of
     * ALL (and REFRESH) will have their contents reset to match the data in the database.
     *
     * 1) Create Create BiEntityB(id=1)
     * 2) Create new BiEntityA(id=1), setting CascadeAll to BiEntityB(id=1).
     * 3) Start a new transaction
     * 4) With managed copies of the two entities, edit BiEntityA(id=1) and BiEntityA(id=1).CascadeAll's name fields.
     * 5) Invoke EntityManager.refresh() on BiEntityA(id=1)
     * 6) Verify that BiEntityA(id=1)'s name field has been reverted to the value it had when it was created.
     * 7) Verify that BiEntityB(id=1)'s name field has been reverted to the value it had when it was created.
     */

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_006_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_006_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5CA");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_006_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_006_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5CA");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_006_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_006_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5CA");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_006_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_006_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5CA");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_006_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_006_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5CA");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_006_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_006_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5CA");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Test Cascade, Type: PERSIST Verification
     *
     * Verify cascade: persist. By this:
     *
     * Persist:
     *
     * With cascade type set to PERSIST, persist operations are cascaded across the
     * entity relationship. This means if BiEntityA is the target if an
     * EntityManager.persist() operation, the JPA implementation will automatically
     * invoke EntityManager.persist() on all of the BiEntityB entity relationships
     * that are marked with Cascade type PERSIST.
     *
     * 1) Create an unpersisted BiEntityB(id=1)
     * 2) Create an unpersisted BiEntityA(id=1)
     * 3) Set CascadePersist to BiEntityB(id=1).
     * 4) Start a new transaction, persist BiEntityA(id=1), and commit the transaction.
     * 5) Clear persistence context, forcing all entities to become detached.
     * 6) Find BiEntityA(id=1), this should hit the database, returning an entity
     * with a reference in CascadePersist to BiEntityB(id=1) because the persist operation
     * was cascaded.
     */

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_007_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_007_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5CP");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_007_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_007_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5CP");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_007_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_007_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5CP");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_007_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_007_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5CP");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_007_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_007_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5CP");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_007_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_007_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5CP");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Test Cascade, Type: REMOVE Verification
     *
     * Verify cascade: remove. By this:
     *
     * Remove:
     *
     * With cascade type set to REMOVE, remove operations are cascaded across the entity
     * relationship. This means if BiEntityA is the target of an EntityManager.remove()
     * operation, the JPA implementation will automatically invoke EntityManager.remove()
     * on all of the BiEntityB entity relationships that are marked with Cascade type REMOVE.
     *
     * 1) Create Create BiEntityB(id=1)
     * 2) Create new BiEntityA(id=1), setting CascadeRemove to BiEntityB(id=1).
     * 3) Remove BiEntityA(id=1), verify that BiEntityB(id=1) also no longer exists. Test point passes if true.
     *
     */

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_008_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_008_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni008";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5RM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_008_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_008_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni008";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5RM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_008_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_008_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni008";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5RM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_008_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_008_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni008";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5RM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_008_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_008_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni008";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5RM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_008_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_008_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni008";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5RM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Test Cascade, Type: MERGE Verification
     *
     * Verify cascade: merge. By this:
     *
     * Merge:
     *
     * With cascade type set to MERGE, merge operations will cascade across the entity relationship. So changes to
     * the target entity (BiEntityA(id=1)) and changes to all entities with relationship cascade attributes of
     * MERGE will be merged into the persistence context.
     *
     * 1) Create Create BiEntityB(id=1)
     * 2) Create new BiEntityA(id=1), setting CascadeMerge to BiEntityB(id=1).
     * 3) Clear the persistence context, causing all entities to become detached
     * 4) Modify BiEntityA(id=1)'s name field. Modify BiEntityB(id=1)'s name field.
     * 5) Merge BiEntityA(id=1) into the persistence context. Examine the BiEntityB(id=1) referenced by CascadeMerge
     * field of the entity returned from the merge() operation. Both entities should be managed, and both
     * entities should contain the changes from step 4.
     */

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_009_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_009_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni009";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5CM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_009_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_009_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni009";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5CM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_009_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_009_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni009";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5CM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_009_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_009_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni009";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5CM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_009_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_009_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni009";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5CM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_009_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_009_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni009";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5CM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Test Cascade, Type: REFRESH Verification
     *
     * Verify cascade: refresh. By this:
     *
     * Refresh:
     *
     * With cascade type set to REFRESH, refresh operations will cascade across entity relationships. So changes
     * to the target entity (BiEntityA(id=1)) and changes to all entities with relationship cascade attributes of
     * REFRESH will have their contents reset to match the data in the database.
     *
     * 1) Create Create BiEntityB(id=1)
     * 2) Create new BiEntityA(id=1), setting CascadeRefresh to BiEntityB(id=1).
     * 3) Start a new transaction
     * 4) With managed copies of the two entities, edit BiEntityA(id=1) and BiEntityA(id=1).CascadeRefresh's name fields.
     * 5) Invoke EntityManager.refresh() on BiEntityA(id=1)
     * 6) Verify that BiEntityA(id=1)'s name field has been reverted to the value it had when it was created.
     * 7) Verify that BiEntityB(id=1)'s name field has been reverted to the value it had when it was created.
     */

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_010_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_010_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni010";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5RF");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_010_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_010_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni010";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5RF");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_010_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_010_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni010";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5RF");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_010_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_010_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni010";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5RF");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_010_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_010_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni010";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5RF");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_010_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_010_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni010";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5RF");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Cardinality Test: Verify that more then one entity on the owning side of the relationship can have a relationship
     * with a specific instance of an entity on the inverse side of the relationship.
     *
     * 1) Create BiEntityB(id=1), save to database.
     * 2) Create new BiEntityA(id=1), setting defaultRelationship to BiEntityB(id=1), and save to database.
     * 3) Create new BiEntityA(id=2), setting defaultRelationship to BiEntityB(id=1), and save to database.
     * 4) Clear the persistence context, and verify that both entities' database state. Test passes if both have a
     * relationship in the defaultRelationship field with BiEntityB(id=1).
     */

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Bidirectional_Cardinality_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Cardinality_Ano_AMJTA_SL_EJB";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiCardEntA");
        properties.put("EntityBName", "OOBiCardEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Bidirectional_Cardinality_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Cardinality_XML_AMJTA_SL_EJB";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiCardEntA");
        properties.put("EntityBName", "XMLOOBiCardEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Cardinality_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Cardinality_Ano_AMRL_SL_EJB";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiCardEntA");
        properties.put("EntityBName", "OOBiCardEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Cardinality_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Cardinality_XML_AMRL_SL_EJB";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiCardEntA");
        properties.put("EntityBName", "XMLOOBiCardEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Bidirectional_Cardinality_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Cardinality_Ano_CMTS_SL_EJB";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiCardEntA");
        properties.put("EntityBName", "OOBiCardEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Bidirectional_Cardinality_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Cardinality_XML_CMTS_SL_EJB";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiCardEntA");
        properties.put("EntityBName", "XMLOOBiCardEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Verify basic bidirectional One-to-One semantics by creating BiEntityA and BiEntityB, establishing
     * a bidirectional relationship between the two entities, and checking that relationship navigatability
     * on both sides meet expectations.
     *
     * Test verifies function by performing the following:
     * 1) Start a new transaction, T1
     * 2) Create a new BiEntityA(id=1)
     * 3) Create a new BiEntityB_B1(id=1)
     * 4) Establish a One-to-One relationship between the two entities with the B1 One-to-One relationship
     * 5) Commit T1
     * 6) Clear the persistence context
     * 7) Fetch a fresh, managed instance of BiEntityB_B1(id=1) from the DB
     * 8) Clear persistence context again (to verify default eager loading behavior)
     * 9) Verify that the entityAField in BiEntityB_B1(id=1) references BiEntityA(id=1)
     * 10) Verify that the B1 field in BiEntityA(id=1) references BiEntityB_B1(id=1)
     * 11) Test passes if all conditions are met.
     *
     * The test cas confirms the following functions:
     * 1) A traversable OneXOne association between BiEntityB_B1(id=1) to BiEntityA(id=1) is established,
     * and BiEntityA is accessible through the relational reference entityAField in BiEntityB_B1.
     * 2) The default FetchMode, EAGER, for entityAField should make all of BiEntityA(id=1)'s
     * data available for access, even after the entities have been detached from the persistence
     * context.
     */

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_001_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_001_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneBi001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_001_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_001_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneBi001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_001_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_001_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneBi001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_001_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_001_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneBi001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_001_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_001_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneBi001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_001_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_001_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneBi001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Test Cascade, Type: Default (no cascade options specified) Verification (26 POINTS)
     *
     * Verify cascade: none behavior (default). By this:
     *
     * Persist:
     *
     * Both entities in the relationship need to have persist() invoked on them
     * in order to be stored on the database. By default, persisting the inverse side
     * of the bidirectional relationship does not automatically persist the entity on the owning
     * side, and vice versa.
     *
     * 1) In a new transaction, create an persisted BiEntityB_DR(id=1)
     * and an unpersisted BiEntityA(id=1), setting BiEntityB_DR(id=1)'s entityA Collection to BiEntityA(id=1).
     * 2) Commit the transaction. An IllegalStateException should be thrown, because
     * entityAField is referencing an entity that is not managed and does not exist on the database.
     *
     * Remove:
     *
     * By default, removing the entity on the inverse side of the manyXmany bidirectional relationship does
     * not cause the entity on the owning side of the relationship to become removed as well.
     *
     * 1) Create BiEntityB_DR(id=1)
     * 2) Create new BiEntityA(id=1), set BiEntityB_DR(id=1)'s entityA Collection to BiEntityA(id=1).
     * 3) Remove BiEntityB_DR(id=1)), verify that BiEntityA(id=1) still exists. Test point passes if it does.
     *
     * If the entity on the owning side of the bidirectional relationship is removed, it should not remove the entity
     * on the inverse of the relationship. Also, since the manyXmany relationship is optional, the relationship
     * field should be set null on fresh instances of the inverse entity from find().
     *
     * 1) Create BiEntityB_DR(id=1)
     * 2) Create new BiEntityA(id=1), set BiEntityB_DR(id=1)'s entityA Collection to BiEntityA(id=1).
     * 3) Remove BiEntityA(id=1)
     * 4) Clear the persistence context
     * 5) Find BiEntityB_DR(id=1). It should still exist, and entityAField should be null.
     *
     * Merge:
     *
     * Merge will attempt to update the managed entity to point to managed versions of entities referenced by
     * the detached entity.
     *
     * 1) Create BiEntityB_DR(id=1)
     * 2) Create new BiEntityA(id=1), set BiEntityB_DR(id=1)'s entityA Collection to BiEntityA(id=1).
     * 3) Clear the persistence context, causing all entities to become detached
     * 4) Perform the following operations:
     * Modify the name field on BiEntityB_DR(id=1)
     * Modify the name field on BiEntityA(id=1)
     * 5) Merge BiEntityB_DR(id=1) into the persistence context.
     * 6) Verify the change to BiEntityB_DR(id=1)'s name field. Verify that BiEntityB_DR(id=1)'s entityAField field references
     * a managed copy of BiEntityA(id=1) and that BiEntityA(id=1)'s name field contains the original value.
     * BiEntityA(id=1)'s name field should not have been changed because EntityManager.merge() was never called
     * on it directly, and merge by default does not cascade across entity relationships.
     *
     * If an entity being merged points to a removed entity, an IllegalArgumentException should be thrown.
     *
     * 1) Create BiEntityB_DR(id=1)
     * 2) Create new BiEntityA(id=1), set BiEntityB_DR(id=1)'s entityA Collection to BiEntityA(id=1).
     * 3) Clear the persistence context
     * 4) In a single transaction, find BiEntityA(id=1) and remove it. This deletes BiEntityA(id=1) from the database.
     * Note: the original detached BiEntityA(id=1) reference still exists, and is referenced by the detached copy of
     * BiEntityB_DR(id=1).
     * 5) Modify the name field of BiEntityB_DR(id=1) and attempt to merge it. An IllegalArgumentException because the
     * detached entity still has a references in its entityAField field to BiEntityA(id=1), which no longer exists in the database.
     *
     * Refresh:
     *
     * Refresh operations are, by default, not cascaded across entity relationships. Without the REFRESH cascade
     * option, a refresh operation will stop at the source entity.
     *
     * 1) Create BiEntityB_DR(id=1)
     * 2) Create new BiEntityA(id=1), set BiEntityB_DR(id=1)'s entityA Collection to BiEntityA(id=1).
     * 3) Start a new transaction
     * 4) With managed copies of the two entities, edit BiEntityB_DR(id=1) and BiEntityB_DR(id=1).BiEntityB_DR(id=1)'s name fields.
     * 5) Invoke EntityManager.refresh() on BiEntityB_DR(id=1)
     * 6) Verify that BiEntityB_DR(id=1)'s name field has been reverted to the value it had when it was created.
     * 7) Verify that BiEntityA(id=1) still has the new value
     */

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_002_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_002_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneBi002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_002_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_002_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneBi002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_002_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_002_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneBi002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_002_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_002_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneBi002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_002_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_002_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneBi002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_002_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_002_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneBi002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B1");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Cascade type overrides are unidirectional, meaning that in a bidirectional relationship, a @ManyToMany relational
     * marker with CascadeType=ALL only applies to the direction of the relationship the tag modifies, and not also
     * to the inverse direction. So therefore, if CascadeType=ALL is applied to the relationship on the owning side,
     * only operations originating on the owning relationship are cascaded to the entity on the inverse side of the
     * relationship. Unless the relational field on the entity of the inverse side is also marked CascadeType=ALL,
     * then operations originating on the inverse entity are not cascaded across to the entity on the owning side of
     * the relationship.
     *
     *
     * Verify cascade:all behavior on the owning side does not override default cascade:none behavior on the inverse side.
     *
     * Persist:
     *
     * Both entities in the relationship need to have persist() invoked on them
     * in order to be stored on the database. By default, persisting the inverse side
     * of the bidirectional relationship does not automatically persist the entity on the owning
     * side, and vice versa.
     *
     * 1) In a new transaction, create an persisted BiEntityB_CA(id=1)
     * and an unpersisted BiEntityA(id=1), adding BiEntityA(id=1) to BiEntityB_CA(id=1)'s EntityA Collection.
     * 2) Commit the transaction. An IllegalStateException should be thrown, because
     * entityAField is referencing an entity that is not managed and does not exist on the database.
     *
     * Remove:
     *
     * By default, removing the entity on the inverse side of the oneXone bidirectional relationship does
     * not cause the entity on the owning side of the relationship to become removed as well.
     *
     * 1) Create BiEntityB_CA(id=1)
     * 2) Create new BiEntityA(id=1), add BiEntityA(id=1) to BiEntityB_CA(id=1)'s EntityA Collection.
     * 3) Remove BiEntityB_CA(id=1)), verify that BiEntityA(id=1) still exists. Test point passes if it does.
     *
     * Merge:
     *
     * Merge will attempt to update the managed entity to point to managed versions of entities referenced by
     * the detached entity.
     *
     * 1) Create BiEntityB_CA(id=1)
     * 2) Create new BiEntityA(id=1), add BiEntityA(id=1) to BiEntityB_CA(id=1)'s EntityA Collection.
     * 3) Clear the persistence context, causing all entities to become detached
     * 4) Perform the following operations:
     * Modify the name field on BiEntityB_CA(id=1)
     * Modify the name field on BiEntityA(id=1)
     * 5) Merge BiEntityB_CA(id=1) into the persistence context.
     * 6) Verify the change to BiEntityB_CA(id=1)'s name field. Verify that BiEntityB_CA(id=1)'s entityAField collection contains
     * a managed copy of BiEntityA(id=1) and that BiEntityA(id=1)'s name field contains the original value.
     * BiEntityA(id=1)'s name field should not have been changed because EntityManager.merge() was never called
     * on it directly, and merge by default does not cascade across entity relationships.
     *
     * Refresh:
     *
     * Refresh operations are, by default, not cascaded across entity relationships. Without the REFRESH cascade
     * option, a refresh operation will stop at the source entity.
     *
     * 1) Create BiEntityB_CA(id=1)
     * 2) Create new BiEntityA(id=1), add BiEntityB_CA(id=1)'s EntityA Collection to BiEntityA(id=1).
     * 3) Start a new transaction
     * 4) With managed copies of the two entities, edit BiEntityB_CA(id=1) and BiEntityB_CA(id=1)->BiEntityA(id=1)'s name fields.
     * 5) Invoke EntityManager.refresh() on BiEntityB_CA(id=1)
     * 6) Verify that BiEntityB_CA(id=1)'s name field has been reverted to the value it had when it was created.
     * 7) Verify that BiEntityA(id=1) still has the new value
     */

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_003_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_003_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneBi003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5CA");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_003_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_003_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneBi003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5CA");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_003_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_003_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneBi003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5CA");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_003_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_003_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneBi003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5CA");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_003_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_003_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneBi003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5CA");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_003_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_003_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneBi003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5CA");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Cascade:Persist declaration verification. (1 POINT)
     *
     * Cascade type overrides are unidirectional, meaning that in a bidirectional relationship, a @ManyToMany relational
     * marker with CascadeType=PERSIST only applies to the direction of the relationship the tag modifies, and not also
     * to the inverse direction. So therefore, if CascadeType=PERSIST is applied to the relationship on the owning side,
     * only operations originating on the owning relationship are cascaded to the entity on the inverse side of the
     * relationship. Unless the relational field on the entity of the inverse side is also marked CascadeType=PERSIST,
     * then operations originating on the inverse entity are not cascaded across to the entity on the owning side of
     * the relationship.
     *
     * Verify cascade:persist behavior on the owning side does not override default cascade:none behavior on the inverse side.
     *
     * Both entities in the relationship need to have persist() invoked on them
     * in order to be stored on the database. By default, persisting the inverse side
     * of the bidirectional relationship does not automatically persist the entity on the owning
     * side, and vice versa.
     *
     * 1) In a new transaction, create an persisted BiEntityB_CP(id=1)
     * and an unpersisted BiEntityA(id=1), add BiEntityA(id=1) to BiEntityB_CP(id=1)'s EntityA Collection.
     * 2) Commit the transaction. An IllegalStateException should be thrown, because
     * the EntityA in BiEntityB_CP is an entity that is not managed and does not exist on the database.
     */

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_004_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_004_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneBi004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5CP");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_004_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_004_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneBi004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5CP");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_004_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_004_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneBi004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5CP");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_004_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_004_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneBi004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5CP");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_004_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_004_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneBi004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5CP");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_004_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_004_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneBi004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5CP");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Cascade:Remove declaration verification.
     *
     * Cascade type overrides are unidirectional, meaning that in a bidirectional relationship, a @ManyToMany relational
     * marker with CascadeType=REMOVE only applies to the direction of the relationship the tag modifies, and not also
     * to the inverse direction. So therefore, if CascadeType=REMOVE is applied to the relationship on the owning side,
     * only operations originating on the owning relationship are cascaded to the entity on the inverse side of the
     * relationship. Unless the relational field on the entity of the inverse side is also marked CascadeType=REMOVE,
     * then operations originating on the inverse entity are not cascaded across to the entity on the owning side of
     * the relationship.
     *
     * Verify cascade:remove behavior on the owning side does not override default cascade:none behavior on the inverse side.
     *
     * By default, removing the entity on the inverse side of the oneXone bidirectional relationship does
     * not cause the entity on the owning side of the relationship to become removed as well.
     *
     * 1) Create BiEntityB_CRM(id=1)
     * 2) Create new BiEntityA(id=1), add BiEntityA(id=1) to BiEntityB_CRM(id=1)'s EntityA Collection.
     * 3) Remove BiEntityB_CRM(id=1)), verify that BiEntityA(id=1) still exists. Test point passes if it does.
     */

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_005_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_005_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneBi005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5RM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_005_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_005_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneBi005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5RM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_005_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_005_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneBi005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5RM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_005_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_005_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneBi005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5RM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_005_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_005_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneBi005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5RM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_005_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_005_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneBi005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5RM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Cascade type overrides are unidirectional, meaning that in a bidirectional relationship, a @ManyToMany relational
     * marker with CascadeType=MERGE only applies to the direction of the relationship the tag modifies, and not also
     * to the inverse direction. So therefore, if CascadeType=MERGE is applied to the relationship on the owning side,
     * only operations originating on the owning relationship are cascaded to the entity on the inverse side of the
     * relationship. Unless the relational field on the entity of the inverse side is also marked CascadeType=MERGE,
     * then operations originating on the inverse entity are not cascaded across to the entity on the owning side of
     * the relationship.
     *
     * Verify cascade:merge behavior on the owning side does not override default cascade:none behavior on the inverse side.
     *
     * Merge will attempt to update the managed entity to point to managed versions of entities referenced by
     * the detached entity.
     *
     * 1) Create BiEntityB_CM(id=1)
     * 2) Create new BiEntityA(id=1), add BiEntityA(id=1) to BiEntityB_CM(id=1)'s EntityA Collection.
     * 3) Clear the persistence context, causing all entities to become detached
     * 4) Perform the following operations:
     * Modify the name field on BiEntityB_CM(id=1)
     * Modify the name field on BiEntityA(id=1)
     * 5) Merge BiEntityB_CM(id=1) into the persistence context.
     * 6) Verify the change to BiEntityB_CM(id=1)'s name field. Verify that BiEntityB_CM(id=1)'s entityAField field references
     * a managed copy of BiEntityA(id=1) and that BiEntityA(id=1)'s name field contains the original value.
     * BiEntityA(id=1)'s name field should not have been changed because EntityManager.merge() was never called
     * on it directly, and merge by default does not cascade across entity relationships.
     */

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_006_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_006_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneBi006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5CM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_006_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_006_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneBi006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5CM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_006_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_006_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneBi006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5CM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_006_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_006_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneBi006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5CM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_006_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_006_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneBi006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5CM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_006_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_006_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneBi006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5CM");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Cascade:refresh declaration verification.
     *
     * Cascade type overrides are unidirectional, meaning that in a bidirectional relationship, a @ManyToMany relational
     * marker with CascadeType=REFRESH only applies to the direction of the relationship the tag modifies, and not also
     * to the inverse direction. So therefore, if CascadeType=REFRESH is applied to the relationship on the owning side,
     * only operations originating on the owning relationship are cascaded to the entity on the inverse side of the
     * relationship. Unless the relational field on the entity of the inverse side is also marked CascadeType=REFRESH,
     * then operations originating on the inverse entity are not cascaded across to the entity on the owning side of
     * the relationship.
     *
     * Verify cascade:refresh behavior on the owning side does not override default cascade:none behavior on the inverse side.
     *
     * Refresh operations are, by default, not cascaded across entity relationships. Without the REFRESH cascade
     * option, a refresh operation will stop at the source entity.
     *
     * 1) Create BiEntityB_CRF(id=1)
     * 2) Create new BiEntityA(id=1), add BiEntityA(id=1) to BiEntityB_CRF(id=1)'s EntityA Collection.
     * 3) Start a new transaction
     * 4) With managed copies of the two entities, edit BiEntityB_B5RF(id=1) and BiEntityB_CRF(id=1)->BiEntityA(id=1)'s name fields.
     * 5) Invoke EntityManager.refresh() on BiEntityB_CRF(id=1)
     * 6) Verify that BiEntityB_CRF(id=1)'s name field has been reverted to the value it had when it was created.
     * 7) Verify that BiEntityA(id=1) still has the new value
     */

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_007_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_007_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneBi007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5RF");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_007_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_007_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneBi007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5RF");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_007_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_007_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneBi007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5RF");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_007_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_007_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneBi007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5RF");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_007_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_007_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneBi007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B5RF");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_007_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_007_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneBi007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B5RF");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Verify that relational field LAZY fetch behavior on the owning side of the relationship does not cause
     * the inverse side of the bidirectional relationship to default to LAZY (fetch type overrides are unidirectional.)
     *
     * Test verifies function performing the following:
     * 1) Create new BiEntityB_B4(id=1)
     * 2) Create new BiEntityA(id=1).
     * 3) Create a OneXOne bidirectional relationship between BiEntityA(id=1).B4 and
     * BiEntityB_B4(id=1).
     * 4) Clear the persistence context, forcing all entities to become detached.
     * 5) Acquire a fresh, managed copy of BiEntityA(id=1) by invoking
     * find BiEntityB_B4(id=1) and access BiEntityB_B4(id=1).entityAField's name field. The
     * data in BiEntityA(id=1) should be accessible. TranScoped PC's will need
     * to take special care to perform this work under a live persistence context
     * (ie, find and data retrieval operations are in the same transaction)
     * 6) Clear the persistence context again, forcing all entties to become detached.
     * 7) Acquire a new copy of BiEntityB_B4(id=1), and clear the persistence context again
     * BEFORE attempting to access any fields. This forces the entity to become detached.
     * 8) Access BiEntityB_B4(id=1).entityAField's name field. It should be available since
     * the entityAField relational field was never marked with fetch=FetchMode.LAZY. The
     * fact that BiEntityA(id=1).b4 is marked LAZY should not make its partner field on
     * the inverse entity, entityAField, lazy.
     */

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_008_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_008_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneBi008";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B4");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_008_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_008_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneBi008";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B4");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_008_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_008_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneBi008";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B4");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_008_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_008_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneBi008";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B4");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_008_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_008_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneBi008";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOBiEntA");
        properties.put("EntityBName", "OOBiEntB_B4");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Bidirectional_Inverse_008_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Bidirectional_Inverse_008_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneBi008";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOBiEntA");
        properties.put("EntityBName", "XMLOOBiEntB_B4");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

}
