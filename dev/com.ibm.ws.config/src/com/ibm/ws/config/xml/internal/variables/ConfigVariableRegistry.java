/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.config.xml.internal.variables;

import static com.ibm.ws.config.admin.internal.ConfigurationStorageHelper.readMap;
import static com.ibm.ws.config.admin.internal.ConfigurationStorageHelper.toMapOrDictionary;
import static com.ibm.ws.config.admin.internal.ConfigurationStorageHelper.writeMap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.config.xml.ConfigVariables;
import com.ibm.ws.config.xml.LibertyVariable;
import com.ibm.ws.config.xml.internal.StringUtils;
import com.ibm.ws.config.xml.internal.XMLConfigConstants;
import com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinition;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;

public class ConfigVariableRegistry implements VariableRegistry, ConfigVariables {

    private static final TraceComponent tc = Tr.register(ConfigVariableRegistry.class, XMLConfigConstants.TR_GROUP, XMLConfigConstants.NLS_PROPS);

    public static final String UNIQUE = "UNIQUE_";
    public static final String IN_USE = "WLP_VAR_IN_USE";

    private final VariableRegistry registry;
    private final File variableCacheFile;
    // variables explicitly defined in server.xml
    private Map<String, LibertyVariable> configVariables;
    // cache of external variables
    private Map<String, Object> variableCache;
    // cache of variables defined in default configurations
    private Map<String, Object> defaultVariableCache;
    // Variables passed in as command line arguments. These override all other variables.
    private final List<CommandLineVariable> commandLineVariables = new ArrayList<CommandLineVariable>();

    private final StringUtils stringUtils = new StringUtils();

    // variables defined on the files system in $SERVICE_BINDING_ROOT
    private final HashMap<String, ServiceBindingVariable> serviceBindingVariables;

    private final String bindingRoot;

    private final File bindingRootDirectoryFile;

    public ConfigVariableRegistry(VariableRegistry registry, String[] cmdArgs, File variableCacheFile, WsLocationAdmin locationService) {
        this.registry = registry;
        this.bindingRoot = locationService.resolveString(WsLocationConstants.SYMBOL_SERVICE_BINDING_ROOT);
        this.bindingRootDirectoryFile = new File(bindingRoot);
        this.serviceBindingVariables = new HashMap<String, ServiceBindingVariable>();
        this.configVariables = Collections.emptyMap();
        this.variableCacheFile = variableCacheFile;
        if (variableCacheFile != null) {
            loadVariableCache();
        }
        if (this.variableCache == null) {
            this.variableCache = new HashMap<String, Object>();
        }
        if (this.defaultVariableCache == null) {
            this.defaultVariableCache = new HashMap<String, Object>();
        }

        for (String cmdArg : cmdArgs) {
            CommandLineVariable clv = new CommandLineVariable(cmdArg);
            if (clv.isValid()) {
                commandLineVariables.add(clv);
                registry.replaceVariable(clv.getName(), clv.getValue());
            }
        }

        File bindings = new File(bindingRoot);
        if (bindings.exists() && bindings.isDirectory()) {
            for (File f : bindings.listFiles()) {
                if (f.isFile()) {
                    serviceBindingVariables.put(f.getName(), new ServiceBindingVariable(f));
                } else if (f.isDirectory()) {
                    for (File varFile : f.listFiles()) {
                        if (varFile.isFile()) {
                            serviceBindingVariables.put(f.getName() + "/" + varFile.getName(), new ServiceBindingVariable(varFile));
                        }
                    }
                }
            }
        }

    }

    @Trivial
    private static final class CommandLineVariable extends AbstractLibertyVariable {
        private final String name;
        private final String value;
        private final boolean isValid;

        public CommandLineVariable(String cmdArg) {
            int idx = cmdArg.indexOf('=');
            if (!cmdArg.startsWith("--") || (idx <= 2)) {
                // Must start with "--". No equal sign or an equal sign that starts the variable means this is invalid
                isValid = false;
                name = null;
                value = null;
            } else {
                isValid = true;
                name = cmdArg.substring(2, idx);
                value = cmdArg.substring(idx + 1);
            }

        }

        public boolean isValid() {
            return this.isValid;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public String getValue() {
            return this.value;
        }

        @Override
        public boolean isSensitive() {
            return false;
        }

        @Override
        public Source getSource() {
            return Source.COMMAND_LINE;
        }

        @Override
        public String getDefaultValue() {
            return null;
        }

        @Override
        public String toString() {
            // Value is intentionally omitted
            StringBuilder builder = new StringBuilder("ServiceBindingVariable[");
            builder.append("name=").append(name).append(", ");
            builder.append("value=").append(getObscuredValue()).append(", ");
            builder.append("source=").append(Source.SERVICE_BINDING);
            builder.append("]");
            return builder.toString();
        }
    }

    // The value for a ServiceBindingVariable is only read when it is used
    private final class ServiceBindingVariable extends AbstractLibertyVariable {
        final File variableFile;
        String value;

        public ServiceBindingVariable(File varFile) {
            this.variableFile = varFile;
        }

        @Override
        public String getValue() {
            if (this.value != null)
                return this.value;

            try {
                List<String> contents = Files.readAllLines(variableFile.toPath());
                if (contents != null && !contents.isEmpty())
                    this.value = contents.get(0);
            } catch (IOException ex) {
                Tr.error(tc, "error.bad.variable.file", variableFile.getAbsolutePath());
            }
            return this.value;
        }

        @Override
        public String getName() {
            return getServiceBindingVariableName(variableFile);
        }

        @Override
        public boolean isSensitive() {
            return true;
        }

        @Override
        public Source getSource() {
            return Source.SERVICE_BINDING;
        }

        @Override
        public String getDefaultValue() {
            return null;
        }

        @Override
        public String toString() {
            // Value is intentionally omitted
            StringBuilder builder = new StringBuilder("ServiceBindingVariable[");
            builder.append("name=").append(getServiceBindingVariableName(variableFile)).append(", ");
            builder.append("source=").append(Source.SERVICE_BINDING);
            builder.append("]");
            return builder.toString();
        }
    }

    /*
     * Override system variables.
     */
    public void updateSystemVariables(@Sensitive Map<String, LibertyVariable> newVariables) {
        for (String variableName : configVariables.keySet()) {
            if (!newVariables.containsKey(variableName)) {
                registry.removeVariable(variableName);
            }
        }
        for (Map.Entry<String, LibertyVariable> entry : newVariables.entrySet()) {
            String variableName = entry.getKey();
            String variableValue = entry.getValue().getValue();
            if (variableValue != null)
                registry.replaceVariable(variableName, variableValue);
            else
                registry.removeVariable(variableName);
        }
        configVariables = newVariables;

        // Override with command line variables if necessary
        for (CommandLineVariable clv : commandLineVariables) {
            registry.replaceVariable(clv.getName(), clv.getValue());
        }

        // Add Service Binding Variables ( if not present)
        for (LibertyVariable v : serviceBindingVariables.values()) {
            registry.addVariable(v.getName(), v.getValue());
        }
    }

    /*
     * Update variable cache on disk.
     */
    public synchronized void updateVariableCache(Map<String, Object> variables) {
        boolean dirty = false;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String variableName = entry.getKey();
            // skip any variables defined with values in server.xml
            if (configVariables.containsKey(variableName)) {
                LibertyVariable var = configVariables.get(variableName);
                if (var.getDefaultValue() == null)
                    continue;
            }
            Object variableValue = entry.getValue();
            if (!isVariableCached(variableName, variableValue)) {
                variableCache.put(variableName, variableValue);
                dirty = true;
            }
        }
        if (dirty) {
            saveVariableCache();
        }
    }

    private boolean isVariableCached(String variableName, Object variableValue) {
        if (variableCache.containsKey(variableName)) {
            Object cachedVariableValue = variableCache.get(variableName);
            return isEqual(cachedVariableValue, variableValue);
        } else {
            return false;
        }
    }

    private static boolean isEqual(Object oldVariableValue, Object newVariableValue) {
        if (oldVariableValue == null) {
            return newVariableValue == null;
        } else if (newVariableValue == null) {
            return false;
        } else {
            return oldVariableValue.equals(newVariableValue);
        }
    }

    /*
     * Checks cached variable values against the current variable values.
     * Returns true if at least one variable has changed. False, otherwise.
     */
    public synchronized boolean variablesChanged() {
        for (Map.Entry<String, Object> entry : variableCache.entrySet()) {
            String variableName = entry.getKey();
            Object oldVariableValue = entry.getValue();
            Object newVariableValue = lookupVariable(variableName);
            if (newVariableValue == null) {
                newVariableValue = defaultVariableCache.get(variableName);
            }

            if (newVariableValue == null) {
                newVariableValue = lookupVariableFromAdditionalSources(variableName);
            }

            if (!isEqual(oldVariableValue, newVariableValue)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Variable " + variableName + " has changed. ");
                }
                return true;
            }
        }
        return false;
    }

    public String lookupVariableFromAdditionalSources(String variableName) {

        String value = null;

        // Try to resolve as an env variable ( resolve env.var-Name )

        value = lookupVariable("env." + variableName);

        // Try to resolve with non-alpha characters replaced ( resolve env.var_Name )
        if (value == null) {
            variableName = stringUtils.replaceNonAlpha(variableName);
            value = lookupVariable("env." + variableName);
        }

        // Try to resolve with upper case ( resolve env.VAR_NAME )
        if (value == null) {
            variableName = variableName.toUpperCase();
            value = lookupVariable("env." + variableName);
        }

        return value;

    }

    /*
     * Clear variable cache.
     */
    public synchronized void clearVariableCache() {
        variableCache.clear();
        defaultVariableCache.clear();
    }

    /**
     * Resolve the given variable.
     *
     * Formerly, this value was path-normalized (i.e, "//" is replaced
     * with "/" and any trailing "/" are removed from the value).
     *
     * This has changed, at least temporarily, to not do path normalization for variables.
     *
     * @param variableName
     * @return The resolved value or null if the variable doesn't exist
     */
    @Sensitive
    public String lookupVariable(String variableName) {

        String varReference = XMLConfigConstants.VAR_OPEN + variableName + XMLConfigConstants.VAR_CLOSE;
        String resolvedVar = registry.resolveRawString(varReference);

        if (varReference.equalsIgnoreCase(resolvedVar)) {
            LibertyVariable var = serviceBindingVariables.get(variableName);
            return var == null ? null : var.getValue();
        }

        return resolvedVar;

    }

    @FFDCIgnore({ Exception.class })
    private synchronized void loadVariableCache() {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(variableCacheFile)))) {
            readMap(in, toMapOrDictionary(this.variableCache = new HashMap<>()));
            readMap(in, toMapOrDictionary(this.defaultVariableCache = new HashMap<>()));
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "loadVariableCache():  Exception = " + e.getMessage());
            }
        }
    }

    @FFDCIgnore({ IOException.class })
    private synchronized void saveVariableCache() {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(variableCacheFile)))) {
            writeMap(out, toMapOrDictionary(variableCache));
            writeMap(out, toMapOrDictionary(defaultVariableCache));
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "saveVariableCache():  Exception = " + e.getMessage());
            }
            FFDCFilter.processException(e, ConfigVariableRegistry.class.getName(), "saveVariableCache(): Exception = " + e.getMessage());
        }
    }

    public synchronized void setDefaultVariables(Map<String, LibertyVariable> variables) {
        boolean dirty = false;
        for (Map.Entry<String, LibertyVariable> entry : variables.entrySet()) {
            String variableName = entry.getKey();
            String currentValue = lookupVariable(variableName);
            if (currentValue == null) {
                // variable is not set anywhere, so set it to default value
                String defaultValue = entry.getValue().getValue();
                registry.addVariable(variableName, defaultValue);
                Object oldValue = defaultVariableCache.put(variableName, defaultValue);
                dirty = (oldValue == null) ? true : !oldValue.equals(defaultValue);
            }
        }
        if (dirty) {
            saveVariableCache();
        }
    }

    @Override
    public boolean addVariable(String variable, String value) {
        return registry.addVariable(variable, value);
    }

    @Override
    public void replaceVariable(String variable, String value) {
        registry.replaceVariable(variable, value);
    }

    @Override
    public String resolveString(String variableName) {
        return registry.resolveString(variableName);
    }

    @Override
    public String resolveRawString(String variableName) {
        return registry.resolveRawString(variableName);
    }

    @Override
    public void removeVariable(String symbol) {
        registry.removeVariable(symbol);
    }

    /**
     * Returns a String that we use to check whether a value is unique across all ibm:unique attributes
     * (eg UNIQUE_jndiName_value )
     */
    public String getUniqueVarString(ExtendedAttributeDefinition attrDef, String value) {
        return UNIQUE + attrDef.getUniqueCategory() + value;
    }

    /**
     * Removes a unique variable to indicate that the value is no longer being used for an ibm;unique attribute
     */
    public void removeUniqueVariable(ExtendedAttributeDefinition attrDef, String attributeValue) {
        removeVariable(getUniqueVarString(attrDef, attributeValue));

    }

    /**
     * adds the unique variable to the registry to mark it as being used
     */
    public void addVariableInUse(String variable) {
        addVariable(variable, IN_USE);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.config.xml.Variables#getUserDefinedVariables()
     */
    @Override
    @Sensitive
    public Map<String, String> getUserDefinedVariables() {
        HashMap<String, String> userDefinedVariables = new HashMap<String, String>();

        for (Entry<String, ServiceBindingVariable> entry : serviceBindingVariables.entrySet()) {
            userDefinedVariables.put(entry.getKey(), entry.getValue().getValue());
        }

        for (Map.Entry<String, LibertyVariable> entry : configVariables.entrySet()) {
            LibertyVariable var = entry.getValue();
            if (var.getValue() != null) {
                userDefinedVariables.put(var.getName(), var.getValue());
            }
        }
        for (CommandLineVariable clVar : commandLineVariables) {
            userDefinedVariables.put(clVar.getName(), clVar.getValue());
        }
        return userDefinedVariables;
    }

    /**
     * Returns the defaultValue from the variable definition, or null if it doesn't exist.
     */
    public String lookupVariableDefaultValue(String variableName) {
        LibertyVariable cv = configVariables.get(variableName);
        return cv == null ? null : cv.getDefaultValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.config.xml.ConfigVariables#getUserDefinedVariableDefaults()
     */
    @Override
    public Map<String, String> getUserDefinedVariableDefaults() {
        HashMap<String, String> userDefinedVariables = new HashMap<String, String>();
        for (Map.Entry<String, LibertyVariable> entry : configVariables.entrySet()) {
            LibertyVariable var = entry.getValue();
            if (var.getValue() == null && var.getDefaultValue() != null) {
                userDefinedVariables.put(var.getName(), var.getDefaultValue());
            }
        }
        return userDefinedVariables;
    }

    @Override
    public Collection<LibertyVariable> getAllLibertyVariables() {
        Collection<LibertyVariable> variables = new ArrayList<LibertyVariable>();
        variables.addAll(configVariables.values());
        variables.addAll(serviceBindingVariables.values());
        variables.addAll(commandLineVariables);
        return Collections.unmodifiableCollection(variables);
    }

    public boolean removeServiceBindingVariable(String name) {
        LibertyVariable var = serviceBindingVariables.remove(name);
        return var == null ? false : true;
    }

    public void addServiceBindingVariable(File value) {
        ServiceBindingVariable sbv = new ServiceBindingVariable(value);
        serviceBindingVariables.put(sbv.getName(), sbv);

    }

    public void modifyServiceBindingVariable(File f) {
        ServiceBindingVariable var = new ServiceBindingVariable(f);
        serviceBindingVariables.put(var.getName(), var);
    }

    @Override
    public String getServiceBindingRootDirectory() {
        return this.bindingRoot;
    }

    public String getServiceBindingVariableName(File f) {
        String name = f.getName();

        if ( f.getParentFile().compareTo(bindingRootDirectoryFile) == 0)
            return name;

        return f.getParentFile().getName() + "/" + name;
    }
}
