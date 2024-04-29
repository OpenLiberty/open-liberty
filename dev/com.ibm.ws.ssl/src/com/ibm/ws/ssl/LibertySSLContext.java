/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.ssl;

import java.security.Provider;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;

/**
 * Backed by {@link LibertySSLContextSpi} to return a
 * {@link LibertySSLSocketFactoryWrapper} from {@code getSocketFactory()}
 */
public class LibertySSLContext extends SSLContext {

    /**
     * @param contextSpi
     * @param provider
     * @param protocol
     */
    public LibertySSLContext(SSLContextSpi contextSpi, Provider provider, String protocol) {
        super(contextSpi, provider, protocol);
    }
}
