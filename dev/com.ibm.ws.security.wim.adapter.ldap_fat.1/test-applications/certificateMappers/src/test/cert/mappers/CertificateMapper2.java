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
 * LDAP CertificateMapper instance that returns a LDAP filter.
 */
public class CertificateMapper2 implements X509CertificateMapper {

    private static final String CLASS_NAME = CertificateMapper2.class.getSimpleName();

    @Override
    public String mapCertificate(X509Certificate[] certificates) throws CertificateMapFailedException {

        if (certificates == null || certificates.length == 0 || certificates[0] == null) {
            throw new CertificateMapFailedException("No certificate was provided.");
        }

        LdapName dn;
        try {
            dn = new LdapName(certificates[0].getSubjectX500Principal().getName());
        } catch (InvalidNameException e) {
            throw new CertificateMapFailedException("The certificate's subject's X.500 principal was not in the form of a distinguished name.", e);
        }

        List<Rdn> rdns = dn.getRdns();
        Rdn rdn = rdns.get(rdns.size() - 1);

        String filter = "(" + rdn.toString() + ")";
        System.out.println(CLASS_NAME + ".mapCertificate(...) returns: " + filter);
        return filter;
    }
}
