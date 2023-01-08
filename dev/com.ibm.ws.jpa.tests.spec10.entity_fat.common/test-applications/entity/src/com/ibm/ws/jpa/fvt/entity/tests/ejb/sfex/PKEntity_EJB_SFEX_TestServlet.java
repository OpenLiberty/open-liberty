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

import com.ibm.ws.jpa.fvt.entity.testlogic.PKEntityTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBDBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/PKEntity_EJB_SFEX_TestServlet")
public class PKEntity_EJB_SFEX_TestServlet extends EJBDBTestVehicleServlet {
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

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_Byte_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_Byte_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityByte");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_ByteWrapper_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_ByteWrapper_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityByteWrapper");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_ByteWrapper_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_ByteWrapper_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityByteWrapper");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_Char_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_Char_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityChar");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_Char_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_Char_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityChar");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_CharWrapper_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_CharWrapper_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityCharWrapper");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_CharWrapper_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_CharWrapper_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityCharWrapper");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_Int_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_Int_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityInt");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_Int_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_Int_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityInt");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_IntWrapper_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_IntWrapper_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityIntWrapper");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_IntWrapper_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_IntWrapper_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityIntWrapper");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_Long_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_Long_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityLong");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_Long_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_Long_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityLong");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_LongWrapper_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_LongWrapper_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityLongWrapper");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_LongWrapper_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_LongWrapper_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityLongWrapper");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_Short_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_Short_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityShort");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_Short_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_Short_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityShort");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_ShortWrapper_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_ShortWrapper_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityShortWrapper");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_ShortWrapper_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_ShortWrapper_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityShortWrapper");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_String_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_String_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityString");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_String_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_String_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityString");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_JavaSqlDate_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_JavaSqlDate_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityJavaSqlDate");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_JavaSqlDate_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_JavaSqlDate_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityJavaSqlDate");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_JavaUtilDate_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_JavaUtilDate_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKEntityJavaUtilDate");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKEntity_JavaUtilDate_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKEntity_JavaUtilDate_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKEntity001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKEntityJavaUtilDate");

        executeTest(testName, testMethod, testResource, properties);
    }

}
