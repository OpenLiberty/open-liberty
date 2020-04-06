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
package com.ibm.oauth.core.internal.config;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OAuthConfigurationException;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.oauth20.util.ConfigUtils;

/**
 * This class was imported from tWAS to make only those changes necessary to 
 * run OAuth on Liberty. The mission was not to refactor, restructure, or 
 * generally cleanup the code. 
 */
public class OAuthConfigurationImpl {

    protected OAuthComponentConfiguration _oldconfig;

    private static final TraceComponent tc =
            Tr.register(OAuthConfigurationImpl.class, "OAUTH", "com.ibm.ws.security.oauth20.internal.resources.OAuthMessages");

    public OAuthConfigurationImpl(OAuthComponentConfiguration configIn) {
        _oldconfig = configIn;
    }

    protected Object processClass(String className, String configConstant,
            Class<?> interfaceName) throws OAuthException {
        if (className == null) {
            throw new OAuthConfigurationException("security.oauth.error.config.notspecified.exception", configConstant, "null", null);
        }
        try {
            ClassLoader cl = null;
            if (!ConfigUtils.isBuiltinClass(className)) {
                cl = _oldconfig.getPluginClassLoader();
            }
            else {
                cl = this.getClass().getClassLoader();
            }
            Class<?> klass = cl.loadClass(className);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "processClass", new Object[] { className, configConstant, interfaceName, cl });
            Object ret = klass.newInstance();
            if (!(interfaceName.isAssignableFrom(ret.getClass()))) {
                throw new OAuthConfigurationException("security.oauth.error.classmismatch.exception", configConstant, interfaceName.getName(), null);
            }
            return ret;
        } catch (ClassNotFoundException e) {
            throw new OAuthConfigurationException("security.oauth.error.classinstantiation.exception", configConstant, className, e);
        } catch (IllegalAccessException e) {
            throw new OAuthConfigurationException("security.oauth.error.classinstantiation.exception", configConstant, className, e);
        } catch (InstantiationException e) {
            throw new OAuthConfigurationException("security.oauth.error.classinstantiation.exception", configConstant, className, e);
        }
    }

    protected int validateNonNegativeInt(String configConstant)
            throws OAuthException {
        int result = _oldconfig.getConfigPropertyIntValue(configConstant);
        if (result < 0) {
            throw new OAuthConfigurationException("security.oauth.error.invalidconfig.exception", configConstant, result + "", null);
        }
        return result;
    }

    protected boolean validateBoolean(String configConstant)
            throws OAuthException {
        boolean result = _oldconfig
                .getConfigPropertyBooleanValue(configConstant);
        return result;
    }
}
