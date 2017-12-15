/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.common.util;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;

/**
 *
 */
public final class SystemPropertyAction implements PrivilegedAction<String> {
    private static final Logger LOG = LogUtils.getL7dLogger(SystemPropertyAction.class);
    private final String property;
    private final String def;
    private SystemPropertyAction(String name) {
        property = name;
        def = null;
    }
    private SystemPropertyAction(String name, String d) {
        property = name;
        def = d;
    }

    /* (non-Javadoc)
     * @see java.security.PrivilegedAction#run()
     */
    public String run() {
        if (def != null) {
            return System.getProperty(property, def);
        }
        return System.getProperty(property);
    }

    public static String getProperty(String name) {
        return AccessController.doPrivileged(new SystemPropertyAction(name));
    }

    public static String getProperty(String name, String def) {
        try {
            return AccessController.doPrivileged(new SystemPropertyAction(name, def));
        } catch (SecurityException ex) {
            LOG.log(Level.FINE, "SecurityException raised getting property " + name, ex);
            return def;
        }
    }

    /**
     * Get the system property via the AccessController, but if a SecurityException is
     * raised, just return null;
     * @param name
     */
    public static String getPropertyOrNull(String name) {
        try {
            return AccessController.doPrivileged(new SystemPropertyAction(name));
        } catch (SecurityException ex) {
            LOG.log(Level.FINE, "SecurityException raised getting property " + name, ex);
            return null;
        }
    }

    public static int getInteger(String name, int def) {
        try {
            return AccessController.doPrivileged(new PrivilegedAction<Integer>() {
                @Override
                public Integer run() {
                    return Integer.getInteger(name, def);
                } });
        } catch (SecurityException ex) {
            LOG.log(Level.FINE, "SecurityException raised getting property " + name, ex);
            return def;
        }
    }
}
