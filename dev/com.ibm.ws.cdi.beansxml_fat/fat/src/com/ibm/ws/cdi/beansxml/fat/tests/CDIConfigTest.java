/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.cdi.beansxml.fat.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.Cdi;
import com.ibm.websphere.simplicity.config.Cdi12;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.EERepeatActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class CDIConfigTest {
    public static final String SERVER_NAME = "cdi12ConfigServer";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE10, EERepeatActions.EE9, EERepeatActions.EE7); //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code

    @After
    public void setup() throws Exception {
        //clear all the CDI config after each test
        ServerConfiguration config = server.getServerConfiguration();

        ConfigElementList<Cdi12> cdi12s = config.getCdi12();
        cdi12s.clear();

        ConfigElementList<Cdi> cdis = config.getCdi();
        cdis.clear();

        server.updateServerConfiguration(config);
    }

    @Test
    public void testCdi12Warning() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();

        ConfigElementList<Cdi12> cdi12s = config.getCdi12();
        Cdi12 cdi12 = new Cdi12();
        cdi12.setEnableImplicitBeanArchives(true);
        cdi12s.add(cdi12);

        ConfigElementList<Cdi> cdis = config.getCdi();
        Cdi cdi = new Cdi();
        cdi.setEnableImplicitBeanArchives(true);
        cdis.add(cdi);

        server.updateServerConfiguration(config);
        try {
            server.startServer();
            List<String> infoMessages = server.findStringsInLogs("CWOWB1015I: The cdi12 configuration element is superseded by the cdi configuration element.");
            assertTrue("Message CWOWB1015I not found", infoMessages.size() > 0);
            assertEquals("Message CWOWB1015I was found more than once", 1, infoMessages.size());
        } finally {
            if (server.isStarted()) {
                server.stopServer("CWOWB1009W");
            }
        }
    }

    @Test
    /**
     * If emptyBeansXmlCDI3Compatibility is set when using a CDI version less than 4.0, the attribute should be just
     * silently ignored. This test set the attribute, starts the server and shuts down again. It checks there were NO
     * unexpected warnings/errors. This should remain the case even when using CDI 4.0.
     *
     * @throws Exception
     */
    public void testEmptyBeansXmlCDI3CompatibilityWarning() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Cdi> cdis = config.getCdi();
        Cdi cdi = new Cdi();
        cdi.setEmptyBeansXmlCDI3Compatibility(true);
        cdis.add(cdi);

        server.updateServerConfiguration(config);
        try {
            server.startServer();
        } finally {
            if (server.isStarted()) {
                server.stopServer();
            }
        }
    }

    @Test
    public void testEnableImplicitBeanArchivesWarning() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Cdi12> cdi12s = config.getCdi12();
        Cdi12 cdi12 = new Cdi12();
        cdi12.setEnableImplicitBeanArchives(true);
        cdi12s.add(cdi12);

        ConfigElementList<Cdi> cdis = config.getCdi();
        Cdi cdi = new Cdi();
        cdi.setEnableImplicitBeanArchives(false);
        cdis.add(cdi);

        server.updateServerConfiguration(config);
        try {
            server.startServer();
            List<String> warningMessages = server.findStringsInLogs("CWOWB1017W: The enableImplicitBeanArchives attribute was set on both cdi12 and cdi configuration elements. The value from the cdi element is used and the value in the cdi12 element is ignored.");
            assertTrue("Message CWOWB1017W not found", warningMessages.size() > 0);
            assertEquals("Message CWOWB1017W was found more than once", 1, warningMessages.size());
        } finally {
            if (server.isStarted()) {
                server.stopServer("CWOWB1009W", "CWOWB1017W");
            }
        }
    }

    @Test
    public void testEnableImplicitBeanArchivesNoWarning() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Cdi12> cdi12s = config.getCdi12();
        Cdi12 cdi12 = new Cdi12();
        cdi12.setEnableImplicitBeanArchives(true);
        cdi12s.add(cdi12);

        ConfigElementList<Cdi> cdis = config.getCdi();
        Cdi cdi = new Cdi();
        cdi.setEnableImplicitBeanArchives(true); // both versions of the attribute are set the same so should be no warning
        cdis.add(cdi);

        server.updateServerConfiguration(config);
        try {
            server.startServer();
            List<String> warningMessages = server.findStringsInLogs("CWOWB1017W: The enableImplicitBeanArchives attribute was set on both cdi12 and cdi configuration elements. The value from the cdi element is used and the value in the cdi12 element is ignored.");
            assertTrue("Message CWOWB1017W was found when it should not have been", warningMessages.size() == 0);
        } finally {
            if (server.isStarted()) {
                server.stopServer("CWOWB1009W");
            }
        }
    }
}
