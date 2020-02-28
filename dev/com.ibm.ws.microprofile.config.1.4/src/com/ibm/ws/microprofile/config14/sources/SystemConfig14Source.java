/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.sources;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.microprofile.config.sources.SystemConfigSource;

/**
 *
 */
public class SystemConfig14Source extends SystemConfigSource {

    private static long DELAY = 500;

    private Map<String, String> systemPropertyMap = new HashMap<>();

    public SystemConfig14Source(ScheduledExecutorService scheduledExecutorService) {
        super();
        ScheduledExecutorService executor = scheduledExecutorService;
        if (executor == null) {
            executor = Executors.newScheduledThreadPool(1);
        }

        executor.scheduleAtFixedRate(() -> updateSystemPropertyMap(), 0, DELAY, TimeUnit.MILLISECONDS);
    }

    public void updateSystemPropertyMap() {
        HashMap<String, String> props = new HashMap<>();
        Properties sysProps = SystemConfigSource.getSystemProperties();
        Set<String> keys = sysProps.stringPropertyNames();
        for (String key : keys) {
            if (key != null) {
                String value = sysProps.getProperty(key);
                if (value != null) { //it is possible that a property could be removed while we are looking at them
                    props.put(key, value);
                }
            }
        }
        systemPropertyMap = props;
    }

    @Override
    public Map<String, String> getProperties() {
        return systemPropertyMap;
    }
}
