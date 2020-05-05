/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.acme.docker.boulder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map.Entry;

import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ContainerNetwork.Ipam;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.acme.docker.CAContainer;
import com.ibm.ws.security.acme.docker.pebble.PebbleContainer;

/**
 * Testcontainer implementation for the letsencrypt/boulder container.
 * 
 * <p/>
 * This testcontainer contains both the Boulder ACME compliant CA server as well
 * as the challtestsrv server servering as a mock DNS server. The container is
 * initialized to return all HTTP-01 challenges to the client IP address.
 */
public class BoulderContainer extends CAContainer {
	/*
	 * !!!! README !!!
	 * 
	 * Boulder is designed to run with docker-compose. In order to integrate
	 * Boulder into the acme FAT, the docker-compose file from letsencrypt was
	 * turned into the following GenericContainers. This class consists of three
	 * containers and two networks. Because Boulder consists of many files, a
	 * Docker image was created that is built on top of
	 * letsencrypt/boulder-tools-go and includes all the files that would
	 * otherwise need to be copied at runtime. This saves 8+ minutes per run.
	 */

	private static final String FILE_MINICA_PEM = "lib/LibertyFATTestFiles/boulder.minica.pem";

	public final Network bluenet = Network.builder().createNetworkCmdModifier(cmd -> {
		cmd.withDriver("bridge");
		cmd.withIpam(new com.github.dockerjava.api.model.Network.Ipam().withDriver("default")
				.withConfig(new com.github.dockerjava.api.model.Network.Ipam.Config().withSubnet("10.77.77.0/24")));
	}).build();

	public final Network rednet = Network.builder().createNetworkCmdModifier(cmd -> {
		cmd.withDriver("bridge");
		cmd.withIpam(new com.github.dockerjava.api.model.Network.Ipam().withDriver("default")
				.withConfig(new com.github.dockerjava.api.model.Network.Ipam.Config().withSubnet("10.88.88.0/24")));
	}).build();

	/**
	 * Container that runs the hardware security module. Use the same Docker
	 * Image as the base container.
	 */
	public final GenericContainer<?> bhsm = new GenericContainer<>(DOCKER_IMAGE)
			.withEnv("PKCS11_DAEMON_SOCKET", "tcp://0.0.0.0:5657").withExposedPorts(5657).withNetwork(bluenet)
			.withNetworkAliases("boulder-hsm")
			.withCommand("/usr/local/bin/pkcs11-daemon /usr/lib/softhsm/libsofthsm2.so")
			.withLogConsumer(o -> System.out.print("[HSM] " + o.getUtf8String()));

	/**
	 * Container that runs MariaDB
	 */
	public final GenericContainer<?> bmysql = new GenericContainer<>("mariadb:10.3").withNetwork(bluenet)
			.withExposedPorts(3306).withNetworkAliases("boulder-mysql").withEnv("MYSQL_ALLOW_EMPTY_PASSWORD", "yes")
			.withCommand(
					"mysqld --bind-address=0.0.0.0 --slow-query-log --log-output=TABLE --log-queries-not-using-indexes=ON")
			.withLogConsumer(o -> System.out.print("[SQL] " + o.getUtf8String()));

	/**
	 * Docker image that contains all the files from boulder-tools-go.
	 */
	private static final String DOCKER_IMAGE = "ryanesch/acme-boulder:1.1";

	/**
	 * Create and start a BoulderContainer. Use a custom docker image built on
	 * top of boulder-tools-go.
	 */
	public BoulderContainer() {
		super(DOCKER_IMAGE, 5002, 4001, 8055);

		/*
		 * Need to expose the HTTP port that is used to answer the HTTP-01
		 * challenge.
		 */
		Testcontainers.exposeHostPorts(getHttpPort());
		start();

		try {
			/*
			 * Default responses to the client host.
			 */
			setDnsDefaultIpv4(getClientHost());

			/*
			 * Disable the IPv6 responses. The Pebble CA server responds on AAAA
			 * (IPv6) responses before A (IPv4) responses, and we don't
			 * currently have the testcontainer host's IPv6 address.
			 */
			setDnsDefaultIpv6("");
		} catch (IOException e) {
			throw new IllegalStateException("Failed to set default mock DNS A and AAAA record IP addresses.", e);
		}

		Log.info(PebbleContainer.class, "BoulderContainer", "ContainerIpAddress: " + getContainerIpAddress());
		Log.info(PebbleContainer.class, "BoulderContainer", "DockerImageName:    " + getDockerImageName());
		Log.info(PebbleContainer.class, "BoulderContainer", "ContainerInfo:      " + getContainerInfo());
	}

	@Override
	protected void configure() {
		this.withEnv("FAKE_DNS", "172.17.0.68").withEnv("PKCS11_PROXY_SOCKET", "tcp://boulder-hsm:5657")
				.withEnv("BOULDER_CONFIG_DIR", "test/config").withEnv("GO111MODULE", "on")
				.withEnv("GOFLAGS", "-mod=vendor").withEnv("PYTHONIOENCODING", "utf-8")
				.withCreateContainerCmdModifier(cmd -> {
					cmd.withDns("10.77.77.77");
				})

				.withWorkingDirectory("/go/src/github.com/letsencrypt/boulder")
				.withCommand("/go/src/github.com/letsencrypt/boulder/test/entrypoint.sh").withNetworkAliases("boulder")
				.withCreateContainerCmdModifier(cmd -> cmd.withHostName("boulder"))
				.withExposedPorts(getDnsManagementPort(), getAcmeListenPort())
				.withLogConsumer(o -> System.out.print("[BOL] " + o.getUtf8String()))
				.withStartupTimeout(Duration.ofMinutes(3));
	}

	@Override
	protected void containerIsCreated(String containerId) {
		getDockerClient().connectToNetworkCmd().withNetworkId(bluenet.getId()).withContainerId(containerId)
				.withContainerNetwork(new ContainerNetwork().withIpv4Address("10.77.77.77")
						.withNetworkID(bluenet.getId()).withIpamConfig(new Ipam().withIpv4Address("10.77.77.77"))
						.withAliases("sa1.boulder", "ca1.boulder", "ra1.boulder", "va1.boulder", "publisher1.boulder",
								"ocsp-updater.boulder", "admin-revoker.boulder", "nonce1.boulder"))
				.exec();

		getDockerClient().connectToNetworkCmd().withNetworkId(rednet.getId()).withContainerId(containerId)
				.withContainerNetwork(new ContainerNetwork().withIpv4Address("10.88.88.88")
						.withNetworkID(rednet.getId()).withIpamConfig(new Ipam().withIpv4Address("10.88.88.88"))
						.withAliases("sa2.boulder", "ca2.boulder", "ra2.boulder", "va2.boulder", "publisher2.boulder",
								"nonce2.boulder"))
				.exec();
	}

	@Override
	public void start() {
		/*
		 * We need to start up the containers in an orderly fashion so that we
		 * can pass the IP address of the DNS server to the Boulder server.
		 */
		bmysql.start();
		bhsm.start();
		super.start();
	}

	@Override
	public void stop() {
		/*
		 * Stop all the containers, the challenge server, and the networks.
		 */
		bmysql.stop();
		bhsm.stop();
		super.stop();
		bluenet.close();
		rednet.close();
	}

	@Override
	public byte[] getAcmeCaIntermediateCertificate() throws Exception {
		return Files.readAllBytes(new File(FILE_MINICA_PEM).toPath());
	}

	@Override
	protected String getDnsManagementAddress() {
		return "http://" + this.getContainerIpAddress() + ":" + this.getMappedPort(getDnsManagementPort());
	}

	@Override
	public String getAcmeDirectoryURI(boolean usePebbleURI) {
		return "http://" + this.getContainerIpAddress() + ":" + this.getMappedPort(getAcmeListenPort()) + "/directory";
	}

	@Override
	protected String getIntraContainerIP() {
		String intraContainerIpAddress = null;
		for (Entry<String, ContainerNetwork> entry : getContainerInfo().getNetworkSettings().getNetworks().entrySet()) {
			intraContainerIpAddress = entry.getValue().getIpAddress();
			break;
		}
		if (intraContainerIpAddress == null) {
			throw new IllegalStateException("Didn't find IP address for challtestsrv server.");
		}
		return intraContainerIpAddress;
	}
}
