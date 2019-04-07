/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.config;

import com.ibm.ws.kernel.instrument.serialfilter.agenthelper.ObjectInputStreamClassInjector;
import com.ibm.ws.kernel.instrument.serialfilter.config.ConfigSetting.*;

import java.util.Map;
import java.util.Properties;

import static com.ibm.ws.kernel.instrument.serialfilter.config.ConfigSetting.PropertiesSetter.LOAD;

class ConfigProxy implements SimpleConfig {
    private static final ConfigProxy INSTANCE = new ConfigProxy();
    private final Configurator<SimpleConfig> cfg;

    private ConfigProxy() {
        // retrieve the map from ObjectInputStream.class
        final Map<?, ?> configMap = ObjectInputStreamClassInjector.getConfigMap();
        // create the cfg using that map as the config target
        cfg = new Configurator<SimpleConfig>(configMap);
    }

    public static ConfigProxy getConfigProxy() {
        return INSTANCE;
    }

    @Override
    public void reset() {
        cfg.send(Resetter.RESET, null);
    }

    @Override
    public ValidationMode getDefaultMode() {return cfg.send(DefaultValidationModeGetter.GET_DEFAULT_VALIDATION_MODE, null);}

    @Override
    public ValidationMode getValidationMode(String s) {
        return cfg.send(ValidationModeGetter.GET_VALIDATION_MODE, s);
    }

    @Override
    public boolean setValidationMode(ValidationMode mode, String specifier) {return cfg.send(mode, specifier);}

    @Override
    public boolean setPermission(PermissionMode mode, String specifier) {return cfg.send(mode, specifier);}

    @Override
    public void load(Properties props) {cfg.send(LOAD, props);}
}
