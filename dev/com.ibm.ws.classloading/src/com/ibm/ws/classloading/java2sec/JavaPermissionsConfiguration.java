/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.java2sec;

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * This class holds the Java 2 Security permissions configured in the server.xml
 */
@Trivial
public class JavaPermissionsConfiguration {

	/**
	 * The trace component used for logging to the trace.log file.
	 */
	private static final TraceComponent tc = Tr.register(JavaPermissionsConfiguration.class);
	
	/**
	 * Constant for the configuration Id
	 */
	private static final String KEY_ID = "config.id";
	
	/**
	 * Constant for the codebase configuration key.
	 */
	public static final String CODE_BASE = "codebase";
	
	/**
	 * Constant for the signedBy configuration key.
	 */
	public static final String SIGNED_BY = "signedBy";

	/**
	 * Constant for the principalType configuration key.
	 */
	public static final String PRINCIPAL_TYPE = "principalType";

	/**
	 * Constant for the principalName configuration key.
	 */
	public static final String PRINCIPAL_NAME = "principalName";

	/**
	 * Constant for the permission class configuration key.
	 */
	public static final String PERMISSION = "className";

	/**
	 * Constant for the targetName configuration key.
	 */
	public static final String TARGET_NAME = "name";

	/**
	 * Constant for the actions configuration key.
	 */
	public static final String ACTIONS = "actions";

	/**
	 * Constant for the restriction configuration key.
	 */
	public static final String RESTRICTION = "restriction";

	/**
	 * Map to hold the configuration properties for this permission object
	 */
	public volatile Map<String, Object> config;
	
	/**
	 * Activate the Java Permission configuration
	 * @param properties
	 * @param cc
	 */
    @Activate
	protected void activate(Map<String, Object> properties, ComponentContext cc) {
    	config = properties;
    }
    
    /**
     * Deactivate the Java Permission configuration
     */
    @Deactivate
    protected void deactivate(ComponentContext cc) {
    	config = null;
    }
    
    /**
     * Get the data/value associated with the specified key for this permission configuration.
     * 
     * @param key
     */
    public Object get(String key) {
    	if(config != null) {
    		return config.get(key);
    	}
    	else
    		return null;
    }
}
