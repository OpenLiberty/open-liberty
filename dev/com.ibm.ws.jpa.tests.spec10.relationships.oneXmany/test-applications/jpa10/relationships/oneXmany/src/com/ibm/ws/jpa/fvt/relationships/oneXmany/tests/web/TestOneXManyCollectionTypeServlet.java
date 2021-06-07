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

package com.ibm.ws.jpa.fvt.relationships.oneXmany.tests.web;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.relationships.oneXmany.testlogic.OneXManyCollectionTypeTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOneXManyCollectionTypeServlet")
public class TestOneXManyCollectionTypeServlet extends JPATestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "OneXMany_Collection_JTA")
    private EntityManager oxmCollectionEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "OneXMany_Collection_JTA")
    private EntityManagerFactory oxmCollectionEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "OneXMany_Collection_RL")
    private EntityManagerFactory oxmCollectionEmfRL;

    private final String testLogicClassName = OneXManyCollectionTypeTestLogic.class.getName();

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "oxmCollectionEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "oxmCollectionEmfRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "oxmCollectionEm"));
    }

    /*
     * Executes test for OneXMany relationship using a Collection-type.
     *
     * Test Algorithm:
     * 1) Start a new transaction
     * 2) Create a new IContainerTypeEntityA(id=1)
     * 3) Create new instances of IContainerTypeEntityB
     * 4) Add all IContainerTypeEntityB's to the genericizedCollectionType field in IContainerTypeEntityA(id=1)
     * 5) Commit the transaction
     * 6) Clear the persistence context, and fetch genericizedCollectionType collection from the database
     * 7) Verify that each IContainerTypeEntityB is present in the collection
     *
     * Test passes if all steps execute correctly.
     */

    @Test
    public void jpa10_Relationships_OneXMany_CollectionType_GENERIC_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CollectionType_GENERIC_001_Ano_AMJTA_Web";
        final String testMethod = "testCollectionType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericCollection", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CollectionType_GENERIC_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CollectionType_GENERIC_001_XML_AMJTA_Web";
        final String testMethod = "testCollectionType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericCollection", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CollectionType_GENERIC_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CollectionType_GENERIC_001_Ano_AMRL_Web";
        final String testMethod = "testCollectionType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericCollection", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CollectionType_GENERIC_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CollectionType_GENERIC_001_XML_AMRL_Web";
        final String testMethod = "testCollectionType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericCollection", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CollectionType_GENERIC_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CollectionType_GENERIC_001_Ano_CMTS_Web";
        final String testMethod = "testCollectionType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericCollection", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CollectionType_GENERIC_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CollectionType_GENERIC_001_XML_CMTS_Web";
        final String testMethod = "testCollectionType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericCollection", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CollectionType_NONGENERIC_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CollectionType_NONGENERIC_001_Ano_AMJTA_Web";
        final String testMethod = "testCollectionType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericCollection", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CollectionType_NONGENERIC_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CollectionType_NONGENERIC_001_XML_AMJTA_Web";
        final String testMethod = "testCollectionType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericCollection", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CollectionType_NONGENERIC_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CollectionType_NONGENERIC_001_Ano_AMRL_Web";
        final String testMethod = "testCollectionType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericCollection", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CollectionType_NONGENERIC_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CollectionType_NONGENERIC_001_XML_AMRL_Web";
        final String testMethod = "testCollectionType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericCollection", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CollectionType_NONGENERIC_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CollectionType_NONGENERIC_001_Ano_CMTS_Web";
        final String testMethod = "testCollectionType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericCollection", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CollectionType_NONGENERIC_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CollectionType_NONGENERIC_001_XML_CMTS_Web";
        final String testMethod = "testCollectionType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericCollection", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    /*
     * Executes test for OneXMany relationship using a Set-type.
     *
     * Test Algorithm:
     * 1) Start a new transaction
     * 2) Create a new IContainerTypeEntityA(id=1)
     * 3) Create new instances of IContainerTypeEntityB
     * 4) Add all IContainerTypeEntityB's to the (un)genericizedSetType field in IContainerTypeEntityA(id=1)
     * 5) Commit the transaction
     * 6) Clear the persistence context, and fetch (un)genericizedSetType set from the database
     * 7) Verify that each IContainerTypeEntityB is present in the set
     *
     * Test passes if all steps execute correctly.
     */

    @Test
    public void jpa10_Relationships_OneXMany_SetType_GENERIC_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_SetType_GENERIC_001_Ano_AMJTA_Web";
        final String testMethod = "testSetType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericSet", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_SetType_GENERIC_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_SetType_GENERIC_001_XML_AMJTA_Web";
        final String testMethod = "testSetType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericSet", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_SetType_GENERIC_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_SetType_GENERIC_001_Ano_AMRL_Web";
        final String testMethod = "testSetType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericSet", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_SetType_GENERIC_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_SetType_GENERIC_001_XML_AMRL_Web";
        final String testMethod = "testSetType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericSet", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_SetType_GENERIC_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_SetType_GENERIC_001_Ano_CMTS_Web";
        final String testMethod = "testSetType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericSet", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_SetType_GENERIC_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_SetType_GENERIC_001_XML_CMTS_Web";
        final String testMethod = "testSetType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericSet", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_SetType_NONGENERIC_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_SetType_NONGENERIC_001_Ano_AMJTA_Web";
        final String testMethod = "testSetType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericSet", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_SetType_NONGENERIC_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_SetType_NONGENERIC_001_XML_AMJTA_Web";
        final String testMethod = "testSetType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericSet", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_SetType_NONGENERIC_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_SetType_NONGENERIC_001_Ano_AMRL_Web";
        final String testMethod = "testSetType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericSet", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_SetType_NONGENERIC_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_SetType_NONGENERIC_001_XML_AMRL_Web";
        final String testMethod = "testSetType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericSet", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_SetType_NONGENERIC_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_SetType_NONGENERIC_001_Ano_CMTS_Web";
        final String testMethod = "testSetType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericSet", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_SetType_NONGENERIC_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_SetType_NONGENERIC_001_XML_CMTS_Web";
        final String testMethod = "testSetType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericSet", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    /*
     * Executes test for OneXMany relationship using a List-type.
     *
     * Test Algorithm:
     * 1) Start a new transaction
     * 2) Create a new IContainerTypeEntityA(id=1)
     * 3) Create new instances of IContainerTypeEntityB
     * 4) Add all IContainerTypeEntityB's to the (un)genericizedListType field in IContainerTypeEntityA(id=1)
     * 5) Commit the transaction
     * 6) Clear the persistence context, and fetch (un)genericizedListType set from the database
     * 7) Verify that each IContainerTypeEntityB is present in the set
     *
     * Test passes if all steps execute correctly.
     */

    @Test
    public void jpa10_Relationships_OneXMany_ListType_GENERIC_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_GENERIC_001_Ano_AMJTA_Web";
        final String testMethod = "testListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericList", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_ListType_GENERIC_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_GENERIC_001_XML_AMJTA_Web";
        final String testMethod = "testListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericList", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_ListType_GENERIC_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_GENERIC_001_Ano_AMRL_Web";
        final String testMethod = "testListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericList", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_ListType_GENERIC_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_GENERIC_001_XML_AMRL_Web";
        final String testMethod = "testListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericList", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_ListType_GENERIC_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_GENERIC_001_Ano_CMTS_Web";
        final String testMethod = "testListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericList", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_ListType_GENERIC_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_GENERIC_001_XML_CMTS_Web";
        final String testMethod = "testListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericList", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_ListType_NONGENERIC_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_NONGENERIC_001_Ano_AMJTA_Web";
        final String testMethod = "testListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericList", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_ListType_NONGENERIC_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_NONGENERIC_001_XML_AMJTA_Web";
        final String testMethod = "testListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericList", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_ListType_NONGENERIC_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_NONGENERIC_001_Ano_AMRL_Web";
        final String testMethod = "testListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericList", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_ListType_NONGENERIC_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_NONGENERIC_001_XML_AMRL_Web";
        final String testMethod = "testListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericList", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_ListType_NONGENERIC_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_NONGENERIC_001_Ano_CMTS_Web";
        final String testMethod = "testListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericList", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_ListType_NONGENERIC_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_NONGENERIC_001_XML_CMTS_Web";
        final String testMethod = "testListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericList", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    /*
     * Executes test for OneXMany relationship using a List-type with name-field ordering.
     *
     * Test Algorithm:
     * 1) Start a new transaction
     * 2) Create a new IContainerTypeEntityA(id=1)
     * 3) Create several new instance of IContainerTypeEntityB(id=1 through 100). For each, generate
     * a string of random length with random characters.
     * 4) Add each IContainerTypeEntityB to the orderedListType field in IContainerTypeEntityA(id=1)
     * 5) Commit the transaction
     * 6) Clear the persistence context, and fetch IContainerTypeEntityA(id=1) from the database
     * 7) Fetch the instance of List from IContainerTypeEntityA(id=1).genericizedListType
     * 8) Iterate through the List obtained in step 7. All of the entries should be sorted by
     * IContainerTypeEntityB.name in ascending order.
     *
     * Test passes if all steps execute correctly.
     */

    @Test
    public void jpa10_Relationships_OneXMany_ListType_Ordered_GENERIC_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_Ordered_GENERIC_001_Ano_AMJTA_Web";
        final String testMethod = "testOrderedListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_ListType_Ordered_GENERIC_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_Ordered_GENERIC_001_XML_AMJTA_Web";
        final String testMethod = "testOrderedListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_ListType_Ordered_GENERIC_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_Ordered_GENERIC_001_Ano_AMRL_Web";
        final String testMethod = "testOrderedListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_ListType_Ordered_GENERIC_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_Ordered_GENERIC_001_XML_AMRL_Web";
        final String testMethod = "testOrderedListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_ListType_Ordered_GENERIC_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_Ordered_GENERIC_001_Ano_CMTS_Web";
        final String testMethod = "testOrderedListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_ListType_Ordered_GENERIC_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_Ordered_GENERIC_001_XML_CMTS_Web";
        final String testMethod = "testOrderedListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_ListType_Ordered_NONGENERIC_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_Ordered_NONGENERIC_001_Ano_AMJTA_Web";
        final String testMethod = "testOrderedListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_ListType_Ordered_NONGENERIC_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_Ordered_NONGENERIC_001_XML_AMJTA_Web";
        final String testMethod = "testOrderedListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_ListType_Ordered_NONGENERIC_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_Ordered_NONGENERIC_001_Ano_AMRL_Web";
        final String testMethod = "testOrderedListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_ListType_Ordered_NONGENERIC_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_Ordered_NONGENERIC_001_XML_AMRL_Web";
        final String testMethod = "testOrderedListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_ListType_Ordered_NONGENERIC_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_Ordered_NONGENERIC_001_Ano_CMTS_Web";
        final String testMethod = "testOrderedListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_ListType_Ordered_NONGENERIC_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_ListType_Ordered_NONGENERIC_001_XML_CMTS_Web";
        final String testMethod = "testOrderedListType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    /*
     * Executes test for OneXMany relationship using a Map-type.
     *
     * Test Algorithm:
     * 1) Start a new transaction
     * 2) Create a new IContainerTypeEntityA(id=1)
     * 3) Create new instances of IContainerTypeEntityB
     * 4) Add all IContainerTypeEntityB's to the (un)genericizedMapType field in IContainerTypeEntityA(id=1)
     * 5) Commit the transaction
     * 6) Clear the persistence context, and fetch (un)genericizedMapType set from the database
     * 7) Verify that each IContainerTypeEntityB is present in the set
     *
     * Test passes if all steps execute correctly.
     */

    @Test
    public void jpa10_Relationships_OneXMany_MapType_GENERIC_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_GENERIC_001_Ano_AMJTA_Web";
        final String testMethod = "testMapType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericMap", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_MapType_GENERIC_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_GENERIC_001_XML_AMJTA_Web";
        final String testMethod = "testMapType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericMap", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_MapType_GENERIC_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_GENERIC_001_Ano_AMRL_Web";
        final String testMethod = "testMapType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericMap", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_MapType_GENERIC_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_GENERIC_001_XML_AMRL_Web";
        final String testMethod = "testMapType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericMap", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_MapType_GENERIC_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_GENERIC_001_Ano_CMTS_Web";
        final String testMethod = "testMapType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericMap", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_MapType_GENERIC_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_GENERIC_001_XML_CMTS_Web";
        final String testMethod = "testMapType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericMap", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

//    @Test
    public void jpa10_Relationships_OneXMany_MapType_NONGENERIC_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_NONGENERIC_001_Ano_AMJTA_Web";
        final String testMethod = "testMapType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericMap", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

//    @Test
    public void jpa10_Relationships_OneXMany_MapType_NONGENERIC_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_NONGENERIC_001_XML_AMJTA_Web";
        final String testMethod = "testMapType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericMap", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

//    @Test
    public void jpa10_Relationships_OneXMany_MapType_NONGENERIC_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_NONGENERIC_001_Ano_AMRL_Web";
        final String testMethod = "testMapType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericMap", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

//    @Test
    public void jpa10_Relationships_OneXMany_MapType_NONGENERIC_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_NONGENERIC_001_XML_AMRL_Web";
        final String testMethod = "testMapType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericMap", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

//    @Test
    public void jpa10_Relationships_OneXMany_MapType_NONGENERIC_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_NONGENERIC_001_Ano_CMTS_Web";
        final String testMethod = "testMapType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericMap", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

//    @Test
    public void jpa10_Relationships_OneXMany_MapType_NONGENERIC_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_NONGENERIC_001_XML_CMTS_Web";
        final String testMethod = "testMapType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericMap", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    /*
     * Executes test for OneXMany relationship using a Map-type.
     *
     * Test Algorithm:
     * 1) Start a new transaction
     * 2) Create a new IContainerTypeEntityA(id=1)
     * 3) Create new instances of IContainerTypeEntityB
     * 4) Add all IContainerTypeEntityB's to the (un)genericizedMapType field in IContainerTypeEntityA(id=1)
     * 5) Commit the transaction
     * 6) Clear the persistence context, and fetch (un)genericizedMapType set from the database
     * 7) Verify that each IContainerTypeEntityB is present in the set
     *
     * Test passes if all steps execute correctly.
     */

    @Test
    public void jpa10_Relationships_OneXMany_MapType_WithKey_GENERIC_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_WithKey_GENERIC_001_Ano_AMJTA_Web";
        final String testMethod = "testMapWithKeyType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericMap", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_MapType_WithKey_GENERIC_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_WithKey_GENERIC_001_XML_AMJTA_Web";
        final String testMethod = "testMapWithKeyType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericMap", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_MapType_WithKey_GENERIC_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_WithKey_GENERIC_001_Ano_AMRL_Web";
        final String testMethod = "testMapWithKeyType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericMap", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_MapType_WithKey_GENERIC_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_WithKey_GENERIC_001_XML_AMRL_Web";
        final String testMethod = "testMapWithKeyType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericMap", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_MapType_WithKey_GENERIC_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_WithKey_GENERIC_001_Ano_CMTS_Web";
        final String testMethod = "testMapWithKeyType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericMap", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_MapType_WithKey_GENERIC_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_WithKey_GENERIC_001_XML_CMTS_Web";
        final String testMethod = "testMapWithKeyType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericMap", "true");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_MapType_WithKey_NONGENERIC_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_WithKey_NONGENERIC_001_Ano_AMJTA_Web";
        final String testMethod = "testMapWithKeyType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericMap", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_MapType_WithKey_NONGENERIC_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_WithKey_NONGENERIC_001_XML_AMJTA_Web";
        final String testMethod = "testMapWithKeyType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericMap", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_MapType_WithKey_NONGENERIC_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_WithKey_NONGENERIC_001_Ano_AMRL_Web";
        final String testMethod = "testMapWithKeyType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericMap", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_MapType_WithKey_NONGENERIC_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_WithKey_NONGENERIC_001_XML_AMRL_Web";
        final String testMethod = "testMapWithKeyType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericMap", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_MapType_WithKey_NONGENERIC_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_WithKey_NONGENERIC_001_Ano_CMTS_Web";
        final String testMethod = "testMapWithKeyType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "OMContainerTypeEntityA");
        properties.put("EntityBName", "OMContainerTypeEntityB");
        properties.put("UseGenericMap", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_MapType_WithKey_NONGENERIC_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_MapType_WithKey_NONGENERIC_001_XML_CMTS_Web";
        final String testMethod = "testMapWithKeyType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLOMContainerTypeEntityA");
        properties.put("EntityBName", "XMLOMContainerTypeEntityB");
        properties.put("UseGenericMap", "false");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }
}
