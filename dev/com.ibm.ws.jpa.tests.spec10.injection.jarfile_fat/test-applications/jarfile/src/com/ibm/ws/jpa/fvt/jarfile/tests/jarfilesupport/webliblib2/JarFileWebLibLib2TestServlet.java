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

package com.ibm.ws.jpa.fvt.jarfile.tests.jarfilesupport.webliblib2;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.jarfile.testlogic.JarFileSupportTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPADBTestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JarFileWebLibLib2TestServlet")
public class JarFileWebLibLib2TestServlet extends JPADBTestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "JarFileSupport_JEE")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "JarFileSupport_JEE")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "JarFileSupport_RL")
    private EntityManagerFactory amrlEmf;

    @PostConstruct
    private void initFAT() {
        testClassName = JarFileSupportTestLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    /*
     *
     * Performs basic CRUD operations with an entity within the managed component archive:
     * 1) Create a new instance of the entity class
     * 2) Persist the new entity to the database
     * 3) Verify the entity was saved to the database
     * 4) Update the entity
     * 5) Verify the entity update was saved to the database
     * 6) Delete the entity from the database
     * 7) Verify the entity remove was successful
     */

    @Test
    public void jpa10_injection_jarfilesupport_web_lib_lib_EntitiesInComponentPURoot_Test001_AMJTA_Web() throws Exception {
        final String testName = "jpa10_injection_jarfilesupport_web_lib_lib_EntitiesInComponentPURoot_Test001_AMJTA_Web";
        final String testMethod = "testEntitiesInComponentArchive001";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "SimpleEntity10");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_injection_jarfilesupport_web_lib_lib_EntitiesInComponentPURoot_Test001_AMRL_Web() throws Exception {
        final String testName = "jpa10_injection_jarfilesupport_web_lib_lib_EntitiesInComponentPURoot_Test001_AMRL_Web";
        final String testMethod = "testEntitiesInComponentArchive001";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "SimpleEntity10");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_injection_jarfilesupport_web_lib_lib_EntitiesInComponentPURoot_Test001_CMTS_Web() throws Exception {
        final String testName = "jpa10_injection_jarfilesupport_web_lib_lib_EntitiesInComponentPURoot_Test001_CMTS_Web";
        final String testMethod = "testEntitiesInComponentArchive001";
        final String testResource = "test-jpa-resource-cmts";

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "SimpleEntity10");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_injection_jarfilesupport_web_lib_lib_EntitiesInComponentPURoot_XML_Test001_AMJTA_Web() throws Exception {
        final String testName = "jpa10_injection_jarfilesupport_web_lib_lib_EntitiesInComponentPURoot_XML_Test001_AMJTA_Web";
        final String testMethod = "testEntitiesInComponentArchive001";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XMLSimpleEntity10");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_injection_jarfilesupport_web_lib_lib_EntitiesInComponentPURoot_XML_Test001_AMRL_Web() throws Exception {
        final String testName = "jpa10_injection_jarfilesupport_web_lib_lib_EntitiesInComponentPURoot_XML_Test001_AMRL_Web";
        final String testMethod = "testEntitiesInComponentArchive001";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XMLSimpleEntity10");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_injection_jarfilesupport_web_lib_lib_EntitiesInComponentPURoot_XML_Test001_CMTS_Web() throws Exception {
        final String testName = "jpa10_injection_jarfilesupport_web_lib_lib_EntitiesInComponentPURoot_XML_Test001_CMTS_Web";
        final String testMethod = "testEntitiesInComponentArchive001";
        final String testResource = "test-jpa-resource-cmts";

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XMLSimpleEntity10");
        executeTest(testName, testMethod, testResource, properties);
    }

    /*
     * Performs basic CRUD operations with an entity within the jar archive.
     */

    @Test
    public void jpa10_injection_jarfilesupport_web_lib_lib_EntitiesInJarArchiveComponentPURoot_Test001_AMJTA_Web() throws Exception {
        final String testName = "jpa10_injection_jarfilesupport_web_lib_lib_EntitiesInJarArchiveComponentPURoot_Test001_AMJTA_Web";
        final String testMethod = "testEntitiesInJarFile001";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_injection_jarfilesupport_web_lib_lib_EntitiesInJarArchiveComponentPURoot_Test001_AMRL_Web() throws Exception {
        final String testName = "jpa10_injection_jarfilesupport_web_lib_lib_EntitiesInJarArchiveComponentPURoot_Test001_AMRL_Web";
        final String testMethod = "testEntitiesInJarFile001";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_injection_jarfilesupport_web_lib_lib_EntitiesInJarArchiveComponentPURoot_Test001_CMTS_Web() throws Exception {
        final String testName = "jpa10_injection_jarfilesupport_web_lib_lib_EntitiesInJarArchiveComponentPURoot_Test001_CMTS_Web";
        final String testMethod = "testEntitiesInJarFile001";
        final String testResource = "test-jpa-resource-cmts";

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

}
