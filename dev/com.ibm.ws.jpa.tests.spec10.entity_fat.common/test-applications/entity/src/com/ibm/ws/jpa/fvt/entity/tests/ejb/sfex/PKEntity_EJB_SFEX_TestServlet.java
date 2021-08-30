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

package com.ibm.ws.jpa.fvt.entity.tests.ejb.sfex;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.entity.testlogic.PKEntityTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/PKEntity_EJB_SFEX_TestServlet")
public class PKEntity_EJB_SFEX_TestServlet extends EJBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = PKEntityTestLogic.class.getName();
        ejbJNDIName = "ejb/EntitySFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Entity_CMEX"));
        jpaPctxMap.put("test-jpa-resource-2",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Entity_AMRL"));
    }

    @Test
    public void jpa10_Entity_PKEntity_Byte_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_Byte_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityByte");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_Byte_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_Byte_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityByte");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_ByteWrapper_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_ByteWrapper_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityByteWrapper");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_ByteWrapper_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_ByteWrapper_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityByteWrapper");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_Char_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_Char_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityChar");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_Char_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_Char_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityChar");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_CharWrapper_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_CharWrapper_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityCharWrapper");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_CharWrapper_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_CharWrapper_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityCharWrapper");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_Int_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_Int_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityInt");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_Int_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_Int_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityInt");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_IntWrapper_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_IntWrapper_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityIntWrapper");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_IntWrapper_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_IntWrapper_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityIntWrapper");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_Long_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_Long_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityLong");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_Long_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_Long_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityLong");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_LongWrapper_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_LongWrapper_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityLongWrapper");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_LongWrapper_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_LongWrapper_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityLongWrapper");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_Short_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_Short_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityShort");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_Short_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_Short_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityShort");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_ShortWrapper_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_ShortWrapper_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityShortWrapper");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_ShortWrapper_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_ShortWrapper_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityShortWrapper");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_String_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_String_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityString");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_String_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_String_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityString");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_JavaSqlDate_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_JavaSqlDate_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityJavaSqlDate");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_JavaSqlDate_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_JavaSqlDate_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityJavaSqlDate");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_JavaUtilDate_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_JavaUtilDate_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityJavaUtilDate");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_JavaUtilDate_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_JavaUtilDate_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityJavaUtilDate");

        executeDDL("JPA10_ENTITY_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource, properties);
    }

}
