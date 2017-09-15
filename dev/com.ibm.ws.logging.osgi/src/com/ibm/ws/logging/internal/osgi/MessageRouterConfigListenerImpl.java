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
package com.ibm.ws.logging.internal.osgi;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import com.ibm.ws.logging.osgi.MessageRouterConfigListener;

/**
 * Service that will register to listen for updates to the <zosLogging> config element
 * for changes to the defined message routing groups.
 */
public class MessageRouterConfigListenerImpl implements MessageRouterConfigListener {

    /**
     * MessageRouterConfigurator to forward config updates to.
     */
    private final MessageRouterConfigurator msgRouterConfigurator;

    /**
     * Registration of this service.
     */
    private ServiceRegistration<MessageRouterConfigListener> serviceRegistration;

    /**
     * Constructor.
     * 
     * @param configurator
     */
    public MessageRouterConfigListenerImpl(MessageRouterConfigurator configurator) {
        this.msgRouterConfigurator = configurator;
    }

    /**
     * Register as a service to be consumed by the <zosLogging> config service.
     * 
     * @param bundleContext
     * @return
     */
    public synchronized MessageRouterConfigListenerImpl register(BundleContext bundleContext) {
        if (serviceRegistration != null) {
            return this; // Already registered.
        }

        Dictionary<String, Object> props = new Hashtable<String, Object>(1);
        props.put(Constants.SERVICE_VENDOR, "IBM");

        // register the ManagedService that will handle the config
        serviceRegistration = bundleContext.registerService(MessageRouterConfigListener.class, this, props);
        return this;
    }

    /**
     * Unregister this service.
     */
    public synchronized void unregister() {
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
        }
    }

    @Override
    public void updateMessageListForHandler(String msgIds, String handlerId) {
        msgRouterConfigurator.updateMessageListForHandler(msgIds, handlerId);;
    }
}
