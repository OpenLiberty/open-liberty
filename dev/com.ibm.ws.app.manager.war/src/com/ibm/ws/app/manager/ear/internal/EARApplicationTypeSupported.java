/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package com.ibm.ws.app.manager.ear.internal;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.wsspi.application.handler.ApplicationTypeSupported;

@Component(service = ApplicationTypeSupported.class, immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "type:String=ear" })
public class EARApplicationTypeSupported implements ApplicationTypeSupported {

    @Activate
    protected void activate(ComponentContext context) {
        //
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        //
    }
}
