/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.jwt.servers;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.servers.ServerInstanceUtils;

import componenttest.topology.impl.LibertyServer;

public class JwtServerInstanceUtils extends ServerInstanceUtils {
    private final static Class<?> thisClass = JwtServerInstanceUtils.class;

    // method should be called for any server that could be using PSS algorithms
    public static void addEncryptionSettingToBootstrap(LibertyServer server, boolean encryptIt) {
        String thisMethod = "addEncryptionSettingToBootstrap";

        String encrypt_fileName_extension = null;
        if (encryptIt) {
            encrypt_fileName_extension = "_withEncryption";
        } else {
            encrypt_fileName_extension = "";
        }
        try {
            // Some tests need to know if PSS Algs can be used - set a property in bootstrap that can be
            // used as part of a key/trust file name
            bootstrapUtils.writeBootstrapProperty(server, JwtConstants.BOOTSTRAP_PROP_ENCRYPTION_SETTING, encrypt_fileName_extension);
        } catch (Exception e) {
            e.printStackTrace();
            Log.info(thisClass, thisMethod, "Setup failed to add encryption file name extension info to bootstrap.properties");
        }
    }
}
