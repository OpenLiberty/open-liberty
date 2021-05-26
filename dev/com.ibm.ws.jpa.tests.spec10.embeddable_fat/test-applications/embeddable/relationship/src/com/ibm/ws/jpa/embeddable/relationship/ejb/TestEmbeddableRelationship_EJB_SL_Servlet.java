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

package com.ibm.ws.jpa.embeddable.relationship.ejb;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.embeddable.relationship.testlogic.EmbeddableRelationshipLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestEmbeddableRelationship_EJB_SL_Servlet")
public class TestEmbeddableRelationship_EJB_SL_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = EmbeddableRelationshipLogic.class.getName();
        ejbJNDIName = "ejb/EmbeddableRelationshipSLEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Embeddable_Relationship_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Embeddable_Relationship_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Embeddable_Relationship_CMTS"));
    }

    // testEmbeddableRelationship01

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship01_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship01_EJB_SL_AMJTA_Web";
        final String testMethod = "testEmbeddableRelationship01";
        final String testResource = "test-jpa-resource-amjta";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship01_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship01_EJB_SL_AMRL_Web";
        final String testMethod = "testEmbeddableRelationship01";
        final String testResource = "test-jpa-resource-amrl";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship01_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship01_EJB_SL_CMTS_Web";
        final String testMethod = "testEmbeddableRelationship01";
        final String testResource = "test-jpa-resource-cmts";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableRelationship02

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship02_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship02_EJB_SL_AMJTA_Web";
        final String testMethod = "testEmbeddableRelationship02";
        final String testResource = "test-jpa-resource-amjta";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship02_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship02_EJB_SL_AMRL_Web";
        final String testMethod = "testEmbeddableRelationship02";
        final String testResource = "test-jpa-resource-amrl";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship02_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship02_EJB_SL_CMTS_Web";
        final String testMethod = "testEmbeddableRelationship02";
        final String testResource = "test-jpa-resource-cmts";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableRelationship03

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship03_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship03_EJB_SL_AMJTA_Web";
        final String testMethod = "testEmbeddableRelationship03";
        final String testResource = "test-jpa-resource-amjta";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship03_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship03_EJB_SL_AMRL_Web";
        final String testMethod = "testEmbeddableRelationship03";
        final String testResource = "test-jpa-resource-amrl";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship03_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship03_EJB_SL_CMTS_Web";
        final String testMethod = "testEmbeddableRelationship03";
        final String testResource = "test-jpa-resource-cmts";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableRelationship04

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship04_EJB_SL_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship04_EJB_SL_AMJTA_Web";
        final String testMethod = "testEmbeddableRelationship04";
        final String testResource = "test-jpa-resource-amjta";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship04_EJB_SL_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship04_EJB_SL_AMRL_Web";
        final String testMethod = "testEmbeddableRelationship04";
        final String testResource = "test-jpa-resource-amrl";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship04_EJB_SL_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship04_EJB_SL_CMTS_Web";
        final String testMethod = "testEmbeddableRelationship04";
        final String testResource = "test-jpa-resource-cmts";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }
}
