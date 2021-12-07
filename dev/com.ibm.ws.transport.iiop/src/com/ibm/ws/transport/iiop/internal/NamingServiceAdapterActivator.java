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
package com.ibm.ws.transport.iiop.internal;

import static org.apache.yoko.orb.spi.naming.NameServiceInitializer.createPOAPolicies;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.omg.CORBA.Policy;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.AdapterAlreadyExists;
import org.omg.PortableServer.POAPackage.InvalidPolicy;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.ws.transport.iiop.spi.AdapterActivatorOp;
import com.ibm.ws.transport.iiop.spi.ORBRef;
import com.ibm.ws.transport.iiop.spi.ServerPolicySource;

/*
 * N.B. IF we need any more of these, we can make everything except the application of "static" policies into an abstract superclass easily.
 */
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE, property = { "POAName=NameServicePOA" })
public class NamingServiceAdapterActivator implements AdapterActivatorOp {

    private ServerPolicySource serverPolicySource;

    //org.apache.yoko.orb.spi.naming.NameServiceInitializer.POA_NAME
    private final String poaName = "NameServicePOA";

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    protected void setServerPolicySource(ServerPolicySource serverPolicySource) {
        this.serverPolicySource = serverPolicySource;
    }

    @Override
    public boolean unknown_adapter(POA parent, String name, ORBRef orbRef) {
	return Optional.ofNullable(name)
		.filter(poaName::equals)
		.map(n -> getServerPolicies(orbRef))
		.map(l -> createPoa(parent, orbRef.getPOA(), l))
		.orElse(false);
    }

    private List<Policy> getServerPolicies(ORBRef orbRef) {
	return Optional.ofNullable(serverPolicySource)
		.map(Optional::of)
		.orElse(Optional.of(orbRef)
			.filter(ServerPolicySource.class::isInstance)
			.map(ServerPolicySource.class::cast))
		.map(sps -> listServerPolicies(orbRef, sps))
		.orElse(null);
    }

    private List<Policy> listServerPolicies(ORBRef orbRef, ServerPolicySource svrPolicySrc) {
	try {
	    final List<Policy> policies = new ArrayList<>();
	    svrPolicySrc.addConfiguredPolicies(policies, orbRef);
	    return policies;
	} catch (Exception e) { //TODO figure out what exceptions can occur
	    throw new IllegalStateException(e);
	}
    }

    private boolean createPoa(POA parent, POA rootPOA, List<Policy> policies) {
	Stream.of(createPOAPolicies(rootPOA)).forEach(policies::add);
	try {
	    parent.create_POA(poaName, parent.the_POAManager(), policies.toArray(new Policy[0]));
	    return true;
	} catch (AdapterAlreadyExists e) {
	    return false;
	} catch (InvalidPolicy e) {
	    throw new IllegalStateException(e);
	}
    }
}
