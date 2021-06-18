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

@WebServlet(urlPatterns = "/TestManyXManyCollectionType_EJB_SL_Servlet")
public class TestManyXManyCollectionType_EJB_SL_Servlet extends EJBTestVehicleServlet {
    private final String testLogicClassName = "com.ibm.ws.jpa.fvt.relationships.manyXmany.testlogic.ManyXManyCollectionTypeTestLogic";

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    private final static String ejbJNDIName = "ejb/ManyXManySLEJB";

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/ManyXMany_Container_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/ManyXMany_Container_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/ManyXMany_Container_CMTS"));
    }

/*
 * Executes test for ManyXMany relationship using a Collection-type.
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
    public void jpa10_Relationships_ManyXMany_Collection_Generic_001_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Collection_Generic_001_Ano_AMJTA_EJB_SL";
        final String testMethod = "testCollectionType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMContainerTypeEntityA");
        properties.put("EntityBName", "MMContainerTypeEntityB");
        properties.put("UseGenericCollection", "true");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Collection_Generic_001_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Collection_Generic_001_XML_AMJTA_EJB_SL";
        final String testMethod = "testCollectionType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMContainerTypeEntityA");
        properties.put("EntityBName", "XMLMMContainerTypeEntityB");
        properties.put("UseGenericCollection", "true");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Collection_Generic_001_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Collection_Generic_001_Ano_AMRL_EJB_SL";
        final String testMethod = "testCollectionType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMContainerTypeEntityA");
        properties.put("EntityBName", "XMLMMContainerTypeEntityB");
        properties.put("UseGenericCollection", "true");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Collection_Generic_001_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Collection_Generic_001_XML_AMRL_EJB_SL";
        final String testMethod = "testCollectionType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMContainerTypeEntityA");
        properties.put("EntityBName", "XMLMMContainerTypeEntityB");
        properties.put("UseGenericCollection", "true");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Collection_Generic_001_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Collection_Generic_001_Ano_CMTS_EJB_SL";
        final String testMethod = "testCollectionType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "MMContainerTypeEntityA");
        properties.put("EntityBName", "MMContainerTypeEntityB");
        properties.put("UseGenericCollection", "true");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Relationships_ManyXMany_Collection_Generic_001_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Relationships_ManyXMany_Collection_Generic_001_XML_CMTS_EJB_SL";
        final String testMethod = "testCollectionType";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLMMContainerTypeEntityA");
        properties.put("EntityBName", "XMLMMContainerTypeEntityB");
        properties.put("UseGenericCollection", "true");

        executeDDL("JPA10_MANYXMANY_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }
}
