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

import com.ibm.ws.jpa.fvt.callback.entities.defaultlistener.EntitySupportingDefaultCallbacks;
import com.ibm.ws.jpa.fvt.callback.entities.defaultlistener.XMLEntitySupportingDefaultCallbacks;
import com.ibm.ws.jpa.fvt.callback.testlogic.CallbackRuntimeExceptionTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

import componenttest.annotation.ExpectedFFDC;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestCallbackDefaultListenerException_EJB_SFEX_Servlet")
public class TestCallbackDefaultListenerException_EJB_SFEX_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = CallbackRuntimeExceptionTestLogic.class.getName();
        ejbJNDIName = "ejb/CallbackSFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Callback-DefaultListener_CMEX"));
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
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_CallbackDefaultListener_RuntimeException_PackageProtection_001_Ano_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_CallbackDefaultListener_RuntimeException_PackageProtection_001_Ano_CMEX_EJB_SF";
        final String testMethod = "testCallbackRuntimeException002";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", EntitySupportingDefaultCallbacks.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PACKAGE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_CallbackDefaultListener_RuntimeException_PackageProtection_001_XML_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_CallbackDefaultListener_RuntimeException_PackageProtection_001_XML_CMEX_EJB_SF";
        final String testMethod = "testCallbackRuntimeException002";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLEntitySupportingDefaultCallbacks.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PACKAGE");

        executeTest(testName, testMethod, testResource, properties);
    }

    // Private Protection

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_CallbackDefaultListener_RuntimeException_PrivateProtection_001_Ano_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_CallbackDefaultListener_RuntimeException_PrivateProtection_001_Ano_CMEX_EJB_SF";
        final String testMethod = "testCallbackRuntimeException002";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", EntitySupportingDefaultCallbacks.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PRIVATE");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_CallbackDefaultListener_RuntimeException_PrivateProtection_001_XML_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_CallbackDefaultListener_RuntimeException_PrivateProtection_001_XML_CMEX_EJB_SF";
        final String testMethod = "testCallbackRuntimeException002";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLEntitySupportingDefaultCallbacks.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PRIVATE");

        executeTest(testName, testMethod, testResource, properties);
    }

    // Protected Protection

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_CallbackDefaultListener_RuntimeException_ProtectedProtection_001_Ano_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_CallbackDefaultListener_RuntimeException_ProtectedProtection_001_Ano_CMEX_EJB_SF";
        final String testMethod = "testCallbackRuntimeException002";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", EntitySupportingDefaultCallbacks.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PROTECTED");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_CallbackDefaultListener_RuntimeException_ProtectedProtection_001_XML_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_CallbackDefaultListener_RuntimeException_ProtectedProtection_001_XML_CMEX_EJB_SF";
        final String testMethod = "testCallbackRuntimeException002";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLEntitySupportingDefaultCallbacks.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PROTECTED");

        executeTest(testName, testMethod, testResource, properties);
    }

    // Public Protection

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_CallbackDefaultListener_RuntimeException_PublicProtection_001_Ano_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_CallbackDefaultListener_RuntimeException_PublicProtection_001_Ano_CMEX_EJB_SF";
        final String testMethod = "testCallbackRuntimeException002";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", EntitySupportingDefaultCallbacks.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PUBLIC");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_CallbackDefaultListener_RuntimeException_PublicProtection_001_XML_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_CallbackDefaultListener_RuntimeException_PublicProtection_001_XML_CMEX_EJB_SF";
        final String testMethod = "testCallbackRuntimeException002";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", XMLEntitySupportingDefaultCallbacks.class.getSimpleName());
        properties.put("ListenerMethodProtectionType", "PT_PUBLIC");

        executeTest(testName, testMethod, testResource, properties);
    }
}
