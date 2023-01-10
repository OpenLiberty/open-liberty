/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.entity.tests.web;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.entity.testlogic.VersioningTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPADBTestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/VersioningTestServlet")
public class VersioningTestServlet extends JPADBTestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "ENTITY_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "ENTITY_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "ENTITY_RL")
    private EntityManagerFactory amrlEmf;

    @PostConstruct
    private void initFAT() {
        testClassName = VersioningTestLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
        jpaPctxMap.put("test-jpa-resource-2",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));

    }

    // testVersioning001

    @Test
    public void jpa10_Entity_Versioning_Int_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Int_Ano_AMJTA_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedIntEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Int_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Int_XML_AMJTA_Web";
        final String testMethod = "testVersioning001";
        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedIntEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Int_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Int_Ano_AMRL_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedIntEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Int_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Int_XML_AMRL_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedIntEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Int_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Int_Ano_CMTS_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedIntEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Int_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Int_XML_CMTS_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedIntEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_IntWrapper_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_IntWrapper_Ano_AMJTA_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedIntWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_IntWrapper_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_IntWrapper_XML_AMJTA_Web";
        final String testMethod = "testVersioning001";
        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedIntWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_IntWrapper_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_IntWrapper_Ano_AMRL_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedIntWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_IntWrapper_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_IntWrapper_XML_AMRL_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedIntWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_IntWrapper_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_IntWrapper_Ano_CMTS_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedIntWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_IntWrapper_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_IntWrapper_XML_CMTS_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedIntWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Long_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Long_Ano_AMJTA_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedLongEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Long_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Long_XML_AMJTA_Web";
        final String testMethod = "testVersioning001";
        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedLongEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Long_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Long_Ano_AMRL_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedLongEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Long_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Long_XML_AMRL_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedLongEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Long_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Long_Ano_CMTS_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedLongEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Long_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Long_XML_CMTS_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedLongEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_LongWrapper_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_LongWrapper_Ano_AMJTA_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedLongWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_LongWrapper_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_LongWrapper_XML_AMJTA_Web";
        final String testMethod = "testVersioning001";
        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedLongWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_LongWrapper_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_LongWrapper_Ano_AMRL_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedLongWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_LongWrapper_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_LongWrapper_XML_AMRL_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedLongWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_LongWrapper_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_LongWrapper_Ano_CMTS_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedLongWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_LongWrapper_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_LongWrapper_XML_CMTS_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedLongWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Short_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Short_Ano_AMJTA_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedShortEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Short_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Short_XML_AMJTA_Web";
        final String testMethod = "testVersioning001";
        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedShortEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Short_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Short_Ano_AMRL_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedShortEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Short_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Short_XML_AMRL_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedShortEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Short_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Short_Ano_CMTS_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedShortEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Short_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Short_XML_CMTS_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedShortEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_ShortWrapper_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_ShortWrapper_Ano_AMJTA_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedShortWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_ShortWrapper_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_ShortWrapper_XML_AMJTA_Web";
        final String testMethod = "testVersioning001";
        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedShortWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_ShortWrapper_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_ShortWrapper_Ano_AMRL_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedShortWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_ShortWrapper_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_ShortWrapper_XML_AMRL_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedShortWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_ShortWrapper_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_ShortWrapper_Ano_CMTS_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedShortWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_ShortWrapper_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_ShortWrapper_XML_CMTS_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedShortWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_SqlTimestamp_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_SqlTimestamp_Ano_AMJTA_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_SqlTimestamp_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_SqlTimestamp_XML_AMJTA_Web";
        final String testMethod = "testVersioning001";
        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_SqlTimestamp_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_SqlTimestamp_Ano_AMRL_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_SqlTimestamp_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_SqlTimestamp_XML_AMRL_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_SqlTimestamp_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_SqlTimestamp_Ano_CMTS_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_SqlTimestamp_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning_SqlTimestamp_XML_CMTS_Web";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    // testVersioning002 - Only Timestamp testing is necessary

    @Test
    public void jpa10_Entity_Versioning2_SqlTimestamp_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning2_SqlTimestamp_Ano_AMJTA_Web";
        final String testMethod = "testVersioning002";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning2_SqlTimestamp_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning2_SqlTimestamp_XML_AMJTA_Web";
        final String testMethod = "testVersioning002";
        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning2_SqlTimestamp_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning2_SqlTimestamp_Ano_AMRL_Web";
        final String testMethod = "testVersioning002";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning2_SqlTimestamp_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning2_SqlTimestamp_XML_AMRL_Web";
        final String testMethod = "testVersioning002";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning2_SqlTimestamp_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning2_SqlTimestamp_Ano_CMTS_Web";
        final String testMethod = "testVersioning002";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning2_SqlTimestamp_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning2_SqlTimestamp_XML_CMTS_Web";
        final String testMethod = "testVersioning002";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    // testVersioning003

    @Test
    public void jpa10_Entity_Versioning3_Int_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Int_Ano_AMJTA_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedIntEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_Int_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Int_XML_AMJTA_Web";
        final String testMethod = "testVersioning003";
        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedIntEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_Int_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Int_Ano_AMRL_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedIntEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_Int_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Int_XML_AMRL_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedIntEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_Int_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Int_Ano_CMTS_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedIntEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_Int_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Int_XML_CMTS_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedIntEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_IntWrapper_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_IntWrapper_Ano_AMJTA_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedIntWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_IntWrapper_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_IntWrapper_XML_AMJTA_Web";
        final String testMethod = "testVersioning003";
        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedIntWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_IntWrapper_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_IntWrapper_Ano_AMRL_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedIntWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_IntWrapper_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_IntWrapper_XML_AMRL_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedIntWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_IntWrapper_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_IntWrapper_Ano_CMTS_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedIntWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_IntWrapper_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_IntWrapper_XML_CMTS_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedIntWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_Long_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Long_Ano_AMJTA_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedLongEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_Long_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Long_XML_AMJTA_Web";
        final String testMethod = "testVersioning003";
        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedLongEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_Long_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Long_Ano_AMRL_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedLongEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_Long_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Long_XML_AMRL_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedLongEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_Long_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Long_Ano_CMTS_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedLongEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_Long_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Long_XML_CMTS_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedLongEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_LongWrapper_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_LongWrapper_Ano_AMJTA_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedLongWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_LongWrapper_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_LongWrapper_XML_AMJTA_Web";
        final String testMethod = "testVersioning003";
        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedLongWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_LongWrapper_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_LongWrapper_Ano_AMRL_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedLongWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_LongWrapper_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_LongWrapper_XML_AMRL_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedLongWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_LongWrapper_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_LongWrapper_Ano_CMTS_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedLongWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_LongWrapper_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_LongWrapper_XML_CMTS_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedLongWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_Short_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Short_Ano_AMJTA_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedShortEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_Short_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Short_XML_AMJTA_Web";
        final String testMethod = "testVersioning003";
        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedShortEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_Short_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Short_Ano_AMRL_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedShortEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_Short_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Short_XML_AMRL_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedShortEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_Short_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Short_Ano_CMTS_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedShortEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_Short_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Short_XML_CMTS_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedShortEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_ShortWrapper_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_ShortWrapper_Ano_AMJTA_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedShortWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_ShortWrapper_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_ShortWrapper_XML_AMJTA_Web";
        final String testMethod = "testVersioning003";
        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedShortWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_ShortWrapper_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_ShortWrapper_Ano_AMRL_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedShortWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_ShortWrapper_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_ShortWrapper_XML_AMRL_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedShortWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_ShortWrapper_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_ShortWrapper_Ano_CMTS_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedShortWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_ShortWrapper_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_ShortWrapper_XML_CMTS_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedShortWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_SqlTimestamp_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_SqlTimestamp_Ano_AMJTA_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_SqlTimestamp_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_SqlTimestamp_XML_AMJTA_Web";
        final String testMethod = "testVersioning003";
        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_SqlTimestamp_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_SqlTimestamp_Ano_AMRL_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_SqlTimestamp_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_SqlTimestamp_XML_AMRL_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_SqlTimestamp_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_SqlTimestamp_Ano_CMTS_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_SqlTimestamp_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_SqlTimestamp_XML_CMTS_Web";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

}
