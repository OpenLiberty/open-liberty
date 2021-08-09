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
@WebServlet(urlPatterns = "/TestEmbeddableRelationship_EJB_SFEx_Servlet")
public class TestEmbeddableRelationship_EJB_SFEx_Servlet extends EJBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = EmbeddableRelationshipLogic.class.getName();
        ejbJNDIName = "ejb/EmbeddableRelationshipSFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/Embeddable_Relationship_CMEX"));
    }

    // testEmbeddableRelationship01

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship01_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship01_EJB_SFEx_CMEX_Web";
        final String testMethod = "testEmbeddableRelationship01";
        final String testResource = "test-jpa-resource-cmex";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableRelationship02

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship02_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship02_EJB_SFEx_CMEX_Web";
        final String testMethod = "testEmbeddableRelationship02";
        final String testResource = "test-jpa-resource-cmex";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableRelationship03

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship03_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec10_embeddable_relationship_testEmbeddableRelationship03_EJB_SFEx_CMEX_Web";
        final String testMethod = "testEmbeddableRelationship03";
        final String testResource = "test-jpa-resource-cmex";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    // testEmbeddableRelationship04

    @Test
    public void jpa_spec10_embeddable_relationship_testEmbeddableRelationship04_EJB_SFEx_CMEX_Web() throws Exception {
        final String testName = "jpa_spec10_relationship_testEmbeddableRelationship04_EJB_SFEx_CMEX_Web";
        final String testMethod = "testEmbeddableRelationship04";
        final String testResource = "test-jpa-resource-cmex";
        //TODO: OPENJPA-2874: orphaned references are not removed from the database
        executeDDL("JPA10_EMBEDDABLE_RELATIONSHIP_DELETE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }
}
