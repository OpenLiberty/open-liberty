/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.callback.tests.web;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.callback.entities.listener.ano.AnoListenerEntity;
import com.ibm.ws.jpa.fvt.callback.entities.listener.ano.AnoListenerExcludeMSCEntity;
import com.ibm.ws.jpa.fvt.callback.entities.listener.ano.AnoListenerMSCEntity;
import com.ibm.ws.jpa.fvt.callback.entities.listener.xml.XMLListenerEntity;
import com.ibm.ws.jpa.fvt.callback.entities.listener.xml.XMLListenerExcludeMSCEntity;
import com.ibm.ws.jpa.fvt.callback.entities.listener.xml.XMLListenerMSCEntity;
import com.ibm.ws.jpa.fvt.callback.testlogic.CallbackTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestCallbackListenerServlet")
public class TestCallbackListenerServlet extends JPATestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "Callback_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "Callback_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "Callback_RL")
    private EntityManagerFactory amrlEmf;

    @PostConstruct
    private void initFAT() {
        testClassName = CallbackTestLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    /*
     * Test Basic Callback Function. Verify that a callback method on an entity-declared listener is called
     * when appropriate in an its lifecycle.
     *
     * Combination Patterns:
     * Callback Method Protection Type: Package, Private, Protected, Public
     * Entity Declaration: Annotation, XML-ORM
     * Persistence Context Type: AM-JTA, AM-RL, CM-TS
     */

    @Test
    public void jpa10_CallbackListener_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_001_Ano_AMJTA_Web";
        final String testMethod = "testCallback004";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_001_XML_AMJTA_Web";
        final String testMethod = "testCallback004";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_001_Ano_AMRL_Web";
        final String testMethod = "testCallback004";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_001_XML_AMRL_Web";
        final String testMethod = "testCallback004";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_001_Ano_CMTS_Web";
        final String testMethod = "testCallback004";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_001_XML_CMTS_Web";
        final String testMethod = "testCallback004";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // MSC

    @Test
    public void jpa10_CallbackListener_MSC_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_001_Ano_AMJTA_Web";
        final String testMethod = "testCallback004";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_001_XML_AMJTA_Web";
        final String testMethod = "testCallback004";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_001_Ano_AMRL_Web";
        final String testMethod = "testCallback004";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_001_XML_AMRL_Web";
        final String testMethod = "testCallback004";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_001_Ano_CMTS_Web";
        final String testMethod = "testCallback004";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_001_XML_CMTS_Web";
        final String testMethod = "testCallback004";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Test Exclude Mapped-Superclass Defined Listeners Function. Verify that an entity marked with
     *
     * @ExcludeSuperclassListeners or the exclude-superclass-listeners XML element do not fire default listener
     * lifecycle methods.
     *
     * Combination Patterns:
     * Callback Method Protection Type: Package, Private, Protected, Public
     * Entity Declaration: Annotation, XML-ORM
     * Persistence Context Type: AM-JTA, AM-RL, CM-TS
     *
     */

    @Test
    public void jpa10_ExcludeSuperclassCallbackListener_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_ExcludeSuperclassCallbackListener_001_Ano_AMJTA_Web";
        final String testMethod = "testCallback005";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerExcludeMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_ExcludeSuperclassCallbackListener_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_ExcludeSuperclassCallbackListener_001_XML_AMJTA_Web";
        final String testMethod = "testCallback005";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerExcludeMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_ExcludeSuperclassCallbackListener_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_ExcludeSuperclassCallbackListener_001_Ano_AMRL_Web";
        final String testMethod = "testCallback005";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerExcludeMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_ExcludeSuperclassCallbackListener_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_ExcludeSuperclassCallbackListener_001_XML_AMRL_Web";
        final String testMethod = "testCallback005";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerExcludeMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_ExcludeSuperclassCallbackListener_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_ExcludeSuperclassCallbackListener_001_Ano_CMTS_Web";
        final String testMethod = "testCallback005";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerExcludeMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_ExcludeSuperclassCallbackListener_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_ExcludeSuperclassCallbackListener_001_XML_CMTS_Web";
        final String testMethod = "testCallback005";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerExcludeMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

}
