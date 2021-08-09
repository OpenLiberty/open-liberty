/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.bus;

import java.util.Map;

import org.apache.cxf.bus.extension.ExtensionManagerBus;

/**
 *
 */
public class LibertyApplicationBus extends ExtensionManagerBus {

    public enum Type {
        SERVER, CLIENT
    }

    /**
     * @param e
     * @param properties
     * @param extensionClassLoader
     */
    public LibertyApplicationBus(Map<Class<?>, Object> e, Map<String, Object> properties, ClassLoader extensionClassLoader) {
        super(e, properties, extensionClassLoader);
    }

    /**
     * Comparing with getExtension method, getLocalExtension will not trigger to create/search ConfiguredBeanLocator
     * 
     * @param extensionType
     * @return
     */
    public <T> T getLocalExtension(Class<T> extensionType) {
        Object obj = extensions.get(extensionType);
        if (null != obj) {
            return extensionType.cast(obj);
        }
        return null;
    }

    /**
     * Comparing with hasExtensionByName method, hasLocalExtension will not trigger to create/search ConfiguredBeanLocator
     * Also, it will class instance to search the extension map.
     * 
     * @param extensionType
     * @return
     */
    public boolean hasLocalExtension(Class<?> extensionType) {
        return extensions.containsKey(extensionType);
    }
}
