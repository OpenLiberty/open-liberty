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

package com.ibm.ws.jpa.fvt.entity.tests.ejb.sf;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.entity.testlogic.PKGeneratorTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/PKGenerator_EJB_SF_TestServlet")
public class PKGenerator_EJB_SF_TestServlet extends EJBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = PKGeneratorTestLogic.class.getName();
        ejbJNDIName = "ejb/EntitySFEJB";

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
    public void jpa10_Entity_PKGenerator_Auto_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_Auto_Ano_AMJTA_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenAutoEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_Auto_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_Auto_XML_AMJTA_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenAutoEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_Auto_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_Auto_Ano_AMRL_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenAutoEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_Auto_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_Auto_XML_AMRL_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenAutoEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_Auto_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_Auto_Ano_CMTS_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenAutoEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_Auto_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_Auto_XML_CMTS_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenAutoEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_Identity_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_Identity_Ano_AMJTA_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenIdentityEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_Identity_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_Identity_XML_AMJTA_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenIdentityEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_Identity_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_Identity_Ano_AMRL_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenIdentityEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_Identity_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_Identity_XML_AMRL_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenIdentityEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_Identity_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_Identity_Ano_CMTS_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenIdentityEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_Identity_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_Identity_XML_CMTS_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenIdentityEntity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType1_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType1_Ano_AMJTA_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenTableType1Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType1_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType1_XML_AMJTA_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenTableType1Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType1_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType1_Ano_AMRL_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenTableType1Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType1_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType1_XML_AMRL_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenTableType1Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType1_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType1_Ano_CMTS_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenTableType1Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType1_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType1_XML_CMTS_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenTableType1Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType2_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType2_Ano_AMJTA_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenTableType2Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType2_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType2_XML_AMJTA_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenTableType2Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType2_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType2_Ano_AMRL_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenTableType2Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType2_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType2_XML_AMRL_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenTableType2Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType2_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType2_Ano_CMTS_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenTableType2Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType2_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType2_XML_CMTS_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenTableType2Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType3_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType3_Ano_AMJTA_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenTableType3Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType3_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType3_XML_AMJTA_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenTableType3Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType3_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType3_Ano_AMRL_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenTableType3Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType3_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType3_XML_AMRL_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenTableType3Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType3_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType3_Ano_CMTS_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenTableType3Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType3_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType3_XML_CMTS_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenTableType3Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType4_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType4_Ano_AMJTA_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenTableType4Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType4_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType4_XML_AMJTA_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenTableType4Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType4_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType4_Ano_AMRL_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenTableType4Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType4_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType4_XML_AMRL_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenTableType4Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType4_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType4_Ano_CMTS_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenTableType4Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType4_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType4_XML_CMTS_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenTableType4Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

//    @Test
    public void jpa10_Entity_PKGenerator_SequenceType1_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_SequenceType1_Ano_AMJTA_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amjta";

        if (getDbProductName().toLowerCase().contains("derby")) {
            // Derby doesn't support sequences, so do not run.
            return;
        }

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenSequenceType1Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

//    @Test
    public void jpa10_Entity_PKGenerator_SequenceType1_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_SequenceType1_XML_AMJTA_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amjta";

        if (getDbProductName().toLowerCase().contains("derby")) {
            // Derby doesn't support sequences, so do not run.
            return;
        }

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenSequenceType1Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

//    @Test
    public void jpa10_Entity_PKGenerator_SequenceType1_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_SequenceType1_Ano_AMRL_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amrl";

        if (getDbProductName().toLowerCase().contains("derby")) {
            // Derby doesn't support sequences, so do not run.
            return;
        }

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenSequenceType1Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

//    @Test
    public void jpa10_Entity_PKGenerator_SequenceType1_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_SequenceType1_XML_AMRL_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amrl";

        if (getDbProductName().toLowerCase().contains("derby")) {
            // Derby doesn't support sequences, so do not run.
            return;
        }

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenSequenceType1Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

//    @Test
    public void jpa10_Entity_PKGenerator_SequenceType1_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_SequenceType1_Ano_CMTS_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmts";

        if (getDbProductName().toLowerCase().contains("derby")) {
            // Derby doesn't support sequences, so do not run.
            return;
        }

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenSequenceType1Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

//    @Test
    public void jpa10_Entity_PKGenerator_SequenceType1_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_SequenceType1_XML_CMTS_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmts";

        if (getDbProductName().toLowerCase().contains("derby")) {
            // Derby doesn't support sequences, so do not run.
            return;
        }

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenSequenceType1Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

//    @Test
    public void jpa10_Entity_PKGenerator_SequenceType2_Ano_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_SequenceType2_Ano_AMJTA_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amjta";

        if (getDbProductName().toLowerCase().contains("derby")) {
            // Derby doesn't support sequences, so do not run.
            return;
        }

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenSequenceType2Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

//    @Test
    public void jpa10_Entity_PKGenerator_SequenceType2_XML_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_SequenceType2_XML_AMJTA_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amjta";

        if (getDbProductName().toLowerCase().contains("derby")) {
            // Derby doesn't support sequences, so do not run.
            return;
        }

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenSequenceType2Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

//    @Test
    public void jpa10_Entity_PKGenerator_SequenceType2_Ano_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_SequenceType2_Ano_AMRL_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amrl";

        if (getDbProductName().toLowerCase().contains("derby")) {
            // Derby doesn't support sequences, so do not run.
            return;
        }

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenSequenceType2Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

//    @Test
    public void jpa10_Entity_PKGenerator_SequenceType2_XML_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_SequenceType2_XML_AMRL_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-amrl";

        if (getDbProductName().toLowerCase().contains("derby")) {
            // Derby doesn't support sequences, so do not run.
            return;
        }

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenSequenceType2Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

//    @Test
    public void jpa10_Entity_PKGenerator_SequenceType2_Ano_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_SequenceType2_Ano_CMTS_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmts";

        if (getDbProductName().toLowerCase().contains("derby")) {
            // Derby doesn't support sequences, so do not run.
            return;
        }

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenSequenceType2Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

//    @Test
    public void jpa10_Entity_PKGenerator_SequenceType2_XML_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_SequenceType2_XML_CMTS_EJB_SF";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmts";

        if (getDbProductName().toLowerCase().contains("derby")) {
            // Derby doesn't support sequences, so do not run.
            return;
        }

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenSequenceType2Entity");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

}
