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
package com.ibm.ws.wsat.policy;

import org.apache.cxf.Bus;
import org.apache.cxf.ws.policy.AssertionBuilderLoader;
import org.apache.cxf.ws.policy.AssertionBuilderRegistry;
import org.apache.cxf.ws.policy.PolicyInterceptorProviderLoader;
import org.apache.cxf.ws.policy.PolicyInterceptorProviderRegistry;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.wsat.Constants;

public final class WSATPolicyLoader implements PolicyInterceptorProviderLoader, AssertionBuilderLoader {

    private final TraceComponent tc = Tr.register(
                                                  WSATPolicyLoader.class, Constants.TRACE_GROUP, null);

    private final Bus bus;

    public WSATPolicyLoader(Bus b) {

        bus = b;
        boolean bOk = true;
        try {
            registerBuilders();
            registerProviders();
        } catch (Throwable t) {
            //probably ATAssertion isn't found or something. We'll ignore this
            //as the policy framework will then not find the providers
            //and error out at that point.  If nothing uses ws-at policy
            //no warnings/errors will display
            bOk = false;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "error.policy.notloaded");
            }
        }
        if (bOk) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The WS-AT Policy Loader is invoked successfully.");
            }
        }
    }

    public void registerBuilders() {
        AssertionBuilderRegistry reg = bus.getExtension(AssertionBuilderRegistry.class);
        if (reg == null) {
            return;
        }
        reg.registerBuilder(new WSATAssertionBuilder());

    }

    public void registerProviders() {
        //interceptor providers for all of the above
        PolicyInterceptorProviderRegistry reg = bus.getExtension(PolicyInterceptorProviderRegistry.class);
        if (reg == null) {
            return;
        }
        reg.register(new WSATAssertionPolicyProvider());

    }

}
