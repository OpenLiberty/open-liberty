/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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
package com.ibm.ws.jaxrs.monitor;

import java.util.List;
import java.util.Set;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.container.service.app.deploy.ApplicationInfo;
import com.ibm.ws.jaxrs.monitor.JaxRsMonitorFilter.RestMetricInfo;
import com.ibm.ws.jaxrs20.providers.api.JaxRsProviderRegister;

@Component(immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE, service = {JaxRsProviderRegister.class})
public class JaxRsMonitorProviderRegister implements JaxRsProviderRegister {

	private JaxRsMonitorFilter monitorFilter = new JaxRsMonitorFilter();
	
    @Override
    public void installProvider(boolean clientSide, List<Object> providers, Set<String> features) {
        
    	// Register the metrics monitor filter class if we are not on the client.
        if (!clientSide) {
            // Add  built-in ContainerRequestFilter/ContainerResponseFilter to enable metric collection.
            providers.add(monitorFilter);
        }
    }
}