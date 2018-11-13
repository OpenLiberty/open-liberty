/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.concurrent.mp.cdi;

import javax.enterprise.inject.spi.Extension;

import org.eclipse.microprofile.concurrent.ManagedExecutorConfig;
import org.eclipse.microprofile.concurrent.ThreadContextConfig;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.ws.cdi.extension.WebSphereCDIExtension;

// TODO replace with real implementation
@Component(service = WebSphereCDIExtension.class, configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true)
public class ConcurrencyCDIExtension implements Extension, WebSphereCDIExtension {
    @Activate
    protected void activate(ComponentContext osgiComponentContext) {
        System.out.println("TODO add integration for " + ManagedExecutorConfig.class + " and " + ThreadContextConfig.class);
    }

    @Deactivate
    protected void deactivate(ComponentContext osgiComponentContext) {}
}