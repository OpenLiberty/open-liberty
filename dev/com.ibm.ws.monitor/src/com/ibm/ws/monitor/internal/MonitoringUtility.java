/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.monitor.internal;

import java.util.Collection;
import java.util.HashSet;

import org.osgi.framework.Bundle;

/**
 *
 */
public class MonitoringUtility {
    public static Collection<Class<?>> loadMonitoringClasses(Bundle bundle) {
        Collection<Class<?>> classes = new HashSet<Class<?>>();
        String header = bundle.getHeaders("").get("Liberty-Monitoring-Components");
        String[] classNames = header.split("[,\\s]");

        for (String className : classNames) {
            className = className.trim();
            if (className.isEmpty())
                continue;
            Class<?> clazz = ReflectionHelper.loadClass(bundle, className);
            if (clazz != null && !classes.contains(clazz)) {
                classes.add(clazz);
            }
        }

        return classes;
    }
}
