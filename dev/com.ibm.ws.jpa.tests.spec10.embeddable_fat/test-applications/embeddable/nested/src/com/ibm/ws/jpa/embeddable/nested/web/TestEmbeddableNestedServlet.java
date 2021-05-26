/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.embeddable.nested.web;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.embeddable.nested.testlogic.EmbeddableNestedLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestEmbeddableNestedServlet")
public class TestEmbeddableNestedServlet extends JPATestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "EMBEDDABLE_NESTED_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "EMBEDDABLE_NESTED_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "EMBEDDABLE_NESTED_RL")
    private EntityManagerFactory amrlEmf;

    @PostConstruct
    private void initFAT() {
        testClassName = EmbeddableNestedLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    // testEmbeddableNested01
    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested01_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested01_AMJTA_Web";
        final String testMethod = "testEmbeddableNested01";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested01_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested01_AMRL_Web";
        final String testMethod = "testEmbeddableNested01";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested01_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested01_CMTS_Web";
        final String testMethod = "testEmbeddableNested01";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested02
    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested02_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested02_AMJTA_Web";
        final String testMethod = "testEmbeddableNested02";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested02_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested02_AMRL_Web";
        final String testMethod = "testEmbeddableNested02";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested02_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested02_CMTS_Web";
        final String testMethod = "testEmbeddableNested02";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested03
    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested03_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested03_AMJTA_Web";
        final String testMethod = "testEmbeddableNested03";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested03_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested03_AMRL_Web";
        final String testMethod = "testEmbeddableNested03";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested03_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested03_CMTS_Web";
        final String testMethod = "testEmbeddableNested03";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested04
    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested04_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested04_AMJTA_Web";
        final String testMethod = "testEmbeddableNested04";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested04_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested04_AMRL_Web";
        final String testMethod = "testEmbeddableNested04";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested04_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested04_CMTS_Web";
        final String testMethod = "testEmbeddableNested04";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested05
    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested05_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested05_AMJTA_Web";
        final String testMethod = "testEmbeddableNested05";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested05_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested05_AMRL_Web";
        final String testMethod = "testEmbeddableNested05";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested05_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested05_CMTS_Web";
        final String testMethod = "testEmbeddableNested05";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested06
    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested06_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested06_AMJTA_Web";
        final String testMethod = "testEmbeddableNested06";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested06_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested06_AMRL_Web";
        final String testMethod = "testEmbeddableNested06";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested06_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested06_CMTS_Web";
        final String testMethod = "testEmbeddableNested06";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested07
    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested07_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested07_AMJTA_Web";
        final String testMethod = "testEmbeddableNested07";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested07_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested07_AMRL_Web";
        final String testMethod = "testEmbeddableNested07";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested07_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested07_CMTS_Web";
        final String testMethod = "testEmbeddableNested07";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested08
    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested08_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested08_AMJTA_Web";
        final String testMethod = "testEmbeddableNested08";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested08_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested08_AMRL_Web";
        final String testMethod = "testEmbeddableNested08";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested08_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested08_CMTS_Web";
        final String testMethod = "testEmbeddableNested08";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested09
    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested09_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested09_AMJTA_Web";
        final String testMethod = "testEmbeddableNested09";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested09_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested09_AMRL_Web";
        final String testMethod = "testEmbeddableNested09";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested09_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested09_CMTS_Web";
        final String testMethod = "testEmbeddableNested09";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested10
    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested10_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested10_AMJTA_Web";
        final String testMethod = "testEmbeddableNested10";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested10_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested10_AMRL_Web";
        final String testMethod = "testEmbeddableNested10";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested10_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested10_CMTS_Web";
        final String testMethod = "testEmbeddableNested10";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested11
    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested11_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested11_AMJTA_Web";
        final String testMethod = "testEmbeddableNested11";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested11_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested11_AMRL_Web";
        final String testMethod = "testEmbeddableNested11";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested11_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested11_CMTS_Web";
        final String testMethod = "testEmbeddableNested11";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested12
    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested12_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested12_AMJTA_Web";
        final String testMethod = "testEmbeddableNested12";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested12_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested12_AMRL_Web";
        final String testMethod = "testEmbeddableNested12";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested12_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested12_CMTS_Web";
        final String testMethod = "testEmbeddableNested12";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested13
    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested13_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested13_AMJTA_Web";
        final String testMethod = "testEmbeddableNested13";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested13_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested13_AMRL_Web";
        final String testMethod = "testEmbeddableNested13";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested13_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested13_CMTS_Web";
        final String testMethod = "testEmbeddableNested13";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested14
    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested14_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested14_AMJTA_Web";
        final String testMethod = "testEmbeddableNested14";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested14_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested14_AMRL_Web";
        final String testMethod = "testEmbeddableNested14";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested14_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested14_CMTS_Web";
        final String testMethod = "testEmbeddableNested14";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested15
    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested15_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested15_AMJTA_Web";
        final String testMethod = "testEmbeddableNested15";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested15_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested15_AMRL_Web";
        final String testMethod = "testEmbeddableNested15";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested15_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested15_CMTS_Web";
        final String testMethod = "testEmbeddableNested15";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested16
    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested16_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested16_AMJTA_Web";
        final String testMethod = "testEmbeddableNested16";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested16_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested16_AMRL_Web";
        final String testMethod = "testEmbeddableNested16";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested16_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested16_CMTS_Web";
        final String testMethod = "testEmbeddableNested16";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested17
    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested17_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested17_AMJTA_Web";
        final String testMethod = "testEmbeddableNested17";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested17_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested17_AMRL_Web";
        final String testMethod = "testEmbeddableNested17";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested17_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested17_CMTS_Web";
        final String testMethod = "testEmbeddableNested17";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested18
    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested18_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested18_AMJTA_Web";
        final String testMethod = "testEmbeddableNested18";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested18_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested18_AMRL_Web";
        final String testMethod = "testEmbeddableNested18";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested18_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested18_CMTS_Web";
        final String testMethod = "testEmbeddableNested18";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
