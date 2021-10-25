/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.rest.client30.internal;

import java.util.Collections;
import java.util.Set;

import javax.enterprise.inject.spi.Extension;

import org.jboss.resteasy.microprofile.client.RestClientExtension;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.cdi.extension.CDIExtensionMetadataInternal;

import io.openliberty.cdi.spi.CDIExtensionMetadata;

/**
 * Registers the RestEasy MP Rest Client extension
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class RestClientCdiExtensionMetadata implements CDIExtensionMetadata, CDIExtensionMetadataInternal {

    @Override
    public Set<Class<? extends Extension>> getExtensions() {
        return Collections.singleton(RestClientExtension.class);
    }

    @Override
    public boolean applicationBeansVisible() {
        return true;
    }

}
