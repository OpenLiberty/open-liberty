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
                PropsTest.class
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
        else if ("1.2".equals(version) || "1.3".equals(version)) {
            return use(action, "mpConfig", "1.3", "1.1", "1.4");
        }
        else {
            return use(action, "mpConfig", "1.4", "1.1", "1.3");
        }
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
