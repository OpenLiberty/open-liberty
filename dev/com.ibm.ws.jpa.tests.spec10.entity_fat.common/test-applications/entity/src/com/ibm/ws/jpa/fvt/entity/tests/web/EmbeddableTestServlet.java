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

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.entity.testlogic.EmbeddableTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPADBTestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/EmbeddableTestServlet")
public class EmbeddableTestServlet extends JPADBTestServlet {
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
        testClassName = EmbeddableTestLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    @Test
    public void jpa10_Entity_embeddable_Embeddable_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Embeddable_Ano_AMJTA_Web";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "EmbeddedObjectEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_embeddable_Embeddable_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Embeddable_XML_AMJTA_Web";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLEmbeddedObjectEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_embeddable_Embeddable_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Embeddable_Ano_AMRL_Web";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "EmbeddedObjectEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_embeddable_Embeddable_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Embeddable_XML_AMRL_Web";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLEmbeddedObjectEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_embeddable_Embeddable_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Embeddable_Ano_CMTS_Web";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "EmbeddedObjectEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_embeddable_Embeddable_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Embeddable_XML_CMTS_Web";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLEmbeddedObjectEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_embeddable_EmbeddableAO_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_EmbeddableAO_Ano_AMJTA_Web";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "EmbeddedObjectAOEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_embeddable_EmbeddableAO_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_EmbeddableAO_XML_AMJTA_Web";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLEmbeddedObjectAOEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_embeddable_EmbeddableAO_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_EmbeddableAO_Ano_AMRL_Web";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "EmbeddedObjectAOEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_embeddable_EmbeddableAO_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_EmbeddableAO_XML_AMRL_Web";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLEmbeddedObjectAOEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_embeddable_EmbeddableAO_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_EmbeddableAO_Ano_CMTS_Web";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "EmbeddedObjectAOEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_embeddable_EmbeddableAO_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_EmbeddableAO_XML_CMTS_Web";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLEmbeddedObjectAOEntity");

        executeTest(testName, testMethod, testResource, properties);
    }
}
