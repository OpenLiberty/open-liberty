/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
     * Answer a table of errors which are allowed to appear in server logs
     * when starting a server with the single named feature provisioned.
     *
     * @return The table of allowed errors.
     */
    public static Map<String, String[]> getAllowedErrors() {
        Map<String, String[]> allowedErrors = new HashMap<>();

        // TODO: OpenAPI code needs to be reworked so that it
        // doesn't leave threads around when the server stops
        // before it has finished initializing. Once OpenAPI is
        // fixed, these QUISCE_FAILURES should be removed.
        // TODO: This might be fixed by now.
        String[] QUIESCE_FAILURES = new String[] { "CWWKE1102W", "CWWKE1107W" };
        allowedErrors.put("openapi-3.0", QUIESCE_FAILURES);
        allowedErrors.put("openapi-3.1", QUIESCE_FAILURES);
        allowedErrors.put("mpOpenApi-1.0", QUIESCE_FAILURES);

        allowedErrors.put("batchSMFLogging-1.0", new String[] { "CWWKE0702E: .* com.ibm.ws.jbatch.smflogging" });
        
        allowedErrors.put("zosAutomaticRestartManager-1.0", new String[] { "CWWKB0758E" });

        // requires binaryLogging-1.0 to be enabled via bootstrap.properties
        allowedErrors.put("logAnalysis-1.0", new String[] { "CWWKE0702E: .* com.ibm.ws.loganalysis" });

        // The Rtcomm service is not able to connect to tcp://localhost:1883.
        allowedErrors.put("rtcomm-1.0", new String[] { "CWRTC0002E" });
        // The Rtcomm service is not able to connect to tcp://localhost:1883.
        // The Rtcomm service - The following virtual hosts could not be found or are not correctly configured: [abcdefg].
        allowedErrors.put("rtcommGateway-1.0", new String[] { "CWRTC0002E", "SRVE9956W" });

        // lets the user now certain config attributes will be ignored depending on whether or not 'inboundPropagation' is configured
        allowedErrors.put("samlWeb-2.0", new String[] { "CWWKS5207W: .* inboundPropagation" });
        // pulls in the samlWeb-2.0 feature
        allowedErrors.put("wsSecuritySaml-1.1", new String[] { "CWWKS5207W: .* inboundPropagation" });

        // Ignore required config warnings for the 'collectiveMember-1.0' feature, and all features that include it
        String[] COLLECTIVE_MEMBER_WARNINGS = new String[] { "CWWKG0033W: .*collectiveTrust", "CWWKG0033W: .*serverIdentity" };
        allowedErrors.put("collectiveMember-1.0", COLLECTIVE_MEMBER_WARNINGS);
        allowedErrors.put("collectiveController-1.0", COLLECTIVE_MEMBER_WARNINGS);
        allowedErrors.put("clusterMember-1.0", COLLECTIVE_MEMBER_WARNINGS);
        allowedErrors.put("dynamicRouting-1.0", COLLECTIVE_MEMBER_WARNINGS);
        allowedErrors.put("healthAnalyzer-1.0", COLLECTIVE_MEMBER_WARNINGS);
        allowedErrors.put("healthManager-1.0", COLLECTIVE_MEMBER_WARNINGS);
        allowedErrors.put("scalingController-1.0", COLLECTIVE_MEMBER_WARNINGS);
        allowedErrors.put("scalingMember-1.0", COLLECTIVE_MEMBER_WARNINGS);

        return allowedErrors;
    }
}
