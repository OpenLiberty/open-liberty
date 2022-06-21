/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.osgi.framework.Version;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.classloading.ApiType;
import com.ibm.wsspi.classloading.GatewayConfiguration;

@Trivial
class GatewayConfigurationImpl implements GatewayConfiguration {
    private Iterable<String> bundleRequirements;
    private Iterable<String> packageImports;
    private Iterable<String> dynamicPackageImports;
    private boolean delegateToSystem = true;
    private volatile EnumSet<ApiType> apiTypeVisibility;
    private String appName;
    private Version appVersion;

    public GatewayConfigurationImpl() {}

    @Override
    public GatewayConfiguration setRequireBundle(List<String> bundleRequirements) {
        this.bundleRequirements = bundleRequirements;
        return this;
    }

    @Override
    public GatewayConfiguration setRequireBundle(String... bundleRequirements) {
        return setRequireBundle(Arrays.asList(bundleRequirements));
    }

    @Override
    public GatewayConfiguration setImportPackage(List<String> packageImports) {
        this.packageImports = packageImports;
        return this;
    }

    @Override
    public GatewayConfiguration setImportPackage(String... packageImports) {
        return setImportPackage(Arrays.asList(packageImports));
    }

    @Override
    public GatewayConfiguration setDynamicImportPackage(List<String> packageImports) {
        this.dynamicPackageImports = packageImports;
        return this;
    }

    @Override
    public GatewayConfiguration setDynamicImportPackage(String... packageImports) {
        return setDynamicImportPackage(Arrays.asList(packageImports));
    }

    @Override
    public GatewayConfiguration setApplicationName(String name) {
        this.appName = name;
        return this;
    }

    @Override
    public GatewayConfiguration setApplicationVersion(Version version) {
        this.appVersion = version;
        return this;
    }

    @Override
    public Iterable<String> getRequireBundle() {
        return bundleRequirements;
    }

    @Override
    public Iterable<String> getImportPackage() {
        return packageImports;
    }

    @Override
    public Iterable<String> getDynamicImportPackage() {
        return this.dynamicPackageImports;
    }

    @Override
    public String getApplicationName() {
        return appName;
    }

    @Override
    public Version getApplicationVersion() {
        return appVersion;
    }

    @Override
    public GatewayConfiguration setDelegateToSystem(boolean delegateToSystem) {
        this.delegateToSystem = delegateToSystem;
        return this;
    }

    @Override
    public boolean getDelegateToSystem() {
        return delegateToSystem;
    }

    @Override
    public EnumSet<ApiType> getApiTypeVisibility() {
        return apiTypeVisibility == null ? null : apiTypeVisibility.clone();
    }

    @Override
    public GatewayConfiguration setApiTypeVisibility(ApiType... types) {
        EnumSet<ApiType> set = EnumSet.noneOf(ApiType.class);
        for (ApiType t : types)
            if (t != null)
                set.add(t);
        this.apiTypeVisibility = set;
        return this;
    }

    @Override
    public GatewayConfiguration setApiTypeVisibility(Iterable<ApiType> types) {
        EnumSet<ApiType> set = EnumSet.noneOf(ApiType.class);
        if (types != null)
            for (ApiType t : types)
                if (t != null)
                    set.add(t);
        this.apiTypeVisibility = set;
        return this;
    }
}
