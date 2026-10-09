/*******************************************************************************
 * Copyright (c) 2023, 2026 IBM Corporation and others.
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
package com.ibm.ws.test.featurestart.features;

import java.util.HashMap;
import java.util.Map;

public class FeatureErrors {

    /**
     * Tell if a feature should receive an extra delay between starting
     * and stopping the feature.
     * 
     * This is here specifically for logstashCollector, which needs
     * extra time (10 s) between starting and stopping the feature.
     * 
     * This is very specific, and is not error data. However, adding
     * additional classes for this one case is too much.
     * 
     * @param shortFeatureName A feature short name.
     * 
     * @return The delay that is needed between starting and stopping
     *     the feature, in milliseconds. Return zero if the feature
     *     does not need a delay.
     */
    public static long getFeatureDelayMs(String shortFeatureName) {
        if ( shortFeatureName.equals("logstashCollector-1.0") ) {
            return 10000; // wait 10 seconds for logstashCollector
        } else {
            return 0;
        }
    }
    
    /**
     * Answer a table of errors which are required to appear in server logs
     * when starting a server with the single named feature provisioned.
     *
     * @return The table of required errors.
     */
    public static Map<String, String[]> getRequiredErrorsRegEx() {
        Map<String, String[]> requiredErrors = new HashMap<>();

        // "openapi-3.0" and "openapi-3.1" previously left threads
        // when the server was stopped. Those errors no longer occur.
        //
        // Errors still occur when stopping "mpOpenApi-1.0".

        String[] QUIESCE_FAILURES = new String[] { "CWWKE1102W", "CWWKE1107W" };
        requiredErrors.put("mpOpenApi-1.0", QUIESCE_FAILURES);

        requiredErrors.put("batchSMFLogging-1.0", new String[] { "CWWKE0702E: .* com.ibm.ws.jbatch.smflogging" });
        
        requiredErrors.put("zosLocalAdapters-1.0", new String[] { "CWWKE0702E: .* com.ibm.ws.security.thread.zos",
                                                                 "CWWKE0702E: .* com.ibm.ws.webcontainer" });

        requiredErrors.put("zosWlm-1.0", new String[] { "CWWKB0160W" });

        requiredErrors.put("zosAutomaticRestartManager-1.0", new String[] { "CWWKB0758E" });

        // requires binaryLogging-1.0 to be enabled via bootstrap.properties
        requiredErrors.put("logAnalysis-1.0", new String[] { "CWWKE0702E: .* com.ibm.ws.loganalysis" });

        // The Rtcomm service is not able to connect to tcp://localhost:1883.
        requiredErrors.put("rtcomm-1.0", new String[] { "CWRTC0002E" });
        // The Rtcomm service is not able to connect to tcp://localhost:1883.
        // The Rtcomm service - The following virtual hosts could not be found or are not correctly configured: [abcdefg].
        requiredErrors.put("rtcommGateway-1.0", new String[] { "CWRTC0002E", "SRVE9956W" });

        // [10/07/2026 21:27:32:360 UTC] 002 FeaturesStartTest4             processExpected                I
        // Feature failure [ wsSecuritySaml-1.1] (Feature specified (expected)):
        // CWWKS5207W: .* inboundPropagation
        // [10/07/2026 21:27:32:360 UTC] 002 FeaturesStartTest4             processExpected                I
        // Feature failure [ wsSecuritySaml-1.1] (Feature specified (expected)):
        // Unexpected error [ [10/7/26, 21:27:28:514 UTC] 00000021 com.ibm.ws.security.saml.sso20.internal.SsoConfigImpl        W
        // CWWKS5207W: The inboundPropagation attribute is set to [false] in the configuration of samlWebSso20 [defaultSP]. The attributes [headerName, audiences] will be ignored during processing. ]
        
        // lets the user now certain config attributes will be ignored depending on whether or not 'inboundPropagation' is configured
        requiredErrors.put("samlWeb-2.0", new String[] { "CWWKS5207W: .* inboundPropagation" });
        // pulls in the samlWeb-2.0 feature
        requiredErrors.put("wsSecuritySaml-1.1", new String[] { "CWWKS5207W: .* inboundPropagation" });

        // Ignore required config warnings for the 'collectiveMember-1.0' feature, and all features that include it
        String[] COLLECTIVE_MEMBER_WARNINGS = new String[] { "CWWKG0033W: .*collectiveTrust", "CWWKG0033W: .*serverIdentity" };
        requiredErrors.put("collectiveMember-1.0", COLLECTIVE_MEMBER_WARNINGS);
        requiredErrors.put("collectiveController-1.0", COLLECTIVE_MEMBER_WARNINGS);
        requiredErrors.put("clusterMember-1.0", COLLECTIVE_MEMBER_WARNINGS);
        requiredErrors.put("dynamicRouting-1.0", COLLECTIVE_MEMBER_WARNINGS);
        requiredErrors.put("healthAnalyzer-1.0", COLLECTIVE_MEMBER_WARNINGS);
        requiredErrors.put("healthManager-1.0", COLLECTIVE_MEMBER_WARNINGS);
        requiredErrors.put("scalingController-1.0", COLLECTIVE_MEMBER_WARNINGS);
        requiredErrors.put("scalingMember-1.0", COLLECTIVE_MEMBER_WARNINGS);

        return requiredErrors;
    }
}
