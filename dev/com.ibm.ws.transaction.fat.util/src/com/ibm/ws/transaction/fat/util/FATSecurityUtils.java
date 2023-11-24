package com.ibm.ws.transaction.fat.util;

import java.io.IOException;
import java.util.ArrayList;

import componenttest.topology.impl.LibertyServer;

public class FATSecurityUtils {
	private static String KEYSTORE_PATH = "/resources/security/bey.p12";
	
	static void createKeyStore(LibertyServer... servers) throws IOException {
		ArrayList<String> cmd = new ArrayList<String>();
		
		for (LibertyServer s : servers) {
			try {
				s.deleteFileFromLibertyServerRoot(KEYSTORE_PATH);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// keytool -genkey -alias <server name> -keystore WSATSSL_Server2.jks -keyalg RSA -validity 3650
			cmd.add(getAbsoluteKeytoolPath());
			cmd.add("-genkey");
			cmd.add("-alias");
			cmd.add(s.getServerName());
			cmd.add("-keystore");
			cmd.add(s.getServerRoot()+KEYSTORE_PATH);
			cmd.add("-keyalg");
			cmd.add("PKCS12");
			cmd.add("-validity");
			cmd.add("2");

			Process proc = Runtime.getRuntime().exec(cmd.toArray(new String[cmd.size()]));
		}
	}

	static String getAbsoluteKeytoolPath() {
		String javaHome = System.getProperty("java.home");
		String keytool = javaHome + "/bin/keytool";
		if (System.getProperty("os.name").toLowerCase().startsWith("win")) {
			keytool = keytool + ".exe";
		}
		return keytool;
	}
}