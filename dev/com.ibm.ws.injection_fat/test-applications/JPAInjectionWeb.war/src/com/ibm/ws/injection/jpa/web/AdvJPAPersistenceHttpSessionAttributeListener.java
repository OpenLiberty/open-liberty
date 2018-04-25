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

import java.util.HashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;

@PersistenceContext(name = "com.ibm.ws.injection.jpa.web.AdvJPAPersistenceHttpSessionAttributeListener/JNDI_Class_Ann_PC", unitName = "test")
@PersistenceUnit(name = "com.ibm.ws.injection.jpa.web.AdvJPAPersistenceHttpSessionAttributeListener/JNDI_Class_Ann_PU", unitName = "test")
public class AdvJPAPersistenceHttpSessionAttributeListener implements HttpSessionAttributeListener {
    private static final String CLASS_NAME = AdvJPAPersistenceHttpSessionAttributeListener.class.getName();

    HashMap<String, Object> map;

    String[] JNDI_NAMES = {
                            CLASS_NAME + "/EntityManagerFldXML",
                            CLASS_NAME + "/EntityManagerMthdXML",
                            CLASS_NAME + "/EntityManagerFactoryFldXML",
                            CLASS_NAME + "/EntityManagerFactoryMthdXML",
                            CLASS_NAME + "/JNDI_Class_Ann_PC",
                            CLASS_NAME + "/JNDI_Class_Ann_PU",
    };

    /* Persistence Context targets */
    // Annotated targets
    @PersistenceContext(unitName = "test")
    EntityManager emFldAnn;
    EntityManager emMthdAnn;

    // XML targets
    EntityManager emFldXML;
    EntityManager emMthdXML;

    /* Persistence Unit Targets */
    // Annotated targets
    @PersistenceUnit(unitName = "test")
    EntityManagerFactory emfFldAnn;
    EntityManagerFactory emfMthdAnn;

    // XML targets
    EntityManagerFactory emfFldXML;
    EntityManagerFactory emfMthdXML;

    public AdvJPAPersistenceHttpSessionAttributeListener() {
        map = new HashMap<String, Object>();
    }

    @Override
    public void attributeAdded(HttpSessionBindingEvent arg0) {
        doWork(WCEventTracker.KEY_LISTENER_ADD_AdvJPAPersistenceHttpSessionAttributeListener);
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent arg0) {
        doWork(WCEventTracker.KEY_LISTENER_DEL_AdvJPAPersistenceHttpSessionAttributeListener);
    }

    @Override
    public void attributeReplaced(HttpSessionBindingEvent arg0) {
        doWork(WCEventTracker.KEY_LISTENER_REP_AdvJPAPersistenceHttpSessionAttributeListener);
    }

    public void doWork(String key) {
        populateMap();
        JPATestHelper.processRequest(key, map);
        JPATestHelper.testJNDILookupWrapper(key, JNDI_NAMES);
    }

    public void populateMap() {
        map.clear();
        map.put("emFldAnn", emFldAnn);
        map.put("emMthdAnn", emMthdAnn);
        map.put("emFldXML", emFldXML);
        map.put("emMthdXML", emMthdXML);

        map.put("emfFldAnn", emfFldAnn);
        map.put("emfMthdAnn", emfMthdAnn);
        map.put("emfFldXML", emfFldXML);
        map.put("emfMthdXML", emfMthdXML);
    }

    @PersistenceContext(unitName = "test")
    public void setEntityManagerAnn(EntityManager em) {
        emMthdAnn = em;
    }

    public void setEntityManagerXML(EntityManager em) {
        emMthdXML = em;
    }

    @PersistenceUnit(unitName = "test")
    public void setEntityManagerFactoryAnn(EntityManagerFactory emf) {
        emfMthdAnn = emf;
    }

    public void setEntityManagerFactoryXML(EntityManagerFactory emf) {
        emfMthdXML = emf;
    }
}