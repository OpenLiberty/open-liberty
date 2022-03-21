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
package io.openliberty.jakarta.jsonb.tck;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.BeforeClass;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.MinimumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.MvnUtils;

/**
 * This is a test class that runs the whole Jakarta JSON-B TCK. The TCK results
 * are copied in the results/junit directory before the Simplicity FAT framework
 * generates the html report - so there is detailed information on individual
 * tests as if they were running as simplicity junit FAT tests in the standard
 * location.
 */
@RunWith(FATRunner.class)
@MinimumJavaLevel(javaLevel = 11)
public class JsonbTckLauncher {

    final static Map<String, String> additionalProps = new HashMap<>();

    //This is a standalone test no server needed
    @Server
    public static LibertyServer DONOTSTART;

    @BeforeClass
    public static void setUp() throws Exception {
        int javaSpecVersion = Integer.parseInt(System.getProperty("java.specification.version"));
        //To work around the issue described in issue:
        //https://github.com/eclipse-ee4j/jsonb-api/issues/272
        if(javaSpecVersion >= 13) {
            additionalProps.put("java.locale.providers", "COMPAT");
        }
    }

    /**
     * Run the TCK (controlled by autoFVT/publish/tckRunner/tck/*)
     */
    @Test
    @AllowedFFDC // The tested exceptions cause FFDC so we have to allow for this.
    public void launchJsonbTCK() throws Exception {
        Map<String, String> resultInfo = MvnUtils.getResultInfo(DONOTSTART);

        /**
         * The runTCKMvnCmd will set the following properties for use by arquillian
         * [ wlp, tck_server, tck_port, tck_failSafeUndeployment, tck_appDeployTimeout, tck_appUndeployTimeout ]
         * and then run the mvn test command.
         */
        int result = MvnUtils.runTCKMvnCmd(
                                           DONOTSTART, //server to run on
                                           "io.openliberty.jakarta.jsonb.3.0_fat_tck", //bucket name
                                           this.getClass() + ":launchJsonbTCK", //launching method
                                           null, //suite file to run
                                           additionalProps, //additional props
                                           Collections.emptySet() //additional jars
        );

        resultInfo.put("results_type", "Jakarta");
        resultInfo.put("feature_name", "jsonb");
        resultInfo.put("feature_version", "3.0");
        MvnUtils.preparePublicationFile(resultInfo);
        assertEquals(0, result);
    }
}