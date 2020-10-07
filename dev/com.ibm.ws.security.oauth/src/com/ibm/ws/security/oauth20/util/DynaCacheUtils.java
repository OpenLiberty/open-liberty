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

package com.ibm.ws.security.oauth20.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.security.oauth20.platform.PlatformServiceFactory;

@SuppressWarnings("unchecked")
public class DynaCacheUtils {

    private static TraceComponent tc = Tr.register(DynaCacheUtils.class,
            "OAuth20Provider",
            "com.ibm.ws.security.oauth20.resources.ProviderMsgs");

    // Static for the locally generated HashMaps, we want them shared in the
    // server
    protected final static HashMap<String, Map> dynaCaches = new HashMap<String, Map>();
    protected final static HashMap<String, Map> serverCaches = new HashMap<String, Map>();

    /*-
    private Logger logger = Logger.getLogger(DynaCacheUtils.class.getName());
    private ResourceBundle resBundle = ResourceBundle.getBundle(
            Constants.RESOURCE_BUNDLE, Locale.getDefault());
     */

    public static <K, V> Map<K, V> getDynamicCache(final String jndiName,
            final K[] arg0,
            final V[] arg1) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getDynamicCache");

        Map<K, V> result = null;

        result = dynaCaches.get(jndiName);

        if (result == null) {
            result = serverCaches.get(jndiName);
        }

        if (result == null) {
            if (PlatformServiceFactory.getPlatformService()
                    .isDistributedCapable()) {
                result = PlatformServiceFactory.getPlatformService()
                        .getDistributedMap(jndiName, arg0, arg1);
                if (result != null) {
                    dynaCaches.put(jndiName, result);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Found dynacache for: " + jndiName);
                }
            }
        }

        if (result == null) {
            result =
                    Collections.synchronizedMap(new BoundedCache<K, V>());
            serverCaches.put(jndiName, result);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "No dynacache, generated BoundCache for:" + jndiName);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getDynamicCache");

        return result;
    }
}
