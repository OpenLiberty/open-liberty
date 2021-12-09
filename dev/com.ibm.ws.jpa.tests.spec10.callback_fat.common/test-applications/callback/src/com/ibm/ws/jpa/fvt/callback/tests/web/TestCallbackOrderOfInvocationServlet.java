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

import com.ibm.ws.jpa.fvt.callback.entities.orderofinvocation.ano.AnoOOILeafPackageEntity;
import com.ibm.ws.jpa.fvt.callback.entities.orderofinvocation.ano.AnoOOILeafPrivateEntity;
import com.ibm.ws.jpa.fvt.callback.entities.orderofinvocation.ano.AnoOOILeafProtectedEntity;
import com.ibm.ws.jpa.fvt.callback.entities.orderofinvocation.ano.AnoOOILeafPublicEntity;
import com.ibm.ws.jpa.fvt.callback.entities.orderofinvocation.xml.XMLOOILeafPackageEntity;
import com.ibm.ws.jpa.fvt.callback.entities.orderofinvocation.xml.XMLOOILeafPrivateEntity;
import com.ibm.ws.jpa.fvt.callback.entities.orderofinvocation.xml.XMLOOILeafProtectedEntity;
import com.ibm.ws.jpa.fvt.callback.entities.orderofinvocation.xml.XMLOOILeafPublicEntity;
import com.ibm.ws.jpa.fvt.callback.testlogic.CallbackOrderOfInvocationTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestCallbackOrderOfInvocationServlet")
public class TestCallbackOrderOfInvocationServlet extends JPATestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "Callback-OrderOfInvocation_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "Callback-OrderOfInvocation_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "Callback-OrderOfInvocation_RL")
    private EntityManagerFactory amrlEmf;

    @PostConstruct
    private void initFAT() {
        testClassName = CallbackOrderOfInvocationTestLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    /*
     * Test Order of Invocation
     *
     * Verify that the order of invocation of callback methods, as defined by the JPA Specification
     * section 3.5.4, is demonstrated:
     *
     * Default Listener, invoked in the order they are defined in the XML Mapping File
     * Entity Listeners defined by the EntityListener annotation on an Entity class or Mapped Superclass (in the order of appearance in the annotation).
     * With inheritance, the order of invocation starts at the highest superclass defining an EntityListener, moving down to the leaf entity class.
     * Lifecycle methods defined by entity classes and mapped superclasses are invoked in the order from highest superclass to the leaf entity class
     *
     * To verify this, the test will execute in the following environment:
     *
     * Default Entity Listener: DefaultListener1 and DefaultListener2, defined in that order in the XML Mapping File
     * Abstract Entity using Table-Per-Class inheritance methodology, with the following:
     * EntityListenerA1, EntityListenerA2, defined in that order
     * Callback methods for each lifecycle type (A_PrePersist, A_PostPersist, etc.)
     * Mapped Superclass with the following:
     * EntityListenerB1, EntityListenerB2, defined in that order
     * Callback methods for each lifecycle type (B_PrePersist, B_PostPersist, etc.)
     * Leaf entity with the following:
     * EntityListenerC1, EntityListenerC2, defined in that order
     * Callback methods for each lifecycle type (C_PrePersist, C_PostPersist, etc.)
     *
     * For each callback type, the following invocation order is expected:
     * DefaultCallbackListener[ProtType]G1
     * DefaultCallbackListener[ProtType]G2
     * [EntType]CallbackListener[ProtType]A1
     * [EntType]CallbackListener[ProtType]A2
     * [EntType]CallbackListener[ProtType]B1
     * [EntType]CallbackListener[ProtType]B2
     * [EntType]CallbackListener[ProtType]C1
     * [EntType]CallbackListener[ProtType]C2
     * [EntType]OOIRoot[ProtType]Entity
     * [EntType]OOIMSC[ProtType]Entity
     * [EntType]OOILeaf[ProtType]Entity
     *
     * Where [ProtType] = Package|Private|Protected|Public
     * Where [EntType] = Ano|XML
     *
     * Combination Patterns:
     * Callback Method Protection Type: Package, Private, Protected, Public
     * Entity Declaration: Annotation, XML-ORM
     * Persistence Context Type: AM-JTA, AM-RL, CM-TS
     */

    // Package Protection

    @Test
    public void jpa10_Callback_OrderOfInvocation_PackageProtection_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PackageProtection_001_Ano_AMJTA_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoOOILeafPackageEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PACKAGE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_PackageProtection_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PackageProtection_001_XML_AMJTA_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLOOILeafPackageEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PACKAGE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_PackageProtection_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PackageProtection_001_Ano_AMRL_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoOOILeafPackageEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PACKAGE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_PackageProtection_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PackageProtection_001_XML_AMRL_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLOOILeafPackageEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PACKAGE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_PackageProtection_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PackageProtection_001_Ano_CMTS_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoOOILeafPackageEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PACKAGE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_PackageProtection_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PackageProtection_001_XML_CMTS_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLOOILeafPackageEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PACKAGE");

        executeTest(testName, testMethod, testResource, properties);
    }

    // Private Protection

    @Test
    public void jpa10_Callback_OrderOfInvocation_PrivateProtection_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PrivateProtection_001_Ano_AMJTA_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoOOILeafPrivateEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PRIVATE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_PrivateProtection_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PrivateProtection_001_XML_AMJTA_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLOOILeafPrivateEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PRIVATE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_PrivateProtection_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PrivateProtection_001_Ano_AMRL_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoOOILeafPrivateEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PRIVATE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_PrivateProtection_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PrivateProtection_001_XML_AMRL_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLOOILeafPrivateEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PRIVATE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_PrivateProtection_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PrivateProtection_001_Ano_CMTS_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoOOILeafPrivateEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PRIVATE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_PrivateProtection_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PrivateProtection_001_XML_CMTS_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLOOILeafPrivateEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PRIVATE");

        executeTest(testName, testMethod, testResource, properties);
    }

    // Protected Protection

    @Test
    public void jpa10_Callback_OrderOfInvocation_ProtectedProtection_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_ProtectedProtection_001_Ano_AMJTA_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoOOILeafProtectedEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PROTECTED");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_ProtectedProtection_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_ProtectedProtection_001_XML_AMJTA_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLOOILeafProtectedEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PROTECTED");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_ProtectedProtection_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_ProtectedProtection_001_Ano_AMRL_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoOOILeafProtectedEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PROTECTED");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_ProtectedProtection_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_ProtectedProtection_001_XML_AMRL_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLOOILeafProtectedEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PROTECTED");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_ProtectedProtection_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_ProtectedProtection_001_Ano_CMTS_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoOOILeafProtectedEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PROTECTED");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_ProtectedProtection_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_ProtectedProtection_001_XML_CMTS_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLOOILeafProtectedEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PROTECTED");

        executeTest(testName, testMethod, testResource, properties);
    }

    // Public Protection

    @Test
    public void jpa10_Callback_OrderOfInvocation_PublicProtection_001_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PublicProtection_001_Ano_AMJTA_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoOOILeafPublicEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PUBLIC");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_PublicProtection_001_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PublicProtection_001_XML_AMJTA_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLOOILeafPublicEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PUBLIC");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_PublicProtection_001_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PublicProtection_001_Ano_AMRL_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoOOILeafPublicEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PUBLIC");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_PublicProtection_001_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PublicProtection_001_XML_AMRL_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLOOILeafPublicEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PUBLIC");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_PublicProtection_001_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PublicProtection_001_Ano_CMTS_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", AnoOOILeafPublicEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PUBLIC");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_PublicProtection_001_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PublicProtection_001_XML_CMTS_Web";
        final String testMethod = "testOrderOfInvocation001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLOOILeafPublicEntity.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PUBLIC");

        executeTest(testName, testMethod, testResource, properties);
    }

}
