package com.ibm.ws.security.acme.docker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.testcontainers.Testcontainers;

/**
 * Driver to start Pebble container services from within a stand-alone JVM.
 */
public class PebbleDriver {
	public static ChalltestsrvContainer challtestsrv = null;

	public static PebbleContainer pebble = null;

	static {
		// TODO Should support remote docker via Consul?
		System.setProperty("global.consulServerList", "");
	}

	public static void main(String[] args) throws Exception {
		System.out.println("\n\n");

		/*
		 * Start the Pebble environment.
		 */
		start();

		/*
		 * Get the domains to configure.
		 */
		String domains = System.getProperty("domains");
		List<String> domainList = new ArrayList<String>();
		if (domains != null && !domains.trim().isEmpty()) {
			StringTokenizer tknzr = new StringTokenizer(domains, ",");
			while (tknzr.hasMoreTokens()) {
				String token = tknzr.nextToken().trim();
				if (!token.isEmpty()) {
					domainList.add(token);
				}
			}
		} else {
			/*
			 * Default to domain.com.
			 */
			domainList.add("domain.com");
		}

		/*
		 * Configure the DNS to point back to us.
		 */
		configureDnsForDomains(domainList.toArray(new String[0]));

		/*
		 * Wait until the process has been cancelled via ctrl-c.
		 */
		printBanner();
		while (true) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}

		stop();
	}

	private static void printBanner() {
		StringBuffer banner = new StringBuffer();
		banner.append("\n\n\n");
		banner.append("***********************************************************************\n");
		banner.append("*\n");
		banner.append("*\n");
		banner.append(
				"* Pebble URI: " + pebble.getAcmeDirectoryURI(true) + " or " + pebble.getAcmeDirectoryURI(true) + "\n");
		banner.append("* HTTP port: " + PebbleContainer.HTTP_PORT + "\n");
		banner.append("*\n");
		banner.append("*\n");
		banner.append("***********************************************************************\n");
		banner.append("\n\n\n");
		banner.append("Use 'ctrl-c' to terminate execution...");

		System.out.println(banner.toString());
		System.out.flush();
	}

	private static void start() {

		System.out.println("Starting Pebble environment bring up");

		/*
		 * Need to expose the HTTP port that is used to answer the HTTP-01
		 * challenge.
		 */
		System.out.println("Running Testcontainers.exposeHostPorts");
		Testcontainers.exposeHostPorts(PebbleContainer.HTTP_PORT);

		/*
		 * Startup the challtestsrv container first. This container will serve
		 * as a mock DNS server to the Pebble server that starts on the other
		 * container.
		 */
		System.out.println("Starting ChalltestsrvContainer");
		challtestsrv = new ChalltestsrvContainer();
		challtestsrv.start();

		/*
		 * Startup the pebble server.
		 */
		System.out.println("Starting PebbleContainer");
		pebble = new PebbleContainer(challtestsrv.getIntraContainerIP() + ":" + ChalltestsrvContainer.DNS_PORT,
				challtestsrv.getNetwork());
		pebble.start();
	}

	private static void stop() {
		System.out.println("Stopping PebbleContainer.");
		if (pebble != null) {
			pebble.stop();
		}

		System.out.println("Stopping ChalltestsrvContainer.");
		if (challtestsrv != null) {
			challtestsrv.stop();
		}
	}

	// TODO Would be nice to just use the AcmeFATUtils version.
	public static void configureDnsForDomains(String... domains) throws Exception {

		System.out.println("Configuring DNS with the following domains: " + Arrays.toString(domains));

		for (String domain : domains) {
			/*
			 * Disable the IPv6 responses for this domain. The Pebble CA server
			 * responds on AAAA (IPv6) responses before A (IPv4) responses, and
			 * we don't currently have the testcontainer host's IPv6 address.
			 */
			challtestsrv.addARecord(domain, pebble.getClientHost());
			challtestsrv.addAAAARecord(domain, "");
		}
	}
}
