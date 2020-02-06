/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.rest.client.fat;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import componenttest.rules.repeater.FeatureReplacementAction;

@RunWith(Suite.class)
@SuiteClasses({
                RetryTest.class,
                TimeoutTest.class
})
public class FATSuite {
    private static final String[] ALL_VERSIONS = {"1.0", "1.1", "1.2", "1.3", "1.4"};

    static FeatureReplacementAction MP_REST_CLIENT(String version, String serverName) {
        return MP_REST_CLIENT(new FeatureReplacementAction(), version, serverName);
    }

    static FeatureReplacementAction MP_REST_CLIENT(FeatureReplacementAction action, String version, String serverName) {
        return use(action, "mpRestClient", version)
                        .withID("mpRestClient-" + version)
                        .forServers(serverName);
    }

    static FeatureReplacementAction MP_REST_CLIENT_WITH_CONFIG(String version, String serverName) {
        return MP_REST_CLIENT_WITH_CONFIG(new FeatureReplacementAction(), version, serverName);
    }

    static FeatureReplacementAction MP_REST_CLIENT_WITH_CONFIG(FeatureReplacementAction action, String version, String serverName) {
        action = use(action, "mpRestClient", version)
                        .withID("mpRestClient-" + version)
                        .forServers(serverName);
        if ("1.0".equals(version) || "1.1".equals(version)) {
            return use(action, "mpConfig", "1.1", "1.3", "1.4");
        }
        return use(action, "mpConfig", "1.3", "1.1");
    }

    private static FeatureReplacementAction use(FeatureReplacementAction action, String featureName, String version) {
        return use(action, featureName, version, ALL_VERSIONS);
    }

    private static FeatureReplacementAction use(FeatureReplacementAction action, String featureName, String version, String... versionsToRemove) {
        String feature = featureName + "-" + version;
        action = action.addFeature(featureName + "-" + version);
        for (String remove : versionsToRemove) {
            if (!version.equals(remove)) {
                action = action.removeFeature(featureName + "-" + remove);
            }
        }
        return action;
    }
}
