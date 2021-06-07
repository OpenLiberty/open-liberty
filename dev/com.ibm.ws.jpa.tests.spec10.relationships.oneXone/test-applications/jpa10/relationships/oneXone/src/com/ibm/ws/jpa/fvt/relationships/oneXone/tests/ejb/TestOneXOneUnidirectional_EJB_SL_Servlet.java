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
@WebServlet(urlPatterns = "/TestOneXOneUnidirectional_EJB_SL_Servlet")
public class TestOneXOneUnidirectional_EJB_SL_Servlet extends EJBTestVehicleServlet {
    private final String testLogicClassName = "com.ibm.ws.jpa.fvt.relationships.oneXone.testlogic.OneXOneUnidirectionalTestLogic";

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    private final static String ejbJNDIName = "ejb/OneXOneSLEJB";

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OneXOne_Uni_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OneXOne_Uni_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OneXOne_Uni_CMTS"));
    }

    /*
     * Verify basic One-to-One service by creating UniEntityA and UniEntityB, and
     * link UniEntityA.defaultRelationship to the instance of UniEntityB.
     *
     * Test verifies function performing the following:
     * 1) Create new UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting defaultRelationship to UniEntityB(id=1).
     * 3) Keep other (optional) OneXOne fields set to null.
     * 4) Clear persistence context (if necessary), and find UniEntityA(id=1).
     * 5) Verify field data in UniEntityA(id=1)
     * 6) Verify field data in UniEntityA.defaultRelationship (should reference UniEntityB(id=1))
     * 7) Test passes if all these conditions are met.
     *
     * This test case confirms the following function verification:
     * 1) Optional relational fields can be set to null without persistence errors
     * (all fields except defaultRelationship are null in UniEntityA(id=1))
     * 2) A traversable OneXOne association between UniEntityA to UniEntityB(id=1) is
     * established, and UniEntityB is accessible through the relational
     * reference defaultRelationship in UniEntityA.
     * 3) The default FetchMode, EAGER, for defaultRelationship should make all of UniEntityB(id=1)'s
     * data available for access, even after the entities have been detached
     * from the persistence context.
     */

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_001_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_001_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_001_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_001_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_001_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_001_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_001_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_001_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_001_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_001_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_001_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_001_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Verify JoinColumn table name annotation
     *
     * Test verifies function performing the following:
     * 1) Create new UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting overrideColumnNameRelationship to UniEntityB(id=1).
     * 3) Keep other (optional) OneXOne fields set to null.
     * 4) Clear persistence context (if necessary), and find UniEntityA(id=1).
     * 5) Verify field data in UniEntityA(id=1)
     * 6) Verify field data in UniEntityA.b2 (should reference UniEntityB(id=1))
     * 7) Test passes if all these conditions are met.
     *
     * This test case is virtually idential to testOneXOneUni001, only that the target
     * field used is marked with a column-name override in the JoinColumn annotation.
     */

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_002_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_002_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_002_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_002_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_002_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_002_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_002_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_002_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_002_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_002_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_002_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_002_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni002";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

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
     * 1) Create a new UniEntityANoOpt(id=1), setting all relational fields to null
     * 2) A persistence exception should be thrown while trying to persist UniEntityA(id=1)
     * to the database. Test Point passes if an exception is thrown on transaction commit.
     * - Verify that an existing entity with a non-optional field cannot change the optional field
     * to a null value and save it to the database.
     * 1) Create a new Create new UniEntityB(id=1)
     * 2) Create a new UniEntityANoOpt(id=1), setting b to UniEntityB(id=1). Save to the database
     * (not expecting any exceptions)
     * 3) Clear the persistence context, find UniEntityANoOpt(id=1)
     * 4) Set the b field on the UniEntityANoOpt(id=1) returned by find() to null. An exception
     * should be thrown when the transaction intended to save the changes to the db attempts to
     * commit. Test point passes if an exception is thrown.
     */

//    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_003_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_003_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OONoOptEntityA");
        properties.put("EntityBName", "OONoOptEntityB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

//    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_003_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_003_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOONoOptEntityA");
        properties.put("EntityBName", "XMLOONoOptEntityB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

//    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_003_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_003_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OONoOptEntityA");
        properties.put("EntityBName", "OONoOptEntityB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

//    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_003_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_003_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOONoOptEntityA");
        properties.put("EntityBName", "XMLOONoOptEntityB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

//    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_003_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_003_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OONoOptEntityA");
        properties.put("EntityBName", "OONoOptEntityB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

//    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_003_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_003_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni003";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOONoOptEntityA");
        properties.put("EntityBName", "XMLOONoOptEntityB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Verify relational field LAZY fetch behavior.
     *
     * Test verifies function performing the following:
     * 1) Create new UniEntityB(id=1)
     * 3) Create new UniEntityA(id=1), setting Lazy to UniEntity(id=1)].
     * Keep other (optional) OneXOne fields set to null.
     * 4) Clear the persistence context, forcing all entities to become detached.
     * 5) Acquire a fresh, managed copy of UniEntityA(id=1) by invoking
     * find UniEntityA(id=1) and access UniEntityA(id=1).Lazy's name field. The
     * data in UniEntityB(id=1) should be accessible. TranScoped PC's will need
     * to take special care to perform this work under a live persistence context
     * (ie, find and data retrieval operations are in the same transaction)
     * 6) Clear the persistence context again, forcing all entties to become detached.
     * 7) Acquire a new copy of UniEntityA(id=1), and clear the persistence context again
     * BEFORE attempting to access any fields. This forces the entity to become detached.
     * 6) The data should not be available since Lazy was never accessed.
     * 7) Try to access Lazy, which should be null because it is lazy loaded, and was never
     * accessed before the entity was detached.
     *
     * Test passes if :
     * 1) UniEntityB(id=1)'s name field could be accessed while the entity
     * is not detached
     * 2) If UniEntityB(id=1)'s name field could NOT be accessed if the entity was
     * never previously accessed before becomming a detached entity.
     */

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_004_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_004_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_004_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_004_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_004_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_004_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_004_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_004_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_004_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_004_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_004_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_004_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

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
     * 1) In a new transaction, create an unpersisted UniEntityB(id=1).
     * and a persisted UniEntityA(id=1), setting defaultRelationship to UniEntityB(id=1).
     * 2) Commit the transaction. An IllegalStateException should be thrown, because
     * b1 is referencing an entity that is not managed and does not exist on the database.
     *
     * Remove:
     *
     * By default, removing the entity on the owning side of the oneXone relationship does
     * not cause the entity on the inverse side of the relationship to become removed as well.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting defaultRelationship to UniEntityB(id=1).
     * 3) Remove UniEntityA(id=1), verify that UniEntityB(id=1) still exists. Test point passes if it does.
     *
     * If the entity on the inverse side of the relationship is removed, it should not remove the entity
     * on the owning of the relationship. Also, since the oneXone relationship is optional, the relationship
     * field should be set null on fresh instances of the owning entity from find().
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting defaultRelationship to UniEntityB(id=1).
     * 3) Remove UniEntityB(id=1)
     * 4) Clear the persistence context
     * 5) Find UniEntityA(id=1). It should still exist (especially given that it is a unidirectional
     * relationship), and defaultRelationship should be null.
     *
     * Merge:
     *
     * Merge will attempt to update the managed entity to point to managed versions of entities referenced by
     * the detached entity.
     *
     * 1) Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting defaultRelationship to UniEntityB(id=1).
     * 3) Clear the persistence context, causing all entities to become detached
     * 4) Perform the following operations:
     * Modify the name field on UniEntityB(id=1)
     * Modify the name field on UniEntityA(id=1)
     * 5) Merge UniEntityA(id=1) into the persistence context.
     * 6) Verify the change to UniEntityA(id=1)'s name field. Verify that UniEntityA(id=1)'s defaultRelationship field references
     * a managed copy of UniEntityB(id=1) and that UniEntityB(id=1)'s name field contains the original value.
     * UniEntityB(id=1)'s name field should not have been changed because EntityManager.merge() was never called
     * on it directly, and merge by default does not cascade across entity relationships.
     *
     * If an entity being merged points to a removed entity, an IllegalArgumentException should be thrown.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting defaultRelationship to UniEntityB(id=1).
     * 3) Clear the persistence context
     * 4) In a single transaction, find UniEntityB(id=1) and remove it. This deletes UniEntityB(id=1) from the database.
     * Note: the original detached UniEntityB(id=1) reference still exists, and is referenced by the detached copy of
     * UniEntityA(id=1).
     * 5) Modify the name field of UniEntityA(id=1) and attempt to merge it. An IllegalArgumentException because the
     * detached entity still has a references in its defaultRelationship field to UniEntityB(id=1), which no longer exists in the database.
     *
     * Lazy loaded relationships, if not triggered while the entity was managed, are ignored during merge().
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting lazy to UniEntityB(id=1).
     * 3) Clear the persistence context, causing all entities to become detached
     * 4) Find UniEntityA(id=1), but do not access the lazy field.
     * 5) Clear the persistence context, causing all entities to become detached
     * 6) Modify UniEntityA(id=1)'s name field, and set lazy to null.
     * 7) Merge UniEntityA(id=1) into the persistence context. The field lazy should still refer to UniEntityB(id=1) because
     * it was never triggered on the original entity before it became detached.
     *
     * On the other hand, if a lazy-loaded relationship is loaded before the entity is detached, then changes to that
     * relationship are merged.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting lazy to UniEntityB(id=1).
     * 3) Clear the persistence context, causing all entities to become detached
     * 4) Find UniEntityA(id=1), and access the lazy field, loading the entity on the inverse side of the relationship
     * into the persistence context.
     * 5) Clear the persistence context, causing all entities to become detached
     * 6) Modify UniEntityA(id=1)'s name field, and set lazy to null.
     * 7) Merge UniEntityA(id=1) into the persistence context. The field lazy should now be null since lazy was accessed
     * before the entity became detached.
     *
     * Refresh:
     *
     * Refresh operations are, by default, not cascaded across entity relationships. Without the REFRESH cascade
     * option, a refresh operation will stop at the soure tntity.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting defaultRelationship to UniEntityB(id=1).
     * 3) Start a new transaction
     * 4) With managed copies of the two entities, edit UniEntityA(id=1) and UniEntityA(id=1).defaultRelationship's name fields.
     * 5) Invoke EntityManager.refresh() on UniEntityA(id=1)
     * 6) Verify that UniEntityA(id=1)'s name field has been reverted to the value it had when it was created.
     * 7) Verify that UniEntityB(id=1) still has the new value
     */

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Unidirectional_005_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_005_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Unidirectional_005_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_005_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_005_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_005_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_005_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_005_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Unidirectional_005_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_005_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Unidirectional_005_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_005_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

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
     * entity relationship. This means if UniEntityA is the target if an
     * EntityManager.persist() operation, the JPA implementation will automatically
     * invoke EntityManager.persist() on all of the UniEntityB entity relationships
     * that are marked with Cascade type ALL (or PERSIST).
     *
     * 1) Create an unpersisted UniEntityB(id=1)
     * 2) Create an unpersisted UniEntityA(id=1)
     * 3) Set CascadeAll to UniEntityB(id=1).
     * 4) Start a new transaction, persist UniEntityA(id=1), and commit the transaction.
     * 5) Clear persistence context, forcing all entities to become detached.
     * 6) Find UniEntityA(id=1), this should hit the database, returning an entity
     * with a reference in CascadeAll to UniEntityB(id=1) because the persist operation
     * was cascaded.
     *
     * Remove:
     *
     * With cascade type set to ALL, remove operations are cascaded across the entity
     * relationship. This means if UniEntityA is the target of an EntityManager.remove()
     * operation, the JPA implementation will automatically invoke EntityManager.remove()
     * on all of the UniEntityB entity relationships that are marked with Cascade type ALL
     * (or REMOVE).
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting CascadeAll to UniEntityB(id=1).
     * 3) Remove UniEntityA(id=1), verify that UniEntityB(id=1) also no longer exists. Test point passes if true.
     *
     * Merge:
     *
     * With cascade type set to ALL, merge operations will cascade across the entity relationship. So changes to
     * the target entity (UniEntityA(id=1)) and changes to all entities with relationship cascade attributes of
     * ALL (and MERGE) will be merged into the persistence context.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting CascadeAll to UniEntityB(id=1).
     * 3) Clear the persistence context, causing all entities to become detached
     * 4) Modify UniEntityA(id=1)'s name field. Modify UniEntityB(id=1)'s name field.
     * 5) Merge UniEntityA(id=1) into the persistence context. Examine the UniEntityB(id=1) referenced by the CascadeAll
     * field of the entity returned from the merge() operation. Both entities should be managed, and both
     * entities should contain the changes from step 4.
     *
     * Refresh:
     *
     * with cascade type set to ALL, refresh operations will cascade across entity relationships. So changes
     * to the target entity (UniEntityA(id=1)) and changes to all entities with relationship cascade attributes of
     * ALL (and REFRESH) will have their contents reset to match the data in the database.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting CascadeAll to UniEntityB(id=1).
     * 3) Start a new transaction
     * 4) With managed copies of the two entities, edit UniEntityA(id=1) and UniEntityA(id=1).CascadeAll's name fields.
     * 5) Invoke EntityManager.refresh() on UniEntityA(id=1)
     * 6) Verify that UniEntityA(id=1)'s name field has been reverted to the value it had when it was created.
     * 7) Verify that UniEntityB(id=1)'s name field has been reverted to the value it had when it was created.
     */

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_006_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_006_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_006_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_006_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_006_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_006_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_006_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_006_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_006_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_006_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_006_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_006_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni006";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

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
     * entity relationship. This means if UniEntityA is the target if an
     * EntityManager.persist() operation, the JPA implementation will automatically
     * invoke EntityManager.persist() on all of the UniEntityB entity relationships
     * that are marked with Cascade type PERSIST.
     *
     * 1) Create an unpersisted UniEntityB(id=1)
     * 2) Create an unpersisted UniEntityA(id=1)
     * 3) Set CascadePersist to UniEntityB(id=1).
     * 4) Start a new transaction, persist UniEntityA(id=1), and commit the transaction.
     * 5) Clear persistence context, forcing all entities to become detached.
     * 6) Find UniEntityA(id=1), this should hit the database, returning an entity
     * with a reference in CascadePersist to UniEntityB(id=1) because the persist operation
     * was cascaded.
     */

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_007_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_007_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_007_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_007_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_007_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_007_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_007_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_007_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_007_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_007_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_007_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_007_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni007";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

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
     * relationship. This means if UniEntityA is the target of an EntityManager.remove()
     * operation, the JPA implementation will automatically invoke EntityManager.remove()
     * on all of the UniEntityB entity relationships that are marked with Cascade type REMOVE.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting CascadeRemove to UniEntityB(id=1).
     * 3) Remove UniEntityA(id=1), verify that UniEntityB(id=1) also no longer exists. Test point passes if true.
     */

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_008_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_008_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni008";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_008_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_008_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni008";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_008_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_008_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni008";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_008_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_008_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni008";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_008_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_008_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni008";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_008_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_008_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni008";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

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
     * the target entity (UniEntityA(id=1)) and changes to all entities with relationship cascade attributes of
     * MERGE will be merged into the persistence context.
     *
     * 1) Create Create UniEntityB(id=1)
     * 2) Create new UniEntityA(id=1), setting CascadeMerge to UniEntityB(id=1).
     * 3) Clear the persistence context, causing all entities to become detached
     * 4) Modify UniEntityA(id=1)'s name field. Modify UniEntityB(id=1)'s name field.
     * 5) Merge UniEntityA(id=1) into the persistence context. Examine the UniEntityB(id=1) referenced by CascadeMerge
     * field of the entity returned from the merge() operation. Both entities should be managed, and both
     * entities should contain the changes from step 4.
     */

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_009_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_009_Ano_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni009";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_009_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_009_XML_AMJTA_SL_EJB";
        final String testMethod = "testOneXOneUni009";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_009_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_009_Ano_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni009";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_009_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_009_XML_AMRL_SL_EJB";
        final String testMethod = "testOneXOneUni009";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_009_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_009_Ano_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni009";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOUniEntA");
        properties.put("EntityBName", "OOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_009_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_009_XML_CMTS_SL_EJB";
        final String testMethod = "testOneXOneUni009";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOUniEntA");
        properties.put("EntityBName", "XMLOOUniEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    /*
     * Cardinality Test: Verify that more then one entity on the owning side of the relationship can have a relationship
     * with a specific instance of an entity on the inverse side of the relationship.
     *
     * 1) Create UniEntityB(id=1), save to database.
     * 2) Create new UniEntityA(id=1), setting defaultRelationship to UniEntityB(id=1), and save to database.
     * 3) Create new UniEntityA(id=2), setting defaultRelationship to UniEntityB(id=1), and save to database.
     * 4) Clear the persistence context, and verify that both entities' database state. Test passes if both have a
     * relationship in the defaultRelationship field with UniEntityB(id=1).
     */

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Unidirectional_Test_Cardinality_Ano_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_Test_Cardinality_Ano_AMJTA_SL_EJB";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOCardEntA");
        properties.put("EntityBName", "OOCardEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Unidirectional_Test_Cardinality_XML_AMJTA_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_Test_Cardinality_XML_AMJTA_SL_EJB";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOCardEntA");
        properties.put("EntityBName", "XMLOOCardEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_Test_Cardinality_Ano_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_Test_Cardinality_Ano_AMRL_SL_EJB";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOCardEntA");
        properties.put("EntityBName", "OOCardEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXOne_Unidirectional_Test_Cardinality_XML_AMRL_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_Test_Cardinality_XML_AMRL_SL_EJB";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOCardEntA");
        properties.put("EntityBName", "XMLOOCardEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Unidirectional_Test_Cardinality_Ano_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_Test_Cardinality_Ano_CMTS_SL_EJB";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OOCardEntA");
        properties.put("EntityBName", "OOCardEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Relationships_OneXOne_Unidirectional_Test_Cardinality_XML_CMTS_SL_EJB() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_Unidirectional_Test_Cardinality_XML_CMTS_SL_EJB";
        final String testMethod = "testCardinality001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOOCardEntA");
        properties.put("EntityBName", "XMLOOCardEntB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }
}
