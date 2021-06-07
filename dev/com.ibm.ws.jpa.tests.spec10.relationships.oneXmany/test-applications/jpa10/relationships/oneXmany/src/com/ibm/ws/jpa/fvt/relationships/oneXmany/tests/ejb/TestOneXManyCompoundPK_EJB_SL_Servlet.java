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

package com.ibm.ws.jpa.fvt.relationships.oneXmany.tests.ejb;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOneXManyCompoundPK_EJB_SL_Servlet")
public class TestOneXManyCompoundPK_EJB_SL_Servlet extends EJBTestVehicleServlet {
    private final String testLogicClassName = "com.ibm.ws.jpa.fvt.relationships.oneXmany.testlogic.OneXManyCompoundPKTestLogic";

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    private final static String ejbJNDIName = "ejb/OneXManySLEJB";

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OneXMany_CompoundPK_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OneXMany_CompoundPK_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/OneXMany_CompoundPK_CMTS"));
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
    public void jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_Ano_AMJTA_EJB_SL";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "EmbedIDOMEntityA");
        properties.put("EntityBName", "EmbedIDOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_XML_AMJTA_EJB_SL";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLEmbedIDOMEntityA");
        properties.put("EntityBName", "XMLEmbedIDOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_Ano_AMRL_EJB_SL";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "EmbedIDOMEntityA");
        properties.put("EntityBName", "EmbedIDOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_XML_AMRL_EJB_SL";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLEmbedIDOMEntityA");
        properties.put("EntityBName", "XMLEmbedIDOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_Ano_CMTS_EJB_SL";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "EmbedIDOMEntityA");
        properties.put("EntityBName", "EmbedIDOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_Embeddable_001_XML_CMTS_EJB_SL";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLEmbedIDOMEntityA");
        properties.put("EntityBName", "XMLEmbedIDOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_Ano_AMJTA_EJB_SL";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "IDClassOMEntityA");
        properties.put("EntityBName", "IDClassOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_XML_AMJTA_EJB_SL";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLIDClassOMEntityA");
        properties.put("EntityBName", "XMLIDClassOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_Ano_AMRL_EJB_SL";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "IDClassOMEntityA");
        properties.put("EntityBName", "IDClassOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_XML_AMRL_EJB_SL";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLIDClassOMEntityA");
        properties.put("EntityBName", "XMLIDClassOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_Ano_CMTS_EJB_SL";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "IDClassOMEntityA");
        properties.put("EntityBName", "IDClassOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_OneXMany_CompoundPK_IDClass_001_XML_CMTS_EJB_SL";
        final String testMethod = "testOneXManyCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLIDClassOMEntityA");
        properties.put("EntityBName", "XMLIDClassOMEntityB");

        executeDDL("JPA10_ONEXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }
}
