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
package com.ibm.ws.sib.admin;

import java.util.Map;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.component.ComponentContext;

/**
 * This class provides the functionality to consume the messaging properties sent by the configuration admin
 * and to use it
 */
public abstract class JsMainAdminService {

    /**
     * Will be called to clean up all the resources.Is invoked when deactivate() is called by DS
     * 
     * @param context
     * @param properties
     */
    public abstract void deactivate(ComponentContext context,
                                    Map<String, Object> properties);

    /**
     * Is used to handle the modification in server.xml
     * 
     * @param context
     * @param properties
     */
    public abstract void modified(ComponentContext context,
                                  Map<String, Object> properties, ConfigurationAdmin configAdmin);

    /**
     * Is invoked to consume the messaging properties and construct JSMEconfig Object
     * 
     * @param context
     * @param properties
     * @param serviceList
     */
    public abstract void activate(ComponentContext context, Map<String, Object> properties,
                                  ConfigurationAdmin configAdmin);

    /**
     * Get the state of the Messaging Engine.
     * 
     * @return String
     */
    public abstract String getMeState();

    /**
     * forward configuration events from the listener to the object tracking pids.
     * 
     * @param event configuration event
     * @param configAdmin configuration admin service
     */
    public abstract void configurationEvent(ConfigurationEvent event, ConfigurationAdmin configAdmin);

}
