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

package com.ibm.ws.jpa.embeddable.relationship.web;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.embeddable.relationship.testlogic.EmbeddableRelationshipLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestEmbeddableRelationshipServlet")
public class TestEmbeddableRelationshipServlet extends JPATestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "EMBEDDABLE_RELATIONSHIP_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "EMBEDDABLE_RELATIONSHIP_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "EMBEDDABLE_RELATIONSHIP_RL")
    private EntityManagerFactory amrlEmf;

    @PostConstruct
    private void initFAT() {
        testClassName = EmbeddableRelationshipLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    // testEmbeddableRelationship01

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship01_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship01_AMJTA_Web";
        final String testMethod = "testEmbeddableRelationship01";
        final String testResource = "test-jpa-resource-amjta";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship01_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship01_AMRL_Web";
        final String testMethod = "testEmbeddableRelationship01";
        final String testResource = "test-jpa-resource-amrl";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship01_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship01_CMTS_Web";
        final String testMethod = "testEmbeddableRelationship01";
        final String testResource = "test-jpa-resource-cmts";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableRelationship02

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship02_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship02_AMJTA_Web";
        final String testMethod = "testEmbeddableRelationship02";
        final String testResource = "test-jpa-resource-amjta";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship02_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship02_AMRL_Web";
        final String testMethod = "testEmbeddableRelationship02";
        final String testResource = "test-jpa-resource-amrl";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship02_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship02_CMTS_Web";
        final String testMethod = "testEmbeddableRelationship02";
        final String testResource = "test-jpa-resource-cmts";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableRelationship03

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship03_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship03_AMJTA_Web";
        final String testMethod = "testEmbeddableRelationship03";
        final String testResource = "test-jpa-resource-amjta";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship03_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship03_AMRL_Web";
        final String testMethod = "testEmbeddableRelationship03";
        final String testResource = "test-jpa-resource-amrl";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship03_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship03_CMTS_Web";
        final String testMethod = "testEmbeddableRelationship03";
        final String testResource = "test-jpa-resource-cmts";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableRelationship04

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship04_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship04_AMJTA_Web";
        final String testMethod = "testEmbeddableRelationship04";
        final String testResource = "test-jpa-resource-amjta";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship04_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship04_AMRL_Web";
        final String testMethod = "testEmbeddableRelationship04";
        final String testResource = "test-jpa-resource-amrl";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship04_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship04_CMTS_Web";
        final String testMethod = "testEmbeddableRelationship04";
        final String testResource = "test-jpa-resource-cmts";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }
}
