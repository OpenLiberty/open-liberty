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

import com.ibm.ws.jpa.fvt.entity.testlogic.EmbeddableTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBDBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/Embeddable_EJB_SL_TestServlet")
public class Embeddable_EJB_SL_TestServlet extends EJBDBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = EmbeddableTestLogic.class.getName();
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
    public void jpa10_Entity_embeddable_Embeddable_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Embeddable_Ano_AMJTA_EJB_SL";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "EmbeddedObjectEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_embeddable_Embeddable_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Embeddable_XML_AMJTA_EJB_SL";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLEmbeddedObjectEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_embeddable_Embeddable_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Embeddable_Ano_AMRL_EJB_SL";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "EmbeddedObjectEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_embeddable_Embeddable_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Embeddable_XML_AMRL_EJB_SL";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLEmbeddedObjectEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_embeddable_Embeddable_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Embeddable_Ano_CMTS_EJB_SL";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "EmbeddedObjectEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_embeddable_Embeddable_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_Embeddable_XML_CMTS_EJB_SL";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLEmbeddedObjectEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_embeddable_EmbeddableAO_Ano_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_EmbeddableAO_Ano_AMJTA_EJB_SL";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "EmbeddedObjectAOEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_embeddable_EmbeddableAO_XML_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_EmbeddableAO_XML_AMJTA_EJB_SL";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-amjta";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLEmbeddedObjectAOEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_embeddable_EmbeddableAO_Ano_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_EmbeddableAO_Ano_AMRL_EJB_SL";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "EmbeddedObjectAOEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_embeddable_EmbeddableAO_XML_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_EmbeddableAO_XML_AMRL_EJB_SL";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-amrl";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLEmbeddedObjectAOEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_embeddable_EmbeddableAO_Ano_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_EmbeddableAO_Ano_CMTS_EJB_SL";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "EmbeddedObjectAOEntity");

        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Entity_embeddable_EmbeddableAO_XML_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Entity_EmbeddableAO_XML_CMTS_EJB_SL";
        final String testMethod = "testEmbeddable001";
        final String testResource = "test-jpa-resource-cmts";

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityName", "XMLEmbeddedObjectAOEntity");

        executeTest(testName, testMethod, testResource, properties);
    }
}
