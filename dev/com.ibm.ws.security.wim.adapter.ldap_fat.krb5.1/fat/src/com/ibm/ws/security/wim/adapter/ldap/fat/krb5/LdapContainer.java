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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.wim.adapter.ldap.fat.krb5.utils.LdapKerberosUtils;

import componenttest.containers.ExternalDockerClientFilter;
import componenttest.containers.SimpleLogConsumer;
import componenttest.custom.junit.runner.FATRunner;

public class LdapContainer extends GenericContainer<LdapContainer> {

    private static final Class<?> c = LdapContainer.class;

    public static final String KRB5_REALM = "EXAMPLE.COM";
    public static String LDAP_HOSTNAME = "notsetyet";
    public static String KDC_HOSTNAME = "kerberos";
    public static String EXTERNAL_HOSTNAME = "notsetyet";
    public static String DOCKERHOST_DOMAIN = "fyre.ibm.com";

    public static String BASE_DN = LdapKerberosUtils.BASE_DN; // default, override in extending class

    protected static String bindPassword = LdapKerberosUtils.BIND_PASSWORD; // default, override in extending class

    protected static String bindUserName = LdapKerberosUtils.BIND_USER; // default, override in extending class

    protected static String bindPrincipalName = LdapKerberosUtils.BIND_PRINCIPAL_NAME; // default, override in extending class

    //TODO Start using ImageBuilder
//    private static final DockerImageName LDAP_SERVER = ImageBuilder.build("ldap-server:3.17").getDockerImageName();

    // NOTE: If this is ever updated, don't forget to push to docker hub, but DO NOT overwrite existing versions
    private static final DockerImageName LDAP_SERVER = DockerImageName.parse("zachhein/ldap-server:0.5");

    private int tcp_389;

    public LdapContainer(Network network) {
        super(LDAP_SERVER);
        withNetwork(network);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void configure() {
        String method = "configure";

        if (ExternalDockerClientFilter.instance().isValid()) {
            EXTERNAL_HOSTNAME = ExternalDockerClientFilter.instance().getHostname();
            DOCKERHOST_DOMAIN = EXTERNAL_HOSTNAME.substring(EXTERNAL_HOSTNAME.indexOf('.') + 1);
            Log.info(c, "configure", "Setting DOCKERHOST_DOMAIN to: " + DOCKERHOST_DOMAIN);
        } else {
            Log.info(c, "configure", "external docker hostname is null, using getHost(ip) instead");
            EXTERNAL_HOSTNAME = getHost();
        }
        withEnv("DOCKERHOST_DOMAIN", DOCKERHOST_DOMAIN);
        withEnv("EXTERNAL_HOSTNAME", EXTERNAL_HOSTNAME);
        Log.info(c, method, "Using EXTERNAL_HOSTNAME=" + EXTERNAL_HOSTNAME);

        LDAP_HOSTNAME = EXTERNAL_HOSTNAME;
        Log.info(c, method, "Using LDAP_HOSTNAME=" + LDAP_HOSTNAME);

        withNetworkAliases(LDAP_HOSTNAME);
        withCreateContainerCmdModifier(cmd -> {
            cmd.withHostName(LDAP_HOSTNAME);
        });
        withEnv("KRB5_REALM", KRB5_REALM);
        withEnv("KDC_HOSTNAME", KDC_HOSTNAME);
        Log.info(c, method, "setting env KDC_HOSTNAME: " + KDC_HOSTNAME);

        withLogConsumer(new SimpleLogConsumer(c, "ldap"));
        waitingFor(new LogMessageWaitStrategy()
                        .withRegEx("^.*LDAP SERVER SETUP COMPLETE.*$")
                        .withStartupTimeout(Duration.ofSeconds(FATRunner.FAT_TEST_LOCALRUN ? 50 : 300)));
        withCreateContainerCmdModifier(cmd -> {
            //Add previously exposed ports and UDP port

            List<ExposedPort> exposedPorts = new ArrayList<ExposedPort>();
            for (ExposedPort p : cmd.getExposedPorts()) {
                Log.info(c, method, "ExposedPort=" + p.getPort());
                exposedPorts.add(p);
            }
            exposedPorts.add(ExposedPort.tcp(389));
            cmd.withExposedPorts(exposedPorts);

            // Add previous port bindings and KDC and LDAP ports
            Ports ports = cmd.getPortBindings();

            String ldapPortMapping = String.format("%d:%d/%s", 389, 389, InternetProtocol.TCP);
            Log.info(c, method, "adding ldap port mapping: " + ldapPortMapping);

            Log.info(c, method, "PortBinding.parse(ldapPortMapping): " + PortBinding.parse(ldapPortMapping));
            ports.add(PortBinding.parse(ldapPortMapping));

            Log.info(c, method, "ports: " + ports);
            cmd.withPortBindings(ports);
            cmd.withHostName(LDAP_HOSTNAME);
        });
    }

    @Override
    public Integer getMappedPort(int originalPort) {
        // For this container assume we always want the UDP port when we ask for port 88
        if (originalPort == 389) {
            return tcp_389;
        } else {
            return super.getMappedPort(originalPort);
        }
    }
}
