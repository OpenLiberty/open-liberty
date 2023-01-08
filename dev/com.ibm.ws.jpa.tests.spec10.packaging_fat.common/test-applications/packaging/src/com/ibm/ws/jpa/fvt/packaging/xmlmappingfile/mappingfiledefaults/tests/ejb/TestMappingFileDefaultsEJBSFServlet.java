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
@WebServlet(urlPatterns = "/TestMappingFileDefaultsEJBSFServlet")
public class TestMappingFileDefaultsEJBSFServlet extends EJBDBTestVehicleServlet {

    @PostConstruct
    private void initFAT() {
        testClassName = MappingFileDefaultsTestLogic.class.getName();
        ejbJNDIName = "ejb/MappingFileDefaultsSFEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/MappingFileDefaults_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/MappingFileDefaults_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/MappingFileDefaults_CMTS"));
    }

    /*
     * Verify that mapping file defaults for schema and catalog are observed by all entities in the persistence unit.
     */

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test001_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test001_AMJTA_EJB_SF";
        final String testMethod = "executeBasicMappingFileDefaultTest";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", "MFDEntity1");
        properties.put("EntityBName", "MFDEntity2");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test001_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test001_AMRL_EJB_SF";
        final String testMethod = "executeBasicMappingFileDefaultTest";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", "MFDEntity1");
        properties.put("EntityBName", "MFDEntity2");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test001_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test001_CMTS_EJB_SF";
        final String testMethod = "executeBasicMappingFileDefaultTest";
        final String testResource = "test-jpa-resource-cmts";

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
    public void jpa10_Packaging_MappingFileDefaults_Test002_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test002_AMJTA_EJB_SF";
        final String testMethod = "executeBasicMappingFileDefaultTest";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", "MFDMSC1Ent");
        properties.put("EntityBName", "MFDMSC2Ent");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test002_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test002_AMRL_EJB_SF";
        final String testMethod = "executeBasicMappingFileDefaultTest";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", "MFDMSC1Ent");
        properties.put("EntityBName", "MFDMSC2Ent");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test002_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test002_CMTS_EJB_SF";
        final String testMethod = "executeBasicMappingFileDefaultTest";
        final String testResource = "test-jpa-resource-cmts";

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
    public void jpa10_Packaging_MappingFileDefaults_Test003_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test003_AMJTA_EJB_SF";
        final String testMethod = "executeBasicMappingFileDefaultTest";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", "MFDFQEmbedEnt");
        properties.put("EntityBName", "MFDNFQEmbedEnt");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test003_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test003_AMRL_EJB_SF";
        final String testMethod = "executeBasicMappingFileDefaultTest";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityAName", "MFDFQEmbedEnt");
        properties.put("EntityBName", "MFDNFQEmbedEnt");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test003_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test003_CMTS_EJB_SF";
        final String testMethod = "executeBasicMappingFileDefaultTest";
        final String testResource = "test-jpa-resource-cmts";

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
    public void jpa10_Packaging_MappingFileDefaults_Test004_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test004_AMJTA_EJB_SF";
        final String testMethod = "executeListenerTest";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test004_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test004_AMRL_EJB_SF";
        final String testMethod = "executeListenerTest";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test004_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test004_CMTS_EJB_SF";
        final String testMethod = "executeListenerTest";
        final String testResource = "test-jpa-resource-cmts";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Verify that defining a package mapping default entry applies to entity-listener class names.
     */

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test005_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test005_AMJTA_EJB_SF";
        final String testMethod = "testManyToOneTargetEntityClass";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test005_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test005_AMRL_EJB_SF";
        final String testMethod = "testManyToOneTargetEntityClass";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test005_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test005_CMTS_EJB_SF";
        final String testMethod = "testManyToOneTargetEntityClass";
        final String testResource = "test-jpa-resource-cmts";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Verify that defining a package mapping default entry applies to target-entity class names for
     * one-to-one elements.
     */

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test006_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test006_AMJTA_EJB_SF";
        final String testMethod = "testOneToOneTargetEntityClass";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test006_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test006_AMRL_EJB_SF";
        final String testMethod = "testOneToOneTargetEntityClass";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test006_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test006_CMTS_EJB_SF";
        final String testMethod = "testOneToOneTargetEntityClass";
        final String testResource = "test-jpa-resource-cmts";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Verify that defining a package mapping default entry applies to target-entity class names for
     * one-to-many elements.
     */

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test007_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test007_AMJTA_EJB_SF";
        final String testMethod = "testOneToManyTargetEntityClass";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test007_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test007_AMRL_EJB_SF";
        final String testMethod = "testOneToManyTargetEntityClass";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test007_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test007_CMTS_EJB_SF";
        final String testMethod = "testOneToManyTargetEntityClass";
        final String testResource = "test-jpa-resource-cmts";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Verify that defining a package mapping default entry applies to target-entity class names for
     * many-to-many elements.
     */

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test008_AMJTA_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test008_AMJTA_EJB_SF";
        final String testMethod = "testManyToManyTargetEntityClass";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test008_AMRL_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test008_AMRL_EJB_SF";
        final String testMethod = "testManyToManyTargetEntityClass";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_Packaging_MappingFileDefaults_Test008_CMTS_EJB_SF() throws Exception {
        final String testName = "jpa10_Packaging_MappingFileDefaults_Test008_CMTS_EJB_SF";
        final String testMethod = "testManyToManyTargetEntityClass";
        final String testResource = "test-jpa-resource-cmts";

        executeDDL("JPA10_PACKAGING_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

}
