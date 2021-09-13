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
package io.openliberty.concurrent.cdi;

import java.util.Collections;
import java.util.Set;

import javax.enterprise.inject.spi.Extension;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import io.openliberty.cdi.spi.CDIExtensionMetadata;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
           service = CDIExtensionMetadata.class)
public class ConcurrencyExtensionMetadata implements CDIExtensionMetadata {
    @Override
    public Set<Class<? extends Extension>> getExtensions() {
        return Collections.singleton(ConcurrencyExtension.class);
    }
}