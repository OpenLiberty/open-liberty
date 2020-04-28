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
package com.ibm.ws.microprofile.config14.cdi;

import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.ws.microprofile.config.cdi.DelegatingConfig;

/**
 * Config implementation which delegates to the config for the current TCCL
 * <p>
 * This can be used as the Config Bean implementation to work around the difference between the scope of a Config and the standard CDI scopes.
 */
public class Config14DelegatingConfig extends DelegatingConfig implements Config {

    public static Config14DelegatingConfig INSTANCE = new Config14DelegatingConfig();

    @Override
    public <T> Optional<Converter<T>> getConverter(Class<T> forType) {
        return getDelegate().getConverter(forType);
    }

}
