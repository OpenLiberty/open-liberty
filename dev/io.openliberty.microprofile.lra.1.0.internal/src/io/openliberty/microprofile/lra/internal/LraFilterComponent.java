/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.lra.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.providers.api.JaxRsProviderRegister;

import io.narayana.lra.client.NarayanaLRAClient;
import io.narayana.lra.filter.ClientLRARequestFilter;
import io.narayana.lra.filter.ClientLRAResponseFilter;
import io.narayana.lra.filter.ServerLRAFilter;

/**
 * A declarative services component can be completely POJO based
 * (no awareness/use of OSGi services).
 *
 * OSGi methods (activate/deactivate) should be protected.
 */
@Component(service = { JaxRsProviderRegister.class })
public class LraFilterComponent implements JaxRsProviderRegister {

    private static final TraceComponent tc = Tr.register(LraFilterComponent.class);

    @Reference
    private LraConfig config;

    /**
     * DS method to activate this component.
     * Best practice: this should be a protected method, not public or private
     *
     * @param properties : Map containing service & config properties
     *            populated/provided by config admin
     */
    @Activate
    protected void activate(Map<String, Object> properties) throws LraException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "LraFilterComponent activated", properties);
        }
        String coordString = "http://" + config.getHost() + ":" + config.getPort() + "/" + config.getPath();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Attempting to contact coordinator at " + coordString);
        }
        try {
            URI coord = new URI(coordString);
            NarayanaLRAClient.setDefaultCoordinatorEndpoint(coord);
        } catch (URISyntaxException e) {
            throw new LraException(Tr.formatMessage(tc, "LRA_INVALID_COORDINATOR_URI.CWMRX5000E"), e);
        }

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
    }

    /** {@inheritDoc} */
    @Override
    public void installProvider(boolean clientSide, List<Object> providers, Set<String> features) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Registering LRA filters");
        }

        if (clientSide) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Registering client side filters");
            }
            ClientLRARequestFilter requestFilter = new ClientLRARequestFilter();
            providers.add(requestFilter);
            ClientLRAResponseFilter responseFilter = new ClientLRAResponseFilter();
            providers.add(responseFilter);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Registering serverside side filters");
            }
            try {
                // Rather unhelpfully, the ServerLRAFilter constructor throws 'Exception'. There isn't much we can do with
                // that. Re-throwing should prevent the servlet being initialized, which is probably better than swallowing
                // the exception
                providers.add(new ServerLRAFilter());
            } catch (Exception e) {
                throw new LraRuntimeException(Tr.formatMessage(tc, "LRA_CANT_REGISTER_FILTERS.CWMRX5001E", e), e);
            }
        }
    }

}
