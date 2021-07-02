/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.Config;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.app.deploy.ModuleInfo;
import com.ibm.ws.kernel.productinfo.ProductInfo;

import io.openliberty.microprofile.openapi20.utils.Constants;
import io.openliberty.microprofile.openapi20.utils.MessageConstants;

/**
 * Handles reading the merge include/exclude configuration properties and indicating whether a particular module should be included or excluded.
 */
public class ModuleSelectionConfig {
    
    private static final TraceComponent tc = Tr.register(ModuleSelectionConfig.class);

    private boolean isAll = false;
    private boolean isFirst = false;
    private List<ModuleName> included;
    private List<ModuleName> excluded;
    
    /**
     * Builds a {@code ModuleSelectionConfig} based on the given {@code Config}
     * <p>
     * This will read and parse the {@value Constants#MERGE_INCLUDE_CONFIG} and {@value Constants#MERGE_EXCLUDE_CONFIG} config properties.
     * <p>
     * If the config is invalid, this method will output warning messages but still return a usable result object.
     * 
     * @param config the config to read
     * @return the module selection config
     */
    public static ModuleSelectionConfig fromConfig(Config config) {
        ModuleSelectionConfig result = new ModuleSelectionConfig();
        
        // BETA Guard - do no merging, returning the first module only, unless running as beta
        if (!ProductInfo.getBetaEdition()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(null, tc, "Not beta, so setting config to useFirstModuleOnly");
            }
            
            result.isFirst = true;
            return result;
        }
        
        String inclusion = config.getOptionalValue(Constants.MERGE_INCLUDE_CONFIG, String.class).orElse("first").trim();
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(null, tc, "Names in config: " + config.getPropertyNames());
            Tr.debug(null, tc, "Inclusion read from config: " + inclusion);
        }
        
        if (inclusion.equals("none")) {
            result.included = Collections.emptyList();
        } else if (inclusion.equals("all")) {
            result.isAll = true;
        } else if (inclusion.equals("first")) {
            result.isFirst = true;
        } else {
            result.included = parseModuleNames(inclusion, Constants.MERGE_INCLUDE_CONFIG);
        }
        
        String exclusion = config.getOptionalValue(Constants.MERGE_EXCLUDE_CONFIG, String.class).orElse("none").trim();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(null, tc, "Exclusion read from config: " + exclusion);
        }
        
        if (exclusion.equals("none")) {
            result.excluded = Collections.emptyList();
        } else {
            result.excluded = parseModuleNames(exclusion, Constants.MERGE_EXCLUDE_CONFIG);
        }
        
        return result;
    }
    
    @Override
    public String toString() {
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
    public boolean useFirstModuleOnly() {
        return isFirst;
    }
    
    /**
     * Whether the given module should be used to create the OpenAPI document, based on the config
     * 
     * @param module the module to check
     * @return {@code true} if the module should be used, {@code false} otherwise
     */
    public boolean isIncluded(ModuleInfo module) {
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
        
        if (result == true) {
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
    public List<String> findIncludesNotMatchingAnything(Collection<? extends ModuleInfo> moduleInfos) {
        if (isAll || isFirst) {
            return Collections.emptyList();
        }

        List<ModuleName> includedNotYetSeen = new ArrayList<>(included);
        for (Iterator<ModuleName> iterator = includedNotYetSeen.iterator(); iterator.hasNext();) {
            ModuleName moduleName = (ModuleName) iterator.next();
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
        private String appName;
        
        /**
         * The module name, may be {@code null} if the configuration just indicates an application name
         */
        private String moduleName;

        /**
         * @param appName
         * @param moduleName
         */
        public ModuleName(String appName, String moduleName) {
            super();
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

}
