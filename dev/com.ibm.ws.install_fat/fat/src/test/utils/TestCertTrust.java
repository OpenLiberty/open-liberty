package test.utils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class TestCertTrust {

	public static void trustAll () { 
        HostnameVerifier trustAll = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
            	System.out.println("Hostname verifier callback asked to verify hostname of "+hostname);
            	System.out.println("Hostname verifier hardcoded to return true. (trusting every hostname)");
                return true;
            }
        };	
		HttpsURLConnection.setDefaultHostnameVerifier(trustAll);
	}
	
	public static void disableSNIExtension () {
		System.setProperty("jsse.enableSNIExtension","false");
	}
}
