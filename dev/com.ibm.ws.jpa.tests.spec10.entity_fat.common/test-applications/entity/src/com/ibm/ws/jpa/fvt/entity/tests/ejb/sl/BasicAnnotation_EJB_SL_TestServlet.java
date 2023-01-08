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

package com.ibm.ws.jpa.fvt.entity.tests.ejb.sl;

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
@WebServlet(urlPatterns = "/BasicAnnotation_EJB_SL_TestServlet")
public class BasicAnnotation_EJB_SL_TestServlet extends EJBDBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = BasicAnnotationTestLogic.class.getName();
        ejbJNDIName = "ejb/EntitySLEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Entity_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Entity_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Entity_CMTS"));
        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Entity_CMEX"));
        jpaPctxMap.put("test-jpa-resource-2",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Entity_AMRL"));
    }

    // testEagerFetchFunction

    @Test
    public void jpa10_Entity_EagerFetch_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_EagerFetch_Ano_AMJTA_EJB_SL";
        final String testMethod = "testEagerFetchFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_EagerFetch_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_EagerFetch_XML_AMJTA_EJB_SL";
        final String testMethod = "testEagerFetchFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_EagerFetch_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_EagerFetch_Ano_AMRL_EJB_SL";
        final String testMethod = "testEagerFetchFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_EagerFetch_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_EagerFetch_XML_AMRL_EJB_SL";
        final String testMethod = "testEagerFetchFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_EagerFetch_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_EagerFetch_Ano_CMTS_EJB_SL";
        final String testMethod = "testEagerFetchFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_EagerFetch_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_EagerFetch_XML_CMTS_EJB_SL";
        final String testMethod = "testEagerFetchFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    // testLazyFetchFunction

    @Test
    public void jpa10_Entity_LazyFetch_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_LazyFetch_Ano_AMJTA_EJB_SL";
        final String testMethod = "testLazyFetchFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_LazyFetch_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_LazyFetch_XML_AMJTA_EJB_SL";
        final String testMethod = "testLazyFetchFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_LazyFetch_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_LazyFetch_Ano_AMRL_EJB_SL";
        final String testMethod = "testLazyFetchFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_LazyFetch_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_LazyFetch_XML_AMRL_EJB_SL";
        final String testMethod = "testLazyFetchFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_LazyFetch_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_LazyFetch_Ano_CMTS_EJB_SL";
        final String testMethod = "testLazyFetchFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_LazyFetch_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_LazyFetch_XML_CMTS_EJB_SL";
        final String testMethod = "testLazyFetchFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    // testNonOptionalFunction

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_NonOptional_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_NonOptional_Ano_AMJTA_EJB_SL";
        final String testMethod = "testNonOptionalFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_NonOptional_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_NonOptional_XML_AMJTA_EJB_SL";
        final String testMethod = "testNonOptionalFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_NonOptional_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_NonOptional_Ano_AMRL_EJB_SL";
        final String testMethod = "testNonOptionalFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_NonOptional_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_NonOptional_XML_AMRL_EJB_SL";
        final String testMethod = "testNonOptionalFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_NonOptional_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_NonOptional_Ano_CMTS_EJB_SL";
        final String testMethod = "testNonOptionalFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_NonOptional_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_NonOptional_XML_CMTS_EJB_SL";
        final String testMethod = "testNonOptionalFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    // testColumnNameOverrideFunction

    @Test
    public void jpa10_Entity_ColumnNameOverride_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_ColumnNameOverride_Ano_AMJTA_EJB_SL";
        final String testMethod = "testColumnNameOverrideFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ColumnNameOverride_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_ColumnNameOverride_XML_AMJTA_EJB_SL";
        final String testMethod = "testColumnNameOverrideFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ColumnNameOverride_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_ColumnNameOverride_Ano_AMRL_EJB_SL";
        final String testMethod = "testColumnNameOverrideFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ColumnNameOverride_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_ColumnNameOverride_XML_AMRL_EJB_SL";
        final String testMethod = "testColumnNameOverrideFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ColumnNameOverride_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_ColumnNameOverride_Ano_CMTS_EJB_SL";
        final String testMethod = "testColumnNameOverrideFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ColumnNameOverride_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_ColumnNameOverride_XML_CMTS_EJB_SL";
        final String testMethod = "testColumnNameOverrideFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    // testNullableFunction

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_Nullable_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Nullable_Ano_AMJTA_EJB_SL";
        final String testMethod = "testNullableFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_Nullable_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Nullable_XML_AMJTA_EJB_SL";
        final String testMethod = "testNullableFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
//    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_Nullable_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Nullable_Ano_AMRL_EJB_SL";
        final String testMethod = "testNullableFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
//    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_Nullable_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Nullable_XML_AMRL_EJB_SL";
        final String testMethod = "testNullableFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_Nullable_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Nullable_Ano_CMTS_EJB_SL";
        final String testMethod = "testNullableFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_Nullable_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Nullable_XML_CMTS_EJB_SL";
        final String testMethod = "testNullableFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    //testUniqueFunction

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_Unique_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Unique_Ano_AMJTA_EJB_SL";
        final String testMethod = "testUniqueFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_Unique_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Unique_XML_AMJTA_EJB_SL";
        final String testMethod = "testUniqueFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
//    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_Unique_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Unique_Ano_AMRL_EJB_SL";
        final String testMethod = "testUniqueFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
//    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_Unique_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Unique_XML_AMRL_EJB_SL";
        final String testMethod = "testUniqueFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_Unique_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Unique_Ano_CMTS_EJB_SL";
        final String testMethod = "testUniqueFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_Unique_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Unique_XML_CMTS_EJB_SL";
        final String testMethod = "testUniqueFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    //testAttributeTableFunction

    @Test
    public void jpa10_Entity_AttributeTable_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_AttributeTable_Ano_AMJTA_EJB_SL";
        final String testMethod = "testAttributeTableFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_AttributeTable_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_AttributeTable_XML_AMJTA_EJB_SL";
        final String testMethod = "testAttributeTableFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_AttributeTable_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_AttributeTable_Ano_AMRL_EJB_SL";
        final String testMethod = "testAttributeTableFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_AttributeTable_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_AttributeTable_XML_AMRL_EJB_SL";
        final String testMethod = "testAttributeTableFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_AttributeTable_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_AttributeTable_Ano_CMTS_EJB_SL";
        final String testMethod = "testAttributeTableFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_AttributeTable_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_AttributeTable_XML_CMTS_EJB_SL";
        final String testMethod = "testAttributeTableFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    // testColumnLengthFunction

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_ColumnLength_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_ColumnLength_Ano_AMJTA_EJB_SL";
        final String testMethod = "testColumnLengthFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_ColumnLength_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_ColumnLength_XML_AMJTA_EJB_SL";
        final String testMethod = "testColumnLengthFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ColumnLength_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_ColumnLength_Ano_AMRL_EJB_SL";
        final String testMethod = "testColumnLengthFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ColumnLength_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_ColumnLength_XML_AMRL_EJB_SL";
        final String testMethod = "testColumnLengthFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_ColumnLength_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_ColumnLength_Ano_CMTS_EJB_SL";
        final String testMethod = "testColumnLengthFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_ColumnLength_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_ColumnLength_XML_CMTS_EJB_SL";
        final String testMethod = "testColumnLengthFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    //testUniqueConstraintsFunction

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_UniqueConstraint_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_UniqueConstraint_Ano_AMJTA_EJB_SL";
        final String testMethod = "testUniqueConstraintsFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_UniqueConstraint_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_UniqueConstraint_XML_AMJTA_EJB_SL";
        final String testMethod = "testUniqueConstraintsFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_UniqueConstraint_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_UniqueConstraint_Ano_AMRL_EJB_SL";
        final String testMethod = "testUniqueConstraintsFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_UniqueConstraint_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_UniqueConstraint_XML_AMRL_EJB_SL";
        final String testMethod = "testUniqueConstraintsFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_UniqueConstraint_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_UniqueConstraint_Ano_CMTS_EJB_SL";
        final String testMethod = "testUniqueConstraintsFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @AllowedFFDC("javax.transaction.RollbackException")
    public void jpa10_Entity_UniqueConstraint_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_UniqueConstraint_XML_CMTS_EJB_SL";
        final String testMethod = "testUniqueConstraintsFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }
}
