/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jarkartaee10.internal.tests.util;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.ibm.ws.kernel.boot.cmdline.Utils;
import com.ibm.ws.kernel.boot.internal.KernelUtils;
import com.ibm.ws.kernel.feature.internal.FeatureResolverImpl;
import com.ibm.ws.kernel.feature.internal.subsystem.FeatureRepository;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver;
import com.ibm.ws.kernel.provisioning.BundleRepositoryRegistry;

import componenttest.common.apiservices.Bootstrap;

public class FATFeatureResolver {
    public static void setup() throws Exception {
        setRepository();
        setResolver();
    }

    public static String getInstallRoot() throws Exception {
        return Bootstrap.getInstance().getValue("libertyInstallPath");
        // 'getInstance' throws Exception
    }

    //

    private static FeatureRepository repository;

    public static void setRepository() throws Exception {
        if (repository != null) {
            return;
        }

        File lib = new File(getInstallRoot(), "lib");

        Utils.setInstallDir(lib.getParentFile());
        KernelUtils.setBootStrapLibDir(lib);

        // Null: Don't cache.
        BundleRepositoryRegistry.initializeDefaults(null, true);

        repository = new FeatureRepository();
        repository.init();
    }

    public static FeatureRepository getRepository() {
        return repository;
    }

    //

    private static FeatureResolver resolver;

    public static void setResolver() {
        if (resolver != null) {
            return;
        }

        resolver = new FeatureResolverImpl();
    }

    public FeatureResolver getResolver() {
        return resolver;
    }

    private static final Set<String> EMPTY_STRINGS = Collections.emptySet();
    private static final Set<ProvisioningFeatureDefinition> EMPTY_DEFS = Collections.emptySet();

    public static FeatureResolver.Result resolve(Collection<String> rootFeatures) {
        return resolver.resolveFeatures(repository,
                                        EMPTY_DEFS, rootFeatures,
                                        EMPTY_STRINGS,
                                        false);
    }

    public static FeatureResolver.Result resolve(Collection<String> rootFeatures, Collection<String> platforms) {
        return resolver.resolve(repository,
                                EMPTY_DEFS, rootFeatures,
                                EMPTY_STRINGS,
                                false,
                                platforms);
    }
}
