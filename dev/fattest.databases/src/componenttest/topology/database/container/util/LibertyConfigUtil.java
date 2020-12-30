/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package componenttest.topology.database.container.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.testcontainers.containers.JdbcDatabaseContainer;

import com.ibm.websphere.simplicity.config.Variable;

import componenttest.topology.database.container.DatabaseContainerType;

/**
 * Generic liberty config utilities used by the fattest.database project
 */
public class LibertyConfigUtil {
    
    //List of config variables to be added to server/client config
    private static final Map<String, String> configVariables = new HashMap<>();
    
    /**
     * Returns true if modification to server/client config is necessary based off of the type of JdbcDatabaseContainer we have setup.
     * 
     * @param cont
     * @return boolean - true modification necessary, false otherwise.
     */
    public static boolean needsModification(JdbcDatabaseContainer<?> cont) {
    	return DatabaseContainerType.valueOf(cont) != DatabaseContainerType.Derby && DatabaseContainerType.valueOf(cont) != DatabaseContainerType.DerbyClient;
    }
    
    //No objects should be created from this class
    private LibertyConfigUtil() {}
    
    /**
     * Add a key/value pair to later be added to your server/client config
     * 
     * @param key
     * @param value
     */
    public static void addConfigVariable(String key, String value) throws IllegalArgumentException{
        if (!Pattern.matches("[a-zA-Z_]+[a-zA-Z0-9_]*", key)) {
            throw new IllegalArgumentException("Invalid environment variable key '" + key +
                                               "'. Variable keys must consist of characers [a-zA-Z0-9_] " +
                                               "in order to be compatible on all OSes.");
        }
        
    	configVariables.put(key, value);
    }
    
    /**
     * Returns all config variables added by user as a list of Variables
     * that should be added to a server/client config
     * 
     * @see com.ibm.websphere.simplicity.config.Variable
     * 
     * @return List of variables
     */
    public static List<Variable> getConfigVariables(){
    	ArrayList<Variable> variables = new ArrayList<>();
    	for(Entry<String, String> entry : configVariables.entrySet()) {
    		variables.add(new Variable(entry.getKey(), entry.getValue()));
    	}
    	return variables;
    }
    
    /**
     * Removes all config variables.
     */
    public static void clearVariables() {
    	configVariables.clear();
    }
}
