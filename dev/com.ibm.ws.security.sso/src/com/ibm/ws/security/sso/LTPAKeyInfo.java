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
package com.ibm.ws.security.sso;

import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

public interface LTPAKeyInfo {

    public void prepareLTPAKeyInfo(WsLocationAdmin locService, String keyImportFile, byte[] keyPassword) throws Exception;

    /**
     * Get the LTPA secret key.
     *
     * @param keyImportFile The URL of the key import file. If it's not the URL, it is assumed as a relative path from
     *            ${App.root}/config
     * @return The LTPA secret key
     */
    public byte[] getSecretKey(String keyImportFile);

    /**
     * Get the LTPA private key.
     *
     * @param keyImportFile The URL of the key import file. If it's not the URL, it is assumed as a relative path from
     *            ${App.root}/config
     * @return The LTPA private key
     */
    public byte[] getPrivateKey(String keyImportFile);

    /**
     * Get the LTPA public key.
     *
     * @param keyImportFile The URL of the key import file. If it's not the URL, it is assumed as a relative path from
     *            ${App.root}/config
     * @return The LTPA public key
     */
    public byte[] getPublicKey(String keyImportFile);

}
