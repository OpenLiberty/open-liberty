/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.bval.v20.fat;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.ibm.ws.beanvalidation.fat.FATSuite;

import componenttest.topology.impl.LibertyServerFactory;

/**
 * All Bean Validation tests for the 1.1 feature level.
 */
public class BasicValidation11Test extends BasicValidation11CommonTest {

    public BasicValidation11Test() {
        isUsingBval11 = true;
    }

    @BeforeClass
    public static void setUp() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.beanvalidation_1.1.fat");

        FATSuite.createAndExportWARs();

        server.addInstalledAppForValidation(FATSuite.DEFAULT_BEAN_VALIDATION10);
        server.addInstalledAppForValidation(FATSuite.DEFAULT_BEAN_VALIDATION11);
        server.addInstalledAppForValidation(FATSuite.BEAN_VALIDATION10);
        server.addInstalledAppForValidation(FATSuite.BEAN_VALIDATION11);
        server.startServer(BasicValidation11Test.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

}
