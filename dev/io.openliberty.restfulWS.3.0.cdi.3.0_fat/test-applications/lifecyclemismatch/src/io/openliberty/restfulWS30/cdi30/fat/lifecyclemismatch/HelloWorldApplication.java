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
package io.openliberty.restfulWS30.cdi30.fat.lifecyclemismatch;

import java.util.HashSet;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Application;

/**
 * {@code HelloWorldApplication} is a {@link jakarta.ws.rs.core.Application} subclass which contains configuration information for the JAX-RS application.
 * Any JAX-RS resources or providers that are to be used must be returned in
 * either the {@link #getClasses()} or {@link #getSingletons()} methods. Note
 * that providers are always singletons according to the JavaDoc.
 */

public class HelloWorldApplication extends Application {
    
    private ApplicationScopedProvider applicationScopedProvider;
    private ApplicationScopedSingleton applicationScopedSingleton;
    private DefaultProvider defaultProvider;
    private DefaultSingleton defaultSingleton;
    private DependentProvider dependentProvider;
    private DependentSingleton dependentSingleton;
    private RequestScopedProvider requestScopedProvider;
    private RequestScopedSingleton requestScopedSingleton;
    private SessionScopedProvider sessionScopedProvider;
    private SessionScopedSingleton sessionScopedSingleton;


    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(ApplicationScopedResource.class);
        classes.add(RequestScopedResource.class);
        classes.add(DefaultResource.class);
        classes.add(SessionScopedResource.class);
        classes.add(DependentResource.class);
        classes.add(ProviderResource.class);

        return classes;
    }

    @Override
    public Set<Object> getSingletons() {

        Set<Object> singletons = new HashSet<Object>();
        singletons.add(applicationScopedProvider);
        singletons.add(applicationScopedSingleton);
        singletons.add(defaultProvider);
        singletons.add(defaultSingleton);
        singletons.add(dependentProvider);
        singletons.add(dependentSingleton);
        singletons.add(requestScopedProvider);
        singletons.add(requestScopedSingleton);
        singletons.add(sessionScopedProvider);
        singletons.add(sessionScopedSingleton);
        return singletons;
    }

    @Inject
    public void setApplicationScopedProvider(ApplicationScopedProvider applicationScopedProvider) {
        this.applicationScopedProvider = applicationScopedProvider;
    }

    @Inject
    public void setApplicationScopedSingleton(ApplicationScopedSingleton applicationScopedSingleton) {
        this.applicationScopedSingleton = applicationScopedSingleton;
    }

    @Inject
    public void setDefaultProvider(DefaultProvider defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    @Inject
    public void setDefaultSingleton(DefaultSingleton defaultSingleton) {
        this.defaultSingleton = defaultSingleton;
    }

    @Inject
    public void setDependentProvider(DependentProvider dependentProvider) {
        this.dependentProvider = dependentProvider;
    }

    @Inject
    public void setDependentSingleton(DependentSingleton dependentSingleton) {
        this.dependentSingleton = dependentSingleton;
    }

    @Inject
    public void setRequestScopedProvider(RequestScopedProvider requestScopedProvider) {
        this.requestScopedProvider = requestScopedProvider;
    }

    @Inject
    public void setRequestScopedSingleton(RequestScopedSingleton requestScopedSingleton) {
        this.requestScopedSingleton = requestScopedSingleton;
    }

    @Inject
    public void setSessionScopedProvider(SessionScopedProvider sessionScopedProvider) {
        this.sessionScopedProvider = sessionScopedProvider;
    }

    @Inject
    public void setSessionScopedSingleton(SessionScopedSingleton sessionScopedSingleton) {
        this.sessionScopedSingleton = sessionScopedSingleton;
    }
}
