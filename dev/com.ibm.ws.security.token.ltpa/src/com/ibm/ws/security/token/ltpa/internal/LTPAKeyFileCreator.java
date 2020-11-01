/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.token.ltpa.internal;

import java.util.Properties;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.crypto.ltpakeyutil.LTPAKeyFileUtility;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;

/**
 * Utility class to create the LTPA keys file.
 */
public interface LTPAKeyFileCreator extends LTPAKeyFileUtility {

    /**
     * Create the LTPA keys file at the specified location using
     * the specified password bytes.
     * <p>
     * Access the keyFile using the WsLocationAdmin
     *
     * @param locService
     * @param keyFile
     * @param keyPasswordBytes
     * @param realmName
     * @return A Properties object containing the various attributes created for the LTPA keys
     * @throws Exception
     */
    public Properties createLTPAKeysFile(WsLocationAdmin locService, String keyFile, @Sensitive byte[] keyPasswordBytes, String realmName) throws Exception;

}