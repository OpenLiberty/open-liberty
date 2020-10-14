/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.thread.zos.hooks.internal;

import org.eclipse.osgi.internal.hookregistry.BundleFileWrapperFactoryHook;
import org.eclipse.osgi.internal.hookregistry.HookConfigurator;
import org.eclipse.osgi.internal.hookregistry.HookRegistry;
import org.eclipse.osgi.storage.BundleInfo.Generation;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.storage.bundlefile.BundleFileWrapper;

public class ThreadIdentityBundleFileWrapperFactoryHook implements BundleFileWrapperFactoryHook, HookConfigurator {
    @Override
    public void addHooks(HookRegistry hookRegistry) {
        hookRegistry.addBundleFileWrapperFactoryHook(this);
    }

    @Override
    public BundleFileWrapper wrapBundleFile(final BundleFile bundleFile, final Generation generation, final boolean base) {
        return new ThreadIdentityBundleFileWrapper(bundleFile);
    }
}