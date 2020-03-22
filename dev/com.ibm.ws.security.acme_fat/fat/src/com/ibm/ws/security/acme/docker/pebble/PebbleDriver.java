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
 
package com.ibm.ws.security.acme.docker.pebble;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import com.ibm.ws.security.acme.docker.CAContainer;

/**
 * Driver to start Pebble container services from within a stand-alone JVM.
 */
public class PebbleDriver {

	public static CAContainer pebble;

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
		printBanner(domainList);
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

	private static void printBanner(List<String> domainList) {
		StringBuffer banner = new StringBuffer();
		banner.append("\n\n\n");
		banner.append("***********************************************************************\n");
		banner.append("*\n");
		banner.append("*\n");
		banner.append("* Pebble URI: " + pebble.getAcmeDirectoryURI(false) + " or " + pebble.getAcmeDirectoryURI(true)
				+ "\n");
		banner.append("* HTTP port: " + pebble.getHttpPort() + "\n");
		banner.append("* Domains: " + domainList + "\n");
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
		 * Startup the pebble server.
		 */
		System.out.println("Starting PebbleContainer");
		pebble = new PebbleContainer();
	}

	private static void stop() {
		System.out.println("Stopping PebbleContainer.");
		if (pebble != null) {
			pebble.stop();
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
			pebble.addARecord(domain, pebble.getClientHost());
			pebble.addAAAARecord(domain, "");
		}
	}
}
