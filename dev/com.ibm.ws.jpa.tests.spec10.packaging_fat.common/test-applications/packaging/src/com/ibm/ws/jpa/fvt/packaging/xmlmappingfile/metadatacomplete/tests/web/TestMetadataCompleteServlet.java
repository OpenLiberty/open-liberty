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

package com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.metadatacomplete.tests.web;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.metadatacomplete.testlogic.MetadataCompleteTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPADBTestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestMetadataCompleteServlet")
public class TestMetadataCompleteServlet extends JPADBTestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "MetadataCompleteUnit_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "MetadataCompleteUnit_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "MetadataCompleteUnit_RL")
    private EntityManagerFactory amrlEmf;

    @PostConstruct
    private void initFAT() {
        testClassName = MetadataCompleteTestLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    /*
     * Verify that annotations in the specified entity are ignored.
     */

    @Test
    public void jpa10_Packaging_MetadataComplete_Test001_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Packaging_MetadataComplete_Test001_AMJTA_Web";
        final String testMethod = "executeMetadataCompleteTest001";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", "MDCEntity");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_MetadataComplete_Test001_AMRL_Web() throws Exception {
        final String testName = "jpa10_Packaging_MetadataComplete_Test001_AMRL_Web";
        final String testMethod = "executeMetadataCompleteTest001";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", "MDCEntity");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_MetadataComplete_Test001_CMTS_Web() throws Exception {
        final String testName = "jpa10_Packaging_MetadataComplete_Test001_CMTS_Web";
        final String testMethod = "executeMetadataCompleteTest001";
        final String testResource = "test-jpa-resource-cmts";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", "MDCEntity");
        executeTest(testName, testMethod, testResource, properties);
    }

}
