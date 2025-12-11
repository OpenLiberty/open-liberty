/*******************************************************************************
 * Copyright (c) 2019, 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.thirdparty.apps.hibernateSearchWar.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.annotation.Resource;
import javax.persistence.PersistenceUnit;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;
import javax.persistence.EntityManagerFactory;

import org.junit.Assert;
import org.junit.Test;

import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * 
 * The HibernateSearchTestServlet is intended for demonstrating a straightforward issue where no user activity
 * is required aside from accessing the servlet with a web browser.
 * 
 * For more complicated scenarios where it is desirable to step-by-step demonstrate a problem with
 * navigable steps, use the ActionedTestServlet.
 * 
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/HibernateSearchTestServlet")
public class HibernateSearchTestServlet extends FATServlet {
    @PersistenceUnit(unitName = "TestPU")
    private EntityManagerFactory emf;

    @PersistenceContext(unitName = "TestPU")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @Inject
    EmptyBean bean;

    private static boolean fieldBridgeCalled = false;

    @Test
    @Mode(TestMode.FULL)
    public void testHibernateSearch() throws ServletException, IOException {
        try {
            PrintWriter pw = new PrintWriter(new ByteArrayOutputStream());
            CommonTestCode.populate(pw, em, tx);
            Assert.assertTrue(fieldBridgeCalled);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    public static void registerFieldBridgeCalled() {
        fieldBridgeCalled = true;
    }
    
}
