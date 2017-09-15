/*******************************************************************************
 * Copyright (c) 1997, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.security.auth.callback;

import java.util.Map;

import javax.security.auth.callback.Callback;

/**
 * <p>
 * The <code>WSMappingPropertiesCallback</code> allows a HashMap object to be gathered by
 * <code>CallbackHandler</code> and pass it to the <code>LoginModule</code> stack.
 * </p>
 * 
 * @author IBM Corporation
 * @version 1.0
 * @see javax.security.auth.callback.CallbackHandler
 * @see com.ibm.wsspi.security.auth.callback.WSiMappingCallbackHandler
 * @since WAS V6.0
 * 
 * @ibm-spi
 */
public class WSMappingPropertiesCallback implements Callback {

    private final String prompt;
    private Map properties;

    /**
     * <p>
     * Construct a <code>WSMappingPropertiesCallback</code> object with a prompt hint.
     * </p>
     * 
     * @param prompt The prompt hint.
     */
    public WSMappingPropertiesCallback(String prompt) {
        this.prompt = prompt;
    }

    /**
     * <p>
     * Construct a <code>WSMappingPropertiesCallback</code> object with a prompt hint and
     * a Map object instance.
     * </p>
     * 
     * @param prompt The prompt hint.
     * @param Map properties
     */
    public WSMappingPropertiesCallback(String prompt, Map properties) {
        this.prompt = prompt;
        this.properties = properties;
    }

    /**
     * <p>
     * Set the properties.
     * </p>
     * 
     * @param properties The properties Map.
     */
    public void setProperties(Map properties) {
        this.properties = properties;
    }

    /**
     * <p>
     * Return the properties Map. If the properties object set in
     * Constructor is <code>null</code>, then <code>null</code> is returned.
     * </p>
     * 
     * @return The properties Map, could be <code>null</code>.
     */
    public Map getProperties() {
        return properties;
    }

    /**
     * <p>
     * Return the prompt. If the prompt set in Constructor
     * is <code>null</code>, then <code>null</code> is returned.
     * </p>
     * 
     * @return The prompt, could be <code>null</code>.
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * <p>
     * Returns the name of the Callback. Typically, it is the name of the class.
     * </p>
     * 
     * @return The name of the Callback.
     */
    @Override
    public String toString() {
        return getClass().getName();
    }

}
