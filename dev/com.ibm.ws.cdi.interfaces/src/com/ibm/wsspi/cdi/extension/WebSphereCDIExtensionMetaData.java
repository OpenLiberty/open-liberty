/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.cdi.extension;

import java.util.Collections;
import java.util.Set;

import javax.enterprise.inject.spi.Extension;

/**
 * This is a *marker* interface for Weld Runtime extensions. All runtime extensions need to register a service
 * under this interface. This bundle will find all of the services and then get hold of the bundle classloader and
 * pass onto Weld.
 *
 * To use this class you must implement one of the two methods. If you implement CDIExtensionClasses then it must
 * not return the class annotated WebSphereCDIExtensionMetaData.
 *
 * The class that implements this interface should be annotated with @Component(service = WebSphereCDIExtensionMetaData.class)
 *
 * For example:
 *
 * @Component(service = WebSphereCDIExtensionMetaData.class)
 *                    public class JwtCDIExtension implements WebSphereCDIExtensionMetaData { ... }
 */

public interface WebSphereCDIExtensionMetaData {

    /**
     * All classes returned by this method will be treated as CDI beans. All classes must be in the same archive as your WebSphereCDIExtensionMetaData.
     */
    default public Set<Class<?>> getCDIBeans() {
        return Collections.emptySet();
    }

    /**
     * All classes returned by this method will be treated as CDI extensions.
     *
     * If a class in returned by this method implements WebSphereCDIExtensionMetaData an IllegalArguementException is thrown.
     * This is because it will result in OSGI and Weld independently instantiating the class, while it is unlikely for the
     * two instances to conflict, it is best to keep the OSGI service and CDI extension separate.
     */
    public default Set<Class<? extends Extension>> getCDIExtensions() {
        return Collections.emptySet();
    }

}
