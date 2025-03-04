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
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.containers.ExternalDockerClientFilter;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;

public class KerberosContainer extends GenericContainer<KerberosContainer> {

    private static final Class<?> c = KerberosContainer.class;

    public static final String KRB5_REALM = "EXAMPLE.COM";
    public static final String KRB5_KDC = "kerberos";
    public static final String KRB5_PWD = "pwd";
    public static String DOCKERHOST_DOMAIN = "fyre.ibm.com";

    //TODO Start using ImageBuilder
//    private static final DockerImageName KRB5_SERVER = ImageBuilder.build("kdc-security-server:3.17").getDockerImageName();

    // NOTE: If this is ever updated, don't forget to push to docker hub, but DO NOT overwrite existing versions
    private static final DockerImageName KRB5_SERVER = DockerImageName.parse("zachhein/krb5-server:0.2");

    private int tcp_88;

    public KerberosContainer(Network network) {
        super(KRB5_SERVER);
        withNetwork(network);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void configure() {
        //withExposedPorts(99, 464, 749);
        withNetworkAliases(KRB5_KDC);
        withCreateContainerCmdModifier(cmd -> {
            cmd.withHostName(KRB5_KDC);
        });

        String hostname = "";
        if (ExternalDockerClientFilter.instance().isValid()) {
            hostname = ExternalDockerClientFilter.instance().getHostname();
            DOCKERHOST_DOMAIN = hostname.substring(hostname.indexOf('.') + 1);
            Log.info(c, "configure", "Setting DOCKERHOST_DOMAIN to: " + DOCKERHOST_DOMAIN);
        } else {
            Log.info(c, "configure", "external docker hostname is null, using getHost(ip) instead");
            hostname = getHost();
        }

        withEnv("EXTERNAL_HOSTNAME", hostname);
        Log.info(c, "configure", "Using EXTERNAL_HOSTNAME=" + hostname);

        withEnv("KRB5_REALM", KRB5_REALM);
        withEnv("KRB5_KDC", "localhost");
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
            exposedPorts.add(ExposedPort.tcp(88));
            cmd.withExposedPorts(exposedPorts);

            // Add previous port bindings and KDC and LDAP ports
            Ports ports = cmd.getPortBindings();
            int containerPort = 88;
            int hostPort = 88;

            String kdcPortMapping = String.format("%d:%d/%s", hostPort, containerPort, InternetProtocol.TCP);
            Log.info(c, "configure", "adding KDC port mapping: " + kdcPortMapping);

            Log.info(c, "configure", "PortBinding.parse(kdcPortMapping): " + PortBinding.parse(kdcPortMapping));
            ports.add(PortBinding.parse(kdcPortMapping));

            Log.info(c, "configure", "ports: " + ports);
            cmd.withPortBindings(ports);

            cmd.withHostName(KRB5_KDC);
        });
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        String udp88 = containerInfo.getNetworkSettings().getPorts().getBindings().get(new ExposedPort(88, InternetProtocol.TCP))[0].getHostPortSpec();
        tcp_88 = Integer.valueOf(udp88);
    }

    @Override
    public Integer getMappedPort(int originalPort) {
        // For this container assume we always want the UDP port when we ask for port 99
        if (originalPort == 88) {
            return tcp_88;
        } else {
            return super.getMappedPort(originalPort);
        }
    }

    public void generateConf(Path outputPath, boolean optionalKdcPorts) throws IOException {
        String conf = "[libdefaults]\n" +
                      "        rdns = false\n" +
                      "        renew_lifetime = 7d\n" +
                      "        ticket_lifetime = 24h\n" +
                      "        udp_preference_limit = 1\n" +
                      "        ignore_acceptor_hostname = true\n" +
                      "        dns_lookup_realm = false\n";
        if (optionalKdcPorts)
            conf += "        kdc_ports = " + getMappedPort(88) + "\n";

        conf += "        default_realm = " + KRB5_REALM.toUpperCase() + "\n" +
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
                "                kdc = " + getHost() + ":" + getMappedPort(88) + "\n" +
                "                admin_server = " + getHost() + "\n" +
                "        }\n" +
                "\n" +
                "[domain_realm]\n" +
                "        ." + DOCKERHOST_DOMAIN + " = EXAMPLE.COM \n" +
                "        " + DOCKERHOST_DOMAIN + " = EXAMPLE.COM \n" +
                "        .fyre.ibm.com = EXAMPLE.COM \n" +
                "        fyre.ibm.com = EXAMPLE.COM \n" +
                "        ." + KRB5_REALM.toLowerCase() + " = " + KRB5_REALM.toUpperCase() + "\n" +
                "        " + KRB5_REALM.toLowerCase() + " = " + KRB5_REALM.toUpperCase() + "\n";
        outputPath.getParent().toFile().mkdirs();
        Files.write(outputPath, conf.getBytes(StandardCharsets.UTF_8));
        Log.info(c, "generateConf", "krb5.conf: \n" + conf);
    }
}
