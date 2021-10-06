/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.manyXmany.testlogic.tests.ejb;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@WebServlet(urlPatterns = "/TestManyXManyCompoundPK_EJB_SF_Servlet")
public class TestManyXManyCompoundPK_EJB_SF_Servlet extends EJBTestVehicleServlet {
    private static final long serialVersionUID = -6684407686587457314L;

    private final String testLogicClassName = "com.ibm.ws.jpa.fvt.relationships.manyXmany.testlogic.ManyXManyCompoundPKTestLogic";

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    private final static String ejbJNDIName = "ejb/ManyXManySFEJB";

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/ManyXMany_CompoundPK_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/ManyXMany_CompoundPK_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/ManyXMany_CompoundPK_CMTS"));
    }

/*
 * Verify that the JPA provider can manage ManyXMany relationships where the
 * entity on the inverse side of the relationship has a compound (Embeddable/ID Class) primary key.
 * Entities and IdClass are defined in annotation.
 *
 * Test Strategy:
 *
 * 1) Start a new transaction
 * 2) Create ICompoundPKManyXManyEntityA(id=1)
 * Create ICompoundPKManyXManyEntityB(id=1)
 * Set ICompoundPKManyXManyEntityA(id=1) to reference ICompoundPKManyXManyEntityB(id=1) in
 * a ManyXMany relationship
 * 3) Commit the transaction
 * 4) Clear the persistence context
 * 5) Find ICompoundPKManyXManyEntityA(id=1), access ICompoundPKManyXManyEntityB(id=1) from
 * ICompoundPKManyXManyEntityA(id=1)'s ManyXMany relationship field.
 *
 * Test passes if the relationship properly references ICompoundPKManyXManyEntityB(id=1)
 */

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_Ano_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "EmbedIDMMEntityA");
        properties.put("EntityBName", "EmbedIDMMEntityB");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_XML_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLEmbedIDMMEntityA");
        properties.put("EntityBName", "XMLEmbedIDMMEntityB");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_Ano_AMRL_EJB_SF";
        final String testMethod = "testManyXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "EmbedIDMMEntityA");
        properties.put("EntityBName", "EmbedIDMMEntityB");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_XML_AMRL_EJB_SF";
        final String testMethod = "testManyXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLEmbedIDMMEntityA");
        properties.put("EntityBName", "XMLEmbedIDMMEntityB");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_Ano_CMTS_EJB_SF";
        final String testMethod = "testManyXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "EmbedIDMMEntityA");
        properties.put("EntityBName", "EmbedIDMMEntityB");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_Embeddable_001_XML_CMTS_EJB_SF";
        final String testMethod = "testManyXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLEmbedIDMMEntityA");
        properties.put("EntityBName", "XMLEmbedIDMMEntityB");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_Ano_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "IDClassMMEntityA");
        properties.put("EntityBName", "IDClassMMEntityB");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_XML_AMJTA_EJB_SF";
        final String testMethod = "testManyXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLIDClassMMEntityA");
        properties.put("EntityBName", "XMLIDClassMMEntityB");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_Ano_AMRL_EJB_SF";
        final String testMethod = "testManyXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "IDClassMMEntityA");
        properties.put("EntityBName", "IDClassMMEntityB");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_XML_AMRL_EJB_SF";
        final String testMethod = "testManyXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLIDClassMMEntityA");
        properties.put("EntityBName", "XMLIDClassMMEntityB");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_Ano_CMTS_EJB_SF";
        final String testMethod = "testManyXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "IDClassMMEntityA");
        properties.put("EntityBName", "IDClassMMEntityB");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_CompoundPK_IDClass_001_XML_CMTS_EJB_SF";
        final String testMethod = "testManyXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLIDClassMMEntityA");
        properties.put("EntityBName", "XMLIDClassMMEntityB");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

}
