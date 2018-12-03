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
package com.ibm.ws.config.xml.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.config.xml.ConfigVariables;
import com.ibm.ws.config.xml.internal.metatype.ExtendedAttributeDefinition;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.kernel.service.location.VariableRegistry;

class ConfigVariableRegistry implements VariableRegistry, ConfigVariables {

    private static final TraceComponent tc = Tr.register(ConfigVariableRegistry.class, XMLConfigConstants.TR_GROUP, XMLConfigConstants.NLS_PROPS);

    public static final String UNIQUE = "UNIQUE_";
    public static final String IN_USE = "WLP_VAR_IN_USE";

    private final VariableRegistry registry;
    private final File variableCacheFile;
    // variables explicitly defined in server.xml
    private Map<String, ConfigVariable> configVariables;
    // cache of external variables
    private Map<String, Object> variableCache;
    // cache of variables defined in default configurations
    private Map<String, Object> defaultVariableCache;
    // Variables passed in as command line arguments. These override all other variables.
    private final List<CommandLineVariable> commandLineVariables = new ArrayList<CommandLineVariable>();

    public ConfigVariableRegistry(VariableRegistry registry, String[] cmdArgs, File variableCacheFile) {
        this.registry = registry;
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

    }

    @Trivial
    private static final class CommandLineVariable {
        private final String name;
        private final String value;
        private final boolean isValid;

        /**
         * @param cmdArg
         */
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

        /**
         * @return
         */
        public boolean isValid() {
            return this.isValid;
        }

        /**
         * @return
         */
        public String getName() {
            return this.name;
        }

        /**
         * @return
         */
        public String getValue() {
            return this.value;
        }

    }

    /*
     * Override system variables.
     */
    public void updateSystemVariables(Map<String, ConfigVariable> newVariables) {
        for (String variableName : configVariables.keySet()) {
            if (!newVariables.containsKey(variableName)) {
                registry.removeVariable(variableName);
            }
        }
        for (Map.Entry<String, ConfigVariable> entry : newVariables.entrySet()) {
            String variableName = entry.getKey();
            String variableValue = entry.getValue().getValue();
            registry.replaceVariable(variableName, variableValue);
        }
        configVariables = newVariables;

        // Override with command line variables if necessary
        for (CommandLineVariable clv : commandLineVariables) {
            registry.replaceVariable(clv.getName(), clv.getValue());
        }
    }

    /*
     * Update variable cache on disk.
     */
    public synchronized void updateVariableCache(Map<String, Object> variables) {
        boolean dirty = false;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String variableName = entry.getKey();
            // skip any variables defined in server.xml
            if (configVariables.containsKey(variableName)) {
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
            if (!isEqual(oldVariableValue, newVariableValue)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Variable " + variableName + " has changed. Old value: " + oldVariableValue + " New value: " + newVariableValue);
                }
                return true;
            }
        }
        return false;
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
    public String lookupVariable(String variableName) {

        String varReference = XMLConfigConstants.VAR_OPEN + variableName + XMLConfigConstants.VAR_CLOSE;
        String resolvedVar = registry.resolveRawString(varReference);

        return varReference.equalsIgnoreCase(resolvedVar) ? null : resolvedVar;

    }

    @SuppressWarnings("unchecked")
    @FFDCIgnore({ Exception.class })
    private synchronized void loadVariableCache() {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            fis = new FileInputStream(variableCacheFile);
            ois = new ObjectInputStream(fis);
            this.variableCache = (Map<String, Object>) ois.readObject();
            this.defaultVariableCache = (Map<String, Object>) ois.readObject();
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "loadVariableCache():  Exception = " + e.getMessage());
            }
        } finally {
            ConfigUtil.closeIO(ois);
            ConfigUtil.closeIO(fis);
        }
    }

    @FFDCIgnore({ IOException.class })
    private synchronized void saveVariableCache() {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = new FileOutputStream(variableCacheFile, false);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(variableCache);
            oos.writeObject(defaultVariableCache);
        } catch (IOException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "saveVariableCache():  Exception = " + e.getMessage());
            }
            FFDCFilter.processException(e, ConfigVariableRegistry.class.getName(), "saveVariableCache(): Exception = " + e.getMessage());
        } finally {
            ConfigUtil.closeIO(oos);
            ConfigUtil.closeIO(fos);
        }
    }

    public synchronized void setDefaultVariables(Map<String, ConfigVariable> variables) {
        boolean dirty = false;
        for (Map.Entry<String, ConfigVariable> entry : variables.entrySet()) {
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
    public String resolveString(String string) {
        return registry.resolveString(string);
    }

    @Override
    public String resolveRawString(String string) {
        return registry.resolveRawString(string);
    }

    @Override
    public void removeVariable(String symbol) {
        registry.removeVariable(symbol);
    }

    /**
     * @param attrDef
     * @param value
     * @return
     */
    public String getUniqueVarString(ExtendedAttributeDefinition attrDef, String value) {
        return UNIQUE + attrDef.getUniqueCategory() + value;
    }

    /**
     * @param attrDef
     * @param attributeValue
     */
    public void removeUniqueVariable(ExtendedAttributeDefinition attrDef, String attributeValue) {
        removeVariable(getUniqueVarString(attrDef, attributeValue));

    }

    /**
     * @param variable
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
    public Map<String, String> getUserDefinedVariables() {
        HashMap<String, String> userDefinedVariables = new HashMap<String, String>();
        for (Map.Entry<String, ConfigVariable> entry : configVariables.entrySet()) {
            ConfigVariable var = entry.getValue();
            userDefinedVariables.put(var.getName(), var.getValue());
        }
        for (CommandLineVariable clVar : commandLineVariables) {
            userDefinedVariables.put(clVar.getName(), clVar.getValue());
        }
        return userDefinedVariables;
    }

}
