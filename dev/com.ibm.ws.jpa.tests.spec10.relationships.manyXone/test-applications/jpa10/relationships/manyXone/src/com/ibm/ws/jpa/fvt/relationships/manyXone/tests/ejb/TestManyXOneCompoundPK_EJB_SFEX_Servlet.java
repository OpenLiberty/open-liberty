/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.relationships.manyXone.tests.ejb;

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
@WebServlet(urlPatterns = "/TestManyXOneCompoundPK_EJB_SFEX_Servlet")
public class TestManyXOneCompoundPK_EJB_SFEX_Servlet extends EJBTestVehicleServlet {
    private final String testLogicClassName = "com.ibm.ws.jpa.fvt.relationships.manyXone.testlogic.ManyXOneCompoundPKTestLogic";

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    private final static String ejbJNDIName = "ejb/ManyXOneSFExEJB";

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/ManyXOne_CompoundPK_CMEX"));
    }

    /*
     * Verify that the JPA provider can manage ManyXOne relationships where the
     * entity on the inverse side of the relationship has a compound (Embeddable/ID Class) primary key.
     * Entities and IdClass are defined in annotation.
     *
     * Test Strategy:
     *
     * 1) Start a new transaction
     * 2) Create ICompoundPKManyXOneEntityA(id=1)
     * Create ICompoundPKManyXOneEntityB(id=1)
     * Set ICompoundPKManyXOneEntityA(id=1) to reference ICompoundPKManyXOneEntityB(id=1) in
     * a ManyXOne relationship
     * 3) Commit the transaction
     * 4) Clear the persistence context
     * 5) Find ICompoundPKManyXOneEntityA(id=1), access ICompoundPKManyXOneEntityB(id=1) from
     * ICompoundPKManyXOneEntityA(id=1)'s ManyXOne relationship field.
     *
     * Test passes if the relationship properly references ICompoundPKManyXOneEntityB(id=1)
     */

    @Test
    public void jpa10_Relationships_ManyXOne_CompoundPK_Embeddable_001_Ano_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXOne_CompoundPK_Embeddable_001_Ano_CMEX_EJB_SF";
        final String testMethod = "testManyXOneCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmex"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "EmbedIDMOEntityA");
        properties.put("EntityBName", "EmbedIDMOEntityB");

        executeDDL("JPA10_MANYXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXOne_CompoundPK_Embeddable_001_XML_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXOne_CompoundPK_Embeddable_001_XML_CMEX_EJB_SF";
        final String testMethod = "testManyXOneCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmex"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLEmbedIDMOEntityA");
        properties.put("EntityBName", "XMLEmbedIDMOEntityB");

        executeDDL("JPA10_MANYXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXOne_CompoundPK_IDClass_001_Ano_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXOne_CompoundPK_IDClass_001_Ano_CMEX_EJB_SF";
        final String testMethod = "testManyXOneCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmex"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "IDClassMOEntityA");
        properties.put("EntityBName", "IDClassMOEntityB");

        executeDDL("JPA10_MANYXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXOne_CompoundPK_IDClass_001_XML_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Relationships_ManyXOne_CompoundPK_IDClass_001_XML_CMEX_EJB_SF";
        final String testMethod = "testManyXOneCompoundPK001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmex"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLIDClassMOEntityA");
        properties.put("EntityBName", "XMLIDClassMOEntityB");

        executeDDL("JPA10_MANYXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }
}
