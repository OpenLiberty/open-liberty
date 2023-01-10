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

package com.ibm.ws.jpa.fvt.jarfile.tests.jarfilesupport.ejb;

import java.util.HashMap;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.jarfile.testlogic.JarFileSupportTestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.EJBDBTestVehicleServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JarFileTestServletEJBSL")
public class JarFileTestServletEJBSL extends EJBDBTestVehicleServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = JarFileSupportTestLogic.class.getName();
        ejbJNDIName = "ejb/JarFileSupportSLEJB";

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/JPAJarFileSupport_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/JPAJarFileSupport_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/JPAJarFileSupport_CMTS"));
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
    public void jpa10_injection_jarfilesupport_ejb_EntitiesInComponentPURoot_Test001_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_injection_jarfilesupport_ejb_EntitiesInComponentPURoot_Test001_AMJTA_EJB_SL";
        final String testMethod = "testEntitiesInComponentArchive001";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "SimpleEntity10");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_injection_jarfilesupport_ejb_EntitiesInComponentPURoot_Test001_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_injection_jarfilesupport_ejb_EntitiesInComponentPURoot_Test001_AMRL_EJB_SL";
        final String testMethod = "testEntitiesInComponentArchive001";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "SimpleEntity10");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_injection_jarfilesupport_ejb_EntitiesInComponentPURoot_Test001_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_injection_jarfilesupport_ejb_EntitiesInComponentPURoot_Test001_CMTS_EJB_SL";
        final String testMethod = "testEntitiesInComponentArchive001";
        final String testResource = "test-jpa-resource-cmts";

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "SimpleEntity10");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_injection_jarfilesupport_ejb_EntitiesInComponentPURoot_XML_Test001_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_injection_jarfilesupport_ejb_EntitiesInComponentPURoot_XML_Test001_AMJTA_EJB_SL";
        final String testMethod = "testEntitiesInComponentArchive001";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XMLSimpleEntity10");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_injection_jarfilesupport_ejb_EntitiesInComponentPURoot_XML_Test001_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_injection_jarfilesupport_ejb_EntitiesInComponentPURoot_XML_Test001_AMRL_EJB_SL";
        final String testMethod = "testEntitiesInComponentArchive001";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        properties.put("EntityType", "XMLSimpleEntity10");
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_injection_jarfilesupport_ejb_EntitiesInComponentPURoot_XML_Test001_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_injection_jarfilesupport_ejb_EntitiesInComponentPURoot_XML_Test001_CMTS_EJB_SL";
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
    public void jpa10_injection_jarfilesupport_ejb_EntitiesInJarArchiveComponentPURoot_Test001_AMJTA_EJB_SL() throws Exception {
        final String testName = "jpa10_injection_jarfilesupport_ejb_EntitiesInJarArchiveComponentPURoot_Test001_AMJTA_EJB_SL";
        final String testMethod = "testEntitiesInJarFile001";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_injection_jarfilesupport_ejb_EntitiesInJarArchiveComponentPURoot_Test001_AMRL_EJB_SL() throws Exception {
        final String testName = "jpa10_injection_jarfilesupport_ejb_EntitiesInJarArchiveComponentPURoot_Test001_AMRL_EJB_SL";
        final String testMethod = "testEntitiesInJarFile001";
        final String testResource = "test-jpa-resource-amrl";

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

    @Test
    public void jpa10_injection_jarfilesupport_ejb_EntitiesInJarArchiveComponentPURoot_Test001_CMTS_EJB_SL() throws Exception {
        final String testName = "jpa10_injection_jarfilesupport_ejb_EntitiesInJarArchiveComponentPURoot_Test001_CMTS_EJB_SL";
        final String testMethod = "testEntitiesInJarFile001";
        final String testResource = "test-jpa-resource-cmts";

        executeDDL("JPA10_INJECTION_DELETE_${dbvendor}.ddl");

        HashMap<String, java.io.Serializable> properties = new HashMap<String, java.io.Serializable>();
        executeTest(testName, testMethod, testResource, properties);
    }

}
