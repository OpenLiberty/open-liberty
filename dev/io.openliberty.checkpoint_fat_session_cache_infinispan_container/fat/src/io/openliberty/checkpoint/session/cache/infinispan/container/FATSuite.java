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
package io.openliberty.checkpoint.session.cache.infinispan.container;

import java.io.File;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.ImageNameSubstitutor;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Variable;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.SimpleLogConsumer;
import componenttest.containers.TestContainerSuite;
import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyFileManager;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                CheckpointSessionCacheOneServerTest.class,
                CheckpointSessionCacheTwoServerTest.class,
                CheckpointSessionCacheTwoServerTimeoutTest.class,
})

public class FATSuite extends TestContainerSuite {

    @ClassRule
    public static RepeatTests repeat;

    static {
        if (JavaInfo.JAVA_VERSION >= 11) {
            repeat = RepeatTests.withoutModificationInFullMode()
                            .andWith(new JakartaEE9Action()
                                            .forServers("com.ibm.ws.session.cache.fat.infinispan.container.checkpointServer",
                                                        "com.ibm.ws.session.cache.fat.infinispan.container.checkpointServerA",
                                                        "com.ibm.ws.session.cache.fat.infinispan.container.checkpointServerB",
                                                        "com.ibm.ws.session.cache.fat.infinispan.container.checkpointTimeoutServerA",
                                                        "com.ibm.ws.session.cache.fat.infinispan.container.checkpointTimeoutServerB")
                                            .fullFATOnly())
                            .andWith(new JakartaEE10Action()
                                            .forServers("com.ibm.ws.session.cache.fat.infinispan.container.checkpointServer",
                                                        "com.ibm.ws.session.cache.fat.infinispan.container.checkpointServerA",
                                                        "com.ibm.ws.session.cache.fat.infinispan.container.checkpointServerB",
                                                        "com.ibm.ws.session.cache.fat.infinispan.container.checkpointTimeoutServerA",
                                                        "com.ibm.ws.session.cache.fat.infinispan.container.checkpointTimeoutServerB"));
        } else {
            repeat = RepeatTests.withoutModificationInFullMode()
                            .andWith(new JakartaEE9Action()
                                            .forServers("com.ibm.ws.session.cache.fat.infinispan.container.checkpointServer",
                                                        "com.ibm.ws.session.cache.fat.infinispan.container.checkpointServerA",
                                                        "com.ibm.ws.session.cache.fat.infinispan.container.checkpointServerB",
                                                        "com.ibm.ws.session.cache.fat.infinispan.container.checkpointTimeoutServerA",
                                                        "com.ibm.ws.session.cache.fat.infinispan.container.checkpointTimeoutServerB"));
        }
    }

    @BeforeClass
    public static void beforeSuite() throws Exception {
        // Delete the Infinispan jars that might have been left around by previous test buckets.
        LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.session.cache.fat.infinispan.container.checkpointServer");
        Machine machine = server.getMachine();
        String installRoot = server.getInstallRoot();
        LibertyFileManager.deleteLibertyDirectoryAndContents(machine, installRoot + "/usr/shared/resources/infinispan");

        if (JakartaEEAction.isEE9OrLaterActive()) {
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
    //TODO switch to use quay.io/infinispan/server:10.0.1.Final
    //TODO remove withDockerfileFromBuilder and instead create a dockerfile
    @ClassRule
    public static GenericContainer<?> infinispan = new GenericContainer<>(new ImageFromDockerfile()
                    .withDockerfileFromBuilder(builder -> builder.from(
                                                                       ImageNameSubstitutor.instance()
                                                                                       .apply(DockerImageName.parse("infinispan/server:10.0.1.Final"))
                                                                                       .asCanonicalNameString())
                                    .user("root")
                                    .copy("/opt/infinispan_config/config.xml", "/opt/infinispan_config/config.xml")
                                    .copy("/opt/infinispan/server/conf/users.properties", "/opt/infinispan/server/conf/users.properties")
                                    .build())
                    .withFileFromFile("/opt/infinispan_config/config.xml", new File("lib/LibertyFATTestFiles/infinispan/config.xml"))
                    .withFileFromFile("/opt/infinispan/server/conf/users.properties", new File("lib/LibertyFATTestFiles/infinispan/users.properties")))
                    .withCommand("./bin/server.sh -c /opt/infinispan_config/config.xml")
                    .withExposedPorts(11222)
                    .waitingFor(new LogMessageWaitStrategy()
                                    .withRegEx(".*ISPN080001: Infinispan Server.*")
                                    .withStartupTimeout(Duration.ofMinutes(FATRunner.FAT_TEST_LOCALRUN ? 5 : 15)))
                    .withLogConsumer(new SimpleLogConsumer(FATSuite.class, "Infinispan"));

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
        Log.info(FATSuite.class, "run", "JAVA_VERSION: " + JavaInfo.JAVA_VERSION);

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

    static void updateVariableConfig(LibertyServer server, String name, String value) throws Exception {
        // change config of variable for restore
        ServerConfiguration config = removeTestKeyVar(server.getServerConfiguration(), name);
        config.getVariables().add(new Variable(name, value));
        server.updateServerConfiguration(config);
    }

    static ServerConfiguration removeTestKeyVar(ServerConfiguration config, String key) {
        for (Iterator<Variable> iVars = config.getVariables().iterator(); iVars.hasNext();) {
            Variable var = iVars.next();
            if (var.getName().equals(key)) {
                iVars.remove();
            }
        }
        return config;
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
