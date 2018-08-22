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
package com.ibm.ws.beanvalidation.fat.cdi;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.ws.beanvalidation.fat.basic.BasicValidation_Common;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;

/**
 * Collection of tests to be run when both cdi-1.1 and beanValidation-1.1 are enabled
 * together. Include all common tests from {@link BasicValidation_Common} to ensure
 * that everything that worked without CDI works with it as well.
 */
@RunWith(FATRunner.class)
public class BeanValidation11CDITest extends BeanValidationCDI_Common {

    @Server("com.ibm.ws.beanvalidation.cdi_1.1.fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server, PrivHelper.JAXB_PERMISSION);
        bvalVersion = 11;
        createAndExportCommonWARs(server);
        createAndExportApacheWARs(server);
        createAndExportCDIWARs(server);
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();

        // TODO this needs to be debugged, as currently all @PreDestroy methods
        // created by bean validation get called twice.
//        List<String> destroyedList;
//        destroyedList = server.findStringsInLogs("CustomConstraintValidatorFactory is getting destroyed.");
//        Assert.assertEquals("CustomConstraintValidatorFactory wasn't destroyed once: " + destroyedList,
//                            1, destroyedList.size());
//
//        destroyedList = server.findStringsInLogs("CustomMessageInterpolator is getting destroyed.");
//        Assert.assertEquals("CustomConstraintValidatorFactory wasn't destroyed once: " + destroyedList,
//                            1, destroyedList.size());
//
//        destroyedList = server.findStringsInLogs("TestAnnotationValidator is getting destroyed.");
//        Assert.assertEquals("CustomConstraintValidatorFactory wasn't destroyed once: " + destroyedList,
//                            1, destroyedList.size());

        //Check that server logs are really collected when an application fails to start, if this line is ever re-enabled.
        //Currently they do not get collected when server.stopServer(false) is called.
        //server.postStopServerArchive();
    }

    @Override
    public LibertyServer getServer() {
        return server;
    }
}
