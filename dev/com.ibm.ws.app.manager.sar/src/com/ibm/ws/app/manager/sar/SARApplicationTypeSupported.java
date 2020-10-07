/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.sar;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.wsspi.application.handler.ApplicationTypeSupported;

/**
 * @author SAGIA
 * Add support for the sar file extention
 */
@Component(service = ApplicationTypeSupported.class, immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = { "service.vendor=IBM", "type:String=sar" })
public class SARApplicationTypeSupported implements ApplicationTypeSupported {

    @Activate
    protected void activate(ComponentContext context) {
        //
    }

    @Deactivate
    protected void deactivate(ComponentContext context) {
        //
    }
}
