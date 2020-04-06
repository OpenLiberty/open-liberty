/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.api.config;

/**
 * OAuth component configuration interface. A consumer of the OAuth component is
 * responsible for providing an implementation class and passing it to the
 * ComponentInstance constructor. The configuration properties that must be
 * supplied are described in the {@link OAuthComponentConfigurationConstants}
 * interface.
 * 
 * @see OAuthComponentConfigurationConstants
 * @see SampleComponentConfiguration
 */
public interface OAuthComponentConfiguration {

    /**
     * Returns a unique identifier representing this configuration instance.
     * This is used to logically separate instances of the
     * OAuthComponentInstance.The implementation need ensure this UniqueId
     * uniqueness if there are multiple OAuth service provider components.
     * 
     * @return a unique identifier representing an instance of the OAuth
     *         component within an java process. Typically there will only be
     *         one instance per JVM however logically there could be more if
     *         there multiple OAuth components in one JVM .
     */
    public String getUniqueId();

    /**
     * Return a class loader suitable for loading other customer plugin classes
     * defined in the configuration.
     * 
     * @return a class loader suitable for loading other customer plugin
     *         classes.
     */
    public ClassLoader getPluginClassLoader();

    /**
     * Returns the first string value for the property if any.
     * 
     * @return the first string value for the property if any.
     */
    public String getConfigPropertyValue(String name);

    /**
     * The Array of string values for the property.
     * 
     * @return the array of string values for the property.
     */
    public String[] getConfigPropertyValues(String name);

    /**
     * Returns the first integer value for the property if any.
     * 
     * @return the first integer value for the property if any.
     */
    public int getConfigPropertyIntValue(String name);

    /**
     * The boolean value for the property.
     * 
     * @return the boolean value for the property.
     */
    public boolean getConfigPropertyBooleanValue(String name);
}
