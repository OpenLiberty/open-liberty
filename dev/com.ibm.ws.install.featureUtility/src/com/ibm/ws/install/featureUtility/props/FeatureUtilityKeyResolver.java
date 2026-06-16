/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.install.featureUtility.props;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.wsspi.security.crypto.KeyStringResolver;

/**
 * KeyStringResolver implementation for featureUtility that resolves encryption keys
 * from bootstrap.properties, server.env, and environment variables.
 * 
 * This resolver enables featureUtility to properly decrypt AES-encrypted passwords
 * that use custom encryption keys specified via wlp.password.encryption.key or
 * wlp.aes.encryption.key properties.
 */
public class FeatureUtilityKeyResolver implements KeyStringResolver {

    private static final Logger logger = Logger.getLogger(FeatureUtilityKeyResolver.class.getName());
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    
    private final Map<String, String> resolvedVariables;

    /**
     * Creates a new FeatureUtilityKeyResolver.
     * Loads encryption key variables from:
     * 1. Environment variables
     * 2. bootstrap.properties (if server name is provided)
     * 3. server.env (if server name is provided)
     */
    public FeatureUtilityKeyResolver() {
        this(null, null);
    }

    /**
     * Creates a new FeatureUtilityKeyResolver with featureUtility.properties.
     * Loads encryption key variables from:
     * 1. Environment variables
     * 2. featureUtility.properties (if provided)
     * 3. bootstrap.properties (if server name is provided)
     * 4. server.env (if server name is provided)
     *
     * @param featureUtilityProps the featureUtility.properties to load encryption keys from,
     *                            or null to skip loading from featureUtility.properties
     */
    public FeatureUtilityKeyResolver(Properties featureUtilityProps) {
        this(null, featureUtilityProps);
    }

    /**
     * Creates a new FeatureUtilityKeyResolver for a specific server.
     *
     * @param serverName the server name to load bootstrap.properties and server.env from,
     *                   or null to only use environment variables
     * @param featureUtilityProps the featureUtility.properties to load encryption keys from,
     *                            or null to skip loading from featureUtility.properties
     */
    public FeatureUtilityKeyResolver(String serverName, Properties featureUtilityProps) {
        this.resolvedVariables = new HashMap<>();
        loadVariables(serverName, featureUtilityProps);
    }

    /**
     * Loads encryption key variables from various sources.
     *
     * @param serverName the server name, or null
     * @param featureUtilityProps the featureUtility.properties, or null
     */
    private void loadVariables(String serverName, Properties featureUtilityProps) {
        // 1. Load from environment variables
        loadFromEnvironment();
        
        // 2. Load from featureUtility.properties if provided
        if (featureUtilityProps != null) {
            loadFromFeatureUtilityProperties(featureUtilityProps);
        }
        
        // 3. Load from bootstrap.properties if server name is provided
        if (serverName != null && !serverName.isEmpty()) {
            loadFromBootstrapProperties(serverName);
            loadFromServerEnv(serverName);
        }
        
        logger.log(Level.FINE, "FeatureUtilityKeyResolver initialized with {0} variables",
                   resolvedVariables.size());
        if (logger.isLoggable(Level.FINE)) {
            for (String key : resolvedVariables.keySet()) {
                logger.log(Level.FINE, "  Loaded variable: {0}", key);
            }
        }
    }

    /**
     * Loads encryption key variables from environment variables.
     */
    private void loadFromEnvironment() {
        // Check for common encryption key environment variables
        String value = System.getenv("WLP_PASSWORD_ENCRYPTION_KEY");
        if (value != null && !value.isEmpty()) {
            resolvedVariables.put("WLP_PASSWORD_ENCRYPTION_KEY", value);
            logger.log(Level.FINE, "Loaded encryption key variable from environment: WLP_P***");
        }
        
        value = System.getenv("WLP_AES_ENCRYPTION_KEY");
        if (value != null && !value.isEmpty()) {
            resolvedVariables.put("wlp.aes.encryption.key", value);
            logger.log(Level.FINE, "Loaded encryption key variable from environment: WLP_A***");
        }
      
    }

    /**
     * Loads encryption key variables from featureUtility.properties.
     *
     * @param featureUtilityProps the featureUtility.properties
     */
    private void loadFromFeatureUtilityProperties(Properties featureUtilityProps) {
        // Check for encryption key properties
        String[] keyProps = {
            "wlp.password.encryption.key",
            "wlp.aes.encryption.key"
        };
        
        for (String key : keyProps) {
            String value = featureUtilityProps.getProperty(key);
            if (value != null && !value.isEmpty()) {
                resolvedVariables.put(key, value);
                logger.log(Level.FINE, "Loaded encryption key from featureUtility.properties: {0} ",
                          new Object[]{key});
            }
        }
    }

    /**
     * Loads variables from bootstrap.properties for the specified server.
     * 
     * @param serverName the server name
     */
    private void loadFromBootstrapProperties(String serverName) {
        String wlpUsrDir = System.getProperty("wlp.user.dir", 
                                              Utils.getInstallDir().getAbsolutePath() + "/usr");
        String bootstrapPath = wlpUsrDir + "/servers/" + serverName + "/bootstrap.properties";
        File bootstrapFile = new File(bootstrapPath);
        
        if (!bootstrapFile.exists()) {
            logger.log(Level.FINE, "bootstrap.properties not found at: {0}", bootstrapPath);
            return;
        }
        
        Properties props = new Properties();
        try (InputStream input = new FileInputStream(bootstrapFile)) {
            props.load(input);
            
            // Load encryption key properties
            for (Object key : props.keySet()) {
                String keyStr = (String) key;
                if (keyStr.contains("encryption.key")) {
                    String value = props.getProperty(keyStr);
                    resolvedVariables.put(keyStr, value);
                    logger.log(Level.FINE, "Loaded encryption key from bootstrap.properties: {0} ",
                              new Object[]{keyStr});
                }
            }
        } catch (FileNotFoundException e) {
            logger.log(Level.FINE, "bootstrap.properties not found: {0}", bootstrapPath);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error reading bootstrap.properties: " + bootstrapPath, e);
        }
    }

    /**
     * Loads variables from server.env for the specified server.
     * 
     * @param serverName the server name
     */
    private void loadFromServerEnv(String serverName) {
        String wlpUsrDir = System.getProperty("wlp.user.dir", 
                                              Utils.getInstallDir().getAbsolutePath() + "/usr");
        String serverEnvPath = wlpUsrDir + "/servers/" + serverName + "/server.env";
        File serverEnvFile = new File(serverEnvPath);
        
        if (!serverEnvFile.exists()) {
            logger.log(Level.FINE, "server.env not found at: {0}", serverEnvPath);
            return;
        }
        
        Properties props = new Properties();
        try (InputStream input = new FileInputStream(serverEnvFile)) {
            props.load(input);
            
            // Load encryption key properties
            for (Object key : props.keySet()) {
                String keyStr = (String) key;
                if (keyStr.contains("encryption.key")) {
                    String value = props.getProperty(keyStr);
                    resolvedVariables.put(keyStr, value);
                    logger.log(Level.FINE, "Loaded encryption key from server.env: {0} ",
                              new Object[]{keyStr});
                }
            }
        } catch (FileNotFoundException e) {
            logger.log(Level.FINE, "server.env not found: {0}", serverEnvPath);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error reading server.env: " + serverEnvPath, e);
        }
    }

    /**
     * Resolves a key string that may contain variable references.
     * 
     * @param keyString the key string, which may be:
     *                  - A variable reference like "${wlp.password.encryption.key}"
     *                  - A direct key value
     *                  - null
     * @return the resolved key as a character array, or the original string as char[]
     */
    @Override
    public char[] getKey(String keyString) {
        if (keyString == null || keyString.isEmpty()) {
            logger.log(Level.WARNING, "Null or empty key string provided to resolver");
            return new char[0];
        }
        
        // Check if the string contains variable references
        Matcher matcher = VARIABLE_PATTERN.matcher(keyString);
        if (matcher.find()) {
            String variableName = matcher.group(1);
            logger.log(Level.FINE, "Attempting to resolve variable: {0}", variableName);
            String resolvedValue = resolveVariable(variableName);
            
            if (resolvedValue != null) {
                logger.log(Level.FINE, "Successfully resolved variable {0}",
                          new Object[]{variableName});
                return resolvedValue.toCharArray();
            } else {
                logger.log(Level.WARNING, "Could not resolve variable: {0}. Returning original string.", variableName);
                // Return the original string if we can't resolve it
                return keyString.toCharArray();
            }
        }
        
        // No variable reference, return as-is
        logger.log(Level.FINE, "No variable reference found. Using keyString directly");
        return keyString.toCharArray();
    }

    /**
     * Resolves a variable name to its value.
     * 
     * @param variableName the variable name (without ${})
     * @return the resolved value, or null if not found
     */
    private String resolveVariable(String variableName) {
        // First check our loaded variables
        String value = resolvedVariables.get(variableName);
        if (value != null) {
            return value;
        }
        
        // Fall back to system properties
        value = System.getProperty(variableName);
        if (value != null) {
            return value;
        }
        
        // Fall back to environment variables (with dot-to-underscore conversion)
        String envVarName = variableName.replace('.', '_').toUpperCase();
        value = System.getenv(envVarName);
        if (value != null) {
            return value;
        }
        
        // Try the original name as environment variable
        value = System.getenv(variableName);
        return value;
    }

    /**
     * Gets the number of variables loaded by this resolver.
     * Useful for testing and debugging.
     * 
     * @return the number of loaded variables
     */
    public int getLoadedVariableCount() {
        return resolvedVariables.size();
    }
}