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
import com.ibm.ws.jpa.fvt.callback.testlogic.CallbackTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.testinfo.TestExecutionContext;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestCallback_EJB_SL_Servlet")
public class TestCallback_EJB_SL_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = CallbackTestLogic.class.getName();
        ejbJNDIName = "ejb/CallbackSLEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Callback_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Callback_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Callback_CMTS"));
    }

    /*
     * Test Basic Callback Function. Verify that a callback method on an entity is called when appropriate in an
     * its lifecycle.
     *
     * Combination Patterns:
     * Callback Method Protection Type: Package, Private, Protected, Public
     * Entity Declaration: Annotation, XML-ORM
     * Persistence Context Type: AM-JTA, AM-RL, CM-TS
     */

    // Package Protection

    @Test
    public void jpa10_Callback_EntityDeclared_PackageProtection_001_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_PackageProtection_001_Ano_AMJTA_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPackageEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_PackageProtection_001_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_PackageProtection_001_XML_AMJTA_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPackageEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_PackageProtection_001_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_PackageProtection_001_Ano_AMRL_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPackageEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_PackageProtection_001_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_PackageProtection_001_XML_AMRL_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPackageEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_PackageProtection_001_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_PackageProtection_001_Ano_CMTS_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPackageEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_PackageProtection_001_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_PackageProtection_001_XML_CMTS_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPackageEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Private Protection

    @Test
    public void jpa10_Callback_EntityDeclared_PrivateProtection_001_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_PrivateProtection_001_Ano_AMJTA_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPrivateEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_PrivateProtection_001_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_PrivateProtection_001_XML_AMJTA_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPrivateEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_PrivateProtection_001_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_PrivateProtection_001_Ano_AMRL_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPrivateEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_PrivateProtection_001_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_PrivateProtection_001_XML_AMRL_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPrivateEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_PrivateProtection_001_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_PrivateProtection_001_Ano_CMTS_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPrivateEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_PrivateProtection_001_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_PrivateProtection_001_XML_CMTS_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPrivateEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Protected Protection

    @Test
    public void jpa10_Callback_EntityDeclared_ProtectedProtection_001_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_ProtectedProtection_001_Ano_AMJTA_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackProtectedEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_ProtectedProtection_001_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_ProtectedProtection_001_XML_AMJTA_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amjta";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-amjta"));

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackProtectedEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_ProtectedProtection_001_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_ProtectedProtection_001_Ano_AMRL_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackProtectedEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_ProtectedProtection_001_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_ProtectedProtection_001_XML_AMRL_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackProtectedEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_ProtectedProtection_001_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_ProtectedProtection_001_Ano_CMTS_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackProtectedEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_ProtectedProtection_001_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_ProtectedProtection_001_XML_CMTS_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackProtectedEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Public Protection

    @Test
    public void jpa10_Callback_EntityDeclared_PublicProtection_001_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_PublicProtection_001_Ano_AMJTA_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPublicEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_PublicProtection_001_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_PublicProtection_001_XML_AMJTA_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPublicEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_PublicProtection_001_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_PublicProtection_001_Ano_AMRL_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPublicEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_PublicProtection_001_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_PublicProtection_001_XML_AMRL_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPublicEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_PublicProtection_001_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_PublicProtection_001_Ano_CMTS_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPublicEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_PublicProtection_001_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_PublicProtection_001_XML_CMTS_EJB_SL";
        final String testMethod = "testCallback001";
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
    public void jpa10_Callback_EntityDeclared_MSC_PackageProtection_001_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_PackageProtection_001_Ano_AMJTA_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPackageMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_MSC_PackageProtection_001_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_PackageProtection_001_XML_AMJTA_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPackageMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_MSC_PackageProtection_001_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_PackageProtection_001_Ano_AMRL_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPackageMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_MSC_PackageProtection_001_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_PackageProtection_001_XML_AMRL_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPackageMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_MSC_PackageProtection_001_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_PackageProtection_001_Ano_CMTS_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPackageMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_MSC_PackageProtection_001_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_PackageProtection_001_XML_CMTS_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPackageMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Private Protection

    @Test
    public void jpa10_Callback_EntityDeclared_MSC_PrivateProtection_001_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_PrivateProtection_001_Ano_AMJTA_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPrivateMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_MSC_PrivateProtection_001_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_PrivateProtection_001_XML_AMJTA_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPrivateMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_MSC_PrivateProtection_001_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_PrivateProtection_001_Ano_AMRL_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPrivateMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_MSC_PrivateProtection_001_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_PrivateProtection_001_XML_AMRL_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPrivateMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_MSC_PrivateProtection_001_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_PrivateProtection_001_Ano_CMTS_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPrivateMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_MSC_PrivateProtection_001_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_PrivateProtection_001_XML_CMTS_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPrivateMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Protected Protection

    @Test
    public void jpa10_Callback_EntityDeclared_MSC_ProtectedProtection_001_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_ProtectedProtection_001_Ano_AMJTA_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackProtectedMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_MSC_ProtectedProtection_001_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_ProtectedProtection_001_XML_AMJTA_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackProtectedMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_MSC_ProtectedProtection_001_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_ProtectedProtection_001_Ano_AMRL_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackProtectedMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_MSC_ProtectedProtection_001_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_ProtectedProtection_001_XML_AMRL_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackProtectedMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_MSC_ProtectedProtection_001_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_ProtectedProtection_001_Ano_CMTS_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackProtectedMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_MSC_ProtectedProtection_001_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_ProtectedProtection_001_XML_CMTS_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackProtectedMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    // Public Protection

    @Test
    public void jpa10_Callback_EntityDeclared_MSC_PublicProtection_001_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_PublicProtection_001_Ano_AMJTA_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPublicMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_MSC_PublicProtection_001_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_PublicProtection_001_XML_AMJTA_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPublicMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_MSC_PublicProtection_001_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_PublicProtection_001_Ano_AMRL_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPublicMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_MSC_PublicProtection_001_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_PublicProtection_001_XML_AMRL_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPublicMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_MSC_PublicProtection_001_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_PublicProtection_001_Ano_CMTS_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", CallbackPublicMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Callback_EntityDeclared_MSC_PublicProtection_001_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Callback_EntityDeclared_MSC_PublicProtection_001_XML_CMTS_EJB_SL";
        final String testMethod = "testCallback001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLCallbackPublicMSCEntity.class.getSimpleName());

        executeTest(testName, testMethod, testResource, properties);
    }
}
