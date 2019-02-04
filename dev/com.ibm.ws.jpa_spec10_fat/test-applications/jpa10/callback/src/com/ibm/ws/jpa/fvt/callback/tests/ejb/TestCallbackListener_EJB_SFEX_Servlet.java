/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestCallbackListener_EJB_SFEX_Servlet")
public class TestCallbackListener_EJB_SFEX_Servlet extends EJBTestVehicleServlet {
    private final String testLogicClassName = "com.ibm.ws.jpa.fvt.callback.testlogic.CallbackTestLogic";

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    private final static String ejbJNDIName = "ejb/CallbackSFExEJB";

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Callback_CMEX"));
        jpaPctxMap.put("cleanup",
                       new JPAPersistenceContext("cleanup", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/cleanup"));

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
    public void jpa10_CallbackListener_001_Ano_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_CallbackListener_001_Ano_CMEX_EJB_SF";
        final String testMethod = "testCallback004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmex"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoListenerEntity");

        executeDDL("JPA10_CALLBACK_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_CallbackListener_001_XML_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_CallbackListener_001_XML_CMEX_EJB_SF";
        final String testMethod = "testCallback004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmex"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLListenerEntity");

        executeDDL("JPA10_CALLBACK_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    // MSC

    @Test
    public void jpa10_CallbackListener_MSC_001_Ano_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_001_Ano_CMEX_EJB_SF";
        final String testMethod = "testCallback004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmex"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoListenerMSCEntity");

        executeDDL("JPA10_CALLBACK_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_CallbackListener_MSC_001_XML_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_001_XML_CMEX_EJB_SF";
        final String testMethod = "testCallback004";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmex"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLListenerMSCEntity");

        executeDDL("JPA10_CALLBACK_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
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
    public void jpa10_ExcludeSuperclassCallbackListener_001_Ano_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_ExcludeSuperclassCallbackListener_001_Ano_CMEX_EJB_SF";
        final String testMethod = "testCallback005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmex"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoListenerExcludeMSCEntity");

        executeDDL("JPA10_CALLBACK_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_ExcludeSuperclassCallbackListener_001_XML_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_ExcludeSuperclassCallbackListener_001_XML_CMEX_EJB_SF";
        final String testMethod = "testCallback005";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmex"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLListenerExcludeMSCEntity");

        executeDDL("JPA10_CALLBACK_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }
}
