/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.rest.client.fat;

import java.util.HashSet;
import java.util.Set;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import componenttest.rules.repeater.FeatureReplacementAction;

@RunWith(Suite.class)
@SuiteClasses({
                AsyncMethodTest.class,
                BasicTest.class,
                BasicCdiTest.class,
                BasicCdiInEE8Test.class,
                BasicEJBTest.class,
                CdiPropsAndProvidersTest.class,
                CollectionsTest.class,
                HandleResponsesTest.class,
                HeaderPropagationTest.class,
                HeaderPropagation12Test.class,
                HostnameVerifierTest.class,
                JsonbContextTest.class,
                MultiClientCdiTest.class,
                ProduceConsumeTest.class,
                PropsTest.class,
                SseTest.class
})
public class FATSuite {
    private static final String[] ALL_VERSIONS = {"1.0", "1.1", "1.2", "1.3", "1.4", "2.0"};

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
        switch(version) {
            case "1.0":
            case "1.1": return use(action, "mpConfig", "1.1", "1.0", "1.2", "1.3", "1.4", "2.0");
            case "1.2":
            case "1.3": return use(action, "mpConfig", "1.3", "1.0", "1.1", "1.2", "1.4", "2.0");
            case "1.4": return use(action, "mpConfig", "1.4", "1.0", "1.1", "1.2", "1.3", "2.0");
            case "2.0":
            default:    return use(action, "mpConfig", "2.0", "1.0", "1.1", "1.2", "1.3", "1.4");
        }
    }

    private static FeatureReplacementAction use(FeatureReplacementAction action, String featureName, String version) {
        return use(action, featureName, version, ALL_VERSIONS);
    }

    private static FeatureReplacementAction use(FeatureReplacementAction action, String featureName, String version, String... versionsToRemove) {
        action = action.addFeature(featureName + "-" + version);
        Set<String> featuresToRemove = new HashSet<>();
        for (String remove : versionsToRemove) {
            if (!version.equals(remove)) {
                featuresToRemove.add(featureName + "-" + remove);
            }
        }
        action = action.removeFeatures(featuresToRemove);
        if (!isLatest(featureName, version)) {
            action.fullFATOnly();
        }
        return action;
    }

    private static boolean isLatest(String featureName, String version) {
        return "mpRestClient".equals(featureName) && "2.0".equals(version);
    }
}
