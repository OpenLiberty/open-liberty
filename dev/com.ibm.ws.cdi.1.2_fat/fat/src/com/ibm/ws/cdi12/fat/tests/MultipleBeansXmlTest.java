/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi12.fat.tests;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.cdi12.suite.ShutDownSharedServer;
import com.ibm.ws.fat.util.LoggingTest;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

public class MultipleBeansXmlTest extends LoggingTest {

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @ClassRule
    public static ShutDownSharedServer SHARED_SERVER = new ShutDownSharedServer("cdi12MultipleBeansXmlServer");

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.LoggingTest#getSharedServer()
     */
    @Override
    protected ShutDownSharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    @Mode(TestMode.FULL)
    public void testMultipleBeansXml() throws Exception {
        //part of multiModuleApp1
        this.verifyResponse(
                            "/multipleBeansXml/",
                            "MyBean");
    }

    @Test
    public void testMultipleBeansXmlWarningMessage() throws Exception {
        Assert.assertFalse("Test for extension loaded",
                           SHARED_SERVER.getLibertyServer().findStringsInLogs("CWOWB1001W(?=.*multipleBeansXml#multipleBeansXml.war)(?=.*WEB-INF/beans.xml)(?=.*WEB-INF/classes/META-INF/beans.xml)").isEmpty());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (SHARED_SERVER != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            //Expected warning about multiple beans.xml files.
            SHARED_SERVER.getLibertyServer().stopServer("CWOWB1001W");
        }
    }

}
