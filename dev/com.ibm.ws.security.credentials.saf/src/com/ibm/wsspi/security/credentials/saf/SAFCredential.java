/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.credentials.saf;

import java.security.cert.X509Certificate;

/**
 * Java representation for a native SAF credential (i.e an ACEE).
 *
 * @author IBM Corporation
 * @version 1.2
 * @ibm-spi
 */
public interface SAFCredential {

    /**
     * SAFCredential types.
     */
    public enum Type {
        ASSERTED,
        BASIC,
        DEFAULT,
        CERTIFICATE,
        SERVER,
        MAPPED
    };

    /**
     * Return the user id associated with this SAFCredential.
     * If the distributed user id was mapped, the mapped
     * user id is returned.
     *
     * @return The user id.
     */
    public String getUserId();

    /**
     * Retrieve the audit string used when authenticating this SAFCredential.
     *
     * @return The audit string.
     */
    public String getAuditString();

    /**
     * Retrive the certificate used when authenticating this SAFCredential.
     *
     * @return The certificate.
     */
    public X509Certificate getCertificate();

    /**
     * Retrieve the credential type.
     *
     * @return The credential type.
     */
    public Type getType();

    /**
     * Returns whether or not the SAFCredential has been authenticated.
     *
     * @return true if the SAFCredential has been authenticated; false otherwise.
     */
    public boolean isAuthenticated();

    /**
     * Retrieve the MVS user id this SAFCredential was mapped to.
     * If the user id was not mapped, the unmapped
     * user id is returned.
     *
     * @return The user name.
     */
    String getMvsUserId();

    /**
     * Return the distributed user id associated with this SAFCredential.
     *
     * @return The distributed user id.
     */
    String getDistributedUserId();

    /**
     * Retrieve the realm associated with this SAFCredential.
     *
     * @return The realm.
     */
    String getRealm();

}
