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

import com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.ano.CallbackPackageEntity;
import com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.ano.CallbackPrivateEntity;
import com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.ano.CallbackProtectedEntity;
import com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.ano.CallbackPublicEntity;
import com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.mappedsuperclass.ano.CallbackPackageMSCEntity;
import com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.mappedsuperclass.ano.CallbackPrivateMSCEntity;
import com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.mappedsuperclass.ano.CallbackProtectedMSCEntity;
import com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.mappedsuperclass.ano.CallbackPublicMSCEntity;
import com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.mappedsuperclass.xml.XMLCallbackPackageMSCEntity;
import com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.mappedsuperclass.xml.XMLCallbackPrivateMSCEntity;
import com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.mappedsuperclass.xml.XMLCallbackProtectedMSCEntity;
import com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.mappedsuperclass.xml.XMLCallbackPublicMSCEntity;
import com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.xml.XMLCallbackPackageEntity;
import com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.xml.XMLCallbackPrivateEntity;
import com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.xml.XMLCallbackProtectedEntity;
import com.ibm.ws.jpa.fvt.callback.entities.entitydeclared.xml.XMLCallbackPublicEntity;
import com.ibm.ws.jpa.fvt.callback.testlogic.CallbackRuntimeExceptionTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestCallbackExceptionServlet")
public class TestCallbackExceptionServlet extends JPATestServlet {
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
    public void jpa10_Callback_RuntimeException_EntityDeclared_PackageProtection_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_PackageProtection_001_Ano_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPackageEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_PackageProtection_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_PackageProtection_001_XML_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPackageEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_PackageProtection_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_PackageProtection_001_Ano_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPackageEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_PackageProtection_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_PackageProtection_001_XML_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPackageEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_PackageProtection_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_PackageProtection_001_Ano_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPackageEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_PackageProtection_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_PackageProtection_001_XML_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPackageEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Private Protection

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_PrivateProtection_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_PrivateProtection_001_Ano_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPrivateEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_PrivateProtection_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_PrivateProtection_001_XML_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPrivateEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_PrivateProtection_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_PrivateProtection_001_Ano_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPrivateEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_PrivateProtection_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_PrivateProtection_001_XML_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPrivateEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_PrivateProtection_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_PrivateProtection_001_Ano_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPrivateEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_PrivateProtection_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_PrivateProtection_001_XML_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPrivateEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Protected Protection

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_ProtectedProtection_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_ProtectedProtection_001_Ano_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackProtectedEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_ProtectedProtection_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_ProtectedProtection_001_XML_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackProtectedEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_ProtectedProtection_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_ProtectedProtection_001_Ano_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackProtectedEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_ProtectedProtection_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_ProtectedProtection_001_XML_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackProtectedEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_ProtectedProtection_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_ProtectedProtection_001_Ano_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackProtectedEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_ProtectedProtection_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_ProtectedProtection_001_XML_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackProtectedEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Public Protection

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_PublicProtection_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_PublicProtection_001_Ano_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPublicEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_PublicProtection_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_PublicProtection_001_XML_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPublicEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_PublicProtection_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_PublicProtection_001_Ano_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPublicEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_PublicProtection_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_PublicProtection_001_XML_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPublicEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_PublicProtection_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_PublicProtection_001_Ano_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPublicEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_PublicProtection_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_PublicProtection_001_XML_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPublicEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Test Basic Callback Function. Verify that a callback method on an entity where callback methods are defined
     * on a MappedSuperclass are called when appropriate in an its lifecycle.
     *
     * Combination Patterns:
     * Callback Method Protection Type: Package, Private, Protected, Public
     * Entity Declaration: Annotation, XML-ORM
     * Persistence Context Type: AM-JTA, AM-RL, CM-TS
     */

    // Package Protection

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_PackageProtection_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_PackageProtection_001_Ano_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPackageMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_PackageProtection_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_PackageProtection_001_XML_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPackageMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_PackageProtection_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_PackageProtection_001_Ano_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPackageMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_PackageProtection_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_PackageProtection_001_XML_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPackageMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_PackageProtection_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_PackageProtection_001_Ano_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPackageMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_PackageProtection_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_PackageProtection_001_XML_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPackageMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Private Protection

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_PrivateProtection_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_PrivateProtection_001_Ano_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPrivateMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_PrivateProtection_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_PrivateProtection_001_XML_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPrivateMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_PrivateProtection_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_PrivateProtection_001_Ano_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPrivateMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_PrivateProtection_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_PrivateProtection_001_XML_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPrivateMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_PrivateProtection_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_PrivateProtection_001_Ano_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPrivateMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_PrivateProtection_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_PrivateProtection_001_XML_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPrivateMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Protected Protection

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_ProtectedProtection_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_ProtectedProtection_001_Ano_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackProtectedMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_ProtectedProtection_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_ProtectedProtection_001_XML_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackProtectedMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_ProtectedProtection_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_ProtectedProtection_001_Ano_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackProtectedMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_ProtectedProtection_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_ProtectedProtection_001_XML_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackProtectedMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_ProtectedProtection_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_ProtectedProtection_001_Ano_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackProtectedMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_ProtectedProtection_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_ProtectedProtection_001_XML_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackProtectedMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Public Protection

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_PublicProtection_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_PublicProtection_001_Ano_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPublicMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_PublicProtection_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_PublicProtection_001_XML_AMJTA_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPublicMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_PublicProtection_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_PublicProtection_001_Ano_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPublicMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_PublicProtection_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_PublicProtection_001_XML_AMRL_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPublicMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_PublicProtection_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_PublicProtection_001_Ano_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPublicMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_RuntimeException_EntityDeclared_MSC_PublicProtection_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_RuntimeException_EntityDeclared_MSC_PublicProtection_001_XML_CMTS_Web";
        final String testMethod = "testCallbackRuntimeException001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPublicMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }
}
