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
package com.ibm.ws.jta.fat;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.topology.impl.LibertyServerFactory;

public class XATest extends AbstractTxFAT {
    private static final String CONTEXT = "jta";
    private static final String SERVER_NAME = "com.ibm.ws.jta.fat";

    private static final Set<String> appNames = new TreeSet<String>(Arrays.asList(CONTEXT));

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME);
        server.installSystemFeature("jtafat-1.0");

        WebArchive war = ShrinkWrap.create(WebArchive.class, CONTEXT + ".war");
        war.addPackage("web");
        war.addAsWebInfResource(new File("test-applications/" + CONTEXT + "/resources/WEB-INF/web.xml"));
        ShrinkHelper.exportToServer(server, "dropins", war);

        for (String name : appNames)
            server.addInstalledAppForValidation(name);

        server.restartServer();
    }

    @Override
    public void beforeTest() throws Exception {
        Log.info(getClass(), "beforeTest", "Starting test: " + testName.getMethodName());
    }

    @Override
    public void afterTest() {
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("WTRN0076W", "WTRN0048W", "WTRN0075W");
        }
    }

    @Test
    public void testXA001() throws Exception {
        runInServlet("testXA001");
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "jakarta.transaction.RollbackException" })
    public void testXA002() throws Exception {
        runInServlet("testXA002");
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "jakarta.transaction.RollbackException" })
    public void testXA003() throws Exception {
        runInServlet("testXA003");
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "jakarta.transaction.RollbackException" })
    public void testXA004() throws Exception {
        runInServlet("testXA004");
    }

    @Test
    public void testXA005() throws Exception {
        runInServlet("testXA005");
    }

    @Test
    public void testXA006() throws Exception {
        runInServlet("testXA006");
    }

    @Test
    public void testXA007() throws Exception {
        runInServlet("testXA007");
    }

    @Test
    public void testXA008() throws Exception {
        runInServlet("testXA008");
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "jakarta.transaction.RollbackException" })
    public void testXA009() throws Exception {
        runInServlet("testXA009");
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "jakarta.transaction.RollbackException" })
    public void testXA010() throws Exception {
        runInServlet("testXA010");
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "jakarta.transaction.RollbackException" })
    public void testXA011() throws Exception {
        runInServlet("testXA011");
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException", "jakarta.transaction.RollbackException" })
    public void testXA012() throws Exception {
        runInServlet("testXA012");
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testXA013() throws Exception {
        runInServlet("testXA013");
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testXA014() throws Exception {
        runInServlet("testXA014");
    }

    @Test
    @ExpectedFFDC(value = { "javax.transaction.xa.XAException" })
    public void testXA015() throws Exception {
        runInServlet("testXA015");
    }

    @Override
    protected String getTest() {
        return "xa";
    }

    @Override
    protected String getContext() {
        return CONTEXT;
    }
}