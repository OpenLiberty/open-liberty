/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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
package com.ibm.ws.feature.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class VersionlessPlatformTest extends VersionlessTestBase{

    public static final String SERVER_NAME = "platforms";

    public static final String[] ALLOWED_ERRORS = { "CWWKF0001E", "CWWKF0048E", "CWWKF0033E", "CWWKF0046W", "CWWKF0044E", "CWWKF0047E"};

    public static final String features = "servlet,mpMetrics,mpHealth,jpa,ejb,servlet,mpOpenAPI,websocket,restfulWS,mpJwt,mpFaultTolerance,mpRestClient";

    public static List<String> featureList;

    public static List<String[]> platformCombos;

    @BeforeClass
    public static void setup(){
        featureList = Arrays.asList(features.split(","));
        platformCombos = data();
    }

    public static List<String[]> data(){
        //Get all Javaee/Jakartaee platform versions
        //Get all MicroProfile platform versions
        //Get all combinations of these
        //Run a test with the featuresList + each combination of jakartaee/microprofile

        List<String[]> platformCombos = new ArrayList<String[]>();

        //Eventually we want a way to do this without hardcoding
        String eeVersions = "7.0,8.0,9.0,10.0";
        String[] eeVersionsArray = eeVersions.split(",");

        String mpVersions = "1.0,1.2,1.3,1.4,2.0,2.1,2.2,3.0,3.2,3.3,4.0,4.1,5.0,6.0,6.1";
        String[] mpVersionsArray = mpVersions.split(",");

        for(String eeVersion : eeVersionsArray){
            for(String mpVersion : mpVersionsArray){
                String jakarta = "jakartaee-" + eeVersion;
                String microprofile = "microprofile-" + mpVersion;
                platformCombos.add(new String[] {jakarta, microprofile});
            }
        }

        System.out.println("Running VersionlessPlatformTest with " + platformCombos.size() + " combinations.");

        return platformCombos;
    }

    @Test
    public void VersionlessFeaturesAndPlatformTest() throws Exception {
        LibertyServer server = LibertyServerFactory.getLibertyServer(SERVER_NAME);

        //combo[0] == ee version, combo[1] == mp version
        for(String[] combo : platformCombos){
            List<String> platforms = new ArrayList<>();
            platforms.add(combo[0]);
            platforms.add(combo[1]);
            server.changeFeaturesAndPlatforms(featureList, platforms);

            try{
                test(server, ALLOWED_ERRORS, null, new String[] {}, new String[] {});
            }
            catch (Exception e){
                //we want to get through all combinations, so ignore
            }
            if(server.isStarted()){
                server.stopServer(ALLOWED_ERRORS);
            }
        }
    }

}
