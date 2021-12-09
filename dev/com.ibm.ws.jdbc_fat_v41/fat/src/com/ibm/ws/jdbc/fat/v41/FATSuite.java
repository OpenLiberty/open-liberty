/*******************************************************************************
 * Copyright (c) 2017,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.v41;

import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({
                JDBC41UpgradeTest.class,
                JDBC41Test.class,
                ErrorMappingTest.class,
                ErrorMappingConfigUpdateTest.class
})
public class FATSuite {
    public static final String appName = "basicfat";
    public static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jdbc.fat.v41");

    @ClassRule
    public static RepeatTests r = RepeatTests
                    .withoutModification()
                    .andWith(new JakartaEE9Action().fullFATOnly());

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultApp(server, appName, "jdbc.fat.v41.web");
        exportCustomDriver(server, "derby");
    }

    public static void exportCustomDriver(LibertyServer server, String dir) throws Exception {
        JavaArchive slowdriver = ShrinkHelper.buildJavaArchive("slowdriver.jar", "jdbc.fat.v41.slowdriver");
        ShrinkHelper.exportToServer(server, dir, slowdriver);
    }
}