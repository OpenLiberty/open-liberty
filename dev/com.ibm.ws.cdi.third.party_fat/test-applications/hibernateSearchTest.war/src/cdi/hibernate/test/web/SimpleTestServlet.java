/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cdi.hibernate.test.web;

import static componenttest.annotation.SkipForRepeat.NO_MODIFICATION;
import static org.junit.Assert.fail; 
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import javax.inject.Inject;

/**
 * 
 * The SimpleTestServlet is intended for demonstrating a straightforward issue where no user activity
 * is required aside from accessing the servlet with a web browser.
 * 
 * For more complicated scenarios where it is desirable to step-by-step demonstrate a problem with
 * navigable steps, use the ActionedTestServlet.
 * 
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/SimpleTestServlet")
public class SimpleTestServlet extends FATServlet {
    @PersistenceContext(unitName = "TestPU")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @Inject
    EmptyBean bean;

    private static boolean fieldBridgeCalled = false;
    
    @Test
    @Mode(TestMode.FULL)
    @SkipForRepeat(NO_MODIFICATION)
    public void testHibernateSearch()
            throws ServletException, IOException {
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
