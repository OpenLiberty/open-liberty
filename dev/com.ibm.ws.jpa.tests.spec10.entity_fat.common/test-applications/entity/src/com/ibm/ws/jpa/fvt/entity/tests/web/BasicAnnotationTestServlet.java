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

package com.ibm.ws.jpa.fvt.entity.tests.web;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.entity.testlogic.BasicAnnotationTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPADBTestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/BasicAnnotationTestServlet")
public class BasicAnnotationTestServlet extends JPADBTestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "ENTITY_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "ENTITY_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "ENTITY_RL")
    private EntityManagerFactory amrlEmf;

    @PostConstruct
    private void initFAT() {
        testClassName = BasicAnnotationTestLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    @Test
    public void jpa10_Entity_EagerFetch_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_EagerFetch_Ano_AMJTA_Web";
        final String testMethod = "testEagerFetchFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_EagerFetch_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_EagerFetch_XML_AMJTA_Web";
        final String testMethod = "testEagerFetchFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_EagerFetch_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_EagerFetch_Ano_AMRL_Web";
        final String testMethod = "testEagerFetchFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_EagerFetch_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_EagerFetch_XML_AMRL_Web";
        final String testMethod = "testEagerFetchFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_EagerFetch_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_EagerFetch_Ano_CMTS_Web";
        final String testMethod = "testEagerFetchFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_EagerFetch_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_EagerFetch_XML_CMTS_Web";
        final String testMethod = "testEagerFetchFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_LazyFetch_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_LazyFetch_Ano_AMJTA_Web";
        final String testMethod = "testLazyFetchFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_LazyFetch_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_LazyFetch_XML_AMJTA_Web";
        final String testMethod = "testLazyFetchFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_LazyFetch_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_LazyFetch_Ano_AMRL_Web";
        final String testMethod = "testLazyFetchFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_LazyFetch_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_LazyFetch_XML_AMRL_Web";
        final String testMethod = "testLazyFetchFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_LazyFetch_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_LazyFetch_Ano_CMTS_Web";
        final String testMethod = "testLazyFetchFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_LazyFetch_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_LazyFetch_XML_CMTS_Web";
        final String testMethod = "testLazyFetchFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_NonOptional_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_NonOptional_Ano_AMJTA_Web";
        final String testMethod = "testNonOptionalFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_NonOptional_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_NonOptional_XML_AMJTA_Web";
        final String testMethod = "testNonOptionalFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_NonOptional_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_NonOptional_Ano_AMRL_Web";
        final String testMethod = "testNonOptionalFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_NonOptional_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_NonOptional_XML_AMRL_Web";
        final String testMethod = "testNonOptionalFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_NonOptional_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_NonOptional_Ano_CMTS_Web";
        final String testMethod = "testNonOptionalFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_NonOptional_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_NonOptional_XML_CMTS_Web";
        final String testMethod = "testNonOptionalFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ColumnNameOverride_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_ColumnNameOverride_Ano_AMJTA_Web";
        final String testMethod = "testColumnNameOverrideFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ColumnNameOverride_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_ColumnNameOverride_XML_AMJTA_Web";
        final String testMethod = "testColumnNameOverrideFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ColumnNameOverride_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_ColumnNameOverride_Ano_AMRL_Web";
        final String testMethod = "testColumnNameOverrideFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ColumnNameOverride_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_ColumnNameOverride_XML_AMRL_Web";
        final String testMethod = "testColumnNameOverrideFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ColumnNameOverride_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_ColumnNameOverride_Ano_CMTS_Web";
        final String testMethod = "testColumnNameOverrideFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ColumnNameOverride_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_ColumnNameOverride_XML_CMTS_Web";
        final String testMethod = "testColumnNameOverrideFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Nullable_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Nullable_Ano_AMJTA_Web";
        final String testMethod = "testNullableFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Nullable_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Nullable_XML_AMJTA_Web";
        final String testMethod = "testNullableFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Nullable_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Nullable_Ano_AMRL_Web";
        final String testMethod = "testNullableFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Nullable_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Nullable_XML_AMRL_Web";
        final String testMethod = "testNullableFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Nullable_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Nullable_Ano_CMTS_Web";
        final String testMethod = "testNullableFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Nullable_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Nullable_XML_CMTS_Web";
        final String testMethod = "testNullableFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Unique_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Unique_Ano_AMJTA_Web";
        final String testMethod = "testUniqueFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Unique_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_Unique_XML_AMJTA_Web";
        final String testMethod = "testUniqueFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Unique_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Unique_Ano_AMRL_Web";
        final String testMethod = "testUniqueFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Unique_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_Unique_XML_AMRL_Web";
        final String testMethod = "testUniqueFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Unique_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Unique_Ano_CMTS_Web";
        final String testMethod = "testUniqueFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_Unique_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_Unique_XML_CMTS_Web";
        final String testMethod = "testUniqueFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_AttributeTable_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_AttributeTable_Ano_AMJTA_Web";
        final String testMethod = "testAttributeTableFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_AttributeTable_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_AttributeTable_XML_AMJTA_Web";
        final String testMethod = "testAttributeTableFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_AttributeTable_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_AttributeTable_Ano_AMRL_Web";
        final String testMethod = "testAttributeTableFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_AttributeTable_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_AttributeTable_XML_AMRL_Web";
        final String testMethod = "testAttributeTableFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_AttributeTable_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_AttributeTable_Ano_CMTS_Web";
        final String testMethod = "testAttributeTableFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_AttributeTable_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_AttributeTable_XML_CMTS_Web";
        final String testMethod = "testAttributeTableFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ColumnLength_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_ColumnLength_Ano_AMJTA_Web";
        final String testMethod = "testColumnLengthFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ColumnLength_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_ColumnLength_XML_AMJTA_Web";
        final String testMethod = "testColumnLengthFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ColumnLength_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_ColumnLength_Ano_AMRL_Web";
        final String testMethod = "testColumnLengthFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ColumnLength_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_ColumnLength_XML_AMRL_Web";
        final String testMethod = "testColumnLengthFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ColumnLength_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_ColumnLength_Ano_CMTS_Web";
        final String testMethod = "testColumnLengthFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_ColumnLength_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_ColumnLength_XML_CMTS_Web";
        final String testMethod = "testColumnLengthFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_UniqueConstraint_Ano_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_UniqueConstraint_Ano_AMJTA_Web";
        final String testMethod = "testUniqueConstraintsFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_UniqueConstraint_XML_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Entity_UniqueConstraint_XML_AMJTA_Web";
        final String testMethod = "testUniqueConstraintsFunction";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_UniqueConstraint_Ano_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_UniqueConstraint_Ano_AMRL_Web";
        final String testMethod = "testUniqueConstraintsFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_UniqueConstraint_XML_AMRL_Web() throws Exception {
        final String testName = "jpa10_Entity_UniqueConstraint_XML_AMRL_Web";
        final String testMethod = "testUniqueConstraintsFunction";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_UniqueConstraint_Ano_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_UniqueConstraint_Ano_CMTS_Web";
        final String testMethod = "testUniqueConstraintsFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_UniqueConstraint_XML_CMTS_Web() throws Exception {
        final String testName = "jpa10_Entity_UniqueConstraint_XML_CMTS_Web";
        final String testMethod = "testUniqueConstraintsFunction";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLAttrConfigFieldEntity");

        executeTest(testName, testMethod, testResource, properties);
    }
}
