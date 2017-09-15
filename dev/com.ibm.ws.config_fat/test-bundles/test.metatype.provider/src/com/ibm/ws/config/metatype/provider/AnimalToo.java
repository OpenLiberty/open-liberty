/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.metatype.provider;

import java.util.Map;

import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

/**
 *
 */
@Component(service = { AnimalToo.class, ConfigurationListener.class }, immediate = true,
           property = { Constants.SERVICE_VENDOR + "=" + "IBM" })
public class AnimalToo implements ConfigurationListener {

    @Activate
    protected void activate(ComponentContext ctx, Map<String, Object> config) {
        System.out.println("Activate");
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        System.out.println("Deactivate");
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        System.out.println("MOdified");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.cm.ConfigurationListener#configurationEvent(org.osgi.service.cm.ConfigurationEvent)
     */
    @Override
    public void configurationEvent(ConfigurationEvent arg0) {
        System.out.println("config event");

    }
}
