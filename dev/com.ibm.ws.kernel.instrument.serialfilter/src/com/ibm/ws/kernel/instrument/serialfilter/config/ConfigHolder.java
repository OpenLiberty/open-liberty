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

import java.io.ObjectInputStream;
import java.util.Map;

public abstract class ConfigHolder extends ConfigurableFunctor<SimpleConfig, ObjectInputStream, Map<Class<?>, Class<?>>> {
    protected ConfigHolder(Config config) {
        super(config);
    }

    /** All the valid config options are enums in this package that implement {@link ConfigSetting}. */
    @Override
    protected boolean isExpectedType(@SuppressWarnings("rawtypes") Class<? extends ConfigurationSetting> c) {
        return c.isEnum() && ConfigSetting.class.isAssignableFrom(c) && c.getPackage() == ConfigSetting.class.getPackage();
    }
}
