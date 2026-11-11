/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
package com.ibm.ws.jaxws.wsat.components;

import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
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
        Set<String> installedFeatures = featureProvisioner.getService().getInstalledFeatures();
        // The public feature short-name is "wsAtomicTransaction-1.2".
        // The private internal feature symbolic names cover ee-6.0, ee-9.0, ee-10.0 and
        // jaxws-2.2 variants.  Any of these indicates WS-AT runtime bundles are present.
        return installedFeatures.contains(Constants.FEATURE_WSAT_NAME)
               || installedFeatures.contains("io.openliberty.wsAtomicTransaction1.2.internal.ee-6.0")
               || installedFeatures.contains("io.openliberty.wsAtomicTransaction1.2.internal.ee-9.0")
               || installedFeatures.contains("io.openliberty.wsAtomicTransaction1.2.internal.ee-10.0")
               || installedFeatures.contains("io.openliberty.wsAtomicTransaction1.2.internal.jaxws-2.2");
    }

    /**
     * Returns {@code true} only when the public {@code wsAtomicTransaction-1.2} feature is
     * installed.  This is distinct from {@link #isWSATPresent()} which also returns {@code true}
     * for the private internal feature variants (inbound-only).
     */
    public static boolean isPublicFeaturePresent() {
        FeatureProvisioner provisioner = featureProvisioner.getService();
        return provisioner != null && provisioner.getInstalledFeatures().contains(Constants.FEATURE_WSAT_NAME);
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
