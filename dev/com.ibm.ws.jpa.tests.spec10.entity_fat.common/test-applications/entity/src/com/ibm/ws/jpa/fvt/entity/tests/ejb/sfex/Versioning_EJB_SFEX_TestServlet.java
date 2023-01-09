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
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.entity.testlogic.VersioningTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBDBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/Versioning_EJB_SFEX_TestServlet")
public class Versioning_EJB_SFEX_TestServlet extends EJBDBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = VersioningTestLogic.class.getName();
        ejbJNDIName = "ejb/EntitySFExEJB";
        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Entity_CMEX"));
        jpaPctxMap.put("test-jpa-resource-2",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/EntityJPAResource2"));
    }

    // testVersioning001

    @Test
    public void jpa10_Entity_Versioning_Int_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Int_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedIntEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Int_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Int_XML_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedIntEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_IntWrapper_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning_IntWrapper_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedIntWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_IntWrapper_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning_IntWrapper_XML_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedIntWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Long_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Long_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedLongEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Long_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Long_XML_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedLongEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_LongWrapper_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning_LongWrapper_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedLongWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_LongWrapper_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning_LongWrapper_XML_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedLongWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Short_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Short_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedShortEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_Short_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning_Short_XML_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedShortEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_ShortWrapper_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning_ShortWrapper_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedShortWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_ShortWrapper_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning_ShortWrapper_XML_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedShortWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_SqlTimestamp_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning_SqlTimestamp_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning_SqlTimestamp_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning_SqlTimestamp_XML_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning001";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");
        testResourcesList.put("test-jpa-resource2", "test-jpa-resource-2");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    // testVersioning002 - Only Timestamp testing is necessary

    @Test
    public void jpa10_Entity_Versioning2_SqlTimestamp_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning2_SqlTimestamp_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning002";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning2_SqlTimestamp_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning2_SqlTimestamp_XML_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning002";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    // testVersioning003

    @Test
    public void jpa10_Entity_Versioning3_Int_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Int_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedIntEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_Int_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Int_XML_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedIntEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_IntWrapper_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_IntWrapper_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedIntWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_IntWrapper_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_IntWrapper_XML_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedIntWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_Long_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Long_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedLongEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_Long_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Long_XML_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedLongEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_LongWrapper_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_LongWrapper_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedLongWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_LongWrapper_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_LongWrapper_XML_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedLongWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_Short_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Short_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedShortEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_Short_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_Short_XML_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedShortEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_ShortWrapper_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_ShortWrapper_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedShortWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_ShortWrapper_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_ShortWrapper_XML_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedShortWrapperEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_SqlTimestamp_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_SqlTimestamp_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "VersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

    @Test
    public void jpa10_Entity_Versioning3_SqlTimestamp_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_Versioning3_SqlTimestamp_XML_CMEX_EJB_SFEX";
        final String testMethod = "testVersioning003";

        final Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", "test-jpa-resource-cmex");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLVersionedSqlTimestampEntity");

        executeTest(testName, testMethod, testResourcesList, properties);
    }

}
