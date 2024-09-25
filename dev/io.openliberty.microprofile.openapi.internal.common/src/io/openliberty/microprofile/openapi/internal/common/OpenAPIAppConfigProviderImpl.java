/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package io.openliberty.microprofile.openapi.internal.common;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.productinfo.ProductInfo;

import io.openliberty.microprofile.openapi.internal.common.services.OpenAPIAppConfigProvider;

@Component(configurationPolicy = ConfigurationPolicy.OPTIONAL, immediate = true, configurationPid = "io.openliberty.microprofile.openapi")
public class OpenAPIAppConfigProviderImpl implements OpenAPIAppConfigProvider {

    private static final TraceComponent tc = Tr.register(OpenAPIAppConfigProviderImpl.class);

    private static final String INCLUDE_PROPERTY_NAME = "include";
    private static final String EXCLUDE_PROPERTY_NAME = "exclude";

    private Optional<String> includedModules = null;
    private Optional<String> excludedModules = null;

    private final Set<OpenAPIAppConfigListener> openAPIAppConfigListeners = new TreeSet<OpenAPIAppConfigListener>();

    private boolean issuedBetaMessage;

    @Activate
    protected void activate(BundleContext context, Map<String, Object> properties) {
        if (ProductInfo.getBetaEdition()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Initial processing of server.xml");
            }
            processModuleConfig(properties);
        }
    }

    @Modified
    protected void modified(Map<String, Object> properties) {
        if (ProductInfo.getBetaEdition()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Processing update to server.xml");
            }
            processModuleConfig(properties);
            for (OpenAPIAppConfigListener listener : openAPIAppConfigListeners) {
                listener.processConfigUpdate();
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        if (ProductInfo.getBetaEdition()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Deactivating OpenAPIAppConfigProviderImpl");
            }
            includedModules = null;
            excludedModules = null;
        }
    }

    private void betaFenceCheck() throws UnsupportedOperationException {
        // Not running beta edition, throw exception
        if (!ProductInfo.getBetaEdition()) {
            throw new UnsupportedOperationException("This method is beta and is not available.");
        } else {
            // Running beta exception, issue message if we haven't already issued one for this class
            if (!issuedBetaMessage) {
                Tr.info(tc, "BETA: A beta method has been invoked for the class " + this.getClass().getName() + " for the first time.");
                issuedBetaMessage = !issuedBetaMessage;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public Optional<String> getIncludedModules() {

        betaFenceCheck();
        return includedModules;
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public Optional<String> getExcludedModules() {
        betaFenceCheck();
        return excludedModules;
    }

    @Override
    public void registerAppConfigListener(OpenAPIAppConfigListener listener) {
        openAPIAppConfigListeners.add(listener);
    }

    @Override
    public void unregisterAppConfigListener(OpenAPIAppConfigListener listener) {
        openAPIAppConfigListeners.add(listener);
    }

    /**
     * @param includedModulesString
     * @param excludedModulesString
     */
    private void processModuleConfig(Map<String, Object> properties) {

        includedModules = Optional.ofNullable((String) properties.get(INCLUDE_PROPERTY_NAME));
        excludedModules = Optional.ofNullable((String) properties.get(EXCLUDE_PROPERTY_NAME));

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "OpenAPI finished processing modules from server.xml."
                         + "found the following configuration string for included modules {0},"
                         + "found the following configuration string for excluded modules {1}",
                     includedModules, excludedModules);
        }
    }

}
