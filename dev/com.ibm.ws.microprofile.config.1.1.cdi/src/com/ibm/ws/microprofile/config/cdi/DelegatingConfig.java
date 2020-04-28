/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.cdi;

import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Config implementation which delegates to the config for the current TCCL
 * <p>
 * This can be used as the Config Bean implementation to work around the difference between the scope of a Config and the standard CDI scopes.
 */
public class DelegatingConfig implements Config {

    public static DelegatingConfig INSTANCE = new DelegatingConfig();

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return getDelegate().getConfigSources();
    }

    @Override
    public <T> Optional<T> getOptionalValue(String arg0, Class<T> arg1) {
        return getDelegate().getOptionalValue(arg0, arg1);
    }

    @Override
    public Iterable<String> getPropertyNames() {
        return getDelegate().getPropertyNames();
    }

    @Override
    public <T> T getValue(String arg0, Class<T> arg1) {
        return getDelegate().getValue(arg0, arg1);
    }

    protected Config getDelegate() {
        return ConfigProvider.getConfig();
    }

}
