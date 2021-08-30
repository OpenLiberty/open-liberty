/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.entity.tests.ejb.sl;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.entity.testlogic.VersioningTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/Versioning_EJB_SL_TestServlet")
public class Versioning_EJB_SL_TestServlet extends EJBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = VersioningTestLogic.class.getName();
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

    @Test

    public void jpa10_Entity_Versioning_Int_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Int_Ano_AMJTA_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedIntEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_Int_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Int_XML_AMJTA_EJB_SL";
        final String testMethod = "testVersioning001";
        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedIntEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Int_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Int_Ano_AMRL_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedIntEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Int_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Int_XML_AMRL_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedIntEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_Int_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Int_Ano_CMTS_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedIntEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_Int_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Int_XML_CMTS_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedIntEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_IntWrapper_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_IntWrapper_Ano_AMJTA_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedIntWrapperEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_IntWrapper_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_IntWrapper_XML_AMJTA_EJB_SL";
        final String testMethod = "testVersioning001";
        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedIntWrapperEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_IntWrapper_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_IntWrapper_Ano_AMRL_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedIntWrapperEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_IntWrapper_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_IntWrapper_XML_AMRL_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedIntWrapperEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_IntWrapper_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_IntWrapper_Ano_CMTS_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedIntWrapperEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_IntWrapper_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_IntWrapper_XML_CMTS_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedIntWrapperEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_Long_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Long_Ano_AMJTA_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedLongEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_Long_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Long_XML_AMJTA_EJB_SL";
        final String testMethod = "testVersioning001";
        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedLongEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Long_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Long_Ano_AMRL_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedLongEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Long_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Long_XML_AMRL_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedLongEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_Long_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Long_Ano_CMTS_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedLongEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_Long_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Long_XML_CMTS_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedLongEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_LongWrapper_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_LongWrapper_Ano_AMJTA_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedLongWrapperEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_LongWrapper_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_LongWrapper_XML_AMJTA_EJB_SL";
        final String testMethod = "testVersioning001";
        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedLongWrapperEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_LongWrapper_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_LongWrapper_Ano_AMRL_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedLongWrapperEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_LongWrapper_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_LongWrapper_XML_AMRL_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedLongWrapperEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_LongWrapper_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_LongWrapper_Ano_CMTS_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedLongWrapperEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_LongWrapper_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_LongWrapper_XML_CMTS_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedLongWrapperEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_Short_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Short_Ano_AMJTA_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedShortEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_Short_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Short_XML_AMJTA_EJB_SL";
        final String testMethod = "testVersioning001";
        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedShortEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Short_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Short_Ano_AMRL_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedShortEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Short_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Short_XML_AMRL_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedShortEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_Short_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Short_Ano_CMTS_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedShortEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_Short_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Short_XML_CMTS_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedShortEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_ShortWrapper_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_ShortWrapper_Ano_AMJTA_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedShortWrapperEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_ShortWrapper_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_ShortWrapper_XML_AMJTA_EJB_SL";
        final String testMethod = "testVersioning001";
        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedShortWrapperEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_ShortWrapper_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_ShortWrapper_Ano_AMRL_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedShortWrapperEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_ShortWrapper_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_ShortWrapper_XML_AMRL_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedShortWrapperEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_ShortWrapper_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_ShortWrapper_Ano_CMTS_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedShortWrapperEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_ShortWrapper_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_ShortWrapper_XML_CMTS_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedShortWrapperEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_SqlTimestamp_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_SqlTimestamp_Ano_AMJTA_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedSqlTimestampEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_SqlTimestamp_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_SqlTimestamp_XML_AMJTA_EJB_SL";
        final String testMethod = "testVersioning001";
        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amjta");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedSqlTimestampEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_SqlTimestamp_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_SqlTimestamp_Ano_AMRL_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedSqlTimestampEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_SqlTimestamp_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_SqlTimestamp_XML_AMRL_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-amrl");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedSqlTimestampEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_SqlTimestamp_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_SqlTimestamp_Ano_CMTS_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedSqlTimestampEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test

    public void jpa10_Entity_Versioning_SqlTimestamp_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Versioning_SqlTimestamp_XML_CMTS_EJB_SL";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmts");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedSqlTimestampEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, properties);
    }

}
