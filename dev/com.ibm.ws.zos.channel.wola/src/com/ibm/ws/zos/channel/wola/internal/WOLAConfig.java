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
package com.ibm.ws.zos.channel.wola.internal;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * DS component, encapsulates WOLA config (<zosLocalAdapters>).
 */
public class WOLAConfig {

    /**
     * Attribute keys.
     */
    final protected static String WOLA_GROUP_KEY = "wolaGroup";
    final protected static String WOLA_NAME2_KEY = "wolaName2";
    final protected static String WOLA_NAME3_KEY = "wolaName3";
    final protected static String WOLA_USE_CICS_TASK_USER_ID_KEY = "useCicsTaskUserId";
    private static final TraceComponent tc = Tr.register(WOLAConfig.class);

    /**
     * The config map.
     */
    Map<String, Object> config;

    /**
     * DS method.
     */
    protected void activate(Map<String, Object> config) {
        // cache config to preserve upcasing of group name
        this.config = new HashMap<String, Object>(config);
        enforceUppercaseNames();
    }

    /**
     * check case
     * issue warning
     * force upper
     */
    private void enforceUppercaseNames() {
        String[] keys = new String[] { WOLA_GROUP_KEY,
                                       WOLA_NAME2_KEY,
                                       WOLA_NAME3_KEY };
        for (String key : keys) {
            String val = (String) config.get(key);
            String upVal = val.toUpperCase();
            if (!val.equals(upVal)) {
                Tr.warning(tc, "WOLA_LOWERCASE_GROUP_NAME", key, val, upVal);
                config.put(key, upVal);
            }
        }
    }

    /**
     * @return the configured wolaGroup.
     */
    protected String getWolaGroup() {
        return (String) config.get(WOLA_GROUP_KEY);
    }

    /**
     * @return the configured 2nd part of the WOLA name.
     */
    protected String getWolaName2() {
        return (String) config.get(WOLA_NAME2_KEY);
    }

    /**
     * @return the configured 2nd part of the WOLA name.
     */
    protected String getWolaName3() {
        return (String) config.get(WOLA_NAME3_KEY);
    }

    /**
     * @return true if we are allowed to propagate the CICS task user ID to this region.
     */
    protected boolean allowCicsTaskUserIdPropagation() {
        return (Boolean) config.get(WOLA_USE_CICS_TASK_USER_ID_KEY);
    }
}
