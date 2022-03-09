/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import componenttest.annotation.SkipForRepeat;
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
    public static RepeatTests r = EERepeatActions.repeat(SERVER_NAME, EERepeatActions.EE9, EERepeatActions.EE10, EERepeatActions.EE7); //not bothering to repeat with EE8 ... the EE9 version is mostly a transformed version of the EE8 code

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
            List<String> warningMessages = server.findStringsInLogs("CWOWB1015W: The cdi12 configuration element is superseded by the cdi configuration element.");
            assertTrue("Message CWOWB1015W not found", warningMessages.size() > 0);
            assertEquals("Message CWOWB1015W was found more than once", 1, warningMessages.size());
        } finally {
            if (server.isStarted()) {
                server.stopServer("CWOWB1009W", "CWOWB1015W");
            }
        }
    }

    @Test
    @SkipForRepeat(EERepeatActions.EE10_ID)
    public void testCdiEmptyBeansXMLExplicitArchiveWarning() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Cdi> cdis = config.getCdi();
        Cdi cdi = new Cdi();
        cdi.setEmptyBeansXMLExplicitArchive(true);
        cdis.add(cdi);

        server.updateServerConfiguration(config);
        try {
            server.startServer();
            List<String> warningMessages = server.findStringsInLogs("CWOWB1016W: The emptyBeansXMLExplicitArchive attribute of the cdi configuration element is supported only on CDI 4.0 or newer. This attribute is ignored.");
            assertTrue("Message CWOWB1016W not found", warningMessages.size() > 0);
            assertEquals("Message CWOWB1016W was found more than once", 1, warningMessages.size());
        } finally {
            if (server.isStarted()) {
                server.stopServer("CWOWB1015W", "CWOWB1016W");
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
                server.stopServer("CWOWB1009W", "CWOWB1015W", "CWOWB1017W");
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
                server.stopServer("CWOWB1009W", "CWOWB1015W");
            }
        }
    }
}
