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
package io.openliberty.microprofile.openapi20;

import java.io.IOException;

import org.eclipse.microprofile.openapi.spi.OASFactoryResolver;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.smallrye.openapi.spi.OASFactoryResolverImpl;

@Component(service = OASFactoryResolver.class, configurationPolicy = ConfigurationPolicy.IGNORE, property = { "service.vendor=IBM" }, immediate = true)
public class OLOASFactoryResolverImpl extends OASFactoryResolverImpl {
    /**
     * Activate a context and set the instance
     *
     * @param cc
     */
    public void activate(ComponentContext cc) {
    	OASFactoryResolver.setInstance(this);
    }

    /**
     * Deactivate a context and set the instance to null
     *
     * @param cc
     */
    public void deactivate(ComponentContext cc) throws IOException {
    	OASFactoryResolver.setInstance(null);
    }
}
