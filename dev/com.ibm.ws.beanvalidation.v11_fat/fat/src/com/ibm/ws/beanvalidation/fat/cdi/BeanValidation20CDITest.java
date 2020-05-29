/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.beanvalidation.fat.cdi;

import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.beanvalidation.fat.basic.BasicValidation_Common;

import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.PrivHelper;

/**
 * Collection of tests to be run when both cdi-2.0 and beanValidation-2.0 are enabled
 * together. Include all common tests from {@link BasicValidation_Common} to ensure
 * that everything that worked without CDI works with it as well.
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 8)
// TODO: Remove skip when injection is enabled for jakartaee9; issue #12435
@SkipForRepeat({ SkipForRepeat.EE9_FEATURES })
public class BeanValidation20CDITest extends BeanValidationCDI_Common {

    @Server("com.ibm.ws.beanvalidation.cdi_2.0.fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        PrivHelper.generateCustomPolicy(server, PrivHelper.JAXB_PERMISSION);
        bvalVersion = 20;
        createAndExportCommonWARs(server);
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

    /**
     * Test that a servlet can use @Resource to inject a ValidatorFactory that
     * configures a custom MessageInterpolator. This custom component uses @Inject
     * to implement the interface.
     */
    @Test
    public void testDynamicStartStopOfCDI() throws Exception {
        ServerConfiguration config = getServer().getServerConfiguration();

        //Run with CDI enabled.
        run("BeanValidationCDI_11", "BValAtResourceServlet", "testCDIInjectionInInterpolatorAtResource");

        try {
            //Run with CDI disabled.
            config.getFeatureManager().getFeatures().remove("cdi-2.0");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton("BeanValidationCDI_11"), true, "CWWKZ000[13]I.* BeanValidationCDI_11");
            run("BeanValidationCDI_11", "BValAtResourceServlet", "testDynamicStopOfCDI");

            //Run again with CDI enabled.
            config.getFeatureManager().getFeatures().add("cdi-2.0");
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(Collections.singleton("BeanValidationCDI_11"), true, "CWWKZ000[13]I.* BeanValidationCDI_11");
            run("BeanValidationCDI_11", "BValAtResourceServlet", "testCDIInjectionInInterpolatorAtResource");
        } finally {
            //Make sure all test applications are up and running after toggling the CDI feature.
            config.getFeatureManager().getFeatures().add("cdi-2.0");
            server.updateServerConfiguration(config);
            server.stopServer();
            server.startServer();
        }
    }
}
