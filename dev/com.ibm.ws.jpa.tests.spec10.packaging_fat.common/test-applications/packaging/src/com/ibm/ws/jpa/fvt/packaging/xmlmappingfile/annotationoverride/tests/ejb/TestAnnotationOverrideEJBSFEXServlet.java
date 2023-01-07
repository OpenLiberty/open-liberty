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
@WebServlet(urlPatterns = "/TestAnnotationOverrideEJBSFEXServlet")
public class TestAnnotationOverrideEJBSFEXServlet extends EJBDBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = AnnotationOverrideTestLogic.class.getName();
        ejbJNDIName = "ejb/AnoOverrideSFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/AnoOverride_CMEX"));
    }

    /*
     * Verify that table schema annotations can be overriden by their XML counterparts.
     */

    @Test
    public void jpa10_Packaging_AnnotationOverride_Test001_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test001_CMEX_EJB_SF";
        final String testMethod = "testAnnotationOverride001";
        final String testResource = "test-jpa-resource-cmex";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Verify that eager/lazy loading annotations can be overridden by their XML counterparts.
     */

    @Test
    public void jpa10_Packaging_AnnotationOverride_Test002_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test002_CMEX_EJB_SF";
        final String testMethod = "testAnnotationOverride002";
        final String testResource = "test-jpa-resource-cmex";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Verify that optionality annotations can be overridden by their XML counterparts.
     */

    @Test
    public void jpa10_Packaging_AnnotationOverride_Test003_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test003_CMEX_EJB_SF";
        final String testMethod = "testAnnotationOverride003";
        final String testResource = "test-jpa-resource-cmex";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Verify that unique-attribute annotations can be overriden by their XML counterparts.
     */

    @Test
    @ExpectedFFDC("javax.transaction.RollbackException")
    public void jpa10_Packaging_AnnotationOverride_Test004_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test004_CMEX_EJB_SF";
        final String testMethod = "testAnnotationOverride004";
        final String testResource = "test-jpa-resource-cmex";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Verify that column (String) length annotations can be overridden by their XML counterparts.
     */

//    @Test
    public void jpa10_Packaging_AnnotationOverride_Test005_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_AnnotationOverride_Test005_CMEX_EJB_SF";
        final String testMethod = "testAnnotationOverride005";
        final String testResource = "test-jpa-resource-cmex";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

}
