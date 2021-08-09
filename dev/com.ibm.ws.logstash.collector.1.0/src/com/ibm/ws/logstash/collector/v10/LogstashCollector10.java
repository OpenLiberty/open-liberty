/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logstash.collector.v10;

import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.logstash.collector.LogstashRuntimeVersion;

@Component(name = LogstashCollector10.COMPONENT_NAME, service = { LogstashRuntimeVersion.class }, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" })
public class LogstashCollector10 implements LogstashRuntimeVersion {

    public static final String COMPONENT_NAME = "com.ibm.ws.logstash.collector.v10.LogstashCollector10";

    @Override
    public Version getVersion() {
        return VERSION_1_0;
    }

}
