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

import com.ibm.ws.jpa.fvt.relationships.oneXmany.testlogic.OneXManyCompoundPKTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOneXManyCompoundPKServlet")
public class TestOneXManyCompoundPKServlet extends JPATestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "OneXMany_CompoundPK_JTA")
    private EntityManager oxmCompoundPKEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "OneXMany_CompoundPK_JTA")
    private EntityManagerFactory oxmCompoundPKEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "OneXMany_CompoundPK_RL")
    private EntityManagerFactory oxmCompoundPKEmfRL;

    private final String testLogicClassName = OneXManyCompoundPKTestLogic.class.getName();

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "oxmCompoundPKEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "oxmCompoundPKEmfRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "oxmCompoundPKEm"));
    }

    /*
     * Verify that the JPA provider can manage OneXMany relationships where the
     * entity on the inverse side of the relationship has a compound (Embeddable/ID Class) primary key.
     * Entities and IdClass are defined in annotation.
     *
     * Test Strategy:
     *
     * 1) Start a new transaction
     * 2) Create ICompoundPKOneXManyEntityA(id=1)
     * Create ICompoundPKOneXManyEntityB(id=1)
     * Set ICompoundPKOneXManyEntityA(id=1) to reference ICompoundPKOneXManyEntityB(id=1) in
     * a OneXMany relationship
     * 3) Commit the transaction
     * 4) Clear the persistence context
     * 5) Find ICompoundPKOneXManyEntityA(id=1), access ICompoundPKOneXManyEntityB(id=1) from
     * ICompoundPKOneXManyEntityA(id=1)'s OneXMany relationship field.
     *
     * Test passes if the relationship properly references ICompoundPKOneXManyEntityB(id=1)
     */

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_Ano_AMJTA_Web";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "EmbedIDOMEntityA");
        properties.put("EntityBName", "EmbedIDOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_XML_AMJTA_Web";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLEmbedIDOMEntityA");
        properties.put("EntityBName", "XMLEmbedIDOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_Ano_AMRL_Web";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "EmbedIDOMEntityA");
        properties.put("EntityBName", "EmbedIDOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_XML_AMRL_Web";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLEmbedIDOMEntityA");
        properties.put("EntityBName", "XMLEmbedIDOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_Ano_CMTS_Web";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "EmbedIDOMEntityA");
        properties.put("EntityBName", "EmbedIDOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_XML_CMTS_Web";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLEmbedIDOMEntityA");
        properties.put("EntityBName", "XMLEmbedIDOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_Ano_AMJTA_Web";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "IDClassOMEntityA");
        properties.put("EntityBName", "IDClassOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_XML_AMJTA_Web";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLIDClassOMEntityA");
        properties.put("EntityBName", "XMLIDClassOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_Ano_AMRL_Web";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "IDClassOMEntityA");
        properties.put("EntityBName", "IDClassOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_XML_AMRL_Web";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLIDClassOMEntityA");
        properties.put("EntityBName", "XMLIDClassOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_Ano_CMTS_Web";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "IDClassOMEntityA");
        properties.put("EntityBName", "IDClassOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_XML_CMTS_Web";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLIDClassOMEntityA");
        properties.put("EntityBName", "XMLIDClassOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }
}
