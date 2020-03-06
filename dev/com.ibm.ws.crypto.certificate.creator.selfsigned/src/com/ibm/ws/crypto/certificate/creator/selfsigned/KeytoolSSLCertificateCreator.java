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
package com.ibm.ws.crypto.certificate.creator.selfsigned;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.crypto.certificateutil.DefaultSSLCertificateCreator;

/**
 * A {@link DefaultSSLCertificateCreator} OSGi service that will create a default
 * certificate using keytool. The the resulting certificate is self-signed.
 *
 * <p/>This class is merely a OSGi service wrapper for the
 * {@link com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator} class.
 */
@Component(service = DefaultSSLCertificateCreator.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class KeytoolSSLCertificateCreator extends com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator {

    public KeytoolSSLCertificateCreator() {
        super();
    }
}
