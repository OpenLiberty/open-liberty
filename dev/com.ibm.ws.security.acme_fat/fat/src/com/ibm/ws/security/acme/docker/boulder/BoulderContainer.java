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
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ContainerNetwork.Ipam;
import com.github.dockerjava.api.model.PruneType;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.acme.docker.CAContainer;

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

	private static final int OCSP_PORT = 4002;

	private static final String FILE_INTERMEDIATE_PEM = "lib/LibertyFATTestFiles/boulder.intermediate.pem";

	public Network bluenet = Network.builder().createNetworkCmdModifier(cmd -> {
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
	public GenericContainer<?> bhsm = null;
	/**
	 * Container that runs MariaDB
	 */
	public GenericContainer<?> bmysql = null;

	/**
	 * Docker image that contains all the files from boulder-tools-go.
	 */
	private static final String DOCKER_IMAGE = "ryanesch/acme-boulder:1.1";

	/**
	 * Create and start a BoulderContainer. Use a custom docker image built on
	 * top of boulder-tools-go.
	 */
	public BoulderContainer() {
		super(DOCKER_IMAGE, 5002, 4431, 8055);

		/*
		 * Need to expose the HTTP port that is used to answer the HTTP-01
		 * challenge on the client.
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

		Log.info(BoulderContainer.class, "BoulderContainer", "ContainerIpAddress: " + getContainerIpAddress());
		Log.info(BoulderContainer.class, "BoulderContainer", "DockerImageName:    " + getDockerImageName());
		Log.info(BoulderContainer.class, "BoulderContainer", "ContainerInfo:      " + getContainerInfo());
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
				.withExposedPorts(getDnsManagementPort(), getAcmeListenPort(), OCSP_PORT)
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

		/*
		 * The excessive restarts and sleeps on bmysql are to avoid hitting
		 * com.github.dockerjava.api.exception.DockerException:
		 * {"message":"Pool overlaps with other one on this address space"} when the
		 * build system is running fat tests in parallel. Usually, once we get the
		 * database container running, we don't need restarts on the rest of the
		 * containers.
		 * 
		 * If the test starts failing with "pool" errors, the docker container might
		 * have a stale mariadb and/or boulder container running on it. Since the IP
		 * addresses are hardcoded in the Boulder imaging we are using, we can't start
		 * another container without conflicting with the stale container.
		 * 
		 * To clean:
		 * 
		 * You will need to have a docker client installed.
		 * 
		 * Check the output.txt for this, If you need to connect to any currently
		 * running docker containers manually, export the following environment
		 * variables in your terminal, and set the three variables in a terminal (swap
		 * export to set for windows).
		 * 
		 * Now when you run docker commands, you will be connected to the remote
		 * machine.
		 * 
		 * Run this to view the running containers:
		 * 
		 * > docker container ls
		 * 
		 * In the output, look for images with these names that are old (like > 1 hour)
		 * mariaDB boulder letsencrypt
		 * 
		 * Copy the CONTAINER ID (first column) and then run
		 * 
		 * > docker container stop CONTAINER_ID
		 */
		boolean everythingStarted = false;
		try {
			initSQL();
			for (int i = 0; i < (NUM_RESTART_ATTEMPTS_ON_EXCEPTION * 2); i++) {
				try {
					bmysql.start();
					break;
				} catch (Throwable t) {
					Log.error(BoulderContainer.class, "start", t, "Failed to start bmysql, sleep and try again.");

					/*
					 * clean up and reinitialize If we don't recreate bluenet from scratch, we'll
					 * get a NPE on NetworkMode exception on restart
					 */
					bmysql.stop();
					try {
						bluenet.close();
					} catch (Throwable bn) {
						// may throw an NPE, but we don't care
					}
					bluenet = null;

					try {
						/*
						 * On Windows, sometimes we get a java.lang.NoClassDefFoundError:
						 * com/sun/jna/platform/win32/Kernel32 and a shorter sleep before a retry
						 * normally works around it
						 */
						Thread.sleep(t instanceof NoClassDefFoundError ? 1000 : (120000));
					} catch (InterruptedException e) {
					}
					initSQL();
				}
			}
			if (!bmysql.isRunning()) {
				/*
				 * One more try
				 */
				bmysql.start();
			}

			initSM();
			for (int i = 0; i < NUM_RESTART_ATTEMPTS_ON_EXCEPTION; i++) {
				try {
					bhsm.start();
					break;
				} catch (Throwable t) {
					Log.error(BoulderContainer.class, "start", t, "Failed to start bhsm, try again.");

					bhsm.stop();
					initSM();
					try {
						Thread.sleep(t instanceof NoClassDefFoundError ? 1000 : (120000));
					} catch (InterruptedException e) {
					}
				}
			}
			if (!bhsm.isRunning()) {
				/*
				 * One more try
				 */
				bhsm.start();
			}

			super.withStartupAttempts(WITH_STARTUP_ATTEMPTS);
			for (int i = 0; i < NUM_RESTART_ATTEMPTS_ON_EXCEPTION; i++) {
				try {
					super.start();
					break;
				} catch (Throwable t) {
					if (t instanceof ContainerLaunchException) {
						Log.error(BoulderContainer.class, "start", t,
								"Possibly failed to start boulder, to far along in the init process to restart.");
						break;
					}

					Log.error(BoulderContainer.class, "start", t, "Failed to start boulder, try again.");
					super.stop();

					try {
						/*
						 * On Windows, sometimes we get a java.lang.NoClassDefFoundError:
						 * com/sun/jna/platform/win32/Kernel32 and a shorter sleep before a retry
						 * normally works around it
						 */
						Thread.sleep(t instanceof NoClassDefFoundError ? 1000 : (120000 * i));
					} catch (InterruptedException e) {
					}
				}
			}
			if (!super.isRunning()) {
				super.start();
			}

			everythingStarted = true;
		} finally {
			if (!everythingStarted) {
				/*
				 * Have to call stop here as we haven't finished initializing the caller for the
				 * FAT test to call stop on teardown
				 */
				stop();
			}
		}

	}

	@Override
	public void stop() {
		Log.info(BoulderContainer.class, "stop", "Stopping Boulder services");
		/*
		 * Stop all the containers, the challenge server, and the networks. Do our best
		 * to stop and clean everything, otherwise if we leave artifacts on the docker
		 * containers, we can be prevented from started on them in the future with
		 * "Pool overlaps with other one on this address space" exception
		 */

		try {
			if (bmysql != null) {
				bmysql.stop();
				bmysql.close();
			}
		} catch (Exception e) {

		}

		try {

			if (bhsm != null) {
				bhsm.stop();
				bhsm.close();

			}
		} catch (Exception e) {

		}

		try {
			super.getDockerClient().disconnectFromNetworkCmd().withNetworkId(bluenet.getId());
			super.getDockerClient().disconnectFromNetworkCmd().withNetworkId(rednet.getId());
		} catch (Exception e) {

		}

		try {
			if (bluenet != null) {
				bluenet.close();
			}
		} catch (Exception e) {

		}
		try {
			if (rednet != null) {
				rednet.close();
			}
		} catch (Exception e) {

		}

		super.stop();

		super.getDockerClient().pruneCmd(PruneType.NETWORKS);

		Log.info(BoulderContainer.class, "stop", "Stopped Boulder services");
	}

	@Override
	public byte[] getAcmeCaIntermediateCertificate() throws Exception {
		return Files.readAllBytes(new File(FILE_INTERMEDIATE_PEM).toPath());
	}

	@Override
	protected String getDnsManagementAddress() {
		return "http://" + this.getContainerIpAddress() + ":" + this.getMappedPort(getDnsManagementPort());
	}

	@Override
	public String getAcmeDirectoryURI(boolean useAcmeURI) {
		return "https://" + this.getContainerIpAddress() + ":" + this.getMappedPort(getAcmeListenPort()) + "/directory";
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

	@Override
	public String getOcspResponderUrl() {
		return "http://" + this.getContainerIpAddress() + ":" + this.getMappedPort(OCSP_PORT);
	}

	@Override
	public void startDNSServer() {
		throw new UnsupportedOperationException(
				getClass().getSimpleName() + " does not provider support for starting the DNS server.");
	}

	@Override
	public void stopDNSServer() {
		throw new UnsupportedOperationException(
				getClass().getSimpleName() + " does not provider support for stopping the DNS server.");
	}

	private void initSQL() {
		super.getDockerClient().pruneCmd(PruneType.NETWORKS);

		if (bluenet == null) {
			bluenet = Network.builder().createNetworkCmdModifier(cmd -> {
				cmd.withDriver("bridge");
				cmd.withIpam(new com.github.dockerjava.api.model.Network.Ipam().withDriver("default").withConfig(
						new com.github.dockerjava.api.model.Network.Ipam.Config().withSubnet("10.77.77.0/24")));
			}).build();
		}
		bmysql = new GenericContainer<>("mariadb:10.3").withNetwork(bluenet).withNetworkMode("host")
				.withExposedPorts(3306).withNetworkAliases("boulder-mysql").withEnv("MYSQL_ALLOW_EMPTY_PASSWORD", "yes")
				.withCommand(
						"mysqld --bind-address=0.0.0.0 --slow-query-log --log-output=TABLE --log-queries-not-using-indexes=ON")
				.withLogConsumer(o -> System.out.print("[SQL] " + o.getUtf8String()));
		/*
		 * If we get the "Pool overlaps with other one on this address space" then
		 * retrying without rebuilding the supplied network results in a no NetworkMode
		 * exception
		 */
		bmysql.withStartupAttempts(1);
	}

	private void initSM() {
		bhsm = new GenericContainer<>(DOCKER_IMAGE)
		.withEnv("PKCS11_DAEMON_SOCKET", "tcp://0.0.0.0:5657").withExposedPorts(5657).withNetwork(bluenet)
		.withNetworkAliases("boulder-hsm")
		.withCommand("/usr/local/bin/pkcs11-daemon /usr/lib/softhsm/libsofthsm2.so")
		.withLogConsumer(o -> System.out.print("[HSM] " + o.getUtf8String()));
		bhsm.withStartupAttempts(WITH_STARTUP_ATTEMPTS);
	}
}
