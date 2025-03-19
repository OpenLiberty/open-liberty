/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
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
package com.ibm.ws.security.acme.docker.boulder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import org.testcontainers.Testcontainers;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;

import com.github.dockerjava.api.model.Container;
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

	private static final Long EXPIRE_TIME = 3600000L; // 7200000L;

	private static final long LONG_SLEEP = 120000;

	private static final long SHORT_SLEEP = 1000;

	private static final long WAITING_FOR_RUNNING_BOULDER = 15;

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
	 * TODO publish Dockerfile for this custom image
	 */
	private static final String DOCKER_IMAGE = "ryanesch/acme-boulder:1.2";

	/**
	 * Log the output from this testcontainer.
	 * 
	 * @param frame
	 *                  The frame containing log data.
	 */
	public static void log(OutputFrame frame) {
		String msg = frame.getUtf8String();
		if (msg.endsWith("\n"))
			msg = msg.substring(0, msg.length() - 1);
		Log.info(BoulderContainer.class, "boulder", msg);
	}

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

		Log.info(BoulderContainer.class, "BoulderContainer", "ContainerIpAddress: " + getHost());
		Log.info(BoulderContainer.class, "BoulderContainer", "DockerImageName:    " + getDockerImageName());
		Log.info(BoulderContainer.class, "BoulderContainer", "ContainerInfo:      " + getContainerInfo());
	}

	@Override
	protected void configure() {
		this.withEnv("FAKE_DNS", "172.17.0.68");
		this.withEnv("PKCS11_PROXY_SOCKET", "tcp://boulder-hsm:5657");
		this.withEnv("BOULDER_CONFIG_DIR", "test/config");
		this.withEnv("GO111MODULE", "on");
		this.withEnv("GOFLAGS", "-mod=vendor");
		this.withEnv("PYTHONIOENCODING", "utf-8");
		this.withCreateContainerCmdModifier(cmd -> {
			cmd.withDns("10.77.77.77");
		});

		this.withWorkingDirectory("/go/src/github.com/letsencrypt/boulder");
		this.withCommand("/go/src/github.com/letsencrypt/boulder/test/entrypoint.sh");
		this.withNetworkAliases("boulder");
		this.withCreateContainerCmdModifier(cmd -> cmd.withHostName("boulder"));
		this.withExposedPorts(getDnsManagementPort(), getAcmeListenPort(), OCSP_PORT);
		this.withLogConsumer(BoulderContainer::log);
		this.withStartupTimeout(Duration.ofMinutes(10));
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
		 * To manually clean:
		 * 
		 * You will need to have a docker client installed.
		 * 
		 * Check the output.txt for this, "If you need to connect to any currently
		 * running docker containers manually." Look at a local output.txt (run acme_fat
		 * if you don't have current out). There are three DOCKER environment variables
		 * to export (DOCKER_HOST, etc).
		 * 
		 * After exporting (or setting) the three DOCKER variables, when you run docker
		 * commands, you will be connected to the remote machine.
		 * 
		 * Run this to view the running containers:
		 * 
		 * > docker container ls
		 * 
		 * In the output, look for images with these names that are old (like > 1 hour):
		 * mariaDB boulder letsencrypt pebble
		 * 
		 * Copy the CONTAINER ID (first column) and then run
		 * 
		 * > docker container stop CONTAINER_ID
		 */
		boolean everythingStarted = false;

		/*
		 * Try to wait if there's another boulder instance running. If we just try to
		 * start/stop/restart, we can stop the instance for another test, causing
		 * Connection Failed exceptions.
		 * 
		 * If the Boulder/MariaDB refs have been running for a long time, try to clean
		 * it up or it will block us from starting
		 */
		Log.info(BoulderContainer.class, "start", "Checking for other Boulder/MariaDB containers");
		try {
			boolean clearToContinue = true;
			boolean sleep = false;
			for (int b = 1; b <= WAITING_FOR_RUNNING_BOULDER; b++) {
				clearToContinue = true;
				sleep = false;
				Log.info(BoulderContainer.class, "start", "Checking Round " + b);
				List<Container> currentContainers = super.getDockerClient().listContainersCmd().exec();
				for (Container container : currentContainers) {
					String imageName = container.getImage();
					Log.info(BoulderContainer.class, "start", "ImageName: " + imageName);
					if (imageName.toLowerCase().contains("acme-boulder")
							|| imageName.toLowerCase().contains("mariadb")) {
						Long created = container.getCreated() * 1000; // Adjust time to milliseconds
						if ((System.currentTimeMillis() - created) > EXPIRE_TIME) {
							String id = container.getId();
							Log.info(BoulderContainer.class, "start",
									"*****Found a " + imageName + " container already running on the server for "
											+ created + " (" + new Date(created) + ") which is longer than "
											+ EXPIRE_TIME + "ms. Attempt to stop ID " + id);
							super.getDockerClient().stopContainerCmd(id).exec();
						} else {
							Log.info(BoulderContainer.class, "start",
									imageName + " already running on this container from " + created + "ms ("
											+ new Date(created) + "). Sleep and recheck.");
							sleep = true;
						}
						clearToContinue = false;
					}
				}
				if (clearToContinue) {
					break;
				}
				if (sleep) {
					Log.info(BoulderContainer.class, "start",
							"Found running Boulder/MariaDB containers, sleep and recheck");
					try {
						/*
						 * Currently the AcmeRevocationTest runs about 5-6 minutes
						 */
						Thread.sleep(LONG_SLEEP);
					} catch (InterruptedException e) {
						/* Ignore. */
					}
				}
			}
		} catch (Exception e) {
			Log.error(BoulderContainer.class, "start", e,
					"Failed to clean up prior containers. Proceeding in case the next series of retries will work.");
		}

		Log.info(BoulderContainer.class, "start", "Start Boulder containers: Maria DB, Faux Domain server and Boulder");
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
						Thread.sleep(t instanceof NoClassDefFoundError ? SHORT_SLEEP : (LONG_SLEEP));
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
						Thread.sleep(t instanceof NoClassDefFoundError ? SHORT_SLEEP : (LONG_SLEEP));
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

			/*
			 * Only do 1 internal restart. If we hit a ContainerLaunchException and
			 * auto-retry, we'll get a Pool overlap exception (aka, docker container still
			 * running on our ports).
			 * 
			 * Manually clear the boulder container and restart.
			 */
			super.withStartupAttempts(1);
			for (int i = 0; i < NUM_RESTART_ATTEMPTS_ON_EXCEPTION; i++) {
				try {
					super.start();
					break;
				} catch (Throwable t) {
					if (t instanceof ContainerLaunchException) {
						Log.error(BoulderContainer.class, "start", t,
								"Failed mid-start on Boulder container. Stop container manually and try to restart.");

						List<Container> currentContainers = super.getDockerClient().listContainersCmd().exec();
						for (Container container : currentContainers) {
							String imageName = container.getImage();
							if (imageName.toLowerCase().contains("acme-boulder")
									&& !container.getId().equals(bhsm.getContainerId())) {
								/*
								 * Only stop the Boulder image that we just tried to start -- don't stop the
								 * bhsm (domain server). They have the same image name.
								 */
								String id = container.getId();
								Log.info(BoulderContainer.class, "start",
										"Stopping failed boulder image at container ID " + id);
								super.getDockerClient().stopContainerCmd(id).exec();
								break;
							}
						}
					}

					Log.error(BoulderContainer.class, "start", t, "Failed to start boulder, try again.");
					super.stop();

					try {
						/*
						 * On Windows, sometimes we get a java.lang.NoClassDefFoundError:
						 * com/sun/jna/platform/win32/Kernel32 and a shorter sleep before a retry
						 * normally works around it
						 */
						Thread.sleep(t instanceof NoClassDefFoundError ? SHORT_SLEEP : (LONG_SLEEP * i));
					} catch (InterruptedException e) {
						/* Ignore. */
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
			Log.error(BoulderContainer.class, "stop", e, "Error stopping MariaDB container.");
		}

		try {

			if (bhsm != null) {
				bhsm.stop();
				bhsm.close();
			}
		} catch (Exception e) {
			Log.error(BoulderContainer.class, "stop", e, "Error stopping Hardware Security Module (HSM) container.");
		}

		try {
			super.getDockerClient().disconnectFromNetworkCmd().withNetworkId(bluenet.getId()).exec();
			super.getDockerClient().disconnectFromNetworkCmd().withNetworkId(rednet.getId()).exec();
		} catch (Exception e) {
			Log.error(BoulderContainer.class, "stop", e, "Error disconnecting from networks.");
		}

		try {
			if (bluenet != null) {
				bluenet.close();
			}
		} catch (Exception e) {
			Log.error(BoulderContainer.class, "stop", e, "Error closing bluenet.");
		}
		try {
			if (rednet != null) {
				rednet.close();
			}
		} catch (Exception e) {
			Log.error(BoulderContainer.class, "stop", e, "Error closing rednet.");
		}

		super.stop();

		pruneNetwork();

		Log.info(BoulderContainer.class, "stop", "Stopped Boulder services");
	}

	@Override
	public byte[] getAcmeCaIntermediateCertificate() throws Exception {
		return Files.readAllBytes(new File(FILE_INTERMEDIATE_PEM).toPath());
	}

	@Override
	protected String getDnsManagementAddress() {
		return "http://" + this.getHost() + ":" + this.getMappedPort(getDnsManagementPort());
	}

	@Override
	public String getAcmeDirectoryURI(boolean useAcmeURI) {
		return "https://" + this.getHost() + ":" + this.getMappedPort(getAcmeListenPort()) + "/directory";
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
		return "http://" + this.getHost() + ":" + this.getMappedPort(OCSP_PORT);
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
		pruneNetwork();

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
		bhsm = new GenericContainer<>(DOCKER_IMAGE).withEnv("PKCS11_DAEMON_SOCKET", "tcp://0.0.0.0:5657")
				.withExposedPorts(5657).withNetwork(bluenet).withNetworkAliases("boulder-hsm")
				.withCommand("/usr/local/bin/pkcs11-daemon /usr/lib/softhsm/libsofthsm2.so")
				.withLogConsumer(o -> System.out.print("[HSM] " + o.getUtf8String()));
		bhsm.withStartupAttempts(WITH_STARTUP_ATTEMPTS);
	}

	private void pruneNetwork() {
		try {
			super.getDockerClient().pruneCmd(PruneType.NETWORKS).exec();
		} catch (Exception e) {
			Log.info(BoulderContainer.class, "pruneNetwork", "Ignoring exception on prune: " + e);
		}
	}
}
