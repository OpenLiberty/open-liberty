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

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.entity.testlogic.MultiTableTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/MultiTable_EJB_SL_TestServlet")
public class MultiTable_EJB_SL_TestServlet extends EJBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = MultiTableTestLogic.class.getName();
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
    public void jpa10_Entity_MultiTable_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_MultiTable_Ano_AMJTA_EJB_SL";
        final String testMethod = "testMultiTable001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AnnMultiTableEnt");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_MultiTable_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_MultiTable_XML_AMJTA_EJB_SL";
        final String testMethod = "testMultiTable001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLMultiTableEnt");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_MultiTable_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_MultiTable_Ano_AMRL_EJB_SL";
        final String testMethod = "testMultiTable001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AnnMultiTableEnt");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_MultiTable_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_MultiTable_XML_AMRL_EJB_SL";
        final String testMethod = "testMultiTable001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLMultiTableEnt");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_MultiTable_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_MultiTable_Ano_CMTS_EJB_SL";
        final String testMethod = "testMultiTable001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AnnMultiTableEnt");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_MultiTable_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_MultiTable_XML_CMTS_EJB_SL";
        final String testMethod = "testMultiTable001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLMultiTableEnt");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_MultiTable_Embed_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_MultiTable_Embed_Ano_AMJTA_EJB_SL";
        final String testMethod = "testMultiTable001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AnnEmbedMultiTableEnt");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_MultiTable_Embed_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_MultiTable_Embed_XML_AMJTA_EJB_SL";
        final String testMethod = "testMultiTable001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLEmbedMultiTableEnt");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_MultiTable_Embed_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_MultiTable_Embed_Ano_AMRL_EJB_SL";
        final String testMethod = "testMultiTable001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AnnEmbedMultiTableEnt");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_MultiTable_Embed_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_MultiTable_Embed_XML_AMRL_EJB_SL";
        final String testMethod = "testMultiTable001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLEmbedMultiTableEnt");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_MultiTable_Embed_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_MultiTable_Embed_Ano_CMTS_EJB_SL";
        final String testMethod = "testMultiTable001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AnnEmbedMultiTableEnt");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_MultiTable_Embed_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_MultiTable_Embed_XML_CMTS_EJB_SL";
        final String testMethod = "testMultiTable001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLEmbedMultiTableEnt");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_MultiTable_MSC_Embed_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_MultiTable_MSC_Embed_Ano_AMJTA_EJB_SL";
        final String testMethod = "testMultiTable001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AnnMSCMultiTableEnt");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_MultiTable_MSC_Embed_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_MultiTable_MSC_Embed_XML_AMJTA_EJB_SL";
        final String testMethod = "testMultiTable001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLMSCMultiTableEnt");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_MultiTable_MSC_Embed_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_MultiTable_MSC_Embed_Ano_AMRL_EJB_SL";
        final String testMethod = "testMultiTable001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AnnMSCMultiTableEnt");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_MultiTable_MSC_Embed_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_MultiTable_MSC_Embed_XML_AMRL_EJB_SL";
        final String testMethod = "testMultiTable001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLMSCMultiTableEnt");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_MultiTable_MSC_Embed_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_MultiTable_MSC_Embed_Ano_CMTS_EJB_SL";
        final String testMethod = "testMultiTable001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "AnnMSCMultiTableEnt");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_MultiTable_MSC_Embed_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_MultiTable_MSC_Embed_XML_CMTS_EJB_SL";
        final String testMethod = "testMultiTable001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLMSCMultiTableEnt");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

}
