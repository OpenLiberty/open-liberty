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
package com.ibm.ws.jaxws.wsat.components;

import java.util.Map;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.bus.LibertyApplicationBusFactory;
import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.jaxws.wsat.WSATFeatureBusListener;
import com.ibm.ws.kernel.feature.FeatureProvisioner;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

public class WSATFeatureService {

    private static final String FEATUREPROVISIONER_REFERENCE_NAME = "featureProvisioner";

    private static final TraceComponent tc = Tr.register(
                                                         WSATFeatureService.class, Constants.TRACE_GROUP, null);

    private static final AtomicServiceReference<FeatureProvisioner> featureProvisioner = new AtomicServiceReference<FeatureProvisioner>(
                    FEATUREPROVISIONER_REFERENCE_NAME);

    private WSATFeatureBusListener listener;

    protected void setFeatureProvisioner(
                                         ServiceReference<FeatureProvisioner> ref) {
        featureProvisioner.setReference(ref);
    }

    protected void unsetFeatureProvisioner(
                                           ServiceReference<FeatureProvisioner> ref) {
        featureProvisioner.unsetReference(ref);
    }

    protected void activate(ComponentContext cc) {
        featureProvisioner.activate(cc);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "activate", "Activate featureProvisioner",
                     featureProvisioner);
        if (featureProvisioner.getService() == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "activate", "featureProvisioner service is NULL??");
            return;
        }
        // Keeping the Interceptor in bus because if we would like to check by using isWSATPresent()
        // We need consider both jaxws and wsat features dynamic enable and disable situation
        goInsertListeners();
    }

    protected void deactivate(ComponentContext cc) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "deactivate", cc);
        }
        featureProvisioner.deactivate(cc);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "deactivate", this);
        }
        goRemoveListeners();
    }

    protected void modified(Map<String, Object> newProps) {}

    public static boolean isWSATPresent() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "isWSATPresent",
                     "[WSATFeatureService] checking wsat feature....");
        return featureProvisioner.getService().getInstalledFeatures()
                        .contains(Constants.FEATURE_WSAT_NAME);
    }

    private void goInsertListeners() {
        if (listener == null) {
            listener = new WSATFeatureBusListener();
        }
        if (LibertyApplicationBusFactory.getInstance() != null) {
            LibertyApplicationBusFactory.getInstance()
                            .registerApplicationBusListener(
                                                            listener);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "goInsertListeners", "NO BUS FACTORY");
        }
    }

    private void goRemoveListeners() {
        if (LibertyApplicationBusFactory.getInstance() != null && listener != null) {
            LibertyApplicationBusFactory.getInstance()
                            .unregisterApplicationBusListener(
                                                              listener);
        }
    }
}