/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.internal;

import javax.net.ssl.SSLSocketFactory;

/**
 * Interface for creating a SSLSocketFactory from SSLConfiguration.
 */
public interface SSLHelper {
    /**
     * Get an SSLSocketFactory for the SSLConfiguration with the specified id.
     * 
     * @param sslConfigID id of a sslConfiguration element.
     * @return SSLSocketFactory for the SSLConfiguration with the specified id.
     * @throws Exception if an error occurs.
     */
    SSLSocketFactory getSSLSocketFactory(String sslConfigID) throws Exception;
}
