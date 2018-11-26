/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.adaptable.module.Container;
import com.ibm.wsspi.classloading.ClassLoaderConfiguration;
import com.ibm.wsspi.classloading.ClassLoaderIdentity;

class ClassLoaderConfigurationImpl implements ClassLoaderConfiguration {
    private final static ProtectionDomain DEFAULT_PROTECTION_DOMAIN = new ProtectionDomain(new CodeSource((URL) null, (Certificate[]) null), null);
    private boolean delegateLast;
    private boolean includeAppExtensions;
    private ClassLoaderIdentity id;
    private ClassLoaderIdentity parentId;
    private List<String> sharedLibraries = new ArrayList<String>();
    private List<String> commonLibraries = Collections.emptyList();
    private List<String> providers = Collections.emptyList();
    private List<Container> nativeLibraryContainers = Collections.emptyList();
    private ProtectionDomain protectionDomain = DEFAULT_PROTECTION_DOMAIN;

    @Override
    public ClassLoaderConfiguration setDelegateToParentAfterCheckingLocalClasspath(boolean delegateLast) {
        this.delegateLast = delegateLast;
        return this;
    }

    @Override
    public ClassLoaderConfiguration setId(ClassLoaderIdentity id) {
        this.id = id;
        return this;
    }

    @Override
    public ClassLoaderConfiguration setParentId(ClassLoaderIdentity id) {
        this.parentId = id;
        return this;
    }

    @Override
    public ClassLoaderConfiguration setSharedLibraries(List<String> libs) {
        this.sharedLibraries = libs == null ? Collections.<String> emptyList() : libs;
        return this;
    }

    @Override
    public ClassLoaderConfiguration setSharedLibraries(String... libs) {
        return setSharedLibraries(libs == null ? null : Arrays.asList(libs));
    }

    @Override
    public ClassLoaderConfiguration addSharedLibraries(List<String> libs) {
        for (String lib : libs)
            if (!this.sharedLibraries.contains(lib))
                this.sharedLibraries.add(lib);
        return this;
    }

    @Override
    public ClassLoaderConfiguration addSharedLibraries(String... libs) {
        return libs == null ? this : this.addSharedLibraries(Arrays.asList(libs));
    }

    @Override
    public ClassLoaderConfiguration setCommonLibraries(List<String> libs) {
        this.commonLibraries = libs == null ? Collections.<String> emptyList() : libs;
        return this;
    }

    @Override
    public ClassLoaderConfiguration setCommonLibraries(String... libs) {
        return setCommonLibraries(libs == null ? null : Arrays.asList(libs));
    }

    @Override
    public ClassLoaderConfiguration setClassProviders(List<String> providers) {
        this.providers = providers == null ? Collections.<String> emptyList() : providers;
        return this;
    }

    @Override
    public ClassLoaderConfiguration setClassProviders(String... providers) {
        return setClassProviders(providers == null ? null : Arrays.asList(providers));
    }

    @Override
    public ClassLoaderConfiguration setNativeLibraryContainers(List<Container> containers) {
        this.nativeLibraryContainers = containers == null ? Collections.<Container> emptyList() : containers;
        return this;
    }

    @Override
    public ClassLoaderConfiguration setNativeLibraryContainers(Container... containers) {
        return setNativeLibraryContainers(containers == null ? null : Arrays.asList(containers));
    }

    @Override
    @Trivial
    public boolean getDelegateToParentAfterCheckingLocalClasspath() {
        return delegateLast;
    }

    @Override
    @Trivial
    public ClassLoaderIdentity getId() {
        return id;
    }

    @Override
    @Trivial
    public ClassLoaderIdentity getParentId() {
        return parentId;
    }

    @Override
    @Trivial
    public List<String> getSharedLibraries() {
        return Collections.unmodifiableList(sharedLibraries);
    }

    @Override
    @Trivial
    public List<String> getCommonLibraries() {
        return Collections.unmodifiableList(commonLibraries);
    }

    @Override
    public List<String> getClassProviders() {
        return Collections.unmodifiableList(providers);
    }

    @Override
    @Trivial
    public List<Container> getNativeLibraryContainers() {
        return Collections.unmodifiableList(nativeLibraryContainers);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(id)
          .append(" [child of ").append(parentId).append("]")
          .append(" privateLibraries = ").append(sharedLibraries)
          .append(" commonLibraries = ").append(commonLibraries)
          .append(" providers = ").append(providers)
          .append(" nativeLibraries = ").append(nativeLibraryContainers);
        return sb.toString();
    }

    @Override
    @Trivial // injected trace calls ProtectedDomain.toString() which requires privileged access
    public ClassLoaderConfiguration setProtectionDomain(ProtectionDomain domain) {
        this.protectionDomain = domain;
        return this;
    }

    @Override
    @Trivial // injected trace calls ProtectedDomain.toString() which requires privileged access
    public ProtectionDomain getProtectionDomain() {
        return protectionDomain;
    }

    @Override
    public ClassLoaderConfiguration setIncludeAppExtensions(boolean include) {
        includeAppExtensions = include;
        return this;
    }

    @Override
    public boolean getIncludeAppExtensions() {
        return includeAppExtensions;
    }
}
