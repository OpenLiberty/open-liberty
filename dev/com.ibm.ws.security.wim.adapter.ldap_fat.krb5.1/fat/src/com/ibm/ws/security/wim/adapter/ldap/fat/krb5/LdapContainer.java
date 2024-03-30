/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.wim.adapter.ldap.fat.krb5.utils.LdapKerberosUtils;

import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;

public class LdapContainer extends GenericContainer<LdapContainer> {

    private static final Class<?> c = LdapContainer.class;

    public static final String KRB5_REALM = "EXAMPLE.COM";
    public static String KRB5_KDC = "ldap.example.com";
    public static final String KRB5_PASS = "password";
    public static String KDC_HOSTNAME = "notSetYet";

    protected static boolean IS_BEING_USED = false;

    public static String BASE_DN = LdapKerberosUtils.BASE_DN; // default, override in extending class

    /* The Domain needs to be capitalized for Kerberos, but not necessarily for LDAP. */
    public static String DOMAIN = LdapKerberosUtils.DOMAIN; // default, override in extending class

    protected static String bindPassword = LdapKerberosUtils.BIND_PASSWORD; // default, override in extending class

    protected static String bindUserName = LdapKerberosUtils.BIND_USER; // default, override in extending class

    protected static String bindPrincipalName = LdapKerberosUtils.BIND_PRINCIPAL_NAME; // default, override in extending class

    // NOTE: If this is ever updated, don't forget to push to docker hub, but DO NOT overwrite existing versions
    private static final String IMAGE = "zachhein/ldap-krb5-server:0.8";

    private int tcp_88;

    public LdapContainer(Network network) {
        super(IMAGE);
        withNetwork(network);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void configure() {
        withNetworkAliases(KRB5_KDC);
        withCreateContainerCmdModifier(cmd -> {
            cmd.withHostName(KRB5_KDC);
        });
        withEnv("KRB5_REALM", KRB5_REALM);
        Log.info(c, "configure", "setting env KRB5_KDC: " + KRB5_KDC);
        withEnv("KRB5_KDC", KRB5_KDC);
        withEnv("KRB5_PASS", KRB5_PASS);
        withEnv("KDC_HOSTNAME", KDC_HOSTNAME);
        Log.info(c, "configure", "setting env KDC_HOSTNAME: " + KDC_HOSTNAME);

        withLogConsumer(new SimpleLogConsumer(c, "krb5"));
        waitingFor(new LogMessageWaitStrategy()
                        .withRegEx("^.*stop the ldap.*$")
                        .withStartupTimeout(Duration.ofSeconds(FATRunner.FAT_TEST_LOCALRUN ? 30 : 300)));
        withCreateContainerCmdModifier(cmd -> {
            //Add previously exposed ports and UDP port

            List<ExposedPort> exposedPorts = new ArrayList<ExposedPort>();
            for (ExposedPort p : cmd.getExposedPorts()) {
                Log.info(c, "configure", "ExposedPort=" + p.getPort());
                exposedPorts.add(p);
            }
            exposedPorts.add(ExposedPort.tcp(88));
            exposedPorts.add(ExposedPort.tcp(389));
            cmd.withExposedPorts(exposedPorts);

            // Add previous port bindings and KDC and LDAP ports
            Ports ports = cmd.getPortBindings();
            int containerPort = 88;
            int hostPort = 88;

            String kdcPortMapping = String.format("%d:%d/%s", hostPort, containerPort, InternetProtocol.TCP);
            Log.info(c, "configure", "adding KDC port mapping: " + kdcPortMapping);

            Log.info(c, "configure", "PortBinding.parse(kdcPortMapping): " + PortBinding.parse(kdcPortMapping));
            ports.add(PortBinding.parse(kdcPortMapping));

            String ldapPortMapping = String.format("%d:%d/%s", 389, 389, InternetProtocol.TCP);
            Log.info(c, "configure", "adding ldap port mapping: " + ldapPortMapping);

            Log.info(c, "configure", "PortBinding.parse(ldapPortMapping): " + PortBinding.parse(ldapPortMapping));
            ports.add(PortBinding.parse(ldapPortMapping));

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
    public void start() {
        String dockerHostIp = DockerClientFactory.instance().dockerHostIpAddress();
        withEnv("EXTERNAL_HOSTNAME", dockerHostIp);
        Log.info(c, "start", "Using EXTERNAL_HOSTNAME=" + dockerHostIp);
        Log.info(c, "start", "getHost()=" + getHost());
        KDC_HOSTNAME = dockerHostIp;
        KRB5_KDC = KDC_HOSTNAME;
        super.start();
    }

    @Override
    public Integer getMappedPort(int originalPort) {
        // For this container assume we always want the UDP port when we ask for port 88
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
                "        ." + KRB5_REALM.toLowerCase() + " = " + KRB5_REALM.toUpperCase() + "\n" +
                "        " + KRB5_REALM.toLowerCase() + " = " + KRB5_REALM.toUpperCase() + "\n";
        outputPath.getParent().toFile().mkdirs();
        Files.write(outputPath, conf.getBytes(StandardCharsets.UTF_8));
        Log.info(c, "generateConf", "krb5.conf: \n" + conf);
    }
}
