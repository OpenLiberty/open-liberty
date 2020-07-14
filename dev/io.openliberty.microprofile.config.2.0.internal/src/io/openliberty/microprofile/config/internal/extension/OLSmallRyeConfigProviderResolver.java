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
package io.openliberty.microprofile.config.internal.extension;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.smallrye.config.SmallRyeConfigBuilder;
import io.smallrye.config.SmallRyeConfigProviderResolver;

@Component(service = ConfigProviderResolver.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" }, immediate = true)
public class OLSmallRyeConfigProviderResolver extends SmallRyeConfigProviderResolver {
    /**
     * Activate a context and set the instance
     *
     * @param cc
     */
    public void activate(ComponentContext cc) {
        ConfigProviderResolver.setInstance(this);
    }

    /**
     * Deactivate a context and set the instance to null
     *
     * @param cc
     */
    public void deactivate(ComponentContext cc) throws IOException {
        ConfigProviderResolver.setInstance(null);
    }

    /**
     * The following method is a temporary solution to a Security manager issue with the SmallRye Config code.
     *
     * When the new version of SmallRye Config is uploaded to Maven Central, resolving the issue, this method will be removed.
     */
    @Override
    public SmallRyeConfigBuilder getBuilder() {
        SmallRyeConfigBuilder builder = null;
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            builder = AccessController.doPrivileged((PrivilegedAction<SmallRyeConfigBuilder>) OLSmallRyeConfigBuilder::new);
        } else {
            builder = new OLSmallRyeConfigBuilder();
        }

        return builder;
    }

}
