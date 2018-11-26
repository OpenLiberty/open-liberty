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

import com.ibm.ws.microprofile.config.impl.AbstractConfigBuilder;
import com.ibm.ws.microprofile.config.impl.AbstractProviderResolver;

public class ConfigProviderResolverImpl extends AbstractProviderResolver {

    /** {@inheritDoc} */
    @Override
    protected AbstractConfigBuilder newBuilder(ClassLoader classLoader) {
        return new ConfigBuilderImpl(classLoader, getScheduledExecutorService());
    }

}