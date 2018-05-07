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

import com.ibm.websphere.security.CertificateMapNotSupportedException;
import com.ibm.websphere.security.X509CertificateMapper;

/**
 * CertificateMapper that throws a CertificateMapperNotSupportedException.
 */
public class CertificateMapper3 implements X509CertificateMapper {

    @Override
    public String mapCertificate(X509Certificate[] certificates) throws CertificateMapNotSupportedException {
        throw new CertificateMapNotSupportedException();
    }
}
