package com.ibm.ws.transaction.fat.util;

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
		for (LibertyServer s : servers) {
			ArrayList<String> cmd = new ArrayList<String>();

			cmd.add("keytool");
			cmd.add("-exportcert");
			cmd.add("-rfc");
			cmd.add("-keystore");
			cmd.add(s.getServerRoot()+KEYSTORE_PATH);
			cmd.add("-storepass");
			cmd.add("password");
			cmd.add("-alias");
			cmd.add("default");
			cmd.add("-file");
			cmd.add(s.getServerRoot()+CERT_PATH);

			try {
				Process proc = new ProcessBuilder(cmd).start();
				while (true) {
					try {
						if (proc.exitValue() == 0) 
							Log.info(FATSecurityUtils.class, "extractPublicCerts", s.getServerRoot().concat(CERT_PATH) + " extracted");
						break;
					} catch (IllegalThreadStateException e) {
						Log.info(FATSecurityUtils.class, "extractPublicCerts", s.getServerRoot().concat(CERT_PATH) + " not extracted yet");
						Thread.sleep(1000);
					}
				}
				Log.info(FATSecurityUtils.class, "extractPublicCerts", "Certificate exported for " + s.getServerName());
			} catch (Throwable t) {
				Log.error(FATSecurityUtils.class, "extractPublicCerts", t);
			}
		}
	}

	public static void establishTrust(LibertyServer trusting, LibertyServer trusted) {
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

		try {
			Log.info(FATSecurityUtils.class, "establishTrust", "Running keytool");
			Process proc = new ProcessBuilder(cmd).start();
			while (true) {
				try {
					Log.info(FATSecurityUtils.class, proc.exitValue() + " establishTrust", trusting.getServerName() + " now trusts " + trusted.getServerName());
					break;
				} catch (IllegalThreadStateException e) {
					Log.info(FATSecurityUtils.class, "establishTrust", "keytool -importcert..... has not finished yet");
					Thread.sleep(1000);
				}
			}
		} catch (Throwable e) {
			Log.error(FATSecurityUtils.class, "establishTrust", e);
		}
	}
}