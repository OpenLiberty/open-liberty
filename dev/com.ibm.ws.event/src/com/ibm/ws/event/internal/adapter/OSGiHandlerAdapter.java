/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.event.internal.adapter;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.event.Event;

import com.ibm.websphere.event.EventHandler;

public class OSGiHandlerAdapter implements EventHandler {

    final org.osgi.service.event.EventHandler osgiEventHandler;

    public OSGiHandlerAdapter(org.osgi.service.event.EventHandler eventHandler) {
        osgiEventHandler = eventHandler;
    }

    public void handleEvent(com.ibm.websphere.event.Event event) {
        String topic = event.getTopic();
        Map<String, Object> properties = new HashMap<String, Object>();
        for (String key : event.getPropertyNames()) {
            properties.put(key, event.getProperty(key));
        }

        Event osgiEvent = new org.osgi.service.event.Event(topic, properties);
        osgiEventHandler.handleEvent(osgiEvent);
    }

}
