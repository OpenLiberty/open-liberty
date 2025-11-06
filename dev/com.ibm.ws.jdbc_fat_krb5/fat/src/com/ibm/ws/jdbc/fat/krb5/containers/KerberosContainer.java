/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
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
package com.ibm.ws.jdbc.fat.krb5.containers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

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
    public static final String KRB5_KDC_INTERNAL = "localhost";
    public static final String KRB5_KDC_EXTERNAL = "kerberos";
    public static final String KRB5_PASS = "password";

    private static final DockerImageName KDC_JDBC_SERVER = ImageBuilder //
                    .build("kdc-jdbc-server:3.0.0.2") //
                    .getDockerImageName();

    /**
     * A map of user to keytab file.
     * Avoids calls to the method {@link #requestKeyTable(String)}
     * from requesting the same keytab from the container multiple times
     */
    private static final Map<String, String> EXTERNAL_KEY_TABLE = new HashMap<>();

    private int udp_99;

    public KerberosContainer(Network network) {
        super(KDC_JDBC_SERVER);
        withNetwork(network);
    }

    @Override
    protected void configure() {
        withExposedPorts(99, 464, 749);
        withNetworkAliases(KRB5_KDC_EXTERNAL);
        withCreateContainerCmdModifier(cmd -> {
            cmd.withHostName(KRB5_KDC_EXTERNAL);
        });
        withEnv("KRB5_REALM", KRB5_REALM);
        withEnv("KRB5_KDC", KRB5_KDC_INTERNAL);
        withEnv("KRB5_PASS", KRB5_PASS);

        withLogConsumer(new SimpleLogConsumer(c, "krb5"));
        waitingFor(new LogMessageWaitStrategy()
                        .withRegEx("^.*KERB SETUP COMPLETE.*$")
                        .withStartupTimeout(Duration.ofSeconds(FATRunner.FAT_TEST_LOCALRUN ? 15 : 300)));
        withCreateContainerCmdModifier(cmd -> {
            //Add previously exposed ports and UDP port
            List<ExposedPort> exposedPorts = new ArrayList<>();
            for (ExposedPort p : cmd.getExposedPorts()) {
                exposedPorts.add(p);
            }
            exposedPorts.add(ExposedPort.udp(99));
            cmd.withExposedPorts(exposedPorts);

            //Add previous port bindings and UDP port binding
            Ports ports = cmd.getPortBindings();
            ports.bind(ExposedPort.udp(99), Ports.Binding.empty());
            cmd.withPortBindings(ports);
            cmd.withHostName(KRB5_KDC_EXTERNAL);
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
        super.start();
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

    /**
     * Generates a krb5 keytab in the KDC container,
     * and then copy it to the destination location on the client system.
     *
     * @param destination where to copy the keytab file
     * @param user        the user to generate a keytab file
     */
    public void copyUserKeytab(final Path destination, final String user) {
        try {
            this.copyFileFromContainer(requestKeyTable(user), destination.toAbsolutePath().toString());
        } catch (Exception e) {
            throw new RuntimeException("Could not copy keytab file from KDC " + this.getContainerId(), e);
        }
    }

    /**
     * Requests the KDC to export a keytab file for this user.
     * Then returns the location the keytab file was saved in the container.
     *
     * @param user to export to the keytab file
     * @return the container location for the keytab file
     * @throws Exception if generating the keytab failed
     */
    private String requestKeyTable(final String user) throws Exception {
        final String m = "requestKeyTable";
        String containerLocation;

        // Try to find cached key table
        if (EXTERNAL_KEY_TABLE.containsKey(user)) {
            containerLocation = EXTERNAL_KEY_TABLE.get(user);
            Log.info(c, m, "Returning cached external key table from: " + this.getContainerId() + ":" + containerLocation);
            return containerLocation;
        } else {
            containerLocation = "/tmp/client_" + user + "_krb5.keytab";
        }

        // Queries
        final String[] script = new String[] { "/tmp/client-keytab.sh", user };

        // Export principle to new keytab file
        Log.info(c, m, "Execute in container " + this.getContainerName() + " : " + Arrays.toString(script));
        ExecResult result = this.execInContainer(script);
        if (result.getExitCode() != 0) {
            Log.info(c, m, "\tSTDOUT: " + result.getStdout());
            Log.info(c, m, "\tSTDERR: " + result.getStderr());
            throw new IllegalStateException("Could not generate keytab file because exit code was: " + result.getExitCode() + " see logs for details.");
        } else {
            Log.info(c, m, "\tSTDOUT: " + result.getStdout());
        }

        // Cache location
        EXTERNAL_KEY_TABLE.put(user, containerLocation);

        return containerLocation;
    }
}
