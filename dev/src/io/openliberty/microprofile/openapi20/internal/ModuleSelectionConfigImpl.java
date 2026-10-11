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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;

import io.openliberty.microprofile.openapi.internal.common.services.OpenAPIAppConfigProvider;
import io.openliberty.microprofile.openapi.internal.common.services.OpenAPIServerXMLConfig;
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
        if (config.inclusionMode == InclusionMode.FIRST) {
            sb.append("useFirstModuleOnly");
        } else {
            if (config.inclusionMode == InclusionMode.ALL) {
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
        return getConfigValues().inclusionMode == InclusionMode.FIRST;
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
        if (config.inclusionMode == InclusionMode.FIRST) {
            return true;
        }

        boolean result = false;
        if (config.inclusionMode == InclusionMode.ALL) {
            result = true;
        } else {
            for (ModuleName name : config.included) {
                if (matches(name, module, config)) {
                    result = true;
                    break;
                }
            }
        }

        if (result) {
            for (ModuleName name : config.excluded) {
                if (matches(name, module, config)) {
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
        if (config.inclusionMode != InclusionMode.NORMAL) {
            return;
        }

        List<ModuleName> includedNotYetSeen = new ArrayList<>(config.included);
        for (Iterator<ModuleName> iterator = includedNotYetSeen.iterator(); iterator.hasNext();) {
            ModuleName moduleName = iterator.next();
            for (ModuleInfo moduleInfo : moduleInfos) {
                if (matches(moduleName, moduleInfo, config)) {
                    iterator.remove();
                    break;
                }
            }
        }

        Map<ModuleName, String> includedNotYetSeenButSeenUnderOldNaming = new HashMap<>();
        if (config.serverxmlmode) {
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
            if (config.serverxmlmode) {
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
                if (matches(moduleName, moduleInfo, config)) {
                    iterator.remove();
                    break;
                }
            }
        }

        Map<ModuleName, String> excludedNotYetSeenButSeenUnderOldNaming = new HashMap<>();
        if (config.serverxmlmode) {
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

    protected enum InclusionMode {
        ALL,
        FIRST,
        NONE,
        NORMAL
    }

    protected InclusionMode getDefaultInclusion() {
        return InclusionMode.FIRST;
    }

    /**
     *
     * @param name a name we're configured to include in the openAPI documentation
     * @param module an actual module deployed on the server
     */
    private boolean matches(ModuleName name, ModuleInfo module, ConfigValues config) {
        return matches(name, module, config.serverxmlmode ? MatchingMode.SERVER_XML_NAME : MatchingMode.DEPLOYMENT_DESCRIPTOR_NAME);
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

        @Override
        public int hashCode() {
            return Objects.hash(appName, moduleName);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ModuleName other = (ModuleName) obj;
            return Objects.equals(appName, other.appName) && Objects.equals(moduleName, other.moduleName);
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
     * @param warningMode whether to warn if there are errors parsing the module names
     * @return the list of parsed names
     */
    private static List<ModuleName> parseModuleNames(String nameList, String configKey, WarningMode warningMode) {
        List<ModuleName> result = new ArrayList<>();

        for (String configValuePart : nameList.split(",")) {
            Optional<ModuleName> processedName = parseModuleName(configValuePart, configKey, warningMode);
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
     * @param warningMode whether to warn if there are errors parsing the module name
     * @return the parsed name or an empty {@code Optional} if it did not fit the format.
     */
    private static Optional<ModuleName> parseModuleName(String name, String configKey, WarningMode warningMode) {
        Matcher m = CONFIG_VALUE_NAME_REFERENCE.matcher(name);

        if (!m.matches()) {
            if (warningMode == WarningMode.LOG_WARNINGS) {
                Tr.warning(tc, MessageConstants.OPENAPI_MERGE_INVALID_NAME_CWWKO1666W, configKey, name);
            }
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
                configValues = readConfigValues();
            }
            return configValues;
        }
    }

    private ConfigValues readConfigValues() {
        Config mpConfig = ConfigProvider.getConfig(ModuleSelectionConfigImpl.class.getClassLoader());
        Optional<ConfigValues> fromServerXml = readFromServerXml();
        if (fromServerXml.isPresent()) {
            Optional<ConfigValues> fromMpConfig = readFromMpConfig(mpConfig, WarningMode.SUPPRESS_WARNINGS);
            if (fromMpConfig.isPresent()) {
                warnMpConfigIgnored(fromServerXml.get(), fromMpConfig.get(), mpConfig);
            }
        }

        return fromServerXml.orElseGet(() -> readFromMpConfig(mpConfig, WarningMode.LOG_WARNINGS).orElse(getDefaultConfig()));
    }

    /**
     * Get the default {@link ConfigValues} object to be used if there's no configuration in server.xml or MP Config
     *
     * @return the default configuration values
     */
    private ConfigValues getDefaultConfig() {
        return new ConfigValues(getDefaultInclusion(), false, Collections.emptyList(), Collections.emptyList());
    }

    /**
     * Warn that certain MP Config properties are being ignored because config is provided in server.xml.
     * <p>
     * This method is only called if we've managed to read config values from MP Config.
     *
     * @param fromServerXml the config values read from server.xml
     * @param fromMpConfig the config values read from MP Config
     * @param mpConfig the config for MP Config
     */
    private void warnMpConfigIgnored(ConfigValues fromServerXml, ConfigValues fromMpConfig, Config mpConfig) {
        Stream<String> propertiesInUse = Stream.of(Constants.MERGE_INCLUDE_CONFIG,
                                                   Constants.MERGE_EXCLUDE_CONFIG)
                                               .filter(prop -> mpConfig.getOptionalValue(prop, String.class).isPresent());
        if (fromServerXml.equivalentTo(fromMpConfig)) {
            propertiesInUse.forEach(prop -> Tr.info(tc, MessageConstants.OPENAPI_MP_CONFIG_REDUNDANT_CWWKO1685I, prop));
        } else {
            propertiesInUse.forEach(prop -> Tr.warning(tc, MessageConstants.OPENAPI_MP_CONFIG_CONFLICTS_CWWKO1686W, prop));
        }
    }

    /**
     * Read the config values from server.xml
     *
     * @return the read config values, or an empty {@code Optional} if there's no module inclusion configuration in server.xml
     */
    private Optional<ConfigValues> readFromServerXml() {
        OpenAPIServerXMLConfig configFromServerXML = configFromServerXMLProvider.getConfiguration();
        if (!configFromServerXML.wasAnyConfigFound()) {
            return Optional.empty();
        }

        InclusionMode inclusionMode;
        List<ModuleName> included;
        List<ModuleName> excluded;

        inclusionMode = configFromServerXML.getConfigMode().map(configMode -> {
            if (configMode == OpenAPIServerXMLConfig.ConfigMode.All) {
                return InclusionMode.ALL;
            } else if (configMode == OpenAPIServerXMLConfig.ConfigMode.First) {
                return InclusionMode.FIRST;
            } else if (configMode == OpenAPIServerXMLConfig.ConfigMode.None) {
                return InclusionMode.NONE;
            } else {
                throw new IllegalArgumentException("configMode");
            }
        }).orElseGet(() -> configFromServerXML.getIncludedAppsAndModules().isPresent() ? InclusionMode.NORMAL : getDefaultInclusion());

        if (inclusionMode == InclusionMode.NORMAL) {
            included = configFromServerXML.getIncludedAppsAndModules()
                                          .map(rawNames -> {
                                              return rawNames.stream()
                                                             .map(rawName -> parseModuleName(rawName, Constants.MERGE_INCLUDE_CONFIG, WarningMode.LOG_WARNINGS))
                                                             .filter(Optional::isPresent)
                                                             .map(Optional::get)
                                                             .collect(Collectors.toList());
                                          })
                                          .orElse(Collections.emptyList());
        } else {
            included = Collections.emptyList();
        }

        excluded = configFromServerXML.getExcludedAppsAndModules()
                                      .map(rawNames -> rawNames.stream()
                                                               .map(rawName -> parseModuleName(rawName, Constants.MERGE_INCLUDE_CONFIG, WarningMode.LOG_WARNINGS))
                                                               .filter(Optional::isPresent)
                                                               .map(Optional::get)
                                                               .collect(Collectors.toList()))
                                      .orElse(Collections.emptyList());

        return Optional.of(new ConfigValues(inclusionMode, true, included, excluded));
    }

    /**
     * Read the config values from MP Config
     *
     * @param configFromMPConfig the current {@link Config}
     * @param warningMode whether or not to emit warnings if the config is in some way invalid
     * @return the read config values, or an empty {@code Optional} if there's no module inclusion configuration in MP Config
     */
    private Optional<ConfigValues> readFromMpConfig(Config configFromMPConfig, WarningMode warningMode) {
        Optional<String> inclusionProperty = configFromMPConfig.getOptionalValue(Constants.MERGE_INCLUDE_CONFIG, String.class);
        Optional<String> exclusionProperty = configFromMPConfig.getOptionalValue(Constants.MERGE_EXCLUDE_CONFIG, String.class);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Inclusion read from config: " + inclusionProperty);
            Tr.debug(this, tc, "Exclusion read from config: " + exclusionProperty);
            Tr.debug(this, tc, "Names in config: " + configFromMPConfig.getPropertyNames());
        }

        if (!inclusionProperty.isPresent() && !exclusionProperty.isPresent()) {
            return Optional.empty();
        }

        InclusionMode inclusionMode;
        List<ModuleName> included;
        List<ModuleName> excluded;

        inclusionMode = inclusionProperty.map(inclusion -> {
            if (inclusion.equals("none")) {
                return InclusionMode.NONE;
            } else if (inclusion.equals("all")) {
                return InclusionMode.ALL;
            } else if (inclusion.equals("first")) {
                return InclusionMode.FIRST;
            } else {
                return InclusionMode.NORMAL;
            }
        }).orElseGet(this::getDefaultInclusion);

        if (inclusionMode == InclusionMode.NORMAL) {
            included = parseModuleNames(inclusionProperty.orElse(""), Constants.MERGE_INCLUDE_CONFIG, warningMode);
        } else {
            included = Collections.emptyList();
        }

        String exclusion = exclusionProperty.orElse("none")
                                            .trim();

        if (exclusion.equals("none")) {
            excluded = Collections.emptyList();
        } else {
            excluded = parseModuleNames(exclusion, Constants.MERGE_EXCLUDE_CONFIG, warningMode);
        }

        return Optional.of(new ConfigValues(inclusionMode, false, included, excluded));
    }

    private enum WarningMode {
        LOG_WARNINGS,
        SUPPRESS_WARNINGS
    }

    /**
     * Holds a snapshot of the module selection configuration read from a particular source.
     */
    private static class ConfigValues {
        protected final InclusionMode inclusionMode;
        protected final boolean serverxmlmode;
        protected final List<ModuleName> included;
        protected final List<ModuleName> excluded;

        public ConfigValues(InclusionMode inclusionMode, boolean serverxmlmode, List<ModuleName> included, List<ModuleName> excluded) {
            // Enforce consistency
            if (inclusionMode != InclusionMode.NORMAL && !included.isEmpty()) {
                throw new IllegalStateException("InclusionMode = " + inclusionMode + ", included = " + included);
            }

            this.inclusionMode = inclusionMode;
            this.serverxmlmode = serverxmlmode;
            this.included = included;
            this.excluded = excluded;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ConfigValues [inclusionMode=");
            builder.append(inclusionMode);
            builder.append(", serverxmlmode=");
            builder.append(serverxmlmode);
            builder.append(", included=");
            builder.append(included);
            builder.append(", excluded=");
            builder.append(excluded);
            builder.append("]");
            return builder.toString();
        }

        /**
         * Returns whether two {@code ConfigValue} objects are equivalent.
         * <p>
         * Config is equivalent if they're equal, ignoring the ordering of {@code included} and {@code excluded}
         *
         * @param other the object to compare to, must not be {@code null}
         * @return {@code true} if {@code other} is equivalent to this object, otherwise {@code false}
         */
        public boolean equivalentTo(ConfigValues other) {
            return Objects.equals(new HashSet<>(excluded), new HashSet<>(other.excluded))
                   && Objects.equals(new HashSet<>(included), new HashSet<>(other.included))
                   && equivalent(inclusionMode, other.inclusionMode);
        }

        private static boolean equivalent(InclusionMode a, InclusionMode b) {
            // NONE and NORMAL are equivalent, since NONE guarantees that included is empty
            // convert NONE to NORMAL before comparing
            InclusionMode aNormal = a == InclusionMode.NONE ? InclusionMode.NORMAL : a;
            InclusionMode bNormal = b == InclusionMode.NONE ? InclusionMode.NORMAL : b;

            return aNormal == bNormal;
        }
    }

}
