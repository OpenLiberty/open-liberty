/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2011
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.lra;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.providers.api.JaxRsProviderRegister;

import io.narayana.lra.filter.ServerLRAFilter;

/**
 * A declarative services component can be completely POJO based
 * (no awareness/use of OSGi services).
 *
 * OSGi methods (activate/deactivate) should be protected.
 */
@Component(service = { JaxRsProviderRegister.class })
public class SampleComponent2 implements JaxRsProviderRegister {

    private static final TraceComponent tc = Tr.register(SampleComponent2.class);

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param properties : Map containing service & config properties
     *            populated/provided by config admin
     */
    @Activate
    protected void activate(Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "SampleComponent activated with a service", properties);
        }

        Tr.warning(tc, "Alert, Another activation!. Has the world not ended yet? with a service");
        String name = ServerLRAFilter.class.getName();
        Tr.warning(tc, "The name is " + name);
    }

    /**
     * DS method to deactivate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param reason int representation of reason the component is stopping
     */
    protected void deactivate(int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "SampleComponent deactivated, reason=" + reason);
        }

        Tr.warning(tc, "Phew, we are secondly being deactivated");
    }

    /** {@inheritDoc} */
    @Override
    public void installProvider(boolean clientSide, List<Object> providers, Set<String> features) {
        Tr.warning(tc, "I don't want to do anything just yet");
        String methodName = "installProvider";

        if (clientSide) {
            OpentracingClientFilter useClientFilter = getClientFilter();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName, "Client Filter", useClientFilter);
            }
            if (useClientFilter != null) {
                providers.add(useClientFilter);
            } else {
                // Ignore: The component is not active.
            }

        } else {
            OpentracingContainerFilter useContainerFilter = getContainerFilter();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName, "Container Filter", useContainerFilter);
            }
            if (useContainerFilter != null) {
                providers.add(useContainerFilter);
            } else {
                // Ignore: The component is not active.
            }
        }

    }

    public static class OpentracingClientFilter implements ClientRequestFilter, ClientResponseFilter {

    }

    public static class OpentracingContainerFilter implements ClientRequestFilter, ClientResponseFilter {

    }

    public OpentracingClientFilter getClientFilter() {
        return new OpentracingClientFilter();
    }

    public OpentracingContainerFilter getContainerFilter() {
        return new OpentracingContainerFilter();
    }

}
