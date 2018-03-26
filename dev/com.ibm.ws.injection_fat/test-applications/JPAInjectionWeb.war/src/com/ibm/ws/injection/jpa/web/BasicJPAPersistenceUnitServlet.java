/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.injection.jpa.web;

import java.util.logging.Logger;

import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@PersistenceUnit(name = "com.ibm.ws.injection.jpa.web.BasicJPAPersistenceUnitServlet/JNDI_Class_Ann_PU", unitName = "test")
@WebServlet("/BasicJPAPersistenceUnitServlet")
public class BasicJPAPersistenceUnitServlet extends FATServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = BasicJPAPersistenceUnitServlet.class.getName();
    private final Logger svLogger = Logger.getLogger(CLASS_NAME);

    // Annotated targets
    @PersistenceUnit(unitName = "test")
    EntityManagerFactory emfFldAnn;
    EntityManagerFactory emfMthdAnn;

    // XML targets
    EntityManagerFactory emfFldXML;
    EntityManagerFactory emfMthdXML;

    /**
     * Tests annotated injection of an EntityManagerFactory into this servlet
     */
    @Test
    public void testPersistenceUnitAnnInjection() {
        svLogger.info("Connected to servlet via testPersistenceUnitAnnInjection");
        JPATestHelper.testEntityManagerFactory(emfFldAnn, " annotation, field injected");
        JPATestHelper.testEntityManagerFactory(emfMthdAnn, " annotation, method injected");
    }

    /**
     * Tests XML injection of an EntityManagerFactory into this servlet
     */
    @Test
    public void testPersistenceUnitXMLInjection() {
        svLogger.info("Connected to servlet via testPersistenceUnitXMLInjection");
        JPATestHelper.testEntityManagerFactory(emfFldXML, " XML, field injected");
        JPATestHelper.testJNDILookup(CLASS_NAME + "/EntityManagerFactoryFldXML");

        JPATestHelper.testEntityManagerFactory(emfMthdXML, " XML, method injected");
        JPATestHelper.testJNDILookup(CLASS_NAME + "/EntityManagerFactoryMthdXML");
    }

    /**
     * Tests JNDI lookup of PersistenceUnits defined in class level annotations
     * and web.xml.
     */
    @Test
    public void testPersistenceUnitClassLevelReferenceLookup() {
        JPATestHelper.testJNDILookup(CLASS_NAME + "/JNDI_Class_Ann_PU");
    }

    @PersistenceUnit(unitName = "test")
    public void setEntityManagerFactoryAnn(EntityManagerFactory emf) {
        emfMthdAnn = emf;
    }

    public void setEntityManagerFactoryXML(EntityManagerFactory emf) {
        emfMthdXML = emf;
    }
}