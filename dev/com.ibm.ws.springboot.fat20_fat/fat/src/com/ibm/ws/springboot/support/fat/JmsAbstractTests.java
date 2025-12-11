/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package com.ibm.ws.springboot.support.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.MountableFile;

import com.ibm.websphere.simplicity.config.IncludeElement;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.SkipIfSysProp;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

@RunWith(FATRunner.class)
@SkipIfSysProp(SkipIfSysProp.OS_ZOS)
public abstract class JmsAbstractTests extends AbstractSpringTests {

    private static final String mqVersion = "9.3.2.0-r2";
    private static final int MQ_LISTENER_PORT = 1414;

    @SuppressWarnings("resource")
    @ClassRule
    public static GenericContainer<?> container = new GenericContainer<>("icr.io/ibm-messaging/mq:" + mqVersion)
                    .withExposedPorts(MQ_LISTENER_PORT)
                    .withEnv("MQ_DEV", "true")
                    .withEnv("LICENSE", "accept")
                    .withEnv("MQ_QMGR_NAME", "QM1")
                    .withEnv("MQ_ADMIN_PASSWORD", "passw0rd")
                    .withCopyFileToContainer(MountableFile.forHostPath(Paths.get("lib/LibertyFATTestFiles/mqconfig.mqsc")), "/etc/mqm/mqconfig.mqsc")
                    .withLogConsumer(new SimpleLogConsumer(JmsAbstractTests.class, "mq-init"))
                    .waitingFor(new LogMessageWaitStrategy()
                                    .withRegEx(".*AMQ5026I.*")
                                    .withStartupTimeout(Duration.ofMinutes(FATRunner.FAT_TEST_LOCALRUN ? 3 : 10)));

    @BeforeClass
    public static void setupJms() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        IncludeElement includeJms = new IncludeElement();
        includeJms.setLocation("${server.config.dir}/includeJms.xml");
        config.getIncludes().add(includeJms);
        server.updateServerConfiguration(config);
        setupHostAndPort();
    }

    public static void setupHostAndPort() throws Exception {
        server.setHttpDefaultPort(DEFAULT_HTTP_PORT);
        String mq_host = container.getHost();
        String mq_port = String.valueOf(container.getMappedPort(1414));
        Map<String, String> vars = new HashMap<>();
        vars.put("MQ_HOST", mq_host);
        vars.put("MQ_PORT", mq_port);
        configureEnvVariable(server, vars);
    }

    @Override
    public String getApplication() {
        return SPRING_BOOT_20_APP_JMS;
    }

    @Override
    public boolean useDefaultVirtualHost() {
        return true;
    }

    public String getContextRoot() {
        return "/";
    }

    protected void testJmsWithTransaction() throws Exception {
        HttpUtils.findStringInUrl(server, getContextRoot() + "book?name=Alice&failTransaction=false", "Booking Successful");
        assertNotNull("Did not find message printed by JMS Listener", server.waitForStringInLog("Received message: Booking Confirmed for Alice"));
        HttpUtils.findStringInUrl(server, getContextRoot() + "totalBookings1", "1");
        HttpUtils.findStringInUrl(server, getContextRoot() + "totalBookings2", "1");

        URL url = new URL("http://localhost:" + server.getHttpDefaultPort() + getContextRoot() + "book?name=Bob&failTransaction=true");
        int responseCode = HttpUtils.getHttpConnection(url, 5000, HTTPRequestMethod.GET).getResponseCode();
        assertEquals("Expected response code not found", 500, responseCode);
        assertNull("Message should not be printed by JMS Listener", server.waitForStringInLog("Received message: Booking Confirmed for Bob"));
        HttpUtils.findStringInUrl(server, getContextRoot() + "totalBookings1", "1");
        HttpUtils.findStringInUrl(server, getContextRoot() + "totalBookings2", "1");

        HttpUtils.findStringInUrl(server, getContextRoot() + "book?name=Carol&failTransaction=false", "Booking Successful");
        assertNotNull("Did not find message printed by JMS Listener", server.waitForStringInLog("Received message: Booking Confirmed for Carol"));
        HttpUtils.findStringInUrl(server, getContextRoot() + "totalBookings1", "2");
        HttpUtils.findStringInUrl(server, getContextRoot() + "totalBookings2", "2");
    }

    @AfterClass
    public static void stopServerWithException() throws Exception {
        server.stopServer("SRVE0777E", "J2CA0046E");
    }
}
