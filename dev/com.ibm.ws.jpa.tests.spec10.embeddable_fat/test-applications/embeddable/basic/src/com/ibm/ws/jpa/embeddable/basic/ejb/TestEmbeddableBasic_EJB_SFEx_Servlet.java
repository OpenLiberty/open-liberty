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

package com.ibm.ws.jpa.embeddable.basic.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.embeddable.basic.testlogic.EmbeddableBasicLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestEmbeddableBasic_EJB_SFEx_Servlet")
public class TestEmbeddableBasic_EJB_SFEx_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = EmbeddableBasicLogic.class.getName();
        ejbJNDIName = "ejb/EmbeddableBasicSFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Embeddable_Basic_CMEX"));
    }

    // testEmbeddableBasic01
    @Test
    public void jpa_spec10_embeddable_basic_testEmbeddableBasic01_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_basic_testEmbeddableBasic01_EJB_SFEx_CMEX_Web";
        final String testMethod = "testEmbeddableBasic01";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableBasic02
    @Test
    public void jpa_spec10_embeddable_basic_testEmbeddableBasic02_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_basic_testEmbeddableBasic02_EJB_SFEx_CMEX_Web";
        final String testMethod = "testEmbeddableBasic02";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableBasic03
    @Test
    public void jpa_spec10_embeddable_basic_testEmbeddableBasic03_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_basic_testEmbeddableBasic03_EJB_SFEx_CMEX_Web";
        final String testMethod = "testEmbeddableBasic03";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

// TODO: Enable with delivery of OLGH16588
//    // testEmbeddableBasic04
//    @Test
//    public void jpa_spec10_embeddable_basic_testEmbeddableBasic04_EJB_SFEx_CMEX_Web() throws Exception {
//        final String testName = "jpa_spec10_embeddable_basic_testEmbeddableBasic04_EJB_SFEx_CMEX_Web";
//        final String testMethod = "testEmbeddableBasic04";
//        final String testResource = "test-jpa-resource-cmex";
//        executeTest(testName, testMethod, testResource);
//    }

    // testEmbeddableBasic05
    @Test
    public void jpa_spec10_embeddable_basic_testEmbeddableBasic05_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_basic_testEmbeddableBasic05_EJB_SFEx_CMEX_Web";
        final String testMethod = "testEmbeddableBasic05";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableBasic06
    @Test
    public void jpa_spec10_embeddable_basic_testEmbeddableBasic06_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_basic_testEmbeddableBasic06_EJB_SFEx_CMEX_Web";
        final String testMethod = "testEmbeddableBasic06";
        final String testResource = "test-jpa-resource-cmex";
        executeTest(testName, testMethod, testResource);
    }
}
