/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.entity.tests.ejb.sfex;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.entity.testlogic.BasicAnnotationTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBDBTestVehicleServlet;

import componenttest.annotation.AllowedFFDC;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/BasicAnnotation_EJB_SFEX_TestServlet")
public class BasicAnnotation_EJB_SFEX_TestServlet extends EJBDBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = BasicAnnotationTestLogic.class.getName();
        ejbJNDIName = "ejb/EntitySFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Entity_CMEX"));
        jpaPctxMap.put("test-jpa-resource-2",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Entity_AMRL"));
    }

    // testEagerFetchFunction

    @Test
    public void jpa10_Entity_EagerFetch_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_EagerFetch_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testEagerFetchFunction";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_EagerFetch_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_EagerFetch_XML_CMEX_EJB_SFEX";
        final String testMethod = "testEagerFetchFunction";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    // testLazyFetchFunction

    @Test
    public void jpa10_Entity_LazyFetch_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_LazyFetch_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testLazyFetchFunction";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_LazyFetch_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_LazyFetch_XML_CMEX_EJB_SFEX";
        final String testMethod = "testLazyFetchFunction";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    // testNonOptionalFunction

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_NonOptional_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_NonOptional_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testNonOptionalFunction";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_NonOptional_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_NonOptional_XML_CMEX_EJB_SFEX";
        final String testMethod = "testNonOptionalFunction";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    // testColumnNameOverrideFunction

    @Test
    public void jpa10_Entity_ColumnNameOverride_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_ColumnNameOverride_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testColumnNameOverrideFunction";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ColumnNameOverride_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_ColumnNameOverride_XML_CMEX_EJB_SFEX";
        final String testMethod = "testColumnNameOverrideFunction";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    // testNullableFunction

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_Nullable_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Nullable_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testNullableFunction";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_Nullable_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Nullable_XML_CMEX_EJB_SFEX";
        final String testMethod = "testNullableFunction";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    // testUniqueFunction

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_Unique_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Unique_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testUniqueFunction";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_Unique_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Unique_XML_CMEX_EJB_SFEX";
        final String testMethod = "testUniqueFunction";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    // testAttributeTableFunction

    @Test
    public void jpa10_Entity_AttributeTable_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_AttributeTable_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testAttributeTableFunction";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_AttributeTable_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_AttributeTable_XML_CMEX_EJB_SFEX";
        final String testMethod = "testAttributeTableFunction";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    // testColumnLengthFunction

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_ColumnLength_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_ColumnLength_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testColumnLengthFunction";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_ColumnLength_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_ColumnLength_XML_CMEX_EJB_SFEX";
        final String testMethod = "testColumnLengthFunction";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    // testUniqueConstraintsFunction

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_UniqueConstraint_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_UniqueConstraint_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testUniqueConstraintsFunction";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_UniqueConstraint_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_UniqueConstraint_XML_CMEX_EJB_SFEX";
        final String testMethod = "testUniqueConstraintsFunction";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }
}
