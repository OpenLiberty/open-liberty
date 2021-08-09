/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.crypto.certificateutil;

import com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator;

/**
 * Extension point for future enhancement, specifically creating the
 * certificate using IBM-JVM APIs, which do not exist on other JVMs.
 */
public class DefaultSSLCertificateFactory {

    /**
     * The default certificate creator. This is necessary for scripts that use this factory that are
     * not running under OSGi (i.e. - securityUtility).
     */
    private static final DefaultSSLCertificateCreator DEFAULT_CERTIFICATE_CREATOR = new KeytoolSSLCertificateCreator();

    private static DefaultSSLCertificateCreator certificateCreator = null;

    /**
     * Returns an implementation of a {@link DefaultSSLCertificateCreator}.
     *
     * @return The {@link DefaultSSLCertificateCreator} that was set in {@link #setDefaultSSLCertificateCreator(DefaultSSLCertificateCreator)}.
     */
    public static DefaultSSLCertificateCreator getDefaultSSLCertificateCreator() {
        return (certificateCreator == null) ? DEFAULT_CERTIFICATE_CREATOR : certificateCreator;
    }

    /**
     * Controls which implementation of {@link DefaultSSLCertificateCreator} to return.
     *
     * @param creator A {@link DefaultSSLCertificateCreator} instance.
     */
    public static void setDefaultSSLCertificateCreator(DefaultSSLCertificateCreator creator) {
        DefaultSSLCertificateFactory.certificateCreator = creator;
    }
}
