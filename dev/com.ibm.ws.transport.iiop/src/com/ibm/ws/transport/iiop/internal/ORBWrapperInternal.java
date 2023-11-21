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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.rmi.CORBA.Util;

import org.apache.felix.scr.ext.annotation.DSExt;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.CORBA.PolicyManager;
import org.omg.CORBA.SetOverrideType;
import org.omg.PortableServer.AdapterActivator;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.transport.iiop.config.ConfigAdapter;
import com.ibm.ws.transport.iiop.spi.AdapterActivatorOp;
import com.ibm.ws.transport.iiop.spi.ClientORBRef;
import com.ibm.ws.transport.iiop.spi.IIOPEndpoint;
import com.ibm.ws.transport.iiop.spi.ORBRef;
import com.ibm.ws.transport.iiop.spi.OrbConfigurator;
import com.ibm.ws.transport.iiop.spi.ServerPolicySource;
import com.ibm.ws.transport.iiop.spi.SubsystemFactory;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 * Provides access to the ORB.
 */
@Component(factory = "com.ibm.ws.transport.iiop.internal.ORBWrapperInternal",
                property = { "service.vendor=IBM" })
@DSExt.PersistentFactoryComponent
public class ORBWrapperInternal extends ServerPolicySourceImpl implements ORBRef, ClientORBRef, ServerPolicySource {
    private static final TraceComponent tc = Tr.register(ORBWrapperInternal.class);

    private static final String KEY = "AdapterActivatorOp";

    private static final String POA_NAME = "POAName";

    private ConfigAdapter configAdapter;
    private ORB orb;
    private POA rootPOA;

    private final List<IIOPEndpoint> endpoints = new ArrayList<>();

    private final Map<String, Object> extraConfig = new HashMap<>();

    private final transient ConcurrentServiceReferenceMap<String, AdapterActivatorOp> map = new ConcurrentServiceReferenceMap<>(KEY);
    
    private final CheckpointPhase checkpointPhase = CheckpointPhase.getPhase();

    
    @Activate
    protected void activate(Map<String, Object> properties, ComponentContext cc) throws Exception {
        map.activate(cc);
        super.activate(properties, cc.getBundleContext());
        try {
            if (checkpointPhase != CheckpointPhase.INACTIVE) {
                Util.createValueHandler().getRunTimeCodeBase();
            }
        } catch (Exception ignored) {
        }
        try {
            if (endpoints.isEmpty()) {
                this.orb = configAdapter.createClientORB(properties, orbConfigurators);
            } else {
                // the config adapter creates the actual ORB instance for us.
                orb = configAdapter.createServerORB(properties, extraConfig, endpoints, orbConfigurators);

                org.omg.CORBA.Object obj = orb.resolve_initial_references("RootPOA");
                rootPOA = POAHelper.narrow(obj);
                rootPOA.the_activator(new DelegatingAdapterActivator());
            }
            if (tc.isDebugEnabled())
                Tr.debug(tc, "activate() using ORB: " + orb);
            PolicyManager policyManager = (PolicyManager) orb.resolve_initial_references("ORBPolicyManager");
            List<Policy> policies = new ArrayList<>();
            for (OrbConfigurator oc : orbConfigurators) {
                Policy policy = oc.getClientPolicy(orb, properties);
                if (policy != null) policies.add(policy);
            }
            policyManager.set_policy_overrides(policies.toArray(new Policy[policies.size()]), SetOverrideType.ADD_OVERRIDE);
            for (IIOPEndpoint endpoint : endpoints) {
                Tr.audit(tc, "NAME_SERVER_AVAILABLE", "corbaloc:iiop:" + endpoint.getHost() + ":" + endpoint.getIiopPort() + "/NameService");
            }
        } catch (NoSuchMethodError|Exception ignored) {
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext cc) {
        if (orb != null) {
            orb.destroy();
            orb = null;
            for (IIOPEndpoint endpoint : endpoints) {
                Tr.audit(tc, "NAME_SERVER_UNAVAILABLE", "corbaloc:iiop:" + endpoint.getHost() + ":" + endpoint.getIiopPort() + "/NameService");
            }
        }
        map.deactivate(cc);
    }

    @Reference
    protected void setConfigAdapter(ConfigAdapter configAdapter) {
        this.configAdapter = configAdapter;
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setIiopEndpoint(IIOPEndpoint ep) {
        endpoints.add(ep);
    }

    @Reference(service = AdapterActivatorOp.class, cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void setAdapterActivatorOp(ServiceReference<AdapterActivatorOp> ops) {
        map.putReference((String) ops.getProperty(POA_NAME), ops);
    }

    protected void unsetAdapterActivatorOp(ServiceReference<AdapterActivatorOp> ops) {
        map.removeReference((String) ops.getProperty(POA_NAME), ops);
        //TODO shutdown poa from this ops??
    }

    @Override
    public ORB getORB() {
        return orb;
    }

    /**
     * Get the root POA() instance associated with the ORB.
     * 
     * @return The rootPOA instance obtained from the ORB.
     */
    @Override
    public POA getPOA() {
        return rootPOA;
    }

    @Override
    public Map<String, Object> getExtraConfig() {
        return extraConfig;
    }

    private class DelegatingAdapterActivator extends LocalObject implements AdapterActivator {
        private static final long serialVersionUID = 1L;
        @Override
        public boolean unknown_adapter(POA parent, String name) {
            AdapterActivatorOp ops = map.getService(name);
            if (null == ops) return false;
            return ops.unknown_adapter(parent, name, ORBWrapperInternal.this);
        }
    }
}
