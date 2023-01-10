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

package com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.annotationoverride.tests.ejb;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.annotationoverride.testlogic.AnnotationOverrideTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBDBTestVehicleServlet;

import componenttest.annotation.ExpectedFFDC;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestAnnotationOverrideEJBSLServlet")
public class TestAnnotationOverrideEJBSLServlet extends EJBDBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = AnnotationOverrideTestLogic.class.getName();
        ejbJNDIName = "ejb/AnoOverrideSLEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/AnoOverride_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/AnoOverride_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/AnoOverride_CMTS"));
    }

    /*
     * Verify that table schema annotations can be overriden by their XML counterparts.
     */

    @Test
    public void jpa10_Packaging_AnnotationOverride_Test001_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test001_AMJTA_EJB_SL";
        final String testMethod = "testAnnotationOverride001";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_AnnotationOverride_Test001_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test001_AMRL_EJB_SL";
        final String testMethod = "testAnnotationOverride001";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_AnnotationOverride_Test001_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test001_CMTS_EJB_SL";
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
    public void jpa10_Packaging_AnnotationOverride_Test002_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test002_AMJTA_EJB_SL";
        final String testMethod = "testAnnotationOverride002";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_AnnotationOverride_Test002_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test002_AMRL_EJB_SL";
        final String testMethod = "testAnnotationOverride002";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_AnnotationOverride_Test002_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test002_CMTS_EJB_SL";
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
    public void jpa10_Packaging_AnnotationOverride_Test003_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test003_AMJTA_EJB_SL";
        final String testMethod = "testAnnotationOverride003";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_AnnotationOverride_Test003_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test003_AMRL_EJB_SL";
        final String testMethod = "testAnnotationOverride003";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_AnnotationOverride_Test003_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test003_CMTS_EJB_SL";
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
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Packaging_AnnotationOverride_Test004_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test004_AMJTA_EJB_SL";
        final String testMethod = "testAnnotationOverride004";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_AnnotationOverride_Test004_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test004_AMRL_EJB_SL";
        final String testMethod = "testAnnotationOverride004";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Packaging_AnnotationOverride_Test004_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test004_CMTS_EJB_SL";
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
    public void jpa10_Packaging_AnnotationOverride_Test005_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test005_AMJTA_EJB_SL";
        final String testMethod = "testAnnotationOverride005";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

//    @Test
    public void jpa10_Packaging_AnnotationOverride_Test005_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test005_AMRL_EJB_SL";
        final String testMethod = "testAnnotationOverride005";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

//    @Test
    public void jpa10_Packaging_AnnotationOverride_Test005_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test005_CMTS_EJB_SL";
        final String testMethod = "testAnnotationOverride005";
        final String testResource = "test-jpa-resource-cmts";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

}
