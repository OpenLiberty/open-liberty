/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.MSSQLServerContainer;

import componenttest.containers.ExternalTestServiceDockerClientStrategy;
import componenttest.containers.SimpleLogConsumer;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({ FailoverTest.class })
public class FATSuite {

    //Required to ensure we calculate the correct strategy each run even when
    //switching between local and remote docker hosts.
    static {
        ExternalTestServiceDockerClientStrategy.setupTestcontainers();
    }

    // Using the RepeatTests @ClassRule will cause all tests to be run twice.
    // First without any modifications, then again with all features upgraded to
    // their EE8 equivalents.
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification().andWith(FeatureReplacementAction.EE8_FEATURES());

    @ClassRule
    public static MSSQLServerContainer<?> sqlserver = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2019-CU2-ubuntu-16.04")//
                    .withLogConsumer(new SimpleLogConsumer(FATSuite.class, "SQLServer"))
                    .acceptLicense();
}
