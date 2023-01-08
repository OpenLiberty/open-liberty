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

package com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.tests.ejb;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.mappingfiledefaults.testlogic.MappingFileDefaultsTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBDBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestMappingFileDefaultsEJBSFEXServlet")
public class TestMappingFileDefaultsEJBSFEXServlet extends EJBDBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = MappingFileDefaultsTestLogic.class.getName();
        ejbJNDIName = "ejb/MappingFileDefaultsSFExEJB";

        jpaPctxMap.put("test-jpa-resource-cmex",
                       new JPAPersistenceContext("test-jpa-resource-cmex", PersistenceContextType.CONTAINER_MANAGED_ES, PersistenceInjectionType.JNDI, "java:comp/env/jpa/MappingFileDefaults_CMEX"));
    }

    /*
     * Verify that mapping file defaults for schema and catalog are observed by all entities in the persistence unit.
     */

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test001_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test001_CMEX_EJB_SF";
        final String testMethod = "executeBasicMappingFileDefaultTest";
        final String testResource = "test-jpa-resource-cmex";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", "MFDEntity1");
        properties.put("EntityBName", "MFDEntity2");
        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Verify that defining a package mapping default entry applies to mapped superclass class names.
     */

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test002_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test002_CMEX_EJB_SF";
        final String testMethod = "executeBasicMappingFileDefaultTest";
        final String testResource = "test-jpa-resource-cmex";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", "MFDMSC1Ent");
        properties.put("EntityBName", "MFDMSC2Ent");
        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Verify that defining a package mapping default entry applies to embeddable class names.
     */

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test003_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test003_CMEX_EJB_SF";
        final String testMethod = "executeBasicMappingFileDefaultTest";
        final String testResource = "test-jpa-resource-cmex";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", "MFDFQEmbedEnt");
        properties.put("EntityBName", "MFDNFQEmbedEnt");
        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Verify that defining a package mapping default entry applies to entity-listener class names.
     */

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test004_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test004_CMEX_EJB_SF";
        final String testMethod = "executeListenerTest";
        final String testResource = "test-jpa-resource-cmex";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Verify that defining a package mapping default entry applies to entity-listener class names.
     */

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test005_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test005_CMEX_EJB_SF";
        final String testMethod = "testManyToOneTargetEntityClass";
        final String testResource = "test-jpa-resource-cmex";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Verify that defining a package mapping default entry applies to target-entity class names for
     * one-to-one elements.
     */

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test006_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test006_CMEX_EJB_SF";
        final String testMethod = "testOneToOneTargetEntityClass";
        final String testResource = "test-jpa-resource-cmex";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Verify that defining a package mapping default entry applies to target-entity class names for
     * one-to-many elements.
     */

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test007_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test007_CMEX_EJB_SF";
        final String testMethod = "testOneToManyTargetEntityClass";
        final String testResource = "test-jpa-resource-cmex";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Verify that defining a package mapping default entry applies to target-entity class names for
     * many-to-many elements.
     */

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test008_CMEX_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test008_CMEX_EJB_SF";
        final String testMethod = "testManyToManyTargetEntityClass";
        final String testResource = "test-jpa-resource-cmex";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

}
