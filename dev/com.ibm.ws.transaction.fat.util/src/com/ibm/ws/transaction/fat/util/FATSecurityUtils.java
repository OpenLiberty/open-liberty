/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.transaction.fat.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;

public class FATSecurityUtils {
	private static String CERT_PATH = "/resources/security/key.cert";
	private static String KEYSTORE_PATH = "/resources/security/key.p12";
	
	public static void createKeys(LibertyServer... servers) {
		if (servers.length < 1) {
			throw new IllegalArgumentException();
		}

		String securityUtility = servers[0].getInstallRoot().concat("/bin/securityUtility");
		if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
			securityUtility = securityUtility + ".bat";
		}

		for (LibertyServer s : servers) {
			ArrayList<String> cmd = new ArrayList<String>();

			cmd.add(securityUtility);
			cmd.add("createSSLCertificate");
			cmd.add("--server="+s.getServerName());
			cmd.add("--password=password");

			try {
				Process proc = new ProcessBuilder(cmd).start();
				while (true) {
					try {
						if (proc.exitValue() == 0)
							Log.info(FATSecurityUtils.class, "createKeys", s.getServerRoot().concat(KEYSTORE_PATH) + " created");
						break;
					} catch (IllegalThreadStateException e) {
						Log.info(FATSecurityUtils.class, "createKeys", s.getServerRoot().concat(KEYSTORE_PATH) + " does not exist yet");
						Thread.sleep(1000);
					}
				}
				Log.info(FATSecurityUtils.class, "createKeys", "Certificate created for " + s.getServerName());
			} catch (Throwable t) {
				Log.error(FATSecurityUtils.class, "createKeys", t);
			}
		}
	}

	public static void extractPublicCertifcate(LibertyServer... servers) {
		ArrayList<String> cmd;

		for (LibertyServer s : servers) {
			cmd = new ArrayList<String>();

			cmd.add("keytool");
			cmd.add("-exportcert");
			cmd.add("-rfc");
			cmd.add("-keystore");
			cmd.add(s.getServerRoot()+KEYSTORE_PATH);
			cmd.add("-storepass");
			cmd.add("password");
			cmd.add("-storetype");
			cmd.add("PKCS12");
			cmd.add("-alias");
			cmd.add("default");
			cmd.add("-file");
			cmd.add(s.getServerRoot()+CERT_PATH);

			try {
				Process proc = new ProcessBuilder(cmd).redirectErrorStream(true).start();
				Log.info(FATSecurityUtils.class, "extractPublicCerts", "Running " + String.join(" ", cmd));
				try (InputStream is = proc.getInputStream()) {
					final BufferedReader reader =
							new BufferedReader(new InputStreamReader(is));
					String line;
					while ((line = reader.readLine()) != null) {
						Log.info(FATSecurityUtils.class, "extractPublicCerts", line);
					}
				}
			} catch (Throwable t) {
				Log.error(FATSecurityUtils.class, "extractPublicCerts", t);
			}
		}
	}

	public static void establishTrust(LibertyServer trusting, LibertyServer trusted) {
		try {
			ArrayList<String> cmd = new ArrayList<String>();

			cmd.add("keytool");
			cmd.add("-importcert");
			cmd.add("-noprompt");
			cmd.add("-file");
			cmd.add(trusted.getServerRoot()+CERT_PATH);
			cmd.add("-alias");
			cmd.add(trusted.getServerName());
			cmd.add("-keystore");
			cmd.add(trusting.getServerRoot()+KEYSTORE_PATH);
			cmd.add("-storepass");
			cmd.add("password");
			cmd.add("-storetype");
			cmd.add("PKCS12");

			Log.info(FATSecurityUtils.class, "establishTrust", "Running " + String.join(" ", cmd));
			Process proc = new ProcessBuilder(cmd).redirectErrorStream(true).start();
			try (InputStream is = proc.getInputStream()) {
				final BufferedReader reader =
						new BufferedReader(new InputStreamReader(is));
				String line;
				while ((line = reader.readLine()) != null) {
					Log.info(FATSecurityUtils.class, "establishTrust", line);
				}
			}
			
			cmd = new ArrayList<String>();
			cmd.add("keytool");
			cmd.add("-list");
			cmd.add("-keystore");
			cmd.add(trusting.getServerRoot()+KEYSTORE_PATH);
			cmd.add("-storepass");
			cmd.add("password");
			cmd.add("-storetype");
			cmd.add("PKCS12");
			
			Log.info(FATSecurityUtils.class, "establishTrust", "Running " + String.join(" ", cmd));
			proc = new ProcessBuilder(cmd).redirectErrorStream(true).start();
			try (InputStream is = proc.getInputStream()) {
				final BufferedReader reader =
						new BufferedReader(new InputStreamReader(is));
				String line;
				while ((line = reader.readLine()) != null) {
					Log.info(FATSecurityUtils.class, "establishTrust", line);
				}
			}
		} catch (Throwable e) {
			Log.error(FATSecurityUtils.class, "establishTrust", e);
		}
	}
}