/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.logging.internal;

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.ws.http.logging.source.AccessLogSource;

@Component(service = { ConfigurationSetterLogstash.class }, immediate = true)
public class ConfigurationSetterLogstash {

    @Activate
    protected void activate(ComponentContext context, Map<String, Object> properties) {
    }

    public void setConfig(String logFormat) {
        AccessLogSource.jsonAccessLogFieldsLogstashConfig = logFormat;
    }

    @Deactivate
    protected void deactivate(ComponentContext context, int reason) {
    }
}
