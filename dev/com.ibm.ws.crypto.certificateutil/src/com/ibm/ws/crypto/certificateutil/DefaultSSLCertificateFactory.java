package com.ibm.ws.crypto.certificateutil;

import com.ibm.ws.crypto.certificateutil.keytool.KeytoolSSLCertificateCreator;

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

/**
 * Extension point for future enhancement, specifically creating the
 * certificate using IBM-JVM APIs, which do not exist on other JVMs.
 */
public class DefaultSSLCertificateFactory {
    private static DefaultSSLCertificateCreator creator = new KeytoolSSLCertificateCreator();

    /**
     * Returns an implementation of a DefaultSSLCertificateCreator.
     * 
     * @return
     */
    public static DefaultSSLCertificateCreator getDefaultSSLCertificateCreator() {
        return creator;
    }

    /**
     * Controls which implementation of DefaultSSLCertificateCreator to return.
     * 
     * @param obj
     */
    public static void setDefaultSSLCertificateCreator(DefaultSSLCertificateCreator creator) {
        DefaultSSLCertificateFactory.creator = creator;
    }
}
