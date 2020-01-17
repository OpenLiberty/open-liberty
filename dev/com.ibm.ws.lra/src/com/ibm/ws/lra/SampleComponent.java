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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * A declarative services component can be completely POJO based
 * (no awareness/use of OSGi services).
 *
 * OSGi methods (activate/deactivate) should be protected.
 */
public class SampleComponent {

    private static final TraceComponent tc = Tr.register(SampleComponent.class);

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param properties : Map containing service & config properties
     *                       populated/provided by config admin
     */
    protected void activate(Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "SampleComponent activated", properties);
        }

        Tr.warning(tc, "Alert, have been activated, the world may end");
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
        Tr.warning(tc, "Phew, we are firstly being deactivated");
    }
}
