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

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import com.ibm.websphere.security.CertificateMapFailedException;
import com.ibm.websphere.security.CertificateMapNotSupportedException;
import com.ibm.websphere.security.CertificateMapper;

/**
 * LDAP CertificateMapper instance that returns a DN.
 */
public class CertificateMapper1 implements CertificateMapper {

    private static final String CLASS_NAME = CertificateMapper1.class.getSimpleName();

    @Override
    public String getId() {
        return "mapper1";
    }

    @Override
    public String mapCertificate(X509Certificate certificate) throws CertificateMapNotSupportedException, CertificateMapFailedException {
        if (certificate == null) {
            throw new CertificateMapFailedException("Certificate is null.");
        }

        LdapName dn;
        try {
            dn = new LdapName(certificate.getSubjectX500Principal().getName());
        } catch (InvalidNameException e) {
            throw new CertificateMapFailedException("The certificate's subject's X.500 principal was not in the form of a distinguished name.", e);
        }

        System.out.println(CLASS_NAME + ".mapCertificate(...) returns: " + dn);
        return dn.toString();
    }
}
