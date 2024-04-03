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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

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
    public static String LDAP_HOSTNAME = "ldap";
    public static final String KRB5_PASS = "admin";
    public static String KDC_HOSTNAME = "kerberos";
    public static String EXTERNAL_HOSTNAME = "notsetyet";

    public static String BASE_DN = LdapKerberosUtils.BASE_DN; // default, override in extending class

    /* The Domain needs to be capitalized for Kerberos, but not necessarily for LDAP. */
    public static String DOMAIN = LdapKerberosUtils.DOMAIN; // default, override in extending class

    protected static String bindPassword = LdapKerberosUtils.BIND_PASSWORD; // default, override in extending class

    protected static String bindUserName = LdapKerberosUtils.BIND_USER; // default, override in extending class

    protected static String bindPrincipalName = LdapKerberosUtils.BIND_PRINCIPAL_NAME; // default, override in extending class

    // NOTE: If this is ever updated, don't forget to push to docker hub, but DO NOT overwrite existing versions
    private static final String IMAGE = "zachhein/ldap-server:0.1";

    private int tcp_389;

    public LdapContainer(Network network) {
        super(IMAGE);
        withNetwork(network);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void configure() {
        withNetworkAliases(LDAP_HOSTNAME);
        withCreateContainerCmdModifier(cmd -> {
            cmd.withHostName(LDAP_HOSTNAME);
            //Log.info(c, "configure", "adding hosts entry: " + EXTERNAL_HOSTNAME + ":" + "127.0.0.1");
            //cmd.withExtraHosts(KDC_HOSTNAME + ":" + EXTERNAL_HOSTNAME);
            //cmd.withExtraHosts(EXTERNAL_HOSTNAME + ":" + "127.0.0.1");
        });
        withEnv("KRB5_REALM", KRB5_REALM);
        Log.info(c, "configure", "setting env LDAP_HOSTNAME: " + LDAP_HOSTNAME);
        withEnv("LDAP_HOSTNAME", LDAP_HOSTNAME);
        withEnv("KRB5_PASS", KRB5_PASS);
        withEnv("KDC_HOSTNAME", KDC_HOSTNAME);
        Log.info(c, "configure", "setting env KDC_HOSTNAME: " + KDC_HOSTNAME);

        withLogConsumer(new SimpleLogConsumer(c, "ldap"));
        waitingFor(new LogMessageWaitStrategy()
                        .withRegEx("^.*stop the ldap.*$")
                        .withStartupTimeout(Duration.ofSeconds(FATRunner.FAT_TEST_LOCALRUN ? 50 : 300)));
        withCreateContainerCmdModifier(cmd -> {
            //Add previously exposed ports and UDP port

            List<ExposedPort> exposedPorts = new ArrayList<ExposedPort>();
            for (ExposedPort p : cmd.getExposedPorts()) {
                Log.info(c, "configure", "ExposedPort=" + p.getPort());
                exposedPorts.add(p);
            }
            exposedPorts.add(ExposedPort.tcp(389));
            cmd.withExposedPorts(exposedPorts);

            // Add previous port bindings and KDC and LDAP ports
            Ports ports = cmd.getPortBindings();

            String ldapPortMapping = String.format("%d:%d/%s", 389, 389, InternetProtocol.TCP);
            Log.info(c, "configure", "adding ldap port mapping: " + ldapPortMapping);

            Log.info(c, "configure", "PortBinding.parse(ldapPortMapping): " + PortBinding.parse(ldapPortMapping));
            ports.add(PortBinding.parse(ldapPortMapping));

            Log.info(c, "configure", "ports: " + ports);
            cmd.withPortBindings(ports);
            cmd.withHostName(LDAP_HOSTNAME);
        });
    }

    //@Override
    //protected void containerIsStarted(InspectContainerResponse containerInfo) {
    ///    String tcp389 = containerInfo.getNetworkSettings().getPorts().getBindings().get(new ExposedPort(389, InternetProtocol.TCP))[0].getHostPortSpec();
    //    tcp_389 = Integer.valueOf(tcp389);
    // }

    @Override
    public void start() {
        String dockerHostIp = DockerClientFactory.instance().dockerHostIpAddress();
        withEnv("EXTERNAL_HOSTNAME", dockerHostIp);
        Log.info(c, "start", "Using EXTERNAL_HOSTNAME=" + dockerHostIp);
        Log.info(c, "start", "getHost()=" + getHost());
        EXTERNAL_HOSTNAME = dockerHostIp;
        //KDC_HOSTNAME = dockerHostIp;
        //KRB5_KDC = KDC_HOSTNAME;
        super.start();
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
