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
package com.ibm.ws.security.wim.adapter.ldap.fat.krb5;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.images.RemoteDockerImage;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.Ports;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.ExternalDockerClientFilter;
import componenttest.containers.ImageBuilder;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;

public class KerberosContainer extends GenericContainer<KerberosContainer> {

    private static final Class<?> c = KerberosContainer.class;

    public static final String KRB5_REALM = "EXAMPLE.COM";
    public static final String KRB5_KDC_EXTERNAL = "kerberos";
    public static final String KRB5_PWD = "pwd";
    public static String KRB5_PORT = "notSetYet";
    public static String KRB5_HOSTNAME = "notSetYet";
    public static final String KRB5_KDC_INTERNAL = "localhost";

    private static final RemoteDockerImage LDAP_KDC_SERVER = ImageBuilder.build("ldap-kdc-server:1.0.0.1").getFuture();

    private int udp_99;

    public KerberosContainer(Network network) {
        super(LDAP_KDC_SERVER);
        withNetwork(network);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void configure() {
        withExposedPorts(99, 464, 749);
        withNetworkAliases(KRB5_KDC_EXTERNAL);
        withCreateContainerCmdModifier(cmd -> {
            cmd.withHostName(KRB5_KDC_EXTERNAL);
        });

        String hostname = "";
        if (ExternalDockerClientFilter.instance().isValid()) {
            hostname = ExternalDockerClientFilter.instance().getHostname();
            //DOCKERHOST_DOMAIN = hostname.substring(hostname.indexOf('.') + 1);
            //Log.info(c, "configure", "Setting DOCKERHOST_DOMAIN to: " + DOCKERHOST_DOMAIN);
        } else {
            Log.info(c, "configure", "external docker hostname is null, using getHost(ip) instead");
            hostname = getHost();
        }

        KRB5_HOSTNAME = hostname;
        withEnv("EXTERNAL_HOSTNAME", hostname);
        Log.info(c, "start", "===== KDC DOCKER HOST INFO =====");
        Log.info(c, "start", "Using EXTERNAL_HOSTNAME=" + hostname);
        Log.info(c, "start", "Using KRB5_HOSTNAME=" + KRB5_HOSTNAME);
        Log.info(c, "start", "getHost()=" + getHost());
        Log.info(c, "start", "===== KDC DOCKER HOST INFO =====");

        withEnv("KRB5_REALM", KRB5_REALM);
        withEnv("KRB5_KDC", KRB5_KDC_INTERNAL);
        withEnv("KRB5_PASS", KRB5_PWD);

        withLogConsumer(new SimpleLogConsumer(c, "krb5"));
        waitingFor(new LogMessageWaitStrategy()
                        .withRegEx("^.*KERB SETUP COMPLETE.*$")
                        .withStartupTimeout(Duration.ofSeconds(FATRunner.FAT_TEST_LOCALRUN ? 15 : 300)));
        withCreateContainerCmdModifier(cmd -> {

            List<ExposedPort> exposedPorts = new ArrayList<ExposedPort>();
            for (ExposedPort p : cmd.getExposedPorts()) {
                Log.info(c, "configure", "ExposedPort=" + p.getPort());
                exposedPorts.add(p);
            }

            exposedPorts.add(ExposedPort.udp(99));
            cmd.withExposedPorts(exposedPorts);

            // Add previous port bindings and KDC and LDAP ports
            Ports ports = cmd.getPortBindings();
            ports.bind(ExposedPort.udp(99), Ports.Binding.empty());

            //Log.info(c, "configure", "ports: " + ports);
            cmd.withPortBindings(ports);

            cmd.withHostName(KRB5_KDC_EXTERNAL);
        });
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        String udp99 = containerInfo.getNetworkSettings().getPorts().getBindings().get(new ExposedPort(99, InternetProtocol.UDP))[0].getHostPortSpec();
        udp_99 = Integer.valueOf(udp99);
        KRB5_PORT = "" + udp_99;
        Log.info(c, "containerIsStarted", "Using KRB5_PORT=" + KRB5_PORT);
    }

    @Override
    public Integer getMappedPort(int originalPort) {
        // For this container assume we always want the UDP port when we ask for port 99
        if (originalPort == 99) {
            return udp_99;
        } else {
            return super.getMappedPort(originalPort);
        }
    }

    public void generateConf(Path outputPath, boolean optionalKdcPorts) throws IOException {
        String conf = "[libdefaults]\n" +
                      "        rdns = false\n" +
                      "        renew_lifetime = 7d\n" +
                      "        ticket_lifetime = 24h\n" +
                      //"        udp_preference_limit = 1\n" +
                      "        ignore_acceptor_hostname = true\n" +
                      "        dns_lookup_realm = false\n" +
                      "        default_realm = " + KRB5_REALM.toUpperCase() + "\n" +
                      "\n" +
                      "# The following krb5.conf variables are only for MIT Kerberos.\n" +
                      "        kdc_timesync = 1\n" +
                      "        ccache_type = 4\n" +
                      "        forwardable = true\n" +
                      "        proxiable = false\n" +
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
        Log.info(c, "generateConf", "krb5.conf: \n" + conf);
    }
}
