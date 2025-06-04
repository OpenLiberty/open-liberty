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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

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
import componenttest.topology.utils.FileUtils;

public class KerberosContainer extends GenericContainer<KerberosContainer> {

    private static final Class<?> c = KerberosContainer.class;

    public static final String KRB5_REALM = "EXAMPLE.COM";
    public static final String KRB5_KDC_INTERNAL = "localhost";
    public static final String KRB5_KDC_EXTERNAL = "kerberos";
    public static final String KRB5_PASS = "password";

    private static final DockerImageName KDC_JDBC_SERVER = ImageBuilder.build("kdc-jdbc-server:3.0.0.1").getDockerImageName();

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
        debugKerberosVersion();
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

    public void generateConf(Path outputPath, String... additionalLibDefaults) throws IOException {
        String nl = "\n";
        String tab = "\t";

        StringBuilder conf = new StringBuilder();
        conf.append("[libdefaults]").append(nl);
        conf.append(tab).append("rdns = false").append(nl);
        conf.append(tab).append("renew_lifetime = 7d").append(nl);
        conf.append(tab).append("ticket_lifetime = 24h").append(nl);
        conf.append(tab).append("dns_lookup_realm = false").append(nl);
        conf.append(tab).append("default_realm = " + KRB5_REALM.toUpperCase()).append(nl);
        conf.append(nl);

        conf.append("# The following krb5.conf variables are only for MIT Kerberos.").append(nl);
        conf.append(tab).append("kdc_timesync = 1").append(nl);
        conf.append(tab).append("ccache_type = 4").append(nl);
        conf.append(tab).append("forwardable = true").append(nl);
        conf.append(tab).append("proxiable = true").append(nl);
        conf.append(nl);

        conf.append("# The following libdefaults parameters are only for Heimdal Kerberos.").append(nl);
        conf.append(tab).append("fcc-mit-ticketflags = true").append(nl);
        conf.append(nl);

        if (additionalLibDefaults != null && additionalLibDefaults.length > 0) {
            conf.append("# Additional configurations for this test case.").append(nl);
            for (String config : additionalLibDefaults) {
                conf.append(tab).append(config).append(nl);
            }
            conf.append(nl);
        }

        conf.append("[realms]").append(nl);
        conf.append(tab).append(KRB5_REALM.toUpperCase() + " = {").append(nl);
        conf.append(tab).append(tab).append("kdc = " + getHost() + ":" + getMappedPort(99)).append(nl);
        conf.append(tab).append(tab).append("admin_server = " + getHost()).append(nl);
        conf.append(tab).append("}").append(nl);
        conf.append(nl);

        conf.append("[domain_realm]").append(nl);
        conf.append(tab).append("." + KRB5_REALM.toLowerCase() + " = " + KRB5_REALM.toUpperCase()).append(nl);
        conf.append(tab).append(KRB5_REALM.toLowerCase() + " = " + KRB5_REALM.toUpperCase()).append(nl);

        outputPath.getParent().toFile().mkdirs();
        Files.write(outputPath, conf.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Use generateConf instead
     */
    @Deprecated
    public void configureKerberos() throws IOException {
        Path krbConfPath = Paths.get("/etc/krb5.conf");
        String krbConf = FileUtils.readFile(krbConfPath.toAbsolutePath().toString());

        krbConf = configureProperty(krbConf, "libdefaults", "default_realm", KRB5_REALM);
        krbConf = configureProperty(krbConf, "libdefaults", "dns_lookup_realm", "false");
        krbConf = configureProperty(krbConf, "libdefaults", "ticket_lifetime", "24h");
        krbConf = configureProperty(krbConf, "libdefaults", "renew_lifetime", "7d");
        krbConf = configureProperty(krbConf, "libdefaults", "forwardable", "true");
        krbConf = configureProperty(krbConf, "libdefaults", "rdns", "false");

        if (!krbConf.contains("[realms]")) {
            krbConf += "\n\n[realms]";
        }
        if (!krbConf.contains(KRB5_REALM + " = {")) {
            krbConf = krbConf.replace("[realms]", "[realms]\n\t" +
                                                  KRB5_REALM + " = {\n\t\t" +
                                                  "kdc = " + getHost() + ":" + getMappedPort(99) + "\n\t\t" +
                                                  "admin_server = " + getHost() + "\n\t}\n");
        }

        if (!krbConf.contains("[domain_realm]")) {
            krbConf += "\n\n[domain_realm]";
        }
        krbConf = configureProperty(krbConf, "domain_realm", KRB5_REALM.toLowerCase(), KRB5_REALM);
        krbConf = configureProperty(krbConf, "domain_realm", "." + KRB5_REALM.toLowerCase(), KRB5_REALM);

        Log.info(c, "configureKerberos", "Transformed kerberos config:\n" + krbConf);
        Files.write(krbConfPath, krbConf.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    private static String configureProperty(String krbConf, String section, String key, String value) {
        if (krbConf.contains(key + " = " + value)) {
            // already correct
        } else if (krbConf.contains(key + " = ")) {
            krbConf = krbConf.replaceAll(key.replace(".", "\\.") + " = .*", key + " = " + value);
        } else {
            krbConf = krbConf.replace("[" + section + "]", "[" + section + "]\n\t" + key + " = " + value);
        }
        return krbConf;
    }

    private void debugKerberosVersion() {
        final String m = "debugKerberosVersion";
        final String nl = "\n";

        ProcessBuilder builder = new ProcessBuilder("ktutil", "--version");
        StringBuilder output = new StringBuilder().append(nl);
        try {
            Process proc = builder.start();
            if (!proc.waitFor(15, TimeUnit.SECONDS)) {
                Log.info(c, m, "Proc timed out... destroying forcibly");
                proc.destroyForcibly();
            }

            output.append("STDOUT:").append(nl);
            output.append(readInputStream(proc.getInputStream())).append(nl);
            output.append("STDERR:").append(nl);
            output.append(readInputStream(proc.getErrorStream())).append(nl);

            Log.info(c, m, "Process output:");
            Log.info(c, m, output.toString());

            if (proc.exitValue() != 0) {
                Log.info(c, m, "Execution failed with exit code: " + proc.exitValue());
            }
        } catch (Exception e) {
            Log.info(c, m, "Execution of command failed with message: " + e.getMessage());
        }

    }

    /**
     * This doesn't seem to be necessary because we can simply check in the keytab file
     */
    @Deprecated
    public void generateKeytab(String username, Path output) throws Exception {
        Files.deleteIfExists(output);
        Process proc = Runtime.getRuntime().exec("ktutil");
        OutputStream ktInput = proc.getOutputStream();
        ktInput.write(("add_entry -password -p " + username + "@" + KRB5_REALM +
                       " -k 1 -e aes256-cts\n" + KRB5_PASS + "\nwkt " + output.toAbsolutePath().toString()).getBytes());
        ktInput.flush();
        ktInput.close();
        if (!proc.waitFor(15, TimeUnit.SECONDS)) {
            Log.info(c, "generateKeytab", "Proc timed out... destroying forcibly");
            proc.destroyForcibly();
        }
        Log.info(c, "generateKeytab", "Process stdout:");
        String procOut = "STDOUT:\n" + readInputStream(proc.getInputStream());
        procOut += "\nSTDERR:\n" + readInputStream(proc.getErrorStream());
        Log.info(c, "generateKeytab", procOut);
        if (proc.exitValue() != 0) {
            throw new RuntimeException("Process failed with output: " + procOut);
        }
    }

    private static String readInputStream(InputStream is) {
        @SuppressWarnings("resource")
        Scanner s = new Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
