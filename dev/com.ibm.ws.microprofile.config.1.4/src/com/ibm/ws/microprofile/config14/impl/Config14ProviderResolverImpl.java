/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.impl;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import com.ibm.ws.microprofile.config.impl.AbstractConfigBuilder;
import com.ibm.ws.microprofile.config13.impl.Config13ProviderResolverImpl;

public class Config14ProviderResolverImpl extends Config13ProviderResolverImpl {

    /** {@inheritDoc} */
    @Override
    protected AbstractConfigBuilder newBuilder(ClassLoader classLoader) {
        return new Config14BuilderImpl(classLoader, getScheduledExecutorService(), getInternalConfigSources());
    }

    @Override
    protected void setStaticInstance(ConfigProviderResolver instance) {
        //NO-OP ... this version relies purely on the ServiceLoader mechanism
    }
}
