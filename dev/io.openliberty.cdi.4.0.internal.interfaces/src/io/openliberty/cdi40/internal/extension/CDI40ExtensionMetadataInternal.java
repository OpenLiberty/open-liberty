/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.cdi40.internal.extension;

import java.util.Collections;
import java.util.Set;

import com.ibm.ws.cdi.extension.CDIExtensionMetadataInternal;

import io.openliberty.cdi.spi.CDIExtensionMetadata;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;

/**
 * CDI40 extensions to CDIExtensionMetadataInternal
 * <p>
 * Any component providing a {@link CDIExtensionMetadata} service may also implement this interface to use internal extensions. Including new options available in cdi 4.0
 *
 * Implementation note: this interface intentionally does not extend CDIExtensionMetadata because {@code @Component} by default only exposes directly implemented interfaces as
 * services. If this interface extended CDIExtensionMetadata, providers would have to either directly implement both interfaces anyway, or explicitly declare
 * {@code CDIExtensnionMetadata} as a service in their {@code @Component} annotation.
 */
public interface CDI40ExtensionMetadataInternal extends CDIExtensionMetadataInternal {

    /**
     * All classes returned by this method will be treated as CDI build compatible extensions. Override this method if you need to observe CDI container
     * lifecycle events to do something more advanced that just providing additional bean classes.
     * All extensions must be in the same archive as your CDIExtensionMetadata.
     *
     * Warning! Build compatible extensions have worse performence than CDI extensions. Use a regular extension if you can.
     */
    public default Set<Class<? extends BuildCompatibleExtension>> getBuildCompatibleExtensions() {
        return Collections.emptySet();
    }

}
