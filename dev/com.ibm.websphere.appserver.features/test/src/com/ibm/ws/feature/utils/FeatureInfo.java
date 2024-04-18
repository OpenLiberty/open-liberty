/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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

package com.ibm.ws.feature.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import com.ibm.ws.feature.tasks.FeatureBnd;
import com.ibm.ws.feature.tasks.FeatureBuilder;

import aQute.bnd.header.Attrs;

public class FeatureInfo {

    private String[] lockedAutoFeatures;
    private Map<String, Attrs> lockedDependentFeatures;
    private String[] lockedActivatingAutoFeature;

    private Set<String> autoFeatures = new LinkedHashSet<String>();
    private Map<String, Attrs> dependentFeatures = new LinkedHashMap<String, Attrs>();
    private Set<String> activatingAutoFeature = new LinkedHashSet<String>();
    private List<String> sortedDependentNames;

    private String edition;
    private String kind;

    private boolean isInit = false;
    private final File feature;
    private String name;
    private boolean isAutoFeature = false;
    private boolean isParallelActivationEnabled = false;
    private boolean isDisableOnConflictEnabled = true;
    private boolean isDisableOnConflictSet = false;
    private boolean isAlsoKnownAsSet = false;
    private String alsoKnownAs;
    private boolean isSingleton = false;
    private String visibility = "private";
    private String shortName;

    // Using a list in order to find duplicates.
    private List<ExternalPackageInfo> APIs;
    private List<ExternalPackageInfo> SPIs;

    public FeatureInfo(File feature) {
        this.feature = feature;
    }

    public File getFeatureFile() {
        return feature;
    }

    public String[] getAutoFeatures() {
        if (!isInit)
            populateInfo();

        return this.lockedAutoFeatures;
    }

    public String getName() {
        if (!isInit)
            populateInfo();

        return this.name;
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

    public String getBaseName() {
        if (!isInit)
            populateInfo();

        return this.name.split("-")[0];
    }

    public String getVersion() {
        if (!isInit)
            populateInfo();

        if (this.shortName.split("-").length > 1) {
            return this.shortName.split("-")[1];
        }
        return null;
    }

    public boolean isAutoFeature() {
        if (!isInit)
            populateInfo();

        return this.isAutoFeature;
    }

    public boolean isParallelActivationEnabled() {
        if (!isInit)
            populateInfo();

        return this.isParallelActivationEnabled;
    }

    public boolean isDisableOnConflictEnabled() {
        if (!isInit)
            populateInfo();

        return this.isDisableOnConflictEnabled;
    }

    public boolean isDisableOnConflictSet() {
        if (!isInit)
            populateInfo();

        return this.isDisableOnConflictSet;
    }

    public boolean isAlsoKnownAsSet() {
        if (!isInit)
            populateInfo();

        return this.isAlsoKnownAsSet;
    }

    public String getAlsoKnownAs() {
        if (!isInit)
            populateInfo();

        return this.alsoKnownAs;
    }

    public boolean isSingleton() {
        if (!isInit)
            populateInfo();

        return this.isSingleton;
    }

    public String getVisibility() {
        if (!isInit)
            populateInfo();

        return this.visibility;
    }

    public boolean isPublic() {
        if (!isInit)
            populateInfo();

        if(this.visibility.toLowerCase().equals("public")){
            return true;
        }
        return false;
    }

    //Activating autofeature just means "I'm an autofeature, and i *might* activate this other feature
    //So it's like a "Sometimes" dependency, but is potentially useful for figuring out a superset of
    //potential provisioned features.
    protected void addActivatingAutoFeature(String featureName) {
        if (!isInit)
            populateInfo();

        if (activatingAutoFeaturesLocked)
            return;

        this.activatingAutoFeature.add(featureName);
    }

    public String[] getActivatingAutoFeatures() {
        if (activatingAutoFeaturesLocked)
            return this.lockedActivatingAutoFeature;
        else
            return null;

    }

    private boolean activatingAutoFeaturesLocked = false;

    protected synchronized void lockActivatingAutoFeatures() {
        this.lockedActivatingAutoFeature = this.activatingAutoFeature.toArray(new String[this.activatingAutoFeature.size()]);
        activatingAutoFeaturesLocked = true;
        activatingAutoFeature = null;
    }

    public Map<String, Attrs> getDependentFeatures() {
        if (!isInit)
            populateInfo();

        return this.lockedDependentFeatures;
    }

    public List<String> getSortedDependentNames() {
        return sortedDependentNames;
    }

    public void forEachSortedDepName(Consumer<? super String> consumer) {
        getSortedDependentNames().forEach(consumer);
    }

    public String getEdition() {
        if (!isInit)
            populateInfo();

        return this.edition;

    }

    public String getKind() {
        if (!isInit)
            populateInfo();

        return this.kind;
    }

    public String getShortName() {
        if (!isInit)
            populateInfo();

        return this.shortName;
    }

    public List<ExternalPackageInfo> getAPIs() {
        if (!isInit)
            populateInfo();

        return this.APIs;
    }

    public List<ExternalPackageInfo> getSPIs() {
        if (!isInit)
            populateInfo();

        return this.SPIs;
    }

    private synchronized void populateInfo() {
        if (isInit)
            return;

        FeatureBuilder builder = new FeatureBuilder();

        try {
            builder.setProperties(this.feature);

            String edition = builder.getProperty("edition");
            String kind = builder.getProperty("kind");
            this.name = builder.getProperty("symbolicName");
            this.isAutoFeature = builder.getProperty(FeatureBnd.IBM_PROVISION_CAPABILITY) != null;
            String activationType = builder.getProperty("WLP-Activation-Type");
            this.isParallelActivationEnabled = activationType != null && "parallel".equals(activationType.trim());
            String disableOnConflict = builder.getProperty("WLP-DisableAllFeatures-OnConflict");
            this.isDisableOnConflictSet = disableOnConflict != null;
            this.isDisableOnConflictEnabled = disableOnConflict == null || "true".equals(disableOnConflict);
            this.isAlsoKnownAsSet = builder.getProperty("WLP-AlsoKnownAs") != null;
            this.alsoKnownAs = builder.getProperty("WLP-AlsoKnownAs");
            String singleton = builder.getProperty("singleton");
            this.isSingleton = singleton != null && "true".equals(singleton.trim());
            String vis = builder.getProperty("visibility");
            if (vis != null) {
                visibility = vis.trim();
            }
            this.shortName = builder.getProperty(FeatureBnd.IBM_SHORT_NAME);

            this.edition = edition;
            this.kind = kind;

            String ibmAPIsString = builder.getProperty("IBM-API-Package");
            String ibmSPIsString = builder.getProperty("IBM-SPI-Package");

            this.APIs = parseExternalPackages(ibmAPIsString, null);
            this.SPIs = parseExternalPackages(ibmSPIsString, "ibm-spi");

            for (String autoFeature : builder.getAutoFeatures()) {
                this.autoFeatures.add(autoFeature);
            }
            this.lockedAutoFeatures = this.autoFeatures.toArray(new String[this.autoFeatures.size()]);

            Set<Map.Entry<String, Attrs>> useFeatures = builder.getFeatures();

            List<String> useDepNames = new ArrayList<>(useFeatures.size());
            Map<String, Attrs> useDeps = new LinkedHashMap<>(useFeatures.size());

            useFeatures.forEach((Map.Entry<String, Attrs> entry) -> {
                String depName = entry.getKey();
                useDepNames.add(depName);
                useDeps.put(depName, entry.getValue());

            });

            useDepNames.sort(Comparator.comparing(String::toString));

            this.sortedDependentNames = useDepNames;
            this.dependentFeatures = useDeps;

            this.lockedDependentFeatures = Collections.unmodifiableMap(new LinkedHashMap<String, Attrs>(this.dependentFeatures));

            this.autoFeatures = null;

            builder.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            builder = null;
        }

        isInit = true;
    }

    public static final class ExternalPackageInfo {

        final String packageName;
        final String type;

        ExternalPackageInfo(String packageName, String type) {
            this.packageName = packageName;
            this.type = type;
        }

        public String getPackageName() {
            return packageName;
        }

        public String getType() {
            return type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(packageName, type);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ExternalPackageInfo other = (ExternalPackageInfo) obj;
            return Objects.equals(packageName, other.packageName) && Objects.equals(type, other.type);
        }

        @Override
        public String toString() {
            return packageName + " [" + type + "]";
        }
    }

    private List<ExternalPackageInfo> parseExternalPackages(String packageList, String defaultType) {
        if (packageList == null) {
            return null;
        }

        String[] packageNames = packageList.split(",");
        List<ExternalPackageInfo> extPackageInfoSet = new ArrayList<>();
        for (String packageName : packageNames) {
            String[] packageParts = packageName.split(";");
            String externalPackage = packageParts[0].trim();
            String type = null;
            if (packageParts.length > 1) {
                for (int i = 1; i < packageParts.length; ++i) {
                    String packagePart = packageParts[i].trim();
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
            extPackageInfoSet.add(new ExternalPackageInfo(externalPackage, type));
        }
        return Collections.unmodifiableList(extPackageInfoSet);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return name + " " + visibility;
    }

}
