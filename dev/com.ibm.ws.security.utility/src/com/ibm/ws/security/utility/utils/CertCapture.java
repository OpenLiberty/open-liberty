package com.ibm.ws.security.utility.utils;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class CertCapture {

    public static X509Certificate[] retrieveCertificates(String host, int port) {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            CapturingTrustManager tm = new CapturingTrustManager();
            ctx.init(null, new TrustManager[]{tm}, null);
            SSLSocket soc = (SSLSocket) ctx.getSocketFactory().createSocket(host, port);
            soc.startHandshake();
            soc.close();
            return tm.getAcceptedIssuers();
        } catch (NoSuchAlgorithmException | KeyManagementException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class CapturingTrustManager extends X509ExtendedTrustManager {
        private X509Certificate[] capturedCerts;

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
            capturedCerts = new X509Certificate[chain.length];
            System.arraycopy(chain, 0, capturedCerts, 0, chain.length);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {

        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return capturedCerts;
        }
    }
}
