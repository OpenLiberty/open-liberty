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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

@PersistenceContext(name = "com.ibm.ws.injection.jpa.web.BasicJPAPersistenceContextServlet/JNDI_Class_Ann_PC", unitName = "test")
@WebServlet("/BasicJPAPersistenceContextServlet")
public class BasicJPAPersistenceContextServlet extends FATServlet {
    private static final long serialVersionUID = 1L;
    private static final String CLASS_NAME = BasicJPAPersistenceContextServlet.class.getName();
    private final Logger svLogger = Logger.getLogger(CLASS_NAME);

    // Annotated targets
    @PersistenceContext(unitName = "test")
    EntityManager emFldAnn;
    EntityManager emMthdAnn;

    // XML targets
    EntityManager emFldXML;
    EntityManager emMthdXML;

    /**
     * Tests annotated injection of an EntityManager into a servlet
     */
    @Test
    public void testPersistenceContextAnnInjection() {
        svLogger.info("Connected to servlet via testPersistenceContextAnnInjection");
        JPATestHelper.testEntityManager(emFldAnn, "emFldAnn");
        JPATestHelper.testEntityManager(emMthdAnn, "emMthdAnn");
    }

    /**
     * Tests XML injection of an EntityManager into this servlet
     */
    @Test
    public void testPersistenceContextXMLInjection() {
        svLogger.info("Connected to servlet via testPersistenceContextXMLInjection");
        JPATestHelper.testEntityManager(emFldXML, "emFldXML");
        JPATestHelper.testJNDILookup(CLASS_NAME + "/EntityManagerFldXML");

        JPATestHelper.testEntityManager(emMthdXML, "emMthdXML");
        JPATestHelper.testJNDILookup(CLASS_NAME + "/EntityManagerMthdXML");
    }

    /**
     * Tests class-level @PersistenceContext by looking up the JNDI name and
     * testing the resulting EntityManager
     */
    @Test
    public void testPersistenceContextClassLevelReferenceLookup() {
        JPATestHelper.testJNDILookup(CLASS_NAME + "/JNDI_Class_Ann_PC");
    }

    @PersistenceContext(unitName = "test")
    public void setEntityManagerAnn(EntityManager em) {
        emMthdAnn = em;
    }

    public void setEntityManagerXML(EntityManager em) {
        emMthdXML = em;
    }
}