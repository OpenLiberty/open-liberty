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
package com.ibm.wsspi.kernel.equinox.module;

import org.eclipse.osgi.storage.bundlefile.BundleFileWrapper;

/**
 * A factory for creating bundle files based on {@link ModuleInfo module info}.
 * This service will get called each time a bundle file is requested by the
 * framework. This happens when bundles are installed or when attempting to
 * load them from a cached state. It also happens when inner jars of a bundle
 * are used as part of the bundle class path.
 */
public interface ModuleBundleFileFactory {
    /**
     * Returns a bundle file for the given {@link ModuleInfo} or {@code null} if none is available.
     * 
     * @param moduleInfo the module info.
     * @return a bundle file for the module info or {@code null}.
     */
    public BundleFileWrapper createBundleFile(ModuleInfo moduleInfo);
}
