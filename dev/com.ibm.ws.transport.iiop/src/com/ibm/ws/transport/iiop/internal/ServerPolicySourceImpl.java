/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
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
import com.ibm.ws.transport.iiop.spi.OrbConfigurator;
import com.ibm.ws.transport.iiop.spi.ServerPolicySource;
import com.ibm.ws.transport.iiop.spi.SubsystemFactory;

@Component(factory = "com.ibm.ws.transport.iiop.internal.ServerPolicySourceImpl",
                property = { "service.vendor=IBM", "service.ranking:Integer=10" })
@DSExt.PersistentFactoryComponent
public class ServerPolicySourceImpl implements ServerPolicySource {

    /* DS adds the references in the correct ranked order */
    protected final List<OrbConfigurator> orbConfigurators = new ArrayList<>();
    protected Map<String, Object> properties;

    /* SubsystemFactories will eventually be converted to OrbConfigurators. *
     * Until then, this object will accept both types of component.         *
     * Each component should provide only on of these two services.         */

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void addSubsystemFactory(SubsystemFactory sf) { orbConfigurators.add(sf); }
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void addOrbConfigurator(OrbConfigurator oc) { orbConfigurators.add(oc); }

    @Activate
    protected void activate(Map<String, Object> properties, BundleContext ctx) { this.properties = properties; }

    //n.b. no modified method: changing the security config should result in all apps restarting and writing out the new IORs.

    @Override
    public void addConfiguredPolicies(List<Policy> policies, ORBRef server) throws Exception {
        ORB orb = server.getORB();
        for (OrbConfigurator oc : orbConfigurators) {
            Policy targetPolicy = oc.getTargetPolicy(orb, properties, server.getExtraConfig());
            if (null == targetPolicy) continue;
            policies.add(targetPolicy);
        }
    }
}
