/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.extension;

import io.openliberty.cdi.spi.CDIExtensionMetadata;

/**
 * Internal extensions to CDIExtensionMetadata
 * <p>
 * Any component providing a {@link CDIExtensionMetadata} service may also implement this interface to use internal extensions.
 * <p>
 * Example:
 *
 * <pre>
 * &#64;Component(configurationPolicy = IGNORE)
 * public class ExampleInternalExtension implements CDIExtensionMetadata, CDIExtensionMetadataInternal {
 *
 *     &#64;Override
 *     public Set&lt;Class&lt;?&gt;&gt; getBeanClasses() {
 *         Set&lt;Class&lt;?&gt;&gt; beans = new HashSet&lt;Class&lt;?&gt;&gt;();
 *         //This will register a producer class and expose it's produced beans to applications
 *         beans.add(ClassSPIRegisteredProducer.class);
 *     }
 *
 *     {@code @Override}
 *     public boolean applicationBeansVisible() {
 *         return true;
 *     }
 * }
 * </pre>
 *
 * <p>
 * Implementation note: this interface intentionally does not extend CDIExtensionMetadata because {@code @Component} by default only exposes directly implemented interfaces as
 * services. If this interface extended CDIExtensionMetadata, providers would have to either directly implement both interfaces anyway, or explicitly declare
 * {@code CDIExtensnionMetadata} as a service in their {@code @Component} annotation.
 */
public interface CDIExtensionMetadataInternal {

    /**
     * @return whether beans created from this extension can see and inject beans provided by the application and other extensions.
     */
    default boolean applicationBeansVisible() {
        return false;
    }

}
