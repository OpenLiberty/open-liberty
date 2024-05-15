/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
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
package io.openliberty.checkpoint.session.cache.infinispan;

import java.io.File;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                CheckpointSessionCacheOneServerTest.class,
                CheckpointSessionCacheTwoServerTest.class,
                CheckpointSessionCacheTwoServerTimeoutTest.class
})

public class FATSuite {

    public static final String CACHE_MANAGER_EE9_ID = JakartaEE9Action.ID + "_CacheManager";
    public static final String CACHE_MANAGER_EE10_ID = JakartaEE10Action.ID + "_CacheManager";

    public static String run(LibertyServer server, String path, String testMethod, List<String> session) throws Exception {
        HttpURLConnection con = HttpUtils.getHttpConnection(server, path + '?' + FATServletClient.TEST_METHOD + '=' + testMethod);
        Log.info(FATSuite.class, "run", "HTTP GET: " + con.getURL());

        if (session != null)
            for (String cookie : session)
                con.addRequestProperty("Cookie", cookie);

        con.connect();
        try {
            String servletResponse = HttpUtils.readConnection(con);

            if (servletResponse == null || !servletResponse.contains(FATServletClient.SUCCESS))
                Assert.fail("Servlet call was not successful: " + servletResponse);

            if (session != null) {
                List<String> setCookies = con.getHeaderFields().get("Set-Cookie");
                if (setCookies != null) {
                    session.clear();
                    for (String setCookie : setCookies)
                        session.add(setCookie.split(";", 2)[0]);
                }
            }

            return servletResponse;
        } finally {
            con.disconnect();
        }
    }

    public static FeatureReplacementAction checkpointRepeatActionEE9(FeatureReplacementAction action, String ID, String[] servers) {
        return action.removeFeature("mpMetrics-5.1")
                        .addFeature("mpMetrics-4.0")
                        .withID(ID)
                        .forServers(servers);
    }

    public static FeatureReplacementAction checkpointRepeatActionEE10(FeatureReplacementAction action, String ID, String[] servers) {
        return action.removeFeature("mpMetrics-4.0")
                        .addFeature("mpMetrics-5.1")
                        .withID(ID)
                        .forServers(servers);
    }

    static void configureEnvVariable(LibertyServer server, Map<String, String> newEnv) throws Exception {
        File serverEnvFile = new File(server.getFileFromLibertyServerRoot("server.env").getAbsolutePath());
        try (PrintWriter out = new PrintWriter(serverEnvFile)) {
            for (Map.Entry<String, String> entry : newEnv.entrySet()) {
                out.println(entry.getKey() + "=" + entry.getValue());
            }
        }
    }

}
