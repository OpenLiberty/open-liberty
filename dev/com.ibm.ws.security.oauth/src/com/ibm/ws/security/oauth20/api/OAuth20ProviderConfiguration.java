/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.api;

import java.util.List;
import java.util.Properties;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.ws.security.oauth20.exception.CannotModifyOAuthParameterException;
import com.ibm.ws.security.oauth20.filter.OAuthResourceProtectionFilter;
import com.ibm.ws.security.oauth20.util.OAuth20Parameter;

/**
 * This class was imported from tWAS to make only those changes necessary to
 * run OAuth on Liberty. The mission was not to refactor, restructure, or
 * generally cleanup the code.
 */
public interface OAuth20ProviderConfiguration extends
        OAuthComponentConfiguration {

    /**
     * Returns the key:values pairs associated with this configuration. Both WAS
     * and OAuth core component parameters are included. Useful for getting and
     * changing the component configuration.
     */
    public List<OAuth20Parameter> getParameters();

    /**
     * Gets properties as a flat list, with comma-separated values
     *
     * @return provider properties
     */
    public Properties getCustomizableProperties();

    /**
     * Returns a new parameter list for this OAuth provider with updated
     * properties Also validates the properties are customizable.
     *
     * @return updated parameters
     */
    public List<OAuth20Parameter> mergeCustomizedProperties(Properties props)
            throws CannotModifyOAuthParameterException;

    /**
      * Returns the provider's filter
      *
      * @return OAuthResourceProtectionFilter
      */
    public OAuthResourceProtectionFilter getFilter();
}
