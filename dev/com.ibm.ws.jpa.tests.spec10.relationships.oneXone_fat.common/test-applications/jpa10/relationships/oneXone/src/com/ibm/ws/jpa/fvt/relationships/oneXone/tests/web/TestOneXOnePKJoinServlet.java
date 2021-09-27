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

package com.ibm.ws.jpa.fvt.relationships.oneXone.tests.web;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.relationships.oneXone.testlogic.OneXOnePKJoinTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOneXOnePKJoinServlet")
public class TestOneXOnePKJoinServlet extends JPATestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "OneXOne_PKJoinColumn_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "OneXOne_PKJoinColumn_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "OneXOne_PKJoinColumn_RL")
    private EntityManagerFactory amrlEmf;

    private final String testLogicClassName = OneXOnePKJoinTestLogic.class.getName();

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    @Test
    public void jpa10_Relationships_OneXOne_PKJoin_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_PKJoin_001_Ano_AMJTA_Web";
        final String testMethod = "testPKJoin001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "PKJoinOOEntityA");
        properties.put("EntityBName", "PKJoinOOEntityB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXOne_PKJoin_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_PKJoin_001_XML_AMJTA_Web";
        final String testMethod = "testPKJoin001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLPKJoinOOEntityA");
        properties.put("EntityBName", "XMLPKJoinOOEntityB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXOne_PKJoin_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_PKJoin_001_Ano_AMRL_Web";
        final String testMethod = "testPKJoin001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "PKJoinOOEntityA");
        properties.put("EntityBName", "PKJoinOOEntityB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXOne_PKJoin_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_PKJoin_001_XML_AMRL_Web";
        final String testMethod = "testPKJoin001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amrl"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLPKJoinOOEntityA");
        properties.put("EntityBName", "XMLPKJoinOOEntityB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXOne_PKJoin_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_PKJoin_001_Ano_CMTS_Web";
        final String testMethod = "testPKJoin001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "PKJoinOOEntityA");
        properties.put("EntityBName", "PKJoinOOEntityB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

    @Test
    public void jpa10_Relationships_OneXOne_PKJoin_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Relationships_OneXOne_PKJoin_001_XML_CMTS_Web";
        final String testMethod = "testPKJoin001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmts"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityAName", "XMLPKJoinOOEntityA");
        properties.put("EntityBName", "XMLPKJoinOOEntityB");

        executeDDL("JPA10_ONEXONE_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx);
    }

}
