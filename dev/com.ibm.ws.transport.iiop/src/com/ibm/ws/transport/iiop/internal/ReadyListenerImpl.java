/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Some of the code was derived from code supplied by the Apache Software Foundation licensed under the Apache License, Version 2.0.
 */
package com.ibm.ws.transport.iiop.internal;

import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.transport.iiop.spi.ReadyListener;
import com.ibm.ws.transport.iiop.spi.SubsystemFactory;

/**
 * A TSSBean represents a transport-level security profile for exported EJB objects. An
 * exported object is attached to a TSSBean-created named POA. The TSSBean POA
 * is created in the context of the ORB controlled by a CORBABean instance.
 * The parent CORBABean controls the transport-level security of the host connection and
 * defines the endpoint connnection for the object (host and listener port).
 * TSSBean may then define additional characteristics that
 * get encoded in the IOR of the connection.
 * 
 * @version $Revision: 497125 $ $Date: 2007-01-17 10:51:30 -0800 (Wed, 17 Jan 2007) $
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE,
                service = {},
                property = { "service.vendor=IBM", "service.ranking:Integer=10" })
public class ReadyListenerImpl implements ReadyListener {

    /* DS adds the references in the correct ranked order */
    protected final LinkedHashMap<SubsystemFactory, Boolean> subsystemFactories = new LinkedHashMap<SubsystemFactory, Boolean>();
    protected Map<String, Object> properties;

    protected ComponentFactory factory;

    protected volatile ComponentInstance instance;

    private BundleContext ctx;

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setSubsystemFactory(SubsystemFactory sf) {
        subsystemFactories.put(sf, false);
    }

    //appropriate target filter should be set in metatype
    //ComponentFactory.target=(component.factory=<...>)
    @Reference
    protected void setComponentFactory(ComponentFactory factory) {
        this.factory = factory;
    }

    @Activate
    protected void activate(Map<String, Object> properties, BundleContext ctx) {
        this.properties = properties;
        this.ctx = ctx;
        register();
    }

    void register() {
        for (SubsystemFactory sf : subsystemFactories.keySet()) {
            sf.register(this, properties, null);
        }
    }

    @Deactivate
    protected void deactivate() {
        for (SubsystemFactory sf : subsystemFactories.keySet()) {
            sf.unregister(this);
        }
    }

    //n.b. no modified method: changing the security config should result in all apps restarting and writing out the new IORs.

    /** {@inheritDoc} */
    @Override
    public void readyChanged(SubsystemFactory id, boolean ready) {
        boolean allReady = true;
        synchronized (subsystemFactories) {
            subsystemFactories.put(id, ready);
            for (Boolean b : subsystemFactories.values()) {
                allReady &= b;
            }
        }
        //TODO NO THREAD SAFETY use an atomic ref.??
        if (allReady && instance == null) {

            Hashtable<String, Object> properties = new Hashtable<String, Object>();
            properties.putAll(this.properties);
            instance = factory.newInstance(properties);
        } else if (!allReady && instance != null) {
            instance.dispose();
            instance = null;
        }
    }

    @Override
    public String listenerId() {
        return (String) properties.get("id");
    }

}
