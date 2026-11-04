/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

package com.ibm.ws.jpa.fvt.entitylocking.tests.web;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.fvt.entitylocking.testlogic.EntityLocking20TestLogic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPADBTestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/EntityLocking20WebTestServlet")
public class EntityLocking20WebTestServlet extends JPADBTestServlet {
    @PostConstruct
    private void initFAT() {
        testClassName = EntityLocking20TestLogic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.JNDI, "java:comp/env/jpa/EntityLock_AMJTA"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/EntityLock_AMRL"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.JNDI, "java:comp/env/jpa/EntityLock_CMTS"));
        jpaPctxMap.put("test-client-b-resource",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.JNDI, "java:comp/env/jpa/EntityLock_AMRL"));

    }

    @Test
    public void jpa_entitylocking_web_testReadLock001_AMJTA() throws Exception {
        final String testName = "jpa_entitylocking_web_testReadLock001_AMJTA";
        final String testMethod = "testReadLock001";
        final String testResource = "test-jpa-resource-amjta";

        Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", testResource);
        testResourcesList.put("test-jpa-2-resource", "test-client-b-resource");

        executeDDL("JPA20_ENTITYLOCKING_DEFAULT_DELETE_${dbvendor}.ddl");
        executeDDL("JPA20_ENTITYLOCKING_DEFAULT_POPULATE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, null);
    }

    @Test
    public void jpa_entitylocking_web_testReadLock001A_AMJTA() throws Exception {
        final String testName = "jpa_entitylocking_web_testReadLock001A_AMJTA";
        final String testMethod = "testReadLock001A";
        final String testResource = "test-jpa-resource-amjta";

        Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", testResource);
        testResourcesList.put("test-jpa-2-resource", "test-client-b-resource");

        executeDDL("JPA20_ENTITYLOCKING_DEFAULT_DELETE_${dbvendor}.ddl");
        executeDDL("JPA20_ENTITYLOCKING_DEFAULT_POPULATE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, null);
    }

    @Test
    public void jpa_entitylocking_web_testReadLock002_AMJTA() throws Exception {
        final String testName = "jpa_entitylocking_web_testReadLock002_AMJTA";
        final String testMethod = "testReadLock002";
        final String testResource = "test-jpa-resource-amjta";

        Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", testResource);
        testResourcesList.put("test-jpa-2-resource", "test-client-b-resource");

        executeDDL("JPA20_ENTITYLOCKING_DEFAULT_DELETE_${dbvendor}.ddl");
        executeDDL("JPA20_ENTITYLOCKING_DEFAULT_POPULATE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, null);
    }

    @Test
    public void jpa_entitylocking_web_testReadLock002A_AMJTA() throws Exception {
        final String testName = "jpa_entitylocking_web_testReadLock002A_AMJTA";
        final String testMethod = "testReadLock002A";
        final String testResource = "test-jpa-resource-amjta";

        Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", testResource);
        testResourcesList.put("test-jpa-2-resource", "test-client-b-resource");

        executeDDL("JPA20_ENTITYLOCKING_DEFAULT_DELETE_${dbvendor}.ddl");
        executeDDL("JPA20_ENTITYLOCKING_DEFAULT_POPULATE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, null);
    }

    @Test
    public void jpa_entitylocking_web_testWriteLock001_AMJTA() throws Exception {
        final String testName = "jpa_entitylocking_web_testWriteLock001_AMJTA";
        final String testMethod = "testWriteLock001";
        final String testResource = "test-jpa-resource-amjta";

        Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", testResource);
        testResourcesList.put("test-jpa-2-resource", "test-client-b-resource");

        executeDDL("JPA20_ENTITYLOCKING_DEFAULT_DELETE_${dbvendor}.ddl");
        executeDDL("JPA20_ENTITYLOCKING_DEFAULT_POPULATE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, null);
    }

    @Test
    public void jpa_entitylocking_web_testWriteLock001A_AMJTA() throws Exception {
        final String testName = "jpa_entitylocking_web_testWriteLock001A_AMJTA";
        final String testMethod = "testWriteLock001A";
        final String testResource = "test-jpa-resource-amjta";

        Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", testResource);
        testResourcesList.put("test-jpa-2-resource", "test-client-b-resource");

        executeDDL("JPA20_ENTITYLOCKING_DEFAULT_DELETE_${dbvendor}.ddl");
        executeDDL("JPA20_ENTITYLOCKING_DEFAULT_POPULATE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, null);
    }

    @Test
    public void jpa_entitylocking_web_testWriteLock002_AMJTA() throws Exception {
        final String testName = "jpa_entitylocking_web_testWriteLock002_AMJTA";
        final String testMethod = "testWriteLock002";
        final String testResource = "test-jpa-resource-amjta";

        Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", testResource);
        testResourcesList.put("test-jpa-2-resource", "test-client-b-resource");

        executeDDL("JPA20_ENTITYLOCKING_DEFAULT_DELETE_${dbvendor}.ddl");
        executeDDL("JPA20_ENTITYLOCKING_DEFAULT_POPULATE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, null);
    }

    @Test
    public void jpa_entitylocking_web_testWriteLock002A_AMJTA() throws Exception {
        final String testName = "jpa_entitylocking_web_testWriteLock002A_AMJTA";
        final String testMethod = "testWriteLock002A";
        final String testResource = "test-jpa-resource-amjta";

        Map<String, String> testResourcesList = new HashMap<String, String>();
        testResourcesList.put("test-jpa-resource", testResource);
        testResourcesList.put("test-jpa-2-resource", "test-client-b-resource");

        executeDDL("JPA20_ENTITYLOCKING_DEFAULT_DELETE_${dbvendor}.ddl");
        executeDDL("JPA20_ENTITYLOCKING_DEFAULT_POPULATE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResourcesList, null);
    }

    @Test
    public void jpa_entitylocking_web_testPessimisticReadLock001_AMJTA() throws Exception {
        final String testName = "jpa_entitylocking_web_testPessimisticReadLock001_AMJTA";
        final String testMethod = "testPessimisticReadLock001";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA20_ENTITYLOCKING_DEFAULT_DELETE_${dbvendor}.ddl");
        executeDDL("JPA20_ENTITYLOCKING_DEFAULT_POPULATE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_entitylocking_web_testPessimisticWriteLock001_AMJTA() throws Exception {
        final String testName = "jpa_entitylocking_web_testPessimisticWriteLock001_AMJTA";
        final String testMethod = "testPessimisticWriteLock001";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA20_ENTITYLOCKING_DEFAULT_DELETE_${dbvendor}.ddl");
        executeDDL("JPA20_ENTITYLOCKING_DEFAULT_POPULATE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_entitylocking_web_testPessimisticForceIncrementLock001_AMJTA() throws Exception {
        final String testName = "jpa_entitylocking_web_testPessimisticForceIncrementLock001_AMJTA";
        final String testMethod = "testPessimisticForceIncrementLock001";
        final String testResource = "test-jpa-resource-amjta";

        executeDDL("JPA20_ENTITYLOCKING_DEFAULT_DELETE_${dbvendor}.ddl");
        executeDDL("JPA20_ENTITYLOCKING_DEFAULT_POPULATE_${dbvendor}.ddl");
        executeTest(testName, testMethod, testResource);
    }
}
