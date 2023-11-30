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

import componenttest.topology.impl.JavaInfo;

public class FeatureFilter {

    // TODO: Is this the correct implementation of this test?
    // How is the test specific to the java which is running the test
    // server?  The test seems to be specific to the java running the
    // FAT.

    public static final String HEALTH_CENTER_CLASS_NAME = "com.ibm.java.diagnostics.healthcenter.agent.mbean.HealthCenter";

    public static final boolean haveHealthCenter = JavaInfo.isSystemClassAvailable(HEALTH_CENTER_CLASS_NAME);

    public static boolean isHealthCenterAvailable() {
        return haveHealthCenter;
    }

    /**
     * Tell if a feature cannot be started by itself. Answer null if the
     * feature can be started. Answer a non-null reason if the feature cannot
     * be started.
     *
     * Irrespective of the value returned by this method, other tests may cause
     * a feature to be skipped.
     *
     * @param shortName The short name of the feature which is to be tested.
     * @return Null if the feature is to be started. A descriptive message if
     *         the feature is not to be started.
     */
    public static String skipFeature(String shortName) {
        if (shortName.equalsIgnoreCase("wsSecurity-1.1")) {
            // This feature is grand-fathered in on not starting cleanly on its own.
            // Fixing it could potentially break existing configurations
            return "Cannot start by itself";

        } else if (shortName.equalsIgnoreCase("constrainedDelegation-1.0")) {
            return "Requires spnego-1.0 or OIDC";

        } else if (shortName.equalsIgnoreCase("logstashCollector-1.0")) {
            // Only IBM JDK includes Health Center and IBM JDK 11+ (Semeru)
            // is based on Adopt JDK 11+, which does not include Health Center.
            if (!isHealthCenterAvailable()) {
                return "Requires Health Center";
            } else {
                return null;
            }

        } else if (shortName.equalsIgnoreCase("wmqMessagingClient-3.0") ||
                   shortName.equalsIgnoreCase("wmqJmsClient-2.0") ||
                   shortName.equalsIgnoreCase("wmqJmsClient-1.1")) {
            // WMQ features require a RAR location variable to be set.
            // These simple tests do not have a RAR and have not set the
            // location variable.
            return "Required variable 'wmqJmsClient.rar.location' is not set";

        } else if (shortName.equalsIgnoreCase("mpHealth") ||
                   shortName.equalsIgnoreCase("mpMetrics")){
            // Versionless feature cannot run by itself
            // Requires other features to be configured for it to resolve
            return "Cannot start by itself";

        } else {
            return null;
        }
    }

    /**
     * Tell if a feature is not to be run depending on whether the test environment
     * is ZOS.
     *
     * Irrespective of the value returned by this method, other tests may cause
     * a test to be skipped.
     *
     * @param shortName The short name of the feature which is to be tested.
     * @param isZOS     True or false telling if the test environment is ZOS.
     * @return Null if the feature is to be started. A descriptive message if
     *         the feature is not to be started.
     */
    public static String zosSkip(String shortName, boolean isZOS) {
        if (!isZOS) {
            if (((shortName.startsWith("zos") && !shortName.startsWith("zosConnect-")) ||
                 shortName.equalsIgnoreCase("batchSMFLogging-1.0"))) {
                return "z/OS only";
            } else {
                return null;
            }
        } else {
            if (shortName.equalsIgnoreCase("logstashCollector-1.0")) {
                return "Requires the attach API, which is disabled on z/OS";
            } else {
                return null;
            }
        }
    }

    /**
     * Tell if a feature is a client-only feature.  Currently,
     * features which have names which contain 'eeClient' or 'securityClient'
     * are client-only features.  Those are 'jakartaeeClient' and
     * 'securityClient'.
     *
     * Previously, the test was against 'client', but that was incorrect.
     *
     * @param shortName The short name of the feature which is to be tested.
     * @return True or false telling if the feature is a client-only feature.
     */
    public static boolean isClient(String shortName) {
        String upperShortName = shortName.toUpperCase();
        return ( upperShortName.contains("EECLIENT") ||
                 upperShortName.contains("SECURITYCLIENT") );
    }
}

