/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package io.openliberty.microprofile.openapi.internal.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.openapi.internal.common.services.OpenAPIAppConfigProvider;
import io.openliberty.microprofile.openapi.internal.common.services.OpenAPIServerXMLConfig;

@Component(configurationPolicy = ConfigurationPolicy.OPTIONAL, configurationPid = "io.openliberty.microprofile.openapi")
public class OpenAPIAppConfigProviderImpl implements OpenAPIAppConfigProvider {

    private static final TraceComponent tc = Tr.register(OpenAPIAppConfigProviderImpl.class);

    private static final String INVALID_APP_WARNING = "OPEN_API_SLASH_IN_APPLICATION_CWWKO1678W";
    private static final String INVALID_MODULE_WARNING = "OPEN_API_SLASH_IN_MODULE_CWWKO1679W";

    private static final String INCLUDE_APP_PROPERTY_NAME = "includeApplication";
    private static final String EXCLUDE_APP_PROPERTY_NAME = "excludeApplication";
    private static final String INCLUDE_MODULE_PROPERTY_NAME = "includeModule";
    private static final String EXCLUDE_MODULE_PROPERTY_NAME = "excludeModule";

    private final List<OpenAPIAppConfigListener> openAPIAppConfigListeners = new CopyOnWriteArrayList<OpenAPIAppConfigListener>();

    private volatile MpConfigServerConfigObject config = null;

    @Activate
    protected void activate(BundleContext context, Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Initial processing of server.xml");
        }

        MpConfigServerConfigObject newConfig = new MpConfigServerConfigObject(properties);
        config = newConfig;
    }

    @Modified
    protected void modified(Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Processing update to server.xml");
        }

        MpConfigServerConfigObject newConfig = new MpConfigServerConfigObject(properties);

        if (config.equals(newConfig)) {
            Tr.event(this, tc, "After update there is no need to rebuild the openAPI document");
        } else {
            Tr.event(this, tc, "Clearing out the outdated openAPI document");

            config = newConfig;
            openAPIAppConfigListeners.sort((OpenAPIAppConfigListener o1, OpenAPIAppConfigListener o2) -> Integer.compare(o1.getConfigListenerPriority(),
                                                                                                                         o2.getConfigListenerPriority()));

            for (OpenAPIAppConfigListener listener : openAPIAppConfigListeners) {
                listener.processConfigUpdate();
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Deactivating OpenAPIAppConfigProviderImpl");
        }
        config = null;
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public MpConfigServerConfigObject getConfiguration() {
        return (config);
    }

    @Override
    public void registerAppConfigListener(OpenAPIAppConfigListener listener) {
        openAPIAppConfigListeners.add(listener);
    }

    @Override
    public void unregisterAppConfigListener(OpenAPIAppConfigListener listener) {
        openAPIAppConfigListeners.remove(listener);
    }

    private static boolean isNotValidModuleName(String elementName) {
        //Check it contains at least one '/' and at least one character
        //before and after the first '/'

        //The looseness of this rule is intentional. If the name is not valid
        //under liberty rules we'll emit a different error message when we can't
        //link this name to an actual module
        if (elementName.indexOf('/') < 0
            || elementName.indexOf('/') == elementName.length() - 1) {
            Tr.warning(tc, INVALID_MODULE_WARNING, elementName, elementName);
            return true;
        }
        return false;
    }

    private static boolean isNotValidAppName(String elementName) {
        if (elementName.indexOf('/') >= 0
            || elementName.length() == 0) {
            Tr.warning(tc, INVALID_APP_WARNING, elementName, elementName);
            return true;
        }
        return false;
    }

    private static class MpConfigServerConfigObject implements OpenAPIServerXMLConfig {

        private final List<String> includedApps = new ArrayList<String>();
        private final List<String> excludedApps = new ArrayList<String>();

        private final List<String> includedModules = new ArrayList<String>();
        private final List<String> excludedModules = new ArrayList<String>();

        private Optional<ConfigMode> configMode = Optional.empty();

        private MpConfigServerConfigObject(Map<String, Object> properties) {
            //Read the server.xml
            Optional<String[]> includedAppArray = Optional.ofNullable((String[]) properties.get(INCLUDE_APP_PROPERTY_NAME));
            Optional<String[]> excludedAppArray = Optional.ofNullable((String[]) properties.get(EXCLUDE_APP_PROPERTY_NAME));
            Optional<String[]> includedModulesArray = Optional.ofNullable((String[]) properties.get(INCLUDE_MODULE_PROPERTY_NAME));
            Optional<String[]> excludedModulesArray = Optional.ofNullable((String[]) properties.get(EXCLUDE_MODULE_PROPERTY_NAME));

            Optional<List<String>> includedAppList = includedAppArray.map((String[] array) -> new ArrayList<>(Arrays.asList(array)));
            Optional<List<String>> excludedAppList = excludedAppArray.map((String[] array) -> new ArrayList<>(Arrays.asList(array)));
            Optional<List<String>> includedModulesList = includedModulesArray.map((String[] array) -> new ArrayList<>(Arrays.asList(array)));
            Optional<List<String>> excludedModulesList = excludedModulesArray.map((String[] array) -> new ArrayList<>(Arrays.asList(array)));

            //Send a warning message and filter if the contents are malformed
            includedAppList.ifPresent((List<String> list) -> list.removeIf(OpenAPIAppConfigProviderImpl::isNotValidAppName));
            excludedAppList.ifPresent((List<String> list) -> list.removeIf(OpenAPIAppConfigProviderImpl::isNotValidAppName));
            includedModulesList.ifPresent((List<String> list) -> list.removeIf(OpenAPIAppConfigProviderImpl::isNotValidModuleName));
            excludedModulesList.ifPresent((List<String> list) -> list.removeIf(OpenAPIAppConfigProviderImpl::isNotValidModuleName));

            if (includedAppList.isPresent() && includedAppList.get().size() == 1) {
                String includedString = includedAppList.get().get(0).toLowerCase().trim();
                if (includedString.equals("all")) {
                    configMode = Optional.of(ConfigMode.All);
                    includedAppList = Optional.empty();
                } else if (includedString.equals("first")) {
                    configMode = Optional.of(ConfigMode.First);
                    includedAppList = Optional.empty();
                } else if (includedString.equals("none")) {
                    configMode = Optional.of(ConfigMode.None);
                    includedAppList = Optional.empty();
                }
            }

            //store outside of an optional because merging two optional lists is convoluted
            includedApps.addAll(includedAppList.orElse(Collections.emptyList()));
            excludedApps.addAll(excludedAppList.orElse(Collections.emptyList()));
            includedModules.addAll(includedModulesList.orElse(Collections.emptyList()));
            excludedModules.addAll(excludedModulesList.orElse(Collections.emptyList()));

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "OpenAPI finished processing modules from server.xml."
                             + " found the following configuration for included apps {0},"
                             + " found the following configuration for excluded apps {1},"
                             + " found the following configuration for included modules {2},"
                             + " found the following configuration for excluded modules {3}",
                         includedApps, excludedApps, includedModules, excludedModules);
            }
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (other == this) {
                return true;
            }
            if (!(other instanceof MpConfigServerConfigObject)) {
                return false;
            }

            MpConfigServerConfigObject otherConfig = (MpConfigServerConfigObject) other;

            return (includedApps.equals(otherConfig.includedApps)
                    && excludedApps.equals(otherConfig.excludedModules)
                    && includedModules.equals(otherConfig.includedModules)
                    && excludedModules.equals(otherConfig.excludedModules)
                    && configMode.equals(otherConfig.configMode));
        }

        @Override
        public Optional<List<String>> getIncludedAppsAndModules() {
            List<String> combinedList = new ArrayList<String>(includedApps);
            combinedList.addAll(includedModules);
            return combinedList.isEmpty() ? Optional.<List<String>> empty() : Optional.of(Collections.unmodifiableList(combinedList));
        }

        @Override
        public Optional<List<String>> getExcludedAppsAndModules() {
            List<String> combinedList = new ArrayList<String>(excludedApps);
            combinedList.addAll(excludedModules);
            return combinedList.isEmpty() ? Optional.<List<String>> empty() : Optional.of(Collections.unmodifiableList(combinedList));
        }

        @Override
        public Optional<ConfigMode> getConfigMode() {
            return configMode;
        }

        @Override
        public boolean wasAnyConfigFound() {
            return (includedApps.size() > 0
                    || excludedApps.size() > 0
                    || includedModules.size() > 0
                    || excludedModules.size() > 0
                    || configMode.isPresent());
        }
    }

}
