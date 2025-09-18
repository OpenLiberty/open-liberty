/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
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
package com.ibm.ws.security.spnego.fat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.images.RemoteDockerImage;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.Ports;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.ImageBuilder;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;

public class KerberosContainer extends GenericContainer<KerberosContainer> {

    private static final Class<?> c = KerberosContainer.class;

    public static final String KRB5_REALM = "EXAMPLE.COM";
    public static final String KRB5_KDC = "kerberos";
    public static final String KRB5_PASS = "password";
    public static String KDC_HOSTNAME = "notSetYet";

    private static final RemoteDockerImage SPNEGO_KDC_SERVER = ImageBuilder.build("spnego-kdc-server:1.0.0.1").getFuture();

    private int udp_99;

    public KerberosContainer(Network network) {
        super(SPNEGO_KDC_SERVER);
        withNetwork(network);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void configure() {
        withExposedPorts(99, 464, 749);
        withNetworkAliases(KRB5_KDC);
        withCreateContainerCmdModifier(cmd -> {
            cmd.withHostName(KRB5_KDC);
        });
        withEnv("KRB5_REALM", KRB5_REALM);
        withEnv("KRB5_KDC", "localhost");
        withEnv("KRB5_PASS", KRB5_PASS);

        withLogConsumer(new SimpleLogConsumer(c, "krb5"));
        waitingFor(new LogMessageWaitStrategy()
                        .withRegEx("^.*KERB SETUP COMPLETE.*$")
                        .withStartupTimeout(Duration.ofSeconds(FATRunner.FAT_TEST_LOCALRUN ? 15 : 300)));
        withCreateContainerCmdModifier(cmd -> {
            //Add previously exposed ports and UDP port

            List<ExposedPort> exposedPorts = new ArrayList<ExposedPort>();
            for (ExposedPort p : cmd.getExposedPorts()) {
                Log.info(c, "configure", "ExposedPort=" + p.getPort());
                exposedPorts.add(p);
            }
            exposedPorts.add(ExposedPort.udp(99));
            cmd.withExposedPorts(exposedPorts);

            // Add previous port bindings and UDP port binding
            Ports ports = cmd.getPortBindings();
            ports.bind(ExposedPort.udp(99), Ports.Binding.empty());
            /*
             * uncomment to hardcode port to 88
             * Ports ports = cmd.getPortBindings();
             * int containerPort = 99;
             * int hostPort = 88;
             *
             * String kdcPortMapping = String.format("%d:%d/%s", hostPort, containerPort, InternetProtocol.UDP);
             * Log.info(c, "configure", "adding KDC port mapping: " + kdcPortMapping);
             *
             * Log.info(c, "configure", "PortBinding.parse(kdcPortMapping): " + PortBinding.parse(kdcPortMapping));
             * ports.add(PortBinding.parse(kdcPortMapping));
             */
            Log.info(c, "configure", "ports: " + ports);
            cmd.withPortBindings(ports);
            cmd.withHostName(KRB5_KDC);
        });
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        String udp99 = containerInfo.getNetworkSettings().getPorts().getBindings().get(new ExposedPort(99, InternetProtocol.UDP))[0].getHostPortSpec();
        udp_99 = Integer.valueOf(udp99);
    }

    @Override
    public void start() {
        String dockerHostIp = DockerClientFactory.instance().dockerHostIpAddress();
        withEnv("EXTERNAL_HOSTNAME", dockerHostIp);
        Log.info(c, "start", "Using EXTERNAL_HOSTNAME=" + dockerHostIp);
        Log.info(c, "start", "getHost()=" + getHost());
        KDC_HOSTNAME = dockerHostIp;
        super.start();
    }

    @Override
    public Integer getMappedPort(int originalPort) {
        // For this container assume we always want the UDP port when we ask for port 88
        if (originalPort == 99) {
            return udp_99;
        } else {
            return super.getMappedPort(originalPort);
        }
    }

    public void generateConf(Path outputPath) throws IOException {
        String conf = "[libdefaults]\n" +
                      "        rdns = false\n" +
                      "        renew_lifetime = 7d\n" +
                      "        ticket_lifetime = 24h\n" +
                      "        dns_lookup_realm = false\n" +
                      "        default_realm = " + KRB5_REALM.toUpperCase() + "\n" +
                      "\n" +
                      "# The following krb5.conf variables are only for MIT Kerberos.\n" +
                      "        kdc_timesync = 1\n" +
                      "        ccache_type = 4\n" +
                      "        forwardable = true\n" +
                      "        proxiable = true\n" +
                      "\n" +
                      "# The following libdefaults parameters are only for Heimdal Kerberos.\n" +
                      "        fcc-mit-ticketflags = true\n" +
                      "\n" +
                      "[realms]\n" +
                      "        " + KRB5_REALM.toUpperCase() + " = {\n" +
                      "                kdc = " + getHost() + ":" + getMappedPort(99) + "\n" +
                      "                admin_server = " + getHost() + "\n" +
                      "        }\n" +
                      "\n" +
                      "[domain_realm]\n" +
                      "        ." + KRB5_REALM.toLowerCase() + " = " + KRB5_REALM.toUpperCase() + "\n" +
                      "        " + KRB5_REALM.toLowerCase() + " = " + KRB5_REALM.toUpperCase() + "\n";
        outputPath.getParent().toFile().mkdirs();
        Files.write(outputPath, conf.getBytes(StandardCharsets.UTF_8));
    }

    public void generateJAASConf(Path outputPath) throws IOException {
        String conf = "ibmKrb5Login {\n" +
                      "        com.ibm.security.auth.module.Krb5LoginModule required \n" +
                      "        debug=\"true\";\n" +
                      " };\n" +
                      "" +
                      "sunKrb5Login {\n" +
                      "        com.sun.security.auth.module.Krb5LoginModule required \n" +
                      "        debug=\"true\";\n" +
                      " };\n" +
                      "" +
                      "sunKrb5LoginRefreshKrb5Config {\n" +
                      "        com.sun.security.auth.module.Krb5LoginModule required \n" +
                      "        debug=\"true\"  refreshKrb5Config=\"true\";\n" +
                      " };\n";
        outputPath.getParent().toFile().mkdirs();
        Files.write(outputPath, conf.getBytes(StandardCharsets.UTF_8));
    }
}
