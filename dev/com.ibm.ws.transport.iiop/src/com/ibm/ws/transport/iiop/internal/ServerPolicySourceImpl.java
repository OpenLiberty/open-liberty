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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.ext.annotation.DSExt;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.ws.transport.iiop.spi.ORBRef;
import com.ibm.ws.transport.iiop.spi.ServerPolicySource;
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
@Component(factory = "com.ibm.ws.transport.iiop.internal.ServerPolicySourceImpl",
                property = { "service.vendor=IBM", "service.ranking:Integer=10" })
@DSExt.PersistentFactoryComponent
public class ServerPolicySourceImpl implements ServerPolicySource {

    /* DS adds the references in the correct ranked order */
    protected final List<SubsystemFactory> subsystemFactories = new ArrayList<SubsystemFactory>();
    protected Map<String, Object> properties;

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setSubsystemFactory(SubsystemFactory sf) {
        subsystemFactories.add(sf);
    }

    @Activate
    protected void activate(Map<String, Object> properties, BundleContext ctx) {
        this.properties = properties;
    }

    @Deactivate
    protected void deactivate() {}

    //n.b. no modified method: changing the security config should result in all apps restarting and writing out the new IORs.

    /** {@inheritDoc} */
    @Override
    public void addConfiguredPolicies(List<Policy> policies, ORBRef server) throws Exception {
        ORB orb = server.getORB();

        for (SubsystemFactory sf : subsystemFactories) {
            Policy targetPolicy = sf.getTargetPolicy(orb, properties, server.getExtraConfig());
            if (targetPolicy != null) {
                policies.add(targetPolicy);
            }
        }
    }

}
