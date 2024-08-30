/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

 package com.ibm.ws.jpa.olgh29319.web;

 import javax.annotation.PostConstruct;
 import javax.persistence.EntityManager;
 import javax.persistence.EntityManagerFactory;
 import javax.persistence.PersistenceContext;
 import javax.persistence.PersistenceUnit;
 import javax.servlet.annotation.WebServlet;
 
 import org.junit.Test;
 
 import com.ibm.ws.jpa.olgh29319.testlogic.JPATestOLGH29319Logic;
 import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext;
 import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceContextType;
 import com.ibm.ws.testtooling.testinfo.JPAPersistenceContext.PersistenceInjectionType;
 import com.ibm.ws.testtooling.vehicle.web.JPADBTestServlet;
 
 import componenttest.annotation.SkipIfSysProp;
 
 @SuppressWarnings("serial")
 @WebServlet(urlPatterns = "/TestOLGH29319Servlet")
 public class TestOLGH29319Servlet extends JPADBTestServlet {
 
     // Container Managed Transaction Scope
     @PersistenceContext(unitName = "OLGH29319_JTA")
     private EntityManager cmtsEm;
 
     // Application Managed JTA
     @PersistenceUnit(unitName = "OLGH29319_JTA")
     private EntityManagerFactory amjtaEmf;
 
     // Application Managed Resource-Local
     @PersistenceUnit(unitName = "OLGH29319_RL")
     private EntityManagerFactory amrlEmf;
 
     @PostConstruct
     private void initFAT() {
         testClassName = JPATestOLGH29319Logic.class.getName();
 
         jpaPctxMap.put("test-jpa-resource-amjta",
                        new JPAPersistenceContext("test-jpa-resource-amjta", PersistenceContextType.APPLICATION_MANAGED_JTA, PersistenceInjectionType.FIELD, "amjtaEmf"));
         jpaPctxMap.put("test-jpa-resource-amrl",
                        new JPAPersistenceContext("test-jpa-resource-amrl", PersistenceContextType.APPLICATION_MANAGED_RL, PersistenceInjectionType.FIELD, "amrlEmf"));
         jpaPctxMap.put("test-jpa-resource-cmts",
                        new JPAPersistenceContext("test-jpa-resource-cmts", PersistenceContextType.CONTAINER_MANAGED_TS, PersistenceInjectionType.FIELD, "cmtsEm"));
     }
 
     // testEclipseLinkCaseQueryConcurrency
     @Test
     @SkipIfSysProp(SkipIfSysProp.OS_ZOS)
     public void jpa_spec10_query_olgh29319_testEclipseLinkCaseQueryConcurrency_AMJTA_Web() throws Exception {
         final String testName = "jpa10_query_olgh29319_testEclipseLinkCaseQueryConcurrency_AMJTA_Web";
         final String testMethod = "testEclipseLinkCaseQueryConcurrency";
         final String testResource = "test-jpa-resource-amjta";
         executeTest(testName, testMethod, testResource);
     }
 
     @Test
     @SkipIfSysProp(SkipIfSysProp.OS_ZOS)
     public void jpa_spec10_query_olgh29319_testEclipseLinkCaseQueryConcurrency_AMRL_Web() throws Exception {
         final String testName = "jpa10_query_olgh29319_testEclipseLinkCaseQueryConcurrency_AMRL_Web";
         final String testMethod = "testEclipseLinkCaseQueryConcurrency";
         final String testResource = "test-jpa-resource-amrl";
         executeTest(testName, testMethod, testResource);
     }
 
     // @Test // Not a valid scenario
     public void jpa_spec10_query_olgh29319_testEclipseLinkCaseQueryConcurrency_CMTS_Web() throws Exception {
         final String testName = "jpa10_query_olgh29319_testEclipseLinkCaseQueryConcurrency_CMTS_Web";
         final String testMethod = "testEclipseLinkCaseQueryConcurrency";
         final String testResource = "test-jpa-resource-cmts";
         executeTest(testName, testMethod, testResource);
     }
 }
 