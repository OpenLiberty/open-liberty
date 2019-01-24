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

package com.ibm.ws.jpa.fvt.inheritance.tests.web;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.inheritance.testlogic.InheritanceTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestInheritanceServlet")
public class TestInheritanceServlet extends JPATestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "Inheritance_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "Inheritance_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "Inheritance_RL")
    private EntityManagerFactory amrlEmf;

    // Cleanup
    @PersistenceUnit(unitName = "Cleanup")
    private EntityManagerFactory cleanupEmf;

    private final String testLogicClassName = InheritanceTestLogic.class.getName();

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
        jpaPctxMap.put("cleanup",
                       new JPAPersistenceContext("cleanup", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "cleanupEmf"));

    }

    /*
     * Application Managed JTA
     */

    //  TABLE-PER-CLASS (Concrete Table) Ano and XML Tests

    @Test
    public void jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoConcreteTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoConcreteTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoConcreteTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLConcreteTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLConcreteTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLConcreteTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    // Joined Table, Character Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTCDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTCDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTCDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTCDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTCDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTCDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    // Joined Table, Integer Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTIDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTIDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTIDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTIDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTIDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTIDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    // Joined Table, String Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTSDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTSDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTSDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTSDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    // Single Table, Character Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTCDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTCDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTCDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTCDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTCDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTCDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    // Single Table, Integer Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTIDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTIDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTIDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTIDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTIDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTIDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    // Single Table, String Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    // Mapped Superclass Inheritance Tests

    @Test
    public void jpa10_Inheritance_MappedSuperclass_AnoMSC_AnoEntity_CRUDTest_001_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_AnoMSC_AnoEntity_CRUDTest_001_AMJTA_Web";
        final String testMethod = "testMSCInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoAnoMSCEntity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_MappedSuperclass_AnoMSC_XMLEntity_CRUDTest_001_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_AnoMSC_XMLEntity_CRUDTest_001_AMJTA_Web";
        final String testMethod = "testMSCInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLAnoMSCEntity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_MappedSuperclass_XMLMSC_AnoEntity_CRUDTest_001_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_XMLMSC_AnoEntity_CRUDTest_001_AMJTA_Web";
        final String testMethod = "testMSCInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoXMLMSCEntity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_MappedSuperclass_XMLMSC_XMLEntity_CRUDTest_001_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_XMLMSC_XMLEntity_CRUDTest_001_AMJTA_Web";
        final String testMethod = "testMSCInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLXMLMSCEntity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    /*
     * Application Managed Resource Local
     */

    //  TABLE-PER-CLASS (Concrete Table) Ano and XML Tests

    @Test
    public void jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoConcreteTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoConcreteTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoConcreteTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLConcreteTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLConcreteTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLConcreteTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    // Joined Table, Character Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTCDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTCDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTCDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTCDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTCDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTCDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    // Joined Table, Integer Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTIDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTIDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTIDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTIDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTIDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTIDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    // Joined Table, String Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTSDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTSDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTSDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTSDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    // Single Table, Character Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTCDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTCDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTCDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTCDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTCDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTCDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    // Single Table, Integer Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTIDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTIDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTIDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTIDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTIDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTIDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    // Single Table, String Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    // Mapped Superclass Inheritance Tests

    @Test
    public void jpa10_Inheritance_MappedSuperclass_AnoMSC_AnoEntity_CRUDTest_001_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_AnoMSC_AnoEntity_CRUDTest_001_AMRL_Web";
        final String testMethod = "testMSCInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoAnoMSCEntity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_MappedSuperclass_AnoMSC_XMLEntity_CRUDTest_001_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_AnoMSC_XMLEntity_CRUDTest_001_AMRL_Web";
        final String testMethod = "testMSCInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLAnoMSCEntity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_MappedSuperclass_XMLMSC_AnoEntity_CRUDTest_001_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_XMLMSC_AnoEntity_CRUDTest_001_AMRL_Web";
        final String testMethod = "testMSCInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoXMLMSCEntity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_MappedSuperclass_XMLMSC_XMLEntity_CRUDTest_001_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_XMLMSC_XMLEntity_CRUDTest_001_AMRL_Web";
        final String testMethod = "testMSCInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLXMLMSCEntity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    /*
     * Container Managed Transaction Scope
     */

    //  TABLE-PER-CLASS (Concrete Table) Ano and XML Tests

    @Test
    public void jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoConcreteTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoConcreteTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoConcreteTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLConcreteTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLConcreteTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLConcreteTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    // Joined Table, Character Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTCDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTCDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTCDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTCDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTCDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTCDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    // Joined Table, Integer Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTIDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTIDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTIDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTIDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTIDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTIDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    // Joined Table, String Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTSDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoJTSDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTSDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLJTSDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    // Single Table, Character Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTCDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTCDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTCDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTCDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTCDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTCDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    // Single Table, Integer Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTIDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTIDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTIDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTIDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTIDTreeLeaf2Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTIDTreeLeaf3Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    // Single Table, String Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoSTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLSTSDTreeLeaf1Entity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    // Mapped Superclass Inheritance Tests

    @Test
    public void jpa10_Inheritance_MappedSuperclass_AnoMSC_AnoEntity_CRUDTest_001_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_AnoMSC_AnoEntity_CRUDTest_001_CMTS_Web";
        final String testMethod = "testMSCInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoAnoMSCEntity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_MappedSuperclass_AnoMSC_XMLEntity_CRUDTest_001_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_AnoMSC_XMLEntity_CRUDTest_001_CMTS_Web";
        final String testMethod = "testMSCInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLAnoMSCEntity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_MappedSuperclass_XMLMSC_AnoEntity_CRUDTest_001_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_XMLMSC_AnoEntity_CRUDTest_001_CMTS_Web";
        final String testMethod = "testMSCInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoXMLMSCEntity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Inheritance_MappedSuperclass_XMLMSC_XMLEntity_CRUDTest_001_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_XMLMSC_XMLEntity_CRUDTest_001_CMTS_Web";
        final String testMethod = "testMSCInheritance001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLXMLMSCEntity");

        executeDDL("JPA10_INHERITANCE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }
}
