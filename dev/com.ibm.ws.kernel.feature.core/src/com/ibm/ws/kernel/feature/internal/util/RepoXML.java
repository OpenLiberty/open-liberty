/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.util;

import static com.ibm.ws.kernel.feature.internal.util.RepoXMLConstants.AUTO_TAG;
import static com.ibm.ws.kernel.feature.internal.util.RepoXMLConstants.CHECKSUM_TAG;
import static com.ibm.ws.kernel.feature.internal.util.RepoXMLConstants.CLIENT_TAG;
import static com.ibm.ws.kernel.feature.internal.util.RepoXMLConstants.CONSTITUENT_TAG;
import static com.ibm.ws.kernel.feature.internal.util.RepoXMLConstants.FEATURE_TAG;
import static com.ibm.ws.kernel.feature.internal.util.RepoXMLConstants.FILE_TAG;
import static com.ibm.ws.kernel.feature.internal.util.RepoXMLConstants.IBM_VERSION_TAG;
import static com.ibm.ws.kernel.feature.internal.util.RepoXMLConstants.NAME_TAG;
import static com.ibm.ws.kernel.feature.internal.util.RepoXMLConstants.REPOSITORY_TAG;
import static com.ibm.ws.kernel.feature.internal.util.RepoXMLConstants.RESTART_TAG;
import static com.ibm.ws.kernel.feature.internal.util.RepoXMLConstants.SERVER_TAG;
import static com.ibm.ws.kernel.feature.internal.util.RepoXMLConstants.SHORT_TAG;
import static com.ibm.ws.kernel.feature.internal.util.RepoXMLConstants.SINGLETON_TAG;
import static com.ibm.ws.kernel.feature.internal.util.RepoXMLConstants.SUPPORTED_VERSION_TAG;
import static com.ibm.ws.kernel.feature.internal.util.RepoXMLConstants.SYMBOLIC_TAG;
import static com.ibm.ws.kernel.feature.internal.util.RepoXMLConstants.VERSION_TAG;
import static com.ibm.ws.kernel.feature.internal.util.RepoXMLConstants.VISIBILITY_TAG;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.ibm.ws.kernel.feature.ProcessType;
import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.provisioning.FeatureResource;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Repository;
import com.ibm.ws.kernel.feature.resolver.FeatureResolver.Selector;

//Source restricted to java7.

public class RepoXML extends BaseXML {

    public static Selector<ProvisioningFeatureDefinition> IS_PUBLIC = new Selector<ProvisioningFeatureDefinition>() {
        @Override
        public boolean test(ProvisioningFeatureDefinition def) {
            return isPublic(def);
        }
    };

    public static boolean isPublic(ProvisioningFeatureDefinition def) {
        return (def.getVisibility() == Visibility.PUBLIC);
    }

    public static Selector<ProvisioningFeatureDefinition> IS_CLIENT = new Selector<ProvisioningFeatureDefinition>() {
        @Override
        public boolean test(ProvisioningFeatureDefinition def) {
            return isClient(def);
        }
    };

    public static boolean isClient(ProvisioningFeatureDefinition def) {
        return (def.getProcessTypes().contains(ProcessType.CLIENT));
    }

    public static Selector<ProvisioningFeatureDefinition> IS_SERVER = new Selector<ProvisioningFeatureDefinition>() {
        @Override
        public boolean test(ProvisioningFeatureDefinition def) {
            return isServer(def);
        }
    };

    public static boolean isServer(ProvisioningFeatureDefinition def) {
        return (def.getProcessTypes().contains(ProcessType.SERVER));
    }

    private static Comparator<ProvisioningFeatureDefinition> COMPARE = new Comparator<ProvisioningFeatureDefinition>() {
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
        @Override
        public int compare(ProvisioningFeatureDefinition def1,
                           ProvisioningFeatureDefinition def2) {
            return def1.getSymbolicName().compareToIgnoreCase(def2.getSymbolicName());
        }
    };

    public static void write(File file, final List<ProvisioningFeatureDefinition> features) throws Exception {
        write(file, new FailableConsumer<PrintWriter, Exception>() {
            @Override
            public void accept(PrintWriter pW) throws Exception {
                try (RepoXMLWriter xW = new RepoXMLWriter(pW)) {
                    xW.write(features);
                }
            }
        });
    }

    public static void write(File file, final Repository repo) throws Exception {
        write(file, new FailableConsumer<PrintWriter, Exception>() {
            @Override
            public void accept(PrintWriter pW) throws Exception {
                try (RepoXMLWriter xW = new RepoXMLWriter(pW)) {
                    xW.write(repo);
                }
            }
        });
    }

    public static class RepoXMLWriter extends BaseXMLWriter {
        public RepoXMLWriter(PrintWriter pW) {
            super(pW);
        }

        public void write(List<ProvisioningFeatureDefinition> features) {
            openElement(REPOSITORY_TAG);
            upIndent();

            for (ProvisioningFeatureDefinition def : features) {
                write(def);
            }

            downIndent();
            closeElement(REPOSITORY_TAG);
        }

        public void write(Repository repo) {
            List<ProvisioningFeatureDefinition> features = repo.getFeatures();
            ProvisioningFeatureDefinition[] featuresArray = features.toArray( new ProvisioningFeatureDefinition[ features.size() ]);
            Arrays.sort(featuresArray, RepoXML.COMPARE);

            openElement(REPOSITORY_TAG);
            upIndent();

            for (ProvisioningFeatureDefinition def : featuresArray) {
                write(def);
            }

            downIndent();
            closeElement(REPOSITORY_TAG);
        }

        public void write(ProvisioningFeatureDefinition def) {
            openElement(FEATURE_TAG);
            upIndent();

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

            for (FeatureResource resource : def.getConstituents(null)) {
                printElement(CONSTITUENT_TAG, resource.getLocation());
            }

            downIndent();
            closeElement(FEATURE_TAG);
        }
    }
}
