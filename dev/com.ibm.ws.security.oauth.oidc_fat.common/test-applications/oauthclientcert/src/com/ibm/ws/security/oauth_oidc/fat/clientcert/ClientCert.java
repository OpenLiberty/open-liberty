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
package com.ibm.ws.security.oauth_oidc.fat.clientcert;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLSocketFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class ClientCert {

    public static SSLSocketFactory getClientCertSocketFactory( String ksPath,
                                        String ksPassword,
                                        String ksType) {
        String thisMethod = "getClientCertSocketFactory:";

        System.out.println(thisMethod);
        try {
            KeyManager keyManagers[] = null;

            System.out.println("setup Keymanager" +
                               "ksPath=" + ksPath + " ksPassword=" + ksPassword + " ksType=" + ksType); 
            KeyManagerFactory kmFactory = KeyManagerFactory
                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());

            File ksFile = new File(ksPath);
            KeyStore keyStore = KeyStore.getInstance(ksType);
            FileInputStream ksStream = new FileInputStream(ksFile);
            keyStore.load(ksStream, ksPassword.toCharArray());

            kmFactory.init(keyStore, ksPassword.toCharArray());
            keyManagers = kmFactory.getKeyManagers();                   

            // Create a trust manager that does not validate certificate chains
            /* */
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs,
                        String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs,
                        String authType) {
                }
            } };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(keyManagers, trustAllCerts, new java.security.SecureRandom());
            return sc.getSocketFactory();
        } catch (Exception e) {
            System.out.println( "Unable to set default TrustManager becaise of " + e);
        }
        return null;
    }
    //// for future usage (to define a TrsutManager with a TrustStore)
    //protected TrustManager[] getTrustManagers() throws IOException, GeneralSecurityException    {
    //    String alg=TrustManagerFactory.getDefaultAlgorithm();
    //    TrustManagerFactory tmFact=TrustManagerFactory.getInstance(alg);
    //    FileInputStream fis=new FileInputStream(getTrustStore());
    //    KeyStore ks=KeyStore.getInstance("jks");
    //    ks.load(fis, getTrustStorePassword().toCharArray());
    //    fis.close();
    //    tmFact.init(ks);
    //    TrustManager[] tms=tmFact.getTrustManagers();
    //    return tms;
    //}

}
