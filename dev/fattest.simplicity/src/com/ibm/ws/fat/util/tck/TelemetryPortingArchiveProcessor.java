/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.util.tck;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.shrinkwrap.api.asset.StringAsset;

/**
 * Open Telemetry requires implementors of the TCK to provide a porting jar
 */
public class TelemetryPortingArchiveProcessor extends AbstractArchiveWeaver {

    private static final String EXECUTOR_PROPERTY_NAME = "telemetry.tck.executor";
    private static final String PATH = "META-INF/microprofile-telemetry-tck.properties";

    @Override
    protected Set<Class<?>> getClassesToWeave() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(LibertyOpenTelemetryTCKExecutor.class);
        return classes;
    }

    @Override
    protected Map<String, StringAsset> getStringFilesToWeave() {
        HashMap<String, StringAsset> map = new HashMap<String, StringAsset>();
        StringAsset Stringasset = new StringAsset(EXECUTOR_PROPERTY_NAME + "=" + LibertyOpenTelemetryTCKExecutor.class.getName());
        map.put(PATH, Stringasset);
        return map;
    }

}