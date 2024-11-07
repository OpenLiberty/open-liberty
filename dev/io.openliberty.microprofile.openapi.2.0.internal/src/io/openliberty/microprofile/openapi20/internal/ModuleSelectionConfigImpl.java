/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.kernel.productinfo.ProductInfo;

import io.openliberty.microprofile.openapi.internal.common.services.OpenAPIAppConfigProvider;
import io.openliberty.microprofile.openapi.internal.common.services.OpenAPIServerXMLConfig;
import io.openliberty.microprofile.openapi.internal.common.services.OpenAPIServerXMLConfig.ConfigMode;
import io.openliberty.microprofile.openapi20.internal.services.ModuleSelectionConfig;
import io.openliberty.microprofile.openapi20.internal.utils.Constants;
import io.openliberty.microprofile.openapi20.internal.utils.MessageConstants;

/**
 * Handles reading the merge include/exclude configuration properties and indicating whether a particular module should be included or excluded.
 */
public class ModuleSelectionConfigImpl implements ModuleSelectionConfig, OpenAPIAppConfigProvider.OpenAPIAppConfigListener {

    private static final TraceComponent tc = Tr.register(ModuleSelectionConfigImpl.class);

    private volatile ConfigValues configValues = null;

    private enum MatchingMode {
        SERVER_XML_NAME,
        DEPLOYMENT_DESCRIPTOR_NAME
    }

    @Reference
    protected OpenAPIAppConfigProvider configFromServerXMLProvider;

    @Activate
    public void activate() {
        configFromServerXMLProvider.registerAppConfigListener(this);
    }

    @Deactivate
    public void deactivate() {
        configFromServerXMLProvider.unregisterAppConfigListener(this);
    }

    @Override
    public String toString() {
        ConfigValues config = configValues;
        if (config == null) {
            return ("Unconfigured Module Selection Config");
        }

        StringBuilder sb = new StringBuilder("Module Selection Config[");
        if (config.isFirst) {
            sb.append("useFirstModuleOnly");
        } else {
            if (config.isAll) {
                sb.append("include = all");
            } else {
                sb.append("include = ").append(config.included);
            }
            sb.append(", ");
            sb.append("exclude = ").append(config.excluded);
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
        return getConfigValues().isFirst;
    }

    /**
     * Whether the given module should be used to create the OpenAPI document, based on the config
     *
     * @param module the module to check
     * @return {@code true} if the module should be used, {@code false} otherwise
     */
    @Override
    public boolean isIncluded(ModuleInfo module) {
        ConfigValues config = getConfigValues();
        if (config.isFirst) {
            return true;
        }

        boolean result = false;
        if (config.isAll) {
            result = true;
        } else {
            for (ModuleName name : config.included) {
                if (matches(name, module)) {
                    result = true;
                    break;
                }
            }
        }

        if (result) {
            for (ModuleName name : config.excluded) {
                if (matches(name, module)) {
                    result = false;
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Given a complete list of all application modules deployed, emit a warning for all applications or modules included in the OpenAPI
     * document that do not match a deployed module. The warning will specify if the include statement matches the deployment descriptor
     * when we're looking for the name that's specified in server.xml.
     *
     * Also throw a warning for an application or module excluded from the openAPI document if it matches a deployment descriptor when
     * we're looking for the name that's specified in server.xml, but not if it doesn't match either.
     *
     * @param moduleInfos the deployed module infos
     */
    @Override
    public void sendWarningsForAppsAndModulesNotMatchingAnything(Collection<? extends ModuleInfo> moduleInfos) {
        ConfigValues config = getConfigValues();
        if (config.isAll || config.isFirst) {
            return;
        }

        List<ModuleName> includedNotYetSeen = new ArrayList<>(config.included);
        for (Iterator<ModuleName> iterator = includedNotYetSeen.iterator(); iterator.hasNext();) {
            ModuleName moduleName = iterator.next();
            for (ModuleInfo moduleInfo : moduleInfos) {
                if (matches(moduleName, moduleInfo)) {
                    iterator.remove();
                    break;
                }
            }
        }

        Map<ModuleName, String> includedNotYetSeenButSeenUnderOldNaming = new HashMap<>();
        if (config.serverxmlmode && ProductInfo.getBetaEdition()) {
            for (Iterator<ModuleName> iterator = includedNotYetSeen.iterator(); iterator.hasNext();) {
                ModuleName moduleName = iterator.next();
                for (ModuleInfo moduleInfo : moduleInfos) {
                    if (matches(moduleName, moduleInfo, MatchingMode.DEPLOYMENT_DESCRIPTOR_NAME)) {
                        includedNotYetSeenButSeenUnderOldNaming.put(moduleName,
                                                                    moduleInfo.getApplicationInfo().getDeploymentName());
                        iterator.remove();
                        break;
                    }
                }
            }

            for (ModuleName unmatchedInclude : includedNotYetSeenButSeenUnderOldNaming.keySet()) {
                String appOrModule = unmatchedInclude.moduleName != null ? "includeModule" : "includeApplication";

                Tr.warning(tc, MessageConstants.OPENAPI_USING_WRONG_NAME_SOURCE_CWWKO1680W, appOrModule, unmatchedInclude.appName,
                           includedNotYetSeenButSeenUnderOldNaming.get(unmatchedInclude));
            }
        }

        for (ModuleName unmatchedInclude : includedNotYetSeen) {
            if (config.serverxmlmode && ProductInfo.getBetaEdition()) {
                String elementName = unmatchedInclude.moduleName == null ? "includeApplication" : "includeModule";
                Tr.warning(tc, MessageConstants.OPENAPI_MERGE_UNUSED_INCLUDE_SERVERXML_CWWKO1684W, elementName, unmatchedInclude);
            } else {
                Tr.warning(tc, MessageConstants.OPENAPI_MERGE_UNUSED_INCLUDE_CWWKO1667W, Constants.MERGE_INCLUDE_CONFIG, unmatchedInclude);
            }
        }

        //Now we repeat the process for excluded applications and modules, however this time we only throw a warning message if we get a match on the deployment descriptor's name

        List<ModuleName> excludedNotYetSeen = new ArrayList<>(config.excluded);
        for (Iterator<ModuleName> iterator = excludedNotYetSeen.iterator(); iterator.hasNext();) {
            ModuleName moduleName = iterator.next();
            for (ModuleInfo moduleInfo : moduleInfos) {
                if (matches(moduleName, moduleInfo)) {
                    iterator.remove();
                    break;
                }
            }
        }

        Map<ModuleName, String> excludedNotYetSeenButSeenUnderOldNaming = new HashMap<>();
        if (config.serverxmlmode && ProductInfo.getBetaEdition()) {
            for (Iterator<ModuleName> iterator = excludedNotYetSeen.iterator(); iterator.hasNext();) {
                ModuleName moduleName = iterator.next();
                for (ModuleInfo moduleInfo : moduleInfos) {
                    if (matches(moduleName, moduleInfo, MatchingMode.DEPLOYMENT_DESCRIPTOR_NAME)) {
                        excludedNotYetSeenButSeenUnderOldNaming.put(moduleName,
                                                                    moduleInfo.getApplicationInfo().getDeploymentName());
                        iterator.remove();
                        break;
                    }
                }
            }

            for (ModuleName unmatchedExclude : excludedNotYetSeenButSeenUnderOldNaming.keySet()) {

                String appOrModule = unmatchedExclude.moduleName != null ? "excludeModule" : "excludeApplication";

                Tr.warning(tc, MessageConstants.OPENAPI_USING_WRONG_NAME_SOURCE_CWWKO1680W, appOrModule, unmatchedExclude.appName,
                           excludedNotYetSeenButSeenUnderOldNaming.get(unmatchedExclude));
            }
        }

    }

    protected enum DefaultInclusion {
        ALL, FIRST
    }

    protected DefaultInclusion getDefaultInclusion() {
        return DefaultInclusion.FIRST;
    }

    /**
     *
     * @param name a name we're configured to include in the openAPI documentation
     * @param module an actual module deployed on the server
     */
    private boolean matches(ModuleName name, ModuleInfo module) {
        return matches(name, module, getConfigValues().serverxmlmode ? MatchingMode.SERVER_XML_NAME : MatchingMode.DEPLOYMENT_DESCRIPTOR_NAME);
    }

    private static boolean matches(ModuleName name, ModuleInfo module, MatchingMode matchingMode) {
        if (name.moduleName != null && !name.moduleName.equals(module.getName())) {
            return false;
        }

        if (matchingMode == MatchingMode.SERVER_XML_NAME) {
            if (!name.appName.equals(module.getApplicationInfo().getDeploymentName())) {
                //Deployment name comes from the server.xml and this is the intended name to use when enabling an app
                //getName comes from a metadata file in the war/ear, and is included under the grandfather clause.
                return false;
            }
        } else {
            if (!name.appName.equals(module.getApplicationInfo().getName())) {
                return false;
            }
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
        //Just wipe the configuration so its recreated next time its read
        synchronized (this) {
            configValues = null;
        }
    }

    /*
     * At least one character that is not a '/', non-greedy.
     * Then optionally: Exactly one '/' followed by at least one character.
     */
    private static final Pattern CONFIG_VALUE_NAME_REFERENCE = Pattern.compile("([^/]+?)(/(.+))?");

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
     * @return the list of parsed names
     */
    private static List<ModuleName> parseModuleNames(String nameList, String configKey) {
        List<ModuleName> result = new ArrayList<>();

        for (String configValuePart : nameList.split(",")) {
            Optional<ModuleName> processedName = parseModuleName(configValuePart, configKey);
            processedName.ifPresent(result::add);
        }

        return result;
    }

    /**
     * Parses an app or module name into a {@code ModuleName}
     * <p>
     * The name must be in one of these formats:
     * <ul>
     * <li>appName</li>
     * <li>appName/moduleName</li>
     * </ul>
     *
     * @param name the name to parse
     * @param configKey the name of the config property holding the list (used for reporting errors)
     * @return the parsed name or an empty {@code Optional} if it did not fit the format.
     */
    private static Optional<ModuleName> parseModuleName(String name, String configKey) {
        Matcher m = CONFIG_VALUE_NAME_REFERENCE.matcher(name);

        if (!m.matches()) {
            Tr.warning(tc, MessageConstants.OPENAPI_MERGE_INVALID_NAME_CWWKO1666W, configKey, name);
            return Optional.empty();
        }

        String appName = m.group(1).trim();
        String moduleName = m.group(3);
        if (moduleName != null) {
            moduleName = moduleName.trim();
        }
        return Optional.of(new ModuleName(appName, moduleName));
    }

    @Override
    public int getConfigListenerPriority() {
        return 1;
    }

    private ConfigValues getConfigValues() {
        synchronized (this) {
            if (configValues == null) {
                configValues = new ConfigValues();
            }
            return configValues;
        }
    }

    private class ConfigValues {
        protected final boolean isAll;
        protected final boolean isFirst;
        protected final boolean serverxmlmode;
        protected final List<ModuleName> included;
        protected final List<ModuleName> excluded;

        /**
         * A consistent set of configuration values used for module selection
         * <p>
         * Callers should get an instance by calling {@code getConfigValues()}.
         */
        protected ConfigValues() {
            //defaults
            boolean isAll = false;
            boolean isFirst = false;
            boolean serverxmlmode = false;
            List<ModuleName> included = Collections.emptyList();
            List<ModuleName> excluded = Collections.emptyList();

            Config configFromMPConfig = ConfigProvider.getConfig(ApplicationRegistryImpl.class.getClassLoader());

            OpenAPIServerXMLConfig configFromServerXML = null;
            if (!ProductInfo.getBetaEdition()) {
                serverxmlmode = false;
            } else {
                configFromServerXML = configFromServerXMLProvider.getConfiguration();
                serverxmlmode = configFromServerXML.wasAnyConfigFound();
            }

            if (serverxmlmode) {
                Tr.debug(this, tc, "Acquired includes statement from server.xml");

                if (configFromServerXML.getConfigMode().isPresent()) {
                    ConfigMode configMode = configFromServerXML.getConfigMode().get();
                    if (configMode == OpenAPIServerXMLConfig.ConfigMode.All) {
                        isAll = true;
                    } else if (configMode == OpenAPIServerXMLConfig.ConfigMode.First) {
                        isFirst = true;
                    } else if (configMode == OpenAPIServerXMLConfig.ConfigMode.None) {
                        included = Collections.emptyList();
                    }
                } else if (configFromServerXML.getIncludedAppsAndModules().isPresent()) {
                    List<String> rawNames = configFromServerXML.getIncludedAppsAndModules().get();
                    included = rawNames.stream().map((String rawName) -> parseModuleName(rawName, Constants.MERGE_INCLUDE_CONFIG))
                                       .filter(Optional::isPresent)
                                       .map(Optional::get)
                                       .collect(Collectors.toList());
                } else {
                    DefaultInclusion defaultInclusion = getDefaultInclusion();
                    if (defaultInclusion == DefaultInclusion.FIRST) {
                        isFirst = true;
                    } else {
                        isAll = true;
                    }
                }
            } else {
                String defaultInclude = getDefaultInclusion() == DefaultInclusion.FIRST ? "first" : "all";
                String inclusion = configFromMPConfig.getOptionalValue(Constants.MERGE_INCLUDE_CONFIG, String.class).orElse(defaultInclude);
                Tr.debug(this, tc, "Names in config: " + configFromMPConfig.getPropertyNames());
                Tr.debug(this, tc, "Inclusion read from config: " + inclusion);

                if (inclusion.equals("none")) {
                    included = Collections.emptyList();
                } else if (inclusion.equals("all")) {
                    isAll = true;
                } else if (inclusion.equals("first")) {
                    isFirst = true;
                } else {
                    included = parseModuleNames(inclusion, Constants.MERGE_INCLUDE_CONFIG);
                }
            }

            if (serverxmlmode) {
                if (configFromServerXML.getExcludedAppsAndModules().isPresent()) {
                    List<String> rawNames = configFromServerXML.getExcludedAppsAndModules().get();
                    excluded = rawNames.stream().map((String rawName) -> parseModuleName(rawName, Constants.MERGE_INCLUDE_CONFIG))
                                       .filter(Optional::isPresent)
                                       .map(Optional::get)
                                       .collect(Collectors.toList());
                } else {
                    excluded = Collections.emptyList();
                }
            } else {
                String exclusion = configFromMPConfig.getOptionalValue(Constants.MERGE_EXCLUDE_CONFIG, String.class).orElse("none")
                                                     .trim();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Exclusion read from config: " + exclusion);
                }

                if (exclusion.equals("none")) {
                    excluded = Collections.emptyList();
                } else {
                    excluded = parseModuleNames(exclusion, Constants.MERGE_EXCLUDE_CONFIG);
                }
            }

            this.isAll = isAll;
            this.isFirst = isFirst;
            this.serverxmlmode = serverxmlmode;
            this.included = included;
            this.excluded = excluded;

        }
    }

}
