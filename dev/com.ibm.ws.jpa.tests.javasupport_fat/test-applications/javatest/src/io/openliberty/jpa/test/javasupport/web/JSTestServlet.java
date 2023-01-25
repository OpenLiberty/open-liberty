/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

package io.openliberty.jpa.test.javasupport.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.UserTransaction;

import componenttest.app.AssertionErrorSerializer;
import io.openliberty.jpa.test.javasupport.model.JSEntity;
import junit.framework.Assert;

@WebServlet(urlPatterns = "/JSTestServlet")
public class JSTestServlet extends HttpServlet {
    private static final long serialVersionUID = 2549590008493151755L;

    @PersistenceContext(unitName = "JSPersistence")
    private EntityManager em;

    @Resource
    private UserTransaction tx;

    @PostConstruct
    private void initFAT() {

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        System.out.println(">>> BEGIN: ");
        System.out.println("Request URL: " + request.getRequestURL() + '?' + request.getQueryString());
        PrintWriter writer = response.getWriter();

        try {
            testCRUD();
            writer.println("[TEST PASSED]");
        } catch (Throwable t) {
            if (t instanceof InvocationTargetException) {
                t = t.getCause();
            }

            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            System.err.print(sw);
            if (t instanceof AssertionError && t.getCause() == null) {
                AssertionError e = (AssertionError) t;
                System.out.println("ASSERTION ERROR: " + e);
                writer.write(AssertionErrorSerializer.START_TAG);
                AssertionErrorSerializer.serialize(e, writer);
                writer.write(AssertionErrorSerializer.END_TAG);
            } else {
                System.out.println("ERROR: " + t);
                writer.println("ERROR: Caught exception attempting to call test method testCRUD on servlet " + getClass().getName());
                t.printStackTrace(writer);
            }
        }

        writer.flush();
        writer.close();

        System.out.println("<<< END:   ");
    }

    public void testCRUD() throws Exception {
        JSEntity newEntity = new JSEntity();
        newEntity.setStrData("A fist full of data");

        tx.begin();
        em.persist(newEntity);
        tx.commit();

        em.clear();

        JSEntity findEntity = em.find(JSEntity.class, newEntity.getId());
        Assert.assertNotNull(findEntity);
        Assert.assertNotSame(findEntity, newEntity);
        Assert.assertEquals(findEntity.getStrData(), "A fist full of data");

        findEntity.setStrData("For a few datas more");
        tx.begin();
        em.merge(findEntity);
        tx.commit();

        em.clear();

        JSEntity findEntity2 = em.find(JSEntity.class, newEntity.getId());
        Assert.assertNotNull(findEntity2);
        Assert.assertNotSame(findEntity2, findEntity);
        Assert.assertEquals(findEntity2.getStrData(), "For a few datas more");

        tx.begin();
        JSEntity removeEntity = em.find(JSEntity.class, newEntity.getId());
        em.remove(removeEntity);
        tx.commit();

        em.clear();

        JSEntity findEntity3 = em.find(JSEntity.class, newEntity.getId());
        Assert.assertNull(findEntity3);

        // Now Verify Enhancement
        System.out.println("Verifying Enhancement");
        Class entityClass = JSEntity.class;
        Class[] ifaces = entityClass.getInterfaces();
        if (ifaces != null && ifaces.length > 0) {
            System.out.println("<br>Interfaces:");
            for (Class i : ifaces) {
                System.out.println("<br>     " + i.getName());
            }
        } else {
            Assert.fail("No Interfaces Found for " + entityClass + ", which means it has not been enhanced.");
        }
    }
}
