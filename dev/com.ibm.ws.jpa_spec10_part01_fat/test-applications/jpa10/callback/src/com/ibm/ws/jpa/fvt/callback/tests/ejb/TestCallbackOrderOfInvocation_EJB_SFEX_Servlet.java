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
@WebServlet(urlPatterns = "/TestCallbackOrderOfInvocation_EJB_SFEX_Servlet")
public class TestCallbackOrderOfInvocation_EJB_SFEX_Servlet extends EJBTestVehicleServlet {
    private final String testLogicClassName = "com.ibm.ws.jpa.fvt.callback.testlogic.CallbackOrderOfInvocationTestLogic";

    private final HashMap<String, JPAPersistenceContext> jpaPctxMap = new HashMap<String, JPAPersistenceContext>();

    private final static String ejbJNDIName = "ejb/CallbackSFExEJB";

    @PostConstruct
    private void initFAT() {
        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Callback-OrderOfInvocation_CMEX"));
        jpaPctxMap.put("cleanup",
                       new JPAPersistenceContext("cleanup", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/cleanup"));

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
    public void jpa10_Callback_OrderOfInvocation_PackageProtection_001_Ano_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PackageProtection_001_Ano_CMEX_EJB_SF";
        final String testMethod = "testOrderOfInvocation001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmex"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoOOILeafPackageEntity");
        properties.put("ListenerMethodProtectionType", "PT_PACKAGE");

        executeDDL("JPA10_CALLBACK_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_PackageProtection_001_XML_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PackageProtection_001_XML_CMEX_EJB_SF";
        final String testMethod = "testOrderOfInvocation001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmex"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLOOILeafPackageEntity");
        properties.put("ListenerMethodProtectionType", "PT_PACKAGE");

        executeDDL("JPA10_CALLBACK_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    // Private Protection

    @Test
    public void jpa10_Callback_OrderOfInvocation_PrivateProtection_001_Ano_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PrivateProtection_001_Ano_CMEX_EJB_SF";
        final String testMethod = "testOrderOfInvocation001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmex"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoOOILeafPrivateEntity");
        properties.put("ListenerMethodProtectionType", "PT_PRIVATE");

        executeDDL("JPA10_CALLBACK_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_PrivateProtection_001_XML_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PrivateProtection_001_XML_CMEX_EJB_SF";
        final String testMethod = "testOrderOfInvocation001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmex"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLOOILeafPrivateEntity");
        properties.put("ListenerMethodProtectionType", "PT_PRIVATE");

        executeDDL("JPA10_CALLBACK_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    // Protected Protection

    @Test
    public void jpa10_Callback_OrderOfInvocation_ProtectedProtection_001_Ano_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_ProtectedProtection_001_Ano_CMEX_EJB_SF";
        final String testMethod = "testOrderOfInvocation001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmex"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoOOILeafProtectedEntity");
        properties.put("ListenerMethodProtectionType", "PT_PROTECTED");

        executeDDL("JPA10_CALLBACK_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_ProtectedProtection_001_XML_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_ProtectedProtection_001_XML_CMEX_EJB_SF";
        final String testMethod = "testOrderOfInvocation001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmex"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLOOILeafProtectedEntity");
        properties.put("ListenerMethodProtectionType", "PT_PROTECTED");

        executeDDL("JPA10_CALLBACK_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    // Public Protection

    @Test
    public void jpa10_Callback_OrderOfInvocation_PublicProtection_001_Ano_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PublicProtection_001_Ano_CMEX_EJB_SF";
        final String testMethod = "testOrderOfInvocation001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmex"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "AnoOOILeafPublicEntity");
        properties.put("ListenerMethodProtectionType", "PT_PUBLIC");

        executeDDL("JPA10_CALLBACK_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

    @Test
    public void jpa10_Callback_OrderOfInvocation_PublicProtection_001_XML_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Callback_OrderOfInvocation_PublicProtection_001_XML_CMEX_EJB_SF";
        final String testMethod = "testOrderOfInvocation001";

        final TestExecutionContext testExecCtx = new TestExecutionContext(testName, testLogicClassName, testMethod);

        final HashMap<String, JPAPersistenceContext> jpaPCInfoMap = testExecCtx.getJpaPCInfoMap();
        jpaPCInfoMap.put("test-jpa-resource", jpaPctxMap.get("test-jpa-resource-cmex"));
        jpaPCInfoMap.put("cleanup", jpaPctxMap.get("cleanup"));

        HashMap<String, java.io.Serializable> properties = testExecCtx.getProperties();
        properties.put("EntityName", "XMLOOILeafPublicEntity");
        properties.put("ListenerMethodProtectionType", "PT_PUBLIC");

        executeDDL("JPA10_CALLBACK_DELETE_${dbvendor}.ddl");
        executeTestVehicle(testExecCtx, ejbJNDIName);
    }

}
