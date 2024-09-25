/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.microprofile.openapi20.internal;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.kernel.productinfo.ProductInfo;

import io.openliberty.microprofile.openapi.internal.common.services.OpenAPIAppConfigProvider;
import io.openliberty.microprofile.openapi.internal.common.services.OpenAPIAppConfigProvider.OpenAPIAppConfigListener;
import io.openliberty.microprofile.openapi20.internal.services.ModuleSelectionConfig;
import io.openliberty.microprofile.openapi20.internal.utils.Constants;
import io.openliberty.microprofile.openapi20.internal.utils.MessageConstants;

/**
 * Handles reading the merge include/exclude configuration properties and indicating whether a particular module should be included or excluded.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class ModuleSelectionConfigImpl implements ModuleSelectionConfig, OpenAPIAppConfigProvider.OpenAPIAppConfigListener {

    private static final TraceComponent tc = Tr.register(ModuleSelectionConfigImpl.class);

    private boolean hasBeenInit = false;
    private boolean isAll = false;
    private boolean isFirst = false;
    private List<ModuleName> included;
    private List<ModuleName> excluded;

    private OpenAPIAppConfigProvider configFromServerXML;

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.STATIC, unbind = "unbindAppConfigListener")
    public void bindAppConfigListener(OpenAPIAppConfigProvider openAPIAppConfigProvider) {
        this.configFromServerXML = openAPIAppConfigProvider;
        openAPIAppConfigProvider.registerAppConfigListener(this);
    }

    public void unbindAppConfigListener(OpenAPIAppConfigProvider openAPIAppConfigProvider) {
        openAPIAppConfigProvider.unregisterAppConfigListener(this);
        this.configFromServerXML = null;
    }

    /**
     * Builds a {@code ModuleSelectionConfig}
     * <p>
     * This will read and parse the {@value Constants#MERGE_INCLUDE_CONFIG} and {@value Constants#MERGE_EXCLUDE_CONFIG} config properties from mpConfig.
     * <p>
     * TODO doc what it reads from server.xml
     * <p>
     * If the config is invalid, this method will output warning messages but still return a usable result object.
     *
     * @return the module selection config
     */
    private synchronized void lazyInit() {
        if (hasBeenInit) {
            return;
        }

        Config configFromMPConfig = ConfigProvider.getConfig(ApplicationRegistryImpl.class.getClassLoader());

        String inclusion;
        if (!ProductInfo.getBetaEdition()) {
            inclusion = configFromMPConfig.getOptionalValue(Constants.MERGE_INCLUDE_CONFIG, String.class).orElse("first");

        } else {
            inclusion = configFromServerXML.getIncludedModules()
                                           .orElse(configFromMPConfig.getOptionalValue(Constants.MERGE_INCLUDE_CONFIG, String.class).orElse("first"))
                                           .trim();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(null, tc, "Names in config: " + configFromMPConfig.getPropertyNames());
            Tr.debug(null, tc, "Inclusion read from config: " + inclusion);
        }

        if (inclusion.equals("none")) {
            included = Collections.emptyList();
        } else if (inclusion.equals("all")) {
            isAll = true;
        } else if (inclusion.equals("first")) {
            isFirst = true;
        } else {
            included = parseModuleNames(inclusion, Constants.MERGE_INCLUDE_CONFIG);
        }

        String exclusion;

        if (!ProductInfo.getBetaEdition()) {
            exclusion = configFromMPConfig.getOptionalValue(Constants.MERGE_EXCLUDE_CONFIG, String.class).orElse("none")
                                          .trim();
        } else {
            exclusion = configFromServerXML.getExcludedModules()
                                           .orElse(configFromMPConfig.getOptionalValue(Constants.MERGE_EXCLUDE_CONFIG, String.class).orElse("none"))
                                           .trim();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(null, tc, "Exclusion read from config: " + exclusion);
        }

        if (exclusion.equals("none")) {
            excluded = Collections.emptyList();
        } else {
            excluded = parseModuleNames(exclusion, Constants.MERGE_EXCLUDE_CONFIG);
        }

        hasBeenInit = true;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "lazyInit", toString());
        }
    }

    @Override
    public String toString() {
        lazyInit();
        StringBuilder sb = new StringBuilder("Module Selection Config[");
        if (isFirst) {
            sb.append("useFirstModuleOnly");
        } else {
            if (isAll) {
                sb.append("include = all");
            } else {
                sb.append("include = ").append(included);
            }
            sb.append(", ");
            sb.append("exclude = ").append(excluded);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Whether the legacy "first module only" mode should be used.
     * <p>
     * As this requires special handling, if this method returns {@code true}, the other methods on this object should not be called.
     *
     * @return {@code true} if only the first module found should be processed for OpenAPI annotations, {@code false} otherwise.
     */
    @Override
    public boolean useFirstModuleOnly() {
        lazyInit();
        return isFirst;
    }

    /**
     * Whether the given module should be used to create the OpenAPI document, based on the config
     *
     * @param module the module to check
     * @return {@code true} if the module should be used, {@code false} otherwise
     */
    @Override
    public boolean isIncluded(ModuleInfo module) {
        lazyInit();
        if (isFirst) {
            return true;
        }

        boolean result = false;
        if (isAll) {
            result = true;
        } else {
            for (ModuleName name : included) {
                if (matches(name, module)) {
                    result = true;
                    break;
                }
            }
        }

        if (result) {
            for (ModuleName name : excluded) {
                if (matches(name, module)) {
                    result = false;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Given a complete list of all application modules deployed, return a list of entries from the include configuration which didn't match any of the deployed modules.
     *
     * @param moduleInfos the deployed module infos
     * @return the list of include configuration entries which was unused
     */
    @Override
    public List<String> findIncludesNotMatchingAnything(Collection<? extends ModuleInfo> moduleInfos) {
        lazyInit();
        if (isAll || isFirst) {
            return Collections.emptyList();
        }

        List<ModuleName> includedNotYetSeen = new ArrayList<>(included);
        for (Iterator<ModuleName> iterator = includedNotYetSeen.iterator(); iterator.hasNext();) {
            ModuleName moduleName = iterator.next();
            for (ModuleInfo moduleInfo : moduleInfos) {
                if (matches(moduleName, moduleInfo)) {
                    iterator.remove();
                    break;
                }
            }
        }

        return includedNotYetSeen.stream().map(ModuleName::toString).collect(toList());
    }

    private boolean matches(ModuleName name, ModuleInfo module) {
        lazyInit();
        if (name.moduleName != null && !name.moduleName.equals(module.getName())) {
            return false;
        }

        if (!name.appName.equals(module.getApplicationInfo().getName())) {
            return false;
        }

        return true;
    }

    private static class ModuleName {
        /**
         * The application name
         */
        private final String appName;

        /**
         * The module name, may be {@code null} if the configuration just indicates an application name
         */
        private final String moduleName;

        /**
         * @param appName
         * @param moduleName
         */
        public ModuleName(String appName, String moduleName) {
            this.appName = appName;
            this.moduleName = moduleName;
        }

        @Override
        public String toString() {
            if (moduleName == null) {
                return appName;
            } else {
                return appName + "/" + moduleName;
            }
        }
    }

    @Override
    public void processConfigUpdate() {
        hasBeenInit = false;
    }

    private static final Pattern CONFIG_VALUE_NAME_REFERENCE = Pattern.compile("(.+?)(/(.+))?");

    /**
     * Parses a comma separated list of app and module names into a list of {@code ModuleName}
     * <p>
     * Names must be in one of these formats:
     * <ul>
     * <li>appName</li>
     * <li>appName/moduleName</li>
     * </ul>
     *
     * @param nameList the comma separated list
     * @param configKey the name of the config property holding the list (used for reporting errors)
     * @param the list of parsed names
     */
    private static List<ModuleName> parseModuleNames(String nameList, String configKey) {
        List<ModuleName> result = new ArrayList<>();

        for (String configValuePart : nameList.split(",")) {
            Matcher m = CONFIG_VALUE_NAME_REFERENCE.matcher(configValuePart);

            if (!m.matches()) {
                Tr.warning(tc, MessageConstants.OPENAPI_MERGE_INVALID_NAME_CWWKO1666W, configKey, configValuePart);
                continue;
            }

            String appName = m.group(1).trim();
            String moduleName = m.group(3);
            if (moduleName != null) {
                moduleName = moduleName.trim();
            }
            result.add(new ModuleName(appName, moduleName));
        }

        return result;
    }

    @Override
    public int compareTo(OpenAPIAppConfigListener o) {
        if (this == o || this.equals(o)) {
            return 0;
        } else {
            return this.getConfigListenerPriority() - o.getConfigListenerPriority();
        }
    }

    @Override
    public int getConfigListenerPriority() {
        return 1;
    }

}
