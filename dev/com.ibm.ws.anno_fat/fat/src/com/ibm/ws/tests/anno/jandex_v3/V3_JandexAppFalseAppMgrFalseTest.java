/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
package com.ibm.ws.tests.anno.jandex_v3;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.fat.util.SharedServer;

public class V3_JandexAppFalseAppMgrFalseTest extends V3_JandexAppTest {
    private static final Logger LOG = Logger.getLogger(V3_JandexAppFalseAppMgrFalseTest.class.getName());

    public static SharedServer SHARED_SERVER = new SharedServer("v3_annoFat_server", false);

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        V3_JandexAppTest.setUp(LOG, SHARED_SERVER, "jandexAppFalseAppMgrFalse_server.xml", null);
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        V3_JandexAppTest.tearDown(LOG, SHARED_SERVER);
    }

    //

    @Test
    public void falseFalse_testServletIsRunning() throws Exception {
        super.testServletIsRunning();
    }

    @Test
    public void falseFalse_testServletisRunning31() throws Exception {
        super.testServletIsRunning31();
    }

    @Test
    public void falseFalse_testServletVersions() throws Exception {
        super.testServletVersions();
    }

    @Test
    public void falseFalse_testJandex() throws Exception {
        super.testJandex(V3_JandexAppTest.DO_NOT_EXPECT_JANDEX);
    }
}
