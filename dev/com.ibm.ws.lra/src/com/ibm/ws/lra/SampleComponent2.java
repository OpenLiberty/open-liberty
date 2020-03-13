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

import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.narayana.lra.filter.ServerLRAFilter;

/**
 * A declarative services component can be completely POJO based
 * (no awareness/use of OSGi services).
 *
 * OSGi methods (activate/deactivate) should be protected.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class SampleComponent2 {

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
            Tr.event(tc, "SampleComponent activated", properties);
        }

        Tr.warning(tc, "Alert, Another activation!. Has the world not ended yet?");
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
}
