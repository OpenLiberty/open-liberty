/*******************************************************************************
 * Copyright (c) 201, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsf.config.annotation;

import java.util.HashMap;

import javax.faces.context.ExternalContext;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.myfaces.config.annotation.LifecycleProvider;
import org.apache.myfaces.config.annotation.LifecycleProviderFactory;
import org.apache.myfaces.config.annotation.NoAnnotationLifecyleProvider;
import org.apache.myfaces.shared.util.ClassUtils;

import com.ibm.ws.jsf.config.annotation.WebSphereAnnotationLifecycleProvider;

public class WebSphereLifecycleProviderFactory extends LifecycleProviderFactory {
    private static HashMap<ClassLoader, LifecycleProvider> lifecycleProviders;
    private static final Logger log = Logger.getLogger(WebSphereLifecycleProviderFactory.class.getName());

    public WebSphereLifecycleProviderFactory() {
        if (lifecycleProviders == null) {
            lifecycleProviders = new HashMap<ClassLoader, LifecycleProvider>();
        }
    }

    public LifecycleProvider getLifecycleProvider(ExternalContext externalContext) {
        ClassLoader cl = ClassUtils.getContextClassLoader();
        LifecycleProvider provider = null;

        provider = lifecycleProviders.get(cl);
        if (provider == null) {
            if (externalContext != null) {
                provider = new WebSphereAnnotationLifecycleProvider(externalContext);
                lifecycleProviders.put(cl, provider);
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("ExternalContext not found, resolve fallback LifecycleProvider");
                }
                provider = new NoAnnotationLifecyleProvider();
            }

        }

        return provider;
    }

    @Override
    public void release() {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("about to clear lifecycle provider map");
        }
        lifecycleProviders.clear();
    }
}
