/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ssl.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * CertificateEnvHelper
 * <p>
 * This class handles getting certificate from an environment variable
 * </p>
 *
 */
public class CertificateEnvHelper {
    private static final TraceComponent tc = Tr.register(CertificateEnvHelper.class, "SSL", "com.ibm.ws.ssl.resources.ssl");

    /**
     * Constructor.
     */
    public CertificateEnvHelper() {
        // do nothing
    }

    public List<Certificate> getCertificatesForKeyStore(String keyStoreName) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "getCertificatesForKeyStore", new Object[] { keyStoreName });
        }

        String envKey = "cert_" + keyStoreName;
        InputStream inputStream = null;
        List<Certificate> envCerts = new ArrayList<Certificate>();

        String envVal = System.getenv(envKey);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.debug(this, tc, "value return from the environment for " + envKey + " is " + envVal);
        }
        if (envVal != null && !envVal.isEmpty()) {
            try {
                // get the inputStream
                inputStream = getInputStreamForCert(envVal);

                // create cert
                CertificateFactory factory = CertificateFactory.getInstance("X.509");
                Collection<? extends Certificate> certs = factory.generateCertificates(inputStream);

                for (Certificate cert : certs) {
                    envCerts.add(cert);
                }

            } catch (Exception e) {
                String extendedMsg = e.getMessage();
                Tr.warning(tc, "ssl.environment.cert.error.CWPKI0826W", envKey, keyStoreName, extendedMsg);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(this, tc, "getCertificatesForKeyStore", new Object[] { envCerts });
        }
        return envCerts;
    }

    /**
     * @param env_Value
     * @return
     * @throws FileNotFoundException
     */
    private InputStream getInputStreamForCert(String env_Value) throws Exception {

        String certBeginTag = "-----BEGIN CERTIFICATE-----";
        InputStream inputStream = null;

        // If env_Value starts with the certificate tag assume a certificate otherwise treat like a file
        if (env_Value.startsWith(certBeginTag)) {
            inputStream = new ByteArrayInputStream(env_Value.getBytes(Charset.forName("UTF-8")));
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.debug(this, tc, "The value from the environment did not start with -----BEGIN CERTIFICATE----- so treating it like a file");
            }

            File certFile = new File(env_Value);
            if (certFile.isFile()) {
                inputStream = new FileInputStream(certFile);
            } else {
                throw new Exception("A valid file or certificate is not specified on the environment variable.");
            }
        }

        return inputStream;
    }

}
