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

import com.ibm.ws.jpa.fvt.entity.testlogic.PKGeneratorTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBDBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/PKGenerator_EJB_SFEX_TestServlet")
public class PKGenerator_EJB_SFEX_TestServlet extends EJBDBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = PKGeneratorTestLogic.class.getName();
        ejbJNDIName = "ejb/EntitySFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Entity_CMEX"));
        jpaPctxMap.put("test-jpa-resource-2",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Entity_AMRL"));
    }

    @Test
    public void jpa10_Entity_PKGenerator_Auto_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_Auto_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenAutoEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_Auto_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_Auto_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenAutoEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_Identity_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_Identity_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenIdentityEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_Identity_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_Identity_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenIdentityEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType1_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType1_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenTableType1Entity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType1_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType1_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenTableType1Entity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType2_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType2_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenTableType2Entity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType2_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType2_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenTableType2Entity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType3_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType3_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenTableType3Entity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType3_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType3_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenTableType3Entity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType4_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType4_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenTableType4Entity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_PKGenerator_TableType4_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_TableType4_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmex";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenTableType4Entity");

        executeTest(testName, testMethod, testResource, properties);
    }

//    @Test
    public void jpa10_Entity_PKGenerator_SequenceType1_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_SequenceType1_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmex";

        if (getDbProductName().toLowerCase().contains("derby")) {
            // Derby doesn't support sequences, so do not run.
            return;
        }

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenSequenceType1Entity");

        executeTest(testName, testMethod, testResource, properties);
    }

//    @Test
    public void jpa10_Entity_PKGenerator_SequenceType1_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_SequenceType1_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmex";

        if (getDbProductName().toLowerCase().contains("derby")) {
            // Derby doesn't support sequences, so do not run.
            return;
        }

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenSequenceType1Entity");

        executeTest(testName, testMethod, testResource, properties);
    }

//    @Test
    public void jpa10_Entity_PKGenerator_SequenceType2_Ano_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_SequenceType2_Ano_CMEX_EJB_SFEX";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmex";

        if (getDbProductName().toLowerCase().contains("derby")) {
            // Derby doesn't support sequences, so do not run.
            return;
        }

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "PKGenSequenceType2Entity");

        executeTest(testName, testMethod, testResource, properties);
    }

//    @Test
    public void jpa10_Entity_PKGenerator_SequenceType2_XML_CMEX_EJB_SFEX() throws Exception {
        final String testName = "jpa10_Entity_PKGenerator_SequenceType2_XML_CMEX_EJB_SFEX";
        final String testMethod = "testPKGenerator001";
        final String testResource = "test-jpa-resource-cmex";

        if (getDbProductName().toLowerCase().contains("derby")) {
            // Derby doesn't support sequences, so do not run.
            return;
        }

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLPKGenSequenceType2Entity");

        executeTest(testName, testMethod, testResource, properties);
    }

}
