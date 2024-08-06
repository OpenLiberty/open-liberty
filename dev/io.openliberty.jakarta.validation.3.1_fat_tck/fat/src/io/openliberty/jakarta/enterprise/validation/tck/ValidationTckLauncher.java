/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
package io.openliberty.jakarta.enterprise.validation.tck;

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.tck.TCKResultsInfo.Type;
import componenttest.topology.utils.tck.TCKRunner;

/**
 * This is a test class that runs the entire Jakarta Validation TCK against Full Profile.
 *
 * The TCK results are copied in the results/junit directory before the Simplicity FAT framework
 * generates the html report - so there is detailed information on individual
 * tests as if they were running as simplicity junit FAT tests in the standard location.
 */

@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 17)
public class ValidationTckLauncher {

    private final static Map<String, String> additionalProps = new HashMap<>();
    private final static String validationProvider = "org.hibernate.validator.HibernateValidator";
    private final static String validationCustomFeature = "io.openliberty.valThirdParty-3.1";

    @Server("ValidationTCKServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        /*
         * Server config:
         * - Path that jimage will output modules for signature testing
         * - validation.provider for applications to use
         */
        Map<String, String> opts = server.getJvmOptionsAsMap();
        opts.put("-Djimage.dir", server.getServerSharedPath() + "jimage/output/");
        opts.put("-Dvalidation.provider", validationProvider);

        /*
         * Client config:
         * - validation.provider for client to use
         */
        additionalProps.put("validation.provider", validationProvider);

        //Configure server and install user feature
        server.setJvmOptions(opts);
        server.installSystemFeature(validationCustomFeature);

        //Finally start the server
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer(
                          "WLTC0032W", //Transaction rollback warning.
                          "WLTC0033W", //Transaction rollback warning.
                          "CWWKS0901E" //Quickstart security
        );

        server.uninstallSystemFeature(validationCustomFeature);
    }

    /**
     * Run the TCK (controlled by autoFVT/publish/tckRunner/tck/*)
     */
    @Test
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    public void launchValidation31TCK() throws Exception {

        /**
         * The runTCKMvnCmd will set the following properties for use by arquillian
         * [ wlp, tck_server, tck_port, tck_failSafeUndeployment, tck_appDeployTimeout, tck_appUndeployTimeout ]
         * and then run the mvn test command.
         */
        String bucketName = "io.openliberty.jakarta.validation.3.1_fat_tck";
        String testName = this.getClass() + ":launchValidation31TCK";
        Type type = Type.JAKARTA;
        String specName = "Validation ";
        TCKRunner.runTCK(server, bucketName, testName, type, specName, "validation-tck-tests-suite.xml", additionalProps);
    }
}