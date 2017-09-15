/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.wsat;

import java.util.HashSet;
import java.util.Set;

import org.apache.cxf.Bus;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
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

public class WSATFeatureBusListener implements LibertyApplicationBusListener {
    private static final TraceComponent tc = Tr.register(
                                                         WSATFeatureBusListener.class, Constants.TRACE_GROUP, null);

    private static Set<Bus> busSet;

    public static Set<Bus> getBusSet() {
        return busSet;
    }

    private synchronized static void insertBus(Bus b) {
        if (busSet == null) {
            busSet = new HashSet<Bus>();
        }
        busSet.add(b);
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

        LibertyApplicationBus.Type busType = bus
                        .getExtension(LibertyApplicationBus.Type.class);
        if (busType == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "initComplete",
                         "Can not determine server type, not Liberty BUS?");
            return;
        } else {
            insertBus(bus);
        }
        AssertionBuilderRegistry reg = bus.getExtension(AssertionBuilderRegistry.class);
        if (reg != null) {
            reg.registerBuilder(new WSATAssertionBuilder());
        }

        PolicyInterceptorProviderRegistry regIPR = bus.getExtension(PolicyInterceptorProviderRegistry.class);
        if (reg != null) {
            WSATAssertionPolicyProvider _policyProvider = new WSATAssertionPolicyProvider();
            regIPR.register(_policyProvider);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "initComplete",
                     "If we've got here, then the interceptors should be inserted");

            // Prettyprint the SOAP if we're debugging
            final LoggingInInterceptor in = new LoggingInInterceptor();
            in.setPrettyLogging(true);
            bus.getInInterceptors().add(in);

            final LoggingOutInterceptor out = new LoggingOutInterceptor();
            out.setPrettyLogging(true);
            bus.getOutInterceptors().add(out);
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

    }

}
