/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package test.cert.mappers;

import java.security.cert.X509Certificate;

import com.ibm.websphere.security.X509CertificateMapper;

/**
 * CertificateMapper that returns a null result.
 */
public class CertificateMapper5 implements X509CertificateMapper {

    @Override
    public String mapCertificate(X509Certificate[] certificates) {
        return null;
    }
}
