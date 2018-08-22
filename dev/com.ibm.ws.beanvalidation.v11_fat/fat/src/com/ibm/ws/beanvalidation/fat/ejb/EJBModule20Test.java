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
package com.ibm.ws.beanvalidation.fat.ejb;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;

/**
 * Run ejb module tests on bval-2.0.
 *
 * Test various combinations where an application is packaged with one web module
 * and either one or two ejb modules. The web module does not include a validation.xml
 * and both ejb modules do. This covers what validation.xml is found both by the
 * container and provider and needs to be common between bval-1.0 and bval-1.1.
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 8)
public class EJBModule20Test extends EJBModule_Common {

    @Server("com.ibm.ws.beanvalidation.ejb_2.0.fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server, PrivHelper.JAXB_PERMISSION);
        createAndExportEJBWARs(server);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Override
    protected LibertyServer getServer() {
        return server;
    }
}
