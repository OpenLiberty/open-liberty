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

package com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.annotationoverride.tests.web;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.annotationoverride.testlogic.AnnotationOverrideTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPADBTestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestAnnotationOverrideServlet")
public class TestAnnotationOverrideServlet extends JPADBTestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "AnnotationOverrideUnit_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "AnnotationOverrideUnit_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "AnnotationOverrideUnit_RL")
    private EntityManagerFactory amrlEmf;

    @PostConstruct
    private void initFAT() {
        testClassName = AnnotationOverrideTestLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    /*
     * Verify that table schema annotations can be overriden by their XML counterparts.
     */

    @Test
    public void jpa10_Packaging_AnnotationOverride_Test001_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test001_AMJTA_Web";
        final String testMethod = "testAnnotationOverride001";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_AnnotationOverride_Test001_AMRL_Web() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test001_AMRL_Web";
        final String testMethod = "testAnnotationOverride001";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_AnnotationOverride_Test001_CMTS_Web() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test001_CMTS_Web";
        final String testMethod = "testAnnotationOverride001";
        final String testResource = "test-jpa-resource-cmts";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Verify that eager/lazy loading annotations can be overridden by their XML counterparts.
     */

    @Test
    public void jpa10_Packaging_AnnotationOverride_Test002_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test002_AMJTA_Web";
        final String testMethod = "testAnnotationOverride002";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_AnnotationOverride_Test002_AMRL_Web() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test002_AMRL_Web";
        final String testMethod = "testAnnotationOverride002";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_AnnotationOverride_Test002_CMTS_Web() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test002_CMTS_Web";
        final String testMethod = "testAnnotationOverride002";
        final String testResource = "test-jpa-resource-cmts";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Verify that optionality annotations can be overridden by their XML counterparts.
     */

    @Test
    public void jpa10_Packaging_AnnotationOverride_Test003_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test003_AMJTA_Web";
        final String testMethod = "testAnnotationOverride003";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_AnnotationOverride_Test003_AMRL_Web() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test003_AMRL_Web";
        final String testMethod = "testAnnotationOverride003";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_AnnotationOverride_Test003_CMTS_Web() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test003_CMTS_Web";
        final String testMethod = "testAnnotationOverride003";
        final String testResource = "test-jpa-resource-cmts";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Verify that unique-attribute annotations can be overriden by their XML counterparts.
     */

    @Test
    public void jpa10_Packaging_AnnotationOverride_Test004_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test004_AMJTA_Web";
        final String testMethod = "testAnnotationOverride004";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_AnnotationOverride_Test004_AMRL_Web() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test004_AMRL_Web";
        final String testMethod = "testAnnotationOverride004";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_AnnotationOverride_Test004_CMTS_Web() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test004_CMTS_Web";
        final String testMethod = "testAnnotationOverride004";
        final String testResource = "test-jpa-resource-cmts";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Verify that column (String) length annotations can be overridden by their XML counterparts.
     */

//    @Test
    public void jpa10_Packaging_AnnotationOverride_Test005_AMJTA_Web() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test005_AMJTA_Web";
        final String testMethod = "testAnnotationOverride005";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

//    @Test
    public void jpa10_Packaging_AnnotationOverride_Test005_AMRL_Web() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test005_AMRL_Web";
        final String testMethod = "testAnnotationOverride005";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

//    @Test
    public void jpa10_Packaging_AnnotationOverride_Test005_CMTS_Web() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test005_CMTS_Web";
        final String testMethod = "testAnnotationOverride005";
        final String testResource = "test-jpa-resource-cmts";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

}
