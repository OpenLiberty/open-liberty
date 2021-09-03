/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.exception2x.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.servlet.annotation.WebServlet;
import javax.transaction.UserTransaction;

import org.junit.Test;

import com.ibm.websphere.ejbcontainer.test.tools.FATHelper;
import com.ibm.ws.ejbcontainer.exception2x.ejb.SFRa;
import com.ibm.ws.ejbcontainer.exception2x.ejb.SFRaHome;

import componenttest.annotation.ExpectedFFDC;
import componenttest.app.FATServlet;

/**
 * <dl>
 * <dt>Test Name:
 * <dd>WWSTestEjbMthd_SFRTTest
 *
 * <dt>Test Descriptions:
 * <dd>EJB Container Exception on EJB.ejbMethods tests:
 *
 * <dt>Command options:
 * <dd>
 * <TABLE width="100%">
 * <COL span="1" width="25%" align="left"> <COL span="1" align="left">
 * <TBODY>
 * <TR> <TH>Option</TH> <TH>Description</TH> </TR>
 * <TR> <TD>None</TD>
 * <TD></TD>
 * </TR>
 * </TBODY>
 * </TABLE>
 *
 * <dt>Test Matrix:
 * <dd>
 * <br>Sub-tests
 * <ul>
 * <li>ejbMthd_normal - No Exception
 * <li>ejbMthd_create - Exception on ejbCreate
 * <li>ejbMthd_postCreate - Exception on ejbPostCreate
 * <li>ejbMthd_Remove - Exception on ejbRemove
 * <li>PQ70353 - Run regression test, which includes all of the above tests.
 * </ul>
 * <br>Data Sources
 * </dl>
 */
@SuppressWarnings("serial")
@WebServlet("/LifecycleMethod2xServlet")
public class LifecycleMethod2xServlet extends FATServlet {
    private static final String CLASS_NAME = LifecycleMethod2xServlet.class.getName();
    private static final Logger svLogger = Logger.getLogger(CLASS_NAME);

    private static SFRaHome fhome1;

    private static final String fksNormal = Integer.toString(SFRa.Normal);
    private static SFRa fejbNormal;
    private static final String fksCreate = Integer.toString(SFRa.Create);
    private static SFRa fejbCreate;
    private static final String fksPostCreate = Integer.toString(SFRa.PostCreate);
    private static SFRa fejbPostCreate;
    private static final String fksRemove = Integer.toString(SFRa.Remove);
    private static SFRa fejbRemove;

    @PostConstruct
    private void initializeHome() {
        try {
            fhome1 = (SFRaHome) PortableRemoteObject.narrow(new InitialContext().lookup("java:app/Exception2xBean/SFRaEJB"), SFRaHome.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //
    // ejbMthd_normal - Normal call, no exception
    //
    @Test
    public void testNormal() throws Exception {
        svLogger.info("-----> ejbMthd_normal - Normal call, no exception - started.");
        String pKey = "ejbMthd_normal_Key";
        fejbNormal = null;
        UserTransaction ut = null;
        try {
            fejbNormal = fhome1.create(fksNormal, SFRa.Normal, fksNormal);
            assertNotNull("      Created ejb instance - normally", fejbNormal);

            ut = FATHelper.lookupUserTransaction();
            assertNotNull("      Get UserTransaction", ut);
            ut.begin();

            String echoString = fejbNormal.echoRequired(pKey);
            assertEquals("      test echoRequired(" + pKey + ").", echoString, pKey + ":" + fksNormal);
            assertEquals("      test getPKey().", fejbNormal.getPKey(), fksNormal);
            assertEquals("      test getIntValue().", fejbNormal.getIntValue(), SFRa.Normal);
            assertEquals("      test getStringValue().", fejbNormal.getStringValue(), fksNormal);
        } finally {
            FATHelper.cleanupUserTransaction(ut);
            if (fejbNormal != null) {
                fejbNormal.remove();
                fejbNormal = null;
            }
        }
        svLogger.info("<----- ejbMthd_normal - Normal call, no exception - ended.");
    }

    //
    // ejbMthd_create - Exception thrown on ejbCreate
    //
    @Test
    @ExpectedFFDC({ "com.ibm.ejs.container.CreateFailureException", "com.ibm.ws.LocalTransaction.RolledbackException" })
    public void testCreate() throws Exception {
        svLogger.info("-----> ejbMthd_create - Exception thrown on ejbCreate - started.");
        String pKey = "ejbMthd_create_Key";
        fejbNormal = null;
        fejbCreate = null;
        UserTransaction ut = null;
        try {
            fejbNormal = fhome1.create(pKey, SFRa.Normal, pKey);
            assertNotNull("      Created ejb instance - normally", fejbNormal);
            fejbCreate = fhome1.create(fksCreate, SFRa.Create, fksCreate);
            fail("      Unexpected return from successfull create.");
        } catch (RemoteException rex) {
            String exString = rex.toString();
            svLogger.info("      Caught expected " + rex.getClass().getName());
            assertContains("      Check for CreateFailureException.", "com.ibm.ejs.container.CreateFailureException", exString);

            ut = FATHelper.lookupUserTransaction();
            assertNotNull("      Get UserTransaction", ut);
            ut.begin();

            String echoString = fejbNormal.echoRequired(pKey);
            assertEquals("      test echoRequired( " + pKey + " ).", echoString, pKey + ":" + pKey);
            assertEquals("      test getPKey().", fejbNormal.getPKey(), pKey);
            assertEquals("      test getIntValue().", fejbNormal.getIntValue(), SFRa.Normal);
            assertEquals("      test getStringValue().", fejbNormal.getStringValue(), pKey);
        } finally {
            FATHelper.cleanupUserTransaction(ut);

            if (fejbNormal != null) {
                fejbNormal.remove();
                fejbNormal = null;
            }

            if (fejbCreate != null) {
                fejbCreate.remove();
                fejbCreate = null;
            }
        }
        svLogger.info("<----- ejbMthd_create - Exception thrown on ejbCreate - ended.");
    }

    //
    // ejbMthd_postCreate - Exception thrown on ejbPostCreate
    //
    @Test
    public void testPostCreate() throws Exception {
        svLogger.info("-----> ejbMthd_postCreate - Exception thrown on ejbPostCreate - started.");
        String pKey = "ejbMthd_postCreate_Key";
        fejbNormal = null;
        fejbPostCreate = null;
        UserTransaction ut = null;
        try {
            fejbNormal = fhome1.create(pKey, SFRa.Normal, pKey);
            assertNotNull("      Created ejb instance - normally", fejbNormal);
            fejbPostCreate = fhome1.create(fksPostCreate, SFRa.PostCreate, fksPostCreate);
            assertNotNull("      Created ejb instance - normally", fejbPostCreate);
            ut = FATHelper.lookupUserTransaction();
            assertNotNull("      Get UserTransaction", ut);
            ut.begin();

            String echoString = fejbNormal.echoRequired(pKey);
            assertEquals("      test normal.echoRequired( " + pKey + " ).", echoString, pKey + ":" + pKey);
            assertEquals("      test getPKey().", fejbNormal.getPKey(), pKey);
            assertEquals("      test getIntValue().", fejbNormal.getIntValue(), SFRa.Normal);
            assertEquals("      test getStringValue().", fejbNormal.getStringValue(), pKey);

            echoString = fejbPostCreate.echoRequired(pKey);
            assertEquals("      test postCreate.echoRequired( " + pKey + " ).", echoString, pKey + ":" + fksPostCreate);
            assertEquals("      test getPKey().", fejbPostCreate.getPKey(), fksPostCreate);
            assertEquals("      test getIntValue().", fejbPostCreate.getIntValue(), SFRa.PostCreate);
            assertEquals("      test getStringValue().", fejbPostCreate.getStringValue(), fksPostCreate);
        } finally {
            FATHelper.cleanupUserTransaction(ut);
            if (fejbNormal != null) {
                fejbNormal.remove();
                fejbNormal = null;
            }
            if (fejbPostCreate != null) {
                fejbPostCreate.remove();
                fejbPostCreate = null;
            }
        }
        svLogger.info("<----- ejbMthd_postCreate - Exception thrown on ejbPostCreate - ended.");
    }

    //
    // ejbMthd_remove - Exception thrown on ejbRemove
    //
    @Test
    @ExpectedFFDC({ "java.lang.RuntimeException" })
    public void testRemove() throws Exception {
        svLogger.info("-----> ejbMthd_remove - Exception thrown on ejbRemove - started.");
        String pKey = "ejbMthd_Remove_Key";
        fejbRemove = null;
        try {
            fejbRemove = fhome1.create(pKey, SFRa.Remove, fksRemove);
            assertNotNull("      Created ejb instance - normally", fejbRemove);
            fejbRemove.remove();
            svLogger.info("      Return successfully from remove().");
            fejbRemove = null;
        } finally {
            if (fejbRemove != null) {
                fejbRemove.remove();
                fejbRemove = null;
            }
        }
        svLogger.info("<----- ejbMthd_remove - Exception thrown on ejbRemove - ended.");
    }

    private static void assertContains(String message, String expected, String actual) {
        if (actual == null || actual.indexOf(expected) == -1) {
            fail("String \"" + actual + "\" does not contain \"" + expected + "\"");
        }
    }
}