/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
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

package com.ibm.ws.feature.tests.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Builder;

public class FeatureInfo {

    public FeatureInfo(File featureFile) throws IOException {
        this.featureFile = featureFile;

        // System.out.println("Reading [ " + featureFile.getName() + " ]");

        try (FeatureBuilder builder = new FeatureBuilder()) {
            builder.setProperties(featureFile);

            String rawVisibility = builder.getProperty(FeatureConstants.VISIBILITY);
            if (rawVisibility != null) {
                rawVisibility = rawVisibility.trim();
            } else {
                rawVisibility = FeatureConstants.VISIBILITY_PRIVATE;
            }
            this.visibility = rawVisibility;
            this.isPublic = (rawVisibility.toLowerCase().equals(FeatureConstants.VISIBILITY_PUBLIC));
            this.isPrivate = (rawVisibility.toLowerCase().equals(FeatureConstants.VISIBILITY_PRIVATE));

            this.isAutoFeature = (builder.getProperty(FeatureConstants.IBM_PROVISION_CAPABILITY) != null);

            // this.name = builder.getProperty(FeatureConstants.SUBSYSTEM_SYMBOLIC_NAME);
            this.name = builder.getProperty(FeatureConstants.SYMBOLIC_NAME);

            // Versions cannot always be parsed off of the names of auto features.
            // For example, this auto-feature has no version:
            //   com.ibm.websphere.appserver.cdi2.0-appSecurity1.0
            // There *are* auto-features with an apparent version, for example:
            //   com.ibm.websphere.appserver.beanValidationCDI-1.0
            //   com.ibm.websphere.appserver.beanValidationCDI-2.0
            // That version is not used when resolving features.

            this.baseName = (this.isAutoFeature ? this.name : getBaseName(this.name));
            this.version = (this.isAutoFeature ? null : getVersion(this.name));

            this.featureName = builder.getProperty(FeatureConstants.SUBSYSTEM_NAME);
            this.description = builder.getProperty(FeatureConstants.SUBSYSTEM_DESCRIPTION);

            this.shortName = builder.getProperty(FeatureConstants.IBM_SHORT_NAME);
            this.alsoKnownAs = builder.getProperty(FeatureConstants.WLP_ALSO_KNOWN_AS);

            this.edition = builder.getProperty(FeatureConstants.EDITION);
            this.kind = builder.getProperty(FeatureConstants.KIND);

            String singleton = builder.getProperty(FeatureConstants.SINGLETON);
            this.isSingleton = ((singleton != null) && "true".equals(singleton.trim()));

            String activationType = builder.getProperty(FeatureConstants.WLP_ACTIVATION_TYPE);
            this.isParallelActivationSet = (activationType != null);
            this.isParallelActivationEnabled = ((activationType != null) && FeatureConstants.WLP_ACTIVATION_TYPE_PARALLEL.equals(activationType.trim()));

            String forceAppRestart = builder.getProperty(FeatureConstants.IBM_APP_FORCE_RESTART);
            this.isForceAppRestartSet = (forceAppRestart != null);
            this.isForceAppRestartEnabled = ((forceAppRestart == null) || "true".equals(forceAppRestart));

            String disableOnConflict = builder.getProperty(FeatureConstants.WLP_DISABLE_ON_CONFLICT);
            this.isDisableOnConflictSet = (disableOnConflict != null);
            this.isDisableOnConflictEnabled = ((disableOnConflict == null) || "true".equals(disableOnConflict));

            String instantOnEnabled = builder.getProperty(FeatureConstants.WLP_INSTANT_ON_ENABLED);
            this.isInstantOnEnabledSet = (instantOnEnabled != null);
            this.isInstantOnEnabled = ((instantOnEnabled == null) || "true".equals(instantOnEnabled.trim()));

            this.licenseInformation = builder.getProperty(FeatureConstants.IBM_LICENSE_INFORMATION);
            this.licenseAgreement = builder.getProperty(FeatureConstants.IBM_LICENSE_AGREEMENT);

            //

            Set<Map.Entry<String, Attrs>> rawDeps = builder.getFeatures();

            List<String> depNames = new ArrayList<>(rawDeps.size());
            Map<String, Attrs> depsMap = new LinkedHashMap<>(rawDeps.size());

            for (Map.Entry<String, Attrs> depEntry : rawDeps) {
                String depName = depEntry.getKey();
                Attrs depAttrs = depEntry.getValue();

                depNames.add(depName);
                depsMap.put(depName, depAttrs);
            }

            String[] depNamesArray = depNames.toArray(new String[depNames.size()]);
            Arrays.sort(depNamesArray);

            this.dependentNames = depNamesArray;
            this.dependentFeatures = depsMap;

            Set<String> rawAutoFeatures = new LinkedHashSet<String>();
            for (String autoFeature : builder.getAutoFeatures()) {
                rawAutoFeatures.add(autoFeature);
            }
            this.autoFeatures = rawAutoFeatures.toArray(new String[rawAutoFeatures.size()]);

            //

            String rawAPIs = builder.getProperty(FeatureConstants.IBM_API_PACKAGE);
            this.apis = parseExternalPackages(rawAPIs, null);

            String rawSPIs = builder.getProperty(FeatureConstants.IBM_SPI_PACKAGE);
            this.spis = parseExternalPackages(rawSPIs, FeatureConstants.IBM_SPI);

            //

            this.platforms = parseList(builder.getProperty(FeatureConstants.WLP_PLATFORM));

            //

            this.hashCode = ((name == null) ? 0 : name.hashCode());

            this.toString = getClass().getSimpleName() +
                            "( name=\"" + this.name + "\"" +
                            ", visibility=\"" + visibility + "\"" +
                            ", edition=\"" + edition + "\")";
        }
    }

    private final int hashCode;

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
        } else if (!(obj instanceof FeatureInfo)) {
            return false;
        } else {
            FeatureInfo other = (FeatureInfo) obj;
            if (hashCode() != other.hashCode()) {
                return false;
            }

            String thisName = getName();
            String otherName = other.getName();
            if (thisName == null) {
                return (otherName == null);
            } else if (otherName == null) {
                return false;
            } else {
                return thisName.equals(otherName);
            }
        }
    }

    private final String toString;

    @Override
    public String toString() {
        return toString;
    }

    //

    private final File featureFile;

    public File getFeatureFile() {
        return featureFile;
    }

    //

    // name: symbolicName=com.ibm.websphere.appserver.acmeCA-2.0
    // featureName: Subsystem-Name: Automatic Certificate Management Environment (ACME) Support 2.0

    private final String name; // The symbolic name
    private final String baseName;
    private final String version;

    private final String featureName; // The long descriptive name

    public String getName() {
        return name;
    }

    public String getBaseName() {
        return baseName;
    }

    public String getVersion() {
        return version;
    }

    public String getFeatureName() {
        return featureName;
    }

    //

    private final String edition;
    private final String kind;

    public String getEdition() {
        return edition;
    }

    public String getKind() {
        return kind;
    }

    //

    private final String shortName;
    private final String alsoKnownAs;

    private final String description;

    private final boolean isSingleton;
    private final String visibility;
    private final boolean isPublic;
    private final boolean isPrivate;

    public String getShortName() {
        return shortName;
    }

    public String getAlsoKnownAs() {
        return alsoKnownAs;
    }

    public boolean isAlsoKnownAsSet() {
        return (alsoKnownAs != null);
    }

    public String getDescription() {
        return description;
    }

    public boolean isSingleton() {
        return isSingleton;
    }

    public String getVisibility() {
        return visibility;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    //

    private final boolean isAutoFeature;

    private final boolean isParallelActivationSet;
    private final boolean isParallelActivationEnabled;

    private final boolean isForceAppRestartSet;
    private final boolean isForceAppRestartEnabled;

    private final boolean isDisableOnConflictSet;
    private final boolean isDisableOnConflictEnabled;

    private final boolean isInstantOnEnabledSet;
    private final boolean isInstantOnEnabled;

    public boolean isAutoFeature() {
        return isAutoFeature;
    }

    public boolean isParallelActivationSet() {
        return isParallelActivationSet;
    }

    public boolean isParallelActivationEnabled() {
        return isParallelActivationEnabled;
    }

    public boolean isForceAppRestartSet() {
        return isForceAppRestartSet;
    }

    public boolean isForceAppRestartEnabled() {
        return isForceAppRestartEnabled;
    }

    public boolean isDisableOnConflictSet() {
        return isDisableOnConflictSet;
    }

    public boolean isDisableOnConflictEnabled() {
        return isDisableOnConflictEnabled;
    }

    public boolean isInstantOnEnabledSet() {
        return isInstantOnEnabledSet;
    }

    public boolean isInstantOnEnabled() {
        return isInstantOnEnabled;
    }

    private final String licenseInformation;
    private final String licenseAgreement;

    public String getLicenseInformation() {
        return licenseInformation;
    }

    public String getLicenseAgreement() {
        return licenseAgreement;
    }

    //

    private final String[] dependentNames;
    private final Map<String, Attrs> dependentFeatures;

    public String[] getDependentNames() {
        return dependentNames;
    }

    public Map<String, Attrs> getDependentFeatures() {
        return dependentFeatures;
    }

    //

    private String[] autoFeatures;

    public String[] getAutoFeatures() {
        return autoFeatures;
    }

    //

    private List<ExternalPackageInfo> apis;
    private List<ExternalPackageInfo> spis;

    public List<ExternalPackageInfo> getAPIs() {
        return apis;
    }

    public List<ExternalPackageInfo> getSPIs() {
        return spis;
    }

    //

    private final List<String> platforms;

    public List<String> getPlatforms() {
        return platforms;
    }

    //

    public static class ExternalPackageInfo {
        protected ExternalPackageInfo(String packageName, String type) {
            this.packageName = packageName;
            this.type = type;

            int pHash = (packageName == null) ? 0 : packageName.hashCode();
            int tHash = (type == null) ? 0 : type.hashCode();

            this.hashCode = pHash * 31 + tHash;

            this.asString = packageName + " [" + type + "]";
        }

        private final String packageName;
        private final String type;

        public String getPackageName() {
            return packageName;
        }

        public String getType() {
            return type;
        }

        //

        private final int hashCode;

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            } else if (this == obj) {
                return true;
            } else if (getClass() != obj.getClass()) {
                return false;
            }

            ExternalPackageInfo other = (ExternalPackageInfo) obj;

            String otherPackageName = other.packageName;
            if (packageName == null) {
                if (otherPackageName != null) {
                    return false;
                }
            } else if (otherPackageName == null) {
                return false;
            } else if (!packageName.equals(otherPackageName)) {
                return false;
            }

            String otherType = other.type;
            if (type == null) {
                if (otherType != null) {
                    return false;
                }
            } else if (otherType == null) {
                return false;
            } else if (!type.equals(otherType)) {
                return false;
            }

            return true;
        }

        private final String asString;

        @Override
        public String toString() {
            return asString;
        }
    }

    private static List<ExternalPackageInfo> parseExternalPackages(String packageList, String defaultType) {
        if (packageList == null) {
            return null;
        }

        List<ExternalPackageInfo> storage = new ArrayList<>();

        String[] packageNames = packageList.split(",");
        for (String packageName : packageNames) {
            String[] packageParts = packageName.split(";");

            String externalPackage = packageParts[0].trim();

            String type = null;
            if (packageParts.length > 1) {
                for (int partNo = 1; partNo < packageParts.length; ++partNo) {
                    String packagePart = packageParts[partNo].trim();
                    if (packagePart.startsWith("type")) {
                        type = packagePart.substring(packagePart.indexOf('=') + 1).trim();
                        while (type.startsWith("\"")) {
                            type = type.substring(1);
                        }
                        while (type.endsWith("\"")) {
                            type = type.substring(0, type.length() - 1);
                        }
                        break;
                    }
                }
            }

            if (type == null) {
                type = defaultType;
            }

            storage.add(new ExternalPackageInfo(externalPackage, type));
        }

        return new ArrayList<>(storage);
    }

    //

    public static List<String> parseList(String raw) {
        if (raw == null) {
            return null;
        }

        String[] rawElements = raw.split(",");
        List<String> asList = new ArrayList<>(rawElements.length);
        for (String rawElement : rawElements) {
            rawElement = rawElement.trim();
            if (!rawElement.isEmpty()) {
                asList.add(rawElement);
            }
        }
        return (asList.isEmpty() ? null : asList);
    }

    public static String getBaseName(String featureName) {
        int versionIndex = featureName.lastIndexOf('-');
        if (versionIndex != -1) {
            return featureName.substring(0, versionIndex);
        } else {
            return featureName;
        }
    }

    public static String getVersion(String featureName) {
        int versionIndex = featureName.lastIndexOf('-');
        if (versionIndex != -1) {
            return featureName.substring(versionIndex + 1, featureName.length());
        } else {
            return null;
        }
    }

    public static class FeatureBuilder extends Builder {

        public Parameters getSubsystemContent() {
            return getParameters(FeatureConstants.SUBSYSTEM_CONTENT);
        }

        //

        public Set<Map.Entry<String, Attrs>> getFiles() {
            return getContent(FeatureConstants.IBM_CONTENT_FILES);
        }

        public Set<Map.Entry<String, Attrs>> getJars() {
            return getContent(FeatureConstants.IBM_CONTENT_JARS);
        }

        public Set<Map.Entry<String, Attrs>> getFeatures() {
            return getContent(FeatureConstants.IBM_CONTENT_FEATURES);
        }

        public Set<Map.Entry<String, Attrs>> getBundles() {
            return getContent(FeatureConstants.IBM_CONTENT_BUNDLES);
        }

        public Set<Map.Entry<String, Attrs>> getIBMProvisionCapability() {
            return getContent(FeatureConstants.IBM_PROVISION_CAPABILITY);
        }

        public Set<String> getAutoFeatures() {
            Set<String> autoFeatures = new HashSet<String>();

            Set<Entry<String, Attrs>> content = getContent(FeatureConstants.IBM_PROVISION_CAPABILITY);
            String rawContent = content.toString();
            String[] contentElements = rawContent.split(FeatureConstants.OSGI_IDENTITY);

            for (String contentElement : contentElements) {
                if (isAuto(contentElement)) {
                    autoFeatures.add(trimContentElement(contentElement));
                }
            }

            return autoFeatures;
        }

        private static boolean isAuto(String feature) {
            return feature.startsWith("com") || feature.startsWith("io.openliberty");
        }

        private static String trimContentElement(String autoFeature) {
            int closeIndex = autoFeature.indexOf(")");
            if (closeIndex > 0) {
                return autoFeature.substring(0, closeIndex);
            } else {
                return autoFeature;
            }
        }

        //

        private Set<Map.Entry<String, Attrs>> getContent(String contentType) {
            String rawContent = getProperty(contentType, "");
            if ((rawContent == null) || rawContent.isEmpty()) {
                return Collections.emptySet();
            }

            return (new Parameters(rawContent)).entrySet();
        }
    }
}
