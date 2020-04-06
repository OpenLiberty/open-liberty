/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

/**
 * This class was imported from tWAS to make only those changes necessary to 
 * run OAuth on Liberty. The mission was not to refactor, restructure, or 
 * generally cleanup the code. 
 */
package com.ibm.ws.security.oauth20.platform;

import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.exception.OAuthProviderException;

public class LibertyPlatformService implements PlatformService {
    private static TraceComponent tc = Tr.register(
            LibertyPlatformService.class, "OAuth20Provider",
            "com.ibm.ws.security.oauth20.resources.ProviderMsgs");

    private static String configFileDir = null;
    private static String serverDirKey = "server.config.dir";

    public void init() {
        // nothing needed for now
    }

    public boolean skipInit() {
        return false;
    }

    public boolean isDistributedCapable() {
        return false;
    }

    public String getRewrite(String key) throws OAuthProviderException {
        // is this needed for liberty?
        throw new OAuthProviderException(new UnsupportedOperationException());
    }

    public String getConfigFolder() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getConfigFolder");

        if (configFileDir == null) {
            String base =
                    AccessController.doPrivileged(new PrivilegedAction<String>() {
                        public String run() {
                            return System.getProperty(serverDirKey);
                        }
                    });
            if (base == null) {
                Tr.error(tc, "no property SERVER_CONFIG_DIR");
            } else {
                File oauth20dir = new File(configFileDir, "oauth20");
                configFileDir = oauth20dir.getAbsolutePath();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "OAuthConfigFileDir: " + configFileDir);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getConfigFolder");

        return configFileDir;
    }

    public <K, V> Map<K, V> getDistributedMap(String jndiName, final K[] arg0, final V[] arg1) {
        // no cluster, never needed
        throw new UnsupportedOperationException();
    }

}
