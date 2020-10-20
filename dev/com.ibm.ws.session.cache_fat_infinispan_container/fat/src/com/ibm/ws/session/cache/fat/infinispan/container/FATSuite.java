/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.cache.fat.infinispan.container;

import java.io.File;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.ExternalTestServiceDockerClientStrategy;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(Suite.class)
@SuiteClasses({
                SessionCacheOneServerTest.class,
                SessionCacheTwoServerTest.class,
                SessionCacheTimeoutTest.class,
                SessionCacheTwoServerTimeoutTest.class
})

public class FATSuite {

    @ClassRule
    public static RepeatTests repeat = RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action()
                                    .forServers("com.ibm.ws.session.cache.fat.infinispan.container.server",
                                                "com.ibm.ws.session.cache.fat.infinispan.container.serverA",
                                                "com.ibm.ws.session.cache.fat.infinispan.container.serverB",
                                                "com.ibm.ws.session.cache.fat.infinispan.container.timeoutServerA",
                                                "com.ibm.ws.session.cache.fat.infinispan.container.timeoutServerB"));

    // Used in conjunction with fat.test.use.remote.docker property to use a remote docker host for local testing.
    static {
        ExternalTestServiceDockerClientStrategy.clearTestcontainersConfig();
    }

    @BeforeClass
    public static void beforeSuite() throws Exception {
        // Delete the Infinispan jars that might have been left around by previous test buckets.
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.session.cache.fat.infinispan.container.server");
        Machine machine = server.getMachine();
        String installRoot = server.getInstallRoot();
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/infinispan");

        if (JakartaEE9Action.isActive()) {
            LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/infinispan-jakarta");
            RemoteFile jakartaResourceDir = LibertyFileManager.createRemoteFile(machine, installRoot + "/usr/shared/resources/infinispan-jakarta");
            jakartaResourceDir.mkdirs();
        }
    }

    /**
     * Infinispan container
     * - Copies config.xml for server config
     * - Copies user.properties for authentication (user: user, password: pass)
     * - Starts using a custom command to call server.sh with the copied config.xml
     */
    @ClassRule
    public static GenericContainer<?> infinispan = new GenericContainer<>(new ImageFromDockerfile()
                    .withDockerfileFromBuilder(builder -> builder.from("infinispan/server:10.0.1.Final")
                                    .user("root")
                                    .copy("/opt/infinispan_config/config.xml", "/opt/infinispan_config/config.xml")
                                    .copy("/opt/infinispan/server/conf/users.properties", "/opt/infinispan/server/conf/users.properties")
                                    .build())
                    .withFileFromFile("/opt/infinispan_config/config.xml", new File("lib/LibertyFATTestFiles/infinispan/config.xml"))
                    .withFileFromFile("/opt/infinispan/server/conf/users.properties", new File("lib/LibertyFATTestFiles/infinispan/users.properties")))
                                    .withCommand("./bin/server.sh -c /opt/infinispan_config/config.xml")
                                    .waitingFor(new LogMessageWaitStrategy()
                                                    .withRegEx(".*ISPN080001: Infinispan Server.*")
                                                    .withStartupTimeout(Duration.ofMinutes(FATRunner.FAT_TEST_LOCALRUN ? 5 : 15)))
                                    .withLogConsumer(FATSuite::log);

    /**
     * Custom runner used by test classes.
     *
     * Creates HTTP connection, adds cookie request, makes connection, analyses response, and finally returns response
     *
     * @param server     - The liberty server that is hosting the URL
     * @param path       - The path to the URL with the output to test (excluding port and server information). For instance "/someContextRoot/servlet1"
     * @param testMethod - Method on server to test against
     * @param session    - Session to be persisted.
     * @return String - servletResponse
     * @throws Exception - Thrown when encountering connection issues.
     */
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

    // Logger used by infinispan container
    private static void log(Object frame) {
        String msg = ((OutputFrame) frame).getUtf8String();
        if (msg.endsWith("\n"))
            msg = msg.substring(0, msg.length() - 1);
        Log.info(GenericContainer.class, "infinispan", msg);
    }

}
