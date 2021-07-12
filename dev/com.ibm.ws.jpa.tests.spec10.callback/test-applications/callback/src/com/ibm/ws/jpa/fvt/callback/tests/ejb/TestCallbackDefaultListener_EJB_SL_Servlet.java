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

package com.ibm.ws.jpa.fvt.callback.tests.ejb;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.callback.entities.defaultlistener.EntityNotSupportingDefaultCallbacks;
import com.ibm.ws.jpa.fvt.callback.entities.defaultlistener.EntitySupportingDefaultCallbacks;
import com.ibm.ws.jpa.fvt.callback.entities.defaultlistener.XMLEntityNotSupportingDefaultCallbacks;
import com.ibm.ws.jpa.fvt.callback.entities.defaultlistener.XMLEntitySupportingDefaultCallbacks;
import com.ibm.ws.jpa.fvt.callback.testlogic.CallbackTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestCallbackDefaultListener_EJB_SL_Servlet")
public class TestCallbackDefaultListener_EJB_SL_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = CallbackTestLogic.class.getName();
        ejbJNDIName = "ejb/CallbackSLEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Callback-DefaultListener_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Callback-DefaultListener_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Callback-DefaultListener_CMTS"));
    }

    /*
     * Test Basic Callback Function. Verify that a callback method on an listener is called when appropriate in an
     * its lifecycle.
     *
     * Combination Patterns:
     * Callback Method Protection Type: Package, Private, Protected, Public
     * Entity Declaration: Annotation, XML-ORM
     * Persistence Context Type: AM-JTA, AM-RL, CM-TS
     */

    @Test
    public void jpa10_CallbackDefaultListener_001_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_CallbackDefaultListener_001_Ano_AMJTA_EJB_SL";
        final String testMethod = "testCallback002";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", EntitySupportingDefaultCallbacks.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackDefaultListener_001_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_CallbackDefaultListener_001_XML_AMJTA_EJB_SL";
        final String testMethod = "testCallback002";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLEntitySupportingDefaultCallbacks.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackDefaultListener_001_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_CallbackDefaultListener_001_Ano_AMRL_EJB_SL";
        final String testMethod = "testCallback002";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", EntitySupportingDefaultCallbacks.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackDefaultListener_001_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_CallbackDefaultListener_001_XML_AMRL_EJB_SL";
        final String testMethod = "testCallback002";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLEntitySupportingDefaultCallbacks.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackDefaultListener_001_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_CallbackDefaultListener_001_Ano_CMTS_EJB_SL";
        final String testMethod = "testCallback002";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", EntitySupportingDefaultCallbacks.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackDefaultListener_001_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_CallbackDefaultListener_001_XML_CMTS_EJB_SL";
        final String testMethod = "testCallback002";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLEntitySupportingDefaultCallbacks.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Test Default Listener Exclusion Function. Verify that an entity marked with @ExcludeDefaultListeners or
     * the exclude-default-listeners XML element do not fire default listener lifecycle methods.
     *
     * Combination Patterns:
     * Callback Method Protection Type: Package, Private, Protected, Public
     * Entity Declaration: Annotation, XML-ORM
     * Persistence Context Type: AM-JTA, AM-RL, CM-TS
     *
     */

    @Test
    public void jpa10_ExcludeSuperclassCallbackDefaultListener_001_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_ExcludeSuperclassCallbackDefaultListener_001_Ano_AMJTA_EJB_SL";
        final String testMethod = "testCallback003";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", EntityNotSupportingDefaultCallbacks.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_ExcludeSuperclassCallbackDefaultListener_001_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_ExcludeSuperclassCallbackDefaultListener_001_XML_AMJTA_EJB_SL";
        final String testMethod = "testCallback003";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLEntityNotSupportingDefaultCallbacks.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_ExcludeSuperclassCallbackDefaultListener_001_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_ExcludeSuperclassCallbackDefaultListener_001_Ano_AMRL_EJB_SL";
        final String testMethod = "testCallback003";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", EntityNotSupportingDefaultCallbacks.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_ExcludeSuperclassCallbackDefaultListener_001_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_ExcludeSuperclassCallbackDefaultListener_001_XML_AMRL_EJB_SL";
        final String testMethod = "testCallback003";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLEntityNotSupportingDefaultCallbacks.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_ExcludeSuperclassCallbackDefaultListener_001_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_ExcludeSuperclassCallbackDefaultListener_001_Ano_CMTS_EJB_SL";
        final String testMethod = "testCallback003";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", EntityNotSupportingDefaultCallbacks.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_ExcludeSuperclassCallbackDefaultListener_001_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_ExcludeSuperclassCallbackDefaultListener_001_XML_CMTS_EJB_SL";
        final String testMethod = "testCallback003";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLEntityNotSupportingDefaultCallbacks.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

}
