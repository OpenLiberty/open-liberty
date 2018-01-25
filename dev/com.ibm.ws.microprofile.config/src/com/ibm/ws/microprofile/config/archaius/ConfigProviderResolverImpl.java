/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.archaius;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.microprofile.config.impl.AbstractConfigBuilder;
import com.ibm.ws.microprofile.config.impl.AbstractProviderResolver;

@Component(name = "com.ibm.ws.microprofile.config.archaius.ConfigProviderResolverImpl", service = { ConfigProviderResolver.class }, property = { "service.vendor=IBM" }, immediate = true)
public class ConfigProviderResolverImpl extends AbstractProviderResolver {

    /** {@inheritDoc} */
    @Override
    protected AbstractConfigBuilder newBuilder(ClassLoader classLoader) {
        return new ConfigBuilderImpl(classLoader, getScheduledExecutorService());
    }

}