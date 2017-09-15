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
package com.ibm.wsspi.kernel.equinox.module;

import org.osgi.framework.Bundle;

/**
 * A service to provide class loaders which will be used
 * as a delegate class loader for a bundle class loader.
 * One one instance of this service is used at runtime.
 */
public interface ModuleDelegateClassLoaderFactory {
    /**
     * Return a delegate class loader for the specified bundle or {@code null} if no delegate should be used for the bundle.
     * 
     * @param bundle the bundle
     * @return the delegate class loader
     */
    ClassLoader getDelegateClassLoader(Bundle bundle);
}
