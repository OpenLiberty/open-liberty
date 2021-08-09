/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package test.cert.mappers;

import java.security.cert.X509Certificate;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import com.ibm.websphere.security.CertificateMapFailedException;
import com.ibm.websphere.security.X509CertificateMapper;

/**
 * CertificateMapper that processes a certificate chain.
 */
public class CertificateMapper4 implements X509CertificateMapper {
    private static final String CLASS_NAME = CertificateMapper4.class.getSimpleName();

    @Override
    public String mapCertificate(X509Certificate[] certificates) throws CertificateMapFailedException {

        if (certificates == null || certificates.length != 2) {
            throw new CertificateMapFailedException("The expected certificate chain was not provided.");
        }

        X509Certificate subject = certificates[0];
        X509Certificate intermediate = certificates[1];

        /*
         * Do some simple (non-sensical) checks on the certificate chain.
         */
        String issuer = subject.getIssuerDN().getName();
        if (!("cn=chain_ca".equalsIgnoreCase(issuer))) {
            throw new CertificateMapFailedException("Subject's certificate was signed by unknown CA: " + issuer);
        }
        issuer = intermediate.getIssuerDN().getName();
        if (!("cn=chain_root".equalsIgnoreCase(intermediate.getIssuerDN().getName()))) {
            throw new CertificateMapFailedException("CA's certificate was signed by unknown root: " + issuer);
        }

        LdapName dn;
        try {
            dn = new LdapName(certificates[0].getSubjectX500Principal().getName());
        } catch (InvalidNameException e) {
            throw new CertificateMapFailedException("The certificate's subject's X.500 principal was not in the form of a distinguished name.", e);
        }

        List<Rdn> rdns = dn.getRdns();
        Rdn rdn = rdns.get(rdns.size() - 1);

        String name = (String) rdn.getValue();
        System.out.println(CLASS_NAME + ".mapCertificate(...) returns: " + name);
        return name;
    }
}
