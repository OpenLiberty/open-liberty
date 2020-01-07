/*******************************************************************************
 * Copyright (c) 1997, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import com.ibm.oauth.core.api.config.SampleComponentConfiguration;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.api.OAuth20ProviderConfiguration;
import com.ibm.ws.security.oauth20.exception.CannotModifyOAuthParameterException;
import com.ibm.ws.security.oauth20.filter.OAuthResourceProtectionFilter;
import com.ibm.ws.security.oauth20.util.OAuth20Parameter;

/*
 * Configuration class used to store parameters for the core OAuth component, implements OAuthComponentConfiguration
 */

/**
 * This class was imported from tWAS to make only those changes necessary to
 * run OAuth on Liberty. The mission was not to refactor, restructure, or
 * generally cleanup the code.
 */
public class OAuth20ComponentConfigurationImpl extends SampleComponentConfiguration implements OAuth20ProviderConfiguration {

    private static final TraceComponent tc = Tr.register(OAuth20ComponentConfigurationImpl.class, "OAUTH", "com.ibm.ws.security.oauth20.internal.resources.OAuthMessages");

    String uniqueId;
    private OAuthResourceProtectionFilter filter = null;
    private static final String FILTER = "filter";
    private ClassLoader pluginClassLoader = null;
    List<OAuth20Parameter> params;

    public OAuth20ComponentConfigurationImpl(String id,
            List<OAuth20Parameter> parameters, ClassLoader classLoader) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "ctor", new Object[] { id, parameters, classLoader });
        }
        uniqueId = id;
        params = parameters;
        pluginClassLoader = classLoader;
        _config.clear(); // Clear Tivoli's values. Default values handled in the provider metatype.
        for (OAuth20Parameter param : params) {
            this.putConfigPropertyValues(param.getName(), param.getValues()
                    .toArray(new String[0]));
        }
    }

    @Override
    public List<OAuth20Parameter> getParameters() {
        return params;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public Properties getCustomizableProperties() {
        Properties props = new Properties();
        for (OAuth20Parameter param : params) {
            if ("true".equalsIgnoreCase(param.getCustomizable())) {
                String key = param.getName();
                String value = param.getValues().get(0);
                if (param.getValues().size() > 1) {
                    for (int i = 1; i < param.getValues().size(); i++) {
                        // comma-separated values
                        value += "," + param.getValues().get(i);
                    }
                }
                props.put(key, value);
            }
        }
        return props;
    }

    @Override
    public List<OAuth20Parameter> mergeCustomizedProperties(Properties props)
            throws CannotModifyOAuthParameterException {
        HashSet<String> addedprops = new HashSet<String>();

        List<OAuth20Parameter> newparams = new ArrayList<OAuth20Parameter>();
        for (OAuth20Parameter param : params) {
            String name = param.getName();
            String newValue = props.getProperty(name);
            OAuth20Parameter newparam = new OAuth20Parameter(param);
            if (newValue != null) {
                // Validate it can be customized
                if ("false".equalsIgnoreCase(param.getCustomizable())) {
                    throw new CannotModifyOAuthParameterException(name);
                }
                List<String> values = Arrays.asList(newValue.split(","));
                newparam.setValues(values);
            }
            newparams.add(newparam);
            addedprops.add(name);
        }
        // also add params that were not in the list, for supportability
        for (Entry<Object, Object> entry : props.entrySet()) {
            String name = (String) entry.getKey();
            String newValue = (String) entry.getValue();
            if (!addedprops.contains(name) && newValue != null) {
                List<String> values = Arrays.asList(newValue.split(","));
                OAuth20Parameter newparam = new OAuth20Parameter(name,
                        Constants.XML_PARAM_TYPE_COMPONENT, true + "");
                newparam.setValues(values);
                newparams.add(newparam);
            }
        }
        return newparams;
    }

    /** {@inheritDoc} */
    @Override
    public OAuthResourceProtectionFilter getFilter() {
        if (filter == null) {
            String filterString = getConfigPropertyValue(FILTER);
            if (filterString != null) {
                filter = new OAuthResourceProtectionFilter(filterString, false);
            } else {
                filter = new OAuthResourceProtectionFilter(false);
            }
        }
        return filter;
    }

    /** {@inheritDoc} */
    @Override
    public ClassLoader getPluginClassLoader() {
        if (pluginClassLoader != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "using user defined shared lib classloader: ", pluginClassLoader);
            }
            return pluginClassLoader;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "using default classloader");
        }
        return super.getPluginClassLoader();
    }
}
