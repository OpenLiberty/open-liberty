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
package com.ibm.ws.crypto.certificate.creator.acme;

import java.io.File;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateCreator;
import com.ibm.ws.security.acme.AcmeProvider;

/**
 * A {@link DefaultSSLCertificateCreator} OSGi service that will create a default
 * certificate using the ACME 2.0 protocol.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class AcmeSSLCertificateCreator implements DefaultSSLCertificateCreator {

    private static final TraceComponent tc = Tr.register(AcmeSSLCertificateCreator.class);

    /** Reference to the AcmeProvider service. */
    private static final AtomicReference<AcmeProvider> acmeProviderRef = new AtomicReference<AcmeProvider>();

    @Override
    public File createDefaultSSLCertificate(String filePath, @Sensitive String password, String keyStoreType, String keyStoreProvider, int validity, String subjectDN, int keySize,
                                            String sigAlg,
                                            List<String> extInfo) throws CertificateException {
        return getAcmeProvider().createDefaultSSLCertificate(filePath, password, keyStoreType, keyStoreProvider);
    }

    @Override
    public void updateDefaultSSLCertificate(KeyStore keyStore, File keyStoreFile, @Sensitive String password) throws CertificateException {
        getAcmeProvider().updateDefaultSSLCertificate(keyStore, keyStoreFile, password);
    }

    @Override
    public String getType() {
        return TYPE_ACME;
    }

    /**
     * Convenience method that will retrieve the {@link AcmeProvider}
     * instance or throw an {@link CertificateException} if the
     * {@link AcmeProvider} is null.
     *
     * @return The {@link AcmeProvider} instance to use.
     * @throws CertificateException
     *             If the {@link AcmeProvider} instance is null.
     */
    @Trivial
    private AcmeProvider getAcmeProvider() throws CertificateException {
        if (acmeProviderRef.get() == null) {
            throw new CertificateException("Internal error. AcmeProvider was not registered.");
        }
        return acmeProviderRef.get();
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC)
    protected void setAcmeProvider(AcmeProvider acmeProvider) {
        acmeProviderRef.set(acmeProvider);
    }

    protected void unsetAcmeProvider(AcmeProvider acmeProvider) {
        acmeProviderRef.compareAndSet(acmeProvider, null);
    }
}
