/*******************************************************************************
 * Copyright (c) 2018,2026 IBM Corporation and others.
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
package com.ibm.ws.tests.anno.jandex;

import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.tests.anno.JandexV1RepeatAction;
import com.ibm.ws.tests.anno.JandexIndexV12RepeatAction;
import com.ibm.ws.tests.anno.JandexIndexV13RepeatAction;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;

@RunWith(FATRunner.class)
public class JandexAppFalseAppMgrTrueTest extends JandexAppTest {
    private static final Logger LOG = Logger.getLogger(JandexAppFalseAppMgrTrueTest.class.getName());

    public static SharedServer SHARED_SERVER = new SharedServer("annoFat_server", false);
    
    @ClassRule
    public static RepeatTests r = RepeatTests.with(new JandexV1RepeatAction()).andWith(new JandexIndexV12RepeatAction()).andWith(new JandexIndexV13RepeatAction());

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        JandexAppTest.setUp(LOG, SHARED_SERVER, "jandexAppFalseAppMgrTrue_server.xml", null);
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        JandexAppTest.tearDown(LOG, SHARED_SERVER);
    }

    //

    @Test
    public void falseTrue_testServletIsRunning() throws Exception {
        super.testServletIsRunning();
    }

    @Test
    public void falseTrue_testServletisRunning31() throws Exception {
        super.testServletIsRunning31();
    }

    @Test
    public void falseTrue_testServletVersions() throws Exception {
        super.testServletVersions();
    }

    @Test
    public void falseTrue_testJandex() throws Exception {
        super.testJandex(JandexAppTest.DO_NOT_EXPECT_JANDEX);
    }
}
