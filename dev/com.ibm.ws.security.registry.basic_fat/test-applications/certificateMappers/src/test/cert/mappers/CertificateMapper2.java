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

import com.ibm.websphere.security.CertificateMapFailedException;
import com.ibm.websphere.security.CertificateMapNotSupportedException;
import com.ibm.websphere.security.CertificateMapper;

/**
 * CertificateMapper that throws a CertificateMapperNotSupportedException.
 */
public class CertificateMapper2 implements CertificateMapper {

    @Override
    public String getId() {
        return "mapper2";
    }

    @Override
    public String mapCertificate(X509Certificate certificate) throws CertificateMapNotSupportedException, CertificateMapFailedException {
        throw new CertificateMapNotSupportedException();
    }
}
