/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
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

import com.ibm.ws.jpa.fvt.inheritance.entities.concretetable.ano.AnoConcreteTreeLeaf1Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.concretetable.ano.AnoConcreteTreeLeaf2Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.concretetable.ano.AnoConcreteTreeLeaf3Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.concretetable.xml.XMLConcreteTreeLeaf1Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.concretetable.xml.XMLConcreteTreeLeaf2Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.concretetable.xml.XMLConcreteTreeLeaf3Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.jointable.ano.AnoJTCDTreeLeaf1Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.jointable.ano.AnoJTCDTreeLeaf2Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.jointable.ano.AnoJTCDTreeLeaf3Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.jointable.ano.AnoJTIDTreeLeaf1Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.jointable.ano.AnoJTIDTreeLeaf2Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.jointable.ano.AnoJTIDTreeLeaf3Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.jointable.ano.AnoJTSDTreeLeaf1Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.jointable.ano.AnoJTSDTreeLeaf2Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.jointable.ano.AnoJTSDTreeLeaf3Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.jointable.xml.XMLJTCDTreeLeaf1Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.jointable.xml.XMLJTCDTreeLeaf2Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.jointable.xml.XMLJTCDTreeLeaf3Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.jointable.xml.XMLJTIDTreeLeaf1Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.jointable.xml.XMLJTIDTreeLeaf2Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.jointable.xml.XMLJTIDTreeLeaf3Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.jointable.xml.XMLJTSDTreeLeaf1Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.jointable.xml.XMLJTSDTreeLeaf2Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.jointable.xml.XMLJTSDTreeLeaf3Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.msc.ano.AnoAnoMSCEntity;
import com.ibm.ws.jpa.fvt.inheritance.entities.msc.ano.XMLAnoMSCEntity;
import com.ibm.ws.jpa.fvt.inheritance.entities.msc.xml.AnoXMLMSCEntity;
import com.ibm.ws.jpa.fvt.inheritance.entities.msc.xml.XMLXMLMSCEntity;
import com.ibm.ws.jpa.fvt.inheritance.entities.singletable.ano.AnoSTCDTreeLeaf1Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.singletable.ano.AnoSTCDTreeLeaf2Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.singletable.ano.AnoSTCDTreeLeaf3Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.singletable.ano.AnoSTIDTreeLeaf1Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.singletable.ano.AnoSTIDTreeLeaf2Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.singletable.ano.AnoSTIDTreeLeaf3Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.singletable.ano.AnoSTSDTreeLeaf1Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.singletable.xml.XMLSTCDTreeLeaf1Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.singletable.xml.XMLSTCDTreeLeaf2Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.singletable.xml.XMLSTCDTreeLeaf3Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.singletable.xml.XMLSTIDTreeLeaf1Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.singletable.xml.XMLSTIDTreeLeaf2Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.singletable.xml.XMLSTIDTreeLeaf3Entity;
import com.ibm.ws.jpa.fvt.inheritance.entities.singletable.xml.XMLSTSDTreeLeaf1Entity;
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

    @PostConstruct
    private void initFAT() {
        testClassName = InheritanceTestLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    /*
     * Application Managed JTA
     */

    //  TABLE-PER-CLASS (Concrete Table) Ano and XML Tests

    @Test
    public void jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoConcreteTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoConcreteTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoConcreteTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLConcreteTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLConcreteTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLConcreteTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Joined Table, Character Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTCDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTCDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTCDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTCDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTCDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTCDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Joined Table, Integer Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTIDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTIDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTIDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTIDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTIDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTIDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Joined Table, String Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTSDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTSDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTSDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTSDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Single Table, Character Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTCDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTCDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTCDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTCDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTCDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTCDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Single Table, Integer Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTIDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTIDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTIDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTIDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTIDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTIDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Single Table, String Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_Ano_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_XML_AMJTA_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Mapped Superclass Inheritance Tests

    @Test
    public void jpa10_Inheritance_MappedSuperclass_AnoMSC_AnoEntity_CRUDTest_001_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_AnoMSC_AnoEntity_CRUDTest_001_AMJTA_Web";
        final String testMethod = "testMSCInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoAnoMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_MappedSuperclass_AnoMSC_XMLEntity_CRUDTest_001_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_AnoMSC_XMLEntity_CRUDTest_001_AMJTA_Web";
        final String testMethod = "testMSCInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLAnoMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_MappedSuperclass_XMLMSC_AnoEntity_CRUDTest_001_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_XMLMSC_AnoEntity_CRUDTest_001_AMJTA_Web";
        final String testMethod = "testMSCInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoXMLMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_MappedSuperclass_XMLMSC_XMLEntity_CRUDTest_001_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_XMLMSC_XMLEntity_CRUDTest_001_AMJTA_Web";
        final String testMethod = "testMSCInheritance001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLXMLMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Application Managed Resource Local
     */

    //  TABLE-PER-CLASS (Concrete Table) Ano and XML Tests

    @Test
    public void jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoConcreteTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoConcreteTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoConcreteTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLConcreteTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLConcreteTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLConcreteTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Joined Table, Character Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTCDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTCDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTCDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTCDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTCDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTCDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Joined Table, Integer Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTIDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTIDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTIDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTIDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTIDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTIDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Joined Table, String Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTSDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTSDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTSDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTSDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Single Table, Character Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTCDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTCDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTCDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTCDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTCDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTCDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Single Table, Integer Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTIDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTIDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTIDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTIDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTIDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTIDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Single Table, String Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_Ano_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_XML_AMRL_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Mapped Superclass Inheritance Tests

    @Test
    public void jpa10_Inheritance_MappedSuperclass_AnoMSC_AnoEntity_CRUDTest_001_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_AnoMSC_AnoEntity_CRUDTest_001_AMRL_Web";
        final String testMethod = "testMSCInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoAnoMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_MappedSuperclass_AnoMSC_XMLEntity_CRUDTest_001_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_AnoMSC_XMLEntity_CRUDTest_001_AMRL_Web";
        final String testMethod = "testMSCInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLAnoMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_MappedSuperclass_XMLMSC_AnoEntity_CRUDTest_001_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_XMLMSC_AnoEntity_CRUDTest_001_AMRL_Web";
        final String testMethod = "testMSCInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoXMLMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_MappedSuperclass_XMLMSC_XMLEntity_CRUDTest_001_AMRL_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_XMLMSC_XMLEntity_CRUDTest_001_AMRL_Web";
        final String testMethod = "testMSCInheritance001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLXMLMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Container Managed Transaction Scope
     */

    //  TABLE-PER-CLASS (Concrete Table) Ano and XML Tests

    @Test
    public void jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoConcreteTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoConcreteTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoConcreteTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf1_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLConcreteTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf2_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLConcreteTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_Concrete_Leaf3_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLConcreteTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Joined Table, Character Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTCDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTCDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTCDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf1_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTCDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf2_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTCDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_CharDisc_Leaf3_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTCDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Joined Table, Integer Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTIDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTIDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTIDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf1_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTIDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf2_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTIDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_IntDisc_Leaf3_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTIDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Joined Table, String Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTSDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoJTSDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf1_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf2_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTSDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_JoinedTable_StringDisc_Leaf3_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLJTSDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Single Table, Character Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTCDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTCDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTCDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf1_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTCDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf2_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTCDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_CharDisc_Leaf3_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTCDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Single Table, Integer Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTIDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTIDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTIDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf1_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTIDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf2_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTIDTreeLeaf2Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_IntDisc_Leaf3_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTIDTreeLeaf3Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Single Table, String Discrminator Ano and XML Tests

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_Ano_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoSTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf1_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf2_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_SingleTable_StringDisc_Leaf3_CRUDTest_001_XML_CMTS_Web";
        final String testMethod = "testInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLSTSDTreeLeaf1Entity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Mapped Superclass Inheritance Tests

    @Test
    public void jpa10_Inheritance_MappedSuperclass_AnoMSC_AnoEntity_CRUDTest_001_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_AnoMSC_AnoEntity_CRUDTest_001_CMTS_Web";
        final String testMethod = "testMSCInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoAnoMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_MappedSuperclass_AnoMSC_XMLEntity_CRUDTest_001_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_AnoMSC_XMLEntity_CRUDTest_001_CMTS_Web";
        final String testMethod = "testMSCInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLAnoMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_MappedSuperclass_XMLMSC_AnoEntity_CRUDTest_001_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_XMLMSC_AnoEntity_CRUDTest_001_CMTS_Web";
        final String testMethod = "testMSCInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoXMLMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Inheritance_MappedSuperclass_XMLMSC_XMLEntity_CRUDTest_001_CMTS_Web() throws Exception {
        final String testName = "jpa10_Inheritance_MappedSuperclass_XMLMSC_XMLEntity_CRUDTest_001_CMTS_Web";
        final String testMethod = "testMSCInheritance001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLXMLMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }
}
