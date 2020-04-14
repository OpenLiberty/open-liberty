/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.olgh11795.web;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import com.ibm.ws.jpa.olgh11795.testlogic.JPATestOLGH11795Logic;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
import com.ibm.ws.testtooling.vehicle.web.JPATestServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/TestOLGH11795Servlet")
public class TestOLGH11795Servlet extends JPATestServlet {
    // Container Managed Transaction Scope
    @PersistenceContext(unitName = "OLGH11795_JTA")
    private EntityManager cmtsEm;

    // Application Managed JTA
    @PersistenceUnit(unitName = "OLGH11795_JTA")
    private EntityManagerFactory amjtaEmf;

    // Application Managed Resource-Local
    @PersistenceUnit(unitName = "OLGH11795_RL")
    private EntityManagerFactory amrlEmf;

    @PostConstruct
    private void initFAT() {
        testClassName = JPATestOLGH11795Logic.class.getName();

        jpaPctxMap.put("test-jpa-resource-amjta",
                       new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
        jpaPctxMap.put("test-jpa-resource-amrl",
                       new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
        jpaPctxMap.put("test-jpa-resource-cmts",
                       new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
    }

    @Test
    public void jpa_spec10_olgh11795_testJoinColumnWithSameDuplicateName_AMJTA_Web() throws Exception {
        final String testName = "jpa_spec10_olgh11795_testJoinColumnWithSameDuplicateName_AMJTA_Web";
        final String testMethod = "testJoinColumnWithSameDuplicateName";
        final String testResource = "test-jpa-resource-amjta";

        // TODO: Disable this test for JPA 2.2 testing. But will be fixed when ECL 2.7 is updated
        if (isUsingJPA22Feature()) {
            return;
        }

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_olgh11795_testJoinColumnWithSameDuplicateName_AMRL_Web() throws Exception {
        final String testName = "jpa_spec10_olgh11795_testJoinColumnWithSameDuplicateName_AMRL_Web";
        final String testMethod = "testJoinColumnWithSameDuplicateName";
        final String testResource = "test-jpa-resource-amrl";

        // TODO: Disable this test for JPA 2.2 testing. But will be fixed when ECL 2.7 is updated
        if (isUsingJPA22Feature()) {
            return;
        }

        executeTest(testName, testMethod, testResource);
    }

    @Test
    public void jpa_spec10_olgh11795_testJoinColumnWithSameDuplicateName_CMTS_Web() throws Exception {
        final String testName = "jpa_spec10_olgh11795_testJoinColumnWithSameDuplicateName_CMTS_Web";
        final String testMethod = "testJoinColumnWithSameDuplicateName";
        final String testResource = "test-jpa-resource-cmts";

        // TODO: Disable this test for JPA 2.2 testing. But will be fixed when ECL 2.7 is updated
        if (isUsingJPA22Feature()) {
            return;
        }

        executeTest(testName, testMethod, testResource);
    }
}
