/*******************************************************************************
 * Copyright (c) 2012, 2022 IBM Corporation and others.
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.StringTokenizer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.config.xml.ConfigVariables;
import com.ibm.ws.config.xml.LibertyVariable;
import com.ibm.ws.config.xml.internal.ConfigComparator.DeltaType;
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
    // variables explicitly defined in bundle defaultInstances files
    private Map<String, LibertyVariable> defaultConfigVariables;

    // Immutable map of name to value for user defined variables, replaced when updated, never modified
    private volatile Map<String, String> userDefinedVariableMap;
    // Immutable map of name to defaultValue for user defined variables, replaced when updated, never modified
    private volatile Map<String, String> userDefinedVariableDefaultsMap;

    // cache of external variables
    private Map<String, Object> variableCache;
    // cache of variables defined in default configurations
    private Map<String, Object> defaultVariableCache;
    // Variables passed in as command line arguments. These override all other variables.
    private final List<CommandLineVariable> commandLineVariables = new ArrayList<CommandLineVariable>();

    private final StringUtils stringUtils = new StringUtils();

    // variables defined on the files system in $VARIABLE_SOURCE_DIRS
    private final HashMap<String, FileSystemVariable> fileSystemVariables;

    private final List<String> fileVariableRootDirs = new ArrayList<String>();

    private final List<File> fsVarRootDirectoryFiles = new ArrayList<File>();

    public ConfigVariableRegistry(VariableRegistry registry, String[] cmdArgs, File variableCacheFile, WsLocationAdmin locationService) {
        this.registry = registry;
        String fileVariableDirString = locationService.resolveString(WsLocationConstants.SYMBOL_VARIABLE_SOURCE_DIRS);
        StringTokenizer st = new StringTokenizer(fileVariableDirString, File.pathSeparator);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            fileVariableRootDirs.add(token);
            fsVarRootDirectoryFiles.add(new File(token));
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Adding file system variable source: " + token);
            }
        }

        this.fileSystemVariables = new HashMap<String, FileSystemVariable>();
        this.configVariables = Collections.emptyMap();
        this.defaultConfigVariables = Collections.emptyMap();
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

        for (File bindings : fsVarRootDirectoryFiles) {
            if (bindings.exists() && bindings.isDirectory()) {
                for (File f : bindings.listFiles()) {
                    if (f.isFile()) {
                        if (f.getName().endsWith(".properties")) {
                            try (FileInputStream fis = new FileInputStream(f)) {
                                Properties props = new Properties();
                                props.load(fis);
                                props.forEach((key, value) -> {
                                    fileSystemVariables.put((String) key, new FileSystemVariable(f, (String) key, (String) value));
                                });
                            } catch (IOException ex) {
                                Tr.error(tc, "error.bad.variable.file", f.getAbsolutePath());
                            }
                        } else {
                            fileSystemVariables.put(f.getName(), new FileSystemVariable(f));
                        }
                    } else if (f.isDirectory()) {
                        for (File varFile : f.listFiles()) {
                            if (varFile.isFile()) {
                                fileSystemVariables.put(f.getName() + "/" + varFile.getName(), new FileSystemVariable(varFile));
                            }
                        }
                    }
                }
            }
        }

        updateUserDefinedVariableMap();
        updateUserDefinedVariableDefaultsMap();
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
            StringBuilder builder = new StringBuilder("CommandLineVariable[");
            builder.append("name=").append(name).append(", ");
            builder.append("value=").append(getObscuredValue()).append(", ");
            builder.append("source=").append(Source.COMMAND_LINE);
            builder.append("]");
            return builder.toString();
        }
    }

    // The value for a FileSystemVariable is only read when it is used (except for variables from
    // property files which we process immediately)
    protected final class FileSystemVariable extends AbstractLibertyVariable {
        final File variableFile;
        String value;
        String name;
        String propertiesFileName;

        public FileSystemVariable(File varFile) {
            this.variableFile = varFile;
        }

        public FileSystemVariable(File propertiesFile, String name, String value) {
            this.name = name;
            this.value = value;
            this.propertiesFileName = propertiesFile.getName();
            this.variableFile = null;
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
            if (this.name == null)
                return getFileSystemVariableName(variableFile);

            return this.name;
        }

        @Override
        public boolean isSensitive() {
            return true;
        }

        @Override
        public Source getSource() {
            return Source.FILE_SYSTEM;
        }

        @Override
        public String getDefaultValue() {
            return null;
        }

        public String getPropertiesFileName() {
            return this.propertiesFileName;
        }

        @Override
        public String toString() {
            // Value is intentionally omitted
            StringBuilder builder = new StringBuilder("FileSystemVariable[");
            builder.append("name=").append(getName()).append(", ");
            builder.append("source=").append(Source.FILE_SYSTEM);
            builder.append("]");
            return builder.toString();
        }
    }

    @Sensitive
    public Map<String, LibertyVariable> getConfigVariables() {
        return this.configVariables;
    }

    @Sensitive
    public Map<String, LibertyVariable> getDefaultConfigVariables() {
        return this.defaultConfigVariables;
    }

    /*
     * Override system variables.
     */
    public void updateSystemVariables(@Sensitive Map<String, LibertyVariable> newVariables) {
        // Remove any variables that were removed from config. Replace with a defaultInstance value if it exists.
        for (String variableName : configVariables.keySet()) {
            if (!newVariables.containsKey(variableName)) {
                LibertyVariable defaultInstanceVar = defaultConfigVariables.get(variableName);
                if (defaultInstanceVar == null || defaultInstanceVar.getValue() == null)
                    registry.removeVariable(variableName);
                else
                    registry.replaceVariable(variableName, defaultInstanceVar.getValue());
            }
        }

        // Replace values that have changed
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

        // Add File System Variables ( if not present)
        for (LibertyVariable v : fileSystemVariables.values()) {
            registry.addVariable(v.getName(), v.getValue());
        }

        updateUserDefinedVariableMap();
        updateUserDefinedVariableDefaultsMap();
    }

    /*
     * Update variable cache on disk.
     */
    public synchronized void updateVariableCache(Map<String, Object> variables) {
        boolean dirty = false;

        String variableSrcDirsKey = WsLocationConstants.LOC_VARIABLE_SOURCE_DIRS;
        if (variableCache.get(variableSrcDirsKey) == null) {
            Object variableSrcDirsValue = lookupVariable(variableSrcDirsKey);
            variableCache.put(variableSrcDirsKey, variableSrcDirsValue);
            dirty = true;
        }
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
            return Objects.equals(cachedVariableValue, variableValue);
        } else {
            return false;
        }
    }

    /*
     * Checks cached variable values against the current variable values.
     * Returns true if at least one variable has changed. False, otherwise.
     */
    public synchronized Map<String, DeltaType> variablesChanged() {
        Map<String, DeltaType> changed = null;
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

            if (newVariableValue == null) {
                LibertyVariable cv = configVariables.get(variableName);
                newVariableValue = cv == null ? null : cv.getDefaultValue();
            }

            DeltaType deltaType = getDeltaType(oldVariableValue, newVariableValue);
            if (deltaType != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Variable " + variableName + " has changed. ");
                }
                if (changed == null) {
                    changed = new HashMap<>();
                }
                changed.put(variableName, deltaType);
            }
        }
        return changed == null ? Collections.emptyMap() : changed;
    }

    DeltaType getDeltaType(Object oldValue, Object newValue) {
        if (oldValue == null) {
            return newValue != null ? DeltaType.ADDED : null;
        }
        if (newValue == null) {
            return DeltaType.REMOVED;
        }
        return oldValue.equals(newValue) ? null : DeltaType.MODIFIED;
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
            return null;
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

    @Sensitive
    public synchronized void setDefaultVariables(Map<String, LibertyVariable> variables) {
        boolean dirty = false;

        for (Map.Entry<String, LibertyVariable> entry : defaultConfigVariables.entrySet()) {
            // If the variable doesn't exist in the new set of defaultInstances and doesn't exist in server.xml, remove it from
            // the registry
            if (!variables.containsKey(entry.getKey())) {
                LibertyVariable configuredVar = configVariables.get(entry.getKey());
                if (configuredVar == null && entry.getValue().getValue() != null) {
                    registry.removeVariable(entry.getKey());
                    defaultVariableCache.remove(entry.getKey());
                    dirty = true;
                }
            }
        }

        defaultConfigVariables = variables;

        for (Map.Entry<String, LibertyVariable> entry : variables.entrySet()) {
            String variableName = entry.getKey();
            String variableValue = entry.getValue().getValue();

            // If there is no server.xml version, update the registry
            LibertyVariable configVariable = configVariables.get(entry.getKey());
            if (configVariable == null && variableValue != null) {
                registry.replaceVariable(variableName, variableValue);
                Object oldValue = defaultVariableCache.put(variableName, variableValue);
                dirty = (oldValue == null) ? true : !oldValue.equals(variableValue);
            }
        }
        if (dirty) {
            saveVariableCache();
        }
        updateUserDefinedVariableMap();
        updateUserDefinedVariableDefaultsMap();
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
        return userDefinedVariableMap;
    }

    @Sensitive
    private void updateUserDefinedVariableMap() {
        HashMap<String, String> userDefinedVariables = new HashMap<String, String>();

        for (Entry<String, FileSystemVariable> entry : fileSystemVariables.entrySet()) {
            userDefinedVariables.put(entry.getKey(), entry.getValue().getValue());
        }

        for (Map.Entry<String, LibertyVariable> entry : defaultConfigVariables.entrySet()) {
            LibertyVariable var = entry.getValue();
            if (var.getValue() != null) {
                userDefinedVariables.put(var.getName(), var.getValue());
            }
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

        userDefinedVariableMap = Collections.unmodifiableMap(userDefinedVariables);
    }

    /**
     * Returns the defaultValue from the variable definition, or null if it doesn't exist.
     */
    public String lookupVariableDefaultValue(String variableName) {
        LibertyVariable cv = configVariables.get(variableName);

        if (cv == null)
            cv = defaultConfigVariables.get(variableName);

        return cv == null ? null : cv.getDefaultValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.config.xml.ConfigVariables#getUserDefinedVariableDefaults()
     */
    @Override
    public Map<String, String> getUserDefinedVariableDefaults() {
        return userDefinedVariableDefaultsMap;
    }

    private void updateUserDefinedVariableDefaultsMap() {
        HashMap<String, String> userDefinedVariables = new HashMap<String, String>();
        for (Map.Entry<String, LibertyVariable> entry : configVariables.entrySet()) {
            LibertyVariable var = entry.getValue();
            if (var.getDefaultValue() != null) {
                userDefinedVariables.put(var.getName(), var.getDefaultValue());
            }
        }
        for (Map.Entry<String, LibertyVariable> entry : defaultConfigVariables.entrySet()) {
            LibertyVariable var = entry.getValue();
            if (!userDefinedVariables.containsKey(entry.getKey())) {
                // Add the defaultValue if there is no server.xml version
                if (var.getDefaultValue() != null) {
                    userDefinedVariables.put(var.getName(), var.getDefaultValue());
                }
            }
        }
        userDefinedVariableDefaultsMap = Collections.unmodifiableMap(userDefinedVariables);
    }

    @Override
    public Collection<LibertyVariable> getAllLibertyVariables() {
        Collection<LibertyVariable> variables = new ArrayList<LibertyVariable>();
        variables.addAll(configVariables.values());
        variables.addAll(fileSystemVariables.values());
        variables.addAll(commandLineVariables);
        return Collections.unmodifiableCollection(variables);
    }

    public void removeFileSystemVariableDeletes(Collection<File> deletedFiles, Map<String, DeltaType> deltaMap) {
        for (File f : deletedFiles) {
            if (f.getName().endsWith(".properties")) {
                Iterator<FileSystemVariable> iter = fileSystemVariables.values().iterator();
                while (iter.hasNext()) {
                    FileSystemVariable var = iter.next();
                    if (f.getName().equals(var.getPropertiesFileName())) {
                        iter.remove();
                        deltaMap.put(var.getName(), DeltaType.REMOVED);
                        registry.removeVariable(var.getName());
                    }
                }
            } else {
                // Only create a delta if a variable is actually removed (otherwise it's a directory)
                if (removeFileSystemVariable(getFileSystemVariableName(f))) {
                    deltaMap.put(f.getName(), DeltaType.REMOVED);
                }
            }
        }
        updateUserDefinedVariableMap();
    }

    private boolean removeFileSystemVariable(String name) {
        LibertyVariable var = fileSystemVariables.remove(name);
        if (var == null)
            return false;

        registry.removeVariable(name);
        return true;
    }

    public void addFileSystemVariableCreates(Collection<File> createdFiles, Map<String, DeltaType> deltaMap) {
        for (File file : createdFiles) {
            if (file.isFile()) {

                if (file.getName().endsWith(".properties")) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        Properties props = new Properties();
                        props.load(fis);
                        for (String key : props.stringPropertyNames()) {
                            FileSystemVariable sbv = new FileSystemVariable(file, key, props.getProperty(key));
                            fileSystemVariables.put(sbv.getName(), sbv);
                            deltaMap.put(key, DeltaType.ADDED);
                        }
                    } catch (IOException ex) {
                        Tr.error(tc, "error.bad.variable.file", file.getAbsolutePath());
                    }
                } else {
                    FileSystemVariable sbv = new FileSystemVariable(file);
                    fileSystemVariables.put(sbv.getName(), sbv);
                    deltaMap.put(sbv.getName(), DeltaType.ADDED);
                }
            }
        }

        updateUserDefinedVariableMap();
    }

    public void modifyFileSystemVariables(Collection<File> modifiedFiles, Map<String, DeltaType> deltaMap) {
        for (File f : modifiedFiles) {
            if (f.isFile()) {
                if (f.getName().endsWith(".properties")) {
                    try (FileInputStream fis = new FileInputStream(f)) {
                        Properties props = new Properties();
                        props.load(fis);

                        // Remove all existing variables that came from this properties file
                        Iterator<FileSystemVariable> iter = fileSystemVariables.values().iterator();
                        while (iter.hasNext()) {
                            FileSystemVariable var = iter.next();
                            if (f.getName().equals(var.getPropertiesFileName())) {
                                iter.remove();
                                deltaMap.put(var.getName(), DeltaType.REMOVED);
                                registry.removeVariable(var.getName());
                            }
                        }

                        // Add the current values from the properties file
                        for (String key : props.stringPropertyNames()) {
                            FileSystemVariable sbv = new FileSystemVariable(f, key, props.getProperty(key));
                            fileSystemVariables.put(sbv.getName(), sbv);
                            deltaMap.put(key, DeltaType.MODIFIED);
                        }
                    } catch (IOException ex) {
                        Tr.error(tc, "error.bad.variable.file", f.getAbsolutePath());
                    }
                } else {
                    FileSystemVariable var = new FileSystemVariable(f);
                    fileSystemVariables.put(var.getName(), var);
                    // The variable will be added back by updateSystemVariables, but remove it for now.
                    registry.removeVariable(var.getName());
                    deltaMap.put(var.getName(), DeltaType.MODIFIED);
                }
            }
        }
        updateUserDefinedVariableMap();
    }

    @Override
    public List<String> getFileSystemVariableRootDirectories() {
        return this.fileVariableRootDirs;
    }

    public String getFileSystemVariableName(File f) {
        String name = f.getName();

        // If the parent file is one of our root directories, just return the file name
        for (File fsVarRootDirectoryFile : this.fsVarRootDirectoryFiles) {
            if (f.getParentFile().compareTo(fsVarRootDirectoryFile) == 0)
                return name;
        }

        // Otherwise, return the parent directory name + / + file name
        return f.getParentFile().getName() + "/" + name;
    }
}
