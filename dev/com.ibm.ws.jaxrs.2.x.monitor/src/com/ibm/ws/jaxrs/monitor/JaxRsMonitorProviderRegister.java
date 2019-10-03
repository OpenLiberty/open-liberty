/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.monitor;

import java.util.List;
import java.util.Set;

import org.osgi.service.component.annotations.Component;

import com.ibm.ws.jaxrs20.providers.api.JaxRsProviderRegister;

@Component(immediate = true)
public class JaxRsMonitorProviderRegister implements JaxRsProviderRegister {

    @Override
    public void installProvider(boolean clientSide, List<Object> providers, Set<String> features) {

    	System.out.println("Jim... JaxRsMonitorProviderRegister.installProvider:  " + clientSide);
    	System.out.println("Jim... features = " + features.toString());
        if (!clientSide) {
            if (features.contains("monitor-1.0") || features.contains("mpMetrics-1.1")) {
                //add one built-in ContainerRequestFilter/ContainerResponseFilter to enable metric collection.
                JaxRsMonitorFilter jmf = new JaxRsMonitorFilter();
                providers.add(jmf);
            }
        }
    }

}
