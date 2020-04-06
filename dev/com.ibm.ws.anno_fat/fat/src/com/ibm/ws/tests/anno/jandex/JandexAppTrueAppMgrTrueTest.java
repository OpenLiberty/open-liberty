/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.tests.anno.jandex;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.fat.util.SharedServer;

public class JandexAppTrueAppMgrTrueTest extends JandexAppTest {
    private static final Logger LOG = Logger.getLogger(JandexAppTrueAppMgrTrueTest.class.getName());

    public static SharedServer SHARED_SERVER = new SharedServer("annoFat_server", false);

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        JandexAppTest.setUp(LOG, SHARED_SERVER, "jandexAppTrueAppMgrTrue_server.xml", null);
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        JandexAppTest.tearDown(LOG, SHARED_SERVER);
    }

    //

    @Test
    public void trueTrue_testServletIsRunning() throws Exception {
        super.testServletIsRunning();
    }

    @Test
    public void trueTrue_testServletisRunning31() throws Exception {
        super.testServletIsRunning31();
    }

    @Test
    public void trueTrue_testServletVersions() throws Exception {
        super.testServletVersions();
    }

    @Test
    public void trueTrue_testJandex() throws Exception {
        super.testJandex(JandexAppTest.DO_EXPECT_JANDEX);
    }
}
