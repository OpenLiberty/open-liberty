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

import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class PlatformServiceFactory {

    private static TraceComponent tc = Tr.register(
            PlatformServiceFactory.class, "OAuth20Provider",
            "com.ibm.ws.security.oauth20.resources.ProviderMsgs");

    static PlatformService platform = null;

    protected static final String WAS_CLASS = "com.ibm.websphere.cache.DistributedMap";
    protected static final String TEST_PROP = "OAUTH_UNIT_TEST";
    protected static final String LIBERTY_PROP = "server.config.dir";

    public static PlatformService getPlatformService() {

        if (tc.isEntryEnabled())
            Tr.entry(tc, "getPlatformService");

        if (platform == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Checking platform type");

            if (tryProp(TEST_PROP)) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Checking platform type");
                platform = new TestPlatformService();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "found prop " + TEST_PROP);
            } else if (tryProp(LIBERTY_PROP)) {
                platform = new LibertyPlatformService();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "found prop " + LIBERTY_PROP);
            } else if (tryClass(WAS_CLASS)) {
                platform = new WASPlatformService();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "found class " + WAS_CLASS);
            } else {
                // defaulting to WAS
                platform = new WASPlatformService();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "nothing found, default platform");
            }
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Platform: " + platform.getClass().getName());
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getPlatformService");

        return platform;
    }

    private static boolean tryClass(String className) {
        try {
            Class.forName(className);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Found class name");
            // it worked
            return true;
        } catch (Throwable e) {
            // not found
            return false;
        }
    }

    private static boolean tryProp(final String prop) {
        String value =
                AccessController.doPrivileged(new PrivilegedAction<String>() {
                    public String run() {
                        return System.getProperty(prop);
                    }
                });
        if (tc.isDebugEnabled())
            Tr.debug(tc, "tryProp", "value: " + value);
        return value != null;
    }

}
