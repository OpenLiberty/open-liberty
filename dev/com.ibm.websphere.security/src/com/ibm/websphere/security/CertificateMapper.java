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

package com.ibm.websphere.security;

import java.security.cert.X509Certificate;

/**
 * Interface for custom X.509 certificate mapping. Implementing classes are required to define
 * a zero-argument constructor so that they can be instantiated during loading.
 */
public interface CertificateMapper {

    /**
     * Get the ID of this {@link CertificateMapper}. The ID must be non-null and unique identifier
     * for all {@link CertificateMapper} implementations.
     *
     * @return The ID.
     */
    public String getId();

    /**
     *
     * Map the X.509 certificate. Implementations of this method must be thread-safe.
     *
     * <p/>A {@link CertificateMapper} for an LDAP registry should return a string that is one of either:
     * <ol>
     * <li>a distinguished name (DN). For example: uid=user1,o=ibm,c=us</li>
     * <li>an LDAP search filter surrounded by parenthesis. For example: (uid=user1)</li>
     * </ol>
     *
     * <p/>A {@link CertificateMapper} for a basic registry should return a string that corresponds
     * to the user's name in the registry. For example: user1
     *
     * @param certificate The certificate containing the certificate to map.
     * @return The registry specific string returned used by the
     *         repository to search for the user.
     * @throws CertificateMapNotSupportedException If certificate
     *             mapping is not supported.
     * @throws CertificateMapFailedException If the certificate
     *             could not be mapped.
     */
    public String mapCertificate(X509Certificate certificate) throws CertificateMapNotSupportedException, CertificateMapFailedException;
}
