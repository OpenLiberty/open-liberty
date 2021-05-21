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

package com.ibm.ws.jpa.embeddable.nested.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.embeddable.nested.testlogic.EmbeddableNestedLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestEmbeddableNested_EJB_SL_Servlet")
public class TestEmbeddableNested_EJB_SL_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = EmbeddableNestedLogic.class.getName();
        ejbJNDIName = "ejb/EmbeddableNestedSLEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Embeddable_Nested_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Embeddable_Nested_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Embeddable_Nested_CMTS"));
    }

    // testEmbeddableNested01

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested01_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested01_EJB_SL_AMJTA_Web";
        final String testMethod = "testEmbeddableNested01";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested01_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested01_EJB_SL_AMRL_Web";
        final String testMethod = "testEmbeddableNested01";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested01_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested01_EJB_SL_CMTS_Web";
        final String testMethod = "testEmbeddableNested01";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested02

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested02_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested02_EJB_SL_AMJTA_Web";
        final String testMethod = "testEmbeddableNested02";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested02_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested02_EJB_SL_AMRL_Web";
        final String testMethod = "testEmbeddableNested02";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested02_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested02_EJB_SL_CMTS_Web";
        final String testMethod = "testEmbeddableNested02";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested03

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested03_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested03_EJB_SL_AMJTA_Web";
        final String testMethod = "testEmbeddableNested03";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested03_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested03_EJB_SL_AMRL_Web";
        final String testMethod = "testEmbeddableNested03";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested03_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested03_EJB_SL_CMTS_Web";
        final String testMethod = "testEmbeddableNested03";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested04

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested04_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested04_EJB_SL_AMJTA_Web";
        final String testMethod = "testEmbeddableNested04";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested04_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested04_EJB_SL_AMRL_Web";
        final String testMethod = "testEmbeddableNested04";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested04_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested04_EJB_SL_CMTS_Web";
        final String testMethod = "testEmbeddableNested04";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested05

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested05_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested05_EJB_SL_AMJTA_Web";
        final String testMethod = "testEmbeddableNested05";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested05_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested05_EJB_SL_AMRL_Web";
        final String testMethod = "testEmbeddableNested05";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested05_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested05_EJB_SL_CMTS_Web";
        final String testMethod = "testEmbeddableNested05";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested06

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested06_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested06_EJB_SL_AMJTA_Web";
        final String testMethod = "testEmbeddableNested06";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested06_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested06_EJB_SL_AMRL_Web";
        final String testMethod = "testEmbeddableNested06";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested06_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested06_EJB_SL_CMTS_Web";
        final String testMethod = "testEmbeddableNested06";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested07

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested07_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested07_EJB_SL_AMJTA_Web";
        final String testMethod = "testEmbeddableNested07";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested07_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested07_EJB_SL_AMRL_Web";
        final String testMethod = "testEmbeddableNested07";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested07_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested07_EJB_SL_CMTS_Web";
        final String testMethod = "testEmbeddableNested07";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested08

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested08_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested08_EJB_SL_AMJTA_Web";
        final String testMethod = "testEmbeddableNested08";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested08_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested08_EJB_SL_AMRL_Web";
        final String testMethod = "testEmbeddableNested08";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested08_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested08_EJB_SL_CMTS_Web";
        final String testMethod = "testEmbeddableNested08";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested09

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested09_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested09_EJB_SL_AMJTA_Web";
        final String testMethod = "testEmbeddableNested09";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested09_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested09_EJB_SL_AMRL_Web";
        final String testMethod = "testEmbeddableNested09";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested09_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested09_EJB_SL_CMTS_Web";
        final String testMethod = "testEmbeddableNested09";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested10

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested10_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested10_EJB_SL_AMJTA_Web";
        final String testMethod = "testEmbeddableNested10";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested10_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested10_EJB_SL_AMRL_Web";
        final String testMethod = "testEmbeddableNested10";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested10_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested10_EJB_SL_CMTS_Web";
        final String testMethod = "testEmbeddableNested10";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested11

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested11_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested11_EJB_SL_AMJTA_Web";
        final String testMethod = "testEmbeddableNested11";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested11_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested11_EJB_SL_AMRL_Web";
        final String testMethod = "testEmbeddableNested11";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested11_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested11_EJB_SL_CMTS_Web";
        final String testMethod = "testEmbeddableNested11";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested12

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested12_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested12_EJB_SL_AMJTA_Web";
        final String testMethod = "testEmbeddableNested12";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested12_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested12_EJB_SL_AMRL_Web";
        final String testMethod = "testEmbeddableNested12";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested12_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested12_EJB_SL_CMTS_Web";
        final String testMethod = "testEmbeddableNested12";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested13

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested13_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested13_EJB_SL_AMJTA_Web";
        final String testMethod = "testEmbeddableNested13";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested13_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested13_EJB_SL_AMRL_Web";
        final String testMethod = "testEmbeddableNested13";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested13_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested13_EJB_SL_CMTS_Web";
        final String testMethod = "testEmbeddableNested13";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested14

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested14_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested14_EJB_SL_AMJTA_Web";
        final String testMethod = "testEmbeddableNested14";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested14_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested14_EJB_SL_AMRL_Web";
        final String testMethod = "testEmbeddableNested14";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested14_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested14_EJB_SL_CMTS_Web";
        final String testMethod = "testEmbeddableNested14";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested15

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested15_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested15_EJB_SL_AMJTA_Web";
        final String testMethod = "testEmbeddableNested15";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested15_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested15_EJB_SL_AMRL_Web";
        final String testMethod = "testEmbeddableNested15";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested15_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested15_EJB_SL_CMTS_Web";
        final String testMethod = "testEmbeddableNested15";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested16

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested16_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested16_EJB_SL_AMJTA_Web";
        final String testMethod = "testEmbeddableNested16";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested16_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested16_EJB_SL_AMRL_Web";
        final String testMethod = "testEmbeddableNested16";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested16_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested16_EJB_SL_CMTS_Web";
        final String testMethod = "testEmbeddableNested16";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested17

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested17_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested17_EJB_SL_AMJTA_Web";
        final String testMethod = "testEmbeddableNested17";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested17_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested17_EJB_SL_AMRL_Web";
        final String testMethod = "testEmbeddableNested17";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested17_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested17_EJB_SL_CMTS_Web";
        final String testMethod = "testEmbeddableNested17";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableNested18

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested18_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested18_EJB_SL_AMJTA_Web";
        final String testMethod = "testEmbeddableNested18";
        final String testResource = "test-jpa-resource-amjta";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested18_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested18_EJB_SL_AMRL_Web";
        final String testMethod = "testEmbeddableNested18";
        final String testResource = "test-jpa-resource-amrl";
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_nested_testEmbeddableNested18_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_nested_testEmbeddableNested18_EJB_SL_CMTS_Web";
        final String testMethod = "testEmbeddableNested18";
        final String testResource = "test-jpa-resource-cmts";
        executeTest(testName, testMethod, testResource);
    }
}
