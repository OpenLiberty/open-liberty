/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.feature.resolver.util;

import static com.ibm.ws.kernel.feature.resolver.util.RepoXMLConstants.AUTO_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.RepoXMLConstants.CHECKSUM_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.RepoXMLConstants.CLIENT_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.RepoXMLConstants.CONSTITUENT_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.RepoXMLConstants.FEATURE_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.RepoXMLConstants.FILE_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.RepoXMLConstants.IBM_VERSION_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.RepoXMLConstants.NAME_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.RepoXMLConstants.REPOSITORY_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.RepoXMLConstants.RESTART_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.RepoXMLConstants.SERVER_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.RepoXMLConstants.SHORT_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.RepoXMLConstants.SINGLETON_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.RepoXMLConstants.SUPPORTED_VERSION_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.RepoXMLConstants.SYMBOLIC_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.RepoXMLConstants.VERSION_TAG;
import static com.ibm.ws.kernel.feature.resolver.util.RepoXMLConstants.VISIBILITY_TAG;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Stream;

import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Repository;

public class RepoXML extends BaseXML {
    public static boolean isPublic(ProvisioningFeatureDefinition def) {
        return (def.getVisibility() == Visibility.PUBLIC);
    }

    public static boolean isClient(ProvisioningFeatureDefinition def) {
        return (def.getProcessTypes().contains(ProcessType.CLIENT));
    }

    public static boolean isServer(ProvisioningFeatureDefinition def) {
        return (def.getProcessTypes().contains(ProcessType.SERVER));
    }

    /**
     * Compare two features by their symbolic name.
     *
     * Use a case insensitive comparison.
     *
     * @param def1 A feature definition which is to be compared.
     * @param def2 Another feature definition which is to be compared.
     *
     * @return The features compared by their symbolic name.
     */
    private static int compare(ProvisioningFeatureDefinition def1,
                               ProvisioningFeatureDefinition def2) {
        return def1.getSymbolicName().compareToIgnoreCase(def2.getSymbolicName());
    }

    public static void write(File file, Stream<ProvisioningFeatureDefinition> features) throws Exception {
        write(file, (PrintWriter pW) -> {
            try (RepoXMLWriter xW = new RepoXMLWriter(pW)) {
                xW.write(features);
            }
        });
    }

    public static void write(File file, Repository repo) throws Exception {
        write(file, (PrintWriter pW) -> {
            try (RepoXMLWriter xW = new RepoXMLWriter(pW)) {
                xW.write(repo);
            }
        });
    }

    public static class RepoXMLWriter extends BaseXMLWriter {
        public RepoXMLWriter(PrintWriter pW) {
            super(pW);
        }

        public void write(Stream<ProvisioningFeatureDefinition> features) {
            withinElement(REPOSITORY_TAG, () -> {
                features.forEach(this::write);
            });
        }

        public void write(Repository repo) {
            List<ProvisioningFeatureDefinition> features = repo.select(RepoXML::isPublic);
            features.sort(RepoXML::compare);

            withinElement(REPOSITORY_TAG, () -> {
                features.forEach((ProvisioningFeatureDefinition def) -> write(def));
            });
        }

        public void write(ProvisioningFeatureDefinition def) {
            withinElement(FEATURE_TAG, () -> {
                printElement(FILE_TAG, def.getFeatureDefinitionFile().getName());
                printElement(CHECKSUM_TAG, def.getFeatureChecksumFile().getName());

                printElement(NAME_TAG, def.getFeatureName());
                printElement(SYMBOLIC_TAG, def.getSymbolicName());
                printElement(SHORT_TAG, def.getIbmShortName());
                printElement(VERSION_TAG, def.getVersion().toString());
                printElement(IBM_VERSION_TAG, Integer.toString(def.getIbmFeatureVersion()));
                printElement(SUPPORTED_VERSION_TAG, def.isSupportedFeatureVersion());

                printElement(VISIBILITY_TAG, def.getVisibility().toString());
                printElement(AUTO_TAG, def.isAutoFeature());
                printElement(SINGLETON_TAG, def.isSingleton());
                printElement(SERVER_TAG, def.getProcessTypes().contains(SERVER_TAG));
                printElement(CLIENT_TAG, def.getProcessTypes().contains(CLIENT_TAG));

                printElement(RESTART_TAG, def.getAppForceRestart().toString());

                def.getConstituents(null).forEach((FeatureResource resource) -> printElement(CONSTITUENT_TAG, resource.getLocation()));
            });
        }
    }
}
