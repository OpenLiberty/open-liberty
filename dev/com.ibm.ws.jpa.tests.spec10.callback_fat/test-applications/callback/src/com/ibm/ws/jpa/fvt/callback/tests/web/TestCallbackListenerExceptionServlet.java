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
import com.ibm.ws.jpa.fvt.callback.entities.listener.ano.AnoListenerMSCEntity;
import com.ibm.ws.jpa.fvt.callback.entities.listener.xml.XMLListenerEntity;
import com.ibm.ws.jpa.fvt.callback.entities.listener.xml.XMLListenerMSCEntity;
import com.ibm.ws.jpa.fvt.callback.testlogic.CallbackRuntimeExceptionTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestCallbackListenerExceptionServlet")
public class TestCallbackListenerExceptionServlet extends JPATestServlet {
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
        testClassName = CallbackRuntimeExceptionTestLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    /*
     * Test Callback Function when a RuntimeException is thrown by the callback method.
     * Verify when appropriate that the transaction is still active and is marked for rollback.
     *
     * Combination Patterns:
     * Callback Method Protection Type: Package, Private, Protected, Public
     * Entity Declaration: Annotation, XML-ORM
     * Persistence Context Type: AM-JTA, AM-RL, CM-TS
     */

    // Package Protection

    @Test
    public void jpa10_CallbackListener_RuntimeException_PackageProtection_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_PackageProtection_001_Ano_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PACKAGE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_RuntimeException_PackageProtection_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_PackageProtection_001_XML_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PACKAGE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_RuntimeException_PackageProtection_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_PackageProtection_001_Ano_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PACKAGE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_RuntimeException_PackageProtection_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_PackageProtection_001_XML_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PACKAGE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_RuntimeException_PackageProtection_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_PackageProtection_001_Ano_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PACKAGE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_RuntimeException_PackageProtection_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_PackageProtection_001_XML_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PACKAGE");

        executeTest(testName, testMethod, testResource, properties);
    }

    // Private Protection

    @Test
    public void jpa10_CallbackListener_RuntimeException_PrivateProtection_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_PrivateProtection_001_Ano_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PRIVATE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_RuntimeException_PrivateProtection_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_PrivateProtection_001_XML_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PRIVATE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_RuntimeException_PrivateProtection_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_PrivateProtection_001_Ano_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PRIVATE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_RuntimeException_PrivateProtection_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_PrivateProtection_001_XML_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PRIVATE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_RuntimeException_PrivateProtection_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_PrivateProtection_001_Ano_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PRIVATE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_RuntimeException_PrivateProtection_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_PrivateProtection_001_XML_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PRIVATE");

        executeTest(testName, testMethod, testResource, properties);
    }

    // Protected Protection

    @Test
    public void jpa10_CallbackListener_RuntimeException_ProtectedProtection_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_ProtectedProtection_001_Ano_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PROTECTED");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_RuntimeException_ProtectedProtection_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_ProtectedProtection_001_XML_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PROTECTED");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_RuntimeException_ProtectedProtection_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_ProtectedProtection_001_Ano_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PROTECTED");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_RuntimeException_ProtectedProtection_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_ProtectedProtection_001_XML_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PROTECTED");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_RuntimeException_ProtectedProtection_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_ProtectedProtection_001_Ano_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PROTECTED");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_RuntimeException_ProtectedProtection_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_ProtectedProtection_001_XML_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PROTECTED");

        executeTest(testName, testMethod, testResource, properties);
    }

    // Public Protection

    @Test
    public void jpa10_CallbackListener_RuntimeException_PublicProtection_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_PublicProtection_001_Ano_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PUBLIC");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_RuntimeException_PublicProtection_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_PublicProtection_001_XML_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PUBLIC");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_RuntimeException_PublicProtection_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_PublicProtection_001_Ano_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PUBLIC");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_RuntimeException_PublicProtection_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_PublicProtection_001_XML_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PUBLIC");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_RuntimeException_PublicProtection_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_PublicProtection_001_Ano_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PUBLIC");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_RuntimeException_PublicProtection_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_RuntimeException_PublicProtection_001_XML_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PUBLIC");

        executeTest(testName, testMethod, testResource, properties);
    }

    // Package Protection

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_PackageProtection_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_PackageProtection_001_Ano_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PACKAGE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_PackageProtection_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_PackageProtection_001_XML_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PACKAGE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_PackageProtection_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_PackageProtection_001_Ano_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PACKAGE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_PackageProtection_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_PackageProtection_001_XML_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PACKAGE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_PackageProtection_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_PackageProtection_001_Ano_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PACKAGE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_PackageProtection_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_PackageProtection_001_XML_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PACKAGE");

        executeTest(testName, testMethod, testResource, properties);
    }

    // Private Protection

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_PrivateProtection_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_PrivateProtection_001_Ano_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PRIVATE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_PrivateProtection_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_PrivateProtection_001_XML_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PRIVATE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_PrivateProtection_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_PrivateProtection_001_Ano_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PRIVATE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_PrivateProtection_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_PrivateProtection_001_XML_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PRIVATE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_PrivateProtection_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_PrivateProtection_001_Ano_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PRIVATE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_PrivateProtection_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_PrivateProtection_001_XML_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PRIVATE");

        executeTest(testName, testMethod, testResource, properties);
    }

    // Protected Protection

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_ProtectedProtection_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_ProtectedProtection_001_Ano_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PROTECTED");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_ProtectedProtection_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_ProtectedProtection_001_XML_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PROTECTED");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_ProtectedProtection_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_ProtectedProtection_001_Ano_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PROTECTED");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_ProtectedProtection_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_ProtectedProtection_001_XML_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PROTECTED");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_ProtectedProtection_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_ProtectedProtection_001_Ano_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PROTECTED");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_ProtectedProtection_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_ProtectedProtection_001_XML_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PROTECTED");

        executeTest(testName, testMethod, testResource, properties);
    }

    // Public Protection

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_PublicProtection_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_PublicProtection_001_Ano_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PUBLIC");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_PublicProtection_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_PublicProtection_001_XML_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PUBLIC");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_PublicProtection_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_PublicProtection_001_Ano_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PUBLIC");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_PublicProtection_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_PublicProtection_001_XML_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PUBLIC");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_PublicProtection_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_PublicProtection_001_Ano_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PUBLIC");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_CallbackListener_MSC_RuntimeException_PublicProtection_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_CallbackListener_MSC_RuntimeException_PublicProtection_001_XML_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException003";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLListenerMSCEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PUBLIC");

        executeTest(testName, testMethod, testResource, properties);
    }

}
