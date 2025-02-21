/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
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
package com.ibm.ws.jaxws.wsat;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.Bus;

import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.policy.AssertionBuilderRegistry;
import org.apache.cxf.ws.policy.PolicyInterceptorProviderRegistry;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.bus.LibertyApplicationBus;
import com.ibm.ws.jaxws.bus.LibertyApplicationBusListener;
import com.ibm.ws.wsat.policy.WSATAssertionBuilder;
import com.ibm.ws.wsat.policy.WSATAssertionPolicyProvider;
import com.ibm.ws.wsat.policy.WSATPolicyAwareInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPOutInterceptor;
import io.openliberty.checkpoint.spi.CheckpointPhase;

public class WSATFeatureBusListener implements LibertyApplicationBusListener {
    private static final TraceComponent tc = Tr.register(
                                                         WSATFeatureBusListener.class, Constants.TRACE_GROUP, null);

    private final static Set<Bus> busSet = Collections.newSetFromMap(new ConcurrentHashMap<Bus, Boolean>());
    private static boolean addGzipInInterceptor;
    private static boolean addGzipOutInterceptor;

    static {

        String addGzipInProp = System.getProperty("cxf.add.gzip.in.interceptor");
        String addGzipOutProp = System.getProperty("cxf.add.gzip.out.interceptor");

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
           Tr.debug(tc, "System property cxf.add.gzip.in.interceptor is set to: ", addGzipInProp);
           Tr.debug(tc, "System property cxf.add.gzip.out.interceptor is set to: ", addGzipOutProp);
        }

        if (addGzipInProp != null 
            && addGzipInProp.trim().length() > 0
            && addGzipInProp.trim().equalsIgnoreCase("true")) {
            addGzipInInterceptor = true;
        }
        if (addGzipOutProp != null 
            && addGzipOutProp.trim().length() > 0
            && addGzipOutProp.trim().equalsIgnoreCase("true")) {
            addGzipOutInterceptor = true;
        }
    }

    public static Set<Bus> getBusSet() {
        return busSet;
    }

    @Override
    public void preInit(Bus bus) {

    }

    @Override
    public void initComplete(Bus bus) {
        if (bus == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "initComplete", "bus == NULL");
            return;
        }

        LibertyApplicationBus.Type busType = bus.getExtension(LibertyApplicationBus.Type.class);
        if (busType == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "initComplete",
                         "Can not determine server type, not Liberty BUS?");
            return;
        } else {
            busSet.add(bus);
        }
        AssertionBuilderRegistry reg = bus.getExtension(AssertionBuilderRegistry.class);
        if (reg != null) {
            CheckpointPhase.onRestore(() -> reg.registerBuilder(new WSATAssertionBuilder()));
        }

        PolicyInterceptorProviderRegistry regIPR = bus.getExtension(PolicyInterceptorProviderRegistry.class);
        if (reg != null) {
            CheckpointPhase.onRestore(() -> {
                WSATAssertionPolicyProvider _policyProvider = new WSATAssertionPolicyProvider();
                regIPR.register(_policyProvider);
            });
            
        }

        if (addGzipInInterceptor)  {
           if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
              Tr.debug(tc, "Adding GZIPInInterceptor...");
           }
           final GZIPInInterceptor in1 = new GZIPInInterceptor();
           bus.getInInterceptors().add(in1);
        }
        if (addGzipOutInterceptor)  {
           if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
              Tr.debug(tc, "Adding GZIPOutInterceptor...");
           }
           final GZIPOutInterceptor out1 = new GZIPOutInterceptor();
           bus.getOutInterceptors().add(out1);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "initComplete",
                     "If we've got here, then the interceptors should be inserted");
        }

        bus.getInInterceptors().add(new WSATPolicyAwareInterceptor(Phase.PRE_PROTOCOL, false));
        bus.getInInterceptors().add(new WSATPolicyAwareInterceptor(Phase.RECEIVE, false));
        bus.getOutInterceptors().add(new WSATPolicyAwareInterceptor(Phase.SETUP, true));
        bus.getOutInterceptors().add(new WSATPolicyAwareInterceptor(Phase.WRITE, true));
    }

    @Override
    public void preShutdown(Bus bus) {

    }

    @Override
    public void postShutdown(Bus bus) {
        busSet.remove(bus);
    }

}
