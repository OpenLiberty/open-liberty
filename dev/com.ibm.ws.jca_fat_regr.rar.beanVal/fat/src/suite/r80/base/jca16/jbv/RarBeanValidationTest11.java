/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package suite.r80.base.jca16.jbv;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerFactory;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class RarBeanValidationTest11 extends RarBeanValidationTestCommon {
    private final static String CLASSNAME = "RarBeanValidationTest11";
    private final static Logger logger = Logger.getLogger(CLASSNAME);

    @BeforeClass
    public static void setUp() throws Exception {

        if (logger.isLoggable(Level.FINER))
            logger.entering(CLASSNAME, "setUp");
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jca.fat.regr.bval11", null, false);
        server.setServerConfigurationFile("JBV" + cfgFileExtn); // Empty Configuration.
        originalServerConfig = server.getServerConfiguration().clone();

        RarBeanValidationTestCommon.setupResources(server);
        server.startServer("RarBeanValidationTest.log");
    }
}
