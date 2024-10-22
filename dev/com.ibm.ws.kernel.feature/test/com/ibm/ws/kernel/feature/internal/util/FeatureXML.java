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

import static com.ibm.ws.kernel.feature.internal.util.FeatureXMLConstants.XML_ALSO_KNOWN_AS;
import static com.ibm.ws.kernel.feature.internal.util.FeatureXMLConstants.XML_API_PACKAGE;
import static com.ibm.ws.kernel.feature.internal.util.FeatureXMLConstants.XML_DEPENDENT;
import static com.ibm.ws.kernel.feature.internal.util.FeatureXMLConstants.XML_DESCRIPTION;
import static com.ibm.ws.kernel.feature.internal.util.FeatureXMLConstants.XML_DISABLE_ON_CONFLICT;
import static com.ibm.ws.kernel.feature.internal.util.FeatureXMLConstants.XML_EDITION;
import static com.ibm.ws.kernel.feature.internal.util.FeatureXMLConstants.XML_FEATURE;
import static com.ibm.ws.kernel.feature.internal.util.FeatureXMLConstants.XML_FEATURES;
import static com.ibm.ws.kernel.feature.internal.util.FeatureXMLConstants.XML_FEATURE_NAME;
import static com.ibm.ws.kernel.feature.internal.util.FeatureXMLConstants.XML_FORCE_RESTART;
import static com.ibm.ws.kernel.feature.internal.util.FeatureXMLConstants.XML_INSTANT_ON_ENABLED;
import static com.ibm.ws.kernel.feature.internal.util.FeatureXMLConstants.XML_IS_SINGLETON;
import static com.ibm.ws.kernel.feature.internal.util.FeatureXMLConstants.XML_KIND;
import static com.ibm.ws.kernel.feature.internal.util.FeatureXMLConstants.XML_LICENSE_AGREEMENT;
import static com.ibm.ws.kernel.feature.internal.util.FeatureXMLConstants.XML_LICENSE_INFORMATION;
import static com.ibm.ws.kernel.feature.internal.util.FeatureXMLConstants.XML_PARALLEL;
import static com.ibm.ws.kernel.feature.internal.util.FeatureXMLConstants.XML_PLATFORMS;
import static com.ibm.ws.kernel.feature.internal.util.FeatureXMLConstants.XML_SHORT_NAME;
import static com.ibm.ws.kernel.feature.internal.util.FeatureXMLConstants.XML_SINGLETON;
import static com.ibm.ws.kernel.feature.internal.util.FeatureXMLConstants.XML_SPI_PACKAGE;
import static com.ibm.ws.kernel.feature.internal.util.FeatureXMLConstants.XML_SUBSYSTEM_SYMBOLIC_NAME;
import static com.ibm.ws.kernel.feature.internal.util.FeatureXMLConstants.XML_VISIBILITY;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

//Source restricted to java7.

public class FeatureXML extends BaseXML {
    /**
     * Sort a list of features based on their symbolic names.
     *
     * Ignore case.
     *
     * @param features Features which are to be sorted.
     */
    public static void sort(List<FeatureInfo> features) {
        Collections.sort(features, COMPARE);
    }

    private static Comparator<FeatureInfo> COMPARE = new Comparator<FeatureInfo>() {
        /**
         * Compare two features by their name.
         *
         * Use a case insensitive comparison.
         *
         * @param fInfo1 A feature fInfoinition which is to be compared.
         * @param fInfo2 Another feature fInfoinition which is to be compared.
         *
         * @return The features compared by their symbolic name.
         */
        @Override
        public int compare(FeatureInfo f0, FeatureInfo f1) {
            return f0.getName().compareToIgnoreCase(f1.getName());
        }
    };

    public static void write(File file, final List<FeatureInfo> features) throws Exception {
        write(file, new FailableConsumer<PrintWriter, Exception>() {
            @Override
            public void accept(PrintWriter pW) throws Exception {
                @SuppressWarnings("resource")
                FeatureXMLWriter xW = new FeatureXMLWriter(pW);
                try {
                    xW.write(features);
                } finally {
                    xW.flush();
                }
            }
        });
    }

    public static class FeatureXMLWriter extends BaseXMLWriter {
        public FeatureXMLWriter(PrintWriter pW) {
            super(pW);
        }

        public void write(List<FeatureInfo> features) {
            openElement(XML_FEATURES);
            upIndent();

            for (FeatureInfo fInfo : features) {
                write(fInfo);
            }

            downIndent();
            closeElement(XML_FEATURES);
        }

        public void write(FeatureInfo fInfo) {
            openElement(XML_FEATURE);
            upIndent();

            printElement(XML_SUBSYSTEM_SYMBOLIC_NAME, fInfo.getName());
            printOptionalElement(XML_FEATURE_NAME, fInfo.getFeatureName());
            printOptionalElement(XML_DESCRIPTION, fInfo.getDescription());

            printOptionalElement(XML_SHORT_NAME, fInfo.getShortName());
            printOptionalElement(XML_ALSO_KNOWN_AS, fInfo.getAlsoKnownAs());

            printDelimited(XML_PLATFORMS, ", ", fInfo.getPlatforms());

            printOptionalElement(XML_EDITION, fInfo.getEdition());
            printOptionalElement(XML_KIND, fInfo.getKind());
            printOptionalElement(XML_SINGLETON, fInfo.isSingleton());
            printOptionalElement(XML_VISIBILITY, fInfo.getVisibility());
            printOptionalElement(XML_IS_SINGLETON, fInfo.isSingleton());

            printOptionalElement(XML_FORCE_RESTART, fInfo.isForceAppRestartEnabled());
            printOptionalElement(XML_PARALLEL, fInfo.isParallelActivationEnabled());

            printOptionalElement(XML_DISABLE_ON_CONFLICT, fInfo.isDisableOnConflictEnabled());
            printOptionalElement(XML_INSTANT_ON_ENABLED, fInfo.isInstantOnEnabled());

            printOptionalElement(XML_LICENSE_INFORMATION, fInfo.getLicenseInformation());
            printOptionalElement(XML_LICENSE_AGREEMENT, fInfo.getLicenseAgreement());

            writePIs(XML_API_PACKAGE, fInfo.getAPIs());
            writePIs(XML_SPI_PACKAGE, fInfo.getSPIs());

            printElements(XML_DEPENDENT, fInfo.getDependentNames());

            downIndent();
            closeElement(XML_FEATURE);
        }

        public void writePIs(String xmlTag, List<FeatureInfo.ExternalPackageInfo> pis) {
            if (pis == null) {
                return;
            }
            for (FeatureInfo.ExternalPackageInfo pi : pis) {
                printElement(xmlTag, pi.getPackageName());
            }
        }
    }
}
